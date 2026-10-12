package com.sanchez.mitiemposemanal.data.model

data class Categoria(
    val id: Long = 0,
    val nombre: String
) {
    override fun toString(): String {
        return nombre
    }
}
