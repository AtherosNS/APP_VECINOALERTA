package com.upn.app_vecinoalerta.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.upn.app_vecinoalerta.data.repository.UsuarioRepository
import com.upn.app_vecinoalerta.databinding.FragmentPerfilBinding
import com.upn.app_vecinoalerta.utils.SecurePrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var repo: UsuarioRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val idUsuario = SecurePrefs.getInt(context, "id_usuario", -1)

        if (idUsuario == -1) {
            Toast.makeText(context, "⚠️ Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        // Cargar y observar información del usuario
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.observarPorId(idUsuario).collect { usuario ->
                    usuario?.let {
                        binding.tvNombrePerfil.text = "${it.nombre} ${it.apellido}"
                        binding.tvCorreoPerfil.text = it.correo
                        binding.tvRolPerfil.text = it.rol
                    }
                }
            }
        }

        // Manejar actualización de contraseña
        binding.btnActualizarPassword.setOnClickListener {
            val act = binding.etPasswordActual.text.toString().trim()
            val nueva = binding.etPasswordNueva.text.toString().trim()
            val conf = binding.etPasswordConfirmar.text.toString().trim()

            // Validaciones
            when {
                act.isEmpty() -> {
                    binding.layoutPasswordActual.error = "Ingresa tu contraseña actual"
                    return@setOnClickListener
                }
                nueva.isEmpty() -> {
                    binding.layoutPasswordActual.error = null
                    binding.layoutPasswordNueva.error = "Ingresa tu nueva contraseña"
                    return@setOnClickListener
                }
                nueva.length < 6 -> {
                    binding.layoutPasswordActual.error = null
                    binding.layoutPasswordNueva.error = "Mínimo 6 caracteres"
                    return@setOnClickListener
                }
                conf.isEmpty() -> {
                    binding.layoutPasswordActual.error = null
                    binding.layoutPasswordNueva.error = null
                    binding.layoutPasswordConfirmar.error = "Confirma tu nueva contraseña"
                    return@setOnClickListener
                }
                nueva != conf -> {
                    binding.layoutPasswordActual.error = null
                    binding.layoutPasswordNueva.error = null
                    binding.layoutPasswordConfirmar.error = "Las contraseñas no coinciden"
                    return@setOnClickListener
                }
                else -> {
                    binding.layoutPasswordActual.error = null
                    binding.layoutPasswordNueva.error = null
                    binding.layoutPasswordConfirmar.error = null

                    viewLifecycleOwner.lifecycleScope.launch {
                        binding.btnActualizarPassword.isEnabled = false
                        val exitoso = repo.cambiarPassword(idUsuario, act, nueva)
                        binding.btnActualizarPassword.isEnabled = true

                        if (exitoso) {
                            Toast.makeText(context, "✅ Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                            binding.etPasswordActual.text?.clear()
                            binding.etPasswordNueva.text?.clear()
                            binding.etPasswordConfirmar.text?.clear()
                        } else {
                            Toast.makeText(context, "❌ Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
