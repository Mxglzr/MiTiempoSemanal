package com.sanchez.mitiemposemanal

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.sanchez.mitiemposemanal.data.DatabaseHelper
import com.sanchez.mitiemposemanal.data.model.Categoria
import com.sanchez.mitiemposemanal.data.model.Tarea
import com.sanchez.mitiemposemanal.databinding.ActivityMainBinding
import com.sanchez.mitiemposemanal.databinding.DialogCategoriaBinding
import com.sanchez.mitiemposemanal.databinding.DialogTareaBinding
import com.sanchez.mitiemposemanal.ui.adapter.CategoriaAdapter
import com.sanchez.mitiemposemanal.ui.adapter.TareaAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tareaAdapter: TareaAdapter
    private lateinit var categoriaAdapter: CategoriaAdapter

    private var tabActual: Int = TAB_TAREAS

    companion object {
        private const val TAB_TAREAS = 0
        private const val TAB_CATEGORIAS = 1
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

        configurarRecyclerView()
        configurarTabs()
        configurarFab()

        // Cargar vista inicial de Tareas
        cargarVistaTareas()
    }

    private fun configurarRecyclerView() {
        binding.rvItems.layoutManager = LinearLayoutManager(this)

        // Inicializar adaptadores
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
    }

    private fun configurarTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
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
            if (tabActual == TAB_TAREAS) {
                mostrarDialogoCrearTarea()
            } else {
                mostrarDialogoCrearCategoria()
            }
        }
    }

    // ==========================================
    // CARGAR VISTAS
    // ==========================================

    private fun cargarVistaTareas() {
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
        binding.fabAdd.contentDescription = getString(R.string.crear_tarea)
    }

    private fun cargarVistaCategorias() {
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
        binding.fabAdd.contentDescription = getString(R.string.crear_categoria)
    }

    // ==========================================
    // HU-01: GESTIONAR CATEGORÍAS
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
        // Criterio de aceptación: Si la categoría tiene tareas, la app no permite eliminarla y me avisa
        val tareasAsociadas = dbHelper.contarTareasPorCategoria(categoria.id)
        if (tareasAsociadas > 0) {
            AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_categoria)
                .setMessage(getString(R.string.error_categoria_con_tareas, categoria.nombre))
                .setPositiveButton(R.string.entendido, null)
                .show()
            return
        }

        // Criterio de aceptación: Puedo eliminar una categoría, pidiendo confirmación
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
    // HU-02: GESTIONAR TAREAS CON CATEGORÍA
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

                // Criterio: No se puede guardar una tarea sin nombre
                if (nombre.isEmpty()) {
                    dialogBinding.tilNombreTarea.error = getString(R.string.error_tarea_vacia)
                    return@setOnClickListener
                }

                // Criterio: No se pueden repetir nombres de tareas
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

        // Seleccionar la categoría actual de la tarea
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