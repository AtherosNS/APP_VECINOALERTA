package com.upn.app_vecinoalerta.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upn.app_vecinoalerta.data.local.entities.*
import com.upn.app_vecinoalerta.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════
// ADMIN VIEW MODEL — RF-02 / RF-14 / RF-17 / RF-15 / RF-18
// ══════════════════════════════════════════════════════════════════

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val usuarioRepo: UsuarioRepository,
    private val incidenciaRepo: IncidenciaRepository,
    private val financieroRepo: FinancieroRepository,
    private val inmuebleRepo: InmuebleRepository
) : ViewModel() {

    /** RF-02: cuentas pendientes de aprobación. */
    val pendientes: StateFlow<List<UsuarioEntity>> = usuarioRepo
        .observarPendientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** RF-14: todas las incidencias para gestión. */
    val todasIncidencias: StateFlow<List<IncidenciaEntity>> = incidenciaRepo
        .observarPorEstado(null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** RF-17: cargos pendientes para cobro en caja. */
    val cargosPendientes: StateFlow<List<CargoFinancieroEntity>> = financieroRepo
        .observarTodosPendientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Flujos para la creación de cargos */
    val usuariosActivos: StateFlow<List<UsuarioEntity>> = usuarioRepo
        .observarActivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todosInmuebles: StateFlow<List<InmuebleEntity>> = inmuebleRepo
        .observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun aprobarUsuario(id: Int) = viewModelScope.launch { usuarioRepo.aprobar(id) }
    fun desactivarUsuario(id: Int) = viewModelScope.launch { usuarioRepo.desactivar(id) }

    /** RF-14 */
    fun actualizarEstadoIncidencia(idIncidencia: Int, estado: String, idAdmin: Int) =
        viewModelScope.launch { incidenciaRepo.actualizarEstado(idIncidencia, estado, idAdmin) }

    /** RF-17 */
    fun cobrarEfectivo(idCargo: Int, idAdmin: Int, monto: Double) =
        viewModelScope.launch { financieroRepo.pagarEfectivo(idCargo, idAdmin, monto) }

    /** RF-15 / RF-18: crear cargos/pagos */
    fun crearCargo(idInmueble: Int, idUsuario: Int?, concepto: String, monto: Double, mes: Int, anio: Int, idAdmin: Int) =
        viewModelScope.launch {
            financieroRepo.crearCargo(idInmueble, idUsuario, concepto, monto, mes, anio, idAdmin)
        }

    fun crearCargoTodosInmuebles(concepto: String, monto: Double, mes: Int, anio: Int, idAdmin: Int) =
        viewModelScope.launch {
            val usuariosList = usuarioRepo.obtenerActivosLista()
            val inmueblesList = inmuebleRepo.obtenerTodosLista()
            inmueblesList.forEach { inmueble ->
                // Prioridad 1: residente activo asignado al inmueble
                val residente = usuariosList.firstOrNull {
                    it.idInmuebleAsignado == inmueble.idInmueble && it.rol == "RESIDENTE"
                }
                // Prioridad 2: propietario del inmueble (para que él también vea el cargo)
                val propietario = usuariosList.firstOrNull {
                    it.idUsuario == inmueble.idPropietario && it.rol == "PROPIETARIO"
                }
                // El cargo se asigna al residente si existe, de lo contrario al propietario
                val idUsuario = residente?.idUsuario ?: propietario?.idUsuario
                financieroRepo.crearCargo(inmueble.idInmueble, idUsuario, concepto, monto, mes, anio, idAdmin)
            }
        }
}