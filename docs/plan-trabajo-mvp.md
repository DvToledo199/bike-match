# Plan de trabajo — MVP análisis de cinemática MTB

Documento de trabajo. Orden operativo: qué cerrar antes de codificar, cómo trabajar, en qué orden construir y qué vigilar.

---

## 1. Decisiones a cerrar ANTES de escribir código

### 1.1 Sistemas de suspensión de la v1
- **v1: monopivote (con y sin bieleta que acciona el amortiguador) + 4 barras / Horst link.**
- Dato útil del proyecto de referencia (bikinematicsolver): un solver que cubra monopivote + 4 barras maneja **~80% de las bicis del mercado**, por muchos nombres comerciales que existan (los sistemas de marca son casi todos variantes de estas dos topologías).
- **Fuera de v1:** high pivot con polea (el autor de referencia lo dejó a medias: es notablemente más complejo), VPP/switch infinity y sistemas de más de 4 barras.

### 1.2 Calibración píxel → milímetros (decisión crítica)
El usuario marca puntos en píxeles sobre la foto; el motor necesita distancias reales.
- **Opción recomendada:** el usuario introduce la medida **eye-to-eye del amortiguador** (dato estándar y conocido, p. ej. 230×65). Como ya marca los dos anclajes del amortiguador en la foto, la escala sale de: `mm reales / píxeles entre anclajes`.
- **Alternativa/verificación cruzada:** distancia entre ejes (wheelbase) de la ficha oficial, marcando ambos ejes. Es lo que usa el proyecto de referencia.
- **Regla técnica:** guardar los puntos **normalizados (0–1) respecto a las dimensiones naturales de la imagen**, no en píxeles de pantalla, o el canvas responsive descuadrará las coordenadas según el dispositivo.
- **Sanity check integrado:** el motor calcula el recorrido trasero; si difiere mucho del recorrido declarado por el usuario (>±10%), avisar de que los puntos o la calibración están mal. Esto convierte un punto débil (precisión del marcado) en una funcionalidad de calidad.

### 1.3 Datos mínimos a pedir por bici
Marca, modelo, año, categoría (Enduro / e-Enduro / DH), tipo de suspensión, recorrido trasero declarado (mm), medidas del amortiguador (eye-to-eye y carrera, p. ej. 230×65), tamaño de rueda, dientes del plato (para el kickback), foto lateral. **Opcional:** tabla de geometría.

### 1.4 Stack y versiones (evitar problemas de compatibilidad)
- **Java 21 LTS + Spring Boot 3.x** (la versión que uses en el curso; no mezclar con tutoriales de Boot 2: los imports son `jakarta.*`, no `javax.*`).
- **Spring Security 6:** ya no existe `WebSecurityConfigurerAdapter`; se configura con bean `SecurityFilterChain` + DSL lambda. Ojo con tutoriales antiguos.
- **OpenAPI:** `springdoc-openapi-starter-webmvc-ui` **2.x**. No usar Springfox (muerto, incompatible con Boot 3).
- **JWT:** jjwt 0.12.x (su API cambió respecto a los tutoriales viejos).
- **PostgreSQL 16+** con **Flyway**. `spring.jpa.hibernate.ddl-auto=validate` (nunca `update`); todo cambio de esquema = migración versionada.
- **Frontend: React + Vite** (elige uno y no lo cambies; React es el que mejor genera la IA). Node LTS. Librería de gráficas: la que proponga la IA (Recharts/Chart.js), con tal de que pinte arrays de puntos.
- **Docker Compose** para Postgres desde el día 1.
- **Cálculo:** `double` (es geometría, no dinero; no usar BigDecimal aquí).

### 1.5 Arquitectura y repositorio
- **Hexagonal**, con el **motor de cinemática como dominio puro** (cero dependencias de Spring/BD). Beneficio triple: es la arquitectura que valora el nivel 3 del enunciado, el motor se testea con unit tests puros (cobertura barata y con sentido), y podrías reutilizarlo.
- No sobre-ingenierizar los CRUD simples: hexagonal aplicada con criterio, no ceremonia en cada tabla.
- **Monorepo:** `/backend`, `/frontend`, `/docs` (enunciado incluido), `/docker`. Un solo repo = un solo GitHub Project, issues y PRs enlazados.

