# Lesson 3 — Records, enums and sealed types

**Time:** 2 × 90 min (two sessions; the break is between sections 3.2 and 3.3)

**What you build:** the `revisor`'s data model: a `Servicio` with its name, its address and its timeout, an `Estado` that can only be worth three things, and a `Respuesta` that is either “the service responded” or “it did not respond”, with no `null` in between. The `revisor` goes from one file to seven

**What you learn:** `record` (components, accessors, `equals`, `hashCode` and `toString` for free, compact constructor and additional constructor); why a `record`'s immutability is shallow; `enum`; `sealed` interfaces with `permits`; `switch` with patterns, exhaustive and without `default`; guards with `when`; record patterns and the unnamed pattern `_`; and how to run a multi-file project with `java Revisor.java`

**Where you are coming from.** You bring the `Revisor.java` from [Lesson 2](02-tipos-cadenas-control.md): a single compact source file with `codigoDe`, `tiempoDe`, `clasificar`, `formatearTiempo`, `detalle`, `linea`, `encabezado`, `revisar` and a `main` that prints the table of four services. It works, and it has a weakness that the lesson's project exercise left in plain view: each service's information lives **spread out** across two parallel `switch`es, and a service that did not respond is a `null` in each. If you forget to add a datum in one of the two, the program compiles, runs and shows a wrong row, without any complaint. This lesson fixes that with three tools of the language: types that **group** data (`record`), types that **enumerate** possibilities (`enum`), and types that say “it is one of these forms and no other” (`sealed`).

**What this lesson does not do.** It does not use lists yet: the four services are still written by hand in `main`, one by one, and their checking is still simulated with a `switch`. Collections arrive in [Lesson 4](04-colecciones-genericos-streams.md) and real errors, with `try` and `catch`, in 5. Here `throw` appears once, in an example, and is explained in a box; its full handling belongs to Lesson 5. Nor is there `public` or `private` in the project's code: visibility is explained in Lesson 6, along with packages.

**About the runs.** The loose figures (`fig03_01.java` to `fig03_15.java`) are run with `java fig03_NN.java`. The multi-file project, figure `fig03_16`, is a **folder**: you go into it and run `java Revisor.java`. You have them in [`programas/03-records-enums-sellados/`](../programas/03-records-enums-sellados/). The outputs were obtained with Temurin 25.0.4 and `LANG=C.UTF-8`. The figures that fail on purpose carry their error in a file `fig03_NN.error-esperado.txt`.

## By the end you will be able to

- Declare a `record` and explain what the compiler gives it for free (accessors, `equals`, `hashCode`, `toString`).
- Write a compact constructor that normalizes a datum, and an additional constructor that fills in default values.
- Explain why a `record` with an array inside is not truly immutable.
- Declare an `enum`, walk through its values and write a `switch` on it that the compiler checks.
- Declare a `sealed` interface with its permitted forms and write a `switch` with patterns that covers them all without `default`.
- Use a `when` guard, a record pattern and the unnamed pattern `_`, and say in what order the cases go.
- Show what happens to the compiler when you add a new case to a sealed type, and list the places it forces you to change.

## The why before the how

Until now the `revisor` has a problem of **representation**. To say “catalogo responded 200 in 142 ms” it uses two functions that know nothing of each other; to say “reportes did not respond” it uses two `null`s. A reader of the code cannot see the shape of the data: they have to deduce it from how it is used. And the compiler does not know it either, so it cannot warn you when someone makes a mistake: you can ask for the code of a service that did respond and the time of one that did not, and Java allows it.

The idea of this lesson is one sentence: **make the compiler know the shape of your data, and impossible states can no longer be written**. Two concrete examples from the `revisor`:

- A service that did not respond **has no** HTTP code or time. With a `null` in each you can end up with a code without a time, or the other way around. If instead “responded” and “did not respond” are two different types, and only the first has a code and a time, the second cannot have them even if you want it to.
- A status is `ARRIBA`, `LENTO` or `CAIDO`. With a `String` it can be `"ARIBA"` (with a typo) and the program accepts it. With an `enum` it does not.

If you come from other languages, this will sound familiar: in Rust it is an `enum` with data; in TypeScript, a discriminated union; in modern Python, a `dataclass` with `match`; in Go there is no direct equivalent, and it is simulated with interfaces. Java arrived late to this party —*records* are from Java 16, sealed types from Java 17 and patterns in `switch` from Java 21— but it arrived complete and with the compiler's checking included.

The path has two sessions. The **first** covers 3.1 and 3.2: *records* and *enums*. When you finish it you will be able to write data types in one line and you will know what is and is not immutable. The **second** covers 3.3 to 3.5: sealed types, the `switch` with patterns and the project step. When you finish it, the `revisor` will still print **exactly the same table** as at the end of lesson 2, but built with types and not with `null`. That is the proof: the behavior does not change and the code is different.

## The concepts

### 3.1 Records

#### 3.1.1 The problem: a class just to carry data

Suppose you want to store together a service's name and its time. In Java, the usual path is to write a class. Look at what that path is like, just to see the **size** (you do not need to understand every line):

```java
final class PuntoClasico {
  final String nombre;
  final long tiempoMs;

  PuntoClasico(String nombre, long tiempoMs) {
    this.nombre = nombre;
    this.tiempoMs = tiempoMs;
  }

  String nombre() {
    return nombre;
  }

  long tiempoMs() {
    return tiempoMs;
  }

  @Override
  public boolean equals(Object otro) {
    if (this == otro) {
      return true;
    }
    if (otro == null || getClass() != otro.getClass()) {
      return false;
    }
    PuntoClasico p = (PuntoClasico) otro;
    return tiempoMs == p.tiempoMs && Objects.equals(nombre, p.nombre);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nombre, tiempoMs);
  }

  @Override
  public String toString() {
    return "PuntoClasico[nombre=" + nombre + ", tiempoMs=" + tiempoMs + "]";
  }
}
```

That is thirty-nine lines to say “a name and a time”. And what each one does is mechanical: store the two data, return them, compare them and display them. It is the kind of code that is written the same way every time, and that can have an error every time: forgetting a field in `equals`, or in `hashCode`, or in `toString`. (You already learned why `equals` matters: `==` between objects asks whether they are the same one, not whether they are worth the same.)

