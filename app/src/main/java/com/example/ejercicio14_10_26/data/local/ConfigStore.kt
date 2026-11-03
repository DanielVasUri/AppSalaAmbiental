package com.example.ejercicio14_10_26.data.local

import android.content.Context
import android.content.SharedPreferences

class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("app_config", Context.MODE_PRIVATE)

    var ipArduino: String
        get() = prefs.getString(KEY_IP, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_IP, value).apply()
        }

    var modoSimulacion: Boolean
        get() = prefs.getBoolean(KEY_MODO_SIMULACION, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MODO_SIMULACION, value).apply()
        }

    companion object {
        private const val KEY_IP = "ip_arduino"
        private const val KEY_MODO_SIMULACION = "modo_simulacion"
    }
}
