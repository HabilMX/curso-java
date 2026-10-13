# Leçon 2 — Types, chaînes et contrôle de flux

**Durée :** 2 × 90 min (deux séances ; la coupure se situe entre les sections 2.2 et 2.3)

**Ce que tu construis :** les fonctions de base du `revisor` : décider si un service est en ligne, lent ou tombé, mettre en forme le temps et composer les lignes du tableau, le tout dans un seul fichier compact

**Ce que tu apprends :** types primitifs et types référence, classes enveloppes et `var` ; `String` est immuable et pourquoi `==` n'est pas `equals` ; mettre en forme avec `String.format` et `Locale.ROOT` ; blocs de texte ; `if`, `&&` contre `&` et l'opérateur `? :` ; `switch` comme expression ; méthodes et constantes dans un fichier compact ; `null` et comment lire le message d'une `NullPointerException`

**D'où tu viens.** Tu apportes l'environnement de la [Leçon 1](01-instalacion.md) : un JDK 25 qui répond à `java --version`, l'habitude d'exécuter un fichier avec `java Archivo.java` et un `~/curso-java/revisor/Revisor.java` d'une seule ligne qui imprime « revisor: todavía no revisa nada » (« revisor : il ne vérifie encore rien »). Tu sais aussi lire la première erreur du compilateur : le fichier, le numéro de ligne, le `^` qui désigne l'endroit et les lignes `symbol` et `location`. Cette leçon utilise cette compétence en permanence, parce que Java est un langage qui **te gronde avant d'exécuter**, et apprendre à comprendre ses réprimandes est la moitié du métier.

**Ce que cette leçon ne fait pas.** Elle n'utilise ni listes, ni boucles, ni classes à toi, et ne consulte aucun réseau. Les données des quatre services du `revisor` sont écrites dans deux `switch` parallèles, exprès : c'est une solution inconfortable, et la [Leçon 3](03-records-enums-sellados.md) la remplace par des types qui expriment ce que signifient « un service a répondu » et « un service n'a pas répondu ». Les collections arrivent dans la Leçon 4 ; les exceptions, dans la 5. Ici, quand un programme échoue exprès, nous l'utilisons pour **lire** l'erreur, pas pour l'attraper.

**À propos des exécutions.** Chaque figure est un fichier `fig02_NN.java` qui s'exécute avec `java fig02_NN.java`, depuis le dossier où tu l'as enregistré. Tu les as déjà toutes écrites dans le dépôt, dans [`programas/02-tipos-cadenas-control/`](../programas/02-tipos-cadenas-control/), mais tu apprends davantage si tu les tapes. Toutes les sorties de cette leçon ont été obtenues avec Temurin 25.0.4 et la configuration régionale à `C.UTF-8`. Si dans ton terminal tu vois `?` ou d'autres signes bizarres à la place des accents ou du tiret « — », le problème n'est pas ton programme mais l'encodage : vérifie la configuration régionale avec la commande `locale` (ses valeurs devraient se terminer par `UTF-8`, ce qui est normal sous Mint) et l'encodage de ton terminal. À titre de référence, sur la machine où le cours a été mesuré, avec `LANG=C`, Java a imprimé `?` à la place de ces caractères ; sur un autre système, le symptôme peut être différent. Si ton correctif de Java est un autre, les messages d'erreur sont les mêmes ; la seule chose qui change entre les mises à jour, ce sont les numéros de ligne des lignes `at java.base/...` d'une trace, qui appartiennent au JDK et non à toi. Les figures qui échouent exprès ont leur erreur à côté, dans un fichier `fig02_NN.error-esperado.txt`.

## À la fin, tu seras capable de

- Choisir entre `int`, `long`, `double` et `boolean` pour une donnée, et expliquer ce qui arrive à un `int` qui dépasse son maximum.
- Distinguer une valeur primitive d'une référence et d'une classe enveloppe (`int` contre `Integer`), et dire quand une variable peut valoir `null`.
- Utiliser `var` là où il aide et savoir quel type le compilateur lui a donné.
- Expliquer pourquoi `s.strip();` ne change pas `s`, et pourquoi `==` entre deux `String` peut donner `false` même si elles se ressemblent.
- Mettre en forme un texte avec `String.format` et `Locale.ROOT`, et écrire un bloc de texte de plusieurs lignes.
- Écrire un `switch` comme expression avec des flèches, et expliquer pourquoi `&&` et `&` ne sont pas la même chose.
- Lire le message d'une `NullPointerException`, savoir quelle variable était nulle et pourquoi le message dit parfois `<local1>` au lieu de son nom.

## Le pourquoi avant le comment

Tu arrives à Java avec un langage sur le dos, et ce langage t'a laissé des habitudes. Certaines servent ici telles quelles : un `if` est un `if`. D'autres sont des pièges, parce que Java ressemble assez à ce que tu sais déjà pour que tu ne te méfies pas. Pense à trois expressions qui, dans le langage que tu apportes, signifient peut-être une chose, et qui en Java en signifient une autre :

- Comparer deux textes avec `==`. En Python ou en Go, cela compare leur contenu. En Java, cela compare s'ils sont **le même objet**, et il arrive que cela te dise `true` par hasard.
- Appeler `texto.strip()` et penser que `texto` est devenu propre. En Java, le texte **ne change jamais** : la méthode t'en renvoie un autre, et si tu ne le gardes pas, il se perd sans avertissement.
- Écrire `x != null & x.length() > 0` en croyant que c'est la même chose qu'avec `&&`. Cela explose avec une `NullPointerException` précisément quand `x` est nul, c'est-à-dire précisément quand la vérification servait.

