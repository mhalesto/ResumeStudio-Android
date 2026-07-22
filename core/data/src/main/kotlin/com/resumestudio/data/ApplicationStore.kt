package com.resumestudio.data

import com.resumestudio.model.ApplicationArchive
import com.resumestudio.model.JobApplication
import com.resumestudio.model.JobApplicationStatus
import com.resumestudio.model.nowSeconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The application pipeline, mirroring `ApplicationStore.swift`.
 *
 * Kept in its own file rather than inside the résumé library: the two have
 * different lifetimes, and a corrupt pipeline must not be able to take somebody's
 * résumé down with it.
 */
class ApplicationStore(private val file: File) {

    private val _applications = MutableStateFlow(load())

    init {
        if (!file.exists()) save()
    }

    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    fun add(application: JobApplication) {
        _applications.value = _applications.value + application
        save()
    }

    fun update(application: JobApplication) {
        _applications.value = _applications.value.map {
            if (it.id == application.id) application.copy(updatedAt = nowSeconds()) else it
        }
        save()
    }

    fun remove(id: String) {
        _applications.value = _applications.value.filterNot { it.id == id }
        save()
    }

    fun setStatus(id: String, status: JobApplicationStatus) {
        _applications.value.firstOrNull { it.id == id }?.let { update(it.copy(status = status)) }
    }

    /**
     * How many opportunities were captured in the last seven days.
     *
     * Feeds the campaign's Opportunities pillar. A rolling window rather than a
     * calendar week, because the pillar is about rhythm and a Monday reset
     * would zero somebody's streak mid-effort.
     */
    fun capturedThisWeek(now: Double = nowSeconds()): Int =
        _applications.value.count { now - it.createdAt <= SEVEN_DAYS }

    private fun save() {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(ApplicationArchive(_applications.value)))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }

    private fun load(): List<JobApplication> =
        runCatching {
            file.takeIf { it.exists() }?.readText()
                ?.let { json.decodeFromString<ApplicationArchive>(it).applications }
        }.getOrNull() ?: emptyList()

    private companion object {
        const val SEVEN_DAYS = 7 * 24 * 60 * 60.0

        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
