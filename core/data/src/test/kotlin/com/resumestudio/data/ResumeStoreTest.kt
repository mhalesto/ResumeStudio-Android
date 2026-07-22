package com.resumestudio.data

import com.resumestudio.model.PersonalDetails
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeJson
import com.resumestudio.model.ResumeTemplate
import com.resumestudio.model.blank
import com.resumestudio.model.example
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The store's contract with the file it shares with iOS.
 *
 * Most of these are compatibility rules rather than behaviour: the point is not
 * that the store round-trips its own output — that would pass with any format —
 * but that the bytes on disk stay the ones `ResumeStore.swift` expects.
 */
class ResumeStoreTest {

    @get:Rule val folder = TemporaryFolder()

    private fun draftFile() = File(folder.newFolder(), "draft.json")

    @Test
    fun `a first run starts from the example rather than an empty page`() {
        val store = ResumeStore(draftFile())
        assertEquals("Avery Sample", store.document.personal.fullName)
        assertEquals(1, store.state.value.resumes.size)
    }

    @Test
    fun `an edit survives a reopen`() {
        val file = draftFile()
        ResumeStore(file).edit { it.copy(personal = it.personal.copy(fullName = "Halalisani")) }

        assertEquals("Halalisani", ResumeStore(file).document.personal.fullName)
    }

    @Test
    fun `timestamps are written as seconds since 1970, the way iOS reads them`() {
        val file = draftFile()
        ResumeStore(file).loadExample()

        // A JSON number, not an ISO-8601 string. Getting this wrong produces a
        // file iOS cannot decode, and the symptom is a lost résumé.
        val createdAt = Regex(""""createdAt"\s*:\s*([0-9.eE+-]+)""").find(file.readText())
        assertTrue("createdAt should be a bare number, got: ${file.readText().take(400)}", createdAt != null)
        val seconds = createdAt!!.groupValues[1].toDouble()
        assertTrue("timestamp out of plausible range: $seconds", seconds > 1_600_000_000)
    }

    @Test
    fun `the archive keeps the property names iOS uses`() {
        val file = draftFile()
        ResumeStore(file).loadExample()
        val text = file.readText()
        assertTrue("missing activeResumeID", text.contains("\"activeResumeID\""))
        assertTrue("missing resumes", text.contains("\"resumes\""))
        assertTrue("missing nested document", text.contains("\"document\""))
    }

    @Test
    fun `a bare document from before the library existed is still read`() {
        val file = draftFile()
        val legacy = ResumeDocument.example.copy(
            personal = PersonalDetails(fullName = "Legacy Person", headline = "From an old build"),
        )
        file.parentFile.mkdirs()
        file.writeText(ResumeJson.encode(legacy))

        // Somebody's only résumé may be in this layout, so it must not be dropped.
        assertEquals("Legacy Person", ResumeStore(file).document.personal.fullName)
    }

    @Test
    fun `an unreadable file falls back rather than throwing`() {
        val file = draftFile()
        file.parentFile.mkdirs()
        file.writeText("{ this is not json")

        val store = ResumeStore(file)
        assertEquals("Avery Sample", store.document.personal.fullName)
        assertNull(store.state.value.saveError)
    }

    @Test
    fun `starting blank clears the page and keeps the schema version`() {
        val store = ResumeStore(draftFile())
        store.startBlank()

        assertEquals("", store.document.personal.fullName)
        assertTrue(store.document.competencies.isEmpty())
        assertEquals(ResumeDocument.CURRENT_SCHEMA_VERSION, store.document.schemaVersion)
        assertEquals(0, store.document.completionPercentage)
    }

    @Test
    fun `the example is complete, which is what makes it useful as a fixture`() {
        val store = ResumeStore(draftFile())
        store.loadExample()
        assertEquals(100, store.document.completionPercentage)
    }

    @Test
    fun `switching template and accent persists`() {
        val file = draftFile()
        ResumeStore(file).edit {
            it.copy(template = ResumeTemplate.ATLAS, accent = ResumeAccent.COBALT)
        }

        val reopened = ResumeStore(file)
        assertEquals(ResumeTemplate.ATLAS, reopened.document.template)
        assertEquals(ResumeAccent.COBALT, reopened.document.accent)
    }

