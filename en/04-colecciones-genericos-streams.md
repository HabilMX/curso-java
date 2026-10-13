# Lesson 4 — Collections, generics and streams

**Time:** 2 × 90 min (two sessions; the break is between sections 4.2 and 4.3)

**What you build:** the `revisor`'s list of services and its report: now the program keeps its services in a collection, summarizes the results with a *stream* and delivers them in a table with its summary line

**What you learn:** `List`, `Set` and `Map`; immutable collections and defensive copies; generics and their erasure; lambdas and *streams* with `Collectors`; `Comparator`; and `Optional` as a return value

**Where you are coming from.** You bring the project from [Lesson 3](03-records-enums-sellados.md): seven files that keep a clear deal. `Estado` is an `enum` with three values; `Respuesta` is a sealed interface that can only be `Respondio` (with its code and its time) or `SinRespuesta` (with its reason); `Servicio` and `Resultado` are *records*; `Clasificador` decides the status with an exhaustive `switch`; `Formato` assembles the rows; and `Revisor.java` writes the four services by hand and checks them one by one. The table comes out fine, but it has three gaps. There is no list: the services are four loose variables. There is no summary: no line says how many are up. And the simulated data lives inside a `switch`, mixed with the logic. This lesson fills the three gaps with the tools Java provides for storing and transforming many values. You work in your `~/curso-java/revisor/` folder and run everything with `java`.

**What this lesson does not do.** It does not read files or catch errors: the list of services is still written in `Revisor.java`, and removing it from there is the work of [Lesson 5](05-errores-excepciones.md). Nor does it query any network: the `Simulador` (“simulator”) you write today plays the network's role until lesson 8. And it does not use Maven: the files are still loose in a folder, as in lesson 3.

**About the runs.** To run a figure, go into the lesson's folder in the repository (`programas/04-colecciones-genericos-streams/`) and run `java fig04_01.java`, with the number of the figure you want. The ones that live in their own folder (the project of section 4.5) are run from inside it. All the outputs were obtained with Java 25 (Temurin 25.0.4) and the terminal's language in UTF-8; on Linux Mint that is the norm; if you see question marks where accents go, `echo $LANG` tells you which language your terminal has. Error traces carry lines of the JDK itself (`at java.base/...`) with line numbers that change from one update to another: it is normal for yours to differ in those numbers.

## By the end you will be able to

- Choose among `List`, `Set` and `Map` according to what you need to store, and say what traversal order each implementation has.
- Create an immutable collection with `List.of`, recognize the `UnsupportedOperationException` that comes out when modifying it and protect a *record* with a defensive copy.
- Explain what generics are, what the compiler guarantees with them and what is lost with type erasure.
- Write a lambda and a method reference, and build with them a *stream* of `filter`, `map` and `collect`.
- Summarize a list of results with `Collectors.groupingBy`, `counting` and `average`, and sort it with a `Comparator`.
- Return an `Optional` instead of `null` and read it without blindly calling `get()`.

## The why before the how

Open your `Revisor.java` from lesson 3. The four services are written as four separate calls to `revisar`, and that is where what the program knows about them ends: it cannot say how many there are, cannot traverse them in a loop, cannot add one without writing another line. Now imagine you are asked for the line that closes the report: “4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms” (“4 services · 2 up · 1 slow · 1 down · average 843ms”). To produce it with what you have until today you would need four counters (`arriba`, `lento`, `caido`, `total`), a sum, a divisor, and to update everything every time a service changes status. Each new service, and each new status, would touch several places in the program.

All languages solved this the same way: with **collections**, structures that hold many values and know how to traverse, search and count themselves. If you come from Python you know the list, the set and the dictionary; if you come from Go, the *slice*, the map and the habit of using a `map[string]struct{}` as a set; if you come from TypeScript, the array, the `Set` and the `Map`. Java has all three, and organizes them in a way that is less like the other languages than it seems: they are **interfaces** (`List`, `Set`, `Map`) with **several interchangeable implementations**, and that separation explains almost everything you will see today, from why there are three different maps to why immutable collections exist.

There is one more problem. A collection stores values, but of what type? If it stores anything, the compiler cannot help you: you would have to verify by hand that what you take out of the list is the service you expected. Java solves this with **generics**: you declare that the list is of `Servicio` and the compiler prevents anything else from going in. It is one of the most useful ideas in the language and it has a detail that surprises almost everyone arriving from elsewhere: generics exist only at compile time (they are “erased” at run time), and lesson 0 already anticipated it.

And there remains the question of how to transform one collection into another: filtering the slow ones, converting results into text rows, counting by status. Java does it with **streams**, a way of describing the transformation as a chain of steps. And since not every service has a time (the one that is down did not respond), it is also necessary to decide how “there is no value” is represented without going back to the `null`s of lesson 2: that is what `Optional` is for.

The path has two sessions. The **first** covers sections 4.1 and 4.2: collections, immutable ones and generics; when you finish it you know how to store and protect the `revisor`'s services. The **second** covers 4.3, 4.4 and 4.5: *streams*, `Optional` and the complete report; when you finish it the program prints the table with its summary. This is the destination, so you have the arrival point in view:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

You already knew the table; what is new is the line at the end, and that now it comes from a real list and not from four calls written by hand.

## The concepts

### 4.1 Collections

#### 4.1.1 A list: `List` and `ArrayList`

A **list** is an ordered sequence of values where each one has a position, which in Java starts at 0. In Java, `List` is not a class but an **interface**: a contract that says what can be done with a list (add, search, remove, traverse) without saying how it is stored inside. The implementation you will use almost always is called `ArrayList`, which stores the values in an internal array that grows when it fills up. The usage pattern is to declare the variable with the interface and create the object with the implementation:

```java
List<String> nombres = new ArrayList<>();
```

That line has three pieces worth separating. `List<String>` is the type of the variable: a list of texts (what goes between the `<` and `>` signs is the type of its elements; section 4.2 explains why). `new ArrayList<>()` creates an empty list of the concrete class. And the empty `<>` is called the **diamond**: it tells the compiler “the type of the elements is the same one I already declared on the left, don't make me repeat it”. Why declare the variable as `List` and not as `ArrayList`? Because the rest of the program only needs the contract. If tomorrow you change the implementation to another, you change one word on one line and nothing else. It is the same reason why in lesson 3 `Respuesta` was an interface and not a class.

See everything that can be done with a list in the program `fig04_01`. Before reading it, a clarification: it is a compact source file, like those of lesson 2, and so it does not need to write an `import` for `List` or `ArrayList`. Compact source files automatically import the whole `java.base` module, which includes `java.util`, where the collections live. When a normal class needs them, as happens in section 4.5, it will import them with a line at the beginning of the file.

```java
// fig04_01.java
void main() {
  List<String> nombres = new ArrayList<>();
  nombres.add("catalogo");
  nombres.add("inventario");
  nombres.add("pagos");
  IO.println(nombres);
  IO.println("tamaño: " + nombres.size());
  IO.println("el segundo: " + nombres.get(1));
  IO.println("¿está pagos? " + nombres.contains("pagos"));
  IO.println("posición de pagos: " + nombres.indexOf("pagos"));
  IO.println("primero y último: " + nombres.getFirst() + ", " + nombres.getLast());

  nombres.add(1, "reportes");
  nombres.remove("pagos");
  nombres.set(0, "catalogo-v2");
  IO.println(nombres);

  for (String nombre : nombres) {
    IO.println("servicio: " + nombre);
  }
  for (int i = 0; i < nombres.size(); i++) {
    IO.println(i + " -> " + nombres.get(i));
  }
}
```

Run the program and compare with what you predicted:

```text
$ java fig04_01.java
[catalogo, inventario, pagos]
tamaño: 3
el segundo: inventario
¿está pagos? true
posición de pagos: 2
primero y último: catalogo, pagos
[catalogo-v2, reportes, inventario]
servicio: catalogo-v2
servicio: reportes
servicio: inventario
0 -> catalogo-v2
1 -> reportes
2 -> inventario
```

