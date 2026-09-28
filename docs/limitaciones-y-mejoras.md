# Limitaciones conocidas y mejoras pendientes

Registro vivo de los **compromisos técnicos** que se asumen en la versión actual y de lo
que habría que revisar o mejorar para una versión "seria". Cada entrada dice **qué** se
dejó pasar, **por qué** se aceptó, qué **impacto** tiene y **dónde** se sigue (issue de
GitHub cuando aplica).

> Convención: cada vez que se toma un atajo consciente, se añade aquí una entrada.

---

## Motor de cinemática

### Sistemas no soportados
- **Qué:** solo monopivote simple y tres variantes de cuatro barras. Quedan fuera los
  monopivotes con bieleta que cambia el accionamiento del amortiguador, los pivotes
  virtuales de dos bieletas cortas, el pivote alto con polea y los frenos flotantes.
- **Impacto:** si se marcan con otro sistema, el cálculo suele rechazarse con un error 400
  o da cifras que no representan la bici.
- **Mejora:** cada sistema necesita su propio modelo y una bici de referencia.

### Validación externa incompleta
- **Qué:** el monopivote cuadra con curvas publicadas de dos bicis (recorrido y extremos
  del LR dentro del ±3 %). El kickback con piñón cuadra en una y difiere en otra (28,8°
  frente a unos 22°). Las cuatro barras se han contrastado con recorridos declarados y con
  la curva de un fabricante, pero no de forma sistemática.
- **Impacto:** los resultados son estimaciones de referencia, no una precisión certificada.
- **Dónde:** kickback en #31; cuatro barras en #222 y #237. Datos completos en
  [`modelo-referencia-cinematica.md`](modelo-referencia-cinematica.md).

### Sensibilidad al marcado
- **Qué:** mover el pivote principal unos 5 px cambió el kickback de una bici de 19° a 35°
  (~1,6° por píxel); el crecimiento de cadena depende directamente del arco pivote → eje.
- **Por qué:** es inherente a marcar a mano sobre una foto. Se mitiga con zoom, ajuste con
  teclado, aviso de baja resolución y una cruz visible por punto (se descartó promediar
  varios clics).
- **Impacto:** el kickback es la cifra más frágil. Objetivo orientativo: ≤ ~1,3 mm por
  píxel de foto.

### Calibración con una sola referencia corta
- **Qué:** la escala sale del ojo a ojo del amortiguador. No pide puntos extra y es igual en
  todas las tallas, pero es una referencia corta.
- **Impacto:** un error de marcado en sus anclajes escala las cifras absolutas (recorrido,
  kickback, retroceso) y también el valor del LR, porque la carrera se introduce aparte en
  milímetros. La progresión y la forma no cambian. El recorrido se contrasta con el
  declarado (±10 %).
- **Mejora:** ofrecer referencias más largas (distancia entre ejes, vainas) o usar la
  distancia entre ejes por defecto.

### Condiciones de referencia, no personales
- **Qué:** ruedas con radio nominal, centro de gravedad a 1100 mm, suelo plano, cuadro fijo,
  cadena directa sin polea y foto lateral sin perspectiva (se corrige una inclinación de
  hasta 15°).
- **Impacto:** anti-squat y anti-rise son tendencias comparables entre bicis, no el
  comportamiento de una persona concreta. Por encima de 10 mm de retroceso del eje la cifra
  no se interpreta (base de conocimiento, sección 4).
- **Mejora:** medidas reales de neumáticos y un centro de gravedad configurable, con una
  explicación clara antes de personalizarlo.

### Forma de la curva por tercios fijos
- **Qué:** la forma se lee en tres tercios fijos del recorrido; no se detecta el punto exacto
  donde cambia la pendiente y un bache local dentro de un tercio se promedia.
- **Por qué:** los tercios son robustos al ruido del marcado; detectar el punto exacto daría
  falsos positivos con fotos normales.
- **Dónde:** #35.

### Dos fórmulas para la progresión
- **Qué:** el motor calcula la progresión total respecto al LR final. Muchos fabricantes la
  publican respecto al LR inicial, así que la misma curva da cifras distintas (35 % frente a
  26 %).
- **Impacto:** comparar un porcentaje de BikeMatch con uno publicado puede confundir; los LR
  inicial y final sí son comparables.
- **Dónde:** conversión en la base de conocimiento, sección 3.

### Kickback con el valor absoluto del giro
- **Qué:** el kickback máximo usa `Math.abs` y podría contar un giro de bielas hacia delante
  como retroceso.
- **Dónde:** #228.

---

## API y datos

### Fuente inmutable del análisis guardado
- **Decisión:** al guardar el primer análisis, foto y puntos quedan bloqueados; repetir el
  marcado crea otra bici. El cálculo y ambos guardados comparten transacción, y subir foto o
  finalizar toman un bloqueo de escritura sobre esa bici.
- **Mejora futura:** editar medidas o desarrollo y recalcular el resultado sin tocar la
  fuente; una bici pública debería volver entonces a moderación.

