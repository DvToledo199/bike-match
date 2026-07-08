Tasca S5.02 - Projecte Final MVP

Introducció

En aquest últim projecte et proposem que desenvolupis un MVP (Minimum Viable Product) d'un producte que resolgui alguna necessitat o problema real que t'interessi.

Pots inspirar-te en experiències personals, en idees que sempre has volgut provar, en situacions del teu entorn o en algun sector que t'agradaria explorar.

Un MVP és una versió reduïda del teu producte on hi implementes les accions centrals que li donen sentit.

L'objectiu és que puguis construir una primera versió funcional del teu producte, prou completa per ensenyar-la, explicar-la i continuar-la si vols seguir desenvolupant-la un cop acabat el curs.

Aquest projecte final no s'avalua només per si "funciona", sinó per la teva capacitat de convertir una idea en un producte petit però coherent, planificat, implementat i desplegat amb criteri tècnic.

Objectius del projecte

Amb aquest projecte hauràs de ser capaç de:

Definir un problema o necessitat real i acotar-lo a un MVP viable.
Transformar aquesta idea en un producte executable a partir de User Stories prioritzades.
Organitzar el desenvolupament amb un GitHub Project o un tauler Kanban equivalent.
Treballar amb un flux de Git basat en branches, vinculant el treball a issues i utilitzant pull requests, encara que després les revisis i les tanquis tu.
Construir una solució completa amb:
un frontend desenvolupat amb un framework,
un backend propi exposat com a API REST.
Aplicar de manera integrada els coneixements del curs en persistència, seguretat, qualitat de codi, proves, documentació i desplegament.
Validar el backend mitjançant proves automatitzades obligatòries, incloent com a mínim tests unitaris i tests d'acceptació de l'API.
Demostrar traçabilitat entre la idea inicial, les històries d'usuari i les funcionalitats implementades.
Fer un ús responsable d'IA generativa com a suport de desenvolupament, entenent, revisant i justificant allò que lliures.
Descripció del projecte

Hauràs de plantejar, desenvolupar i lliurar un producte digital funcional que resolgui un cas de negoci concret.

Aquest projecte ha d'incloure com a mínim:

un frontend desenvolupat amb un framework modern,
un backend desenvolupat com a API REST,
una base de dades PostgreSQL,
autenticació i autorització amb JWT utilitzant Spring Security,
documentació de l'API amb OpenAPI i una interfície de consulta com Swagger UI,
un mínim de dos rols d'usuari,
documentació mínima per executar, provar i entendre el projecte.
El frontend pot haver estat creat parcialment amb suport d'IA generativa, però:

ha d'estar fet amb un framework,
l'has de poder executar i adaptar,
l'has de poder explicar tècnicament,
has d'entendre el flux entre interfície, estat, crides a l'API i tractament de respostes,
i ha de mantenir una estructura mínima i entenedora, fent servir components adequats per a cada part de la interfície i evitant concentrar tota la UI i la lògica en una sola pàgina o en un sol fitxer quan el projecte ja demani separar responsabilitats.
El backend, en canvi, haurà de ser una implementació pròpia i coherent, amb responsabilitats clares, validació de dades, seguretat bàsica i respostes HTTP consistents.

L'arquitectura del projecte és lliure. Pots optar per una arquitectura per capes clàssica o per enfocaments com Clean Architecture, arquitectura hexagonal, DDD o similars, sempre que l'elecció sigui coherent amb la mida i les necessitats del projecte.

Requisits mínims obligatoris

Independentment del nivell al qual vulguis aspirar, el projecte ha de complir com a mínim aquests requisits:

