package com.upn.app_vecinoalerta.data.local.dao

import androidx.room.*
import com.upn.app_vecinoalerta.data.local.entities.*
import kotlinx.coroutines.flow.Flow

// ══════════════════════════════════════════════════════════════════
// USUARIO DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(u: UsuarioEntity): Long

    @Update
    suspend fun actualizar(u: UsuarioEntity)

    /** RF-04: borrado lógico. NUNCA llamar a deleteById(). */
    @Query("UPDATE usuarios SET estado='INACTIVO', updated_at=:ts, sync_version=sync_version+1 WHERE id_usuario=:id")
    suspend fun desactivar(id: Int, ts: Long = System.currentTimeMillis())

    /** RF-02: aprobación manual por el Admin. */
    @Query("UPDATE usuarios SET estado='ACTIVO', updated_at=:ts, sync_version=sync_version+1 WHERE id_usuario=:id AND estado='PENDIENTE'")
    suspend fun aprobar(id: Int, ts: Long = System.currentTimeMillis())

    /** RF-03: login — solo usuarios ACTIVOS. */
    @Query("SELECT * FROM usuarios WHERE usuario=:usuario AND estado='ACTIVO' LIMIT 1")
    suspend fun buscarActivoPorUsuario(usuario: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id_usuario=:id LIMIT 1")
    fun observarPorId(id: Int): Flow<UsuarioEntity?>

    /** RF-02: panel de aprobaciones del Admin. */
    @Query("SELECT * FROM usuarios WHERE estado='PENDIENTE' ORDER BY created_at ASC")
    fun observarPendientes(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE estado='ACTIVO' ORDER BY nombre ASC")
    fun observarActivos(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE estado='ACTIVO'")
    suspend fun obtenerActivosLista(): List<UsuarioEntity>

    /** Pendientes de subir a Firebase. */
    @Query("SELECT * FROM usuarios WHERE firebase_uid IS NULL OR sync_version > 1")
    suspend fun pendientesSincronizacion(): List<UsuarioEntity>

    @Query("UPDATE usuarios SET firebase_uid=:uid WHERE id_usuario=:id")
    suspend fun actualizarFirebaseUid(id: Int, uid: String)
}

// ══════════════════════════════════════════════════════════════════
// INMUEBLE DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface InmuebleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(i: InmuebleEntity): Long

    @Update
    suspend fun actualizar(i: InmuebleEntity)

    /** RF-01: Spinner del RESIDENTE — solo inmuebles activos. */
    @Query("SELECT * FROM inmuebles WHERE estado!='INACTIVO' ORDER BY direccion ASC")
    fun observarTodos(): Flow<List<InmuebleEntity>>

    @Query("SELECT * FROM inmuebles WHERE estado!='INACTIVO'")
    suspend fun obtenerTodosLista(): List<InmuebleEntity>

    @Query("SELECT * FROM inmuebles WHERE estado='DISPONIBLE' ORDER BY direccion ASC")
    fun observarDisponibles(): Flow<List<InmuebleEntity>>

    @Query("SELECT * FROM inmuebles WHERE id_propietario=:idPropietario AND estado!='INACTIVO'")
    fun observarPorPropietario(idPropietario: Int): Flow<List<InmuebleEntity>>

    @Query("SELECT * FROM inmuebles WHERE id_inmueble=:id LIMIT 1")
    suspend fun obtenerPorId(id: Int): InmuebleEntity?
}

// ══════════════════════════════════════════════════════════════════
// AVISO DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface AvisoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(a: AvisoEntity): Long

    @Update
    suspend fun actualizar(a: AvisoEntity)

    /** RF-07: Feed cronológico descendente. */
    @Query("SELECT * FROM avisos ORDER BY created_at DESC")
    fun observarTodos(): Flow<List<AvisoEntity>>
}

