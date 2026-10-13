# Lesson 5 — Errors and exceptions

**Time:** 90 min (tight: the lesson has four ideas and a project, and it is best not to rush it)

**What you build:** the reading of the `revisor`'s configuration: the list of services stops being written in the code and is read from a `servicios.txt` file, with a clear message for each way the file can be wrong

**What you learn:** checked and unchecked exceptions; `try`, `catch` and `finally`; `try-with-resources`; custom exceptions with their cause; when a result is better than an exception; and what to do with an `InterruptedException`

**Where you are coming from.** You bring the project from [Lesson 4](04-colecciones-genericos-streams.md): a `revisor` with its `Simulador`, a `Resumen` that counts with *streams*, a `Reporte` with a defensive copy, and a table with its summary line. It works, but it has two deep flaws. The first is the one the lesson 4 project exercise left you: to add a service you have to **open the Java code, edit it and recompile**; the data and the program change for different reasons and should not live together. The second is subtler: the review is instantaneous, because the simulator returns the time without waiting for it, and so you have no way of seeing how the program behaves when a review **takes long**. This lesson fixes both: the data goes to a file that the program reads and distrusts, and the simulation learns to wait for what its time says. You work in your `~/curso-java/revisor/` folder and run everything with `java`.

**What this lesson does not do.** It does not query any network: it still uses the simulated data, and that does not change until [Lesson 8](08-http-y-json.md). It does not use Maven or tests: Maven arrives in [Lesson 6](06-proyecto-maven.md) and tests in [Lesson 7](07-pruebas-junit.md). And the `revisor` still has no command-line options or exit code: today the file's name is written in the program, and when something fails the program prints the reason and ends with code 0. That is a defect that [Lesson 10](10-el-programa-terminado.md) corrects, and this lesson points it out when you see it.

**About the runs.** To run a figure, go into the lesson's folder in the repository (`programas/05-errores-excepciones/`) and run `java fig05_01.java`, with the number of the figure you want. The ones that live in their own folder (that of section 5.3 and that of the 5.6 project) are run from inside it. All the outputs were obtained with Java 25 (Temurin 25.0.4) and the terminal in UTF-8. Error traces carry lines of the JDK itself (`at java.base/...`) with line numbers that change from one update to another: it is normal for yours to differ in those numbers.

## By the end you will be able to

- Say of an exception type whether it is checked or unchecked, and what the compiler requires in each case.
- Write a `try` with `catch` and `finally`, catching only the exception you know how to handle and in the order the compiler accepts.
- Open a text file with `try-with-resources` and explain what that form guarantees compared with closing it by hand.
- Define a custom exception that keeps the original cause, and read a trace with its `Caused by`.
- Decide whether a failure in your program is better represented with an exception or with a value, and defend the decision.
- Handle an `InterruptedException` without swallowing it: propagate it, or restore the interrupt flag.

## The why before the how

Until lesson 4, every datum in the `revisor` was written by you, in the same file as the code. If it was wrong, you knew instantly, because the compiler would not let you through. A configuration file changes that situation completely: someone else writes it, at another time, with another editor, and the program finds out about its errors **at run time**. An outside file can fail in four ways, and all four are normal, not exceptional:

1. **It does not exist**, or it exists in a different folder from the one you think.
2. **It exists but cannot be read**, because it is in another encoding or someone left it without permissions.
3. **It is complete in form but says something absurd**: an address that does not start with `http`, a negative timeout, two services with the same name.
4. **It is empty of useful content**: only comments.

A serious program distinguishes these cases and tells the person **which one happened and how to fix it**, in one line, not with a thirty-line report written for programmers. That is what this lesson does.

But the `revisor` also has another kind of failure that looks like none of the above: a **service** that does not answer. Checking a service that is down is not an error in the program; it is just what it exists for. If the program treated a service that is down as an emergency, a single one down would bring down the report of the other ninety-nine. You already made that decision in lesson 3, without calling it that: `SinRespuesta` is a **value**, not an exception. This lesson explains it and defends it, because understanding when something is an exception and when it is a value is the most important thing Java has to teach you about errors.

A comment for those arriving from other languages. In Go, the error is a value that functions return and is checked with `if err != nil`. In Rust, it is a `Result`. In Python and TypeScript, it is an exception that rises until someone catches it. Java does both: it has exceptions, and it has values like the `Optional`s and the *sealed* types of lesson 3. And it adds something the others do not have: the compiler distinguishes **two kinds of exceptions**, and forces one of them to be dealt with. That is the part that feels strange at first and has its reason; the first section goes there.

The path is one session. Section 5.1 covers checked and unchecked exceptions and the `try` syntax; 5.2, `try-with-resources` and reading files; 5.3, custom exceptions; 5.4, when to prefer a value; 5.5, `InterruptedException`; and 5.6 brings everything together in the project. This is the destination, so you have it in view. Run from a folder other than that of the configuration file, the `revisor` will say (“error de configuración: no existe el archivo servicios.txt” means “configuration error: the file servicios.txt does not exist”):

```text
error de configuración: no existe el archivo servicios.txt
```

A one-line message, instead of a trace. To get there it is necessary to understand, first, what an exception is.

## The concepts

### 5.1 Checked and unchecked exceptions

#### 5.1.1 What an exception is and which families there are

An **exception** is an object that describes that something went wrong. When a line of the program cannot do what it was asked, it **throws** an exception (with the word `throw`, or because a method you called does it inside). The exception rises through the methods that were running, from the most recent to the oldest, looking for someone to **catch** it. If nobody catches it, the thread ends and the JVM prints the trace you already know. Lesson 4 taught you to read it from top to bottom; now we are going to see what is behind it.

All exceptions inherit from one class: `Throwable`. Two branches come from it. `Error` is for serious problems of the JVM itself (memory ran out, a corrupt class file), and the rule is that they **are not caught**: you have no way to recover. `Exception` is for what a reasonable program may want to handle. And inside `Exception` there is a special sub-branch, `RuntimeException`, with which Java divides the world in two:

- **Unchecked exceptions**: `RuntimeException`, `Error` and everything that inherits from them. The compiler **does not force you** to do anything with them. They are programming errors: a `NullPointerException`, an `IllegalArgumentException`, the `UnsupportedOperationException` from lesson 4.
- **Checked exceptions**: all the others, that is, `Exception` and its children that are not `RuntimeException`. The compiler **forces you to deal with them**, and if you do not, the program does not compile. It is the definition from the [language specification](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html): the checked ones are all those that are not unchecked. (In the rest of the course I use “checked” and “unchecked”, which are the names you will see throughout the documentation.)

The program `fig05_01` prints the inheritance chain of five exceptions and says which class each one belongs to. To write it you need a `for` with a condition instead of a traversal (`for (start; condition; step)`: it begins at `start`, repeats while the `condition` is true, and executes the `step` at the end of each iteration), a `Class<?>` (the type that represents a class; the `?` is the wildcard from lesson 4) and the methods `getSuperclass()`, which gives the parent, and `isAssignableFrom`, which answers “does this type inherit from that one?”:

```java
// fig05_01.java
void cadena(Class<?> tipo) {
  String texto = tipo.getSimpleName();
  for (Class<?> padre = tipo.getSuperclass(); padre != null; padre = padre.getSuperclass()) {
    texto = texto + " -> " + padre.getSimpleName();
  }
  boolean esExcepcion = Exception.class.isAssignableFrom(tipo);
  boolean esNoVerificada = RuntimeException.class.isAssignableFrom(tipo);
  String clase;
  if (Error.class.isAssignableFrom(tipo)) {
    clase = "error de la JVM";
  } else if (esExcepcion && !esNoVerificada) {
    clase = "verificada";
  } else {
    clase = "no verificada";
  }
  IO.println(texto);
  IO.println("    " + clase);
}

void main() {
  cadena(NoSuchFileException.class);
  cadena(InterruptedException.class);
  cadena(NumberFormatException.class);
  cadena(NullPointerException.class);
  cadena(OutOfMemoryError.class);
}
```

