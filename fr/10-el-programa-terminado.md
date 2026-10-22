# Leçon 10 — Le programme terminé

**Durée :** 2 × 90 min (deux séances ; la coupure se situe entre les sections 10.3 et 10.4)

**Ce que tu construis :** le `revisor` complet, prêt à livrer : il se pilote depuis la ligne de commande, sort avec un code qu'un script peut lire, peut servir son rapport par HTTP, passe les portes de qualité et s'empaquette dans un seul JAR

**Ce que tu apprends :** arguments et codes de sortie comme contrat ; le serveur HTTP du JDK lui-même ; l'arrêt propre avec un hook d'arrêt ; Checkstyle et SpotBugs dans `mvn verify`, et comment lire ce qu'ils trouvent ; une revue de la sécurité du `revisor` ; et le JAR exécutable avec `maven-shade-plugin`

**D'où tu viens.** Tu arrives avec le projet de la [Leçon 9](09-concurrencia.md) : un `revisor` concurrent qui vérifie vingt services dans le temps du plus lent, avec 28 tests au vert. Il fonctionne, mais c'est encore un projet de programmeur et non un programme qu'une autre personne puisse utiliser. Pour l'exécuter, il faut écrire `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` et se trouver dans le bon dossier ; la limite de concurrence est une constante qu'on change en recompilant ; il n'y a aucun moyen de demander le rapport en JSON ; si tout échoue, le programme se termine comme si tout allait bien ; et personne ne lui a fait passer une revue de style ni une recherche automatique de défauts. Aujourd'hui, ces cinq choses se règlent. Tu travailles dans ton dossier `~/curso-java/revisor/`.

**Ce que cette leçon ne fait pas.** Elle n'utilise pas de bibliothèque pour la ligne de commande (il y en a de bonnes, comme *picocli*, mais le cours évite les dépendances qui ne sont pas nécessaires et sept options se lisent bien à la main). Elle ne met ni chiffrement ni authentification au serveur : il est pensé pour être interrogé par un autre processus de la même machine, et cela est expliqué en 10.5. Et elle ne transforme pas le JAR en installateur : `jpackage` reste « ce qui vient ».

**À propos des exécutions.** Les figures 10.1 à 10.3 sont des fichiers compacts et s'exécutent avec `java figNN_MM.java`. La figure 10.4 est le projet ; elle a besoin du laboratoire dans un autre terminal (`java Laboratorio.java`, port 8089 ; le 8088 reste vide exprès) et, quand elle lance son propre serveur, utilise le port 8090. Les sorties ont été obtenues avec Temurin 25.0.4, Maven 3.10.0 et les *plugins* fixés dans le `pom.xml`. Trois avertissements d'honnêteté : **les temps sont ceux de ma machine** (les tiens seront autres ; les proportions, non) ; **les messages de Checkstyle et de certaines parties de la JVM sortent dans la langue de ton système** (les miens sont sortis en espagnol ; avec `-Duser.language=en` ils sortent en anglais) ; et les commandes qui n'existent que sous Linux (comme `ss`) je les écris comme tu les utiliseras sur ta Mint, mais **je n'ai pas pu les exécuter sur la machine où j'ai mesuré**, donc leur sortie exacte, c'est toi qui la verras.

## À la fin, tu seras capable de

- Lire à la main les arguments de la ligne de commande, les valider, et renvoyer une erreur d'usage claire sans afficher de trace.
- Expliquer ce qu'est un code de sortie, en fixer un pour chaque situation du `revisor` (`0`, `1`, `2`) et le lire dans le terminal avec `$?`.
- Lancer un serveur HTTP avec les classes du JDK, en écoutant seulement sur `127.0.0.1`, et l'arrêter proprement quand quelqu'un l'interrompt.
- Exécuter Checkstyle et SpotBugs dans `mvn verify`, lire leurs messages, et expliquer pourquoi une analyse silencieuse ne prouve pas qu'il n'y a pas de défauts.
- Passer le `revisor` en revue face à une liste de risques concrets (adresses, redirections, sérialisation, interface réseau) et dire ce que couvre chaque décision.
- Empaqueter le programme dans un seul JAR et l'exécuter avec `java -jar` depuis n'importe quel dossier.
- Vérifier, une par une, les six conditions avec lesquelles le plan dit que le cours est terminé.

## Le pourquoi avant le comment

Quand un programme est-il terminé ? Pas quand il tourne sur ton ordinateur : cela, tu l'as obtenu dès la leçon 5, et jusqu'à la 9 chaque leçon a été vérifiée ainsi. Un programme est terminé quand **une autre personne, un autre programme ou toi-même dans six mois peut l'utiliser sans lire le code**. Cela exige des choses qui n'ont rien à voir avec la logique du `revisor`.

Cela exige une **interface** : une façon de lui dire quoi faire sans rien éditer. Cela exige une **réponse honnête à celui qui l'appelle** : un script qui exécute le `revisor` toutes les cinq minutes a besoin de savoir si tout s'est bien passé sans avoir à lire le tableau, et c'est pour cela que le code de sortie existe. Cela exige une **qualité qui se vérifie toute seule**, parce que « je l'ai relu » ne passe pas à l'échelle et qu'un style qui ne dépend que de la discipline des personnes s'érode. Cela exige que ce qui **écoute sur le réseau** le fasse avec soin. Et cela exige une **livraison** simple : un fichier, une commande.

Cette leçon prend ces cinq choses et les règle. La **première** séance (10.1 à 10.3) est l'interface : arguments, codes de sortie et le serveur avec son arrêt propre. La **seconde** (10.4 à 10.6) est la livraison : les portes de qualité, la revue de sécurité et le JAR. Et l'exercice 1 est la liste que le plan du cours appelle « Comment savoir que tu as terminé » : six vérifications mesurables, que tu fais toi-même, avec l'horloge et les commandes, et qui disent si le `revisor` est vraiment terminé.

## Les concepts

### 10.1 Arguments et code de sortie

#### 10.1.1 Les arguments de la ligne de commande

Quand tu écris `java -jar revisor.jar --config otro.json`, tout ce qui vient après le nom du programme arrive à `main` sous forme d'un tableau de textes : le `String[] args` que tu connais depuis la leçon 6. Java **ne les interprète pas** : il ne sait pas ce qu'est `--config`, ni que `otro.json` en est la valeur, ni que `--concurrencia` demande un nombre. Il les remet seulement dans l'ordre où tu les as écrits. La figure 10.1 les imprime, et montre aussi comment on sort d'un programme avec un code :

```java
// fig10_01.java
void main(String[] args) {
  IO.println("recibí " + args.length + " argumentos: " + Arrays.toString(args));
  if (args.length == 1 && args[0].equals("--falla")) {
    System.err.println("error de uso: pediste fallar");
    System.exit(2);
  }
  IO.println("termino bien");
}
```

Sans arguments, le programme les compte et se termine normalement :

```text
$ java fig10_01.java
recibí 0 argumentos: []
termino bien
```

