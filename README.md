# Nexus Recorder X

Aplikasi perekam layar Android (mirip X Recorder) dengan dukungan resolusi & FPS hingga 120,
audio internal/mikrofon, facecam overlay, dan tombol kontrol mengambang.

## ⚠️ Catatan Penting soal 120 FPS

Pilihan 120 fps tersedia di Pengaturan, tapi hasil rekaman nyata tetap dibatasi oleh:
1. Kemampuan hardware encoder video HP kamu (sebagian besar HP hanya stabil di 30-60fps).
2. Refresh rate layar HP (kalau layar HP cuma 60Hz/90Hz, konten yang direkam juga tidak akan
   benar-benar 120fps walau setting-nya 120).
Untuk hasil terbaik, gunakan device dengan layar & GPU 120Hz (flagship/gaming phone).

## Cara Build APK

### Opsi 1: Build Otomatis via GitHub Actions (disarankan)
1. Buat repository baru di GitHub.
2. Upload semua isi folder project ini ke repo tersebut (lewat web upload, GitHub Desktop, atau `git push`).
3. Buka tab **Actions** di repo → workflow "Build APK" akan otomatis berjalan.
4. Setelah selesai (~3-5 menit), buka hasil run-nya → bagian **Artifacts** → download `NexusRecorderX-debug-apk`.
5. Extract zip-nya, install file `.apk` di HP Android (aktifkan "Install dari sumber tidak dikenal").

### Opsi 2: Build via Android Studio
1. Buka Android Studio → **Open** → pilih folder project ini.
2. Tunggu proses Gradle Sync selesai (otomatis download dependency).
3. Klik **Run ▶** untuk langsung install ke HP/emulator, atau
   **Build → Build Bundle(s)/APK(s) → Build APK(s)** untuk hanya menghasilkan file APK.

## Fitur
- Rekam layar via MediaProjection + MediaRecorder (H.264, MP4)
- Pilihan resolusi: 360p s/d 4K
- Pilihan FPS: 24/30/60/90/120
- Pilihan bitrate: 4–50 Mbps
- Rekam audio mikrofon atau tanpa audio
- Tombol kontrol mengambang (pause/resume, stop, toggle facecam, timer) — bisa digeser bebas
- Facecam overlay (kamera depan) yang bisa di-toggle saat merekam
- Galeri hasil rekaman (preview thumbnail, putar, share, hapus)
- Notifikasi foreground service dengan tombol stop cepat

## Struktur Project
```
app/src/main/java/id/nexus/recorder/
 ├─ ui/         MainActivity, SettingsActivity, RecordingsActivity, RecordingAdapter
 ├─ service/    ScreenRecordService, FloatingControlService
 ├─ util/       Prefs (penyimpanan pengaturan)
 └─ model/      RecordingItem
```

## Izin yang Dibutuhkan
- Rekam layar (MediaProjection, diminta saat tekan tombol mulai)
- Mikrofon (kalau audio mic diaktifkan)
- Kamera (kalau facecam diaktifkan)
- Tampil di atas aplikasi lain / overlay (untuk tombol mengambang)
- Notifikasi (Android 13+)
