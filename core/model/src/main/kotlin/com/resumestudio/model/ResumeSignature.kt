package com.resumestudio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A hand-drawn signature placed over one résumé page at export time,
 * mirroring `ResumeSignature.swift`.
 *
 * Vectors are stored rather than a screenshot, so the mark stays crisp in the
 * PDF at any size and can be reopened for another stroke later. The page is
 * zero-based internally; the editor presents it as the familiar 1, 2…
 *
 * **Not interchangeable with the iOS signature.** iOS stores a PencilKit
 * `PKDrawing` blob, which has no reader outside Apple's frameworks. This is the
 * same idea in a format both platforms could read — normalised points — so the
 * two are compatible in intent but not yet in bytes. Anything syncing documents
 * has to treat the signature as platform-local until iOS writes this shape too.
 */
@Serializable
data class ResumeSignature(
    val strokes: List<SignatureStroke> = emptyList(),
    val pageIndex: Int = 0,
    val placement: ResumeSignaturePlacement = ResumeSignaturePlacement.LOWER_TRAILING,
    val widthPoints: Double = 128.0,
) {
    val isEmpty: Boolean get() = strokes.none { it.points.size > 1 }

    /** The same clamps iOS applies on decode, so a hand-edited file cannot break the page. */
    fun normalized(): ResumeSignature = copy(
        pageIndex = pageIndex.coerceAtLeast(0),
        widthPoints = widthPoints.coerceIn(88.0, 176.0),
    )
}

/**
 * One continuous mark, as points normalised to the 0–1 box the pad was drawn in.
 *
 * Normalised rather than in pixels because the pad is whatever size the phone
 * gives it and the PDF is 595pt wide — storing device pixels would tie the
 * signature to the screen it was drawn on.
 */
@Serializable
data class SignatureStroke(
    val points: List<SignaturePoint> = emptyList(),
)

@Serializable
data class SignaturePoint(val x: Float, val y: Float)

@Serializable
enum class ResumeSignaturePlacement(val title: String) {
    @SerialName("lowerLeading") LOWER_LEADING("Left"),
    @SerialName("lowerCenter") LOWER_CENTER("Centre"),
    @SerialName("lowerTrailing") LOWER_TRAILING("Right"),
}
