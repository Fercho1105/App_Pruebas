package com.example.tripmates

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tripmates.ui.theme.LocalDarkThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItinerarioScreen(
    viaje: Viaje,
    onVolver: () -> Unit)
    {
    val context = LocalContext.current
    val db = remember(context) { DBHelper(context) }
    val scope = rememberCoroutineScope()

        var actividades by remember { mutableStateOf(emptyList<Actividad>()) }
        var mostrarDialogo by remember { mutableStateOf(false) }
        var actividadEditando by remember { mutableStateOf<Actividad?>(null) }

        // Sincronizar al iniciar
        LaunchedEffect(Unit) {
            sincronizarItinerario(context, viaje.id)
            actividades = db.obtenerActividadesPorViaje(viaje.id)
        }

        // Estados para los menús y diálogos
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var showOpcionesDialog by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    // Tema (usa AppCompatDelegate o tu propia lógica)
        val darkThemeState = LocalDarkThemeState.current
        val isDark = darkThemeState.value
        val fondo = if (isDark) R.drawable.fondo_oscuro else R.drawable.fondo_claro


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Itinerario de viaje") },
                actions = {
                    TextButton(onClick = onVolver) {
                        Text("Volver")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            sincronizarItinerario(context, viaje.id)
                            actividades = db.obtenerActividadesPorViaje(viaje.id)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                    }
                    // Ícono de tres puntitos
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
                            text = { Text("Cerrar") },
                            onClick = {
                                menuExpanded = false
                                onVolver()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                actividadEditando = null
                mostrarDialogo = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { paddingValues ->
        Image(
            painter = painterResource(id = fondo),
            contentDescription = "Fondo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (actividades.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "No hay actividades registradas.",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(vertical = 24.dp, horizontal = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                }
            }


            actividades.forEach { actividad ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            actividadEditando = actividad
                            mostrarDialogo = true
                        },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Text(actividad.titulo, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Fecha: ${actividad.fecha}", style = MaterialTheme.typography.bodyMedium)
                        Text("Hora: ${actividad.hora ?: "Sin hora"}", style = MaterialTheme.typography.bodyMedium)
                        if (actividad.descripcion.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Descripción:", style = MaterialTheme.typography.bodyMedium)
                            Text(actividad.descripcion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
        // — Ayuda específica del Itinerario
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Ayuda – Itinerario", style = MaterialTheme.typography.titleLarge)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Filled.AddCircle, contentDescription = null)
                            },
                            headlineContent = {
                                Text("Agregar actividad", style = MaterialTheme.typography.titleMedium)
                            },
                            supportingContent = {
                                Text(
                                    "Pulsa “+” y completa los campos obligatorios.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                        Divider()
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                            },
                            headlineContent = {
                                Text("Editar / Eliminar", style = MaterialTheme.typography.titleMedium)
                            },
                            supportingContent = {
                                Text(
                                    "Toca una actividad para editarla o eliminarla en el diálogo.",
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



        // — Opciones: tema claro/oscuro
        if (showOpcionesDialog) {
            AlertDialog(
                onDismissRequest = { showOpcionesDialog = false },
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text("Tema", style = MaterialTheme.typography.titleLarge) },
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

        // — Información del equipo
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = {
                    Text(
                        "Información del Equipo",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TripMates v1.0", style = MaterialTheme.typography.bodyMedium)
                        Text("– Paulín Lisset Ameyalli", style = MaterialTheme.typography.bodySmall)
                        Text("– Joshua Castro Ramírez", style = MaterialTheme.typography.bodySmall)
                        Text("– Fernando Díaz Hidalgo", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Cerrar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        }


        if (mostrarDialogo) {
            DialogoAgregarActividad(
                viajeId = viaje.id,
                actividad = actividadEditando,
                onCancelar = { mostrarDialogo = false },
                onGuardar = { nueva ->
                    scope.launch(Dispatchers.IO) {
                        if (actividadEditando == null) {
                            db.agregarActividad(
                                idViaje = viaje.id,
                                titulo = nueva.titulo,
                                descripcion = nueva.descripcion,
                                fecha = nueva.fecha,
                                hora = nueva.hora,
                                fechaActualizacion = Utilidades.obtenerFechaActual()
                            )
                        } else {
                        db.editarActividad(nueva.copy(fecha_actualizacion = Utilidades.obtenerFechaActual()))
                    }
                    sincronizarItinerario(context, viaje.id)
                    actividades = db.obtenerActividadesPorViaje(viaje.id)
                    mostrarDialogo = false
                }
            },
                onEliminar = { actividad ->
                    scope.launch(Dispatchers.IO) {
                        db.eliminarActividad(actividad.id) // ✅ Elimina local
                        db.eliminarActividadRemota(actividad.id, // ✅ Elimina remoto
                            onSuccess = {
                                actividades = db.obtenerActividadesPorViaje(viaje.id) // Refresca lista
                            },
                            onError = {
                                // Puedes mostrar un Toast si quieres informar de error
                                // Toast.makeText(context, "Error eliminando remotamente", Toast.LENGTH_SHORT).show()
                            }
                        )
                        mostrarDialogo = false
                    }
                }

            ) }

}

@Composable
fun DialogoAgregarActividad(
    viajeId: Int,
    actividad: Actividad?,
    onCancelar: () -> Unit,
    onGuardar: (Actividad) -> Unit,
    onEliminar: (Actividad) -> Unit = {}
) {
    var titulo by rememberSaveable { mutableStateOf(actividad?.titulo ?: "") }
    var descripcion by rememberSaveable { mutableStateOf(actividad?.descripcion ?: "") }
    var fecha by rememberSaveable { mutableStateOf(actividad?.fecha ?: "") }
    var hora by rememberSaveable { mutableStateOf(actividad?.hora ?: "") }

    val context = LocalContext.current
    val calendario = Calendar.getInstance()

    val datePicker = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                fecha = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
    }

    val timePicker = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                hora = String.format("%02d:%02d", hourOfDay, minute)
            },
            calendario.get(Calendar.HOUR_OF_DAY),
            calendario.get(Calendar.MINUTE),
            true
        )
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(if (actividad == null) "Nueva Actividad"
            else "Editar Actividad")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Fecha:")
                TextButton(onClick = { datePicker.show() },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(fecha.ifBlank { "Seleccionar fecha" })
                }

                Text("Hora:")
                TextButton(onClick = { timePicker.show() },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(hora.ifBlank { "Seleccionar hora" })
                }

            }
        },
        confirmButton = {
            val formatoFecha = Regex("""^\d{4}-\d{2}-\d{2}$""")
            val formatoHora = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")
            Button(onClick = {
                if (titulo.isBlank()) {
                    Toast.makeText(context, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!formatoFecha.matches(fecha)) {
                    Toast.makeText(context, "La fecha es inválida (yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (hora.isNotBlank() && !formatoHora.matches(hora)) {
                    Toast.makeText(context, "La hora es inválida (HH:mm)", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val nueva = Actividad(
                    id = actividad?.id ?: 0,
                    id_viaje = viajeId,
                    titulo = titulo,
                    descripcion = descripcion,
                    fecha = fecha,
                    hora = hora,
                    fecha_actualizacion = Utilidades.obtenerFechaActual()
                )
                onGuardar(nueva)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row {
                if (actividad != null) {
                    Button(
                        onClick = { onEliminar(actividad) },
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                TextButton(onClick = onCancelar) {
                    Text("Cancelar")
                }
            }
        }
    )
}