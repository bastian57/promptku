package com.daftech.promptku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.daftech.promptku.engine.Templates
import com.daftech.promptku.ui.DnaScreen
import com.daftech.promptku.ui.HistoryScreen
import com.daftech.promptku.ui.ProjectScreen
import com.daftech.promptku.ui.PromptKuTheme
import com.daftech.promptku.ui.PromptKuViewModel
import com.daftech.promptku.ui.TemplateScreen
import com.daftech.promptku.ui.WizardScreen

class MainActivity : ComponentActivity() {

    private val viewModel: PromptKuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PromptKuTheme {
                PromptKuApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PromptKuApp(viewModel: PromptKuViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage = viewModel.statusMessage

    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(statusMessage)
            viewModel.clearStatusMessage()
        }
    }

    val tabs = listOf("Buat", "Template", "Proyek", "DNA", "Riwayat")
    val icons = listOf(
        Icons.Filled.Create,
        Icons.Filled.Movie,
        Icons.Filled.Folder,
        Icons.Filled.Person,
        Icons.Filled.History
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
                0 -> WizardScreen(
                    viewModel = viewModel,
                    onGoToTemplates = { tab = 1 }
                )
                1 -> TemplateScreen(
                    onUseTemplate = { id ->
                        val template = Templates.all.firstOrNull { it.id == id }
                        if (template != null) {
                            viewModel.applyTemplate(template)
                            tab = 0
                        }
                    }
                )
                2 -> ProjectScreen(
                    viewModel = viewModel,
                    onGoToWizard = { tab = 0 }
                )
                3 -> DnaScreen(viewModel = viewModel)
                4 -> HistoryScreen(viewModel = viewModel)
            }
        }
    }
}
