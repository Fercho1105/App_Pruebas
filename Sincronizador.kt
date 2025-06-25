package com.example.tripmates

import android.content.Context
import android.util.Log
import com.example.tripmates.Viaje
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

fun sincronizarViajes(context: Context, idUsuario: Int) {
    val db = DBHelper(context)
    val listaViajes = db.obtenerTodosLosViajesLocales() // ✅ Ahora obtiene todos los viajes

    val jsonArray = JSONArray()
    for (viaje in listaViajes) {
        val obj = JSONObject()
        obj.put("id", viaje.id)
        obj.put("nombre", viaje.nombre)
        obj.put("destino", viaje.destino)
        obj.put("fecha_inicio", viaje.fecha_inicio)
        obj.put("fecha_fin", viaje.fecha_fin)
        obj.put("creador_id", viaje.creador_id)
        obj.put("fecha_actualizacion", viaje.fecha_actualizacion)
        jsonArray.put(obj)
    }

    val data = JSONObject()
    data.put("usuario_id", idUsuario)
    data.put("viajes", jsonArray)

    val body = data.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(Constantes.BASE_URL + "sincronizar_viajes.php")

        .post(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SincronizarViajes", "Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string()
            val jsonTexto = bodyStr?.trimStart()?.dropWhile { it != '{' } ?: "{}"
            try {
                val json = JSONObject(jsonTexto)
                if (json.getBoolean("success")) {
                    val viajesServidor = json.getJSONArray("viajes")

                    db.eliminarViajesDeUsuario(idUsuario)

                    for (i in 0 until viajesServidor.length()) {
                        val v = viajesServidor.getJSONObject(i)
                        val viaje = Viaje(
                            id = v.getInt("id"),
                            nombre = v.getString("nombre"),
                            destino = v.getString("destino"),
                            fecha_inicio = v.getString("fecha_inicio"),
                            fecha_fin = v.getString("fecha_fin"),
                            creador_id = v.getInt("creador_id"),
                            fecha_actualizacion = v.getString("fecha_actualizacion")
                        )
                        db.insertarViaje(viaje, idUsuario)
                    }
                    Log.d("SincronizarViajes", "JSON enviado: $data")
                    Log.d("SincronizarViajes", "Sincronización exitosa (${viajesServidor.length()} viajes)")
                } else {
                    Log.e("SincronizarViajes", "Fallo en servidor: ${json.optString("error")}")
                }
            } catch (e: Exception) {
                Log.e("SincronizarViajes", "Error al parsear JSON: ${e.message}")
            }
        }
    })
}


