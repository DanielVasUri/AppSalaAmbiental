package com.example.ejercicio14_10_26.data.model

data class EstadoSistema(
    val hum: Int,
    val temp: Double,
    val estado: String,
    val ventilador: String,
    val velocidad: Int,
    val modo: String
)
