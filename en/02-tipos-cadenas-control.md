# Lesson 2 — Types, strings and control flow

**Time:** 2 × 90 min (two sessions; the break is between sections 2.2 and 2.3)

**What you build:** the `revisor`'s base functions: deciding whether a service is up, slow or down, formatting the time and assembling the rows of the table, all in a single compact source file

**What you learn:** primitive and reference types, wrappers and `var`; `String` is immutable and why `==` is not `equals`; formatting with `String.format` and `Locale.ROOT`; text blocks; `if`, `&&` versus `&` and the `? :` operator; `switch` as an expression; methods and constants in a compact source file; `null` and how to read the message of a `NullPointerException`

**Where you are coming from.** You bring the environment from [Lesson 1](01-instalacion.md): a JDK 25 that answers to `java --version`, the habit of running a file with `java Archivo.java` and a one-line `~/curso-java/revisor/Revisor.java` that prints “revisor: todavía no revisa nada” (“revisor: does not review anything yet”). You also know how to read the compiler's first error: the file, the line number, the `^` that marks the spot and the `symbol` and `location` lines. This lesson uses that skill all the time, because Java is a language that **scolds you before running**, and learning to understand its scolding is half the trade.

**What this lesson does not do.** It uses no lists, no loops, no classes of your own, and queries no network. The data of the `revisor`'s four services is written in two parallel `switch`es, on purpose: it is an awkward solution, and [Lesson 3](03-records-enums-sellados.md) replaces it with types that express what “a service responded” and “a service did not respond” mean. Collections arrive in Lesson 4; exceptions, in 5. Here, when a program fails on purpose, we use it to **read** the error, not to catch it.

**About the runs.** Each figure is a file `fig02_NN.java` that is run with `java fig02_NN.java`, from the folder where you saved it. You already have them written in the repository, in [`programas/02-tipos-cadenas-control/`](../programas/02-tipos-cadenas-control/), but you learn more if you type them. All the outputs in this lesson were obtained with Temurin 25.0.4 and the `LANG` variable set to `C.UTF-8`, which is what makes the terminal print accents and the dash “—” correctly; with `LANG=C`, Java prints `?` in their place. If your Java patch is another, the error messages are the same; the only thing that changes between updates are the line numbers of the `at java.base/...` lines in a trace, which belong to the JDK and not to you. The figures that fail on purpose carry their error beside them, in a file `fig02_NN.error-esperado.txt`.

## By the end you will be able to

- Choose among `int`, `long`, `double` and `boolean` for a datum, and explain what happens to an `int` that exceeds its maximum.
- Tell a primitive value from a reference and from a wrapper (`int` versus `Integer`), and say when a variable can be `null`.
- Use `var` where it helps and know which type the compiler gave it.
- Explain why `s.strip();` does not change `s`, and why `==` between two `String`s can give `false` even though they look equal.
- Format a text with `String.format` and `Locale.ROOT`, and write a multi-line text block.
- Write a `switch` as an expression with arrows, and explain why `&&` and `&` are not the same.
- Read the message of a `NullPointerException`, know which variable was null and why the message sometimes says `<local1>` instead of its name.

## The why before the how

You arrive at Java with a language on your back, and that language left you habits. Some are useful here as they are: an `if` is an `if`. Others are traps, because Java looks similar enough to what you already know that you do not suspect. Think of three expressions that, in the language you bring, perhaps mean one thing, and in Java mean another:

- Comparing two texts with `==`. In Python or Go it compares their content. In Java it compares whether they are **the same object**, and sometimes it tells you `true` by chance.
- Calling `texto.strip()` and thinking `texto` is now clean. In Java the text **never changes**: the method returns another one, and if you do not save it, it is lost without warning.
- Writing `x != null & x.length() > 0` believing it is the same as with `&&`. It blows up with a `NullPointerException` exactly when `x` is null, that is, exactly when the check was useful.

