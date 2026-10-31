# Temas: cómo funcionan y cómo agregar uno

Un tema dibuja los **dos carteles** que aparecen al cambiar el volumen: el panel inferior y el lateral. Los 12 que trae la app (11 de los mockups + Clásico) están en `app/src/main/java/local/volumenmaestro/overlay/themes/`. Los diseños originales, animados, se pueden ver en [`mockups/temas.html`](mockups/temas.html).

## El contrato

```kotlin
interface Theme {
    val id: String          // se guarda en la config ("sable", "neon", …)
    val name: String        // lo que se ve en la pestaña Temas
    val description: String // una línea
    fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) // 960 × 150
    fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState)   // 108 × 430
}
```

`OverlayState` trae todo lo necesario:

| Campo | Qué es |
|---|---|
| `level` | Nivel animado 0..1 (se acerca suave al real; en silencio baja a 0) |
| `index` | El número a mostrar (0..100) |
| `muted` | Si está en silencio |
| `timeSec` | Segundos desde que apareció el cartel (animaciones de entrada y continuas) |
| `appear` | 0→1 en 0,18 s al aparecer, 1→0 en 0,3 s al irse |
| `sinceChangeSec` | Segundos desde el último cambio (chispas, destellos, glitches) |
| `message` | Si no es null, un mensaje en lugar del volumen (por ejemplo "Volumen Maestro: ENCENDIDO"); el lateral no se muestra |

## Las 4 reglas

1. **Opaco desde el primer cuadro.** El panel inferior tiene que cubrir siempre (también con `appear=0` y durante la salida) el rectángulo **x 36..924 / y 11..142**, que es donde está el cartel de TCL. Los temas lo resuelven pintando primero un rectángulo opaco ahí. El lateral se pinta entero.
2. **Sin estado.** El tema es un `object` que no recuerda nada entre cuadros: todo sale de `OverlayState`. Partículas con fórmulas cerradas (posición = origen + v·t + ½·g·t²) o arrays fijos sembrados. Así la vista previa de la app y la TV coinciden.
3. **Sin allocations en `draw`.** Paint, Path, RectF y shaders se crean una vez, como campos (los tamaños son fijos). Nada de `BlurMaskFilter` (con aceleración por hardware no anda bien): los resplandores se hacen con trazos superpuestos más anchos y de alfa bajo, o con gradientes. `setShadowLayer` solo en texto.
4. **Fuentes del sistema**: `Typeface.MONOSPACE`, `serif`, `sans-serif-condensed`, `sans-serif-light`, etc.

## Agregar un tema

1. Diseñalo (podés sumar uno a `docs/mockups/temas.html`, que dibuja en las mismas coordenadas).
2. Creá `overlay/themes/MiTema.kt`:

   ```kotlin
   object MiTema : Theme {
       override val id = "mitema"
       override val name = "Mi tema"
       override val description = "Una línea que lo describa."
       private val placa = Paint().apply { color = 0xFF101010.toInt() }

       override fun drawBottom(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
           canvas.drawRect(36f, 11f, 924f, 142f, placa) // regla 1: tapa el cartel de TCL siempre
           // … tu diseño, usando s.level, s.index, s.muted, s.timeSec …
       }

       override fun drawSide(canvas: Canvas, w: Int, h: Int, s: OverlayState) {
           canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), placa)
           // …
       }
   }
   ```
3. Sumalo a la lista de `ThemeRegistry.all` en `overlay/Theme.kt` (el orden es el de la grilla).
4. `/instalar` y probalo: en la app, pestaña Temas → elegirlo → "Probar en pantalla". Mirá que no se asome nada del cartel de TCL abajo ni de la barra gris a la derecha.

## Los temas actuales

| id | Nombre | Lateral |
|---|---|---|
| `sable` | Sable láser rojo | Sable vertical y cristal que late |
| `neon` | Neón cyberpunk | Vúmetro L/R |
| `medieval` | Medieval | Antorcha cuya llama crece |
| `retro` | Retro analógico | Escala con puntero |
| `vidrio` | Minimal vidrio | Píldora tipo iOS |
| `arcade` | Arcade 8-bit | Torre de bloques y moneda |
| `espacial` | Espacial | Cohete que despega |
| `ondas` | Ondas líquidas | Cuerdas que vibran |
| `steampunk` | Steampunk | Cremallera y manómetro |
| `acuatico` | Acuático | Tubo de agua con pez |
| `terminal` | Terminal hacker | Lluvia de caracteres |
| `clasico` | Clásico | Barra simple (el de la v1) |
