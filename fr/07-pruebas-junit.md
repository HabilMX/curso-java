# Leçon 7 — Tests avec JUnit

**Durée :** 90 min (juste : chaque test se voit d'abord en rouge puis en vert, et cela prend du temps ; ne le saute pas)

**Ce que tu construis :** le `revisor` gagne 18 tests automatiques qui s'exécutent avec une seule commande et surveillent ses règles : la classification, le format du temps, la lecture de `servicios.txt` et le résumé

**Ce que tu apprends :** ce qu'est un test automatique et ses trois temps ; JUnit 6 dans le `pom.xml`, avec son BOM et en portée `test` ; `@Test` et `assertEquals` ; `@ParameterizedTest` avec `@CsvSource` (la table de cas) et pourquoi les cas se choisissent à la limite ; `assertThrows` et `@TempDir` ; voir un test en rouge avant de le voir en vert ; et que **le nombre de tests se lit dans le rapport, pas dans le code de sortie**

**D'où tu viens.** Tu arrives avec le projet Maven de la [Leçon 6](06-proyecto-maven.md) : treize fichiers dans le paquet `mx.habil.revisor`, un `pom.xml` avec trois *plugins* à version fixe, et `mvn -B verify` qui compile, empaquette `revisor-0.6.0.jar` et se termine par `BUILD SUCCESS`. Au milieu de cette sortie, il y avait une ligne qui disait `No tests to run.` : ton projet a déjà un endroit où mettre des tests (le dossier `src/test/java`, vide) et quelqu'un pour les exécuter (le *plugin* *Surefire*), mais pas un seul test. Et cela laisse une question gênante : comment sais-tu que le `revisor` fonctionne ? Tu l'as exécuté et tu as regardé le tableau. **Le seul test qu'a ton programme, c'est toi, en train de regarder un écran.** Aujourd'hui, tu changes cela : des tests qui vérifient à ta place, en une fraction de seconde et à chaque changement, que chaque règle continue de faire ce qu'elle dit. Tu travailles dans ton dossier `~/curso-java/revisor/`.

**Ce que cette leçon ne fait pas.** Elle ne teste pas `Revision`, la classe qui simule le réseau en attendant avec `Thread.sleep` : cela durerait autant que le programme, et ce qu'elle surveille (le `Simulador`) disparaît dans la [Leçon 8](08-http-y-json.md), qui teste `revisar` contre un vrai serveur lancé à l'intérieur du test. Elle n'utilise pas non plus de bibliothèques pour simuler des objets et ne mesure pas la couverture des tests : avec de petites fonctions et des données fixes, elles ne sont pas nécessaires. Et elle n'ajoute rien au programme : JUnit ne vit que dans les tests, et le `.jar` livré ne change pas de contenu.

**À propos des exécutions.** Tout ce qui figure dans cette leçon a été exécuté avec Temurin 25 et Maven 3.10.0, sous macOS, avec le terminal en UTF-8. Les sorties de Maven sont abrégées : on garde ce que chaque section analyse, et les trois points (`...`) marquent ce qui a été retiré. Deux nombres de ces sorties seront différents sur ton écran et cela n'a pas d'importance : les `Time elapsed` (ils changent à chaque exécution) et, dans les tests qui échouent, le numéro de ligne de la trace (`ResumenTest.java:32`), qui est celui de ton fichier et ne coïncide que si tu l'as écrit comme la leçon, ligne par ligne. Tout le reste, y compris les décomptes de tests, doit coïncider. Les programmes se trouvent dans [`programas/07-pruebas-junit/`](../programas/07-pruebas-junit), dans le dépôt du cours.

## À la fin, tu seras capable de

- Expliquer ce qu'est un test automatique, quels sont ses trois temps et ce qu'ajoute un *framework* de tests comme JUnit.
- Déclarer JUnit 6 dans le `pom.xml` avec son BOM et en portée `test`, et expliquer pourquoi il ne fait pas partie du programme.
- Écrire des tests avec `@Test` et `assertEquals`, avec la valeur attendue en premier, et une table de cas avec `@ParameterizedTest` et `@CsvSource`, en choisissant les cas autour de la limite.
- Tester une erreur avec `assertThrows` et un fichier avec `@TempDir`.
- Voir un test en rouge avant de le voir en vert, et savoir quoi annuler pour le provoquer.
- Exécuter tous les tests, une classe ou un seul test, et lire dans le rapport combien ont été exécutés.
- Expliquer trois façons d'obtenir un vert qui n'a rien testé, et comment tu te protèges de chacune.

## Le pourquoi avant le comment

On est lundi et quelqu'un te demande que le seuil de « lent » passe d'une seconde à 800 millisecondes. C'est un changement d'une ligne : `UMBRAL_LENTO_MS`. Tu le fais, tu exécutes le `revisor`, tu regardes le tableau… et tout semble bien. Est-ce le cas ? Avec quatre services aux données fixes, `inventario` restait lent et les autres restaient en ligne, donc le tableau est identique à celui d'hier. Mais qu'est-il arrivé à un service qui répond en exactement 800 millisecondes ? Compte-t-il comme lent ou non ? Le tableau d'aujourd'hui ne le sait pas : aucun service de tes données ne mesure 800. Si tu t'es trompé d'un `>` là où il fallait un `>=`, l'erreur attend qu'un vrai service tombe pile sur la limite, et ce jour-là, c'est celui qui utilise ton programme qui le découvrira, et non toi.

Regarder le tableau a trois défauts, et les trois empirent avec le temps. **Il ne voit que les données que tu as**, et les erreurs vivent dans celles que tu n'as pas (le 800 exact, le fichier avec une ligne mal écrite, le cas où personne ne répond). **Il dépend de quelqu'un qui regarde**, avec attention, à chaque fois, et personne ne regarde avec attention la vingtième fois. Et **il ne laisse pas de trace** : si demain une autre personne change le format du temps, rien ne l'avertit qu'elle a cassé ce que tu as vérifié hier. Un test automatique corrige les trois choses à la fois : tu écris le cas une fois, il le vérifie à chaque changement sans se fatiguer, et il reste dans le projet pour la personne suivante.

La leçon a six parties : **ce qu'est un test et comment JUnit entre dans le projet** (7.1) ; **le premier test**, et le voir en rouge (7.2) ; **la table de cas** (7.3) ; **tester un fichier et son erreur** (7.4) ; **lire le rapport**, la partie la plus négligée (7.5) ; et **l'étape du projet** (7.6). Avec cette destination en tête, voici ce que tu verras à la fin de la leçon, là où la Leçon 6 disait `No tests to run.` :

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
```

La ligne qui compte est `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. À la fin, tu sauras d'où sort chacun de ces nombres, ce qui se passe quand l'un d'eux bouge, et pourquoi aucun d'eux ne se lit dans le « succès » que Maven annonce à la fin.

## Les concepts

### 7.1 Ce qu'est un test automatique et ce qu'il faut

#### 7.1.1 Les trois temps d'un test

Reviens au lundi du début : le seuil est passé à 800 millisecondes et personne ne sait si un service d'exactement 800 compte comme lent. Un **test automatique** est la façon de répondre à cette question sans attendre que cela arrive en production : un petit programme qui prépare une donnée connue, appelle la partie du programme que l'on veut surveiller, et compare ce qu'il a obtenu avec ce qu'il devait obtenir. Il a toujours trois temps, que l'on abrège en anglais AAA : **arranger** (*arrange*) les données d'entrée, **agir** (*act*) en appelant le code, et **affirmer** (*assert*) que le résultat est celui attendu. Si l'affirmation se vérifie, le test passe ; sinon, il échoue et dit ce qu'il attendait et ce qu'il a obtenu.

Écrire ce petit programme n'est pas difficile. Ce qui donne du travail, c'est tout le reste : trouver tous les tests du projet, les exécuter un par un sans que l'échec de l'un arrête les autres, et rapporter combien ont réussi. C'est ce que fait un ***framework* de tests**, et celui de Java est **JUnit**, qui en est à sa version 6 (la 6.1.3 est la version en vigueur en octobre 2026 ; elle requiert Java 17 ou supérieur, d'après son [guide de l'utilisateur](https://docs.junit.org/current/user-guide/)). Il met quatre choses à ta disposition : des annotations comme `@Test` pour marquer quelles méthodes sont des tests, des méthodes comme `assertEquals` pour affirmer, un moteur qui découvre et exécute les tests, et un rapport de ce qui s'est passé. Une **annotation** est une étiquette que l'on écrit avec `@` devant une déclaration et que les outils lisent ; elle ne change pas ce que fait la méthode, elle dit seulement à JUnit comment la traiter.

#### 7.1.2 JUnit dans le `pom.xml`

Pour l'avoir dans le projet, il faut déclarer la dépendance dans le `pom.xml`. Voici le fichier complet, avec JUnit. La partie nouvelle se trouve dans `<dependencyManagement>` et `<dependencies>`, et la version du projet passe à `0.7.0`, comme l'avait promis la Leçon 6 : le `.jar` s'appellera `revisor-0.7.0.jar`.

```xml fig07_01/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.7.0</version>

  <properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>6.1.3</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.16.0</version>
      </plugin>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.6.0</version>
      </plugin>
      <plugin>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.5.1</version>
      </plugin>
    </plugins>
  </build>
</project>
```

JUnit n'est pas un seul artefact, mais une famille (l'API pour écrire les tests, le moteur qui les exécute, la prise en charge des tables de cas…) et tous doivent avoir la même version pour fonctionner ensemble. Pour ne pas écrire cette version six fois, et ne pas se tromper sur l'une d'elles, Maven propose le **BOM** (*bill of materials*, « nomenclature ») : un `pom.xml` qui ne contient pas de code mais seulement une liste de versions compatibles. Le bloc `<dependencyManagement>` avec `<type>pom</type>` et `<scope>import</scope>` importe cette liste complète : à partir de là, n'importe quel artefact de JUnit se déclare **sans version** et prend celle que fixe le BOM. La dépendance elle-même est unique, `junit-jupiter`, un artefact « parapluie » qui apporte l'API, la prise en charge des tables de cas et le moteur. C'est la forme que le [guide de JUnit pour Maven](https://docs.junit.org/current/running-tests/build-support.html) recommande pour aligner les versions.

La ligne qui compte le plus est `<scope>test</scope>`. La **portée** (*scope*) d'une dépendance dit où elle est disponible : celle de portée `test` n'existe que lorsqu'on compile et exécute les tests, et ne fait **pas** partie du programme. Qui exécute le `revisor` n'a pas besoin de JUnit, et on ne l'oblige pas à le télécharger. Comme le compilateur a maintenant JUnit dans `src/test/java` et non dans `src/main/java`, le `import org.junit...` qui avait échoué avec le lanceur dans la Leçon 6 (la figure `fig06_01`) fonctionne dans les tests et continuerait d'échouer dans le programme, ce qui est exactement ce que l'on veut.

#### 7.1.3 Ce qu'on teste et ce qu'on ne teste pas

Avant d'écrire le premier, une décision qui vaut pour tout le cours : **quelles parties du `revisor` méritent un test aujourd'hui**. La réponse vient de ce que fait chaque classe. `Clasificador`, `Formato` et `Resumen` sont des **règles** : elles reçoivent une donnée et en renvoient une autre, toujours la même pour la même entrée, sans toucher au réseau, au disque ni à l'horloge. Ce sont les plus faciles à tester et les plus précieuses, parce que c'est là que vivent les décisions que quelqu'un voudra changer un lundi (le seuil, le format du temps, la façon de calculer une moyenne). `Configuracion` est une règle avec un bord tourné vers l'extérieur : elle lit un fichier, et ce qu'il faut surveiller, ce sont ses messages d'erreur, qui sont la seule chose que voit celui qui se trompe en écrivant `servicios.txt`. Les quatre ont un test dans cette leçon.

`Revision` et `Simulador` n'en ont pas, et la raison n'est pas la paresse. `Simulador` est un échafaudage : il invente quatre réponses pour que le programme ait quelque chose à vérifier tant qu'il n'y a pas de réseau, et dans la Leçon 8 il disparaît. Un test d'échafaudage vérifie que l'échafaudage dit ce que tu y as écrit, et on le jette avec lui. `Revision.revisar`, de son côté, **attend** réellement ce que dit chaque service (`Thread.sleep`) : un test des quatre services durerait autant que le programme, environ 2.5 secondes, alors que les 18 tests de cette leçon s'exécutent en entier en une fraction de seconde. Cette vitesse n'est pas un luxe : c'est ce qui fait que tu les exécutes à chaque changement, au lieu de « à la fin ». Quand `revisar` fera une vraie requête, dans la Leçon 8, il aura son propre test contre un serveur lancé à l'intérieur du test lui-même, qui répond ce que le test veut et quand il le veut.

Il y a une troisième raison de commencer par les règles, qui se remarque dans la forme des tests qui viennent : les quatre classes de test vivent dans le **même paquet** que ce qu'elles testent, `mx.habil.revisor`. Grâce à cela, elles voient les classes de visibilité de paquet que la Leçon 6 avait fermées au monde, et il n'a pas fallu ouvrir quoi que ce soit pour pouvoir les tester. Les tests utilisent le programme comme le ferait une autre classe du paquet, et rien de plus.

### 7.2 Le premier test : le résumé

#### 7.2.1 `ResumenTest`, ligne par ligne

Commençons par le plus simple du projet. Le résumé d'une vérification compte les services, et sa moyenne n'inclut que ceux qui ont répondu. Deux tests le surveillent : les quatre services habituels (la moyenne doit être 843), et le cas limite où **personne** ne répond, où la moyenne n'existe pas et où le résumé écrit un tiret.

```java
// fig07_01/src/test/java/mx/habil/revisor/ResumenTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;
import org.junit.jupiter.api.Test;

/** Pruebas del resumen que va debajo de la tabla. */
class ResumenTest {

  private static Resultado resultado(String nombre, Respuesta respuesta) {
    Servicio servicio = new Servicio(nombre, URI.create("https://" + nombre + ".example"));
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  @Test
  void resumeLosCuatroServiciosCanonicos() {
    List<Resultado> resultados =
        List.of(
            resultado("catalogo", new Respondio(200, 142)),
            resultado("inventario", new Respondio(200, 2300)),
            resultado("pagos", new Respondio(200, 87)),
            resultado("reportes", new SinRespuesta("sin conexión")));

    String linea = Formato.resumen(Resumen.de(resultados));

    assertEquals("4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms", linea);
  }

  @Test
  void sinNingunaRespuestaElPromedioEsUnaRaya() {
    List<Resultado> resultados = List.of(resultado("reportes", new SinRespuesta("sin conexión")));

    String linea = Formato.resumen(Resumen.de(resultados));

    assertEquals("1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —", linea);
  }
}
```

Cela se lit de haut en bas. La classe vit dans le même paquet que ce qu'elle teste et s'appelle `ResumenTest` : le nom se termine par `Test` parce que c'est ainsi que le cherche *Surefire*, le *plugin* qui exécute les tests (par défaut il inclut les fichiers qui commencent par `Test` ou se terminent par `Test`, `Tests` ou `TestCase`, selon sa [documentation](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html) ; en 7.5, tu verras ce qui se passe avec un test qui ne s'appelle pas ainsi). La classe n'est pas `public` : JUnit 6 ne l'exige pas, et par la règle de la Leçon 6 (6.1.3) elle garde la visibilité la plus étroite.

