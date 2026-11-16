package com.daftech.promptku.engine

// TokenBoundary — pencocokan kata BERBATAS TOKEN, pengganti bare
// `String.contains()` untuk kata kunci Indonesia.
//
// PELAJARAN ANTARTIKA (16 Nov 2026): `contains("antar")` menelan
// "Ant**antar**tika" jadi cerita kurir; `contains("laut")` menelan
// "lautan" jadi Pantai. Aturan di file ini:
//   - token = kata utuh (huruf/angka), dipisah spasi + tanda baca;
//   - "meninju" TIDAK mengandung "tinju" (asimilasi nasal me- t→n) —
//     jadi bentuk berasimilasi WAJIB ditulis eksplisit di daftar kunci
//     ("mukul", "ninju", "nendang", "nembak"), bukan diharap dari
//     substring;
//   - frasa ("tambal ban", "kutub selatan") dicocokkan sebagai urutan
//     token, bukan substring.
object TokenBoundary {

    private val SPLIT = Regex("[^a-z0-9]+")

    /** Token lowercase dari teks. "Antartika!" -> ["antartika"]. */
    fun tokenize(text: String): List<String> =
        text.lowercase().split(SPLIT).filter { it.isNotEmpty() }

    /** TRUE kalau token persis ada di teks ("laut" TIDAK cocok "lautan"). */
    fun containsToken(text: String, token: String): Boolean =
        tokenize(text).contains(token.lowercase().trim())

    /** TRUE kalau SATU PUN token kunci ada. */
    fun containsAnyToken(text: String, keywords: Collection<String>): Boolean {
        val tokens = tokenize(text).toSet()
        return keywords.any { tokens.contains(it.lowercase().trim()) }
    }

    /** TRUE kalau frasa muncul sebagai urutan token berurutan
     *  ("tambal ban" cocok "tukang tambal ban itu", tidak cocok
     *  "tambal di ban" — urutan token harus berurutan). */
    fun containsPhrase(text: String, phrase: String): Boolean {
        val tokens = tokenize(text)
        val want = tokenize(phrase)
        if (want.isEmpty() || want.size > tokens.size) return false
        for (i in 0..tokens.size - want.size) {
            if (tokens.subList(i, i + want.size) == want) return true
        }
        return false
    }

    /** TRUE kalau ada kunci (token tunggal atau frasa) yang cocok. */
    fun containsAny(text: String, keywords: Collection<String>): Boolean {
        val tokens = tokenize(text)
        val tokenSet = tokens.toSet()
        for (key in keywords) {
            val k = key.lowercase().trim()
            if (k.isEmpty()) continue
            if (' ' in k) {
                if (containsPhrase(text, k)) return true
            } else if (tokenSet.contains(k)) {
                return true
            }
        }
        return false
    }
}
