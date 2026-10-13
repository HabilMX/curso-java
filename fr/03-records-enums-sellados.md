# Leçon 3 — Records, enums et types scellés

**Durée :** 2 × 90 min (deux séances ; la coupure se situe entre les sections 3.2 et 3.3)

**Ce que tu construis :** le modèle de données du `revisor` : un `Servicio` avec son nom, son adresse et son délai maximal, un `Estado` qui ne peut valoir que trois choses, et une `Respuesta` qui est soit « le service a répondu » soit « il n'a pas répondu », sans le moindre `null` entre les deux. Le `revisor` passe d'un fichier à sept

**Ce que tu apprends :** `record` (composants, accesseurs, `equals`, `hashCode` et `toString` gratuits, constructeur compact et constructeur supplémentaire) ; pourquoi l'immuabilité d'un `record` est superficielle ; `enum` ; interfaces `sealed` avec `permits` ; `switch` avec motifs, exhaustif et sans `default` ; gardes avec `when` ; motifs de record et le motif sans nom `_` ; et comment exécuter un projet de plusieurs fichiers avec `java Revisor.java`

**D'où tu viens.** Tu apportes le `Revisor.java` de la [Leçon 2](02-tipos-cadenas-control.md) : un seul fichier compact avec `codigoDe`, `tiempoDe`, `clasificar`, `formatearTiempo`, `detalle`, `linea`, `encabezado`, `revisar` et un `main` qui imprime le tableau de quatre services. Il fonctionne, et il a une faiblesse que l'exercice de projet de la leçon t'a mise sous les yeux : l'information de chaque service vit **répartie** dans deux `switch` parallèles, et un service qui n'a pas répondu est un `null` dans chacun. Si tu oublies d'ajouter une donnée dans l'un des deux, le programme compile, s'exécute et affiche une ligne erronée, sans la moindre plainte. Cette leçon corrige cela avec trois outils du langage : des types qui **regroupent** des données (`record`), des types qui **énumèrent** des possibilités (`enum`), et des types qui disent « c'est l'une de ces formes et aucune autre » (`sealed`).

**Ce que cette leçon ne fait pas.** Elle n'utilise pas encore de listes : les quatre services sont toujours écrits à la main dans le `main`, un par un, et leur vérification est toujours simulée avec un `switch`. Les collections arrivent dans la [Leçon 4](04-colecciones-genericos-streams.md) et les vraies erreurs, avec `try` et `catch`, dans la 5. Ici `throw` apparaît une fois, dans un exemple, et s'explique dans un encadré ; sa gestion complète relève de la Leçon 5. Il n'y a pas non plus de `public` ni de `private` dans le code du projet : la visibilité s'explique dans la Leçon 6, avec les paquets.

**À propos des exécutions.** Les figures isolées (`fig03_01.java` à `fig03_15.java`) s'exécutent avec `java fig03_NN.java`. Le projet de plusieurs fichiers, la figure `fig03_16`, est un **dossier** : tu y entres et tu exécutes `java Revisor.java`. Tu les trouves dans [`programas/03-records-enums-sellados/`](../programas/03-records-enums-sellados/). Les sorties ont été obtenues avec Temurin 25.0.4 et `LANG=C.UTF-8`. Les figures qui échouent exprès ont leur erreur dans un fichier `fig03_NN.error-esperado.txt`.

## À la fin, tu seras capable de

- Déclarer un `record` et expliquer ce que le compilateur te donne gratuitement (accesseurs, `equals`, `hashCode`, `toString`).
- Écrire un constructeur compact qui normalise une donnée, et un constructeur supplémentaire qui complète des valeurs par défaut.
- Expliquer pourquoi un `record` contenant un tableau (*array*) n'est pas vraiment immuable.
- Déclarer un `enum`, parcourir ses valeurs et écrire dessus un `switch` que le compilateur vérifie.
- Déclarer une interface `sealed` avec ses formes autorisées et écrire un `switch` avec motifs qui les couvre toutes sans `default`.
- Utiliser une garde `when`, un motif de `record` et le motif sans nom `_`, et dire dans quel ordre vont les cas.
- Montrer ce qui arrive au compilateur quand tu ajoutes un nouveau cas à un type scellé, et lister les endroits qu'il t'oblige à changer.

## Le pourquoi avant le comment

Jusqu'ici, le `revisor` a un problème de **représentation**. Pour dire « catalogo a répondu 200 en 142 ms », il utilise deux fonctions qui ne savent rien l'une de l'autre ; pour dire « reportes n'a pas répondu », il utilise deux `null`. Un lecteur du code ne peut pas voir la forme des données : il doit la déduire de la façon dont elles sont utilisées. Et le compilateur ne la connaît pas non plus, donc il ne peut pas t'avertir quand quelqu'un se trompe : tu peux demander le code d'un service qui a répondu et le temps d'un qui n'a pas répondu, et Java le permet.

L'idée de cette leçon tient en une phrase : **fais en sorte que le compilateur connaisse la forme de tes données, et les états impossibles cessent de pouvoir s'écrire**. Deux exemples concrets du `revisor` :

- Un service qui n'a pas répondu **n'a pas** de code HTTP ni de temps. Avec un `null` dans chacun, tu peux te retrouver avec un code sans temps, ou l'inverse. Si en revanche « a répondu » et « n'a pas répondu » sont deux types distincts, et que seul le premier a un code et un temps, le second ne peut pas en avoir même si tu le voulais.
- Un état est `ARRIBA`, `LENTO` ou `CAIDO`. Avec un `String`, il peut être `"ARIBA"` (avec une faute) et le programme l'accepte. Avec un `enum`, non.

Si tu viens d'autres langages, cela te dira quelque chose : en Rust, c'est un `enum` avec des données ; en TypeScript, une union discriminée ; en Python moderne, une `dataclass` avec `match` ; en Go, il n'y a pas d'équivalent direct, et on le simule avec des interfaces. Java est arrivé tard à cette fête —les *records* datent de Java 16, les types scellés de Java 17 et les motifs dans le `switch` de Java 21—, mais il est arrivé complet et avec la vérification du compilateur incluse.

Le parcours comporte deux séances. La **première** couvre 3.1 et 3.2 : les *records* et les *enums*. En la terminant, tu pourras écrire des types de données en une ligne et tu sauras ce qui est et n'est pas immuable. La **seconde** couvre 3.3 à 3.5 : les types scellés, le `switch` avec motifs et l'étape du projet. En la terminant, le `revisor` imprimera toujours **exactement le même tableau** qu'à la fin de la leçon 2, mais construit avec des types et non avec des `null`. C'est cela, la preuve : le comportement ne change pas et le code est autre.

## Les concepts

### 3.1 Records

#### 3.1.1 Le problème : une classe uniquement pour transporter des données

