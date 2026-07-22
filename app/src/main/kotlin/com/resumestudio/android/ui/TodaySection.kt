package com.resumestudio.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.android.R
import com.resumestudio.model.TodayAction
import com.resumestudio.model.TodayActionPriority

/**
 * "What should I do next?" — the ranked queue, following HomeView's `today`.
 *
 * One card should be enough to answer the question, which is why the list is
 * sorted rather than filtered: the top card is the recommendation and the rest
 * are there so it does not feel like a command.
 */
@Composable
fun TodaySection(
    actions: List<TodayAction>,
    accent: Color,
    onOpenApplications: () -> Unit,
    onAction: (TodayAction) -> Unit,
) {
    if (actions.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("TODAY", style = EyebrowStyle, color = accent)
                Text(
                    "What should I do next?",
                    fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Theme.ink(),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Applications",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent,
                modifier = Modifier.clickable(onClick = onOpenApplications),
            )
        }

        actions.forEach { action ->
            TodayActionCard(action, accent) { onAction(action) }
        }
    }
}

@Composable
private fun TodayActionCard(action: TodayAction, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(19.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Theme.card())
            .border(1.dp, Theme.hairline(), shape)
            .clickable(onClick = onClick),
    ) {
        // Depth behind the card: the photograph bled in from the trailing edge
        // and faded out before it reaches the words, over a wash of the accent.
        // The picture is never allowed to compete with the title.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.12f),
                            accent.copy(alpha = 0.02f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(132.dp)
                .fillMaxHeight(),
        ) {
            Image(
                painter = painterResource(action.priority.artwork()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.42f,
                modifier = Modifier.matchParentSize(),
            )
            // Two clear stops before any ink, so it reads as depth at the edge
            // of the card rather than as something printed under the words.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Theme.card(),
                            0.55f to Theme.card().copy(alpha = 0.55f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }

        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .shadow(7.dp, RoundedCornerShape(14.dp), ambientColor = accent, spotColor = accent)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.72f))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(action.icon(), null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    action.title,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = Theme.ink(), lineHeight = 19.sp,
                )
                Text(
                    action.detail,
                    fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 15.sp,
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = Theme.mutedInk(), modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The photograph behind a card, chosen by what the card is asking for.
 *
 * The catalogue is the same set the workspace tiles use, so Today is furnished
 * from the app's own shelf rather than a second look invented for it.
 */
private fun TodayActionPriority.artwork(): Int = when (this) {
    TodayActionPriority.RESUME_READINESS -> R.drawable.workspace_resumes
    TodayActionPriority.SMART_LINK, TodayActionPriority.OUTCOME_REVIEW -> R.drawable.review_room_empty_state
    TodayActionPriority.IMMINENT_INTERVIEW -> R.drawable.workspace_interview
    TodayActionPriority.EXPIRING_HOSTED_WORK -> R.drawable.workspace_import
    else -> R.drawable.workspace_applications
}

private fun TodayAction.icon(): ImageVector = when (priority) {
    TodayActionPriority.RESUME_READINESS -> Icons.Filled.AutoFixHigh
    TodayActionPriority.APPLICATION -> Icons.Filled.Verified
    TodayActionPriority.CAMPAIGN -> Icons.Filled.Groups
    else -> Icons.Filled.Bolt
}
