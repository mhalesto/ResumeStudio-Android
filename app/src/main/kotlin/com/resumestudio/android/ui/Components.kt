package com.resumestudio.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
 * A raised card on [Theme.paper]: warm fill, hairline edge, soft lift.
 *
 * The port of `cardSurface()`. Shadow first, then clip, then fill — reversing
 * that order clips the shadow away and the card sits flat on the page.
 */
@Composable
fun Modifier.cardSurface(radius: androidx.compose.ui.unit.Dp = Theme.cardRadius): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(elevation = 8.dp, shape = shape, ambientColor = Theme.ink(), spotColor = Theme.ink())
        .clip(shape)
        .background(Theme.card(), shape)
        .border(1.dp, Theme.hairline(), shape)
}

/** Title over an explanatory line — the heading every Home section opens with. */
@Composable
fun SectionHeading(title: String, subtitle: String? = null) {
    Column {
        Text(
            title,
            style = displayStyle(22),
            color = Theme.ink(),
        )
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = Theme.mutedInk(),
                lineHeight = 18.sp,
            )
        }
    }
}

/** The accent dot and wide-tracked label the hero opens with. */
@Composable
fun Eyebrow(text: String, color: Color, dot: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) {
            Spacer(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.size(8.dp))
        }
        Text(text.uppercase(), style = EyebrowStyle, color = color, maxLines = 1)
    }
}

/** A labelled figure, as the hero's design-library shortcuts use. */
@Composable
fun StatTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.tileRadius))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(Theme.tileRadius))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = accent)
        Text(label, fontSize = 11.sp, color = Theme.heroMutedInk, maxLines = 1)
    }
}

/**
 * The hero's completion track.
 *
 * Drawn with weights rather than a measured width so it needs no layout pass —
 * at 5dp tall a `GeometryReader` equivalent would be more machinery than the
 * bar is worth.
 */
@Composable
fun CompletionBar(fraction: Float, accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f)),
    ) {
        if (fraction > 0f) {
            Spacer(
                Modifier
                    .weight(fraction.coerceIn(0.001f, 1f))
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        if (fraction < 1f) Spacer(Modifier.weight((1f - fraction).coerceIn(0.001f, 1f)))
    }
}
