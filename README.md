
# TripMates - App de viajes colaborativos 🌏🏖️

## App_Pruebas

Repositorio del proyecto final de Ingeniería de Pruebas:
[Fercho1105/App_Pruebas](https://github.com/Fercho1105/App_Pruebas/tree/main)


## 🔁 Descripción

**TripMates** es una aplicación Android desarrollada como proyecto escolar, cuyo propósito principal es gestionar viajes y facilitar el control de gastos, itinerarios y listas de empaque. Ofrece sincronización de datos entre dispositivos, permitiendo que varios usuarios colaboren y compartan la información de un mismo viaje en tiempo real.


## 🎯 Objetivo General

Ofrecer una plataforma colaborativa donde los usuarios puedan planificar sus viajes y compartir información de gastos, itinerarios y equipaje de manera eficiente y sincronizada.


## 🚀 Stack Tecnológico

* **Lenguaje principal:** Kotlin
* **UI:** Jetpack Compose
* **Base de datos local:** SQLite (con `DBHelper.kt`)
* **Backend remoto:** PHP + MySQL (servicios REST personalizados)
* **Formato de intercambio:** JSON
* **HTTP Client:** OkHttp


## 💻 Requisitos

* Android 14.0 o superior
* Android Studio (Ladybug o superior)
* JDK 11 o superior


## ⚙️ Instalación y Configuración

1. Clona este repositorio:

```bash
git clone https://github.com/Fercho1105/App_Pruebas.git
```

2. Abre el proyecto en Android Studio.

3. En tu servidor local (XAMPP, Laragon o similar):

   * Crea la base de datos `tripmates_db`.
   * Ejecuta el script `base.sql` para crear las tablas.
   * Copia los archivos PHP en `htdocs/proymov/`.

4. Cambia la IP de tu servidor local en `Constantes.kt`:

```kotlin
const val BASE_URL = "http://192.168.X.X/proymov/"
```

5. Sincroniza Gradle y ejecuta la app en emulador o dispositivo conectado a la red local.


## ✨ Características Principales

* Registro e inicio de sesión de usuarios.
* Creación, edición y eliminación (lógica) de viajes.
* Invitación a otros usuarios por correo para colaborar en viajes.
* Registro de gastos compartidos.
* Planificación de itinerario por fecha y hora.
* Lista colaborativa de empaque.
* Sincronización bidireccional entre dispositivos.
* Modo claro y oscuro.
* Soporte para orientación portrait y landscape.


## ✅ Validaciones y Tests

* Verificación completa de CRUD en viajes, itinerario, gastos y empaque.
* Pruebas de sincronización colaborativa en tiempo real entre dos dispositivos.
* Validaciones de formularios y formatos de fecha/hora.
* Confirmación de integridad en modo landscape.
* Pruebas de eliminación lógica sin colisiones.


## 🌍 Estructura de carpetas

```
tripmates/
├── app/
│   └── src/main/java/com/example/tripmates/
│       ├── DBHelper.kt
│       ├── ViajesScreen.kt
│       ├── GastosScreen.kt
│       ├── ItinerarioScreen.kt
│       ├── ListaEmpaqueScreen.kt
│       ├── DialogCrearEditarViaje.kt
│       ├── DialogoAgregarActividad.kt
│       ├── Sincronizador.kt
│       └── ...
└── php/
    ├── sincronizar_viajes.php
    ├── eliminar_viaje.php
    ├── sincronizar_gastos.php
    ├── eliminar_gasto.php
    └── ...
```


## 📦 APK de prueba

Este repositorio incluye un archivo `.apk` generado desde Android Studio con una versión funcional de la app configurada para entorno local (modo desarrollador). Puedes instalarlo directamente en un dispositivo Android compatible para probar TripMates sin compilar el proyecto.


## ✅ Estado del proyecto

* [x] CRUD local y remoto para todos los módulos
* [x] Sincronización bidireccional con MySQL
* [x] Modo colaborativo por invitación
* [x] Eliminación lógica sincronizada
* [x] Soporte para orientación landscape
* [x] UI optimizada con Compose y tema oscuro



## 🛠️ Licencia

Este proyecto es de uso académico. Puedes adaptarlo y reutilizarlo libremente para fines educativos.