Java 16 gave this pattern a name: the ***record***. Its [specification](https://openjdk.org/jeps/395) describes it as a transparent carrier of immutable data, and the class above, complete, is written like this:

```java
record Punto(String nombre, long tiempoMs) {}
```

One line. Let us see it in action.

#### 3.1.2 What a `record` gives you

Between the parentheses go the **components**: the type and name of each datum. The compiler uses that list to generate everything else. Look at `fig03_01.java`, which uses the `record` `Punto` (“point”) and tests it:

```java
// fig03_01.java
record Punto(String nombre, long tiempoMs) {}

void main() {
  Punto a = new Punto("catalogo", 142);
  Punto b = new Punto("catalogo", 142);
  Punto c = new Punto("pagos", 87);

  IO.println(a);
  IO.println("nombre:     " + a.nombre());
  IO.println("tiempoMs:   " + a.tiempoMs());
  IO.println("a.equals(b): " + a.equals(b));
  IO.println("a == b:      " + (a == b));
  IO.println("a.equals(c): " + a.equals(c));
  IO.println("mismo hash:  " + (a.hashCode() == b.hashCode()));
}
```

```text
$ java fig03_01.java
Punto[nombre=catalogo, tiempoMs=142]
nombre:     catalogo
tiempoMs:   142
a.equals(b): true
a == b:      false
a.equals(c): false
mismo hash:  true
```

Each line of output shows something the compiler wrote for you:

- **A constructor**, with one parameter per component, in the same order. `new Punto("catalogo", 142)` creates one. (The 142 is an `int`, and it fits in the `long` component through the lossless conversion from the previous lesson.)
- **One accessor per component**, with the same name as the component: `a.nombre()` and `a.tiempoMs()`. Notice that they are **not called `getNombre()`**: it is one of the few Java conventions where the *record* departs from tradition.
- **`toString`**, with the format `Punto[nombre=catalogo, tiempoMs=142]`. It is what comes out when printing the object.
- **`equals`**, which compares **component by component**: `a.equals(b)` is `true` because both are worth the same, even though they are two different objects (`a == b` is `false`, as you expected after Lesson 2). For a `record`, `equals` does what you wanted `==` to do.
- **`hashCode`**, consistent with `equals`: if two objects are equal, their `hashCode` (a number that summarizes the content) matches. You do not use it yet; Lesson 4 needs it for sets and dictionaries.

And there are more rules, which are learned with the compiler. The components are **final**: once the *record* is created, none of them changes. Look at `fig03_02.java`, which tries to change one:

```java
// fig03_02.java
record Punto(String nombre, long tiempoMs) {}

void main() {
  Punto punto = new Punto("catalogo", 142);
  punto.tiempoMs = 5;
}
```

```text
$ java fig03_02.java
fig03_02.java:6: error: cannot assign a value to final variable tiempoMs
  punto.tiempoMs = 5;
       ^
1 error
error: compilation failed
```

The compiler rejects it: “cannot assign a value to final variable `tiempoMs`”. A *record* is, in its basic form, a value that is created complete and not touched again. Nor can a loose field be added (`int extra;` inside the *record* gives the error “field declaration must be static”, with the suggestion of turning it into a component), and a *record* cannot extend another class. What it can do: have **methods**, implement **interfaces** (you will use that in 3.3) and have **`static` constants**.

That restriction is the point. A `record` says with its form: “I am a set of fixed data, and my identity is my content”. Whoever reads it knows what to expect without reading a single line of logic, and whoever uses it cannot break it by accident.

#### 3.1.3 An additional constructor and default values

The `revisor` needs a `Servicio` with three data: a `nombre` (“name”), a `url` and a `timeoutMs`, the maximum time it is waited for (lesson 8 uses it for real). Almost all services use the default value of 5000 ms; a few need another. Java has no parameters with default values (we said so in Lesson 2), but a *record* can have an **additional constructor**, shorter, that calls the main one. Look at `fig03_03.java`:

```java
// fig03_03.java
record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}

void main() {
  URI direccion = URI.create("https://catalogo.example");
  Servicio catalogo = new Servicio("catalogo", direccion);
  Servicio notificaciones =
      new Servicio("notificaciones", URI.create("https://notificaciones.example"), 1000);

  IO.println(catalogo);
  IO.println(notificaciones);
  IO.println("host de catalogo: " + catalogo.url().getHost());
}
```

```text
$ java fig03_03.java
Servicio[nombre=catalogo, url=https://catalogo.example, timeoutMs=5000]
Servicio[nombre=notificaciones, url=https://notificaciones.example, timeoutMs=1000]
host de catalogo: catalogo.example
```

There are four novelties in that figure.

**`URI`** is a JDK class that represents an address. `URI.create("https://catalogo.example")` creates one from a text, and `getHost()` returns the server (`catalogo.example`). It is in the `java.net` package. To use a class from another package in a normal file, you have to **import** it with a line `import java.net.URI;` at the beginning (you will see it in the 3.5 project); the compact source file imports for you everything that the `java.base` module brings, and `URI` is part of it, so here it was not needed.

**A constant inside a *record*.** `static final int TIMEOUT_POR_OMISION_MS = 5000;` is a constant like the one from the previous lesson; `static` makes it belong to the type and not to each object, which is the only way for a *record* to have something loose.

**The additional constructor.** `Servicio(String nombre, URI url)` has two parameters; its body, `this(nombre, url, TIMEOUT_POR_OMISION_MS)`, **calls the main constructor** with the third datum filled in. With that, `new Servicio("catalogo", direccion)` and `new Servicio("notificaciones", ..., 1000)` are valid calls and come out with the corresponding time: 5000 in the first, 1000 in the second. Notice also the name `timeoutMs`: it carries the unit, just as in the previous lesson.

**The call split over two lines.** When a call exceeds 100 columns, it is cut after the `=` and the continuation goes in with four more spaces. It is the style guide we talked about.

#### 3.1.4 The compact constructor: validating and normalizing

Sometimes you want the *record* to **clean or check** its data when created: remove spaces from a name, reject an impossible time. For that there is the **compact constructor**: it is written with the *record*'s name and **without parentheses**, and its body runs before the components are assigned. Inside you can reassign the parameters (which have the components' names) and, if something is wrong, stop the program with an error. Look at `fig03_04.java`:

