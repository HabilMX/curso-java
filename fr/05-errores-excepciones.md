# Leçon 5 — Erreurs et exceptions

**Durée :** 90 min (juste : la leçon a quatre idées et un projet, et il vaut mieux ne pas la presser)

**Ce que tu construis :** la lecture de la configuration du `revisor` : la liste de services cesse d'être écrite dans le code et se lit dans un fichier `servicios.txt`, avec un message clair pour chaque façon dont le fichier peut être incorrect

**Ce que tu apprends :** exceptions contrôlées et non contrôlées ; `try`, `catch` et `finally` ; `try-with-resources` ; exceptions personnalisées avec leur cause ; quand un résultat vaut mieux qu'une exception ; et que faire d'une `InterruptedException`

**D'où tu viens.** Tu apportes le projet de la [Leçon 4](04-colecciones-genericos-streams.md) : un `revisor` avec son `Simulador`, un `Resumen` qui compte avec des *streams*, un `Reporte` avec copie défensive, et un tableau avec sa ligne de résumé. Il fonctionne, mais il a deux failles de fond. La première est celle que t'a laissée l'exercice de projet de la leçon 4 : pour ajouter un service, il faut **ouvrir le code Java, le modifier et recompiler** ; les données et le programme changent pour des raisons différentes et ne devraient pas vivre ensemble. La seconde est plus subtile : la vérification est instantanée, parce que le simulateur renvoie le temps sans l'attendre, et c'est pourquoi tu n'as aucun moyen de voir comment le programme se comporte quand une vérification **prend du temps**. Cette leçon corrige les deux : les données partent dans un fichier que le programme lit et dont il se méfie, et la simulation apprend à attendre ce que dit son temps. Tu travailles dans ton dossier `~/curso-java/revisor/` et tu exécutes tout avec `java`.

**Ce que cette leçon ne fait pas.** Elle ne consulte aucun réseau : elle continue d'utiliser les données simulées, et cela ne change pas avant la [Leçon 8](08-http-y-json.md). Elle n'utilise ni Maven ni tests : Maven arrive dans la [Leçon 6](06-proyecto-maven.md) et les tests dans la [Leçon 7](07-pruebas-junit.md). Et le `revisor` n'a pas encore d'options de ligne de commande ni de code de sortie : aujourd'hui le nom du fichier est écrit dans le programme, et quand quelque chose échoue, le programme imprime le motif et se termine avec le code 0. C'est un défaut que corrige la [Leçon 10](10-el-programa-terminado.md), et cette leçon le signale quand tu le verras.

**À propos des exécutions.** Pour exécuter une figure, entre dans le dossier de la leçon dans le dépôt (`programas/05-errores-excepciones/`) et lance `java fig05_01.java`, avec le numéro de la figure que tu veux. Celles qui vivent dans un dossier à elles (celle de la section 5.3 et celle du projet de la 5.6) s'exécutent depuis l'intérieur. Toutes les sorties ont été obtenues avec Java 25 (Temurin 25.0.4) et le terminal en UTF-8. Les traces d'erreur contiennent des lignes du JDK lui-même (`at java.base/...`) avec des numéros de ligne qui changent d'une mise à jour à l'autre : il est normal que les tiens diffèrent sur ces numéros.

## À la fin, tu seras capable de

- Dire d'un type d'exception s'il est contrôlé ou non contrôlé, et ce que le compilateur impose dans chaque cas.
- Écrire un `try` avec `catch` et `finally`, en n'attrapant que l'exception que tu sais gérer et dans l'ordre que le compilateur accepte.
- Ouvrir un fichier texte avec `try-with-resources` et expliquer ce que cette forme garantit face à une fermeture manuelle.
- Définir une exception personnalisée qui conserve la cause d'origine, et lire une trace avec son `Caused by`.
- Décider si une défaillance de ton programme se représente mieux par une exception ou par une valeur, et défendre la décision.
- Gérer une `InterruptedException` sans l'avaler : la propager, ou restaurer l'indicateur d'interruption.

## Le pourquoi avant le comment

Jusqu'à la leçon 4, chaque donnée du `revisor` a été écrite par toi, dans le même fichier que le code. Si elle était erronée, tu le savais aussitôt, parce que le compilateur ne te laissait pas passer. Un fichier de configuration change cette situation du tout au tout : il est écrit par quelqu'un d'autre, à un autre moment, avec un autre éditeur, et le programme apprend ses erreurs **à l'exécution**. Un fichier extérieur peut échouer de quatre façons, et les quatre sont normales, pas exceptionnelles :

1. **Il n'existe pas**, ou il existe dans un autre dossier que celui que tu crois.
2. **Il existe mais ne peut pas être lu**, parce qu'il est dans un autre encodage ou que quelqu'un l'a laissé sans permissions.
3. **Il est complet dans la forme mais dit quelque chose d'absurde** : une adresse qui ne commence pas par `http`, un délai maximal négatif, deux services avec le même nom.
4. **Il est vide de contenu utile** : seulement des commentaires.

Un programme sérieux distingue ces cas et dit à la personne **lequel lui est arrivé et comment le corriger**, en une ligne, pas avec un rapport de trente lignes écrit pour des programmeurs. C'est ce que fait cette leçon.

Mais le `revisor` a, en plus, une autre classe de défaillance qui ne ressemble à aucune des précédentes : un **service** qui ne répond pas. Vérifier un service tombé n'est pas une erreur du programme ; c'est justement ce pour quoi il existe. Si le programme traitait un service tombé comme une urgence, un seul tombé ferait chuter le rapport des quatre-vingt-dix-neuf autres. Tu as déjà pris cette décision dans la leçon 3, sans l'appeler ainsi : `SinRespuesta` est une **valeur**, pas une exception. Cette leçon l'explique et la défend, parce que comprendre quand quelque chose est une exception et quand c'est une valeur est la chose la plus importante que Java ait à t'apprendre sur les erreurs.

Un commentaire pour ceux qui viennent d'autres langages. En Go, l'erreur est une valeur que les fonctions renvoient et qui se vérifie avec `if err != nil`. En Rust, c'est un `Result`. En Python et TypeScript, c'est une exception qui remonte jusqu'à ce que quelqu'un l'attrape. Java fait les deux : il a des exceptions, et il a des valeurs comme les `Optional` et les *sealed* de la leçon 3. Et il ajoute quelque chose que les autres n'ont pas : le compilateur distingue **deux classes d'exceptions**, et il oblige l'une d'elles à être traitée. C'est la partie qui paraît étrange au début et qui a son sens ; la première section va par là.

Le parcours tient en une séance. La section 5.1 couvre les exceptions contrôlées et non contrôlées et la syntaxe de `try` ; la 5.2, `try-with-resources` et la lecture de fichiers ; la 5.3, les exceptions personnalisées ; la 5.4, quand préférer une valeur ; la 5.5, `InterruptedException` ; et la 5.6 rassemble tout dans le projet. Voici la destination, pour que tu l'aies sous les yeux. Exécuté depuis un autre dossier que celui du fichier de configuration, le `revisor` dira (« erreur de configuration : le fichier servicios.txt n'existe pas ») :

```text
error de configuración: no existe el archivo servicios.txt
```

Un message d'une ligne, au lieu d'une trace. Pour y arriver, il faut comprendre d'abord ce qu'est une exception.

## Les concepts

### 5.1 Exceptions contrôlées et non contrôlées

#### 5.1.1 Ce qu'est une exception et quelles familles il y a

Une **exception** est un objet qui décrit que quelque chose s'est mal passé. Quand une ligne du programme ne peut pas faire ce qu'on lui a demandé, elle **lève** une exception (avec le mot `throw`, ou parce que c'est ce que fait en interne une méthode que tu as appelée). L'exception remonte par les méthodes qui étaient en cours, de la plus récente à la plus ancienne, en cherchant quelqu'un qui l'**attrape**. Si personne ne l'attrape, le thread se termine et la JVM imprime la trace que tu connais déjà. La leçon 4 t'a appris à la lire de haut en bas ; voyons maintenant ce qu'il y a derrière.

Toutes les exceptions héritent d'une classe : `Throwable`. De là sortent deux branches. `Error` est pour les problèmes graves de la JVM elle-même (plus de mémoire, un fichier de classes corrompu), et la règle est qu'**on ne les attrape pas** : tu n'as aucun moyen de t'en remettre. `Exception` est pour ce qu'un programme raisonnable peut vouloir gérer. Et à l'intérieur d'`Exception` il y a une sous-branche spéciale, `RuntimeException` (exception d'exécution), avec laquelle Java divise le monde en deux :

- **Exceptions non contrôlées** (*unchecked*) : `RuntimeException`, `Error` et tout ce qui en hérite. Le compilateur **ne t'oblige à rien** avec elles. Ce sont les erreurs de programmation : une `NullPointerException`, une `IllegalArgumentException`, l'`UnsupportedOperationException` de la leçon 4.
- **Exceptions contrôlées** (*checked*) : toutes les autres, c'est-à-dire `Exception` et ses filles qui ne sont pas des `RuntimeException`. Le compilateur **t'oblige à les traiter**, et si tu ne le fais pas, le programme ne compile pas. C'est la définition de la [spécification du langage](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html) : les contrôlées sont toutes celles qui ne sont pas non contrôlées. (Dans le reste du cours, j'emploie « contrôlée » et « non contrôlée » ; « checked » et « unchecked » sont les noms anglais et tu les verras dans toute la documentation.)

