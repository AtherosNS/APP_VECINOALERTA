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
class ChatRepository @Inject constructor(
    private val dao: ChatDao
) {
    /** RF-11: construye el nombre estático antes de insertar. */
    suspend fun enviarGrupal(idUsuario: Int, nombreCompleto: String, rol: String, contenido: String): Long {
        val nombreEstatico = "$nombreCompleto ($rol)"
        return dao.insertarGrupal(MensajeGrupalEntity(idUsuario = idUsuario, nombreEmisorEstatico = nombreEstatico, contenido = contenido))
    }

    suspend fun enviarPrivado(idEmisor: Int, idReceptor: Int, nombreCompleto: String, rol: String, contenido: String): Long {
        val nombreEstatico = "$nombreCompleto ($rol)"
        return dao.insertarPrivado(MensajePrivadoEntity(idEmisor = idEmisor, idReceptor = idReceptor, nombreEmisorEstatico = nombreEstatico, contenido = contenido))
    }

    fun observarChatGrupal(): Flow<List<MensajeGrupalEntity>>          = dao.observarGrupal()
    fun observarHiloPrivado(a: Int, b: Int): Flow<List<MensajePrivadoEntity>> = dao.observarHiloPrivado(a, b)
    fun observarNoLeidos(id: Int): Flow<Int>                           = dao.observarNoLeidos(id)
    suspend fun marcarLeidos(receptor: Int, emisor: Int)               = dao.marcarLeidos(receptor, emisor)
}