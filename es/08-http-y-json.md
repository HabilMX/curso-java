# Lección 8 — HTTP y JSON

**Tiempo:** 2 × 90 min (dos sesiones; el corte está entre las secciones 8.3 y 8.4)

**Qué construyes:** el `revisor` deja de inventar sus respuestas: consulta cada servicio por HTTP de verdad y lee su lista de un archivo `servicios.json` que valida antes de creerle

**Qué aprendes:** el `HttpClient` del JDK (un cliente, tiempo de conexión y tiempo límite por petición, redirecciones, códigos de estado); cómo convertir cada falla de red en un detalle corto sin que escape una excepción; JSON y Jackson 3 (`tools.jackson`), y qué cambia respecto a casi todo lo que hay en internet; leer un JSON como árbol y validarlo a mano; escribir el reporte con *records*; y **SSRF: la URL la escribe otra persona**

**De dónde vienes.** Traes el proyecto de Maven de la [Lección 7](07-pruebas-junit.md): paquetes, un `pom.xml`, 18 pruebas en verde y un `Simulador` que hace el papel de la red con cuatro respuestas inventadas. Esa invención es lo que se acaba hoy. Un revisor que nunca habla con la red no revisa nada; y una lista de servicios en texto plano, que no distingue un número de un texto, no aguanta el primer archivo escrito por alguien distinto de ti. Esta lección resuelve las dos cosas, y de paso te enseña la parte del trabajo que más se subestima: **qué hacer con lo que viene de fuera**. Trabajas en tu carpeta `~/curso-java/revisor/`.

**Qué no hace esta lección.** No consulta los servicios a la vez: `revisarTodos` los revisa uno tras otro (en serie), y por eso el reporte tarda unos 2.5 segundos, la suma de lo que tarda cada servicio; revisarlos todos a la vez es la [Lección 9](09-concurrencia.md). Tampoco lee el cuerpo de las respuestas (al `revisor` solo le importa el código y cuánto tardó), ni tiene todavía una línea de comandos: el reporte en JSON existe y se prueba hoy, pero se pedirá con una opción hasta la [Lección 10](10-el-programa-terminado.md).

**Sobre las corridas.** Los servicios que se consultan son del **laboratorio** (8.1), un servidor local de práctica que contesta siempre lo mismo; por eso el ESTADO y el DETALLE de cada renglón son idénticos en cada corrida. Lo que cambia entre corridas es la columna TIEMPO, porque es un tiempo medido de verdad: si tu tabla dice `171ms` donde aquí dice `180ms`, es normal. Todo se ejecutó con Temurin 25, Maven 3.10.0, JUnit 6.1.3 y Jackson 3.2.3. Los ejemplos que usan Jackson se ejecutan desde la raíz del proyecto, con `java -cp "target/lib/*" fig08_06.java` (8.5 explica por qué), y los programas están en [`programas/08-http-y-json/`](../programas/08-http-y-json), en el repositorio del curso.

## Al terminar vas a poder

- Hacer una petición con `HttpClient`, leer el código de estado y medir cuánto tardó la respuesta.
- Explicar la diferencia entre el tiempo de conexión y el tiempo límite de una petición, y por qué ninguna petición se manda sin tiempo límite.
- Convertir cada falla de red (no conecta, tiempo agotado, conexión cerrada) en un detalle corto, sin que una excepción de red escape de `revisar`.
- Declarar la dependencia de Jackson 3, distinguir `tools.jackson` de `com.fasterxml.jackson`, y explicar qué es el *classpath* y para qué sirve `target/lib`.
- Leer un JSON como árbol (`JsonNode`) y validarlo a mano, y explicar por qué mapearlo directo a un *record* acepta `1.5` y `"1000"` como tiempo límite.
- Escribir el reporte como JSON con *records* y llaves en `snake_case`.
- Explicar qué es SSRF y aplicar las defensas del `revisor`: esquema permitido, lista permitida de servidores y redirecciones sin seguir.

## El porqué antes del cómo

Hasta ahora el `revisor` ha sido un actor. Cuando decía que `inventario` tardaba 2.3 segundos, lo decía porque tú lo escribiste en el `Simulador`; no había nadie del otro lado. Cambiar eso es más que cambiar de dónde sale un número, porque el mundo real tiene cuatro costumbres que el simulador nunca tuvo. **Tarda cuando quiere:** un servicio puede contestar en 80 milisegundos o en diez segundos. **No siempre contesta:** el puerto puede estar cerrado, la conexión puede cortarse a la mitad, el servidor puede aceptarla y quedarse callado. **Contesta cosas raras:** un servicio caído a menudo no deja de responder, responde con un 503. Y **a veces manda a otra parte:** una respuesta puede decir «lo que buscas ya no está aquí, ve a esta otra dirección». El `revisor` tiene que clasificar todo eso, sin caerse, y sin quedarse esperando para siempre.

La segunda mitad de la lección es otra clase de problema. Hasta ahora la lista de servicios era un `servicios.txt`: nombre, dirección, un tiempo límite opcional, separados por espacios. Sirve mientras todo sea texto y todo sea fácil. Pero un archivo de texto no distingue el número `1000` del texto `"1000"`, no puede anidar nada, y no tiene una forma estándar de decir «este campo no está». Para eso existe **JSON**, el formato de intercambio de datos más usado de internet, y Java no lo trae en su biblioteca estándar: se usa una biblioteca de otros, y por eso la Lección 6 existió.

Y entre las dos mitades hay una idea que las une y que es la más importante de la lección: **tu programa ahora hace, con lo que dice un archivo, algo en el mundo.** Si el archivo dice «consulta esta dirección», el `revisor` la consulta. Quien pueda escribir en ese archivo, o en el formulario que algún día lo alimente, controla a dónde se conecta tu programa desde tu red. Esa clase de falla tiene nombre propio y está entre las diez más serias de las aplicaciones web: se llama **SSRF**, y la última sección la ataca con cuatro defensas.

El camino tiene dos sesiones. La **primera** cubre 8.1 a 8.3: el laboratorio, el `HttpClient`, los tiempos límite y las fallas, y termina con un `revisor` que consulta de verdad. La **segunda** cubre 8.4 a 8.9: JSON, Jackson 3, validar, escribir y proteger. Con ese destino en mente, esto es lo que imprime el `revisor` al final de la lección, con el laboratorio encendido:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

El ESTADO y el DETALLE son los de siempre. La columna TIEMPO ya no son números escritos por ti: son lo que midió tu computadora.

## Los conceptos

### 8.1 El laboratorio y la primera petición

#### 8.1.1 HTTP en cinco renglones

**HTTP** es el idioma con el que hablan casi todos los servicios de internet. Una conversación es siempre la misma: un **cliente** (tu programa) manda una **petición** y un **servidor** manda una **respuesta**. La petición lleva un **método** que dice qué se quiere (`GET` es «dame esto»), una **ruta** que dice qué (`/pagos`) y unos encabezados con datos extra. La respuesta lleva un **código de estado**, que es un número de tres cifras con el resultado; unos encabezados; y, casi siempre, un **cuerpo** (la página, los datos, el texto «ok»).

