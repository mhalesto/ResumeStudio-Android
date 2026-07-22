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
import com.resumestudio.model.ApplicationAnswer

/**
 * The answer vault, following `ApplicationAnswerVaultView`.
 *
 * Every application form asks the same handful of questions, and the answers
 * are easy to get wrong under time pressure — a salary figure typed in a hurry
 * is hard to walk back. Answered once here, calmly, and copied thereafter.
 */
@Composable
fun AnswerVaultScreen(
    answers: List<ApplicationAnswer>,
    accent: Color,
    onUpdate: (ApplicationAnswer) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = answers.count { it.isUsable }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Theme.paper()).systemBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, bottom = Theme.footerScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
                }
                Spacer(Modifier.size(4.dp))
                Column {
                    Text("Answer vault", style = displayStyle(24), color = Theme.ink())
                    Text(
                        "$ready of ${answers.size} ready to paste",
                        fontSize = 11.sp, color = Theme.mutedInk(),
                    )
                }
            }
        }

        item {
            Text(
                "The questions every application form asks. Write them once, well, " +
                    "instead of improvising a salary figure at eleven at night.",
                fontSize = 12.sp, color = Theme.mutedInk(), lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        items(answers, key = { it.id }) { answer ->
            AnswerRow(answer, accent, onUpdate)
        }
    }
}

@Composable
private fun AnswerRow(
    answer: ApplicationAnswer,
    accent: Color,
    onUpdate: (ApplicationAnswer) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember(answer.id) { mutableStateOf(answer.answer) }

    Column(
        Modifier.fillMaxWidth().cardSurface().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (answer.isUsable) accent else Theme.muted()),
                contentAlignment = Alignment.Center,
            ) {
                if (answer.isUsable) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    answer.category.title,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                )
                Text(
                    answer.answer.ifBlank { answer.title.ifBlank { answer.category.starterTitle } },
                    fontSize = 11.sp,
                    color = if (answer.isUsable) Theme.inkSoft() else Theme.mutedInk(),
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    lineHeight = 15.sp,
                )
            }
        }

        if (expanded) {
            Text(answer.category.starterTitle, fontSize = 11.sp, color = Theme.mutedInk())
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onUpdate(answer.copy(answer = it))
                },
                label = { Text("Your answer", fontSize = 12.sp) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 14.sp, color = Theme.ink()),
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
    }
}
