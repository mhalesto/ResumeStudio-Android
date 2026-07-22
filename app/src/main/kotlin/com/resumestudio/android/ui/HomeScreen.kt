package com.resumestudio.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumestudio.android.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.resumestudio.model.CareerMomentumSnapshot
import com.resumestudio.model.CoverLetterDocument
import androidx.compose.material.icons.filled.AutoAwesome
import com.resumestudio.model.CoverLetterTemplate
import com.resumestudio.model.ResumeAccent
import com.resumestudio.render.CoverLetterPdfRenderer
import com.resumestudio.render.TemplateThumbnailCache
import com.resumestudio.render.family
import com.resumestudio.model.TodayAction
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeTemplate
import com.resumestudio.model.TemplateCatalogue

/**
 * The dashboard, following `HomeView.swift`.
 *
 * Same running order as iOS — hero, start creating, workspace, templates,
 * recently edited, privacy — because the order is the argument the screen
 * makes: what you were doing, then what you could start, then where everything
 * lives. Sections iOS has that have no Android data behind them yet (the Today
 * queue, the career campaign) are left out rather than faked; an empty shell
 * would imply the feature exists.
 */
@Composable
fun HomeScreen(
    document: ResumeDocument,
    accent: Color,
    resumeCount: Int,
    applicationCount: Int,
    todayActions: List<TodayAction>,
    momentum: CareerMomentumSnapshot,
    coverLetter: CoverLetterDocument,
    thumbnails: TemplateThumbnailCache,
    onOpenGallery: () -> Unit,
    onOpenApplications: () -> Unit,
    onOpenCoverLetter: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCoach: () -> Unit,
    onImport: () -> Unit,
    onUnavailable: (String) -> Unit,
    onTodayAction: (TodayAction) -> Unit,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onOpenTemplate: (ResumeTemplate) -> Unit,
    onLoadExample: () -> Unit,
    onStartBlank: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().background(Theme.paper()),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 12.dp, bottom = Theme.footerScrollClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(Theme.sectionSpacing),
    ) {
        item { Hero(document, accent, onPreview, onShare, onOpenGallery, onOpenCoverLetter, onOpenSettings) }
        item { TodaySection(todayActions, accent, onOpenApplications, onTodayAction) }
        item { StartCreating(accent, onLoadExample, onStartBlank) }
        item { CareerCampaign(accent, momentum, onOpenApplications) }
        item { AICareerTools(accent, onOpenCoach, onUnavailable) }
        item {
            CareerWorkspace(
                accent, resumeCount, applicationCount,
                onOpenGallery, onOpenApplications, onOpenCoverLetter, onOpenLibrary, onOpenScan,
                onImport, onUnavailable,
            )
        }
        item { Templates(accent, document, thumbnails, onOpenGallery, onOpenTemplate) }
        item { CoverLetterShelf(accent, coverLetter, onOpenCoverLetter) }
        item { RecentlyEdited(document, accent, onEdit) }
        item { PrivacyNote() }
    }
}

// --- hero -------------------------------------------------------------------

@Composable
private fun Hero(
    document: ResumeDocument,
    accent: Color,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCoverLetter: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val greeting = document.personal.fullName
        .split(' ').firstOrNull { it.isNotBlank() }
        ?.let { "Welcome back, $it" } ?: "Welcome"

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.heroRadius))
            .background(Brush.verticalGradient(listOf(Theme.heroTop(), Theme.heroBottom())))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Eyebrow(greeting, Theme.heroMutedInk, dot = accent)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Build a résumé\nthat feels like you.",
                style = displayStyle(34),
                color = Theme.heroInk,
                lineHeight = 40.sp,
            )
            Text(
                "Edit your story, choose a style, and export a polished PDF in minutes.",
                fontSize = 13.sp,
                color = Theme.heroMutedInk,
                lineHeight = 19.sp,
            )
        }

        // The PDF is the point of the app, so the action that produces one is
        // the only filled button on the screen.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent)
                    .clickable(onClick = onPreview)
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Preview", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onShare)
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Share, null, tint = Theme.heroInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text("Share PDF", color = Theme.heroInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Résumé complete", fontSize = 13.sp, color = Theme.heroMutedInk)
                Text(
                    "${document.completionPercentage}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.heroInk,
                )
            }
            CompletionBar(document.completion.toFloat(), accent)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Design library", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.heroInk)
                Text("Tap to explore", fontSize = 11.sp, color = accent.copy(alpha = 0.88f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    "${TemplateCatalogue.templateCount}", "Templates", accent,
                    Modifier.weight(1f).clickable(onClick = onOpenGallery),
                )
                StatTile(
                    "${CoverLetterTemplate.entries.size}", "Cover letters", accent,
                    Modifier.weight(1f).clickable(onClick = onOpenCoverLetter),
                )
                StatTile(
                    "${ResumeAccent.entries.size}", "Accents", accent,
                    Modifier.weight(1f).clickable(onClick = onOpenSettings),
                )
            }
        }
    }
}

