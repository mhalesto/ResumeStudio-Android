package com.resumestudio.data

import com.resumestudio.model.CareerCoachMessage
import com.resumestudio.model.CareerCoachMessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The coach conversation, mirroring `CareerCoachStore.swift`.
 *
 * The opening message is written here rather than fetched. It carries the
 * user's own name, and there is no reason to send a name to a server to be
 * greeted by it — iOS keeps this on device for the same reason, and drops it
 * from anything it later uploads.
 */
class CareerCoachStore(private val file: File) {

    @Serializable
    private data class Archive(val messages: List<CareerCoachMessage> = emptyList())

    private val _messages = MutableStateFlow(load())

    val messages: StateFlow<List<CareerCoachMessage>> = _messages.asStateFlow()

    fun welcome(name: String) {
        if (_messages.value.isNotEmpty()) return
        val greeting = name.split(' ').firstOrNull { it.isNotBlank() }
            ?.let { "Hi $it — I'm your Career Coach." }
            ?: "Hi — I'm your Career Coach."
        _messages.value = listOf(
            CareerCoachMessage(
                role = CareerCoachMessageRole.ASSISTANT,
                content = "$greeting Ask me about your résumé, a role you are chasing, " +
                    "or what to say in an interview.",
            ),
        )
        save()
    }

    fun append(message: CareerCoachMessage) {
        _messages.value = _messages.value + message
        save()
    }

    fun clear() {
        _messages.value = emptyList()
        save()
    }

    private fun save() {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(Archive(_messages.value)))
        }
    }

    private fun load(): List<CareerCoachMessage> =
        runCatching {
            file.takeIf { it.exists() }?.readText()
                ?.let { json.decodeFromString<Archive>(it).messages }
        }.getOrNull() ?: emptyList()

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
