# Explicación con IA: contrato y funcionamiento

Cómo BikeMatch convierte los números del motor en un resumen en lenguaje sencillo, qué
datos usa, qué no puede afirmar y cómo se protege el coste y la privacidad. Épica #10:
contrato #103, servicio #104, pantalla #105 y guardado desde el asistente #161.

## 1. Objetivo e idea central

BikeMatch debe ayudar a entender una suspensión aunque quien la mire no sepa leer las
gráficas. El resumen es breve: qué tendencias muestra la suspensión, qué significan en
palabras sencillas y qué límites tiene la lectura, con 2–4 cifras que lo respaldan. Las
gráficas siguen visibles; el texto no las sustituye.

El motor es la fuente de verdad: calcula números, curvas y clasificaciones. La IA no
calcula física ni decide por intuición si una bici es progresiva; solo redacta a partir
de datos que ya existen.

Es como separar una hoja de resultados de un comentarista deportivo: el marcador sale del
partido y el comentarista lo traduce para quien no lo ha visto. El comentario no puede
cambiar el marcador.

## 2. Cómo funciona hoy

1. El propietario guarda su bici desde el asistente. La web pide la explicación con
   `POST /api/bikes/{id}/interpretation?language=es|en`, en el idioma de la interfaz.
2. El backend construye un **contexto de interpretación** a partir del resultado guardado
   (sección 5) y se lo pasa al **proveedor** configurado.
3. La respuesta se valida y se guarda. La ficha la muestra con su procedencia: «resumen
   generado con IA» o «resumen basado en reglas».
4. Cualquier visita a una bici pública la lee con `GET` sobre la misma ruta. **Una lectura
   nunca llama al proveedor**: si no hay explicación guardada, no se genera.

El preview anónimo del asistente calcula y muestra las gráficas, pero no pide explicación.

**Proveedores** (`INTERPRETATION_PROVIDER`):

- `rules` (por defecto): textos fijos por reglas, en `messages.properties` y
  `messages_es.properties`. Funciona sin red ni coste; sirve en desarrollo, en tests y
  como reserva.
- `google-genai`: Gemini mediante Spring AI (`ChatClient`), con `GEMINI_API_KEY` y
  `GEMINI_MODEL` solo en el backend. Pide la respuesta en JSON, con un único intento y un
  tiempo máximo de 60 s (`GEMINI_TIMEOUT`), sin los reintentos que Spring AI y el cliente
  de Google harían por defecto.

**Si Gemini no responde** (tiempo agotado, alta demanda o cuota consumida), el backend
guarda el texto por reglas marcado como `source=RULES` y `fallback=true`. La web avisa de
que la IA no ha respondido, muestra ese texto como descripción aproximada y ofrece volver
a intentarlo. Las gráficas, el guardado y el resto de la ficha no se ven afectados.

**Tiempos:** la web espera hasta 4 minutos al generar, porque la API gratuita puede estar
despertando; las demás peticiones esperan hasta 3 minutos.

## 3. Resultado guardado del que parte la explicación

Cada bici tiene un único resultado actual (`kinematics_results`, relación 1:1 con `bikes`).
`POST /api/bikes/{id}/analysis` calcula y guarda en una misma transacción los puntos de
origen y ese resultado; si el cálculo falla, no se guarda nada.

| Campo | Contenido | Motivo |
|---|---|---|
| `result_version` | Versión de este formato (`1`) | Poder leer resultados antiguos |
| `engine_version` | Versión del motor que calculó los datos | No aplicar reglas de una versión a otra |
| `curves` (jsonb) | Todas las muestras de las cinco curvas | Gráficas y futuras comparaciones |
| `descriptors` (jsonb) | Descriptores, comprobación del recorrido y condiciones | Lectura rápida y explicación |
| `capabilities` (jsonb) | Qué afirmaciones permite este resultado | Que el texto no hable de lo que no se calculó |
| `computed_at` | Momento del cálculo | Trazabilidad |

Tras el primer resultado, la foto y los puntos son inmutables: corregirlos crea otro
análisis. No se guardan en el resultado ni el correo, ni el nombre de usuario, ni la foto.

## 4. Capacidades por versión del motor

La explicación se basa en `engineVersion` y `capabilities`, no en el aspecto de los campos.

| Versiones | Puede explicar | No puede afirmar |
|---|---|---|
| Sin ruedas: `monopivot-v1`, `horst-link-v1`, `horst-link-yoke-v2`, `horst-link-seatstay-v1` | Palanca y progresión, trayectoria del eje y kickback simple | Anti-squat, anti-rise o efecto del piñón |
| Con ruedas: `monopivot-reference-v2`, `horst-link-reference-v1`, `horst-link-yoke-reference-v2`, `horst-link-seatstay-reference-v1` | Las cinco curvas, con kickback sensible al piñón y tendencias de anti-squat y anti-rise | Rendimiento garantizado, ajuste personal o precisión certificada |

La web siempre elige ruedas, así que en la práctica se usan las versiones con ruedas. Sus
ruedas y su centro de gravedad (1100 mm) son **condiciones de referencia**, no medidas de
quien consulta. `ANALYTICAL_REFERENCE` significa que las fórmulas se han probado con casos
analíticos, no que la bici se haya medido en un laboratorio. Detalle en
[`modelo-referencia-cinematica.md`](modelo-referencia-cinematica.md).

