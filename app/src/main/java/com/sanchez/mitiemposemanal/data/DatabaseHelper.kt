package com.sanchez.mitiemposemanal.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sanchez.mitiemposemanal.data.model.Categoria
import com.sanchez.mitiemposemanal.data.model.Evento
import com.sanchez.mitiemposemanal.data.model.ReporteCategoria
import com.sanchez.mitiemposemanal.data.model.ReporteSemanal
import com.sanchez.mitiemposemanal.data.model.ResumenCategoriaSemanal
import com.sanchez.mitiemposemanal.data.model.Tarea
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "mitiemposemanal.db"
        const val DATABASE_VERSION = 3

        // Tabla categorias
        const val TABLA_CATEGORIAS = "categorias"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NOMBRE = "nombre"

        // Tabla tareas
        const val TABLA_TAREAS = "tareas"
        const val COL_TAR_ID = "id"
        const val COL_TAR_NOMBRE = "nombre"
        const val COL_TAR_CATEGORIA_ID = "categoria_id"

        // Tabla eventos (Sprint 2)
        const val TABLA_EVENTOS = "eventos"
        const val COL_EVT_ID = "id"
        const val COL_EVT_TAREA_ID = "tarea_id"
        const val COL_EVT_FECHA_INICIO = "fecha_inicio"
        const val COL_EVT_FECHA_FIN = "fecha_fin"
        const val COL_EVT_DURACION_SEGUNDOS = "duracion_segundos"

        // Tabla reportes_semanales (Sprint 4)
        const val TABLA_REPORTES_SEMANALES = "reportes_semanales"
        const val COL_REP_ID = "id"
        const val COL_REP_FECHA_INICIO = "fecha_inicio"
        const val COL_REP_FECHA_FIN = "fecha_fin"
        const val COL_REP_ETIQUETA_RANGO = "etiqueta_rango"
        const val COL_REP_SEGUNDOS_REGISTRADOS = "segundos_registrados"
        const val COL_REP_HORAS_SIN_REGISTRAR = "horas_sin_registrar"
        const val COL_REP_PORCENTAJE_SIN_REGISTRAR = "porcentaje_sin_registrar"

        // Tabla reportes_categorias (Sprint 4)
        const val TABLA_REPORTES_CATEGORIAS = "reportes_categorias"
        const val COL_RC_ID = "id"
        const val COL_RC_REPORTE_ID = "reporte_id"
        const val COL_RC_CATEGORIA_ID = "categoria_id"
        const val COL_RC_CATEGORIA_NOMBRE = "categoria_nombre"
        const val COL_RC_TOTAL_SEGUNDOS = "total_segundos"
        const val COL_RC_HORAS = "horas"
        const val COL_RC_PORCENTAJE = "porcentaje"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val crearTablaCategorias = """
            CREATE TABLE $TABLA_CATEGORIAS (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NOMBRE TEXT NOT NULL UNIQUE COLLATE NOCASE
            );
        """.trimIndent()

        val crearTablaTareas = """
            CREATE TABLE $TABLA_TAREAS (
                $COL_TAR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TAR_NOMBRE TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COL_TAR_CATEGORIA_ID INTEGER NOT NULL,
                FOREIGN KEY ($COL_TAR_CATEGORIA_ID) REFERENCES $TABLA_CATEGORIAS($COL_CAT_ID) ON DELETE RESTRICT
            );
        """.trimIndent()

        val crearTablaEventos = """
            CREATE TABLE $TABLA_EVENTOS (
                $COL_EVT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EVT_TAREA_ID INTEGER NOT NULL,
                $COL_EVT_FECHA_INICIO TEXT NOT NULL,
                $COL_EVT_FECHA_FIN TEXT,
                $COL_EVT_DURACION_SEGUNDOS INTEGER DEFAULT 0,
                FOREIGN KEY ($COL_EVT_TAREA_ID) REFERENCES $TABLA_TAREAS($COL_TAR_ID) ON DELETE CASCADE
            );
        """.trimIndent()

        val crearTablaReportesSemanales = """
            CREATE TABLE $TABLA_REPORTES_SEMANALES (
                $COL_REP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_REP_FECHA_INICIO TEXT NOT NULL,
                $COL_REP_FECHA_FIN TEXT NOT NULL,
                $COL_REP_ETIQUETA_RANGO TEXT NOT NULL,
                $COL_REP_SEGUNDOS_REGISTRADOS INTEGER NOT NULL DEFAULT 0,
                $COL_REP_HORAS_SIN_REGISTRAR REAL NOT NULL DEFAULT 168.0,
                $COL_REP_PORCENTAJE_SIN_REGISTRAR REAL NOT NULL DEFAULT 100.0,
                UNIQUE($COL_REP_FECHA_INICIO, $COL_REP_FECHA_FIN)
            );
        """.trimIndent()

        val crearTablaReportesCategorias = """
            CREATE TABLE $TABLA_REPORTES_CATEGORIAS (
                $COL_RC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RC_REPORTE_ID INTEGER NOT NULL,
                $COL_RC_CATEGORIA_ID INTEGER NOT NULL,
                $COL_RC_CATEGORIA_NOMBRE TEXT NOT NULL,
                $COL_RC_TOTAL_SEGUNDOS INTEGER NOT NULL DEFAULT 0,
                $COL_RC_HORAS REAL NOT NULL DEFAULT 0.0,
                $COL_RC_PORCENTAJE REAL NOT NULL DEFAULT 0.0,
                FOREIGN KEY ($COL_RC_REPORTE_ID) REFERENCES $TABLA_REPORTES_SEMANALES($COL_REP_ID) ON DELETE CASCADE,
                FOREIGN KEY ($COL_RC_CATEGORIA_ID) REFERENCES $TABLA_CATEGORIAS($COL_CAT_ID),
                UNIQUE($COL_RC_REPORTE_ID, $COL_RC_CATEGORIA_ID)
            );
        """.trimIndent()

        db.execSQL(crearTablaCategorias)
        db.execSQL(crearTablaTareas)
        db.execSQL(crearTablaEventos)
        db.execSQL(crearTablaReportesSemanales)
        db.execSQL(crearTablaReportesCategorias)

        // Precarga de las 4 categorías iniciales obligatorias
        val categoriasIniciales = listOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (categoria in categoriasIniciales) {
            val values = ContentValues().apply {
                put(COL_CAT_NOMBRE, categoria)
            }
            db.insert(TABLA_CATEGORIAS, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // En Sprint 2: Si viene de la versión 1, agregamos la tabla de eventos sin perder categorías ni tareas
        if (oldVersion < 2) {
            val crearTablaEventos = """
                CREATE TABLE IF NOT EXISTS $TABLA_EVENTOS (
                    $COL_EVT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_EVT_TAREA_ID INTEGER NOT NULL,
                    $COL_EVT_FECHA_INICIO TEXT NOT NULL,
                    $COL_EVT_FECHA_FIN TEXT,
                    $COL_EVT_DURACION_SEGUNDOS INTEGER DEFAULT 0,
                    FOREIGN KEY ($COL_EVT_TAREA_ID) REFERENCES $TABLA_TAREAS($COL_TAR_ID) ON DELETE CASCADE
                );
            """.trimIndent()
            db.execSQL(crearTablaEventos)
        }

        // En Sprint 4: Si viene de versión anterior a 3, agregamos tablas de reportes
        if (oldVersion < 3) {
            val crearTablaReportesSemanales = """
                CREATE TABLE IF NOT EXISTS $TABLA_REPORTES_SEMANALES (
                    $COL_REP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_REP_FECHA_INICIO TEXT NOT NULL,
                    $COL_REP_FECHA_FIN TEXT NOT NULL,
                    $COL_REP_ETIQUETA_RANGO TEXT NOT NULL,
                    $COL_REP_SEGUNDOS_REGISTRADOS INTEGER NOT NULL DEFAULT 0,
                    $COL_REP_HORAS_SIN_REGISTRAR REAL NOT NULL DEFAULT 168.0,
                    $COL_REP_PORCENTAJE_SIN_REGISTRAR REAL NOT NULL DEFAULT 100.0,
                    UNIQUE($COL_REP_FECHA_INICIO, $COL_REP_FECHA_FIN)
                );
            """.trimIndent()

            val crearTablaReportesCategorias = """
                CREATE TABLE IF NOT EXISTS $TABLA_REPORTES_CATEGORIAS (
                    $COL_RC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_RC_REPORTE_ID INTEGER NOT NULL,
                    $COL_RC_CATEGORIA_ID INTEGER NOT NULL,
                    $COL_RC_CATEGORIA_NOMBRE TEXT NOT NULL,
                    $COL_RC_TOTAL_SEGUNDOS INTEGER NOT NULL DEFAULT 0,
                    $COL_RC_HORAS REAL NOT NULL DEFAULT 0.0,
                    $COL_RC_PORCENTAJE REAL NOT NULL DEFAULT 0.0,
                    FOREIGN KEY ($COL_RC_REPORTE_ID) REFERENCES $TABLA_REPORTES_SEMANALES($COL_REP_ID) ON DELETE CASCADE,
                    FOREIGN KEY ($COL_RC_CATEGORIA_ID) REFERENCES $TABLA_CATEGORIAS($COL_CAT_ID),
                    UNIQUE($COL_RC_REPORTE_ID, $COL_RC_CATEGORIA_ID)
                );
            """.trimIndent()

            db.execSQL(crearTablaReportesSemanales)
            db.execSQL(crearTablaReportesCategorias)
        }
    }

    // ==========================================
    // OPERACIONES CRUD PARA CATEGORÍAS
    // ==========================================

    /**
     * Verifica si existe una categoría con el nombre especificado (no distingue mayúsculas/minúsculas).
     * Opcionalmente excluye un ID para casos de edición.
     */
    fun existeCategoria(nombre: String, excluirId: Long? = null): Boolean {
        val db = readableDatabase
        val nombreLimpio = nombre.trim()
        val cursor = if (excluirId != null) {
            db.rawQuery(
                "SELECT 1 FROM $TABLA_CATEGORIAS WHERE LOWER(TRIM($COL_CAT_NOMBRE)) = LOWER(?) AND $COL_CAT_ID != ?",
                arrayOf(nombreLimpio, excluirId.toString())
            )
        } else {
            db.rawQuery(
                "SELECT 1 FROM $TABLA_CATEGORIAS WHERE LOWER(TRIM($COL_CAT_NOMBRE)) = LOWER(?)",
                arrayOf(nombreLimpio)
            )
        }
        val existe = cursor.moveToFirst()
        cursor.close()
        return existe
    }

    /**
     * Inserta una nueva categoría.
     * Retorna el ID insertado o -1 si falla.
     */
    fun insertarCategoria(nombre: String): Long {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty() || existeCategoria(nombreLimpio)) {
            return -1L
        }
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CAT_NOMBRE, nombreLimpio)
        }
        return db.insert(TABLA_CATEGORIAS, null, values)
    }

    /**
     * Obtiene la lista completa de categorías.
     */
    fun obtenerCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_CAT_ID, $COL_CAT_NOMBRE FROM $TABLA_CATEGORIAS ORDER BY $COL_CAT_NOMBRE ASC",
            null
        )
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_CAT_ID)
            val nombreIndex = cursor.getColumnIndexOrThrow(COL_CAT_NOMBRE)
            do {
                val id = cursor.getLong(idIndex)
                val nombre = cursor.getString(nombreIndex)
                lista.add(Categoria(id, nombre))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    /**
     * Actualiza el nombre de una categoría.
     * Retorna true si tuvo éxito, false si ya existe otra con ese nombre o si falló.
     */
    fun actualizarCategoria(id: Long, nuevoNombre: String): Boolean {
        val nombreLimpio = nuevoNombre.trim()
        if (nombreLimpio.isEmpty() || existeCategoria(nombreLimpio, id)) {
            return false
        }
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CAT_NOMBRE, nombreLimpio)
        }
        val filasAfectadas = db.update(
            TABLA_CATEGORIAS,
            values,
            "$COL_CAT_ID = ?",
            arrayOf(id.toString())
        )
        return filasAfectadas > 0
    }

    /**
     * Cuenta cuántas tareas están asociadas a una categoría.
     */
    fun contarTareasPorCategoria(categoriaId: Long): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLA_TAREAS WHERE $COL_TAR_CATEGORIA_ID = ?",
            arrayOf(categoriaId.toString())
        )
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }
        cursor.close()
        return total
    }

    /**
     * Elimina una categoría si no tiene tareas asociadas.
     * Retorna true si fue eliminada, false si no se pudo eliminar (por ejemplo, si tiene tareas).
     */
    fun eliminarCategoria(categoriaId: Long): Boolean {
        if (contarTareasPorCategoria(categoriaId) > 0) {
            return false
        }
        val db = writableDatabase
        val filas = db.delete(
            TABLA_CATEGORIAS,
            "$COL_CAT_ID = ?",
            arrayOf(categoriaId.toString())
        )
        return filas > 0
    }

    // ==========================================
    // OPERACIONES CRUD PARA TAREAS
    // ==========================================

    /**
     * Verifica si existe una tarea con el nombre especificado (case-insensitive).
     * Opcionalmente excluye un ID para edición.
     */
    fun existeTarea(nombre: String, excluirId: Long? = null): Boolean {
        val db = readableDatabase
        val nombreLimpio = nombre.trim()
        val cursor = if (excluirId != null) {
            db.rawQuery(
                "SELECT 1 FROM $TABLA_TAREAS WHERE LOWER(TRIM($COL_TAR_NOMBRE)) = LOWER(?) AND $COL_TAR_ID != ?",
                arrayOf(nombreLimpio, excluirId.toString())
            )
        } else {
            db.rawQuery(
                "SELECT 1 FROM $TABLA_TAREAS WHERE LOWER(TRIM($COL_TAR_NOMBRE)) = LOWER(?)",
                arrayOf(nombreLimpio)
            )
        }
        val existe = cursor.moveToFirst()
        cursor.close()
        return existe
    }

    /**
     * Insertar
     */
    fun insertarTarea(nombre: String, categoriaId: Long): Long {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty() || existeTarea(nombreLimpio)) {
            return -1L
        }
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TAR_NOMBRE, nombreLimpio)
            put(COL_TAR_CATEGORIA_ID, categoriaId)
        }
        return db.insert(TABLA_TAREAS, null, values)
    }

    /**
     * Obtiene todas las tareas junto con el nombre de su categoría asociada.
     */
    fun obtenerTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val db = readableDatabase
        val query = """
            SELECT t.$COL_TAR_ID, t.$COL_TAR_NOMBRE, t.$COL_TAR_CATEGORIA_ID, c.$COL_CAT_NOMBRE AS cat_nombre
            FROM $TABLA_TAREAS t
            INNER JOIN $TABLA_CATEGORIAS c ON t.$COL_TAR_CATEGORIA_ID = c.$COL_CAT_ID
            ORDER BY t.$COL_TAR_ID DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_TAR_ID)
            val nombreIndex = cursor.getColumnIndexOrThrow(COL_TAR_NOMBRE)
            val catIdIndex = cursor.getColumnIndexOrThrow(COL_TAR_CATEGORIA_ID)
            val catNombreIndex = cursor.getColumnIndexOrThrow("cat_nombre")

            do {
                val id = cursor.getLong(idIndex)
                val nombre = cursor.getString(nombreIndex)
                val catId = cursor.getLong(catIdIndex)
                val catNombre = cursor.getString(catNombreIndex)
                lista.add(Tarea(id, nombre, catId, catNombre))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    /**
     * Actualizar
     */
    fun actualizarTarea(id: Long, nuevoNombre: String, nuevoCategoriaId: Long): Boolean {
        val nombreLimpio = nuevoNombre.trim()
        if (nombreLimpio.isEmpty() || existeTarea(nombreLimpio, id)) {
            return false
        }
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TAR_NOMBRE, nombreLimpio)
            put(COL_TAR_CATEGORIA_ID, nuevoCategoriaId)
        }
        val filasAfectadas = db.update(
            TABLA_TAREAS,
            values,
            "$COL_TAR_ID = ?",
            arrayOf(id.toString())
        )
        return filasAfectadas > 0
    }

    /**
     * Eliminar
     */
    fun eliminarTarea(id: Long): Boolean {
        val db = writableDatabase
        val filas = db.delete(
            TABLA_TAREAS,
            "$COL_TAR_ID = ?",
            arrayOf(id.toString())
        )
        return filas > 0
    }

    // ==========================================
    // OPERACIONES DE BÚSQUEDA (HU-03 / HU-04)
    // ==========================================

    /**
     * Busca tareas cuyo nombre contenga el texto ingresado (al menos 2 letras).
     * Retorna la lista de tareas con su categoría asociada.
     */
    fun buscarTareasPorNombre(query: String): List<Tarea> {
        val textoLimpio = query.trim()
        if (textoLimpio.length < 2) {
            return emptyList()
        }

        val lista = mutableListOf<Tarea>()
        val db = readableDatabase
        val sql = """
            SELECT t.$COL_TAR_ID, t.$COL_TAR_NOMBRE, t.$COL_TAR_CATEGORIA_ID, c.$COL_CAT_NOMBRE AS cat_nombre
            FROM $TABLA_TAREAS t
            INNER JOIN $TABLA_CATEGORIAS c ON t.$COL_TAR_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE LOWER(t.$COL_TAR_NOMBRE) LIKE LOWER(?)
            ORDER BY t.$COL_TAR_NOMBRE ASC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf("%$textoLimpio%"))
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_TAR_ID)
            val nombreIndex = cursor.getColumnIndexOrThrow(COL_TAR_NOMBRE)
            val catIdIndex = cursor.getColumnIndexOrThrow(COL_TAR_CATEGORIA_ID)
            val catNombreIndex = cursor.getColumnIndexOrThrow("cat_nombre")

            do {
                val id = cursor.getLong(idIndex)
                val nombre = cursor.getString(nombreIndex)
                val catId = cursor.getLong(catIdIndex)
                val catNombre = cursor.getString(catNombreIndex)
                lista.add(Tarea(id, nombre, catId, catNombre))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    /**
     * Obtiene una tarea buscando por nombre exacto (case-insensitive).
     */
    fun obtenerTareaPorNombreExacto(nombre: String): Tarea? {
        val textoLimpio = nombre.trim()
        if (textoLimpio.isEmpty()) return null

        val db = readableDatabase
        val sql = """
            SELECT t.$COL_TAR_ID, t.$COL_TAR_NOMBRE, t.$COL_TAR_CATEGORIA_ID, c.$COL_CAT_NOMBRE AS cat_nombre
            FROM $TABLA_TAREAS t
            INNER JOIN $TABLA_CATEGORIAS c ON t.$COL_TAR_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE LOWER(TRIM(t.$COL_TAR_NOMBRE)) = LOWER(?)
            LIMIT 1
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(textoLimpio))
        var tarea: Tarea? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TAR_ID))
            val nom = cursor.getString(cursor.getColumnIndexOrThrow(COL_TAR_NOMBRE))
            val catId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TAR_CATEGORIA_ID))
            val catNom = cursor.getString(cursor.getColumnIndexOrThrow("cat_nombre"))
            tarea = Tarea(id, nom, catId, catNom)
        }
        cursor.close()
        return tarea
    }

    // ==========================================
    // OPERACIONES PARA EVENTOS (HU-05)
    // ==========================================

    /**
     * Inicia un nuevo evento para la tarea especificada.
     * Solo permite iniciar si no hay otro evento en curso.
     * Retorna el ID del evento o -1 si ya hay uno en curso o falla la inserción.
     */
    fun iniciarEvento(tareaId: Long, fechaInicio: String): Long {
        if (obtenerEventoEnCurso() != null) {
            return -1L // Solo puede haber un evento en curso a la vez
        }

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVT_TAREA_ID, tareaId)
            put(COL_EVT_FECHA_INICIO, fechaInicio)
            putNull(COL_EVT_FECHA_FIN)
            put(COL_EVT_DURACION_SEGUNDOS, 0L)
        }
        return db.insert(TABLA_EVENTOS, null, values)
    }

    /**
     * Obtiene el evento actualmente en curso (aquel con fecha_fin = NULL), si existe.
     */
    fun obtenerEventoEnCurso(): Evento? {
        val db = readableDatabase
        val sql = """
            SELECT e.$COL_EVT_ID, e.$COL_EVT_TAREA_ID, t.$COL_TAR_NOMBRE AS tarea_nombre,
                   c.$COL_CAT_NOMBRE AS cat_nombre, e.$COL_EVT_FECHA_INICIO, e.$COL_EVT_FECHA_FIN,
                   e.$COL_EVT_DURACION_SEGUNDOS
            FROM $TABLA_EVENTOS e
            INNER JOIN $TABLA_TAREAS t ON e.$COL_EVT_TAREA_ID = t.$COL_TAR_ID
            INNER JOIN $TABLA_CATEGORIAS c ON t.$COL_TAR_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE e.$COL_EVT_FECHA_FIN IS NULL
            ORDER BY e.$COL_EVT_ID DESC
            LIMIT 1
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        var evento: Evento? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVT_ID))
            val tareaId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVT_TAREA_ID))
            val tareaNombre = cursor.getString(cursor.getColumnIndexOrThrow("tarea_nombre"))
            val catNombre = cursor.getString(cursor.getColumnIndexOrThrow("cat_nombre"))
            val fechaInicio = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVT_FECHA_INICIO))
            val fechaFin = cursor.getString(cursor.getColumnIndexOrThrow(COL_EVT_FECHA_FIN))
            val duracion = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVT_DURACION_SEGUNDOS))

            evento = Evento(
                id = id,
                tareaId = tareaId,
                tareaNombre = tareaNombre,
                categoriaNombre = catNombre,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                duracionSegundos = duracion
            )
        }
        cursor.close()
        return evento
    }

    /**
     * Finaliza un evento en curso guardando la fecha y hora de fin y la duración calculada.
     */
    fun finalizarEvento(idEvento: Long, fechaFin: String, duracionSegundos: Long): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EVT_FECHA_FIN, fechaFin)
            put(COL_EVT_DURACION_SEGUNDOS, duracionSegundos)
        }
        val filas = db.update(
            TABLA_EVENTOS,
            values,
            "$COL_EVT_ID = ?",
            arrayOf(idEvento.toString())
        )
        return filas > 0
    }

    /**
     * Obtiene la lista de todos los eventos finalizados registrados (historial).
     */
    fun obtenerHistorialEventos(): List<Evento> {
        val lista = mutableListOf<Evento>()
        val db = readableDatabase
        val sql = """
            SELECT e.$COL_EVT_ID, e.$COL_EVT_TAREA_ID, t.$COL_TAR_NOMBRE AS tarea_nombre,
                   c.$COL_CAT_NOMBRE AS cat_nombre, e.$COL_EVT_FECHA_INICIO, e.$COL_EVT_FECHA_FIN,
                   e.$COL_EVT_DURACION_SEGUNDOS
            FROM $TABLA_EVENTOS e
            INNER JOIN $TABLA_TAREAS t ON e.$COL_EVT_TAREA_ID = t.$COL_TAR_ID
            INNER JOIN $TABLA_CATEGORIAS c ON t.$COL_TAR_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE e.$COL_EVT_FECHA_FIN IS NOT NULL
            ORDER BY e.$COL_EVT_ID DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_EVT_ID)
            val tareaIdIndex = cursor.getColumnIndexOrThrow(COL_EVT_TAREA_ID)
            val tareaNombreIndex = cursor.getColumnIndexOrThrow("tarea_nombre")
            val catNombreIndex = cursor.getColumnIndexOrThrow("cat_nombre")
            val inicioIndex = cursor.getColumnIndexOrThrow(COL_EVT_FECHA_INICIO)
            val finIndex = cursor.getColumnIndexOrThrow(COL_EVT_FECHA_FIN)
            val duracionIndex = cursor.getColumnIndexOrThrow(COL_EVT_DURACION_SEGUNDOS)

            do {
                val id = cursor.getLong(idIndex)
                val tareaId = cursor.getLong(tareaIdIndex)
                val tareaNombre = cursor.getString(tareaNombreIndex)
                val catNombre = cursor.getString(catNombreIndex)
                val fechaInicio = cursor.getString(inicioIndex)
                val fechaFin = cursor.getString(finIndex)
                val duracion = cursor.getLong(duracionIndex)

                lista.add(
                    Evento(
                        id = id,
                        tareaId = tareaId,
                        tareaNombre = tareaNombre,
                        categoriaNombre = catNombre,
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin,
                        duracionSegundos = duracion
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // ==========================================
    // SPRINT 3: RESUMEN SEMANAL POR CATEGORÍA (HU-06)
    // ==========================================

    /**
     * Calcula los totales por categoría para una semana usando una consulta SQL con SUM y GROUP BY.
     * Retorna todas las categorías existentes (incluso las que tienen 0 segundos acumulados).
     * Requisito técnico: no sumar en un ciclo en Kotlin.
     */
    fun obtenerResumenSemanalCategorias(
        inicioSemana: String,
        finSemana: String
    ): List<ResumenCategoriaSemanal> {
        val lista = mutableListOf<ResumenCategoriaSemanal>()
        val db = readableDatabase
        val sql = """
            SELECT c.$COL_CAT_ID AS cat_id, 
                   c.$COL_CAT_NOMBRE AS cat_nombre, 
                   COALESCE(SUM(e.$COL_EVT_DURACION_SEGUNDOS), 0) AS total_segundos
            FROM $TABLA_CATEGORIAS c
            LEFT JOIN $TABLA_TAREAS t ON t.$COL_TAR_CATEGORIA_ID = c.$COL_CAT_ID
            LEFT JOIN $TABLA_EVENTOS e ON e.$COL_EVT_TAREA_ID = t.$COL_TAR_ID 
                 AND e.$COL_EVT_FECHA_FIN IS NOT NULL 
                 AND e.$COL_EVT_FECHA_INICIO >= ? 
                 AND e.$COL_EVT_FECHA_INICIO <= ?
            GROUP BY c.$COL_CAT_ID, c.$COL_CAT_NOMBRE
            ORDER BY c.$COL_CAT_NOMBRE ASC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(inicioSemana, finSemana))
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow("cat_id")
            val nombreIndex = cursor.getColumnIndexOrThrow("cat_nombre")
            val segundosIndex = cursor.getColumnIndexOrThrow("total_segundos")

            do {
                val id = cursor.getLong(idIndex)
                val nombre = cursor.getString(nombreIndex)
                val segundos = cursor.getLong(segundosIndex)
                lista.add(ResumenCategoriaSemanal(id, nombre, segundos))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    /**
     * Cuenta el número de eventos finalizados en una semana dada.
     */
    fun contarEventosSemana(inicioSemana: String, finSemana: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLA_EVENTOS WHERE $COL_EVT_FECHA_FIN IS NOT NULL AND $COL_EVT_FECHA_INICIO >= ? AND $COL_EVT_FECHA_INICIO <= ?",
            arrayOf(inicioSemana, finSemana)
        )
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }
        cursor.close()
        return total
    }

    /**
     * Obtiene la categoría con más tiempo acumulado en una semana dada (si existe).
     */
    fun obtenerCategoriaTopSemana(inicioSemana: String, finSemana: String): String? {
        val db = readableDatabase
        val sql = """
            SELECT c.$COL_CAT_NOMBRE, SUM(e.$COL_EVT_DURACION_SEGUNDOS) as total
            FROM $TABLA_EVENTOS e
            INNER JOIN $TABLA_TAREAS t ON e.$COL_EVT_TAREA_ID = t.$COL_TAR_ID
            INNER JOIN $TABLA_CATEGORIAS c ON t.$COL_TAR_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE e.$COL_EVT_FECHA_FIN IS NOT NULL 
              AND e.$COL_EVT_FECHA_INICIO >= ? 
              AND e.$COL_EVT_FECHA_INICIO <= ?
            GROUP BY c.$COL_CAT_ID, c.$COL_CAT_NOMBRE
            HAVING total > 0
            ORDER BY total DESC
            LIMIT 1
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(inicioSemana, finSemana))
        var topCat: String? = null
        if (cursor.moveToFirst()) {
            topCat = cursor.getString(0)
        }
        cursor.close()
        return topCat
    }

    // ==========================================
    // SPRINT 4: CIERRE SEMANAL Y REPORTES (HU-07 & HU-08)
    // ==========================================

    /**
     * Verifica si ya existe un reporte guardado para un rango de semana específico.
     * Criterio técnico: Evitar reportes duplicados (UNIQUE).
     */
    fun existeReporteSemanal(inicioSemana: String, finSemana: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLA_REPORTES_SEMANALES WHERE $COL_REP_FECHA_INICIO = ? AND $COL_REP_FECHA_FIN = ? LIMIT 1",
            arrayOf(inicioSemana, finSemana)
        )
        val existe = cursor.moveToFirst()
        cursor.close()
        return existe
    }

    /**
     * Guarda el reporte completo de una semana cerrada (maestro y detalle por categorías).
     * Calcula horas y porcentajes sobre las 168 horas semanales reglamentarias.
     */
    fun guardarCierreSemana(
        inicioSemana: String,
        finSemana: String,
        etiquetaRango: String
    ): Long {
        if (existeReporteSemanal(inicioSemana, finSemana)) {
            return -1L
        }

        val listaResumen = obtenerResumenSemanalCategorias(inicioSemana, finSemana)
        val totalSegundosSemana = 168L * 3600L // 604 800 segundos
        val totalSegundosRegistrados = listaResumen.sumOf { it.totalSegundos }
        val segundosSinRegistrar = (totalSegundosSemana - totalSegundosRegistrados).coerceAtLeast(0L)
        val horasSinRegistrar = segundosSinRegistrar / 3600.0
        val porcentajeSinRegistrar = (segundosSinRegistrar.toDouble() / totalSegundosSemana.toDouble()) * 100.0

        val db = writableDatabase
        db.beginTransaction()
        var reporteId = -1L
        try {
            val valuesReporte = ContentValues().apply {
                put(COL_REP_FECHA_INICIO, inicioSemana)
                put(COL_REP_FECHA_FIN, finSemana)
                put(COL_REP_ETIQUETA_RANGO, etiquetaRango)
                put(COL_REP_SEGUNDOS_REGISTRADOS, totalSegundosRegistrados)
                put(COL_REP_HORAS_SIN_REGISTRAR, horasSinRegistrar)
                put(COL_REP_PORCENTAJE_SIN_REGISTRAR, porcentajeSinRegistrar)
            }
            reporteId = db.insert(TABLA_REPORTES_SEMANALES, null, valuesReporte)

            if (reporteId != -1L) {
                for (resumen in listaResumen) {
                    val valuesCat = ContentValues().apply {
                        put(COL_RC_REPORTE_ID, reporteId)
                        put(COL_RC_CATEGORIA_ID, resumen.categoriaId)
                        put(COL_RC_CATEGORIA_NOMBRE, resumen.categoriaNombre)
                        put(COL_RC_TOTAL_SEGUNDOS, resumen.totalSegundos)
                        put(COL_RC_HORAS, resumen.totalHoras)
                        put(COL_RC_PORCENTAJE, resumen.porcentaje)
                    }
                    db.insert(TABLA_REPORTES_CATEGORIAS, null, valuesCat)
                }
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }
        return reporteId
    }

    /**
     * HU-07: Al abrir la app, si hay semanas terminadas sin reporte, la app guarda el reporte de cada una.
     * Si el usuario no abrió la app durante varias semanas, se generan los reportes de todas las semanas pendientes que tengan eventos.
     */
    fun verificarYGenerarCierresSemanalesPendientes(fechaReferencia: LocalDateTime = LocalDateTime.now()): Int {
        val hoy = fechaReferencia.toLocalDate()
        val lunesActual = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dtfIso = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dtfVisual = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val inicioSemanaActualStr = "${lunesActual.format(dtfIso)} 00:00:00"

        val db = readableDatabase
        // Buscar fechas únicas de eventos finalizados de semanas pasadas
        val cursor = db.rawQuery(
            """
            SELECT DISTINCT substr($COL_EVT_FECHA_INICIO, 1, 10) as fecha 
            FROM $TABLA_EVENTOS 
            WHERE $COL_EVT_FECHA_FIN IS NOT NULL 
              AND $COL_EVT_FECHA_INICIO < ? 
            ORDER BY $COL_EVT_FECHA_INICIO ASC
            """.trimIndent(),
            arrayOf(inicioSemanaActualStr)
        )

        // Agrupar semanas únicas pendientes (lunes a domingo)
        data class RangoSemana(val inicio: String, val fin: String, val etiqueta: String, val fechaLunes: LocalDate)

        val semanasPendientesMap = linkedMapOf<String, RangoSemana>()

        if (cursor.moveToFirst()) {
            val fechaIndex = cursor.getColumnIndexOrThrow("fecha")
            do {
                val fechaStr = cursor.getString(fechaIndex)
                runCatching {
                    val fechaLd = LocalDate.parse(fechaStr, dtfIso)
                    val lunes = fechaLd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val domingo = fechaLd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

                    val inicioStr = "${lunes.format(dtfIso)} 00:00:00"
                    val finStr = "${domingo.format(dtfIso)} 23:59:59"
                    val etiqueta = "${lunes.format(dtfVisual)} – ${domingo.format(dtfVisual)}"

                    val clave = "$inicioStr|$finStr"
                    if (!semanasPendientesMap.containsKey(clave)) {
                        semanasPendientesMap[clave] = RangoSemana(inicioStr, finStr, etiqueta, lunes)
                    }
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        var reportesCreados = 0
        // Orden cronológico
        val semanasOrdenadas = semanasPendientesMap.values.sortedBy { it.fechaLunes }
        for (semana in semanasOrdenadas) {
            if (!existeReporteSemanal(semana.inicio, semana.fin)) {
                val id = guardarCierreSemana(semana.inicio, semana.fin, semana.etiqueta)
                if (id > 0) {
                    reportesCreados++
                }
            }
        }
        return reportesCreados
    }

    /**
     * HU-08: Obtiene la lista de todas las semanas cerradas para llenar el Spinner.
     * Criterio: "de la más reciente a la más antigua", desde la tabla de reportes.
     */
    fun obtenerReportesSemanales(): List<ReporteSemanal> {
        val lista = mutableListOf<ReporteSemanal>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLA_REPORTES_SEMANALES ORDER BY $COL_REP_FECHA_INICIO DESC",
            null
        )

        if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndexOrThrow(COL_REP_ID)
            val inicioIdx = cursor.getColumnIndexOrThrow(COL_REP_FECHA_INICIO)
            val finIdx = cursor.getColumnIndexOrThrow(COL_REP_FECHA_FIN)
            val etiquetaIdx = cursor.getColumnIndexOrThrow(COL_REP_ETIQUETA_RANGO)
            val segIdx = cursor.getColumnIndexOrThrow(COL_REP_SEGUNDOS_REGISTRADOS)
            val horasSinIdx = cursor.getColumnIndexOrThrow(COL_REP_HORAS_SIN_REGISTRAR)
            val porcSinIdx = cursor.getColumnIndexOrThrow(COL_REP_PORCENTAJE_SIN_REGISTRAR)

            do {
                val rep = ReporteSemanal(
                    id = cursor.getLong(idIdx),
                    fechaInicio = cursor.getString(inicioIdx),
                    fechaFin = cursor.getString(finIdx),
                    etiquetaRango = cursor.getString(etiquetaIdx),
                    totalSegundosRegistrados = cursor.getLong(segIdx),
                    horasSinRegistrar = cursor.getDouble(horasSinIdx),
                    porcentajeSinRegistrar = cursor.getDouble(porcSinIdx)
                )
                lista.add(rep)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    /**
     * Obtiene el desglose por categorías de un reporte específico.
     */
    fun obtenerCategoriasPorReporte(reporteId: Long): List<ReporteCategoria> {
        val lista = mutableListOf<ReporteCategoria>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLA_REPORTES_CATEGORIAS 
            WHERE $COL_RC_REPORTE_ID = ? 
            ORDER BY $COL_RC_HORAS DESC, $COL_RC_CATEGORIA_NOMBRE ASC
            """.trimIndent(),
            arrayOf(reporteId.toString())
        )

        if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndexOrThrow(COL_RC_ID)
            val catIdIdx = cursor.getColumnIndexOrThrow(COL_RC_CATEGORIA_ID)
            val catNombreIdx = cursor.getColumnIndexOrThrow(COL_RC_CATEGORIA_NOMBRE)
            val segIdx = cursor.getColumnIndexOrThrow(COL_RC_TOTAL_SEGUNDOS)
            val horasIdx = cursor.getColumnIndexOrThrow(COL_RC_HORAS)
            val porcIdx = cursor.getColumnIndexOrThrow(COL_RC_PORCENTAJE)

            do {
                val cat = ReporteCategoria(
                    id = cursor.getLong(idIdx),
                    reporteId = reporteId,
                    categoriaId = cursor.getLong(catIdIdx),
                    categoriaNombre = cursor.getString(catNombreIdx),
                    totalSegundos = cursor.getLong(segIdx),
                    horas = cursor.getDouble(horasIdx),
                    porcentaje = cursor.getDouble(porcIdx)
                )
                lista.add(cat)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    /**
     * HU-08: Obtiene los eventos de la semana consultada con tarea, categoría, inicio, fin y duración.
     */
    fun obtenerEventosPorRangoFechas(inicioSemana: String, finSemana: String): List<Evento> {
        val lista = mutableListOf<Evento>()
        val db = readableDatabase
        val sql = """
            SELECT e.$COL_EVT_ID AS id,
                   e.$COL_EVT_TAREA_ID AS tarea_id,
                   t.$COL_TAR_NOMBRE AS tarea_nombre,
                   c.$COL_CAT_NOMBRE AS categoria_nombre,
                   e.$COL_EVT_FECHA_INICIO AS fecha_inicio,
                   e.$COL_EVT_FECHA_FIN AS fecha_fin,
                   e.$COL_EVT_DURACION_SEGUNDOS AS duracion_segundos
            FROM $TABLA_EVENTOS e
            INNER JOIN $TABLA_TAREAS t ON e.$COL_EVT_TAREA_ID = t.$COL_TAR_ID
            INNER JOIN $TABLA_CATEGORIAS c ON t.$COL_TAR_CATEGORIA_ID = c.$COL_CAT_ID
            WHERE e.$COL_EVT_FECHA_FIN IS NOT NULL
              AND e.$COL_EVT_FECHA_INICIO >= ?
              AND e.$COL_EVT_FECHA_INICIO <= ?
            ORDER BY e.$COL_EVT_FECHA_INICIO DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(inicioSemana, finSemana))
        if (cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val tareaIdIdx = cursor.getColumnIndexOrThrow("tarea_id")
            val tareaNombreIdx = cursor.getColumnIndexOrThrow("tarea_nombre")
            val catNombreIdx = cursor.getColumnIndexOrThrow("categoria_nombre")
            val inicioIdx = cursor.getColumnIndexOrThrow("fecha_inicio")
            val finIdx = cursor.getColumnIndexOrThrow("fecha_fin")
            val duracionIdx = cursor.getColumnIndexOrThrow("duracion_segundos")

            do {
                val evento = Evento(
                    id = cursor.getLong(idIdx),
                    tareaId = cursor.getLong(tareaIdIdx),
                    tareaNombre = cursor.getString(tareaNombreIdx),
                    categoriaNombre = cursor.getString(catNombreIdx),
                    fechaInicio = cursor.getString(inicioIdx),
                    fechaFin = cursor.getString(finIdx),
                    duracionSegundos = cursor.getLong(duracionIdx)
                )
                lista.add(evento)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }
}
