# MiTiempoSemanal - Sprint 4: Cierre Semanal y Reportes (v1.0.0)

Entrega final de la aplicación **MiTiempoSemanal**, que implementa la persistencia histórica de semanas cerradas, el cierre automático al cambiar de semana y la consulta interactiva de reportes.

## 📌 Datos de Entrega
- **Estudiante:** Miguel Sánchez
- **Rama:** `Sprint4`
- **Versión:** `v1.0.0` (`versionCode = 4`)
- **Application ID:** `com.senati.mitiemposemanal.msanchez`
- **Mensaje de Commit:** `Sprint 4: cierre semanal y reportes`

---

## 🚀 Historias de Usuario Implementadas

### HU-07: Cerrar la semana automáticamente
- **Detección y cierre autónomo:** Al abrir la app, detecta automáticamente si existen eventos de semanas anteriores terminadas y genera el reporte histórico de cada una sobre la base de 168 horas.
- **Semanas acumuladas:** Si la app no se abrió durante varias semanas, genera en orden cronológico los reportes de todas las semanas pasadas que tengan eventos registrados.
- **Integridad y no duplicidad:** 
  - Los eventos de semanas pasadas **no se eliminan**; se conservan asociados a su fecha original.
  - Los reportes guardados no se duplican gracias a restricciones `UNIQUE(fecha_inicio, fecha_fin)` a nivel de base de datos.
- **Reinicio semanal:** El panel de la semana actual inicia automáticamente en cero cada lunes.

### HU-08: Consultar reportes por rango de fechas
- **Pestaña Reportes:** Nueva sección accesible desde el menú principal (`TabLayout`).
- **Selector por Spinner:** Desplegable ordenado de la semana más reciente a la más antigua con formato `"dd/MM/yyyy – dd/MM/yyyy"`, poblado dinámicamente desde la tabla de reportes.
- **Desglose de la semana elegida:** Muestra las horas y porcentajes de cada categoría sobre 168 horas y las horas sin registrar de la semana seleccionada.
- **Detalle de eventos:** Lista completa de los eventos de esa semana con tarea, categoría, fecha/hora de inicio, fin y duración.
- **Estado inicial:** Si no existen semanas cerradas previas, presenta el mensaje `"Aún no hay reportes"`.

---

## 🛠️ Aspectos Técnicos
- **Migración SQLite:** `DATABASE_VERSION = 3` con creación de tablas `reportes_semanales` y `reportes_categorias` mediante `onUpgrade`, preservando todos los datos de sprints anteriores.
- **Pruebas Automatizadas:** 12 pruebas instrumentadas y unitarias ejecutadas y aprobadas al 100% en emulador Android.
