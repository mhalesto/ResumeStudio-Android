package com.resumestudio.data

import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeDraft
import com.resumestudio.model.ResumeLibraryArchive
import com.resumestudio.model.defaultDraftTitle
import kotlinx.serialization.json.Json

/**
 * Moving a library between devices, mirroring the export and import halves of
 * `ResumeStore.swift`.
 *
 * This is what the shared `draft.json` format is *for*. iOS writes it, Android
 * reads it, and neither had a way to hand it over until now — the format being
 * compatible is worth nothing if there is no door.
 */
object ResumeInterchange {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** The whole library, in the shape iOS's importer expects. */
    fun exportLibrary(state: ResumeStore.LibraryState): String =
        json.encodeToString(ResumeLibraryArchive(state.activeResumeID, state.resumes))

    sealed interface ImportResult {
        data class Library(val archive: ResumeLibraryArchive) : ImportResult
        data class Single(val document: ResumeDocument) : ImportResult
        data class Failed(val reason: String) : ImportResult
    }

    /**
     * Reads whatever the user handed over.
     *
     * Three shapes are accepted, in order of how much they carry: a full
     * library, a bare document from before the library existed, and nothing.
     * Falling through the shapes rather than demanding one means a file
     * exported by any version of either app still opens.
     */
    fun import(text: String): ImportResult {
        if (text.isBlank()) return ImportResult.Failed("The file is empty.")

        runCatching { json.decodeFromString<ResumeLibraryArchive>(text) }
            .getOrNull()
            ?.takeIf { it.resumes.isNotEmpty() }
            ?.let { return ImportResult.Library(it) }

        runCatching { json.decodeFromString<ResumeDocument>(text) }
            .getOrNull()
            ?.let { return ImportResult.Single(it) }

        return ImportResult.Failed("This is not a ResumeStudio export.")
    }
}

/**
 * Merges an imported library in beside what is already here.
 *
 * Deliberately additive: replacing would be simpler, but somebody importing a
 * backup usually wants the two together, and the résumé they lose to a replace
 * is the one they did not realise was only on this device.
 */
fun ResumeStore.merge(archive: ResumeLibraryArchive): Int {
    val existing = state.value.resumes.map { it.id }.toSet()
    val incoming = archive.resumes.filterNot { it.id in existing }
    if (incoming.isEmpty()) return 0
    importDrafts(incoming)
    return incoming.size
}

/** Adds an imported document as a new draft and selects it. */
fun ResumeStore.importDocument(document: ResumeDocument) {
    importDrafts(listOf(ResumeDraft(title = defaultDraftTitle(document), document = document)))
}
