# Leçon 0 — Ce qu'est Java et ce qu'il n'est PAS

**Durée :** 90 min

**Ce que tu construis :** rien pour l'instant. C'est une leçon de lecture : on y dresse la carte mentale grâce à laquelle le reste du cours devient prévisible

**Ce que tu apprends :** le chemin du code source jusqu'au programme qui s'exécute (`javac`, *bytecode*, la JVM) ; ce qu'est le JDK et ce qu'était le JRE ; ce que signifie LTS et pourquoi le cours utilise Java 25 ; ce qu'est une fonctionnalité en préversion ; et ce que le compilateur de Java protège et ce qu'il laisse passer (les types sont vérifiés à la compilation, mais les génériques sont effacés et `null` existe toujours)

**D'où tu viens.** Tu n'apportes rien de ce cours : tu n'as pas installé Java ni écrit une seule ligne. Tu apportes, en revanche, ce que tu sais déjà d'un autre langage —Go, TypeScript, Python ou n'importe lequel— : ce qu'est une variable, une boucle, une fonction, une erreur. C'est exactement ce que le cours considère comme acquis. Ce que tu n'apportes pas, c'est la carte de **la façon dont fonctionne la plateforme Java**, qui est différente de celle de chacun de ces langages et qui explique pourquoi les commandes, les messages d'erreur et les habitudes de Java sont ce qu'ils sont.

**Ce que cette leçon ne fait pas.** Elle n'installe rien (c'est la [Leçon 1](01-instalacion.md)) et n'enseigne pas la syntaxe de Java (elle commence à la [Leçon 2](02-tipos-cadenas-control.md)). Elle apporte quatre petits programmes, de quatre à huit lignes, juste pour que tu voies de tes yeux ce que la leçon affirme. Il n'est pas nécessaire de les comprendre ligne par ligne pour l'instant, même si chacun est expliqué : ce qui compte, c'est ce qu'ils **impriment**. On les exécute après la Leçon 1, quand tu auras Java.

**À propos des exécutions.** Les sorties de cette leçon ont été obtenues avec Temurin 25.0.4 (une distribution gratuite de Java, expliquée plus bas) sous macOS, avec la configuration régionale en UTF-8. Sur ton Linux Mint, elles sont identiques ; seules les lignes qui mentionnent le système changent, et on le signale là où cela se produit. Les programmes se trouvent dans [`programas/00-que-es-java/`](../programas/00-que-es-java), dans le dépôt du cours.

## À la fin, tu seras capable de

- Décrire avec tes propres mots le chemin d'un fichier `.java` jusqu'à son exécution : ce que fait `javac`, ce que contient un `.class` et ce que la JVM en fait.
- Distinguer le JDK, le JRE et la JVM, et dire lequel installer et pourquoi.
- Expliquer ce que signifie que Java 25 soit LTS, pourquoi Java 26 et Java 27 existent mais ne sont pas utilisés dans le cours, et ce qu'est une fonctionnalité en préversion.
- Dire, face à une défaillance possible, si le compilateur de Java l'attrape avant l'exécution ou si elle n'apparaît que lorsque le programme s'exécute.
- Expliquer ce qu'est l'effacement des génériques et pourquoi `null` existe toujours dans un langage typé.
- Prédire ce qu'imprime `"142" + 1` en Java et pourquoi ce n'est pas une addition.

## Le pourquoi avant le comment

Imagine que quelqu'un te demande le programme du cours, le `revisor` : il lit une liste de services, les interroge tous en même temps et te rend un rapport. En Go, tu l'écrirais et obtiendrais un fichier exécutable que tu copies sur un serveur et qui s'exécute. En Python, un script qui s'exécute tant que l'interpréteur est installé. En TypeScript, du code que Node exécute après que le compilateur l'a traduit en JavaScript. Trois langages, trois réponses à la même question : **qu'est-ce qui est livré, exactement, et qui le fait fonctionner ?**

Java a sa propre réponse, et c'est la première chose à comprendre, parce que tout le reste en dépend. En Java, tu écris des fichiers `.java`, un programme appelé `javac` les traduit en fichiers `.class`, et un autre programme appelé `java` les exécute à l'intérieur d'une machine virtuelle. Deux programmes, deux formats de fichier et une machine virtuelle : si tu ne sais pas ce que fait chacun, l'installation de la Leçon 1 n'est qu'une liste de commandes que l'on suit à l'aveugle, et les erreurs de la Leçon 2 sont des messages d'origine mystérieuse.

Il y a aussi une raison pratique. Dans la bibliographie et dans les tutoriels sur Java, des conseils vieux de vingt ans côtoient ceux d'aujourd'hui, et ils ne disent presque jamais de quelle année ils datent. Qui arrive sans la carte ne peut pas distinguer « c'était ainsi en 2008 » de « c'est ainsi en 2026 ». Avec la carte de cette leçon, si.

La leçon comporte cinq parties, dans cet ordre :

1. **Le chemin du code** (0.1) : de `.java` à `.class` puis à la JVM.
2. **Les pièces de la plateforme** (0.2) : le JDK, le JRE et les distributions.
3. **Les versions** (0.3) : ce qu'est LTS, ce qu'est la préversion et pourquoi la 25.
4. **Ce que le compilateur protège et ce qu'il ne protège pas** (0.4) : la partie la plus utile de la leçon, avec quatre programmes qui s'exécutent pour de vrai.
5. **Java face à ce que tu connais déjà** (0.5) et **ce qui vient dans le reste du cours** (0.6).

Avec cette destination en tête, voici ce que tu verras dans cette leçon, à la section 0.4.1, quand tu exécuteras le premier programme. C'est un message du compilateur, pas de l'exécution, et la différence entre les deux est l'idée centrale :

```text
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
```

## Les concepts

### 0.1 Le chemin du code : source, *bytecode* et JVM

#### 0.1.1 Les trois choses qui existent

Quand tu écris un programme en Java, il y a trois objets distincts, et il vaut mieux leur donner un nom dès maintenant :

- Le **code source** : le fichier texte que tu écris, avec l'extension `.java`. Il est lu par une personne.
- Le ***bytecode*** : le résultat de la compilation du code source, enregistré dans un fichier d'extension `.class`. C'est un format binaire, compact, qui n'est pas fait pour être lu par une personne et qui n'est le langage d'aucun processeur réel. C'est le langage d'un processeur **imaginaire**.
- La **JVM** (*Java Virtual Machine*, « machine virtuelle Java ») : le programme qui exécute le *bytecode*. C'est le processeur imaginaire rendu réel, un logiciel qui existe pour chaque système d'exploitation et chaque type de processeur.

