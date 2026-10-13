# Leçon 9 — Concurrence : tout vérifier en même temps

**Durée :** 2 × 90 min (deux séances ; la coupure se situe entre les sections 9.4 et 9.5)

**Ce que tu construis :** le `revisor` concurrent : il interroge tous les services en même temps, avec une limite que tu fixes, et livre le rapport dans le même ordre que toujours

**Ce que tu apprends :** threads de plateforme et threads virtuels ; un thread virtuel par tâche et jamais dans un *pool* ; `ExecutorService` comme ressource que l'on ferme ; `invokeAll`, l'ordre stable et `ExecutionException` ; `Semaphore` pour limiter ; une situation de compétition provoquée et mesurée ; `ConcurrentHashMap` et les types atomiques ; `ScopedValue` ; et la concurrence structurée comme « ce qui vient »

**D'où tu viens.** Tu arrives avec le projet de la [Leçon 8](08-http-y-json.md) : un `revisor` sous Maven, avec `HttpClient`, Jackson, la configuration validée dans `servicios.json` et 25 tests au vert. Il fait une vraie requête par service, mesure combien de temps elle met, et ne plante pas quand un service ne répond pas. Mais il les fait **l'une après l'autre** : avec les quatre services canoniques, il met la somme de leurs temps, environ 2.5 secondes, et avec vingt services d'une demi-seconde chacun, il en mettrait dix. Cette lenteur n'est pas celle du programme, c'est celle de sa forme : le `for` de `revisarTodos` attend la réponse de `catalogo` avant de poser sa question à `inventario`, alors qu'aucun des deux n'a besoin de l'autre. Aujourd'hui, tu la lui ôtes. Tu travailles dans ton dossier `~/curso-java/revisor/`.

**Ce que cette leçon ne fait pas.** Elle n'ajoute pas d'options de ligne de commande : la limite de concurrence est une constante jusqu'à la [Leçon 10](10-el-programa-terminado.md), où elle devient `--concurrencia`. Elle ne change pas non plus la façon de vérifier un service : `revisar` reste tel quel, avec ses délais maximaux et ses trois motifs de panne. La seule chose qui change, c'est **qui appelle `revisar` et combien de fois en même temps**.

**À propos des exécutions.** Les figures 9.1 à 9.8 sont des fichiers compacts et s'exécutent avec `java figNN_MM.java` depuis le dossier de la leçon ; elles n'ont pas besoin du laboratoire. La figure 9.9 est le projet, et elle en a besoin : dans un autre terminal, depuis `~/curso-java/`, laisse en marche `java Laboratorio.java` (il écoute sur le port 8089 ; le port 8088 reste vide exprès, c'est celui de `reportes`). Toutes les sorties ont été obtenues avec Temurin 25.0.4 sur une machine de 10 cœurs. **Les temps que tu vois dans cette leçon dépendent de ta machine** : ce qui se répète partout, ce sont les proportions, pas les millisecondes. Les chiffres qui changent d'une exécution à l'autre (comme le résultat d'une situation de compétition) sont présentés comme un fragment, avec une exécution réelle, et non comme une sortie vérifiée.

## À la fin, tu seras capable de

- Expliquer ce qu'est un thread virtuel, en quoi il se distingue d'un thread de plateforme et pourquoi un programme qui attend beaucoup (comme le `revisor`) en tire parti.
- Lancer une tâche pour chaque service avec `Executors.newVirtualThreadPerTaskExecutor()` dans un `try-with-resources`, et dire pourquoi on n'utilise pas un *pool* de threads virtuels.
- Utiliser `invokeAll` pour attendre toutes les tâches et récupérer leurs résultats dans l'ordre d'entrée, et lire une `ExecutionException` jusqu'à sa cause.
- Limiter combien de tâches avancent en même temps avec un `Semaphore`, en le libérant toujours dans un `finally`.
- Provoquer une situation de compétition, mesurer combien on perd, et la corriger avec `ConcurrentHashMap.merge` ou avec un compteur atomique.
- Expliquer pourquoi le `revisor` n'a pas de situations de compétition (il ne partage pas d'état mutable), et à quoi servent `ScopedValue` et la concurrence structurée.
- Mesurer avec l'horloge l'effet de la limite sur vingt services et expliquer pourquoi une limite de 10 prend le double d'une limite de 20.

## Le pourquoi avant le comment

Imagine une cuisine avec un seul cuisinier qui prépare quatre plats. Il met le premier au four et reste à le regarder vingt minutes jusqu'à ce qu'il soit prêt ; ce n'est qu'alors qu'il commence le deuxième. Personne ne dirait que ce cuisinier est lent de ses mains. Il est lent **parce qu'il attend sans rien faire d'autre**. Le `revisor` de la leçon 8 est ce cuisinier : quand il interroge `inventario`, qui met 2.3 secondes à répondre, le programme reste immobile 2.3 secondes, avec le processeur presque endormi, alors que `pagos` et `catalogo` pourraient répondre en même temps.

