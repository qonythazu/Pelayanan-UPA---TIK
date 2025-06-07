package com.dicoding.pelayananupa_tik.backend.model


data class Barang(
    var namaBarang: String = "",
    var jenis: String = "",
    var status: String = "tersedia",
    val peminjam: String = "",
    val peminjamanId: String = "",
    val tanggalPinjam: com.google.firebase.Timestamp? = null,
    val tanggalKembali: com.google.firebase.Timestamp? = null,
    var photoUrl: String = "" // Tambahan field untuk URL gambar
)