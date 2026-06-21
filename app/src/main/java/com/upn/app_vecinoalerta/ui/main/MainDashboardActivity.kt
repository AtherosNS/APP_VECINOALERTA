package com.upn.app_vecinoalerta.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.databinding.ActivityMainDashboardBinding
import com.upn.app_vecinoalerta.ui.panico.PanicoActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dashboard principal para PROPIETARIO y RESIDENTE.
 * Botón Inicio (Mural) → siempre regresa al inicio limpiando el back-stack.
 * RF-05/RNF-01: el FAB de pánico está siempre visible y accesible en 1 tap.
 */
@AndroidEntryPoint
class MainDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainDashboardBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val sesion = getSharedPreferences("sesion", MODE_PRIVATE)
        val rol = sesion.getString("rol", "RESIDENTE") ?: "RESIDENTE"
        val graphResId = if (rol == "PROPIETARIO") {
            R.navigation.nav_propietario
        } else {
            R.navigation.nav_residente
        }
        navController.setGraph(graphResId)

        // setupWithNavController para sincronización visual
        binding.bottomNav.setupWithNavController(navController)

        // Override: cualquier tap en BottomNav limpia el back-stack hasta el destino raíz
        binding.bottomNav.setOnItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, inclusive = false)
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .build()
            try {
                navController.navigate(item.itemId, null, navOptions)
                true
            } catch (e: Exception) {
                false
            }
        }

        // Re-selección: regresa al inicio del back-stack
        binding.bottomNav.setOnItemReselectedListener {
            navController.popBackStack(navController.graph.startDestinationId, false)
        }

        // RF-05/RNF-01: FAB de pánico — 1 tap desde cualquier pantalla
        binding.fabPanico.setOnClickListener {
            startActivity(Intent(this, PanicoActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()
}