Si tu lui passes des arguments, ils arrivent tels quels, comme des textes isolés et dans l'ordre. Voici une exécution avec ceux d'un `revisor` typique (le lanceur d'un seul fichier, `java Archivo.java`, remet à `main` tout ce qui vient après le nom du fichier) :

```text
$ java fig10_01.java --config servicios.json --concurrencia 3
recibí 4 argumentos: [--config, servicios.json, --concurrencia, 3]
termino bien
```

Remarque que le `3` est le texte `"3"`, pas un nombre : le convertir, et décider quoi faire si quelqu'un écrit `tres`, c'est ton travail. C'est ce que fera la classe `Opciones` en 10.1.3.

#### 10.1.2 Le code de sortie : un contrat avec celui qui t'appelle

Tout programme, quand il se termine, remet au système d'exploitation un nombre entre 0 et 255 : son **code de sortie** (*exit status*). C'est la réponse la plus courte possible à la question « comment ça s'est passé ? », et c'est ce que lisent les scripts, les planificateurs de tâches et les outils d'intégration continue. La convention universelle dans les systèmes de type Unix est : **`0` veut dire succès, tout autre nombre veut dire que quelque chose a mal tourné**. Dans un terminal avec `bash`, le code de la dernière commande se trouve dans la variable `$?`, et le [manuel de `bash`](https://www.gnu.org/software/bash/manual/html_node/Exit-Status.html) documente cette convention, qui est celle que regardent `if`, `&&` et `set -e`.

La dernière partie de `fig10_01.java` en fait déjà usage : avec le seul argument `--falla`, le programme écrit une erreur sur la sortie d'erreur et se termine avec `System.exit(2)` :

```text
$ java fig10_01.java --falla
recibí 1 argumentos: [--falla]
error de uso: pediste fallar
$ echo $?
2
```

Deux détails qui semblent mineurs et ne le sont pas. Le premier : l'erreur a été écrite avec `System.err`, pas avec `IO.println`. Un programme a **deux sorties** distinctes : la **sortie standard** (`System.out`, où va le résultat) et la **sortie d'erreur** (`System.err`, où vont les avis destinés à une personne). Si quelqu'un redirige le résultat vers un fichier —`revisor --formato json > reporte.json`— il veut le JSON dans le fichier et les erreurs à l'écran, sans mélange. Le second : `System.exit` **termine toute la JVM et ne revient pas** : la ligne qui suit l'appel ne s'exécute jamais, ni le reste du programme, dans aucun thread. Ce n'est pas une coupure brutale : il déclenche l'arrêt ordonné de la JVM, qui exécute les hooks d'arrêt (10.3) et attend qu'ils se terminent avant de sortir. Mais du point de vue de ton code, tout ce qui venait après n'a plus lieu. C'est pourquoi on l'appelle à un seul endroit, le `main`, et on lui passe un nombre qu'un autre code a calculé.

Le `revisor` fixe ainsi ses codes, et c'est un **contrat** : ce que d'autres programmes peuvent supposer et qui, pour cela, ne change pas sans prévenir.

| Code | Quand | Ce qui s'écrit |
|---|---|---|
| `0` | tous les services sont `ARRIBA` (ou `--ayuda` ou `--version` a été demandé) | le rapport, sur la sortie standard |
| `1` | au moins un service est `LENTO` ou `CAIDO` | le rapport, sur la sortie standard |
| `2` | les options ne sont pas comprises, ou la configuration ne convient pas | un message sur la sortie d'erreur |

La différence entre `1` et `2` est celle dont un script a besoin : `1` veut dire « le programme a fonctionné et a trouvé un problème que tu dois traiter », et `2` veut dire « le programme n'a pas pu faire son travail ; le problème vient de celui qui l'exécute ». Une supervision peut alerter différemment pour chacun.

Il manque une pièce pour pouvoir **tester** ce contrat. Si tout le flux du programme vit dans `main` et que `main` se termine avec `System.exit`, un test qui appellerait `main` emporterait avec lui la JVM des tests. La solution est celle de toujours, séparer ce qui décide de ce qui agit : `main` est une ligne qui ne fait qu'appeler `System.exit(ejecutar(args, System.out, System.err))`, et tout le reste vit dans `ejecutar`, une méthode qui **reçoit** les deux sorties en paramètres et **renvoie** le code. Un test lui passe deux flux en mémoire, lit ce qu'elle a écrit, et vérifie le nombre qu'elle a renvoyé, sans rien terminer. C'est la même idée que la leçon 9 : communique en renvoyant des valeurs.

#### 10.1.3 `Opciones` : lire les arguments à la main

Voici la classe qui convertit le tableau de textes en une valeur déjà validée. Avant de la lire, un résumé des sept options qu'elle reconnaît : `--config RUTA` (par défaut `servicios.json`), `--formato tabla|json` (par défaut `tabla`), `--concurrencia N` (par défaut 10, au moins 1), `--permitir h1,h2` (par défaut `127.0.0.1,localhost`), `--servir PUERTO`, `--version` et `--ayuda`. Toute autre chose est une erreur d'usage. L'erreur est modélisée avec sa propre exception contrôlée, comme `ErrorDeConfiguracion` dans la leçon 5 :

```java
// fig10_04/src/main/java/mx/habil/revisor/ErrorDeUso.java

package mx.habil.revisor;

/** Los argumentos de la línea de comandos no se entienden; el mensaje dice cuál falló. */
final class ErrorDeUso extends Exception {

  private static final long serialVersionUID = 1L;

  ErrorDeUso(String mensaje) {
    super(mensaje);
  }
}
```

```java
// fig10_04/src/main/java/mx/habil/revisor/Opciones.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/** Lo que la persona pidió en la línea de comandos, ya validado. */
record Opciones(
    Path config,
    String formato,
    int concurrencia,
    Set<String> permitidos,
    OptionalInt puerto,
    boolean ayuda,
    boolean version) {

  Opciones {
    permitidos = Set.copyOf(permitidos);
  }

  static Opciones leer(String[] args) throws ErrorDeUso {
    Path config = Path.of("servicios.json");
    String formato = "tabla";
    int concurrencia = Revision.LIMITE_POR_OMISION;
    Set<String> permitidos = Configuracion.SERVIDORES_PERMITIDOS;
    OptionalInt puerto = OptionalInt.empty();
    boolean ayuda = false;
    boolean version = false;

    Deque<String> pendientes = new ArrayDeque<>(List.of(args));
    while (!pendientes.isEmpty()) {
      String bandera = pendientes.removeFirst();
      switch (bandera) {
        case "--ayuda" -> ayuda = true;
        case "--version" -> version = true;
        case "--config" -> config = Path.of(valor(pendientes, bandera));
        case "--formato" -> formato = formato(valor(pendientes, bandera));
        case "--concurrencia" ->
            concurrencia = entero(valor(pendientes, bandera), bandera, 1, Integer.MAX_VALUE);
        case "--permitir" -> permitidos = servidores(valor(pendientes, bandera));
        case "--servir" ->
            puerto = OptionalInt.of(entero(valor(pendientes, bandera), bandera, 1, 65535));
        default -> throw new ErrorDeUso("opción desconocida: " + bandera);
      }
    }
    return new Opciones(config, formato, concurrencia, permitidos, puerto, ayuda, version);
  }

  private static String valor(Deque<String> pendientes, String bandera) throws ErrorDeUso {
    if (pendientes.isEmpty()) {
      throw new ErrorDeUso("falta el valor de " + bandera);
    }
    return pendientes.removeFirst();
  }

  private static String formato(String texto) throws ErrorDeUso {
    if (!texto.equals("tabla") && !texto.equals("json")) {
      throw new ErrorDeUso("--formato debe ser tabla o json, no " + texto);
    }
    return texto;
  }

  private static int entero(String texto, String bandera, int minimo, int maximo)
      throws ErrorDeUso {
    try {
      int numero = Integer.parseInt(texto);
      if (numero >= minimo && numero <= maximo) {
        return numero;
      }
    } catch (NumberFormatException e) {
      // cae al error de abajo: un texto que no es número y un número fuera de rango dicen lo mismo
    }
    String rango =
        maximo == Integer.MAX_VALUE
            ? "mayor o igual que " + minimo
            : "entre " + minimo + " y " + maximo;
    throw new ErrorDeUso(bandera + " debe ser un entero " + rango + ", no " + texto);
  }

  private static Set<String> servidores(String texto) throws ErrorDeUso {
    Set<String> servidores =
        Arrays.stream(texto.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    if (servidores.isEmpty()) {
      throw new ErrorDeUso("--permitir necesita al menos un servidor");
    }
    return servidores;
  }
}
```

Quatre idées de la classe méritent un paragraphe.

**Les arguments se consomment depuis une file.** Au lieu de parcourir le tableau avec un indice et de sauter des positions, on les copie dans une `ArrayDeque` et on sort le premier avec `removeFirst()`. Quand une option a besoin de sa valeur, elle appelle `valor(pendientes, bandera)`, qui sort la suivante ou lève `ErrorDeUso("falta el valor de --config")` (« il manque la valeur de --config ») s'il n'y en a plus. Ainsi le code n'a pas d'arithmétique d'indices susceptible de se tromper.

**Le `switch` décide avec une flèche.** C'est la forme que tu connais de la leçon 2, mais utilisée comme instruction (chaque cas affecte une variable) et non comme expression. Avec des flèches il n'y a pas de *fall-through* : chaque cas se termine de lui-même. Et le `default` lève l'erreur, de sorte qu'une option mal écrite ne passe pas en silence.

**Un nombre hors plage et un texte qui n'est pas un nombre disent la même chose.** `entero` essaie de convertir avec `Integer.parseInt` ; s'il échoue, ou si la valeur est hors plage, le message est unique (« debe ser un entero mayor o igual que 1, no tres », « doit être un entier supérieur ou égal à 1, et non tres »). Pour la personne qui a mal écrit l'option, distinguer les deux causes ne sert à rien.

**Le *record* se protège.** Son constructeur compact fait `permitidos = Set.copyOf(permitidos)` : la même copie défensive que la leçon 4, et pour la même raison, que la collection que garde le *record* ne soit pas une collection qu'un autre puisse changer ensuite. Et une note sur `OptionalInt puerto` : la leçon 4 a dit qu'un `Optional` s'utilise comme valeur de retour et non comme champ. Ici il y a une exception consciente : `Opciones` est une valeur en lecture seule qui **décrit ce qui a été demandé**, et « aucun serveur demandé » n'a pas de nombre qui le représente (pour le serveur, le 0 signifie bien quelque chose : « choisis un port libre »). L'`OptionalInt` dit exactement cela.

Avec les options déjà résolues, `ejecutar` les utilise ; tu le vois en entier en 10.3.3. Il faut d'abord parler de l'autre façon dont le `revisor` livre son rapport.

### 10.2 Le serveur HTTP du JDK

#### 10.2.1 Pourquoi un serveur

Jusqu'à maintenant, pour voir le rapport il faut exécuter le programme, qui vérifie et se termine. Cela convient à une personne dans un terminal ; cela ne convient pas à un autre outil qui voudrait consulter l'état chaque minute, ou à un tableau de bord qui afficherait le résultat. Pour cela, le `revisor` peut rester **à l'écoute** : avec `--servir 8090`, il ouvre un serveur HTTP et, chaque fois que quelqu'un lui demande `/reporte`, il fait une vérification complète et répond avec le JSON de la leçon 8.

Ce qui est bien, c'est qu'aucune bibliothèque n'est nécessaire pour y parvenir. Le JDK apporte un serveur HTTP simple, le paquet [`com.sun.net.httpserver`](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html), partie du module `jdk.httpserver`. C'est une API documentée et prise en charge (le nom `com.sun` est historique), et c'est **la même classe qu'utilise `Laboratorio.java`**, le programme que tu fais tourner depuis la leçon 8 : si tu l'ouvres, tu y trouveras `HttpServer.create`, une liste de `createContext` et un exécuteur de threads virtuels. Aujourd'hui, tu écris toi-même ce que tu ne faisais qu'utiliser. (Pour servir des fichiers statiques, le JDK apporte en plus un outil en ligne de commande, `jwebserver`, décrit dans [JEP 408](https://openjdk.org/jeps/408) ; nous n'en avons pas besoin.)

#### 10.2.2 Un serveur minimal, de bout en bout

La figure 10.2 est le plus petit serveur qui vaille la peine d'être lu. Elle lance un serveur, lui enregistre un chemin qui répond un JSON d'une ligne, se fait une requête à elle-même avec le `HttpClient` de la leçon 8, imprime ce qu'elle a reçu et s'arrête. Comme c'est un programme qui démarre et se termine tout seul, il peut être vérifié. (Une note de lecture : les quatre `import` du début sont nécessaires parce qu'un fichier compact n'importe de lui-même que `java.base`, et ni le serveur ni le client HTTP n'y vivent ; si tu les retires, le compilateur te dit `cannot find symbol` pour `HttpClient` et les autres.)

```java
// fig10_02.java
import com.sun.net.httpserver.HttpServer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {
  var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  servidor.createContext(
      "/hola",
      intercambio -> {
        byte[] cuerpo = "{\"hola\": \"mundo\"}".getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json");
        intercambio.sendResponseHeaders(200, cuerpo.length);
        try (var salida = intercambio.getResponseBody()) {
          salida.write(cuerpo);
        }
      });
  servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
  servidor.start();

  InetSocketAddress direccion = servidor.getAddress();
  IO.println("dirección: " + direccion.getAddress().getHostAddress());
  IO.println("solo la interfaz local: " + direccion.getAddress().isLoopbackAddress());

  URI url = URI.create("http://127.0.0.1:" + direccion.getPort() + "/hola");
  try (var cliente = HttpClient.newHttpClient()) {
    var respuesta =
        cliente.send(HttpRequest.newBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
    IO.println("código " + respuesta.statusCode() + ", cuerpo " + respuesta.body());
  }
  servidor.stop(0);
  IO.println("servidor detenido");
}
```

```text
$ java fig10_02.java
dirección: 127.0.0.1
solo la interfaz local: true
código 200, cuerpo {"hola": "mundo"}
servidor detenido
```

Les cinq étapes, une par une. **`HttpServer.create(direccion, 0)`** crée le serveur sans le démarrer ; le deuxième argument est la taille de la file des connexions en attente, et 0 signifie « celle que décide le système ». **`createContext("/hola", manejador)`** enregistre quoi faire des requêtes dont le chemin commence par `/hola`. **`setExecutor(...)`** décide qui traite les requêtes ; **`start()`** le met à l'écoute ; **`stop(0)`** l'arrête (le nombre indique les secondes pendant lesquelles il attend les requêtes en vol ; on y revient en 10.3).

Trois décisions de cette figure qui se répètent dans le serveur du `revisor` :

**L'adresse est `127.0.0.1`, pas « toutes ».** `new InetSocketAddress("127.0.0.1", puerto)` fait que le serveur écoute **seulement sur l'interface locale** : seuls les programmes de cette même machine peuvent lui parler. Si au lieu de cela tu écris `new InetSocketAddress(puerto)`, sans adresse, Java utilise l'adresse générique (*wildcard*) et écoute sur **toutes les interfaces**, réseau compris : n'importe quel équipement qui atteint ta machine pourrait demander le rapport. Pour un outil interne qu'interroge un autre processus local, la première est la bonne ; ouvrir la seconde est une décision qui se prend exprès, avec authentification et chiffrement, pas par défaut. La figure le vérifie en imprimant `isLoopbackAddress()`.

**Le port 0 signifie « choisis-en un libre ».** Le système en attribue un quelconque qui ne soit pas occupé, et `servidor.getAddress().getPort()` dit lequel c'était. C'est ce qu'utilisent les tests de la leçon 8 et ceux d'aujourd'hui, pour ne pas entrer en conflit avec ce qui tourne.

