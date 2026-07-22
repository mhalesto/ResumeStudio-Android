package com.resumestudio.data

import com.resumestudio.model.CoverLetterDocument
import com.resumestudio.model.ResumeDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The cover letter, mirroring `CoverLetterStore.swift`.
 *
 * One letter, like iOS: a cover letter is written for an application and
 * rewritten for the next one, so a library of them would be a filing problem
 * rather than a feature. Seeded from the résumé the first time it is opened.
 */
class CoverLetterStore(private val file: File) {

    private val _document = MutableStateFlow(load())

    val document: StateFlow<CoverLetterDocument> = _document.asStateFlow()

    fun update(document: CoverLetterDocument) {
        _document.value = document
        save()
    }

    fun edit(transform: (CoverLetterDocument) -> CoverLetterDocument) = update(transform(_document.value))

    /** Fills the sender block and the paired template from the résumé. */
    fun seedFrom(resume: ResumeDocument) = update(CoverLetterDocument.fromResume(resume))

    private fun save() {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(_document.value))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }

    private fun load(): CoverLetterDocument =
        runCatching {
            file.takeIf { it.exists() }?.readText()?.let { json.decodeFromString<CoverLetterDocument>(it) }
        }.getOrNull() ?: CoverLetterDocument()

    private companion object {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
