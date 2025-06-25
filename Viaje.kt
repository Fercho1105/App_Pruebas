package com.example.tripmates

data class Viaje(
    val id: Int,
    val nombre: String,
    val destino: String,
    val fecha_inicio: String,
    val fecha_fin: String,
    val creador_id: Int?,
    val fecha_actualizacion: String
)
