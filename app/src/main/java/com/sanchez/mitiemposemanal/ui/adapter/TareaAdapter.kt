package com.sanchez.mitiemposemanal.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sanchez.mitiemposemanal.data.model.Tarea
import com.sanchez.mitiemposemanal.databinding.ItemTareaBinding

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val onEditClick: (Tarea) -> Unit,
    private val onDeleteClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    inner class TareaViewHolder(val binding: ItemTareaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TareaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = tareas[position]

        // Formato requerido explícitamente: "Nombre Tarea · Categoría"
        holder.binding.tvTareaConCategoria.text = "${tarea.nombre} · ${tarea.categoriaNombre}"
        holder.binding.tvCategoriaBadge.text = "Categoría: ${tarea.categoriaNombre}"

        holder.binding.btnEditarTarea.setOnClickListener {
            onEditClick(tarea)
        }

        holder.binding.btnEliminarTarea.setOnClickListener {
            onDeleteClick(tarea)
        }
    }

    override fun getItemCount(): Int = tareas.size

    fun actualizarLista(nuevaLista: List<Tarea>) {
        tareas = nuevaLista
        notifyDataSetChanged()
    }
}
