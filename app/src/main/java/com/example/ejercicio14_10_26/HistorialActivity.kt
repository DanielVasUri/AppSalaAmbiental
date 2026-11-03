package com.example.ejercicio14_10_26

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ejercicio14_10_26.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistorialActivity : AppCompatActivity() {

    private lateinit var btnBackHistorial: ImageButton
    private lateinit var tvHistorialVacio: TextView
    private lateinit var rvHistorial: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historial)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.historial_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnBackHistorial = findViewById(R.id.btnBackHistorial)
        tvHistorialVacio = findViewById(R.id.tvHistorialVacio)
        rvHistorial = findViewById(R.id.rvHistorial)

        btnBackHistorial.setOnClickListener { finish() }

        rvHistorial.layoutManager = LinearLayoutManager(this)

        cargarHistorial()
    }

    private fun cargarHistorial() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val incidentes = db.incidenteDao().obtenerTodos()

            withContext(Dispatchers.Main) {
                if (incidentes.isEmpty()) {
                    tvHistorialVacio.visibility = View.VISIBLE
                    rvHistorial.visibility = View.GONE
                } else {
                    tvHistorialVacio.visibility = View.GONE
                    rvHistorial.visibility = View.VISIBLE
                    rvHistorial.adapter = HistorialAdapter(incidentes)
                }
            }
        }
    }
}
