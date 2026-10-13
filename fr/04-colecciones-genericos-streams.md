# Leçon 4 — Collections, génériques et streams

**Durée :** 2 × 90 min (deux séances ; la coupure se situe entre les sections 4.2 et 4.3)

**Ce que tu construis :** la liste de services et le rapport du `revisor` : désormais le programme garde ses services dans une collection, résume les résultats avec un *stream* et les livre dans un tableau avec sa ligne de résumé

**Ce que tu apprends :** `List`, `Set` et `Map` ; collections immuables et copies défensives ; génériques et leur effacement ; lambdas et *streams* avec `Collectors` ; `Comparator` ; et `Optional` comme valeur de retour

**D'où tu viens.** Tu apportes le projet de la [Leçon 3](03-records-enums-sellados.md) : sept fichiers qui respectent un accord clair. `Estado` est un `enum` à trois valeurs ; `Respuesta` est une interface scellée qui ne peut être que `Respondio` (avec son code et son temps) ou `SinRespuesta` (avec son motif) ; `Servicio` et `Resultado` sont des *records* ; `Clasificador` décide l'état avec un `switch` exhaustif ; `Formato` compose les lignes ; et `Revisor.java` écrit à la main les quatre services et les vérifie un par un. Le tableau sort bien, mais il a trois trous. Il n'y a pas de liste : les services sont quatre variables isolées. Il n'y a pas de résumé : aucune ligne ne dit combien sont en ligne. Et les données simulées vivent dans un `switch`, mélangées à la logique. Cette leçon comble les trois trous avec les outils que Java apporte pour garder et transformer beaucoup de valeurs. Tu travailles dans ton dossier `~/curso-java/revisor/` et tu exécutes tout avec `java`.

**Ce que cette leçon ne fait pas.** Elle ne lit pas de fichiers et n'attrape pas d'erreurs : la liste des services reste écrite dans `Revisor.java`, et l'en retirer est le travail de la [Leçon 5](05-errores-excepciones.md). Elle ne consulte aucun réseau non plus : le `Simulador` que tu écris aujourd'hui joue le rôle du réseau jusqu'à la leçon 8. Et elle n'utilise pas Maven : les fichiers restent en vrac dans un dossier, comme dans la leçon 3.

**À propos des exécutions.** Pour exécuter une figure, entre dans le dossier de la leçon dans le dépôt (`programas/04-colecciones-genericos-streams/`) et lance `java fig04_01.java`, avec le numéro de la figure que tu veux. Celles qui vivent dans un dossier à elles (celle du projet de la section 4.5) s'exécutent depuis l'intérieur. Toutes les sorties ont été obtenues avec Java 25 (Temurin 25.0.4) et la langue du terminal en UTF-8 ; sous Linux Mint, c'est la norme ; si tu vois des points d'interrogation là où vont les accents, `echo $LANG` te dit quelle langue a ton terminal. Les traces d'erreur contiennent des lignes du JDK lui-même (`at java.base/...`) avec des numéros de ligne qui changent d'une mise à jour à l'autre : il est normal que les tiens diffèrent sur ces numéros.

## À la fin, tu seras capable de

- Choisir entre `List`, `Set` et `Map` selon ce que tu dois garder, et dire quel ordre de parcours a chaque implémentation.
- Créer une collection immuable avec `List.of`, reconnaître l'`UnsupportedOperationException` qui sort quand on la modifie et protéger un *record* par une copie défensive.
- Expliquer ce que sont les génériques, ce que le compilateur garantit avec eux et ce que l'effacement des types fait perdre.
- Écrire une lambda et une référence de méthode, et monter avec elles un *stream* de `filter`, `map` et `collect`.
- Résumer une liste de résultats avec `Collectors.groupingBy`, `counting` et `average`, et la trier avec un `Comparator`.
- Renvoyer un `Optional` au lieu de `null` et le lire sans appeler `get()` à l'aveugle.

## Le pourquoi avant le comment

Ouvre ton `Revisor.java` de la leçon 3. Les quatre services sont écrits comme quatre appels séparés à `revisar`, et c'est là que s'arrête ce que le programme sait d'eux : il ne peut pas dire combien ils sont, il ne peut pas les parcourir dans une boucle, il ne peut pas en ajouter un sans écrire une ligne de plus. Imagine maintenant qu'on te demande la ligne qui clôt le rapport : « 4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms » (« 4 services · 2 en ligne · 1 lent · 1 tombé · moyenne 843ms »). Pour la produire avec ce que tu as jusqu'à aujourd'hui, il te faudrait quatre compteurs (`arriba`, `lento`, `caido`, `total`), une somme, un diviseur, et tout mettre à jour chaque fois qu'un service change d'état. Chaque nouveau service, et chaque nouvel état, toucherait plusieurs endroits du programme.

Tous les langages ont résolu cela de la même façon : avec des **collections**, des structures qui gardent beaucoup de valeurs et savent se parcourir, faire des recherches et se compter toutes seules. Si tu viens de Python, tu connais la liste, l'ensemble et le dictionnaire ; si tu viens de Go, le *slice*, la map et l'habitude d'utiliser un `map[string]struct{}` comme ensemble ; si tu viens de TypeScript, le tableau, le `Set` et le `Map`. Java a les trois, et les organise d'une façon qui ressemble moins aux autres langages qu'il n'y paraît : ce sont des **interfaces** (`List`, `Set`, `Map`) avec **plusieurs implémentations** interchangeables, et cette séparation explique presque tout ce que tu vas voir aujourd'hui, de la raison pour laquelle il y a trois maps différentes à celle pour laquelle les collections immuables existent.

