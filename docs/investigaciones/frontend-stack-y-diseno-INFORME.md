# Informe — stack y diseño del frontend

> Responde al encargo [`frontend-stack-y-diseno.md`](frontend-stack-y-diseno.md).
> El análisis se ha hecho **sin tocar el código de producción**: no se ha instalado ninguna
> dependencia en el repo, no se ha modificado `frontend/` ni `backend/`, y no se ha abierto
> ningún PR. Los datos de versión y estado de mantenimiento de cada librería se verificaron
> contra su documentación oficial y npm **a fecha de septiembre de 2026**; conviene
> re-confirmarlos con un *spike* (prueba corta y desechable) cuando arranque el sprint.
> Este informe **recomienda; la decisión final es del responsable del proyecto.**

---

## 0. Contexto técnico que condiciona las decisiones

Antes de comparar librerías conviene fijar **qué tiene que renderizar exactamente el front**,
porque eso pesa más que cualquier moda. Todo sale del contrato del endpoint ya construido
(`POST /api/kinematics/preview`).

**Lo que el front envía** (resumen del contrato): una lista de **puntos marcados** sobre la
foto (cada uno con su etiqueta: pivote principal, anclajes del amortiguador, pedalier, ejes),
la calibración **eye-to-eye** del amortiguador (distancia entre sus anclajes, en mm) y los
**parámetros** de ficha (carrera, dientes de plato/piñón, recorrido declarado, sag).

**Lo que el front recibe y tiene que dibujar** — tres series de puntos (~100 cada una) y un
bloque de descriptores numéricos:

| Salida | Forma de los datos | Cómo se dibuja |
|---|---|---|
| **Leverage ratio** (relación de palanca) | ~100 pares `wheelTravelMm → ratio` | curva de línea normal, X = recorrido de rueda, Y = ratio |
| **Pedal kickback** (retroceso de pedal) | ~100 pares `wheelTravelMm → kickbackDegrees` | curva de línea normal; se presenta como banda ("medio/alto"), no cifra exacta |
| **Axle path** (trayectoria del eje) | ~100 pares `x, y` **en mm** | **caso especial**: no es `y = f(x)`; es una curva 2D paramétrica (el eje traza un arco), y ambos ejes van en mm |
| Descriptores de leverage | LR inicial/sag/final/medio, progresión total y útil, pendientes, **forma en 3 fases**, **banda de progresión** | tarjetas/etiquetas numéricas + indicadores de forma |
| Descriptores de trayectoria | `maxRearwardMm`, `atTravelPercent` (retroceso máximo y dónde ocurre) | tarjetas numéricas |
| **TravelCheck** | recorrido calculado vs declarado, desviación %, `withinTolerance` | aviso/alerta si se sale de ±10 % |

Tres consecuencias que guían toda la comparativa:

1. **El rendimiento NO es un criterio de peso.** ~100 puntos por curva es un volumen trivial:
   cualquier librería seria lo dibuja sin despeinarse. Por tanto **no** hay que optimizar por
   velocidad; hay que optimizar por lo demás (sobre todo por *cuán explicable* queda el código).
2. **La trayectoria del eje es el único gráfico "raro".** Al ser una curva 2D (x e y varían a
   la vez, ambos en mm), el eje horizontal ya no es el "recorrido" independiente. Si se deja
   que cada eje se auto-escale, la forma sale **distorsionada** (un retroceso de pocos mm
   frente a ~150 mm de recorrido se vería como una curva dramática que no es real). Es una
   decisión de **diseño** (mantener la misma escala mm en ambos ejes) más que de librería,
   pero condiciona qué librería resulta cómoda.