Le programme `javac` est le compilateur : il convertit le code source en *bytecode*. Le programme `java` est le lanceur : il démarre une JVM et lui demande d'exécuter le *bytecode* d'une classe. Le mot « classe » va compter, donc une définition provisoire : en Java, chaque `.class` contient **une** unité de code appelée classe (il y aura plus tard d'autres variantes, comme les `record` de la Leçon 3, qui finissent elles aussi dans un `.class`). C'est pourquoi on parle de « fichier de classe ».

Voici le dessin complet du chemin :

```text
Hola.java  ──javac──►  Hola.class  ──java──►  [ JVM ]  ──►  lo que el programa hace
(código fuente)        (bytecode)             (ejecuta)
```

Pourquoi deux étapes et non une ? Parce que chaque étape résout un problème différent et le résout **à un moment différent**. Le compilateur travaille une fois, sur ta machine de développement, et c'est lui qui vérifie que ce que tu as écrit a du sens (par exemple, que tu n'essaies pas de mettre un texte là où va un nombre, comme tu le verras en 0.4). La JVM travaille à chaque exécution du programme, sur la machine où il s'exécute, et c'est elle qui le fait fonctionner avec ce système d'exploitation et ce processeur. Le *bytecode* est le contrat entre les deux moitiés : le compilateur promet de produire un *bytecode* valide, et la JVM promet de l'exécuter.

#### 0.1.2 Un `.class` de l'intérieur

Tout cela paraît abstrait tant qu'on ne l'a pas vu. Dans la Leçon 1, tu compileras ton premier programme et tu verras le `.class` apparaître dans ton dossier. En attendant, regarde ce que contient un tout petit. Ce fragment n'est pas un programme de la leçon : c'est une classe minimale, `Suma`, avec une méthode qui additionne deux entiers, compilée avec `javac Suma.java`. On a ensuite demandé au désassembleur du JDK, `javap`, d'afficher le *bytecode* de cette méthode :

```text
$ javap -c Suma
Compiled from "Suma.java"
class Suma {
  Suma();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  static int sumar(int, int);
    Code:
       0: iload_0
       1: iload_1
       2: iadd
       3: ireturn
}
```

Tu n'as pas besoin de comprendre chaque ligne, mais regarde la méthode `sumar`, celle du bas. Ses instructions sont `iload_0`, `iload_1`, `iadd` et `ireturn` : « charge le premier argument entier », « charge le deuxième », « additionne deux entiers », « renvoie un entier ». Ce sont les instructions d'un processeur simple, de ceux qui se programment avec une pile : on met deux nombres sur la pile, on les additionne, le résultat reste au sommet. Ce processeur n'existe dans aucun ordinateur réel. Il n'existe qu'à l'intérieur de la JVM. Les instructions et le format du fichier sont définis par une spécification publique, la [Spécification de la machine virtuelle Java](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html), que toutes les JVM respectent.

Deux autres données sur le fichier, mesurées avec `xxd` et avec `javap -v`. Les huit premiers octets d'un `.class` de Java 25 sont `cafe babe 0000 0045`. Les quatre premiers, `cafebabe`, forment un nombre magique qui identifie n'importe quel fichier de classe, de n'importe quelle version : la spécification le fixe ainsi. Les deux derniers octets (`0045` en hexadécimal) sont la **version majeure** du format, 69 en décimal, qui correspond à Java 25. Chaque nouvelle version de Java augmente ce nombre, et une vieille JVM refuse d'exécuter un `.class` dont le numéro est supérieur à celui qu'elle comprend. Quand tu verras plus tard un message qui parle de « class file version », c'est de cela qu'il s'agit.

#### 0.1.3 Ce que fait la JVM du *bytecode*

La JVM ne se contente pas d'« interpréter » le *bytecode* instruction par instruction. Elle fait trois choses, et il vaut la peine de savoir lesquelles, même si tu n'en programmeras aucune :

1. **Elle charge et vérifie.** Elle lit le `.class`, contrôle que le *bytecode* est cohérent (qu'il n'essaie pas de retirer de la pile ce qu'il n'y a jamais mis, par exemple) et seulement alors le laisse s'exécuter. C'est un filet de sécurité contre les fichiers corrompus ou mal faits.
2. **Elle exécute, et pendant qu'elle exécute, elle observe.** Elle commence par interpréter le *bytecode*. À mesure que le programme tourne, elle repère les méthodes qui s'exécutent très souvent.
3. **Elle compile les méthodes « chaudes » en vrai code machine**, à chaud, pendant que le programme continue de fonctionner. Cette pièce s'appelle le compilateur JIT (*just in time*, « juste à temps »). C'est la raison pour laquelle un programme Java, après quelques instants de montée en température, peut s'exécuter à des vitesses comparables à celles d'un programme compilé dès le départ.

