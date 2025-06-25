package com.example.tripmates

import android.content.Context

fun guardarUsuarioLogueado(context: Context, id: Int) {
    val prefs = context.getSharedPreferences("tripmates_prefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("usuario_id", id).apply()
}

fun obtenerUsuarioLogueado(context: Context): Int {
    val prefs = context.getSharedPreferences("tripmates_prefs", Context.MODE_PRIVATE)
    return prefs.getInt("usuario_id", -1)
}

fun cerrarSesion(context: Context) {
    val prefs = context.getSharedPreferences("tripmates_prefs", Context.MODE_PRIVATE)
    prefs.edit().remove("usuario_id").apply()
}