Le `import static` est nouveau. Un `import` normal te laisse utiliser une **classe** par son nom court ; un `import static` te laisse utiliser une **méthode statique** d'une classe sans écrire la classe devant. Avec `import static org.junit.jupiter.api.Assertions.assertEquals;`, tu peux écrire `assertEquals(...)` au lieu de `Assertions.assertEquals(...)`. Les tests s'en servent partout, donc cela devient une habitude.

`private static Resultado resultado(String nombre, Respuesta respuesta)` est un **auxiliaire** : une fonction que les deux tests partagent pour construire un `Resultado` sans répéter trois lignes à chaque fois. Il est privé parce qu'il n'importe qu'à cette classe. Et chaque méthode avec `@Test` est un test : elle a un nom qui **dit ce qu'on affirme** (`sinNingunaRespuestaElPromedioEsUnaRaya`, et non `prueba2`), ne reçoit pas de paramètres et ne renvoie rien, et respecte les trois temps séparés par des lignes vides : on construit la liste, on appelle `Formato.resumen(Resumen.de(resultados))`, et on affirme. Dans `assertEquals(esperado, actual)`, **la valeur attendue vient en premier** ; si tu les inverses, le test réussit et échoue pareil, mais le message d'erreur dira le contraire de ce qui s'est produit.

Depuis la racine du projet, avec `-Dtest=ResumenTest` pour n'exécuter que cette classe :

```text
$ mvn -B test -Dtest=ResumenTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`. Deux tests, aucun n'a échoué. Tu sais déjà qu'ils passent. Reste à savoir s'ils **peuvent échouer**.

#### 7.2.2 Le voir en rouge avant de le voir en vert

