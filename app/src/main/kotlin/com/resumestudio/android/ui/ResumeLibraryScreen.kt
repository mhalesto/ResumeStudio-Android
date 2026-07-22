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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.ResumeDraft
import kotlin.math.roundToLong

/**
 * Every saved résumé, following `ResumeLibraryView`.
 *
 * The library exists so a résumé can be tailored to one job without losing the
 * general one, which is why duplicate is the prominent action rather than
 * "new" — starting blank throws away the work that makes tailoring quick.
 */
@Composable
fun ResumeLibraryScreen(
    resumes: List<ResumeDraft>,
    activeID: String,
    accent: Color,
    onSelect: (String) -> Unit,
    onDuplicate: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var renaming by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Theme.paper()).systemBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, bottom = Theme.footerScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
                }
                Spacer(Modifier.size(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("My résumés", style = displayStyle(24), color = Theme.ink())
                    Text(
                        "${resumes.size} version${if (resumes.size == 1) "" else "s"}",
                        fontSize = 11.sp, color = Theme.mutedInk(),
                    )
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent)
                    .clickable(onClick = onDuplicate)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ContentCopy, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Duplicate this résumé",
                    color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                )
            }
        }

        items(resumes, key = { it.id }) { draft ->
            LibraryRow(
                draft = draft,
                isActive = draft.id == activeID,
                // The last résumé cannot be deleted: every screen behind this
                // one assumes an active document exists.
                canDelete = resumes.size > 1,
                isRenaming = renaming == draft.id,
                accent = accent,
                onSelect = { onSelect(draft.id) },
                onStartRename = { renaming = draft.id },
                onRename = { title ->
                    onRename(draft.id, title)
                    renaming = null
                },
                onDelete = { onDelete(draft.id) },
            )
        }
    }
}

@Composable
private fun LibraryRow(
    draft: ResumeDraft,
    isActive: Boolean,
    canDelete: Boolean,
    isRenaming: Boolean,
    accent: Color,
    onSelect: () -> Unit,
    onStartRename: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().cardSurface().clickable(onClick = onSelect).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isActive) accent else Theme.muted()),
                contentAlignment = Alignment.Center,
            ) {
                if (isActive) {
                    Icon(Icons.Filled.Check, "Active", tint = Color.White, modifier = Modifier.size(17.dp))
                } else {
                    Text(
                        draft.document.initials.ifBlank { "?" },
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.mutedInk(),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    draft.title,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                )
                Text(
                    listOf(
                        draft.document.personal.fullName.ifBlank { "Untitled" },
                        draft.document.template.wireName.replaceFirstChar { it.uppercase() },
                        relativeTime(draft.updatedAt),
                    ).joinToString(" · "),
                    fontSize = 11.sp, color = Theme.mutedInk(),
                )
            }
            IconButton(onClick = onStartRename, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Filled.Edit, "Rename", tint = Theme.mutedInk(), modifier = Modifier.size(15.dp))
            }
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", tint = Theme.mutedInk(), modifier = Modifier.size(15.dp))
                }
            }
        }

        if (isRenaming) {
            var title by remember(draft.id) { mutableStateOf(draft.title) }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 14.sp, color = Theme.ink()),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = Theme.hairline(),
                    focusedLabelColor = accent,
                    cursorColor = accent,
                ),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent)
                    .clickable { onRename(title) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save name", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

/** "just now", "4m", "3h", "2d" — the shape iOS's relative style produces. */
private fun relativeTime(seconds: Double): String {
    val elapsed = (System.currentTimeMillis() / 1000.0 - seconds).roundToLong()
    return when {
        elapsed < 60 -> "just now"
        elapsed < 3_600 -> "${elapsed / 60}m ago"
        elapsed < 86_400 -> "${elapsed / 3_600}h ago"
        else -> "${elapsed / 86_400}d ago"
    }
}
