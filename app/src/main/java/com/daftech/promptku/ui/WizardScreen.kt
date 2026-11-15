@file:OptIn(ExperimentalLayoutApi::class)
// Opt-in FILE-LEVEL: FlowRow dipakai di DUA composable file ini
// (chip genre dan chip engine hasil). @OptIn di satu fungsi (dulu
// cuma di baris 47) TIDAK menular ke fungsi lain — makanya
// compileDebugKotlin merah di baris 400.
// PENTING: anotasi @file: WAJIB di atas deklarasi package. Kalau
// ditaruh di bawah package, kompiler nolak import sesudahnya
// ("imports are only allowed in the beginning of file") — itu yang
// bikin fix2 merah di kaptGenerateStubsDebugKotlin.
package com.daftech.promptku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.daftech.promptku.engine.CameraAngle
import com.daftech.promptku.engine.CameraMove
import com.daftech.promptku.engine.CharacterCard
import com.daftech.promptku.engine.CharacterDna
import com.daftech.promptku.engine.DialogueCheck
import com.daftech.promptku.engine.DialogueLang
import com.daftech.promptku.engine.DialogueLine
import com.daftech.promptku.engine.DialogueMatrix
import com.daftech.promptku.engine.FocusMode
import com.daftech.promptku.engine.Genre
import com.daftech.promptku.engine.HideTactic
import com.daftech.promptku.engine.LightKind
import com.daftech.promptku.engine.ShotSize
import com.daftech.promptku.engine.TargetEngine
import com.daftech.promptku.engine.VisualStyle
import com.daftech.promptku.engine.WorldLock

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (Int) -> Unit,
    enabled: Boolean = true
) {
    Text(text = label, style = MaterialTheme.typography.titleSmall)
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(index) },
                label = { Text(option) },
                enabled = enabled
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun WizardScreen(
    viewModel: PromptKuViewModel,
    onGoToTemplates: () -> Unit,
    onGoToQuick: (() -> Unit)? = null
) {
    val input = viewModel.input
    val step = viewModel.wizardStep
    val clipboard = LocalClipboardManager.current
    var showProjectPicker by remember { mutableStateOf(false) }
    var newProjectTitle by remember { mutableStateOf("") }
    var sceneTitle by remember { mutableStateOf("") }

    val stepTitles = listOf(
        "1. Genre + Engine",
        "2. Gaya Visual",
        "3. Tokoh + Dunia (DNA)",
        "4. Shot",
        "5. Cerita / Beat"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Wizard Sutradara — Mode Detail",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Jawab 5 langkah. Input Bahasa Indonesia, prompt jadi Bahasa Inggris.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (onGoToQuick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onGoToQuick, modifier = Modifier.fillMaxWidth()) {
                Text("Mode Cepat (1 kalimat jadi film)")
            }
        }
        // Banner Global Lock v1.1: field yang dikunci proyek tidak bisa diubah
        // di wizard — generate menegakkan lock.
        val activeLock = viewModel.activeLock
        if (viewModel.activeProjectId != null && activeLock != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Global Lock aktif: ${activeLock.summaryId()}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Field yang dikunci tidak bisa diubah di sini — ubah di layar Proyek.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { viewModel.exitProjectMode() }) {
                        Text("Keluar dari mode proyek")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { (step + 1) / 5f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stepTitles[step], style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        when (step) {
            0 -> {
                ChipRow(
                    label = "Engine tujuan (output teks saja)",
                    options = TargetEngine.values().map { it.labelId },
                    selected = input.engine?.labelId,
                    onSelect = { index -> viewModel.updateEngine(TargetEngine.values()[index]) }
                )
                ChipRow(
                    label = "Genre",
                    options = Genre.values().map { it.labelId },
                    selected = input.genre?.labelId,
                    onSelect = { index -> viewModel.updateGenre(Genre.values()[index]) }
                )
                OutlinedTextField(
                    value = input.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Judul scene (misal: Scene 01 Establishing)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            1 -> {
                ChipRow(
                    label = "Gaya visual",
                    options = VisualStyle.values().map { it.labelId },
                    selected = input.style?.labelId,
                    onSelect = { index -> viewModel.updateStyle(VisualStyle.values()[index]) },
                    enabled = activeLock?.style == null
                )
                Text(
                    text = "Gaya menentukan tampilan + grade warna yang dikunci di semua scene.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            2 -> {
                Text(text = "Tokoh utama (DNA Lock)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                val charNames = viewModel.dnaCharacters.map { it.name }
                ChipRow(
                    label = "Pilih karakter tersimpan",
                    options = charNames,
                    selected = input.character?.name,
                    onSelect = { index ->
                        val item = viewModel.dnaCharacters[index]
                        viewModel.updateCharacter(CharacterDna(item.name, item.descriptionEn))
                    },
                    enabled = activeLock?.character == null
                )
                OutlinedTextField(
                    value = input.character?.descriptionEn ?: "",
                    onValueChange = { value ->
                        val name = input.character?.name ?: "Tokoh"
                        viewModel.updateCharacter(CharacterDna(name, value))
                    },
                    label = { Text("Deskripsi karakter (Inggris, verbatim)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = activeLock?.character == null
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Kartu 3 jangkar (dipakai di prompt)", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "1 sifat + 1 gestur + 1 benda (+ jangkar wajah). ~12-15 kata — ini yang mengunci wajah, bukan DNA panjang.",
                    style = MaterialTheme.typography.bodySmall
                )
                val card = input.characterCard ?: CharacterCard()
                OutlinedTextField(
                    value = card.trait,
                    onValueChange = { value ->
                        viewModel.updateCharacterCard(card.copy(trait = value))
                    },
                    label = { Text("Sifat (misal: calm confident look)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeLock?.character == null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = card.gesture,
                    onValueChange = { value ->
                        viewModel.updateCharacterCard(card.copy(gesture = value))
                    },
                    label = { Text("Gestur (misal: hand to hair)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeLock?.character == null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = card.objectProp,
                    onValueChange = { value ->
                        viewModel.updateCharacterCard(card.copy(objectProp = value))
                    },
                    label = { Text("Benda (misal: torn denim jacket)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeLock?.character == null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = card.faceAnchor,
                    onValueChange = { value ->
                        viewModel.updateCharacterCard(card.copy(faceAnchor = value))
                    },
                    label = { Text("Jangkar wajah (misal: grease-smudged face)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activeLock?.character == null
                )
                val cardWords = card.toDescription().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                Text(
                    text = "Kartu: $cardWords kata (target 12-15)" +
                        if (card.isEmpty()) " — kosong = pakai DNA verbatim (berisiko trim)." else "",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Tokoh kedua (opsional)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                ChipRow(
                    label = "Pilih karakter kedua",
                    options = charNames,
                    selected = input.secondCharacter?.name,
                    onSelect = { index ->
                        val item = viewModel.dnaCharacters[index]
                        viewModel.updateSecondCharacter(CharacterDna(item.name, item.descriptionEn))
                    },
                    enabled = activeLock?.secondCharacter == null
                )
                TextButton(
                    onClick = { viewModel.updateSecondCharacter(null) },
                    enabled = activeLock?.secondCharacter == null
                ) {
                    Text("Hapus tokoh kedua")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Dunia (World Lock)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                val worldNames = viewModel.dnaWorlds.map { it.name }
                ChipRow(
                    label = "Pilih dunia tersimpan",
                    options = worldNames,
                    selected = input.world?.name,
                    onSelect = { index ->
                        val item = viewModel.dnaWorlds[index]
                        viewModel.updateWorld(WorldLock(item.name, item.descriptionEn))
                    },
                    enabled = activeLock?.world == null
                )
                OutlinedTextField(
                    value = input.world?.descriptionEn ?: "",
                    onValueChange = { value ->
                        val name = input.world?.name ?: "Dunia"
                        viewModel.updateWorld(WorldLock(name, value))
                    },
                    label = { Text("Deskripsi dunia (Inggris)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = activeLock?.world == null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DNA baru disimpan di tab DNA. Nama berlisensi (misal tokoh kartun terkenal) bisa ditolak filter IP Flow/Veo — app akan memperingatkan, tidak memblokir.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            3 -> {
                ChipRow(
                    label = "Ukuran shot",
                    options = ShotSize.values().map { it.labelId },
                    selected = input.shotSize?.labelId,
                    onSelect = { index -> viewModel.updateShotSize(ShotSize.values()[index]) }
                )
                ChipRow(
                    label = "Sudut kamera",
                    options = CameraAngle.values().map { it.labelId },
                    selected = input.angle?.labelId,
                    onSelect = { index -> viewModel.updateAngle(CameraAngle.values()[index]) }
                )
                ChipRow(
                    label = "Lensa",
                    options = listOf("24mm", "35mm", "50mm", "85mm"),
                    selected = input.lensMm?.let { "${it}mm" },
                    onSelect = { index ->
                        viewModel.updateLens(listOf(24, 35, 50, 85)[index])
                    },
                    enabled = activeLock?.lensMm == null
                )
                ChipRow(
                    label = "Gerakan kamera (pilih 1)",
                    options = CameraMove.values().map { it.labelId },
                    selected = input.cameraMove?.labelId,
                    onSelect = { index -> viewModel.updateCameraMove(CameraMove.values()[index]) }
                )
                ChipRow(
                    label = "Cahaya",
                    options = LightKind.values().map { it.labelId },
                    selected = input.light?.labelId,
                    onSelect = { index -> viewModel.updateLight(LightKind.values()[index]) },
                    enabled = activeLock?.light == null
                )
                ChipRow(
                    label = "Fokus: wajah ATAU tangan",
                    options = FocusMode.values().map { it.labelId },
                    selected = input.focusMode?.labelId,
                    onSelect = { index -> viewModel.updateFocus(FocusMode.values()[index]) }
                )
                ChipRow(
                    label = "Taktik hide-the-weakness",
                    options = HideTactic.values().map { it.labelId },
                    selected = input.hideTactic.labelId,
                    onSelect = { index -> viewModel.updateHide(HideTactic.values()[index]) }
                )
            }
            4 -> {
                OutlinedTextField(
                    value = input.actionEn,
                    onValueChange = { viewModel.updateAction(it) },
                    label = { Text("Satu aksi (Inggris, misal: lands hard and stands up slowly)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input.poseStart,
                    onValueChange = { viewModel.updatePoses(it, input.poseEnd) },
                    label = { Text("Pose awal") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input.poseEnd,
                    onValueChange = { viewModel.updatePoses(input.poseStart, it) },
                    label = { Text("Pose akhir") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input.materialEn,
                    onValueChange = { viewModel.updateMaterial(it) },
                    label = { Text("Material + fisika (debu, retakan, percikan)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input.audioEn,
                    onValueChange = { viewModel.updateAudio(it) },
                    label = { Text("Audio SFX / ambient") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Dialog (opsional — 1 pembicara per klip)", style = MaterialTheme.typography.titleSmall)
                val dialogueLine = input.dialogue ?: DialogueLine(
                    speaker = input.dialogueName,
                    textEn = input.dialogueLine
                )
                OutlinedTextField(
                    value = dialogueLine.speaker,
                    onValueChange = { value ->
                        viewModel.updateWizardDialogue(dialogueLine.copy(speaker = value))
                    },
                    label = { Text("Nama pengucap dialog") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dialogueLine.textId,
                    onValueChange = { value ->
                        viewModel.updateWizardDialogue(dialogueLine.copy(textId = value))
                    },
                    label = { Text("Dialog Indonesia (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dialogueLine.textEn,
                    onValueChange = { value ->
                        viewModel.updateWizardDialogue(dialogueLine.copy(textEn = value))
                    },
                    label = { Text("Dialog Inggris (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dialogueLine.tone,
                    onValueChange = { value ->
                        viewModel.updateWizardDialogue(dialogueLine.copy(tone = value))
                    },
                    label = { Text("Nada (misal: a low, steady voice)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dialogueLine.tactic,
                    onValueChange = { value ->
                        viewModel.updateWizardDialogue(dialogueLine.copy(tactic = value))
                    },
                    label = { Text("Taktik (misal: memohon)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ChipRow(
                    label = "Bahasa dialog",
                    options = DialogueLang.values().map { it.labelId },
                    selected = dialogueLine.language.labelId,
                    onSelect = { index ->
                        viewModel.updateWizardDialogue(
                            dialogueLine.copy(language = DialogueLang.values()[index])
                        )
                    }
                )
                val matrixCheck = DialogueMatrix.check(
                    input.engine ?: TargetEngine.FLOW,
                    dialogueLine.language
                )
                if (matrixCheck.check != DialogueCheck.ALLOWED) {
                    Text(text = matrixCheck.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { viewModel.updateWizardStep(step - 1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Kembali")
                }
            }
            if (step < 4) {
                Button(
                    onClick = { viewModel.updateWizardStep(step + 1) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Lanjut")
                }
            } else {
                Button(
                    onClick = { viewModel.generate() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Generate Prompt")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.rollDice() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Dadu Sutradara")
            }
            OutlinedButton(
                onClick = onGoToTemplates,
                modifier = Modifier.weight(1f)
            ) {
                Text("Galeri Template")
            }
            OutlinedButton(
                onClick = { viewModel.resetWizard() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }

        val generated = viewModel.generatedAll
        val currentScore = viewModel.score
        if (generated.isNotEmpty() && currentScore != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Prompt Score: ${currentScore.score}/10",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (currentScore.missing.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Yang kurang:", style = MaterialTheme.typography.titleSmall)
                        currentScore.missing.forEach { item ->
                            Text(
                                text = "- ${item.labelId}: ${item.missingHint}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    val scoreV4 = viewModel.scoreV4
                    if (scoreV4 != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Skor Prompt v4: ${scoreV4.score}/5",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (scoreV4.missing.isNotEmpty()) {
                            scoreV4.missing.forEach { item ->
                                Text(
                                    text = "- ${item.labelId}: ${item.missingHint}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Hasil per engine (tap untuk ganti):", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                generated.forEachIndexed { index, prompt ->
                    FilterChip(
                        selected = viewModel.resultEngineIndex == index,
                        onClick = { viewModel.updateResultEngine(index) },
                        label = { Text("${prompt.engine.labelId} (${prompt.wordCount} kata)") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val active = generated[viewModel.resultEngineIndex]
            if (active.meter.isNotBlank()) {
                Text(text = active.meter, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (active.warnings.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        active.warnings.forEach { warning ->
                            Text(text = warning, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SelectionContainer {
                        Text(text = active.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (active.negativePrompt != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Negative prompt:", style = MaterialTheme.typography.titleSmall)
                        SelectionContainer {
                            Text(text = active.negativePrompt, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(active.text))
                        viewModel.saveCurrentToHistory()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salin + Simpan")
                }
                OutlinedButton(
                    onClick = { showProjectPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tambah ke Proyek")
                }
            }
        }

        if (showProjectPicker) {
            AlertDialog(
                onDismissRequest = { showProjectPicker = false },
                title = { Text("Tambah ke Proyek") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = sceneTitle,
                            onValueChange = { sceneTitle = it },
                            label = { Text("Judul scene") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newProjectTitle,
                            onValueChange = { newProjectTitle = it },
                            label = { Text("Buat proyek baru (judul)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Atau pilih proyek yang sudah ada:", style = MaterialTheme.typography.titleSmall)
                        viewModel.projects.forEach { project ->
                            TextButton(
                                onClick = {
                                    viewModel.addCurrentSceneToProject(project.id, sceneTitle)
                                    showProjectPicker = false
                                }
                            ) {
                                Text(project.title)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.createProject(newProjectTitle) { id ->
                                viewModel.addCurrentSceneToProject(id, sceneTitle)
                            }
                            showProjectPicker = false
                        }
                    ) {
                        Text("Buat + Tambah")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProjectPicker = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
