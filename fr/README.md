# Cours de Java — du langage moderne à un programme qui vérifie plusieurs services à la fois

**Par Dorian Chávez, fondateur de Hábil et architecte d'intégration.**

**À qui il s'adresse :** à qui programme déjà dans un autre langage —Go, TypeScript, Python ou n'importe lequel— et veut apprendre le Java d'aujourd'hui. C'est un cours de niveau intermédiaire : on n'explique pas ce qu'est une variable ni une boucle, on explique ce que Java fait de différent et pourquoi cela existe. Chaque nouveau concept est expliqué au moment où il apparaît. Si tu n'as jamais programmé, commence par le [cours de Python](https://github.com/HabilMX/curso-python) ou celui de [Go](https://www.habil.mx/fr/cours/go/).

**Ce que tu sauras faire à la fin :** écrire un programme complet en Java 25, avec le modèle de données exprimé en types, des tests et de la concurrence, comprendre ce que tu as écrit et pouvoir l'expliquer à quelqu'un d'autre. Et garder le jugement nécessaire pour lire par toi-même la documentation du langage.

**Ce dont tu as besoin avant de commencer :** un ordinateur avec Linux Mint, savoir ouvrir un terminal et avoir écrit des programmes avec des fonctions, des conditionnelles et des boucles dans n'importe quel langage. La [Leçon 1](01-instalacion.md) installe le JDK de zéro, et la [Leçon 6](06-proyecto-maven.md), Maven.

## Le projet que tu vas construire

Le **`revisor`** : un programme qui lit une liste de services, les interroge **tous en même temps** par HTTP, mesure combien de temps met chacun, isole la défaillance de chaque service et livre un rapport toujours dans le même ordre. À la fin, il l'expose par un petit serveur et l'empaquette dans un JAR. C'est le même problème que dans les cours de Go, de Rust et de TypeScript, résolu cette fois en Java.

    $ java -jar revisor.jar --config servicios.json
    SERVICIO    ESTADO    TIEMPO  DETALLE
    catalogo    ARRIBA     182ms  200
    inventario  LENTO       2.3s  200
    pagos       ARRIBA     132ms  200
    reportes    CAIDO          —  sin conexión

    4 servicios · 2 arriba · 1 lento · 1 caído · promedio 883ms

(Une exécution réelle du programme terminé contre le serveur d'entraînement du cours, le 13 octobre 2026 ; les temps changent à chaque exécution. Le programme imprime ses messages en espagnol : « sin conexión » veut dire « sans connexion », et la ligne finale, « 4 services · 2 en ligne · 1 lent · 1 tombé · moyenne 883ms ».)

Cela paraît petit et ne l'est pas : pour l'écrire correctement, tu as besoin de types scellés, de collections, d'exceptions, d'un projet Maven avec des tests, du client HTTP, de JSON, de threads virtuels et d'un paquet que l'on puisse livrer.

## Les onze leçons

| Leçon | | Ce que tu construis | Ce que tu apprends |
|---|---|---|---|
| 0 | [Ce qu'est Java et ce qu'il n'est PAS](00-que-es-java.md) | rien pour l'instant (lecture) | code source, *bytecode* et JVM ; JDK et JRE ; ce qu'est LTS et pourquoi la 25 ; ce que le compilateur protège et ce qu'il ne protège pas |
| 1 | [Installer Java 25 sur ton Linux Mint](01-instalacion.md) | l'environnement et ton premier programme | JDK, `java --version` ; `void main()` et `IO.println` ; `java Hola.java` ; `jshell` ; `javac` ; la première erreur |
| 2 | [Types, chaînes et contrôle de flux](02-tipos-cadenas-control.md) | les fonctions de base du `revisor` | primitifs et références ; `var` ; `==` contre `equals` ; `&&` contre `&` ; blocs de texte ; `switch` comme expression ; `null` |
| 3 | [Records, enums et types scellés](03-records-enums-sellados.md) | le modèle `Servicio` / `Estado` | `record` ; immuabilité ; `enum` ; `sealed` ; `switch` exhaustif avec motifs ; motifs de record |
| 4 | [Collections, génériques et streams](04-colecciones-genericos-streams.md) | la liste de services et le rapport | `List`, `Map`, `Set` ; collections immuables ; génériques ; `Comparator` ; *streams* ; `Optional` |
| 5 | [Erreurs et exceptions](05-errores-excepciones.md) | le `revisor` qui ne plante pas | contrôlées et non contrôlées ; `try-with-resources` ; exceptions personnalisées ; résultat contre exception |
| 6 | [Projet avec Maven](06-proyecto-maven.md) | le vrai projet : il compile, s'empaquette et s'exécute | paquets et visibilité ; installer Maven ; structure de Maven ; `pom.xml` ; le cycle de vie ; ce qu'il y a dans un `.jar` |
| 7 | [Tests avec JUnit](07-pruebas-junit.md) | les 18 premiers tests du `revisor` | JUnit 6 ; `@Test` ; tests pilotés par table de cas ; `assertThrows` et `@TempDir` ; la voir en rouge ; lire le rapport |
| 8 | [HTTP et JSON](08-http-y-json.md) | une vraie requête et la configuration | `HttpClient` ; délais maximaux ; Jackson 3 ; valider les données de l'extérieur ; SSRF |
| 9 | [Concurrence : tout vérifier en même temps](09-concurrencia.md) | le `revisor` concurrent | threads virtuels ; un thread par tâche ; `invokeAll` ; `Semaphore` ; `ConcurrentHashMap` ; une situation de compétition mesurée |
| 10 | [Le programme terminé](10-el-programa-terminado.md) | le `revisor` complet, prêt à livrer | ligne de commande et code de sortie ; serveur du JDK ; arrêt propre ; qualité ; JAR exécutable |

À la fin de chaque leçon, il y a des exercices avec leurs solutions, et une section de sources avec les liens de ce qui a été affirmé. Et le [journal de bord](bitacora.md) est à toi : note-y ce qui t'a coûté.

## Trois critères qui traversent tout le cours

**Les erreurs s'enseignent par ce qu'elles coûtent.** Les études menées avec les données de centaines de milliers d'étudiants de Java montrent que les enseignants se trompent en devinant quelles erreurs sont difficiles. Celles qui mettent le plus de temps à être corrigées sont confondre `==` avec `equals`, `&` avec `&&` et ne pas utiliser la valeur que renvoie une méthode. Celles-là apparaissent quand c'est leur tour, et chaque leçon provoque exprès l'erreur que tu vas voir.

**Les tests et la sécurité ne sont pas des leçons, ce sont des habitudes.** Il y a des tests dès que le projet peut les exécuter (leçon 7, juste après l'avoir converti en projet Maven), et un test ne compte pas tant que tu ne l'as pas vu échouer. La sécurité apparaît là où apparaît le risque du `revisor` : l'adresse que quelqu'un écrit dans un fichier, les données de l'extérieur que l'on valide, la sérialisation native que l'on n'utilise pas avec des données externes.

**On n'enseigne que ce qui est déjà définitif dans Java 25.** Ce qui reste en préversion apparaît dans un encadré « ce qui arrive », avec l'option qu'il exige, et l'on dit de ne pas l'utiliser en production. Un cours qui enseigne ce qui vient de sortir vieillit en six mois.

**Pour continuer à lire sur le blog de Hábil :** [quand il convient (et quand non) de compiler Java en natif](https://www.habil.mx/fr/blog/java-natif-quand-oui-quand-non/), [des tests qui attrapent vraiment les erreurs](https://www.habil.mx/fr/blog/tests-qui-attrapent-erreurs-couverture-quality-gate/) et [la sécurité des API](https://www.habil.mx/fr/blog/securite-api-cto-identite-passerelle-jetons/). Et s'il te manque la base d'un autre langage, le [cours de Python](https://github.com/HabilMX/curso-python) ou celui de [TypeScript](https://www.habil.mx/fr/cours/typescript/).

## Comment savoir que tu as terminé

Le cours ne se termine pas quand tu as lu la leçon 10, mais quand ton `revisor` remplit ces six conditions :

1. **`mvn -B verify` se termine au vert et le rapport dit combien de tests se sont exécutés.** Compte-les dans le rapport, pas dans le code de sortie : un vert avec zéro test ressemble à un vrai vert.
2. **Il vérifie 20 services à la fois en un temps proche de celui du plus lent**, pas de la somme de tous. Tu le mesures toi-même, avec l'horloge, en série et en parallèle.
3. **Un service en panne, lent ou qui répond n'importe quoi ne fait pas tomber le rapport :** il apparaît dans sa ligne avec son erreur, et le reste sort complet et dans le même ordre que toujours.
4. **Aucune requête ne peut attendre indéfiniment :** toutes ont un délai de connexion et un délai maximal.
5. **Il rejette les adresses qui ne sont pas `http` ou `https` et celles qui ne figurent pas dans la liste autorisée**, au lieu de consulter ce qu'on lui écrit dans le fichier.
6. **Il s'exécute avec `java -jar revisor.jar` depuis un autre dossier et sort avec le code 0 si tout est en ligne et 1 sinon.**
