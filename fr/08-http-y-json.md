# Leçon 8 — HTTP et JSON

**Durée :** 2 × 90 min (deux séances ; la coupure se situe entre les sections 8.3 et 8.4)

**Ce que tu construis :** le `revisor` cesse d'inventer ses réponses : il interroge chaque service par HTTP pour de vrai et lit sa liste dans un fichier `servicios.json` qu'il valide avant de lui faire confiance

**Ce que tu apprends :** le `HttpClient` du JDK (un client, délai de connexion et délai maximal par requête, redirections, codes d'état) ; comment convertir chaque panne réseau en un détail court sans qu'aucune exception ne s'échappe ; JSON et Jackson 3 (`tools.jackson`), et ce qui change par rapport à presque tout ce qu'il y a sur internet ; lire un JSON comme un arbre et le valider à la main ; écrire le rapport avec des *records* ; et **SSRF : l'URL est écrite par quelqu'un d'autre**

**D'où tu viens.** Tu arrives avec le projet Maven de la [Leçon 7](07-pruebas-junit.md) : des paquets, un `pom.xml`, 18 tests au vert et un `Simulador` qui joue le rôle du réseau avec quatre réponses inventées. Cette invention, c'est ce qui prend fin aujourd'hui. Un revisor qui ne parle jamais au réseau ne vérifie rien ; et une liste de services en texte brut, qui ne distingue pas un nombre d'un texte, ne résiste pas au premier fichier écrit par quelqu'un d'autre que toi. Cette leçon règle les deux choses, et au passage t'enseigne la partie du travail la plus sous-estimée : **que faire de ce qui vient de l'extérieur**. Tu travailles dans ton dossier `~/curso-java/revisor/`.

**Ce que cette leçon ne fait pas.** Elle n'interroge pas les services en même temps : `revisarTodos` les vérifie l'un après l'autre (en série), et c'est pourquoi le rapport prend environ 2.5 secondes, la somme de ce que prend chaque service ; les vérifier tous en même temps, c'est la [Leçon 9](09-concurrencia.md). Elle ne lit pas non plus le corps des réponses (le `revisor` ne s'intéresse qu'au code et à la durée), et elle n'a pas encore de ligne de commande : le rapport en JSON existe et se teste aujourd'hui, mais il ne se demandera avec une option qu'à partir de la [Leçon 10](10-el-programa-terminado.md).

**À propos des exécutions.** Les services interrogés sont ceux du **laboratoire** (8.1), un serveur local d'entraînement qui répond toujours la même chose ; c'est pourquoi l'ESTADO et le DETALLE de chaque ligne sont identiques à chaque exécution. Ce qui change d'une exécution à l'autre, c'est la colonne TIEMPO, parce que c'est un temps réellement mesuré : si ton tableau dit `171ms` là où celui-ci dit `180ms`, c'est normal. Tout a été exécuté avec Temurin 25, Maven 3.10.0, JUnit 6.1.3 et Jackson 3.2.3. Les exemples qui utilisent Jackson s'exécutent depuis la racine du projet, avec `java -cp "target/lib/*" fig08_06.java` (8.5 explique pourquoi), et les programmes se trouvent dans [`programas/08-http-y-json/`](../programas/08-http-y-json), dans le dépôt du cours.

## À la fin, tu seras capable de

- Faire une requête avec `HttpClient`, lire le code d'état et mesurer le temps que la réponse a mis.
- Expliquer la différence entre le délai de connexion et le délai maximal d'une requête, et pourquoi aucune requête ne s'envoie sans délai maximal.
- Convertir chaque panne réseau (pas de connexion, délai dépassé, connexion fermée) en un détail court, sans qu'une exception réseau s'échappe de `revisar`.
- Déclarer la dépendance de Jackson 3, distinguer `tools.jackson` de `com.fasterxml.jackson`, et expliquer ce qu'est le *classpath* et à quoi sert `target/lib`.
- Lire un JSON comme un arbre (`JsonNode`) et le valider à la main, et expliquer pourquoi le mapper directement vers un *record* accepte `1.5` et `"1000"` comme délai maximal.
- Écrire le rapport en JSON avec des *records* et des clés en `snake_case`.
- Expliquer ce qu'est SSRF et appliquer les défenses du `revisor` : schéma autorisé, liste autorisée d'hôtes et redirections non suivies.

## Le pourquoi avant le comment

Jusqu'ici, le `revisor` a été un acteur. Quand il disait que `inventario` mettait 2.3 secondes, c'était parce que tu l'avais écrit dans le `Simulador` ; il n'y avait personne de l'autre côté. Changer cela, c'est plus que changer l'origine d'un nombre, parce que le monde réel a quatre habitudes que le simulateur n'a jamais eues. **Il met le temps qu'il veut :** un service peut répondre en 80 millisecondes ou en dix secondes. **Il ne répond pas toujours :** le port peut être fermé, la connexion peut se couper à mi-chemin, le serveur peut l'accepter et rester muet. **Il répond des choses étranges :** souvent, un service en panne ne cesse pas de répondre, il répond avec un 503. Et **il renvoie parfois ailleurs :** une réponse peut dire « ce que tu cherches n'est plus ici, va à cette autre adresse ». Le `revisor` doit classer tout cela, sans planter, et sans attendre indéfiniment.

La seconde moitié de la leçon est un autre genre de problème. Jusqu'ici, la liste de services était un `servicios.txt` : nom, adresse, un délai maximal facultatif, séparés par des espaces. Cela suffit tant que tout est du texte et que tout est facile. Mais un fichier texte ne distingue pas le nombre `1000` du texte `"1000"`, ne peut rien imbriquer, et n'a pas de façon standard de dire « ce champ n'est pas là ». Pour cela existe **JSON**, le format d'échange de données le plus utilisé d'internet, et Java ne l'inclut pas dans sa bibliothèque standard : on utilise la bibliothèque de quelqu'un d'autre, et c'est pourquoi la Leçon 6 a existé.

Et entre les deux moitiés, il y a une idée qui les relie et qui est la plus importante de la leçon : **ton programme fait maintenant, avec ce que dit un fichier, quelque chose dans le monde.** Si le fichier dit « interroge cette adresse », le `revisor` l'interroge. Qui peut écrire dans ce fichier, ou dans le formulaire qui l'alimentera un jour, contrôle vers où ton programme se connecte depuis ton réseau. Cette classe de faille a un nom propre et figure parmi les dix plus sérieuses des applications web : elle s'appelle **SSRF**, et la dernière section l'attaque avec quatre défenses.

Le chemin comporte deux séances. La **première** couvre 8.1 à 8.3 : le laboratoire, le `HttpClient`, les délais maximaux et les pannes, et se termine avec un `revisor` qui interroge pour de vrai. La **seconde** couvre 8.4 à 8.9 : JSON, Jackson 3, valider, écrire et protéger. Avec cette destination en tête, voici ce qu'imprime le `revisor` à la fin de la leçon, avec le laboratoire allumé :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

L'ESTADO et le DETALLE sont ceux de toujours. La colonne TIEMPO n'est plus faite de nombres écrits par toi : c'est ce qu'a mesuré ton ordinateur.

## Les concepts

### 8.1 Le laboratoire et la première requête

#### 8.1.1 HTTP en cinq lignes

**HTTP** est la langue que parlent presque tous les services d'internet. Une conversation est toujours la même : un **client** (ton programme) envoie une **requête** et un **serveur** envoie une **réponse**. La requête porte une **méthode** qui dit ce que l'on veut (`GET` est « donne-moi ceci »), un **chemin** qui dit quoi (`/pagos`) et des en-têtes avec des données supplémentaires. La réponse porte un **code d'état**, qui est un nombre à trois chiffres avec le résultat ; des en-têtes ; et, presque toujours, un **corps** (la page, les données, le texte « ok »).

Du code d'état, le `revisor` n'a besoin que du premier chiffre, qui est une famille, d'après la [spécification de HTTP](https://www.rfc-editor.org/rfc/rfc9110) : `2xx` est un succès (`200` est « voici »), `3xx` est une redirection (« cherche ailleurs »), `4xx` est une erreur de celui qui demande (`404` est « cela n'existe pas ») et `5xx` est une erreur du serveur (`503` est « je ne peux pas te servir en ce moment »). La règle de classification du `revisor` que tu as déjà, « un code de 400 ou plus laisse le service en panne », utilise exactement cette division : répondre `503`, c'est être en panne même si l'on répond vite.

Voici ce qui voyage vraiment. Avec `curl`, le programme du terminal qui fait une requête et affiche la réponse, l'option `-i` laisse voir le code et les en-têtes avant le corps (la date change à chaque exécution) :

```text
$ curl -si http://127.0.0.1:8089/pagos
HTTP/1.1 200 OK
Date: Thu, 08 Oct 2026 03:42:51 GMT
Content-length: 3

ok
```

Première ligne : `HTTP/1.1 200 OK`, la version du protocole, le code et son nom. Ensuite les en-têtes, un par ligne, et une ligne vide ; puis le corps, qui est ici le texte `ok`. Il n'y a rien de plus. Les deux choses qui importent au `revisor` sont le `200` et le temps qu'il a mis à arriver.

#### 8.1.2 Le laboratoire

