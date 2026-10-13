# MiTiempoSemanal - Sprint 2: Eventos y Búsqueda de Tareas

Segunda entrega del proyecto **MiTiempoSemanal**, incorporando el registro en tiempo real de actividades con temporizador y búsqueda interactiva.

## 📌 Datos de Entrega
- **Estudiante:** Miguel Sánchez
- **Rama:** `Sprint2`
- **Versión:** `v0.2.0` (`versionCode = 2`)
- **Application ID:** `com.senati.mitiemposemanal.msanchez`
- **Mensaje de Commit:** `Sprint 2: eventos`

---

## 🚀 Historias de Usuario Implementadas

### HU-03: Búsqueda dinámica de tareas
- **Buscador predictivo:** `AutoCompleteTextView` que consulta la base de datos a partir de 2 caracteres ingresados.
- **Asociación visual:** Al elegir una tarea de la lista desplegable, se detecta y muestra inmediatamente la categoría vinculada.

### HU-04: Creación de tarea rápida si no existe
- Si el usuario escribe una tarea inexistente y presiona **Iniciar**, la aplicación solicita mediante un diálogo modal asignarle una categoría.
- La tarea se registra en la base de datos (disponible también en la pestaña Tareas) e inicia el evento en un solo paso.

### HU-05: Iniciar y finalizar evento
- **Captura automática de tiempo:** Toma la fecha y hora exacta del sistema (`yyyy-MM-dd HH:mm:ss`) al pulsar **Iniciar** y **Finalizar**, sin manipulación manual.
- **Control de estados:** 
  - Solo puede existir **un evento en curso** a la vez.
  - Al iniciar, el botón *Iniciar* se deshabilita y *Finalizar* se habilita.
- **Persistencia de sesión:** Si la app se cierra o reinicia con un evento en marcha, al reabrirla continúa en curso con su estado guardado.
- **Historial de eventos:** Lista cronológica de eventos culminados mostrando tarea, categoría, hora de inicio, hora de fin y duración calculada en formato `HH:mm:ss`.

---

## 🛠️ Aspectos Técnicos
- **Migración de Base de Datos:** `DATABASE_VERSION = 2` mediante `onUpgrade`, añadiendo la tabla `eventos` vinculada a `tareas` (`ON DELETE CASCADE`) sin pérdida de datos.
- **Fechas y Duración:** Implementación con `java.time.LocalDateTime` y `java.time.Duration` mediante Core Library Desugaring.
