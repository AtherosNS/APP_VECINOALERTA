package com.upn.app_vecinoalerta

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.upn.app_vecinoalerta.utils.NotificationHelper
import com.upn.app_vecinoalerta.utils.SecurePrefs
import com.upn.app_vecinoalerta.workers.SyncIncidenciasWorker
import dagger.hilt.android.HiltAndroidApp
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.TimeUnit

/**
 * Application class.
 * @HiltAndroidApp arranca el grafo de dependencias de Hilt.
 * Aquí también se inicializan los canales de notificación (RF-09) y precalienta Flutter.
 */
@HiltAndroidApp
class VecinoAlertaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // RF-09: crear canales de notificación antes de cualquier notify()
        NotificationHelper.crearCanales(this)

        // Firestore sync: sincronizar incidencias pendientes cada 15 minutos con red
        val syncRequest = PeriodicWorkRequestBuilder<SyncIncidenciasWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync_incidencias",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // Precalentar FlutterEngine para carga instantánea de la UI de Flutter (0ms latencia)
        val flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )
        FlutterEngineCache.getInstance().put("my_flutter_engine", flutterEngine)

        // RF-15: Canal de comunicación bidireccional entre Flutter y Android Nativo
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.upn.vecinoalerta/channel")
            .setMethodCallHandler { call, result ->
                if (call.method == "getNativeUserInfo") {
                    val nombre    = SecurePrefs.getString(this, "nombre", "No autenticado") ?: "No autenticado"
                    val rol       = SecurePrefs.getString(this, "rol", "Ninguno") ?: "Ninguno"
                    val idUsuario = SecurePrefs.getInt(this, "id_usuario")
                    
                    val info = "👤 Usuario Nativo:\n• Nombre: $nombre\n• Rol: $rol\n• ID: $idUsuario\n\nSincronizado vía Platform Channel"
                    result.success(info)
                } else {
                    result.notImplemented()
                }
            }
    }
}