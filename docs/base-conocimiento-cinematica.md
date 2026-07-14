# Base de conocimiento — Interpretación de cinemática de suspensión MTB

Material de referencia para la capa de IA de BikeMatch. El motor de la aplicación calcula los números; este documento define cómo traducirlos a una valoración en lenguaje natural, como lo haría un especialista en suspensiones. Está pensado para inyectarse (completo o por secciones) como contexto del modelo de lenguaje.

---

## 1. Reglas de oro para la IA

1. **Los números mandan.** Toda afirmación debe estar ligada a un dato calculado por el motor: "es claramente progresiva (24% de progresión útil)". Nunca inventar cifras ni hablar de métricas que el motor no haya entregado en esta bici. Si un dato no está (p. ej. anti-squat en la v1), no se menciona o se dice explícitamente que no se ha calculado.
2. **Margen de error honesto.** Los puntos provienen de una foto marcada a mano: usar lenguaje de tendencia ("en torno a", "aproximadamente"), no de precisión de laboratorio. Si el motor marca la alerta de recorrido incoherente (calculado vs. declarado), la valoración entera debe advertirlo y rebajar su confianza.
3. **No exagerar diferencias pequeñas.** Entre bicis convencionales muchas métricas son muy parecidas (la trayectoria del eje, sobre todo). Decir "prácticamente idénticas en este aspecto" es una respuesta experta; fabricar diferencias donde los números no las muestran es el error típico del aficionado.
4. **El conjunto importa más que la pieza.** El comportamiento final = cinemática del cuadro + amortiguador (tipo, volumen, hidráulico) + reglaje (sag, presiones, clics) + piloto. Un cuadro "peor" bien equipado y regulado puede funcionar mejor que uno "mejor" mal montado. Recordarlo cuando se den veredictos.
5. **Sin dogmas ni bala de plata.** Casi toda decisión de diseño es un compromiso: lo que se gana por un lado se paga por otro. La valoración debe nombrar los dos lados del compromiso, no vender uno como superior absoluto.
6. **Sin marcas ni productos concretos.** Hablar de tipos y características ("un amortiguador de aire de cámara reducida", "un desacoplador de núcleo"), nunca recomendar marcas o modelos comerciales.
7. **Adaptar al usuario si hay perfil.** Si el usuario ha respondido el cuestionario (peso, estilo, preferencias), la valoración se personaliza con la sección 9. Si no, se da la lectura neutra "para el ciclista tipo de esa disciplina".
8. **Citar las condiciones de medida.** El pedal kickback depende del desarrollo y el anti-squat del punto de sag y el desarrollo: siempre indicar con qué plato/piñón y sag se ha calculado.

---

## 2. Convenciones de lectura

- Las curvas se expresan respecto al **recorrido de la rueda trasera (0–100%)**.
- El **punto de sag** (hundimiento estático con el piloto encima) es la referencia central de análisis. Valores orientativos por disciplina: XC/Downcountry 20–25%, Trail 25–30%, Enduro ~30%, DH ~30%. En BikeMatch, todas las categorías soportadas (Enduro, e-Enduro, DH) se analizan con un **30% por defecto**.
- **La curva se lee partida en dos por el sag.** El tramo inicial (0% → sag) gobierna la sensibilidad, la tracción y la absorción de impactos pequeños. El tramo útil (sag → 100%) gobierna el apoyo, la capacidad de tragar impactos grandes y la resistencia a hacer tope. Este método evita el error clásico de juzgar una curva solo por sus extremos: dos bicis con la misma cifra global pueden comportarse de forma opuesta según a qué altura de la curva les caiga el sag.

---

## 3. Leverage ratio (relación de palanca) y progresión

**Qué es (una línea):** cuántos milímetros recorre la rueda por cada milímetro de carrera del amortiguador. Un LR de 2.8 significa que la rueda "gana" al amortiguador por 2.8 a 1.

**Valores de contexto:** el LR medio de la mayoría de dobles modernas está entre ~2.2 y ~3.2. Un LR medio alto (≥2.9) exige más presión/muelle y un hidráulico más firme; uno bajo (≤2.3) lo contrario. Fuera de ese rango, mencionarlo como rasgo singular.

**Métricas que entrega el motor:** LR inicial, LR en sag, LR final, progresión total, **progresión útil** (la del tramo sag→final: `(LR_sag / LR_final − 1) × 100`), pendiente por tramos y puntos de inflexión.

**La cifra estrella es la progresión útil**, no la total. Bandas orientativas (heurísticas, ajustables):

