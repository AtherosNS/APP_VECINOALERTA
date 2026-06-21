package com.upn.app_vecinoalerta.ui.encuestas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upn.app_vecinoalerta.data.local.entities.OpcionEncuestaEntity
import com.upn.app_vecinoalerta.databinding.FragmentVotarBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VotarFragment : Fragment() {

    private var _binding: FragmentVotarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EncuestaViewModel by viewModels()

    private var idEncuesta: Int = -1
    private var opciones: List<OpcionEncuestaEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVotarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        idEncuesta = arguments?.getInt("id_encuesta", -1) ?: -1
        val pregunta = arguments?.getString("pregunta", "Encuesta") ?: "Encuesta"

        binding.tvPregunta.text = pregunta

        val sesion = requireActivity().getSharedPreferences("sesion", 0)
        val idUsuario = sesion.getInt("id_usuario", -1)

        // Verificar si el usuario ya votó
        viewModel.verificarVoto(idEncuesta, idUsuario)

        // Observar si ya votó
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.yaVotoState.collect { yaVoto ->
                    if (yaVoto == true) {
                        // Ocultar votación
                        binding.rgOpciones.visibility = View.GONE
                        binding.btnVotar.visibility = View.GONE
                        
                        // Mostrar mensaje de ya votó y resultados
                        binding.tvYaVoto.visibility = View.VISIBLE
                        binding.layoutResultados.visibility = View.VISIBLE
                        
                        // Cargar resultados
                        launch {
                            viewModel.observarResultados(idEncuesta).collect { resultados ->
                                binding.layoutResultados.removeAllViews()
                                val totalVotos = resultados.sumOf { it.total_votos }
                                
                                resultados.forEach { res ->
                                    val itemLayout = android.widget.LinearLayout(requireContext()).apply {
                                        orientation = android.widget.LinearLayout.VERTICAL
                                        layoutParams = android.widget.LinearLayout.LayoutParams(
                                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                        ).also { it.bottomMargin = 24 }
                                    }
                                    
                                    val tvTexto = android.widget.TextView(requireContext()).apply {
                                        text = res.texto_opcion
                                        setTextColor(android.graphics.Color.WHITE)
                                        textSize = 16f
                                        setTypeface(null, android.graphics.Typeface.BOLD)
                                    }
                                    itemLayout.addView(tvTexto)
                                    
                                    val porcentaje = if (totalVotos > 0) {
                                        (res.total_votos.toDouble() / totalVotos) * 100
                                    } else {
                                        0.0
                                    }
                                    
                                    val progress = android.widget.ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                                        max = 100
                                        setProgress(porcentaje.toInt())
                                        progressTintList = android.content.res.ColorStateList.valueOf(
                                            android.graphics.Color.parseColor("#6366F1")
                                        )
                                        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
                                            android.graphics.Color.parseColor("#242B3D")
                                        )
                                        layoutParams = android.widget.LinearLayout.LayoutParams(
                                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                            (8 * resources.displayMetrics.density).toInt()
                                        ).also {
                                            it.topMargin = 8
                                            it.bottomMargin = 4
                                        }
                                    }
                                    itemLayout.addView(progress)
                                    
                                    val tvStats = android.widget.TextView(requireContext()).apply {
                                        text = "${res.total_votos} votos (${String.format(java.util.Locale.US, "%.1f", porcentaje)}%)"
                                        setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                                        textSize = 13f
                                    }
                                    itemLayout.addView(tvStats)
                                    
                                    binding.layoutResultados.addView(itemLayout)
                                }
                            }
                        }
                    } else if (yaVoto == false) {
                        // Mostrar votación
                        binding.rgOpciones.visibility = View.VISIBLE
                        binding.btnVotar.visibility = View.VISIBLE
                        
                        binding.tvYaVoto.visibility = View.GONE
                        binding.layoutResultados.visibility = View.GONE
                        
                        // Cargar opciones para votar
                        launch {
                            viewModel.observarOpciones(idEncuesta).collect { lista ->
                                opciones = lista
                                binding.rgOpciones.removeAllViews()
                                lista.forEach { opcion ->
                                    val rb = RadioButton(requireContext()).apply {
                                        id = opcion.idOpcion
                                        text = opcion.textoOpcion
                                        setTextColor(android.graphics.Color.WHITE)
                                        buttonTintList = android.content.res.ColorStateList.valueOf(
                                            android.graphics.Color.parseColor("#6366F1")
                                        )
                                        textSize = 16f
                                        setPadding(16, 12, 16, 12)
                                    }
                                    binding.rgOpciones.addView(rb)
                                }
                            }
                        }
                    }
                }
            }
        }

        binding.btnVotar.setOnClickListener {
            val checkedId = binding.rgOpciones.checkedRadioButtonId
            if (checkedId != -1) {
                viewModel.votar(idEncuesta, idUsuario, checkedId)
            } else {
                Toast.makeText(requireContext(), "Selecciona una opción", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.votoState.collect { state ->
                    when (state) {
                        is VotoUiState.Exitoso -> {
                            Toast.makeText(requireContext(), "✓ Voto emitido correctamente", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        is VotoUiState.YaVoto -> {
                            Toast.makeText(requireContext(), "⚠️ Ya has votado en esta encuesta", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        is VotoUiState.Error -> {
                            Toast.makeText(requireContext(), state.msg, Toast.LENGTH_SHORT).show()
                            viewModel.resetVotoState()
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
