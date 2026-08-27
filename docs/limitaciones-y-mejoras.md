# Limitaciones conocidas y mejoras pendientes

Registro vivo de los **compromisos técnicos** que asumimos en la versión actual
(MVP del curso) y de lo que habría que revisar o mejorar para una versión "seria".
La idea es no perder de vista ningún atajo: cada entrada dice **qué** se dejó pasar,
**por qué** se aceptó ahora, qué **impacto** tiene y **dónde** se sigue (issue de
GitHub cuando aplica).

> Convención: cada vez que tomemos un atajo consciente, se añade aquí una entrada.
> Este archivo lo leemos David y la IA; es la fuente única de "cosas a mejorar".

---

## Motor de cinemática

### 1. Validación del pedal kickback con tolerancia ±30%
- **Qué:** el test `surgeKickbackMatchesTheReference` valida el kickback contra la
  gráfica de BikeChecker con una banda ancha del **±30%**, no con el ±3% del leverage.
- **Por qué ahora:** el kickback es hípersensible al marcado del pivote (ver punto 3),
  así que una foto marcada a mano no puede clavarlo con precisión.
- **Impacto:** el kickback es una estimación aproximada; en producto se presenta como
  banda ("medio/alto/…"), no como cifra exacta.
- **Dónde:** issue #31.

### 2. Modelo de kickback simplificado (v1)
- **Qué:** el crecimiento de cadena se calcula como el cambio de la distancia recta
  pedalier→eje; se desprecian el enrollado en el piñón y el *wheel wind-up*. El número
  lo fija solo el plato; el piñón se registra como condición de cálculo, no influye.
- **Por qué ahora:** limpio y defendible; el efecto del piñón queda tapado por el ruido
  de marcado (punto 3), así que afinarlo no mejoraría la validación.
- **Impacto:** el motor no distingue el efecto real del piñón en el kickback.
- **Dónde:** issue #31 (modelo *cog-aware* validado).

### 3. Sensibilidad extrema al marcado del pivote principal
- **Qué:** mover el pivote principal ~5 px cambia el kickback de la Surge de 19° a 35°
  (~1,6°/px). El crecimiento de cadena depende directo del arco pivote→eje.
- **Por qué ahora:** es inherente a marcar a mano sobre foto; para la v1 lo asumimos.
- **Impacto:** limita la precisión del kickback (y del crecimiento de cadena). Se agrava
  con fotos de referencia pequeñas (la Surge es 500×280 px, ~3,9 mm/px).
- **Dónde:** investigación dedicada en
  [`docs/investigaciones/sensibilidad-marcado-pivote.md`](investigaciones/sensibilidad-marcado-pivote.md);
  relacionado con #31.

### 4. Guard numérico del `acos` en el solver
- **Qué:** con geometría imposible (marcado muy malo), `MonopivotSolver.pivotAngle`
  puede pasar a `Math.acos` un valor fuera de [-1, 1] → `NaN` silencioso.
- **Por qué ahora:** las bicis válidas no lo tocan; no bloquea nada.
- **Impacto:** curvas basura sin aviso si el marcado es incoherente.
- **Dónde:** issue #17 (recortar a [-1, 1] el ruido de coma flotante; fail-fast / avisar
  en geometría realmente imposible).

---

## Alcance (decisiones de producto, no atajos)

- **Anti-squat / anti-rise fuera de la v1:** requieren la altura del centro de gravedad
  del conjunto bici+ciclista, dato que no está en la foto. Documentado en
  `fundamentos-motor-cinematica.md` §8.
- **Solo monopivote en la v1:** los sistemas de 4 barras (Horst link) llegan después
  (issue #19).
