# BikeMatch — la idea y el producto

## Qué es este documento

Es el resumen del proyecto: de dónde sale la idea, qué hace la aplicación entregada, qué
decisiones de diseño la sostienen y hacia dónde podría crecer. Está redactado para que
alguien que no ha participado en el proyecto entienda todas las ideas. No pretende enseñar
cómo funciona la suspensión de una bicicleta; solo dar el contexto suficiente para que las
decisiones se entiendan.

---

## Contexto

- Es el **proyecto final de un bootcamp de desarrollo Java**.
- **El backend lo desarrolla el alumno**, apoyándose en IA como herramienta de consulta, pero
  sin que la IA lo haga sola. **El frontend se hace con IA**: es un reto planteado por el
  propio curso, porque no se ha dado nada de frontend y esta es la forma de sacar adelante
  esa parte. Debe poder explicarse.
- La intención es poder **continuar el proyecto después del curso** para tener algo real que
  enseñar a la hora de buscar trabajo.
- **De dónde nace la idea:** hoy ya existen programas (por ejemplo *Linkage* o *BikeChecker*)
  que calculan la cinemática de la suspensión trasera de una bicicleta. El problema es que
  exigen bastante trabajo y conocimiento al usuario: hay que darles una foto lateral
  "limpia", marcar a mano los puntos de la suspensión y después interpretar uno mismo unas
  gráficas técnicas. **La propuesta es hacer algo parecido, pero facilitándoselo al usuario
  aficionado:** guiarlo al marcar los puntos y, sobre todo, traducirle el resultado a
  lenguaje normal.
- **Sobre el alcance real:** es un tema de nicho y probablemente no sea una web con gran
  recorrido comercial. Para el objetivo (proyecto final + pieza de portfolio) eso da igual:
  lo que importa es que el código demuestre que se sabe construir un sistema de verdad.
  Aun así, está desplegada y pensada para poder llevarla a producción.

---

## Glosario mínimo

*Solo para ubicar los términos que aparecen más abajo. No explica la parte técnica.*

- **Cinemática de la suspensión:** cómo se comporta la suspensión trasera de la bici a lo
  largo de su recorrido. Es lo que calcula la aplicación.
- **Pivotes / puntos de giro:** los puntos donde articula la suspensión trasera. El usuario
  los marca sobre la foto y son la entrada del cálculo.
- **Sistemas de suspensión** (monopivote, cuatro barras o Horst link, pivote alto…): distintas
  formas de construir la suspensión trasera. Importan porque **el cálculo es diferente para
  cada uno**.
- **Relación de palanca y progresión, trayectoria del eje, pedal kickback, anti-squat,
  anti-rise:** los resultados que calcula el motor. Qué significan está en
  [`base-conocimiento-cinematica.md`](base-conocimiento-cinematica.md).
- **Disciplinas** (Enduro, E-Enduro, Descenso): tipos de bicicleta de montaña cuyo público sí
  se interesa por la cinemática. XC queda fuera.

---

## La aplicación entregada

### Flujo del usuario

1. **Analizar sin cuenta.** El asistente pide una foto lateral y el sistema de suspensión,
   guía el marcado punto a punto (6, 9 o 10 puntos), pide el tipo de bici y unas pocas
   medidas y muestra al momento las cinco curvas.
2. **Guardar con cuenta.** Tras registrarse o iniciar sesión, sin perder el análisis, el
   usuario guarda la bici en privado con su foto y recibe una explicación en lenguaje
   sencillo.
