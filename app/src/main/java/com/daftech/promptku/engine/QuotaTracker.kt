package com.daftech.promptku.engine

// QuotaTracker — hitung kuota harian per provider (Kotlin murni).
// Penyimpanan (tanggal + hitungan) ada di data layer; di sini hanya
// aritmetika + rantai fallback. Angka batas dari riset 16 Nov 2026:
// Gemini 3.8 Flash ~20 req/hari, Flash-Lite 500, Groq 1.000.
// Satu film ≈ 7 panggilan: 2 vision + 1 director + ~4 prompter
// (batch 3–4 scene). Estimasi film pakai 7 supaya tidak overjanji.

typealias QuotaCounts = Map<V2Provider, Int>

object QuotaTracker {

    /** Sisa kuota provider hari ini (tidak pernah negatif). */
    fun remaining(provider: V2Provider, counts: QuotaCounts): Int =
        (provider.dailyLimit - (counts[provider] ?: 0)).coerceAtLeast(0)

    /** TRUE kalau provider masih bisa dipanggil hari ini. */
    fun canCall(provider: V2Provider, counts: QuotaCounts): Boolean =
        remaining(provider, counts) > 0

    /** Hitungan baru setelah 1 panggilan berhasil. */
    fun record(counts: QuotaCounts, provider: V2Provider): QuotaCounts {
        val next = counts.toMutableMap()
        next[provider] = (next[provider] ?: 0) + 1
        return next
    }

    /** Provider pertama di rantai yang masih punya kuota, atau null. */
    fun nextAvailable(chain: List<V2Provider>, counts: QuotaCounts): V2Provider? =
        chain.firstOrNull { canCall(it, counts) }

    /**
     * Perkiraan film penuh yang masih bisa dibuat hari ini.
     * Vision butuh 2 panggilan Groq; director+prompter butuh ~5 panggilan
     * dari rantai director (Flash -> Flash-Lite -> Groq).
     */
    fun filmsEstimate(counts: QuotaCounts): Int {
        val visionCalls = remaining(V2Provider.GROQ_VISION, counts) / 2
        val directorCalls = V2Provider.directorChain().sumOf { remaining(it, counts) } / 5
        return minOf(visionCalls, directorCalls).coerceAtLeast(0)
    }
}
