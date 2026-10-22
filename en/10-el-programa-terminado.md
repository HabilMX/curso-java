# Lesson 10 — The finished program

**Time:** 2 × 90 min (two sessions; the break is between sections 10.3 and 10.4)

**What you build:** the complete `revisor`, ready to hand over: you drive it from the command line, it exits with a code a script can read, it can serve its report over HTTP, it passes the quality gates, and it is packaged into a single JAR

**What you learn:** arguments and exit codes as a contract; the JDK's own HTTP server; graceful shutdown with a shutdown hook; Checkstyle and SpotBugs inside `mvn verify`, and how to read what they find; a security review of the `revisor`; and the executable JAR with `maven-shade-plugin`

**Where you are coming from.** You bring the project from [Lesson 9](09-concurrencia.md): a concurrent `revisor` that checks twenty services in the time of the slowest one, with 28 tests passing. It works, but it is still a programmer's project and not a program someone else can use. To run it you have to type `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` and be standing in the right folder; the concurrency limit is a constant you change by recompiling; there is no way to ask for the report as JSON; if everything fails, the program ends the same way it would if everything were fine; and nobody has run a style review or an automatic defect search on it. Today those five things get closed. You work in your folder `~/curso-java/revisor/`.

**What this lesson does not do.** It does not use a command-line library (there are good ones, such as *picocli*, but the course avoids dependencies that are not needed, and seven options read fine by hand). It adds no encryption or authentication to the server: it is meant to be queried by another process on the same machine, and that is explained in 10.5. And it does not turn the JAR into an installer: `jpackage` is left as “what is coming”.

**About the runs.** Figures 10.1 to 10.3 are compact source files and run with `java figNN_MM.java`. Figure 10.4 is the project; it needs the lab in another terminal (`java Laboratorio.java`, port 8089; 8088 is left empty on purpose) and, when it starts its own server, it uses port 8090. The outputs were obtained with Temurin 25.0.4, Maven 3.10.0 and the *plugins* pinned in the `pom.xml`. Three honest warnings: **the timings are from my machine** (yours will differ; the proportions will not); **Checkstyle's messages, and some from parts of the JVM, come out in your system's language** (mine came out in Spanish; with `-Duser.language=en` they come out in English); and the commands that exist only on Linux (such as `ss`) I write the way you will use them on your Mint, but **I could not run them on the machine where I measured**, so you will see their exact output yourself.

## By the end you will be able to

- Read command-line arguments by hand, validate them, and return a clear usage error without showing a stack trace.
- Explain what an exit code is, set one for each situation of the `revisor` (`0`, `1`, `2`), and read it in the terminal with `$?`.
- Start an HTTP server with JDK classes, listening only on `127.0.0.1`, and shut it down in an orderly way when someone interrupts it.
- Run Checkstyle and SpotBugs in `mvn verify`, read their messages, and explain why a silent analysis does not prove there are no defects.
- Review the `revisor` against a list of concrete risks (addresses, redirects, serialization, network interface) and say what each decision covers.
- Package the program into a single JAR and run it with `java -jar` from any folder.
- Check, one by one, the six conditions by which the syllabus says the course is finished.

## The why before the how

When is a program finished? Not when it runs on your computer: you achieved that back in lesson 5, and up to lesson 9 every lesson was tested that way. A program is finished when **another person, another program, or you yourself six months from now can use it without reading the code**. That demands things that have nothing to do with the `revisor`'s logic.

It demands an **interface**: a way to tell it what to do without editing anything. It demands an **honest answer to whoever calls it**: a script that runs the `revisor` every five minutes needs to know whether everything went well without having to read the table, and that is what the exit code is for. It demands **quality that verifies itself**, because “I reviewed it” does not scale and a style that depends only on people's discipline erodes. It demands that whatever **listens on the network** does so carefully. And it demands a simple **delivery**: one file, one command.

This lesson takes those five and closes them. The **first** session (10.1 to 10.3) is the interface: arguments, exit codes and the server with its graceful shutdown. The **second** (10.4 to 10.6) is the delivery: the quality gates, the security review and the JAR. And exercise 1 is the list that the course syllabus calls “How you know you are done”: six measurable checks that you perform yourself, with the clock and the commands, and that tell you whether the `revisor` is truly finished.

## The concepts

### 10.1 Arguments and exit code

#### 10.1.1 Command-line arguments

When you type `java -jar revisor.jar --config otro.json`, everything that comes after the program name reaches `main` as an array of strings: the `String[] args` you already know from lesson 6. Java **does not interpret them**: it does not know what `--config` is, or that `otro.json` is its value, or that `--concurrencia` asks for a number. It just hands them over in the order you typed them. Figure 10.1 prints them, and it also shows how to leave a program with a code:

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

With no arguments, the program counts them and ends normally:

```text
$ java fig10_01.java
recibí 0 argumentos: []
termino bien
```

If you pass arguments, they arrive as they are, as separate strings and in order. This is a run with those of a typical `revisor` (the single-file launcher, `java Archivo.java`, hands `main` everything that comes after the file name):

```text
$ java fig10_01.java --config servicios.json --concurrencia 3
recibí 4 argumentos: [--config, servicios.json, --concurrencia, 3]
termino bien
```

Notice that the `3` is the string `"3"`, not a number: converting it, and deciding what to do if someone types `tres`, is your job. That is what the `Opciones` class will do in 10.1.3.

#### 10.1.2 The exit code: a contract with whoever calls you