Frontend amb framework.
Backend com a API REST.
Persistència de dades amb PostgreSQL.
Migracions per a la base de dades.
L'esquema de PostgreSQL s'haurà de gestionar mitjançant una eina de migracions com Flyway o Liquibase.
No s'acceptarà una base de dades que depengui de creació manual de taules o canvis manuals fora del codi del projecte.
JWT obligatori per a l'autenticació i l'autorització mitjançant Spring Security.
Documentació obligatòria de l'API amb OpenAPI i una interfície com Swagger UI.
Aquesta documentació ha de permetre consultar, com a mínim, els endpoints disponibles, els principals esquemes de request/response i els requisits d'autenticació dels endpoints protegits.
Un mínim de dos rols d'usuari amb permisos coherents amb el cas d'ús.
Model de dades coherent amb el procés de negoci principal.
Tests obligatoris al backend.
Com a mínim, el backend haurà d'incloure:
tests unitaris,
tests d'acceptació sobre l'API.
El backend ha d'assolir una cobertura mínima de proves del 60%.
Aquesta mètrica no substitueix la qualitat dels tests: la cobertura ha de reflectir proves útils sobre el flux principal, els casos d'error rellevants i els punts crítics de validació o seguretat.
No s'acceptarà un backend sense aquest mínim de proves automatitzades.
Docker obligatori per a l'entorn de desenvolupament i la infraestructura, incloent com a mínim la base de dades.
Desplegament funcional d'una versió accessible del projecte o, com a mínim, del backend.
README.md complet amb instruccions d'execució.
Tauler de treball en GitHub Project o Kanban equivalent.
Flux de treball amb branches, issues vinculades i pull requests.
Abans de començar a programar

1. Pensa una idea (1 frase)

Primer de tot, pensa en un producte que t'agradaria crear i defineix-lo amb una frase clara.

No cal complicar-ho: només has d'explicar qui l'utilitza i quin problema resol.

Per exemple:

"Vull crear una aplicació perquè els cuidadors de gent gran puguin registrar de manera ràpida les activitats que fan amb cada pacient."
2. Defineix el MVP

Abans d'escriure cap línia de codi, concreta què farà la primera versió funcional:

Funcionalitat mínima: què és imprescindible que pugui fer?
Accions centrals: quines són les accions més importants de cada usuari?
Procés de negoci: quins són els passos essencials que passen dins l'aplicació?
Per exemple, si vols fer un marketplace:

Funcionalitat mínima:
Els venedors poden publicar un producte amb títol, preu i foto.
Els compradors poden veure els productes i marcar-ne un com a interessant.
Accions centrals:
Crear producte (venedor).
Llistar productes (comprador).
Marcar un producte com a favorit o com a interessant.
Procés de negoci essencial:
"crear producte -> apareix al catàleg -> comprador el pot veure i marcar com interessat".
En aquest punt, has de consultar amb el mentor per validar la idea i assegurar que el projecte és factible i no massa gran.
3. Escriu entre 6 i 10 User Stories

Ara que l'MVP està clar, transforma'l en històries d'usuari:

històries del procés de negoci,
històries d'autenticació i rols,
històries de pantalles principals.
Exemple:

"Com a cuidador vull afegir una cura a un pacient per mantenir un registre de què s'ha fet."
Aquestes històries seran la teva guia de desenvolupament.

4. Organitza-les al GitHub Project

Crea un tauler Kanban, distribueix i prioritza totes les User Stories en el backlog.

Es recomana utilitzar GitHub Project, però pots fer servir una eina equivalent si mantens clarament visibles:

el backlog,
la priorització,
l'estat de cada història,
i la relació entre cada història i el que s'ha acabat implementant.
A partir d'aquí, treballaràs sempre seguint aquest flux.

5. Treballa amb Git durant tot el desenvolupament

Aquest projecte final també s'ha de treballar amb una dinàmica de desenvolupament semblant a la d'un equip real:

crea issues per representar les funcionalitats o tasques principals,
vincula cada issue amb les User Stories corresponents quan sigui possible,
si una història o una issue és massa gran, desglossa-la en subissues o tasques tècniques més petites per separar millor la implementació, per exemple model de dades, seguretat, proves, documentació de l'API o desplegament,
desenvolupa cada bloc de treball en una branch separada,
i obre una pull request per integrar els canvis, encara que la revisió i el tancament els facis tu.
L'objectiu no és simular burocràcia, sinó demostrar traçabilitat, ordre i bons hàbits de treball.

6. Disseny de la base de dades

Després d'escriure les User Stories i validar-les amb el mentor, dissenyaràs:

les entitats principals,
les seves relacions (1:N, N:M),
i les dades mínimes necessàries per implementar el procés de negoci.
La base de dades del projecte haurà de ser PostgreSQL i ha de reflectir només allò que el MVP necessita, no funcionalitats futures.

Criteris d'avaluació

Els nivells següents s'han d'entendre com tres graus de qualitat, maduresa tècnica i profunditat sobre un mateix projecte final.

⭐ Nivell 1 - MVP funcional

En aquest nivell el teu projecte haurà de resoldre de manera clara el seu cas d'ús principal i funcionar de punta a punta.