    @Test
    fun `an interrupted write leaves no stray temp file behind`() {
        val file = draftFile()
        val store = ResumeStore(file)
        store.loadExample()

        val strays = file.parentFile.listFiles()?.filter { it.name.endsWith(".tmp") }.orEmpty()
        assertTrue("temp files left behind: ${strays.map { it.name }}", strays.isEmpty())
    }

    @Test
    fun `each new draft gets its own identity`() {
        val store = ResumeStore(draftFile())
        val first = store.state.value.activeResumeID
        store.startBlank()
        assertNotEquals(first, store.state.value.activeResumeID)
    }

    @Test
    fun `the draft title follows the headline, as iOS names it`() {
        val store = ResumeStore(draftFile())
        store.loadExample()
        assertEquals("People Operations Manager | Employee Experience", store.state.value.active?.title)

        store.startBlank()
        assertEquals("My Résumé", store.state.value.active?.title)
    }
}

/** Split out because it is about the file appearing, not about its contents. */
class ResumeStoreFirstRunTest {

    @get:Rule val folder = TemporaryFolder()

    @Test
    fun `a first run leaves a library on disk`() {
        val file = File(folder.newFolder(), "draft.json")
        val store = ResumeStore(file)

        assertTrue("draft.json should exist after a first run", file.exists())

        // And the identity it wrote is the identity it reopens with, so anything
        // that later syncs these drafts sees stable IDs.
        assertEquals(store.state.value.activeResumeID, ResumeStore(file).state.value.activeResumeID)
    }
}

/** The library operations, whose failure modes are all about losing work. */
class ResumeLibraryTest {

    @get:Rule val folder = TemporaryFolder()

    private fun store() = ResumeStore(File(folder.newFolder(), "draft.json"))

    @Test
    fun `duplicating copies the document and makes the copy active`() {
        val store = store()
        val original = store.state.value.activeResumeID
        store.duplicateActive()

        assertEquals(2, store.state.value.resumes.size)
        assertNotEquals(original, store.state.value.activeResumeID)
        // The copy carries the content — tailoring starts from the work already done.
        assertEquals("Avery Sample", store.document.personal.fullName)
        assertTrue(store.state.value.active!!.title.endsWith("Copy"))
    }

    @Test
    fun `editing after duplicating leaves the original untouched`() {
        val store = store()
        val originalID = store.state.value.activeResumeID
        store.duplicateActive()
        store.edit { it.copy(personal = it.personal.copy(fullName = "Tailored Version")) }

        val original = store.state.value.resumes.first { it.id == originalID }
        assertEquals("Avery Sample", original.document.personal.fullName)
        assertEquals("Tailored Version", store.document.personal.fullName)
    }

    @Test
    fun `the last resume cannot be deleted`() {
        val store = store()
        store.delete(store.state.value.activeResumeID)

        // Every screen behind the library assumes an active document exists.
        assertEquals(1, store.state.value.resumes.size)
    }

    @Test
    fun `deleting the active resume selects a neighbour`() {
        val store = store()
        store.duplicateActive()
        val active = store.state.value.activeResumeID

        store.delete(active)

        assertEquals(1, store.state.value.resumes.size)
        assertNotEquals(active, store.state.value.activeResumeID)
        assertNotNull(store.state.value.active)
    }

    @Test
    fun `renaming falls back rather than leaving a blank title`() {
        val store = store()
        val id = store.state.value.activeResumeID
        store.rename(id, "   ")
        assertEquals("Untitled Résumé", store.state.value.active?.title)
    }

    @Test
    fun `the library survives a reopen with its selection intact`() {
        val file = File(folder.newFolder(), "draft.json")
        val store = ResumeStore(file)
        store.duplicateActive()
        store.rename(store.state.value.activeResumeID, "Tailored for Acme")
        val activeID = store.state.value.activeResumeID

        val reopened = ResumeStore(file)
        assertEquals(2, reopened.state.value.resumes.size)
        assertEquals(activeID, reopened.state.value.activeResumeID)
        assertEquals("Tailored for Acme", reopened.state.value.active?.title)
    }
}