// --- start creating ---------------------------------------------------------

@Composable
private fun StartCreating(accent: Color, onLoadExample: () -> Unit, onStartBlank: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeading("Start creating", "Use example content or begin with a clean page.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickStartCard(
                "Example Résumé", "A complete, fictional sample",
                Icons.Filled.Description, accent, Color.White,
                Modifier.weight(1f).clickable(onClick = onLoadExample),
            )
            QuickStartCard(
                "Blank Résumé", "Build every section yourself",
                // Inverted rather than a fixed dark: a navy badge vanishes on a dark card.
                Icons.Filled.Add, Theme.ink(), Theme.paper(),
                Modifier.weight(1f).clickable(onClick = onStartBlank),
            )
        }
    }
}

@Composable
private fun QuickStartCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: Color,
    badgeForeground: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.cardSurface().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(badge),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = badgeForeground, modifier = Modifier.size(18.dp))
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
        Text(subtitle, fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 15.sp)
    }
}

// --- workspace --------------------------------------------------------------

@Composable
private fun CareerWorkspace(
    accent: Color,
    resumeCount: Int,
    applicationCount: Int,
    onOpenGallery: () -> Unit,
    onOpenApplications: () -> Unit,
    onOpenCoverLetter: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenScan: () -> Unit,
    onImport: () -> Unit,
    onUnavailable: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeading("Career workspace", "Keep every résumé version and application connected.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtworkCard(
                "My résumés",
                "$resumeCount version${if (resumeCount == 1) "" else "s"}",
                R.drawable.workspace_resumes, accent, Modifier.weight(1f), onOpenLibrary,
            )
            ArtworkCard(
                "Applications", "$applicationCount tracked",
                R.drawable.workspace_applications, accent, Modifier.weight(1f), onOpenApplications,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtworkCard(
                "Interview prep", "Nothing upcoming",
                R.drawable.workspace_interview, accent, Modifier.weight(1f),
            ) { onUnavailable("Interview prep needs the AI backend, which is not wired on Android yet.") }
            ArtworkCard(
                "ATS check", "Evidence-based review",
                R.drawable.workspace_ats, accent, Modifier.weight(1f), onOpenScan,
            )
        }
        ArtworkCard(
            "Find jobs", "Browse trusted boards, then share one here",
            R.drawable.workspace_import, accent, Modifier.fillMaxWidth(),
        ) { onUnavailable("Job boards need the capture backend, which is not wired on Android yet.") }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtworkCard(
                "Cover letters", "Draft to match",
                R.drawable.workspace_import, accent, Modifier.weight(1f), onOpenCoverLetter,
            )
            ArtworkCard(
                "Import résumé", "PDF, DOCX or a backup",
                R.drawable.workspace_import, accent, Modifier.weight(1f), onImport,
            )
        }

        SmartLinksCard(
            accent = accent,
            linkCount = 0,
            viewCount = 0,
            onClick = {
                onUnavailable("Tracked links need the hosted backend, which is not wired on Android yet.")
            },
        )
    }
}

/**
 * The AI tools, following `aiCareerTools`.
 *
 * Both entries lead somewhere that needs the model backend, so both say so
 * rather than opening a screen that can only apologise.
 */
