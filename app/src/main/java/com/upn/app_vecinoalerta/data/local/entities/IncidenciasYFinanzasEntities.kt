package com.upn.app_vecinoalerta.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "incidencias",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("id_usuario")]
)
data class IncidenciaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_incidencia")    val idIncidencia: Int = 0,
    @ColumnInfo(name = "id_usuario")       val idUsuario: Int,
    @ColumnInfo(name = "categoria")        val categoria: String,
    @ColumnInfo(name = "descripcion")      val descripcion: String,
    @ColumnInfo(name = "foto_path")        val fotoPath: String?,
    @ColumnInfo(name = "estado", defaultValue = "PENDIENTE")
    val estado: String = "PENDIENTE", // PENDIENTE, EN_PROCESO, RESUELTA
    @ColumnInfo(name = "id_administrador") val idAdministrador: Int? = null,
    @ColumnInfo(name = "created_at")       val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")       val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_version", defaultValue = "1")
    val syncVersion: Long = 1L
)

@Entity(
    tableName = "cargos_financieros",
    foreignKeys = [
        ForeignKey(
            entity = InmuebleEntity::class,
            parentColumns = ["id_inmueble"],
            childColumns = ["id_inmueble"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("id_inmueble"), Index("id_usuario")]
)
data class CargoFinancieroEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_cargo")         val idCargo: Int = 0,
    @ColumnInfo(name = "id_inmueble")      val idInmueble: Int?,
    @ColumnInfo(name = "id_usuario")       val idUsuario: Int?,
    @ColumnInfo(name = "concepto")         val concepto: String,
    @ColumnInfo(name = "monto")            val monto: Double,
    @ColumnInfo(name = "mes")              val mes: Int,
    @ColumnInfo(name = "anio")             val anio: Int,
    @ColumnInfo(name = "estado_pago")      val estadoPago: String = "PENDIENTE", // PENDIENTE, PAGADO, ATRASADO
    @ColumnInfo(name = "id_administrador") val idAdministrador: Int? = null,
    @ColumnInfo(name = "created_at")       val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")       val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_version", defaultValue = "1")
    val syncVersion: Long = 1L
)

@Entity(
    tableName = "transacciones",
    foreignKeys = [
        ForeignKey(
            entity = CargoFinancieroEntity::class,
            parentColumns = ["id_cargo"],
            childColumns = ["id_cargo"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id_usuario"],
            childColumns = ["id_usuario"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("id_cargo"), Index("id_usuario")]
)
data class TransaccionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_transaccion")   val idTransaccion: Int = 0,
    @ColumnInfo(name = "id_cargo")         val idCargo: Int,
    @ColumnInfo(name = "id_usuario")       val idUsuario: Int?,
    @ColumnInfo(name = "metodo_pago")      val metodoPago: String, // VISA, MASTERCARD, YAPE, PLIN, EFECTIVO
    @ColumnInfo(name = "referencia_pago")  val referenciaPago: String?,
    @ColumnInfo(name = "estado", defaultValue = "COMPLETADA")
    val estado: String = "COMPLETADA", // COMPLETADA, FALLIDA
    @ColumnInfo(name = "monto_pagado")     val montoPagado: Double,
    @ColumnInfo(name = "created_at")       val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_version", defaultValue = "1")
    val syncVersion: Long = 1L
)

@Entity(tableName = "contactos_emergencia")
data class ContactoEmergenciaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_contacto")      val idContacto: Int = 0,
    @ColumnInfo(name = "nombre")           val nombre: String,
    @ColumnInfo(name = "telefono")         val telefono: String,
    @ColumnInfo(name = "tipo")             val tipo: String, // SERENO, BOMBEROS, POLICIA
    @ColumnInfo(name = "activo")           val activo: Boolean = true
)