Three details of that output. First, `println` knows how to print a whole list: it writes it between square brackets with the elements separated by commas, because `ArrayList` defines its own representation as text. Second, `remove("pagos")` removed the element whose value is “pagos”; with texts there is no possible doubt, but `remove` has two versions (one that receives a value and one that receives a position) and with lists of numbers they get confused. The section “What gets done wrong” shows the trap. Third, `getFirst()` and `getLast()` are relatively new: they arrived in Java 21 with sequenced collections ([JEP 431](https://openjdk.org/jeps/431)) and avoid the old `get(lista.size() - 1)` for the last element.

The two ways of traversing a list in the program deserve explanation, because the course had not used them until today. The **enhanced `for`** (`for (String nombre : nombres)`) reads “for each `nombre` in `nombres`”: on each iteration, `nombre` is worth the next element, and there is no index to keep. It is the usual form. The **classic `for`** (`for (int i = 0; i < nombres.size(); i++)`) keeps the index by hand: it starts at 0 (`int i = 0`), repeats while the condition is true (`i < nombres.size()`) and advances at the end of each iteration (`i++`, which means “add one to `i`”). Use it only when you need the position. If you get it wrong, the list tells you with an exception: asking for `nombres.get(10)` on a three-element list throws `IndexOutOfBoundsException: Index 10 out of bounds for length 3`, and the message carries the index and the size, which is all you need to understand what happened.

One last comparison with the languages you already know, because the question always comes up. Java also has arrays (`String[]`, of fixed size, which serve for few things and which you will see in lesson 5 and in 6 when writing `String[] args`), but in everyday code lists are used: they grow, are searched, are passed to functions and combine with *streams*. What in Go is a *slice* and in Python a `list` is, in Java, a `List<T>`.

#### 4.1.2 Sets and maps

A **set** (`Set`) stores values **without repeating**: adding one that is already there does nothing. A **map** (`Map`) stores key-value **pairs**, and looks up the value by the key. The program `fig04_02` uses both:

```java
// fig04_02.java
void main() {
  Set<String> vistos = new LinkedHashSet<>();
  IO.println(vistos.add("catalogo"));
  IO.println(vistos.add("pagos"));
  IO.println(vistos.add("catalogo"));
  IO.println(vistos + ", tamaño " + vistos.size());

  Map<String, Long> tiempos = new LinkedHashMap<>();
  tiempos.put("catalogo", 142L);
  tiempos.put("inventario", 2300L);
  tiempos.put("pagos", 87L);
  IO.println(tiempos);
  IO.println(tiempos.get("pagos"));
  IO.println(tiempos.get("reportes"));
  IO.println(tiempos.getOrDefault("reportes", 0L));
  IO.println(tiempos.containsKey("reportes"));
  IO.println(tiempos.put("pagos", 90L));
  IO.println(tiempos);

  for (Map.Entry<String, Long> par : tiempos.entrySet()) {
    IO.println(par.getKey() + " tardó " + par.getValue() + " ms");
  }

  Map<String, Long> ordenado = new TreeMap<>(tiempos);
  IO.println(ordenado);
}
```

```text
$ java fig04_02.java
true
true
false
[catalogo, pagos], tamaño 2
{catalogo=142, inventario=2300, pagos=87}
87
null
0
false
87
{catalogo=142, inventario=2300, pagos=90}
catalogo tardó 142 ms
inventario tardó 2300 ms
pagos tardó 90 ms
{catalogo=142, inventario=2300, pagos=90}
```

Read the output calmly, because it has several lessons. In the set, `add` **returns a `boolean`**: `true` if the value went in and `false` if it was already there. That is why the first three lines say `true`, `true`, `false`: it is a cheap way of asking “had I already seen it?”, and you will use it in lesson 5 to detect repeated names in the configuration.

In the map, `put` stores a pair and **returns the previous value** of that key, or `null` if there was none. In the output, `put("pagos", 90L)` printed `87`, the value it had just replaced. `get` looks up by key and returns `null` if the key does not exist: `tiempos.get("reportes")` printed `null`. That `null` is the same source of errors as in lesson 2, and that is why `getOrDefault("reportes", 0L)` exists: it returns the default value you indicate when the key is missing. `containsKey` answers the question “is this key there?” without touching the value.

To traverse a map you traverse the set of its pairs with `entrySet()`; each pair is a `Map.Entry`, of which `getKey()` gives the key and `getValue()` the value. You will see that the type is written long (`Map.Entry<String, Long>`); in real code it is shortened with `var`, which you know from lesson 2: `for (var par : tiempos.entrySet())`.

The question that follows is the most important in the section: **in what order do the elements come out?** The answer depends on the implementation, and that is why Java has several of each interface:

| Implementation | Traversal order | When to use it |
|---|---|---|
| `HashSet`, `HashMap` | none you can promise | when only fast lookup matters |
| `LinkedHashSet`, `LinkedHashMap` | the order in which they were inserted | when the report must always come out the same |
| `TreeSet`, `TreeMap` | sorted by the key | when you need the natural order (alphabetical, numeric) |

The figure used `LinkedHashSet` and `LinkedHashMap` so that the output is always the one you see, and ended by copying the map to a `TreeMap`, which reordered it alphabetically (here it coincides with the insertion order because the names were already in order). For the `revisor` this is not a detail: the course syllabus promises a report “in the same order always”, and a table that comes out in a different order every time you run the program is a useless report. The practical rule is: **if the order matters to whoever reads the result, choose an implementation that guarantees it**, and do not trust that a `HashMap` “seems” ordered on your machine. In fact, the immutable collections you see in the next section (`Set.of`, `Map.of`) have an order that **changes from one run to another** on purpose: I measured the same line, `Set.of("catalogo", "inventario", "pagos", "reportes")`, in three consecutive runs of the same program and it came out `[pagos, reportes, catalogo, inventario]`, then `[catalogo, inventario, pagos, reportes]` and then `[catalogo, reportes, pagos, inventario]`. The documentation warns of it: [“the iteration order of set elements is unspecified and is subject to change”](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Set.html). The order is not part of the contract, and the JDK does not bother to keep it stable.

One question remains: how does a `Set` know that two values are “the same”? It uses two methods: `equals`, which you already know, and `hashCode`, which returns an integer that summarizes the object's content (two equal objects must give the same number, and that lets the set search without comparing with all of them). With texts and numbers it works without you doing anything. With your own types, it depends on how they are defined, and here the *records* of lesson 3 pay off what they promised: they generate `equals` and `hashCode` from all their components. The program `fig04_03` shows it: two `Direccion` (“address”) with the same server and the same port are equal even though they are two different objects, and that is why the set does not repeat and the map finds the value with a freshly built key.

```java
// fig04_03.java
record Direccion(String servidor, int puerto) {}

void main() {
  Set<Direccion> direcciones = new LinkedHashSet<>();
  direcciones.add(new Direccion("catalogo.example", 443));
  direcciones.add(new Direccion("pagos.example", 443));
  direcciones.add(new Direccion("catalogo.example", 443));
  IO.println(direcciones.size());
  IO.println(direcciones);

  Map<Direccion, String> duenos = new HashMap<>();
  duenos.put(new Direccion("pagos.example", 443), "equipo de cobranza");
  IO.println(duenos.get(new Direccion("pagos.example", 443)));
  IO.println(duenos.get(new Direccion("pagos.example", 8443)));
}
```

```text
$ java fig04_03.java
2
[Direccion[servidor=catalogo.example, puerto=443], Direccion[servidor=pagos.example, puerto=443]]
equipo de cobranza
null
```

The set ended up with two elements and not three, the map found the “equipo de cobranza” (“collections team”) with a new key, and the key with another port (`8443`) does not exist, so `get` returned `null`. If instead of a *record* you had written a normal class without defining `equals` and `hashCode`, each object would be equal only to itself, and the set would store all three. That is one of the reasons the course prefers *records* for data.

#### 4.1.3 Immutable collections

Until here the collections could be modified: add, remove, replace. Many times that is exactly what you do not want. The list of services the `revisor` reads should not change while someone else is traversing it, and a report that has already been computed should not be alterable afterward. Java solves this with **immutable collections**, which are created with three methods: `List.of(...)`, `Set.of(...)` and `Map.of(...)`. (They are called static factory methods: you do not need `new`, just the interface's name, a dot and the values.) See what happens when you try to modify one:

```java
// fig04_04.java
void main() {
  List<String> nombres = List.of("catalogo", "inventario", "pagos");
  IO.println(nombres);
  nombres.add("reportes");
  IO.println(nombres);
}
```

```text
$ java fig04_04.java
[catalogo, inventario, pagos]
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_04.main(fig04_04.java:5)
```

The program prints the list once, and the `add` throws `java.lang.UnsupportedOperationException`. It is an exception without a message, and it does not need one: the requested operation simply does not exist on that list. The trace is read **from top to bottom**, unlike Python's: the first line says what was thrown, the second where it was born inside the JDK (`ImmutableCollections.uoe`, “unsupported operation exception”), the third which method you called (`add`) and the last, `fig04_04.main(fig04_04.java:5)`, which line of **your** program did it. Always look for the first line that mentions your file: that is where your error is. (The numbers `159` and `164` are from this JDK and will change in yours.)

The immutability of these collections is real and complete as far as the collection is concerned: elements cannot be added, removed or replaced. The JDK authors added other rules, which appear in the documentation of [`List`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html) and which I verified when writing this lesson:

- **They do not admit `null`.** `List.of("a", null)` throws `NullPointerException` when created. It is a decision in favor of fewer surprises: a `null` inside a collection is almost always a mistake.
- **`Set.of` and `Map.of` reject repeats.** `Map.of("a", 1, "a", 2)` throws `IllegalArgumentException: duplicate key: a`, and `Set.of("a", "a")` throws `IllegalArgumentException: duplicate element: a`. A repeated key in a map of fixed data is almost surely an oversight, and it is discovered at startup, not when someone queries.
- **Their order is not stable between runs**, as measured above.
- **`Map.of` accepts up to ten pairs.** For more, there is `Map.ofEntries`.

Two clarifications so as not to confuse what immutability promises. The first: **immutable does not mean the elements are immutable**. A `List.of(new ArrayList<>(), new ArrayList<>())` does not allow adding or removing lists, but each inner list can still be modified. With *records* of simple components (texts, numbers, other *records*), like the `revisor`'s, the problem does not exist; with mutable elements, it does. The second: lesson 3 already mentioned the **shallow immutability** of *records* (an array component is still mutable), and this is the same thing under another name: the *record* cannot change list, but if the list it stores is mutable, the list does change. That leads to the next section.

#### 4.1.4 Defensive copies

Here is a trap that is best learned by seeing it. Look at the program `fig04_05`. It defines two *records* with the same shape: `ReporteIngenuo` (“naive report”), which stores the list just as it is given, and `ReporteSeguro` (“safe report”), which in its compact constructor (the one from lesson 3) replaces the list with `List.copyOf(nombres)`, an **immutable copy**. Then it passes them the same list and modifies it afterward:

```java
// fig04_05.java
record ReporteIngenuo(List<String> nombres) {}

record ReporteSeguro(List<String> nombres) {
  ReporteSeguro {
    nombres = List.copyOf(nombres);
  }
}

void main() {
  List<String> nombres = new ArrayList<>(List.of("catalogo", "pagos"));
  ReporteIngenuo ingenuo = new ReporteIngenuo(nombres);
  ReporteSeguro seguro = new ReporteSeguro(nombres);
  nombres.add("reportes");
  IO.println("ingenuo: " + ingenuo);
  IO.println("seguro:  " + seguro);

  List<String> vista = Collections.unmodifiableList(nombres);
  List<String> copia = List.copyOf(nombres);
  nombres.add("facturacion");
  IO.println("vista: " + vista);
  IO.println("copia: " + copia);
}
```

```text
$ java fig04_05.java
ingenuo: ReporteIngenuo[nombres=[catalogo, pagos, reportes]]
seguro:  ReporteSeguro[nombres=[catalogo, pagos]]
vista: [catalogo, pagos, reportes, facturacion]
copia: [catalogo, pagos, reportes]
```

The naive report changed by itself, without anyone touching it: it stored a **reference** to the original list, not a copy, and when the original owner added `reportes`, the already-computed “report” was altered. The safe one kept the two names it had at the moment it was created. This is a **defensive copy**: the constructor defends itself against someone else still holding a reference to data that is now its own. It is one of those things that don't fail: they return a wrong number a week later.

The last two lines of the figure show another difference that confuses. `Collections.unmodifiableList(nombres)` copies nothing: it is a **view** that does not allow modifying *through it*, but that reflects everything the original owner changes (that is why `vista` ended with `facturacion`). `List.copyOf(nombres)` does make an independent copy (that is why `copia` does not have it). If what you want is for nobody to be able to alter your data, you need the copy; the view only stops whoever receives it from *writing*.

This technique is not a stylistic ornament. What you just saw is exactly the reason why the `Reporte` of section 4.5 carries `resultados = List.copyOf(resultados);` in its constructor. And it is not a quirk of this course: code analyzers know the hole. In lesson 10 you will meet SpotBugs, a tool that checks the compiled program looking for probable defects, and which calls this pattern `EI_EXPOSE_REP` and `EI_EXPOSE_REP2` (from *exposing internal representation*). With a condition worth knowing from now on, because it was measured: SpotBugs only reports it on classes that are visible from outside their package. The `revisor`'s `Reporte` is not public, and if you remove the copy the tool stays silent. The copy is not there to please a tool: it is there because the list belongs to the *record*, and what the tool does not check does not stop being a defect.

A note on efficiency: `List.copyOf` is smart. If the list you pass it is already immutable (for example, the one created by `List.of`), it does not copy it, it returns the same one. It makes sense: there is nothing to protect. The documentation says it with the phrase “if the given Collection is an unmodifiable List, calling copyOf will generally not create a copy”.

### 4.2 Generics

#### 4.2.1 What they are and what they give you

Every time you wrote `List<String>` or `Map<String, Long>` you used **generics**: a class or interface that is defined for “some type”, and that you make concrete when using it. `List<E>` is defined once, with a type `E` to be filled in, and you decide whether it is a list of `String`, of `Integer` or of `Servicio`. What you gain is verification **at compile time**: the compiler knows what type of element the list has and rejects what does not fit. Let us see what it says when you try to put a text in a list of numbers:

```java
// fig04_06.java
void main() {
  List<Integer> tiempos = new ArrayList<>();
  tiempos.add(142);
  tiempos.add("87 ms");
  IO.println(tiempos);
}
```

```text
$ java fig04_06.java
fig04_06.java:5: error: no suitable method found for add(String)
  tiempos.add("87 ms");
         ^
    method List.add(Integer) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method List.add(int,Integer) is not applicable
      (actual and formal argument lists differ in length)
1 error
error: compilation failed
```

The message is longer than you would expect. The compiler says `no suitable method found for add(String)` because `List` has two methods called `add`: one that receives an element (`add(Integer)`) and another that receives a position and an element (`add(int, Integer)`). It tried both and explains why neither works: the first because `String` is not converted to `Integer` (*argument mismatch*), the second because it does not even have the same number of parameters. Always read the **line in parentheses**: there is the real cause. If instead you try to take out a value and assign it to the wrong variable (`String texto = tiempos.get(0);`) the message is the simplest: `incompatible types: Integer cannot be converted to String`.

Without generics, neither of the two errors would exist at compile time: the list would accept anything, and the first warning would arrive at run time, far from where the error was made. It is the difference between a language that protects you and one that lets you write anything. (Section 4.2.3 shows what happens when someone skips the protection on purpose.)

#### 4.2.2 Generics only accept references: wrappers

Write `List<int>` and see what the compiler says:

```java
// fig04_07.java
void main() {
  List<int> tiempos = new ArrayList<>();
  tiempos.add(142);
  IO.println(tiempos);
}
```

```text
$ java fig04_07.java
fig04_07.java:3: error: unexpected type
  List<int> tiempos = new ArrayList<>();
       ^
  required: reference
  found:    int
1 error
error: compilation failed
```

The message is `unexpected type; required: reference; found: int`: a reference was required and a primitive was found. Generics only work with reference types, and `int` is a primitive (lesson 2). The solution is to use the primitive's **wrapper**: `Integer` for `int`, `Long` for `long`, `Double` for `double`, `Boolean` for `boolean`. They are the types you know from lesson 2 and that now have their reason for being. Java converts between a primitive and its wrapper by itself, in both directions, when needed: that is called ***autoboxing*** (putting the `142` in an `Integer`) and *unboxing* (taking it out). It is convenient, but it has two traps that you already know under another name, and each is worth a figure.

The first: `==` between wrappers compares **references**, as with `String`s. The program `fig04_08` measures it:

```java
// fig04_08.java
void main() {
  List<Integer> tiempos = List.of(87, 87, 1000, 1000);
  IO.println(tiempos.get(0) == tiempos.get(1));
  IO.println(tiempos.get(2) == tiempos.get(3));
  IO.println(tiempos.get(2).equals(tiempos.get(3)));
}
```

```text
$ java fig04_08.java
true
false
true
```

The two `87`s are “the same” object and the two `1000`s are not. The reason is that *autoboxing* uses `Integer.valueOf`, and [its documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Integer.html) promises that it “will always cache values in the range -128 to 127, inclusive, and may cache other values outside of this range”: for small ones it reuses the same object, and for the `1000` of this figure, in practice, it creates a new one each time. It is a documented behavior, not a bug, and that is why it is dangerous: a program that compares with `==` works with the small times of the tests and fails with those of production. The rule is the one from lesson 2, extended: **compare values with `equals`**, never with `==`, whenever the type is a reference, and wrappers are.

The second trap is the *unboxing* of a value that is not there. The program `fig04_09` asks a `Map<String, Long>` for a time and stores it in a `long` variable:

```java
// fig04_09.java
void main() {
  Map<String, Long> tiempos = Map.of("catalogo", 142L, "pagos", 87L);
  long tiempoPagos = tiempos.get("pagos");
  IO.println("pagos: " + tiempoPagos);
  long tiempoReportes = tiempos.get("reportes");
  IO.println("reportes: " + tiempoReportes);
}
```

```text
$ java fig04_09.java
pagos: 87
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Long.longValue()" because the return value of "java.util.Map.get(Object)" is null
	at fig04_09.main(fig04_09.java:6)
```

The first lookup works. The second fails with a `NullPointerException` whose message is a gem of diagnosis: `Cannot invoke "java.lang.Long.longValue()" because the return value of "java.util.Map.get(Object)" is null`. It reads like this: “I couldn't call `longValue()` because what `Map.get` returned is `null`”. Nobody wrote `longValue()`: the compiler inserted it when converting the `Long` into a `long`, and it failed because `get("reportes")` returned `null` (the key did not exist). `NullPointerException` messages always tell you **what** was null, and in this case even which method returned it. Two ways to avoid the error: ask for `getOrDefault("reportes", 0L)`, or store the result in a `Long` (not `long`) and ask `if (tiempo != null)`. Section 4.4 brings a third, better one.

#### 4.2.3 Type erasure

Lesson 0 already anticipated it: **generics are erased**. The compiler uses `List<String>` to verify your code, and after verifying it, it removes the types: in the *bytecode*, a `List<String>` and a `List<Integer>` are the same `List`. That is how they have been since Java 5: erasure ensures that no new class is created for each parameterized type, so that generics [cost nothing at run time](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html), and code written before they existed (almost all collections were) kept working. The program `fig04_10` confirms it and shows what it costs:

```java
// fig04_10.java
void main() {
  List<String> nombres = new ArrayList<>();
  List<Integer> tiempos = new ArrayList<>();
  IO.println(nombres.getClass() == tiempos.getClass());
  IO.println(nombres.getClass().getName());

  List crudo = nombres;
  crudo.add(142);
  IO.println("agregué un número a una lista de String y nadie se quejó");
  String primero = nombres.get(0);
  IO.println(primero);
}
```

```text
$ java fig04_10.java
fig04_10.java:9: warning: [unchecked] unchecked call to add(E) as a member of the raw type List
  crudo.add(142);
           ^
  where E is a type-variable:
    E extends Object declared in interface List
1 warning
true
java.util.ArrayList
agregué un número a una lista de String y nadie se quejó
Exception in thread "main" java.lang.ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String (java.lang.Integer and java.lang.String are in module java.base of loader 'bootstrap')
	at fig04_10.main(fig04_10.java:11)
```

There are several things to read here. The compiler printed a warning (`warning: [unchecked] unchecked call to add(E) as a member of the raw type List`) before running the program: it warns you that on line 9 you did something it cannot verify. The cause is on line 8: `List crudo = nombres;` (`crudo` means “raw”). A `List` without the `<...>` is a **raw type**, the same type as before Java 5. The compiler accepts storing a list of `String` in it and accepts adding a `142` to it, because with the raw type it verifies nothing, it only warns. Then the program prints “agregué un número a una lista de String y nadie se quejó” (“I added a number to a String list and nobody complained”) and fails on **line 11**, not on 9:

`ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String`

The error appeared when *taking out* the element, two lines after the one that put it in. In a real program they could be two different classes and ten minutes of debugging. It is the consequence of generics being verified only at compile time: the compiler could promise you the list was of `String` only as long as nobody jumped over the type system, and the raw type is the gap. The rule: **never use raw types in new code**; if you see an `[unchecked]` warning, it is the compiler telling you to fix something.

The first two lines of the output are the other side of erasure. `nombres.getClass() == tiempos.getClass()` is `true` and the class's name is `java.util.ArrayList`, with no trace of the `<String>`: at run time, the list does not know what it is a list of. From there come three limits you will find in practice, and that are worth knowing from now on so you do not lose an hour looking for them: you cannot write `new T()` inside a generic method, you cannot ask `if (lista instanceof List<String>)` (only `List<?>`), and you cannot overload two methods that are distinguished only by the generic type (`void f(List<String>)` and `void f(List<Integer>)` clash because, erased, they are the same method). The [official Java tutorial on erasure](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html) lists the full rules.

#### 4.2.4 Writing a generic method and reading a signature

You will almost never *define* a generic class, but you will read the signatures of the JDK's, and from time to time you will write a generic method. Look at one in `fig04_11`:

```java
// fig04_11.java
static <T> T primeroO(List<T> lista, T porOmision) {
  if (lista.isEmpty()) {
    return porOmision;
  }
  return lista.get(0);
}

void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<Long> tiempos = List.of();
  String primerNombre = primeroO(nombres, "ninguno");
  Long primerTiempo = primeroO(tiempos, 0L);
  IO.println(primerNombre);
  IO.println(primerTiempo);
}
```

```text
$ java fig04_11.java
catalogo
0
```

The `<T>` before the return type declares a **type variable**: “in this method, `T` is a type to be determined”. The compiler deduces it from the arguments: in the first call `T` is `String` and in the second `Long`, and that is why the result can be stored in a `String` and in a `Long` without any conversion. Writing it with `Object` instead of `T` would force you to convert by hand.

And to read the documentation, one more symbol: `?`, the **wildcard**. You will see signatures like `List.copyOf(Collection<? extends E> coll)`. It reads “a collection of *some type* that is `E` or a subclass of `E`”. The word `extends` here means “is `E` or inherits from it”, and the wildcard lets you pass, for example, a list of `Respondio` where a collection of `Respuesta` is expected. You do not need to write wildcards yet; you need to be able to read them without being frightened, because they appear in `Collectors`, in `Comparator` and in almost the whole *streams* library that follows.

That ends the first session. Before going on, check that you can answer without looking: what is the difference between `List.copyOf` and `Collections.unmodifiableList`? Why does `Set.of` print in a different order each time? What fails at compile time with `List<int>` and what fails at run time with a raw list? If you doubt any, go back to its figure before opening session two.

### 4.3 Streams

The second session begins. Until now you stored values; now you are going to transform them. The `revisor`'s concrete question is: given the list of results, how many are up, how many slow, how many down, and what is the average of the times of those that responded? With what you know you can write a `for` with four counters. *Streams* are the other answer, and to understand them you first have to understand the tool they are written with: lambdas.

#### 4.3.1 Lambdas and method references

A **lambda** is a small function, without a name, that you write at the place where you need it and pass as if it were a value. Its form is `parameters -> body`. You already saw the arrow in the `switch` of lesson 2, but here it means something else: not “if the case matches, return this”, but “given this parameter, compute this”. These are the forms that exist:

- `medicion -> medicion.tiempoMs() >= 1000`: one parameter, a body that is an expression (the result is the expression's value).
- `(a, b) -> a + b`: two parameters, in parentheses.
- `() -> "revisando..."`: none, with empty parentheses.
- `medicion -> { ...several lines...; return valor; }`: a block between braces, with `return` if it must return something.

And what is the *type* of a lambda? In Java, a lambda is always assigned to a **functional interface**: an interface with a single abstract method. The lambda is the implementation of that method. The JDK provides several that are used constantly: `Predicate<T>` (one method, `test`, which receives a `T` and returns `boolean`), `Function<T, R>` (`apply`, receives a `T`, returns an `R`), `Supplier<T>` (`get`, receives nothing and returns a `T`) and `Comparator<T>` (`compare`, which receives two and returns an integer). The program `fig04_12` uses them one by one:

```java
// fig04_12.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  Predicate<Medicion> esLenta = medicion -> medicion.tiempoMs() >= 1000;
  Function<Medicion, String> etiqueta = medicion -> medicion.nombre() + ":" + medicion.tiempoMs();
  Function<Medicion, String> soloNombre = Medicion::nombre;
  Supplier<String> saludo = () -> "revisando...";

  Medicion inventario = new Medicion("inventario", 2300);
  Medicion pagos = new Medicion("pagos", 87);
  IO.println(esLenta.test(inventario));
  IO.println(esLenta.test(pagos));
  IO.println(etiqueta.apply(pagos));
  IO.println(soloNombre.apply(inventario));
  IO.println(saludo.get());
}
```

```text
$ java fig04_12.java
true
false
pagos:87
inventario
revisando...
```

`esLenta` (“is slow”) is a `Predicate<Medicion>`: you pass it a measurement with `test` and it answers `true` or `false`. `etiqueta` (“label”) is a `Function<Medicion, String>`: it converts a measurement into a text. `saludo` (“greeting”) is a `Supplier<String>`: it receives nothing and produces a value. When you write a lambda inside a *stream* you do not name these types; the compiler deduces them from the method you pass it to. But knowing them is what lets you read the documentation: when `filter` asks for a `Predicate`, it is asking for “a function that receives an element and answers yes or no”.

`soloNombre` shows the second tool: the **method reference**. `Medicion::nombre` is the short form of `medicion -> medicion.nombre()`: “call the `nombre` method of the element you are given”. It reads better when the lambda only calls a method, and you will see it everywhere (`Resultado::estado`, `Revision::revisar`, `Formato::linea`). Use the one that reads clearer; the compiler treats them the same.

There is a rule of lambdas that surprises someone arriving from JavaScript or Python. Look at this program, which tries to add up the times with a lambda that modifies a variable from outside:

```java
// fig04_13.java
void main() {
  List<Long> tiempos = List.of(142L, 2300L, 87L);
  long suma = 0;
  tiempos.forEach(tiempo -> suma += tiempo);
  IO.println(suma);
}
```

```text
$ java fig04_13.java
fig04_13.java:5: error: local variables referenced from a lambda expression must be final or effectively final
  tiempos.forEach(tiempo -> suma += tiempo);
                            ^
1 error
error: compilation failed
```

The error says `local variables referenced from a lambda expression must be final or effectively final`: a lambda can only use outside local variables if they are **never reassigned** (they are *effectively final*). Here `suma` (“sum”) changes on every iteration, and the compiler rejects it. The reason is that a lambda may run later, or on another thread, and Java does not want the lambda and the outside code to share a variable that changes. In lesson 9 you will see why it is a valuable protection. The way out of this problem is not to dodge the rule but to use the right tool: `mapToLong(...).sum()`, which is exactly what comes in the next section. If you find yourself writing a lambda that modifies something outside, it is almost always a sign that a `collect` or a `sum` is missing.

#### 4.3.2 A *stream* is a pipeline

A ***stream*** is not a collection: it is a **description of a sequence of steps** applied to the elements of a source. It has three parts. A **source**, which is almost always a collection and is obtained with `.stream()`. Zero or more **intermediate operations** (`filter`, `map`, `sorted`...), which transform the stream into another stream. And a **terminal operation** (`toList`, `count`, `sum`, `collect`...), which produces the final result and is the one that really sets everything in motion. The steps are written chained, one per line, and are read from top to bottom like a recipe. Look at the program `fig04_14`, which does the same job twice: first with a loop and then with a *stream*.

```java
// fig04_14.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", 142),
          new Medicion("inventario", 2300),
          new Medicion("pagos", 87));

  List<String> conBucle = new ArrayList<>();
  for (Medicion medicion : mediciones) {
    if (medicion.tiempoMs() >= 100) {
      conBucle.add(medicion.nombre() + ":" + medicion.tiempoMs());
    }
  }

  List<String> conStream =
      mediciones.stream()
          .filter(medicion -> medicion.tiempoMs() >= 100)
          .map(medicion -> medicion.nombre() + ":" + medicion.tiempoMs())
          .toList();

  IO.println(conBucle);
  IO.println(conStream);
  IO.println(conBucle.equals(conStream));

  List<String> nombres = mediciones.stream().map(Medicion::nombre).toList();
  long suma = mediciones.stream().mapToLong(Medicion::tiempoMs).sum();
  long rapidos = mediciones.stream().filter(medicion -> medicion.tiempoMs() < 100).count();
  IO.println(nombres);
  IO.println("suma: " + suma + " ms");
  IO.println("rápidos: " + rapidos);
}
```

```text
$ java fig04_14.java
[catalogo:142, inventario:2300]
[catalogo:142, inventario:2300]
true
[catalogo, inventario, pagos]
suma: 2529 ms
rápidos: 1
```

The loop and the *stream* produce the same list (the last line checks with `equals` that they are equal). The *stream* reads: “from the measurements, **keep** the ones that meet `tiempoMs() >= 100` (`filter`), **convert** each one into a text (`map`) and **gather** the result into a list (`toList`)”. Notice what is not there: there is no empty list being filled, no condition wrapped in an `if`, no variable that changes. Whoever reads describes what they want, not how to do it.

The operations you will use most often:

| Operation | Type | What it does |
|---|---|---|
| `filter(Predicate)` | intermediate | lets through only the elements that meet the condition |
| `map(Function)` | intermediate | transforms each element into another value |
| `mapToLong(...)` | intermediate | transforms each element into a primitive `long` (no wrappers) |
| `sorted(Comparator)` | intermediate | sorts (section 4.3.5) |
| `toList()` | terminal | gathers the elements into an **immutable** list |
| `count()` | terminal | counts the elements |
| `sum()`, `average()`, `max()` | terminal | aggregate; exist on primitive streams |
| `collect(Collector)` | terminal | gathers the elements with a recipe (section 4.3.4) |

Two clarifications about the table. `mapToLong` produces a specialized stream (`LongStream`) that knows how to add and average without wrapping each number in a `Long`; its siblings `IntStream` and `DoubleStream` exist. And `toList()` returns an **immutable** list, like `List.of`: if later you need it modifiable, you copy it with `new ArrayList<>(lista)`. The section “The error you will see” will show the exception that comes out when this is forgotten.

#### 4.3.3 Lazy and single-use

There are two properties of *streams* that cannot be guessed and that produce errors if you ignore them. The first is that they are **lazy**: assembling the stream does nothing. According to the [documentation of the `java.util.stream` package](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html), intermediate operations “are always lazy”: `filter` filters nothing when you call it, it just creates a new stream that will filter when someone traverses it. The work happens when the terminal operation arrives. The program `fig04_15` makes it visible with `peek`, an operation that lets each element through and in passing executes an action (its documentation says it exists “mainly to support debugging”, and only for that do we use it here):

```java
// fig04_15.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", 142),
          new Medicion("inventario", 2300),
          new Medicion("pagos", 87));

  Stream<String> armado =
      mediciones.stream()
          .peek(medicion -> IO.println("miro " + medicion.nombre()))
          .filter(medicion -> medicion.tiempoMs() >= 100)
          .peek(medicion -> IO.println("  pasa " + medicion.nombre()))
          .map(Medicion::nombre);
  IO.println("el stream está armado; todavía no pasó nada");

  List<String> resultado = armado.toList();
  IO.println(resultado);
}
```

```text
$ java fig04_15.java
el stream está armado; todavía no pasó nada
miro catalogo
  pasa catalogo
miro inventario
  pasa inventario
miro pagos
[catalogo, inventario]
```

Look at the order. “el stream está armado; todavía no pasó nada” (“the stream is assembled; nothing has happened yet”) comes out **first**, before any “miro” (“I look at”). The `peek`s and the `filter` did not run a single time when the stream was assembled: they waited for `toList()`. And when they finally run, they do not do so step by step over the whole list, but **element by element**: `catalogo` goes through `peek`, `filter`, `peek` and `map` before `inventario` enters the stream, and `pagos`, which does not meet the filter, stays at the first `peek`. This has practical consequences: a stream without a terminal operation is dead code (the compiler does not flag it), and an intermediate operation with a side effect, such as printing, runs in an order that is not “first all, then all”.

The second property is that a stream can be traversed **only once**. The [`Stream` documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Stream.html) says that “a stream should be operated on (invoking an intermediate or terminal stream operation) only once”. What happens if you ignore it:

```java
// fig04_16.java
void main() {
  Stream<String> nombres = Stream.of("catalogo", "inventario", "pagos");
  IO.println(nombres.count());
  IO.println(nombres.count());
}
```

```text
$ java fig04_16.java
3
Exception in thread "main" java.lang.IllegalStateException: stream has already been operated upon or closed
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:260)
	at java.base/java.util.stream.ReferencePipeline.count(ReferencePipeline.java:750)
	at fig04_16.main(fig04_16.java:5)
```

The first call to `count()` works (3) and consumes the stream. The second throws `IllegalStateException: stream has already been operated upon or closed`. The fix is to create a new stream each time you need one, from the collection: `nombres.stream()` is cheap. That is why the course's variables hold the **list**, and the stream is assembled and consumed in the same expression.

#### 4.3.4 `Collectors`: gathering the result in different ways

`toList()` covers one case. For the others there is `collect`, which receives a **collector**: a recipe for gathering the elements. The `Collectors` class provides the most common recipes. The program `fig04_17` uses the ones the `revisor`'s summary needs:

```java
// fig04_17.java
record Medicion(String nombre, String estado, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", "ARRIBA", 142),
          new Medicion("inventario", "LENTO", 2300),
          new Medicion("pagos", "ARRIBA", 87),
          new Medicion("reportes", "CAIDO", 0));

  Map<String, Long> porEstado =
      mediciones.stream()
          .collect(Collectors.groupingBy(Medicion::estado, TreeMap::new, Collectors.counting()));
  IO.println(porEstado);

  Map<String, List<String>> nombresPorEstado =
      mediciones.stream()
          .collect(
              Collectors.groupingBy(
                  Medicion::estado,
                  TreeMap::new,
                  Collectors.mapping(Medicion::nombre, Collectors.toList())));
  IO.println(nombresPorEstado);

  String todos = mediciones.stream().map(Medicion::nombre).collect(Collectors.joining(", "));
  IO.println(todos);

  OptionalDouble promedio =
      mediciones.stream()
          .filter(medicion -> !medicion.estado().equals("CAIDO"))
          .mapToLong(Medicion::tiempoMs)
          .average();
  IO.println(promedio);
  IO.println(Math.round(promedio.getAsDouble()));

  OptionalDouble sinDatos =
      mediciones.stream()
          .filter(medicion -> medicion.tiempoMs() > 9999)
          .mapToLong(Medicion::tiempoMs)
          .average();
  IO.println(sinDatos);
}
```

```text
$ java fig04_17.java
{ARRIBA=2, CAIDO=1, LENTO=1}
{ARRIBA=[catalogo, pagos], CAIDO=[reportes], LENTO=[inventario]}
catalogo, inventario, pagos, reportes
OptionalDouble[843.0]
843
OptionalDouble.empty
```

The first is the most important: `groupingBy(Medicion::estado, TreeMap::new, Collectors.counting())`. `groupingBy` **groups** the elements by the value the function returns (here, the status) and applies another collector to each group (here, `counting()`, which counts). The result is a map from status to quantity: `{ARRIBA=2, CAIDO=1, LENTO=1}`. Without the second argument, `groupingBy` would return a `HashMap` of unpromised order; with `TreeMap::new` (a constructor reference: “call `new TreeMap()` when you need to create the map”) the result comes out alphabetically sorted. That resembles the “how many of each status” column of the `revisor`'s summary.

The second recipe groups the same but, instead of counting, **stores the names** of each group: `Collectors.mapping(Medicion::nombre, Collectors.toList())` transforms each element before gathering it. It is the “group and then reduce each group” pattern that you will see with different reducers. `Collectors.joining(", ")` joins texts with a separator; it is what assembles the table's body in section 4.5.

The last part of the figure is the one that teaches the most. `average()` computes the average of a stream of `long` and returns an `OptionalDouble`, not a `double`. It is printed as `OptionalDouble[843.0]`, and `getAsDouble()` takes the number out. When the stream is empty (the last case, where the filter lets no one through), the average **does not exist** and the result is `OptionalDouble.empty`. It is a smart decision: other languages would return zero or `NaN` (a number meaning “not a number”), and a zero would lie, because the average of nothing is not zero. Java forces you to decide what to do when there is no data. Section 4.4 explains `Optional` in full.

A detail about `Math.round(843.0)`: it rounds a `double` to the nearest integer and returns a `long`. (If you want the average without rounding, leave the `double`.) For the `revisor`, which shows whole milliseconds, rounding is the right thing.

#### 4.3.5 Sorting: `Comparator`

Sorting requires knowing how to **compare** two elements. For texts and numbers there is a natural order (alphabetical, numeric) and `sorted()` without arguments uses it. For your own types, you say how to compare with a `Comparator`, and the JDK provides a toolbox for building them without writing the `compare` method by hand. The program `fig04_18` exercises them with four measurements, two of them with the same time so that the tie-break is visible:

```java
// fig04_18.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("pagos", 87),
          new Medicion("catalogo", 142),
          new Medicion("busqueda", 142),
          new Medicion("inventario", 2300));

  Comparator<Medicion> porTiempo = Comparator.comparingLong(Medicion::tiempoMs);

  List<Medicion> ascendente = mediciones.stream().sorted(porTiempo).toList();
  List<Medicion> descendente = mediciones.stream().sorted(porTiempo.reversed()).toList();
  List<Medicion> desempatado =
      mediciones.stream().sorted(porTiempo.thenComparing(Medicion::nombre)).toList();

  ascendente.forEach(medicion -> IO.println("asc  " + medicion));
  descendente.forEach(medicion -> IO.println("desc " + medicion));
  desempatado.forEach(medicion -> IO.println("tiempo y nombre " + medicion));

  Optional<Medicion> masLenta = mediciones.stream().max(porTiempo);
  IO.println(masLenta);
  IO.println(mediciones);
}
```

```text
$ java fig04_18.java
asc  Medicion[nombre=pagos, tiempoMs=87]
asc  Medicion[nombre=catalogo, tiempoMs=142]
asc  Medicion[nombre=busqueda, tiempoMs=142]
asc  Medicion[nombre=inventario, tiempoMs=2300]
desc Medicion[nombre=inventario, tiempoMs=2300]
desc Medicion[nombre=catalogo, tiempoMs=142]
desc Medicion[nombre=busqueda, tiempoMs=142]
desc Medicion[nombre=pagos, tiempoMs=87]
tiempo y nombre Medicion[nombre=pagos, tiempoMs=87]
tiempo y nombre Medicion[nombre=busqueda, tiempoMs=142]
tiempo y nombre Medicion[nombre=catalogo, tiempoMs=142]
tiempo y nombre Medicion[nombre=inventario, tiempoMs=2300]
Optional[Medicion[nombre=inventario, tiempoMs=2300]]
[Medicion[nombre=pagos, tiempoMs=87], Medicion[nombre=catalogo, tiempoMs=142], Medicion[nombre=busqueda, tiempoMs=142], Medicion[nombre=inventario, tiempoMs=2300]]
```

What was read in the output:

- `Comparator.comparingLong(Medicion::tiempoMs)` builds a comparator from a function that extracts the value to compare. There are versions for `comparing` (any type with natural order), `comparingInt`, `comparingLong` and `comparingDouble`.
- `.reversed()` inverts the order. `.thenComparing(...)` adds a **tie-break**: it is used only when the two elements tie on the first criterion. That is why, in the third block, `busqueda` comes before `catalogo`: they have the same 142 ms and the tie-break sorts them alphabetically.
- Without a tie-break, in the first two blocks `catalogo` comes before `busqueda` in both directions. It is no coincidence: the [`Stream.sorted` documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Stream.html) guarantees that “for ordered streams, the sort is stable”: elements that tie keep the order they came in.
- **`sorted` does not touch the original list.** The last lines prove it: `mediciones` is still in the order it was created. If you want to sort the list itself, `lista.sort(comparador)` does it in place, but it only works with a modifiable list: on a `List.of` it throws `UnsupportedOperationException` again.
- `max(porTiempo)` returns an `Optional<Medicion>` and not a `Medicion`, and is printed as `Optional[Medicion[nombre=inventario, tiempoMs=2300]]`. Asking for the maximum of an empty list has no answer, and the type says so.

A `forEach` appeared twice in the program: `ascendente.forEach(medicion -> IO.println(...))`. It receives a lambda that returns nothing (a `Consumer`, the functional interface of “do this with each element”) and is available on lists and on streams. For printing it is fine; for filling another collection, it is not (see “What gets done wrong”).

#### 4.3.6 Stream or loop?

The wrong message about *streams* is that “they are better”. They are not: they are another tool. Prefer a **stream** when you transform or summarize a collection (filter, convert, count, group, sort), because the code says what is wanted. Prefer a **loop** when you need to exit early with `break`, keep an index, modify something outside, or when the body can throw a checked exception (a lambda cannot throw more checked exceptions than its functional interface declares, and the `Function` that `map` receives declares none; lesson 5 explains it, and that is why `revisarTodos` will go back to being a `for` at that moment: it is not a step backward, it is the right tool). And a reading rule that saves arguments: if a stream no longer fits in six or seven lines, or has a multi-line lambda, extract that lambda to a named method and use the reference (`Revision::revisar` reads better than five lines inside a `map`).

### 4.4 `Optional`: a value that may not be there

The `revisor` has a representation problem that has kept repeating: what does a function return when there is no value? `Map.get("reportes")` returned `null`; a service that is down has no response time; the average of nobody does not exist. Lesson 2 showed the cost of `null`: the compiler does not distinguish it from a value, and the error appears far away, as a `NullPointerException`. `Optional<T>` is the JDK's answer: a little box that **has a value or is empty**, and whose type tells whoever receives it that they have to decide what to do in both cases.

The program `fig04_19` goes through the vocabulary:

```java
// fig04_19.java
Optional<Long> tiempoDe(Map<String, Long> tiempos, String nombre) {
  return Optional.ofNullable(tiempos.get(nombre));
}

void main() {
  Map<String, Long> tiempos = Map.of("catalogo", 142L, "pagos", 87L);

  Optional<Long> hay = tiempoDe(tiempos, "pagos");
  Optional<Long> noHay = tiempoDe(tiempos, "reportes");
  IO.println(hay);
  IO.println(noHay);
  IO.println(hay.isPresent() + " " + noHay.isEmpty());

  IO.println(hay.orElse(0L));
  IO.println(noHay.orElse(0L));
  IO.println(hay.map(tiempo -> tiempo + " ms").orElse("sin tiempo"));
  IO.println(noHay.map(tiempo -> tiempo + " ms").orElse("sin tiempo"));
  hay.ifPresent(tiempo -> IO.println("pagos respondió en " + tiempo));
  noHay.ifPresent(tiempo -> IO.println("esto no se imprime"));

  OptionalLong mayor = tiempos.values().stream().mapToLong(Long::longValue).max();
  IO.println(mayor);
  IO.println(mayor.getAsLong());
}
```

```text
$ java fig04_19.java
Optional[87]
Optional.empty
true true
87
0
87 ms
sin tiempo
pagos respondió en 87
OptionalLong[142]
142
```

The three ways of **creating** an `Optional`: `Optional.of(valor)` (the value cannot be `null`; if it is, it throws `NullPointerException`), `Optional.ofNullable(valor)` (if the value is `null`, it is left empty; it is the one `tiempoDe` uses to adapt a `get` that could return `null`) and `Optional.empty()`. The ways of **consuming** it; the last one is the one to avoid:

- `orElse(porOmision)` returns the value, or the one you give if it is empty. `orElseGet(proveedor)` does the same but receives a lambda that only runs if needed. `orElseThrow()` throws an exception if it is empty (lesson 5 explains which).
- `map(función)` transforms the value if it exists and leaves the empty one as it is: `hay.map(tiempo -> tiempo + " ms")` produces an `Optional<String>` with “87 ms” inside, which `orElse` unwraps; with the empty one, `map` does nothing, the result stays empty and `orElse` gives “sin tiempo” (“no time”). It is what avoids writing `if (x != null)` everywhere.
- `ifPresent(acción)` runs the lambda only if there is a value; `isPresent()` and `isEmpty()` ask.
- `get()` takes the value out, **and throws an exception if there is none**. It exists, but it is the entrance door to the same problem `Optional` wanted to solve.

Let us see the cost of `get()` without checking:

```java
// fig04_20.java
void main() {
  Optional<String> nombre = Optional.empty();
  IO.println("a punto de pedir el valor");
  IO.println(nombre.get());
}
```

```text
$ java fig04_20.java
a punto de pedir el valor
Exception in thread "main" java.util.NoSuchElementException: No value present
	at java.base/java.util.Optional.get(Optional.java:143)
	at fig04_20.main(fig04_20.java:5)
```

`NoSuchElementException: No value present`. The error is honest and is the same as the usual `NullPointerException`, under another name: someone assumed there was a value. Now, with the difference that the `Optional` type made the risk evident at the exact point where the decision was made. The practical rule: **do not call `get()` unless you have checked in the same block**, and almost always there is an `orElse` or a `map` that says the same thing better.

There is also `orElse` with a detail that costs dearly: its argument is computed **always**, even if it is not needed. The program `fig04_21` shows it with a default value that announces when it is computed:

```java
// fig04_21.java
String calcular(String quien) {
  IO.println("  (calculando el valor por omisión para " + quien + ")");
  return "ninguno";
}

void main() {
  Optional<String> hay = Optional.of("catalogo");
  Optional<String> noHay = Optional.empty();

  IO.println("orElse con valor presente:");
  IO.println(hay.orElse(calcular("orElse")));
  IO.println("orElseGet con valor presente:");
  IO.println(hay.orElseGet(() -> calcular("orElseGet")));
  IO.println("orElseGet con valor ausente:");
  IO.println(noHay.orElseGet(() -> calcular("orElseGet")));
}
```

```text
$ java fig04_21.java
orElse con valor presente:
  (calculando el valor por omisión para orElse)
catalogo
orElseGet con valor presente:
catalogo
orElseGet con valor ausente:
  (calculando el valor por omisión para orElseGet)
ninguno
```

With the value present, `orElse` computed the default value anyway (the “calculando...” message, “calculating...”, came out) and threw it away; `orElseGet` did not compute it. For a literal `"ninguno"` (“none”) it makes no difference; for something expensive, like a query or a large object, use `orElseGet`. It is the same idea as in lesson 2 with `&&` versus `&`: do not evaluate what you do not need.

And the last part, the one that brings design rules. The [`Optional`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html) documentation says, in an API note, that it “is primarily intended for use as a method return type where there is a clear need to represent 'no result,' and where using null is likely to cause errors”. Three rules come from there:

1. **Use it as a return type**, not as a parameter (it forces the caller to wrap a value they already have) nor as a list element (`List<Optional<T>>` is almost always a missing `filter`).
2. **An `Optional` variable must never be `null`**: the documentation says so in those words. A `null` where an `Optional` was expected is worse than either of the two separately.
3. **For primitives, `OptionalInt`, `OptionalLong` and `OptionalDouble`**: without wrappers, with `getAsLong()` instead of `get()`. They are used by `average()` and `max()` of numeric streams, and by the `revisor`, for the time.

The project breaks the “not as a field” rule in one place, and it is worth saying it head-on: the `Resumen` (“summary”) *record* of section 4.5 stores an `OptionalLong promedioMs`. It is an immutable *record*, created once and read several times; the absent value is part of the data (when nobody responded, there is no average), and the alternatives are worse: a `Long` that can be `null` is the problem we just left behind, and a `0` would lie. It is a conscious exception to a guideline, not an oversight. In lesson 8 you will see how that absent value crosses the border into JSON, where the format does ask for a `null`.

### 4.5 The project step: the report with a summary

Now to put it all together. Lesson 3 left a `Revisor.java` that checked four services written by hand and a `simular` with a single `switch`, which gathered the data of the four services. Today the project changes like this:

- The **simulated data** leaves the `switch` and goes to a map in a new class, `Simulador`. It is looked up by name and returns an `Optional`.
- **`Revision`** (“review”) checks one service (querying the simulator) and checks a whole list with a *stream*.
- **`Resumen`** counts and averages with `groupingBy` and `average`; **`Reporte`** (“report”) gathers the results and their summary, with a defensive copy.
- **`Resultado`** learns to give its time as an `OptionalLong`; **`Formato`** learns to assemble the summary and the complete table.
- **`Revisor.java`** stops calling `revisar` four times: it has a list and asks for the report all at once.

Four files do not change: `Estado`, `Respuesta`, `Servicio` and `Clasificador`. I include them anyway because the figure's folder is the **complete** project, and this way you can run everything from it. Copy them as they are from lesson 3. Start with them:

```java
// fig04_22/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig04_22/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig04_22/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

```java
// fig04_22/Clasificador.java
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

A note about `import`. The four files above and the ones that follow are normal classes (not compact source files), and that is why they begin, when they need it, with lines such as `import java.util.List;`. An `import` tells the compiler where a name that is not from `java.lang` comes from (the package Java imports by itself, with `String`, `Math` and `Thread`). One is written per class, without wildcards, as Google's style guide, which the course follows since lesson 1, indicates. The compact source file `Revisor.java`, by contrast, imports all of `java.base` by itself and needs none.

Next is `Resultado` (“result”), which gains a method. `tiempoMs()` returns an `OptionalLong`: with a value when there was a response and empty when not. It is the same exhaustive `switch` from lesson 3: if someday someone adds a third type of `Respuesta`, the compiler will flag this method and `detalle()` as incomplete.

```java
// fig04_22/Resultado.java
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

`Simulador` is the class that plays the network until lesson 8. Its `RESPUESTAS` is an immutable `Map` (`Map.of`) from name to response, with the three that respond and the one that does not. `buscar` (“search”) wraps the map's `get` in an `Optional.ofNullable`, and it is the only point where the absence of a name becomes a type that forces a decision.

```java
// fig04_22/Simulador.java
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

`Revision` has the two methods that produce results. `revisar` asks the simulator for the response and, if the name is not there, uses a `SinRespuesta("sin conexión")` (“no connection”; an `orElse` with a cheap value). `revisarTodos` (“review all”) is the line that justifies the whole lesson: a *stream* over the list of services, with `Revision::revisar` as the `map` and `toList()` at the end.

```java
// fig04_22/Revision.java
import java.util.List;

final class Revision {
  static Resultado revisar(Servicio servicio) {
    Respuesta respuesta =
        Simulador.buscar(servicio.nombre()).orElse(new Respuesta.SinRespuesta("sin conexión"));
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  static List<Resultado> revisarTodos(List<Servicio> servicios) {
    return servicios.stream().map(Revision::revisar).toList();
  }
}
```

`Resumen` is where *streams* and collectors come together. `groupingBy` counts how many there are of each status and `average` computes the average of the times of those that responded. Two details. One: `groupingBy` only creates a key for the statuses that **appear**; if no service is down, `porEstado.get(Estado.CAIDO)` would be `null`. That is why `getOrDefault(..., 0L)` is used: without it, the first report with no service down would bring the program down with the `NullPointerException` of section 4.2.2. `intValue()` converts the count's `Long` into the `int` that the *record* asks for (the counts fit with room to spare). Two: the average counts everyone who responded, whatever their code (a 503 responded, even if it responded badly), and that is why it is computed over `tiempoMs()` and not over the status.

```java
// fig04_22/Resumen.java
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

`Reporte` is the *record* of the defensive copy from section 4.1.4. Its factory `de` (“from”) gathers the list of results with its summary:

```java
// fig04_22/Reporte.java
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

`Formato` grows with three methods: `contar` (“count”), which puts a word in the plural (the course's only plural rule: an “s” is added if the quantity is not 1), `resumen`, which assembles the last line, and `tabla` (“table”), which gathers the header, the rows (joined with `Collectors.joining("\n")`), a blank line and the summary. The `? :` in `contar` is the **conditional operator** from lesson 2 (2.3.1): `cantidad == 1 ? "" : "s"` reads “if the quantity is 1, nothing; otherwise, an ‘s’”. (The same operator appears in `Resumen`, to decide whether there is an average.) And there is a small change in a method you already had, which is worth saying out loud: `formatearTiempo` stops comparing against `Clasificador.UMBRAL_LENTO_MS` and compares against `1000`. They are two different ideas that today are worth the same: the “slow” threshold is a business rule, and the step from milliseconds to seconds is a units rule. If tomorrow the slow threshold goes down to 800 ms, the table must keep writing `999ms` and not `1.0s`; with the shared constant, changing one rule would have changed the other without anyone asking. (Lesson 7 makes exactly that threshold change, and a test watches over it.) The rest of the file, `PATRON_LINEA`, `tiempo`, `encabezado` and `linea`, stays as in lesson 3.

```java
// fig04_22/Formato.java
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

And `Revisor.java` ends up short. It has two novelties that explain themselves: a **list** of services (a field of the compact source file, that is, a variable that is outside any method and that all the file's methods can use) marked `final`, which means “this variable cannot be reassigned”, and a one-line `main`:

```java
// fig04_22/Revisor.java
final List<Servicio> servicios =
    List.of(
        new Servicio("catalogo", URI.create("https://catalogo.example")),
        new Servicio("inventario", URI.create("https://inventario.example")),
        new Servicio("pagos", URI.create("https://pagos.example")),
        new Servicio("reportes", URI.create("https://reportes.example")));

void main() {
  IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
}
```

Run the project from its folder:

```text
$ cd fig04_22
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

That is the table of lesson 3, with what it was missing: the blank line and the summary. See how the summary reads against the code: `4 servicios` is `contar(4, "servicio")`, `2 arriba` is the count of `ARRIBA`, `1 lento` and `1 caído` are `contar(...)` with their words, and `843ms` is the `Math.round` of 2529 / 3, formatted with `formatearTiempo` (which already knows that 843 < 1000 is written with “ms”). When the summary gives you a number you do not expect, start with `Resumen`: that is where everything is computed.

## The error you will see

This lesson's error is `UnsupportedOperationException`, and it is the one almost everyone meets in the first week with collections. You already saw it with `List.of`. This is the same trace, so that you read it line by line:

```text
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_04.main(fig04_04.java:5)
```

**First line: what.** `UnsupportedOperationException` is the type; it carries no message, because the operation is not supported and there is nothing more to say. **Second and third: where, inside the JDK.** They speak of `ImmutableCollections`, the class that implements the collections of `List.of`, `Set.of` and `Map.of` (and of `toList()`). When you see that name, you already know you tried to modify an immutable collection. **Fourth: where, in your code.** The first line that mentions your file is the one you should look at.

The same error appears by other paths that are worth recognizing. It occurs when you modify what a stream's `toList()` returned:

```java
// fig04_23.java
void main() {
  List<String> nombres = Stream.of("catalogo", "inventario", "pagos").toList();
  IO.println(nombres);
  nombres.add("reportes");
}
```

```text
$ java fig04_23.java
[catalogo, inventario, pagos]
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_23.main(fig04_23.java:5)
```

The trace is identical, with another line number from your program. And it occurs when you call `sort` on an immutable list. There are two solutions, depending on what you want. If you truly need a modifiable list, make it explicit with a copy: `new ArrayList<>(nombres)`. If what you wanted was a sorted list, do not sort it in place: use `stream().sorted(...).toList()` and receive another list. **The best reaction to the error is not to make the list modifiable by reflex, but to ask yourself whether the operation you are attempting was supposed to exist.**

## What gets done wrong

- **Modifying a list while traversing it with `for`.** It is the classic error, and the trap is that sometimes it does not even fail. If you remove an element inside the enhanced `for`, most of the time the program stops with an exception:

```java
// fig04_24.java
void main() {
  List<String> nombres = new ArrayList<>(List.of("catalogo", "inventario", "pagos", "reportes"));
  for (String nombre : nombres) {
    if (nombre.startsWith("i")) {
      nombres.remove(nombre);
    }
  }
  IO.println(nombres);
}
```

```text
$ java fig04_24.java
Exception in thread "main" java.util.ConcurrentModificationException
	at java.base/java.util.ArrayList$Itr.checkForComodification(ArrayList.java:1096)
	at java.base/java.util.ArrayList$Itr.next(ArrayList.java:1050)
	at fig04_24.main(fig04_24.java:4)
```

  `ConcurrentModificationException` (“concurrent modification”; the name misleads, because there are no threads involved: the list detected that it changed while the traversal was still going). The worst is the silent case: I measured that if the element you remove is the **second to last**, the traversal ends without complaint and skips the last one. With `["catalogo", "inventario", "pagos"]` removing `"inventario"`, the output is `[catalogo, pagos]` and the program never looked at `pagos`. The cost is a program that sometimes loses an element and does not say so. The solution: `lista.removeIf(nombre -> nombre.startsWith("i"))`, which exists precisely for this, or a stream with `filter` that produces a new list.

- **Confusing `remove(int)` with `remove(Object)`.** On a `List<Integer>`, `lista.remove(87)` interprets the 87 as a **position**, not as a value.

```java
// fig04_25.java
void main() {
  List<Integer> tiempos = new ArrayList<>(List.of(142, 2300, 87));
  tiempos.remove(Integer.valueOf(87));
  IO.println(tiempos);
  tiempos.remove(87);
  IO.println(tiempos);
}
```

```text
$ java fig04_25.java
[142, 2300]
Exception in thread "main" java.lang.IndexOutOfBoundsException: Index 87 out of bounds for length 2
	at java.base/jdk.internal.util.Preconditions.outOfBounds(Preconditions.java:100)
	at java.base/jdk.internal.util.Preconditions.outOfBoundsCheckIndex(Preconditions.java:106)
	at java.base/jdk.internal.util.Preconditions.checkIndex(Preconditions.java:302)
	at java.base/java.util.Objects.checkIndex(Objects.java:365)
	at java.base/java.util.ArrayList.remove(ArrayList.java:552)
	at fig04_25.main(fig04_25.java:6)
```

  The first call (`Integer.valueOf(87)`, which asks to remove the value) worked; the second looked for position 87 in a two-element list and threw `IndexOutOfBoundsException`. It is a case where the convenience of *autoboxing* from section 4.2.2 turns against you: the compiler chooses the `remove(int)` method because `87` is an `int`. With lists of texts there is no ambiguity. When you need to remove a number by its value, wrap it on purpose, as in the first call.

- **Using raw types (`List` without `<...>`).** As the erasure figure showed, it takes away the compiler's ability to warn you, and the error appears far from its cause, as `ClassCastException`. Cost: debugging a type error at run time, which the compiler would have flagged on the line of the oversight. Any `[unchecked]` warning is this.

- **Calling `Optional.get()` without checking**, or declaring an `Optional` as a field or parameter. The documentation discourages it, and both practices bring back the problems of `null` under another name. Cost: `NoSuchElementException` in production, and signatures that force the caller to wrap and unwrap values needlessly.

- **Filling an outside collection from a `forEach` or a `map`.** Writing `mediciones.stream().forEach(m -> destino.add(m.nombre()))` modifies an external state from a lambda; it works with a sequential stream and breaks in subtle ways with a parallel one. Cost: fragile code that looks like a badly written `map`; the right tool is `collect` or `toList()`.

- **Assuming the order of a `HashMap`, a `HashSet` or a `Set.of`.** It works on your machine and changes on another, or on the next run. Cost: a report that sometimes comes out in another order and that no test reproduces at first. If the order matters to the reader, choose `LinkedHashMap`, `TreeMap` or a list.

- **Returning or storing the internal list without copying it.** It is the trap of section 4.1.4: other code keeps the reference and modifies your data without you noticing. Cost: values that change by themselves, the kind of error hardest to locate because it occurs far from where the effect is seen.

## Exercises

### Exercise 1 — Add two services to the project

In your `Revisor.java`, add to the list `facturacion` (`https://facturacion.example`) and `notificaciones` (`https://notificaciones.example`). **Before running**, predict what status they will have and what the summary will be. Then run. Then add the two to `Simulador` (`facturacion` responds with code 503 in 95 ms; `notificaciones` does not respond, with the reason “tiempo agotado”, “timed out”), predict the summary again and run. Write in your logbook what happened to the `notificaciones` row.

### Exercise 2 — Sort the report

Make the table come out sorted from the slowest service to the fastest, with those that did not respond at the end, and with ties broken by name. Hint: `Comparator.comparingLong`, `reversed` and `thenComparing`; the time of one that did not respond can be replaced with `orElse(-1)`. Pass the already sorted list to `Reporte.de`.

### Exercise 3 — Which one throws an exception?

For each line, decide without running whether it throws an exception or not, and which. Then check it in a compact source file, running each line alone (one per run: the first exception stops the program):

1. `List.of("catalogo", null)`
2. `Map.of("a", 1, "a", 2)`
3. `new ArrayList<>(List.of("a")).get(5)`
4. `Set.of("a").add("b")`
5. `new HashMap<String, Integer>().put(null, 1)`
6. `new TreeMap<String, Integer>().put(null, 1)`

### Exercise 4 — The names by status

Print, from the `revisor`'s results, a map from each `Estado` to the list of service names that have that status, **in the order in which the statuses are declared** (`ARRIBA`, `LENTO`, `CAIDO`). Hint: `groupingBy` accepts as a second argument a function that makes the map, and `EnumMap` is a map specialized for `enum` keys that orders them by their declaration.

## Solutions

### Solution 1

Without touching `Simulador`, the two new services are not in the responses map, so `buscar` returns an empty `Optional` and `revisar` uses the default `SinRespuesta("sin conexión")`: both come out `CAIDO`. With the change in `Revisor.java` (adding two `new Servicio(...)` to the list), the table is:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO          —  sin conexión
notificacionesCAIDO          —  sin conexión

6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 843ms
```

There are 6 services, 2 up, 1 slow and 3 down; the average does not change (843 ms) because neither of the two new ones responds. Then, in `Simulador`, two pairs are added to the `Map.of` (which accepts up to ten; for more you would have to use `Map.ofEntries`):

```java
"facturacion", new Respuesta.Respondio(503, 95),
"notificaciones", new Respuesta.SinRespuesta("tiempo agotado"));
```

and the result is:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO       95ms  503
notificacionesCAIDO          —  tiempo agotado

6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 656ms
```

`facturacion` is `CAIDO` because its code is 503 (≥ 400), but it **did respond**: it has a time (95 ms) and counts toward the average. The average is now (142 + 2300 + 87 + 95) / 4 = 656. It is the difference between “did not respond” and “responded badly”, and the reason `Resumen` averages over `tiempoMs()` and not over the status.

In the `notificaciones` row there is a visible defect: the name is 14 characters long and the column, 12. `%-12s` does not truncate, so the status ends up stuck to the name (`notificacionesCAIDO`). The format pads but does not invent space. Computing the column width from the longest name is an optional exercise in lesson 10; for now, you know that a fixed format is a contract that the data can break.

### Solution 2

```java
void main() {
  List<Resultado> resultados = Revision.revisarTodos(servicios);
  Comparator<Resultado> porTiempoDescendente =
      Comparator.comparingLong((Resultado resultado) -> resultado.tiempoMs().orElse(-1))
          .reversed()
          .thenComparing(resultado -> resultado.servicio().nombre());
  List<Resultado> ordenados = resultados.stream().sorted(porTiempoDescendente).toList();
  IO.println(Formato.tabla(Reporte.de(ordenados)));
}
```

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
inventario  LENTO       2.3s  200
catalogo    ARRIBA     142ms  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Notice the type written by hand in the lambda: `(Resultado resultado) -> ...`. It is not decoration. If you omit it and write `Comparator.comparingLong(resultado -> resultado.tiempoMs().orElse(-1)).reversed()`, the compiler cannot deduce the type of `resultado` (because `reversed()` is chained before it knows where it will be assigned) and takes it as `Object`:

```text
Revisor.java:4: error: cannot find symbol
      Comparator.comparingLong(resultado -> resultado.tiempoMs().orElse(-1)).reversed();
                                                     ^
  symbol:   method tiempoMs()
  location: variable resultado of type Object
```

It is a known inference error: the compiler deduces a lambda's types by looking at where it is going, and a method chain hides the destination from it. With the explicit type on the first comparator, the chain works. The final `thenComparing` does not need a type because it already knows it is over `Resultado`. The services that did not respond (`reportes`) end up last because their `-1` is the smallest and the order is descending. With ties between two that did not respond, `thenComparing` breaks them by name.

### Solution 3

| Line | What happens |
|---|---|
| `List.of("catalogo", null)` | `NullPointerException`: immutable collections do not admit `null` |
| `Map.of("a", 1, "a", 2)` | `IllegalArgumentException: duplicate key: a` |
| `new ArrayList<>(List.of("a")).get(5)` | `IndexOutOfBoundsException: Index 5 out of bounds for length 1` |
| `Set.of("a").add("b")` | `UnsupportedOperationException` |
| `new HashMap<String, Integer>().put(null, 1)` | throws nothing: a `HashMap` accepts a `null` key and ends up `{null=1}` |
| `new TreeMap<String, Integer>().put(null, 1)` | `NullPointerException`: a `TreeMap` needs to compare the keys and cannot with `null` |

I measured all six with JDK 25. Two useful conclusions. Immutable collections are strict from the moment they are created, not when used: `List.of("catalogo", null)` already fails at creation. And that `HashMap` accepts `null` as a key and `TreeMap` does not is an example that the implementations of the same interface **are not interchangeable in everything**: the common contract (`Map`) covers the basics, and the details of each are stated by its documentation.

### Solution 4

```java
void main() {
  List<Resultado> resultados = Revision.revisarTodos(servicios);
  Map<Estado, List<String>> nombresPorEstado =
      resultados.stream()
          .collect(
              Collectors.groupingBy(
                  Resultado::estado,
                  () -> new EnumMap<>(Estado.class),
                  Collectors.mapping(
                      resultado -> resultado.servicio().nombre(), Collectors.toList())));
  IO.println(nombresPorEstado);
}
```

```text
{ARRIBA=[catalogo, pagos], LENTO=[inventario], CAIDO=[reportes]}
```

`groupingBy` with three arguments: the classifying function (`Resultado::estado`), the map's factory (`() -> new EnumMap<>(Estado.class)`, a lambda that receives nothing and returns a new map) and the collector for each group (`mapping` extracts the name and `toList` gathers them). `EnumMap` receives the `enum`'s class (`Estado.class`) and traverses its keys in the declaration order: `ARRIBA`, `LENTO`, `CAIDO`. With a `HashMap` the order would not be promised. `EnumMap` is made for `enum` keys and makes it clear, to whoever reads, that they are.

## How I know I got it

- [ ] `java fig04_01.java` prints the list, `primero y último: catalogo, pagos` (“first and last”), and you can say why `remove("pagos")` removes a value and not a position.
- [ ] `java fig04_04.java` ends with `UnsupportedOperationException`, and you can point out in the trace which line is from your program and which are from the JDK.
- [ ] `java fig04_05.java` prints that the naive report changed and the safe one did not, and you can explain why the second is protected with `List.copyOf`.
- [ ] `java fig04_10.java` fails with a `ClassCastException` on a line that is not the one that made the error, and you know what made it possible (a raw type).
- [ ] `java fig04_15.java` prints “el stream está armado; todavía no pasó nada” before any “miro”, and you can explain why.
- [ ] From the project folder, `java Revisor.java` prints the table of four services with `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`.
- [ ] With the two services of exercise 1 in `Simulador`, the summary says `6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 656ms`, and you know why `facturacion` counts toward the average even though it is down.
- [ ] You can explain why `Resumen.de` uses `getOrDefault(..., 0L)` and what would happen without it.
- [ ] You wrote in your logbook a *stream* that you rewrote as a loop, or the other way around, and which one read better.

## Summary

Answer without looking at the lesson:

1. Why is `List<String> x = new ArrayList<>()` declared and not `ArrayList<String> x = ...`?
2. What traversal order does a `HashMap`, a `LinkedHashMap` and a `TreeMap` have? Which would you use for a report?
3. What two things do `Set.add` and `Map.put` return, and what are they for?
4. What does `List.of("a").add("b")` throw? And `List.of("a", null)`?
5. What is the difference between `List.copyOf(x)` and `Collections.unmodifiableList(x)`?
6. Why does `List<int>` not exist? What trap comes with comparing two `Integer`s with `==`?
7. What does it mean that generics are “erased”? Give one consequence.
8. Which three parts does a *stream* have? When is the work executed?
9. What happens if you traverse the same *stream* twice?
10. What does `average()` return and why not a `double`?
11. When do you prefer a loop to a *stream*?
12. Why is `get()` not called without checking on an `Optional`? What do you use instead?

## Further reading

- [Oracle, “Java Collections Framework”](https://docs.oracle.com/en/java/javase/25/core/java-collections-framework.html): the official guide to the interfaces and implementations, with the table of which to use when. Consulted on October 7, 2026.
- [Oracle, “Generics” (Java tutorial)](https://docs.oracle.com/javase/tutorial/java/generics/index.html): from generic types to wildcards and erasure, with more extensive examples than this lesson. Consulted on October 7, 2026.
- [Oracle, “Stream API”](https://docs.oracle.com/en/java/javase/25/core/stream-api.html): the guide to streams, with the difference between intermediate and terminal operations and the collectors. Consulted on October 7, 2026.
- [`Optional` documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html): includes the API note on when to use it. Consulted on October 7, 2026.

---

**Previous:** [Lesson 3 — Records, enums and sealed types](03-records-enums-sellados.md) ·
**Next:** [Lesson 5 — Errors and exceptions](05-errores-excepciones.md)
