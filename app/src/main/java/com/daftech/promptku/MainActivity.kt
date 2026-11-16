package com.daftech.promptku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.daftech.promptku.engine.Templates
import com.daftech.promptku.ui.DnaScreen
import com.daftech.promptku.ui.EffectsScreen
import com.daftech.promptku.ui.HistoryScreen
import com.daftech.promptku.ui.ProjectScreen
import com.daftech.promptku.ui.PromptKuTheme
import com.daftech.promptku.ui.PromptKuViewModel
import com.daftech.promptku.ui.QuickScreen
import com.daftech.promptku.ui.ScriptScreen
import com.daftech.promptku.ui.TemplateScreen
import com.daftech.promptku.ui.V2IdeaScreen
import com.daftech.promptku.ui.V2KeysScreen
import com.daftech.promptku.ui.V2PromptScreen
import com.daftech.promptku.ui.V2Step
import com.daftech.promptku.ui.V2StoryScreen
import com.daftech.promptku.ui.V2ViewModel
import com.daftech.promptku.ui.WizardScreen

class MainActivity : ComponentActivity() {

    private val viewModel: PromptKuViewModel by viewModels()
    private val v2ViewModel: V2ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PromptKuTheme {
                PromptKuApp(viewModel = viewModel, v2ViewModel = v2ViewModel)
            }
        }
    }
}

@Composable
fun PromptKuApp(viewModel: PromptKuViewModel, v2ViewModel: V2ViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    // Mode Cepat v1.4 = layar pertama tab Buat; false = wizard Mode Detail.
    var quickMode by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage = viewModel.statusMessage

    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(statusMessage)
            viewModel.clearStatusMessage()
        }
    }

    // v2.0 — tab "Cerita" (alur baru) di index 0; tab lama geser +1.
    val tabs = listOf("Cerita", "Buat", "Template", "Proyek", "Naskah", "DNA", "Riwayat", "Efek")
    val icons = listOf(
        Icons.Filled.MenuBook,
        Icons.Filled.Create,
        Icons.Filled.Movie,
        Icons.Filled.Folder,
        Icons.Filled.Description,
        Icons.Filled.Person,
        Icons.Filled.History,
        Icons.Filled.Star
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(padding)
        ) {
            when (tab) {
                0 -> when (v2ViewModel.step) {
                    V2Step.IDEA -> V2IdeaScreen(v2 = v2ViewModel)
                    V2Step.STORY -> V2StoryScreen(v2 = v2ViewModel)
                    V2Step.PROMPTS -> V2PromptScreen(v2 = v2ViewModel)
                    V2Step.KEYS -> V2KeysScreen(v2 = v2ViewModel)
                }
                1 -> if (quickMode) {
                    QuickScreen(
                        viewModel = viewModel,
                        onGoToDetail = { quickMode = false },
                        onGoToScript = { tab = 4 }
                    )
                } else {
                    WizardScreen(
                        viewModel = viewModel,
                        onGoToTemplates = { tab = 2 },
                        onGoToQuick = { quickMode = true }
                    )
                }
                2 -> TemplateScreen(
                    onUseTemplate = { id ->
                        val template = Templates.all.firstOrNull { it.id == id }
                        if (template != null) {
                            viewModel.applyTemplate(template)
                            quickMode = false
                            tab = 1
                        }
                    }
                )
                3 -> ProjectScreen(
                    viewModel = viewModel,
                    onGoToWizard = {
                        quickMode = false
                        tab = 1
                    }
                )
                4 -> ScriptScreen(
                    viewModel = viewModel,
                    onGoToProjects = { tab = 3 }
                )
                5 -> DnaScreen(viewModel = viewModel)
                6 -> HistoryScreen(viewModel = viewModel)
                7 -> EffectsScreen()
            }
        }
    }
}
