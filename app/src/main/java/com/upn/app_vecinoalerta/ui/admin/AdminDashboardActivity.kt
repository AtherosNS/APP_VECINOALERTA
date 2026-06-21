package com.upn.app_vecinoalerta.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.databinding.ActivityDashboardAdminBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dashboard principal para el ADMINISTRADOR.
 * Botón Inicio → siempre regresa a AdminHomeFragment limpiando el back-stack.
 */
@AndroidEntryPoint
class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardAdminBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_admin)

        // setupWithNavController gestiona la sincronización visual del BottomNav
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

        // Re-selección: si ya estás en ese tab, vuelve al inicio del back-stack
        binding.bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.dest_admin_home) {
                // Ya estás en Home, no hacer nada
            } else {
                navController.popBackStack(navController.graph.startDestinationId, false)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()
}