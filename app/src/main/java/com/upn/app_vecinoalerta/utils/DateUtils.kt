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

object DateUtils {

    private val locale = Locale("es", "PE")

    fun epochToFechaHora(epochMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", locale).format(Date(epochMillis))

    fun epochToFecha(epochMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", locale).format(Date(epochMillis))

    fun epochToHora(epochMillis: Long): String =
        SimpleDateFormat("HH:mm", locale).format(Date(epochMillis))

    /** Devuelve "hace X minutos", "ayer", etc. para feeds de chat/avisos. */
    fun epochToRelativo(epochMillis: Long): String {
        val diff = System.currentTimeMillis() - epochMillis
        val minutos = diff / 60_000
        val horas   = diff / 3_600_000
        val dias    = diff / 86_400_000
        return when {
            minutos < 1  -> "ahora mismo"
            minutos < 60 -> "hace $minutos min"
            horas   < 24 -> "hace $horas h"
            dias    < 7  -> "hace $dias días"
            else         -> epochToFecha(epochMillis)
        }
    }

    fun mesYAnioLabel(mes: Int, anio: Int): String {
        val cal = Calendar.getInstance(locale)
        cal.set(Calendar.MONTH, mes - 1)
        return SimpleDateFormat("MMMM yyyy", locale).format(cal.time)
            .replaceFirstChar { it.uppercase() }
    }
}