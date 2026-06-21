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
object NotificationHelper {

    const val CHANNEL_ASAMBLEAS  = "canal_asambleas"
    const val CHANNEL_INCIDENCIAS = "canal_incidencias"
    const val CHANNEL_PAGOS       = "canal_pagos"

    /** Llamar en VecinoAlertaApp.onCreate() */
    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            listOf(
                NotificationChannel(CHANNEL_ASAMBLEAS,   "Asambleas y reuniones", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_INCIDENCIAS, "Incidencias",            NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_PAGOS,       "Pagos y cargos",         NotificationManager.IMPORTANCE_DEFAULT),
            ).forEach { manager.createNotificationChannel(it) }
        }
    }

    /** RF-09: notificación al crear/modificar una asamblea. */
    fun notificarAsamblea(context: Context, titulo: String, detalle: String, id: Int) {
        if (!tienePermisoNotificaciones(context)) return
        val notif = NotificationCompat.Builder(context, CHANNEL_ASAMBLEAS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📋 Nueva asamblea: $titulo")
            .setContentText(detalle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notif)
    }

    fun notificarIncidenciaActualizada(context: Context, estado: String, id: Int) {
        if (!tienePermisoNotificaciones(context)) return
        val notif = NotificationCompat.Builder(context, CHANNEL_INCIDENCIAS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Incidencia actualizada")
            .setContentText("Tu reporte pasó a estado: $estado")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id + 1000, notif)
    }

    fun notificarNuevoPago(context: Context, concepto: String, monto: Double, id: Int) {
        if (!tienePermisoNotificaciones(context)) return
        val formattedMonto = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(monto)
        val notif = NotificationCompat.Builder(context, CHANNEL_PAGOS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("💰 Nuevo cargo financiero")
            .setContentText("$concepto - Importe: $formattedMonto")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id + 2000, notif)
    }

    private fun tienePermisoNotificaciones(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}