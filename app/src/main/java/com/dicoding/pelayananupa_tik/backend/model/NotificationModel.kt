package com.dicoding.pelayananupa_tik.backend.model

import com.google.firebase.firestore.PropertyName

data class NotificationModel(
    val title: String = "",
    val body: String = "",
    val timestamp: String = "",
    @get:PropertyName("documentId") @set:PropertyName("documentId") var documentId: String = "",
    @get:PropertyName("collectionName") @set:PropertyName("collectionName") var collectionName: String = ""

)