### 1.6 Almacenamiento de fotos
**Cloudinary (free tier) desde el principio.** Motivo práctico: los despliegues gratuitos (Render/Railway/Fly) tienen **filesystem efímero: las fotos guardadas en disco desaparecen en cada redeploy**. Además cuenta como integración externa de nivel 3.

---

## 2. Checklist de requisitos del enunciado

Obligatorios (nivel 1) y cómo los cubre este proyecto:

| Requisito | Cómo se cubre |
|---|---|
| Frontend con framework | React + Vite (generado con IA, revisado y explicable) |
| Backend API REST | Spring Boot |
| PostgreSQL + migraciones | Flyway desde V1, `ddl-auto=validate` |
| JWT + Spring Security | Login/registro, filtro JWT, rutas protegidas |
| ≥2 roles coherentes | `USER` (crea/publica/vota) y `MODERATOR` (aprueba/rechaza) — salen naturales del caso de uso |
| OpenAPI + Swagger UI | springdoc 2.x, anotaciones en controllers, esquemas y auth documentados |
| Modelo de datos coherente | Ver sección 6 |
| Tests unitarios + aceptación, ≥60% cobertura | Unit tests del motor (muchos y baratos) + tests de API (flujo principal y errores). JaCoCo con umbral |
| Docker (mínimo BD) | docker-compose con Postgres (y opcionalmente backend) |
| Despliegue funcional | Backend en Render/Railway/Fly + front en Vercel/Netlify. Desplegar YA en fase 0, no al final |
| README completo | Seguir la lista literal del enunciado como plantilla |
| GitHub Project + issues/branches/PRs | Ver sección 3 |

Nivel 2 (calidad): validaciones con `@Valid`, gestión de errores consistente (handler global), variables de entorno, logs útiles, migraciones reproducibles, front con componentes separados (no todo en un fichero).

Nivel 3 (a los que este proyecto apunta de forma natural): **arquitectura hexagonal bien aplicada, integración externa (Cloudinary), Spring AI (capa de análisis en lenguaje natural), CI/CD con GitHub Actions, caché si aporta** (p. ej. rankings).

---

## 3. Método de trabajo: rebanadas verticales

Backend y frontend avanzan juntos, historia a historia. Nunca "todo el backend y luego el front": la integración (CORS, JWT, formas de los JSON) es donde mueren estos proyectos, y en pequeñito se resuelve cada dos días en vez de en pánico la última semana.

**Bucle por cada historia de usuario:**
1. Mover la tarjeta en el GitHub Project → crear **issue** → crear **branch** desde main.
2. Escribir el **contrato JSON** del endpoint (request/response, códigos de error) ANTES de codificar. Vive en `docs/api-contracts.md` o directamente como anotaciones OpenAPI.
3. **Backend:** migración → dominio → servicio → controller con validación → tests (unit + aceptación del endpoint).
4. **Frontend con IA:** darle el contrato + un boceto de la pantalla como especificación. Revisar el código generado.
5. Prueba manual de punta a punta (front real contra back real).
6. **Pull request** → revisión propia → merge → cerrar issue → mover tarjeta.

**Definition of Done por historia:** tests en verde, cobertura no baja, Swagger actualizado, migración aplicada en limpio, la pantalla funciona contra el backend, tarjeta cerrada.

**Regla para el frontend generado por IA** (el enunciado exige poder explicarlo): antes de mergear una pantalla, saber responder: ¿dónde vive el estado? ¿qué componente llama a la API y cómo viaja el token? ¿cómo se gestionan carga y errores? Si no lo sabes, pedir a la IA que lo explique primero.

**Commits/PRs pequeños.** Si una historia es grande, partirla en sub-issues técnicas (modelo, seguridad, tests, pantalla…), como pide el enunciado.

---

## 4. Fases (orientado a ~4 semanas)

