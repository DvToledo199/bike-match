# Seguimiento de la auditoría del 6 de septiembre

Cierre documental: 7 de septiembre de 2026. El [JSON original](2026-09-06-informe.json)
conserva íntegro el informe previo; es una fotografía histórica de `2f0feb6`, no el
estado actual. Este documento distingue correcciones, pruebas y límites pendientes.

## Correcciones implementadas

| Bloque | Cambios | Seguimiento |
|---|---|---|
| Validación Java/API | Seis tipos únicos; coordenadas obligatorias y finitas; medidas acotadas; dientes enteros; geometría, divisores y resultados comprobados. Error 400 explicado en lugar de 200 con NaN o 403 opaco. | #33, PR #71 |
| Orientación | Eje trasero inicial como origen; normalizar fotos mirando a ambos lados; rechazar ejes ambiguos y fotos excesivamente inclinadas. | #70, PR #77 |
| Peticiones y foto | Rechazar JSON/contratos incompletos; timeout de 15 s, cancelar al volver o desmontar, ignorar respuestas antiguas; conservar nombre de foto; descartar decodificaciones antiguas y liberar sus URLs. | #73, PR #78 |
| Gráficas e interpretación | Área útil cuadrada y dominios iguales; desplazamiento relativo y arriba positivo; segmentos lineales conservan cambios de dirección. Banda regresiva para progresión negativa; no confundir liberación de cadena con kickback positivo. Piñón y sag explicados como realmente se usan. | #72, PR #79 |
| Accesibilidad | Crear los seis puntos con teclado; deshacer elimina cruz y datos inmediatamente. Foco opaco de alto contraste, cambio de foco al pasar de etapa, anuncio de progreso y unidades accesibles. | #74, PR #80 |
| Seguridad/configuración | Parches de dependencias sin cambiar Spring Boot; PostgreSQL y API en loopback; carga explícita de .env; permisos de CI de solo lectura. | #75, PR #81 |
| Documentación | README útiles, contrato/orientación/límites actualizados, eliminación de la exigencia de cinco clics, limitaciones y hoja de ruta coherentes. | #76 |

En Java se aplicó la skill **121-java-object-oriented-design** para mantener controles
de entrada y contratos de error explícitos, con una utilidad pequeña `CurveChecks`
compartida por los cálculos. Compilación previa y verificación completa posterior;
sin cambiar la versión Spring Boot 3.5.16 ni introducir funcionalidades futuras.

## Pruebas y evidencia

- `./mvnw clean verify`: **64 tests**, sin fallos. Localmente se verificó con
  `-Dspring.flyway.enabled=false -Dspring.datasource.hikari.read-only=true` para no
  alterar la base existente; GitHub Actions prueba Flyway normalmente sobre una BD nueva.
- `npm test`: **22 tests**, sin fallos. `npm run lint` y `npm run build`: correctos.
  Se añadió CI frontend. Los PR se integraron después de sus comprobaciones verdes.
- Backend real actualizado: fixture y reflejo devuelven 200; recorrido
  **164.35362427131602 mm**, diferencia entre reflejos de unos **1.7e-13 mm**
  (redondeo numérico). Dientes fraccionarios y anclajes coincidentes devuelven 400.
- Navegador: área útil del axle path **464×464 px** en escritorio y **214×214 px**
  en viewport de 375 px; mismo número de píxeles por mm en ambos ejes, sin desborde
  horizontal. La vista temporal se cerró y el tamaño de navegador se restauró.
- `npm audit`, incluyendo desarrollo: **0 vulnerabilidades conocidas**.
- Consulta de las **74 dependencias Maven de runtime** a OSV tras actualizar:
  **0 avisos devueltos**. Es una consulta fechada, no una garantía de ausencia de fallos.
- PostgreSQL recreó únicamente su contenedor: el volumen `docker_postgres_data`
  se conserva; puerto publicado en `127.0.0.1:5432`. API en `127.0.0.1:8080`, health UP.
- No se tocaron secretos ni se borraron datos de usuario. El `AGENTS.md` local
  preexistente, no versionado, se dejó fuera de los commits.

## Parches de dependencias

| Componente | Antes | Después |
|---|---|---|
| Jackson BOM | 2.21.4 | 2.21.6 |
| Tomcat | 10.1.55 | 10.1.59 |
| Log4j | 2.24.3 | 2.25.5 |
| PostgreSQL JDBC | 42.7.11 | 42.7.13 |
| PostCSS | 8.5.16 | 8.5.28 |
| nanoid | 3.3.15 | 3.3.18 |

Se contrastaron las versiones con Maven Central/npm y los avisos oficiales:
[Jackson](https://github.com/FasterXML/jackson-databind/security/advisories/GHSA-5jmj-h7xm-6q6v),
[Tomcat](https://tomcat.apache.org/security-10.html),
[Log4j](https://github.com/advisories/GHSA-qv9r-c865-cp47),
[PostgreSQL JDBC](https://github.com/advisories/GHSA-j92g-9f8w-j867).
La carga de archivos de configuración sigue el mecanismo de
[Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html).

## Lo que no se debe dar por cerrado

1. **#54, foto real contra referencia:** falta completar y registrar la prueba manual
   con David. Los tests y la vista del fixture no prueban la precisión de su marcado.
2. **Despliegue público y aprobación del mentor:** no verificados. El pendiente de
   despliegue de #20 se mantiene en la nueva **#82**; no se ha publicado nada.
3. **#1 frente a #5:** decidir si el acceso usará email, username o ambos antes de
   programar registro. No se ha inventado una decisión de producto ni cerrado esas historias.
4. **Sprints posteriores:** JWT/persistencia/catálogo, cobertura JaCoCo ≥60%, IA,
   cuatro barras y kickback cog-aware. Las etiquetas actuales preparan la IA; no la sustituyen.
5. **Precisión física:** foto nivelada, lateral y extendida; sin corrección de
   perspectiva. Kickback simplificado y sensibilidad al marcado; tolerancia de
   referencia de kickback ±30%, no la ±3% del leverage. La calibración sí puede
   afectar el leverage porque la carrera en mm se introduce por separado.
6. **Rendimiento y accesibilidad:** queda el aviso de Vite por un chunk >500 kB sin
   comprimir (~190 kB gzip de JS). Se retiró la carga diferida problemática, no el
   aviso. No se afirma una certificación formal WCAG de toda la aplicación.
7. **Antes de producción:** TLS, secretos reales, CORS correcto y límites de tamaño
   y tasa de peticiones, además de nueva revisión de dependencias. Cero avisos
   conocidos no significa riesgo cero.
