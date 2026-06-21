package com.upn.app_vecinoalerta.data.local.entities

import androidx.room.*

// ── RF-06/07: AVISOS ──────────────────────────────────────────────────────────

@Entity(
    tableName = "avisos",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id_usuario"], ["id_administrador"],
        onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.CASCADE)],
    indices = [Index("id_administrador"), Index("created_at")]
)
data class AvisoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_aviso")          val idAviso: Int = 0,
    @ColumnInfo(name = "titulo")            val titulo: String,
    @ColumnInfo(name = "contenido")         val contenido: String,
    @ColumnInfo(name = "id_administrador")  val idAdministrador: Int,
    @ColumnInfo(name = "sync_version", defaultValue = "1") val syncVersion: Long = 1L,
    @ColumnInfo(name = "created_at")        val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")        val updatedAt: Long = System.currentTimeMillis()
)

// ── RF-08/09: ASAMBLEAS ───────────────────────────────────────────────────────

@Entity(
    tableName = "asambleas",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id_usuario"], ["id_administrador"],
        onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.CASCADE)],
    indices = [Index("id_administrador"), Index("fecha_hora")]
)
data class AsambleaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_asamblea")       val idAsamblea: Int = 0,
    @ColumnInfo(name = "titulo")            val titulo: String,
    @ColumnInfo(name = "agenda")            val agenda: String,
    /** Epoch millis de la fecha/hora de la reunión. */
    @ColumnInfo(name = "fecha_hora")        val fechaHora: Long,
    @ColumnInfo(name = "lugar")             val lugar: String,
    /** "PROGRAMADA" | "REALIZADA" | "CANCELADA" */
    @ColumnInfo(name = "estado", defaultValue = "PROGRAMADA") val estado: String = "PROGRAMADA",
    @ColumnInfo(name = "id_administrador")  val idAdministrador: Int,
    @ColumnInfo(name = "sync_version", defaultValue = "1") val syncVersion: Long = 1L,
    @ColumnInfo(name = "created_at")        val createdAt: Long = System.currentTimeMillis()
)

// ── RF-10: ENCUESTAS ──────────────────────────────────────────────────────────

@Entity(
    tableName = "encuestas",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id_usuario"], ["id_administrador"],
        onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.CASCADE)],
    indices = [Index("id_administrador"), Index("estado")]
)
data class EncuestaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_encuesta")       val idEncuesta: Int = 0,
    @ColumnInfo(name = "pregunta")          val pregunta: String,
    @ColumnInfo(name = "id_administrador")  val idAdministrador: Int,
    /** "BORRADOR" | "ACTIVA" | "CERRADA" */
    @ColumnInfo(name = "estado", defaultValue = "BORRADOR") val estado: String = "BORRADOR",
    @ColumnInfo(name = "fecha_cierre")      val fechaCierre: Long? = null,
    @ColumnInfo(name = "sync_version", defaultValue = "1") val syncVersion: Long = 1L,
    @ColumnInfo(name = "created_at")        val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "opciones_encuesta",
    foreignKeys = [ForeignKey(EncuestaEntity::class, ["id_encuesta"], ["id_encuesta"],
        onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE)],
    indices = [Index("id_encuesta")]
)
data class OpcionEncuestaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_opcion")     val idOpcion: Int = 0,
    @ColumnInfo(name = "id_encuesta")   val idEncuesta: Int,
    @ColumnInfo(name = "texto_opcion")  val textoOpcion: String
)

/**
 * RF-10: UNIQUE(id_encuesta, id_usuario) garantiza un voto por persona por encuesta.
 * El DAO usa INSERT OR IGNORE para manejar el intento de doble voto sin crash.
 */
@Entity(
    tableName = "votos",
    foreignKeys = [
        ForeignKey(EncuestaEntity::class,      ["id_encuesta"], ["id_encuesta"], ForeignKey.CASCADE, ForeignKey.CASCADE),
        ForeignKey(UsuarioEntity::class,       ["id_usuario"],  ["id_usuario"],  ForeignKey.RESTRICT, ForeignKey.CASCADE),
        ForeignKey(OpcionEncuestaEntity::class,["id_opcion"],   ["id_opcion"],   ForeignKey.RESTRICT, ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["id_encuesta", "id_usuario"], unique = true), // RF-10 constraint
        Index("id_opcion")
    ]
)
data class VotoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_voto")       val idVoto: Int = 0,
    @ColumnInfo(name = "id_encuesta")   val idEncuesta: Int,
    @ColumnInfo(name = "id_usuario")    val idUsuario: Int,
    @ColumnInfo(name = "id_opcion")     val idOpcion: Int,
    @ColumnInfo(name = "created_at")    val createdAt: Long = System.currentTimeMillis()
)