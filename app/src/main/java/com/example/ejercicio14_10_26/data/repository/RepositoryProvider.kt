package com.example.ejercicio14_10_26.data.repository

import android.content.Context
import com.example.ejercicio14_10_26.data.local.ConfigStore

object RepositoryProvider {

    fun obtenerRepositorio(context: Context): ArduinoRepository {
        val configStore = ConfigStore(context)
        return if (configStore.modoSimulacion) {
            MockArduinoRepository()
        } else {
            ArduinoHttpRepository(configStore.ipArduino)
        }
    }
}
