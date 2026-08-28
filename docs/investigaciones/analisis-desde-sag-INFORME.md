# Informe — análisis de la curva de leverage desde el sag

> Responde al encargo [`analisis-desde-sag.md`](analisis-desde-sag.md).
> Investigación **documental + web**; **no se ha tocado código ni ningún otro archivo**:
> lo único que se crea es este informe. Los ajustes a la base de conocimiento se
> **proponen** aquí (§9), no se aplican. Las fuentes están al final y se han **verificado en
> origen** —incluida Vorsprung, leída directamente en el navegador tras el bloqueo inicial
> del descargador automático de texto.

---

## 1. La pregunta

La base de conocimiento (`base-conocimiento-cinematica.md`, §2–3) manda **leer la curva de
leverage partida en el punto de sag (30 %)** y usar la **progresión útil** (tramo
sag→100 %) como *cifra estrella*, por delante de la progresión total (0→100 %). La duda:
¿es eso siempre correcto? La suspensión también trabaja desde el recorrido 0 (aterrizar un
salto con la bici extendida, descargar y volver a comprimir). Cuatro preguntas concretas:

1. ¿Por qué el sector lee la curva partida en el sag, y por qué 30 %?
2. ¿La progresión se mide desde 0 o desde el sag? ¿Cuál debe ser la cifra principal?
3. ¿En qué situaciones trabaja de verdad cada tramo (0→sag vs sag→100 %)?
4. ¿El 30 % debería variar por disciplina o dejarse fijo?

## 2. Respuesta corta

- **Leer desde el sag está bien fundamentado** y es práctica real del sector, pero la BC
  lo lleva un paso demasiado lejos al declarar la progresión útil *la* cifra y relegar la
  total. **La respuesta correcta es "ambas, para preguntas distintas".**
- **Progresión útil (sag→100 %)** = titular para **apoyo y resistencia al tope en uso
  normal desde el ride height** (la mayor parte del tiempo la suspensión trabaja alrededor
  y por debajo del sag).
- **Progresión total (0→100 %)** = la cifra que publican fabricantes y software, y la
  **lente correcta para las recepciones desde el aire** (justo el caso que plantea la
  duda): en el aire la suspensión se extiende del todo y al aterrizar se recorre **todo**
  el viaje.
- Lo más importante (y en lo que coincide todo el sector): **la forma de la curva pesa más
  que cualquier cifra**. Hay que reportar las dos progresiones **y decir dónde se concentra
  la progresión respecto al sag**, citando siempre el sag asumido.
- El motor **ya calcula ambas** (BC §3), así que esto es un ajuste de **interpretación y
  presentación**, no de código.

---

## 3. Fundamento físico: por qué el sag es el punto de lectura

**El sag es la altura de trabajo (ride height).** Es la posición en la que la suspensión
se sienta con el piloto encima, quieto, en llano. No es un extremo de la curva: es el
**punto de equilibrio** desde el que la bici sale a rodar.

**El recorrido "cuelga" del sag en dos sentidos.** Una analogía útil: es como estar de pie
con las rodillas algo flexionadas.

- Desde ahí puedes **flexionar más** para encajar un golpe o una caída → es la compresión,
  el tramo **sag→100 %** (~70 % del recorrido con sag al 30 %).
- Y puedes **estirar la pierna** para mantener el pie en el suelo cuando el terreno se
  hunde bajo de ti → es la extensión, el tramo **0→sag** (~30 % del recorrido), la
  *reserva de extensión* o *droop*.

**Por qué la resistencia al tope se lee desde el sag y no desde 0.** Durante un impacto
normal partes del ride height (sag) y comprimes **hacia abajo**. El tramo 0→sag ya lo has
"gastado" estáticamente con tu peso; no lo tienes disponible para tragar ese bache. Por
tanto, la rampa de dureza que te separa del tope es la de **sag→abajo**, no la del recorrido
completo. De ahí que dos bicis con la misma progresión *total* se comporten al revés según
a qué altura de su curva caiga el sag (BC §2, correcto). Es exactamente la razón por la que
el análisis de reglaje mide desde el sag.

