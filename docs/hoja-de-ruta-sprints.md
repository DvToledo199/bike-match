# Hoja de ruta por sprints — mini-tareas ordenadas

**Regla de oro contra la saturación:** solo existe el sprint actual. No mires los demás. Haz la primera tarea sin marcar, márcala, pasa a la siguiente. Si una tarea te lleva más de un día, pártela en dos.

**Si un sprint se retrasa 2–3 días:** se come el colchón del Sprint 4, nunca los Sprints 2 y 3.

---

## Sprint 0 — Decidir y montar el tablero (días 1–3)

**Objetivo:** historias validadas por el mentor + esqueleto desplegado en una URL pública. Aquí NO se escribe lógica de negocio.

### Decisiones (una tarde, con timebox)
- [ ] Nombre provisional del repo (máx. 30 min; se puede renombrar luego)
- [ ] Idioma: código/API/BD/commits en inglés; UI en inglés con todos los textos en un archivo i18n desde el día 1
- [ ] Confirmar stack exacto: versión de Spring Boot que usa el curso, Java 21, React + Vite

### Planificación
- [ ] Repasar las 8+2 historias de usuario del plan, retocarlas a tu gusto y pasarlas al formato del curso
- [ ] Enviárselas al mentor y conseguir el OK (el enunciado lo exige antes de programar)

### Repo y tablero
- [ ] Crear el repo (carpetas: `/backend`, `/frontend`, `/docs`, `/docker`)
- [ ] Subir a `/docs` el enunciado y el plan de trabajo
- [ ] Crear GitHub Project (columnas: Backlog / To do / In progress / Done) y una issue por historia, priorizadas

### Esqueletos (primer PR: aquí empieza la evidencia del flujo git)
- [ ] Generar el backend con Spring Initializr (web, validation, jpa, security, postgresql, flyway) y que arranque
- [ ] docker-compose con Postgres + la app conecta + migración V1 (tabla users vacía vale)
- [ ] Crear el front con Vite: una página que llama a `GET /api/health` del back y muestra la respuesta (aquí se resuelve CORS)
- [ ] GitHub Actions: workflow que ejecuta `mvn verify` en cada push
- [ ] Desplegar el back (Render/Railway) y el front (Vercel/Netlify) tal cual están
- [ ] Todo esto en una branch, con su issue y su PR mergeada

**Hecho cuando:** el mentor ha validado las historias y una URL pública responde.

---

## Sprint 1 — El motor y la pantalla de marcado (semana 1)

**Objetivo:** demo de punta a punta con UNA bici real: foto → marcar puntos → curvas correctas. (La "parte difícil primero" que dijo el profe.)

- [ ] Elegir la bici de validación (un monopivote sencillo) y sacar sus curvas en Linkage → esa es la "verdad" contra la que testear
- [ ] Modelo de entrada del motor en Java: punto 2D, tipos de punto, parámetros (carrera, plato, rueda) — dominio puro, sin Spring
- [ ] Solver monopivote: posición del eje en cada paso del barrido (arco alrededor del pivote) + tests unitarios con casos geométricos simples
- [ ] Curva de leverage + trayectoria del eje + recorrido total + test contra el fixture de Linkage (tolerancia ±3%)
- [ ] Pedal kickback simplificado + test
- [ ] Descriptores (progresión %, pendiente por tercios) + clasificación por reglas ("progresiva/lineal…") + tests
- [ ] Sanity check: recorrido calculado vs. declarado → aviso si difiere >10%
- [ ] Endpoint `POST /api/kinematics/preview` (sin BD): recibe puntos + calibración + parámetros, devuelve curvas y descriptores + test de aceptación
- [ ] Front (IA): pantalla de subir foto, marcarla con puntos guiados según tipo de suspensión, e inputs de calibración (eye-to-eye)
- [ ] Front (IA): gráficas de las curvas que devuelve el endpoint
- [ ] Demo completa con tu foto real

**Hecho cuando:** le enseñas al mentor una foto convertida en curvas que cuadran con Linkage.

---

## Sprint 2 — De demo a producto (semana 2)

**Objetivo:** usuarios reales se registran y guardan sus bicis.

- [ ] Migraciones: users, bikes, kinematics_results
- [ ] Registro + login con JWT (Spring Security 6) + tests
- [ ] Roles USER/MODERATOR + seed por migración del moderador (tu cuenta) y 2–3 bicis de muestra
- [ ] Crear bici: metadatos + foto a Cloudinary + guardar puntos + persistir resultados del motor
- [ ] "Mis bicis" (privadas) y detalle de bici con gráficas y números
- [ ] Catálogo público (solo aprobadas) con filtro por categoría y paginación
- [ ] Swagger al día, con la autenticación documentada
- [ ] Front (IA): pantallas de registro/login, mis bicis, catálogo y detalle

**Hecho cuando:** en la URL pública alguien se registra, crea una bici y la ve en "mis bicis".

---

## Sprint 3 — Comunidad y mínimos del enunciado (semana 3)

**Objetivo:** cumplir TODOS los obligatorios. Al acabar este sprint podrías entregar y aprobar.

- [ ] Publicar bici (PRIVATE → PENDING)
- [ ] Cola de moderación + aprobar/rechazar (solo MODERATOR) + tests de autorización (un USER recibe 403)
- [ ] Votos (uno por usuario y bici) + rankings por categoría
- [ ] Front (IA): botón publicar, vista de moderación, votar, página de rankings
- [ ] Cobertura ≥60% con JaCoCo (rellenar huecos con tests útiles, no de relleno)
- [ ] README completo siguiendo punto por punto la lista del enunciado
- [ ] Repasar el checklist "cosas que se olvidan" del plan (.env.example, CORS de producción, límites de subida…)
- [ ] Despliegue final verificado, Swagger probado en producción

**Hecho cuando:** revisas los requisitos del enunciado uno a uno y todos están en verde.

---

## Sprint 4 — Diferenciación (semana 4 o lo que quede)

En este orden; lo que no salga, al README como trabajo futuro (queda bien, es visión):

- [ ] Solver de 4 barras (Horst link) + fixture de Linkage
- [ ] Capa IA con Spring AI: cuestionario de estilo + análisis narrado a partir de los números del motor (con modo mock en desarrollo)
- [ ] Traducción de la UI al español (archivo i18n → tarea de una tarde)
- [ ] Comparador de dos bicis
- [ ] Extras: caché en rankings, actuator/health, mejoras de CI/CD

---

## Ritual de cada día (10 min)

1. Abre el GitHub Project, mira solo la columna "In progress" / "To do" del sprint actual.
2. Una tarea = una branch = una issue = un PR pequeño.
3. Al acabar el día: marca lo hecho, apunta en una línea dónde lo dejaste (te lo agradecerás mañana).
