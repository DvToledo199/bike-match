# Encargo de investigación — análisis de la curva desde el sag

> **Cómo usar:** abre un chat NUEVO de Claude Code en este repo y dile
> *"Lee y ejecuta docs/investigaciones/analisis-desde-sag.md"*. Todo el encargo está aquí;
> ese chat investiga y deja su informe en el archivo indicado abajo, **sin tocar código**.
> Después, el chat principal lee el informe y se decide.

---

## Contexto

BikeMatch calcula la curva de leverage (relación de palanca) de una suspensión trasera a
lo largo de TODO el recorrido de la rueda (0→100%). Para interpretarla, la base de
conocimiento (`docs/base-conocimiento-cinematica.md`, §2–3) propone **leer la curva
partida en el punto de sag (30% por defecto)**: el tramo 0→sag gobierna sensibilidad y
tracción, y el tramo sag→100% ("progresión útil") gobierna el apoyo y la resistencia a
hacer tope. La "cifra estrella" que propone el documento es la progresión útil (sag→100%),
no la total (0→100%).

## La duda a resolver

No está claro que analizar/priorizar desde el sag sea siempre lo correcto. La suspensión
trabaja también desde el recorrido 0 en situaciones reales: por ejemplo, aterrizar un
salto con la suspensión extendida, o descargar peso y volver a comprimir. Queremos
entender:

1. **¿Por qué el sector analiza la curva partida en el sag, y por qué 30%?** ¿Cuál es el
   fundamento físico (el sag como punto de trabajo estático del que "cuelga" el recorrido
   disponible por arriba y por abajo)?
2. **¿La progresión debe medirse desde 0 o desde el sag?** ¿Cuándo tiene sentido cada una?
   ¿Es correcto que la progresión útil (sag→100%) sea la cifra principal, o debería serlo
   la total (0→100%), o ambas con distinto peso?
3. **¿En qué situaciones trabaja de verdad cada tramo** (0→sag vs sag→100%), incluyendo
   recepciones de salto (suspensión extendida) o baches con la bici descargada?
4. **¿El 30% debería variar por disciplina** (XC/Trail/Enduro/DH) o dejarse fijo?

## Tu tarea (solo investigar; NO modificar código)

- Investiga la práctica del sector (análisis de suspensiones MTB) y el fundamento físico,
  con búsqueda web si hace falta y leyendo `docs/base-conocimiento-cinematica.md`.
- Da una recomendación clara para BikeMatch v1: qué métricas de progresión calcular y
  **desde dónde** medirlas, y cómo presentarlas sin sobre-simplificar.
- Señala si algo de la base de conocimiento (§2–3) habría que matizar o corregir.

## Entregable (lo ÚNICO que creas)

Un informe en **`docs/investigaciones/analisis-desde-sag-INFORME.md`** (español) con:
fundamento físico, práctica del sector (con fuentes), respuesta razonada a "¿desde dónde
medir la progresión?", recomendación para v1, y ajustes propuestos a la base de
conocimiento si procede. No modifiques ningún otro archivo ni el código.

## Punteros

- Interpretación (fuente de verdad): `docs/base-conocimiento-cinematica.md` §2–3.
- Cómo se calcula la curva: `docs/fundamentos-motor-cinematica.md`.
- Modelo de leverage en código: `backend/src/main/java/com/bikematch/kinematics/curve/LeverageCurve.java`.
