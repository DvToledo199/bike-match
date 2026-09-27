# Horst con el amortiguador empujado por los tirantes

Issue #288. Cuarto tipo de suspensión del motor: `HORST_LINK_SEATSTAY`.

## Qué cambia respecto al Horst con bieleta

La cuatro barras es la misma: pivote principal → pivote Horst → unión de los tirantes
con la bieleta → pivote de la bieleta en el cuadro. Lo que cambia es quién
comprime el amortiguador:

- **Horst con bieleta** (`HORST_LINK`): el amortiguador va del cuadro a la bieleta.
- **Horst por los tirantes** (`HORST_LINK_SEATSTAY`): el amortiguador va del cuadro a
  un punto de los tirantes, casi siempre una prolongación suya más allá de la bieleta.
  La bieleta solo guía los tirantes y no toca el amortiguador.

Si se calcula una bici de este segundo tipo como si fuera del primero, la bieleta se
queda corta: con una bici real de 160 mm y un amortiguador de 230 × 65 mm, el modelo
con bieleta solo admitía 25 mm de compresión. Con el modelo por los tirantes, los 65 mm
dan 162,9 mm de recorrido, un 1,8 % más que el declarado.

## Puntos

Nueve puntos, los mismos que en el Horst con bieleta salvo el ojo móvil del
amortiguador:

- `SHOCK_FRAME`: ojo del amortiguador fijado al cuadro.
- `SHOCK_SEATSTAY`: ojo del amortiguador unido a los tirantes.

La calibración sigue siendo `eyeToEyeMm / distancia(SHOCK_FRAME, SHOCK_SEATSTAY)`.

## Cálculo

En el Horst con bieleta, la longitud del amortiguador sitúa la bieleta con una sola
intersección de circunferencias. Aquí no se puede, porque el ojo móvil está en los
tirantes y su posición depende de toda la cuatro barras. El motor usa como entrada el giro
de la bieleta:

1. Para un giro dado, la cuatro barras se resuelve igual que en el Horst con bieleta
   (una intersección de circunferencias) y los tirantes llevan consigo el eje trasero y
   el ojo del amortiguador como un sólido rígido.
2. En cada uno de los 100 pasos de compresión, el giro avanza en saltos de 0,5° hasta
   que el amortiguador queda más corto que la longitud buscada, y después se afina por
   bisección hasta clavarla.
3. Si la cuatro barras no se puede montar o la bieleta supera los 120° de giro sin
   llegar a la compresión pedida, el cálculo se rechaza con un error 400.

Como cada paso parte del anterior, el mecanismo no puede saltar a la otra forma de
montarse. Las curvas (palanca, eje, kickback, anti-squat y anti-rise) se calculan igual
que en el Horst con bieleta, con el freno en los tirantes.

## Comprobación

Cuando el ojo del amortiguador coincide con la unión de los tirantes y la bieleta, ese
punto pertenece a las dos piezas y los dos modelos describen el mismo mecanismo. Un test
comprueba que entonces las dos soluciones coinciden paso a paso.
