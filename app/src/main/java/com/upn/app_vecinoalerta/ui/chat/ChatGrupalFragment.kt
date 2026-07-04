package com.upn.app_vecinoalerta.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.MensajeGrupalEntity
import com.upn.app_vecinoalerta.databinding.FragmentChatGrupalBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * RF-11: Chat grupal del condominio.
 * El nombre del emisor se muestra desde [nombreEmisorEstatico],
 * nunca desde la tabla usuarios — preserva identidad de inactivos.
 */
@AndroidEntryPoint
class ChatGrupalFragment : Fragment() {

    private var _binding: FragmentChatGrupalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatGrupalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idUsuario     = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)
        val nombreCompleto = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(requireContext(), "nombre", "Usuario") ?: "Usuario"
        val rol            = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(requireContext(), "rol", "RESIDENTE") ?: "RESIDENTE"

        viewModel.inicializarSincronizacion(idUsuario)

        binding.rvMensajes.layoutManager =
            LinearLayoutManager(requireContext()).apply { stackFromEnd = true }

        binding.btnEnviar.setOnClickListener {
            val texto = binding.etMensaje.text.toString()
            if (texto.isNotBlank()) {
                viewModel.enviarMensajeGrupal(idUsuario, nombreCompleto, rol, texto)
                binding.etMensaje.setText("")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mensajesGrupales.collect { mensajes ->
                    binding.rvMensajes.adapter = MensajeAdapter(mensajes, idUsuario)
                    binding.rvMensajes.scrollToPosition(mensajes.size - 1)
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private class MensajeAdapter(
        private val list: List<com.upn.app_vecinoalerta.data.local.entities.MensajeGrupalEntity>,
        private val currentUserId: Int
    ) : RecyclerView.Adapter<MensajeAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTexto: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val density = parent.context.resources.displayMetrics.density
            val pad16 = (16 * density).toInt()
            val pad12 = (12 * density).toInt()
            val pad8 = (8 * density).toInt()
            val pad24 = (24 * density).toInt()

            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(pad24, pad8, pad24, pad8)
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                textSize = 15f
                setPadding(pad16, pad12, pad16, pad12)
                // Max width: 75% of screen
                val displayMetrics = parent.context.resources.displayMetrics
                maxWidth = (displayMetrics.widthPixels * 0.75).toInt()
            }
            layout.addView(t1)
            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvTexto.text = "${item.nombreEmisorEstatico}: ${item.contenido}"
            val layout = holder.itemView as android.widget.LinearLayout
            if (item.idUsuario == currentUserId) {
                layout.gravity = android.view.Gravity.END
                holder.tvTexto.setTextColor(android.graphics.Color.WHITE)
                holder.tvTexto.setBackgroundResource(R.drawable.bg_bubble_sent)
            } else {
                layout.gravity = android.view.Gravity.START
                holder.tvTexto.setTextColor(android.graphics.Color.parseColor("#1F2937"))
                holder.tvTexto.setBackgroundResource(R.drawable.bg_bubble_received)
            }
        }

        override fun getItemCount() = list.size
    }
}