# Panel de administración — #243

Ruta: `#/admin`. Solo se ofrece a una sesión con rol `ADMIN`.

## Reparto del código

- `AdminPage`: guarda cuentas, cuenta seleccionada, carga, envío, errores y
  confirmación. Carga la lista con `useEffect` y envía los cambios desde el
  manejador del formulario.
- `UsersTable`: muestra username, rol y fecha de alta. El botón de cada fila
  comunica la cuenta seleccionada a la pantalla; la tabla no hace llamadas HTTP.
- `ChangeRoleForm`: guarda el rol elegido mientras se edita, presenta el cambio
  anterior → nuevo y pide confirmación. Elegir una opción aún no envía nada.
- `services/admin.js`: llama a los dos endpoints existentes mediante `requestApi`,
  que añade el token y trata las respuestas HTTP.

## Flujo

Al entrar, `GET /api/admin/users` devuelve una lista sin paginación. No se piden
correos, contraseñas ni datos personales adicionales.

Seleccionar una cuenta abre el formulario. Confirmar envía
`PUT /api/admin/users/{id}/role` con `{ "role": "MODERATOR" }` o `USER`.
La respuesta correcta es `204`, sin JSON. Solo entonces React actualiza el rol
de esa fila y muestra el mensaje de éxito. Mientras espera, bloquea otro envío.

El alcance de la issue es conceder o retirar el rol de moderador, no nombrar
administradores: el formulario ofrece `USER` y `MODERATOR`. Las cuentas `ADMIN`
y la propia cuenta se muestran en solo lectura. La comprobación local de la
propia cuenta usa el username que ya contiene la sesión; la comprobación definitiva
del backend sigue usando el id autenticado.

## Permisos, errores y limitación actual

El menú no es una barrera de seguridad: Spring Security protege `/api/admin/**`.
Forzar la ruta sin ser administrador no muestra cuentas. Si una petición devuelve
401 o 403, también se retiran los datos y acciones de la pantalla.

- `400`: el servidor no acepta el cambio; conservar la selección para corregirla.
- `404`: la cuenta ya no existe; actualizar la lista.
- `409`: no puedes cambiar tu propio rol, aunque la comprobación local no lo
  hubiera detectado. No confundirlo con el 409 de una bici ya revisada en moderación.
- Red, tiempo de espera o servidor: no dar el cambio por confirmado; actualizar
  antes de intentarlo de nuevo porque el servidor podría haberlo aplicado.

**La web es hoy más estricta que el servidor.** La tabla oculta el botón en
cualquier fila con rol `ADMIN`, pero el backend solo rechaza el cambio sobre la
propia cuenta autenticada (`409`). Una llamada directa a
`PUT /api/admin/users/{id}/role` sobre otro administrador sería aceptada. No es
una escalada de privilegios, porque la ruta sigue exigiendo rol `ADMIN`, pero la
regla de no degradar a otro administrador vive únicamente en el frontend.
Trasladarla al backend necesitaría su propia tarea.

**Un cambio de rol no revoca los JWT ya emitidos.** El backend actual lee el rol
del token: los nuevos inicios de sesión obtienen el rol actualizado y las sesiones
anteriores conservan sus permisos hasta caducar. El formulario y el mensaje de
éxito lo explican. La invalidación inmediata necesitaría su propia tarea de backend,
fuera de esta issue; no se ha cambiado ningún contrato ni código de backend.

Tests: carga, vacío, reintento, permisos, cambio en ambas direcciones, confirmación,
actualización tras 204, bloqueo de duplicados y mensajes por código HTTP.
