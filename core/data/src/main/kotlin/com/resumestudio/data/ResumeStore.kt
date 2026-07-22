package com.resumestudio.data

import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeDraft
import com.resumestudio.model.ResumeLibraryArchive
import com.resumestudio.model.blank
import com.resumestudio.model.defaultDraftTitle
import com.resumestudio.model.example
import com.resumestudio.model.nowSeconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The résumé library and the file it lives in, mirroring `ResumeStore.swift`.
 *
 * Reads and writes `draft.json` in the same shape iOS does, because that file is
 * the handover between the two apps. Everything about the format that is not
 * obvious — seconds-since-1970 timestamps, the bare-document legacy layout — is
 * a compatibility requirement rather than a preference, and is pinned by tests.
 *
 * Takes a [File] rather than a platform context so it stays unit-testable.
 */
class ResumeStore(private val file: File) {

    private val _state = MutableStateFlow(load())

    init {
        // iOS writes the library it just constructed, so a first run leaves a
        // file behind instead of rebuilding the example on every launch. Without
        // this the library has no identity until the first edit — draft IDs would
        // change under anything that later syncs or imports them.
        if (!file.exists()) save()
    }

    val state: StateFlow<LibraryState> = _state.asStateFlow()

    val document: ResumeDocument get() = _state.value.document

    data class LibraryState(
        val resumes: List<ResumeDraft>,
        val activeResumeID: String,
        /** Set when the last write failed, so the UI can say so rather than lying. */
        val saveError: String? = null,
    ) {
        val active: ResumeDraft? get() = resumes.firstOrNull { it.id == activeResumeID }
        val document: ResumeDocument get() = active?.document ?: ResumeDocument.blank
    }

    // --- editing ----------------------------------------------------------

    /** Replaces the active document and writes. */
    fun update(document: ResumeDocument) {
        val current = _state.value
        val updated = current.resumes.map { draft ->
            if (draft.id == current.activeResumeID) {
                draft.copy(document = document, updatedAt = nowSeconds())
            } else {
                draft
            }
        }
        _state.value = current.copy(resumes = updated)
        save()
    }

    /** Edits the active document in place — the common case from an editor field. */
    fun edit(transform: (ResumeDocument) -> ResumeDocument) = update(transform(document))

    fun loadExample() = replaceActive(ResumeDocument.example)

    fun startBlank() = replaceActive(ResumeDocument.blank)

    private fun replaceActive(document: ResumeDocument) {
        val draft = ResumeDraft(title = defaultDraftTitle(document), document = document)
        _state.value = LibraryState(
            resumes = _state.value.resumes.filterNot { it.id == _state.value.activeResumeID } + draft,
            activeResumeID = draft.id,
        )
        save()
    }

    /**
     * Copies the active résumé, mirroring `duplicateActiveResume`.
     *
     * Tailoring a résumé to one job without losing the general one is the whole
     * reason the library exists, so the copy becomes active immediately — the
     * next edit belongs to the copy, not the original.
     */
    fun duplicateActive(): String {
        val current = _state.value
        val base = current.active?.title ?: defaultDraftTitle(document)
        val copy = ResumeDraft(title = "$base Copy", document = document)
        _state.value = current.copy(
            resumes = current.resumes + copy,
            activeResumeID = copy.id,
        )
        save()
        return copy.id
    }

    fun rename(id: String, title: String) {
        _state.value = _state.value.copy(
            resumes = _state.value.resumes.map {
                if (it.id == id) {
                    it.copy(title = title.trim().ifBlank { "Untitled Résumé" }, updatedAt = nowSeconds())
                } else {
                    it
                }
            },
        )
        save()
    }

    /**
     * Deletes a résumé, unless it is the only one.
     *
     * iOS guards on `resumes.count > 1` for the same reason: an empty library
     * has no active document, and the screens behind it all assume one exists.
     * Refusing is better than silently creating a blank replacement.
     */
    fun delete(id: String) {
        val current = _state.value
        if (current.resumes.size <= 1) return
        val index = current.resumes.indexOfFirst { it.id == id }
        if (index < 0) return

        val remaining = current.resumes.filterNot { it.id == id }
        _state.value = current.copy(
            resumes = remaining,
            activeResumeID = if (current.activeResumeID == id) {
                remaining[index.coerceAtMost(remaining.lastIndex)].id
            } else {
                current.activeResumeID
            },
        )
        save()
    }

    /**
     * Adds drafts from an import and selects the first of them.
     *
     * Selecting the newcomer is the point: somebody who has just imported a
     * backup is looking for what they imported, not for what was already open.
     */
    fun importDrafts(drafts: List<ResumeDraft>) {
        if (drafts.isEmpty()) return
        _state.value = _state.value.copy(
            resumes = _state.value.resumes + drafts,
            activeResumeID = drafts.first().id,
        )
        save()
    }

    fun select(id: String) {
        if (_state.value.resumes.any { it.id == id }) {
            _state.value = _state.value.copy(activeResumeID = id)
            save()
        }
    }

    // --- persistence ------------------------------------------------------

    fun save() {
        val current = _state.value
        val error = runCatching {
            file.parentFile?.mkdirs()
            val archive = ResumeLibraryArchive(current.activeResumeID, current.resumes)
            // Written beside the target and moved into place, so an interrupted
            // write leaves the previous library intact rather than a half file.
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(archive))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }.exceptionOrNull()

        _state.value = current.copy(saveError = error?.message)
    }

    private fun load(): LibraryState {
        val text = runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()

        if (text != null) {
            runCatching { json.decodeFromString<ResumeLibraryArchive>(text) }
                .getOrNull()
                ?.takeIf { it.resumes.isNotEmpty() }
                ?.let { archive ->
                    val active = archive.resumes
                        .firstOrNull { it.id == archive.activeResumeID }
                        ?: archive.resumes.first()
                    return LibraryState(archive.resumes, active.id)
                }

            // The layout before the library existed: a bare document. Read rather
            // than discarded, because somebody's only résumé may be in it.
            runCatching { json.decodeFromString<ResumeDocument>(text) }
                .getOrNull()
                ?.takeIf { it.schemaVersion >= ResumeDocument.CURRENT_SCHEMA_VERSION }
                ?.let { return singleDraft(it) }
        }

        return singleDraft(ResumeDocument.example)
    }

    private fun singleDraft(document: ResumeDocument): LibraryState {
        val draft = ResumeDraft(title = defaultDraftTitle(document), document = document)
        return LibraryState(listOf(draft), draft.id)
    }

    private companion object {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
