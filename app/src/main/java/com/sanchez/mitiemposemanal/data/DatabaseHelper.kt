package com.sanchez.mitiemposemanal.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sanchez.mitiemposemanal.data.model.Categoria
import com.sanchez.mitiemposemanal.data.model.Tarea

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "mitiemposemanal.db"
        const val DATABASE_VERSION = 1

        // Tabla categorias
        const val TABLA_CATEGORIAS = "categorias"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NOMBRE = "nombre"

        // Tabla tareas
        const val TABLA_TAREAS = "tareas"
        const val COL_TAR_ID = "id"
        const val COL_TAR_NOMBRE = "nombre"
        const val COL_TAR_CATEGORIA_ID = "categoria_id"
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

        db.execSQL(crearTablaCategorias)
        db.execSQL(crearTablaTareas)

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
        db.execSQL("DROP TABLE IF EXISTS $TABLA_TAREAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_CATEGORIAS")
        onCreate(db)
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
     * Inserta una nueva tarea asociada a una categoría.
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
     * Actualiza el nombre y/o la categoría de una tarea existente.
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
     * Elimina una tarea por su ID.
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
}
