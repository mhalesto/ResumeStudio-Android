package com.resumestudio.data

import com.resumestudio.model.AnswerVaultArchive
import com.resumestudio.model.ApplicationAnswer
import com.resumestudio.model.nowSeconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The answer vault, mirroring `ApplicationAnswerVaultStore.swift`.
 *
 * Opens as a checklist of the questions every form asks rather than an empty
 * page: the value is in having answered them before the form is in front of
 * you, and an empty list does not prompt anyone to do that.
 */
class AnswerVaultStore(private val file: File) {

    private val _answers = MutableStateFlow(load())

    init {
        if (!file.exists()) save()
    }

    val answers: StateFlow<List<ApplicationAnswer>> = _answers.asStateFlow()

    fun update(answer: ApplicationAnswer) {
        _answers.value = _answers.value.map {
            if (it.id == answer.id) answer.copy(updatedAt = nowSeconds()) else it
        }
        save()
    }

    fun add(answer: ApplicationAnswer) {
        _answers.value = _answers.value + answer
        save()
    }

    fun remove(id: String) {
        _answers.value = _answers.value.filterNot { it.id == id }
        save()
    }

    /** How many of the standard questions have a usable answer. */
    fun readyCount(): Int = _answers.value.count { it.isUsable }

    private fun save() {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(AnswerVaultArchive(_answers.value)))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }

    private fun load(): List<ApplicationAnswer> =
        runCatching {
            file.takeIf { it.exists() }?.readText()
                ?.let { json.decodeFromString<AnswerVaultArchive>(it).answers }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: AnswerVaultArchive.starter().answers

    private companion object {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
