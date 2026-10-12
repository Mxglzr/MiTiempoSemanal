package com.sanchez.mitiemposemanal

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sanchez.mitiemposemanal.data.DatabaseHelper
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperTest {

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME)
        dbHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    fun testCategoriasInicialesPreCargadas() {
        val categorias = dbHelper.obtenerCategorias().map { it.nombre }
        assertEquals(4, categorias.size)
        assertTrue(categorias.contains("Estudio"))
        assertTrue(categorias.contains("Trabajo"))
        assertTrue(categorias.contains("Descanso"))
        assertTrue(categorias.contains("Ocio"))
    }

    @Test
    fun testCrearCategoriaYEvitarDuplicados() {
        val idNueva = dbHelper.insertarCategoria("Deporte")
        assertTrue(idNueva > 0)

        // Verificar que existe en la lista
        val categorias = dbHelper.obtenerCategorias().map { it.nombre }
        assertTrue(categorias.contains("Deporte"))

        // Intentar duplicado exacto
        val idDuplicada = dbHelper.insertarCategoria("Deporte")
        assertEquals(-1L, idDuplicada)

        // Intentar duplicado en minúsculas
        val idDuplicadaMin = dbHelper.insertarCategoria("deporte")
        assertEquals(-1L, idDuplicadaMin)
    }

    @Test
    fun testEditarCategoria() {
        val categorias = dbHelper.obtenerCategorias()
        val estudio = categorias.first { it.nombre == "Estudio" }

        val editado = dbHelper.actualizarCategoria(estudio.id, "Estudio Avanzado")
        assertTrue(editado)

        val categoriasActualizadas = dbHelper.obtenerCategorias().map { it.nombre }
        assertTrue(categoriasActualizadas.contains("Estudio Avanzado"))
        assertFalse(categoriasActualizadas.contains("Estudio"))

        // Intentar editar con un nombre que ya existe (ej. "Trabajo")
        val intentoDuplicado = dbHelper.actualizarCategoria(estudio.id, "Trabajo")
        assertFalse(intentoDuplicado)
    }

    @Test
    fun testEliminarCategoriaSinTareas() {
        val idOcio = dbHelper.obtenerCategorias().first { it.nombre == "Ocio" }.id
        val eliminada = dbHelper.eliminarCategoria(idOcio)
        assertTrue(eliminada)

        val categorias = dbHelper.obtenerCategorias().map { it.nombre }
        assertFalse(categorias.contains("Ocio"))
    }

    @Test
    fun testBloquearEliminarCategoriaConTareasAsociadas() {
        val idEstudio = dbHelper.obtenerCategorias().first { it.nombre == "Estudio" }.id

        // Crear una tarea en Estudio
        val idTarea = dbHelper.insertarTarea("Leer Kotlin", idEstudio)
        assertTrue(idTarea > 0)

        // Intentar eliminar la categoría Estudio
        val eliminada = dbHelper.eliminarCategoria(idEstudio)
        assertFalse(eliminada)

        // Verificar que sigue existiendo
        val categorias = dbHelper.obtenerCategorias().map { it.nombre }
        assertTrue(categorias.contains("Estudio"))
    }

    @Test
    fun testCrearTareaYEvitarDuplicados() {
        val idEstudio = dbHelper.obtenerCategorias().first { it.nombre == "Estudio" }.id

        // No se puede guardar tarea sin nombre
        val idVacia = dbHelper.insertarTarea("   ", idEstudio)
        assertEquals(-1L, idVacia)

        // Crear tarea válida
        val idTarea = dbHelper.insertarTarea("Repasar SQLite", idEstudio)
        assertTrue(idTarea > 0)

        // Evitar duplicado
        val idRepetida = dbHelper.insertarTarea("Repasar SQLite", idEstudio)
        assertEquals(-1L, idRepetida)

        // Formato y persistencia
        val tareas = dbHelper.obtenerTareas()
        val tarea = tareas.first { it.id == idTarea }
        assertEquals("Repasar SQLite", tarea.nombre)
        assertEquals("Estudio", tarea.categoriaNombre)
    }

    @Test
    fun testEditarYEliminarTarea() {
        val categorias = dbHelper.obtenerCategorias()
        val idEstudio = categorias.first { it.nombre == "Estudio" }.id
        val idTrabajo = categorias.first { it.nombre == "Trabajo" }.id

        val idTarea = dbHelper.insertarTarea("Tarea 1", idEstudio)
        assertTrue(idTarea > 0)

        // Editar nombre y categoría
        val editada = dbHelper.actualizarTarea(idTarea, "Tarea 1 Modificada", idTrabajo)
        assertTrue(editada)

        val tareas = dbHelper.obtenerTareas()
        val tareaEditada = tareas.first { it.id == idTarea }
        assertEquals("Tarea 1 Modificada", tareaEditada.nombre)
        assertEquals("Trabajo", tareaEditada.categoriaNombre)

        // Eliminar tarea
        val eliminada = dbHelper.eliminarTarea(idTarea)
        assertTrue(eliminada)
        assertFalse(dbHelper.obtenerTareas().any { it.id == idTarea })
    }
}
