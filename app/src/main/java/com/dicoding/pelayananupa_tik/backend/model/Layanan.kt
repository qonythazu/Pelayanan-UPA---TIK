package com.dicoding.pelayananupa_tik.backend.model

data class Layanan(
    val id: String = "",
    val title: String = "",
    val endpoint: String = "",
    val method: String = "",
    val authorization: String = "",
    val response: Map<String, Any> = emptyMap()
)
