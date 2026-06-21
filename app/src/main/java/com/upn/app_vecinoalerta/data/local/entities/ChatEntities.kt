package com.upn.app_vecinoalerta.data.local.entities

import androidx.room.*
/**
 * [nombreEmisorEstatico]: copia del nombre al momento del envío.
 * RF-11: Los mensajes conservan identidad aunque el usuario sea INACTIVO.
 * La FK es SET_NULL para que el mensaje no se borre con el usuario.
 */
@Entity(
    tableName = "mensajes_grupales",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id_usuario"], ["id_usuario"],
        onDelete = ForeignKey.SET_NULL, onUpdate = ForeignKey.CASCADE)],
    indices = [Index("id_usuario"), Index("created_at")]
)
data class MensajeGrupalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_mensaje")               val idMensaje: Int = 0,
    @ColumnInfo(name = "id_usuario")               val idUsuario: Int? = null,
    /** RF-11: Copia inmutable. Formato: "Juan Pérez (Propietario)" */
    @ColumnInfo(name = "nombre_emisor_estatico")   val nombreEmisorEstatico: String,
    @ColumnInfo(name = "contenido")                val contenido: String,
    @ColumnInfo(name = "created_at")               val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_version", defaultValue = "1") val syncVersion: Long = 1L
)

@Entity(
    tableName = "mensajes_privados",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_emisor"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_receptor"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("id_emisor"),
        Index("id_receptor"),
        Index("created_at")
    ]
)
data class MensajePrivadoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_mensaje")               val idMensaje: Int = 0,
    @ColumnInfo(name = "id_emisor")                val idEmisor: Int? = null,
    @ColumnInfo(name = "id_receptor")              val idReceptor: Int? = null,
    @ColumnInfo(name = "nombre_emisor_estatico")   val nombreEmisorEstatico: String,
    @ColumnInfo(name = "contenido")                val contenido: String,
    @ColumnInfo(name = "leido")                    val leido: Int = 0,
    @ColumnInfo(name = "created_at")               val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_version", defaultValue = "1") val syncVersion: Long = 1L
)