# Curvas ampliadas: modelo de referencia y mejoras futuras

Decisión aprobada por David el 10 de septiembre de 2026. Se adelantan las curvas
anti-squat/anti-rise y el kickback cog-aware antes de persistencia e IA. Issues
#108 (dominio), #31 (kickback) y #109 (API/web). No se modifica auth #94.

## Experiencia del MVP

- Selector: Full 29, Mullet (29 delante/27,5 detrás), Full 27,5.
- No pedir peso, altura corporal, centro de gravedad ni radios escritos a mano.
- Condiciones de referencia visibles como aviso breve y conservadas en la API.
- La versión gratuita usa el mismo método físico; personalización y posibles
  modalidades de pago quedan por decidir, no implementadas.
- No se añaden tablas masivas ni marcado repetido de puntos.

## Valores elegidos y significado

| Configuración | Radio delantero | Radio trasero |
|---|---:|---:|
| FULL_29 | 371 mm | 371 mm |
| MULLET | 371 mm | 352 mm |
| FULL_27_5 | 352 mm | 352 mm |

Son radios **exteriores estimados**, no radios de llanta ni medidas reales del
neumático. Convención: asiento de talón 622/584 mm + dos alturas nominales de
neumático de 60 mm. El ancho nominal no determina exactamente la altura real.
Cambiar cubierta/presión/carga altera el radio: futura entrada avanzada medida.

La altura de centro de gravedad usada es **1100 mm** sobre el plano horizontal
de referencia de cada posición. Es una convención explícita para comparar, NO un
promedio humano validado, ni el valor garantizado de Linkage, ni una medición del
usuario. No se inventa un peso: en estos cocientes geométricos se cancela la masa.
Duplicar esa altura divide por dos ambos porcentajes; los tests comprueban esto.

## Alcance físico

Monopivote simple, cadena directa sin roldana, pinza fija al basculante. Ejes
x hacia delante e y hacia abajo, como las coordenadas de imagen existentes.
Cuadro con orientación fija durante el barrido; no simulamos cabeceo dinámico,
movimiento de horquilla, neumático deformable, tracción ni movimiento del ciclista.
En cada muestra la referencia de suelo está un radio por debajo del eje trasero;
la altura de CG se mantiene a 1100 mm sobre esa referencia. La distancia entre
ejes es su separación horizontal en esa muestra. Es un ensayo geométrico de
referencia, no una trayectoria dinámica con ambas ruedas impuestas al mismo suelo.

No confundir estos porcentajes con eficiencia energética o potencia de frenado.
Un valor >100% no es automáticamente mejor; valores negativos no se recortan.

## Anti-squat y anti-rise

Con A=eje trasero, P=pivote, S=P−A, W=distancia horizontal entre ejes,
R=radio exterior trasero y h=altura CG:

1. Anti-rise: prolongar contacto trasero→pivote hasta la vertical delantera.
   `AR = 100 · W/h · (R − Sy)/Sx`.
2. Anti-squat: intersectar eje→pivote con la tangente superior de la cadena;
   unir ese punto al contacto trasero y proyectar a la vertical delantera.
   La implementación equivalente evita crear una intersección en el infinito:
   `AS = 100 · W/h · [R · (n·S)/rCog − Sy]/Sx`.
   `n=(sin(θ),−cos(θ))` es la normal superior de la cadena.

El modelo de cadena usa círculos de paso lisos `r=N·12,7/(2π)` (no dientes
discretos). La tangente tiene longitud `sqrt(d²−(rCog−rRing)²)` y ángulo
`θ=atan2(BBy−Ay, BBx−Ax)+asin((rCog−rRing)/d)`.

No se confunde anti-rise con anti-dive de la horquilla. Un freno flotante o Horst
requiere otro centro instantáneo de reacción; NO reutilizar el pivote principal.

## Validación y limitaciones de la evidencia

### Kickback cog-aware

Se suma, respecto al reposo, en radianes:

`ΔL/rRing + Δθ·(N_cog/N_ring−1) + (Ax_reposo−Ax_actual)/R·N_cog/N_ring`.

Los tres términos son crecimiento del tramo tangente, enrollado/desenrollado al
girar la línea de cadena y giro de rueda asociado al desplazamiento horizontal.
El signo positivo representa retroceso de bielas; no recortar valores negativos
ni exigir que la curva sea monótona para toda geometría. Se incluye la muestra
de reposo (0 mm, 0°). El caso de platos/piñones iguales cancela el segundo término.

