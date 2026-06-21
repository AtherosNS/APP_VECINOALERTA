package com.upn.app_vecinoalerta.ui.incidencias

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.entities.IncidenciaEntity
import com.upn.app_vecinoalerta.data.repository.IncidenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════
// INCIDENCIA VIEW MODEL — RF-13 / RF-14
// ══════════════════════════════════════════════════════════════════

sealed class IncidenciaUiState {
    object Idle : IncidenciaUiState()
    object Enviando : IncidenciaUiState()
    object Enviado : IncidenciaUiState()
    data class Error(val mensaje: String) : IncidenciaUiState()
}

@HiltViewModel
class IncidenciaViewModel @Inject constructor(
    private val repo: IncidenciaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<IncidenciaUiState>(IncidenciaUiState.Idle)
    val uiState: StateFlow<IncidenciaUiState> = _uiState.asStateFlow()

    fun observarMisIncidencias(idUsuario: Int): Flow<List<IncidenciaEntity>> =
        repo.observarDeUsuario(idUsuario)

    fun observarPorEstado(estado: String? = null): Flow<List<IncidenciaEntity>> =
        repo.observarPorEstado(estado)

    /**
     * RF-13: reporta una incidencia.
     * Si hay foto, la copia al directorio privado de la app para que
     * persista aunque el usuario cambie de galería.
     * RNF-01: máx 3 pasos desde el dashboard — este método es el paso final.
     */
    fun reportar(
        idUsuario: Int,
        categoria: String,
        descripcion: String,
        fotoUri: Uri? = null
    ) {
        if (categoria.isBlank() || descripcion.isBlank()) {
            _uiState.value = IncidenciaUiState.Error("Completa categoría y descripción")
            return
        }
        viewModelScope.launch {
            _uiState.value = IncidenciaUiState.Enviando
            try {
                val fotoPath = fotoUri?.let { copiarFotoAlStorage(it) }
                repo.reportar(idUsuario, categoria, descripcion.trim(), fotoPath)
                _uiState.value = IncidenciaUiState.Enviado
            } catch (e: Exception) {
                _uiState.value = IncidenciaUiState.Error("Error al enviar el reporte")
            }
        }
    }

    /** RF-14: solo el Admin llama a esto desde AdminViewModel o directamente. */
    fun actualizarEstado(idIncidencia: Int, nuevoEstado: String, idAdmin: Int) =
        viewModelScope.launch { repo.actualizarEstado(idIncidencia, nuevoEstado, idAdmin) }

    /**
     * Copia la foto desde la Uri (cámara/galería) al directorio privado de la app.
     * Devuelve la ruta absoluta del archivo copiado.
     */
    private fun copiarFotoAlStorage(uri: Uri): String {
        val dir = File(context.filesDir, "incidencias").also { it.mkdirs() }
        val destino = File(dir, "img_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destino).use { output -> input.copyTo(output) }
        }
        return destino.absolutePath
    }

    fun resetState() { _uiState.value = IncidenciaUiState.Idle }
}