Un test que tu n'as jamais vu échouer ne démontre rien. Il peut comparer la mauvaise chose, ou comparer quelque chose qui est toujours vrai, et son vert serait un ornement. La seule façon de savoir qu'un test surveille ce qu'il dit surveiller est de **casser volontairement le code qu'il surveille et de vérifier qu'il passe au rouge**. Fais-le maintenant avec ceux du résumé. Dans `Resumen.java`, la moyenne se calcule seulement avec les services qui ont répondu : c'est le `filter(OptionalLong::isPresent)` qui s'en charge. Remplace ces deux lignes (`.filter(OptionalLong::isPresent)` et `.mapToLong(OptionalLong::getAsLong)`) par une seule, `.mapToLong(t -> t.orElse(0))` : maintenant le service qui n'a pas répondu compte pour **zéro milliseconde**, et relance :

```text
$ mvn -B test -Dtest=ResumenTest
[INFO] Running mx.habil.revisor.ResumenTest
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 0.027 s <<< FAILURE! -- in mx.habil.revisor.ResumenTest
[ERROR] mx.habil.revisor.ResumenTest.resumeLosCuatroServiciosCanonicos -- Time elapsed: 0.016 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms> but was: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 632ms>
	at mx.habil.revisor.ResumenTest.resumeLosCuatroServiciosCanonicos(ResumenTest.java:32)
[ERROR] mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya -- Time elapsed: 0.003 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio 0ms>
	at mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya(ResumenTest.java:41)
[ERROR]   ResumenTest.resumeLosCuatroServiciosCanonicos:32 expected: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms> but was: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 632ms>
[ERROR]   ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya:41 expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio 0ms>
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Maintenant il y a des messages, et ce sont de bons messages. `Failures: 2` : les deux tests ont échoué, chacun par une conséquence différente de la même erreur. Le premier dit `expected: <… promedio 843ms> but was: <… promedio 632ms>` : il attendait 843, il a obtenu 632 ; et 632 est `2529 / 4`, la somme des temps divisée par les quatre services au lieu des trois qui ont répondu. Le second dit `expected: <… promedio —> but was: <… promedio 0ms>` : quand personne ne répond, le résumé doit dire qu'il n'y a pas de moyenne, et le changement la transforme en « zéro milliseconde », un nombre qui ressemble à une donnée et n'en est pas une. La ligne qui commence par `at` est la trace : elle dit dans quel fichier et à quelle ligne du test se trouvait l'affirmation qui a échoué (`ResumenTest.java:32`), ce que tu ouvres en premier. Avec ces deux messages en main, tu sais où chercher, sans ouvrir le débogueur. (Deux tests qui tombent pour une même cause racontent aussi quelque chose : ce défaut se remarquait sous deux angles, et le second test existe justement pour cela.)

Annule le changement et vérifie que tout revient au vert. Cet exercice de **revenir en arrière et voir le rouge** est l'habitude qui te coûte le moins cher de tout le cours : il prend une minute et te dit si tes tests valent quelque chose. Dans le cours, on le répétera chaque fois qu'on écrira un nouveau test.

### 7.3 La table de cas : `@ParameterizedTest` et `@CsvSource`

#### 7.3.1 Un test, plusieurs lignes

La classification d'un service est une règle avec une limite : mille millisecondes ou plus, c'est lent. Pour la surveiller correctement, il faut tester plusieurs cas, et tous ont la même forme (un code et un temps entrent, un état sort). Écrire six méthodes presque identiques est un bruit qui cache ce qui est intéressant. JUnit a une meilleure façon, le **test paramétré** : on écrit la méthode une fois, avec des paramètres, et une **table de cas** avec les valeurs :

```java
// fig07_01/src/test/java/mx/habil/revisor/ClasificadorTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de las reglas de clasificación. */
class ClasificadorTest {

  @ParameterizedTest
  @CsvSource({
    "200, 142,  ARRIBA",
    "200, 999,  ARRIBA",
    "200, 1000, LENTO",
    "200, 2300, LENTO",
    "503, 95,   CAIDO",
    "404, 50,   CAIDO"
  })
  void clasificaUnaRespuesta(int codigo, long tiempoMs, Estado esperado) {
    assertEquals(esperado, Clasificador.clasificar(new Respondio(codigo, tiempoMs)));
  }

  @Test
  void sinRespuestaEsCaido() {
    assertEquals(Estado.CAIDO, Clasificador.clasificar(new SinRespuesta("sin conexión")));
  }
}
```

`@ParameterizedTest` remplace `@Test` et dit à JUnit que cette méthode sera exécutée plusieurs fois. `@CsvSource` est la table : chaque texte de la liste est une **ligne**, et les virgules séparent les **colonnes** (CSV vient de *comma-separated values*, « valeurs séparées par des virgules »). JUnit prend chaque ligne, convertit le texte de chaque colonne au type du paramètre qui lui correspond —`int`, `long`, et même `Estado`, qu'il reconnaît par le nom de la constante— et appelle la méthode une fois par ligne. Cela fait six appels, donc le rapport comptera **six tests** pour cette seule déclaration, plus le septième : celui de `SinRespuesta`, qui figure comme `@Test` à part parce qu'il n'a pas la même forme (il n'y a ni code ni temps).

#### 7.3.2 Les cas se choisissent à la limite

Le test du format du temps est la même idée avec une colonne d'entrée et une de texte attendu. Ses quatre lignes sont celles qui comptent autour du changement d'unité : 87 et 999 s'écrivent en millisecondes, 1000 et 2300 en secondes.

```java
// fig07_01/src/test/java/mx/habil/revisor/FormatoTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de cómo se escribe el tiempo en el reporte. */
class FormatoTest {

  @ParameterizedTest
  @CsvSource({"87, 87ms", "999, 999ms", "1000, 1.0s", "2300, 2.3s"})
  void formateaElTiempo(long tiempoMs, String esperado) {
    assertEquals(esperado, Formato.formatearTiempo(tiempoMs));
  }
}
```

Les cas n'ont pas été choisis au hasard. Ils ont été choisis **autour de la limite** : 142 est un service normal ; 999 est la dernière valeur qui reste en ligne ; 1000 est la première qui est lente (le cas le plus important de tous : c'est celui qui détecte un `>` là où il fallait un `>=`) ; 2300 est nettement lent ; 503 et 404 sont des erreurs du serveur et du client qui, quelle que soit la rapidité de leur réponse, font classer le service comme tombé. Les **valeurs limites** sont l'endroit où se cachent les erreurs de décalage d'une unité (*off-by-one*), et une table qui ne les inclut pas laisse le bord sans surveillance. Vérifie-le de la façon que tu connais déjà : dans `Clasificador.java`, remplace `tiempoMs >= UMBRAL_LENTO_MS` par `tiempoMs > UMBRAL_LENTO_MS` et exécute tout le projet :

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.058 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[3] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <LENTO> but was: <ARRIBA>
	at mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(ClasificadorTest.java:26)

[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[ERROR] Failures: 
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:26 expected: <LENTO> but was: <ARRIBA>
[ERROR] Tests run: 18, Failures: 1, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[ERROR] BUILD FAILURE
```

Lis-le calmement, parce que c'est le rapport d'une vraie panne et qu'il contient quatre choses qu'il convient de reconnaître au premier regard. Premièrement, **quelle classe a échoué** : `ClasificadorTest`, avec `Tests run: 7, Failures: 1` ; les trois autres classes restent au vert : l'échec de l'une n'arrête pas les autres. Deuxièmement, **quel test** : `clasificaUnaRespuesta(int, long, Estado)[3]`, où `[3]` est le **numéro de la ligne de la table** : la troisième, `200, 1000, LENTO`. Troisièmement, **ce qui s'est passé** : `expected: <LENTO> but was: <ARRIBA>`. Le service de 1000 millisecondes devait être lent et il est ressorti en ligne, parce qu'avec `>` le 1000 ne compte plus. Et quatrièmement, la ligne de clôture : `Tests run: 18, Failures: 1` et `BUILD FAILURE`, avec le code de sortie différent de zéro qui fait passer au rouge n'importe quel serveur d'intégration. Un seul changement d'un caractère, une seule ligne en rouge, et le rapport te dit laquelle : voilà ce qu'apporte la table. Annule le changement avant de continuer.

#### 7.3.3 Exécuter un seul test

Quand une ligne passe au rouge, ce que tu veux, c'est répéter **ce** test pendant que tu le corriges, pas les 18. Tu as déjà utilisé `-Dtest=ResumenTest` pour exécuter une classe ; avec un `#` et le nom de la méthode, tu choisis un seul test de la classe :

