package com.resumestudio.android.ui

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resumestudio.android.R
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * The career coach's face, ported from `CareerCoachFace` in `CareerCoachView.swift`.
 *
 * Two stills cross-faded — open eyes and closed — on an irregular 4.2–7.2s
 * interval. The irregularity is the whole trick: a fixed cadence reads as a
 * loading spinner rather than as someone waiting for you to finish typing.
 *
 * Held still when the system's animation scale is zero. Someone who has turned
 * animations off usually did so because movement is a problem, and a face
 * blinking from the corner of the screen is exactly the kind they meant.
 */
@Composable
fun CareerCoachFace(
    accent: Color,
    size: Dp,
    animatesBlink: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    var blinking by remember { mutableStateOf(false) }

    if (animatesBlink && !reduceMotion) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(Random.nextLong(4_200, 7_200))
                blinking = true
                delay(130)
                blinking = false
            }
        }
    }

    val blinkAlpha by animateFloatAsState(
        targetValue = if (blinking) 1f else 0f,
        animationSpec = tween(90),
        label = "blink",
    )

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Theme.card())
            .border(
                width = maxOf(1.5.dp, size * 0.045f),
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.95f), accent.copy(alpha = 0.88f)),
                ),
                shape = CircleShape,
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.career_coach_portrait),
            contentDescription = "Career coach",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(1f - blinkAlpha),
        )
        Image(
            painter = painterResource(R.drawable.career_coach_portrait_blink),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(blinkAlpha),
        )
    }
}
