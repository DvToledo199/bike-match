# BikeMatch

MVP de un bootcamp Java: foto lateral de una bici → seis puntos → medidas → curvas
de leverage, kickback, trayectoria del eje, anti-squat y anti-rise de referencia.
Los descriptores serán entrada de una futura explicación con IA. El backend ya tiene
registro/login JWT, control de acceso por roles y persistencia de bicicletas, fotos,
puntos y resultados. La interfaz de producto y la explicación con IA siguen pendientes.

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
curvas: esos datos pertenecen a la futura pantalla de detalle.

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
