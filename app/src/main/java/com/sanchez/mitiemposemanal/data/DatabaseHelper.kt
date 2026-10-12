package com.sanchez.mitiemposemanal.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sanchez.mitiemposemanal.data.model.Categoria
import com.sanchez.mitiemposemanal.data.model.Evento
import com.sanchez.mitiemposemanal.data.model.Tarea

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "mitiemposemanal.db"
        const val DATABASE_VERSION = 2

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

        db.execSQL(crearTablaCategorias)
        db.execSQL(crearTablaTareas)
        db.execSQL(crearTablaEventos)

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
}
