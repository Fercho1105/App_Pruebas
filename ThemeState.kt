package com.example.tripmates.ui.theme

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

/** Un state holder para el flag dark/light */
val LocalDarkThemeState = compositionLocalOf<MutableState<Boolean>> {
    error("No ThemeState provided")
}
