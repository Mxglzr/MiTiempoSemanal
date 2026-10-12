package com.sanchez.mitiemposemanal.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sanchez.mitiemposemanal.data.model.Categoria
import com.sanchez.mitiemposemanal.databinding.ItemCategoriaBinding

class CategoriaAdapter(
    private var categorias: List<Categoria>,
    private val getCantidadTareas: (Long) -> Int,
    private val onEditClick: (Categoria) -> Unit,
    private val onDeleteClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    inner class CategoriaViewHolder(val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val binding = ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val categoria = categorias[position]
        val cantidad = getCantidadTareas(categoria.id)

        holder.binding.tvNombreCategoria.text = categoria.nombre
        holder.binding.tvCantidadTareas.text = if (cantidad == 1) {
            "1 tarea asociada"
        } else {
            "$cantidad tareas asociadas"
        }

        holder.binding.btnEditarCategoria.setOnClickListener {
            onEditClick(categoria)
        }

        holder.binding.btnEliminarCategoria.setOnClickListener {
            onDeleteClick(categoria)
        }
    }

    override fun getItemCount(): Int = categorias.size

    fun actualizarLista(nuevaLista: List<Categoria>) {
        categorias = nuevaLista
        notifyDataSetChanged()
    }
}