De plus, la JVM gère la mémoire : elle réserve celle que le programme demande et la libère toute seule quand plus personne ne l'utilise, grâce à une pièce appelée ramasse-miettes (*garbage collector*). Tu connais déjà l'idée si tu viens de Go, de Python ou de TypeScript. Le [guide de la JVM de Java 25](https://docs.oracle.com/en/java/javase/25/vm/) et le [guide du ramasse-miettes](https://docs.oracle.com/en/java/javase/25/gctuning/) en racontent les détails ; pour ce cours, il suffit de savoir qu'ils existent et que tu ne libéreras pas la mémoire à la main.

La conséquence pratique de tout cela est une promesse célèbre de Java : **« écris une fois, exécute partout »**. Le même `.class` s'exécute sous Linux, sous macOS et sous Windows, parce que chaque système a sa propre JVM qui sait l'exécuter. La promesse est réelle, mais elle a des limites, et il vaut mieux les connaître dès maintenant pour ne pas y croire trop. Deux exemples mesurés : le nom des chemins, l'encodage du texte et les sauts de ligne changent d'un système à l'autre, et le code qui les suppose se casse même si le `.class` est le même. Et la **sortie de texte** dépend de la configuration du système : avec la configuration régionale minimale (`LC_ALL=C`), un programme qui imprime `sin conexión` (« sans connexion ») a écrit `sin conexi?n`, parce que le système a dit à Java que sa sortie était en ASCII ; avec `LC_ALL=C.UTF-8`, il a imprimé le bon texte. Depuis Java 18, le texte des fichiers est lu et écrit en UTF-8 par défaut ([JEP 400](https://openjdk.org/jeps/400)), mais la sortie standard suit la configuration du système. Portable ne veut pas dire identique.

#### 0.1.4 Et `java Hola.java`, laquelle des deux étapes est-ce ?

Tu verras plus tard qu'on peut exécuter un programme avec une seule commande, `java Hola.java`, sans passer par un `javac` visible. Cela ne contredit rien de ce qui précède. Ce mode, appelé lanceur de fichier source ([JEP 330](https://openjdk.org/jeps/330)), compile le fichier **en mémoire**, sans laisser de `.class` sur le disque, et l'exécute aussitôt. Les deux étapes ont lieu ; simplement, l'une d'elles ne laisse pas de trace. C'est pratique pour apprendre et pour les programmes d'un seul fichier ; pour de vrais projets, on compile séparément, et c'est ce que tu fais à partir de la Leçon 6.

### 0.2 Les pièces de la plateforme : JDK, JRE et distributions

#### 0.2.1 JDK et JRE

Il y a deux sigles que tu verras dans tous les tutoriels, et dont le sens a changé avec les années :

- Le **JRE** (*Java Runtime Environment*) est ce qu'il faut pour **exécuter** des programmes Java : la JVM et les bibliothèques standard. Avec un JRE, tu peux exécuter un `.class` ou un JAR, mais tu ne peux pas compiler.
- Le **JDK** (*Java Development Kit*) est le JRE **plus** les outils pour développer : le compilateur `javac`, le désassembleur `javap`, l'interpréteur interactif `jshell` que tu utiliseras dans la Leçon 1, l'empaqueteur `jar` et d'autres. Le dossier `bin` d'un Temurin 25.0.4 contient 30 programmes (comptés avec `ls | wc -l` puis recomptés un à un dans la liste : `jar`, `jarsigner`, `java`, `javac`, `javadoc`, `javap`, `jcmd`, `jconsole`, `jdb`, `jdeprscan`, `jdeps`, `jfr`, `jhsdb`, `jimage`, `jinfo`, `jlink`, `jmap`, `jmod`, `jnativescan`, `jpackage`, `jps`, `jrunscript`, `jshell`, `jstack`, `jstat`, `jstatd`, `jwebserver`, `keytool`, `rmiregistry`, `serialver`).

Ce cours a besoin de compiler, donc installe **le JDK**. La confusion historique est que, pendant des années, les gens installaient « Java » et obtenaient le JRE (ce qui suffisait pour exécuter des programmes d'autrui, comme un installateur ou une application de bureau), et que pour programmer il fallait aller chercher « le JDK ». Aujourd'hui, cette distinction n'a presque plus d'importance sur ton ordinateur de développement, puisque tu installes le JDK et c'est tout ; mais elle en a encore sur un serveur ou dans une image de conteneur, où il vaut mieux n'installer que le nécessaire pour exécuter. Certaines distributions publient encore des paquets d'exécution seule ; par exemple, la page d'installation de Temurin pour Linux les propose sous le nom `temurin-<version>-jre`.

Et un troisième sigle qu'il vaut mieux ne pas mélanger : la **JVM** est uniquement la machine virtuelle, un composant qui vit à l'intérieur des deux. « Installer la JVM » n'est pas quelque chose que l'on fait séparément.

#### 0.2.2 Une spécification, un projet et de nombreuses distributions

Ce qui déconcerte le plus quand on vient d'autres langages : **il n'y a pas un seul « Java » à télécharger à un seul endroit**. Il y a trois couches.

1. **La spécification.** Le langage et la JVM sont définis par des documents publics : la [Spécification du langage Java](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html) (JLS) et celle de la JVM (JVMS) que tu as vue en 0.1. N'importe qui peut les implémenter.
2. **OpenJDK.** C'est le projet open source où se développe l'implémentation de référence. Chaque nouvelle version a sa page sur [openjdk.org](https://openjdk.org/projects/jdk/25/) avec les fonctionnalités qu'elle apporte et son calendrier. OpenJDK publie du code source ; ce n'est pas ce que tu installes.
3. **Les distributions.** Ce sont les binaires que quelqu'un construit à partir d'OpenJDK, teste et publie avec sa propre politique de mises à jour : Eclipse Temurin (de la fondation Eclipse, via le projet Adoptium), celles d'Oracle, celles d'Azul, celles d'Amazon, celles qu'Ubuntu inclut dans son dépôt, et d'autres. Toutes exécutent le même langage ; elles diffèrent par qui les construit, combien de temps on les maintient et sous quelle licence.

Ce cours utilise **Eclipse Temurin**, la distribution gratuite du projet Adoptium, de la fondation Eclipse, qui n'a pas de propriétaire commercial et publie un calendrier de mises à jour clair : sa [page de support](https://adoptium.net/support/) indique combien de temps chaque version reçoit des correctifs, et c'est de là que vient le tableau de la section suivante. Toutes les sorties du cours ont été mesurées avec Temurin 25. La Leçon 1 l'installe avec **SDKMAN**, un gestionnaire de versions qui travaille dans ton dossier personnel, sans `sudo`, et qui installe aussi Maven dans la Leçon 6. Elle explique au passage pourquoi on n'utilise **pas** `default-jdk`, le paquet que Linux Mint te suggère. En avant-goût de la raison : sur Mint 22, ce paquet installe Java 21, pas Java 25.

### 0.3 Les versions : rythme, LTS et préversion

#### 0.3.1 Une nouvelle version tous les six mois

Java a cessé de sortir une grosse version à plusieurs années d'intervalle et a adopté un rythme fixe : **une nouvelle version tous les six mois**, en mars et en septembre. La page de support de Temurin le dit : « OpenJDK provide a new feature release every six months ». Chacune apporte peu de fonctionnalités, pensées pour que le changement reste petit.

La conséquence est qu'il y a beaucoup de versions, et que la plupart **cessent de recevoir des mises à jour au bout de six mois**. Ce serait ingérable pour qui maintient un programme en production. C'est pourquoi le concept de LTS existe.

#### 0.3.2 LTS : la version maintenue pendant des années

LTS est le sigle de *Long-Term Support*, « support à long terme ». Une version LTS est une version que les éditeurs s'engagent à maintenir, avec des correctifs de sécurité, pendant des années. La page de support de Temurin la décrit ainsi : tous les deux ans, depuis 2021, une version est désignée LTS, et Temurin s'engage à la maintenir au moins quatre ans. Les chiffres qui y sont publiés (consultés le 7 octobre 2026) :

| Version | Sortie | Type | Temurin la maintient |
|---|---|---|---|
| Java 17 | septembre 2021 | LTS | au moins jusqu'en octobre 2027 |
| Java 21 | septembre 2023 | LTS | au moins jusqu'en décembre 2029 |
| **Java 25** | **septembre 2025** | **LTS** | **au moins jusqu'en septembre 2031** |
| Java 26 | mars 2026 | non LTS | terminé en septembre 2026 |
| Java 27 | 15 septembre 2026 | non LTS | jusqu'en mars 2027 |

Le tableau répond à la question « pourquoi Java 25 s'il y a déjà une 27 ? ». Parce que la 27 **n'est pas LTS** : dans six mois, elle cessera de recevoir des correctifs et il faudra sauter vers une autre. Un cours publié aujourd'hui et lu pendant des années ne doit pas enseigner sur une base qui expire dans six mois. La 25 est la LTS en vigueur, et c'est celle que toute équipe sérieuse aura en production dans les prochaines années. Si le rythme de deux ans se maintient, la prochaine LTS sera la 29, en septembre 2027 ; c'est une extrapolation de ce rythme, pas une promesse de quiconque.

Deux précisions qui évitent des confusions. Premièrement, « LTS » ne veut pas dire « sans changements » : la 25 reçoit des correctifs de sécurité et d'erreurs, mais pas de nouvelles fonctionnalités. Deuxièmement, les numéros de correctif —ce qui vient après `25.` dans ce qu'imprime `java --version`, qui commence par 25— changent tous les trois mois ; c'est pourquoi le cours dit « Java 25 » et jamais un numéro de correctif.

#### 0.3.3 La préversion : ce qui existe mais n'est pas encore définitif

Java intègre les nouvelles fonctionnalités par une étape intermédiaire : la **préversion** (*preview*), décrite dans le [JEP 12](https://openjdk.org/jeps/12). Une fonctionnalité en préversion est déjà spécifiée et programmée, mais l'équipe de Java se réserve le droit de la **changer ou de la retirer** dans la version suivante, selon ce qu'elle apprend de ceux qui l'essaient. Pour empêcher qu'on l'utilise par accident, le compilateur la refuse à moins qu'on ne le demande expressément avec l'option `--enable-preview`.

La préversion a une conséquence concrète qu'il vaut mieux voir sur un cas réel, celui de la **concurrence structurée**, qui reste en préversion dans Java 25 ([JEP 505](https://openjdk.org/jeps/505)). Ce programme l'utilise :

```java
import java.util.concurrent.StructuredTaskScope;

void main() throws Exception {
  try (var alcance = StructuredTaskScope.open()) {
    var tarea = alcance.fork(() -> "hola");
    alcance.join();
    IO.println(tarea.get());
  }
}
```

Sans l'option, le compilateur refuse. Voici ce qu'il imprime, tel quel (c'est un fragment, pas une figure du cours, parce qu'il ne compile pas exprès) :

```text
$ java Preview.java
Preview.java:1: error: StructuredTaskScope is a preview API and is disabled by default.
import java.util.concurrent.StructuredTaskScope;
                           ^
  (use --enable-preview to enable preview APIs)
Preview.java:4: error: StructuredTaskScope is a preview API and is disabled by default.
  try (var alcance = StructuredTaskScope.open()) {
                     ^
  (use --enable-preview to enable preview APIs)
2 errors
error: compilation failed
```

Le message est clair : « c'est une API en préversion et elle est désactivée par défaut ». Avec `java --enable-preview --source 25 Preview.java`, le même programme imprime `hola` (« salut »). Mais il y a un piège que le message ne montre pas : un `.class` compilé avec des fonctionnalités en préversion porte dans son en-tête la marque de « préversion », mesurée avec `javap -v` : `minor version: 65535`, au lieu du `0` habituel. Cette marque fait que **seule la version exacte de Java avec laquelle il a été compilé peut l'exécuter**. Un programme construit sur une fonctionnalité en préversion de la 25 peut cesser de fonctionner en passant à la 26. C'est pourquoi le cours fixe une règle qui ne se discute pas : **rien en préversion n'est utilisé dans le code du cours**. La concurrence structurée apparaît dans la Leçon 9 sous forme d'encadré « ce qui arrive », avec son option et l'avertissement de ne pas l'utiliser en production ; pour le reste, uniquement ce qui est déjà définitif dans la 25.

### 0.4 Ce que le compilateur protège et ce qu'il ne protège pas

C'est la section la plus importante de la leçon, et celle qui ressemble le plus à une carte de champ de mines. Java se vante d'être un langage à typage statique, c'est-à-dire que le compilateur vérifie les types **avant** l'exécution du programme. C'est vrai, et c'est un énorme avantage. Mais « typage statique » ne veut pas dire « sans erreurs à l'exécution », et il y a quatre trous qu'il vaut mieux connaître dès le premier jour. Chacun vient avec un programme à exécuter après la Leçon 1.

#### 0.4.1 Ce que le compilateur attrape bel et bien : les types

Commençons par ce qu'il protège. Le programme `fig00_01.java` déclare une variable censée contenir un nombre entier et lui assigne un texte :

```java
// fig00_01.java
void main() {
  int tiempoMs = "142";
  IO.println(tiempoMs);
}
```

Ce programme **ne s'exécute pas** : le compilateur le refuse avant. Exécute-le avec `java fig00_01.java` et tu obtiens :

```text
$ java fig00_01.java
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
1 error
error: compilation failed
```

Lis-le de haut en bas, comme tu le liras des centaines de fois. La première ligne apporte trois informations : le fichier (`fig00_01.java`), le numéro de ligne (`3`) et le diagnostic (`incompatible types: String cannot be converted to int`, « types incompatibles : un `String` ne peut pas être converti en `int` »). En dessous, le compilateur recopie la ligne en cause et place une flèche `^` à l'endroit où il s'en est rendu compte. À la fin, `1 error` compte les problèmes et `error: compilation failed` est le lanceur qui dit qu'il n'a rien exécuté. Remarque aussi ce qui ne s'est pas produit : le programme n'a rien imprimé, pas même un résultat partiel. Une erreur de compilation arrête tout **avant** la première instruction.

Les lignes du programme, pour qu'elles ne restent pas un mystère : `void main()` est la façon de démarrer un programme (la Leçon 1 l'explique en détail), `int tiempoMs` déclare une variable de type entier, et `IO.println` imprime une ligne. Ce qui compte, c'est la comparaison avec tes autres langages. En Python, l'équivalent (`tiempo_ms: int = "142"`) s'exécute sans se plaindre, parce que les annotations ne sont pas vérifiées à l'exécution. En TypeScript, le compilateur l'attrape comme Java. En Go, aussi. Java fait partie du groupe qui l'attrape avant l'exécution.

#### 0.4.2 Premier trou : les génériques sont effacés

Les **génériques** sont la syntaxe de Java pour dire « une liste *de textes* », « une liste *de nombres* » : `ArrayList<String>`, `ArrayList<Integer>`. (Le sujet est celui de la [Leçon 4](04-colecciones-genericos-streams.md) ; ici, seule une conséquence nous intéresse.) Le compilateur utilise cette information pour vérifier ton code. Mais après la compilation, **l'information disparaît** : le compilateur l'efface et laisse le *bytecode* sans elle. Cela s'appelle l'**effacement des types** (*type erasure*).

`fig00_02.java` le montre : il crée une liste de textes et une liste de nombres et demande si elles sont de la **même classe** :

```java
// fig00_02.java
import java.util.ArrayList;

void main() {
  var palabras = new ArrayList<String>();
  var numeros = new ArrayList<Integer>();
  IO.println(palabras.getClass() == numeros.getClass());
  IO.println(palabras.getClass().getName());
}
```

```text
$ java fig00_02.java
true
java.util.ArrayList
```

Le programme, ligne par ligne. La première ligne, `import`, dit au compilateur où trouver `ArrayList` : elle est dans le paquet `java.util` des bibliothèques standard. `var` demande au compilateur de déduire le type de la variable à partir de la valeur qu'on lui assigne (la Leçon 2 l'explique). Les deux `new ArrayList<...>()` créent une liste vide, l'une annoncée comme une liste de textes et l'autre comme une liste de nombres. `getClass()` renvoie la classe d'un objet **à l'exécution**, et `==` compare si c'est la même. La réponse est `true` : pour la JVM, les deux listes sont un `java.util.ArrayList`, sans plus. Le `<String>` et le `<Integer>` n'existent plus quand le programme s'exécute.

Pourquoi l'a-t-on conçu ainsi ? Par compatibilité. Les génériques sont arrivés dans Java à la version 5, et il existait alors des millions de lignes de code et de bibliothèques qui n'en avaient pas. Effacer l'information a permis à l'ancien et au nouveau code de cohabiter. La spécification elle-même reconnaît cet héritage quand elle parle des « types bruts » (*raw types*), qui ne sont permis que « comme une concession à la compatibilité avec l'ancien code » ([JLS, section 4.8](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.8)). Un avantage collatéral, que souligne le [tutoriel officiel](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html), est que les génériques ne coûtent rien à l'exécution : on ne crée pas de nouvelles classes pour chaque combinaison.

La conséquence pour toi est que certaines choses que tu attendrais d'un langage doté de types génériques **ne peuvent pas se faire** en Java : demander à l'exécution « est-ce une liste de textes ? » n'a pas de réponse, parce que cette information n'existe pas. Quand tu arriveras à la Leçon 4 et que tu rencontreras des restrictions étranges avec les génériques, la cause sera presque toujours cet effacement. Compare avec TypeScript, où **tous** les types sont effacés à la génération du JavaScript. En Java, seuls les arguments des génériques sont effacés ; la classe de l'objet, elle, survit, et c'est pourquoi le programme a pu imprimer `java.util.ArrayList`.

#### 0.4.3 Deuxième trou : `null` existe toujours

Java a des types, mais il n'empêche pas qu'une variable d'un type objet, comme `String`, vaille **« rien »**. Ce « rien » s'écrit `null`. Le compilateur accepte que tu l'assignes et ne t'avertit pas quand tu l'utilises mal. `fig00_03.java` déclare un détail de service, le laisse à `null` et lui demande sa longueur :

```java
// fig00_03.java
void main() {
  String detalle = null;
  IO.println("antes de usar el detalle");
  IO.println(detalle.length());
  IO.println("esto no se imprime");
}
```

```text
$ java fig00_03.java
antes de usar el detalle
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "<local1>" is null
	at fig00_03.main(fig00_03.java:5)
```

Ce programme **compile sans problème**. L'erreur n'apparaît que lorsque la cinquième ligne s'exécute, et c'est pourquoi le premier `IO.println` a eu le temps d'imprimer. C'est la différence entre une erreur de compilation (0.4.1 : rien ne s'exécute) et une erreur d'exécution : le programme s'exécute jusqu'à ce qu'il se heurte au problème, et s'arrête là. Le nom de l'erreur, `NullPointerException`, signifie « exception de pointeur nul » ; une **exception** est la façon qu'a Java de signaler que quelque chose s'est mal passé pendant l'exécution (la [Leçon 5](05-errores-excepciones.md) l'étudie en entier). Son message dit ce que tu as essayé de faire (`Cannot invoke "String.length()"`, « impossible d'invoquer `String.length()` ») et pourquoi cela a échoué (`because "<local1>" is null`, « parce que `<local1>` est nul »). Depuis Java 15, la JVM donne par défaut des messages détaillés comme celui-ci, grâce au [JEP 358](https://openjdk.org/jeps/358).

Tu seras peut-être surpris que la variable s'appelle `<local1>` au lieu de `detalle`. Ce n'est pas une erreur de la leçon : quand tu exécutes un fichier directement avec `java Archivo.java`, le compilateur interne ne conserve pas les noms des variables locales. La [Leçon 2](02-tipos-cadenas-control.md) montre comment les récupérer avec une autre façon de compiler. Ce qui compte aujourd'hui, c'est l'idée : **le compilateur de Java ne te protège pas de `null`**.

L'invention de `null` a été regrettée par son propre auteur, Tony Hoare, qui l'a appelée en 2009 « mon erreur à un milliard de dollars » ([conférence](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/)), à cause des pannes qu'elle a provoquées pendant des décennies. Java ne l'a pas supprimée, mais avec les années il offre des outils pour ne pas en dépendre : dans la Leçon 3, les types scellés remplacent les `null` du `revisor` par un type qui dit explicitement « il y a eu une réponse » ou « il n'y en a pas eu », et dans la Leçon 4, `Optional` couvre les cas de « parfois il y a une donnée ».

#### 0.4.4 Troisième trou : le `+` n'additionne pas toujours

Le troisième est un piège pour qui vient d'un autre langage. `fig00_04.java` utilise l'opérateur `+` de trois façons :

```java
// fig00_04.java
void main() {
  String tiempo = "142";
  IO.println(tiempo + 1);
  IO.println(1 + 42 + "ms");
  IO.println("ms" + 1 + 42);
}
```

```text
$ java fig00_04.java
1421
43ms
ms142
```

Les trois résultats viennent d'une seule règle : **si l'un des deux côtés du `+` est un texte, le `+` concatène ; si les deux sont des nombres, il additionne** ; et l'évaluation se fait de gauche à droite. La première ligne assemble le texte `"142"` avec le nombre `1` et donne `"1421"`, un texte de quatre caractères, pas le nombre 143. La deuxième additionne d'abord `1 + 42` (les deux sont des nombres) et donne `43`, et c'est seulement ensuite qu'elle l'assemble avec `"ms"`. La troisième commence par un texte, donc `"ms" + 1` est déjà une concaténation et donne `"ms1"`, puis `"ms1" + 42` donne `"ms142"`. La règle figure dans la [spécification du langage](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.18.1).

Ce qui rend ce cas dangereux n'est pas qu'il soit illégal, mais qu'il **compile sans un seul avertissement** et donne un résultat plausible. Si, dans le `revisor`, un temps arrive sous forme de texte, `"142" + 1` ne t'avertira de rien : il produira `"1421"`, et cette fausse donnée voyagera jusqu'au rapport. En Python, `"142" + 1` est un `TypeError` sur-le-champ ; en TypeScript, il donne `"1421"` comme en Java. Sur ce point, Java est du côté de TypeScript, et c'est pourquoi la Leçon 2 insiste pour que chaque donnée du `revisor` ait le bon type dès son entrée.

#### 0.4.5 Quatrième trou : ce que le compilateur ne verra jamais

Les trois précédents peuvent se provoquer avec des programmes de quelques lignes. Il y a un quatrième groupe, plus large : **les valeurs que l'on ne connaît qu'à l'exécution**. Le compilateur vérifie les types ; il ne vérifie pas les valeurs. Ces fragments ont été exécutés et voici ce qu'ils impriment :

```java
void main() {
  int maximo = Integer.MAX_VALUE;
  IO.println(maximo + 1);
}
```

```text
$ java Desborde.java
-2147483648
```

Un entier Java a une taille fixe (quatre octets), et si une addition dépasse le maximum, le résultat **fait le tour** jusqu'au minimum, sans erreur. La spécification le dit sans détour : « les opérateurs entiers n'indiquent d'aucune façon un dépassement de capacité » ([JLS, section 4.2.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2.2)). Deux autres cas, mesurés de la même façon, chacun dans son propre fichier avec un `void main()` qui ne contient que la ligne indiquée :

```java
IO.println(1 / 0);
```

```text
$ java Division.java
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at Division.main(Division.java:2)
```

```java
IO.println("hola".charAt(10));
```

```text
$ java Caracter.java
Exception in thread "main" java.lang.StringIndexOutOfBoundsException: Index 10 out of bounds for length 4
	at java.base/jdk.internal.util.Preconditions$1.apply(Preconditions.java:55)
	...
	at Caracter.main(Caracter.java:2)
```

Les points de suspension remplacent neuf lignes internes du JDK, dont les numéros changent à chaque mise à jour. Diviser un entier par zéro est une exception à l'exécution ; demander le caractère numéro 10 d'un texte de quatre caractères aussi. Dans aucun des deux cas, le compilateur ne pouvait le savoir sans exécuter le programme. Le résumé de la section tient dans un tableau :

| Quoi | Quand est-ce détecté ? | Exemple de cette leçon |
|---|---|---|
| Un texte là où va un nombre | À la compilation | `int tiempoMs = "142";` (fig00_01) |
| Un `null` utilisé comme si c'était un objet | À l'exécution | `detalle.length()` (fig00_03) |
| Un `+` qui concatène au lieu d'additionner | Jamais : le résultat est valide | `"142" + 1` (fig00_04) |
| Le type des éléments d'une liste | À la compilation, puis il est effacé | `ArrayList<String>` (fig00_02) |
| Un entier qui dépasse son maximum | Jamais : il fait le tour | `Integer.MAX_VALUE + 1` |
| Division entière par zéro | À l'exécution | `1 / 0` |

Le reste du cours cherche à rendre cette colonne du milieu aussi précoce que possible : faire passer des erreurs de « jamais » ou « à l'exécution » à « à la compilation ». Les types scellés de la Leçon 3 font qu'oublier un cas devient une erreur du compilateur. Les collections immuables de la Leçon 4 empêchent les changements accidentels. Les tests de la Leçon 7 attrapent ce que le compilateur ne peut pas. Ce n'est pas un hasard : c'est la méthode.

### 0.5 Java face à ce que tu connais déjà

Avec la carte qui précède, la comparaison avec les autres langages du cours cesse d'être vague. Voici ce qui change, et ce qui ne change pas :

| | Go | TypeScript | Python | **Java** |
|---|---|---|---|---|
| Que produit le compilateur ? | un exécutable pour un système et un processeur | du JavaScript (les types disparaissent) | du *bytecode* de l'interpréteur, automatiquement | du *bytecode* de la JVM, dans une étape explicite |
| Quand les types sont-ils vérifiés ? | à la compilation | à la compilation (avec `tsc`) | ils ne sont pas vérifiés à l'exécution | à la compilation |
| Qu'est-ce qui exécute le programme ? | le processeur directement | Node ou un navigateur | l'interpréteur Python | la JVM |
| Y a-t-il un ramasse-miettes ? | oui | oui (le moteur JavaScript) | oui | oui |
| Comment le livre-t-on ? | copier l'exécutable | copier le JavaScript et Node | copier le code et l'interpréteur | copier un JAR (un fichier contenant des `.class`) et avoir une JVM |

Deux choses sautent aux yeux. La première : si tu viens de Python, le modèle « code source, *bytecode*, machine virtuelle » **tu le connais déjà** ; Python compile lui aussi en *bytecode* et l'exécute dans une machine virtuelle. Ce qui change en Java, c'est que l'étape de compilation est explicite et visible, et qu'elle vérifie les types au passage. La seconde : si tu viens de Go, la différence est la JVM comme intermédiaire. Un exécutable Go embarque tout ; un programme Java a besoin d'une JVM installée là où il s'exécute, en échange d'être portable entre systèmes avec le même fichier.

Il existe aussi une voie pour produire un exécutable natif à partir d'un programme Java (compilation anticipée avec GraalVM). Elle n'est pas utilisée dans ce cours. Le blog de Hábil explique [quand il convient, et quand il ne convient pas, de compiler Java en natif](https://www.habil.mx/fr/blog/java-natif-quand-oui-quand-non/) ; lis-le quand tu auras terminé la Leçon 10, pas avant.

### 0.6 Ce qui vient dans le reste du cours

Tout ce qui précède est mis en pratique dans les leçons suivantes. Le cours construit un seul programme, le `revisor`, et chaque leçon ajoute une pièce :

| Leçon | Ce qui s'ajoute | Quel trou de cette leçon elle aide à combler |
|---|---|---|
| 1 | Tu installes Java 25 et tu écris et compiles ton premier programme | le chemin de 0.1, dans ton terminal |
| 2 | Types, chaînes et contrôle de flux | `null`, `==` contre `equals`, le `+` qui concatène |
| 3 | `record`, `enum` et types scellés | `null` remplacé par des types qui énoncent tous leurs cas |
| 4 | Collections, génériques et *streams* | l'effacement des types, des listes qui ne changent pas |
| 5 | Erreurs et exceptions | les erreurs d'exécution, traitées au lieu d'être tolérées |
| 6 | Projet avec Maven (l'outil qui construit les projets Java) | le compilé comme produit : paquets, un `pom.xml` et un `.jar` |
| 7 | Tests avec JUnit | ce que le compilateur ne voit pas, attrapé par des tests |
| 8 | HTTP et JSON | des données qui arrivent de l'extérieur et qu'il faut valider |
| 9 | Concurrence | vérifier tous les services en même temps |
| 10 | Le programme terminé | ligne de commande, serveur, qualité et un JAR |

Il y a un accord pratique de plus. Les programmes des Leçons 1 à 5 s'exécutent avec `java Archivo.java`, sans outils supplémentaires. À partir de la Leçon 6, le projet vit dans une structure standard, avec Maven. Le cours utilise une **indentation de deux espaces** dans tout le code, celle du guide de style de Google pour Java, parce que dans la Leçon 10 un outil vérifie ce style et il serait absurde d'avouer alors que tout ce qui précède était écrit autrement. Beaucoup de code sur internet utilise quatre espaces ; cela fonctionne pareil.

## L'erreur que tu vas voir

Cette leçon a une erreur principale, celle du compilateur qui refuse, et une erreur d'exécution qu'il faut savoir reconnaître au premier regard. Les deux viennent des sections précédentes.

**L'erreur principale : `incompatible types`.** C'est celle de `fig00_01.java` :

```text
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
```

Elle se lit ainsi : le fichier et la ligne (`fig00_01.java:3`), ce qui s'est passé (un `String` ne peut pas être converti en `int`), la ligne en cause, et la flèche à l'endroit exact. Elle se corrige de deux façons, selon ce que tu voulais : si la donnée était un texte, la variable devait être de type `String` ; si tu voulais le nombre, il faut convertir explicitement le texte (la Leçon 2 s'en occupe). Ce qu'on ne fait **pas**, c'est l'ignorer : une erreur de compilation ne se « laisse jamais passer », parce que le programme ne s'exécute tout simplement pas.

**Une erreur d'exécution que tu peux déjà reconnaître : `NullPointerException`.**

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "<local1>" is null
```

Elle se lit de gauche à droite : dans le thread principal (`main`), une `NullPointerException` s'est produite ; tu as essayé d'invoquer `String.length()` et cela n'a pas été possible parce que ce qu'il y avait dans la variable était `null`. En dessous, la ligne qui commence par `at` dit **où** : `fig00_03.main(fig00_03.java:5)`, c'est-à-dire la méthode `main` du fichier, ligne 5. Regarde cette ligne et demande-toi d'où a pu venir un `null`.

## Ce qui se fait de travers

**Installer « Java » sans savoir ce que l'on installe.** Qui tape `sudo apt install default-jre` n'obtient que la partie d'exécution, et, sur Mint 22, d'une version antérieure à celle qu'utilise le cours. Pour programmer, le JDK ; pour le cours, celui de Java 25.

**Utiliser la version la plus récente parce que c'est la plus récente.** Java 27 est sortie le 15 septembre 2026 et n'est pas LTS. Pour apprendre, pour un serveur ou pour un programme qui va vivre des années, la LTS en vigueur. Si tu as déjà la 27 installée, rien de grave pour ce cours, mais la Leçon 1 te dit comment t'assurer de la version que tu utilises.

**Croire qu'« avoir des types » signifie « ne pas échouer à l'exécution ».** Les types attrapent une famille d'erreurs, pas toutes. Celles de 0.4 existent dans n'importe quel programme Java, écrit par qui que ce soit.

**Copier un tutoriel sans regarder sa date.** Sur internet circulent des versions de « Hello world » vieilles de vingt ans qui obligent à écrire une classe et une méthode `public static void main(String[] args)`. Elles fonctionnent, mais pour un fichier d'un seul programme plus rien de tout cela n'est nécessaire, comme tu le verras dans la Leçon 1. Il en va de même pour les bibliothèques : presque tout ce qui est publié sur JSON ou sur les tests est écrit pour des versions antérieures à celles du cours. Quand une recette ne compile pas, la première chose à vérifier, c'est la version.

**Utiliser une fonctionnalité en préversion « parce que ça marche ».** Ça marche aujourd'hui, avec l'option, dans cette version exacte. Selon le JEP 12, elle peut changer ou disparaître dans la suivante. Pas dans du code que quelqu'un d'autre va maintenir.

**Confondre Java et JavaScript.** Ils partagent des lettres du nom et la moitié des accolades, et rien de plus. Ce sont des langages différents, avec des modèles d'exécution différents et une relation historique que personne n'a besoin de connaître pour utiliser l'un des deux.

**Attendre que « écris une fois, exécute partout » inclue l'environnement.** Le `.class` est portable ; l'encodage de la sortie, les chemins et les sauts de ligne ne le sont pas d'eux-mêmes. On mesure, on ne suppose pas.

## Exercices

Les exercices de cette leçon ne demandent pas Java : le premier est de lecture, le deuxième se répond **avant** d'exécuter quoi que ce soit et se vérifie après la Leçon 1, et le troisième et le quatrième se répondent par écrit, avec tes mots. La meilleure façon de profiter du deuxième est d'écrire ta prédiction dans le [journal de bord](bitacora.md) et de la comparer ensuite.

### Exercice 1 — Les données du `revisor` (projet)

Voici le tableau que produira le `revisor` à la fin du cours. Pour chaque colonne, indique si la donnée est un **nombre**, un **texte** ou peut être **rien**. Fais surtout attention au temps de `reportes`.

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Ensuite, réponds : que ferait Java avec `"142" + 1`, si un temps arrivait sous forme de texte ? Et quel problème y aurait-il si un programme enregistrait le temps de `reportes` comme `null` puis essayait de le formater ?

### Exercice 2 — Quand le programme s'en aperçoit-il ?

Pour chacun de ces six fragments, prédis si (a) le compilateur le refuse, (b) le programme compile et échoue à l'exécution, ou (c) il compile, s'exécute et donne un résultat sans avertir. Si c'est (b), dis quelle exception tu attends.

1. `int tiempoMs = "142";`
2. `String detalle = null;` suivi de `detalle.length()`.
3. `Integer.MAX_VALUE + 1`
4. `var palabras = new java.util.ArrayList<String>();` suivi de `palabras.add(5);`
5. `1 / 0` (entre entiers).
6. `"hola".charAt(10)`

### Exercice 3 — Choisis la version

Décide quelle version de Java tu utiliserais dans chaque cas et pourquoi : (a) un nouveau programme que ton équipe va maintenir pendant plusieurs années ; (b) tu as Java 27 installé et tu veux suivre ce cours ; (c) un article utilise `StructuredTaskScope` et tu veux l'essayer sur ta machine.

### Exercice 4 — Explique-le en cinq phrases

Un collègue qui programme en Go te demande ce qui se passe entre « j'ai écrit un `.java` » et « le programme s'exécute ». Réponds-lui en cinq phrases au maximum, en utilisant les mots *code source*, `javac`, *bytecode*, JVM et compilateur JIT. Sans regarder la leçon.

## Solutions

### Solution 1

Les colonnes, dans l'ordre :

- **SERVICIO** est un **texte** (`catalogo`, `pagos`...). **ESTADO** est un **texte** qui ne peut valoir que l'une de trois choses (`ARRIBA`, `LENTO`, `CAIDO`) ; dans la Leçon 3, il devient un type propre qui n'en admet pas d'autres.
- **TIEMPO** est un **nombre** (142, 2300, 87) affiché avec une unité (`142ms`, `2.3s`) et qui **peut être rien** : `reportes` n'a pas répondu et n'a pas de temps. Ce tiret `—` représente une donnée qui n'existe pas.
- **DETALLE** est un **texte** même si l'on voit un nombre : le `200` est le code d'état, mais dans le tableau il s'affiche comme du texte, tout comme `sin conexión`.

Avec `"142" + 1`, Java **n'additionne pas** : il concatène et donne le texte `"1421"` (fig00_04). Comme cela compile sans avertir, une fausse donnée entrerait dans le rapport sans que personne s'en aperçoive. Et avec le temps de `reportes` enregistré comme `null`, le formater avec quelque chose comme `tiempo.length()` lève une `NullPointerException` à l'exécution (fig00_03), parce que le compilateur ne protège pas de cela. Ces deux situations sont la raison d'être des Leçons 2 et 3 : dans la 2, on apprend à écrire le code avec soin face à `null`, et dans la 3, on change la conception pour que « il n'y a pas eu de réponse » soit un cas explicite du type et non un `null` caché.

### Solution 2

Après la Leçon 1, tu peux les vérifier avec `jshell` ou avec un fichier de programme. Les réponses, mesurées avec Temurin 25.0.4 :

1. **(a)** Le compilateur le refuse : `incompatible types: String cannot be converted to int` (fig00_01).
2. **(b)** Il compile et échoue à l'exécution avec `java.lang.NullPointerException` (fig00_03).
3. **(c)** Il compile, s'exécute et donne `-2147483648`, sans avertissement : l'entier fait le tour.
4. **(a)** Le compilateur le refuse. C'est la valeur du générique : la liste est de textes et `5` n'en est pas un. Le message commence par `error: no suitable method found for add(int)` et se poursuit par une liste des variantes de `add` qu'il a essayées, chacune avec sa raison (celle qui compte dit `argument mismatch; int cannot be converted to String`). C'est un exemple du fait que le compilateur utilise bien les génériques, même s'il les efface ensuite.
5. **(b)** Il compile et échoue à l'exécution avec `java.lang.ArithmeticException: / by zero`.
6. **(b)** Il compile et échoue à l'exécution avec `java.lang.StringIndexOutOfBoundsException: Index 10 out of bounds for length 4`.

Si tu t'es trompé sur le 3, c'est normal : c'est le seul où le programme « fonctionne » et donne un nombre faux, et c'est pourquoi c'est le plus dangereux des six.

### Solution 3

(a) **Java 25**, la LTS en vigueur : elle aura des correctifs de sécurité au moins jusqu'en septembre 2031. Une version non LTS cesserait d'être maintenue au bout de six mois.

(b) **Inutile de désinstaller quoi que ce soit, mais il ne convient pas non plus de l'utiliser pour le cours.** Java 27 n'est pas LTS et le cours vérifie ses programmes avec la 25 ; le plus sûr est d'installer Temurin 25 (avec SDKMAN, la Leçon 1, les deux cohabitent et tu choisis laquelle utiliser) et de vérifier avec `java --version` laquelle est active. Ce qu'il ne faut pas faire, c'est supposer que « la plus récente convient à tout ».

(c) **Dans la version 25, avec `--enable-preview`**, en sachant que c'est une fonctionnalité en préversion : l'API peut changer dans la version suivante (elle a d'ailleurs changé entre la 25 et la 26, selon le [JEP 525](https://openjdk.org/jeps/525)), le `.class` obtenu ne s'exécute que dans cette version exacte et il ne faut pas l'utiliser dans un programme qu'un autre va maintenir.

### Solution 4

Une réponse possible : « Tu écris le *code source* dans un `.java`. Le compilateur `javac` le vérifie (surtout les types) et le traduit en *bytecode*, qui va dans un `.class`. Ce *bytecode* n'est pas le code d'un processeur réel : c'est la JVM qui l'exécute, et elle existe pour chaque système d'exploitation. Pendant que le programme s'exécute, la JVM repère les méthodes très utilisées et les convertit en code machine avec le compilateur JIT. C'est pourquoi le même `.class` s'exécute sur tout système qui a une JVM, contrairement à un exécutable Go, qui embarque déjà le code de son processeur ».

## Comment savoir que j'ai réussi

- [ ] Tu dessines de mémoire le chemin `.java` → `javac` → `.class` → `java` → JVM, et tu dis ce que fait chaque flèche.
- [ ] Tu dis la différence entre JDK, JRE et JVM, et lequel s'installe pour ce cours.
- [ ] Tu expliques pourquoi il y a une Java 27 et que le cours utilise la 25, avec le mot LTS.
- [ ] Tu sais ce qu'est une fonctionnalité en préversion et quelle option elle exige.
- [ ] Pour chaque ligne du tableau de 0.4.5, tu dis quand le problème est détecté.
- [ ] Après la Leçon 1, tu exécutes `java fig00_01.java`, `fig00_02.java`, `fig00_03.java` et `fig00_04.java` et tes sorties coïncident avec celles de cette leçon, au chemin près.
- [ ] Tu prédis dans le journal de bord ce qu'imprime `"ms" + 1 + 42` avant de l'exécuter, et tu vérifies.
- [ ] Tu as noté dans ton journal de bord une chose que tu croyais de la JVM et qui s'est révélée différente.

## Résumé

Réponds sans regarder la leçon :

1. Que contient un `.class` et qui l'exécute ?
2. Qu'ajoute le JDK au JRE ? Pourquoi installe-t-on le JDK dans ce cours ?
3. Qu'est-ce qu'OpenJDK et qu'est-ce qu'une distribution comme Temurin ?
4. Que signifie LTS ? Pourquoi Java 27 n'est-elle pas la version du cours ?
5. Que se passe-t-il avec un `.class` compilé avec des fonctionnalités en préversion ?
6. En quoi une erreur de compilation et une erreur d'exécution se distinguent-elles ? Donne un exemple de chacune.
7. Qu'est-ce que l'effacement des génériques et que prouve `fig00_02.java` ?
8. Pourquoi `"142" + 1` vaut-il `"1421"` et non `143` ?
9. Pourquoi le compilateur de Java ne t'avertit-il pas d'un `null` mal utilisé ?

## Pour aller plus loin

- [JEP 512, « Compact Source Files and Instance Main Methods »](https://openjdk.org/jeps/512) : la proposition qui permet, dans Java 25, d'écrire un programme sans classe ni `public static`, ce qui est le style de ce cours. Elle se lit mieux après la Leçon 1. Consulté le 7 octobre 2026.
- [JEP 12, « Preview Features »](https://openjdk.org/jeps/12) : ce qu'est la préversion, pourquoi elle existe et quelles garanties elle ne donne pas. Consulté le 7 octobre 2026.
- [Spécification de la machine virtuelle Java, SE 25, chapitre 4](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html) : le format exact du fichier `.class`, y compris le nombre magique et la version. Consulté le 7 octobre 2026.
- [Temurin Support, Adoptium](https://adoptium.net/support/) : le calendrier des versions et la fin de support de chacune, d'où vient le tableau de 0.3. Consulté le 7 octobre 2026.

---

**Suivante :** [Leçon 1 — Installer Java 25 sur ton Linux Mint](01-instalacion.md)
