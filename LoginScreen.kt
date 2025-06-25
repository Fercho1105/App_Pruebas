package com.example.tripmates

import androidx.compose.ui.graphics.Color
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import android.os.Handler
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource

@Composable
fun LoginScreen(
    onLoginExitoso: (Usuario) -> Unit,
    onRegistroClick: () -> Unit
) {
    val context = LocalContext.current
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 1) Fondo
        Image(
            painter = painterResource(id = R.drawable.fondoinicio),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2) Capa semitransparente + formulario
        Box(
            modifier = Modifier
                .fillMaxSize()
                // color blanco con alpha al 80%
                .background(Color.White.copy(alpha = 0.8f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Opcional: usa Card para sombra y borde redondeado
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (LocalConfiguration.current.screenWidthDp > 500) 0.6f else 1f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Iniciar Sesión",
                        style = MaterialTheme.typography.headlineSmall)

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo") },
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
                            // 1) Campos no vacíos
                            if (correo.isBlank() || contrasena.isBlank()) {
                                mensajeError = "Completa todos los campos"
                                return@Button
                            }
                            // 2) Formato de correo: debe tener '@' y terminar en ".com"
                            if (!correo.contains("@") || !correo.endsWith(".com")) {
                                mensajeError = "Correo inválido (debe contener @ y .com)"
                                return@Button
                            }
                            // 3) Sin espacios en contraseña
                            if (contrasena.contains(" ")) {
                                mensajeError = "La contraseña no puede contener espacios"
                                return@Button
                            }
                            // 4) Longitud mínima de contraseña (opcional)
                            if (contrasena.length < 6) {
                                mensajeError = "La contraseña debe tener al menos 6 caracteres"
                                return@Button
                            }

                            //
                            mensajeError = null
                            loginUsuario(correo, contrasena) { usuario ->
                                if (usuario != null) {
                                    Log.d("loginUsuario", "Usuario logueado: ${usuario.nombre}")

                                    // Guardar en SQLite local para que PantallaInicio lo detecte
                                    val db = DBHelper(context)
                                    db.insertarUsuarioLocal(usuario)

                                    guardarUsuarioLogueado(context, usuario.id)
                                    onLoginExitoso(usuario)
                                } else {
                                    Log.e("loginUsuario", "Login fallido: usuario es null")
                                    mensajeError = "Correo o contraseña incorrectos"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entrar")
                    }


                    Spacer(Modifier.height(12.dp))

                    TextButton(onClick = onRegistroClick) {
                        Text("¿No tienes cuenta? Regístrate aquí")
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

