package com.resumestudio.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.JobApplication
import com.resumestudio.model.JobApplicationStatus

/**
 * The pipeline, following `ApplicationCommandCenterView`.
 *
 * Capturing an opportunity here is what makes the campaign's Opportunities
 * pillar move, so the add form is on the screen rather than behind a menu — the
 * dashboard is asking for this action by name.
 */
@Composable
fun ApplicationsScreen(
    applications: List<JobApplication>,
    accent: Color,
    onAdd: (JobApplication) -> Unit,
    onSetStatus: (String, JobApplicationStatus) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var role by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<JobApplicationStatus?>(null) }

    val shown = applications.filter { filter == null || it.status == filter }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Theme.paper()),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 20.dp, bottom = Theme.footerScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { CommandCentreHero(accent) }

        item { Metrics(applications, accent) }

        item {
            // The pipeline as filters, with counts. A stage showing zero is
            // worth seeing — an empty Offers column is the whole reason someone
            // opens this screen.
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                StageChip("All", null, applications.size, filter == null, accent) { filter = null }
                JobApplicationStatus.entries.forEach { status ->
                    StageChip(
                        status.title,
                        status.icon(),
                        applications.count { it.status == status },
                        filter == status,
                        accent,
                    ) { filter = if (filter == status) null else status }
                }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().cardSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionHeading("Capture an opportunity")
                CaptureField("Role", role, accent) { role = it }
                CaptureField("Company", company, accent) { company = it }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (role.isBlank() && company.isBlank()) Theme.muted() else accent)
                        .clickable(enabled = role.isNotBlank() || company.isNotBlank()) {
                            onAdd(JobApplication(role = role.trim(), company = company.trim()))
                            role = ""
                            company = ""
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Add to pipeline",
                        color = if (role.isBlank() && company.isBlank()) Theme.mutedInk() else Color.White,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    )
                }
            }
        }

        items(shown, key = { it.id }) { application ->
            ApplicationRow(application, accent, onSetStatus, onRemove)
        }
    }
}

/**
 * The command centre's opening, following `pipelineHero`.
 *
 * A diagonal gradient with the accent blooming out of the top-right corner —
 * the orb is what stops a dark slab reading as a header bar, and it is the
 * user's own accent so the screen belongs to their palette.
 */
@Composable
private fun CommandCentreHero(accent: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Theme.heroTop(), Theme.heroBottom()),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
    ) {
        // iOS blurs a 230pt circle by 40. A radial gradient is the same bloom
        // without needing API 31's blur, so it looks right on every device the
        // app supports rather than only the recent ones.
        Box(
            Modifier
                .size(230.dp)
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.34f), Color.Transparent),
                    ),
                    shape = CircleShape,
                ),
        )

        Column(
            Modifier.align(Alignment.BottomStart).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("APPLICATION COMMAND CENTER", style = EyebrowStyle, color = accent)
            Text(
                "Know what is moving\nand what needs you.",
                style = displayStyle(28), color = Theme.heroInk, lineHeight = 34.sp,
            )
            Text(
                "Every opportunity, document, interview and contact in one connected timeline.",
                fontSize = 12.5.sp, color = Theme.heroMutedInk, lineHeight = 17.sp,
            )
        }
    }
}

/** Three cards under the hero, not tiles inside it — the way iOS lays them out. */
@Composable
private fun Metrics(applications: List<JobApplication>, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Metric("${applications.size}", "Tracked", Color(0xFF3B82F6), Modifier.weight(1f))
        Metric(
            "${applications.count { it.status == JobApplicationStatus.INTERVIEW }}",
            "Interview", accent, Modifier.weight(1f),
        )
        Metric(
            "${applications.count { it.status == JobApplicationStatus.OFFER }}",
            "Offers", Color(0xFF2AA84F), Modifier.weight(1f),
        )
    }
}

@Composable
private fun Metric(value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.cardSurface(radius = 17.dp).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = tint)
        Text(label, fontSize = 11.sp, color = Theme.mutedInk())
    }
}

@Composable
private fun StageChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    count: Int,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) accent else Theme.muted())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let {
            Icon(
                it, null,
                tint = if (selected) Color.White else Theme.ink(),
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            label,
            fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Theme.ink(),
        )
        Text(
            "$count",
            fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
            color = (if (selected) Color.White else Theme.ink()).copy(alpha = 0.7f),
        )
    }
}

/** The icons iOS gives each stage. */
private fun JobApplicationStatus.icon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    JobApplicationStatus.SAVED -> Icons.Filled.BookmarkBorder
    JobApplicationStatus.APPLIED -> Icons.AutoMirrored.Filled.Send
    JobApplicationStatus.INTERVIEW -> Icons.Filled.Groups
    JobApplicationStatus.OFFER -> Icons.Filled.Star
    JobApplicationStatus.REJECTED -> Icons.Filled.Close
}

@Composable
private fun ApplicationRow(
    application: JobApplication,
    accent: Color,
    onSetStatus: (String, JobApplicationStatus) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().cardSurface().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    application.role.ifBlank { "Untitled role" },
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                )
                if (application.company.isNotBlank()) {
                    Text(application.company, fontSize = 11.5.sp, color = Theme.mutedInk())
                }
            }
            Icon(
                Icons.Filled.Close, "Remove",
                tint = Theme.mutedInk(),
                modifier = Modifier.size(16.dp).clickable { onRemove(application.id) },
            )
        }

        // The pipeline as a row of stages: tapping one is how a status changes,
        // which keeps the common edit to a single tap.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            JobApplicationStatus.entries.forEach { status ->
                val selected = status == application.status
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) accent else Theme.muted())
                        .clickable { onSetStatus(application.id, status) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        status.title,
                        fontSize = 9.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else Theme.mutedInk(),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureField(label: String, value: String, accent: Color, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Theme.ink()),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = Theme.hairline(),
            focusedLabelColor = accent,
            unfocusedLabelColor = Theme.mutedInk(),
            cursorColor = accent,
        ),
    )
}
