package com.resumestudio.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.resumestudio.android.R
import kotlinx.coroutines.delay

/**
 * The launch screen, following `SplashView.swift`.
 *
 * The mark is the app icon itself, so the tile the launcher animates from and
 * the one that lands here are the same image — anything else reads as two
 * different apps in the half second between them.
 *
 * The field is the same stack iOS builds: the career photograph desaturated
 * under a wash of the launch navy, a diagonal gradient over it, and a spotlight
 * lifted behind where the mark sits.
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var fieldIn by remember { mutableStateOf(false) }

    val field by animateFloatAsState(
        targetValue = if (fieldIn) 1f else 0f,
        animationSpec = tween(520),
        label = "field",
    )
    val mark by animateFloatAsState(
        targetValue = if (fieldIn) 1f else 0f,
        animationSpec = tween(460, delayMillis = 140),
        label = "mark",
    )

    LaunchedEffect(Unit) {
        fieldIn = true
        delay(1_500)
        onFinish()
    }

    Box(Modifier.fillMaxSize().background(Launch), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.splash_career_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = field * 0.82f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Launch.copy(alpha = 0.24f * field)))
        Box(
            Modifier
                .fillMaxSize()
                .alpha(field)
                .background(
                    Brush.linearGradient(
                        listOf(FieldTop.copy(alpha = 0.48f), FieldBottom.copy(alpha = 0.78f)),
                    ),
                ),
        )
        // The spotlight sits slightly above centre, where the mark lands.
        Box(
            Modifier
                .fillMaxSize()
                .alpha(field)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Spotlight.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 900f,
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(mark),
        ) {
            Image(
                // A raster, not the launcher mipmap: on API 26+ that resolves
                // to the adaptive-icon XML, which painterResource cannot load.
                painter = painterResource(R.drawable.splash_mark),
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)),
            )
            Box(Modifier.height(22.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Resume ") }
                    withStyle(SpanStyle(color = SplashAccent)) { append("Studio") }
                },
                style = displayStyle(34),
            )
            Box(Modifier.height(10.dp))
            Text(
                buildAnnotatedString {
                    val muted = Color.White.copy(alpha = 0.55f)
                    withStyle(SpanStyle(color = muted)) { append("CRAFT · ") }
                    withStyle(SpanStyle(color = SplashAccent)) { append("STYLE") }
                    withStyle(SpanStyle(color = muted)) { append(" · EXPORT") }
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
        }
    }
}

// The launch palette, from `BrandPalette.swift`. Held here rather than in Theme
// because these are the brand's own colours, not the workspace tokens — the
// splash is the one screen the user's accent does not tint.
private val Launch = Color(0.10f, 0.12f, 0.22f)
private val FieldTop = Color(0.16f, 0.20f, 0.34f)
private val FieldBottom = Color(0.05f, 0.06f, 0.13f)
private val Spotlight = Color(0.55f, 0.68f, 1.00f)
private val SplashAccent = Color(0.98f, 0.52f, 0.14f)
