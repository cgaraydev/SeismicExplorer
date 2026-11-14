
# SeismicExplorer - Desarrollo

Documento interno de referencia para tecnologías, funcionalidades pendientes, observaciones y revisiones técnicas.

## Tecnologías y versiones

### Plataforma y herramientas

- Android Gradle Plugin: 9.5.1
- Gradle Wrapper: 9.9.1
- Kotlin: 2.4.21
- JDK de Gradle: 25
- Compatibilidad Java configurada: 11
- compileSdk: 37
- targetSdk: 37
- minSdk: 29
- Gradle Kotlin DSL
- GitHub Actions: integración continua

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
- Inyección de dependencias mediante Hilt, pendiente de incorporación.
- Preferencia por APIs estables y componentes reutilizables.

## Funcionalidades pendientes

- Integración con los servicios sísmicos de USGS.
- Persistencia local y sincronización offline-first.
- Inyección de dependencias con Hilt.
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

Este apartado se utilizará para incorporar notas, ideas y mejoras pequeñas durante el desarrollo.

Las observaciones pendientes se revisarán después de cada push y podrán incorporarse a los incrementos siguientes.

Conservar las observaciones que todavía no hayan sido analizadas o resueltas.
