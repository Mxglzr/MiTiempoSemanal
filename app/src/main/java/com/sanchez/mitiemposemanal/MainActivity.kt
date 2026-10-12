package com.sanchez.mitiemposemanal

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.sanchez.mitiemposemanal.data.DatabaseHelper
import com.sanchez.mitiemposemanal.data.model.Categoria
import com.sanchez.mitiemposemanal.data.model.Tarea
import com.sanchez.mitiemposemanal.databinding.ActivityMainBinding
import com.sanchez.mitiemposemanal.databinding.DialogCategoriaBinding
import com.sanchez.mitiemposemanal.databinding.DialogCrearTareaEventoBinding
import com.sanchez.mitiemposemanal.databinding.DialogTareaBinding
import com.sanchez.mitiemposemanal.ui.adapter.CategoriaAdapter
import com.sanchez.mitiemposemanal.ui.adapter.EventoAdapter
import com.sanchez.mitiemposemanal.ui.adapter.TareaAdapter
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tareaAdapter: TareaAdapter
    private lateinit var categoriaAdapter: CategoriaAdapter
    private lateinit var eventoAdapter: EventoAdapter

    private var tareasEncontradas: List<Tarea> = emptyList()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private var tabActual: Int = TAB_EVENTOS

    companion object {
        private const val TAB_EVENTOS = 0
        private const val TAB_TAREAS = 1
        private const val TAB_CATEGORIAS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)

        configurarRecyclerViews()
        configurarBuscadorYEventos()
        configurarTabs()
        configurarFab()

        // Cargar vista inicial de Eventos (Sprint 2)
        cargarVistaEventos()
    }

    override fun onResume() {
        super.onResume()
        // Criterio HU-05: Si cierro la app con un evento en curso, al volver a abrirla sigue en curso
        if (tabActual == TAB_EVENTOS) {
            actualizarVistaEventos()
        }
    }

    private fun configurarRecyclerViews() {
        binding.rvItems.layoutManager = LinearLayoutManager(this)

        // Adaptadores Sprint 1
        tareaAdapter = TareaAdapter(
            tareas = emptyList(),
            onEditClick = { tarea -> mostrarDialogoEditarTarea(tarea) },
            onDeleteClick = { tarea -> confirmarEliminarTarea(tarea) }
        )

        categoriaAdapter = CategoriaAdapter(
            categorias = emptyList(),
            getCantidadTareas = { categoriaId -> dbHelper.contarTareasPorCategoria(categoriaId) },
            onEditClick = { categoria -> mostrarDialogoEditarCategoria(categoria) },
            onDeleteClick = { categoria -> intentarEliminarCategoria(categoria) }
        )

        // Adaptador Sprint 2 (Eventos)
        eventoAdapter = EventoAdapter(emptyList())
        binding.rvEventos.layoutManager = LinearLayoutManager(this)
        binding.rvEventos.adapter = eventoAdapter
    }

    private fun configurarTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    TAB_EVENTOS -> {
                        tabActual = TAB_EVENTOS
                        cargarVistaEventos()
                    }
                    TAB_TAREAS -> {
                        tabActual = TAB_TAREAS
                        cargarVistaTareas()
                    }
                    TAB_CATEGORIAS -> {
                        tabActual = TAB_CATEGORIAS
                        cargarVistaCategorias()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun configurarFab() {
        binding.fabAdd.setOnClickListener {
            when (tabActual) {
                TAB_TAREAS -> mostrarDialogoCrearTarea()
                TAB_CATEGORIAS -> mostrarDialogoCrearCategoria()
            }
        }
    }

    // ==========================================
    // HU-03 & HU-04: BÚSQUEDA Y SELECCIÓN DE TAREAS
    // ==========================================

    private fun configurarBuscadorYEventos() {
        // HU-03: Al escribir 2 letras o más, buscar en la BD con cada letra
        binding.actvBuscarTarea.doAfterTextChanged { editable ->
            val texto = editable?.toString()?.trim().orEmpty()
            binding.tilBuscarTarea.error = null

            if (texto.length >= 2) {
                tareasEncontradas = dbHelper.buscarTareasPorNombre(texto)
                val sugerencias = tareasEncontradas.map { it.nombre }
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    sugerencias
                )
                binding.actvBuscarTarea.setAdapter(adapter)
                if (sugerencias.isNotEmpty()) {
                    binding.actvBuscarTarea.showDropDown()
                }

                // Si coincide exactamente con alguna tarea encontrada, mostrar su categoría
                val tareaCoincidente = tareasEncontradas.find { it.nombre.equals(texto, ignoreCase = true) }
                if (tareaCoincidente != null) {
                    mostrarCategoriaSugerida(tareaCoincidente.categoriaNombre)
                } else {
                    ocultarCategoriaSugerida()
                }
            } else {
                tareasEncontradas = emptyList()
                ocultarCategoriaSugerida()
            }
        }

        // HU-03: Al elegir una tarea de la lista desplegable, se muestra su categoría
        binding.actvBuscarTarea.setOnItemClickListener { parent, _, position, _ ->
            val itemSeleccionado = parent.getItemAtPosition(position) as? String
            val tarea = tareasEncontradas.find { it.nombre.equals(itemSeleccionado, ignoreCase = true) }
                ?: dbHelper.obtenerTareaPorNombreExacto(itemSeleccionado.orEmpty())

            if (tarea != null) {
                mostrarCategoriaSugerida(tarea.categoriaNombre)
            }
        }

        // Botón Iniciar (HU-04 & HU-05)
        binding.btnIniciar.setOnClickListener {
            if (dbHelper.obtenerEventoEnCurso() != null) {
                Toast.makeText(this, R.string.error_evento_ya_en_curso, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nombreTarea = binding.actvBuscarTarea.text?.toString()?.trim().orEmpty()
            if (nombreTarea.isEmpty()) {
                binding.tilBuscarTarea.error = getString(R.string.error_escribe_tarea)
                return@setOnClickListener
            }
            binding.tilBuscarTarea.error = null

            val tareaExistente = dbHelper.obtenerTareaPorNombreExacto(nombreTarea)
            if (tareaExistente != null) {
                // Tarea ya existe, iniciar directamente
                iniciarEventoConTarea(tareaExistente.id)
            } else {
                // HU-04: Si el nombre escrito no existe y pulso Iniciar, la app me pide elegir una categoría
                mostrarDialogoCrearTareaRapida(nombreTarea)
            }
        }

        // Botón Finalizar (HU-05)
        binding.btnFinalizar.setOnClickListener {
            finalizarEventoActual()
        }
    }

    private fun mostrarCategoriaSugerida(categoriaNombre: String) {
        binding.tvCategoriaSugerida.text = getString(R.string.categoria_label, categoriaNombre)
        binding.tvCategoriaSugerida.visibility = View.VISIBLE
    }

    private fun ocultarCategoriaSugerida() {
        binding.tvCategoriaSugerida.visibility = View.GONE
    }

    // ==========================================
    // HU-04: CREAR TAREA SI NO EXISTE
    // ==========================================

    private fun mostrarDialogoCrearTareaRapida(nombreTarea: String) {
        val listaCategorias = dbHelper.obtenerCategorias()
        if (listaCategorias.isEmpty()) {
            Toast.makeText(this, "No hay categorías disponibles. Crea una primero.", Toast.LENGTH_LONG).show()
            return
        }

        val dialogBinding = DialogCrearTareaEventoBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvTituloCrearTareaEvento.text = getString(R.string.dialog_crear_tarea_rapida_titulo, nombreTarea)
        dialogBinding.tvMensajeCrearTareaEvento.text = getString(R.string.dialog_crear_tarea_rapida_msg)

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaCategorias
        )
        dialogBinding.spCategoriaRapida.adapter = spinnerAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_iniciar, null)
            .setNegativeButton(R.string.cancelar, null)
            .create()

        dialog.setOnShowListener {
            val btnConfirmar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnConfirmar.setOnClickListener {
                val categoria = dialogBinding.spCategoriaRapida.selectedItem as? Categoria
                if (categoria == null) {
                    Toast.makeText(this, R.string.error_tarea_sin_categoria, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Guardar la tarea en la base de datos (aparece también en la lista de tareas del Sprint 1)
                val nuevaTareaId = dbHelper.insertarTarea(nombreTarea, categoria.id)
                if (nuevaTareaId > 0) {
                    Toast.makeText(this, R.string.msg_tarea_creada, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    // Iniciar el evento inmediatamente con la nueva tarea
                    iniciarEventoConTarea(nuevaTareaId)
                } else {
                    Toast.makeText(this, R.string.error_tarea_duplicada, Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    // ==========================================
    // HU-05: INICIAR Y FINALIZAR EVENTO
    // ==========================================

    private fun iniciarEventoConTarea(tareaId: Long) {
        // HU-05: Se toma la fecha y hora actual del celular (no la escribe el usuario)
        val ahora = LocalDateTime.now()
        val fechaInicioStr = ahora.format(dateTimeFormatter)

        val eventoId = dbHelper.iniciarEvento(tareaId, fechaInicioStr)
        if (eventoId > 0) {
            Toast.makeText(this, R.string.msg_evento_iniciado, Toast.LENGTH_SHORT).show()
            binding.actvBuscarTarea.setText("")
            ocultarCategoriaSugerida()
            actualizarVistaEventos()
        } else {
            Toast.makeText(this, R.string.error_evento_ya_en_curso, Toast.LENGTH_SHORT).show()
        }
    }

    private fun finalizarEventoActual() {
        val eventoEnCurso = dbHelper.obtenerEventoEnCurso() ?: return

        // HU-05: Fecha y hora de fin del celular y duración
        val ahora = LocalDateTime.now()
        val fechaFinStr = ahora.format(dateTimeFormatter)

        val inicioLdt = runCatching {
            LocalDateTime.parse(eventoEnCurso.fechaInicio, dateTimeFormatter)
        }.getOrDefault(ahora)

        val duracionSegundos = Duration.between(inicioLdt, ahora).seconds.coerceAtLeast(0)

        val exito = dbHelper.finalizarEvento(eventoEnCurso.id, fechaFinStr, duracionSegundos)
        if (exito) {
            val eventoFinalizado = eventoEnCurso.copy(
                fechaFin = fechaFinStr,
                duracionSegundos = duracionSegundos
            )
            Toast.makeText(
                this,
                getString(R.string.msg_evento_finalizado, eventoFinalizado.duracionFormateada()),
                Toast.LENGTH_SHORT
            ).show()
            actualizarVistaEventos()
        }
    }

    private fun actualizarVistaEventos() {
        val eventoEnCurso = dbHelper.obtenerEventoEnCurso()

        if (eventoEnCurso != null) {
            // Estado: Evento en curso activo
            binding.badgeEstadoEvento.text = "EN CURSO"
            binding.badgeEstadoEvento.setBackgroundResource(R.drawable.bg_badge_active)
            binding.layoutDatosEventoEnCurso.visibility = View.VISIBLE
            binding.tvSinEventoEnCurso.visibility = View.GONE

            binding.tvEnCursoTareaNombre.text = eventoEnCurso.tareaNombre
            binding.tvEnCursoCategoria.text = getString(R.string.categoria_label, eventoEnCurso.categoriaNombre)
            binding.tvEnCursoHoraInicio.text = getString(R.string.hora_inicio_label, eventoEnCurso.fechaInicio)

            // Criterio HU-05: Iniciar queda desactivado y Finalizar activado
            binding.btnIniciar.isEnabled = false
            binding.btnIniciar.alpha = 0.5f
            binding.btnFinalizar.isEnabled = true
            binding.btnFinalizar.alpha = 1.0f
        } else {
            // Estado: Inactivo
            binding.badgeEstadoEvento.text = "INACTIVO"
            binding.badgeEstadoEvento.setBackgroundResource(R.drawable.bg_badge_muted)
            binding.layoutDatosEventoEnCurso.visibility = View.GONE
            binding.tvSinEventoEnCurso.visibility = View.VISIBLE

            binding.btnIniciar.isEnabled = true
            binding.btnIniciar.alpha = 1.0f
            binding.btnFinalizar.isEnabled = false
            binding.btnFinalizar.alpha = 0.5f
        }

        // Cargar historial de eventos finalizados
        val historial = dbHelper.obtenerHistorialEventos()
        eventoAdapter.actualizarLista(historial)

        if (historial.isEmpty()) {
            binding.layoutEmptyEventos.visibility = View.VISIBLE
            binding.rvEventos.visibility = View.GONE
        } else {
            binding.layoutEmptyEventos.visibility = View.GONE
            binding.rvEventos.visibility = View.VISIBLE
        }
    }

    // ==========================================
    // CAMBIO DE VISTAS (TABS)
    // ==========================================

    private fun cargarVistaEventos() {
        binding.layoutEventosContainer.visibility = View.VISIBLE
        binding.layoutListaContainer.visibility = View.GONE
        binding.fabAdd.hide()
        actualizarVistaEventos()
    }

    private fun cargarVistaTareas() {
        binding.layoutEventosContainer.visibility = View.GONE
        binding.layoutListaContainer.visibility = View.VISIBLE
        binding.fabAdd.show()
        binding.fabAdd.contentDescription = getString(R.string.crear_tarea)

        val listaTareas = dbHelper.obtenerTareas()
        binding.rvItems.adapter = tareaAdapter
        tareaAdapter.actualizarLista(listaTareas)

        if (listaTareas.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvItems.visibility = View.GONE
            binding.ivEmptyIcon.setImageResource(R.drawable.ic_task)
            binding.tvEmptyMessage.text = getString(R.string.empty_tareas)
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvItems.visibility = View.VISIBLE
        }
    }

    private fun cargarVistaCategorias() {
        binding.layoutEventosContainer.visibility = View.GONE
        binding.layoutListaContainer.visibility = View.VISIBLE
        binding.fabAdd.show()
        binding.fabAdd.contentDescription = getString(R.string.crear_categoria)

        val listaCategorias = dbHelper.obtenerCategorias()
        binding.rvItems.adapter = categoriaAdapter
        categoriaAdapter.actualizarLista(listaCategorias)

        if (listaCategorias.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvItems.visibility = View.GONE
            binding.ivEmptyIcon.setImageResource(R.drawable.ic_category)
            binding.tvEmptyMessage.text = getString(R.string.empty_categorias)
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvItems.visibility = View.VISIBLE
        }
    }

    // ==========================================
    // HU-01: GESTIONAR CATEGORÍAS (SPRINT 1)
    // ==========================================

    private fun mostrarDialogoCrearCategoria() {
        val dialogBinding = DialogCategoriaBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogCategoriaTitulo.text = getString(R.string.crear_categoria)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)
            .create()

        dialog.setOnShowListener {
            val botonGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            botonGuardar.setOnClickListener {
                val nombre = dialogBinding.etNombreCategoria.text?.toString()?.trim().orEmpty()

                if (nombre.isEmpty()) {
                    dialogBinding.tilNombreCategoria.error = getString(R.string.error_categoria_vacia)
                    return@setOnClickListener
                }

                if (dbHelper.existeCategoria(nombre)) {
                    dialogBinding.tilNombreCategoria.error = getString(R.string.error_categoria_duplicada)
                    Toast.makeText(this, R.string.error_categoria_duplicada, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val resultado = dbHelper.insertarCategoria(nombre)
                if (resultado != -1L) {
                    Toast.makeText(this, R.string.msg_categoria_creada, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    cargarVistaCategorias()
                } else {
                    dialogBinding.tilNombreCategoria.error = getString(R.string.error_categoria_duplicada)
                }
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoEditarCategoria(categoria: Categoria) {
        val dialogBinding = DialogCategoriaBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogCategoriaTitulo.text = getString(R.string.editar_categoria)
        dialogBinding.etNombreCategoria.setText(categoria.nombre)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)
            .create()

        dialog.setOnShowListener {
            val botonGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            botonGuardar.setOnClickListener {
                val nuevoNombre = dialogBinding.etNombreCategoria.text?.toString()?.trim().orEmpty()

                if (nuevoNombre.isEmpty()) {
                    dialogBinding.tilNombreCategoria.error = getString(R.string.error_categoria_vacia)
                    return@setOnClickListener
                }

                if (dbHelper.existeCategoria(nuevoNombre, categoria.id)) {
                    dialogBinding.tilNombreCategoria.error = getString(R.string.error_categoria_duplicada)
                    Toast.makeText(this, R.string.error_categoria_duplicada, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val exito = dbHelper.actualizarCategoria(categoria.id, nuevoNombre)
                if (exito) {
                    Toast.makeText(this, R.string.msg_categoria_actualizada, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    cargarVistaCategorias()
                } else {
                    dialogBinding.tilNombreCategoria.error = getString(R.string.error_categoria_duplicada)
                }
            }
        }
        dialog.show()
    }

    private fun intentarEliminarCategoria(categoria: Categoria) {
        val tareasAsociadas = dbHelper.contarTareasPorCategoria(categoria.id)
        if (tareasAsociadas > 0) {
            AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_categoria)
                .setMessage(getString(R.string.error_categoria_con_tareas, categoria.nombre))
                .setPositiveButton(R.string.entendido, null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.eliminar_categoria)
            .setMessage(getString(R.string.confirmar_eliminar_categoria, categoria.nombre))
            .setPositiveButton(R.string.eliminar) { _, _ ->
                val eliminada = dbHelper.eliminarCategoria(categoria.id)
                if (eliminada) {
                    Toast.makeText(this, R.string.msg_categoria_eliminada, Toast.LENGTH_SHORT).show()
                    cargarVistaCategorias()
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    // ==========================================
    // HU-02: GESTIONAR TAREAS CON CATEGORÍA (SPRINT 1)
    // ==========================================

    private fun mostrarDialogoCrearTarea() {
        val listaCategorias = dbHelper.obtenerCategorias()
        if (listaCategorias.isEmpty()) {
            Toast.makeText(this, "Primero debes crear al menos una categoría", Toast.LENGTH_LONG).show()
            return
        }

        val dialogBinding = DialogTareaBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTareaTitulo.text = getString(R.string.crear_tarea)

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaCategorias
        )
        dialogBinding.spCategoria.adapter = spinnerAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)
            .create()

        dialog.setOnShowListener {
            val botonGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            botonGuardar.setOnClickListener {
                val nombre = dialogBinding.etNombreTarea.text?.toString()?.trim().orEmpty()

                if (nombre.isEmpty()) {
                    dialogBinding.tilNombreTarea.error = getString(R.string.error_tarea_vacia)
                    return@setOnClickListener
                }

                if (dbHelper.existeTarea(nombre)) {
                    dialogBinding.tilNombreTarea.error = getString(R.string.error_tarea_duplicada)
                    Toast.makeText(this, R.string.error_tarea_duplicada, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val categoriaSeleccionada = dialogBinding.spCategoria.selectedItem as? Categoria
                if (categoriaSeleccionada == null) {
                    Toast.makeText(this, R.string.error_tarea_sin_categoria, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val resultado = dbHelper.insertarTarea(nombre, categoriaSeleccionada.id)
                if (resultado != -1L) {
                    Toast.makeText(this, R.string.msg_tarea_creada, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    cargarVistaTareas()
                } else {
                    dialogBinding.tilNombreTarea.error = getString(R.string.error_tarea_duplicada)
                }
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoEditarTarea(tarea: Tarea) {
        val listaCategorias = dbHelper.obtenerCategorias()
        if (listaCategorias.isEmpty()) {
            Toast.makeText(this, "No hay categorías disponibles", Toast.LENGTH_LONG).show()
            return
        }

        val dialogBinding = DialogTareaBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTareaTitulo.text = getString(R.string.editar_tarea)
        dialogBinding.etNombreTarea.setText(tarea.nombre)

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listaCategorias
        )
        dialogBinding.spCategoria.adapter = spinnerAdapter

        val posicionActual = listaCategorias.indexOfFirst { it.id == tarea.categoriaId }
        if (posicionActual >= 0) {
            dialogBinding.spCategoria.setSelection(posicionActual)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)
            .create()

        dialog.setOnShowListener {
            val botonGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            botonGuardar.setOnClickListener {
                val nuevoNombre = dialogBinding.etNombreTarea.text?.toString()?.trim().orEmpty()

                if (nuevoNombre.isEmpty()) {
                    dialogBinding.tilNombreTarea.error = getString(R.string.error_tarea_vacia)
                    return@setOnClickListener
                }

                if (dbHelper.existeTarea(nuevoNombre, tarea.id)) {
                    dialogBinding.tilNombreTarea.error = getString(R.string.error_tarea_duplicada)
                    Toast.makeText(this, R.string.error_tarea_duplicada, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val categoriaSeleccionada = dialogBinding.spCategoria.selectedItem as? Categoria
                if (categoriaSeleccionada == null) {
                    Toast.makeText(this, R.string.error_tarea_sin_categoria, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val exito = dbHelper.actualizarTarea(tarea.id, nuevoNombre, categoriaSeleccionada.id)
                if (exito) {
                    Toast.makeText(this, R.string.msg_tarea_actualizada, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    cargarVistaTareas()
                } else {
                    dialogBinding.tilNombreTarea.error = getString(R.string.error_tarea_duplicada)
                }
            }
        }
        dialog.show()
    }

    private fun confirmarEliminarTarea(tarea: Tarea) {
        AlertDialog.Builder(this)
            .setTitle(R.string.eliminar_tarea)
            .setMessage(getString(R.string.confirmar_eliminar_tarea, tarea.nombre))
            .setPositiveButton(R.string.eliminar) { _, _ ->
                val eliminada = dbHelper.eliminarTarea(tarea.id)
                if (eliminada) {
                    Toast.makeText(this, R.string.msg_tarea_eliminada, Toast.LENGTH_SHORT).show()
                    cargarVistaTareas()
                }
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
}