**F0 — Arranque (días 1–3).** Historias de usuario redactadas y **validadas con el mentor** (lo exige el enunciado antes de programar). GitHub Project montado con backlog priorizado. Decisiones de la sección 1 cerradas. Esqueletos back y front compilando, docker-compose con Postgres, migración V1, CI mínima (GitHub Actions: `mvn verify`), y un hello-world **desplegado** (back y front). Desplegar el día 3, no el día 25: el despliegue siempre da guerra.

**F1 — La parte difícil primero (semana 1).** Objetivo: flujo foto → puntos → curvas demostrable de punta a punta.
- Motor monopivote como módulo de dominio puro + tests validados contra Linkage.
- Pantalla de subir foto + marcar puntos guiado + calibración (front con IA).
- Endpoint que recibe puntos y parámetros y devuelve las curvas; gráfica en el front.
- Esto desriesga el proyecto entero: si algo no funciona como se espera, se descubre en la semana 1, con margen para reaccionar.

**F2 — Producto alrededor del motor (semana 2).** Registro/login JWT + roles. CRUD completo de bici (metadatos + foto en Cloudinary + puntos + resultados persistidos). Catálogo público con filtros por categoría. Detalle de bici con gráficas y números. Privado vs. publicar.

**F3 — Comunidad y cierre de mínimos (semana 3).** Moderación (PENDING → APPROVED/REJECTED, endpoints solo MODERATOR). Votos (N:M con restricción de único) + rankings por categoría. Cobertura ≥60% real, Swagger pulido, README completo, despliegue final verificado.

**F4 — Valor añadido (semana 4 / colchón).** Por orden: solver 4 barras (Horst) → capa IA con Spring AI (preguntas de estilo + análisis narrado desde los números del motor) → comparador de bicis → extras de CI/CD y observabilidad (actuator health, logs estructurados).

El MVP defendible se cierra en F3. F4 es la lista de "si hay tiempo", y lo que quede fuera va al README como trabajo futuro.

---

## 5. Historias de usuario (borrador, 8 + 2 stretch)

1. Como **visitante** quiero navegar el catálogo público con filtros por categoría para descubrir bicis analizadas.
2. Como **visitante** quiero ver el detalle de una bici (gráficas, números y explicación) para entender su comportamiento.
3. Como **usuario** quiero registrarme e iniciar sesión para crear y gestionar mis bicis. *(auth JWT)*
4. Como **usuario** quiero crear una bici subiendo una foto lateral, calibrando la escala y marcando los pivotes de forma guiada, para que el sistema calcule su cinemática. *(historia estrella; partirla en sub-issues)*
5. Como **usuario** quiero guardar mi bici en privado o publicarla para compartirla con la comunidad.
6. Como **moderador** quiero revisar las bicis pendientes y aprobarlas o rechazarlas para mantener la calidad del catálogo. *(2º rol)*
7. Como **usuario** quiero votar bicis públicas para destacar las mejores.
8. Como **visitante** quiero ver rankings por categoría (Enduro, e-Enduro, DH) basados en votos.
9. *(stretch)* Como **usuario** quiero responder unas preguntas sobre mi estilo y recibir un análisis personalizado en lenguaje natural generado por IA a partir de los números calculados.
10. *(stretch)* Como **usuario** quiero comparar dos bicis lado a lado.

Cumple el reparto que pide el enunciado: negocio (1, 2, 4, 5, 7, 8), auth y roles (3, 6), pantallas principales (1, 2, 4).

---

## 6. Modelo de datos (borrador)

- **users**: id, email (único), password_hash, role (`USER`/`MODERATOR`), created_at.
- **bikes**: id, owner_id → users, brand, model, year, category (enum), suspension_type (enum), travel_declared_mm, shock_eye_to_eye_mm, shock_stroke_mm, wheel_size, chainring_teeth, photo_url, status (`PRIVATE` / `PENDING` / `PUBLIC` / `REJECTED`), created_at.
- **bikes.linkage_points** (jsonb): puntos etiquetados con coordenadas normalizadas + datos de calibración. Es jsonb porque el conjunto de puntos varía según la topología.
- **kinematics_results**: bike_id (1:1), curves (jsonb: arrays de las curvas), descriptors (jsonb: progresión %, pendientes por tramos, etc.), engine_version, computed_at. Se calcula una vez al crear/editar y se persiste (no recalcular en cada GET). `engine_version` permite recalcular todo si el motor mejora.
- **votes**: user_id + bike_id (PK compuesta / unique) → relación **N:M**, created_at.
- *(stretch)* **comments**.

