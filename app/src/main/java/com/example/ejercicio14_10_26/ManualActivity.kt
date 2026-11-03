package com.example.ejercicio14_10_26

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.ejercicio14_10_26.data.local.ConfigStore
import com.example.ejercicio14_10_26.data.model.ErrorConexion
import com.example.ejercicio14_10_26.data.model.EstadoSistema
import com.example.ejercicio14_10_26.data.model.Resultado
import com.example.ejercicio14_10_26.data.repository.RepositoryProvider
import com.example.ejercicio14_10_26.util.RedWifi
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ManualActivity : AppCompatActivity() {

    private lateinit var configStore: ConfigStore
    private lateinit var redWifi: RedWifi

    private lateinit var btnBackHeader: ImageButton
    private lateinit var tvManualVentilador: TextView
    private lateinit var tvManualVelocidad: TextView
    private lateinit var tvManualModo: TextView
    private lateinit var tvManualHumedad: TextView

    private lateinit var swVentiladorEstado: MaterialSwitch
    private lateinit var tvVelocidadValor: TextView
    private lateinit var sliderVelocidad: Slider

    private lateinit var tvManualError: TextView
    private lateinit var btnVolverBottom: Button

    private var pollingJob: Job? = null
    private var peticionEnCurso = false
    private var sincronizandoUI = false
    private var ultimaVelocidadElegida = 60
    private var ultimoEstadoReal: EstadoSistema? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manual)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manual_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configStore = ConfigStore(this)
        redWifi = RedWifi(this)

        btnBackHeader = findViewById(R.id.btnBackHeader)
        tvManualVentilador = findViewById(R.id.tvManualVentilador)
        tvManualVelocidad = findViewById(R.id.tvManualVelocidad)
        tvManualModo = findViewById(R.id.tvManualModo)
        tvManualHumedad = findViewById(R.id.tvManualHumedad)

        swVentiladorEstado = findViewById(R.id.swVentiladorEstado)
        tvVelocidadValor = findViewById(R.id.tvVelocidadValor)
        sliderVelocidad = findViewById(R.id.sliderVelocidad)

        tvManualError = findViewById(R.id.tvManualError)
        btnVolverBottom = findViewById(R.id.btnVolverBottom)

        btnBackHeader.setOnClickListener { finish() }
        btnVolverBottom.setOnClickListener { finish() }

        // Inicialmente deshabilitado si está apagado
        sliderVelocidad.isEnabled = swVentiladorEstado.isChecked

        sliderVelocidad.addOnChangeListener { _, value, _ ->
            ultimaVelocidadElegida = value.toInt()
            tvVelocidadValor.text = getString(R.string.lbl_velocidad_slider, ultimaVelocidadElegida)
        }

        sliderVelocidad.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                if (sincronizandoUI || peticionEnCurso) return
                val encendido = swVentiladorEstado.isChecked
                enviarComandoManual(encendido, ultimaVelocidadElegida)
            }
        })

        swVentiladorEstado.setOnCheckedChangeListener { _, isChecked ->
            if (sincronizandoUI || peticionEnCurso) return@setOnCheckedChangeListener
            sliderVelocidad.isEnabled = isChecked
            enviarComandoManual(isChecked, ultimaVelocidadElegida)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!configStore.modoSimulacion) {
            redWifi.activar()
        }
        iniciarPolling()
    }

    override fun onStop() {
        super.onStop()
        detenerPolling()
        redWifi.liberar()
    }

    private fun iniciarPolling() {
        detenerPolling()
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                consultarEstadoActual()
                delay(2000)
            }
        }
    }

    private fun detenerPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun consultarEstadoActual() {
        if (peticionEnCurso) return

        lifecycleScope.launch {
            val repository = RepositoryProvider.obtenerRepositorio(this@ManualActivity)
            val resultado = repository.obtenerEstado()

            if (!peticionEnCurso) {
                when (resultado) {
                    is Resultado.Exito -> {
                        tvManualError.visibility = View.GONE
                        sincronizarEstadoUI(resultado.datos)
                    }
                    is Resultado.Error -> {
                        tvManualError.text = obtenerTextoError(resultado.error)
                        tvManualError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun enviarComandoManual(encendido: Boolean, velocidad: Int) {
        peticionEnCurso = true
        actualizarEstadoControlesHabilitados(false)
        tvManualError.visibility = View.GONE

        lifecycleScope.launch {
            val repository = RepositoryProvider.obtenerRepositorio(this@ManualActivity)
            val resultado = repository.enviarControlManual(encendido, velocidad)

            peticionEnCurso = false

            when (resultado) {
                is Resultado.Exito -> {
                    val mensajeConfirmacion = if (encendido) {
                        getString(R.string.msg_fan_encendido, velocidad)
                    } else {
                        getString(R.string.msg_fan_apagado)
                    }
                    Toast.makeText(
                        this@ManualActivity,
                        mensajeConfirmacion,
                        Toast.LENGTH_SHORT
                    ).show()
                    sincronizarEstadoUI(resultado.datos)
                }
                is Resultado.Error -> {
                    val msgError = obtenerTextoError(resultado.error)
                    tvManualError.text = getString(R.string.msg_comando_manual_error, msgError)
                    tvManualError.visibility = View.VISIBLE
                    revertirAEstadoRealConocido()
                }
            }

            actualizarEstadoControlesHabilitados(true)
        }
    }

    private fun sincronizarEstadoUI(estado: EstadoSistema) {
        ultimoEstadoReal = estado

        tvManualVentilador.text = getString(R.string.lbl_estado_ventilador_fmt, estado.ventilador)
        tvManualVelocidad.text = getString(R.string.lbl_velocidad_fmt, estado.velocidad)
        tvManualModo.text = getString(R.string.lbl_modo_fmt, estado.modo)
        tvManualHumedad.text = getString(R.string.lbl_humedad_fmt, estado.hum)

        sincronizandoUI = true
        val estaEncendido = estado.ventilador.equals("ON", ignoreCase = true)
        swVentiladorEstado.isChecked = estaEncendido

        if (estado.velocidad in 0..100) {
            ultimaVelocidadElegida = estado.velocidad
            sliderVelocidad.value = estado.velocidad.toFloat()
            tvVelocidadValor.text = getString(R.string.lbl_velocidad_slider, ultimaVelocidadElegida)
        }

        if (!peticionEnCurso) {
            sliderVelocidad.isEnabled = estaEncendido
        }

        sincronizandoUI = false
    }

    private fun revertirAEstadoRealConocido() {
        ultimoEstadoReal?.let { estado ->
            sincronizarEstadoUI(estado)
        }
    }

    private fun actualizarEstadoControlesHabilitados(habilitar: Boolean) {
        swVentiladorEstado.isEnabled = habilitar
        if (!habilitar) {
            sliderVelocidad.isEnabled = false
        } else {
            sliderVelocidad.isEnabled = swVentiladorEstado.isChecked
        }
    }

    private fun obtenerTextoError(error: ErrorConexion): String {
        return when (error) {
            is ErrorConexion.SinIp -> getString(R.string.err_sin_ip)
            is ErrorConexion.Timeout -> getString(R.string.err_timeout)
            is ErrorConexion.Inalcanzable -> getString(R.string.err_inalcanzable)
            is ErrorConexion.ErrorHttp -> getString(R.string.err_http, error.codigo)
            is ErrorConexion.RespuestaInvalida -> getString(R.string.err_respuesta_invalida)
        }
    }
}
