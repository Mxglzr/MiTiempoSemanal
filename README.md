# MiTiempoSemanal - Sprint 3: Porcentaje Semanal por Categoría

Tercera entrega del proyecto **MiTiempoSemanal**, introduciendo el cálculo matemático y visual de la distribución del tiempo semanal y el Dashboard de Inicio.

## 📌 Datos de Entrega
- **Estudiante:** Miguel Sánchez
- **Rama:** `Sprint3`
- **Versión:** `v0.3.0` (`versionCode = 3`)
- **Application ID:** `com.senati.mitiemposemanal.msanchez`
- **Mensaje de Commit:** `Sprint 3: porcentaje semanal`

---

## 🚀 Historias de Usuario Implementadas

### HU-06: Porcentaje semanal por categoría
- **Base oficial de 168 horas:** Cálculo semanal estricto sobre 168 horas (10 080 minutos = 604 800 segundos), que corresponden a una semana completa (Lunes 00:00:00 a Domingo 23:59:59).
- **Fórmula reglamentaria:** `minutos ÷ 10 080 × 100` con visualización a un decimal (ej. `12.5 %`).
- **Cálculo SQL agrupado:** Los totales se obtienen directamente mediante consulta SQL utilizando `LEFT JOIN`, `SUM(duracion_segundos)` y `GROUP BY`, sin sumar en bucles en memoria.
- **Filas dinámicas:**
  - Incluye todas las categorías existentes (incluso con `0.0 h (0.0 %)`).
  - Fila obligatoria **"Sin registrar"** que representa el saldo restante ($168\text{ h} - \sum\text{horas registradas}$).
- **Barras de progreso proporcionales:** Indicadores visuales con escala milimétrica para precisión decimal.
- **Actualización reactiva:** El panel se actualiza de inmediato al finalizar una actividad sin recargar la pantalla.

### Pestaña Inicio (Dashboard Principal)
- **Tarjetas KPI:**
  - *Tiempo registrado:* Total de horas dedicadas en la semana actual.
  - *Tiempo libre:* Horas no registradas restantes de las 168 horas.
  - *Categoría top:* Categoría con mayor cantidad de tiempo invertido.
  - *Eventos semana:* Contador de eventos terminados en la semana en curso.
- **Widget de actividad actual:** Muestra la tarea en curso o el estado *Inactivo* con acceso rápido.

---

## 🛠️ Aspectos Técnicos
- Consulta SQL optimizada en `DatabaseHelper`:
  `SELECT c.id, c.nombre, COALESCE(SUM(e.duracion_segundos), 0) ... GROUP BY c.id, c.nombre`.
- Delimitación temporal automática por `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)` y `nextOrSame(DayOfWeek.SUNDAY)`.
