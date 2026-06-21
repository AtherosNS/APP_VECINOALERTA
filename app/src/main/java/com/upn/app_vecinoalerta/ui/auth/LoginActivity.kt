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
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val usuario  = binding.etUsuario.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.login(usuario, password)
        }

        binding.tvRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
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
        // Guardamos el id del usuario en sesión (SharedPreferences simple)
        getSharedPreferences("sesion", MODE_PRIVATE).edit()
            .putInt("id_usuario", usuario.idUsuario)
            .putString("rol", usuario.rol)
            .putString("nombre", "${usuario.nombre} ${usuario.apellido}")
            .apply()

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
}