**Por qué 30 %.** Es el valor central del sector para gravedad. Equilibra tres cosas a la
vez: (a) **geometría** — mantiene ángulo de dirección y altura de pedalier en la ventana de
diseño; (b) **reserva de extensión** — ~30 % de droop para seguir el suelo y dar tracción;
(c) **reserva de compresión** — ~70 % para absorber y no hacer tope. Además es donde los
fabricantes afinan la curva del cuadro y el tune del amortiguador. Las fuentes de reglaje
coinciden: **más sag = más recorrido "colgando" por debajo = más progresión disponible para
los grandes impactos**, a cambio de un ángulo algo más tumbado y dirección más lenta.

---

## 4. Qué hace de verdad el sector (con fuentes)

**Conviven dos convenciones para "progresión", y no son la misma cifra:**

| Convención | Quién la usa | Qué compara |
|---|---|---|
| **Total / recorrido completo** | Fabricantes, *Linkage*, Norco, Vorsprung | LR en (casi) extensión total vs LR en (casi) compresión total |
| **Desde el sag** | Análisis de reglaje (p. ej. BikeRadar) | LR en el sag vs LR a tope |

BikeRadar lo hace explícito con números: *"comparando el leverage ratio en el sag con el
de bottom-out se calcula la progresividad como porcentaje"*, y da el ejemplo
`(2.62 − 2.33) / 2.62 = 11 %` medido **del sag (25 %) al 95 %** del recorrido (nótese que
recorta el último 5 %, donde la curva es menos fiable). Norco y el manual de *Linkage*, en
cambio, definen la progresión como el cambio de LR **entre extensión y compresión total**.
Es decir: **el sector no tiene una sola cifra "oficial"**; la BC ha elegido una de las dos
familias (la de sag) y ha hecho bien, pero conviene saber que la otra es la que verá el
usuario en la ficha del fabricante.

**La enseñanza más repetida: la FORMA importa más que la cifra.** Vorsprung —una de las
referencias técnicas del sector— distingue tres formas de curva de palanca (lineal,
progresiva y *falling rate*) y resume el efecto de una progresiva con esta frase literal:
*"Progressive leverage rates give a feel that is relatively soft in the initial travel and
relatively stiff later in the travel"* (blando al inicio, firme al final). Añade que **cómo
termina la curva es lo que decide si conviene muelle o aire**, y que una *falling rate* (LR
que sube al comprimir) solo se usa en bicis de XC, porque dificulta a la vez la absorción
del pequeño impacto y la resistencia al tope.

Ese reparto por tramos (inicio blando / medio con apoyo / final progresivo) es, en rigor,
una propiedad del **resorte**, no de la palanca: en otro artículo, Vorsprung describe la
curva del **resorte de aire** en tres tramos —inicio más rígido por la fricción de los
sellos (tacto "harsh"), zona media que se hunde ("lack of support") y final progresivo que
protege del tope y se afina con *tokens*—. Cuadro y resorte se multiplican: comportamiento
final = curva de la palanca × curva del resorte (BC §3).

BikeRadar llama a esto la **"curva colgante" (hanging)**: mucho LR arriba para suavizar el
inicio y una caída rápida a LR bajo para dar apoyo **justo después del sag**. Es idéntico a
la forma "cae fuerte al inicio y se aplana" de la BC §3 — y es el caso donde leer desde el
sag es más necesario, porque **gran parte de la caída de LR ocurre antes del sag**: la total
parece alta pero la útil es baja.

**Sag por disciplina (varias guías de reglaje):**

| Disciplina | Sag trasero habitual |
|---|---|
| XC / Downcountry | 25–28 % |
| Trail | 28–30 % |
| Enduro | 30–33 % |
| DH | 33–38 % |

---

## 5. ¿Desde 0 o desde el sag? La duda central, razonada

**Respuesta: ambas, porque responden a preguntas físicas distintas.**

