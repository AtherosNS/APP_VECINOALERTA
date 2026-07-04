package com.upn.app_vecinoalerta.ui.panico

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.ContactoEmergenciaEntity
import com.upn.app_vecinoalerta.databinding.ActivityPanicoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * RF-05: Botón de Pánico.
 * RNF-01: máximo 3 pasos desde el dashboard:
 *   1. Tap en FAB de pánico (dashboard)
 *   2. Se abre esta pantalla con los botones por categoría
 *   3. Tap en el contacto → Intent ACTION_DIAL inmediato
 *
 * RNF-05: funciona 100% offline — solo lee de Room local.
 */
@AndroidEntryPoint
class PanicoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanicoBinding
    private val viewModel: PanicoViewModel by viewModels()

    private val solicitarPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido)
            Toast.makeText(this, "Permiso de llamada necesario para marcar", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanicoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RF-05: botón volver → cierra la pantalla de pánico
        binding.btnVolver.setOnClickListener { finish() }

        // Solicitar permiso CALL_PHONE al abrir la pantalla
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            solicitarPermiso.launch(Manifest.permission.CALL_PHONE)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.contactosPorTipo.collect { grupos ->
                    renderizarContactos(grupos)
                }
            }
        }
    }

    /**
     * Genera dinámicamente los botones de emergencia agrupados por tipo.
     * RF-05: cada botón dispara ACTION_DIAL directo al número.
     */
    private fun renderizarContactos(grupos: Map<String, List<ContactoEmergenciaEntity>>) {
        binding.llContactos.removeAllViews()
        val iconos = mapOf(
            "POLICIA"    to "🚔",
            "BOMBEROS"   to "🚒",
            "SERENAZGO"  to "🛡️",
            "AMBULANCIA" to "🚑",
            "OTRO"       to "📞"
        )
        grupos.forEach { (tipo, contactos) ->
            contactos.forEach { contacto ->
                val icono = iconos[tipo] ?: "📞"
                val btn = MaterialButton(this).apply {
                    text = "$icono  ${contacto.nombre}\n${contacto.telefono}"
                    setOnClickListener { marcar(contacto.telefono) }
                    setPadding(32, 24, 32, 24)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(0, 0, 0, 24) }
                }
                binding.llContactos.addView(btn)
            }
        }
    }

    /** RF-05: abre el marcador telefónico del sistema (paso 3 de 3). */
    private fun marcar(telefono: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono"))
        startActivity(intent)
    }
}