La solution est aussi de cuisine : mettre plusieurs plats au four à la fois. En programmation, cela s'appelle la **concurrence** : plusieurs tâches en cours en même temps, de sorte que le temps qu'une passe à attendre, une autre le met à profit. (Ce n'est pas la même chose que le **parallélisme**, qui consiste à avoir plusieurs processeurs qui travaillent littéralement en même temps. La concurrence est une façon d'organiser le programme ; le parallélisme, quelque chose que l'équipement peut offrir ou non. Dans cette leçon, tu verras les deux, mais le bénéfice du `revisor` vient surtout de la première.)

Le faire en Java a toujours eu un obstacle, et cette leçon existe à cause de la façon dont Java l'a levé. Pendant plus de vingt ans, l'unité de concurrence a été le **thread du système d'exploitation** : un thread est une ligne d'exécution indépendante à l'intérieur du programme, et chaque thread de Java était un thread du système, avec sa propre pile et ses ressources du système d'exploitation, et un coût réel. (Ce qu'il n'avait pas en propre, c'était le tas : les objets y vivent et tous les threads les partagent, ce qui est justement l'origine des situations de compétition de 9.5.) En créer dix mille pour dix mille requêtes n'était pas viable, donc la pratique standard était d'avoir quelques threads « de service » (un *pool*) et de se partager les tâches, avec des files, des limites et beaucoup de code pour que personne ne reste à attendre inutilement. Depuis Java 21, la plateforme offre autre chose : des **threads virtuels**, si bon marché que chaque tâche peut avoir le sien, et avec eux le code redevient ce qu'il était dans la cuisine d'un seul cuisinier —une ligne après l'autre, chaque étape attendant la précédente—, sauf que maintenant ce sont de nombreux cuisiniers bon marché.

Cette leçon a deux séances. La **première** (9.1 à 9.4) enseigne les threads virtuels, `invokeAll` et le sémaphore, et se termine par une mesure : le `revisor` vérifiant vingt services en même temps. La **seconde** (9.5 à 9.8) enseigne ce qu'il faut savoir pour que la concurrence ne te trahisse pas : l'état partagé, la situation de compétition qui le rend dangereux, les valeurs à portée limitée, et ce qui vient dans la plateforme. C'est la leçon qui change le plus ce que tu sais de Java, et celle qui te servira le plus : tout serveur moderne en Java s'écrit avec ces idées.

## Les concepts

### 9.1 Deux sortes de thread

#### 9.1.1 Ce qu'est un thread et ce qu'il coûte

Un **thread** est une séquence d'instructions qui avance de son côté. Ton programme en a toujours au moins un : celui qui exécute `main`. Avec plus de threads, plusieurs séquences avancent « en même temps » : le système d'exploitation répartit le temps des processeurs entre eux, et quand l'un se bloque —en attendant une réponse du réseau, par exemple— il donne la main à un autre.

Jusqu'à Java 20, chaque `Thread` était exactement cela : **un thread du système d'exploitation**, avec sa propre pile d'appels et sa propre inscription dans les tables du système. On les appelle maintenant des **threads de plateforme** (*platform threads*). Ils sont puissants et chers : le système impose un plafond au nombre de threads, chacun réserve de la mémoire pour sa pile, et passer de l'un à l'autre coûte. C'est pourquoi un programme serveur ne pouvait pas se permettre un thread par requête quand il y avait des milliers de requêtes, et la technique consistait à les partager avec un *pool*.

Un **thread virtuel** est autre chose, bien qu'on s'en serve de la même façon. C'est aussi un `Thread` —le même type, les mêmes méthodes, `join`, `interrupt`—, mais il est géré par la machine virtuelle Java au lieu du système d'exploitation. Un thread virtuel n'est pas attaché à un thread du système : la JVM a un petit groupe de threads de plateforme, appelés **threads porteurs** (*carrier threads*), et « monte » sur eux les threads virtuels qui ont quelque chose à faire. Quand un thread virtuel arrive à une opération qui bloque —lire sur un socket, dormir, attendre un sémaphore—, la JVM le **démonte** : elle sauvegarde son état dans la mémoire du tas, libère le porteur et monte un autre thread virtuel qui, lui, peut avancer. Quand la réponse arrive, le thread virtuel se remonte sur un porteur quelconque. C'est la même idée que le cuisinier qui ne reste pas à regarder le four, réalisée par la plateforme au lieu de toi. La [documentation officielle des threads virtuels](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html) le décrit en ces termes, et la spécification du changement est [JEP 444](https://openjdk.org/jeps/444), définitive depuis Java 21.

La figure 9.1 les met côte à côte. Elle demande un thread de plateforme et un thread virtuel avec le même constructeur de threads (`Thread.ofPlatform()` et `Thread.ofVirtual()`), leur donne un nom, les démarre, et chacun dit ce qu'il est :

```java
// fig09_01.java
void main() throws InterruptedException {
  Thread plataforma =
      Thread.ofPlatform()
          .name("de-plataforma")
          .start(() -> describir("de-plataforma"));
  plataforma.join();

  Thread virtual =
      Thread.ofVirtual()
          .name("virtual-1")
          .start(() -> describir("virtual-1"));
  virtual.join();
}

void describir(String nombre) {
  Thread hilo = Thread.currentThread();
  IO.println(nombre + ": virtual=" + hilo.isVirtual() + ", demonio=" + hilo.isDaemon());
}
```

Les figures de cette leçon n'ont pas d'`import` : un fichier compact importe de lui-même tout ce qu'exporte `java.base`, ce qui inclut `java.util.concurrent`. La méthode `join()` fait que `main` attend que le thread termine ; sans elle, le programme pourrait se terminer avant que le thread n'imprime. Voici ce qu'elle imprime :

```text
$ java fig09_01.java
de-plataforma: virtual=false, demonio=false
virtual-1: virtual=true, demonio=true
```

Trois données en deux lignes. Le thread de plateforme est `virtual=false` ; l'autre, `virtual=true`. Et le virtuel est **démon** (`demonio=true`, « démon » en espagnol) : un thread démon ne maintient pas la JVM en vie, de sorte que si `main` se termine et qu'il ne reste que des threads virtuels, le programme s'arrête sans les attendre. C'est une différence qui mord : si tu lances un thread virtuel « pour qu'il travaille en arrière-plan » et que `main` se termine, tu le coupes. (C'est pourquoi l'exécuteur de 9.2 se ferme en attendant ses tâches.)

Tu peux voir la mécanique des porteurs sur n'importe quel thread virtuel. Imprimer `Thread.currentThread()` depuis l'un d'eux donne quelque chose comme :

```text
VirtualThread[#26]/runnable@ForkJoinPool-1-worker-1
```

Après l'arobase se trouve le thread porteur sur lequel il tournait à ce moment-là. En lançant vingt mille threads virtuels et en notant leur porteur, sur ma machine sont apparus exactement dix noms, de `worker-1` à `worker-10` : autant que de cœurs. Vingt mille threads « de Java » répartis sur dix threads « du système ».

#### 9.1.2 Ce que l'on gagne : de l'échelle, pas de la vitesse

La figure 9.2 met la différence en chiffres, avec une tâche qui ne fait rien d'autre qu'attendre 200 millisecondes (un service qui met du temps à répondre). Elle en lance mille deux fois : une fois dans un *pool* classique de 50 threads de plateforme et une fois avec un thread virtuel par tâche. Elle mesure chacune avec l'horloge du système (`System.nanoTime()`) :

```java
// fig09_02.java
static final int TAREAS = 1_000;
static final int ESPERA_MS = 200;

void esperar() {
  try {
    Thread.sleep(ESPERA_MS);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
}

long medirMs(ExecutorService ejecutor) {
  long inicio = System.nanoTime();
  try (ejecutor) {
    for (int i = 0; i < TAREAS; i++) {
      ejecutor.submit(this::esperar);
    }
  }
  return (System.nanoTime() - inicio) / 1_000_000;
}

void main() {
  long conPool = medirMs(Executors.newFixedThreadPool(50));
  long conVirtuales = medirMs(Executors.newVirtualThreadPerTaskExecutor());
  IO.println("pool de 50 hilos de plataforma, más de 3 s: " + (conPool > 3000));
  IO.println("un hilo virtual por tarea, menos de 1 s: " + (conVirtuales < 1000));
}
```

Quelques détails de lecture avant de l'exécuter. `try (ejecutor) { ... }` utilise une variable qui existe déjà comme ressource du `try-with-resources` (Java le permet depuis la 9, tant que la variable n'est pas réaffectée) ; c'est la même idée que dans la leçon 5, et en sortant du bloc on appelle `close()`. `this::esperar` est une référence à une méthode de cette même classe, que Java convertit en tâche. Et les deux lignes finales impriment un booléen, pas les millisecondes, pour que la sortie soit toujours la même :

```text
$ java fig09_02.java
pool de 50 hilos de plataforma, más de 3 s: true
un hilo virtual por tarea, menos de 1 s: true
```

Les vraies millisecondes, tu les vois si tu imprimes les variables `conPool` et `conVirtuales`. Sur trois exécutions de suite, j'ai obtenu 4,152, 4,148 et 4,161 ms pour le *pool*, et 221, 214 et 206 ms pour les threads virtuels. L'arithmétique explique le premier : mille tâches sur 50 threads font 20 vagues, et 20 vagues de 200 ms font 4 secondes. Avec un thread virtuel par tâche, il n'y a pas de vagues : les mille attendent ensemble, et le temps total est celui d'une seule attente, plus le coût du démarrage. Dans une autre mesure, cent mille threads virtuels dormant une seconde chacun se sont terminés en 1.4 seconde.

Il convient ici de couper court à un malentendu qui ruine plus d'une conception. La documentation d'Oracle le dit sans détour : les threads virtuels **ne sont pas des threads plus rapides** ; ils n'exécutent pas le code plus vite qu'un thread de plateforme. Ils donnent de l'**échelle** (plus de tâches en cours en même temps), pas de la **vitesse** (moins de temps par tâche). Et ils servent aux tâches qui passent presque tout leur temps à **attendre**, pas au travail de processeur : si chaque tâche était une longue factorisation, dix mille threads virtuels ne termineraient pas avant dix, parce que les cœurs sont les mêmes. Le `revisor` est le cas idéal : chaque tâche envoie une requête et attend.

Une donnée de plus, qui t'épargne des précautions que tu liras dans des livres plus anciens : avec Java 21, il y avait un problème appelé *pinning* (un thread virtuel qui bloquait à l'intérieur d'un bloc `synchronized` restait collé à son porteur et ne le libérait pas). Depuis Java 24, [cela n'arrive plus](https://openjdk.org/jeps/491) dans ces blocs ; le conseil « évite `synchronized` avec les threads virtuels » qui circule encore date d'une autre époque.

### 9.2 Un thread virtuel par tâche

#### 9.2.1 L'exécuteur qui ferme

Créer des threads à la main avec `Thread.ofVirtual().start(...)` sert à comprendre, pas à organiser du travail. Pour l'organiser, on utilise un **exécuteur**, un objet auquel tu remets des tâches et qui se charge de les exécuter. La fabrique qui nous intéresse tient en une ligne :

```java
try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
  // aquí se mandan tareas con ejecutor.submit(...) o ejecutor.invokeAll(...)
}
```

Son nom dit tout : *virtual thread per task executor*, « exécuteur à un thread virtuel par tâche ». Chaque tâche que tu lui envoies reçoit un nouveau thread virtuel, qui meurt quand la tâche se termine. Il n'y a pas de file, pas de taille de *pool*, rien à configurer. Et comme `ExecutorService` est `AutoCloseable` depuis Java 19, le `try-with-resources` fait l'essentiel : en sortant du bloc il appelle `close()`, qui **attend que toutes les tâches soient terminées** avant de continuer. C'est la garantie que n'avaient pas les anciens exécuteurs, où oublier le `shutdown()` laissait des threads vivants et un programme qui ne se terminait pas. Ici, le bloc `try` est une clôture : ce que tu commences à l'intérieur se termine à l'intérieur.

La règle de cette section tient en une phrase, et c'est celle que le plan du cours met en gras : **un thread virtuel par tâche, jamais dans un *pool***. Un *pool* existe pour répartir une ressource rare —les threads de plateforme— entre de nombreuses tâches. Les threads virtuels ne sont pas rares : on peut en avoir des millions. Si tu leur mets un *pool* par-dessus, tu recrées une rareté artificielle, avec les mêmes files et les mêmes plafonds, et tu perds justement ce qui les rendait utiles. Le guide officiel le résume ainsi : les threads virtuels ne doivent pas représenter une ressource partagée mais **une tâche**. Si ce que tu veux, c'est limiter combien de tâches avancent en même temps, il existe un outil fait pour cela, et tu le vois en 9.4.

#### 9.2.2 Pourquoi un `send` qui bloque est acceptable

Regarde de nouveau comment le `revisor` vérifie un service depuis la leçon 8 : `cliente.send(peticion, BodyHandlers.discarding())` **bloque** : le thread qui l'appelle reste à attendre jusqu'à ce que la réponse arrive ou que le délai expire. Sur un thread de plateforme, c'était un luxe coûteux ; c'est pourquoi la programmation concurrente en Java a passé des années à chercher des façons de ne pas bloquer (*callbacks*, `CompletableFuture`, bibliothèques réactives), avec un code qui se lit la tête à l'envers. Sur un thread virtuel, attendre une entrée ou sortie comme celle-ci **coûte très peu** : la JVM démonte le thread pendant qu'il attend et utilise le porteur pour un autre. Peu n'est pas rien : il y a des cas où le thread virtuel reste **épinglé** à son porteur et l'occupe pendant qu'il attend (par exemple, pendant un appel à du code natif), et ce qu'il y a de l'autre côté, connexions, services, mémoire, reste limité : un thread bon marché ne rend pas bon marché le serveur que tu interroges. C'est pourquoi `revisar` ne change pas d'une ligne dans cette leçon. Il reste la méthode linéaire de la leçon 8 : il construit la requête, l'envoie, attend, classe. La concurrence n'est pas à l'intérieur de `revisar` ; elle est dans la façon de l'appeler.

Deux choses de cet appel sont à garder à l'esprit. D'abord, le `HttpClient` **se partage** : la documentation de la classe dit qu'une instance est immuable et sert à envoyer de nombreuses requêtes, donc le `revisor` n'en crée qu'un et toutes les tâches l'utilisent ; créer un client par requête, ce serait jeter ses connexions réutilisables. Ensuite, le client est aussi `AutoCloseable`, et l'ordre de fermeture compte : dans un `try-with-resources` à deux ressources, elles se ferment **dans l'ordre inverse de leur déclaration**. C'est pourquoi le client se déclare en premier et l'exécuteur ensuite : à la sortie, on ferme l'exécuteur (il attend toutes les tâches) et *ensuite* le client. À l'envers, tu fermerais le client avec des requêtes en vol.

### 9.3 `invokeAll` et l'ordre stable

#### 9.3.1 `Callable`, `Future` et attendre toutes les tâches

`submit` accepte deux sortes de tâche. Un `Runnable` ne renvoie rien et ne peut pas lever d'exception contrôlée. Un **`Callable<T>`** renvoie une valeur de type `T` et peut lever n'importe quelle exception : c'est ce dont a besoin `revisar`, qui renvoie un `Resultado` et peut lever `InterruptedException` (la leçon 5 a expliqué pourquoi celle-là ne s'avale jamais). Ce que tu reçois en envoyant une tâche est un **`Future<T>`** : une promesse de résultat. `futuro.get()` attend —si besoin— que la tâche se termine et remet sa valeur.

