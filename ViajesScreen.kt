package com.example.tripmates

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.layout.ContentScale
import com.example.tripmates.ui.theme.LocalDarkThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Brush
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import okio.IOException
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViajesScreen(
    usuario: Usuario,
    onCerrarSesion: () -> Unit,
    onAbrirGastos: (Viaje) -> Unit,
    onAbrirItinerario: (Viaje) -> Unit,
    onAbrirListaEmpaque: (Viaje) -> Unit
) {
    val context = LocalContext.current
    val db = remember { DBHelper(context) }
    val scope = rememberCoroutineScope()

    var listaViajes by remember { mutableStateOf(emptyList<Viaje>()) }
    var mostrarDialogoCrear by rememberSaveable { mutableStateOf(false) }
    var viajeEditandoId by rememberSaveable { mutableStateOf<Int?>(null) }
    var viajeSeleccionadoId by rememberSaveable { mutableStateOf<Int?>(null) }

    var mostrarDialogoInvitar by rememberSaveable { mutableStateOf(false) }
    var mensajeInvitacion by remember { mutableStateOf<String?>(null) }

    // Menú overflow
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    // Diálogos de Opciones / Info / Ayuda
    var showOpcionesDialog by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    // Tema actual
    val darkThemeState = LocalDarkThemeState.current
    val isDark = darkThemeState.value
    val fondo = if (isDark) R.drawable.fondo_oscuro else R.drawable.fondo_claro

    val viajeEditando = listaViajes.find { it.id == viajeEditandoId }
    val viajeSeleccionado = listaViajes.find { it.id == viajeSeleccionadoId }

    LaunchedEffect(Unit) {
        sincronizarViajes(context, usuario.id)
        listaViajes = db.obtenerViajesPorUsuario(usuario.id)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Viajes") },
            actions = {
                IconButton(onClick = {
                    scope.launch {
                        sincronizarViajes(context, usuario.id)
                        sincronizarViajesUsuarios(context, usuario.id) // ✅ también sincroniza relaciones
                        listaViajes = db.obtenerViajesPorUsuario(usuario.id)
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                }

                IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Opciones") },
                            onClick = {
                                menuExpanded = false
                                showOpcionesDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Información") },
                            onClick = {
                                menuExpanded = false
                                showInfoDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ayuda") },
                            onClick = {
                                menuExpanded = false
                                showHelpDialog = true
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                menuExpanded = false
                                onCerrarSesion()
                            }
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viajeEditandoId = null
                mostrarDialogoCrear = true
            }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Image(
                painter = painterResource(id = fondo),
                contentDescription = "Fondo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(listaViajes) { viaje ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable {
                                viajeSeleccionadoId = viaje.id },
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Text(viaje.nombre, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("Destino: ${viaje.destino}", style = MaterialTheme.typography.bodyMedium)
                            Text("Inicio: ${viaje.fecha_inicio}", style = MaterialTheme.typography.bodySmall)
                            Text("Fin: ${viaje.fecha_fin}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }


    }

    // ——————————————————————————————————————————
    // Diálogo Opciones: selector de tema claro/oscuro
    if (showOpcionesDialog) {
        AlertDialog(
            onDismissRequest = { showOpcionesDialog = false },
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text("Tema", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("Claro", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = darkThemeState.value,
                        onCheckedChange = { darkThemeState.value = it }
                    )
                    Text("Oscuro", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showOpcionesDialog = false }) {
                    Text("Cerrar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

// … reemplaza showInfoDialog así:
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text("Información del Equipo", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TripMates v1.0", style = MaterialTheme.typography.bodyMedium)
                    Text("Desarrollado por:", style = MaterialTheme.typography.bodyMedium)
                    Text("- Paulín Lisset Ameyalli", style = MaterialTheme.typography.bodySmall)
                    Text("- Joshua Castro Ramírez", style = MaterialTheme.typography.bodySmall)
                    Text("- Fernando Díaz Hidalgo", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Cerrar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

// … y finalmente reemplaza showHelpDialog así:
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,              // ← aquí
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Ayuda", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                Icons.Filled.AddCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        headlineContent = {
                            Text("Agregar un viaje", style = MaterialTheme.typography.titleMedium)
                        },
                        supportingContent = {
                            Text(
                                "Pulsa el botón “+” en la esquina inferior derecha, completa los campos y guarda.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                    Divider()
                    ListItem(
                        leadingContent = {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        headlineContent = {
                            Text("Editar un viaje", style = MaterialTheme.typography.titleMedium)
                        },
                        supportingContent = {
                            Text(
                                "Toca la tarjeta del viaje, elige “Editar viaje” y modifica los datos.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Cerrar", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }



    if (mostrarDialogoCrear) {
        DialogCrearEditarViaje(
            viaje = viajeEditando,
            onCancelar = { mostrarDialogoCrear = false },
            onGuardar = { actualizado ->
                scope.launch(Dispatchers.IO) {
                    if (viajeEditando == null) {
                        val id = db.crearViaje(
                            nombre = actualizado.nombre,
                            destino = actualizado.destino,
                            fechaInicio = actualizado.fecha_inicio,
                            fechaFin = actualizado.fecha_fin,
                            creadorId = usuario.id,
                            fechaActualizacion = Utilidades.obtenerFechaActual()
                        )

                        if (id != -1L) {
                            sincronizarViajes(context, usuario.id)
                        }
                    } else {
                        val actualizadoConFecha = actualizado.copy(
                            fecha_actualizacion = Utilidades.obtenerFechaActual()
                        )
                        db.actualizarViaje(actualizadoConFecha)
                        sincronizarViajes(context, usuario.id)
                    }

                    listaViajes = db.obtenerViajesPorUsuario(usuario.id)
                    mostrarDialogoCrear = false
                }
            }

            ,
            onEliminar = {
                val viaje = viajeSeleccionado
                if (viaje != null) {
                    scope.launch(Dispatchers.IO) {
                        db.eliminarViaje(viaje.id)
                        eliminarViajeRemoto(viaje.id)
                        listaViajes = db.obtenerViajesPorUsuario(usuario.id)
                        viajeSeleccionadoId = null
                    }
                }
            }
        )
    }

    if (viajeSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { viajeSeleccionadoId = null },
            title = { Text(viajeSeleccionado.nombre) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Destino: ${viajeSeleccionado.destino}")
                    Text("Inicio: ${viajeSeleccionado.fecha_inicio}")
                    Text("Fin: ${viajeSeleccionado.fecha_fin}")
                }
            },
            confirmButton = {
                Column {
                    Button(
                        onClick = {
                            mostrarDialogoInvitar = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Invitar a otro usuario")
                    }

                    Button(
                        onClick = {
                            viajeEditandoId = viajeSeleccionado.id
                            mostrarDialogoCrear = true
                            viajeSeleccionadoId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar viaje")
                    }




                    Button(
                        onClick = {
                            onAbrirGastos(viajeSeleccionado)
                            viajeSeleccionadoId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver gastos")
                    }

                    Button(
                        onClick = {
                            onAbrirItinerario(viajeSeleccionado)
                            viajeSeleccionadoId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver itinerario")
                    }

                    Button(
                        onClick = {
                            onAbrirListaEmpaque(viajeSeleccionado)
                            viajeSeleccionadoId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver lista de empaque")
                    }

                    Button(
                        onClick = {
                            val viaje = viajeSeleccionado
                            if (viaje != null) {
                                scope.launch(Dispatchers.IO) {
                                    eliminarViajeRemoto(
                                        idViaje = viaje.id,
                                        onSuccess = {
                                            db.eliminarViaje(viaje.id)
                                            listaViajes = db.obtenerViajesPorUsuario(usuario.id)
                                            viajeSeleccionadoId = null
                                        },
                                        onError = { error ->
                                            Handler(Looper.getMainLooper()).post {
                                                Toast.makeText(context, "Error al eliminar: $error", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer)
                    }


                }
            },
            dismissButton = {
                TextButton(onClick = { viajeSeleccionadoId = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (mostrarDialogoInvitar && viajeSeleccionado != null) {
        DialogoInvitarUsuario(
            viaje = viajeSeleccionado,
            db = db,
            onCerrar = { mostrarDialogoInvitar = false },
            onMensaje = { mensajeInvitacion = it }
        )
    }

    mensajeInvitacion?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            mensajeInvitacion = null
        }
    }
}


@Composable
fun DialogoInvitarUsuario(
    viaje: Viaje,
    db: DBHelper,
    onCerrar: () -> Unit,
    onMensaje: (String) -> Unit
) {
    val context = LocalContext.current
    var correo by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Invitar usuario a viaje") },
        text = {
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo del usuario") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                buscarUsuarioPorCorreoRemoto(correo) { usuario ->
                    if (usuario == null) {
                        onMensaje("No se encontró un usuario con ese correo.")
                        onCerrar()
                    } else if (db.usuarioYaEstaEnViaje(usuario.id, viaje.id)) {
                        onMensaje("Ese usuario ya está en el viaje.")
                        onCerrar()
                    } else {
                        agregarUsuarioAViaje(
                            context = context,
                            usuarioId = usuario.id,
                            viajeId = viaje.id,
                            rol = "participante",
                            fechaActualizacion = Utilidades.obtenerFechaActual()
                        ) { exito ->
                            if (exito) {
                                onMensaje("Usuario invitado correctamente.")
                            } else {
                                onMensaje("Ocurrió un error al invitar.")
                            }
                            onCerrar()
                        }
                    }
                }
            }) {
                Text("Invitar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cancelar")
            }
        }
    )
}



@Composable
fun DialogVerViaje(
    viaje: Viaje,
    onCerrar: () -> Unit,
    onEditar: (Viaje) -> Unit,
    onEliminar: (Viaje) -> Unit,
    onVerGastos: (Viaje) -> Unit,


    ) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(text = viaje.nombre)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Destino: ${viaje.destino}")
                Text("Fecha inicio: ${viaje.fecha_inicio}")
                Text("Fecha fin: ${viaje.fecha_fin}")
            }
        },
        confirmButton = {
            Column {
                Button(onClick = { onVerGastos(viaje) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver gastos")
                }
                Button(onClick = { onEditar(viaje) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Editar viaje")
                }
                Button(
                    onClick = { onEliminar(viaje) },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer)
                }

            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cerrar")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogCrearEditarViaje(
    viaje: Viaje?,
    onCancelar: () -> Unit,
    onGuardar: (Viaje) -> Unit,
    onEliminar: (Viaje) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(viaje?.nombre ?: "") }
    var destino by rememberSaveable { mutableStateOf(viaje?.destino ?: "") }
    var fechaInicio by rememberSaveable { mutableStateOf(viaje?.fecha_inicio ?: "") }
    var fechaFin by rememberSaveable { mutableStateOf(viaje?.fecha_fin ?: "") }

    val context = LocalContext.current
    val calendario = Calendar.getInstance()

    val datePickerInicio = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                fechaInicio = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
    }

    val datePickerFin = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                fechaFin = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
    }

    BoxWithConstraints {
        val scrollState = rememberScrollState()
        val isHorizontal = maxWidth > 500.dp
        val fieldModifier = Modifier.fillMaxWidth(if (isHorizontal) 0.95f else 1f)

        AlertDialog(
            onDismissRequest = onCancelar,
            title = {
                Text(if (viaje == null) "Nuevo Viaje" else "Editar Viaje")
            },
            text = {
                // Área scrollable del diálogo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.8f)
                        .verticalScroll(scrollState)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        modifier = fieldModifier
                    )
                    OutlinedTextField(
                        value = destino,
                        onValueChange = { destino = it },
                        label = { Text("Destino") },
                        modifier = fieldModifier
                    )
                    Text("Fecha de inicio:")
                    TextButton(onClick = { datePickerInicio.show() }, modifier = fieldModifier) {
                        Text(fechaInicio.ifBlank { "Seleccionar fecha" })
                    }
                    Text("Fecha de fin:")
                    TextButton(onClick = { datePickerFin.show() }, modifier = fieldModifier) {
                        Text(fechaFin.ifBlank { "Seleccionar fecha" })
                    }
                }
            },
            confirmButton = {
                val formatoFecha = Regex("""^\d{4}-\d{2}-\d{2}$""")
                Button(onClick = {
                    if (nombre.isBlank() || destino.isBlank()) {
                        Toast.makeText(context, "Nombre y destino son obligatorios", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!formatoFecha.matches(fechaInicio)) {
                        Toast.makeText(context, "Fecha de inicio inválida (formato: yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!formatoFecha.matches(fechaFin)) {
                        Toast.makeText(context, "Fecha de fin inválida (formato: yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (fechaFin < fechaInicio) {
                        Toast.makeText(context, "La fecha de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val viajeNuevo = Viaje(
                        id = viaje?.id ?: 0,
                        nombre = nombre,
                        destino = destino,
                        fecha_inicio = fechaInicio,
                        fecha_fin = fechaFin,
                        creador_id = viaje?.creador_id ?: 0,
                        fecha_actualizacion = viaje?.fecha_actualizacion ?: Utilidades.obtenerFechaActual()
                    )
                    onGuardar(viajeNuevo)
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Row {
                    if (viaje != null) {
                        TextButton(onClick = { onEliminar(viaje) }) {
                            Text("Eliminar")
                        }
                    }
                    TextButton(onClick = onCancelar) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }
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
