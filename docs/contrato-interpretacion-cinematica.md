# Contrato de interpretación cinemática

Decisión de la issue #103, 12 de septiembre de 2026. Este documento define la
frontera entre el motor matemático de BikeMatch y la futura capa que lo explica
en lenguaje sencillo. No añade IA, dependencias, tablas ni cálculos: #6 usará este
contrato al guardar un resultado y #104/#105 lo usarán al redactar y mostrar el
resumen.

## 1. Idea central

El motor es la fuente de verdad: recibe puntos y parámetros, calcula números y
curvas, y los devuelve en `PreviewResponse`. La futura IA no calcula física ni
decide por intuición si una bici es progresiva; solo redacta a partir de datos y
clasificaciones que ya existan.

Es como separar una hoja de resultados de un comentarista deportivo: el marcador
sale del partido y el comentarista lo traduce para quien no lo ha visto. El
comentario no puede cambiar el marcador.

El contrato de persistencia no cambia todavía el JSON público de
`POST /api/kinematics/preview`. Define el resultado completo que #6 debe guardar
después de ejecutar ese endpoint internamente.

## 2. Resultado canónico que se guarda

Cada bicicleta tendrá un único resultado actual (`kinematics_results`, relación
1:1 con `bikes`). Al crear o editar la bici se recalcula y se reemplaza de forma
atómica; no se recalcula en cada consulta. El historial de ejecuciones es una
posible mejora futura, no una tabla prematura del MVP.

La migración de #6 debe conservar estos campos, aunque algunos estén duplicados
de forma útil entre columnas y JSON:

| Campo de `kinematics_results` | Tipo previsto | Contenido | Motivo |
|---|---|---|---|
| `bike_id` | FK única | Bici propietaria | Una ficha actual por bici |
| `result_version` | entero | Versión de este formato, inicialmente `1` | Poder leer resultados antiguos |
| `engine_version` | texto | `monopivot-v1` o `monopivot-reference-v2` | Saber qué motor calculó los datos |
| `curves` | `jsonb` | Todas las muestras de las curvas disponibles | Gráficas y futuras comparaciones |
| `descriptors` | `jsonb` | Descriptores, `travelCheck` y condiciones | Lectura rápida y explicación |
| `capabilities` | `jsonb` | Qué afirmaciones permite este resultado | No aplicar reglas de una versión a otra |
| `computed_at` | timestamp con zona horaria | Momento del cálculo | Trazabilidad |

No se guardan por separado el correo, contraseña, nombre de usuario ni la foto en
este resultado. La foto pertenece a la bicicleta; las credenciales pertenecen a
`users` y nunca salen de backend.

### Forma lógica del contenido

La migración concreta podrá serializar los bloques anteriores en las columnas
indicadas, pero el contenido lógico será equivalente a este ejemplo. Los nombres
de las curvas son los nombres reales de `PreviewResponse`:

```json
{
  "resultVersion": 1,
  "engineVersion": "monopivot-reference-v2",
  "curves": {
    "leverageCurve": [{ "wheelTravelMm": 0.0, "ratio": 2.65 }],
    "kickbackCurve": [{ "wheelTravelMm": 0.0, "degrees": 0.0 }],
    "axlePath": [{ "x": 0.0, "y": 0.0 }],
    "antiSquatCurve": [{ "wheelTravelMm": 0.0, "percentage": 112.0 }],
    "antiRiseCurve": [{ "wheelTravelMm": 0.0, "percentage": 78.0 }]
  },
  "descriptors": {
    "leverageDescriptors": { "usefulProgressionPercent": 18.0 },
    "axlePathDescriptors": { "maxRearwardMm": 12.0 },
    "travelCheck": { "withinTolerance": true, "deviationPercent": 3.1 },
    "conditions": {
      "sagPercent": 30.0,
      "chainringTeeth": 32,
      "sprocketTeeth": 50,
      "modelVersion": "monopivot-reference-v2",
      "reference": {
        "wheelConfiguration": "FULL_29",
        "frontWheelRadiusMm": 371.0,
        "rearWheelRadiusMm": 371.0,
        "centerOfGravityHeightMm": 1100.0,
        "photoRotationDegrees": 0.0,
        "motionModel": "FIXED_FRAME_LOCAL_GROUND",
        "brakeModel": "SWINGARM_FIXED",
        "validationLevel": "ANALYTICAL_REFERENCE"
      }
    }
  },
  "capabilities": {
    "cogAwareKickback": true,
    "antiSquat": true,
    "antiRise": true,
    "referenceOnly": true
  }
}
```

