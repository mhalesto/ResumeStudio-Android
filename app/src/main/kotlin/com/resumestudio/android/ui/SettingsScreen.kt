package com.resumestudio.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeFontChoice
import com.resumestudio.model.ResumePaperSize

/**
 * Settings, following the section order of `SettingsView.swift`.
 *
 * The rows that lead somewhere Android has not built yet are shown as
 * unavailable rather than hidden. Hiding them would make the two apps look like
 * different products; marking them says the same thing honestly and keeps the
 * shape of the screen recognisable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    document: ResumeDocument,
    accent: Color,
    onPickAccent: (ResumeAccent) -> Unit,
    onPickPaper: (ResumePaperSize) -> Unit,
    onPickFont: (ResumeFontChoice) -> Unit,
    onOpenUrl: (String) -> Unit,
    versionName: String,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Theme.paper()),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 20.dp, bottom = Theme.footerScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { Text("Settings", style = displayStyle(30), color = Theme.ink()) }

        item {
            SettingsSection("Appearance", "Tints the app and drives the exported PDF.") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumeAccent.entries.forEach { entry ->
                        Box(
                            Modifier
                                .padding(vertical = 4.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(entry.argb))
                                .clickable { onPickAccent(entry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (entry == document.accent) {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        document.accent.title,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                    )
                    if (document.accent.isPremium) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            document.accent.tier.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 10.sp, color = accent,
                        )
                    }
                }
            }
        }

        item {
            SettingsSection("Page", "Applies to every export.") {
                ChoiceRow(
                    label = "Paper size",
                    options = ResumePaperSize.entries.map { it to it.name },
                    selected = document.layout.paperSize,
                    accent = accent,
                    onPick = onPickPaper,
                )
                Spacer(Modifier.height(10.dp))
                ChoiceRow(
                    label = "Typeface",
                    options = ResumeFontChoice.entries.map { it to it.title },
                    selected = document.layout.fontChoice,
                    accent = accent,
                    onPick = onPickFont,
                )
            }
        }

        item {
            SettingsSection("ResumeStudio") {
                SettingsRow("Account and backup", Icons.Filled.Person, available = false)
                SettingsRow("Saved AI work", Icons.Filled.AutoAwesome, available = false)
                SettingsRow("Plans and purchases", Icons.Filled.AutoAwesome, available = false)
            }
        }

        item {
            SettingsSection(
                "Sync",
                // Worth stating plainly: iOS syncs through iCloud, which has no
                // Android equivalent, so this will arrive as Firestore rather
                // than as the same feature.
                "iCloud has no Android equivalent. Cloud sync will arrive separately.",
            ) {
                SettingsRow("Sync documents", Icons.Filled.CloudOff, available = false)
                SettingsRow("Language", Icons.Filled.Language, detail = "System")
            }
        }

        item {
            SettingsSection("Integrations") {
                SettingsRow("Application Answer Vault", Icons.Filled.Extension, available = false)
                SettingsRow("Calendar, Mail and Share", Icons.Filled.Extension, available = false)
            }
        }

        item {
            SettingsSection("Privacy") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Theme.tileRadius))
                        .background(Theme.muted())
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Theme.mutedInk(), modifier = Modifier.size(15.dp))
                    Text(
                        "Your résumé, applications and settings are stored on this device only. " +
                            "Nothing is uploaded unless you share it yourself.",
                        fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 16.sp,
                    )
                }
            }
        }

        item {
            SettingsSection("Help and legal") {
                SettingsRow("ResumeStudio website", Icons.Filled.Public, external = true) {
                    onOpenUrl("https://resumestudio.app")
                }
                SettingsRow("Support", Icons.Filled.HelpOutline, external = true) {
                    onOpenUrl("https://resumestudio.app/support")
                }
                SettingsRow("Privacy policy", Icons.Filled.Policy, external = true) {
                    onOpenUrl("https://resumestudio.app/privacy")
                }
                SettingsRow("Send feedback", Icons.Filled.MailOutline, external = true) {
                    onOpenUrl("mailto:mbanjwa.hg@gmail.com?subject=ResumeStudio%20for%20Android")
                }
            }
        }

        item {
            Text(
                "ResumeStudio for Android $versionName",
                fontSize = 11.sp, color = Theme.mutedInk(),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title, subtitle)
        Column(Modifier.fillMaxWidth().cardSurface().padding(14.dp)) { content() }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector,
    detail: String? = null,
    available: Boolean = true,
    external: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null && available) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon, null,
            tint = if (available) Theme.inkSoft() else Theme.mutedInk(),
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            fontSize = 13.sp,
            color = if (available) Theme.ink() else Theme.mutedInk(),
            modifier = Modifier.weight(1f),
        )
        if (!available) {
            Text("Not yet on Android", fontSize = 10.sp, color = Theme.mutedInk())
        } else {
            detail?.let { Text(it, fontSize = 11.sp, color = Theme.mutedInk()) }
            Icon(
                if (external) Icons.AutoMirrored.Filled.OpenInNew
                else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null, tint = Theme.mutedInk(), modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    accent: Color,
    onPick: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontSize = 12.sp, color = Theme.mutedInk())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.forEach { (value, title) ->
                val isSelected = value == selected
                Box(
                    Modifier
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) accent else Theme.muted())
                        .clickable { onPick(value) }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Text(
                        title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Theme.inkSoft(),
                    )
                }
            }
        }
    }
}
