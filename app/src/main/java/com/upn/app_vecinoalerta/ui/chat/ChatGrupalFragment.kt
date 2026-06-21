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

        val sesion        = requireActivity().getSharedPreferences("sesion", 0)
        val idUsuario     = sesion.getInt("id_usuario", -1)
        val nombreCompleto = sesion.getString("nombre", "Usuario") ?: "Usuario"
        val rol            = sesion.getString("rol", "RESIDENTE") ?: "RESIDENTE"

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
        private val list: List<MensajeGrupalEntity>,
        private val currentUserId: Int
    ) : RecyclerView.Adapter<MensajeAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTexto: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 8, 32, 8)
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.WHITE)
                textSize = 15f
                setPadding(16, 12, 16, 12)
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
                holder.tvTexto.setBackgroundResource(android.R.color.holo_blue_dark)
            } else {
                layout.gravity = android.view.Gravity.START
                holder.tvTexto.setBackgroundResource(android.R.color.darker_gray)
            }
        }

        override fun getItemCount() = list.size
    }
}