# BikeMatch

BikeMatch explica cómo trabaja la suspensión trasera de una bici de montaña a partir de
una foto lateral. Se marcan los pivotes sobre la foto (6, 9 o 10 puntos según la
suspensión), se indican el tipo de bici y unas pocas medidas, y el backend calcula las
curvas de relación de palanca, pedal kickback, trayectoria del eje, anti-squat y anti-rise.
Un resumen en palabras sencillas, generado con Gemini mediante Spring AI y con un texto por
reglas como respaldo, cuenta qué significan. Las bicis se guardan en privado y pueden
publicarse en un catálogo comunitario, previa moderación.

Proyecto final de un bootcamp Java. Versión publicada: https://bikematch.onrender.com

**Problema que resuelve.** Los programas que calculan la cinemática de una suspensión
exigen mucho al aficionado: marcar a mano los puntos sin guía y, sobre todo, interpretar
solo unas gráficas técnicas. BikeMatch guía el marcado y traduce el resultado a lenguaje
normal. La idea completa está en [la idea y el producto](docs/proyecto-cinematica-bicis.md).

## Funcionalidades

- **Motor cinemático** para monopivote y tres variantes de cuatro barras (Horst link):
  amortiguador en la bieleta, con extensión rígida del amortiguador y empujado por los
  tirantes ([modelos](docs/modelos-cuatro-barras.md)). El monopivote se validó con una foto
  real frente a curvas publicadas ([#54](https://github.com/DvToledo199/bike-match/issues/54));
  la validación sistemática de las cuatro barras sigue abierta
  ([#222](https://github.com/DvToledo199/bike-match/issues/222),
  [#237](https://github.com/DvToledo199/bike-match/issues/237)).
- **Asistente**: foto, marcado guiado de puntos, tipo de bici y medidas, y gráficas al
  momento, sin necesidad de cuenta.
- **Cuentas y roles**: registro y login con JWT; usuario, moderador y administrador.
- **Mis bicis**: guardar el análisis con su foto (Cloudinary), pedir su publicación desde la
  lista o desde la ficha, y borrarla.
- **Comunidad**: catálogo público por categorías, moderación (aprobar, rechazar y retirar
  con aviso al dueño) y administración de cuentas y roles.
- **Explicación**: Gemini mediante Spring AI; si no responde, un texto por reglas lo
  sustituye y la web lo indica.
- **Idiomas**: interfaz y explicación en inglés y en español.

No soporta monopivotes con bieleta que modifica el accionamiento del amortiguador, pivotes
virtuales, pivote alto con polea ni frenos flotantes. Los resultados son estimaciones
geométricas a partir de una foto, no mediciones de laboratorio; el detalle está en las
[limitaciones](docs/limitaciones-y-mejoras.md).

## Historias de usuario

Tablero del proyecto (backlog, prioridad y estado):
https://github.com/users/DvToledo199/projects/2

| Historia | Estado |
|---|---|
| [#2](https://github.com/DvToledo199/bike-match/issues/2) Catálogo público con filtros por categoría | Hecha |
| [#3](https://github.com/DvToledo199/bike-match/issues/3) Detalle de bici: gráficas, números y explicación | Hecha |
| [#5](https://github.com/DvToledo199/bike-match/issues/5) Registro, login JWT y roles | Hecha |
| [#6](https://github.com/DvToledo199/bike-match/issues/6) Crear bici: foto, calibración y marcado guiado | Hecha |
| [#7](https://github.com/DvToledo199/bike-match/issues/7) Guardar bici en privado o publicarla | Hecha |
| [#8](https://github.com/DvToledo199/bike-match/issues/8) Moderación: aprobar o rechazar bicis pendientes | Hecha |
| [#10](https://github.com/DvToledo199/bike-match/issues/10) Explicación sencilla con IA | Hecha (la personalización por cuestionario, pendiente) |
| [#9](https://github.com/DvToledo199/bike-match/issues/9) Votar bicis públicas | Futuro |
| [#4](https://github.com/DvToledo199/bike-match/issues/4) Rankings por categoría | Futuro |
| [#11](https://github.com/DvToledo199/bike-match/issues/11) Comparador de bicis | Futuro |

Las historias se desglosaron en issues técnicas más pequeñas (motor, persistencia,
seguridad, pantallas…).

## Stack

| Parte | Tecnologías |
|---|---|
| Backend | Java 21, Spring Boot **3.5.16** (la versión del curso), Spring Web, Spring Security con JWT (jjwt), Spring Data JPA, Bean Validation, springdoc-openapi (Swagger UI), Spring AI con Google GenAI (Gemini), SDK de Cloudinary |
| Base de datos | PostgreSQL 16 con Flyway |
| Frontend | React 19 + Vite (JavaScript), react-i18next, Recharts, CSS Modules |
| Pruebas | JUnit 5, Mockito, MockMvc, JaCoCo; Vitest y Testing Library |
| Infraestructura | Docker Compose (PostgreSQL local), GitHub Actions, Render (web y API) y Neon (PostgreSQL) |

## Arquitectura

Monorepo: `backend/` (API REST), `frontend/` (web), `docker/` (PostgreSQL local) y `docs/`.

**Backend.** Organizado por funcionalidades (`auth`, `bike`, `moderation`, `user`,
`interpretation`, `kinematics`…), cada una con su controlador → servicio → repositorio.
Los controladores validan la entrada con `@Valid`, y un manejador global
(`ApiExceptionHandler`) convierte los errores en respuestas `ProblemDetail` coherentes
(400, 404, 409, 429…). Los 401 y 403 de Spring Security usan de momento la respuesta de
error estándar de Spring Boot; unificarlos está pendiente en
[#202](https://github.com/DvToledo199/bike-match/issues/202). Dos piezas merecen mención:

- **El motor** (`kinematics`) es un dominio puro, sin Spring ni base de datos: recibe
  puntos en milímetros y devuelve curvas y descriptores. Se prueba de forma aislada. Ver
  [fundamentos del motor](docs/fundamentos-motor-cinematica.md).
- **Los servicios externos** se usan a través de interfaces: `ImageStorage` (implementada
  con Cloudinary) e `InterpretationProvider` (reglas o Gemini). Cambiar de proveedor no
  toca el resto del código.

**Modelo de datos.** `users` (1:N) `bikes` (1:1) `kinematics_results`, que tiene una
explicación por idioma en `kinematics_interpretations`; `bike_removal_notices` guarda los
avisos de bicis retiradas para su dueño. Los puntos marcados se guardan como JSON en `bikes`.

**Frontend.** Una carpeta por funcionalidad en `frontend/src/features`, las llamadas a la
API en `frontend/src/services` y los textos en los archivos de traducción. Detalle en el
[README del frontend](frontend/README.md).

## Arranque local

Requisitos: Java 21, Node 24.15+ o Node 26, npm y Docker con Compose.

Crea `.env` copiando `.env.example` y `frontend/.env` copiando `frontend/.env.example`,
**solo si no existen**. No sobrescribas credenciales. Los valores de ejemplo son
exclusivamente locales. Usa entradas `KEY=value`, sin `export` ni comillas: Spring importa
el `.env` raíz cuando se arranca desde la raíz o desde `backend/`. Las variables del
entorno tienen prioridad.

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

**Migraciones.** Flyway aplica las migraciones de `backend/src/main/resources/db/migration`
(V1 a V11) cada vez que arranca el backend, y Hibernate solo valida el esquema
(`ddl-auto=validate`): no se crea ni se modifica ninguna tabla a mano.

API y PostgreSQL escuchan solo en loopback por defecto. Cambiar una contraseña en `.env`
no la cambia dentro de un volumen PostgreSQL ya inicializado; no borres ese volumen sin
proteger sus datos. Recargar la página durante el asistente **pierde el análisis no
guardado**, que solo vive en memoria.

### Variables de entorno

Todas están en `.env.example`, con comentarios:

| Variables | Para qué |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_HOST`, `DB_PORT` | Base de datos local |
| `SERVER_ADDRESS`, `CORS_ALLOWED_ORIGINS` | Dónde escucha la API y qué web puede llamarla |
| `JWT_SECRET_BASE64`, `JWT_EXPIRATION` | Firma y caducidad de los tokens |
| `INITIAL_MODERATOR_*`, `INITIAL_ADMIN_*` | Cuentas iniciales opcionales (ver *Cuentas iniciales*) |
| `CLOUDINARY_URL` | Subida de fotos |
| `INTERPRETATION_PROVIDER`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_TIMEOUT`, `INTERPRETATION_GENERATION_COOLDOWN` | Explicación: `rules` o Gemini |
| `VITE_API_URL` (en `frontend/.env`) | Dirección de la API para la web |

### Fotos con Cloudinary

Guardar una bici sube su foto a Cloudinary, así que el backend necesita `CLOUDINARY_URL`
en el `.env` raíz:

1. En el panel de Cloudinary, crea una clave API propia para BikeMatch en lugar de usar
   la clave Root de la cuenta.
2. Asigna a esa clave un **rol con permiso para subir y borrar imágenes**. Sin rol,
   Cloudinary responde `Request forbidden due to missing permissions`: la foto no se
   guarda y el motivo queda en el log del backend.
3. Copia la variable completa, `cloudinary://<api_key>:<api_secret>@<cloud_name>`, y
   sustituye los marcadores que muestra el panel (`<your_api_key>`, `<your_api_secret>`)
   por los valores de la clave. Si queda algún marcador, la subida falla.

El `api_secret` es un secreto: no lo compartas ni lo subas al repositorio.

## Autenticación y roles

El análisis de una foto es deliberadamente público: una persona puede probar BikeMatch
sin crear una cuenta, pero no se guarda ni la foto ni el resultado. Registro, login,
health, `POST /api/kinematics/preview`, el catálogo público y la ficha y la explicación de
una bici pública no requieren token. Las demás rutas privadas requieren
`Authorization: Bearer <JWT>`.

| Rol | Puede |
|---|---|
| `USER` | Analizar, guardar sus bicis, pedir su publicación, borrarlas y ver sus avisos |
| `MODERATOR` | Además, aprobar, rechazar o retirar bicis (`/api/moderation/**`) |
| `ADMIN` | Además, ver las cuentas y conceder o retirar el rol de moderador (`/api/admin/**`) |

- Sin token o con un token inválido/caducado: `401 Unauthorized`.
- Token válido sin el permiso requerido: `403 Forbidden`.

La sesión es *stateless*: el backend no guarda una sesión web. En cada petición privada,
el filtro verifica firma, caducidad, ID y rol del JWT antes de llegar al controlador.

### Cuentas iniciales

La aplicación puede sembrar al arrancar dos cuentas opcionales, una moderadora y una
administradora. Cada una necesita **sus tres** variables en el `.env` local, que Git
ignora:

| Cuenta | Variables |
|---|---|
| Moderador | `INITIAL_MODERATOR_EMAIL`, `INITIAL_MODERATOR_USERNAME`, `INITIAL_MODERATOR_PASSWORD_HASH` |
| Administrador | `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_USERNAME`, `INITIAL_ADMIN_PASSWORD_HASH` |

Existen porque ninguna de las dos se puede crear desde la aplicación: el registro da
siempre de alta usuarios normales, y solo un administrador puede cambiarle el rol a otra
persona. Sin esta siembra, una base de datos nueva no tendría a nadie con permisos.

El tercer valor debe ser un **hash BCrypt**, no una contraseña. Con Docker instalado
puedes generar uno sin que la contraseña se muestre por pantalla:

```bash
docker run --rm -it httpd:2.4-alpine htpasswd -nBC 12 moderator
```

En macOS, `htpasswd` ya viene instalado y no hace falta Docker:

```bash
htpasswd -nBC 12 moderator
```

El comando pide la contraseña de forma oculta y muestra `moderator:$2...`; se copia solo
la parte que empieza por `$2`.

Al arrancar, la aplicación crea cada cuenta si no existe. Con sus tres variables vacías no
hace nada. Una configuración incompleta, un hash que no sea BCrypt o un conflicto con una
cuenta que ya existe **detienen el arranque**: así no se crea una cuenta insegura, y nadie
asciende a un usuario existente con solo editar una variable.

Para iniciar sesión se usa el correo y la **contraseña que se escribió al generar el
hash**, nunca el hash. Si la cuenta ya existe, cambiar estas variables no la modifica. El
esquema sigue perteneciendo a Flyway: esto es un dato inicial, no una contraseña dentro de
una migración.

## Documentación de la API (Swagger)

| | Local | Publicada |
|---|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html | https://bikematch-api.onrender.com/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs | https://bikematch-api.onrender.com/v3/api-docs |

Las operaciones marcadas con un candado piden un JWT. Para probar una operación protegida:

1. Crea una cuenta con `POST /api/auth/register`, o usa una que ya exista.
2. Ejecuta `POST /api/auth/login` y copia el valor de `accessToken` de la respuesta.
3. Pulsa **Authorize**, pega el token (sin escribir `Bearer`) y confirma.
4. Desde ese momento, Swagger UI envía `Authorization: Bearer <token>` en cada petición.

**Endpoints principales** (el detalle de cada esquema y respuesta está en Swagger):

| Ruta | Acceso | Qué hace |
|---|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Público | Crear cuenta y obtener el JWT |
| `POST /api/kinematics/preview` | Público | Calcula las curvas sin guardar nada |
| `GET /api/bikes?page=&category=` | Público | Catálogo de bicis públicas, 12 por página |
| `GET /api/bikes/{id}` | Pública: todos; si no, su dueño | Ficha completa con el resultado guardado |
| `GET` / `POST /api/bikes/{id}/interpretation?language=` | Leer: como la ficha; generar: el dueño | Explicación en `en` o `es` |
| `POST /api/bikes`, `POST /api/bikes/{id}/photo`, `POST /api/bikes/{id}/analysis` | Usuario / dueño | Guardar una bici, su foto y su análisis |
| `GET /api/my-bikes` | Usuario | Resúmenes de sus bicis |
| `POST /api/bikes/{id}/publish` | Dueño | Pedir la publicación (pasa a `PENDING`) |
| `DELETE /api/bikes/{id}` | Dueño | Borrado definitivo, foto incluida |
| `GET /api/my-notices`, `POST /api/my-notices/{id}/dismiss` | Usuario | Avisos de bicis retiradas |
| `GET /api/moderation/pending`, `POST /api/moderation/{id}/approve \| reject \| remove` | `MODERATOR` o `ADMIN` | Cola de moderación y decisiones |
| `GET /api/admin/users`, `PUT /api/admin/users/{id}/role` | `ADMIN` | Cuentas y rol de moderador |

Tras guardar el primer análisis, la foto y los puntos de una bici quedan bloqueados:
volver a marcar crea otra bici. Una ficha nunca devuelve los puntos de marcado, el correo
ni datos de autenticación.

## Explicación con IA

La explicación la genera el backend a petición del propietario y se guarda por idioma;
una visita pública solo la lee y nunca llama al proveedor. El modo por defecto es `rules`,
determinista y sin coste. Gemini se activa con `INTERPRETATION_PROVIDER=google-genai`,
`GEMINI_API_KEY` y `GEMINI_MODEL` (por ejemplo, `gemini-3.6-flash`), con un solo intento y
60 s como máximo. Si falla, se guarda el texto por reglas con `fallback=true` y la web avisa
y permite reintentar. Al proveedor solo viajan cifras y clasificaciones del motor: nunca
foto, puntos, correo ni contraseña. Contrato, versiones y reglas en
[explicación con IA](docs/contrato-interpretacion-cinematica.md).

## Despliegue

La versión publicada usa dos servicios con plan gratuito sin fecha de caducidad:

| Pieza | Servicio | Cómo se construye |
|---|---|---|
| Web (React) | Render, sitio estático | `npm ci && npm run build` en `frontend/` |
| API (Spring Boot) | Render, contenedor | [`backend/Dockerfile`](backend/Dockerfile) |
| Base de datos | Neon, PostgreSQL | Flyway crea el esquema al arrancar la API |

| Enlace | Dirección |
|---|---|
| Web | https://bikematch.onrender.com |
| API | https://bikematch-api.onrender.com |
| Swagger desplegado | https://bikematch-api.onrender.com/swagger-ui.html |

[`render.yaml`](render.yaml) describe los dos servicios de Render (un *Blueprint*): al
conectarlo, Render crea la API y la web y pide los valores secretos, que no están en el
repositorio. Cada fusión en `main` vuelve a desplegar.

**El Dockerfile** tiene dos etapas. La primera compila el jar con el Maven del proyecto;
la segunda solo lleva Java y ese jar, sin Maven ni el código fuente, y lo ejecuta con un
usuario sin privilegios. La imagen no ejecuta los tests porque necesitan PostgreSQL, y ya
corren en CI en cada pull request.

**Variables de la API**

| Variable | Contenido |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host de Neon>/<base>?sslmode=require`, sin usuario ni contraseña |
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | Credenciales de Neon |
| `JWT_SECRET_BASE64` | La genera Render: clave aleatoria de 256 bits en base64 |
| `CORS_ALLOWED_ORIGINS` | Dirección de la web publicada |
| `CLOUDINARY_URL` | Credenciales de Cloudinary para las fotos |
| `INTERPRETATION_PROVIDER`, `GEMINI_API_KEY`, `GEMINI_MODEL` | Explicación con Gemini |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_USERNAME`, `INITIAL_ADMIN_PASSWORD_HASH` | Primer administrador; la contraseña solo como hash BCrypt |

La web solo necesita `VITE_API_URL`, la dirección de la API. Vite la incorpora al compilar:
si cambia, hay que volver a desplegar la web.

**Límites del plan gratuito y decisiones**

- La API se duerme tras 15 minutos sin visitas, y arrancarla en el plan gratuito llevaba
  unos dos minutos y medio (Render registró 143,7 s). Para que no llegue a dormirse, el
  flujo [`keep-api-awake.yml`](.github/workflows/keep-api-awake.yml) consulta `/api/health`
  cada diez minutos; las 750 horas mensuales del plan cubren un servicio encendido todo el
  mes. Si aun así se duerme, la web espera hasta tres minutos y el catálogo avisa de que el
  servidor está despertando.
- El contenedor tiene 512 MB y una décima de CPU. Java limita su memoria al 75 %, usa el
  recolector más ligero y solo el compilador JIT rápido: con tan poca CPU, el compilador
  optimizador compite con la aplicación mientras arranca. En una prueba local con ese mismo
  límite, el arranque pasó de 314 s a 170 s, y la API arranca usando unos 350 MB.
- La base de datos de Neon es accesible desde internet, con conexión cifrada obligatoria y
  contraseña. La alternativa privada dentro de Render caduca a los 30 días en el plan
  gratuito, así que se descartó.
- Dentro del contenedor la API escucha en todas las interfaces (`SERVER_ADDRESS=0.0.0.0`) y
  en el puerto que asigna Render (`PORT`). En local sigue escuchando solo en `127.0.0.1`.

## Pruebas

```bash
# Desde backend/, con PostgreSQL funcionando
./mvnw clean verify

# Desde frontend/
npm test
npm run lint
npm run build
```

**Backend** (364 tests en 71 clases):

- **Unitarios:** el motor (geometría, solvers, curvas y descriptores, con bicis reales como
  referencia) y los servicios, con dobles de sus dependencias.
- **Aceptación de la API** (MockMvc sobre cada controlador): respuestas correctas, validación,
  códigos de error y permisos (`401`/`403`) de cada endpoint.
- **Persistencia** contra PostgreSQL real: migraciones, restricciones y consultas.
- **Arranque completo** de la aplicación y de su documentación OpenAPI.

Los tests usan su propia base de datos, `bikematch_test`, para no modificar nunca los datos
de desarrollo. En un volumen de Docker nuevo se crea automáticamente; si tu volumen ya
existía, créala una sola vez:

```bash
docker exec bikematch-postgres sh -c 'createdb -U "$POSTGRES_USER" bikematch_test'
```

Para usar otro nombre, define `POSTGRES_TEST_DB`.

**Cobertura** con JaCoCo: **95,0 % de líneas** y 73,4 % de ramas. El enunciado pide un
mínimo del 60 %, y `./mvnw verify` **falla por debajo** de ese umbral, también en CI. El
informe se genera en `backend/target/site/jacoco/index.html`; en GitHub Actions, cada
ejecución lo muestra en el resumen y lo adjunta como `backend-coverage-report`.

**Frontend** (225 tests con Vitest y Testing Library): pasos del asistente, marcado,
validación de respuestas, guardado y reintentos, permisos de cada pantalla y paridad de
las claves de traducción.

## Flujo de trabajo

- Una tarea = una issue = una rama = un PR pequeño con `Closes #N`, integrado solo cuando
  la CI pasa. Las historias grandes se desglosan en issues técnicas.
- El [tablero](https://github.com/users/DvToledo199/projects/2) mueve cada issue por
  Backlog → Ready → In progress → In review → Done.
- GitHub Actions ejecuta en cada pull request los tests y la cobertura del backend y, por
  separado, los tests, el lint y el build del frontend. Render despliega al fusionar en `main`.
- Código, API, base de datos y commits en inglés; textos de la interfaz en los archivos de
  traducción (inglés y español).

## Documentación

- [La idea y el producto](docs/proyecto-cinematica-bicis.md)
- [Plan de trabajo](docs/plan-trabajo-mvp.md) y [hoja de ruta](docs/hoja-de-ruta-sprints.md)
- [Fundamentos del motor](docs/fundamentos-motor-cinematica.md),
  [modelos de cuatro barras](docs/modelos-cuatro-barras.md) y
  [modelo de referencia](docs/modelo-referencia-cinematica.md)
- [Base de conocimiento](docs/base-conocimiento-cinematica.md) y
  [explicación con IA](docs/contrato-interpretacion-cinematica.md)
- [Panel de moderación](docs/panel-moderacion.md) y
  [panel de administración](docs/panel-administracion.md)
- [Limitaciones y mejoras](docs/limitaciones-y-mejoras.md)
- [README del frontend](frontend/README.md)
- [Enunciado del proyecto](docs/enunciado.md)
