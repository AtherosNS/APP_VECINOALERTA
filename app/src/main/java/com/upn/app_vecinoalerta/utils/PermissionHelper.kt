package com.upn.app_vecinoalerta.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.mindrot.jbcrypt.BCrypt
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
object PermissionHelper {

    /** Permisos para el Botón de Pánico (RF-05). */
    val PANICO = arrayOf(Manifest.permission.CALL_PHONE)

    /** Permisos para reportar incidencias con foto (RF-13). */
    val CAMARA = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** Permisos de notificaciones (Android 13+). */
    val NOTIFICACIONES = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else emptyArray()

    fun tieneTodos(context: Context, permisos: Array<String>): Boolean =
        permisos.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
}