Il existe une étude portant sur des centaines de milliers d'étudiants de Java (Brown et Altadmri, avec les données de BlueJ) qui a mesuré quelles erreurs mettent le plus de temps à être corrigées. Les enseignants qui y ont participé ne sont pas tombés juste : leur intuition sur celles qui étaient difficiles ne coïncidait pas avec les données. Celles qui prennent le plus de temps sont justement ces trois-là : confondre `==` avec `equals`, confondre `&` avec `&&` et ne pas utiliser la valeur que renvoie une méthode. C'est pourquoi cette leçon ne les mentionne pas en passant : **chacune est provoquée exprès, on la voit échouer et on l'explique**. Tu peux lire l'article sur la [page de l'Université de Kent](https://kar.kent.ac.uk/57219/).

Le parcours comporte deux séances. La **première** couvre 2.1 et 2.2 : les types et les chaînes. En la terminant, tu sauras ce que garde chaque variable et ce que fait un texte quand tu le « modifies ». La **seconde** couvre 2.3 à 2.5 : le contrôle de flux, le `null` et l'étape du projet. En la terminant, tu auras le `revisor` qui imprime ce tableau, calculé par des fonctions à toi :

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Ce tableau est le fil conducteur de tout le cours. Remarque la dernière ligne : `reportes` n'a pas répondu, donc il n'a ni temps ni code. Comment représenter « il n'y a pas de donnée » dans un langage aux types stricts est la question qui traverse toute la leçon, et sa mauvaise réponse —`null`— est la raison de la moitié des erreurs que tu vas voir.

## Les concepts

### 2.1 Types

#### 2.1.1 Primitifs : des valeurs isolées de taille fixe

En Java, toute variable a un **type** que tu écris toi-même, avant le nom, et qui **ne change jamais**. `int codigo = 200;` déclare une variable appelée `codigo` qui ne peut garder que des entiers de cette taille. Si plus tard tu essaies d'y mettre un texte, le compilateur refuse. Tu t'en souviendras si tu viens de Go ou de Rust ; si tu viens de Python ou de JavaScript, c'est le plus grand changement : les erreurs de type apparaissent **avant** l'exécution, pas pendant.

Les types de Java se divisent en deux familles. La première, ce sont les **primitifs** : huit types, intégrés au langage, qui gardent la valeur directement, sans objet entre les deux. Dans le `revisor`, tu n'en as besoin que de quatre, mais il vaut mieux voir les huit :

| Type | Ce qu'il garde | Taille | Plage ou exemple |
|---|---|---|---|
| `byte` | entier très petit | 8 bits | de −128 à 127 |
| `short` | petit entier | 16 bits | de −32 768 à 32 767 |
| `int` | entier (celui d'usage courant) | 32 bits | de −2 147 483 648 à 2 147 483 647 |
| `long` | grand entier | 64 bits | jusqu'à environ 9.2 × 10¹⁸ |
| `float` | décimal en précision simple | 32 bits | rarement utilisé |
| `double` | décimal en double précision | 64 bits | le décimal d'usage courant |
| `boolean` | vrai ou faux | non fixée | `true` ou `false` |
| `char` | une unité de texte UTF-16 | 16 bits | `'c'`, entre apostrophes |

Les plages et les tailles ne sont décidées ni par ton ordinateur ni par ton système d'exploitation : elles sont fixées par le [langage](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2), et c'est pourquoi un `int` a la même taille partout. Les noms s'écrivent en minuscules ; cela t'avertit que ce ne sont pas des classes.

Regarde le programme `fig02_01.java`. Il déclare une donnée de chacun des quatre types qu'utilisera le `revisor` (plus un `char`, pour compléter la vue) et fait un peu d'arithmétique. Une remarque sur la syntaxe : le `+` entre un texte et n'importe quoi d'autre **convertit cette chose en texte et la colle**, ce qui est la façon dont se compose chaque ligne de sortie. Et `IO.println` est celle que tu as vue dans la Leçon 1 ; sa sœur `IO.print` fait la même chose mais sans saut de ligne à la fin.

```java
// fig02_01.java
void main() {
  int codigo = 200;
  long tiempoMs = 2300L;
  double segundos = tiempoMs / 1000.0;
  boolean lento = tiempoMs >= 1000;
  char inicial = 'c';

  IO.println("código:    " + codigo);
  IO.println("tiempo:    " + tiempoMs);
  IO.println("segundos:  " + segundos);
  IO.println("lento:     " + lento);
  IO.println("inicial:   " + inicial);

  IO.println("7 / 2      = " + 7 / 2);
  IO.println("7 / 2.0    = " + 7 / 2.0);
  IO.println("2300 / 1000 = " + 2300 / 1000);
  IO.println("0.1 + 0.2  = " + (0.1 + 0.2));
}
```

```text
$ java fig02_01.java
código:    200
tiempo:    2300
segundos:  2.3
lento:     true
inicial:   c
7 / 2      = 3
7 / 2.0    = 3.5
2300 / 1000 = 2
0.1 + 0.2  = 0.30000000000000004
```

Il y a trois choses dans cette sortie qui méritent que tu t'arrêtes.

**La division d'entiers écarte la partie décimale.** `7 / 2` donne `3`, pas `3.5`, parce que les deux opérandes sont des `int` et que le résultat est un `int`. Pour obtenir `3.5`, il suffit que l'un des deux soit un `double` : `7 / 2.0`. Il en va de même pour `2300 / 1000`, qui donne `2`, pas `2.3`. Cela surprend qui vient de Python 3 (où `/` donne toujours un décimal) ou de JavaScript (où tout est décimal), et pas qui vient de Go ou de Rust. C'est l'erreur silencieuse la plus courante dans les calculs de temps : si tu calcules des secondes comme `tiempoMs / 1000`, tu obtiens des secondes **entières**, et 999 ms font 0 s.

**La variable `tiempoMs` est un `long` et sa valeur se termine par `L`.** `2300L` est un littéral de type `long` ; sans le `L`, `2300` serait un `int`. Pour de petits nombres, la différence ne se voit pas, parce que Java convertit un `int` en `long` tout seul (c'est une conversion qui ne perd rien). Avec de grands nombres, le `L` est obligatoire, comme tu le verras dans un instant.

**`0.1 + 0.2` donne `0.30000000000000004`.** Ce n'est pas un défaut de Java : c'est la façon dont n'importe quel ordinateur garde les décimaux binaires (la norme IEEE 754), et cela t'arrive pareil en Python, JavaScript, Go et Rust. Ce qui compte, c'est la conséquence : **ne compare jamais des décimaux avec `==`**, et ne garde jamais d'argent dans un `double`. Le `revisor` garde ses temps en millisecondes **entières** pour cette raison : c'est un nombre qui n'a pas besoin de décimales.

Maintenant, le piège qui est bien propre à Java et aux langages qui partagent son modèle d'entiers de taille fixe. Que se passe-t-il quand un nombre dépasse son maximum ? Regarde `fig02_02.java` :

```java
// fig02_02.java
void main() {
  int maximo = Integer.MAX_VALUE;
  IO.println("máximo de un int: " + maximo);
  IO.println("máximo + 1:       " + (maximo + 1));

  long mayor = Long.MAX_VALUE;
  IO.println("máximo de un long: " + mayor);

  int milisegundosEnUnDia = 24 * 60 * 60 * 1000;
  IO.println("ms en un día:      " + milisegundosEnUnDia);
  int milisegundosEnUnMes = 30 * 24 * 60 * 60 * 1000;
  IO.println("ms en 30 días (int):  " + milisegundosEnUnMes);
  long enUnMes = 30L * 24 * 60 * 60 * 1000;
  IO.println("ms en 30 días (long): " + enUnMes);
}
```

```text
$ java fig02_02.java
máximo de un int: 2147483647
máximo + 1:       -2147483648
máximo de un long: 9223372036854775807
ms en un día:      86400000
ms en 30 días (int):  -1702967296
ms en 30 días (long): 2592000000
```

Un `int` qui atteint son maximum et en reçoit un de plus **fait le tour** et devient le minimum, sans aucun avertissement : pas d'erreur, pas d'exception, seulement un nombre négatif là où tu en attendais un énorme. Dans le programme, trente jours en millisecondes ne tiennent pas dans un `int` (le résultat correct est 2 592 000 000, qui est supérieur au maximum de 2 147 483 647), et la valeur qui sort est `-1702967296`. La correction consiste à faire le calcul en `long` : il suffit d'écrire un `L` dans **un** des nombres, `30L * 24 * 60 * 60 * 1000`, et tout le calcul se fait en `long`. Remarque la différence entre les deux dernières lignes : c'est exactement une lettre.

Un détail de lecture de cette figure : `Integer.MAX_VALUE` et `Long.MAX_VALUE` sont des constantes que fournissent les classes `Integer` et `Long`, dont nous parlons tout de suite. Et la première ligne du `main` de chaque figure de cette leçon est celle que tu connais déjà : `void main()`, le fichier compact de la Leçon 1.

#### 2.1.2 Références, classes enveloppes et `null`

La seconde famille de types, ce sont les types **référence** : tout ce qui n'est pas l'un des huit primitifs. `String` est un type référence. `Integer`, `Long`, `URI` ou toute classe que tu écris le sont aussi. La différence avec un primitif est profonde, et se comprend avec une image. Une variable primitive est **une boîte avec la valeur dedans** : `int codigo = 200` est une boîte qui dit 200. Une variable référence est **une boîte avec une adresse dedans** : l'adresse de l'endroit où se trouve l'objet. Deux variables référence peuvent avoir la même adresse (et alors c'est le même objet), ou des adresses différentes qui pointent vers des objets qui se ressemblent. C'est cette distinction qu'il faudra comprendre en 2.2.2.

Et une variable référence peut avoir, à la place d'une adresse, **aucune**. Cet « aucune » s'écrit `null`. Les primitifs ne peuvent pas être `null` : un `int` vaut toujours un nombre. Cette différence compte pour le `revisor`, parce que `reportes` n'a pas répondu et que son code HTTP n'est ni « zéro » ni « moins un » : il **n'existe pas**. Avec un `int`, il n'y a pas de façon honnête de dire cela. Avec une référence, si.

C'est pourquoi Java fournit une **classe enveloppe** (*wrapper*) pour chaque primitif : `Integer` pour `int`, `Long` pour `long`, `Double` pour `double`, `Boolean` pour `boolean`, `Character` pour `char`. Une classe enveloppe est un objet dont l'unique travail est de contenir une valeur primitive, et comme c'est une référence, **elle peut être `null`**. Java convertit automatiquement entre le primitif et son enveloppe, dans les deux sens ; on appelle cela *autoboxing* quand on met la valeur dans la boîte (de `int` à `Integer`) et *unboxing* quand on la sort. Regarde `fig02_03.java`, qui rassemble ce que nous venons de dire :

```java
// fig02_03.java
void main() {
  int codigo = 200;
  Integer codigoCaja = 200;
  Integer sinRespuesta = null;

  IO.println("primitivo:  " + codigo);
  IO.println("envoltorio: " + codigoCaja);
  IO.println("nulo:       " + sinRespuesta);
  IO.println("¿iguales?   " + codigoCaja.equals(codigo));

  Integer a = 127;
  Integer b = 127;
  IO.println("127 == 127:   " + (a == b));
  Integer c = 2300;
  Integer d = 2300;
  IO.println("2300 == 2300: " + (c == d));
  IO.println("2300 equals:  " + c.equals(d));
}
```

```text
$ java fig02_03.java
primitivo:  200
envoltorio: 200
nulo:       null
¿iguales?   true
127 == 127:   true
2300 == 2300: false
2300 equals:  true
```

Trois lignes méritent un commentaire. `Integer sinRespuesta = null;` est une déclaration parfaitement valide, et à l'impression elle sort `null`. `codigoCaja.equals(codigo)` appelle la méthode `equals` de l'enveloppe avec un `int`, et Java le met dans une boîte pour toi afin de le comparer : cela donne `true`. Et les deux comparaisons avec `==` sont la **surprise de cette section** : `127 == 127` donne `true` et `2300 == 2300` donne `false`, alors que sur les deux lignes on compare deux enveloppes de même valeur. C'est parce que `==` entre deux références demande « est-ce le même objet ? », et non « valent-ils la même chose ? ». Java réutilise les objets `Integer` de −128 à 127 (c'est une garantie de la [spécification](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.1.7)), donc `127` et `127` sont le même objet et `2300` et `2300` sont deux objets distincts. Avec `==` entre enveloppes, ton programme fonctionne avec les petits codes HTTP et échoue avec les grands temps, ce qui est le pire type d'erreur : celui qui dépend de la donnée.

La règle pratique est courte. **Entre primitifs, `==` compare des valeurs. Entre références, il compare l'identité, et pour comparer des valeurs on utilise `equals`.** Et une mise en garde propre aux enveloppes : `Long.equals` compare aussi le type. Un `Long` valant 142 n'est jamais égal à un `Integer` valant 142 ; `equals` renvoie `false` si tu lui passes l'autre type. Quand tu compares un `Long` avec un littéral, écris le `L`.

Dans le `revisor`, tu vas utiliser des enveloppes exactement pour ce à quoi elles servent : `Integer codigoDe(String nombre)` renvoie le code HTTP du service, ou `null` si le service n'a pas répondu ; `Long tiempoDe(String nombre)` fait la même chose avec le temps. La Leçon 3 remplacera cette astuce par quelque chose de mieux, mais pour comprendre pourquoi c'est mieux, il faut l'avoir subie.

#### 2.1.3 Les types sont vérifiés avant l'exécution

Comme chaque variable a son type fixe, le compilateur vérifie que ce que tu lui assignes **tient**. Il va te gronder avec un message qu'il vaut mieux reconnaître, parce que tu le verras souvent. Regarde `fig02_04.java`, où un `long` essaie de se mettre dans un `int` :

```java
// fig02_04.java
void main() {
  long tiempoMs = 2300L;
  int tiempoEntero = tiempoMs;
  IO.println(tiempoEntero);
}
```

```text
$ java fig02_04.java
fig02_04.java:4: error: incompatible types: possible lossy conversion from long to int
  int tiempoEntero = tiempoMs;
                     ^
1 error
error: compilation failed
```

Lis-le avec ce que tu as appris dans la Leçon 1. D'abord le fichier et la ligne (`fig02_04.java:4`), puis la classe d'erreur (`incompatible types`), ensuite le détail (« une conversion qui pourrait perdre de l'information, de `long` à `int` »), et le `^` qui pointe l'endroit. Le lanceur termine par `error: compilation failed`.

Le message dit quelque chose d'important avec le mot **possible**. Java ne sait pas si la valeur tient : il voit le type, pas le contenu. Un `long` *pourrait* valoir 3 000 000 000, et un `int` ne le supporte pas, donc le compilateur t'oblige à décider. Tu as trois issues, et la bonne dépend du cas. Changer le type de destination en `long` est presque toujours le mieux. Écrire une **conversion explicite** (*cast*) comme `(int) tiempoMs` dit au compilateur « je sais ce que je fais », et c'est dangereux : il n'y a aucune protection, et `(int) 3_000_000_000L` donne `-1294967296`, le même dépassement silencieux de 2.1.1. La troisième est de ne pas mélanger les types dès le départ. La conversion explicite s'utilise, avec beaucoup de précaution, quand tu veux vraiment tronquer, par exemple `(int) 2.9` donne `2`.

Dans le sens inverse, il n'y a pas de plainte, mais les deux conversions ne sont pas égales. Un `int` entre dans un `long` **exactement** : toute valeur d'`int` existe dans `long`. Un `long` entre aussi dans un `double` sans demander la permission, parce que l'ordre de grandeur ne se perd jamais, mais il **peut être arrondi** : un `double` garde environ 15 ou 16 chiffres significatifs, et un grand `long` en a jusqu'à 19, donc les derniers sont approchés. Pour des millisecondes comme celles du `revisor`, rien de grave, et c'est pourquoi `double segundos = tiempoMs / 1000.0` de `fig02_01.java` a compilé sans cérémonie ; pour un identifiant numérique long, cela compterait.

#### 2.1.4 `var` : laisser le compilateur écrire le type

Écrire le type de chaque variable donne de la sécurité mais devient répétitif quand la valeur du côté droit dit déjà tout. C'est pour cela que `var` existe. Regarde `fig02_05.java` :

```java
// fig02_05.java
void main() {
  var nombre = "catalogo";
  var codigo = 200;
  var tiempoMs = 2300L;
  var segundos = tiempoMs / 1000.0;

  IO.println(nombre + " " + codigo + " " + tiempoMs + " " + segundos);
}
```

```text
$ java fig02_05.java
catalogo 200 2300 2.3
```

Chaque `var` est une variable de type fixe, comme si tu l'avais écrit : `nombre` est un `String`, `codigo` est un `int`, `tiempoMs` est un `long` (à cause du `L`) et `segundos` est un `double`. **`var` ne rend pas Java dynamique** : le compilateur déduit le type **une seule fois**, à partir de la valeur avec laquelle tu initialises, et dès lors la variable ne peut plus le changer. Si tu essaies ensuite `nombre = 5;`, le compilateur se plaint comme avant. La [proposition qui l'a introduit](https://openjdk.org/jeps/286) (JEP 286, Java 10) le limite aux variables locales avec initialiseur : il ne vaut ni pour les champs, ni pour les paramètres, ni pour les types de retour.

Sans valeur dont déduire, pas de type. Regarde ce qui se passe avec `fig02_06.java` :

```java
// fig02_06.java
void main() {
  var respuesta = null;
  IO.println(respuesta);
}
```

```text
$ java fig02_06.java
fig02_06.java:3: error: cannot infer type for local variable respuesta
  var respuesta = null;
      ^
  (variable initializer is 'null')
1 error
error: compilation failed
```

`null` tout seul ne dit pas de quel type il est, et le compilateur refuse de deviner. C'est le même problème qu'avec les enveloppes, vu d'un autre côté : une valeur « sans rien » a besoin d'un type qui lui donne un sens.

**Quand utiliser `var`.** Dans ce cours, nous l'utilisons quand le type saute aux yeux sur la même ligne (`var nombre = "catalogo";`) et nous l'évitons quand il cache de l'information (`var resultado = revisar(servicio);` ne te dit pas ce qu'est `resultado` sans aller lire `revisar`). C'est un critère de lecture, pas une règle du langage : le code se lit bien plus souvent qu'il ne s'écrit.

### 2.2 Chaînes

#### 2.2.1 `String` est un objet, et il est immuable

`String` est une classe de la bibliothèque standard, donc chaque texte est un objet avec des méthodes : elles s'écrivent derrière un point, comme en Python ou en JavaScript. Les plus utilisées sont `length()` (combien de caractères), `strip()` (retire les espaces aux extrémités), `isBlank()` (est-il vide ou ne contient-il que des espaces ?), `toUpperCase()`, `contains(otro)`, `startsWith(prefijo)` et `substring(desde, hasta)` (un morceau ; le second nombre n'est pas inclus). Vois comment elles s'utilisent dans `fig02_07.java`, et fais surtout attention aux deux premières lignes de sortie :

