package com.upn.app_vecinoalerta.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.upn.app_vecinoalerta.databinding.ActivitySplashBinding
import com.upn.app_vecinoalerta.ui.admin.AdminDashboardActivity
import com.upn.app_vecinoalerta.ui.main.MainDashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Esperar 2 segundos y redirigir según el estado de la sesión
        lifecycleScope.launch {
            delay(2000)

            val idUsuario = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(this@SplashActivity, "id_usuario")
            val rol = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(this@SplashActivity, "rol")

            val intent = if (idUsuario != -1 && rol != null) {
                // Ya ha iniciado sesión, ir directo al dashboard
                val destino = if (rol == "ADMINISTRADOR") {
                    AdminDashboardActivity::class.java
                } else {
                    MainDashboardActivity::class.java
                }
                Intent(this@SplashActivity, destino)
            } else {
                // No ha iniciado sesión, ir a Bienvenida
                Intent(this@SplashActivity, BienvenidaActivity::class.java)
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
