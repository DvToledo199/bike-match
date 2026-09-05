# Arranque del frontend — paquete de traspaso

> **Cómo usar este archivo:** abre un chat NUEVO de Claude Code en este repo para **construir
> el frontend** y dile *"Lee `docs/frontend-arranque.md`"*. Reúne TODO lo ya decidido para
> arrancar sin repetir el análisis. El estudio completo que respalda las decisiones está en
> [`investigaciones/frontend-stack-y-diseno-INFORME.md`](investigaciones/frontend-stack-y-diseno-INFORME.md).

---

## 0. Contexto en una frase
El **backend ya está hecho**: motor de cinemática (dominio puro) + endpoint REST
**`POST /api/kinematics/preview`** (público, **sin base de datos**). Falta el **frontend**:
subir una foto lateral → **marcar puntos** → meter parámetros → llamar al endpoint → **dibujar
las curvas y los descriptores**. Es el **MVP final de un bootcamp**: debe quedar **EXPLICABLE**.

## 1. Perfil de David y forma de trabajar (IMPORTANTE)
- **David** es estudiante de bootcamp. El **backend lo ha hecho él a mano**; en **frontend parte
  de CERO** (no ha tocado HTML/CSS/JS/React). **Explícale los conceptos en español, sencillo,
  con analogías**, y traduce identificadores/comentarios en inglés cuando ayude.
- El frontend **puede generarse con ayuda de IA, pero debe quedar EXPLICABLE** (defendible en una
  entrevista). Prioriza **claridad** sobre "rápido". Explica el porqué de cada pieza.
- **Idioma:** código, identificadores y commits en **inglés**; el texto de la UI vive en
  **react-i18next** (inglés como idioma base) — nunca cadenas fijas en el JSX.
- **Git:** una tarea = una rama = un PR pequeño. David revisa y hace merge; incluye `Closes #N`.
- **Diseño:** David dirige **reaccionando a lo que ve** (no sabe teoría de diseño). Ver §6.

## 2. Stack decidido (CERRADO — no re-decidir)
| Capa | Elegido | Nota |
|---|---|---|
| Framework / build | **React + Vite** (JavaScript) | Ya fijado en `CLAUDE.md`; esqueleto ya montado |
| Idiomas (i18n) | **react-i18next** | Ya instalado, inglés base |
| Gráficas | **Recharts** | **Única dependencia nueva imprescindible** |
| Marcado sobre foto | **SVG** (zoom por `viewBox` + ajuste con teclado) | Nativo, cero deps |
| Estilo | **CSS Modules + design tokens** | Nativo, cero deps |
| Componentes accesibles | **HTML nativo** | *Headless* (Base UI/Radix/Headless UI) **solo** si aparece un widget complejo |

**Dos fricciones conocidas a confirmar con un *spike* de ~30 min al arrancar** (ver informe §2 y §5):
1. **React 19.2 + Recharts** puede necesitar una *override* de `react-is` en `package.json`.
2. Ergonomía del **zoom en SVG** para el marcado.

**Vías de "subir de nivel" sin rehacer** (por si algo se queda corto): gráficas → Chart.js;
marcado → react-konva; estilo → Tailwind v4. No hacen falta ahora.

## 3. Qué construir (alcance v1)
Un **asistente por pasos**: parámetros → subir foto → marcar puntos → resultados.
- **Foto del usuario:** aceptar **JPG / PNG / WebP** (formatos de foto normales). **Nunca SVG**
  (el SVG es solo la capa de dibujo de encima, no el formato de la foto).
- **Marcado:** 6 puntos (`MAIN_PIVOT`, `SHOCK_FRAME`, `SHOCK_SWINGARM`, `BOTTOM_BRACKET`,
  `REAR_AXLE`, `FRONT_AXLE`) sobre una capa **SVG** encima de la foto. Requisitos de precisión
  (críticos — ver [`sensibilidad-marcado-pivote-INFORME.md`](investigaciones/sensibilidad-marcado-pivote-INFORME.md)):
  **zoom**, **ajuste fino con flechas del teclado**, **promediar varios clics** en pivote +
  pedalier, y **avisar si la foto es de baja resolución** (mm/px alto).
- **Calibración v1:** eye-to-eye del amortiguador (distancia entre sus dos anclajes, en mm).
- **Resultados:** 3 gráficas (leverage, kickback, trayectoria del eje) + tarjetas de
  descriptores + banda de progresión + las 3 fases de forma + retroceso + aviso de recorrido.
- **Fuera de la v1:** NO dibujar los polígonos del cuadro; NO animación de la bici.

## 4. Contrato del endpoint (lo que el front llama y recibe)
`POST /api/kinematics/preview` — **público** (sin auth por ahora), **sin base de datos**.
CORS permite `http://localhost:5173`. Fuente de verdad: los DTOs en
`backend/src/main/java/com/bikematch/kinematics/api/`.

**Envía** (los puntos van en un espacio de coordenadas 2D consistente; **píxeles de la imagen**
es lo natural — el backend calibra px→mm internamente con el eye-to-eye):
```json
{
  "points": [
    { "type": "MAIN_PIVOT",     "x": 208.3, "y": 167.8 },
    { "type": "SHOCK_FRAME",    "x": 250.9, "y": 114.4 },
    { "type": "SHOCK_SWINGARM", "x": 192.8, "y": 121.3 },
    { "type": "BOTTOM_BRACKET", "x": 198.7, "y": 190.3 },
    { "type": "REAR_AXLE",      "x": 86.0,  "y": 187.3 },
    { "type": "FRONT_AXLE",     "x": 408.5, "y": 190.0 }
  ],
  "eyeToEyeMm": 230.0,
  "parameters": {
    "shockStrokeMm": 65, "chainringTeeth": 34, "sprocketTeeth": 50,
    "declaredTravelMm": 164, "sagPercent": 30
  }
}
```

