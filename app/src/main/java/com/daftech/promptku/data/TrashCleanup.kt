package com.daftech.promptku.data

// Logika bersih-bersih v1.4.4 — fungsi MURNI (tanpa Room) supaya bisa
// dites lokal. ViewModel hanya memanggil DAO + fungsi-fungsi ini.
// Aturan identitas: JUDUL = identitas film. Dua naskah / dua scene
// proyek bertitel sama = dua generasi film yang sama — hanya yang
// terbaru yang dipertahankan, sisanya sampah.
object TrashCleanup {

    // Scene proyek duplikat: tiap (projectId, judul) hanya baris
    // TERBARU (id terbesar) yang dipertahankan. Mengembalikan id
    // baris yang harus dihapus. Shot Wizard (judul unik) tidak
    // tersentuh — judulnya tidak pernah kembar dalam satu proyek.
    fun duplicateSceneIds(scenes: List<SceneItem>): List<Long> {
        return scenes.groupBy { it.projectId to it.title }
            .values.flatMap { group ->
                group.sortedByDescending { it.id }.drop(1).map { it.id }
            }
    }

    // Naskah basi: tiap judul hanya naskah TERBARU (id terbesar) yang
    // dipertahankan. Naskah lama bertitel sama = cache generasi
    // sebelumnya (misal film yang sama di-generate ulang) — dihapus
    // beserta scene naskahnya (FK CASCADE). Naskah bertitel unik
    // (termasuk draft kosong yang baru dibuat) tidak tersentuh.
    fun supersededScripts(scripts: List<ScriptItem>): List<ScriptItem> {
        return scripts.groupBy { it.title }
            .values.flatMap { group ->
                group.sortedByDescending { it.id }.drop(1)
            }
    }
}
