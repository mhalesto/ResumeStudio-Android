package com.resumestudio.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.CoverLetterDocument
import com.resumestudio.model.CoverLetterTemplate
import com.resumestudio.render.CoverLetterPageRasterizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The cover-letter editor, with the page it produces above the fields.
 *
 * The résumé keeps its preview on a separate screen because a résumé is edited
 * section by section. A letter is three paragraphs, so the page is worth its
 * space here — the whole document is visible while it is being written.
 */
@Composable
fun CoverLetterScreen(
    document: CoverLetterDocument,
    accent: Color,
    onEdit: ((CoverLetterDocument) -> CoverLetterDocument) -> Unit,
    onSeedFromResume: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cacheDir = LocalContext.current.cacheDir
    val key = document.template.wireName

    val preview by produceState<Bitmap?>(null, document) {
        value = if (!document.isReadyToPreview) {
            null
        } else {
            withContext(Dispatchers.Default) {
                CoverLetterPageRasterizer(cacheDir).rasterize(document, widthPx = 1000)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Theme.paper()).systemBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, bottom = Theme.footerScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
                }
                Spacer(Modifier.size(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cover letter", style = displayStyle(24), color = Theme.ink())
                    Text(document.template.title, fontSize = 11.sp, color = Theme.mutedInk())
                }
                Text(
                    "Share",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (document.isReadyToPreview) accent else Theme.mutedInk(),
                    modifier = Modifier
                        .clickable(enabled = document.isReadyToPreview, onClick = onShare)
                        .padding(8.dp),
                )
            }
        }

        item { LetterPreview(preview, accent, document.isReadyToPreview) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeading("Letterhead", "${CoverLetterTemplate.entries.size} to choose from.")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoverLetterTemplate.entries.forEach { template ->
                        val selected = template == document.template
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) accent else Theme.card())
                                .clickable { onEdit { it.copy(template = template) } }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                template.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Color.White else Theme.inkSoft(),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().cardSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { SectionHeading("From") }
                    Text(
                        "Use résumé details",
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accent,
                        modifier = Modifier.clickable(onClick = onSeedFromResume),
                    )
                }
                LetterField("Your name", document.senderName, accent, key) { v -> onEdit { it.copy(senderName = v) } }
                LetterField("Headline", document.senderHeadline, accent, key) { v -> onEdit { it.copy(senderHeadline = v) } }
                LetterField("Email", document.senderEmail, accent, key) { v -> onEdit { it.copy(senderEmail = v) } }
                LetterField("Phone", document.senderPhone, accent, key) { v -> onEdit { it.copy(senderPhone = v) } }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().cardSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("To", "Leave blank and the letter simply opens with the greeting.")
                LetterField("Recipient", document.recipientName, accent, key) { v -> onEdit { it.copy(recipientName = v) } }
                LetterField("Their title", document.recipientTitle, accent, key) { v -> onEdit { it.copy(recipientTitle = v) } }
                LetterField("Company", document.companyName, accent, key) { v -> onEdit { it.copy(companyName = v) } }
                LetterField("Role applied for", document.jobTitle, accent, key) { v -> onEdit { it.copy(jobTitle = v) } }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().cardSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("The letter", "Three paragraphs is the shape that gets read.")
                LetterField("Greeting", document.greeting, accent, key) { v -> onEdit { it.copy(greeting = v) } }

                val paragraphs = document.bodyParagraphs.ifEmpty { listOf("", "", "") }
                paragraphs.forEachIndexed { index, paragraph ->
                    LetterField(
                        label = when (index) {
                            0 -> "Why this role"
                            1 -> "What you bring"
                            else -> "Close"
                        },
                        value = paragraph,
                        accent = accent,
                        resetKey = key,
                        minLines = 4,
                    ) { v ->
                        onEdit { doc ->
                            val updated = doc.bodyParagraphs.ifEmpty { listOf("", "", "") }.toMutableList()
                            updated[index] = v
                            doc.copy(bodyParagraphs = updated)
                        }
                    }
                }

                LetterField("Sign-off", document.closing, accent, key) { v -> onEdit { it.copy(closing = v) } }
            }
        }
    }
}

@Composable
private fun LetterPreview(bitmap: Bitmap?, accent: Color, ready: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(595f / 842f)
            .cardSurface(radius = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Rendered cover letter",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            !ready -> Text(
                "Add your name and a paragraph to see the page.",
                fontSize = 12.sp, color = Theme.mutedInk(),
                modifier = Modifier.padding(24.dp),
            )
            else -> CircularProgressIndicator(color = accent)
        }
    }
}

@Composable
private fun LetterField(
    label: String,
    value: String,
    accent: Color,
    resetKey: Any,
    minLines: Int = 1,
    onChange: (String) -> Unit,
) {
    // Same rule as the résumé editor: the field owns its text while on screen,
    // or fast typing races the store and drops characters.
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
