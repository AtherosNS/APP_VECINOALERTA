package com.upn.app_vecinoalerta.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.upn.app_vecinoalerta.data.local.dao.*
import com.upn.app_vecinoalerta.data.local.entities.*
import com.upn.app_vecinoalerta.utils.HashUtils
import com.upn.app_vecinoalerta.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancieroRepository @Inject constructor(
    private val dao: FinancieroDao,
    @ApplicationContext private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var escuchadorCargosActivo = false

    /**
     * Inicia escuchador Firestore en tiempo real para cargos financieros.
     * Descarga todos los cargos de Firestore a Room para que el estado de cuenta
     * muestre las deudas creadas por el admin.
     */
    fun iniciarEscuchadorCargos() {
        if (escuchadorCargosActivo) return
        escuchadorCargosActivo = true

        firestore.collection("fees")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                repositoryScope.launch {
                    for (doc in snapshots.documents) {
                        val idCargoRemoto = doc.getLong("id_cargo")?.toInt() ?: 0
                        val idInmueble = doc.getLong("id_inmueble")?.toInt()
                        val idUsuario = doc.getLong("id_usuario")?.toInt()
                        val concepto = doc.getString("concepto") ?: continue
                        val monto = doc.getDouble("monto") ?: continue
                        val mes = doc.getLong("mes")?.toInt() ?: continue
                        val anio = doc.getLong("anio")?.toInt() ?: continue
                        val estadoPago = doc.getString("estado_pago") ?: "PENDIENTE"
                        val idAdmin = doc.getLong("id_administrador")?.toInt()
                        val createdAt = doc.getLong("created_at") ?: System.currentTimeMillis()
                        val updatedAt = doc.getLong("updated_at") ?: System.currentTimeMillis()

                        // Si el cargo remoto tiene id_cargo asignado, verificar si ya existe
                        if (idCargoRemoto > 0) {
                            if (dao.contarCargoPorId(idCargoRemoto) == 0) {
                                try {
                                    dao.insertarCargo(
                                        CargoFinancieroEntity(
                                            idCargo = idCargoRemoto,
                                            idInmueble = idInmueble,
                                            idUsuario = idUsuario,
                                            concepto = concepto,
                                            monto = monto,
                                            mes = mes,
                                            anio = anio,
                                            estadoPago = estadoPago,
                                            idAdministrador = idAdmin,
                                            createdAt = createdAt,
                                            updatedAt = updatedAt
                                        )
                                    )
                                } catch (ex: Exception) {
                                    // Cargo ya existente en Room con ese id — ignorar
                                }
                            }
                        } else {
                            // Documento sin id_cargo numérico: usar timestamp como key de deduplicación
                            if (dao.contarCargoPorTimestamp(createdAt, concepto) == 0) {
                                try {
                                    dao.insertarCargo(
                                        CargoFinancieroEntity(
                                            idInmueble = idInmueble,
                                            idUsuario = idUsuario,
                                            concepto = concepto,
                                            monto = monto,
                                            mes = mes,
                                            anio = anio,
                                            estadoPago = estadoPago,
                                            idAdministrador = idAdmin,
                                            createdAt = createdAt,
                                            updatedAt = updatedAt
                                        )
                                    )
                                } catch (ex: Exception) {
                                    // Ignorar conflictos
                                }
                            }
                        }
                    }
                }
            }
    }

    suspend fun crearCargo(idInmueble: Int, idUsuario: Int?, concepto: String, monto: Double, mes: Int, anio: Int, idAdmin: Int): Long {
        val id = dao.insertarCargo(CargoFinancieroEntity(idInmueble = idInmueble, idUsuario = idUsuario, concepto = concepto, monto = monto, mes = mes, anio = anio, idAdministrador = idAdmin))
        NotificationHelper.notificarNuevoPago(context, concepto, monto, id.toInt())
        // Sincronizar el nuevo cargo a Firestore
        val map = hashMapOf(
            "id_cargo" to id.toInt(),
            "id_inmueble" to idInmueble,
            "id_usuario" to idUsuario,
            "concepto" to concepto,
            "monto" to monto,
            "mes" to mes,
            "anio" to anio,
            "estado_pago" to "PENDIENTE",
            "id_administrador" to idAdmin,
            "created_at" to System.currentTimeMillis(),
            "updated_at" to System.currentTimeMillis()
        )
        firestore.collection("fees").add(map)
        return id
    }

    /** RF-16: pago digital simulado (VISA/MC/YAPE/PLIN). */
    suspend fun pagarDigital(idCargo: Int, idUsuario: Int, metodoPago: String, monto: Double): Boolean {
        val referencia = "DIG-${UUID.randomUUID().toString().take(8).uppercase()}"
        dao.procesarPago(TransaccionEntity(idCargo = idCargo, idUsuario = idUsuario, metodoPago = metodoPago, montoPagado = monto, referenciaPago = referencia))
        return true
    }

    /** RF-17: pago en efectivo registrado por el Admin. */
    suspend fun pagarEfectivo(idCargo: Int, idAdmin: Int, monto: Double) {
        val referencia = "CAJA-${System.currentTimeMillis()}"
        dao.procesarPago(TransaccionEntity(idCargo = idCargo, idUsuario = idAdmin, metodoPago = "EFECTIVO", montoPagado = monto, referenciaPago = referencia))
    }

    fun observarCargosDeUsuario(idU: Int): Flow<List<CargoFinancieroEntity>> = dao.observarCargosDeUsuario(idU)
    fun observarCargosDePropietario(idP: Int): Flow<List<CargoFinancieroEntity>> = dao.observarCargosDePropietario(idP)
    fun observarDeudasHeredadas(idI: Int): Flow<List<CargoFinancieroEntity>> = dao.observarDeudasHeredadas(idI)
    fun observarDeudaTotalPropietario(idP: Int): Flow<List<CargoFinancieroEntity>> = dao.observarDeudaTotalPropietario(idP)
    fun observarTodosPendientes(): Flow<List<CargoFinancieroEntity>> = dao.observarTodosPendientes()
    fun observarTransacciones(idC: Int): Flow<List<TransaccionEntity>> = dao.observarTransacciones(idC)
}