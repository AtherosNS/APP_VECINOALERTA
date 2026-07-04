package com.upn.app_vecinoalerta.ui.financiero

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
import com.upn.app_vecinoalerta.data.local.entities.CargoFinancieroEntity
import com.upn.app_vecinoalerta.databinding.FragmentAlertaDeudasBinding
import com.upn.app_vecinoalerta.utils.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlertaDeudasFragment : Fragment() {

    private var _binding: FragmentAlertaDeudasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinancieroViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertaDeudasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idUsuario = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)

        binding.rvDeudasHeredadas.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observarDeudaTotalPropietario(idUsuario).collect { deudas ->
                    val heredadas = deudas.filter { it.idUsuario == null }
                    binding.rvDeudasHeredadas.adapter = DeudasHeredadasAdapter(heredadas)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class DeudasHeredadasAdapter(
        private val list: List<CargoFinancieroEntity>
    ) : RecyclerView.Adapter<DeudasHeredadasAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvConcepto: TextView = view.findViewById(android.R.id.text1)
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
                setTextColor(android.graphics.Color.RED)
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
            holder.tvConcepto.text = "⚠️ DEUDA HEREDADA: ${item.concepto}"
            holder.tvDetalle.text = "Mes: ${item.mes}/${item.anio} - Monto: ${CurrencyFormatter.formatear(item.monto)}"
        }

        override fun getItemCount() = list.size
    }
}
