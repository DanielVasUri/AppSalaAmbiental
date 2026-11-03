package com.example.ejercicio14_10_26.data.repository

import com.example.ejercicio14_10_26.data.model.ErrorConexion
import com.example.ejercicio14_10_26.data.model.EstadoSistema
import com.example.ejercicio14_10_26.data.model.Resultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class ArduinoHttpRepository(private val rawIp: String) : ArduinoRepository {

    override suspend fun obtenerEstado(): Resultado<EstadoSistema> = withContext(Dispatchers.IO) {
        ejecutarPeticion(path = "status", method = "GET", bodyJson = null)
    }

    override suspend fun enviarControlManual(encendido: Boolean, velocidad: Int): Resultado<EstadoSistema> = withContext(Dispatchers.IO) {
        val velClamped = velocidad.coerceIn(0, 100)
        val body = "{\"encendido\":$encendido,\"velocidad\":$velClamped}"
        ejecutarPeticion(path = "fan", method = "POST", bodyJson = body)
    }

    override suspend fun activarAutomatico(): Resultado<EstadoSistema> = withContext(Dispatchers.IO) {
        ejecutarPeticion(path = "auto", method = "POST", bodyJson = "{}")
    }

    private fun ejecutarPeticion(
        path: String,
        method: String,
        bodyJson: String?
    ): Resultado<EstadoSistema> {
        val cleanIp = rawIp.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')

        if (cleanIp.isBlank()) {
            return Resultado.Error(ErrorConexion.SinIp)
        }

        val urlString = "http://$cleanIp/$path"
        var connection: HttpURLConnection? = null

        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 3000
                readTimeout = 5000
                useCaches = false
                setRequestProperty("Connection", "close")

                if (method == "POST") {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    val payload = (bodyJson ?: "{}").toByteArray(Charsets.UTF_8)
                    setFixedLengthStreamingMode(payload.size)
                    outputStream.use { os ->
                        os.write(payload)
                        os.flush()
                    }
                }
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Resultado.Error(ErrorConexion.ErrorHttp(responseCode))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }.trim()
            parsearEstadoSistema(responseBody)
        } catch (e: SocketTimeoutException) {
            Resultado.Error(ErrorConexion.Timeout)
        } catch (e: ConnectException) {
            Resultado.Error(ErrorConexion.Inalcanzable)
        } catch (e: UnknownHostException) {
            Resultado.Error(ErrorConexion.Inalcanzable)
        } catch (e: IOException) {
            Resultado.Error(ErrorConexion.Inalcanzable)
        } catch (e: Exception) {
            Resultado.Error(ErrorConexion.RespuestaInvalida)
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) {
                // Ignore disconnect exceptions
            }
        }
    }

    private fun parsearEstadoSistema(jsonText: String): Resultado<EstadoSistema> {
        if (jsonText.isEmpty()) {
            return Resultado.Error(ErrorConexion.RespuestaInvalida)
        }

        return try {
            val json = JSONObject(jsonText)

            if (!json.has("hum") || !json.has("temp") || !json.has("estado") ||
                !json.has("ventilador") || !json.has("velocidad") || !json.has("modo")
            ) {
                return Resultado.Error(ErrorConexion.RespuestaInvalida)
            }

            val hum = json.optInt("hum", -1)
            val temp = json.optDouble("temp", Double.NaN)
            val estado = json.optString("estado", "")
            val ventilador = json.optString("ventilador", "")
            val velocidad = json.optInt("velocidad", -1)
            val modo = json.optString("modo", "")

            if (hum !in 0..100) return Resultado.Error(ErrorConexion.RespuestaInvalida)
            if (temp.isNaN() || temp.isInfinite()) return Resultado.Error(ErrorConexion.RespuestaInvalida)
            if (estado !in listOf("NORMAL", "ADVERTENCIA", "ALTO")) return Resultado.Error(ErrorConexion.RespuestaInvalida)
            if (ventilador !in listOf("ON", "OFF")) return Resultado.Error(ErrorConexion.RespuestaInvalida)
            if (velocidad !in 0..100) return Resultado.Error(ErrorConexion.RespuestaInvalida)
            if (modo !in listOf("AUTO", "MANUAL")) return Resultado.Error(ErrorConexion.RespuestaInvalida)

            Resultado.Exito(
                EstadoSistema(
                    hum = hum,
                    temp = temp,
                    estado = estado,
                    ventilador = ventilador,
                    velocidad = velocidad,
                    modo = modo
                )
            )
        } catch (e: Exception) {
            Resultado.Error(ErrorConexion.RespuestaInvalida)
        }
    }
}
