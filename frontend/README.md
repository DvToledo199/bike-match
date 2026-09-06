# Frontend de BikeMatch

React + Vite, JavaScript, CSS Modules, react-i18next y Recharts. Arranque y pruebas
en el [README raíz](../README.md).

`AnalysisWizard` coordina foto → marcado → parámetros → resultados.
`useWizardState` conserva la ficha en memoria (parecido a un objeto Java con foto,
puntos y medidas). Cada paso cambia su parte. `usePreview` envía la ficha y controla
carga, errores y cancelación; `KinematicsCharts` dibuja la respuesta validada y
`KinematicsDescriptors` explica sus tendencias. No ejecuta una IA.

## Interacción

- Un clic por punto. Su cruz permanece hasta corregirlo o pulsar **Undo point**.
- Zoom hasta 800%; botones de flechas para desplazar la vista.
- Teclado: Tab hasta la foto, flechas para colocar el cursor y Enter para confirmar;
  Shift acelera. Seleccionar un punto permite reajustarlo.
- Nombre de la foto conservado al cancelar el selector y al volver a ese paso.
- Inicio/final de las gráficas en texto, sin tabla desplegable. En axle path,
  izquierda es retroceso y arriba compresión; escala física igual en ambos ejes.
- Recargar o cerrar la pestaña elimina la ficha: todavía no hay persistencia.

## Comandos y pruebas

`npm ci`, `npm run dev`, `npm test`, `npm run lint`, `npm run build`.
Vitest, Testing Library y jsdom son solo para desarrollo/CI. Las pruebas cubren
errores de API, respuestas antiguas, selección de foto, teclado y escala gráfica.
La prueba con foto real y referencia sigue siendo necesaria (#54).

Las gráficas se cargan con la aplicación para evitar que un import diferido fallido
atrape el reintento. JavaScript: unos 190 kB gzip; Vite avisa de un chunk de más de
500 kB sin comprimir. Es un aviso de rendimiento conocido, no un fallo de build.
