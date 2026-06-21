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
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Cargando : LoginUiState()
    data class Exitoso(val usuario: UsuarioEntity) : LoginUiState()
    data class Error(val mensaje: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val usuarioRepo: UsuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(usuario: String, password: String) {
        if (usuario.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Completa todos los campos")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Cargando
            val result = usuarioRepo.login(usuario.trim(), password)
            _uiState.value = if (result != null)
                LoginUiState.Exitoso(result)
            else
                LoginUiState.Error("Usuario o contraseña incorrectos")
        }
    }

    fun resetState() { _uiState.value = LoginUiState.Idle }
}