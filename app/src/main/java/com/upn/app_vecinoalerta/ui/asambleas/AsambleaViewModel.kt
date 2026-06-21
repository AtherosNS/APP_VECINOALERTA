package com.upn.app_vecinoalerta.ui.asambleas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.entities.AsambleaEntity
import com.upn.app_vecinoalerta.data.repository.ComunicacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════
// ASAMBLEA VIEW MODEL — RF-08 / RF-09
// ══════════════════════════════════════════════════════════════════

sealed class AsambleaUiState {
    object Idle : AsambleaUiState()
    object Guardado : AsambleaUiState()
    data class Error(val mensaje: String) : AsambleaUiState()
}

@HiltViewModel
class AsambleaViewModel @Inject constructor(
    private val repo: ComunicacionRepository
) : ViewModel() {

    val proximas: StateFlow<List<AsambleaEntity>> = repo
        .observarAsambleasProximas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todas: StateFlow<List<AsambleaEntity>> = repo
        .observarTodasAsambleas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow<AsambleaUiState>(AsambleaUiState.Idle)
    val uiState: StateFlow<AsambleaUiState> = _uiState.asStateFlow()

    /** RF-08: crea convocatoria y dispara notificación push (RF-09). */
    fun crear(titulo: String, agenda: String, fechaHora: Long, lugar: String, idAdmin: Int) {
        if (titulo.isBlank() || agenda.isBlank() || lugar.isBlank()) {
            _uiState.value = AsambleaUiState.Error("Completa todos los campos")
            return
        }
        viewModelScope.launch {
            try {
                repo.crearAsamblea(titulo.trim(), agenda.trim(), fechaHora, lugar.trim(), idAdmin)
                _uiState.value = AsambleaUiState.Guardado
            } catch (e: Exception) {
                _uiState.value = AsambleaUiState.Error(e.message ?: "Error al convocar la asamblea")
            }
        }
    }

    fun actualizar(asamblea: AsambleaEntity) =
        viewModelScope.launch { repo.actualizarAsamblea(asamblea) }

    fun resetState() { _uiState.value = AsambleaUiState.Idle }
}