3. **El marcado de puntos es el punto crítico de precisión**, no un detalle cosmético. La
   investigación [`sensibilidad-marcado-pivote-INFORME.md`](sensibilidad-marcado-pivote-INFORME.md)
   demostró con números que mover el pivote ~1 px puede cambiar el kickback ~1,6°/px, y que
   las mitigaciones eficaces son **todas de frontend**: marcar con **zoom** (precisión
   sub-píxel), **promediar varios clics** en los puntos críticos (pivote + pedalier) y **avisar
   si la resolución de la foto es baja**. Esto convierte el "lienzo de marcado" (frente 3) en
   el frente que más hay que cuidar, y es un requisito directo del issue **#6**.

**De dónde partimos (no es de cero).** El esqueleto en `frontend/` ya trae **React 19.2 +
Vite 8**, **react-i18next** (i18n con inglés base), **oxlint** como linter y **CSS plano**
(`App.css` / `index.css`). No hay todavía ninguna librería de gráficas, de lienzo ni de
estilado: se elegirán aquí.

**Estrella polar de todo el informe** (restricciones del proyecto): es un **MVP de curso** que
debe quedar **EXPLICABLE** —cada parte debe poder defenderse en una entrevista—, con
**dependencias mínimas** y añadidas **solo cuando el sprint las necesita**. React + Vite ya
está fijado en `CLAUDE.md`. Todas las librerías candidatas son de **licencia permisiva**
(MIT en su mayoría, ISC/BSD alguna), así que **la licencia no distingue** entre opciones y no
se repite en cada tabla.

---

## 1. Framework / build

**Qué se decide:** con qué construir una SPA (aplicación de una sola página) que tiene un
lienzo interactivo (marcado sobre foto) y tres gráficas. La pregunta del encargo es si se
**confirma React + Vite** o si alguna alternativa encaja mejor, dejando claro el **coste de
cambiar** respecto a lo ya fijado.

| Opción | Curva de aprendizaje | Encaje en MVP de curso | Doc / comunidad | Explicable | Coste de cambiar desde lo ya hecho |
|---|---|---|---|---|---|
| **React + Vite** (lo fijado) | Media, pero **ya iniciada** | Alto: estándar de industria, encaja con el enunciado | Enorme | Alto (JSX se lee casi como HTML) | **Cero**: ya está montado y es lo que pide `CLAUDE.md` |
| Vue + Vite | Algo más suave que React | Alto | Grande | Alto | **Alto**: rehacer esqueleto + aprender otro ecosistema |
| SvelteKit | Suave en lo básico | Medio: framework *full-stack* con más "magia" (rutas, SSR) que aquí sobra | Media-grande | Medio: menos código, pero más conceptos implícitos que defender | **Alto**: cambia build, mental model y contradice el enunciado |

**Lecturas.** React+Vite no es solo "lo que había": es la opción correcta para este caso. Vite
da un servidor de desarrollo instantáneo y un build simple; React aporta el modelo de
componentes más extendido (útil para el "sé explicarlo" y para el CV). Las alternativas son
buenas en abstracto, pero aquí **cambiar cuesta y no aporta**: obliga a rehacer el esqueleto,
a aprender un segundo ecosistema desde cero (se parte de **cero** en frontend) y a
contradecir el enunciado del curso. SvelteKit además arrastra funcionalidad *full-stack* (SSR,
enrutado de servidor) que este front —una SPA que llama a una API— no necesita.

> **Recomendación (frente 1): confirmar React + Vite.** Es lo ya montado, lo que pide el
> enunciado y lo más explicable; ninguna alternativa compensa el coste de cambiar. Decisión
> honesta, no inercia: aunque partiéramos de cero, para una SPA con lienzo + gráficas seguiría
> siendo una elección de primera.

---

## 2. Librería de gráficas

**Qué se decide:** con qué pintar las tres curvas. Recuerda del §0 que el rendimiento **no**
es criterio (100 puntos es nada) y que la **trayectoria del eje** es el gráfico especial (2D).
El criterio dominante es **cuán explicable** queda el código y lo fácil que sea darle una
estética moderna.