En aquest nivell es valorarà especialment:

que la idea estigui ben acotada i tingui sentit com a MVP,
que hi hagi entre 6 i 10 User Stories coherents amb el problema plantejat,
que el treball estigui organitzat en un GitHub Project o Kanban equivalent,
que existeixi traçabilitat entre User Stories, backlog i funcionalitats implementades,
que hagis treballat amb issues, branches i pull requests d'una manera ordenada i coherent,
que el frontend estigui desenvolupat amb un framework,
que el backend sigui una API REST funcional,
que hi hagi persistència de dades amb PostgreSQL,
que hi hagi autenticació i autorització amb JWT implementada amb Spring Security,
que existeixin com a mínim dos rols d'usuari amb comportaments o permisos diferenciats,
que la documentació de l'API amb OpenAPI/Swagger estigui disponible i sigui coherent amb els endpoints realment implementats,
que el backend inclogui com a mínim tests unitaris i tests d'acceptació de l'API,
que aquestes proves cobreixin com a mínim el flux principal del negoci i alguns casos d'error rellevants,
que el flux principal del negoci estigui complet i es pugui demostrar,
que el projecte es pugui executar seguint les instruccions del README.md.
En resum, en aquest nivell hauràs de demostrar que has estat capaç de convertir una idea en un producte.

⭐⭐ Nivell 2 - Qualitat tècnica i consistència

En aquest nivell es valorarà la qualitat de la implementació i la solidesa tècnica del projecte.

En aquest nivell es valorarà especialment:

una arquitectura clara, coherent i ben aplicada, sigui per capes o amb un altre enfocament justificat,
una bona separació de responsabilitats entre controladors, serveis, persistència i seguretat,
l'ús correcte de validacions i la gestió consistent d'errors,
la qualitat dels tests obligatoris del backend,
la claredat i utilitat dels tests unitaris,
la cobertura dels tests d'acceptació sobre els endpoints principals,
que la cobertura de tests del backend sigui coherent amb els riscos reals del projecte i no es limiti a inflar una mètrica,
la validació tant de casos correctes com de casos d'error previsibles,
la qualitat del model de dades i la coherència amb el procés de negoci,
la capacitat de refinar la feina en subissues o tasques tècniques quan això ajudi a planificar i implementar millor una funcionalitat,
que el frontend mantingui una estructura prou clara, amb components adequats, responsabilitats separades i una organització coherent amb la mida real de cada funcionalitat,
que les migracions estiguin ben organitzades i permetin crear o actualitzar l'esquema de manera reproduïble,
la configuració del projecte mitjançant variables d'entorn quan sigui necessari,
la qualitat de la documentació de l'API, incloent la descripció dels endpoints, els codis de resposta, els models de dades i l'autenticació,
la presència de logs útils per entendre què passa a l'aplicació,
l'ús correcte de Docker per muntar l'entorn de desenvolupament i la infraestructura necessària,
el desplegament funcional d'una versió del producte,
la documentació tècnica mínima del projecte i, si escau, de l'API.
En resum, en aquest nivell hauràs de demostrar que el teu projecte no només funciona, sinó que està ben construït i preparat per ser mantingut i revisat per altres desenvolupadors.

⭐⭐⭐ Nivell 3 - Maduresa tècnica i valor afegit

En aquest nivell es valorarà el criteri tècnic, la capacitat d'anar més enllà del mínim i l'ús justificat de pràctiques més avançades.

En aquest nivell es valorarà especialment:

l'ús d'un pipeline de CI/CD,
integracions externes amb sentit de producte, com ara:
pujada de fitxers a Cloudinary o un servei equivalent,
enviament d'emails,
altres serveis externs justificats pel cas d'ús,
l'ús de cache si aporta una millora al producte,
un nivell superior de seguretat, observabilitat o control operatiu,
l'aplicació d'una arquitectura més avançada, com ara arquitectura hexagonal, sempre que estigui ben entesa i ben aplicada,
l'ús d'idees properes a DDD o d'un model de domini més ric quan el projecte ho justifiqui,
la maduresa global del projecte: coherència, justificació de decisions i capacitat d'explicar les eleccions tècniques.
Algunes línies tècniques avançades que pots explorar

No cal que incorporis totes aquestes idees. L'important és que, si decideixes afegir-ne alguna, aporti valor real al producte i estigui ben justificada.

