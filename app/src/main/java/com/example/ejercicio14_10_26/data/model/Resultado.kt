package com.example.ejercicio14_10_26.data.model

sealed class Resultado<out T> {
    data class Exito<out T>(val datos: T) : Resultado<T>()
    data class Error(val error: ErrorConexion) : Resultado<Nothing>()
}
