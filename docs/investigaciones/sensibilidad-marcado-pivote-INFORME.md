# Informe — sensibilidad del kickback al marcado del pivote

> **Informe histórico, no especificación de interfaz.** Decisión posterior de David:
> no exigir cinco clics ni promediar marcas. Usar cruz por punto, zoom y deshacer.
> La guía vigente es `../frontend-arranque.md`.

> Responde al encargo [`sensibilidad-marcado-pivote.md`](sensibilidad-marcado-pivote.md).
> Todo el análisis se hizo **fuera del código de producción**, con una réplica en
> Python del motor (paquete `com.bikematch.kinematics`) validada contra las curvas de
> referencia. No se ha tocado el motor ni ningún otro archivo. Los scripts son
> desechables (vivieron en un scratchpad fuera del repo); al final se indica cómo
> reproducirlos.

---

## 1. Planteamiento

El objetivo era **entender por qué el pedal kickback es tan sensible al marcado del
pivote** y **probar con números** qué soluciones lo reducen de verdad.

Método:

1. **Réplica fiel del motor** en Python (solver monopivote, calibrado px→mm de los
   fixtures, curvas de leverage y kickback), verificada reproduciendo las referencias.
2. **Sensibilidad por punto** con diferencias finitas: cuánto mueve cada métrica un
   error de 1 px en cada punto marcado (pivote, eje, anclajes, pedalier).
3. **Descomposición causa raíz**: separar la sensibilidad *física del método* (°/mm)
   de la *resolución de la foto* (mm/px).
4. **Propagación de ruido de marcado** (Monte Carlo, 15–20k muestras): con ruido
   gaussiano de σ píxeles por punto, ¿qué banda del 95% sale en cada métrica? ¿Qué
   precisión hace falta para ±10%?
5. **Prototipado y medida de soluciones** candidatas.

**Fidelidad de la réplica** (misma entrada que los fixtures, en píxeles):

| Bici | mm/px | Recorrido | Leverage ini/fin | Kickback pico |
|---|---|---|---|---|
| Surge (500×280) | 3,93 | 164,4 mm (decl. 164) | 2,548 / 2,504 (ref 2,55 / 2,50) | 26,5° (banda test ±30% = 23,8–44,2) |
| Stage 6 (1800×1200) | 1,29 | 153,2 mm (decl. 150) | 2,833 / 2,732 (ref 2,775 / 2,675) | 22,0° (sin ref pública) |

Todo cae dentro de tolerancia → la réplica reproduce el motor y sirve de laboratorio.

---

## 2. Análisis cuantitativo

### 2.1. Qué punto es crítico (error de 1 px, Orange Surge)

Magnitud del gradiente de cada métrica ante 1 px de error en cada punto (peor
dirección):

| Punto | **Kickback** (°/px) | Leverage ini (/px) | Recorrido (mm/px) |
|---|---|---|---|
| **Pivote principal** | **1,62**  (casi todo en *y*) | 0,055 (2,2%) | 3,45 (2,1%) |
| **Pedalier (BB)** | **1,15** | 0 (no entra) | 0 (no entra) |
| Anclaje basculante | 0,53 | 0,044 (1,7%) | 3,20 (1,9%) |
| Eje trasero | **0,05** | 0,021 (0,8%) | 1,33 (0,8%) |
| Anclaje chasis | 0,03 | 0,009 (0,3%) | 0,13 (0,1%) |

Lecturas:

- **El pivote domina el kickback** (1,62 °/px) y **el eje trasero apenas influye**
  (0,05 °/px). Contraintuitivo, pero correcto: el kickback depende del *arco* que
  traza el eje, y el pivote es el **centro** de ese arco; mover el eje solo desplaza
  el arco entero y el crecimiento de cadena (una diferencia) casi no cambia.
- El **pedalier es el segundo crítico** (1,15 °/px): es el otro extremo de la línea
  de cadena.
- 1,62 °/px reproduce el **"~1,6 °/px"** del registro de limitaciones.

### 2.2. Reproducción del "19° → 35°"

Moviendo solo el pivote en vertical (su peor eje) en la Surge:

| Δpivote (px) | −5 | −2 | 0 | +2 | +5 |
|---|---|---|---|---|---|
| Kickback (°) | **35,5** | 29,9 | 26,5 | 23,4 | **19,1** |

±5 px → **19,1°…35,5°**. Es exactamente el fenómeno documentado. En la Stage 6 (foto
3× más fina) el mismo ±5 px solo da 19,6°…24,5°: **la banda se estrecha 3×**.

### 2.3. Propagación de ruido de marcado (banda del 95%, ± % del valor)

Ruido gaussiano independiente de σ px en **todos** los puntos:

| Bici (mm/px) | σ = 0,5 px | σ = 1 px | σ = 2 px |
|---|---|---|---|
| **Surge (3,93)** — kickback | ±7,6% | **±15,2%** | ±30,4% |
| Surge — leverage / recorrido | ±3% | ±6% | ±12% |
| **Stage 6 (1,29)** — kickback | ±2,8% | **±5,5%** | ±11,0% |
| Stage 6 — leverage / recorrido | ±1% | ±2% | ±4% |

