package com.example.ejercicio14_10_26.data.repository

import com.example.ejercicio14_10_26.data.model.EstadoSistema
import com.example.ejercicio14_10_26.data.model.Resultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MockArduinoRepository : ArduinoRepository {

    private var actualHum = 50
    private var humSubiendo = true
    private var modoActual = "AUTO"
    private var manualEncendido = false
    private var manualVelocidad = 0

    override suspend fun obtenerEstado(): Resultado<EstadoSistema> = withContext(Dispatchers.IO) {
        delay(300)
        actualizarSimulacion()
        Resultado.Exito(generarEstadoActual())
    }

    override suspend fun enviarControlManual(encendido: Boolean, velocidad: Int): Resultado<EstadoSistema> = withContext(Dispatchers.IO) {
        delay(300)
        modoActual = "MANUAL"
        manualEncendido = encendido
        manualVelocidad = velocidad.coerceIn(0, 100)
        actualizarSimulacion()
        Resultado.Exito(generarEstadoActual())
    }

    override suspend fun activarAutomatico(): Resultado<EstadoSistema> = withContext(Dispatchers.IO) {
        delay(300)
        modoActual = "AUTO"
        actualizarSimulacion()
        Resultado.Exito(generarEstadoActual())
    }

    private fun actualizarSimulacion() {
        val paso = (1..3).random()
        if (humSubiendo) {
            actualHum += paso
            if (actualHum >= 90) {
                actualHum = 90
                humSubiendo = false
            }
        } else {
            actualHum -= paso
            if (actualHum <= 40) {
                actualHum = 40
                humSubiendo = true
            }
        }

        val nuevoEstado = calcularEstado(actualHum)
        if (modoActual == "MANUAL" && nuevoEstado == "NORMAL") {
            modoActual = "AUTO"
        }
    }

    private fun calcularEstado(hum: Int): String {
        return when {
            hum >= 75 -> "ALTO"
            hum >= 60 -> "ADVERTENCIA"
            else -> "NORMAL"
        }
    }

    private fun generarEstadoActual(): EstadoSistema {
        val estadoStr = calcularEstado(actualHum)
        val temp = 20.0 + (actualHum - 40) * 0.2

        val (ventiladorStr, velocidadVal) = if (modoActual == "AUTO") {
            if (estadoStr == "ALTO") {
                Pair("ON", 80)
            } else {
                Pair("OFF", 0)
            }
        } else {
            if (manualEncendido) {
                Pair("ON", manualVelocidad)
            } else {
                Pair("OFF", 0)
            }
        }

        return EstadoSistema(
            hum = actualHum,
            temp = (temp * 10).roundToInt() / 10.0,
            estado = estadoStr,
            ventilador = ventiladorStr,
            velocidad = velocidadVal,
            modo = modoActual
        )
    }
}
