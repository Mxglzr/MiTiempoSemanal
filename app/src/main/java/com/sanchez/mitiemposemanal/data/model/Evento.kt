package com.sanchez.mitiemposemanal.data.model

data class Evento(
    val id: Long = 0,
    val tareaId: Long,
    val tareaNombre: String = "",
    val categoriaNombre: String = "",
    val fechaInicio: String,
    val fechaFin: String? = null,
    val duracionSegundos: Long = 0
) {
    /**
     * Retorna la duración en un formato amigable como "1 h 25 min", "45 min" o "30 s".
     */
    fun duracionFormateada(): String {
        if (duracionSegundos <= 0) return "0 s"
        val horas = duracionSegundos / 3600
        val minutos = (duracionSegundos % 3600) / 60
        val segundos = duracionSegundos % 60

        return when {
            horas > 0 && minutos > 0 -> "${horas}h ${minutos}m"
            horas > 0 -> "${horas}h"
            minutos > 0 && segundos > 0 -> "${minutos}m ${segundos}s"
            minutos > 0 -> "${minutos}m"
            else -> "${segundos}s"
        }
    }
}