Del código de estado el `revisor` solo necesita la primera cifra, que es una familia, según la [especificación de HTTP](https://www.rfc-editor.org/rfc/rfc9110): `2xx` es éxito (`200` es «aquí está»), `3xx` es redirección («busca en otro lado»), `4xx` es un error de quien pregunta (`404` es «eso no existe»), y `5xx` es un error del servidor (`503` es «ahora mismo no puedo atenderte»). La regla de clasificación del `revisor` que ya tienes, «un código de 400 o más deja al servicio caído», usa exactamente esa división: contestar `503` es estar caído aunque se conteste rápido.

Esto es lo que viaja de verdad. Con `curl`, el programa de la terminal que hace una petición y muestra la respuesta, la opción `-i` deja ver el código y los encabezados antes del cuerpo (la fecha cambia en cada corrida):

```text
$ curl -si http://127.0.0.1:8089/pagos
HTTP/1.1 200 OK
Date: Thu, 08 Oct 2026 03:42:51 GMT
Content-length: 3

ok
```

Primer renglón: `HTTP/1.1 200 OK`, la versión del protocolo, el código y su nombre. Luego los encabezados, uno por línea, y una línea en blanco; y después el cuerpo, que aquí es el texto `ok`. No hay nada más. Las dos cosas que le importan al `revisor` son el `200` y cuánto tardó en llegar.

#### 8.1.2 El laboratorio

Para practicar con una red hay dos caminos, y los dos tienen un problema. Consultar servicios públicos de internet vuelve cada corrida distinta (hoy contestan, mañana no, y sus tiempos dependen de tu conexión), y no es de buena educación practicar contra servidores ajenos. Escribir un servidor propio exige conceptos que todavía no tienes. La salida de este curso es un **laboratorio**: un programa pequeño que levanta un servidor HTTP en tu propia computadora (`127.0.0.1`, que es el nombre de «esta misma computadora»), con servicios que contestan siempre lo mismo y tardan siempre lo mismo. Así los resultados se pueden comparar con los de esta lección, renglón por renglón.

Por ahora el laboratorio es una **caja negra**: lo vas a correr, no a escribir. Usa herramientas que todavía no se explican (un servidor HTTP que viene en el propio JDK y un hilo por cada petición), y es una excepción a la regla de no usar nada antes de explicarlo (hay otra pequeña en 8.3.4); la [Lección 10](10-el-programa-terminado.md) lo abre y lo usa de verdad. Guárdalo como `~/curso-java/Laboratorio.java`, **un nivel arriba de la carpeta del proyecto**, porque no es parte de él:

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

Y córrelo **en otra terminal**, que dejas abierta mientras practicas. Se queda corriendo, esperando peticiones, hasta que lo detienes con `Ctrl`+`C`:

```text
$ java Laboratorio.java
laboratorio escuchando en http://127.0.0.1:8089
```

Si en cambio ves `java.net.BindException: Address already in use`, es que otro programa de tu computadora ya usa el puerto 8089 (un **puerto** es el número de la «puerta» por donde un servidor escucha). El puerto lo libera **detener el proceso** que lo ocupa, no abrir otra terminal: casi siempre es un laboratorio anterior que sigue corriendo, y se detiene con `Ctrl`+`C` en la terminal donde lo arrancaste. Si no encuentras esa terminal, `ss -ltnp 'sport = :8089'` muestra qué proceso escucha en el 8089 (con su nombre y su número de proceso), y con ese número lo detienes. Estas son sus rutas:

| Ruta | Qué contesta | Y qué tendrá que concluir el `revisor` |
|---|---|---|
| `/catalogo` | `200` tras 142 ms | ARRIBA |
| `/inventario` | `200` tras 2300 ms | LENTO |
| `/pagos` | `200` tras 87 ms | ARRIBA |
| `/facturacion` | `503` tras 95 ms | CAIDO (`503`) |
| `/notificaciones` | `200`, pero tras 10 segundos | CAIDO si se le pone un tiempo límite de 1 s |
| `/basura` | cierra la conexión sin contestar | CAIDO (`error de red`) |
| `/espera/<ms>` | `200` tras `<ms>` milisegundos | según el tiempo |

Falta un servicio de los cuatro de siempre, y no está en el laboratorio a propósito: `reportes` apunta a `http://127.0.0.1:8088/`, donde **nadie escucha**, para tener un servicio cuya conexión se rechaza. Los cuatro de las tablas de siempre son `catalogo`, `inventario`, `pagos` y `reportes`; los otros son para ejercicios y pruebas.

#### 8.1.3 La primera petición

Con el laboratorio encendido, esta es la petición más corta que se puede hacer con Java 25:

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

Se lee de arriba abajo. **`HttpClient`** es el objeto que sabe hablar HTTP; está en el paquete `java.net.http` del JDK y no necesita ninguna dependencia. Se abre con `try (...)`, el `try-with-resources` de la Lección 5: un `HttpClient` guarda conexiones y trabajo interno, y cerrarlo lo libera; es cerrable (`AutoCloseable`) desde Java 21, según su [documentación](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html). **`HttpRequest.newBuilder(URI.create(...)).build()`** arma la petición: un objeto que ya no cambia, con la dirección a la que va. Si no dices otra cosa, el método es `GET`. **`cliente.send(peticion, BodyHandlers.ofString())`** la envía y **espera** la respuesta; hasta que llega, el programa no avanza. El segundo parámetro dice qué hacer con el cuerpo: `ofString()` lo convierte en un texto, y el resultado es un `HttpResponse<String>`, la respuesta, del que se piden el `statusCode()` y el `body()`.

Para medir cuánto tardó se toma el reloj antes y después de `send`, con `System.nanoTime()`, y se restan. Se usa `nanoTime` y no `currentTimeMillis` porque la [documentación](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/System.html) de `nanoTime` lo dice sin rodeos: sirve para medir **intervalos** de tiempo y no tiene relación con la hora del día. La hora de un reloj puede saltar (por un ajuste automático, por el horario de verano) en medio de una medición, y el intervalo saldría negativo o enorme. `nanoTime` entrega nanosegundos, y `/ 1_000_000` los vuelve milisegundos; el guion bajo en el número es un separador que Java ignora, para que cuente la vista.

```text
$ java fig08_02.java
código de estado: 200
cuerpo: ok
tardó al menos 87 ms: true
```

El código `200`, el cuerpo `ok` que el laboratorio manda, y una comprobación: el tiempo fue, por lo menos, los 87 milisegundos que `/pagos` se hace esperar. (Se imprime la comprobación y no el número porque el número cambia en cada corrida.) `throws Exception` en el `main` es un atajo de este ejemplo, que deja pasar cualquier excepción; en el proyecto cada una tendrá su tratamiento.

### 8.2 El `HttpClient` del `revisor`

#### 8.2.1 Un cliente, muchas peticiones

Un `HttpClient` no es solo la función `send`: es un objeto que se arma una vez y se queda con cosas que valen la pena reutilizar. La documentación lo dice: una vez construido, **es inmutable** y se puede usar para enviar muchas peticiones, y comparte recursos entre todas las que pasan por él (las conexiones ya abiertas, por ejemplo, que no hace falta volver a negociar). Por eso el `revisor` crea **un cliente por revisión** y lo usa para todos los servicios, no uno por servicio. Y se crea con el *builder* cuando se quiere configurar algo que `newHttpClient()` no configura, que es el caso: se necesita un tiempo de conexión y una política de redirecciones explícita.

```java
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }
```

Dos configuraciones, y las dos son decisiones, no adornos. `connectTimeout` fija cuánto se espera para **establecer la conexión** (más en 8.3). `followRedirects(HttpClient.Redirect.NEVER)` dice «si el servidor responde con una redirección, no la sigas»; es el valor que el cliente ya trae por omisión, y se escribe igual, **explícito**, porque es una decisión de seguridad (8.8) y una decisión que alguien escribió es una decisión que alguien puede revisar. El valor por omisión, según la documentación, es `NEVER`; dejarlo implícito significa que el día que alguien lo cambie, nadie lo va a notar.

#### 8.2.2 Qué hacer con el cuerpo: `BodyHandlers.discarding()`

`send` necesita saber qué hacer con el cuerpo de la respuesta, y lo pregunta con un segundo parámetro, el **manejador del cuerpo**. En el ejemplo anterior se usó `ofString()`, que lo lee completo y lo entrega como texto. El `revisor` no necesita el cuerpo: solo el código y el tiempo. Para eso existe `BodyHandlers.discarding()`: lee el cuerpo y lo **tira**, sin guardarlo. Hay una razón práctica detrás: si un servicio responde con un cuerpo de cien megabytes, `ofString()` los metería en la memoria de tu programa; `discarding()` los lee y no guarda nada. El tipo de la respuesta cambia en consecuencia: `HttpResponse<Void>`, una respuesta cuyo cuerpo es «nada» (`Void`, con mayúscula, es una clase del JDK que no tiene ningún valor posible salvo `null`; se usa justo en los genéricos, donde un parámetro de tipo tiene que ser una clase y no puede ser la palabra `void`, para decir «aquí no hay nada»).

### 8.3 Cuando algo falla: tiempos límite, excepciones y redirecciones

#### 8.3.1 Dos tiempos límite, y por qué ninguna petición se manda sin ellos

Esta es la sección que más separa a un programa de práctica de uno que se puede dejar corriendo. La [documentación de `HttpRequest.Builder`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html) dice sobre el tiempo límite de una petición: «el efecto de no poner un tiempo límite es el mismo que poner uno infinito, es decir, **esperar para siempre**». Si el servidor acepta la conexión y se queda callado, tu programa se queda esperando, hoy, mañana y el año que entra. Un `revisor` que revisa servicios para saber si están caídos y que se cuelga por culpa de uno de ellos es peor que no tener `revisor`.

Por eso hay **dos** tiempos límite, y cada uno cubre una etapa distinta:

| Tiempo | Dónde se configura | Qué cubre | Qué lanza si se acaba |
|---|---|---|---|
| **De conexión** | en el cliente (`connectTimeout`) | establecer la conexión con el servidor | `HttpConnectTimeoutException` |
| **De la petición** | en cada petición (`timeout`) | esperar a que llegue la respuesta | `HttpTimeoutException` |

La conexión es la etapa de «¿alguien contesta en esa dirección?», y el `revisor` le da dos segundos (`TIEMPO_CONEXION_MS = 2000`, la constante de `Revision`). Es una decisión del programa, no una ley de la red: en una red lenta o congestionada, una conexión podría establecerse después. Lo que dice la constante es «si en dos segundos no hay conexión, el `revisor` deja de esperar y reporta `sin conexión`», que para servicios que viven cerca, como los del laboratorio, es un margen amplio. La petición es la etapa de «ya me atendieron, ¿cuándo me responden?», y su tiempo es **del servicio**: viene de `servicio.timeoutMs()`, que por omisión vale 5000 y que el archivo de configuración puede cambiar para un servicio que se sabe lento. El tiempo de conexión de un cliente [solo tiene efecto cuando hay que abrir una conexión nueva](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.Builder.html); si ya hay una abierta de una petición anterior, se reutiliza y no aplica.

#### 8.3.2 Qué excepciones llegan, y qué detalle les toca

`send` declara dos excepciones verificadas: `InterruptedException`, que ya conoces de la Lección 5, y `IOException`, que agrupa **todo lo que puede salir mal en la red**. Para ver qué tipo concreto llega en cada caso, este programa hace cuatro peticiones a rutas del laboratorio y escribe la clase de la excepción y su mensaje:

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

Cuatro caminos, cuatro resultados distintos, y una lección en cada uno. `/facturacion` **sí contestó**: la respuesta es un `503`, y no hay excepción: un servidor caído a menudo no lo parece, porque responde que no puede. El puerto 8088, donde nadie escucha, dio `ConnectException` («no se pudo conectar») y su mensaje es **`null`**: la excepción más común de la red no trae un texto que sirva. `/notificaciones` hace esperar al cliente 10 segundos y el tiempo límite era de 1: `HttpTimeoutException`. Y `/basura` abre la conexión y la cierra sin decir nada: `IOException`, a secas, con un mensaje que habla del interior del cliente (`HTTP/1.1 header parser received no bytes`).

De ahí salen dos reglas. La primera, que el `revisor` traduce cada tipo a **uno de tres detalles fijos** y nunca muestra el mensaje de la excepción, por dos razones: a veces es `null`, y cuando existe cambia entre versiones del JDK y entre sistemas operativos. La tabla de traducción es la que ya conoces desde las primeras lecciones:

| La excepción | El detalle | Qué pasó |
|---|---|---|
| `HttpTimeoutException` (incluye `HttpConnectTimeoutException`) | `tiempo agotado` | se acabó el tiempo, de conexión o de petición |
| `ConnectException` | `sin conexión` | puerto cerrado, o nombre que no existe |
| cualquier otra `IOException` | `error de red` | la conexión se cortó o la respuesta no se entendió |

La segunda regla es de **orden**. `HttpTimeoutException` y `ConnectException` son, las dos, tipos de `IOException`: son casos particulares de ella. Cuando una excepción puede atraparse por su tipo particular o por el general, Java usa el primer `catch` que le sirva, así que **el tipo particular va antes que el general**. Si los pones al revés, el compilador no te deja:

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

`exception ConnectException has already been caught`: el `catch (IOException)` de arriba ya atrapa todas las `ConnectException`, de modo que el de abajo nunca se ejecutaría. Por eso se ordena siempre de lo particular a lo general. Con esta tabla y esta regla, el método central del `revisor` queda así:

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

Léelo por etapas. Se arma la petición, con el tiempo límite **del servicio** (`timeout`). Se toma el reloj. Se envía; si hubo respuesta, se mide el tiempo y se construye un `Respondio` con el código y los milisegundos (el `/ 1_000_000` convierte los nanosegundos). Y cada falla se vuelve un `SinRespuesta` con su detalle. Al final, el estado no se decide aquí: se le pasa la respuesta, sea cual sea, al `Clasificador` de la Lección 3, que no sabe nada de redes. Dos propiedades importan más que las demás. La primera: **`revisar` no deja escapar ninguna `IOException`**; una falla de la red es un resultado, no un error del programa (la misma idea que la Lección 5 llamó «resultado contra excepción»). La segunda: la `InterruptedException` **sí sube**, como se aprendió, porque quien interrumpe al hilo tiene derecho a que el programa pare.

#### 8.3.3 Redirecciones: `3xx` es una respuesta

Falta una de las cuatro costumbres del mundo real. A veces un servidor responde con un código `3xx` y un encabezado `Location` que dice «ahora estoy en esta otra dirección». Un navegador la sigue sin que lo notes. Un cliente HTTP puede hacer lo mismo si se le pide. Este programa levanta su propio servidor, en un puerto libre que el sistema elige, con una ruta que redirige a otra, y consulta con dos clientes:

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

El cliente por omisión **no** siguió la redirección: contestó `302`, el código de la respuesta que recibió. El que se configuró con `Redirect.NORMAL` fue a la dirección nueva y entregó el `200` final. El `revisor` es del primer tipo, y la regla de clasificación lo dice sin ambigüedad: un `3xx` es una **respuesta**, no se sigue, y no es un error. Un servicio que redirige está vivo. Hay además una razón de seguridad más seria que se explica en 8.8: una dirección que el archivo permitió no tiene por qué llevar a una que el archivo no permitió.

#### 8.3.4 `revisarTodos` y la prueba contra un servidor de verdad

Con `revisar` hecha, `revisarTodos` se vuelve corto: un cliente, un `for`, el orden de la lista.

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

Es **en serie** a propósito: cada servicio se revisa cuando el anterior terminó. La razón de que haya un `try (HttpClient cliente = nuevoCliente())` alrededor es el cierre: al terminar el bloque, `close()` espera a que no haya peticiones en curso y libera todo. Y el costo de la serie es visible, y es el tema de la siguiente lección: 142 + 2300 + 87 milisegundos es, grosso modo, lo que tarda el reporte.

Falta vigilar `revisar` con pruebas. Aquí hay una dificultad que la Lección 7 no tenía: la prueba necesita **un servidor que se porte como queramos** (que conteste rápido, que falle, que se quede callado), y no se puede depender del laboratorio, que puede estar apagado en otra computadora. La solución es que la prueba levante el suyo, con el mismo `HttpServer` del JDK del laboratorio, **dentro de la prueba**, en un puerto que el sistema elige:

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

Tres ideas nuevas. **El puerto 0.** Pedir `InetSocketAddress("127.0.0.1", 0)` significa «cualquier puerto libre»; el sistema elige uno, y la prueba lo lee después con `servidor.getAddress().getPort()`. Así nunca choca con otro programa. **`@BeforeAll` y `@AfterAll`** son anotaciones de JUnit para código que corre **una vez** antes de todas las pruebas de la clase y una vez después de todas; levantar y apagar el servidor en cada prueba sería lento. Los métodos son `static` por una regla de JUnit. Y **cinco pruebas, una por camino**: respuesta rápida (ARRIBA), un `503` (CAIDO), un puerto cerrado (`sin conexión`), una ruta que tarda 2 segundos con un tiempo límite de 200 milisegundos (`tiempo agotado`), y una conexión que se cierra sin contestar (`error de red`). El puerto cerrado se obtiene de forma ingeniosa: se abre un socket en el puerto 0, se anota qué puerto le tocó, y se cierra; ese número ya sabemos que está libre. (El caso LENTO no se prueba aquí; ya lo vigila `ClasificadorTest`, y esperar un segundo de verdad en una prueba la volvería lenta sin enseñar nada nuevo.) Una línea del servidor merece una nota: `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` pide que cada petición se atienda en su propio hilo, para que la ruta lenta no bloquee a las otras; el concepto se explica en la Lección 9, y por ahora basta con saber que sin esa línea las rutas se atenderían una por una.

Como siempre, **primero se ve en rojo**. En `Revision.java`, quita la línea del `.timeout(...)` de la petición y corre `mvn -B verify`:

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

Sin el tiempo límite, la ruta `/lento` hace esperar al cliente sus 2 segundos completos, la respuesta llega (un `200`, tardío) y se clasifica LENTO, en lugar de CAIDO. La prueba espera `CAIDO`, y se pone roja con un mensaje que dice justo lo que está mal. Es la misma falla que, en producción, no pasaría en una prueba rápida sino con un servicio de verdad colgado, y que se vería como un reporte que nunca termina. Restaura la línea.

#### 8.3.5 El fin de la primera sesión

Tienes todo para que el `revisor` consulte de verdad. En tu carpeta, cambia dos cosas: borra `Simulador.java` (su trabajo lo hace la red), y haz que las direcciones de `servicios.txt` apunten al laboratorio (`http://127.0.0.1:8089/catalogo`, y así con los demás; `reportes` va a `http://127.0.0.1:8088/`). Copia `Revision.java` y `RevisionTest.java` de arriba. Con el laboratorio encendido en otra terminal:

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

Son 23 pruebas: las 18 de la lección anterior más las cinco nuevas de `RevisionTest`. Las de `ConfiguracionTest` siguen siendo las del archivo de texto; en la segunda sesión se reescriben. Y el `revisor` ya consulta de verdad:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      92ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 861ms
```

Dos cosas para notar. El ESTADO y el DETALLE son idénticos a los de la lección anterior, y la columna TIEMPO ya no es la que escribiste: la midió tu computadora. Y el programa tardó, grosso modo, la suma de todos (un poco más de 2.5 segundos), no el más lento: es el costo de revisar en serie. Hay una prueba más de que consulta de verdad: apaga el laboratorio y vuelve a correr; los cuatro servicios salen CAIDO con `sin conexión`, y el promedio es una raya, porque nadie respondió. Esto cierra la primera sesión.


### 8.4 JSON: por qué el texto plano ya no alcanza

#### 8.4.1 Lo que un archivo de texto no puede decir

El `servicios.txt` de la Lección 5 tenía un servicio por renglón, con campos separados por espacios, y el tercero, opcional, era el tiempo límite. Para cuatro servicios es cómodo. Pero hazle tres preguntas. ¿Cómo distingue el número `1000` del texto `"1000"`? No lo distingue: son los mismos cuatro caracteres, y el programa los convierte porque *sabe* que ahí va un número. ¿Cómo agrega un campo que algunos servicios tienen y otros no, en el medio? No puede, sin romper la posición de los demás. ¿Cómo dice que un servicio tiene una lista de cosas, como etiquetas o varios responsables? Tampoco. Cada vez que el archivo necesita decir algo nuevo hay que inventar una convención y volver a escribir el lector.

**JSON** (*JavaScript Object Notation*, aunque hoy lo usan todos los lenguajes) resuelve eso con una idea sencilla: un formato de texto que **lleva sus tipos escritos**. Según su [norma, la RFC 8259](https://www.rfc-editor.org/rfc/rfc8259), un valor JSON es una de estas cosas: un **objeto** (`{"nombre": "pagos"}`, pares llave-valor entre llaves), un **arreglo** (`[1, 2, 3]`, una lista entre corchetes), un **texto** entre comillas dobles, un **número**, o uno de tres valores fijos: `true`, `false` y `null` («no hay valor»). Eso es todo el formato, y de ahí viene que se pueda leer desde cualquier lenguaje. Y nota lo que cambió: `1000` (sin comillas) es un número y `"1000"` (con comillas) es un texto, y el formato los distingue desde el principio.

Este es el archivo de configuración del `revisor` desde hoy, el `servicios.json`, con los mismos cuatro servicios de siempre apuntando ahora al laboratorio:

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

Es un objeto con una llave, `servicios`, cuyo valor es un arreglo de objetos, y cada objeto lleva `nombre` y `url`. Hay un tercer campo posible, `timeout_ms`, el tiempo límite de la petición en milisegundos: es **opcional**, y quien lo omita obtiene los 5000 milisegundos de siempre. Las llaves van en `snake_case` (palabras en minúscula unidas por guion bajo, como en `timeout_ms`) porque son las mismas que usa el [curso de Python](https://github.com/HabilMX/curso-python): el archivo y el reporte se ven igual en los dos. Con este archivo, **`servicios.txt` se borra**: ya no se usa.

Por último, el JSON también ocupa un lugar en la **salida** del `revisor`: el reporte que entregará el programa para que otro programa lo lea (el curso lo fija y no cambia de aquí en adelante). Lo verás en 8.7.

### 8.5 Jackson 3: la dependencia, los paquetes y el *classpath*

#### 8.5.1 Qué es Jackson, y por qué hay dos

Java no trae un lector de JSON en su biblioteca estándar; se usa una biblioteca, y la más usada es **Jackson**. Aquí hay una trampa que se paga una vez: Jackson tuvo una versión 2, que domina casi todo lo que hay escrito en internet (respuestas viejas, tutoriales, libros), y desde 2025 tiene una **versión 3**, que cambió su nombre de paquete. La 2 vive en `com.fasterxml.jackson...`; la 3, en **`tools.jackson...`**. Si copias un ejemplo de internet y su `import` empieza con `com.fasterxml.jackson.databind`, es de la versión 2, y con el proyecto de este curso no compila. Se puede ver en dos renglones:

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

`package com.fasterxml.jackson.databind does not exist`, y detrás una cascada de `cannot find symbol` que no son más que la consecuencia del primero. El mensaje es el de siempre cuando falta una dependencia (lo viste en la Lección 6), pero aquí la dependencia de Jackson **sí está declarada**; lo que no existe en tu proyecto es la versión 2. Cuando un ejemplo de Jackson no compile, revisa primero qué paquete importa. La [guía oficial de migración a Jackson 3](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) lista los cambios: el paquete nuevo, el JDK mínimo (Java 17), los mapeadores inmutables que se construyen con un *builder*, y que las excepciones ya no son verificadas. Un único detalle queda a medias: las **anotaciones** (`@JsonProperty` y compañía) siguen en `com.fasterxml.jackson.annotation`, con versión 2.x; esa es la razón de que más abajo veas un archivo `jackson-annotations-2.22.jar` junto a los de la versión 3. El curso usa la 3.2.3, la última a octubre de 2026.

#### 8.5.2 Declarar la dependencia

Una **dependencia** es una biblioteca que tu proyecto usa y no escribiste. Se declara en el `pom.xml` con sus coordenadas, igual que declaraste JUnit, pero con dos diferencias: sin alcance (el alcance por omisión es `compile`, «la necesita el programa para compilar y para correr») y con versión escrita. Este es el `pom.xml` del proyecto de la lección, completo; lo nuevo es la dependencia de Jackson y un segundo *plugin*, que se explica enseguida:

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

La dependencia, `tools.jackson.core:jackson-databind`, trae otras por su cuenta: Maven lee el `pom.xml` de Jackson, ve que necesita `jackson-core` y `jackson-annotations`, y las descarga también. A eso se le llama **dependencias transitivas**, y es el motivo por el que se escribe una línea y se descargan tres JARs. La versión subió a `0.8.0`, y la de JUnit sigue importada del mismo BOM.

#### 8.5.3 El *classpath* y por qué `target/lib`

Compilar con Maven funciona, porque Maven sabe qué dependencias hay y se las pasa a `javac`. Pero **ejecutar** es otra cosa, y es donde aparece el error más típico de la lección. Con el comando de la lección anterior:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError: tools/jackson/core/JacksonException`. La JVM empezó a ejecutar el programa, llegó a una línea que necesita una clase de Jackson, y no la encontró. Recuerda lo que significaba `-cp`: la lista de lugares donde la JVM busca clases, el ***classpath***. En las Lecciones 6 y 7 bastaba con `target/classes`, porque todo el código era tuyo. Ahora hay que agregar los JARs de Jackson. Maven los tiene guardados en `~/.m2/repository`, pero esa ruta es larga y depende de la versión; para no escribirla, el `pom.xml` agrega el *plugin* `maven-dependency-plugin`, que en la fase `package` **copia las dependencias del programa a `target/lib`**:

```text
$ ls target/lib
jackson-annotations-2.22.jar
jackson-core-3.2.3.jar
jackson-databind-3.2.3.jar
```

Y se ejecuta con ambas cosas en el *classpath*, `target/classes` (tu código) y `target/lib/*` (los JARs; el `*` pide «todos los `.jar` de esa carpeta»), separados por dos puntos, que es el separador en Linux:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
```

Las comillas son para que la terminal no intente expandir el `*` ella misma. Y como los JARs se copian en la fase `package`, hay que haber corrido `mvn -B verify` (o `package`) al menos una vez para que `target/lib` exista. Esto es lo que molesta de un *classpath* a mano, y por eso la [Lección 10](10-el-programa-terminado.md) lo resuelve con un solo `.jar` que lleva todo dentro: ya lo sufriste, así que sabrás qué problema resuelve.

### 8.6 Leer un JSON como árbol y validar a mano

#### 8.6.1 `JsonNode`: el árbol

Jackson ofrece dos formas de leer un JSON. La primera lo convierte en un **árbol** de objetos `JsonNode`, uno por cada valor, que puedes recorrer y preguntar. La segunda lo mapea directo a un *record* tuyo. Empecemos por la primera, que es la que usa la configuración:

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

Los nombres de los métodos son los de Jackson 3 (y vale la pena fijarse, porque casi todo lo que encuentres en internet usa los de la 2): `readTree` convierte el texto en el árbol y devuelve su raíz; `get("servicios")` pide el valor de una llave de un objeto, y `get(1)` el elemento de un arreglo; `isArray()` pregunta qué tipo de valor es; `size()` cuenta los elementos; `stringValue()` entrega el texto de un nodo de texto y `intValue()` el número entero de uno numérico. Los nombres viejos (`asText`, `isTextual`) siguen existiendo para quien migra, pero el código nuevo usa `stringValue` e `isString`. Y hay un detalle que decide muchos errores: `get("timeout_ms")` sobre una llave que **no existe** devuelve `null` de Java, no un nodo; para preguntar sin riesgo de `NullPointerException` existe `path("timeout_ms")`, que devuelve un nodo «ausente» (`isMissingNode()`) en lugar de `null`. Lo que el `revisor` hace con eso es siempre revisar primero si es `null` y después preguntar el tipo.

#### 8.6.2 La trampa: mapear directo a un *record* acepta lo que no debe

La segunda forma es tentadora, porque son dos renglones y los tipos de Java se ocupan de todo: declaras un *record* con los campos que quieres y Jackson lo llena.

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

Mira las cinco filas. `1000` entra como 1000, bien. Pero **`1.5` entra como `1`**: Jackson recorta el decimal en silencio. **`"1000"` entra como `1000`**: convierte el texto en número sin avisar. **`-5` entra como `-5`**: un tiempo límite negativo, que `HttpRequest` rechazaría con una excepción cuando el programa lo use, lejos de donde se escribió. Solo `true` se rechazó, con una excepción de Jackson (`MismatchedInputException`). El punto no es que Jackson esté mal: es que hace lo que se le pidió, que era *convertir*, y no lo que se necesita, que es *verificar*. Un archivo de configuración con `"timeout_ms": "1000"` casi seguro es un error de quien lo escribió, y se quiere saber.

La lección es general: **lo que llega de fuera se valida donde entra**, una vez, con reglas explícitas, y después de esa validación el resto del programa trabaja con tipos que ya son de confianza (`Servicio` tiene un `int` y un `URI`, no un texto que quizá era un número). Por eso la configuración se lee como **árbol** y se valida a mano con `isInt()`, `isString()` y `isObject()`, y el mapeo directo a *records* se queda para **escribir** (8.7), donde los datos los controla el programa y no una persona.

#### 8.6.3 La configuración, validada

La clase `Configuracion` se reescribe para leer JSON. Léela como una cascada: `leer` abre el archivo y valida la forma general, `leerServicio` valida un servicio, y cada campo tiene su pequeño método.

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

Tres decisiones valen un comentario. **La primera: cada falla es un `ErrorDeConfiguracion` con un mensaje que dice qué arreglar y dónde.** `servicio 2: falta 'nombre'` es un mensaje que una persona puede usar; un `NullPointerException` en la línea 57 no. La posición (`servicio 2`) cuenta desde 1, como la cuenta una persona. **La segunda: los errores de Jackson se traducen.** `JacksonException` es una excepción **no verificada** en la versión 3 (una de las diferencias con la 2), así que el compilador no te obliga a atraparla, y por eso es fácil olvidarla y dejar que un archivo mal escrito tumbe el programa con una traza. El `catch` la convierte en `ErrorDeConfiguracion`, y de paso aprovecha que la excepción sabe en qué renglón y columna del archivo se rompió (`getLocation()`). **La tercera: el archivo se lee primero a texto con `Files.readString`**, con el manejo de errores de la Lección 5 (`NoSuchFileException` se vuelve «no existe el archivo»), y recién entonces se le pasa el texto a Jackson. Cada error tiene un solo origen, y cada cual se trata en su lugar.

Estos son los mensajes que produce. La posición cuenta desde 1. Las pruebas de más abajo vigilan los más peligrosos; con los demás, los provocas tú en el ejercicio 3:

| Caso | Mensaje |
|---|---|
| no existe el archivo | `no existe el archivo servicios.json` |
| JSON mal escrito | `servicios.json no es JSON válido: renglón 4, columna 5` |
| falta la lista | `falta la lista 'servicios'` |
| el servicio no es un objeto | `servicio 2: debe ser un objeto` |
| falta un campo | `servicio 2: falta 'nombre'` |
| nombre vacío o que no es texto | `servicio 2: 'nombre' debe ser un texto que no esté vacío` |
| url que no es `http`/`https` | `servicio 2: 'url' debe empezar con http:// o https://` |
| url sin servidor o mal escrita | `servicio 2: 'url' no es válida` |
| servidor fuera de la lista permitida | `servicio 2: el servidor 169.254.169.254 no está en la lista permitida` |
| tiempo límite inválido | `servicio 3: 'timeout_ms' debe ser un entero mayor que 0` |
| nombres repetidos | `nombres repetidos: catalogo` |

Con un archivo al que le falta una coma entre dos servicios (aquí, la del final del renglón de `catalogo`), el `revisor` dice exactamente dónde:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

`renglón 4, columna 5`: el punto donde Jackson esperaba una coma y encontró la `{` que abre el siguiente servicio. Con un archivo vacío, dice lo que falta y no una traza; y con una lista de textos donde debía haber objetos, dice cuál servicio es el que está mal:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

La clase de pruebas se reescribe con los casos del JSON. Es la misma estructura de la Lección 7, con una novedad: la tabla de casos ahora son archivos JSON de varias líneas, y una tabla de `@CsvSource` con eso sería ilegible. JUnit tiene otra fuente de datos, **`@MethodSource`**, que apunta a un método (aquí `archivosInvalidos`) que devuelve un `Stream<Arguments>`, una lista de filas donde cada `Arguments.of(...)` trae los valores de una. Los bloques de texto (`"""`) de la Lección 2 permiten escribir cada JSON como lo ves en un archivo.

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

Siete pruebas: el caso feliz y los seis rechazos más peligrosos: una URL que no es `http`, un servidor fuera de la lista (el `169.254.169.254` de 8.8), los dos tiempos límite que el mapeo directo habría dejado pasar (`1.5` y `"1000"`), un servicio sin nombre y nombres repetidos. Los dos del tiempo límite son la razón de ser de la sección anterior: si alguien «simplifica» la validación a un mapeo directo, esas dos filas se ponen rojas.

### 8.7 Escribir el reporte como JSON

Escribir es la otra dirección, y aquí sí conviene que Jackson trabaje por ti, porque los datos los produce el programa y no una persona. Se le entrega un *record* y devuelve el texto. Este es el ejemplo mínimo, con los nombres que se mostraron antes de la validación:

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

`writerWithDefaultPrettyPrinter()` produce el texto con sangría; sin él sale todo en una sola línea (que es lo que prefiere una máquina). La opción `SNAKE_CASE`, que se pone **una sola vez** al construir el mapeador, hace que `tiempoMs` salga como `tiempo_ms`: el *record* se escribe con el estilo de Java (`camelCase`) y el JSON con el del contrato. Y mira el servicio que no respondió: su `tiempo_ms` es `null`, no cero y no ausente. Eso es información: significa «no se midió», y es el campo de tipo `Long` (con mayúscula, el envoltorio de la Lección 2, que admite `null`) y no `long` quien lo permite. Los espacios alrededor de los dos puntos (`"nombre" : "pagos"`) son los del formateador de Jackson, que no son los de otros lenguajes; son válidos y cualquier lector de JSON los acepta.

En el `revisor` hay tres piezas. La primera, `Json`, es **el** mapeador del programa, uno solo, configurado una vez (son objetos pensados para reutilizarse):

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

La segunda, `ReporteJson`, es la **forma** del JSON, separada a propósito de los tipos internos del programa. `Reporte`, `Resultado` y `Resumen` son las clases con las que el programa trabaja; `ReporteJson` es el contrato con quien lea el reporte. Si mañana cambias algo interno (cómo se guardan los tiempos, un nombre), el JSON no cambia a menos que lo decidas tú, y no por accidente:

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

El método `de` convierte un `Reporte` en su forma de JSON, y la conversión de `OptionalLong` a `Long` (`nulo`) es la que produce el `null` de los servicios sin respuesta. El constructor compacto con `List.copyOf` es la copia defensiva de la Lección 4; la Lección 10 muestra qué dice (y qué no dice) una herramienta de análisis sobre ella. La tercera pieza es `Formato`, que gana un método, `json`, y es el único cambio de un archivo cuyo resto ya conoces:

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

El `revisor` todavía no ofrece una opción para pedir ese JSON (llega con la línea de comandos de la Lección 10), pero puedes verlo cambiando temporalmente en `Revisor.java` la línea que imprime la tabla por `System.out.println(Formato.json(reporte));`. Así empieza lo que imprime:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
{
  "servicios" : [ {
    "nombre" : "catalogo",
    "url" : "http://127.0.0.1:8089/catalogo",
    "estado" : "ARRIBA",
```

Es el JSON que el curso fija para el reporte del `revisor`, el mismo del [curso de Python](https://github.com/HabilMX/curso-python): `servicios` es una lista con una entrada por servicio, `tiempo_ms` es `null` para el que no respondió, y `resumen` repite las cuentas. Restaura la línea de la tabla antes de seguir.

### 8.8 SSRF: la URL la escribe otra persona

#### 8.8.1 El problema, con un ejemplo concreto

Mira de nuevo `servicios.json`. Cada `url` es una dirección a la que **tu programa se conecta, desde tu red, con los permisos de tu máquina**. Imagina que hoy el archivo lo escribes tú, pero que mañana alguien arma una pantalla donde otra persona agrega servicios, o que un archivo de otro equipo se copia a ese lugar. Esa persona ya no escribe un dato: escribe **una instrucción para tu servidor**. ¿Qué dirección pondría un atacante? Una a la que él no puede llegar desde fuera pero tu servidor sí. En la nube existe una muy conocida: `http://169.254.169.254/latest/meta-data/`. Es una dirección «de enlace local», de las que no salen de la red de la propia máquina, que en AWS (y con mecanismos parecidos en otras nubes) contesta con los datos del servidor y, en algunas configuraciones, con credenciales temporales; la [documentación de AWS](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html) la describe. Un `revisor` sin defensas conectaría, diría `ARRIBA`, y habría hecho lo que el atacante quería sin que nadie tocara el servidor.

A esa clase de falla se le llama **SSRF** (*Server-Side Request Forgery*, «falsificación de peticiones desde el servidor»): conseguir que un servidor haga una petición a donde no debía. Es el décimo puesto de la [lista OWASP Top 10 de 2021](https://owasp.org/Top10/A10_2021-Server-Side_Request_Forgery_%28SSRF%29/), el catálogo de riesgos de aplicaciones web más citado, y tiene su entrada en el catálogo de debilidades de software, [CWE-918](https://cwe.mitre.org/data/definitions/918.html). El `revisor` no lee el cuerpo de las respuestas, lo que reduce el daño (el atacante sabría que algo contestó, no qué dijo), pero aun así revela qué existe dentro de la red, y un programa que algún día leyera ese cuerpo lo entregaría.

La defensa no es una sola cosa, es una pila de cuatro, y cada una cubre lo que la anterior deja pasar. Es lo que se llama *defensa en profundidad*.

#### 8.8.2 Primera defensa: el esquema

Una URL empieza con un **esquema**, la parte antes de los dos puntos: `http`, `https`, `ftp`, `file`. El `revisor` solo habla HTTP, así que acepta dos, y lo comprueba con `URI.getScheme()`, como viste en `Configuracion`. El resto de esquemas, algunos peligrosos (`file:///etc/passwd` apunta a archivos de la propia máquina), se rechazan. El `HttpClient` ya es cauto por su cuenta, y lo muestra la última línea de este programa, que también enseña cómo lee Java una dirección:

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

`file:` no pasa ni el constructor de la petición (`invalid URI scheme file`). Pero no confíes solo en eso: el `revisor` rechaza el esquema antes, con un mensaje claro, en la configuración.

#### 8.8.3 Segunda defensa: la lista permitida de servidores

Con esquema `http` todavía queda `http://169.254.169.254/`. La segunda defensa es la que más pesa: una **lista permitida** de servidores. En vez de intentar adivinar qué direcciones son peligrosas (hay demasiadas, y siempre se te olvida una), se enumeran las únicas que se aceptan y se rechaza todo lo demás:

```java
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");
```

Hoy la lista es `127.0.0.1` y `localhost`: la propia máquina, donde vive el laboratorio. En la Lección 10 la lista se vuelve configurable desde la línea de comandos. Verifica el rechazo con un archivo que apunta al servidor peligroso:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

Antes de que el programa mande un solo paquete, la configuración ya se negó, con un mensaje que dice qué servidor y por qué. Fíjate en cómo se compara: **con `url.getHost()`, el servidor que el analizador de URI extrajo, y por igualdad exacta**. Mira lo que devuelve esa función en el programa anterior para direcciones traviesas: `http://127.0.0.1@169.254.169.254/latest/meta-data/` parece, a simple vista, una dirección de `127.0.0.1`; pero para HTTP, lo que va antes de una arroba es un usuario (¡el usuario `127.0.0.1`!), y el servidor es `169.254.169.254`. Una validación hecha con `texto.startsWith("http://127.0.0.1")` dejaría pasar justo ese ataque; la hecha con `getHost()` lo rechaza. Y `http://127.0.0.1.otro.example/` empieza con la IP pero es otro servidor, y `http://127.0.0.1:puerto/` ni siquiera tiene servidor reconocible (el `revisor` lo rechaza como «no es válida»). La regla: **nunca valides una dirección con operaciones de texto; analízala con el analizador de URI y compara sus partes.**

La lista distingue mayúsculas, y rechaza cosas válidas, como `http://LOCALHOST:8089/` o la forma de IPv6 de la propia máquina, `http://[::1]:8089/`:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

Eso es deliberado: una lista permitida debe ser **estricta por omisión**. Si algún día se necesita aceptar otra forma, se agrega a la lista a propósito, y no se relaja la comparación.

#### 8.8.4 Tercera defensa: no seguir redirecciones

Un servidor que sí está en la lista puede responder con una redirección a uno que no lo está. Si el cliente la siguiera, la lista permitida quedaría saltada por la puerta de atrás: tú autorizaste `127.0.0.1`, y el cliente terminaría en `169.254.169.254`. Por eso el cliente del `revisor` usa `Redirect.NEVER` explícito (8.2.1) y trata un `3xx` como una respuesta más (8.3.3): lo que no se sigue no se puede aprovechar en tu contra. Aviso honesto: **ninguna de las 25 pruebas vigila esta defensa**. Se midió: cambiar `NEVER` por `ALWAYS` en `Revision.java` deja las 25 en verde. La vigila el programa de 8.3.3, que no es parte del proyecto. Escribir esa prueba (un servidor de prueba que redirija a otro) es un buen ejercicio de cierre.

#### 8.8.5 Cuarta defensa, y lo que esto NO resuelve

La cuarta es mínima: el tiempo límite de cada petición. Un servidor hostil puede contestar muy lento a propósito para mantener ocupado a tu programa; el tiempo límite acota cuánto puede abusar de él.

Y ahora lo honesto, porque la seguridad que se oculta se vuelve falsa: **estas cuatro defensas son un buen comienzo, no un muro.** La lista permitida compara *nombres*, no direcciones reales. `localhost` es un nombre que tu sistema traduce a una dirección consultando el archivo `/etc/hosts` o un servidor de nombres; un servidor de nombres hostil puede contestar una dirección distinta de la esperada (el ataque se llama *DNS rebinding*). Para un servicio que sale a internet con un servidor de nombres que no controlas, la defensa completa consiste en resolver la dirección, comprobar que no pertenece a un rango interno o especial, y conectarse a *esa* dirección y no al nombre, además de aislar la red del servidor para que lo interno no sea alcanzable ni aunque la validación falle. Esa es la última línea de defensa y es de infraestructura, no de código. La [hoja de trucos de OWASP sobre SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) describe las dos capas y por qué ninguna alcanza sola. El `revisor` de este curso trabaja en un entorno local que tú controlas: su lista permite `127.0.0.1` y `localhost`, y en tu máquina `localhost` lo traduce tu archivo `/etc/hosts` a la misma dirección. Ahí, la lista de nombres alcanza; fuera de un entorno así, aceptar un nombre como `localhost` no es una defensa completa, por lo que se acaba de explicar; lo importante de la lección es el hábito: **toda dirección que escribió otra persona es sospechosa hasta que pasa por una lista de lo permitido.** Hay un artículo de Hábil sobre [seguridad de APIs: identidad, puerta de enlace y tokens](https://www.habil.mx/es/blog/seguridad-apis-cto-identidad-gateway-tokens/) que sigue el tema desde la arquitectura.

### 8.9 El paso del proyecto: el `revisor` consulta de verdad

Todas las piezas están. Este es el proyecto como queda al terminar la lección:

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

Ya viste completos `pom.xml`, `servicios.json`, `Json`, `Configuracion`, `Revision`, `ReporteJson`, `Formato`, `ConfiguracionTest` y `RevisionTest`. El único que falta es el `Revisor`, que cambia dos cosas: lee `servicios.json` y le pasa a `Configuracion.leer` la lista permitida.

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

Los once archivos que no cambiaron respecto a la Lección 7 se incluyen para que el proyecto se pueda armar sin mirar otra lección:

<details>
<summary>Los once archivos que no cambiaron</summary>

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

Construye y comprueba el conteo. Las pruebas del proyecto no necesitan el laboratorio (`RevisionTest` levanta su propio servidor), pero la corrida de después sí:

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

`Tests run: 25`: 18 de la Lección 7, menos las 5 de `ConfiguracionTest` que leían texto, más las 7 nuevas que leen JSON, más las 5 de `RevisionTest` (18 − 5 + 7 + 5). Anótalo en tu bitácora. Si no sale 25, algo se movió, y es una pregunta que vale la pena contestar antes de seguir. Y la corrida, con el *classpath* completo:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

La tabla de siempre, ahora con tiempos medidos, y el resumen con un promedio que depende de ellos.

**Lo que estas 25 pruebas no vigilan.** Una cifra verde no dice qué cubre. Para saberlo se rompió, de uno en uno, el código que sigue, y se midió si alguna prueba se ponía roja. Se pusieron rojas, y por eso están vigilados: el rechazo de esquemas, de servidores fuera de la lista, de `timeout_ms` inválido y de nombres repetidos; la traducción de `ConnectException` y de `HttpTimeoutException`; y el tiempo límite de la petición. **Siguieron en verde**, o sea, sin vigilancia: el mensaje `'nombre' debe ser un texto que no esté vacío`, el de `falta la lista 'servicios'`, el `connectTimeout` del cliente, la política `Redirect.NEVER` y la conversión a `null` del tiempo en `ReporteJson` (esta última la vigila el contrato de la Lección 10). No es un defecto de la lección, es la regla: cada prueba nueva que agregues cierra uno de esos huecos, y los ejercicios 3 y 4 empiezan por ahí.


## El error que vas a ver

La lección tiene un error principal, el de la clase de Jackson que no aparece, y varios mensajes de apoyo que conviene saber leer de una mirada.

**El error principal.** Corres el `revisor` con el comando de las Lecciones 6 y 7, que no incluye los JARs de Jackson:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError` (el nombre de la clase que falta aparece con barras, `tools/jackson/core/JacksonException`) no es un error de compilación. El programa **compiló** bien, porque Maven sí conocía la dependencia; falla al ejecutarse, porque la JVM no sabe dónde buscarla. La causa que sigue (`Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException`) lo confirma: esa clase no está en ninguno de los lugares del *classpath*. La solución es ejecutar con `-cp "target/classes:target/lib/*"` (y haber corrido antes `mvn -B verify`, que es quien copia los JARs a `target/lib`). Si alguien te pregunta «¿compila pero no corre?», la respuesta casi siempre es el *classpath*. Y su pariente cercano, el de 8.5.1, es copiar un ejemplo de Jackson 2: `package com.fasterxml.jackson.databind does not exist`. Se arregla cambiando `com.fasterxml.jackson` por `tools.jackson` en el `import` y revisando los nombres de los métodos.

**Los mensajes de apoyo**, para reconocerlos de una mirada:

- `java.net.BindException: Address already in use` al arrancar el laboratorio: otro programa usa el puerto 8089, o dejaste un laboratorio anterior corriendo. Ciérralo con `Ctrl`+`C` en su terminal.
- Todos los servicios con `sin conexión`: el laboratorio no está encendido. Enciéndelo en otra terminal y vuelve a correr.
- `exception ConnectException has already been caught`: pusiste un `catch` del tipo general antes del particular. De lo particular a lo general.
- `ConnectException` con mensaje `null` (`fig08_03.java`): normal. Por eso el `revisor` nunca muestra el mensaje de una excepción de red.
- `error de configuración: servicios.json no es JSON válido: renglón 4, columna 5`: falta o sobra algo cerca de ese punto; casi siempre es una coma. Mira el renglón anterior al que se indica.
- `error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida`: la defensa funcionando. No es un fallo: es el aviso de que alguien pidió una dirección que el `revisor` no consulta.
- `invalid URI scheme file` (`fig08_09.java`): el `HttpClient` rechaza esquemas que no son HTTP; el `revisor` los rechaza antes, en la configuración.

## Lo que se hace mal

**Mandar una petición sin tiempo límite.** Es el error más caro de la lección porque no falla en las pruebas: funciona el día del ejemplo y cuelga el programa el día en que un servicio se queda callado. Toda petición lleva su `timeout`, y el cliente su `connectTimeout`.

**Crear un `HttpClient` por petición, o no cerrarlo.** Cada cliente guarda conexiones y recursos; uno por petición tira a la basura lo que el cliente ahorra al reutilizar, y no cerrarlo los deja ocupados. Uno por revisión, con `try-with-resources`.

**Mostrar `e.getMessage()` de una excepción de red.** A veces es `null`, a veces es un texto interno de la implementación, y cambia entre versiones y sistemas. Traduce el **tipo** a un detalle fijo y deja el mensaje fuera.

**Atrapar `Exception` «por si acaso» alrededor de la red.** Se traga también los errores de programación (un `NullPointerException` de tu propia lógica) y los disfraza de «error de red». Se atrapan los tipos concretos: `HttpTimeoutException`, `ConnectException`, `IOException`, en ese orden.

**Confiar en un JSON porque «lo escribo yo».** El archivo lo editará alguien más, o tú con prisa. Mapear directo a un *record* deja pasar `1.5` y `"1000"`, y un `-5` llega hasta una excepción lejos de donde se escribió. Se valida en la entrada, con mensajes que dicen qué arreglar.

**Validar una dirección con operaciones de texto.** `startsWith("http://127.0.0.1")` deja pasar `http://127.0.0.1@servidor-del-atacante/`. Se analiza con `URI` y se compara `getHost()` con una lista permitida.

**Una lista de lo prohibido en lugar de una de lo permitido.** Las direcciones peligrosas son demasiadas y nuevas cada año; las que necesitas son pocas y las conoces. Lo que no está en la lista se rechaza.

**Seguir las redirecciones a ciegas.** Una dirección autorizada que lleva a una no autorizada anula la lista. `Redirect.NEVER`, escrito.

**Pruebas que dependen de un servicio que no controlan.** Si `RevisionTest` consultara el laboratorio, fallaría en cualquier computadora donde no estuviera encendido. La prueba levanta su propio servidor, con comportamiento conocido, en un puerto libre.

**Medir tiempos con la hora del reloj.** `System.currentTimeMillis()` puede saltar con un ajuste del reloj; `System.nanoTime()` es la que sirve para medir intervalos.

## Ejercicios

### Ejercicio 1 — La dirección que no debe pasar, y sus primas

En tu `servicios.json`, cambia el segundo servicio a cada una de estas direcciones, **de una en una**, y para cada una **predice primero** qué imprime el `revisor`: (a) `http://169.254.169.254/latest/meta-data/`; (b) `http://localhost:8089/pagos`; (c) `http://[::1]:8089/pagos` (la dirección de «esta misma computadora» en la notación IPv6); (d) `http://LOCALHOST:8089/pagos`. Después corre cada una y compara. Anota en la bitácora la que no acertaste, y por qué la lista permitida de 8.8.3 se comporta como se comporta con ella.

### Ejercicio 2 — Un servicio que nunca contesta

Agrega a tu `servicios.json` tres servicios del laboratorio: `facturacion` (`/facturacion`), `notificaciones` (`/notificaciones`, con `"timeout_ms": 1000`) y `basura` (`/basura`). **Antes de ejecutar**, predice el estado y el detalle de cada uno, y cuántos arriba, lentos y caídos dirá el resumen. Después ejecuta. Por último, quita de `Revision.java` la línea del `.timeout(...)`, vuelve a ejecutar y explica qué pasó con `notificaciones`, cuánto tardó el programa, y qué habría pasado si el servicio no hubiera contestado nunca.

### Ejercicio 3 — Tres archivos mal escritos

Con el `servicios.json` original, rompe el archivo de tres maneras, una por vez, y **predice el mensaje** antes de ejecutar: (a) quita la coma del final del renglón del servicio `catalogo`; (b) deja el archivo completamente vacío; (c) cambia el valor de `servicios` por `["catalogo"]`, una lista de textos en lugar de objetos. Para (a), antes de ejecutar, anota en qué renglón y columna esperas que falle.

### Ejercicio 4 — Dos filas que el mapeo directo habría dejado pasar

Agrega dos casos a la tabla `archivosInvalidos` de `ConfiguracionTest`: un servicio con `"timeout_ms": -5` y otro con `"timeout_ms": true`. **Predice** el mensaje de cada uno y cuántas pruebas dirá el informe (pista: cada `Arguments.of` es una prueba). Ejecuta `mvn -B verify`. ¿Hubo que cambiar algo en `Configuracion.java` para que pasaran?

## Soluciones

### Solución 1

(a) La dirección del servicio de metadatos se rechaza antes de mandar nada:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

(b) `localhost` está en la lista permitida, así que `http://localhost:8089/pagos` pasa, y el `revisor` consulta al laboratorio como si fuera `127.0.0.1`. Fíjate en lo que cambió y en lo que no: el segundo servicio sigue llamándose `inventario` (solo cambiaste su dirección), pero ahora pregunta por la ruta de `pagos`, que contesta rápido, así que sale ARRIBA y no LENTO:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     180ms  200
inventario  ARRIBA      92ms  200
pagos       ARRIBA      91ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 3 arriba · 0 lentos · 1 caído · promedio 121ms
```

Es una lección pequeña sobre nombres: el nombre de un servicio es una etiqueta que escribe una persona, y la dirección es lo que de verdad se consulta. El `revisor` no comprueba que coincidan.

(c) y (d): **ninguna de las dos pasa**, y es deliberado. `[::1]` es el servidor `[::1]` para `getHost()`, y no está en la lista; `LOCALHOST` en mayúsculas tampoco, porque la comparación es exacta.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor LOCALHOST no está en la lista permitida
```

Si esperabas que (c) o (d) pasaran, es que pensaste en lo que *significan* (la misma computadora) y no en lo que *dicen* (otro texto). Una lista permitida compara textos, y se mantiene estricta a propósito: aceptar otra forma es cuestión de agregarla a la lista, con la cabeza fría, y no de relajar la comparación para que «casi coincida».

### Solución 2

**Predicción.** Los cuatro de siempre quedan igual: `catalogo` y `pagos` ARRIBA, `inventario` LENTO, `reportes` CAIDO con `sin conexión`. De los tres nuevos, `facturacion` responde `503`, CAIDO con detalle `503`; `notificaciones` espera más que su tiempo límite de un segundo, CAIDO con `tiempo agotado`; `basura` cierra la conexión sin contestar, CAIDO con `error de red`. Resumen: 7 servicios, 2 arriba, 1 lento, 4 caídos, y el promedio solo incluye a los que respondieron (`catalogo`, `inventario`, `pagos` y `facturacion`, que respondió aunque con error): unos 670 milisegundos, porque los 2.3 segundos de `inventario` pesan mucho entre cuatro.

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

El promedio es la suma de los cuatro que respondieron entre cuatro. (Los nombres largos desbordan la columna, un defecto que el formato de la lección 2 arrastra y que el ejercicio opcional de la Lección 10 resuelve.) Ahora, sin el `.timeout(...)`:

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

`notificaciones` ya no es `tiempo agotado`: sin tiempo límite, el cliente esperó los 10 segundos que el laboratorio se tarda, recibió el `200` y lo clasificó LENTO (`10.0s`). El programa completo tardó unos 13 segundos (medido con `time`: 12.9) y el promedio se disparó. Pero el laboratorio es amable: contesta al cabo de diez segundos. **Un servicio real que se queda callado no contesta nunca**, y entonces `send` no regresa, el programa se cuelga y ninguna otra revisión se completa. Por eso el tiempo límite de la petición nunca se quita: es lo único que acota el peor caso. Vuelve a poner la línea.

### Solución 3

(a) Sin la coma, Jackson llega al `{` de `inventario` esperando una coma o el cierre del arreglo, y falla ahí; el mensaje cae en el renglón 4 (el de `inventario`), columna 5, que es donde empieza esa llave, después de la sangría de cuatro espacios. Quien se equivocó dejó el defecto en el renglón 3, pero el analizador solo se da cuenta cuando ve lo que sigue.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

(b) Un archivo vacío es JSON inválido, pero el árbol que entrega Jackson para un texto vacío no es un error: es un nodo «ausente», y el `revisor` lo trata como «no tiene la lista»:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

(c) Una lista de textos tiene la forma de lista, pero cada elemento debería ser un objeto:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

### Solución 4

Las dos filas se agregan al final de la tabla: la última que ya existía pasa a terminar con coma, y detrás van las dos nuevas, con el mismo formato de las demás:

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

Los dos mensajes son el de siempre, `servicio 1: 'timeout_ms' debe ser un entero mayor que 0`: `-5` es un entero pero no es mayor que 0, y `true` no es un entero (`isInt()` es falso). Sin tocar `Configuracion.java`, porque la validación ya cubría esos dos casos desde el principio. Con el mapeo directo, `-5` habría pasado en silencio (lo viste en 8.6.2) y `true` se habría rechazado, pero con un error de Jackson y no con tu mensaje. El conteo sube de 25 a 27:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, ... in mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Cómo sé que lo logré

- [ ] `mvn -B verify`, desde la raíz de `~/curso-java/revisor/`, termina con `BUILD SUCCESS` y dice `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`, y sabes de dónde sale cada número (18 − 5 + 7 + 5).
- [ ] Con el laboratorio encendido, `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` imprime la tabla de cuatro servicios con ESTADO y DETALLE idénticos a los de esta lección, y un resumen con `2 arriba · 1 lento · 1 caído`.
- [ ] Sin el laboratorio encendido, los cuatro servicios salen CAIDO con `sin conexión`.
- [ ] Un servicio apuntado a `http://169.254.169.254/latest/meta-data/` hace que el `revisor` imprima `el servidor 169.254.169.254 no está en la lista permitida` sin haber mandado ninguna petición.
- [ ] Quitaste el `.timeout(...)` de `Revision.java` y viste que la prueba de `tiempo agotado` se pone roja; lo restauraste.
- [ ] Un `servicios.json` con una coma de menos produce `no es JSON válido: renglón N, columna M`, y sabes en qué renglón mirar.
- [ ] Puedes explicar por qué `"timeout_ms": "1000"` entra como 1000 con el mapeo directo y se rechaza con la validación a mano.
- [ ] Anotaste en tu bitácora qué mensaje de esta lección te costó más leer.

## Resumen

Responde sin mirar la lección:

1. ¿Qué dos tiempos límite hay en una petición con `HttpClient`, dónde se configura cada uno y qué lanza cada uno al acabarse?
2. ¿Por qué `HttpRequest` sin `timeout` es peligroso?
3. ¿A qué detalle del `revisor` corresponde cada una de estas excepciones: `HttpTimeoutException`, `ConnectException` y cualquier otra `IOException`? ¿Por qué se atrapan en ese orden?
4. ¿Por qué un `503` no es una excepción? ¿Y un `302`, con la política de redirecciones del `revisor`?
5. ¿Por qué se mide con `System.nanoTime()` y no con `currentTimeMillis()`?
6. ¿Qué diferencia hay entre `tools.jackson` y `com.fasterxml.jackson`? ¿Qué anotaciones no cambiaron de paquete?
7. ¿Qué es el *classpath* y por qué falta `target/lib` en `java -cp target/classes` aunque el programa compile?
8. ¿Por qué la configuración se lee como árbol y se valida a mano en lugar de mapearla directo a un *record*? Nombra dos valores que el mapeo directo deja pasar.
9. ¿Qué es SSRF y cuáles son las cuatro defensas del `revisor`? ¿Por qué `startsWith("http://127.0.0.1")` no es una defensa?
10. ¿Qué no resuelve la lista permitida de nombres? ¿Qué más haría falta en un servicio que sale a internet?

## Para leer más

- [Java SE 25, paquete `java.net.http`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/package-summary.html): el cliente HTTP del JDK completo, con las peticiones asíncronas, HTTP/2 y WebSocket que esta lección no usa. En inglés, como casi toda la documentación oficial. Consultado el 7 de octubre de 2026.
- [RFC 9110, «HTTP Semantics»](https://www.rfc-editor.org/rfc/rfc9110): la norma de los métodos, los códigos de estado y las redirecciones; la sección 15 es la lista de códigos de estado. Consultado el 7 de octubre de 2026.
- [Guía de migración a Jackson 3](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) y [el repositorio de `jackson-databind`](https://github.com/FasterXML/jackson-databind): qué cambió respecto a la versión 2 y cómo se usa la biblioteca. Consultado el 7 de octubre de 2026.
- [OWASP, «Server-Side Request Forgery Prevention Cheat Sheet»](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html): las defensas por capas contra SSRF y por qué una sola no alcanza. Consultado el 7 de octubre de 2026.

---

**Anterior:** [Lección 7 — Pruebas con JUnit](07-pruebas-junit.md) ·
**Siguiente:** [Lección 9 — Concurrencia: revisar todo a la vez](09-concurrencia.md)