Il y a un problème de plus. Une collection garde des valeurs, mais de quel type ? Si elle garde n'importe quoi, le compilateur ne peut pas t'aider : tu devrais vérifier à la main que ce que tu sors de la liste est le service que tu attendais. Java résout cela avec les **génériques** : tu déclares que la liste est de `Servicio` et le compilateur empêche que quoi que ce soit d'autre y entre. C'est l'une des idées les plus utiles du langage et elle a un détail qui surprend presque tous ceux qui arrivent d'ailleurs : les génériques n'existent qu'à la compilation (ils sont « effacés » à l'exécution), et la leçon 0 l'avait déjà annoncé.

Et il reste la question de la façon de transformer une collection en une autre : filtrer les lents, convertir des résultats en lignes de texte, compter par état. Java le fait avec les **streams**, une façon de décrire la transformation comme une chaîne d'étapes. Et comme tout service n'a pas de temps (le tombé n'a pas répondu), il faut aussi décider comment représenter « il n'y a pas de valeur » sans retomber dans les `null` de la leçon 2 : c'est pour cela qu'`Optional` existe.

Le parcours comporte deux séances. La **première** couvre les sections 4.1 et 4.2 : les collections, les immuables et les génériques ; en la terminant, tu sais garder et protéger les services du `revisor`. La **seconde** couvre 4.3, 4.4 et 4.5 : les *streams*, `Optional` et le rapport complet ; en la terminant, le programme imprime le tableau avec son résumé. Voici la destination, pour que tu aies le point d'arrivée sous les yeux :

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Le tableau, tu le connaissais déjà ; ce qui est nouveau, c'est la ligne finale, et le fait qu'il sorte maintenant d'une vraie liste et non de quatre appels écrits à la main.

## Les concepts

### 4.1 Collections

#### 4.1.1 Une liste : `List` et `ArrayList`

Une **liste** est une séquence ordonnée de valeurs où chacune a une position, qui en Java commence à 0. En Java, `List` n'est pas une classe mais une **interface** : un contrat qui dit ce qu'on peut faire avec une liste (ajouter, chercher, retirer, parcourir) sans dire comment elle est stockée à l'intérieur. L'implémentation que tu vas utiliser presque toujours s'appelle `ArrayList`, qui garde les valeurs dans un tableau interne qui grandit quand il se remplit. Le schéma d'usage consiste à déclarer la variable avec l'interface et à créer l'objet avec l'implémentation :

```java
List<String> nombres = new ArrayList<>();
```

Cette ligne a trois pièces qu'il vaut mieux séparer. `List<String>` est le type de la variable : une liste de textes (ce qui est entre les signes `<` et `>` est le type de ses éléments ; la section 4.2 explique pourquoi). `new ArrayList<>()` crée une liste vide de la classe concrète. Et le `<>` vide s'appelle le **diamant** : il dit au compilateur « le type des éléments est le même que celui que j'ai déjà déclaré à gauche, ne me fais pas le répéter ». Pourquoi déclarer la variable comme `List` et non comme `ArrayList` ? Parce que le reste du programme n'a besoin que du contrat. Si demain tu changes l'implémentation pour une autre, tu changes un mot sur une ligne et rien de plus. C'est la même raison pour laquelle, dans la leçon 3, `Respuesta` était une interface et non une classe.

Regarde tout ce qu'on peut faire avec une liste dans le programme `fig04_01`. Avant de le lire, une précision : c'est un fichier compact, comme ceux de la leçon 2, et c'est pourquoi il n'a pas besoin d'écrire d'`import` pour `List` ni `ArrayList`. Les fichiers compacts importent tout seuls tout le module `java.base`, qui inclut `java.util`, où vivent les collections. Quand une classe normale en aura besoin, comme dans la section 4.5, elle les importera avec une ligne au début du fichier.

```java
// fig04_01.java
void main() {
  List<String> nombres = new ArrayList<>();
  nombres.add("catalogo");
  nombres.add("inventario");
  nombres.add("pagos");
  IO.println(nombres);
  IO.println("tamaño: " + nombres.size());
  IO.println("el segundo: " + nombres.get(1));
  IO.println("¿está pagos? " + nombres.contains("pagos"));
  IO.println("posición de pagos: " + nombres.indexOf("pagos"));
  IO.println("primero y último: " + nombres.getFirst() + ", " + nombres.getLast());

  nombres.add(1, "reportes");
  nombres.remove("pagos");
  nombres.set(0, "catalogo-v2");
  IO.println(nombres);

  for (String nombre : nombres) {
    IO.println("servicio: " + nombre);
  }
  for (int i = 0; i < nombres.size(); i++) {
    IO.println(i + " -> " + nombres.get(i));
  }
}
```

Exécute le programme et compare avec ce que tu avais prédit :

```text
$ java fig04_01.java
[catalogo, inventario, pagos]
tamaño: 3
el segundo: inventario
¿está pagos? true
posición de pagos: 2
primero y último: catalogo, pagos
[catalogo-v2, reportes, inventario]
servicio: catalogo-v2
servicio: reportes
servicio: inventario
0 -> catalogo-v2
1 -> reportes
2 -> inventario
```

Trois détails de cette sortie. Premièrement, `println` sait imprimer une liste entière : il l'écrit entre crochets avec les éléments séparés par des virgules, parce qu'`ArrayList` définit sa propre représentation sous forme de texte. Deuxièmement, `remove("pagos")` a retiré l'élément dont la valeur est « pagos » ; avec des textes il n'y a aucun doute possible, mais `remove` a deux versions (l'une qui reçoit une valeur et l'autre qui reçoit une position) et avec des listes de nombres elles se confondent. La section « Ce qui se fait de travers » montre le piège. Troisièmement, `getFirst()` et `getLast()` sont relativement nouveaux : ils sont arrivés dans Java 21 avec les collections séquencées ([JEP 431](https://openjdk.org/jeps/431)) et évitent l'ancien `get(lista.size() - 1)` pour le dernier élément.

Les deux façons de parcourir une liste dans le programme méritent une explication, parce que le cours ne les avait pas utilisées jusqu'à aujourd'hui. Le **`for` amélioré** (`for (String nombre : nombres)`) se lit « pour chaque `nombre` dans `nombres` » : à chaque tour, `nombre` vaut l'élément suivant, et il n'y a pas d'indice à tenir. C'est la forme habituelle. Le **`for` classique** (`for (int i = 0; i < nombres.size(); i++)`) tient l'indice à la main : il commence à 0 (`int i = 0`), répète tant que la condition est vraie (`i < nombres.size()`) et avance à la fin de chaque tour (`i++`, qui veut dire « ajoute un à `i` »). Utilise-le seulement quand tu as besoin de la position. Si tu te trompes avec elle, la liste te le dit avec une exception : demander `nombres.get(10)` dans une liste de trois éléments lève `IndexOutOfBoundsException: Index 10 out of bounds for length 3`, et le message apporte l'indice et la taille, ce qui est tout ce dont tu as besoin pour comprendre ce qui s'est passé.

Une dernière comparaison avec les langages que tu connais déjà, parce que la question revient toujours. Java a aussi des tableaux (`String[]`, de taille fixe, qui servent à peu de choses et que tu verras dans la leçon 5 et dans la 6 en écrivant `String[] args`), mais dans le code de tous les jours on utilise des listes : elles grandissent, permettent des recherches, se passent à des fonctions et se combinent avec des *streams*. Ce qui est un *slice* en Go et une `list` en Python est, en Java, un `List<T>`.

#### 4.1.2 Ensembles et maps

Un **ensemble** (`Set`) garde des valeurs **sans répétition** : en ajouter une qui y est déjà ne fait rien. Une **map** (`Map`) garde des **paires** clé-valeur, et cherche la valeur par la clé. Le programme `fig04_02` utilise les deux :

```java
// fig04_02.java
void main() {
  Set<String> vistos = new LinkedHashSet<>();
  IO.println(vistos.add("catalogo"));
  IO.println(vistos.add("pagos"));
  IO.println(vistos.add("catalogo"));
  IO.println(vistos + ", tamaño " + vistos.size());

  Map<String, Long> tiempos = new LinkedHashMap<>();
  tiempos.put("catalogo", 142L);
  tiempos.put("inventario", 2300L);
  tiempos.put("pagos", 87L);
  IO.println(tiempos);
  IO.println(tiempos.get("pagos"));
  IO.println(tiempos.get("reportes"));
  IO.println(tiempos.getOrDefault("reportes", 0L));
  IO.println(tiempos.containsKey("reportes"));
  IO.println(tiempos.put("pagos", 90L));
  IO.println(tiempos);

  for (Map.Entry<String, Long> par : tiempos.entrySet()) {
    IO.println(par.getKey() + " tardó " + par.getValue() + " ms");
  }

  Map<String, Long> ordenado = new TreeMap<>(tiempos);
  IO.println(ordenado);
}
```

```text
$ java fig04_02.java
true
true
false
[catalogo, pagos], tamaño 2
{catalogo=142, inventario=2300, pagos=87}
87
null
0
false
87
{catalogo=142, inventario=2300, pagos=90}
catalogo tardó 142 ms
inventario tardó 2300 ms
pagos tardó 90 ms
{catalogo=142, inventario=2300, pagos=90}
```

Lis la sortie calmement, parce qu'elle contient plusieurs leçons. Dans l'ensemble, `add` **renvoie un `boolean`** : `true` si la valeur est entrée et `false` si elle y était déjà. C'est pourquoi les trois premières lignes disent `true`, `true`, `false` : c'est une façon économique de demander « l'ai-je déjà vu ? », et tu vas l'utiliser dans la leçon 5 pour détecter des noms répétés dans la configuration.

Dans la map, `put` enregistre une paire et **renvoie la valeur précédente** de cette clé, ou `null` s'il n'y en avait pas. Dans la sortie, `put("pagos", 90L)` a imprimé `87`, la valeur qu'il venait de remplacer. `get` cherche par clé et renvoie `null` si la clé n'existe pas : `tiempos.get("reportes")` a imprimé `null`. Ce `null` est la même source d'erreurs que dans la leçon 2, et c'est pourquoi `getOrDefault("reportes", 0L)` existe : il renvoie la valeur par défaut que tu indiques quand la clé manque. `containsKey` répond à la question « cette clé est-elle là ? » sans toucher à la valeur.

Pour parcourir une map, on parcourt l'ensemble de ses paires avec `entrySet()` ; chaque paire est un `Map.Entry`, dont `getKey()` donne la clé et `getValue()` la valeur. Tu verras que le type est long à écrire (`Map.Entry<String, Long>`) ; dans le code réel, on le raccourcit avec `var`, que tu connais de la leçon 2 : `for (var par : tiempos.entrySet())`.

La question qui suit est la plus importante de la section : **dans quel ordre sortent les éléments ?** La réponse dépend de l'implémentation, et c'est pourquoi Java en a plusieurs pour chaque interface :

| Implémentation | Ordre de parcours | Quand l'utiliser |
|---|---|---|
| `HashSet`, `HashMap` | aucun que tu puisses promettre | quand seule compte la recherche rapide |
| `LinkedHashSet`, `LinkedHashMap` | l'ordre dans lequel ils ont été insérés | quand le rapport doit toujours sortir pareil |
| `TreeSet`, `TreeMap` | trié par la clé | quand tu as besoin de l'ordre naturel (alphabétique, numérique) |

La figure a utilisé `LinkedHashSet` et `LinkedHashMap` pour que la sortie soit toujours celle que tu vois, et a fini par copier la map dans une `TreeMap`, qui l'a réordonnée alphabétiquement (ici cela coïncide avec l'ordre d'insertion parce que les noms étaient déjà en ordre). Pour le `revisor`, ce n'est pas un détail : le plan du cours promet un rapport « toujours dans le même ordre », et un tableau qui sort dans un autre ordre à chaque exécution du programme est un rapport inutile. La règle pratique est : **si l'ordre compte pour qui lit le résultat, choisis une implémentation qui le garantit**, et ne te fie pas au fait qu'une `HashMap` « semble » ordonnée sur ta machine. En fait, les collections immuables que tu verras dans la section suivante (`Set.of`, `Map.of`) ont un ordre qui **change d'une exécution à l'autre** exprès : j'ai mesuré la même ligne, `Set.of("catalogo", "inventario", "pagos", "reportes")`, dans trois exécutions consécutives du même programme et il est sorti `[pagos, reportes, catalogo, inventario]`, puis `[catalogo, inventario, pagos, reportes]` et puis `[catalogo, reportes, pagos, inventario]`. La documentation l'avertit : [« the iteration order of set elements is unspecified and is subject to change »](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Set.html). L'ordre ne fait pas partie du contrat, et le JDK ne se donne pas la peine de le garder stable.

Reste une question : comment un `Set` sait-il que deux valeurs sont « la même » ? Il utilise deux méthodes : `equals`, que tu connais déjà, et `hashCode`, qui renvoie un nombre entier qui résume le contenu de l'objet (deux objets égaux doivent donner le même nombre, ce qui permet à l'ensemble de chercher sans comparer avec tous). Avec des textes et des nombres, cela fonctionne sans que tu fasses rien. Avec tes propres types, cela dépend de la façon dont ils sont définis, et ici les *records* de la leçon 3 tiennent ce qu'ils avaient promis : ils génèrent `equals` et `hashCode` à partir de tous leurs composants. Le programme `fig04_03` le montre : deux `Direccion` avec le même serveur et le même port sont égales même si ce sont deux objets distincts, et c'est pourquoi l'ensemble ne répète pas et la map trouve la valeur avec une clé tout juste construite.

```java
// fig04_03.java
record Direccion(String servidor, int puerto) {}

void main() {
  Set<Direccion> direcciones = new LinkedHashSet<>();
  direcciones.add(new Direccion("catalogo.example", 443));
  direcciones.add(new Direccion("pagos.example", 443));
  direcciones.add(new Direccion("catalogo.example", 443));
  IO.println(direcciones.size());
  IO.println(direcciones);

  Map<Direccion, String> duenos = new HashMap<>();
  duenos.put(new Direccion("pagos.example", 443), "equipo de cobranza");
  IO.println(duenos.get(new Direccion("pagos.example", 443)));
  IO.println(duenos.get(new Direccion("pagos.example", 8443)));
}
```

```text
$ java fig04_03.java
2
[Direccion[servidor=catalogo.example, puerto=443], Direccion[servidor=pagos.example, puerto=443]]
equipo de cobranza
null
```

L'ensemble a fini avec deux éléments et non trois, la map a trouvé l'« equipo de cobranza » (« équipe de recouvrement ») avec une nouvelle clé, et la clé avec un autre port (`8443`) n'existe pas, donc `get` a renvoyé `null`. Si au lieu d'un *record* tu avais écrit une classe normale sans définir `equals` et `hashCode`, chaque objet serait égal seulement à lui-même, et l'ensemble garderait les trois. C'est l'une des raisons pour lesquelles le cours préfère les *records* pour les données.

#### 4.1.3 Collections immuables

Jusqu'ici, les collections pouvaient se modifier : ajouter, retirer, remplacer. Souvent, c'est justement ce que tu ne veux pas. La liste de services que lit le `revisor` ne devrait pas changer pendant que quelqu'un d'autre la parcourt, et un rapport déjà calculé ne devrait pas pouvoir être altéré ensuite. Java résout cela avec les **collections immuables**, qui se créent avec trois méthodes : `List.of(...)`, `Set.of(...)` et `Map.of(...)`. (On les appelle méthodes de fabrique statiques : tu n'as pas besoin de `new`, seulement du nom de l'interface, d'un point et des valeurs.) Regarde ce qui se passe quand tu essaies d'en modifier une :

```java
// fig04_04.java
void main() {
  List<String> nombres = List.of("catalogo", "inventario", "pagos");
  IO.println(nombres);
  nombres.add("reportes");
  IO.println(nombres);
}
```

```text
$ java fig04_04.java
[catalogo, inventario, pagos]
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_04.main(fig04_04.java:5)
```

Le programme imprime la liste une fois, et le `add` lève `java.lang.UnsupportedOperationException` (« opération non supportée »). C'est une exception sans message, et elle n'en a pas besoin : l'opération demandée n'existe tout simplement pas dans cette liste. La trace se lit **de haut en bas**, à l'inverse de celle de Python : la première ligne dit ce qui a été levé, la deuxième où cela est né à l'intérieur du JDK (`ImmutableCollections.uoe`, « unsupported operation exception »), la troisième quelle méthode tu as appelée (`add`) et la dernière, `fig04_04.main(fig04_04.java:5)`, quelle ligne de **ton** programme l'a fait. Cherche toujours la première ligne qui mentionne ton fichier : c'est là qu'est ton erreur. (Les numéros `159` et `164` sont de ce JDK et changeront dans le tien.)

L'immuabilité de ces collections est réelle et complète pour ce qui concerne la collection : on ne peut ni ajouter, ni retirer, ni remplacer d'éléments. Les auteurs du JDK ont ajouté d'autres règles, qui figurent dans la documentation de [`List`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html) et que j'ai vérifiées en écrivant cette leçon :

- **Elles n'admettent pas `null`.** `List.of("a", null)` lève `NullPointerException` à la création. C'est une décision en faveur de moins de surprises : un `null` dans une collection est presque toujours une erreur.
- **`Set.of` et `Map.of` rejettent les doublons.** `Map.of("a", 1, "a", 2)` lève `IllegalArgumentException: duplicate key: a`, et `Set.of("a", "a")` lève `IllegalArgumentException: duplicate element: a`. Une clé répétée dans une map de données fixes est presque sûrement un oubli, et on le découvre au démarrage, pas quand quelqu'un consulte.
- **Leur ordre n'est pas stable d'une exécution à l'autre**, comme on l'a mesuré plus haut.
- **`Map.of` accepte jusqu'à dix paires.** Pour davantage, il existe `Map.ofEntries`.

Deux précisions pour ne pas confondre ce que promet l'immuabilité. La première : **immuable ne signifie pas que les éléments soient immuables**. Une `List.of(new ArrayList<>(), new ArrayList<>())` ne laisse ni ajouter ni retirer de listes, mais chaque liste de l'intérieur peut toujours être modifiée. Avec des *records* à composants simples (textes, nombres, autres *records*), comme ceux du `revisor`, le problème n'existe pas ; avec des éléments modifiables, si. La seconde : la leçon 3 a déjà mentionné l'**immuabilité superficielle** des *records* (un composant tableau reste modifiable), et c'est la même chose sous un autre nom : le *record* ne peut pas changer de liste, mais si la liste qu'il garde est modifiable, la liste, elle, change. Cela mène à la section suivante.

#### 4.1.4 Copies défensives

Voici un piège qui s'apprend mieux en le voyant. Regarde le programme `fig04_05`. Il définit deux *records* de même forme : `ReporteIngenuo`, qui garde la liste telle qu'on la lui donne, et `ReporteSeguro`, qui, dans son constructeur compact (celui de la leçon 3), remplace la liste par `List.copyOf(nombres)`, une **copie immuable**. Ensuite il leur passe la même liste et la modifie après :

```java
// fig04_05.java
record ReporteIngenuo(List<String> nombres) {}

record ReporteSeguro(List<String> nombres) {
  ReporteSeguro {
    nombres = List.copyOf(nombres);
  }
}

void main() {
  List<String> nombres = new ArrayList<>(List.of("catalogo", "pagos"));
  ReporteIngenuo ingenuo = new ReporteIngenuo(nombres);
  ReporteSeguro seguro = new ReporteSeguro(nombres);
  nombres.add("reportes");
  IO.println("ingenuo: " + ingenuo);
  IO.println("seguro:  " + seguro);

  List<String> vista = Collections.unmodifiableList(nombres);
  List<String> copia = List.copyOf(nombres);
  nombres.add("facturacion");
  IO.println("vista: " + vista);
  IO.println("copia: " + copia);
}
```

```text
$ java fig04_05.java
ingenuo: ReporteIngenuo[nombres=[catalogo, pagos, reportes]]
seguro:  ReporteSeguro[nombres=[catalogo, pagos]]
vista: [catalogo, pagos, reportes, facturacion]
copia: [catalogo, pagos, reportes]
```

Le rapport naïf a changé tout seul, sans que personne y touche : il a gardé une **référence** à la liste d'origine, pas une copie, et quand le propriétaire d'origine a ajouté `reportes`, le « rapport » déjà calculé s'est altéré. Le rapport sûr a conservé les deux noms qu'il avait au moment de sa création. C'est une **copie défensive** : le constructeur se défend contre le fait que quelqu'un d'autre garde une référence aux données qui sont désormais les siennes. C'est l'une de ces choses qui n'échouent pas : elles renvoient un mauvais nombre une semaine plus tard.

Les deux dernières lignes de la figure montrent une autre différence qui embrouille. `Collections.unmodifiableList(nombres)` ne copie rien : c'est une **vue** qui ne permet pas de modifier *à travers elle*, mais qui reflète tout ce que le propriétaire d'origine change (c'est pourquoi `vista` a fini avec `facturacion`). `List.copyOf(nombres)` fait bien une copie indépendante (c'est pourquoi `copia` ne l'a pas). Si ce que tu veux, c'est que personne ne puisse altérer tes données, il te faut la copie ; la vue empêche seulement celui qui la reçoit d'*écrire*.

Cette technique n'est pas un ornement de style. Ce que tu viens de voir est exactement la raison pour laquelle le `Reporte` de la section 4.5 porte `resultados = List.copyOf(resultados);` dans son constructeur. Et ce n'est pas une manie de ce cours : les analyseurs de code connaissent la faille. Dans la leçon 10, tu feras connaissance avec SpotBugs, un outil qui examine le programme compilé à la recherche de défauts probables, et qui appelle ce schéma `EI_EXPOSE_REP` et `EI_EXPOSE_REP2` (de *exposing internal representation*, « exposer la représentation interne »). Avec une condition qu'il vaut mieux connaître dès maintenant, parce qu'elle a été mesurée : SpotBugs ne le signale que dans les classes visibles depuis l'extérieur de leur paquet. Le `Reporte` du `revisor` n'est pas public, et si tu retires la copie, l'outil reste muet. La copie n'est pas là pour contenter un outil : elle est là parce que la liste appartient au *record*, et ce que l'outil ne vérifie pas ne cesse pas d'être un défaut.

Une note d'efficacité : `List.copyOf` est intelligent. Si la liste que tu lui passes est déjà immuable (par exemple, celle qu'a créée `List.of`), il ne la copie pas, il renvoie la même. C'est logique : il n'y a rien à protéger. La documentation le dit par la phrase « if the given Collection is an unmodifiable List, calling copyOf will generally not create a copy ».

### 4.2 Génériques

#### 4.2.1 Ce qu'ils sont et ce qu'ils te donnent

Chaque fois que tu as écrit `List<String>` ou `Map<String, Long>`, tu as utilisé des **génériques** : une classe ou interface qui est définie pour « un certain type », et que tu précises à l'usage. `List<E>` se définit une seule fois, avec un type `E` à remplir, et c'est toi qui décides si c'est une liste de `String`, de `Integer` ou de `Servicio`. Ce que tu y gagnes, c'est une vérification **à la compilation** : le compilateur sait quel type d'élément a la liste et rejette ce qui n'y tient pas. Voyons ce qu'il dit quand tu essaies de mettre un texte dans une liste de nombres :

```java
// fig04_06.java
void main() {
  List<Integer> tiempos = new ArrayList<>();
  tiempos.add(142);
  tiempos.add("87 ms");
  IO.println(tiempos);
}
```

```text
$ java fig04_06.java
fig04_06.java:5: error: no suitable method found for add(String)
  tiempos.add("87 ms");
         ^
    method List.add(Integer) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method List.add(int,Integer) is not applicable
      (actual and formal argument lists differ in length)
1 error
error: compilation failed
```

Le message est plus long que tu ne l'attendrais. Le compilateur dit `no suitable method found for add(String)` (« je n'ai pas trouvé de méthode `add` qui convienne à un `String` ») parce que `List` a deux méthodes appelées `add` : l'une qui reçoit un élément (`add(Integer)`) et l'autre qui reçoit une position et un élément (`add(int, Integer)`). Il les a essayées toutes les deux et explique pourquoi aucune ne convient : la première parce qu'un `String` ne se convertit pas en `Integer` (*argument mismatch*, « l'argument ne correspond pas »), la seconde parce qu'elle n'a même pas le même nombre de paramètres. Lis toujours la **ligne entre parenthèses** : c'est là qu'est la vraie cause. Si en revanche tu essaies de sortir une valeur et de l'assigner à la mauvaise variable (`String texto = tiempos.get(0);`), le message est le plus simple : `incompatible types: Integer cannot be converted to String`.

Sans génériques, aucune des deux erreurs n'existerait à la compilation : la liste accepterait n'importe quoi, et le premier avertissement arriverait à l'exécution, loin de l'endroit où l'erreur a été commise. C'est la différence entre un langage qui te protège et un langage qui te laisse écrire n'importe quoi. (La section 4.2.3 montre ce qui se passe quand quelqu'un contourne la protection exprès.)

#### 4.2.2 Les génériques n'acceptent que des références : les classes enveloppes

Écris `List<int>` et regarde ce que dit le compilateur :

```java
// fig04_07.java
void main() {
  List<int> tiempos = new ArrayList<>();
  tiempos.add(142);
  IO.println(tiempos);
}
```

```text
$ java fig04_07.java
fig04_07.java:3: error: unexpected type
  List<int> tiempos = new ArrayList<>();
       ^
  required: reference
  found:    int
1 error
error: compilation failed
```

Le message est `unexpected type; required: reference; found: int` : on demandait une référence et on a trouvé un primitif. Les génériques ne travaillent qu'avec des types référence, et `int` est un primitif (leçon 2). La solution est d'utiliser la **classe enveloppe** du primitif : `Integer` pour `int`, `Long` pour `long`, `Double` pour `double`, `Boolean` pour `boolean`. Ce sont les types que tu connais de la leçon 2 et qui ont maintenant leur raison d'être. Java convertit tout seul entre un primitif et son enveloppe, dans les deux sens, quand il le faut : cela s'appelle ***autoboxing*** (mettre le `142` dans un `Integer`) et *unboxing* (l'en sortir). C'est commode, mais cela comporte deux pièges que tu connais déjà sous un autre nom, et qui valent une figure chacun.

Le premier : `==` entre enveloppes compare des **références**, comme avec les `String`. Le programme `fig04_08` le mesure :

```java
// fig04_08.java
void main() {
  List<Integer> tiempos = List.of(87, 87, 1000, 1000);
  IO.println(tiempos.get(0) == tiempos.get(1));
  IO.println(tiempos.get(2) == tiempos.get(3));
  IO.println(tiempos.get(2).equals(tiempos.get(3)));
}
```

```text
$ java fig04_08.java
true
false
true
```

Les deux `87` sont « le même » objet et les deux `1000` non. La raison en est que l'*autoboxing* utilise `Integer.valueOf`, et [sa documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Integer.html) promet qu'il « met toujours en cache les valeurs de −128 à 127, et peut en mettre en cache d'autres » : pour les petites, il réutilise le même objet, et pour le `1000` de cette figure, en pratique, il en crée un nouveau à chaque fois. C'est un comportement documenté, pas une erreur, et c'est pourquoi il est dangereux : un programme qui compare avec `==` fonctionne avec les petits temps des tests et échoue avec ceux de la production. La règle est celle de la leçon 2, étendue : **compare les valeurs avec `equals`**, jamais avec `==`, chaque fois que le type est une référence, et c'est le cas des enveloppes.

Le second piège est l'*unboxing* d'une valeur qui n'est pas là. Le programme `fig04_09` demande à une `Map<String, Long>` un temps et le garde dans une variable `long` :

```java
// fig04_09.java
void main() {
  Map<String, Long> tiempos = Map.of("catalogo", 142L, "pagos", 87L);
  long tiempoPagos = tiempos.get("pagos");
  IO.println("pagos: " + tiempoPagos);
  long tiempoReportes = tiempos.get("reportes");
  IO.println("reportes: " + tiempoReportes);
}
```

```text
$ java fig04_09.java
pagos: 87
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Long.longValue()" because the return value of "java.util.Map.get(Object)" is null
	at fig04_09.main(fig04_09.java:6)
```

La première recherche fonctionne. La seconde échoue avec une `NullPointerException` dont le message est un bijou de diagnostic : `Cannot invoke "java.lang.Long.longValue()" because the return value of "java.util.Map.get(Object)" is null`. Elle se lit ainsi : « je n'ai pas pu appeler `longValue()` parce que ce que `Map.get` a renvoyé est `null` ». Personne n'a écrit `longValue()` : c'est le compilateur qui l'a inséré en convertissant le `Long` en `long`, et cela a échoué parce que `get("reportes")` a renvoyé `null` (la clé n'existait pas). Les messages de `NullPointerException` te disent toujours **ce qui** était nul, et dans ce cas même quelle méthode l'a renvoyé. Deux façons d'éviter l'erreur : demander `getOrDefault("reportes", 0L)`, ou garder le résultat dans un `Long` (pas `long`) et demander `if (tiempo != null)`. La section 4.4 en apporte une troisième, meilleure.

#### 4.2.3 L'effacement des types

La leçon 0 l'a déjà annoncé : **les génériques sont effacés**. Le compilateur utilise `List<String>` pour vérifier ton code, et après l'avoir vérifié il retire les types : dans le *bytecode*, une `List<String>` et une `List<Integer>` sont la même `List`. Ils sont restés ainsi depuis Java 5 : l'effacement assure qu'on ne crée pas de nouvelle classe pour chaque type paramétré, de sorte que les génériques [ne coûtent rien à l'exécution](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html), et que le code écrit avant qu'ils n'existent (presque toutes les collections l'étaient) a continué de fonctionner. Le programme `fig04_10` le confirme et montre ce que cela coûte :

```java
// fig04_10.java
void main() {
  List<String> nombres = new ArrayList<>();
  List<Integer> tiempos = new ArrayList<>();
  IO.println(nombres.getClass() == tiempos.getClass());
  IO.println(nombres.getClass().getName());

  List crudo = nombres;
  crudo.add(142);
  IO.println("agregué un número a una lista de String y nadie se quejó");
  String primero = nombres.get(0);
  IO.println(primero);
}
```

```text
$ java fig04_10.java
fig04_10.java:9: warning: [unchecked] unchecked call to add(E) as a member of the raw type List
  crudo.add(142);
           ^
  where E is a type-variable:
    E extends Object declared in interface List
1 warning
true
java.util.ArrayList
agregué un número a una lista de String y nadie se quejó
Exception in thread "main" java.lang.ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String (java.lang.Integer and java.lang.String are in module java.base of loader 'bootstrap')
	at fig04_10.main(fig04_10.java:11)
```

Il y a plusieurs choses à lire ici. Le compilateur a imprimé un avertissement (`warning: [unchecked] unchecked call to add(E) as a member of the raw type List`) avant d'exécuter le programme : il te prévient qu'à la ligne 9 tu as fait quelque chose qu'il ne peut pas vérifier. La cause est à la ligne 8 : `List crudo = nombres;`. Une `List` sans le `<...>` est un **type brut** (*raw type*), le même type qu'avant Java 5. Le compilateur accepte d'y stocker une liste de `String` et accepte d'y ajouter un `142`, parce qu'avec le type brut il ne vérifie rien, il avertit seulement. Ensuite, le programme imprime « agregué un número a una lista de String y nadie se quejó » (« j'ai ajouté un nombre à une liste de String et personne ne s'est plaint ») et échoue à la **ligne 11**, pas à la 9 :

`ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String`

L'erreur est apparue en *sortant* l'élément, deux lignes après celle qui l'avait mis. Dans un vrai programme, ce pourrait être deux classes différentes et dix minutes de débogage. C'est la conséquence du fait que les génériques ne sont vérifiés qu'à la compilation : le compilateur n'a pu te promettre que la liste était de `String` que tant que personne ne contournait le système de types, et le type brut est la brèche. La règle : **n'utilise jamais de types bruts dans du code nouveau** ; si tu vois un avertissement `[unchecked]`, c'est le compilateur qui te dit de corriger quelque chose.

Les deux premières lignes de la sortie sont l'autre face de l'effacement. `nombres.getClass() == tiempos.getClass()` est `true` et le nom de la classe est `java.util.ArrayList`, sans trace du `<String>` : à l'exécution, la liste ne sait pas de quoi elle est. De là sortent trois limites que tu vas rencontrer en pratique, et qu'il vaut mieux connaître dès maintenant pour ne pas perdre une heure à les chercher : tu ne peux pas écrire `new T()` dans une méthode générique, tu ne peux pas demander `if (lista instanceof List<String>)` (seulement `List<?>`), et tu ne peux pas surcharger deux méthodes qui ne se distinguent que par le type générique (`void f(List<String>)` et `void f(List<Integer>)` entrent en conflit parce que, une fois effacées, ce sont la même méthode). Le [tutoriel officiel de Java sur l'effacement](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html) liste les règles complètes.

#### 4.2.4 Écrire une méthode générique et lire une signature

Tu ne vas presque jamais *définir* une classe générique, mais tu vas lire les signatures de celles du JDK, et de temps en temps tu écriras une méthode générique. Regarde-en une dans `fig04_11` :

```java
// fig04_11.java
static <T> T primeroO(List<T> lista, T porOmision) {
  if (lista.isEmpty()) {
    return porOmision;
  }
  return lista.get(0);
}

void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<Long> tiempos = List.of();
  String primerNombre = primeroO(nombres, "ninguno");
  Long primerTiempo = primeroO(tiempos, 0L);
  IO.println(primerNombre);
  IO.println(primerTiempo);
}
```

```text
$ java fig04_11.java
catalogo
0
```

Le `<T>` avant le type de retour déclare une **variable de type** : « dans cette méthode, `T` est un type à déterminer ». Le compilateur le déduit des arguments : dans le premier appel `T` est `String` et dans le second `Long`, et c'est pourquoi le résultat peut se garder dans un `String` et dans un `Long` sans aucune conversion. L'écrire avec `Object` au lieu de `T` t'obligerait à convertir à la main.

Et pour lire la documentation, un symbole de plus : `?`, le **joker** (*wildcard*). Tu verras des signatures comme `List.copyOf(Collection<? extends E> coll)`. Elle se lit « une collection d'*un certain type* qui soit `E` ou une sous-classe de `E` ». Le mot `extends` signifie ici « est `E` ou en hérite », et le joker permet de lui passer, par exemple, une liste de `Respondio` là où l'on attend une collection de `Respuesta`. Tu n'as pas encore besoin d'écrire des jokers ; tu as besoin de pouvoir les lire sans t'effrayer, parce qu'ils apparaissent dans `Collectors`, dans `Comparator` et dans presque toute la bibliothèque de *streams* qui suit.

Ainsi se termine la première séance. Avant de continuer, vérifie que tu peux répondre sans regarder : quelle différence y a-t-il entre `List.copyOf` et `Collections.unmodifiableList` ? Pourquoi `Set.of` imprime-t-il dans un autre ordre chaque fois ? Qu'est-ce qui échoue à la compilation avec `List<int>` et qu'est-ce qui échoue à l'exécution avec une liste brute ? Si tu hésites sur l'une d'elles, retourne à sa figure avant d'ouvrir la séance deux.

### 4.3 Streams

La seconde séance commence. Jusqu'ici, tu as gardé des valeurs ; maintenant tu vas les transformer. La question concrète du `revisor` est : étant donné la liste de résultats, combien sont en ligne, combien lents, combien tombés, et quelle est la moyenne des temps de ceux qui ont répondu ? Avec ce que tu sais, tu peux écrire un `for` avec quatre compteurs. Les *streams* sont l'autre réponse, et pour les comprendre il faut d'abord comprendre l'outil avec lequel ils s'écrivent : les lambdas.

#### 4.3.1 Lambdas et références de méthode

Une **lambda** est une petite fonction, sans nom, que tu écris à l'endroit où tu en as besoin et que tu passes comme si c'était une valeur. Sa forme est `paramètres -> corps`. Tu as déjà vu la flèche dans le `switch` de la leçon 2, mais ici elle signifie autre chose : non pas « si le cas coïncide, renvoie ceci », mais « étant donné ce paramètre, calcule ceci ». Voici les formes qui existent :

- `medicion -> medicion.tiempoMs() >= 1000` : un paramètre, un corps qui est une expression (le résultat est la valeur de l'expression).
- `(a, b) -> a + b` : deux paramètres, entre parenthèses.
- `() -> "revisando..."` : aucun, avec des parenthèses vides.
- `medicion -> { ...varias líneas...; return valor; }` : un bloc entre accolades, avec `return` s'il doit renvoyer quelque chose.

Et quel est le *type* d'une lambda ? En Java, une lambda s'assigne toujours à une **interface fonctionnelle** : une interface avec une unique méthode abstraite. La lambda est l'implémentation de cette méthode. Le JDK en apporte plusieurs qui s'utilisent sans cesse : `Predicate<T>` (une méthode, `test`, qui reçoit un `T` et renvoie `boolean`), `Function<T, R>` (`apply`, reçoit un `T`, renvoie un `R`), `Supplier<T>` (`get`, ne reçoit rien et renvoie un `T`) et `Comparator<T>` (`compare`, qui en reçoit deux et renvoie un entier). Le programme `fig04_12` les utilise une par une :

```java
// fig04_12.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  Predicate<Medicion> esLenta = medicion -> medicion.tiempoMs() >= 1000;
  Function<Medicion, String> etiqueta = medicion -> medicion.nombre() + ":" + medicion.tiempoMs();
  Function<Medicion, String> soloNombre = Medicion::nombre;
  Supplier<String> saludo = () -> "revisando...";

  Medicion inventario = new Medicion("inventario", 2300);
  Medicion pagos = new Medicion("pagos", 87);
  IO.println(esLenta.test(inventario));
  IO.println(esLenta.test(pagos));
  IO.println(etiqueta.apply(pagos));
  IO.println(soloNombre.apply(inventario));
  IO.println(saludo.get());
}
```

```text
$ java fig04_12.java
true
false
pagos:87
inventario
revisando...
```

`esLenta` est un `Predicate<Medicion>` : on lui passe une mesure avec `test` et il répond `true` ou `false`. `etiqueta` est une `Function<Medicion, String>` : elle convertit une mesure en un texte. `saludo` est un `Supplier<String>` : il ne reçoit rien et produit une valeur. Quand tu écris une lambda dans un *stream*, tu ne nommes pas ces types ; le compilateur les déduit de la méthode à laquelle tu la passes. Mais les connaître est ce qui te permet de lire la documentation : quand `filter` demande un `Predicate`, il demande « une fonction qui reçoit un élément et répond oui ou non ».

`soloNombre` montre le second outil : la **référence de méthode**. `Medicion::nombre` est la forme courte de `medicion -> medicion.nombre()` : « appelle la méthode `nombre` de l'élément qu'on te donne ». Elle se lit mieux quand la lambda ne fait qu'appeler une méthode, et tu la verras partout (`Resultado::estado`, `Revision::revisar`, `Formato::linea`). Utilise celle qui se lit le plus clairement ; le compilateur les traite pareil.

Il y a une règle des lambdas qui surprend ceux qui arrivent de JavaScript ou de Python. Regarde ce programme, qui essaie de sommer les temps avec une lambda qui modifie une variable extérieure :

```java
// fig04_13.java
void main() {
  List<Long> tiempos = List.of(142L, 2300L, 87L);
  long suma = 0;
  tiempos.forEach(tiempo -> suma += tiempo);
  IO.println(suma);
}
```

```text
$ java fig04_13.java
fig04_13.java:5: error: local variables referenced from a lambda expression must be final or effectively final
  tiempos.forEach(tiempo -> suma += tiempo);
                            ^
1 error
error: compilation failed
```

L'erreur dit `local variables referenced from a lambda expression must be final or effectively final` : une lambda ne peut utiliser des variables locales extérieures que si elles ne sont **jamais réassignées** (elles sont *effectivement finales*). Ici `suma` change à chaque tour, et le compilateur le rejette. La raison est qu'une lambda peut s'exécuter plus tard, ou dans un autre thread, et Java ne veut pas que la lambda et le code extérieur partagent une variable qui change. Dans la leçon 9, tu verras pourquoi c'est une protection précieuse. La solution à ce problème n'est pas de contourner la règle mais d'utiliser le bon outil : `mapToLong(...).sum()`, ce qui vient justement dans la section suivante. Si tu te surprends à écrire une lambda qui modifie quelque chose à l'extérieur, c'est presque toujours le signe qu'il lui manque un `collect` ou un `sum`.

#### 4.3.2 Un *stream* est un tuyau

Un ***stream*** (flux) n'est pas une collection : c'est une **description d'une séquence d'étapes** qui s'appliquent aux éléments d'une source. Il a trois parties. Une **source**, qui est presque toujours une collection et s'obtient avec `.stream()`. Zéro ou plusieurs **opérations intermédiaires** (`filter`, `map`, `sorted`...), qui transforment le flux en un autre flux. Et une **opération terminale** (`toList`, `count`, `sum`, `collect`...), qui produit le résultat final et qui est celle qui met vraiment tout en marche. Les étapes s'écrivent enchaînées, une par ligne, et se lisent de haut en bas comme une recette. Regarde le programme `fig04_14`, qui fait le même travail deux fois : d'abord avec une boucle puis avec un *stream*.

```java
// fig04_14.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", 142),
          new Medicion("inventario", 2300),
          new Medicion("pagos", 87));

  List<String> conBucle = new ArrayList<>();
  for (Medicion medicion : mediciones) {
    if (medicion.tiempoMs() >= 100) {
      conBucle.add(medicion.nombre() + ":" + medicion.tiempoMs());
    }
  }

  List<String> conStream =
      mediciones.stream()
          .filter(medicion -> medicion.tiempoMs() >= 100)
          .map(medicion -> medicion.nombre() + ":" + medicion.tiempoMs())
          .toList();

  IO.println(conBucle);
  IO.println(conStream);
  IO.println(conBucle.equals(conStream));

  List<String> nombres = mediciones.stream().map(Medicion::nombre).toList();
  long suma = mediciones.stream().mapToLong(Medicion::tiempoMs).sum();
  long rapidos = mediciones.stream().filter(medicion -> medicion.tiempoMs() < 100).count();
  IO.println(nombres);
  IO.println("suma: " + suma + " ms");
  IO.println("rápidos: " + rapidos);
}
```

```text
$ java fig04_14.java
[catalogo:142, inventario:2300]
[catalogo:142, inventario:2300]
true
[catalogo, inventario, pagos]
suma: 2529 ms
rápidos: 1
```

La boucle et le *stream* produisent la même liste (la dernière ligne vérifie avec `equals` qu'elles sont égales). Le *stream* se lit ainsi : « parmi les mesures, **garde** celles qui remplissent `tiempoMs() >= 100` (`filter`), **convertis** chacune en un texte (`map`) et **rassemble** le résultat dans une liste (`toList`) ». Remarque ce qu'il n'y a pas : il n'y a pas de liste vide qu'on remplit, ni de condition enveloppée dans un `if`, ni de variable qui change. Qui lit décrit ce qu'il veut, pas comment le faire.

Les opérations que tu utiliseras le plus souvent :

| Opération | Type | Ce qu'elle fait |
|---|---|---|
| `filter(Predicate)` | intermédiaire | ne laisse passer que les éléments qui remplissent la condition |
| `map(Function)` | intermédiaire | transforme chaque élément en une autre valeur |
| `mapToLong(...)` | intermédiaire | transforme chaque élément en un `long` primitif (sans enveloppes) |
| `sorted(Comparator)` | intermédiaire | trie (section 4.3.5) |
| `toList()` | terminale | rassemble les éléments dans une liste **immuable** |
| `count()` | terminale | compte les éléments |
| `sum()`, `average()`, `max()` | terminale | agrègent ; elles existent sur les flux de primitifs |
| `collect(Collector)` | terminale | rassemble les éléments avec une recette (section 4.3.4) |

Deux précisions sur le tableau. `mapToLong` produit un flux spécialisé (`LongStream`) qui sait sommer et faire la moyenne sans envelopper chaque nombre dans un `Long` ; ses frères `IntStream` et `DoubleStream` existent. Et `toList()` renvoie une liste **immuable**, comme `List.of` : si plus tard tu la veux modifiable, tu la copies avec `new ArrayList<>(lista)`. La section « L'erreur que tu vas voir » montrera l'exception qui sort quand on l'oublie.

#### 4.3.3 Paresseux et à usage unique

Il y a deux propriétés des *streams* qui ne se devinent pas et qui produisent des erreurs si tu les ignores. La première est qu'ils sont **paresseux** : monter le flux ne fait rien. Selon la [documentation du paquet `java.util.stream`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html), les opérations intermédiaires « sont toujours paresseuses » : `filter` ne filtre rien quand tu l'appelles, il crée seulement un nouveau flux qui filtrera quand quelqu'un le parcourra. Le travail a lieu quand arrive l'opération terminale. Le programme `fig04_15` le rend visible avec `peek`, une opération qui laisse passer chaque élément et exécute au passage une action (sa documentation dit qu'elle existe « principalement pour déboguer », et c'est seulement pour cela que nous l'utilisons ici) :

```java
// fig04_15.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", 142),
          new Medicion("inventario", 2300),
          new Medicion("pagos", 87));

  Stream<String> armado =
      mediciones.stream()
          .peek(medicion -> IO.println("miro " + medicion.nombre()))
          .filter(medicion -> medicion.tiempoMs() >= 100)
          .peek(medicion -> IO.println("  pasa " + medicion.nombre()))
          .map(Medicion::nombre);
  IO.println("el stream está armado; todavía no pasó nada");

  List<String> resultado = armado.toList();
  IO.println(resultado);
}
```

```text
$ java fig04_15.java
el stream está armado; todavía no pasó nada
miro catalogo
  pasa catalogo
miro inventario
  pasa inventario
miro pagos
[catalogo, inventario]
```

Regarde l'ordre. « el stream está armado; todavía no pasó nada » (« le stream est monté ; il ne s'est encore rien passé ») sort **en premier**, avant tout « miro » (« je regarde »). Les `peek` et le `filter` ne se sont pas exécutés une seule fois quand le flux a été monté : ils ont attendu `toList()`. Et quand ils s'exécutent enfin, ils ne le font pas étape par étape sur toute la liste, mais **élément par élément** : `catalogo` passe par `peek`, `filter`, `peek` et `map` avant que `inventario` entre dans le flux, et `pagos`, qui ne remplit pas le filtre, reste au premier `peek`. Cela a des conséquences pratiques : un flux sans opération terminale est du code mort (le compilateur ne le signale pas), et une opération intermédiaire avec un effet de bord, comme imprimer, s'exécute dans un ordre qui n'est pas celui de « d'abord tous, ensuite tous ».

La seconde propriété est qu'un flux ne peut être parcouru qu'**une seule fois**. La [documentation de `Stream`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Stream.html) dit qu'« un flux ne doit être opéré qu'une seule fois ». Ce qui se passe si tu l'ignores :

```java
// fig04_16.java
void main() {
  Stream<String> nombres = Stream.of("catalogo", "inventario", "pagos");
  IO.println(nombres.count());
  IO.println(nombres.count());
}
```

```text
$ java fig04_16.java
3
Exception in thread "main" java.lang.IllegalStateException: stream has already been operated upon or closed
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:260)
	at java.base/java.util.stream.ReferencePipeline.count(ReferencePipeline.java:750)
	at fig04_16.main(fig04_16.java:5)
```

Le premier appel à `count()` fonctionne (3) et consomme le flux. Le second lève `IllegalStateException: stream has already been operated upon or closed` (« le flux a déjà été opéré ou fermé »). La correction est de créer un nouveau flux chaque fois que tu en as besoin, depuis la collection : `nombres.stream()` est bon marché. C'est pourquoi les variables du cours gardent la **liste**, et que le flux se monte et se consomme dans la même expression.

#### 4.3.4 `Collectors` : rassembler le résultat de façons différentes

`toList()` couvre un cas. Pour les autres, il y a `collect`, qui reçoit un **collecteur** (*collector*) : une recette pour rassembler les éléments. La classe `Collectors` apporte les recettes les plus courantes. Le programme `fig04_17` utilise celles dont a besoin le résumé du `revisor` :

```java
// fig04_17.java
record Medicion(String nombre, String estado, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", "ARRIBA", 142),
          new Medicion("inventario", "LENTO", 2300),
          new Medicion("pagos", "ARRIBA", 87),
          new Medicion("reportes", "CAIDO", 0));

  Map<String, Long> porEstado =
      mediciones.stream()
          .collect(Collectors.groupingBy(Medicion::estado, TreeMap::new, Collectors.counting()));
  IO.println(porEstado);

  Map<String, List<String>> nombresPorEstado =
      mediciones.stream()
          .collect(
              Collectors.groupingBy(
                  Medicion::estado,
                  TreeMap::new,
                  Collectors.mapping(Medicion::nombre, Collectors.toList())));
  IO.println(nombresPorEstado);

  String todos = mediciones.stream().map(Medicion::nombre).collect(Collectors.joining(", "));
  IO.println(todos);

  OptionalDouble promedio =
      mediciones.stream()
          .filter(medicion -> !medicion.estado().equals("CAIDO"))
          .mapToLong(Medicion::tiempoMs)
          .average();
  IO.println(promedio);
  IO.println(Math.round(promedio.getAsDouble()));

  OptionalDouble sinDatos =
      mediciones.stream()
          .filter(medicion -> medicion.tiempoMs() > 9999)
          .mapToLong(Medicion::tiempoMs)
          .average();
  IO.println(sinDatos);
}
```

```text
$ java fig04_17.java
{ARRIBA=2, CAIDO=1, LENTO=1}
{ARRIBA=[catalogo, pagos], CAIDO=[reportes], LENTO=[inventario]}
catalogo, inventario, pagos, reportes
OptionalDouble[843.0]
843
OptionalDouble.empty
```

La première est la plus importante : `groupingBy(Medicion::estado, TreeMap::new, Collectors.counting())`. `groupingBy` **regroupe** les éléments par la valeur que renvoie la fonction (ici, l'état) et applique à chaque groupe un autre collecteur (ici, `counting()`, qui compte). Le résultat est une map d'état vers quantité : `{ARRIBA=2, CAIDO=1, LENTO=1}`. Sans le second argument, `groupingBy` renverrait une `HashMap` dont l'ordre n'est pas garanti ; avec `TreeMap::new` (une référence de constructeur : « appelle `new TreeMap()` quand tu auras besoin de créer la map ») le résultat sort trié alphabétiquement. Cela ressemble à la colonne « combien y en a-t-il de chaque état » du résumé du `revisor`.

La deuxième recette regroupe de la même façon mais, au lieu de compter, **garde les noms** de chaque groupe : `Collectors.mapping(Medicion::nombre, Collectors.toList())` transforme chaque élément avant de le rassembler. C'est le schéma « regroupe puis réduis chaque groupe » que tu verras avec différents réducteurs. `Collectors.joining(", ")` joint des textes avec un séparateur ; c'est ce qui compose le corps du tableau dans la section 4.5.

La dernière partie de la figure est celle qui enseigne le plus. `average()` calcule la moyenne d'un flux de `long` et renvoie un `OptionalDouble`, pas un `double`. Il s'imprime comme `OptionalDouble[843.0]`, et `getAsDouble()` en sort le nombre. Quand le flux est vide (le dernier cas, où le filtre ne laisse passer personne), la moyenne **n'existe pas** et le résultat est `OptionalDouble.empty`. C'est une décision intelligente : d'autres langages renverraient zéro ou `NaN` (un nombre qui veut dire « n'est pas un nombre »), et un zéro mentirait, parce que la moyenne de rien n'est pas zéro. Java t'oblige à décider quoi faire quand il n'y a pas de données. La section 4.4 explique `Optional` en entier.

Un détail sur `Math.round(843.0)` : il arrondit un `double` à l'entier le plus proche et renvoie un `long`. (Si tu veux la moyenne sans arrondi, laisse le `double`.) Pour le `revisor`, qui affiche des millisecondes entières, l'arrondi est ce qu'il faut.

#### 4.3.5 Trier : `Comparator`

Trier suppose de savoir **comparer** deux éléments. Pour les textes et les nombres, il y a un ordre naturel (l'alphabétique, le numérique) et `sorted()` sans arguments l'utilise. Pour tes propres types, c'est toi qui dis comment comparer avec un `Comparator`, et le JDK apporte une boîte à outils pour les monter sans écrire la méthode `compare` à la main. Le programme `fig04_18` les met en œuvre avec quatre mesures, deux d'entre elles ayant le même temps pour que l'on voie le départage :

```java
// fig04_18.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("pagos", 87),
          new Medicion("catalogo", 142),
          new Medicion("busqueda", 142),
          new Medicion("inventario", 2300));

  Comparator<Medicion> porTiempo = Comparator.comparingLong(Medicion::tiempoMs);

  List<Medicion> ascendente = mediciones.stream().sorted(porTiempo).toList();
  List<Medicion> descendente = mediciones.stream().sorted(porTiempo.reversed()).toList();
  List<Medicion> desempatado =
      mediciones.stream().sorted(porTiempo.thenComparing(Medicion::nombre)).toList();

  ascendente.forEach(medicion -> IO.println("asc  " + medicion));
  descendente.forEach(medicion -> IO.println("desc " + medicion));
  desempatado.forEach(medicion -> IO.println("tiempo y nombre " + medicion));

  Optional<Medicion> masLenta = mediciones.stream().max(porTiempo);
  IO.println(masLenta);
  IO.println(mediciones);
}
```

```text
$ java fig04_18.java
asc  Medicion[nombre=pagos, tiempoMs=87]
asc  Medicion[nombre=catalogo, tiempoMs=142]
asc  Medicion[nombre=busqueda, tiempoMs=142]
asc  Medicion[nombre=inventario, tiempoMs=2300]
desc Medicion[nombre=inventario, tiempoMs=2300]
desc Medicion[nombre=catalogo, tiempoMs=142]
desc Medicion[nombre=busqueda, tiempoMs=142]
desc Medicion[nombre=pagos, tiempoMs=87]
tiempo y nombre Medicion[nombre=pagos, tiempoMs=87]
tiempo y nombre Medicion[nombre=busqueda, tiempoMs=142]
tiempo y nombre Medicion[nombre=catalogo, tiempoMs=142]
tiempo y nombre Medicion[nombre=inventario, tiempoMs=2300]
Optional[Medicion[nombre=inventario, tiempoMs=2300]]
[Medicion[nombre=pagos, tiempoMs=87], Medicion[nombre=catalogo, tiempoMs=142], Medicion[nombre=busqueda, tiempoMs=142], Medicion[nombre=inventario, tiempoMs=2300]]
```

Ce que l'on a lu dans la sortie :

- `Comparator.comparingLong(Medicion::tiempoMs)` construit un comparateur à partir d'une fonction qui extrait la valeur à comparer. Il existe des versions pour `comparing` (n'importe quel type avec un ordre naturel), `comparingInt`, `comparingLong` et `comparingDouble`.
- `.reversed()` inverse l'ordre. `.thenComparing(...)` ajoute un **départage** : il ne s'utilise que lorsque les deux éléments sont à égalité sur le premier critère. C'est pourquoi, dans le troisième bloc, `busqueda` se retrouve avant `catalogo` : ils ont les mêmes 142 ms et le départage les ordonne alphabétiquement.
- Sans départage, dans les deux premiers blocs `catalogo` reste avant `busqueda` dans les deux sens. Ce n'est pas un hasard : la [documentation de `Stream.sorted`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Stream.html) garantit que « pour les flux ordonnés, le tri est stable » : les éléments à égalité conservent l'ordre qu'ils avaient.
- **`sorted` ne touche pas à la liste d'origine.** Les dernières lignes le prouvent : `mediciones` reste dans l'ordre où elle a été créée. Si tu veux trier la liste elle-même, `lista.sort(comparador)` le fait sur place, mais cela ne fonctionne qu'avec une liste modifiable : sur une `List.of`, cela lève de nouveau `UnsupportedOperationException`.
- `max(porTiempo)` renvoie un `Optional<Medicion>` et non une `Medicion`, et s'imprime comme `Optional[Medicion[nombre=inventario, tiempoMs=2300]]`. Demander le maximum d'une liste vide n'a pas de réponse, et le type le dit.

Un `forEach` est apparu deux fois dans le programme : `ascendente.forEach(medicion -> IO.println(...))`. Il reçoit une lambda qui ne renvoie rien (un `Consumer`, l'interface fonctionnelle du « fais ceci avec chaque élément ») et il est disponible sur les listes et sur les flux. Pour imprimer, c'est bien ; pour remplir une autre collection, non (voir « Ce qui se fait de travers »).

#### 4.3.6 Flux ou boucle ?

Le mauvais message sur les *streams* est qu'ils sont « meilleurs ». Ils ne le sont pas : c'est un autre outil. Préfère un **flux** quand tu transformes ou résumes une collection (filtrer, convertir, compter, regrouper, trier), parce que le code dit ce qu'on veut. Préfère une **boucle** quand tu as besoin de sortir plus tôt avec `break`, de tenir un indice, de modifier quelque chose à l'extérieur, ou quand le corps peut lever une exception contrôlée (une lambda ne peut pas lever plus d'exceptions contrôlées que celles que déclare son interface fonctionnelle, et la `Function` que reçoit `map` n'en déclare aucune ; la leçon 5 l'explique, et c'est pourquoi `revisarTodos` redeviendra un `for` à ce moment-là : ce n'est pas un recul, c'est l'outil adapté). Et une règle de lecture qui évite des discussions : si un flux ne tient plus en six ou sept lignes, ou s'il a une lambda de plusieurs lignes, extrais cette lambda dans une méthode nommée et utilise la référence (`Revision::revisar` se lit mieux que cinq lignes à l'intérieur d'un `map`).

### 4.4 `Optional` : une valeur qui peut ne pas être là

Le `revisor` a un problème de représentation qui n'a cessé de se répéter : que renvoie une fonction quand il n'y a pas de valeur ? `Map.get("reportes")` renvoyait `null` ; un tombé n'a pas de temps de réponse ; la moyenne de personne n'existe pas. La leçon 2 a montré le coût de `null` : le compilateur ne le distingue pas d'une valeur, et l'erreur apparaît loin, sous forme de `NullPointerException`. `Optional<T>` est la réponse du JDK : une petite boîte qui **a une valeur ou est vide**, et dont le type avertit celui qui la reçoit qu'il doit décider quoi faire dans les deux cas.

Le programme `fig04_19` parcourt le vocabulaire :

```java
// fig04_19.java
Optional<Long> tiempoDe(Map<String, Long> tiempos, String nombre) {
  return Optional.ofNullable(tiempos.get(nombre));
}

void main() {
  Map<String, Long> tiempos = Map.of("catalogo", 142L, "pagos", 87L);

  Optional<Long> hay = tiempoDe(tiempos, "pagos");
  Optional<Long> noHay = tiempoDe(tiempos, "reportes");
  IO.println(hay);
  IO.println(noHay);
  IO.println(hay.isPresent() + " " + noHay.isEmpty());

  IO.println(hay.orElse(0L));
  IO.println(noHay.orElse(0L));
  IO.println(hay.map(tiempo -> tiempo + " ms").orElse("sin tiempo"));
  IO.println(noHay.map(tiempo -> tiempo + " ms").orElse("sin tiempo"));
  hay.ifPresent(tiempo -> IO.println("pagos respondió en " + tiempo));
  noHay.ifPresent(tiempo -> IO.println("esto no se imprime"));

  OptionalLong mayor = tiempos.values().stream().mapToLong(Long::longValue).max();
  IO.println(mayor);
  IO.println(mayor.getAsLong());
}
```

```text
$ java fig04_19.java
Optional[87]
Optional.empty
true true
87
0
87 ms
sin tiempo
pagos respondió en 87
OptionalLong[142]
142
```

Les trois façons de **créer** un `Optional` : `Optional.of(valor)` (la valeur ne peut pas être `null` ; si elle l'est, il lève `NullPointerException`), `Optional.ofNullable(valor)` (si la valeur est `null`, il reste vide ; c'est celle qu'utilise `tiempoDe` pour adapter un `get` qui pouvait renvoyer `null`) et `Optional.empty()`. Les façons de le **consommer** ; la dernière est celle qu'il convient d'éviter :

- `orElse(porOmision)` renvoie la valeur, ou celle que tu donnes s'il est vide. `orElseGet(proveedor)` fait la même chose mais reçoit une lambda qui ne s'exécute que si c'est nécessaire. `orElseThrow()` lève une exception s'il est vide (la leçon 5 explique lesquelles).
- `map(función)` transforme la valeur si elle existe et laisse le vide tel quel : `hay.map(tiempo -> tiempo + " ms")` produit un `Optional<String>` avec « 87 ms » dedans, que `orElse` déballe ; avec le vide, `map` ne fait rien, le résultat reste vide et `orElse` donne « sin tiempo » (« sans temps »). C'est ce qui évite d'écrire `if (x != null)` partout.
- `ifPresent(acción)` exécute la lambda seulement s'il y a une valeur ; `isPresent()` et `isEmpty()` interrogent.
- `get()` en sort la valeur, **et lève une exception s'il n'y en a pas**. Il existe, mais c'est la porte d'entrée vers le même problème qu'`Optional` voulait résoudre.

Voyons le coût de `get()` sans vérifier :

```java
// fig04_20.java
void main() {
  Optional<String> nombre = Optional.empty();
  IO.println("a punto de pedir el valor");
  IO.println(nombre.get());
}
```

```text
$ java fig04_20.java
a punto de pedir el valor
Exception in thread "main" java.util.NoSuchElementException: No value present
	at java.base/java.util.Optional.get(Optional.java:143)
	at fig04_20.main(fig04_20.java:5)
```

`NoSuchElementException: No value present` (« pas de valeur »). L'erreur est honnête et c'est la même que la `NullPointerException` de toujours, sous un autre nom : quelqu'un a supposé qu'il y avait une valeur. Désormais, avec la différence que le type `Optional` a rendu le risque évident à l'endroit exact où la décision a été prise. La règle pratique : **n'appelle pas `get()` à moins de l'avoir vérifié dans le même bloc**, et presque toujours il y a un `orElse` ou un `map` qui dit la même chose mieux.

`orElse` a aussi un détail qui coûte cher : son argument est **toujours** calculé, même si ce n'est pas nécessaire. Le programme `fig04_21` le montre avec une valeur par défaut qui prévient quand elle est calculée :

```java
// fig04_21.java
String calcular(String quien) {
  IO.println("  (calculando el valor por omisión para " + quien + ")");
  return "ninguno";
}

void main() {
  Optional<String> hay = Optional.of("catalogo");
  Optional<String> noHay = Optional.empty();

  IO.println("orElse con valor presente:");
  IO.println(hay.orElse(calcular("orElse")));
  IO.println("orElseGet con valor presente:");
  IO.println(hay.orElseGet(() -> calcular("orElseGet")));
  IO.println("orElseGet con valor ausente:");
  IO.println(noHay.orElseGet(() -> calcular("orElseGet")));
}
```

```text
$ java fig04_21.java
orElse con valor presente:
  (calculando el valor por omisión para orElse)
catalogo
orElseGet con valor presente:
catalogo
orElseGet con valor ausente:
  (calculando el valor por omisión para orElseGet)
ninguno
```

Avec la valeur présente, `orElse` a calculé la valeur par défaut malgré tout (le message « calculando... » (« calcul en cours... ») est sorti) et l'a jetée ; `orElseGet` ne l'a pas calculée. Pour un `"ninguno"` littéral, cela revient au même ; pour quelque chose de coûteux, comme une requête ou un gros objet, utilise `orElseGet`. C'est la même idée que dans la leçon 2 avec `&&` contre `&` : n'évalue pas ce dont tu n'as pas besoin.

Et la dernière partie, celle qui apporte des règles de conception. La documentation d'[`Optional`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html) dit, dans une note d'API, qu'il « est principalement conçu pour être utilisé comme type de retour d'une méthode où il y a un besoin clair de représenter *aucun résultat*, et où utiliser `null` causera probablement des erreurs ». De là sortent trois règles :

1. **Utilise-le comme type de retour**, pas comme paramètre (il oblige l'appelant à envelopper une valeur qu'il a déjà) ni comme liste d'éléments (`List<Optional<T>>` est presque toujours un `filter` qui manque).
2. **Une variable `Optional` ne doit jamais être `null`** : la documentation le dit avec ces mots. Un `null` là où l'on attendait un `Optional` est pire que n'importe lequel des deux séparément.
3. **Pour les primitifs, `OptionalInt`, `OptionalLong` et `OptionalDouble`** : sans enveloppes, avec `getAsLong()` au lieu de `get()`. `average()` et `max()` des flux numériques les utilisent, et le `revisor` aussi, pour le temps.

Le projet enfreint la règle du « pas comme champ » à un endroit, et il vaut mieux le dire franchement : le *record* `Resumen` de la section 4.5 garde un `OptionalLong promedioMs`. C'est un *record* immuable, qui se crée une fois et se lit plusieurs fois ; la valeur absente fait partie des données (quand personne n'a répondu, il n'y a pas de moyenne), et les alternatives sont pires : un `Long` qui peut être `null` est le problème que nous venons de laisser derrière nous, et un `0` mentirait. C'est une exception consciente à une recommandation, pas un oubli. Dans la leçon 8, tu verras comment cette valeur absente franchit la frontière vers le JSON, où le format demande bel et bien un `null`.

### 4.5 L'étape du projet : le rapport avec résumé

Maintenant, assemblons tout. La leçon 3 a laissé un `Revisor.java` qui vérifiait quatre services écrits à la main et un `simular` avec un seul `switch`, qui rassemblait les données des quatre services. Aujourd'hui le projet change ainsi :

- Les **données simulées** sortent du `switch` et vont dans une map, dans une nouvelle classe, `Simulador`. On y cherche par nom et il renvoie un `Optional`.
- **`Revision`** vérifie un service (en consultant le simulateur) et vérifie toute une liste avec un *stream*.
- **`Resumen`** compte et fait la moyenne avec `groupingBy` et `average` ; **`Reporte`** rassemble les résultats et leur résumé, avec une copie défensive.
- **`Resultado`** apprend à donner son temps comme `OptionalLong` ; **`Formato`** apprend à composer le résumé et le tableau complet.
- **`Revisor.java`** cesse d'appeler `revisar` quatre fois : il a une liste et demande le rapport d'un coup.

Quatre fichiers ne changent pas : `Estado`, `Respuesta`, `Servicio` et `Clasificador`. Je les inclus quand même parce que le dossier de la figure est le projet **complet**, et que tu peux ainsi tout exécuter depuis lui. Copie-les tels quels de la leçon 3. Commence par eux :

```java
// fig04_22/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig04_22/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig04_22/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

```java
// fig04_22/Clasificador.java
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

Une note sur `import`. Les quatre fichiers ci-dessus et ceux qui suivent sont des classes normales (pas des fichiers compacts), et c'est pourquoi ils commencent, quand ils en ont besoin, par des lignes `import java.util.List;`. Un `import` dit au compilateur d'où vient un nom qui n'est pas de `java.lang` (le paquet que Java importe tout seul, avec `String`, `Math` et `Thread`). On en écrit un par classe, sans jokers, comme l'indique le guide de style de Google que le cours suit depuis la leçon 1. Le fichier compact `Revisor.java`, en revanche, importe tout seul tout `java.base` et n'en a besoin d'aucun.

Suit `Resultado`, qui gagne une méthode. `tiempoMs()` renvoie un `OptionalLong` : avec valeur quand il y a eu réponse et vide quand il n'y en a pas eu. C'est le même `switch` exhaustif que dans la leçon 3 : si un jour quelqu'un ajoute un troisième type de `Respuesta`, le compilateur marquera cette méthode et `detalle()` comme incomplètes.

```java
// fig04_22/Resultado.java
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

`Simulador` est la classe qui joue le rôle du réseau jusqu'à la leçon 8. Son `RESPUESTAS` est une `Map` immuable (`Map.of`) de nom vers réponse, avec les trois qui répondent et celle qui ne répond pas. `buscar` enveloppe le `get` de la map dans un `Optional.ofNullable`, et c'est le seul point où l'absence d'un nom se convertit en un type qui oblige à décider.

```java
// fig04_22/Simulador.java
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

`Revision` a les deux méthodes qui produisent des résultats. `revisar` demande la réponse au simulateur et, si le nom n'y est pas, utilise un `SinRespuesta("sin conexión")` (un `orElse` avec une valeur bon marché). `revisarTodos` est la ligne qui justifie toute la leçon : un *stream* sur la liste de services, avec `Revision::revisar` comme `map` et `toList()` à la fin.

```java
// fig04_22/Revision.java
import java.util.List;

final class Revision {
  static Resultado revisar(Servicio servicio) {
    Respuesta respuesta =
        Simulador.buscar(servicio.nombre()).orElse(new Respuesta.SinRespuesta("sin conexión"));
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  static List<Resultado> revisarTodos(List<Servicio> servicios) {
    return servicios.stream().map(Revision::revisar).toList();
  }
}
```

`Resumen` est l'endroit où se rejoignent les *streams* et les collecteurs. `groupingBy` compte combien il y en a de chaque état et `average` calcule la moyenne des temps de ceux qui ont répondu. Deux détails. Un : `groupingBy` ne crée une clé que pour les états qui **apparaissent** ; si aucun service n'est tombé, `porEstado.get(Estado.CAIDO)` serait `null`. C'est pourquoi on utilise `getOrDefault(..., 0L)` : sans lui, le premier rapport sans tombés ferait chuter le programme avec la `NullPointerException` de la section 4.2.2. `intValue()` convertit le `Long` du comptage en l'`int` que demande le *record* (les comptages tiennent largement). Deux : la moyenne compte tous ceux qui ont répondu, quel que soit leur code (un 503 a répondu, même s'il a mal répondu), et c'est pourquoi elle se calcule sur `tiempoMs()` et non sur l'état.

```java
// fig04_22/Resumen.java
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

`Reporte` est le *record* de la copie défensive de la section 4.1.4. Sa fabrique `de` rassemble la liste de résultats avec son résumé :

```java
// fig04_22/Reporte.java
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

`Formato` s'enrichit de trois méthodes : `contar`, qui met un mot au pluriel (la seule règle de pluriel du cours : on ajoute un « s » si la quantité n'est pas 1), `resumen`, qui compose la ligne finale, et `tabla`, qui rassemble l'en-tête, les lignes (jointes avec `Collectors.joining("\n")`), une ligne vide et le résumé. Le `? :` de `contar` est l'**opérateur conditionnel** de la leçon 2 (2.3.1) : `cantidad == 1 ? "" : "s"` se lit « si la quantité est 1, rien ; sinon, un "s" ». (Le même opérateur apparaît dans `Resumen`, pour décider s'il y a une moyenne.) Et il y a un petit changement dans une méthode que tu avais déjà, qu'il vaut mieux dire à voix haute : `formatearTiempo` cesse de comparer à `Clasificador.UMBRAL_LENTO_MS` et compare à `1000`. Ce sont deux idées différentes qui aujourd'hui valent la même chose : le seuil de « lent » est une règle du métier, et le passage des millisecondes aux secondes est une règle d'unités. Si demain le seuil de lent descend à 800 ms, le tableau doit continuer à écrire `999ms` et non `1.0s` ; avec la constante partagée, changer une règle aurait changé l'autre sans que personne l'ait demandé. (La leçon 7 fait justement ce changement de seuil, et un test le surveille.) Le reste du fichier, `PATRON_LINEA`, `tiempo`, `encabezado` et `linea`, reste comme dans la leçon 3.

```java
// fig04_22/Formato.java
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

Et `Revisor.java` devient court. Il a deux nouveautés qui s'expliquent d'elles-mêmes : une **liste** de services (un champ du fichier compact, c'est-à-dire une variable qui est en dehors de toute méthode et que toutes les méthodes du fichier peuvent utiliser) marquée `final`, ce qui veut dire « cette variable ne peut pas être réassignée », et un `main` d'une seule ligne :

```java
// fig04_22/Revisor.java
final List<Servicio> servicios =
    List.of(
        new Servicio("catalogo", URI.create("https://catalogo.example")),
        new Servicio("inventario", URI.create("https://inventario.example")),
        new Servicio("pagos", URI.create("https://pagos.example")),
        new Servicio("reportes", URI.create("https://reportes.example")));

void main() {
  IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
}
```

Exécute le projet depuis son dossier :

```text
$ cd fig04_22
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

C'est le tableau de la leçon 3, avec ce qui lui manquait : la ligne vide et le résumé. Regarde comment le résumé se lit face au code : `4 servicios` est `contar(4, "servicio")`, `2 arriba` est le comptage de `ARRIBA`, `1 lento` et `1 caído` sont des `contar(...)` avec leurs mots, et `843ms` est le `Math.round` de 2529 / 3, formaté avec `formatearTiempo` (qui sait déjà que 843 < 1000 s'écrit avec « ms »). Quand le résumé te donne un nombre que tu n'attends pas, commence par `Resumen` : c'est là que tout se calcule.

## L'erreur que tu vas voir

L'erreur de cette leçon est `UnsupportedOperationException`, et c'est celle que presque tout le monde rencontre la première semaine avec les collections. Tu l'as déjà vue avec `List.of`. Voici la même trace, pour que tu la lises ligne par ligne :

```text
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_04.main(fig04_04.java:5)
```

**Première ligne : quoi.** `UnsupportedOperationException` est le type ; il n'apporte pas de message, parce que l'opération n'est pas supportée et qu'il n'y a rien de plus à dire. **Deuxième et troisième : où, à l'intérieur du JDK.** Elles parlent d'`ImmutableCollections`, la classe qui implémente les collections de `List.of`, `Set.of` et `Map.of` (et de `toList()`). Quand tu verras ce nom, tu sauras que tu as essayé de modifier une collection immuable. **Quatrième : où, dans ton code.** La première ligne qui mentionne ton fichier est celle que tu dois regarder.

La même erreur apparaît par d'autres chemins qu'il vaut mieux reconnaître. Elle se produit quand tu modifies ce qu'a renvoyé le `toList()` d'un flux :

```java
// fig04_23.java
void main() {
  List<String> nombres = Stream.of("catalogo", "inventario", "pagos").toList();
  IO.println(nombres);
  nombres.add("reportes");
}
```

```text
$ java fig04_23.java
[catalogo, inventario, pagos]
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_23.main(fig04_23.java:5)
```

La trace est identique, avec un autre numéro de ligne de ton programme. Et elle se produit quand tu appelles `sort` sur une liste immuable. Il y a deux solutions, selon ce que tu veux. Si tu as vraiment besoin d'une liste modifiable, rends-le explicite avec une copie : `new ArrayList<>(nombres)`. Si ce que tu voulais, c'était une liste triée, ne la trie pas sur place : utilise `stream().sorted(...).toList()` et reçois une autre liste. **La meilleure réaction à l'erreur n'est pas de rendre la liste modifiable par réflexe, mais de te demander si l'opération que tu tentes devait exister.**

## Ce qui se fait de travers

- **Modifier une liste pendant que tu la parcours avec `for`.** C'est l'erreur classique, et le piège est qu'elle n'échoue parfois même pas. Si tu retires un élément dans le `for` amélioré, la plupart du temps le programme s'arrête avec une exception :

```java
// fig04_24.java
void main() {
  List<String> nombres = new ArrayList<>(List.of("catalogo", "inventario", "pagos", "reportes"));
  for (String nombre : nombres) {
    if (nombre.startsWith("i")) {
      nombres.remove(nombre);
    }
  }
  IO.println(nombres);
}
```

```text
$ java fig04_24.java
Exception in thread "main" java.util.ConcurrentModificationException
	at java.base/java.util.ArrayList$Itr.checkForComodification(ArrayList.java:1096)
	at java.base/java.util.ArrayList$Itr.next(ArrayList.java:1050)
	at fig04_24.main(fig04_24.java:4)
```

  `ConcurrentModificationException` (« modification concurrente » ; le nom trompe, parce qu'il n'y a pas de threads en jeu : la liste a détecté qu'elle avait changé alors que le parcours était en cours). Le pire est le cas silencieux : j'ai mesuré que si l'élément que tu retires est l'**avant-dernier**, le parcours se termine sans se plaindre et saute le dernier. Avec `["catalogo", "inventario", "pagos"]` en retirant `"inventario"`, la sortie est `[catalogo, pagos]` et le programme n'a jamais regardé `pagos`. Le coût est un programme qui perd parfois un élément et ne le dit pas. La solution : `lista.removeIf(nombre -> nombre.startsWith("i"))`, qui existe justement pour cela, ou un flux avec `filter` qui produise une nouvelle liste.

- **Confondre `remove(int)` avec `remove(Object)`.** Dans une `List<Integer>`, `lista.remove(87)` interprète le 87 comme une **position**, pas comme une valeur.

```java
// fig04_25.java
void main() {
  List<Integer> tiempos = new ArrayList<>(List.of(142, 2300, 87));
  tiempos.remove(Integer.valueOf(87));
  IO.println(tiempos);
  tiempos.remove(87);
  IO.println(tiempos);
}
```

```text
$ java fig04_25.java
[142, 2300]
Exception in thread "main" java.lang.IndexOutOfBoundsException: Index 87 out of bounds for length 2
	at java.base/jdk.internal.util.Preconditions.outOfBounds(Preconditions.java:100)
	at java.base/jdk.internal.util.Preconditions.outOfBoundsCheckIndex(Preconditions.java:106)
	at java.base/jdk.internal.util.Preconditions.checkIndex(Preconditions.java:302)
	at java.base/java.util.Objects.checkIndex(Objects.java:365)
	at java.base/java.util.ArrayList.remove(ArrayList.java:552)
	at fig04_25.main(fig04_25.java:6)
```

  Le premier appel (`Integer.valueOf(87)`, qui demande de retirer la valeur) a fonctionné ; le second a cherché la position 87 dans une liste de deux éléments et a levé `IndexOutOfBoundsException`. C'est un cas où la commodité de l'*autoboxing* de la section 4.2.2 se retourne contre toi : le compilateur choisit la méthode `remove(int)` parce que `87` est un `int`. Avec des listes de textes, il n'y a pas d'ambiguïté. Quand tu devras retirer un nombre par sa valeur, enveloppe-le exprès, comme dans le premier appel.

- **Utiliser des types bruts (`List` sans `<...>`).** Comme l'a montré la figure de l'effacement, cela retire au compilateur la capacité de t'avertir, et l'erreur apparaît loin de sa cause, sous forme de `ClassCastException`. Coût : déboguer une erreur de type à l'exécution, que le compilateur t'aurait marquée à la ligne de la négligence. C'est exactement ce que signale tout avertissement `[unchecked]`.

- **Appeler `Optional.get()` sans vérifier**, ou déclarer un `Optional` comme champ ou paramètre. La documentation le déconseille, et les deux pratiques ramènent les problèmes du `null` sous un autre nom. Coût : `NoSuchElementException` en production, et des signatures qui obligent l'appelant à envelopper et déballer des valeurs sans nécessité.

- **Remplir une collection extérieure depuis un `forEach` ou un `map`.** Écrire `mediciones.stream().forEach(m -> destino.add(m.nombre()))` modifie un état externe depuis une lambda ; cela fonctionne avec un flux séquentiel et se casse de façons subtiles avec un flux parallèle. Coût : du code fragile qui ressemble à un `map` mal écrit ; l'outil correct est `collect` ou `toList()`.

- **Tenir pour acquis l'ordre d'une `HashMap`, d'un `HashSet` ou d'un `Set.of`.** Cela fonctionne sur ta machine et change sur une autre, ou à l'exécution suivante. Coût : un rapport qui sort parfois dans un autre ordre et qu'aucun test ne reproduit du premier coup. Si l'ordre compte pour qui lit, choisis `LinkedHashMap`, `TreeMap` ou une liste.

- **Renvoyer ou garder la liste interne sans la copier.** C'est le piège de la section 4.1.4 : un autre code conserve la référence et modifie tes données sans que tu t'en aperçoives. Coût : des valeurs qui changent toutes seules, le type d'erreur le plus difficile à localiser parce qu'il se produit loin de l'endroit où l'on voit l'effet.

## Exercices

### Exercice 1 — Ajoute deux services au projet

Dans ton `Revisor.java`, ajoute à la liste `facturacion` (`https://facturacion.example`) et `notificaciones` (`https://notificaciones.example`). **Avant d'exécuter**, prédis quel état ils auront et quel sera le résumé. Ensuite exécute. Puis ajoute les deux à `Simulador` (`facturacion` répond avec le code 503 en 95 ms ; `notificaciones` ne répond pas, avec le motif « tiempo agotado » (« délai dépassé »)), prédis de nouveau le résumé et exécute. Note dans ton journal de bord ce qui est arrivé à la ligne de `notificaciones`.

### Exercice 2 — Trie le rapport

Fais en sorte que le tableau sorte trié du service le plus lent au plus rapide, avec ceux qui n'ont pas répondu à la fin, et que les égalités soient départagées par le nom. Indice : `Comparator.comparingLong`, `reversed` et `thenComparing` ; le temps de qui n'a pas répondu peut se remplacer par `orElse(-1)`. Passe la liste déjà triée à `Reporte.de`.

### Exercice 3 — Laquelle lève une exception ?

Pour chaque ligne, décide sans exécuter si elle lève une exception ou non, et laquelle. Ensuite vérifie-le dans un fichier compact, en exécutant chaque ligne seule (une par exécution : la première exception arrête le programme) :

1. `List.of("catalogo", null)`
2. `Map.of("a", 1, "a", 2)`
3. `new ArrayList<>(List.of("a")).get(5)`
4. `Set.of("a").add("b")`
5. `new HashMap<String, Integer>().put(null, 1)`
6. `new TreeMap<String, Integer>().put(null, 1)`

### Exercice 4 — Les noms par état

Imprime, à partir des résultats du `revisor`, une map de chaque `Estado` vers la liste des noms de service qui ont cet état, **dans l'ordre où les états sont déclarés** (`ARRIBA`, `LENTO`, `CAIDO`). Indice : `groupingBy` accepte comme deuxième argument une fonction qui fabrique la map, et `EnumMap` est une map spécialisée pour les clés d'un `enum` qui les ordonne selon leur déclaration.

## Solutions

### Solution 1

Sans toucher à `Simulador`, les deux nouveaux services ne sont pas dans la map de réponses, donc `buscar` renvoie un `Optional` vide et `revisar` utilise le `SinRespuesta("sin conexión")` par défaut : les deux sortent `CAIDO`. Avec le changement dans `Revisor.java` (ajouter deux `new Servicio(...)` à la liste), le tableau est :

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO          —  sin conexión
notificacionesCAIDO          —  sin conexión

6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 843ms
```

Cela fait 6 services, 2 en ligne, 1 lent et 3 tombés ; la moyenne ne change pas (843 ms) parce qu'aucun des deux nouveaux ne répond. Ensuite, dans `Simulador`, on ajoute deux paires au `Map.of` (qui en accepte jusqu'à dix ; pour davantage il faudrait utiliser `Map.ofEntries`) :

```java
"facturacion", new Respuesta.Respondio(503, 95),
"notificaciones", new Respuesta.SinRespuesta("tiempo agotado"));
```

et le résultat est :

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO       95ms  503
notificacionesCAIDO          —  tiempo agotado

6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 656ms
```

`facturacion` est `CAIDO` parce que son code est 503 (≥ 400), mais elle **a bien répondu** : elle a un temps (95 ms) et compte pour la moyenne. La moyenne est maintenant (142 + 2300 + 87 + 95) / 4 = 656. C'est la différence entre « n'a pas répondu » et « a mal répondu », et la raison pour laquelle `Resumen` fait la moyenne sur `tiempoMs()` et non sur l'état.

Dans la ligne de `notificaciones`, il y a un défaut visible : le nom fait 14 caractères et la colonne, 12. `%-12s` ne tronque pas, donc l'état se retrouve collé au nom (`notificacionesCAIDO`). Le format remplit mais n'invente pas d'espace. Calculer la largeur de la colonne à partir du nom le plus long est un exercice facultatif de la leçon 10 ; pour l'instant, tu sais déjà qu'un format fixe est un contrat que les données peuvent rompre.

### Solution 2

```java
void main() {
  List<Resultado> resultados = Revision.revisarTodos(servicios);
  Comparator<Resultado> porTiempoDescendente =
      Comparator.comparingLong((Resultado resultado) -> resultado.tiempoMs().orElse(-1))
          .reversed()
          .thenComparing(resultado -> resultado.servicio().nombre());
  List<Resultado> ordenados = resultados.stream().sorted(porTiempoDescendente).toList();
  IO.println(Formato.tabla(Reporte.de(ordenados)));
}
```

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
inventario  LENTO       2.3s  200
catalogo    ARRIBA     142ms  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Remarque le type écrit à la main dans la lambda : `(Resultado resultado) -> ...`. Ce n'est pas un ornement. Si tu l'omets et écris `Comparator.comparingLong(resultado -> resultado.tiempoMs().orElse(-1)).reversed()`, le compilateur ne peut pas déduire le type de `resultado` (parce que `reversed()` s'enchaîne avant qu'il sache à quoi il sera assigné) et le prend pour `Object` :

```text
Revisor.java:4: error: cannot find symbol
      Comparator.comparingLong(resultado -> resultado.tiempoMs().orElse(-1)).reversed();
                                                     ^
  symbol:   method tiempoMs()
  location: variable resultado of type Object
```

C'est une erreur d'inférence connue : le compilateur déduit les types d'une lambda en regardant où elle va, et une chaîne de méthodes lui cache la destination. Avec le type explicite dans le premier comparateur, la chaîne fonctionne. Le `thenComparing` final n'a pas besoin de type parce qu'il sait déjà qu'il porte sur `Resultado`. Les services qui n'ont pas répondu (`reportes`) restent à la fin parce que leur `-1` est le plus petit et que l'ordre est décroissant. Avec des égalités entre deux qui n'ont pas répondu, `thenComparing` les départage par nom.

### Solution 3

| Ligne | Ce qui se passe |
|---|---|
| `List.of("catalogo", null)` | `NullPointerException` : les collections immuables n'admettent pas `null` |
| `Map.of("a", 1, "a", 2)` | `IllegalArgumentException: duplicate key: a` |
| `new ArrayList<>(List.of("a")).get(5)` | `IndexOutOfBoundsException: Index 5 out of bounds for length 1` |
| `Set.of("a").add("b")` | `UnsupportedOperationException` |
| `new HashMap<String, Integer>().put(null, 1)` | ne lève rien : une `HashMap` accepte une clé `null` et elle contient `{null=1}` |
| `new TreeMap<String, Integer>().put(null, 1)` | `NullPointerException` : une `TreeMap` doit comparer les clés et ne peut pas le faire avec `null` |

J'ai mesuré les six avec JDK 25. Deux conclusions utiles. Les collections immuables sont strictes dès la création, pas à l'usage : `List.of("catalogo", null)` échoue déjà à la création. Et le fait que `HashMap` accepte `null` comme clé et que `TreeMap` non est un exemple du fait que les implémentations d'une même interface **ne sont pas interchangeables en tout** : le contrat commun (`Map`) couvre l'essentiel, et les détails de chacune, c'est sa documentation qui les dit.

### Solution 4

```java
void main() {
  List<Resultado> resultados = Revision.revisarTodos(servicios);
  Map<Estado, List<String>> nombresPorEstado =
      resultados.stream()
          .collect(
              Collectors.groupingBy(
                  Resultado::estado,
                  () -> new EnumMap<>(Estado.class),
                  Collectors.mapping(
                      resultado -> resultado.servicio().nombre(), Collectors.toList())));
  IO.println(nombresPorEstado);
}
```

```text
{ARRIBA=[catalogo, pagos], LENTO=[inventario], CAIDO=[reportes]}
```

`groupingBy` à trois arguments : la fonction qui classe (`Resultado::estado`), la fabrique de la map (`() -> new EnumMap<>(Estado.class)`, une lambda qui ne reçoit rien et renvoie une nouvelle map) et le collecteur de chaque groupe (`mapping` extrait le nom et `toList` les rassemble). `EnumMap` reçoit la classe de l'`enum` (`Estado.class`) et parcourt ses clés dans l'ordre de la déclaration : `ARRIBA`, `LENTO`, `CAIDO`. Avec une `HashMap`, l'ordre ne serait pas garanti. `EnumMap` est fait pour les clés d'un `enum` et rend clair, pour qui lit, que ce sont bien elles.

## Comment savoir que j'ai réussi

- [ ] `java fig04_01.java` imprime la liste, `primero y último: catalogo, pagos`, et tu peux dire pourquoi `remove("pagos")` retire une valeur et non une position.
- [ ] `java fig04_04.java` se termine par `UnsupportedOperationException`, et tu peux indiquer dans la trace quelle ligne est de ton programme et lesquelles sont du JDK.
- [ ] `java fig04_05.java` imprime que le rapport naïf a changé et le sûr non, et tu sais expliquer pourquoi le second se protège avec `List.copyOf`.
- [ ] `java fig04_10.java` échoue avec une `ClassCastException` sur une ligne qui n'est pas celle qui a commis l'erreur, et tu sais ce qui l'a rendu possible (un type brut).
- [ ] `java fig04_15.java` imprime « el stream está armado; todavía no pasó nada » avant tout « miro », et tu peux expliquer pourquoi.
- [ ] Depuis le dossier du projet, `java Revisor.java` imprime le tableau de quatre services avec `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`.
- [ ] Avec les deux services de l'exercice 1 dans `Simulador`, le résumé dit `6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 656ms`, et tu sais pourquoi `facturacion` compte pour la moyenne bien qu'elle soit tombée.
- [ ] Tu peux expliquer pourquoi `Resumen.de` utilise `getOrDefault(..., 0L)` et ce qui se passerait sans lui.
- [ ] Tu as noté dans ton journal de bord un *stream* que tu as réécrit en boucle, ou l'inverse, et lequel se lisait le mieux.

## Résumé

Réponds sans regarder la leçon :

1. Pourquoi déclare-t-on `List<String> x = new ArrayList<>()` et non `ArrayList<String> x = ...` ?
2. Quel ordre de parcours ont une `HashMap`, une `LinkedHashMap` et une `TreeMap` ? Laquelle utiliserais-tu pour un rapport ?
3. Quelles deux choses renvoient `Set.add` et `Map.put`, et à quoi servent-elles ?
4. Que lève `List.of("a").add("b")` ? Et `List.of("a", null)` ?
5. Quelle différence y a-t-il entre `List.copyOf(x)` et `Collections.unmodifiableList(x)` ?
6. Pourquoi `List<int>` n'existe-t-il pas ? Quel piège comporte la comparaison de deux `Integer` avec `==` ?
7. Que signifie que les génériques sont « effacés » ? Donne une conséquence.
8. Quelles trois parties a un *stream* ? Quand le travail s'exécute-t-il ?
9. Que se passe-t-il si tu parcours le même *stream* deux fois ?
10. Que renvoie `average()` et pourquoi pas un `double` ?
11. Quand préfères-tu une boucle à un *stream* ?
12. Pourquoi n'appelle-t-on pas `get()` sans vérifier sur un `Optional` ? Qu'utilises-tu à la place ?

## Pour aller plus loin

- [Oracle, « Java Collections Framework »](https://docs.oracle.com/en/java/javase/25/core/java-collections-framework.html) : le guide officiel des interfaces et des implémentations, avec le tableau indiquant laquelle utiliser et quand. Consulté le 7 octobre 2026.
- [Oracle, « Generics » (tutoriel Java)](https://docs.oracle.com/javase/tutorial/java/generics/index.html) : des types génériques aux jokers et à l'effacement, avec des exemples plus étendus que cette leçon. Consulté le 7 octobre 2026.
- [Oracle, « Stream API »](https://docs.oracle.com/en/java/javase/25/core/stream-api.html) : le guide des flux, avec la différence entre opérations intermédiaires et terminales et les collecteurs. Consulté le 7 octobre 2026.
- [Documentation d'`Optional`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html) : elle inclut la note d'API sur le moment de l'utiliser. Consulté le 7 octobre 2026.

---

**Précédente :** [Leçon 3 — Records, enums et types scellés](03-records-enums-sellados.md) ·
**Suivante :** [Leçon 5 — Erreurs et exceptions](05-errores-excepciones.md)
