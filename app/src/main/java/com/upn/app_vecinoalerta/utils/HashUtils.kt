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
object HashUtils {

    /**
     * Genera el hash BCrypt de una contraseña en texto plano.
     * Usar en RegistroViewModel antes de persistir.
     */
    fun hashPassword(plainPassword: String): String =
        BCrypt.hashpw(plainPassword, BCrypt.gensalt(12))

    /**
     * Verifica que [plainPassword] corresponde al [hash] almacenado.
     * Usar en LoginViewModel.
     */
    fun checkPassword(plainPassword: String, hash: String): Boolean =
        BCrypt.checkpw(plainPassword, hash)
}