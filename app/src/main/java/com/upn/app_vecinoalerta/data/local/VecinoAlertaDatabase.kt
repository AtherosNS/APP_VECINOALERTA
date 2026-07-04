package com.upn.app_vecinoalerta.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.upn.app_vecinoalerta.data.local.dao.*
import com.upn.app_vecinoalerta.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base de datos Room — Single Source of Truth local.
 *
 * Incluye TODAS las entidades del proyecto.
 * Al incrementar [version], agregar una [Migration] antes de publicar.
 *
 * exportSchema = true → genera JSON en /schemas para control de versiones.
 */
@Database(
    entities = [
        // Usuarios e Inmuebles
        UsuarioEntity::class,
        InmuebleEntity::class,
        // Comunicación
        AvisoEntity::class,
        AsambleaEntity::class,
        EncuestaEntity::class,
        OpcionEncuestaEntity::class,
        VotoEntity::class,
        // Chat
        MensajeGrupalEntity::class,
        MensajePrivadoEntity::class,
        // Incidencias
        IncidenciaEntity::class,
        // Finanzas
        CargoFinancieroEntity::class,
        TransaccionEntity::class,
        // Emergencia
        ContactoEmergenciaEntity::class,
    ],
    version = 6,
    exportSchema = true
)


abstract class VecinoAlertaDatabase : RoomDatabase() {

    // ── DAOs expuestos ─────────────────────────────────────────────
    abstract fun usuarioDao(): UsuarioDao
    abstract fun inmuebleDao(): InmuebleDao
    abstract fun avisoDao(): AvisoDao
    abstract fun asambleaDao(): AsambleaDao
    abstract fun encuestaDao(): EncuestaDao
    abstract fun chatDao(): ChatDao
    abstract fun incidenciaDao(): IncidenciaDao
    abstract fun financieroDao(): FinancieroDao
    abstract fun emergenciaDao(): EmergenciaDao

    companion object {

        @Volatile
        private var INSTANCE: VecinoAlertaDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): VecinoAlertaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    VecinoAlertaDatabase::class.java,
                    "vecino_alerta.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    // RNF-04: WAL mejora el rendimiento de lecturas concurrentes
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /** Agrega columna sync_pendiente a la tabla incidencias (v5 → v6). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE incidencias ADD COLUMN sync_pendiente INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }

    /**
     * Se ejecuta solo la PRIMERA vez que se crea la base de datos.
     * RF-05/RNF-05: pre-popula los contactos de emergencia locales de Laredo/Trujillo.
     */
    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            
            val passwordHash = com.upn.app_vecinoalerta.utils.HashUtils.hashPassword("admin")
            val ts = System.currentTimeMillis()
            
            db.beginTransaction()
            try {
                // 1. Insertar contactos de emergencia si no existen
                val cursorEmergencias = db.query("SELECT COUNT(*) FROM contactos_emergencia")
                cursorEmergencias.moveToFirst()
                val cantEmergencias = cursorEmergencias.getInt(0)
                cursorEmergencias.close()
                
                if (cantEmergencias == 0) {
                    val contactos = listOf(
                        Triple("Comisaría de Laredo", "044-461021", "POLICIA"),
                        Triple("PNP Central Trujillo", "044-249581", "POLICIA"),
                        Triple("Bomberos Trujillo Cía 29", "044-223333", "BOMBEROS"),
                        Triple("Serenazgo La Esperanza", "044-261600", "SERENAZGO"),
                        Triple("SAMU Trujillo", "106", "AMBULANCIA"),
                        Triple("Emergencias General", "105", "POLICIA")
                    )
                    for (c in contactos) {
                        db.execSQL("""
                            INSERT INTO contactos_emergencia (nombre, telefono, tipo, activo) 
                            VALUES ('${c.first}', '${c.second}', '${c.third}', 1)
                        """)
                    }
                }
                
                // 2. Verificar si el admin ya existe. Si no, insertarlo
                val cursorAdmin = db.query("SELECT id_usuario FROM usuarios WHERE usuario = 'admin' LIMIT 1")
                val adminExiste = cursorAdmin.count > 0
                cursorAdmin.close()
                
                if (!adminExiste) {
                    val cursorId1 = db.query("SELECT id_usuario FROM usuarios WHERE id_usuario = 1 LIMIT 1")
                    val id1Existe = cursorId1.count > 0
                    cursorId1.close()
                    
                    if (id1Existe) {
                        db.execSQL("""
                            INSERT INTO usuarios (nombre, apellido, dni, correo, usuario, contrasena_hash, rol, estado, sync_version, created_at, updated_at)
                            VALUES ('Administrador', 'Condominio', '00000000', 'admin@vecinoalerta.com', 'admin', '$passwordHash', 'ADMINISTRADOR', 'ACTIVO', 1, $ts, $ts)
                        """)
                    } else {
                        db.execSQL("""
                            INSERT INTO usuarios (id_usuario, nombre, apellido, dni, correo, usuario, contrasena_hash, rol, estado, sync_version, created_at, updated_at)
                            VALUES (1, 'Administrador', 'Condominio', '00000000', 'admin@vecinoalerta.com', 'admin', '$passwordHash', 'ADMINISTRADOR', 'ACTIVO', 1, $ts, $ts)
                        """)
                    }
                }
                
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                db.endTransaction()
            }
        }
    }
}