package com.upn.app_vecinoalerta.ui.encuestas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.dao.ResultadoOpcion
import com.upn.app_vecinoalerta.data.local.entities.EncuestaEntity
import com.upn.app_vecinoalerta.data.local.entities.OpcionEncuestaEntity
import com.upn.app_vecinoalerta.data.repository.ComunicacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════
// ENCUESTA VIEW MODEL — RF-10
// ══════════════════════════════════════════════════════════════════

sealed class VotoUiState {
    object Idle : VotoUiState()
    object Votando : VotoUiState()
    object Exitoso : VotoUiState()
    object YaVoto : VotoUiState()       // RF-10: intento de doble voto
    data class Error(val msg: String) : VotoUiState()
}

@HiltViewModel
class EncuestaViewModel @Inject constructor(
    private val repo: ComunicacionRepository
) : ViewModel() {

    val encuestasActivas: StateFlow<List<EncuestaEntity>> = repo
        .observarEncuestasActivas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _votoState = MutableStateFlow<VotoUiState>(VotoUiState.Idle)
    val votoState: StateFlow<VotoUiState> = _votoState.asStateFlow()

    private val _yaVotoState = MutableStateFlow<Boolean?>(null)
    val yaVotoState: StateFlow<Boolean?> = _yaVotoState.asStateFlow()

    fun verificarVoto(idEncuesta: Int, idUsuario: Int) {
        viewModelScope.launch {
            _yaVotoState.value = repo.yaVoto(idEncuesta, idUsuario)
        }
    }

    fun observarOpciones(idEncuesta: Int): Flow<List<OpcionEncuestaEntity>> =
        repo.observarOpciones(idEncuesta)

    fun observarResultados(idEncuesta: Int): Flow<List<ResultadoOpcion>> =
        repo.observarResultados(idEncuesta)

    /** RF-10: el repository garantiza un voto por usuario. */
    fun votar(idEncuesta: Int, idUsuario: Int, idOpcion: Int) {
        viewModelScope.launch {
            _votoState.value = VotoUiState.Votando
            val ok = repo.votar(idEncuesta, idUsuario, idOpcion)
            _votoState.value = if (ok) VotoUiState.Exitoso else VotoUiState.YaVoto
        }
    }

    fun crearEncuesta(pregunta: String, opciones: List<String>, idAdmin: Int) {
        if (pregunta.isBlank() || opciones.size < 2) {
            _votoState.value = VotoUiState.Error("Ingresa la pregunta y al menos 2 opciones")
            return
        }
        viewModelScope.launch {
            try {
                repo.crearEncuesta(pregunta.trim(), opciones, idAdmin)
            } catch (e: Exception) {
                _votoState.value = VotoUiState.Error(e.message ?: "Error al crear la encuesta")
            }
        }
    }

    fun resetVotoState() { _votoState.value = VotoUiState.Idle }
}