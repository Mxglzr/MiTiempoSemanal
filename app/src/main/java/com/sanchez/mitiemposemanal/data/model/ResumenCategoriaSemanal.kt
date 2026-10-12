package com.sanchez.mitiemposemanal.data.model

import java.util.Locale

data class ResumenCategoriaSemanal(
    val categoriaId: Long,
    val categoriaNombre: String,
    val totalSegundos: Long
) {
    /**
     * Total de minutos acumulados en la semana.
     */
    val totalMinutos: Double
        get() = totalSegundos / 60.0

    /**
     * Total de horas acumuladas en la semana.
     */
    val totalHoras: Double
        get() = totalSegundos / 3600.0

    /**
     * Porcentaje sobre las 168 horas semanales (10 080 minutos).
     * Fórmula oficial: minutos ÷ 10 080 × 100
     */
    val porcentaje: Double
        get() = (totalMinutos / 10080.0) * 100.0

    /**
     * Retorna el porcentaje formateado con 1 decimal (ejemplo: "12.5 %").
     */
    fun porcentajeFormateado(): String {
        return String.format(Locale.US, "%.1f %%", porcentaje)
    }

    /**
     * Retorna las horas acumuladas formateadas con 1 decimal (ejemplo: "21.0 h").
     */
    fun horasFormateadas(): String {
        return String.format(Locale.US, "%.1f h", totalHoras)
    }
}
