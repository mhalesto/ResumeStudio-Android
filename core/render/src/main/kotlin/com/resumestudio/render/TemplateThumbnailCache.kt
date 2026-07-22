package com.resumestudio.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Rendered page thumbnails for the gallery, mirroring `ThumbnailCache.swift`.
 *
 * Rendering 140 templates is not something to do on a scroll. Two layers:
 * an in-memory [LruCache] for the current session, and PNGs on disk keyed by a
 * digest of the document so a thumbnail survives a relaunch but is discarded
 * the moment the résumé it depicts changes.
 *
 * Renders are limited to a few at a time. Letting a fast scroll start 140 of
 * them would spend the whole CPU on thumbnails that scrolled past before they
 * finished — the visible ones would arrive last, which is exactly backwards.
 */
class TemplateThumbnailCache(
    private val directory: File,
    private val rasterizer: ResumePageRasterizer,
) {

    private val memory = LruCache<String, Bitmap>(MEMORY_ENTRIES)
    private val gate = Semaphore(CONCURRENT_RENDERS)

    /**
     * The thumbnail for [template], rendering it if needed.
     *
     * Returns null rather than throwing when a template will not render: a
     * missing thumbnail costs the user a picture, where a thrown one would cost
     * them the gallery.
     */
    suspend fun thumbnail(
        document: ResumeDocument,
        template: ResumeTemplate,
        widthPx: Int = THUMBNAIL_WIDTH,
    ): Bitmap? {
        val key = key(document, template, widthPx)
        memory.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            diskFile(key).takeIf { it.exists() }
                ?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }
                ?.also { memory.put(key, it) }
                ?: render(document, template, widthPx, key)
        }
    }

    private suspend fun render(
        document: ResumeDocument,
        template: ResumeTemplate,
        widthPx: Int,
        key: String,
    ): Bitmap? = gate.withPermit {
        // Checked again inside the gate: while this call was queued, the
        // thumbnail it wanted may already have been produced by another.
        memory.get(key)?.let { return@withPermit it }

        val page = rasterizer.rasterize(document.copy(template = template), 0, widthPx)
            ?: return@withPermit null

        memory.put(key, page.bitmap)
        runCatching {
            directory.mkdirs()
            diskFile(key).outputStream().use { page.bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
            prune()
        }
        page.bitmap
    }

    /** Drops the oldest files once the directory outgrows its budget. */
    private fun prune() {
        val files = directory.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= MAX_DISK_BYTES) break
            total -= file.length()
            file.delete()
        }
    }

    /**
     * Keyed on the content, not the template name.
     *
     * A thumbnail depicts one résumé in one template. Keying on the template
     * alone would show the previous owner's name after the document changed,
     * which is the kind of bug that looks like a privacy failure.
     */
    private fun key(document: ResumeDocument, template: ResumeTemplate, widthPx: Int): String {
        val source = buildString {
            append(template.wireName).append('|')
            append(document.accent.wireName).append('|')
            append(document.layout.paperSize.name).append('|')
            append(document.layout.fontChoice.name).append('|')
            append(widthPx).append('|')
            append(document.personal.fullName).append('|')
            append(document.personal.headline).append('|')
            append(document.professionalProfile.length).append('|')
            append(document.competencies.joinToString(",")).append('|')
            append(document.experience.joinToString(",") { it.role + it.company + it.highlights.size })
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }

    private fun diskFile(key: String) = File(directory, "$key.png")

    private companion object {
        const val MEMORY_ENTRIES = 60
        const val CONCURRENT_RENDERS = 3
        const val THUMBNAIL_WIDTH = 220
        const val MAX_DISK_BYTES = 24L * 1024 * 1024
    }
}