fun eliminarViajeRemoto(
    idViaje: Int,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val json = JSONObject().apply { put("id", idViaje) }
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

    val request = Request.Builder()
        .url(Constantes.BASE_URL + "eliminar_viaje.php")

        .post(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("EliminarViajeRemoto", "Error: ${e.message}")
            onError(e.message ?: "Error de conexión")
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val bodyStr = response.body?.string()
                val json = JSONObject(bodyStr ?: "{}")
                if (json.getBoolean("success")) {
                    Log.d("EliminarViajeRemoto", "Viaje eliminado remotamente")
                    onSuccess()
                } else {
                    val errorMsg = json.optString("error", "Error desconocido")
                    Log.e("EliminarViajeRemoto", "Error del servidor: $errorMsg")
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("EliminarViajeRemoto", "Error al parsear respuesta: ${e.message}")
                onError("Error al procesar respuesta del servidor")
            }
        }
    })
}
fun sincronizarViajesUsuarios(context: Context, usuarioId: Int) {
    val db = DBHelper(context)
    val relacionesLocales = db.obtenerRelacionesDeUsuario(usuarioId)

    val json = JSONObject().apply {
        put("usuario_id", usuarioId)
        put("relaciones", JSONArray().apply {
            for (relacion in relacionesLocales) {
                put(JSONObject().apply {
                    put("usuario_id", relacion.usuario_id)
                    put("viaje_id", relacion.viaje_id)
                    put("rol", relacion.rol)
                    put("fecha_actualizacion", relacion.fecha_actualizacion)
                })
            }
        })
    }

    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(Constantes.BASE_URL + "sincronizar_viajes_usuarios.php")

        .post(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SincronizarViajesUsuarios", "Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string()
            try {
                val jsonResp = JSONObject(bodyStr ?: "{}")
                if (jsonResp.optBoolean("success")) {
                    val relaciones = jsonResp.getJSONArray("relaciones")
                    db.reemplazarRelacionesDeUsuario(usuarioId, relaciones)
                    Log.d("SincronizarViajesUsuarios", "Respuesta del servidor: $bodyStr")
                    Log.d("SincronizarViajesUsuarios", "Sincronización exitosa (${relaciones.length()} relaciones)")
                } else {
                    Log.d("SincronizarViajesUsuarios", "Respuesta del servidor: $bodyStr")
                    Log.e("SincronizarViajesUsuarios", "Error del servidor: ${jsonResp.optString("error")}")
                }
            } catch (e: Exception) {
                Log.e("SincronizarViajesUsuarios", "Error al parsear JSON: ${e.message}")
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
    callback: (Boolean) -> Unit = {}
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
            Log.e("RelacionRemota", "Error: ${e.message}")
            callback(false)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val bodyStr = response.body?.string()
                val jsonResp = JSONObject(bodyStr ?: "{}")
                if (jsonResp.optBoolean("success")) {
                    Log.d("RelacionRemota", "Relación sincronizada con éxito")
                    callback(true)
                } else {
                    Log.e("RelacionRemota", "Fallo: ${jsonResp.optString("error")}")
                    callback(false)
                }
            } catch (e: Exception) {
                Log.e("RelacionRemota", "Excepción: ${e.message}")
                callback(false)
            }
        }
    })
}

fun sincronizarItinerario(context: Context, idViaje: Int) {
    val db = DBHelper(context)
    val actividadesLocales = db.obtenerActividadesPorViaje(idViaje)

    val jsonArray = JSONArray()
    for (a in actividadesLocales) {
        val obj = JSONObject().apply {
            put("id", a.id)
            put("id_viaje", a.id_viaje)
            put("titulo", a.titulo)
            put("descripcion", a.descripcion)
            put("fecha", a.fecha)
            put("hora", a.hora ?: "")
            put("fecha_actualizacion", a.fecha_actualizacion)
        }
        jsonArray.put(obj)
    }

    val data = JSONObject().apply {
        put("id_viaje", idViaje)
        put("itinerario", jsonArray)
    }

    val body = data.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(Constantes.BASE_URL + "sincronizar_itinerario.php")

        .post(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SincronizarItinerario", "Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string()
            val jsonTexto = bodyStr?.trimStart()?.dropWhile { it != '{' } ?: "{}"
            try {
                val json = JSONObject(jsonTexto)
                if (json.getBoolean("success")) {
                    val actividadesServidor = json.getJSONArray("itinerario")

                    db.eliminarItinerarioDeViaje(idViaje)
                    for (i in 0 until actividadesServidor.length()) {
                        val a = actividadesServidor.getJSONObject(i)
                        val actividad = Actividad(
                            id = a.getInt("id"),
                            id_viaje = a.getInt("id_viaje"),
                            titulo = a.getString("titulo"),
                            descripcion = a.getString("descripcion"),
                            fecha = a.getString("fecha"),
                            hora = a.optString("hora"),
                            fecha_actualizacion = a.getString("fecha_actualizacion")
                        )
                        db.insertarActividad(actividad)
                    }
                    Log.d("SincronizarItinerario", "Itinerario sincronizado: ${actividadesServidor.length()} actividades")
                } else {
                    Log.e("SincronizarItinerario", "Error del servidor: ${json.optString("error")}")
                }
            } catch (e: Exception) {
                Log.e("SincronizarItinerario", "Excepción: ${e.message}")
            }
        }
    })
}