**Un thread virtuel par requête.** `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` est la leçon 9 appliquée. Sans exécuteur, le serveur traite **une requête à la fois** dans un seul thread ; je l'ai mesuré : six requêtes simultanées de 100 ms sont arrivées au gestionnaire une par une (maximum simultané : 1). Avec un thread virtuel par requête, une requête lente ne gêne pas les autres.

#### 10.2.3 `Servidor` : le chemin `/reporte`

La classe qu'utilise le `revisor` reçoit le port et **une fonction qui produit un `Reporte`** (un `Callable<Reporte>`). Qu'elle reçoive la fonction et non le travail déjà fait a une raison : la vérification doit avoir lieu **chaque fois que quelqu'un demande le rapport**, pas une seule fois au démarrage. Et elle en a une autre, que tu verras dans les tests : on peut lui donner un rapport fixe et vérifier ce qu'elle répond, sans toucher à aucun réseau.

```java
// fig10_04/src/main/java/mx/habil/revisor/Servidor.java

package mx.habil.revisor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** El servidor del propio JDK que entrega el reporte en {@code /reporte}. */
final class Servidor {

  private Servidor() {}

  static HttpServer iniciar(int puerto, Callable<Reporte> revisar) throws IOException {
    HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
    servidor.createContext("/", intercambio -> atender(intercambio, revisar));
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    return servidor;
  }

  private static void atender(HttpExchange intercambio, Callable<Reporte> revisar)
      throws IOException {
    try (intercambio) {
      if (!"/reporte".equals(intercambio.getRequestURI().getPath())) {
        responder(intercambio, 404, "text/plain; charset=utf-8", "no existe");
      } else if (!"GET".equals(intercambio.getRequestMethod())) {
        responder(intercambio, 405, "text/plain; charset=utf-8", "solo GET");
      } else {
        try {
          responder(intercambio, 200, "application/json", Formato.json(revisar.call()));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          responder(intercambio, 500, "text/plain; charset=utf-8", "error interno");
        } catch (Exception e) {
          responder(intercambio, 500, "text/plain; charset=utf-8", "error interno");
        }
      }
    }
  }

  private static void responder(HttpExchange intercambio, int codigo, String tipo, String texto)
      throws IOException {
    byte[] cuerpo = texto.getBytes(StandardCharsets.UTF_8);
    intercambio.getResponseHeaders().set("Content-Type", tipo);
    intercambio.sendResponseHeaders(codigo, cuerpo.length);
    intercambio.getResponseBody().write(cuerpo);
  }
}
```

Ce que répond chaque situation, pour que tu n'aies pas à le deviner dans le code : un `GET /reporte` répond `200` avec le JSON et `Content-Type: application/json` ; tout autre chemin, `404` ; toute autre méthode (un `POST`, par exemple), `405` ; et si la vérification échoue à cause d'une erreur du programme, `500` avec le texte générique `error interno` (« erreur interne »). Ce dernier point est délibéré : le message d'une exception peut contenir des chemins, des noms de classes ou des données qui ne doivent pas sortir par le réseau, donc à celui qui demande on dit que quelque chose a échoué et rien de plus. (À celui qui exploite le programme, on le dirait par un journal, que ce cours ne construit pas.) Le `try (intercambio)` ferme la conversation avec le client quoi qu'il arrive, et le corps s'écrit sous forme d'octets en UTF-8, parce que le JSON porte des accents (« sin conexión »).

Un avertissement de conception qu'il convient de formuler. Chaque `GET /reporte` fait **une vérification complète** : il interroge tous les services et met le temps que met le plus lent (environ 2.5 secondes avec les quatre canoniques). Ce n'est pas un cache. Si dix personnes demandent le rapport en même temps, le `revisor` fait dix vérifications en même temps. C'est une conséquence que la section 10.5 reprend, car elle a un côté sécurité.

### 10.3 L'arrêt propre et le `revisor` complet

#### 10.3.1 Le problème : un serveur qui ne sait pas s'arrêter

Un serveur est un programme qui ne se termine pas de lui-même ; il se termine quand quelqu'un l'interrompt : toi avec `Ctrl`+`C`, le système en s'éteignant, ou un outil qui lui envoie le signal de terminaison. Si à cet instant il y a une requête à moitié traitée, l'interrompre brutalement la coupe. La solution est l'**arrêt propre** (*graceful shutdown*) : cesser d'accepter de nouvelles requêtes, laisser quelques secondes à celles qui sont déjà en route, et seulement alors sortir.

Java offre un mécanisme pour cela : le **hook d'arrêt** (*shutdown hook*). C'est un thread que tu enregistres avec `Runtime.getRuntime().addShutdownHook(hilo)` et que la JVM démarre quand elle commence à s'éteindre, pour n'importe quelle raison : parce que le dernier thread normal s'est terminé, parce que quelqu'un a appelé `System.exit`, ou parce que le système lui a envoyé un signal de terminaison comme celui de `Ctrl`+`C` (la [documentation de `Runtime`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html) énumère les cas). Il ne s'exécute pas si le processus meurt brutalement, comme avec `kill -9` ou une coupure de courant ; là, aucun arrêt propre n'est possible. La figure 10.3 le montre dans sa plus petite forme :

```java
// fig10_03.java
void main() throws InterruptedException {
  Runtime.getRuntime()
      .addShutdownHook(new Thread(() -> IO.println("gancho: cerrando con orden")));
  IO.println("trabajando...");
  Thread.sleep(300);
  IO.println("main termina");
}
```

```text
$ java fig10_03.java
trabajando...
main termina
gancho: cerrando con orden
```

Le programme se termine de son propre chef (`main termina`, « main se termine »), et **ensuite** le hook s'exécute. Avec `Ctrl`+`C`, il se passerait la même chose. Deux règles pour écrire un bon hook : qu'il soit **court** et ne reste pas bloqué (la JVM attend que tous les hooks se terminent, donc un hook qui ne se termine jamais laisse le programme sans pouvoir sortir ; s'il y a une limite, elle vient de l'extérieur, par exemple du système d'exploitation qui, en s'éteignant, termine le processus brutalement), et qu'il **n'appelle jamais `System.exit`** (l'arrêt est déjà en cours, et l'appeler de l'intérieur le fait s'attendre lui-même pour toujours).

#### 10.3.2 Comment le `revisor` s'en sert

En mode serveur, le `revisor` fait trois choses, dans cet ordre : il démarre le serveur ; il enregistre un hook qui appelle `servidor.stop(1)` (il cesse d'accepter, attend jusqu'à 1 seconde que les requêtes en cours se terminent, et ferme) et écrit `servidor detenido` (« serveur arrêté ») ; et il **attend**. Cette attente est la partie trompeuse. Si `ejecutar` renvoyait un nombre et que `main` appelait `System.exit`, le serveur mourrait un instant après sa naissance. C'est pourquoi le thread principal reste à attendre avec `new CountDownLatch(1).await()` : un verrou (*latch*) que personne n'ouvrira, de sorte que `await()` **ne revient jamais** et que le programme vit jusqu'à ce que la JVM s'éteigne de l'extérieur, ce qui permet au hook de faire son travail.

Tu vas l'essayer à la main en 10.3.5, dès que tu auras le `Revisor` complet de la section suivante et ses tests au vert.

#### 10.3.3 Le `revisor` complet

Maintenant on peut voir `Revisor.java` en entier. Lis-le comme une recette en six étapes, qui sont celles de `ejecutar` : **1** lire les options (si elles échouent : message d'usage et code 2) ; **2** si `--ayuda` ou `--version` a été demandé, répondre et sortir avec 0 ; **3** lire la configuration, validée avec la liste autorisée choisie (si elle échoue : message et code 2) ; **4** si `--servir` a été demandé, confier le travail à `servir` et ne pas revenir ; **5** sinon, tout vérifier avec la limite de concurrence demandée ; **6** écrire le tableau ou le JSON et renvoyer 0 si tous les services sont en marche, et 1 sinon.

```java
// fig10_04/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/** El revisor: consulta los servicios de la configuración y entrega el reporte. */
public final class Revisor {

  static final int CODIGO_TODO_ARRIBA = 0;
  static final int CODIGO_HAY_PROBLEMAS = 1;
  static final int CODIGO_USO_INVALIDO = 2;

  private static final String AYUDA =
      """
      uso: revisor [opciones]

        --config RUTA      archivo de servicios (por omisión servicios.json)
        --formato F        tabla o json (por omisión tabla)
        --concurrencia N   revisiones a la vez, al menos 1 (por omisión {limite})
        --permitir H1,H2   servidores permitidos (por omisión 127.0.0.1,localhost)
        --servir PUERTO    en vez de imprimir, sirve /reporte en 127.0.0.1:PUERTO
        --version          imprime la versión
        --ayuda            imprime esta ayuda

      códigos de salida: 0 todo arriba · 1 hay un servicio lento o caído · 2 uso o configuración
      inválidos
      """;

  private Revisor() {}

  /**
   * Punto de entrada: corre el revisor y sale con su código.
   *
   * @param args las opciones de la línea de comandos; {@code --ayuda} las lista
   */
  public static void main(String[] args) {
    System.exit(ejecutar(args, System.out, System.err));
  }

  static int ejecutar(String[] args, PrintStream salida, PrintStream errores) {
    Opciones opciones;
    try {
      opciones = Opciones.leer(args);
    } catch (ErrorDeUso e) {
      errores.println("error de uso: " + e.getMessage());
      errores.println("prueba: revisor --ayuda");
      return CODIGO_USO_INVALIDO;
    }
    if (opciones.ayuda()) {
      salida.print(ayuda());
      return CODIGO_TODO_ARRIBA;
    }
    if (opciones.version()) {
      salida.println("revisor " + version());
      return CODIGO_TODO_ARRIBA;
    }
    List<Servicio> servicios;
    try {
      servicios = Configuracion.leer(opciones.config(), opciones.permitidos());
    } catch (ErrorDeConfiguracion e) {
      errores.println("error de configuración: " + e.getMessage());
      return CODIGO_USO_INVALIDO;
    }
    try {
      if (opciones.puerto().isPresent()) {
        return servir(opciones.puerto().getAsInt(), servicios, opciones, salida);
      }
      Reporte reporte = revisar(servicios, opciones);
      boolean comoJson = opciones.formato().equals("json");
      salida.println(comoJson ? Formato.json(reporte) : Formato.tabla(reporte));
      boolean todoArriba = reporte.resumen().arriba() == reporte.resumen().total();
      return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;
    } catch (IOException e) {
      errores.println("no se pudo abrir el puerto: " + e.getMessage());
      return CODIGO_USO_INVALIDO;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      errores.println("revisión interrumpida");
      return CODIGO_HAY_PROBLEMAS;
    }
  }

  private static Reporte revisar(List<Servicio> servicios, Opciones opciones)
      throws InterruptedException {
    return Reporte.de(Revision.revisarTodos(servicios, opciones.concurrencia()));
  }

  private static int servir(
      int puerto, List<Servicio> servicios, Opciones opciones, PrintStream salida)
      throws IOException, InterruptedException {
    HttpServer servidor = Servidor.iniciar(puerto, () -> revisar(servicios, opciones));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  servidor.stop(1);
                  salida.println("servidor detenido");
                }));
    salida.println("sirviendo http://127.0.0.1:" + puerto + "/reporte (Ctrl+C para detener)");
    new CountDownLatch(1).await();
    return CODIGO_TODO_ARRIBA;
  }

  static String ayuda() {
    return AYUDA.replace("{limite}", String.valueOf(Revision.LIMITE_POR_OMISION));
  }

  static String version() {
    Properties propiedades = new Properties();
    try (InputStream flujo = Revisor.class.getResourceAsStream("/revisor.properties")) {
      if (flujo != null) {
        propiedades.load(flujo);
      }
    } catch (IOException e) {
      return "desconocida";
    }
    return propiedades.getProperty("version", "desconocida");
  }
}
```

Quelques choses qui ne sont pas visibles. La version qu'imprime `--version` **n'est pas écrite dans le code** : elle sort du fichier `revisor.properties`, que Maven remplit avec la version du `pom.xml`, de sorte que le nombre ne se répète pas en deux endroits qui se désynchroniseraient (tu le construis juste en dessous). Si le port est occupé, `HttpServer.create` lève une `IOException` et `ejecutar` l'attrape et dit `no se pudo abrir el puerto: Address already in use` (« impossible d'ouvrir le port »), avec le code 2 : le problème vient de celui qui l'exécute. Et le `catch (InterruptedException)` rétablit l'indicateur d'interruption, comme l'a enseigné la leçon 5, avant de sortir avec 1.

