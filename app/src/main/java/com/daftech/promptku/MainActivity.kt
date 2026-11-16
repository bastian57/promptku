package com.daftech.promptku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.Templates
import com.daftech.promptku.ui.PromptKuTheme
import com.daftech.promptku.ui.PromptKuViewModel
import com.daftech.promptku.ui.QuickScreen
import com.daftech.promptku.ui.ScriptScreen
import com.daftech.promptku.ui.TemplateScreen
import com.daftech.promptku.ui.V2IdeaScreen
import com.daftech.promptku.ui.V2KeysScreen
import com.daftech.promptku.ui.V2ProjectsScreen
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
    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage = viewModel.statusMessage

    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(statusMessage)
            viewModel.clearStatusMessage()
        }
    }

    // v2.1 — bottom bar cuma 2 tab: Cerita + Proyek. Tab lama yang dihapus
    // (Buat/Template/Naskah/Riwayat/DNA/Efek) fungsinya PINDAH, bukan hilang:
    // - Buat (wizard v1) -> langkah MANUAL di dalam tab Cerita
    // - Template -> chip "Ide cepat" di Layar Ide
    // - Naskah + Riwayat -> di dalam kartu tiap film di tab Proyek
    // - DNA -> bank tokoh di Layar Ide ("Tokoh dari DNA")
    // - Efek -> 100% kosakata internal engine (tidak ada lagi UI efek)
    val tabs = listOf("Cerita", "Proyek")
    val icons = listOf(
        Icons.Filled.MenuBook,
        Icons.Filled.Folder
    )

    // fix2 — Mode Manual dulu jalan buntu: Quick/Wizard/Template/Script
    // tidak punya tombol kembali ke Ide, tap tab Cerita tidak reset step,
    // dan tidak ada BackHandler (pelarian di v1 adalah ganti tab, yang
    // di v2.1 sudah tidak ada). Tiga jalan pulang sekaligus: tombol di
    // atas layar, tap ulang tab Cerita, tombol back sistem. Reset tab
    // HANYA dari langkah manual — layar v2 (Cerita/Prompt/Kunci) punya
    // tombol kembali sendiri, dan tap tak sengaja tidak boleh membuang
    // bible yang belum tersimpan.
    val isManualStep = v2ViewModel.step == V2Step.MANUAL ||
        v2ViewModel.step == V2Step.MANUAL_DETAIL ||
        v2ViewModel.step == V2Step.MANUAL_TEMPLATES ||
        v2ViewModel.step == V2Step.MANUAL_SCRIPT

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            if (index == 0 && tab == 0 && isManualStep) {
                                v2ViewModel.updateStep(V2Step.IDEA)
                            }
                            tab = index
                        },
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
                    // Mode Manual (mesin v1) — alur lama tetap hidup di sini.
                    V2Step.MANUAL, V2Step.MANUAL_DETAIL,
                    V2Step.MANUAL_TEMPLATES, V2Step.MANUAL_SCRIPT -> {
                        BackHandler { v2ViewModel.updateStep(V2Step.IDEA) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedButton(
                                onClick = { v2ViewModel.updateStep(V2Step.IDEA) },
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) { Text("← Kembali ke Ide") }
                            Box(modifier = Modifier.weight(1f)) {
                                when (v2ViewModel.step) {
                                    V2Step.MANUAL -> QuickScreen(
                                        viewModel = viewModel,
                                        onGoToDetail = { v2ViewModel.updateStep(V2Step.MANUAL_DETAIL) },
                                        onGoToScript = { v2ViewModel.updateStep(V2Step.MANUAL_SCRIPT) }
                                    )
                                    V2Step.MANUAL_DETAIL -> WizardScreen(
                                        viewModel = viewModel,
                                        onGoToTemplates = { v2ViewModel.updateStep(V2Step.MANUAL_TEMPLATES) },
                                        onGoToQuick = { v2ViewModel.updateStep(V2Step.MANUAL) }
                                    )
                                    V2Step.MANUAL_TEMPLATES -> TemplateScreen(
                                        onUseTemplate = { id ->
                                            val template = Templates.all.firstOrNull { it.id == id }
                                            if (template != null) {
                                                viewModel.applyTemplate(template)
                                                v2ViewModel.updateStep(V2Step.MANUAL_DETAIL)
                                            }
                                        }
                                    )
                                    V2Step.MANUAL_SCRIPT -> ScriptScreen(
                                        viewModel = viewModel,
                                        onGoToProjects = { v2ViewModel.updateStep(V2Step.MANUAL) }
                                    )
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
                1 -> V2ProjectsScreen(
                    v2 = v2ViewModel,
                    onOpenBible = { id ->
                        v2ViewModel.loadBible(id)
                        tab = 0
                    },
                    onContinueSeries = { id ->
                        v2ViewModel.continueSeries(id)
                        tab = 0
                    },
                    onNewFilm = {
                        v2ViewModel.updateStep(V2Step.IDEA)
                        tab = 0
                    }
                )
            }
        }
    }
}