### La respuesta del motor reutiliza sus records
- **Qué:** `PreviewResponse` agrupa directamente los records del motor en lugar de copiarlos
  a DTOs propios.
- **Por qué:** en la salida no hay nada que validar ni traducir y el único consumidor es la
  propia web (YAGNI).
- **Impacto:** renombrar un campo del motor cambia el JSON y obliga a tocar la web a la vez.
- **Mejora:** DTOs de respuesta si la API se abre a terceros.

### Formato de los errores de seguridad
- **Qué:** los 401 y 403 de Spring Security usan la respuesta de error estándar de Spring
  Boot, mientras el resto de errores usan `ProblemDetail`.
- **Dónde:** #202, junto con documentar en Swagger las operaciones de token opcional.

### Roles y tokens
- **Qué:** un cambio de rol no revoca los JWT ya emitidos: las sesiones abiertas conservan
  sus permisos hasta caducar. Y la regla de no degradar a otro administrador solo está en
  la web; el backend solo impide cambiar el propio rol.
- **Impacto:** no es una escalada de privilegios (la ruta exige `ADMIN`), pero ambas reglas
  deberían vivir en el backend. Ver [`panel-administracion.md`](panel-administracion.md).

### Cuentas
- **Qué:** no se confirma el correo al registrarse ni se puede recuperar la contraseña.
- **Dónde:** #257 y #186 (necesitan enviar correos).

---

## Fotos

### Derechos de las fotos publicadas: solo un aviso
- **Qué:** la aplicación no comprueba de quién es la foto; avisa al elegirla y al pedir la
  publicación de que debe ser propia o con permiso de su autor.
- **Por qué:** resolverlo de verdad necesita asesoramiento legal y un proceso de retirada.
- **Dónde:** #167. La moderación sí puede retirar una bici con aviso al dueño.

### Fotos privadas accesibles por su URL
- **Qué:** la aplicación protege la relación bici-foto por propietario, pero Cloudinary
  entrega una URL HTTPS normal: quien conozca exactamente esa URL podría abrirla.
- **Mejora:** recursos autenticados, URLs firmadas de corta duración o servir la imagen desde
  el backend.

### Fotos huérfanas al borrar
- **Qué:** al borrar una bici se eliminan primero sus datos y, después, su foto. Si Cloudinary
  falla en ese momento, la foto queda almacenada y el fallo queda en el log.
- **Por qué:** es preferible una foto huérfana a una bici a medias que ya no se puede borrar.
- **Mejora:** una tarea periódica que borre las fotos sin bici. Dónde: #181.

### Permisos de la clave de Cloudinary
- **Qué:** en las pruebas locales la clave tenía un rol de administración completo, aunque el
  backend solo necesita subir y borrar imágenes. No se ha comprobado el rol de la clave de
  producción.
- **Mejora:** usar una clave con un rol a medida (subir y borrar, limitado a la carpeta
  `bikematch/` si el plan lo permite) y rotarla.

---

## Explicación con IA

- **Cuota y disponibilidad de Gemini:** en horas de alta demanda o con la cuota consumida,
  Gemini no responde. Se guarda el texto por reglas, la web lo indica y permite reintentar;
  no hay reintentos automáticos para no gastar cuota.
- **Idioma:** la explicación se genera en el idioma del propietario. Quien visita la bici en
  el otro idioma no la ve hasta que se genere también en el suyo (#264).
- **Una sola instancia:** el límite de 30 s entre generaciones vive en memoria del backend;
  con varias instancias habría que sustituirlo por una cuota compartida.
- **Sin explicación en el análisis anónimo:** solo se genera sobre bicis guardadas, para que
  una visita no pueda disparar llamadas de pago sin límite.

---

## Despliegue

- **Plan gratuito de Render:** la API se duerme tras 15 minutos sin visitas y tarda unos dos
  minutos y medio en despertar. Un flujo programado de GitHub Actions la mantiene despierta y
  la web espera hasta 3 minutos (4 al generar la explicación). Con 512 MB y una décima de
  CPU, la JVM se ajustó para arrancar antes.
- **Base de datos accesible desde internet:** Neon exige conexión cifrada y contraseña. La
  alternativa privada dentro de Render caduca a los 30 días en el plan gratuito.

---

## Interfaz web

- **Borrador en memoria:** el análisis sin guardar se conserva al navegar por la web, pero se
  pierde al recargar la página.
- **Trackpad:** la rueda del ratón y el pellizco hacen zoom; mover la foto con dos dedos
  necesita distinguir el ratón del trackpad (#300).
- **Móvil:** la interfaz se adapta a pantallas estrechas, pero el marcado táctil y la prueba
  en dispositivos quedan para #162.
- **Rendimiento:** las gráficas se cargan con la aplicación (~190 kB comprimidos) para evitar
  que un import diferido fallido atrape el reintento; Vite avisa del tamaño.
