package com.example.tripmates

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.tripmates.ui.theme.LocalDarkThemeState
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosScreen(
    usuario: Usuario,
    viaje: Viaje,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { DBHelper(context) }
    val scope = rememberCoroutineScope()

    var gastos by rememberSaveable { mutableStateOf(emptyList<Gasto>()) }
    var mostrarDialogo by rememberSaveable { mutableStateOf(false) }
    var gastoEditandoId by rememberSaveable { mutableStateOf<Int?>(null) }
    var viajeEditarId by rememberSaveable { mutableStateOf<Int?>(null) }
    val mostrarDialogoEditarViaje = viajeEditarId == viaje.id

    // Estados para los menús y diálogos
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var showOpcionesDialog by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    // Tema actual
    val darkThemeState = LocalDarkThemeState.current
    val isDark = darkThemeState.value
    val fondo = if (isDark) R.drawable.fondo_oscuro else R.drawable.fondo_claro


    val gastoEditando = gastos.find { it.id == gastoEditandoId }

    LaunchedEffect(viaje.id) {
        sincronizarGastos(context, viaje.id)
        gastos = db.obtenerGastosPorViaje(viaje.id)
    }

    val total = gastos.sumOf { it.monto }
    val formato = DecimalFormat("#,##0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gastos del viaje") },
                actions = {
                    // Botón Volver
                    TextButton(onClick = onVolver) {
                        Text("Volver")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            sincronizarGastos(context, viaje.id)
                            gastos = db.obtenerGastosPorViaje(viaje.id)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
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
                gastoEditandoId = null
                mostrarDialogo = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar gasto")
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
                .fillMaxSize()
                .padding(paddingValues) //
                .padding(16.dp) //
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.85f
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "Total gastado: $${formato.format(total)}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }

            gastos.forEach { gasto ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable {
                            gastoEditandoId = gasto.id
                            mostrarDialogo = true
                        },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
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

                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(gasto.descripcion, style = MaterialTheme.typography.titleMedium)
                            Text("Monto: $${gasto.monto}")
                            Text("Fecha: ${gasto.fecha}")
                        }
                    }
                }
            }
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


        // — Ayuda específica de Gastos
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
                        Text("Ayuda - Gastos", style = MaterialTheme.typography.titleLarge)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Filled.AddCircle, contentDescription = null)
                            },
                            headlineContent = {
                                Text(
                                    "Agregar un gasto",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    "Pulsa “+” y completa los campos.",
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
                                Text(
                                    "Editar o eliminar",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    "Haz clic en un gasto para editarlo o eliminarlo en el diálogo.",
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


        // — Diálogo de Agregar/Editar Gasto existente

    @Composable
fun DialogoAgregarGasto(
    usuario: Usuario,
    idViaje: Int,
    gastoExistente: Gasto? = null,
    onCancelar: () -> Unit,
    onGuardar: (Gasto) -> Unit,
    onEliminar: (Gasto) -> Unit = {}
) {
    var descripcion by rememberSaveable { mutableStateOf(gastoExistente?.descripcion ?: "") }
    var montoTexto by rememberSaveable { mutableStateOf(gastoExistente?.monto?.toString() ?: "") }
    var fecha by rememberSaveable {
        mutableStateOf(
            gastoExistente?.fecha ?: Utilidades.obtenerFechaActual().substring(0, 10)
        )
    }

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

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(if (gastoExistente == null) "Nuevo Gasto" else "Editar Gasto")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp) // LIMITE de altura para evitar errores
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )


                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

                )

                Text("Fecha:")
                TextButton(onClick = { datePicker.show() }, modifier = Modifier.fillMaxWidth()) {
                    Text(fecha.ifBlank { "Seleccionar fecha" })
                }

                /*if (gastoExistente != null) {
                    Button(
                        onClick = { onEliminar(gastoExistente) },
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }*/

                if (gastoExistente != null) {
                    Button(
                        onClick = { onEliminar(gastoExistente) },
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        },
        confirmButton = {
            val formatoFecha = Regex("""^\d{4}-\d{2}-\d{2}$""")
            Button(onClick = {
                val monto = montoTexto.toDoubleOrNull()
                if (descripcion.isBlank()) {
                    Toast.makeText(context, "La descripción es obligatoria", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (monto == null || monto <= 0.0) {
                    Toast.makeText(context, "El monto debe ser un número mayor a cero", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!formatoFecha.matches(fecha)) {
                    Toast.makeText(context, "La fecha es inválida (yyyy-MM-dd)", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val gasto = Gasto(
                    id = gastoExistente?.id ?: 0,
                    id_viaje = idViaje,
                    id_usuario = usuario.id,
                    descripcion = descripcion,
                    monto = monto,
                    fecha = fecha,
                    fecha_actualizacion = Utilidades.obtenerFechaActual()
                )
                onGuardar(gasto)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

        if (mostrarDialogo) {
            DialogoAgregarGasto(
                usuario = usuario,
                idViaje = viaje.id,
                gastoExistente = gastoEditando,
                onCancelar = {
                    mostrarDialogo = false
                    gastoEditandoId = null
                },
                onGuardar = { nuevoGasto ->
                    scope.launch(Dispatchers.IO) {
                        if (gastoEditando == null) {
                            db.agregarGasto(
                                idViaje = viaje.id,
                                idUsuario = usuario.id,
                                descripcion = nuevoGasto.descripcion,
                                monto = nuevoGasto.monto,
                                fecha = nuevoGasto.fecha,
                                fechaActualizacion = nuevoGasto.fecha_actualizacion
                            )
                        } else {
                            db.editarGasto(nuevoGasto.copy(id = gastoEditando.id))
                        }
                        sincronizarGastos(context, viaje.id)
                        gastos = db.obtenerGastosPorViaje(viaje.id)
                        mostrarDialogo = false
                        gastoEditandoId = null
                    }
                },
                onEliminar = { gasto ->
                    scope.launch(Dispatchers.IO) {
                        db.eliminarGasto(gasto.id)
                        db.eliminarGastoRemoto(gasto.id,
                            onSuccess = {
                                gastos = db.obtenerGastosPorViaje(viaje.id)
                            },
                            onError = {
                                // Puedes mostrar un mensaje si quieres
                            }
                        )
                        sincronizarGastos(context, viaje.id)
                        mostrarDialogo = false
                        gastoEditandoId = null
                    }
                }

            )
        }
    }
}