```text
$ java fig05_01.java
NoSuchFileException -> FileSystemException -> IOException -> Exception -> Throwable -> Object
    verificada
InterruptedException -> Exception -> Throwable -> Object
    verificada
NumberFormatException -> IllegalArgumentException -> RuntimeException -> Exception -> Throwable -> Object
    no verificada
NullPointerException -> RuntimeException -> Exception -> Throwable -> Object
    no verificada
OutOfMemoryError -> VirtualMachineError -> Error -> Throwable -> Object
    error de la JVM
```

The chain reads from left to right as “is a... which is a...”. `NoSuchFileException`, the one Java throws when a file does not exist, is a `FileSystemException`, which is an `IOException`, which is an `Exception`: checked. `InterruptedException` is a direct child of `Exception`: checked. `NumberFormatException` passes through `IllegalArgumentException` and `RuntimeException`: unchecked. `NullPointerException` too. And `OutOfMemoryError` is on the `Error` branch, which is of another family: you will never catch it. Notice also that **the checked one on the first line is an `IOException`**: the whole family of input and output problems (files, network) is checked. It is the kind of error the program cannot avoid by writing better code, because it depends on an outside world that can fail.

#### 5.1.2 What the compiler requires: catch it or declare it

Let us see what “the compiler forces you” means. This program reads a text file and counts its lines. It looks harmless:

```java
// fig05_02.java
void main() {
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
  IO.println(renglones.size());
}
```

```text
$ java fig05_02.java
fig05_02.java:3: error: unreported exception IOException; must be caught or declared to be thrown
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
                                             ^
1 error
error: compilation failed
```

`unreported exception IOException; must be caught or declared to be thrown`. The `^` arrow points to the call to `readAllLines`, the one that can throw it. For the program to compile you have **two options**, and one of the two is mandatory:

1. **Catch it** where you can handle it, with `try` and `catch` (the next section).
2. **Declare it**: warn in the method's signature that it can throw it, with `throws`. For example `void main() throws IOException`. Declaring it does not handle the error; it **hands it over** to whoever calls the method, who in turn will have to catch it or declare it. The exception rises until someone catches it; if it reaches `main` and `main` declares it too, the JVM prints the trace and the program dies.

The idea behind the rule is that certain failures are not programmer errors but errors of the situation (the file may not exist), and that it would be unfair for the code to ignore them out of carelessness. The compiler turns “I forgot this can fail” into a compile error. If it seems a burden, reserve judgment until section 5.4: it is a tool with a precise use.

#### 5.1.3 `try`, `catch` and `finally`

Catching an exception is saying: “try this; if it fails in *this* way, do *this other thing* instead of dying”. It is written with `try`, `catch` and, optionally, `finally`. The program `fig05_03` converts a text into a number, which is what the configuration needs to read a `timeout_ms`:

```java
// fig05_03.java
int leerTimeout(String texto) {
  try {
    int valor = Integer.parseInt(texto);
    IO.println("  convertí " + valor);
    return valor;
  } catch (NumberFormatException e) {
    IO.println("  no sirve: " + e.getMessage());
    return -1;
  } finally {
    IO.println("  (fin de la conversión)");
  }
}

void main() {
  IO.println("leyendo 5000");
  IO.println("  resultado: " + leerTimeout("5000"));
  IO.println("leyendo 5 s");
  IO.println("  resultado: " + leerTimeout("5 s"));
}
```

```text
$ java fig05_03.java
leyendo 5000
  convertí 5000
  (fin de la conversión)
  resultado: 5000
leyendo 5 s
  no sirve: For input string: "5 s"
  (fin de la conversión)
  resultado: -1
```

`Integer.parseInt(texto)` converts a text into an `int`. With “5000” it works; with “5 s” it throws `NumberFormatException` with the message `For input string: "5 s"` (the text it could not convert). The three blocks (the program's messages `convertí`, `no sirve` and `fin de la conversión` mean “I converted”, “it's no good” and “end of the conversion”):