```java
// fig03_04.java
record Umbral(String nombre, long limiteMs) {
  Umbral {
    nombre = nombre.strip();
    if (limiteMs <= 0) {
      throw new IllegalArgumentException("limiteMs debe ser mayor que 0 y llegó " + limiteMs);
    }
  }
}

void main() {
  Umbral lento = new Umbral("  lento ", 1000);
  IO.println(lento);
  Umbral roto = new Umbral("roto", 0);
  IO.println(roto);
}
```

```text
$ java fig03_04.java
Umbral[nombre=lento, limiteMs=1000]
Exception in thread "main" java.lang.IllegalArgumentException: limiteMs debe ser mayor que 0 y llegó 0
	at fig03_04$Umbral.<init>(fig03_04.java:6)
	at fig03_04.main(fig03_04.java:14)
```

The first creation goes through the compact constructor: `nombre = nombre.strip();` reassigns the parameter with its clean version (the rule from 2.2.1: `strip` returns a new text, and here we do save it), and the *record* ends up as `Umbral[nombre=lento, limiteMs=1000]`, with the name without the spaces it arrived with. The second creation has `limiteMs` at 0, and the `throw` statement stops the execution (the message, in Spanish, says `limiteMs debe ser mayor que 0 y llegó 0`: “limiteMs must be greater than 0 and it was 0”).

> **A box about `throw`.** `throw new IllegalArgumentException("...")` means “here is a problem I do not know how to solve; stop what you are doing and report”. `IllegalArgumentException` is a JDK class whose name means exactly that. When nobody catches it, the program ends and the JVM prints the message and the **trace** (the list of where it happened: here, the constructor `Umbral.<init>` called from `main`). How to catch an exception, when it makes sense and when not, is Lesson 5. For now you only need to know that a compact constructor is the place to reject data that makes no sense.

The trace is a good opportunity to practice reading. What it says, from top to bottom: the error was `IllegalArgumentException` with the message we wrote; it happened in `fig03_04$Umbral.<init>` (the *record*'s constructor, inside the compact source file's class) on line 6, and that call was made by `main` on line 14. The `$` separates the file's implicit class from the `Umbral` class that lives inside it.

In the `revisor`'s `Servicio` we **do not validate** in the compact constructor: validating what arrives from a configuration file is a task that has its own place, which will arrive with custom exceptions (Lesson 5) and JSON (Lesson 8).

#### 3.1.5 Immutability is shallow

A *record* is immutable in the sense that **its components are not reassigned**. But if a component is, in turn, an object that can be modified, that object is still modifiable. Immutability reaches only the first layer. To see it you need a mutable type, and the simplest one in Java is the **array**.

> **An array in three lines.** An array is a fixed-size sequence of elements of the same type. `String[] etapas = {"conectar", "esperar"};` creates one of two texts. `etapas[0]` is the first element (counting starts at 0), and `etapas[0] = "otro";` **changes** that element: arrays are mutable. To print their content you use `Arrays.toString(etapas)`; without it, it prints an unreadable address. Arrays are hardly used in everyday Java; in Lesson 4 lists replace them. But they are the clearest example of a mutable object.

Look at `fig03_05.java`:

```java
// fig03_05.java
record Ruta(String nombre, String[] etapas) {}

void main() {
  String[] etapas = {"conectar", "esperar"};
  Ruta ruta = new Ruta("catalogo", etapas);
  IO.println("antes:   " + Arrays.toString(ruta.etapas()));

  etapas[0] = "CAMBIADO DESDE FUERA";
  ruta.etapas()[1] = "CAMBIADO DESDE EL ACCESOR";
  IO.println("después: " + Arrays.toString(ruta.etapas()));

  Ruta copia = new Ruta("catalogo", new String[] {"conectar", "esperar"});
  Ruta otra = new Ruta("catalogo", new String[] {"conectar", "esperar"});
  IO.println("copia.equals(otra): " + copia.equals(otra));
}
```

```text
$ java fig03_05.java
antes:   [conectar, esperar]
después: [CAMBIADO DESDE FUERA, CAMBIADO DESDE EL ACCESOR]
copia.equals(otra): false
```

There are two holes. The first: `etapas[0] = "CAMBIADO DESDE FUERA"` (“CHANGED FROM OUTSIDE”) modifies the **original** array that we passed to the constructor, and since the *record* stores **the same reference** (not a copy), the *record* changed without anyone touching it. The second: `ruta.etapas()[1] = ...` modifies the array through the accessor, which returns the reference it stores. That is why the program prints two changes on the second line. The *record* could not prevent either one, because what is final is the **reference** `etapas`, not the content of the array.

The last line has another surprise. Two `Ruta`s created with arrays of equal content are **not equal** according to `equals`. The *record*'s `equals` compares component by component, and to compare the arrays it uses the arrays' `equals`, which asks about identity, like `==`. It is the same problem as in 2.2.2, again.

The practical lesson: **a *record* is only as immutable as its components.** With texts, numbers, `enum`s and other immutable *records* there is no problem. With an array, or with any object that can be modified, immutability is an illusion. In Lesson 4 you will see the usual solution: storing an **unmodifiable copy** of the list in the compact constructor (`List.copyOf`), and the door will be closed.

### 3.2 Enums

The `revisor` classifies each service into one of three statuses: `ARRIBA` (up), `LENTO` (slow) or `CAIDO` (down). In Lesson 2 the status was a `String`, which means any text was valid: `"ARIBA"`, `"caído"`, `""`. An ***enum*** (from “enumeration”) is a type whose possible values are **a fixed list, written by you**, and nothing else. Look at `fig03_06.java`:

```java
// fig03_06.java
enum Estado { ARRIBA, LENTO, CAIDO }

enum Severidad {
  BAJA(1), MEDIA(5), ALTA(10);

  final int peso;

  Severidad(int peso) {
    this.peso = peso;
  }

  boolean urgente() {
    return peso >= 5;
  }
}

String aviso(Estado estado) {
  return switch (estado) {
    case ARRIBA -> "todo bien";
    case LENTO -> "revisar pronto";
    case CAIDO -> "avisar ya";
  };
}

void main() {
  Estado estado = Estado.LENTO;
  IO.println(estado);
  IO.println("name():    " + estado.name());
  IO.println("ordinal(): " + estado.ordinal());
  IO.println("== :       " + (estado == Estado.LENTO));
  IO.println("valueOf:   " + Estado.valueOf("CAIDO"));
  IO.println("values():  " + Arrays.toString(Estado.values()));
  IO.println(aviso(estado));
  IO.println("ALTA pesa " + Severidad.ALTA.peso + " y urgente = " + Severidad.ALTA.urgente());
  IO.println("BAJA urgente = " + Severidad.BAJA.urgente());
}
```

