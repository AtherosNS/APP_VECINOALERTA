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
class ComunicacionRepository @Inject constructor(
    private val avisoDao: AvisoDao,
    private val asambleaDao: AsambleaDao,
    private val encuestaDao: EncuestaDao,
    @ApplicationContext private val context: Context
) {
    // Avisos
    suspend fun publicarAviso(titulo: String, contenido: String, idAdmin: Int): Long =
        avisoDao.insertar(AvisoEntity(titulo = titulo, contenido = contenido, idAdministrador = idAdmin))

    fun observarAvisos(): Flow<List<AvisoEntity>> = avisoDao.observarTodos()

    // Asambleas
    suspend fun crearAsamblea(titulo: String, agenda: String, fechaHora: Long, lugar: String, idAdmin: Int): Long {
        val id = asambleaDao.insertar(AsambleaEntity(titulo = titulo, agenda = agenda, fechaHora = fechaHora, lugar = lugar, idAdministrador = idAdmin))
        // RF-09: notificación push local
        NotificationHelper.notificarAsamblea(context, titulo, "Fecha: $fechaHora", id.toInt())
        return id
    }

    suspend fun actualizarAsamblea(asamblea: AsambleaEntity) {
        asambleaDao.actualizar(asamblea.copy(syncVersion = asamblea.syncVersion + 1))
        NotificationHelper.notificarAsamblea(context, asamblea.titulo, "Reunión actualizada", asamblea.idAsamblea)
    }

    fun observarAsambleasProximas(): Flow<List<AsambleaEntity>> = asambleaDao.observarProximas()
    fun observarTodasAsambleas(): Flow<List<AsambleaEntity>>    = asambleaDao.observarTodas()

    // Encuestas
    suspend fun crearEncuesta(pregunta: String, opciones: List<String>, idAdmin: Int): Long {
        val idEncuesta = encuestaDao.insertarEncuesta(EncuestaEntity(pregunta = pregunta, idAdministrador = idAdmin, estado = "ACTIVA"))
        opciones.forEach { encuestaDao.insertarOpcion(OpcionEncuestaEntity(idEncuesta = idEncuesta.toInt(), textoOpcion = it)) }
        return idEncuesta
    }

    /** RF-10: retorna false si el usuario ya votó. */
    suspend fun votar(idEncuesta: Int, idUsuario: Int, idOpcion: Int): Boolean {
        val yaVoto = encuestaDao.contarVotosDelUsuario(idEncuesta, idUsuario) > 0
        if (yaVoto) return false
        val insertado = encuestaDao.insertarVoto(VotoEntity(idEncuesta = idEncuesta, idUsuario = idUsuario, idOpcion = idOpcion))
        return insertado > 0
    }

    suspend fun yaVoto(idEncuesta: Int, idUsuario: Int): Boolean =
        encuestaDao.contarVotosDelUsuario(idEncuesta, idUsuario) > 0

    fun observarEncuestasActivas(): Flow<List<EncuestaEntity>> = encuestaDao.observarActivas()
    fun observarOpciones(id: Int): Flow<List<OpcionEncuestaEntity>> = encuestaDao.observarOpciones(id)
    fun observarResultados(id: Int): Flow<List<ResultadoOpcion>>    = encuestaDao.observarResultados(id)
}