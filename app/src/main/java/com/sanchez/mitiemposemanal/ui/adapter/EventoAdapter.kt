package com.sanchez.mitiemposemanal.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sanchez.mitiemposemanal.R
import com.sanchez.mitiemposemanal.data.model.Evento
import com.sanchez.mitiemposemanal.databinding.ItemEventoBinding

class EventoAdapter(
    private var eventos: List<Evento>
) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    inner class EventoViewHolder(val binding: ItemEventoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val binding = ItemEventoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventos[position]
        val context = holder.itemView.context

        holder.binding.tvTareaNombre.text = evento.tareaNombre
        holder.binding.tvCategoriaNombre.text = context.getString(R.string.categoria_label, evento.categoriaNombre)
        holder.binding.tvFechaInicio.text = context.getString(R.string.hora_inicio_label, evento.fechaInicio)
        holder.binding.tvFechaFin.text = context.getString(R.string.hora_fin_label, evento.fechaFin ?: "-")
        holder.binding.tvDuracionBadge.text = evento.duracionFormateada()
    }

    override fun getItemCount(): Int = eventos.size

    fun actualizarLista(nuevaLista: List<Evento>) {
        eventos = nuevaLista
        notifyDataSetChanged()
    }
}
