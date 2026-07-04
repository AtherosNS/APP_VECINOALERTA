package com.upn.app_vecinoalerta.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.net.Uri
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.IncidenciaEntity
import com.upn.app_vecinoalerta.databinding.FragmentGestionIncidenciasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class GestionIndicidenciasFragment : Fragment() {

    private var _binding: FragmentGestionIncidenciasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestionIncidenciasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idAdmin = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)

        binding.rvIncidencias.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.todasIncidencias.collect { lista ->
                    binding.tvSinIncidencias.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvIncidencias.adapter = IncidenciasAdapter(lista) { incidencia, nuevoEstado ->
                        try {
                            viewModel.actualizarEstadoIncidencia(incidencia.idIncidencia, nuevoEstado, idAdmin)
                            Toast.makeText(requireContext(), "Estado actualizado a $nuevoEstado", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error al actualizar incidencia", Toast.LENGTH_SHORT).show()
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

    private class IncidenciasAdapter(
        private val list: List<IncidenciaEntity>,
        private val onAccion: (IncidenciaEntity, String) -> Unit
    ) : RecyclerView.Adapter<IncidenciasAdapter.ViewHolder>() {

        class ViewHolder(view: View, val ivFoto: android.widget.ImageView) : RecyclerView.ViewHolder(view) {
            val tvCategoria: TextView = view.findViewById(android.R.id.text1)
            val tvDetalle: TextView = view.findViewById(android.R.id.text2)
            val btnProceso: Button = com.google.android.material.button.MaterialButton(view.context).apply {
                text = "En Proceso"
                setBackgroundColor(android.graphics.Color.parseColor("#D97706"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 11f
                minimumHeight = 0
                minimumWidth = 0
                setPadding(16, 8, 16, 8)
                (this as? com.google.android.material.button.MaterialButton)?.cornerRadius = 12
            }
            val btnResuelto: Button = com.google.android.material.button.MaterialButton(view.context).apply {
                text = "Resolver"
                setBackgroundColor(android.graphics.Color.parseColor("#10B981"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 11f
                minimumHeight = 0
                minimumWidth = 0
                setPadding(16, 8, 16, 8)
                (this as? com.google.android.material.button.MaterialButton)?.cornerRadius = 12
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val density = parent.context.resources.displayMetrics.density
            val margin12 = (12 * density).toInt()
            val padding16 = (16 * density).toInt()

            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, margin12, 0, margin12)
                }
                setPadding(padding16, padding16, padding16, padding16)
                setBackgroundResource(R.drawable.bg_card_white)
            }

            val textLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.parseColor("#1F2937"))
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val t2 = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.parseColor("#6B7280"))
                textSize = 13f
            }
            textLayout.addView(t1)
            textLayout.addView(t2)

            val ivFoto = android.widget.ImageView(parent.context).apply {
                val dp150 = (150 * density).toInt()
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp150
                ).also {
                    it.topMargin = 12
                }
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                visibility = View.GONE
            }
            textLayout.addView(ivFoto)
            layout.addView(textLayout)

            val holder = ViewHolder(layout, ivFoto)

            val buttonsLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.END
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (12 * density).toInt()
                }
            }

            val btnParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (34 * density).toInt()
            ).apply {
                setMargins((8 * density).toInt(), 0, 0, 0)
            }
            holder.btnProceso.layoutParams = btnParams
            holder.btnResuelto.layoutParams = btnParams

            buttonsLayout.addView(holder.btnProceso)
            buttonsLayout.addView(holder.btnResuelto)
            layout.addView(buttonsLayout)
            
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvCategoria.text = "[${item.categoria}] Estado: ${item.estado}"
            holder.tvDetalle.text = item.descripcion

            if (!item.fotoPath.isNullOrEmpty()) {
                val file = File(item.fotoPath)
                if (file.exists()) {
                    holder.ivFoto.setImageURI(Uri.fromFile(file))
                    holder.ivFoto.visibility = View.VISIBLE
                } else {
                    holder.ivFoto.visibility = View.GONE
                }
            } else {
                holder.ivFoto.visibility = View.GONE
            }

            holder.btnProceso.setOnClickListener { onAccion(item, "EN_PROCESO") }
            holder.btnResuelto.setOnClickListener { onAccion(item, "RESUELTA") }
        }

        override fun getItemCount() = list.size
    }
}
