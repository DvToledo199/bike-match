# Modelos de cuatro barras (Horst link)

BikeMatch calcula tres variantes de cuatro barras. Las tres comparten el mismo
mecanismo y solo cambian en **quién comprime el amortiguador**. Los fundamentos
generales (sólidos rígidos, cruce de circunferencias, barrido en 100 pasos) están en
[`fundamentos-motor-cinematica.md`](fundamentos-motor-cinematica.md).

## El mecanismo común

La cuatro barras es un lazo de cuatro piezas: pivote principal → pivote Horst → unión del
tirante con la bieleta → pivote de la bieleta en el cuadro. La vaina gira en el pivote
principal, la bieleta gira en el cuadro y el tirante une ambas y **lleva el eje trasero**
como un punto rígido suyo.

Puntos comunes (8 de los 9 o 10 que se marcan):

- `MAIN_PIVOT`, `HORST_PIVOT`, `ROCKER_FRAME_PIVOT`, `ROCKER_SEATSTAY_PIVOT`: los cuatro
  pivotes del lazo.
- `SHOCK_FRAME`: ojo del amortiguador fijado al cuadro.
- `BOTTOM_BRACKET`, `REAR_AXLE`, `FRONT_AXLE`.

En cada paso, una vez colocada la bieleta, el pivote Horst sale del cruce de dos
circunferencias (una centrada en el pivote principal con el largo de la vaina y otra en la
unión de la bieleta con el largo del tirante). El eje trasero se traslada con el tirante.
No hay optimizador: una posición imposible o tangente produce un error 400 en lugar de
alargar piezas o recortar resultados.

El freno está en el tirante. Por eso anti-squat y anti-rise usan el **centro instantáneo
del tirante**: el cruce de la recta de la vaina con la recta de la bieleta, que puede
estar muy lejos o en el infinito sin que el cálculo falle (ver
[`modelo-referencia-cinematica.md`](modelo-referencia-cinematica.md)).

| Variante | Tipo | Ojo móvil del amortiguador | Puntos | Versiones del motor |
|---|---|---|---|---|
| Amortiguador en la bieleta | `HORST_LINK` | `SHOCK_ROCKER` | 9 | `horst-link-v1`, `horst-link-reference-v1` |
| Con extensión rígida del amortiguador | `HORST_LINK_YOKE` | `SHOCK_YOKE_EYE` + `YOKE_ROCKER_PIVOT` | 10 | `horst-link-yoke-v2`, `horst-link-yoke-reference-v2` |
| Amortiguador empujado por los tirantes | `HORST_LINK_SEATSTAY` | `SHOCK_SEATSTAY` | 9 | `horst-link-seatstay-v1`, `horst-link-seatstay-reference-v1` |

La versión `-reference-` se usa cuando se elige configuración de ruedas (siempre, desde la
web) e incluye las cinco curvas. En todas, la calibración es
`ojo a ojo / distancia(SHOCK_FRAME, ojo móvil real)`.

## 1. Amortiguador en la bieleta

El amortiguador va del cuadro a la bieleta. Su longitud fija el giro de la bieleta con un
solo cruce de circunferencias: una centrada en el pivote de la bieleta (radio: bieleta
hasta el ojo) y otra en el anclaje del cuadro (radio: longitud del amortiguador en ese
paso). Girada la bieleta, se resuelve el resto del lazo.

## 2. Con extensión rígida del amortiguador

Corrección #249. La extensión es rígida respecto al eje del amortiguador y está articulada
en la bieleta. No representa una extensión soldada a la bieleta ni una barra libre en
ambos extremos: ese último mecanismo necesitaría otra restricción física.

- `SHOCK_YOKE_EYE`: ojo físico del amortiguador unido a la extensión (E).
- `YOKE_ROCKER_PIVOT`: articulación entre extensión y bieleta (J).

La calibración usa el ojo físico E: la extensión no forma parte del ojo a ojo del
amortiguador (por ejemplo, 210 mm en un 210 × 55) ni se resta a su carrera.

