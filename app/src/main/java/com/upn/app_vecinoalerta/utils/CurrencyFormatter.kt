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
object CurrencyFormatter {

    private val format = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    /** Ej: 150.5 → "S/ 150.50" */
    fun formatear(monto: Double): String = format.format(monto)
}