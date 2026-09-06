# Limitaciones conocidas y mejoras pendientes

Registro vivo de los **compromisos técnicos** que se asumen en la versión actual
(MVP del curso) y de lo que habría que revisar o mejorar para una versión "seria".
La idea es no perder de vista ningún atajo: cada entrada dice **qué** se dejó pasar,
**por qué** se aceptó ahora, qué **impacto** tiene y **dónde** se sigue (issue de
GitHub cuando aplica).

> Convención: cada vez que se toma un atajo consciente, se añade aquí una entrada.
> Es la referencia interna del proyecto para las mejoras técnicas pendientes.

---

## Motor de cinemática

### 1. Validación del pedal kickback con tolerancia ±30%
- **Qué:** el test `surgeKickbackMatchesTheReference` valida el kickback contra la
  gráfica de BikeChecker con una banda ancha del **±30%**, no con el ±3% del leverage.
- **Por qué ahora:** el kickback es hípersensible al marcado del pivote (ver punto 3),
  así que una foto marcada a mano no puede clavarlo con precisión.
- **Impacto:** el kickback es una estimación aproximada; en producto se presenta como
  banda ("medio/alto/…"), no como cifra exacta.
- **Dónde:** issue #31.

### 2. Modelo de kickback simplificado (v1)
- **Qué:** el crecimiento de cadena se calcula como el cambio de la distancia recta
  pedalier→eje; se desprecian el enrollado en el piñón y el *wheel wind-up*. El número
  lo fija solo el plato; el piñón se registra como condición de cálculo, no influye.
- **Por qué ahora:** limpio y defendible; el efecto del piñón queda tapado por el ruido
  de marcado (punto 3), así que afinarlo no mejoraría la validación.
- **Impacto:** el motor no distingue el efecto real del piñón en el kickback.
- **Dónde:** issue #31 (modelo *cog-aware* validado).

### 3. Sensibilidad extrema al marcado del pivote principal
- **Qué:** mover el pivote principal ~5 px cambia el kickback de la Surge de 19° a 35°
  (~1,6°/px). El crecimiento de cadena depende directo del arco pivote→eje.
- **Por qué ahora:** es inherente a marcar a mano sobre foto; para la v1 se asume.
- **Impacto:** limita la precisión del kickback (y del crecimiento de cadena). Se agrava
  con fotos de referencia pequeñas (la Surge es 500×280 px, ~3,9 mm/px).
- **Dónde:** investigación **completada** →
  [`sensibilidad-marcado-pivote-INFORME.md`](investigaciones/sensibilidad-marcado-pivote-INFORME.md).
  **Conclusión:** no es un fallo del motor, es resolución de foto × precisión de marcado;
  se aplican mejor foto, zoom y corrección visual. David descartó promediar cinco
  clics: un punto se marca una vez, con cruz visible y deshacer inmediato.
  Objetivo orientativo: mm/px ≤ ~1,3. Relacionado con #31 y #54.

### 4. Guard numérico del `acos` — corregido
- El solver tolera solo ruido de coma flotante; rechaza geometría imposible.
  La auditoría añadió controles de longitudes, divisores y resultados finitos,
  puntos completos y errores HTTP 400 explicados (#33).
- Esto no identifica todos los errores humanos que aún forman una geometría
  matemáticamente posible: sigue siendo necesaria la revisión visual.

### 5. Clasificación de forma por tercios fijos (v1)
- **Qué:** la forma de la curva se lee en tres tercios fijos (inicial/medio/final). El punto
  de transición real (dónde cambia de verdad la pendiente) no se detecta con precisión, y una
  jorobita local dentro de un tercio se promedia.
- **Por qué ahora:** los tercios son robustos al ruido de marcado y captan lo importante
  (incluida la zona de tope = tercio final); detectar el punto exacto punto-a-punto sería
  sensible al ruido → falsos positivos.
- **Impacto:** la forma es correcta "a grandes rasgos"; no da el % exacto donde ocurre el
  cambio ni caza micro-tramos.
- **Dónde:** issue #35 (transición variable + inflexiones + robustez al ruido, para fotos
  de más resolución).

### 6. Calibración con una sola referencia (eye-to-eye) en la v1
- **Qué:** la conversión px→mm usa solo el **eye-to-eye del amortiguador**. No requiere
  puntos extra y es igual en todas las tallas, pero es una referencia **corta** → más
  sensible al error de marcado en la escala.
- **Por qué ahora:** simple y sin puntos extra. La escala afecta recorrido, kickback
  y retroceso, y **también puede cambiar leverage y el tramo de curva**, porque la
  carrera se introduce aparte en mm y no se escala con la foto. El recorrido se
  contrasta además con el declarado (#17).
- **Impacto:** menor precisión de escala que con una referencia larga.
- **Dónde:** mejora futura en #6 (crear bici): ofrecer varias referencias
  (**vainas / wheelbase / amortiguador**, como BikeChecker) o usar la **wheelbase** por
  defecto (más larga = más precisa, y fácil de encontrar en la ficha).

---

## API REST

### 7. La respuesta reutiliza los records del dominio (sin DTOs de salida)
- **Qué:** `PreviewResponse` agrupa directamente los records del motor (series de
  curvas, descriptores, `TravelCheck`) en lugar de copiarlos a DTOs de respuesta propios.
- **Por qué ahora:** en la salida no hay nada que validar ni traducir, y la serialización
  a JSON ya entrega una copia desprendida (los records son inmutables y el front recibe
  texto, no el objeto Java). Con un único consumidor controlado (el propio front), una capa
  de DTOs de salida sería aislamiento para un problema que aún no existe (YAGNI).
- **Impacto:** el contrato JSON queda atado a la forma interna del motor; renombrar un campo
  del dominio cambia el JSON y obliga a tocar el front a la vez.
- **Dónde:** mejora futura si la API se abre a consumidores externos → introducir DTOs de
  respuesta como capa de traducción estable entre el dominio y el contrato público.

---

## Alcance (decisiones de producto, no atajos)

- **Orientación:** se normaliza izquierda/derecha, no perspectiva ni inclinación.
  Se necesita foto lateral, nivelada y suspensión extendida. No se rota usando la
  línea de ejes porque puede haber ruedas de tamaños diferentes.
- **Prueba final pendiente:** #54 requiere foto real de David comparada con una
  referencia y desviaciones documentadas; los tests no la sustituyen.
- **Antes de publicar:** configurar secretos, TLS, CORS y límites de tamaño/tasa
  de peticiones, y repetir la revisión de seguridad. El aislamiento local y cero
  avisos conocidos no certifican un despliegue público como seguro.
- **Rendimiento:** gráficas en la carga inicial (~190 kB gzip de JS) para evitar
  reintentos atrapados por imports diferidos. Se mantiene el aviso de tamaño de Vite.

- **Anti-squat / anti-rise fuera de la v1:** requieren la altura del centro de gravedad
  del conjunto bici+ciclista, dato que no está en la foto. Documentado en
  `fundamentos-motor-cinematica.md` §8.
- **Solo monopivote en la v1:** los sistemas de 4 barras (Horst link) llegan después
  (issue #19).
