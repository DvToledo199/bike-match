# Hoja de ruta por sprints — mini-tareas ordenadas

> **Estado final, 27 de septiembre de 2026: MVP entregado y desplegado.** Esta hoja se
> conserva como registro de cómo se trabajó. Lo que no se hizo está en «Después del MVP»,
> con su issue. La comparación entre lo planificado y lo real está en la sección 12 de
> [`plan-trabajo-mvp.md`](plan-trabajo-mvp.md).

**Regla de oro contra la saturación:** solo existe el sprint actual. No mires los demás. Haz la primera tarea sin marcar, márcala, pasa a la siguiente. Si una tarea te lleva más de un día, pártela en dos.

**Si un sprint se retrasa 2–3 días:** se come el colchón del Sprint 4, nunca los Sprints 2 y 3.

---

## Sprint 0 — Decidir y montar el tablero (días 1–3)

**Objetivo:** historias de usuario validadas + esqueleto desplegado en una URL pública. Aquí NO se escribe lógica de negocio.

### Decisiones (una tarde, con timebox)
- [x] Nombre provisional del repo (máx. 30 min; se puede renombrar luego)
- [x] Idioma: código/API/BD/commits en inglés; UI en inglés con todos los textos en un archivo i18n desde el día 1
- [x] Confirmar stack exacto: versión de Spring Boot que usa el curso, Java 21, React + Vite

