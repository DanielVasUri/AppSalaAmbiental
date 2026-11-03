package com.example.ejercicio14_10_26

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ejercicio14_10_26.data.local.Incidente
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistorialAdapter(private val incidentes: List<Incidente>) :
    RecyclerView.Adapter<HistorialAdapter.IncidenteViewHolder>() {

    class IncidenteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInicio: TextView = view.findViewById(R.id.tvItemInicio)
        val tvFin: TextView = view.findViewById(R.id.tvItemFin)
        val tvTipo: TextView = view.findViewById(R.id.tvItemTipo)
        val cardItem: MaterialCardView = view.findViewById(R.id.cardItemIncidente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidenteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incidente, parent, false)
        return IncidenteViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncidenteViewHolder, position: Int) {
        val item = incidentes[position]
        val context = holder.itemView.context

        holder.tvInicio.text = context.getString(R.string.lbl_historial_inicio, formatTimestamp(item.fechaHoraInicio))

        if (item.fechaHoraFin == null) {
            val textoEnCurso = context.getString(R.string.en_curso)
            holder.tvFin.text = context.getString(R.string.lbl_historial_fin, textoEnCurso)
            holder.tvTipo.text = context.getString(R.string.lbl_historial_tipo, textoEnCurso)
            holder.cardItem.setCardBackgroundColor(ContextCompat.getColor(context, R.color.conexion_simulacion))
        } else {
            holder.tvFin.text = context.getString(R.string.lbl_historial_fin, formatTimestamp(item.fechaHoraFin))
            val tipo = item.tipoFinal ?: if (item.llegoAAlto) "ALTO" else "ADVERTENCIA"
            holder.tvTipo.text = context.getString(R.string.lbl_historial_tipo, tipo)

            if (tipo == "ALTO") {
                holder.cardItem.setCardBackgroundColor(ContextCompat.getColor(context, R.color.estado_alto))
            } else {
                holder.cardItem.setCardBackgroundColor(ContextCompat.getColor(context, R.color.estado_advertencia))
            }
        }
    }

    override fun getItemCount(): Int = incidentes.size

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "En curso"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
