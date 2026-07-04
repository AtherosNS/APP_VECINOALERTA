package com.upn.app_vecinoalerta.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.databinding.FragmentAdminHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Saludo dinámico
        val nombreCompleto = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(requireContext(), "nombre", "Admin") ?: "Admin"
        val primerNombre = nombreCompleto.split(" ").firstOrNull() ?: "Admin"
        binding.tvGreeting.text = "Hola, $primerNombre 👋"

        binding.cardAprobaciones.setOnClickListener {
            findNavController().navigate(R.id.dest_aprobaciones)
        }

        binding.cardCobros.setOnClickListener {
            findNavController().navigate(R.id.dest_cobros)
        }

        binding.cardCrearCargo.setOnClickListener {
            findNavController().navigate(R.id.dest_crear_cargo)
        }

        binding.cardIncidencias.setOnClickListener {
            findNavController().navigate(R.id.dest_gestion_incidencias)
        }

        binding.cardAsambleas.setOnClickListener {
            findNavController().navigate(R.id.dest_asambleas)
        }

        binding.cardEncuestas.setOnClickListener {
            findNavController().navigate(R.id.dest_encuestas)
        }


        binding.ivAvatar.setOnClickListener {
            val dialogView = layoutInflater.inflate(com.upn.app_vecinoalerta.R.layout.dialog_menu_perfil, null)
            val dialog = android.app.AlertDialog.Builder(requireContext(), com.upn.app_vecinoalerta.R.style.Theme_VecinoAlerta_Dialog)
                .setView(dialogView)
                .create()

            dialogView.findViewById<android.view.View>(com.upn.app_vecinoalerta.R.id.optionPerfil).setOnClickListener {
                dialog.dismiss()
                findNavController().navigate(com.upn.app_vecinoalerta.R.id.dest_perfil)
            }

            dialogView.findViewById<android.view.View>(com.upn.app_vecinoalerta.R.id.optionSalir).setOnClickListener {
                dialog.dismiss()
                cerrarSesion()
            }

            dialog.show()
        }
    }

    private fun cerrarSesion() {
        com.upn.app_vecinoalerta.utils.SecurePrefs.limpiarSesion(requireContext())
        val intent = android.content.Intent(requireContext(), com.upn.app_vecinoalerta.ui.auth.LoginActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
