package com.upn.app_vecinoalerta.data.local.entities

import androidx.room.*

/**
 * RF-01: La dirección de este inmueble aparece en el Spinner del RESIDENTE.
 * RF-18: Los cargos financieros quedan anclados aquí aunque el inquilino sea INACTIVO.
 */
@Entity(
    tableName = "inmuebles",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns  = ["id_propietario"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("id_propietario"), Index("estado")]
)
data class InmuebleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_inmueble")    val idInmueble: Int = 0,
    /** RF-01: Texto libre ingresado por el PROPIETARIO. Ej: "Torre A - Dpto 301" */
    @ColumnInfo(name = "direccion")      val direccion: String,
    @ColumnInfo(name = "descripcion")    val descripcion: String? = null,
    @ColumnInfo(name = "id_propietario") val idPropietario: Int,
    /** "DISPONIBLE" | "OCUPADO" | "INACTIVO" */
    @ColumnInfo(name = "estado", defaultValue = "DISPONIBLE")
    val estado: String = "DISPONIBLE",
    @ColumnInfo(name = "sync_version", defaultValue = "1")
    val syncVersion: Long = 1L,
    @ColumnInfo(name = "created_at")     val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")     val updatedAt: Long = System.currentTimeMillis()
)