Condiciones: transmisión engranada, retroceso de rueda sin deslizamiento asociado
al movimiento del eje; sin velocidad de avance ni dinámica de rueda libre. No
significa que el usuario vaya a sentir siempre ese giro al bajar rodando.

Se conserva el método V1 para clientes que no indican configuración de ruedas;
el API debe distinguir las versiones, nunca rellenar esos datos silenciosamente.

### Evidencia y comprobaciones pendientes

- Tests analíticos: construcción geométrica independiente, caso paralelo,
  efecto de marcha/altura, radios mullet, tangencias y entradas degeneradas.
- Eso valida la implementación del modelo, no mide su precisión en una bici real.
- Las referencias Orange existentes siguen siendo una validación real útil de
  recorrido y leverage. Sus coordenadas son marcados sobre fotos, no una exportación
  de coordenadas del archivo de Linkage. No ajustar h ni mover puntos para forzar
  coincidencia con una curva.
- Antes de atribuir precisión comparable a Linkage hace falta un fixture/export
  con coordenadas, radios, marcha, CG y convención de cuadro/suelo conocidos.
- El ruido del marcado y la simplificación física son errores distintos: una foto
  ruidosa NO demuestra que se puedan ignorar componentes del modelo.

### Comprobación de las dos Orange (10/09/2026)

Se reutilizan `OrangeStage6Fixture` y `OrangeSurgeFixture`, sin cambiar sus puntos
ni las tolerancias. `ReferenceBikeV2Test` prueba el API ampliado: recorrido y ambos
extremos del leverage continúan dentro del **±3%**. Los tests anteriores siguen
ejecutándose sobre V1. No se está sustituyendo una validación real por tests sintéticos.

| Bici / transmisión | Referencia publicada | V1 | V2 de referencia |
|---|---|---|---|
| Stage 6 / 32×50 — recorrido | 150 mm | 153,17 mm | 153,04 mm |
| Stage 6 — leverage inicial/final | 2,775 / 2,675 | 2,833 / 2,732 | 2,835 / 2,725 |
| Stage 6 — kickback | ≈22° | 21,98° | **28,79°** |
| Surge / 34×50 — recorrido | 164 mm | 164,35 mm | 164,36 mm |
| Surge — leverage inicial/final | 2,55 / 2,50 | 2,548 / 2,504 | 2,544 / 2,508 |
| Surge — kickback | ≈34° | 26,53° | **35,86°** |

Los resultados propios se evalúan al final del recorrido calculado. Las cifras
publicadas son aproximaciones de gráficas, no muestras digitales. La diferencia
de kickback de la Stage 6 es importante y **no queda resuelta**: la mejora en la
Surge no demuestra por sí sola que el modelo esté validado. La issue #31 sigue
abierta. El test histórico de kickback solo cubría Surge con ±30%; no existía una
aserción equivalente de Stage 6. No se amplía ninguna tolerancia para ocultarlo.