Pour que cela fonctionne, il faut trois changements en dehors du code, et tu les fais maintenant, avant les tests, parce que l'un d'eux vérifie justement la version. Le premier est le fichier de propriétés, dans un nouveau dossier, `src/main/resources/`, celui des fichiers qui accompagnent le programme sans être du code (Maven les copie dans `target/classes`). Il tient en une seule ligne :

```properties fig10_04/src/main/resources/revisor.properties
version=${project.version}
```

Les deux autres vont dans ton `pom.xml`. La version passe de `0.9.0` à **`1.0.0`**, celle de la livraison. Et dans `<build>`, avant `<plugins>`, on ajoute le bloc qui demande à Maven de **filtrer** ce dossier, c'est-à-dire de remplacer `${project.version}` par la version du `pom.xml` lors de la copie du fichier :

```xml
  <version>1.0.0</version>
  ...
  <build>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
      </resource>
    </resources>

    <plugins>
      ...
```

C'est un fragment : le reste du `pom.xml` reste comme tu l'as de la leçon 9, avec `maven-dependency-plugin` inclus, que tu utilises encore pour exécuter le programme. Le `pom.xml` complet de la livraison apparaît en 10.4.2, quand arriveront les portes de qualité et le JAR.

Le fichier de configuration avec les sept services (les quatre canoniques et les trois supplémentaires du laboratoire) se place dans le dossier du projet, et tu t'en serviras plus bas :

```json fig10_04/servicios-extra.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "inventario", "url": "http://127.0.0.1:8089/inventario"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"},
    {"nombre": "reportes", "url": "http://127.0.0.1:8088/"},
    {"nombre": "facturacion", "url": "http://127.0.0.1:8089/facturacion"},
    {"nombre": "notificaciones", "url": "http://127.0.0.1:8089/notificaciones", "timeout_ms": 1000},
    {"nombre": "basura", "url": "http://127.0.0.1:8089/basura"}
  ]
}
```

#### 10.3.4 Les tests du contrat

`RevisorTest` apporte six tests, chacun sur une promesse de la section : la version sort du `pom.xml`, une concurrence invalide est une erreur d'usage avec le code 2, tous en ligne donne 0, un tombé donne 1, le JSON d'un rapport connu est **exactement** le contrat de la leçon 8, et le serveur répond `200` avec `application/json`.

```java
// fig10_04/src/test/java/mx/habil/revisor/RevisorTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RevisorTest {

  private static HttpServer servidor;
  private static int puerto;

  @TempDir Path carpeta;

  @BeforeAll
  static void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/bien", i -> responder(i, 200));
    servidor.createContext("/mal", i -> responder(i, 503));
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    puerto = servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, int codigo) throws IOException {
    intercambio.sendResponseHeaders(codigo, -1);
    intercambio.close();
  }

  /** Lo que dejó una corrida de {@code ejecutar}: el código de salida y los dos flujos. */
  private record Corrida(int codigo, String salida, String errores) {}

  private static Corrida correr(String... args) {
    var salida = new ByteArrayOutputStream();
    var errores = new ByteArrayOutputStream();
    int codigo =
        Revisor.ejecutar(
            args,
            new PrintStream(salida, true, StandardCharsets.UTF_8),
            new PrintStream(errores, true, StandardCharsets.UTF_8));
    return new Corrida(
        codigo,
        salida.toString(StandardCharsets.UTF_8),
        errores.toString(StandardCharsets.UTF_8));
  }

  private Path configuracionCon(String... rutas) throws IOException {
    var servicios = new StringBuilder();
    for (int i = 0; i < rutas.length; i++) {
      servicios.append(i == 0 ? "" : ",");
      String url = "http://127.0.0.1:" + puerto + rutas[i];
      servicios.append("{\"nombre\": \"s%d\", \"url\": \"%s\"}".formatted(i, url));
    }
    Path archivo = carpeta.resolve("servicios.json");
    Files.writeString(archivo, "{\"servicios\": [" + servicios + "]}", StandardCharsets.UTF_8);
    return archivo;
  }

  private static Reporte reporteConocido() {
    var servicios =
        List.of(
            resultado("catalogo", "http://127.0.0.1:8089/catalogo", new Respondio(200, 142)),
            resultado("inventario", "http://127.0.0.1:8089/inventario", new Respondio(200, 2300)),
            resultado("pagos", "http://127.0.0.1:8089/pagos", new Respondio(200, 87)),
            resultado("reportes", "http://127.0.0.1:8088/", new SinRespuesta("sin conexión")));
    return Reporte.de(servicios);
  }

  private static Resultado resultado(String nombre, String url, Respuesta respuesta) {
    return new Resultado(
        new Servicio(nombre, URI.create(url)), Clasificador.clasificar(respuesta), respuesta);
  }

  @Test
  void laVersionSaleDelPomYElCodigoEsCero() {
    Corrida corrida = correr("--version");
    assertEquals(0, corrida.codigo());
    assertEquals("revisor 1.0.0", corrida.salida().strip());
  }

  @Test
  void unaConcurrenciaInvalidaEsUnErrorDeUsoConCodigoDos() {
    Corrida corrida = correr("--concurrencia", "0");
    assertEquals(2, corrida.codigo());
    assertTrue(corrida.errores().contains("--concurrencia debe ser un entero"), corrida.errores());
    assertEquals("", corrida.salida());
  }

  @Test
  void conTodosArribaElCodigoEsCero() throws Exception {
    Path config = configuracionCon("/bien", "/bien");
    Corrida corrida = correr("--config", config.toString());
    assertEquals(0, corrida.codigo());
    assertTrue(corrida.salida().contains("2 servicios · 2 arriba"), corrida.salida());
  }

  @Test
  void conUnServicioCaidoElCodigoEsUno() throws Exception {
    Path config = configuracionCon("/bien", "/mal");
    Corrida corrida = correr("--config", config.toString());
    assertEquals(1, corrida.codigo());
    assertTrue(corrida.salida().contains("1 caído"), corrida.salida());
  }

  @Test
  void elJsonDeUnReporteConocidoEsElContrato() {
    String esperado =
        """
        {
          "servicios" : [ {
            "nombre" : "catalogo",
            "url" : "http://127.0.0.1:8089/catalogo",
            "estado" : "ARRIBA",
            "tiempo_ms" : 142,
            "detalle" : "200"
          }, {
            "nombre" : "inventario",
            "url" : "http://127.0.0.1:8089/inventario",
            "estado" : "LENTO",
            "tiempo_ms" : 2300,
            "detalle" : "200"
          }, {
            "nombre" : "pagos",
            "url" : "http://127.0.0.1:8089/pagos",
            "estado" : "ARRIBA",
            "tiempo_ms" : 87,
            "detalle" : "200"
          }, {
            "nombre" : "reportes",
            "url" : "http://127.0.0.1:8088/",
            "estado" : "CAIDO",
            "tiempo_ms" : null,
            "detalle" : "sin conexión"
          } ],
          "resumen" : {
            "total" : 4,
            "arriba" : 2,
            "lento" : 1,
            "caido" : 1,
            "promedio_ms" : 843
          }
        }""";
    assertEquals(esperado, Formato.json(reporteConocido()));
  }

  @Test
  void elServidorContestaElReporteComoJson() throws Exception {
    HttpServer propio = Servidor.iniciar(0, RevisorTest::reporteConocido);
    try (HttpClient cliente = HttpClient.newHttpClient()) {
      URI direccion = URI.create("http://127.0.0.1:" + propio.getAddress().getPort() + "/reporte");
      var respuesta =
          cliente.send(HttpRequest.newBuilder(direccion).GET().build(), BodyHandlers.ofString());
      assertEquals(200, respuesta.statusCode());
      assertEquals("application/json", respuesta.headers().firstValue("Content-Type").orElse(""));
      assertEquals(Formato.json(reporteConocido()), respuesta.body());
    } finally {
      propio.stop(0);
    }
  }
}
```

Trois choses pour la lire avec profit. La classe `Corrida` est un *record* privé qui regroupe ce qu'une exécution a laissé : le code et les deux textes. `correr(...)` appelle `Revisor.ejecutar` avec deux flux en mémoire et les décode en UTF-8 ; il ne termine aucune JVM. Et le test du JSON compare avec un bloc de texte écrit à la main **caractère par caractère** (y compris les espaces autour des deux-points, qui sont ceux de Jackson) : si quelqu'un change le nom d'une clé, l'ordre des champs ou le format, ce test passe au rouge. C'est ce que signifie que le JSON soit un contrat, et non un détail d'implémentation.

Comme toujours, avant de te fier au test, mets-le au rouge. Dans `Revisor.java`, remplace `return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;` par `return CODIGO_TODO_ARRIBA;` (le programme « dit toujours que tout va bien ») et exécute les tests :

```text
[ERROR] mx.habil.revisor.RevisorTest.conUnServicioCaidoElCodigoEsUno -- Time elapsed: 0.154 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
[ERROR] Tests run: 34, Failures: 1, Errors: 0, Skipped: 0
```

Le seul qui passe au rouge est celui du code pour un service en panne, avec les nombres qu'il devait : il attendait 1 et a reçu 0. Rétablis la ligne et tout revient au vert : cela fait **34** tests, 28 de la leçon 9 et 6 d'aujourd'hui.

#### 10.3.5 L'essayer à la main

Avec les 34 tests au vert, `mvn -B verify` a aussi laissé les bibliothèques dans `target/lib/` (la copie de la leçon 8), donc le `revisor` s'exécute avec le *classpath* de toujours. Avec le laboratoire allumé, dans un terminal :

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor --servir 8090
sirviendo http://127.0.0.1:8090/reporte (Ctrl+C para detener)
```

et dans un autre, `curl -i http://127.0.0.1:8090/reporte` renvoie (abrégé) :

