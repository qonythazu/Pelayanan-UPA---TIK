package com.dicoding.pelayananupa_tik.helper

data class Triplet<out A, out B, out C>(
    val first: A,
    val second: B,
    val third: C
)