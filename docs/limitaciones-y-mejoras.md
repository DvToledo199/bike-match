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

### Ampliación del 10/09/2026 — referencia, no medición personalizada

- Selector Full 29/Mullet/Full 27,5; radios nominales y CG de referencia 1100 mm.
- Cinco curvas, kickback cog-aware y normalización de inclinación consciente de
  radios distintos. Versiones/condiciones incluidas en la API.
- Pendiente: export o fixture externo con geometría y condiciones conocidas para
  contraste estricto (issue #31); validación física de CG/ruedas reales, neumáticos,
  dinámica de horquilla/rueda libre y otros montajes. No afirmar paridad con Linkage.
- Documentación y fórmulas: [`modelo-referencia-cinematica.md`](modelo-referencia-cinematica.md).
- Los apartados 1–3 siguientes explican las decisiones y pruebas históricas de V1;
  su tolerancia ±30% no valida las curvas añadidas ni justifica ignorar el piñón.
- Las dos Orange conservan recorrido y leverage dentro del ±3% en V2. Kickback:
  Surge mejora (35,86° frente a ≈34°); Stage 6 discrepa (28,79° frente a ≈22°).
  No se atribuye automáticamente a error de marcado: queda investigarlo en #31.
  La tabla original Stage 6 sí da 32/50, CDG 1065 y sag 25% F+R; ver comparación
  completa y condiciones todavía no reproducidas en el documento del modelo.

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
- **Motivo histórico:** mantener un modelo defendible para el curso. La suposición
  de que el ruido siempre tapaba el piñón no estaba suficientemente demostrada.
- **Impacto actual:** solo las peticiones legacy sin ruedas conservan este método;
  el frontend usa ahora el cálculo cog-aware. Su contraste externo sigue pendiente.
- **Dónde:** issue #31 (modelo *cog-aware* validado).

### 3. Sensibilidad extrema al marcado del pivote principal
- **Qué:** mover el pivote principal ~5 px cambia el kickback de la Surge de 19° a 35°
  (~1,6°/px). El crecimiento de cadena depende directo del arco pivote→eje.
- **Por qué ahora:** es inherente a marcar a mano sobre foto; para la v1 se asume.
- **Impacto:** limita la precisión del kickback (y del crecimiento de cadena). Se agrava
  con fotos de referencia pequeñas (la Surge es 500×280 px, ~3,9 mm/px).
- **Dónde:** investigación **completada** →
  [`sensibilidad-marcado-pivote-INFORME.md`](investigaciones/sensibilidad-marcado-pivote-INFORME.md).
  **Conclusión acotada:** el ensayo demuestra sensibilidad al marcado, pero no
  descarta errores o limitaciones del motor en comparaciones externas. Se aplican
  mejor foto, zoom y corrección visual. David descartó promediar cinco
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

- **Orientación:** izquierda/derecha y corrección de inclinación de hasta 15° con
  las ruedas seleccionadas; se descuenta la diferencia de radios para mullet.
  Se asume suelo plano y suspensión extendida. No corrige perspectiva ni distingue
  giro de cámara de bicicleta sobre pendiente; radios nominales introducen error.
  Los clientes legacy sin ruedas conservan la normalización antigua sin rotación.
- **Prueba final con foto real — completada (#54):** David marcó manualmente una
  Orange Stage 6 29'' 2020 en la aplicación, a partir de una foto lateral real.
  El cálculo devolvió 149,0 mm de recorrido frente a 150 mm declarados (0,7 % de
  diferencia), y un leverage ratio de 2,75 → 2,66. La referencia de
  [Linkage Design](https://linkagedesign.blogspot.com/2019/11/orange-stage-6-29-2020.html)
  describe la misma bici como casi lineal, 2,775 → 2,675, y cita 22° de
  pedal kickback; BikeMatch obtuvo 20,9°. La coincidencia entra holgadamente
  en la tolerancia del ±3 % para recorrido y leverage; el kickback queda como
  validación orientativa por su sensibilidad al marcado.
- **Acción final duplicada en resultados — aplazada:** al llegar al cuarto paso,
  sigue visible y desactivado el botón `View results`, aunque los resultados ya
  están en pantalla. No tiene función en ese estado y puede confundir. Se
  mantiene sin cambios por ahora: la pantalla de resultados se revisará como
  conjunto antes de decidir si se elimina, se convierte en otra acción o se
  cambia su navegación.
- **Antes de publicar:** configurar secretos, TLS, CORS y límites de tamaño/tasa
  de peticiones, y repetir la revisión de seguridad. El aislamiento local y cero
  avisos conocidos no certifican un despliegue público como seguro.
- **Rendimiento:** gráficas en la carga inicial (~190 kB gzip de JS) para evitar
  reintentos atrapados por imports diferidos. Se mantiene el aviso de tamaño de Vite.

- **Anti-squat / anti-rise:** fuera de V1 histórica; incorporadas como estimaciones
  de referencia en `monopivot-reference-v2`. El CG no se detecta en la fotografía.
- **Solo monopivote en la v1:** los sistemas de 4 barras (Horst link) llegan después
  (issue #19).
