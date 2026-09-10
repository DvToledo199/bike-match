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
- Las referencias Orange existentes vienen de fotos marcadas a mano y gráficas;
  no contienen todas las condiciones de anti-squat/anti-rise. No ajustar h para
  forzar coincidencia ni declarar validación externa estricta con ellas.
- Antes de atribuir precisión comparable a Linkage hace falta un fixture/export
  con coordenadas, radios, marcha, CG y convención de cuadro/suelo conocidos.
- El ruido del marcado y la simplificación física son errores distintos: una foto
  ruidosa NO demuestra que se puedan ignorar componentes del modelo.

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
