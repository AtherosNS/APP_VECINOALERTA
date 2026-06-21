package com.upn.app_vecinoalerta.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.entities.InmuebleEntity
import com.upn.app_vecinoalerta.data.local.entities.UsuarioEntity
import com.upn.app_vecinoalerta.data.repository.InmuebleRepository
import com.upn.app_vecinoalerta.data.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
sealed class RegistroUiState {
    object Idle : RegistroUiState()
    object Cargando : RegistroUiState()
    object Exitoso : RegistroUiState()    // cuenta creada, esperando aprobación (RF-02)
    data class Error(val mensaje: String) : RegistroUiState()
}

@HiltViewModel
class RegistroViewModel @Inject constructor(
    private val usuarioRepo: UsuarioRepository,
    private val inmuebleRepo: InmuebleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    /** RF-01: Spinner del RESIDENTE — solo inmuebles activos. */
    val inmuebles: StateFlow<List<InmuebleEntity>> = inmuebleRepo
        .observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun registrar(
        nombre: String, apellido: String, dni: String,
        correo: String, usuario: String, password: String, confirmPassword: String,
        rol: String, idInmueble: Int? = null, direccion: String? = null
    ) {
        // Validaciones básicas
        when {
            listOf(nombre, apellido, dni, correo, usuario, password).any { it.isBlank() } ->
            { _uiState.value = RegistroUiState.Error("Todos los campos son obligatorios"); return }
            rol == "PROPIETARIO" && direccion.isNullOrBlank() ->
            { _uiState.value = RegistroUiState.Error("La dirección de la propiedad es obligatoria"); return }
            password != confirmPassword ->
            { _uiState.value = RegistroUiState.Error("Las contraseñas no coinciden"); return }
            password.length < 6 ->
            { _uiState.value = RegistroUiState.Error("La contraseña debe tener al menos 6 caracteres"); return }
            dni.length != 8 ->
            { _uiState.value = RegistroUiState.Error("El DNI debe tener 8 dígitos"); return }
            rol == "RESIDENTE" && idInmueble == null ->
            { _uiState.value = RegistroUiState.Error("Selecciona tu inmueble"); return }
        }

        viewModelScope.launch {
            _uiState.value = RegistroUiState.Cargando
            try {
                val userId = usuarioRepo.registrar(nombre.trim(), apellido.trim(), dni.trim(),
                    correo.trim(), usuario.trim(), password, rol, idInmueble)
                
                if (rol == "PROPIETARIO" && !direccion.isNullOrBlank()) {
                    inmuebleRepo.registrar(direccion.trim(), "Propiedad de ${nombre.trim()} ${apellido.trim()}", userId.toInt())
                }
                
                _uiState.value = RegistroUiState.Exitoso
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("UNIQUE") == true -> "DNI, correo o usuario ya registrado"
                    else -> "Error al registrar. Intenta de nuevo"
                }
                _uiState.value = RegistroUiState.Error(msg)
            }
        }
    }

    fun resetState() { _uiState.value = RegistroUiState.Idle }
}