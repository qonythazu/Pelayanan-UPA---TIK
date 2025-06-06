package com.dicoding.pelayananupa_tik.backend.model

data class LayananItem(
    val documentId: String = "",
    val judul: String = "",
    val tanggal: String = "",
    val status: String = "",
    // Data untuk form pemeliharaan akun
    val layanan: String = "",
    val jenis: String = "",
    val akun: String = "",
    val alasan: String = "",
    val filePath: String = "",
    val userEmail: String = "",
    val timestamp: String = "",
    // Untuk form lainnya, bisa tambah field sesuai kebutuhan
    val formType: String = "" // "pemeliharaan_akun", "form_lain", etc.
)