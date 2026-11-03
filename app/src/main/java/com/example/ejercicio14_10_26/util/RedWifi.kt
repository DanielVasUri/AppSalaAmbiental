package com.example.ejercicio14_10_26.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class RedWifi(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun activar() {
        if (connectivityManager == null) return

        liberar()

        try {
            val builder = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    try {
                        connectivityManager.bindProcessToNetwork(network)
                    } catch (e: Exception) {
                        // Ignorar error al atar proceso a la red
                    }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    // Si no hay red WiFi disponible, no hacer nada y continuar
                }
            }

            networkCallback = callback
            connectivityManager.requestNetwork(builder.build(), callback)
        } catch (e: Exception) {
            // Si falla la solicitud o permisos, no lanzar error
            networkCallback = null
        }
    }

    fun liberar() {
        if (connectivityManager == null) return

        try {
            connectivityManager.bindProcessToNetwork(null)
        } catch (e: Exception) {
            // Ignorar error
        }

        networkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignorar si no estaba registrado
            }
            networkCallback = null
        }
    }
}
