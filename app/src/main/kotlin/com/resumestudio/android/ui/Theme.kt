package com.resumestudio.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.ResumeAccent

/**
 * The app's surface and type tokens, ported from `Theme.swift`.
 *
 * The light surfaces use a soft cream-white palette so the workspace feels warm
 * without reading as beige; the dark set keeps the deeper navy treatment.
 *
 * Deliberately *not* here: the brand colour. The UI is tinted with the user's
 * chosen [ResumeAccent], so all of their accents drive the design rather than
 * one hard-coded orange. Read it from [LocalAccent].
 *
 * Values are the same 0–1 components the Swift file declares, converted once
 * here rather than being re-typed as hex — the two palettes have to stay
 * comparable by eye across the two apps.
 */
object Theme {

    // --- surfaces ---------------------------------------------------------

    /** The page. A dreamy cream-white, rather than a bright or cool system white. */
    val paper = Adaptive(rgb(0.965f, 0.941f, 0.910f), rgb(0.045f, 0.061f, 0.086f))

    /** Raised cards stay gently lighter than the page without becoming stark white. */
    val card = Adaptive(rgb(0.988f, 0.973f, 0.945f), rgb(0.085f, 0.105f, 0.138f))

    /** Recessed surfaces — the privacy note, progress tracks. */
    val muted = Adaptive(rgb(0.918f, 0.882f, 0.839f), rgb(0.128f, 0.152f, 0.190f))

    val hairline = Adaptive(rgb(0.871f, 0.827f, 0.773f), Color.White.copy(alpha = 0.08f))

    // --- type -------------------------------------------------------------

    val ink = Adaptive(rgb(0.038f, 0.069f, 0.121f), rgb(0.956f, 0.946f, 0.931f))
    val inkSoft = Adaptive(rgb(0.167f, 0.201f, 0.256f), rgb(0.782f, 0.767f, 0.741f))
    val mutedInk = Adaptive(rgb(0.363f, 0.391f, 0.435f), rgb(0.616f, 0.594f, 0.557f))

    // --- hero -------------------------------------------------------------

    /**
     * The hero card stays dark in both appearances: in light mode a bold dark
     * slab on warm paper, in dark mode a raised card.
     *
     * It has to lift *away* from the page in dark mode, so the gradient is a
     * step lighter there — a card darker than its background reads as a hole.
     */
    val heroTop = Adaptive(rgb(0.085f, 0.105f, 0.138f), rgb(0.118f, 0.140f, 0.180f))
    val heroBottom = Adaptive(rgb(0.031f, 0.044f, 0.067f), rgb(0.063f, 0.079f, 0.107f))
    val heroInk = rgb(0.956f, 0.946f, 0.931f)
    val heroMutedInk = rgb(0.616f, 0.594f, 0.557f)

    // --- geometry ---------------------------------------------------------

    val heroRadius = 28.dp
    val cardRadius = 22.dp
    val tileRadius = 16.dp

    val screenPadding = PaddingValues(horizontal = 20.dp)

    /**
     * Space after the final row in every tab-owned scroll view.
     *
     * The floating coach is 68dp plus its 18dp inset; the remainder keeps the
     * last card visibly clear of it at the end of a scroll rather than tucked
     * underneath. iOS carries the same constant for the same reason.
     */
    val footerScrollClearance = 104.dp
    val sectionSpacing = 30.dp

    /** Resolves per appearance, so tokens work in light and dark. */
    class Adaptive(private val light: Color, private val dark: Color) {
        @Composable
        @ReadOnlyComposable
        operator fun invoke(): Color = if (isSystemInDarkTheme()) dark else light
    }

    private fun rgb(r: Float, g: Float, b: Float) = Color(r, g, b)
}

/**
 * The display face.
 *
 * iOS sets `.serif`, which resolves to New York — the prototype's intended
 * typeface. [FontFamily.Serif] is the nearest guaranteed equivalent here; a
 * bundled face is what would actually match, and is the same open question the
 * PDF renderer has.
 */
val DisplayFamily = FontFamily.Serif

/** Small uppercase label with wide tracking, as used for the greeting. */
val EyebrowStyle = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.4.sp,
)

fun displayStyle(size: Int) = TextStyle(
    fontFamily = DisplayFamily,
    fontSize = size.sp,
    fontWeight = FontWeight.Normal,
)

/** The user's chosen accent, which tints the whole UI. */
val LocalAccent = staticCompositionLocalOf { Color(ResumeAccent.ORANGE.argb) }

@Composable
fun ResumeStudioTheme(
    accent: ResumeAccent = ResumeAccent.ORANGE,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val accentColor = Color(accent.argb)

    // Material's scheme is kept in step with the tokens so any stock component
    // that slips in does not arrive wearing default purple.
    val scheme = if (dark) {
        darkColorScheme(
            primary = accentColor,
            background = Theme.paper(),
            surface = Theme.card(),
            onBackground = Theme.ink(),
            onSurface = Theme.ink(),
            surfaceVariant = Theme.muted(),
            outline = Theme.hairline(),
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            background = Theme.paper(),
            surface = Theme.card(),
            onBackground = Theme.ink(),
            onSurface = Theme.ink(),
            surfaceVariant = Theme.muted(),
            outline = Theme.hairline(),
        )
    }

    CompositionLocalProvider(LocalAccent provides accentColor) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}
