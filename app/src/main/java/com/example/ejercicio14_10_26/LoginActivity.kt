package com.example.ejercicio14_10_26

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {

    // Credenciales fijas para validación básica
    companion object {
        const val USUARIO_CORRECTO = "admin"
        const val PASSWORD_CORRECTA = "1234"
    }

    private lateinit var edtUsuario: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnIngresar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        edtUsuario = findViewById(R.id.edtUsuario)
        edtPassword = findViewById(R.id.edtPassword)
        btnIngresar = findViewById(R.id.btnIngresar)

        btnIngresar.setOnClickListener {
            val usuarioInput = edtUsuario.text.toString().trim()
            val passwordInput = edtPassword.text.toString().trim()

            if (usuarioInput == USUARIO_CORRECTO && passwordInput == PASSWORD_CORRECTA) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, getString(R.string.msg_error_login), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
