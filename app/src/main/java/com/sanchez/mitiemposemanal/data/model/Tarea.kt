package com.sanchez.mitiemposemanal.data.model

data class Tarea(
    val id: Long = 0,
    val nombre: String,
    val categoriaId: Long,
    val categoriaNombre: String = ""
)