Relaciones que pide el enunciado: 1:N (users→bikes, bikes→results) y N:M (votes). Geometría de cuadro: si entra, columna jsonb opcional en bikes; no diseñar tablas para funcionalidades futuras (el enunciado lo dice explícitamente).

---

## 7. Motor de cinemática: enfoque técnico

**Pipeline:** puntos normalizados → calibración a mm → solver según topología → barrido del recorrido → curvas → descriptores → clasificaciones por reglas.

- **Entrada del motor** (ya en mm, tras calibrar): puntos etiquetados según topología (pivote principal, anclajes de amortiguador, eje trasero, eje de pedalier; en Horst: pivote Horst, unión bieleta-tirante, pivote de bieleta), carrera del amortiguador, plato, rueda.
- **Barrido:** discretizar la carrera del amortiguador en ~100 pasos; en cada paso resolver la posición del eje trasero.
- **Monopivote:** el eje gira en arco alrededor del pivote principal. Relación ángulo ↔ longitud de amortiguador por triángulos (ley del coseno). Salidas directas: trayectoria del eje, recorrido vertical, leverage ratio por paso (Δrecorrido/Δcarrera).
- **4 barras:** en cada paso, posicionar los eslabones por **intersección de circunferencias** (las longitudes de barra son constantes). Geometría cerrada, sin optimizador numérico: el proyecto de referencia usa scipy porque es Python; en Java la vía geométrica es más simple, más rápida y más fácil de testear.
- **Pedal kickback:** derivado del crecimiento de la distancia pedalier–eje + dientes de plato/piñón. Documentar los supuestos simplificadores.
- **Descriptores de curva:** LR inicial y final, % de progresión, pendiente por tercios (inicio/medio/final), detección de inflexiones, recorrido calculado (para el sanity check contra el declarado).
- **Validación:** fixtures de 2–3 bicis contrastadas con Linkage, tolerancia ±2–3%, como tests automáticos. La Canyon Strive:ON ya analizada a fondo es candidata ideal de primer fixture.
- **Anti-squat / anti-rise: fuera de v1** (requieren asumir centro de gravedad y línea de cadena; el proyecto de referencia pide `cog_height` justo por esto). Al README como trabajo futuro.

---

## 8. Contrato API inicial (borrador)

- `POST /api/auth/register` · `POST /api/auth/login` → JWT
- `GET /api/bikes?category=&sort=votes&page=` (públicas y aprobadas) · `GET /api/bikes/{id}`
- `GET /api/my-bikes` (auth)
- `POST /api/bikes` (auth; metadatos) · `POST /api/bikes/{id}/photo` (multipart → Cloudinary)
- `PUT /api/bikes/{id}/linkage` (puntos + calibración → dispara cálculo → devuelve curvas y descriptores)
- `POST /api/bikes/{id}/publish` (auth, dueño) → estado PENDING
- `GET /api/moderation/pending` · `POST /api/moderation/{id}/approve|reject` (solo MODERATOR)
- `PUT /api/bikes/{id}/vote` · `DELETE /api/bikes/{id}/vote` (auth)
- `GET /api/rankings?category=`
- *(stretch)* `POST /api/bikes/{id}/analysis` (respuestas del cuestionario → texto IA)

Respuestas de error consistentes (handler global): `{timestamp, status, error, message, path}`.

---

## 9. Riesgos y plan de recorte

**Orden de sacrificio si no llega el tiempo** (de primero a último en caer):
1. Capa IA narrativa → queda como trabajo futuro (los números y clasificaciones por reglas ya se muestran).
2. Comparador de bicis.
3. Solver 4 barras → v1 solo monopivote (sigue habiendo motor real que enseñar).
4. Rankings elaborados → orden simple por número de votos.