| Progresión útil | Etiqueta | Lectura rápida |
|---|---|---|
| < 5% | Lineal | Predecible y constante; apoyo uniforme; hará topes con facilidad si no lo compensa el amortiguador |
| 5–12% | Ligeramente progresiva | Versátil con aire; con muelle irá justa de reserva final |
| 12–20% | Progresión media | El punto dulce de polivalencia: admite aire y, en la parte alta de la banda, muelle |
| 20–30% | Alta | Mucha reserva final, tacto con "pop"; territorio natural del muelle |
| > 30% | Muy alta | Difícil aprovechar todo el recorrido; solo tiene sentido para saltos grandes o pilotos muy agresivos |

**Taxonomía de formas** (la forma importa tanto como la cifra):

- **Lineal descendente continua:** comportamiento uniforme; fácil de regular; el apoyo no cambia por sorpresa.
- **Progresiva continua (pendiente suave y constante):** la forma más agradecida; se adapta bien a casi cualquier amortiguador.
- **Cae fuerte al inicio y se aplana (progresiva→lineal):** muy sensible arriba; pero si el sag cae ya en la zona plana, la progresión útil real es baja aunque la total parezca alta. Avisar del riesgo de topes.
- **Plana al inicio y cae al final (lineal→progresiva):** firme en la zona de pedaleo, con reserva de final; carácter saltarín.
- **Con tramo regresivo (el LR sube en algún punto):** el apoyo desaparece justo ahí; la bici "se hunde". Un final regresivo es el peor caso para hacer topes: desaconsejar muelle y sugerir aire con volumen reducido. Un tramo regresivo suave al final (≤ ~0.1 de subida) puede tratarse como "casi lineal" sin alarmar.
- **Regresiva→progresiva (curva en lomo):** si el sag coincide con la cima del lomo, el tramo útil es más progresivo de lo que sugiere la cifra global. Es el ejemplo de manual de por qué se analiza desde el sag.

**Emparejamiento con el amortiguador** (regla de composición: comportamiento total ≈ curva del cuadro × curva del resorte):

- **Muelle** = resorte lineal. Pide cuadros con progresión útil ≳15–20% y **sin** tramo regresivo final. En cuadros muy progresivos funciona de maravilla (sensibilidad de muelle + reserva del cuadro).
- **Aire de cámara reducida** = resorte muy progresivo. Rescata cuadros lineales o regresivos.
- **Aire de cámara grande** = resorte poco progresivo. Casa con cuadros de progresión media-alta; los reductores de volumen (spacers) permiten afinar entre medias.
- Dos caminos distintos pueden llevar al mismo sitio: cuadro lineal + aire progresivo ≈ cuadro progresivo + aire de gran cámara. Por eso la pregunta correcta no es "¿qué cuadro es mejor?" sino "¿qué combinación te deja en el comportamiento que buscas?".
- **Hidráulico:** un LR medio alto tiende a necesitar compresión más firme; en cuadros muy progresivos, la parte final del recorrido ya frena por sí sola y conviene no pasarse de compresión de alta velocidad.

**Frases tipo:** "Con un 22% de progresión útil y una curva continua, admite tanto aire como muelle sin manías"; "La cifra global (18%) engaña: casi toda la progresión ocurre antes del sag, así que en el uso real se comporta casi lineal y agradecerá spacers".

---

## 4. Trayectoria del eje trasero (axle path)

**Qué es:** el dibujo que traza el eje de la rueda al comprimirse la suspensión, visto de lado. Se resume en el **retroceso máximo (mm)** y en qué tramo ocurre.

**Bandas orientativas de retroceso máximo:** <2 mm despreciable · 2–5 mm leve · 5–10 mm apreciable · >10 mm marcado (territorio de pivote alto con polea, donde puede superar los 20–25 mm).

**Lectura:** una trayectoria con retroceso inicial ayuda a tragar impactos frontales (escalones, pedreras) y a que la bici no se "atranque"; a cambio alarga las vainas al comprimir (más estable, menos juguetona) y aumenta el pedal kickback. Una trayectoria vertical o que avanza pronto hace la bici más ágil y de recuperación más rápida, a costa de encajar peor el golpe cuadrado.

**Regla de honestidad específica:** entre bicis convencionales (sin polea elevada), la inmensa mayoría de trayectorias son casi calcadas; las diferencias reales solo aparecen en los extremos del espectro o en diseños de pivote alto. Si el retroceso es <3 mm, lo experto es decir "trayectoria convencional, sin efecto perceptible", no construir un relato sobre décimas de milímetro.

---

## 5. Pedal kickback (retroceso de pedal)

**Qué es:** los grados que giran las bielas hacia atrás cuando la suspensión se comprime bruscamente, por el crecimiento de la distancia entre pedalier y eje (tensión de cadena).

**Depende del desarrollo:** el valor cambia mucho según plato y piñón; el caso más desfavorable es el desarrollo de subida (piñón grande). **Siempre citar el desarrollo con el que se ha calculado.** Bandas orientativas *medidas en el desarrollo de subida*: <20° bajo · 20–35° medio · 35–45° alto · >45° muy alto. (Con desarrollos de bajada los valores caen a menos de la mitad; no mezclar escalas.)

