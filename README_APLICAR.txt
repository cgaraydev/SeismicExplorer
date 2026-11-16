SeismicExplorer - Bloque 2.1: recuperación de carga de MapLibre
Referencia GitHub main: b31ba1ce3b2921e7f1addb17f308ea75866c80ce

Archivos:
- REEMPLAZAR: app/src/main/java/com/cgaraydev/seismicexplorer/presentation/map/SeismicMapView.kt
- NUEVO: app/src/main/res/values/map_style_strings.xml
- NUEVO: app/src/androidTest/java/com/cgaraydev/seismicexplorer/presentation/map/SeismicMapStyleFailureTest.kt
- NUEVO: app/src/androidTest/java/com/cgaraydev/seismicexplorer/presentation/map/SeismicMapStyleLoadFailureTest.kt

No cambia el esquema de Room, el GPS, los filtros o el comportamiento de la cámara.
El código no se ha compilado en este entorno; ejecutar Gradle y validar en el dispositivo.

NOTA: el test de estilo inválido puede depender del callback nativo de MapLibre.
Si falla por timeout, revisar el logcat/resultado antes de cambiar código de producción.
