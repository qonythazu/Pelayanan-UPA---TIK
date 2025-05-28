package com.dicoding.pelayananupa_tik.backend.model

data class Barang(
    val namaBarang: String = "",
    val jenis: String = "",
    val status: String = "tersedia",
    val peminjam: String = "",
    val peminjamanId: String = "",
    val tanggalPinjam: com.google.firebase.Timestamp? = null,
    val tanggalKembali: com.google.firebase.Timestamp? = null
)