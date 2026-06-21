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
class UsuarioRepository @Inject constructor(
    private val dao: UsuarioDao
) {
    /** RF-01: Auto-registro. Hashea la contraseña antes de persistir. */
    suspend fun registrar(
        nombre: String, apellido: String, dni: String,
        correo: String, usuario: String, password: String,
        rol: String, idInmueble: Int? = null
    ): Long {
        val entity = UsuarioEntity(
            nombre = nombre, apellido = apellido, dni = dni,
            correo = correo, usuario = usuario,
            contrasenaHash = HashUtils.hashPassword(password),
            rol = rol, idInmuebleAsignado = idInmueble
            // estado = "PENDIENTE" por defecto (RF-02)
        )
        return dao.insertar(entity)
    }

    /**
     * RF-03: Login. Verifica hash BCrypt.
     * Retorna el usuario si las credenciales son válidas, null si no.
     */
    suspend fun login(usuario: String, password: String): UsuarioEntity? {
        val entity = dao.buscarActivoPorUsuario(usuario) ?: return null
        return if (HashUtils.checkPassword(password, entity.contrasenaHash)) entity else null
    }

    /** RF-02: el Admin aprueba una cuenta PENDIENTE. */
    suspend fun aprobar(idUsuario: Int) = dao.aprobar(idUsuario)

    /** RF-04: borrado lógico. */
    suspend fun desactivar(idUsuario: Int) = dao.desactivar(idUsuario)

    suspend fun obtenerActivosLista(): List<UsuarioEntity> = dao.obtenerActivosLista()

    fun observarPendientes(): Flow<List<UsuarioEntity>> = dao.observarPendientes()
    fun observarActivos(): Flow<List<UsuarioEntity>>    = dao.observarActivos()
    fun observarPorId(id: Int): Flow<UsuarioEntity?>   = dao.observarPorId(id)
}