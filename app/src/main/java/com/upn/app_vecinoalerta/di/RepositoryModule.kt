package com.upn.app_vecinoalerta.di

import android.content.Context
import com.upn.app_vecinoalerta.data.local.VecinoAlertaDatabase
import com.upn.app_vecinoalerta.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Provee la instancia singleton de Room y todos los DAOs.
 * Los DAOs se inyectan directamente en los Repositories via constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): VecinoAlertaDatabase = VecinoAlertaDatabase.getInstance(context, scope)

    @Provides fun provideUsuarioDao(db: VecinoAlertaDatabase):    UsuarioDao    = db.usuarioDao()
    @Provides fun provideInmuebleDao(db: VecinoAlertaDatabase):   InmuebleDao   = db.inmuebleDao()
    @Provides fun provideAvisoDao(db: VecinoAlertaDatabase):      AvisoDao      = db.avisoDao()
    @Provides fun provideAsambleaDao(db: VecinoAlertaDatabase):   AsambleaDao   = db.asambleaDao()
    @Provides fun provideEncuestaDao(db: VecinoAlertaDatabase):   EncuestaDao   = db.encuestaDao()
    @Provides fun provideChatDao(db: VecinoAlertaDatabase):       ChatDao       = db.chatDao()
    @Provides fun provideIncidenciaDao(db: VecinoAlertaDatabase): IncidenciaDao = db.incidenciaDao()
    @Provides fun provideFinancieroDao(db: VecinoAlertaDatabase): FinancieroDao = db.financieroDao()
    @Provides fun provideEmergenciaDao(db: VecinoAlertaDatabase): EmergenciaDao = db.emergenciaDao()
}