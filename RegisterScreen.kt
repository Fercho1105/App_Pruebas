package com.example.tripmates

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegistroExitoso: (Usuario) -> Unit,
    onVolverLogin: () -> Unit
) {
    val context = LocalContext.current

    var correo by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }

    var mensajeError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo completo
        Image(
            painter = painterResource(id = R.drawable.fondoinicio),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Capa semitransparente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.8f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Card con el formulario
            Card(
                modifier = Modifier
                    .fillMaxWidth(
                        if (LocalConfiguration.current.screenWidthDp > 500) 0.6f else 1f
                    ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Registro", style = MaterialTheme.typography.headlineSmall)

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contrasena,
                        onValueChange = { contrasena = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (correo.isBlank() || nombre.isBlank() || contrasena.isBlank()) {
                                mensajeError = "Por favor completa todos los campos"
                                return@Button
                            }

                            registrarUsuario(correo, nombre, contrasena, context) { usuario ->
                                if (usuario != null) {
                                    guardarUsuarioLogueado(context, usuario.id)
                                    onRegistroExitoso(usuario)
                                } else {
                                    mensajeError = "No se pudo registrar. El correo ya existe o hubo error."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrarse")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onVolverLogin) {
                        Text("¿Ya tienes cuenta? Inicia sesión")
                    }

                    mensajeError?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
