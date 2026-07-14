# Proyecto final — Web de análisis de cinemática de bicicletas de montaña

*(Nombre por decidir)*

---

## Qué es este documento

Es el resumen de trabajo del proyecto. Recoge la idea, las decisiones que ya se han tomado y lo que queda por decidir. Está redactado para que alguien que no ha participado en las conversaciones previas pueda entender todas las ideas. No pretende enseñar cómo funciona la suspensión de una bicicleta; solo dar el contexto suficiente para que las decisiones se entiendan.

---

## Contexto

- Es el **proyecto final de un bootcamp de desarrollo Java**.
- **El backend lo desarrolla el alumno**, apoyándose en IA como herramienta de consulta, pero sin que la IA lo haga sola. **El frontend se hace con IA**: es un reto planteado por el propio curso, porque no se ha dado nada de frontend y esta es la forma de sacar adelante esa parte.
- El curso **no obliga a terminar** el proyecto, sino a implementar ciertas historias de usuario. La intención es poder **continuar el proyecto después del curso** para tener algo real que enseñar a la hora de buscar trabajo.
- **De dónde nace la idea:** hoy ya existen programas (por ejemplo *Linkage* o *BikeChecker*) que calculan la cinemática de la suspensión trasera de una bicicleta. El problema es que exigen bastante trabajo y conocimiento al usuario: hay que darles una foto lateral "limpia" (totalmente de lado, sin perspectiva desde arriba ni desde abajo), marcar a mano los puntos de la suspensión, y después interpretar uno mismo unas gráficas técnicas. **La propuesta es hacer algo parecido, pero facilitándoselo al usuario aficionado:** guiarlo al marcar los puntos y, sobre todo, traducirle el resultado a lenguaje normal.
- **Sobre el alcance real:** es un tema de nicho y probablemente no sea una web con gran recorrido comercial. Pero para el objetivo (proyecto final + pieza de portfolio para buscar empleo) eso da igual: lo que importa es que el código demuestre que se sabe construir un sistema de verdad. Se plantea, aun así, pensada para poder llevarla a producción, por si en el futuro se decidiera lanzarla.

---

## Glosario mínimo

*Solo para ubicar los términos que aparecen más abajo. No explica la parte técnica.*

- **Cinemática de la suspensión:** cómo se comporta la suspensión trasera de la bici a lo largo de su recorrido. Es lo principal que calcula la aplicación.
- **Geometría:** las medidas del cuadro (alcance, ángulos, etc.). En esta app es un dato secundario y opcional.
- **Pivotes / puntos de giro:** los puntos donde articula la suspensión trasera. El usuario los marca sobre la foto y son la entrada del cálculo.
- **Sistemas de suspensión** (monopivote, Horst link, high pivot, etc.): distintas formas de construir la suspensión trasera de una bici. Importan porque **el cálculo es diferente para cada uno**.
- **Leverage / progresión, trayectoria del eje, pedal kickback:** los distintos resultados que calcula el motor a partir de los pivotes. Qué significan técnicamente queda fuera de este documento.
- **Disciplinas** (Enduro, Downhill, XC…): tipos de bicicleta de montaña. La app se centra en las disciplinas cuyo público sí se interesa por la cinemática.

---

## La aplicación

### Concepto

Una web donde consultar la cinemática (y opcionalmente la geometría) de bicicletas de montaña. Equivalente a *Linkage* / *BikeChecker*, pero pensada para el usuario aficionado: menos fricción al introducir los datos y resultados explicados en lenguaje natural.

### Reparto del trabajo

- **Backend:** desarrollado por el alumno, en **Java (Spring Boot)**.
- **Frontend:** desarrollado con IA (reto del curso).

### Núcleo: el motor de cinemática

Es la parte central y la más exigente del proyecto.

- A partir de los pivotes que marca el usuario, **calcula las curvas de comportamiento** de la suspensión: leverage/progresión, trayectoria del eje y pedal kickback.
- Es **código determinista, sin IA**: son cálculos matemáticos, no una interpretación.
- **Importante sobre la "forma" de las curvas:** dos bicicletas pueden tener el mismo dato resumido (por ejemplo, el mismo porcentaje de progresión) y comportarse de forma muy distinta según la forma de la curva (una recta, una que empieza pronunciada y se aplana, una que empieza plana y se acentúa al final). Por eso el motor no saca un único número, sino **varios descriptores de la propia curva** (pendiente por tramos, puntos de inflexión, etc.). De esta forma la "forma" también son datos calculados, no algo que haya que interpretar mirando una imagen.
- El cálculo concreto **depende del sistema de suspensión** (por eso hay que decidir qué sistemas se soportan; ver "Decisiones pendientes").
- Los resultados se **validan contra un programa existente** (*Linkage*) para comprobar que el cálculo es correcto.

### Papel de la inteligencia artificial

Esta es una decisión de diseño clave, para evitar el principal riesgo (que la IA se invente o falle):