There is a study with hundreds of thousands of Java students (Brown and Altadmri, with BlueJ data) that measured which errors take longest to fix. The teachers who took part did not get it right: their intuition about which ones were hard did not match the data. The ones that take longest are precisely these three: confusing `==` with `equals`, confusing `&` with `&&`, and not using the value a method returns. That is why this lesson does not mention them in passing: **each one is provoked on purpose, seen failing and explained**. You can read the article on the [University of Kent page](https://kar.kent.ac.uk/57219/).

The path has two sessions. The **first** covers 2.1 and 2.2: types and strings. When you finish it you will know what each variable holds and what a text does when you “modify” it. The **second** covers 2.3 to 2.5: control flow, `null` and the project step. When you finish it you will have the `revisor` printing this table, computed by your own functions:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

That table is the thread of the whole course. Notice the last row: `reportes` did not respond, so it has neither a time nor a code (the program prints `sin conexión`, “no connection”). How to represent “there is no datum” in a strictly typed language is the question that runs through the whole lesson, and its bad answer —`null`— is the reason for half the errors you are going to see.

## The concepts

### 2.1 Types

#### 2.1.1 Primitives: loose values of fixed size

In Java every variable has a **type** that you write, before the name, and that **never changes**. `int codigo = 200;` declares a variable called `codigo` (“code”) that can only hold integers of that size. If later you try to store a text in it, the compiler refuses. You will remember it from Go or Rust; if you come from Python or JavaScript, it is the biggest change: type errors appear **before** running, not during.

Java's types are divided into two families. The first are the **primitives**: eight types, built into the language, that hold the value directly, with no object in between. In the `revisor` you only need four, but it pays to see all eight:

| Type | What it holds | Size | Range or example |
|---|---|---|---|
| `byte` | very small integer | 8 bits | from −128 to 127 |
| `short` | small integer | 16 bits | from −32 768 to 32 767 |
| `int` | integer (the common one) | 32 bits | from −2 147 483 648 to 2 147 483 647 |
| `long` | large integer | 64 bits | up to about 9.2 × 10¹⁸ |
| `float` | single-precision decimal | 32 bits | rarely used |
| `double` | double-precision decimal | 64 bits | the common decimal |
| `boolean` | true or false | not fixed | `true` or `false` |
| `char` | one UTF-16 unit of text | 16 bits | `'c'`, in single quotes |

The ranges and sizes are not decided by your computer or your operating system: they are fixed by the [language](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2), and that is why an `int` measures the same everywhere. The names are written in lowercase; that warns you they are not classes.

Look at the program `fig02_01.java`. It declares one datum of each of the four types the `revisor` will use (plus a `char`, to complete the picture) and does a little arithmetic. A note on syntax: the `+` between a text and anything else **converts that thing to text and glues it on**, which is how each output line is assembled. And `IO.println` is the one you saw in Lesson 1; its sister `IO.print` does the same but without a line break at the end.

```java
// fig02_01.java
void main() {
  int codigo = 200;
  long tiempoMs = 2300L;
  double segundos = tiempoMs / 1000.0;
  boolean lento = tiempoMs >= 1000;
  char inicial = 'c';

  IO.println("código:    " + codigo);
  IO.println("tiempo:    " + tiempoMs);
  IO.println("segundos:  " + segundos);
  IO.println("lento:     " + lento);
  IO.println("inicial:   " + inicial);

  IO.println("7 / 2      = " + 7 / 2);
  IO.println("7 / 2.0    = " + 7 / 2.0);
  IO.println("2300 / 1000 = " + 2300 / 1000);
  IO.println("0.1 + 0.2  = " + (0.1 + 0.2));
}
```

```text
$ java fig02_01.java
código:    200
tiempo:    2300
segundos:  2.3
lento:     true
inicial:   c
7 / 2      = 3
7 / 2.0    = 3.5
2300 / 1000 = 2
0.1 + 0.2  = 0.30000000000000004
```

There are three things in that output worth pausing on.

**Integer division discards the decimal part.** `7 / 2` gives `3`, not `3.5`, because both operands are `int` and the result is an `int`. To get `3.5` it is enough for one of the two to be `double`: `7 / 2.0`. The same happens with `2300 / 1000`, which gives `2`, not `2.3`. This surprises someone coming from Python 3 (where `/` always gives a decimal) or from JavaScript (where everything is decimal), and not someone coming from Go or Rust. It is the most common silent error in time calculations: if you compute seconds as `tiempoMs / 1000` you get **whole** seconds, and 999 ms are 0 s.

**The variable `tiempoMs` is a `long` and its value ends in `L`.** `2300L` is a literal of type `long`; without the `L`, `2300` would be an `int`. For small numbers the difference is not noticeable, because Java converts an `int` to a `long` by itself (it is a conversion that loses nothing). With large numbers the `L` is mandatory, as you will see in a moment.

**`0.1 + 0.2` gives `0.30000000000000004`.** It is not a defect of Java: it is the way any computer stores binary decimals (the IEEE 754 standard), and it happens to you equally in Python, JavaScript, Go and Rust. What matters is the consequence: **never compare decimals with `==`**, and never store money in a `double`. The `revisor` stores its times in **whole** milliseconds for that reason: it is a number that does not need decimals.

Now, the trap that is indeed particular to Java and to the languages that share its model of fixed-size integers. What happens when a number exceeds its maximum? Look at `fig02_02.java`:

```java
// fig02_02.java
void main() {
  int maximo = Integer.MAX_VALUE;
  IO.println("máximo de un int: " + maximo);
  IO.println("máximo + 1:       " + (maximo + 1));

  long mayor = Long.MAX_VALUE;
  IO.println("máximo de un long: " + mayor);

  int milisegundosEnUnDia = 24 * 60 * 60 * 1000;
  IO.println("ms en un día:      " + milisegundosEnUnDia);
  int milisegundosEnUnMes = 30 * 24 * 60 * 60 * 1000;
  IO.println("ms en 30 días (int):  " + milisegundosEnUnMes);
  long enUnMes = 30L * 24 * 60 * 60 * 1000;
  IO.println("ms en 30 días (long): " + enUnMes);
}
```

```text
$ java fig02_02.java
máximo de un int: 2147483647
máximo + 1:       -2147483648
máximo de un long: 9223372036854775807
ms en un día:      86400000
ms en 30 días (int):  -1702967296
ms en 30 días (long): 2592000000
```

An `int` that reaches its maximum and receives one more **wraps around** and becomes the minimum, with no warning at all: there is no error, no exception, just a negative number where you expected an enormous one. In the program, thirty days in milliseconds do not fit in an `int` (the correct result is 2 592 000 000, which is more than the maximum of 2 147 483 647), and the value that comes out is `-1702967296`. The fix is to do the calculation in `long`: it is enough to write an `L` on **one** of the numbers, `30L * 24 * 60 * 60 * 1000`, and the whole calculation is done in `long`. Notice the difference between the last two lines: it is exactly one letter.

A reading detail of that figure: `Integer.MAX_VALUE` and `Long.MAX_VALUE` are constants provided by the `Integer` and `Long` classes, which we talk about right away. And the first line of each figure's `main` in this lesson is the one you already know: `void main()`, the compact source file from Lesson 1.

#### 2.1.2 References, wrappers and `null`

The second family of types are the **reference** ones: everything that is not one of the eight primitives. `String` is a reference type. So are `Integer`, `Long`, `URI` or any class you write. The difference from a primitive is deep, and is understood with an image. A primitive variable is **a box with the value inside**: `int codigo = 200` is a box that says 200. A reference variable is **a box with an address inside**: the address of where the object is. Two reference variables can have the same address (and then they are the same object), or different addresses that point to objects that look alike. That distinction is the one that will be needed in 2.2.2.

And a reference variable can have, instead of an address, **none**. That “none” is written `null`. Primitives cannot be `null`: an `int` is always worth some number. This difference matters for the `revisor`, because `reportes` did not respond and its HTTP code is not “zero” or “minus one”: it **does not exist**. With an `int` there is no honest way to say that. With a reference there is.

That is why Java provides a **wrapper** for each primitive: `Integer` for `int`, `Long` for `long`, `Double` for `double`, `Boolean` for `boolean`, `Character` for `char`. A wrapper is an object whose only job is to contain a primitive value, and since it is a reference, **it can be `null`**. Java converts between the primitive and its wrapper automatically, in both directions; it is called *autoboxing* when putting the value in the box (from `int` to `Integer`) and *unboxing* when taking it out. Look at `fig02_03.java`, which brings together what we just said:

```java
// fig02_03.java
void main() {
  int codigo = 200;
  Integer codigoCaja = 200;
  Integer sinRespuesta = null;

  IO.println("primitivo:  " + codigo);
  IO.println("envoltorio: " + codigoCaja);
  IO.println("nulo:       " + sinRespuesta);
  IO.println("¿iguales?   " + codigoCaja.equals(codigo));

  Integer a = 127;
  Integer b = 127;
  IO.println("127 == 127:   " + (a == b));
  Integer c = 2300;
  Integer d = 2300;
  IO.println("2300 == 2300: " + (c == d));
  IO.println("2300 equals:  " + c.equals(d));
}
```

```text
$ java fig02_03.java
primitivo:  200
envoltorio: 200
nulo:       null
¿iguales?   true
127 == 127:   true
2300 == 2300: false
2300 equals:  true
```

Three lines deserve comment. `Integer sinRespuesta = null;` is a perfectly valid declaration (`sinRespuesta` means “no response”), and when printed it comes out `null`. `codigoCaja.equals(codigo)` calls the wrapper's `equals` method with an `int`, and Java puts it in a box for you to compare it: it gives `true`. And the two comparisons with `==` are the **surprise of this section**: `127 == 127` gives `true` and `2300 == 2300` gives `false`, even though on both lines two wrappers with the same value are compared. That is because `==` between two references asks “are they the same object?”, and not “are they worth the same?”. Java reuses the `Integer` objects from −128 to 127 (it is a guarantee of the [specification](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.1.7)), so `127` and `127` are the same object and `2300` and `2300` are two different objects. With `==` between wrappers your program works with the small HTTP codes and fails with the large times, which is the worst kind of error: the one that depends on the data.

The practical rule is short. **Between primitives, `==` compares values. Between references, it compares identity, and to compare values you use `equals`.** And a warning specific to wrappers: `Long.equals` also compares the type. A `Long` with 142 is never equal to an `Integer` with 142; `equals` returns `false` if you pass it the other type. When you compare a `Long` with a literal, write the `L`.

In the `revisor` you will use wrappers for exactly what they are for: `Integer codigoDe(String nombre)` returns the service's HTTP code, or `null` if the service did not respond; `Long tiempoDe(String nombre)` does the same with the time. Lesson 3 will replace this trick with something better, but to understand why it is better you have to have suffered it.

#### 2.1.3 Types are checked before running

Since each variable has its fixed type, the compiler checks that what you assign to it **fits**. It will scold you with a message worth recognizing, because you will see it a lot. Look at `fig02_04.java`, where a `long` tries to squeeze into an `int`:

```java
// fig02_04.java
void main() {
  long tiempoMs = 2300L;
  int tiempoEntero = tiempoMs;
  IO.println(tiempoEntero);
}
```

```text
$ java fig02_04.java
fig02_04.java:4: error: incompatible types: possible lossy conversion from long to int
  int tiempoEntero = tiempoMs;
                     ^
1 error
error: compilation failed
```

Read it with what you learned in Lesson 1. First the file and the line (`fig02_04.java:4`), then the kind of error (`incompatible types`), then the detail (“a conversion that could lose information, from `long` to `int`”), and the `^` that points to the place. The launcher closes with `error: compilation failed`.

The message says something important with the word **possible**. Java does not know whether the value fits: it sees the type, not the content. A `long` *could* be worth 3 000 000 000, and an `int` cannot hold that, so the compiler forces you to decide. You have three ways out, and the right one depends on the case. Changing the destination type to `long` is almost always the best. Writing a **cast** like `(int) tiempoMs` tells the compiler “I know what I am doing”, and it is dangerous: there is no protection, and `(int) 3_000_000_000L` gives `-1294967296`, the same silent overflow as in 2.1.1. The third is not to mix the types from the start. The cast is used, very carefully, when you really want to truncate, for example `(int) 2.9` gives `2`.

In the opposite direction there is no complaint: an `int` fits in a `long` and a `long` in a `double`, because there the order of magnitude is never lost. That is why `double segundos = tiempoMs / 1000.0` in `fig02_01.java` compiled without ceremony.

#### 2.1.4 `var`: letting the compiler write the type

Writing the type on every variable gives security but is repetitive when the value on the right side already says it all. That is what `var` is for. Look at `fig02_05.java`:

```java
// fig02_05.java
void main() {
  var nombre = "catalogo";
  var codigo = 200;
  var tiempoMs = 2300L;
  var segundos = tiempoMs / 1000.0;

  IO.println(nombre + " " + codigo + " " + tiempoMs + " " + segundos);
}
```

```text
$ java fig02_05.java
catalogo 200 2300 2.3
```

Each `var` is a variable of fixed type, just as if you had written it: `nombre` is a `String`, `codigo` is an `int`, `tiempoMs` is a `long` (because of the `L`) and `segundos` is a `double`. **`var` does not make Java dynamic**: the compiler deduces the type **once**, from the value you initialize with, and from then on the variable cannot change it. If you later try `nombre = 5;`, the compiler complains just as before. The [proposal that introduced it](https://openjdk.org/jeps/286) (JEP 286, Java 10) limits it to local variables with an initializer: it does not apply to fields, parameters or return types.

Without a value to deduce from, there is no type. Look at what happens with `fig02_06.java`:

```java
// fig02_06.java
void main() {
  var respuesta = null;
  IO.println(respuesta);
}
```

```text
$ java fig02_06.java
fig02_06.java:3: error: cannot infer type for local variable respuesta
  var respuesta = null;
      ^
  (variable initializer is 'null')
1 error
error: compilation failed
```

`null` by itself does not say what type it is, and the compiler refuses to guess. It is the same problem as with wrappers, seen from another side: a “nothing” value needs a type to give it meaning.

**When to use `var`.** In this course we use it when the type is obvious on the same line (`var nombre = "catalogo";`) and avoid it when it hides information (`var resultado = revisar(servicio);` does not tell you what `resultado` is without going to read `revisar`). It is a reading criterion, not a rule of the language: code is read many more times than it is written.

### 2.2 Strings

#### 2.2.1 `String` is an object, and it is immutable

`String` is a class of the standard library, so each text is an object with methods: they are written after a dot, just as in Python or JavaScript. The most used are `length()` (how many characters), `strip()` (removes the spaces at the ends), `isBlank()` (is it empty or only spaces?), `toUpperCase()`, `contains(otro)`, `startsWith(prefijo)` and `substring(desde, hasta)` (a piece; the second number is not included). See how they are used in `fig02_07.java`, and pay particular attention to the first two lines of output:

```java
// fig02_07.java
void main() {
  String linea = "  catalogo  ";
  linea.strip();
  IO.println("[" + linea + "]");

  linea = linea.strip();
  IO.println("[" + linea + "]");

  IO.println("longitud:    " + linea.length());
  IO.println("mayúsculas:  " + linea.toUpperCase());
  IO.println("¿contiene 'log'? " + linea.contains("log"));
  IO.println("¿empieza con 'cat'? " + linea.startsWith("cat"));
  IO.println("recorte:     " + linea.substring(0, 3));
  IO.println("¿vacía?      " + "   ".isBlank());
}
```

```text
$ java fig02_07.java
[  catalogo  ]
[catalogo]
longitud:    8
mayúsculas:  CATALOGO
¿contiene 'log'? true
¿empieza con 'cat'? true
recorte:     cat
¿vacía?      true
```

**The first line of output is the error.** We wrote `linea.strip();` and the text kept its spaces: `[  catalogo  ]`. There was no warning or error. It is **the ignored return value**, one of the three errors that take longest to fix according to the study I mentioned. The cause is that Java's `String`s are **immutable**: once created, a text does not change. The [documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html) puts it this way: strings are constant and their values cannot be changed after they are created. All the methods that seem to “modify” (`strip`, `toUpperCase`, `substring`...) actually **return a new text** and leave the original untouched. If you do not save it, you threw it away.

The correct line is the one below: `linea = linea.strip();`, which reassigns the **variable** so that it points to the new text. Notice what changed: not the object, but what the variable points to. It is the model of boxes with addresses from 2.1.2.

Why was it designed this way? Because an immutable text can be shared without fear: if a hundred parts of the program hold the same `String`, none of them can spoil it for the others. It is a property you already know from Python (`str`) and JavaScript (primitive strings), and that you will see again in Java with the *records* of Lesson 3. The cost is that the programmer must remember the rule.

A natural question: if each “modification” creates a new text, isn't it very expensive to build a long text by gluing pieces together? In a loop with thousands of iterations it is, and for that there is a separate class, `StringBuilder`. You do not need it in this course: the `revisor` assembles short lines, and `String.format` (2.2.3) already does the job.

#### 2.2.2 `==` versus `equals`

This is the most important error in the lesson. Look at `fig02_08.java`, which builds the text `inventario` (“inventory”) in two ways:

```java
// fig02_08.java
void main() {
  String prefijo = "inven";
  String armado = prefijo + "tario";
  String literal = "inventario";
  String constante = "inven" + "tario";

  IO.println("armado == literal:       " + (armado == literal));
  IO.println("armado.equals(literal):  " + armado.equals(literal));
  IO.println("constante == literal:    " + (constante == literal));
  IO.println("mayúsculas distintas:    " + "INVENTARIO".equals(literal));
  IO.println("ignorando mayúsculas:    " + "INVENTARIO".equalsIgnoreCase(literal));
}
```

```text
$ java fig02_08.java
armado == literal:       false
armado.equals(literal):  true
constante == literal:    true
mayúsculas distintas:    false
ignorando mayúsculas:    true
```

The first three lines tell a story that reads better in order.

`armado == literal` gives **`false`**, even though both texts contain `inventario`. `armado` (“assembled”) was built **at run time**, gluing the value of the variable `prefijo` to `"tario"`, so it is a new object at a new address. `literal` is an object the compiler created once, and `==` asks whether both variables point to the same one. They do not. `armado.equals(literal)` gives `true`, because `equals` compares the **content**, character by character.

The third line is the trap inside the trap. `constante == literal` gives **`true`**, even though `constante` was also written with a `+`. The reason: `"inven" + "tario"` joins two literals, and the compiler knows the result before running, so it computes it itself and treats it as the literal `"inventario"` (the [specification](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.29) calls it a “constant expression”). Literal texts with the same content are **the same object** across the whole program. That is why an `==` between texts usually works in a small toy test and fails as soon as the text arrives from **outside**: from a file, a keyboard, a network, a `+` with a variable. It is the kind of error that passes every test you write and fails in production.

The last two lines show what is written instead. `equals` distinguishes capital letters, and `equalsIgnoreCase` does not. And a habit worth gold: **put the literal first**. `"INVENTARIO".equals(literal)` cannot throw a `NullPointerException`, because the literal is never null; by contrast, `variable.equals("inventario")` blows up if `variable` is `null`. With the literal on the left, if the variable is null the answer is simply `false`.

**This lesson's rule, the most important one:** between references (texts, wrappers, any object), `==` asks whether it is the same object; `equals` asks whether they are worth the same. Almost always you want the second. Use `==` between primitives, and between `enum`s (Lesson 3), where it is indeed correct.

Why doesn't Java make `==` compare content on texts, like Python? Because Java does not let the programmer change the meaning of an operator, and `==` means the same on all reference types. It is a design decision that has been causing trouble for decades, and that is not going to change.

#### 2.2.3 Formatting: `String.format` and `Locale.ROOT`

Each row of the `revisor`'s table has aligned columns: the name takes up 12 spaces, the status 10, and so on. Building that with `+` and spaces by hand is a nightmare, and for that Java provides `String.format`: it receives a **template** with marks and the values that fill them. The marks you need today:

- `%s` puts in a text (or any value, converted to text).
- `%d` puts in an integer.
- `%.1f` puts in a decimal with one decimal after the point.
- `%-12s` puts in a text **taking up 12 spaces, left-aligned** (without the hyphen, right-aligned). `%6s` takes up 6 and aligns right.

And there is a problem you will not see until your program runs on someone else's computer: decimals are written differently depending on the language. In Mexico the decimal separator is the point (`2.3`), in Spain it is the comma (`2,3`). If you ask `String.format` to format a decimal without telling it the language, it **uses that of the machine where it runs**. Your program would come out with points on your laptop and with commas on a colleague's. To prevent that, you pass it as the first argument a `Locale`, the object that represents a language and a region. Look at `fig02_09.java`:

```java
// fig02_09.java
void main() {
  double segundos = 2.3;
  Locale mexico = Locale.of("es", "MX");
  Locale espana = Locale.of("es", "ES");

  IO.println(String.format(Locale.ROOT, "%.1fs", segundos));
  IO.println(String.format(mexico, "%.1fs", segundos));
  IO.println(String.format(espana, "%.1fs", segundos));
  IO.println(String.format(Locale.ROOT, "[%-12s][%6s][%d]", "catalogo", "142ms", 200));
  IO.println("%s tardó %d ms".formatted("pagos", 87));
}
```

```text
$ java fig02_09.java
2.3s
2.3s
2,3s
[catalogo    ][ 142ms][200]
pagos tardó 87 ms
```

`Locale.of("es", "MX")` builds Mexico's Spanish and `Locale.of("es", "ES")` Spain's. With Spain's, the same `%.1f` gives **`2,3s`**. `Locale.ROOT` is the “neutral language”, which uses the most stable format (decimal point, no thousands separators), and **it is the one used in the course whenever a number with decimals is formatted**. The reason is not nationalistic: it is that the `revisor`'s table must come out identical on any machine, and a program whose output depends on the locale is a program that a test cannot verify.

The fourth line of output is the template for the table's rows in miniature (`%-12s` for the name, `%6s` for the time and `%d` for the code), with the brackets so you can see how much space each column takes. The last one uses `formatted`, a version of the same mechanism that is called on the text itself: `"...".formatted(values)` is equivalent to `String.format("...", values)` (without the `Locale`: use it only with texts and integers).

#### 2.2.4 Text blocks

Sometimes you need a text of several lines: a template, a piece of configuration, an example JSON. Writing it with `"` and `\n` on every line is horrible. Java has **text blocks**, delimited by three double quotes. They were introduced as a final feature in Java 15 ([JEP 378](https://openjdk.org/jeps/378)). Look at `fig02_10.java`:

```java
// fig02_10.java
void main() {
  String encabezado = """
      # nombre      url
      catalogo      https://catalogo.example
      pagos         https://pagos.example
      """;
  IO.print(encabezado);
  IO.println("renglones con salto final: " + encabezado.endsWith("\n"));

  String cita = """
      El servicio "%s" tardó %d ms.\
       Revísalo.""".formatted("inventario", 2300);
  IO.println(cita);
}
```

```text
$ java fig02_10.java
# nombre      url
catalogo      https://catalogo.example
pagos         https://pagos.example
renglones con salto final: true
El servicio "inventario" tardó 2300 ms. Revísalo.
```

The rule that makes it useful is how it treats indentation. The block's text starts on the line below the three opening quotes, and Java **removes the indentation that all the lines share** (the compiler computes it from the least-indented line, also counting the closing `"""` line). That is why the printed text starts at `# nombre` without the six spaces with which we wrote the block in the code. If you move the closing `"""` to the left, you keep more indentation; if you leave it at the end of the last line of text, there is no line break at the end.

Two more details. Inside a block you can write double quotes without escaping them (`"%s"` in the second block). And a backslash `\` at the end of the line **does not insert the break**: it joins the line to the next one. In the second block, the `\` makes the sentence come out in one go, with a space that comes from the extra indentation of the next line (seven spaces, one more than the six common ones). And notice the use of `.formatted(...)` right after the closing: a text block is a `String` like any other. The line with `endsWith("\n")` checks that the first block ends with a line break, which in a normal text is written `\n`; that is why `IO.print`'s output leaves the three lines together and the next `IO.println` starts on a clean line.

That ends the first session. Before going on, check that you can answer, without looking: what is the difference between `int` and `Integer`? What does `armado == literal` print in `fig02_08.java` and why? What happens if you write `linea.strip();` and nothing else? If any of the three is hard for you, reread the corresponding section before the second session.


### 2.3 Control flow

#### 2.3.1 `if`, conditions and the `? :` operator

Java's `if` is the one you already know, with two particularities. The first: **the condition has to be a `boolean`**, and nothing else. In Python or JavaScript, a number, a text or a list can act as a condition (they are called “truthy” or “falsy” depending on their content); in Java, `if (codigo)` with an `int` does not even compile:

```text
Prueba.java:3: error: incompatible types: int cannot be converted to boolean
  if (codigo) {
      ^
```

It is an advantage disguised as an annoyance: it forces you to write what you really want to ask (`codigo >= 400`, `texto != null`, `!texto.isBlank()`), and the errors of “zero also counts as false” are over. The second particularity is one of style: **braces always**, even if the `if` has a single line. It is what Google's style guide asks for, which all the course's code follows (two-space indentation, lines of up to 100 columns), and which Checkstyle will verify in Lesson 10; if you write them from today, you will not have to fix them later. An `if` without braces is the recipe for a classic error: someone adds a second line below, with the correct indentation, and the second line is not part of the `if`.

Look at `fig02_11.java`. It brings together an `if` with `else if`, a boolean variable, and the **ternary operator**, which is an `if` in the form of an expression: `condition ? valueIfTrue : valueIfFalse`.

```java
// fig02_11.java
void main() {
  int codigo = 503;
  long tiempoMs = 95;

  if (codigo >= 400) {
    IO.println("CAIDO: el servicio contestó " + codigo);
  } else if (tiempoMs >= 1000) {
    IO.println("LENTO");
  } else {
    IO.println("ARRIBA");
  }

  boolean respondio = codigo != 0 && tiempoMs > 0;
  IO.println("¿respondió? " + respondio);
  String mensaje = respondio ? "con respuesta" : "sin respuesta";
  IO.println(mensaje);
}
```

```text
$ java fig02_11.java
CAIDO: el servicio contestó 503
¿respondió? true
con respuesta
```

The condition `codigo >= 400` is the `revisor`'s first rule: a service that answers with an error code (400 or more) is down, even though it responded. Notice that the `else if` is only evaluated if the first one was false, and that the branches are mutually exclusive. The ternary is used to choose a value, not to execute actions; when the expression on each side gets long, a normal `if` reads better.

#### 2.3.2 `&&` versus `&`

Here comes the second error from the list of those slow to fix. Java has two operators for “and”: `&&` and `&` (and two for “or”: `||` and `|`). Both do the same with booleans, with a difference that matters: `&&` **stops as soon as it knows the answer**. If the left side is false, the result is false no matter the right, and the right **is not even evaluated**. It is called short-circuit evaluation. `&` **always evaluates both sides**. (With integer numbers, `&` is something else: an operation on bits. We do not use it in this course, but it explains why it exists.)

Look at the program `fig02_12.java`. It wants to ask: “does the detail exist and is it also blank?”:

```java
// fig02_12.java
void main() {
  String detalle = null;
  if (detalle != null & detalle.isBlank()) {
    IO.println("detalle vacío");
  }
  IO.println("fin");
}
```

```text
$ java fig02_12.java
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.isBlank()" because "<local1>" is null
	at fig02_12.main(fig02_12.java:4)
```

`detalle` (“detail”) is worth `null`, so `detalle != null` is false... but the `&` evaluated the right side **anyway**, `detalle.isBlank()`, and calling a method on `null` blows up. It is the check you put in to avoid the problem, causing the problem. With `&&` the right side is not touched when the left fails. It is `fig02_13.java`:

```java
// fig02_13.java
void main() {
  String detalle = null;
  if (detalle != null && detalle.isBlank()) {
    IO.println("detalle vacío");
  }
  IO.println("fin sin errores");
}
```

```text
$ java fig02_13.java
fin sin errores
```

This pattern, **check that it is not null and, only if it is not, use it**, is the reason for `&&` in the `revisor`. You will use it in `clasificar`, and you are going to write it hundreds of times in your life with Java. The rule is: in a condition, `&&` and `||`; `&` and `|` are left for operations on bits. It is one of those faults that the compiler does not flag, because both are valid; your attention catches them.

#### 2.3.3 `switch` as an expression

Java's classic `switch`, inherited from C, has a famous defect: **if you forget the `break`, execution continues in the case below**. Look at `fig02_14.java`, which writes the same selector in two ways:

```java
// fig02_14.java
void main() {
  String nombre = "inventario";
  int antiguo = 0;
  switch (nombre) {
    case "catalogo":
      antiguo = 1;
    case "inventario":
      antiguo = 2;
    case "pagos":
      antiguo = 3;
      break;
    default:
      antiguo = -1;
  }
  IO.println("switch con dos puntos y sin break: " + antiguo);

  int moderno = switch (nombre) {
    case "catalogo" -> 1;
    case "inventario" -> 2;
    case "pagos" -> 3;
    default -> -1;
  };
  IO.println("switch con flecha:                " + moderno);
}
```

```text
$ java fig02_14.java
switch con dos puntos y sin break: 3
switch con flecha:                2
```

With the old syntax (the colons), `inventario` enters case 2, **finds no `break`**, falls into case 3 and ends up leaving `antiguo` (“old”) at 3. With the new one (the arrow `->`), each case is independent: there is no `break` and no fall-through. You have to recognize the old syntax because it is in almost all the Java code you will find out there, but **in this course it is always written with arrows**. `switch`es with arrows entered Java in version 14 ([JEP 361](https://openjdk.org/jeps/361)).

The second difference is the one that changes how you think: a `switch` with arrows is an **expression**, that is, it **produces a value**. You can assign it (`int moderno = switch ... ;`), return it with `return switch ...` or pass it to a method. Three rules characterize it. A case can have **several values** separated by commas (`case "catalogo", "inventario", "pagos" -> 200;`). The result of each case is what is on the right of the arrow. And if you need more than one statement, the case carries a block between braces and delivers its value with `yield`. See `fig02_15.java`:

```java
// fig02_15.java
void main() {
  IO.println(describir(200));
  IO.println(describir(301));
  IO.println(describir(404));
  IO.println(describir(503));
  IO.println(describir(0));
}

String describir(int codigo) {
  int familia = codigo / 100;
  return switch (familia) {
    case 2 -> "correcto";
    case 3 -> "redirección";
    case 4, 5 -> {
      String quien = familia == 4 ? "cliente" : "servidor";
      yield "error del " + quien;
    }
    default -> "sin clasificar";
  };
}
```

```text
$ java fig02_15.java
correcto
redirección
error del cliente
error del servidor
sin clasificar
```

There are three new things in that figure. `codigo / 100` takes advantage of the integer division from 2.1.1 to obtain the code's “family” (503 ends up in 5). `case 4, 5 -> { ... yield ...; }` is the block with `yield`, which is to a case what `return` is to a method. And the `default` catches the rest. One limitation: the `switch` with values compares by **equality** with constants; it is no use for ranges like “400 or more”. For that you use an `if`, or, from Lesson 3, a `switch` with patterns and conditions.

The rule that makes the `switch` as an expression valuable is that it **has to cover all possible cases**, because it has to produce a value whatever the input. If something is missing, the compiler says so. `fig02_16.java` forgets the `default`:

```java
// fig02_16.java
void main() {
  String nombre = "reportes";
  int codigo = switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
  };
  IO.println(codigo);
}
```

```text
$ java fig02_16.java
fig02_16.java:4: error: the switch expression does not cover all possible input values
  int codigo = switch (nombre) {
               ^
1 error
error: compilation failed
```

The compiler knows there are infinitely many different texts, so without a `default` it cannot guarantee that there is a value. Later, in Lesson 3, you will see that with `enum` and with sealed types the compiler can check that **no case is missing without needing `default`**, and that is one of the best reasons to prefer them.

A `switch` on a text compares with `equals` (inside), not with `==`, so it does not fall into the trap of 2.2.2. And if the text is `null`, the `switch` throws a `NullPointerException` before looking at any case.

#### 2.3.4 Methods and constants

Java methods are declared by writing, in order, the **type they return**, the **name**, the **parameters with their type** between parentheses and the body between braces. `String marcar(long tiempoMs)` is a method that receives a `long` and returns a `String`. If it returns nothing, the type is `void` (as in `void main()`). It returns with `return`, and the compiler demands that **all paths** of the method return something: if an `if` without `else` leaves a path without `return`, it is a compile error. Inside a compact source file, methods are written at the same level as `main`, and `main` calls them by name.

Two particularities compared with other languages. Java **has no parameters with default values**: if you want a short version of a method, you write another with the same name and fewer parameters (Lesson 3 will do so with *records*). And arguments are passed **by value**: the method receives a copy. Look at `fig02_17.java`, which also introduces a constant:

```java
// fig02_17.java
static final int UMBRAL_LENTO_MS = 1000;

void main() {
  long tiempoMs = 2300;
  IO.println(marcar(tiempoMs));
  IO.println("después de la llamada, tiempoMs sigue valiendo " + tiempoMs);
  IO.println(marcar(87));
}

String marcar(long tiempoMs) {
  String marca = tiempoMs >= UMBRAL_LENTO_MS ? "lento" : "rápido";
  tiempoMs = 0;
  return marca;
}
```

```text
$ java fig02_17.java
lento
después de la llamada, tiempoMs sigue valiendo 2300
rápido
```

The `marcar` method sets its parameter `tiempoMs` to 0, and when it returns, the variable in `main` is still worth 2300. Changing the parameter changed the copy, not the original. (With objects the same happens, but what is copied is the **address**: the method can change the content of a mutable object, but it cannot make the caller's variable point to another.)

The line at the beginning, `static final int UMBRAL_LENTO_MS = 1000;`, is a **constant**. Every word counts. `final` means “assigned only once”; an attempt to reassign it is a compile error. `static` means “belongs to the class and not to an object”; in a compact source file, the “class” is the whole file. The name goes in `UPPERCASE_WITH_UNDERSCORES` (as Java's convention asks), and carries the unit: `_MS`. Writing it in a single place and using it by name, instead of writing `1000` in each comparison, has a practical reason: when the threshold changes, it changes in one place; and a communication one: `tiempoMs >= UMBRAL_LENTO_MS` reads well, and `tiempoMs >= 1000` forces you to remember what a thousand is.

### 2.4 `null` and the messages of `NullPointerException`

#### 2.4.1 What `null` is and when it blows up

We said in 2.1.2 that a reference may point to nothing, and that this is written `null`. The problem is not that it exists, but what happens when you **use** a `null` as if it were an object: calling a method on it, reading one of its fields, or, in the case of a wrapper, taking its value out. In any of those cases the program stops with an exception called `NullPointerException` (called “NPE”). Tony Hoare, who invented the concept in 1965, later called it [“my billion-dollar mistake”](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/). Java did not remove it, and most of this course is the story of how to live without depending on it.

Look at `fig02_18.java`. The `codigoDe` method returns `null` for an unknown service, and `main` tries to add 1 to it:

```java
// fig02_18.java
void main() {
  Integer codigo = codigoDe("reportes");
  IO.println("código: " + codigo);
  int sumado = codigo + 1;
  IO.println("sumado: " + sumado);
}

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}
```

```text
$ java fig02_18.java
código: null
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "<local1>" is null
	at fig02_18.main(fig02_18.java:5)
```

The first line comes out fine, because gluing a `null` to a text with `+` just writes “null”. The second is the one that blows up: `codigo + 1` needs the number inside the `Integer`, that is, to do the *unboxing* from 2.1.2, and there is no number. It is the trap of wrappers: **the sum looks like an addition of two integers, but one of them may not exist**. If `codigo` were `int`, nothing would fail, but there would be no way to say “no response” either.

#### 2.4.2 Reading the message

For several versions of Java now, a `NullPointerException` does not just say “it is null”: it says **what you were trying to do** and **what was null**. It is the feature of “helpful NPE messages”, described in [JEP 358](https://openjdk.org/jeps/358). The one from `fig02_18.java` says:

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "<local1>" is null
	at fig02_18.main(fig02_18.java:5)
```

It is read in two halves. **Before `because`: what the program was trying.** “Cannot invoke `Integer.intValue()`”, that is, take the value out of the wrapper. **After `because`: what was null.** “The variable `<local1>`”. And the line below, `at fig02_18.main(fig02_18.java:5)`, says where: the `main` method, file `fig02_18.java`, line 5. An NPE is fixed by reading that message, not by opening a debugger.

Now, the detail the syllabus promises to clear up. Why does it say `<local1>` and not `codigo`? Because **the `java Archivo.java` launcher compiles the file in memory without saving the names of local variables**. Those names only exist in the source code; for the JVM to know them, the compiler has to include them, and you ask it to with the `-g` option. Compile it yourself, with `javac -g`, and run it with `java`:

```text
$ javac -g fig02_18.java
$ java fig02_18
código: null
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "codigo" is null
	at fig02_18.main(fig02_18.java:5)
```

Now it does: `because "codigo" is null`. It is the same program, the same error and the same line, with a much clearer message. When you run into a `<local3>` at work, you already know what it is: the number of the slot the JVM gave that local variable (in an instance method slot 0 is `this`, and the rest are handed out in the order they are declared; a `long` or a `double` takes two). And when you want the name, compile with `-g`.

There are more message forms. If what is null is not a variable but the **result of a method**, the message says so. It is `fig02_19.java`:

```java
// fig02_19.java
void main() {
  String texto = codigoDe("reportes").toString();
  IO.println(texto);
}

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}
```

```text
$ java fig02_19.java
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.toString()" because the return value of "fig02_19.codigoDe(String)" is null
	at fig02_19.main(fig02_19.java:3)
```

“Because the return value of `fig02_19.codigoDe(String)` is null” translates as: the method `codigoDe` returned null, and the program asked `toString()` of that value. There is no need to guess which part of the expression `codigoDe("reportes").toString()` was null: the message points to the one that was. When a chain has several chained calls (`a.b().c().d()`), that is what saves you half an hour.

#### 2.4.3 Three rules for living with `null`

`null` does not disappear from real code, but there are ways to tame it, and the `revisor` applies them from today:

1. **Check before using, with `&&`.** `codigo != null && tiempoMs != null` is the `revisor`'s condition for knowing whether a service responded. If the first part fails, the second is not evaluated.
2. **Put the literal on the left in `equals`.** `"pagos".equals(nombre)` cannot blow up; `nombre.equals("pagos")` can.
3. **Do not return `null` when there is a better way to say “there is no datum”.** Here we do it because we do not have another yet, but in Lesson 3 `null` disappears from the `revisor` and a sealed type replaces it, and in 4 you will see `Optional`. A method that can return null is a method that forces everyone who calls it to remember to check.

### 2.5 The project step: the `revisor` with its base functions

Now you put everything together. You want `Revisor.java` to print the table the lesson started from, computing each cell with functions. The rules are the usual four of the `revisor`:

1. If the service did not respond, it is **CAIDO** (down).
2. If it responded with a code of 400 or more, it is **CAIDO** (the detail is the code).
3. If it responded and took 1000 ms or more, it is **LENTO** (slow).
4. Otherwise, it is **ARRIBA** (up).

The data of the four services lives, for now, in two parallel `switch`es: one returns the code and the other the time. A service that did not respond has `null` in both. Your `Revisor.java` looks like this (save it in `~/curso-java/revisor/`; the figure calls it `fig02_20.java` to tell it apart from the others, and its content is that of your file):

```java
// fig02_20.java
static final int UMBRAL_LENTO_MS = 1000;

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}

Long tiempoDe(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142L;
    case "inventario" -> 2300L;
    case "pagos" -> 87L;
    default -> null;
  };
}

String clasificar(Integer codigo, Long tiempoMs) {
  boolean respondio = codigo != null && tiempoMs != null;
  if (!respondio) {
    return "CAIDO";
  }
  if (codigo >= 400) {
    return "CAIDO";
  }
  if (tiempoMs >= UMBRAL_LENTO_MS) {
    return "LENTO";
  }
  return "ARRIBA";
}

String formatearTiempo(Long tiempoMs) {
  if (tiempoMs == null) {
    return "—";
  }
  if (tiempoMs < UMBRAL_LENTO_MS) {
    return tiempoMs + "ms";
  }
  return String.format(Locale.ROOT, "%.1fs", tiempoMs / 1000.0);
}

String detalle(Integer codigo) {
  if (codigo == null) {
    return "sin conexión";
  }
  return String.valueOf(codigo);
}

String linea(String nombre, String estado, String tiempo, String detalle) {
  return String.format(Locale.ROOT, "%-12s%-10s%6s  %s", nombre, estado, tiempo, detalle);
}

String encabezado() {
  return linea("SERVICIO", "ESTADO", "TIEMPO", "DETALLE");
}

String revisar(String nombre) {
  Integer codigo = codigoDe(nombre);
  Long tiempoMs = tiempoDe(nombre);
  return linea(
      nombre, clasificar(codigo, tiempoMs), formatearTiempo(tiempoMs), detalle(codigo));
}

void main() {
  IO.println(encabezado());
  IO.println(revisar("catalogo"));
  IO.println(revisar("inventario"));
  IO.println(revisar("pagos"));
  IO.println(revisar("reportes"));
}
```

```text
$ java fig02_20.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Walk through the program with what you know, from top to bottom:

- **`UMBRAL_LENTO_MS`** (“slow threshold in ms”) is the constant from 2.3.4.
- **`codigoDe` and `tiempoDe`** are the two arrow `switch`es from 2.3.3. They return `Integer` and `Long`, not `int` and `long`, so that they can return `null` (2.1.2). The literals carry the `L` in `tiempoDe` because the result is a `Long`, and a bare `142` would only be an `Integer`.
- **`clasificar`** implements the four rules. The first line, `codigo != null && tiempoMs != null`, uses the `&&` from 2.3.2: otherwise, the comparisons below would unbox a null. The rules are applied in order, each with its `return`; when one is met, the others are not evaluated.
- **`formatearTiempo`** receives a `Long`, which may be null (the dash “—” is the signal for “no datum”), and otherwise decides between `142ms` and `2.3s`. Notice that it uses `Locale.ROOT` (2.2.3).
- **`detalle`** returns “sin conexión” (“no connection”) for the null and, otherwise, the code as text. `String.valueOf(codigo)` converts an `Integer` into a `String`.
- **`linea`** assembles a row with the template `%-12s%-10s%6s  %s`: 12 wide for the name, 10 for the status, 6 right-aligned for the time, two spaces and the detail. `encabezado` reuses `linea` with the titles, so that they have the same measures.
- **`revisar`** ties everything together: it looks up a service's code and time, and delivers its complete row. The call to `linea` is split into two lines because it would exceed 100 columns; the continuation goes with four more spaces.
- **`main`** prints the header and the four rows, one by one. There is no loop because there is no list: the services are not yet data that can be traversed.

And the table that comes out is, **character by character**, the one we promised at the start. Observe that `reportes` went through the whole path of `null`: `codigoDe` and `tiempoDe` return null for it, `clasificar` sends it to `CAIDO` by rule 1, `formatearTiempo` puts the dash on it and `detalle` writes “sin conexión”. Each piece decided on its own what to do with the emptiness. But that decision was repeated **four times** (once per function), and that is what the next lesson is about.

To run it from your working folder:

```text
$ cd ~/curso-java/revisor
$ java Revisor.java
```

## The error you will see

The lesson has one main error and four supporting messages worth knowing how to read at a glance.

**The main error: an `==` that gives `false` when you expected `true`.** It is the one from `fig02_08.java`. When a text built at run time is compared with `==` against a literal, Java asks whether they are the same object, and they are not:

```text
armado == literal:       false
armado.equals(literal):  true
```

There is no error message; that is why it is the one that costs the most. The signal is that **a comparison that should be true gives false** and everything else seems fine. If you see an `==` between two texts or two wrappers in an `if`, suspect it before anything else.

**The four supporting messages:**

- `error: incompatible types: possible lossy conversion from long to int`: you assigned a large type to a smaller one. Change the destination type, or make the conversion explicit knowing what you may lose (`fig02_04.java`).
- `error: cannot infer type for local variable respuesta`, with `(variable initializer is 'null')`: you used `var` without a value to deduce the type from (`fig02_06.java`).
- `error: the switch expression does not cover all possible input values`: a `switch` that produces a value and does not cover all cases. Add the `default` (or, with `enum` and sealed types, the missing case) (`fig02_16.java`).
- `NullPointerException: Cannot invoke "String.isBlank()" because "<local1>" is null`: you used a null as if it were an object. The part after `because` says which one was null; if it says `<localN>` instead of the name, the file was compiled without `-g` (`fig02_12.java`, `fig02_18.java`).

And a background effect that produces no message at all: `2,3s` instead of `2.3s`. It happens on a machine with Spain's locale when a decimal is formatted without `Locale.ROOT` (`fig02_09.java`).

## What gets done wrong

**Comparing texts or wrappers with `==`.** It works with literals and with small numbers, and fails with data that arrives from outside. With texts, `equals` (with the literal on the left); with wrappers, `equals` or, if they are primitives, `==`.

**Calling a `String` method and not saving the result.** A bare `linea.strip();` does nothing. Texts do not change: the methods return another.

**Using `&` or `|` where `&&` or `||` go.** Both are valid, and the first one blows up exactly when the check was useful.

**An `int` for something that can grow.** The milliseconds in a month do not fit in an `int`. If the datum is a time, a count or a size, start with `long`.

**A `double` for money or for comparing with `==`.** Binary decimals are not exact (`0.1 + 0.2`). The `revisor`'s times are whole milliseconds precisely to avoid it.

**An `(int)` cast to silence the compiler.** It silences the warning and leaves the overflow unannounced. Before writing it, ask yourself whether the destination type should be larger.

**`String.format` with decimals without a `Locale`.** It works on your machine and fails on someone else's. `Locale.ROOT` always.

**A `switch` with colon syntax.** A forgotten `break` produces errors that do not show up in the compiler. With the arrow the whole class of errors disappears.

**Returning `null` out of habit.** Every `null` that leaves a method forces everyone who calls it to remember to check. Here we use it because we do not have another tool yet, and Lesson 3 brings it.

**`var` where the type is not visible.** `var r = revisar(s);` forces the reader to go look for what `r` is. Use it when the type is obvious.

## Exercises

### Exercise 1 — Predict, then run

Write in a file the `main` that prints these six expressions, **one per line**, and before running it note on paper what you think each one will print:

1. `"142" + 1`
2. `1 + 2 + "3"`
3. `"1" + 2 + 3`
4. `-7 / 2`
5. `0.1 + 0.2 == 0.3`
6. `'a' + 1`

Run it and compare. In each case where you were wrong, write a sentence in your [logbook](bitacora.md) with the rule you did not have in mind.

### Exercise 2 — The ignored return value

Write a method `String normalizar(String nombre)` that removes the spaces at the ends and turns the text to lowercase, and test it with `"  PAGOS "`. Write it **first the wrong way** (calling `strip()` and `toLowerCase()` without saving the result) and see what it returns. Then fix it. Can it be fixed in a single line?

### Exercise 3 — Two ways of not blowing up

Write two methods: `boolean vacio(String detalle)`, which returns `true` if the detail is null **or** blank, and `boolean contieneError(String detalle)`, which returns `true` if the detail is not null **and** contains the word `error`. Test them with `null`, with `"   "`, with `"200"` and with `"error de red"`. Think before writing: which of the two uses `&&` and which `||`? What would happen with the wrong operator?

### Exercise 4 — The project: one datum in a single place

In the `Revisor.java` of section 2.5, add `facturacion` (“billing”), which responded with code **503** in **95 ms**. Do it **in two steps**. First, add it only in `codigoDe` (and to `main`, so that its row is printed), and run. Predict beforehand which row will come out. Then add it in `tiempoDe` as well and run again. Write in the logbook what problem there is in maintaining the same information in two places.

## Solutions

### Solution 1

```text
1421
33
123
-3
false
98
```

1. `"142" + 1` is a text glued to a number: the `+` converts the 1 to text and joins them, `"1421"`. It is not a sum.
2. `1 + 2 + "3"` is evaluated from left to right: first `1 + 2` (two numbers) gives 3, and then `3 + "3"` glues, `"33"`.
3. `"1" + 2 + 3` starts with a text, so everything that follows is glued: `"123"`.
4. `-7 / 2` is an integer division, which discards the decimal part toward zero: `-3`, not `-4`.
5. `0.1 + 0.2 == 0.3` is `false`, because the sum is `0.30000000000000004`, as we saw in 2.1.1.
6. `'a' + 1` adds a `char` and an `int`; the `char` is converted to its number (97), and gives `98`. To obtain the text “a1” you would have to start from a text: `"" + 'a' + 1`.

### Solution 2

The wrong version, which changes nothing:

```text
[  PAGOS ]
false
```

The correct version is written on a single line, **chaining** the calls, because each method returns a `String` on which the next one can be called:

```java
String normalizar(String nombre) {
  return nombre.strip().toLowerCase();
}
```

With it, the output is:

```text
[pagos]
true
```

### Solution 3

`vacio` uses `||`: it is enough that **one** of the two things holds. `contieneError` uses `&&`: **both** must hold, and the first protects the second.

```java
boolean vacio(String detalle) {
  return detalle == null || detalle.isBlank();
}

boolean contieneError(String detalle) {
  return detalle != null && detalle.contains("error");
}
```

The tests give `true`, `true`, `false` for `vacio` with `null`, `"   "` and `"200"`, and `false`, `true` for `contieneError` with `null` and with `"error de red"`. If you write `|` instead of `||` in `vacio`, with `null` the right side (`detalle.isBlank()`) is evaluated and blows up; with `&` instead of `&&` in `contieneError`, the same. And a third way out, without a condition: `"sin conexión".equals(detalle)` gives `false` with a null `detalle`, because the literal on the left is never null.

### Solution 4

With `facturacion` only in `codigoDe`, the row that appears is this:

```text
facturacion CAIDO          —  503
```

It comes out CAIDO for the wrong reason, which is the danger of this design. `codigoDe` says 503, but `tiempoDe` says `null`, so `clasificar` applies rule 1 (“did not respond”) and not rule 2 (“responded with an error code”), and the time comes out as a dash even though the service did answer in 95 ms. The detail shows the 503, which contradicts the dash. There is no compiler or JVM error at all. With the datum also in `tiempoDe`, the correct row is:

```text
facturacion CAIDO       95ms  503
```

The lesson that goes into the logbook: **a datum that lives in two places ends up saying two things**. It is the reason the next lesson gathers code and time into a single value, and the reason that the compiler, and not your memory, is what forces you to complete it.

## How I know I got it

- [ ] `java fig02_08.java` prints `false` and `true` on the first two lines, and you can explain why without looking at the lesson.
- [ ] `java fig02_07.java` shows `[  catalogo  ]` on the first line, and you know what you would change to make it show `[catalogo]`.
- [ ] If you remove the line `default -> null;` from `codigoDe`, the launcher answers with `the switch expression does not cover all possible input values`.
- [ ] In `fig02_12.java` you see the `NullPointerException` with `"<local1>"`, and with `javac -g` you see the name `"detalle"`.
- [ ] From `~/curso-java/revisor/`, `java Revisor.java` prints the header and the four rows of the table, with `reportes` in `CAIDO`, `—` and `sin conexión`.
- [ ] You can explain why `2300 == 2300` gives `false` with two `Integer`s and `true` with two `int`s.
- [ ] You predicted exercises 1 and 3 before running, and wrote in your logbook which of the six expressions in exercise 1 you missed.
- [ ] You wrote in your logbook which of this lesson's messages was hardest for you to read.

## Summary

Answer without looking at the lesson:

1. What is the difference between `int` and `Integer`, and when do you need the second?
2. What happens when an `int` exceeds its maximum value? How do you avoid it in a millisecond calculation?
3. What type does a variable declared with `var` have, and when is it decided?
4. Why does `linea.strip();` not change `linea`? What do you write instead?
5. What does `==` compare between two references? Why does `armado == literal` give `false` and `constante == literal` give `true`?
6. Why is `Locale.ROOT` passed to `String.format` when there are decimals?
7. What is the difference between `&&` and `&` when the left side is false?
8. Which two things distinguish a `switch` with arrows from the classic one? What happens if it does not cover all cases?
9. What do `static` and `final` mean in a constant?
10. How is a `NullPointerException` read: what does it say before `because` and what after? Why does `<local1>` sometimes appear?

## Further reading

- [The Java Tutorials, “Primitive Data Types”](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html): the eight primitive types with their ranges and default values, in the official documentation. Consulted on October 7, 2026.
- [`String` Javadoc, Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html): all the methods of the class, with the rule that strings are constant. Consulted on October 7, 2026.
- [JEP 361, “Switch Expressions”](https://openjdk.org/jeps/361) and [JEP 378, “Text Blocks”](https://openjdk.org/jeps/378): the why and the exact rules of two of the features used in this lesson. Consulted on October 7, 2026.
- [JEP 358, “Helpful NullPointerExceptions”](https://openjdk.org/jeps/358): what each part of the message says and why the names are missing when compiling without `-g`. Consulted on October 7, 2026.

---

**Previous:** [Lesson 1 — Installing Java 25 on your Linux Mint](01-instalacion.md) ·
**Next:** [Lesson 3 — Records, enums and sealed types](03-records-enums-sellados.md)