**Está acoplado a la eficacia de pedaleo:** en monopivotes es casi proporcional al anti-squat: cuadro que pedalea firme = cuadro con kickback. Algunos diseños de pivote virtual, y sobre todo los de pivote alto con polea, rompen esa proporción y logran pedaleo firme con kickback bajo; cuando los números muestren ese desacople, destacarlo como virtud de diseño.

**Cuándo se siente de verdad:**
- Pedaleando por terreno roto (subidas técnicas): tirones en los pedales.
- En bajada, la visión clásica dice que apenas afecta porque no se pedalea y el núcleo libre lo absorbe; la experiencia moderna matiza: con bielas en horizontal, frenando en pedrera, con bujes de poco engagement o transmisiones siempre engranadas, parte de ese tirón llega a los pies como castigo e inestabilidad. En valores altos (>35–40° en subida), mencionar este efecto en bajadas rotas es pertinente, especialmente si el usuario reporta pies castigados o pérdida de apoyo.
- **E-bikes:** doble matiz. El motor y la transmisión hacen que el retroceso se note más, y además en e-bike se pedalea en tramos donde con una bici convencional no se pedalearía; un kickback alto penaliza más en una e-bike de enduro que en una DH pura (donde casi no se pedalea y suele venir acompañado de una trayectoria de eje favorable: allí es más peaje que defecto).
- Existen paliativos genéricos que se pueden mencionar sin marcas: desacopladores de núcleo/araña, bujes de bajo engagement (con sus contras), y elegir desarrollos menos extremos.

---

## 6. Anti-squat (eficacia de pedaleo) — para cuando el motor lo calcule

**Qué es:** en qué medida la tensión de cadena contrarresta el hundimiento de la suspensión al pedalear. 100% = pedaleo neutro (ni se hunde ni se extiende).

**Cómo se lee:** el dato que importa es el **valor en la zona de sag y en el desarrollo de subida**; la cantidad pesa más que la forma de la curva. Bandas orientativas en sag: <80% blando al pedalear (balanceo perceptible, tacto muy sensible) · 80–100% equilibrado · 100–120% eficaz y firme (la referencia del buen pedaleador) · 120–140% muy firme, con el peaje de más kickback · >140% extremo.

**La forma, para hilar fino:** una curva plana mantiene el mismo carácter incluso en rampas empinadas, donde el peso carga atrás y la suspensión trabaja más comprimida de lo normal (rasgo muy deseable en bicis de pedalear). Una curva descendente logra la misma eficacia en el sag con menos kickback en la parte profunda del recorrido: buen compromiso para bicis de bajada. El debate abierto del sector es cuán ancha es la "zona de pedaleo real" (¿20–40% del recorrido? ¿20–60%?); ante la duda, valorar el intervalo alrededor del sag.

**E-bikes:** con asistencia, la penalización de un anti-squat modesto se reduce (el motor compensa) y el confort/tracción ganan peso; no exigir el 120% a una e-bike como se le exigiría a una enduro muscular.

---

## 7. Anti-rise (comportamiento al frenar) — para cuando el motor lo calcule

**Qué es:** en qué medida la frenada trasera comprime o deja libre la suspensión trasera. 0% = la suspensión ignora el freno y sigue su comportamiento natural (extenderse por la transferencia de peso); 100% = la frenada mantiene la geometría hundiendo lo justo.

**Lectura por bandas:** <50% suspensión muy activa frenando, más cabeceo de la bici · 50–80% el equilibrio más común en bicis modernas · 80–110% geometría muy estable al frenar, a costa de una suspensión que trabaja peor y pierde finura de tracción justo cuando frenas · >110% alto, carácter de "se sienta al frenar".

**Matices de experto:** el cabeceo lo domina la horquilla (el freno delantero es responsable de la gran mayoría del hundimiento frontal), así que el anti-rise trasero afina el comportamiento pero no lo decide; hay bicis rápidas con enfoques opuestos y los pilotos se adaptan. Presentarlo siempre como compromiso con dos caras, nunca como nota buena/mala.

---

## 8. Contexto que modula toda la valoración

Antes de redactar, la IA debe considerar: **disciplina y recorrido** (a una DH se le pide reserva y estabilidad; a una trail, eficacia y agilidad), **si es e-bike** (peso extra, kickback más presente, anti-squat menos crítico, se pedalea en más sitios), **medidas del amortiguador** (una carrera larga respecto al recorrido implica LR bajo y viceversa) y **el sag recomendado de la disciplina**, que fija dónde se corta la curva.

---

## 9. Cruce con el perfil del usuario (cuestionario)

Mapa de respuestas → qué métricas mirar y hacia dónde inclinar el consejo:

