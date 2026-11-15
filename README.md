# PromptKu v1.0.0 — Studio Prompt Sinematik

APK Android Kotlin native, offline-first. User pilih genre + gaya visual,
app merakit prompt video AI (struktur 9 lapis DANCUK Cinematic v2) siap
tempel ke Flow/Veo, WanGP, Kling, atau Seedance. Input Bahasa Indonesia,
prompt output Bahasa Inggris.

## Yang dilakukan app ini

- Wizard Sutradara 5 langkah: Genre (10) -> Gaya Visual (6) -> Tokoh + Dunia (DNA Lock) -> Shot -> Cerita/Beat
- Mesin terjemah engine: satu ide jadi 4 varian prompt (Flow, WanGP, Kling, Seedance)
- DNA Lock Library: karakter + dunia disimpan verbatim, dipakai ulang di semua scene
- Prompt Score v2: checklist 10 poin + baris yang kurang, sebelum generate
- Dadu Sutradara + Galeri Template (13 template, termasuk preset Goku vs Cell)
- Proyek Film multi-scene + ekspor .txt ke folder Download
- Riwayat offline (Room), cari / duplikat / salin ulang

## Yang TIDAK dilakukan app ini (v1)

- Tidak generate video. Generate tetap di Flow / WanGP Kaggle / Kling / Seedance.
- Tidak upload / analisa video. Tidak ada TTS. Tidak ada edit video.
- Tidak ada AI online / enhancer / API key / login / akun / cloud.
- Alat pribadi dulu, tanpa lisensi device-bound.

## Mesin prompt (aturan dari riset 15 Nov 2026)

- Struktur 9 lapis: Shot dulu -> Subject DNA verbatim -> Action 1 aksi ->
  Camera 1 gerakan -> World -> Light bernama -> Material+Physics ->
  Color/Film -> Audio. Global Lock dicopas identik tiap prompt.
- Flow/Veo: Cinematography + Subject + Action + Context + Style, timestamp
  [00:00-00:05], negative ditulis positif. Target 40-200 kata.
- WanGP: Subject + Scene + Motion saja (gambar mengunci subjek), target
  80-120 kata (batas keras encoder 512 token), negative field terpisah
  Mandarin verbatim. Tanpa audio.
- Kling: Subject + Action + Setting + Camera + Lighting + Mood, dialog
  `Nama (tone): "..."`. Target 60-100 kata.
- Seedance: 8 elemen berurutan + timeline 3 segmen, TIDAK ada negative
  prompt (pakai constraints). Target 60-100 kata.
- Filter kata mitos otomatis: 8K, 4K, ultra detailed, masterpiece,
  best quality, trending on ArtStation, hyperrealistic, dan pengulangan
  kata cinematic — dihapus, diganti slot konkret (lensa, cahaya, kamera).
- Peringatan filter IP: nama berlisensi bisa ditolak Flow/Veo — app
  menyarankan nama generik, tidak memblokir user.

## Build (GitHub Actions)

Push ke branch `main` -> workflow `.github/workflows/promptku-build.yml`
jalan: unit test -> build debug + release APK -> gerbang fingerprint
keystore + gerbang sertifikat APK (wajib kunci tetap Putra) -> upload
ke rolling release `latest` sebagai `PromptKu-v1.0.0.apk`.

Pin yang terbukti: compileSdk 35, minSdk 26, targetSdk 34, AGP 8.7.3,
Gradle 8.9, Kotlin 1.9.25, Room 2.6.1. Jangan naikkan diam-diam.

## Signing (kunci tetap)

Keystore TIDAK ada di repo. Di Actions, secret `KEYSTORE_BASE64`
(nilai keystore yang SAMA dengan SuaraKu/ClipKu/UnduhKu) didecode ke
`signing/promptku-tetap.keystore` di workspace, lalu gerbang keras
memeriksa fingerprint-nya sebelum build:

`0A:A5:2A:F9:10:17:42:AE:5D:5A:13:80:F7:F2:E8:A4:F1:F5:88:03:80:6F:B6:09:43:72:D3:3C:78:10:30:20`

Sertifikat APK harus `0aa52af9101742ae5d5a1380f7f2e8a4f1f58803806fb6094372d33c78103020`.
Kalau tidak cocok, build dibatalkan — signature tidak boleh acak lagi.

## Cara push pertama (buat Putra)

1. Buat repo baru di GitHub: `bastian57/promptku` (public, tanpa README).
2. Di repo itu: Settings -> Secrets -> Actions -> New secret
   `KEYSTORE_BASE64` (isi: base64 keystore tetap yang sama seperti SuaraKu).
   Jangan paste di web UI kalau file besar — pakai `gh secret set` dari Termux.
3. Di Termux (pakai data seluler, jangan /tmp):
   `cd ~/promptku && git init && git add -A && git commit -m "PromptKu v1.0.0" && git branch -M main && git remote add origin https://github.com/bastian57/promptku.git && git push -u origin main`
4. Cek tab Actions sampai hijau, lalu cek release `latest`.
5. Download APK: HAPUS dulu salinan lama bernama sama di HP, unduh TANPA
   resume, cek ukuran byte + SHA-256 + sertifikat sebelum install.

## Struktur source

- `app/src/main/java/com/daftech/promptku/engine/` — mesin prompt murni
  (Models, MythFilter, PromptEngine, PromptScore, Templates, Dice, ExportText)
- `app/src/main/java/com/daftech/promptku/data/` — Room + ekspor Downloads
- `app/src/main/java/com/daftech/promptku/ui/` — ViewModel + layar Compose
- `app/src/test/` — unit test JUnit mesin prompt
