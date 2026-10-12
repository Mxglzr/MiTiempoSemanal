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

    // ==========================================
    // PRUEBAS SPRINT 2 (HU-03, HU-04, HU-05)
    // ==========================================

    @Test
    fun testBuscarTareasPorNombre() {
        val idEstudio = dbHelper.obtenerCategorias().first { it.nombre == "Estudio" }.id
        val idTrabajo = dbHelper.obtenerCategorias().first { it.nombre == "Trabajo" }.id

        dbHelper.insertarTarea("Leer Kotlin", idEstudio)
        dbHelper.insertarTarea("Proyecto Android", idTrabajo)

        // Búsqueda con menos de 2 letras debe retornar lista vacía (criterio HU-03)
        val resultado1Letra = dbHelper.buscarTareasPorNombre("k")
        assertTrue(resultado1Letra.isEmpty())

        // Búsqueda con 2 o más letras
        val resultadoKo = dbHelper.buscarTareasPorNombre("ko")
        assertEquals(1, resultadoKo.size)
        assertEquals("Leer Kotlin", resultadoKo[0].nombre)
        assertEquals("Estudio", resultadoKo[0].categoriaNombre)

        // Búsqueda insensible a mayúsculas/minúsculas
        val resultadoMayus = dbHelper.buscarTareasPorNombre("ANDROID")
        assertEquals(1, resultadoMayus.size)
        assertEquals("Proyecto Android", resultadoMayus[0].nombre)
        assertEquals("Trabajo", resultadoMayus[0].categoriaNombre)
    }

    @Test
    fun testIniciarYFinalizarEvento() {
        val idEstudio = dbHelper.obtenerCategorias().first { it.nombre == "Estudio" }.id
        val idTarea = dbHelper.insertarTarea("Aprender Room", idEstudio)

        // 1. Iniciar evento
        val fechaInicio = "2026-10-12 10:00:00"
        val idEvento = dbHelper.iniciarEvento(idTarea, fechaInicio)
        assertTrue(idEvento > 0)

        // 2. Verificar que el evento está en curso
        val enCurso = dbHelper.obtenerEventoEnCurso()
        assertNotNull(enCurso)
        assertEquals(idEvento, enCurso?.id)
        assertEquals("Aprender Room", enCurso?.tareaNombre)
        assertEquals("Estudio", enCurso?.categoriaNombre)
        assertEquals(fechaInicio, enCurso?.fechaInicio)
        assertNull(enCurso?.fechaFin)

        // 3. Solo puede haber un evento en curso a la vez (criterio HU-05)
        val segundoEvento = dbHelper.iniciarEvento(idTarea, "2026-10-12 10:05:00")
        assertEquals(-1L, segundoEvento)

        // 4. Finalizar evento
        val fechaFin = "2026-10-12 10:45:00"
        val duracionSegundos = 2700L // 45 minutos
        val finalizado = dbHelper.finalizarEvento(idEvento, fechaFin, duracionSegundos)
        assertTrue(finalizado)

        // 5. Verificar que ya no hay evento en curso
        val enCursoDespues = dbHelper.obtenerEventoEnCurso()
        assertNull(enCursoDespues)

        // 6. Verificar historial de eventos
        val historial = dbHelper.obtenerHistorialEventos()
        assertEquals(1, historial.size)
        val eventoRegistrado = historial[0]
        assertEquals("Aprender Room", eventoRegistrado.tareaNombre)
        assertEquals("Estudio", eventoRegistrado.categoriaNombre)
        assertEquals(fechaInicio, eventoRegistrado.fechaInicio)
        assertEquals(fechaFin, eventoRegistrado.fechaFin)
        assertEquals(duracionSegundos, eventoRegistrado.duracionSegundos)
        assertEquals("45m", eventoRegistrado.duracionFormateada())
    }
}
