package com.upn.app_vecinoalerta.data.repository

import android.content.Context
import com.upn.app_vecinoalerta.data.local.dao.*
import com.upn.app_vecinoalerta.data.local.entities.*
import com.upn.app_vecinoalerta.utils.HashUtils
import com.upn.app_vecinoalerta.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class FinancieroRepository @Inject constructor(
    private val dao: FinancieroDao,
    @ApplicationContext private val context: Context
) {
    suspend fun crearCargo(idInmueble: Int, idUsuario: Int?, concepto: String, monto: Double, mes: Int, anio: Int, idAdmin: Int): Long {
        val id = dao.insertarCargo(CargoFinancieroEntity(idInmueble = idInmueble, idUsuario = idUsuario, concepto = concepto, monto = monto, mes = mes, anio = anio, idAdministrador = idAdmin))
        NotificationHelper.notificarNuevoPago(context, concepto, monto, id.toInt())
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