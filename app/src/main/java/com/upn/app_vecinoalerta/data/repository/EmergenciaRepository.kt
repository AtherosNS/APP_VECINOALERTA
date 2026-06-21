package com.upn.app_vecinoalerta.data.repository

import android.content.Context
import com.upn.app_vecinoalerta.data.local.dao.*
import com.upn.app_vecinoalerta.data.local.entities.*
import com.upn.app_vecinoalerta.utils.HashUtils
import com.upn.app_vecinoalerta.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class EmergenciaRepository @Inject constructor(
    private val dao: EmergenciaDao
) {
    /** RF-05/RNF-05: funciona 100% offline. */
    fun observarContactos(): Flow<List<ContactoEmergenciaEntity>> = dao.observarActivos()
}