Suppose que tu veuilles garder ensemble le nom d'un service et son temps. En Java, le chemin de toujours est d'écrire une classe. Regarde ce chemin, juste pour en voir la **taille** (il n'est pas nécessaire que tu comprennes chaque ligne) :

```java
final class PuntoClasico {
  final String nombre;
  final long tiempoMs;

  PuntoClasico(String nombre, long tiempoMs) {
    this.nombre = nombre;
    this.tiempoMs = tiempoMs;
  }

  String nombre() {
    return nombre;
  }

  long tiempoMs() {
    return tiempoMs;
  }

  @Override
  public boolean equals(Object otro) {
    if (this == otro) {
      return true;
    }
    if (otro == null || getClass() != otro.getClass()) {
      return false;
    }
    PuntoClasico p = (PuntoClasico) otro;
    return tiempoMs == p.tiempoMs && Objects.equals(nombre, p.nombre);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nombre, tiempoMs);
  }

  @Override
  public String toString() {
    return "PuntoClasico[nombre=" + nombre + ", tiempoMs=" + tiempoMs + "]";
  }
}
```

Ce sont trente-neuf lignes pour dire « un nom et un temps ». Et ce que fait chacune est mécanique : garder les deux données, les renvoyer, les comparer et les afficher. C'est le genre de code qui s'écrit de la même façon à chaque fois, et qui à chaque fois peut contenir une erreur : oublier un champ dans `equals`, ou dans `hashCode`, ou dans `toString`. (Tu as déjà appris pourquoi `equals` compte : `==` entre objets demande s'ils sont le même objet, et non s'ils valent la même chose.)

Java 16 a donné un nom à ce schéma : le ***record***. Sa [spécification](https://openjdk.org/jeps/395) le décrit comme un porteur transparent de données immuables, et la classe ci-dessus, complète, s'écrit ainsi :

```java
record Punto(String nombre, long tiempoMs) {}
```

Une ligne. Voyons-le à l'œuvre.

#### 3.1.2 Ce que te donne un `record`

Entre les parenthèses vont les **composants** : le type et le nom de chaque donnée. Le compilateur utilise cette liste pour générer tout le reste. Regarde `fig03_01.java`, qui utilise le `record` `Punto` et le teste :

```java
// fig03_01.java
record Punto(String nombre, long tiempoMs) {}

void main() {
  Punto a = new Punto("catalogo", 142);
  Punto b = new Punto("catalogo", 142);
  Punto c = new Punto("pagos", 87);

  IO.println(a);
  IO.println("nombre:     " + a.nombre());
  IO.println("tiempoMs:   " + a.tiempoMs());
  IO.println("a.equals(b): " + a.equals(b));
  IO.println("a == b:      " + (a == b));
  IO.println("a.equals(c): " + a.equals(c));
  IO.println("mismo hash:  " + (a.hashCode() == b.hashCode()));
}
```

```text
$ java fig03_01.java
Punto[nombre=catalogo, tiempoMs=142]
nombre:     catalogo
tiempoMs:   142
a.equals(b): true
a == b:      false
a.equals(c): false
mismo hash:  true
```

Chaque ligne de sortie montre quelque chose que le compilateur a écrit pour toi :

- **Un constructeur**, avec un paramètre par composant, dans le même ordre. `new Punto("catalogo", 142)` en crée un. (Le 142 est un `int`, et il entre dans le composant `long` par la conversion sans perte de la leçon précédente.)
- **Un accesseur par composant**, avec le même nom que le composant : `a.nombre()` et `a.tiempoMs()`. Remarque qu'**ils ne s'appellent pas `getNombre()`** : c'est l'une des rares conventions de Java où le *record* s'écarte de la tradition.
- **`toString`**, avec le format `Punto[nombre=catalogo, tiempoMs=142]`. C'est ce qui sort quand on imprime l'objet.
- **`equals`**, qui compare **composant par composant** : `a.equals(b)` est `true` parce que les deux valent la même chose, bien que ce soient deux objets distincts (`a == b` est `false`, comme tu t'y attendais après la Leçon 2). Pour un `record`, `equals` fait ce que tu voulais que `==` fasse.
- **`hashCode`**, cohérent avec `equals` : si deux objets sont égaux, leur `hashCode` (un nombre qui résume le contenu) coïncide. Tu ne l'utilises pas encore ; la Leçon 4 en a besoin pour les ensembles et les maps.

Et il y a d'autres règles, qui s'apprennent avec le compilateur. Les composants sont **finaux** : une fois le *record* créé, aucun ne change. Regarde `fig03_02.java`, qui essaie d'en changer un :

```java
// fig03_02.java
record Punto(String nombre, long tiempoMs) {}

void main() {
  Punto punto = new Punto("catalogo", 142);
  punto.tiempoMs = 5;
}
```

```text
$ java fig03_02.java
fig03_02.java:6: error: cannot assign a value to final variable tiempoMs
  punto.tiempoMs = 5;
       ^
1 error
error: compilation failed
```

Le compilateur le refuse : « impossible d'assigner une valeur à la variable finale `tiempoMs` ». Un *record* est, dans sa forme de base, une valeur qui se crée complète et qu'on ne touche plus. On ne peut pas non plus ajouter un champ isolé (`int extra;` dans le *record* donne l'erreur « field declaration must be static », avec la suggestion de le convertir en composant), et un *record* ne peut pas étendre une autre classe. Ce qu'il peut : avoir des **méthodes**, implémenter des **interfaces** (tu l'utiliseras en 3.3) et avoir des **constantes `static`**.

Cette restriction est le but. Un `record` dit par sa forme : « je suis un ensemble de données fixes, et mon identité est mon contenu ». Qui le lit sait à quoi s'attendre sans lire une seule ligne de logique, et qui l'utilise ne peut pas le casser par accident.

#### 3.1.3 Un constructeur supplémentaire et les valeurs par défaut

Le `revisor` a besoin d'un `Servicio` avec trois données : un `nombre`, une `url` et un `timeoutMs`, le temps maximal qu'on l'attend (la leçon 8 l'utilise pour de vrai). Presque tous les services utilisent la valeur par défaut de 5000 ms ; quelques-uns en ont besoin d'une autre. Java n'a pas de paramètres avec valeur par défaut (nous l'avons dit dans la Leçon 2), mais un *record* peut avoir un **constructeur supplémentaire**, plus court, qui appelle le principal. Regarde `fig03_03.java` :

```java
// fig03_03.java
record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}

void main() {
  URI direccion = URI.create("https://catalogo.example");
  Servicio catalogo = new Servicio("catalogo", direccion);
  Servicio notificaciones =
      new Servicio("notificaciones", URI.create("https://notificaciones.example"), 1000);

  IO.println(catalogo);
  IO.println(notificaciones);
  IO.println("host de catalogo: " + catalogo.url().getHost());
}
```

```text
$ java fig03_03.java
Servicio[nombre=catalogo, url=https://catalogo.example, timeoutMs=5000]
Servicio[nombre=notificaciones, url=https://notificaciones.example, timeoutMs=1000]
host de catalogo: catalogo.example
```

Il y a quatre nouveautés dans cette figure.

**`URI`** est une classe du JDK qui représente une adresse. `URI.create("https://catalogo.example")` en crée une à partir d'un texte, et `getHost()` renvoie le serveur (`catalogo.example`). Elle se trouve dans le paquet `java.net`. Pour utiliser une classe d'un autre paquet dans un fichier normal, il faut l'**importer** avec une ligne `import java.net.URI;` au début (tu le verras dans le projet de 3.5) ; le fichier compact importe pour toi tout ce qu'apporte le module `java.base`, et `URI` en fait partie, donc ici ce n'était pas nécessaire.

**Une constante dans un *record*.** `static final int TIMEOUT_POR_OMISION_MS = 5000;` est une constante comme celle de la leçon précédente ; `static` la fait appartenir au type et non à chaque objet, ce qui est la seule façon pour un *record* d'avoir quelque chose d'isolé.

**Le constructeur supplémentaire.** `Servicio(String nombre, URI url)` a deux paramètres ; son corps, `this(nombre, url, TIMEOUT_POR_OMISION_MS)`, **appelle le constructeur principal** avec la troisième donnée complétée. Grâce à cela, `new Servicio("catalogo", direccion)` et `new Servicio("notificaciones", ..., 1000)` sont des appels valides et sortent avec le temps qui convient : 5000 dans le premier, 1000 dans le second. Remarque aussi le nom `timeoutMs` : il porte l'unité, comme dans la leçon précédente.

**L'appel coupé en deux lignes.** Quand un appel dépasse 100 colonnes, on le coupe après le `=` et la suite entre avec quatre espaces de plus. C'est le guide de style dont nous avons parlé.

#### 3.1.4 Le constructeur compact : valider et normaliser

Parfois tu veux que le *record* **nettoie ou vérifie** ses données à la création : retirer les espaces d'un nom, rejeter un temps impossible. Pour cela existe le **constructeur compact** : il s'écrit avec le nom du *record* et **sans parenthèses**, et son corps s'exécute avant que les composants soient assignés. À l'intérieur, tu peux réassigner les paramètres (qui portent le nom des composants) et, si quelque chose ne va pas, arrêter le programme avec une erreur. Regarde `fig03_04.java` :

```java
// fig03_04.java
record Umbral(String nombre, long limiteMs) {
  Umbral {
    nombre = nombre.strip();
    if (limiteMs <= 0) {
      throw new IllegalArgumentException("limiteMs debe ser mayor que 0 y llegó " + limiteMs);
    }
  }
}

void main() {
  Umbral lento = new Umbral("  lento ", 1000);
  IO.println(lento);
  Umbral roto = new Umbral("roto", 0);
  IO.println(roto);
}
```

```text
$ java fig03_04.java
Umbral[nombre=lento, limiteMs=1000]
Exception in thread "main" java.lang.IllegalArgumentException: limiteMs debe ser mayor que 0 y llegó 0
	at fig03_04$Umbral.<init>(fig03_04.java:6)
	at fig03_04.main(fig03_04.java:14)
```

La première création passe par le constructeur compact : `nombre = nombre.strip();` réassigne le paramètre avec sa version nettoyée (la règle de 2.2.1 : `strip` renvoie un nouveau texte, et ici nous le gardons bel et bien), et le *record* devient `Umbral[nombre=lento, limiteMs=1000]`, avec le nom sans les espaces avec lesquels il est arrivé. La seconde création a `limiteMs` à 0, et l'instruction `throw` arrête l'exécution.

> **Un encadré sur `throw`.** `throw new IllegalArgumentException("...")` signifie « il y a ici un problème que je ne sais pas résoudre ; arrête ce que tu fais et préviens ». `IllegalArgumentException` est une classe du JDK dont le nom veut dire « argument illégal ». Quand personne ne l'attrape, le programme se termine et la JVM imprime le message et la **trace** (la liste de l'endroit où cela s'est produit : ici, le constructeur `Umbral.<init>` appelé depuis `main`). Comment attraper une exception, quand cela convient et quand non, c'est la Leçon 5. Pour l'instant, tu as seulement besoin de savoir qu'un constructeur compact est l'endroit pour rejeter des données qui n'ont pas de sens.

La trace est une bonne occasion de s'exercer à la lecture. Ce qu'elle dit, de haut en bas : l'erreur était `IllegalArgumentException` avec le message que nous avons écrit ; elle s'est produite dans `fig03_04$Umbral.<init>` (le constructeur du *record*, à l'intérieur de la classe du fichier compact) à la ligne 6, et cet appel a été fait par `main` à la 14. Le `$` sépare la classe implicite du fichier de la classe `Umbral` qui vit à l'intérieur.

Dans le `Servicio` du `revisor`, nous **ne validons pas** dans le constructeur compact : la validation de ce qui arrive d'un fichier de configuration est une tâche qui a sa propre place, et qui viendra avec tes propres exceptions (Leçon 5) et le JSON (Leçon 8).

#### 3.1.5 L'immuabilité est superficielle

Un *record* est immuable en ce sens que **ses composants ne sont pas réassignés**. Mais si un composant est, à son tour, un objet qui peut être modifié, cet objet reste modifiable. L'immuabilité ne va que jusqu'à la première couche. Pour le voir, il faut un type modifiable, et le plus simple de Java est le **tableau**.

> **Un tableau en trois lignes.** Un tableau (*array*) est une séquence de taille fixe d'éléments du même type. `String[] etapas = {"conectar", "esperar"};` en crée un de deux textes. `etapas[0]` est le premier élément (on compte à partir de 0), et `etapas[0] = "otro";` **change** cet élément : les tableaux sont modifiables. Pour imprimer leur contenu, on utilise `Arrays.toString(etapas)` ; sans cela, il imprime une adresse illisible. On n'utilise presque pas les tableaux dans le Java de tous les jours ; dans la Leçon 4, les listes les remplacent. Mais ils sont l'exemple le plus clair d'un objet modifiable.

Regarde `fig03_05.java` :

```java
// fig03_05.java
record Ruta(String nombre, String[] etapas) {}

void main() {
  String[] etapas = {"conectar", "esperar"};
  Ruta ruta = new Ruta("catalogo", etapas);
  IO.println("antes:   " + Arrays.toString(ruta.etapas()));

  etapas[0] = "CAMBIADO DESDE FUERA";
  ruta.etapas()[1] = "CAMBIADO DESDE EL ACCESOR";
  IO.println("después: " + Arrays.toString(ruta.etapas()));

  Ruta copia = new Ruta("catalogo", new String[] {"conectar", "esperar"});
  Ruta otra = new Ruta("catalogo", new String[] {"conectar", "esperar"});
  IO.println("copia.equals(otra): " + copia.equals(otra));
}
```

```text
$ java fig03_05.java
antes:   [conectar, esperar]
después: [CAMBIADO DESDE FUERA, CAMBIADO DESDE EL ACCESOR]
copia.equals(otra): false
```

Il y a deux trous. Le premier : `etapas[0] = "CAMBIADO DESDE FUERA"` (« CHANGÉ DEPUIS L'EXTÉRIEUR ») modifie le tableau **original** que nous avons passé au constructeur, et comme le *record* garde **la même référence** (pas une copie), le *record* a changé sans que personne y touche. Le second : `ruta.etapas()[1] = ...` modifie le tableau à travers l'accesseur, qui renvoie la référence qu'il garde. C'est pourquoi le programme imprime deux changements dans la seconde ligne. Le *record* n'a pu éviter ni l'un ni l'autre, parce que ce qui est final, c'est la **référence** `etapas`, pas le contenu du tableau.

La dernière ligne réserve une autre surprise. Deux `Ruta` créées avec des tableaux de même contenu **ne sont pas égales** selon `equals`. Le `equals` du *record* compare composant par composant, et pour comparer les tableaux il utilise le `equals` des tableaux, qui demande l'identité, comme `==`. C'est le même problème qu'en 2.2.2, une fois de plus.

La leçon pratique : **un *record* est aussi immuable que ses composants.** Avec des textes, des nombres, des `enum`s et d'autres *records* immuables, il n'y a pas de problème. Avec un tableau, ou n'importe quel objet qui peut être modifié, l'immuabilité est une illusion. Dans la Leçon 4, tu verras la solution habituelle pour les listes : garder une **copie non modifiable** dans le constructeur compact (`List.copyOf`). Attention à ce qu'elle ferme et à ce qu'elle ne ferme pas : personne ne pourra ajouter, retirer ni changer d'éléments **de la liste**, mais `List.copyOf` ne copie pas les éléments et ne les rend pas immuables. Cela suffit quand les éléments sont déjà immuables, comme des textes ou des *records* aux composants immuables (le cas du `revisor`) ; s'ils étaient des objets modifiables, il faudrait aussi copier chaque élément ou les concevoir immuables.

### 3.2 Enums

Le `revisor` classe chaque service dans l'un de trois états : `ARRIBA`, `LENTO` ou `CAIDO`. Dans la Leçon 2, l'état était un `String`, ce qui veut dire que n'importe quel texte était valide : `"ARIBA"`, `"caído"`, `""`. Un ***enum*** (de « énumération ») est un type dont les valeurs possibles sont **une liste fixe, écrite par toi**, et rien de plus. Regarde `fig03_06.java` :

```java
// fig03_06.java
enum Estado { ARRIBA, LENTO, CAIDO }

enum Severidad {
  BAJA(1), MEDIA(5), ALTA(10);

  final int peso;

  Severidad(int peso) {
    this.peso = peso;
  }

  boolean urgente() {
    return peso >= 5;
  }
}

String aviso(Estado estado) {
  return switch (estado) {
    case ARRIBA -> "todo bien";
    case LENTO -> "revisar pronto";
    case CAIDO -> "avisar ya";
  };
}

void main() {
  Estado estado = Estado.LENTO;
  IO.println(estado);
  IO.println("name():    " + estado.name());
  IO.println("ordinal(): " + estado.ordinal());
  IO.println("== :       " + (estado == Estado.LENTO));
  IO.println("valueOf:   " + Estado.valueOf("CAIDO"));
  IO.println("values():  " + Arrays.toString(Estado.values()));
  IO.println(aviso(estado));
  IO.println("ALTA pesa " + Severidad.ALTA.peso + " y urgente = " + Severidad.ALTA.urgente());
  IO.println("BAJA urgente = " + Severidad.BAJA.urgente());
}
```

```text
$ java fig03_06.java
LENTO
name():    LENTO
ordinal(): 1
== :       true
valueOf:   CAIDO
values():  [ARRIBA, LENTO, CAIDO]
revisar pronto
ALTA pesa 10 y urgente = true
BAJA urgente = false
```

La première ligne, `enum Estado { ARRIBA, LENTO, CAIDO }`, déclare le type avec ses trois constantes. Les constantes s'écrivent en majuscules, sans accent (c'est ainsi que le cours le fixe pour celles du `revisor` : `CAIDO`, pas `CAÍDO`), et s'utilisent sous la forme `Estado.LENTO`. Ce qui se passe dans `main` :

- Imprimer une valeur de l'*enum* écrit son nom : `LENTO`. `name()` renvoie ce même nom sous forme de texte.
- `ordinal()` dit à quelle position elle a été déclarée (`ARRIBA` est 0, `LENTO` est 1, `CAIDO` est 2). On s'en sert peu, et quand on s'en sert, c'est presque toujours un symptôme : si tu dépends de l'ordre, changer la liste casse le programme.
- **`==` est la bonne comparaison entre valeurs d'un *enum*.** Chaque constante n'existe qu'une seule fois dans tout le programme, donc demander si deux variables pointent vers la même revient exactement à demander si elles valent la même chose. Avec les *enums*, `==` est précisément ce que tu veux.
- `valueOf("CAIDO")` convertit un texte en la valeur (et lève une erreur si le texte n'est le nom d'aucune), et `values()` renvoie un tableau avec toutes, dans l'ordre où elles ont été déclarées.

Un *enum* peut avoir des **données et des méthodes propres**. `Severidad` a un champ `peso`, un constructeur qui le reçoit et une méthode `urgente()` ; chaque constante porte sa donnée entre parenthèses : `BAJA(1)`, `MEDIA(5)`, `ALTA(10)`. Cela sert à ce que l'information qui correspond à chaque valeur vive **à côté de la valeur**, et non dans un `switch` à part.

La meilleure raison d'utiliser un *enum* se voit dans `aviso` : c'est un `switch` à flèches, comme ceux de la leçon précédente, **sur un *enum*** et sans `default`. Le compilateur sait combien de valeurs a `Estado`, et si le `switch` ne les couvre pas toutes, il le refuse. Regarde `fig03_07.java`, où nous avons retiré le cas `CAIDO` :

```java
// fig03_07.java
enum Estado { ARRIBA, LENTO, CAIDO }

String aviso(Estado estado) {
  return switch (estado) {
    case ARRIBA -> "todo bien";
    case LENTO -> "revisar pronto";
  };
}

void main() {
  IO.println(aviso(Estado.CAIDO));
}
```

```text
$ java fig03_07.java
fig03_07.java:5: error: the switch expression does not cover all possible input values
  return switch (estado) {
         ^
1 error
error: compilation failed
```

C'est le même message que dans la leçon précédente (« le `switch` ne couvre pas toutes les valeurs possibles »), mais avec une énorme différence. Avec des textes, le compilateur exigeait un `default` parce qu'il y a une infinité de textes. Avec un *enum*, **tu n'as pas besoin de `default`** : le compilateur sait qu'il y en a trois, et si quelqu'un ajoute à l'avenir un quatrième état à l'*enum*, chaque `switch` qui ne le couvre pas cesse de compiler. Les erreurs du type « j'ai oublié un cas » cessent d'être des erreurs d'exécution pour devenir des erreurs de compilation. C'est l'une des meilleures nouvelles de la leçon, et la seconde partie la porte à un niveau supérieur.

Ainsi se termine la première séance. Avant de continuer, vérifie que tu peux répondre sans regarder : quelles quatre choses un `record` te donne-t-il sans que tu les écrives ? Pourquoi `Ruta` de `fig03_05.java` n'est-il pas immuable ? Pourquoi un `switch` sur un *enum* n'a-t-il pas besoin de `default` ?


### 3.3 Interfaces scellées

#### 3.3.1 Une interface : un type avec plusieurs formes

Un service peut répondre ou ne pas répondre, et chaque cas a besoin de **données différentes** : s'il a répondu, il a un code et un temps ; sinon, il a un motif. Un `record` ne décrit qu'une seule forme. Pour dire « une valeur peut être d'une forme ou d'une autre », il faut un type qui réunisse les deux, et en Java ce type est une **interface**.

Une interface est un type qui déclare **ce qu'on peut faire** sans dire comment ; toute classe ou tout *record* qui déclare `implements NombreDeLaInterfaz` est, à la fois, une valeur de ce type. Une variable de type `Respuesta` peut garder n'importe quoi qui implémente `Respuesta`. C'est l'outil que Java offre depuis toujours pour le « c'est l'un de plusieurs », et il a un défaut : **n'importe qui peut ajouter une nouvelle forme** en écrivant un autre type qui dit `implements Respuesta`, dans n'importe quel fichier, et le compilateur n'a aucun moyen de savoir combien il y en a.

Pour le `revisor`, c'est un problème. La classification doit décider quoi faire de chaque forme de réponse, et veut que le compilateur lui garantisse qu'**aucune n'a été oubliée**. Cette garantie est le travail des interfaces **scellées**.

#### 3.3.2 `sealed` et `permits`

Une interface scellée est une interface qui liste, à l'avance, **tous les types qui peuvent l'implémenter**. Elle s'écrit avec le mot `sealed` et une clause `permits`. Regarde `fig03_08.java`, qui définit la `Respuesta` du `revisor` :

```java
// fig03_08.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio r -> "respondió " + r.codigo() + " en " + r.tiempoMs() + " ms";
    case Respuesta.SinRespuesta s -> "no respondió: " + s.motivo();
  };
}

void main() {
  Respuesta uno = new Respuesta.Respondio(200, 142);
  Respuesta otro = new Respuesta.SinRespuesta("sin conexión");
  IO.println(describir(uno));
  IO.println(describir(otro));
}
```

```text
$ java fig03_08.java
respondió 200 en 142 ms
no respondió: sin conexión
```

Lis la première ligne : `sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta`. Elle dit : « `Respuesta` est une interface ; seules ces deux formes sont permises ». À l'intérieur, entre les accolades de l'interface, les deux formes sont définies comme des *records* **imbriqués** : `Respondio(int codigo, long tiempoMs)` et `SinRespuesta(String motivo)`, chacun avec `implements Respuesta`. Un type défini à l'intérieur d'un autre se nomme avec son préfixe, d'où les noms `Respuesta.Respondio` et `Respuesta.SinRespuesta` dans le reste du programme. (Dans un projet de plusieurs fichiers sans paquet, on ne peut pas écrire un `import` pour les raccourcir ; tu utiliseras le nom complet.)

Regarde maintenant ce qu'exprime le modèle. **Un service qui a répondu** a un code et un temps, toujours les deux. **Celui qui n'a pas répondu** a un motif, et n'a ni code ni temps, pas même un `null` : tout simplement, ces données n'existent pas dans cette forme. Fini l'état impossible de la Leçon 2 (code sans temps, ou l'inverse). C'est une union discriminée de TypeScript, un `enum` avec données de Rust, un type somme de Haskell : la même idée avec la syntaxe de Java.

Les formes permises doivent être `final`, `sealed` ou `non-sealed` ; les *records* sont `final` automatiquement, de sorte qu'ici il n'y a rien à écrire. Et elles doivent vivre dans le même paquet (dans notre cas, le paquet sans nom dans lequel se trouve tout le projet) ou module. Si quelqu'un essaie d'ajouter une forme qui n'est pas dans la liste, le compilateur l'arrête. C'est `fig03_09.java`, qui ajoute `Redirigida` en dehors du `permits` :

```java
// fig03_09.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

record Redirigida(int codigo, String destino) implements Respuesta {}

void main() {
  Respuesta respuesta = new Redirigida(301, "https://catalogo.example/nuevo");
  IO.println(respuesta);
}
```

```text
$ java fig03_09.java
fig03_09.java:8: error: class is not allowed to extend sealed class: Respuesta (as it is not listed in its 'permits' clause)
record Redirigida(int codigo, String destino) implements Respuesta {}
^
1 error
error: compilation failed
```

Le message est clair : « la classe n'est pas autorisée à étendre la classe scellée : `Respuesta` (car elle ne figure pas dans sa clause `permits`) ». Pour ajouter une nouvelle forme, il faut **modifier l'interface**, et cette modification est ce qui déclenche les avertissements du compilateur dans le reste du programme. En 3.4, tu verras lesquels.

Un détail de `fig03_08.java` : le `switch` de `describir` a deux cas, `case Respuesta.Respondio r ->` et `case Respuesta.SinRespuesta s ->`, et n'a pas de `default`. C'est ce qu'explique la section suivante.

### 3.4 `switch` avec motifs

#### 3.4.1 Motifs de type et exhaustivité

Dans la Leçon 2, le `switch` comparait une valeur à des constantes (`case "catalogo"`). Depuis Java 21, un `switch` peut comparer à des **motifs** (*patterns*) : au lieu de « est-ce que ça vaut ceci ? », il demande « est-ce que ça a cette forme ? ». Le motif le plus simple est le **motif de type** : `case Respuesta.Respondio r` veut dire « si la réponse est un `Respondio`, appelle-la `r` dans ce cas ». À l'intérieur, `r` a déjà le type précis, donc on peut écrire `r.codigo()` et `r.tiempoMs()` sans aucune conversion. La fonctionnalité a été standardisée dans le [JEP 441](https://openjdk.org/jeps/441).

Ce qui compte, c'est ce que fait le compilateur avec un `switch` sur un type scellé : il **sait combien il y a de formes**. Si les cas les couvrent toutes, le `switch` est **exhaustif** et n'a pas besoin de `default`. S'il en manque une, il ne compile pas. Regarde `fig03_10.java`, qui l'illustre en retirant la forme `SinRespuesta` :

```java
// fig03_10.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio r -> "respondió " + r.codigo();
  };
}

void main() {
  IO.println(describir(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_10.java
fig03_10.java:9: error: the switch expression does not cover all possible input values
  return switch (respuesta) {
         ^
1 error
error: compilation failed
```

Le message est le même qu'avec l'*enum* de 3.2 : « le `switch` ne couvre pas toutes les valeurs possibles ». C'est la même garantie, maintenant pour des formes avec des données.

Et le contraire est un risque tout aussi réel. Un `default` dans un `switch` sur un type scellé éteint cette garantie. Regarde `fig03_11.java`. On a ajouté une troisième forme, `Redirigida`, et le `switch` de `clasificar` a un `default` :

```java
// fig03_11.java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.Respondio _ -> "ARRIBA";
    default -> "CAIDO";
  };
}

void main() {
  Respuesta redirigida = new Respuesta.Redirigida(301, "https://catalogo.example/nuevo");
  IO.println(clasificar(redirigida));
}
```

```text
$ java fig03_11.java
CAIDO
```

Le programme compile, s'exécute et dit qu'une redirection est `CAIDO`. Personne n'a décidé cela : le `default` a avalé la nouvelle forme et a répondu pour toutes celles qu'il ne mentionne pas. C'est une erreur silencieuse, le pire type, et le compilateur n'a pas pu avertir parce que le `default` lui a dit « j'ai déjà tout couvert ». **Avec un type scellé, ou avec un *enum*, n'écris pas de `default`.** Si tu l'omets, chaque nouvelle forme casse la compilation dans chaque `switch` qui doit la prendre en compte, ce qui est exactement ce que tu veux.

#### 3.4.2 Gardes, motifs de record et `_`

Le `revisor` a besoin de plus que de distinguer entre « a répondu » et « n'a pas répondu » : dans « a répondu », il y a trois cas (code d'erreur, lent, normal). Les motifs ont deux outils pour cela.

Le premier est la **garde** `when` : une condition supplémentaire que le cas doit remplir. `case Respuesta.Respondio r when r.codigo() >= 400` n'entre que si la réponse est un `Respondio` **et** que son code est, de plus, 400 ou plus. C'est déjà le `&&` de la leçon précédente, appliqué au motif.

Le second est le **motif de record** : au lieu de nommer l'objet puis de lui demander ses composants, le motif les **démonte** dans le même cas. `case Respuesta.Respondio(int codigo, long tiempoMs)` veut dire « si c'est un `Respondio`, extrais son code dans la variable `codigo` et son temps dans `tiempoMs` ». Il est défini dans le [JEP 440](https://openjdk.org/jeps/440).

Et un détail de confort : si tu n'as pas besoin de la valeur d'un composant, tu le remplaces par le **motif sans nom** `_` (définitif depuis Java 22, [JEP 456](https://openjdk.org/jeps/456)). Il dit « il y a quelque chose ici, et cela m'est égal ». Regarde `fig03_12.java`, qui implémente la classification du `revisor` de bout en bout :

```java
// fig03_12.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

static final long UMBRAL_LENTO_MS = 1000;

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS -> "LENTO";
    case Respuesta.Respondio _ -> "ARRIBA";
    case Respuesta.SinRespuesta _ -> "CAIDO";
  };
}

void main() {
  IO.println(clasificar(new Respuesta.Respondio(200, 142)));
  IO.println(clasificar(new Respuesta.Respondio(200, 2300)));
  IO.println(clasificar(new Respuesta.Respondio(503, 95)));
  IO.println(clasificar(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_12.java
ARRIBA
LENTO
CAIDO
CAIDO
```

Quatre cas, dans cet ordre, chacun étant une règle du `revisor` :

1. `Respondio(int codigo, long _) when codigo >= 400` est la règle 2 : a répondu avec une erreur. Il utilise le code et ne se soucie pas du temps.
2. `Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS` est la règle 3 : a répondu et a mis du temps. Maintenant il utilise le temps et pas le code.
3. `Respondio _` est la règle 4, celle qui reste : toute autre réponse est ARRIBA. Avec `_` à la place d'un nom, on ne donne même pas de nom à la variable, parce qu'elle n'est pas utilisée.
4. `SinRespuesta _` est la règle 1 : sans réponse, CAIDO.

Les cas sont évalués **de haut en bas, et le premier qui est rempli gagne**. C'est pourquoi l'ordre compte, et le compilateur te protège d'un ordre impossible. Regarde `fig03_13.java`, où le cas général `Respondio _` a été écrit avant le cas spécifique :

```java
// fig03_13.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio _ -> "ARRIBA";
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.SinRespuesta _ -> "CAIDO";
  };
}

void main() {
  IO.println(clasificar(new Respuesta.Respondio(503, 95)));
}
```

```text
$ java fig03_13.java
fig03_13.java:11: error: this case label is dominated by a preceding case label
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
         ^
1 error
error: compilation failed
```

« Ce cas est dominé par un cas précédent ». Le premier cas attrape déjà **tous** les `Respondio`, de sorte que ceux qui viennent après, avec garde, ne pourraient jamais s'exécuter. Java le signale comme une erreur, pas comme un avertissement. La règle : **du plus spécifique au plus général**, et le cas sans garde d'un type va après ceux qui en ont une.

Avant de continuer, un contraste avec la Leçon 2. Là, `clasificar` recevait un `Integer` et un `Long` qui pouvaient être nuls, et utilisait `&&` pour ne pas déballer un vide. Ici, `clasificar` reçoit **une** `Respuesta`, et ses formes possibles disent déjà quelles données il y a. Il n'y a pas de nuls à vérifier, parce qu'il n'y a aucun moyen de demander le code à une réponse qui n'en a pas. La vérification que tu faisais avant, à la main, c'est maintenant le compilateur qui la fait.

#### 3.4.3 `null` et `instanceof`

Il y a deux autres choses qu'il vaut mieux connaître, parce qu'elles apparaissent dans le code que tu liras.

**Un `switch` sur une référence nulle explose**, à moins que tu ne le préviennes. Un `case null` capture le cas, et si tu l'omets, le `switch` lève une `NullPointerException` avant de regarder le moindre autre cas. Regarde `fig03_14.java` :

```java
// fig03_14.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case null -> "no hay respuesta registrada";
    case Respuesta.Respondio(int codigo, long _) -> "respondió " + codigo;
    case Respuesta.SinRespuesta(String motivo) -> "no respondió: " + motivo;
  };
}

String sinCasoNulo(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio _ -> "respondió";
    case Respuesta.SinRespuesta _ -> "no respondió";
  };
}

void main() {
  IO.println(describir(new Respuesta.Respondio(200, 142)));
  IO.println(describir(null));
  IO.println(sinCasoNulo(null));
}
```

```text
$ java fig03_14.java
respondió 200
no hay respuesta registrada
Exception in thread "main" java.lang.NullPointerException
	at java.base/java.util.Objects.requireNonNull(Objects.java:220)
	at fig03_14.sinCasoNulo(fig03_14.java:17)
	at fig03_14.main(fig03_14.java:26)
```

`describir` inclut `case null`, et c'est pourquoi il répond à `null` par un texte. `sinCasoNulo` ne l'inclut pas, et le même `null` finit dans la `NullPointerException` que l'on voit à la fin. La trace comporte une ligne `at java.base/java.util.Objects.requireNonNull(Objects.java:220)` : le numéro 220 est une ligne du JDK lui-même et peut être différent dans ta version. Ce qui compte, c'est de lire les deux lignes suivantes, qui sont à toi : `sinCasoNulo` à la ligne 17, appelée depuis `main` à la 26. Dans le `revisor`, aucun nul n'arrive à un `switch`, parce que le type scellé rend leur existence impossible ; mais il vaut mieux savoir ce qui se passe quand il y en a.

**`instanceof` avec motif** est le frère du `switch` pour une seule question. `respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400` demande « est-ce un `Respondio` ? », l'appelle `r` si c'est le cas, et poursuit avec la condition. Regarde `fig03_15.java` :

```java
// fig03_15.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

boolean esError(Respuesta respuesta) {
  return respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400;
}

void main() {
  IO.println(esError(new Respuesta.Respondio(503, 95)));
  IO.println(esError(new Respuesta.Respondio(200, 142)));
  IO.println(esError(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_15.java
true
false
false
```

C'est une expression qui donne `true` ou `false`. Remarque comment le `&&` fait office de court-circuit : `r` n'existe, et `r.codigo()` n'est évalué, que si l'`instanceof` était vrai. Utilise-le quand tu as une seule question sur un type ; s'il y a deux formes ou plus à traiter, un `switch` exhaustif est préférable, parce que le compilateur veille sur les cas qui manquent.

### 3.5 L'étape du projet : le `revisor` en plusieurs fichiers

Maintenant tu réorganises le `revisor` avec tout ce que tu as appris. Il y a un changement de forme, et un changement de contenu.

**La forme.** Le `Revisor.java` d'une seule pièce se répartit en sept fichiers, **tous dans le même dossier** `~/curso-java/revisor/`, sans aucun sous-dossier. Question naturelle : comment exécute-t-on quelque chose de plusieurs fichiers sans Maven ? Avec la même commande que toujours, `java Revisor.java`. Depuis Java 22, le lanceur compile le fichier que tu lui donnes et cherche dans le même dossier les autres fichiers `.java` dont celui-ci a besoin ([JEP 458](https://openjdk.org/jeps/458)). Le fichier d'entrée reste compact, avec `void main()` ; les autres sont des types normaux, chacun dans le fichier qui porte son nom. C'est ce qui rend possible un projet à plusieurs pièces **avant** de parler de Maven, qui arrive dans la Leçon 6.

**Le contenu.** Il y a quatre changements, et il vaut la peine de les énumérer parce qu'ils sont la leçon en miniature :

1. Le `String` d'état devient un `enum`.
2. Les deux `null` de « sans réponse » deviennent une interface scellée avec deux formes.
3. Les données d'un service sont réunies dans un `record`.
4. Les deux `switch` parallèles (code et temps) deviennent **un seul**, qui renvoie la forme complète.

Les pièces, une par une :

**`Estado.java`** est l'*enum* de 3.2, avec les trois constantes. Le guide de style de Google écrit les constantes une par ligne.

```java
// fig03_16/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

**`Respuesta.java`** est l'interface scellée de 3.3, telle quelle.

```java
// fig03_16/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

**`Servicio.java`** est le *record* de 3.1 : le nom, l'adresse et le délai maximal, avec le constructeur supplémentaire qui complète la valeur par défaut. C'est la première fois que tu vois un vrai `import` : un fichier normal n'importe rien de lui-même, donc `URI` s'importe avec une ligne.

```java
// fig03_16/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

**`Resultado.java`** rassemble ce que le `revisor` sait d'un service déjà vérifié : lequel c'est, dans quel `Estado` il est resté et quelle a été sa `Respuesta`. Il a une méthode, `detalle()`, qui utilise un `switch` avec des motifs de record : s'il a répondu, le détail est le code sous forme de texte ; sinon, c'est le motif. Sans `default`, et sans nuls.

```java
// fig03_16/Resultado.java
record Resultado(Servicio servicio, Estado estado, Respuesta respuesta) {
  String detalle() {
    return switch (respuesta) {
      case Respuesta.Respondio(int codigo, long _) -> String.valueOf(codigo);
      case Respuesta.SinRespuesta(String motivo) -> motivo;
    };
  }
}
```

**`Clasificador.java`** contient la classification de 3.4.2. C'est une classe `final` (qu'on ne peut pas étendre) dont l'unique travail est de regrouper une fonction et la constante qu'elle utilise. Les méthodes et constantes `static` appartiennent à la classe et non à un objet, et s'appellent avec le nom de la classe devant : `Clasificador.clasificar(respuesta)`. (La Leçon 6 explique `private`, le mot qui ferme cette porte pour que personne ne crée un `Clasificador` qui ne sert à rien.) On voit que `clasificar` **renvoie maintenant un `Estado`**, et non un `String`.

```java
// fig03_16/Clasificador.java
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

**`Formato.java`** réunit ce qui met en forme. Il y a un changement de signature important par rapport à la leçon précédente : `formatearTiempo` reçoit maintenant un `long`, pas un `Long`. Elle n'a plus à décider quoi faire d'un nul, parce que le cas « sans temps » n'arrive pas jusqu'à elle. Celle qui décide est la fonction `tiempo(Respuesta)`, avec un `switch` exhaustif : s'il y a un temps, elle le met en forme, et sinon, elle écrit le tiret. Le type a fait le travail que faisait avant la vérification `if (tiempoMs == null)`. Le modèle des colonnes, qui était écrit deux fois avant (dans `linea` et dans `encabezado`), est maintenant la constante `PATRON_LINEA`. On utilise `import java.util.Locale;` parce que le fichier n'est pas compact.

```java
// fig03_16/Formato.java
import java.util.Locale;

final class Formato {
  static final String PATRON_LINEA = "%-12s%-10s%6s  %s";

  static String formatearTiempo(long tiempoMs) {
    if (tiempoMs < Clasificador.UMBRAL_LENTO_MS) {
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
}
```

**`Revisor.java`** est le seul fichier compact, et le point d'entrée. `simular` est l'**unique `switch`** qui remplace `codigoDe` et `tiempoDe` : il renvoie un `Respondio` avec le code et le temps ensemble, ou un `SinRespuesta` pour tout service qu'il ne connaît pas. `revisar` reçoit un `Servicio`, simule la réponse, la classe et renvoie le `Resultado`. Le `main` crée les quatre services avec `URI.create(...)` et les imprime. (Les quatre sont écrits un par un, parce qu'il n'y a pas encore de listes ; la Leçon 4 corrige cela.)

```java
// fig03_16/Revisor.java
Respuesta simular(String nombre) {
  return switch (nombre) {
    case "catalogo" -> new Respuesta.Respondio(200, 142);
    case "inventario" -> new Respuesta.Respondio(200, 2300);
    case "pagos" -> new Respuesta.Respondio(200, 87);
    default -> new Respuesta.SinRespuesta("sin conexión");
  };
}

Resultado revisar(Servicio servicio) {
  Respuesta respuesta = simular(servicio.nombre());
  return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
}

void main() {
  Servicio catalogo = new Servicio("catalogo", URI.create("https://catalogo.example"));
  Servicio inventario = new Servicio("inventario", URI.create("https://inventario.example"));
  Servicio pagos = new Servicio("pagos", URI.create("https://pagos.example"));
  Servicio reportes = new Servicio("reportes", URI.create("https://reportes.example"));

  IO.println(Formato.encabezado());
  IO.println(Formato.linea(revisar(catalogo)));
  IO.println(Formato.linea(revisar(inventario)));
  IO.println(Formato.linea(revisar(pagos)));
  IO.println(Formato.linea(revisar(reportes)));
}
```

Pour l'exécuter :

```text
$ cd fig03_16
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

C'est **caractère par caractère le même tableau** qu'à la fin de la Leçon 2, avec une différence qui ne se voit pas et qui compte : maintenant, pour que `reportes` sorte comme il est sorti, personne n'a écrit un `null`. Le type `SinRespuesta` est ce qui fait qu'il n'a pas de temps, que son détail est le motif et que son état est `CAIDO`.

Ce qu'il y a de bon dans la conception apparaît quand tu la changes. L'exercice 4 de cette leçon est celui-là.

## L'erreur que tu vas voir

La leçon a un message principal et quatre d'appui.

**L'erreur principale : « the switch expression does not cover all possible input values ».** C'est celle que tu vois quand il manque un cas à un `switch` sur un `enum` ou une interface scellée, et c'est **le même texte dans les deux cas**. Elle apparaît dans `fig03_07.java` (l'*enum*) et dans `fig03_10.java` (l'interface scellée) :

```text
fig03_10.java:9: error: the switch expression does not cover all possible input values
  return switch (respuesta) {
         ^
```

C'est le signe que le compilateur **t'aide** : tu as retiré un cas, ou ajouté une forme, et il y a un endroit que tu dois examiner. Ne le fais pas taire avec un `default`. Lis la ligne que désigne le `^` et décide, pour cette forme, ce que tu veux qu'il se passe.

**Les quatre messages d'appui :**

- `error: cannot assign a value to final variable tiempoMs` : tu as essayé de changer un composant d'un *record*. Ce n'est pas possible. Si tu as besoin d'une autre valeur, crée un autre *record* (`fig03_02.java`).
- `error: class is not allowed to extend sealed class: Respuesta (as it is not listed in its 'permits' clause)` : tu as voulu implémenter une interface scellée avec un type qui n'est pas dans la liste. Ajoute-le au `permits` (`fig03_09.java`).
- `error: this case label is dominated by a preceding case label` : un cas général est avant un cas spécifique. Réordonne du plus spécifique au plus général (`fig03_13.java`).
- `IllegalArgumentException` avec ton message, depuis un constructeur compact : quelqu'un a essayé de créer un *record* avec des données que ton constructeur rejette. La trace dit qui l'a tenté (`fig03_04.java`).

## Ce qui se fait de travers

**Un `default` dans un `switch` sur un type scellé ou un *enum*.** Il fait taire la vérification qui justifie de les avoir écrits, comme l'a montré `fig03_11.java` : le programme a compilé et a mal classé une redirection. Si la nouvelle forme mérite le même traitement qu'une autre, écris son cas à part (ou joins les deux avec une virgule).

**Un `record` avec un tableau, ou avec n'importe quel objet qui peut être modifié.** Il semble immuable et ne l'est pas : on le change par l'accesseur et par la référence d'origine, et son `equals` compare les tableaux par identité. Dans la Leçon 4, tu verras `List.copyOf`.

**Un `String` là où un `enum` convient.** Un état, un type, un niveau. Toute valeur qui sort d'une liste fixe doit être un *enum*, pour qu'une faute de frappe ne soit pas un nouvel état.

**Un `null` là où une forme convient.** Deux `null` parallèles (code, temps) permettent des états impossibles. Si « il n'y a pas de donnée » est une possibilité qui a son propre sens, donne-lui sa propre forme dans un type scellé, avec les données qu'elle a bel et bien (ici, le motif).

**Utiliser `ordinal()` pour autre chose que le débogage.** Le numéro d'une constante change si quelqu'un réordonne l'*enum*.

**Comparer des `enum`s avec `equals`.** Cela fonctionne, mais `==` est la forme idiomatique, et évite une `NullPointerException` si la variable est nulle (`estado.equals(...)` explose avec `null`, `estado == ...` non).

**Un cas général avant un cas spécifique.** Le compilateur le détecte, mais c'est une erreur qu'il vaut mieux comprendre et pas seulement corriger : l'ordre des cas fait partie du sens.

**Mettre de longues validations dans le constructeur compact.** C'est l'endroit pour normaliser et rejeter ce qui est clairement absurde. La logique métier va ailleurs.

## Exercices

### Exercice 1 — Quelle égalité te donne un `record`

Déclare `record Servicio(String nombre, URI url, int timeoutMs) {}` dans un fichier compact et crée trois objets : `a` et `b` avec les mêmes valeurs (5000 de délai maximal) et `c` identique à eux mais avec 1000. **Avant d'exécuter**, prédis le résultat de `a == b`, `a.equals(b)`, `a.equals(c)`, `a.hashCode() == b.hashCode()` et de l'impression de `c`. Exécute et compare.

### Exercice 2 — Fermer une porte de l'immuabilité

Prends `fig03_05.java` et ajoute à `Ruta` un constructeur compact qui garde une **copie** du tableau (les tableaux ont une méthode `clone()` qui renvoie une copie). Exécute de nouveau. Lequel des deux changements a cessé d'affecter le *record* ? Lequel continue de se produire ? Que te faut-il pour fermer l'autre porte ?

### Exercice 3 — Un nouvel état dans l'`enum`

Dans l'`enum` de `fig03_06.java`, ajoute une quatrième valeur, `DEGRADADO`, entre `LENTO` et `CAIDO`, et ne touche à rien d'autre. Essaie d'exécuter. Que dit le compilateur et dans quelle fonction ? Ensuite corrige-le avec le message qui te paraît raisonnable pour le nouvel état, et écris dans ton journal de bord ce qui se serait passé si `Estado` avait été un `String`.

### Exercice 4 — Le projet : une forme de réponse de plus

Dans le projet de 3.5, ajoute une troisième forme à `Respuesta` : `record Redirigida(int codigo, String destino)`, qui représente un service qui a répondu avec un code 3xx et indique où envoyer. Fais-le dans `Respuesta.java` (la forme et le `permits`) et **ne touche à rien d'autre**. Exécute `java Revisor.java`. Note **chaque endroit** que le compilateur t'oblige à corriger, un par un : corrige-les et réexécute jusqu'à ce que cela compile. Décide ce que tu veux qu'il se passe avec une redirection : quel état lui correspond, quel temps affiche-t-elle, quel détail ?

## Solutions

### Solution 1

```text
false
true
false
true
Servicio[nombre=pagos, url=https://pagos.example, timeoutMs=1000]
```

`a == b` est `false` : ce sont deux objets distincts. `a.equals(b)` est `true` : le `equals` du *record* compare composant par composant (et le `equals` de `URI`, à son tour, compare le contenu). `a.equals(c)` est `false` : ils diffèrent par `timeoutMs`. Les `hashCode` de `a` et `b` coïncident parce que `equals` les déclare égaux, et le *record* garantit cette cohérence. Et `c` s'imprime avec le format habituel.

### Solution 2

Le constructeur compact qui garde la copie :

```java
record Ruta(String nombre, String[] etapas) {
  Ruta {
    etapas = etapas.clone();
  }
}
```

Avec lui, la sortie est :

```text
tras cambiar el original: [conectar, esperar]
tras cambiar por el accesor: [conectar, CAMBIADO DESDE EL ACCESOR]
```

Le premier trou s'est fermé : changer le tableau d'origine n'affecte plus le *record*, parce qu'il garde sa propre copie. Le second reste ouvert : l'accesseur renvoie la référence à la copie qu'il garde, et la changer de l'extérieur la modifie. Pour le fermer, il faudrait redéfinir l'accesseur pour qu'il renvoie une autre copie ; dans un *record*, un accesseur écrit à la main doit être `public` (la Leçon 6 explique ce que cela signifie), et même ainsi `equals` continuerait de comparer les tableaux par identité. La conclusion pratique est qu'**un tableau est un mauvais composant**. Dans la leçon suivante, tu verras `List.copyOf`, qui ferme bien les deux portes.

### Solution 3

Le compilateur rejette le programme dans `aviso`, la fonction avec le `switch` :

```text
error: the switch expression does not cover all possible input values
  return switch (estado) {
         ^
```

Il manque le cas `DEGRADADO`. Cela se corrige en ajoutant `case DEGRADADO -> "revisar ya";` (ou le message que tu choisis). Avec un `String`, rien n'aurait averti : `"DEGRADADO"` serait un texte de plus, `aviso` tomberait dans le `default` (qui aurait été obligatoire) et le service dégradé recevrait l'avertissement réservé aux inconnus. L'*enum* a converti cet oubli en erreur de compilation.

### Solution 4

Avec la nouvelle forme ajoutée seulement dans `Respuesta.java` :

```java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}
```

le compilateur se plaint d'abord de `Clasificador.java` (le chemin complet de ton dossier apparaît devant le nom du fichier ; ici je l'abrège) :

```text
Clasificador.java:6: error: the switch expression does not cover all possible input values
    return switch (respuesta) {
           ^
```

On ajoute un cas (par exemple, `case Respuesta.Redirigida _ -> Estado.ARRIBA;` : une redirection est une réponse valide), et en réexécutant, le suivant est `Formato.java:15` (la fonction `tiempo`), puis `Resultado.java:4` (la méthode `detalle`). **Ce sont trois endroits** : la classification, le temps affiché et le détail. Dans mon exécution, le compilateur les a montrés un par un, dans cet ordre. Tu peux décider autrement ce que fait chacun : un état `ARRIBA` est raisonnable parce que le service a répondu, le temps peut être le tiret (nous n'en avons pas dans la forme) et le détail peut être le code avec la destination. Ce qui compte n'est pas la décision, mais que le compilateur t'a **obligé à la prendre** à chaque endroit. Avec deux `null` parallèles, rien ne t'aurait averti.

## Comment savoir que j'ai réussi

- [ ] `java fig03_01.java` imprime `Punto[nombre=catalogo, tiempoMs=142]` et tu peux expliquer pourquoi `a == b` est `false` et `a.equals(b)` est `true`.
- [ ] `java fig03_05.java` montre les deux modifications de la `Ruta`, et tu peux dire pourquoi elles se produisent.
- [ ] Dans `fig03_10.java`, tu vois l'erreur « does not cover all possible input values », et tu sais quel cas manquait.
- [ ] Dans `fig03_11.java`, tu vois que le `default` classe une redirection comme `CAIDO`, et tu peux dire pourquoi c'est pire que l'erreur précédente.
- [ ] Depuis `fig03_16/`, `java Revisor.java` imprime exactement le tableau de quatre lignes de la Leçon 2 : `catalogo` ARRIBA 142ms, `inventario` LENTO 2.3s, `pagos` ARRIBA 87ms, `reportes` CAIDO — « sin conexión » (« sans connexion »).
- [ ] Après avoir ajouté `Redirigida`, le compilateur t'a montré trois endroits et tu les as corrigés un par un.
- [ ] Tu peux expliquer avec tes mots pourquoi le `revisor` de cette leçon n'a pas un seul `null`.
- [ ] Tu as noté dans ton journal de bord l'état impossible que le type scellé t'a empêché d'écrire, et ce qui s'est passé quand tu as ajouté un nouveau cas.

## Résumé

Réponds sans regarder la leçon :

1. Quelles quatre choses le compilateur génère-t-il pour un `record` ? Que compare son `equals` ?
2. Quelle différence y a-t-il entre un constructeur compact et un constructeur supplémentaire, et à quoi sert chacun ?
3. Pourquoi un `record` contenant un tableau n'est-il pas immuable ? Que fait-on dans la Leçon 4 pour le corriger ?
4. Pourquoi compare-t-on un `enum` avec `==` ? Quel avantage y a-t-il à écrire un `switch` sur un `enum` sans `default` ?
5. Que dit `sealed ... permits` et que se passe-t-il si tu essaies d'implémenter l'interface avec un type qui n'est pas dans la liste ?
6. Qu'est-ce qu'une garde `when`, un motif de record et le motif `_` ? Donne un exemple de chacun avec le `revisor`.
7. Pourquoi l'ordre des cas compte-t-il ? Que se passe-t-il si tu mets le cas général avant ?
8. Que fait un `switch` sur une référence nulle s'il n'a pas de `case null` ?
9. Qu'est-ce qui a changé dans la signature de `formatearTiempo` entre la Leçon 2 et la 3, et qu'est-ce qui l'a rendu possible ?
10. Pourquoi un `default` dans un `switch` sur un type scellé est-il un risque ?

## Pour aller plus loin

- [Les tutoriels Java, « Record Classes »](https://docs.oracle.com/en/java/javase/25/language/records.html) : les *records* expliqués avec plus d'exemples, dans la documentation officielle (en anglais). Consulté le 7 octobre 2026.
- [JEP 409, « Sealed Classes »](https://openjdk.org/jeps/409) : le pourquoi et les règles complètes de `sealed`, `permits`, `final` et `non-sealed`. Consulté le 7 octobre 2026.
- [JEP 441, « Pattern Matching for switch »](https://openjdk.org/jeps/441) et [JEP 440, « Record Patterns »](https://openjdk.org/jeps/440) : les règles exactes de l'exhaustivité, de la domination et des gardes. Consulté le 7 octobre 2026.
- [Les tutoriels Java, « Enum Types »](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html) : les *enums* avec champs, constructeurs et méthodes. Consulté le 7 octobre 2026.

---

**Précédente :** [Leçon 2 — Types, chaînes et contrôle de flux](02-tipos-cadenas-control.md) ·
**Suivante :** [Leçon 4 — Collections, génériques et streams](04-colecciones-genericos-streams.md)