3. **Publicar.** El dueño puede solicitar que su bici se publique. Nace **pendiente** y pasa
   a **pública** cuando un moderador la aprueba (mismo patrón que "tu anuncio tardará un
   rato en publicarse" de los portales de anuncios). El moderador también puede rechazarla
   o retirarla con un motivo, que le llega al dueño como aviso.
4. **Consultar.** Cualquiera puede explorar el catálogo por categorías y abrir la ficha de
   una bici pública: foto, datos, explicación y gráficas, sin repetir el marcado.

Hay tres roles: **usuario**, **moderador** y **administrador** (este último concede o retira
el rol de moderador). Interfaz y explicación están en inglés y en español.

### Núcleo: el motor de cinemática

Es la parte central y la más exigente del proyecto.

- A partir de los pivotes marcados, **calcula las curvas de comportamiento** de la
  suspensión: relación de palanca, trayectoria del eje, pedal kickback, anti-squat y
  anti-rise.
- Es **código determinista, sin IA**: cálculos geométricos, no una interpretación.
- **La "forma" de las curvas también son datos:** dos bicis pueden tener el mismo porcentaje
  de progresión y comportarse de forma muy distinta. Por eso el motor saca varios
  descriptores de la propia curva (valores en el sag, forma en tres fases…), no un único
  número.
- **Sistemas soportados:** monopivote y tres variantes de cuatro barras. El método está en
  [`fundamentos-motor-cinematica.md`](fundamentos-motor-cinematica.md).
- **Validación:** contra curvas publicadas de bicis reales, con recorrido y palanca dentro
  del ±3 % en monopivote.

### Papel de la inteligencia artificial

Decisión de diseño clave, para evitar el principal riesgo (que la IA se invente o falle):

- **La IA no decide la física.** Todo lo que se puede calcular o clasificar (si una bici es
  más progresiva o más lineal, etc.) lo hace el motor mediante reglas sobre sus números.
- **La IA solo "traduce":** recibe los números y clasificaciones ya calculados y redacta un
  resumen breve, acompañado de 2–4 cifras para poder contrastarlo. Si no responde, un texto
  por reglas la sustituye y la web lo indica.
- **No hay que "entrenar" ningún modelo:** se trabaja con *prompting* (darle al modelo el
  contexto y los números) y con reglas codificadas.

Detalle en [`contrato-interpretacion-cinematica.md`](contrato-interpretacion-cinematica.md).

### Decisiones de producto

- **Sin geometría del cuadro.** Pedirla añadiría fricción y la cinemática funciona sin ella.
- **Tipo de bici en lugar de dientes.** Cualquiera sabe si su bici es de enduro, pero no
  siempre los dientes de su transmisión: el tipo fija un desarrollo de referencia.
- **Sag fijo del 30 %** para que las cifras «en el sag» se puedan comparar entre bicis.
- **Moderación antes de publicar:** una foto mal marcada podría dar datos engañosos a toda
  la comunidad.
- **Una bici guardada no cambia su foto ni sus puntos:** repetir el marcado crea otro
  análisis.

---

## Futuro

Ideas que quedaron fuera del MVP, con su issue cuando existe:

- **Comunidad:** votos (#9), rankings por categoría (#4), comentarios (#86) y perfil público
  con un rango que premie contribuciones aprobadas, nunca con permisos (#87).
- **Comparador:** 2 a 4 bicis con sus curvas superpuestas y una comparación con IA (#11).
- **Personalización:** cuestionario opcional (peso, estilo, preferencias) y chat sobre una
  bici (#106).
- **Modelo gratuito y de pago:** hipótesis de dos bicis privadas como máximo en la cuenta
  gratuita, con las públicas sin límite para favorecer el catálogo (#88). Se diseñarían
  planes y permisos antes de integrar un proveedor de pagos.
- **Motor:** otros sistemas (pivotes virtuales, pivote alto con polea, frenos flotantes),
  simulador de emparejamiento con resortes genéricos y calibración con referencias más
  largas. Ver [`limitaciones-y-mejoras.md`](limitaciones-y-mejoras.md).
- **Descartado:** detectar los pivotes automáticamente en la foto (visión por computador;
  hoy no es fiable) y extraer la geometría de las webs de las marcas (frágil y costoso de
  mantener).
