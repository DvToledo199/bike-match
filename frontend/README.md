# Frontend de BikeMatch

React 19 + Vite, en JavaScript. Arranque, variables y despliegue en el
[README raíz](../README.md). Se construyó con apoyo de IA, como permite el curso, y está
pensado para poder explicarse: se prioriza la claridad sobre la rapidez y cada pieza tiene
un motivo que se pueda defender.

## Stack

| Pieza | Elegido | Motivo |
|---|---|---|
| Framework / build | React + Vite | Estándar, rápido y con buen soporte |
| Idiomas | react-i18next | Inglés base y español; un test obliga a que los dos tengan las mismas claves |
| Gráficas | Recharts | Pinta arrays de puntos sin configuración de más |
| Marcado sobre la foto | SVG | Zoom con `viewBox` y coordenadas de la imagen original, sin dependencias |
| Estilos | CSS Modules + design tokens (`styles/tokens.css`) | Nativo; modo claro y oscuro desde el principio |
| Componentes | HTML nativo | Accesible sin librerías de interfaz |
| Pruebas | Vitest + Testing Library + jsdom | Solo en desarrollo y CI |

## Estructura

- `App.jsx` decide qué pantalla se ve según la ruta (`hooks/useAppRoute.js`) y guarda la
  sesión. `components/Layout.jsx` pinta la cabecera, el menú según el rol y el selector
  de idioma.
- `features/`: una carpeta por funcionalidad, con sus componentes, estilos y tests.
  - `analysis-wizard/`: el asistente (foto → marcado → datos → resultados) y las gráficas.
  - `auth/`: registro y login.
  - `home/` y `catalog/`: portada y catálogo, que comparten `PublicBikeGallery`.
  - `my-bikes/`: Mis bicis, la ficha de una bici y su explicación.
  - `moderation/` y `admin/`: paneles de moderación y de administración (ver
    [`panel-moderacion.md`](../docs/panel-moderacion.md) y
    [`panel-administracion.md`](../docs/panel-administracion.md)).
- `services/`: todas las llamadas a la API. `apiClient.js` añade el token, espera hasta
  3 minutos (la API gratuita puede estar despertando) y convierte cada fallo en un
  `ApiError` con su tipo (`network`, `timeout`, `invalidRequest`, `server`…). Las
  respuestas del motor se validan en `previewValidation.js` antes de dibujarlas.
- `models/`: datos fijos compartidos, como los tipos de bici y su desarrollo de referencia.
- `locales/en` y `locales/es`: todos los textos de la interfaz. Nunca hay texto fijo en el JSX.

## Rutas y estado

Rutas con fragmento, sin librería de enrutado: `#/`, `#/analyze`, `#/catalog`, `#/login`,
`#/register`, `#/my-bikes`, `#/bikes/:id`, `#/moderation` y `#/admin`. Atrás y Adelante
funcionan con el historial del navegador.

- **Sesión:** el token y el usuario viven en `sessionStorage` (`services/session.js`) y se
  pierden al cerrar la pestaña. El idioma elegido se recuerda en `localStorage`.
- **Asistente:** `useWizardState` guarda foto, puntos y medidas en memoria (como un objeto
  Java con sus campos), y `usePreview` envía el cálculo y controla carga, error y
  cancelación. El asistente sigue montado al navegar, así que ir al catálogo o iniciar
  sesión no pierde el análisis; recargar la página sí.
- **Ocultar un menú no es seguridad:** la protección real está en Spring Security. Cada
  pantalla maneja también el `401`/`403` del servidor.

## Marcado de puntos

- Primero se elige el sistema de suspensión; la guía pide sus puntos en orden (6, 9 o 10),
  con una descripción de cada uno.
- Un clic marca el punto. La cruz queda visible y **Deshacer punto** la quita al momento.
- **Zoom** hasta el 1200 % con la rueda del ratón o pellizcando en el trackpad, siempre
  alrededor del cursor. La foto se mueve arrastrando con el botón derecho o con los
  botones. Mover con dos dedos en el trackpad queda para #300.
- **Teclado:** Tab hasta la foto, flechas para colocar el cursor (Shift va más rápido) y
  Enter para marcar. Elegir un punto de la lista permite reajustarlo.
- Los puntos se guardan en píxeles de la imagen original, no de la pantalla.

## Guardar desde los resultados

Sin sesión, los resultados ofrecen registrarse o iniciar sesión sin perder el análisis. Con
sesión, `saveAnalysis.js` guarda la bici en cuatro pasos: crear la bici privada, subir la
foto, finalizar el análisis con los puntos y pedir la explicación. Cada paso confirmado se
recuerda en memoria para poder reintentar el siguiente sobre la misma bici, sin duplicarla.
Un fallo de la explicación no deshace el guardado. Al terminar se abre la ficha. Cuando se
sale de la bici guardada, o si cambia la cuenta o se cierra sesión, el asistente empieza un
análisis nuevo.

## Decisiones de diseño

- **Moderno y minimalista**, con verde contenido, buen contraste y modo oscuro.
- **Accesibilidad:** HTML semántico, `label` en cada campo, foco visible, alternativa por
  teclado en el marcado y equivalente en texto de cada gráfica (inicio y final).
- **Gráficas:** un color por curva. La trayectoria del eje usa la misma escala en los dos
  ejes, porque es un recorrido físico; el eje vertical de la palanca se ajusta a cada bici.
- **Formulario:** se pregunta el tipo de bici (Enduro, E-Enduro o Descenso) en lugar de los
  dientes de la transmisión, con el desarrollo en pequeño en cada opción. Se distingue la
  carrera del amortiguador del recorrido de la rueda.

## Comandos y pruebas

```bash
npm ci
npm run dev     # http://localhost:5173
npm test
npm run lint
npm run build
```

Abre la web en `localhost:5173`, no en `127.0.0.1:5173`: el backend solo acepta ese origen
en desarrollo (`CORS_ALLOWED_ORIGINS`).

Las pruebas cubren los pasos del asistente, el marcado (ratón, pellizco y teclado), los
errores de la API, las respuestas antiguas o incompletas, el guardado y sus reintentos,
los permisos de cada pantalla y que los dos idiomas tengan las mismas claves.

Las gráficas se cargan con la aplicación para evitar que un import diferido fallido atrape
el reintento. El JavaScript pesa unos 190 kB comprimido; Vite avisa de un bloque de más de
500 kB sin comprimir, que es un aviso de rendimiento conocido, no un fallo.