```java
// fig02_07.java
void main() {
  String linea = "  catalogo  ";
  linea.strip();
  IO.println("[" + linea + "]");

  linea = linea.strip();
  IO.println("[" + linea + "]");

  IO.println("longitud:    " + linea.length());
  IO.println("mayúsculas:  " + linea.toUpperCase());
  IO.println("¿contiene 'log'? " + linea.contains("log"));
  IO.println("¿empieza con 'cat'? " + linea.startsWith("cat"));
  IO.println("recorte:     " + linea.substring(0, 3));
  IO.println("¿vacía?      " + "   ".isBlank());
}
```

```text
$ java fig02_07.java
[  catalogo  ]
[catalogo]
longitud:    8
mayúsculas:  CATALOGO
¿contiene 'log'? true
¿empieza con 'cat'? true
recorte:     cat
¿vacía?      true
```

**La première ligne de sortie est l'erreur.** Nous avons écrit `linea.strip();` et le texte a gardé ses espaces : `[  catalogo  ]`. Ni avertissement ni erreur. C'est **la valeur de retour ignorée**, l'une des trois erreurs qui mettent le plus de temps à être corrigées d'après l'étude que j'ai mentionnée. La cause est que les `String` de Java sont **immuables** : une fois créé, un texte ne change pas. La [documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html) le dit ainsi : les chaînes sont constantes et leurs valeurs ne changent pas après leur création. Toutes les méthodes qui semblent « modifier » (`strip`, `toUpperCase`, `substring`...) **renvoient en réalité un nouveau texte** et laissent l'original intact. Si tu ne le gardes pas, tu l'as jeté.

La bonne ligne est celle du dessous : `linea = linea.strip();`, qui réassigne la **variable** pour qu'elle pointe vers le nouveau texte. Remarque ce qui a changé : pas l'objet, mais ce vers quoi pointe la variable. C'est le modèle des boîtes avec adresses de 2.1.2.

Pourquoi l'a-t-on conçu ainsi ? Parce qu'un texte immuable peut être partagé sans crainte : si cent parties du programme gardent le même `String`, aucune ne peut l'abîmer pour les autres. C'est une propriété que tu connais déjà de Python (`str`) et de JavaScript (les textes primitifs), et que tu reverras en Java avec les *records* de la Leçon 3. Le coût est que le programmeur doit se souvenir de la règle.

Une question naturelle : si chaque « modification » crée un nouveau texte, n'est-ce pas hors de prix de composer un long texte en collant des morceaux ? Dans une boucle de milliers de tours, si, et il existe pour cela une classe à part, `StringBuilder`. Tu n'en as pas besoin dans ce cours : le `revisor` compose des lignes courtes, et `String.format` (2.2.3) fait déjà le travail.

#### 2.2.2 `==` contre `equals`

C'est l'erreur la plus importante de la leçon. Regarde `fig02_08.java`, qui compose le texte `inventario` de deux façons :

```java
// fig02_08.java
void main() {
  String prefijo = "inven";
  String armado = prefijo + "tario";
  String literal = "inventario";
  String constante = "inven" + "tario";

  IO.println("armado == literal:       " + (armado == literal));
  IO.println("armado.equals(literal):  " + armado.equals(literal));
  IO.println("constante == literal:    " + (constante == literal));
  IO.println("mayúsculas distintas:    " + "INVENTARIO".equals(literal));
  IO.println("ignorando mayúsculas:    " + "INVENTARIO".equalsIgnoreCase(literal));
}
```

