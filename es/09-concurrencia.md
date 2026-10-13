# Lección 9 — Concurrencia: revisar todo a la vez

**Tiempo:** 2 × 90 min (dos sesiones; el corte está entre las secciones 9.4 y 9.5)

**Qué construyes:** el `revisor` concurrente: consulta todos los servicios al mismo tiempo, con un límite que tú fijas, y entrega el reporte en el mismo orden de siempre

**Qué aprendes:** hilos de plataforma e hilos virtuales; un hilo virtual por tarea y nunca en un *pool*; `ExecutorService` como recurso que se cierra; `invokeAll`, el orden estable y `ExecutionException`; `Semaphore` para limitar; una condición de carrera provocada y medida; `ConcurrentHashMap` y los tipos atómicos; `ScopedValue`; y la concurrencia estructurada como «lo que viene»

**De dónde vienes.** Traes el proyecto de la [Lección 8](08-http-y-json.md): un `revisor` en Maven, con `HttpClient`, Jackson, la configuración validada en `servicios.json` y 25 pruebas en verde. Hace una petición real por servicio, mide cuánto tarda, y no se cae cuando un servicio no contesta. Pero las hace **una tras otra**: con los cuatro servicios canónicos tarda lo que suman sus tiempos, unos 2.5 segundos, y con veinte servicios de medio segundo cada uno tardaría diez. Esa lentitud no es del programa, es de su forma: el `for` de `revisarTodos` espera la respuesta de `catalogo` antes de preguntarle a `inventario`, aunque ninguno de los dos necesite al otro. Hoy se la quitas. Trabajas en tu carpeta `~/curso-java/revisor/`.

**Qué no hace esta lección.** No agrega opciones de línea de comandos: el límite de concurrencia es una constante hasta la [Lección 10](10-el-programa-terminado.md), donde se vuelve `--concurrencia`. Tampoco cambia cómo se revisa un servicio: `revisar` queda tal cual, con sus tiempos límite y sus tres motivos de caída. Lo único que cambia es **quién llama a `revisar` y cuántas veces a la vez**.

**Sobre las corridas.** Las figuras 9.1 a 9.8 son archivos compactos y se corren con `java figNN_MM.java` desde la carpeta de la lección; no necesitan el laboratorio. La figura 9.9 es el proyecto, y sí lo necesita: en otra terminal, desde `~/curso-java/`, deja corriendo `java Laboratorio.java` (escucha en el puerto 8089; el puerto 8088 queda vacío a propósito, es el de `reportes`). Todas las salidas se obtuvieron con Temurin 25.0.4 en una máquina de 10 núcleos. **Los tiempos que ves en esta lección dependen de tu máquina**: lo que se repite en todas son las proporciones, no los milisegundos. Las cifras que cambian de una corrida a otra (como el resultado de una carrera) se presentan como fragmento, con una corrida real, y no como salida verificada.

## Al terminar vas a poder

- Explicar qué es un hilo virtual, en qué se distingue de un hilo de plataforma y por qué un programa que espera mucho (como el `revisor`) los aprovecha.
- Lanzar una tarea por cada servicio con `Executors.newVirtualThreadPerTaskExecutor()` dentro de un `try-with-resources`, y decir por qué no se usa un *pool* de hilos virtuales.
- Usar `invokeAll` para esperar todas las tareas y recuperar sus resultados en el orden de entrada, y leer una `ExecutionException` hasta su causa.
- Limitar cuántas tareas avanzan a la vez con un `Semaphore`, liberándolo siempre en un `finally`.
- Provocar una condición de carrera, medir cuánto se pierde, y arreglarla con `ConcurrentHashMap.merge` o con un contador atómico.
- Explicar por qué el `revisor` no tiene carreras (no comparte estado mutable), y para qué sirven `ScopedValue` y la concurrencia estructurada.
- Medir con el reloj el efecto del límite sobre veinte servicios y explicar por qué un límite de 10 tarda el doble que uno de 20.

## El porqué antes del cómo

Imagina una cocina con un solo cocinero que prepara cuatro platillos. Mete el primero al horno y se queda mirándolo veinte minutos hasta que está listo; solo entonces empieza el segundo. Nadie diría que ese cocinero es lento con las manos. Es lento **porque espera sin hacer nada más**. El `revisor` de la lección 8 es ese cocinero: cuando le pregunta a `inventario`, que tarda 2.3 segundos en contestar, el programa se queda parado 2.3 segundos, con el procesador casi dormido, mientras `pagos` y `catalogo` podrían estar contestando al mismo tiempo.

La solución también es de cocina: meter varios platillos al horno a la vez. En programación se llama **concurrencia**: varias tareas en curso al mismo tiempo, de modo que el tiempo que una pasa esperando lo aprovecha otra. (No es lo mismo que el **paralelismo**, que es tener varios procesadores trabajando literalmente a la vez. La concurrencia es una forma de organizar el programa; el paralelismo, algo que el equipo puede o no ofrecer. En esta lección vas a ver las dos, pero el beneficio del `revisor` viene sobre todo de la primera.)

Hacerlo en Java ha tenido siempre un obstáculo, y esta lección existe por el modo en que Java lo quitó. Durante más de veinte años, la unidad de concurrencia fue el **hilo del sistema operativo**: un hilo es una línea de ejecución independiente dentro del programa, y cada hilo de Java era un hilo del sistema, con su memoria propia y un costo real. Crear diez mil de ellos para diez mil peticiones no era viable, así que la práctica estándar fue tener unos pocos hilos «de planta» (un *pool*) y turnarse las tareas, con colas, límites y mucho código para que nadie se quedara esperando de más. Desde Java 21, la plataforma ofrece otra cosa: **hilos virtuales**, tan baratos que cada tarea puede tener el suyo, y con ellos el código vuelve a ser lo que era en la cocina de un solo cocinero —una línea tras otra, cada paso esperando al anterior—, sólo que ahora son muchos cocineros baratos.

Esta lección tiene dos sesiones. La **primera** (9.1 a 9.4) enseña los hilos virtuales, `invokeAll` y el semáforo, y termina con una medición: el `revisor` revisando veinte servicios a la vez. La **segunda** (9.5 a 9.8) enseña lo que hay que saber para que la concurrencia no te traicione: el estado compartido, la condición de carrera que lo vuelve peligroso, los valores con ámbito, y lo que viene en la plataforma. Es la lección que más cambia lo que sabes de Java, y la que más te va a servir: cualquier servidor moderno en Java se escribe con estas ideas.

## Los conceptos

### 9.1 Dos clases de hilo

#### 9.1.1 Qué es un hilo y qué cuesta

