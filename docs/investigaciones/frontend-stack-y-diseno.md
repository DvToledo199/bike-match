# Encargo de investigación — stack y diseño del frontend

> **Cómo usar este archivo:** abre un chat NUEVO de Claude Code en este repo y dile
> *"Lee y ejecuta `docs/investigaciones/frontend-stack-y-diseno.md`"*. Todo el encargo está
> aquí dentro; ese chat hace el análisis y deja su informe en el archivo indicado abajo,
> **sin tocar el código de producción ni instalar nada permanente**. Después, el chat
> principal (o David) lee el informe y **decide**. Este encargo **informa y recomienda,
> pero NO decide** por su cuenta.

---

## Contexto

BikeMatch (repo `bike-match`) es el MVP final de un bootcamp de Java. El **backend ya está
hecho**: un motor de cinemática (dominio puro, `com.bikematch.kinematics`) y un endpoint REST
**`POST /api/kinematics/preview`** que recibe los puntos marcados sobre una foto + calibración
+ parámetros y devuelve, **sin base de datos**, las curvas y los descriptores. Falta construir
el **frontend**, que es el objeto de esta investigación (todavía no se ha empezado).

**Qué tiene que hacer el frontend (alcance v1):**
- Subir una foto lateral de la bici y **marcar puntos** encima (pivote, ejes, anclajes del
  amortiguador, pedalier) sobre un lienzo con zoom.
- Pedir al usuario la **calibración** (eye-to-eye del amortiguador, v1) y los **parámetros**
  (carrera, dientes plato/piñón, recorrido declarado, sag).
- Llamar al endpoint y **dibujar tres gráficas** (leverage ratio, pedal kickback, trayectoria
  del eje) + mostrar los **descriptores numéricos**, la **banda de progresión**, los tres
  tramos de forma, el **retroceso** y el **aviso de recorrido** (TravelCheck).
- Estética **moderna** (referencia negativa: BikeChecker se ve anticuado). Textos vía
  **react-i18next** (inglés como idioma base). **Sin** dibujar los polígonos del cuadro y
  **sin** animación de la bici comprimiéndose (fuera de la v1).

**Restricciones de proyecto que condicionan las decisiones:**
- Es un **MVP de curso**: el frontend puede generarse con ayuda de IA, **pero debe quedar
  EXPLICABLE** (David tiene que poder defender cada parte).
- En `CLAUDE.md` ya está fijado **React + Vite (JavaScript)**. No se parte de cero: cambiarlo
  implicaría rehacer y contradecir el enunciado — pero se analiza **con honestidad**.

## Tu tarea (analizar y RECOMENDAR; NO decidir; NO modificar el código de producción)

Investiga estos **cuatro frentes** y, para cada uno, compara opciones con criterios explícitos
(curva de aprendizaje, encaje en un MVP de curso, calidad de documentación/comunidad, tamaño,
licencia, y sobre todo **cuán explicable queda**), y **termina con una recomendación clara**
—pero deja la decisión final a David:

1. **Framework / build.** ¿Se confirma **React + Vite** o hay una alternativa mejor para este
   caso (SPA con lienzo interactivo + gráficas)? Valora al menos el confirmar React+Vite frente
   a 1–2 alternativas realistas, dejando claro el coste de cambiar respecto a lo ya fijado.
2. **Librería de gráficas.** Para pintar las tres curvas (series de puntos `x,y`: recorrido de
   rueda vs. ratio / grados / posición del eje). Compara opciones (p. ej. Recharts, Chart.js,
   visx, uPlot, D3 directo…): facilidad, personalización para una estética moderna, rendimiento
   con ~100 puntos por curva, y lo explicable que resulta el código.
3. **Lienzo de marcado de puntos.** Cómo marcar puntos sobre la foto con **zoom** y precisión
   (clave: el marcado del pivote es muy sensible — ver `docs/limitaciones-y-mejoras.md` y
   `docs/investigaciones/sensibilidad-marcado-pivote-INFORME.md`). Valora enfoques (canvas
   nativo, SVG, o librería tipo Konva/Fabric) y cuál ayuda a marcar con más precisión.
4. **Estilo / design system.** Cómo lograr la estética moderna de forma mantenible y explicable:
   opciones de estilado (CSS Modules, Tailwind, CSS-in-JS) y si conviene una base de componentes
   (headless tipo Radix/Headless UI, o una librería completa) o algo mínimo a mano. Incluye una
   nota de **accesibilidad**.

## Entregable (lo ÚNICO que debes crear en el repo)

Un informe en **`docs/investigaciones/frontend-stack-y-diseno-INFORME.md`** (en español) con,
por cada uno de los cuatro frentes: una **tabla comparativa** con los criterios de arriba, los
pros/contras, y una **recomendación razonada**. Cierra con:
- Un **stack recomendado de conjunto** (framework + gráficas + marcado + estilo) coherente, con
  el porqué — presentado para que **David elija**, no como decisión tomada.
- Una **nota para una futura *skill* de diseño**: qué convenciones debería recoger (buenas
  prácticas de frontend: estructura de componentes, i18n, accesibilidad; y diseño gráfico:
  lenguaje visual, estilo de gráficas, color/tipografía). **No** crees la skill aquí; solo deja
  apuntado qué debería contener.

**Restricciones:** no modifiques ningún archivo salvo ese informe; no toques el backend; no
abras PRs; no añadas dependencias al repo. Puedes crear experimentos/prototipos desechables
**fuera** de `frontend/` y `backend/` (p. ej. en un directorio temporal) y borrarlos al acabar.

## Punteros técnicos

- Guía del proyecto y stack ya fijado: `CLAUDE.md` (sección *Structure* → `frontend/`).
- **Contrato del endpoint** que el front consumirá (leer para conocer la forma exacta de la
  petición y la respuesta):
  `backend/src/main/java/com/bikematch/kinematics/api/PreviewRequest.java`,
  `PointDto.java`, `KinematicsParametersDto.java`, `PreviewResponse.java`,
  `MeasurementConditions.java`, `KinematicsController.java`.
- Interpretación de las cifras (qué significan leverage, progresión, tramos, kickback,
  retroceso): `docs/base-conocimiento-cinematica.md` y `docs/fundamentos-motor-cinematica.md`.
- Cómo lo hace la referencia y qué NO copiamos (polígonos/animación): `docs/limitaciones-y-mejoras.md`.
- Comprueba si ya existe algún esqueleto en `frontend/` y descríbelo si lo hay.
- Issues relacionados: **#6** (crear bici / marcado en el front). Idioma de la UI: inglés base
  vía react-i18next (la traducción al español es tarea de un sprint posterior).
