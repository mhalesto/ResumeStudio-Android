package com.resumestudio.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.RecruiterScan
import com.resumestudio.model.ResumeDocument

/**
 * The seven-second scan, following `RecruiterScanView`.
 *
 * The score is deliberately framed as seconds rather than as a mark out of a
 * hundred wherever there is room to say it: "4.8 of 7.4 seconds land on
 * something useful" is a fact about the page, where "65%" invites the reader to
 * hear a judgement about their career.
 */
@Composable
fun RecruiterScanScreen(
    document: ResumeDocument,
    accent: Color,
    onOpenEditor: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var strictness by remember { mutableStateOf(RecruiterScan.Strictness.MEDIUM) }
    val report = remember(document, strictness) { RecruiterScan.analyze(document, strictness) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Theme.paper()).systemBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, bottom = Theme.footerScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
                }
                Spacer(Modifier.size(4.dp))
                Column {
                    Text("Recruiter scan", style = displayStyle(24), color = Theme.ink())
                    Text("The first seven seconds.", fontSize = 11.sp, color = Theme.mutedInk())
                }
            }
        }

        item { ScoreHero(report, accent) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeading("How hard to mark", strictness.detail)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    RecruiterScan.Strictness.entries.forEach { level ->
                        val selected = level == strictness
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) accent else Theme.muted())
                                .clickable { strictness = level }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                level.title,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Color.White else Theme.inkSoft(),
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeading("What the scan found", "${report.findings.size} checks against the study's pass.") }

        items(report.findings, key = { it.id }) { finding ->
            FindingRow(finding, accent, onOpenEditor)
        }

        if (report.missedFacts.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Theme.tileRadius))
                        .background(Theme.muted())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Missed in the pass",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                    )
                    report.missedFacts.forEach {
                        Text("· $it", fontSize = 11.5.sp, color = Theme.mutedInk())
                    }
                }
            }
        }

        item {
            Text(
                "This measures how much of a seven-second pass lands on real information. " +
                    "It is not a prediction of hiring outcomes.",
                fontSize = 10.5.sp, color = Theme.mutedInk(), lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun ScoreHero(report: RecruiterScan.Report, accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.heroRadius))
            .background(Brush.verticalGradient(listOf(Theme.heroTop(), Theme.heroBottom())))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("SEVEN-SECOND SCAN", style = EyebrowStyle, color = accent)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${report.score}",
                fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Theme.heroInk,
            )
            Text(
                "/100",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Theme.heroMutedInk,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                report.verdict,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.heroInk,
            )
        }
        // Said in seconds as well as points: the seconds are a fact about the
        // page, the score invites a judgement about the person.
        Text(
            "%.1f of %.1f seconds land on something a reader can use.".format(
                report.usefulSeconds, RecruiterScan.SCAN_SECONDS,
            ),
            fontSize = 12.sp, color = Theme.heroMutedInk,
        )
        CompletionBar((report.score / 100f).coerceIn(0f, 1f), accent)
    }
}

@Composable
private fun FindingRow(
    finding: RecruiterScan.Finding,
    accent: Color,
    onOpenEditor: () -> Unit,
) {
    val tint = when (finding.severity) {
        RecruiterScan.Severity.PASS -> Color(0xFF2AA84F)
        RecruiterScan.Severity.WARNING -> Color(0xFFD98A28)
        RecruiterScan.Severity.ACTION -> Color(0xFFCC3B34)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .cardSurface()
            .then(if (finding.section != null) Modifier.clickable(onClick = onOpenEditor) else Modifier)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                when (finding.severity) {
                    RecruiterScan.Severity.PASS -> Icons.Filled.Check
                    RecruiterScan.Severity.WARNING -> Icons.Filled.PriorityHigh
                    RecruiterScan.Severity.ACTION -> Icons.Filled.Close
                },
                null, tint = tint, modifier = Modifier.size(14.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    finding.title,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%.1fs".format(finding.gazeSeconds),
                    fontSize = 10.sp, color = Theme.mutedInk(),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(finding.detail, fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 15.sp)
        }
    }
}
