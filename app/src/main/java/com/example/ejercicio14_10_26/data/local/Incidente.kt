package com.example.ejercicio14_10_26.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidentes")
data class Incidente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fechaHoraInicio: Long,
    val fechaHoraFin: Long? = null,
    val llegoAAlto: Boolean = false,
    val tipoFinal: String? = null
)
