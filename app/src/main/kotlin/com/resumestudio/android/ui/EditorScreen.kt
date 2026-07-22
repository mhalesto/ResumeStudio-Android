package com.resumestudio.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.ExperienceEntry
import com.resumestudio.model.ResumeDocument

/**
 * The résumé editor.
 *
 * Every field writes straight through to the store, which saves on each change.
 * There is no explicit save, matching iOS — a résumé is not a document you
 * commit, it is a thing you keep adjusting, and an unsaved-changes prompt would
 * only ever be a way to lose work.
 *
 * Covers personal details, the profile, competencies and roles. Education,
 * references and the extra sections are not here yet; they follow the same shape
 * as roles and are the next obvious addition.
 */
@Composable
fun EditorScreen(
    document: ResumeDocument,
    /**
     * Identity of the document being edited. Fields re-seed when this changes —
     * i.e. when the document is replaced wholesale — and not otherwise.
     */
    documentKey: String,
    accent: Color,
    onEdit: ((ResumeDocument) -> ResumeDocument) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper())
            .systemBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
                }
                Spacer(Modifier.size(4.dp))
                Column {
                    Text("Edit résumé", style = displayStyle(24), color = Theme.ink())
                    Text(
                        "${document.completionPercentage}% complete · saves as you type",
                        fontSize = 11.sp, color = Theme.mutedInk(),
                    )
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().cardSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Personal details")
                Field("Full name", document.personal.fullName, accent, documentKey) { value ->
                    onEdit { it.copy(personal = it.personal.copy(fullName = value)) }
                }
                Field("Headline", document.personal.headline, accent, documentKey) { value ->
                    onEdit { it.copy(personal = it.personal.copy(headline = value)) }
                }
                Field("Email", document.personal.email, accent, documentKey) { value ->
                    onEdit { it.copy(personal = it.personal.copy(email = value)) }
                }
                Field("Phone", document.personal.phone, accent, documentKey) { value ->
                    onEdit { it.copy(personal = it.personal.copy(phone = value)) }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().cardSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Professional profile", "The opening paragraph a recruiter reads first.")
                Field("Profile", document.professionalProfile, accent, documentKey, minLines = 4) { value ->
                    onEdit { it.copy(professionalProfile = value) }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().cardSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading(
                    "Core competencies",
                    // Worth saying, because several templates rank by list order.
                    "Order matters: the meter and dot styles rank by position.",
                )
                document.competencies.forEachIndexed { index, skill ->
                    RemovableRow(skill, accent) {
                        onEdit { doc ->
                            doc.copy(competencies = doc.competencies.filterIndexed { i, _ -> i != index })
                        }
                    }
                }
                AddRow("Add a competency", accent) { value ->
                    onEdit { it.copy(competencies = it.competencies + value) }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().cardSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Experience", "${document.experience.size} role${if (document.experience.size == 1) "" else "s"}.")
                document.experience.forEachIndexed { index, entry ->
                    RoleEditor(
                        entry = entry,
                        accent = accent,
                        resetKey = "$documentKey:${entry.id}",
                        onChange = { updated ->
                            onEdit { doc ->
                                doc.copy(
                                    experience = doc.experience.toMutableList().also { it[index] = updated },
                                )
                            }
                        },
                        onRemove = {
                            onEdit { doc ->
                                doc.copy(experience = doc.experience.filterIndexed { i, _ -> i != index })
                            }
                        },
                    )
                }
                AddRow("Add a role", accent) { value ->
                    onEdit { it.copy(experience = it.experience + ExperienceEntry(role = value)) }
                }
            }
        }
    }
}

@Composable
private fun RoleEditor(
    entry: ExperienceEntry,
    accent: Color,
    resetKey: Any,
    onChange: (ExperienceEntry) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.tileRadius))
            .background(Theme.muted().copy(alpha = 0.45f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(entry.role.ifBlank { "Untitled role" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, "Remove role", tint = Theme.mutedInk(), modifier = Modifier.size(15.dp))
            }
        }
        Field("Role", entry.role, accent, resetKey) { onChange(entry.copy(role = it)) }
        Field("Company", entry.company, accent, resetKey) { onChange(entry.copy(company = it)) }
        Field("Period", entry.period, accent, resetKey) { onChange(entry.copy(period = it)) }

        entry.highlights.forEachIndexed { index, highlight ->
            RemovableRow(highlight, accent) {
                onChange(entry.copy(highlights = entry.highlights.filterIndexed { i, _ -> i != index }))
            }
        }
        AddRow("Add a highlight", accent) { onChange(entry.copy(highlights = entry.highlights + it)) }
    }
}

/**
 * A text field that owns what it is showing.
 *
 * Feeding [value] straight back from the store made fast typing drop characters:
 * every keystroke writes, the write lands on a StateFlow, and the recomposition
 * carrying the new value arrives after the next keystroke has already been
 * handled against the old one. Typing "Halalisani" persisted "H".
 *
 * So the field is the source of truth for its own text while it is on screen,
 * and the store is written on every change. [resetKey] re-seeds it when the
 * document underneath is swapped for a different one.
 */
@Composable
private fun Field(
    label: String,
    value: String,
    accent: Color,
    resetKey: Any,
    minLines: Int = 1,
    onChange: (String) -> Unit,
) {
    var text by remember(resetKey) { mutableStateOf(value) }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        label = { Text(label, fontSize = 12.sp) },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Theme.ink()),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = Theme.hairline(),
            focusedLabelColor = accent,
            unfocusedLabelColor = Theme.mutedInk(),
            cursorColor = accent,
        ),
    )
}

@Composable
private fun RemovableRow(text: String, accent: Color, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.size(10.dp))
        Text(text, fontSize = 13.sp, color = Theme.inkSoft(), modifier = Modifier.weight(1f), lineHeight = 18.sp)
        IconButton(onClick = onRemove, modifier = Modifier.size(22.dp)) {
            Icon(Icons.Filled.Close, "Remove", tint = Theme.mutedInk(), modifier = Modifier.size(14.dp))
        }
    }
}

/** A field that commits on the button and then clears itself. */
@Composable
private fun AddRow(placeholder: String, accent: Color, onAdd: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = { Text(placeholder, fontSize = 12.sp, color = Theme.mutedInk()) },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Theme.ink()),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = Theme.hairline(),
                cursorColor = accent,
            ),
        )
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (draft.isBlank()) Theme.muted() else accent)
                .clickable(enabled = draft.isNotBlank()) {
                    onAdd(draft.trim())
                    draft = ""
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add, "Add",
                tint = if (draft.isBlank()) Theme.mutedInk() else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
