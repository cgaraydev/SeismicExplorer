
# SeismicExplorer - Desarrollo

Documento interno de referencia para tecnologías, funcionalidades pendientes, observaciones y revisiones técnicas.

## Tecnologías y versiones

### Plataforma y herramientas

- Android Gradle Plugin: 9.5.1
- Gradle Wrapper: 9.9.1
- Kotlin: 2.4.21
- JDK de Gradle: 25
- Compatibilidad Java configurada: 17
- compileSdk: 37
- targetSdk: 37
- minSdk: 29
- Gradle Kotlin DSL
- GitHub Actions: integración continua
- Dagger Hilt: 2.60.1
- KSP: 2.3.12
- Retrofit: 3.0.0
- Kotlin Serialization JSON: 1.13.0
- Retrofit Kotlin Serialization Converter: 3.0.0

### Android y presentación

- AndroidX Core KTX: 1.19.1
- AndroidX Activity Compose: 1.13.0
- AndroidX Lifecycle: 2.11.0
- Jetpack Compose BOM: 2026.09.00
- Material 3: versión gestionada por Compose BOM
- Kotlin Coroutines: 1.11.0
- UDF con ViewModel y StateFlow

### Pruebas

- JUnit 4: 4.13.2
- AndroidX Test JUnit: 1.3.0
- Espresso: 3.7.0
- Compose UI Test: versión gestionada por Compose BOM
- Pruebas unitarias JVM
- Pruebas instrumentadas Android
- Android Lint
- CI mediante GitHub Actions

## Arquitectura

- Kotlin y Jetpack Compose.
- Clean Architecture pragmática.
- Principios SOLID.
- Flujo de datos unidireccional (UDF).
- Separación entre presentación, dominio y datos.
- Modelos de dominio independientes de Android y de las fuentes externas.
- Enfoque offline-first.
- Inyección de dependencias mediante Hilt y generación de código con KSP.
- Preferencia por APIs estables y componentes reutilizables.

## Funcionalidades pendientes

- Integración con los servicios sísmicos de USGS.
- Persistencia local y sincronización offline-first.
- Listado y detalle de eventos sísmicos.
- Filtros avanzados y consultas históricas.
- Mapas interactivos.
- Estadísticas y visualización de datos.
- Diseño visual, animaciones y accesibilidad.
- Optimización, pruebas y preparación para Google Play.

## Revisiones técnicas periódicas

Al cerrar una funcionalidad importante o aproximadamente cada tres o cuatro incrementos, realizar una revisión técnica integral utilizando el modelo de razonamiento más potente disponible en ese momento.

Aspectos a revisar:

- Arquitectura y separación de responsabilidades.
- Aplicación de SOLID y Clean Architecture sin sobreingeniería.
- Gestión de estado y flujo UDF.
- Calidad, legibilidad y mantenibilidad del código.
- Dependencias, versiones y APIs deprecadas o experimentales.
- Rendimiento, concurrencia y consumo de recursos.
- Seguridad y tratamiento de datos.
- Cobertura y calidad de las pruebas.
- Diseño, accesibilidad y experiencia de usuario.

Discutir los hallazgos antes de implementar cambios. Priorizar problemas reales sobre recomendaciones puramente estilísticas.

## Observaciones para próximos incrementos

luego de conseguir un mvp, debemos darle un poco de tiempo a discutir temas visuales, la tipografia, q formas vamos a usar, vamos a usar lo q recomienda material3? lo mismo los iconoes, ocuparemos recursos de google icons o google fonts? crearemos nuestros iconos
no escatimes en badges en el readme, puedes poner de kotlin entre otras
en el build gradle de la app, comentar la libreria q usaremos antes de la implementation, por ejemplo:
// Hilt
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
crear paletas de colores opcionales para ver ambos modos, no me convence la paleta actual de ambos modos pero esta queda para mas adelante, una vez q tengamos varias screens integradas para poder decidir mejor 


