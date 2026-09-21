# Horst con extensión rígida del amortiguador

Corrección #249, dentro de #19. La comparación con una bicicleta real y sus
curvas publicadas sigue pendiente en #237.

## Uniones y puntos

`HORST_LINK_YOKE` representa una extensión rígida respecto al eje del
amortiguador, articulada en la bieleta. No representa una extensión soldada a la
bieleta ni una barra que pueda girar libremente en ambos extremos en el plano
lateral. Ese último mecanismo necesita otra restricción física.

- `SHOCK_FRAME`: ojo del amortiguador fijado al cuadro (A).
- `SHOCK_YOKE_EYE`: ojo físico del amortiguador unido a la extensión (E).
- `YOKE_ROCKER_PIVOT`: articulación entre extensión y bieleta (J).
- `ROCKER_FRAME_PIVOT`: pivote fijo de la bieleta (O).

Los diez puntos existentes bastan. La calibración sigue siendo
`eyeToEyeMm / distancia(A, E)`. La extensión no forma parte de los 210 mm de
un amortiguador 210x55; tampoco se resta a su carrera de 55 mm.

El [manual oficial de Stumpjumper EVO](https://media.specialized.com/support/collateral/2021_STUMPJUMPER_EVO_USER_MANUAL_ENGLISH.pdf),
apartado 6.2, figuras 6.4 y 6.7 (páginas impresas 18 y 19), distingue el montaje
del ojo a la extensión del montaje de la extensión sobre los rodamientos de la
bieleta. Es una referencia de montaje, no una exportación de coordenadas ni una
validación de las curvas calculadas. El modelo 2D asume que el ángulo entre
extensión y eje del amortiguador permanece constante en el plano de la foto.

## Cálculo

En reposo, `u` es el vector unitario de A a E y `v` su perpendicular. Se expresan
los desplazamientos de E a J como `a = (J-E)·u` y `b = (J-E)·v`. La extensión
debe avanzar desde el ojo hacia su unión con la bieleta (`a > 0`). Su longitud
`sqrt(a²+b²)` se mantiene constante.

Para una compresión física `c`, la longitud del amortiguador es `s = s0-c` y la
distancia desde A hasta J es `sqrt((s+a)²+b²)`. Si están alineados, se reduce a
`s+a`. Se conserva el desplazamiento transversal marcado; no se desplazan puntos
para forzar colinealidad o coincidencia con una referencia.

1. Intersectar el círculo de centro O y radio OJ con el círculo de centro A y
   radio `sqrt((s+a)²+b²)`; elegir la solución continua respecto al paso anterior.
2. Girar la bieleta a partir de J y resolver vainas/tirantes por intersección de
   circunferencias, transportando el eje trasero con los tirantes.
3. Reconstruir E: la dirección del amortiguador es el ángulo AJ menos
   `atan2(b, s+a)` y su longitud es `s`. El ojo E no gira solidariamente con la
   bieleta.
4. Derivar las curvas usando incrementos de compresión física del amortiguador.
   Con un desplazamiento transversal, la variación de AJ no equivale a la carrera.

No hay optimizador. Una posición imposible o tangente produce un error; no se
alargan barras ni se recortan resultados para que el cálculo termine.

## Versiones y resultados guardados

Las versiones corregidas son `horst-link-yoke-v2` y
`horst-link-yoke-reference-v2`. V10 permite guardarlas y conserva las versiones
anteriores. `monopivot-reference-v2` y los motores Horst directos no cambian.
La migración no recalcula análisis, no borra datos ni regenera explicaciones.
Los resultados yoke v1 que pudieran existir no quedan corregidos por esta
migración: requieren un análisis nuevo. La validación de respuestas del asistente
acepta la versión de referencia yoke v2 y mantiene el rechazo de yoke v1.

## Evidencia y siguiente prueba

- Regresión sintética: mecanismo alcanzable de 210x55 que el modelo anterior
  bloqueaba tras unos 7 mm y que ahora completa 101 posiciones.
- Oráculo independiente: paralelogramo con posición del eje conocida y una
  posición final de bieleta construida a partir de un ángulo elegido.
- Conservación de longitudes y desplazamientos de extensión, ambos signos del
  desplazamiento transversal, orientación invertida de la foto y cambio de escala.
- Contrato HTTP de diez puntos, cinco curvas y persistencia de versiones.

Estos tests comprueban el modelo; no certifican precisión ±3% en una Stumpjumper.
Para #237 hay que conservar foto y coordenadas exactas, repetir 210x55, Full 29,
150 mm, 32/51 y sag 25%, y comparar muestras a iguales recorridos. La referencia
publicada indica CG 1065 mm, mientras el modelo estándar usa 1100 mm: los
porcentajes anti-squat/anti-rise no deben compararse como condiciones idénticas.
No se ajustan puntos ni tolerancias para ocultar diferencias.