- **`try { ... }`** contains the line that can fail, and preferably **only that one** and the ones that depend on it. If there is no error, the program continues to the end of the block. If there is, Java abandons the `try` at that very line and jumps to the corresponding `catch`.
- **`catch (NumberFormatException e) { ... }`** runs if the error was of that type. The `e` is the caught exception: `e.getMessage()` gives its message. Any other type of exception would pass by.
- **`finally { ... }`** runs **always**: whether there was an error or not, and even if the `try` or the `catch` did a `return`. Look at the output: with “5000”, “convertí 5000” is printed, then the `finally`, and **afterward** the result reaches the caller, although the `return valor` appears earlier in the code. It is used to clean up. [Oracle's documentation](https://docs.oracle.com/javase/tutorial/essential/exceptions/finally.html) summarizes it: “the `finally` block always executes when the `try` block exits”.

A note on form: returning `-1` to say “not valid” is a bad habit, because it is a magic value (nothing prevents someone from using it as if it were a real time). It is here only because the example is minimal; in section 5.3 the `catch` will do something better.

There are three rules for catching well, and all three are broken so often that it is worth writing them down:

1. **Catch the specific exception that you know how to handle.** A `catch (Exception e)` also catches the `NullPointerException` produced by a typo, and the program will keep running with wrong data. Better for it to stop with its trace.
2. **If you do not know what to do with an error, do not catch it.** Let it rise. A program that stops with a clear trace is better than one that continues blindly.
3. **A single `catch` can cover two types if the response is the same**, with a vertical bar: `catch (NumberFormatException | NullPointerException e)`. The types cannot be one a child of the other.

The **order** of the `catch`es matters, and the compiler watches it. If the most general goes first, the others could never run:

```java
// fig05_04.java
void main() {
  try {
    IO.println(Integer.parseInt("5 s"));
  } catch (Exception e) {
    IO.println("algo falló");
  } catch (NumberFormatException e) {
    IO.println("no es un número");
  }
}
```

```text
$ java fig05_04.java
fig05_04.java:7: error: exception NumberFormatException has already been caught
  } catch (NumberFormatException e) {
    ^
1 error
error: compilation failed
```

`exception NumberFormatException has already been caught`. The first `catch (Exception e)` swallows everything, including the `NumberFormatException`, and the second block would be dead code. The rule: **from the particular to the general**, just as when the `switch` of lesson 3 asked to order the cases.

#### 5.1.4 Why a checked exception does not cross a lambda

There is a place where the rule of checked exceptions feels like a nuisance, and it is worth knowing now because you will run into it in the `revisor`. It is inside a lambda. This program tries to wait a little inside a `map`:

```java
// fig05_05.java
void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<String> revisados =
      nombres.stream()
          .map(
              nombre -> {
                Thread.sleep(10);
                return nombre + " revisado";
              })
          .toList();
  IO.println(revisados);
}
```

```text
$ java fig05_05.java
fig05_05.java:8: error: unreported exception InterruptedException; must be caught or declared to be thrown
                Thread.sleep(10);
                            ^
1 error
error: compilation failed
```

The same error as before, but now inside the lambda: `Thread.sleep` throws `InterruptedException` (checked) and the `map` lambda neither declares nor catches it. The reason is that `map` expects a `Function`, the functional interface from lesson 4, and the `apply` method of a `Function` **declares no checked exception** in its signature. A lambda cannot throw more than its interface declares. The ways out are three, and none is pleasant: catch the exception inside the lambda (the body fills up with `try`), wrap it in an unchecked one (the advantage of the checked one is lost), or **not use a *stream* there and go back to a `for` loop**, where the method that contains it can declare the `throws`. In today's project we will take the last: `revisarTodos` goes from *stream* to `for`. It is not a step backward, it is choosing the tool that allows what the problem asks.

If you prefer a method reference (`.map(Revision::revisar)`) the message changes but the cause is the same: `incompatible thrown types InterruptedException in functional expression`. Exercise 4 asks you to provoke it.

#### 5.1.5 When checked and when not?

When you design your own exceptions you have to decide which family they belong to, and Oracle's guide gives a one-sentence rule: [“if a client can reasonably be expected to recover from an exception, make it a checked exception; if a client cannot do anything to recover from the exception, make it an unchecked exception”](https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html). A configuration file that does not exist is a good example of the first: the caller can do something useful (tell the user how to fix it). A `null` where there should never have been one is the example of the second: there is nothing to “recover”, there is a defect to correct.

It is honest to say that this Java decision is debated. Checked exceptions impose a useful discipline, but in large projects they produce a cascade of `throws` in all the signatures and of `catch`es that only forward, and other languages of the same family, like C# and Kotlin, decided not to have them. Java keeps them, and your job is not to have an opinion about them but to know that the compiler treats them differently and why.

### 5.2 `try-with-resources`: what is opened gets closed

#### 5.2.1 Resources and the form that closes them by itself

A **resource** is something that is opened and has to be closed: a file, a network connection. If you do not close it, the operating system keeps it open: with a file, that translates into a descriptor that runs out, and with a connection, into a server that keeps waiting. The usual way to close it properly was a `try` with a `finally` that closes the resource, and it has a family defect: if the `try` throws an exception and the `finally`'s `close()` does too, the second one **covers** the first, and you lose the error that really mattered.

Java 7 added a form that solves it: **`try-with-resources`**. You put the resources in parentheses right after the `try`, and Java closes them **by itself**, in reverse order from how it opened them, whether the block ends well or with an exception. A resource is any object whose type implements the `AutoCloseable` interface, which has a single method, `close()`. The program `fig05_06` demonstrates it with an invented resource that announces when it opens and when it closes (the messages `abro`, `cierro`, `trabajo con` are “I open”, “I close”, “I work with”; `frágil` is “fragile”):

```java
// fig05_06.java
record Recurso(String nombre, boolean falla) implements AutoCloseable {
  Recurso {
    IO.println("abro " + nombre);
  }

  @Override
  public void close() {
    IO.println("cierro " + nombre);
    if (falla) {
      throw new IllegalStateException("no pude cerrar " + nombre);
    }
  }
}

void main() {
  try (Recurso entrada = new Recurso("entrada", false);
      Recurso salida = new Recurso("salida", false)) {
    IO.println("trabajo con " + entrada.nombre() + " y " + salida.nombre());
  }
  IO.println("--");
  try (Recurso fragil = new Recurso("frágil", true)) {
    IO.println("trabajo con " + fragil.nombre());
    throw new IllegalArgumentException("falló el trabajo");
  } catch (IllegalArgumentException e) {
    IO.println("atrapé: " + e.getMessage());
    for (Throwable suprimida : e.getSuppressed()) {
      IO.println("  suprimida: " + suprimida.getMessage());
    }
  }
}
```

```text
$ java fig05_06.java
abro entrada
abro salida
trabajo con entrada y salida
cierro salida
cierro entrada
--
abro frágil
trabajo con frágil
cierro frágil
atrapé: falló el trabajo
  suprimida: no pude cerrar frágil
```

Before reading the output, three details of the declaration that you had not seen yet. `implements AutoCloseable` is the same `implements` from lesson 3: the *record* commits to fulfilling the interface's contract, which demands a `close()` method. `@Override` is an **annotation**: it tells the compiler “this method replaces the interface's”, and if you get the name wrong, it fails to compile instead of creating a different method without you noticing. And `public` is a word you had not needed yet: an interface's methods are always public, and that is why whoever implements them must write it. In lesson 6 you will see the full word, with its meaning and its siblings.

Now the output. In the first block you open two resources (`entrada` and `salida`, “input” and “output”) and they are closed **in reverse**: first `salida`, then `entrada`. It is the correct order: the last thing you opened usually depends on the previous one. In the second block the `close()` of `frágil` throws an exception, and so does the body of the `try` (thrown by `throw new IllegalArgumentException(...)`, which is the way to throw one by hand). The result: the `catch` receives the exception **from the body**, the one that mattered (`falló el trabajo`, “the work failed”), and the one thrown by `close()` is neither lost nor covers it: it stays **attached** to the first as a *suppressed* exception, and is retrieved with `e.getSuppressed()` (its message, `no pude cerrar frágil`, means “I couldn't close frágil”). [Oracle's guide](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) explains it with another example: with a `finally`, the exception that comes out is the `finally`'s and the `try`'s is suppressed; with `try-with-resources`, it is the other way around, and it is the order you want.

#### 5.2.2 Reading a text file

To read the `revisor`'s `servicios.txt` you need three pieces you had not used. **`Path`** represents a file path as a value, and is created with `Path.of("servicios.txt")`; `ruta.getFileName()` gives the last part. **`Files`** is a class with static methods for operating on a `Path`. And **`BufferedReader`** is an object that reads text line by line. `Files.newBufferedReader(ruta)` opens the file and returns a `BufferedReader`, and since it is a resource, it goes in a `try-with-resources`:

```java
try (BufferedReader lector = Files.newBufferedReader(ruta)) {
  String renglon;
  while ((renglon = lector.readLine()) != null) {
    // ...usar el renglón...
  }
}
```

`lector.readLine()` returns the next line as a `String`, or `null` when the file is over. Here the `null` is the way Java chose since its first versions to say “there is no more”; `Optional` did not exist. The `while` is a loop that repeats its body while the condition is true, and the condition has a very common trick: `(renglon = lector.readLine()) != null` **assigns and compares at once**, because an assignment is worth what was assigned. It reads “read a line and store it in `renglon`; if what was read is not `null`, enter the body”.

Three things about `Files` worth knowing, all from the [class documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html):

- **It reads in UTF-8.** `Files.newBufferedReader(ruta)` and `Files.readAllLines(ruta)` decode the file with UTF-8, which is what you want for a file with accents. (Other parts of Java, such as `FileReader`, used the machine's encoding, and that is why a program read well on your computer and badly on another; since Java 18, [JEP 400](https://openjdk.org/jeps/400), the default encoding of the whole platform is also UTF-8.)
- **A relative path is resolved from the folder where you run `java`**, not from where the `.java` file lives. `Path.of("servicios.txt")` looks for `servicios.txt` in the terminal's current folder. It is the cause of almost all the “can't find the file” errors you will see, and the section 5.6 project will show it.
- **A file that does not exist throws `NoSuchFileException`**, which as you saw in the inheritance chain is an `IOException`: checked. A file that exists but has bytes that are not valid UTF-8 throws `MalformedInputException`, another `IOException`, when read.

A variant to keep in mind: `Files.readAllLines(ruta)` reads the whole file into a list of lines and closes it for you (you do not need `try-with-resources`), and is good for small files. `Files.lines(ruta)` returns a *stream* of lines, but **that one does have to be closed**: the stream keeps the file open, and its documentation says so in those words. For the `revisor` the `BufferedReader` has a decisive advantage: you know which line you are on without writing a separate index, and the line number is what a useful error message needs.

### 5.3 Custom exceptions, with their cause

#### 5.3.1 Why a custom one

The configuration can fail for a dozen reasons, and all of them land in the same place: `main`, which has to tell a person something. If each reason were a different JDK exception, `main` would have to catch `NoSuchFileException`, `MalformedInputException`, `URISyntaxException`, `NumberFormatException` and half a dozen more, and know the details of each. Worse: if all were `IllegalArgumentException`, `main` could not distinguish “the configuration is wrong” from “my program has a defect”. The solution is an exception with a name of its own, which means something in the domain: **`ErrorDeConfiguracion`** (“configuration error”). It will have a single meaning (“the configuration file cannot be used”), `main` will catch only that one, and any other that escapes will be, rightly, a failure of the program that must stop it with its trace.

A custom exception is defined with a line of **inheritance**. Look at the figure `fig05_07`, a folder with two files: the definition of the exception and a program that uses it. The new word is **`extends`**: a class that `extends` another *inherits* everything the other has. It is like `implements` but for a class instead of an interface: `ErrorDeConfiguracion` *is an* `Exception` and brings all its methods (`getMessage`, `getCause`, `printStackTrace`) without writing them. The only thing needed is to say how it is built, and for that the parent class's constructor is called with **`super`**: `super(mensaje, causa)` means “build me as an `Exception` with this message and this cause”.

```java
// fig05_07/ErrorDeConfiguracion.java
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
// fig05_07/Demo.java
void leer(Path ruta) throws ErrorDeConfiguracion {
  try {
    Files.readAllLines(ruta);
  } catch (NoSuchFileException e) {
    throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e);
  } catch (IOException e) {
    throw new ErrorDeConfiguracion("no se pudo leer el archivo " + ruta.getFileName(), e);
  }
}

void main() {
  try {
    leer(Path.of("servicios-que-no-existe.txt"));
  } catch (ErrorDeConfiguracion e) {
    IO.println("mensaje: " + e.getMessage());
    IO.println("causa:   " + e.getCause());
    e.printStackTrace();
  }
}
```

The `Demo` program defines a `leer` (“read”) method that tries to read a file that does not exist and, if it fails, throws **`ErrorDeConfiguracion` with a cause**: `throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e)` (“no existe el archivo” means “the file does not exist”). The second argument, `e`, is the original exception. The two `catch`es are in the right order: first the `NoSuchFileException` (more particular), then the general `IOException`. Run the figure from its folder:

```text
$ cd fig05_07
$ java Demo.java
mensaje: no existe el archivo servicios-que-no-existe.txt
causa:   java.nio.file.NoSuchFileException: servicios-que-no-existe.txt
ErrorDeConfiguracion: no existe el archivo servicios-que-no-existe.txt
	at Demo.leer(Demo.java:6)
	at Demo.main(Demo.java:14)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.execute(SourceLauncher.java:256)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.run(SourceLauncher.java:138)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.main(SourceLauncher.java:76)
Caused by: java.nio.file.NoSuchFileException: servicios-que-no-existe.txt
	at java.base/sun.nio.fs.UnixException.translateToIOException(UnixException.java:92)
	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:106)
	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:111)
	at java.base/sun.nio.fs.UnixFileSystemProvider.newFileChannel(UnixFileSystemProvider.java:213)
	at java.base/sun.nio.fs.UnixFileSystemProvider.newByteChannel(UnixFileSystemProvider.java:244)
	at java.base/java.nio.file.Files.newByteChannel(Files.java:357)
	at java.base/java.nio.file.Files.newByteChannel(Files.java:399)
	at java.base/java.nio.file.spi.FileSystemProvider.newInputStream(FileSystemProvider.java:371)
	at java.base/java.nio.file.Files.newInputStream(Files.java:154)
	at java.base/java.nio.file.Files.newBufferedReader(Files.java:2645)
	at java.base/java.nio.file.Files.readAllLines(Files.java:3085)
	at java.base/java.nio.file.Files.readAllLines(Files.java:3122)
	at Demo.leer(Demo.java:4)
	... 6 more
```

Read the output calmly, because it is the first trace with **two** parts. The first two lines are the ones the program printed: `mensaje:` (“message”) is what you would tell the person (“the file servicios-que-no-existe.txt does not exist”), and `causa:` (“cause”) is what **really** happened, with its full type: `java.nio.file.NoSuchFileException: servicios-que-no-existe.txt`. The rest is what `e.printStackTrace()` prints, which writes the complete trace to the error output. It has two blocks. The first is the exception you caught, with its trace: `Demo.leer(Demo.java:6)` is the line of the `throw`, and `Demo.main(Demo.java:14)` is the caller. The lines that follow, from `jdk.internal.reflect` and `SourceLauncher`, are the machinery with which `java Archivo.java` starts your program. The second block begins with **`Caused by:`**: it is the cause, with its own trace, which goes down to the operating system (`sun.nio.fs.UnixException...`) and rises through `Files.readAllLines` to the line `Demo.leer(Demo.java:4)`, which is the line where the file was really tried to be opened. The last line, `... 6 more`, means that the six remaining lines are the same ones you already saw above.

That is what gives value to keeping the cause. The **message** is for the person using the program: short, in Spanish, without jargon. The **cause** is for whoever has to fix the problem: the original exception with its complete trace. Losing the cause is losing the trail. Two house rules derived from here:

1. **When you convert one exception into another, pass the original as the cause**, always.
2. **The message says what happened and what to do; the cause says why.** In the `revisor`'s message the text of the original exception never appears, because it changes between systems and versions; it is in the cause.

#### 5.3.2 The worst way to handle an error: ignoring it

If there is something never to do with an exception, it is this:

```java
// fig05_08.java
int leerTimeout(String texto) {
  try {
    return Integer.parseInt(texto);
  } catch (Exception e) {
  }
  return 5000;
}

void main() {
  IO.println("timeout_ms = " + leerTimeout("2000"));
  IO.println("timeout_ms = " + leerTimeout("dos mil"));
}
```

```text
$ java fig05_08.java
timeout_ms = 2000
timeout_ms = 5000
```

The second line says `timeout_ms = 5000` when the value written was “dos mil” (“two thousand”), with no sign that anything went wrong. The **empty** `catch (Exception e)` caught the `NumberFormatException`, did nothing with it and the function returned the default value as if nothing had happened. It is Java's equivalent of Python's `except: pass`. The person who wrote “dos mil” believes the program obeyed, and the program works with another value. When something really fails, the reason has already vanished and there is no datum to investigate. An empty `catch` is almost always a mistake, and there are three reasons to remember it: it erases the cause, it turns an error the compiler pointed out to you into one nobody sees, and it happens precisely when one is in a hurry to “make it compile”.

If you really do not know what to do with an exception, do not catch it; if you catch it, **do something with it**: fix it, translate it into another with a cause, or log it and leave a record of why you ignore it. A note on `printStackTrace()`: it is fine for learning and debugging, but it is not a way to handle an error; it only shows it.

#### 5.3.3 Two output channels

A practical question that arises here: where does the `revisor` write when something fails? The program has **two channels** of text output, which the operating system treats separately. `System.out` is the normal output, and is where `IO.println` writes. `System.err` is the **error output**, for warnings and failures. Both appear in your terminal, but a script or a pipeline can separate them: `java Revisor.java 2> errores.txt` sends only the errors to a file, and `java Revisor.java > reporte.txt` saves only the report. That is why the configuration message is printed with `System.err.println(...)`. Whoever uses the program in a script must be able to save the clean report and see the error separately. `IO.println` has no version for the error output, and that is why from here on `System.out` and `System.err` are used explicitly when the difference matters.

### 5.4 An exception or a value?

You now have the two tools to represent that something did not go as expected: the **exception** and the **value** (an `Optional`, or a sealed type like `Respuesta`). And the design question that matters most in the lesson: which do I use when? The program `fig05_09` checks the same three names in both ways, with a service that does not respond in the middle (`sin conexión`, `se cayó el lote` and `sin respuesta` mean “no connection”, “the batch went down” and “no response”):

```java
// fig05_09.java
long tiempoOLanza(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142;
    case "pagos" -> 87;
    default -> throw new IllegalStateException(nombre + ": sin conexión");
  };
}

OptionalLong tiempoO(String nombre) {
  return switch (nombre) {
    case "catalogo" -> OptionalLong.of(142);
    case "pagos" -> OptionalLong.of(87);
    default -> OptionalLong.empty();
  };
}

void main() {
  List<String> nombres = List.of("catalogo", "reportes", "pagos");

  IO.println("con una excepción:");
  try {
    for (String nombre : nombres) {
      IO.println("  " + nombre + ": " + tiempoOLanza(nombre) + " ms");
    }
  } catch (IllegalStateException e) {
    IO.println("  se cayó el lote: " + e.getMessage());
  }

  IO.println("con un valor:");
  for (String nombre : nombres) {
    OptionalLong tiempo = tiempoO(nombre);
    String texto = tiempo.isPresent() ? tiempo.getAsLong() + " ms" : "sin respuesta";
    IO.println("  " + nombre + ": " + texto);
  }
}
```

```text
$ java fig05_09.java
con una excepción:
  catalogo: 142 ms
  se cayó el lote: reportes: sin conexión
con un valor:
  catalogo: 142 ms
  reportes: sin respuesta
  pagos: 87 ms
```

With the exception, at the second service the program abandoned the entire traversal: `pagos`, which was perfectly fine, was never checked. With the value, each service produced its own response, the absence of one did not affect the others, and the report came out complete. That is the reason `Respuesta.SinRespuesta` from lesson 3 is a value.

The first version could be fixed by putting a `try` inside the loop, to catch each service separately. It would work, but notice what you did: you converted the exception into a value on every iteration, that is, you **reinvented the value**, with more code. The difference between the two tools is one of intention, and from there comes a rule for deciding:

| Question | If the answer is yes |
|---|---|
| Is it **one of the normal results** of the operation? (“this service is down”). | **A value.** |
| Does the caller have a normal path to **continue** with the next one? (a batch of a hundred services). | **A value.** |
| Could the operation **not be done** and continuing would be inventing? (the configuration file does not exist). | **An exception.** |
| Is it a **defect of the program**, of whoever wrote it? (an unexpected `null`). | **An unchecked exception.** |

For the `revisor`, then: a service that is down is a value (`SinRespuesta`) because the question the program answers is “is it up?”, and “no” is an answer. An unreadable configuration file is an exception (`ErrorDeConfiguracion`), because without a list of services the program has nothing to check, and continuing would be inventing a list. It is a design decision and you make it, but it is not arbitrary: the whole lesson comes down to asking yourself that question every time something can go wrong.

### 5.5 `InterruptedException`: it is not swallowed

The last exception in the lesson is the most misunderstood. Today's `revisor` is going to **wait**: to simulate that a service takes as long as its time says, the review calls `Thread.sleep(ms)`, which stops the thread for that number of milliseconds. And `Thread.sleep` declares **`throws InterruptedException`**, a checked exception. Why?

A Java program can have several threads of execution at the same time (lesson 9 is all about that, and today it is enough to know that a *thread* is an independent line of execution and that your program already has one, the main thread). One thread can ask another to **stop**: that is called **interrupting** it, and it is a polite request, not an order: the interrupted thread decides how and when to stop. Mechanically, interrupting a thread turns on a **flag** in it (its “interrupt status”). If the thread is asleep in a `sleep` when that happens, the `sleep` wakes up immediately and throws `InterruptedException`. According to [the tutorial documentation](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html): an interrupt is “an indication to a thread that it should stop what it is doing and do something else. It's up to the programmer to decide exactly how a thread responds to an interrupt, but it is very common for the thread to terminate.”

It is checked so that you cannot forget it: someone asked your code to stop, and ignoring it is almost never right. To see the mechanics without needing two threads, the program `fig05_10` interrupts itself: `Thread.currentThread()` returns “the thread executing this code”, and `interrupt()` turns its flag on.

```java
// fig05_10.java
void main() {
  Thread.currentThread().interrupt();
  IO.println("¿marcado como interrumpido? " + Thread.currentThread().isInterrupted());
  try {
    Thread.sleep(1000);
    IO.println("esto no se imprime");
  } catch (InterruptedException e) {
    IO.println("sleep lanzó: " + e.getMessage());
    IO.println("¿sigue marcado? " + Thread.currentThread().isInterrupted());
    Thread.currentThread().interrupt();
    IO.println("¿y después de restaurarlo? " + Thread.currentThread().isInterrupted());
  }
}
```

```text
$ java fig05_10.java
¿marcado como interrumpido? true
sleep lanzó: sleep interrupted
¿sigue marcado? false
¿y después de restaurarlo? true
```

The first line confirms that the flag is on. The `sleep(1000)` **slept not at all**: it saw the flag, threw `InterruptedException` with the message `sleep interrupted` and, and this is the important thing, **turned the flag off** when throwing: the third line says `false`. The [`Thread` documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html) puts it this way: “the interrupted status of the current thread is cleared when this exception is thrown”. That creates a problem: if you catch the exception and carry on as if nothing, the request to stop **disappears**, and nobody else (for example, the code that called yours) finds out that someone wanted to stop. Hence the two rules, and **there is no third**:

1. **Propagate it**: declare `throws InterruptedException` and let it rise. It is what the `revisor` does.
2. **If you must catch it**, restore the flag before carrying on: `Thread.currentThread().interrupt()`. It is the figure's fourth line: it left the flag on again (`true`) for whoever comes next.

What is not done is catching it and staying quiet (`catch (InterruptedException e) {}`): it is the section 5.3.2 version for a case where the cost is worse, because a program that does not obey when asked to stop cannot be shut down gracefully. In lesson 9 you will see who interrupts and why.

### 5.6 The project step: the `revisor` reads its configuration

Now to put it all together. The `revisor` changes like this:

- **`servicios.txt`**, a text file, replaces the hand-written list. It has one service per line, with the name, the address and, optionally, the timeout in milliseconds. Blank lines are ignored and those that start with `#` are comments.
- **`ErrorDeConfiguracion`** is the exception from section 5.3.
- **`Configuracion`** (“configuration”) reads the file and converts it into a list of `Servicio`, and is where the error messages live.
- **`Revision`** changes: the review waits for what the simulated time says, and so `revisar` and `revisarTodos` declare `throws InterruptedException`. `revisarTodos` goes from *stream* to `for`, because of what you saw in 5.1.4.
- **`Revisor.java`** reads the configuration, catches `ErrorDeConfiguracion` and prints it to the error output.

Nine files do not change from lesson 4: `Estado`, `Respuesta`, `Servicio`, `Resultado`, `Clasificador`, `Simulador`, `Resumen`, `Reporte` and `Formato`. They go here because the figure's folder is the complete project. Copy them as they are from lesson 4:

```java
// fig05_11/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig05_11/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig05_11/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

```java
// fig05_11/Resultado.java
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
// fig05_11/Clasificador.java
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
// fig05_11/Simulador.java
import java.util.Map;
import java.util.Optional;

final class Simulador {
  static final Map<String, Respuesta> RESPUESTAS =
      Map.of(
          "catalogo", new Respuesta.Respondio(200, 142),
          "inventario", new Respuesta.Respondio(200, 2300),
          "pagos", new Respuesta.Respondio(200, 87),
          "reportes", new Respuesta.SinRespuesta("sin conexión"));

  static Optional<Respuesta> buscar(String nombre) {
    return Optional.ofNullable(RESPUESTAS.get(nombre));
  }
}
```

```java
// fig05_11/Resumen.java
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
// fig05_11/Reporte.java
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
// fig05_11/Formato.java
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
}
```

The data file is the one described at the beginning of this section: the name, the address and, if you want, the timeout. The addresses still end in `.example`, a reserved domain that never resolves: no request in the course goes out to the internet.

```text fig05_11/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

The custom exception is the one from section 5.3.1, with its two constructors:

```java
// fig05_11/ErrorDeConfiguracion.java
final class ErrorDeConfiguracion extends Exception {
  ErrorDeConfiguracion(String mensaje) {
    super(mensaje);
  }

  ErrorDeConfiguracion(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
```

`Configuracion` is the big piece, and it is worth reading in parts. Look at the complete code and then we continue:

```java
// fig05_11/Configuracion.java
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class Configuracion {
  static List<Servicio> leer(Path ruta) throws ErrorDeConfiguracion {
    String archivo = ruta.getFileName().toString();
    List<Servicio> servicios = new ArrayList<>();
    try (BufferedReader lector = Files.newBufferedReader(ruta)) {
      String renglon;
      int numero = 0;
      while ((renglon = lector.readLine()) != null) {
        numero++;
        String limpio = renglon.strip();
        if (limpio.isEmpty() || limpio.startsWith("#")) {
          continue;
        }
        servicios.add(leerRenglon(limpio, numero));
      }
    } catch (NoSuchFileException e) {
      throw new ErrorDeConfiguracion("no existe el archivo " + archivo, e);
    } catch (IOException e) {
      throw new ErrorDeConfiguracion("no se pudo leer el archivo " + archivo, e);
    }
    verificarRepetidos(servicios);
    if (servicios.isEmpty()) {
      throw new ErrorDeConfiguracion("el archivo no tiene servicios");
    }
    return servicios;
  }

  static Servicio leerRenglon(String renglon, int numero) throws ErrorDeConfiguracion {
    String[] campos = renglon.split("\\s+");
    if (campos.length < 2 || campos.length > 3) {
      throw new ErrorDeConfiguracion(
          "renglón " + numero + ": se esperaban 2 o 3 campos y hay " + campos.length);
    }
    URI url = leerUrl(campos[1], numero);
    if (campos.length == 2) {
      return new Servicio(campos[0], url);
    }
    return new Servicio(campos[0], url, leerTimeout(campos[2], numero));
  }

  static URI leerUrl(String texto, int numero) throws ErrorDeConfiguracion {
    if (!texto.startsWith("http://") && !texto.startsWith("https://")) {
      throw new ErrorDeConfiguracion(
          "renglón " + numero + ": la url debe empezar con http:// o https://");
    }
    try {
      return new URI(texto);
    } catch (URISyntaxException e) {
      throw new ErrorDeConfiguracion("renglón " + numero + ": la url no es válida", e);
    }
  }

  static int leerTimeout(String texto, int numero) throws ErrorDeConfiguracion {
    String mensaje = "renglón " + numero + ": timeout_ms debe ser un entero mayor que 0";
    try {
      int timeoutMs = Integer.parseInt(texto);
      if (timeoutMs > 0) {
        return timeoutMs;
      }
    } catch (NumberFormatException e) {
      throw new ErrorDeConfiguracion(mensaje, e);
    }
    throw new ErrorDeConfiguracion(mensaje);
  }

  static void verificarRepetidos(List<Servicio> servicios) throws ErrorDeConfiguracion {
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

**`leer`** is the public method. It opens the file with `try-with-resources` and traverses the lines with the `while` from section 5.2.2, keeping count in `numero` (“number”). For each line, `strip()` removes the spaces at its ends; if the result is empty (`isEmpty()`) or starts with `#` (`startsWith("#")`), `continue` jumps immediately to the next line (the word `continue` means “end this iteration of the loop and go on to the next”). Any other line is converted into a `Servicio` with `leerRenglon`. The two final `catch`es convert the JDK's exceptions into `ErrorDeConfiguracion`, **passing the original as the cause**: first `NoSuchFileException`, which gives “no existe el archivo” (“the file does not exist”), and then the general `IOException`, which gives “no se pudo leer el archivo” (“the file could not be read”). The file's name comes from `ruta.getFileName().toString()`, and that explains the message in the figure above: it says `servicios.txt`, not the full path. Notice also that `numero` counts the **file's** lines, including comments and blank ones: if the error is on line 4 of the file, the message says 4, and it is the number you will see in your editor.

**`leerRenglon`** (“read line”) splits the line into fields with `renglon.split("\\s+")`. `split` receives a **regular expression** (a description of a text pattern) and returns an **array** of texts: `\s+` means “one or more spaces or tabs”, and is written with a double backslash because in a Java text the backslash has a use of its own (`\n`, `\"`) and has to be written twice for one to arrive. An **array** (`String[]`) is a fixed-size sequence: it is consulted with `campos[0]` (the first) and its size is `campos.length`, without parentheses, because it is a datum and not a method. It is the same type as `String[] args`, which you will see in lesson 6. If there are fewer than two fields or more than three, the message says how many there are: `renglón 3: se esperaban 2 o 3 campos y hay 1` (“line 3: 2 or 3 fields were expected and there is 1”).

**`leerUrl`** and **`leerTimeout`** are the two validators. `leerUrl` rejects whatever does not start with `http://` or `https://` (first filter, with its message) and then tries to build a `URI` with `new URI(texto)`. That form throws `URISyntaxException`, checked, when the address is badly written, and `leerUrl` converts it into `ErrorDeConfiguracion` with the `URISyntaxException` as the cause. (It is the reason for using `new URI(...)` and not `URI.create(...)`, which you used in lesson 3: `URI.create` wraps the same exception in an unchecked one, and here it is better to see it as it is.) `leerTimeout` converts the text with `Integer.parseInt` and rejects whatever is not an integer greater than zero. Notice how it is written, because it is a useful pattern: the `try` attempts the conversion and, if the number is valid, **returns right there**; if the conversion fails, the `catch` throws the error with the `NumberFormatException` as the cause; and if the conversion works but the number is not greater than zero, the `try` returns nothing and the flow falls to the last line, which throws the same message without a cause. One message, two error paths.

**`verificarRepetidos`** (“check repeats”) uses the trick from section 4.1.2: `vistos.add(nombre)` returns `false` if the name was already there, and those names are gathered in a `LinkedHashSet` (which keeps the order in which they were found) and `String.join(", ", repetidos)`, which joins the elements with a separator, lists them all in the message: `nombres repetidos: catalogo` (“repeated names: catalogo”). A detail about what this method does: **the program stops at the first error**, and does not gather all the file's errors to report them at once. It is simpler, and enough for a four-line file.

Now `Revision`, which changes in two ways. First, `revisar` waits for the response's time with `Thread.sleep(tiempo.getAsLong())` if there is one (the `OptionalLong` from lesson 4), and so it declares `throws InterruptedException`. Second, `revisarTodos` is a `for` that fills a list, because the lambda of a *stream* cannot throw the checked exception, as was seen in section 5.1.4:

```java
// fig05_11/Revision.java
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

final class Revision {
  static Resultado revisar(Servicio servicio) throws InterruptedException {
    Respuesta respuesta =
        Simulador.buscar(servicio.nombre()).orElse(new Respuesta.SinRespuesta("sin conexión"));
    Resultado resultado =
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
    OptionalLong tiempo = resultado.tiempoMs();
    if (tiempo.isPresent()) {
      Thread.sleep(tiempo.getAsLong());
    }
    return resultado;
  }

  static List<Resultado> revisarTodos(List<Servicio> servicios) throws InterruptedException {
    List<Resultado> resultados = new ArrayList<>();
    for (Servicio servicio : servicios) {
      resultados.add(revisar(servicio));
    }
    return resultados;
  }
}
```

And `Revisor.java` closes the circle. It reads the configuration, checks and assembles the report, and catches `ErrorDeConfiguracion` to print it to the error output with the prefix `error de configuración:` (“configuration error:”). Its `main` declares `throws InterruptedException`, which is rule 1 of section 5.5: it does not handle it because it would not know what to do, it just lets it rise.

```java
// fig05_11/Revisor.java
void main() throws InterruptedException {
  Path ruta = Path.of("servicios.txt");
  try {
    List<Servicio> servicios = Configuracion.leer(ruta);
    IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
  } catch (ErrorDeConfiguracion e) {
    System.err.println("error de configuración: " + e.getMessage());
  }
}
```

Run the project from its folder. The review **now waits**: the table takes about two and a half seconds to appear (the sum of 142, 2300 and 87 ms; I measured about three seconds in total with Java's startup included). It is what would happen in series with real services, and it is the problem lesson 9 will solve.

```text
$ cd fig05_11
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

And now, the lesson's destination. Run the `revisor` **from another folder**, without going through where the configuration file is. Then run it with the error output discarded (`2>/dev/null`, which means “send channel 2, the error output, to nothing”) and ask for the last command's exit code with `$?`:

```text
$ java fig05_11/Revisor.java
error de configuración: no existe el archivo servicios.txt
$ java fig05_11/Revisor.java 2>/dev/null
$ echo "código de salida: $?"
código de salida: 0
```

Three things happened. First, the program said `error de configuración: no existe el archivo servicios.txt`: the `Path.of("servicios.txt")` is relative, it was resolved against the folder where you are standing, and there the file does not exist, although it does exist in the project's folder. Second, with `2>/dev/null` **nothing appeared**: the message went through the error channel, and when discarded there is no output. It is exactly the separation of section 5.3.3. Third, the exit code was `0`: the program failed, and the operating system believes everything went fine. For a script that runs the `revisor` that is a lie, and lesson 10 fixes it with `System.exit`. For now, note it as a known defect.

## The error you will see

This lesson's error is the one from section 5.1.2, and almost everyone meets it the first time they use an input/output method:

```text
fig05_02.java:3: error: unreported exception IOException; must be caught or declared to be thrown
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
                                             ^
```

**First line: where and what.** It carries the file and the line (`fig05_02.java:3`), the word `error` and the diagnosis: `unreported exception IOException`. “Unreported” means the method can throw that exception and nobody caught it or declared it. **Second and third: the line of code and the arrow.** The `^` points to the parenthesis of the call that can throw it, which is what you should look at. **What to do.** There are two fixes, and the choice is one of design. If at that place you know what to do when it fails, **catch it** (and do something useful, not an empty `catch`). If you do not know, **declare it with `throws`** and let whoever called handle it. A bad fix, which is sometimes seen: declaring `throws Exception` to silence the compiler. It works, and it hides from the signatures what can go wrong.

The lesson's second error is not the compiler's: it is the `revisor` from another folder, and you already saw it in section 5.6. The message `no existe el archivo servicios.txt` does not tell you **where** it looked. If you run into it and the file exists, the first thing to check is from which folder you ran `java`.

## What gets done wrong

- **An empty `catch`**, or with only `printStackTrace()`. It is the one from section 5.3.2: the error disappears, or is printed and the program carries on as if nothing. Cost: wrong values that nobody sees, and when something finally fails, a trail that no longer exists.

- **Catching `Exception` or `Throwable` just in case.** It also catches programming defects (a `NullPointerException`) and turns them into a misleading message. Cost: the real defect is buried. The only place for a `catch (Exception e)` is the program's last frontier, to log and end gracefully, not to continue.

- **Converting one exception into another without passing the cause.** It is writing `throw new ErrorDeConfiguracion("el archivo no sirve")` without the second argument. Cost: the message says it failed and the cause, which was what allowed fixing it, is gone.

- **Returning a magic value** (`-1`, `null`, an empty string) instead of throwing or returning a typed value. Cost: the caller has to remember to check, and the compiler does not help.

- **A `return` inside a `finally`.** If the `finally` returns a value, it **discards** the exception that was rising, and the error is lost without a single trace. Cost: the same as an empty `catch`, with the difference that the code looks more innocent. The `finally` is for cleaning up, not for deciding what is returned.

- **Swallowing an `InterruptedException`.** Cost: a program that does not obey when asked to stop (section 5.5). Propagate it or restore the flag.

- **Using an exception for normal flow.** For example, calling `Integer.parseInt` in a loop and catching `NumberFormatException` to decide whether a text “is a number”, when it is the expected thing, not the exceptional. Cost: slower code (creating the trace costs) and the design decision of section 5.4 turned upside down: if the absence of a result is normal, it is a value.

## Exercises

### Exercise 1 — One broken file for each error

`Configuracion` knows how to fail in several ways, each with its message. For each of the eight cases below, **predict the message before running** and then check it. Create a new folder per case (for example, `roto-1/`), put inside a `servicios.txt` with the indicated content and run the `revisor` **from that folder**: `java ../revisor/Revisor.java` (adjust the path to wherever you have your project). Since the program looks for `servicios.txt` in the current folder, you do not need to touch the code.

1. `catalogo https://catalogo.example` and, on the next line, only `pagos`.
2. A comment (`# x`) on line 1 and, on line 2, `catalogo ftp://catalogo.example`.
3. `catalogo https://c.example` and `pagos http://pa|gos.example`.
4. `catalogo https://c.example 5000` and `pagos https://p.example abc`.
5. Two lines, both with the name `catalogo`.
6. A file with only comments and blank lines.
7. `catalogo https://c.example 0`.
8. `catalogo https://c.example 1 2 3`.

### Exercise 2 — Make the reason disappear

Change `leerTimeout` in `Configuracion.java` so that the `catch (NumberFormatException e)` throws nothing and simply returns `5000`. In a new folder, put a `servicios.txt` with the single line `pagos https://p.example abc` and run from there (`java ../revisor/Revisor.java`). What do you see? Then restore the code. Write in your logbook how long it would take you to discover the problem if the file had a hundred lines.

### Exercise 3 — A file in another encoding

In a new folder (`mkdir latin1 && cd latin1`), create a file with an accent written in the old ISO-8859-1 encoding (the *latin1* of old systems), which is not UTF-8, with this command:

```bash
printf 'catalogo https://catalogo.example\n# caf\xe9\n' > servicios.txt
```

Run the `revisor` from that folder (`java ../revisor/Revisor.java`) and read the message. Then, add a line to the `catch` of `Revisor.java` to print the cause (`e.getCause()`) and find out which JDK exception is behind the message. Restore the code.

### Exercise 4 — Turn `revisarTodos` back into a *stream*

Replace the `for` of `revisarTodos` with `servicios.stream().map(Revision::revisar).toList()`. **Predict** whether it compiles and what the compiler will say. Then check. And answer: what would you have to change in `revisar` for that version to compile?

## Solutions

### Solution 1

| Content of `servicios.txt` | Message |
|---|---|
| `catalogo https://catalogo.example` and `pagos` | `error de configuración: renglón 2: se esperaban 2 o 3 campos y hay 1` |
| `catalogo ftp://catalogo.example` (after a comment) | `error de configuración: renglón 2: la url debe empezar con http:// o https://` |
| `pagos http://pa\|gos.example` (line 2) | `error de configuración: renglón 2: la url no es válida` |
| `pagos https://p.example abc` (line 2) | `error de configuración: renglón 2: timeout_ms debe ser un entero mayor que 0` |
| two lines with `catalogo` | `error de configuración: nombres repetidos: catalogo` |
| only comments and blank lines | `error de configuración: el archivo no tiene servicios` |
| `catalogo https://c.example 0` | `error de configuración: renglón 1: timeout_ms debe ser un entero mayor que 0` |
| `catalogo https://c.example 1 2 3` | `error de configuración: renglón 1: se esperaban 2 o 3 campos y hay 5` |

I measured the eight messages with the section 5.6 project (“la url debe empezar con http:// o https://” is “the url must start with http:// or https://”; “la url no es válida”, “the url is not valid”; “timeout_ms debe ser un entero mayor que 0”, “timeout_ms must be an integer greater than 0”; “el archivo no tiene servicios”, “the file has no services”). There are two things to look at carefully. In case 2, the comment on line 1 counts: the error is on line 2 **of the file**, not on the second service. And in cases 4 and 7 the message is the same although the reason is different (a text that is not a number, and a number that is not positive): for whoever writes the file, the fix is the same. The difference is in the cause, which only exists in the first (the `NumberFormatException`), and is seen by whoever debugs.

### Solution 2

```java
static int leerTimeout(String texto, int numero) throws ErrorDeConfiguracion {
  String mensaje = "renglón " + numero + ": timeout_ms debe ser un entero mayor que 0";
  try {
    int timeoutMs = Integer.parseInt(texto);
    if (timeoutMs > 0) {
      return timeoutMs;
    }
  } catch (NumberFormatException e) {
    return 5000;
  }
  throw new ErrorDeConfiguracion(mensaje);
}
```

With a `servicios.txt` that only has `pagos https://p.example abc`, the `revisor` prints `pagos`'s table as `ARRIBA`, with no warning at all: `abc` disappeared and the service ended up with the default timeout. With a hundred lines, you would have no way of knowing which of them had an invalid time: the program “works”. It is the problem of section 5.3.2 in miniature. The original error message (`timeout_ms debe ser un entero mayor que 0`) was not a nuisance: it was the only warning the person writing the file was going to get.

### Solution 3

The message is `error de configuración: no se pudo leer el archivo servicios.txt` (“configuration error: the file servicios.txt could not be read”): it does not say why. With `System.err.println(e.getCause());` in the `catch` of `Revisor.java`, this is printed:

```text
java.nio.charset.MalformedInputException: Input length = 1
```

The byte `0xE9` is the `é` in ISO-8859-1, but as a UTF-8 sequence it is invalid (in UTF-8 the `é` takes two bytes). `Files.newBufferedReader` decodes in UTF-8 and, on running into the byte, throws `MalformedInputException`, an `IOException` that `Configuracion.leer` converts into “no se pudo leer”. Notice that the message for the person is correct but not very informative, and that the **cause** has the datum that is needed. It is the usefulness of keeping it: a short message for the user, the complete cause for whoever debugs.

### Solution 4

It does not compile. The compiler says (I shortened the path, which the launcher prints in full):

```text
Revision.java:20: error: incompatible thrown types InterruptedException in functional expression
    return servicios.stream().map(Revision::revisar).toList();
                                  ^
```

`Revision::revisar` declares `throws InterruptedException` and the `Function` that `map` expects cannot throw it. For it to compile you would have to remove the `throws` from `revisar`, and that means **catching the `InterruptedException` inside `revisar`**, where you do not know what to do with it; the right thing would be to restore the flag with `Thread.currentThread().interrupt()` and return something, which hides the interruption from the caller. That is why the course keeps the `for`: it is the one that lets the exception pass through to whoever can decide.

## How I know I got it

- [ ] `java fig05_01.java` prints the five chains, and you can say why `NoSuchFileException` is checked and `NumberFormatException` is not.
- [ ] `java fig05_02.java` fails with `unreported exception IOException`, and you know the two fixes the compiler accepts.
- [ ] `java fig05_04.java` fails with `has already been caught`, and you know why the order of the `catch`es goes from the particular to the general.
- [ ] `java fig05_06.java` prints the closings in reverse order, and you find in the output the suppressed exception.
- [ ] From the folder of the custom exceptions figure, `java Demo.java` prints a `Caused by:`, and you know which JDK exception is behind it.
- [ ] `java fig05_10.java` prints `true`, `false` and `true`, and you can explain what each one did.
- [ ] From the project folder, `java Revisor.java` prints the table with `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`, and takes about two and a half seconds.
- [ ] From another folder, the `revisor` prints `error de configuración: no existe el archivo servicios.txt`, and you can explain why the file exists and yet it does not find it.
- [ ] The eight broken files of exercise 1 produce the eight messages of the solution, and you predicted them before running.
- [ ] You wrote in your logbook a checked exception that forced you to decide something.

## Summary

Answer without looking at the lesson:

1. What is the difference between a checked and an unchecked exception? Which one does the compiler force you to deal with?
2. What does `unreported exception IOException; must be caught or declared to be thrown` mean, and what are the two fixes?
3. What does a `finally` guarantee? And why should it not carry a `return`?
4. Why does the order of the `catch`es go from the particular to the general?
5. What does `try-with-resources` guarantee compared with closing by hand? In what order does it close?
6. Why can a checked exception not be thrown from inside a `map` lambda, and what does the `revisor` use instead?
7. What is the cause of an exception, and what happens if you do not pass it when converting it into another?
8. Why is a service that is down a value and an unreadable configuration file an exception?
9. What does `Thread.sleep` do when someone interrupts the thread, and what happens to the interrupt flag?
10. What are the two correct ways of handling an `InterruptedException`?

## Further reading

- [Oracle, “Lesson: Exceptions” (Java tutorial)](https://docs.oracle.com/javase/tutorial/essential/exceptions/index.html): the whole topic, from what an exception is to chaining them, with the discussion about unchecked ones. Consulted on October 7, 2026.
- [The Java Language Specification, chapter 11, “Exceptions”](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html): the exact definition of checked and unchecked and of what each method must declare. Consulted on October 7, 2026.
- [Oracle, “The try-with-resources Statement”](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html): the closing order and suppressed exceptions, with examples. Consulted on October 7, 2026.
- [Oracle, “Interrupts” (concurrency tutorial)](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html): how a thread is stopped politely, and why `InterruptedException` is not ignored. Consulted on October 7, 2026.

---

**Previous:** [Lesson 4 — Collections, generics and streams](04-colecciones-genericos-streams.md) ·
**Next:** [Lesson 6 — A Maven project](06-proyecto-maven.md)
