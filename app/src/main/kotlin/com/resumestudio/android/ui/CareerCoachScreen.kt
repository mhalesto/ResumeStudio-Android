package com.resumestudio.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.resumestudio.model.CareerCoachMessage
import com.resumestudio.model.CareerCoachMessageRole

/**
 * The coach conversation, following `CareerCoachView`.
 *
 * The face stays at the top of the thread rather than beside every message: a
 * portrait repeated down a chat reads as a stock avatar, where one at the head
 * of the conversation reads as the person you are talking to.
 */
@Composable
fun CareerCoachScreen(
    messages: List<CareerCoachMessage>,
    suggestions: List<String>,
    isThinking: Boolean,
    error: String?,
    accent: Color,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier.fillMaxSize().background(Theme.paper()).systemBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
            }
            CareerCoachFace(accent = accent, size = 34.dp)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Career coach", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
                Text(
                    if (isThinking) "Thinking…" else "Here when you need a second opinion",
                    fontSize = 10.5.sp, color = Theme.mutedInk(),
                )
            }
            if (messages.size > 1) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.DeleteOutline, "Clear conversation", tint = Theme.mutedInk(), modifier = Modifier.size(19.dp))
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { message -> Bubble(message, accent) }

            if (isThinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.size(9.dp))
                        Text("Working on it…", fontSize = 12.sp, color = Theme.mutedInk())
                    }
                }
            }

            error?.let {
                item {
                    Text(
                        it,
                        fontSize = 12.sp, color = Color(0xFFCC3B34), lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFCC3B34).copy(alpha = 0.10f))
                            .padding(12.dp),
                    )
                }
            }
        }

        if (suggestions.isNotEmpty() && !isThinking) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.forEach { prompt ->
                    Text(
                        prompt,
                        fontSize = 11.5.sp, color = Theme.inkSoft(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Theme.card())
                            .border(1.dp, Theme.hairline(), RoundedCornerShape(14.dp))
                            .clickable { onSend(prompt) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Ask the coach…", fontSize = 13.sp, color = Theme.mutedInk()) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                textStyle = TextStyle(fontSize = 14.sp, color = Theme.ink()),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = Theme.hairline(),
                    cursorColor = accent,
                ),
            )
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(if (draft.isBlank() || isThinking) Theme.muted() else accent)
                    .clickable(enabled = draft.isNotBlank() && !isThinking) {
                        onSend(draft.trim())
                        draft = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, "Send",
                    tint = if (draft.isBlank() || isThinking) Theme.mutedInk() else Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun Bubble(message: CareerCoachMessage, accent: Color) {
    val fromUser = message.role == CareerCoachMessageRole.USER

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            message.content,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = if (fromUser) Color.White else Theme.ink(),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (fromUser) 16.dp else 4.dp,
                        bottomEnd = if (fromUser) 4.dp else 16.dp,
                    ),
                )
                .background(if (fromUser) accent else Theme.card())
                .then(
                    if (fromUser) Modifier
                    else Modifier.border(
                        1.dp, Theme.hairline(),
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = 4.dp, bottomEnd = 16.dp,
                        ),
                    ),
                )
                .padding(horizontal = 13.dp, vertical = 10.dp),
        )
    }
}