```text
HTTP/1.1 200 OK
Date: Tue, 13 Oct 2026 21:12:08 GMT
Content-type: application/json
Content-length: 736

{
  "servicios" : [ {
    "nombre" : "catalogo",
    "url" : "http://127.0.0.1:8089/catalogo",
    "estado" : "ARRIBA",
    "tiempo_ms" : 176,
    "detalle" : "200"
  }, {
```

Une requête vers un autre chemin renvoie `404`, et un `POST` vers ce même `/reporte`, `405`. Pour vérifier qu'il écoute **seulement** sur l'interface locale, sur ta Mint tu peux demander à `ss` les ports à l'écoute, en filtrant le 8090 (`ss -ltn 'sport = :8090'`) : l'adresse locale doit être `127.0.0.1:8090`, pas `0.0.0.0:8090` ni `*:8090`. Sur la machine où j'ai mesuré, qui n'est pas Linux, l'équivalent `lsof -nP -iTCP:8090 -sTCP:LISTEN` a dit `TCP 127.0.0.1:8090 (LISTEN)`.

Et l'arrêt. En lui envoyant le signal qu'envoie `Ctrl`+`C` (`SIGINT`), le terminal a affiché :

```text
servidor detenido
```

et le code de sortie a été **130**. Avec `kill -TERM`, le signal qu'envoient les administrateurs de services, `servidor detenido` est aussi apparu et le code a été **143**. Ce ne sont pas des erreurs du `revisor` : c'est la convention du terminal, qui, lorsqu'un processus meurt à cause d'un signal, rapporte `128 + le numéro du signal` (2 pour `SIGINT`, 15 pour `SIGTERM`), comme le documente ce même manuel de `bash`. Si, dans un script, ton `revisor` en mode serveur se termine avec 143, cela signifie qu'on l'a arrêté, pas qu'il a échoué. Après l'avoir arrêté, le port 8090 est libre : je l'ai vérifié avec la même commande, qui ne renvoie aucune ligne. Dans la seconde séance, cette même commande se raccourcit en `java -jar target/revisor.jar --servir 8090` (10.6).

*(Ici se termine la première séance.)*

### 10.4 Qualité : Checkstyle et SpotBugs dans `verify`

#### 10.4.1 Deux portes distinctes

Qu'un programme fonctionne ne veut pas dire qu'il soit bien fait. Il y a deux sortes de problèmes que les tests ne voient pas, et que deux outils automatiques voient.

**Checkstyle** vérifie la **forme** du code source : que l'indentation soit de deux espaces, que les lignes ne dépassent pas 100 colonnes, que les noms suivent la convention, que toute classe et méthode publiques aient leur commentaire Javadoc. Il le fait selon une configuration ; celle qu'utilise le cours est `google_checks.xml`, qui vient avec Checkstyle et codifie le [guide de style de Google pour Java](https://google.github.io/styleguide/javaguide.html). C'est pourquoi, dès la leçon 1, tout le code du cours porte deux espaces d'indentation : pour qu'aujourd'hui la porte s'ouvre sans rien réécrire. Un style commun n'est pas de l'esthétique : c'est ce qui permet de lire le code d'une autre personne sans trébucher, et qu'une différence dans une revue soit une vraie différence et non une différence d'espaces.

**SpotBugs** vérifie le **comportement probable** : il ne lit pas le texte, mais le *bytecode* déjà compilé, et cherche des motifs qui sont souvent des défauts : une ressource qui n'est pas fermée, un `null` déréférencé, une collection mutable exposée, une chaîne de format incorrecte. Chaque motif a un code en majuscules (comme `OS_OPEN_STREAM`) que l'on peut chercher dans son [catalogue](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html). Il ne remplace pas les tests : il couvre autre chose. Un test dit « ce cas se passe bien » ; une analyse statique dit « ce type d'erreur n'est nulle part dans le code », même dans les chemins qu'aucun test ne parcourt.

#### 10.4.2 Le `pom.xml` de la livraison

Les deux outils se branchent sur le cycle de Maven avec la phase `verify` (la dernière que tu utilises depuis la leçon 6), de sorte que `mvn -B verify` ne signifie plus « compile et passe les tests » mais « passe toutes les portes ». Voici le `pom.xml` complet de la version 1.0.0 :

```xml fig10_04/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>1.0.0</version>

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
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
      </resource>
    </resources>

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
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.6.2</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <finalName>revisor</finalName>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>mx.habil.revisor.Revisor</mainClass>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>3.6.0</version>
        <dependencies>
          <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>14.3.0</version>
          </dependency>
        </dependencies>
        <configuration>
          <configLocation>google_checks.xml</configLocation>
          <violationSeverity>warning</violationSeverity>
        </configuration>
        <executions>
          <execution>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <groupId>com.github.spotbugs</groupId>
        <artifactId>spotbugs-maven-plugin</artifactId>
        <version>4.10.4.1</version>
        <executions>
          <execution>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

Cinq choses du `pom.xml` comptent, dans cet ordre :

1. **La version est `1.0.0`**, et c'est le seul endroit où elle s'écrit. Le bloc `<resources>` avec `<filtering>true</filtering>`, que tu as ajouté en 10.3.3, fait que Maven remplace `${project.version}` à l'intérieur des fichiers de `src/main/resources` ; ainsi `revisor.properties` finit par dire `version=1.0.0` sans que personne ne l'écrive deux fois.
2. **`maven-dependency-plugin` a disparu.** On ne copie plus les dépendances dans `target/lib/` ; l'empaquetage de 10.6 les inclut dans le JAR.
3. **`maven-checkstyle-plugin` 3.6.0 porte une dépendance propre : Checkstyle 14.3.0.** Ce n'est pas un ornement. Le *plugin* apporte par défaut un Checkstyle plus ancien qui ne comprend pas la syntaxe de Java 25, et échoue en lisant n'importe quel fichier avec les motifs des leçons 3 et 4. Je l'ai vérifié en retirant cette dépendance :

   ```text
   [ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: Failed during checkstyle configuration: Exception was thrown while processing .../Clasificador.java: IllegalStateException occurred while parsing file .../Clasificador.java. 15:41: mismatched input 'when' expecting '->': InputMismatchException
   ```

   C'est un message qui ne mentionne aucune version, et qui pointe vers une ligne de ton code qui est correcte : le `when` d'un `switch` avec motifs. La cause est toujours la même, un analyseur plus ancien que le langage. Si tu vois une erreur d'« analyse » dans un fichier qui compile, soupçonne la version de l'outil avant ton code.
4. **`<violationSeverity>warning</violationSeverity>`.** Les problèmes que Checkstyle trouve avec `google_checks.xml` sont des avertissements (*warning*). Par défaut, le *plugin* ne casse la construction que pour des erreurs, et alors la porte serait ouverte même s'il y avait cent infractions. Avec `warning`, toute infraction casse `verify`.
5. **`spotbugs-maven-plugin` 4.10.4.1** avec l'objectif `check` dans `verify` : il exécute l'analyse et casse la construction s'il trouve quelque chose.

Quand tout est en ordre, `mvn -B verify` se termine ainsi (j'abrège ce que nous n'analysons pas) :

```text
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] --- shade:3.6.2:shade (default) @ revisor ---
[INFO] --- checkstyle:3.6.0:check (default) @ revisor ---
[INFO] You have 0 Checkstyle violations.
[INFO] --- spotbugs:4.10.4.1:check (default) @ revisor ---
[INFO] BugInstance size is 0
[INFO] BUILD SUCCESS
```

Lis les **nombres** et non le `BUILD SUCCESS` : 34 tests, 0 infraction, 0 défaut. Une porte qui n'a pas été exécutée se termine aussi par un succès.

#### 10.4.3 Ce qu'ils ont trouvé dans mon propre code

Je raconte cela parce que c'est le plus utile de la section. La première fois que j'ai exécuté `verify` sur le `revisor` de cette leçon, **Checkstyle a signalé cinq infractions de format** (des lignes de plus de 100 colonnes et les guillemets de fermeture d'un bloc de texte, que le guide de Google demande sur leur propre ligne), que j'ai corrigées en quelques minutes. Et quand tout a été propre, **SpotBugs a trouvé deux choses** dans du code que je croyais bon. Voici ses messages, tels quels :

```text
[ERROR] Medium: mx.habil.revisor.Revisor.version() may fail to close stream [mx.habil.revisor.Revisor] At Revisor.java:[line 118] OS_OPEN_STREAM
[ERROR] Medium: Format string should use %n rather than \n in mx.habil.revisor.Revisor.<static initializer for Revisor>() [mx.habil.revisor.Revisor] At Revisor.java:[line 35] VA_FORMAT_STRING_USES_NEWLINE
```

Cela se lit ainsi : un niveau de gravité (`Medium`), une description, la classe et la méthode, la ligne, et à la fin le code du motif. Les deux étaient vrais, de manières différentes. Le premier venait de ce que je lisais les propriétés avec `new InputStreamReader(flujo, UTF_8)` : bien que le `flujo` fût dans un `try-with-resources`, le lecteur qui l'enveloppe était un second objet que SpotBugs ne voyait pas fermé. La solution simple a été de lire les propriétés avec `propiedades.load(flujo)`, sans enveloppe (un fichier de propriétés se lit comme de l'ISO-8859-1, et un numéro de version est de l'ASCII). Le second était plus subtil : je construisais le texte d'aide avec un bloc de texte et `.formatted(...)`, et le bloc contient des sauts de ligne (`\n`) à l'intérieur d'une chaîne de format, là où Java attend `%n`, le saut de ligne de la plateforme. Ce n'était pas une erreur sous Linux, mais SpotBugs a raison de dire que c'est une fragilité. Je l'ai résolu en supprimant le formatage : le texte porte un marqueur `{limite}` et une méthode le remplace. Les deux corrections figurent dans le `Revisor.java` que tu vois en 10.3.3.

La leçon n'est pas que SpotBugs soit plus malin que toi. C'est que **l'outil trouve ce que tu ne cherchais pas**, et que corriger ses constats laisse presque toujours le code plus simple, pas plus long.

#### 10.4.4 Lire ce qu'ils disent et ne pas les éteindre

Quand Checkstyle casse la construction, le message apporte le fichier, la ligne et la colonne, la catégorie et le nom de la règle. Si tu retires le commentaire Javadoc de `main`, voici ce qu'il dit avec la JVM en espagnol :

```text
[WARNING] src/main/java/mx/habil/revisor/Revisor.java:[38,3] (javadoc) MissingJavadocMethod: Falta el comentario Javadoc para 'main'.
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: You have 1 Checkstyle violation.
```

(Le texte sort dans la langue de la JVM : avec `MAVEN_OPTS="-Duser.language=en"`, le même message dit `Missing a Javadoc comment for 'main'.`. Le nom de la règle, `MissingJavadocMethod`, est le même dans n'importe quelle langue, et c'est ce que tu dois chercher dans la [liste des règles](https://checkstyle.org/checks.html).) C'est pourquoi le `main` du `revisor` a un Javadoc, et pourquoi toute classe et méthode **publiques** en ont un ; les autres classes sont de paquet et ne l'exigent pas, ce qui est l'une des raisons de tout laisser fermé (leçon 6).

Il y a une seconde tentation qu'il convient de nommer et de rejeter : **éteindre la règle**. Tous ces outils offrent des façons de se taire (`@SuppressWarnings`, fichiers d'exclusion, baisse de la sévérité). Utilise-les seulement quand le constat est faux et que tu peux dire pourquoi ; jamais pour que la construction passe. Un `verify` vert parce que tu l'as fait taire ne vaut pas plus qu'un qui n'a pas été exécuté.

Et un avertissement sur le silence de SpotBugs, mesuré. Dans la leçon 4, tu as mis `List.copyOf` dans le constructeur de `Reporte` pour que le *record* ait sa propre copie de la liste, et cette même leçon a mesuré que SpotBugs se tait à son sujet dans une classe de paquet. Ici on voit pourquoi : **cela dépend de la visibilité.** Si je retire cette ligne de `Reporte`, qui est une classe de paquet (sans `public`), SpotBugs **ne dit rien** : `BugInstance size is 0`. Si je rends `Reporte` public et que je retire de nouveau la ligne, alors oui :

```text
[ERROR] Medium: mx.habil.revisor.Reporte.resultados() may expose internal representation by returning Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 6] EI_EXPOSE_REP
[ERROR] Medium: new mx.habil.revisor.Reporte(List, Resumen) may expose internal representation by storing an externally mutable object into Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 8] EI_EXPOSE_REP2
```

SpotBugs se préoccupe de ce qui **se voit depuis l'extérieur du paquet**, et une classe de paquet ne se voit pas. Ce que cela enseigne est double. La copie défensive reste correcte même si l'outil ne l'exige pas (il l'exigerait le jour où `Reporte` deviendrait public, et alors il serait trop tard pour y penser). Et le silence d'un outil est une information sur ce qu'il **a vérifié**, pas une garantie sur tout le reste.

*(Pour en savoir plus sur la façon dont ces portes s'intègrent dans un flux de livraison, il y a deux articles du blog : [des tests qui attrapent les erreurs](https://www.habil.mx/fr/blog/tests-qui-attrapent-erreurs-couverture-quality-gate/) et [CI/CD et DevSecOps](https://www.habil.mx/fr/blog/cicd-devsecops-infrastructure-propre/).)*

### 10.5 Revue de sécurité

La sécurité du `revisor` n'a pas été ajoutée à la fin : elle s'est construite là où le risque apparaissait. Cette section rassemble ce qui existe déjà et le passe en revue face à la surface d'attaque complète, qui compte quatre choses : **ce qui entre** (la configuration et les options), **où cela sort** (les requêtes qu'il fait), **ce qui écoute** (le serveur) et **ce qu'il montre** (le rapport).

**1. Ce qui entre : l'adresse de chaque service est écrite par une autre personne.** C'est le risque de la leçon 8, celui des requêtes falsifiées côté serveur (SSRF, pour *server-side request forgery*) : si le `revisor` interrogeait n'importe quelle adresse qui apparaît dans `servicios.json`, celui qui éditerait ce fichier en ferait un intermédiaire pour atteindre des choses qu'il n'atteint pas mais que le `revisor` atteint (comme le service de métadonnées d'un cloud, qui vit à `169.254.169.254`). Les défenses sont au nombre de trois, toutes d'une liste de ce qui est autorisé et aucune de ce qui est interdit : seulement les schémas `http` et `https` ; seulement des hôtes d'une **liste autorisée** ; et aucune redirection suivie. Le [guide d'OWASP sur SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) concorde : valider contre ce qui est autorisé, pas contre ce qui est connu comme mauvais.

Dans la leçon 8, la liste autorisée était une constante. Aujourd'hui elle est **configurable**, avec `--permitir h1,h2`, et cela change la question : qui contrôle cette option ? Elle appartient à celui qui **exécute** le programme, pas à celui qui édite le fichier de services. Cette séparation est la raison pour laquelle la liste ne va pas dans `servicios.json` : un fichier ne peut pas se donner la permission à lui-même. Si un jour l'idée te vient de lire la liste autorisée dans le même fichier qu'elle valide, ou depuis le réseau, tu as défait la défense. On peut le vérifier avec les trois fichiers qui accompagnent la leçon : un avec `ftp://`, un avec `file:///etc/passwd` et un avec `http://169.254.169.254/latest/meta-data/`. Les trois sont rejetés avant de faire une seule requête, avec le code 2 (voir l'exercice 1).

