# Sistem Pakar Diagnosa Penyakit Infeksi dan Non-Infeksi

Sistem Pakar Diagnosa Penyakit Infeksi dan Non-Infeksi adalah sebuah aplikasi desktop berbasis Java (Swing) yang dirancang untuk membantu mendiagnosis penyakit berdasarkan gejala-gejala yang dialami pengguna. Sistem ini menggunakan dua metode inferensi:
1. **Rule-Based**: Diagnosa dilakukan jika *semua* gejala untuk sebuah penyakit terpenuhi.
2. **Bobot / Persentase**: Diagnosa dilakukan berdasarkan persentase kecocokan gejala (dengan threshold minimal 50%).

Aplikasi ini ditujukan untuk tugas mata kuliah **Kecerdasan Buatan (Artificial Intelligence)**.

## Fitur Utama

- **Data Master**:
  - **Data Penyakit**: Mengelola data penyakit infeksi (Influenza, Demam Berdarah) dan non-infeksi (Diabetes, Hipertensi).
  - **Data Gejala**: Mengelola data gejala-gejala medis.
- **Aturan (Rule)**:
  - **Data Gejala Penyakit**: Menentukan relasi aturan / *rule* antara gejala dengan penyakit yang sesuai.
- **Konsultasi**:
  - **Mulai Konsultasi**: Melakukan proses diagnosa dengan menjawab pertanyaan-pertanyaan (gejala) yang disediakan.
  - **Riwayat Konsultasi**: Melihat hasil konsultasi atau diagnosa yang telah dilakukan sebelumnya.

## Teknologi yang Digunakan

- **Bahasa Pemrograman**: Java 17
- **GUI Framework**: Java Swing (dengan custom JDesktopPane background)
- **Database**: SQLite (`sistem_pakar.db`)
- **Build Tool**: Gradle
- **Logging**: SLF4J

## Struktur Proyek

```text
source_code/
├── src/
│   ├── config/      # Konfigurasi Database (Koneksi SQLite)
│   ├── form/        # Tampilan Antarmuka (GUI / JFrame / JInternalFrame)
│   ├── model/       # Logika Sistem Pakar (ExpertSystemEngine.java)
│   └── MainApp.java # Entry Point Aplikasi
├── build.gradle     # Konfigurasi Gradle
├── sistem_pakar.db  # Database SQLite lokal
└── gradlew / gradlew.bat # Gradle Wrapper
```

## Prasyarat

- Java Development Kit (JDK) 17 atau yang lebih baru.
- Tidak perlu menginstal SQLite secara terpisah, karena sistem menggunakan driver SQLite JDBC yang ter-bundle via Gradle.

## Cara Menjalankan Aplikasi

1. Buka terminal atau command prompt dan arahkan ke direktori proyek (`source_code`).
2. Jalankan perintah Gradle berikut untuk melakukan kompilasi dan menjalankan aplikasi:
### Windows
   ```bash
   gradle run
   ```
### Linux
   ```bash
   ./gradlew run
   ```

3. Aplikasi Swing akan terbuka dan Anda bisa mulai melakukan Data Master manajemen atau mencoba fitur Konsultasi.

## Cara Build menjadi `.jar`

Jika ingin membangun aplikasi menjadi sebuah file `.jar` yang *executable*, jalankan:
```bash
./gradlew jar
```
(Gunakan `gradlew.bat jar` di Windows).
Hasil build akan berada di dalam folder `build/libs/`.

---
*Mata Kuliah Kecerdasan Buatan*
