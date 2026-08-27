# Encargo de investigación — sensibilidad al marcado del pivote

> **Cómo usar este archivo:** abre un chat NUEVO de Claude Code en este repo y dile
> *"Lee y ejecuta `docs/investigaciones/sensibilidad-marcado-pivote.md`"*. Todo el encargo
> está aquí dentro; ese chat hace el análisis y deja su informe en el archivo indicado
> abajo, **sin tocar el código de producción**. Después, el chat principal lee el informe
> y se decide qué implementar.

---

## Contexto

BikeMatch (repo `bike-match`) calcula la cinemática de la suspensión trasera de bicis de
MTB a partir de puntos marcados **a mano** sobre una foto lateral. El motor (Java, paquete
`com.bikematch.kinematics`) es dominio puro y determinista.

**El problema a resolver:** el pedal kickback —y en general el crecimiento de cadena— es
**muy sensible al marcado del pivote principal**. Medido sobre el fixture de la Orange
Surge, mover el pivote ~5 px cambia el kickback a tope de recorrido de **19° a 35°**
(~1,6°/px). La foto de referencia de la Surge es pequeña (500×280 px, ~3,9 mm/px). Por eso
el kickback hoy se valida con una tolerancia ancha (±30%) en vez del ±3% del leverage. El
objetivo de este encargo es **entender la causa raíz y proponer soluciones concretas y
probadas** para reducir esa sensibilidad.

## Tu tarea (analizar y PROBAR soluciones; NO modificar el código de producción)

1. **Lee lo fundamental del motor** para entender de qué depende el cálculo: el solver
   (`MonopivotSolver`), los fixtures de referencia y el modelo de kickback (`KickbackCurve`).
2. **Reproduce y cuantifica** la sensibilidad: barre la posición del pivote (y de los demás
   puntos: eje, pedalier, anclajes) y mide cuánto varían kickback, leverage y recorrido.
   Identifica qué puntos son críticos y cuánta imprecisión de marcado tolera cada métrica.
3. **Diagnostica la causa raíz:** ¿por qué el kickback es tan sensible (geometría del arco
   pivote→eje), y cuánto pesa la resolución de la foto frente al método en sí?
4. **Prototipa y PRUEBA soluciones candidatas** en scripts desechables (Python o similar,
   fuera de `backend/`), iterando hasta demostrar **con números** cuáles reducen de verdad
   la sensibilidad y en cuánto. Candidatas a valorar (no exhaustivo): fotos de mayor
   resolución; marcado con zoom/sub-píxel; marcar el pivote como promedio de varios clics;
   suavizar/ajustar el arco; recomendar una resolución mínima de foto; cuantificar qué
   precisión de marcado hace falta para llegar a, p. ej., ±10%.
5. Para cada solución probada indica: **cuánto reduce la sensibilidad (con datos)**, qué
   habría que cambiar en el código/flujo (a alto nivel), esfuerzo, riesgo, y si rompería la
   API del motor.

## Entregable (lo ÚNICO que debes crear)

Un informe en **`docs/investigaciones/sensibilidad-marcado-pivote-INFORME.md`** (en español)
con: planteamiento, análisis cuantitativo (tus números), causa raíz, y una tabla de
**soluciones probadas** ordenadas por relación esfuerzo/impacto, cada una con la mejora
medida, el cambio de código que implicaría, esfuerzo y riesgo. Termina con una
**recomendación clara** que permita decidir entre: (a) **mantener la v1 tal cual** y dejar
la mejora para el final, o (b) **implementar ahora** una de las soluciones (cuál y por qué).

**Restricciones:** no modifiques ningún archivo salvo ese informe, no toques el código del
motor, no abras PRs. Puedes crear scripts de análisis desechables fuera de `backend/` y
borrarlos al terminar.

## Punteros técnicos

- Solver monopivote: `backend/src/main/java/com/bikematch/kinematics/solver/MonopivotSolver.java`
- Fixtures de referencia (puntos en píxeles):
  `backend/src/test/java/com/bikematch/kinematics/solver/OrangeSurgeFixture.java`
  y `OrangeStage6Fixture.java`
- Modelo de kickback: `backend/src/main/java/com/bikematch/kinematics/curve/KickbackCurve.java`
- Issue relacionado: #31. Limitaciones generales: `docs/limitaciones-y-mejoras.md`.
