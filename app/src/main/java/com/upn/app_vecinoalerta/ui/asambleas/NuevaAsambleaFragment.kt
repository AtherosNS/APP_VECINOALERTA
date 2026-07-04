package com.upn.app_vecinoalerta.ui.asambleas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.upn.app_vecinoalerta.databinding.FragmentNuevaAsambleaBinding
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class NuevaAsambleaFragment : Fragment() {

    private var _binding: FragmentNuevaAsambleaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AsambleaViewModel by viewModels()

    private var fechaHoraSeleccionada: Long = 0L // 0 = no seleccionada

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevaAsambleaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val idAdmin = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)

        // Abrir DatePicker → al confirmar → abrir TimePicker
        binding.btnFechaHora.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, anio, mes, dia ->
                    // Mes en Calendar es 0-based, TimePickerDialog usa hora 24h
                    TimePickerDialog(
                        requireContext(),
                        { _, hora, minuto ->
                            cal.set(anio, mes, dia, hora, minuto, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            fechaHoraSeleccionada = cal.timeInMillis

                            // Mostrar fecha seleccionada en el label verde
                            val fmt = SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale("es", "PE"))
                            binding.tvFechaSeleccionada.text = "✅ Fecha: ${fmt.format(cal.time)}"
                            binding.tvFechaSeleccionada.visibility = View.VISIBLE
                            binding.btnFechaHora.text = "📅  ${fmt.format(cal.time)}"
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        true // formato 24h
                    ).show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).also { dialog ->
                // No permitir seleccionar fechas pasadas
                dialog.datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }

        binding.btnConvocar.setOnClickListener {
            val titulo = binding.etTitulo.text.toString().trim()
            val agenda = binding.etDescripcion.text.toString().trim()
            val lugar = binding.etLugar.text.toString().trim()
                .ifBlank { "Salón de Reuniones Principal" }

            when {
                titulo.isBlank() -> {
                    binding.layoutTitulo.error = "Ingresa el título de la asamblea"
                    return@setOnClickListener
                }
                fechaHoraSeleccionada == 0L -> {
                    Toast.makeText(requireContext(),
                        "⚠️ Debes seleccionar la fecha y hora de la asamblea",
                        Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else -> {
                    binding.layoutTitulo.error = null
                    viewModel.crear(titulo, agenda, fechaHoraSeleccionada, lugar, idAdmin)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AsambleaUiState.Guardado -> {
                            Toast.makeText(requireContext(),
                                "✅ Asamblea convocada exitosamente", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        is AsambleaUiState.Error -> {
                            Toast.makeText(requireContext(), state.mensaje, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