Un **hilo** (*thread*) es una secuencia de instrucciones que avanza por su cuenta. Tu programa siempre tiene al menos uno: el que ejecuta `main`. Con más hilos, varias secuencias avanzan «a la vez»: el sistema operativo reparte el tiempo de los procesadores entre ellos, y cuando uno se bloquea —esperando una respuesta de la red, por ejemplo— le da el turno a otro.

Hasta Java 20 cada `Thread` era exactamente eso: **un hilo del sistema operativo**, con su propia pila de llamadas y su propio registro en las tablas del sistema. A esos se les llama ahora **hilos de plataforma** (*platform threads*). Son poderosos y caros: el sistema impone un tope al número de hilos, cada uno reserva memoria para su pila, y cambiar de uno a otro cuesta. Por eso un programa de servidor no podía permitirse un hilo por petición cuando había miles de peticiones, y la técnica fue compartirlos con un *pool*.

Un **hilo virtual** es otra cosa, aunque se usa igual. Es también un `Thread` —el mismo tipo, los mismos métodos, `join`, `interrupt`—, pero lo administra la máquina virtual de Java en vez del sistema operativo. Un hilo virtual no está atado a un hilo del sistema: la JVM tiene un grupo pequeño de hilos de plataforma, llamados **hilos portadores** (*carrier threads*), y «monta» sobre ellos a los hilos virtuales que tienen algo que hacer. Cuando un hilo virtual llega a una operación que bloquea —leer de un socket, dormir, esperar un semáforo—, la JVM lo **desmonta**: guarda su estado en la memoria del montón, deja libre al portador y monta a otro hilo virtual que sí puede avanzar. Cuando la respuesta llega, el hilo virtual vuelve a montarse en algún portador. Es la misma idea del cocinero que no se queda mirando el horno, hecha por la plataforma en lugar de por ti. La [documentación oficial de los hilos virtuales](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html) lo describe con estos términos, y la especificación del cambio es [JEP 444](https://openjdk.org/jeps/444), final desde Java 21.

La figura 9.1 los pone lado a lado. Pide un hilo de plataforma y uno virtual con el mismo constructor de hilos (`Thread.ofPlatform()` y `Thread.ofVirtual()`), les da nombre, los arranca, y cada uno dice qué es:

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

Las figuras de esta lección no llevan `import`: un archivo compacto importa por sí solo todo lo que exporta `java.base`, que incluye `java.util.concurrent`. El método `join()` hace que `main` espere a que el hilo termine; sin él, el programa podría terminar antes de que el hilo imprima. Esto es lo que imprime:

```text
$ java fig09_01.java
de-plataforma: virtual=false, demonio=false
virtual-1: virtual=true, demonio=true
```

Tres datos en dos renglones. El hilo de plataforma es `virtual=false`; el otro, `virtual=true`. Y el virtual es **demonio** (`demonio=true`): un hilo demonio no mantiene viva a la JVM, de modo que si `main` termina y solo quedan hilos virtuales, el programa se acaba sin esperarlos. Es una diferencia que muerde: si lanzas un hilo virtual «para que trabaje de fondo» y `main` termina, lo cortas. (Por eso el ejecutor de 9.2 se cierra esperando a sus tareas.)

Puedes ver la mecánica de los portadores en cualquier hilo virtual. Imprimir `Thread.currentThread()` desde uno da algo así:

```text
VirtualThread[#26]/runnable@ForkJoinPool-1-worker-1
```

Después de la arroba está el hilo portador sobre el que corría en ese momento. Al lanzar veinte mil hilos virtuales y anotar su portador, en mi máquina aparecieron exactamente diez nombres, de `worker-1` a `worker-10`: tantos como núcleos tiene. Veinte mil hilos «de Java» repartidos en diez hilos «del sistema».

#### 9.1.2 Lo que se gana: escala, no velocidad

La figura 9.2 pone la diferencia en números, con una tarea que no hace nada más que esperar 200 milisegundos (un servicio que tarda en contestar). Lanza mil de ellas dos veces: una en un *pool* clásico de 50 hilos de plataforma y otra con un hilo virtual por tarea. Mide cada una con el reloj del sistema (`System.nanoTime()`):

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

Un par de detalles de lectura antes de correrla. `try (ejecutor) { ... }` usa una variable que ya existe como recurso del `try-with-resources` (Java lo permite desde la 9, mientras la variable no se reasigne); es la misma idea de la lección 5, y al salir del bloque se llama `close()`. `this::esperar` es una referencia a un método de esta misma clase, que Java convierte en la tarea. Y las dos líneas finales imprimen un booleano, no los milisegundos, para que la salida sea siempre la misma:

```text
$ java fig09_02.java
pool de 50 hilos de plataforma, más de 3 s: true
un hilo virtual por tarea, menos de 1 s: true
```

Los milisegundos reales los ves si imprimes las variables `conPool` y `conVirtuales`. En tres corridas seguidas me dieron 4,152, 4,148 y 4,161 ms para el *pool*, y 221, 214 y 206 ms para los hilos virtuales. La aritmética explica el primero: mil tareas entre 50 hilos son 20 tandas, y 20 tandas de 200 ms son 4 segundos. Con un hilo virtual por tarea no hay tandas: las mil esperan juntas, y el tiempo total es el de una sola espera, más el costo de arrancar. En otra medición, cien mil hilos virtuales durmiendo un segundo cada uno terminaron en 1.4 segundos.

Aquí conviene frenar un malentendido que arruina más de un diseño. La documentación de Oracle lo dice sin rodeos: los hilos virtuales **no son hilos más rápidos**; no ejecutan el código más aprisa que uno de plataforma. Dan **escala** (más tareas en curso a la vez), no **velocidad** (menos tiempo por tarea). Y sirven para tareas que pasan casi todo su tiempo **esperando**, no para trabajo de procesador: si cada tarea fuera una factorización larga, diez mil hilos virtuales no terminarían antes que diez, porque los núcleos son los mismos. El `revisor` es el caso ideal: cada tarea manda una petición y espera.

Un dato más, que te ahorra cuidados que vas a leer en libros anteriores: con Java 21 había un problema llamado *pinning* (un hilo virtual que bloqueaba dentro de un bloque `synchronized` se quedaba pegado a su portador y no lo liberaba). Desde Java 24 [ya no ocurre](https://openjdk.org/jeps/491) en esos bloques; el consejo de «evita `synchronized` con hilos virtuales» que todavía circula es de otra época.

### 9.2 Un hilo virtual por tarea

#### 9.2.1 El ejecutor que cierra

Crear hilos a mano con `Thread.ofVirtual().start(...)` sirve para entender, no para organizar trabajo. Para organizarlo se usa un **ejecutor**, un objeto al que le entregas tareas y que se encarga de correrlas. La fábrica que nos interesa es una línea:

```java
try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
  // aquí se mandan tareas con ejecutor.submit(...) o ejecutor.invokeAll(...)
}
```

Su nombre lo dice todo: *virtual thread per task executor*, «ejecutor de un hilo virtual por tarea». Cada tarea que le mandas recibe un hilo virtual nuevo, que muere cuando la tarea termina. No hay cola, no hay tamaño de *pool*, no hay nada que configurar. Y como `ExecutorService` es `AutoCloseable` desde Java 19, el `try-with-resources` hace lo importante: al salir del bloque llama a `close()`, que **espera a que todas las tareas terminen** antes de continuar. Es la garantía que no tenían los ejecutores viejos, donde olvidarse del `shutdown()` dejaba hilos vivos y un programa que no terminaba. Aquí el bloque `try` es un cerco: lo que empiezas adentro termina adentro.

La regla de esta sección cabe en una frase, y es la que el temario del curso pone en negritas: **un hilo virtual por tarea, nunca en un *pool***. Un *pool* existe para repartir un recurso escaso —los hilos de plataforma— entre muchas tareas. Los hilos virtuales no son escasos: se pueden tener millones. Si les pones un *pool* encima, vuelves a crear una escasez artificial, con las mismas colas y los mismos topes, y pierdes justo lo que los hacía útiles. La guía oficial lo resume así: los hilos virtuales no deben representar un recurso compartido sino **una tarea**. Si lo que quieres es limitar cuántas tareas avanzan a la vez, existe una herramienta hecha para eso, y la ves en 9.4.

#### 9.2.2 Por qué un `send` que bloquea está bien

Mira otra vez cómo revisa un servicio el `revisor` desde la lección 8: `cliente.send(peticion, BodyHandlers.discarding())` **bloquea**: el hilo que lo llama se queda esperando hasta que llega la respuesta o se agota el tiempo. En un hilo de plataforma eso era un lujo caro; por eso la programación concurrente en Java pasó años buscando formas de no bloquear (*callbacks*, `CompletableFuture`, bibliotecas reactivas), con código que se lee de cabeza. En un hilo virtual el bloqueo **no cuesta nada**: la JVM desmonta al hilo mientras espera y usa el portador para otro. Por eso `revisar` no cambia ni una línea en esta lección. Sigue siendo el método lineal de la lección 8: arma la petición, la manda, espera, clasifica. La concurrencia no está dentro de `revisar`; está en cómo se le llama.

Dos cosas de esa llamada hay que tenerlas presentes. Primero, el `HttpClient` **se comparte**: la documentación de la clase dice que una instancia es inmutable y sirve para mandar muchas peticiones, así que el `revisor` crea uno solo y todas las tareas lo usan; crear un cliente por petición sería tirar sus conexiones reutilizables. Segundo, el cliente también es `AutoCloseable`, y el orden de cierre importa: en un `try-with-resources` con dos recursos se cierran **al revés de como se declararon**. Por eso el cliente se declara primero y el ejecutor después: al salir, se cierra el ejecutor (espera a todas las tareas) y *luego* el cliente. Al revés, cerrarías el cliente con peticiones en vuelo.

### 9.3 `invokeAll` y el orden estable

#### 9.3.1 `Callable`, `Future` y esperar a todas

`submit` acepta dos clases de tarea. Un `Runnable` no devuelve nada ni puede lanzar una excepción verificada. Un **`Callable<T>`** devuelve un valor de tipo `T` y puede lanzar cualquier excepción: es lo que necesita `revisar`, que devuelve un `Resultado` y puede lanzar `InterruptedException` (la lección 5 explicó por qué esa nunca se traga). Lo que recibes al mandar una tarea es un **`Future<T>`**: una promesa de resultado. `futuro.get()` espera —si hace falta— a que la tarea termine y entrega su valor.

El método que une las dos cosas es `invokeAll`: recibe una colección de `Callable`, las ejecuta todas, **espera a que todas terminen** y devuelve una `List<Future<T>>`. Lo importante está en la documentación y es lo que lo hace adecuado para el `revisor`: la lista que devuelve tiene **el mismo orden que la que le diste**, sin importar cuál terminó primero. La figura 9.3 lo comprueba con tres tareas que tardan 300, 50 y 150 ms; la más rápida es la segunda de la lista, y aun así sale segunda:

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

Sigue la figura por partes. `tarea` duerme y devuelve su nombre. La lista de tres `Callable` se escribe con lambdas, y `invokeAll` las corre todas con hilos virtuales. El primer `for` recorre los `Future` en orden. Luego se arma una segunda lista donde una tarea **lanza una excepción**, y se ve algo importante: `invokeAll` regresa normalmente, la tarea aparece como terminada, y el fallo no explota hasta que pides su valor con `get()`. Esta es la corrida:

```text
$ java fig09_03.java
en el orden de la lista: lenta
en el orden de la lista: rapida
en el orden de la lista: mediana
invokeAll regresó; la tarea 2 terminó: true
get() lanza ExecutionException
la causa es java.lang.IllegalStateException: fallé a propósito
```

Los tres primeros renglones salen en el orden de la lista (`lenta`, `rapida`, `mediana`), no en el orden en que las tareas terminaron (que fue `rapida`, `mediana`, `lenta`). Para el `revisor` esto es lo que garantiza el punto 3 de «Cómo sabes que terminaste»: el reporte sale **siempre en el mismo orden**, el de la configuración, aunque `inventario` tarde dos segundos y `pagos` ochenta y siete milisegundos. Si recogieras los resultados conforme llegan, el orden cambiaría en cada corrida y comparar dos reportes sería un dolor.

#### 9.3.2 `ExecutionException`: la excepción de otro hilo

Cuando una tarea lanza una excepción en su hilo, esa excepción no puede subir por la pila de quien la mandó —son pilas distintas—. Java la guarda dentro del `Future` y, al llamar `get()`, la entrega **envuelta** en una `ExecutionException`. Las dos últimas líneas de la figura lo muestran: el tipo que atrapas es `ExecutionException`, pero lo que importa es su **causa**, `getCause()`, que es la excepción original (`IllegalStateException: fallé a propósito`). Quien la atrapa y solo imprime el mensaje de la envoltura pierde justo la información que necesita. Es el mismo principio de la lección 5, el de las excepciones propias con su causa: una excepción envuelta se desenvuelve, no se descarta.

Esto te obliga a decidir qué hacer con ella, y la decisión del `revisor` es deliberada. Hay dos clases de problema. Un **servicio caído** es un resultado esperado: `revisar` lo atrapa y lo convierte en un `Resultado` con estado `CAIDO` y su motivo, y nunca deja escapar una `IOException`. Por eso ninguna tarea falla por culpa de la red, y el reporte sale completo aunque medio mundo esté caído. Un **error del programa** (un `NullPointerException`, un error de lógica) es otra cosa: si una tarea falla así, algo está mal en el código, y esconderlo detrás de un «servicio caído» sería mentir. El `revisor` lo relanza como `IllegalStateException` con la causa original adjunta. En una frase: **un servicio caído no tumba el reporte; un error del programa no se esconde**.

Un último detalle del contrato: si el hilo que espera en `invokeAll` es interrumpido, el método **cancela las tareas que no hayan terminado** y lanza `InterruptedException`. Por eso la firma de `revisarTodos` declara `throws InterruptedException` y no la traga.

### 9.4 Limitar con `Semaphore`

#### 9.4.1 Para qué limitar si los hilos son baratos

Si los hilos virtuales son casi gratis, ¿para qué limitar? Porque el recurso escaso ya no es el hilo; es **lo que hay del otro lado**. Veinte servicios consultados a la vez es una cortesía razonable. Veinte mil servicios consultados a la vez —o veinte peticiones simultáneas contra el mismo servidor— pueden tumbar a un servicio frágil, agotar los puertos del sistema o disparar la alarma de un proveedor. El límite protege al **destino**, no a la JVM. Y esa decisión no debería depender del número de hilos que casualmente tenga un *pool*.

Java tiene una herramienta pensada para esto desde hace veinte años: el **`Semaphore`**. Un semáforo guarda un número de **permisos**. Quien quiere avanzar llama `acquire()` (tomar un permiso); si no hay, **espera** hasta que otro lo suelte con `release()`. Con `new Semaphore(2)`, a lo más dos tareas están dentro de la zona protegida; las demás hacen fila. La figura 9.4 lanza seis tareas de 100 ms con un semáforo de dos permisos, y registra el máximo de tareas simultáneas con un `AtomicInteger` (un entero que se puede modificar desde varios hilos sin perder cuentas; vuelve en 9.5):

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

Observa la forma de la tarea: `acquire()`, luego un `try` con el trabajo, y `release()` en el **`finally`**. No es estilo: es una condición de correctitud. Si el trabajo lanza una excepción y el `release()` no está en un `finally`, ese permiso se pierde para siempre; tras suficientes fallos, todos los permisos están perdidos y las tareas que quedan esperan eternamente, un programa colgado sin ningún error a la vista. La corrida:

```text
$ java fig09_04.java
tareas lanzadas: 6, máximo al mismo tiempo: 2
```

Seis tareas lanzadas, nunca más de dos al mismo tiempo. Y fíjate en lo que **no** hizo el semáforo: no limitó los hilos. Las seis tareas tienen su hilo virtual desde el principio; cuatro están dormidas en `acquire()` y no cuestan nada. La guía oficial de Oracle recomienda justo esto —un semáforo para limitar— y explica que su efecto se parece al de un *pool* de tamaño fijo, porque en ambos casos las tareas que sobran hacen fila, pero sin reservar hilos para eso.

#### 9.4.2 Por qué no un *pool* de hilos virtuales

La tentación es resolver el límite con lo que ya conoces: `Executors.newFixedThreadPool(2, fabrica)`, donde la fábrica produce hilos virtuales. Funciona, y esa es la trampa. La figura 9.5 mide el máximo de tareas simultáneas de las dos formas:

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

El *pool* fijo limita a 2, el ejecutor por tarea deja pasar a las 6. Hay dos problemas con usar el primero para limitar. Uno es de claridad: el límite quedó escondido en un argumento numérico de una fábrica, y nadie que lea el código entenderá que «2» significa «peticiones simultáneas permitidas». El otro es de ingeniería: acabas de reintroducir una cola, un tamaño que afinar y un hilo que reservar, para obtener lo que `Semaphore(2)` da en una línea y con su propósito escrito en el nombre. La guía oficial es tajante: los hilos virtuales **no deben agruparse nunca**. Menos concurrencia, más código, ninguna ventaja.

#### 9.4.3 El paso del proyecto: `revisarTodos` concurrente

Ya tienes las piezas. Así queda `Revision.java`; lo nuevo es la constante `LIMITE_POR_OMISION` y el método `revisarTodos`, y todo lo demás (`revisar`, `nuevoCliente`) es el de la lección 8. Lee `revisarTodos` en orden: primero valida el límite; crea el semáforo; abre el cliente y el ejecutor (en ese orden, como viste en 9.2.2); arma una tarea por servicio, que toma un permiso, revisa, y lo suelta en un `finally`; manda todas con `invokeAll`; y recoge los resultados **en orden**, desenvolviendo cualquier `ExecutionException`:

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

Tres decisiones de esta función que conviene ver de cerca. La primera: **el semáforo se toma dentro de la tarea, no antes de lanzarla**. Todas las tareas existen desde el inicio, y cada una espera su turno por sí misma; el `invokeAll` queda simple. La segunda: el tiempo que `revisar` mide con `System.nanoTime()` empieza **después** de tomar el permiso, de modo que la columna TIEMPO sigue diciendo cuánto tardó el servicio y no cuánto esperó en la fila. La tercera: `limite < 1` se rechaza de entrada con una `IllegalArgumentException`, porque un semáforo con cero permisos dejaría a todas las tareas esperando para siempre.

La constante `LIMITE_POR_OMISION` vale 10, y `Revisor` la usa por ahora. `Revisor` cambia en dos cosas: el primer argumento, si lo hay, es el archivo de configuración (así puedes revisar otra lista sin editar nada), y llama a `revisarTodos` con el límite por omisión:

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

El archivo `servicios-20.json` tiene veinte servicios, `servicio01` a `servicio20`, todos apuntados a la ruta `/espera/500` del laboratorio, que contesta 200 después de medio segundo. Así es completo:

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

### Medir: el reloj decide

Con el laboratorio encendido, compila (`mvn -B verify`) y corre el `revisor` contra esos veinte servicios, midiendo cuánto tarda el programa completo. En Linux, en una terminal con `bash`, la forma limpia de medir es la palabra reservada `time` con un formato corto:

```text
$ TIMEFORMAT='%R s'
$ time java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor servicios-20.json > /dev/null
1.258 s
```

Cambia la constante `LIMITE_POR_OMISION` en `Revision.java` (por ahora no hay otra forma; en la Lección 10 será `--concurrencia`), recompila y repite. Estas son mis mediciones, dos corridas por límite, siempre con los veinte servicios de 500 ms:

| Límite | Tandas | Lo que predice la aritmética | Lo que medí (ambas corridas) |
|---|---|---|---|
| 1 (equivale a **en serie**) | 20 | 20 × 0.5 = 10 s | 10.413 s y 10.387 s |
| 3 | 7 | 7 × 0.5 = 3.5 s | 3.808 s y 3.799 s |
| 10 | 2 | 2 × 0.5 = 1 s | 1.258 s y 1.298 s |
| 20 | 1 | 1 × 0.5 = 0.5 s | 0.751 s y 0.779 s |

Dos cosas para leer la tabla sin engañarte. **Una:** lo medido siempre pasa de lo predicho por unos 0.2 segundos, y es el costo fijo de arrancar la JVM y el cliente HTTP. Lo comprobé: un solo servicio con espera cero tarda 0.2 segundos en total. Resta 0.2 a cada medición y la aritmética se cumple con una o dos décimas de diferencia. **Dos:** la última columna responde la pregunta del temario, «por qué un límite de 10 tarda el doble que uno de 20». Con límite 10 y veinte servicios hay **dos tandas**: los primeros diez toman permiso, esperan medio segundo; cuando sueltan, entran los otros diez y esperan otro medio segundo. Con límite 20 entran todos de golpe. El tiempo total es el número de tandas por lo que dura cada espera, no el número de servicios.

Con los cuatro servicios canónicos, en cambio, la mejora es modesta: pasan de unos 2.8 segundos en serie a unos 2.5. No es un defecto: el tiempo ya no es la suma (2.5 s) sino **el del más lento** (`inventario`, 2.3 s), y como ese manda, hay poco que ganar. La concurrencia ayuda cuando hay muchas esperas parecidas; con una dominante, casi no ayuda. Es importante que lo sepas, porque «se hizo concurrente» no significa «se hizo rápido»: se mide.

Y con los cuatro servicios canónicos de `servicios.json`, desde la carpeta del proyecto y con el laboratorio encendido, una corrida completa se ve así:

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

Los tiempos de la columna TIEMPO son los de una corrida mía; los tuyos serán distintos, pero ESTADO y DETALLE no cambian nunca.

*(Aquí termina la primera sesión.)*

### 9.5 Estado compartido: donde la concurrencia muerde

#### 9.5.1 Una condición de carrera, provocada y medida

Todo lo anterior fue fácil porque las tareas no se tocan entre sí. En cuanto dos hilos **escriben el mismo dato**, aparece el problema que le da mala fama a la concurrencia. Se ve mejor provocándolo. Este programa lanza 10,000 tareas, y cada una suma 100 veces uno a un contador que comparten. Si todo saliera bien, el total sería exactamente un millón. Guárdalo como `Carrera.java`:

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

No lleva marcador de figura a propósito: su resultado cambia en cada corrida, así que no se puede verificar contra una salida fija. Corrí el programa cuatro veces seguidas y esto fue lo que contó:

```text
esperado 1000000, contado 841360
esperado 1000000, contado 852985
esperado 1000000, contado 773685
esperado 1000000, contado 855739
```

No hay una sola excepción, no hay un aviso del compilador, y el programa «funciona»: termina, imprime un número, sale con código 0. Pero **perdió entre 144,000 y 226,000 sumas**, y no pierde las mismas dos veces. Este es el rasgo que vuelve peligrosa a una carrera: no se anuncia. Un programa con una carrera pasa sus pruebas el 99 % de las veces y falla en producción, un martes, con carga.

Por qué pasa es más sencillo de lo que parece. `contador++` se ve como una operación, pero son tres: **leer** el valor, **sumarle uno** y **escribir** el resultado. Si dos hilos leen el 41 a la vez, los dos calculan 42 y los dos escriben 42: se sumó una vez en lugar de dos, y una suma desapareció. Con diez portadores trabajando en paralelo de verdad, ese traslape ocurre cientos de miles de veces. (Hay además una segunda causa, la de la **visibilidad**: sin una instrucción que lo ordene, la memoria de un hilo puede no ver lo que escribió otro. El [modelo de memoria de Java](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html), en el capítulo 17 de la especificación del lenguaje, define cuándo una escritura queda garantizada de verse: la regla se llama *happens-before*.)

No es un problema exclusivo de los números. Con una colección compartida es peor. El mismo experimento con un `HashMap` —guarda 10,000 claves distintas, cada una desde su tarea— debería terminar con 10,000 entradas. Guárdalo como `MapaCompartido.java`:

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

Mis tres corridas terminaron con 8,339, 7,195 y 7,563 entradas. Faltaban entre 1,600 y 2,800 entradas. Un `HashMap` no está diseñado para escribirse desde varios hilos, y su estructura interna se corrompe: además puede lanzar excepciones y, en versiones antiguas de Java, quedarse en un ciclo infinito. Que a veces «salga bien» no es consuelo; es lo que lo hace difícil de cazar.

#### 9.5.2 Tres maneras de arreglarlo, en orden de preferencia

**La primera, y la mejor: no compartir.** Si cada tarea trabaja con sus propios datos y devuelve un resultado, no hay nada que proteger. Es lo que hace el `revisor`, y lo explico en 9.5.3.

**La segunda: tipos hechos para compartirse.** El paquete [`java.util.concurrent`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/package-summary.html) trae tipos cuyas operaciones son atómicas, es decir, indivisibles. `AtomicInteger` y `AtomicLong` son enteros que se pueden sumar desde varios hilos sin perder cuentas; `LongAdder`, pensado para contadores muy disputados, es más rápido cuando hay muchos hilos. Y `ConcurrentHashMap` es el mapa para escrituras concurrentes. Su método `merge` es el que corrige el caso típico de contar por categoría: `mapa.merge("ARRIBA", 1, Integer::sum)` significa «si la llave no existe, ponle 1; si existe, sustitúyela por el resultado de sumar»; y según la documentación, toda la invocación se hace **de forma atómica**. La figura 9.6 repite el experimento de contar estados con 10,000 tareas, ahora con `merge`, y suma también con un contador atómico:

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

A diferencia de la carrera, este resultado sale **siempre** igual, las veces que lo corras: 10,000 en el mapa y 10,000 en el contador. Eso sí se puede verificar, y por eso esta figura sí lleva marcador.

**La tercera: candados.** Java tiene `synchronized` y los candados de `java.util.concurrent.locks` para proteger un trozo de código de modo que solo un hilo a la vez lo ejecute. Funcionan, y a veces son lo único que sirve; pero son la herramienta más fácil de usar mal (bloqueos mutuos, candados que se olvidan, secciones demasiado largas), y el curso no los usa. Con las dos opciones anteriores casi nunca hacen falta.

#### 9.5.3 Por qué el `revisor` no tiene carreras

Revisa cada dato que tocan las tareas de `revisarTodos`, uno por uno, porque aquí se cobra lo que aprendiste en la lección 3. El `Servicio` que recibe cada tarea es un *record*: **inmutable**, nadie lo cambia. El `HttpClient` es seguro para compartirse (la documentación lo garantiza). La tarea no escribe en ninguna variable de afuera: **devuelve** su `Resultado`, que es otro *record* inmutable. El único objeto mutable que comparten las tareas es el `Semaphore`, que está hecho precisamente para eso. Y la lista `resultados`, que sí es una `ArrayList` normal (no segura para varios hilos), solo la toca el hilo que llamó a `revisarTodos`, **después** de que `invokeAll` terminó; el propio paquete `java.util.concurrent` documenta que lo que una tarea hizo antes de terminar queda garantizado de verse en quien recoge su resultado con `get()`.

La regla de diseño que sale de aquí vale más que cualquier herramienta: **comunica devolviendo valores, no compartiendo memoria**. Cuando una tarea necesita decirle algo a quien la lanzó, que lo devuelva. La inmutabilidad que el curso viene cuidando desde la lección 3 no era un adorno de estilo: es lo que hace que una pieza se pueda correr en cien hilos sin pensar en ellos.

### 9.6 Valores con ámbito: `ScopedValue`

Hay un problema que las tareas concurrentes agravan: el **contexto**. Imagina que cada revisión lleva un identificador de corrida, `r-001`, y que quieres que cualquier método —el que clasifica, el que arma la petición, el que escribe un mensaje de diagnóstico— pueda leerlo. La salida obvia es pasarlo como parámetro por cada método de la cadena, y se vuelve un ruido que nadie quiere. La salida de siempre en Java fue `ThreadLocal`, una variable que tiene un valor distinto en cada hilo. Pero tiene tres defectos conocidos: es **mutable** (cualquiera la cambia, y nadie sabe quién), vive **hasta que alguien la limpie** (olvidarla es una fuga de memoria), y copiarla a hilos hijos es **cara**, lo que pesa cuando son millones de hilos virtuales.

Para eso existe `ScopedValue`, final desde Java 25 ([JEP 506](https://openjdk.org/jeps/506)). Un valor con ámbito es **inmutable** y existe **solo mientras dura un bloque de código**: lo enlazas con `ScopedValue.where(CLAVE, valor).run(...)` (o `.call(...)` si el bloque devuelve algo o lanza excepciones), y cualquier método llamado desde adentro puede leerlo con `CLAVE.get()`. Al salir del bloque, el valor desaparece solo. La figura 9.7 lo muestra, y muestra también su límite:

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

El método `anotar` no recibe el identificador como parámetro y lo lee igual: ese es el servicio que presta. La segunda línea es la parte que sorprende: el hilo virtual que se crea **dentro** del bloque a mano **no ve el valor** (`ligado = false`). El valor viaja a los llamados de un mismo hilo, no a los hilos que lanzas por tu cuenta; solo la concurrencia estructurada de 9.7 lo hereda a sus hijos. Y la tercera línea confirma que fuera del bloque no queda nada.

El `revisor` de este curso **no usa** `ScopedValue`: no tiene ningún contexto que pasar. Lo verás en cuanto escribas un servidor que atienda peticiones de varios usuarios y necesites saber, en cualquier punto del código, para quién estás trabajando. Está aquí porque es lo que reemplaza a `ThreadLocal` en el Java que vas a escribir de aquí en adelante, y porque se estrenó en esta versión.

### 9.7 Lo que viene: la concurrencia estructurada

En 9.3 viste que `invokeAll` espera a todas las tareas, pase lo que pase. Falta una forma que mucha gente quiere: «lanza estas tareas **como una unidad**: si una falla, cancela las demás; si el bloque termina, ninguna sigue viva». A eso se le llama **concurrencia estructurada**, y el JDK 25 la trae... **en vista previa**. Una vista previa (*preview*) es una función completa pero todavía sujeta a cambios, que solo se activa con una bandera. Esta ha cambiado de forma en cada versión desde la 21; la quinta vista previa es [JEP 505](https://openjdk.org/jeps/505).

La figura 9.8 usa la API (`StructuredTaskScope.open()`, con `fork` para lanzar y `join` para esperar). Compilada tal cual, **no corre**:

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

Leer el mensaje te dice todo: la clase existe, pero es una API de vista previa y está apagada. Con la bandera que pide el propio mensaje, sí funciona:

```text
$ java --enable-preview --source 25 fig09_08.java
respuesta de catalogo / respuesta de pagos
```

**No la uses en el `revisor` ni en producción**: la API puede cambiar entre versiones del JDK, y un programa que dependa de ella tendría que reescribirse cuando cambie. Por eso este curso la deja como curiosidad. Lo que sí te llevas es la idea: cuando se estabilice, será la forma natural de decir «estas tres consultas son un solo trabajo», y `invokeAll` con un `Semaphore` habrá sido tu forma de llegar hasta ahí. Mientras tanto, lo que acabas de aprender funciona en Java 21, 25 y las que siguen.

### 9.8 Las pruebas del `revisor` concurrente

El `revisor` concurrente trae tres pruebas nuevas, en la clase `RevisionConcurrenteTest`. Cada una prueba una de las promesas de 9.3 y 9.4, y ninguna depende del reloj más de lo necesario. Esto es lo que verifican: **el orden** (el servicio más lento, puesto primero en la lista, sigue siendo el primero del resultado); **el límite** (con límite 2 y ocho peticiones, el servidor de la prueba nunca ve más de dos a la vez, lo cuenta un `AtomicInteger` dentro del propio servidor); y **la validación** (un límite de 0 es una `IllegalArgumentException`). Fíjate que el servidor de la prueba se crea con `Executors.newVirtualThreadPerTaskExecutor()`: el servidor de `com.sun.net.httpserver`, si no se le da un ejecutor, atiende las peticiones **una por una** en un solo hilo, y la prueba del límite mediría 1 en vez de 2 sin que el semáforo tuviera nada que ver.

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

Una prueba que nunca has visto fallar no demuestra nada, así que haz el experimento. En `Revision.java`, cambia `new Semaphore(limite)` por `new Semaphore(Integer.MAX_VALUE)` (un semáforo con tantos permisos que nunca frena) y corre las pruebas. Esto es lo que dice el informe de *surefire*:

```text
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.737 s <<< FAILURE! -- in mx.habil.revisor.RevisionConcurrenteTest
[ERROR] mx.habil.revisor.RevisionConcurrenteTest.conLimiteDosElServidorNuncaVeMasDeDosPeticionesALaVez -- Time elapsed: 0.220 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <2> but was: <8>
```

Se puso roja justo la prueba del límite, y con el número que debía: sin freno, las **ocho** peticiones llegaron a la vez. Regresa el semáforo a su valor, y el informe vuelve a verde. Para contar las pruebas sin fiarte del código de salida de Maven, súmalas del informe, de dos formas distintas (un conteo que decide algo se hace dos veces):

```text
$ grep -h -o ' tests="[0-9]*"' target/surefire-reports/TEST-*.xml | awk -F'"' '{s+=$2} END {print s}'
28
$ cat target/surefire-reports/TEST-*.xml | grep -c '<testcase '
28
```

Veinticinco de la lección 8 más las tres de hoy: 28, y ninguna saltada.

El `pom.xml` solo cambia en una cosa: la versión pasa a `0.9.0`. Fuera de los archivos que viste completos en esta lección (`Revision.java`, `Revisor.java`, `RevisionConcurrenteTest.java` y `servicios-20.json`), todo el proyecto queda exactamente como estaba en la lección 8, sin tocar un renglón.

## El error que vas a ver

El error de esta lección no es un mensaje, es una **ausencia de mensaje**: la carrera de 9.5.1. El programa termina sin excepciones y con un número equivocado (`esperado 1000000, contado 773685`). Si en algún momento un programa tuyo da resultados distintos en corridas iguales, sospecha de una carrera antes que de cualquier otra cosa, y busca qué dato escriben dos hilos a la vez. Cuatro mensajes de apoyo, para reconocerlos de una mirada:

- `java.util.concurrent.ExecutionException: java.lang.IllegalStateException: fallé a propósito`: una tarea falló en otro hilo. La causa está después de los dos puntos; mira `getCause()` (`fig09_03.java`).
- `AssertionFailedError: expected: <2> but was: <8>`: la prueba del límite te dice que el semáforo no frenó (9.8).
- Un `HashMap` compartido que termina con menos entradas de las que escribiste (`esperadas 10000, hay 7195`): el mapa se corrompió; usa `ConcurrentHashMap`.
- `error: StructuredTaskScope is a preview API and is disabled by default.`: usaste una función en vista previa sin la bandera (`fig09_08.java`).

## Lo que se hace mal

**Meter los hilos virtuales en un *pool*.** `newFixedThreadPool(10, Thread.ofVirtual().factory())` limita, pero escondido, con una cola y un tamaño que alguien tendrá que afinar. Un hilo virtual por tarea, y el límite con un `Semaphore` (9.4.2).

**Soltar el permiso fuera de un `finally`.** Una excepción entre `acquire()` y `release()` pierde el permiso. Tras unos cuantos fallos todas las tareas esperan para siempre, y el programa se cuelga sin explicación (9.4.1).

**Tragarse la `InterruptedException`.** Un `catch (InterruptedException e) {}` vacío esconde la señal de que alguien quiere que la tarea pare. Si no puedes propagarla, restaura la bandera con `Thread.currentThread().interrupt()` (lección 5).

**Compartir una colección que no es concurrente.** `ArrayList`, `HashMap` y `HashSet` no están hechas para escribirse desde varios hilos. Usa las de `java.util.concurrent`, o mejor, no compartas (9.5).

**Usar hilos virtuales para trabajo de procesador.** No dan velocidad, dan escala. Una tarea de cálculo largo ocupa a su portador todo el rato y no cede el turno; para eso, lo que cuenta es el número de núcleos (9.1.2).

**Lanzar sin límite contra un servicio ajeno.** Que los hilos sean baratos no quiere decir que el destino aguante. El límite se pone pensando en el servicio que recibe las peticiones, no en la JVM (9.4.1).

**Guardar contexto en un `ThreadLocal` mutable y olvidarse de limpiarlo.** Para lo que dura un bloque, `ScopedValue` (9.6).

**Probar concurrencia con `Thread.sleep` para «dar tiempo».** Una prueba que espera un tiempo fijo y asume que ya pasó lo que esperaba se rompe en una máquina lenta. Espera lo que de verdad hay que esperar (`join`, `invokeAll`, `close`), como hacen las pruebas de 9.8.

## Ejercicios

### Ejercicio 1 — Mide los tres tiempos

Con el laboratorio encendido y `servicios-20.json`, mide el `revisor` con `LIMITE_POR_OMISION` en 1, en 10 y en 20. Antes de correr, escribe tu predicción de cada una y explica con tus palabras por qué el límite de 10 tarda el doble que el de 20. Anota los tres tiempos reales en tu bitácora, en la línea de la lección 9.

### Ejercicio 2 — Predice el 3

Sin correrlo todavía: ¿cuánto debe tardar el `revisor` con límite 3 sobre esos mismos veinte servicios? Escribe la cuenta (cuántas tandas, cuánto dura cada una). Después mídelo, resta lo que cuesta arrancar la JVM, y compara. Si tu predicción falló por más de unas décimas, busca qué dejaste fuera.

### Ejercicio 3 — Arregla las dos carreras

Toma `Carrera.java` y `MapaCompartido.java` de 9.5.1 y corrígelos, cada uno con una herramienta distinta de 9.5.2. Debes lograr que el contador dé exactamente 1,000,000 y que el mapa tenga exactamente 10,000 entradas, **en cada corrida**. Corre tu versión tres veces para ver que no cambia.

### Ejercicio 4 — Un permiso que se pierde, y la prueba que lo atrapa

Las tres pruebas de 9.8 no detectan un `release()` mal puesto. Escribe una cuarta que sí: dos servicios, límite 1, y el primero con una dirección que haga fallar a `revisar` con una excepción que **no** es de red (por ejemplo `ftp://127.0.0.1/x`, que `HttpRequest` rechaza al construir la petición). Antes de escribirla, predice qué pasaría si `semaforo.release()` estuviera fuera del `finally`. Después sácalo del `finally`, mira la prueba en rojo, y restáuralo.

## Soluciones

### Solución 1

Mis tiempos fueron 10.4 s, 1.27 s y 0.76 s (ver la tabla de 9.4.3). Las predicciones correctas eran 10, 1 y 0.5 segundos más unos 0.2 de arranque. Con límite 10 hay dos tandas de medio segundo, con límite 20 una; el tiempo es *tandas × espera*, no *servicios × espera*. Si tu cifra para el límite 20 es mucho mayor que 0.7 s, mira si el laboratorio estaba atendiendo otra cosa.

### Solución 2

Veinte servicios entre tres permisos son siete tandas (seis llenas y una de dos): 7 × 0.5 = 3.5 s, más unos 0.2 s de arranque, o sea 3.7 s. Medí 3.808 s y 3.799 s. La décima que sobra son costos de operación (abrir conexiones, repartir hilos) que no medí por separado; lo importante es que la cuenta de tandas acertó. Fíjate en la última tanda: aunque solo tenga dos servicios, espera el medio segundo completo, porque una tanda dura lo que dura su servicio más lento, no lo que dura cada uno de sus miembros.

### Solución 3

Para el contador, un `LongAdder` (también sirve `AtomicLong`); para el mapa, un `ConcurrentHashMap`. Los dos cambios caben en un programa:

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

Las tres corridas dieron:

```text
esperado 1000000, contado 1000000
esperadas 10000 entradas, hay 10000
```

La lección es que el arreglo no es «poner más cuidado»: es cambiar el tipo del dato compartido por uno hecho para compartirse.

### Solución 4

La predicción: con límite 1, el primer servicio entra, `revisar` lanza la excepción, y como el `release()` quedó fuera del `finally`, **nunca se ejecuta**: el único permiso se perdió. El segundo servicio se queda esperando en `acquire()` para siempre, `invokeAll` nunca regresa y el programa se cuelga. La prueba, con un tope de tiempo de JUnit para que el cuelgue se convierta en un fallo con mensaje:

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

(Necesita `import org.junit.jupiter.api.Timeout;`.) Con el `finally` en su lugar, la prueba pasa: el primer servicio falla y suelta su permiso, el segundo se revisa, y `revisarTodos` lanza la `IllegalStateException` que ya conoces. Con el `release()` fuera del `finally`, el informe dice:

```text
[ERROR] mx.habil.revisor.RevisionConcurrenteTest.unErrorDelProgramaNoDejaPermisosPerdidos -- Time elapsed: 5.013 s <<< ERROR!
java.util.concurrent.TimeoutException: unErrorDelProgramaNoDejaPermisosPerdidos() timed out after 5 seconds
```

Sin el `@Timeout`, esta misma prueba **no fallaría: se colgaría**, y un cuelgue en integración continua cuesta mucho más que un fallo. Cuando pruebes concurrencia, ponle tope de tiempo a todo lo que podría esperar para siempre. Restaura el `finally` y la prueba vuelve a verde (con las cuatro pruebas de esta clase, el total sería 29; la figura de la lección se queda con las tres originales).

## Cómo sé que lo logré

- [ ] `java fig09_01.java` imprime `virtual=true, demonio=true` para el hilo virtual, y puedes explicar por qué un hilo virtual que no se espera se corta cuando `main` termina.
- [ ] `java fig09_02.java` imprime `true` en sus dos renglones, y sabes calcular a mano por qué un *pool* de 50 tarda cuatro segundos con mil tareas de 200 ms.
- [ ] En `fig09_03.java` los tres resultados salen en el orden de la lista y no en el de terminación, y sabes decir qué es la causa de una `ExecutionException`.
- [ ] `java fig09_04.java` reporta que el máximo simultáneo es 2, y sabes por qué el `release()` va en un `finally`.
- [ ] Corriste `Carrera.java` varias veces y viste que el número cambia y nunca llega a 1,000,000.
- [ ] `java fig09_06.java` imprime `{ARRIBA=10000}` en cada corrida.
- [ ] `mvn -B verify` termina en verde, y la suma de pruebas del informe, contada de dos formas, es 28.
- [ ] Con el laboratorio encendido, `servicios-20.json` tarda unos 10 s con límite 1, cerca de 1 s con límite 10 y cerca de 0.5 s con límite 20 (más lo que cuesta arrancar la JVM), y los anotaste en la bitácora.
- [ ] Con el semáforo sin freno, la prueba del límite se pone roja con `expected: <2> but was: <8>`.

## Resumen

Responde sin mirar la lección:

1. ¿Qué es un hilo portador, y qué hace la JVM cuando un hilo virtual llega a una operación que bloquea?
2. ¿Qué diferencia hay entre dar «escala» y dar «velocidad»? ¿Para qué clase de tarea sirven los hilos virtuales y para cuál no?
3. ¿Por qué un hilo virtual por tarea y no un *pool*? ¿Qué se usa para limitar cuántas tareas avanzan a la vez?
4. ¿En qué orden devuelve los `Future` el método `invokeAll`, y por qué le importa eso al reporte?
5. ¿Qué contiene una `ExecutionException` y por qué hay que mirar su causa? ¿Qué decide el `revisor` hacer con ella y por qué?
6. ¿Por qué el `release()` de un semáforo va en un `finally`? ¿Qué pasa si no?
7. ¿Por qué `contador++` pierde sumas cuando lo ejecutan varios hilos? ¿Qué tres maneras de arreglarlo conoces y cuál es la preferida?
8. ¿Por qué el `revisor` no tiene condiciones de carrera aunque corre diez tareas al mismo tiempo?
9. ¿Qué tiene `ScopedValue` que no tenga `ThreadLocal`, y por qué un hilo creado a mano dentro del bloque no ve el valor?
10. ¿Por qué la concurrencia estructurada se queda como «lo que viene» en este curso?

**Para comparar con otros lenguajes.** Si vienes de Go, la tarea con un hilo virtual por servicio es el equivalente a lanzar una *goroutine* por servicio y esperar con un [`sync.WaitGroup`](https://pkg.go.dev/sync#WaitGroup); la diferencia es que en Java los resultados vuelven en `Future` y en orden, y no por un canal. Si vienes de TypeScript, [`Promise.all`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all) es la comparación natural, con una diferencia que importa: `Promise.all` rechaza en cuanto una promesa falla, mientras `invokeAll` espera siempre a todas. Y en Python, [`asyncio.TaskGroup`](https://docs.python.org/3/library/asyncio-task.html#task-groups) cancela las demás tareas si una falla, que es justo lo que promete la concurrencia estructurada de 9.7. Los cursos de [Go](https://www.habil.mx/es/cursos/go/), [TypeScript](https://www.habil.mx/es/cursos/typescript/) y Python resuelven este mismo `revisor` con sus herramientas. Y si te interesa cuándo conviene consultar en línea, como hace el `revisor`, y cuándo conviene una cola o un evento, el artículo [estilos de integración: en línea, cola o evento](https://www.habil.mx/es/blog/estilos-integracion-en-linea-cola-evento/) lo discute con sus costos.

## Para leer más

- [Oracle, «Virtual Threads» (guía del JDK 25)](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html): qué son, para qué sirven, por qué no se agrupan y por qué un semáforo limita mejor que un *pool*. En inglés. Consultado el 7 de octubre de 2026.
- [JEP 444, «Virtual Threads»](https://openjdk.org/jeps/444) y [JEP 491, «Synchronize Virtual Threads without Pinning»](https://openjdk.org/jeps/491): la especificación de los hilos virtuales y la corrección del *pinning* en Java 24. Consultado el 7 de octubre de 2026.
- [`ExecutorService`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html) y [`ConcurrentHashMap`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) en la API de Java 25: el contrato exacto de `invokeAll`, `close` y `merge`. Consultado el 7 de octubre de 2026.
- [JEP 506, «Scoped Values»](https://openjdk.org/jeps/506) y [JEP 505, «Structured Concurrency (Fifth Preview)»](https://openjdk.org/jeps/505): los valores con ámbito (final en 25) y la concurrencia estructurada (todavía en vista previa). Consultado el 7 de octubre de 2026.

---

**Anterior:** [Lección 8 — HTTP y JSON](08-http-y-json.md) ·
**Siguiente:** [Lección 10 — El programa terminado](10-el-programa-terminado.md)
