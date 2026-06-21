package com.upn.app_vecinoalerta

import android.app.Application
import com.upn.app_vecinoalerta.utils.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class.
 * @HiltAndroidApp arranca el grafo de dependencias de Hilt.
 * Aquí también se inicializan los canales de notificación (RF-09).
 */
@HiltAndroidApp
class VecinoAlertaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // RF-09: crear canales de notificación antes de cualquier notify()
        NotificationHelper.crearCanales(this)
    }
}