Integracions amb serveis externs: connexió amb APIs de tercers, consum de dades externes, webhooks, sincronització amb altres plataformes o integració amb eines de negoci.
Sistemes de pagament: passarel·les com Stripe o similars per gestionar cobraments, subscripcions o pagaments puntuals.
Geolocalització i mapes: localització d'usuaris o recursos, cerca per proximitat, rutes, càlcul de distàncies o funcionalitats basades en coordenades.
Pujada i gestió de fitxers: integració amb serveis com Cloudinary o equivalents per gestionar imatges o altres arxius.
Correu i notificacions: enviament de correus transaccionals, notificacions de negoci o fluxos d'activació d'usuaris.
Observabilitat i operació: logs estructurats, mètriques, health checks, traçabilitat o eines bàsiques de monitoratge.
Rendiment i escalabilitat: cache, reducció de consultes repetitives o millores específiques de rendiment.
IA i funcionalitats intel·ligents: classificació automàtica, recomanacions, resum de textos, cerca semàntica o assistents de suport.
LLMs i Spring AI: integració de funcionalitats basades en models de llenguatge utilitzant eines com Spring AI, sempre que tinguin sentit dins del producte.
Bases de dades vectorials o cerca semàntica: per exemple, ús de pgvector sobre PostgreSQL o altres solucions semblants per construir funcionalitats de cerca avançada o RAG.
En resum, en aquest nivell hauràs de demostrar que no només has desenvolupat un MVP funcional, sinó que també has incorporat elements de maduresa professional propis d'un projecte més complet.

Lliurament

Per al lliurament final hauràs d'aportar com a mínim:

l'enllaç al repositori del projecte,
l'enllaç al GitHub Project o tauler Kanban equivalent,
un README.md complet,
evidència del flux de treball amb issues, branches i pull requests,
les instruccions per executar el frontend i el backend,
les instruccions per executar els tests del backend,
l'enllaç o la ruta d'accés a la documentació Swagger/OpenAPI,
un fitxer .env.example o document equivalent si cal configuració,
l'enllaç del desplegament del frontend i/o del backend, segons correspongui,
i qualsevol informació addicional necessària perquè el mentor pugui provar i entendre el projecte.
README mínim recomanat

El README.md hauria d'incloure com a mínim:

objectiu del producte,
problema que resol,
stack tecnològic,
instruccions d'instal·lació i execució,
variables d'entorn necessàries,
com aixecar l'entorn amb Docker,
com executar les migracions de base de dades o en quin moment s'apliquen,
descripció breu de l'arquitectura,
descripció del model d'autenticació i dels rols disponibles,
com accedir a la documentació Swagger/OpenAPI en local i, si existeix, en entorn desplegat,
com autenticar-se des de Swagger UI per provar endpoints protegits,
explicació breu de les suites de tests existents i com executar-les,
explicació breu del flux de treball seguit amb issues, branches i pull requests,
funcionalitats implementades,
enllaç al tauler d'User Stories,
i enllaç al desplegament del frontend i/o del backend, segons correspongui.
Recursos recomanats

Si necessites suport, pots consultar aquests recursos:

Com escriure User Stories: Atlassian - User Stories
Com organitzar el treball en GitHub Project: GitHub Docs - About Projects
Com treballar amb issues a GitHub: GitHub Docs - About issues
Com treballar amb branches a GitHub: GitHub Docs - About branches
Com treballar amb issues i pull requests a GitHub: GitHub Docs - About pull requests
Com escriure una bona pull request: GitHub Docs - Creating a pull request
Què és un MVP i com acotar-lo: Atlassian - Minimum Viable Product
Spring Security: Spring Security Reference
OpenAPI Specification: OpenAPI Documentation
Swagger UI: Swagger UI Documentation
springdoc-openapi: springdoc.org
JWT: JWT Introduction
PostgreSQL: PostgreSQL Documentation
Migracions de base de dades amb Flyway: Flyway Documentation
Alternativa de migracions amb Liquibase: Liquibase Documentation
Docker Compose per a entorns de desenvolupament: Docker Docs - Docker Compose
Proves amb Spring Boot: Spring Boot Testing Reference
GitHub Actions per a CI/CD: GitHub Docs - Understanding GitHub Actions
Spring AI: Spring AI Reference
Cloudinary: Cloudinary Documentation
Stripe: Stripe Documentation
pgvector: pgvector Documentation