**2. Où cela sort : les redirections.** `HttpClient` a `followRedirects(HttpClient.Redirect.NEVER)` écrit dans `Revision.nuevoCliente()`. C'est la valeur par défaut, et on l'écrit quand même exprès : un hôte autorisé pourrait répondre « j'ai déménagé vers une autre adresse » en pointant vers une qui ne l'est pas, et suivre cette redirection sans la vérifier contournerait la liste. Le `revisor` traite un `3xx` comme une réponse de plus et la classe ; il ne la suit pas.

**3. Ce qui écoute : seulement l'interface locale.** Le serveur de 10.2 écoute sur `127.0.0.1`. Il y a un risque propre à la conception qui a déjà été annoncé : chaque `GET /reporte` déclenche une vérification complète, avec une requête par service. Un processus local malveillant, ou simplement négligent, qui demande le rapport en boucle, fait que le `revisor` interroge tous ces services sans répit. L'atténuation immédiate est l'interface locale, qui réduit ceux qui peuvent le faire aux programmes de la même machine ; les suivantes (un cache de quelques secondes, une limite de requêtes par seconde) ne sont pas faites, et cette leçon ne les simule pas : il est dit qu'elles manquent, et c'est le premier endroit où il faudrait continuer à travailler.

**4. Ce qu'il montre : le rapport est public pour qui le demande.** Le JSON inclut le champ `url` de chaque service, et `/reporte` le donne à qui le demande. C'est pourquoi le `revisor` ne compte pas sur un fichier propre : une adresse avec utilisateur ou mot de passe (`http://usuario:clave@...`) est rejetée au chargement, avec le code 2 et un message qui ne la répète pas, et celle qui porte un `token` dans la requête ou un fragment sort raccourcie : `ReporteJson` n'écrit que le schéma, l'hôte, le port et le chemin (Leçon 8). C'est le type de décision qui se prend **avant** que la donnée n'existe. Une erreur interne est réglée elle aussi : elle renvoie `error interno` et rien de l'exception.