- **σ = 2 px ≈ ±30%**: la banda ancha del test actual equivale a tolerar ~2 px de
  marcado. Es decir, el ±30% no es arbitrario, es "hand-marking flojo en foto pequeña".
- **El kickback es ~2,6× más frágil** que leverage/recorrido ante el mismo ruido.
- En la Surge, a σ = 1 px la **propia leverage ya se sale del ±3%** (±6%). Que hoy
  pase el test implica que el fixture está marcado en realidad a **~0,5 px**.

### 2.4. ¿Cuánta precisión hace falta para el kickback?

| Precisión de marcado | Surge (3,93 mm/px) | Stage 6 (1,29 mm/px) |
|---|---|---|
| σ = 2,0 px | ±30% | ±11% |
| σ = 1,0 px | ±15% | ±5,5% |
| σ = 0,5 px | ±7,6% | ±2,7% |
| σ = 0,25 px | ±3,8% | ±1,4% |

Para **±10%** en la Surge hace falta **σ ≈ 0,65 px** (muy exigente en 500×280). En la
Stage 6 se cumple **±10% incluso con marcado flojo (~1,8 px)**.

---

## 3. Causa raíz

Son **dos causas que se multiplican**: una geométrica (del método) y una de resolución
(de la foto).

### 3.1. Amplificador geométrico (por qué el kickback es delicado en sí)

El modelo calcula `kickback = crecimiento_cadena / radio_paso_plato`. En la Surge:

- Radio del basculante (pivote→eje): **R = 487 mm**.
- Línea de cadena BB→eje: **443 mm en reposo → 475 mm a tope**.
- **Crecimiento = 31,8 mm**, que es la **diferencia de dos distancias de ~443 mm**
  casi iguales (**solo el 7,2%**).

Esa **casi-cancelación** es el amplificador: el resultado útil (32 mm) es un 7% de las
magnitudes que se restan, así que un error pequeño que afecte de forma distinta al
reposo y al final se nota mucho en la diferencia. Además, el eje se desplaza casi
**perpendicular** a la línea de cadena (cos ≈ 0,015), con lo que el crecimiento es un
efecto casi de **segundo orden** → numéricamente sensible por naturaleza.

Y el pivote es el peor sitio para equivocarse porque es el **centro del arco**: un error
suyo cambia a la vez (a) la posición del arco, (b) el radio R y (c) el ángulo de giro
del basculante por unidad de carrera. Toca los dos extremos de esa resta a la vez.

Medida: la sensibilidad **física** del método es **~0,4 °/mm** de error del pivote, y
—clave— es **casi igual en las dos bicis** (Surge 0,41 °/mm, Stage 6 0,38 °/mm). No es
que una bici sea "más sensible": el método lo es por igual.

### 3.2. Multiplicador de resolución (por qué la foto pequeña lo empeora)

El calibrado convierte 1 px de error en **(mm/px) mm** de error real. Como el kickback
va en mm y el radio del plato es fijo:

```
sensibilidad(°/px)  =  sensibilidad(°/mm)  ×  (mm/px)
      1,62 (Surge)  =        0,41          ×   3,93
      0,49 (Stage6) =        0,38          ×   1,29
```

**El salto de 1,62 a 0,49 °/px es casi todo resolución** (mm/px): la Surge (3,93 mm/px,
foto 500×280) hace que cada píxel "valga" 3× más milímetros que la Stage 6
(1,29 mm/px, 1800×1200). La parte del método (0,4 °/mm) es prácticamente irreducible;
la parte de resolución **sí es accionable**.

### 3.3. Fórmula unificadora

Todas las palancas actúan sobre el **mismo producto**. La banda del 95% del kickback es,
con muy buena aproximación:

```
banda%  ≈  k · (mm/px) · σ_px / √K        con k ≈ 3,8–4,3  (geometría de la bici)
```

donde σ_px es la precisión de marcado y K el nº de clics promediados por punto. Subir
resolución, promediar clics o marcar con zoom son **la misma medida** (bajar ese
producto) y **se combinan de forma multiplicativa**.

---

## 4. Soluciones probadas (ordenadas por relación esfuerzo/impacto)

Todas medidas sobre el caso difícil (**Surge, σ = 1 px, baseline ±15,2%**). Ninguna
toca el motor ni su API; son de **captura de datos / frontend / guía**.

