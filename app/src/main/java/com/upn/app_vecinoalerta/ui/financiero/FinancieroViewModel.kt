package com.upn.app_vecinoalerta.ui.financiero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.entities.CargoFinancieroEntity
import com.upn.app_vecinoalerta.data.repository.FinancieroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PagoUiState {
    object Idle : PagoUiState()
    object Procesando : PagoUiState()
    object Exitoso : PagoUiState()
    data class Error(val mensaje: String) : PagoUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinancieroViewModel @Inject constructor(
    private val repo: FinancieroRepository
) : ViewModel() {

    init {
        // Iniciar sync Firestore→Room de cargos financieros desde el arranque del ViewModel
        repo.iniciarEscuchadorCargos()
    }

    private val _pagoState = MutableStateFlow<PagoUiState>(PagoUiState.Idle)
    val pagoState: StateFlow<PagoUiState> = _pagoState.asStateFlow()

    // IDs reactivos — el Fragment los establece una sola vez en onViewCreated
    private val _idUsuario = MutableStateFlow(-1)
    private val _idPropietario = MutableStateFlow(-1)

    /**
     * RF-15: StateFlow en ViewModel scope. Room sigue emitiendo cargos nuevos
     * incluso cuando el Fragment no es visible. Al volver, el último valor
     * llega inmediatamente — el cobro del admin ya está disponible.
     */
    val misCargos: StateFlow<List<CargoFinancieroEntity>> = _idUsuario
        .flatMapLatest { idU ->
            if (idU == -1) flowOf(emptyList())
            else repo.observarCargosDeUsuario(idU)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cargosDePropietario: StateFlow<List<CargoFinancieroEntity>> = _idPropietario
        .flatMapLatest { idP ->
            if (idP == -1) flowOf(emptyList())
            else repo.observarCargosDePropietario(idP)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val deudaTotalPropietario: StateFlow<List<CargoFinancieroEntity>> = _idPropietario
        .flatMapLatest { idP ->
            if (idP == -1) flowOf(emptyList())
            else repo.observarDeudaTotalPropietario(idP)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Llamar desde Fragment.onViewCreated() para activar los Flows */
    fun inicializarUsuario(idU: Int) { _idUsuario.value = idU }
    fun inicializarPropietario(idP: Int) { _idPropietario.value = idP }

    // Métodos legacy mantenidos para compatibilidad
    fun observarMisCargos(idU: Int): Flow<List<CargoFinancieroEntity>> {
        _idUsuario.value = idU
        return repo.observarCargosDeUsuario(idU)
    }

    fun observarCargosDePropietario(idP: Int): Flow<List<CargoFinancieroEntity>> {
        _idPropietario.value = idP
        return repo.observarCargosDePropietario(idP)
    }

    fun observarDeudaTotalPropietario(idP: Int): Flow<List<CargoFinancieroEntity>> {
        _idPropietario.value = idP
        return repo.observarDeudaTotalPropietario(idP)
    }

    fun pagarDigital(idCargo: Int, idUsuario: Int, metodoPago: String, monto: Double) {
        viewModelScope.launch {
            _pagoState.value = PagoUiState.Procesando
            try {
                val success = repo.pagarDigital(idCargo, idUsuario, metodoPago, monto)
                if (success) {
                    _pagoState.value = PagoUiState.Exitoso
                } else {
                    _pagoState.value = PagoUiState.Error("Error al procesar el pago digital")
                }
            } catch (e: Exception) {
                _pagoState.value = PagoUiState.Error(e.message ?: "Error al procesar pago")
            }
        }
    }

    fun pagarEfectivo(idCargo: Int, idAdmin: Int, monto: Double) {
        viewModelScope.launch {
            try {
                repo.pagarEfectivo(idCargo, idAdmin, monto)
            } catch (e: Exception) { /* error handling */ }
        }
    }

    fun resetPagoState() { _pagoState.value = PagoUiState.Idle }
}