**La progresión útil (sag→100 %) es la cifra correcta para el uso "de suelo".** La inmensa
mayoría del tiempo la rueda va cargada y trabaja alrededor y por debajo del ride height:
absorber baches, apoyar en curva, aguantar un g-out, no hacer tope. Para todo eso, el tramo
que se recorre es sag→abajo, y por eso el análisis de reglaje mide ahí. La BC acierta al
destacarla.

**Pero la progresión total (0→100 %) no sobra — y es precisamente la lente del caso que
plantea la duda.** Al saltar, en el aire **no hay carga sobre la rueda**, así que la
suspensión **se extiende hasta el tope superior (topout, 0 %)**. Al aterrizar, la rueda
comprime **desde 0**, recorriendo **todo** el viaje: el inicio de la curva aporta la
"suavidad de contacto" (touchdown) y luego la rampa profunda recoge el aterrizaje. En ese
evento el tramo 0→sag **sí hace trabajo real**, no es un mero offset estático. La total es
además la cifra que el usuario encontrará en la ficha del fabricante, así que hace falta
para **cotejar**.

**Matiz fino que evita el error contrario:** aunque el aterrizaje empiece en topout, lo que
te salva del **tope duro** sigue siendo la **rampa profunda (sag→100 %)** — el tramo alto se
consume enseguida. El 0→sag manda en el **tacto inicial** del aterrizaje y en **cuánto
recorrido total** hay, no tanto en el tope. Es decir: los saltos **justifican reportar
también la total**, pero **no derriban** la lógica de leer la resistencia al tope desde el
sag. Las dos verdades conviven, y por eso la solución no es elegir una cifra sino **mostrar
las dos y la forma**.

**Conclusión operativa:** nunca presentar la progresión útil **sola**, como si el 0→sag no
existiera. Titular con la útil, acompañar siempre de la total, y describir **dónde se
concentra la progresión respecto al sag**.

---

## 6. En qué situaciones trabaja de verdad cada tramo

| Tramo | Nombre | Cuándo se recorre de verdad | Qué gobierna |
|---|---|---|---|
| **0 → sag** (~30 %) | Reserva de extensión / *droop* / inicio de curva | Rueda descargada; **agujeros y contrapendientes** (seguir el suelo); **rebote** tras un impacto; **"touchdown"** al aterrizar; pequeño impacto y tracción en llano/subida | Sensibilidad, tracción, **contacto con el suelo**, tacto del primer instante |
| **sag → 100 %** (~70 %) | Reserva de compresión / trabajo + final | Absorción de la mayoría de impactos **desde el ride height**; apoyo medio (pump, curvas, g-outs); grandes golpes y **recepciones** | Apoyo, **progresión sentida**, **resistencia al tope** |

**Sobre "la bici descargada" (el corazón de la duda):** el 0→sag no es solo el hueco que
ocupa tu peso en reposo. Se recorre **continuamente** al descargar, bombear, en el rebote y
al caer en agujeros. Ese es el argumento legítimo para **no despreciarlo**. Lo que sí es
cierto es que, con la rueda cargada —que es la mayor parte del tiempo—, el trabajo pasa
**alrededor y por debajo** del sag; por eso el titular de apoyo/tope se lee desde ahí.

---

## 7. ¿30 % fijo o por disciplina?

La v1 soporta **Enduro, e-Enduro y DH**, y las tres viven cerca del 30 % (enduro 30–33 %,
DH 33–38 %). Por tanto **un 30 % fijo es defendible y, además, comparable entre bicis**. El
único sesgo: es **algo conservador para DH** — a 33–35 % de sag real el corte cae más
profundo, con lo que la progresión útil sale un poco **más baja** y la reserva de extensión,
mayor.

