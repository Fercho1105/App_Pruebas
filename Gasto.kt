package com.example.tripmates

data class Gasto(
    val id: Int,
    val id_viaje: Int,
    val id_usuario: Int,
    val descripcion: String,
    val monto: Double,
    val fecha: String,
    val fecha_actualizacion: String
)