El ejemplo solo enseña una muestra por curva para que se pueda leer. En la base de
datos se guardan todas las muestras que entrega el motor. Si una curva no está
calculada en V1, se conserva como lista vacía; no se inventan ceros ni porcentajes.

## 3. Capacidades por versión del motor

La capa de interpretación debe basarse primero en `engineVersion` y en
`capabilities`, no deducir capacidades porque un campo tenga un aspecto concreto.
Así una versión futura podrá añadir datos sin reescribir resultados anteriores.

| Versión | Puede explicar | No puede afirmar |
|---|---|---|
| `monopivot-v1` | Leverage/progresión, trayectoria del eje y kickback simple | Anti-squat, anti-rise o influencia real del piñón: el kickback V1 no lo usa |
| `monopivot-reference-v2` | Las cinco curvas, incluyendo kickback sensible al piñón y tendencias de anti-squat/anti-rise | Rendimiento garantizado, ajuste personal o precisión certificada frente a Linkage |

En V2, `conditions.reference` es obligatoria y se guarda completa. Sus ruedas,
CG de 1100 mm, cuadro fijo y freno en basculante son condiciones de cálculo; no
son medidas del usuario ni de su postura. `ANALYTICAL_REFERENCE` significa que
las fórmulas y los casos analíticos se han probado, no que la bici haya sido
medida en laboratorio.

La comparación externa pendiente de #31 sigue abierta: la Orange Stage 6 no
coincide de forma suficiente en kickback. Por eso los textos de V2 deben hablar de
"estimación de referencia" y no prometer equivalencia con Linkage. Las reglas y
la evidencia completa están en
[`modelo-referencia-cinematica.md`](modelo-referencia-cinematica.md).

## 4. Contexto que recibirá la futura explicación

El adaptador de #104 construirá, desde un resultado guardado, un objeto interno
versionado. No enviará una captura de las gráficas. Su forma mínima será:

```json
{
  "interpretationContextVersion": 1,
  "resultVersion": 1,
  "engineVersion": "monopivot-reference-v2",
  "rulesVersion": "kinematics-rules-1",
  "language": "en",
  "dataQuality": {
    "travelCheckPassed": true,
    "warning": null
  },
  "capabilities": {
    "antiSquat": true,
    "antiRise": true,
    "cogAwareKickback": true,
    "referenceOnly": true
  },
  "evidence": [
    { "key": "usefulProgressionPercent", "value": 18.0, "unit": "%" },
    { "key": "maxRearwardMm", "value": 12.0, "unit": "mm" }
  ],
  "limits": ["Analytical reference; not a personal setup recommendation."],
  "allowedTopics": ["leverage", "axlePath", "kickback", "antiSquat", "antiRise"],
  "forbiddenTopics": ["pressure", "clicks", "productModels", "riderSuitability"]
}
```

`evidence` selecciona entre dos y cuatro cifras canónicas, con unidades, que se
mostrarán junto al texto. La IA no puede añadir una cifra que no esté ahí. El
adaptador añade `antiSquat` y `antiRise` a `allowedTopics` solo cuando existan
esas capacidades; no basta con que el modelo de lenguaje conozca el concepto.

Cada explicación guardada o en caché deberá llevar, como mínimo, `resultVersion`,
`interpretationContextVersion`, `rulesVersion`, idioma y versión del prompt. Si
cambia el motor, las reglas o el prompt, el texto se invalida y se regenera para
el resultado correspondiente. La explicación es un derivado reemplazable; las
curvas numéricas originales nunca se sustituyen por texto.

## 5. Reglas de seguridad de la explicación

1. Si `travelCheck.withinTolerance` es `false`, el resumen empieza avisando de
   que el recorrido calculado no coincide con el declarado. Pide revisar foto,
   marcado y calibración; no ofrece conclusiones firmes ni orientación de ajuste.
2. Con V1 no menciona anti-squat, anti-rise ni el efecto de cambiar de piñón.
3. Con V2 puede describir tendencias de pedaleo y frenada como referencia
   geométrica, pero nunca como una promesa de eficiencia, agarre o seguridad.
