package com.upn.app_vecinoalerta.utils

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.upn.app_vecinoalerta.data.repository.UsuarioRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker para sincronización en background con Firebase (Fase 2).
 *
 * Arquitectura Local-First:
 * 1. Toda escritura va primero a Room (Single Source of Truth).
 * 2. Este worker sube los registros pendientes a Firestore cuando hay red.
 * 3. Un registro está "pendiente" si firebase_uid IS NULL (nunca sincronizado)
 *    o sync_version > 1 (modificado localmente después de la última sync).
 *
 * En Fase 1 (solo SQLite): este worker no hace nada dañino;
 * simplemente no encuentra registros que subir porque Firebase no está configurado.
 * Se puede dejar registrado en VecinoAlertaApp sin problemas.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val usuarioRepo: UsuarioRepository
    // En Fase 2: inyectar FirebaseFirestore y los demás repos aquí
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Fase 2: descomentar y completar con la lógica de Firestore
            // val pendientes = usuarioRepo.pendientesSincronizacion()
            // pendientes.forEach { usuario ->
            //     val doc = firestore.collection("usuarios").document()
            //     doc.set(usuario.toFirestoreMap()).await()
            //     usuarioRepo.actualizarFirebaseUid(usuario.idUsuario, doc.id)
            // }
            Result.success()
        } catch (e: Exception) {
            // Reintenta hasta 3 veces con backoff exponencial
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "vecino_alerta_sync"

        /**
         * Programa la sincronización periódica cada 15 minutos cuando hay WiFi o datos.
         * Llamar en VecinoAlertaApp.onCreate() para la Fase 2.
         */
        fun programar(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}