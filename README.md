# BikeMatch

MVP de un bootcamp Java: foto lateral de una bici → seis puntos → medidas → curvas
de leverage, kickback, trayectoria del eje, anti-squat y anti-rise de referencia.
Los descriptores alimentan una explicación breve y controlada. El backend ya tiene
registro/login JWT, control de acceso por roles y persistencia de bicicletas, fotos,
puntos, resultados e interpretaciones.

## Estado

Sprint 1 completado: motor monopivote simple y asistente React operativos, incluida
la validación manual de una foto real contra Linkage Design ([#54](https://github.com/DvToledo199/bike-match/issues/54)).
Sprint 2 en curso: el backend ya permite registrar, iniciar sesión, crear una bicicleta,
subir su foto y finalizar un análisis persistente. El frontend continúa usando el preview
público, que no guarda la foto ni datos en la base de datos. PostgreSQL es necesario para
arrancar Spring y Flyway.
No soporta monopivotes con bieleta que modifica el accionamiento del amortiguador,
cuatro barras ni pivotes virtuales. Son estimaciones, no mediciones de laboratorio.

## Arranque local

Requisitos: Java 21, Node 24.15+ o Node 26, npm y Docker con Compose. Spring Boot
permanece en **3.5.16**, la versión del curso.

Crea `.env` copiando `.env.example` y `frontend/.env` copiando
`frontend/.env.example`, **solo si no existen**. No sobrescribas credenciales.
Los valores de ejemplo son exclusivamente locales. Usa entradas `KEY=value`, sin
`export` ni comillas: Spring importa el `.env` raíz cuando se arranca desde la raíz
o desde `backend/`. Las variables del entorno tienen prioridad.

```bash
# Terminal 1, desde la raíz
docker compose --env-file .env -f docker/docker-compose.yml up -d

# Terminal 2, desde la raíz
cd backend
./mvnw spring-boot:run

# Terminal 3, desde la raíz
cd frontend
npm ci
npm run dev
```

Web: http://localhost:5173. Health: http://localhost:8080/api/health.
API y PostgreSQL escuchan solo en loopback por defecto. Para desplegar habrá que
configurar `SERVER_ADDRESS`, `CORS_ALLOWED_ORIGINS`, secretos y TLS: **esto no es un
despliegue de producción**. Cambiar una contraseña en `.env` no la cambia dentro de
un volumen PostgreSQL ya inicializado; no borres ese volumen sin proteger sus datos.

Si falla la conexión durante el asistente público, comprueba Docker y health. El botón
de reintento conserva tus marcas. Recargar o cerrar la pestaña **sí las pierde**: ese
preview solo vive en memoria. El flujo autenticado para guardar una bicicleta ya existe
en backend, pero todavía no está conectado a la interfaz.

## Autenticación y roles

El análisis de una foto es deliberadamente público: una persona puede probar
BikeMatch sin crear una cuenta, pero no se guarda ni la foto ni el resultado. Registro,
login, health y `POST /api/kinematics/preview` no requieren token. Las demás rutas
privadas requieren `Authorization: Bearer <JWT>`.

- Sin token o con un token inválido/caducado: `401 Unauthorized`.
- Token válido sin el permiso requerido: `403 Forbidden`.
- Las rutas futuras bajo `/api/moderation/**` exigen `ROLE_MODERATOR`; las de usuario
  quedan protegidas y los controladores concretos añadirán sus reglas al crearse.

La sesión es *stateless*: el backend no guarda una sesión web. En cada petición
privada, el filtro verifica firma, caducidad, ID y rol del JWT antes de llegar al
controlador.

### Moderador inicial local

La cuenta moderadora es opcional. Añade **las tres** variables a tu `.env` local
ignorando por Git: `INITIAL_MODERATOR_EMAIL`, `INITIAL_MODERATOR_USERNAME` e
`INITIAL_MODERATOR_PASSWORD_HASH`. El último valor debe ser un hash BCrypt, no una
contraseña. Con Docker instalado puedes generar uno sin que se muestre la contraseña:

```bash
docker run --rm -it httpd:2.4-alpine htpasswd -nBC 12 moderator
```

El comando la solicita de forma oculta y muestra `moderator:$2...`; copia solo la
parte que empieza por `$2` como `INITIAL_MODERATOR_PASSWORD_HASH`. Al arrancar, la
aplicación crea la cuenta si no existe. Con las tres variables vacías no hace nada;
una configuración incompleta, un hash que no sea BCrypt o un conflicto con otra cuenta
detienen el arranque para no crear una cuenta insegura ni promocionar a alguien por
accidente. El esquema sigue perteneciendo a Flyway: esto es un dato inicial local, no
una contraseña dentro de una migración versionada.

## Documentación de la API (Swagger)

Con el backend arrancado, la documentación está en:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Especificación OpenAPI (JSON): http://localhost:8080/v3/api-docs

Las dos son públicas. Las operaciones marcadas con un candado piden un JWT. Health,
preview, registro, login y catálogo no lo necesitan. La ficha y la explicación de una
bici pública tampoco, pero sí las de una bici privada, que solo ve su propietario.

Para probar una operación protegida:

1. Crea una cuenta con `POST /api/auth/register`, o usa una que ya exista.
2. Ejecuta `POST /api/auth/login` y copia el valor de `accessToken` de la respuesta.
3. Pulsa **Authorize**, pega el token (sin escribir `Bearer`) y confirma.
4. Desde ese momento, Swagger UI envía `Authorization: Bearer <token>` en cada petición.

La moderación exige además una cuenta con rol `MODERATOR` (ver el apartado anterior).

## Pruebas

```bash
# Desde backend/, con PostgreSQL funcionando
./mvnw clean verify

# Desde frontend/
npm test
npm run lint
npm run build
npm audit
```

Los tests del backend usan su propia base de datos, `bikematch_test`, para no modificar
nunca los datos de desarrollo. En un volumen de Docker nuevo se crea automáticamente; si tu
volumen ya existía, créala una sola vez:

```bash
docker exec bikematch-postgres sh -c 'createdb -U "$POSTGRES_USER" bikematch_test'
```

Para usar otro nombre, define `POSTGRES_TEST_DB`. El siguiente `docker compose up -d`
recrea el contenedor para montar el script de inicio, sin tocar el volumen de datos.

GitHub Actions ejecuta Java y, por separado, tests/lint/build del frontend. JaCoCo y la
cobertura ≥60% siguen previstos para Sprint 3: el número de tests no acredita por sí solo
ese porcentaje.

## Organización y contrato

- `backend/`: DTOs/API Spring y motor geométrico Java independiente de Spring.
- `frontend/src/features/analysis-wizard/`: pasos, estado y gráficas.
- `frontend/src/services/`: peticiones, timeout, cancelación y validación de respuestas.
- `frontend/src/styles/tokens.css`: colores y medidas; modos claro y oscuro.
- `frontend/src/locales/en/translation.json`: textos de la interfaz.
- `docker/`: PostgreSQL local; Flyway es el único dueño del esquema.

`POST /api/kinematics/preview` es público y solo calcula. Recibe seis tipos de punto
únicos, coordenadas finitas y medidas dentro de los límites del formulario. Los
datos inválidos se rechazan con HTTP 400; ejemplo en [la guía frontend](docs/frontend-arranque.md).
La foto debe ser lateral, nivelada y con suspensión extendida; puede mirar a ambos
lados. Selector Full 29/Mullet/Full 27,5, sin pedir peso. El cálculo ampliado incluye
piñón, radios nominales, CG de referencia y corrección de inclinación teniendo en
cuenta ruedas distintas. Versiones y supuestos viajan con los resultados.

El flujo persistente requiere JWT: `POST /api/bikes` guarda los metadatos privados,
`POST /api/bikes/{id}/photo` adjunta la foto y `POST /api/bikes/{id}/analysis` recibe
las dimensiones naturales y los seis puntos, calcula las curvas y guarda fuente y
resultado en una misma transacción. Desde entonces foto y puntos quedan bloqueados;
volver a marcarlos crea otra bicicleta/análisis. Los parámetros técnicos podrán tener
un flujo de edición y recálculo posterior sin modificar esa fuente.

`GET /api/my-bikes` requiere JWT y devuelve únicamente los resúmenes de las bicicletas
del usuario autenticado, ordenados de más reciente a más antigua. No expone puntos ni
curvas: esos datos pertenecen a la pantalla de detalle.

`GET /api/bikes/{id}` devuelve la ficha completa. Una bici `PUBLIC` se puede consultar
sin iniciar sesión; una bici `PRIVATE`, `PENDING` o `REJECTED` solo se devuelve a su
propietario autenticado. La respuesta incluye la foto, los metadatos, el username público
del propietario y el resultado guardado con sus curvas, descriptores y capacidades. No
devuelve los puntos internos de marcado, el correo ni ningún dato de autenticación.

La explicación básica se genera y se reutiliza desde el backend. `POST
/api/bikes/{id}/interpretation?language=en` solo lo puede pedir el propietario de la
bici y devuelve un resumen junto con 2–4 cifras que lo respaldan. `GET
/api/bikes/{id}/interpretation?language=en` permite leer una explicación ya guardada
si la bici es pública o si quien consulta es su propietario; una visita pública no
genera una llamada al proveedor. El modo predeterminado es `rules`, determinista y sin
coste externo. Gemini se activa únicamente con `INTERPRETATION_PROVIDER=gemini`,
`GEMINI_API_KEY` y `GEMINI_MODEL` en el entorno del backend; si falla, se guarda una
explicación por reglas identificada como `source=RULES`. No se envían foto, puntos,
correo, contraseña ni perfil personal al proveedor.

La caché se versiona por resultado, contexto, reglas, idioma, proveedor y prompt. Las
peticiones que tendrían que llamar al proveedor tienen además un enfriamiento local
configurable (`INTERPRETATION_GENERATION_COOLDOWN`, 30 segundos por defecto); el
resumen y el contexto tienen límites de tamaño y la respuesta externa se valida como
texto plano con evidencias conocidas. El límite local deberá sustituirse por una
cuota compartida si el backend se despliega en varias instancias.

`GET /api/bikes?page=0&category=ENDURO` es el catálogo público. No necesita JWT, solo
devuelve bicicletas `PUBLIC` y responde con páginas de 12 tarjetas (`items`, `page`,
`totalPages` y `hasNext`). La categoría es opcional y admite `ENDURO`, `E_ENDURO` o
`DOWNHILL`. El catálogo no incluye curvas ni datos privados: para eso se abre la ficha.

La pantalla de catálogo del frontend usa `listPublicBikes()` para cargar esas páginas,
permite filtrar por categoría y abre la ficha pública reutilizando `getBikeDetail(id)`.

Después del login, el cliente HTTP del frontend añade automáticamente el JWT guardado
en la sesión del navegador a las peticiones privadas. La pantalla de "Mis bicis" usa
`listMyBikes()` para los resúmenes; al abrir una tarjeta, la ficha usa
`getBikeDetail(id)` y reutiliza las gráficas con el resultado guardado.

`POST /api/bikes/{id}/publish` requiere JWT y permite al propietario pedir la
publicación de una bici privada. La bici pasa a `PENDING`, pendiente de moderación;
otro usuario recibe `403` y una bici inexistente recibe `404`.

La moderación requiere un JWT de una cuenta con rol `MODERATOR`:

- `GET /api/moderation/pending` devuelve la cola de bicicletas pendientes, con sus
  datos de identificación y el username de quien la solicitó, sin exponer puntos ni
  resultados completos.
- `POST /api/moderation/{id}/approve` cambia una bici `PENDING` a `PUBLIC`.
- `POST /api/moderation/{id}/reject` cambia una bici `PENDING` a `REJECTED`.

Una cuenta `USER` recibe `403` en estas rutas. Una bicicleta inexistente devuelve
`404`; si ya no estaba pendiente, devuelve `409` y no se modifica.

Son **estimaciones bajo condiciones de referencia**, no medidas individuales ni
validación de campo. Los clientes sin ruedas conservan el cálculo V1 (sin curvas
anti ni efecto del piñón). Fuentes, fórmulas, límites y mejoras futuras:
[modelo de referencia](docs/modelo-referencia-cinematica.md). El contraste externo
estricto sigue abierto en #31.

## Seguimiento

Una tarea = una issue = una rama = un PR pequeño. Código y commits en inglés;
explicaciones de aprendizaje en español. La evolución comunitaria se mantiene en una
[visión de producto separada](docs/vision-producto-v2.md) para no mezclarla con la V1.

- [Hoja de ruta](docs/hoja-de-ruta-sprints.md)
- [Visión de producto V2](docs/vision-producto-v2.md)
- [Limitaciones](docs/limitaciones-y-mejoras.md)
- [Informe completo histórico en JSON](docs/auditorias/2026-09-06-informe.json)
- [Correcciones, pruebas y pendientes](docs/auditorias/2026-09-06-correcciones.md)
