
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
- Navigation 3: 1.2.1
- AndroidX Hilt Lifecycle ViewModel Compose: 1.4.0
- Lifecycle ViewModel Navigation 3: 2.11.0

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
- Pruebas JVM del HomeViewModel con repositorio falso y reloj controlado.
- Pruebas de cambios de filtros sin descargas innecesarias.
- Pruebas de cancelación de sincronizaciones y errores con datos almacenados.
- Pruebas Compose de estados vacíos, actualización y datos locales visibles.
- Pruebas de consulta individual por ID en Room.
- Pruebas JVM del EarthquakeDetailViewModel.
- Pruebas de serialización de destinos Navigation 3.
- Pruebas Compose del detalle, retroceso, selección y enlaces oficiales.
- Pruebas instrumentadas de MainActivity con HiltTestApplication.
- Sustitución del repositorio de producción mediante @UninstallModules y @BindValue.
- Clock fijo y repositorio falso para pruebas deterministas.
- MainActivityTest no depende de solicitudes HTTP reales a USGS.

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
- HomeViewModel conectado a EarthquakeRepository mediante UDF y StateFlow.
- Observación de Room independiente de la sincronización remota.
- Cambios de magnitud sin solicitudes HTTP adicionales.
- Cancelación y protección frente a resultados de sincronización obsoletos.
- Actualización manual con renovación de los límites temporales.
- Transformaciones de listas fuera de Main mediante Dispatchers.
- Navegación con NavDisplay y pila restaurable mediante rememberNavBackStack.
- Destinos tipados que implementan NavKey y Kotlin Serialization.
- ViewModels asociados a entradas de navegación mediante ViewModelStoreNavEntryDecorator.
- Inyección asistida de argumentos de navegación mediante Hilt.
- Detalle sísmico observable por ID desde Room, sin solicitudes HTTP adicionales.

### Integración cartográfica

- MapLibre Native Android con OpenFreeMap como proveedor principal.
- MapView encapsulado mediante AndroidView.
- SeismicMapViewModel observa EarthquakeRepository y Room.
- La pantalla de mapa no inicia solicitudes HTTP a USGS.
- Conversión de terremotos a GeoJSON fuera del hilo principal.
- GeoJsonSource actualizable mediante setGeoJson.
- CircleLayer para representar los eventos geográficos.
- Visualización inicial de eventos almacenados de los últimos 7 días.
- Dispatcher de procesamiento cartográfico inyectable para pruebas.

## Funcionalidades pendientes

- Listado y detalle básico de eventos sísmicos implementados.
- Pendiente ampliación de datos USGS: intensidad, tipo de magnitud, alertas y productos.
- Pendiente diferenciar URL de página pública y URL del GeoJSON de detalle.
- Pendiente mapa interactivo y navegación desde mapa al detalle.
- Revisión visual integral después de integrar Home, Detalle y Mapa.
- Investigación de APIs sísmicas chilenas, condiciones de uso y deduplicación.

### Próximos incrementos del mapa

- Clustering de eventos y optimización con grandes volúmenes de datos.
- Diferenciar círculos según magnitud.
- Selección de un terremoto desde el mapa.
- Navegación Map → Detalle mediante Navigation 3.
- Filtros de período y magnitud compartidos o coordinados con Home.
- Evaluar consultas geográficas eficientes sobre Room.
- Persistir preferencias pequeñas en DataStore.
- Implementar políticas de frescura y evitar sincronizaciones redundantes.
- Evaluar failover entre OpenFreeMap y proveedores autorizados.
- Preparar cartografía offline.
- Evaluar MapLibre Compose en una pantalla experimental.

### Revisión técnica posterior al mapa

- Revisar arquitectura de presentación, dominio y datos.
- Revisar rendimiento de GeoJSON y clustering.
- Verificar ciclo de vida, memoria y recursos nativos de MapLibre.
- Revisar sincronización, Room, caché y concurrencia.
- Evaluar resiliencia de proveedores cartográficos.
- Ejecutar una revisión con razonamiento Extra High antes del rediseño visual.

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

## Pendientes de presentación y publicación

- Completar y validar la integración del mapa sísmico.
- Realizar una revisión visual integral de Home, Detalle y Mapa.
- Definir tipografía, colores, formas, componentes e iconografía.
- Revisar los modos claro y oscuro y la accesibilidad.
- Rehacer README.md después de aprobar el diseño visual.
- Documentar arquitectura, tecnologías y funcionalidades.
- Incorporar badges relevantes y capturas reales de la aplicación.
- Actualizar el roadmap y las funcionalidades pendientes.

## Observaciones para próximos incrementos
revisar version de mockweserber, me da una posibilidad un update a version 5.x
OfflineFirstEarthquakeRepository - revisar wearnigns de no uso de exception:
Parameter "exception" is never used
Parameter "exception" is never used
Parameter "exception" is never used

### Estabilidad de pruebas instrumentadas

- Investigar fallo intermitente de ActivityScenario.close()
  al esperar DESTROYED, con último estado PAUSED.
- Los fallos se observaron en MainActivityTest y
  EarthquakeDetailScreenTest durante ejecuciones completas.
- Las mismas clases pasaron al ejecutarse aisladamente.
- Mantener captura de Logcat y estado de Activities si reaparece.
- No atribuir el problema a MapLibre sin evidencia.
- Añadir una prueba independiente de integración de arranque
  con dependencias reales, sin introducirla todavía en la
  suite determinista.
- Preparar inicialización de MapLibre para futuras pruebas
  instrumentadas con MapView real.


### Bloque 14B.2B: gestión del ciclo de vida del mapa

- MapView creado y liberado mediante AndroidView.
- Contenedor nativo con propiedad única sobre MapView.
- Eventos lifecycle mediante DefaultLifecycleObserver.
- Liberación idempotente para evitar destrucciones duplicadas.
- Protección frente a callbacks asíncronos posteriores al cierre.
- Actualización de GeoJSON sin recrear el renderer.
- Prueba instrumentada nativa con estilo JSON local.

### Pendientes

- Restauración de la cámara cartográfica.
- Manejo de memoria baja y recursos nativos.
- Prueba de recreación real de Activity.
- Pruebas de actualización efectiva de GeoJsonSource.
- Medición de memoria después de ciclos repetidos.
- Diagnóstico de ActivityScenario si reaparece el fallo.
- 
### Bloque 14B.3A: clustering sísmico

- Agrupaciones nativas mediante GeoJsonOptions.
- Una única GeoJsonSource para eventos individuales y clusters.
- CircleLayers diferenciados por rangos de magnitud.
- SymbolLayer para mostrar el número de eventos agrupados.
- Estilos y umbrales visuales provisionales.
- Clustering limitado hasta zoom 11.
- Pendiente selección de clusters y navegación a eventos.
- Pendiente medición del rendimiento con grandes conjuntos de datos.