package com.upn.app_vecinoalerta.ui.financiero

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.upn.app_vecinoalerta.R
import com.upn.app_vecinoalerta.databinding.FragmentPasarelaPagosBinding
import com.upn.app_vecinoalerta.utils.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * RF-16: Simulación de pasarela de pagos.
 * Métodos disponibles: VISA, MASTERCARD, YAPE, PLIN.
 * Al confirmar, el cargo queda marcado como PAGADO en Room de forma atómica.
 */
@AndroidEntryPoint
class PasarelaPagosFragment : Fragment() {

    private var _binding: FragmentPasarelaPagosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinancieroViewModel by viewModels()

    private var metodoPagoSeleccionado: String = "YAPE"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasarelaPagosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sesion     = requireActivity().getSharedPreferences("sesion", 0)
        val idUsuario  = sesion.getInt("id_usuario", -1)
        val idCargo    = arguments?.getInt("idCargo", -1) ?: -1
        val monto      = arguments?.getString("monto")?.toDoubleOrNull() ?: 0.0

        binding.tvMonto.text = "Total a pagar: ${CurrencyFormatter.formatear(monto)}"

        // Selección del método de pago
        binding.rgMetodoPago.setOnCheckedChangeListener { _, checkedId ->
            metodoPagoSeleccionado = when (checkedId) {
                binding.rbVisa.id        -> "VISA"
                binding.rbMastercard.id  -> "MASTERCARD"
                binding.rbYape.id        -> "YAPE"
                binding.rbPlin.id        -> "PLIN"
                else                     -> "YAPE"
            }
        }

        binding.btnConfirmarPago.setOnClickListener {
            mostrarSimulacionPago(idCargo, idUsuario, metodoPagoSeleccionado, monto)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pagoState.collect { state ->
                    when (state) {
                        is PagoUiState.Procesando -> {
                            binding.btnConfirmarPago.isEnabled = false
                            binding.btnConfirmarPago.text = "Procesando..."
                        }
                        is PagoUiState.Exitoso -> {
                            Toast.makeText(requireContext(), "✅ Pago realizado con éxito", Toast.LENGTH_LONG).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                        is PagoUiState.Error -> {
                            binding.btnConfirmarPago.isEnabled = true
                            binding.btnConfirmarPago.text = "Confirmar pago"
                            Toast.makeText(requireContext(), state.mensaje, Toast.LENGTH_SHORT).show()
                            viewModel.resetPagoState()
                        }
                        else -> {
                            binding.btnConfirmarPago.isEnabled = true
                            binding.btnConfirmarPago.text = "Confirmar pago"
                        }
                    }
                }
            }
        }
    }

    private fun mostrarSimulacionPago(idCargo: Int, idUsuario: Int, metodo: String, monto: Double) {
        val context = requireContext()
        val builder = androidx.appcompat.app.AlertDialog.Builder(context, R.style.Theme_VecinoAlerta)
        
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#1A1F2C"))
        }

        val title = android.widget.TextView(context).apply {
            text = "Simulación Pasarela: $metodo"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        container.addView(title)

        if (metodo == "VISA" || metodo == "MASTERCARD") {
            val etCard = android.widget.EditText(context).apply {
                hint = "Número de Tarjeta (16 dígitos)"
                setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
                setTextColor(android.graphics.Color.WHITE)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
                setPadding(24, 24, 24, 24)
            }
            container.addView(etCard)

            val layoutHorizontal = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 16 }
            }

            val etVence = android.widget.EditText(context).apply {
                hint = "MM/YY"
                setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
                setTextColor(android.graphics.Color.WHITE)
                inputType = android.text.InputType.TYPE_CLASS_DATETIME or android.text.InputType.TYPE_DATETIME_VARIATION_DATE
                setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
                setPadding(24, 24, 24, 24)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).also {
                    it.marginEnd = 8
                }
            }
            val etCvv = android.widget.EditText(context).apply {
                hint = "CVV"
                setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
                setTextColor(android.graphics.Color.WHITE)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
                setPadding(24, 24, 24, 24)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).also {
                    it.marginStart = 8
                }
            }
            layoutHorizontal.addView(etVence)
            layoutHorizontal.addView(etCvv)
            container.addView(layoutHorizontal)

            val etName = android.widget.EditText(context).apply {
                hint = "Nombre del Titular"
                setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
                setTextColor(android.graphics.Color.WHITE)
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
                setPadding(24, 24, 24, 24)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 16 }
            }
            container.addView(etName)

            builder.setView(container)
            builder.setPositiveButton("Simular Pago", null)
            builder.setNegativeButton("Cancelar", null)

            val dialog = builder.create()
            dialog.show()

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val card = etCard.text.toString().trim()
                val vence = etVence.text.toString().trim()
                val cvv = etCvv.text.toString().trim()
                val name = etName.text.toString().trim()

                if (card.length < 16 || vence.isEmpty() || cvv.length < 3 || name.isEmpty()) {
                    Toast.makeText(context, "Completa los datos de la tarjeta", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                iniciarProcesamientoSimulado(idCargo, idUsuario, metodo, monto)
            }
        } else {
            val info = android.widget.TextView(context).apply {
                text = "Realiza la transferencia al número 987-654-321.\n\nIngresa tu número de celular registrado:"
                setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                textSize = 14f
                setPadding(0, 0, 0, 16)
            }
            container.addView(info)

            val qrMock = android.widget.TextView(context).apply {
                text = "[ Escanear Código QR VecinoAlerta ]"
                setTextColor(android.graphics.Color.parseColor("#10B981"))
                textSize = 15f
                setPadding(24, 24, 24, 24)
                gravity = android.view.Gravity.CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 16 }
            }
            container.addView(qrMock)

            val etCelular = android.widget.EditText(context).apply {
                hint = "Celular (9 dígitos)"
                setHintTextColor(android.graphics.Color.parseColor("#94A3B8"))
                setTextColor(android.graphics.Color.WHITE)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setBackgroundColor(android.graphics.Color.parseColor("#242B3D"))
                setPadding(24, 24, 24, 24)
            }
            container.addView(etCelular)

            builder.setView(container)
            builder.setPositiveButton("Simular Pago", null)
            builder.setNegativeButton("Cancelar", null)

            val dialog = builder.create()
            dialog.show()

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val cel = etCelular.text.toString().trim()
                if (cel.length < 9) {
                    Toast.makeText(context, "Ingresa un número de celular válido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                iniciarProcesamientoSimulado(idCargo, idUsuario, metodo, monto)
            }
        }
    }

    private fun iniciarProcesamientoSimulado(idCargo: Int, idUsuario: Int, metodo: String, monto: Double) {
        val context = requireContext()
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(context, R.style.Theme_VecinoAlerta)
            .setCancelable(false)
            .create()

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(android.graphics.Color.parseColor("#1A1F2C"))
            gravity = android.view.Gravity.CENTER
        }

        val progress = android.widget.ProgressBar(context).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6366F1"))
        }
        val status = android.widget.TextView(context).apply {
            text = "Conectando con la pasarela..."
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            setPadding(0, 24, 0, 0)
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(progress)
        layout.addView(status)
        progressDialog.setView(layout)
        progressDialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(1000)
            status.text = "Verificando fondos y autorizando..."
            kotlinx.coroutines.delay(1000)
            status.text = "Procesando transacción..."
            kotlinx.coroutines.delay(800)
            progressDialog.dismiss()
            viewModel.pagarDigital(idCargo, idUsuario, metodo, monto)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}