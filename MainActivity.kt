package com.example.tripmates

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.example.tripmates.ui.theme.LocalDarkThemeState
import com.example.tripmates.ui.theme.TripmatesTheme
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1) Hacemos que Compose dibuje tras la status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 2) Ponemos la status bar transparente
        window.statusBarColor = Color.TRANSPARENT
        setContent {
            // 1) Estado global para el tema (light/dark)
            val darkThemeState = rememberSaveable { mutableStateOf(false) }
            // 2) Estado para mostrar el splash
            var showSplash by rememberSaveable { mutableStateOf(true) }

            // 3) Proveemos el state del tema a toda la jerarquía
            CompositionLocalProvider(
                LocalDarkThemeState provides darkThemeState
            ) {
                // 4) Aplicamos tu tema, pasando el flag
                TripmatesTheme(darkTheme = darkThemeState.value) {
                    if (showSplash) {
                        SplashScreen { showSplash = false }
                    } else {
                        PantallaInicio()  // ya NO recibe isDarkTheme ni onToggleTheme
                    }
                }
            }
        }
    }

    // VideoView que ocupa toda la pantalla
    class FullScreenVideoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : VideoView(context, attrs, defStyle) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val h = MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension(w, h)
        }
    }

    @Composable
    fun SplashScreen(onFinish: () -> Unit) {
        val context = LocalContext.current
        var clicked by remember { mutableStateOf(false) }
        var videoVisible by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // Fade‐in del video
        val videoAlpha by animateFloatAsState(
            targetValue = if (videoVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 1_000)
        )

        // Fade‐in y escala del logo
        val logoAlpha by animateFloatAsState(
            targetValue = if (videoVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 1_000, delayMillis = 500)
        )
        val logoScale by animateFloatAsState(
            targetValue = if (clicked) 3f else 1f,
            animationSpec = tween(durationMillis = 600)
        )

        // Iniciar el fade‐in
        LaunchedEffect(Unit) {
            delay(300)
            videoVisible = true
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Video de loop con fade
            AndroidView(
                factory = { ctx ->
                    FullScreenVideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(
                            Uri.parse("android.resource://${ctx.packageName}/${R.raw.background_loop}")
                        )
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            start()
                        }
                    }
                },
                update = { /* nada más */ },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = videoAlpha }
            )

            // Logo con fade y click
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .wrapContentSize()
                    .graphicsLayer {
                        alpha = logoAlpha
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .clickable(enabled = videoVisible) {
                        clicked = true
                        scope.launch {
                            delay(600)
                            onFinish()
                        }
                    }
            )
        }
    }

    @Composable
    fun PantallaInicio() {
        val context = LocalContext.current
        val db = remember { DBHelper(context) }

        var usuarioId by rememberSaveable { mutableStateOf<Int?>(null) }
        var mostrarRegistro by rememberSaveable { mutableStateOf(false) }

        var viajeGastosId by rememberSaveable { mutableStateOf<Int?>(null) }
        var viajeItinerarioId by rememberSaveable { mutableStateOf<Int?>(null) }
        var viajeEmpaqueId by rememberSaveable { mutableStateOf<Int?>(null) }

        val usuarioActual = usuarioId?.let { db.loginUsuarioPorId(it) }

        // Recuperar sesión si existe
        LaunchedEffect(Unit) {
            val idGuardado = obtenerUsuarioLogueado(context)
            if (idGuardado > 0) {
                usuarioId = idGuardado
                sincronizarViajesUsuarios(context, idGuardado) // ✅ sincroniza relaciones al iniciar
            }
        }

        Surface {
            when {
                usuarioActual != null && viajeGastosId != null -> {
                    val viaje = db.obtenerViajePorId(viajeGastosId!!)
                    if (viaje != null) {
                        GastosScreen(
                            usuario = usuarioActual,
                            viaje = viaje,
                            onVolver = { viajeGastosId = null }
                        )
                    } else {
                        Text("No se encontró el viaje.")
                    }
                }

                usuarioActual != null && viajeItinerarioId != null -> {
                    val viaje = db.obtenerViajePorId(viajeItinerarioId!!)
                    if (viaje != null) {
                        ItinerarioScreen(
                            viaje = viaje,
                            onVolver = { viajeItinerarioId = null }
                        )
                    } else {
                        Text("No se encontró el viaje.")
                    }
                }


                usuarioActual != null && viajeEmpaqueId != null -> {
                    val viaje = db.obtenerViajePorId(viajeEmpaqueId!!)
                    if (viaje != null) {
                        ListaEmpaqueScreen(
                            usuario = usuarioActual,
                            viaje = viaje,
                            onVolver = { viajeEmpaqueId = null }
                        )
                    } else {
                        Text("No se encontró el viaje.")
                    }
                }

                usuarioActual != null && viajeEmpaqueId != null -> {
                    val viaje = db.obtenerViajePorId(viajeEmpaqueId!!)
                    if (viaje != null) {
                        ListaEmpaqueScreen(
                            usuario = usuarioActual,
                            viaje = viaje,
                            onVolver = { viajeEmpaqueId = null }
                        )
                    } else {
                        Text("No se encontró el viaje.")
                    }
                }

                usuarioActual != null -> {
                    ViajesScreen(
                        usuario = usuarioActual,
                        onCerrarSesion = {
                            cerrarSesion(context)
                            usuarioId = null
                            mostrarRegistro = false
                        },
                        onAbrirGastos = { viaje ->
                            viajeGastosId = viaje.id
                        },
                        onAbrirItinerario = { viaje ->
                            viajeItinerarioId = viaje.id
                        },
                        onAbrirListaEmpaque = { viaje ->
                            viajeEmpaqueId = viaje.id
                        }
                    )
                }

                mostrarRegistro -> {
                    RegisterScreen(
                        onRegistroExitoso = {
                            guardarUsuarioLogueado(context, it.id)
                            usuarioId = it.id
                        },
                        onVolverLogin = { mostrarRegistro = false }
                    )
                }

                else -> {
                    LoginScreen(
                        onLoginExitoso = {
                            guardarUsuarioLogueado(context, it.id)
                            usuarioId = it.id
                        },
                        onRegistroClick = {
                            mostrarRegistro = true
                        }
                    )
                }
            }
        }
    }
}

