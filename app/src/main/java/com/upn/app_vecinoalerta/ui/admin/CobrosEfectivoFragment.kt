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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.CargoFinancieroEntity
import com.upn.app_vecinoalerta.databinding.FragmentCobrosEfectivoBinding
import com.upn.app_vecinoalerta.utils.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CobrosEfectivoFragment : Fragment() {

    private var _binding: FragmentCobrosEfectivoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCobrosEfectivoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idAdmin = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)

        binding.rvCargos.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cargosPendientes.collect { cargos ->
                    binding.tvSinCargos.visibility = if (cargos.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvCargos.adapter = CobrosAdapter(cargos) { cargo ->
                        viewModel.cobrarEfectivo(cargo.idCargo, idAdmin, cargo.monto)
                        Toast.makeText(requireContext(), "Cobro registrado en efectivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class CobrosAdapter(
        private val list: List<CargoFinancieroEntity>,
        private val onCobrar: (CargoFinancieroEntity) -> Unit
    ) : RecyclerView.Adapter<CobrosAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvConcepto: TextView = view.findViewById(android.R.id.text1)
            val tvMonto: TextView = view.findViewById(android.R.id.text2)
            val btnAccion: Button = com.google.android.material.button.MaterialButton(view.context).apply {
                text = "Cobrar"
                setBackgroundColor(android.graphics.Color.parseColor("#D97706"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
                minimumHeight = 0
                minimumWidth = 0
                setPadding(24, 12, 24, 12)
                (this as? com.google.android.material.button.MaterialButton)?.cornerRadius = 16
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val density = parent.context.resources.displayMetrics.density
            val margin12 = (12 * density).toInt()
            val padding16 = (16 * density).toInt()

            val layout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
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
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
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
            layout.addView(textLayout)
            
            val holder = ViewHolder(layout)
            
            val btnParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (36 * density).toInt()
            ).apply {
                marginStart = (12 * density).toInt()
            }
            holder.btnAccion.layoutParams = btnParams
            
            layout.addView(holder.btnAccion)
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvConcepto.text = item.concepto
            holder.tvMonto.text = "Mes: ${item.mes}/${item.anio} - Monto: ${CurrencyFormatter.formatear(item.monto)}"
            holder.btnAccion.setOnClickListener { onCobrar(item) }
        }

        override fun getItemCount() = list.size
    }
}
