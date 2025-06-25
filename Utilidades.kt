package com.example.tripmates

import java.text.SimpleDateFormat
import java.util.*

object Utilidades {
    fun obtenerFechaActual(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formato.format(Date())
    }
}