La méthode qui unit les deux choses est `invokeAll` : elle reçoit une collection de `Callable`, les exécute toutes, **attend qu'elles soient toutes terminées** et renvoie une `List<Future<T>>`. L'important est dans la documentation et c'est ce qui la rend adaptée au `revisor` : la liste qu'elle renvoie a **le même ordre que celle que tu lui as donnée**, quelle que soit celle qui a terminé la première. La figure 9.3 le vérifie avec trois tâches qui mettent 300, 50 et 150 ms ; la plus rapide est la deuxième de la liste, et pourtant elle sort deuxième :

```java
// fig09_03.java
String tarea(String nombre, long esperaMs) throws InterruptedException {
  Thread.sleep(esperaMs);
  return nombre;
}

void main() throws Exception {
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Callable<String>> tareas =
        List.of(
            () -> tarea("lenta", 300),
            () -> tarea("rapida", 50),
            () -> tarea("mediana", 150));

    for (Future<String> futuro : ejecutor.invokeAll(tareas)) {
      IO.println("en el orden de la lista: " + futuro.get());
    }

    List<Callable<String>> conFallo =
        List.of(
            () -> tarea("buena", 10),
            () -> {
              throw new IllegalStateException("fallé a propósito");
            });
    List<Future<String>> futuros = ejecutor.invokeAll(conFallo);
    IO.println("invokeAll regresó; la tarea 2 terminó: " + futuros.get(1).isDone());
    try {
      futuros.get(1).get();
    } catch (ExecutionException e) {
      IO.println("get() lanza " + e.getClass().getSimpleName());
      IO.println("la causa es " + e.getCause());
    }
  }
}
```

Suis la figure par parties. `tarea` dort et renvoie son nom. La liste de trois `Callable` s'écrit avec des lambdas, et `invokeAll` les exécute toutes avec des threads virtuels. Le premier `for` parcourt les `Future` dans l'ordre. Ensuite on construit une seconde liste où une tâche **lève une exception**, et on voit quelque chose d'important : `invokeAll` revient normalement, la tâche apparaît comme terminée, et l'échec n'explose que lorsque tu demandes sa valeur avec `get()`. Voici l'exécution :

```text
$ java fig09_03.java
en el orden de la lista: lenta
en el orden de la lista: rapida
en el orden de la lista: mediana
invokeAll regresó; la tarea 2 terminó: true
get() lanza ExecutionException
la causa es java.lang.IllegalStateException: fallé a propósito
```

Les trois premières lignes sortent dans l'ordre de la liste (`lenta`, `rapida`, `mediana`), pas dans l'ordre où les tâches ont terminé (qui a été `rapida`, `mediana`, `lenta`). Pour le `revisor`, c'est ce qui garantit le point 3 de « Comment savoir que tu as terminé » : le rapport sort **toujours dans le même ordre**, celui de la configuration, même si `inventario` met deux secondes et `pagos` quatre-vingt-sept millisecondes. Si tu récupérais les résultats au fur et à mesure de leur arrivée, l'ordre changerait à chaque exécution et comparer deux rapports serait un supplice.

#### 9.3.2 `ExecutionException` : l'exception d'un autre thread