Pour s'entraîner avec un réseau, il y a deux chemins, et les deux ont un problème. Interroger des services publics d'internet rend chaque exécution différente (aujourd'hui ils répondent, demain non, et leurs temps dépendent de ta connexion), et ce n'est pas très poli de s'exercer contre les serveurs d'autrui. Écrire un serveur à soi exige des concepts que tu n'as pas encore. La solution retenue par ce cours est un **laboratoire** : un petit programme qui lance un serveur HTTP sur ton propre ordinateur (`127.0.0.1`, qui est le nom de « cet ordinateur-ci »), avec des services qui répondent toujours la même chose et mettent toujours le même temps. Ainsi, les résultats peuvent être comparés à ceux de cette leçon, ligne par ligne.

Pour l'instant, le laboratoire est une **boîte noire** : tu vas l'exécuter, pas l'écrire. Il utilise des outils qui ne sont pas encore expliqués (un serveur HTTP fourni avec le JDK lui-même et un thread par requête), et c'est une exception à la règle de ne rien utiliser avant de l'avoir expliqué (il y en a une autre, petite, en 8.3.4) ; la [Leçon 10](10-el-programa-terminado.md) l'ouvre et s'en sert pour de bon. Enregistre-le sous `~/curso-java/Laboratorio.java`, **un niveau au-dessus du dossier du projet**, parce qu'il n'en fait pas partie :

```java
// fig08_01/Laboratorio.java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Contesta "ok" con el código dado, después de esperar el tiempo dado.
void responder(HttpExchange intercambio, int codigo, long esperaMs) throws IOException {
  try {
    Thread.sleep(esperaMs);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
  byte[] cuerpo = "ok\n".getBytes(StandardCharsets.UTF_8);
  intercambio.sendResponseHeaders(codigo, cuerpo.length);
  try (var salida = intercambio.getResponseBody()) {
    salida.write(cuerpo);
  }
}

// Atiende /espera/<ms>: contesta 200 después de <ms> milisegundos.
void responderEspera(HttpExchange intercambio) throws IOException {
  String ruta = intercambio.getRequestURI().getPath();
  try {
    responder(intercambio, 200, Long.parseLong(ruta.substring("/espera/".length())));
  } catch (NumberFormatException e) {
    responder(intercambio, 400, 0);
  }
}

void main() throws IOException {
  HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 8089), 128);
  servidor.createContext("/catalogo", i -> responder(i, 200, 142));
  servidor.createContext("/inventario", i -> responder(i, 200, 2300));
  servidor.createContext("/pagos", i -> responder(i, 200, 87));
  servidor.createContext("/facturacion", i -> responder(i, 503, 95));
  servidor.createContext("/notificaciones", i -> responder(i, 200, 10_000));
  servidor.createContext("/espera/", this::responderEspera);
  servidor.createContext("/basura", HttpExchange::close);
  servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
  servidor.start();
  IO.println("laboratorio escuchando en http://127.0.0.1:8089");
}
```

Et exécute-le **dans un autre terminal**, que tu laisses ouvert pendant que tu t'exerces. Il reste en marche, en attendant des requêtes, jusqu'à ce que tu l'arrêtes avec `Ctrl`+`C` :

```text
$ java Laboratorio.java
laboratorio escuchando en http://127.0.0.1:8089
```

Si, au contraire, tu vois `java.net.BindException: Address already in use`, c'est qu'un autre programme de ton ordinateur utilise déjà le port 8089 (un **port** est le numéro de la « porte » par où un serveur écoute). Ce qui libère le port, c'est **arrêter le processus** qui l'occupe, pas ouvrir un autre terminal : c'est presque toujours un laboratoire précédent qui tourne encore, et on l'arrête avec `Ctrl`+`C` dans le terminal où tu l'as lancé. Si tu ne retrouves pas ce terminal, `ss -ltnp 'sport = :8089'` montre quel processus écoute sur le 8089 (avec son nom et son numéro de processus), et avec ce numéro tu l'arrêtes. Voici ses chemins :

| Chemin | Ce qu'il répond | Et ce que le `revisor` devra en conclure |
|---|---|---|
| `/catalogo` | `200` après 142 ms | ARRIBA |
| `/inventario` | `200` après 2300 ms | LENTO |
| `/pagos` | `200` après 87 ms | ARRIBA |
| `/facturacion` | `503` après 95 ms | CAIDO (`503`) |
| `/notificaciones` | `200`, mais après 10 secondes | CAIDO si on lui met un délai maximal de 1 s |
| `/basura` | ferme la connexion sans répondre | CAIDO (`error de red`, « erreur réseau ») |
| `/espera/<ms>` | `200` après `<ms>` millisecondes | selon le temps |

Il manque un service des quatre habituels, et il n'est pas dans le laboratoire exprès : `reportes` pointe vers `http://127.0.0.1:8088/`, où **personne n'écoute**, pour avoir un service dont la connexion est refusée. Les quatre des tableaux habituels sont `catalogo`, `inventario`, `pagos` et `reportes` ; les autres sont pour les exercices et les tests.

#### 8.1.3 La première requête

Avec le laboratoire allumé, voici la requête la plus courte que l'on puisse faire avec Java 25 :

```java
// fig08_02.java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

void main() throws Exception {
  try (HttpClient cliente = HttpClient.newHttpClient()) {
    HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8089/pagos")).build();
    long inicio = System.nanoTime();
    HttpResponse<String> respuesta = cliente.send(peticion, BodyHandlers.ofString());
    long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
    IO.println("código de estado: " + respuesta.statusCode());
    IO.println("cuerpo: " + respuesta.body().strip());
    IO.println("tardó al menos 87 ms: " + (tiempoMs >= 87));
  }
}
```

Cela se lit de haut en bas. **`HttpClient`** est l'objet qui sait parler HTTP ; il se trouve dans le paquet `java.net.http` du JDK et n'a besoin d'aucune dépendance. Il s'ouvre avec `try (...)`, le `try-with-resources` de la Leçon 5 : un `HttpClient` garde des connexions et du travail interne, et le fermer les libère ; il est fermable (`AutoCloseable`) depuis Java 21, d'après sa [documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html). **`HttpRequest.newBuilder(URI.create(...)).build()`** construit la requête : un objet qui ne change plus, avec l'adresse où elle va. Si tu ne dis rien d'autre, la méthode est `GET`. **`cliente.send(peticion, BodyHandlers.ofString())`** l'envoie et **attend** la réponse ; tant qu'elle n'arrive pas, le programme n'avance pas. Le second paramètre dit quoi faire du corps : `ofString()` le convertit en texte, et le résultat est un `HttpResponse<String>`, la réponse, dont on demande le `statusCode()` et le `body()`.

Pour mesurer le temps mis, on prend l'horloge avant et après `send`, avec `System.nanoTime()`, et on soustrait. On utilise `nanoTime` et non `currentTimeMillis` parce que la [documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/System.html) de `nanoTime` le dit sans détour : elle sert à mesurer des **intervalles** de temps et n'a aucun rapport avec l'heure de la journée. L'heure d'une horloge peut sauter (à cause d'un ajustement automatique, du changement d'heure) au milieu d'une mesure, et l'intervalle sortirait négatif ou énorme. `nanoTime` fournit des nanosecondes, et `/ 1_000_000` les transforme en millisecondes ; le tiret bas dans le nombre est un séparateur que Java ignore, pour que l'œil s'y retrouve.

```text
$ java fig08_02.java
código de estado: 200
cuerpo: ok
tardó al menos 87 ms: true
```

Le code `200`, le corps `ok` que le laboratoire envoie, et une vérification : le temps a été, au moins, les 87 millisecondes que `/pagos` se fait attendre. (On imprime la vérification et non le nombre parce que le nombre change à chaque exécution.) `throws Exception` dans le `main` est un raccourci de cet exemple, qui laisse passer n'importe quelle exception ; dans le projet, chacune aura son traitement.

### 8.2 Le `HttpClient` du `revisor`

#### 8.2.1 Un client, plusieurs requêtes

Un `HttpClient` n'est pas seulement la fonction `send` : c'est un objet que l'on construit une fois et qui garde des choses qui valent la peine d'être réutilisées. La documentation le dit : une fois construit, **il est immuable** et peut servir à envoyer de nombreuses requêtes, et il partage des ressources entre toutes celles qui passent par lui (les connexions déjà ouvertes, par exemple, qu'il n'est pas nécessaire de renégocier). C'est pourquoi le `revisor` crée **un client par vérification** et l'utilise pour tous les services, et non un par service. Et on le crée avec le *builder* quand on veut configurer quelque chose que `newHttpClient()` ne configure pas, ce qui est le cas : il faut un délai de connexion et une politique de redirections explicite.

```java
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }
```

Deux configurations, et toutes deux sont des décisions, pas des ornements. `connectTimeout` fixe combien de temps on attend pour **établir la connexion** (plus en 8.3). `followRedirects(HttpClient.Redirect.NEVER)` dit « si le serveur répond par une redirection, ne la suis pas » ; c'est la valeur que le client a déjà par défaut, et on l'écrit quand même, **explicitement**, parce que c'est une décision de sécurité (8.8) et qu'une décision que quelqu'un a écrite est une décision que quelqu'un peut relire. La valeur par défaut, d'après la documentation, est `NEVER` ; la laisser implicite signifie que le jour où quelqu'un la changera, personne ne le remarquera.

#### 8.2.2 Que faire du corps : `BodyHandlers.discarding()`

`send` a besoin de savoir quoi faire du corps de la réponse, et il le demande avec un second paramètre, le **gestionnaire de corps** (*body handler*). Dans l'exemple précédent, on a utilisé `ofString()`, qui le lit en entier et le remet sous forme de texte. Le `revisor` n'a pas besoin du corps : seulement du code et du temps. Pour cela existe `BodyHandlers.discarding()` : il lit le corps et le **jette**, sans le conserver. Il y a une raison pratique derrière : si un service répond avec un corps de cent mégaoctets, `ofString()` les mettrait dans la mémoire de ton programme ; `discarding()` les lit et ne garde rien. Le type de la réponse change en conséquence : `HttpResponse<Void>`, une réponse dont le corps est « rien » (`Void`, avec une majuscule, est une classe du JDK qui n'a aucune valeur possible hormis `null` ; on s'en sert justement dans les génériques, où un paramètre de type doit être une classe et ne peut pas être le mot `void`, pour dire « ici il n'y a rien »).

### 8.3 Quand quelque chose échoue : délais maximaux, exceptions et redirections

#### 8.3.1 Deux délais maximaux, et pourquoi aucune requête ne s'envoie sans eux

C'est la section qui sépare le plus un programme d'entraînement d'un programme que l'on peut laisser tourner. La [documentation de `HttpRequest.Builder`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html) dit à propos du délai maximal d'une requête : « l'effet de ne pas mettre de délai maximal est le même que d'en mettre un infini, c'est-à-dire **attendre pour toujours** ». Si le serveur accepte la connexion et reste muet, ton programme reste à attendre, aujourd'hui, demain et l'année prochaine. Un `revisor` qui vérifie des services pour savoir s'ils sont en panne et qui se bloque à cause de l'un d'eux est pire que de ne pas avoir de `revisor` du tout.

C'est pourquoi il y a **deux** délais maximaux, et chacun couvre une étape différente :

| Délai | Où il se configure | Ce qu'il couvre | Ce qu'il lève quand il expire |
|---|---|---|---|
| **De connexion** | dans le client (`connectTimeout`) | établir la connexion avec le serveur | `HttpConnectTimeoutException` |
| **De la requête** | dans chaque requête (`timeout`) | attendre que la réponse arrive | `HttpTimeoutException` |

La connexion est l'étape du « quelqu'un répond-il à cette adresse ? », et le `revisor` lui donne deux secondes (`TIEMPO_CONEXION_MS = 2000`, la constante de `Revision`). C'est une décision du programme, pas une loi du réseau : sur un réseau lent ou congestionné, une connexion pourrait s'établir plus tard. Ce que dit la constante, c'est « si en deux secondes il n'y a pas de connexion, le `revisor` cesse d'attendre et signale `sin conexión` », ce qui, pour des services proches, comme ceux du laboratoire, est une marge large. La requête est l'étape du « on s'est occupé de moi, quand me répond-on ? », et son délai est **celui du service** : il vient de `servicio.timeoutMs()`, qui vaut 5000 par défaut et que le fichier de configuration peut changer pour un service que l'on sait lent. Le délai de connexion d'un client [n'a d'effet que lorsqu'il faut ouvrir une nouvelle connexion](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.Builder.html) ; s'il y en a déjà une ouverte d'une requête précédente, elle est réutilisée et il ne s'applique pas.

#### 8.3.2 Quelles exceptions arrivent, et quel détail leur revient

`send` déclare deux exceptions contrôlées : `InterruptedException`, que tu connais déjà de la Leçon 5, et `IOException`, qui regroupe **tout ce qui peut mal tourner sur le réseau**. Pour voir quel type concret arrive dans chaque cas, ce programme fait quatre requêtes vers des chemins du laboratoire et écrit la classe de l'exception et son message :

```java
// fig08_03.java
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

void intentar(HttpClient cliente, String url, int timeoutMs) throws InterruptedException {
  HttpRequest peticion =
      HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMillis(timeoutMs)).build();
  try {
    int codigo = cliente.send(peticion, BodyHandlers.discarding()).statusCode();
    IO.println(url + " -> contestó " + codigo);
  } catch (IOException e) {
    IO.println(url + " -> " + e.getClass().getName() + ", mensaje: " + e.getMessage());
  }
}

void main() throws InterruptedException {
  try (HttpClient cliente = HttpClient.newHttpClient()) {
    intentar(cliente, "http://127.0.0.1:8089/facturacion", 5000);
    intentar(cliente, "http://127.0.0.1:8088/", 5000);
    intentar(cliente, "http://127.0.0.1:8089/notificaciones", 1000);
    intentar(cliente, "http://127.0.0.1:8089/basura", 5000);
  }
}
```

```text
$ java fig08_03.java
http://127.0.0.1:8089/facturacion -> contestó 503
http://127.0.0.1:8088/ -> java.net.ConnectException, mensaje: null
http://127.0.0.1:8089/notificaciones -> java.net.http.HttpTimeoutException, mensaje: request timed out
http://127.0.0.1:8089/basura -> java.io.IOException, mensaje: HTTP/1.1 header parser received no bytes
```

Quatre chemins, quatre résultats différents, et une leçon dans chacun. `/facturacion` **a bien répondu** : la réponse est un `503`, et il n'y a pas d'exception : un serveur en panne n'en a souvent pas l'air, parce qu'il répond qu'il ne peut pas. Le port 8088, où personne n'écoute, a donné `ConnectException` (« impossible de se connecter ») et son message est **`null`** : l'exception la plus courante du réseau n'apporte pas de texte utile. `/notificaciones` fait attendre le client 10 secondes et le délai maximal était de 1 : `HttpTimeoutException`. Et `/basura` ouvre la connexion et la ferme sans rien dire : `IOException`, tout court, avec un message qui parle de l'intérieur du client (`HTTP/1.1 header parser received no bytes`).

De là sortent deux règles. La première, que le `revisor` traduit chaque type en **l'un de trois détails fixes** et n'affiche jamais le message de l'exception, pour deux raisons : parfois il vaut `null`, et quand il existe il change selon les versions du JDK et les systèmes d'exploitation. Le tableau de traduction est celui que tu connais depuis les premières leçons :

| L'exception | Le détail | Ce qui s'est passé |
|---|---|---|
| `HttpTimeoutException` (inclut `HttpConnectTimeoutException`) | `tiempo agotado` (« délai dépassé ») | le temps s'est écoulé, de connexion ou de requête |
| `ConnectException` | `sin conexión` (« sans connexion ») | port fermé, ou nom qui n'existe pas |
| toute autre `IOException` | `error de red` (« erreur réseau ») | la connexion s'est coupée ou la réponse n'a pas été comprise |

La seconde règle est d'**ordre**. `HttpTimeoutException` et `ConnectException` sont toutes deux des types d'`IOException` : ce sont des cas particuliers de celle-ci. Quand une exception peut être attrapée par son type particulier ou par le général, Java utilise le premier `catch` qui lui convient, donc **le type particulier vient avant le général**. Si tu les mets à l'envers, le compilateur ne te laisse pas faire :

```java
// orden/Orden.java
import java.io.IOException;
import java.net.ConnectException;

void probar() {
  try {
    throw new ConnectException();
  } catch (IOException e) {
    IO.println("general");
  } catch (ConnectException e) {
    IO.println("particular");
  }
}

void main() {
  probar();
}
```

```text
$ java Orden.java
Orden.java:9: error: exception ConnectException has already been caught
  } catch (ConnectException e) {
    ^
1 error
error: compilation failed
```

`exception ConnectException has already been caught` : le `catch (IOException)` du dessus attrape déjà toutes les `ConnectException`, de sorte que celui du dessous ne s'exécuterait jamais. C'est pourquoi on ordonne toujours du particulier au général. Avec ce tableau et cette règle, la méthode centrale du `revisor` se présente ainsi :

```java
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
```

Lis-la par étapes. On construit la requête, avec le délai maximal **du service** (`timeout`). On prend l'horloge. On envoie ; s'il y a eu une réponse, on mesure le temps et on construit un `Respondio` avec le code et les millisecondes (le `/ 1_000_000` convertit les nanosecondes). Et chaque panne devient un `SinRespuesta` avec son détail. À la fin, l'état ne se décide pas ici : on passe la réponse, quelle qu'elle soit, au `Clasificador` de la Leçon 3, qui ne sait rien des réseaux. Deux propriétés comptent plus que les autres. La première : **`revisar` ne laisse échapper aucune `IOException`** ; une panne du réseau est un résultat, pas une erreur du programme (la même idée que la Leçon 5 a appelée « résultat contre exception »). La seconde : l'`InterruptedException` **remonte**, comme on l'a appris, parce que celui qui interrompt le thread a droit à ce que le programme s'arrête.

#### 8.3.3 Redirections : `3xx` est une réponse

Il manque l'une des quatre habitudes du monde réel. Parfois un serveur répond avec un code `3xx` et un en-tête `Location` qui dit « maintenant je suis à cette autre adresse ». Un navigateur la suit sans que tu le remarques. Un client HTTP peut faire de même si on le lui demande. Ce programme lance son propre serveur, sur un port libre que le système choisit, avec un chemin qui redirige vers un autre, et interroge avec deux clients :

```java
// fig08_04.java
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

int consultar(HttpClient cliente, String url) throws IOException, InterruptedException {
  HttpRequest peticion = HttpRequest.newBuilder(URI.create(url)).build();
  return cliente.send(peticion, BodyHandlers.discarding()).statusCode();
}

void main() throws IOException, InterruptedException {
  HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  String base = "http://127.0.0.1:" + servidor.getAddress().getPort();
  servidor.createContext("/viejo", intercambio -> {
    intercambio.getResponseHeaders().add("Location", base + "/nuevo");
    intercambio.sendResponseHeaders(302, -1);
    intercambio.close();
  });
  servidor.createContext("/nuevo", intercambio -> {
    intercambio.sendResponseHeaders(200, -1);
    intercambio.close();
  });
  servidor.start();
  try (HttpClient porOmision = HttpClient.newHttpClient();
      HttpClient siguiendo =
          HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
    IO.println("cliente por omisión:      " + consultar(porOmision, base + "/viejo"));
    IO.println("cliente que las sigue:    " + consultar(siguiendo, base + "/viejo"));
  } finally {
    servidor.stop(0);
  }
}
```

```text
$ java fig08_04.java
cliente por omisión:      302
cliente que las sigue:    200
```

Le client par défaut n'a **pas** suivi la redirection : il a répondu `302`, le code de la réponse qu'il a reçue. Celui qui a été configuré avec `Redirect.NORMAL` est allé à la nouvelle adresse et a remis le `200` final. Le `revisor` est du premier type, et la règle de classification le dit sans ambiguïté : un `3xx` est une **réponse**, on ne la suit pas, et ce n'est pas une erreur. Un service qui redirige est vivant. Il y a en outre une raison de sécurité plus sérieuse qui est expliquée en 8.8 : une adresse que le fichier a autorisée n'a pas à mener à une adresse que le fichier n'a pas autorisée.

#### 8.3.4 `revisarTodos` et le test contre un vrai serveur

Avec `revisar` faite, `revisarTodos` devient court : un client, un `for`, l'ordre de la liste.

```java
// fig08_10/src/main/java/mx/habil/revisor/Revision.java

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
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;

/** Consulta los servicios por HTTP y arma sus resultados. */
final class Revision {

  /** Cuánto se espera para establecer la conexión, antes de decir que no hay respuesta. */
  static final long TIEMPO_CONEXION_MS = 2000;

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

  /** Revisa todos, uno tras otro (en serie), y conserva el orden de la lista. */
  static List<Resultado> revisarTodos(List<Servicio> servicios) throws InterruptedException {
    List<Resultado> resultados = new ArrayList<>();
    try (HttpClient cliente = nuevoCliente()) {
      for (Servicio servicio : servicios) {
        resultados.add(revisar(cliente, servicio));
      }
    }
    return resultados;
  }
}
```

Il est **en série** exprès : chaque service est vérifié quand le précédent a terminé. La raison de la présence d'un `try (HttpClient cliente = nuevoCliente())` autour est la fermeture : à la fin du bloc, `close()` attend qu'il n'y ait plus de requêtes en cours et libère tout. Et le coût de la série est visible, et c'est le sujet de la leçon suivante : 142 + 2300 + 87 millisecondes, c'est, grosso modo, ce que prend le rapport.

Il reste à surveiller `revisar` avec des tests. Il y a ici une difficulté que la Leçon 7 n'avait pas : le test a besoin d'**un serveur qui se comporte comme on veut** (qui réponde vite, qui échoue, qui reste muet), et on ne peut pas dépendre du laboratoire, qui peut être éteint sur un autre ordinateur. La solution est que le test lance le sien, avec le même `HttpServer` du JDK que le laboratoire, **à l'intérieur du test**, sur un port que le système choisit :

```java
// fig08_10/src/test/java/mx/habil/revisor/RevisionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pruebas de la revisión contra un servidor de verdad, levantado dentro de la prueba. */
class RevisionTest {

  private static HttpServer servidor;
  private static String base;

  @BeforeAll
  static void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/rapido", intercambio -> responder(intercambio, 200, 0));
    servidor.createContext("/falla", intercambio -> responder(intercambio, 503, 0));
    servidor.createContext("/lento", intercambio -> responder(intercambio, 200, 2000));
    servidor.createContext("/basura", HttpExchange::close);
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    base = "http://127.0.0.1:" + servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, int codigo, long esperaMs)
      throws IOException {
    try {
      Thread.sleep(esperaMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    intercambio.sendResponseHeaders(codigo, -1);
    intercambio.close();
  }

  private static Resultado revisar(String url, int timeoutMs) throws InterruptedException {
    Servicio servicio = new Servicio("prueba", URI.create(url), timeoutMs);
    try (HttpClient cliente = Revision.nuevoCliente()) {
      return Revision.revisar(cliente, servicio);
    }
  }

  @Test
  void unaRespuestaRapidaEsArriba() throws Exception {
    Resultado resultado = revisar(base + "/rapido", 5000);

    assertEquals(Estado.ARRIBA, resultado.estado());
    assertEquals("200", resultado.detalle());
  }

  @Test
  void unCodigo503EsCaido() throws Exception {
    Resultado resultado = revisar(base + "/falla", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("503", resultado.detalle());
  }

  @Test
  void unPuertoCerradoEsSinConexion() throws Exception {
    int puertoCerrado;
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      puertoCerrado = socket.getLocalPort();
    }

    Resultado resultado = revisar("http://127.0.0.1:" + puertoCerrado + "/", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("sin conexión", resultado.detalle());
  }

  @Test
  void unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado() throws Exception {
    Resultado resultado = revisar(base + "/lento", 200);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("tiempo agotado", resultado.detalle());
  }

  @Test
  void unaConexionCerradaSinRespuestaEsErrorDeRed() throws Exception {
    Resultado resultado = revisar(base + "/basura", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("error de red", resultado.detalle());
  }
}
```

Trois idées nouvelles. **Le port 0.** Demander `InetSocketAddress("127.0.0.1", 0)` signifie « n'importe quel port libre » ; le système en choisit un, et le test le lit ensuite avec `servidor.getAddress().getPort()`. Ainsi il n'entre jamais en conflit avec un autre programme. **`@BeforeAll` et `@AfterAll`** sont des annotations de JUnit pour du code qui s'exécute **une fois** avant tous les tests de la classe et une fois après tous ; lancer et arrêter le serveur dans chaque test serait lent. Les méthodes sont `static` par une règle de JUnit. Et **cinq tests, un par chemin** : réponse rapide (ARRIBA), un `503` (CAIDO), un port fermé (`sin conexión`), un chemin qui met 2 secondes avec un délai maximal de 200 millisecondes (`tiempo agotado`), et une connexion qui se ferme sans répondre (`error de red`). Le port fermé s'obtient de façon ingénieuse : on ouvre un socket sur le port 0, on note quel port lui est échu, et on le ferme ; ce numéro, on sait qu'il est libre. (Le cas LENTO n'est pas testé ici ; `ClasificadorTest` le surveille déjà, et attendre vraiment une seconde dans un test le rendrait lent sans rien enseigner de nouveau.) Une ligne du serveur mérite une note : `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` demande que chaque requête soit traitée dans son propre thread, pour que le chemin lent ne bloque pas les autres ; le concept est expliqué dans la Leçon 9, et pour l'instant il suffit de savoir que, sans cette ligne, les chemins seraient traités un par un.

