package com.dicoding.pelayananupa_tik.backend.model

import com.google.firebase.firestore.PropertyName

data class NotificationModel(
    val title: String = "",
    val body: String = "",
    val timestamp: String = "",
    @get:PropertyName("document_id") @set:PropertyName("document_id") var documentId: String = ""
)