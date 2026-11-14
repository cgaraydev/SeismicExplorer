
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
- Room 3: 3.0.3
- Room Gradle Plugin: 3.0.3
- AndroidX SQLite Framework: 2.7.1

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
- AndroidX Test Core: 1.7.0
- Kotlin Coroutines Test: 1.11.0
- Pruebas instrumentadas de consultas y escritura SQLite
- Pruebas de observación continua de Flow.
- Pruebas de escrituras concurrentes y selección de la revisión más reciente.
- Pruebas de IDs duplicados, empates de timestamps y procesamiento por lotes.
- MockWebServer: 4.12.0 (pruebas HTTP locales)
- Pruebas JVM de Retrofit y Kotlin Serialization con MockWebServer.
- Validación de respuestas HTTP 200, 404, 500 y 503.
- Pruebas de JSON malformado y tipos incompatibles.
- Pruebas de clasificación de errores y conservación del caché.
- Pruebas de propagación de CancellationException.

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
- Repositorio offline-first con Room como fuente observable de datos.
- Sincronización inicial mediante feeds recientes de USGS.
- Actualización condicionada por la fecha de revisión de cada evento.
- Separación entre ventanas recientes de sincronización e intervalos absolutos de consulta.
- Ventanas USGS de 24 horas, 7 días y 30 días; cobertura histórica exacta pendiente de FDSN.
- Consultas Room observables ejecutadas en transacción para conservar la consistencia de lecturas grandes.
- Escrituras centralizadas en upsertIfNewer, con protección basada en updatedAt.
- Ante versiones con el mismo timestamp, se conserva la versión previamente almacenada.
- Resultado tipado de sincronización mediante RefreshResult.
- Clasificación de errores de conexión, servicio y respuestas inválidas.
- Propagación de cancelaciones de coroutines y excepciones inesperadas.
- Conservación de datos locales ante errores de actualización remota.

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