Comme toujours, **on le voit d'abord en rouge**. Dans `Revision.java`, retire la ligne du `.timeout(...)` de la requête et lance `mvn -B verify` :

```text
$ mvn -B verify
...
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0 <<< FAILURE! -- in mx.habil.revisor.RevisionTest
[ERROR] mx.habil.revisor.RevisionTest.unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <CAIDO> but was: <LENTO>
[ERROR]   RevisionTest.unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado:92 expected: <CAIDO> but was: <LENTO>
[ERROR] Tests run: 23, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Sans le délai maximal, le chemin `/lento` fait attendre le client ses 2 secondes complètes, la réponse arrive (un `200`, tardif) et se classe LENTO, au lieu de CAIDO. Le test attend `CAIDO`, et passe au rouge avec un message qui dit justement ce qui ne va pas. C'est la même panne qui, en production, ne se produirait pas dans un test rapide mais avec un vrai service bloqué, et qui se verrait comme un rapport qui ne termine jamais. Rétablis la ligne.

#### 8.3.5 La fin de la première séance

Tu as tout pour que le `revisor` interroge pour de vrai. Dans ton dossier, change deux choses : supprime `Simulador.java` (son travail, c'est le réseau qui le fait), et fais en sorte que les adresses de `servicios.txt` pointent vers le laboratoire (`http://127.0.0.1:8089/catalogo`, et ainsi de suite pour les autres ; `reportes` va vers `http://127.0.0.1:8088/`). Copie `Revision.java` et `RevisionTest.java` ci-dessus. Avec le laboratoire allumé dans un autre terminal :

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Cela fait 23 tests : les 18 de la leçon précédente plus les cinq nouveaux de `RevisionTest`. Ceux de `ConfiguracionTest` sont toujours ceux du fichier texte ; dans la seconde séance, ils sont réécrits. Et le `revisor` interroge déjà pour de vrai :

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      92ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 861ms
```

Deux choses à remarquer. L'ESTADO et le DETALLE sont identiques à ceux de la leçon précédente, et la colonne TIEMPO n'est plus celle que tu avais écrite : c'est ton ordinateur qui l'a mesurée. Et le programme a mis, grosso modo, la somme de tous (un peu plus de 2.5 secondes), pas le plus lent : c'est le coût de vérifier en série. Il y a une preuve de plus qu'il interroge pour de vrai : éteins le laboratoire et relance ; les quatre services sortent CAIDO avec `sin conexión`, et la moyenne est un tiret, parce que personne n'a répondu. Cela clôt la première séance.


### 8.4 JSON : pourquoi le texte brut ne suffit plus

#### 8.4.1 Ce qu'un fichier texte ne peut pas dire

Le `servicios.txt` de la Leçon 5 avait un service par ligne, avec des champs séparés par des espaces, et le troisième, facultatif, était le délai maximal. Pour quatre services, c'est commode. Mais pose-lui trois questions. Comment distingue-t-il le nombre `1000` du texte `"1000"` ? Il ne les distingue pas : ce sont les mêmes quatre caractères, et le programme les convertit parce qu'il *sait* qu'à cet endroit va un nombre. Comment ajoute-t-il, au milieu, un champ que certains services ont et d'autres non ? Il ne peut pas, sans casser la position des autres. Comment dit-il qu'un service a une liste de choses, comme des étiquettes ou plusieurs responsables ? Pas davantage. Chaque fois que le fichier a besoin de dire quelque chose de nouveau, il faut inventer une convention et réécrire le lecteur.

**JSON** (*JavaScript Object Notation*, bien qu'aujourd'hui tous les langages l'utilisent) résout cela avec une idée simple : un format de texte qui **porte ses types écrits**. Selon sa [norme, la RFC 8259](https://www.rfc-editor.org/rfc/rfc8259), une valeur JSON est l'une de ces choses : un **objet** (`{"nombre": "pagos"}`, des paires clé-valeur entre accolades), un **tableau** (*array* : `[1, 2, 3]`, une liste entre crochets), une **chaîne** entre guillemets doubles, un **nombre**, ou l'une de trois valeurs fixes : `true`, `false` et `null` (« il n'y a pas de valeur »). C'est tout le format, et c'est de là que vient qu'on puisse le lire depuis n'importe quel langage. Et remarque ce qui a changé : `1000` (sans guillemets) est un nombre et `"1000"` (avec guillemets) est une chaîne, et le format les distingue dès le départ.

Voici le fichier de configuration du `revisor` à partir d'aujourd'hui, le `servicios.json`, avec les mêmes quatre services de toujours pointant maintenant vers le laboratoire :

```json fig08_10/servicios.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "inventario", "url": "http://127.0.0.1:8089/inventario"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"},
    {"nombre": "reportes", "url": "http://127.0.0.1:8088/"}
  ]
}
```

C'est un objet avec une clé, `servicios`, dont la valeur est un tableau d'objets, et chaque objet porte `nombre` et `url`. Il y a un troisième champ possible, `timeout_ms`, le délai maximal de la requête en millisecondes : il est **facultatif**, et qui l'omet obtient les 5000 millisecondes de toujours. Les clés sont en `snake_case` (des mots en minuscules reliés par un tiret bas, comme dans `timeout_ms`) parce que ce sont les mêmes que celles qu'utilise le [cours de Python](https://github.com/HabilMX/curso-python) : le fichier et le rapport se ressemblent dans les deux. Avec ce fichier, **`servicios.txt` se supprime** : on ne s'en sert plus.

Enfin, JSON occupe aussi une place dans la **sortie** du `revisor` : le rapport que livrera le programme pour qu'un autre programme le lise (le cours le fixe et il ne change plus à partir d'ici). Tu le verras en 8.7.

### 8.5 Jackson 3 : la dépendance, les paquets et le *classpath*

#### 8.5.1 Ce qu'est Jackson, et pourquoi il y en a deux

Java n'inclut pas de lecteur de JSON dans sa bibliothèque standard ; on utilise une bibliothèque, et la plus utilisée est **Jackson**. Il y a ici un piège qu'on paie une fois : Jackson a eu une version 2, qui domine presque tout ce qui est écrit sur internet (anciennes réponses, tutoriels, livres), et depuis 2025 il a une **version 3**, qui a changé de nom de paquet. La 2 vit dans `com.fasterxml.jackson...` ; la 3, dans **`tools.jackson...`**. Si tu copies un exemple d'internet et que son `import` commence par `com.fasterxml.jackson.databind`, il est de la version 2, et avec le projet de ce cours il ne compile pas. On peut le voir en deux lignes :

```java
// fig08_05.java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

