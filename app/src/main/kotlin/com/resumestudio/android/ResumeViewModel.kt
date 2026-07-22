package com.resumestudio.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.resumestudio.data.AnswerVaultStore
import com.resumestudio.data.CareerCoachStore
import com.resumestudio.data.ResumeAIClient
import com.resumestudio.data.ApplicationStore
import com.resumestudio.data.CoverLetterStore
import com.resumestudio.data.ResumeStore
import com.resumestudio.data.ResumeInterchange
import com.resumestudio.data.importDocument
import com.resumestudio.data.merge
import com.resumestudio.model.ApplicationAnswer
import com.resumestudio.model.CareerCoachMessage
import com.resumestudio.model.CareerCoachMessageRole
import com.resumestudio.model.coachContext
import com.resumestudio.model.CareerMomentumMission
import com.resumestudio.model.CoverLetterDocument
import com.resumestudio.model.CareerMomentumPillar
import com.resumestudio.model.CareerMomentumSnapshot
import com.resumestudio.model.JobApplication
import com.resumestudio.model.JobApplicationStatus
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeFontChoice
import com.resumestudio.model.ResumePaperSize
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeTemplate
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Holds the store for the lifetime of the screen.
 *
 * The store itself takes a plain [File] so it can be tested off-device; this is
 * the only place that knows where on Android that file goes. iOS keeps it in
 * Application Support under the same name, which is what makes the two libraries
 * the same artefact rather than merely a similar one.
 */
class ResumeViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ResumeStore(File(application.filesDir, "draft.json"))
    private val applicationStore = ApplicationStore(File(application.filesDir, "applications.json"))
    private val coverLetterStore = CoverLetterStore(File(application.filesDir, "cover-letter.json"))
    private val vaultStore = AnswerVaultStore(File(application.filesDir, "answers.json"))
    private val coachStore = CareerCoachStore(File(application.filesDir, "coach.json"))

    /**
     * The AI endpoint, pointed at the same backend iOS uses.
     *
     * Tokens come back null because Firebase is not wired on Android yet, so the
     * client refuses to send rather than firing unauthenticated requests that
     * would look like they worked until the server started rejecting them.
     */
    private val ai = ResumeAIClient(
        baseUrl = AI_BASE_URL,
        clientId = installationId(application),
        tokens = { ResumeAIClient.Tokens(idToken = null, appCheck = null) },
    )

    val coachMessages = coachStore.messages

    val answers = vaultStore.answers

    val coverLetter = coverLetterStore.document

    val applications = applicationStore.applications

    // iOS keeps this in @AppStorage. Held in preferences rather than in
    // composition state so the coach introduces itself once, not on every cold
    // start — a greeting that repeats stops reading as a greeting.
    private val prefs = application.getSharedPreferences("coach", android.content.Context.MODE_PRIVATE)

    private val _coachIntroDismissed = MutableStateFlow(prefs.getBoolean(KEY_INTRO_DISMISSED, false))
    val coachIntroDismissed: StateFlow<Boolean> = _coachIntroDismissed.asStateFlow()

    fun dismissCoachIntro() {
        prefs.edit().putBoolean(KEY_INTRO_DISMISSED, true).apply()
        _coachIntroDismissed.value = true
    }

    val state = store.state

    val document: ResumeDocument get() = store.document

    fun loadExample() = store.loadExample()

    fun startBlank() = store.startBlank()

    fun edit(transform: (ResumeDocument) -> ResumeDocument) = store.edit(transform)

    // --- library ----------------------------------------------------------

    fun selectResume(id: String) = store.select(id)

    fun duplicateResume() { store.duplicateActive() }

    fun renameResume(id: String, title: String) = store.rename(id, title)

    fun deleteResume(id: String) = store.delete(id)

    fun setAccent(accent: ResumeAccent) = edit { it.copy(accent = accent) }

    fun setTemplate(template: ResumeTemplate) = edit { it.copy(template = template) }

    fun setPaperSize(size: ResumePaperSize) = edit { it.copy(layout = it.layout.copy(paperSize = size)) }

    fun setFontChoice(choice: ResumeFontChoice) = edit { it.copy(layout = it.layout.copy(fontChoice = choice)) }

    // --- career coach -------------------------------------------------------

    private val _coachThinking = MutableStateFlow(false)
    val coachThinking: StateFlow<Boolean> = _coachThinking.asStateFlow()

    private val _coachError = MutableStateFlow<String?>(null)
    val coachError: StateFlow<String?> = _coachError.asStateFlow()

    private val _coachSuggestions = MutableStateFlow(DEFAULT_PROMPTS)
    val coachSuggestions: StateFlow<List<String>> = _coachSuggestions.asStateFlow()

    fun openCoach() = coachStore.welcome(document.personal.fullName)

    fun clearCoach() {
        coachStore.clear()
        _coachError.value = null
        _coachSuggestions.value = DEFAULT_PROMPTS
        openCoach()
    }

    fun sendToCoach(text: String) {
        if (text.isBlank() || _coachThinking.value) return
        coachStore.append(CareerCoachMessage(role = CareerCoachMessageRole.USER, content = text))
        _coachError.value = null
        _coachThinking.value = true

        viewModelScope.launch {
            val context = coachContext(
                document = document,
                title = state.value.active?.title.orEmpty(),
                applications = applicationStore.applications.value,
            )
            when (val result = ai.careerCoach(context, coachStore.messages.value)) {
                is ResumeAIClient.Result.Success -> {
                    coachStore.append(
                        CareerCoachMessage(
                            role = CareerCoachMessageRole.ASSISTANT,
                            content = result.value.reply,
                        ),
                    )
                    if (result.value.suggestedPrompts.isNotEmpty()) {
                        _coachSuggestions.value = result.value.suggestedPrompts
                    }
                }
                is ResumeAIClient.Result.Failed -> _coachError.value = result.message
                is ResumeAIClient.Result.Offline ->
                    _coachError.value = "No connection. The coach needs one; everything else here does not."
                ResumeAIClient.Result.Unauthenticated ->
                    _coachError.value = "The coach needs an account. Sign-in is not on Android yet — " +
                        "it arrives with Firebase Auth, and this screen is ready for it."
            }
            _coachThinking.value = false
        }
    }

    // --- answer vault -------------------------------------------------------

    fun updateAnswer(answer: ApplicationAnswer) = vaultStore.update(answer)

    // --- backup -------------------------------------------------------------

    /** The whole library as JSON, in the shape iOS's importer expects. */
    fun exportLibrary(): String = ResumeInterchange.exportLibrary(store.state.value)

    /**
     * Reads a library or a single document out of [text].
     *
     * Merged in rather than replacing: somebody importing a backup usually
     * wants both, and the résumé they would lose to a replace is the one they
     * did not realise was only on this device.
     */
    fun importLibrary(text: String): String = when (val result = ResumeInterchange.import(text)) {
        is ResumeInterchange.ImportResult.Library -> {
            val added = store.merge(result.archive)
            if (added == 0) "Nothing new — those résumés are already here."
            else "Imported $added résumé${if (added == 1) "" else "s"}."
        }
        is ResumeInterchange.ImportResult.Single -> {
            store.importDocument(result.document)
            "Imported one résumé."
        }
        is ResumeInterchange.ImportResult.Failed -> result.reason
    }

    // --- cover letter ------------------------------------------------------

    fun editCoverLetter(transform: (CoverLetterDocument) -> CoverLetterDocument) =
        coverLetterStore.edit(transform)

    fun seedCoverLetterFromResume() = coverLetterStore.seedFrom(document)

    // --- pipeline ---------------------------------------------------------

    fun addApplication(application: JobApplication) = applicationStore.add(application)

    fun setApplicationStatus(id: String, status: JobApplicationStatus) =
        applicationStore.setStatus(id, status)

    fun removeApplication(id: String) = applicationStore.remove(id)

    /**
     * The campaign, with the Opportunities pillar filled from the pipeline.
     *
     * Relationships and Practice still read zero: there is no contact book or
     * practice recorder yet, and inventing their counts would make the score
     * meaningless. The pillar that *can* be true, is.
     */
    fun momentum(): CareerMomentumSnapshot {
        val captured = applicationStore.capturedThisWeek()
        return CareerMomentumSnapshot(
            missions = listOf(
                CareerMomentumMission(CareerMomentumPillar.OPPORTUNITIES, captured, 4),
                CareerMomentumMission(CareerMomentumPillar.RELATIONSHIPS, 0, 3),
                CareerMomentumMission(CareerMomentumPillar.PRACTICE, 0, 1),
            ),
        )
    }

    private companion object {
        const val KEY_INTRO_DISMISSED = "introDismissed"

        /** The same Cloud Functions host the iOS build points at. */
        const val AI_BASE_URL = "https://api-3wnrjfvyeq-uc.a.run.app"

        val DEFAULT_PROMPTS = listOf(
            "How is my résumé looking?",
            "Rewrite my summary",
            "What should I ask in an interview?",
        )

        /** Stable per-install id, matching what iOS sends as `clientID`. */
        fun installationId(application: Application): String {
            val prefs = application.getSharedPreferences("identity", android.content.Context.MODE_PRIVATE)
            return prefs.getString("installationID", null) ?: java.util.UUID.randomUUID().toString()
                .also { prefs.edit().putString("installationID", it).apply() }
        }
    }
}
