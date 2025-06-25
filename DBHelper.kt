package com.example.tripmates

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject

class DBHelper(context: Context) : SQLiteOpenHelper(context, "tripmates.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                correo TEXT UNIQUE NOT NULL,
                nombre TEXT,
                contrasena TEXT,
                fecha_actualizacion TEXT NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE viajes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT,
                destino TEXT,
                fecha_inicio TEXT,
                fecha_fin TEXT,
                creador_id INTEGER,
                fecha_actualizacion TEXT,
                FOREIGN KEY (creador_id) REFERENCES usuarios(id) ON DELETE SET NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE viajes_usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL,
                viaje_id INTEGER NOT NULL,
                rol TEXT NOT NULL CHECK(rol IN ('admin', 'participante')),
                fecha_actualizacion TEXT NOT NULL,
                UNIQUE(usuario_id, viaje_id),
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                FOREIGN KEY (viaje_id) REFERENCES viajes(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE gastos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_viaje INTEGER NOT NULL,
                id_usuario INTEGER NOT NULL,
                descripcion TEXT NOT NULL,
                monto REAL NOT NULL,
                fecha TEXT NOT NULL,
                fecha_actualizacion TEXT NOT NULL,
                FOREIGN KEY (id_viaje) REFERENCES viajes(id) ON DELETE CASCADE,
                FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE itinerario (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_viaje INTEGER NOT NULL,
                titulo TEXT NOT NULL,
                descripcion TEXT,
                fecha TEXT NOT NULL,
                hora TEXT,
                fecha_actualizacion TEXT NOT NULL,
                FOREIGN KEY (id_viaje) REFERENCES viajes(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE lista_empaque (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_viaje INTEGER NOT NULL,
                id_usuario INTEGER NOT NULL,
                item TEXT NOT NULL,
                marcado INTEGER NOT NULL DEFAULT 0,
                fecha_actualizacion TEXT NOT NULL,
                FOREIGN KEY(id_viaje) REFERENCES viajes(id) ON DELETE CASCADE,
                FOREIGN KEY(id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE
            )
        """.trimIndent())



    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS viajes_usuarios")
        db.execSQL("DROP TABLE IF EXISTS viajes")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    fun registrarUsuario(correo: String, nombre: String, contrasena: String, fechaActualizacion: String): Int {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT id FROM usuarios WHERE correo = ?", arrayOf(correo))
        if (cursor.moveToFirst()) {
            cursor.close()
            return -1
        }
        cursor.close()
        db.execSQL(
            "INSERT INTO usuarios (correo, nombre, contrasena, fecha_actualizacion) VALUES (?, ?, ?, ?)",
            arrayOf(correo, nombre, contrasena, fechaActualizacion)
        )
        val idCursor = db.rawQuery("SELECT last_insert_rowid()", null)
        val id = if (idCursor.moveToFirst()) idCursor.getInt(0) else -1
        idCursor.close()
        return id
    }

    fun loginUsuario(correo: String, contrasena: String): Usuario? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM usuarios WHERE correo = ? AND contrasena = ?", arrayOf(correo, contrasena))
        return if (cursor.moveToFirst()) {
            val usuario = Usuario(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                contrasena = cursor.getString(cursor.getColumnIndexOrThrow("contrasena")),
                fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
            )
            cursor.close()
            usuario
        } else {
            cursor.close()
            null
        }
    }


    fun crearViaje(
        nombre: String,
        destino: String,
        fechaInicio: String,
        fechaFin: String,
        creadorId: Int,
        fechaActualizacion: String = Utilidades.obtenerFechaActual()
    ): Long {
        val db = writableDatabase
        val valores = ContentValues().apply {
            put("nombre", nombre)
            put("destino", destino)
            put("fecha_inicio", fechaInicio)
            put("fecha_fin", fechaFin)
            put("creador_id", creadorId)
            put("fecha_actualizacion", fechaActualizacion)
        }
        val id = db.insert("viajes", null, valores)

        if (id != -1L) {
            val relacion = ContentValues().apply {
                put("usuario_id", creadorId)
                put("viaje_id", id.toInt())
                put("rol", "admin")
                put("fecha_actualizacion", fechaActualizacion)
            }
            db.insert("viajes_usuarios", null, relacion)
        }

        return id
    }





    fun actualizarViaje(viaje: Viaje) {
        val db = writableDatabase
        val valores = ContentValues().apply {
            put("nombre", viaje.nombre)
            put("destino", viaje.destino)
            put("fecha_inicio", viaje.fecha_inicio)
            put("fecha_fin", viaje.fecha_fin)
            put("creador_id", viaje.creador_id)
            put("fecha_actualizacion", Utilidades.obtenerFechaActual()) // ✅ actualiza siempre
        }
        db.update("viajes", valores, "id = ?", arrayOf(viaje.id.toString()))
    }


    fun eliminarViaje(viajeId: Int): Boolean {
        val db = writableDatabase
        db.execSQL("DELETE FROM viajes WHERE id = ?", arrayOf(viajeId))
        return true
    }


    fun agregarGasto(
        idViaje: Int,
        idUsuario: Int,
        descripcion: String,
        monto: Double,
        fecha: String,
        fechaActualizacion: String
    ) {
        val db = writableDatabase
        db.execSQL(
            "INSERT INTO gastos (id_viaje, id_usuario, descripcion, monto, fecha, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf(idViaje, idUsuario, descripcion, monto, fecha, fechaActualizacion)
        )
    }


    fun obtenerGastosPorViaje(idViaje: Int): List<Gasto> {
        val db = readableDatabase
        val lista = mutableListOf<Gasto>()
        val cursor = db.rawQuery("SELECT * FROM gastos WHERE id_viaje = ?", arrayOf(idViaje.toString()))
        while (cursor.moveToNext()) {
            lista.add(
                Gasto(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    id_viaje = cursor.getInt(cursor.getColumnIndexOrThrow("id_viaje")),
                    id_usuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario")),
                    descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    monto = cursor.getDouble(cursor.getColumnIndexOrThrow("monto")),
                    fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")),
                    fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
                )
            )
        }
        cursor.close()
        return lista
    }

    fun eliminarGasto(id: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM gastos WHERE id = ?", arrayOf(id))
    }


    fun editarGasto(gasto: Gasto) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE gastos SET descripcion = ?, monto = ?, fecha = ?, fecha_actualizacion = ? WHERE id = ?",
            arrayOf(gasto.descripcion, gasto.monto, gasto.fecha, gasto.fecha_actualizacion, gasto.id)
        )
    }

    fun insertarGasto(gasto: Gasto) {
        val db = writableDatabase
        val valores = ContentValues().apply {
            put("id", gasto.id)
            put("id_viaje", gasto.id_viaje)
            put("id_usuario", gasto.id_usuario)
            put("descripcion", gasto.descripcion)
            put("monto", gasto.monto)
            put("fecha", gasto.fecha)
            put("fecha_actualizacion", gasto.fecha_actualizacion)
        }
        db.insertWithOnConflict("gastos", null, valores, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun eliminarGastosDeViaje(idViaje: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM gastos WHERE id_viaje = ?", arrayOf(idViaje))
    }

    fun eliminarGastoRemoto(
        idGasto: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val json = JSONObject().apply { put("id", idGasto) }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(Constantes.BASE_URL + "eliminar_gasto.php")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EliminarGasto", "Error: ${e.message}")
                onError(e.message ?: "Error de conexión")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyStr = response.body?.string()
                    val json = JSONObject(bodyStr ?: "{}")
                    if (json.optBoolean("success")) {
                        onSuccess()
                    } else {
                        val errorMsg = json.optString("error", "Error desconocido")
                        onError(errorMsg)
                    }
                } catch (e: Exception) {
                    onError("Error al procesar respuesta")
                }
            }
        })
    }






    fun sincronizarRelacionIndividual(
        context: Context,
        usuarioId: Int,
        viajeId: Int,
        rol: String,
        fechaActualizacion: String,
        callback: (Boolean) -> Unit
    ) {
        val json = JSONObject().apply {
            put("usuario_id", usuarioId)
            put("viaje_id", viajeId)
            put("rol", rol)
            put("fecha_actualizacion", fechaActualizacion)
        }

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(Constantes.BASE_URL + "agregar_relacion.php")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyStr = response.body?.string()
                    val jsonResp = JSONObject(bodyStr ?: "{}")
                    callback(jsonResp.getBoolean("success"))
                } catch (e: Exception) {
                    callback(false)
                }
            }
        })
    }




    fun agregarUsuarioAViaje(
        context: Context,
        usuarioId: Int,
        viajeId: Int,
        rol: String = "participante",
        fechaActualizacion: String = Utilidades.obtenerFechaActual(),
        callback: (Boolean) -> Unit = {}
    ) {
        val db = DBHelper(context).writableDatabase
        try {
            db.execSQL(
                "INSERT OR REPLACE INTO viajes_usuarios (usuario_id, viaje_id, rol, fecha_actualizacion) VALUES (?, ?, ?, ?)",
                arrayOf(usuarioId, viajeId, rol, fechaActualizacion)
            )
            sincronizarRelacionIndividual(context, usuarioId, viajeId, rol, fechaActualizacion, callback)
        } catch (e: Exception) {
            Log.e("AgregarUsuario", "Error: ${e.message}")
            callback(false)
        }
    }




    fun usuarioYaEstaEnViaje(usuarioId: Int, viajeId: Int): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM viajes_usuarios WHERE usuario_id = ? AND viaje_id = ?",
            arrayOf(usuarioId.toString(), viajeId.toString())
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }
    fun obtenerUsuarioPorCorreo(correo: String): Usuario? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM usuarios WHERE correo = ?", arrayOf(correo))
        return if (cursor.moveToFirst()) {
            Usuario(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                contrasena = cursor.getString(cursor.getColumnIndexOrThrow("contrasena")),
                fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
            )
        } else {
            cursor.close()
            null
        }
    }

    fun agregarActividad(
        idViaje: Int,
        titulo: String,
        descripcion: String,
        fecha: String,
        hora: String?,
        fechaActualizacion: String
    ) {
        val db = writableDatabase
        db.execSQL(
            "INSERT INTO itinerario (id_viaje, titulo, descripcion, fecha, hora, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf(idViaje, titulo, descripcion, fecha, hora, fechaActualizacion)
        )
    }

    fun obtenerActividadesPorViaje(idViaje: Int): List<Actividad> {
        val db = readableDatabase
        val lista = mutableListOf<Actividad>()

        val cursor = db.rawQuery(
            "SELECT * FROM itinerario WHERE id_viaje = ? ORDER BY fecha, hora",
            arrayOf(idViaje.toString())
        )

        while (cursor.moveToNext()) {
            lista.add(
                Actividad(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    id_viaje = cursor.getInt(cursor.getColumnIndexOrThrow("id_viaje")),
                    titulo = cursor.getString(cursor.getColumnIndexOrThrow("titulo")),
                    descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
                    fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")),
                    hora = cursor.getString(cursor.getColumnIndexOrThrow("hora")),
                    fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
                )
            )
        }

        cursor.close()
        return lista
    }

    fun editarActividad(actividad: Actividad) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE itinerario SET titulo = ?, descripcion = ?, fecha = ?, hora = ?, fecha_actualizacion = ? WHERE id = ?",
            arrayOf(
                actividad.titulo,
                actividad.descripcion,
                actividad.fecha,
                actividad.hora,
                actividad.fecha_actualizacion,
                actividad.id
            )
        )
    }

    fun eliminarActividad(id: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM itinerario WHERE id = ?", arrayOf(id))
    }



    fun obtenerViajePorId(id: Int): Viaje? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM viajes WHERE id = ?", arrayOf(id.toString()))
        return if (cursor.moveToFirst()) {
            Viaje(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                destino = cursor.getString(cursor.getColumnIndexOrThrow("destino")),
                fecha_inicio = cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio")),
                fecha_fin = cursor.getString(cursor.getColumnIndexOrThrow("fecha_fin")),
                creador_id = cursor.getInt(cursor.getColumnIndexOrThrow("creador_id")),
                fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
            )
        } else {
            cursor.close()
            null
        }
    }

    fun insertarActividad(actividad: Actividad) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", actividad.id)
            put("id_viaje", actividad.id_viaje)
            put("titulo", actividad.titulo)
            put("descripcion", actividad.descripcion)
            put("fecha", actividad.fecha)
            put("hora", actividad.hora)
            put("fecha_actualizacion", actividad.fecha_actualizacion)
        }
        db.insertWithOnConflict("itinerario", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun eliminarItinerarioDeViaje(idViaje: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM itinerario WHERE id_viaje = ?", arrayOf(idViaje))
    }

    fun eliminarActividadRemota(
        idActividad: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val json = JSONObject().apply { put("id", idActividad) }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(Constantes.BASE_URL + "eliminar_actividad.php")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EliminarActividad", "Error: ${e.message}")
                onError(e.message ?: "Error de conexión")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyStr = response.body?.string()
                    val json = JSONObject(bodyStr ?: "{}")
                    if (json.optBoolean("success")) {
                        onSuccess()
                    } else {
                        val errorMsg = json.optString("error", "Error desconocido")
                        onError(errorMsg)
                    }
                } catch (e: Exception) {
                    onError("Error al procesar respuesta")
                }
            }
        })
    }




    fun agregarItemEmpaque(
        idViaje: Int,
        idUsuario: Int,
        item: String,
        fechaActualizacion: String
    ) {
        val db = writableDatabase
        db.execSQL(
            "INSERT INTO lista_empaque (id_viaje, id_usuario, item, marcado, fecha_actualizacion) VALUES (?, ?, ?, 0, ?)",
            arrayOf(idViaje, idUsuario, item, fechaActualizacion)
        )
    }
    fun obtenerItemsEmpaquePorViaje(idViaje: Int): List<ItemEmpaque> {
        val db = readableDatabase
        val lista = mutableListOf<ItemEmpaque>()

        val cursor = db.rawQuery(
            "SELECT * FROM lista_empaque WHERE id_viaje = ? ORDER BY id DESC",
            arrayOf(idViaje.toString())
        )

        while (cursor.moveToNext()) {
            lista.add(
                ItemEmpaque(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    id_viaje = cursor.getInt(cursor.getColumnIndexOrThrow("id_viaje")),
                    id_usuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario")),
                    item = cursor.getString(cursor.getColumnIndexOrThrow("item")),
                    marcado = cursor.getInt(cursor.getColumnIndexOrThrow("marcado")) == 1,
                    fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
                )
            )
        }

        cursor.close()
        return lista
    }

    fun actualizarItemEmpaqueMarcar(idItem: Int, marcado: Boolean, fechaActualizacion: String) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE lista_empaque SET marcado = ?, fecha_actualizacion = ? WHERE id = ?",
            arrayOf(if (marcado) 1 else 0, fechaActualizacion, idItem)
        )
    }

    fun eliminarItemEmpaque(idItem: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM lista_empaque WHERE id = ?", arrayOf(idItem))
    }

    fun insertarItemEmpaque(item: ItemEmpaque) {
        val db = writableDatabase
        val valores = ContentValues().apply {
            put("id", item.id)
            put("id_viaje", item.id_viaje)
            put("id_usuario", item.id_usuario)
            put("item", item.item)
            put("marcado", if (item.marcado) 1 else 0)
            put("fecha_actualizacion", item.fecha_actualizacion)
        }
        db.insertWithOnConflict("lista_empaque", null, valores, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun eliminarEmpaqueDeViaje(idViaje: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM lista_empaque WHERE id_viaje = ?", arrayOf(idViaje))
    }

    fun eliminarItemEmpaqueRemoto(
        idItem: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val json = JSONObject().apply { put("id", idItem) }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(Constantes.BASE_URL + "eliminar_empaque.php")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EliminarEmpaque", "Error: ${e.message}")
                onError(e.message ?: "Error de conexión")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyStr = response.body?.string()
                    val json = JSONObject(bodyStr ?: "{}")
                    if (json.optBoolean("success")) {
                        onSuccess()
                    } else {
                        val errorMsg = json.optString("error", "Error desconocido")
                        onError(errorMsg)
                    }
                } catch (e: Exception) {
                    onError("Error al procesar respuesta")
                }
            }
        })
    }




    fun insertarOActualizarViajeDesdeServidor(viaje: Viaje) {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT fecha_actualizacion FROM viajes WHERE id = ?", arrayOf(viaje.id.toString()))
        if (cursor.moveToFirst()) {
            val fechaLocal = cursor.getString(0)
            if (fechaLocal < viaje.fecha_actualizacion) {
                db.execSQL("""
                UPDATE viajes SET 
                    nombre = ?, destino = ?, fecha_inicio = ?, fecha_fin = ?, creador_id = ?, fecha_actualizacion = ?
                WHERE id = ?
            """.trimIndent(), arrayOf(
                    viaje.nombre, viaje.destino, viaje.fecha_inicio, viaje.fecha_fin,
                    viaje.creador_id, viaje.fecha_actualizacion, viaje.id
                ))
            }
        } else {
            db.execSQL("""
            INSERT INTO viajes (id, nombre, destino, fecha_inicio, fecha_fin, creador_id, fecha_actualizacion)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(), arrayOf(
                viaje.id, viaje.nombre, viaje.destino, viaje.fecha_inicio,
                viaje.fecha_fin, viaje.creador_id, viaje.fecha_actualizacion
            ))
        }
        cursor.close()
    }



    fun eliminarViajesDeUsuario(usuarioId: Int) {
        val db = writableDatabase
        db.delete("viajes", "creador_id = ?", arrayOf(usuarioId.toString()))
    }

    fun insertarViaje(viaje: Viaje, usuarioId: Int) {
        val db = writableDatabase

        // Insertar o reemplazar el viaje
        val valores = ContentValues().apply {
            put("id", viaje.id)
            put("nombre", viaje.nombre)
            put("destino", viaje.destino)
            put("fecha_inicio", viaje.fecha_inicio)
            put("fecha_fin", viaje.fecha_fin)
            put("creador_id", viaje.creador_id)
            put("fecha_actualizacion", viaje.fecha_actualizacion)
        }
        db.insertWithOnConflict("viajes", null, valores, SQLiteDatabase.CONFLICT_REPLACE)

        // Insertar la relación con el usuario (solo si no existe)
        val cursor = db.rawQuery(
            "SELECT * FROM viajes_usuarios WHERE usuario_id = ? AND viaje_id = ?",
            arrayOf(usuarioId.toString(), viaje.id.toString())
        )

        val yaExisteRelacion = cursor.moveToFirst()
        cursor.close()

        if (!yaExisteRelacion) {
            val valoresRelacion = ContentValues().apply {
                put("usuario_id", usuarioId)
                put("viaje_id", viaje.id)
                put("rol", "admin") // o "participante" según el caso
                put("fecha_actualizacion", viaje.fecha_actualizacion)
            }
            db.insert("viajes_usuarios", null, valoresRelacion)
        }
    }


    fun obtenerViajesPorUsuario(usuarioId: Int): List<Viaje> {
        val db = readableDatabase
        val lista = mutableListOf<Viaje>()

        val query = """
        SELECT v.* FROM viajes v
        INNER JOIN viajes_usuarios vu ON v.id = vu.viaje_id
        WHERE vu.usuario_id = ?
        ORDER BY v.fecha_inicio DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(usuarioId.toString()))
        while (cursor.moveToNext()) {
            lista.add(
                Viaje(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    destino = cursor.getString(cursor.getColumnIndexOrThrow("destino")),
                    fecha_inicio = cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio")),
                    fecha_fin = cursor.getString(cursor.getColumnIndexOrThrow("fecha_fin")),
                    creador_id = cursor.getInt(cursor.getColumnIndexOrThrow("creador_id")),
                    fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
                )
            )
        }
        cursor.close()
        return lista
    }


    fun insertarUsuarioLocal(usuario: Usuario) {
        val db = writableDatabase
        val valores = ContentValues().apply {
            put("id", usuario.id)
            put("correo", usuario.correo)
            put("nombre", usuario.nombre)
            put("contrasena", usuario.contrasena)
            put("fecha_actualizacion", usuario.fecha_actualizacion)
        }
        db.insertWithOnConflict("usuarios", null, valores, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun loginUsuarioPorId(id: Int): Usuario? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM usuarios WHERE id = ?", arrayOf(id.toString()))
        return if (cursor.moveToFirst()) {
            val usuario = Usuario(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                correo = cursor.getString(cursor.getColumnIndexOrThrow("correo")),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                contrasena = cursor.getString(cursor.getColumnIndexOrThrow("contrasena")),
                fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
            )
            cursor.close()
            usuario
        } else {
            cursor.close()
            null
        }


    }

    fun obtenerRelacionesDeUsuario(usuarioId: Int): List<RelacionViajeUsuario> {
        val db = readableDatabase
        val lista = mutableListOf<RelacionViajeUsuario>()
        val cursor = db.rawQuery(
            "SELECT * FROM viajes_usuarios WHERE usuario_id = ?",
            arrayOf(usuarioId.toString())
        )
        while (cursor.moveToNext()) {
            lista.add(
                RelacionViajeUsuario(
                    usuario_id = cursor.getInt(cursor.getColumnIndexOrThrow("usuario_id")),
                    viaje_id = cursor.getInt(cursor.getColumnIndexOrThrow("viaje_id")),
                    rol = cursor.getString(cursor.getColumnIndexOrThrow("rol")),
                    fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
                )
            )
        }
        cursor.close()
        return lista
    }

    fun reemplazarRelacionesDeUsuario(usuarioId: Int, jsonArray: JSONArray) {
        val db = writableDatabase
        db.delete("viajes_usuarios", "usuario_id = ?", arrayOf(usuarioId.toString()))

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val valores = ContentValues().apply {
                put("usuario_id", obj.getInt("usuario_id"))
                put("viaje_id", obj.getInt("viaje_id"))
                put("rol", obj.getString("rol"))
                put("fecha_actualizacion", obj.getString("fecha_actualizacion"))
            }
            db.insert("viajes_usuarios", null, valores)
        }
    }

    fun obtenerTodosLosViajesLocales(): List<Viaje> {
        val lista = mutableListOf<Viaje>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM viajes", null)

        while (cursor.moveToNext()) {
            lista.add(
                Viaje(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    destino = cursor.getString(cursor.getColumnIndexOrThrow("destino")),
                    fecha_inicio = cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio")),
                    fecha_fin = cursor.getString(cursor.getColumnIndexOrThrow("fecha_fin")),
                    creador_id = cursor.getInt(cursor.getColumnIndexOrThrow("creador_id")),
                    fecha_actualizacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion"))
                )
            )
        }
        cursor.close()
        return lista
    }

    fun buscarUsuarioPorCorreoRemoto(correo: String, callback: (Usuario?) -> Unit) {
        val json = JSONObject().apply { put("correo", correo) }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(Constantes.BASE_URL + "buscar_usuario_por_correo.php")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bodyStr = response.body?.string()
                    val jsonResp = JSONObject(bodyStr ?: "{}")

                    if (jsonResp.getBoolean("success")) {
                        val u = jsonResp.getJSONObject("usuario")
                        val usuario = Usuario(
                            id = u.getInt("id"),
                            correo = u.getString("correo"),
                            nombre = u.getString("nombre"),
                            contrasena = u.getString("contrasena"),
                            fecha_actualizacion = u.getString("fecha_actualizacion")
                        )
                        callback(usuario)
                    } else {
                        callback(null)
                    }
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }






















}
