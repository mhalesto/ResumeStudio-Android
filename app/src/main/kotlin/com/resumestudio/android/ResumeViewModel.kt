package com.resumestudio.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.resumestudio.data.ResumeStore
import com.resumestudio.model.ResumeAccent
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

    fun setAccent(accent: ResumeAccent) = edit { it.copy(accent = accent) }

    fun setTemplate(template: ResumeTemplate) = edit { it.copy(template = template) }

    private companion object {
        const val KEY_INTRO_DISMISSED = "introDismissed"
    }
}
