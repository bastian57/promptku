package com.daftech.promptku.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Pelajaran Antartika (16 Nov 2026): substring menelan kata lain.
// "Antartika" mengandung "antar"; "lautan" mengandung "laut".
class TokenBoundaryTest {

    @Test
    fun antartikaDoesNotContainAntar() {
        assertFalse(TokenBoundary.containsToken("Antartika", "antar"))
        assertFalse(
            TokenBoundary.containsAnyToken(
                "Pertempuran di atas lautan Antartika",
                listOf("antar", "kurir", "paket")
            )
        )
    }

    @Test
    fun lautanDoesNotContainLaut() {
        assertFalse(TokenBoundary.containsToken("lautan", "laut"))
        assertTrue(TokenBoundary.containsToken("di laut lepas", "laut"))
    }

    @Test
    fun courierWordAsTokenStillMatches() {
        assertTrue(
            TokenBoundary.containsAnyToken(
                "Kurir mengantar paket ke pelanggan",
                listOf("kurir", "paket", "antar")
            )
        )
        assertTrue(TokenBoundary.containsToken("jasa antar paket", "antar"))
    }

    @Test
    fun assimilatedMeFormsNeedExplicitForms() {
        // "meninju" TIDAK mengandung "tinju" (t->n); "memukul" TIDAK
        // mengandung "pukul" (p->m). Daftar kunci harus memuat bentuk
        // berasimilasi itu sendiri.
        assertFalse(TokenBoundary.containsToken("meninju", "tinju"))
        assertFalse(TokenBoundary.containsToken("memukul", "pukul"))
        assertFalse(TokenBoundary.containsToken("menendang", "tendang"))
        assertFalse(TokenBoundary.containsToken("menembak", "tembak"))
        assertTrue(
            TokenBoundary.containsAnyToken(
                "Dia meninju dan memukul lawannya",
                listOf("meninju", "memukul", "menendang", "menembak")
            )
        )
    }

    @Test
    fun phraseMatchesTokenSequenceOnly() {
        assertTrue(TokenBoundary.containsPhrase("tukang tambal ban itu", "tambal ban"))
        assertFalse(TokenBoundary.containsPhrase("tambal di ban bocor", "tambal ban"))
        assertTrue(TokenBoundary.containsPhrase("kutub selatan yang dingin", "kutub selatan"))
    }

    @Test
    fun punctuationIsIgnored() {
        assertTrue(TokenBoundary.containsToken("Antartika!", "antartika"))
        assertTrue(TokenBoundary.containsToken("(Bejo) melawan.", "bejo"))
    }
}
