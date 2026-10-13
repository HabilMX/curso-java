# Lesson 9 — Concurrency: checking everything at once

**Time:** 2 × 90 min (two sessions; the break is between sections 9.4 and 9.5)

**What you build:** the concurrent `revisor`: it queries all the services at the same time, with a limit that you set, and delivers the report in the same order as always

**What you learn:** platform threads and virtual threads; one virtual thread per task and never in a *pool*; `ExecutorService` as a resource that gets closed; `invokeAll`, stable ordering and `ExecutionException`; `Semaphore` to limit; a race condition provoked and measured; `ConcurrentHashMap` and the atomic types; `ScopedValue`; and structured concurrency as “what is coming”

**Where you are coming from.** You bring the project from [Lesson 8](08-http-y-json.md): a `revisor` in Maven, with `HttpClient`, Jackson, the validated configuration in `servicios.json` and 25 tests passing. It makes one real request per service, measures how long it takes, and does not crash when a service does not answer. But it makes them **one after another**: with the four canonical services it takes the sum of their times, about 2.5 seconds, and with twenty services of half a second each it would take ten. That slowness is not the program's, it is its shape: the `for` in `revisarTodos` waits for the answer from `catalogo` before asking `inventario`, even though neither needs the other. Today you take that away. You work in your folder `~/curso-java/revisor/`.

**What this lesson does not do.** It does not add command-line options: the concurrency limit is a constant until [Lesson 10](10-el-programa-terminado.md), where it becomes `--concurrencia`. Nor does it change how a service is checked: `revisar` stays as it is, with its timeouts and its three reasons for being down. The only thing that changes is **who calls `revisar` and how many times at once**.

**About the runs.** Figures 9.1 to 9.8 are compact source files and are run with `java figNN_MM.java` from the lesson folder; they do not need the lab. Figure 9.9 is the project, and it does need it: in another terminal, from `~/curso-java/`, leave `java Laboratorio.java` running (it listens on port 8089; port 8088 is left empty on purpose, it is the one for `reportes`). All the outputs were obtained with Temurin 25.0.4 on a 10-core machine. **The times you see in this lesson depend on your machine**: what repeats on every machine is the proportions, not the milliseconds. Figures that change from one run to the next (such as the result of a race) are shown as a fragment, with a real run, and not as verified output.

## By the end you will be able to

- Explain what a virtual thread is, how it differs from a platform thread, and why a program that waits a lot (like the `revisor`) takes advantage of them.
- Launch one task per service with `Executors.newVirtualThreadPerTaskExecutor()` inside a `try-with-resources`, and say why a *pool* of virtual threads is not used.
- Use `invokeAll` to wait for all the tasks and get their results back in input order, and read an `ExecutionException` down to its cause.
- Limit how many tasks advance at once with a `Semaphore`, always releasing it in a `finally`.
- Provoke a race condition, measure how much is lost, and fix it with `ConcurrentHashMap.merge` or with an atomic counter.
- Explain why the `revisor` has no races (it shares no mutable state), and what `ScopedValue` and structured concurrency are for.
- Use the clock to measure the effect of the limit on twenty services and explain why a limit of 10 takes twice as long as one of 20.

## The why before the how

Imagine a kitchen with a single cook preparing four dishes. He puts the first one in the oven and stands there watching it for twenty minutes until it is done; only then does he start the second. Nobody would say that cook is slow with his hands. He is slow **because he waits without doing anything else**. The `revisor` from lesson 8 is that cook: when it asks `inventario`, which takes 2.3 seconds to answer, the program stands still for 2.3 seconds, with the processor almost asleep, while `pagos` and `catalogo` could be answering at the same time.

The solution is also from the kitchen: put several dishes in the oven at once. In programming this is called **concurrency**: several tasks in progress at the same time, so that the time one spends waiting is used by another. (It is not the same as **parallelism**, which is having several processors literally working at once. Concurrency is a way of organizing the program; parallelism is something the hardware may or may not offer. In this lesson you will see both, but the benefit for the `revisor` comes mostly from the first.)

Doing this in Java has always had an obstacle, and this lesson exists because of the way Java removed it. For more than twenty years, the unit of concurrency was the **operating-system thread**: a thread is an independent line of execution inside the program, and every Java thread was a system thread, with its own stack and its own operating-system resources, and a real cost. (What it did not have of its own was the heap: objects live there and all threads share them, which is exactly where the races of 9.5 come from.) Creating ten thousand of them for ten thousand requests was not viable, so the standard practice was to have a few “staff” threads (a *pool*) and take turns with the tasks, with queues, limits and a lot of code so that nobody was left waiting longer than necessary. Since Java 21, the platform offers something else: **virtual threads**, so cheap that every task can have its own, and with them the code goes back to being what it was in the one-cook kitchen —one line after another, each step waiting for the previous one—, only now there are many cheap cooks.

This lesson has two sessions. The **first** (9.1 to 9.4) teaches virtual threads, `invokeAll` and the semaphore, and ends with a measurement: the `revisor` checking twenty services at once. The **second** (9.5 to 9.8) teaches what you need to know so that concurrency does not betray you: shared state, the race condition that makes it dangerous, scoped values, and what is coming in the platform. It is the lesson that changes what you know about Java the most, and the one that will serve you the most: every modern server in Java is written with these ideas.

## The concepts

### 9.1 Two kinds of thread

#### 9.1.1 What a thread is and what it costs

A **thread** is a sequence of instructions that advances on its own. Your program always has at least one: the one that runs `main`. With more threads, several sequences advance “at once”: the operating system divides the processors' time among them, and when one blocks —waiting for a response from the network, for example— it gives the turn to another.