4. No convierte el análisis en recomendación personal: sin peso, estilo, muelle,
   amortiguador, presión, clics, compatibilidad física ni productos concretos.
   La futura orientación aire/muelle será condicional, conservadora y trazable a
   reglas revisadas; no se habilita por este contrato.
5. El texto no puede ocultar limitaciones, presentar CG/radios de referencia como
   datos reales de la persona ni usar HTML ejecutable generado por un proveedor.
6. La salida se valida estructuralmente y se muestra como texto. Si falla el
   proveedor, curvas, guardado y gráficas siguen funcionando; se muestra un
   estado de indisponibilidad o un resumen determinista identificado como tal.

## 6. Formato de la primera explicación

El resumen básico de #104/#105 tendrá esta estructura, visible sin necesidad de
leer toda la ficha técnica:

1. Una frase principal sobre la tendencia más relevante.
2. Entre dos y cuatro evidencias numéricas con unidad y nombre comprensible.
3. Un intercambio o límite: qué no permite concluir ese análisis.
4. Si procede, una sugerencia conservadora de seguir mirando las gráficas, no una
   receta de reglaje.

La interfaz distingue siempre su procedencia: "resumen basado en reglas" o
"resumen generado con IA". Ambos enlazan mentalmente con las cifras mostradas y
ninguno reemplaza las curvas.

## 7. Ejemplos de comportamiento esperado

Estos ejemplos son casos de contrato para pruebas humanas y automáticas. Las cifras
son ilustrativas; no describen una bicicleta concreta ni son texto producido por IA.

### A. V2 con comprobación de recorrido correcta

Entrada: `monopivot-reference-v2`, comprobación correcta, progresión útil 18 %,
recorrido trasero máximo 12 mm y curvas AS/AR disponibles.

Resultado permitido: "La relación de palanca baja durante la parte útil del
recorrido, una tendencia progresiva de referencia. La progresión útil es 18 % y
el eje se desplaza hasta 12 mm hacia atrás. También se muestran anti-squat y
anti-rise como estimaciones bajo las ruedas y CG de referencia, no como un ajuste
personal ni una medida de laboratorio."

### B. Recorrido sospechoso

Entrada: cualquier versión con `travelCheck.withinTolerance=false` y desviación
del 14 %.

Resultado permitido: "Antes de interpretar la suspensión, revisa el marcado y la
calibración: el recorrido calculado difiere un 14 % del declarado. Las curvas se
mantienen visibles como ayuda para corregir los puntos, pero no se emite una
conclusión sobre su comportamiento."

### C. V1 con métricas que no existen

Entrada: `monopivot-v1`, comprobación correcta y kickback simple disponible.

Resultado permitido: "El resultado muestra la progresión, la trayectoria del eje
y el kickback del modelo monopivote básico. No incluye anti-squat ni anti-rise,
y este cálculo de kickback no cambia según el piñón seleccionado; por eso no se
extraen conclusiones de pedaleo, frenada o desarrollo."

## 8. Privacidad, permisos y disponibilidad

La generación solo se solicita sobre un resultado guardado al que el solicitante
tenga acceso. La primera versión no genera IA para cada preview anónimo: así una
visita pública no puede disparar llamadas de pago sin límite.

Al proveedor externo solo viajarán el contexto de interpretación necesario, las
capacidades y las cifras seleccionadas. No viajan correo, contraseña, nombre de
usuario, foto, puntos de marcado ni conversaciones personales. Las claves del
proveedor permanecen en backend y las entradas de usuario se tratan como datos,
nunca como instrucciones del sistema.

Un chat personalizado será otra fase (#106): sus respuestas, límites de uso y
retención serán privadas y no se mezclarán con este resumen general ni con una
caché pública.

## 9. Consecuencia para las siguientes issues

- **#6** crea la migración y persiste el resultado con las columnas y el contenido
  de la sección 2. No guarda todavía una explicación generada.
- **#104** crea el adaptador de contexto, las reglas/fallback, proveedor y caché
  versionada respetando las secciones 4, 5 y 8.
- **#105** muestra el resumen junto a la foto, las gráficas y las evidencias de la
  ficha de una bici; no añade un segundo botón para revelar resultados ya calculados.
- **#31** permanece abierto hasta conseguir una referencia externa reproducible;
  este contrato no lo da por resuelto.

Con esto se puede persistir una bici ahora y añadir el texto después sin cambiar
el motor, reescribir las gráficas ni mezclar una estimación de referencia con una
recomendación personal.
