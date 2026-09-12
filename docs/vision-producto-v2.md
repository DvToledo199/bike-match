# Visión de producto V2 — comunidad de bicicletas analizadas

> Decisiones de producto acordadas el 7 de septiembre de 2026. Este documento
> orienta la evolución posterior a la V1 técnica. Los detalles que aún aparecen
> como propuesta se cerrarán antes de programar la tarea correspondiente.

## Visión

BikeMatch quiere combinar dos ideas:

- la utilidad técnica de Linkage/BikeChecker para analizar una suspensión;
- una comunidad abierta, fácil de explorar y con perfiles y publicaciones, tomando
  MakerWorld como referencia de navegación y presentación, no como producto que copiar.

El resultado buscado es un catálogo vivo de bicicletas ya analizadas. Una persona
puede consultar una bici sin repetir el marcado, y la comunidad puede valorar,
comentar y mejorar el conocimiento disponible.

La explicación comprensible de la cinemática es parte central del producto: también
debe servir a quien empieza y no sabe interpretar las gráficas. La decisión del
9 de septiembre separa un resumen básico sin cuestionario, una personalización
posterior y un posible chat futuro. Alcance y tareas en
[`plan-ia-explicacion.md`](plan-ia-explicacion.md), épica #10.

## Punto de partida: V1 terminada

La V1 local ya permite subir una foto, marcar una monopivote, introducir parámetros,
llamar al motor Java y mostrar curvas y descriptores. La prueba manual con una Orange
Stage 6 29'' 2020 quedó validada contra Linkage Design en la issue #54.

La V1 no tiene todavía cuentas, persistencia de bicicletas, catálogo ni publicación.
El endpoint de preview calcula, pero no guarda datos ni la foto.

## Experiencia de producto V2

### Ficha de bicicleta: orden acordado el 10/09/2026

