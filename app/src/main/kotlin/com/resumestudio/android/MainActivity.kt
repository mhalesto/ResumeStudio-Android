package com.resumestudio.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.resumestudio.android.ui.EditorScreen
import com.resumestudio.android.ui.ApplicationsScreen
import com.resumestudio.android.ui.RecruiterScanScreen
import com.resumestudio.android.ui.ResumeLibraryScreen
import com.resumestudio.android.ui.CoverLetterScreen
import com.resumestudio.android.ui.FloatingCareerCoach
import com.resumestudio.android.ui.SettingsScreen
import com.resumestudio.android.ui.GalleryScreen
import com.resumestudio.android.ui.HomeScreen
import com.resumestudio.android.ui.LocalAccent
import com.resumestudio.android.ui.ResumeStudioTheme
import com.resumestudio.android.ui.SectionHeading
import com.resumestudio.android.ui.TemplatePreviewScreen
import com.resumestudio.android.ui.Theme
import com.resumestudio.android.ui.cardSurface
import com.resumestudio.android.ui.displayStyle
import com.resumestudio.model.ResumeAccent
import com.resumestudio.model.TodayActions
import com.resumestudio.model.TodayRoute
import com.resumestudio.model.ResumeTemplate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppShell() }
    }
}

/** The tab bar, matching `AppTab` in `RootView.swift`. */
private enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("Today", Icons.Filled.AutoAwesome),
    DOCUMENTS("Documents", Icons.Filled.Description),
    APPLICATIONS("Applications", Icons.Filled.WorkOutline),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@Composable
