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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

    val open = applications.count { it.status.isOpen }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Theme.paper()),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 20.dp, bottom = Theme.footerScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("Applications", style = displayStyle(30), color = Theme.ink())
                Spacer(Modifier.height(4.dp))
                Text(
                    if (applications.isEmpty()) {
                        "Track an opportunity and it counts towards this week's campaign."
                    } else {
                        "$open open of ${applications.size} tracked."
                    },
                    fontSize = 13.sp, color = Theme.mutedInk(), lineHeight = 18.sp,
                )
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

        items(applications, key = { it.id }) { application ->
            ApplicationRow(application, accent, onSetStatus, onRemove)
        }
    }
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
