# Encargo de investigación — sensibilidad al marcado del pivote

> **Cómo usar este archivo:** abre un chat NUEVO de Claude Code en este repo y dile
> *"Lee y ejecuta `docs/investigaciones/sensibilidad-marcado-pivote.md`"*. Ese chat hará
> el análisis y dejará su informe en el archivo indicado abajo, sin tocar el código.
> Después, el chat principal lee ese informe y decide qué implementar.

---

## Contexto

BikeMatch (repo `bike-match`) calcula la cinemática de la suspensión trasera de bicis
de MTB a partir de puntos marcados **a mano** sobre una foto lateral. El motor (Java,
paquete `com.bikematch.kinematics`) es dominio puro y determinista.

**El problema:** el pedal kickback —y en general el crecimiento de cadena— es
**extremadamente sensible al marcado del pivote principal**. Medido sobre el fixture de
la Orange Surge: mover el pivote ~5 px cambia el kickback a tope de recorrido de **19° a
35°** (~1,6°/px). La foto de referencia de la Surge es pequeña (500×280 px, ~3,9 mm/px).
Por eso el kickback se valida con una tolerancia ancha (±30%) en vez del ±3% del leverage.

A David le "chirría" que un solo píxel cambie tanto el resultado. Sospecha que puede ser
calidad de la foto. Queremos entenderlo a fondo y allanar el camino para mejorarlo.

## Tu tarea (SOLO análisis — NO modifiques código)

1. **Reproduce y cuantifica** la sensibilidad. Barre la posición del pivote (y de otros
   puntos: eje, pedalier, anclajes) y mide cuánto varían kickback, leverage y recorrido.
   ¿Cuántos grados de kickback por píxel de pivote? ¿Qué puntos son los críticos?
   (Puedes escribir scripts desechables en Python fuera de `backend/`, o replicar el
   solver y razonarlo. NO ejecutes ni modifiques el motor Java de producción.)
2. **Diagnostica la causa raíz:** ¿por qué el kickback es tan sensible (geometría del
   arco pivote→eje) y cuánto pesa la resolución de la foto frente al método en sí?
3. **Propón mitigaciones** ordenadas por relación esfuerzo/impacto. A valorar (no
   exhaustivo): fotos de referencia de mayor resolución; marcado con zoom/sub-píxel;
   marcar el pivote como promedio de varios clics; recomendar una resolución mínima de
   foto; hacer el cálculo más robusto al ruido; y **cuantificar qué precisión de marcado
   hace falta** para alcanzar una tolerancia dada (p. ej. ±10%).
4. Para cada mitigación indica: qué cambiaría en el código/flujo (a alto nivel), esfuerzo
   aproximado, riesgo, y si rompería la API del motor.

## Entregable (lo ÚNICO que debes crear)

Un informe en **`docs/investigaciones/sensibilidad-marcado-pivote-INFORME.md`**, en
español, con: planteamiento, análisis cuantitativo (con tus números), causa raíz, tabla
de mitigaciones (esfuerzo / impacto / riesgo) y una recomendación final priorizada.

**Restricciones:** no modifiques ningún otro archivo, no toques el código del motor, no
abras PRs. El informe es para que el chat principal lo lea y decida qué implementar.

## Punteros técnicos

- Solver monopivote: `backend/src/main/java/com/bikematch/kinematics/solver/MonopivotSolver.java`
- Fixtures de referencia (puntos en píxeles):
  `backend/src/test/java/com/bikematch/kinematics/solver/OrangeSurgeFixture.java`
  y `OrangeStage6Fixture.java`
- Modelo de kickback: `backend/src/main/java/com/bikematch/kinematics/curve/KickbackCurve.java`
- Issue relacionado: #31. Limitaciones generales: `docs/limitaciones-y-mejoras.md`.
