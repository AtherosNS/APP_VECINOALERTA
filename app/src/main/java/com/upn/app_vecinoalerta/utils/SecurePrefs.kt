package com.upn.app_vecinoalerta.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wrapper sobre EncryptedSharedPreferences.
 * Reemplaza el uso de getSharedPreferences("sesion", 0) en toda la app.
 * Cifra claves con AES256_SIV y valores con AES256_GCM.
 */
object SecurePrefs {

    private const val PREFS_NAME = "sesion_segura"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putInt(context: Context, key: String, value: Int) =
        getPrefs(context).edit().putInt(key, value).apply()

    fun putString(context: Context, key: String, value: String?) =
        getPrefs(context).edit().putString(key, value).apply()

    fun putBoolean(context: Context, key: String, value: Boolean) =
        getPrefs(context).edit().putBoolean(key, value).apply()

    fun getInt(context: Context, key: String, default: Int = -1): Int =
        getPrefs(context).getInt(key, default)

    fun getString(context: Context, key: String, default: String? = null): String? =
        getPrefs(context).getString(key, default)

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean =
        getPrefs(context).getBoolean(key, default)

    fun clear(context: Context) = getPrefs(context).edit().clear().apply()

    /** Guarda todos los campos de sesión de una vez */
    fun guardarSesion(context: Context, idUsuario: Int, rol: String, nombre: String, firebaseUid: String = "") {
        getPrefs(context).edit()
            .putInt("id_usuario", idUsuario)
            .putString("rol", rol)
            .putString("nombre", nombre)
            .putString("firebase_uid", firebaseUid)
            .apply()
    }

    fun limpiarSesion(context: Context) = clear(context)
}