Quand une tâche lève une exception dans son thread, cette exception ne peut pas remonter la pile de celui qui l'a envoyée —ce sont des piles différentes—. Java la garde à l'intérieur du `Future` et, lorsqu'on appelle `get()`, la remet **enveloppée** dans une `ExecutionException`. Les deux dernières lignes de la figure le montrent : le type que tu attrapes est `ExecutionException`, mais ce qui compte, c'est sa **cause**, `getCause()`, qui est l'exception d'origine (`IllegalStateException: fallé a propósito`, « j'ai échoué exprès »). Qui l'attrape et n'imprime que le message de l'enveloppe perd justement l'information dont il a besoin. C'est le même principe que la leçon 5, celui des exceptions personnalisées avec leur cause : une exception enveloppée se déballe, elle ne se jette pas.

Cela t'oblige à décider quoi en faire, et la décision du `revisor` est délibérée. Il y a deux sortes de problème. Un **service en panne** est un résultat attendu : `revisar` l'attrape et le convertit en un `Resultado` avec l'état `CAIDO` et son motif, et ne laisse jamais échapper une `IOException`. C'est pourquoi aucune tâche n'échoue à cause du réseau, et le rapport sort complet même si la moitié du monde est en panne. Une **erreur du programme** (un `NullPointerException`, une erreur de logique) est autre chose : si une tâche échoue ainsi, quelque chose ne va pas dans le code, et le cacher derrière un « service en panne » serait mentir. Le `revisor` la relance comme `IllegalStateException` avec la cause d'origine jointe. En une phrase : **un service en panne ne fait pas tomber le rapport ; une erreur du programme ne se cache pas**.

Un dernier détail du contrat : si le thread qui attend dans `invokeAll` est interrompu, la méthode **annule les tâches qui ne sont pas terminées** et lève `InterruptedException`. C'est pourquoi la signature de `revisarTodos` déclare `throws InterruptedException` et ne l'avale pas.

### 9.4 Limiter avec `Semaphore`

#### 9.4.1 Pourquoi limiter si les threads sont bon marché

Si les threads virtuels sont presque gratuits, pourquoi limiter ? Parce que la ressource rare n'est plus le thread ; c'est **ce qu'il y a de l'autre côté**. Vingt services interrogés en même temps, c'est une courtoisie raisonnable. Vingt mille services interrogés en même temps —ou vingt requêtes simultanées contre le même serveur— peuvent faire tomber un service fragile, épuiser les ports du système ou déclencher l'alarme d'un fournisseur. La limite protège la **destination**, pas la JVM. Et cette décision ne devrait pas dépendre du nombre de threads qu'un *pool* a par hasard.

Java a un outil pensé pour cela depuis vingt ans : le **`Semaphore`**. Un sémaphore garde un nombre de **permis**. Qui veut avancer appelle `acquire()` (prendre un permis) ; s'il n'y en a pas, il **attend** qu'un autre le rende avec `release()`. Avec `new Semaphore(2)`, au plus deux tâches se trouvent dans la zone protégée ; les autres font la queue. La figure 9.4 lance six tâches de 100 ms avec un sémaphore de deux permis, et enregistre le maximum de tâches simultanées avec un `AtomicInteger` (un entier qui peut être modifié depuis plusieurs threads sans perdre de comptes ; il revient en 9.5) :

```java
// fig09_04.java
final AtomicInteger activas = new AtomicInteger();
final AtomicInteger maximo = new AtomicInteger();

void trabajar() throws InterruptedException {
  int ahora = activas.incrementAndGet();
  maximo.accumulateAndGet(ahora, Math::max);
  Thread.sleep(100);
  activas.decrementAndGet();
}

void main() throws Exception {
  var semaforo = new Semaphore(2);
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 6; i++) {
      ejecutor.submit(
          () -> {
            semaforo.acquire();
            try {
              trabajar();
            } finally {
              semaforo.release();
            }
            return null;
          });
    }
  }
  IO.println("tareas lanzadas: 6, máximo al mismo tiempo: " + maximo.get());
}
```

Observe la forme de la tâche : `acquire()`, puis un `try` avec le travail, et `release()` dans le **`finally`**. Ce n'est pas du style : c'est une condition d'exactitude. Si le travail lève une exception et que le `release()` n'est pas dans un `finally`, ce permis est perdu pour toujours ; après assez d'échecs, tous les permis sont perdus et les tâches restantes attendent éternellement, un programme bloqué sans aucune erreur en vue. L'exécution :

```text
$ java fig09_04.java
tareas lanzadas: 6, máximo al mismo tiempo: 2
```

Six tâches lancées, jamais plus de deux en même temps. Et remarque ce que le sémaphore n'a **pas** fait : il n'a pas limité les threads. Les six tâches ont leur thread virtuel dès le début ; quatre dorment dans `acquire()` et ne coûtent rien. Le guide officiel d'Oracle recommande justement cela —un sémaphore pour limiter— et explique que son effet ressemble à celui d'un *pool* de taille fixe, parce que dans les deux cas les tâches en trop font la queue, mais sans réserver de threads pour cela.

#### 9.4.2 Pourquoi pas un *pool* de threads virtuels

La tentation est de résoudre la limite avec ce que tu connais déjà : `Executors.newFixedThreadPool(2, fabrica)`, où la fabrique produit des threads virtuels. Cela fonctionne, et c'est le piège. La figure 9.5 mesure le maximum de tâches simultanées des deux façons :

```java
// fig09_05.java
final AtomicInteger activas = new AtomicInteger();
final AtomicInteger maximo = new AtomicInteger();

void trabajar() {
  int ahora = activas.incrementAndGet();
  maximo.accumulateAndGet(ahora, Math::max);
  try {
    Thread.sleep(100);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
  activas.decrementAndGet();
}

int maximoCon(ExecutorService ejecutor) {
  maximo.set(0);
  try (ejecutor) {
    for (int i = 0; i < 6; i++) {
      ejecutor.submit(this::trabajar);
    }
  }
  return maximo.get();
}

void main() {
  var fabrica = Thread.ofVirtual().factory();
  int conPool = maximoCon(Executors.newFixedThreadPool(2, fabrica));
  int sinPool = maximoCon(Executors.newVirtualThreadPerTaskExecutor());
  IO.println("pool fijo de 2 hilos virtuales: " + conPool);
  IO.println("un hilo virtual por tarea: " + sinPool);
}
```

```text
$ java fig09_05.java
pool fijo de 2 hilos virtuales: 2
un hilo virtual por tarea: 6
```

Le *pool* fixe limite à 2, l'exécuteur par tâche en laisse passer 6. Il y a deux problèmes à utiliser le premier pour limiter. L'un est de clarté : la limite est restée cachée dans un argument numérique d'une fabrique, et personne qui lira le code ne comprendra que « 2 » signifie « requêtes simultanées autorisées ». L'autre est d'ingénierie : tu viens de réintroduire une file, une taille à ajuster et un thread à réserver, pour obtenir ce que `Semaphore(2)` donne en une ligne et avec son but écrit dans le nom. Le guide officiel est tranchant : les threads virtuels **ne doivent jamais être mis en pool**. Moins de concurrence, plus de code, aucun avantage.

#### 9.4.3 L'étape du projet : `revisarTodos` concurrent

Tu as les pièces. Voici `Revision.java` ; ce qui est nouveau, c'est la constante `LIMITE_POR_OMISION` et la méthode `revisarTodos`, et tout le reste (`revisar`, `nuevoCliente`) est celui de la leçon 8. Lis `revisarTodos` dans l'ordre : d'abord il valide la limite ; crée le sémaphore ; ouvre le client et l'exécuteur (dans cet ordre, comme tu l'as vu en 9.2.2) ; construit une tâche par service, qui prend un permis, vérifie, et le rend dans un `finally` ; envoie tout avec `invokeAll` ; et récupère les résultats **dans l'ordre**, en déballant toute `ExecutionException` :

```java
// fig09_09/src/main/java/mx/habil/revisor/Revision.java

package mx.habil.revisor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;

/** Consulta los servicios por HTTP y arma sus resultados. */
final class Revision {

  /** Cuánto se espera para establecer la conexión, antes de decir que no hay respuesta. */
  static final long TIEMPO_CONEXION_MS = 2000;

  /** Cuántas peticiones puede haber a la vez si nadie dice otra cosa. */
  static final int LIMITE_POR_OMISION = 10;

  private Revision() {}

  /** Un cliente HTTP con tiempo de conexión y sin seguir redirecciones. */
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  /** Consulta un servicio y mide cuánto tarda; ninguna falla de red sale de este método. */
  static Resultado revisar(HttpClient cliente, Servicio servicio) throws InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(servicio.url())
            .timeout(Duration.ofMillis(servicio.timeoutMs()))
            .GET()
            .build();
    long inicio = System.nanoTime();
    Respuesta respuesta;
    try {
      HttpResponse<Void> http = cliente.send(peticion, BodyHandlers.discarding());
      long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
      respuesta = new Respondio(http.statusCode(), tiempoMs);
    } catch (HttpTimeoutException e) {
      respuesta = new SinRespuesta("tiempo agotado");
    } catch (ConnectException e) {
      respuesta = new SinRespuesta("sin conexión");
    } catch (IOException e) {
      respuesta = new SinRespuesta("error de red");
    }
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  /**
   * Revisa todos los servicios a la vez, con a lo más {@code limite} peticiones en vuelo.
   *
   * @param servicios los servicios, en el orden en que debe salir el reporte
   * @param limite cuántas peticiones puede haber al mismo tiempo; al menos 1
   * @return los resultados, en el mismo orden que {@code servicios}
   * @throws InterruptedException si alguien interrumpe al programa mientras espera
   */
  static List<Resultado> revisarTodos(List<Servicio> servicios, int limite)
      throws InterruptedException {
    if (limite < 1) {
      throw new IllegalArgumentException("el límite debe ser al menos 1");
    }
    var semaforo = new Semaphore(limite);
    try (var cliente = nuevoCliente();
        var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
      var tareas = new ArrayList<Callable<Resultado>>();
      for (Servicio servicio : servicios) {
        tareas.add(
            () -> {
              semaforo.acquire();
              try {
                return revisar(cliente, servicio);
              } finally {
                semaforo.release();
              }
            });
      }
      var resultados = new ArrayList<Resultado>();
      for (Future<Resultado> futuro : ejecutor.invokeAll(tareas)) {
        try {
          resultados.add(futuro.get());
        } catch (ExecutionException e) {
          throw new IllegalStateException("la revisión de un servicio falló", e.getCause());
        }
      }
      return resultados;
    }
  }
}
```

Trois décisions de cette fonction qu'il convient de regarder de près. La première : **le sémaphore se prend à l'intérieur de la tâche, pas avant de la lancer**. Toutes les tâches existent dès le départ, et chacune attend son tour d'elle-même ; l'`invokeAll` reste simple. La deuxième : le temps que `revisar` mesure avec `System.nanoTime()` commence **après** avoir pris le permis, de sorte que la colonne TIEMPO continue de dire combien de temps a mis le service et non combien de temps il a attendu dans la file. La troisième : `limite < 1` est rejeté d'entrée avec une `IllegalArgumentException`, parce qu'un sémaphore à zéro permis laisserait toutes les tâches attendre pour toujours.

La constante `LIMITE_POR_OMISION` vaut 10, et `Revisor` l'utilise pour l'instant. `Revisor` change en deux choses : le premier argument, s'il y en a un, est le fichier de configuration (ainsi tu peux vérifier une autre liste sans rien éditer), et il appelle `revisarTodos` avec la limite par défaut :

```java
// fig09_09/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.List;

/** El revisor: lee servicios.json, consulta cada servicio y escribe la tabla. */
public final class Revisor {

  private Revisor() {}

  /**
   * Punto de entrada del programa.
   *
   * @param args el primero, si lo hay, es el archivo de configuración
   * @throws InterruptedException si alguien interrumpe al programa mientras espera
   */
  public static void main(String[] args) throws InterruptedException {
    List<Servicio> servicios;
    try {
      Path ruta = args.length > 0 ? Path.of(args[0]) : Path.of("servicios.json");
      servicios = Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS);
    } catch (ErrorDeConfiguracion e) {
      System.err.println("error de configuración: " + e.getMessage());
      return;
    }
    var resultados = Revision.revisarTodos(servicios, Revision.LIMITE_POR_OMISION);
    System.out.println(Formato.tabla(Reporte.de(resultados)));
  }
}
```

Le fichier `servicios-20.json` a vingt services, `servicio01` à `servicio20`, tous pointés vers le chemin `/espera/500` du laboratoire, qui répond 200 après une demi-seconde. Le voici en entier :

```json fig09_09/servicios-20.json
{
  "servicios": [
    {"nombre": "servicio01", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio02", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio03", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio04", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio05", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio06", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio07", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio08", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio09", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio10", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio11", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio12", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio13", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio14", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio15", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio16", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio17", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio18", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio19", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio20", "url": "http://127.0.0.1:8089/espera/500"}
  ]
}
```

### Mesurer : l'horloge décide

Avec le laboratoire allumé, compile (`mvn -B verify`) et exécute le `revisor` contre ces vingt services, en mesurant combien de temps met le programme entier. Sous Linux, dans un terminal avec `bash`, la façon propre de mesurer est le mot réservé `time` avec un format court :

```text
$ TIMEFORMAT='%R s'
$ time java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor servicios-20.json > /dev/null
1.258 s
```

Change la constante `LIMITE_POR_OMISION` dans `Revision.java` (pour l'instant il n'y a pas d'autre moyen ; dans la Leçon 10 ce sera `--concurrencia`), recompile et répète. Voici mes mesures, deux exécutions par limite, toujours avec les vingt services de 500 ms :

| Limite | Vagues | Ce que prédit l'arithmétique | Ce que j'ai mesuré (les deux exécutions) |
|---|---|---|---|
| 1 (équivaut à **en série**) | 20 | 20 × 0.5 = 10 s | 10.413 s et 10.387 s |
| 3 | 7 | 7 × 0.5 = 3.5 s | 3.808 s et 3.799 s |
| 10 | 2 | 2 × 0.5 = 1 s | 1.258 s et 1.298 s |
| 20 | 1 | 1 × 0.5 = 0.5 s | 0.751 s et 0.779 s |

Deux choses pour lire le tableau sans te tromper. **Une :** ce qui est mesuré dépasse toujours ce qui est prédit d'environ 0.2 seconde, et c'est le coût fixe du démarrage de la JVM et du client HTTP. Je l'ai vérifié : un seul service avec une attente de zéro met 0.2 seconde au total. Retire 0.2 à chaque mesure et l'arithmétique se vérifie à une ou deux décimales près. **Deux :** la dernière colonne répond à la question du plan, « pourquoi une limite de 10 prend le double d'une limite de 20 ». Avec une limite de 10 et vingt services, il y a **deux vagues** : les dix premiers prennent un permis, attendent une demi-seconde ; quand ils les rendent, les dix autres entrent et attendent une autre demi-seconde. Avec une limite de 20, tous entrent d'un coup. Le temps total est le nombre de vagues multiplié par la durée de chaque attente, pas le nombre de services.

Avec les quatre services canoniques, en revanche, l'amélioration est modeste : on passe d'environ 2.8 secondes en série à environ 2.5. Ce n'est pas un défaut : le temps n'est plus la somme (2.5 s) mais **celui du plus lent** (`inventario`, 2.3 s), et comme c'est lui qui commande, il y a peu à gagner. La concurrence aide quand il y a beaucoup d'attentes semblables ; avec une dominante, elle n'aide presque pas. Il est important que tu le saches, parce que « c'est devenu concurrent » ne signifie pas « c'est devenu rapide » : on mesure.

Et avec les quatre services canoniques de `servicios.json`, depuis le dossier du projet et avec le laboratoire allumé, une exécution complète ressemble à ceci :

```text
$ cd fig09_09
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     176ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA     122ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 878ms
```

Les temps de la colonne TIEMPO sont ceux d'une de mes exécutions ; les tiens seront différents, mais ESTADO et DETALLE ne changent jamais.

*(Ici se termine la première séance.)*

### 9.5 État partagé : là où la concurrence mord

#### 9.5.1 Une situation de compétition, provoquée et mesurée

Tout ce qui précède était facile parce que les tâches ne se touchent pas entre elles. Dès que deux threads **écrivent la même donnée**, apparaît le problème qui donne mauvaise réputation à la concurrence. On le voit mieux en le provoquant. Ce programme lance 10,000 tâches, et chacune ajoute 100 fois un à un compteur qu'elles partagent. Si tout se passait bien, le total serait exactement un million. Enregistre-le sous `Carrera.java` :

```java
int contador = 0;

void main() {
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      ejecutor.submit(
          () -> {
            for (int j = 0; j < 100; j++) {
              contador++;
            }
          });
    }
  }
  IO.println("esperado 1000000, contado " + contador);
}
```

Il ne porte pas de marqueur de figure exprès : son résultat change à chaque exécution, donc on ne peut pas le vérifier contre une sortie fixe. J'ai exécuté le programme quatre fois de suite et voici ce qu'il a compté (`esperado` signifie « attendu » et `contado`, « compté ») :

```text
esperado 1000000, contado 841360
esperado 1000000, contado 852985
esperado 1000000, contado 773685
esperado 1000000, contado 855739
```

Il n'y a pas une seule exception, pas un avertissement du compilateur, et le programme « fonctionne » : il se termine, imprime un nombre, sort avec le code 0. Mais il **a perdu entre 144,000 et 226,000 additions**, et il ne perd pas les mêmes deux fois. C'est ce trait qui rend une situation de compétition dangereuse : elle ne s'annonce pas. Un programme avec une situation de compétition passe ses tests 99 % du temps et échoue en production, un mardi, sous charge.

Pourquoi cela arrive est plus simple qu'il n'y paraît. `contador++` ressemble à une opération, mais ce sont trois : **lire** la valeur, **lui ajouter un** et **écrire** le résultat. Si deux threads lisent le 41 en même temps, tous deux calculent 42 et tous deux écrivent 42 : on a ajouté une fois au lieu de deux, et une addition a disparu. Avec dix porteurs travaillant vraiment en parallèle, ce chevauchement se produit des centaines de milliers de fois. (Il y a en outre une seconde cause, celle de la **visibilité** : sans une instruction qui l'ordonne, la mémoire d'un thread peut ne pas voir ce qu'un autre a écrit. Le [modèle mémoire de Java](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html), au chapitre 17 de la spécification du langage, définit quand une écriture est garantie d'être vue : la règle s'appelle *happens-before*.)

Ce n'est pas un problème exclusif aux nombres. Avec une collection partagée, c'est pire. La même expérience avec une `HashMap` —on enregistre 10,000 clés distinctes, chacune depuis sa tâche— devrait se terminer avec 10,000 entrées. Enregistre-le sous `MapaCompartido.java` :

```java
void main() {
  var mapa = new HashMap<Integer, Integer>();
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      int clave = i;
      ejecutor.submit(() -> mapa.put(clave, clave));
    }
  }
  IO.println("esperadas 10000, hay " + mapa.size());
}
```

Mes trois exécutions se sont terminées avec 8,339, 7,195 et 7,563 entrées. Il manquait entre 1,600 et 2,800 entrées. Un `HashMap` n'est pas conçu pour être écrit depuis plusieurs threads, et sa structure interne se corrompt : en plus, il peut lever des exceptions et, dans d'anciennes versions de Java, rester dans une boucle infinie. Que cela « sorte bien » parfois n'est pas une consolation ; c'est ce qui le rend difficile à attraper.

#### 9.5.2 Trois façons de le corriger, par ordre de préférence

**La première, et la meilleure : ne pas partager.** Si chaque tâche travaille avec ses propres données et renvoie un résultat, il n'y a rien à protéger. C'est ce que fait le `revisor`, et je l'explique en 9.5.3.

**La deuxième : des types faits pour être partagés.** Le paquet [`java.util.concurrent`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/package-summary.html) apporte des types dont les opérations sont atomiques, c'est-à-dire indivisibles. `AtomicInteger` et `AtomicLong` sont des entiers auxquels on peut ajouter depuis plusieurs threads sans perdre de comptes ; `LongAdder`, pensé pour des compteurs très disputés, est plus rapide quand il y a beaucoup de threads. Et `ConcurrentHashMap` est la map pour des écritures concurrentes. Sa méthode `merge` est celle qui corrige le cas typique du comptage par catégorie : `mapa.merge("ARRIBA", 1, Integer::sum)` signifie « si la clé n'existe pas, mets-lui 1 ; si elle existe, remplace-la par le résultat de l'addition » ; et selon la documentation, toute l'invocation se fait **de façon atomique**. La figure 9.6 répète l'expérience de comptage des états avec 10,000 tâches, maintenant avec `merge`, et additionne aussi avec un compteur atomique :

```java
// fig09_06.java
void main() {
  var porEstado = new ConcurrentHashMap<String, Integer>();
  var total = new AtomicInteger();
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      ejecutor.submit(
          () -> {
            porEstado.merge("ARRIBA", 1, Integer::sum);
            total.incrementAndGet();
          });
    }
  }
  IO.println(porEstado);
  IO.println("total atómico: " + total.get());
}
```

```text
$ java fig09_06.java
{ARRIBA=10000}
total atómico: 10000
```

Contrairement à la situation de compétition, ce résultat sort **toujours** pareil, autant de fois que tu l'exécutes : 10,000 dans la map et 10,000 dans le compteur. Cela, on peut le vérifier, et c'est pourquoi cette figure porte un marqueur.

**La troisième : les verrous.** Java a `synchronized` et les verrous de `java.util.concurrent.locks` pour protéger un morceau de code de façon qu'un seul thread à la fois l'exécute. Ils fonctionnent, et parfois c'est la seule chose qui marche ; mais ce sont les outils les plus faciles à mal utiliser (interblocages, verrous oubliés, sections trop longues), et le cours ne s'en sert pas. Avec les deux options précédentes, ils ne sont presque jamais nécessaires.

#### 9.5.3 Pourquoi le `revisor` n'a pas de situations de compétition

Passe en revue chaque donnée que touchent les tâches de `revisarTodos`, une par une, parce que c'est ici que se récolte ce que tu as appris dans la leçon 3. Le `Servicio` que reçoit chaque tâche est un *record* : **immuable**, personne ne le change. Le `HttpClient` est sûr à partager (la documentation le garantit). La tâche n'écrit dans aucune variable extérieure : elle **renvoie** son `Resultado`, qui est un autre *record* immuable. Le seul objet mutable que les tâches partagent est le `Semaphore`, qui est fait précisément pour cela. Et la liste `resultados`, qui est bien une `ArrayList` normale (non sûre pour plusieurs threads), n'est touchée que par le thread qui a appelé `revisarTodos`, **après** que `invokeAll` a terminé ; le paquet `java.util.concurrent` lui-même documente que ce qu'une tâche a fait avant de se terminer est garanti d'être vu par celui qui récupère son résultat avec `get()`.

La règle de conception qui en sort vaut plus que n'importe quel outil : **communique en renvoyant des valeurs, pas en partageant de la mémoire**. Quand une tâche a besoin de dire quelque chose à celui qui l'a lancée, qu'elle le renvoie. L'immuabilité que le cours soigne depuis la leçon 3 n'était pas un ornement de style : c'est ce qui permet qu'une pièce s'exécute dans cent threads sans qu'on ait à penser à eux.

### 9.6 Valeurs à portée limitée : `ScopedValue`

Il y a un problème que les tâches concurrentes aggravent : le **contexte**. Imagine que chaque vérification porte un identifiant d'exécution, `r-001`, et que tu veux que n'importe quelle méthode —celle qui classe, celle qui construit la requête, celle qui écrit un message de diagnostic— puisse le lire. La solution évidente est de le passer en paramètre dans chaque méthode de la chaîne, et cela devient un bruit que personne ne veut. La solution de toujours en Java était `ThreadLocal`, une variable qui a une valeur différente dans chaque thread. Mais elle a trois défauts connus : elle est **mutable** (n'importe qui la change, et personne ne sait qui), elle vit **jusqu'à ce que quelqu'un la nettoie** (l'oublier est une fuite de mémoire), et la copier vers des threads enfants est **coûteux**, ce qui pèse quand il y a des millions de threads virtuels.

Pour cela existe `ScopedValue`, définitif depuis Java 25 ([JEP 506](https://openjdk.org/jeps/506)). Une valeur à portée limitée est **immuable** et existe **seulement pendant la durée d'un bloc de code** : tu la lies avec `ScopedValue.where(CLAVE, valor).run(...)` (ou `.call(...)` si le bloc renvoie quelque chose ou lève des exceptions), et n'importe quelle méthode appelée de l'intérieur peut la lire avec `CLAVE.get()`. En sortant du bloc, la valeur disparaît toute seule. La figure 9.7 le montre, et montre aussi sa limite :

```java
// fig09_07.java
static final ScopedValue<String> CORRIDA = ScopedValue.newInstance();

void anotar(String mensaje) {
  IO.println("[corrida " + CORRIDA.get() + "] " + mensaje);
}

void main() throws Exception {
  ScopedValue.where(CORRIDA, "r-001")
      .call(
          () -> {
            anotar("revisando 4 servicios");
            Thread otro =
                Thread.ofVirtual()
                    .start(() -> IO.println("en un hilo nuevo, ligado = " + CORRIDA.isBound()));
            otro.join();
            return null;
          });
  IO.println("fuera del bloque, ligado = " + CORRIDA.isBound());
}
```

```text
$ java fig09_07.java
[corrida r-001] revisando 4 servicios
en un hilo nuevo, ligado = false
fuera del bloque, ligado = false
```

La méthode `anotar` ne reçoit pas l'identifiant en paramètre et le lit quand même : c'est le service qu'elle rend. La deuxième ligne est la partie qui surprend : le thread virtuel créé **à l'intérieur** du bloc, à la main, **ne voit pas la valeur** (`ligado = false`). La valeur voyage vers les appels d'un même thread, pas vers les threads que tu lances de ton côté ; seule la concurrence structurée de 9.7 la transmet à ses enfants. Et la troisième ligne confirme qu'en dehors du bloc il ne reste rien.

Le `revisor` de ce cours **n'utilise pas** `ScopedValue` : il n'a aucun contexte à passer. Tu le verras dès que tu écriras un serveur qui traite des requêtes de plusieurs utilisateurs et que tu auras besoin de savoir, en n'importe quel point du code, pour qui tu travailles. Il est ici parce que c'est ce qui remplace `ThreadLocal` dans le Java que tu vas écrire désormais, et parce qu'il a fait ses débuts dans cette version.

### 9.7 Ce qui vient : la concurrence structurée

En 9.3, tu as vu que `invokeAll` attend toutes les tâches, quoi qu'il arrive. Il manque une forme que beaucoup de gens veulent : « lance ces tâches **comme une unité** : si une échoue, annule les autres ; si le bloc se termine, aucune ne reste vivante ». Cela s'appelle la **concurrence structurée**, et le JDK 25 l'apporte... **en préversion**. Une préversion (*preview*) est une fonctionnalité complète mais encore sujette à changement, qui ne s'active qu'avec une option. Celle-ci a changé de forme à chaque version depuis la 21 ; la cinquième préversion est [JEP 505](https://openjdk.org/jeps/505).

La figure 9.8 utilise l'API (`StructuredTaskScope.open()`, avec `fork` pour lancer et `join` pour attendre). Compilée telle quelle, **elle ne tourne pas** :

```java
// fig09_08.java
String consultar(String nombre) {
  return "respuesta de " + nombre;
}

void main() throws Exception {
  try (var alcance = StructuredTaskScope.open()) {
    var catalogo = alcance.fork(() -> consultar("catalogo"));
    var pagos = alcance.fork(() -> consultar("pagos"));
    alcance.join();
    IO.println(catalogo.get() + " / " + pagos.get());
  }
}
```

```text
$ java fig09_08.java
fig09_08.java:7: error: StructuredTaskScope is a preview API and is disabled by default.
  try (var alcance = StructuredTaskScope.open()) {
                     ^
  (use --enable-preview to enable preview APIs)
1 error
error: compilation failed
```

Lire le message te dit tout : la classe existe, mais c'est une API de préversion et elle est désactivée. Avec l'option que demande le message lui-même, elle fonctionne :

```text
$ java --enable-preview --source 25 fig09_08.java
respuesta de catalogo / respuesta de pagos
```

**Ne l'utilise ni dans le `revisor` ni en production** : l'API peut changer d'une version du JDK à l'autre, et un programme qui en dépendrait devrait être réécrit quand elle changera. C'est pourquoi ce cours la laisse comme une curiosité. Ce que tu emportes, c'est l'idée : quand elle se stabilisera, ce sera la façon naturelle de dire « ces trois requêtes sont un seul travail », et `invokeAll` avec un `Semaphore` aura été ta façon d'y arriver. En attendant, ce que tu viens d'apprendre fonctionne en Java 21, 25 et les suivants.

### 9.8 Les tests du `revisor` concurrent

Le `revisor` concurrent apporte trois nouveaux tests, dans la classe `RevisionConcurrenteTest`. Chacun teste l'une des promesses de 9.3 et 9.4, et aucun ne dépend de l'horloge plus que nécessaire. Voici ce qu'ils vérifient : **l'ordre** (le service le plus lent, placé en premier dans la liste, reste le premier du résultat) ; **la limite** (avec une limite de 2 et huit requêtes, le serveur du test ne voit jamais plus de deux à la fois, c'est un `AtomicInteger` à l'intérieur du serveur lui-même qui le compte) ; et **la validation** (une limite de 0 est une `IllegalArgumentException`). Remarque que le serveur du test est créé avec `Executors.newVirtualThreadPerTaskExecutor()` : le serveur de `com.sun.net.httpserver`, si on ne lui donne pas d'exécuteur, traite les requêtes **une par une** dans un seul thread, et le test de la limite mesurerait 1 au lieu de 2 sans que le sémaphore y soit pour quelque chose.

```java
// fig09_09/src/test/java/mx/habil/revisor/RevisionConcurrenteTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RevisionConcurrenteTest {

  private static HttpServer servidor;
  private static int puerto;
  private static final AtomicInteger ACTIVAS = new AtomicInteger();
  private static final AtomicInteger MAXIMO = new AtomicInteger();

  @BeforeAll
  static void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/lento", i -> responder(i, 400));
    servidor.createContext("/rapido", i -> responder(i, 0));
    servidor.createContext("/contado", RevisionConcurrenteTest::responderContando);
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    puerto = servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, long esperaMs) throws IOException {
    try {
      Thread.sleep(esperaMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    intercambio.sendResponseHeaders(200, -1);
    intercambio.close();
  }

  private static void responderContando(HttpExchange intercambio) throws IOException {
    int ahora = ACTIVAS.incrementAndGet();
    MAXIMO.accumulateAndGet(ahora, Math::max);
    try {
      responder(intercambio, 200);
    } finally {
      ACTIVAS.decrementAndGet();
    }
  }

  private static Servicio servicio(String nombre, String ruta) {
    return new Servicio(nombre, URI.create("http://127.0.0.1:" + puerto + ruta));
  }

  @Test
  void elMasLentoPrimeroEnLaListaSigueSiendoElPrimeroEnElResultado() throws Exception {
    var servicios = List.of(servicio("lento", "/lento"), servicio("rapido", "/rapido"));
    List<Resultado> resultados = Revision.revisarTodos(servicios, 10);
    assertEquals("lento", resultados.get(0).servicio().nombre());
    assertEquals("rapido", resultados.get(1).servicio().nombre());
    assertEquals(Estado.ARRIBA, resultados.get(1).estado());
  }

  @Test
  void conLimiteDosElServidorNuncaVeMasDeDosPeticionesALaVez() throws Exception {
    var servicios = new ArrayList<Servicio>();
    for (int i = 1; i <= 8; i++) {
      servicios.add(servicio("s" + i, "/contado"));
    }
    MAXIMO.set(0);
    Revision.revisarTodos(servicios, 2);
    assertEquals(2, MAXIMO.get());
  }

  @Test
  void unLimiteMenorQueUnoSeRechaza() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Revision.revisarTodos(List.of(servicio("s", "/rapido")), 0));
  }
}
```

Un test que tu n'as jamais vu échouer ne démontre rien, alors fais l'expérience. Dans `Revision.java`, remplace `new Semaphore(limite)` par `new Semaphore(Integer.MAX_VALUE)` (un sémaphore avec tant de permis qu'il ne freine jamais) et exécute les tests. Voici ce que dit le rapport de *surefire* :

```text
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.737 s <<< FAILURE! -- in mx.habil.revisor.RevisionConcurrenteTest
[ERROR] mx.habil.revisor.RevisionConcurrenteTest.conLimiteDosElServidorNuncaVeMasDeDosPeticionesALaVez -- Time elapsed: 0.220 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <2> but was: <8>
```

C'est justement le test de la limite qui est passé au rouge, et avec le nombre qu'il devait : sans frein, les **huit** requêtes sont arrivées en même temps. Remets le sémaphore à sa valeur, et le rapport revient au vert. Pour compter les tests sans te fier au code de sortie de Maven, additionne-les à partir du rapport, de deux façons différentes (un décompte qui décide quelque chose se fait deux fois) :

```text
$ grep -h -o ' tests="[0-9]*"' target/surefire-reports/TEST-*.xml | awk -F'"' '{s+=$2} END {print s}'
28
$ cat target/surefire-reports/TEST-*.xml | grep -c '<testcase '
28
```

Vingt-cinq de la leçon 8 plus les trois d'aujourd'hui : 28, et aucun sauté.

Le `pom.xml` ne change qu'en une chose : la version passe à `0.9.0`. En dehors des fichiers que tu as vus en entier dans cette leçon (`Revision.java`, `Revisor.java`, `RevisionConcurrenteTest.java` et `servicios-20.json`), tout le projet reste exactement comme il était dans la leçon 8, sans toucher une seule ligne.

## L'erreur que tu vas voir

L'erreur de cette leçon n'est pas un message, c'est une **absence de message** : la situation de compétition de 9.5.1. Le programme se termine sans exceptions et avec un nombre faux (`esperado 1000000, contado 773685`, « attendu 1000000, compté 773685 »). Si à un moment un programme à toi donne des résultats différents lors d'exécutions identiques, soupçonne une situation de compétition avant toute autre chose, et cherche quelle donnée deux threads écrivent en même temps. Quatre messages secondaires, pour les reconnaître d'un coup d'œil :

- `java.util.concurrent.ExecutionException: java.lang.IllegalStateException: fallé a propósito` : une tâche a échoué dans un autre thread. La cause est après les deux-points ; regarde `getCause()` (`fig09_03.java`).
- `AssertionFailedError: expected: <2> but was: <8>` : le test de la limite te dit que le sémaphore n'a pas freiné (9.8).
- Un `HashMap` partagé qui se termine avec moins d'entrées que tu n'en as écrit (`esperadas 10000, hay 7195`, « attendues 10000, il y en a 7195 ») : la map s'est corrompue ; utilise `ConcurrentHashMap`.
- `error: StructuredTaskScope is a preview API and is disabled by default.` : tu as utilisé une fonctionnalité en préversion sans l'option (`fig09_08.java`).

## Ce qui se fait de travers

**Mettre les threads virtuels dans un *pool*.** `newFixedThreadPool(10, Thread.ofVirtual().factory())` limite, mais de façon cachée, avec une file et une taille que quelqu'un devra ajuster. Un thread virtuel par tâche, et la limite avec un `Semaphore` (9.4.2).

**Rendre le permis hors d'un `finally`.** Une exception entre `acquire()` et `release()` perd le permis. Après quelques échecs, toutes les tâches attendent pour toujours, et le programme se bloque sans explication (9.4.1).

**Avaler l'`InterruptedException`.** Un `catch (InterruptedException e) {}` vide cache le signal que quelqu'un veut que la tâche s'arrête. Si tu ne peux pas la propager, rétablis l'indicateur avec `Thread.currentThread().interrupt()` (leçon 5).

**Partager une collection qui n'est pas concurrente.** `ArrayList`, `HashMap` et `HashSet` ne sont pas faites pour être écrites depuis plusieurs threads. Utilise celles de `java.util.concurrent`, ou mieux, ne partage pas (9.5).

**Utiliser des threads virtuels pour du travail de processeur.** Ils ne donnent pas de vitesse, ils donnent de l'échelle. Une tâche de calcul long occupe son porteur tout le temps et ne cède pas la main ; pour cela, ce qui compte, c'est le nombre de cœurs (9.1.2).

**Lancer sans limite contre un service d'autrui.** Que les threads soient bon marché ne veut pas dire que la destination tienne le coup. La limite se fixe en pensant au service qui reçoit les requêtes, pas à la JVM (9.4.1).

**Garder le contexte dans un `ThreadLocal` mutable et oublier de le nettoyer.** Pour ce qui ne dure que le temps d'un bloc, `ScopedValue` (9.6).

**Tester la concurrence avec `Thread.sleep` pour « laisser le temps ».** Un test qui attend un temps fixe et suppose que ce qu'il attendait s'est produit casse sur une machine lente. Attends ce qu'il faut vraiment attendre (`join`, `invokeAll`, `close`), comme le font les tests de 9.8.

## Exercices

### Exercice 1 — Mesure les trois temps

Avec le laboratoire allumé et `servicios-20.json`, mesure le `revisor` avec `LIMITE_POR_OMISION` à 1, à 10 et à 20. Avant d'exécuter, écris ta prédiction pour chacune et explique avec tes mots pourquoi la limite de 10 prend le double de celle de 20. Note les trois temps réels dans ton journal de bord, sur la ligne de la leçon 9.

### Exercice 2 — Prédis le 3

Sans l'exécuter encore : combien de temps doit mettre le `revisor` avec une limite de 3 sur ces mêmes vingt services ? Écris le calcul (combien de vagues, combien dure chacune). Ensuite mesure-le, retire ce que coûte le démarrage de la JVM, et compare. Si ta prédiction a raté de plus de quelques dixièmes, cherche ce que tu as laissé de côté.

### Exercice 3 — Corrige les deux situations de compétition

Prends `Carrera.java` et `MapaCompartido.java` de 9.5.1 et corrige-les, chacun avec un outil différent de 9.5.2. Tu dois obtenir que le compteur donne exactement 1,000,000 et que la map ait exactement 10,000 entrées, **à chaque exécution**. Exécute ta version trois fois pour voir qu'elle ne change pas.

### Exercice 4 — Un permis qui se perd, et le test qui l'attrape

Les trois tests de 9.8 ne détectent pas un `release()` mal placé. Écris-en un quatrième qui le fait : deux services, limite 1, et le premier avec une adresse qui fasse échouer `revisar` avec une exception qui **n'est pas** réseau (par exemple `ftp://127.0.0.1/x`, que `HttpRequest` rejette lors de la construction de la requête). Avant de l'écrire, prédis ce qui se passerait si `semaforo.release()` était hors du `finally`. Ensuite sors-le du `finally`, regarde le test au rouge, et rétablis-le.

## Solutions

### Solution 1

Mes temps ont été 10.4 s, 1.27 s et 0.76 s (voir le tableau de 9.4.3). Les prédictions correctes étaient 10, 1 et 0.5 seconde plus environ 0.2 de démarrage. Avec une limite de 10 il y a deux vagues d'une demi-seconde, avec une limite de 20 une seule ; le temps est *vagues × attente*, pas *services × attente*. Si ton chiffre pour la limite 20 est bien supérieur à 0.7 s, regarde si le laboratoire n'était pas occupé à autre chose.

### Solution 2

Vingt services sur trois permis font sept vagues (six pleines et une de deux) : 7 × 0.5 = 3.5 s, plus environ 0.2 s de démarrage, soit 3.7 s. J'ai mesuré 3.808 s et 3.799 s. Le dixième en trop correspond à des coûts d'exploitation (ouvrir des connexions, répartir des threads) que je n'ai pas mesurés séparément ; l'important est que le calcul des vagues soit juste. Remarque la dernière vague : même si elle n'a que deux services, elle attend la demi-seconde complète, parce qu'une vague dure ce que dure son service le plus lent, pas ce que dure chacun de ses membres.

### Solution 3

Pour le compteur, un `LongAdder` (`AtomicLong` convient aussi) ; pour la map, une `ConcurrentHashMap`. Les deux changements tiennent dans un seul programme :

```java
void main() {
  var contador = new LongAdder();
  var mapa = new ConcurrentHashMap<Integer, Integer>();
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      int clave = i;
      ejecutor.submit(
          () -> {
            for (int j = 0; j < 100; j++) {
              contador.increment();
            }
            mapa.put(clave, clave);
          });
    }
  }
  IO.println("esperado 1000000, contado " + contador.sum());
  IO.println("esperadas 10000 entradas, hay " + mapa.size());
}
```

Les trois exécutions ont donné :

```text
esperado 1000000, contado 1000000
esperadas 10000 entradas, hay 10000
```

La leçon est que la correction n'est pas « faire plus attention » : c'est changer le type de la donnée partagée pour un type fait pour être partagé.

### Solution 4

La prédiction : avec une limite de 1, le premier service entre, `revisar` lève l'exception, et comme le `release()` est resté hors du `finally`, il **ne s'exécute jamais** : l'unique permis est perdu. Le deuxième service reste à attendre dans `acquire()` pour toujours, `invokeAll` ne revient jamais et le programme se bloque. Le test, avec un plafond de temps de JUnit pour que le blocage se transforme en un échec avec un message :

```java
  @Test
  @Timeout(5)
  void unErrorDelProgramaNoDejaPermisosPerdidos() {
    var servicios =
        List.of(
            new Servicio("malo", URI.create("ftp://127.0.0.1/x")), servicio("rapido", "/rapido"));
    assertThrows(IllegalStateException.class, () -> Revision.revisarTodos(servicios, 1));
  }
```

(Il faut `import org.junit.jupiter.api.Timeout;`.) Avec le `finally` en place, le test passe : le premier service échoue et rend son permis, le deuxième est vérifié, et `revisarTodos` lève l'`IllegalStateException` que tu connais déjà. Avec le `release()` hors du `finally`, le rapport dit :

```text
[ERROR] mx.habil.revisor.RevisionConcurrenteTest.unErrorDelProgramaNoDejaPermisosPerdidos -- Time elapsed: 5.013 s <<< ERROR!
java.util.concurrent.TimeoutException: unErrorDelProgramaNoDejaPermisosPerdidos() timed out after 5 seconds
```

Sans le `@Timeout`, ce même test **n'échouerait pas : il se bloquerait**, et un blocage en intégration continue coûte bien plus cher qu'un échec. Quand tu testes la concurrence, mets un plafond de temps à tout ce qui pourrait attendre pour toujours. Rétablis le `finally` et le test revient au vert (avec les quatre tests de cette classe, le total serait 29 ; la figure de la leçon en reste aux trois d'origine).

## Comment savoir que j'ai réussi

- [ ] `java fig09_01.java` imprime `virtual=true, demonio=true` pour le thread virtuel, et tu sais expliquer pourquoi un thread virtuel que l'on n'attend pas est coupé quand `main` se termine.
- [ ] `java fig09_02.java` imprime `true` sur ses deux lignes, et tu sais calculer à la main pourquoi un *pool* de 50 met quatre secondes avec mille tâches de 200 ms.
- [ ] Dans `fig09_03.java`, les trois résultats sortent dans l'ordre de la liste et non dans celui de la fin, et tu sais dire ce qu'est la cause d'une `ExecutionException`.
- [ ] `java fig09_04.java` rapporte que le maximum simultané est 2, et tu sais pourquoi le `release()` va dans un `finally`.
- [ ] Tu as exécuté `Carrera.java` plusieurs fois et tu as vu que le nombre change et n'atteint jamais 1,000,000.
- [ ] `java fig09_06.java` imprime `{ARRIBA=10000}` à chaque exécution.
- [ ] `mvn -B verify` se termine au vert, et la somme des tests du rapport, comptée de deux façons, est 28.
- [ ] Avec le laboratoire allumé, `servicios-20.json` met environ 10 s avec une limite de 1, près de 1 s avec une limite de 10 et près de 0.5 s avec une limite de 20 (plus ce que coûte le démarrage de la JVM), et tu les as notés dans le journal de bord.
- [ ] Avec le sémaphore sans frein, le test de la limite passe au rouge avec `expected: <2> but was: <8>`.

## Résumé

Réponds sans regarder la leçon :

1. Qu'est-ce qu'un thread porteur, et que fait la JVM quand un thread virtuel arrive à une opération qui bloque ?
2. Quelle différence y a-t-il entre donner de l'« échelle » et donner de la « vitesse » ? Pour quelle sorte de tâche les threads virtuels servent-ils et pour laquelle non ?
3. Pourquoi un thread virtuel par tâche et non un *pool* ? Qu'utilise-t-on pour limiter combien de tâches avancent en même temps ?
4. Dans quel ordre la méthode `invokeAll` renvoie-t-elle les `Future`, et pourquoi cela importe-t-il au rapport ?
5. Que contient une `ExecutionException` et pourquoi faut-il regarder sa cause ? Que décide de faire le `revisor` avec elle et pourquoi ?
6. Pourquoi le `release()` d'un sémaphore va-t-il dans un `finally` ? Que se passe-t-il sinon ?
7. Pourquoi `contador++` perd-il des additions quand plusieurs threads l'exécutent ? Quelles trois façons de le corriger connais-tu et laquelle est la préférée ?
8. Pourquoi le `revisor` n'a-t-il pas de situations de compétition alors qu'il exécute dix tâches en même temps ?
9. Qu'a `ScopedValue` que n'a pas `ThreadLocal`, et pourquoi un thread créé à la main dans le bloc ne voit-il pas la valeur ?
10. Pourquoi la concurrence structurée reste-t-elle « ce qui vient » dans ce cours ?

**Pour comparer avec d'autres langages.** Si tu viens de Go, la tâche avec un thread virtuel par service est l'équivalent de lancer une *goroutine* par service et d'attendre avec un [`sync.WaitGroup`](https://pkg.go.dev/sync#WaitGroup) ; la différence est qu'en Java les résultats reviennent dans des `Future` et dans l'ordre, et non par un canal. Si tu viens de TypeScript, [`Promise.all`](https://developer.mozilla.org/fr/docs/Web/JavaScript/Reference/Global_Objects/Promise/all) est la comparaison naturelle, avec une différence qui compte : `Promise.all` rejette dès qu'une promesse échoue, alors que `invokeAll` attend toujours toutes les tâches. Et en Python, [`asyncio.TaskGroup`](https://docs.python.org/3/library/asyncio-task.html#task-groups) annule les autres tâches si l'une échoue, ce qui est justement ce que promet la concurrence structurée de 9.7. Les cours de [Go](https://www.habil.mx/fr/cours/go/), de [TypeScript](https://www.habil.mx/fr/cours/typescript/) et de Python résolvent ce même `revisor` avec leurs outils. Et si tu te demandes quand il convient d'interroger en ligne, comme le fait le `revisor`, et quand il convient d'utiliser une file ou un événement, l'article [styles d'intégration : en ligne, file ou événement](https://www.habil.mx/fr/blog/styles-integration-en-ligne-file-attente-evenement/) en discute les coûts.

## Pour aller plus loin

- [Oracle, « Virtual Threads » (guide du JDK 25)](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html) : ce qu'ils sont, à quoi ils servent, pourquoi on ne les met pas en pool et pourquoi un sémaphore limite mieux qu'un *pool*. En anglais. Consulté le 7 octobre 2026.
- [JEP 444, « Virtual Threads »](https://openjdk.org/jeps/444) et [JEP 491, « Synchronize Virtual Threads without Pinning »](https://openjdk.org/jeps/491) : la spécification des threads virtuels et la correction du *pinning* dans Java 24. Consulté le 7 octobre 2026.
- [`ExecutorService`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html) et [`ConcurrentHashMap`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) dans l'API de Java 25 : le contrat exact de `invokeAll`, `close` et `merge`. Consulté le 7 octobre 2026.
- [JEP 506, « Scoped Values »](https://openjdk.org/jeps/506) et [JEP 505, « Structured Concurrency (Fifth Preview) »](https://openjdk.org/jeps/505) : les valeurs à portée limitée (définitives en 25) et la concurrence structurée (encore en préversion). Consulté le 7 octobre 2026.

---

**Précédente :** [Leçon 8 — HTTP et JSON](08-http-y-json.md) ·
**Suivante :** [Leçon 10 — Le programme terminé](10-el-programa-terminado.md)