Desde **Mis bicis → bici X**, o desde el catálogo si es pública, se abre la misma
ficha de detalle (#3), con este orden:

1. Ruta de navegación y título de la bicicleta.
2. Foto subida, directamente debajo del título.
3. Ficha técnica: datos de la bici y condiciones del análisis.
4. Gráficas disponibles para su versión de motor (incluidas anti-squat y anti-rise
   de referencia cuando estén calculadas), con inicio/final legibles, no tablas masivas.
5. Explicación humana general y, cuando se implemente #104/#105, texto de IA.

Se presenta todo en esa pantalla: no pedir otra acción para mostrar resultados ya
calculados. Las curvas no esperan a la IA; su carga, error o indisponibilidad afectan
solo al bloque de texto. El usuario debe poder distinguir una explicación fija de
una generada por IA. La foto y el texto respetan los mismos permisos que la bici.

La eliminación del botón inerte `View results` se entrega ahora en #113; esta ficha,
el guardado de la foto, Mis bicis y la IA son trabajo futuro de #3/#6/#103–#105,
no funciones que ya existan por documentarlas. La foto temporal del preview no basta
para recuperarla al abrir una bicicleta guardada.

### Visitante

- Puede explorar el catálogo de bicicletas públicas sin iniciar sesión.
- Puede filtrar por categoría y abrir el detalle con foto, datos, curvas y explicación.
- Puede probar el analizador; para guardar o participar necesita una cuenta.

### Usuario registrado

- Tiene un perfil público con username, rango y bicicletas publicadas.
- Puede crear análisis y decidir expresamente si conserva una bici privada o solicita
  publicarla.
- Puede votar o valorar y comentar bicicletas públicas.
- Puede gestionar sus propias bicicletas y ver el estado de moderación.

### Moderador

- Revisa las bicicletas enviadas a publicación.
- Puede aprobarlas o rechazarlas para mantener la calidad del catálogo.
- El rango de comunidad nunca concede permisos de moderador automáticamente.

## Identidad de usuario propuesta

Para resolver el solapamiento actual entre las issues #1 y #5:

- **email único y privado** para iniciar sesión y recuperar la cuenta;
- **username único y público** para el perfil y los comentarios;
- contraseña guardada únicamente como hash BCrypt;
- roles de seguridad separados: `USER` y `MODERATOR`.

Es el equivalente a entrar en una plataforma con un dato privado, mientras los demás
ven un alias público. Esta propuesta debe confirmarse antes de la primera migración de
Sprint 2 y quedar unificada en una sola historia de autenticación.

## Visibilidad y publicación de bicicletas

Estados previstos:

1. `PRIVATE`: solo puede verla su propietario.
2. `PENDING`: el propietario ha solicitado publicarla y espera moderación.
3. `PUBLIC`: está aprobada y aparece en el catálogo.
4. `REJECTED`: no se publica y el propietario recibe el estado.

Nunca se convierte una bicicleta privada en pública sin una acción clara de su dueño.
La publicación pasa por moderación porque una fotografía mal marcada podría generar
datos engañosos para toda la comunidad.

## Comunidad, reputación y motivación

La primera comunidad se construye con piezas pequeñas:

- voto o valoración de bicicletas públicas;
- comentarios en el detalle de una bicicleta;
- perfil público con sus análisis aprobados;
- rankings de bicicletas por categoría;
- rango del usuario como reconocimiento visible.

El rango debe premiar contribuciones útiles, no la cantidad de subidas sin control.
La primera versión puede basarse principalmente en bicicletas aprobadas y votos
recibidos. Los nombres de rangos, umbrales y peso de comentarios se decidirán cuando
existan datos reales. El rango será motivacional, no una autorización de seguridad.

## Futuro modelo gratuito y de pago

Hipótesis de producto para una fase posterior al MVP del curso:

- una cuenta gratuita puede mantener como máximo **dos bicicletas privadas**;
- las bicicletas públicas aprobadas no consumen ese límite, para favorecer un catálogo vivo;
- al alcanzar el límite, el usuario puede publicar una bici con su consentimiento o
  contratar en el futuro una modalidad que amplíe las plazas privadas;
- no se implementarán pagos ni límites en Sprint 2: primero se valida que cuentas,
  guardado, publicación y comunidad funcionan correctamente;
- antes de programarlo hay que cerrar cómo cuentan los estados `PENDING` y `REJECTED`,
  además de borrados, cancelaciones y pérdida de una suscripción.

No se añadirá todavía un simple booleano `premium`: cuando llegue el momento se
diseñarán planes y permisos sin acoplar el dominio de bicicletas a un proveedor de pago.

## Orden de construcción del backend

David implementará el backend con acompañamiento, una tarea pequeña cada vez:

1. Cerrar el modelo de cuenta y consolidar #1 dentro de #5.
2. Migración versionada de `users`, entidad y repositorio.
3. Registro con validaciones, BCrypt y tests.
4. Login y JWT, seguido de rutas protegidas y tests 401/403.
5. Roles y usuario moderador inicial desde variables locales ignoradas por Git, con
   hash BCrypt. Flyway continúa siendo el dueño exclusivo del **esquema**; una cuenta
   con contraseña no se versiona como una migración SQL.
6. Migraciones de `bikes` y `kinematics_results`.
   Antes de cerrarlas, definir en #103 los datos y versiones que necesita la explicación.
7. Crear y guardar una bicicleta reutilizando el motor de la V1.
8. Reglas de propietario y visibilidad privada/pública.
9. Catálogo y detalle públicos.
10. Explicación básica de los resultados con IA (#104/#105), reutilizando el motor y
    el detalle, antes de las funciones sociales y sin esperar a cuatro barras.
11. Moderación, votos, comentarios, perfiles y rangos, cada uno en su issue.
12. Personalización por cuestionario (#10, Sprint 4) y chat futuro (#106). El posible
    pago del chat es una hipótesis pendiente; no se implementa en Sprint 2.

La ayuda seguirá este método: explicación breve del concepto, David plantea o escribe
el código, revisión conjunta, tests, commit, PR y siguiente issue. El frontend se adapta
después de que cada contrato del backend esté claro.

## Mapa de issues

Ya existen: #5 autenticación, #6 creación de bici, #7 privacidad/publicación, #2
catálogo, #3 detalle, #8 moderación, #9 votos y #4 rankings. La planificación añade
#86 para comentarios, #87 para perfil/rango y #88 para el límite privado de una futura
modalidad de pago. Esta última queda en el backlog comercial, fuera del Sprint 2.

La épica #10 incluye el resumen básico de Sprint 2 (#103 contrato, #104 servicio,
#105 pantalla) y conserva el cuestionario como ampliación posterior. El chat #106
queda en Backlog. Los detalles y dependencias están en `plan-ia-explicacion.md`.
