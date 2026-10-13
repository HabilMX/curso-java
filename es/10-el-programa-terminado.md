# Lección 10 — El programa terminado

**Tiempo:** 2 × 90 min (dos sesiones; el corte está entre las secciones 10.3 y 10.4)

**Qué construyes:** el `revisor` completo, listo para entregar: se maneja desde la línea de comandos, sale con un código que un guion puede leer, puede servir su reporte por HTTP, pasa las puertas de calidad y se empaqueta en un solo JAR

**Qué aprendes:** argumentos y códigos de salida como contrato; el servidor HTTP del propio JDK; el cierre ordenado con un gancho de apagado; Checkstyle y SpotBugs dentro de `mvn verify`, y cómo leer lo que encuentran; un repaso de la seguridad del `revisor`; y el JAR ejecutable con `maven-shade-plugin`

**De dónde vienes.** Traes el proyecto de la [Lección 9](09-concurrencia.md): un `revisor` concurrente que revisa veinte servicios en el tiempo del más lento, con 28 pruebas en verde. Funciona, pero todavía es un proyecto de programador y no un programa que otra persona pueda usar. Para correrlo hay que escribir `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` y estar parado en la carpeta correcta; el límite de concurrencia es una constante que se cambia recompilando; no hay forma de pedir el reporte en JSON; si todo falla, el programa termina igual que si todo estuviera bien; y nadie le ha pasado una revisión de estilo ni una búsqueda automática de defectos. Hoy se cierran esas cinco cosas. Trabajas en tu carpeta `~/curso-java/revisor/`.

**Qué no hace esta lección.** No usa una biblioteca para la línea de comandos (hay buenas, como *picocli*, pero el curso evita dependencias que no hacen falta y siete opciones se leen bien a mano). No pone cifrado ni autenticación al servidor: está pensado para que lo consulte otro proceso de la misma máquina, y eso se explica en 10.5. Y no convierte el JAR en un instalador: `jpackage` queda como «lo que viene».

**Sobre las corridas.** Las figuras 10.1 a 10.3 son archivos compactos y se corren con `java figNN_MM.java`. La figura 10.4 es el proyecto; necesita el laboratorio en otra terminal (`java Laboratorio.java`, puerto 8089; el 8088 queda vacío a propósito) y, cuando levanta su propio servidor, usa el puerto 8090. Las salidas se obtuvieron con Temurin 25.0.4, Maven 3.10.0 y los *plugins* fijados en el `pom.xml`. Tres advertencias de honestidad: **los tiempos son de mi máquina** (los tuyos serán otros; las proporciones, no); **los mensajes de Checkstyle y de algunas partes de la JVM salen en el idioma de tu sistema** (los míos salieron en español; con `-Duser.language=en` salen en inglés); y los comandos que sólo existen en Linux (como `ss`) los escribo como los vas a usar en tu Mint, pero **no pude ejecutarlos en la máquina donde medí**, así que su salida exacta la verás tú.

## Al terminar vas a poder

- Leer los argumentos de la línea de comandos a mano, validarlos, y devolver un error de uso claro sin mostrar una traza.
- Explicar qué es un código de salida, fijar uno por cada situación del `revisor` (`0`, `1`, `2`) y leerlo en la terminal con `$?`.
- Levantar un servidor HTTP con las clases del JDK, escuchando sólo en `127.0.0.1`, y apagarlo con orden cuando alguien lo interrumpe.
- Correr Checkstyle y SpotBugs en `mvn verify`, leer sus mensajes, y explicar por qué un análisis en silencio no prueba que no haya defectos.
- Repasar el `revisor` contra una lista de riesgos concretos (direcciones, redirecciones, serialización, interfaz de red) y decir qué cubre cada decisión.
- Empaquetar el programa en un solo JAR y ejecutarlo con `java -jar` desde cualquier carpeta.
- Comprobar, una por una, las seis condiciones con las que el temario dice que el curso terminó.

## El porqué antes del cómo

¿Cuándo está terminado un programa? No cuando corre en tu computadora: eso ya lo lograste desde la lección 5, y hasta la 9 cada lección se probó así. Un programa está terminado cuando **lo puede usar otra persona, otro programa o tú mismo dentro de seis meses, sin leer el código**. Eso exige cosas que nada tienen que ver con la lógica del `revisor`.

Exige una **interfaz**: una manera de decirle qué hacer sin editar nada. Exige una **respuesta honesta a quien lo llama**: un guion que corre el `revisor` cada cinco minutos necesita saber si todo salió bien sin tener que leer la tabla, y para eso existe el código de salida. Exige **calidad que se verifica sola**, porque «yo lo revisé» no escala y un estilo que sólo depende de la disciplina de las personas se erosiona. Exige que lo que **escucha en la red** lo haga con cuidado. Y exige una **entrega** simple: un archivo, un comando.

Esta lección toma esas cinco y las cierra. La **primera** sesión (10.1 a 10.3) es la interfaz: argumentos, códigos de salida y el servidor con su cierre ordenado. La **segunda** (10.4 a 10.6) es la entrega: las puertas de calidad, la revisión de seguridad y el JAR. Y el ejercicio 1 es la lista que el temario del curso llama «Cómo sabes que terminaste»: seis comprobaciones medibles, que haces tú, con el reloj y los comandos, y que dicen si el `revisor` está terminado de verdad.

## Los conceptos

### 10.1 Argumentos y código de salida

#### 10.1.1 Los argumentos de la línea de comandos

Cuando escribes `java -jar revisor.jar --config otro.json`, todo lo que viene después del nombre del programa llega a `main` como un arreglo de textos: el `String[] args` que ya conoces desde la lección 6. Java **no los interpreta**: no sabe qué es `--config`, ni que `otro.json` es su valor, ni que `--concurrencia` pide un número. Sólo los entrega en el orden en que los escribiste. La figura 10.1 los imprime, y también muestra cómo se sale de un programa con un código:

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

Sin argumentos, el programa los cuenta y termina con normalidad:

```text
$ java fig10_01.java
recibí 0 argumentos: []
termino bien
```

Si le pasas argumentos, llegan tal cual, como textos sueltos y en orden. Esta es una corrida con los de un `revisor` típico (el lanzador de un solo archivo, `java Archivo.java`, entrega a `main` todo lo que va después del nombre del archivo):

```text
$ java fig10_01.java --config servicios.json --concurrencia 3
recibí 4 argumentos: [--config, servicios.json, --concurrencia, 3]
termino bien
```

Fíjate en que el `3` es el texto `"3"`, no un número: convertirlo, y decidir qué hacer si alguien escribe `tres`, es trabajo tuyo. Eso es lo que hará la clase `Opciones` en 10.1.3.

#### 10.1.2 El código de salida: un contrato con quien te llama