```text
$ java fig02_08.java
armado == literal:       false
armado.equals(literal):  true
constante == literal:    true
mayúsculas distintas:    false
ignorando mayúsculas:    true
```

Les trois premières lignes racontent une histoire qui se lit mieux dans l'ordre.

`armado == literal` donne **`false`**, bien que les deux textes contiennent `inventario`. `armado` a été construit **à l'exécution**, en collant la valeur de la variable `prefijo` avec `"tario"`, c'est donc un nouvel objet à une nouvelle adresse. `literal` est un objet que le compilateur a créé une fois, et `==` demande si les deux variables pointent vers le même. Ce n'est pas le cas. `armado.equals(literal)` donne `true`, parce que `equals` compare le **contenu**, caractère par caractère.

La troisième ligne est le piège dans le piège. `constante == literal` donne **`true`**, bien que `constante` ait elle aussi été écrite avec un `+`. La raison : `"inven" + "tario"` assemble deux littéraux, et le compilateur connaît le résultat avant l'exécution, donc il le calcule lui-même et le traite comme le littéral `"inventario"` (la [spécification](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.29) l'appelle « expression constante »). Les textes littéraux de même contenu sont **le même objet** dans tout le programme. C'est pourquoi un `==` entre textes fonctionne souvent dans un petit test jouet et échoue dès que le texte arrive de **l'extérieur** : d'un fichier, d'un clavier, d'un réseau, d'un `+` avec une variable. C'est le genre d'erreur qui passe tous les tests que tu écris et échoue en production.

Les deux dernières lignes montrent ce qu'on écrit à la place. `equals` distingue les majuscules, et `equalsIgnoreCase` non. Et une habitude qui vaut de l'or : **mets le littéral en premier**. `"INVENTARIO".equals(literal)` ne peut pas lever de `NullPointerException`, parce que le littéral n'est jamais nul ; en revanche, `variable.equals("inventario")` explose si `variable` est `null`. Avec le littéral à gauche, si la variable est nulle, la réponse est simplement `false`.