```text
$ mvn -B test -Dtest=ClasificadorTest#sinRespuestaEsCaido
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 1` : des sept tests de la classe, seul celui que tu as demandé s'est exécuté. Remarque aussi la phase : `mvn -B test` et non `verify`. Comme `test` vient avant `package` dans le cycle de vie, Maven s'arrête après les tests et n'assemble pas le `.jar`, dont on n'a pas besoin pour répéter un test. C'est la façon de travailler pendant que tu corriges quelque chose : un test, vite, de nombreuses fois. Et c'est aussi un piège si tu oublies que tu l'as utilisée : un `Tests run: 1` au vert ne dit rien des 17 autres. **Avant de considérer quelque chose comme terminé, on exécute tout**, avec `mvn -B verify` et sans `-Dtest`, et on lit le total.

### 7.4 `assertThrows` et `@TempDir` : tester un fichier et son erreur

La troisième famille de tests surveille ce que fait le `revisor` avec le fichier de configuration. Il y a ici deux choses nouvelles. La première est que tester un fichier exige d'avoir un fichier, et on ne veut pas écrire dans ton vrai dossier ni laisser de déchets. La seconde est que ce que l'on veut affirmer n'est pas une valeur renvoyée mais une **exception** : que certains fichiers fassent échouer la lecture, et avec le message exact.

```java
// fig07_01/src/test/java/mx/habil/revisor/ConfiguracionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de la lectura de servicios.txt. */
class ConfiguracionTest {

  @TempDir Path carpeta;

  @Test
  void leeUnArchivoValido() throws Exception {
    Path ruta = carpeta.resolve("servicios.txt");
    Files.writeString(
        ruta,
        """
        # nombre  url  [timeout_ms]
        catalogo   https://catalogo.example
        pagos      https://pagos.example  1000
        """);

    List<Servicio> servicios = Configuracion.leer(ruta);

    assertEquals(2, servicios.size());
    assertEquals("catalogo", servicios.get(0).nombre());
    assertEquals(Servicio.TIMEOUT_POR_OMISION_MS, servicios.get(0).timeoutMs());
    assertEquals(1000, servicios.get(1).timeoutMs());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          a https://a.x;b https://b.x;solo | renglón 3: se esperaban 2 o 3 campos y hay 1
          a https://a.x;b ftp://b.x | renglón 2: la url debe empezar con http:// o https://
          a https://a.x;b https://b.x 0 | renglón 2: timeout_ms debe ser un entero mayor que 0
          catalogo https://a.x;catalogo https://b.x | nombres repetidos: catalogo
          """)
  void rechazaUnArchivoInvalido(String contenido, String mensajeEsperado) throws IOException {
    Path ruta = carpeta.resolve("servicios.txt");
    Files.writeString(ruta, contenido.replace(";", "\n"));

    ErrorDeConfiguracion error =
        assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta));

    assertEquals(mensajeEsperado, error.getMessage());
  }
}
```

**`@TempDir Path carpeta`** demande à JUnit un dossier temporaire : avant chaque test, il le crée neuf et vide, et après, il le supprime avec tout ce qu'il contient. Les tests y écrivent leur `servicios.txt` avec `Files.writeString` (la Leçon 5 lisait des fichiers avec `Files` ; voici l'autre moitié, les écrire) et ne se gênent pas entre eux ni ne salissent le disque.

**`assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta))`** exécute ce que tu lui donnes (une expression écrite sous la forme `() -> ...` : une *lambda*, la même forme que tu as utilisée dans les *streams* de la Leçon 4) et vérifie qu'elle lève **cette** exception. Si elle ne lève rien, ou en lève une autre, le test échoue ; si elle lève celle qui est attendue, `assertThrows` te la renvoie pour que tu l'examines : le test compare son `getMessage()` avec le message exact de la table. Remarque pourquoi on affirme le message et pas seulement le type : ce texte est la seule chose que voit la personne qui édite `servicios.txt` quand elle se trompe ; c'est le contrat avec elle. Si quelqu'un le réécrit sans s'en rendre compte, le test le détecte.

