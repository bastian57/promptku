package com.daftech.promptku.engine

// PromptCore v1.3 — struktur kanonik 12 slot (desain prompt-structure-v3).
// PromptKu berhenti menulis paragraf dan mulai menyusun dokumen film:
// tiap prompt diurai ke slot dengan anggaran kata, dunia & audio
// DILINDUNGI (slot rakus hanya bisa memakan jatahnya sendiri).
// Urutan kanonik teks: SHOT → SUBJECT → ACTION → WORLD → LIGHT →
// CAMERA → DIALOGUE → AUDIO → STYLE → CONTINUITY (+ NEGATIVE terpisah).
// Anggaran per slot dari desain v3 (Flow target 100 kata, keras 180);
// batas keras TRIM tetap engine.maxWords (Flow 200, Kling/Seedance 100,
// Wan 120) — meter memakai batas trim sebagai patokan merah total.
enum class SlotStatus { GREEN, YELLOW, RED }

data class PromptSlots(
    val format: String = "",
    val shot: String = "",
    val subject: String = "",
    val action: String = "",
    val world: String = "",
    val light: String = "",
    val camera: String = "",
    val dialogue: String = "",
    val audio: String = "",
    val style: String = "",
    val continuity: String = "",
    val negative: String = ""
)

data class SlotBudget(
    val slot: String,
    val words: Int,
    val budget: Int,
    val status: SlotStatus
)

object PromptCore {

    // Anggaran per slot (desain v3, target Flow 100 kata).
    // LIGHT + CAMERA + STYLE + CONTINUITY berbagi jatah "gaya" 15 kata
    // + frasa kontinuitas yang dilindungi — di meter digabung ke GAYA.
    // DIALOGUE dihitung dari durasi klip (2,5 kata/detik, 10 dtk -> 25).
    fun slotBudgets(engine: TargetEngine, clipSeconds: Float = 10f): Map<String, Int> {
        val dialogueBudget = (clipSeconds * 2.5f).toInt().coerceAtLeast(5)
        return when (engine) {
            TargetEngine.FLOW -> mapOf(
                "SHOT" to 15, "SUBJECT" to 25, "ACTION" to 15, "WORLD" to 20,
                "DIALOGUE" to dialogueBudget, "AUDIO" to 10, "GAYA" to 15
            )
            TargetEngine.KLING -> mapOf(
                "SHOT" to 12, "SUBJECT" to 12, "ACTION" to 12, "WORLD" to 12,
                "DIALOGUE" to dialogueBudget, "AUDIO" to 8, "GAYA" to 12
            )
            TargetEngine.SEEDANCE -> mapOf(
                "SHOT" to 12, "SUBJECT" to 12, "ACTION" to 12, "WORLD" to 12,
                "DIALOGUE" to dialogueBudget, "AUDIO" to 8, "GAYA" to 12
            )
            TargetEngine.WAN -> mapOf(
                "SHOT" to 10, "SUBJECT" to 12, "ACTION" to 15, "WORLD" to 15,
                "DIALOGUE" to 0, "AUDIO" to 8, "GAYA" to 12
            )
        }
    }

    // Target kerja total per engine (bukan batas keras trim).
    fun targetWords(engine: TargetEngine): Int = when (engine) {
        TargetEngine.FLOW -> 100
        TargetEngine.KLING -> 80
        TargetEngine.SEEDANCE -> 80
        TargetEngine.WAN -> 100
    }

    // Urai input ke slot-slot kanonik. SUBJECT memakai kartu 3 jangkar
    // bila ada (biaya ~15 kata, tidak pernah kena cap DNA); bila tidak,
    // DNA verbatim PENUH — meter menunjukkan biaya mentahnya, supaya user
    // melihat kenapa DNA dipangkas (laporan trim di finish() yang menjelaskan).
    fun slotsFor(input: ShotInput, engine: TargetEngine): PromptSlots {
        val style = input.style ?: VisualStyle.REALISTIC
        val look = PromptEngine.styleLook(style)
        val shot = input.shotSize?.phraseEn ?: "medium shot"
        val angle = input.angle?.phraseEn ?: "eye level"
        val lens = "${input.lensMm ?: 35}mm"
        val camera = input.cameraMove?.phraseEn ?: "static locked camera"
        val light = input.light?.phraseEn ?: LightKind.GOLDEN_HOUR.phraseEn
        val action = input.actionEn.ifBlank { "stands still, breathing slowly" }
        val poseStart = input.poseStart.ifBlank { "standing still" }
        val poseEnd = input.poseEnd.ifBlank { "standing firm" }
        val world = input.world?.descriptionEn
            ?: PromptEngine.defaultWorld(input.genre ?: Genre.DRAMA)
        val audio = input.audioEn.ifBlank { "wind, quiet ambience" }
        val subject = when {
            input.characterCard != null && !input.characterCard.isEmpty() -> {
                val name = input.character?.name ?: "a person"
                val second = input.secondCharacter?.let { ", facing ${it.name}" } ?: ""
                "$name, ${input.characterCard.toDescription()}$second"
            }
            input.character != null -> {
                val second = input.secondCharacter?.let { ", facing ${it.name}, ${it.descriptionEn}" } ?: ""
                "${input.character.name}, ${input.character.descriptionEn}$second"
            }
            else -> "a person"
        }
        val dialogue = PromptEngine.renderDialogue(input, engine)
        val continuity = buildString {
            if (input.screenDirection.isNotBlank()) append("Screen direction: ${input.screenDirection.trim()}. ")
            if (input.axisSide.isNotBlank()) append("camera stays on the ${input.axisSide.trim()} side of the axis. ")
            if (input.transition.isNotBlank()) append("Transition: ${input.transition.trim()}.")
        }.trim()
        val negative = when (engine) {
            TargetEngine.WAN -> PromptEngine.WAN_NEGATIVE_MANDARIN
            TargetEngine.FLOW -> PromptEngine.FLOW_POSITIVE_LOCK
            else -> PromptEngine.POSITIVE_LOCK_SHORT
        }
        return PromptSlots(
            format = "${engine.labelId} • 10 dtk • 24fps",
            shot = "$shot, $angle, $lens",
            subject = subject,
            // v1.3.1 — segmen 10 dtk (batas 8 dtk Veo pensiun bersama
            // Veo 3.1, 5 Des 2026; default Flow kini Omni Flash 1.2).
            action = "[00:00-00:05] $poseStart, $action begins. " +
                "[00:05-00:10] The action completes as $poseEnd.",
            world = world,
            light = light,
            camera = camera,
            dialogue = dialogue,
            audio = audio,
            style = "${look.second}, ${look.third}",
            continuity = continuity,
            negative = negative
        )
    }

