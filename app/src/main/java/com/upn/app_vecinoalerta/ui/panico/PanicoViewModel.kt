package com.upn.app_vecinoalerta.ui.panico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.entities.ContactoEmergenciaEntity
import com.upn.app_vecinoalerta.data.repository.EmergenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════
// PANICO VIEW MODEL — RF-05 / RNF-01 / RNF-05
// ══════════════════════════════════════════════════════════════════

@HiltViewModel
class PanicoViewModel @Inject constructor(
    repo: EmergenciaRepository
) : ViewModel() {

    /**
     * RF-05: lista de contactos activos, pre-poblada en la instalación.
     * RNF-05: funciona 100% offline — solo lee de Room local.
     * RNF-01: la Activity usa este Flow para mostrar los botones
     *         de marcación directa en máximo 1 tap adicional.
     */
    val contactos: StateFlow<List<ContactoEmergenciaEntity>> = repo
        .observarContactos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Agrupa contactos por tipo para la UI (POLICIA, BOMBEROS, etc.) */
    val contactosPorTipo: StateFlow<Map<String, List<ContactoEmergenciaEntity>>> =
        contactos
            .map { lista -> lista.groupBy { it.tipo } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}