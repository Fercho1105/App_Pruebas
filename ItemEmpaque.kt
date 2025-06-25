package com.example.tripmates

data class ItemEmpaque(
    val id: Int,
    val id_viaje: Int,
    val id_usuario: Int,
    val item: String,
    val marcado: Boolean,
    val fecha_actualizacion: String
)
