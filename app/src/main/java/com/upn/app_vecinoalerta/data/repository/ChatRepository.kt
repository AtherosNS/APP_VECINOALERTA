package com.upn.app_vecinoalerta.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.upn.app_vecinoalerta.data.local.dao.ChatDao
import com.upn.app_vecinoalerta.data.local.entities.MensajeGrupalEntity
import com.upn.app_vecinoalerta.data.local.entities.MensajePrivadoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val dao: ChatDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Tracks which user IDs already have active private listeners registered.
    // Using a Set<Int> allows re-initialization when the logged-in user changes.
    private val escuchadoresPrivadosActivos = mutableSetOf<Int>()
    private var escuchadorGrupalActivo = false

    /** RF-11: envía grupal a Room local y sincroniza en Firestore. */
    suspend fun enviarGrupal(idUsuario: Int, nombreCompleto: String, rol: String, contenido: String): Long {
        val nombreEstatico = "$nombreCompleto ($rol)"
        val timestamp = System.currentTimeMillis()

        val idLocal = dao.insertarGrupal(
            MensajeGrupalEntity(
                idUsuario = idUsuario,
                nombreEmisorEstatico = nombreEstatico,
                contenido = contenido,
                createdAt = timestamp
            )
        )

        val map = hashMapOf(
            "id_usuario" to idUsuario,
            "nombre_emisor_estatico" to nombreEstatico,
            "contenido" to contenido,
            "created_at" to timestamp
        )
        firestore.collection("mensajes_grupales").add(map)

        return idLocal
    }

    /** RF-11: envía privado a Room local y sincroniza en Firestore. */
    suspend fun enviarPrivado(idEmisor: Int, idReceptor: Int, nombreCompleto: String, rol: String, contenido: String): Long {
        val nombreEstatico = "$nombreCompleto ($rol)"
        val timestamp = System.currentTimeMillis()

        val idLocal = dao.insertarPrivado(
            MensajePrivadoEntity(
                idEmisor = idEmisor,
                idReceptor = idReceptor,
                nombreEmisorEstatico = nombreEstatico,
                contenido = contenido,
                createdAt = timestamp
            )
        )

        val map = hashMapOf(
            "id_emisor" to idEmisor,
            "id_receptor" to idReceptor,
            "nombre_emisor_estatico" to nombreEstatico,
            "contenido" to contenido,
            "leido" to 0,
            "created_at" to timestamp
        )
        firestore.collection("mensajes_privados").add(map)

        return idLocal
    }

    fun observarChatGrupal(): Flow<List<MensajeGrupalEntity>>               = dao.observarGrupal()
    fun observarHiloPrivado(a: Int, b: Int): Flow<List<MensajePrivadoEntity>> = dao.observarHiloPrivado(a, b)
    fun observarNoLeidos(id: Int): Flow<Int>                                = dao.observarNoLeidos(id)
    suspend fun marcarLeidos(receptor: Int, emisor: Int)                    = dao.marcarLeidos(receptor, emisor)

    /** Inicia el escuchador en tiempo real para mensajes grupales de Firestore a Room. */
    fun iniciarEscuchadorGrupal() {
        if (escuchadorGrupalActivo) return
        escuchadorGrupalActivo = true

        firestore.collection("mensajes_grupales")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                repositoryScope.launch {
                    for (doc in snapshots.documents) {
                        val idUsuario = doc.getLong("id_usuario")?.toInt() ?: continue
                        val nombre = doc.getString("nombre_emisor_estatico") ?: ""
                        val contenido = doc.getString("contenido") ?: ""
                        val timestamp = doc.getLong("created_at") ?: continue

                        if (dao.contarMensajeGrupal(timestamp, idUsuario) == 0) {
                            dao.insertarGrupal(
                                MensajeGrupalEntity(
                                    idUsuario = idUsuario,
                                    nombreEmisorEstatico = nombre,
                                    contenido = contenido,
                                    createdAt = timestamp
                                )
                            )
                        }
                    }
                }
            }
    }

    /**
     * Inicia los escuchadores en tiempo real para mensajes privados de Firestore a Room.
     *
     * Keyed by [currentUserId]: if this user ID was already registered, this is a no-op.
     * When the logged-in user changes (different ID), new listeners are registered correctly
     * because the old ID is not in [escuchadoresPrivadosActivos].
     */
    fun iniciarEscuchadoresPrivados(currentUserId: Int) {
        if (currentUserId <= 0) return
        // Guard is now per-user-ID, not a global boolean — fixes re-init when user changes.
        if (!escuchadoresPrivadosActivos.add(currentUserId)) return

        // 1. Mensajes donde el usuario actual es RECEPTOR
        firestore.collection("mensajes_privados")
            .whereEqualTo("id_receptor", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                repositoryScope.launch {
                    for (doc in snapshots.documents) {
                        val idEmisor = doc.getLong("id_emisor")?.toInt() ?: continue
                        val idReceptor = doc.getLong("id_receptor")?.toInt() ?: continue
                        val nombre = doc.getString("nombre_emisor_estatico") ?: ""
                        val contenido = doc.getString("contenido") ?: ""
                        val timestamp = doc.getLong("created_at") ?: continue
                        val leido = doc.getLong("leido")?.toInt() ?: 0

                        if (dao.contarMensajePrivado(timestamp, idEmisor, idReceptor) == 0) {
                            dao.insertarPrivado(
                                MensajePrivadoEntity(
                                    idEmisor = idEmisor,
                                    idReceptor = idReceptor,
                                    nombreEmisorEstatico = nombre,
                                    contenido = contenido,
                                    leido = leido,
                                    createdAt = timestamp
                                )
                            )
                        }
                    }
                }
            }

        // 2. Mensajes donde el usuario actual es EMISOR (sincronizar desde otros dispositivos)
        firestore.collection("mensajes_privados")
            .whereEqualTo("id_emisor", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                repositoryScope.launch {
                    for (doc in snapshots.documents) {
                        val idEmisor = doc.getLong("id_emisor")?.toInt() ?: continue
                        val idReceptor = doc.getLong("id_receptor")?.toInt() ?: continue
                        val nombre = doc.getString("nombre_emisor_estatico") ?: ""
                        val contenido = doc.getString("contenido") ?: ""
                        val timestamp = doc.getLong("created_at") ?: continue
                        val leido = doc.getLong("leido")?.toInt() ?: 0

                        if (dao.contarMensajePrivado(timestamp, idEmisor, idReceptor) == 0) {
                            dao.insertarPrivado(
                                MensajePrivadoEntity(
                                    idEmisor = idEmisor,
                                    idReceptor = idReceptor,
                                    nombreEmisorEstatico = nombre,
                                    contenido = contenido,
                                    leido = leido,
                                    createdAt = timestamp
                                )
                            )
                        }
                    }
                }
            }
    }
}