void main() throws Exception {
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
  IO.println(raiz.get("servicios").size());
}
```

```text
$ java -cp "target/lib/*" fig08_05.java
fig08_05.java:2: error: package com.fasterxml.jackson.databind does not exist
import com.fasterxml.jackson.databind.JsonNode;
                                     ^
fig08_05.java:3: error: package com.fasterxml.jackson.databind does not exist
import com.fasterxml.jackson.databind.ObjectMapper;
                                     ^
fig08_05.java:6: error: cannot find symbol
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
  ^
  symbol:   class JsonNode
  location: class fig08_05
fig08_05.java:6: error: cannot find symbol
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
                      ^
  symbol:   class ObjectMapper
  location: class fig08_05
4 errors
error: compilation failed
```

`package com.fasterxml.jackson.databind does not exist`, et derrière, une cascade de `cannot find symbol` qui ne sont que la conséquence du premier. Le message est celui de toujours quand il manque une dépendance (tu l'as vu dans la Leçon 6), mais ici la dépendance de Jackson **est bien déclarée** ; ce qui n'existe pas dans ton projet, c'est la version 2. Quand un exemple de Jackson ne compile pas, vérifie d'abord quel paquet il importe. Le [guide officiel de migration vers Jackson 3](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) liste les changements : le nouveau paquet, le JDK minimal (Java 17), les mappers immuables qui se construisent avec un *builder*, et le fait que les exceptions ne sont plus contrôlées. Un seul détail reste à moitié : les **annotations** (`@JsonProperty` et compagnie) restent dans `com.fasterxml.jackson.annotation`, avec la version 2.x ; c'est la raison pour laquelle plus bas tu verras un fichier `jackson-annotations-2.22.jar` à côté de ceux de la version 3. Le cours utilise la 3.2.3, la dernière en octobre 2026.

#### 8.5.2 Déclarer la dépendance

Une **dépendance** est une bibliothèque que ton projet utilise et que tu n'as pas écrite. Elle se déclare dans le `pom.xml` avec ses coordonnées, comme tu as déclaré JUnit, mais avec deux différences : sans portée (la portée par défaut est `compile`, « le programme en a besoin pour compiler et pour tourner ») et avec une version écrite. Voici le `pom.xml` du projet de la leçon, complet ; ce qui est nouveau, c'est la dépendance de Jackson et un second *plugin*, qui est expliqué tout de suite :

```xml fig08_10/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.8.0</version>

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
      <groupId>tools.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>3.2.3</version>
    </dependency>
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
      <plugin>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.11.0</version>
        <executions>
          <execution>
            <id>copiar-dependencias</id>
            <phase>package</phase>
            <goals>
              <goal>copy-dependencies</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.build.directory}/lib</outputDirectory>
              <includeScope>runtime</includeScope>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

La dépendance, `tools.jackson.core:jackson-databind`, en apporte d'autres de son côté : Maven lit le `pom.xml` de Jackson, voit qu'il a besoin de `jackson-core` et de `jackson-annotations`, et les télécharge aussi. C'est ce qu'on appelle les **dépendances transitives**, et c'est la raison pour laquelle on écrit une ligne et on télécharge trois JAR. La version est montée à `0.8.0`, et celle de JUnit reste importée du même BOM.

#### 8.5.3 Le *classpath* et pourquoi `target/lib`

Compiler avec Maven fonctionne, parce que Maven sait quelles dépendances il y a et les passe à `javac`. Mais **exécuter** est autre chose, et c'est là qu'apparaît l'erreur la plus typique de la leçon. Avec la commande de la leçon précédente :

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError: tools/jackson/core/JacksonException`. La JVM a commencé à exécuter le programme, est arrivée à une ligne qui a besoin d'une classe de Jackson, et ne l'a pas trouvée. Rappelle-toi ce que signifiait `-cp` : la liste des endroits où la JVM cherche des classes, le ***classpath***. Dans les Leçons 6 et 7, `target/classes` suffisait, parce que tout le code était à toi. Maintenant il faut ajouter les JAR de Jackson. Maven les garde dans `~/.m2/repository`, mais ce chemin est long et dépend de la version ; pour ne pas l'écrire, le `pom.xml` ajoute le *plugin* `maven-dependency-plugin`, qui, dans la phase `package`, **copie les dépendances du programme dans `target/lib`** :

```text
$ ls target/lib
jackson-annotations-2.22.jar
jackson-core-3.2.3.jar
jackson-databind-3.2.3.jar
```

Et on exécute avec les deux dans le *classpath*, `target/classes` (ton code) et `target/lib/*` (les JAR ; le `*` demande « tous les `.jar` de ce dossier »), séparés par deux-points, qui est le séparateur sous Linux :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
```

Les guillemets servent à ce que le terminal n'essaie pas d'étendre le `*` lui-même. Et comme les JAR sont copiés dans la phase `package`, il faut avoir exécuté `mvn -B verify` (ou `package`) au moins une fois pour que `target/lib` existe. C'est ce qui gêne dans un *classpath* à la main, et c'est pourquoi la [Leçon 10](10-el-programa-terminado.md) le résout avec un seul `.jar` qui contient tout : tu l'as déjà subi, donc tu sauras quel problème il résout.

### 8.6 Lire un JSON comme un arbre et valider à la main

#### 8.6.1 `JsonNode` : l'arbre

Jackson offre deux façons de lire un JSON. La première le convertit en un **arbre** d'objets `JsonNode`, un pour chaque valeur, que tu peux parcourir et interroger. La seconde le mappe directement vers un *record* à toi. Commençons par la première, qui est celle qu'utilise la configuration :

```java
// fig08_06.java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

void main() {
  String texto = """
      {
        "servicios": [
          {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
          {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos", "timeout_ms": 1000}
        ]
      }
      """;
  JsonMapper mapeador = JsonMapper.builder().build();
  JsonNode raiz = mapeador.readTree(texto);

  JsonNode lista = raiz.get("servicios");
  IO.println("¿es lista? " + lista.isArray() + ", elementos: " + lista.size());

  JsonNode segundo = lista.get(1);
  IO.println("nombre: " + segundo.get("nombre").stringValue());
  IO.println("timeout_ms: " + segundo.get("timeout_ms").intValue());

  IO.println("llave que no existe -> " + lista.get(0).get("timeout_ms"));
  IO.println("la misma con path()  -> " + lista.get(0).path("timeout_ms").isMissingNode());
}
```

```text
$ java -cp "target/lib/*" fig08_06.java
¿es lista? true, elementos: 2
nombre: pagos
timeout_ms: 1000
llave que no existe -> null
la misma con path()  -> true
```

Les noms des méthodes sont ceux de Jackson 3 (et cela vaut la peine d'y faire attention, car presque tout ce que tu trouveras sur internet utilise ceux de la 2) : `readTree` convertit le texte en arbre et renvoie sa racine ; `get("servicios")` demande la valeur d'une clé d'un objet, et `get(1)` l'élément d'un tableau ; `isArray()` demande de quel type de valeur il s'agit ; `size()` compte les éléments ; `stringValue()` remet le texte d'un nœud de texte et `intValue()` le nombre entier d'un nœud numérique. Les anciens noms (`asText`, `isTextual`) existent encore pour qui migre, mais le code nouveau utilise `stringValue` et `isString`. Et il y a un détail qui décide de nombreuses erreurs : `get("timeout_ms")` sur une clé qui **n'existe pas** renvoie `null` de Java, pas un nœud ; pour demander sans risque de `NullPointerException`, il y a `path("timeout_ms")`, qui renvoie un nœud « absent » (`isMissingNode()`) au lieu de `null`. Ce que le `revisor` en fait, c'est toujours de vérifier d'abord si c'est `null` et de demander ensuite le type.

#### 8.6.2 Le piège : mapper directement vers un *record* accepte ce qu'il ne doit pas

La seconde façon est tentante, parce que c'est deux lignes et que les types de Java s'occupent de tout : tu déclares un *record* avec les champs que tu veux et Jackson le remplit.

```java
// fig08_07.java
import tools.jackson.core.JacksonException;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

record Config(String nombre, int timeoutMs) {}

void probar(JsonMapper mapeador, String valor) {
  String json = "{\"nombre\": \"pagos\", \"timeout_ms\": " + valor + "}";
  try {
    Config config = mapeador.readValue(json, Config.class);
    IO.println("timeout_ms: " + valor + " -> entra como " + config.timeoutMs());
  } catch (JacksonException e) {
    IO.println("timeout_ms: " + valor + " -> rechazado (" + e.getClass().getSimpleName() + ")");
  }
}

void main() {
  JsonMapper mapeador =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
  probar(mapeador, "1000");
  probar(mapeador, "1.5");
  probar(mapeador, "\"1000\"");
  probar(mapeador, "true");
  probar(mapeador, "-5");
}
```

```text
$ java -cp "target/lib/*" fig08_07.java
timeout_ms: 1000 -> entra como 1000
timeout_ms: 1.5 -> entra como 1
timeout_ms: "1000" -> entra como 1000
timeout_ms: true -> rechazado (MismatchedInputException)
timeout_ms: -5 -> entra como -5
```

Regarde les cinq lignes. `1000` entre comme 1000, bien. Mais **`1.5` entre comme `1`** : Jackson tronque la décimale en silence. **`"1000"` entre comme `1000`** : il convertit le texte en nombre sans prévenir. **`-5` entre comme `-5`** : un délai maximal négatif, que `HttpRequest` rejetterait avec une exception quand le programme l'utilisera, loin de l'endroit où il a été écrit. Seul `true` a été rejeté, avec une exception de Jackson (`MismatchedInputException`). Le point n'est pas que Jackson ait tort : c'est qu'il fait ce qu'on lui a demandé, qui était de *convertir*, et non ce dont on a besoin, qui est de *vérifier*. Un fichier de configuration avec `"timeout_ms": "1000"` est presque sûrement une erreur de celui qui l'a écrit, et on veut le savoir.

La leçon est générale : **ce qui vient de l'extérieur se valide là où ça entre**, une fois, avec des règles explicites, et après cette validation le reste du programme travaille avec des types qui sont déjà de confiance (`Servicio` a un `int` et un `URI`, pas un texte qui était peut-être un nombre). C'est pourquoi la configuration se lit comme **arbre** et se valide à la main avec `isInt()`, `isString()` et `isObject()`, et le mapping direct vers des *records* est réservé à **l'écriture** (8.7), où les données sont contrôlées par le programme et non par une personne.

#### 8.6.3 La configuration, validée

La classe `Configuracion` est réécrite pour lire du JSON. Lis-la comme une cascade : `leer` ouvre le fichier et valide la forme générale, `leerServicio` valide un service, et chaque champ a sa petite méthode.

```java
// fig08_10/src/main/java/mx/habil/revisor/Configuracion.java

package mx.habil.revisor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.JsonNode;

/** Lee la lista de servicios de servicios.json y desconfía de todo lo que trae. */
final class Configuracion {

