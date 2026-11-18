
# SeismicExplorer - Desarrollo

Documento interno de referencia sobre tecnologías, arquitectura, funcionalidades, decisiones técnicas y trabajo pendiente.

## Tecnologías

### Plataforma y herramientas
- Android Gradle Plugin 9.5.1, Gradle 9.9.1 y JDK 25.
- Kotlin 2.4.21, Java 17 y Gradle Kotlin DSL.
- compileSdk 37, targetSdk 37 y minSdk 29.
- GitHub Actions para integración continua.

### Android y presentación
- Jetpack Compose y Material 3 (BOM 2026.09.00).
- AndroidX Core 1.19.1 y Lifecycle 2.11.0.
- Activity Compose 1.13.0.
- Navigation 3 1.2.1.
- Kotlin Coroutines 1.11.0 y StateFlow.
- MapLibre Native Android 13.7.1 y OpenFreeMap.

### Datos y dependencias
- Dagger Hilt 2.60.1 y KSP 2.3.12.
- Room 3.0.3 y AndroidX SQLite Framework 2.7.1.
- Retrofit 3.0.0 y Kotlin Serialization JSON 1.13.0.
- Kotlin Serialization Converter 3.0.0.
- MockWebServer 4.12.0 para pruebas HTTP.

### Calidad
- JUnit 4, AndroidX Test, Espresso y Compose UI Test.
- Kotlin Coroutines Test y pruebas instrumentadas Android.
- Android Lint y validaciones automatizadas mediante CI.

## Arquitectura

- Clean Architecture pragmática y principios SOLID.
- Separación entre presentación, dominio y datos.
- Flujo de datos unidireccional (UDF).
- ViewModels con StateFlow y observación del ciclo de vida.
- Inyección de dependencias mediante Hilt.
- Modelos de dominio independientes de Android y servicios externos.
- Enfoque offline-first con Room como fuente observable.
- Sincronización con USGS y conservación del caché local.
- Actualización de eventos según su fecha de revisión.
- Protección frente a sincronizaciones obsoletas y cancelaciones.
- Operaciones de procesamiento intensivo fuera del hilo principal.
- Navegación tipada con Navigation 3 y estado restaurable.

## Funcionalidades implementadas

### Datos sísmicos
- Integración con feeds USGS de 24 horas, 7 días y 30 días.
- Persistencia local de terremotos mediante Room.
- Sincronización inicial y actualización manual.
- Tratamiento de errores de red y respuestas inválidas.
- Conservación de datos locales ante fallos de sincronización.
- Consultas observables y actualización consistente de registros.

### Pantallas
- Home con listado sísmico, filtros y actualización.
- Detalle de terremotos mediante consulta local por ID.
- Navegación entre Home, Mapa y Detalle.

### Mapa
- Representación de terremotos mediante MapLibre Native.
- OpenFreeMap como proveedor cartográfico principal.
- Conversión de eventos a GeoJSON.
- Clustering nativo y diferenciación visual por magnitud.
- Selección de terremotos y navegación al detalle.
- Ampliación de zoom al seleccionar agrupaciones.
- Conservación de cámara durante navegación y rotación.
- Actualización de datos sin recrear el mapa.

## Pruebas y validaciones

- Pruebas JVM de dominio, repositorios, ViewModels y navegación.
- Pruebas HTTP con MockWebServer.
- Pruebas instrumentadas de Room y consultas observables.
- Pruebas Compose con dependencias controladas.
- Pruebas de ciclo de vida de MapLibre.
- Validación de integridad GeoJSON con grandes colecciones.
- Pruebas nativas de clustering y actualización de GeoJSON.
- Validaciones funcionales en dispositivo físico.

## Funcionalidades pendientes

- Ampliar los datos sísmicos disponibles desde USGS.
- Diferenciar enlaces públicos y recursos de detalle GeoJSON.
- Incorporar controles de cámara y filtros cartográficos.
- Evaluar consultas geográficas eficientes sobre Room.
- Mejorar las políticas de frescura y sincronización.
- Investigar fuentes sísmicas chilenas y deduplicación de eventos.
- Evaluar DataStore para preferencias persistentes.
- Explorar proveedores cartográficos alternativos y soporte offline.
- Realizar el rediseño visual de Home, Detalle y Mapa.
- Revisar accesibilidad y compatibilidad con temas claro y oscuro.
- Actualizar README, capturas y documentación para publicación.

## Observaciones técnicas

- Evaluar actualización de MockWebServer a la versión 5.x.
- Revisar advertencias de parámetros no utilizados en el repositorio.
- Investigar fallos intermitentes de ActivityScenario.close().
- Revisar comportamiento de ADB ante conexiones inalámbricas duplicadas.
- Medir rendimiento y memoria de MapLibre con grandes volúmenes de datos.
- Ampliar pruebas de navegación, recreación y recursos nativos.
- Revisar periódicamente arquitectura, dependencias y rendimiento.
- Realizar una auditoría técnica integral antes del rediseño visual.
- Priorizar estabilidad, simplicidad y mantenibilidad sobre abstracciones innecesarias.


pendiente:
seccion de sabias que o preguntas frecuentes respecto a los sismos y terremotos, hacer una investigacion exhaustiva para q la app tenga un tono cientifico en cuanto a los datos q mostramos
en el detalle de eventos, junto a q tipo de escala se utiliza, podemos ponder un mini icono con un signo de pregunta q nos abra un composable o una screen nueva en q se ensene sobre las escalas de medicion
seccion guardados
seccion ajustes
animaciones, figma, personalizacion
incluir otrasd apis q aporten funcionalidades nuevas o detalles a los eventos
verificar por apis chilenas q aporten detalles 
seccion terremotos mas grandes de la historia
seccion aprendizaje/wiki
cambiar a espanol la informacion recibida de usgs
debe haber un ajusta para seleccionar modo lght o dark o simplemente seleccionar la eleccion de modo del sistema
filtro de screen resumen, el principal de arriba q selecciona tiempo, cuando se expande, las opciones se ven mal porque quedan en el start, el espacio q ocupan tiene q ser del mismo tamano q la tarjeta q se expande
en el detalle, en la parte q dice fecha y hora local, se puede incluir tambien la hora UTC
el identificador usgs puede llevar un icono para copiarlo al portapaleles en caso de q este identificador sirva para algo



