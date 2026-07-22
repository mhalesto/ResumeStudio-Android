package com.resumestudio.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.resumestudio.data.ApplicationStore
import com.resumestudio.data.CoverLetterStore
import com.resumestudio.data.ResumeStore
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
    }
}
