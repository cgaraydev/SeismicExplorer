
# SeismicExplorer

[![Integración continua](https://github.com/cgaraydev/SeismicExplorer/actions/workflows/ci.yml/badge.svg)](https://github.com/cgaraydev/SeismicExplorer/actions/workflows/ci.yml)

**SeismicExplorer** es una aplicación Android nativa desarrollada con Kotlin y Jetpack Compose para explorar la actividad sísmica mundial.

El proyecto tiene como objetivo ofrecer información sobre terremotos mediante datos públicos del United States Geological Survey (USGS), con una interfaz moderna, filtros, mapas y acceso a información almacenada localmente.

## Estado del proyecto

En desarrollo.

Actualmente cuenta con una interfaz inicial en Compose, gestión de estado mediante UDF, filtros de presentación y modelos de dominio para eventos sísmicos.

La integración con USGS y la persistencia local todavía están pendientes.

## Tecnologías

- Kotlin y Jetpack Compose con Material 3.
- Arquitectura por capas, UDF y ViewModel.
- Coroutines y StateFlow.
- Gradle con Kotlin DSL.
- JUnit, AndroidX Test y Compose UI Test.
- GitHub Actions para integración continua.

## Compilación

Requiere Android Studio, Android SDK 37 y un entorno JDK compatible con el proyecto.

En Windows:

    .\gradlew.bat clean assembleDebug testDebugUnitTest lintDebug

Las pruebas instrumentadas pueden ejecutarse con:

    .\gradlew.bat connectedDebugAndroidTest

## Fuente de datos

La aplicación utilizará información pública de [USGS Earthquake Hazards Program](https://earthquake.usgs.gov/).

SeismicExplorer es un proyecto independiente y no constituye un sistema oficial de alerta sísmica.
