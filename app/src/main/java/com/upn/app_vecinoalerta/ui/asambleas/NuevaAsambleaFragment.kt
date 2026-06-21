package com.upn.app_vecinoalerta.ui.asambleas

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

@AndroidEntryPoint
class NuevaAsambleaFragment : Fragment() {

    private var _binding: FragmentNuevaAsambleaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AsambleaViewModel by viewModels()

    private var fechaHoraSeleccionada: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // Mañana por defecto

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevaAsambleaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sesion = requireActivity().getSharedPreferences("sesion", 0)
        val idAdmin = sesion.getInt("id_usuario", -1)

        binding.btnFechaHora.setOnClickListener {
            // Simulamos selección de fecha/hora: seteamos para dentro de 48 horas
            fechaHoraSeleccionada = System.currentTimeMillis() + 48 * 60 * 60 * 1000
            Toast.makeText(requireContext(), "Convocatoria fijada para dentro de 48 horas", Toast.LENGTH_SHORT).show()
        }

        binding.btnConvocar.setOnClickListener {
            val titulo = binding.etTitulo.text.toString()
            val agenda = binding.etDescripcion.text.toString()
            val lugar = "Salón de Reuniones Principal" // Por defecto o estático
            viewModel.crear(titulo, agenda, fechaHoraSeleccionada, lugar, idAdmin)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AsambleaUiState.Guardado -> {
                            Toast.makeText(requireContext(), "Asamblea convocada exitosamente", Toast.LENGTH_SHORT).show()
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