Every program, when it ends, hands the operating system a number between 0 and 255: its **exit code** (*exit status*). It is the shortest possible answer to the question “how did it go?”, and it is what scripts, task schedulers and continuous-integration tools read. The universal convention on Unix-like systems is: **`0` means success, any other number means something went wrong**. In a terminal running `bash`, the code of the last command is in the variable `$?`, and the [`bash` manual](https://www.gnu.org/software/bash/manual/html_node/Exit-Status.html) documents that convention, which is the one that `if`, `&&` and `set -e` look at.

The last part of `fig10_01.java` already makes use of this: with the single argument `--falla`, the program writes an error to the error output and ends with `System.exit(2)`:

```text
$ java fig10_01.java --falla
recibí 1 argumentos: [--falla]
error de uso: pediste fallar
$ echo $?
2
```

Two details that seem minor and are not. The first: the error was written with `System.err`, not with `IO.println`. A program has **two distinct outputs**: the **standard output** (`System.out`, where the result goes) and the **error output** (`System.err`, where the notices for a person go). If someone redirects the result to a file —`revisor --formato json > reporte.json`— they want the JSON in the file and the errors on the screen, not mixed together. The second: `System.exit` **ends the whole JVM and does not return**: the line after the call never runs, and neither does the rest of the program, in any thread. It is not an abrupt cut: it starts the JVM's orderly shutdown, which runs the shutdown hooks (10.3) and waits for them to finish before exiting. But from your code's point of view, everything that came after no longer happens. That is why it is called in a single place, `main`, and is given a number that other code calculated.

The `revisor` sets its codes as follows, and it is a **contract**: what other programs can assume and that, for that reason, is not changed without notice.

| Code | When | What is written |
|---|---|---|
| `0` | all services are `ARRIBA` (or `--ayuda` or `--version` was requested) | the report, on the standard output |
| `1` | at least one service is `LENTO` or `CAIDO` | the report, on the standard output |
| `2` | the options are not understood, or the configuration is unusable | a message on the error output |

The difference between `1` and `2` is the one a script needs: `1` means “the program worked and found a problem you must attend to”, and `2` means “the program could not do its job; the problem lies with whoever runs it”. A monitoring system can alert differently for each.

One piece is missing in order to **test** that contract. If the whole flow of the program lives in `main` and `main` ends with `System.exit`, a test that calls `main` would take the tests' JVM down with it. The solution is the usual one, separating what decides from what acts: `main` is one line that only calls `System.exit(ejecutar(args, System.out, System.err))`, and everything else lives in `ejecutar`, a method that **receives** the two outputs as parameters and **returns** the code. A test gives it two in-memory streams, reads what it wrote, and checks the number it returned, without ending anything. It is the same idea as lesson 9: communicate by returning values.

#### 10.1.3 `Opciones`: reading the arguments by hand

This is the class that turns the array of strings into an already validated value. Before reading it, a summary of the seven flags it recognizes: `--config RUTA` (default `servicios.json`), `--formato tabla|json` (default `tabla`), `--concurrencia N` (default 10, at least 1), `--permitir h1,h2` (default `127.0.0.1,localhost`), `--servir PUERTO`, `--version` and `--ayuda`. Anything else is a usage error. The error is modeled with its own checked exception, just like `ErrorDeConfiguracion` in lesson 5:

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

Four ideas in the class deserve a paragraph.

**The arguments are consumed from a queue.** Instead of walking the array with an index and skipping positions, they are copied into an `ArrayDeque` and the first one is taken out with `removeFirst()`. When a flag needs its value, it calls `valor(pendientes, bandera)`, which takes out the next one or throws `ErrorDeUso("falta el valor de --config")` (the value of --config is missing) if there are none left. This way the code has no index arithmetic that could go wrong.

**The `switch` decides with an arrow.** It is the form you already know from lesson 2, but used as a statement (each case assigns a variable) and not as an expression. With arrows there is no *fall-through*: each case ends on its own. And the `default` throws the error, so a misspelled flag does not slip by silently.

**A number out of range and a text that is not a number say the same thing.** `entero` tries to convert with `Integer.parseInt`; if that fails, or if the value ends up out of range, the message is a single one (“debe ser un entero mayor o igual que 1, no tres” — must be an integer greater than or equal to 1, not tres). It is of no use to the person who mistyped the flag to tell the two causes apart.

**The *record* protects itself.** Its compact constructor does `permitidos = Set.copyOf(permitidos)`: the same defensive copy as in lesson 4, and for the same reason, so that the collection the *record* keeps is not one that someone else can change later. And a note on `OptionalInt puerto`: lesson 4 said that an `Optional` is used as a return value and not as a field. Here there is a conscious exception: `Opciones` is a read-only value that **describes what was requested**, and “no server was requested” has no number to represent it (for the server, 0 does mean something: “pick a free port”). The `OptionalInt` says exactly that.

With the options resolved, `ejecutar` uses them; you will see it in full in 10.3.3. First, though, we need to talk about the other way the `revisor` delivers its report.

### 10.2 The JDK's HTTP server

#### 10.2.1 What a server is for

So far, to see the report you have to run the program, which checks and ends. That works for a person at a terminal; it does not work for another tool that wants to query the status every minute, or for a dashboard that shows the result. For that, the `revisor` can stay **listening**: with `--servir 8090` it opens an HTTP server and, every time someone requests `/reporte`, it does a complete check and responds with the JSON from lesson 8.

The good news is that no library is needed to achieve this. The JDK ships a simple HTTP server, the package [`com.sun.net.httpserver`](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html), part of the `jdk.httpserver` module. It is a documented and supported API (the name `com.sun` is historical), and it is **the same class that `Laboratorio.java` uses**, the program you have been running since lesson 8: if you open it, you will find `HttpServer.create`, a list of `createContext` calls and a virtual-thread executor. Today you write yourself what you used to merely use. (For serving static files, the JDK also ships a command-line tool, `jwebserver`, described in [JEP 408](https://openjdk.org/jeps/408); we do not need it.)

#### 10.2.2 A minimal server, start to finish

Figure 10.2 is the smallest server worth reading. It starts a server, registers a path that answers a one-line JSON, makes a request to itself with the `HttpClient` from lesson 8, prints what it received and shuts down. Since it is a program that starts and ends on its own, it can be verified. (A reading note: the four `import` lines at the top are necessary because a compact source file only imports `java.base` by itself, and neither the server nor the HTTP client lives there; if you remove them, the compiler tells you `cannot find symbol` for `HttpClient` and the rest.)

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

The five steps, one by one. **`HttpServer.create(direccion, 0)`** creates the server without starting it; the second argument is the size of the queue of pending connections, and 0 means “whatever the system decides”. **`createContext("/hola", manejador)`** registers what to do with requests whose path starts with `/hola`. **`setExecutor(...)`** decides who serves the requests; **`start()`** puts it to listen; **`stop(0)`** stops it (the number is the seconds it waits for requests in flight, and it comes back in 10.3).

Three decisions in this figure recur in the `revisor`'s server:

**The address is `127.0.0.1`, not “all”.** `new InetSocketAddress("127.0.0.1", puerto)` makes the server listen **only on the local interface**: only programs on this same machine can talk to it. If instead you write `new InetSocketAddress(puerto)`, with no address, Java uses the wildcard address and listens on **all interfaces**, the network included: any machine that can reach yours could ask for the report. The figure checks it by printing `isLoopbackAddress()`. For an internal tool queried by another local process, the first is the right one; opening the second is a decision made on purpose, with authentication and encryption, not by default.

**Port 0 means “pick a free one”.** The system assigns any port that is not busy, and `servidor.getAddress().getPort()` tells you which it was. It is what the lesson 8 tests use, and today's, so as not to collide with whatever is running.

**One virtual thread per request.** `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` is lesson 9 applied. Without an executor, the server handles **one request at a time** on a single thread; I measured it: six simultaneous 100 ms requests reached the handler one at a time (maximum simultaneous: 1). With one virtual thread per request, a slow request does not get in the way of the others.

#### 10.2.3 `Servidor`: the `/reporte` path

The class the `revisor` uses receives the port and **a function that produces a `Reporte`** (a `Callable<Reporte>`). Receiving the function and not the already finished work has a reason: the check must happen **every time someone requests the report**, not just once at startup. And it has another, which you will see in the tests: it can be given a fixed report and you can check what it answers, without touching any network.

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

What each situation answers, so you do not have to guess it from the code: a `GET /reporte` responds `200` with the JSON and `Content-Type: application/json`; any other path, `404`; any other method (a `POST`, for example), `405`; and if the check fails because of a program error, `500` with the generic text `error interno` (internal error). That last point is deliberate: an exception's message can contain paths, class names or data that must not leave over the network, so whoever asks is told that something failed and nothing more. (Whoever operates the program would be told through a log, which this course does not build.) The `try (intercambio)` closes the conversation with the client no matter what, and the body is written as UTF-8 bytes, because the JSON carries accents (“sin conexión”, no connection).

A design warning worth stating. Each `GET /reporte` does **a complete check**: it queries all the services and takes as long as the slowest one (about 2.5 seconds with the four canonical ones). It is not a cache. If ten people ask for the report at the same time, the `revisor` does ten checks at the same time. It is a consequence that section 10.5 takes up again, because it has a security side.

### 10.3 Graceful shutdown and the complete `revisor`

#### 10.3.1 The problem: a server that does not know how to shut down

A server is a program that does not end on its own; it ends when someone interrupts it: you with `Ctrl`+`C`, the system as it shuts down, or a tool that sends it the termination signal. If at that moment there is a request half done, interrupting it abruptly cuts it off. The solution is **graceful shutdown**: stop accepting new requests, give the ones already on their way a few seconds, and only then exit.

Java offers a mechanism for this: the **shutdown hook**. It is a thread you register with `Runtime.getRuntime().addShutdownHook(hilo)` and that the JVM starts when it begins shutting down, for whatever reason: because the last normal thread ended, because someone called `System.exit`, or because the system sent it a termination signal such as the one from `Ctrl`+`C` (the [`Runtime` documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html) lists the cases). It does not run if the process dies abruptly, as with `kill -9` or a power cut; there, no orderly shutdown is possible. Figure 10.3 shows it in its smallest form:

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

The program ends on its own (`main termina`, main ends), and **afterwards** the hook runs (`gancho: cerrando con orden`, hook: closing in an orderly way). With `Ctrl`+`C` the same would happen. Two rules for writing a good hook: it should be **short** and not stay blocked (the JVM waits for all hooks to finish, so a hook that never ends leaves the program unable to exit; if there is a limit, it comes from outside, for example from the operating system which, when shutting down, ends the process abruptly), and it should **never call `System.exit`** (the shutdown is already under way, and calling it from inside leaves it waiting for itself forever).

#### 10.3.2 How the `revisor` uses it

In server mode, the `revisor` does three things, in this order: it starts the server; it registers a hook that calls `servidor.stop(1)` (stops accepting, waits up to 1 second for in-flight requests to finish, and closes) and writes `servidor detenido` (server stopped); and it **waits**. That wait is the deceptive part. If `ejecutar` returned a number and `main` called `System.exit`, the server would die an instant after being born. That is why the main thread stays waiting with `new CountDownLatch(1).await()`: a latch that nobody is going to open, so `await()` **never returns** and the program lives until the JVM is shut down from outside, whereupon the hook does its job.

You will try it by hand in 10.3.5, once you have the complete `Revisor` from the next section and its tests passing.

#### 10.3.3 The complete `revisor`

Now you can see `Revisor.java` in full. Read it as a six-step recipe, which are the steps of `ejecutar`: **1** read the options (if they fail: usage message and code 2); **2** if they asked for `--ayuda` or `--version`, answer and exit with 0; **3** read the configuration, validated against the allowed list that was chosen (if it fails: message and code 2); **4** if they asked for `--servir`, hand the work to `servir` and do not return; **5** otherwise, check everything with the requested concurrency limit; **6** write the table or the JSON and return 0 if all services are up, and 1 if not.

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

A few things that are not in plain view. The version printed by `--version` **is not written in the code**: it comes from the `revisor.properties` file, which Maven fills in with the `pom.xml` version, so the number is not repeated in two places that could drift out of sync (you set it up right below). If the port is busy, `HttpServer.create` throws an `IOException` and `ejecutar` catches it and says `no se pudo abrir el puerto: Address already in use` (could not open the port), with code 2: it is a problem for whoever runs it. And the `catch (InterruptedException)` restores the interrupt flag, as lesson 5 taught, before exiting with 1.

For that to work you need three changes outside the code, and you make them now, before the tests, because one of them checks precisely the version. The first is the properties file, in a new folder, `src/main/resources/`, the one for files that accompany the program without being code (Maven copies them to `target/classes`). It is a single line:

```properties fig10_04/src/main/resources/revisor.properties
version=${project.version}
```

The other two go in your `pom.xml`. The version goes from `0.9.0` to **`1.0.0`**, the delivery one. And inside `<build>`, before `<plugins>`, you add the block that asks Maven to **filter** that folder, that is, to replace `${project.version}` with the `pom.xml` version when copying the file:

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

It is a fragment: the rest of the `pom.xml` stays as you have it from lesson 9, with `maven-dependency-plugin` included, which you still use to run the program. The complete delivery `pom.xml` appears in 10.4.2, when the quality gates and the JAR arrive.

The configuration file with the seven services (the four canonical ones and the three extra ones from the lab) hangs from the project folder, and you will use it further below:

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

#### 10.3.4 The contract tests

`RevisorTest` brings six tests, each on one promise of the section: the version comes from the `pom.xml`, an invalid concurrency is a usage error with code 2, all up gives 0, one down gives 1, the JSON of a known report is **exactly** the contract from lesson 8, and the server answers `200` with `application/json`.

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

Three things to read it profitably. The `Corrida` class is a private *record* that groups what a run left behind: the code and the two texts. `correr(...)` calls `Revisor.ejecutar` with two in-memory streams and decodes them as UTF-8; it ends no JVM. And the JSON test compares against a hand-written text block **character by character** (including the spaces around the colons, which are Jackson's): if someone changes the name of a key, the order of the fields or the format, this test turns red. That is what it means for the JSON to be a contract, and not an implementation detail.

As always, before trusting the test, turn it red. In `Revisor.java`, change `return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;` to `return CODIGO_TODO_ARRIBA;` (the program “always says all is well”) and run the tests:

```text
[ERROR] mx.habil.revisor.RevisorTest.conUnServicioCaidoElCodigoEsUno -- Time elapsed: 0.154 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
[ERROR] Tests run: 34, Failures: 1, Errors: 0, Skipped: 0
```

The only one that turns red is the code for a down service, with the numbers it should have: it expected 1 and got 0. Restore the line and go back to green: there are **34** tests, 28 from lesson 9 and 6 from today.

#### 10.3.5 Trying it by hand

With the 34 tests passing, `mvn -B verify` has also left the libraries in `target/lib/` (the copy from lesson 8), so the `revisor` runs with the usual *classpath*. With the lab on, in one terminal:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor --servir 8090
sirviendo http://127.0.0.1:8090/reporte (Ctrl+C para detener)
```

and in another, `curl -i http://127.0.0.1:8090/reporte` returns (trimmed):

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

A request to another path returns `404`, and a `POST` to the same `/reporte`, `405`. To check that it listens **only** on the local interface, on your Mint you can ask `ss` for the ports that are listening, filtering for 8090 (`ss -ltn 'sport = :8090'`): the local address must be `127.0.0.1:8090`, not `0.0.0.0:8090` or `*:8090`. On the machine where I measured, which is not Linux, the equivalent `lsof -nP -iTCP:8090 -sTCP:LISTEN` said `TCP 127.0.0.1:8090 (LISTEN)`.

And the shutdown. When sent the signal that `Ctrl`+`C` sends (`SIGINT`), the terminal showed:

```text
servidor detenido
```

and the exit code was **130**. With `kill -TERM`, the signal that service managers send, `servidor detenido` also appeared and the code was **143**. These are not errors of the `revisor`: it is the terminal's convention, which, when a process dies from a signal, reports `128 + the signal number` (2 for `SIGINT`, 15 for `SIGTERM`), as documented in the same `bash` manual. If in a script your `revisor` in server mode ends with 143, it means it was stopped, not that it failed. After stopping it, port 8090 is free: I checked it with the same command, which returns no line. In the second session this same command is shortened to `java -jar target/revisor.jar --servir 8090` (10.6).

*(The first session ends here.)*

### 10.4 Quality: Checkstyle and SpotBugs inside `verify`

#### 10.4.1 Two different gates

A program working does not mean it is well made. There are two kinds of problem that tests do not see, and that two automatic tools do.

**Checkstyle** checks the **form** of the source code: that indentation is two spaces, that lines do not go past 100 columns, that names follow the convention, that every public class and method has its Javadoc comment. It does so against a configuration; the one the course uses is `google_checks.xml`, which comes inside Checkstyle and encodes the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). That is why since lesson 1 all the course's code has two-space indentation: so that today the gate opens without rewriting anything. A common style is not aesthetics: it is what lets you read another person's code without stumbling, and what makes a difference in a review a real difference and not one of spaces.

**SpotBugs** checks the **probable behavior**: it does not read the text, but the already compiled *bytecode*, and it looks for patterns that tend to be defects: a resource that is not closed, a `null` that gets dereferenced, an exposed mutable collection, an incorrect format string. Each pattern has an uppercase code (such as `OS_OPEN_STREAM`) that can be looked up in its [catalog](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html). It does not replace tests: it covers something else. A test says “this case comes out right”; a static analysis says “this kind of error is nowhere in the code”, even on the paths that no test walks.

#### 10.4.2 The delivery `pom.xml`

The two tools hook into Maven's lifecycle at the `verify` phase (the last one you have been using since lesson 6), so `mvn -B verify` no longer means “compile and pass the tests” but “pass all the gates”. This is the complete `pom.xml` for version 1.0.0:

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

Five things in the `pom.xml` matter, in this order:

1. **The version is `1.0.0`**, and it is the only place where it is written. The `<resources>` block with `<filtering>true</filtering>`, which you added in 10.3.3, makes Maven replace `${project.version}` inside the files of `src/main/resources`; that way `revisor.properties` ends up saying `version=1.0.0` without anyone writing it twice.
2. **`maven-dependency-plugin` is gone.** Dependencies are no longer copied to `target/lib/`; the packaging in 10.6 includes them inside the JAR.
3. **`maven-checkstyle-plugin` 3.6.0 carries a dependency of its own: Checkstyle 14.3.0.** It is not decoration. By default the *plugin* brings an older Checkstyle that does not understand Java 25 syntax, and it fails when reading any file with the patterns from lessons 3 and 4. I confirmed it by removing that dependency:

   ```text
   [ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: Failed during checkstyle configuration: Exception was thrown while processing .../Clasificador.java: IllegalStateException occurred while parsing file .../Clasificador.java. 15:41: mismatched input 'when' expecting '->': InputMismatchException
   ```

   It is a message that mentions no version, and it points to a line of your code that is fine: the `when` of a `switch` with patterns. The cause is always the same, a parser that is older than the language. If you see a “parsing” error in a file that compiles, suspect the tool's version before your code.
4. **`<violationSeverity>warning</violationSeverity>`.** The problems Checkstyle finds with `google_checks.xml` are warnings. By default the *plugin* only breaks the build on errors, and then the gate would be open even with a hundred violations. With `warning`, any violation breaks `verify`.
5. **`spotbugs-maven-plugin` 4.10.4.1** with the `check` goal at `verify`: it runs the analysis and breaks the build if it finds anything.

When everything is in order, `mvn -B verify` ends like this (I trim what we do not analyze):

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

Read the **numbers** and not the `BUILD SUCCESS`: 34 tests, 0 violations, 0 defects. A gate that did not run also ends in success.

#### 10.4.3 What they found in my own code

I tell this because it is the most useful part of the section. The first time I ran `verify` on this lesson's `revisor`, **Checkstyle flagged five formatting violations** (lines over 100 columns and the closing quotes of a text block, which Google's guide asks to be on their own line), which I fixed in minutes. And when it was clean, **SpotBugs found two things** in code I believed was fine. These were its messages, verbatim:

```text
[ERROR] Medium: mx.habil.revisor.Revisor.version() may fail to close stream [mx.habil.revisor.Revisor] At Revisor.java:[line 118] OS_OPEN_STREAM
[ERROR] Medium: Format string should use %n rather than \n in mx.habil.revisor.Revisor.<static initializer for Revisor>() [mx.habil.revisor.Revisor] At Revisor.java:[line 35] VA_FORMAT_STRING_USES_NEWLINE
```

It reads like this: a severity level (`Medium`), a description, the class and method, the line, and at the end the pattern's code. Both were true, in different ways. The first came from my reading the properties with `new InputStreamReader(flujo, UTF_8)`: even though the `flujo` was in a `try-with-resources`, the reader wrapping it was a second object that SpotBugs did not see closed. The simple way out was to read the properties with `propiedades.load(flujo)`, with no wrapper (a properties file is read as ISO-8859-1, and a version number is ASCII). The second was subtler: I built the help text with a text block and `.formatted(...)`, and the block carries line breaks (`\n`) inside a format string, where Java expects `%n`, the platform's line break. It was not an error on Linux, but SpotBugs is right that it is a fragility. I resolved it by removing the formatting: the text carries a `{limite}` marker and a method replaces it. Both fixes are in the `Revisor.java` you see in 10.3.3.

The lesson is not that SpotBugs is smarter than you. It is that **the tool finds what you were not looking for**, and that fixing its findings almost always leaves the code simpler, not longer.

#### 10.4.4 Reading what they say and not switching them off

When Checkstyle breaks the build, the message carries the file, the line and the column, the category and the name of the rule. If you remove the Javadoc comment from `main`, this is what it says with the JVM in Spanish:

```text
[WARNING] src/main/java/mx/habil/revisor/Revisor.java:[38,3] (javadoc) MissingJavadocMethod: Falta el comentario Javadoc para 'main'.
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: You have 1 Checkstyle violation.
```

(The text comes out in the JVM's language: with `MAVEN_OPTS="-Duser.language=en"` the same message says `Missing a Javadoc comment for 'main'.`. The rule's name, `MissingJavadocMethod`, is the same in any language, and it is what you should look up in the [list of rules](https://checkstyle.org/checks.html).) That is why the `revisor`'s `main` has a Javadoc, and why every **public** class and method has one; the other classes are package-private and do not require it, which is one of the reasons for keeping everything closed (lesson 6).

There is a second temptation worth naming and rejecting: **switching the rule off**. All these tools offer ways of going quiet (`@SuppressWarnings`, exclusion files, lowering the severity). Use them only when the finding is false and you can say why; never to make the build pass. A `verify` that is green because you silenced it is worth no more than one that did not run.

And a warning about SpotBugs's silence, measured. In lesson 4 you put `List.copyOf` in `Reporte`'s constructor so the *record* would have its own copy of the list, and the same lesson measured that SpotBugs is silent about it in a package-private class. Here you can see why: **it depends on visibility.** If I remove that line from `Reporte`, which is a package-private class (no `public`), SpotBugs **says nothing**: `BugInstance size is 0`. If I make `Reporte` public and remove the line again, then it does:

```text
[ERROR] Medium: mx.habil.revisor.Reporte.resultados() may expose internal representation by returning Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 6] EI_EXPOSE_REP
[ERROR] Medium: new mx.habil.revisor.Reporte(List, Resumen) may expose internal representation by storing an externally mutable object into Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 8] EI_EXPOSE_REP2
```

SpotBugs cares about what is **visible from outside the package**, and a package-private class is not visible. What this teaches is twofold. The defensive copy is still correct even though the tool does not demand it (it would demand it the day `Reporte` became public, and by then it would be too late to think about it). And a tool's silence is information about what it **checked**, not a guarantee about everything else.

*(To learn more about how these gates fit into a delivery flow, there are two blog articles: [tests that catch errors](https://www.habil.mx/en/blog/tests-that-catch-errors-coverage-quality-gate/) and [CI/CD and DevSecOps](https://www.habil.mx/en/blog/cicd-devsecops-own-infrastructure/).)*

### 10.5 Security review

The `revisor`'s security was not added at the end: it was built up where the risk appeared. This section gathers what already exists and reviews it against the complete attack surface, which is four things: **what comes in** (the configuration and the options), **where it goes out** (the requests it makes), **what it listens to** (the server) and **what it shows** (the report).

**1. What comes in: another person writes each service's address.** It is the risk from lesson 8, that of server-side request forgery (SSRF): if the `revisor` queried any address that appears in `servicios.json`, whoever edits that file would turn it into an intermediary to reach things they cannot reach but the `revisor` can (such as a cloud's metadata service, which lives at `169.254.169.254`). The defenses are three, all of an allow-list and none of a deny-list: only `http` and `https` schemes; only hosts from an **allowed list**; and no redirect followed. The [OWASP guide on SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) agrees: validate against what is allowed, not against what is known to be bad.

In lesson 8 the allowed list was a constant. Today it is **configurable**, with `--permitir h1,h2`, and that changes the question: who controls that option? It belongs to whoever **runs** the program, not to whoever edits the services file. That separation is the reason the list does not go inside `servicios.json`: a file cannot grant itself permission. If one day it occurs to you to read the allowed list from the same file it validates, or from the network, you have undone the defense. You can check it with the three files that come with the lesson: one with `ftp://`, one with `file:///etc/passwd` and one with `http://169.254.169.254/latest/meta-data/`. All three are rejected before a single request is made, with code 2 (see exercise 1).

**2. Where it goes out: redirects.** `HttpClient` has `followRedirects(HttpClient.Redirect.NEVER)` written in `Revision.nuevoCliente()`. It is the default value, and it is written out anyway on purpose: an allowed server could answer “I moved to another address” pointing to one that is not allowed, and following that redirect without checking it would evade the list. The `revisor` treats a `3xx` as just another response and classifies it; it does not follow it.

**3. What it listens to: only the local interface.** The server in 10.2 listens on `127.0.0.1`. There is a risk specific to the design that was already announced: each `GET /reporte` triggers a complete check, with one request per service. A malicious local process, or simply a careless one, that requests the report in a loop makes the `revisor` query all those services without rest. The immediate mitigation is the local interface, which reduces who can do it to programs on the same machine; the next ones (a cache of a few seconds, a limit of requests per second) are not built, and this lesson does not fake them: it is stated that they are missing, and it is the first place where further work would be due.

**4. What it shows: the report is public to whoever asks for it.** The JSON includes each service's `url` field, and `/reporte` hands it to whoever asks. That is why the `revisor` does not trust the file to come clean: an address with a user or password (`http://usuario:clave@...`) is rejected at load time, with code 2 and a message that does not repeat it, and one that carries a `token` in the query or a fragment comes out trimmed: `ReporteJson` writes only scheme, host, port and path (Lesson 8). It is the kind of decision that is made **before** the data exists. An internal error is handled too: it returns `error interno` and nothing from the exception.

**5. Native serialization, which is not used.** Java has an old mechanism for saving and recovering objects directly, `ObjectOutputStream` and `ObjectInputStream`. Reading objects with `ObjectInputStream` from a source you do not trust is one of the most serious and best-documented vulnerabilities of the platform, because the read can execute code chosen by whoever wrote the bytes ([OWASP explains it with examples](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html), and the JDK offers [filters](https://docs.oracle.com/en/java/javase/25/core/serialization-filtering1.html) since [JEP 290](https://openjdk.org/jeps/290) to mitigate it). The `revisor` never uses it, and its safety here does not come from the format being JSON (there are also JSON libraries that, badly configured, build whatever object the text asks for), but from three concrete decisions: it **does not use native serialization** anywhere; it **does not enable Jackson's polymorphic deserialization**, the option that lets the text itself say which class to create for each object; and it **validates the configuration field by field against the shape it expects** (lesson 8 reads it as a `JsonNode` tree and checks each value, without mapping it directly to objects). The course's rule is simple: **data from outside comes in as structured text and is validated by hand, never as objects.**

To go on with the topic, the blog article on [API security](https://www.habil.mx/en/blog/api-security-cto-identity-gateway-tokens/) covers identity and *tokens*, which is what would be needed if the server left the local machine.

### 10.6 The executable JAR

#### 10.6.1 The problem of the long *classpath*

Since lesson 8 you have been running the `revisor` with `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor`. It is a long line that forces you to know three things: where your classes are, where the libraries are, and which class has the `main`. And it fails in interesting, silent ways. If you leave out the libraries part, this is what you see:

```text
$ java -cp target/classes mx.habil.revisor.Revisor --config servicios-arriba.json
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
	at mx.habil.revisor.Opciones.leer(Opciones.java:32)
	at mx.habil.revisor.Revisor.ejecutar(Revisor.java:50)
	at mx.habil.revisor.Revisor.main(Revisor.java:44)
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

It reads from the bottom up: a Jackson class is not on the *classpath*, and the JVM discovers it exactly when it needs it. The solution is what you give to whoever receives the program: **a single file** with everything inside.

#### 10.6.2 What a JAR is, and what `shade` does

A **JAR** (*Java archive*) is a ZIP file with classes inside and a special file, `META-INF/MANIFEST.MF`, with metadata. If the manifest carries the line `Main-Class: mx.habil.revisor.Revisor`, `java -jar archivo.jar` knows which class to start. It is the [JAR specification](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html).

`maven-jar-plugin`, which has been running since lesson 6, generates the *project's* JAR: only your classes, with no libraries and no `Main-Class`. That is why, if you run it, this happens:

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

(In English, with the JVM in that language: `no main manifest attribute, in target/revisor-1.0.0.jar`.) It is the “JAR with no entry point” error: your JAR exists and is fine, but it does not say where to start. And it has 32 entries: your classes and little else.

What is needed is [`maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/), which runs afterwards and produces **another** JAR that contains your classes *and* those of all the dependencies, with a manifest that does carry `Main-Class`. In the `pom.xml` from 10.4 it is three lines of configuration: `finalName` makes the file be named `revisor.jar` (without the version, so the usage command does not change with every version); the `ManifestResourceTransformer` with `mainClass` writes the `Main-Class`; and `createDependencyReducedPom` set to `false` keeps the *plugin* from rewriting the project's `pom.xml`, a side effect we do not need.

With it, `mvn -B verify` leaves the complete JAR in `target/`. These are the figures I measured:

```text
$ unzip -p target/revisor.jar META-INF/MANIFEST.MF | grep Main
Main-Class: mx.habil.revisor.Revisor
$ unzip -l target/revisor.jar | tail -1
  6359578                     1337 files
$ unzip -p target/revisor.jar revisor.properties
version=1.0.0
```

1,337 entries and about 2.6 MB, of which 1,176 are under `tools/jackson`. And the version comes from the `pom.xml`, as set up in 10.3.3.

During the build, `shade` prints warnings that are alarming at first sight: `Discovered module-info.class. Shading will break its strong encapsulation.` and `define 2 overlapping resources: META-INF/LICENSE, META-INF/NOTICE`. Both are true and here harmless: Jackson publishes a module description that, once everything is gathered into one JAR, stops being used (we do not use the module system, and it is not taught in this course), and several JARs carry their own license file with the same name, of which `shade` keeps one. When you do not understand a warning, do not ignore it out of habit: read it, and decide with the detail. This one can be left.

#### 10.6.3 The test that counts: another folder

A JAR is only independent if you can run it **from any folder**. Try this, with the full path of the JAR and of the configuration file:

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

It works, and it exits with 1 because there is one slow service and one down. Notice also what happens **without** `--config`: `servicios.json` is a relative path, and from `/tmp` it does not exist, so the program says so with its error and its code:

```text
$ java -jar ~/curso-java/revisor/target/revisor.jar
error de configuración: no existe el archivo servicios.json
$ echo $?
2
```

It is the error that lesson 5 announced, that of the relative path that depends on where you are standing, now with its exit code. It is a correct code 2: it is not a service that failed, it is incorrect usage.

### 10.7 What is coming: `jpackage`

A JAR solves the delivery of the *program*, not of the environment: whoever receives it still needs Java installed. For those who do not have it, the JDK ships two tools that remain a curiosity and are not taught: [`jlink`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jlink.html) builds a trimmed-down version of the JVM with only the modules your program needs, and [`jpackage`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html) packages your JAR together with that JVM into a native installer (a `.deb` for your Mint, for example) that asks for nothing beforehand. They are the natural next step when you deliver the program to people who are not programmers. Another, different way out is to compile the program to native code, which has costs and advantages of its own; the article [when it makes sense to compile Java to native](https://www.habil.mx/en/blog/native-java-when-to-use-it-when-not/) discusses them.

## The error you will see

The lesson's error is the one for the JAR with no entry point, which you just saw in 10.6.2:

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

It means the JAR does not declare which class to start: you ran the project's thin JAR instead of the complete one (`revisor.jar`). It is fixed by running the other one, or by adding the `shade` configuration to the `pom.xml`. The other messages worth recognizing at a glance:

- `error de uso: --concurrencia debe ser un entero mayor o igual que 1, no 0` (usage error: --concurrencia must be an integer greater than or equal to 1, not 0) and code 2: a badly written option; it is the `revisor`'s expected output, not a failure.
- `error de configuración: no existe el archivo servicios.json` (configuration error: the file servicios.json does not exist) and code 2: you ran from another folder without `--config` (10.6.3).
- `no se pudo abrir el puerto: Address already in use` and code 2: something is already listening on that port, perhaps another `revisor` you did not shut down. I confirmed it by starting two with `--servir 8090`.
- `NoClassDefFoundError: tools/jackson/core/JacksonException`: Jackson is missing from the *classpath*; use the complete JAR (10.6.1).
- Checkstyle's `MissingJavadocMethod` and SpotBugs's `EI_EXPOSE_REP`: the quality gates of 10.4; the message carries the rule and the line.

## What gets done wrong

**Calling `System.exit` in the middle of the program.** It kills the JVM without anyone being able to clean anything up and makes what surrounds it impossible to test. It is called once, in `main`, with a number calculated by other code (10.1.2).

**Mixing the result with the notices.** If the JSON and the errors go to the same output, whoever redirects the JSON to a file will find an error message in the middle. The result goes to the standard output, the notices to the error output.

**An exit code that distinguishes nothing.** Always ending with 0 (or always with 1) takes away the program's usefulness in a script. Three codes with fixed, documented meanings are worth more than a clever workaround.

**Listening on all interfaces by oversight.** `new InetSocketAddress(puerto)` without an address exposes the service to the whole network. The address is written out, always (10.2.2).

**A shutdown hook that is long or that calls `System.exit`.** It ends up waiting for itself. A hook does one short thing and finishes (10.3.1).

**Returning an exception's message over the network.** That is handing over paths, classes and sometimes data. A generic text is returned and the detail goes to a log (10.2.3).

**Silencing a rule to make `verify` pass.** Static analysis is worth something as long as it is heeded. Silence it only when the finding is false and you can explain why (10.4.4).

**Not pinning a *plugin*'s version.** One that carries none is at the mercy of whatever Maven brings that day; two machines can build different things. All the `pom.xml`'s *plugins* carry a version.

**Packaging without testing from another folder.** That the JAR runs in the project folder does not prove it is independent (10.6.3).

## Exercises

### Exercise 1 — The course's six checks

The syllabus's “How you know you are done” list is six conditions. Check them **one by one**, with the lab on, writing down the command and what you saw:

1. `mvn -B verify` ends green and the report says how many tests ran.
2. It checks 20 services at once in a time close to that of the slowest, not to the sum (use `servicios-20.json` and `--concurrencia`).
3. A service that is down, slow or answers garbage does not bring down the report (use `servicios-extra.json`, which adds `facturacion`, `notificaciones` and `basura`).
4. No request can wait forever.
5. It rejects addresses that are not `http` or `https` and those that are not in the allowed list.
6. It runs with `java -jar` from another folder and exits with 0 if everything is up and with 1 if not.

Before running each one, write down what you expect to see. Besides `servicios-extra.json` (10.3.3) and `servicios-20.json` (lesson 9), you need four small files in the project folder. The first has two services that are indeed up:

```json fig10_04/servicios-arriba.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"}
  ]
}
```

And these three are addresses the `revisor` must never query: a scheme that is not the web's, a local file, and the address of a cloud's metadata service:

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

### Exercise 2 — Keep `notificaciones` from overflowing the column (optional)

Look at the table for `servicios-extra.json`: the name `notificaciones` has 14 characters and the SERVICIO column is 12 wide, so the name runs into the status. Change `Formato` so that the column width is calculated from the longest name, with 12 as a minimum, and so that the table for the four canonical services comes out **identical** to the usual one.

### Exercise 3 — Open each gate on purpose

Break, one by one, these three components of quality, **predicting beforehand** what the build will say: (a) remove the Javadoc comment from `main`; (b) remove the Checkstyle 14.3.0 dependency from the `pom.xml`; (c) remove the line `resultados = List.copyOf(resultados);` from `Reporte`. In each case write down which tool spoke, what it said, and whether it surprised you by not speaking.

### Exercise 4 — Shut the server down in two ways

Start the `revisor` with `--servir 8090`. Shut it down first with `Ctrl`+`C` and read the exit code; start it again and shut it down now from another terminal with `kill -TERM`. Explain the two numbers and check that the port was left free.

## Solutions

### Solution 1

**1. `mvn -B verify` green, with the count read from the report.** What matters are the numbers, not the exit code (that is why they are counted twice, with two methods):

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

Seven test classes, 34 tests, none skipped.

**2. Twenty services, in series versus all at once.** The twenty each take half a second. With `--concurrencia 1` (equivalent to doing them in series), with 10 and with 20, measuring the whole program with `time`:

```text
$ TIMEFORMAT='%R s'
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 1 > /dev/null
10.398 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 10 > /dev/null
1.245 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 20 > /dev/null
0.738 s
```

(Second runs: 10.385, 1.253 and 0.747.) The sum of the times is 10 s; with the limit at 20, the program takes as long as the slowest one, plus about 0.2 s of JVM startup. What the syllabus asks for, “close to that of the slowest, not to the sum”, is met: 0.74 s against 10.4 s.

**3. What fails does not bring down the report.** With the three extra services, the table comes out complete and in the same order; `facturacion` answers 503, `notificaciones` times out at its 1000 ms limit (the lab's path takes 10 seconds) and `basura` closes the connection without answering:

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

(The name `notificaciones` overflows the column: exercise 2 fixes that.) To see that the **order** does not change even with different concurrency, the names are extracted from the JSON with three different limits:

```text
$ for c in 1 3 10; do java -jar target/revisor.jar --config servicios-extra.json --concurrencia $c --formato json | grep -o '"nombre" : "[a-z]*"' | cut -d'"' -f4 | tr '\n' ' '; echo; done
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
```

(The TIEMPO column's times change on every run, and that is why this table's average is not the 656 that would come out with lesson 4's fixed numbers: it is that of the measured times.)

**4. No request waits forever.** It is checked in two ways. By the code: there is only one place where the `revisor` sends a request, and that place sets both times.

```text
$ grep -n "connectTimeout\|\.timeout(" src/main/java/mx/habil/revisor/Revision.java
31:        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
39:            .timeout(Duration.ofMillis(servicio.timeoutMs()))
$ grep -rn "\.send(" src/main
src/main/java/mx/habil/revisor/Revision.java:44:      var respuesta = cliente.send(peticion, BodyHandlers.discarding());
```

And by the clock: the lab's `/notificaciones` path takes 10 seconds to answer, and even so the complete report with the seven services finished in **2.5 seconds** (`time` gave 2.525 s): the service's 1000 ms limit cut it off, and the slowest of the others, `inventario`, sets the rest.

**5. It rejects what is not `http` or not allowed.** Three files, three rejections, none makes a single request, and all three with code 2:

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

**6. From another folder, with both codes.** You already saw the code 1 case in 10.6.3. For 0 you need a configuration where everything is up (`servicios-arriba.json`, with `catalogo` and `pagos`):

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

All six are met. Write the date and the test count from the first point in your logbook: it is the proof that you finished.

### Solution 2

The pattern is separated from the width format, and `tabla` calculates the width from the longest name (plus two separator spaces), with 12 as a minimum. With the four canonical services, the longest name is 10 long and the calculation gives 12: the table does not change. These are the parts of `Formato.java` that change (the `encabezado()` and `linea(Resultado)` functions without a width are replaced by the ones below, which receive it):

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

With that change the 34 tests are still green, Checkstyle gives 0 violations and SpotBugs 0 defects, and the `servicios-extra.json` table ends up aligned:

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

(a) Without the Javadoc on `main`, **Checkstyle** speaks: `MissingJavadocMethod: Falta el comentario Javadoc para 'main'.`, one violation, and `verify` fails. It is what is expected: `main` is public.

(b) Without the Checkstyle 14.3.0 dependency, **Checkstyle speaks, but with a parsing error and not a style one**: `mismatched input 'when' expecting '->'` in `Clasificador.java`. This is the one that surprises most, because it mentions no version and points to correct code (10.4.2).

(c) Without the defensive copy, **no one speaks**: `BugInstance size is 0`. It is the surprise that was announced: `Reporte` is a package-private class and SpotBugs only complains about what is visible from outside. The copy must stay there; the tool is silent, but the reason it exists has not changed. If you make `Reporte` public (with its constructor), then `EI_EXPOSE_REP` and `EI_EXPOSE_REP2` do appear (10.4.4).

### Solution 4

With `Ctrl`+`C`, `servidor detenido` appears and the code is **130**; with `kill -TERM` (for example, `kill -TERM $(pgrep -f 'revisor.jar --servir')` from another terminal), `servidor detenido` also appears, and the code is **143**. The two numbers are `128 + the signal` (2 and 15), the terminal's convention for processes that die from a signal; they are not failures. After shutting it down, on your Mint `ss -ltn 'sport = :8090'` must show no line: the graceful shutdown freed the port. (I measured the codes by sending the signals from a script; I did not run the `ss` command on the measuring machine.)

## How I know I got it

- [ ] `java fig10_01.java --falla` prints the usage error and `echo $?` says 2, and I can say why the error is written with `System.err`.
- [ ] `java fig10_02.java` and `java fig10_03.java` end with the lesson's outputs, and I can explain what each of the five steps of the minimal server is for and when a shutdown hook runs.
- [ ] **(1)** `mvn -B verify` says `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`, `You have 0 Checkstyle violations` and `BugInstance size is 0`, and the test count tallied in the `surefire-reports` XML files also gives 34.
- [ ] **(2)** With `servicios-20.json`, `--concurrencia 1` takes about 10 s, `--concurrencia 10` about 1.2 s and `--concurrencia 20` about 0.7 s (more or less, depending on my machine), and I measured them myself with `time`.
- [ ] **(3)** With `servicios-extra.json` the table comes out complete, with the seven names in the same order with any `--concurrencia`, and the exit code is 1.
- [ ] **(4)** I can point out in `Revision.java` the two lines that set the connection timeout and the request timeout, and I checked that the report with `notificaciones` (a 10 s path) finishes in about 2.5 s.
- [ ] **(5)** The three files `servicios-ftp.json`, `servicios-archivo.json` and `servicios-nube.json` are rejected with the expected message and code 2.
- [ ] **(6)** `java -jar` from `/tmp` or from my home folder, with full paths, prints the table and exits with 1; with `servicios-arriba.json`, it exits with 0.
- [ ] I tried at least one of the quality gates in red and understood the message (Solution 3), including SpotBugs's silence about `Reporte` without the copy.
- [ ] I wrote down in my logbook which Checkstyle or SpotBugs finding I did not expect.

## Summary

Answer without looking at the lesson:

1. What does `main` receive in `args`, and what does Java *not* do with it?
2. What does an exit code `0` say by convention, and what do the `revisor`'s `1` and `2` distinguish between them?
3. Why is `main` a single line and the logic lives in `ejecutar`? What do the tests gain from that?
4. Why does the result go to the standard output and the notices to the error output?
5. What is the difference between listening on `127.0.0.1` and giving no address? What does port 0 mean?
6. What is a shutdown hook, when does it run and when does it not, and why must it not call `System.exit`?
7. Why does the main thread in server mode stay waiting on a `CountDownLatch` that nobody opens?
8. What does Checkstyle check and what does SpotBugs check? Why does SpotBugs's silence about a mutable collection in a package-private class not prove that it is fine?
9. Why does the allowed list not go inside the services file? Why are redirects not followed?
10. What does `maven-shade-plugin` add that `maven-jar-plugin` does not do, and how do you check that the JAR is independent?

## Further reading

- [Oracle, `com.sun.net.httpserver.HttpServer` (Java 25)](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html) and [`Runtime.addShutdownHook`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html): the JDK's server and the lifecycle of a shutdown. Consulted on October 7, 2026.
- [Checkstyle, “Google's Style”](https://checkstyle.org/google_style.html) and [SpotBugs, catalog of patterns](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html): what each rule checks and what each code means. Consulted on October 7, 2026.
- [Apache Maven, `maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/) and [the JAR specification](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html): how a JAR with everything inside is built and what its manifest says. Consulted on October 7, 2026.
- [OWASP, “Server-Side Request Forgery Prevention”](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) and [“Deserialization”](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html): the two risks of the security review, with their defenses. Consulted on October 7, 2026.

---

**Previous:** [Lesson 9 — Concurrency: checking everything at once](09-concurrencia.md)
