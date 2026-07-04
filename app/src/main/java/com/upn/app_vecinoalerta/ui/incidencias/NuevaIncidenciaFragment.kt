// ═══════════════════════════════════════════════════════════════
// NuevaIncidenciaFragment.kt
// ═══════════════════════════════════════════════════════════════
package com.upn.app_vecinoalerta.ui.incidencias

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.upn.app_vecinoalerta.databinding.FragmentNuevaIncidenciaBinding
import com.upn.app_vecinoalerta.utils.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

/**
 * RF-13: reporte de incidencia con foto y descripción.
 * RNF-01: máximo 3 pasos desde el dashboard.
 */
@AndroidEntryPoint
class NuevaIncidenciaFragment : Fragment() {

    private var _binding: FragmentNuevaIncidenciaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IncidenciaViewModel by viewModels()

    private var fotoUri: Uri? = null

    // Lanzador para tomar foto con la cámara
    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) binding.ivPreviewFoto.setImageURI(fotoUri)
    }

    // Lanzador para elegir de galería
    private val elegirGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { fotoUri = it; binding.ivPreviewFoto.setImageURI(it) }
    }

    // Lanzador para permiso de cámara
    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        if (permisos.all { it.value }) abrirCamara()
        else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNuevaIncidenciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCamara.setOnClickListener {
            if (PermissionHelper.tieneTodos(requireContext(), PermissionHelper.CAMARA))
                abrirCamara()
            else
                pedirPermiso.launch(PermissionHelper.CAMARA)
        }

        binding.btnGaleria.setOnClickListener { elegirGaleria.launch("image/*") }

        binding.btnEnviar.setOnClickListener {
            val idUsuario = com.upn.app_vecinoalerta.utils.SecurePrefs.getInt(requireContext(), "id_usuario", -1)
            val categoria = when (binding.rgCategoria.checkedRadioButtonId) {
                binding.rbSeguridad.id      -> "SEGURIDAD"
                binding.rbLimpieza.id       -> "LIMPIEZA"
                binding.rbInfraestructura.id -> "INFRAESTRUCTURA"
                else -> ""
            }
            viewModel.reportar(idUsuario, categoria, binding.etDescripcion.text.toString(), fotoUri)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is IncidenciaUiState.Enviando -> binding.btnEnviar.isEnabled = false
                        is IncidenciaUiState.Enviado  -> {
                            Toast.makeText(requireContext(), "Incidencia reportada ✓", Toast.LENGTH_SHORT).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                        is IncidenciaUiState.Error    -> {
                            binding.btnEnviar.isEnabled = true
                            Toast.makeText(requireContext(), state.mensaje, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        else -> binding.btnEnviar.isEnabled = true
                    }
                }
            }
        }
    }

    private fun abrirCamara() {
        val dir = File(requireContext().filesDir, "incidencias").also { it.mkdirs() }
        val archivo = File(dir, "foto_temp_${System.currentTimeMillis()}.jpg")
        fotoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", archivo)
        tomarFoto.launch(fotoUri!!)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}