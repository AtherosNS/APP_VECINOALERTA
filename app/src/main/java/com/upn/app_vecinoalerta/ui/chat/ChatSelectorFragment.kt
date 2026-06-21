package com.upn.app_vecinoalerta.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.databinding.FragmentChatSelectorBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Pantalla de selección de canal de chat.
 * - Chat Vecinal   → ChatGrupalFragment (todos los vecinos)
 * - Contactar Admin → ChatPrivadoFragment (id_receptor = 1 = admin)
 *
 * En el panel ADMINISTRADOR, "Contactar Admin" muestra el historial
 * de mensajes privados recibidos de los vecinos (el admin elige a quién responder).
 */
@AndroidEntryPoint
class ChatSelectorFragment : Fragment() {

    private var _binding: FragmentChatSelectorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sesion = requireActivity().getSharedPreferences("sesion", 0)
        val rol    = sesion.getString("rol", "RESIDENTE") ?: "RESIDENTE"
        val idUsuario = sesion.getInt("id_usuario", -1)

        // Adaptar el label del card privado según rol
        if (rol == "ADMINISTRADOR") {
            binding.cardChatAdmin.visibility = View.VISIBLE
            // Para el admin: navega al chat grupal donde puede ver todos los hilos
            // (en esta implementación redirige al chat grupal para gestión básica)
        }

        // Card 1 → Chat Vecinal Grupal
        binding.cardChatGrupal.setOnClickListener {
            // Intentar navegar en cualquiera de los tres grafos
            try {
                findNavController().navigate(R.id.action_chat_to_grupal)
            } catch (e: Exception) {
                findNavController().navigate(R.id.dest_chat_grupal)
            }
        }

        // Card 2 → Chat privado con el admin (id=1 por defecto)
        binding.cardChatAdmin.setOnClickListener {
            val args = Bundle().apply {
                // El residente/propietario contacta al admin (id=1)
                // El admin puede contactar a quien quiera; por ahora al residente 0 (placeholder)
                putInt("id_receptor", if (rol == "ADMINISTRADOR") idUsuario else 1)
            }
            try {
                findNavController().navigate(R.id.action_chat_to_privado_admin, args)
            } catch (e: Exception) {
                findNavController().navigate(R.id.dest_chat_privado, args)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
