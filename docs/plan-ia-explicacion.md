# IA — explicar la cinemática para quien empieza

> Plan acordado el 9 de septiembre de 2026. Épica #10. La planificación del
> contrato de datos (#103) está cerrada; el servicio y la interfaz siguen sin
> implementación. Este documento concreta el alcance y prevalece sobre el antiguo
> plan que dejaba toda la IA como extra posterior al sistema de cuatro barras.

## Objetivo de producto

BikeMatch debe ayudar a entender una suspensión aunque el visitante no sepa leer
las gráficas. La explicación básica forma parte de ese objetivo; el cuestionario
personalizado y un chat son ampliaciones diferentes.

El primer resultado será breve: qué tendencias muestra la suspensión, qué significan
en palabras sencillas y qué límites tiene la interpretación. Se explican términos
como relación de palanca o sag al usarlos. Las gráficas siguen disponibles y solo
2–4 cifras relevantes acompañan al texto; no hace falta volcar todos los descriptores.

## Orden y tareas

| Etapa | Cuándo | Issue |
|---|---|---|
| Contrato de datos, límites y ejemplos de explicación | Completado antes de cerrar la persistencia de #6 | #103 |
| Servicio que genera y reutiliza el resumen básico | Tras guardado y permisos de #6/#7; Sprint 2 | #104 |
| Texto sencillo en resultados guardados y detalle | Con #3 y tras #104; cierre de Sprint 2 | #105 |
| Cuestionario opcional para personalizar la lectura | Después del resumen básico; Sprint 4 | #10, segundo bloque pendiente |
| Chat y evaluación de una posible modalidad de pago | Futuro, sin decisión comercial cerrada | #106 |

Actualización 10/09: se adelanta el bloque de referencia #108/#31/#109, sin tocar
la rama de seguridad #94; después se retoma esta. La primera explicación no depende de cuatro
barras (#19), kickback avanzado (#31), votos, comentarios ni rankings. No necesita
esperar a que todo el catálogo esté terminado: se prueba primero con una bici
guardada accesible a su dueño y luego se reutiliza en el detalle público.

Las fechas y semanas de la hoja de ruta son orientativas, no una estimación nueva
de entrega. La IA no es un requisito obligatorio del curso, pero el resumen básico
sí es prioritario para el producto acordado. No se elimina silenciosamente como
primer recorte: si tiempo o costes obligan a cambiarlo, se revisa con David.

## Qué se reutiliza y qué falta

El flujo actual ya entrega en `PreviewResponse`:

- `leverageCurve`, `kickbackCurve` y `axlePath`;
- `leverageDescriptors` y `axlePathDescriptors`;
- `travelCheck` y `conditions` (sag, plato y piñón registrados).

Desde el 10/09/2026, las peticiones con ruedas añaden `antiSquatCurve` y
`antiRiseCurve`, y `conditions.modelVersion = monopivot-reference-v2` con
`conditions.reference`: ruedas/radios, CG, corrección angular, modelo y nivel de
validación. Guardar TODOS esos datos con el resultado. La V1 sin ruedas sigue
siendo compatible en la API, pero no debe recibir interpretaciones de V2.

La IA recibirá datos estructurados y reglas de interpretación, no una captura de la
gráfica. El cálculo y las clasificaciones siguen siendo deterministas. El motor no
necesita una dependencia de IA ni una segunda implementación de sus fórmulas.

Antes de cerrar las migraciones de resultados en #6, #103 define cómo conservar
curvas, descriptores, unidades/convenciones, condiciones, aviso de recorrido y versión
del motor. Las capacidades y limitaciones se identifican por esa versión, para poder
reconstruir un contexto coherente desde los resultados guardados.

El contrato de interpretación está en
[`contrato-interpretacion-cinematica.md`](contrato-interpretacion-cinematica.md).
Todavía faltan el adaptador desde esos resultados, el servicio narrativo, su
almacenamiento y la presentación. Esto requerirá código nuevo; la separación actual
permite añadirlo sin rehacer el solver ni las gráficas.

## Separación de responsabilidades prevista

1. El motor calcula y devuelve el resultado.
2. El backend prepara un contexto de interpretación con datos canónicos y reglas.
3. Un servicio narrativo usa ese contexto para pedir un resumen al proveedor.
4. La pantalla muestra el texto y conserva acceso a las gráficas y su evidencia.

El servicio y el adaptador del proveedor vivirán fuera del dominio del motor. El
contrato de #103 ya fija sus datos, capacidades, privacidad y límites antes de
programarlo. El resumen general se consultará
por bicicleta/resultado; el endpoint inicialmente previsto
`POST /api/bikes/{id}/analysis` queda para la personalización con cuestionario.
Las rutas y estados HTTP definitivos se cerrarán en #103/#104, sin modificar ahora
el contrato público de `POST /api/kinematics/preview`.

## Límites que el contexto debe expresar

La fuente de interpretación es `base-conocimiento-cinematica.md`, filtrada por las
capacidades reales de cada versión del motor:

- V1 calcula monopivote simple. No dispone de anti-squat ni anti-rise: no atribuir
  eficacia de pedaleo o comportamiento de frenada a datos que no existen.
- El kickback V1 ignora el efecto del piñón. Aunque este se registre en las condiciones,
  no afirmar que cambiarlo modifica la curva actual ni aplicar automáticamente las
  bandas de un modelo completo de transmisión.
- `monopivot-reference-v2` sí incorpora piñón, anti-squat y anti-rise, pero utiliza
  CG de referencia (1100 mm), radios nominales, cuadro fijo y freno en basculante.
  `ANALYTICAL_REFERENCE` no significa validación experimental. Explicar tendencias
  condicionadas, no eficiencia en porcentaje ni comportamiento individual garantizado.
  No inventar descriptores de AS/AR en sag: actualmente se entregan curvas, y el
  adaptador #103 deberá definir y probar su extracción si la necesita.
  El contrato detallado y mejoras pendientes viven en `modelo-referencia-cinematica.md`.
- Las curvas no garantizan el tacto final: influyen amortiguador, reglaje y piloto.
  Un LR aislado no determina que la bici sea blanda o firme; #34 sigue pendiente.
- Si `travelCheck` avisa, destacarlo antes de interpretar y recomendar revisar el
  marcado. No disimular la incertidumbre con una descripción convincente.
- La orientación de muelle/aire será condicional y apoyada en las reglas revisadas;
  no asegura compatibilidad física ni inventa presiones, clics o recomendaciones
  de productos concretos. Los ejemplos antiguos no prueban el comportamiento de V1.

Se prepararán ejemplos de resultados y textos esperados, con revisión humana y tests
de estructura/errores. Los tests automáticos no garantizan por sí solos que una
explicación generada sea correcta.

## Coste, privacidad y disponibilidad

- Elegir proveedor/modelo y comprobar compatibilidad, coste, cuotas y tratamiento de
  datos en #104. Spring AI sigue siendo candidato; no se añade ninguna dependencia
  ahora ni se cambia Spring Boot para acomodarla. No se promete IA gratuita ilimitada.
- El resumen básico no exige peso ni cuestionario. No se envían correo, contraseña,
  foto ni datos personales para explicar los resultados geométricos.
- Guardar o cachear la explicación por versión del resultado, del contexto, de las
  reglas/prompt y por idioma; invalidarla cuando cambien. Agrupar solicitudes
  simultáneas evita pagar varias veces por el mismo resumen.
- Consultar un resumen respeta los permisos de la bici. Su generación se controla en
  backend con límites y claves privadas; una visita pública no dispara una llamada
  ilimitada al proveedor.
- Un fallo de IA no bloquea cálculo, guardado ni gráficas. Mostrar indisponibilidad o
  un texto por reglas identificado como tal; el modo simulado es solo de desarrollo.
- Los textos aportados por usuarios son datos, nunca instrucciones del sistema.
  La salida se valida y se presenta sin ejecutar HTML generado.
- La personalización futura mantiene respuestas y conversaciones privadas, separadas
  del resumen general. No reutilizar una respuesta personal como texto público ni
  como caché compartida entre usuarios.

## Preview, personalización y chat

El preview anónimo seguirá permitiendo calcular y ver gráficas. La primera generación
con IA se integra con resultados guardados, donde ya existen identidad, permisos y
una versión reutilizable. #105 documentará ese paso sin perder los datos del asistente.
Ampliar la generación al preview anónimo requiere decidir límites; no se presupone
un endpoint de IA público sin control.

El segundo bloque de #10 conserva el cuestionario opcional de peso, estilo y
preferencias de la sección 9 de la base de conocimiento. Se desglosará cuando toque,
reutilizando el mismo contexto y servicio, y sin tratar el peso como suficiente para
asegurar que una bicicleta es adecuada.

#106 reserva la conversación con la IA. Cuotas, historial, retención/borrado y posible
pago quedan por decidir junto con los permisos comerciales de #88. No se crean ahora
suscripciones, campos premium ni una infraestructura de chat.
