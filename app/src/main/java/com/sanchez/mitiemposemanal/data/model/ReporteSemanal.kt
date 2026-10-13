package com.sanchez.mitiemposemanal.data.model

import java.util.Locale

data class ReporteSemanal(
    val id: Long = 0,
    val fechaInicio: String,
    val fechaFin: String,
    val etiquetaRango: String,
    val totalSegundosRegistrados: Long,
    val horasSinRegistrar: Double,
    val porcentajeSinRegistrar: Double,
    val categorias: List<ReporteCategoria> = emptyList()
) {
    override fun toString(): String {
        return etiquetaRango
    }
}

data class ReporteCategoria(
    val id: Long = 0,
    val reporteId: Long = 0,
    val categoriaId: Long,
    val categoriaNombre: String,
    val totalSegundos: Long,
    val horas: Double,
    val porcentaje: Double
) {
    fun horasFormateadas(): String {
        return String.format(Locale.US, "%.1f h", horas)
    }

    fun porcentajeFormateado(): String {
        return String.format(Locale.US, "%.1f %%", porcentaje)
    }
}
