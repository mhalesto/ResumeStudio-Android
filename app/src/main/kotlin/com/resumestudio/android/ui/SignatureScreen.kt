package com.resumestudio.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.ResumeSignature
import com.resumestudio.model.ResumeSignaturePlacement
import com.resumestudio.model.SignaturePoint
import com.resumestudio.model.SignatureStroke

/**
 * The signature pad, following `ResumeSignatureView`.
 *
 * Strokes are kept as vectors rather than a bitmap of the pad, so the mark is
 * drawn at the PDF's own resolution instead of being a screenshot of a phone
 * scaled up — the difference is visible the moment anyone prints it.
 *
 * Points are normalised to the pad's box on the way out, so the signature does
 * not carry the dimensions of the screen it happened to be drawn on.
 */
@Composable
fun SignatureScreen(
    existing: ResumeSignature?,
    pageCount: Int,
    accent: Color,
    onSave: (ResumeSignature?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strokes = remember {
        existing?.strokes.orEmpty().map { it.points.toMutableList() }.toMutableStateList()
    }
    var current by remember { mutableStateOf<MutableList<SignaturePoint>?>(null) }
    var placement by remember { mutableStateOf(existing?.placement ?: ResumeSignaturePlacement.LOWER_TRAILING) }
    var page by remember { mutableStateOf(existing?.pageIndex ?: 0) }
    var width by remember { mutableStateOf(existing?.widthPoints ?: 128.0) }
    var padSize by remember { mutableStateOf(Offset(1f, 1f)) }

    val hasMark = strokes.any { it.size > 1 } || (current?.size ?: 0) > 1

    Column(
        modifier
            .fillMaxSize()
            .background(Theme.paper())
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
            }
            Spacer(Modifier.size(4.dp))
            Column(Modifier.weight(1f)) {
                Text("Signature", style = displayStyle(24), color = Theme.ink())
                Text("Drawn once, placed on the page at export.", fontSize = 11.sp, color = Theme.mutedInk())
            }
            Text(
                "Save",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (hasMark) accent else Theme.mutedInk(),
                modifier = Modifier
                    .clickable(enabled = hasMark) {
                        val all = (strokes.map { it.toList() } + listOfNotNull(current?.toList()))
                            .filter { it.size > 1 }
                            .map { SignatureStroke(it) }
                        onSave(
                            ResumeSignature(all, page, placement, width).normalized(),
                        )
                    }
                    .padding(8.dp),
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, Theme.hairline(), RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    padSize = Offset(size.width.toFloat(), size.height.toFloat())
                    detectDragGestures(
                        onDragStart = { offset ->
                            current = mutableListOf(offset.normalise(padSize))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            current?.add(change.position.normalise(padSize))
                            // Reassign so the canvas sees the change; a mutable
                            // list mutated in place is not a new state value.
                            current = current?.toMutableList()
                        },
                        onDragEnd = {
                            current?.let { if (it.size > 1) strokes.add(it) }
                            current = null
                        },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val all = strokes.map { it.toList() } + listOfNotNull(current?.toList())
                all.filter { it.size > 1 }.forEach { points ->
                    val path = Path().apply {
                        moveTo(points.first().x * size.width, points.first().y * size.height)
                        points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF1A1A1A),
                        style = Stroke(width = 3.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
            if (!hasMark) {
                Text(
                    "Sign here",
                    fontSize = 13.sp, color = Color(0xFFB4B4B0),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PadAction("Undo", enabled = strokes.isNotEmpty()) { strokes.removeLastOrNull() }
            PadAction("Clear", enabled = hasMark) { strokes.clear(); current = null }
            PadAction("Remove", enabled = existing != null) { onSave(null) }
        }

        Column(Modifier.fillMaxWidth().cardSurface().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeading("Placement", "Where on the page the mark sits.")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ResumeSignaturePlacement.entries.forEach { option ->
                    val selected = option == placement
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) accent else Theme.muted())
                            .clickable { placement = option }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            option.title,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else Theme.inkSoft(),
                        )
                    }
                }
            }

            if (pageCount > 1) {
                // Presented as 1, 2… though it is zero-based inside.
                SectionHeading("Page", "Which of the $pageCount pages it is placed on.")
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    (0 until pageCount).forEach { index ->
                        val selected = index == page
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) accent else Theme.muted())
                                .clickable { page = index }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Color.White else Theme.inkSoft(),
                            )
                        }
                    }
                }
            }

            SectionHeading("Size", "${width.toInt()}pt wide on the page.")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                // The same 88–176 range iOS clamps to.
                listOf(88.0, 112.0, 128.0, 152.0, 176.0).forEach { option ->
                    val selected = option == width
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) accent else Theme.muted())
                            .clickable { width = option }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${option.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else Theme.inkSoft(),
                        )
                    }
                }
            }
        }

        Text(
            "Stored as strokes, not a picture, so it stays sharp at any size. " +
                "It lives in this résumé only — never in shared settings.",
            fontSize = 11.sp, color = Theme.mutedInk(), lineHeight = 16.sp,
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PadAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Theme.card())
            .border(1.dp, Theme.hairline(), RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (enabled) Theme.ink() else Theme.mutedInk(),
        )
    }
}

/** Pad pixels to the 0–1 box the signature is stored in. */
private fun Offset.normalise(pad: Offset) = SignaturePoint(
    x = (x / pad.x.coerceAtLeast(1f)).coerceIn(0f, 1f),
    y = (y / pad.y.coerceAtLeast(1f)).coerceIn(0f, 1f),
)
