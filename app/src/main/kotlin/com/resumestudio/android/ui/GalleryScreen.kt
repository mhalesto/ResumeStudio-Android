package com.resumestudio.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.resumestudio.model.BodyLayout
import com.resumestudio.model.ResumeDocument
import com.resumestudio.model.ResumeTemplate
import com.resumestudio.model.SideColumn
import com.resumestudio.model.TemplateCatalogue
import com.resumestudio.model.TemplatePlan
import com.resumestudio.render.ResumePageRasterizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalleryScreen(
    accent: Color,
    onOpenTemplate: (ResumeTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val templates = remember { ResumeTemplate.entries.sortedBy { it.wireName } }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Theme.paper()),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text("Templates", style = displayStyle(30), color = Theme.ink())
                Spacer(Modifier.height(4.dp))
                Text(
                    "${TemplateCatalogue.twoColumnTemplates().size} of ${templates.size} lay the page " +
                        "out in two columns. Tap one to render it.",
                    fontSize = 13.sp, color = Theme.mutedInk(), lineHeight = 18.sp,
                )
            }
        }
        items(templates, key = { it.wireName }) { template ->
            TemplateCard(template, accent) { onOpenTemplate(template) }
        }
    }
}

@Composable
private fun TemplateCard(template: ResumeTemplate, accent: Color, onClick: () -> Unit) {
    val plan = template.plan
    Row(
        Modifier.fillMaxWidth().cardSurface().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PageThumb(plan, accent)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                template.wireName.replaceFirstChar { it.uppercase() },
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink(),
            )
            Spacer(Modifier.height(2.dp))
            Text(plan.summary(), fontSize = 11.5.sp, color = Theme.mutedInk(), lineHeight = 15.sp)
        }
    }
}

/** A miniature of the page's own shape — the band where the plan puts it. */
@Composable
private fun PageThumb(plan: TemplatePlan, accent: Color) {
    Box(
        Modifier
            .size(width = 34.dp, height = 48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (plan.darkPaper) Color(0xFF14161A) else Color.White)
            .then(
                Modifier.background(
                    if (plan.darkPaper) Color(0xFF14161A) else Theme.muted().copy(alpha = 0.5f),
                ),
            ),
    ) {
        (plan.body as? BodyLayout.Side)?.let { side ->
            Box(
                Modifier
                    .fillMaxWidth(side.column.width / 595f)
                    .height(48.dp)
                    .align(
                        if (side.column.edge == SideColumn.Edge.LEADING) Alignment.CenterStart
                        else Alignment.CenterEnd,
                    )
                    .background(
                        when (side.column.fill) {
                            SideColumn.Fill.DARK -> Color(0xFF1F2329)
                            SideColumn.Fill.ACCENT -> accent
                            SideColumn.Fill.TINT -> accent.copy(alpha = 0.18f)
                            SideColumn.Fill.NONE -> Color.Transparent
                        },
                    ),
            )
        }
    }
}

@Composable
fun TemplatePreviewScreen(
    template: ResumeTemplate,
    document: ResumeDocument,
    accent: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cacheDir = LocalContext.current.cacheDir

    val page by produceState<ResumePageRasterizer.Page?>(null, template, document) {
        value = withContext(Dispatchers.Default) {
            ResumePageRasterizer(cacheDir).rasterize(document.copy(template = template), 0, widthPx = 1400)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Theme.paper())
            // This screen is shown outside the Scaffold, so nothing else is
            // holding it clear of the status bar — without this the title is
            // drawn underneath the clock.
            .systemBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Theme.ink())
            }
            Spacer(Modifier.size(4.dp))
            Column {
                Text(
                    template.wireName.replaceFirstChar { it.uppercase() },
                    style = displayStyle(22), color = Theme.ink(),
                )
                Text(
                    page?.let { "${it.pageCount} page${if (it.pageCount == 1) "" else "s"}" }
                        ?: "Rendering…",
                    fontSize = 11.sp, color = Theme.mutedInk(),
                )
            }
        }

        Text(
            template.plan.summary(),
            fontSize = 11.5.sp, color = Theme.mutedInk(),
            modifier = Modifier.padding(bottom = 12.dp),
        )

        PagePreview(page?.bitmap, accent)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PagePreview(bitmap: Bitmap?, accent: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(595f / 842f)
            .cardSurface(radius = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            CircularProgressIndicator(color = accent)
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Rendered résumé page",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun TemplatePlan.summary(): String = buildList {
    when (val body = body) {
        is BodyLayout.Side -> add(
            "${body.column.width.toInt()}pt ${body.column.edge.name.lowercase()} column, " +
                body.column.fill.name.lowercase() + " fill",
        )
        BodyLayout.Single -> add("single column")
    }
    add("${experience.name.lowercase().replace('_', ' ')} roles")
    add("${competencies.name.lowercase().replace('_', ' ')} skills")
    if (skillsFirst) add("skills first")
    if (numberedSections) add("numbered")
    if (hangingHeadings) add("hanging headings")
    if (profileInHeader) add("summary in masthead")
    if (bodyInset > 0f) add("${bodyInset.toInt()}pt inset")
    if (density != 1f) add("density $density")
}.joinToString(" · ")
