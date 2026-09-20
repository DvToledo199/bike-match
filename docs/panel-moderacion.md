# Panel de moderación — #238

Ruta: `#/moderation`. El menú la muestra a `MODERATOR` y `ADMIN`.
No modifica el backend, el asistente de análisis ni la interpretación por IA.

## Cómo explicarlo en clase

1. `useAppRoute.js` lee la ruta y `App.jsx` muestra `ModerationPage`, pasándole
   la sesión actual.
2. `ModerationPage` guarda el estado de la pantalla con `useState`: vista elegida,
   página del catálogo, lista, errores, operación en curso y mensaje de resultado.
   Un `useEffect` carga la lista al entrar, cambiar de vista/página o actualizar.
3. `ModerationBikeCard` presenta cada bici. Sus botones avisan a la pantalla
   mediante funciones recibidas por propiedades; la tarjeta no hace peticiones.
4. `RemoveBikeForm` conserva el motivo mientras se escribe. Comprueba que tenga
   contenido y no supere 500 caracteres. Su confirmación avisa explícitamente
   del borrado permanente y de que el propietario recibirá un aviso.
5. La pantalla llama a `services/moderation.js`, que usa `requestApi`.
   Este cliente común añade el token y convierte los fallos HTTP en errores.
6. Solo después de recibir la respuesta correcta aparece el mensaje de éxito
   y se recarga la lista. Durante el envío se desactivan las acciones para evitar
   duplicados. Ante un fallo no se anuncia un éxito ni se elimina la tarjeta.

## Dos listas con contratos distintos

| Vista | Endpoint | Datos y paginación |
| --- | --- | --- |
| Pendientes | `GET /api/moderation/pending` | Lista completa, sin paginación; incluye propietario y fecha de solicitud. |
| Publicadas | `GET /api/bikes` | Reutiliza `listPublicBikes` y la paginación del catálogo. No incluye propietario y no se consulta otro endpoint para obtenerlo. |

Las pendientes permiten aprobar, rechazar o retirar. Las públicas solo retirar.
Rechazar no borra la bici. Retirar la borra definitivamente junto con su análisis
y foto; el backend crea el aviso para el propietario. Si una retirada deja fuera
de rango la página actual del catálogo, se vuelve a una página que exista.

## Permisos y errores

Ocultar el enlace y comprobar el rol en React solo ayuda a navegar. La protección
real sigue en Spring Security. Aunque la sesión local diga que se tiene permiso,
la pantalla maneja también el rechazo del servidor:

- `400`: datos inválidos; el formulario conserva el motivo para corregirlo.
- `401`: iniciar sesión de nuevo.
- `403`: no hay permiso; se ocultan los datos y acciones del panel.
- `404`: la bici ya no existe o no está disponible para moderación.
- `409`: otra decisión ha cambiado el estado; actualizar antes de actuar otra vez.
- Red, tiempo de espera o servidor: la operación no está confirmada; actualizar
  antes de reintentar, pues el servidor podría haberla completado.

Los textos están en i18n y los estilos en un CSS Module. No hay dependencias nuevas,
hooks genéricos ni cambios de contrato. Los tests comprueban carga, vacío, errores,
permisos, acciones, validación del motivo, respuestas tardías y paginación.
