package com.dicoding.pelayananupa_tik.backend.model

import java.text.SimpleDateFormat
import java.util.Locale

data class BarangDipinjam(
    val jenis: String = "",
    val nama: String = "",
    val namaBarang: String = "",
    val photoUrl: String = ""
) {
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
    val jenisBarang: String = "",
    val photoUrl: String = "",
    val tanggalMulai: Any? = null,
    val tanggalPengajuan: String = "",
    val tanggalSelesai: Any? = null
) {
    fun getNamaBarang(): String {
        return when {
            namaPerangkat.isNotEmpty() -> namaPerangkat
            barangDipinjam.isNotEmpty() -> {
                val barang = barangDipinjam.first()
                barang.getDisplayName()
            }
            else -> "Tidak diketahui"
        }
    }

    fun getDisplayJenisBarang(): String {
        return when {
            jenisBarang.isNotEmpty() -> jenisBarang
            barangDipinjam.isNotEmpty() -> barangDipinjam.first().jenis
            else -> "Tidak diketahui"
        }
    }

    fun getDisplayPhotoUrl(): String {
        return when {
            photoUrl.isNotEmpty() -> photoUrl
            barangDipinjam.isNotEmpty() -> barangDipinjam.first().photoUrl
            else -> ""
        }
    }

    fun getFormattedRentangTanggal(): String {
        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            if (rentangTanggal.isNotEmpty() && rentangTanggal != "Tanggal tidak tersedia") {
                return rentangTanggal.replace("-", " - ")
            }
            val startDateStr = when (tanggalMulai) {
                is com.google.firebase.Timestamp -> {
                    formatter.format(tanggalMulai.toDate())
                }
                is java.util.Date -> {
                    formatter.format(tanggalMulai)
                }
                is String -> {
                    try {
                        val parseFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = parseFormatter.parse(tanggalMulai)
                        date?.let { formatter.format(it) }
                    } catch (e: Exception) {
                        tanggalMulai.takeIf { it.isNotEmpty() }
                    }
                }
                else -> null
            }

            val endDateStr = when (tanggalSelesai) {
                is com.google.firebase.Timestamp -> {
                    formatter.format(tanggalSelesai.toDate())
                }
                is java.util.Date -> {
                    formatter.format(tanggalSelesai)
                }
                is String -> {
                    try {
                        val parseFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = parseFormatter.parse(tanggalSelesai)
                        date?.let { formatter.format(it) }
                    } catch (e: Exception) {
                        tanggalSelesai.takeIf { it.isNotEmpty() }
                    }
                }
                else -> null
            }

            when {
                startDateStr != null && endDateStr != null -> {
                    "$startDateStr - $endDateStr"
                }
                startDateStr != null -> {
                    "Mulai: $startDateStr"
                }
                endDateStr != null -> {
                    "Selesai: $endDateStr"
                }
                else -> {
                    "Tanggal tidak tersedia"
                }
            }
        } catch (e: Exception) {
            "Tanggal tidak tersedia"
        }
    }
}