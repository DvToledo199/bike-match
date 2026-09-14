# Diseño y prueba del frontend — 14/09/2026

Dirección acordada: moderno, minimalista, verde contenido y contraste legible.
Referencia de producto: MakerWorld, por su acceso al catálogo y a la contribución,
sin copiar su identidad ni presentar funciones sociales que todavía no existen.

## Entregas pequeñas

| Issue | Alcance |
| --- | --- |
| #158 | Inicio con propuesta breve, CTA de análisis, catálogo, cuenta, logo de inicio e historial |
| #159 | Controles superpuestos al marcado, rueda, arrastre derecho y nuevo orden |
| #160 | Formulario compacto y términos ingleses revisados |
| #161 | Conectar preview → cuenta → guardado privado → ficha → explicación |
| #162 | Futuro: pinza y gestos móviles, pantalla completa y prueba en dispositivos |
| #163 | Futuro: sugerencias de transmisión, catálogo/ficha y evolución visual |

Cada entrega tiene su propia rama, commit y PR.

## Inicio y navegación (#158)

La portada presenta qué hace BikeMatch, una ilustración identificada como tal,
tres pasos y acceso al catálogo. El CTA «Analyze your bike» es visible en la
portada y en la cabecera. Login y registro siguen accesibles.

Se usan enlaces con fragmentos (#/, #/analyze, #/catalog, #/login, #/register,
#/my-bikes, #/bikes/:id). Atrás/Adelante funciona mediante historial del navegador,
sin añadir una dependencia. El logo vuelve al inicio desde todas las vistas.
El asistente permanece montado y oculto al navegar, conservando foto, puntos,
parámetros y resultados en memoria. Una recarga completa todavía descarta el
borrador: persistencia recuperable se reserva para trabajo futuro.

## Criterios de marcado

Orden: pivote principal → pedalier → amortiguador/cuadro → amortiguador/basculante
→ eje trasero → eje delantero. Son los mismos seis tipos de punto del contrato.
Los controles y la indicación del punto activo quedan fuera del SVG que se amplía.
La edición individual bajo la imagen conserva los otros puntos. Deshacer elimina
inmediatamente el punto y su cruz.

La rueda amplía alrededor del cursor y el arrastre derecho desplaza la foto.
Se mantienen botones y teclado como alternativa. En móvil se ofrece un layout
sin solapamientos; los gestos multitáctiles avanzados requieren #162.

## Terminología y formulario

Una sola sección con unidades y ayudas breves. Se distingue «Shock stroke»
(carrera del amortiguador) de «Rear wheel travel» (recorrido trasero).
«Shock eye-to-eye» indica distancia entre centros de los anclajes;
«Chainring teeth» es el plato delantero y «Largest rear sprocket» pide el piñón
mayor del cassette. El motor usa exactamente ese número para la marcha calculada.
No se cambia el contrato genérico sprocketTeeth ni se afirma que el backend
compruebe qué piñón es el mayor. La interfaz no inventa dientes por disciplina.
El selector de ruedas y el sag se mantienen. Errores y advertencias que ayudan a
corregir una medición siguen visibles; se retiran explicaciones de implementación.

Fuentes de vocabulario consultadas:

- [RockShox: Eye to eye / Stroke](https://www.sram.com/en/service/models/rs-vivd-bse-c1).
- [Shimano: Number of largest sprocket teeth](https://productinfo.shimano.com/pdfs/product/archive/2023-2024_Compatibility_v030_en.pdf).

## Explicación y pruebas

El preview anónimo calcula curvas; no llama a Gemini. La explicación de #105
estaba conectada únicamente a fichas guardadas. Faltaba el puente de guardado en
el frontend, que se implementa en #161. La afirmación anterior de que aparecería
tras calcular el preview era incorrecta.

El proveedor por defecto es rules. Una respuesta identificada como RULES no es
una llamada a IA externa; Gemini requiere configuración privada en el backend.
La prueba con datos reales debe distinguir: cálculo, guardado con Cloudinary,
texto por reglas, llamada externa a Gemini y comprensión del texto con David.
No dar por probadas las dos últimas solo porque pasen los tests automáticos.

Para desarrollo, abrir http://localhost:5173: el backend permite ese origen.
Abrir 127.0.0.1:5173 con la configuración actual provoca rechazo CORS. No se
relaja CORS globalmente. Una prueba que requiera ambos orígenes debe configurarlos
explícitamente en CORS_ALLOWED_ORIGINS.
