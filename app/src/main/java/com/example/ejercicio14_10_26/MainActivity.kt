package com.example.ejercicio14_10_26

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.ejercicio14_10_26.data.local.AppDatabase
import com.example.ejercicio14_10_26.data.local.ConfigStore
import com.example.ejercicio14_10_26.data.local.Incidente
import com.example.ejercicio14_10_26.data.model.ErrorConexion
import com.example.ejercicio14_10_26.data.model.EstadoSistema
import com.example.ejercicio14_10_26.data.model.Resultado
import com.example.ejercicio14_10_26.data.repository.ArduinoHttpRepository
import com.example.ejercicio14_10_26.data.repository.RepositoryProvider
import com.example.ejercicio14_10_26.util.RedWifi
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var configStore: ConfigStore
    private lateinit var redWifi: RedWifi

    private lateinit var btnHistorial: ImageButton
    private lateinit var btnConfiguracion: ImageButton
    private lateinit var tvHumedad: TextView
    private lateinit var tvTemperatura: TextView
    private lateinit var tvEstadoAula: TextView
    private lateinit var tvVentilador: TextView
    private lateinit var tvVelocidad: TextView
    private lateinit var tvModo: TextView

    private lateinit var cardConexion: MaterialCardView
    private lateinit var tvIndicadorConexion: TextView
    private lateinit var tvDetalleConexion: TextView

    private lateinit var btnManual: Button
    private lateinit var btnAutomatico: Button

    private var pollingJob: Job? = null
    private var permisoSolicitadoAnteriormente = false

    private val requestLocalNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                consultarEstado()
            } else {
                mostrarDialogoExplicativoPermiso()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configStore = ConfigStore(this)
        redWifi = RedWifi(this)

        btnHistorial = findViewById(R.id.btnHistorial)
        btnConfiguracion = findViewById(R.id.btnConfiguracion)
        tvHumedad = findViewById(R.id.tvHumedad)
        tvTemperatura = findViewById(R.id.tvTemperatura)
        tvEstadoAula = findViewById(R.id.tvEstadoAula)
        tvVentilador = findViewById(R.id.tvVentilador)
        tvVelocidad = findViewById(R.id.tvVelocidad)
        tvModo = findViewById(R.id.tvModo)

        cardConexion = findViewById(R.id.cardConexion)
        tvIndicadorConexion = findViewById(R.id.tvIndicadorConexion)
        tvDetalleConexion = findViewById(R.id.tvDetalleConexion)

        btnManual = findViewById(R.id.btnManual)
        btnAutomatico = findViewById(R.id.btnAutomatico)

        btnHistorial.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
            startActivity(intent)
        }

        btnConfiguracion.setOnClickListener {
            mostrarDialogoConfiguracion()
        }

        btnManual.setOnClickListener {
            val intent = Intent(this, ManualActivity::class.java)
            startActivity(intent)
        }

        btnAutomatico.setOnClickListener {
            activarModoAutomatico()
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
                verificarYConsultar()
                delay(2000)
            }
        }
    }

    private fun detenerPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun verificarYConsultar() {
        if (!configStore.modoSimulacion && configStore.ipArduino.isBlank()) {
            mostrarEstadoSinIp()
            return
        }

        if (!configStore.modoSimulacion &&
            Build.VERSION.SDK_INT >= 37 &&
            applicationInfo.targetSdkVersion >= 37
        ) {
            val permiso = "android.permission.ACCESS_LOCAL_NETWORK"
            if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                if (!permisoSolicitadoAnteriormente) {
                    permisoSolicitadoAnteriormente = true
                    requestLocalNetworkPermissionLauncher.launch(permiso)
                } else {
                    mostrarEstadoSinConexion(getString(R.string.permiso_red_local_requerido))
                }
                return
            }
        }

        consultarEstado()
    }

    private fun consultarEstado() {
        lifecycleScope.launch {
            val repository = RepositoryProvider.obtenerRepositorio(this@MainActivity)
            val resultado = repository.obtenerEstado()
            procesarResultado(resultado)
        }
    }

    private fun procesarResultado(resultado: Resultado<EstadoSistema>) {
        when (resultado) {
            is Resultado.Exito -> {
                val estado = resultado.datos
                tvHumedad.text = getString(R.string.lbl_humedad, "${estado.hum} %")
                tvTemperatura.text = getString(R.string.lbl_temperatura, "${estado.temp} °C")
                tvEstadoAula.text = estado.estado

                when (estado.estado.uppercase()) {
                    "NORMAL" -> tvEstadoAula.setBackgroundColor(ContextCompat.getColor(this, R.color.estado_normal))
                    "ADVERTENCIA" -> tvEstadoAula.setBackgroundColor(ContextCompat.getColor(this, R.color.estado_advertencia))
                    "ALTO" -> tvEstadoAula.setBackgroundColor(ContextCompat.getColor(this, R.color.estado_alto))
                    else -> tvEstadoAula.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
                }

                tvVentilador.text = getString(R.string.lbl_ventilador, estado.ventilador)
                tvVelocidad.text = getString(R.string.lbl_velocidad, "${estado.velocidad} %")
                tvModo.text = getString(R.string.lbl_modo, estado.modo)

                mostrarEstadoConectado()

                // Gestión de incidentes en Room
                procesarIncidentes(estado.estado)
            }
            is Resultado.Error -> {
                mostrarEstadoSinConexion(obtenerTextoError(resultado.error))
            }
        }
    }

    private fun procesarIncidentes(nuevoEstadoStr: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.incidenteDao()
            val incidenteAbierto = dao.obtenerIncidenteAbierto()

            val estadoUpper = nuevoEstadoStr.uppercase()

            if (estadoUpper == "NORMAL") {
                if (incidenteAbierto != null) {
                    val tipoFinalCalculado = if (incidenteAbierto.llegoAAlto) "ALTO" else "ADVERTENCIA"
                    val incidenteCerrado = incidenteAbierto.copy(
                        fechaHoraFin = System.currentTimeMillis(),
                        tipoFinal = tipoFinalCalculado
                    )
                    dao.actualizar(incidenteCerrado)
                }
            } else if (estadoUpper == "ADVERTENCIA" || estadoUpper == "ALTO") {
                if (incidenteAbierto == null) {
                    val nuevoIncidente = Incidente(
                        fechaHoraInicio = System.currentTimeMillis(),
                        fechaHoraFin = null,
                        llegoAAlto = (estadoUpper == "ALTO"),
                        tipoFinal = null
                    )
                    dao.insertar(nuevoIncidente)
                } else {
                    if (estadoUpper == "ALTO" && !incidenteAbierto.llegoAAlto) {
                        val incidenteActualizado = incidenteAbierto.copy(llegoAAlto = true)
                        dao.actualizar(incidenteActualizado)
                    }
                }
            }
        }
    }

    private fun mostrarEstadoConectado() {
        if (configStore.modoSimulacion) {
            cardConexion.setCardBackgroundColor(ContextCompat.getColor(this, R.color.conexion_simulacion))
            tvIndicadorConexion.text = getString(R.string.estado_modo_simulacion)
            tvIndicadorConexion.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvDetalleConexion.visibility = View.GONE
        } else {
            cardConexion.setCardBackgroundColor(ContextCompat.getColor(this, R.color.conexion_ok))
            tvIndicadorConexion.text = getString(R.string.estado_conectado)
            tvIndicadorConexion.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvDetalleConexion.visibility = View.GONE
        }
    }

    private fun mostrarEstadoSinConexion(detalleError: String) {
        if (configStore.modoSimulacion) {
            mostrarEstadoConectado()
            return
        }
        cardConexion.setCardBackgroundColor(ContextCompat.getColor(this, R.color.conexion_error))
        tvIndicadorConexion.text = getString(R.string.estado_sin_conexion)
        tvIndicadorConexion.setTextColor(ContextCompat.getColor(this, R.color.white))
        tvDetalleConexion.visibility = View.VISIBLE
        tvDetalleConexion.text = detalleError
        tvDetalleConexion.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun mostrarEstadoSinIp() {
        cardConexion.setCardBackgroundColor(ContextCompat.getColor(this, R.color.conexion_error))
        tvIndicadorConexion.text = getString(R.string.estado_sin_conexion)
        tvIndicadorConexion.setTextColor(ContextCompat.getColor(this, R.color.white))
        tvDetalleConexion.visibility = View.VISIBLE
        tvDetalleConexion.text = getString(R.string.estado_sin_ip)
        tvDetalleConexion.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun activarModoAutomatico() {
        btnAutomatico.isEnabled = false
        lifecycleScope.launch {
            val repository = RepositoryProvider.obtenerRepositorio(this@MainActivity)
            val resultado = repository.activarAutomatico()
            btnAutomatico.isEnabled = true

            when (resultado) {
                is Resultado.Exito -> {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.msg_modo_auto_exito),
                        Toast.LENGTH_SHORT
                    ).show()
                    procesarResultado(resultado)
                }
                is Resultado.Error -> {
                    val msgError = obtenerTextoError(resultado.error)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.msg_modo_auto_error, msgError),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun mostrarDialogoConfiguracion() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_config, null)
        val edtDialogIp = dialogView.findViewById<EditText>(R.id.edtDialogIp)
        val swDialogSimulacion = dialogView.findViewById<MaterialSwitch>(R.id.swDialogSimulacion)
        val btnDialogProbarConexion = dialogView.findViewById<Button>(R.id.btnDialogProbarConexion)
        val tvDialogResultadoProbar = dialogView.findViewById<TextView>(R.id.tvDialogResultadoProbar)

        edtDialogIp.setText(configStore.ipArduino)
        swDialogSimulacion.isChecked = configStore.modoSimulacion

        btnDialogProbarConexion.setOnClickListener {
            val ipIngresada = edtDialogIp.text.toString().trim()
            if (!esIpValida(ipIngresada)) {
                tvDialogResultadoProbar.text = getString(R.string.ip_invalida)
                tvDialogResultadoProbar.setTextColor(ContextCompat.getColor(this, R.color.conexion_error))
                return@setOnClickListener
            }

            tvDialogResultadoProbar.text = getString(R.string.probando_conexion, ipIngresada)
            tvDialogResultadoProbar.setTextColor(ContextCompat.getColor(this, R.color.black))
            btnDialogProbarConexion.isEnabled = false

            lifecycleScope.launch {
                val repoPrueba = ArduinoHttpRepository(ipIngresada)
                val res = repoPrueba.obtenerEstado()
                btnDialogProbarConexion.isEnabled = true

                when (res) {
                    is Resultado.Exito -> {
                        tvDialogResultadoProbar.text = getString(R.string.conexion_probar_exito, res.datos.hum)
                        tvDialogResultadoProbar.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.conexion_ok))
                    }
                    is Resultado.Error -> {
                        tvDialogResultadoProbar.text = obtenerTextoError(res.error)
                        tvDialogResultadoProbar.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.conexion_error))
                    }
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_config)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar) { dialog, _ ->
                val nuevaIp = edtDialogIp.text.toString().trim()
                val nuevoModoSimulacion = swDialogSimulacion.isChecked

                if (!nuevoModoSimulacion && !esIpValida(nuevaIp)) {
                    Toast.makeText(this, getString(R.string.ip_invalida), Toast.LENGTH_LONG).show()
                } else {
                    configStore.ipArduino = nuevaIp
                    configStore.modoSimulacion = nuevoModoSimulacion

                    if (!nuevoModoSimulacion) {
                        redWifi.activar()
                    } else {
                        redWifi.liberar()
                    }
                    consultarEstado()
                    dialog.dismiss()
                }
            }
            .setNegativeButton(R.string.btn_cancelar) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun mostrarDialogoExplicativoPermiso() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permiso_denegado_titulo)
            .setMessage(R.string.permiso_red_local_requerido)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
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

    private fun esIpValida(ip: String): Boolean {
        if (ip.isBlank()) return false
        val regex = Regex("""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""")
        return regex.matches(ip.trim())
    }
}
