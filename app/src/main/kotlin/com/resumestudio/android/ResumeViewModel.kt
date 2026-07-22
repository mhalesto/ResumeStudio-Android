package com.resumestudio.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.resumestudio.data.ResumeStore
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeTemplate
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

    val state = store.state

    val document: ResumeDocument get() = store.document

    fun loadExample() = store.loadExample()

    fun startBlank() = store.startBlank()

    fun edit(transform: (ResumeDocument) -> ResumeDocument) = store.edit(transform)

    fun setAccent(accent: ResumeAccent) = edit { it.copy(accent = accent) }

    fun setTemplate(template: ResumeTemplate) = edit { it.copy(template = template) }
}