**Cálculo.** En reposo, `u` es el vector unitario del anclaje del cuadro A hacia E y `v` su
perpendicular. Los desplazamientos de E a J son `a = (J−E)·u` y `b = (J−E)·v`; la extensión
avanza desde el ojo hacia la bieleta (`a > 0`) y su longitud `√(a²+b²)` es constante. Para
una compresión `c`, el amortiguador mide `s = s₀ − c` y la distancia de A a J es
`√((s+a)² + b²)`.

1. Cruzar la circunferencia de centro O (pivote de la bieleta) y radio OJ con la de centro
   A y radio `√((s+a)² + b²)`; elegir la solución continua respecto al paso anterior.
2. Girar la bieleta a partir de J y resolver el resto del lazo.
3. Reconstruir E: la dirección del amortiguador es el ángulo AJ menos `atan2(b, s+a)` y su
   longitud es `s`. E no gira solidariamente con la bieleta.
4. Derivar las curvas con incrementos de compresión física del amortiguador; con un
   desplazamiento transversal, la variación de AJ no equivale a la carrera.

El modelo 2D asume que el ángulo entre extensión y eje del amortiguador se mantiene
constante en el plano de la foto.

**Versiones.** Las versiones corregidas son las `v2`. La migración V10 las admite y
conserva las `v1` anteriores sin recalcularlas: un resultado `v1` necesita un análisis
nuevo. La web rechaza respuestas `v1`.

## 3. Amortiguador empujado por los tirantes

Issue #288. El amortiguador va del cuadro a un punto de los tirantes, casi siempre una
prolongación suya más allá de la bieleta; la bieleta solo guía los tirantes y no toca el
amortiguador.

Si una bici así se calcula como si el amortiguador fuera en la bieleta, la bieleta se
queda corta: con una bici real de 160 mm y un amortiguador de 230 × 65 mm, ese modelo solo
admitía 25 mm de compresión. Con el modelo por los tirantes, los 65 mm dan 162,9 mm de
recorrido, un 1,8 % más que el declarado.

**Cálculo.** Aquí la longitud del amortiguador depende de todo el lazo, así que un solo
cruce de circunferencias no basta. El motor usa como entrada el giro de la bieleta:

1. Para un giro dado, el lazo se resuelve igual que en la variante 1 y los tirantes llevan
   consigo el eje y el ojo del amortiguador como un sólido rígido.
2. En cada paso de compresión, el giro avanza en saltos de 0,5° hasta que el amortiguador
   queda más corto que la longitud buscada, y después se afina por bisección hasta clavarla.
3. Si el lazo no se puede montar o la bieleta supera los 120° de giro sin llegar a la
   compresión pedida, el cálculo se rechaza con un error 400.

Como cada paso parte del anterior, el mecanismo no puede saltar a la otra forma de
montarse.

## Evidencia y validación pendiente

- **Tests del modelo:** conservación de longitudes en todo el barrido, ambos signos del
  desplazamiento de la extensión, foto invertida o cambiada de tamaño, contratos HTTP y
  persistencia de las versiones. Un oráculo independiente (paralelogramo con posición del
  eje conocida) comprueba la extensión rígida.
- **Comparación entre modelos:** cuando el ojo del amortiguador coincide con la unión de
  los tirantes y la bieleta, ese punto pertenece a las dos piezas y el modelo por los
  tirantes coincide paso a paso con el de bieleta.
- **Bicis reales:** además del recorrido anterior (1,8 %), la curva de palanca de una
  cuatro barras coincidió con la publicada por su fabricante (de 2,97 a 2,20 frente a 2,95
  a 2,15).
- **Pendiente:** una validación sistemática contra curvas publicadas, conservando foto y
  coordenadas y comparando muestras a iguales recorridos (#222, #237). Las referencias
  externas pueden usar otra altura de centro de gravedad (por ejemplo, 1065 mm frente a
  los 1100 mm del modelo), así que sus porcentajes de anti-squat y anti-rise no se
  comparan como condiciones idénticas. No se ajustan puntos ni tolerancias para ocultar
  diferencias.
