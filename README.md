# BikeMatch

MVP de un bootcamp Java: foto lateral de una bici → seis puntos → medidas → curvas
de leverage, kickback, trayectoria del eje, anti-squat y anti-rise de referencia.
Los descriptores serán entrada de una futura explicación con IA. El backend ya tiene
registro/login; autorización #94, persistencia de bicis e IA siguen pendientes.

## Estado

Sprint 1 completado: motor monopivote simple y asistente React operativos, incluida
la validación manual de una foto real contra Linkage Design ([#54](https://github.com/DvToledo199/bike-match/issues/54)).
Comienza Sprint 2: cuentas y persistencia. El preview actual no guarda la foto ni
datos en la base de datos. PostgreSQL es necesario para arrancar Spring y Flyway.
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

Si falla la conexión, comprueba Docker y health. El botón de reintento conserva tus
marcas. Recargar o cerrar la pestaña **sí las pierde**: solo viven en memoria.

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

GitHub Actions ejecuta Java y, por separado, tests/lint/build del frontend. Auditoría
actualizada el 10 de septiembre: **112 tests Java y 34 frontend**. JaCoCo y la cobertura ≥60%
siguen previstos para Sprint 3: el número de tests no acredita ese porcentaje.

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