    // Meter per slot: kata aktual vs anggaran -> status.
    // GREEN <= anggaran; YELLOW <= 1,5x anggaran; RED di atas itu.
    fun meterFor(input: ShotInput, engine: TargetEngine, clipSeconds: Float = 10f): List<SlotBudget> {
        val slots = slotsFor(input, engine)
        val budgets = slotBudgets(engine, clipSeconds)
        val counts = mapOf(
            "SHOT" to MythFilter.countWords(slots.shot),
            "SUBJECT" to MythFilter.countWords(slots.subject),
            "ACTION" to MythFilter.countWords(slots.action),
            "WORLD" to MythFilter.countWords(slots.world),
            "DIALOGUE" to MythFilter.countWords(slots.dialogue),
            "AUDIO" to MythFilter.countWords(slots.audio),
            "GAYA" to MythFilter.countWords("${slots.light} ${slots.camera} ${slots.style} ${slots.continuity}")
        )
        return budgets.map { (slot, budget) ->
            val words = counts[slot] ?: 0
            val status = when {
                words <= budget -> SlotStatus.GREEN
                words <= (budget * 1.5).toInt() -> SlotStatus.YELLOW
                else -> SlotStatus.RED
            }
            SlotBudget(slot, words, budget, status)
        }
    }

    // Satu baris meter, selalu terlihat di UI: "SHOT 8/15 • SUBJECT 18/25
    // • ACTION 12/15 • WORLD 16/20 • DIALOGUE 6/25 • AUDIO 7/10 •
    // GAYA 14/15 • TOTAL 132/200". Slot yang lewat anggaran ditandai "!".
    fun meterLine(input: ShotInput, engine: TargetEngine, clipSeconds: Float = 10f): String {
        val meter = meterFor(input, engine, clipSeconds)
        val total = meter.sumOf { it.words }
        val parts = meter.map { item ->
            val mark = if (item.status == SlotStatus.GREEN) "" else "!"
            "${item.slot} ${item.words}/${item.budget}$mark"
        }
        return (parts + "TOTAL $total/${engine.maxWords}").joinToString(" • ")
    }

    // Slot mana yang makan jatah terbesar (untuk "kenapa prompt ini gemuk").
    fun hungriestSlot(input: ShotInput, engine: TargetEngine, clipSeconds: Float = 10f): SlotBudget? {
        return meterFor(input, engine, clipSeconds)
            .maxByOrNull { it.words.toDouble() / it.budget.coerceAtLeast(1) }
    }
}

// Matriks bahasa dialog ↔ engine (desain v3 §2.2 / pitch §3.3).
// Ditegakkan di UI + Score v4 aturan 5 — bukan di builder (builder tetap
// merender apa yang diminta user, tapi dengan peringatan yang jelas).
enum class DialogueCheck { ALLOWED, WARN, BLOCKED, SILENT }

data class DialogueMatrixResult(
    val check: DialogueCheck,
    val message: String
)

object DialogueMatrix {
    fun check(engine: TargetEngine, lang: DialogueLang): DialogueMatrixResult {
        return when (engine) {
            TargetEngine.SEEDANCE -> DialogueMatrixResult(
                DialogueCheck.ALLOWED,
                "Seedance 2.5 mendukung dialog ${lang.labelId} secara resmi."
            )
            TargetEngine.FLOW -> when (lang) {
                DialogueLang.EN -> DialogueMatrixResult(
                    DialogueCheck.ALLOWED,
                    "Flow (Omni Flash 1.2): dialog Inggris didukung."
                )
                DialogueLang.ID -> DialogueMatrixResult(
                    DialogueCheck.WARN,
                    "Dialog Indonesia di Flow belum teruji — tes 1 klip dulu sebelum batch."
                )
            }
            TargetEngine.KLING -> when (lang) {
                DialogueLang.EN -> DialogueMatrixResult(
                    DialogueCheck.ALLOWED,
                    "Kling: dialog Inggris didukung."
                )
                DialogueLang.ID -> DialogueMatrixResult(
                    DialogueCheck.WARN,
                    "Dialog Indonesia di Kling belum teruji — tes 1 klip dulu sebelum batch."
                )
            }
            TargetEngine.WAN -> DialogueMatrixResult(
                DialogueCheck.SILENT,
                "WanGP bisu — dialog tidak masuk prompt, pakai Lembar Suara Pasca-Produksi (TTS/dubbing di CapCut)."
            )
        }
    }
}
