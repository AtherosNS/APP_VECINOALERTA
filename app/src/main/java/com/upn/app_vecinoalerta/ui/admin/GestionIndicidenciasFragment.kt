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

        val sesion = requireActivity().getSharedPreferences("sesion", 0)
        val idAdmin = sesion.getInt("id_usuario", -1)

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
            val btnProceso: Button = Button(view.context).apply { text = "En Proceso" }
            val btnResuelto: Button = Button(view.context).apply { text = "Resolver" }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPaddingRelative(32, 16, 32, 16)
            }
            val textLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f
                )
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
            }
            val t2 = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.GRAY)
                textSize = 14f
            }
            textLayout.addView(t1)
            textLayout.addView(t2)

            val ivFoto = android.widget.ImageView(parent.context).apply {
                val dp150 = (150 * parent.context.resources.displayMetrics.density).toInt()
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

            val buttonsLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
            }
            val holder = ViewHolder(layout, ivFoto)
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
