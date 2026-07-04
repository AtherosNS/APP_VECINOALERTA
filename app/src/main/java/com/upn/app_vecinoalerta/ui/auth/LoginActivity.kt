package com.upn.app_vecinoalerta.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.upn.app_vecinoalerta.data.local.entities.UsuarioEntity
import com.upn.app_vecinoalerta.databinding.ActivityLoginBinding
import com.upn.app_vecinoalerta.ui.admin.AdminDashboardActivity
import com.upn.app_vecinoalerta.ui.main.MainDashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import com.upn.app_vecinoalerta.utils.SecurePrefs
import kotlinx.coroutines.launch

/**
 * RF-03: pantalla de inicio de sesión.
 * Redirige al panel correspondiente según el rol del usuario autenticado.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login: si ya hay una sesión activa en SharedPreferences, entrar directo sin pedir login online
        val idUsuario = SecurePrefs.getInt(this, "id_usuario")
        val rol = SecurePrefs.getString(this, "rol")

        if (idUsuario != -1 && rol != null) {
            val destino = if (rol == "ADMINISTRADOR")
                AdminDashboardActivity::class.java
            else
                MainDashboardActivity::class.java

            startActivity(Intent(this, destino).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {

            val identificador = binding.etUsuario.text.toString()
            val password      = binding.etPassword.text.toString()
            viewModel.login(identificador, password)
        }


        binding.tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        binding.tvOlvidePassword.setOnClickListener {
            mostrarDialogoRestablecerPassword()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle      -> setLoading(false)
                        is LoginUiState.Cargando  -> setLoading(true)
                        is LoginUiState.Error     -> {
                            setLoading(false)
                            Toast.makeText(this@LoginActivity, state.mensaje, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        is LoginUiState.Exitoso   -> irAlPanel(state.usuario)
                    }
                }
            }
        }
    }

    /** RF-03: redirige según el rol. */
    private fun irAlPanel(usuario: UsuarioEntity) {
        // Guardamos el id del usuario en sesión (EncryptedSharedPreferences)
        SecurePrefs.guardarSesion(
            context = this,
            idUsuario = usuario.idUsuario,
            rol = usuario.rol,
            nombre = "${usuario.nombre} ${usuario.apellido}"
        )

        val destino = if (usuario.rol == "ADMINISTRADOR")
            AdminDashboardActivity::class.java
        else
            MainDashboardActivity::class.java

        startActivity(Intent(this, destino).also { it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun mostrarDialogoRestablecerPassword() {
        val input = android.widget.EditText(this).apply {
            hint = "correo@ejemplo.com"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val container = android.widget.FrameLayout(this).apply {
            addView(input)
            val paddingPx = (24 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, 0, paddingPx, 0)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Restablecer Contraseña")
            .setMessage("Ingresa tu correo registrado en Firebase:")
            .setView(container)
            .setPositiveButton("Enviar") { _, _ ->
                val email = input.text.toString().trim()
                if (email.contains("@")) {
                    com.google.firebase.auth.FirebaseAuth.getInstance()
                        .sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Se envió el correo de restablecimiento a $email", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Por favor ingresa un correo válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}