**Recomendación:** mantener **30 % por defecto en v1**; seguir teniéndolo como **parámetro**
(ya lo es); al ampliar a **XC/Trail/Downcountry**, bajar el defecto a **20–25 %**; y valorar
**~33 % para DH** si se quiere afinar. Y lo esencial, transversal a todo: como la cifra
estrella **se mueve con el sag asumido** —que es una **elección de reglaje**, no un dato que
esté en la foto— hay que **citarlo siempre** e, idealmente, **mostrar su sensibilidad**
(p. ej. la progresión útil al 25 % y al 30 %, o la curva con el corte marcado). Esto encaja
con la regla de honestidad de la BC (§1.2, §1.8) y con el espíritu del informe hermano de
sensibilidad.

---

## 8. Recomendación para BikeMatch v1

1. **Métricas: nada que tocar en el motor.** Ya entrega LR inicial/sag/final, progresión
   **total** y **útil**, pendiente por tramos y puntos de inflexión (BC §3). El cambio vive
   en la **capa de interpretación y presentación**.
2. **Titular con la útil, pero mostrar siempre la total** y **describir la forma** (dónde se
   concentra la progresión respecto al sag). Para el perfil "saltos grandes / topes", traer
   explícitamente la **total + la rampa profunda**.
3. **Transparencia del sag:** citar el sag usado en toda valoración; presentar la útil como
   **dependiente de esa suposición**; mantener el sag configurable.
4. **Fijar y documentar la convención del denominador** (ver §9): dos convenciones legítimas
   dan cifras distintas para la *misma* bici. Recomendación: alinear con la de "desde el sag"
   (÷ LR_sag, la de BikeRadar) para ser **directamente comparable** con la prensa de reglaje;
   si se cambia, **recalibrar las bandas** de §3.
5. **30 % fijo en v1**; revisar por disciplina al crecer el alcance (§7).
6. *(Opcional)* medir la útil hasta **~95 %** y no hasta el 100 %, para evitar el ruido del
   último tramo de la curva (es lo que hace BikeRadar).

---

## 9. Ajustes propuestos a la base de conocimiento (§2–3)

> Propuestas; **no aplicadas**. La BC sigue siendo la fuente de verdad y su autor decide.

**9.1. Matizar "la cifra estrella es la progresión útil, no la total" (§3).** Sugerido:
> La progresión **útil** (sag→100 %) es el **titular para el apoyo y la resistencia al tope
> en uso normal desde el ride height**. La progresión **total** (0→100 %) no se descarta: es
> la lente correcta para **recepciones de salto** (la suspensión se recorre entera desde el
> topout) y es la cifra que publican **fabricantes y software**, necesaria para cotejar.
> Reportar **ambas** y, sobre todo, **dónde se concentra la progresión respecto al sag**.

