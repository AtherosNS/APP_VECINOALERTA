package com.upn.app_vecinoalerta.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.upn.app_vecinoalerta.data.local.dao.*
import com.upn.app_vecinoalerta.data.local.entities.*
import com.upn.app_vecinoalerta.utils.HashUtils
import com.upn.app_vecinoalerta.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class IncidenciaRepository @Inject constructor(
    private val dao: IncidenciaDao,
    @ApplicationContext private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    /** RF-13: registra incidencia. [fotoPath] es la ruta del archivo copiado al storage interno. */
    suspend fun reportar(idUsuario: Int, categoria: String, descripcion: String, fotoPath: String?): Long =
        dao.insertar(IncidenciaEntity(idUsuario = idUsuario, categoria = categoria, descripcion = descripcion, fotoPath = fotoPath))

    /** RF-14: el Admin actualiza el estado. */
    suspend fun actualizarEstado(idIncidencia: Int, nuevoEstado: String, idAdmin: Int) {
        dao.actualizarEstado(idIncidencia, nuevoEstado, idAdmin)
        NotificationHelper.notificarIncidenciaActualizada(context, nuevoEstado, idIncidencia)
    }

    fun observarPorEstado(estado: String? = null): Flow<List<IncidenciaEntity>> = dao.observarPorEstado(estado)
    fun observarDeUsuario(idU: Int): Flow<List<IncidenciaEntity>>               = dao.observarDeUsuario(idU)
    fun observarPorId(id: Int): Flow<IncidenciaEntity?>                         = dao.observarPorId(id)

    /**
     * Sincroniza todas las incidencias pendientes a Firestore
     * y las marca como sincronizadas en Room.
     */
    suspend fun sincronizarPendientes() {
        val pendientes = dao.obtenerPendientesSync()
        for (inc in pendientes) {
            try {
                val map = hashMapOf(
                    "id_incidencia" to inc.idIncidencia,
                    "id_usuario" to inc.idUsuario,
                    "categoria" to inc.categoria,
                    "descripcion" to inc.descripcion,
                    "foto_path" to inc.fotoPath,
                    "estado" to inc.estado,
                    "created_at" to inc.createdAt
                )
                firestore.collection("incidencias").add(map).await()
                dao.marcarComoSincronizada(inc.idIncidencia)
            } catch (e: Exception) {
                // Se reintentará en la próxima ejecución del Worker
            }
        }
    }
}