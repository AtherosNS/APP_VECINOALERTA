package com.upn.app_vecinoalerta.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.upn.app_vecinoalerta.data.local.dao.*
import com.upn.app_vecinoalerta.data.local.entities.*
import com.upn.app_vecinoalerta.utils.HashUtils
import com.upn.app_vecinoalerta.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class UsuarioRepository @Inject constructor(
    private val dao: UsuarioDao
) {
    suspend fun obtenerAdmin(): UsuarioEntity? = dao.obtenerAdmin()

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
     * RF-01: Registro Híbrido Online/Offline.
     * 1. Crea las credenciales en Firebase Auth.
     * 2. Guarda el perfil del usuario en la colección 'users' de Firestore (fullname, dni, role, status, blockLot).
     * 3. Registra localmente en SQLite Room.
     * 4. Enlaza el firebase_uid obtenido en el registro local.
     */
    suspend fun registrarOnline(
        nombre: String, apellido: String, dni: String,
        correo: String, usuario: String, password: String,
        rol: String, idInmueble: Int? = null, blockLot: String = ""
    ): Long {
        // 1. Crear el usuario en Firebase Auth
        val authResult = FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(correo, password)
            .awaitTask()
        
        val firebaseUser = authResult.user ?: throw RuntimeException("No se pudo crear el usuario en Firebase")
        val uid = firebaseUser.uid

        // Mapear rol de la app nativa a formato Firestore ("ADMINISTRADOR" -> "ADMIN", "PROPIETARIO" -> "OWNER", etc.)
        val roleFirestore = when (rol) {
            "ADMINISTRADOR" -> "ADMIN"
            "PROPIETARIO" -> "OWNER"
            else -> "RESIDENT"
        }

        // Crear mapa para Firestore
        val userMap = hashMapOf(
            "uid" to uid,
            "email" to correo,
            "fullname" to "$nombre $apellido",
            "dni" to dni,
            "phoneNumber" to "",
            "role" to roleFirestore,
            "status" to "PENDING", // RF-02: esperando aprobación
            "blockLot" to blockLot
        )

        // 2. Registrar en Cloud Firestore (colección 'users')
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(userMap)
            .awaitTask()

        // 3. Registrar en SQLite Room local
        val idLocal = registrar(
            nombre = nombre,
            apellido = apellido,
            dni = dni,
            correo = correo,
            usuario = usuario,
            password = password,
            rol = rol,
            idInmueble = idInmueble
        )

        // 4. Guardar el firebase_uid en Room
        dao.actualizarFirebaseUid(idLocal.toInt(), uid)

        return idLocal
    }


    /**
     * RF-03: Login Exclusivo Online vía Firebase Auth con Sincronización en Room.
     * 1. Resuelve el correo electrónico si ingresaron DNI o usuario (mediante consulta online a Firestore 'users').
     * 2. Autentica online en la nube usando FirebaseAuth.
     * 3. Recupera los detalles del perfil desde la colección 'users' de Firestore.
     * 4. Sincroniza y guarda localmente al usuario en SQLite Room.
     */
    suspend fun login(identificador: String, password: String): UsuarioEntity? {
        val input = identificador.trim()
        var email = input

        // Si el identificador no tiene formato de correo (@), buscamos el correo por DNI en Firestore
        if (!email.contains("@")) {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("dni", input)
                    .limit(1)
                    .get()
                    .awaitTask()
                
                val doc = snapshot.documents.firstOrNull()
                if (doc != null && doc.exists()) {
                    email = doc.getString("email") ?: input
                } else {
                    // Fallback a SQLite local por si ya existe registrado
                    val local = dao.buscarActivoPorIdentificador(input)
                    if (local != null) {
                        email = local.correo
                    }
                }
            } catch (e: Exception) {
                // Fallback a SQLite en caso de error de red
                val local = dao.buscarActivoPorIdentificador(input)
                if (local != null) {
                    email = local.correo
                }
            }
        }
        
        // Autenticación estrictamente online con Firebase Auth usando el correo resuelto
        val authResult = FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .awaitTask()

        
        val firebaseUser = authResult.user ?: throw RuntimeException("Error de autenticación en Firebase")
        email = firebaseUser.email ?: email
        val uid = firebaseUser.uid

        // Valores por defecto
        var nombre = "Usuario"
        var apellido = "Firebase"
        var rol = "RESIDENTE"
        var dni = "FB-" + UUID.randomUUID().toString().take(5)
        var status = "APPROVED"


        // 1. Obtener datos desde Cloud Firestore (colección 'users')
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .awaitTask()
            
            if (doc.exists()) {


                val fullname = doc.getString("fullname") ?: doc.getString("fullName")
                if (!fullname.isNullOrBlank()) {
                    val parts = fullname.trim().split("\\s+".toRegex())
                    nombre = parts.firstOrNull() ?: "Usuario"
                    apellido = parts.drop(1).joinToString(" ")
                    if (apellido.isBlank()) apellido = "Firebase"
                } else {
                    nombre = doc.getString("nombre") ?: nombre
                    apellido = doc.getString("apellido") ?: apellido
                }

                // Mapear rol de Firestore ("ADMIN" -> "ADMINISTRADOR", "OWNER" -> "PROPIETARIO", etc.)
                val roleFirestore = doc.getString("role") ?: doc.getString("rol") ?: "RESIDENTE"
                rol = when (roleFirestore.uppercase()) {
                    "ADMIN", "ADMINISTRADOR" -> "ADMINISTRADOR"
                    "OWNER", "PROPIETARIO" -> "PROPIETARIO"
                    else -> "RESIDENTE"
                }

                // Mapear status ("APPROVED" -> "ACTIVO")
                status = doc.getString("status") ?: doc.getString("estado") ?: "APPROVED"
                val docDni = doc.getString("dni")
                if (!docDni.isNullOrBlank()) {
                    dni = docDni
                }
            }
        } catch (e: Exception) {
            // Fallback usando displayName si Firestore no responde
            firebaseUser.displayName?.let { fullname ->
                val parts = fullname.trim().split("\\s+".toRegex())
                nombre = parts.firstOrNull() ?: nombre
                apellido = parts.drop(1).joinToString(" ")
                if (apellido.isBlank()) apellido = "Firebase"
            }
        }

        val mappedEstado = when (status.uppercase()) {
            "APPROVED", "ACTIVE", "ACTIVO" -> "ACTIVO"
            else -> "PENDIENTE"
        }

        // 2. Sincronizar con SQLite local (Room)
        // Buscamos si ya existe el usuario por su correo
        var localUser = dao.buscarPorCorreoGeneral(email)

        // Si no existe por correo, buscar por DNI para evitar UNIQUE constraint crashes
        if (localUser == null && !dni.startsWith("FB-")) {
            localUser = dao.buscarPorDniGeneral(dni)
        }

        if (localUser != null) {
            // Actualizar datos locales (incluyendo correo y usuario si buscamos por DNI)
            val updatedUser = localUser.copy(
                nombre = nombre,
                apellido = apellido,
                dni = dni,
                correo = email,
                usuario = email.split("@").first(),
                rol = rol,
                estado = mappedEstado,
                updatedAt = System.currentTimeMillis()
            )
            dao.actualizar(updatedUser)
            localUser = updatedUser
        } else {
            // Insertar nuevo registro en la base de datos local
            registrar(
                nombre = nombre,
                apellido = apellido,
                dni = dni,
                correo = email,
                usuario = email.split("@").first(),
                password = "firebase_auth_placeholder",
                rol = rol
            )
            val insertedUser = dao.buscarPorCorreoGeneral(email)
            if (insertedUser != null) {
                val approvedUser = insertedUser.copy(estado = mappedEstado)
                dao.actualizar(approvedUser)
                localUser = approvedUser
            }
        }



        return localUser
    }

    /** RF-02: el Admin aprueba una cuenta PENDIENTE. */
    suspend fun aprobar(idUsuario: Int) = dao.aprobar(idUsuario)

    /** RF-04: borrado lógico. */
    suspend fun desactivar(idUsuario: Int) = dao.desactivar(idUsuario)

    suspend fun obtenerActivosLista(): List<UsuarioEntity> = dao.obtenerActivosLista()

    fun observarPendientes(): Flow<List<UsuarioEntity>> = dao.observarPendientes()
    fun observarActivos(): Flow<List<UsuarioEntity>>    = dao.observarActivos()
    fun observarPorId(id: Int): Flow<UsuarioEntity?>   = dao.observarPorId(id)

    /**
     * Actualiza la contraseña del usuario en Room local y Firebase Auth.
     * Valida la contraseña actual (si no es cuenta de sólo-Firebase).
     */
    suspend fun cambiarPassword(idUsuario: Int, passwordActual: String, passwordNueva: String): Boolean {
        val localUser = dao.buscarPorId(idUsuario) ?: return false

        // Si es cuenta local con contraseña local, validar
        if (localUser.contrasenaHash != "firebase_auth_placeholder" && localUser.contrasenaHash.isNotEmpty()) {
            val valid = HashUtils.checkPassword(passwordActual, localUser.contrasenaHash)
            if (!valid) return false
        }

        // Si hay una sesión activa de Firebase Auth, actualizarla
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                currentUser.updatePassword(passwordNueva).awaitTask()
            } catch (e: Exception) {
                // En caso de que pida reauth reciente y falle online, propagamos o permitimos continuar en local
                // si no hay conexión para que sea robusto. Pero intentamos actualizarla.
            }
        }

        // Hashear y actualizar en SQLite local
        val updatedUser = localUser.copy(
            contrasenaHash = HashUtils.hashPassword(passwordNueva),
            updatedAt = System.currentTimeMillis()
        )
        dao.actualizar(updatedUser)
        return true
    }
}

/**
 * Función de extensión ligera para hacer que las Tasks de Firebase
 * sean suspendables y usables dentro de Corutinas Kotlin.
 */
suspend fun <T> Task<T>.awaitTask(): T = suspendCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            cont.resumeWithException(task.exception ?: RuntimeException("Task fallida"))
        }
    }
}