- **"Hago saltos grandes / suelo hacer topes"** → progresión útil alta (≥20%) y sin final regresivo; si la bici es lineal, recomendar compensación por amortiguador (volumen reducido/spacers) y ser franco con el límite.
- **"Quiero tacto sensible / máxima tracción"** → tramo inicial de la curva (0→sag) y tipo de resorte: aquí el muelle o un buen aire de gran cámara marcan más diferencia que el cuadro; anti-squat moderado suma tracción al pedalear.
- **"Prefiero bici estable y planchadora"** → retroceso de eje (si lo hay), vainas que crecen al comprimir, anti-rise medio-alto, progresión con buen apoyo medio.
- **"Prefiero bici juguetona y reactiva"** → formas lineal→progresiva (apoyo firme y pop), trayectorias convencionales, LR medio contenido.
- **"Quiero montar muelle"** → exigir progresión útil ≳15–20% y vetarlo con final regresivo; si no llega, proponer aire progresivo como plan B honesto.
- **Peso alto (>90 kg equipado)** → la progresión gana importancia (más energía que absorber) y los cuadros de LR medio alto exigen presiones/muelles fuertes; avisar si el conjunto se va a ir de rango.
- **Peso ligero (<65 kg)** → cuadros muy progresivos pueden no entregar todo el recorrido; sugerir cámara grande o menos spacers.
- **"Es mi e-bike"** → aplicar los matices e-bike de las secciones 5 y 6.
- **"Pedaleo mucho / rutas largas"** → anti-squat en sag 100–120% y curva plana como ideal; kickback como peaje a vigilar en técnico.

Arquetipos rápidos si el usuario no concreta: *saltador/agresivo* → progresión alta; *pedaleador/rutero* → eficacia y curvas planas; *polivalente* → progresión media y formas continuas.

---

## 10. Estructura de la valoración generada

1. **Carácter en una frase.** "Enduro de carácter progresivo y pedaleo firme, pensada para ir rápido en roto."
2. **Los datos que lo sostienen.** 2–4 métricas con su cifra y su lectura, de mayor a menor relevancia.
3. **Compromisos y avisos.** Qué se paga a cambio; alertas de forma (tramos regresivos, sag en zona engañosa) y la advertencia de datos si el motor la marcó.
4. **Amortiguador y reglaje.** Tipo de resorte que casa (muelle / aire y volumen), y uno o dos consejos de ajuste genéricos.
5. **Para quién es.** Perfil de piloto/uso al que le encaja y a quién no, personalizado si hay cuestionario.
6. **Cierre honesto.** Recordatorio breve de que el análisis parte de puntos marcados sobre foto y de que el reglaje final se afina rodando.

### Ejemplo completo (números ficticios)

> **Una enduro progresiva y con nervio, más de bajar fuerte que de contemplar el paisaje.**
> Su curva de palanca va de 2.9 a 2.35 con una progresión útil del 21%, de pendiente continua: apoyo creciente, reserva de sobra para recepciones y libertad total para elegir entre aire y muelle. El pedal kickback, calculado en 32×51, ronda los 38°: en el tramo alto, y como su trayectoria de eje es convencional (retroceso máximo de 2 mm), aquí es peaje sin contrapartida: en subidas rotas notarás tirones en los pedales y en pedreras frenadas puede castigar los pies.
> Si montas muelle, esta curva lo admite sin dramas. Con aire, empieza sin spacers y añade solo si buscas más tope de seguridad.
> Le encajará a quien pese sus buenos 80 kg y baje con intención; un piloto muy ligero puede quedarse sin usar el último tramo de recorrido.
> Recuerda que estos números salen de los puntos marcados sobre la foto: tómalos como una radiografía fiable de tendencias, y remata el ajuste fino sobre el terreno.

---

## 11. Glosario mínimo (ES ↔ EN)

Relación de palanca = *leverage ratio (LR)* · Progresión = *progression* · Regresivo = *falling-rate inverso / digressive* · Hacer tope = *bottom-out* · Hundimiento estático = *sag* · Retroceso de pedal = *pedal kickback (PK)* · Eficacia de pedaleo ≈ *anti-squat (AS)* · Comportamiento al frenar ≈ *anti-rise / brake squat (AR)* · Trayectoria del eje = *axle path* · Vainas = *chainstays* · Cámara de aire grande/reducida = *high/low volume* · Reductor de volumen = *volume spacer/token* · Desacoplador de núcleo = *drivetrain decoupler*.

---

## 12. Nota de mantenimiento

Las bandas numéricas de este documento son **heurísticas de partida**, sintetizadas de la práctica habitual del sector, y deben validarse y ajustarse con el criterio del autor del proyecto a medida que el motor analice bicis reales. Cuando una banda cambie, cambiarla aquí: este documento es la única fuente de verdad de la capa de interpretación.
