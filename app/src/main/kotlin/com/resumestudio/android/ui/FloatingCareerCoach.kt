package com.resumestudio.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The coach's entry point, ported from `FloatingCareerCoachButton`.
 *
 * It floats over the tab content rather than sitting in the feed, which is the
 * whole difference between an assistant that is available and a menu item that
 * has to be found. The green dot says the same thing a messaging app's does —
 * someone is there — and the accent glow is what keeps a 68dp circle from
 * disappearing into a busy dashboard.
 */
@Composable
fun FloatingCareerCoach(
    accent: Color,
    showIntro: Boolean,
    onDismissIntro: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = showIntro,
            enter = slideInHorizontally { it / 2 } + fadeIn(),
            exit = slideOutHorizontally { it / 2 } + fadeOut(),
        ) {
            Row(
                Modifier
                    .shadow(10.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Theme.card())
                    .border(1.dp, Theme.hairline(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column {
                    Text(
                        "Hi, I'm your Career Coach",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Theme.ink(),
                    )
                    Text(
                        "Ask me about work or your job hunt",
                        fontSize = 10.5.sp, color = Theme.mutedInk(),
                    )
                }
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss coach introduction",
                    tint = Theme.mutedInk(),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onDismissIntro),
                )
            }
        }

        Box(
            Modifier
                .size(68.dp)
                // The glow is the accent's, so the coach belongs to whatever
                // palette the résumé is wearing rather than to the app.
                .shadow(18.dp, CircleShape, ambientColor = accent, spotColor = accent)
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            CareerCoachFace(accent = accent, size = 68.dp, animatesBlink = true)

            // Presence, in the corner where a messaging app puts it.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(Theme.card()),
                contentAlignment = Alignment.Center,
            ) {
                Spacer(
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color(red = 0.16f, green = 0.78f, blue = 0.38f)),
                )
            }
        }
    }
}