La table de cas de cette classe utilise une variante de `@CsvSource` qui accepte un bloc de texte (`textBlock`), avec le délimiteur `|` au lieu de la virgule pour que chaque ligne ressemble à une table. Il n'y a qu'une complication : chaque ligne de la table tient sur une seule ligne de texte, mais un fichier de configuration en a plusieurs. Pour cela, la première colonne écrit les lignes du fichier séparées par `;` (`a https://a.x;b ftp://b.x` sont deux lignes), et le test les convertit en vrais sauts de ligne avant d'écrire le fichier. Les quatre lignes sont quatre des motifs de rejet qu'a définis la Leçon 5 (ceux qu'on commet le plus) : il manque des champs, l'adresse n'est ni `http` ni `https`, le délai maximal n'est pas supérieur à zéro, et il y a des noms répétés. Le cinquième test de la classe, `leeUnArchivoValido`, est le cas nominal : deux services, l'un avec le délai maximal par défaut et l'autre avec 1000. **Il en faut toujours un :** une classe qui ne teste que des rejets pourrait passer avec un lecteur qui rejette tout.

### 7.5 Lire le rapport : le décompte des tests

#### 7.5.1 Où est le nombre, et où il n'est pas

Exécute tout le projet, en entier, avec la commande habituelle :

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
```

Ce qui compte dans ce rapport est une seule ligne : `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. Les quatre nombres disent combien de tests ont été **exécutés**, combien ont **échoué** sur une affirmation, combien se sont terminés par une **erreur** inattendue (une exception que le test n'attendait pas) et combien ont été **sautés** (ils sont marqués pour ne pas s'exécuter). Et les 18 sortent, exactement, de ce que tu sais déjà : `ClasificadorTest` 7 (six lignes et celle de `SinRespuesta`), `FormatoTest` 4 (quatre lignes), `ConfiguracionTest` 5 (quatre lignes et le cas nominal) et `ResumenTest` 2. Additionne-les : 7 + 4 + 5 + 2. Si un jour le 18 change sans que tu aies ajouté ou retiré un test, quelque chose a bougé, et c'est une question qui vaut la peine d'être résolue.

Chaque classe de tests laisse en outre son propre rapport dans le dossier `target/surefire-reports`, sous deux formats :

```text
$ ls target/surefire-reports
TEST-mx.habil.revisor.ClasificadorTest.xml
TEST-mx.habil.revisor.ConfiguracionTest.xml
TEST-mx.habil.revisor.FormatoTest.xml
TEST-mx.habil.revisor.ResumenTest.xml
mx.habil.revisor.ClasificadorTest.txt
mx.habil.revisor.ConfiguracionTest.txt
mx.habil.revisor.FormatoTest.txt
mx.habil.revisor.ResumenTest.txt
```

Les `.txt` sont faits pour que tu les lises, et les `TEST-*.xml` pour les machines : le serveur d'intégration continue, l'éditeur, ou un script à toi lisent le XML pour compter. Un rapport texte d'une classe au vert tient en trois lignes, ce que tu as vu plus haut mais pour une seule classe :

```text
$ cat target/surefire-reports/mx.habil.revisor.ConfiguracionTest.txt
-------------------------------------------------------------------------------
Test set: mx.habil.revisor.ConfiguracionTest
-------------------------------------------------------------------------------
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
```

#### 7.5.2 Trois verts qui n'ont rien testé

Et maintenant, la raison pour laquelle cette section existe. **Le nombre se lit dans le rapport, jamais dans le message de succès ni dans le code de sortie.** Il y a une façon pour Maven de se terminer par `BUILD SUCCESS` sans avoir exécuté un seul test, et elle est plus facile à provoquer qu'il n'y paraît : il suffit que les noms des classes de test ne soient pas ceux que cherche *Surefire*. Pour le voir, on a renommé les quatre classes de ce projet de `...Test` en `...Prueba` (par exemple `ClasificadorPrueba`), ce qui en espagnol sonne pareil, et on a exécuté la même commande :

```text
$ mvn -B verify
...
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
$ echo $?
0
```

Il n'y a aucune ligne `T E S T S`, ni `Running`, ni `Tests run`. Et pourtant : `BUILD SUCCESS`, et le code de sortie est `0`, celui qui signifie « tout va bien ». Maven n'a trouvé aucun test, n'avait rien à faire échouer et s'est déclaré en succès. Si le seul critère avait été « est-ce que ça s'est terminé au vert ? », ce projet sans un seul test effectif aurait passé n'importe quelle revue. Le dossier `target/surefire-reports` n'est pas créé non plus, ce qui est un autre indice : le rapport n'existe pas parce qu'il n'y avait rien à rapporter. Compare avec ce que tu as vu dans la Leçon 6, quand le dossier de tests était vide : là, *Surefire* disait `No tests to run.`, un avertissement honnête. Ici, il y a quatre classes de test compilées dans `target/test-classes`, et comme aucune ne s'appelle comme il le cherche, il ne dit rien. **Le silence est pire que l'avertissement**, et c'est pourquoi on cherche le nombre activement : ne pas apparaître est aussi une donnée.

Il y a deux autres variantes du même mécanisme, mesurées elles aussi. La première : une classe de tests qui **se saute toute seule**. JUnit permet à un test de dire « si telle condition n'est pas remplie, ne m'exécute pas » (cela s'appelle une *hypothèse*, `Assumptions`, et cela sert vraiment pour des tests qui ont besoin de quelque chose qui n'est pas toujours là, comme une connexion). Mais une bévue dans cette condition éteint tous les tests de la classe sans que rien ne passe au rouge. On a ajouté une hypothèse fausse au début de `FormatoTest` pour le voir. Voici ce fragment, placé dans la classe, avant le test qu'elle avait déjà (avec ses deux `import` : `org.junit.jupiter.api.Assumptions` et `org.junit.jupiter.api.BeforeAll`) :

```java
  @BeforeAll
  static void soloSiHayRed() {
    Assumptions.assumeTrue(false, "sin red");
  }
```

`@BeforeAll` marque une méthode que JUnit exécute une fois, avant tous les tests de la classe (c'est pourquoi elle est `static` : il n'y a pas encore d'objet de la classe). `assumeTrue(condición, motivo)` dit « si la condition est fausse, n'avance pas ». Ici la condition est `false` à vue d'œil, pour que l'effet soit évident ; dans un vrai projet, ce serait quelque chose comme « il y a une connexion à la base de données », et la bévue, une condition qui ne se remplit jamais sur la machine d'intégration. Le résultat :

```text
$ mvn -B verify
...
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.002 s -- in mx.habil.revisor.FormatoTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
$ echo $?
0
```

Maven a affiché un `WARNING` (jaune, pas rouge) et a continué : la classe entière s'est retrouvée en `Skipped: 1`, le total est passé de 18 à 15, `BUILD SUCCESS` et code de sortie `0`. Qui ne regarde que le vert ne saura jamais que quatre tests ont cessé de surveiller le format du temps. La seconde variante vient du terminal et non de Maven : **le code de sortie d'un tube est celui du dernier programme du tube.** Si, par commodité, tu raccourcis la sortie avec `| tail` et que tu vérifies `$?`, ce que tu vérifies, c'est le code de `tail`, qui sort toujours bien :

```text
$ mvn -B verify | tail -2
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
0
```

Ici Maven a échoué (c'est l'exécution avec le `>` cassé de 7.3.2, et son propre code de sortie était 1), mais le `0` que tu vois est celui de `tail`. Si tu as vraiment besoin d'un tube, le terminal a une option pour que le code de sortie soit celui du premier programme qui a échoué et non du dernier :

```text
$ set -o pipefail
$ mvn -B verify | tail -1
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
1
```

Avec `pipefail`, le `1` de Maven survit au tube. Cela vaut pour le terminal où tu l'as écrit (ou pour le script qui l'a au début), et ne change pas l'essentiel : le plus sûr est d'exécuter les tests **sans filtrer la sortie**, et d'y lire la ligne `Tests run:`. Dans les trois cas, la défense est la même, et c'est l'habitude de ce cours : après avoir exécuté les tests, lis cette ligne et compare le nombre avec celui que tu attends.

Il y a un quatrième vert, mais celui-là prévient : `mvn -B verify -DskipTests` compile les tests, ne les exécute pas, dit `Tests are skipped.` et se termine par `BUILD SUCCESS`. C'est une option légitime, que l'on utilise exprès quand on ne veut que le `.jar` et qu'on a déjà exécuté les tests avant ; il faut se rappeler que ce vert ne signifie pas que quelque chose a été testé, et que ce n'est jamais la commande avec laquelle on décide si un changement est bon.

#### 7.5.3 Le nombre se note

Dans un projet qui existe déjà, la façon de savoir combien en attendre est de l'avoir noté : dans le journal de bord du cours, à côté de la date. Celui d'aujourd'hui dit 18. Quand tu ajouteras un test, ce sera 19, et tu le noteras. C'est une façon peu coûteuse de faire qu'un changement inattendu dans le décompte —vers le haut ou vers le bas— attire ton attention.

### 7.6 L'étape du projet : le `revisor` avec des tests

Tu as toutes les pièces. Voici le projet complet, tel qu'il se présente à la fin de la leçon. Par rapport à la Leçon 6, deux choses changent : le `pom.xml` (version 0.7.0 et JUnit en portée de tests, en 7.1.2) et le dossier `src/test/java`, qui cesse d'être vide. **Le code du programme n'a pas changé d'une seule ligne** : les tests ont été écrits pour le programme qui existait déjà. On inclut tout pour que le projet puisse être monté sans regarder une autre leçon :

```text
revisor/
  pom.xml                 versión 0.7.0 · release 25 · JUnit 6.1.3 en alcance de pruebas
  servicios.txt           sin cambios
  src/main/java/mx/habil/revisor/                       (los trece, sin cambios)
    Revisor.java  Estado.java  Respuesta.java  Servicio.java  Resultado.java
    Clasificador.java  Formato.java  Simulador.java  Revision.java  Resumen.java
    Reporte.java  Configuracion.java  ErrorDeConfiguracion.java
  src/test/java/mx/habil/revisor/
    ClasificadorTest.java  FormatoTest.java  ConfiguracionTest.java  ResumenTest.java   NUEVOS
```

Tu as déjà vu en entier le `pom.xml` et les quatre tests. Ton `servicios.txt` est celui de toujours :

```text fig07_01/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

Et voici les treize fichiers du programme, identiques à ceux de la Leçon 6. Si tu l'as suivie, tu les as déjà ; on les inclut pour que le projet de cette leçon puisse être construit sans dépendre de rien d'autre.

<details>
<summary>Les treize fichiers du programme, sans changement par rapport à la Leçon 6</summary>

```java
// fig07_01/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.List;

/** El punto de entrada: lee servicios.txt, revisa cada servicio e imprime el reporte. */
public final class Revisor {

  private Revisor() {}

  /** Arranca el revisor; los errores de configuración salen por la salida de error. */
  public static void main(String[] args) throws InterruptedException {
    try {
      List<Servicio> servicios = Configuracion.leer(Path.of("servicios.txt"));
      Reporte reporte = Reporte.de(Revision.revisarTodos(servicios));
      System.out.println(Formato.tabla(reporte));
    } catch (ErrorDeConfiguracion e) {
      System.err.println("error de configuración: " + e.getMessage());
    }
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Servicio.java

package mx.habil.revisor;

import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Resultado.java

package mx.habil.revisor;

import java.util.OptionalLong;

record Resultado(Servicio servicio, Estado estado, Respuesta respuesta) {
  String detalle() {
    return switch (respuesta) {
      case Respuesta.Respondio(int codigo, long _) -> String.valueOf(codigo);
      case Respuesta.SinRespuesta(String motivo) -> motivo;
    };
  }

  OptionalLong tiempoMs() {
    return switch (respuesta) {
      case Respuesta.Respondio(int _, long tiempoMs) -> OptionalLong.of(tiempoMs);
      case Respuesta.SinRespuesta _ -> OptionalLong.empty();
    };
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Clasificador.java

package mx.habil.revisor;

final class Clasificador {
  static final long UMBRAL_LENTO_MS = 1000;

  static Estado clasificar(Respuesta respuesta) {
    return switch (respuesta) {
      case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> Estado.CAIDO;
      case Respuesta.Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS ->
          Estado.LENTO;
      case Respuesta.Respondio _ -> Estado.ARRIBA;
      case Respuesta.SinRespuesta _ -> Estado.CAIDO;
    };
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Simulador.java

package mx.habil.revisor;

import java.util.Map;
import java.util.Optional;

final class Simulador {
  static final Map<String, Respuesta> RESPUESTAS =
      Map.of(
          "catalogo", new Respuesta.Respondio(200, 142),
          "inventario", new Respuesta.Respondio(200, 2300),
          "pagos", new Respuesta.Respondio(200, 87),
          "reportes", new Respuesta.SinRespuesta("sin conexión"));

  static Optional<Respuesta> buscar(String nombre) {
    return Optional.ofNullable(RESPUESTAS.get(nombre));
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Revision.java

package mx.habil.revisor;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

final class Revision {
  static Resultado revisar(Servicio servicio) throws InterruptedException {
    Respuesta respuesta =
        Simulador.buscar(servicio.nombre()).orElse(new Respuesta.SinRespuesta("sin conexión"));
    Resultado resultado =
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
    OptionalLong tiempo = resultado.tiempoMs();
    if (tiempo.isPresent()) {
      Thread.sleep(tiempo.getAsLong());
    }
    return resultado;
  }

  static List<Resultado> revisarTodos(List<Servicio> servicios) throws InterruptedException {
    List<Resultado> resultados = new ArrayList<>();
    for (Servicio servicio : servicios) {
      resultados.add(revisar(servicio));
    }
    return resultados;
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Resumen.java

package mx.habil.revisor;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.Collectors;

record Resumen(int total, int arriba, int lento, int caido, OptionalLong promedioMs) {
  static Resumen de(List<Resultado> resultados) {
    Map<Estado, Long> porEstado =
        resultados.stream()
            .collect(Collectors.groupingBy(Resultado::estado, Collectors.counting()));
    OptionalDouble promedio =
        resultados.stream()
            .map(Resultado::tiempoMs)
            .filter(OptionalLong::isPresent)
            .mapToLong(OptionalLong::getAsLong)
            .average();
    OptionalLong promedioMs =
        promedio.isPresent()
            ? OptionalLong.of(Math.round(promedio.getAsDouble()))
            : OptionalLong.empty();
    return new Resumen(
        resultados.size(),
        porEstado.getOrDefault(Estado.ARRIBA, 0L).intValue(),
        porEstado.getOrDefault(Estado.LENTO, 0L).intValue(),
        porEstado.getOrDefault(Estado.CAIDO, 0L).intValue(),
        promedioMs);
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Reporte.java

package mx.habil.revisor;

import java.util.List;

record Reporte(List<Resultado> resultados, Resumen resumen) {
  Reporte {
    resultados = List.copyOf(resultados);
  }

  static Reporte de(List<Resultado> resultados) {
    return new Reporte(resultados, Resumen.de(resultados));
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Formato.java

package mx.habil.revisor;

import java.util.Locale;
import java.util.stream.Collectors;

final class Formato {
  static final String PATRON_LINEA = "%-12s%-10s%6s  %s";

  static String formatearTiempo(long tiempoMs) {
    if (tiempoMs < 1000) {
      return tiempoMs + "ms";
    }
    return String.format(Locale.ROOT, "%.1fs", tiempoMs / 1000.0);
  }

  static String tiempo(Respuesta respuesta) {
    return switch (respuesta) {
      case Respuesta.Respondio(int _, long tiempoMs) -> formatearTiempo(tiempoMs);
      case Respuesta.SinRespuesta _ -> "—";
    };
  }

  static String encabezado() {
    return String.format(Locale.ROOT, PATRON_LINEA, "SERVICIO", "ESTADO", "TIEMPO", "DETALLE");
  }

  static String linea(Resultado resultado) {
    return String.format(
        Locale.ROOT,
        PATRON_LINEA,
        resultado.servicio().nombre(),
        resultado.estado(),
        tiempo(resultado.respuesta()),
        resultado.detalle());
  }

  static String contar(long cantidad, String palabra) {
    return cantidad + " " + palabra + (cantidad == 1 ? "" : "s");
  }

  static String resumen(Resumen resumen) {
    String promedio =
        resumen.promedioMs().isPresent() ? formatearTiempo(resumen.promedioMs().getAsLong()) : "—";
    return contar(resumen.total(), "servicio")
        + " · "
        + resumen.arriba()
        + " arriba · "
        + contar(resumen.lento(), "lento")
        + " · "
        + contar(resumen.caido(), "caído")
        + " · promedio "
        + promedio;
  }

  static String tabla(Reporte reporte) {
    String renglones =
        reporte.resultados().stream().map(Formato::linea).collect(Collectors.joining("\n"));
    return encabezado() + "\n" + renglones + "\n\n" + resumen(reporte.resumen());
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Configuracion.java

package mx.habil.revisor;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class Configuracion {
  static List<Servicio> leer(Path ruta) throws ErrorDeConfiguracion {
    String archivo = ruta.getFileName().toString();
    List<Servicio> servicios = new ArrayList<>();
    try (BufferedReader lector = Files.newBufferedReader(ruta)) {
      String renglon;
      int numero = 0;
      while ((renglon = lector.readLine()) != null) {
        numero++;
        String limpio = renglon.strip();
        if (limpio.isEmpty() || limpio.startsWith("#")) {
          continue;
        }
        servicios.add(leerRenglon(limpio, numero));
      }
    } catch (NoSuchFileException e) {
      throw new ErrorDeConfiguracion("no existe el archivo " + archivo, e);
    } catch (IOException e) {
      throw new ErrorDeConfiguracion("no se pudo leer el archivo " + archivo, e);
    }
    verificarRepetidos(servicios);
    if (servicios.isEmpty()) {
      throw new ErrorDeConfiguracion("el archivo no tiene servicios");
    }
    return servicios;
  }

  static Servicio leerRenglon(String renglon, int numero) throws ErrorDeConfiguracion {
    String[] campos = renglon.split("\\s+");
    if (campos.length < 2 || campos.length > 3) {
      throw new ErrorDeConfiguracion(
          "renglón " + numero + ": se esperaban 2 o 3 campos y hay " + campos.length);
    }
    URI url = leerUrl(campos[1], numero);
    if (campos.length == 2) {
      return new Servicio(campos[0], url);
    }
    return new Servicio(campos[0], url, leerTimeout(campos[2], numero));
  }

  static URI leerUrl(String texto, int numero) throws ErrorDeConfiguracion {
    if (!texto.startsWith("http://") && !texto.startsWith("https://")) {
      throw new ErrorDeConfiguracion(
          "renglón " + numero + ": la url debe empezar con http:// o https://");
    }
    URI url;
    try {
      url = new URI(texto);
    } catch (URISyntaxException _) {
      // Sin la causa: su mensaje repite la dirección entera, con usuario, clave y token.
      throw new ErrorDeConfiguracion("renglón " + numero + ": la url no es válida");
    }
    if (url.getHost() == null) {
      throw new ErrorDeConfiguracion("renglón " + numero + ": la url no es válida");
    }
    if (url.getRawUserInfo() != null) {
      throw new ErrorDeConfiguracion(
          "renglón " + numero + ": la url no puede llevar usuario ni clave");
    }
    return url;
  }

  static int leerTimeout(String texto, int numero) throws ErrorDeConfiguracion {
    String mensaje = "renglón " + numero + ": timeout_ms debe ser un entero mayor que 0";
    try {
      int timeoutMs = Integer.parseInt(texto);
      if (timeoutMs > 0) {
        return timeoutMs;
      }
    } catch (NumberFormatException e) {
      throw new ErrorDeConfiguracion(mensaje, e);
    }
    throw new ErrorDeConfiguracion(mensaje);
  }

  static void verificarRepetidos(List<Servicio> servicios) throws ErrorDeConfiguracion {
    Set<String> vistos = new HashSet<>();
    Set<String> repetidos = new LinkedHashSet<>();
    for (Servicio servicio : servicios) {
      if (!vistos.add(servicio.nombre())) {
        repetidos.add(servicio.nombre());
      }
    }
    if (!repetidos.isEmpty()) {
      throw new ErrorDeConfiguracion("nombres repetidos: " + String.join(", ", repetidos));
    }
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

package mx.habil.revisor;

final class ErrorDeConfiguracion extends Exception {
  ErrorDeConfiguracion(String mensaje) {
    super(mensaje);
  }

  ErrorDeConfiguracion(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
```

</details>

Pour terminer la leçon : `mvn -B verify` doit dire `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` et `BUILD SUCCESS`, et laisser `target/revisor-0.7.0.jar` ; `java -cp target/classes mx.habil.revisor.Revisor` doit afficher le tableau avec le résumé de la Leçon 4 (4.5) de 843 ms, comme dans la Leçon 6, parce que le programme n'a pas changé. Il existe un article de Hábil sur [les tests qui attrapent vraiment les erreurs et la façon de mesurer la couverture](https://www.habil.mx/fr/blog/tests-qui-attrapent-erreurs-couverture-quality-gate/), qui complète cette leçon du côté de la qualité ; et un autre sur [la façon dont se monte l'intégration continue d'une entreprise de logiciel](https://www.habil.mx/fr/blog/cicd-devsecops-infrastructure-propre/), l'endroit où ces 18 tests s'exécutent tout seuls à chaque changement.


## L'erreur que tu vas voir

La leçon a une erreur principale, celle du test qui passe au rouge, et plusieurs messages secondaires qu'il convient de savoir lire d'un coup d'œil.

**L'erreur principale.** C'est celle de 7.3.2. Tu remplaces `>=` par `>` dans la règle du seuil, tu exécutes `mvn -B verify`, et le rapport dit que l'une des lignes de la table de cas, la troisième, attendait `LENTO` et a obtenu `ARRIBA`. Elle se lit en quatre temps, toujours les mêmes : **quelle classe** a échoué (la ligne avec `<<< FAILURE!`), **quel test** (le nom de la méthode et, entre crochets, le numéro de la ligne), **ce qu'il attendait et ce qu'il a obtenu** (`expected: <...> but was: <...>`), et **combien** au total (`Tests run: 18, Failures: 1`). Avec cela, tu sais quoi changer sans ouvrir de débogueur, et tu as la certitude que le test surveille ce qu'il dit surveiller. Le voir échouer, exprès, est le premier usage que tu en fais.

**Les messages secondaires**, pour les reconnaître d'un coup d'œil :

- `org.opentest4j.AssertionFailedError: expected: <...> but was: <...>` : une affirmation ne s'est pas vérifiée. Le rapport la compte dans `Failures`. Si les valeurs semblent inversées, vérifie l'ordre de `assertEquals` : l'attendue vient en premier.
- Une ligne avec `Errors: 1` au lieu de `Failures: 1` : le test n'a pas pu affirmer quoi que ce soit parce qu'une exception qu'il n'attendait pas a été levée. C'est un autre type de problème : non pas « le résultat est autre », mais « le code a planté ».
- `Expected mx.habil.revisor.ErrorDeConfiguracion to be thrown, but nothing was thrown.` : c'est la façon dont JUnit dit qu'un `assertThrows` n'a pas vu l'exception qu'il demandait. La lecture du fichier a accepté quelque chose qu'elle devait rejeter. (Mesuré en remplaçant dans `Configuracion.java` le `timeoutMs > 0` par `timeoutMs >= 0` : un `timeout_ms` de 0 cesse d'être rejeté et la ligne `[3]` de `rechazaUnArchivoInvalido` tombe.)
- `[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1` : une classe entière a été sautée. Jaune, pas rouge, et le total baisse : cherche une hypothèse (7.5.2).
- `Tests are skipped.` : quelqu'un a exécuté avec `-DskipTests`. Ce vert n'a rien testé.
- Aucune ligne `Tests run:` et `BUILD SUCCESS` : *Surefire* n'a pas trouvé de classes qui s'appellent comme il le cherche (7.5.2).

## Ce qui se fait de travers

**Se fier au `BUILD SUCCESS`.** Tu as déjà vu le coût en 7.5 : un projet sans un seul test effectif se termine au vert et avec un code de sortie zéro. Ce qui compte, c'est la ligne `Tests run:` et que le nombre soit celui qu'on attend.

**Faire confiance à un test que tu n'as jamais vu en rouge.** Un test qui passe toujours ne se distingue pas d'un test qui ne surveille rien. Avant de le tenir pour bon, casse ce qu'il surveille, vérifie qu'il échoue avec le message que tu attends et annule le changement.

**Un test sans affirmation.** Une méthode avec `@Test` qui appelle le code mais n'appelle jamais `assertEquals` ni `assertThrows` passe toujours, sauf si le code lève une exception : elle exécute le code, elle ne vérifie rien. Le vert qu'elle donne ne vaut pas plus que celui d'un projet sans tests.

**Ne tester que le cas nominal, ou que les rejets.** Une classe qui ne teste que le fait qu'un bon fichier se lit bien ne sait pas si les mauvais sont rejetés ; une qui ne teste que des rejets passerait avec un lecteur qui rejette tout. Il faut les deux côtés, comme dans `ConfiguracionTest`.

**Choisir les cas au hasard.** Les erreurs se cachent à la limite : le 999 et le 1000, le 399 et le 400. Une table de cas qui n'a pas les deux côtés de chaque limite laisse le bord sans surveillance, et l'exercice 1 mesure combien cela coûte.

**Des tests qui dépendent de quelque chose que tu ne contrôles pas :** le réseau, l'heure de la journée, l'ordre dans lequel ils s'exécutent, le dossier depuis lequel on les lance ou un fichier qui « était déjà là ». Un test doit donner le même résultat demain et sur l'ordinateur d'une autre personne ; pour les fichiers, `@TempDir` ; pour les données, que le cas les construise lui-même.

**Commenter ou supprimer un test qui échoue « pour que ça passe ».** Le test disait quelque chose. Comprends l'échec : soit le code est faux, soit le test décrit une règle qui a changé, et alors on met à jour le test (comme dans l'exercice 2), on ne le fait pas taire.

**Raccourcir la sortie des tests avec un tube.** `mvn -B verify | tail` remplace le code de sortie par celui de `tail`, et l'échec disparaît (7.5.2). On exécute les tests sans filtrer, ou avec `set -o pipefail`.

## Exercices

**Chaque exercice part du projet de 7.6, avec ses 18 tests.** Si un exercice te laisse des tests en trop ou un changement dans le code, annule-le avant de commencer le suivant ; ainsi les décomptes que tu prédis et ceux des solutions sont les mêmes.

### Exercice 1 — La limite qui manque

La table de cas de `ClasificadorTest` a deux erreurs HTTP, 503 et 404, mais aucune près de la limite entre « a bien répondu » et « a répondu avec une erreur ». La règle dit qu'un code **supérieur ou égal à 400** fait classer le service comme tombé. **Avant de toucher à quoi que ce soit, prédis :** si quelqu'un remplace dans `Clasificador.java` la comparaison `codigo >= 400` par `codigo > 400`, combien des 18 tests passent au rouge ? Exécute-le et vérifie ta prédiction. Ensuite, ajoute à la table les cas `400, 50, CAIDO` et `399, 50, ARRIBA`, répète le changement et note quelle ligne passe au rouge et combien de tests dit maintenant le rapport.

### Exercice 2 — La question du lundi

Réponds à la question par laquelle a commencé la leçon. Change `UMBRAL_LENTO_MS` à 800 dans `Clasificador.java`. **Prédis** quelle ligne de la table passe au rouge et pourquoi, et exécute `mvn -B verify` pour le vérifier. Ensuite, mets à jour le **test**, pas le code : change les deux lignes de la limite pour qu'elles surveillent la nouvelle valeur (une valeur juste en dessous et la valeur du seuil), et vérifie que le rapport indique de nouveau `Tests run: 18, Failures: 0`. Note dans le journal de bord pourquoi le test a dû changer.

### Exercice 3 — Un nouveau test, à la main

`Formato.contar(long cantidad, String palabra)` ajoute un « s » au mot sauf quand la quantité est 1 (`1 caído`, `3 caídos`). Écris dans `FormatoTest` un test paramétré avec trois lignes : `0, servicio` (doit donner `0 servicios`), `1, caído` et `3, caído`. **Avant d'exécuter**, note combien de tests tu t'attends à ce que le rapport indique (indice : il y a maintenant trois lignes de plus). Ensuite, fais-le échouer exprès en remplaçant `cantidad == 1` par `cantidad <= 1` dans `Formato.java`, et observe combien de tests passent au rouge et de quelles classes. T'attendais-tu à ce qu'ils soient de plus d'une classe ?

### Exercice 4 — Le vert qui n'a rien testé

Renomme la classe `ResumenTest` en `ResumenPrueba`, dans le fichier et dans la déclaration de la classe. Exécute `mvn -B verify` et réponds à trois questions : cela s'est-il terminé par `BUILD SUCCESS` ? Combien de tests dit maintenant le rapport ? Quel est le code de sortie, avec `echo $?` juste après ? Rétablis le nom, vérifie que le décompte revient à 18 (si un jour un décompte ne tombe pas juste après avoir renommé ou supprimé une classe, `mvn -B clean verify` supprime `target/` et construit à partir de zéro) et écris dans le journal de bord la phrase avec laquelle tu te souviendras de lire la ligne `Tests run:`.

## Solutions

### Solution 1

**Prédiction.** Aucun ne passe au rouge : les 18 passent. Les cas d'erreur de la table sont 503 et 404, et les deux sont supérieurs à 400 avec l'une ou l'autre comparaison. Le passage de `>=` à `>` n'importe que pour la valeur 400 exacte, et la table ne la contient pas. C'est un cas réel de changement que les tests **ne détectent pas** : le défaut survit, et le vert dit que tout va bien.

Les deux nouveaux cas s'ajoutent à la fin de la table :

```java
  @CsvSource({
    "200, 142,  ARRIBA",
    "200, 999,  ARRIBA",
    "200, 1000, LENTO",
    "200, 2300, LENTO",
    "503, 95,   CAIDO",
    "404, 50,   CAIDO",
    "400, 50,   CAIDO",
    "399, 50,   ARRIBA"
  })
```

Avec les deux cas et le passage de `>=` à `>` dans `Clasificador.java`, le rapport dit :

```text
$ mvn -B verify
...
[ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.053 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[7] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <CAIDO> but was: <ARRIBA>
...
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:28 expected: <CAIDO> but was: <ARRIBA>
[ERROR] Tests run: 20, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

La ligne `[7]` est celle de `400, 50, CAIDO` : elle attendait `CAIDO` et a obtenu `ARRIBA`, parce qu'avec `>` le 400 n'est plus une erreur. Et le total est monté à 20, les 18 d'origine plus les deux nouvelles lignes. La ligne `399, 50, ARRIBA` passe dans les deux versions ; elle est là pour que l'autre côté de la limite soit surveillé lui aussi. Annule le changement de `Clasificador.java` : avec les deux cas toujours dans la table, le rapport doit dire `Tests run: 20, Failures: 0`. Dans un vrai projet, ces deux lignes restent, parce qu'elles surveillent une limite que personne ne surveillait avant ; pour poursuivre les exercices, retire-les et reviens aux 18.

### Solution 2

Avec le seuil à 800, la règle surveille une valeur que la table n'attendait pas. La ligne `[2]` échoue, `200, 999, ARRIBA` : un service de 999 millisecondes n'est plus en ligne, il est lent.

```text
$ mvn -B verify
...
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.050 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[2] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <ARRIBA> but was: <LENTO>
...
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:26 expected: <ARRIBA> but was: <LENTO>
[ERROR] Tests run: 18, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Le `expected: <ARRIBA> but was: <LENTO>` le dit. Les autres lignes continuent de passer (le 142 reste en ligne, et le 1000 et le 2300 restent lents), et aucune autre classe de tests ne casse, parce qu'aucune autre ne dépend du seuil. Ce qui échoue, c'est le test, et la raison est que la **règle métier a changé** : le test décrivait l'ancien seuil. C'est pourquoi c'est lui qu'on met à jour et non le code. Les deux lignes de la limite deviennent `200, 799, ARRIBA` (juste en dessous) et `200, 800, LENTO` (le seuil) :

```java
    "200, 142,  ARRIBA",
    "200, 799,  ARRIBA",
    "200, 800,  LENTO",
    "200, 2300, LENTO",
```

et le rapport revient au vert, avec le même total qu'au début, parce qu'on a changé le contenu de deux lignes et qu'on n'en a ajouté aucune :

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Et la question du lundi a maintenant une réponse écrite, exécutable et surveillée : un service d'exactement 800 millisecondes est lent.

### Solution 3

Le nouveau test, dans la classe `FormatoTest`, à côté de celui qu'elle a déjà :

```java
  @ParameterizedTest
  @CsvSource({"0, servicio, 0 servicios", "1, caído, 1 caído", "3, caído, 3 caídos"})
  void pluralizaSalvoCuandoEsUno(long cantidad, String palabra, String esperado) {
    assertEquals(esperado, Formato.contar(cantidad, palabra));
  }
```

Il y a trois lignes, donc le décompte passe de 18 à **21** :

```text
$ mvn -B verify
...
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Avec `cantidad <= 1` au lieu de `cantidad == 1`, le zéro cesse de porter le « s » (`0 servicio`) et ce cas échoue :

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in mx.habil.revisor.ClasificadorTest
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.007 s <<< FAILURE! -- in mx.habil.revisor.ResumenTest
[ERROR] mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lento · 1 caído · promedio —>
...
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.009 s <<< FAILURE! -- in mx.habil.revisor.FormatoTest
[ERROR] mx.habil.revisor.FormatoTest.pluralizaSalvoCuandoEsUno(long, String, String)[1] -- Time elapsed: 0.001 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <0 servicios> but was: <0 servicio>
...
[ERROR]   FormatoTest.pluralizaSalvoCuandoEsUno:22 expected: <0 servicios> but was: <0 servicio>
[ERROR]   ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya:41 expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lento · 1 caído · promedio —>
[ERROR] Tests run: 21, Failures: 2, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

**Deux** tests échouent, de **deux classes** : le nouveau (`FormatoTest`, ligne `[1]`, celle de `0 servicios`) et un qui existait déjà, `ResumenTest`, dont le cas « personne n'a répondu » attend `0 lentos` (« 0 lents »). Il est normal que la surprise soit là : les tests s'écrivent pour une chose, mais ils surveillent tout ce qui passe par là. Un changement dans une fonction partagée est plus facile à détecter si plusieurs parties du programme en dépendent. Et c'est une bonne raison de ne pas supprimer un test « redondant ».

### Solution 4

Sans la classe `ResumenTest`, *Surefire* ne la trouve plus, et le rapport dit :

```text
$ mvn -B verify
...
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
$ echo $?
0
```

`BUILD SUCCESS`, code de sortie `0`, et **16** tests au lieu de 18 : les deux de `ResumenTest` manquent. Si tu ne savais pas que le bon nombre était 18, rien ne t'aurait averti. Voilà la phrase pour le journal de bord : **« Tests run: 18, toujours ; si le nombre change et que je ne l'ai pas changé, je m'arrête. »** En rétablissant le nom, le décompte revient à 18. Un détail mesuré : avec Maven 3.10.0, `mvn -B verify` tout court donne déjà 16, parce que le compilateur détecte qu'une source a changé, recompile tout le module et ne laisse pas dans `target/test-classes` l'ancien `ResumenTest.class` ; `mvn -B clean verify` donne aussi 16 et ne dépend pas de ce comportement, donc c'est la façon sûre quand tu renommes ou supprimes des classes.

## Comment savoir que j'ai réussi

- [ ] `mvn -B verify`, depuis la racine de `~/curso-java/revisor/`, se termine par `BUILD SUCCESS` et dit `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`, et tu sais d'où sort chacun des 18 (7 + 4 + 5 + 2).
- [ ] Le `.jar` s'appelle maintenant `revisor-0.7.0.jar`, et `java -cp target/classes mx.habil.revisor.Revisor` continue d'afficher le tableau avec `promedio 843ms`.
- [ ] Tu as remplacé `>=` par `>` dans `Clasificador.java`, tu as vu la ligne `[3]` en rouge avec `expected: <LENTO> but was: <ARRIBA>`, et tu as annulé le changement.
- [ ] Tu as exécuté un seul test avec `-Dtest=Clase#metodo` puis tous, et tu sais pourquoi on les exécute tous à la fin.
- [ ] Tu as renommé `ResumenTest` en `ResumenPrueba`, tu as vu `BUILD SUCCESS` avec 16 tests, et tu as écrit dans le journal de bord comment tu te souviendras de lire le décompte.
- [ ] Tu sais expliquer à quoi sert `<scope>test</scope>` et pourquoi JUnit ne va pas dans le programme.
- [ ] Tu as noté dans ton journal de bord le nombre que dit le rapport, avec la date.

## Résumé

Réponds sans regarder la leçon :

1. Quels sont les trois temps d'un test ? Quel argument de `assertEquals` vient en premier, et que se passe-t-il si tu les inverses ?
2. À quoi sert le BOM de JUnit et que fait `<scope>test</scope>` ?
3. Pourquoi cette leçon teste-t-elle `Clasificador`, `Formato`, `Resumen` et `Configuracion`, et pas `Revision` ni `Simulador` ?
4. Pourquoi casse-t-on exprès le code avant de tenir un test pour bon ?
5. Que signifie le `[3]` dans le nom d'un test paramétré qui échoue ? Pourquoi les cas de la table se choisissent-ils autour de la limite ?
6. Que vérifie `assertThrows` et que se passe-t-il si le code ne lève rien ? Que fait `@TempDir` ?
7. Quelle différence y a-t-il entre `Failures` et `Errors` dans le rapport ?
8. Trois façons dont Maven se termine au vert sans avoir testé ce que tu crois : lesquelles sont-elles et comment te protèges-tu de chacune ?

## Pour aller plus loin

- [JUnit 6, guide de l'utilisateur](https://docs.junit.org/current/user-guide/) : les annotations, les [affirmations](https://docs.junit.org/current/writing-tests/assertions.html), les [hypothèses](https://docs.junit.org/current/writing-tests/assumptions.html) et les [tests paramétrés](https://docs.junit.org/current/writing-tests/parameterized-classes-and-tests.html) avec toutes leurs sources de données, au-delà de `@CsvSource`. Consulté le 13 octobre 2026.
- [Maven Surefire, « Inclusions and Exclusions of Tests »](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html) : quels noms de classe il exécute et comment le changer, utile quand un décompte ne tombe pas juste. Consulté le 7 octobre 2026.
- [Maven, « Introduction to the Dependency Mechanism »](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html) : les portées (`test` parmi elles), le BOM et la façon de l'importer avec `<scope>import</scope>`. Consulté le 13 octobre 2026.
- [Manuel de Bash, la commande `set`](https://www.gnu.org/software/bash/manual/html_node/The-Set-Builtin.html) : ce que fait `pipefail` au code de sortie d'un tube. Consulté le 13 octobre 2026.

---

**Précédente :** [Leçon 6 — Projet avec Maven](06-proyecto-maven.md) ·
**Suivante :** [Leçon 8 — HTTP et JSON](08-http-y-json.md)
