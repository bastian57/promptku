package com.daftech.promptku.engine

// Filter kata mitos — riset 15 Nov 2026 (panduan resmi + studi 3 juta prompt).
// Kata-kata ini TIDAK menambah kualitas, hanya membuang anggaran token:
// resolusi ditentukan parameter model, bukan kata; "masterpiece" dan
// "trending on ArtStation" adalah fosil 2022 dari dunia gambar statis.
object MythFilter {

    // Daftar frasa terlarang — dipakai filter untuk MENGHAPUS, bukan untuk ditulis.
    // Template PromptKu tidak boleh mengandung frasa ini di output akhirnya.
    val bannedPhrases: List<String> = listOf(
        "8k",
        "4k",
        "ultra detailed",
        "ultra-detailed",
        "ultra realistic",
        "ultra-realistic",
        "hyperrealistic",
        "hyper-realistic",
        "masterpiece",
        "best quality",
        "trending on artstation"
    )

    private val bannedRegexes: List<Regex> = bannedPhrases.map { phrase ->
        Regex("(?i)\\b" + Regex.escape(phrase) + "\\b")
    }

    // Nama berlisensi yang rawan ditolak filter IP Flow/Veo.
    // App hanya MEMPERINGATKAN + menyarankan nama generik, tidak memblokir user.
    val licensedNames: List<String> = listOf(
        "goku", "son goku", "cell", "kamehameha", "vegeta", "gohan",
        "piccolo", "frieza", "krillin", "trunks", "android 18",
        "dragon ball", "naruto", "sasuke", "luffy", "one piece",
        "batman", "spider-man", "spiderman", "iron man", "superman",
        "mickey mouse", "pokemon", "pikachu", "gundam", "kamen rider"
    )

    fun filterMyths(text: String): String {
        var out = text
        for (regex in bannedRegexes) {
            out = regex.replace(out, "")
        }
        out = collapseCinematic(out)
        // Bersihkan sisa penghapusan TANPA memakan koma pemisah:
        // "kata ," -> "kata," dan ", ," -> "," — koma tunggal + spasi dipertahankan.
        return out.replace(Regex("\\s+"), " ")
            .replace(Regex(",\\s*,\\s*"), ", ")
            .replace(Regex("\\s+([.,])"), "$1")
            .trim()
    }

    // "Cinematic" boleh muncul SEKALI sebagai penanda gaya; pengulangan dibuang.
    fun collapseCinematic(text: String): String {
        val regex = Regex("(?i)\\bcinematic\\b")
        var seen = false
        return regex.replace(text) { match ->
            if (seen) "" else {
                seen = true
                match.value
            }
        }
    }

    fun countWords(text: String): Int {
        val clean = text.trim()
        if (clean.isEmpty()) return 0
        return clean.split(Regex("\\s+")).size
    }

    // Pangkas ke batas kata dengan membuang kalimat dari AKHIR — kalimat
    // pertama (subjek + aksi) tidak pernah dibuang, karena itu yang paling
    // tidak boleh hilang kalau encoder memotong diam-diam.
    fun trimToWords(text: String, maxWords: Int): String {
        if (countWords(text) <= maxWords) return text
        val sentences = text.split(Regex("(?<=[.!])\\s+")).toMutableList()
        while (sentences.size > 1 && countWords(sentences.joinToString(" ")) > maxWords) {
            sentences.removeAt(sentences.size - 1)
        }
        var out = sentences.joinToString(" ")
        if (countWords(out) > maxWords) {
            out = out.split(Regex("\\s+")).take(maxWords).joinToString(" ")
        }
        return out.trim()
    }

    // Deteksi nama berlisensi di teks user (nama karakter / cerita).
    fun detectLicensed(text: String): List<String> {
        val lower = text.lowercase()
        return licensedNames.filter { lower.contains(it) }.distinct()
    }

    fun genericSuggestion(name: String): String {
        return when (name.lowercase()) {
            "goku", "son goku" -> "the hero"
            "cell" -> "the bio-android villain"
            "kamehameha" -> "blue energy beam"
            "vegeta" -> "the proud rival warrior"
            "gohan" -> "the boy"
            "piccolo" -> "the green warrior"
            "frieza" -> "the alien tyrant"
            else -> "a generic character"
        }
    }
}
