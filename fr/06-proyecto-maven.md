# Leçon 6 — Projet avec Maven

**Durée :** 90 min (juste : cela inclut l'installation de Maven, et la première construction télécharge des pièces depuis internet ; ne la bouscule pas)

**Ce que tu construis :** le `revisor` cesse d'être un dossier de fichiers isolés et devient un projet Maven, organisé en paquets, qui se compile, s'empaquette dans un `.jar` et s'exécute toujours avec les deux mêmes commandes

**Ce que tu apprends :** quand un fichier isolé ne suffit plus ; paquets et visibilité ; le retour de `public static void main(String[] args)` ; installer Maven 3.10.0 avec SDKMAN ; la structure standard de Maven et le `pom.xml` ; le cycle `compile`, `test`, `package`, `verify` ; ce qu'il y a dans un `.jar` et pourquoi il ne s'exécute pas encore avec `java -jar`

**D'où tu viens.** Tu arrives avec le projet de la [Leçon 5](05-errores-excepciones.md) : un dossier avec plusieurs fichiers `.java` (le modèle, la classification, le formatage, le rapport, la lecture de `servicios.txt` avec ses propres exceptions), qui s'exécute avec `java Revisor.java` et prend environ 2.5 secondes parce que la simulation attend ce que dit chaque service. Ça marche, et tu le lances avec une seule commande. Mais cette commande a des limites qui vont se faire sentir dès que le programme demandera quelque chose que tu n'as pas écrit toi-même : la prochaine leçon a besoin d'une bibliothèque de tests, et la suivante, d'une bibliothèque pour lire du JSON. Le lanceur de fichiers source ne sait pas utiliser les bibliothèques des autres, ne conserve rien de ce qu'il compile et n'a pas de forme reconnaissable. Aujourd'hui, tu transformes le `revisor` en vrai projet : avec la structure que tout développeur Java reconnaît, et avec un outil qui le compile, l'empaquette et, dès la prochaine leçon, le teste. Tu travailles dans ton dossier `~/curso-java/revisor/`.

**Ce que cette leçon ne fait pas.** Elle n'écrit pas encore de tests : c'est la [Leçon 7](07-pruebas-junit.md), qui s'appuie sur ce que tu construis aujourd'hui (le `pom.xml`, le dossier de tests qui reste vide pour l'instant et le cycle de vie de Maven). Elle ne consulte aucun réseau : le `Simulador` continue de jouer le rôle des services et cela ne change qu'à la [Leçon 8](08-http-y-json.md). Elle n'ajoute pas non plus de bibliothèques au programme (la première dépendance du programme arrive à la 8) et ne vérifie pas les services en parallèle (c'est la [Leçon 9](09-concurrencia.md)). Ce qui se règle ici, c'est **comment s'organise, se compile et s'empaquette un programme Java qui compte déjà plus de dix fichiers**.

**À propos des exécutions.** Tout ce qui figure dans cette leçon a été exécuté avec Temurin 25 et Maven 3.10.0, sous macOS, avec le terminal en UTF-8 ; l'installation de Maven avec SDKMAN et son `mvn --version` ont en outre été mesurés sur un Linux Mint 22.3, le 13 octobre 2026. Les sorties de Maven sont longues et changent à chaque exécution (téléchargements, durées, chemins), donc elles sont abrégées : on garde ce que chaque section analyse, et les trois points (`...`) marquent ce qui a été retiré ; les chemins longs sont affichés à partir de `~/curso-java`. Les messages du lanceur Java (ceux qui commencent par `Error:`) sortent dans la langue du système : ici ils sont affichés en anglais, et si ton Linux Mint est en espagnol tu verras la première ligne traduite ; le reste ne change pas. Les programmes se trouvent dans [`programas/06-proyecto-maven/`](../programas/06-proyecto-maven), dans le dépôt du cours.

## À la fin, tu seras capable de

- Expliquer quelles trois choses le lanceur de fichiers source ne fait pas et ce qu'un projet Maven apporte à ton programme.
- Déclarer un paquet, placer le fichier dans le dossier qui lui correspond et dire quelles classes chacune voit selon sa visibilité.
- Installer Maven 3.10.0 avec SDKMAN et reconnaître dans `mvn --version` qu'il utilise ton Temurin 25.
- Lire un `pom.xml` et dire à quoi sert chaque élément, y compris la raison de fixer la version de chaque *plugin*.
- Dire quelles phases exécute `mvn -B verify`, ce que chacune laisse dans `target/` et ce qu'il y a dans le `.jar`.
- Déménager le `revisor` vers la structure Maven et l'exécuter depuis `target/classes` et depuis son `.jar`.

## Le pourquoi avant le comment

Jusqu'à la leçon précédente, ton outil de construction a été `java` lui-même : tu lui donnes un fichier `.java`, il le compile en mémoire avec les autres du dossier et l'exécute. Pour apprendre, c'est parfait. Pour un programme qui va grandir, il a trois limites, et les trois se règlent avec le même outil :

1. **Il ne peut pas utiliser les bibliothèques des autres.** Le lanceur de la [Leçon 1](01-instalacion.md) (`java Revisor.java`) compile tes fichiers et rien d'autre. La prochaine leçon a besoin d'une bibliothèque de tests, et la Leçon 8, d'une bibliothèque pour lire du JSON ; sans moyen de déclarer des dépendances, il n'y a pas de chemin.
2. **Il ne laisse rien.** Il compile à chaque exécution et ne conserve pas le résultat : il n'existe pas de « programme compilé » que tu puisses livrer, ni de moyen de dire « compile, mais n'exécute pas encore ». Il n'a pas non plus d'endroit où ranger des tests ni personne pour les lancer.
3. **Il n'a pas de forme.** Quand le programme compte dix fichiers, trois personnes et deux ans, « un dossier avec des fichiers » cesse d'être une structure. Tous les projets Java du monde partagent une convention de dossiers, et c'est cette convention qui permet à un outil, ou à une nouvelle personne, de les comprendre sans que personne ne les lui explique.

L'outil, c'est **Maven**, qui compile, teste et empaquette les projets Java à partir d'un fichier qui les décrit. On l'installe une fois, et dès lors chaque projet Java qui utilise Maven se construit avec la même commande. Pour l'utiliser, il faut franchir deux étapes préalables que la leçon explique dans l'ordre : mettre le code dans des **paquets** (la façon qu'a Java d'ordonner ses classes par nom et par dossier), et apprendre quelles classes chacune voit parmi les autres (la **visibilité**).

La leçon a quatre parties : **paquets et visibilité** (6.1) ; **Maven** : l'installer, sa structure, son `pom.xml`, son cycle de vie et le déménagement du `revisor` (6.2) ; **le cycle complet et le `.jar`** (6.3) ; et **l'étape du projet** (6.4). Avec cette destination en tête, voici ce que tu verras à la fin, quand tu construiras le projet complet :

```text
$ mvn -B verify
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
...
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] No tests to run.
[INFO] 
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.6.0.jar
[INFO] BUILD SUCCESS
```

Treize fichiers compilés, un `.jar` construit et une ligne qui passe inaperçue : `No tests to run.`, « il n'y a aucun test à exécuter ». À la fin, tu sauras d'où vient chaque ligne, ce qui se trouve dans le `.jar`, et pourquoi ce `BUILD SUCCESS` avec zéro test est justement la question par laquelle commence la Leçon 7.

## Les concepts

### 6.1 Paquets et visibilité

#### 6.1.1 Ce que le lanceur ne fait pas

Le lanceur de fichiers source (`java Revisor.java`) est une commodité que Java a ajoutée pour qu'apprendre n'exige pas d'outil de construction. Il compile en mémoire, à chaque exécution, les fichiers dont le programme a besoin, exécute le résultat et ne laisse rien sur le disque ([JEP 458](https://openjdk.org/jeps/458), définitif depuis Java 22). C'est parfait pour ce que tu as fait jusqu'ici et il a trois limites qui font maintenant mal. La première peut être provoquée en deux lignes. Écris un fichier qui utilise JUnit, la bibliothèque de tests que tu vas utiliser dans la Leçon 7 :

```java
// fig06_01.java
import org.junit.jupiter.api.Test;

void main() {
  IO.println("revisor: todavía no revisa nada");
}
```

C'est le programme habituel, avec une ligne en plus : un `import` d'une classe de JUnit. Un `import` (tu le connais par les fichiers qui ont utilisé `java.util`) dit au compilateur de quel paquet vient un nom. Exécute-le :

```text
$ java fig06_01.java
fig06_01.java:2: error: package org.junit.jupiter.api does not exist
import org.junit.jupiter.api.Test;
                            ^
1 error
error: compilation failed
```

Le compilateur cherche le paquet `org.junit.jupiter.api` et ne le trouve pas, parce que le lanceur ne sait pas où le prendre : il n'existe aucun endroit où déclarer que ton programme dépend d'une bibliothèque. Quand tu vois `package ... does not exist` avec un nom qui n'est pas le tien, cela signifie presque toujours ceci : il manque une dépendance, ce n'est pas une faute de frappe.

La deuxième limite ne produit pas de message, mais une habitude : comme le lanceur compile tout à chaque exécution et ne conserve rien, le « compilé » n'existe pas comme produit. Il n'y a pas de `.class` à livrer, ni de moyen de dire « compile, mais n'exécute pas encore ». Avec treize fichiers, le coût en temps est une fraction de seconde (mesuré : environ 0.3 seconde sur cette machine), mais le coût en ordre grandit avec le projet. La troisième limite est celle des tests : le lanceur ne sait pas ce que c'est, ni où ils vivent, ni comment les exécuter tous ensemble.

Avant Maven, il y a encore une pièce que Java exige et que ton projet n'a pas encore : les paquets.

#### 6.1.2 Paquets : un nom et un dossier

Imagine que deux personnes écrivent, chacune de son côté, une classe appelée `Servicio`. Si les deux se retrouvent dans le même programme, le compilateur ne peut pas savoir laquelle est laquelle. Le problème est ancien et la solution de Java aussi : chaque classe vit dans un **paquet**, qui est un nom avec des points —`mx.habil.revisor`— et qui fait partie du nom complet de la classe. Ton `Servicio` s'appelle alors `mx.habil.revisor.Servicio`, et celui de l'autre personne, `com.otraempresa.inventario.Servicio`. Dans le JDK, c'est la même chose : il existe `java.util.List` et il existe `java.awt.List`, deux choses différentes avec le même nom court.

La convention pour choisir le nom est d'utiliser le domaine de celui qui écrit le code, à l'envers (`habil.mx` devient `mx.habil`), suivi du nom du projet (`revisor`). Un domaine est unique au monde, donc le nom l'est aussi. Les règles du langage sont au nombre de trois, et les trois figurent dans la [spécification, chapitre 7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html) :

1. **Le fichier déclare son paquet à la première ligne** (sans compter les commentaires) : `package mx.habil.revisor;`.
2. **Le dossier porte le même nom que le paquet**, avec un sous-dossier pour chaque point : le fichier vit dans `mx/habil/revisor/`.
3. **Pour utiliser une classe d'un autre paquet, on l'importe** : `import mx.habil.revisor.Estado;`. Les classes d'un même paquet se voient entre elles sans `import`, et celles de `java.lang` (comme `String`) toujours.

Voyons cela avec le plus petit programme que l'on puisse écrire dans un paquet. C'est une classe, `Hola`, avec un `main` qui affiche le nom complet de sa propre classe :

```java
// fig06_02/src/mx/habil/revisor/Hola.java

package mx.habil.revisor;

/** Una clase mínima dentro de un paquete. */
public final class Hola {

  private Hola() {}

  /** Imprime el nombre completo de su propia clase. */
  public static void main(String[] args) {
    System.out.println("hola desde " + Hola.class.getName());
  }
}
```

Trois choses sont nouvelles. La première est le `package`. La deuxième est que cette classe n'est plus un fichier compact : elle a sa `class` et son `main` complet, `public static void main(String[] args)`, que la section 6.1.4 explique. La troisième, `Hola.class.getName()`, demande à Java le nom de la classe tel qu'il le connaît ; elle sert ici à voir que le paquet en fait partie. (Les commentaires entre `/**` et `*/` sont de la documentation ; ils sont expliqués en 6.1.3.)

Compiler et exécuter un fichier avec paquet, ce n'est plus `java Hola.java`. Cela se fait en deux étapes, et les deux ont un détail :

```text
$ cd fig06_02
$ javac -d out src/mx/habil/revisor/Hola.java
$ java -cp out mx.habil.revisor.Hola
hola desde mx.habil.revisor.Hola
```

`javac -d out` dit « dépose les `.class` dans le dossier `out` », et `javac` fait quelque chose qui ne se voit pas : il **crée les sous-dossiers du paquet** (`out/mx/habil/revisor/Hola.class`). Les `.class` se retrouvent dans un dossier de même forme que celui du code. À l'exécution, `-cp out` est l'abréviation de *classpath*, « le chemin de classes » : la liste des endroits où la JVM cherche les `.class`. Avec cela, la JVM sait par où commencer à chercher, et `mx.habil.revisor.Hola` est le **nom complet** de la classe, avec son paquet : la JVM le convertit en chemin `mx/habil/revisor/Hola.class` et le cherche à l'intérieur de `out`.

Voyons maintenant les deux erreurs que tout le monde commet la première fois. La première est d'écrire seulement le nom court :

```text
$ java -cp out Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola
```

La JVM cherche `Hola.class` directement dans `out`, ne le trouve pas (il est deux niveaux plus bas) et le dit avec une exception au nom long, `ClassNotFoundException` : « il n'y a aucune classe appelée `Hola` ». La solution est d'écrire le nom complet. La deuxième erreur consiste à « entrer dans le dossier de la classe » et à exécuter de là, en pensant que si tu te places à côté du fichier, elle le trouvera :

```text
$ cd out/mx/habil/revisor
$ java Hola
Error: Could not find or load main class Hola
Caused by: java.lang.NoClassDefFoundError: Hola (wrong name: mx/habil/revisor/Hola)
```

Ici, elle trouve bien un `Hola.class`, mais son vrai nom est `mx/habil/revisor/Hola`, pas `Hola`, et la JVM le rejette avec `(wrong name: mx/habil/revisor/Hola)` : « le fichier dit que la classe s'appelle autrement ». C'est la même règle vue de l'autre côté. **Le nom de la classe et le chemin de son fichier doivent coïncider, et la JVM le vérifie.** C'est pourquoi les programmes avec paquets s'exécutent toujours depuis la racine des classes (`out`, ou `target/classes` avec Maven), avec le nom complet.

Reste une question que le compilateur a le droit de te poser : que se passe-t-il si le fichier déclare un paquet et se trouve dans un autre dossier ? La réponse est moins dramatique qu'il n'y paraît, et mieux vaut la connaître telle qu'elle est. On a déplacé `Clasificador.java` un dossier au-dessus de celui qui lui revenait, en laissant sa ligne `package mx.habil.revisor;` intacte, et on a demandé à `javac` de compiler `Revision.java` en cherchant les autres classes par dossier :

```text
$ javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java
src/main/java/mx/habil/revisor/Revision.java:14: error: cannot find symbol
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
                                ^
  symbol:   variable Clasificador
  location: class Revision
1 error
```

Le compilateur part chercher `mx/habil/revisor/Clasificador.java` parce que `Revision` le mentionne, ne le trouve pas là où le nom du paquet dit qu'il devrait être, et conclut qu'il n'existe pas. Ce qui surprend, c'est l'inverse : **quand on lui remet explicitement tous les fichiers, `javac` ne se plaint pas**, et Maven procède ainsi (mesuré : le projet compile et les `.class` se retrouvent où il faut même si le `.java` est dans le mauvais dossier). Le dossier qui ment ne casse pas la compilation de Maven ; il casse tout ce qui cherche par dossier : les éditeurs, les recherches, la compilation incrémentale, et la personne qui viendra après toi. C'est une erreur silencieuse, des pires : la règle est que le dossier dise toujours la vérité, même si le compilateur, aujourd'hui, te laisse mentir.

Il y a une dernière erreur de cette famille, celle du fichier qui ne porte pas le nom de sa classe publique. Le fichier `Revisor.java` de cet exemple déclare une classe avec un autre nom :

```java
package mx.habil.revisor;

public final class Principal {}
```

```text
$ javac -d out mx/habil/revisor/Revisor.java
mx/habil/revisor/Revisor.java:3: error: class Principal is public, should be declared in a file named Principal.java
public final class Principal {}
             ^
1 error
```

Une classe `public` doit vivre dans un fichier du même nom. C'est une autre coïncidence que le compilateur exige bel et bien. Dans les fichiers compacts des premières leçons, cela ne se remarquait pas, parce que la classe implicite prend le nom du fichier (`fig02_04.java` définit une classe `fig02_04`).

#### 6.1.3 Visibilité : ce que chaque classe voit des autres

Avec les paquets apparaît une question qui n'avait pas de sens avant : une classe de `mx.habil.otro` peut-elle utiliser une classe de `mx.habil.revisor` ? La réponse est donnée par la **visibilité**, qui s'écrit avec un mot au début de la déclaration. Java a quatre niveaux, et le cours en utilise trois :

| Ce qu'on écrit | Qui la voit | À quoi ça sert |
|---|---|---|
| `private` | seulement la classe où elle est déclarée | les détails internes : un champ, un constructeur que personne ne doit appeler |
| *(rien)* | n'importe quelle classe du **même paquet** | presque tout le projet : c'est la visibilité « de paquet » |
| `public` | n'importe quelle classe de n'importe quel paquet | ce qui est offert au monde : le point d'entrée |

(Le quatrième, `protected`, mêle la visibilité de paquet à l'héritage entre classes ; le cours n'utilise pas l'héritage, et il n'apparaît pas.)

La règle est : **la visibilité la plus étroite qui fonctionne**. Ce n'est pas une question de style. Chaque chose `public` est une promesse faite à qui l'utilise : si demain tu la changes ou la retires, tu casses le code de cette personne. Une classe de paquet, tu ne la promets qu'à ton propre paquet, et tu peux la changer sans prévenir personne. Jusqu'ici, tout était de paquet sans que tu le remarques : tes classes ne portaient aucun mot devant. Maintenant qu'il y a plus d'un paquet dans le monde, le compilateur le fait respecter. Deux fichiers d'exemple : un `Estado` de paquet dans `mx.habil.revisor`, et une classe d'un autre paquet, `mx.habil.otro`, qui veut l'utiliser.

```java
package mx.habil.revisor;

enum Estado {
  ARRIBA,
  CAIDO
}
```

```java
package mx.habil.otro;

import mx.habil.revisor.Estado;

class Intruso {
  Estado estado = Estado.ARRIBA;
}
```

En les compilant ensemble :

```text
$ javac -d out src/mx/habil/revisor/Estado.java src/mx/habil/otro/Intruso.java
src/mx/habil/otro/Intruso.java:3: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
import mx.habil.revisor.Estado;
                       ^
src/mx/habil/otro/Intruso.java:6: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
  Estado estado = Estado.ARRIBA;
  ^
src/mx/habil/otro/Intruso.java:6: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
  Estado estado = Estado.ARRIBA;
                  ^
3 errors
```

Voilà ce qui se passe quand une classe de `mx.habil.otro` essaie d'utiliser un `Estado` de visibilité de paquet : `Estado is not public in mx.habil.revisor; cannot be accessed from outside package`. Le message est clair, et la solution n'est presque jamais de « lui mettre `public` » : c'est de se demander si cette autre classe devrait utiliser `Estado`.

Dans le `revisor`, **tout est de visibilité de paquet sauf une classe : `Revisor`**, le point d'entrée, qui est la seule chose dont quelqu'un de l'extérieur ait besoin. Voici, par exemple, le `Estado` de ton projet dans sa nouvelle maison :

```java
// fig06_03/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

C'est le même `enum` que dans la leçon 3, avec une seule ligne de plus : le `package` du haut. Il ne porte aucun mot de visibilité devant, donc il est de paquet, et toutes les classes de `mx.habil.revisor` le voient et aucune de l'extérieur. À l'unique endroit où `public` apparaît (le `Revisor`, en 6.2.6), il faut en plus un commentaire de documentation, écrit entre `/**` et `*/`, qui s'appelle **Javadoc** : c'est le commentaire que les outils Java (et ton éditeur) affichent quand quelqu'un demande « qu'est-ce que c'est ? ». La règle du projet à partir de cette leçon est que toute classe et toute méthode `public` porte un Javadoc d'au moins une phrase terminée par un point ; dans la [Leçon 10](10-el-programa-terminado.md), un outil le vérifiera. Comme presque tout est de paquet, ce Javadoc obligatoire se réduit au minimum.

#### 6.1.4 `public static void main(String[] args)` : le retour de la forme longue

Dans les leçons 1 à 5, tu écrivais `void main()` et rien de plus. Maintenant le `Revisor` revient à la forme que tu as vue dans n'importe quel code Java d'autrui : une classe, et à l'intérieur, une méthode `main` avec trois mots de plus :

- `public` : la JVM l'appelle de l'extérieur, donc il doit être visible de tous.
- `static` : il appartient à la classe, pas à un objet. On peut l'appeler sans en avoir créé aucun.
- `String[] args` : la liste des arguments que la personne a écrits après le nom du programme sur la ligne de commande (`java ... Revisor uno dos`). Aujourd'hui elle ne sert pas ; les Leçons 9 et 10 s'en servent.

On est en droit de se demander pourquoi revenir à une forme plus longue si Java 25 admet `void main()`. La réponse honnête a deux parties. La première est que **la forme courte continue de fonctionner dans un paquet** : on peut le vérifier, et le programme tourne. La seconde est qu'elle a une condition que le `Revisor` ne remplit pas, et l'erreur est instructive. Une classe qui ne fait que contenir un `main` n'a aucune raison de laisser quelqu'un l'instancier, donc on lui met un constructeur `private`, ce que fait `private Revisor() {}`. Mais un `main` d'instance exige que la JVM puisse créer l'objet. Cette classe, dans un paquet, a le `main` court et le constructeur privé :

```java
package mx.x;

public final class Prueba {
  private Prueba() {}

  void main() {
    IO.println("hola");
  }
}
```

```text
$ javac -d out mx/x/Prueba.java
$ java -cp out mx.x.Prueba
Error: no non-private zero argument constructor found in class mx.x.Prueba
remove private from existing constructor or define as:
   public mx.x.Prueba()
```

`no non-private zero argument constructor found` : la JVM cherche un constructeur sans paramètres qui ne soit pas privé, et il n'y en a pas. La forme classique ne dépend pas de cela, fonctionne avec toutes les versions du JDK depuis la première, et c'est celle que reconnaissent tous les outils et tout le code d'autrui que tu liras. Pour un programme qui va vivre des années, c'est le bon motif : **ne pas dépendre d'une commodité récente si l'ancienne forme ne coûte rien.** `IO.println` continue lui aussi de fonctionner dans n'importe quelle classe, mais le projet utilise `System.out`, que tu connais déjà depuis la leçon 5, parce que c'est ce que tu trouveras partout.


### 6.2 Maven : l'installer, sa structure et son `pom.xml`

#### 6.2.1 Ce que fait Maven, en quatre tâches

Maven est un outil de construction : il reçoit un projet Java et fait avec lui, toujours dans le même ordre, ce que tu faisais à la main. Ses tâches sont au nombre de quatre. **Compiler** le code (il appelle `javac` pour toi, avec les bonnes options). **Exécuter les tests** et s'arrêter si l'un échoue. **Empaqueter** le résultat dans un fichier `.jar` (une archive compressée de classes, semblable à un `.zip`). Et **obtenir les dépendances** : quand ton projet dit « j'ai besoin de la bibliothèque X version Y », Maven la télécharge depuis un dépôt public, [Maven Central](https://central.sonatype.com/), la stocke sur ton ordinateur et la met à portée du compilateur.

Ce qui rend Maven spécial est une idée qui a un nom : **la convention plutôt que la configuration**. Au lieu que chaque projet raconte dans un fichier de configuration où est son code, où sont ses tests et où il dépose ses résultats, Maven fixe une structure de dossiers que tout le monde utilise, et le fichier du projet ne dit que ce qui sort de l'ordinaire. La conséquence est celle qui compte : qui connaît un projet Maven les connaît tous, et les outils (l'éditeur, le serveur d'intégration continue, l'analyseur de code) savent où regarder sans qu'on le leur dise. Le [guide d'introduction officiel](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) raconte la même chose en cinq minutes de lecture.

Maven ne fait pas le travail lui-même. C'est un orchestrateur : chaque tâche est réalisée par une pièce spécialisée appelée ***plugin*** (le compilateur en est un, celui qui exécute les tests en est un autre, celui qui assemble le `.jar` en est un autre), et Maven décide quand appeler lequel. Cette division a une conséquence pratique que tu verras dans le `pom.xml` : chaque *plugin* a sa propre version.

#### 6.2.2 Installer Maven 3.10.0 avec SDKMAN

Maven s'installe avec le même outil que celui avec lequel tu as installé Java dans la [Leçon 1](01-instalacion.md) : SDKMAN. Comme pour Java, il travaille dans ton dossier personnel, sans `sudo` et sans toucher aux programmes du système, et te laisse choisir la version exacte. Le cours fixe **Maven 3.10.0**, que la [page de téléchargement de Maven](https://maven.apache.org/download.cgi) désigne comme la version actuelle et recommandée pour tous (elle demande un JDK 8 ou supérieur pour s'exécuter ; avec ton Temurin 25, c'est largement suffisant). Fixer la version compte pour une raison que l'on voit à l'écran : mesuré avec Maven 3.9.11 sur Java 25, au démarrage il affiche quatre avertissements `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` (ils viennent d'une bibliothèque interne de Maven, pas de ton code) ; la 3.10.0 ne les affiche pas. Ce qu'on veut, c'est que ta sortie soit la même que celle de cette leçon.

D'abord, vérifie que SDKMAN la propose. `sdk list maven` montre les versions disponibles ; le 13 octobre 2026, sur un Linux Mint 22.3, la liste comprenait la `3.10.0` (et aussi des versions `4.0.0-rc-…`, candidates de la prochaine version majeure, qui n'est pas encore définitive et que le cours n'utilise pas). Ensuite, installe-la avec son numéro complet :

```text
$ sdk install maven 3.10.0
...
Done installing!

Setting maven 3.10.0 as default.
```

Contrairement à Java, ici le numéro, c'est toi qui l'écris tel quel : `3.10.0` est la version du cours, et elle ne change pas même si d'autres sortent. La dernière ligne dit qu'elle la laisse par défaut. Si, à la place, SDKMAN te **demande** si tu veux qu'elle soit la version par défaut, réponds oui, avec `Enter` ou `Y` ; et si tu as répondu non, tu le corriges avec `sdk default maven 3.10.0`. Maintenant, comme dans la Leçon 1, **ouvre un nouveau terminal**, pour qu'il prenne en compte les changements de SDKMAN, et vérifie :

```text
$ mvn --version
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
Maven home: ~/.sdkman/candidates/maven/current
Java version: 25.0.3, vendor: Eclipse Adoptium, runtime: ~/.sdkman/candidates/java/25.0.3.0+9-tem
...
OS name: "linux", ... arch: "amd64"
```

C'est la sortie mesurée le 13 octobre 2026 sur la Mint 22.3 (abrégée : les trois points marquent ce qui a été retiré, et les chemins ont été raccourcis avec `~` ; Maven les affiche en entier, comme `/home/<ton utilisateur>/.sdkman/candidates/maven/current`). Sur la tienne, tu dois reconnaître trois choses. **`Apache Maven 3.10.0`** à la première ligne : la version du cours. **`Java version: 25.x`**, avec le correctif que tu as, et **`vendor: Eclipse Adoptium`** : Maven utilise ton Temurin 25 (Adoptium est le projet qui publie Temurin). Et les deux chemins, `Maven home` et `runtime`, à l'intérieur de `~/.sdkman/candidates/` : les deux outils viennent de SDKMAN. Si `Java version` indique autre chose que 25, Maven utilise un autre JDK : soit le terminal est ancien, soit une variable `JAVA_HOME` pointe ailleurs. SDKMAN tient cette variable à jour quand tu changes de version avec `sdk default` ou `sdk use` (l'encadré de la Leçon 1, 1.1.7) ; vérifie-la avec `echo $JAVA_HOME`.

#### 6.2.3 La structure standard de dossiers

Maven installé, il reste à lui donner un projet de la forme qu'il attend. La voici, identique dans tous les projets Maven du monde :

```text
revisor/
  pom.xml                         ← la descripción del proyecto
  servicios.txt                   ← la configuración del revisor (sigue en la raíz)
  src/
    main/java/mx/habil/revisor/   ← el código del programa
    test/java/mx/habil/revisor/   ← el código de las pruebas
  target/                         ← lo que Maven genera; se puede borrar entero
```

Chaque dossier a un seul rôle. **`src/main/java`** est le code du programme, avec un sous-dossier pour chaque point du nom du paquet, comme dans `out` tout à l'heure. **`src/test/java`** est le code des tests, avec **la même structure de paquets** que le programme : le test de `Clasificador`, que tu écriras dans la Leçon 7, sera `mx.habil.revisor.ClasificadorTest`. Que les tests vivent dans le même paquet n'est ni un hasard ni un ornement : c'est ce qui leur permet de voir les classes de visibilité de paquet que le programme garde fermées au monde (6.1.3). Un test qui devrait exiger `public` pour tout obligerait à ouvrir le programme rien que pour pouvoir le tester.

**`target/`** est ce que Maven produit : les `.class` du programme dans `target/classes`, ceux des tests dans `target/test-classes`, les rapports de tests et le `.jar`. C'est un dossier **jetable** : on peut le supprimer en entier à tout moment et il se régénère avec une commande, parce que tout ce qu'il contient sort de ce qui est dans `src`. De là, deux règles : on n'édite jamais rien dans `target`, et on ne l'envoie jamais dans un dépôt (si un jour tu conserves ce projet avec Git, ajoute `target/` à son fichier `.gitignore`). `servicios.txt` reste à la racine parce que le programme le cherche par un chemin relatif, depuis le dossier où il s'exécute, et tu exécuteras toujours depuis la racine du projet.

#### 6.2.4 Le `pom.xml`, ligne par ligne

Le fichier qui décrit le projet s'appelle `pom.xml` (*POM* est le sigle de « modèle objet du projet » ; `.xml` est le format). **XML** est un format de texte fait de balises imbriquées : chaque élément s'ouvre avec `<nom>` et se ferme avec `</nom>`, et ce qu'il y a dedans est sa valeur ou ses enfants. Il est plus bruyant que le JSON de la Leçon 8, mais c'est celui que Maven a choisi et tu ne l'écriras pas souvent à partir de zéro ; tu le liras, et c'est pourquoi il convient de le comprendre une fois en entier. Voici celui du projet, au complet :

```xml fig06_03/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.6.0</version>

  <properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

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

**Les données du projet.** `<modelVersion>4.0.0</modelVersion>` est la version du format du `pom.xml` lui-même ; c'est toujours `4.0.0` et on l'écrit sans y penser. Les trois qui suivent, `groupId`, `artifactId` et `version`, sont les **coordonnées** du projet, l'adresse avec laquelle n'importe quel projet Maven du monde peut se référer à celui-ci. Le `groupId` est celui qui le publie (`mx.habil`, comme le nom du paquet) ; l'`artifactId` est le nom du projet (`revisor`) ; et la `version` dit où il en est. Ici c'est `0.6.0` parce que c'est l'état du cours à la Leçon 6 ; chaque leçon la fera monter, et la 10 la laissera à `1.0.0`. Les coordonnées ressemblent à une formalité jusqu'à ce que, dans la Leçon 7, tu déclares ta première dépendance : une dépendance **est** un `groupId`, un `artifactId` et une `version`. Et elles décident d'une chose que tu verras en 6.3 : le nom du `.jar`.

**Les propriétés.** `<properties>` regroupe des valeurs nommées que d'autres parties du fichier (ou des *plugins*) lisent. Deux sont indispensables. `maven.compiler.release` à `25` dit au compilateur « compile comme Java 25 », ce qui inclut une garantie que l'ancien couple `source`/`target` ne donnait pas : avec `release`, le compilateur ne se contente pas de générer le format de la version demandée, il **rejette toute classe du JDK qui n'existait pas dans cette version**. Et elle sert d'alerte précoce ; voici ce qui se produit si quelqu'un essaie de construire le projet avec un JDK 21 installé :

```text
$ mvn -B compile
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile (default-compile) on project revisor: Fatal error compiling: error: release version 25 not supported -> [Help 1]
```

`release version 25 not supported` : le JDK que Maven utilise ne sait pas compiler pour Java 25. C'est l'erreur que voit celui qui a installé le `default-jdk` de Mint (Java 21, [Leçon 1](01-instalacion.md)) et celui qui suit un tutoriel avec une autre version : au lieu d'échouer à moitié avec une erreur étrange, le projet refuse dès la première seconde et dit pourquoi. L'autre propriété, `project.build.sourceEncoding`, à `UTF-8`, déclare l'encodage de tes fichiers `.java` ; on l'écrit explicitement pour que le résultat ne dépende pas de l'ordinateur où l'on compile, et pour que les accents de `"sin conexión"` (« sans connexion ») soient toujours les mêmes.

**Les *plugins*.** Dans `<build>`, `<plugins>` déclare les trois dont le projet a besoin aujourd'hui : le compilateur (`maven-compiler-plugin`), celui qui exécute les tests (`maven-surefire-plugin`, qui n'a aujourd'hui aucun test à exécuter et que la Leçon 7 met au travail) et celui qui assemble le `.jar` (`maven-jar-plugin`). Chacun porte sa `<version>` écrite, et c'est une décision, pas un oubli. Maven peut bien fonctionner sans elle, parce qu'il apporte une version par défaut de chaque *plugin* essentiel ; voici ce qui se passe si tu retires la version de l'un d'eux :

```text
$ mvn -B verify
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for mx.habil:revisor:jar:0.6.0
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-surefire-plugin is missing.
```

Maven prévient que la version manque et que « les futures versions de Maven pourraient cesser d'accepter de tels projets ». Il y a une raison plus pratique : **si tu ne fixes pas la version, ta construction change quand Maven change**, pas quand tu le décides. Le même projet, la même commande, et un résultat différent le jour où quelqu'un met l'outil à jour. Fixer la version de chaque pièce est ce qui rend une construction reproductible : le guide de Maven sur les [constructions reproductibles](https://maven.apache.org/guides/mini/guide-reproducible-builds.html) traite de ce problème et commence par demander que les *plugins* soient dans des versions précises et à jour. Il en ira de même pour les dépendances.

Un dernier détail avant de s'en servir. Le `pom.xml` ci-dessus ne porte pas la déclaration `<?xml ... ?>` qui figure dans presque tous les exemples d'internet. Elle est facultative, et la retirer évite une erreur classique : cette déclaration doit être la toute première chose du fichier, sans commentaire ni espace avant, et qui l'écrit puis ajoute un commentaire au-dessus obtient un fichier invalide avec un message qui n'aide pas.

#### 6.2.5 Le cycle de vie

Maven n'exécute pas des *plugins* isolés, il exécute des **phases**, et chaque phase appelle les *plugins* qui lui correspondent. Les phases forment une chaîne dans un ordre fixe, le **cycle de vie** ([documentation officielle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)), et une règle simple le gouverne : **quand on demande une phase, Maven exécute d'abord toutes les précédentes.** Celles que tu utiliseras dans le cours sont au nombre de quatre :

| Phase | Ce qu'elle fait | Ce qu'elle laisse |
|---|---|---|
| `compile` | compile le code de `src/main/java` | `target/classes` |
| `test` | compile les tests et les exécute (jusqu'à la Leçon 7, il n'y en a aucun) | `target/test-classes` et `target/surefire-reports` |
| `package` | assemble le `.jar` avec ce qui est compilé | `target/revisor-0.6.0.jar` |
| `verify` | vérifications finales sur le paquet (pour l'instant, aucune de plus) | — |

Par la règle ci-dessus, `mvn verify` compile, teste et empaquette ; `mvn test` compile et teste ; et `mvn compile` compile seulement. Dans le cours, on utilise `verify` comme « la commande complète » : dans la Leçon 10, tu y accrocheras les contrôles de qualité, et la commande que tu connais déjà ne changera pas. Et toujours avec `-B` (*batch*, « par lots ») : il dit à Maven de ne pas utiliser de couleurs ni de barres de progression et de ne poser aucune question, ce qui est la façon de demander une sortie propre que l'on puisse copier, coller dans un message ou lire sur un serveur sans écran.

#### 6.2.6 Le déménagement du `revisor` vers un projet

Il est temps de faire ce que montre l'arborescence. Ce sont trois étapes et une amélioration facultative, et aucune ne change ce que fait le programme. Depuis `~/curso-java/revisor/` :

```text
$ mkdir -p src/main/java/mx/habil/revisor src/test/java/mx/habil/revisor
$ mv *.java src/main/java/mx/habil/revisor/
```

`mkdir -p` crée un dossier et tous ceux qui manquent sur son chemin. `mv` déplace les fichiers ; fais attention à l'exécuter depuis `revisor/` et à ce que `servicios.txt` **ne** soit **pas** déplacé (le `*.java` n'y touche pas). Maintenant, chaque fichier a besoin de sa ligne `package`. La faire à la main dans treize fichiers est un travail ennuyeux, alors cette boucle du terminal le fait pour toi : pour chaque fichier, elle écrit la nouvelle ligne puis le contenu original dans un fichier temporaire, et le renomme par-dessus l'original :

```text
$ for f in src/main/java/mx/habil/revisor/*.java; do { printf 'package mx.habil.revisor;\n\n'; cat "$f"; } > "$f.nuevo" && mv "$f.nuevo" "$f"; done
```

Vérifie en ouvrant n'importe lequel : la première ligne doit être `package mx.habil.revisor;`, suivie d'une ligne vide. Il reste deux choses que le terminal ne peut pas faire pour toi, parce qu'elles demandent de comprendre le code.

**La première est le `Revisor`.** Ton `Revisor.java` de la leçon 5 était un fichier compact : un `main` sans classe autour. Maintenant c'est une classe, la seule publique du projet, avec son constructeur privé (6.1.3) et le `main` de la forme longue (6.1.4). Il est court, donc le voici en entier :

```java
// fig06_03/src/main/java/mx/habil/revisor/Revisor.java

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

Remarque ce qui n'a **pas** changé : le corps du `try` est celui de la leçon 5, ligne par ligne. Il lit la configuration, vérifie les services dans l'ordre de la liste, construit le rapport, affiche le tableau ; et si la configuration est mauvaise, le `catch` affiche le message en espagnol sur la sortie d'erreur. La seule nouveauté est l'enveloppe, et trois choses qui changent de signification : le fichier compact voyait tout le module `java.base` sans le demander, et une classe normale a besoin de ses propres `import` (c'est pourquoi `Path` et `List` apparaissent) ; le `main` est maintenant celui que la JVM appelle (et c'est pourquoi il doit être `public static`) ; et les classes qu'il voyait auparavant « parce qu'elles étaient dans le même dossier », il les voit maintenant parce qu'elles sont dans le même paquet.

**La seconde est une possibilité qu'ouvre le paquet, et elle est facultative.** Dans les leçons 3 à 5, sans paquet, chaque fois qu'il fallait `Respondio`, on devait l'écrire `Respuesta.Respondio`, parce que **d'un paquet sans nom on ne peut rien importer** (si tu essaies, le compilateur dit `package R does not exist`). Avec un paquet, le `import mx.habil.revisor.Respuesta.Respondio;` fonctionne, et le code peut dire `Respondio` tout court. Les tests que tu écriras dans la Leçon 7 s'en servent ; le code du programme, tel que tu l'apportes de la leçon 5, continue de fonctionner sans y toucher, et tu peux le laisser ainsi.

#### 6.2.7 La première construction

Le projet déménagé, voici la première construction. Depuis la racine (`~/curso-java/revisor/`, là où se trouve le `pom.xml`), et avec le `pom.xml` ci-dessus, qui n'a pas encore de tests :

```text
$ mvn -B compile
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------------------< mx.habil:revisor >--------------------------
[INFO] Building revisor 0.6.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.5.0:resources (default-resources) @ revisor ---
[INFO] skip non existing resourceDirectory ~/curso-java/revisor/src/main/resources
[INFO] 
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

Lis-le de haut en bas, parce que chaque ligne `--- plugin:version:objectif ---` te dit quelle pièce a travaillé. `resources:resources` copie les fichiers qui ne sont pas du code (ici il n'y en a pas : « skip non existing resourceDirectory »). `compiler:compile` fait le travail : `Compiling 13 source files with javac [debug release 25] to target/classes` dit combien de fichiers il a compilés, avec quelles options (`debug` signifie qu'il conserve les noms des variables, ce qu'explique le paragraphe suivant) et où il a déposé les `.class`. Et `BUILD SUCCESS` est l'avis qu'aucune phase n'a échoué. La première fois que tu lanceras Maven, tu verras en plus beaucoup de lignes `Downloading` qui ont été retirées ici : Maven télécharge ses propres *plugins* depuis Maven Central et les range dans `~/.m2/repository`, un dossier caché de ton répertoire personnel qui sert de **cache local**. Cela explique deux choses : la première exécution est lente et a besoin d'internet, et les suivantes sont rapides et fonctionnent hors connexion.

Avec les classes dans `target/classes`, le programme s'exécute avec le même `-cp` que tout à l'heure, cette fois avec la racine des classes de Maven :

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

C'est le tableau habituel, avec le même résumé : `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`. Ce nombre, le programme le calcule à chaque fois, et les tests de la Leçon 7 vont le surveiller.

Le `debug` de la ligne `Compiling` cache une récompense qui renvoie à la [Leçon 2](02-tipos-cadenas-control.md) : le message de `NullPointerException` qui, avec `java Revisor.java`, disait `"<local1>"` (le lanceur compile sans cette information) donne maintenant le nom de la variable. Avec une classe de test, compilée par Maven, qui fait `detalle.isBlank()` sur un `null` :

```text
$ java -cp target/classes mx.habil.revisor.Nulo
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.isBlank()" because "detalle" is null
	at mx.habil.revisor.Nulo.main(Nulo.java:6)
```

`because "detalle" is null` : le nom de la variable qui a causé l'erreur. Maven compile par défaut avec des informations de débogage, et c'est pourquoi les messages des programmes du projet sont plus faciles à lire que ceux des fichiers isolés.

#### 6.2.8 Trois vérifications avant de continuer

Si tu es arrivé jusqu'ici, le projet compile déjà et s'exécute. Vérifie trois choses, dans cet ordre :

1. `mvn -B compile` se termine par `BUILD SUCCESS` et dit `Compiling 13 source files`.
2. `java -cp target/classes mx.habil.revisor.Revisor` affiche le tableau des quatre services avec le résumé de 843 ms.
3. Exécutée depuis un autre dossier, la même commande ne trouve pas `servicios.txt` : c'est le comportement correct, et tu le verras dans « L'erreur que tu vas voir ».

Si l'une échoue, l'erreur relève presque toujours des trois mêmes familles : un fichier sans sa ligne `package` (le compilateur dit qu'il ne trouve pas une classe), un fichier dans le mauvais dossier (6.1.2), ou une commande exécutée hors de la racine du projet, où il n'y a pas de `pom.xml` :

```text
$ cd ~/curso-java
$ mvn -B verify
[ERROR] BUILD FAILURE
[ERROR] The goal you specified requires a project to execute but there is no POM in this directory (~/curso-java). Please verify you invoked Maven from the correct directory. -> [Help 1]
```

Maven le dit en toutes lettres : il n'y a pas de POM dans le dossier où tu te trouves. Ce qu'il manque, c'est un `cd` jusqu'à la racine du projet.


### 6.3 Le cycle complet et le `.jar`

#### 6.3.1 Ce que fait `mvn -B verify` aujourd'hui

Jusqu'ici, tu as utilisé `compile`, la première phase. Demande maintenant la dernière, celle que tu écriras pendant tout le reste du cours. Depuis la racine du projet :

```text
$ mvn -B verify
[INFO] Building revisor 0.6.0
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] 
[INFO] --- resources:3.5.0:testResources (default-testResources) @ revisor ---
[INFO] skip non existing resourceDirectory ~/curso-java/revisor/src/test/resources
[INFO] 
[INFO] --- compiler:3.16.0:testCompile (default-testCompile) @ revisor ---
[INFO] No sources to compile
[INFO] 
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] No tests to run.
[INFO] 
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.6.0.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

Lis-le avec le tableau de 6.2.5 à côté, parce que chaque bloc `--- plugin:version:objectif ---` est une phase au travail, dans l'ordre de la chaîne. D'abord, la phase `compile`, comme en 6.2.7 : treize fichiers vers `target/classes`. Viennent ensuite trois lignes qui appartiennent à la phase `test` et qui n'ont pas encore de travail. `testResources` cherche des fichiers auxiliaires pour les tests dans `src/test/resources` et il n'y en a pas. `testCompile` est le même compilateur, mais appliqué à `src/test/java`, et il dit `No sources to compile` : le dossier de tests que tu as créé en 6.2.6 existe et il est vide. Et `surefire:test`, le *plugin* qui exécute les tests, dit `No tests to run.`, parce qu'il n'y en a aucun. Enfin, la phase `package` appelle `jar:jar`, qui assemble le fichier `target/revisor-0.6.0.jar`. La phase `verify` n'ajoute encore rien (la Leçon 10 y accrochera les contrôles de qualité), et Maven conclut par `BUILD SUCCESS`.

Deux détails méritent un second regard. Le premier est le nom du `.jar` : tu n'as écrit `revisor-0.6.0.jar` nulle part, Maven le construit avec l'`artifactId` et la `version` du `pom.xml`. Quand la version montera, le nom du fichier montera avec elle ; c'est pourquoi les coordonnées ne sont pas une formalité. Le second est la ligne de *Surefire*. Remarque ce qui s'est passé : Maven n'a pas exécuté un seul test et a terminé au vert. Aujourd'hui, c'est la vérité, parce que tu n'as pas écrit de tests, et le rapport le dit en toutes lettres. Dans la Leçon 7, tu verras qu'un vert sans tests peut aussi apparaître quand tu **as** écrit des tests, sans cet avertissement, et pourquoi le nombre de tests se lit toujours dans le rapport et jamais dans le `BUILD SUCCESS`. Garde cette image : `No tests to run.` est ce que dit Maven quand il sait qu'il n'y a pas de tests.

#### 6.3.2 Ce qu'il y a dans le `.jar`

Un fichier `.jar` (de *Java ARchive*) est un fichier compressé au même format qu'un `.zip`, qui conserve des classes compilées et un peu d'information à leur sujet. Le JDK fournit un outil pour le manipuler, qui porte le même nom, `jar`, et son option `tf` liste le contenu (`t` est « liste », `f` est « du fichier que je te dis ») :

```text
$ jar tf target/revisor-0.6.0.jar
META-INF/
META-INF/MANIFEST.MF
mx/
mx/habil/
mx/habil/revisor/
META-INF/maven/
META-INF/maven/mx.habil/
META-INF/maven/mx.habil/revisor/
mx/habil/revisor/Clasificador.class
mx/habil/revisor/Configuracion.class
mx/habil/revisor/ErrorDeConfiguracion.class
mx/habil/revisor/Estado.class
mx/habil/revisor/Formato.class
mx/habil/revisor/Reporte.class
mx/habil/revisor/Respuesta$Respondio.class
mx/habil/revisor/Respuesta$SinRespuesta.class
mx/habil/revisor/Respuesta.class
mx/habil/revisor/Resultado.class
mx/habil/revisor/Resumen.class
mx/habil/revisor/Revision.class
mx/habil/revisor/Revisor.class
mx/habil/revisor/Servicio.class
mx/habil/revisor/Simulador.class
META-INF/maven/mx.habil/revisor/pom.xml
META-INF/maven/mx.habil/revisor/pom.properties
```

Il y a trois groupes. Le plus grand, ce sont tes classes, et elles arrivent **avec les dossiers de leur paquet** : `mx/habil/revisor/Clasificador.class`, exactement la forme que `javac -d out` a produite en 6.1.2 et que la JVM exige pour trouver une classe par son nom complet. Un `.jar` est, au fond, un dossier `target/classes` rangé dans un seul fichier. Compte les classes : il y en a quinze, pour treize fichiers source. Les deux en plus sont `Respuesta$Respondio.class` et `Respuesta$SinRespuesta.class` : les deux *records* qui vivent **à l'intérieur** de l'interface `Respuesta` (Leçon 3). Chaque classe ou *record* imbriqué est compilé en son propre `.class`, avec le nom de la classe qui le contient, un `$` et son nom.

Le deuxième groupe est `META-INF/MANIFEST.MF`, le **manifeste** : un fichier texte avec des données sur le `.jar` que lit la JVM. Le troisième, `META-INF/maven/...`, est une copie du `pom.xml` et un fichier avec les coordonnées ; Maven l'ajoute pour que celui qui trouve le `.jar` isolé sache de quel projet il provient et dans quelle version. Ce qui n'y est **pas** compte aussi : pas un seul `.java` (le `.jar` contient ce qui est compilé, pas la source), pas de `servicios.txt` (il est à la racine du projet, pas dans `src/main/resources`, donc il ne fait pas partie du programme), ni rien de JUnit ni de tests.

#### 6.3.3 Exécuter depuis le `.jar`, et ce qui manque encore

Comme le `.jar` contient la même chose que `target/classes`, il sert pareillement de *classpath* :

```text
$ java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Le même tableau, maintenant depuis un seul fichier que tu pourrais copier sur un autre ordinateur avec Java 25. Il existe une forme plus courte, `java -jar archivo.jar`, qui est celle qu'utilisent les programmes Java que l'on livre à d'autres personnes. Essaie-la :

```text
$ java -jar target/revisor-0.6.0.jar
no main manifest attribute, in target/revisor-0.6.0.jar
```

« No hay atributo principal en el manifiesto » (« aucun attribut principal dans le manifeste »). Avec `-jar`, la JVM ne reçoit pas le nom de la classe qu'elle doit démarrer : elle le cherche dans le manifeste, sur une ligne appelée `Main-Class`. Et le manifeste construit par Maven ne l'a pas. Tu peux le voir, parce que `unzip -p` affiche un fichier de l'intérieur du `.jar` sans l'extraire :

```text
$ unzip -p target/revisor-0.6.0.jar META-INF/MANIFEST.MF
Manifest-Version: 1.0
Created-By: Maven JAR Plugin 3.5.1
Java-Version: 25
Build-Jdk-Spec: 25
```

Quatre lignes : la version du format, qui l'a créé, et avec quel Java. Rien n'indique quelle est la classe d'entrée. (Si ta JVM est en espagnol, le message d'erreur sort traduit ; mesuré avec la JVM en espagnol du Mexique : `no hay ningún atributo de manifiesto principal en target/revisor-0.6.0.jar`. Le code de sortie est 1 dans les deux langues.) On pourrait ajouter cette ligne avec la configuration du *plugin* `jar`, mais cela ne résoudrait pas le problème de fond, qui apparaît dans la Leçon 8 : quand le programme utilisera une bibliothèque, le `.jar` contiendra tes classes et pas celles de la bibliothèque, et la JVM ne les trouvera pas. La [Leçon 10](10-el-programa-terminado.md) règle les deux choses à la fois avec un `.jar` qui contient tout et sait par où commencer. En attendant, la commande du cours est celle de `-cp`, qui fonctionne aussi bien avec `target/classes` qu'avec le `.jar`.

#### 6.3.4 `target/` peut être jeté

Tu as maintenant sous les yeux tout ce qui vit dans `target/` : `classes` avec les `.class`, le `.jar`, et quelques dossiers de travail de Maven (`maven-status`, `maven-archiver`, `generated-sources`) qui conservent des données pour l'exécution suivante. **Tout cela sort de `src/` et du `pom.xml`**, et c'est pourquoi on peut le supprimer en entier à tout moment : le prochain `mvn -B verify` le recrée à l'identique. C'est la preuve qu'un projet est bien monté. Si un jour tu supprimes `target/` et que le projet ne se construit plus, c'est qu'il y avait quelque chose d'important là où il ne devait pas être.

De là sortent les deux règles que tu as déjà vues en 6.2.3, qu'il convient de répéter parce que ce sont celles qu'on enfreint le plus. **On n'édite jamais rien dans `target/`** : un changement fait là se perd à la construction suivante, et le pire est que, entre-temps, on a l'impression que ça a marché. Et **on ne conserve jamais `target/` dans un dépôt** : tu garderais le résultat et non la source, et chaque personne qui construirait le projet verrait des changements dans des fichiers qu'elle n'a pas touchés. Ce qu'on conserve, c'est `src/`, `pom.xml` et ce dont le programme a besoin pour tourner, comme `servicios.txt`.


### 6.4 L'étape du projet : le `revisor` comme projet Maven

Tu as toutes les pièces. Voici le projet complet, tel qu'il se présente à la fin de la leçon. Tu en connais déjà la plus grande partie : dans le programme, seul le `Revisor` (6.2.6) a vraiment changé par rapport à la Leçon 5, et les autres classes ont seulement gagné leur ligne `package`. On les inclut toutes pour que le projet puisse être monté sans regarder une autre leçon :

```text
revisor/
  pom.xml                 versión 0.6.0 · release 25 · tres plugins con versión fija
  servicios.txt           sin cambios
  src/main/java/mx/habil/revisor/
    Revisor.java          NUEVO como clase pública: el único punto de entrada
    Estado.java  Respuesta.java  Servicio.java  Resultado.java  Clasificador.java
    Formato.java  Simulador.java  Revision.java  Resumen.java  Reporte.java
    Configuracion.java  ErrorDeConfiguracion.java       (solo ganan su línea package)
  src/test/java/mx/habil/revisor/                       (vacía: la llena la Lección 7)
```

Tu as déjà vu en entier le `pom.xml` (6.2.4), `Revisor.java` (6.2.6) et `Estado.java` (6.1.3). Ton `servicios.txt` est celui de toujours :

```text fig06_03/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

Et voici les onze fichiers restants. Si tu as suivi les leçons 3 à 5, tu les as déjà, et la seule différence est ce qui a été dit en 6.2.6 ; je les inclus pour que le projet de cette leçon puisse être construit sans dépendre de rien d'autre.

<details>
<summary>Les onze fichiers du programme qui ne changent qu'à leur première ligne</summary>

```java
// fig06_03/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig06_03/src/main/java/mx/habil/revisor/Servicio.java

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
// fig06_03/src/main/java/mx/habil/revisor/Resultado.java

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
// fig06_03/src/main/java/mx/habil/revisor/Clasificador.java

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
// fig06_03/src/main/java/mx/habil/revisor/Simulador.java

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
// fig06_03/src/main/java/mx/habil/revisor/Revision.java

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
// fig06_03/src/main/java/mx/habil/revisor/Resumen.java

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
// fig06_03/src/main/java/mx/habil/revisor/Reporte.java

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
// fig06_03/src/main/java/mx/habil/revisor/Formato.java

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
// fig06_03/src/main/java/mx/habil/revisor/Configuracion.java

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
    try {
      return new URI(texto);
    } catch (URISyntaxException e) {
      throw new ErrorDeConfiguracion("renglón " + numero + ": la url no es válida", e);
    }
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
// fig06_03/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

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

Pour terminer la leçon : `mvn -B verify` doit dire `Compiling 13 source files`, `No tests to run.` et `BUILD SUCCESS`, et laisser `target/revisor-0.6.0.jar` ; `java -cp target/classes mx.habil.revisor.Revisor` et `java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor` doivent tous deux afficher le tableau avec le résumé de la Leçon 4 (4.5) de 843 ms. Il existe un article de Hábil sur [la façon dont se monte l'intégration continue d'une entreprise de logiciel](https://www.habil.mx/fr/blog/cicd-devsecops-infrastructure-propre/), l'endroit où `mvn -B verify` s'exécute tout seul à chaque changement ; il vaut la peine de le lire maintenant que la commande est devenue la tienne.


## L'erreur que tu vas voir

La leçon a une erreur principale, celle de la classe que la JVM ne trouve pas, et plusieurs messages secondaires qu'il convient de savoir lire d'un coup d'œil.

**L'erreur principale.** C'est celle de 6.1.2, et tu la verras dès que tu exécuteras un programme avec paquet de la mauvaise façon :

```text
$ java -cp out Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola
```

Elle se lit en trois temps, toujours les mêmes. **Ce qu'elle cherchait** : le nom après `class`, ici `Hola` tout court. **Où elle l'a cherché** : dans le *classpath* que tu lui as donné avec `-cp`, ici `out`. **Ce qu'elle a trouvé** : rien (`ClassNotFoundException`), ou un fichier dont le vrai nom est autre (`NoClassDefFoundError ... wrong name: mx/habil/revisor/Hola`). Avec ces trois réponses, la correction est toujours l'une des deux suivantes : écrire le nom complet de la classe, avec son paquet, ou exécuter depuis la racine des classes (`out`, `target/classes` ou le `.jar`). Dans le `revisor`, il n'y a qu'une seule bonne forme et elle ne change pas pour le reste de la leçon : `java -cp target/classes mx.habil.revisor.Revisor`, depuis la racine du projet.

**Les messages secondaires**, pour les reconnaître d'un coup d'œil :

- `package org.junit.jupiter.api does not exist` (`fig06_01.java`) : il manque une dépendance. Le lanceur ne connaît pas les dépendances ; dans Maven, elles se déclarent dans le `pom.xml`, et la Leçon 7 déclare justement celle-là.
- `cannot find symbol` avec `symbol: variable Clasificador`, en compilant avec une recherche par dossier : un fichier est dans un dossier qui ne correspond pas à son paquet. Le dossier doit dire la vérité (6.1.2).
- `Estado is not public in mx.habil.revisor; cannot be accessed from outside package` : tu as demandé depuis un autre paquet une classe de visibilité de paquet. Avant de lui mettre `public`, demande-toi si cette classe devrait l'utiliser.
- `no non-private zero argument constructor found` : un `main` d'instance dans une classe avec constructeur privé. La forme longue `public static void main(String[] args)` n'a pas ce problème (6.1.4).
- `The goal you specified requires a project to execute but there is no POM in this directory` : tu as exécuté `mvn` hors de la racine du projet. Entre dans le dossier qui contient le `pom.xml`.
- `release version 25 not supported` : Maven utilise un JDK plus ancien que le 25. Vérifie la ligne `Java version` de `mvn --version`, la variable `JAVA_HOME` et, s'il y a plusieurs JDK, l'encadré de la Leçon 1 sur `sdk default`.
- `no main manifest attribute` : tu as essayé `java -jar` avec un `.jar` qui ne dit pas quelle est sa classe d'entrée. Utilise `java -cp` avec le nom de la classe ; la Leçon 10 règle cela.
- `error de configuración: no existe el archivo servicios.txt` (« erreur de configuration : le fichier servicios.txt n'existe pas ») en exécutant depuis un autre dossier, qui est le dernier et celui qui se répète le plus :

```text
$ cd ..
$ java -cp revisor/target/classes mx.habil.revisor.Revisor
error de configuración: no existe el archivo servicios.txt
```

Le programme cherche `servicios.txt` par un chemin **relatif**, `Path.of("servicios.txt")`, et un chemin relatif signifie « le fichier du dossier où je me trouve », pas « celui qui est à côté du programme ». Depuis le dossier parent, il n'existe pas. Remarque ce que le programme a bien fait : il n'a pas affiché une exception de trente lignes ni un `NoSuchFileException`, mais le message en espagnol de la Leçon 5. Pour savoir dans quel dossier tu es, la commande `pwd` te le dit. Dans la Leçon 9, le premier argument de la ligne de commande te laissera indiquer le fichier sans dépendre de l'endroit où tu es.

## Ce qui se fait de travers

**Installer Maven par un chemin différent de celui de Java.** Si Java vient de SDKMAN et Maven d'ailleurs (un paquet du système, une archive décompressée à la main), chacun a ses propres idées sur le JDK à utiliser, et le jour où elles ne coïncident pas, l'erreur ne dit pas pourquoi. Avec les deux outils dans SDKMAN, `mvn --version` et `java --version` parlent du même Temurin 25.

**Mettre `public` à tout « pour que ça compile ».** Chaque chose publique est une promesse faite à qui l'utilise, et chaque promesse est un coût quand tu voudras la changer. L'erreur `is not public` signifie presque toujours que quelque chose est au mauvais endroit, pas qu'il manque un mot.

**Un dossier qui ne correspond pas au paquet, « parce que ça compile ».** Maven le compile (6.1.2), mais cela casse tout ce qui cherche par dossier : l'éditeur, les recherches, la compilation incrémentale et la personne suivante. C'est une erreur qui ne prévient pas, et c'est pourquoi on l'évite par habitude et non à cause du message.

**Des dépendances et des *plugins* sans version, ou avec la plus récente.** Cela fait que ta construction change le jour où quelqu'un publie une nouvelle version ou met Maven à jour, sans que tu aies touché à rien. Chaque pièce porte sa version écrite, et on la fait monter exprès.

**Éditer ou conserver `target/`.** C'est ce que Maven génère et que l'on peut supprimer et régénérer à tout moment (6.3.4). Un changement fait là se perd à la construction suivante, et si tu l'envoies dans un dépôt, tu conserves le résultat et non la source.

**Exécuter depuis n'importe quel dossier et accuser le programme.** Maven doit se trouver dans le dossier du `pom.xml`, et le `revisor` dans le dossier de `servicios.txt` : aujourd'hui c'est le même, la racine du projet. Avant de lire une erreur « n'existe pas », lis `pwd`.

## Exercices

### Exercice 1 — Le dossier qui ment

Déplace `Clasificador.java` un dossier au-dessus de celui qui lui revient, vers `src/main/java/mx/habil/`, sans toucher à sa ligne `package mx.habil.revisor;`. **Avant d'exécuter quoi que ce soit, prédis** ce que fait chacune de ces deux commandes : `javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java` (qui compile `Revision` en cherchant les autres classes par dossier) et `mvn -B compile`. Exécute-les et compare. Où `Clasificador.class` s'est-il retrouvé dans `target/classes` ? Remets le fichier dans son dossier et supprime le dossier `out` créé par `javac`.

### Exercice 2 — Ce que Maven décide pour toi

Retire du `pom.xml` la ligne `<version>3.5.1</version>` du *plugin* `maven-jar-plugin` et lance `mvn -B verify`. Lis les lignes `WARNING` du début : que disent-elles qu'il manque, et où ? Cherche ensuite la ligne `--- jar:...` de la sortie : quelle version du *plugin* Maven a-t-il utilisée ? Avec cette réponse, explique dans ton journal de bord pourquoi le `pom.xml` fixe la version même si, aujourd'hui, cela donne la même chose. Remets la ligne en place à la fin.

### Exercice 3 — Le mauvais JDK

Si tu as un JDK 21 installé avec SDKMAN en plus du 25 (les JDK de SDKMAN vivent dans `~/.sdkman/candidates/java/`, un dossier par identifiant ; `ls ~/.sdkman/candidates/java/` te les montre), oriente Maven vers lui pour une seule commande, en plaçant la variable devant : `JAVA_HOME=~/.sdkman/candidates/java/<l'identifiant du 21> mvn -B compile`. **Avant, prédis** dans quelle phase cela échoue et avec quel message. Si tu n'as pas de 21, ne l'installe pas pour cela : lis la solution. Dans les deux cas, réponds : quelle propriété du `pom.xml` produit l'erreur, et pourquoi vaut-il mieux que cela échoue là et pas plus tard ?

### Exercice 4 — Ce qu'il y a dans ton `.jar`

Lance `jar tf target/revisor-0.6.0.jar` et compte les fichiers qui se terminent par `.class`. **Avant de compter, prédis** combien il devrait y en avoir, sachant que le projet a treize fichiers `.java`. Si le nombre ne coïncide pas avec ta prédiction, trouve ceux qui sont en trop et explique d'où ils sortent. Essaie ensuite `java -jar target/revisor-0.6.0.jar` et explique le message avec ce que tu as vu dans le manifeste.

## Solutions

### Solution 1

Avec `Clasificador.java` un dossier plus haut, les deux commandes se comportent différemment :

```text
$ javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java
src/main/java/mx/habil/revisor/Revision.java:14: error: cannot find symbol
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
                                ^
  symbol:   variable Clasificador
  location: class Revision
1 error
$ mvn -B compile
...
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] BUILD SUCCESS
```

`javac` avec `-sourcepath` cherche chaque classe dont il a besoin **par dossier** : pour `mx.habil.revisor.Clasificador`, il regarde dans `src/main/java/mx/habil/revisor/Clasificador.java`, ne le trouve pas et conclut qu'il n'existe pas. Maven, en revanche, remet à `javac` la liste complète des fichiers de `src/main/java`, trouve les treize et compile sans se plaindre. Et `Clasificador.class` se retrouve dans `target/classes/mx/habil/revisor/`, **là où le dit son paquet**, pas là où se trouvait le `.java` : le compilateur range la sortie d'après la ligne `package`. C'est pourquoi l'erreur ne se voit pas dans Maven et se voit dans tout outil qui cherche par dossier. Le programme tourne pareil ; le projet, lui, ment déjà.

### Solution 2

Sans la version, Maven construit, mais prévient au début :

```text
$ mvn -B verify
[INFO] Scanning for projects...
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for mx.habil:revisor:jar:0.6.0
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-jar-plugin is missing. @ line 25, column 15
[WARNING] 
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[WARNING] 
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.
...
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
...
[INFO] BUILD SUCCESS
```

Il dit ce qui manque (la version du *plugin* `maven-jar-plugin`) et où (`@ line 25, column 15`, la ligne du `pom.xml` où commence ce *plugin* ; si ton fichier a une autre forme, le numéro change). Et la réponse à la deuxième question est celle qui rend l'exercice intéressant : Maven a utilisé **la même 3.5.1** que celle que fixait ton `pom.xml`. Aujourd'hui, avec Maven 3.10.0, la version par défaut coïncide avec celle du cours. Mais c'est une coïncidence de cette version de Maven, pas une propriété de ton projet : la version par défaut est décidée par celui qui publie Maven, et elle change quand Maven change. La fixer, c'est la différence entre « aujourd'hui ça donne la même chose » et « ça donne toujours la même chose ». La phrase pour le journal de bord : **on fixe la version pour que le résultat ne dépende pas du Maven que chacun a installé.**

### Solution 3

Avec un JDK 21, Maven démarre, arrive à la phase `compile` et s'arrête là :

```text
$ JAVA_HOME=<la carpeta del JDK 21> mvn -B compile
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] ------------------------------------------------------------------------
[ERROR] BUILD FAILURE
...
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile (default-compile) on project revisor: Fatal error compiling: error: release version 25 not supported -> [Help 1]
```

(Mesuré avec un JDK 21 d'une autre distribution ; le message vient du compilateur et n'en dépend pas.) La propriété responsable est `maven.compiler.release` à `25` : le compilateur du JDK 21 ne sait pas produire des classes pour Java 25 et le dit avant de compiler un seul fichier. Il vaut mieux que cela échoue là parce que le message nomme la cause (la version) au lieu d'un symptôme (une méthode ou une syntaxe qui « n'existe pas »), et parce que l'erreur apparaît dès la première phase, avant que quelqu'un perde du temps avec des tests ou des paquets à moitié faits. Remarque aussi ce que démontre l'exercice : Maven n'utilise **pas** nécessairement le `java` de ton terminal, mais celui de `JAVA_HOME` si elle est définie. C'est pourquoi, quand Maven et `java --version` ne coïncident pas, la première chose à faire est de regarder cette variable.

### Solution 4

Il y a **quinze** fichiers `.class`, pas treize :

```text
$ jar tf target/revisor-0.6.0.jar
...
mx/habil/revisor/Respuesta$Respondio.class
mx/habil/revisor/Respuesta$SinRespuesta.class
mx/habil/revisor/Respuesta.class
...
```

Les deux en plus sont les *records* imbriqués dans l'interface `Respuesta` : chaque classe, *record* ou `enum` est compilé en son propre `.class`, et s'il vit à l'intérieur d'une autre, son nom est précédé de celui de la classe qui la contient et d'un `$`. Un fichier `.java` peut produire plusieurs `.class`, et la relation « un fichier, une classe » n'est obligatoire que pour les classes `public` (6.1.2). Et `java -jar` échoue avec `no main manifest attribute` parce que le manifeste que tu as vu en 6.3.3 n'a pas la ligne `Main-Class` : la JVM, avec `-jar`, ne sait pas par quelle classe commencer. Avec `java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor`, c'est toi qui le lui dis, et ça marche.

## Comment savoir que j'ai réussi

- [ ] `mvn --version` dit `Apache Maven 3.10.0` et une version de Java qui commence par 25.
- [ ] `mvn -B verify`, depuis la racine de `~/curso-java/revisor/`, dit `Compiling 13 source files`, `No tests to run.` et `BUILD SUCCESS`, et laisse `target/revisor-0.6.0.jar`.
- [ ] `java -cp target/classes mx.habil.revisor.Revisor` affiche le tableau des quatre services avec le résumé `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`, et la même chose avec le `.jar` à la place de `target/classes`.
- [ ] Depuis le dossier parent, la même commande avec `revisor/target/classes` affiche `error de configuración: no existe el archivo servicios.txt`, et tu sais expliquer pourquoi.
- [ ] Tu sais expliquer ce qui se passe si un fichier déclare `package mx.habil.revisor;` et se trouve dans un autre dossier, et pourquoi Maven te le permet alors que ce n'est pas une bonne idée.
- [ ] Tu sais expliquer pourquoi il y a quinze `.class` dans le `.jar` et pourquoi `java -jar` ne fonctionne pas encore.
- [ ] Tu as noté dans ton journal de bord quel message de cette leçon t'a coûté le plus à lire.

## Résumé

Réponds sans regarder la leçon :

1. Quelles trois choses le lanceur de fichiers source ne peut-il pas faire, qu'un projet Maven fait ?
2. Quelle relation doit-il y avoir entre le nom d'un paquet, la ligne `package` et le dossier du fichier ? Qui le vérifie, et qui ne le vérifie pas ?
3. Que voit une classe de visibilité de paquet, et que ne voit-elle pas ? Quelle classe du `revisor` est `public`, et pourquoi seulement celle-là ?
4. Pourquoi le `Revisor` utilise-t-il `public static void main(String[] args)` et non `void main()` ?
5. Avec quoi installes-tu Maven et quelles trois choses reconnais-tu dans `mvn --version` pour savoir que c'est bien installé ?
6. Que sont les coordonnées d'un projet Maven ? À quoi sert de fixer la version de chaque *plugin* ?
7. Quelles phases exécute `mvn verify` et dans quel ordre ? Que laisse chacune dans `target/` ?
8. Qu'y a-t-il dans un `.jar`, qu'est-ce qui n'y est pas, et pourquoi `java -jar` ne l'exécute-t-il pas encore ?

## Pour aller plus loin

- [Maven, « Introduction to the Build Lifecycle »](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) et [« Introduction to the POM »](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html) : les phases, quel *plugin* s'exécute dans chacune, et tout ce que peut contenir un `pom.xml`. En anglais, comme presque toute la documentation officielle. Consulté le 7 octobre 2026.
- [La spécification du langage, chapitre 7 : paquets et modules](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html) et [le tutoriel d'Oracle sur les paquets](https://docs.oracle.com/javase/tutorial/java/package/index.html) : les règles exactes de noms, dossiers et importations. Consulté le 7 octobre 2026.
- [Oracle, « JAR File Specification » (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html) et [le manuel de la commande `jar`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jar.html) : ce que contient un `.jar`, ce qu'est le manifeste et à quoi sert `Main-Class`. Consulté le 13 octobre 2026.
- [SDKMAN, utilisation](https://sdkman.io/usage/) : `list`, `install`, `use` et `default`, les mêmes commandes pour Java et pour Maven. Consulté le 13 octobre 2026.

---

**Précédente :** [Leçon 5 — Erreurs et exceptions](05-errores-excepciones.md) ·
**Suivante :** [Leçon 7 — Tests avec JUnit](07-pruebas-junit.md)