### Planificación
- [x] Repasar las 8+2 historias de usuario del plan, retocarlas a tu gusto y pasarlas al formato del curso (issues #2–#11)
- [ ] Validar las historias antes de programar, como exige el enunciado

### Repo y tablero
- [x] Crear el repo (carpetas: `/backend`, `/frontend`, `/docs`, `/docker`)
- [x] Subir a `/docs` el enunciado y el plan de trabajo
- [x] Crear GitHub Project (columnas: Backlog / Ready / In progress / In review / Done) y una issue por historia, priorizadas

### Esqueletos (primer PR: aquí empieza la evidencia del flujo git)
- [x] Generar el backend con Spring Initializr (web, validation, jpa, security, postgresql, flyway) y que arranque
- [x] docker-compose con Postgres + la app conecta + migración V1 (tabla users vacía vale)
- [x] Crear el front con Vite: una página que llama a `GET /api/health` del back y muestra la respuesta (aquí se resuelve CORS)
- [x] GitHub Actions: workflow que ejecuta `mvn verify` en cada push
- [x] Desplegar el back y el front: se hizo al final, en Render y Neon (#82)
- [x] Todo esto en una branch, con su issue y su PR mergeada (#20, #21)

**Hecho cuando:** las historias están validadas y una URL pública responde.

---

## Sprint 1 — El motor y la pantalla de marcado (semana 1)

**Objetivo:** demo de punta a punta con UNA bici real: foto → marcar puntos → curvas correctas. (La parte difícil, primero.)

- [x] Elegir la bici de validación (un monopivote sencillo) y sacar sus curvas de BikeChecker → esa es la "verdad" contra la que testear
- [x] Modelo de entrada del motor en Java: punto 2D, tipos de punto, parámetros (carrera, plato, rueda) — dominio puro, sin Spring
- [x] Solver monopivote: posición del eje en cada paso del barrido (arco alrededor del pivote) + tests unitarios con casos geométricos simples
- [x] Curva de leverage + trayectoria del eje + recorrido total + test contra el fixture de BikeChecker (tolerancia ±3%)
- [x] Pedal kickback simplificado + test
- [x] Descriptores desde el punto de sag (progresión útil, LR en sag, retroceso del eje…) + clasificación por reglas ("progresiva/lineal…") + tests
- [x] Sanity check: recorrido calculado vs. declarado → aviso si difiere >10%
- [x] Endpoint `POST /api/kinematics/preview` (sin BD): recibe puntos + calibración + parámetros, devuelve curvas y descriptores + test de aceptación
- [x] Front (IA): pantalla de subir foto, marcarla con puntos guiados según el diseño del sistema de suspensión, e inputs de calibración (eye-to-eye)
- [x] Front (IA): gráficas de las curvas que devuelve el endpoint
- [x] Demo completa con tu foto real (Orange Stage 6, issue #54)

**Hecho cuando:** una foto se convierte en curvas que cuadran con la referencia externa.

---

## Sprint 2 — De demo a producto (semana 2)

**Objetivo:** usuarios reales se registran, guardan sus bicis y entienden el análisis.

- [x] Motor de referencia: ruedas 29/mullet/27,5 y anti-squat/anti-rise (#108)
- [x] Kickback cog-aware con tests analíticos; sin declarar paridad externa (#31)
- [x] Selector de ruedas, cinco gráficas y condiciones/versiones API (#109)

- [x] Confirmar email de acceso + username público y consolidar el contrato de identidad
- [x] Migraciones: users, bikes, kinematics_results (#92, #118)
- [x] Antes de cerrar resultados de #6: contrato de interpretación, capacidades y versiones (#103).
  Ver [`contrato-interpretacion-cinematica.md`](contrato-interpretacion-cinematica.md).
- [x] Registro + login con JWT, filtro stateless, respuestas 401/403 y tests (#94)
- [x] Roles USER/MODERATOR y moderador inicial opcional desde `.env` con hash BCrypt (#94).
  El esquema sigue en Flyway; la cuenta no va en una migración con contraseña. Bicis de
  muestra quedan para la issue de creación/catálogo.
- [x] Crear una bici privada y guardar sus metadatos (#122)
- [x] Subir su foto a Cloudinary y guardar la URL con control de propietario (#124)
- [x] Guardar los puntos naturales de la foto y persistir atómicamente el resultado del motor
- [x] "Mis bicis" (privadas) y detalle de bici con gráficas y números
  - [x] API `GET /api/my-bikes` con resúmenes privados del propietario (#130)
  - [x] Cliente frontend preparado para adjuntar el JWT y consultar esa API (#142)
  - [x] API `GET /api/bikes/{id}` con reglas de visibilidad y resultado persistido (#148)
- [x] Catálogo público (solo aprobadas) con filtro por categoría y paginación
  - [x] API pública paginada y filtrable por categoría (#152)
  - [x] Pantalla frontend con tarjetas, filtro, paginación y acceso al detalle (#154)
- [x] Swagger al día, con la autenticación documentada (#176)
- [x] Front (IA): pantallas de registro/login, mis bicis, catálogo y detalle
  - [x] Registro/login y sesión frontend (#138, #140, #142)
  - [x] Mis bicis y solicitud de publicación (#144, #146)
  - [x] Ficha de bici con foto, metadatos y gráficas guardadas (#150)
- [x] Servicio de explicación básica sin cuestionario: reutilización, límites, privacidad y recuperación de errores (#104)
  - [x] Proveedor determinista por reglas como modo local y fallback
  - [x] Adaptador opcional de Gemini sin dependencia nueva, con clave solo en servidor
  - [x] Adaptador de Gemini sobre Spring AI en lugar del cliente HTTP escrito a mano (#258)
  - [x] Caché versionada, permisos de lectura/generación y límite de peticiones MVP
- [x] Texto sencillo junto a las gráficas del detalle (#105)
- [x] Guardado desde el asistente sin perder el análisis al iniciar sesión (#161)
- [x] Prueba real: subida a Cloudinary, explicación por reglas/Gemini y comprensión del texto (#170)
  - [x] Bloques 0–4: configuración de Cloudinary, cuenta, guardado, marcado y moderación
  - [x] Bloque 5: explicación con Gemini
  - [x] Tests con base de datos propia (#178)
  - [x] Backend: punto final del resumen, fecha de solicitud en moderación y log de fallos de Cloudinary (#179)
  - [x] Frontend: foto al guardar y sesión iniciada tras registrarse (#180)
  - [x] Borrar una bici: el dueño (#181, #182) y el moderador, con aviso al dueño (#183, #184)
  - [x] README y limitaciones: Cloudinary, moderador inicial y traducción automática (#185)
  - [x] Interfaz y explicación en español, con selector de idioma (#210)
- [x] Navegación de inicio, controles de marcado y formulario compacto (#158–#160)
- [x] Portada comunitaria centrada en fotos reales del catálogo público (#169)

**Hecho cuando:** en la URL pública alguien se registra, crea una bici y la ve en
"mis bicis" con una explicación comprensible. La indisponibilidad del proveedor de
IA no bloquea los resultados; su integración se valida con ejemplos reales.

---

## Sprint 3 — Comunidad y mínimos del enunciado (semana 3)

**Objetivo:** cumplir TODOS los obligatorios. Al acabar este sprint podrías entregar y aprobar.

- [x] Publicar bici (PRIVATE → PENDING) desde backend (#132)
- [x] Cola de moderación + aprobar/rechazar (solo MODERATOR) + tests de autorización (un USER recibe 403) (#134, #135)
- [x] Front: botón publicar y paneles de moderación y administración (#146, #238, #243)
- [x] Retirar una bici con motivo y aviso al dueño; rol de administrador para gestionar moderadores
- [x] Cobertura ≥60% con JaCoCo (rellenar huecos con tests útiles, no de relleno): 95,0 % de líneas y 72,9 % de ramas; el build falla por debajo del 60 %
- [x] README completo siguiendo punto por punto la lista del enunciado
- [x] Repasar el checklist "cosas que se olvidan" del plan (.env.example, CORS de producción, límites de subida…)
- [x] Despliegue final verificado, Swagger probado en producción

**Hecho cuando:** revisas los requisitos del enunciado uno a uno y todos están en verde.

---

## Sprint 4 — Diferenciación (semana 4 o lo que quede)

En este orden; lo que no salga, al README como trabajo futuro (queda bien, es visión):

- [x] Solver de 4 barras (Horst link), en tres variantes: amortiguador en la bieleta, con extensión y empujado por los tirantes (#19, #288)
- [x] Traducción de la UI y de la explicación al español (#210)
- [x] Extras: health check propio, cobertura en CI y API despierta con un flujo programado

## Después del MVP

Lo que no entró, en el orden en que se recortó:

- [ ] Personalización con cuestionario opcional de estilo/peso (#10, segundo bloque)
- [ ] Comparador de 2 a 4 bicis, con comparación por IA (#11)
- [ ] Votos (uno por usuario y bici) y rankings por categoría (#9, #4)
- [ ] Comentarios en bicicletas públicas (#86)
- [ ] Perfil público y rango basado en contribuciones aprobadas (#87)
- [ ] Validación externa de las cuatro barras y del kickback (#222, #237, #31)
- [ ] Recuperar la contraseña y confirmar el correo (#186, #257)
- [ ] Derechos de las fotos publicadas: decisión y asesoramiento (#167)
- [ ] Buscar y paginar cuentas en el panel de administración (#254)
- [ ] Explicación en el idioma de quien la lee (#264)
- [ ] Mover la foto con dos dedos en el trackpad sin perder el zoom con la rueda (#300)

Modelo de producto:

- [ ] Validar la regla gratuita de dos bicicletas privadas como máximo
- [ ] Diseñar planes y permisos antes de integrar un proveedor de pagos
- [ ] Mantener ilimitadas las bicicletas públicas aprobadas para favorecer el catálogo
- [ ] Chat sobre el análisis, privacidad del historial, cuotas y posible modalidad de pago por decidir (#106)

---

## Ritual de cada día (10 min)

1. Abre el GitHub Project, mira solo la columna "In progress" / "Ready" del sprint actual.
2. Una tarea = una branch = una issue = un PR pequeño.
3. Al acabar el día: marca lo hecho, apunta en una línea dónde lo dejaste (te lo agradecerás mañana).
