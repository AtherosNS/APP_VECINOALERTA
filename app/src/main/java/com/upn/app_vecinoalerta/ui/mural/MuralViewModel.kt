
package com.upn.app_vecinoalerta.ui.mural

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.entities.AvisoEntity
import com.upn.app_vecinoalerta.data.repository.ComunicacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════
// MURAL VIEW MODEL — RF-06 / RF-07
// ══════════════════════════════════════════════════════════════════

@HiltViewModel
class MuralViewModel @Inject constructor(
    private val repo: ComunicacionRepository
) : ViewModel() {

    /** RF-07: feed cronológico descendente listo para la UI. */
    val avisos: StateFlow<List<AvisoEntity>> = repo
        .observarAvisos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun publicar(titulo: String, contenido: String, idAdmin: Int) {
        if (titulo.isBlank() || contenido.isBlank()) return
        viewModelScope.launch { repo.publicarAviso(titulo.trim(), contenido.trim(), idAdmin) }
    }
}