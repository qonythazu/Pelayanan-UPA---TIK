package com.dicoding.pelayananupa_tik.backend.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

data class BarangDipinjam(
    val jenis: String = "",
    val nama: String = "",
    val namaBarang: String = "",
    val photoUrl: String = "" // Tambahkan field photoUrl
) {
    // Helper method dengan nama berbeda
    fun getDisplayName(): String {
        return when {
            namaBarang.isNotEmpty() -> namaBarang
            nama.isNotEmpty() -> nama
            else -> ""
        }
    }
}

data class FormPeminjaman(
    val namaPerangkat: String = "",
    val barangDipinjam: List<BarangDipinjam> = emptyList(),
    val statusPeminjaman: String = "",
    val harapanAnda: String = "",
    val judul: String = "",
    val kontakPenanggungJawab: String = "",
    val namaPenanggungJawab: String = "",
    val rentangTanggal: String = "",

    // Field tanggal sebagai Timestamp untuk Firestore
    val tanggalMulai: Any? = null, // Bisa Timestamp atau String
    val tanggalPengajuan: String = "",
    val tanggalSelesai: Any? = null // Bisa Timestamp atau String
) {
    constructor() : this("", emptyList(), "", "", "", "", "", "", null, "", null)

    // Helper functions untuk adapter
    fun getNamaBarang(): String {
        return when {
            barangDipinjam.isNotEmpty() -> {
                val barang = barangDipinjam.first()
                barang.getDisplayName()
            }
            namaPerangkat.isNotEmpty() -> namaPerangkat
            else -> "Tidak diketahui"
        }
    }

    fun getJenisBarang(): String {
        return if (barangDipinjam.isNotEmpty()) {
            barangDipinjam.first().jenis
        } else {
            "Tidak diketahui"
        }
    }

    // Helper untuk mendapatkan photoUrl barang pertama
    fun getPhotoUrl(): String {
        return if (barangDipinjam.isNotEmpty()) {
            barangDipinjam.first().photoUrl
        } else {
            ""
        }
    }

    // Helper untuk format tanggal
    fun getFormattedTanggalMulai(): String {
        return formatTimestamp(tanggalMulai)
    }

    fun getFormattedTanggalSelesai(): String {
        return formatTimestamp(tanggalSelesai)
    }

    private fun formatTimestamp(timestamp: Any?): String {
        return when (timestamp) {
            is com.google.firebase.Timestamp -> {
                val date = timestamp.toDate()
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date)
            }
            is String -> timestamp
            else -> "Tanggal tidak tersedia"
        }
    }
}