Todo programa, cuando termina, le entrega al sistema operativo un número entre 0 y 255: su **código de salida** (*exit status*). Es la respuesta más corta posible a la pregunta «¿cómo te fue?», y es lo que leen los guiones, los planificadores de tareas y las herramientas de integración continua. La convención universal en sistemas tipo Unix es: **`0` quiere decir éxito, cualquier otro número quiere decir que algo salió mal**. En una terminal con `bash`, el código del último comando está en la variable `$?`, y el [manual de `bash`](https://www.gnu.org/software/bash/manual/html_node/Exit-Status.html) documenta esa convención, que es la que miran `if`, `&&` y `set -e`.

La última parte de `fig10_01.java` ya hace uso de esto: con el único argumento `--falla`, el programa escribe un error en la salida de error y termina con `System.exit(2)`:

```text
$ java fig10_01.java --falla
recibí 1 argumentos: [--falla]
error de uso: pediste fallar
$ echo $?
2
```

Dos detalles que parecen menores y no lo son. El primero: el error se escribió con `System.err`, no con `IO.println`. Un programa tiene **dos salidas** distintas: la **salida estándar** (`System.out`, donde va el resultado) y la **salida de error** (`System.err`, donde van los avisos para una persona). Si alguien redirige el resultado a un archivo —`revisor --formato json > reporte.json`— quiere el JSON en el archivo y los errores en la pantalla, no mezclados. El segundo: `System.exit` **termina toda la JVM y no regresa**: la línea que sigue a la llamada nunca se ejecuta, y tampoco el resto del programa, en ningún hilo. No es un corte brusco: inicia el apagado ordenado de la JVM, que corre los ganchos de apagado (10.3) y espera a que terminen antes de salir. Pero desde el punto de vista de tu código, todo lo que venía después ya no ocurre. Por eso se llama en un solo lugar, el `main`, y se le pasa un número que otro código calculó.

El `revisor` fija así sus códigos, y es un **contrato**: lo que otros programas pueden asumir y que, por eso, no se cambia sin avisar.

| Código | Cuándo | Qué se escribe |
|---|---|---|
| `0` | todos los servicios están `ARRIBA` (o se pidió `--ayuda` o `--version`) | el reporte, en la salida estándar |
| `1` | al menos un servicio está `LENTO` o `CAIDO` | el reporte, en la salida estándar |
| `2` | las opciones no se entienden, o la configuración no sirve | un mensaje en la salida de error |

La diferencia entre `1` y `2` es la que un guion necesita: `1` quiere decir «el programa funcionó y encontró un problema que debes atender», y `2` quiere decir «el programa no pudo hacer su trabajo; el problema es de quien lo corre». Un monitoreo puede alertar de forma distinta a cada uno.

Falta una pieza para poder **probar** ese contrato. Si todo el flujo del programa vive en `main` y `main` termina con `System.exit`, una prueba que llame a `main` se llevaría la JVM de las pruebas con ella. La solución es la de siempre, separar lo que decide de lo que actúa: `main` es una línea que sólo llama a `System.exit(ejecutar(args, System.out, System.err))`, y todo lo demás vive en `ejecutar`, un método que **recibe** las dos salidas como parámetros y **devuelve** el código. Una prueba le pasa dos flujos en memoria, lee lo que escribió, y comprueba el número que devolvió, sin terminar nada. Es la misma idea de la lección 9: comunica devolviendo valores.

#### 10.1.3 `Opciones`: leer los argumentos a mano

Esta es la clase que convierte el arreglo de textos en un valor ya validado. Antes de leerla, un resumen de las siete banderas que reconoce: `--config RUTA` (por omisión `servicios.json`), `--formato tabla|json` (por omisión `tabla`), `--concurrencia N` (por omisión 10, al menos 1), `--permitir h1,h2` (por omisión `127.0.0.1,localhost`), `--servir PUERTO`, `--version` y `--ayuda`. Cualquier otra cosa es un error de uso. El error se modela con su propia excepción verificada, igual que `ErrorDeConfiguracion` en la lección 5:

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

Cuatro ideas de la clase merecen un párrafo.

**Los argumentos se consumen de una cola.** En lugar de recorrer el arreglo con un índice y saltarse posiciones, se copian a una `ArrayDeque` y se va sacando el primero con `removeFirst()`. Cuando una bandera necesita su valor, llama a `valor(pendientes, bandera)`, que saca el siguiente o lanza `ErrorDeUso("falta el valor de --config")` si ya no hay. Así el código no tiene aritmética de índices que se pueda equivocar.

**El `switch` decide con una flecha.** Es la forma que ya conoces de la lección 2, pero usada como instrucción (cada caso asigna una variable) y no como expresión. Con flechas no hay *fall-through*: cada caso termina en sí mismo. Y el `default` lanza el error, de modo que una bandera mal escrita no pasa en silencio.

**Un número fuera de rango y un texto que no es número dicen lo mismo.** `entero` intenta convertir con `Integer.parseInt`; si falla, o si el valor queda fuera de rango, el mensaje es uno solo («debe ser un entero mayor o igual que 1, no tres»). A la persona que escribió mal la bandera no le sirve distinguir las dos causas.

**El *record* se protege.** Su constructor compacto hace `permitidos = Set.copyOf(permitidos)`: la misma copia defensiva de la lección 4, y por la misma razón, que la colección que guarda el *record* no sea una que alguien más pueda cambiar después. Y una anotación sobre `OptionalInt puerto`: la lección 4 dijo que un `Optional` se usa como valor de retorno y no como campo. Aquí hay una excepción consciente: `Opciones` es un valor de sólo lectura que **describe lo que se pidió**, y «no se pidió servidor» no tiene un número que lo represente (para el servidor, el 0 sí significa algo: «elige un puerto libre»). El `OptionalInt` dice exactamente eso.

Con las opciones ya resueltas, `ejecutar` las usa; lo ves completo en 10.3.3. Falta antes hablar del otro modo en que el `revisor` entrega su reporte.

### 10.2 El servidor HTTP del JDK

#### 10.2.1 Para qué un servidor

Hasta ahora, para ver el reporte hay que correr el programa, que revisa y termina. Sirve para una persona en una terminal; no sirve para otra herramienta que quiera consultar el estado cada minuto, o para un tablero que muestre el resultado. Para eso, el `revisor` puede quedarse **escuchando**: con `--servir 8090` abre un servidor HTTP y, cada vez que alguien le pide `/reporte`, hace una revisión completa y responde el JSON de la lección 8.

Lo bueno es que no hace falta ninguna biblioteca para lograrlo. El JDK trae un servidor HTTP sencillo, el paquete [`com.sun.net.httpserver`](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html), parte del módulo `jdk.httpserver`. Es una API documentada y soportada (el nombre `com.sun` es histórico), y es **la misma clase que usa `Laboratorio.java`**, el programa que llevas corriendo desde la lección 8: si lo abres, encontrarás `HttpServer.create`, una lista de `createContext` y un ejecutor de hilos virtuales. Hoy escribes tú lo que antes sólo usabas. (Para servir archivos estáticos, el JDK trae además una herramienta de línea de comandos, `jwebserver`, descrita en [JEP 408](https://openjdk.org/jeps/408); no la necesitamos.)

#### 10.2.2 Un servidor mínimo, de principio a fin

La figura 10.2 es el servidor más pequeño que vale la pena leer. Levanta un servidor, le registra una ruta que contesta un JSON de una línea, se hace una petición a sí mismo con el `HttpClient` de la lección 8, imprime lo que recibió y se apaga. Como es un programa que arranca y termina solo, se puede verificar. (Una nota de lectura: los cuatro `import` del principio son necesarios porque un archivo compacto sólo importa por sí mismo `java.base`, y ni el servidor ni el cliente HTTP viven ahí; si los quitas, el compilador te dice `cannot find symbol` para `HttpClient` y los demás.)

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

Los cinco pasos, uno por uno. **`HttpServer.create(direccion, 0)`** crea el servidor sin arrancarlo; el segundo argumento es el tamaño de la cola de conexiones pendientes, y 0 significa «el que decida el sistema». **`createContext("/hola", manejador)`** registra qué hacer con las peticiones cuyo camino empieza con `/hola`. **`setExecutor(...)`** decide quién atiende las peticiones; **`start()`** lo pone a escuchar; **`stop(0)`** lo detiene (el número son los segundos que espera a las peticiones que están en vuelo, y vuelve en 10.3).

Tres decisiones de esta figura que se repiten en el servidor del `revisor`:

**La dirección es `127.0.0.1`, no «todas».** `new InetSocketAddress("127.0.0.1", puerto)` hace que el servidor escuche **sólo en la interfaz local**: únicamente los programas de esta misma máquina pueden hablarle. Si en vez de eso escribes `new InetSocketAddress(puerto)`, sin dirección, Java usa la dirección comodín y escucha en **todas las interfaces**, incluida la red: cualquier equipo que alcance tu máquina podría pedir el reporte. La figura lo comprueba al imprimir `isLoopbackAddress()`. Para una herramienta interna que consulta otro proceso local, la primera es la que va; abrir la segunda es una decisión que se toma a propósito, con autenticación y cifrado, no por omisión.

**El puerto 0 significa «elige uno libre».** El sistema asigna cualquiera que no esté ocupado, y `servidor.getAddress().getPort()` dice cuál fue. Es lo que usan las pruebas de la lección 8 y las de hoy, para no chocar con lo que haya corriendo.

**Un hilo virtual por petición.** `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` es la lección 9 aplicada. Sin ejecutor, el servidor atiende **una petición a la vez** en un solo hilo; lo medí: seis peticiones de 100 ms simultáneas llegaron al manejador de una en una (máximo simultáneo: 1). Con un hilo virtual por petición, una petición lenta no estorba a las demás.

#### 10.2.3 `Servidor`: la ruta `/reporte`

La clase que el `revisor` usa recibe el puerto y **una función que produce un `Reporte`** (un `Callable<Reporte>`). Que reciba la función y no el trabajo ya hecho tiene un motivo: la revisión debe ocurrir **cada vez que alguien pide el reporte**, no una sola vez al arrancar. Y tiene otro, que verás en las pruebas: se le puede dar un reporte fijo y comprobar qué contesta, sin tocar ninguna red.

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

Lo que contesta cada situación, para que no tengas que adivinarlo del código: un `GET /reporte` responde `200` con el JSON y `Content-Type: application/json`; cualquier otro camino, `404`; cualquier otro método (un `POST`, por ejemplo), `405`; y si la revisión falla por un error del programa, `500` con el texto genérico `error interno`. Ese último punto es deliberado: el mensaje de una excepción puede contener rutas, nombres de clases o datos que no deben salir por la red, así que a quien pregunta se le dice que algo falló y nada más. (A quien opera el programa se le diría por un registro, que este curso no construye.) El `try (intercambio)` cierra la conversación con el cliente pase lo que pase, y el cuerpo se escribe como bytes en UTF-8, porque el JSON lleva acentos («sin conexión»).

Una advertencia de diseño que conviene dejar dicha. Cada `GET /reporte` hace **una revisión completa**: consulta todos los servicios y tarda lo que tarde el más lento (unos 2.5 segundos con los cuatro canónicos). No es un caché. Si diez personas piden el reporte a la vez, el `revisor` hace diez revisiones a la vez. Es una consecuencia que la sección 10.5 retoma, porque tiene un lado de seguridad.

### 10.3 El cierre ordenado y el `revisor` completo

#### 10.3.1 El problema: un servidor que no se sabe apagar

Un servidor es un programa que no termina por sí mismo; termina cuando alguien lo interrumpe: tú con `Ctrl`+`C`, el sistema al apagarse, o una herramienta que le manda la señal de terminación. Si en ese instante hay una petición a medias, interrumpirlo de golpe la corta. La solución es el **cierre ordenado** (*graceful shutdown*): dejar de aceptar peticiones nuevas, darles unos segundos a las que ya van en camino, y recién entonces salir.

Java ofrece un mecanismo para ello: el **gancho de apagado** (*shutdown hook*). Es un hilo que registras con `Runtime.getRuntime().addShutdownHook(hilo)` y que la JVM arranca cuando empieza a apagarse, por la razón que sea: porque el último hilo normal terminó, porque alguien llamó a `System.exit`, o porque el sistema le mandó una señal de terminación como la de `Ctrl`+`C` (la [documentación de `Runtime`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html) enumera los casos). No corre si el proceso muere de golpe, como con `kill -9` o un corte de luz; ahí no hay cierre posible. La figura 10.3 lo muestra en su forma más pequeña:

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

El programa termina por su propio pie (`main termina`), y **después** se ejecuta el gancho. Con `Ctrl`+`C` pasaría lo mismo. Dos reglas para escribir un buen gancho: que sea **corto** y no se quede bloqueado (la JVM espera a que todos los ganchos terminen, así que un gancho que nunca termina deja al programa sin poder salir; si hay un límite, viene de fuera, por ejemplo del sistema operativo que, al apagarse, termina el proceso de golpe), y que **nunca llame a `System.exit`** (el apagado ya está en marcha, y llamarlo desde dentro lo deja esperándose a sí mismo para siempre).

#### 10.3.2 Cómo lo usa el `revisor`

En modo servidor, el `revisor` hace tres cosas, en este orden: arranca el servidor; registra un gancho que llama a `servidor.stop(1)` (deja de aceptar, espera hasta 1 segundo a que terminen las peticiones en curso, y cierra) y escribe `servidor detenido`; y **espera**. Esa espera es la parte engañosa. Si `ejecutar` devolviera un número y `main` llamara a `System.exit`, el servidor moriría un instante después de nacer. Por eso el hilo principal se queda esperando con `new CountDownLatch(1).await()`: un cerrojo que nadie va a abrir, de modo que `await()` **no regresa nunca** y el programa vive hasta que la JVM se apague por fuera, con lo cual el gancho hace su trabajo.

Lo vas a probar a mano en 10.3.5, en cuanto tengas el `Revisor` completo de la sección siguiente y sus pruebas en verde.

#### 10.3.3 El `revisor` completo

Ahora sí se puede ver `Revisor.java` entero. Léelo como una receta de seis pasos, que son los de `ejecutar`: **1** leer las opciones (si fallan: mensaje de uso y código 2); **2** si pidieron `--ayuda` o `--version`, contestar y salir con 0; **3** leer la configuración, validada con la lista permitida que se eligió (si falla: mensaje y código 2); **4** si pidieron `--servir`, entregarle el trabajo a `servir` y no regresar; **5** si no, revisar todo con el límite de concurrencia pedido; **6** escribir la tabla o el JSON y devolver 0 si todos los servicios están arriba, y 1 si no.

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

Algunas cosas que no están a la vista. La versión que imprime `--version` **no está escrita en el código**: sale del archivo `revisor.properties`, que Maven llena con la versión del `pom.xml`, de modo que el número no se repite en dos lugares que se desincronicen (lo armas justo abajo). Si el puerto está ocupado, `HttpServer.create` lanza una `IOException` y `ejecutar` la atrapa y dice `no se pudo abrir el puerto: Address already in use`, con código 2: es un problema de quien lo corre. Y el `catch (InterruptedException)` restaura la bandera de interrupción, como enseñó la lección 5, antes de salir con 1.

Para que eso funcione hacen falta tres cambios fuera del código, y los haces ahora, antes de las pruebas, porque una de ellas comprueba justo la versión. El primero es el archivo de propiedades, en una carpeta nueva, `src/main/resources/`, la de los archivos que acompañan al programa sin ser código (Maven los copia a `target/classes`). Es de una sola línea:

```properties fig10_04/src/main/resources/revisor.properties
version=${project.version}
```

Los otros dos van en tu `pom.xml`. La versión pasa de `0.9.0` a **`1.0.0`**, la de la entrega. Y dentro de `<build>`, antes de `<plugins>`, se agrega el bloque que le pide a Maven **filtrar** esa carpeta, es decir, reemplazar `${project.version}` por la versión del `pom.xml` al copiar el archivo:

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

Es un fragmento: lo demás del `pom.xml` queda como lo traes de la lección 9, con `maven-dependency-plugin` incluido, que todavía usas para correr el programa. El `pom.xml` completo de la entrega aparece en 10.4.2, cuando lleguen las puertas de calidad y el JAR.

El archivo de configuración con los siete servicios (los cuatro canónicos y los tres extra del laboratorio) cuelga de la carpeta del proyecto, y lo usarás más abajo:

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

#### 10.3.4 Las pruebas del contrato

`RevisorTest` trae seis pruebas, cada una sobre una promesa de la sección: la versión sale del `pom.xml`, una concurrencia inválida es un error de uso con código 2, todos arriba da 0, un caído da 1, el JSON de un reporte conocido es **exactamente** el contrato de la lección 8, y el servidor contesta `200` con `application/json`.

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

Tres cosas para leerla con provecho. La clase `Corrida` es un *record* privado que agrupa lo que dejó una ejecución: el código y los dos textos. `correr(...)` llama a `Revisor.ejecutar` con dos flujos en memoria y los decodifica en UTF-8; no termina ninguna JVM. Y la prueba del JSON compara contra un bloque de texto escrito a mano **carácter por carácter** (incluidos los espacios alrededor de los dos puntos, que son los de Jackson): si alguien cambia el nombre de una llave, el orden de los campos o el formato, esta prueba se pone roja. Es lo que significa que el JSON sea un contrato, y no un detalle de implementación.

Como siempre, antes de fiarte de la prueba, ponla en rojo. En `Revisor.java`, cambia `return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;` por `return CODIGO_TODO_ARRIBA;` (el programa «siempre dice que todo va bien») y corre las pruebas:

```text
[ERROR] mx.habil.revisor.RevisorTest.conUnServicioCaidoElCodigoEsUno -- Time elapsed: 0.154 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
[ERROR] Tests run: 34, Failures: 1, Errors: 0, Skipped: 0
```

La única que se pone roja es la del código para un servicio caído, con los números que debía: esperaba 1 y recibió 0. Restaura la línea y vuelve a verde: son **34** pruebas, 28 de la lección 9 y 6 de hoy.

#### 10.3.5 Probarlo a mano

Con las 34 pruebas en verde, `mvn -B verify` ya dejó también las bibliotecas en `target/lib/` (la copia de la lección 8), así que el `revisor` corre con el *classpath* de siempre. Con el laboratorio encendido, en una terminal:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor --servir 8090
sirviendo http://127.0.0.1:8090/reporte (Ctrl+C para detener)
```

y en otra, `curl -i http://127.0.0.1:8090/reporte` devuelve (recortado):

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

Una petición a otro camino devuelve `404`, y un `POST` al mismo `/reporte`, `405`. Para comprobar que escucha **sólo** en la interfaz local, en tu Mint puedes pedirle a `ss` los puertos que están a la escucha, filtrando el 8090 (`ss -ltn 'sport = :8090'`): la dirección local debe ser `127.0.0.1:8090`, no `0.0.0.0:8090` ni `*:8090`. En la máquina donde medí, que no es Linux, el equivalente `lsof -nP -iTCP:8090 -sTCP:LISTEN` dijo `TCP 127.0.0.1:8090 (LISTEN)`.

Y el cierre. Al mandarle la señal que manda `Ctrl`+`C` (`SIGINT`), la terminal mostró:

```text
servidor detenido
```

y el código de salida fue **130**. Con `kill -TERM`, la señal que mandan los administradores de servicios, también apareció `servidor detenido` y el código fue **143**. No son errores del `revisor`: es la convención de la terminal, que cuando un proceso muere por una señal informa `128 + el número de la señal` (2 para `SIGINT`, 15 para `SIGTERM`), como lo documenta el mismo manual de `bash`. Si en un guion tu `revisor` en modo servidor termina con 143, significa que lo detuvieron, no que falló. Después de detenerlo, el puerto 8090 queda libre: lo comprobé con el mismo comando, que no devuelve ninguna línea. En la segunda sesión este mismo comando se acorta a `java -jar target/revisor.jar --servir 8090` (10.6).

*(Aquí termina la primera sesión.)*

### 10.4 Calidad: Checkstyle y SpotBugs dentro de `verify`

#### 10.4.1 Dos puertas distintas

Que un programa funcione no quiere decir que esté bien hecho. Hay dos clases de problema que las pruebas no ven, y que dos herramientas automáticas sí.

**Checkstyle** revisa la **forma** del código fuente: que la sangría sea de dos espacios, que los renglones no pasen de 100 columnas, que los nombres sigan la convención, que toda clase y método públicos tengan su comentario Javadoc. Lo hace contra una configuración; la que usa el curso es `google_checks.xml`, que viene dentro de Checkstyle y codifica la [guía de estilo de Google para Java](https://google.github.io/styleguide/javaguide.html). Por eso desde la lección 1 todo el código del curso lleva dos espacios de sangría: para que hoy la puerta se abra sin reescribir nada. Un estilo común no es estética: es lo que permite leer el código de otra persona sin tropezar, y que una diferencia en una revisión sea una diferencia de verdad y no de espacios.

**SpotBugs** revisa el **comportamiento probable**: no lee el texto, sino el *bytecode* ya compilado, y busca patrones que suelen ser defectos: un recurso que no se cierra, un `null` que se desreferencia, una colección mutable expuesta, una cadena de formato incorrecta. Cada patrón tiene un código en mayúsculas (como `OS_OPEN_STREAM`) que se puede buscar en su [catálogo](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html). No sustituye a las pruebas: cubre otra cosa. Una prueba dice «este caso sale bien»; un análisis estático dice «este tipo de error no está en ninguna parte del código», incluso en los caminos que ninguna prueba recorre.

#### 10.4.2 El `pom.xml` de la entrega

Las dos herramientas se conectan al ciclo de Maven con la fase `verify` (la última que llevas usando desde la lección 6), de modo que `mvn -B verify` ya no significa «compila y pasa pruebas» sino «pasa todas las puertas». Este es el `pom.xml` completo de la versión 1.0.0:

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

Cinco cosas del `pom.xml` importan, en este orden:

1. **La versión es `1.0.0`**, y es el único lugar donde se escribe. El bloque `<resources>` con `<filtering>true</filtering>`, que agregaste en 10.3.3, hace que Maven reemplace `${project.version}` dentro de los archivos de `src/main/resources`; así `revisor.properties` termina diciendo `version=1.0.0` sin que nadie lo escriba dos veces.
2. **`maven-dependency-plugin` desapareció.** Ya no se copian las dependencias a `target/lib/`; el empaquetado de 10.6 las incluye dentro del JAR.
3. **`maven-checkstyle-plugin` 3.6.0 lleva una dependencia propia: Checkstyle 14.3.0.** No es adorno. El *plugin* trae por omisión un Checkstyle más viejo que no entiende la sintaxis de Java 25, y falla al leer cualquier archivo con los patrones de las lecciones 3 y 4. Lo comprobé quitando esa dependencia:

   ```text
   [ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: Failed during checkstyle configuration: Exception was thrown while processing .../Clasificador.java: IllegalStateException occurred while parsing file .../Clasificador.java. 15:41: mismatched input 'when' expecting '->': InputMismatchException
   ```

   Es un mensaje que no menciona ninguna versión, y apunta a una línea de tu código que está bien: el `when` de un `switch` con patrones. La causa es siempre la misma, un analizador que es más viejo que el lenguaje. Si ves un error de «análisis» en un archivo que compila, sospecha de la versión de la herramienta antes que de tu código.
4. **`<violationSeverity>warning</violationSeverity>`.** Los problemas que Checkstyle encuentra con `google_checks.xml` son advertencias (*warning*). Por omisión el *plugin* sólo rompe la construcción por errores, y entonces la puerta estaría abierta aunque hubiera cien infracciones. Con `warning`, cualquier infracción rompe `verify`.
5. **`spotbugs-maven-plugin` 4.10.4.1** con el objetivo `check` en `verify`: corre el análisis y rompe la construcción si encuentra algo.

Cuando todo está en orden, `mvn -B verify` termina así (recorto lo que no analizamos):

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

Lee los **números** y no el `BUILD SUCCESS`: 34 pruebas, 0 infracciones, 0 defectos. Una puerta que no corrió también termina en éxito.

#### 10.4.3 Lo que encontraron en mi propio código

Cuento esto porque es lo más útil de la sección. La primera vez que corrí `verify` sobre el `revisor` de esta lección, **Checkstyle marcó cinco infracciones de formato** (renglones de más de 100 columnas y las comillas de cierre de un bloque de texto, que la guía de Google pide en su propio renglón), que arreglé en minutos. Y cuando quedó limpio, **SpotBugs encontró dos cosas** en código que yo creía bien. Estos fueron sus mensajes, tal cual:

```text
[ERROR] Medium: mx.habil.revisor.Revisor.version() may fail to close stream [mx.habil.revisor.Revisor] At Revisor.java:[line 118] OS_OPEN_STREAM
[ERROR] Medium: Format string should use %n rather than \n in mx.habil.revisor.Revisor.<static initializer for Revisor>() [mx.habil.revisor.Revisor] At Revisor.java:[line 35] VA_FORMAT_STRING_USES_NEWLINE
```

Se lee así: un nivel de gravedad (`Medium`), una descripción, la clase y el método, el renglón, y al final el código del patrón. Los dos eran verdad, de maneras distintas. El primero venía de que leía las propiedades con `new InputStreamReader(flujo, UTF_8)`: aunque el `flujo` estaba en un `try-with-resources`, el lector que lo envuelve era un segundo objeto que SpotBugs no veía cerrado. La salida simple fue leer las propiedades con `propiedades.load(flujo)`, sin envoltura (un archivo de propiedades se lee como ISO-8859-1, y un número de versión es ASCII). El segundo era más sutil: yo armaba el texto de ayuda con un bloque de texto y `.formatted(...)`, y el bloque trae saltos de línea (`\n`) dentro de una cadena de formato, donde Java espera `%n`, el salto de línea de la plataforma. No era un error en Linux, pero SpotBugs tiene razón en que es una fragilidad. Lo resolví sacando el formato: el texto lleva un marcador `{limite}` y un método lo reemplaza. Las dos correcciones están en el `Revisor.java` que ves en 10.3.3.

La lección no es que SpotBugs sea más listo que tú. Es que **la herramienta encuentra lo que no estabas buscando**, y que arreglar sus hallazgos casi siempre deja el código más simple, no más largo.

#### 10.4.4 Leer lo que dicen y no apagarlos

Cuando Checkstyle rompe la construcción, el mensaje trae el archivo, la línea y la columna, la categoría y el nombre de la regla. Si le quitas el comentario Javadoc a `main`, esto es lo que dice con la JVM en español:

```text
[WARNING] src/main/java/mx/habil/revisor/Revisor.java:[38,3] (javadoc) MissingJavadocMethod: Falta el comentario Javadoc para 'main'.
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: You have 1 Checkstyle violation.
```

(El texto sale en el idioma de la JVM: con `MAVEN_OPTS="-Duser.language=en"` el mismo mensaje dice `Missing a Javadoc comment for 'main'.`. El nombre de la regla, `MissingJavadocMethod`, es el mismo en cualquier idioma, y es lo que debes buscar en la [lista de reglas](https://checkstyle.org/checks.html).) Por eso el `main` del `revisor` tiene un Javadoc, y por eso lo tiene toda clase y método **públicos**; las demás clases son de paquete y no lo exigen, que es una de las razones para dejar todo cerrado (lección 6).

Hay una segunda tentación que conviene nombrar y rechazar: **apagar la regla**. Todas estas herramientas ofrecen formas de callarse (`@SuppressWarnings`, archivos de exclusión, bajar la severidad). Úsalas sólo cuando el hallazgo es falso y puedes decir por qué; nunca para que la construcción pase. Un `verify` verde porque lo callaste no vale más que uno que no corrió.

Y una advertencia sobre el silencio de SpotBugs, medida. En la lección 4 pusiste `List.copyOf` en el constructor de `Reporte` para que el *record* tuviera su propia copia de la lista, y la misma lección midió que SpotBugs calla sobre ella en una clase de paquete. Aquí se ve por qué: **depende de la visibilidad.** Si quito esa línea de `Reporte`, que es una clase de paquete (sin `public`), SpotBugs **no dice nada**: `BugInstance size is 0`. Si hago `Reporte` público y vuelvo a quitar la línea, entonces sí:

```text
[ERROR] Medium: mx.habil.revisor.Reporte.resultados() may expose internal representation by returning Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 6] EI_EXPOSE_REP
[ERROR] Medium: new mx.habil.revisor.Reporte(List, Resumen) may expose internal representation by storing an externally mutable object into Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 8] EI_EXPOSE_REP2
```

SpotBugs se preocupa por lo que **se ve desde fuera del paquete**, y una clase de paquete no se ve. Lo que esto enseña es doble. La copia defensiva sigue siendo correcta aunque la herramienta no la exija (la exigiría el día que `Reporte` se hiciera público, y entonces ya sería tarde para pensar en ello). Y el silencio de una herramienta es información sobre lo que **revisó**, no una garantía sobre todo lo demás.

*(Para saber más sobre cómo se integran estas puertas en un flujo de entrega, hay dos artículos del blog: [pruebas que atrapan errores](https://www.habil.mx/es/blog/pruebas-que-atrapan-errores-cobertura-quality-gate/) y [CI/CD y DevSecOps](https://www.habil.mx/es/blog/cicd-devsecops-infraestructura-propia/).)*

### 10.5 Repaso de seguridad

La seguridad del `revisor` no se agregó al final: se fue construyendo donde aparecía el riesgo. Esta sección junta lo que ya existe y lo revisa contra la superficie de ataque completa, que son cuatro cosas: **lo que entra** (la configuración y las opciones), **a dónde sale** (las peticiones que hace), **lo que escucha** (el servidor) y **lo que muestra** (el reporte).

**1. Lo que entra: la dirección de cada servicio la escribe otra persona.** Es el riesgo de la lección 8, el de las peticiones falsificadas del lado del servidor (SSRF, por *server-side request forgery*): si el `revisor` consultara cualquier dirección que aparezca en `servicios.json`, quien edite ese archivo lo convertiría en un intermediario para llegar a cosas que él no alcanza pero el `revisor` sí (como el servicio de metadatos de una nube, que vive en `169.254.169.254`). Las defensas son tres, todas de una lista de lo permitido y ninguna de lo prohibido: sólo esquemas `http` y `https`; sólo servidores de una **lista permitida**; y ninguna redirección seguida. La [guía de OWASP sobre SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) coincide: validar contra lo permitido, no contra lo conocido como malo.

En la lección 8 la lista permitida era una constante. Hoy es **configurable**, con `--permitir h1,h2`, y eso cambia la pregunta: ¿quién controla esa opción? Es la de quien **corre** el programa, no la de quien edita el archivo de servicios. Esa separación es la razón por la que la lista no va dentro de `servicios.json`: un archivo no puede darse permiso a sí mismo. Si algún día se te ocurre leer la lista permitida del mismo archivo que valida, o de la red, deshiciste la defensa. Se puede comprobar con los tres archivos que acompañan a la lección: uno con `ftp://`, uno con `file:///etc/passwd` y uno con `http://169.254.169.254/latest/meta-data/`. Los tres se rechazan antes de hacer una sola petición, con el código 2 (ver el ejercicio 1).

**2. A dónde sale: las redirecciones.** `HttpClient` tiene `followRedirects(HttpClient.Redirect.NEVER)` escrito en `Revision.nuevoCliente()`. Es el valor por omisión, y se escribe igual a propósito: un servidor permitido podría contestar «mudé a otra dirección» apuntando a una que no lo está, y seguir esa redirección sin revisarla evadiría la lista. El `revisor` trata un `3xx` como una respuesta más y la clasifica; no la sigue.

**3. Lo que escucha: sólo la interfaz local.** El servidor de 10.2 escucha en `127.0.0.1`. Hay un riesgo propio del diseño que ya se anunció: cada `GET /reporte` dispara una revisión completa, con una petición por servicio. Un proceso local malintencionado, o simplemente descuidado, que pida el reporte en un ciclo, hace que el `revisor` consulte a todos esos servicios sin descanso. La mitigación inmediata es la interfaz local, que reduce quién puede hacerlo a los programas de la misma máquina; las siguientes (un caché de unos segundos, un límite de peticiones por segundo) no están hechas, y esta lección no las simula: queda dicho que faltan, y es el primer lugar donde tocaría seguir trabajando.

**4. Lo que muestra: el reporte es público para quien lo pida.** El JSON incluye el campo `url` de cada servicio tal como está en el archivo. Si algún día una dirección lleva credenciales (`http://usuario:clave@...`) o un `token` en el texto de la consulta, `/reporte` las va a mostrar. Hoy el curso no las usa, y por eso no hay un filtro; pero es el tipo de decisión que se toma **antes** de que el dato exista. Un error interno, en cambio, ya está resuelto: devuelve `error interno` y nada de la excepción.

**5. La serialización nativa, que no se usa.** Java tiene un mecanismo antiguo para guardar y recuperar objetos directamente, `ObjectOutputStream` y `ObjectInputStream`. Leer objetos con `ObjectInputStream` de una fuente en la que no confías es una de las vulnerabilidades más graves y mejor documentadas de la plataforma, porque la lectura puede ejecutar código elegido por quien escribió los bytes ([OWASP lo explica con ejemplos](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html), y el JDK ofrece [filtros](https://docs.oracle.com/en/java/javase/25/core/serialization-filtering1.html) desde [JEP 290](https://openjdk.org/jeps/290) para mitigarlo). El `revisor` jamás la usa, y su seguridad aquí no viene de que el formato sea JSON (también hay bibliotecas de JSON que, mal configuradas, construyen el objeto que el texto les pide), sino de tres decisiones concretas: **no usa la serialización nativa** en ningún lugar; **no habilita la deserialización polimórfica** de Jackson, la opción que deja que el propio texto diga de qué clase crear cada objeto; y **valida la configuración campo por campo contra la forma que espera** (la lección 8 la lee como árbol de `JsonNode` y comprueba cada valor, sin mapearla directo a objetos). La regla del curso es simple: **datos de fuera entran como texto estructurado y se validan a mano, nunca como objetos.**

Para seguir con el tema, el artículo del blog sobre [seguridad de APIs](https://www.habil.mx/es/blog/seguridad-apis-cto-identidad-gateway-tokens/) trata la identidad y los *tokens*, que es lo que haría falta si el servidor saliera de la máquina local.

### 10.6 El JAR ejecutable

#### 10.6.1 El problema del *classpath* largo

Desde la lección 8 corres el `revisor` con `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor`. Es una línea larga que obliga a saber tres cosas: dónde están tus clases, dónde están las bibliotecas, y cuál clase tiene el `main`. Y falla en silencio de maneras interesantes. Si te falta la parte de las bibliotecas, esto es lo que ves:

```text
$ java -cp target/classes mx.habil.revisor.Revisor --config servicios-arriba.json
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
	at mx.habil.revisor.Opciones.leer(Opciones.java:32)
	at mx.habil.revisor.Revisor.ejecutar(Revisor.java:50)
	at mx.habil.revisor.Revisor.main(Revisor.java:44)
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

Se lee de abajo hacia arriba: una clase de Jackson no está en el *classpath*, y la JVM lo descubre justo cuando la necesita. La solución es lo que se le da a quien recibe el programa: **un solo archivo** con todo adentro.

#### 10.6.2 Qué es un JAR, y qué hace `shade`

Un **JAR** (*Java archive*) es un archivo ZIP con clases dentro y un archivo especial, `META-INF/MANIFEST.MF`, con metadatos. Si el manifiesto trae la línea `Main-Class: mx.habil.revisor.Revisor`, `java -jar archivo.jar` sabe cuál clase arrancar. Es la [especificación del JAR](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html).

`maven-jar-plugin`, que ya corre desde la lección 6, genera el JAR del *proyecto*: sólo tus clases, sin bibliotecas y sin `Main-Class`. Por eso, si lo ejecutas, pasa esto:

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

(En inglés, con la JVM en ese idioma: `no main manifest attribute, in target/revisor-1.0.0.jar`.) Es el error de «JAR sin entrada»: tu JAR existe y está bien, pero no dice por dónde empezar. Y tiene 32 entradas: tus clases y poco más.

Lo que hace falta es [`maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/), que corre después y produce **otro** JAR que contiene tus clases *y* las de todas las dependencias, con un manifiesto que sí trae `Main-Class`. En el `pom.xml` de 10.4 son tres líneas de configuración: `finalName` hace que el archivo se llame `revisor.jar` (sin la versión, para que el comando de uso no cambie con cada versión); el `ManifestResourceTransformer` con `mainClass` escribe el `Main-Class`; y `createDependencyReducedPom` en `false` evita que el *plugin* reescriba el `pom.xml` del proyecto, un efecto secundario que no necesitamos.

Con él, `mvn -B verify` deja en `target/` el JAR completo. Estas son las cifras que medí:

```text
$ unzip -p target/revisor.jar META-INF/MANIFEST.MF | grep Main
Main-Class: mx.habil.revisor.Revisor
$ unzip -l target/revisor.jar | tail -1
  6359578                     1337 files
$ unzip -p target/revisor.jar revisor.properties
version=1.0.0
```

1,337 entradas y unos 2.6 MB, de las cuales 1,176 están bajo `tools/jackson`. Y la versión sale del `pom.xml`, como se armó en 10.3.3.

Durante la construcción, `shade` imprime advertencias que a primera vista alarman: `Discovered module-info.class. Shading will break its strong encapsulation.` y `define 2 overlapping resources: META-INF/LICENSE, META-INF/NOTICE`. Las dos son ciertas y aquí inofensivas: Jackson publica una descripción de módulo que, al juntarse todo en un JAR, deja de usarse (nosotros no usamos el sistema de módulos, y no se enseña en este curso), y varios JAR traen su propio archivo de licencia con el mismo nombre, del que `shade` conserva uno. Cuando no entiendas una advertencia, no la ignores por costumbre: léela, y decide con el detalle. Esta es de las que se pueden dejar.

#### 10.6.3 La prueba que cuenta: otra carpeta

Un JAR sólo es independiente si lo puedes correr **desde cualquier carpeta**. Prueba esto, con la ruta completa del JAR y del archivo de configuración:

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

Funciona, y sale con 1 porque hay un servicio lento y uno caído. Fíjate también en lo que pasa **sin** `--config`: `servicios.json` es una ruta relativa, y desde `/tmp` no existe, así que el programa lo dice con su error y su código:

```text
$ java -jar ~/curso-java/revisor/target/revisor.jar
error de configuración: no existe el archivo servicios.json
$ echo $?
2
```

Es el error que la lección 5 anunció, el de la ruta relativa que depende de dónde estés parado, ahora con su código de salida. Es un código 2 correcto: no es un servicio que falló, es un uso incorrecto.

### 10.7 Lo que viene: `jpackage`

Un JAR resuelve la entrega del *programa*, no la del entorno: quien lo reciba sigue necesitando un Java instalado. Para quien no lo tiene, el JDK trae dos herramientas que quedan como curiosidad y no se enseñan: [`jlink`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jlink.html) arma una versión recortada de la JVM con sólo los módulos que tu programa necesita, y [`jpackage`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html) empaqueta tu JAR junto con esa JVM en un instalador nativo (un `.deb` para tu Mint, por ejemplo) que no pide nada previo. Son el siguiente paso natural cuando entregas el programa a personas que no son programadoras. Otra salida, distinta, es compilar el programa a código nativo, que tiene costos y ventajas propias; el artículo [cuándo conviene compilar Java a nativo](https://www.habil.mx/es/blog/java-nativo-cuando-si-cuando-no/) las discute.

## El error que vas a ver

El error de la lección es el del JAR sin entrada, que acabas de ver en 10.6.2:

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

Significa que el JAR no declara cuál clase arrancar: ejecutaste el JAR delgado del proyecto en vez del completo (`revisor.jar`). Se arregla ejecutando el otro, o agregando la configuración de `shade` al `pom.xml`. Los otros mensajes que conviene reconocer de una mirada:

- `error de uso: --concurrencia debe ser un entero mayor o igual que 1, no 0` y código 2: una opción mal escrita; es la salida esperada del `revisor`, no un fallo.
- `error de configuración: no existe el archivo servicios.json` y código 2: ejecutaste desde otra carpeta sin `--config` (10.6.3).
- `no se pudo abrir el puerto: Address already in use` y código 2: ya hay algo escuchando en ese puerto, quizá otro `revisor` que no apagaste. Lo comprobé arrancando dos con `--servir 8090`.
- `NoClassDefFoundError: tools/jackson/core/JacksonException`: falta Jackson en el *classpath*; usa el JAR completo (10.6.1).
- `MissingJavadocMethod` de Checkstyle y `EI_EXPOSE_REP` de SpotBugs: las puertas de calidad de 10.4; el mensaje trae la regla y el renglón.

## Lo que se hace mal

**Llamar a `System.exit` en medio del programa.** Mata la JVM sin que nadie pueda limpiar nada y vuelve imposible de probar lo que lo rodea. Se llama una vez, en `main`, con un número calculado por otro código (10.1.2).

**Mezclar el resultado con los avisos.** Si el JSON y los errores van a la misma salida, quien redirija el JSON a un archivo se encontrará un mensaje de error en medio. El resultado a la salida estándar, los avisos a la de error.

**Un código de salida que no distingue nada.** Terminar siempre con 0 (o siempre con 1) le quita al programa su utilidad en un guion. Tres códigos con significados fijos y documentados valen más que un arreglo ingenioso.

**Escuchar en todas las interfaces por descuido.** `new InetSocketAddress(puerto)` sin dirección expone el servicio a toda la red. La dirección se escribe, siempre (10.2.2).

**Un gancho de apagado largo o que llama a `System.exit`.** Se queda esperándose a sí mismo. Un gancho hace una cosa corta y termina (10.3.1).

**Devolver el mensaje de una excepción por la red.** Es entregar rutas, clases y a veces datos. Se devuelve un texto genérico y el detalle va a un registro (10.2.3).

**Callar una regla para que `verify` pase.** El análisis estático vale mientras se le hace caso. Callarlo sólo cuando el hallazgo es falso y puedes explicar por qué (10.4.4).

**No fijar la versión de un *plugin*.** El que no la lleva queda a merced de lo que traiga Maven ese día; dos máquinas pueden construir cosas distintas. Todos los *plugins* del `pom.xml` llevan versión.

**Empaquetar sin probar desde otra carpeta.** Que el JAR corra en la carpeta del proyecto no prueba que sea independiente (10.6.3).

## Ejercicios

### Ejercicio 1 — Las seis comprobaciones del curso

La lista de «Cómo sabes que terminaste» del temario son seis condiciones. Compruébalas **una por una**, con el laboratorio encendido, anotando el comando y lo que viste:

1. `mvn -B verify` termina en verde y el informe dice cuántas pruebas corrieron.
2. Revisa 20 servicios a la vez en un tiempo cercano al del más lento, no a la suma (usa `servicios-20.json` y `--concurrencia`).
3. Un servicio caído, lento o que contesta basura no tumba el reporte (usa `servicios-extra.json`, que agrega `facturacion`, `notificaciones` y `basura`).
4. Ninguna petición puede esperar para siempre.
5. Rechaza las direcciones que no son `http` o `https` y las que no están en la lista permitida.
6. Se ejecuta con `java -jar` desde otra carpeta y sale con 0 si todo está arriba y con 1 si no.

Antes de correr cada una, escribe qué esperas ver. Además de `servicios-extra.json` (10.3.3) y `servicios-20.json` (lección 9), necesitas cuatro archivos pequeños en la carpeta del proyecto. El primero tiene dos servicios que sí están arriba:

```json fig10_04/servicios-arriba.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"}
  ]
}
```

Y estos tres son direcciones que el `revisor` no debe consultar nunca: un esquema que no es de la web, un archivo local y la dirección del servicio de metadatos de una nube:

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

### Ejercicio 2 — Que `notificaciones` no desborde la columna (opcional)

Mira la tabla de `servicios-extra.json`: el nombre `notificaciones` tiene 14 caracteres y la columna SERVICIO mide 12, así que el nombre se pega al estado. Cambia `Formato` para que el ancho de la columna se calcule a partir del nombre más largo, con 12 como mínimo, y de modo que la tabla de los cuatro servicios canónicos salga **idéntica** a la de siempre.

### Ejercicio 3 — Abre cada puerta a propósito

Rompe, uno por uno, estos tres componentes de la calidad, **prediciendo antes** lo que dirá la construcción: (a) quita el comentario Javadoc de `main`; (b) quita la dependencia de Checkstyle 14.3.0 del `pom.xml`; (c) quita la línea `resultados = List.copyOf(resultados);` de `Reporte`. En cada caso anota qué herramienta habló, qué dijo, y si la sorprendió que no hablara.

### Ejercicio 4 — Apaga el servidor de dos maneras

Levanta el `revisor` con `--servir 8090`. Apágalo primero con `Ctrl`+`C` y lee el código de salida; vuelve a levantarlo y apágalo ahora desde otra terminal con `kill -TERM`. Explica los dos números y comprueba que el puerto quedó libre.

## Soluciones

### Solución 1

**1. `mvn -B verify` en verde, con el conteo leído del informe.** Lo que importa son los números, no el código de salida (por eso se cuentan dos veces, con dos métodos):

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

Siete clases de prueba, 34 pruebas, ninguna saltada.

**2. Veinte servicios, en serie contra a la vez.** Los veinte tardan medio segundo cada uno. Con `--concurrencia 1` (equivale a hacerlos en serie), con 10 y con 20, midiendo el programa completo con `time`:

```text
$ TIMEFORMAT='%R s'
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 1 > /dev/null
10.398 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 10 > /dev/null
1.245 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 20 > /dev/null
0.738 s
```

(Segundas corridas: 10.385, 1.253 y 0.747.) La suma de los tiempos es 10 s; con el límite en 20, el programa tarda lo que el más lento, más unos 0.2 s de arranque de la JVM. Lo que el temario pide, «cercano al del más lento, no a la suma», se cumple: 0.74 s contra 10.4 s.

**3. Lo que falla no tumba el reporte.** Con los tres servicios extra, la tabla sale completa y en el mismo orden; `facturacion` contesta 503, `notificaciones` agota su tiempo límite de 1000 ms (la ruta del laboratorio tarda 10 segundos) y `basura` cierra la conexión sin contestar:

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

(El nombre `notificaciones` desborda la columna: eso arregla el ejercicio 2.) Para ver que el **orden** no cambia ni siquiera con distinta concurrencia, se extraen los nombres del JSON con tres límites distintos:

```text
$ for c in 1 3 10; do java -jar target/revisor.jar --config servicios-extra.json --concurrencia $c --formato json | grep -o '"nombre" : "[a-z]*"' | cut -d'"' -f4 | tr '\n' ' '; echo; done
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
```

(Los tiempos de la columna TIEMPO cambian en cada corrida, y por eso el promedio de esta tabla no es el 656 que saldría con los números fijos de la lección 4: es el de los tiempos medidos.)

**4. Ninguna petición espera para siempre.** Se comprueba de dos formas. Por el código: sólo hay un lugar donde el `revisor` manda una petición, y ese lugar fija los dos tiempos.

```text
$ grep -n "connectTimeout\|\.timeout(" src/main/java/mx/habil/revisor/Revision.java
31:        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
39:            .timeout(Duration.ofMillis(servicio.timeoutMs()))
$ grep -rn "\.send(" src/main
src/main/java/mx/habil/revisor/Revision.java:44:      var respuesta = cliente.send(peticion, BodyHandlers.discarding());
```

Y por el reloj: la ruta `/notificaciones` del laboratorio tarda 10 segundos en contestar, y aun así el reporte completo con los siete servicios terminó en **2.5 segundos** (`time` dio 2.525 s): el límite de 1000 ms del servicio la cortó, y el más lento de los demás, `inventario`, fija el resto.

**5. Rechaza lo que no es `http` ni está permitido.** Tres archivos, tres rechazos, ninguno hace una sola petición, y los tres con código 2:

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

**6. Desde otra carpeta, con los dos códigos.** Ya viste el caso del código 1 en 10.6.3. Para el 0 se necesita una configuración donde todo esté arriba (`servicios-arriba.json`, con `catalogo` y `pagos`):

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

Las seis se cumplen. Anota en tu bitácora la fecha y el conteo de pruebas del primer punto: es la prueba de que terminaste.

### Solución 2

Se separa el patrón del formato del ancho, y `tabla` calcula el ancho a partir del nombre más largo (más dos espacios de separación), con 12 como mínimo. Con los cuatro servicios canónicos, el nombre más largo mide 10 y el cálculo da 12: la tabla no cambia. Estas son las partes de `Formato.java` que se modifican (las funciones `encabezado()` y `linea(Resultado)` sin ancho se reemplazan por las de abajo, que lo reciben):

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

Con ese cambio las 34 pruebas siguen verdes, Checkstyle da 0 infracciones y SpotBugs 0 defectos, y la tabla de `servicios-extra.json` queda alineada:

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

### Solución 3

(a) Sin Javadoc en `main`, habla **Checkstyle**: `MissingJavadocMethod: Falta el comentario Javadoc para 'main'.`, una infracción, y `verify` falla. Es lo que se espera: `main` es público.

(b) Sin la dependencia de Checkstyle 14.3.0, habla **Checkstyle, pero con un error de análisis y no de estilo**: `mismatched input 'when' expecting '->'` en `Clasificador.java`. Este es el que más sorprende, porque no menciona la versión y apunta a código correcto (10.4.2).

(c) Sin la copia defensiva, **no habla nadie**: `BugInstance size is 0`. Es la sorpresa que se anunciaba: `Reporte` es una clase de paquete y SpotBugs sólo se queja de lo que se ve desde fuera. La copia debe seguir ahí; la herramienta calla, pero la razón por la que existe no ha cambiado. Si haces público `Reporte` (con su constructor), entonces sí aparecen `EI_EXPOSE_REP` y `EI_EXPOSE_REP2` (10.4.4).

### Solución 4

Con `Ctrl`+`C` aparece `servidor detenido` y el código es **130**; con `kill -TERM` (por ejemplo, `kill -TERM $(pgrep -f 'revisor.jar --servir')` desde otra terminal) también aparece `servidor detenido`, y el código es **143**. Los dos números son `128 + la señal` (2 y 15), la convención de la terminal para los procesos que mueren por una señal; no son fallos. Después de apagarlo, en tu Mint `ss -ltn 'sport = :8090'` no debe mostrar ninguna línea: el cierre ordenado liberó el puerto. (Medí los códigos mandando las señales desde un guion; el comando `ss` no lo ejecuté en la máquina de medición.)

## Cómo sé que lo logré

- [ ] `java fig10_01.java --falla` imprime el error de uso y `echo $?` dice 2, y sé decir por qué el error se escribe con `System.err`.
- [ ] `java fig10_02.java` y `java fig10_03.java` terminan con las salidas de la lección, y puedo explicar para qué sirve cada uno de los cinco pasos del servidor mínimo y cuándo corre un gancho de apagado.
- [ ] **(1)** `mvn -B verify` dice `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`, `You have 0 Checkstyle violations` y `BugInstance size is 0`, y el conteo de pruebas contado en los XML de `surefire-reports` también da 34.
- [ ] **(2)** Con `servicios-20.json`, `--concurrencia 1` tarda unos 10 s, `--concurrencia 10` unos 1.2 s y `--concurrencia 20` unos 0.7 s (más o menos, según mi máquina), y los medí yo con `time`.
- [ ] **(3)** Con `servicios-extra.json` la tabla sale completa, con los siete nombres en el mismo orden con cualquier `--concurrencia`, y el código de salida es 1.
- [ ] **(4)** Sé señalar en `Revision.java` las dos líneas que fijan el tiempo de conexión y el tiempo límite, y comprobé que el reporte con `notificaciones` (una ruta de 10 s) termina en unos 2.5 s.
- [ ] **(5)** Los tres archivos `servicios-ftp.json`, `servicios-archivo.json` y `servicios-nube.json` se rechazan con el mensaje esperado y el código 2.
- [ ] **(6)** `java -jar` desde `/tmp` o desde mi carpeta personal, con las rutas completas, imprime la tabla y sale con 1; con `servicios-arriba.json`, sale con 0.
- [ ] Probé al menos una de las puertas de calidad en rojo y entendí el mensaje (Solución 3), incluido el silencio de SpotBugs ante `Reporte` sin copia.
- [ ] Anoté en mi bitácora qué hallazgo de Checkstyle o SpotBugs no esperaba.

## Resumen

Responde sin mirar la lección:

1. ¿Qué recibe `main` en `args`, y qué es lo que Java *no* hace con ello?
2. ¿Qué dice por convención un código de salida `0`, y qué distinguen entre sí el `1` y el `2` del `revisor`?
3. ¿Por qué `main` es una sola línea y la lógica vive en `ejecutar`? ¿Qué ganan las pruebas con eso?
4. ¿Por qué el resultado va a la salida estándar y los avisos a la de error?
5. ¿Qué diferencia hay entre escuchar en `127.0.0.1` y no poner dirección? ¿Qué significa el puerto 0?
6. ¿Qué es un gancho de apagado, cuándo corre y cuándo no, y por qué no debe llamar a `System.exit`?
7. ¿Por qué el hilo principal del modo servidor se queda esperando en un `CountDownLatch` que nadie abre?
8. ¿Qué revisa Checkstyle y qué revisa SpotBugs? ¿Por qué el silencio de SpotBugs ante una colección mutable en una clase de paquete no prueba que esté bien?
9. ¿Por qué la lista permitida no va dentro del archivo de servicios? ¿Por qué no se siguen las redirecciones?
10. ¿Qué agrega `maven-shade-plugin` que no hace `maven-jar-plugin`, y cómo se comprueba que el JAR es independiente?

## Para leer más

- [Oracle, `com.sun.net.httpserver.HttpServer` (Java 25)](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html) y [`Runtime.addShutdownHook`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html): el servidor del JDK y el ciclo de vida de un apagado. En inglés. Consultado el 7 de octubre de 2026.
- [Checkstyle, «Google's Style»](https://checkstyle.org/google_style.html) y [SpotBugs, catálogo de patrones](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html): qué comprueba cada regla y qué significa cada código. Consultado el 7 de octubre de 2026.
- [Apache Maven, `maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/) y [la especificación del JAR](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html): cómo se arma un JAR con todo adentro y qué dice su manifiesto. Consultado el 7 de octubre de 2026.
- [OWASP, «Server-Side Request Forgery Prevention»](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) y [«Deserialization»](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html): los dos riesgos del repaso de seguridad, con sus defensas. Consultado el 7 de octubre de 2026.

---

**Anterior:** [Lección 9 — Concurrencia: revisar todo a la vez](09-concurrencia.md)