@Composable
private fun AICareerTools(accent: Color, onOpenCoach: () -> Unit, onUnavailable: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeading("AI career tools", "Write stronger content without losing control of your facts.")
        HeroBanner(
            title = "Career intelligence",
            subtitle = "Benchmarks, market signals and what they mean for your next move.",
            artwork = R.drawable.career_intelligence_hero,
            onClick = {
                onUnavailable("Career intelligence needs the benchmarks backend, which is not wired on Android yet.")
            },
        )

        Row(
            Modifier.fillMaxWidth().cardSurface().clickable(onClick = onOpenCoach).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = accent, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Target a job", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
                Text(
                    "Match the advert, find gaps, and create a reviewed tailored draft.",
                    fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 15.sp,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Theme.mutedInk(), modifier = Modifier.size(15.dp))
        }
    }
}

/**
 * The weekly campaign, following HomeView's `campaign` section.
 *
 * The goals are the iOS defaults; the counts are zero because the stores that
 * would fill them are not ported. Stated plainly on the card rather than
 * padded out with plausible-looking numbers.
 */
@Composable
private fun CareerCampaign(accent: Color, momentum: CareerMomentumSnapshot, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeading("This week", "Momentum comes from a rhythm, not a burst.")
        MomentumCard(snapshot = momentum, accent = accent, onClick = onOpen)
    }
}

@Composable
private fun WorkspaceCard(
    title: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.cardSurface().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
            Text(detail, fontSize = 11.sp, color = Theme.mutedInk())
        }
    }
}

// --- templates --------------------------------------------------------------

@Composable
private fun Templates(
    accent: Color,
    document: ResumeDocument,
    thumbnails: TemplateThumbnailCache,
    onOpenGallery: () -> Unit,
    onOpenTemplate: (ResumeTemplate) -> Unit,
) {
    // A row you push through rather than a list you scroll past. Choosing a
    // template is a browsing decision, and browsing is horizontal — the same
    // shape the gallery uses on iOS.
    val featured = remember {
        listOf(
            ResumeTemplate.ATLAS, ResumeTemplate.NOIR, ResumeTemplate.GAUGE,
            ResumeTemplate.MODERN, ResumeTemplate.ALCOVE, ResumeTemplate.APERTURE,
            ResumeTemplate.CHRONICLE, ResumeTemplate.GENEVA,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Box(Modifier.widthIn(max = 240.dp)) {
                SectionHeading("Templates", "${TemplateCatalogue.templateCount} layouts, one shared vocabulary.")
            }
            Text(
                "See all",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent,
                modifier = Modifier.clickable(onClick = onOpenGallery),
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(featured, key = { it.wireName }) { template ->
                TemplateTile(template, document, thumbnails, accent) { onOpenTemplate(template) }
            }
        }
    }
}

@Composable
private fun TemplateTile(
    template: ResumeTemplate,
    document: ResumeDocument,
    thumbnails: TemplateThumbnailCache,
    accent: Color,
    onClick: () -> Unit,
) {
    val isCurrent = template == document.template
    val thumbnail by produceState<android.graphics.Bitmap?>(null, template, document) {
        value = thumbnails.thumbnail(document, template)
    }

    Column(
        Modifier.width(112.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(595f / 842f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) accent else Theme.hairline(),
                    shape = RoundedCornerShape(10.dp),
                ),
        ) {
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            template.wireName.replaceFirstChar { it.uppercase() },
            fontSize = 11.5.sp,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCurrent) accent else Theme.ink(),
            maxLines = 1,
        )
    }
}