**Innegociable** (son los mínimos del enunciado): auth + 2 roles, un motor completo funcionando, moderación, tests con 60%, Swagger, Docker, migraciones, despliegue, README, flujo Git ordenado.

**Riesgos concretos y mitigación:**
- Calibración/marcado impreciso → sanity check de recorrido (1.2) + foto de ejemplo buena en el onboarding.
- Solver 4 barras se atasca → por eso monopivote primero y 4 barras en F4.
- Integración front-back al final → por eso rebanadas verticales y despliegue en F0.
- Fotos desaparecen al redesplegar → Cloudinary desde el principio.
- Coste/cuota de la API de IA → clave por variable de entorno, modo mock en desarrollo, y es la primera en caer del alcance.

---

## 10. Cosas que se suelen olvidar (checklist)

- [ ] `.env.example` con todas las variables (BD, JWT secret, Cloudinary, API IA) y `.env` en `.gitignore`.
- [ ] **CORS** configurado para el origen del front en dev (localhost:5173) y en producción.
- [ ] **Seed de datos por migración o runner: al menos el usuario MODERATOR inicial** (sin él no se puede aprobar nada en la demo) y 2–3 bicis de muestra para que el catálogo no salga vacío.
- [ ] Handler global de excepciones + `@Valid` en todos los DTOs de entrada.
- [ ] Límite de tamaño de subida (`spring.servlet.multipart.max-file-size`) y validación de tipo de imagen.
- [ ] JaCoCo configurado con umbral del 60% para que la CI falle si baja.
- [ ] Paginación en catálogo y rankings.
- [ ] Índices en BD para las consultas de ranking (category, status, votos).
- [ ] Timestamps en UTC.
- [ ] Instrucción en el README de cómo autenticarse desde Swagger UI (botón Authorize + token) — lo pide el enunciado.
- [ ] Actuator health check para el despliegue.
- [ ] El enunciado del proyecto guardado en `/docs` del repo, para consultarlo (tú o la IA) durante todo el desarrollo.

---

## 11. Notas del análisis de BiKinematics / bikinematicsolver (referencia, no guía)

**Qué es:** app de escritorio en Python (Kivy) + librería `bikinematicsolver` separada, del mismo autor (mark-bak). Semi-abandonada (último movimiento 2024; la web en Heroku ya no existe).

**Ideas que valen la pena (adaptadas, no copiadas):**
- **El solver como librería aparte con sus propios tests** (`test_solver.py`, `test_geometry.py`, `test_bike.py`) — equivale a nuestro motor como dominio puro testeado. Confirma el enfoque.
- **Modelo de entrada:** puntos etiquetados con un **tipo** (`ground` = fijo al cuadro, `linkage` = móvil, `rear_wheel`, `bottom_bracket`…) + coordenadas **en píxeles de la imagen de fondo** + parámetros aparte (dientes de plato/piñón, wheelbase para escalar, tamaño de rueda). Valida nuestro diseño de captura sobre foto + calibración por medida conocida.
- Su parámetro `cog_height` confirma que anti-squat exige asumir el centro de gravedad → bien dejado fuera de v1.
- Su alcance real (mono + 4 barras ≈ 80% del mercado; high pivot inacabado) confirma nuestro recorte de v1.

**Qué NO imitar:**
- Su solver es un **grafo genérico de puntos+barras** resuelto numéricamente (numpy/scipy). Elegante, pero más difícil de implementar, testear y explicar. Para este proyecto: **solvers explícitos por topología** (más simples, más defendibles en una entrevista, y la UI guiada ya obliga a elegir topología). La puerta a generalizar queda abierta.
- Artefactos de build **comiteados** en el repo (`build/`, `dist/`, `egg-info/`) y documentación casi nula. Justo lo contrario de lo que pide el enunciado: `.gitignore` correcto y README cuidado.
- Monolito de escritorio con la lógica pegada a la GUI vs. nuestra separación API / front.
