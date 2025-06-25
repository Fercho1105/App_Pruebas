package com.example.tripmates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tripmates.ui.theme.LocalDarkThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaEmpaqueScreen(
    usuario: Usuario,
    viaje: Viaje,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { DBHelper(context) }
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf(emptyList<ItemEmpaque>()) }
    var nuevoItem by rememberSaveable { mutableStateOf("") }
    var mostrarDialogo by rememberSaveable { mutableStateOf(false) }

    // Menús y diálogos
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var showOpcionesDialog by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    // Tema y fondo
    val darkThemeState = LocalDarkThemeState.current
    val isDark = darkThemeState.value
    val fondo = if (isDark) R.drawable.fondo_oscuro else R.drawable.fondo_claro

    LaunchedEffect(Unit) {
        sincronizarEmpaque(context, viaje.id)
        items = db.obtenerItemsEmpaquePorViaje(viaje.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista por empacar") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            sincronizarEmpaque(context, viaje.id)
                            items = db.obtenerItemsEmpaquePorViaje(viaje.id)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
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
                            onClick = { menuExpanded = false; showOpcionesDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Información") },
                            onClick = { menuExpanded = false; showInfoDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Ayuda") },
                            onClick = { menuExpanded = false; showHelpDialog = true }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Cerrar") },
                            onClick = { menuExpanded = false; onVolver() }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .heightIn(min = 72.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nuevoItem,
                        onValueChange = { nuevoItem = it },
                        placeholder = { Text("Agregar nuevo ítem") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (nuevoItem.isNotBlank()) {
                                scope.launch(Dispatchers.IO) {
                                    db.agregarItemEmpaque(
                                        idViaje = viaje.id,
                                        idUsuario = usuario.id,
                                        item = nuevoItem.trim(),
                                        fechaActualizacion = Utilidades.obtenerFechaActual()
                                    )
                                    sincronizarEmpaque(context, viaje.id)
                                    items = db.obtenerItemsEmpaquePorViaje(viaje.id)
                                    nuevoItem = ""
                                }
                            }
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Agregar")
                    }
                }
            }
        }
    ) { paddingValues ->
        Image(
            painter = painterResource(id = fondo),
            contentDescription = "Fondo",
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)  // Usa los paddingValues del Scaffold
                .systemBarsPadding()     // Añade padding para las barras del sistema
                .navigationBarsPadding() // Padding específico para la barra de navegación
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "Aún no hay elementos en la lista.",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items.forEach { item ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Checkbox(
                                checked = item.marcado,
                                onCheckedChange = {
                                    scope.launch(Dispatchers.IO) {
                                        db.actualizarItemEmpaqueMarcar(
                                            idItem = item.id,
                                            marcado = it,
                                            fechaActualizacion = Utilidades.obtenerFechaActual()
                                        )
                                        sincronizarEmpaque(context, viaje.id)
                                        items = db.obtenerItemsEmpaquePorViaje(viaje.id)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.item,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                db.eliminarItemEmpaque(item.id)
                                db.eliminarItemEmpaqueRemoto(item.id,
                                    onSuccess = {
                                        sincronizarEmpaque(context, viaje.id)
                                        items = db.obtenerItemsEmpaquePorViaje(viaje.id)
                                    },
                                    onError = {
                                        // Puedes mostrar un Toast si quieres avisar del error
                                    }
                                )
                            }

                        },
                    modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Diálogos de opciones, info y ayuda (sin cambios)
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
                    Text("Ayuda - Lista de Empaque", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.AddCircle, contentDescription = null) },
                        headlineContent = { Text("Agregar ítem", style = MaterialTheme.typography.titleMedium) },
                        supportingContent = {
                            Text("Pulsa “+” para crear un nuevo elemento.", style = MaterialTheme.typography.bodyMedium)
                        }
                    )
                    Divider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        headlineContent = { Text("Eliminar ítem", style = MaterialTheme.typography.titleMedium) },
                        supportingContent = {
                            Text("Toca el ícono de basura para eliminar un elemento.", style = MaterialTheme.typography.bodyMedium)
                        }
                    )
                    Divider()
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        headlineContent = { Text("Marcar/Desmarcar", style = MaterialTheme.typography.titleMedium) },
                        supportingContent = {
                            Text("Toca el elemento o usa el checkbox para marcarlo.", style = MaterialTheme.typography.bodyMedium)
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
}

@Composable
fun DialogoAgregarItemEmpaque(
    usuario: Usuario,
    viaje: Viaje,
    onGuardar: (ItemEmpaque) -> Unit,
    onCancelar: () -> Unit
) {
    var itemTexto by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text("Nuevo ítem para empacar")
        },
        text = {
            OutlinedTextField(
                value = itemTexto,
                onValueChange = { itemTexto = it },
                label = { Text("¿Qué necesitas empacar?") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemTexto.isNotBlank()) {
                        val nuevoItem = ItemEmpaque(
                            id = 0,
                            id_viaje = viaje.id,
                            id_usuario = usuario.id,
                            item = itemTexto,
                            marcado = false,
                            fecha_actualizacion = Utilidades.obtenerFechaActual()
                        )
                        onGuardar(nuevoItem)
                    }
                }
            ) {
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