| Opción | Modelo mental | Explicable | Estética moderna | Trayectoria 2D del eje | Nota React 19.2 |
|---|---|---|---|---|---|
| **Recharts** | Componentes React declarativos (`<LineChart>`, `<Line>`) | **Muy alto**: el gráfico se lee como HTML | Alta, con estilo propio fácil | Correcta vía `ScatterChart` con dominios fijos (algo de cuidado manual) | Requiere *override* de `react-is` (ver abajo) |
| Chart.js (+ `react-chartjs-2`) | Objeto de configuración grande | Medio: es un `options` extenso, menos "JSX" | Alta | **La más natural de fábrica**: tipo `scatter` con ejes lineales | Soportado |
| visx (D3 en componentes React) | Primitivas de bajo nivel | Bajo-medio: das más control pero escribes más | Muy alta (control total) | Muy buena (control total) | **v4/React 19 aún en alpha**; ritmo de releases lento |
| uPlot | Imperativo, ultraligero | Bajo: API tersa orientada a rendimiento | Media (hay que currársela) | Posible, pero manual | Sin problema (agnóstico) |
| D3 directo | Todo a mano (escalas, ejes, SVG) | Bajo para principiante: máxima potencia, máxima carga | Máxima | Total, pero todo manual | Sin problema (agnóstico) |

**El detalle de React 19.2 (honesto).** El proyecto va en React **19.2.7**. Recharts soporta
React 19, pero con esa serie hace falta fijar una *override* de la dependencia `react-is` en
`package.json` (se ha reportado que sin ella algún render falla en 19.2.x). Es una línea de
configuración conocida y documentada, no un bloqueo —pero hay que saberlo y dejarlo anotado
para no perder una tarde. Chart.js y uPlot, al ser agnósticos de React, no tienen este roce.

**Por qué Recharts pese a ese roce.** El criterio número uno es *explicable*, y ahí Recharts
gana con claridad. Una curva se escribe así:

```jsx
<LineChart data={leverageCurve}>
  <XAxis dataKey="wheelTravelMm" />
  <YAxis />
  <Tooltip />
  <Line dataKey="ratio" dot={false} />
</LineChart>
```

Se **lee como HTML** y cada línea mapea 1:1 con lo que se ve (un eje X, un eje Y, una línea).
Para alguien que parte de cero y tiene que defender su código, eso vale más que cualquier
micro-optimización. El equivalente en Chart.js es un objeto `options`/`data` más grande y menos
declarativo (se configura, no se "compone"), y visx/D3 te hacen dibujar ejes y escalas a mano.

**El gráfico especial (trayectoria del eje).** Como es una curva 2D (§0), en Recharts se hace
con un `ScatterChart` de ejes numéricos y una `Scatter` con `line` para unir los puntos,
**fijando los dominios** para mantener la misma escala en mm en ambos ejes (si no, la forma
sale distorsionada). Alternativa muy limpia: **dibujarla como un pequeño SVG a mano** —y aquí
aparece una sinergia útil con el frente 3, porque el marcado ya se hará en SVG, así que ese
"saber dibujar en SVG" se reutiliza y el control de la escala 1:1 es total. Chart.js, por su
parte, trae el tipo `scatter` que maneja la curva 2D de forma algo más natural de fábrica:
por eso es la **alternativa** si esa curva o el estilado se hicieran incómodos.

> **Recomendación (frente 2): Recharts** para las dos curvas de línea (leverage y kickback),
> por ser lo más explicable y declarativo, con estética moderna sencilla de lograr. La
> **trayectoria del eje** se resuelve con `ScatterChart` de dominios fijos **o** con un SVG a
> mano (reutilizando el frente 3). Dejar anotada la *override* de `react-is` para React 19.2.
> **Alternativa: Chart.js (`react-chartjs-2`)** si se prefiere una sola herramienta que trate
> la curva 2D de forma nativa. **Descartar para este MVP:** visx (su soporte de React 19 está
> en alpha), uPlot y D3 directo (potencia que aquí no hace falta a cambio de menos
> explicabilidad).