La [entrada original de Stage 6](https://linkagedesign.blogspot.com/2019/11/orange-stage-6-29-2020.html)
sí publica datos útiles: tabla con `SAG 25% F+R`, `CDG 1065`, B.S. 90% y desarrollo
32/50 en la gráfica de kickback. La curva azul ronda 22° a 150 mm y continúa hacia
160 mm. La tabla es una condición al sag, no necesariamente el primer punto de la
curva. No comparar esos porcentajes directamente con nuestros valores a reposo.
El CG afecta AS/AR, **no explica la diferencia de kickback**. Quedan por contrastar
las coordenadas exactas, radios exteriores y convenciones de posición/horquilla
de ese análisis; no se presume que sean la causa del desacuerdo.

Para cerrar #31: obtener el modelo/export o una referencia reproducible con esas
condiciones, contrastar muestras a iguales recorridos en ambas bicis, separar
errores de entrada de posibles errores del motor y fijar una tolerancia justificada.
No presentar las tres curvas ampliadas como precisión certificada de Linkage.

## API, orientación y comprobación de integración (#109)

- `parameters.wheelConfiguration`: `FULL_29`, `MULLET` o `FULL_27_5`.
  Omitido/null conserva V1; un valor desconocido, vacío o numérico se rechaza.
- `conditions.modelVersion`: `monopivot-v1` o `monopivot-reference-v2`.
  En V1, `reference=null` y curvas anti vacías; no rellenar resultados no calculados.
- `conditions.reference`: ruedas, radios, altura CG, corrección de foto en grados,
  `motionModel=FIXED_FRAME_LOCAL_GROUND`, `brakeModel=SWINGARM_FIXED`,
  `validationLevel=ANALYTICAL_REFERENCE`. Debe persistirse junto con las curvas.
- Se refleja la orientación izquierda/derecha. En V2, tras calibrar, la diferencia
  de altura esperada entre ejes descuenta los radios distintos antes de corregir
  inclinación; rechazo si la corrección supera 15°. Se asume suelo plano, fotografía
  lateral y suspensión extendida. No corrige perspectiva ni detecta una cuesta.
- La web exige elegir ruedas y recibe cinco curvas con condiciones visibles;
  no acepta como V2 una respuesta antigua/incompleta. Números negativos o >100%
  válidos se conservan. No añade peso ni tablas de todos los puntos.
- Pruebas: 112 tests Java y 28 frontend, lint/build; navegador Chromium con llamada
  real al backend, las tres selecciones, cinco curvas, modos claro/oscuro a 1280 px
  y móvil a 390 px sin desbordamiento. La foto sintética de esta comprobación prueba
  la interfaz, no sustituye las dos referencias Orange de la tabla anterior.

## Trazabilidad de esta ampliación

- #108 → [PR #110](https://github.com/DvToledo199/bike-match/pull/110): geometría de cadena,
  condiciones y curvas anti-squat/anti-rise, con tests independientes de Spring.
- #31 → [PR #111](https://github.com/DvToledo199/bike-match/pull/111): kickback cog-aware;
  implementado, pero issue abierta por el contraste externo pendiente.
- #109: integración API/selector/gráficas, normalización mullet y documentos
  sincronizados. El cambio de navegación final se tramita aparte.

## Mejoras futuras, sin implementarlas por adelantado

- Medidas exteriores reales de neumáticos y calibración secundaria (distancia
  entre ejes). Evitar inferir inclinación de ejes sin descontar ruedas mullet (#97).
- CG configurable/perfiles de postura: revisión física y explicación clara antes
  de personalizar; el peso por sí solo no localiza el CG.
- Horst, bieletas, frenos flotantes, roldanas/idlers y extensores (#19/#98): modelo
  explícito y fixture propio; no activarlos por parecido visual.
- Kickback dinámico: velocidad, rueda libre, contacto/deslizamiento y horquilla;
  la curva cuasiestática no equivale a lo que siempre siente el ciclista rodando.
- Simulador de fuerza: muelle/aire genéricos normalizados al sag, luego datos
  medidos. Sigue fuera de este bloque por modelado/validación, no por pedir peso.
- Persistencia/IA (#103): guardar versión, condiciones y capacidades con cada
  resultado. Invalidar/recalcular resúmenes al cambiar el motor; nunca presentar
  una condición de referencia como información personal real del usuario.

## Fuentes de las construcciones (no de los valores predeterminados)

- [Linkage FAQ](https://www.bikechecker.com/faq.php): CG, ruedas, limitaciones del
  cuadro fijo y horquilla; no garantiza coincidencia de convenciones entre programas.
- [Altair, anti-squat](https://help.altair.com/hwdesktop/hwx/topics/motionview/two_wheeler_library_antisquat_antidive_r.htm):
  construcción con cadena, contacto, distancia entre ejes y altura CG.
- [Cannondale Habit, white paper, §2](https://www.wideopenmountainbike.com/images/C19_Habit_WHITE_PAPER.pdf):
  construcción de anti-rise y supuestos; documento del fabricante alojado por un tercero.
- [Linkage, kickback](https://www.bikechecker.com/linkagedoc/PedalKickbackCalculation.pdf):
  longitud tangente, giro de cadena y rotación de rueda, con condiciones cuasiestáticas.
- [Schwalbe, medidas](https://www.schwalbe.com/en/tube-search): nomenclatura de
  neumáticos; las pulgadas comerciales no son un diámetro exterior exacto.
