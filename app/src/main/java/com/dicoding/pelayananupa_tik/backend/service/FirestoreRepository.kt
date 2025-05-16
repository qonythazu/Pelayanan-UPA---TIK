package com.dicoding.pelayananupa_tik.backend.service

import com.dicoding.pelayananupa_tik.backend.model.Layanan
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // Create or Update
    fun addLayanan(layanan: Layanan, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("layanan").document(layanan.id)
            .set(layanan)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Read (Get All Data)
    fun getLayananList(onSuccess: (List<Layanan>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("layanan").get()
            .addOnSuccessListener { result ->
                val list = result.map { it.toObject<Layanan>() }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Read (Get Single Data by ID)
    fun getLayananById(id: String, onSuccess: (Layanan?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("layanan").document(id).get()
            .addOnSuccessListener { document ->
                onSuccess(document.toObject<Layanan>())
            }
            .addOnFailureListener { onFailure(it) }
    }

    // Delete
    fun deleteLayanan(id: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("layanan").document(id).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}