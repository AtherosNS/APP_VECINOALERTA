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
class InmuebleRepository @Inject constructor(
    private val dao: InmuebleDao
) {
    suspend fun registrar(direccion: String, descripcion: String?, idPropietario: Int): Long =
        dao.insertar(InmuebleEntity(direccion = direccion, descripcion = descripcion, idPropietario = idPropietario))

    /** RF-01: alimenta el Spinner del RESIDENTE. */
    fun observarTodos(): Flow<List<InmuebleEntity>>          = dao.observarTodos()
    fun observarDisponibles(): Flow<List<InmuebleEntity>>    = dao.observarDisponibles()
    fun observarPorPropietario(id: Int): Flow<List<InmuebleEntity>> = dao.observarPorPropietario(id)

    suspend fun obtenerTodosLista(): List<InmuebleEntity> = dao.obtenerTodosLista()

    suspend fun obtenerPorId(id: Int): InmuebleEntity? = dao.obtenerPorId(id)

    suspend fun actualizarEstado(inmueble: InmuebleEntity, nuevoEstado: String) =
        dao.actualizar(inmueble.copy(estado = nuevoEstado, updatedAt = System.currentTimeMillis(), syncVersion = inmueble.syncVersion + 1))
}