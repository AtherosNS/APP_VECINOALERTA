package com.upn.app_vecinoalerta.data.local.entities

import androidx.room.*

/**
 * RF-01: Registro dinámico según rol (PROPIETARIO / RESIDENTE).
 * RF-02: estado = "PENDIENTE" al crear; Admin lo aprueba manualmente.
 * RF-03: campo [rol] determina el panel destino tras login.
 * RF-04: NUNCA DELETE físico → estado = "INACTIVO" (borrado lógico).
 * RNF-03: [contrasenaHash] almacena solo el hash BCrypt.
 *
 * Campos de sincronización:
 * - [syncVersion] se incrementa en cada UPDATE local.
 * - [firebaseUid] es null hasta la primera sync exitosa con Firestore.
 */
@Entity(
    tableName = "usuarios",
    foreignKeys = [
        ForeignKey(
            entity = InmuebleEntity::class,
            parentColumns = ["id_inmueble"],
            childColumns = ["id_inmueble_asignado"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dni",    unique = true),
        Index("correo", unique = true),
        Index("usuario", unique = true),
        Index("id_inmueble_asignado"),
        Index("rol"),
        Index("estado")
    ]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_usuario")       val idUsuario: Int = 0,
    @ColumnInfo(name = "nombre")           val nombre: String,
    @ColumnInfo(name = "apellido")         val apellido: String,
    @ColumnInfo(name = "dni")              val dni: String,
    @ColumnInfo(name = "correo")           val correo: String,
    @ColumnInfo(name = "usuario")          val usuario: String,
    /** RNF-03: solo BCrypt hash, nunca texto plano. */
    @ColumnInfo(name = "contrasena_hash")  val contrasenaHash: String,
    /** "ADMINISTRADOR" | "PROPIETARIO" | "RESIDENTE" */
    @ColumnInfo(name = "rol")              val rol: String,
    /** "PENDIENTE" | "ACTIVO" | "INACTIVO" */
    @ColumnInfo(name = "estado", defaultValue = "PENDIENTE")
    val estado: String = "PENDIENTE",
    /** Null para PROPIETARIO; FK al inmueble elegido del Spinner para RESIDENTE. */
    @ColumnInfo(name = "id_inmueble_asignado")
    val idInmuebleAsignado: Int? = null,
    @ColumnInfo(name = "sync_version", defaultValue = "1")
    val syncVersion: Long = 1L,
    @ColumnInfo(name = "firebase_uid")     val firebaseUid: String? = null,
    @ColumnInfo(name = "created_at")       val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")       val updatedAt: Long = System.currentTimeMillis()
)