**La règle de cette leçon, la plus importante :** entre références (textes, enveloppes, n'importe quel objet), `==` demande si c'est le même objet ; `equals` demande s'ils valent la même chose. Presque toujours, tu veux le second. Utilise `==` entre primitifs, et entre `enum`s (Leçon 3), où c'est bien la bonne chose.

Pourquoi Java ne fait-il pas que `==` compare le contenu des textes, comme Python ? Parce que Java ne laisse pas le programmeur changer le sens d'un opérateur, et que `==` signifie la même chose pour tous les types référence. C'est une décision de conception qui donne du fil à retordre depuis des décennies, et qui ne va pas changer.

#### 2.2.3 Mettre en forme : `String.format` et `Locale.ROOT`

Chaque ligne du tableau du `revisor` a des colonnes alignées : le nom occupe 12 espaces, l'état 10, et ainsi de suite. Composer cela avec des `+` et des espaces à la main est un cauchemar, et c'est pour cela que Java fournit `String.format` : il reçoit un **modèle** avec des marques et les valeurs qui les remplissent. Les marques dont tu as besoin aujourd'hui :

- `%s` met un texte (ou n'importe quelle valeur, convertie en texte).
- `%d` met un entier.
- `%.1f` met un décimal avec une décimale après le point.
- `%-12s` met un texte **occupant 12 espaces, aligné à gauche** (sans le tiret, à droite). `%6s` en occupe 6 et aligne à droite.

Et il y a un problème que tu ne verras que lorsque ton programme s'exécutera sur l'ordinateur de quelqu'un d'autre : les décimaux s'écrivent différemment selon la langue. Au Mexique, le séparateur décimal est le point (`2.3`), en Espagne c'est la virgule (`2,3`). Si tu demandes à `String.format` de formater un décimal sans lui dire la langue, **il utilise celle de la machine où il s'exécute**. Ton programme sortirait avec des points sur ton portable et avec des virgules sur celui d'un collègue. Pour que cela n'arrive pas, on lui passe comme premier argument un `Locale`, l'objet qui représente une langue et une région. Regarde `fig02_09.java` :

```java
// fig02_09.java
void main() {
  double segundos = 2.3;
  Locale mexico = Locale.of("es", "MX");
  Locale espana = Locale.of("es", "ES");

  IO.println(String.format(Locale.ROOT, "%.1fs", segundos));
  IO.println(String.format(mexico, "%.1fs", segundos));
  IO.println(String.format(espana, "%.1fs", segundos));
  IO.println(String.format(Locale.ROOT, "[%-12s][%6s][%d]", "catalogo", "142ms", 200));
  IO.println("%s tardó %d ms".formatted("pagos", 87));
}
```

```text
$ java fig02_09.java
2.3s
2.3s
2,3s
[catalogo    ][ 142ms][200]
pagos tardó 87 ms
```

`Locale.of("es", "MX")` construit la langue espagnole du Mexique et `Locale.of("es", "ES")` celle de l'Espagne. Avec celle de l'Espagne, le même `%.1f` donne **`2,3s`**. `Locale.ROOT` est la « langue neutre », qui utilise le format le plus stable (point décimal, sans séparateurs de milliers), et **c'est celle qu'on utilise dans le cours chaque fois qu'on formate un nombre à décimales**. La raison n'est pas nationaliste : c'est que le tableau du `revisor` doit sortir identique sur n'importe quelle machine, et un programme dont la sortie dépend de la configuration régionale est un programme qu'un test ne peut pas vérifier.

La quatrième ligne de sortie est le modèle des lignes du tableau en miniature (`%-12s` pour le nom, `%6s` pour le temps et `%d` pour le code), avec les crochets pour que tu voies combien d'espace occupe chaque colonne. La dernière utilise `formatted`, une version du même mécanisme qui s'appelle sur le texte lui-même : `"...".formatted(valores)` équivaut à `String.format("...", valores)` (sans le `Locale` : utilise-la seulement avec des textes et des entiers).

#### 2.2.4 Blocs de texte

Parfois il te faut un texte de plusieurs lignes : un modèle, un morceau de configuration, un JSON d'exemple. L'écrire avec des `"` et des `\n` à chaque ligne est horrible. Java a les **blocs de texte**, délimités par trois guillemets doubles. Ils ont été introduits comme fonctionnalité définitive dans Java 15 ([JEP 378](https://openjdk.org/jeps/378)). Regarde `fig02_10.java` :

```java
// fig02_10.java
void main() {
  String encabezado = """
      # nombre      url
      catalogo      https://catalogo.example
      pagos         https://pagos.example
      """;
  IO.print(encabezado);
  IO.println("renglones con salto final: " + encabezado.endsWith("\n"));

  String cita = """
      El servicio "%s" tardó %d ms.\
       Revísalo.""".formatted("inventario", 2300);
  IO.println(cita);
}
```

```text
$ java fig02_10.java
# nombre      url
catalogo      https://catalogo.example
pagos         https://pagos.example
renglones con salto final: true
El servicio "inventario" tardó 2300 ms. Revísalo.
```

La règle qui le rend utile est la façon dont il traite l'indentation. Le texte du bloc commence à la ligne située sous les trois guillemets d'ouverture, et Java **retire l'indentation que toutes les lignes partagent** (le compilateur la calcule à partir de la ligne la moins indentée, en comptant aussi la ligne de fermeture `"""`). C'est pourquoi le texte imprimé commence à `# nombre` sans les six espaces avec lesquels nous avons écrit le bloc dans le code. Si tu déplaces la fermeture `"""` vers la gauche, tu conserves plus d'indentation ; si tu la laisses à la fin de la dernière ligne de texte, il n'y a pas de saut de ligne final.

Deux détails de plus. Dans un bloc, tu peux écrire des guillemets doubles sans les échapper (`"%s"` dans le second bloc). Et une barre oblique inverse `\` en fin de ligne **n'insère pas le saut** : elle joint la ligne avec la suivante. Dans le second bloc, le `\` fait que la phrase sort d'un seul tenant, avec une espace qui vient de l'indentation supplémentaire de la ligne suivante (sept espaces, un de plus que les six communs). Et remarque l'usage de `.formatted(...)` juste après la fermeture : un bloc de texte est un `String` comme un autre. La ligne avec `endsWith("\n")` vérifie que le premier bloc se termine par un saut de ligne, qui dans un texte normal s'écrit `\n` ; c'est pourquoi la sortie de `IO.print` laisse les trois lignes à la suite et le `IO.println` suivant commence sur une ligne propre.

Ainsi se termine la première séance. Avant de continuer, vérifie que tu peux répondre, sans regarder : quelle est la différence entre `int` et `Integer` ? Qu'imprime `armado == literal` dans `fig02_08.java` et pourquoi ? Que se passe-t-il si tu écris `linea.strip();` et rien de plus ? Si l'une des trois te coûte, relis la section correspondante avant la seconde séance.


### 2.3 Contrôle de flux

#### 2.3.1 `if`, conditions et l'opérateur `? :`

Le `if` de Java est celui que tu connais déjà, avec deux particularités. La première : **la condition doit être un `boolean`**, et rien d'autre. En Python ou en JavaScript, un nombre, un texte ou une liste peuvent faire office de condition (on les dit « vrais » ou « faux » selon leur contenu) ; en Java, `if (codigo)` avec un `int` ne compile même pas :

```text
Prueba.java:3: error: incompatible types: int cannot be converted to boolean
  if (codigo) {
      ^
```

C'est un avantage déguisé en gêne : cela t'oblige à écrire ce que tu veux vraiment demander (`codigo >= 400`, `texto != null`, `!texto.isBlank()`), et finies les erreurs du type « le zéro compte aussi comme faux ». La seconde particularité relève du style : **toujours des accolades**, même si le `if` n'a qu'une seule ligne. C'est ce que demande le guide de style de Google que suit tout le code du cours (deux espaces d'indentation, lignes d'au plus 100 colonnes), et que Checkstyle vérifiera dans la Leçon 10 ; si tu les écris dès aujourd'hui, tu n'auras pas à les corriger ensuite. Un `if` sans accolades est la recette d'une erreur classique : quelqu'un ajoute une deuxième ligne dessous, avec la bonne indentation, et la deuxième ligne ne fait pas partie du `if`.

Regarde `fig02_11.java`. Il rassemble un `if` avec `else if`, une variable booléenne, et l'**opérateur ternaire**, qui est un `if` sous forme d'expression : `condicion ? valorSiVerdadero : valorSiFalso`.

```java
// fig02_11.java
void main() {
  int codigo = 503;
  long tiempoMs = 95;

  if (codigo >= 400) {
    IO.println("CAIDO: el servicio contestó " + codigo);
  } else if (tiempoMs >= 1000) {
    IO.println("LENTO");
  } else {
    IO.println("ARRIBA");
  }

  boolean respondio = codigo != 0 && tiempoMs > 0;
  IO.println("¿respondió? " + respondio);
  String mensaje = respondio ? "con respuesta" : "sin respuesta";
  IO.println(mensaje);
}
```

```text
$ java fig02_11.java
CAIDO: el servicio contestó 503
¿respondió? true
con respuesta
```

La condition `codigo >= 400` est la première règle du `revisor` : un service qui répond avec un code d'erreur (400 ou plus) est tombé, même s'il a répondu. Remarque que le `else if` n'est évalué que si le premier était faux, et que les branches sont exclusives. Le ternaire sert à choisir une valeur, pas à exécuter des actions ; quand l'expression de chaque côté devient longue, un `if` normal se lit mieux.

#### 2.3.2 `&&` contre `&`

Voici la deuxième erreur de la liste de celles qui sont lentes à corriger. Java a deux opérateurs pour « et » : `&&` et `&` (et deux pour « ou » : `||` et `|`). Les deux font la même chose avec des booléens, avec une différence qui compte : `&&` **s'arrête dès qu'il connaît la réponse**. Si le côté gauche est faux, le résultat est faux quel que soit le droit, et le droit **n'est même pas évalué**. On appelle cela l'évaluation en court-circuit. `&` évalue **toujours les deux côtés**. (Avec des nombres entiers, `&` est autre chose : une opération sur les bits. Dans ce cours, nous ne l'utilisons pas, mais cela explique pourquoi il existe.)

Regarde le programme `fig02_12.java`. Il veut demander : « le détail existe-t-il et en plus est-il vide ? » :

```java
// fig02_12.java
void main() {
  String detalle = null;
  if (detalle != null & detalle.isBlank()) {
    IO.println("detalle vacío");
  }
  IO.println("fin");
}
```

```text
$ java fig02_12.java
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.isBlank()" because "<local1>" is null
	at fig02_12.main(fig02_12.java:4)
```

`detalle` vaut `null`, donc `detalle != null` est faux... mais le `&` a évalué **malgré tout** le côté droit, `detalle.isBlank()`, et appeler une méthode sur `null` explose. C'est la vérification que tu as mise pour éviter le problème, qui cause le problème. Avec `&&`, le côté droit n'est pas touché quand le gauche échoue. C'est `fig02_13.java` :

```java
// fig02_13.java
void main() {
  String detalle = null;
  if (detalle != null && detalle.isBlank()) {
    IO.println("detalle vacío");
  }
  IO.println("fin sin errores");
}
```

```text
$ java fig02_13.java
fin sin errores
```

Ce schéma, **vérifier que ce n'est pas nul et, seulement si ça ne l'est pas, l'utiliser**, est la raison d'être de `&&` dans le `revisor`. Tu l'utiliseras dans `clasificar`, et tu l'écriras des centaines de fois dans ta vie avec Java. La règle est : dans une condition, `&&` et `||` ; on réserve `&` et `|` aux opérations sur les bits. C'est l'une de ces fautes que le compilateur ne signale pas, parce que les deux sont valides ; c'est ton attention qui les attrape.

#### 2.3.3 `switch` comme expression

Le `switch` classique de Java, hérité de C, a un défaut célèbre : **si tu oublies le `break`, l'exécution continue dans le cas du dessous**. Regarde `fig02_14.java`, qui écrit le même sélecteur de deux façons :

```java
// fig02_14.java
void main() {
  String nombre = "inventario";
  int antiguo = 0;
  switch (nombre) {
    case "catalogo":
      antiguo = 1;
    case "inventario":
      antiguo = 2;
    case "pagos":
      antiguo = 3;
      break;
    default:
      antiguo = -1;
  }
  IO.println("switch con dos puntos y sin break: " + antiguo);

  int moderno = switch (nombre) {
    case "catalogo" -> 1;
    case "inventario" -> 2;
    case "pagos" -> 3;
    default -> -1;
  };
  IO.println("switch con flecha:                " + moderno);
}
```

```text
$ java fig02_14.java
switch con dos puntos y sin break: 3
switch con flecha:                2
```

Avec l'ancienne syntaxe (les deux-points), `inventario` entre dans le cas 2, **ne trouve pas de `break`**, tombe dans le cas 3 et finit par laisser `antiguo` à 3. Avec la nouvelle (la flèche `->`), chaque cas est indépendant : ni `break` ni chute. Il faut reconnaître l'ancienne syntaxe parce qu'elle est dans presque tout le code Java que tu trouveras, mais **dans ce cours on l'écrit toujours avec la flèche**. Les `switch` à flèche sont entrés dans Java à la version 14 ([JEP 361](https://openjdk.org/jeps/361)).

La seconde différence est celle qui change ta façon de penser : un `switch` peut s'employer comme une **expression**, c'est-à-dire comme quelque chose qui **produit une valeur**. (La flèche seule n'en fait pas une expression : on peut aussi l'employer dans un `switch` ordinaire, comme instruction, et son avantage y est qu'il n'y a pas de chute d'un cas au suivant. Ce qui produit une valeur, c'est le `switch` employé là où l'on attend une valeur.) Tu peux l'assigner (`int moderno = switch ... ;`), le renvoyer avec `return switch ...` ou le passer à une méthode. Trois règles le caractérisent. Un cas peut avoir **plusieurs valeurs** séparées par des virgules (`case "catalogo", "inventario", "pagos" -> 200;`). Le résultat de chaque cas est ce qui est à droite de la flèche. Et si tu as besoin de plus d'une instruction, le cas porte un bloc entre accolades et livre sa valeur avec `yield`. Vois `fig02_15.java` :

```java
// fig02_15.java
void main() {
  IO.println(describir(200));
  IO.println(describir(301));
  IO.println(describir(404));
  IO.println(describir(503));
  IO.println(describir(0));
}

String describir(int codigo) {
  int familia = codigo / 100;
  return switch (familia) {
    case 2 -> "correcto";
    case 3 -> "redirección";
    case 4, 5 -> {
      String quien = familia == 4 ? "cliente" : "servidor";
      yield "error del " + quien;
    }
    default -> "sin clasificar";
  };
}
```

```text
$ java fig02_15.java
correcto
redirección
error del cliente
error del servidor
sin clasificar
```

Il y a trois nouveautés dans cette figure. `codigo / 100` met à profit la division entière de 2.1.1 pour obtenir la « famille » du code (503 tombe dans la 5). `case 4, 5 -> { ... yield ...; }` est le bloc avec `yield`, qui est à un cas ce que `return` est à une méthode. Et le `default` attrape le reste. Une limite : le `switch` avec des valeurs compare par **égalité** avec des constantes ; il ne sert pas pour des plages comme « 400 ou plus ». Pour cela, on utilise un `if`, ou, à partir de la Leçon 3, un `switch` avec des motifs et des conditions.

La règle qui rend le `switch` comme expression précieux est qu'il **doit couvrir tous les cas possibles**, parce qu'il doit produire une valeur quelle que soit l'entrée. S'il manque quelque chose, le compilateur le dit. `fig02_16.java` oublie le `default` :

```java
// fig02_16.java
void main() {
  String nombre = "reportes";
  int codigo = switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
  };
  IO.println(codigo);
}
```

```text
$ java fig02_16.java
fig02_16.java:4: error: the switch expression does not cover all possible input values
  int codigo = switch (nombre) {
               ^
1 error
error: compilation failed
```

Le compilateur sait qu'il y a une infinité de textes différents, donc sans `default` il ne peut pas garantir qu'il y aura une valeur. Plus loin, dans la Leçon 3, tu verras qu'avec `enum` et avec les types scellés, le compilateur peut vérifier, sans avoir besoin de `default`, qu'**aucun cas ne manque**, et c'est l'une des meilleures raisons de les préférer.

Un `switch` sur un texte compare avec `equals` (en interne), pas avec `==`, de sorte qu'il ne tombe pas dans le piège de 2.2.2. Et si le texte est `null`, le `switch` lève une `NullPointerException` avant de regarder le moindre cas.

#### 2.3.4 Méthodes et constantes

Les méthodes de Java se déclarent en écrivant, dans l'ordre, le **type qu'elles renvoient**, le **nom**, les **paramètres avec leur type** entre parenthèses et le corps entre accolades. `String marcar(long tiempoMs)` est une méthode qui reçoit un `long` et renvoie un `String`. Si elle ne renvoie rien, le type est `void` (comme dans `void main()`). On renvoie avec `return`, et le compilateur exige que **tous les chemins** de la méthode renvoient quelque chose : si un `if` sans `else` laisse un chemin sans `return`, c'est une erreur de compilation. Dans un fichier compact, les méthodes s'écrivent à la même hauteur que `main`, et `main` les appelle par leur nom.

Deux particularités par rapport à d'autres langages. Java **n'a pas de paramètres avec valeur par défaut** : si tu veux une version courte d'une méthode, tu en écris une autre avec le même nom et moins de paramètres (la Leçon 3 le fera avec les *records*). Et les arguments se passent **par valeur** : la méthode reçoit une copie. Regarde `fig02_17.java`, qui étrenne en plus une constante :

```java
// fig02_17.java
static final int UMBRAL_LENTO_MS = 1000;

void main() {
  long tiempoMs = 2300;
  IO.println(marcar(tiempoMs));
  IO.println("después de la llamada, tiempoMs sigue valiendo " + tiempoMs);
  IO.println(marcar(87));
}

String marcar(long tiempoMs) {
  String marca = tiempoMs >= UMBRAL_LENTO_MS ? "lento" : "rápido";
  tiempoMs = 0;
  return marca;
}
```

```text
$ java fig02_17.java
lento
después de la llamada, tiempoMs sigue valiendo 2300
rápido
```

La méthode `marcar` met son paramètre `tiempoMs` à 0, et au retour, la variable du `main` vaut toujours 2300. Changer le paramètre a changé la copie, pas l'original. (Avec les objets, il se passe la même chose, mais ce qui est copié, c'est l'**adresse** : la méthode peut changer le contenu d'un objet modifiable, mais elle ne peut pas faire pointer la variable de l'appelant vers un autre.)

La ligne du début, `static final int UMBRAL_LENTO_MS = 1000;`, est une **constante**. Chaque mot compte. `final` veut dire « s'assigne une seule fois » ; toute tentative de la réassigner est une erreur de compilation. `static` veut dire « appartient à la classe et non à un objet » ; dans un fichier compact, la « classe » est le fichier entier. Le nom se met en `MAJUSCULES_AVEC_TIRET_BAS` (comme le demande la convention de Java), et porte l'unité : `_MS`. L'écrire à un seul endroit et l'utiliser par son nom, au lieu d'écrire `1000` dans chaque comparaison, a un motif pratique : quand le seuil changera, il changera à un seul endroit ; et un motif de communication : `tiempoMs >= UMBRAL_LENTO_MS` se lit, et `tiempoMs >= 1000` oblige à se rappeler ce qu'est mille.

### 2.4 `null` et les messages de `NullPointerException`

#### 2.4.1 Ce qu'est `null` et quand il explose

Nous avons dit en 2.1.2 qu'une référence peut ne pointer vers rien, et que cela s'écrit `null`. Le problème n'est pas qu'il existe, mais ce qui se passe quand tu **utilises** un `null` comme si c'était un objet : lui appeler une méthode, lire l'un de ses champs, ou, dans le cas d'une enveloppe, en extraire la valeur. Dans chacun de ces cas, le programme s'arrête avec une exception appelée `NullPointerException` (on dit « NPE »). Tony Hoare, qui a inventé le concept en 1965, l'a appelé ensuite [« mon erreur à un milliard de dollars »](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/). Java ne l'a pas supprimé, et la plus grande partie de ce cours est l'histoire de la façon de vivre sans en dépendre.

Regarde `fig02_18.java`. La méthode `codigoDe` renvoie `null` pour un service inconnu, et le `main` essaie de lui ajouter 1 :

```java
// fig02_18.java
void main() {
  Integer codigo = codigoDe("reportes");
  IO.println("código: " + codigo);
  int sumado = codigo + 1;
  IO.println("sumado: " + sumado);
}

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}
```

```text
$ java fig02_18.java
código: null
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "<local1>" is null
	at fig02_18.main(fig02_18.java:5)
```

La première ligne sort bien, parce que coller un `null` à un texte avec `+` écrit seulement « null ». La seconde est celle qui explose : `codigo + 1` a besoin du nombre qui est dans l'`Integer`, c'est-à-dire de faire l'*unboxing* de 2.1.2, et il n'y a pas de nombre. C'est le piège des enveloppes : **l'addition semble porter sur deux entiers, mais l'un d'eux peut ne pas exister**. Si `codigo` était un `int`, rien ne pourrait échouer, mais il n'y aurait pas non plus de moyen de dire « sans réponse ».

#### 2.4.2 Lire le message

Depuis plusieurs versions de Java, une `NullPointerException` ne se limite pas à dire « c'est nul » : elle dit **ce que tu essayais de faire** et **ce qui était nul**. C'est la fonction des « messages utiles de NPE », décrite dans le [JEP 358](https://openjdk.org/jeps/358). Celui de `fig02_18.java` dit :

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "<local1>" is null
	at fig02_18.main(fig02_18.java:5)
```

Il se lit en deux moitiés. **Avant `because` : ce que le programme essayait.** « Impossible d'invoquer `Integer.intValue()` », c'est-à-dire extraire la valeur de l'enveloppe. **Après `because` : ce qui était nul.** « La variable `<local1>` ». Et la ligne du dessous, `at fig02_18.main(fig02_18.java:5)`, dit où : la méthode `main`, fichier `fig02_18.java`, ligne 5. Une NPE se corrige en lisant ce message, pas en ouvrant un débogueur.

Maintenant, le détail que le plan du cours promet d'éclaircir. Pourquoi dit-il `<local1>` et non `codigo` ? Parce que **le lanceur `java Archivo.java` compile le fichier en mémoire sans conserver les noms des variables locales**. Ces noms n'existent que dans le code source ; pour que la JVM les connaisse, le compilateur doit les inclure, et tu le lui demandes avec l'option `-g`. Compile-le toi-même, avec `javac -g`, et exécute-le avec `java` :

```text
$ javac -g fig02_18.java
$ java fig02_18
código: null
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "codigo" is null
	at fig02_18.main(fig02_18.java:5)
```

Là, oui : `because "codigo" is null`. C'est le même programme, la même erreur et la même ligne, avec un message beaucoup plus clair. Quand tu tomberas sur un `<local3>` au travail, tu sauras ce que c'est : le numéro de l'emplacement (*slot*) que la JVM a donné à cette variable locale (dans une méthode d'instance, l'emplacement 0 est `this`, et les autres se répartissent dans l'ordre où elles sont déclarées ; un `long` ou un `double` en occupe deux). Et quand tu voudras le nom, compile avec `-g`.

Il y a d'autres formes de message. Si ce qui est nul n'est pas une variable mais le **résultat d'une méthode**, le message le dit. C'est `fig02_19.java` :

```java
// fig02_19.java
void main() {
  String texto = codigoDe("reportes").toString();
  IO.println(texto);
}

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}
```

```text
$ java fig02_19.java
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.toString()" because the return value of "fig02_19.codigoDe(String)" is null
	at fig02_19.main(fig02_19.java:3)
```

« Because the return value of `fig02_19.codigoDe(String)` is null » se traduit : la méthode `codigoDe` a renvoyé nul, et le programme a demandé `toString()` à cette valeur. Il n'est pas nécessaire de deviner laquelle des parties de l'expression `codigoDe("reportes").toString()` était nulle : le message désigne celle qui l'était. Quand une expression comporte plusieurs appels enchaînés (`a.b().c().d()`), c'est ce qui t'épargne une demi-heure.

#### 2.4.3 Trois règles pour vivre avec `null`

Le `null` ne disparaît pas du code réel, mais il y a des façons de l'apprivoiser, et le `revisor` les applique dès aujourd'hui :

1. **Vérifie avant d'utiliser, avec `&&`.** `codigo != null && tiempoMs != null` est la condition du `revisor` pour savoir si un service a répondu. Si la première partie échoue, la seconde n'est pas évaluée.
2. **Mets le littéral à gauche dans `equals`.** `"pagos".equals(nombre)` ne peut pas exploser ; `nombre.equals("pagos")`, si.
3. **Ne renvoie pas `null` quand il existe une meilleure façon de dire « il n'y a pas de donnée ».** Ici nous le faisons parce que nous n'en avons pas encore d'autre, mais dans la Leçon 3, `null` disparaît du `revisor` et un type scellé le remplace, et dans la 4 tu verras `Optional`. Une méthode qui peut renvoyer nul est une méthode qui oblige tous ceux qui l'appellent à se souvenir de vérifier.

### 2.5 L'étape du projet : le `revisor` avec ses fonctions de base

Maintenant, tu rassembles tout. Tu veux que `Revisor.java` imprime le tableau dont est partie la leçon, en calculant chaque cellule avec des fonctions. Les règles sont les quatre habituelles du `revisor` :

1. Si le service n'a pas répondu, il est **CAIDO** (tombé).
2. S'il a répondu avec un code de 400 ou plus, il est **CAIDO** (le détail est le code).
3. S'il a répondu et a mis 1000 ms ou plus, il est **LENTO**.
4. Sinon, il est **ARRIBA** (en ligne).

Les données des quatre services vivent, pour l'instant, dans deux `switch` parallèles : l'un renvoie le code et l'autre le temps. Un service qui n'a pas répondu a `null` dans les deux. Ton `Revisor.java` devient ceci (enregistre-le dans `~/curso-java/revisor/` ; la figure l'appelle `fig02_20.java` pour le distinguer des autres, et son contenu est celui de ton fichier) :

```java
// fig02_20.java
static final int UMBRAL_LENTO_MS = 1000;

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}

Long tiempoDe(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142L;
    case "inventario" -> 2300L;
    case "pagos" -> 87L;
    default -> null;
  };
}

String clasificar(Integer codigo, Long tiempoMs) {
  boolean respondio = codigo != null && tiempoMs != null;
  if (!respondio) {
    return "CAIDO";
  }
  if (codigo >= 400) {
    return "CAIDO";
  }
  if (tiempoMs >= UMBRAL_LENTO_MS) {
    return "LENTO";
  }
  return "ARRIBA";
}

String formatearTiempo(Long tiempoMs) {
  if (tiempoMs == null) {
    return "—";
  }
  if (tiempoMs < UMBRAL_LENTO_MS) {
    return tiempoMs + "ms";
  }
  return String.format(Locale.ROOT, "%.1fs", tiempoMs / 1000.0);
}

String detalle(Integer codigo) {
  if (codigo == null) {
    return "sin conexión";
  }
  return String.valueOf(codigo);
}

String linea(String nombre, String estado, String tiempo, String detalle) {
  return String.format(Locale.ROOT, "%-12s%-10s%6s  %s", nombre, estado, tiempo, detalle);
}

String encabezado() {
  return linea("SERVICIO", "ESTADO", "TIEMPO", "DETALLE");
}

String revisar(String nombre) {
  Integer codigo = codigoDe(nombre);
  Long tiempoMs = tiempoDe(nombre);
  return linea(
      nombre, clasificar(codigo, tiempoMs), formatearTiempo(tiempoMs), detalle(codigo));
}

void main() {
  IO.println(encabezado());
  IO.println(revisar("catalogo"));
  IO.println(revisar("inventario"));
  IO.println(revisar("pagos"));
  IO.println(revisar("reportes"));
}
```

```text
$ java fig02_20.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Parcours le programme avec ce que tu sais, de haut en bas :

- **`UMBRAL_LENTO_MS`** est la constante de 2.3.4.
- **`codigoDe` et `tiempoDe`** sont les deux `switch` à flèche de 2.3.3. Ils renvoient `Integer` et `Long`, pas `int` et `long`, pour pouvoir renvoyer `null` (2.1.2). Les littéraux portent le `L` dans `tiempoDe` parce que le résultat est un `Long`, et un `142` ne serait qu'un `Integer`.
- **`clasificar`** implémente les quatre règles. La première ligne, `codigo != null && tiempoMs != null`, utilise le `&&` de 2.3.2 : sinon, les comparaisons du dessous déballeraient un nul. Les règles s'appliquent dans l'ordre, chacune avec son `return` ; quand l'une est remplie, les autres ne sont pas évaluées.
- **`formatearTiempo`** reçoit un `Long`, qui peut être nul (le tiret « — » est le signal de « pas de donnée »), et sinon décide entre `142ms` et `2.3s`. Remarque qu'il utilise `Locale.ROOT` (2.2.3).
- **`detalle`** renvoie « sin conexión » (« sans connexion ») pour le nul et, sinon, le code sous forme de texte. `String.valueOf(codigo)` convertit un `Integer` en `String`.
- **`linea`** compose une ligne avec le modèle `%-12s%-10s%6s  %s` : 12 de large pour le nom, 10 pour l'état, 6 aligné à droite pour le temps, deux espaces et le détail. `encabezado` réutilise `linea` avec les titres, pour qu'ils aient les mêmes dimensions.
- **`revisar`** réunit tout : il cherche le code et le temps d'un service, et livre sa ligne complète. L'appel à `linea` est coupé en deux lignes parce qu'il dépasserait 100 colonnes ; la suite se met avec quatre espaces de plus.
- **`main`** imprime l'en-tête et les quatre lignes, une par une. Il n'y a pas de boucle parce qu'il n'y a pas de liste : les services ne sont pas encore des données que l'on puisse parcourir.

Et le tableau qui sort est, **caractère par caractère**, celui que nous avions promis au début. Remarque que `reportes` a parcouru tout le chemin du `null` : `codigoDe` et `tiempoDe` lui renvoient nul, `clasificar` l'envoie à `CAIDO` par la règle 1, `formatearTiempo` lui met le tiret et `detalle` écrit « sin conexión ». Chaque pièce a décidé seule quoi faire du vide. Mais cette décision s'est répétée **quatre fois** (une par fonction), et c'est de cela que traite la leçon suivante.

Pour l'exécuter depuis ton dossier de travail :

```text
$ cd ~/curso-java/revisor
$ java Revisor.java
```

## L'erreur que tu vas voir

La leçon a une erreur principale et quatre messages d'appui qu'il vaut mieux savoir lire d'un coup d'œil.

**L'erreur principale : un `==` qui donne `false` quand tu attendais `true`.** C'est celle de `fig02_08.java`. Quand un texte composé à l'exécution est comparé avec `==` à un littéral, Java demande si c'est le même objet, et ce n'est pas le cas :

```text
armado == literal:       false
armado.equals(literal):  true
```

Il n'y a pas de message d'erreur ; c'est pourquoi c'est celle qui coûte le plus. Le signal, c'est qu'**une comparaison qui devrait être vraie est fausse** et que tout le reste semble correct. Si tu vois un `==` entre deux textes ou deux enveloppes dans un `if`, soupçonne-le avant toute autre chose.

**Les quatre messages d'appui :**

- `error: incompatible types: possible lossy conversion from long to int` : tu as assigné un grand type à un plus petit. Change le type de destination, ou fais la conversion explicite en sachant ce que tu peux perdre (`fig02_04.java`).
- `error: cannot infer type for local variable respuesta`, avec `(variable initializer is 'null')` : tu as utilisé `var` sans valeur dont déduire le type (`fig02_06.java`).
- `error: the switch expression does not cover all possible input values` : un `switch` qui produit une valeur et ne couvre pas tous les cas. Ajoute le `default` (ou, avec `enum` et les types scellés, le cas qui manque) (`fig02_16.java`).
- `NullPointerException: Cannot invoke "String.isBlank()" because "<local1>" is null` : tu as utilisé un nul comme si c'était un objet. La partie qui suit `because` dit lequel était nul ; si elle dit `<localN>` au lieu du nom, le fichier a été compilé sans `-g` (`fig02_12.java`, `fig02_18.java`).

Et un effet de fond qui ne produit aucun message : `2,3s` au lieu de `2.3s`. Cela arrive sur une machine avec une configuration régionale d'Espagne quand on formate un décimal sans `Locale.ROOT` (`fig02_09.java`).

## Ce qui se fait de travers

**Comparer des textes ou des enveloppes avec `==`.** Cela fonctionne avec les littéraux et avec les petits nombres, et échoue avec les données qui viennent de l'extérieur. Avec des textes, `equals` (avec le littéral à gauche) ; avec des enveloppes, `equals` ou, s'ils sont primitifs, `==`.

**Appeler une méthode de `String` et ne pas garder le résultat.** `linea.strip();` isolé ne fait rien. Les textes ne changent pas : les méthodes en renvoient un autre.

**Utiliser `&` ou `|` là où vont `&&` ou `||`.** Les deux sont valides, et le premier explose précisément quand la vérification servait.

**Un `int` pour quelque chose qui peut grandir.** Les millisecondes d'un mois ne tiennent pas dans un `int`. Si la donnée est un temps, un compte ou une taille, commence par `long`.

**Un `double` pour de l'argent ou pour comparer avec `==`.** Les décimaux binaires ne sont pas exacts (`0.1 + 0.2`). Les temps du `revisor` sont des entiers en millisecondes justement pour l'esquiver.

**Une conversion explicite `(int)` pour faire taire le compilateur.** Elle fait taire l'avertissement et laisse passer le dépassement sans prévenir. Avant de l'écrire, demande-toi si le type de destination ne devrait pas être plus grand.

**`String.format` avec des décimaux sans `Locale`.** Cela fonctionne sur ta machine et échoue sur celle d'un autre. `Locale.ROOT` toujours.

**Un `switch` avec la syntaxe à deux-points.** Un `break` oublié produit des erreurs qui n'apparaissent pas dans le compilateur. Avec la flèche, toute cette classe d'erreurs disparaît.

**Renvoyer `null` par habitude.** Chaque `null` qui sort d'une méthode oblige tous ceux qui l'appellent à se souvenir de vérifier. Ici nous l'utilisons parce que nous n'avons pas encore d'autre outil, et la Leçon 3 l'apporte.

**`var` là où le type ne se voit pas.** `var r = revisar(s);` oblige le lecteur à aller chercher ce qu'est `r`. Utilise-le quand le type saute aux yeux.

## Exercices

### Exercice 1 — Prédis, puis exécute

Écris dans un fichier le `main` qui imprime ces six expressions, **une par ligne**, et avant de l'exécuter, note sur un papier ce que tu crois que chacune imprimera :

1. `"142" + 1`
2. `1 + 2 + "3"`
3. `"1" + 2 + 3`
4. `-7 / 2`
5. `0.1 + 0.2 == 0.3`
6. `'a' + 1`

Exécute-le et compare. Dans chaque cas où tu t'es trompé, écris une phrase dans ton [journal de bord](bitacora.md) avec la règle que tu n'avais pas en tête.

### Exercice 2 — La valeur de retour ignorée

Écris une méthode `String normalizar(String nombre)` qui retire les espaces des extrémités et passe le texte en minuscules, et teste-la avec `"  PAGOS "`. Écris-la **d'abord de la mauvaise façon** (en appelant `strip()` et `toLowerCase()` sans garder le résultat) et regarde ce qu'elle renvoie. Ensuite corrige-la. Peut-on la corriger en une seule ligne ?

### Exercice 3 — Deux façons de ne pas exploser

Écris deux méthodes : `boolean vacio(String detalle)`, qui renvoie `true` si le détail est nul **ou** vide, et `boolean contieneError(String detalle)`, qui renvoie `true` si le détail n'est pas nul **et** contient le mot `error`. Teste-les avec `null`, avec `"   "`, avec `"200"` et avec `"error de red"` (« erreur réseau »). Réfléchis avant d'écrire : lequel des deux utilise `&&` et lequel `||` ? Que se passerait-il avec le mauvais opérateur ?

### Exercice 4 — Le projet : une donnée à un seul endroit

Dans le `Revisor.java` de la section 2.5, ajoute `facturacion`, qui a répondu avec le code **503** en **95 ms**. Fais-le **en deux étapes**. D'abord, ajoute-le seulement dans `codigoDe` (et dans `main`, pour que sa ligne s'imprime), et exécute. Prédis avant quelle ligne sortira. Ensuite ajoute-le aussi dans `tiempoDe` et réexécute. Note dans le journal de bord quel problème pose le fait de maintenir la même information à deux endroits.

## Solutions

### Solution 1

```text
1421
33
123
-3
false
98
```

1. `"142" + 1` est un texte collé à un nombre : le `+` convertit le 1 en texte et les joint, `"1421"`. Ce n'est pas une addition.
2. `1 + 2 + "3"` s'évalue de gauche à droite : d'abord `1 + 2` (deux nombres) donne 3, puis `3 + "3"` colle, `"33"`.
3. `"1" + 2 + 3` commence par un texte, donc tout ce qui suit est collé : `"123"`.
4. `-7 / 2` est une division entière, qui écarte la partie décimale en direction du zéro : `-3`, pas `-4`.
5. `0.1 + 0.2 == 0.3` est `false`, parce que la somme est `0.30000000000000004`, comme nous l'avons vu en 2.1.1.
6. `'a' + 1` additionne un `char` et un `int` ; le `char` est converti en son numéro (97), et cela donne `98`. Pour obtenir le texte « a1 », il faudrait partir d'un texte : `"" + 'a' + 1`.

### Solution 2

La version erronée, qui ne change rien :

```text
[  PAGOS ]
false
```

La version correcte s'écrit en une seule ligne, en **enchaînant** les appels, parce que chaque méthode renvoie un `String` sur lequel on peut appeler la suivante :

```java
String normalizar(String nombre) {
  return nombre.strip().toLowerCase();
}
```

Avec elle, la sortie est :

```text
[pagos]
true
```

### Solution 3

`vacio` utilise `||` : il suffit que **l'une** des deux choses soit remplie. `contieneError` utilise `&&` : il faut que **les deux** soient remplies, et la première protège la seconde.

```java
boolean vacio(String detalle) {
  return detalle == null || detalle.isBlank();
}

boolean contieneError(String detalle) {
  return detalle != null && detalle.contains("error");
}
```

Les tests donnent `true`, `true`, `false` pour `vacio` avec `null`, `"   "` et `"200"`, et `false`, `true` pour `contieneError` avec `null` et avec `"error de red"`. Si tu écris `|` au lieu de `||` dans `vacio`, avec `null` le côté droit (`detalle.isBlank()`) est évalué et explose ; avec `&` au lieu de `&&` dans `contieneError`, pareil. Et une troisième issue, sans condition : `"sin conexión".equals(detalle)` donne `false` avec un `detalle` nul, parce que le littéral de gauche n'est jamais nul.

### Solution 4

Avec `facturacion` seulement dans `codigoDe`, la ligne qui apparaît est celle-ci :

```text
facturacion CAIDO          —  503
```

Elle sort CAIDO pour une mauvaise raison, ce qui est le danger de cette conception. `codigoDe` dit 503, mais `tiempoDe` dit `null`, donc `clasificar` applique la règle 1 (« n'a pas répondu ») et non la 2 (« a répondu avec un code d'erreur »), et le temps sort comme un tiret alors que le service a bien répondu en 95 ms. Le détail montre le 503, qui contredit le tiret. Il n'y a aucune erreur du compilateur ni de la JVM. Avec la donnée aussi dans `tiempoDe`, la bonne ligne est :

```text
facturacion CAIDO       95ms  503
```

La leçon qui va dans le journal de bord : **une donnée qui vit à deux endroits finit par dire deux choses**. C'est la raison pour laquelle la leçon suivante réunit code et temps en une seule valeur, et la raison pour laquelle c'est le compilateur, et non ta mémoire, qui t'oblige à la compléter.

## Comment savoir que j'ai réussi

- [ ] `java fig02_08.java` imprime `false` et `true` dans les deux premières lignes, et tu sais expliquer pourquoi sans regarder la leçon.
- [ ] `java fig02_07.java` montre `[  catalogo  ]` dans la première ligne, et tu sais ce que tu changerais pour qu'il montre `[catalogo]`.
- [ ] Si tu retires la ligne `default -> null;` de `codigoDe`, le lanceur répond avec `the switch expression does not cover all possible input values`.
- [ ] Dans `fig02_12.java`, tu vois la `NullPointerException` avec `"<local1>"`, et avec `javac -g` tu vois le nom `"detalle"`.
- [ ] Depuis `~/curso-java/revisor/`, `java Revisor.java` imprime l'en-tête et les quatre lignes du tableau, avec `reportes` en `CAIDO`, `—` et `sin conexión`.
- [ ] Tu peux expliquer pourquoi `2300 == 2300` donne `false` avec deux `Integer` et `true` avec deux `int`.
- [ ] Les exercices 1 et 3, tu les as prédits avant d'exécuter, et tu as noté dans ton journal de bord laquelle des six expressions de l'exercice 1 tu n'as pas devinée.
- [ ] Tu as noté dans ton journal de bord lequel des messages de cette leçon t'a coûté le plus à lire.

## Résumé

Réponds sans regarder la leçon :

1. Quelle différence y a-t-il entre `int` et `Integer`, et quand as-tu besoin du second ?
2. Que se passe-t-il quand un `int` dépasse sa valeur maximale ? Comment l'évites-tu dans un calcul de millisecondes ?
3. Quel type a une variable déclarée avec `var`, et quand est-il décidé ?
4. Pourquoi `linea.strip();` ne change-t-il pas `linea` ? Qu'écris-tu à la place ?
5. Que compare `==` entre deux références ? Pourquoi `armado == literal` donne-t-il `false` et `constante == literal` donne-t-il `true` ?
6. Pourquoi passe-t-on `Locale.ROOT` à `String.format` quand il y a des décimaux ?
7. Quelle différence y a-t-il entre `&&` et `&` quand le côté gauche est faux ?
8. Quelles deux choses distinguent un `switch` à flèches du classique ? Que se passe-t-il s'il ne couvre pas tous les cas ?
9. Que signifient `static` et `final` dans une constante ?
10. Comment se lit une `NullPointerException` : que dit-elle avant `because` et que dit-elle après ? Pourquoi apparaît-il parfois `<local1>` ?

## Pour aller plus loin

- [Les tutoriels Java, « Primitive Data Types »](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) : les huit types primitifs avec leurs plages et valeurs par défaut, dans la documentation officielle (en anglais, comme presque toute la documentation). Consulté le 7 octobre 2026.
- [Javadoc de `String`, Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html) : toutes les méthodes de la classe, avec la règle selon laquelle les chaînes sont constantes. Consulté le 7 octobre 2026.
- [JEP 361, « Switch Expressions »](https://openjdk.org/jeps/361) et [JEP 378, « Text Blocks »](https://openjdk.org/jeps/378) : le pourquoi et les règles exactes de deux des fonctionnalités utilisées dans cette leçon. Consulté le 7 octobre 2026.
- [JEP 358, « Helpful NullPointerExceptions »](https://openjdk.org/jeps/358) : ce que dit chaque partie du message et pourquoi les noms manquent quand on compile sans `-g`. Consulté le 7 octobre 2026.

---

**Précédente :** [Leçon 1 — Installer Java 25 sur ton Linux Mint](01-instalacion.md) ·
**Suivante :** [Leçon 3 — Records, enums et types scellés](03-records-enums-sellados.md)