  /** Los únicos servidores a los que el revisor acepta consultar. */
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");

  private Configuracion() {}

  /** Lee y valida el archivo completo; cualquier problema sale como un solo tipo de error. */
  static List<Servicio> leer(Path ruta, Set<String> permitidos) throws ErrorDeConfiguracion {
    String nombreArchivo = String.valueOf(ruta.getFileName());
    JsonNode raiz = leerArbol(ruta, nombreArchivo);
    JsonNode lista = raiz.get("servicios");
    if (lista == null || !lista.isArray()) {
      throw new ErrorDeConfiguracion("falta la lista 'servicios'");
    }
    List<Servicio> servicios = new ArrayList<>();
    for (int i = 0; i < lista.size(); i++) {
      servicios.add(leerServicio(lista.get(i), i + 1, permitidos));
    }
    verificarNombresUnicos(servicios);
    return servicios;
  }

  private static JsonNode leerArbol(Path ruta, String nombreArchivo) throws ErrorDeConfiguracion {
    String texto;
    try {
      texto = Files.readString(ruta, StandardCharsets.UTF_8);
    } catch (NoSuchFileException e) {
      throw new ErrorDeConfiguracion("no existe el archivo " + nombreArchivo, e);
    } catch (IOException e) {
      throw new ErrorDeConfiguracion("no se pudo leer el archivo " + nombreArchivo, e);
    }
    try {
      return Json.MAPEADOR.readTree(texto);
    } catch (JacksonException e) {
      TokenStreamLocation lugar = e.getLocation();
      String donde =
          lugar == null
              ? ""
              : ": renglón %d, columna %d".formatted(lugar.getLineNr(), lugar.getColumnNr());
      throw new ErrorDeConfiguracion(nombreArchivo + " no es JSON válido" + donde, e);
    }
  }

  /** Valida un elemento de la lista y lo convierte en un servicio. */
  static Servicio leerServicio(JsonNode nodo, int posicion, Set<String> permitidos)
      throws ErrorDeConfiguracion {
    String prefijo = "servicio " + posicion + ": ";
    if (!nodo.isObject()) {
      throw new ErrorDeConfiguracion(prefijo + "debe ser un objeto");
    }
    String nombre = leerNombre(nodo, prefijo);
    URI url = leerUrl(nodo, prefijo, permitidos);
    JsonNode timeout = nodo.get("timeout_ms");
    if (timeout == null) {
      return new Servicio(nombre, url);
    }
    if (!timeout.isInt() || timeout.intValue() <= 0) {
      throw new ErrorDeConfiguracion(prefijo + "'timeout_ms' debe ser un entero mayor que 0");
    }
    return new Servicio(nombre, url, timeout.intValue());
  }

  private static String leerNombre(JsonNode nodo, String prefijo) throws ErrorDeConfiguracion {
    JsonNode nombre = nodo.get("nombre");
    if (nombre == null) {
      throw new ErrorDeConfiguracion(prefijo + "falta 'nombre'");
    }
    if (!nombre.isString() || nombre.stringValue().isBlank()) {
      throw new ErrorDeConfiguracion(
          prefijo + "'nombre' debe ser un texto que no esté vacío");
    }
    return nombre.stringValue();
  }

  private static URI leerUrl(JsonNode nodo, String prefijo, Set<String> permitidos)
      throws ErrorDeConfiguracion {
    JsonNode campo = nodo.get("url");
    if (campo == null) {
      throw new ErrorDeConfiguracion(prefijo + "falta 'url'");
    }
    if (!campo.isString()) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    URI url;
    try {
      url = new URI(campo.stringValue());
    } catch (URISyntaxException e) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida", e);
    }
    String esquema = url.getScheme();
    if (!"http".equals(esquema) && !"https".equals(esquema)) {
      throw new ErrorDeConfiguracion(prefijo + "'url' debe empezar con http:// o https://");
    }
    String servidor = url.getHost();
    if (servidor == null) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    if (!permitidos.contains(servidor)) {
      throw new ErrorDeConfiguracion(
          prefijo + "el servidor " + servidor + " no está en la lista permitida");
    }
    return url;
  }

  private static void verificarNombresUnicos(List<Servicio> servicios)
      throws ErrorDeConfiguracion {
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

Trois décisions méritent un commentaire. **La première : chaque erreur est une `ErrorDeConfiguracion` avec un message qui dit quoi corriger et où.** `servicio 2: falta 'nombre'` est un message qu'une personne peut utiliser ; un `NullPointerException` à la ligne 57, non. La position (`servicio 2`) compte à partir de 1, comme la compte une personne. **La deuxième : les erreurs de Jackson sont traduites.** `JacksonException` est une exception **non contrôlée** dans la version 3 (l'une des différences avec la 2), donc le compilateur ne t'oblige pas à l'attraper, et c'est pourquoi il est facile de l'oublier et de laisser un fichier mal écrit faire tomber le programme avec une trace. Le `catch` la convertit en `ErrorDeConfiguracion`, et au passage profite du fait que l'exception sait à quelle ligne et à quelle colonne le fichier est cassé (`getLocation()`). **La troisième : le fichier est d'abord lu en texte avec `Files.readString`**, avec la gestion d'erreurs de la Leçon 5 (`NoSuchFileException` devient « le fichier n'existe pas »), et c'est seulement alors qu'on passe le texte à Jackson. Chaque erreur a une seule origine, et chacune est traitée à sa place.

Voici les messages qu'il produit. La position compte à partir de 1. Les tests ci-dessous surveillent les plus dangereux ; pour les autres, c'est toi qui les provoques dans l'exercice 3 :

| Cas | Message |
|---|---|
| le fichier n'existe pas | `no existe el archivo servicios.json` |
| JSON mal écrit | `servicios.json no es JSON válido: renglón 4, columna 5` |
| la liste manque | `falta la lista 'servicios'` |
| le service n'est pas un objet | `servicio 2: debe ser un objeto` |
| un champ manque | `servicio 2: falta 'nombre'` |
| nom vide ou qui n'est pas un texte | `servicio 2: 'nombre' debe ser un texto que no esté vacío` |
| url qui n'est pas `http`/`https` | `servicio 2: 'url' debe empezar con http:// o https://` |
| url sans hôte ou mal écrite | `servicio 2: 'url' no es válida` |
| hôte hors de la liste autorisée | `servicio 2: el servidor 169.254.169.254 no está en la lista permitida` |
| délai maximal invalide | `servicio 3: 'timeout_ms' debe ser un entero mayor que 0` |
| noms répétés | `nombres repetidos: catalogo` |

Avec un fichier auquel il manque une virgule entre deux services (ici, celle de la fin de la ligne de `catalogo`), le `revisor` dit exactement où :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

`renglón 4, columna 5` (« ligne 4, colonne 5 ») : le point où Jackson attendait une virgule et a trouvé le `{` qui ouvre le service suivant. Avec un fichier vide, il dit ce qui manque et non une trace ; et avec une liste de textes là où il devait y avoir des objets, il dit quel service est le fautif :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

La classe de tests est réécrite avec les cas du JSON. C'est la même structure que la Leçon 7, avec une nouveauté : la table de cas est maintenant faite de fichiers JSON de plusieurs lignes, et une table de `@CsvSource` avec cela serait illisible. JUnit a une autre source de données, **`@MethodSource`**, qui pointe vers une méthode (ici `archivosInvalidos`) qui renvoie un `Stream<Arguments>`, une liste de lignes où chaque `Arguments.of(...)` apporte les valeurs de l'une d'elles. Les blocs de texte (`"""`) de la Leçon 2 permettent d'écrire chaque JSON tel que tu le vois dans un fichier.

```java
// fig08_10/src/test/java/mx/habil/revisor/ConfiguracionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Pruebas de la lectura y validación de servicios.json. */
class ConfiguracionTest {

  @TempDir Path carpeta;

  private Path escribir(String json) throws IOException {
    Path ruta = carpeta.resolve("servicios.json");
    Files.writeString(ruta, json);
    return ruta;
  }

  @Test
  void leeUnArchivoValido() throws Exception {
    Path ruta =
        escribir(
            """
            {"servicios": [
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
              {"nombre": "pagos", "url": "http://localhost:8089/pagos", "timeout_ms": 1000}
            ]}
            """);

    List<Servicio> servicios = Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS);

    assertEquals(2, servicios.size());
    assertEquals("catalogo", servicios.get(0).nombre());
    assertEquals(Servicio.TIMEOUT_POR_OMISION_MS, servicios.get(0).timeoutMs());
    assertEquals(1000, servicios.get(1).timeoutMs());
  }

  static Stream<Arguments> archivosInvalidos() {
    return Stream.of(
        Arguments.of(
            """
            {"servicios": [{"nombre": "a", "url": "ftp://127.0.0.1/a"}]}
            """,
            "servicio 1: 'url' debe empezar con http:// o https://"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a"},
              {"nombre": "b", "url": "http://169.254.169.254/latest/meta-data/"}
            ]}
            """,
            "servicio 2: el servidor 169.254.169.254 no está en la lista permitida"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": 1.5}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": "1000"}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [{"url": "http://127.0.0.1:8089/a"}]}
            """,
            "servicio 1: falta 'nombre'"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/a"},
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/b"}
            ]}
            """,
            "nombres repetidos: catalogo"));
  }

  @ParameterizedTest
  @MethodSource("archivosInvalidos")
  void rechazaUnArchivoInvalido(String json, String mensajeEsperado) throws IOException {
    Path ruta = escribir(json);

    ErrorDeConfiguracion error =
        assertThrows(
            ErrorDeConfiguracion.class,
            () -> Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS));

    assertEquals(mensajeEsperado, error.getMessage());
  }
}
```

Sept tests : le cas nominal et les six rejets les plus dangereux : une URL qui n'est pas `http`, un hôte hors de la liste (le `169.254.169.254` de 8.8), les deux délais maximaux que le mapping direct aurait laissé passer (`1.5` et `"1000"`), un service sans nom et des noms répétés. Les deux du délai maximal sont la raison d'être de la section précédente : si quelqu'un « simplifie » la validation en un mapping direct, ces deux lignes passent au rouge.

### 8.7 Écrire le rapport en JSON

Écrire est l'autre direction, et ici il convient que Jackson travaille pour toi, parce que les données sont produites par le programme et non par une personne. On lui remet un *record* et il renvoie le texte. Voici l'exemple minimal, avec les noms qui ont été montrés avant la validation :

