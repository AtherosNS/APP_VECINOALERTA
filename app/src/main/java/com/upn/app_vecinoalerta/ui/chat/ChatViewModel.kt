package com.upn.app_vecinoalerta.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.entities.MensajeGrupalEntity
import com.upn.app_vecinoalerta.data.local.entities.MensajePrivadoEntity
import com.upn.app_vecinoalerta.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════
// CHAT VIEW MODEL — RF-11
// ══════════════════════════════════════════════════════════════════

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepository
) : ViewModel() {

    fun inicializarSincronizacion(currentUserId: Int) {
        repo.iniciarEscuchadorGrupal()
        repo.iniciarEscuchadoresPrivados(currentUserId)
    }

    // ── Chat Grupal ────────────────────────────────────────────────

    val mensajesGrupales: StateFlow<List<MensajeGrupalEntity>> = repo
        .observarChatGrupal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * RF-11: el nombre estático se construye aquí antes de persistir.
     * Aunque el usuario pase a INACTIVO, los mensajes conservarán la identidad.
     */
    fun enviarMensajeGrupal(idUsuario: Int, nombreCompleto: String, rol: String, contenido: String) {
        if (contenido.isBlank()) return
        viewModelScope.launch {
            repo.enviarGrupal(idUsuario, nombreCompleto, rol, contenido.trim())
        }
    }

    // ── Chat Privado ───────────────────────────────────────────────

    /**
     * Retorna un Flow del hilo privado entre dos usuarios.
     * Se llama desde ChatPrivadoFragment pasando los IDs de ambos.
     */
    fun observarHiloPrivado(usuarioA: Int, usuarioB: Int): Flow<List<MensajePrivadoEntity>> =
        repo.observarHiloPrivado(usuarioA, usuarioB)

    fun enviarMensajePrivado(
        idEmisor: Int, idReceptor: Int,
        nombreCompleto: String, rol: String,
        contenido: String
    ) {
        if (contenido.isBlank()) return
        viewModelScope.launch {
            repo.enviarPrivado(idEmisor, idReceptor, nombreCompleto, rol, contenido.trim())
        }
    }

    fun marcarLeidos(idReceptor: Int, idEmisor: Int) =
        viewModelScope.launch { repo.marcarLeidos(idReceptor, idEmisor) }

    /** Badge de no leídos para la UI del menú. */
    fun observarNoLeidos(idUsuario: Int): Flow<Int> = repo.observarNoLeidos(idUsuario)
}