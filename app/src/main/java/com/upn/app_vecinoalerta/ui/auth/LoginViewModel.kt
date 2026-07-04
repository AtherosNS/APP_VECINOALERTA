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

    fun login(identificador: String, password: String) {
        if (identificador.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Completa todos los campos")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Cargando
            try {
                val result = usuarioRepo.login(identificador.trim(), password)
                _uiState.value = if (result != null)
                    LoginUiState.Exitoso(result)
                else
                    LoginUiState.Error("Correo, DNI o contraseña incorrectos")
            } catch (e: Exception) {
                val msg = when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Contraseña incorrecta"
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "El usuario no está registrado en Firebase"
                    else -> "${e.javaClass.simpleName}: ${e.localizedMessage}"
                }
                _uiState.value = LoginUiState.Error(msg)
            }
        }


    }


    fun resetState() { _uiState.value = LoginUiState.Idle }
}