---

## 3. Lienzo de marcado de puntos

**Qué se decide:** cómo marcar los puntos sobre la foto con **zoom** y **precisión**. Este es
el frente crítico (§0): la [investigación de sensibilidad](sensibilidad-marcado-pivote-INFORME.md)
probó que la calidad del marcado es lo que fija la precisión del kickback, y que las palancas
que funcionan son de UI: **zoom sub-píxel**, **promediar clics** en pivote + pedalier y
**avisar si la foto es de baja resolución**. La opción elegida debe hacer que esas tres cosas
sean **baratas de implementar**.

| Opción | Modelo mental | Explicable | Zoom / precisión sub-píxel | Arrastrar/editar puntos | Peso (dependencia) |
|---|---|---|---|---|---|
| **SVG sobre la foto** (`<image>` + `<circle>`) | Cada punto es un elemento del DOM | **Muy alto**: inspeccionas cada marcador en el navegador | Zoom por `viewBox`; coords en float → sub-píxel natural | Trivial (los nodos SVG reciben eventos y foco) | **Cero** (nativo) |
| Canvas 2D nativo | Dibujas imagen + marcadores tú mismo | Medio: "sin magia", pero tú programas zoom, *hit-testing* y lupa a mano → mucho código fino | Total, pero **todo manual** | Manual (detectar clic sobre punto a mano) | Cero (nativo) |
| **react-konva** (Konva) | Escena de canvas como componentes React (`<Stage>`, `<Layer>`, `<Circle draggable>`) | Alto: declarativo, pero añade el modelo Stage/Layer/Node | Zoom/escala y arrastre **de fábrica** | **De fábrica** (`draggable`, detección de clic incluida) | Media (dep nueva; **react-konva 19.x soporta React 19.2** limpiamente) |
| Fabric.js | Editor de objetos (selección, transformación) | Medio-bajo: potente pero más "editor gráfico" que lo que pedimos | Total | De fábrica | Alta (más de lo necesario, menos idiomático en React) |

**Cómo se traducen las mitigaciones de sensibilidad a cada enfoque (lo importante):**

- **Marcar con zoom (sub-píxel).** En **SVG** es casi gratis: reducir el `viewBox` a una
  sub-región amplía la zona bajo el cursor, y como las coordenadas son números en coma
  flotante (no píxeles enteros), la precisión es continua. Además, permitir mover el punto con
  las **flechas del teclado** (un pequeño delta) da un ajuste fino literalmente sub-píxel —y de
  paso es accesible.
- **Promediar N clics** en pivote + pedalier: en cualquier enfoque es "recoger N clics y
  promediar sus coordenadas normalizadas", pero con marcadores que son elementos (SVG) o nodos
  (Konva) es más fácil enseñar/rehacer cada clic.
- **Aviso de resolución baja:** es lógica pura a partir del calibrado (mm/px), independiente de
  la librería; encaja en cualquiera.

Un esqueleto en SVG de lo explicable que queda:

```jsx
<svg viewBox={`${vx} ${vy} ${vw} ${vh}`} onClick={mark}>   {/* zoom = encoger el viewBox */}
  <image href={photoUrl} width={imgW} height={imgH} />
  {points.map(p => (
    <circle key={p.type} cx={p.x * imgW} cy={p.y * imgH} r={4}
            tabIndex={0} onKeyDown={nudgeWithArrows} />   {/* ajuste fino sub-píxel */}
  ))}
</svg>
```