Le programme `fig05_01` imprime la chaîne d'héritage de cinq exceptions et dit à quelle classe appartient chacune. Pour l'écrire, il faut un `for` avec une condition au lieu d'un parcours (`for (inicio; condición; paso)` : il commence à `inicio`, répète tant que la `condición` est vraie, et exécute le `paso` à la fin de chaque tour), une `Class<?>` (le type qui représente une classe ; le `?` est le joker de la leçon 4) et les méthodes `getSuperclass()`, qui donne le parent, et `isAssignableFrom`, qui répond à « ce type hérite-t-il de celui-là ? » :

```java
// fig05_01.java
void cadena(Class<?> tipo) {
  String texto = tipo.getSimpleName();
  for (Class<?> padre = tipo.getSuperclass(); padre != null; padre = padre.getSuperclass()) {
    texto = texto + " -> " + padre.getSimpleName();
  }
  boolean esExcepcion = Exception.class.isAssignableFrom(tipo);
  boolean esNoVerificada = RuntimeException.class.isAssignableFrom(tipo);
  String clase;
  if (Error.class.isAssignableFrom(tipo)) {
    clase = "error de la JVM";
  } else if (esExcepcion && !esNoVerificada) {
    clase = "verificada";
  } else {
    clase = "no verificada";
  }
  IO.println(texto);
  IO.println("    " + clase);
}

void main() {
  cadena(NoSuchFileException.class);
  cadena(InterruptedException.class);
  cadena(NumberFormatException.class);
  cadena(NullPointerException.class);
  cadena(OutOfMemoryError.class);
}
```

```text
$ java fig05_01.java
NoSuchFileException -> FileSystemException -> IOException -> Exception -> Throwable -> Object
    verificada
InterruptedException -> Exception -> Throwable -> Object
    verificada
NumberFormatException -> IllegalArgumentException -> RuntimeException -> Exception -> Throwable -> Object
    no verificada
NullPointerException -> RuntimeException -> Exception -> Throwable -> Object
    no verificada
OutOfMemoryError -> VirtualMachineError -> Error -> Throwable -> Object
    error de la JVM
```

La chaîne se lit de gauche à droite comme « est un... qui est un... ». `NoSuchFileException`, celle que Java lève quand un fichier n'existe pas, est une `FileSystemException`, qui est une `IOException`, qui est une `Exception` : contrôlée. `InterruptedException` est une fille directe d'`Exception` : contrôlée. `NumberFormatException` passe par `IllegalArgumentException` et par `RuntimeException` : non contrôlée. `NullPointerException` aussi. Et `OutOfMemoryError` est dans la branche d'`Error`, qui est d'une autre famille : tu ne l'attraperas jamais. Remarque aussi que **la contrôlée de la première ligne est une `IOException`** : toute la famille des problèmes d'entrée et de sortie (fichiers, réseau) est contrôlée. C'est la classe d'erreur que le programme ne peut pas éviter en écrivant mieux son code, parce qu'elle dépend d'un monde extérieur qui peut échouer.

#### 5.1.2 Ce qu'impose le compilateur : attrape-la ou déclare-la

Voyons ce que signifie « le compilateur t'oblige ». Ce programme lit un fichier texte et compte ses lignes. Il paraît inoffensif :

```java
// fig05_02.java
void main() {
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
  IO.println(renglones.size());
}
```

```text
$ java fig05_02.java
fig05_02.java:3: error: unreported exception IOException; must be caught or declared to be thrown
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
                                             ^
1 error
error: compilation failed
```

`unreported exception IOException; must be caught or declared to be thrown` : « exception `IOException` non signalée ; elle doit être attrapée ou déclarée ». La flèche `^` pointe l'appel à `readAllLines`, celui qui peut la lever. Pour que le programme compile, tu as **deux options**, et l'une des deux est obligatoire :

1. **L'attraper** là où tu peux la gérer, avec `try` et `catch` (la section suivante).
2. **La déclarer** : avertir dans la signature de la méthode qu'elle peut la lever, avec `throws`. Par exemple `void main() throws IOException`. La déclarer ne gère pas l'erreur ; cela la **transmet** à qui appelle la méthode, qui à son tour devra l'attraper ou la déclarer. L'exception remonte jusqu'à ce que quelqu'un l'attrape ; si elle arrive à `main` et que `main` la déclare aussi, la JVM imprime la trace et le programme meurt.

L'idée derrière la règle est que certaines défaillances ne sont pas des erreurs du programmeur mais de la situation (le fichier a pu ne pas exister), et qu'il serait injuste que le code les ignore par négligence. Le compilateur convertit le « j'ai oublié que ceci peut échouer » en erreur de compilation. Si cela te paraît une charge, réserve ton jugement jusqu'à la section 5.4 : c'est un outil avec un usage précis.

#### 5.1.3 `try`, `catch` et `finally`

Attraper une exception, c'est dire : « essaie ceci ; si cela échoue de *cette* manière, fais *autre chose* au lieu de mourir ». Cela s'écrit avec `try` (essaie), `catch` (attrape) et, facultativement, `finally` (finalement). Le programme `fig05_03` convertit un texte en nombre, ce dont la configuration a besoin pour lire un `timeout_ms` :

```java
// fig05_03.java
int leerTimeout(String texto) {
  try {
    int valor = Integer.parseInt(texto);
    IO.println("  convertí " + valor);
    return valor;
  } catch (NumberFormatException e) {
    IO.println("  no sirve: " + e.getMessage());
    return -1;
  } finally {
    IO.println("  (fin de la conversión)");
  }
}

void main() {
  IO.println("leyendo 5000");
  IO.println("  resultado: " + leerTimeout("5000"));
  IO.println("leyendo 5 s");
  IO.println("  resultado: " + leerTimeout("5 s"));
}
```

```text
$ java fig05_03.java
leyendo 5000
  convertí 5000
  (fin de la conversión)
  resultado: 5000
leyendo 5 s
  no sirve: For input string: "5 s"
  (fin de la conversión)
  resultado: -1
```

