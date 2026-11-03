package com.example.ejercicio14_10_26.data.repository

import com.example.ejercicio14_10_26.data.model.EstadoSistema
import com.example.ejercicio14_10_26.data.model.Resultado

interface ArduinoRepository {
    suspend fun obtenerEstado(): Resultado<EstadoSistema>
    suspend fun enviarControlManual(encendido: Boolean, velocidad: Int): Resultado<EstadoSistema>
    suspend fun activarAutomatico(): Resultado<EstadoSistema>
}
