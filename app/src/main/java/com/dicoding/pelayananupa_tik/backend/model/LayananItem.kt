package com.dicoding.pelayananupa_tik.backend.model

data class LayananItem(
    val documentId: String = "",
    val judul: String = "",
    val tanggal: String = "",
    val status: String = "",
    val layanan: String = "",
    val jenis: String = "",
    val akun: String = "",
    val alasan: String = "",
    val filePath: String = "",
    val userEmail: String = "",
    val timestamp: String = "",
    val jumlah: String = "",
    val kontak: String = "",
    val tujuan: String = "",
    val keluhan: String = "",
    val formType: String = "" // "pemeliharaan_akun", "form_lain", etc.
)