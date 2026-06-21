package com.upn.app_vecinoalerta.ui.asambleas

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
import com.upn.app_vecinoalerta.data.local.entities.AsambleaEntity
import com.upn.app_vecinoalerta.databinding.FragmentAsambleasBinding
import com.upn.app_vecinoalerta.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AsambleasFragment : Fragment() {

    private var _binding: FragmentAsambleasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AsambleaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAsambleasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sesion = requireActivity().getSharedPreferences("sesion", 0)
        val rol = sesion.getString("rol", "") ?: ""

        if (rol == "ADMINISTRADOR") {
            binding.fabNuevaAsamblea.visibility = View.VISIBLE
            binding.fabNuevaAsamblea.setOnClickListener {
                findNavController().navigate(R.id.dest_nueva_asamblea)
            }
        }

        binding.rvAsambleas.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.todas.collect { lista ->
                    binding.tvSinAsambleas.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvAsambleas.adapter = AsambleasAdapter(lista)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class AsambleasAdapter(
        private val list: List<AsambleaEntity>
    ) : RecyclerView.Adapter<AsambleasAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitulo: TextView = view.findViewById(android.R.id.text1)
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
                setTextColor(android.graphics.Color.GRAY)
                textSize = 14f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 8 }
            }
            layout.addView(t1)
            layout.addView(t2)
            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvTitulo.text = item.titulo
            holder.tvDetalle.text = "Fecha: ${DateUtils.epochToFechaHora(item.fechaHora)} | Lugar: ${item.lugar}\n${item.agenda}"
        }

        override fun getItemCount() = list.size
    }
}
