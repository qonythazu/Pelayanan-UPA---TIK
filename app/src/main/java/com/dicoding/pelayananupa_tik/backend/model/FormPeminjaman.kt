package com.dicoding.pelayananupa_tik.backend.model

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
        return if (rentangTanggal.isNotEmpty()) {
            rentangTanggal.replace("-", " - ")
        } else {
            "Tanggal tidak tersedia"
        }
    }
}