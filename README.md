# Mesin Pencari Sederhana (Boolean & Tolerant Retrieval)

## Langkah-langkah menjalankan kode program: 
### 1. Clone Repositori
Buka terminal atau command prompt, lalu jalankan perintah berikut:
```bash
git clone [https://github.com/mkeaudric/Tugas1_IR.git](https://github.com/mkeaudric/Tugas1_IR.git)
cd Tugas1_IR
```

### 2. Kompilasi Program
javac *.java

### 3. Jalankan Program (Catatan: Jika menggunakan VS Code, bisa langsung menekan tombol 'Run' pada file SearchEngine.java)
java SearchEngine

### 4. Cara Penggunaan Program 
Setelah berhasil menjalankan program, ikuti langkah-langkah berikut 
1. **Input Query**: Masukkan kata kunci pencarian pada kolom yang tersedia.
2. **Format Query**: Sistem mendukung beberapa jenis format pencarian:
    *   **Pencarian Sederhana**: Masukkan satu kata atau frasa.
        > *Contoh: `aerodynamics` atau `supersonic wing`*
    *   **Boolean Query**: Gunakan operator `AND`, `OR`, `NOT`, dan tanda kurung `()` untuk logika pencarian yang lebih kompleks.
        > *Contoh: `sistem AND (informasi OR komputer) NOT perpustakaan`*
    *   **Tolerant Retrieval**: Jangan khawatir jika terjadi typo. Sistem akan otomatis menyarankan atau mencari kata yang paling mendekati menggunakan *Edit Distance*.
        > *Contoh: Jika mengetik `arodynmsi`, sistem tetap dapat menemukan dokumen `aerodynamics`.*
3. **Eksekusi**: Tekan tombol **Enter** untuk memulai pencarian.
4. **Hasil**: Program akan menampilkan tabel hasil yang berisi:
    *   **DOC ID**: Identitas dokumen yang relevan.
    *   **FREQ**: Jumlah frekuensi kemunculan kata kunci di dokumen tersebut.
    *   **TITLE**: Judul atau cuplikan isi dokumen.