/** The same shelf for letterheads, so the two documents browse alike. */
@Composable
private fun CoverLetterShelf(
    accent: Color,
    letter: CoverLetterDocument,
    onOpen: () -> Unit,
) {
    val featured = remember {
        listOf(
            CoverLetterTemplate.MODERN, CoverLetterTemplate.GRADIENT, CoverLetterTemplate.EXECUTIVE,
            CoverLetterTemplate.MONOGRAM, CoverLetterTemplate.SIDEBAR, CoverLetterTemplate.NOIR,
            CoverLetterTemplate.CLASSIC, CoverLetterTemplate.MINIMAL,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Box(Modifier.widthIn(max = 240.dp)) {
                SectionHeading("Cover letters", "${CoverLetterTemplate.entries.size} letterheads, paired to your templates.")
            }
            Text(
                "Write one",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent,
                modifier = Modifier.clickable(onClick = onOpen),
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(featured, key = { it.wireName }) { template ->
                LetterTile(template, template == letter.template, accent, onOpen)
            }
        }
    }
}

@Composable
private fun LetterTile(
    template: CoverLetterTemplate,
    isCurrent: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(112.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // Drawn rather than rendered: a letterhead is a band and a rule, which
        // is legible at this size where 10pt body copy would not be.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(595f / 842f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) accent else Theme.hairline(),
                    shape = RoundedCornerShape(10.dp),
                ),
        ) {
            LetterheadSketch(template, accent)
        }
        Text(
            template.title,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCurrent) accent else Theme.ink(),
            maxLines = 2,
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun LetterheadSketch(template: CoverLetterTemplate, accent: Color) {
    val family = template.family()
    Column(Modifier.fillMaxSize()) {
        when (family) {
            CoverLetterPdfRenderer.HeaderFamily.BAND ->
                Box(Modifier.fillMaxWidth().height(22.dp).background(accent))
            CoverLetterPdfRenderer.HeaderFamily.DARK ->
                Box(Modifier.fillMaxWidth().height(26.dp).background(Color(0xFF14161A)))
            CoverLetterPdfRenderer.HeaderFamily.SIDEBAR ->
                Box(Modifier.fillMaxWidth()) {
                    Box(Modifier.width(9.dp).height(120.dp).background(accent))
                }
            else -> Spacer(Modifier.height(14.dp))
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (family == CoverLetterPdfRenderer.HeaderFamily.CENTRED) {
                Box(Modifier.fillMaxWidth(0.6f).height(4.dp).background(Theme.mutedInk().copy(alpha = 0.5f)))
                Spacer(Modifier.height(2.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(accent))
            } else if (family == CoverLetterPdfRenderer.HeaderFamily.RULE) {
                Box(Modifier.fillMaxWidth(0.55f).height(4.dp).background(Theme.mutedInk().copy(alpha = 0.5f)))
                Spacer(Modifier.height(2.dp))
                Box(Modifier.fillMaxWidth().height(1.5.dp).background(accent))
            }
            Spacer(Modifier.height(3.dp))
            repeat(7) { row ->
                Box(
                    Modifier
                        .fillMaxWidth(if (row == 6) 0.5f else 0.92f)
                        .height(2.dp)
                        .background(Theme.mutedInk().copy(alpha = 0.28f)),
                )
            }
        }
    }
}

// --- recently edited --------------------------------------------------------

@Composable
private fun RecentlyEdited(document: ResumeDocument, accent: Color, onEdit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeading("Recently edited", "Pick up where you left off.")
        Row(
            Modifier.fillMaxWidth().cardSurface().clickable(onClick = onEdit).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    document.initials.ifBlank { "?" },
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    document.personal.fullName.ifBlank { "Untitled résumé" },
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
                )
                Text(
                    "${document.template.wireName.replaceFirstChar { it.uppercase() }} · tap to edit",
                    fontSize = 11.sp, color = Theme.mutedInk(),
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Theme.mutedInk(), modifier = Modifier.size(15.dp))
        }
    }
}

// --- privacy ----------------------------------------------------------------

@Composable
private fun PrivacyNote() {
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
            "Your résumé stays on this device unless you choose to sync or share it.",
            fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 16.sp,
        )
    }
}

private fun planSummary(template: ResumeTemplate): String = buildList {
    val plan = template.plan
    when (val body = plan.body) {
        is com.resumestudio.model.BodyLayout.Side ->
            add("${body.column.width.toInt()}pt ${body.column.edge.name.lowercase()} column")
        com.resumestudio.model.BodyLayout.Single -> add("single column")
    }
    add("${plan.competencies.name.lowercase().replace('_', ' ')} skills")
    if (plan.darkPaper) add("dark paper")
    if (plan.numberedSections) add("numbered")
}.joinToString(" · ")