`Integer.parseInt(texto)` convertit un texte en `int`. Avec « 5000 » cela fonctionne ; avec « 5 s » il lève `NumberFormatException` avec le message `For input string: "5 s"` (le texte qu'il n'a pas pu convertir). Le programme imprime en espagnol « leyendo 5000 » (« lecture de 5000 »), « convertí 5000 » (« j'ai converti 5000 »), « (fin de la conversión) » (« (fin de la conversion) »), « resultado » (« résultat ») et « no sirve » (« ne convient pas »). Les trois blocs :

- **`try { ... }`** contient la ligne qui peut échouer, et de préférence **seulement celle-là** et celles qui en dépendent. S'il n'y a pas d'erreur, le programme continue jusqu'à la fin du bloc. S'il y en a une, Java abandonne le `try` à cette même ligne et saute au `catch` qui correspond.
- **`catch (NumberFormatException e) { ... }`** s'exécute si l'erreur était de ce type. Le `e` est l'exception attrapée : `e.getMessage()` donne son message. Tout autre type d'exception passerait son chemin.
- **`finally { ... }`** s'exécute à la sortie du `try`, quelle que soit la façon dont on en sort : qu'il y ait eu une erreur ou non, et même si le `try` ou le `catch` ont fait `return`. La seule exception est que la JVM s'arrête net pendant qu'elle est dans le `try` ou le `catch` : un appel à `System.exit` (que la leçon 10 explique), un plantage du processus ou un arrêt forcé. Regarde la sortie : avec « 5000 », « convertí 5000 » s'imprime, puis le `finally`, et **après** seulement le résultat arrive à l'appelant, bien que le `return valor` apparaisse avant dans le code. Il sert à nettoyer. La [documentation d'Oracle](https://docs.oracle.com/javase/tutorial/essential/exceptions/finally.html) le résume : « le bloc `finally` s'exécute toujours quand le `try` se termine », et la même page note cette exception.

Une note de forme : renvoyer `-1` pour dire « non valide » est une mauvaise habitude, parce que c'est une valeur magique (rien n'empêche quelqu'un de l'utiliser comme si c'était un vrai temps). Il n'est là que parce que l'exemple est minimal ; dans la section 5.3, le `catch` fera quelque chose de mieux.

Il y a trois règles pour bien attraper, et les trois sont enfreintes si souvent qu'il vaut la peine de les écrire :

1. **Attrape l'exception concrète que tu sais gérer.** Un `catch (Exception e)` attrape aussi la `NullPointerException` produite par une faute de frappe, et le programme continuera de fonctionner avec des données erronées. Mieux vaut qu'il s'arrête avec sa trace.
2. **Si tu ne sais pas quoi faire d'une erreur, ne l'attrape pas.** Laisse-la remonter. Un programme qui s'arrête avec une trace claire vaut mieux qu'un programme qui continue à l'aveugle.
3. **Un seul `catch` peut couvrir deux types si la réponse est la même**, avec une barre verticale : `catch (NumberFormatException | NullPointerException e)`. Les types ne peuvent pas être l'un une sous-classe de l'autre.

L'**ordre** des `catch` compte, et le compilateur y veille. Si le plus général vient en premier, les autres ne pourraient jamais s'exécuter :

```java
// fig05_04.java
void main() {
  try {
    IO.println(Integer.parseInt("5 s"));
  } catch (Exception e) {
    IO.println("algo falló");
  } catch (NumberFormatException e) {
    IO.println("no es un número");
  }
}
```

```text
$ java fig05_04.java
fig05_04.java:7: error: exception NumberFormatException has already been caught
  } catch (NumberFormatException e) {
    ^
1 error
error: compilation failed
```

`exception NumberFormatException has already been caught` : « a déjà été attrapée ». Le premier `catch (Exception e)` avale tout, y compris la `NumberFormatException`, et le second bloc serait du code mort. La règle : **du particulier au général**, comme quand le `switch` de la leçon 3 demandait d'ordonner les cas.

#### 5.1.4 Pourquoi une contrôlée ne traverse pas la lambda d'un `map`

Il y a un endroit où la règle des contrôlées se ressent comme une gêne, et il vaut mieux le connaître maintenant parce que tu t'y heurteras dans le `revisor`. C'est à l'intérieur d'une lambda. Ce programme essaie d'attendre un peu à l'intérieur d'un `map` :

```java
// fig05_05.java
void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<String> revisados =
      nombres.stream()
          .map(
              nombre -> {
                Thread.sleep(10);
                return nombre + " revisado";
              })
          .toList();
  IO.println(revisados);
}
```

```text
$ java fig05_05.java
fig05_05.java:8: error: unreported exception InterruptedException; must be caught or declared to be thrown
                Thread.sleep(10);
                            ^
1 error
error: compilation failed
```

La même erreur que tout à l'heure, mais maintenant dans la lambda : `Thread.sleep` lève `InterruptedException` (contrôlée) et la lambda du `map` ne la déclare ni ne l'attrape. La raison est que `map` attend une `Function`, l'interface fonctionnelle de la leçon 4, et que la méthode `apply` d'une `Function` **ne déclare aucune exception contrôlée** dans sa signature. Une lambda ne peut pas lever plus que ce que son interface déclare. (Avec d'autres interfaces, elle le peut : la méthode `call` de `Callable` déclare `throws Exception`, et dans la leçon 9 tu vas en profiter.) Les issues sont au nombre de trois, et aucune n'est agréable : attraper l'exception dans la lambda (le corps se remplit de `try`), l'envelopper dans une non contrôlée (on perd l'avantage de la contrôlée), ou **ne pas utiliser de *stream* ici et revenir à une boucle `for`**, où la méthode qui la contient peut, elle, déclarer le `throws`. Dans le projet d'aujourd'hui nous prendrons cette dernière : `revisarTodos` passe de *stream* à `for`. Ce n'est pas un recul, c'est choisir l'outil qui permet ce que le problème demande.

Si tu préfères une référence de méthode (`.map(Revision::revisar)`), le message change mais la cause est la même : `incompatible thrown types InterruptedException in functional expression`. L'exercice 4 te demande de le provoquer.

#### 5.1.5 Quand contrôlée et quand non ?

Quand tu conçois tes propres exceptions, tu dois décider de quelle famille elles sont, et le guide d'Oracle donne une règle d'une phrase : [« si l'on peut raisonnablement attendre de l'appelant qu'il se remette de l'exception, fais-la contrôlée ; s'il ne peut rien faire pour s'en remettre, fais-la non contrôlée »](https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html). Un fichier de configuration qui n'existe pas est un bon exemple de la première : l'appelant peut faire quelque chose d'utile (indiquer à l'utilisateur comment le corriger). Un `null` là où il n'aurait jamais dû y en avoir est l'exemple de la seconde : il n'y a rien à « récupérer », il y a un défaut à corriger.

Il est honnête de dire que cette décision de Java est discutée. Les contrôlées imposent une discipline utile, mais dans de grands projets elles produisent une cascade de `throws` dans toutes les signatures et de `catch` qui ne font que retransmettre, et d'autres langages de la même famille, comme C# et Kotlin, ont décidé de ne pas en avoir. Java les conserve, et ton travail n'est pas d'avoir une opinion à leur sujet mais de savoir que le compilateur les traite différemment et pourquoi.

### 5.2 `try-with-resources` : ce qui s'ouvre se ferme

#### 5.2.1 Les ressources et la forme qui les ferme toute seule

Une **ressource** est quelque chose qui s'ouvre et qu'il faut fermer : un fichier, une connexion réseau. Si tu ne la fermes pas, le système d'exploitation la maintient ouverte : avec un fichier, cela se traduit par un descripteur qui s'épuise, et avec une connexion, par un serveur qui continue d'attendre. La façon habituelle de bien la fermer était un `try` avec un `finally` qui ferme la ressource, et elle a un défaut de famille : si le `try` lève une exception et que le `close()` du `finally` en lève une aussi, la seconde **masque** la première, et tu perds l'erreur qui comptait vraiment.

Java 7 a ajouté une forme qui règle cela : **`try-with-resources`** (*try avec ressources*). Tu mets les ressources entre parenthèses juste après le `try`, et Java les ferme **tout seul**, dans l'ordre inverse de leur ouverture, que le bloc se termine bien ou avec une exception. Une ressource est n'importe quel objet dont le type implémente l'interface `AutoCloseable`, qui a une seule méthode, `close()`. Le programme `fig05_06` le démontre avec une ressource inventée qui prévient quand elle s'ouvre et quand elle se ferme :

```java
// fig05_06.java
record Recurso(String nombre, boolean falla) implements AutoCloseable {
  Recurso {
    IO.println("abro " + nombre);
  }

  @Override
  public void close() {
    IO.println("cierro " + nombre);
    if (falla) {
      throw new IllegalStateException("no pude cerrar " + nombre);
    }
  }
}

void main() {
  try (Recurso entrada = new Recurso("entrada", false);
      Recurso salida = new Recurso("salida", false)) {
    IO.println("trabajo con " + entrada.nombre() + " y " + salida.nombre());
  }
  IO.println("--");
  try (Recurso fragil = new Recurso("frágil", true)) {
    IO.println("trabajo con " + fragil.nombre());
    throw new IllegalArgumentException("falló el trabajo");
  } catch (IllegalArgumentException e) {
    IO.println("atrapé: " + e.getMessage());
    for (Throwable suprimida : e.getSuppressed()) {
      IO.println("  suprimida: " + suprimida.getMessage());
    }
  }
}
```

```text
$ java fig05_06.java
abro entrada
abro salida
trabajo con entrada y salida
cierro salida
cierro entrada
--
abro frágil
trabajo con frágil
cierro frágil
atrapé: falló el trabajo
  suprimida: no pude cerrar frágil
```

Avant de lire la sortie, trois détails de la déclaration que tu n'avais pas encore vus. `implements AutoCloseable` est le même `implements` que dans la leçon 3 : le *record* s'engage à respecter le contrat de l'interface, qui exige une méthode `close()`. `@Override` est une **annotation** : elle dit au compilateur « cette méthode remplace celle de l'interface », et si tu te trompes de nom, cela échoue à la compilation au lieu de créer une méthode distincte sans que tu le remarques. Et `public` est un mot dont tu n'avais pas encore eu besoin : les méthodes d'une interface sont toujours publiques, et c'est pourquoi qui les implémente doit l'écrire. Dans la leçon 6, tu verras le mot en entier, avec sa signification et ses frères.

Maintenant la sortie (le programme imprime « abro » (« j'ouvre »), « cierro » (« je ferme »), « trabajo con » (« je travaille avec »), « atrapé » (« j'ai attrapé »), « suprimida » (« supprimée »), « no pude cerrar » (« je n'ai pas pu fermer »), « falló el trabajo » (« le travail a échoué »), « frágil » (« fragile »), « entrada » (« entrée ») et « salida » (« sortie »)). Dans le premier bloc, tu ouvres deux ressources (`entrada` et `salida`) et elles se ferment **à l'envers** : d'abord `salida`, ensuite `entrada`. C'est le bon ordre : ce que tu as ouvert en dernier dépend souvent de ce qui précède. Dans le second bloc, le `close()` de `frágil` lève une exception, et le corps du `try` aussi (c'est `throw new IllegalArgumentException(...)` qui la lève, ce qui est la façon d'en lever une à la main). Le résultat : le `catch` reçoit l'exception **du corps**, celle qui comptait (`falló el trabajo`), et celle qu'a levée `close()` ne se perd pas et ne la masque pas : elle reste **attachée** à la première comme exception *supprimée*, et se récupère avec `e.getSuppressed()`. Le [guide d'Oracle](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) l'explique avec un autre exemple : avec un `finally`, l'exception qui sort est celle du `finally` et celle du `try` est supprimée ; avec `try-with-resources`, c'est l'inverse, et c'est l'ordre que tu veux.

#### 5.2.2 Lire un fichier texte

Pour lire le `servicios.txt` du `revisor`, il faut trois pièces que tu n'avais pas utilisées. **`Path`** représente un chemin de fichier comme une valeur, et se crée avec `Path.of("servicios.txt")` ; `ruta.getFileName()` donne la dernière partie. **`Files`** est une classe avec des méthodes statiques pour opérer sur un `Path`. Et **`BufferedReader`** est un objet qui lit du texte ligne par ligne. `Files.newBufferedReader(ruta)` ouvre le fichier et renvoie un `BufferedReader`, et comme c'est une ressource, il va dans un `try-with-resources` :

```java
try (BufferedReader lector = Files.newBufferedReader(ruta)) {
  String renglon;
  while ((renglon = lector.readLine()) != null) {
    // ...usar el renglón...
  }
}
```

`lector.readLine()` renvoie la ligne suivante sous forme de `String`, ou `null` quand le fichier est fini. Ici, le `null` est la façon que Java a choisie dès ses premières versions pour dire « il n'y en a plus » ; `Optional` n'existait pas. Le `while` (*tant que*) est une boucle qui répète son corps tant que la condition est vraie, et la condition comporte une astuce très courante : `(renglon = lector.readLine()) != null` **assigne et compare à la fois**, parce qu'une assignation vaut ce qui a été assigné. Cela se lit « lis une ligne et garde-la dans `renglon` ; si ce qui a été lu n'est pas `null`, entre dans le corps ».

Trois choses de `Files` qu'il vaut mieux savoir, toutes tirées de la [documentation de la classe](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html) :

- **Elle lit en UTF-8.** `Files.newBufferedReader(ruta)` et `Files.readAllLines(ruta)` décodent le fichier en UTF-8, ce que tu veux pour un fichier avec des accents. (D'autres parties de Java, comme `FileReader`, utilisaient l'encodage de la machine, et c'est pourquoi un programme lisait bien sur ton ordinateur et mal sur un autre ; depuis Java 18, [JEP 400](https://openjdk.org/jeps/400), l'encodage par défaut de toute la plateforme est aussi UTF-8.)
- **Le chemin relatif se résout depuis le dossier où tu exécutes `java`**, pas depuis l'endroit où vit le fichier `.java`. `Path.of("servicios.txt")` cherche `servicios.txt` dans le dossier courant du terminal. C'est la cause de presque tous les « il ne trouve pas le fichier » que tu verras, et le projet de la section 5.6 le montrera.
- **Un fichier qui n'existe pas lève `NoSuchFileException`**, qui, comme tu l'as vu dans la chaîne d'héritage, est une `IOException` : contrôlée. Un fichier qui existe mais contient des octets qui ne sont pas de l'UTF-8 valide lève `MalformedInputException`, une autre `IOException`, à la lecture.

Une variante à garder en tête : `Files.readAllLines(ruta)` lit tout le fichier dans une liste de lignes et le ferme pour toi (tu n'as pas besoin de `try-with-resources`), et il sert pour les petits fichiers. `Files.lines(ruta)` renvoie un *stream* de lignes, mais **celui-là, il faut le fermer** : le flux maintient le fichier ouvert, et sa documentation le dit avec ces mots. Pour le `revisor`, le `BufferedReader` a un avantage décisif : tu sais à quelle ligne tu en es sans écrire un indice à part, et le numéro de ligne est ce dont a besoin un message d'erreur utile.

### 5.3 Exceptions personnalisées, avec leur cause

#### 5.3.1 Pourquoi une exception personnalisée

La configuration peut échouer pour une douzaine de raisons, et toutes atterrissent au même endroit : `main`, qui doit dire quelque chose à une personne. Si chaque raison était une exception différente du JDK, `main` devrait attraper `NoSuchFileException`, `MalformedInputException`, `URISyntaxException`, `NumberFormatException` et une demi-douzaine d'autres, et connaître les détails de chacune. Pire : si toutes étaient `IllegalArgumentException`, `main` ne pourrait pas distinguer « la configuration est mauvaise » de « mon programme a un défaut ». La solution est une exception avec un nom propre, qui signifie quelque chose du domaine : **`ErrorDeConfiguracion`**. Elle aura un seul sens (« le fichier de configuration est inutilisable »), `main` n'attrapera que celle-là, et toute autre qui s'échappe sera, à bon droit, une défaillance du programme qui doit l'arrêter avec sa trace.

Une exception personnalisée se définit avec une ligne d'**héritage**. Regarde la figure `fig05_07`, un dossier avec deux fichiers : la définition de l'exception et un programme qui l'utilise. Le mot nouveau est **`extends`** : une classe qui `extends` une autre *hérite* de tout ce que l'autre a. C'est comme `implements` mais pour une classe au lieu d'une interface : `ErrorDeConfiguracion` *est une* `Exception` et apporte toutes ses méthodes (`getMessage`, `getCause`, `printStackTrace`) sans que tu les écrives. La seule chose qu'il faut, c'est dire comment elle se construit, et pour cela on appelle le constructeur de la classe mère avec **`super`** : `super(mensaje, causa)` signifie « construis-moi comme une `Exception` avec ce message et cette cause ».

```java
// fig05_07/ErrorDeConfiguracion.java
final class ErrorDeConfiguracion extends Exception {
  ErrorDeConfiguracion(String mensaje) {
    super(mensaje);
  }

  ErrorDeConfiguracion(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
```

```java
// fig05_07/Demo.java
void leer(Path ruta) throws ErrorDeConfiguracion {
  try {
    Files.readAllLines(ruta);
  } catch (NoSuchFileException e) {
    throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e);
  } catch (IOException e) {
    throw new ErrorDeConfiguracion("no se pudo leer el archivo " + ruta.getFileName(), e);
  }
}

void main() {
  try {
    leer(Path.of("servicios-que-no-existe.txt"));
  } catch (ErrorDeConfiguracion e) {
    IO.println("mensaje: " + e.getMessage());
    IO.println("causa:   " + e.getCause());
    e.printStackTrace();
  }
}
```

Le programme `Demo` définit une méthode `leer` qui essaie de lire un fichier qui n'existe pas et, si cela échoue, lève **`ErrorDeConfiguracion` avec une cause** : `throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e)`. Le second argument, `e`, est l'exception d'origine. Les deux `catch` sont dans le bon ordre : d'abord la `NoSuchFileException` (plus particulière), ensuite l'`IOException` générale. Exécute la figure depuis son dossier :

```text
$ cd fig05_07
$ java Demo.java
mensaje: no existe el archivo servicios-que-no-existe.txt
causa:   java.nio.file.NoSuchFileException: servicios-que-no-existe.txt
ErrorDeConfiguracion: no existe el archivo servicios-que-no-existe.txt
	at Demo.leer(Demo.java:6)
	at Demo.main(Demo.java:14)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.execute(SourceLauncher.java:256)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.run(SourceLauncher.java:138)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.main(SourceLauncher.java:76)
Caused by: java.nio.file.NoSuchFileException: servicios-que-no-existe.txt
	at java.base/sun.nio.fs.UnixException.translateToIOException(UnixException.java:92)
	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:106)
	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:111)
	at java.base/sun.nio.fs.UnixFileSystemProvider.newFileChannel(UnixFileSystemProvider.java:213)
	at java.base/sun.nio.fs.UnixFileSystemProvider.newByteChannel(UnixFileSystemProvider.java:244)
	at java.base/java.nio.file.Files.newByteChannel(Files.java:357)
	at java.base/java.nio.file.Files.newByteChannel(Files.java:399)
	at java.base/java.nio.file.spi.FileSystemProvider.newInputStream(FileSystemProvider.java:371)
	at java.base/java.nio.file.Files.newInputStream(Files.java:154)
	at java.base/java.nio.file.Files.newBufferedReader(Files.java:2645)
	at java.base/java.nio.file.Files.readAllLines(Files.java:3085)
	at java.base/java.nio.file.Files.readAllLines(Files.java:3122)
	at Demo.leer(Demo.java:4)
	... 6 more
```

Lis la sortie calmement, parce que c'est la première trace en **deux** parties. Les deux premières lignes sont celles qu'a imprimées le programme : `mensaje:` (« message : ») est ce que tu dirais à la personne (« no existe el archivo servicios-que-no-existe.txt », c'est-à-dire « le fichier servicios-que-no-existe.txt n'existe pas »), et `causa:` (« cause : ») est ce qui s'est **vraiment** passé, avec son type complet : `java.nio.file.NoSuchFileException: servicios-que-no-existe.txt`. Le reste est ce qu'imprime `e.printStackTrace()`, qui écrit la trace complète sur la sortie d'erreur. Elle a deux blocs. Le premier est l'exception que tu as attrapée, avec sa trace : `Demo.leer(Demo.java:6)` est la ligne du `throw`, et `Demo.main(Demo.java:14)` est l'appelant. Les lignes qui suivent, de `jdk.internal.reflect` et `SourceLauncher`, sont la machinerie avec laquelle `java Archivo.java` démarre ton programme. Le second bloc commence par **`Caused by:`** (« causée par ») : c'est la cause, avec sa propre trace, qui descend jusqu'au système d'exploitation (`sun.nio.fs.UnixException...`) et remonte par `Files.readAllLines` jusqu'à la ligne `Demo.leer(Demo.java:4)`, qui est la ligne où l'on a vraiment essayé d'ouvrir le fichier. La dernière ligne, `... 6 more` (« et 6 de plus »), veut dire que les six lignes restantes sont les mêmes que celles que tu as déjà vues plus haut.

C'est cela qui donne sa valeur à la conservation de la cause. Le **message** est pour la personne qui utilise le programme : court, en espagnol, sans jargon. La **cause** est pour qui doit corriger le problème : l'exception d'origine avec sa trace complète. Perdre la cause, c'est perdre la piste. Deux règles de la maison qui en découlent :

1. **Quand tu convertis une exception en une autre, passe l'originale comme cause**, toujours.
2. **Le message dit ce qui s'est passé et quoi faire ; la cause dit pourquoi.** Dans le message du `revisor`, le texte de l'exception d'origine n'apparaît jamais, parce qu'il change selon les systèmes et les versions ; il est dans la cause.

#### 5.3.2 La pire façon de gérer une erreur : l'ignorer

S'il y a une chose à ne jamais faire avec une exception, c'est celle-ci :

```java
// fig05_08.java
int leerTimeout(String texto) {
  try {
    return Integer.parseInt(texto);
  } catch (Exception e) {
  }
  return 5000;
}

void main() {
  IO.println("timeout_ms = " + leerTimeout("2000"));
  IO.println("timeout_ms = " + leerTimeout("dos mil"));
}
```

```text
$ java fig05_08.java
timeout_ms = 2000
timeout_ms = 5000
```

La seconde ligne dit `timeout_ms = 5000` alors que la valeur écrite était « dos mil » (« deux mille »), sans le moindre signe que quelque chose s'est mal passé. Le `catch (Exception e)` **vide** a attrapé la `NumberFormatException`, n'en a rien fait et la fonction a renvoyé la valeur par défaut comme si de rien n'était. C'est l'équivalent en Java du `except: pass` de Python. La personne qui a écrit « dos mil » croit que le programme lui a obéi, et le programme fonctionne avec une autre valeur. Quand quelque chose échouera vraiment, le motif aura déjà disparu et il n'y aura aucune donnée à investiguer. Un `catch` vide est presque toujours une erreur, et il y a trois raisons pour lesquelles il vaut mieux s'en souvenir : il efface la cause, il convertit une erreur que le compilateur t'avait signalée en une que personne ne voit, et il survient justement quand on est pressé de « faire compiler ».

Si tu ne sais vraiment pas quoi faire d'une exception, ne l'attrape pas ; si tu l'attrapes, **fais quelque chose avec elle** : corrige-la, traduis-la en une autre avec cause, ou enregistre-la et laisse une trace de la raison pour laquelle tu l'ignores. Une note sur `printStackTrace()` : c'est bien pour apprendre et pour déboguer, mais ce n'est pas une façon de gérer une erreur ; il ne fait que la montrer.

#### 5.3.3 Deux canaux de sortie

Une question pratique qui surgit ici : où écrit le `revisor` quand quelque chose échoue ? Le programme a **deux canaux** de sortie de texte, que le système d'exploitation traite séparément. `System.out` est la sortie normale, et c'est là qu'écrit `IO.println`. `System.err` est la **sortie d'erreur**, pour les avertissements et les défaillances. Les deux apparaissent dans ton terminal, mais un script ou un tube peut les séparer : `java Revisor.java 2> errores.txt` envoie seulement les erreurs dans un fichier, et `java Revisor.java > reporte.txt` ne garde que le rapport. C'est pourquoi le message de configuration s'imprime avec `System.err.println(...)`. Qui utilise le programme dans un script doit pouvoir garder le rapport propre et voir l'erreur à part. `IO.println` n'a pas de version pour la sortie d'erreur, et c'est pourquoi à partir d'ici on utilise `System.out` et `System.err` explicitement quand la différence compte.

### 5.4 Une exception ou une valeur ?

Tu as déjà les deux outils pour représenter que quelque chose ne s'est pas passé comme prévu : l'**exception** et la **valeur** (un `Optional`, ou un type scellé comme `Respuesta`). Et la question de conception qui compte le plus dans la leçon : lequel utiliser quand ? Le programme `fig05_09` vérifie les trois mêmes noms des deux façons, avec un service qui ne répond pas au milieu :

```java
// fig05_09.java
long tiempoOLanza(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142;
    case "pagos" -> 87;
    default -> throw new IllegalStateException(nombre + ": sin conexión");
  };
}

OptionalLong tiempoO(String nombre) {
  return switch (nombre) {
    case "catalogo" -> OptionalLong.of(142);
    case "pagos" -> OptionalLong.of(87);
    default -> OptionalLong.empty();
  };
}

void main() {
  List<String> nombres = List.of("catalogo", "reportes", "pagos");

  IO.println("con una excepción:");
  try {
    for (String nombre : nombres) {
      IO.println("  " + nombre + ": " + tiempoOLanza(nombre) + " ms");
    }
  } catch (IllegalStateException e) {
    IO.println("  se cayó el lote: " + e.getMessage());
  }

  IO.println("con un valor:");
  for (String nombre : nombres) {
    OptionalLong tiempo = tiempoO(nombre);
    String texto = tiempo.isPresent() ? tiempo.getAsLong() + " ms" : "sin respuesta";
    IO.println("  " + nombre + ": " + texto);
  }
}
```

```text
$ java fig05_09.java
con una excepción:
  catalogo: 142 ms
  se cayó el lote: reportes: sin conexión
con un valor:
  catalogo: 142 ms
  reportes: sin respuesta
  pagos: 87 ms
```

Avec l'exception, au deuxième service le programme a abandonné le parcours entier : `pagos`, qui allait parfaitement bien, n'a jamais été vérifié. Avec la valeur, chaque service a produit sa propre réponse, l'absence de l'un n'a pas affecté les autres, et le rapport est sorti complet. C'est la raison pour laquelle le `Respuesta.SinRespuesta` de la leçon 3 est une valeur. (Le programme imprime « con una excepción » (« avec une exception »), « se cayó el lote » (« le lot a chuté »), « con un valor » (« avec une valeur ») et « sin respuesta » (« sans réponse »).)

On pourrait corriger la première version en mettant un `try` dans la boucle, pour attraper chaque service séparément. Cela fonctionnerait, mais regarde ce que tu as fait : tu as converti l'exception en valeur à chaque tour, c'est-à-dire que tu as **réinventé la valeur**, avec plus de code. La différence entre les deux outils est une différence d'intention, et de là sort une règle pour décider :

| Question | Si la réponse est oui |
|---|---|
| Est-ce **l'un des résultats normaux** de l'opération ? (« ce service est tombé »). | **Une valeur.** |
| L'appelant a-t-il un chemin normal pour **continuer** avec le suivant ? (un lot de cent services). | **Une valeur.** |
| L'opération **n'a-t-elle pas pu se faire** et continuer serait inventer ? (le fichier de configuration n'existe pas). | **Une exception.** |
| Est-ce un **défaut du programme**, de celui qui l'a écrit ? (un `null` inattendu). | **Une exception non contrôlée.** |

Pour le `revisor`, donc : un service tombé est une valeur (`SinRespuesta`) parce que la question à laquelle répond le programme est « est-il en ligne ? », et « non » est une réponse. Un fichier de configuration illisible est une exception (`ErrorDeConfiguracion`), parce que sans liste de services le programme n'a rien à vérifier, et continuer serait inventer une liste. C'est une décision de conception et c'est toi qui la prends, mais elle n'est pas arbitraire : toute la leçon se réduit à te poser cette question chaque fois que quelque chose peut mal tourner.

### 5.5 `InterruptedException` : on ne l'avale pas

La dernière exception de la leçon est la plus mal comprise. Le `revisor` d'aujourd'hui va **attendre** : pour simuler qu'un service met le temps qu'indique son temps simulé, la vérification appelle `Thread.sleep(ms)`, qui arrête le thread pendant ce nombre de millisecondes. Et `Thread.sleep` déclare **`throws InterruptedException`**, une exception contrôlée. Pourquoi ?

Un programme Java peut avoir plusieurs threads d'exécution en même temps (la leçon 9 est tout entière là-dessus, et aujourd'hui il suffit de savoir qu'un *thread* est une ligne d'exécution indépendante et que ton programme en a déjà une, le thread principal). Un thread peut demander à un autre de **s'arrêter** : cela s'appelle **l'interrompre**, et c'est une demande courtoise, pas un ordre : le thread interrompu décide comment et quand s'arrêter. Mécaniquement, interrompre un thread allume un **indicateur** (*flag*) en lui (son « état d'interruption »). Si le thread dort dans un `sleep` quand cela arrive, le `sleep` se réveille aussitôt et lève `InterruptedException`. Selon [la documentation du tutoriel](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html) : une interruption est « une indication à un thread qu'il doit arrêter ce qu'il fait et faire autre chose ; c'est au programmeur de décider comment il réagit, mais il est très courant que le thread se termine ».

Elle est contrôlée pour que tu ne puisses pas l'oublier : quelqu'un a demandé à ton code de s'arrêter, et l'ignorer n'est presque jamais la bonne chose. Pour voir la mécanique sans avoir besoin de deux threads, le programme `fig05_10` s'interrompt lui-même : `Thread.currentThread()` renvoie « le thread qui exécute ce code », et `interrupt()` allume son indicateur.

```java
// fig05_10.java
void main() {
  Thread.currentThread().interrupt();
  IO.println("¿marcado como interrumpido? " + Thread.currentThread().isInterrupted());
  try {
    Thread.sleep(1000);
    IO.println("esto no se imprime");
  } catch (InterruptedException e) {
    IO.println("sleep lanzó: " + e.getMessage());
    IO.println("¿sigue marcado? " + Thread.currentThread().isInterrupted());
    Thread.currentThread().interrupt();
    IO.println("¿y después de restaurarlo? " + Thread.currentThread().isInterrupted());
  }
}
```

```text
$ java fig05_10.java
¿marcado como interrumpido? true
sleep lanzó: sleep interrupted
¿sigue marcado? false
¿y después de restaurarlo? true
```

La première ligne confirme que l'indicateur est allumé. Le `sleep(1000)` **n'a rien dormi** : il a vu l'indicateur, a levé `InterruptedException` avec le message `sleep interrupted` et, et c'est cela l'important, **a éteint l'indicateur** en la levant : la troisième ligne dit `false`. La [documentation de `Thread`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html) le dit ainsi : « l'état d'interruption du thread actuel est effacé quand cette exception est levée ». Cela crée un problème : si tu attrapes l'exception et continues comme si de rien n'était, la demande de s'arrêter **disparaît**, et personne d'autre (par exemple, le code qui a appelé le tien) ne sait que quelqu'un voulait s'arrêter. De là viennent les deux règles, et **il n'y en a pas de troisième** (le programme imprime « ¿marcado como interrumpido? » (« marqué comme interrompu ? »), « sleep lanzó » (« sleep a levé »), « ¿sigue marcado? » (« est-il toujours marqué ? ») et « ¿y después de restaurarlo? » (« et après l'avoir restauré ? »)) :

1. **Propage-la** : déclare `throws InterruptedException` et laisse-la remonter. C'est ce que fait le `revisor`.
2. **Si tu dois l'attraper**, restaure l'indicateur avant de continuer : `Thread.currentThread().interrupt()`. C'est la quatrième ligne de la figure : elle a rallumé l'indicateur (`true`) pour qui vient derrière.

Ce qu'on ne fait pas, c'est l'attraper et se taire (`catch (InterruptedException e) {}`) : c'est la version de la section 5.3.2 pour un cas où le coût est pire, parce qu'un programme qui n'obéit pas quand on lui demande de s'arrêter ne peut pas s'éteindre avec élégance. Dans la leçon 9, tu verras qui interrompt et pourquoi.

### 5.6 L'étape du projet : le `revisor` lit sa configuration

Maintenant, rassemblons tout. Le `revisor` change ainsi :

- **`servicios.txt`**, un fichier texte, remplace la liste écrite à la main. Il a un service par ligne, avec le nom, l'adresse et, facultativement, le délai maximal en millisecondes. Les lignes vides sont ignorées et celles qui commencent par `#` sont des commentaires.
- **`ErrorDeConfiguracion`** est l'exception de la section 5.3.
- **`Configuracion`** lit le fichier et le convertit en une liste de `Servicio`, et c'est là que vivent les messages d'erreur.
- **`Revision`** change : la vérification attend ce que dit le temps simulé, et c'est pourquoi `revisar` et `revisarTodos` déclarent `throws InterruptedException`. `revisarTodos` passe de *stream* à `for`, pour ce que tu as vu en 5.1.4.
- **`Revisor.java`** lit la configuration, attrape `ErrorDeConfiguracion` et l'imprime sur la sortie d'erreur.

Neuf fichiers ne changent pas par rapport à la leçon 4 : `Estado`, `Respuesta`, `Servicio`, `Resultado`, `Clasificador`, `Simulador`, `Resumen`, `Reporte` et `Formato`. Ils sont ici parce que le dossier de la figure est le projet complet. Copie-les tels quels de la leçon 4 :

```java
// fig05_11/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig05_11/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig05_11/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

```java
// fig05_11/Resultado.java
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
// fig05_11/Clasificador.java
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
// fig05_11/Simulador.java
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
// fig05_11/Resumen.java
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
// fig05_11/Reporte.java
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
// fig05_11/Formato.java
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

Le fichier de données est celui que décrit le début de cette section : le nom, l'adresse et, si tu veux, le délai maximal. Les adresses continuent de se terminer par `.example`, un domaine réservé qui ne se résout jamais : aucune requête du cours ne sort sur internet.

```text fig05_11/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

L'exception personnalisée est celle de la section 5.3.1, avec ses deux constructeurs :

```java
// fig05_11/ErrorDeConfiguracion.java
final class ErrorDeConfiguracion extends Exception {
  ErrorDeConfiguracion(String mensaje) {
    super(mensaje);
  }

  ErrorDeConfiguracion(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
```

`Configuracion` est la grosse pièce, et il vaut la peine de la lire par parties. Regarde le code complet, puis on continue :

```java
// fig05_11/Configuracion.java
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

**`leer`** est la méthode publique. Elle ouvre le fichier avec `try-with-resources` et parcourt les lignes avec le `while` de la section 5.2.2, en tenant le compte dans `numero`. Pour chaque ligne, `strip()` lui retire les espaces des extrémités ; si le résultat est vide (`isEmpty()`) ou commence par `#` (`startsWith("#")`), `continue` saute aussitôt à la ligne suivante (le mot `continue` signifie « termine ce tour de la boucle et passe au suivant »). Toute autre ligne est convertie en un `Servicio` avec `leerRenglon`. Les deux `catch` finaux convertissent les exceptions du JDK en `ErrorDeConfiguracion`, **en passant l'originale comme cause** : d'abord `NoSuchFileException`, qui donne « no existe el archivo » (« le fichier n'existe pas »), puis l'`IOException` générale, qui donne « no se pudo leer el archivo » (« impossible de lire le fichier »). Le nom du fichier sort de `ruta.getFileName().toString()`, et cela explique le message de la figure ci-dessus : il dit `servicios.txt`, pas le chemin complet. Remarque aussi que `numero` compte les lignes **du fichier**, y compris les commentaires et les vides : si l'erreur est à la ligne 4 du fichier, le message dit 4, et c'est le numéro que tu verras dans ton éditeur.

**`leerRenglon`** découpe la ligne en champs avec `renglon.split("\\s+")`. `split` reçoit une **expression régulière** (une description d'un motif de texte) et renvoie un **tableau** de textes : `\s+` signifie « un ou plusieurs espaces ou tabulations », et s'écrit avec une double barre oblique inverse parce que dans un texte Java la barre oblique inverse a un usage propre (`\n`, `\"`) et qu'il faut l'écrire deux fois pour qu'il en arrive une. Un **tableau** (`String[]`) est une séquence de taille fixe : on le consulte avec `campos[0]` (le premier) et sa taille est `campos.length`, sans parenthèses, parce que c'est une donnée et non une méthode. C'est le même type que `String[] args` que tu verras dans la leçon 6. S'il y a moins de deux champs ou plus de trois, le message dit combien il y en a : `renglón 3: se esperaban 2 o 3 campos y hay 1` (« ligne 3 : 2 ou 3 champs étaient attendus et il y en a 1 »).

**`leerUrl`** et **`leerTimeout`** sont les deux validateurs. `leerUrl` rejette ce qui ne commence pas par `http://` ou `https://` (premier filtre, avec son message) puis essaie de construire un `URI` avec `new URI(texto)`. Cette forme lève `URISyntaxException`, contrôlée, quand l'adresse est mal écrite, et `leerUrl` la convertit en `ErrorDeConfiguracion`, cette fois **sans** la passer comme cause : le message de `URISyntaxException` répète l'adresse entière, et si elle porte un utilisateur, un mot de passe ou un `token` dans la requête, toute trace de pile qui affiche la cause les publierait. Une fois l'`URI` construit, `leerUrl` exige deux choses de plus : qu'il ait un hôte (`getHost()` n'est pas `null`) et qu'il ne porte ni utilisateur ni mot de passe (`getRawUserInfo()` est `null`). Avec `http://usuario:clave@127.0.0.1:1/estado`, le message est `renglón 1: la url no puede llevar usuario ni clave` : il dit quoi corriger et ne répète pas l'adresse. (C'est la raison d'utiliser `new URI(...)` et non `URI.create(...)`, que tu utilisais dans la leçon 3 : `URI.create` enveloppe la même exception dans une non contrôlée, et ici il convient de la voir telle quelle.) `leerTimeout` convertit le texte avec `Integer.parseInt` et rejette ce qui n'est pas un entier supérieur à zéro. Remarque comment il est écrit, parce que c'est un schéma utile : le `try` tente la conversion et, si le nombre est valide, **renvoie sur-le-champ** ; si la conversion échoue, le `catch` lève l'erreur avec la `NumberFormatException` comme cause ; et si la conversion fonctionne mais que le nombre n'est pas supérieur à zéro, le `try` ne renvoie rien et le flux tombe sur la dernière ligne, qui lève le même message sans cause. Un message, deux chemins d'erreur.

**`verificarRepetidos`** utilise l'astuce de la section 4.1.2 : `vistos.add(nombre)` renvoie `false` si le nom y était déjà, et ces noms sont rassemblés dans un `LinkedHashSet` (qui conserve l'ordre dans lequel ils ont été trouvés) et `String.join(", ", repetidos)`, qui joint les éléments avec un séparateur, les liste tous dans le message : `nombres repetidos: catalogo` (« noms répétés : catalogo »). Un détail sur ce que fait cette méthode : **le programme s'arrête à la première erreur**, et ne rassemble pas toutes les erreurs du fichier pour les dire d'un coup. C'est plus simple, et suffisant pour un fichier de quatre lignes.

Maintenant `Revision`, qui change de deux façons. D'abord, `revisar` attend le temps de la réponse avec `Thread.sleep(tiempo.getAsLong())` s'il y en a un (l'`OptionalLong` de la leçon 4), et c'est pourquoi il déclare `throws InterruptedException`. Ensuite, `revisarTodos` est un `for` qui remplit une liste, parce que la lambda d'un `map` ne peut pas lever l'exception contrôlée (sa `Function` ne la déclare pas), comme on l'a vu dans la section 5.1.4 :

```java
// fig05_11/Revision.java
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

Et `Revisor.java` boucle la boucle. Il lit la configuration, vérifie et construit le rapport, et attrape `ErrorDeConfiguracion` pour l'imprimer sur la sortie d'erreur avec le préfixe `error de configuración:`. Son `main` déclare `throws InterruptedException`, qui est la règle 1 de la section 5.5 : il ne la gère pas parce qu'il ne saurait pas quoi en faire, il la laisse seulement remonter.

```java
// fig05_11/Revisor.java
void main() throws InterruptedException {
  Path ruta = Path.of("servicios.txt");
  try {
    List<Servicio> servicios = Configuracion.leer(ruta);
    IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
  } catch (ErrorDeConfiguracion e) {
    System.err.println("error de configuración: " + e.getMessage());
  }
}
```

Exécute le projet depuis son dossier. La vérification **attend maintenant** : le tableau met environ deux secondes et demie à apparaître (la somme de 142, 2300 et 87 ms ; j'ai mesuré environ trois secondes au total avec le démarrage de Java inclus). C'est ce qui se passerait en série avec de vrais services, et c'est le problème que résoudra la leçon 9.

```text
$ cd fig05_11
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Et maintenant, la destination de la leçon. Exécute le `revisor` **depuis un autre dossier**, sans passer par l'endroit où se trouve le fichier de configuration. Ensuite exécute-le avec la sortie d'erreur jetée (`2>/dev/null`, ce qui signifie « envoie le canal 2, la sortie d'erreur, dans le néant ») et demande le code de sortie de la dernière commande avec `$?` :

```text
$ java fig05_11/Revisor.java
error de configuración: no existe el archivo servicios.txt
$ java fig05_11/Revisor.java 2>/dev/null
$ echo "código de salida: $?"
código de salida: 0
```

Trois choses se sont produites. Premièrement, le programme a dit `error de configuración: no existe el archivo servicios.txt` : le `Path.of("servicios.txt")` est relatif, il s'est résolu par rapport au dossier où tu te trouves, et là le fichier n'existe pas, bien qu'il existe dans le dossier du projet. Deuxièmement, avec `2>/dev/null` **rien n'est apparu** : le message passait par le canal d'erreur, et en le jetant il ne reste aucune sortie. C'est exactement la séparation de la section 5.3.3. Troisièmement, le code de sortie était `0` : le programme a échoué, et le système d'exploitation croit que tout s'est bien passé. Pour un script qui exécute le `revisor`, c'est un mensonge, et la leçon 10 le corrige avec `System.exit`. Pour l'instant, note-le comme défaut connu. (Le programme imprime « código de salida » (« code de sortie »).)

## L'erreur que tu vas voir

L'erreur de cette leçon est celle de la section 5.1.2, et presque tout le monde la rencontre la première fois qu'il utilise une méthode d'entrée et de sortie :

```text
fig05_02.java:3: error: unreported exception IOException; must be caught or declared to be thrown
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
                                             ^
```

**Première ligne : où et quoi.** Elle apporte le fichier et la ligne (`fig05_02.java:3`), le mot `error` et le diagnostic : `unreported exception IOException`. « Non signalée » veut dire que la méthode peut lever cette exception et que personne ne l'a attrapée ni déclarée. **Deuxième et troisième : la ligne de code et la flèche.** Le `^` pointe la parenthèse de l'appel qui peut la lever, ce que tu dois regarder. **Que faire.** Il y a deux corrections, et le choix relève de la conception. Si, à cet endroit, tu sais quoi faire quand cela échoue, **attrape-la** (et fais quelque chose d'utile, pas un `catch` vide). Si tu ne le sais pas, **déclare-la avec `throws`** et laisse celui qui a appelé la gérer. Une mauvaise correction, que l'on voit parfois : déclarer `throws Exception` pour faire taire le compilateur. Cela fonctionne, et efface des signatures ce qui peut mal tourner.

La seconde erreur de la leçon n'est pas celle du compilateur : c'est le `revisor` depuis un autre dossier, et tu l'as déjà vue dans la section 5.6. Le message `no existe el archivo servicios.txt` ne te dit pas **où** il l'a cherché. Si tu tombes dessus et que le fichier existe, la première chose à vérifier est depuis quel dossier tu as exécuté `java`.

## Ce qui se fait de travers

- **Un `catch` vide**, ou avec seulement `printStackTrace()`. C'est la section 5.3.2 : l'erreur disparaît, ou s'imprime et le programme continue comme si de rien n'était. Coût : des valeurs erronées que personne ne voit, et quand enfin quelque chose échoue, une piste qui n'existe plus.

- **Attraper `Exception` ou `Throwable` au cas où.** Cela attrape aussi les défauts de programmation (une `NullPointerException`) et les convertit en un message trompeur. Coût : le vrai défaut reste enterré. Le seul endroit pour un `catch (Exception e)` est la dernière frontière du programme, pour enregistrer et se terminer avec élégance, pas pour continuer.

- **Convertir une exception en une autre sans passer la cause.** C'est écrire `throw new ErrorDeConfiguracion("el archivo no sirve")` sans le second argument. Coût : le message dit que cela a échoué et la cause, qui était ce qui permettait de corriger, n'y est plus.

- **Renvoyer une valeur magique** (`-1`, `null`, une chaîne vide) au lieu de lever ou de renvoyer une valeur typée. Coût : l'appelant doit se souvenir de vérifier, et le compilateur ne l'aide pas.

- **Un `return` dans un `finally`.** Si le `finally` renvoie une valeur, il **écarte** l'exception qui était en train de remonter, et l'erreur se perd sans une seule trace. Coût : le même qu'un `catch` vide, avec la différence que le code a l'air plus innocent. Le `finally` sert à nettoyer, pas à décider ce qui est renvoyé.

- **Avaler une `InterruptedException`.** Coût : un programme qui n'obéit pas quand on lui demande de s'arrêter (section 5.5). Propage-la ou restaure l'indicateur.

- **Utiliser une exception pour le flux normal.** Par exemple, appeler `Integer.parseInt` dans une boucle et attraper `NumberFormatException` pour décider si un texte « est un nombre », alors que c'est l'attendu, pas l'exceptionnel. Coût : du code plus lent (créer la trace coûte) et la décision de conception de la section 5.4 à l'envers : si l'absence d'un résultat est normale, c'est une valeur.

## Exercices

### Exercice 1 — Un fichier cassé pour chaque erreur

`Configuracion` sait échouer de plusieurs façons, chacune avec son message. Pour chacun des huit cas ci-dessous, **prédis le message avant d'exécuter** puis vérifie. Crée un nouveau dossier par cas (par exemple, `roto-1/`), mets-y un `servicios.txt` avec le contenu indiqué et exécute le `revisor` **depuis ce dossier** : `java ../revisor/Revisor.java` (ajuste le chemin à l'endroit où tu as ton projet). Comme le programme cherche `servicios.txt` dans le dossier courant, tu n'as pas besoin de toucher au code.

1. `catalogo https://catalogo.example` et, à la ligne suivante, seulement `pagos`.
2. Un commentaire (`# x`) à la ligne 1 et, à la ligne 2, `catalogo ftp://catalogo.example`.
3. `catalogo https://c.example` et `pagos http://pa|gos.example`.
4. `catalogo https://c.example 5000` et `pagos https://p.example abc`.
5. Deux lignes, toutes deux avec le nom `catalogo`.
6. Un fichier avec seulement des commentaires et des lignes vides.
7. `catalogo https://c.example 0`.
8. `catalogo https://c.example 1 2 3`.

### Exercice 2 — Fais disparaître le motif

Change `leerTimeout` dans `Configuracion.java` pour que le `catch (NumberFormatException e)` ne lève rien et renvoie simplement `5000`. Dans un nouveau dossier, mets un `servicios.txt` avec l'unique ligne `pagos https://p.example abc` et exécute depuis là (`java ../revisor/Revisor.java`). Que vois-tu ? Ensuite restaure le code. Note dans ton journal de bord combien de temps tu mettrais à découvrir le problème si le fichier avait cent lignes.

### Exercice 3 — Un fichier dans un autre encodage

Dans un nouveau dossier (`mkdir latin1 && cd latin1`), crée un fichier avec un accent écrit dans l'ancien encodage ISO-8859-1 (le *latin1* des vieux systèmes), qui n'est pas de l'UTF-8, avec cette commande :

```bash
printf 'catalogo https://catalogo.example\n# caf\xe9\n' > servicios.txt
```

Exécute le `revisor` depuis ce dossier (`java ../revisor/Revisor.java`) et lis le message. Ensuite, ajoute une ligne au `catch` de `Revisor.java` pour imprimer la cause (`e.getCause()`) et découvre quelle exception du JDK se cache derrière le message. Restaure le code.

### Exercice 4 — Reconvertis `revisarTodos` en *stream*

Remplace le `for` de `revisarTodos` par `servicios.stream().map(Revision::revisar).toList()`. **Prédis** si cela compile et ce que dira le compilateur. Ensuite vérifie. Et réponds : que devrais-tu changer dans `revisar` pour que cette version compile ?

## Solutions

### Solution 1

| Contenu de `servicios.txt` | Message |
|---|---|
| `catalogo https://catalogo.example` et `pagos` | `error de configuración: renglón 2: se esperaban 2 o 3 campos y hay 1` |
| `catalogo ftp://catalogo.example` (après un commentaire) | `error de configuración: renglón 2: la url debe empezar con http:// o https://` |
| `pagos http://pa\|gos.example` (ligne 2) | `error de configuración: renglón 2: la url no es válida` |
| `pagos https://p.example abc` (ligne 2) | `error de configuración: renglón 2: timeout_ms debe ser un entero mayor que 0` |
| deux lignes avec `catalogo` | `error de configuración: nombres repetidos: catalogo` |
| seulement des commentaires et des lignes vides | `error de configuración: el archivo no tiene servicios` |
| `catalogo https://c.example 0` | `error de configuración: renglón 1: timeout_ms debe ser un entero mayor que 0` |
| `catalogo https://c.example 1 2 3` | `error de configuración: renglón 1: se esperaban 2 o 3 campos y hay 5` |

J'ai mesuré les huit messages avec le projet de la section 5.6. Il y a deux choses à regarder avec soin. Dans le cas 2, le commentaire de la ligne 1 compte : l'erreur est à la ligne 2 **du fichier**, pas dans le deuxième service. Et dans les cas 4 et 7, le message est le même bien que le motif soit différent (un texte qui n'est pas un nombre, et un nombre qui n'est pas positif) : pour qui écrit le fichier, la correction est la même. La différence est dans la cause, qui n'existe que dans le premier (la `NumberFormatException`), et que voit celui qui débogue.

### Solution 2

```java
static int leerTimeout(String texto, int numero) throws ErrorDeConfiguracion {
  String mensaje = "renglón " + numero + ": timeout_ms debe ser un entero mayor que 0";
  try {
    int timeoutMs = Integer.parseInt(texto);
    if (timeoutMs > 0) {
      return timeoutMs;
    }
  } catch (NumberFormatException e) {
    return 5000;
  }
  throw new ErrorDeConfiguracion(mensaje);
}
```

Avec un `servicios.txt` qui n'a que `pagos https://p.example abc`, le `revisor` imprime le tableau de `pagos` comme `ARRIBA`, sans aucun avertissement : `abc` a disparu et le service est resté avec le délai maximal par défaut. Avec cent lignes, tu n'aurais aucun moyen de savoir laquelle avait un temps invalide : le programme « fonctionne ». C'est le problème de la section 5.3.2 en petit. Le message d'erreur d'origine (`timeout_ms debe ser un entero mayor que 0`) n'était pas une gêne : c'était le seul avertissement que la personne qui écrit le fichier allait recevoir.

### Solution 3

Le message est `error de configuración: no se pudo leer el archivo servicios.txt` : il ne dit pas pourquoi. Avec `System.err.println(e.getCause());` dans le `catch` de `Revisor.java`, il s'imprime :

```text
java.nio.charset.MalformedInputException: Input length = 1
```

L'octet `0xE9` est le `é` en ISO-8859-1, mais comme séquence UTF-8 il est invalide (en UTF-8 le `é` occupe deux octets). `Files.newBufferedReader` décode en UTF-8 et, en tombant sur l'octet, lève `MalformedInputException`, une `IOException` que `Configuracion.leer` convertit en « no se pudo leer ». Remarque que le message pour la personne est correct mais peu informatif, et que la **cause** a la donnée qui manque. C'est l'utilité de la conserver : message court pour l'utilisateur, cause complète pour qui débogue.

### Solution 4

Cela ne compile pas. Le compilateur dit (j'ai abrégé le chemin, que le lanceur imprime en entier) :

```text
Revision.java:20: error: incompatible thrown types InterruptedException in functional expression
    return servicios.stream().map(Revision::revisar).toList();
                                  ^
```

`Revision::revisar` déclare `throws InterruptedException` et la `Function` qu'attend `map` ne peut pas la lever. Pour que cela compile, il faudrait retirer le `throws` de `revisar`, et cela signifie **attraper l'`InterruptedException` dans `revisar`**, où tu ne sais pas quoi en faire ; le bon geste serait de restaurer l'indicateur avec `Thread.currentThread().interrupt()` et de renvoyer quelque chose, ce qui cache l'interruption à l'appelant. C'est pourquoi le cours conserve le `for` : c'est lui qui laisse passer l'exception jusqu'à celui qui peut, lui, décider.

## Comment savoir que j'ai réussi

- [ ] `java fig05_01.java` imprime les cinq chaînes, et tu peux dire pourquoi `NoSuchFileException` est contrôlée et `NumberFormatException` non.
- [ ] `java fig05_02.java` échoue avec `unreported exception IOException`, et tu connais les deux corrections que le compilateur accepte.
- [ ] `java fig05_04.java` échoue avec `has already been caught`, et tu sais pourquoi l'ordre des `catch` va du particulier au général.
- [ ] `java fig05_06.java` imprime les fermetures dans l'ordre inverse, et tu trouves dans la sortie l'exception supprimée.
- [ ] Depuis le dossier de la figure des exceptions personnalisées, `java Demo.java` imprime un `Caused by:`, et tu sais quelle exception du JDK se cache derrière.
- [ ] `java fig05_10.java` imprime `true`, `false` et `true`, et tu sais expliquer ce qu'a fait chacun.
- [ ] Depuis le dossier du projet, `java Revisor.java` imprime le tableau avec `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`, et met environ deux secondes et demie.
- [ ] Depuis un autre dossier, le `revisor` imprime `error de configuración: no existe el archivo servicios.txt`, et tu sais expliquer pourquoi le fichier existe et qu'il ne le trouve pourtant pas.
- [ ] Les huit fichiers cassés de l'exercice 1 produisent les huit messages de la solution, et tu les as prédits avant d'exécuter.
- [ ] Tu as noté dans ton journal de bord une exception contrôlée qui t'a obligé à décider quelque chose.

## Résumé

Réponds sans regarder la leçon :

1. Quelle différence y a-t-il entre une exception contrôlée et une non contrôlée ? Laquelle le compilateur oblige-t-il à traiter ?
2. Que signifie `unreported exception IOException; must be caught or declared to be thrown`, et quelles sont les deux corrections ?
3. Qu'est-ce qu'un `finally` garantit ? Et pourquoi ne doit-il pas porter de `return` ?
4. Pourquoi l'ordre des `catch` va-t-il du particulier au général ?
5. Que garantit `try-with-resources` face à une fermeture manuelle ? Dans quel ordre ferme-t-il ?
6. Pourquoi une exception contrôlée ne peut-elle pas être levée depuis l'intérieur de la lambda d'un `map`, et que le `revisor` utilise-t-il à la place ?
7. Qu'est-ce que la cause d'une exception, et que se passe-t-il si tu ne la passes pas en la convertissant en une autre ?
8. Pourquoi un service tombé est-il une valeur et un fichier de configuration illisible une exception ?
9. Que fait `Thread.sleep` quand quelqu'un interrompt le thread, et qu'arrive-t-il à l'indicateur d'interruption ?
10. Quelles sont les deux façons correctes de gérer une `InterruptedException` ?

## Pour aller plus loin

- [Oracle, « Lesson: Exceptions » (tutoriel Java)](https://docs.oracle.com/javase/tutorial/essential/exceptions/index.html) : le sujet complet, de ce qu'est une exception à leur chaînage, avec la discussion sur les non contrôlées. Consulté le 7 octobre 2026.
- [The Java Language Specification, chapitre 11, « Exceptions »](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html) : la définition exacte de contrôlée et non contrôlée et de ce que chaque méthode doit déclarer. Consulté le 7 octobre 2026.
- [Oracle, « The try-with-resources Statement »](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) : l'ordre de fermeture et les exceptions supprimées, avec des exemples. Consulté le 7 octobre 2026.
- [Oracle, « Interrupts » (tutoriel de concurrence)](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html) : comment on arrête un thread avec courtoisie, et pourquoi `InterruptedException` ne s'ignore pas. Consulté le 7 octobre 2026.

---

**Précédente :** [Leçon 4 — Collections, génériques et streams](04-colecciones-genericos-streams.md) ·
**Suivante :** [Leçon 6 — Projet avec Maven](06-proyecto-maven.md)