fun sincronizarGastos(context: Context, idViaje: Int) {
    val db = DBHelper(context)
    val listaLocales = db.obtenerGastosPorViaje(idViaje)

    val jsonArray = JSONArray()
    for (g in listaLocales) {
        val obj = JSONObject().apply {
            put("id", g.id)
            put("id_viaje", g.id_viaje)
            put("id_usuario", g.id_usuario)
            put("descripcion", g.descripcion)
            put("monto", g.monto)
            put("fecha", g.fecha)
            put("fecha_actualizacion", g.fecha_actualizacion)
        }
        jsonArray.put(obj)
    }

    val data = JSONObject().apply {
        put("id_viaje", idViaje)
        put("gastos", jsonArray)
    }

    val body = data.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(Constantes.BASE_URL + "sincronizar_gastos.php")

        .post(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SincronizarGastos", "Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string()
            val jsonTexto = bodyStr?.trimStart()?.dropWhile { it != '{' } ?: "{}"
            try {
                val json = JSONObject(jsonTexto)
                if (json.getBoolean("success")) {
                    val array = json.getJSONArray("gastos")
                    db.eliminarGastosDeViaje(idViaje)
                    for (i in 0 until array.length()) {
                        val g = array.getJSONObject(i)
                        val gasto = Gasto(
                            id = g.getInt("id"),
                            id_viaje = g.getInt("id_viaje"),
                            id_usuario = g.getInt("id_usuario"),
                            descripcion = g.getString("descripcion"),
                            monto = g.getDouble("monto"),
                            fecha = g.getString("fecha"),
                            fecha_actualizacion = g.getString("fecha_actualizacion")
                        )
                        db.insertarGasto(gasto)
                    }
                    Log.d("SincronizarGastos", "Sincronizados ${array.length()} gastos")
                } else {
                    Log.e("SincronizarGastos", "Error del servidor: ${json.optString("error")}")
                }
            } catch (e: Exception) {
                Log.e("SincronizarGastos", "Excepción: ${e.message}")
            }
        }
    })
}

fun sincronizarEmpaque(context: Context, idViaje: Int) {
    val db = DBHelper(context)
    val itemsLocales = db.obtenerItemsEmpaquePorViaje(idViaje)


    val jsonArray = JSONArray().apply {
        for (i in itemsLocales) {
            put(JSONObject().apply {
                put("id", i.id)
                put("id_viaje", i.id_viaje)
                put("id_usuario", i.id_usuario)
                put("item", i.item)
                put("marcado", i.marcado)
                put("fecha_actualizacion", i.fecha_actualizacion)
            })
        }
    }

    val data = JSONObject().apply {
        put("id_viaje", idViaje)
        put("empaque", jsonArray)
    }

    val body = data.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(Constantes.BASE_URL + "sincronizar_empaque.php")
        .post(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SincronizarEmpaque", "Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string()
            val jsonTexto = bodyStr?.trimStart()?.dropWhile { it != '{' } ?: "{}"
            try {
                val json = JSONObject(jsonTexto)
                if (json.getBoolean("success")) {
                    val array = json.getJSONArray("empaque")
                    db.eliminarEmpaqueDeViaje(idViaje)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val item = ItemEmpaque(
                            id = obj.getInt("id"),
                            id_viaje = obj.getInt("id_viaje"),
                            id_usuario = obj.getInt("id_usuario"),
                            item = obj.getString("item"),
                            marcado = obj.getInt("marcado") == 1
                            ,//Type mismatch: inferred type is Int but Boolean was expected
                            fecha_actualizacion = obj.getString("fecha_actualizacion")
                        )
                        db.insertarItemEmpaque(item)
                    }
                    Log.d("SincronizarEmpaque", "${array.length()} items sincronizados")
                } else {
                    Log.e("SincronizarEmpaque", "Error del servidor: ${json.optString("error")}")
                }
            } catch (e: Exception) {
                Log.e("SincronizarEmpaque", "Excepción: ${e.message}")
            }
        }
    })
}








// Aquí irán más funciones como:
// fun sincronizarGastos(...)
// fun sincronizarItinerario(...)
// fun sincronizarListaEmpaque(...)
