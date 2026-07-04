package com.upn.app_vecinoalerta.ui.mural

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
import androidx.navigation.fragment.findNavController
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.AvisoEntity
import com.upn.app_vecinoalerta.databinding.FragmentMuralBinding
import com.upn.app_vecinoalerta.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * RF-06 / RF-07: Mural de avisos oficiales en feed cronológico descendente.
 */
@AndroidEntryPoint
class MuralFragment : Fragment() {

    private var _binding: FragmentMuralBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MuralViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMuralBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rol = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(requireContext(), "rol", "") ?: ""

        // Saludo dinámico
        val nombreCompleto = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(requireContext(), "nombre", "Vecino") ?: "Vecino"
        val primerNombre = nombreCompleto.split(" ").firstOrNull() ?: "Vecino"
        binding.tvGreeting.text = "Hola, $primerNombre 👋"


        // Solo el Admin ve el botón para publicar
        binding.fabNuevoAviso.visibility =
            if (rol == "ADMINISTRADOR") View.VISIBLE else View.GONE

        binding.fabNuevoAviso.setOnClickListener {
            NuevoAvisoDialog().show(childFragmentManager, "nuevo_aviso")
        }

        binding.tvReglamento.setOnClickListener {
            findNavController().navigate(R.id.dest_reglamento)
        }

        binding.tvReportarIncidencia.setOnClickListener {
            findNavController().navigate(R.id.dest_nueva_incidencia)
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

        binding.rvAvisos.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.avisos.collect { lista ->
                    binding.tvSinAvisos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvAvisos.adapter = AvisoAdapter(lista)
                }
            }
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

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private class AvisoAdapter(
        private val list: List<AvisoEntity>
    ) : RecyclerView.Adapter<AvisoAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitulo: TextView = view.findViewById(android.R.id.text1)
            val tvContenido: TextView = view.findViewById(android.R.id.text2)
            val tvFecha: TextView = TextView(view.context).apply {
                textSize = 12f
                setTextColor(android.graphics.Color.DKGRAY)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 6, 0, 6)
                }
                setPadding(16, 12, 16, 12)
                setBackgroundResource(com.upn.app_vecinoalerta.R.drawable.bg_card_white)
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.parseColor("#1E293B"))
                textSize = 17f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val t2 = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.parseColor("#4B5563"))
                textSize = 14f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 6 }
            }
            layout.addView(t1)
            layout.addView(t2)
            
            val holder = ViewHolder(layout)
            holder.tvFecha.layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 10 }
            holder.tvFecha.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
            layout.addView(holder.tvFecha)
            
            return holder
        }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvTitulo.text = item.titulo
            holder.tvContenido.text = item.contenido
            holder.tvFecha.text = DateUtils.epochToFechaHora(item.createdAt)
        }

        override fun getItemCount() = list.size
    }
}