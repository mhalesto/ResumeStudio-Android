package com.resumestudio.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeTemplate
import com.resumestudio.render.ResumePageRasterizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The résumé preview, following `ResumePreviewView`.
 *
 * The toolbar is iOS's, item for item: the scan, the brush, share-and-save, and
 * the overflow. Sharing and saving share a button because they are one intent —
 * handing the PDF to something else — and the overflow is an overflow so the
 * page itself gets the screen.
 */
@Composable
fun ResumePreviewScreen(
    document: ResumeDocument,
    accent: Color,
    onBack: () -> Unit,
    onPickTemplate: (ResumeTemplate) -> Unit,
    onPickAccent: (ResumeAccent) -> Unit,
    onShare: (ResumeDocument) -> Unit,
    onSavePdf: (ResumeDocument) -> Unit,
    onSaveDocx: (ResumeDocument) -> Unit,
    onScan: () -> Unit,
    onSignature: () -> Unit,
    onAttachments: () -> Unit,
    onTrackableLink: () -> Unit,
    onPrint: (ResumeDocument) -> Unit,
    onCopyText: (ResumeDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    var atsSafe by remember { mutableStateOf(false) }
    var showStyle by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var pageIndex by remember { mutableStateOf(0) }

    // `atsSafeVariant`: the classic template, no photo, no attachments. Rendered
    // instead of the document rather than applied to it — the toggle previews an
    // alternative, it does not edit what the user has.
    val rendered = remember(document, atsSafe) {
        if (atsSafe) {
            document.copy(template = ResumeTemplate.CLASSIC, photo = null, photoCrop = null)
        } else {
            document
        }
    }

    val cacheDir = LocalContext.current.cacheDir
    val page by produceState<ResumePageRasterizer.Page?>(null, rendered, pageIndex) {
        value = withContext(Dispatchers.Default) {
            ResumePageRasterizer(cacheDir).rasterize(rendered, pageIndex, widthPx = 1400)
        }
    }

    Column(modifier.fillMaxSize().background(Theme.paper()).systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
            }

            // Two lines: what you are looking at, and in which style. One line
            // ran past the edge and truncated.
            Column(Modifier.weight(1f)) {
                Text("Preview", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
                Text(
                    if (atsSafe) "ATS-safe layout"
                    else "${document.template.wireName.replaceFirstChar { it.uppercase() }} · ${document.accent.title}",
                    fontSize = 10.5.sp, color = Theme.mutedInk(), maxLines = 1,
                )
            }

            IconButton(onClick = onScan) {
                Icon(Icons.Filled.RemoveRedEye, "Recruiter scan", tint = Theme.ink(), modifier = Modifier.size(20.dp))
            }

            // A brush rather than a pencil: the words are edited elsewhere, and
            // this only ever changes how they look.
            Box {
                IconButton(onClick = { showStyle = true }) {
                    Icon(Icons.Filled.Brush, "Template and colour", tint = Theme.ink(), modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showStyle, onDismissRequest = { showStyle = false }) {
                    StyleMenu(document, accent, atsSafe, onPickTemplate, onPickAccent)
                }
            }

            Box {
                IconButton(onClick = { showShare = true }) {
                    Icon(Icons.Filled.Share, "Share and save", tint = Theme.ink(), modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showShare, onDismissRequest = { showShare = false }) {
                    DropdownMenuItem(
                        text = { Text("Share PDF", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Share, null, Modifier.size(17.dp)) },
                        onClick = { showShare = false; onShare(rendered) },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Save PDF", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Description, null, Modifier.size(17.dp)) },
                        onClick = { showShare = false; onSavePdf(rendered) },
                    )
                    DropdownMenuItem(
                        text = { Text("Save editable DOCX", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Description, null, Modifier.size(17.dp)) },
                        onClick = { showShare = false; onSaveDocx(rendered) },
                    )
                }
            }

            Box {
                IconButton(onClick = { showMore = true }) {
                    Icon(Icons.Filled.MoreVert, "More actions", tint = Theme.ink(), modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                    DropdownMenuItem(
                        text = { Text("ATS-safe layout", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Shield, null, Modifier.size(17.dp)) },
                        trailingIcon = {
                            Switch(
                                checked = atsSafe,
                                onCheckedChange = { atsSafe = it; pageIndex = 0 },
                                colors = SwitchDefaults.colors(checkedTrackColor = accent),
                            )
                        },
                        onClick = { atsSafe = !atsSafe; pageIndex = 0 },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (document.signature == null) "Sign document" else "Edit signature",
                                fontSize = 13.sp,
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Draw, null, Modifier.size(17.dp)) },
                        onClick = { showMore = false; onSignature() },
                    )
                    DropdownMenuItem(
                        text = { Text("Add attachments", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.AttachFile, null, Modifier.size(17.dp)) },
                        onClick = { showMore = false; onAttachments() },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Send as trackable link", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Link, null, Modifier.size(17.dp)) },
                        onClick = { showMore = false; onTrackableLink() },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Print", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.Print, null, Modifier.size(17.dp)) },
                        onClick = { showMore = false; onPrint(rendered) },
                    )
                    DropdownMenuItem(
                        text = { Text("Copy résumé text", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, null, Modifier.size(17.dp)) },
                        onClick = { showMore = false; onCopyText(rendered) },
                    )
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Theme.muted())
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            PageSurface(page?.bitmap, accent)
        }

        page?.takeIf { it.pageCount > 1 }?.let { rendered ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageStep(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous page", pageIndex > 0) {
                    pageIndex -= 1
                }
                Text(
                    "Page ${pageIndex + 1} of ${rendered.pageCount}",
                    fontSize = 12.sp, color = Theme.mutedInk(),
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                PageStep(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next page", pageIndex < rendered.pageCount - 1) {
                    pageIndex += 1
                }
            }
        }
    }
}

@Composable
private fun PageSurface(bitmap: Bitmap?, accent: Color) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(595f / 842f).cardSurface(radius = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            CircularProgressIndicator(color = accent)
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Rendered résumé page",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The style popover: a template stepper and the accent row.
 *
 * A stepper rather than a grid, matching iOS — the catalogue is walked one
 * design at a time so each can be judged against the real page rather than as a
 * thumbnail among 140.
 */
@Composable
private fun StyleMenu(
    document: ResumeDocument,
    accent: Color,
    atsSafe: Boolean,
    onPickTemplate: (ResumeTemplate) -> Unit,
    onPickAccent: (ResumeAccent) -> Unit,
) {
    val templates = remember { ResumeTemplate.entries.toList() }
    val position = templates.indexOf(document.template)

    Column(Modifier.width(280.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Template", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Theme.mutedInk())

        Row(verticalAlignment = Alignment.CenterVertically) {
            PageStep(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous template", position > 0 && !atsSafe) {
                onPickTemplate(templates[(position - 1).coerceAtLeast(0)])
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    document.template.wireName.replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                )
                Text(
                    if (atsSafe) "ATS-safe layout is on"
                    else "${position + 1} of ${templates.size}",
                    fontSize = 10.5.sp, color = Theme.mutedInk(),
                )
            }
            PageStep(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next template", position < templates.lastIndex && !atsSafe) {
                onPickTemplate(templates[(position + 1).coerceAtMost(templates.lastIndex)])
            }
        }

        Text("Colour", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Theme.mutedInk())
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResumeAccent.entries.forEach { option ->
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(option.argb))
                        .clickable { onPickAccent(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (option == document.accent) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PageStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (enabled) Theme.card() else Theme.muted())
            .border(1.dp, Theme.hairline(), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, description,
            tint = if (enabled) Theme.ink() else Theme.mutedInk(),
            modifier = Modifier.size(18.dp),
        )
    }
}