```java
// fig08_08.java
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

record Fila(String nombre, String estado, Long tiempoMs, String detalle) {}

record Resumen(int total, Long promedioMs) {}

record Reporte(List<Fila> servicios, Resumen resumen) {}

void main() {
  JsonMapper mapeador =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
  Reporte reporte =
      new Reporte(
          List.of(
              new Fila("pagos", "ARRIBA", 87L, "200"),
              new Fila("reportes", "CAIDO", null, "sin conexión")),
          new Resumen(2, 87L));
  IO.println(mapeador.writerWithDefaultPrettyPrinter().writeValueAsString(reporte));
}
```

```text
$ java -cp "target/lib/*" fig08_08.java
{
  "servicios" : [ {
    "nombre" : "pagos",
    "estado" : "ARRIBA",
    "tiempo_ms" : 87,
    "detalle" : "200"
  }, {
    "nombre" : "reportes",
    "estado" : "CAIDO",
    "tiempo_ms" : null,
    "detalle" : "sin conexión"
  } ],
  "resumen" : {
    "total" : 2,
    "promedio_ms" : 87
  }
}
```

`writerWithDefaultPrettyPrinter()` produit le texte avec indentation ; sans lui, tout sort sur une seule ligne (ce que préfère une machine). L'option `SNAKE_CASE`, que l'on met **une seule fois** à la construction du mapper, fait que `tiempoMs` sort comme `tiempo_ms` : le *record* s'écrit dans le style de Java (`camelCase`) et le JSON dans celui du contrat. Et regarde le service qui n'a pas répondu : son `tiempo_ms` est `null`, ni zéro ni absent. C'est une information : cela signifie « non mesuré », et c'est le champ de type `Long` (avec une majuscule, le type enveloppe de la Leçon 2, qui admet `null`) et non `long` qui le permet. Les espaces autour des deux-points (`"nombre" : "pagos"`) sont ceux du formateur de Jackson, qui ne sont pas ceux d'autres langages ; ils sont valides et n'importe quel lecteur de JSON les accepte.

Dans le `revisor`, il y a trois pièces. La première, `Json`, est **le** mapper du programme, un seul, configuré une fois (ce sont des objets pensés pour être réutilisés) :

```java
// fig08_10/src/main/java/mx/habil/revisor/Json.java

package mx.habil.revisor;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/** El lector y escritor de JSON del revisor: uno solo, configurado una vez. */
final class Json {

  /** Escribe los nombres en snake_case: {@code tiempoMs} sale como {@code tiempo_ms}. */
  static final JsonMapper MAPEADOR =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

  private Json() {}
}
```

La deuxième, `ReporteJson`, est la **forme** du JSON, séparée exprès des types internes du programme. `Reporte`, `Resultado` et `Resumen` sont les classes avec lesquelles le programme travaille ; `ReporteJson` est le contrat avec celui qui lira le rapport. Si demain tu changes quelque chose d'interne (la façon dont les temps sont conservés, un nom), le JSON ne change pas à moins que tu ne le décides, et non par accident :

```java
// fig08_10/src/main/java/mx/habil/revisor/ReporteJson.java

package mx.habil.revisor;

import java.util.List;
import java.util.OptionalLong;

/** La forma del reporte en JSON: es el contrato con quien lo consuma. */
record ReporteJson(List<Fila> servicios, ResumenJson resumen) {

  ReporteJson {
    servicios = List.copyOf(servicios);
  }

  /** Un servicio en el JSON; {@code tiempoMs} es nulo si no hubo respuesta. */
  record Fila(String nombre, String url, Estado estado, Long tiempoMs, String detalle) {}

  /** Las cuentas del JSON; {@code promedioMs} es nulo si nadie respondió. */
  record ResumenJson(int total, int arriba, int lento, int caido, Long promedioMs) {}

  /** Convierte el reporte del programa en su forma de JSON. */
  static ReporteJson de(Reporte reporte) {
    List<Fila> filas =
        reporte.resultados().stream()
            .map(
                r ->
                    new Fila(
                        r.servicio().nombre(),
                        r.servicio().url().toString(),
                        r.estado(),
                        nulo(r.tiempoMs()),
                        r.detalle()))
            .toList();
    Resumen resumen = reporte.resumen();
    return new ReporteJson(
        filas,
        new ResumenJson(
            resumen.total(),
            resumen.arriba(),
            resumen.lento(),
            resumen.caido(),
            nulo(resumen.promedioMs())));
  }

  private static Long nulo(OptionalLong valor) {
    return valor.isPresent() ? valor.getAsLong() : null;
  }
}
```

La méthode `de` convertit un `Reporte` en sa forme de JSON, et la conversion d'`OptionalLong` en `Long` (`nulo`) est ce qui produit le `null` des services sans réponse. Le constructeur compact avec `List.copyOf` est la copie défensive de la Leçon 4 ; la Leçon 10 montre ce que dit (et ce que ne dit pas) un outil d'analyse à son sujet. La troisième pièce est `Formato`, qui gagne une méthode, `json`, et c'est le seul changement d'un fichier dont tu connais déjà le reste :

```java
// fig08_10/src/main/java/mx/habil/revisor/Formato.java

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

  /** El reporte como JSON con sangría, listo para que lo lea otro programa. */
  static String json(Reporte reporte) {
    return Json.MAPEADOR
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(ReporteJson.de(reporte));
  }
}
```

