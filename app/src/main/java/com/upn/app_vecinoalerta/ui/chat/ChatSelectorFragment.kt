package com.upn.app_vecinoalerta.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.UsuarioEntity
import com.upn.app_vecinoalerta.data.repository.UsuarioRepository
import com.upn.app_vecinoalerta.databinding.FragmentChatSelectorBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pantalla de selección de canal de chat.
 * - Residentes/Propietarios: Ven "Chat Vecinal" y "Contactar Administrador".
 * - Administradores: Ven "Chat Vecinal" y la "Bandeja de Entrada" con el listado de vecinos activos para iniciar/responder mensajes directos.
 */
@AndroidEntryPoint
class ChatSelectorFragment : Fragment() {

    private var _binding: FragmentChatSelectorBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var usuarioRepo: UsuarioRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rol    = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(requireContext(), "rol", "RESIDENTE") ?: "RESIDENTE"

        if (rol == "ADMINISTRADOR") {
            // Para administradores: ocultar card de chatear consigo mismo y mostrar bandeja de entrada de vecinos
            binding.cardChatAdmin.visibility = View.GONE
            binding.tvAdminTitle.visibility = View.VISIBLE
            binding.rvActiveChats.visibility = View.VISIBLE
            
            cargarBandejaVecinos()
        } else {
            // Para vecinos comunes: mostrar chat grupal y contacto de administrador
            binding.cardChatAdmin.visibility = View.VISIBLE
            binding.tvAdminTitle.visibility = View.GONE
            binding.rvActiveChats.visibility = View.GONE
        }

        // Card 1 → Chat Vecinal Grupal
        binding.cardChatGrupal.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_chat_to_grupal)
            } catch (e: Exception) {
                findNavController().navigate(R.id.dest_chat_grupal)
            }
        }

        // Card 2 → Chat privado con el admin (dinámico)
        binding.cardChatAdmin.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val admin = usuarioRepo.obtenerAdmin()
                    val adminId = admin?.idUsuario ?: 1
                    val args = Bundle().apply {
                        putInt("id_receptor", adminId)
                    }
                    try {
                        findNavController().navigate(R.id.action_chat_to_privado_admin, args)
                    } catch (e: Exception) {
                        findNavController().navigate(R.id.dest_chat_privado, args)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun cargarBandejaVecinos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Obtener listado de vecinos activos del sistema (excluyendo administradores)
                val vecinos = usuarioRepo.obtenerActivosLista().filter { it.rol != "ADMINISTRADOR" }
                
                binding.rvActiveChats.layoutManager = LinearLayoutManager(requireContext())
                binding.rvActiveChats.adapter = VecinosAdapter(vecinos) { vecino ->
                    val args = Bundle().apply {
                        putInt("id_receptor", vecino.idUsuario)
                    }
                    try {
                        findNavController().navigate(R.id.action_chat_to_privado_admin, args)
                    } catch (e: Exception) {
                        findNavController().navigate(R.id.dest_chat_privado, args)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class VecinosAdapter(
        private val list: List<UsuarioEntity>,
        private val onClick: (UsuarioEntity) -> Unit
    ) : RecyclerView.Adapter<VecinosAdapter.ViewHolder>() {

        class ViewHolder(
            view: View,
            val ivAvatar: ImageView,
            val tvNombre: TextView,
            val tvSub: TextView
        ) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val density = parent.context.resources.displayMetrics.density
            val size40 = (40 * density).toInt()
            val size18 = (18 * density).toInt()
            val margin16 = (16 * density).toInt()
            val margin12 = (12 * density).toInt()
            val margin8 = (8 * density).toInt()

            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, margin8, 0, margin8)
                }
                setPadding(margin16, margin12, margin16, margin12)
                setBackgroundResource(R.drawable.bg_card_white)
            }

            // Avatar
            val iv = ImageView(parent.context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(size40, size40).apply {
                    marginEnd = margin16
                }
                setImageResource(R.drawable.ic_avatar_placeholder)
            }
            layout.addView(iv)

            // Info del usuario
            val infoLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            val t1 = TextView(parent.context).apply {
                setTextColor(android.graphics.Color.parseColor("#1F2937"))
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val t2 = TextView(parent.context).apply {
                setTextColor(android.graphics.Color.parseColor("#6B7280"))
                textSize = 12f
            }
            infoLayout.addView(t1)
            infoLayout.addView(t2)
            layout.addView(infoLayout)

            // Flecha next
            val ivNext = ImageView(parent.context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(size18, size18)
                setImageResource(android.R.drawable.ic_media_next)
                setColorFilter(android.graphics.Color.parseColor("#9CA3AF"))
            }
            layout.addView(ivNext)

            return ViewHolder(layout, iv, t1, t2)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvNombre.text = "${item.nombre} ${item.apellido}"
            
            val rolText = if (item.rol == "PROPIETARIO") "Propietario" else "Morador"
            val blockLotText = if (item.idInmuebleAsignado != null) " - Vivienda" else ""
            holder.tvSub.text = "$rolText$blockLotText (DNI: ${item.dni})"

            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = list.size
    }
}
