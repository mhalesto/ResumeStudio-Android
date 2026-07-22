package com.resumestudio.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.android.R

/**
 * A workspace tile with the app's own artwork behind it.
 *
 * The image is the same JPEG iOS uses, scrimmed rather than dimmed: a flat
 * overlay would grey the photograph, whereas a gradient from the bottom keeps
 * the top of the image readable while guaranteeing contrast under the label.
 */
@Composable
fun ArtworkCard(
    title: String,
    detail: String,
    artwork: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier
            .height(112.dp)
            .clip(RoundedCornerShape(Theme.cardRadius))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Image(
            painter = painterResource(artwork),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        0.45f to Color.Black.copy(alpha = 0.45f),
                        1f to Color.Black.copy(alpha = 0.82f),
                    ),
                ),
        )
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(detail, fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
        }
    }
}

/**
 * The career coach's entry point.
 *
 * The face is the affordance — it is doing the work an icon would, and it is
 * why this card is worth its height rather than being another row of text.
 */
@Composable
fun CareerCoachCard(accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .cardSurface()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CareerCoachFace(accent = accent, size = 54.dp, animatesBlink = true)
        Column(Modifier.weight(1f)) {
            Text("Career coach", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
            Spacer(Modifier.height(2.dp))
            Text(
                "Talk through a role, a rewrite, or what to say in the interview.",
                fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 15.sp,
            )
        }
    }
}

/**
 * The three pillars of the weekly campaign, following `CareerMomentumPillar`.
 *
 * Reads zero because Android has no application, contact or practice store yet.
 * That is stated rather than dressed up — a card showing invented progress is
 * worse than one admitting there is nothing to show, because the whole point of
 * the pillar is that the number is real.
 */
@Composable
fun MomentumCard(
    accent: Color,
    opportunities: Pair<Int, Int>,
    relationships: Pair<Int, Int>,
    practice: Pair<Int, Int>,
    modifier: Modifier = Modifier,
) {
    val pillars = listOf(
        Triple("Opportunities", opportunities, "captured this week"),
        Triple("Relationships", relationships, "people contacted"),
        Triple("Practice", practice, "sessions run"),
    )
    val total = pillars.sumOf { it.second.first }

    Column(
        modifier.fillMaxWidth().cardSurface().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Career momentum", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
                Text(
                    if (total == 0) "Nothing logged this week yet."
                    else "$total of ${pillars.sumOf { it.second.second }} this week.",
                    fontSize = 11.sp, color = Theme.mutedInk(),
                )
            }
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$total", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }

        pillars.forEach { (title, progress, unit) ->
            val (done, goal) = progress
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, fontSize = 12.sp, color = Theme.inkSoft())
                    Text("$done / $goal $unit", fontSize = 11.sp, color = Theme.mutedInk())
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Theme.muted()),
                ) {
                    val fraction = (done.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
                    if (fraction > 0f) {
                        Spacer(Modifier.weight(fraction).height(5.dp).clip(CircleShape).background(accent))
                    }
                    if (fraction < 1f) Spacer(Modifier.weight(1f - fraction))
                }
            }
        }
    }
}

/**
 * Tracked links, following `SmartLink`.
 *
 * A smart link is a résumé handed out as a URL whose opens can be seen, so the
 * count that matters is views, not links — one link opened four times is the
 * signal, four links never opened is not.
 */
@Composable
fun SmartLinksCard(
    accent: Color,
    linkCount: Int,
    viewCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().cardSurface().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Link, null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Tracked links", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
            Text(
                if (linkCount == 0) "Share a résumé as a link and see when it is opened."
                else "$linkCount link${if (linkCount == 1) "" else "s"} · $viewCount view${if (viewCount == 1) "" else "s"}",
                fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 15.sp,
            )
        }
    }
}

/** Section artwork used as a wide banner, the way the intelligence hub opens. */
@Composable
fun HeroBanner(
    title: String,
    subtitle: String,
    artwork: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(Theme.cardRadius))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Image(
            painter = painterResource(artwork),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.80f),
                    1f to Color.Black.copy(alpha = 0.25f),
                ),
            ),
        )
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = displayStyle(20), color = Color.White)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.82f), lineHeight = 15.sp)
        }
    }
}