## 5. Contexto que recibe el proveedor

El backend construye, desde el resultado guardado, un objeto versionado. Nunca envía una
captura de las gráficas. Forma resumida (ilustrativa):

```json
{
  "interpretationContextVersion": 4,
  "resultVersion": 1,
  "engineVersion": "horst-link-seatstay-reference-v1",
  "rulesVersion": "kinematics-rules-1",
  "language": "es",
  "dataQuality": { "travelCheckPassed": true, "warning": null },
  "capabilities": { "antiSquat": true, "antiRise": true, "cogAwareKickback": true, "referenceOnly": true },
  "evidence": [
    { "key": "totalProgressionPercent", "value": 36.6, "unit": "%" },
    { "key": "leverageRatioAtSag", "value": 2.6, "unit": "ratio" }
  ],
  "allowedTopics": ["leverage", "axlePath", "kickback", "antiSquat", "antiRise"],
  "forbiddenTopics": ["pressure", "clicks", "productModels", "riderSuitability"]
}
```

Además de las cifras, lleva la forma de la curva de palanca (banda de progresión, tendencia
de cada tercio y LR inicial, en el sag y final) y las condiciones del cálculo (categoría,
sag y desarrollo). `evidence` selecciona entre dos y cuatro cifras con unidad, que la web
muestra junto al texto; la IA no puede añadir una cifra que no esté ahí. Anti-squat y
anti-rise solo entran en `allowedTopics` si el resultado los calculó.

La banda de progresión y los criterios de lectura salen de la
[base de conocimiento](base-conocimiento-cinematica.md). El programa no lee ese documento:
sus reglas están implementadas en el código, que cita la sección de la que salen.

## 6. Versiones y caché

Cada explicación guardada lleva `resultVersion`, `interpretationContextVersion`,
`rulesVersion`, idioma, proveedor y su versión (`rules-5` o el prompt
`interpretation-prompt-6` de Gemini). Si cambia cualquiera, la explicación deja de valer y
se vuelve a generar: es un derivado reemplazable, nunca sustituye a las curvas.

- **Generar** reutiliza solo la explicación del proveedor configurado: con Gemini activo,
  una bici que solo tiene el texto por reglas se le vuelve a pedir a Gemini.
- **Leer** devuelve la que haya guardada, sea de Gemini o de la reserva.
- Las explicaciones se guardan **por idioma** (`en` y `es`).

## 7. Reglas de la explicación

1. Si `travelCheck.withinTolerance` es `false`, el resumen empieza avisando de que el
   recorrido calculado no coincide con el declarado y pide revisar foto, marcado y
   calibración, sin conclusiones firmes.
2. Sin ruedas no menciona anti-squat, anti-rise ni el efecto de cambiar de piñón.
3. Con ruedas puede describir tendencias de pedaleo y frenada como referencia geométrica,
   nunca como promesa de eficiencia, agarre o seguridad.
4. No es una recomendación personal: sin peso, estilo, muelle, amortiguador, presión,
   clics, compatibilidad ni marcas o productos.
5. No oculta limitaciones ni presenta el centro de gravedad o las ruedas de referencia como
   datos reales de la persona. La advertencia sobre el alcance del análisis la muestra la
   web una sola vez, al pie de la explicación, no el texto.
6. La salida se valida como texto plano (sin HTML) con evidencias conocidas y límites de
   tamaño, y se muestra como texto.

**Estructura del resumen:** carácter de la bici en una frase, las cifras que lo sostienen,
pedaleo, frenada y para quién encaja (base de conocimiento, sección 10).

**Ejemplo de recorrido sospechoso** (cifras ilustrativas): «Antes de interpretar la
suspensión, revisa el marcado y la calibración: el recorrido calculado difiere un 14 % del
declarado. Las curvas se mantienen visibles como ayuda para corregir los puntos, pero no se
emite una conclusión sobre su comportamiento.»

## 8. Permisos, privacidad y coste

- **Quién genera:** solo el propietario de la bici, sobre un resultado guardado. Hay un
  enfriamiento de 30 s por propietario y bici (`INTERPRETATION_GENERATION_COOLDOWN`).
  Si el backend se desplegara en varias instancias, habría que sustituirlo por una cuota
  compartida.
- **Quién lee:** cualquiera en una bici pública; en una privada, solo su propietario.
- **Qué viaja al proveedor:** solo el contexto de la sección 5. No viajan correo,
  contraseña, nombre de usuario, foto ni puntos de marcado.
- **Seguridad:** las claves del proveedor viven solo en el backend, y los textos de los
  usuarios se tratan como datos, nunca como instrucciones.
- **Coste:** no se promete IA gratuita ilimitada; la cuota depende del proveedor y del
  modelo. La caché evita repetir llamadas para el mismo contexto.

## 9. Futuro

- **Cuestionario opcional** de peso, estilo y preferencias para personalizar la lectura
  (base de conocimiento, sección 9). Tendrá su propio contrato y nunca reutilizará la
  caché del resumen público.
- **Chat** sobre una bici y posible modalidad de pago (#106), con historial privado.
- **Explicación en el idioma de quien la lee**, aunque el propietario la generara en otro
  (#264).
- **Comparar bicis con IA** (#11).