// ══════════════════════════════════════════════════════════════════
// ASAMBLEA DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface AsambleaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(a: AsambleaEntity): Long

    @Update
    suspend fun actualizar(a: AsambleaEntity)

    @Query("SELECT * FROM asambleas WHERE estado='PROGRAMADA' AND fecha_hora>=:ahora ORDER BY fecha_hora ASC")
    fun observarProximas(ahora: Long = System.currentTimeMillis()): Flow<List<AsambleaEntity>>

    @Query("SELECT * FROM asambleas ORDER BY fecha_hora DESC")
    fun observarTodas(): Flow<List<AsambleaEntity>>

    @Query("SELECT * FROM asambleas WHERE id_asamblea=:id LIMIT 1")
    suspend fun obtenerPorId(id: Int): AsambleaEntity?
}

// ══════════════════════════════════════════════════════════════════
// ENCUESTA DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface EncuestaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarEncuesta(e: EncuestaEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarOpcion(o: OpcionEncuestaEntity): Long

    /** RF-10: IGNORE = si ya existe el (encuesta,usuario) no lanza excepción. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarVoto(v: VotoEntity): Long

    @Update
    suspend fun actualizarEncuesta(e: EncuestaEntity)

    @Query("SELECT * FROM encuestas WHERE estado='ACTIVA' ORDER BY created_at DESC")
    fun observarActivas(): Flow<List<EncuestaEntity>>

    @Query("SELECT * FROM encuestas ORDER BY created_at DESC")
    fun observarTodas(): Flow<List<EncuestaEntity>>

    @Query("SELECT * FROM opciones_encuesta WHERE id_encuesta=:idEncuesta")
    fun observarOpciones(idEncuesta: Int): Flow<List<OpcionEncuestaEntity>>

    /** RF-10: devuelve > 0 si el usuario ya votó. */
    @Query("SELECT COUNT(*) FROM votos WHERE id_encuesta=:idE AND id_usuario=:idU")
    suspend fun contarVotosDelUsuario(idE: Int, idU: Int): Int

    @Query("""
        SELECT oe.id_opcion, oe.texto_opcion, COUNT(v.id_voto) AS total_votos
        FROM opciones_encuesta oe
        LEFT JOIN votos v ON oe.id_opcion = v.id_opcion
        WHERE oe.id_encuesta = :idEncuesta
        GROUP BY oe.id_opcion
    """)
    fun observarResultados(idEncuesta: Int): Flow<List<ResultadoOpcion>>
}

data class ResultadoOpcion(
    val id_opcion: Int,
    val texto_opcion: String,
    val total_votos: Int
)

// ══════════════════════════════════════════════════════════════════
// CHAT DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarGrupal(m: MensajeGrupalEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarPrivado(m: MensajePrivadoEntity): Long

    @Query("SELECT * FROM mensajes_grupales ORDER BY created_at ASC")
    fun observarGrupal(): Flow<List<MensajeGrupalEntity>>

    /** RF-11: hilo privado entre A y B, ambas direcciones. */
    @Query("""
        SELECT * FROM mensajes_privados
        WHERE (id_emisor=:a AND id_receptor=:b) OR (id_emisor=:b AND id_receptor=:a)
        ORDER BY created_at ASC
    """)
    fun observarHiloPrivado(a: Int, b: Int): Flow<List<MensajePrivadoEntity>>

    @Query("UPDATE mensajes_privados SET leido=1 WHERE id_receptor=:receptor AND id_emisor=:emisor AND leido=0")
    suspend fun marcarLeidos(receptor: Int, emisor: Int)

    @Query("SELECT COUNT(*) FROM mensajes_privados WHERE id_receptor=:id AND leido=0")
    fun observarNoLeidos(id: Int): Flow<Int>
}

// ══════════════════════════════════════════════════════════════════
// INCIDENCIA DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface IncidenciaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(i: IncidenciaEntity): Long

    /** RF-14: solo el Admin cambia el estado. */
    @Query("UPDATE incidencias SET estado=:estado, id_administrador=:idAdmin, updated_at=:ts, sync_version=sync_version+1 WHERE id_incidencia=:id")
    suspend fun actualizarEstado(id: Int, estado: String, idAdmin: Int, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM incidencias WHERE (:estado IS NULL OR estado=:estado) ORDER BY created_at DESC")
    fun observarPorEstado(estado: String? = null): Flow<List<IncidenciaEntity>>

    @Query("SELECT * FROM incidencias WHERE id_usuario=:idUsuario ORDER BY created_at DESC")
    fun observarDeUsuario(idUsuario: Int): Flow<List<IncidenciaEntity>>

    @Query("SELECT * FROM incidencias WHERE id_incidencia=:id LIMIT 1")
    fun observarPorId(id: Int): Flow<IncidenciaEntity?>
}