| # | Solución | Mejora medida (Surge) | Qué cambiar (alto nivel) | Esfuerzo | Riesgo | ¿Rompe API motor? |
|---|---|---|---|---|---|---|
| 1 | **Guía de resolución mínima + validación** (rechazar/avisar si mm/px alto) | 1500 px → **±5,1%**; 750 px → ±10% | Frontend: avisar según mm/px del calibrado; una línea de guía de captura | **Bajo** | Bajo | No |
| 2 | **Promediar K clics** en puntos críticos (pivote+BB) | K=5 pivote+BB → **±7,7%**; todos → ±6,8% | Frontend: capturar K clics por punto y usar el centroide | Bajo-Medio | Bajo | No |
| 3 | **Marcado con zoom/lupa** (sub-píxel, σ 1→0,3) | **±4,6%** | Frontend: marcador con magnificador | Medio | Bajo | No |
| 4 | **Fotos de referencia de más resolución** (re-sacar/re-buscar) | ±5–7% | Datos: sustituir fotos pequeñas (p. ej. la Surge) | Bajo/bici | Bajo | No |
| 5 | **Combo 1+2** (foto 1500 px + pivote×5) | **±3,6%** (grado leverage) | 1 + 2 juntas | Medio | Bajo | No |
| — | **Suavizar/ajustar el arco** | **~0% (inútil)** | — | — | Medio | Quizá |
| — | Modelo *cog-aware* (#31) | No aplica a la sensibilidad | — | — | — | — |

Notas:

- **Promediar solo el pivote se estanca en ~±10%** (K=5 → ±10,8%): el "suelo" lo pone el
  ruido del **resto** de puntos, sobre todo el pedalier. Por eso hay que promediar
  **pivote + pedalier** como mínimo.
- **"Suavizar el arco" no sirve, y está probado**: el recorrido del eje **ya es un
  círculo exacto** (desviación 1×10⁻¹³ mm con entrada limpia *y* con entrada ruidosa).
  No hay temblor numérico que suavizar; el error vive en el **centro y el radio** (las
  entradas), y suavizar la curva de salida no los recupera. Añadiría complejidad sin
  ganancia → **descartada**.
- **Ya validado dentro del repo**: el fixture **Stage 6 está "re-marcado con lupa sobre
  foto 1800×1200"** (lo dice su Javadoc). El equipo ya aplicó, de hecho, las soluciones
  1, 3 y 4 — y por eso la Stage 6 es ~3× más robusta que la Surge. No es teoría: es el
  contraste que ya existe entre las dos bicis del proyecto.

---

## 5. Recomendación

**Recomiendo la opción (a): mantener la v1 tal cual ahora**, y programar la mejora para
cuando llegue el sprint de captura/frontend. Motivos:

1. **El producto no necesita ±5% hoy.** El kickback se presenta como banda
   ("medio/alto"), no como cifra exacta (limitación #1). El ±30% actual es un
   compromiso **consciente y ya documentado** (#31), y equivale a tolerar ~2 px de
   marcado: es una validación **robusta**, no rota.
2. **No hay nada que arreglar en el motor.** Esto se ha confirmado: la sensibilidad es
   un problema de **calidad de la entrada**, no de cálculo. Ninguna solución eficaz
   toca el motor ni su API, y la única candidata "de motor" (suavizar el arco) está
   **probada como inútil**. Meter mano al motor ahora sería esfuerzo mal colocado y
   contra la disciplina de sprints.
3. **La mejora de verdad es de frontend/datos** (resolución + promediado + zoom), que
   cae en un sprint posterior. Adelantarla ahora sería scope creep.

**Cuando llegue el sprint de marcado**, el plan de máxima relación esfuerzo/impacto,
por orden, es:

1. **Guía + validación de resolución** (solución 1): una línea de guía de captura y un
   aviso cuando el mm/px del calibrado sea alto. Coste mínimo, quita de golpe el factor
   dominante. Objetivo sugerido: **mm/px ≤ ~1,3** (equivale a que el amortiguador
   ocupe ≥ ~160 px, o subir fotos de ≥ ~1500 px de ancho).
2. **Promediado de pivote + pedalier** (solución 2) y/o **marcador con zoom** (solución
   3) en la UI de marcado. Con 1+2 se llega a **±3–5%**, grado leverage.
3. **Sustituir las fotos de referencia pequeñas** (la Surge, solución 4) por versiones
   de más resolución, para estrechar también los tests de referencia.
4. Solo entonces, **estrechar la banda del test** del kickback de ±30% a ~±10–15%, como
   prueba objetiva de que la mejora funciona.

En una frase: **no es un bug del motor, es resolución de foto × precisión de marcado; la
solución es barata pero de frontend/datos, así que se deja para su sprint y hoy la v1 se
mantiene.**

---

## Anexo — cómo reproducir

Réplica del motor en Python puro (sin dependencias). Scripts en un scratchpad fuera del
repo (desechables):

- `engine.py` — réplica del solver + curvas + fixtures (píxeles).
- `step1_baseline.py` — valida la réplica contra las referencias.
- `step2_sensitivity.py` — sensibilidad por punto (diferencias finitas).
- `step3_rootcause.py` — reproduce 19°→35° y separa °/mm vs °/px.
- `step4_noise.py` — Monte Carlo del ruido de marcado y barrido de resolución.
- `step5_solutions.py` — mide cada solución y demuestra que suavizar no sirve.
- `step6_diagnostic.py` — números físicos de la causa raíz.

Supuestos del Monte Carlo: ruido gaussiano independiente por coordenada y punto; banda
del 95% por percentiles 2,5/97,5; semilla fija (reproducible). Las cifras son estables a
±0,2 puntos porcentuales entre corridas.