Up to Java 20 every `Thread` was exactly that: **an operating-system thread**, with its own call stack and its own entry in the system tables. These are now called **platform threads**. They are powerful and expensive: the system imposes a cap on the number of threads, each one reserves memory for its stack, and switching from one to another costs. That is why a server program could not afford one thread per request when there were thousands of requests, and the technique was to share them with a *pool*.

A **virtual thread** is something else, even though it is used the same way. It is also a `Thread` —the same type, the same methods, `join`, `interrupt`—, but it is managed by the Java virtual machine instead of the operating system. A virtual thread is not tied to a system thread: the JVM has a small group of platform threads, called **carrier threads**, and “mounts” onto them the virtual threads that have something to do. When a virtual thread reaches an operation that blocks —reading from a socket, sleeping, waiting for a semaphore—, the JVM **unmounts** it: it saves its state in heap memory, frees the carrier and mounts another virtual thread that can advance. When the response arrives, the virtual thread mounts again on some carrier. It is the same idea as the cook who does not stand watching the oven, done by the platform instead of by you. The [official documentation on virtual threads](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html) describes it in these terms, and the specification of the change is [JEP 444](https://openjdk.org/jeps/444), final since Java 21.

Figure 9.1 puts them side by side. It asks for a platform thread and a virtual one with the same thread builder (`Thread.ofPlatform()` and `Thread.ofVirtual()`), gives them names, starts them, and each one says what it is:

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

Figures in this lesson carry no `import`: a compact source file automatically imports everything that `java.base` exports, which includes `java.util.concurrent`. The `join()` method makes `main` wait for the thread to finish; without it, the program could end before the thread prints. This is what it prints:

```text
$ java fig09_01.java
de-plataforma: virtual=false, demonio=false
virtual-1: virtual=true, demonio=true
```

Three facts in two lines. The platform thread is `virtual=false`; the other one, `virtual=true`. And the virtual one is a **daemon** (`demonio=true`, where `demonio` is “daemon”): a daemon thread does not keep the JVM alive, so if `main` finishes and only virtual threads are left, the program ends without waiting for them. It is a difference that bites: if you launch a virtual thread “to work in the background” and `main` finishes, you cut it off. (That is why the executor in 9.2 closes by waiting for its tasks.)

You can see the carrier mechanics in any virtual thread. Printing `Thread.currentThread()` from one gives something like this:

```text
VirtualThread[#26]/runnable@ForkJoinPool-1-worker-1
```

After the at sign comes the carrier thread it was running on at that moment. When I launched twenty thousand virtual threads and noted their carrier, exactly ten names appeared on my machine, from `worker-1` to `worker-10`: as many as it has cores. Twenty thousand “Java” threads spread over ten “system” threads.

#### 9.1.2 What you gain: scale, not speed

Figure 9.2 puts the difference in numbers, with a task that does nothing but wait 200 milliseconds (a service that is slow to answer). It launches a thousand of them twice: once in a classic *pool* of 50 platform threads and once with one virtual thread per task. It measures each one with the system clock (`System.nanoTime()`):

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

A couple of reading details before you run it. `try (ejecutor) { ... }` uses a variable that already exists as the resource of the `try-with-resources` (Java allows it since version 9, as long as the variable is not reassigned); it is the same idea as in lesson 5, and on leaving the block `close()` is called. `this::esperar` is a reference to a method of this same class, which Java turns into the task. And the last two lines print a boolean, not the milliseconds, so that the output is always the same:

```text
$ java fig09_02.java
pool de 50 hilos de plataforma, más de 3 s: true
un hilo virtual por tarea, menos de 1 s: true
```

You can see the real milliseconds if you print the variables `conPool` and `conVirtuales`. In three consecutive runs I got 4,152, 4,148 and 4,161 ms for the *pool*, and 221, 214 and 206 ms for the virtual threads. The arithmetic explains the first: a thousand tasks divided among 50 threads is 20 rounds, and 20 rounds of 200 ms is 4 seconds. With one virtual thread per task there are no rounds: all thousand wait together, and the total time is that of a single wait, plus the cost of starting. In another measurement, a hundred thousand virtual threads sleeping one second each finished in 1.4 seconds.

Here it is worth stopping a misunderstanding that ruins more than one design. Oracle's documentation says it bluntly: virtual threads are **not faster threads**; they do not run code any quicker than a platform thread. They give **scale** (more tasks in progress at once), not **speed** (less time per task). And they are for tasks that spend almost all their time **waiting**, not for processor work: if each task were a long factorization, ten thousand virtual threads would not finish sooner than ten, because the cores are the same. The `revisor` is the ideal case: each task sends a request and waits.

One more fact, which saves you worries you will read about in older books: with Java 21 there was a problem called *pinning* (a virtual thread that blocked inside a `synchronized` block stayed stuck to its carrier and did not release it). Since Java 24 it [no longer happens](https://openjdk.org/jeps/491) in those blocks; the advice “avoid `synchronized` with virtual threads” that still circulates is from another era.

### 9.2 One virtual thread per task

#### 9.2.1 The executor that closes

Creating threads by hand with `Thread.ofVirtual().start(...)` is good for understanding, not for organizing work. To organize it you use an **executor**, an object to which you hand tasks and which takes care of running them. The factory we are interested in is a single line:

```java
try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
  // aquí se mandan tareas con ejecutor.submit(...) o ejecutor.invokeAll(...)
}
```

Its name says it all: *virtual thread per task executor*. Every task you send it gets a new virtual thread, which dies when the task finishes. There is no queue, no *pool* size, nothing to configure. And since `ExecutorService` has been `AutoCloseable` since Java 19, the `try-with-resources` does the important part: on leaving the block it calls `close()`, which **waits for all the tasks to finish** before continuing. It is the guarantee the old executors did not have, where forgetting `shutdown()` left threads alive and a program that never ended. Here the `try` block is a fence: what you start inside finishes inside.

The rule of this section fits in one sentence, and it is the one the course syllabus puts in bold: **one virtual thread per task, never in a *pool***. A *pool* exists to share out a scarce resource —platform threads— among many tasks. Virtual threads are not scarce: you can have millions. If you put a *pool* on top of them, you create an artificial scarcity again, with the same queues and the same caps, and you lose precisely what made them useful. The official guide sums it up like this: virtual threads should not represent a shared resource but **a task**. If what you want is to limit how many tasks advance at once, there is a tool made for that, and you see it in 9.4.

#### 9.2.2 Why a blocking `send` is fine

Look again at how the `revisor` has checked a service since lesson 8: `cliente.send(peticion, BodyHandlers.discarding())` **blocks**: the thread that calls it stays waiting until the response arrives or the timeout runs out. On a platform thread that was an expensive luxury; that is why concurrent programming in Java spent years looking for ways not to block (*callbacks*, `CompletableFuture`, reactive libraries), with code that reads upside down. On a virtual thread, waiting on input or output like this **costs very little**: the JVM unmounts the thread while it waits and uses the carrier for another. Little is not nothing: there are cases where the virtual thread stays **pinned** to its carrier and occupies it while it waits (for example, during a call to native code), and what is on the other side, connections, services, memory, is still limited: a cheap thread does not make the server you query cheap. That is why `revisar` does not change a single line in this lesson. It is still the linear method from lesson 8: build the request, send it, wait, classify. The concurrency is not inside `revisar`; it is in how it is called.

Two things about that call must be kept in mind. First, the `HttpClient` **is shared**: the class documentation says an instance is immutable and good for sending many requests, so the `revisor` creates only one and all the tasks use it; creating one client per request would throw away its reusable connections. Second, the client is also `AutoCloseable`, and the closing order matters: in a `try-with-resources` with two resources they are closed **in the reverse order of their declaration**. That is why the client is declared first and the executor second: on exit, the executor is closed (it waits for all the tasks) and *then* the client. The other way around, you would close the client with requests in flight.

### 9.3 `invokeAll` and stable ordering

#### 9.3.1 `Callable`, `Future` and waiting for all

`submit` accepts two kinds of task. A `Runnable` returns nothing and cannot throw a checked exception. A **`Callable<T>`** returns a value of type `T` and can throw any exception: it is what `revisar` needs, which returns a `Resultado` and can throw `InterruptedException` (lesson 5 explained why that one is never swallowed). What you receive when you send a task is a **`Future<T>`**: a promise of a result. `futuro.get()` waits —if needed— for the task to finish and hands over its value.

The method that joins the two things is `invokeAll`: it receives a collection of `Callable`, runs them all, **waits for all of them to finish** and returns a `List<Future<T>>`. The important part is in the documentation and is what makes it suitable for the `revisor`: the list it returns has **the same order as the one you gave it**, no matter which finished first. Figure 9.3 checks it with three tasks that take 300, 50 and 150 ms; the fastest is the second in the list, and it still comes out second:

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

Follow the figure in parts. `tarea` sleeps and returns its name. The list of three `Callable` is written with lambdas, and `invokeAll` runs them all with virtual threads. The first `for` walks the `Future`s in order. Then a second list is built where one task **throws an exception**, and you see something important: `invokeAll` returns normally, the task shows up as finished, and the failure does not blow up until you ask for its value with `get()`. This is the run:

```text
$ java fig09_03.java
en el orden de la lista: lenta
en el orden de la lista: rapida
en el orden de la lista: mediana
invokeAll regresó; la tarea 2 terminó: true
get() lanza ExecutionException
la causa es java.lang.IllegalStateException: fallé a propósito
```

The first three lines come out in list order (`lenta`, `rapida`, `mediana`; “slow”, “fast”, “medium”), not in the order in which the tasks finished (which was `rapida`, `mediana`, `lenta`). For the `revisor` this is what guarantees point 3 of “How you know you are done”: the report comes out **always in the same order**, that of the configuration, even if `inventario` takes two seconds and `pagos` eighty-seven milliseconds. If you collected the results as they arrive, the order would change on every run and comparing two reports would be a pain.

#### 9.3.2 `ExecutionException`: the exception from another thread

When a task throws an exception in its thread, that exception cannot go up the stack of whoever sent it —they are different stacks—. Java stores it inside the `Future` and, when you call `get()`, delivers it **wrapped** in an `ExecutionException`. The last two lines of the figure show it: the type you catch is `ExecutionException`, but what matters is its **cause**, `getCause()`, which is the original exception (`IllegalStateException: fallé a propósito`, “I failed on purpose”). Whoever catches it and only prints the wrapper's message loses precisely the information they need. It is the same principle as lesson 5, the one about custom exceptions with their cause: a wrapped exception gets unwrapped, not discarded.

This forces you to decide what to do with it, and the `revisor`'s decision is deliberate. There are two kinds of problem. A **service that is down** is an expected result: `revisar` catches it and turns it into a `Resultado` with status `CAIDO` and its reason, and never lets an `IOException` escape. That is why no task fails because of the network, and the report comes out complete even if half the world is down. A **program error** (a `NullPointerException`, a logic error) is another thing: if a task fails like that, something is wrong in the code, and hiding it behind a “service down” would be lying. The `revisor` rethrows it as an `IllegalStateException` with the original cause attached. In one sentence: **a service that is down does not bring down the report; a program error is not hidden**.

One last detail of the contract: if the thread waiting in `invokeAll` is interrupted, the method **cancels the tasks that have not finished** and throws `InterruptedException`. That is why the signature of `revisarTodos` declares `throws InterruptedException` and does not swallow it.

### 9.4 Limiting with `Semaphore`

#### 9.4.1 Why limit if threads are cheap

If virtual threads are almost free, why limit? Because the scarce resource is no longer the thread; it is **what is on the other side**. Twenty services queried at once is a reasonable courtesy. Twenty thousand services queried at once —or twenty simultaneous requests against the same server— can bring down a fragile service, exhaust the system's ports or set off a provider's alarm. The limit protects the **destination**, not the JVM. And that decision should not depend on the number of threads a *pool* happens to have.

Java has had a tool designed for this for twenty years: the **`Semaphore`**. A semaphore holds a number of **permits**. Whoever wants to advance calls `acquire()` (take a permit); if there is none, it **waits** until another releases one with `release()`. With `new Semaphore(2)`, at most two tasks are inside the protected zone; the rest queue up. Figure 9.4 launches six tasks of 100 ms with a semaphore of two permits, and records the maximum number of simultaneous tasks with an `AtomicInteger` (an integer that can be modified from several threads without losing counts; it comes back in 9.5):

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

Notice the shape of the task: `acquire()`, then a `try` with the work, and `release()` in the **`finally`**. This is not style: it is a condition of correctness. If the work throws an exception and the `release()` is not in a `finally`, that permit is lost forever; after enough failures, all the permits are lost and the remaining tasks wait forever, a hung program with no error in sight. The run:

```text
$ java fig09_04.java
tareas lanzadas: 6, máximo al mismo tiempo: 2
```

Six tasks launched, never more than two at the same time. And notice what the semaphore did **not** do: it did not limit the threads. All six tasks have their virtual thread from the start; four are asleep in `acquire()` and cost nothing. Oracle's official guide recommends exactly this —a semaphore to limit— and explains that its effect is similar to that of a fixed-size *pool*, because in both cases the surplus tasks queue up, but without reserving threads for it.

#### 9.4.2 Why not a *pool* of virtual threads

The temptation is to solve the limit with what you already know: `Executors.newFixedThreadPool(2, fabrica)`, where the factory produces virtual threads. It works, and that is the trap. Figure 9.5 measures the maximum number of simultaneous tasks both ways:

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

The fixed *pool* limits to 2, the per-task executor lets all 6 through. There are two problems with using the first one to limit. One is clarity: the limit ended up hidden in a numeric argument of a factory, and nobody reading the code will understand that “2” means “simultaneous requests allowed”. The other is engineering: you have just reintroduced a queue, a size to tune and a thread to reserve, to obtain what `Semaphore(2)` gives in one line and with its purpose written in the name. The official guide is blunt: virtual threads should **never be pooled**. Less concurrency, more code, no advantage.

#### 9.4.3 The project step: concurrent `revisarTodos`

You have the pieces. This is how `Revision.java` looks; what is new is the constant `LIMITE_POR_OMISION` and the method `revisarTodos`, and everything else (`revisar`, `nuevoCliente`) is the one from lesson 8. Read `revisarTodos` in order: first it validates the limit; it creates the semaphore; it opens the client and the executor (in that order, as you saw in 9.2.2); it builds one task per service, which takes a permit, checks, and releases it in a `finally`; it sends them all with `invokeAll`; and it collects the results **in order**, unwrapping any `ExecutionException`:

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

Three decisions in this function are worth a close look. The first: **the semaphore is taken inside the task, not before launching it**. All the tasks exist from the start, and each one waits for its turn by itself; the `invokeAll` stays simple. The second: the time that `revisar` measures with `System.nanoTime()` starts **after** taking the permit, so the TIEMPO (“time”) column keeps saying how long the service took and not how long it waited in line. The third: `limite < 1` is rejected right away with an `IllegalArgumentException`, because a semaphore with zero permits would leave all the tasks waiting forever.

The constant `LIMITE_POR_OMISION` is 10, and `Revisor` uses it for now. `Revisor` changes in two things: the first argument, if there is one, is the configuration file (so you can check another list without editing anything), and it calls `revisarTodos` with the default limit:

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

The file `servicios-20.json` has twenty services, `servicio01` to `servicio20`, all pointing at the lab's `/espera/500` path, which answers 200 after half a second. This is the complete file:

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

### Measuring: the clock decides

With the lab running, compile (`mvn -B verify`) and run the `revisor` against those twenty services, measuring how long the whole program takes. On Linux, in a `bash` terminal, the clean way to measure is the `time` reserved word with a short format:

```text
$ TIMEFORMAT='%R s'
$ time java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor servicios-20.json > /dev/null
1.258 s
```

Change the constant `LIMITE_POR_OMISION` in `Revision.java` (for now there is no other way; in Lesson 10 it will be `--concurrencia`), recompile and repeat. These are my measurements, two runs per limit, always with the twenty 500 ms services:

| Limit | Rounds | What the arithmetic predicts | What I measured (both runs) |
|---|---|---|---|
| 1 (equivalent to **serial**) | 20 | 20 × 0.5 = 10 s | 10.413 s and 10.387 s |
| 3 | 7 | 7 × 0.5 = 3.5 s | 3.808 s and 3.799 s |
| 10 | 2 | 2 × 0.5 = 1 s | 1.258 s and 1.298 s |
| 20 | 1 | 1 × 0.5 = 0.5 s | 0.751 s and 0.779 s |

Two things to read the table without fooling yourself. **One:** what was measured always exceeds what was predicted by about 0.2 seconds, and that is the fixed cost of starting the JVM and the HTTP client. I checked it: a single service with zero wait takes 0.2 seconds in total. Subtract 0.2 from each measurement and the arithmetic holds to within a tenth or two. **Two:** the last column answers the syllabus question, “why a limit of 10 takes twice as long as one of 20”. With limit 10 and twenty services there are **two rounds**: the first ten take a permit and wait half a second; when they release, the other ten come in and wait another half second. With limit 20 they all come in at once. The total time is the number of rounds times how long each wait lasts, not the number of services.

With the four canonical services, on the other hand, the improvement is modest: they go from about 2.8 seconds in series to about 2.5. That is not a defect: the time is no longer the sum (2.5 s) but **that of the slowest** (`inventario`, 2.3 s), and since that one rules, there is little to gain. Concurrency helps when there are many similar waits; with one dominant wait, it hardly helps. It is important that you know this, because “it was made concurrent” does not mean “it was made fast”: you measure.

And with the four canonical services of `servicios.json`, from the project folder and with the lab running, a complete run looks like this:

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

The times in the TIEMPO column are from one run of mine; yours will be different, but ESTADO (“status”) and DETALLE (“detail”) never change.

*(The first session ends here.)*

### 9.5 Shared state: where concurrency bites

#### 9.5.1 A race condition, provoked and measured

Everything so far was easy because the tasks do not touch each other. As soon as two threads **write the same piece of data**, the problem that gives concurrency its bad name appears. It is best seen by provoking it. This program launches 10,000 tasks, and each one adds 1 a hundred times to a counter they share. If everything went well, the total would be exactly one million. Save it as `Carrera.java`:

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

It carries no figure marker on purpose: its result changes on every run, so it cannot be verified against a fixed output. I ran the program four times in a row and this is what it counted:

```text
esperado 1000000, contado 841360
esperado 1000000, contado 852985
esperado 1000000, contado 773685
esperado 1000000, contado 855739
```

There is not a single exception, there is no compiler warning, and the program “works”: it finishes, prints a number, exits with code 0. But it **lost between 144,000 and 226,000 additions**, and it does not lose the same amount twice. This is the trait that makes a race dangerous: it does not announce itself. A program with a race passes its tests 99 % of the time and fails in production, on a Tuesday, under load.

Why it happens is simpler than it seems. `contador++` looks like one operation, but it is three: **read** the value, **add one** to it and **write** the result. If two threads read 41 at the same time, both calculate 42 and both write 42: one addition was made instead of two, and an addition vanished. With ten carriers really working in parallel, that overlap happens hundreds of thousands of times. (There is also a second cause, that of **visibility**: without an instruction that orders it, one thread's memory may not see what another wrote. The [Java memory model](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html), in chapter 17 of the language specification, defines when a write is guaranteed to be seen: the rule is called *happens-before*.)

It is not a problem exclusive to numbers. With a shared collection it is worse. The same experiment with a `HashMap` —it stores 10,000 distinct keys, each one from its own task— should end with 10,000 entries. Save it as `MapaCompartido.java`:

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

My three runs ended with 8,339, 7,195 and 7,563 entries. Between 1,600 and 2,800 entries were missing. A `HashMap` is not designed to be written from several threads, and its internal structure gets corrupted: it can also throw exceptions and, in old versions of Java, get stuck in an infinite loop. That it sometimes “comes out right” is no comfort; it is what makes it hard to catch.

#### 9.5.2 Three ways to fix it, in order of preference

**The first, and the best: do not share.** If each task works with its own data and returns a result, there is nothing to protect. It is what the `revisor` does, and I explain it in 9.5.3.

**The second: types made to be shared.** The [`java.util.concurrent`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/package-summary.html) package brings types whose operations are atomic, that is, indivisible. `AtomicInteger` and `AtomicLong` are integers that can be added to from several threads without losing counts; `LongAdder`, designed for heavily contended counters, is faster when there are many threads. And `ConcurrentHashMap` is the map for concurrent writes. Its `merge` method is the one that fixes the typical case of counting by category: `mapa.merge("ARRIBA", 1, Integer::sum)` means “if the key does not exist, set it to 1; if it exists, replace it with the result of adding”; and according to the documentation, the whole invocation is done **atomically**. Figure 9.6 repeats the experiment of counting statuses with 10,000 tasks, now with `merge`, and also adds with an atomic counter:

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

Unlike the race, this result comes out **always** the same, however many times you run it: 10,000 in the map and 10,000 in the counter. That can be verified, and that is why this figure does carry a marker.

**The third: locks.** Java has `synchronized` and the locks in `java.util.concurrent.locks` to protect a piece of code so that only one thread at a time runs it. They work, and sometimes they are the only thing that does; but they are the easiest tool to misuse (deadlocks, forgotten locks, sections that are too long), and the course does not use them. With the two previous options they are almost never needed.

#### 9.5.3 Why the `revisor` has no races

Go over each piece of data the tasks of `revisarTodos` touch, one by one, because this is where what you learned in lesson 3 pays off. The `Servicio` that each task receives is a *record*: **immutable**, nobody changes it. The `HttpClient` is safe to share (the documentation guarantees it). The task does not write to any outside variable: it **returns** its `Resultado`, which is another immutable *record*. The only mutable object the tasks share is the `Semaphore`, which is made precisely for that. And the `resultados` list, which is indeed a normal `ArrayList` (not safe for several threads), is touched only by the thread that called `revisarTodos`, **after** `invokeAll` finished; the `java.util.concurrent` package itself documents that what a task did before finishing is guaranteed to be visible to whoever collects its result with `get()`.

The design rule that comes out of this is worth more than any tool: **communicate by returning values, not by sharing memory**. When a task needs to tell something to whoever launched it, let it return it. The immutability the course has been looking after since lesson 3 was not a style ornament: it is what makes a piece able to run on a hundred threads without thinking about them.

### 9.6 Scoped values: `ScopedValue`

There is a problem that concurrent tasks aggravate: **context**. Imagine each check carries a run identifier, `r-001`, and you want any method —the one that classifies, the one that builds the request, the one that writes a diagnostic message— to be able to read it. The obvious way out is to pass it as a parameter through every method in the chain, and it becomes noise nobody wants. The usual way out in Java was `ThreadLocal`, a variable that has a different value in each thread. But it has three known defects: it is **mutable** (anyone changes it, and nobody knows who), it lives **until someone cleans it up** (forgetting it is a memory leak), and copying it to child threads is **expensive**, which matters when there are millions of virtual threads.

`ScopedValue` exists for that, final since Java 25 ([JEP 506](https://openjdk.org/jeps/506)). A scoped value is **immutable** and exists **only for the duration of a block of code**: you bind it with `ScopedValue.where(CLAVE, valor).run(...)` (or `.call(...)` if the block returns something or throws exceptions), and any method called from inside can read it with `CLAVE.get()`. On leaving the block, the value disappears by itself. Figure 9.7 shows it, and also shows its limit:

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

The `anotar` method does not receive the identifier as a parameter and reads it anyway: that is the service it provides. The second line is the part that surprises: the virtual thread created **inside** the block by hand **does not see the value** (`ligado = false`, where `ligado` is “bound”). The value travels to the calls of the same thread, not to the threads you launch on your own; only the structured concurrency of 9.7 inherits it to its children. And the third line confirms that nothing is left outside the block.

The `revisor` of this course **does not use** `ScopedValue`: it has no context to pass. You will see it as soon as you write a server that handles requests from several users and need to know, at any point in the code, whom you are working for. It is here because it is what replaces `ThreadLocal` in the Java you will write from now on, and because it debuted in this version.

### 9.7 What is coming: structured concurrency

In 9.3 you saw that `invokeAll` waits for all the tasks, whatever happens. What is missing is a form many people want: “launch these tasks **as a unit**: if one fails, cancel the others; if the block ends, none stays alive”. That is called **structured concurrency**, and JDK 25 brings it... **in preview**. A preview is a complete feature that is still subject to change, and that is only activated with a flag. This one has changed shape in every version since 21; the fifth preview is [JEP 505](https://openjdk.org/jeps/505).

Figure 9.8 uses the API (`StructuredTaskScope.open()`, with `fork` to launch and `join` to wait). Compiled as is, it **does not run**:

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

Reading the message tells you everything: the class exists, but it is a preview API and it is turned off. With the flag that the message itself asks for, it does work:

```text
$ java --enable-preview --source 25 fig09_08.java
respuesta de catalogo / respuesta de pagos
```

**Do not use it in the `revisor` or in production**: the API can change between JDK versions, and a program that depends on it would have to be rewritten when it changes. That is why this course leaves it as a curiosity. What you do take away is the idea: when it stabilizes, it will be the natural way to say “these three queries are a single job”, and `invokeAll` with a `Semaphore` will have been your way of getting there. In the meantime, what you have just learned works in Java 21, 25 and the ones that follow.

### 9.8 The tests of the concurrent `revisor`

The concurrent `revisor` comes with three new tests, in the class `RevisionConcurrenteTest`. Each one tests one of the promises of 9.3 and 9.4, and none depends on the clock more than necessary. This is what they verify: **the order** (the slowest service, put first in the list, is still the first in the result); **the limit** (with limit 2 and eight requests, the test server never sees more than two at once, counted by an `AtomicInteger` inside the server itself); and **the validation** (a limit of 0 is an `IllegalArgumentException`). Notice that the test server is created with `Executors.newVirtualThreadPerTaskExecutor()`: the `com.sun.net.httpserver` server, if not given an executor, serves requests **one by one** on a single thread, and the limit test would measure 1 instead of 2 without the semaphore having anything to do with it.

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

A test you have never seen fail proves nothing, so do the experiment. In `Revision.java`, change `new Semaphore(limite)` to `new Semaphore(Integer.MAX_VALUE)` (a semaphore with so many permits that it never brakes) and run the tests. This is what the *surefire* report says:

```text
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.737 s <<< FAILURE! -- in mx.habil.revisor.RevisionConcurrenteTest
[ERROR] mx.habil.revisor.RevisionConcurrenteTest.conLimiteDosElServidorNuncaVeMasDeDosPeticionesALaVez -- Time elapsed: 0.220 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <2> but was: <8>
```

Precisely the limit test turned red, and with the number it should have: without a brake, **eight** requests arrived at once. Put the semaphore back to its value, and the report goes green again. To count the tests without trusting Maven's exit code, add them up from the report, in two different ways (a count that decides something is done twice):

```text
$ grep -h -o ' tests="[0-9]*"' target/surefire-reports/TEST-*.xml | awk -F'"' '{s+=$2} END {print s}'
28
$ cat target/surefire-reports/TEST-*.xml | grep -c '<testcase '
28
```

Twenty-five from lesson 8 plus today's three: 28, and none skipped.

The `pom.xml` changes in only one thing: the version becomes `0.9.0`. Apart from the files you saw complete in this lesson (`Revision.java`, `Revisor.java`, `RevisionConcurrenteTest.java` and `servicios-20.json`), the whole project stays exactly as it was in lesson 8, without touching a line.

## The error you will see

The error of this lesson is not a message, it is an **absence of a message**: the race of 9.5.1. The program finishes without exceptions and with a wrong number (`esperado 1000000, contado 773685`, “expected 1000000, counted 773685”). If at any moment a program of yours gives different results on identical runs, suspect a race before anything else, and look for which piece of data two threads write at the same time. Four supporting messages, to recognize them at a glance:

- `java.util.concurrent.ExecutionException: java.lang.IllegalStateException: fallé a propósito`: a task failed in another thread. The cause comes after the colon; look at `getCause()` (`fig09_03.java`).
- `AssertionFailedError: expected: <2> but was: <8>`: the limit test is telling you the semaphore did not brake (9.8).
- A shared `HashMap` that ends with fewer entries than you wrote (`esperadas 10000, hay 7195`, “expected 10000, there are 7195”): the map got corrupted; use `ConcurrentHashMap`.
- `error: StructuredTaskScope is a preview API and is disabled by default.`: you used a preview feature without the flag (`fig09_08.java`).

## What gets done wrong

**Putting virtual threads in a *pool*.** `newFixedThreadPool(10, Thread.ofVirtual().factory())` limits, but hidden, with a queue and a size that someone will have to tune. One virtual thread per task, and the limit with a `Semaphore` (9.4.2).

**Releasing the permit outside a `finally`.** An exception between `acquire()` and `release()` loses the permit. After a few failures all the tasks wait forever, and the program hangs with no explanation (9.4.1).

**Swallowing the `InterruptedException`.** An empty `catch (InterruptedException e) {}` hides the signal that someone wants the task to stop. If you cannot propagate it, restore the flag with `Thread.currentThread().interrupt()` (lesson 5).

**Sharing a collection that is not concurrent.** `ArrayList`, `HashMap` and `HashSet` are not made to be written from several threads. Use the ones in `java.util.concurrent`, or better, do not share (9.5).

**Using virtual threads for processor work.** They do not give speed, they give scale. A long calculation task occupies its carrier the whole time and does not yield the turn; for that, what counts is the number of cores (9.1.2).

**Launching without a limit against someone else's service.** That threads are cheap does not mean the destination can take it. The limit is set thinking about the service that receives the requests, not about the JVM (9.4.1).

**Keeping context in a mutable `ThreadLocal` and forgetting to clean it up.** For what lasts a block, `ScopedValue` (9.6).

**Testing concurrency with `Thread.sleep` to “give it time”.** A test that waits a fixed time and assumes that what it was waiting for has already happened breaks on a slow machine. Wait for what you really have to wait for (`join`, `invokeAll`, `close`), as the tests in 9.8 do.

## Exercises

### Exercise 1 — Measure the three times

With the lab running and `servicios-20.json`, measure the `revisor` with `LIMITE_POR_OMISION` at 1, at 10 and at 20. Before running, write down your prediction for each and explain in your own words why the limit of 10 takes twice as long as the one of 20. Note the three real times in your logbook, on the line for lesson 9.

### Exercise 2 — Predict the 3

Without running it yet: how long should the `revisor` take with limit 3 on those same twenty services? Write the calculation (how many rounds, how long each one lasts). Then measure it, subtract what starting the JVM costs, and compare. If your prediction missed by more than a few tenths, look for what you left out.

### Exercise 3 — Fix the two races

Take `Carrera.java` and `MapaCompartido.java` from 9.5.1 and fix them, each with a different tool from 9.5.2. You must make the counter give exactly 1,000,000 and the map have exactly 10,000 entries, **on every run**. Run your version three times to see that it does not change.

### Exercise 4 — A permit that gets lost, and the test that catches it

The three tests in 9.8 do not detect a misplaced `release()`. Write a fourth one that does: two services, limit 1, and the first with an address that makes `revisar` fail with an exception that is **not** a network one (for example `ftp://127.0.0.1/x`, which `HttpRequest` rejects when building the request). Before writing it, predict what would happen if `semaforo.release()` were outside the `finally`. Then take it out of the `finally`, see the test go red, and restore it.

## Solutions

### Solution 1

My times were 10.4 s, 1.27 s and 0.76 s (see the table in 9.4.3). The correct predictions were 10, 1 and 0.5 seconds plus about 0.2 of startup. With limit 10 there are two rounds of half a second, with limit 20 one; the time is *rounds × wait*, not *services × wait*. If your figure for limit 20 is much greater than 0.7 s, check whether the lab was busy with something else.

### Solution 2

Twenty services among three permits are seven rounds (six full and one of two): 7 × 0.5 = 3.5 s, plus about 0.2 s of startup, that is 3.7 s. I measured 3.808 s and 3.799 s. The extra tenth is operating costs (opening connections, distributing threads) that I did not measure separately; what matters is that the round count was right. Notice the last round: even though it has only two services, it waits the full half second, because a round lasts as long as its slowest service, not as long as each of its members.

### Solution 3

For the counter, a `LongAdder` (an `AtomicLong` also works); for the map, a `ConcurrentHashMap`. Both changes fit in one program:

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

The three runs gave:

```text
esperado 1000000, contado 1000000
esperadas 10000 entradas, hay 10000
```

The lesson is that the fix is not “be more careful”: it is changing the type of the shared data to one made to be shared.

### Solution 4

The prediction: with limit 1, the first service gets in, `revisar` throws the exception, and since the `release()` was left outside the `finally`, it **never runs**: the only permit was lost. The second service stays waiting in `acquire()` forever, `invokeAll` never returns and the program hangs. The test, with a JUnit time limit so that the hang turns into a failure with a message:

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

(It needs `import org.junit.jupiter.api.Timeout;`.) With the `finally` in place, the test passes: the first service fails and releases its permit, the second is checked, and `revisarTodos` throws the `IllegalStateException` you already know. With the `release()` outside the `finally`, the report says:

```text
[ERROR] mx.habil.revisor.RevisionConcurrenteTest.unErrorDelProgramaNoDejaPermisosPerdidos -- Time elapsed: 5.013 s <<< ERROR!
java.util.concurrent.TimeoutException: unErrorDelProgramaNoDejaPermisosPerdidos() timed out after 5 seconds
```

Without the `@Timeout`, this same test **would not fail: it would hang**, and a hang in continuous integration costs far more than a failure. When you test concurrency, put a timeout on everything that could wait forever. Restore the `finally` and the test goes green again (with the four tests of this class, the total would be 29; the lesson's figure stays with the three original ones).

## How I know I got it

- [ ] `java fig09_01.java` prints `virtual=true, demonio=true` for the virtual thread, and you can explain why a virtual thread that is not waited for gets cut off when `main` finishes.
- [ ] `java fig09_02.java` prints `true` on both of its lines, and you know how to calculate by hand why a *pool* of 50 takes four seconds with a thousand tasks of 200 ms.
- [ ] In `fig09_03.java` the three results come out in list order and not in finishing order, and you can say what the cause of an `ExecutionException` is.
- [ ] `java fig09_04.java` reports that the simultaneous maximum is 2, and you know why the `release()` goes in a `finally`.
- [ ] You ran `Carrera.java` several times and saw that the number changes and never reaches 1,000,000.
- [ ] `java fig09_06.java` prints `{ARRIBA=10000}` on every run.
- [ ] `mvn -B verify` ends green, and the sum of tests in the report, counted two ways, is 28.
- [ ] With the lab running, `servicios-20.json` takes about 10 s with limit 1, close to 1 s with limit 10 and close to 0.5 s with limit 20 (plus what starting the JVM costs), and you noted them in the logbook.
- [ ] With the semaphore unbraked, the limit test turns red with `expected: <2> but was: <8>`.

## Summary

Answer without looking at the lesson:

1. What is a carrier thread, and what does the JVM do when a virtual thread reaches an operation that blocks?
2. What is the difference between giving “scale” and giving “speed”? For what kind of task are virtual threads useful and for which are they not?
3. Why one virtual thread per task and not a *pool*? What is used to limit how many tasks advance at once?
4. In what order does the `invokeAll` method return the `Future`s, and why does that matter to the report?
5. What does an `ExecutionException` contain and why must you look at its cause? What does the `revisor` decide to do with it and why?
6. Why does a semaphore's `release()` go in a `finally`? What happens if it does not?
7. Why does `contador++` lose additions when several threads run it? What three ways of fixing it do you know and which is the preferred one?
8. Why does the `revisor` have no race conditions even though it runs ten tasks at the same time?
9. What does `ScopedValue` have that `ThreadLocal` does not, and why does a thread created by hand inside the block not see the value?
10. Why does structured concurrency remain as “what is coming” in this course?

**For comparison with other languages.** If you come from Go, the task with one virtual thread per service is the equivalent of launching a *goroutine* per service and waiting with a [`sync.WaitGroup`](https://pkg.go.dev/sync#WaitGroup); the difference is that in Java the results come back in a `Future` and in order, and not through a channel. If you come from TypeScript, [`Promise.all`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all) is the natural comparison, with a difference that matters: `Promise.all` rejects as soon as one promise fails, while `invokeAll` always waits for all of them. And in Python, [`asyncio.TaskGroup`](https://docs.python.org/3/library/asyncio-task.html#task-groups) cancels the other tasks if one fails, which is exactly what the structured concurrency of 9.7 promises. The [Go](https://www.habil.mx/en/courses/go/) and [TypeScript](https://www.habil.mx/en/courses/typescript/) courses and the Python one solve this same `revisor` with their own tools. And if you are interested in when it makes sense to query inline, as the `revisor` does, and when a queue or an event is better, the article [integration styles: inline, queue or event](https://www.habil.mx/en/blog/online-or-async-integration-styles/) discusses it with its costs.

## Further reading

- [Oracle, “Virtual Threads” (JDK 25 guide)](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html): what they are, what they are for, why they are not pooled and why a semaphore limits better than a *pool*. Consulted on October 7, 2026.
- [JEP 444, “Virtual Threads”](https://openjdk.org/jeps/444) and [JEP 491, “Synchronize Virtual Threads without Pinning”](https://openjdk.org/jeps/491): the specification of virtual threads and the fix of *pinning* in Java 24. Consulted on October 7, 2026.
- [`ExecutorService`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html) and [`ConcurrentHashMap`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) in the Java 25 API: the exact contract of `invokeAll`, `close` and `merge`. Consulted on October 7, 2026.
- [JEP 506, “Scoped Values”](https://openjdk.org/jeps/506) and [JEP 505, “Structured Concurrency (Fifth Preview)”](https://openjdk.org/jeps/505): scoped values (final in 25) and structured concurrency (still in preview). Consulted on October 7, 2026.

---

**Previous:** [Lesson 8 — HTTP and JSON](08-http-y-json.md) ·
**Next:** [Lesson 10 — The finished program](10-el-programa-terminado.md)