Los puntos se guardan **normalizados (0–1)** respecto a las dimensiones de la imagen (lo exige
el issue #6 y ya lo asume el motor), y de ahí se convierten a la escala que espera el backend.

**El equilibrio.** Para ~6 puntos sobre una foto, el **SVG** es lo más explicable y con **cero
dependencias** (encaja con "añadir deps solo cuando el sprint las necesita"): cada marcador es
un elemento que se ve, se enfoca y se arrastra sin trucos, y el zoom por `viewBox` es una línea.
El **canvas nativo** da el mismo resultado pero obliga a programar a mano el zoom, la detección
de clic sobre un punto y la lupa: más código fino y más frágil para un principiante.
**react-konva** es la alternativa pragmática si las interacciones se complican (zoom/paneo
suave, lupa, muchos manejadores): regala arrastre y detección de clics, y su versión 19.x
soporta React 19.2 sin líos —a cambio de una dependencia y de aprender el modelo Stage/Layer.

> **Recomendación (frente 3): SVG sobre la foto** como enfoque principal —máxima
> explicabilidad, cero dependencias y las mitigaciones de sensibilidad (zoom por `viewBox`,
> ajuste con teclado, promediado de clics) salen baratas y directas. **Alternativa: react-konva**
> si el zoom/paneo/lupa hacen que el SVG a mano se quede corto en ergonomía. **Descartar:**
> canvas nativo (mismo fin, más código frágil) y Fabric.js (un editor gráfico completo, más de
> lo que este marcado necesita).

---

## 4. Estilo / design system

**Qué se decide:** cómo lograr la estética **moderna** (la referencia negativa: un aspecto
anticuado) de forma **mantenible y explicable**, y si conviene una base de componentes o algo
mínimo a mano. El alcance visual de la v1 es pequeño: un asistente por pasos (metadatos → foto
→ marcado → resultados), unos formularios y las tres gráficas.

| Opción | Modelo mental | Explicable | Estética moderna | Peso / build | Riesgo para un MVP de curso |
|---|---|---|---|---|---|
| **CSS Modules + design tokens** (variables CSS) | CSS de siempre, con clases con ámbito por archivo | **Muy alto**: es CSS, sin vocabulario nuevo | Alta (tokens de color/tipografía/espaciado + flex/grid) | Cero deps (Vite lo trae de fábrica) | Muy bajo |
| Tailwind v4 | Clases utilitarias en el JSX | Medio-alto: es CSS abreviado, pero hay que defender "por qué estas 12 clases" | Muy alta y **rápida** | Dep + plugin de Vite (oficial, setup mínimo) | Bajo, pero es un vocabulario nuevo que aprender |
| CSS-in-JS (styled-components / emotion) | Estilos como componentes JS | Medio | Alta | Coste en runtime; churn con React 19 | Medio (concepto extra + mantenimiento dudoso) |
| Kit de componentes (MUI / Chakra / Mantine) | Componentes ya hechos y estilados | **Bajo para el "es mío"**: media UI sería de otro | Alta pero "de catálogo" | Dep pesada | **Alto para el objetivo del curso** (debilita el "lo construí yo") |
| Headless (Headless UI / Radix / Base UI) | Comportamiento accesible sin estilo | Alto en su nicho | N/A (no estiliza; lo estilas tú) | Dep, pero acotada | Bajo si se usa **solo** donde hace falta |

**Lecturas.**
- **CSS Modules + tokens** es lo más explicable y con cero dependencias nuevas, y es capaz de
  una estética plenamente moderna: un sistema de **variables CSS** (paleta contenida, escala de
  tipografías, escala de espaciado), *flexbox*/*grid* y estados de foco cuidados bastan para
  que no se vea anticuado. Es además la dirección que ya trae el esqueleto (CSS plano).
- **Tailwind v4** (estable desde inicio de 2025, con **plugin oficial de Vite** y build muy
  rápido) es una alternativa legítima: acelera mucho el diseño. El *pero* es de explicabilidad:
  el JSX se llena de clases utilitarias y hay que poder defender ese enfoque; si se
  siente cómodo con él, es una opción sólida.
- **CSS-in-JS** añade coste en runtime y concepto, y algunas de sus librerías están en modo
  mantenimiento con la llegada de React 19 → poco recomendable para este MVP.
- **Kits de componentes** (catálogos ya estilados) chocan de frente con el objetivo del curso:
  gran parte de la interfaz sería de otro y debilita el "esto lo hice y lo sé explicar".
- **Headless** no es una forma de estilar, sino de obtener el **comportamiento accesible** de un
  *widget* difícil (un diálogo modal, un desplegable/*combobox* con teclado y foco correctos)
  **sin** estilo, para que lo pintes tú. Panorama a septiembre de 2026: **Radix** sigue siendo
  el más extendido pero su mantenimiento se ha ralentizado (pasó a WorkOS); **Base UI** (del
  equipo de MUI) alcanzó su v1.0 estable en diciembre de 2025 y es hoy la capa de primitivas
  más activa; **Headless UI** (del equipo de Tailwind) encaja de forma natural si se elige
  Tailwind. Para este alcance, casi todo se resuelve con **HTML nativo** (`<input>`, `<select>`,
  `<button>`, `<dialog>`), así que un headless solo se justifica cuando aparezca un widget de
  verdad complejo.

**Nota de accesibilidad** (aplica a todo el front, no solo al estilo):
- **HTML semántico** y cada `<input>` con su `<label>`; estados de **foco visibles**
  (`:focus-visible`) —clave si se permite ajustar el marcado con teclado (frente 3).
- **Contraste** de color mínimo AA; no comunicar información **solo** con color (p. ej. la banda
  de progresión o el aviso de TravelCheck deben llevar texto/icono, no solo un color).
- El **lienzo de marcado** necesita alternativa por **teclado** (mover el punto seleccionado con
  flechas) y etiquetas ARIA que digan qué punto se está marcando.
- Las **gráficas** deberían tener un equivalente textual o una tabla de datos accesible por si
  el SVG/canvas no lo es para un lector de pantalla.

> **Recomendación (frente 4): CSS Modules + design tokens** como base —lo más explicable, cero
> dependencias nuevas, ya es la dirección del esqueleto y da de sobra para una estética moderna.
> **Alternativa: Tailwind v4** si se prioriza velocidad de diseño y se está cómodo
> defendiendo las utilidades. Un **headless** (Headless UI si se va con Tailwind; Radix o Base
> UI si no) **solo** cuando aparezca un *widget* accesible complejo; hasta entonces, HTML nativo.
> **Descartar para este MVP:** CSS-in-JS y los kits de componentes completos.

---

## 5. Stack recomendado de conjunto

Presentado como conjunto coherente (framework + gráficas + marcado + estilo), con su
**alternativa** por si se prefiere otra vía. **No es una decisión tomada**: es la
recomendación razonada para la decisión final.

| Capa | Recomendación principal | Alternativa | ¿Dependencia nueva? |
|---|---|---|---|
| Framework / build | **React + Vite** (confirmar; ya montado) | — | No |
| i18n | **react-i18next** (ya instalado, inglés base) | — | No |
| Gráficas | **Recharts** (+ trayectoria del eje en `ScatterChart` o SVG a mano) | Chart.js (`react-chartjs-2`) | **Sí** (una) |
| Marcado sobre foto | **SVG sobre la foto** (zoom por `viewBox`, ajuste con teclado) | react-konva | **No** (SVG) / Sí (si Konva) |
| Estilo | **CSS Modules + design tokens** | Tailwind v4 | **No** (CSS Modules) / Sí (si Tailwind) |
| Componentes accesibles complejos | **HTML nativo**; headless solo si aparece un widget difícil | Headless UI / Radix / Base UI | Solo si se necesita |

**Por qué este conjunto es coherente:**
- **Una sola dependencia nueva imprescindible** (la de gráficas). El marcado y el estilo
  recomendados son nativos (SVG y CSS Modules) → respeta "dependencias mínimas y solo cuando el
  sprint las necesita".
- **Todo optimizado para *explicable*.** Recharts se lee como HTML, el marcado en SVG es
  inspeccionable elemento a elemento, y CSS Modules es CSS de siempre. Encaja con la exigencia
  de poder defender cada parte.
- **Sinergia entre frentes:** el "saber dibujar en SVG" del marcado (frente 3) se reutiliza para
  la trayectoria del eje (frente 2).
- **Sube de nivel sin rehacer:** si algo se queda corto, hay una alternativa clara por capa
  (Chart.js, react-konva, Tailwind) sin tirar el resto.

**Antes de fijarlo, un *spike* corto (recomendado, no obligatorio):** al arrancar el sprint,
montar un prototipo desechable con Recharts (incluida la *override* de `react-is`) y un marcado
en SVG con zoom, para confirmar de primera mano las dos únicas fricciones reales detectadas
(React 19.2 + Recharts, y la ergonomía del zoom en SVG). Es media hora que ahorra sorpresas.

---

## 6. Nota para una futura *skill* de diseño

Qué convendría que recogiera una futura *skill* (no se crea aquí; solo se apunta el contenido):

**Buenas prácticas de frontend (estructura y código):**
- **Estructura de componentes:** carpetas por funcionalidad; componentes pequeños; separar
  presentación de lógica; extraer las llamadas a la API a *hooks* propios (p. ej. un
  `usePreview`) para que la pantalla no mezcle "pintar" con "pedir datos".
- **Flujo de datos claro y defendible:** interfaz → estado local → llamada a la API → respuesta
  → render. Estado local + una capa fina de API; **sin** gestores de estado pesados (Redux) para
  este alcance.
- **i18n:** todo texto visible sale de las traducciones (inglés base), nunca cadenas fijas en el
  JSX; convención de claves por pantalla/sección.
- **Estados de carga y error** siempre contemplados (la llamada al motor puede tardar o fallar),
  y el **aviso de TravelCheck** integrado en la vista de resultados.
- **Accesibilidad como checklist** (ver §4): HTML semántico, `label` en inputs, foco visible,
  contraste AA, alternativa por teclado en el lienzo, equivalente textual de las gráficas.

**Diseño gráfico (lenguaje visual):**
- **Color y tipografía por tokens:** paleta contenida y moderna definida como variables CSS;
  escala tipográfica y escala de espaciado; pensar el modo oscuro desde el principio.
- **Convenciones de las gráficas** (específicas de este proyecto):
  - un color por curva, ejes y rejilla discretos, *tooltip* sobrio;
  - la **trayectoria del eje** siempre con **la misma escala mm en ambos ejes** (regla de oro
    para no falsear la forma);
  - el **kickback** presentado como **banda** ("medio/alto"), coherente con que el motor no da
    una cifra exacta;
  - la **banda de progresión** y la **forma en 3 fases** como indicadores visuales claros
    (etiquetas/chips), nunca solo por color;
  - el **aviso de recorrido** (TravelCheck) con un estilo de alerta reconocible.
- **Referencia de estilo:** limpio y moderno; la referencia negativa es el aspecto anticuado de
  la herramienta comparativa. **Fuera de la v1** (no dibujar): los polígonos del cuadro y la
  animación de la bici comprimiéndose.

---

## En una frase

**Confirmar React + Vite; añadir una sola dependencia (Recharts) para las gráficas; marcar los
puntos en SVG sobre la foto (zoom por `viewBox` + ajuste con teclado, que hace baratas las
mitigaciones de sensibilidad del pivote); y estilar con CSS Modules + tokens para una estética
moderna y explicable —con Chart.js, react-konva y Tailwind como alternativas claras por capa si
algo se queda corto. Todo pensado para que quede EXPLICABLE y con deps mínimas; la decisión es
del responsable del proyecto.**
