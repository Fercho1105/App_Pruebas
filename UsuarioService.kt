package com.example.tripmates

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject

fun registrarUsuario(
    correo: String,
    nombre: String,
    contrasena: String,
    context: Context,
    callback: (Usuario?) -> Unit
) {
    val json = JSONObject()
    json.put("correo", correo)
    json.put("nombre", nombre)
    json.put("contraseña", contrasena)

    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(Constantes.BASE_URL + "registrar_usuario.php")

        .post(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post { callback(null) }
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string()
            val jsonTexto = bodyStr?.trimStart()?.dropWhile { it != '{' } ?: "{}"

            try {
                val json = JSONObject(jsonTexto)
                if (json.optBoolean("success")) {
                    val u = json.getJSONObject("usuario")
                    val usuario = Usuario(
                        id = u.getInt("id"),
                        correo = u.getString("correo"),
                        nombre = u.getString("nombre"),
                        contrasena = u.getString("contrasena"),
                        fecha_actualizacion = u.getString("fecha_actualizacion")
                    )

                    // Guardar también en SQLite local
                    val db = DBHelper(context)
                    db.insertarUsuarioLocal(usuario)

                    Handler(Looper.getMainLooper()).post { callback(usuario) }
                } else {
                    Handler(Looper.getMainLooper()).post { callback(null) }
                }
            } catch (e: Exception) {
                Log.e("registrarUsuario", "Error al parsear JSON: ${e.message}")
                Handler(Looper.getMainLooper()).post { callback(null) }
            }
        }
    })
}



fun loginUsuario(
    correo: String,
    contrasena: String,
    callback: (Usuario?) -> Unit
) {
    val json = JSONObject()
    json.put("correo", correo)
    json.put("contraseña", contrasena)

    Log.d("loginUsuario", "JSON que se enviará: correo=$correo, contraseña=$contrasena")

    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(Constantes.BASE_URL + "login_usuario.php")

        .post(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("loginUsuario", "Fallo de conexión: ${e.message}")
            Handler(Looper.getMainLooper()).post { callback(null) }
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string()
            val jsonTexto = bodyStr?.trimStart()?.dropWhile { it != '{' } ?: "{}"

            try {
                val json = JSONObject(jsonTexto)
                if (json.optBoolean("success")) {
                    val u = json.getJSONObject("usuario")
                    val usuario = Usuario(
                        id = u.getInt("id"),
                        correo = u.getString("correo"),
                        nombre = u.getString("nombre"),
                        contrasena = u.getString("contrasena"),
                        fecha_actualizacion = u.getString("fecha_actualizacion")
                    )
                    Handler(Looper.getMainLooper()).post { callback(usuario) }
                } else {
                    Log.w("loginUsuario", "Credenciales incorrectas o error en servidor: $jsonTexto")
                    Handler(Looper.getMainLooper()).post { callback(null) }
                }
            } catch (e: Exception) {
                Log.e("loginUsuario", "Error al parsear JSON: ${e.message}")
                Handler(Looper.getMainLooper()).post { callback(null) }
            }
        }
    })
}



