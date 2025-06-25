package com.example.tripmates

data class RelacionViajeUsuario(
    val usuario_id: Int,
    val viaje_id: Int,
    val rol: String,
    val fecha_actualizacion: String
)