fun AppShell(viewModel: ResumeViewModel = viewModel()) {
    // The document is the single source of truth for the whole shell, accent
    // included. iOS reads the accent from `store.document.accent`, so one choice
    // tints the UI and sets the ink in the export; a separate UI accent here
    // would let the two drift and make the Settings caption a lie.
    val library by viewModel.state.collectAsState()
    val coachIntroDismissed by viewModel.coachIntroDismissed.collectAsState()
    val applications by viewModel.applications.collectAsState()
    val coverLetter by viewModel.coverLetter.collectAsState()
    val document = library.document
    val accent = document.accent
    val momentum = remember(applications) { viewModel.momentum() }
    val todayActions = remember(document, momentum) { TodayActions.build(document, momentum) }

    var tab by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var previewing by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var coaching by rememberSaveable { mutableStateOf(false) }
    var writingLetter by rememberSaveable { mutableStateOf(false) }
    var browsingLibrary by rememberSaveable { mutableStateOf(false) }
    var scanning by rememberSaveable { mutableStateOf(false) }

    ResumeStudioTheme(accent = accent) {
        val accentColor = LocalAccent.current
        val context = LocalContext.current
        val template = previewing?.let(ResumeTemplate::from)

        if (template != null) {
            BackHandler { previewing = null }
            TemplatePreviewScreen(
                template = template,
                document = document,
                accent = accentColor,
                onBack = { previewing = null },
                onApply = {
                    viewModel.setTemplate(template)
                    previewing = null
                },
                onShare = { ResumeExporter.share(context, document.copy(template = template)) },
                modifier = Modifier.fillMaxSize(),
            )
            return@ResumeStudioTheme
        }

        if (scanning) {
            BackHandler { scanning = false }
            RecruiterScanScreen(
                document = document,
                accent = accentColor,
                onOpenEditor = { scanning = false; editing = true },
                onBack = { scanning = false },
                modifier = Modifier.fillMaxSize(),
            )
            return@ResumeStudioTheme
        }

        if (browsingLibrary) {
            BackHandler { browsingLibrary = false }
            ResumeLibraryScreen(
                resumes = library.resumes,
                activeID = library.activeResumeID,
                accent = accentColor,
                onSelect = viewModel::selectResume,
                onDuplicate = viewModel::duplicateResume,
                onRename = viewModel::renameResume,
                onDelete = viewModel::deleteResume,
                onBack = { browsingLibrary = false },
                modifier = Modifier.fillMaxSize(),
            )
            return@ResumeStudioTheme
        }

        if (writingLetter) {
            BackHandler { writingLetter = false }
            CoverLetterScreen(
                document = coverLetter,
                accent = accentColor,
                onEdit = viewModel::editCoverLetter,
                onSeedFromResume = viewModel::seedCoverLetterFromResume,
                onShare = { ResumeExporter.shareCoverLetter(context, coverLetter) },
                onBack = { writingLetter = false },
                modifier = Modifier.fillMaxSize(),
            )
            return@ResumeStudioTheme
        }

        if (editing) {
            BackHandler { editing = false }
            EditorScreen(
                document = document,
                documentKey = library.activeResumeID,
                accent = accentColor,
                onEdit = viewModel::edit,
                onBack = { editing = false },
                modifier = Modifier.fillMaxSize(),
            )
            return@ResumeStudioTheme
        }

        Scaffold(
            containerColor = Theme.paper(),
            bottomBar = { BottomBar(AppTab.valueOf(tab), accentColor) { tab = it.name } },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (AppTab.valueOf(tab)) {
                    AppTab.HOME -> HomeScreen(
                        document = document,
                        accent = accentColor,
                        resumeCount = library.resumes.size,
                        applicationCount = applications.size,
                        todayActions = todayActions,
                        momentum = momentum,
                        onOpenGallery = { tab = AppTab.DOCUMENTS.name },
                        onOpenApplications = { tab = AppTab.APPLICATIONS.name },
                        onOpenCoverLetter = { writingLetter = true },
                        onOpenLibrary = { browsingLibrary = true },
                        onOpenScan = { scanning = true },
                        onTodayAction = { action ->
                            when (action.route) {
                                TodayRoute.EDITOR, TodayRoute.CLAIMS -> editing = true
                                TodayRoute.GALLERY -> tab = AppTab.DOCUMENTS.name
                                TodayRoute.PREVIEW -> previewing = document.template.wireName
                                TodayRoute.MOMENTUM, TodayRoute.APPLICATIONS ->
                                    tab = AppTab.APPLICATIONS.name
                            }
                        },
                        onPreview = { previewing = document.template.wireName },
                        onShare = { ResumeExporter.share(context, document) },
                        onEdit = { editing = true },
                        onOpenTemplate = { previewing = it.wireName },
                        onLoadExample = viewModel::loadExample,
                        onStartBlank = viewModel::startBlank,
                    )

                    AppTab.DOCUMENTS -> GalleryScreen(
                        document = document,
                        accent = accentColor,
                        onOpenTemplate = { previewing = it.wireName },
                    )

                    AppTab.APPLICATIONS -> ApplicationsScreen(
                        applications = applications,
                        accent = accentColor,
                        onAdd = viewModel::addApplication,
                        onSetStatus = viewModel::setApplicationStatus,
                        onRemove = viewModel::removeApplication,
                    )

                    AppTab.SETTINGS -> SettingsScreen(
                        document = document,
                        accent = accentColor,
                        onPickAccent = viewModel::setAccent,
                        onPickPaper = viewModel::setPaperSize,
                        onPickFont = viewModel::setFontChoice,
                        onOpenUrl = { url ->
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url),
                                    ),
                                )
                            }
                        },
                        versionName = BuildConfig.VERSION_NAME,
                    )
                }

                // Over the content, not in it: an assistant that is available
                // rather than a menu item that has to be found.
                FloatingCareerCoach(
                    accent = accentColor,
                    showIntro = !coachIntroDismissed,
                    onDismissIntro = viewModel::dismissCoachIntro,
                    onOpen = { coaching = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun BottomBar(selected: AppTab, accent: Color, onSelect: (AppTab) -> Unit) {
    NavigationBar(containerColor = Theme.card(), tonalElevation = 0.dp) {
        AppTab.entries.forEach { entry ->
            NavigationBarItem(
                selected = entry == selected,
                onClick = { onSelect(entry) },
                icon = { Icon(entry.icon, contentDescription = entry.title, Modifier.size(20.dp)) },
                label = { Text(entry.title, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    indicatorColor = accent.copy(alpha = 0.14f),
                    unselectedIconColor = Theme.mutedInk(),
                    unselectedTextColor = Theme.mutedInk(),
                ),
            )
        }
    }
}

/**
 * Settings, currently just the accent picker.
 *
 * It earns its place ahead of the rest of Settings because it is the one control
 * that proves the theme is wired the way iOS's is — every surface, tab and
 * button should retint from here, with nothing hard-coded to orange.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(current: ResumeAccent, onPick: (ResumeAccent) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.paper())
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = displayStyle(30), color = Theme.ink())
        SectionHeading("Accent", "Tints the app and drives the exported PDF.")

        Column(Modifier.fillMaxWidth().cardSurface().padding(14.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResumeAccent.entries.forEach { entry ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(entry.argb))
                            .clickable { onPick(entry) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (entry == current) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(current.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink())
                Spacer(Modifier.size(8.dp))
                if (current.isPremium) {
                    Text(
                        current.tier.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp, color = LocalAccent.current,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingSoon(title: String, detail: String) {
    Column(
        Modifier.fillMaxSize().background(Theme.paper()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = displayStyle(30), color = Theme.ink())
        Text(detail, fontSize = 13.sp, color = Theme.mutedInk(), lineHeight = 19.sp)
    }
}
