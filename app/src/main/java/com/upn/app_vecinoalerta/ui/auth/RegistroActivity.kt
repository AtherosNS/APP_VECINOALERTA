package com.upn.app_vecinoalerta.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.InmuebleEntity
import com.upn.app_vecinoalerta.databinding.ActivityRegistroBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * RF-01: formulario de auto-registro con campos dinámicos según rol.
 *  - PROPIETARIO → campo abierto para dirección.
 *  - RESIDENTE   → Spinner con inmuebles existentes del sistema.
 */
@AndroidEntryPoint
class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()

    private var inmuebles: List<InmuebleEntity> = emptyList()
    private var idInmuebleSeleccionado: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarRolSelector()
        configurarBotonRegistro()
        configurarAutoUsername()
        
        binding.tvVolverLogin.setOnClickListener {
            finish()
        }
        
        observarEstados()
    }

    /** RF-01: adapta el formulario según el rol seleccionado. */
    private fun configurarRolSelector() {
        binding.rgRol.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbPropietario.id -> {
                    binding.layoutDireccionLibre.visibility = View.VISIBLE
                    binding.layoutSpinnerInmueble.visibility = View.GONE
                    idInmuebleSeleccionado = null
                }
                binding.rbResidente.id -> {
                    binding.layoutDireccionLibre.visibility = View.GONE
                    binding.layoutSpinnerInmueble.visibility = View.VISIBLE
                    cargarSpinner()
                }
            }
        }
    }

    /** RF-01: carga el Spinner con las direcciones existentes. */
    private fun cargarSpinner() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inmuebles.collect { lista ->
                    inmuebles = lista
                    val nombres = lista.map { it.direccion }
                    val adapter = ArrayAdapter(
                        this@RegistroActivity,
                        R.layout.spinner_item,
                        nombres
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
                    binding.spinnerInmueble.adapter = adapter
                    binding.spinnerInmueble.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                            idInmuebleSeleccionado = inmuebles.getOrNull(pos)?.idInmueble
                        }
                        override fun onNothingSelected(p: AdapterView<*>?) { idInmuebleSeleccionado = null }
                    }
                }
            }
        }
    }

    private fun configurarBotonRegistro() {
        binding.btnRegistrar.setOnClickListener {
            val rol = if (binding.rbPropietario.isChecked) "PROPIETARIO" else "RESIDENTE"
            val direccion = if (rol == "PROPIETARIO") binding.etDireccion.text.toString() else null
            viewModel.registrar(
                nombre          = binding.etNombre.text.toString(),
                apellido        = binding.etApellido.text.toString(),
                dni             = binding.etDni.text.toString(),
                correo          = binding.etCorreo.text.toString(),
                usuario         = binding.etUsuario.text.toString(),
                password        = binding.etPassword.text.toString(),
                confirmPassword = binding.etConfirmPassword.text.toString(),
                rol             = rol,
                idInmueble      = idInmuebleSeleccionado,
                direccion       = direccion
            )
        }
    }

    private fun observarEstados() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RegistroUiState.Idle      -> setLoading(false)
                        is RegistroUiState.Cargando  -> setLoading(true)
                        is RegistroUiState.Error     -> {
                            setLoading(false)
                            Toast.makeText(this@RegistroActivity, state.mensaje, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        is RegistroUiState.Exitoso   -> {
                            setLoading(false)
                            // RF-02: informar que la cuenta está pendiente de aprobación
                            Toast.makeText(
                                this@RegistroActivity,
                                "Registro exitoso. Tu cuenta está pendiente de aprobación por el administrador.",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegistrar.isEnabled = !loading
    }

    private fun configurarAutoUsername() {
        binding.etUsuario.isEnabled = false
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val nombre = binding.etNombre.text.toString()
                val apellido = binding.etApellido.text.toString()
                val usernameGenerado = generarUsername(nombre, apellido)
                binding.etUsuario.setText(usernameGenerado)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.etNombre.addTextChangedListener(textWatcher)
        binding.etApellido.addTextChangedListener(textWatcher)
    }

    private fun generarUsername(nombre: String, apellidos: String): String {
        val firstLetterNombre = nombre.trim().firstOrNull()?.lowercase() ?: ""
        val apellidosClean = apellidos.trim().replace("\\s+".toRegex(), " ")
        if (apellidosClean.isEmpty()) return firstLetterNombre
        
        val partes = apellidosClean.split(" ")
        val apellidoPaterno = partes.firstOrNull()?.lowercase() ?: ""
        val firstLetterMaterno = if (partes.size > 1) {
            partes[1].firstOrNull()?.lowercase() ?: ""
        } else {
            ""
        }
        
        return normalizarTexto(firstLetterNombre + apellidoPaterno + firstLetterMaterno)
    }

    private fun normalizarTexto(texto: String): String {
        val normalized = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("ñ", "n")
            .replace("Ñ", "n")
    }
}