Le `revisor` n'offre pas encore d'option pour demander ce JSON (elle arrive avec la ligne de commande de la Leçon 10), mais tu peux le voir en remplaçant temporairement, dans `Revisor.java`, la ligne qui imprime le tableau par `System.out.println(Formato.json(reporte));`. Voici comment commence ce qu'il imprime :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
{
  "servicios" : [ {
    "nombre" : "catalogo",
    "url" : "http://127.0.0.1:8089/catalogo",
    "estado" : "ARRIBA",
```

C'est le JSON que le cours fixe pour le rapport du `revisor`, le même que celui du [cours de Python](https://github.com/HabilMX/curso-python) : `servicios` est une liste avec une entrée par service, `tiempo_ms` est `null` pour celui qui n'a pas répondu, et `resumen` répète les comptes. Rétablis la ligne du tableau avant de continuer.

### 8.8 SSRF : l'URL est écrite par quelqu'un d'autre

#### 8.8.1 Le problème, avec un exemple concret

Regarde de nouveau `servicios.json`. Chaque `url` est une adresse à laquelle **ton programme se connecte, depuis ton réseau, avec les permissions de ta machine**. Imagine qu'aujourd'hui c'est toi qui écris le fichier, mais que demain quelqu'un monte un écran où une autre personne ajoute des services, ou qu'un fichier d'une autre équipe soit copié à cet endroit. Cette personne n'écrit plus une donnée : elle écrit **une instruction pour ton serveur**. Quelle adresse mettrait un attaquant ? Une adresse qu'il ne peut pas atteindre de l'extérieur mais que ton serveur peut atteindre. Dans le cloud, il en existe une très connue : `http://169.254.169.254/latest/meta-data/`. C'est une adresse « de lien local », de celles qui ne sortent pas du réseau de la machine elle-même, qui sur AWS (et avec des mécanismes semblables dans d'autres clouds) répond avec les données du serveur et, dans certaines configurations, avec des identifiants temporaires ; la [documentation d'AWS](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html) la décrit. Un `revisor` sans défenses se connecterait, dirait `ARRIBA`, et aurait fait ce que voulait l'attaquant sans que personne ne touche au serveur.

On appelle cette classe de faille **SSRF** (*Server-Side Request Forgery*, « falsification de requêtes côté serveur ») : obtenir qu'un serveur fasse une requête là où il n'aurait pas dû. Elle occupe la dixième place de la [liste OWASP Top 10 de 2021](https://owasp.org/Top10/A10_2021-Server-Side_Request_Forgery_%28SSRF%29/), le catalogue de risques des applications web le plus cité, et elle a son entrée dans le catalogue des faiblesses logicielles, [CWE-918](https://cwe.mitre.org/data/definitions/918.html). Le `revisor` ne lit pas le corps des réponses, ce qui réduit les dégâts (l'attaquant saurait que quelque chose a répondu, pas ce que cela a dit), mais cela révèle tout de même ce qui existe à l'intérieur du réseau, et un programme qui lirait un jour ce corps le livrerait.

La défense n'est pas une seule chose, c'est une pile de quatre, et chacune couvre ce que la précédente laisse passer. C'est ce qu'on appelle la *défense en profondeur*.

#### 8.8.2 Première défense : le schéma

Une URL commence par un **schéma**, la partie avant les deux-points : `http`, `https`, `ftp`, `file`. Le `revisor` ne parle que HTTP, donc il en accepte deux, et le vérifie avec `URI.getScheme()`, comme tu l'as vu dans `Configuracion`. Les autres schémas, certains dangereux (`file:///etc/passwd` pointe vers des fichiers de la machine elle-même), sont rejetés. Le `HttpClient` est déjà prudent de lui-même, et la dernière ligne de ce programme le montre, tout en enseignant comment Java lit une adresse :

```java
// fig08_09.java
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;

void mostrar(String texto) {
  try {
    URI url = new URI(texto);
    IO.println(texto + " -> esquema " + url.getScheme() + ", servidor " + url.getHost());
  } catch (URISyntaxException e) {
    IO.println(texto + " -> no es una URI válida");
  }
}

void main() {
  mostrar("http://127.0.0.1:8089/pagos");
  mostrar("http://LOCALHOST:8089/pagos");
  mostrar("http://127.0.0.1@169.254.169.254/latest/meta-data/");
  mostrar("http://127.0.0.1.otro.example/pagos");
  mostrar("http://[::1]:8089/pagos");
  mostrar("http://127.0.0.1:puerto/pagos");
  mostrar("file:///etc/passwd");
  mostrar("pagos");
  try {
    HttpRequest.newBuilder(URI.create("file:///etc/passwd"));
  } catch (IllegalArgumentException e) {
    IO.println("HttpRequest rechaza file: -> " + e.getMessage());
  }
}
```

```text
$ java fig08_09.java
http://127.0.0.1:8089/pagos -> esquema http, servidor 127.0.0.1
http://LOCALHOST:8089/pagos -> esquema http, servidor LOCALHOST
http://127.0.0.1@169.254.169.254/latest/meta-data/ -> esquema http, servidor 169.254.169.254
http://127.0.0.1.otro.example/pagos -> esquema http, servidor 127.0.0.1.otro.example
http://[::1]:8089/pagos -> esquema http, servidor [::1]
http://127.0.0.1:puerto/pagos -> esquema http, servidor null
file:///etc/passwd -> esquema file, servidor null
pagos -> esquema null, servidor null
HttpRequest rechaza file: -> invalid URI scheme file
```

`file:` ne passe même pas le constructeur de la requête (`invalid URI scheme file`). Mais ne te fie pas seulement à cela : le `revisor` rejette le schéma avant, avec un message clair, dans la configuration.

#### 8.8.3 Deuxième défense : la liste autorisée d'hôtes

Avec un schéma `http`, il reste `http://169.254.169.254/`. La deuxième défense est celle qui pèse le plus : une **liste autorisée** d'hôtes. Au lieu d'essayer de deviner quelles adresses sont dangereuses (il y en a trop, et on en oublie toujours une), on énumère les seules que l'on accepte et on rejette tout le reste :

```java
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");
```

Aujourd'hui la liste est `127.0.0.1` et `localhost` : la machine elle-même, où vit le laboratoire. Dans la Leçon 10, la liste devient configurable depuis la ligne de commande. Vérifie le rejet avec un fichier qui pointe vers l'hôte dangereux :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

Avant que le programme n'envoie un seul paquet, la configuration a déjà refusé, avec un message qui dit quel hôte et pourquoi. Remarque comment on compare : **avec `url.getHost()`, l'hôte que l'analyseur d'URI a extrait, et par égalité exacte**. Regarde ce que renvoie cette fonction dans le programme précédent pour des adresses malicieuses : `http://127.0.0.1@169.254.169.254/latest/meta-data/` ressemble, à première vue, à une adresse de `127.0.0.1` ; mais pour HTTP, ce qui vient avant un arobase est un utilisateur (l'utilisateur `127.0.0.1` !), et l'hôte est `169.254.169.254`. Une validation faite avec `texto.startsWith("http://127.0.0.1")` laisserait passer justement cette attaque ; celle faite avec `getHost()` la rejette. Et `http://127.0.0.1.otro.example/` commence par l'IP mais c'est un autre hôte, et `http://127.0.0.1:puerto/` n'a même pas d'hôte reconnaissable (le `revisor` le rejette comme « no es válida »). La règle : **ne valide jamais une adresse avec des opérations sur le texte ; analyse-la avec l'analyseur d'URI et compare ses parties.**

La liste distingue majuscules et minuscules, et rejette des choses valides, comme `http://LOCALHOST:8089/` ou la forme IPv6 de la machine elle-même, `http://[::1]:8089/` :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

C'est délibéré : une liste autorisée doit être **stricte par défaut**. Si un jour il faut accepter une autre forme, on l'ajoute à la liste exprès, et on ne relâche pas la comparaison.

#### 8.8.4 Troisième défense : ne pas suivre les redirections

Un serveur qui est bien dans la liste peut répondre par une redirection vers un qui n'y est pas. Si le client la suivait, la liste autorisée serait contournée par la porte de derrière : tu as autorisé `127.0.0.1`, et le client finirait à `169.254.169.254`. C'est pourquoi le client du `revisor` utilise `Redirect.NEVER` explicite (8.2.1) et traite un `3xx` comme une réponse de plus (8.3.3) : ce qui n'est pas suivi ne peut pas être exploité contre toi. Avertissement honnête : **aucun des 25 tests ne surveille cette défense**. On l'a mesuré : remplacer `NEVER` par `ALWAYS` dans `Revision.java` laisse les 25 au vert. Elle est surveillée par le programme de 8.3.3, qui ne fait pas partie du projet. Écrire ce test (un serveur de test qui redirige vers un autre) est un bon exercice de clôture.

#### 8.8.5 Quatrième défense, et ce que cela NE résout PAS

La quatrième est minimale : le délai maximal de chaque requête. Un serveur hostile peut répondre très lentement exprès pour garder ton programme occupé ; le délai maximal borne combien il peut en abuser.

Et maintenant l'honnêteté, parce que la sécurité que l'on cache devient fausse : **ces quatre défenses sont un bon début, pas un mur.** La liste autorisée compare des *noms*, pas des adresses réelles. `localhost` est un nom que ton système traduit en une adresse en consultant le fichier `/etc/hosts` ou un serveur de noms ; un serveur de noms hostile peut répondre une adresse différente de celle attendue (l'attaque s'appelle *DNS rebinding*). Pour un service qui sort sur internet avec un serveur de noms que tu ne contrôles pas, la défense complète consiste à résoudre l'adresse, vérifier qu'elle n'appartient pas à une plage interne ou spéciale, et se connecter à *cette* adresse et non au nom, en plus d'isoler le réseau du serveur pour que l'interne ne soit pas atteignable même si la validation échoue. C'est la dernière ligne de défense et elle relève de l'infrastructure, pas du code. L'[aide-mémoire d'OWASP sur SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) décrit les deux couches et pourquoi aucune ne suffit seule. Le `revisor` de ce cours travaille dans un environnement local que tu contrôles : sa liste autorise `127.0.0.1` et `localhost`, et sur ta machine `localhost` est traduit par ton fichier `/etc/hosts` vers la même adresse. Là, la liste de noms suffit ; hors d'un environnement de ce genre, accepter un nom comme `localhost` n'est pas une défense complète, pour ce qui vient d'être expliqué ; l'important de la leçon est l'habitude : **toute adresse écrite par une autre personne est suspecte jusqu'à ce qu'elle passe par une liste de ce qui est autorisé.** Il y a un article de Hábil sur la [sécurité des API : identité, passerelle et jetons](https://www.habil.mx/fr/blog/securite-api-cto-identite-passerelle-jetons/) qui suit le sujet depuis l'architecture.

### 8.9 L'étape du projet : le `revisor` interroge pour de vrai

Toutes les pièces sont là. Voici le projet tel qu'il se présente à la fin de la leçon :

```text
revisor/
  pom.xml                  versión 0.8.0 · agrega Jackson 3.2.3 y copy-dependencies
  servicios.json           NUEVO (reemplaza a servicios.txt)
  src/main/java/mx/habil/revisor/
    Revisor.java           lee servicios.json con la lista permitida
    Json.java              NUEVO: el mapeador de Jackson
    Configuracion.java     REESCRITA para JSON y con validación
    Revision.java          REESCRITA: HttpClient en lugar del simulador
    ReporteJson.java       NUEVO: la forma del JSON
    Formato.java           agrega json()
    Estado.java  Respuesta.java  Servicio.java  Resultado.java  Clasificador.java
    Resumen.java  Reporte.java  ErrorDeConfiguracion.java       (sin cambios)
                           (Simulador.java se borró)
  src/test/java/mx/habil/revisor/
    ConfiguracionTest.java  REESCRITA: 7 pruebas
    RevisionTest.java       NUEVA: 5 pruebas contra un servidor de verdad
    ClasificadorTest.java  FormatoTest.java  ResumenTest.java    (sin cambios)
```

Tu as déjà vu en entier `pom.xml`, `servicios.json`, `Json`, `Configuracion`, `Revision`, `ReporteJson`, `Formato`, `ConfiguracionTest` et `RevisionTest`. Le seul qui manque est le `Revisor`, qui change deux choses : il lit `servicios.json` et passe à `Configuracion.leer` la liste autorisée.

```java
// fig08_10/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.List;

/** El punto de entrada: lee servicios.json, revisa cada servicio e imprime el reporte. */
public final class Revisor {

  private Revisor() {}

  /** Arranca el revisor; los errores de configuración salen por la salida de error. */
  public static void main(String[] args) throws InterruptedException {
    try {
      List<Servicio> servicios =
          Configuracion.leer(Path.of("servicios.json"), Configuracion.SERVIDORES_PERMITIDOS);
      Reporte reporte = Reporte.de(Revision.revisarTodos(servicios));
      System.out.println(Formato.tabla(reporte));
    } catch (ErrorDeConfiguracion e) {
      System.err.println("error de configuración: " + e.getMessage());
    }
  }
}
```

Les onze fichiers qui n'ont pas changé par rapport à la Leçon 7 sont inclus pour que le projet puisse être monté sans regarder une autre leçon :

<details>
<summary>Les onze fichiers qui n'ont pas changé</summary>

```java
// fig08_10/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig08_10/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig08_10/src/main/java/mx/habil/revisor/Servicio.java

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
// fig08_10/src/main/java/mx/habil/revisor/Resultado.java

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
// fig08_10/src/main/java/mx/habil/revisor/Clasificador.java

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
// fig08_10/src/main/java/mx/habil/revisor/Resumen.java

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
// fig08_10/src/main/java/mx/habil/revisor/Reporte.java

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
// fig08_10/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/ClasificadorTest.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/FormatoTest.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/ResumenTest.java

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

</details>

Construis et vérifie le décompte. Les tests du projet n'ont pas besoin du laboratoire (`RevisionTest` lance son propre serveur), mais l'exécution d'après, si :

```text
$ mvn -B verify
...
[INFO] Recompiling the module because of changed dependency.
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.RevisionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.RevisionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.FormatoTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- dependency:3.11.0:copy-dependencies (copiar-dependencias) @ revisor ---
[INFO] BUILD SUCCESS
```

`Tests run: 25` : 18 de la Leçon 7, moins les 5 de `ConfiguracionTest` qui lisaient du texte, plus les 7 nouveaux qui lisent du JSON, plus les 5 de `RevisionTest` (18 − 5 + 7 + 5). Note-le dans ton journal de bord. Si tu n'obtiens pas 25, quelque chose a bougé, et c'est une question qui vaut la peine d'être résolue avant de continuer. Et l'exécution, avec le *classpath* complet :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

Le tableau de toujours, maintenant avec des temps mesurés, et le résumé avec une moyenne qui en dépend.

**Ce que ces 25 tests ne surveillent pas.** Un chiffre vert ne dit pas ce qu'il couvre. Pour le savoir, on a cassé, un par un, le code qui suit, et on a mesuré si un test passait au rouge. Des tests sont passés au rouge, et ces points sont donc surveillés : le rejet des schémas, des hôtes hors de la liste, de `timeout_ms` invalide et des noms répétés ; la traduction de `ConnectException` et de `HttpTimeoutException` ; et le délai maximal de la requête. **Les tests sont restés au vert**, c'est-à-dire sans surveillance : le message `'nombre' debe ser un texto que no esté vacío`, celui de `falta la lista 'servicios'`, le `connectTimeout` du client, la politique `Redirect.NEVER` et la conversion en `null` du temps dans `ReporteJson` (cette dernière est surveillée par le contrat de la Leçon 10). Ce n'est pas un défaut de la leçon, c'est la règle : chaque nouveau test que tu ajoutes ferme l'un de ces trous, et les exercices 3 et 4 commencent par là.


## L'erreur que tu vas voir

La leçon a une erreur principale, celle de la classe de Jackson qui n'apparaît pas, et plusieurs messages secondaires qu'il convient de savoir lire d'un coup d'œil.

**L'erreur principale.** Tu exécutes le `revisor` avec la commande des Leçons 6 et 7, qui n'inclut pas les JAR de Jackson :

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError` (le nom de la classe manquante apparaît avec des barres obliques, `tools/jackson/core/JacksonException`) n'est pas une erreur de compilation. Le programme a **compilé** correctement, parce que Maven connaissait bien la dépendance ; il échoue à l'exécution, parce que la JVM ne sait pas où la chercher. La cause qui suit (`Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException`) le confirme : cette classe n'est dans aucun des endroits du *classpath*. La solution est d'exécuter avec `-cp "target/classes:target/lib/*"` (et d'avoir exécuté avant `mvn -B verify`, qui est ce qui copie les JAR dans `target/lib`). Si quelqu'un te demande « ça compile mais ça ne tourne pas ? », la réponse est presque toujours le *classpath*. Et son proche parent, celui de 8.5.1, est de copier un exemple de Jackson 2 : `package com.fasterxml.jackson.databind does not exist`. Cela se corrige en remplaçant `com.fasterxml.jackson` par `tools.jackson` dans le `import` et en vérifiant les noms des méthodes.

**Les messages secondaires**, pour les reconnaître d'un coup d'œil :

- `java.net.BindException: Address already in use` au démarrage du laboratoire : un autre programme utilise le port 8089, ou tu as laissé un laboratoire précédent en marche. Ferme-le avec `Ctrl`+`C` dans son terminal.
- Tous les services avec `sin conexión` : le laboratoire n'est pas allumé. Allume-le dans un autre terminal et relance.
- `exception ConnectException has already been caught` : tu as mis un `catch` du type général avant le particulier. Du particulier au général.
- `ConnectException` avec message `null` (`fig08_03.java`) : normal. C'est pourquoi le `revisor` n'affiche jamais le message d'une exception réseau.
- `error de configuración: servicios.json no es JSON válido: renglón 4, columna 5` : il manque ou il y a en trop quelque chose près de ce point ; c'est presque toujours une virgule. Regarde la ligne précédant celle qui est indiquée.
- `error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida` : la défense en action. Ce n'est pas un défaut : c'est l'avertissement que quelqu'un a demandé une adresse que le `revisor` n'interroge pas.
- `invalid URI scheme file` (`fig08_09.java`) : le `HttpClient` rejette les schémas qui ne sont pas HTTP ; le `revisor` les rejette avant, dans la configuration.

## Ce qui se fait de travers

**Envoyer une requête sans délai maximal.** C'est l'erreur la plus coûteuse de la leçon parce qu'elle n'échoue pas dans les tests : elle fonctionne le jour de l'exemple et bloque le programme le jour où un service reste muet. Toute requête porte son `timeout`, et le client son `connectTimeout`.

**Créer un `HttpClient` par requête, ou ne pas le fermer.** Chaque client garde des connexions et des ressources ; un par requête jette à la poubelle ce que le client économise en réutilisant, et ne pas le fermer les laisse occupées. Un par vérification, avec `try-with-resources`.

**Afficher `e.getMessage()` d'une exception réseau.** Parfois c'est `null`, parfois c'est un texte interne de l'implémentation, et il change selon les versions et les systèmes. Traduis le **type** en un détail fixe et laisse le message de côté.

**Attraper `Exception` « au cas où » autour du réseau.** Cela avale aussi les erreurs de programmation (un `NullPointerException` de ta propre logique) et les déguise en « erreur réseau ». On attrape les types concrets : `HttpTimeoutException`, `ConnectException`, `IOException`, dans cet ordre.

**Faire confiance à un JSON parce que « c'est moi qui l'écris ».** Le fichier sera édité par quelqu'un d'autre, ou par toi, pressé. Mapper directement vers un *record* laisse passer `1.5` et `"1000"`, et un `-5` arrive jusqu'à une exception loin de l'endroit où il a été écrit. On valide à l'entrée, avec des messages qui disent quoi corriger.

**Valider une adresse avec des opérations sur le texte.** `startsWith("http://127.0.0.1")` laisse passer `http://127.0.0.1@servidor-del-atacante/`. On analyse avec `URI` et on compare `getHost()` à une liste autorisée.

**Une liste de ce qui est interdit au lieu d'une liste de ce qui est autorisé.** Les adresses dangereuses sont trop nombreuses et nouvelles chaque année ; celles dont tu as besoin sont peu nombreuses et tu les connais. Ce qui n'est pas dans la liste est rejeté.

**Suivre les redirections à l'aveugle.** Une adresse autorisée qui mène à une non autorisée annule la liste. `Redirect.NEVER`, écrit.

**Des tests qui dépendent d'un service qu'ils ne contrôlent pas.** Si `RevisionTest` interrogeait le laboratoire, il échouerait sur n'importe quel ordinateur où il ne serait pas allumé. Le test lance son propre serveur, au comportement connu, sur un port libre.

**Mesurer des durées avec l'heure de l'horloge.** `System.currentTimeMillis()` peut sauter lors d'un ajustement de l'horloge ; c'est `System.nanoTime()` qui sert à mesurer des intervalles.

## Exercices

### Exercice 1 — L'adresse qui ne doit pas passer, et ses cousines

Dans ton `servicios.json`, change le deuxième service vers chacune de ces adresses, **une à la fois**, et pour chacune **prédis d'abord** ce qu'imprime le `revisor` : (a) `http://169.254.169.254/latest/meta-data/` ; (b) `http://localhost:8089/pagos` ; (c) `http://[::1]:8089/pagos` (l'adresse de « cet ordinateur-ci » en notation IPv6) ; (d) `http://LOCALHOST:8089/pagos`. Ensuite exécute chacune et compare. Note dans le journal de bord celle que tu n'as pas devinée, et pourquoi la liste autorisée de 8.8.3 se comporte comme elle le fait avec elle.

### Exercice 2 — Un service qui ne répond jamais

Ajoute à ton `servicios.json` trois services du laboratoire : `facturacion` (`/facturacion`), `notificaciones` (`/notificaciones`, avec `"timeout_ms": 1000`) et `basura` (`/basura`). **Avant d'exécuter**, prédis l'état et le détail de chacun, et combien de services en ligne, lents et tombés le résumé annoncera. Ensuite exécute. Enfin, retire de `Revision.java` la ligne du `.timeout(...)`, exécute de nouveau et explique ce qui s'est passé avec `notificaciones`, combien de temps a pris le programme, et ce qui se serait passé si le service n'avait jamais répondu.

### Exercice 3 — Trois fichiers mal écrits

Avec le `servicios.json` d'origine, casse le fichier de trois façons, une à la fois, et **prédis le message** avant d'exécuter : (a) retire la virgule de la fin de la ligne du service `catalogo` ; (b) laisse le fichier complètement vide ; (c) remplace la valeur de `servicios` par `["catalogo"]`, une liste de textes au lieu d'objets. Pour (a), avant d'exécuter, note à quelle ligne et colonne tu t'attends à ce que cela échoue.

### Exercice 4 — Deux lignes que le mapping direct aurait laissé passer

Ajoute deux cas à la table `archivosInvalidos` de `ConfiguracionTest` : un service avec `"timeout_ms": -5` et un autre avec `"timeout_ms": true`. **Prédis** le message de chacun et combien de tests dira le rapport (indice : chaque `Arguments.of` est un test). Exécute `mvn -B verify`. A-t-il fallu changer quelque chose dans `Configuracion.java` pour qu'ils passent ?

## Solutions

### Solution 1

(a) L'adresse du service de métadonnées est rejetée avant d'envoyer quoi que ce soit :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

(b) `localhost` est dans la liste autorisée, donc `http://localhost:8089/pagos` passe, et le `revisor` interroge le laboratoire comme s'il s'agissait de `127.0.0.1`. Remarque ce qui a changé et ce qui n'a pas changé : le deuxième service s'appelle toujours `inventario` (tu n'as changé que son adresse), mais il interroge maintenant le chemin de `pagos`, qui répond vite, donc il sort ARRIBA et non LENTO :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     180ms  200
inventario  ARRIBA      92ms  200
pagos       ARRIBA      91ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 3 arriba · 0 lentos · 1 caído · promedio 121ms
```

C'est une petite leçon sur les noms : le nom d'un service est une étiquette qu'écrit une personne, et l'adresse est ce qui est réellement interrogé. Le `revisor` ne vérifie pas qu'ils coïncident.

(c) et (d) : **aucune des deux ne passe**, et c'est délibéré. `[::1]` est l'hôte `[::1]` pour `getHost()`, et il n'est pas dans la liste ; `LOCALHOST` en majuscules non plus, parce que la comparaison est exacte.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor LOCALHOST no está en la lista permitida
```

Si tu t'attendais à ce que (c) ou (d) passent, c'est que tu as pensé à ce qu'elles *signifient* (le même ordinateur) et non à ce qu'elles *disent* (un autre texte). Une liste autorisée compare des textes, et elle reste stricte exprès : accepter une autre forme revient à l'ajouter à la liste, à tête reposée, et non à relâcher la comparaison pour que « ça coïncide presque ».

### Solution 2

**Prédiction.** Les quatre de toujours restent inchangés : `catalogo` et `pagos` ARRIBA, `inventario` LENTO, `reportes` CAIDO avec `sin conexión`. Parmi les trois nouveaux, `facturacion` répond `503`, CAIDO avec le détail `503` ; `notificaciones` attend plus que son délai maximal d'une seconde, CAIDO avec `tiempo agotado` ; `basura` ferme la connexion sans répondre, CAIDO avec `error de red`. Résumé : 7 services, 2 en ligne, 1 lent, 4 tombés, et la moyenne n'inclut que ceux qui ont répondu (`catalogo`, `inventario`, `pagos` et `facturacion`, qui a répondu même si c'était avec une erreur) : environ 670 millisecondes, parce que les 2.3 secondes d'`inventario` pèsent lourd dans une moyenne sur quatre.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     179ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      93ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      100ms  503
notificacionesCAIDO          —  tiempo agotado
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 669ms
```

La moyenne est la somme des quatre qui ont répondu divisée par quatre. (Les noms longs débordent de la colonne, un défaut que le format de la leçon 2 traîne et que l'exercice facultatif de la Leçon 10 résout.) Maintenant, sans le `.timeout(...)` :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     174ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      91ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      102ms  503
notificacionesLENTO      10.0s  200
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 2 lentos · 3 caídos · promedio 2.5s
```

`notificaciones` n'est plus `tiempo agotado` : sans délai maximal, le client a attendu les 10 secondes que met le laboratoire, a reçu le `200` et l'a classé LENTO (`10.0s`). Le programme entier a pris environ 13 secondes (mesuré avec `time` : 12.9) et la moyenne a explosé. Mais le laboratoire est aimable : il répond au bout de dix secondes. **Un vrai service qui reste muet ne répond jamais**, et alors `send` ne revient pas, le programme se bloque et aucune autre vérification ne se termine. C'est pourquoi le délai maximal de la requête ne se retire jamais : c'est la seule chose qui borne le pire cas. Remets la ligne.

### Solution 3

(a) Sans la virgule, Jackson arrive au `{` d'`inventario` en attendant une virgule ou la fermeture du tableau, et échoue là ; le message tombe à la ligne 4 (celle d'`inventario`), colonne 5, qui est l'endroit où commence cette accolade, après l'indentation de quatre espaces. Celui qui s'est trompé a laissé le défaut à la ligne 3, mais l'analyseur ne s'en rend compte que lorsqu'il voit ce qui suit.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

(b) Un fichier vide est un JSON invalide, mais l'arbre que Jackson remet pour un texte vide n'est pas une erreur : c'est un nœud « absent », et le `revisor` le traite comme « il n'a pas la liste » :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

(c) Une liste de textes a la forme d'une liste, mais chaque élément devrait être un objet :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

### Solution 4

Les deux lignes s'ajoutent à la fin de la table : la dernière ligne existante se termine désormais par une virgule, et derrière viennent les deux nouvelles, avec le même format que les autres :

```java
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": -5}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": true}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"));
```

Les deux messages sont celui de toujours, `servicio 1: 'timeout_ms' debe ser un entero mayor que 0` : `-5` est un entier mais n'est pas supérieur à 0, et `true` n'est pas un entier (`isInt()` est faux). Sans toucher à `Configuracion.java`, parce que la validation couvrait déjà ces deux cas dès le départ. Avec le mapping direct, `-5` serait passé en silence (tu l'as vu en 8.6.2) et `true` aurait été rejeté, mais avec une erreur de Jackson et non avec ton message. Le décompte monte de 25 à 27 :

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, ... in mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Comment savoir que j'ai réussi

- [ ] `mvn -B verify`, depuis la racine de `~/curso-java/revisor/`, se termine par `BUILD SUCCESS` et dit `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`, et tu sais d'où sort chaque nombre (18 − 5 + 7 + 5).
- [ ] Avec le laboratoire allumé, `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` imprime le tableau des quatre services avec ESTADO et DETALLE identiques à ceux de cette leçon, et un résumé avec `2 arriba · 1 lento · 1 caído`.
- [ ] Sans le laboratoire allumé, les quatre services sortent CAIDO avec `sin conexión`.
- [ ] Un service pointé vers `http://169.254.169.254/latest/meta-data/` fait que le `revisor` imprime `el servidor 169.254.169.254 no está en la lista permitida` sans avoir envoyé aucune requête.
- [ ] Tu as retiré le `.timeout(...)` de `Revision.java` et tu as vu le test de `tiempo agotado` passer au rouge ; tu l'as rétabli.
- [ ] Un `servicios.json` avec une virgule en moins produit `no es JSON válido: renglón N, columna M`, et tu sais à quelle ligne regarder.
- [ ] Tu sais expliquer pourquoi `"timeout_ms": "1000"` entre comme 1000 avec le mapping direct et se rejette avec la validation à la main.
- [ ] Tu as noté dans ton journal de bord quel message de cette leçon t'a coûté le plus à lire.

## Résumé

Réponds sans regarder la leçon :

1. Quels deux délais maximaux y a-t-il dans une requête avec `HttpClient`, où se configure chacun et que lève chacun en expirant ?
2. Pourquoi un `HttpRequest` sans `timeout` est-il dangereux ?
3. À quel détail du `revisor` correspond chacune de ces exceptions : `HttpTimeoutException`, `ConnectException` et toute autre `IOException` ? Pourquoi les attrape-t-on dans cet ordre ?
4. Pourquoi un `503` n'est-il pas une exception ? Et un `302`, avec la politique de redirections du `revisor` ?
5. Pourquoi mesure-t-on avec `System.nanoTime()` et non avec `currentTimeMillis()` ?
6. Quelle différence y a-t-il entre `tools.jackson` et `com.fasterxml.jackson` ? Quelles annotations n'ont pas changé de paquet ?
7. Qu'est-ce que le *classpath* et pourquoi `target/lib` manque-t-il dans `java -cp target/classes` alors que le programme compile ?
8. Pourquoi la configuration est-elle lue comme un arbre et validée à la main au lieu d'être mappée directement vers un *record* ? Nomme deux valeurs que le mapping direct laisse passer.
9. Qu'est-ce que SSRF et quelles sont les quatre défenses du `revisor` ? Pourquoi `startsWith("http://127.0.0.1")` n'est-il pas une défense ?
10. Que ne résout pas la liste autorisée de noms ? Que faudrait-il de plus dans un service qui sort sur internet ?

## Pour aller plus loin

- [Java SE 25, paquet `java.net.http`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/package-summary.html) : le client HTTP du JDK au complet, avec les requêtes asynchrones, HTTP/2 et WebSocket que cette leçon n'utilise pas. En anglais, comme presque toute la documentation officielle. Consulté le 7 octobre 2026.
- [RFC 9110, « HTTP Semantics »](https://www.rfc-editor.org/rfc/rfc9110) : la norme des méthodes, des codes d'état et des redirections ; la section 15 est la liste des codes d'état. Consulté le 7 octobre 2026.
- [Guide de migration vers Jackson 3](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) et [le dépôt de `jackson-databind`](https://github.com/FasterXML/jackson-databind) : ce qui a changé par rapport à la version 2 et comment on utilise la bibliothèque. Consulté le 7 octobre 2026.
- [OWASP, « Server-Side Request Forgery Prevention Cheat Sheet »](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) : les défenses par couches contre SSRF et pourquoi une seule ne suffit pas. Consulté le 7 octobre 2026.

---

**Précédente :** [Leçon 7 — Tests avec JUnit](07-pruebas-junit.md) ·
**Suivante :** [Leçon 9 — Concurrence : tout vérifier en même temps](09-concurrencia.md)