**Recibe** (tres series de ~100 puntos + descriptores):
```json
{
  "leverageCurve":  [ { "wheelTravelMm": 1.6, "ratio": 2.55 }, "… ~100" ],
  "kickbackCurve":  [ { "wheelTravelMm": 1.6, "kickbackDegrees": 0.4 }, "… ~100" ],
  "axlePath":       [ { "x": 331.9, "y": 726.8 }, "… ~100 (en mm)" ],
  "leverageDescriptors": {
    "lrInitial": 2.55, "lrAtSag": 2.53, "lrFinal": 2.50, "lrMean": 2.52,
    "totalProgressionPercent": 2.0, "usefulProgressionPercent": 1.2,
    "slopeInitialToSag": -0.01, "slopeSagToEnd": -0.01,
    "initialTrend": "PROGRESSIVE", "middleTrend": "LINEAR", "finalTrend": "PROGRESSIVE",
    "progressionBand": "LINEAR"
  },
  "axlePathDescriptors": { "maxRearwardMm": 12.3, "atTravelPercent": 55.0 },
  "travelCheck": { "calculatedTravelMm": 162.0, "declaredTravelMm": 164.0,
                   "deviationPercent": 1.2, "withinTolerance": true },
  "conditions": { "sagPercent": 30.0, "chainringTeeth": 34, "sprocketTeeth": 50 }
}
```
- **`SegmentTrend`**: `PROGRESSIVE` / `LINEAR` / `REGRESSIVE`.
- **`ProgressionBand`**: `LINEAR` / `SLIGHTLY_PROGRESSIVE` / `MEDIUM` / `HIGH` / `VERY_HIGH`.
- **La trayectoria del eje** (`axlePath`) es una curva **2D** (x e y en mm): dibujarla con la
  **misma escala mm en ambos ejes**, o sale distorsionada (informe §0 y §2).
- **Errores → 400** con cuerpo `ProblemDetail` (`{ type, title, status, detail }`): validación
  de entrada (`@Valid`) y punto obligatorio ausente / geometría imposible.

## 5. Mirando al futuro (NO construir ahora, pero no cerrarnos puertas)
- **Login, cuentas de usuario y guardar bicis SÍ son parte del MVP**, pero van en un **sprint
  posterior** (autenticación / JWT en la hoja de ruta). **No se construyen ahora.**
- Aun así, **estructura el frontend para que encajen después sin rehacer**: rutas preparadas
  para una pantalla de login y zonas protegidas, y una **capa de llamadas a la API** aislada (un
  *hook* tipo `usePreview`) donde luego sea fácil añadir un token de autenticación.
- El endpoint de preview es **público a propósito** en esta fase (no toca base de datos).

## 6. Cómo se hará el diseño (con David)
David no sabe diseño; **dirige reaccionando**. Flujo recomendado:
1. Pedirle **referencias** (webs/apps cuyo aspecto le guste) o proponerle 1–2 direcciones.
2. Montar una **primera versión** y **enseñársela** (captura / vista previa del navegador).
3. Él **corrige** ("este color no, bordes más redondos, más espacio") y se **itera**.

Pautas visuales (informe §4 y §6): estética **moderna y limpia** (referencia negativa: el
aspecto anticuado de la herramienta comparativa); **modo oscuro** pensado desde el principio;
**accesibilidad** como checklist (HTML semántico, `label` en inputs, foco visible, contraste AA,
alternativa por teclado en el marcado, equivalente textual de las gráficas). Convenciones de
gráficas: un color por curva; kickback como **banda** ("medio/alto"), no cifra exacta; banda de
progresión y 3 fases como **chips** (nunca solo color); TravelCheck como **alerta** reconocible.

## 7. Punteros
- **Estudio del stack:** [`investigaciones/frontend-stack-y-diseno-INFORME.md`](investigaciones/frontend-stack-y-diseno-INFORME.md)
  (y su encargo [`frontend-stack-y-diseno.md`](investigaciones/frontend-stack-y-diseno.md)).
- **Contrato del endpoint:** `backend/src/main/java/com/bikematch/kinematics/api/`
  (`PreviewRequest`, `PointDto`, `KinematicsParametersDto`, `PreviewResponse`,
  `MeasurementConditions`, `KinematicsController`, `ApiExceptionHandler`).
- **Interpretación de las cifras:** `docs/base-conocimiento-cinematica.md`,
  `docs/fundamentos-motor-cinematica.md`.
- **Límites/atajos conscientes:** `docs/limitaciones-y-mejoras.md`.
- **Roadmap / enunciado / guía:** `docs/hoja-de-ruta-sprints.md`, `docs/enunciado.md`, `CLAUDE.md`.
- **Esqueleto actual en `frontend/`** (verificar al arrancar): React 19.2 + Vite 8 +
  react-i18next + oxlint + CSS plano; sin librería de gráficas/lienzo/estilado todavía.
- **Correr en local:** `docker compose -f docker/docker-compose.yml up -d` (BD, para el backend)
  → `cd backend && ./mvnw spring-boot:run` → `cd frontend && npm install && npm run dev`
  (`http://localhost:5173`).
