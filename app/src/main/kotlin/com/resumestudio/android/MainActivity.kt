package com.resumestudio.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resumestudio.model.BodyLayout
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.ResumeTemplate
import com.resumestudio.model.TemplateCatalogue
import com.resumestudio.model.TemplatePlan

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme { CatalogueScreen() }
        }
    }
}

/**
 * A read-out of the mirrored catalogue.
 *
 * This is scaffolding, not the gallery: it exists so the shared spec can be seen
 * working on a device before the renderer has a preview to show. It is the first
 * thing that should be replaced once `:core:render` produces page images.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen() {
    val templates = remember { ResumeTemplate.entries.sortedBy { it.wireName } }
    val twoColumn = remember { TemplateCatalogue.twoColumnTemplates().toSet() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Templates · ${TemplateCatalogue.templateCount}") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "${twoColumn.size} of ${templates.size} lay the page out in two columns.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(templates) { template ->
                TemplateRow(template, template.plan)
            }
        }
    }
}

@Composable
private fun TemplateRow(template: ResumeTemplate, plan: TemplatePlan) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    template.wireName.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(8.dp))
                if (plan.darkPaper) Swatch(Color(0xFF14161A))
            }
            Spacer(Modifier.size(4.dp))
            Text(plan.summary(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Spacer(Modifier.size(12.dp).clip(CircleShape).background(color))
}

/** The plan in a line, so a row says what the template actually does. */
private fun TemplatePlan.summary(): String = buildList {
    when (val body = body) {
        is BodyLayout.Side -> add(
            "${body.column.width.toInt()}pt ${body.column.edge.name.lowercase()} column, " +
                body.column.fill.name.lowercase() + " fill",
        )
        BodyLayout.Single -> add("single column")
    }
    add("${experience.name.lowercase()} roles")
    add("${competencies.name.lowercase()} skills")
    if (skillsFirst) add("skills first")
    if (numberedSections) add("numbered")
    if (hangingHeadings) add("hanging headings")
    if (profileInHeader) add("summary in masthead")
    if (bodyInset > 0f) add("${bodyInset.toInt()}pt inset")
    if (density != 1f) add("density $density")
}.joinToString(" · ")

/** Kept next to the screen so the accent list has one obvious place to grow. */
internal val previewAccent: ResumeAccent = ResumeAccent.ORANGE
