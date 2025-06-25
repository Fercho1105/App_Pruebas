package com.example.tripmates

data class Actividad(
    val id: Int,
    val id_viaje: Int,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val hora: String?,
    val fecha_actualizacion: String
)
