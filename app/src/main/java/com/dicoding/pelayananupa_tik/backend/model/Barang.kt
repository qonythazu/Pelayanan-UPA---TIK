package com.dicoding.pelayananupa_tik.backend.model

data class Barang(
    val namaBarang: String = "",
    val jenis: String = "",
    val status: String = "tersedia",
    val peminjam: String = "",
    val tanggalPinjam: String = "",
    val tanggalKembali: String = ""
)
