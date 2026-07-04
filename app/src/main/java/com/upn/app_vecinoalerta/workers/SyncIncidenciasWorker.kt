package com.upn.app_vecinoalerta.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.upn.app_vecinoalerta.data.repository.IncidenciaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker periódico que sube a Firestore todas las incidencias con sync_pendiente = 1.
 * Se lanza automáticamente cada 15 minutos cuando hay red disponible.
 */
@HiltWorker
class SyncIncidenciasWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val incidenciaRepository: IncidenciaRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            incidenciaRepository.sincronizarPendientes()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