- **La IA no decide la física.** Todo lo que se puede calcular o clasificar (si una bici es más progresiva o más lineal, etc.) lo hace el motor mediante reglas sobre los números que ya ha calculado. Eso es fiable y no depende de la IA.
- **La IA (un modelo de lenguaje) se encarga solo de "traducir":** coge los números y clasificaciones que produjo el motor, más las respuestas del usuario sobre sus preferencias, y redacta un análisis en lenguaje natural y personalizado. También formula las preguntas al usuario (peso, estilo de conducción ágil o estable, si suele hacer saltos/topes, si prefiere suspensiones progresivas o lineales, etc.).
- Como los números calculados se muestran junto al texto, si la redacción de la IA se desviara, se detectaría a simple vista.
- **Un resultado central de esa traducción en la v1 es la recomendación del tipo de amortiguador** (muelle / aire y volumen de cámara) que casa con el cuadro. Se genera a partir de los descriptores de la curva de leverage y las reglas de emparejamiento de la base de conocimiento (sección 3), **sin necesidad de la gráfica de fuerzas**. El futuro simulador de emparejamiento (ver "Fuera de alcance") solo añadirá la comparación visual de esas curvas.
- **No hay que "entrenar" ningún modelo** (eso sería demasiado costoso para el plazo). Se trabaja con *prompting* (darle al modelo el contexto y los números ya calculados) y con reglas codificadas.

### Flujo del usuario

1. El usuario **busca una bici** en el catálogo público.
2. **Si está:** ve las gráficas ya calculadas y una descripción de su comportamiento con algún consejo.
3. **Si no está:** se registra, **crea la bici** subiendo una foto y marcando los pivotes. Introducir la **geometría es opcional** (se ha decidido no hacerla obligatoria: forzarla añade demasiada fricción y haría abandonar el proceso a muchos usuarios, y la cinemática —que es lo importante— funciona sin ella).
4. Puede **guardar la bici para sí mismo** o **hacerla pública**.

### Comunidad y moderación

- Cuando un usuario **publica** una bici, esta no aparece de inmediato en el catálogo público: nace en estado **pendiente** y pasa a **aprobada** cuando un moderador la revisa (mismo patrón que "tu anuncio tardará un rato en publicarse" de webs como Wallapop). Técnicamente es un campo de estado en el registro más unos permisos por rol; no hay ningún proceso complejo detrás.
- Sobre las bicis públicas, los usuarios pueden **votar y comentar**.
- Esto implica montar **roles** (usuario / moderador), lo cual, además de dar sentido a la comunidad, aporta valor de cara al portfolio (demuestra manejo de permisos y flujos).

### Rankings

*(Idea para dar más recorrido a la web e incentivar que la gente suba sus bicis analizadas.)*

- Rankings por categoría, alimentados por los votos de los usuarios:
  - Bicicletas de **Enduro**.
  - Bicicletas de **Enduro eléctricas**.
  - Bicicletas de **Downhill**.
- **XC queda fuera**, porque ese público no suele interesarse por la cinemática.

### Onboarding (guiar al usuario a marcar los puntos)

- Al crear una bici, se muestra un **esquema sencillo y una explicación breve de cada pivote** (por ejemplo, orientar sobre dónde suele estar el punto de giro principal), para el usuario que tiene interés pero no es experto.
- **Primero se pregunta el tipo de suspensión** (con ejemplos visuales) y, según el sistema elegido, se guía al usuario a marcar los puntos que corresponden a ese sistema concreto.

---

## Fuera de alcance (posible trabajo futuro)

Estas ideas se descartan para la versión inicial pero pueden mencionarse como líneas futuras (en el README quedan bien porque transmiten visión):

- **Detección automática de los pivotes a partir de la foto** mediante visión por computador. Hoy no es viable con fiabilidad suficiente; por eso los programas actuales obligan a marcarlos a mano.
- **Extracción automática de la geometría** desde las webs oficiales de cada marca (scraping): frágil y de mantenimiento costoso.
- La **parte de geometría como foco principal**.
- Cálculos que requieren asumir datos adicionales (como el centro de gravedad), por ejemplo anti-squat / anti-rise.
- **Simulador de emparejamiento de amortiguadores:** comparar, sobre un mismo cuadro, las curvas de fuerza en rueda con resortes genéricos (muelle lineal, aire de cámara grande, aire de cámara reducida) dimensionados para el sag estándar de la disciplina, sin pedir ningún dato al usuario — con un resorte lineal, la forma de la curva de fuerza se deriva directamente de la de leverage. Queda fuera de v1 por el trabajo extra de modelado y validación de esas curvas genéricas, no por falta de datos. Prioridad: después de anti-squat / anti-rise.
- Sistemas de incentivos más elaborados.

---

## Orden de construcción sugerido

1. **Aplicación básica funcionando y desplegada**, con registro/login. Cubre las historias de usuario que exige el curso y garantiza una entrega.
2. **Motor de cinemática**, empezando por el sistema de suspensión más simple para tener el flujo completo funcionando de principio a fin.
3. **Resto de sistemas de suspensión** (los más complejos).
4. **Capa de comunidad y rankings**.
5. **Capa de IA** que redacta el análisis en lenguaje natural.

Este orden asegura entregar algo funcional pronto y dejar la parte más ambiciosa (el motor) como aquello que se sigue puliendo después del curso.

---

## Decisiones pendientes / próximos pasos

- **Definir qué sistemas de suspensión entran en la primera versión.** Es la decisión que más condiciona el trabajo, porque el motor de cálculo es distinto para cada sistema. Propuesta sobre la mesa: empezar por **monopivote** (el más simple, para montar el flujo completo de principio a fin) y después añadir **Horst link**, que entre ambos cubren muchas bicicletas.
- **Escribir las historias de usuario** a partir de este documento.
- **Consultar con alguien con conocimiento profundo de cinemática** para definir qué características de las curvas son relevantes y qué umbrales corresponden a qué comportamiento. Esto no es "entrenar la IA", sino afinar las reglas del motor y el contexto que se le da al modelo de lenguaje.
