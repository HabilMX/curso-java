# Lesson 8 — HTTP and JSON

**Time:** 2 × 90 min (two sessions; the break is between sections 8.3 and 8.4)

**What you build:** the `revisor` stops making up its answers: it queries each service over HTTP for real and reads its list from a `servicios.json` file that it validates before trusting it

**What you learn:** the JDK's `HttpClient` (one client, connection timeout and per-request timeout, redirects, status codes); how to turn every network failure into a short detail without letting an exception escape; JSON and Jackson 3 (`tools.jackson`), and what changes compared with almost everything you will find on the internet; reading JSON as a tree and validating it by hand; writing the report with *records*; and **SSRF: someone else writes the URL**

**Where you are coming from.** You bring the Maven project from [Lesson 7](07-pruebas-junit.md): packages, a `pom.xml`, 18 passing tests and a `Simulador` that plays the role of the network with four made-up responses. That invention is what ends today. A reviewer that never talks to the network reviews nothing; and a plain-text list of services, which cannot tell a number from a string, will not survive the first file written by someone other than you. This lesson solves both problems, and along the way teaches you the part of the job that is most underestimated: **what to do with what comes from outside**. You work in your `~/curso-java/revisor/` folder.

**What this lesson does not do.** It does not query the services at the same time: `revisarTodos` checks them one after another (serially), and that is why the report takes about 2.5 seconds, the sum of what each service takes; checking them all at once is [Lesson 9](09-concurrencia.md). Nor does it read the body of the responses (the `revisor` only cares about the code and how long it took), nor does it have a command line yet: the JSON report exists and is tested today, but it will only be requested with an option in [Lesson 10](10-el-programa-terminado.md).

**About the runs.** The services that get queried belong to the **lab** (8.1), a local practice server that always answers the same thing; that is why the ESTADO (STATUS) and the DETALLE (DETAIL) of every row are identical in every run. What changes between runs is the TIEMPO (TIME) column, because it is a time actually measured: if your table says `171ms` where this one says `180ms`, that is normal. Everything was run with Temurin 25, Maven 3.10.0, JUnit 6.1.3 and Jackson 3.2.3. The examples that use Jackson are run from the project root, with `java -cp "target/lib/*" fig08_06.java` (8.5 explains why), and the programs are in [`programas/08-http-y-json/`](../programas/08-http-y-json), in the course repository.

## By the end you will be able to

- Make a request with `HttpClient`, read the status code and measure how long the response took.
- Explain the difference between the connection timeout and a request's timeout, and why no request is ever sent without a timeout.
- Turn every network failure (cannot connect, timed out, connection closed) into a short detail, without a network exception escaping from `revisar`.
- Declare the Jackson 3 dependency, tell `tools.jackson` apart from `com.fasterxml.jackson`, and explain what the *classpath* is and what `target/lib` is for.
- Read JSON as a tree (`JsonNode`) and validate it by hand, and explain why mapping it directly to a *record* accepts `1.5` and `"1000"` as a timeout.
- Write the report as JSON with *records* and `snake_case` keys.
- Explain what SSRF is and apply the `revisor`'s defenses: allowed scheme, allowed list of hosts, and redirects not followed.

## The why before the how

Until now the `revisor` has been an actor. When it said that `inventario` took 2.3 seconds, it said so because you wrote it into the `Simulador`; there was nobody on the other side. Changing that is more than changing where a number comes from, because the real world has four habits that the simulator never had. **It takes as long as it likes:** a service can answer in 80 milliseconds or in ten seconds. **It does not always answer:** the port may be closed, the connection may be cut halfway, the server may accept it and then stay silent. **It answers strange things:** a service that is down often does not stop responding, it responds with a 503. And **sometimes it sends you elsewhere:** a response can say “what you are looking for is no longer here, go to this other address.” The `revisor` has to classify all of that, without falling over, and without waiting forever.

The second half of the lesson is a different kind of problem. Until now the list of services was a `servicios.txt`: name, address, an optional timeout, separated by spaces. It works as long as everything is text and everything is easy. But a text file cannot tell the number `1000` from the string `"1000"`, cannot nest anything, and has no standard way of saying “this field is missing.” That is what **JSON** is for, the most widely used data-interchange format on the internet, and Java does not ship it in its standard library: you use a library written by others, and that is why Lesson 6 existed.

And between the two halves there is an idea that joins them and is the most important one in the lesson: **your program now does something in the world based on what a file says.** If the file says “query this address,” the `revisor` queries it. Whoever can write to that file, or to the form that will someday feed it, controls where your program connects to from your network. That kind of flaw has a name of its own and is among the ten most serious in web applications: it is called **SSRF**, and the last section attacks it with four defenses.

The path has two sessions. The **first** covers 8.1 to 8.3: the lab, the `HttpClient`, timeouts and failures, and it ends with a `revisor` that queries for real. The **second** covers 8.4 to 8.9: JSON, Jackson 3, validating, writing and protecting. With that destination in mind, this is what the `revisor` prints at the end of the lesson, with the lab turned on:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

The ESTADO (STATUS) and the DETALLE (DETAIL) are the usual ones. The TIEMPO (TIME) column is no longer numbers written by you: it is what your computer measured.

## The concepts

### 8.1 The lab and the first request

#### 8.1.1 HTTP in five lines

**HTTP** is the language almost all internet services speak. A conversation is always the same: a **client** (your program) sends a **request** and a **server** sends a **response**. The request carries a **method** that says what is wanted (`GET` means “give me this”), a **path** that says what (`/pagos`) and some headers with extra data. The response carries a **status code**, which is a three-digit number with the result; some headers; and, almost always, a **body** (the page, the data, the text “ok”).

