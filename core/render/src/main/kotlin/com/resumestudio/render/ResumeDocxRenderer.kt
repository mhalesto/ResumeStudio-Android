package com.resumestudio.render

import com.resumestudio.model.ResumeDocument
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * An editable Word document, porting `ResumeDOCXRenderer` from
 * `ResumeDocumentInterchange.swift`.
 *
 * A .docx is a zip of XML parts, so this writes the same five parts iOS does
 * and zips them — `java.util.zip` is in the platform, which is why ZIPFoundation
 * has no counterpart in this project's dependencies.
 *
 * The output is deliberately plain. Someone asking for DOCX wants to edit the
 * words in Word, not to receive a reproduction of a two-column template that
 * will fall apart the moment they do.
 */
object ResumeDocxRenderer {

    fun render(document: ResumeDocument): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.write("[Content_Types].xml", CONTENT_TYPES)
            zip.write("_rels/.rels", PACKAGE_RELATIONSHIPS)
            zip.write("word/_rels/document.xml.rels", DOCUMENT_RELATIONSHIPS)
            zip.write("word/styles.xml", styles(document))
            zip.write("word/document.xml", documentXml(document))
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.write(name: String, contents: String) {
        putNextEntry(ZipEntry(name))
        write(contents.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun documentXml(document: ResumeDocument): String {
        val body = StringBuilder()

        fun paragraph(text: String, style: String? = null) {
            if (text.isBlank()) return
            val styleXml = style?.let { """<w:pPr><w:pStyle w:val="$it"/></w:pPr>""" }.orEmpty()
            body.append(
                """<w:p>$styleXml<w:r><w:t xml:space="preserve">${text.escaped()}</w:t></w:r></w:p>""",
            )
        }

        fun heading(value: String) = paragraph(value.uppercase(), "Heading1")
        fun bullet(value: String) = paragraph("• $value", "Bullet")

        paragraph(document.personal.fullName, "Title")
        paragraph(document.personal.headline, "Subtitle")
        paragraph(
            listOf(document.personal.phone, document.personal.email)
                .filter { it.isNotBlank() }.joinToString("  |  "),
        )

        heading("Professional Profile")
        paragraph(document.professionalProfile)

        heading("Core Competencies")
        document.competencies.filter { it.isNotBlank() }.forEach(::bullet)

        heading("Professional Experience")
        document.experience
            .filter { it.role.isNotBlank() || it.company.isNotBlank() }
            .forEach { entry ->
                paragraph(entry.role, "Heading2")
                paragraph(listOf(entry.company, entry.period).filter { it.isNotBlank() }.joinToString("  |  "))
                entry.highlights.filter { it.isNotBlank() }.forEach(::bullet)
            }

        heading("Education")
        document.education
            .filter { it.qualification.isNotBlank() || it.institution.isNotBlank() }
            .forEach { entry ->
                paragraph(entry.qualification, "Heading2")
                paragraph(listOf(entry.institution, entry.period).filter { it.isNotBlank() }.joinToString("  |  "))
                paragraph(entry.details)
            }

        document.additionalSections.filter { it.title.isNotBlank() }.forEach { section ->
            heading(section.title)
            section.items.filter { it.isNotBlank() }.forEach(::bullet)
        }

        if (document.references.isNotEmpty()) {
            heading("References")
            document.references.filter { it.name.isNotBlank() }.forEach { entry ->
                paragraph(entry.name, "Heading2")
                paragraph(
                    listOf(entry.company, entry.phone, entry.email)
                        .filter { it.isNotBlank() }.joinToString("  |  "),
                )
            }
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>$body<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>
<w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr></w:body></w:document>"""
    }

    /** Headings carry the user's accent, which is the one piece of the look that survives. */
    private fun styles(document: ResumeDocument): String {
        val hex = "%02X%02X%02X".format(
            (document.accent.red * 255).toInt().coerceIn(0, 255),
            (document.accent.green * 255).toInt().coerceIn(0, 255),
            (document.accent.blue * 255).toInt().coerceIn(0, 255),
        )
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/>
<w:pPr><w:spacing w:after="60"/></w:pPr><w:rPr><w:b/><w:sz w:val="48"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Subtitle"><w:name w:val="Subtitle"/>
<w:pPr><w:spacing w:after="180"/></w:pPr><w:rPr><w:color w:val="$hex"/><w:sz w:val="24"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/>
<w:pPr><w:spacing w:before="280" w:after="80"/></w:pPr>
<w:rPr><w:b/><w:color w:val="$hex"/><w:sz w:val="24"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/>
<w:pPr><w:spacing w:before="140" w:after="20"/></w:pPr><w:rPr><w:b/><w:sz w:val="22"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Bullet"><w:name w:val="List Paragraph"/>
<w:pPr><w:ind w:left="360"/><w:spacing w:after="20"/></w:pPr></w:style>
</w:styles>"""
    }

    private fun String.escaped(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

    private const val PACKAGE_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private const val DOCUMENT_RELATIONSHIPS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
}