Esto además resuelve una **tensión interna** de la propia BC: sus "frases tipo" de §3 ya
usan la **total** para avisar (*"la cifra global (18 %) engaña: casi toda la progresión
ocurre antes del sag…"*), en contradicción con la frase que la relega.

**9.2. Documentar la convención de la fórmula (§3).** La BC define
`progresión útil = (LR_sag / LR_final − 1) × 100`, que **divide por LR_final**. Las
convenciones publicadas dividen por otro valor, y eso cambia la cifra —y a veces la
etiqueta— para la misma bici:

| Convención | Fórmula | Divisor | Resultado* | Etiqueta §3 |
|---|---|---|---|---|
| **Útil, como en la BC** | `(LR_sag / LR_final − 1) × 100` | LR_final | **21,7 %** | **Alta** (20–30) |
| **Útil "desde el sag"** (BikeRadar) | `(LR_sag − LR_final) / LR_sag × 100` | LR_sag | **17,9 %** | **Progresión media** (12–20) |
| **Total** (Norco / *Linkage* / Vorsprung) | `(LR_ini − LR_final) / LR_ini × 100` | LR_ini | 22,0 % | *(otra escala)* |

*\*Ejemplo ilustrativo con `LR_ini = 2,95`, `LR_sag = 2,80`, `LR_final = 2,30`.*

La convención de la BC (÷ LR_final) da **sistemáticamente la cifra más alta** de las tres, y
en el ejemplo **cruza la frontera de banda** (21,7 % "Alta" vs 17,9 % "Progresión media")
respecto a la convención de BikeRadar. Recomendación: **elegir una y documentarla**; si se
adopta ÷ LR_sag para comparar con la prensa, **recalibrar las bandas** (fueron calibradas
sobre la convención actual).

**9.3. §2 — el sag es una elección de reglaje, no una propiedad del cuadro.** Añadir que el
punto de corte —y por tanto el titular— **se mueve con una suposición que no está en la
foto**; hay que citarlo y, si se puede, mostrar su sensibilidad (útil a 25 % vs 30 %).

**9.4. §2 — rango de DH.** El sag trasero real de DH suele ser **33–38 %**, no ~30 %. O se
amplía el rango, o se anota que BikeMatch **estandariza 30 % por comparabilidad** (asumiendo
el sesgo del §7).

**9.5. §2–3 — enriquecer el papel del tramo 0→sag.** Hoy se describe como
"sensibilidad / tracción / pequeño impacto". Añadir que es también la **reserva de
extensión** que mantiene la rueda en el suelo cuando el terreno se hunde, la **zona de
rebote**, y el **"touchdown"** del aterrizaje. Esto responde de frente a la duda de que "la
suspensión también trabaja desde 0".

---

## En una frase

**Leer desde el sag es correcto para el titular de apoyo y resistencia al tope —que es lo
que se juega la mayor parte del tiempo—, pero la progresión total no sobra: es la lente del
aterrizaje desde el aire y la cifra que publican fabricantes y software; para la v1 la
recomendación es mostrar las dos y, sobre todo, la forma de la curva respecto al sag,
citando siempre el sag asumido (y fijando y documentando qué fórmula de progresión se usa).**

---

## Fuentes

- BikeRadar — *The ultimate guide to mountain bike rear suspension systems* (progresión
  medida desde el sag; "hanging curve"; sensibilidad vs resistencia al tope):
  <https://www.bikeradar.com/advice/buyers-guides/the-ultimate-guide-to-mountain-bike-rear-suspension-systems>
- Vorsprung Suspension — *Understanding Leverage Curves* (tres formas de curva; progresiva =
  blando al inicio y firme al final; el final de la curva decide muelle vs aire) y *The
  Limitations of Air Springs* (curva del resorte de aire en tres tramos: inicio rígido, medio
  hundido, final progresivo con tokens):
  <https://vorsprungsuspension.com/blog/learn-4/understanding-leverage-curves-1> ·
  <https://vorsprungsuspension.com/blog/learn-4/the-limitations-of-air-springs-4>
- The Radavist / Travis Engel — *Shock Value: Leverage Curves, Anti-Squat, and Anti-Rise
  Simplified*: <https://theradavist.com/leverage-curves-anti-squat-anti-rise>
- Norco — *Terms and Definitions* (progresión = cambio de LR de extensión a compresión):
  <https://www.norco.com/design-technology/terms-and-definitions/>
- *Linkage* — manual de términos (definición de progresión sobre recorrido completo):
  <https://www.bikechecker.com/linkagedoc/terms.htm>
- PMB Suspension — *Leverage Ratio & Progression Calculator* (clasificación
  progresiva/lineal/regresiva): <https://www.pmbswiss.ch/pages/leverage-ratio-progression-calculator>
- Pinkbike — *The Tuesday Tune Ep 12: Leverage Rates*:
  <https://www.pinkbike.com/news/the-tuesday-tune-ep-12-leverage-rates.html>
- ENDURO Mountainbike Magazine — *Setup Guide* (sag por disciplina):
  <https://enduro-mtb.com/en/setup-guide-mtb-suspension/>
- Worldwide Cyclery — *MTB Suspension Setup: How To Set Sag* (sag por disciplina):
  <https://worldwidecyclery.com/blogs/worldwide-cyclery-blog/mountain-bike-suspension-setup-how-to-set-sag-compression-rebound-ep-1-video>
