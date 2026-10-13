# MiTiempoSemanal - Sprint 1: Categorías y Tareas

Aplicación móvil nativa en Android para la gestión y organización del tiempo personal y académico, desarrollada para el curso de **Desarrollo de Aplicaciones Móviles · SENATI**.

## 📌 Datos de Entrega
- **Estudiante:** Miguel Sánchez
- **Rama:** `Sprint1`
- **Versión:** `v0.1.0` (`versionCode = 1`)
- **Application ID:** `com.senati.mitiemposemanal.msanchez`
- **Mensaje de Commit:** `Sprint 1: categorías y tareas`

---

## 🚀 Historias de Usuario Implementadas

### HU-01: Gestión de Categorías
- **Categorías precargadas:** Inicialización automática con 4 categorías obligatorias: *Estudio*, *Trabajo*, *Descanso* y *Ocio*.
- **Creación:** Diálogo modal para registrar nuevas categorías con validación de nombres vacíos y duplicados (insensible a mayúsculas/minúsculas).
- **Edición:** Modificación del nombre de categorías existentes manteniendo la integridad referencial.
- **Eliminación protegida:** Confirmación previa para eliminar; bloqueo si la categoría contiene tareas asociadas.
- **Contador dinámico:** Muestra la cantidad de tareas asignadas a cada categoría.

### HU-02: Gestión de Tareas
- **Creación:** Registro de tareas asignadas obligatoriamente a una categoría existente seleccionada mediante selector (`Spinner`).
- **Validación:** Prevención de tareas sin nombre, sin categoría o con nombres duplicados.
- **Edición y Reasignación:** Cambio de nombre y transferencia de categoría.
- **Eliminación:** Borrado de tareas con diálogo de confirmación.
- **Visualización:** Tarjetas de tareas con chips visuales que identifican la categoría a la que pertenecen.

---

## 🛠️ Aspectos Técnicos
- **Lenguaje:** Kotlin
- **Base de Datos:** SQLite nativo (`SQLiteOpenHelper`) con claves foráneas activadas (`ON DELETE RESTRICT`).
- **Compatibilidad:** Min SDK 24 (Android 7.0) / Target SDK 36.
- **Arquitectura de Interfaz:** ViewBinding, Material Components, RecyclerView y TabLayout.
