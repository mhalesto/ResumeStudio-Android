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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.android.R
import com.resumestudio.model.CareerMomentumMission
import com.resumestudio.model.CareerMomentumPillar
import com.resumestudio.model.CareerMomentumSnapshot

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
 * The week's scoreboard, following HomeView's `campaign` section.
 *
 * Three bars on a plain card read as a form; a tinted panel with the bolt
 * rising out of the corner reads as progress, which is what the numbers are
 * actually about. That comment is iOS's and it is the reason this card has a
 * backdrop rather than a border.
 *
 * The score is progress against goals the user set, not a rating of them.
 */
@Composable
fun MomentumCard(
    snapshot: CareerMomentumSnapshot,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Theme.card())
            .border(1.dp, Theme.hairline(), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        // The backdrop: accent falling away down the panel, with the mark the
        // score is about lifted out of the bottom-right corner.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to accent.copy(alpha = 0.16f),
                        0.5f to accent.copy(alpha = 0.05f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Icon(
            if (snapshot.isComplete) Icons.Filled.Verified else Icons.Filled.Bolt,
            contentDescription = null,
            tint = accent.copy(alpha = 0.07f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(132.dp)
                .offset(x = 34.dp, y = 26.dp)
                .rotate(-8f),
        )

        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(5.dp))
                Text("CAREER MOMENTUM", style = EyebrowStyle, color = accent)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${snapshot.score}",
                        fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Theme.ink(),
                    )
                    Text(
                        "/100",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Theme.mutedInk(),
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    snapshot.tier.title,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                )
                Spacer(Modifier.weight(1f))
                if (snapshot.rhythmWeeks > 0) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = accent, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.size(3.dp))
                    Text(
                        "${snapshot.rhythmWeeks}-week rhythm",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent,
                    )
                    Spacer(Modifier.size(6.dp))
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = Theme.mutedInk(), modifier = Modifier.size(18.dp),
                )
            }

            snapshot.missions.forEach { mission -> MissionRow(mission, accent) }

            snapshot.nextMission?.let { next ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, tint = accent, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("NEXT CHARGE", style = EyebrowStyle, color = accent)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        next.pillar.title,
                        fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Theme.ink(),
                    )
                    Spacer(Modifier.weight(1f))
                    snapshot.nextMilestone?.let { milestone ->
                        Text(
                            "${snapshot.pointsToNextMilestone} points to $milestone",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Theme.mutedInk(),
                        )
                    }
                }
            }
        }
    }
}

/** Green when the goal is met, accent while it is still being worked towards. */
@Composable
private fun MissionRow(mission: CareerMomentumMission, accent: Color) {
    val done = Color(0xFF2AA84F)
    val bar = if (mission.isComplete) done else accent

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(mission.pillar.icon(), null, tint = Theme.ink(), modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(6.dp))
            Text(
                mission.pillar.title,
                fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${mission.cappedValue}/${mission.goal}",
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (mission.isComplete) done else Theme.mutedInk(),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(Theme.muted()),
        ) {
            val fraction = mission.progress.coerceIn(0f, 1f)
            if (fraction > 0f) {
                Spacer(Modifier.weight(fraction).height(5.dp).clip(CircleShape).background(bar))
            }
            if (fraction < 1f) Spacer(Modifier.weight(1f - fraction))
        }
    }
}

/** The nearest Material equivalents of the SF Symbols iOS uses per pillar. */
private fun CareerMomentumPillar.icon(): ImageVector = when (this) {
    CareerMomentumPillar.OPPORTUNITIES -> Icons.Filled.GpsFixed
    CareerMomentumPillar.RELATIONSHIPS -> Icons.Filled.Groups
    CareerMomentumPillar.PRACTICE -> Icons.Filled.GraphicEq
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
