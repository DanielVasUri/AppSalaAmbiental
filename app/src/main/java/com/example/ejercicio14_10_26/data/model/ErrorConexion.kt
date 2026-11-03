package com.example.ejercicio14_10_26.data.model

sealed class ErrorConexion {
    object SinIp : ErrorConexion()
    object Timeout : ErrorConexion()
    object Inalcanzable : ErrorConexion()
    data class ErrorHttp(val codigo: Int) : ErrorConexion()
    object RespuestaInvalida : ErrorConexion()
}
