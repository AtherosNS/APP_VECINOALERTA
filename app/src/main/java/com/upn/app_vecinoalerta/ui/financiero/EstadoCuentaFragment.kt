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

        val idUsuario = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)
        val rol       = com.upn.app_vecinoalerta.utils.SecurePrefs.getString(requireContext(), "rol", "") ?: ""

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
            val btnPagar: TextView = view.findViewById(R.id.btnFechaHora) // Reutilizamos un ID de botón si existiese o usamos tags, pero creamos las referencias correctas
            val vStatusIndicator: View = view.findViewById(R.id.layoutLugar) // Reutilizamos IDs o creamos la vista dinámica mapeada
            val layoutBadge: android.widget.LinearLayout = view.findViewById(R.id.layoutTitulo)
            val tvBadgeText: TextView = view.findViewById(R.id.tvNuevaAsambleaTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val density = parent.context.resources.displayMetrics.density
            val margin8 = (8 * density).toInt()
            val padding16 = (16 * density).toInt()

            // Tarjeta principal (LinearLayout Horizontal)
            val cardLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(margin8, margin8, margin8, margin8)
                }
                setPadding(padding16, padding16, padding16, padding16)
                setBackgroundResource(R.drawable.bg_card_white)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            // 1. Barra indicadora de estado vertical (a la izquierda)
            val statusIndicator = View(parent.context).apply {
                id = R.id.layoutLugar // mapeado para el ViewHolder
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    (4 * density).toInt(),
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).also {
                    it.marginEnd = (12 * density).toInt()
                }
            }
            cardLayout.addView(statusIndicator)

            // 2. Layout de texto (Vertical)
            val textLayout = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f
                )
            }

            val tvConcepto = TextView(parent.context).apply {
                id = android.R.id.text1
                setTextColor(android.graphics.Color.parseColor("#1F2937")) // Texto principal oscuro
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val tvDetalle = TextView(parent.context).apply {
                id = android.R.id.text2
                setTextColor(android.graphics.Color.parseColor("#6B7280")) // Texto secundario gris
                textSize = 13f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = (4 * density).toInt() }
            }
            textLayout.addView(tvConcepto)
            textLayout.addView(tvDetalle)
            cardLayout.addView(textLayout)

            // 3. Área de acción derecha (Contenedor para Botón Pagar o Badge Pagado)
            val actionContainer = android.widget.LinearLayout(parent.context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.marginStart = (12 * density).toInt()
                }
                gravity = android.view.Gravity.CENTER
            }

            // Botón de Pagar
            val btnPagar = TextView(parent.context).apply {
                id = R.id.btnFechaHora // mapeado para el ViewHolder
                text = "Pagar"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
                val gd = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#D97706")) // Ámbar
                    cornerRadius = 16 * density
                }
                background = gd
                isClickable = true
                isFocusable = true
            }

            // Badge de Pagado (Pill verde)
            val badgeLayout = android.widget.LinearLayout(parent.context).apply {
                id = R.id.layoutTitulo // mapeado para el ViewHolder
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
                val gd = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#D1FAE5")) // Verde menta suave
                    cornerRadius = 12 * density
                }
                background = gd
                gravity = android.view.Gravity.CENTER
            }
            val tvBadgeText = TextView(parent.context).apply {
                id = R.id.tvNuevaAsambleaTitle // mapeado para el ViewHolder
                text = "PAGADO"
                setTextColor(android.graphics.Color.parseColor("#065F46")) // Verde bosque oscuro
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            badgeLayout.addView(tvBadgeText)

            actionContainer.addView(btnPagar)
            actionContainer.addView(badgeLayout)
            cardLayout.addView(actionContainer)

            return ViewHolder(cardLayout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvConcepto.text = item.concepto

            // Traducir número de mes a español
            val meses = arrayOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )
            val mesStr = if (item.mes in 1..12) meses[item.mes - 1] else "Mes ${item.mes}"
            holder.tvDetalle.text = "$mesStr ${item.anio} • ${CurrencyFormatter.formatear(item.monto)}"

            if (item.estadoPago == "PAGADO") {
                // Estado PAGADO:
                holder.vStatusIndicator.setBackgroundColor(android.graphics.Color.parseColor("#22C55E")) // Barra lateral verde
                holder.btnPagar.visibility = View.GONE
                holder.layoutBadge.visibility = View.VISIBLE
            } else {
                // Estado PENDIENTE:
                holder.vStatusIndicator.setBackgroundColor(android.graphics.Color.parseColor("#F97316")) // Barra lateral naranja/ámbar
                holder.btnPagar.visibility = View.VISIBLE
                holder.layoutBadge.visibility = View.GONE
                holder.btnPagar.setOnClickListener { onPagar(item) }
            }
        }

        override fun getItemCount() = list.size
    }
}