// ══════════════════════════════════════════════════════════════════
// FINANCIERO DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface FinancieroDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarCargo(c: CargoFinancieroEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarTransaccion(t: TransaccionEntity): Long

    @Query("UPDATE cargos_financieros SET estado_pago='PAGADO', updated_at=:ts, sync_version=sync_version+1 WHERE id_cargo=:id")
    suspend fun marcarPagado(id: Int, ts: Long = System.currentTimeMillis())

    /**
     * RF-15: estado de cuenta del morador.
     * Busca cargos asignados directamente al usuario O cargos del inmueble que ocupa
     * (cubre el caso en que el cargo fue creado antes de la aprobación del residente
     *  o con idUsuario=NULL pero referenciando su inmueble asignado).
     */
    @Query("""
        SELECT cf.* FROM cargos_financieros cf
        LEFT JOIN usuarios u ON u.id_usuario = :idU
        WHERE cf.id_usuario = :idU
           OR (u.id_inmueble_asignado IS NOT NULL AND cf.id_inmueble = u.id_inmueble_asignado)
        ORDER BY cf.anio DESC, cf.mes DESC
    """)
    fun observarCargosDeUsuario(idU: Int): Flow<List<CargoFinancieroEntity>>

    /** Observar todos los cargos (tanto pagados como pendientes) de las propiedades del propietario. */
    @Query("""
        SELECT cf.* FROM cargos_financieros cf
        INNER JOIN inmuebles i ON cf.id_inmueble = i.id_inmueble
        WHERE i.id_propietario=:idPropietario
        ORDER BY cf.anio DESC, cf.mes DESC
    """)
    fun observarCargosDePropietario(idPropietario: Int): Flow<List<CargoFinancieroEntity>>

    /**
     * RF-18: cargos del inmueble sin usuario asignado = deudas heredadas.
     * El propietario las ve como alerta en rojo.
     */
    @Query("SELECT * FROM cargos_financieros WHERE id_inmueble=:idI AND estado_pago!='PAGADO' AND id_usuario IS NULL ORDER BY anio ASC, mes ASC")
    fun observarDeudasHeredadas(idI: Int): Flow<List<CargoFinancieroEntity>>

    /** RF-18: toda deuda pendiente de los inmuebles de un propietario. */
    @Query("""
        SELECT cf.* FROM cargos_financieros cf
        INNER JOIN inmuebles i ON cf.id_inmueble = i.id_inmueble
        WHERE i.id_propietario=:idPropietario AND cf.estado_pago!='PAGADO'
        ORDER BY cf.anio ASC, cf.mes ASC
    """)
    fun observarDeudaTotalPropietario(idPropietario: Int): Flow<List<CargoFinancieroEntity>>

    /** RF-17: panel del Admin para cobros en caja. */
    @Query("SELECT * FROM cargos_financieros WHERE estado_pago='PENDIENTE' ORDER BY anio ASC, mes ASC")
    fun observarTodosPendientes(): Flow<List<CargoFinancieroEntity>>

    @Query("SELECT * FROM transacciones WHERE id_cargo=:idCargo ORDER BY created_at DESC")
    fun observarTransacciones(idCargo: Int): Flow<List<TransaccionEntity>>

    /** RF-16/17: inserta transacción Y marca cargo como PAGADO atómicamente. */
    @Transaction
    suspend fun procesarPago(t: TransaccionEntity) {
        insertarTransaccion(t)
        marcarPagado(t.idCargo)
    }
}

// ══════════════════════════════════════════════════════════════════
// EMERGENCIA DAO
// ══════════════════════════════════════════════════════════════════
@Dao
interface EmergenciaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(c: ContactoEmergenciaEntity): Long

    /** RF-05/RNF-05: sin red requerida. */
    @Query("SELECT * FROM contactos_emergencia WHERE activo=1 ORDER BY tipo ASC, nombre ASC")
    fun observarActivos(): Flow<List<ContactoEmergenciaEntity>>
}