```text
$ java fig03_06.java
LENTO
name():    LENTO
ordinal(): 1
== :       true
valueOf:   CAIDO
values():  [ARRIBA, LENTO, CAIDO]
revisar pronto
ALTA pesa 10 y urgente = true
BAJA urgente = false
```

The first line, `enum Estado { ARRIBA, LENTO, CAIDO }`, declares the type with its three constants. The constants are written in capital letters, without an accent (the course fixes it that way for the `revisor`'s: `CAIDO`, not `CAÍDO`), and are used as `Estado.LENTO`. What happens in `main`:

- Printing a value of the *enum* writes its name: `LENTO`. `name()` returns that same name as a text.
- `ordinal()` says in which position it was declared (`ARRIBA` is 0, `LENTO` is 1, `CAIDO` is 2). It is rarely used, and when it is, it is almost always a symptom: if you depend on the order, changing the list breaks the program.
- **`==` is the correct comparison between values of an *enum*.** Each constant exists only once in the whole program, so asking whether two variables point to the same one is exactly asking whether they are worth the same. With *enums*, `==` is just what you want.
- `valueOf("CAIDO")` converts a text into the value (and throws an error if the text is not the name of any), and `values()` returns an array with all of them, in the order they were declared.

An *enum* can have **its own data and methods**. `Severidad` (“severity”) has a field `peso` (“weight”), a constructor that receives it, and a method `urgente()`; each constant carries its datum in parentheses: `BAJA(1)`, `MEDIA(5)`, `ALTA(10)`. It serves so that the information that belongs to each value lives **next to the value**, and not in a separate `switch`.

The best reason to use an *enum* is seen in `aviso` (“notice”): it is a `switch` with arrows, like those of the previous lesson, **on an *enum*** and without `default`. The compiler knows how many values `Estado` has, and if the `switch` does not cover them all, it rejects it. Look at `fig03_07.java`, where we remove the `CAIDO` case:

```java
// fig03_07.java
enum Estado { ARRIBA, LENTO, CAIDO }

String aviso(Estado estado) {
  return switch (estado) {
    case ARRIBA -> "todo bien";
    case LENTO -> "revisar pronto";
  };
}

void main() {
  IO.println(aviso(Estado.CAIDO));
}
```

```text
$ java fig03_07.java
fig03_07.java:5: error: the switch expression does not cover all possible input values
  return switch (estado) {
         ^
1 error
error: compilation failed
```

It is the same message as in the previous lesson (“the `switch` does not cover all possible values”), but with an enormous difference. With texts, the compiler demanded a `default` because there are infinitely many texts. With an *enum* **you do not need `default`**: the compiler knows there are three, and if someone adds a fourth status to the *enum* in the future, every `switch` that does not cover it stops compiling. Errors of “I missed a case” go from being run-time errors to compile errors. It is one of the best pieces of news in the lesson, and the second part takes it to another level.

That ends the first session. Before going on, check that you can answer without looking: which four things does a `record` give you without writing them? Why is the `Ruta` of `fig03_05.java` not immutable? Why does a `switch` on an *enum* not need `default`?


### 3.3 Sealed interfaces

#### 3.3.1 An interface: a type with several forms

A service can respond or not respond, and each case needs **different data**: if it responded, it has a code and a time; if not, it has a reason. A `record` only describes one form. To say “a value can be of one form or another” you need a type that brings the two together, and in Java that type is an **interface**.

An interface is a type that declares **what can be done** without saying how; any class or *record* that declares `implements NameOfTheInterface` is, at the same time, a value of that type. A variable of type `Respuesta` (“response”) can hold anything that implements `Respuesta`. It is the tool Java has always offered for “it is one of several”, and it has a defect: **anyone can add a new form** by writing another type that says `implements Respuesta`, in any file, and the compiler has no way of knowing how many there are.

For the `revisor` that is a problem. The classification needs to decide what to do with each form of response, and it wants the compiler to guarantee that **none was left out**. That guarantee is the job of **sealed** interfaces.

#### 3.3.2 `sealed` and `permits`

A sealed interface is an interface that lists, in advance, **all the types that may implement it**. It is written with the word `sealed` and a `permits` clause. Look at `fig03_08.java`, which defines the `revisor`'s `Respuesta`:

```java
// fig03_08.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio r -> "respondió " + r.codigo() + " en " + r.tiempoMs() + " ms";
    case Respuesta.SinRespuesta s -> "no respondió: " + s.motivo();
  };
}

void main() {
  Respuesta uno = new Respuesta.Respondio(200, 142);
  Respuesta otro = new Respuesta.SinRespuesta("sin conexión");
  IO.println(describir(uno));
  IO.println(describir(otro));
}
```

```text
$ java fig03_08.java
respondió 200 en 142 ms
no respondió: sin conexión
```

Read the first line: `sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta`. It says: “`Respuesta` is an interface; only these two forms are permitted”. Inside, between the interface's braces, the two forms are defined as **nested** *records*: `Respondio(int codigo, long tiempoMs)` (“responded”) and `SinRespuesta(String motivo)` (“no response”, with a reason), each with `implements Respuesta`. A type defined inside another is named with its prefix, hence the names `Respuesta.Respondio` and `Respuesta.SinRespuesta` in the rest of the program. (In a multi-file project without a package, you cannot write an `import` to shorten them; you will use the full name.)

Now look at what the model expresses. **A service that responded** has a code and a time, always both. **One that did not respond** has a reason, and has no code or time, not even a `null`: those data simply do not exist in that form. The impossible state of Lesson 2 (code without time, or the other way around) is gone. It is a TypeScript discriminated union, a Rust `enum` with data, a Haskell sum type: the same idea with Java's syntax.

The permitted forms have to be `final`, `sealed` or `non-sealed`; *records* are `final` automatically, so nothing needs to be written here. And they have to live in the same package (in our case, the unnamed package in which the whole project sits) or module. If someone tries to add a form that is not in the list, the compiler stops them. It is `fig03_09.java`, which adds `Redirigida` (“redirected”) outside the `permits`:

```java
// fig03_09.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

record Redirigida(int codigo, String destino) implements Respuesta {}

void main() {
  Respuesta respuesta = new Redirigida(301, "https://catalogo.example/nuevo");
  IO.println(respuesta);
}
```

```text
$ java fig03_09.java
fig03_09.java:8: error: class is not allowed to extend sealed class: Respuesta (as it is not listed in its 'permits' clause)
record Redirigida(int codigo, String destino) implements Respuesta {}
^
1 error
error: compilation failed
```

The message is clear: “the class is not allowed to extend sealed class: `Respuesta` (as it is not listed in its `permits` clause)”. To add a new form you have to **edit the interface**, and that edit is what activates the compiler's warnings in the rest of the program. In 3.4 you will see which.

A detail of `fig03_08.java`: the `describir` `switch` has two cases, `case Respuesta.Respondio r ->` and `case Respuesta.SinRespuesta s ->`, and has no `default`. That is what the next section explains.

### 3.4 `switch` with patterns

#### 3.4.1 Type patterns and exhaustiveness

In Lesson 2 the `switch` compared a value against constants (`case "catalogo"`). Since Java 21, a `switch` can compare against **patterns**: instead of “is it worth this?”, it asks “does it have this shape?”. The simplest pattern is the **type pattern**: `case Respuesta.Respondio r` means “if the response is a `Respondio`, call it `r` inside this case”. Inside, `r` already has the precise type, so you can write `r.codigo()` and `r.tiempoMs()` without any cast. The feature was standardized in [JEP 441](https://openjdk.org/jeps/441).

What matters is what the compiler does with a `switch` on a sealed type: it **knows how many forms there are**. If the cases cover all of them, the `switch` is **exhaustive** and needs no `default`. If one is missing, it does not compile. Look at `fig03_10.java`, which illustrates it by removing the `SinRespuesta` form:

```java
// fig03_10.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio r -> "respondió " + r.codigo();
  };
}

void main() {
  IO.println(describir(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_10.java
fig03_10.java:9: error: the switch expression does not cover all possible input values
  return switch (respuesta) {
         ^
1 error
error: compilation failed
```

The message is the same as with the *enum* in 3.2: “the `switch` does not cover all possible values”. It is the same guarantee, now for forms with data.

And the opposite is an equally real risk. A `default` in a `switch` on a sealed type switches that guarantee off. Look at `fig03_11.java`. A third form, `Redirigida`, was added, and the `clasificar` `switch` has a `default`:

```java
// fig03_11.java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.Respondio _ -> "ARRIBA";
    default -> "CAIDO";
  };
}

void main() {
  Respuesta redirigida = new Respuesta.Redirigida(301, "https://catalogo.example/nuevo");
  IO.println(clasificar(redirigida));
}
```

```text
$ java fig03_11.java
CAIDO
```

The program compiles, runs and says that a redirect is `CAIDO`. Nobody decided that: the `default` swallowed the new form and answered for all those it does not mention. It is a silent error, the worst kind, and the compiler could not warn because the `default` told it “I already covered everything”. **With a sealed type, or with an *enum*, do not write `default`.** If you leave it out, each new form breaks compilation in every `switch` that must consider it, which is exactly what you want.

#### 3.4.2 Guards, record patterns and `_`

The `revisor` needs more than distinguishing between “responded” and “did not respond”: within “responded” there are three cases (error code, slow, normal). Patterns have two tools for that.

The first is the **guard** `when`: an additional condition the case must meet. `case Respuesta.Respondio r when r.codigo() >= 400` enters only if the response is a `Respondio` **and** its code is also 400 or more. It is the `&&` from the previous lesson, applied to the pattern.

The second is the **record pattern**: instead of naming the object and then asking it for its components, the pattern **takes them apart** in the same case. `case Respuesta.Respondio(int codigo, long tiempoMs)` means “if it is a `Respondio`, pull out its code into the variable `codigo` and its time into `tiempoMs`”. It is defined in [JEP 440](https://openjdk.org/jeps/440).

And a comfort detail: if you do not need a component's value, you replace it with the **unnamed pattern** `_` (final since Java 22, [JEP 456](https://openjdk.org/jeps/456)). It says “there is something here, and I don't care”. Look at `fig03_12.java`, which implements the `revisor`'s classification from end to end:

```java
// fig03_12.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

static final long UMBRAL_LENTO_MS = 1000;

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS -> "LENTO";
    case Respuesta.Respondio _ -> "ARRIBA";
    case Respuesta.SinRespuesta _ -> "CAIDO";
  };
}

void main() {
  IO.println(clasificar(new Respuesta.Respondio(200, 142)));
  IO.println(clasificar(new Respuesta.Respondio(200, 2300)));
  IO.println(clasificar(new Respuesta.Respondio(503, 95)));
  IO.println(clasificar(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_12.java
ARRIBA
LENTO
CAIDO
CAIDO
```

Four cases, in this order, each one a rule of the `revisor`:

1. `Respondio(int codigo, long _) when codigo >= 400` is rule 2: responded with an error. It uses the code and does not care about the time.
2. `Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS` is rule 3: responded and took long. Now it uses the time and not the code.
3. `Respondio _` is rule 4, the one left over: any other response is ARRIBA. With `_` instead of a name, the variable is not even given a name, because it is not used.
4. `SinRespuesta _` is rule 1: no response, CAIDO.

The cases are evaluated **from top to bottom, and the first one that matches wins**. That is why order matters, and the compiler looks after you against an impossible order. Look at `fig03_13.java`, where the general case `Respondio _` was written before the specific one:

```java
// fig03_13.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio _ -> "ARRIBA";
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.SinRespuesta _ -> "CAIDO";
  };
}

void main() {
  IO.println(clasificar(new Respuesta.Respondio(503, 95)));
}
```

```text
$ java fig03_13.java
fig03_13.java:11: error: this case label is dominated by a preceding case label
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
         ^
1 error
error: compilation failed
```

“This case is dominated by a preceding case label.” The first case already catches **all** the `Respondio`s, so the ones that come after, with a guard, could never run. Java flags it as an error, not as a warning. The rule: **from the most specific to the most general**, and the unguarded case of a type goes after those that do have a guard.

Before going on, a contrast with Lesson 2. There `clasificar` received an `Integer` and a `Long` that could be null, and used `&&` so as not to unbox an emptiness. Here `clasificar` receives **one** `Respuesta`, and its possible forms already say which data there is. There are no nulls to check, because there is no way to ask the code of a response that does not have one. The check that you used to do by hand, the compiler now does.

#### 3.4.3 `null` and `instanceof`

There are two more things worth knowing, because they appear in the code you will read.

**A `switch` on a null reference blows up**, unless you prevent it. A `case null` captures the case, and if you omit it, the `switch` throws a `NullPointerException` before looking at any other case. Look at `fig03_14.java`:

```java
// fig03_14.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case null -> "no hay respuesta registrada";
    case Respuesta.Respondio(int codigo, long _) -> "respondió " + codigo;
    case Respuesta.SinRespuesta(String motivo) -> "no respondió: " + motivo;
  };
}

String sinCasoNulo(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio _ -> "respondió";
    case Respuesta.SinRespuesta _ -> "no respondió";
  };
}

void main() {
  IO.println(describir(new Respuesta.Respondio(200, 142)));
  IO.println(describir(null));
  IO.println(sinCasoNulo(null));
}
```

```text
$ java fig03_14.java
respondió 200
no hay respuesta registrada
Exception in thread "main" java.lang.NullPointerException
	at java.base/java.util.Objects.requireNonNull(Objects.java:220)
	at fig03_14.sinCasoNulo(fig03_14.java:17)
	at fig03_14.main(fig03_14.java:26)
```

`describir` includes `case null`, and so it answers `null` with a text. `sinCasoNulo` (“without null case”) does not include it, and the same `null` ends in the `NullPointerException` seen at the end. The trace has a line `at java.base/java.util.Objects.requireNonNull(Objects.java:220)`: the number 220 is a line of the JDK itself and may be another in your version. What matters is to read the next two lines, which are yours: `sinCasoNulo` on line 17, called from `main` on 26. In the `revisor` no nulls reach a `switch`, because the sealed type makes it impossible for them to exist; but it is good to know what happens when they do.

**`instanceof` with a pattern** is the `switch`'s sibling for a single question. `respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400` asks “is it a `Respondio`?”, calls it `r` if it is, and continues with the condition. Look at `fig03_15.java`:

```java
// fig03_15.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

boolean esError(Respuesta respuesta) {
  return respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400;
}

void main() {
  IO.println(esError(new Respuesta.Respondio(503, 95)));
  IO.println(esError(new Respuesta.Respondio(200, 142)));
  IO.println(esError(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_15.java
true
false
false
```

It is an expression that gives `true` or `false`. Notice how the `&&` acts as a short circuit: `r` only exists, and `r.codigo()` is only evaluated, if the `instanceof` was true. Use it when you have a single question about a type; if there are two or more forms to handle, an exhaustive `switch` is better, because the compiler looks after the cases that are missing.

### 3.5 The project step: the `revisor` in several files

Now you organize the `revisor` with everything you learned. There is a change of form, and one of content.

**The form.** The one-piece `Revisor.java` is split into seven files, **all in the same folder** `~/curso-java/revisor/`, with no subfolder. Natural question: how do you run something with several files without Maven? With the same command as always, `java Revisor.java`. Since Java 22, the launcher compiles the file you give it and looks in the same folder for the other `.java` files that one needs ([JEP 458](https://openjdk.org/jeps/458)). The entry file is still compact, with `void main()`; the others are normal types, each in the file that carries its name. It is what makes a project with several pieces possible **before** talking about Maven, which arrives in Lesson 6.

**The content.** Four changes are made, and it is worth listing them because they are the lesson in miniature:

1. The status `String` becomes an `enum`.
2. The two `null`s of “no response” become a sealed interface with two forms.
3. A service's data are brought together in a `record`.
4. The two parallel `switch`es (code and time) become a **single** one, which returns the complete form.

The pieces, one by one:

**`Estado.java`** is the *enum* from 3.2, with the three constants. Google's style guide writes the constants one per line.

```java
// fig03_16/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

**`Respuesta.java`** is the sealed interface from 3.3, as is.

```java
// fig03_16/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

**`Servicio.java`** is the *record* from 3.1: the name, the address and the timeout, with the additional constructor that fills in the default value. It is the first time you see a real `import`: a normal file does not import anything by itself, so `URI` is brought in with one line.

```java
// fig03_16/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

**`Resultado.java`** (“result”) gathers what the `revisor` knows about a service already checked: which one it is, what `Estado` it ended in and what its `Respuesta` was. It has one method, `detalle()`, that uses a `switch` with record patterns: if it responded, the detail is the code as text; if not, it is the reason. Without `default`, and without nulls.

```java
// fig03_16/Resultado.java
record Resultado(Servicio servicio, Estado estado, Respuesta respuesta) {
  String detalle() {
    return switch (respuesta) {
      case Respuesta.Respondio(int codigo, long _) -> String.valueOf(codigo);
      case Respuesta.SinRespuesta(String motivo) -> motivo;
    };
  }
}
```

**`Clasificador.java`** (“classifier”) contains the classification from 3.4.2. It is a `final` class (it cannot be extended) whose only job is to group a function and the constant it uses. `static` methods and constants belong to the class and not to an object, and are called with the class name in front: `Clasificador.clasificar(respuesta)`. (Lesson 6 explains `private`, the word that closes that door so nobody creates a `Clasificador` that is of no use.) You can see that `clasificar` now **returns an `Estado`**, and not a `String`.

```java
// fig03_16/Clasificador.java
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

**`Formato.java`** (“format”) gathers what does the formatting. There is an important change of signature compared with the previous lesson: `formatearTiempo` now receives a `long`, not a `Long`. It no longer has to decide what to do with a null, because the “no time” case does not reach it. The one who decides is the function `tiempo(Respuesta)`, with an exhaustive `switch`: if there is a time it formats it, and if not, it writes the dash. The type did the work that the `if (tiempoMs == null)` check used to do. The column template, which used to be written twice (in `linea` and in `encabezado`), is now the constant `PATRON_LINEA`. `import java.util.Locale;` is used because the file is not compact.

```java
// fig03_16/Formato.java
import java.util.Locale;

final class Formato {
  static final String PATRON_LINEA = "%-12s%-10s%6s  %s";

  static String formatearTiempo(long tiempoMs) {
    if (tiempoMs < Clasificador.UMBRAL_LENTO_MS) {
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
}
```

**`Revisor.java`** is the only compact source file, and the entry point. `simular` (“simulate”) is the **single `switch`** that replaces `codigoDe` and `tiempoDe`: it returns a `Respondio` with the code and the time together, or a `SinRespuesta` for any service it does not know. `revisar` receives a `Servicio`, simulates the response, classifies it and returns the `Resultado`. `main` creates the four services with `URI.create(...)` and prints them. (The four written one by one, because there are no lists yet; Lesson 4 fixes that.)

```java
// fig03_16/Revisor.java
Respuesta simular(String nombre) {
  return switch (nombre) {
    case "catalogo" -> new Respuesta.Respondio(200, 142);
    case "inventario" -> new Respuesta.Respondio(200, 2300);
    case "pagos" -> new Respuesta.Respondio(200, 87);
    default -> new Respuesta.SinRespuesta("sin conexión");
  };
}

Resultado revisar(Servicio servicio) {
  Respuesta respuesta = simular(servicio.nombre());
  return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
}

void main() {
  Servicio catalogo = new Servicio("catalogo", URI.create("https://catalogo.example"));
  Servicio inventario = new Servicio("inventario", URI.create("https://inventario.example"));
  Servicio pagos = new Servicio("pagos", URI.create("https://pagos.example"));
  Servicio reportes = new Servicio("reportes", URI.create("https://reportes.example"));

  IO.println(Formato.encabezado());
  IO.println(Formato.linea(revisar(catalogo)));
  IO.println(Formato.linea(revisar(inventario)));
  IO.println(Formato.linea(revisar(pagos)));
  IO.println(Formato.linea(revisar(reportes)));
}
```

To run it:

```text
$ cd fig03_16
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

It is **character for character the same table** as at the end of Lesson 2, with a difference that cannot be seen and that matters: now, for `reportes` to come out the way it did, nobody wrote a `null`. The `SinRespuesta` type is what makes it have no time, its detail be the reason and its status be `CAIDO`.

The good thing about the design shows when you change it. Exercise 4 of this lesson is that.

## The error you will see

The lesson has one main message and four supporting ones.

**The main error: “the switch expression does not cover all possible input values”.** It is the one you see when a `switch` on an `enum` or a sealed interface is missing a case, and it is **the same text in both cases**. It appears in `fig03_07.java` (the *enum*) and in `fig03_10.java` (the sealed interface):

```text
fig03_10.java:9: error: the switch expression does not cover all possible input values
  return switch (respuesta) {
         ^
```

It is the sign that the compiler **is helping you**: you removed a case, or added a form, and there is a place you must review. Do not silence it with a `default`. Read the line the `^` points to and decide, for that form, what you want to happen.

**The four supporting messages:**

- `error: cannot assign a value to final variable tiempoMs`: you tried to change a component of a *record*. It cannot be done. If you need another value, create another *record* (`fig03_02.java`).
- `error: class is not allowed to extend sealed class: Respuesta (as it is not listed in its 'permits' clause)`: you wanted to implement a sealed interface with a type that is not in the list. Add it to the `permits` (`fig03_09.java`).
- `error: this case label is dominated by a preceding case label`: a general case is before a specific one. Reorder from the most specific to the most general (`fig03_13.java`).
- `IllegalArgumentException` with your message, from a compact constructor: someone tried to create a *record* with data that your constructor rejects. The trace says who tried (`fig03_04.java`).

## What gets done wrong

**A `default` in a `switch` on a sealed type or an *enum*.** It silences the check that justifies having written them, as `fig03_11.java` showed: the program compiled and misclassified a redirect. If the new form deserves the same treatment as another, write its case separately (or join the two with a comma).

**A `record` with an array, or with any object that can be modified.** It looks immutable and is not: it is changed through the accessor and through the original reference, and its `equals` compares arrays by identity. In Lesson 4 you will see `List.copyOf`.

**A `String` where an `enum` fits.** A status, a type, a level. Every value that comes from a fixed list should be an *enum*, so that a typo is not a new status.

**A `null` where a form fits.** Two parallel `null`s (code, time) allow impossible states. If “there is no datum” is a possibility with its own meaning, give it its own form in a sealed type, with the data it does have (here, the reason).

**Using `ordinal()` for anything other than debugging.** A constant's number changes if someone reorders the *enum*.

**Comparing `enum`s with `equals`.** It works, but `==` is the idiomatic one, and it avoids a `NullPointerException` if the variable is null (`estado.equals(...)` blows up with `null`, `estado == ...` does not).

**A general case before a specific one.** The compiler detects it, but it is an error worth understanding and not just fixing: the order of the cases is part of the meaning.

**Putting long validations in the compact constructor.** It is the place to normalize and to reject the clearly absurd. Business logic goes elsewhere.

## Exercises

### Exercise 1 — What equality a `record` gives you

Declare `record Servicio(String nombre, URI url, int timeoutMs) {}` in a compact source file and create three objects: `a` and `b` with the same values (5000 timeout) and `c` the same as them but with 1000. **Before running**, predict the result of `a == b`, `a.equals(b)`, `a.equals(c)`, `a.hashCode() == b.hashCode()` and of printing `c`. Run and compare.

### Exercise 2 — Closing a door of immutability

Take `fig03_05.java` and add to `Ruta` a compact constructor that stores a **copy** of the array (arrays have a `clone()` method that returns a copy). Run again. Which of the two changes stopped affecting the *record*? Which one still happens? What do you need to close the other door?

### Exercise 3 — A new status in the `enum`

In the `enum` of `fig03_06.java`, add a fourth value, `DEGRADADO`, between `LENTO` and `CAIDO`, and touch nothing else. Try to run. What does the compiler say and in which function? Then fix it with whatever message seems reasonable to you for the new status, and write in your logbook what would have happened if `Estado` had been a `String`.

### Exercise 4 — The project: one more form of response

In the 3.5 project, add a third form to `Respuesta`: `record Redirigida(int codigo, String destino)`, which represents a service that answered with a 3xx code and says where to send you. Do it in `Respuesta.java` (the form and the `permits`) and **touch nothing else**. Run `java Revisor.java`. Write down **each place** the compiler forces you to fix, one by one: fix them and run again until it compiles. Decide what you want to happen with a redirect: which status corresponds to it, which time does it show, which detail?

## Solutions

### Solution 1

```text
false
true
false
true
Servicio[nombre=pagos, url=https://pagos.example, timeoutMs=1000]
```

`a == b` is `false`: they are two different objects. `a.equals(b)` is `true`: the *record*'s `equals` compares component by component (and `URI`'s `equals`, in turn, compares content). `a.equals(c)` is `false`: they differ in `timeoutMs`. The `hashCode`s of `a` and `b` match because `equals` declares them equal, and the *record* guarantees that consistency. And `c` is printed with the usual format.

### Solution 2

The compact constructor that stores the copy:

```java
record Ruta(String nombre, String[] etapas) {
  Ruta {
    etapas = etapas.clone();
  }
}
```

With it, the output is:

```text
tras cambiar el original: [conectar, esperar]
tras cambiar por el accesor: [conectar, CAMBIADO DESDE EL ACCESOR]
```

The first hole closed: changing the original array no longer affects the *record*, because it stores its own copy. The second is still open: the accessor returns the reference to the copy it stores, and changing it from outside modifies it. To close it you would have to override the accessor so that it returns another copy; in a *record*, a hand-written accessor has to be `public` (Lesson 6 explains what that means), and even so `equals` would still compare arrays by identity. The practical conclusion is that **an array is a bad component**. In the next lesson you will see `List.copyOf`, which does close both doors.

### Solution 3

The compiler rejects the program in `aviso`, the function with the `switch`:

```text
error: the switch expression does not cover all possible input values
  return switch (estado) {
         ^
```

The `DEGRADADO` case is missing. It is fixed by adding `case DEGRADADO -> "revisar ya";` (or whatever message you choose). With a `String`, nothing would have warned: `"DEGRADADO"` would be just one more text, `aviso` would fall into the `default` (which would have been mandatory) and the degraded service would receive the notice for the unknowns. The *enum* turned that oversight into a compile error.

### Solution 4

With the new form added only in `Respuesta.java`:

```java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}
```

the compiler first complains about `Clasificador.java` (the full path of your folder appears in front of the file name; here I shorten it):

```text
Clasificador.java:6: error: the switch expression does not cover all possible input values
    return switch (respuesta) {
           ^
```

A case is added (for example, `case Respuesta.Redirigida _ -> Estado.ARRIBA;`: a redirect is a valid response), and when running again, the next one is `Formato.java:15` (the `tiempo` function), and then `Resultado.java:4` (the `detalle` method). **There are three places**: the classification, the time shown and the detail. In my run the compiler showed them one at a time, in that order. You can decide differently what each does: an `ARRIBA` status is reasonable because the service answered, the time can be the dash (we have none in the form) and the detail can be the code with the destination. What matters is not the decision, but that the compiler **forced you to make it** at each site. With two parallel `null`s, nothing would have warned you.

## How I know I got it

- [ ] `java fig03_01.java` prints `Punto[nombre=catalogo, tiempoMs=142]` and you can explain why `a == b` is `false` and `a.equals(b)` is `true`.
- [ ] `java fig03_05.java` shows the two modifications to the `Ruta`, and you can say why they happen.
- [ ] In `fig03_10.java` you see the error “does not cover all possible input values”, and you know which case was missing.
- [ ] In `fig03_11.java` you see that the `default` classifies a redirect as `CAIDO`, and you can say why it is worse than the previous error.
- [ ] From `fig03_16/`, `java Revisor.java` prints exactly the four-row table from Lesson 2: `catalogo` ARRIBA 142ms, `inventario` LENTO 2.3s, `pagos` ARRIBA 87ms, `reportes` CAIDO — “sin conexión” (“no connection”).
- [ ] After adding `Redirigida`, the compiler showed you three places and you fixed them one by one.
- [ ] You can explain in your own words why this lesson's `revisor` does not have a single `null`.
- [ ] You wrote in your logbook the impossible state that the sealed type kept you from writing, and what happened when you added a new case.

## Summary

Answer without looking at the lesson:

1. Which four things does the compiler generate for a `record`? What does its `equals` compare?
2. What is the difference between a compact constructor and an additional one, and what is each for?
3. Why is a `record` with an array inside not immutable? What is done in Lesson 4 to fix it?
4. Why is an `enum` compared with `==`? What advantage does writing a `switch` on an `enum` without `default` give?
5. What does `sealed ... permits` say and what happens if you try to implement the interface with a type that is not in the list?
6. What is a `when` guard, a record pattern and the `_` pattern? Give an example of each with the `revisor`.
7. Why does the order of the cases matter? What happens if you put the general case first?
8. What does a `switch` on a null reference do if it has no `case null`?
9. What changed in the signature of `formatearTiempo` between Lesson 2 and 3, and what made it possible?
10. Why is a `default` in a `switch` on a sealed type a risk?

## Further reading

- [The Java Tutorials, “Record Classes”](https://docs.oracle.com/en/java/javase/25/language/records.html): *records* explained with more examples, from the official documentation. Consulted on October 7, 2026.
- [JEP 409, “Sealed Classes”](https://openjdk.org/jeps/409): the why and the complete rules of `sealed`, `permits`, `final` and `non-sealed`. Consulted on October 7, 2026.
- [JEP 441, “Pattern Matching for switch”](https://openjdk.org/jeps/441) and [JEP 440, “Record Patterns”](https://openjdk.org/jeps/440): the exact rules of exhaustiveness, dominance and guards. Consulted on October 7, 2026.
- [The Java Tutorials, “Enum Types”](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html): *enums* with fields, constructors and methods. Consulted on October 7, 2026.

---

**Previous:** [Lesson 2 — Types, strings and control flow](02-tipos-cadenas-control.md) ·
**Next:** [Lesson 4 — Collections, generics and streams](04-colecciones-genericos-streams.md)