Of the status code the `revisor` only needs the first digit, which is a family, according to the [HTTP specification](https://www.rfc-editor.org/rfc/rfc9110): `2xx` is success (`200` is “here it is”), `3xx` is redirection (“look somewhere else”), `4xx` is an error by whoever is asking (`404` is “that does not exist”), and `5xx` is a server error (`503` is “right now I cannot serve you”). The `revisor`'s classification rule that you already have, “a code of 400 or more leaves the service down,” uses exactly that division: answering `503` is being down even if the answer comes fast.

This is what actually travels. With `curl`, the terminal program that makes a request and shows the response, the `-i` option lets you see the code and the headers before the body (the date changes on every run):

```text
$ curl -si http://127.0.0.1:8089/pagos
HTTP/1.1 200 OK
Date: Thu, 08 Oct 2026 03:42:51 GMT
Content-length: 3

ok
```

First line: `HTTP/1.1 200 OK`, the protocol version, the code and its name. Then the headers, one per line, and a blank line; and after that the body, which here is the text `ok`. There is nothing else. The two things the `revisor` cares about are the `200` and how long it took to arrive.

#### 8.1.2 The lab

To practice with a network there are two paths, and both have a problem. Querying public internet services makes every run different (today they answer, tomorrow they do not, and their times depend on your connection), and it is not polite to practice against other people's servers. Writing a server of your own requires concepts you do not have yet. This course's way out is a **lab**: a small program that starts an HTTP server on your own computer (`127.0.0.1`, which is the name for “this very computer”), with services that always answer the same thing and always take the same time. That way the results can be compared with those in this lesson, line by line.

For now the lab is a **black box**: you are going to run it, not write it. It uses tools that are not explained yet (an HTTP server that ships in the JDK itself and one thread per request), and it is an exception to the rule of not using anything before explaining it (there is another small one in 8.3.4); [Lesson 10](10-el-programa-terminado.md) opens it up and really uses it. Save it as `~/curso-java/Laboratorio.java`, **one level above the project folder**, because it is not part of it:

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

And run it **in another terminal**, which you leave open while you practice. It keeps running, waiting for requests, until you stop it with `Ctrl`+`C`:

```text
$ java Laboratorio.java
laboratorio escuchando en http://127.0.0.1:8089
```

If instead you see `java.net.BindException: Address already in use`, it means another program on your computer is already using port 8089 (a **port** is the number of the “door” a server listens on). What frees the port is **stopping the process** that occupies it, not opening another terminal: it is almost always a previous lab that is still running, and you stop it with `Ctrl`+`C` in the terminal where you started it. If you cannot find that terminal, `ss -ltnp 'sport = :8089'` shows which process is listening on 8089 (with its name and its process number), and with that number you stop it. These are its paths:

| Path | What it answers | And what the `revisor` will have to conclude |
|---|---|---|
| `/catalogo` | `200` after 142 ms | ARRIBA (UP) |
| `/inventario` | `200` after 2300 ms | LENTO (SLOW) |
| `/pagos` | `200` after 87 ms | ARRIBA |
| `/facturacion` | `503` after 95 ms | CAIDO (DOWN) (`503`) |
| `/notificaciones` | `200`, but after 10 seconds | CAIDO if it is given a timeout of 1 s |
| `/basura` | closes the connection without answering | CAIDO (`error de red`, “network error”) |
| `/espera/<ms>` | `200` after `<ms>` milliseconds | depends on the time |

One service of the usual four is missing, and it is not in the lab on purpose: `reportes` points to `http://127.0.0.1:8088/`, where **nobody is listening**, so there is a service whose connection is refused. The four in the usual tables are `catalogo`, `inventario`, `pagos` and `reportes`; the others are for exercises and tests.

#### 8.1.3 The first request

With the lab on, this is the shortest request you can make with Java 25:

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

Read it from top to bottom. **`HttpClient`** is the object that knows how to speak HTTP; it is in the JDK's `java.net.http` package and needs no dependency. It is opened with `try (...)`, the `try-with-resources` from Lesson 5: an `HttpClient` holds connections and internal work, and closing it releases them; it has been closeable (`AutoCloseable`) since Java 21, according to its [documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html). **`HttpRequest.newBuilder(URI.create(...)).build()`** builds the request: an object that no longer changes, with the address it goes to. If you do not say otherwise, the method is `GET`. **`cliente.send(peticion, BodyHandlers.ofString())`** sends it and **waits** for the response; until it arrives, the program does not move on. The second parameter says what to do with the body: `ofString()` turns it into a string, and the result is an `HttpResponse<String>`, the response, from which you ask for the `statusCode()` and the `body()`.

To measure how long it took, you read the clock before and after `send`, with `System.nanoTime()`, and subtract. `nanoTime` is used and not `currentTimeMillis` because the `nanoTime` [documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/System.html) says it bluntly: it is for measuring **intervals** of time and has no relation to the time of day. A clock's time can jump (because of an automatic adjustment, because of daylight saving time) in the middle of a measurement, and the interval would come out negative or enormous. `nanoTime` returns nanoseconds, and `/ 1_000_000` turns them into milliseconds; the underscore in the number is a separator that Java ignores, so that it is easier on the eye.

```text
$ java fig08_02.java
código de estado: 200
cuerpo: ok
tardó al menos 87 ms: true
```

The `200` code, the `ok` body that the lab sends, and a check: the time was at least the 87 milliseconds that `/pagos` makes you wait. (The check is printed and not the number because the number changes on every run.) `throws Exception` on `main` is a shortcut of this example, which lets any exception through; in the project each one will get its own handling.

### 8.2 The `revisor`'s `HttpClient`

#### 8.2.1 One client, many requests

An `HttpClient` is not just the `send` function: it is an object that is built once and keeps things worth reusing. The documentation says so: once built, it **is immutable** and can be used to send many requests, and it shares resources among all those that go through it (the already-open connections, for example, which do not need to be negotiated again). That is why the `revisor` creates **one client per review** and uses it for all the services, not one per service. And it is created with the *builder* when you want to configure something that `newHttpClient()` does not configure, which is the case here: a connection timeout and an explicit redirect policy are needed.

```java
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }
```

Two settings, and both are decisions, not decorations. `connectTimeout` sets how long to wait to **establish the connection** (more in 8.3). `followRedirects(HttpClient.Redirect.NEVER)` says “if the server responds with a redirect, do not follow it”; it is the value the client already has by default, and it is written anyway, **explicitly**, because it is a security decision (8.8) and a decision someone wrote down is a decision someone can review. The default value, according to the documentation, is `NEVER`; leaving it implicit means that the day someone changes it, nobody will notice.

#### 8.2.2 What to do with the body: `BodyHandlers.discarding()`

`send` needs to know what to do with the response body, and it asks with a second parameter, the **body handler**. In the previous example `ofString()` was used, which reads the whole body and hands it over as a string. The `revisor` does not need the body: only the code and the time. For that there is `BodyHandlers.discarding()`: it reads the body and **throws it away**, without keeping it. There is a practical reason behind it: if a service responds with a hundred-megabyte body, `ofString()` would load all of it into your program's memory; `discarding()` reads it and stores nothing. The response type changes accordingly: `HttpResponse<Void>`, a response whose body is “nothing” (`Void`, with a capital letter, is a JDK class that has no possible value except `null`; it is used precisely in generics, where a type parameter has to be a class and cannot be the word `void`, to say “there is nothing here”).

### 8.3 When something fails: timeouts, exceptions and redirects

#### 8.3.1 Two timeouts, and why no request is sent without them

This is the section that most separates a practice program from one you can leave running. The [`HttpRequest.Builder` documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html) says this about a request's timeout: “the effect of not setting a timeout is the same as setting an infinite Duration, i.e. **block forever**.” If the server accepts the connection and goes silent, your program stays waiting, today, tomorrow and next year. A `revisor` that checks services to find out whether they are down and that hangs because of one of them is worse than having no `revisor`.

That is why there are **two** timeouts, and each one covers a different stage:

| Timeout | Where it is configured | What it covers | What it throws when it runs out |
|---|---|---|---|
| **Connection** | on the client (`connectTimeout`) | establishing the connection with the server | `HttpConnectTimeoutException` |
| **Request** | on each request (`timeout`) | waiting for the response to arrive | `HttpTimeoutException` |

The connection is the stage of “does anyone answer at that address?”, and the `revisor` gives it two seconds (`TIEMPO_CONEXION_MS = 2000`, the constant in `Revision`). That is a decision of the program, not a law of the network: on a slow or congested network, a connection could be established later. What the constant says is “if there is no connection within two seconds, the `revisor` stops waiting and reports `sin conexión`”, which for services that live nearby, like those of the lab, is a generous margin. The request is the stage of “they already picked up, when will they respond?”, and its time **belongs to the service**: it comes from `servicio.timeoutMs()`, which is 5000 by default and which the configuration file can change for a service known to be slow. A client's connection timeout [only has an effect when a new connection has to be opened](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.Builder.html); if one is already open from a previous request, it is reused and the timeout does not apply.

#### 8.3.2 Which exceptions arrive, and which detail they get

`send` declares two checked exceptions: `InterruptedException`, which you already know from Lesson 5, and `IOException`, which groups **everything that can go wrong on the network**. To see which concrete type arrives in each case, this program makes four requests to lab paths and writes the exception's class and its message:

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

Four paths, four different results, and a lesson in each one. `/facturacion` **did answer**: the response is a `503`, and there is no exception: a server that is down often does not look it, because it responds that it cannot. Port 8088, where nobody listens, gave `ConnectException` (“could not connect”) and its message is **`null`**: the most common network exception does not carry a useful text. `/notificaciones` makes the client wait 10 seconds and the timeout was 1: `HttpTimeoutException`. And `/basura` opens the connection and closes it without saying anything: `IOException`, plain, with a message that talks about the client's internals (`HTTP/1.1 header parser received no bytes`).

Two rules come out of this. The first, that the `revisor` translates each type into **one of three fixed details** and never shows the exception's message, for two reasons: sometimes it is `null`, and when it exists it changes between JDK versions and between operating systems. The translation table is the one you have known since the first lessons:

| The exception | The detail | What happened |
|---|---|---|
| `HttpTimeoutException` (includes `HttpConnectTimeoutException`) | `tiempo agotado` (timed out) | time ran out, connection or request |
| `ConnectException` | `sin conexión` (no connection) | closed port, or a name that does not exist |
| any other `IOException` | `error de red` (network error) | the connection was cut or the response was not understood |

The second rule is about **order**. `HttpTimeoutException` and `ConnectException` are both kinds of `IOException`: they are particular cases of it. When an exception can be caught by its particular type or by the general one, Java uses the first `catch` that fits, so **the particular type goes before the general one**. If you put them the other way around, the compiler will not let you:

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

`exception ConnectException has already been caught`: the `catch (IOException)` above already catches all the `ConnectException`s, so the one below would never run. That is why you always order from the particular to the general. With this table and this rule, the `revisor`'s central method looks like this:

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

Read it in stages. The request is built, with the **service's** timeout (`timeout`). The clock is read. It is sent; if there was a response, the time is measured and a `Respondio` is built with the code and the milliseconds (the `/ 1_000_000` converts the nanoseconds). And every failure becomes a `SinRespuesta` with its detail. At the end, the status is not decided here: the response, whichever it is, is handed to Lesson 3's `Clasificador`, which knows nothing about networks. Two properties matter more than the rest. The first: **`revisar` lets no `IOException` escape**; a network failure is a result, not a program error (the same idea Lesson 5 called “result versus exception”). The second: the `InterruptedException` **does** go up, as you learned, because whoever interrupts the thread has the right to have the program stop.

#### 8.3.3 Redirects: `3xx` is a response

One of the four habits of the real world is still missing. Sometimes a server responds with a `3xx` code and a `Location` header that says “I'm now at this other address.” A browser follows it without you noticing. An HTTP client can do the same if asked. This program starts its own server, on a free port that the system picks, with one path that redirects to another, and queries it with two clients:

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

The default client did **not** follow the redirect: it answered `302`, the code of the response it received. The one configured with `Redirect.NORMAL` went to the new address and delivered the final `200`. The `revisor` is of the first kind, and the classification rule says so unambiguously: a `3xx` is a **response**, it is not followed, and it is not an error. A service that redirects is alive. There is also a more serious security reason, explained in 8.8: an address the file allowed has no reason to lead to one the file did not allow.

#### 8.3.4 `revisarTodos` and the test against a real server

With `revisar` done, `revisarTodos` becomes short: one client, a `for`, the order of the list.

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

It is **serial** on purpose: each service is checked when the previous one has finished. The reason for the `try (HttpClient cliente = nuevoCliente())` around it is the shutdown: when the block ends, `close()` waits until no requests are in progress and releases everything. And the cost of the series is visible, and it is the subject of the next lesson: 142 + 2300 + 87 milliseconds is, roughly, what the report takes.

`revisar` still needs to be watched over with tests. Here there is a difficulty that Lesson 7 did not have: the test needs **a server that behaves the way we want** (that answers fast, that fails, that stays silent), and it cannot depend on the lab, which may be turned off on another computer. The solution is for the test to start its own, with the same JDK `HttpServer` as the lab, **inside the test**, on a port the system picks:

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

Three new ideas. **Port 0.** Asking for `InetSocketAddress("127.0.0.1", 0)` means “any free port”; the system picks one, and the test reads it afterwards with `servidor.getAddress().getPort()`. That way it never collides with another program. **`@BeforeAll` and `@AfterAll`** are JUnit annotations for code that runs **once** before all the tests of the class and once after all of them; starting and stopping the server in every test would be slow. The methods are `static` because of a JUnit rule. And **five tests, one per path**: a fast response (ARRIBA), a `503` (CAIDO), a closed port (`sin conexión`), a path that takes 2 seconds with a timeout of 200 milliseconds (`tiempo agotado`), and a connection that closes without answering (`error de red`). The closed port is obtained in a clever way: a socket is opened on port 0, the port it got is noted down, and it is closed; that number we then know is free. (The LENTO case is not tested here; `ClasificadorTest` already watches it, and really waiting a second in a test would make it slow without teaching anything new.) One line of the server deserves a note: `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` asks for each request to be served on its own thread, so that the slow path does not block the others; the concept is explained in Lesson 9, and for now it is enough to know that without that line the paths would be served one by one.

As always, **first you see it red**. In `Revision.java`, remove the `.timeout(...)` line from the request and run `mvn -B verify`:

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

Without the timeout, the `/lento` path makes the client wait its full 2 seconds, the response arrives (a `200`, late) and it is classified LENTO, instead of CAIDO. The test expects `CAIDO`, and it turns red with a message that says exactly what is wrong. It is the same failure that, in production, would not happen in a quick test but with a real service hanging, and it would look like a report that never finishes. Restore the line.

#### 8.3.5 The end of the first session

You have everything for the `revisor` to query for real. In your folder, change two things: delete `Simulador.java` (the network does its job), and make the addresses in `servicios.txt` point to the lab (`http://127.0.0.1:8089/catalogo`, and so on with the others; `reportes` goes to `http://127.0.0.1:8088/`). Copy `Revision.java` and `RevisionTest.java` from above. With the lab on in another terminal:

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

That makes 23 tests: the 18 from the previous lesson plus the five new ones in `RevisionTest`. The `ConfiguracionTest` ones are still those for the text file; in the second session they are rewritten. And the `revisor` now queries for real:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      92ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 861ms
```

Two things to notice. The ESTADO (STATUS) and the DETALLE (DETAIL) are identical to those of the previous lesson, and the TIEMPO (TIME) column is no longer the one you wrote: your computer measured it. And the program took, roughly, the sum of all of them (a little over 2.5 seconds), not the slowest one: that is the cost of checking in series. There is one more proof that it queries for real: turn off the lab and run again; the four services come out CAIDO with `sin conexión`, and the average is a dash, because nobody responded. This closes the first session.


### 8.4 JSON: why plain text is no longer enough

#### 8.4.1 What a text file cannot say

The `servicios.txt` from Lesson 5 had one service per line, with fields separated by spaces, and the third, optional one, was the timeout. For four services it is convenient. But ask it three questions. How does it tell the number `1000` from the string `"1000"`? It does not: they are the same four characters, and the program converts them because it *knows* a number goes there. How does it add a field that some services have and others do not, in the middle? It cannot, without breaking the position of the others. How does it say that a service has a list of things, like tags or several owners? It cannot either. Every time the file needs to say something new, you have to invent a convention and rewrite the reader.

**JSON** (*JavaScript Object Notation*, although today every language uses it) solves that with a simple idea: a text format that **carries its types written out**. According to its [standard, RFC 8259](https://www.rfc-editor.org/rfc/rfc8259), a JSON value is one of these things: an **object** (`{"nombre": "pagos"}`, key-value pairs between braces), an **array** (`[1, 2, 3]`, a list between square brackets), a **string** between double quotes, a **number**, or one of three fixed values: `true`, `false` and `null` (“there is no value”). That is the whole format, and that is why it can be read from any language. And notice what changed: `1000` (without quotes) is a number and `"1000"` (with quotes) is a string, and the format tells them apart from the start.

This is the `revisor`'s configuration file from today on, the `servicios.json`, with the same four usual services now pointing to the lab:

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

It is an object with one key, `servicios`, whose value is an array of objects, and each object carries `nombre` and `url`. There is a third possible field, `timeout_ms`, the request's timeout in milliseconds: it is **optional**, and whoever omits it gets the usual 5000 milliseconds. The keys are in `snake_case` (lowercase words joined by an underscore, as in `timeout_ms`) because they are the same ones used by the [Python course](https://github.com/HabilMX/curso-python): the file and the report look the same in both. With this file, **`servicios.txt` is deleted**: it is no longer used.

Finally, JSON also has a place in the `revisor`'s **output**: the report the program will deliver so that another program can read it (the course fixes it and it does not change from here on). You will see it in 8.7.

### 8.5 Jackson 3: the dependency, the packages and the *classpath*

#### 8.5.1 What Jackson is, and why there are two

Java does not ship a JSON reader in its standard library; you use a library, and the most widely used one is **Jackson**. Here there is a trap you pay for once: Jackson had a version 2, which dominates almost everything written on the internet (old answers, tutorials, books), and since 2025 it has a **version 3**, which changed its package name. Version 2 lives in `com.fasterxml.jackson...`; version 3, in **`tools.jackson...`**. If you copy an example from the internet and its `import` starts with `com.fasterxml.jackson.databind`, it is from version 2, and it does not compile with this course's project. You can see it in two lines:

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

`package com.fasterxml.jackson.databind does not exist`, and behind it a cascade of `cannot find symbol` that is nothing more than the consequence of the first. The message is the usual one when a dependency is missing (you saw it in Lesson 6), but here the Jackson dependency **is** declared; what does not exist in your project is version 2. When a Jackson example does not compile, check first which package it imports. The [official Jackson 3 migration guide](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) lists the changes: the new package, the minimum JDK (Java 17), immutable mappers built with a *builder*, and that exceptions are no longer checked. One detail remains half-way: the **annotations** (`@JsonProperty` and company) are still in `com.fasterxml.jackson.annotation`, with version 2.x; that is why further down you see a `jackson-annotations-2.22.jar` file next to those of version 3. The course uses 3.2.3, the latest as of October 2026.

#### 8.5.2 Declaring the dependency

A **dependency** is a library that your project uses and you did not write. It is declared in the `pom.xml` with its coordinates, just as you declared JUnit, but with two differences: without a scope (the default scope is `compile`, “the program needs it to compile and to run”) and with the version written out. This is the project's `pom.xml` for the lesson, complete; what is new is the Jackson dependency and a second *plugin*, which is explained right away:

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

The dependency, `tools.jackson.core:jackson-databind`, brings others along on its own: Maven reads Jackson's `pom.xml`, sees that it needs `jackson-core` and `jackson-annotations`, and downloads those too. This is called **transitive dependencies**, and it is the reason why you write one line and three JARs get downloaded. The version went up to `0.8.0`, and JUnit's is still imported from the same BOM.

#### 8.5.3 The *classpath* and why `target/lib`

Compiling with Maven works, because Maven knows which dependencies there are and passes them to `javac`. But **running** is another matter, and it is where the lesson's most typical error appears. With the command from the previous lesson:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError: tools/jackson/core/JacksonException`. The JVM started running the program, reached a line that needs a Jackson class, and did not find it. Remember what `-cp` meant: the list of places where the JVM looks for classes, the ***classpath***. In Lessons 6 and 7, `target/classes` was enough, because all the code was yours. Now the Jackson JARs have to be added. Maven keeps them in `~/.m2/repository`, but that path is long and depends on the version; to avoid typing it, the `pom.xml` adds the `maven-dependency-plugin` *plugin*, which in the `package` phase **copies the program's dependencies to `target/lib`**:

```text
$ ls target/lib
jackson-annotations-2.22.jar
jackson-core-3.2.3.jar
jackson-databind-3.2.3.jar
```

And it is run with both things on the *classpath*, `target/classes` (your code) and `target/lib/*` (the JARs; the `*` asks for “all the `.jar` files in that folder”), separated by a colon, which is the separator on Linux:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
```

The quotes are so that the terminal does not try to expand the `*` itself. And since the JARs are copied in the `package` phase, you must have run `mvn -B verify` (or `package`) at least once for `target/lib` to exist. This is what is annoying about a hand-built *classpath*, and that is why [Lesson 10](10-el-programa-terminado.md) solves it with a single `.jar` that carries everything inside: you have already suffered it, so you will know which problem it solves.

### 8.6 Reading JSON as a tree and validating by hand

#### 8.6.1 `JsonNode`: the tree

Jackson offers two ways of reading JSON. The first turns it into a **tree** of `JsonNode` objects, one per value, which you can walk and query. The second maps it directly to a *record* of yours. Let us start with the first, which is the one the configuration uses:

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

The method names are those of Jackson 3 (and it is worth paying attention, because almost everything you find on the internet uses those of version 2): `readTree` turns the text into the tree and returns its root; `get("servicios")` asks for the value of an object's key, and `get(1)` for an array's element; `isArray()` asks what type of value it is; `size()` counts the elements; `stringValue()` returns the text of a string node and `intValue()` the integer of a numeric one. The old names (`asText`, `isTextual`) still exist for those who migrate, but new code uses `stringValue` and `isString`. And there is a detail that decides many errors: `get("timeout_ms")` on a key that **does not exist** returns Java's `null`, not a node; to ask without the risk of a `NullPointerException` there is `path("timeout_ms")`, which returns an “absent” node (`isMissingNode()`) instead of `null`. What the `revisor` does with that is always check first whether it is `null` and then ask the type.

#### 8.6.2 The trap: mapping directly to a *record* accepts what it should not

The second way is tempting, because it is two lines and Java's types take care of everything: you declare a *record* with the fields you want and Jackson fills it in.

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

Look at the five rows. `1000` comes in as 1000, fine. But **`1.5` comes in as `1`**: Jackson silently cuts off the decimal. **`"1000"` comes in as `1000`**: it converts the string into a number without warning. **`-5` comes in as `-5`**: a negative timeout, which `HttpRequest` would reject with an exception when the program uses it, far from where it was written. Only `true` was rejected, with a Jackson exception (`MismatchedInputException`). The point is not that Jackson is wrong: it is that it does what it was asked, which was to *convert*, and not what is needed, which is to *verify*. A configuration file with `"timeout_ms": "1000"` is almost certainly a mistake by whoever wrote it, and you want to know.

The lesson is general: **what comes from outside is validated where it enters**, once, with explicit rules, and after that validation the rest of the program works with types that are already trustworthy (`Servicio` has an `int` and a `URI`, not a string that may have been a number). That is why the configuration is read as a **tree** and validated by hand with `isInt()`, `isString()` and `isObject()`, and direct mapping to *records* is kept for **writing** (8.7), where the data is controlled by the program and not by a person.

#### 8.6.3 The configuration, validated

The `Configuracion` class is rewritten to read JSON. Read it as a cascade: `leer` opens the file and validates the general shape, `leerServicio` validates one service, and each field has its own small method.

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
    } catch (URISyntaxException _) {
      // Sin la causa: su mensaje repite la dirección entera, con usuario, clave y token.
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
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
    if (url.getRawUserInfo() != null) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no puede llevar usuario ni clave");
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

Three decisions deserve a comment. **The first: every failure is an `ErrorDeConfiguracion` with a message that says what to fix and where.** `servicio 2: falta 'nombre'` (service 2: 'nombre' is missing) is a message a person can use; a `NullPointerException` on line 57 is not. The position (`servicio 2`) counts from 1, the way a person counts. **The second: Jackson's errors are translated.** `JacksonException` is an **unchecked** exception in version 3 (one of the differences from version 2), so the compiler does not force you to catch it, and that is why it is easy to forget it and let a badly written file bring the program down with a stack trace. The `catch` turns it into `ErrorDeConfiguracion`, and along the way takes advantage of the fact that the exception knows at which line and column of the file it broke (`getLocation()`). **The third: the file is first read into a string with `Files.readString`**, with the error handling from Lesson 5 (`NoSuchFileException` becomes “no existe el archivo” (the file does not exist)), and only then is the text handed to Jackson. Each error has a single origin, and each one is dealt with in its own place.

**A fourth one, about security: a `url` with a user or password is rejected at load time, and no message repeats the address.** `http://usuario:clave@127.0.0.1:1/estado` has the `http` scheme, has a host, and the host is in the allowed list; but what comes before the at sign (`getRawUserInfo()`) is a credential, and the `revisor` would carry it into everything it writes. That is why it is rejected with `'url' no puede llevar usuario ni clave` ('url' cannot carry a user or password), a message that does not repeat it, and why the `catch` for `URISyntaxException` no longer passes the exception as the cause: its message carries the whole address, with whatever it holds. The check comes after the allowed list on purpose: `http://127.0.0.1@169.254.169.254/` is still rejected because of its host, as you will see in 8.8.3.

These are the messages it produces. The position counts from 1. The tests further down watch over the most dangerous ones; the others you provoke yourself in exercise 3:

| Case | Message |
|---|---|
| the file does not exist | `no existe el archivo servicios.json` (the file servicios.json does not exist) |
| badly written JSON | `servicios.json no es JSON válido: renglón 4, columna 5` (servicios.json is not valid JSON: line 4, column 5) |
| the list is missing | `falta la lista 'servicios'` (the 'servicios' list is missing) |
| the service is not an object | `servicio 2: debe ser un objeto` (service 2: must be an object) |
| a field is missing | `servicio 2: falta 'nombre'` (service 2: 'nombre' is missing) |
| empty name, or one that is not a string | `servicio 2: 'nombre' debe ser un texto que no esté vacío` (service 2: 'nombre' must be a non-empty string) |
| url that is not `http`/`https` | `servicio 2: 'url' debe empezar con http:// o https://` (service 2: 'url' must start with http:// or https://) |
| url without a host or badly written | `servicio 2: 'url' no es válida` (service 2: 'url' is not valid) |
| host outside the allowed list | `servicio 2: el servidor 169.254.169.254 no está en la lista permitida` (service 2: the host 169.254.169.254 is not in the allowed list) |
| url with a user or password | `servicio 2: 'url' no puede llevar usuario ni clave` (service 2: 'url' cannot carry a user or password) |
| invalid timeout | `servicio 3: 'timeout_ms' debe ser un entero mayor que 0` (service 3: 'timeout_ms' must be an integer greater than 0) |
| repeated names | `nombres repetidos: catalogo` (repeated names: catalogo) |

With a file that is missing a comma between two services (here, the one at the end of the `catalogo` line), the `revisor` says exactly where:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

`renglón 4, columna 5` (line 4, column 5): the point where Jackson expected a comma and found the `{` that opens the next service. With an empty file, it says what is missing and not a stack trace; and with a list of strings where there should be objects, it says which service is the wrong one:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

The test class is rewritten with the JSON cases. It is the same structure as Lesson 7, with one novelty: the table of cases is now multi-line JSON files, and a `@CsvSource` table with that would be unreadable. JUnit has another data source, **`@MethodSource`**, which points to a method (here `archivosInvalidos`) that returns a `Stream<Arguments>`, a list of rows where each `Arguments.of(...)` carries the values of one. The text blocks (`"""`) from Lesson 2 let you write each JSON the way you see it in a file.

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

Seven tests: the happy case and the six most dangerous rejections: a URL that is not `http`, a host outside the list (the `169.254.169.254` from 8.8), the two timeouts that direct mapping would have let through (`1.5` and `"1000"`), a service without a name and repeated names. The two timeout ones are the reason for the previous section to exist: if someone “simplifies” the validation into direct mapping, those two rows turn red.

### 8.7 Writing the report as JSON

Writing is the other direction, and here it does make sense to let Jackson work for you, because the data is produced by the program and not by a person. You hand it a *record* and it returns the text. This is the minimal example, with the names that were shown before the validation:

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

`writerWithDefaultPrettyPrinter()` produces the text with indentation; without it everything comes out on a single line (which is what a machine prefers). The `SNAKE_CASE` option, which is set **only once** when the mapper is built, makes `tiempoMs` come out as `tiempo_ms`: the *record* is written in Java's style (`camelCase`) and the JSON in the contract's. And look at the service that did not respond: its `tiempo_ms` is `null`, not zero and not absent. That is information: it means “not measured,” and it is the `Long` field (with a capital letter, the wrapper from Lesson 2, which admits `null`) and not `long` that allows it. The spaces around the colon (`"nombre" : "pagos"`) are those of Jackson's formatter, which are not those of other languages; they are valid and any JSON reader accepts them.

In the `revisor` there are three pieces. The first, `Json`, is **the** program's mapper, a single one, configured once (they are objects meant to be reused):

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

The second, `ReporteJson`, is the JSON's **shape**, deliberately kept separate from the program's internal types. `Reporte`, `Resultado` and `Resumen` are the classes the program works with; `ReporteJson` is the contract with whoever reads the report. If tomorrow you change something internal (how the times are stored, a name), the JSON does not change unless you decide so, and not by accident:

```java
// fig08_10/src/main/java/mx/habil/revisor/ReporteJson.java

package mx.habil.revisor;

import java.net.URI;
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
                        sinSecretos(r.servicio().url()),
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

  /**
   * La dirección sin usuario, clave, consulta ni fragmento: solo esquema, servidor, puerto y
   * ruta. El reporte lo lee quien lo pida, y una consulta puede traer un token.
   */
  static String sinSecretos(URI url) {
    String puerto = url.getPort() == -1 ? "" : ":" + url.getPort();
    String ruta = url.getRawPath() == null ? "" : url.getRawPath();
    return url.getScheme() + "://" + url.getHost() + puerto + ruta;
  }

  private static Long nulo(OptionalLong valor) {
    return valor.isPresent() ? valor.getAsLong() : null;
  }
}
```

The `de` method converts a `Reporte` into its JSON shape, and the conversion from `OptionalLong` to `Long` (`nulo`) is what produces the `null` for the services without a response. The `url` field is not `url().toString()` either: it is `sinSecretos(url)`, which rebuilds the address from scheme, host, port and path, and leaves out user, password, query (`?token=...`) and fragment (`#...`). The JSON is a contract with whoever reads it, so the field stays and keeps its shape; what is removed is what that reader must not see. With `http://127.0.0.1:1/estado?token=SECRETO#x` in `servicios.json`, the JSON says `"url" : "http://127.0.0.1:1/estado"`. The compact constructor with `List.copyOf` is the defensive copy from Lesson 4; Lesson 10 shows what an analysis tool says (and does not say) about it. The third piece is `Formato`, which gains one method, `json`, and is the only change in a file whose remainder you already know:

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

The `revisor` does not yet offer an option to request that JSON (it arrives with the command line in Lesson 10), but you can see it by temporarily changing, in `Revisor.java`, the line that prints the table to `System.out.println(Formato.json(reporte));`. This is how what it prints begins:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
{
  "servicios" : [ {
    "nombre" : "catalogo",
    "url" : "http://127.0.0.1:8089/catalogo",
    "estado" : "ARRIBA",
```

It is the JSON that the course fixes for the `revisor`'s report, the same one as the [Python course](https://github.com/HabilMX/curso-python): `servicios` is a list with one entry per service, `tiempo_ms` is `null` for the one that did not respond, and `resumen` repeats the counts. Restore the table line before moving on.

### 8.8 SSRF: someone else writes the URL

#### 8.8.1 The problem, with a concrete example

Look at `servicios.json` again. Every `url` is an address that **your program connects to, from your network, with your machine's permissions**. Imagine that today you write the file yourself, but tomorrow someone builds a screen where another person adds services, or a file from another team gets copied to that place. That person is no longer writing a piece of data: they are writing **an instruction for your server**. What address would an attacker put? One that they cannot reach from outside but your server can. In the cloud there is a very well-known one: `http://169.254.169.254/latest/meta-data/`. It is a “link-local” address, the kind that does not leave the machine's own network, which on AWS (and with similar mechanisms on other clouds) answers with the server's data and, in some configurations, with temporary credentials; the [AWS documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html) describes it. A `revisor` without defenses would connect, say `ARRIBA`, and would have done what the attacker wanted without anyone touching the server.

This kind of flaw is called **SSRF** (*Server-Side Request Forgery*): getting a server to make a request where it should not. It is tenth place on the [OWASP Top 10 list of 2021](https://owasp.org/Top10/A10_2021-Server-Side_Request_Forgery_%28SSRF%29/), the most widely cited catalog of web application risks, and it has its entry in the catalog of software weaknesses, [CWE-918](https://cwe.mitre.org/data/definitions/918.html). The `revisor` does not read the body of the responses, which reduces the damage (the attacker would know that something answered, not what it said), but it still reveals what exists inside the network, and a program that one day read that body would hand it over.

The defense is not a single thing, it is a stack of four, and each one covers what the previous one lets through. It is what is called *defense in depth*.

#### 8.8.2 First defense: the scheme

A URL begins with a **scheme**, the part before the colon: `http`, `https`, `ftp`, `file`. The `revisor` only speaks HTTP, so it accepts two, and checks it with `URI.getScheme()`, as you saw in `Configuracion`. The rest of the schemes, some of them dangerous (`file:///etc/passwd` points to files on the machine itself), are rejected. The `HttpClient` is already cautious on its own, and the last line of this program shows it, which also shows how Java reads an address:

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

`file:` does not even get past the request's constructor (`invalid URI scheme file`). But do not rely only on that: the `revisor` rejects the scheme earlier, with a clear message, in the configuration.

#### 8.8.3 Second defense: the allowed list of hosts

With the `http` scheme, `http://169.254.169.254/` is still left. The second defense is the one that weighs the most: an **allowed list** of hosts. Instead of trying to guess which addresses are dangerous (there are too many, and you always forget one), you enumerate the only ones that are accepted and reject everything else:

```java
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");
```

Today the list is `127.0.0.1` and `localhost`: the machine itself, where the lab lives. In Lesson 10 the list becomes configurable from the command line. Check the rejection with a file that points to the dangerous host:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

Before the program sends a single packet, the configuration has already refused, with a message that says which host and why. Notice how the comparison is made: **with `url.getHost()`, the host that the URI parser extracted, and by exact equality**. Look at what that function returns in the previous program for mischievous addresses: `http://127.0.0.1@169.254.169.254/latest/meta-data/` looks, at first glance, like an address on `127.0.0.1`; but for HTTP, what comes before an at sign is a user (the user `127.0.0.1`!), and the host is `169.254.169.254`. A validation done with `texto.startsWith("http://127.0.0.1")` would let exactly that attack through; the one done with `getHost()` rejects it. And `http://127.0.0.1.otro.example/` starts with the IP but is another host, and `http://127.0.0.1:puerto/` does not even have a recognizable host (the `revisor` rejects it as “no es válida” (“is not valid”)). The rule: **never validate an address with text operations; parse it with the URI parser and compare its parts.**

The list is case-sensitive, and it rejects valid things, such as `http://LOCALHOST:8089/` or the IPv6 form of the machine itself, `http://[::1]:8089/`:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

That is deliberate: an allowed list must be **strict by default**. If one day another form needs to be accepted, it is added to the list on purpose, and the comparison is not relaxed.

#### 8.8.4 Third defense: do not follow redirects

A server that is on the list can respond with a redirect to one that is not. If the client followed it, the allowed list would be bypassed through the back door: you authorized `127.0.0.1`, and the client would end up at `169.254.169.254`. That is why the `revisor`'s client uses `Redirect.NEVER` explicitly (8.2.1) and treats a `3xx` as one more response (8.3.3): what is not followed cannot be used against you. An honest warning: **none of the 25 tests watches over this defense**. It was measured: changing `NEVER` to `ALWAYS` in `Revision.java` leaves all 25 green. What watches over it is the program from 8.3.3, which is not part of the project. Writing that test (a test server that redirects to another) is a good closing exercise.

#### 8.8.5 Fourth defense, and what this does NOT solve

The fourth is minimal: the timeout of each request. A hostile server can answer very slowly on purpose to keep your program busy; the timeout limits how much it can abuse it.

And now the honest part, because security that is hidden turns false: **these four defenses are a good start, not a wall.** The allowed list compares *names*, not real addresses. `localhost` is a name that your system translates into an address by consulting the `/etc/hosts` file or a name server; a hostile name server can answer an address other than the expected one (the attack is called *DNS rebinding*). For a service that goes out to the internet with a name server you do not control, the complete defense consists of resolving the address, checking that it does not belong to an internal or special range, and connecting to *that* address and not to the name, besides isolating the server's network so that internal things are not reachable even if the validation fails. That is the last line of defense and it is infrastructure, not code. The [OWASP cheat sheet on SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) describes the two layers and why neither is enough on its own. This course's `revisor` works in a local environment that you control: its list allows `127.0.0.1` and `localhost`, and on your machine `localhost` is translated by your `/etc/hosts` file to the same address. There, the list of names is enough; outside an environment like that, accepting a name such as `localhost` is not a complete defense, for what was just explained; what matters in the lesson is the habit: **every address that someone else wrote is suspect until it passes through a list of what is allowed.** There is a Hábil article on [API security: identity, gateway and tokens](https://www.habil.mx/en/blog/api-security-cto-identity-gateway-tokens/) that follows the topic from the architecture side.

### 8.9 The project step: the `revisor` queries for real

All the pieces are there. This is the project as it stands at the end of the lesson:

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

You have already seen complete `pom.xml`, `servicios.json`, `Json`, `Configuracion`, `Revision`, `ReporteJson`, `Formato`, `ConfiguracionTest` and `RevisionTest`. The only one missing is the `Revisor`, which changes two things: it reads `servicios.json` and passes the allowed list to `Configuracion.leer`.

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

The eleven files that did not change compared with Lesson 7 are included so that the project can be put together without looking at another lesson:

<details>
<summary>The eleven files that did not change</summary>

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

Build and check the count. The project's tests do not need the lab (`RevisionTest` starts its own server), but the run afterwards does:

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

`Tests run: 25`: 18 from Lesson 7, minus the 5 in `ConfiguracionTest` that read text, plus the 7 new ones that read JSON, plus the 5 in `RevisionTest` (18 − 5 + 7 + 5). Write it down in your logbook. If it does not come out as 25, something moved, and it is a question worth answering before moving on. And the run, with the full *classpath*:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

The usual table, now with measured times, and the summary with an average that depends on them.

**What these 25 tests do not watch over.** A green number does not say what it covers. To find out, the code that follows was broken one piece at a time, and it was measured whether any test turned red. They turned red, and that is why they are watched: the rejection of schemes, of hosts outside the list, of invalid `timeout_ms` and of repeated names; the translation of `ConnectException` and of `HttpTimeoutException`; and the request's timeout. **They stayed green**, that is, unwatched: the message `'nombre' debe ser un texto que no esté vacío` ('nombre' must be a non-empty string), the one for `falta la lista 'servicios'` (the 'servicios' list is missing), the client's `connectTimeout`, the `Redirect.NEVER` policy and the conversion of the time to `null` in `ReporteJson` (the latter is watched by Lesson 10's contract). It is not a defect in the lesson, it is the rule: every new test you add closes one of those gaps, and exercises 3 and 4 start there.


## The error you will see

The lesson has one main error, that of the Jackson class that does not appear, and several supporting messages that are worth knowing how to read at a glance.

**The main error.** You run the `revisor` with the command from Lessons 6 and 7, which does not include the Jackson JARs:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError` (the name of the missing class appears with slashes, `tools/jackson/core/JacksonException`) is not a compilation error. The program **compiled** fine, because Maven did know about the dependency; it fails when it runs, because the JVM does not know where to look for it. The cause that follows (`Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException`) confirms it: that class is not in any of the places on the *classpath*. The solution is to run with `-cp "target/classes:target/lib/*"` (and to have run `mvn -B verify` beforehand, which is what copies the JARs to `target/lib`). If someone asks you “it compiles but does not run?”, the answer is almost always the *classpath*. And its close relative, the one from 8.5.1, is copying a Jackson 2 example: `package com.fasterxml.jackson.databind does not exist`. It is fixed by changing `com.fasterxml.jackson` to `tools.jackson` in the `import` and checking the method names.

**The supporting messages**, to recognize them at a glance:

- `java.net.BindException: Address already in use` when starting the lab: another program uses port 8089, or you left a previous lab running. Close it with `Ctrl`+`C` in its terminal.
- All the services with `sin conexión`: the lab is not on. Turn it on in another terminal and run again.
- `exception ConnectException has already been caught`: you put a `catch` of the general type before the particular one. From the particular to the general.
- `ConnectException` with a `null` message (`fig08_03.java`): normal. That is why the `revisor` never shows the message of a network exception.
- `error de configuración: servicios.json no es JSON válido: renglón 4, columna 5` (configuration error: servicios.json is not valid JSON: line 4, column 5): something is missing or extra near that point; it is almost always a comma. Look at the line before the one indicated.
- `error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida` (configuration error: service 2: the host 169.254.169.254 is not in the allowed list): the defense working. It is not a failure: it is the notice that someone asked for an address that the `revisor` does not query.
- `invalid URI scheme file` (`fig08_09.java`): the `HttpClient` rejects schemes that are not HTTP; the `revisor` rejects them earlier, in the configuration.

## What gets done wrong

**Sending a request without a timeout.** It is the most expensive mistake in the lesson because it does not fail in the tests: it works on the day of the example and hangs the program on the day a service goes silent. Every request carries its `timeout`, and the client its `connectTimeout`.

**Creating one `HttpClient` per request, or not closing it.** Each client keeps connections and resources; one per request throws away what the client saves by reusing, and not closing it leaves them occupied. One per review, with `try-with-resources`.

**Showing `e.getMessage()` of a network exception.** Sometimes it is `null`, sometimes it is an internal text of the implementation, and it changes between versions and systems. Translate the **type** into a fixed detail and leave the message out.

**Catching `Exception` “just in case” around the network.** It also swallows programming errors (a `NullPointerException` from your own logic) and disguises them as a “network error.” You catch the concrete types: `HttpTimeoutException`, `ConnectException`, `IOException`, in that order.

**Trusting a JSON because “I write it myself.”** The file will be edited by someone else, or by you in a hurry. Mapping directly to a *record* lets `1.5` and `"1000"` through, and a `-5` reaches an exception far from where it was written. You validate at the entrance, with messages that say what to fix.

**Validating an address with text operations.** `startsWith("http://127.0.0.1")` lets `http://127.0.0.1@servidor-del-atacante/` through. You parse it with `URI` and compare `getHost()` with an allowed list.

**A list of what is forbidden instead of one of what is allowed.** The dangerous addresses are too many and new every year; the ones you need are few and you know them. What is not on the list is rejected.

**Following redirects blindly.** An authorized address that leads to an unauthorized one voids the list. `Redirect.NEVER`, written out.

**Tests that depend on a service they do not control.** If `RevisionTest` queried the lab, it would fail on any computer where it was not on. The test starts its own server, with known behavior, on a free port.

**Measuring times with the clock's time of day.** `System.currentTimeMillis()` can jump with a clock adjustment; `System.nanoTime()` is the one that works for measuring intervals.

## Exercises

### Exercise 1 — The address that must not pass, and its cousins

In your `servicios.json`, change the second service to each of these addresses, **one at a time**, and for each one **predict first** what the `revisor` prints: (a) `http://169.254.169.254/latest/meta-data/`; (b) `http://localhost:8089/pagos`; (c) `http://[::1]:8089/pagos` (the address of “this very computer” in IPv6 notation); (d) `http://LOCALHOST:8089/pagos`. Then run each one and compare. Write down in the logbook the one you got wrong, and why the allowed list from 8.8.3 behaves the way it does with it.

### Exercise 2 — A service that never answers

Add three lab services to your `servicios.json`: `facturacion` (`/facturacion`), `notificaciones` (`/notificaciones`, with `"timeout_ms": 1000`) and `basura` (`/basura`). **Before running**, predict the status and detail of each one, and how many up, slow and down the summary will say. Then run. Finally, remove the `.timeout(...)` line from `Revision.java`, run again and explain what happened with `notificaciones`, how long the program took, and what would have happened if the service had never answered.

### Exercise 3 — Three badly written files

With the original `servicios.json`, break the file in three ways, one at a time, and **predict the message** before running: (a) remove the comma at the end of the `catalogo` service's line; (b) leave the file completely empty; (c) change the value of `servicios` to `["catalogo"]`, a list of strings instead of objects. For (a), before running, write down on which line and column you expect it to fail.

### Exercise 4 — Two rows that direct mapping would have let through

Add two cases to the `archivosInvalidos` table in `ConfiguracionTest`: a service with `"timeout_ms": -5` and another with `"timeout_ms": true`. **Predict** the message of each one and how many tests the report will say (hint: each `Arguments.of` is a test). Run `mvn -B verify`. Did anything have to change in `Configuracion.java` for them to pass?

## Solutions

### Solution 1

(a) The metadata service's address is rejected before anything is sent:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

(b) `localhost` is on the allowed list, so `http://localhost:8089/pagos` passes, and the `revisor` queries the lab as if it were `127.0.0.1`. Notice what changed and what did not: the second service is still called `inventario` (you only changed its address), but it now asks for the `pagos` path, which answers fast, so it comes out ARRIBA and not LENTO:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     180ms  200
inventario  ARRIBA      92ms  200
pagos       ARRIBA      91ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 3 arriba · 0 lentos · 1 caído · promedio 121ms
```

It is a small lesson about names: a service's name is a label that a person writes, and the address is what is really queried. The `revisor` does not check that they match.

(c) and (d): **neither of them passes**, and that is deliberate. `[::1]` is the host `[::1]` for `getHost()`, and it is not on the list; `LOCALHOST` in capitals is not either, because the comparison is exact.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor LOCALHOST no está en la lista permitida
```

If you expected (c) or (d) to pass, it is because you thought of what they *mean* (the same computer) and not of what they *say* (another string). An allowed list compares strings, and it is kept strict on purpose: accepting another form is a matter of adding it to the list, with a cool head, and not of relaxing the comparison so that it “almost matches.”

### Solution 2

**Prediction.** The usual four stay the same: `catalogo` and `pagos` ARRIBA, `inventario` LENTO, `reportes` CAIDO with `sin conexión`. Of the three new ones, `facturacion` responds `503`, CAIDO with detail `503`; `notificaciones` waits longer than its timeout of one second, CAIDO with `tiempo agotado`; `basura` closes the connection without answering, CAIDO with `error de red`. Summary: 7 services, 2 up, 1 slow, 4 down, and the average only includes those that responded (`catalogo`, `inventario`, `pagos` and `facturacion`, which responded although with an error): about 670 milliseconds, because the 2.3 seconds of `inventario` weigh heavily among four.

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

The average is the sum of the four that responded divided by four. (The long names overflow the column, a defect that the formatting from Lesson 2 carries along and that Lesson 10's optional exercise solves.) Now, without the `.timeout(...)`:

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

`notificaciones` is no longer `tiempo agotado`: without a timeout, the client waited the 10 seconds the lab takes, received the `200` and classified it LENTO (`10.0s`). The whole program took about 13 seconds (measured with `time`: 12.9) and the average shot up. But the lab is kind: it answers after ten seconds. **A real service that goes silent never answers**, and then `send` does not return, the program hangs and no other check completes. That is why the request's timeout is never removed: it is the only thing that bounds the worst case. Put the line back.

### Solution 3

(a) Without the comma, Jackson reaches the `{` of `inventario` expecting a comma or the end of the array, and fails there; the message lands on line 4 (the one for `inventario`), column 5, which is where that brace begins, after the four-space indentation. Whoever made the mistake left the defect on line 3, but the parser only notices when it sees what follows.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

(b) An empty file is invalid JSON, but the tree that Jackson returns for an empty text is not an error: it is an “absent” node, and the `revisor` treats it as “does not have the list”:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

(c) A list of strings has the shape of a list, but each element should be an object:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

### Solution 4

The two rows are added at the end of the table: the last one that already existed now ends with a comma, and after it go the two new ones, in the same format as the others:

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

Both messages are the usual one, `servicio 1: 'timeout_ms' debe ser un entero mayor que 0` (service 1: 'timeout_ms' must be an integer greater than 0): `-5` is an integer but is not greater than 0, and `true` is not an integer (`isInt()` is false). Without touching `Configuracion.java`, because the validation already covered those two cases from the start. With direct mapping, `-5` would have passed silently (you saw it in 8.6.2) and `true` would have been rejected, but with a Jackson error and not with your message. The count goes up from 25 to 27:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, ... in mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## How I know I got it

- [ ] `mvn -B verify`, from the root of `~/curso-java/revisor/`, ends with `BUILD SUCCESS` and says `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`, and you know where each number comes from (18 − 5 + 7 + 5).
- [ ] With the lab on, `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` prints the table of four services with ESTADO and DETALLE identical to those in this lesson, and a summary with `2 arriba · 1 lento · 1 caído` (2 up · 1 slow · 1 down).
- [ ] With the lab off, the four services come out CAIDO with `sin conexión`.
- [ ] A service pointed at `http://169.254.169.254/latest/meta-data/` makes the `revisor` print `el servidor 169.254.169.254 no está en la lista permitida` without having sent any request.
- [ ] You removed the `.timeout(...)` from `Revision.java` and saw the `tiempo agotado` test turn red; you restored it.
- [ ] A `servicios.json` with one comma too few produces `no es JSON válido: renglón N, columna M` (is not valid JSON: line N, column M), and you know which line to look at.
- [ ] You can explain why `"timeout_ms": "1000"` comes in as 1000 with direct mapping and is rejected with hand validation.
- [ ] You wrote down in your logbook which message in this lesson was hardest for you to read.

## Summary

Answer without looking at the lesson:

1. What two timeouts are there in a request with `HttpClient`, where is each one configured, and what does each one throw when it runs out?
2. Why is an `HttpRequest` without a `timeout` dangerous?
3. Which `revisor` detail corresponds to each of these exceptions: `HttpTimeoutException`, `ConnectException` and any other `IOException`? Why are they caught in that order?
4. Why is a `503` not an exception? And a `302`, with the `revisor`'s redirect policy?
5. Why is time measured with `System.nanoTime()` and not with `currentTimeMillis()`?
6. What is the difference between `tools.jackson` and `com.fasterxml.jackson`? Which annotations did not change package?
7. What is the *classpath* and why is `target/lib` missing in `java -cp target/classes` even though the program compiles?
8. Why is the configuration read as a tree and validated by hand instead of mapping it directly to a *record*? Name two values that direct mapping lets through.
9. What is SSRF and what are the `revisor`'s four defenses? Why is `startsWith("http://127.0.0.1")` not a defense?
10. What does the allowed list of names not solve? What else would be needed in a service that goes out to the internet?

## Further reading

- [Java SE 25, package `java.net.http`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/package-summary.html): the complete JDK HTTP client, with the asynchronous requests, HTTP/2 and WebSocket that this lesson does not use. Consulted on October 7, 2026.
- [RFC 9110, “HTTP Semantics”](https://www.rfc-editor.org/rfc/rfc9110): the standard for methods, status codes and redirects; section 15 is the list of status codes. Consulted on October 7, 2026.
- [Jackson 3 migration guide](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) and [the `jackson-databind` repository](https://github.com/FasterXML/jackson-databind): what changed compared with version 2 and how the library is used. Consulted on October 7, 2026.
- [OWASP, “Server-Side Request Forgery Prevention Cheat Sheet”](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html): the layered defenses against SSRF and why one alone is not enough. Consulted on October 7, 2026.

---

**Previous:** [Lesson 7 — Tests with JUnit](07-pruebas-junit.md) ·
**Next:** [Lesson 9 — Concurrency: checking everything at once](09-concurrencia.md)