**5. La sérialisation native, qui n'est pas utilisée.** Java a un ancien mécanisme pour enregistrer et récupérer des objets directement, `ObjectOutputStream` et `ObjectInputStream`. Lire des objets avec `ObjectInputStream` depuis une source en laquelle tu n'as pas confiance est l'une des vulnérabilités les plus graves et les mieux documentées de la plateforme, parce que la lecture peut exécuter du code choisi par celui qui a écrit les octets ([OWASP l'explique avec des exemples](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html), et le JDK offre des [filtres](https://docs.oracle.com/en/java/javase/25/core/serialization-filtering1.html) depuis [JEP 290](https://openjdk.org/jeps/290) pour l'atténuer). Le `revisor` ne l'utilise jamais, et sa sécurité ici ne vient pas de ce que le format soit du JSON (il existe aussi des bibliothèques de JSON qui, mal configurées, construisent l'objet que le texte leur demande), mais de trois décisions concrètes : **il n'utilise la sérialisation native nulle part** ; **il n'active pas la désérialisation polymorphe** de Jackson, l'option qui laisse le texte lui-même dire de quelle classe créer chaque objet ; et **il valide la configuration champ par champ contre la forme qu'il attend** (la leçon 8 la lit comme un arbre de `JsonNode` et vérifie chaque valeur, sans la mapper directement vers des objets). La règle du cours est simple : **les données de l'extérieur entrent comme du texte structuré et se valident à la main, jamais comme des objets.**

Pour poursuivre sur le sujet, l'article du blog sur la [sécurité des API](https://www.habil.mx/fr/blog/securite-api-cto-identite-passerelle-jetons/) traite de l'identité et des *tokens*, ce qu'il faudrait si le serveur sortait de la machine locale.

### 10.6 Le JAR exécutable

#### 10.6.1 Le problème du *classpath* long

Depuis la leçon 8, tu exécutes le `revisor` avec `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor`. C'est une longue ligne qui oblige à savoir trois choses : où sont tes classes, où sont les bibliothèques, et quelle classe a le `main`. Et elle échoue en silence de façons intéressantes. S'il te manque la partie des bibliothèques, voici ce que tu vois :

```text
$ java -cp target/classes mx.habil.revisor.Revisor --config servicios-arriba.json
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
	at mx.habil.revisor.Opciones.leer(Opciones.java:32)
	at mx.habil.revisor.Revisor.ejecutar(Revisor.java:50)
	at mx.habil.revisor.Revisor.main(Revisor.java:44)
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

Cela se lit de bas en haut : une classe de Jackson n'est pas dans le *classpath*, et la JVM le découvre juste au moment où elle en a besoin. La solution est ce qu'on donne à celui qui reçoit le programme : **un seul fichier** avec tout dedans.

#### 10.6.2 Ce qu'est un JAR, et ce que fait `shade`

Un **JAR** (*Java archive*) est un fichier ZIP avec des classes dedans et un fichier spécial, `META-INF/MANIFEST.MF`, avec des métadonnées. Si le manifeste porte la ligne `Main-Class: mx.habil.revisor.Revisor`, `java -jar archivo.jar` sait quelle classe démarrer. C'est la [spécification du JAR](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html).

`maven-jar-plugin`, qui s'exécute déjà depuis la leçon 6, génère le JAR du *projet* : seulement tes classes, sans bibliothèques et sans `Main-Class`. C'est pourquoi, si tu l'exécutes, voici ce qui se passe :

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

(En anglais, avec la JVM dans cette langue : `no main manifest attribute, in target/revisor-1.0.0.jar`.) C'est l'erreur du « JAR sans entrée » : ton JAR existe et est correct, mais il ne dit pas par où commencer. Et il a 32 entrées : tes classes et peu de chose en plus.

Ce qu'il faut, c'est [`maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/), qui s'exécute après et produit **un autre** JAR qui contient tes classes *et* celles de toutes les dépendances, avec un manifeste qui, lui, porte `Main-Class`. Dans le `pom.xml` de 10.4, ce sont trois lignes de configuration : `finalName` fait que le fichier s'appelle `revisor.jar` (sans la version, pour que la commande d'usage ne change pas à chaque version) ; le `ManifestResourceTransformer` avec `mainClass` écrit le `Main-Class` ; et `createDependencyReducedPom` à `false` évite que le *plugin* ne réécrive le `pom.xml` du projet, un effet secondaire dont nous n'avons pas besoin.

Avec lui, `mvn -B verify` laisse dans `target/` le JAR complet. Voici les chiffres que j'ai mesurés :

```text
$ unzip -p target/revisor.jar META-INF/MANIFEST.MF | grep Main
Main-Class: mx.habil.revisor.Revisor
$ unzip -l target/revisor.jar | tail -1
  6359578                     1337 files
$ unzip -p target/revisor.jar revisor.properties
version=1.0.0
```

1,337 entrées et environ 2.6 MB, dont 1,176 sont sous `tools/jackson`. Et la version sort du `pom.xml`, comme on l'a monté en 10.3.3.

Pendant la construction, `shade` imprime des avertissements qui alarment à première vue : `Discovered module-info.class. Shading will break its strong encapsulation.` et `define 2 overlapping resources: META-INF/LICENSE, META-INF/NOTICE`. Les deux sont vrais et ici inoffensifs : Jackson publie une description de module qui, quand tout est réuni dans un JAR, cesse d'être utilisée (nous n'utilisons pas le système de modules, et il n'est pas enseigné dans ce cours), et plusieurs JAR apportent leur propre fichier de licence du même nom, dont `shade` conserve un. Quand tu ne comprends pas un avertissement, ne l'ignore pas par habitude : lis-le, et décide avec le détail. Celui-ci est de ceux qu'on peut laisser.

#### 10.6.3 Le test qui compte : un autre dossier

Un JAR n'est indépendant que si tu peux l'exécuter **depuis n'importe quel dossier**. Essaie ceci, avec le chemin complet du JAR et du fichier de configuration :

```text
$ cd /tmp
$ java -jar ~/curso-java/revisor/target/revisor.jar --config ~/curso-java/revisor/servicios.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA     128ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 883ms
$ echo $?
1
```

Cela fonctionne, et sort avec 1 parce qu'il y a un service lent et un en panne. Remarque aussi ce qui se passe **sans** `--config` : `servicios.json` est un chemin relatif, et depuis `/tmp` il n'existe pas, donc le programme le dit avec son erreur et son code :

```text
$ java -jar ~/curso-java/revisor/target/revisor.jar
error de configuración: no existe el archivo servicios.json
$ echo $?
2
```

C'est l'erreur qu'a annoncée la leçon 5, celle du chemin relatif qui dépend de l'endroit où tu te trouves, maintenant avec son code de sortie. C'est un code 2 correct : ce n'est pas un service qui a échoué, c'est un usage incorrect.

### 10.7 Ce qui vient : `jpackage`

Un JAR règle la livraison du *programme*, pas celle de l'environnement : celui qui le reçoit a toujours besoin d'un Java installé. Pour celui qui n'en a pas, le JDK apporte deux outils qui restent une curiosité et ne sont pas enseignés : [`jlink`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jlink.html) assemble une version réduite de la JVM avec seulement les modules dont ton programme a besoin, et [`jpackage`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html) empaquette ton JAR avec cette JVM dans un installateur natif (un `.deb` pour ta Mint, par exemple) qui ne demande rien au préalable. Ils constituent l'étape suivante naturelle quand tu livres le programme à des personnes qui ne sont pas programmeurs. Une autre voie, différente, est de compiler le programme en code natif, qui a ses propres coûts et avantages ; l'article [quand convient-il de compiler Java en natif](https://www.habil.mx/fr/blog/java-natif-quand-oui-quand-non/) les discute.

## L'erreur que tu vas voir

L'erreur de la leçon est celle du JAR sans entrée, que tu viens de voir en 10.6.2 :

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

Cela signifie que le JAR ne déclare pas quelle classe démarrer : tu as exécuté le JAR léger du projet au lieu du complet (`revisor.jar`). Cela se corrige en exécutant l'autre, ou en ajoutant la configuration de `shade` au `pom.xml`. Les autres messages qu'il convient de reconnaître d'un coup d'œil :

- `error de uso: --concurrencia debe ser un entero mayor o igual que 1, no 0` (« erreur d'usage : --concurrencia doit être un entier supérieur ou égal à 1, et non 0 ») et code 2 : une option mal écrite ; c'est la sortie attendue du `revisor`, pas une panne.
- `error de configuración: no existe el archivo servicios.json` (« erreur de configuration : le fichier servicios.json n'existe pas ») et code 2 : tu as exécuté depuis un autre dossier sans `--config` (10.6.3).
- `no se pudo abrir el puerto: Address already in use` et code 2 : quelque chose écoute déjà sur ce port, peut-être un autre `revisor` que tu n'as pas arrêté. Je l'ai vérifié en en démarrant deux avec `--servir 8090`.
- `NoClassDefFoundError: tools/jackson/core/JacksonException` : Jackson manque dans le *classpath* ; utilise le JAR complet (10.6.1).
- `MissingJavadocMethod` de Checkstyle et `EI_EXPOSE_REP` de SpotBugs : les portes de qualité de 10.4 ; le message apporte la règle et la ligne.

## Ce qui se fait de travers

**Appeler `System.exit` au milieu du programme.** Il tue la JVM sans que personne puisse rien nettoyer et rend impossible à tester ce qui l'entoure. On l'appelle une fois, dans `main`, avec un nombre calculé par un autre code (10.1.2).

**Mélanger le résultat avec les avis.** Si le JSON et les erreurs vont sur la même sortie, celui qui redirigera le JSON vers un fichier y trouvera un message d'erreur au milieu. Le résultat sur la sortie standard, les avis sur celle d'erreur.

**Un code de sortie qui ne distingue rien.** Se terminer toujours avec 0 (ou toujours avec 1) ôte au programme son utilité dans un script. Trois codes aux significations fixes et documentées valent mieux qu'un arrangement ingénieux.

**Écouter sur toutes les interfaces par négligence.** `new InetSocketAddress(puerto)` sans adresse expose le service à tout le réseau. L'adresse s'écrit, toujours (10.2.2).

**Un hook d'arrêt long ou qui appelle `System.exit`.** Il finit par s'attendre lui-même. Un hook fait une chose courte et se termine (10.3.1).

**Renvoyer le message d'une exception par le réseau.** C'est livrer des chemins, des classes et parfois des données. On renvoie un texte générique et le détail va dans un journal (10.2.3).

**Faire taire une règle pour que `verify` passe.** L'analyse statique vaut tant qu'on l'écoute. La faire taire seulement quand le constat est faux et que tu peux expliquer pourquoi (10.4.4).

**Ne pas fixer la version d'un *plugin*.** Celui qui n'en porte pas est à la merci de ce qu'apporte Maven ce jour-là ; deux machines peuvent construire des choses différentes. Tous les *plugins* du `pom.xml` portent une version.

**Empaqueter sans tester depuis un autre dossier.** Que le JAR tourne dans le dossier du projet ne prouve pas qu'il soit indépendant (10.6.3).

## Exercices

### Exercice 1 — Les six vérifications du cours

La liste de « Comment savoir que tu as terminé » du plan compte six conditions. Vérifie-les **une par une**, avec le laboratoire allumé, en notant la commande et ce que tu as vu :

1. `mvn -B verify` se termine au vert et le rapport dit combien de tests ont été exécutés.
2. Il vérifie 20 services à la fois en un temps proche de celui du plus lent, pas de la somme (utilise `servicios-20.json` et `--concurrencia`).
3. Un service en panne, lent ou qui répond n'importe quoi ne fait pas tomber le rapport (utilise `servicios-extra.json`, qui ajoute `facturacion`, `notificaciones` et `basura`).
4. Aucune requête ne peut attendre pour toujours.
5. Il rejette les adresses qui ne sont pas `http` ou `https` et celles qui ne sont pas dans la liste autorisée.
6. Il s'exécute avec `java -jar` depuis un autre dossier et sort avec 0 si tout est en marche et avec 1 sinon.

Avant d'exécuter chacune, écris ce que tu t'attends à voir. En plus de `servicios-extra.json` (10.3.3) et de `servicios-20.json` (leçon 9), tu as besoin de quatre petits fichiers dans le dossier du projet. Le premier a deux services qui sont bien en marche :

```json fig10_04/servicios-arriba.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"}
  ]
}
```

Et ces trois-ci sont des adresses que le `revisor` ne doit jamais interroger : un schéma qui n'est pas du web, un fichier local et l'adresse du service de métadonnées d'un cloud :

```json fig10_04/servicios-ftp.json
{
  "servicios": [
    {"nombre": "archivo", "url": "ftp://127.0.0.1/datos"}
  ]
}
```

```json fig10_04/servicios-archivo.json
{
  "servicios": [
    {"nombre": "secreto", "url": "file:///etc/passwd"}
  ]
}
```

```json fig10_04/servicios-nube.json
{
  "servicios": [
    {"nombre": "metadatos", "url": "http://169.254.169.254/latest/meta-data/"}
  ]
}
```

### Exercice 2 — Que `notificaciones` ne déborde pas de la colonne (facultatif)

Regarde le tableau de `servicios-extra.json` : le nom `notificaciones` a 14 caractères et la colonne SERVICIO en mesure 12, donc le nom se colle à l'état. Change `Formato` pour que la largeur de la colonne se calcule à partir du nom le plus long, avec 12 au minimum, et de façon que le tableau des quatre services canoniques sorte **identique** à celui de toujours.

### Exercice 3 — Ouvre chaque porte exprès

Casse, un par un, ces trois composants de la qualité, **en prédisant avant** ce que dira la construction : (a) retire le commentaire Javadoc de `main` ; (b) retire la dépendance de Checkstyle 14.3.0 du `pom.xml` ; (c) retire la ligne `resultados = List.copyOf(resultados);` de `Reporte`. Dans chaque cas, note quel outil a parlé, ce qu'il a dit, et si cela t'a surpris qu'il ne parle pas.

### Exercice 4 — Arrête le serveur de deux façons

Lance le `revisor` avec `--servir 8090`. Arrête-le d'abord avec `Ctrl`+`C` et lis le code de sortie ; relance-le et arrête-le maintenant depuis un autre terminal avec `kill -TERM`. Explique les deux nombres et vérifie que le port est libre.

## Solutions

### Solution 1

**1. `mvn -B verify` au vert, avec le décompte lu dans le rapport.** Ce qui compte, ce sont les nombres, pas le code de sortie (c'est pourquoi on compte deux fois, avec deux méthodes) :

```text
$ mvn -B verify
...
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[INFO] You have 0 Checkstyle violations.
[INFO] BugInstance size is 0
[INFO] BUILD SUCCESS
$ grep -h -o ' tests="[0-9]*"' target/surefire-reports/TEST-*.xml | awk -F'"' '{s+=$2} END {print s}'
34
$ cat target/surefire-reports/TEST-*.xml | grep -c '<testcase '
34
$ grep -h -o ' skipped="[0-9]*"' target/surefire-reports/TEST-*.xml | sort | uniq -c
   7  skipped="0"
```

Sept classes de test, 34 tests, aucun sauté.

**2. Vingt services, en série contre en même temps.** Les vingt mettent une demi-seconde chacun. Avec `--concurrencia 1` (équivaut à les faire en série), avec 10 et avec 20, en mesurant le programme entier avec `time` :

```text
$ TIMEFORMAT='%R s'
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 1 > /dev/null
10.398 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 10 > /dev/null
1.245 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 20 > /dev/null
0.738 s
```

(Deuxièmes exécutions : 10.385, 1.253 et 0.747.) La somme des temps est 10 s ; avec la limite à 20, le programme met ce que met le plus lent, plus environ 0.2 s de démarrage de la JVM. Ce que demande le plan, « proche de celui du plus lent, pas de la somme », est respecté : 0.74 s contre 10.4 s.

**3. Ce qui échoue ne fait pas tomber le rapport.** Avec les trois services supplémentaires, le tableau sort complet et dans le même ordre ; `facturacion` répond 503, `notificaciones` épuise son délai maximal de 1000 ms (le chemin du laboratoire met 10 secondes) et `basura` ferme la connexion sans répondre :

```text
$ java -jar target/revisor.jar --config servicios-extra.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA     126ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      128ms  503
notificacionesCAIDO          —  tiempo agotado
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 694ms
$ echo $?
1
```

(Le nom `notificaciones` déborde de la colonne : c'est ce que corrige l'exercice 2.) Pour voir que l'**ordre** ne change pas même avec une concurrence différente, on extrait les noms du JSON avec trois limites différentes :

```text
$ for c in 1 3 10; do java -jar target/revisor.jar --config servicios-extra.json --concurrencia $c --formato json | grep -o '"nombre" : "[a-z]*"' | cut -d'"' -f4 | tr '\n' ' '; echo; done
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
```

(Les temps de la colonne TIEMPO changent à chaque exécution, et c'est pourquoi la moyenne de ce tableau n'est pas le 656 qui sortirait avec les nombres fixes de la leçon 4 : c'est celle des temps mesurés.)

**4. Aucune requête n'attend pour toujours.** On le vérifie de deux façons. Par le code : il n'y a qu'un seul endroit où le `revisor` envoie une requête, et cet endroit fixe les deux délais.

```text
$ grep -n "connectTimeout\|\.timeout(" src/main/java/mx/habil/revisor/Revision.java
31:        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
39:            .timeout(Duration.ofMillis(servicio.timeoutMs()))
$ grep -rn "\.send(" src/main
src/main/java/mx/habil/revisor/Revision.java:44:      var respuesta = cliente.send(peticion, BodyHandlers.discarding());
```

Et par l'horloge : le chemin `/notificaciones` du laboratoire met 10 secondes à répondre, et pourtant le rapport complet avec les sept services s'est terminé en **2.5 secondes** (`time` a donné 2.525 s) : la limite de 1000 ms du service l'a coupée, et le plus lent des autres, `inventario`, fixe le reste.

**5. Il rejette ce qui n'est pas `http` ni autorisé.** Trois fichiers, trois rejets, aucun ne fait une seule requête, et les trois avec le code 2 :

```text
$ java -jar target/revisor.jar --config servicios-ftp.json
error de configuración: servicio 1: 'url' debe empezar con http:// o https://
$ java -jar target/revisor.jar --config servicios-archivo.json
error de configuración: servicio 1: 'url' debe empezar con http:// o https://
$ java -jar target/revisor.jar --config servicios-nube.json
error de configuración: servicio 1: el servidor 169.254.169.254 no está en la lista permitida
$ echo $?
2
```

**6. Depuis un autre dossier, avec les deux codes.** Tu as déjà vu le cas du code 1 en 10.6.3. Pour le 0, il faut une configuration où tout est en ligne (`servicios-arriba.json`, avec `catalogo` et `pagos`) :

```text
$ cd /tmp
$ java -jar ~/curso-java/revisor/target/revisor.jar --config ~/curso-java/revisor/servicios-arriba.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     166ms  200
pagos       ARRIBA     114ms  200

2 servicios · 2 arriba · 0 lentos · 0 caídos · promedio 140ms
$ echo $?
0
```

Les six sont remplies. Note dans ton journal de bord la date et le décompte de tests du premier point : c'est la preuve que tu as terminé.

### Solution 2

On sépare le motif du format de la largeur, et `tabla` calcule la largeur à partir du nom le plus long (plus deux espaces de séparation), avec 12 au minimum. Avec les quatre services canoniques, le nom le plus long mesure 10 et le calcul donne 12 : le tableau ne change pas. Voici les parties de `Formato.java` qui sont modifiées (les fonctions `encabezado()` et `linea(Resultado)` sans largeur sont remplacées par celles d'en dessous, qui la reçoivent) :

```java
  private static final int ANCHO_NOMBRE_MINIMO = 12;

  private static String patron(int anchoNombre) {
    return "%-" + anchoNombre + "s%-10s%6s  %s";
  }

  static String encabezado(int anchoNombre) {
    return String.format(
        Locale.ROOT, patron(anchoNombre), "SERVICIO", "ESTADO", "TIEMPO", "DETALLE");
  }

  static String linea(Resultado resultado, int anchoNombre) {
    return String.format(
        Locale.ROOT,
        patron(anchoNombre),
        resultado.servicio().nombre(),
        resultado.estado(),
        tiempo(resultado.respuesta()),
        resultado.detalle());
  }

  static String tabla(Reporte reporte) {
    int masLargo =
        reporte.resultados().stream()
            .mapToInt(r -> r.servicio().nombre().length())
            .max()
            .orElse(0);
    int ancho = Math.max(ANCHO_NOMBRE_MINIMO, masLargo + 2);
    String renglones =
        reporte.resultados().stream()
            .map(r -> linea(r, ancho))
            .collect(Collectors.joining("\n"));
    return encabezado(ancho) + "\n" + renglones + "\n\n" + resumen(reporte.resumen());
  }
```

Avec ce changement, les 34 tests restent au vert, Checkstyle donne 0 infraction et SpotBugs 0 défaut, et le tableau de `servicios-extra.json` est aligné :

```text
SERVICIO        ESTADO    TIEMPO  DETALLE
catalogo        ARRIBA     175ms  200
inventario      LENTO       2.3s  200
pagos           ARRIBA     122ms  200
reportes        CAIDO          —  sin conexión
facturacion     CAIDO      125ms  503
notificaciones  CAIDO          —  tiempo agotado
basura          CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 690ms
```

### Solution 3

(a) Sans Javadoc sur `main`, c'est **Checkstyle** qui parle : `MissingJavadocMethod: Falta el comentario Javadoc para 'main'.` (« il manque le commentaire Javadoc pour 'main' »), une infraction, et `verify` échoue. C'est ce qu'on attend : `main` est public.

(b) Sans la dépendance de Checkstyle 14.3.0, c'est **Checkstyle qui parle, mais avec une erreur d'analyse et non de style** : `mismatched input 'when' expecting '->'` dans `Clasificador.java`. C'est celle qui surprend le plus, parce qu'elle ne mentionne pas la version et pointe vers du code correct (10.4.2).

(c) Sans la copie défensive, **personne ne parle** : `BugInstance size is 0`. C'est la surprise qui était annoncée : `Reporte` est une classe de paquet et SpotBugs ne se plaint que de ce qui se voit depuis l'extérieur. La copie doit rester là ; l'outil se tait, mais la raison pour laquelle elle existe n'a pas changé. Si tu rends `Reporte` public (avec son constructeur), alors `EI_EXPOSE_REP` et `EI_EXPOSE_REP2` apparaissent (10.4.4).

### Solution 4

Avec `Ctrl`+`C` apparaît `servidor detenido` et le code est **130** ; avec `kill -TERM` (par exemple, `kill -TERM $(pgrep -f 'revisor.jar --servir')` depuis un autre terminal), `servidor detenido` apparaît aussi, et le code est **143**. Les deux nombres sont `128 + le signal` (2 et 15), la convention du terminal pour les processus qui meurent à cause d'un signal ; ce ne sont pas des pannes. Après l'avoir arrêté, sur ta Mint, `ss -ltn 'sport = :8090'` ne doit afficher aucune ligne : l'arrêt propre a libéré le port. (J'ai mesuré les codes en envoyant les signaux depuis un script ; la commande `ss`, je ne l'ai pas exécutée sur la machine de mesure.)

## Comment savoir que j'ai réussi

- [ ] `java fig10_01.java --falla` imprime l'erreur d'usage et `echo $?` dit 2, et je sais dire pourquoi l'erreur s'écrit avec `System.err`.
- [ ] `java fig10_02.java` et `java fig10_03.java` se terminent avec les sorties de la leçon, et je peux expliquer à quoi sert chacune des cinq étapes du serveur minimal et quand s'exécute un hook d'arrêt.
- [ ] **(1)** `mvn -B verify` dit `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`, `You have 0 Checkstyle violations` et `BugInstance size is 0`, et le décompte de tests compté dans les XML de `surefire-reports` donne aussi 34.
- [ ] **(2)** Avec `servicios-20.json`, `--concurrencia 1` met environ 10 s, `--concurrencia 10` environ 1.2 s et `--concurrencia 20` environ 0.7 s (à peu près, selon ma machine), et je les ai mesurés moi-même avec `time`.
- [ ] **(3)** Avec `servicios-extra.json`, le tableau sort complet, avec les sept noms dans le même ordre avec n'importe quelle `--concurrencia`, et le code de sortie est 1.
- [ ] **(4)** Je sais montrer dans `Revision.java` les deux lignes qui fixent le délai de connexion et le délai maximal, et j'ai vérifié que le rapport avec `notificaciones` (un chemin de 10 s) se termine en environ 2.5 s.
- [ ] **(5)** Les trois fichiers `servicios-ftp.json`, `servicios-archivo.json` et `servicios-nube.json` sont rejetés avec le message attendu et le code 2.
- [ ] **(6)** `java -jar` depuis `/tmp` ou depuis mon dossier personnel, avec les chemins complets, imprime le tableau et sort avec 1 ; avec `servicios-arriba.json`, il sort avec 0.
- [ ] J'ai essayé au moins l'une des portes de qualité au rouge et j'ai compris le message (Solution 3), y compris le silence de SpotBugs face à `Reporte` sans copie.
- [ ] J'ai noté dans mon journal de bord quel constat de Checkstyle ou de SpotBugs je n'attendais pas.

## Résumé

Réponds sans regarder la leçon :

1. Que reçoit `main` dans `args`, et qu'est-ce que Java *ne* fait *pas* avec cela ?
2. Que dit par convention un code de sortie `0`, et qu'est-ce qui distingue entre eux le `1` et le `2` du `revisor` ?
3. Pourquoi `main` est-il une seule ligne et la logique vit-elle dans `ejecutar` ? Qu'y gagnent les tests ?
4. Pourquoi le résultat va-t-il sur la sortie standard et les avis sur celle d'erreur ?
5. Quelle différence y a-t-il entre écouter sur `127.0.0.1` et ne mettre aucune adresse ? Que signifie le port 0 ?
6. Qu'est-ce qu'un hook d'arrêt, quand s'exécute-t-il et quand non, et pourquoi ne doit-il pas appeler `System.exit` ?
7. Pourquoi le thread principal du mode serveur reste-t-il à attendre sur un `CountDownLatch` que personne n'ouvre ?
8. Que vérifie Checkstyle et que vérifie SpotBugs ? Pourquoi le silence de SpotBugs face à une collection mutable dans une classe de paquet ne prouve-t-il pas que tout va bien ?
9. Pourquoi la liste autorisée ne va-t-elle pas dans le fichier de services ? Pourquoi ne suit-on pas les redirections ?
10. Qu'ajoute `maven-shade-plugin` que ne fait pas `maven-jar-plugin`, et comment vérifie-t-on que le JAR est indépendant ?

## Pour aller plus loin

- [Oracle, `com.sun.net.httpserver.HttpServer` (Java 25)](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html) et [`Runtime.addShutdownHook`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html) : le serveur du JDK et le cycle de vie d'un arrêt. En anglais. Consulté le 7 octobre 2026.
- [Checkstyle, « Google's Style »](https://checkstyle.org/google_style.html) et [SpotBugs, catalogue de motifs](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html) : ce que vérifie chaque règle et ce que signifie chaque code. Consulté le 7 octobre 2026.
- [Apache Maven, `maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/) et [la spécification du JAR](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html) : comment se monte un JAR avec tout dedans et ce que dit son manifeste. Consulté le 7 octobre 2026.
- [OWASP, « Server-Side Request Forgery Prevention »](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) et [« Deserialization »](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html) : les deux risques de la revue de sécurité, avec leurs défenses. Consulté le 7 octobre 2026.

---

**Précédente :** [Leçon 9 — Concurrence : tout vérifier en même temps](09-concurrencia.md)
