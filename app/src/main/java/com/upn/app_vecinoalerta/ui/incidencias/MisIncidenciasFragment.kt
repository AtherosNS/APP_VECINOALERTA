package com.upn.app_vecinoalerta.ui.incidencias

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
import com.upn.app_vecinoalerta.data.local.entities.IncidenciaEntity
import com.upn.app_vecinoalerta.databinding.FragmentMisIncidenciasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MisIncidenciasFragment : Fragment() {

    private var _binding: FragmentMisIncidenciasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IncidenciaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisIncidenciasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idUsuario = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)

        binding.rvMisIncidencias.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observarMisIncidencias(idUsuario).collect { lista ->
                    binding.tvSinMisIncidencias.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvMisIncidencias.adapter = MisIncidenciasAdapter(lista)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class MisIncidenciasAdapter(
        private val list: List<IncidenciaEntity>
    ) : RecyclerView.Adapter<MisIncidenciasAdapter.ViewHolder>() {

        class ViewHolder(view: View, val ivFoto: android.widget.ImageView) : RecyclerView.ViewHolder(view) {
            val tvCategoria: TextView = view.findViewById(android.R.id.text1)
            val tvDetalle: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 16, 32, 16)
            }
            val t1 = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val t2 = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.LTGRAY)
                textSize = 14f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 8 }
            }
            layout.addView(t1)
            layout.addView(t2)

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
            layout.addView(ivFoto)

            return ViewHolder(layout, ivFoto)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvCategoria.text = "Categoría: ${item.categoria} (Estado: ${item.estado})"
            holder.tvDetalle.text = item.descripcion

            if (!item.fotoPath.isNullOrEmpty()) {
                val file = java.io.File(item.fotoPath)
                if (file.exists()) {
                    holder.ivFoto.setImageURI(android.net.Uri.fromFile(file))
                    holder.ivFoto.visibility = View.VISIBLE
                } else {
                    holder.ivFoto.visibility = View.GONE
                }
            } else {
                holder.ivFoto.visibility = View.GONE
            }
        }

        override fun getItemCount() = list.size
    }
}
