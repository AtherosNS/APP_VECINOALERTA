package com.upn.app_vecinoalerta.ui.financiero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.data.local.entities.CargoFinancieroEntity
import com.upn.app_vecinoalerta.databinding.FragmentEstadoCuentaBinding
import com.upn.app_vecinoalerta.utils.CurrencyFormatter
import com.upn.app_vecinoalerta.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * RF-15: El morador visualiza sus deudas y puede iniciar el pago digital.
 * RF-18: El PROPIETARIO ve también las deudas heredadas de sus inmuebles.
 */
@AndroidEntryPoint
class EstadoCuentaFragment : Fragment() {

    private var _binding: FragmentEstadoCuentaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinancieroViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEstadoCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sesion    = requireActivity().getSharedPreferences("sesion", 0)
        val idUsuario = sesion.getInt("id_usuario", -1)
        val rol       = sesion.getString("rol", "") ?: ""

        binding.rvCargos.layoutManager = LinearLayoutManager(requireContext())

        // Inicializar los StateFlows del ViewModel con el ID del usuario actual
        if (rol == "PROPIETARIO") {
            viewModel.inicializarPropietario(idUsuario)
        } else {
            viewModel.inicializarUsuario(idUsuario)
        }

        // Observar el StateFlow correspondiente — Room emite en tiempo real
        val stateFlowCargos = if (rol == "PROPIETARIO") {
            viewModel.cargosDePropietario
        } else {
            viewModel.misCargos
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateFlowCargos.collect { cargos ->
                    binding.tvSinDeudas.visibility = if (cargos.isEmpty()) View.VISIBLE else View.GONE

                    val totalPendiente = cargos
                        .filter { it.estadoPago == "PENDIENTE" }
                        .sumOf { it.monto }

                    binding.tvTotalDeuda.text = "Total pendiente: ${CurrencyFormatter.formatear(totalPendiente)}"

                    binding.rvCargos.adapter = CargoAdapter(cargos) { cargo ->
                        val bundle = Bundle().apply {
                            putInt("idCargo", cargo.idCargo)
                            putString("monto", cargo.monto.toString())
                        }
                        findNavController().navigate(R.id.action_estadoCuenta_to_pasarelaPagos, bundle)
                    }
                }
            }
        }

        // RF-18: si es PROPIETARIO, mostrar sección de deudas heredadas
        if (rol == "PROPIETARIO") {
            binding.cardDeudasHeredadas.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.deudaTotalPropietario.collect { deudas ->
                        val heredadas = deudas.filter { it.idUsuario == null }
                        if (heredadas.isNotEmpty()) {
                            val totalHeredado = heredadas.sumOf { it.monto }
                            binding.tvAlertaDeudaHeredada.text =
                                "⚠️ Tu(s) inmueble(s) arrastra(n) deudas heredadas de ex-inquilinos: " +
                                        CurrencyFormatter.formatear(totalHeredado)
                            binding.tvAlertaDeudaHeredada.visibility = View.VISIBLE
                        } else {
                            binding.tvAlertaDeudaHeredada.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }


    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private class CargoAdapter(
        private val list: List<CargoFinancieroEntity>,
        private val onPagar: (CargoFinancieroEntity) -> Unit
    ) : RecyclerView.Adapter<CargoAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvConcepto: TextView = view.findViewById(android.R.id.text1)
            val tvDetalle: TextView = view.findViewById(android.R.id.text2)
            val btnPagar: Button = Button(view.context).apply {
                text = "Pagar"
                setBackgroundColor(android.graphics.Color.parseColor("#D97706"))
                setTextColor(android.graphics.Color.WHITE)
            }
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
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val t2 = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.GRAY)
                textSize = 14f
            }
            textLayout.addView(t1)
            textLayout.addView(t2)
            layout.addView(textLayout)

            val holder = ViewHolder(layout)
            holder.btnPagar.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layout.addView(holder.btnPagar)
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvConcepto.text = item.concepto
            val statusText = if (item.estadoPago == "PAGADO") "PAGADO" else "PENDIENTE"
            holder.tvDetalle.text = "Mes: ${item.mes}/${item.anio} - Monto: ${CurrencyFormatter.formatear(item.monto)}\nEstado: $statusText"
            
            if (item.estadoPago == "PAGADO") {
                holder.btnPagar.visibility = View.GONE
            } else {
                holder.btnPagar.visibility = View.VISIBLE
                holder.btnPagar.setOnClickListener { onPagar(item) }
            }
        }

        override fun getItemCount() = list.size
    }
}