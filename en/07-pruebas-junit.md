# Lesson 7 — Tests with JUnit

**Time:** 90 min (tight: you see each test first in red and then in green, and that takes time; do not skip it)

**What you build:** the `revisor` gains 18 automated tests that run with a single command and guard its rules: the classification, the time format, the reading of `servicios.txt`, and the summary

**What you learn:** what an automated test is and its three moments; JUnit 6 in the `pom.xml`, with its BOM and in `test` scope; `@Test` and `assertEquals`; `@ParameterizedTest` with `@CsvSource` (the table of cases) and why the cases are chosen at the boundary; `assertThrows` and `@TempDir`; seeing a test in red before seeing it in green; and that **the number of tests is read from the report, not from the exit code**

**Where you are coming from.** You bring the Maven project from [Lesson 6](06-proyecto-maven.md): thirteen files in the `mx.habil.revisor` package, a `pom.xml` with three fixed-version *plugins*, and `mvn -B verify`, which compiles, packages `revisor-0.6.0.jar`, and ends in `BUILD SUCCESS`. In the middle of that output there was a line that said `No tests to run.`: your project already has a place to put tests (the `src/test/java` folder, empty) and something to run them (the *Surefire* *plugin*), but not a single test. And that leaves an uncomfortable question: how do you know the `revisor` works? You ran it and looked at the table. **The only test your program has is you, looking at a screen.** Today you change that: tests that check for you, in a fraction of a second and on every change, that each rule still does what it says. You work in your `~/curso-java/revisor/` folder.

**What this lesson does not do.** It does not test `Revision`, the class that simulates the network by waiting with `Thread.sleep`: it would take as long as the program itself, and what it guards (the `Simulador`) disappears in [Lesson 8](08-http-y-json.md), which tests `revisar` against a real server started inside the test. Nor does it use libraries to fake objects or measure the coverage of the tests: with small functions and fixed data they are not needed. And it adds nothing to the program: JUnit lives only in the tests, and the `.jar` that gets delivered does not change its contents.

**About the runs.** Everything in this lesson was run with Temurin 25 and Maven 3.10.0, on macOS, with the terminal in UTF-8. Maven's outputs are shown abridged: what each section analyzes is kept, and the three dots (`...`) mark what was removed. Two numbers in those outputs will be different on your screen and it does not matter: the `Time elapsed` values (they change on every run) and, in the tests that fail, the line number of the trace (`ResumenTest.java:32`), which is that of your file and matches only if you wrote it exactly like the lesson, line by line. Everything else, including the test counts, must match. The programs are in [`programas/07-pruebas-junit/`](../programas/07-pruebas-junit), in the course repository.

## By the end you will be able to

- Explain what an automated test is, what its three moments are, and what a testing *framework* such as JUnit adds.
- Declare JUnit 6 in the `pom.xml` with its BOM and in `test` scope, and explain why it is not part of the program.
- Write tests with `@Test` and `assertEquals`, with the expected value first, and a table of cases with `@ParameterizedTest` and `@CsvSource`, choosing the cases around the boundary.
- Test an error with `assertThrows` and a file with `@TempDir`.
- See a test in red before seeing it in green, and know what to revert to cause it.
- Run all the tests, one class, or a single test, and read from the report how many were run.
- Explain three ways of getting a green that tested nothing, and how you protect yourself from each.

## The why before the how

It is Monday and someone asks you to move the “slow” threshold from one second to 800 milliseconds. It is a one-line change: `UMBRAL_LENTO_MS`. You make it, run the `revisor`, look at the table… and everything seems fine. Is it? With four services of fixed data, `inventario` was still slow and the others were still up, so the table looks the same as yesterday. But what happened with a service that responds in exactly 800 milliseconds? Does it count as slow or not? Today's table does not know: no service in your data takes 800. If you got it wrong by a `>` where a `>=` belonged, the error waits for a real service to land right on the boundary, and that day whoever uses your program will discover it, not you.

Looking at the table has three defects, and all three get worse with time. **It only sees the data you have**, and errors live in the data you do not have (the exact 800, the file with a badly written line, the case where nobody responds). **It depends on someone looking**, attentively, every time, and nobody looks attentively the twentieth time. And **it leaves no record**: if tomorrow someone else changes the time format, there is nothing to warn them that they broke what you checked yesterday. An automated test fixes all three at once: you write the case once, it checks it on every change without getting tired, and it stays in the project for the next person.

The lesson has six parts: **what a test is and how JUnit enters the project** (7.1); **the first test**, and seeing it in red (7.2); **the table of cases** (7.3); **testing a file and its error** (7.4); **reading the report**, which is the part that gets neglected the most (7.5); and **the project step** (7.6). With that destination in mind, this is what you will see at the end of the lesson, where Lesson 6 said `No tests to run.`:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
```

The line that matters is `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. By the end you will know where each of those numbers comes from, what happens when one of them moves, and why none of them is read from the “success” that Maven announces at the end.

## The concepts

### 7.1 What an automated test is and what it takes

#### 7.1.1 The three moments of a test

Go back to the Monday at the start: the threshold moved to 800 milliseconds and nobody knows whether a service at exactly 800 counts as slow. An **automated test** is the way to answer that question without waiting for it to happen in production: a small program that prepares a known piece of data, calls the part of the program you want to guard, and compares what it got with what it should have got. It always has three moments, abbreviated AAA in English: **arrange** the input data, **act** by calling the code, and **assert** that the result is the expected one. If the assertion holds, the test passes; if not, it fails and says what it expected and what it got.

Writing that small program is not hard. What takes work is everything else: finding all the tests in the project, running them one by one without the failure of one stopping the others, and reporting how many passed. That is done by a **testing *framework***, and Java's is **JUnit**, which is at version 6 (6.1.3 is the current one as of October 2026; it requires Java 17 or later, according to its [user guide](https://docs.junit.org/current/user-guide/)). It puts four things at your disposal: annotations such as `@Test` to mark which methods are tests, methods such as `assertEquals` to assert, an engine that discovers and runs the tests, and a report of what happened. An **annotation** is a label written with `@` before a declaration and read by tools; it does not change what the method does, it only tells JUnit how to treat it.

#### 7.1.2 JUnit in the `pom.xml`

To have it in the project you must declare the dependency in the `pom.xml`. This is the complete file, now with JUnit. The new part is in `<dependencyManagement>` and `<dependencies>`, and the project's version goes up to `0.7.0`, as Lesson 6 promised: the `.jar` will be called `revisor-0.7.0.jar`.

```xml fig07_01/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.7.0</version>

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
    </plugins>
  </build>
</project>
```

JUnit is not a single artifact but a family of them (the API for writing tests, the engine that runs them, the support for tables of cases…) and all of them must be the same version to work together. So that you do not write that version six times, and do not get one wrong, Maven offers the **BOM** (*bill of materials*): a `pom.xml` that contains no code but only a list of compatible versions. The `<dependencyManagement>` block with `<type>pom</type>` and `<scope>import</scope>` imports that complete list: from then on, any JUnit artifact is declared **without a version** and takes the one the BOM fixes. The dependency itself is just one, `junit-jupiter`, an “umbrella” artifact that brings the API, the support for tables of cases, and the engine. It is the way the [JUnit guide for Maven](https://docs.junit.org/current/running-tests/build-support.html) recommends for aligning the versions.

The line that matters most is `<scope>test</scope>`. A dependency's **scope** says where it is available: one with `test` scope exists only when the tests are compiled and run, and it is **not** part of the program. Whoever runs the `revisor` does not need JUnit, and is not forced to download it. Since the compiler now has JUnit in `src/test/java` and not in `src/main/java`, the `import org.junit...` that failed with the launcher in Lesson 6 (the figure `fig06_01`) works in the tests and would still fail in the program, which is exactly what is wanted.

#### 7.1.3 What is tested and what is not

Before writing the first one, a decision that holds for the whole course: **which parts of the `revisor` deserve a test today**. The answer comes from looking at what each class does. `Clasificador`, `Formato`, and `Resumen` are **rules**: they receive one piece of data and return another, always the same for the same input, without touching the network, the disk, or the clock. They are the easiest to test and the most valuable, because that is where the decisions live that someone will want to change on a Monday (the threshold, the time format, how an average is counted). `Configuracion` is a rule with an edge toward the outside: it reads a file, and what has to be guarded are its error messages, which are the only thing seen by whoever makes a mistake writing `servicios.txt`. All four have a test in this lesson.

`Revision` and `Simulador` do not, and the reason is not laziness. `Simulador` is scaffolding: it invents four responses so the program has something to check while there is no network, and in Lesson 8 it disappears. A test of scaffolding checks that the scaffolding says what you wrote into it, and it gets thrown away along with it. `Revision.revisar`, for its part, truly **waits** for what each service says (`Thread.sleep`): a test of the four services would take as long as the program, about 2.5 seconds, and the 18 tests of this lesson run in full in a fraction of a second. That speed is not a luxury: it is what makes you run them on every change, instead of “at the end”. When `revisar` makes a real request, in Lesson 8, it will have its own test against a server started inside the test itself, which answers what the test wants and when it wants.

There is a third reason to start with the rules, which shows in the shape of the tests that follow: the four test classes live in the **same package** as what they test, `mx.habil.revisor`. Thanks to that they see the package-visibility classes that Lesson 6 left closed to the world, and nothing had to be opened to be able to test it. The tests use the program as another class in the package would, and nothing more.

### 7.2 The first test: the summary

#### 7.2.1 `ResumenTest`, line by line

Let us start with the simplest one in the project. A review's summary counts services, and its average includes only those that responded. Two tests guard it: the four usual services (the average has to be 843), and the edge case where **nobody** answers, where the average does not exist and the summary writes a dash.

```java
// fig07_01/src/test/java/mx/habil/revisor/ResumenTest.java

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

It reads from top to bottom. The class lives in the same package as what it tests and is called `ResumenTest`: the name ends in `Test` because that is how *Surefire*, the *plugin* that runs the tests, looks for it (by default it includes the files that start with `Test` or end in `Test`, `Tests`, or `TestCase`, according to its [documentation](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html); in 7.5 you will see what happens with a test that is not named that way). The class is not `public`: JUnit 6 does not require it, and by the rule of Lesson 6 (6.1.3) it keeps the narrowest visibility.

The `import static` is new. A normal `import` lets you use a **class** by its short name; an `import static` lets you use a **static method** of a class without writing the class in front. With `import static org.junit.jupiter.api.Assertions.assertEquals;` you can write `assertEquals(...)` instead of `Assertions.assertEquals(...)`. The tests use it everywhere, so it becomes a habit.

`private static Resultado resultado(String nombre, Respuesta respuesta)` is a **helper**: a function that the two tests share to build a `Resultado` without repeating three lines each time. It is private because it only matters to this class. And each method with `@Test` is a test: it has a name that **says what is asserted** (`sinNingunaRespuestaElPromedioEsUnaRaya`, not `prueba2`), receives no parameters and returns nothing, and respects the three moments separated by blank lines: the list is built, `Formato.resumen(Resumen.de(resultados))` is called, and then the assertion is made. In `assertEquals(esperado, actual)` **the expected value goes first**; if you put them the other way around the test passes and fails just the same, but the error message will say the opposite of what happened.

From the project root, with `-Dtest=ResumenTest` to run only this class:

```text
$ mvn -B test -Dtest=ResumenTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`. Two tests, none failed. Now you know they pass. What remains is to know whether they **can fail**.

#### 7.2.2 Seeing it in red before seeing it in green

A test you have never seen fail proves nothing. It may be comparing the wrong thing, or comparing something that is always true, and its green would be decoration. The only way to know that a test guards what it says it guards is to **deliberately break the code it guards and check that it turns red**. Do it now with the summary ones. In `Resumen.java`, the average is calculated only with the services that responded: the `filter(OptionalLong::isPresent)` takes care of that. Replace those two lines (`.filter(OptionalLong::isPresent)` and `.mapToLong(OptionalLong::getAsLong)`) with a single one, `.mapToLong(t -> t.orElse(0))`: now the service that did not respond counts as **zero milliseconds**, and run it again:

```text
$ mvn -B test -Dtest=ResumenTest
[INFO] Running mx.habil.revisor.ResumenTest
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 0.027 s <<< FAILURE! -- in mx.habil.revisor.ResumenTest
[ERROR] mx.habil.revisor.ResumenTest.resumeLosCuatroServiciosCanonicos -- Time elapsed: 0.016 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms> but was: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 632ms>
	at mx.habil.revisor.ResumenTest.resumeLosCuatroServiciosCanonicos(ResumenTest.java:32)
[ERROR] mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya -- Time elapsed: 0.003 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio 0ms>
	at mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya(ResumenTest.java:41)
[ERROR]   ResumenTest.resumeLosCuatroServiciosCanonicos:32 expected: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms> but was: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 632ms>
[ERROR]   ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya:41 expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio 0ms>
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Now there are messages, and they are good ones. `Failures: 2`: both tests failed, each one because of a different consequence of the same error. The first says `expected: <… promedio 843ms> but was: <… promedio 632ms>` (the summary line is in Spanish: “promedio” means “average”): it expected 843, it got 632; and 632 is `2529 / 4`, the sum of the times divided by the four services instead of by the three that answered. The second says `expected: <… promedio —> but was: <… promedio 0ms>`: when nobody responds, the summary must say that there is no average, and the change turns it into “zero milliseconds”, a number that looks like data and is not. The line that starts with `at` is the trace: it says in which file and on which line of the test the assertion that failed was (`ResumenTest.java:32`), which is the first thing you open. With those two messages in hand you know where to look, without opening the debugger. (Two tests that fall because of the same cause also tell you something: that defect was noticeable from two angles, and the second test exists precisely for that.)

Undo the change and confirm that it goes back to green. This exercise of **reverting and seeing the red** is the cheapest habit in the whole course: it takes a minute and tells you whether your tests are worth anything. In the course it will be repeated every time a new test is written.

### 7.3 The table of cases: `@ParameterizedTest` and `@CsvSource`

#### 7.3.1 One test, many rows

A service's classification is a rule with a boundary: a thousand milliseconds or more is slow. To guard it properly you have to test several cases, and all of them have the same shape (a code and a time go in, a status comes out). Writing six almost identical methods is noise that hides what is interesting. JUnit has a better way, the **parameterized test**: you write the method once, with parameters, and a **table of cases** with the values:

```java
// fig07_01/src/test/java/mx/habil/revisor/ClasificadorTest.java

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

`@ParameterizedTest` replaces `@Test` and tells JUnit that this method will be run several times. `@CsvSource` is the table: each text in the list is a **row**, and the commas separate the **columns** (CSV comes from *comma-separated values*). JUnit takes each row, converts the text of each column to the type of the parameter that corresponds to it —`int`, `long`, and even `Estado`, which it recognizes by the name of the constant— and calls the method once per row. That is six calls, so the report will count **six tests** from this one declaration, plus the seventh: the `SinRespuesta` one, which goes as a separate `@Test` because it does not have the same shape (there is no code or time).

#### 7.3.2 The cases are chosen at the boundary

The test of the time format is the same idea with one input column and one expected-text column. Its four rows are the ones that matter around the change of unit: 87 and 999 are written in milliseconds, 1000 and 2300 in seconds.

```java
// fig07_01/src/test/java/mx/habil/revisor/FormatoTest.java

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

The cases were not chosen at random. They were chosen **around the boundary**: 142 is a normal service; 999 is the last value that is still up; 1000 is the first one that is slow (the case that matters most of all: it is the one that detects a `>` where a `>=` belonged); 2300 is clearly slow; 503 and 404 are server and client errors that, no matter how fast they answer, leave the service down. **Boundary values** are where off-by-one errors hide, and a table that does not include them leaves the edge unguarded. Check it the way you already know: in `Clasificador.java`, change `tiempoMs >= UMBRAL_LENTO_MS` to `tiempoMs > UMBRAL_LENTO_MS` and run the whole project:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.058 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[3] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <LENTO> but was: <ARRIBA>
	at mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(ClasificadorTest.java:26)

[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[ERROR] Failures: 
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:26 expected: <LENTO> but was: <ARRIBA>
[ERROR] Tests run: 18, Failures: 1, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[ERROR] BUILD FAILURE
```

Read it calmly, because it is the report of a real failure and it has four things worth recognizing at first glance. First, **which class failed**: `ClasificadorTest`, with `Tests run: 7, Failures: 1`; the other three classes stay green: the failure of one does not stop the others. Second, **which test**: `clasificaUnaRespuesta(int, long, Estado)[3]`, where `[3]` is the **number of the table row**: the third, `200, 1000, LENTO`. Third, **what happened**: `expected: <LENTO> but was: <ARRIBA>`. The 1000-millisecond service should have been slow and came out up, because with `>` the 1000 no longer counts. And fourth, the closing line: `Tests run: 18, Failures: 1` and `BUILD FAILURE`, with the non-zero exit code that makes any integration server turn red. A single one-character change, a single row in red, and the report tells you which: that is what the table gives you. Revert the change before continuing.

#### 7.3.3 Running a single test

When a row turns red, what you want is to repeat **that** test while you fix it, not all 18. You already used `-Dtest=ResumenTest` to run a class; with a `#` and the name of the method you choose a single test from the class:

```text
$ mvn -B test -Dtest=ClasificadorTest#sinRespuestaEsCaido
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 1`: of the seven tests in the class, only the one you asked for ran. Notice the phase too: `mvn -B test` and not `verify`. Since `test` comes before `package` in the lifecycle, Maven stops after the tests and does not build the `.jar`, which is not needed to repeat a test. It is the way to work while you fix something: one test, fast, many times. And it is also a trap if you forget that you used it: a green `Tests run: 1` says nothing about the other 17. **Before considering something finished, everything is run**, with `mvn -B verify` and without `-Dtest`, and the total is read.

### 7.4 `assertThrows` and `@TempDir`: testing a file and its error

The third family of tests guards what the `revisor` does with the configuration file. There are two new things here. The first is that testing a file requires having a file, and you do not want to write into your real folder or leave junk behind. The second is that what you want to assert is not a returned value but an **exception**: that certain files make the reading fail, and with the exact message.

```java
// fig07_01/src/test/java/mx/habil/revisor/ConfiguracionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de la lectura de servicios.txt. */
class ConfiguracionTest {

  @TempDir Path carpeta;

  @Test
  void leeUnArchivoValido() throws Exception {
    Path ruta = carpeta.resolve("servicios.txt");
    Files.writeString(
        ruta,
        """
        # nombre  url  [timeout_ms]
        catalogo   https://catalogo.example
        pagos      https://pagos.example  1000
        """);

    List<Servicio> servicios = Configuracion.leer(ruta);

    assertEquals(2, servicios.size());
    assertEquals("catalogo", servicios.get(0).nombre());
    assertEquals(Servicio.TIMEOUT_POR_OMISION_MS, servicios.get(0).timeoutMs());
    assertEquals(1000, servicios.get(1).timeoutMs());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          a https://a.x;b https://b.x;solo | renglón 3: se esperaban 2 o 3 campos y hay 1
          a https://a.x;b ftp://b.x | renglón 2: la url debe empezar con http:// o https://
          a https://a.x;b https://b.x 0 | renglón 2: timeout_ms debe ser un entero mayor que 0
          catalogo https://a.x;catalogo https://b.x | nombres repetidos: catalogo
          """)
  void rechazaUnArchivoInvalido(String contenido, String mensajeEsperado) throws IOException {
    Path ruta = carpeta.resolve("servicios.txt");
    Files.writeString(ruta, contenido.replace(";", "\n"));

    ErrorDeConfiguracion error =
        assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta));

    assertEquals(mensajeEsperado, error.getMessage());
  }
}
```

**`@TempDir Path carpeta`** asks JUnit for a temporary folder: before each test it creates it new and empty, and afterwards it deletes it with everything it contains. The tests write their `servicios.txt` there with `Files.writeString` (Lesson 5 read files with `Files`; this is the other half, writing them) and they do not get in each other's way or dirty the disk.

**`assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta))`** runs what you give it (an expression written as `() -> ...`: a *lambda*, the same form you used in the *streams* of Lesson 4) and checks that it throws **that** exception. If it throws nothing, or another one, the test fails; if it throws the expected one, `assertThrows` returns it to you so you can examine it: the test compares its `getMessage()` with the exact message from the table. Notice why the message is asserted and not only the type: that text is the only thing the person who edits `servicios.txt` sees when they make a mistake; it is the contract with them. If someone rewrites it without noticing, the test detects it.

The table of cases of this class uses a variant of `@CsvSource` that accepts a block of text (`textBlock`), with the delimiter `|` instead of the comma so that each row looks like a table. There is only one complication: each row takes up one line, but a configuration file has several. For that, the first column writes the lines of the file separated by `;` (`a https://a.x;b ftp://b.x` are two lines), and the test converts them into real line breaks before writing the file. The four rows are four of the reasons for rejection that Lesson 5 defined (the ones most often committed): fields are missing, the address is neither `http` nor `https`, the timeout is not greater than zero, and there are repeated names. The fifth test in the class, `leeUnArchivoValido`, is the happy case: two services, one with the default timeout and another with 1000. **You always need one:** a class that only tests rejections could pass with a reader that rejects everything.

### 7.5 Reading the report: the test count

#### 7.5.1 Where the number is, and where it is not

Run the whole project, complete, with the usual command:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
```

What matters in this report is a single line: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. The four numbers say how many tests were **run**, how many **failed** an assertion, how many ended with an unexpected **error** (an exception the test did not expect), and how many were **skipped** (they are marked not to run). And the 18 come out, exactly, from what you already know: `ClasificadorTest` 7 (six rows and the `SinRespuesta` one), `FormatoTest` 4 (four rows), `ConfiguracionTest` 5 (four rows and the happy case), and `ResumenTest` 2. Add them up: 7 + 4 + 5 + 2. If one day the 18 changes without you having added or removed a test, something moved, and it is a question worth answering.

Each test class also leaves its own report in the `target/surefire-reports` folder, in two formats:

```text
$ ls target/surefire-reports
TEST-mx.habil.revisor.ClasificadorTest.xml
TEST-mx.habil.revisor.ConfiguracionTest.xml
TEST-mx.habil.revisor.FormatoTest.xml
TEST-mx.habil.revisor.ResumenTest.xml
mx.habil.revisor.ClasificadorTest.txt
mx.habil.revisor.ConfiguracionTest.txt
mx.habil.revisor.FormatoTest.txt
mx.habil.revisor.ResumenTest.txt
```

The `.txt` files are for you to read, and the `TEST-*.xml` files are for machines: the continuous integration server, the editor, or a script of yours read the XML to count. A text report for a class in green is three lines long, what you saw above but for a single class:

```text
$ cat target/surefire-reports/mx.habil.revisor.ConfiguracionTest.txt
-------------------------------------------------------------------------------
Test set: mx.habil.revisor.ConfiguracionTest
-------------------------------------------------------------------------------
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
```

#### 7.5.2 Three greens that tested nothing

And now, the reason this section exists. **The number is read from the report, never from the success message or the exit code.** There is a way for Maven to end in `BUILD SUCCESS` without having run a single test, and it is easier to cause than it seems: all it takes is for the names of the test classes not to be the ones *Surefire* looks for. To see it, the four classes of this project were renamed from `...Test` to `...Prueba` (for example `ClasificadorPrueba`), which in Spanish sounds the same (“prueba” means “test”), and the same command was run:

```text
$ mvn -B verify
...
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
$ echo $?
0
```

There is no `T E S T S` line, no `Running`, no `Tests run`. And yet: `BUILD SUCCESS`, and the exit code is `0`, the one that means “all good”. Maven found no tests, had nothing to fail, and declared itself successful. If the only criterion had been “did it end in green?”, this project without a single effective test would have passed any review. The `target/surefire-reports` folder is not created either, which is another clue: the report does not exist because there was nothing to report. Compare it with what you saw in Lesson 6, when the test folder was empty: there *Surefire* said `No tests to run.`, an honest notice. Here there are four test classes compiled in `target/test-classes`, and since none is named the way it looks for, it says nothing. **Silence is worse than the notice**, and that is why the number is actively looked for: not appearing is also a piece of data.

There are two other variants of the same mechanism, also measured. The first: a test class that **skips itself**. JUnit allows a test to say “if such a condition is not met, do not run me” (it is called an *assumption*, `Assumptions`, and it is truly useful for tests that need something that is not always there, such as a connection). But a slip in that condition turns off all the tests of the class without anything turning red. A false assumption was added at the start of `FormatoTest` to see it. This is the fragment, placed inside the class, before the test it already had (with its two `import` lines: `org.junit.jupiter.api.Assumptions` and `org.junit.jupiter.api.BeforeAll`):

```java
  @BeforeAll
  static void soloSiHayRed() {
    Assumptions.assumeTrue(false, "sin red");
  }
```

`@BeforeAll` marks a method that JUnit runs once, before all the tests of the class (that is why it is `static`: there is not yet any object of the class). `assumeTrue(condition, reason)` says “if the condition is false, do not go on”. Here the condition is plainly `false`, so that the effect is obvious; in a real project it would be something like “there is a connection to the database”, and the slip, a condition that is never met on the integration machine. The result:

```text
$ mvn -B verify
...
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.002 s -- in mx.habil.revisor.FormatoTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
$ echo $?
0
```

Maven printed a `WARNING` (yellow, not red) and went on: the entire class ended up in `Skipped: 1`, the total dropped from 18 to 15, `BUILD SUCCESS` and exit code `0`. Whoever only looks at the green never finds out that four tests stopped guarding the time format. The second variant belongs to the terminal and not to Maven: **the exit code of a pipeline is that of the last program in the pipeline.** If for convenience you trim the output with `| tail` and check `$?`, what you are checking is the code of `tail`, which always exits successfully:

```text
$ mvn -B verify | tail -2
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
0
```

Here Maven failed (this is the run with the broken `>` of 7.3.2, and its own exit code was 1), but the `0` you see is that of `tail`. If you truly need a pipeline, the terminal has an option so that the exit code is that of the first program that failed and not that of the last:

```text
$ set -o pipefail
$ mvn -B verify | tail -1
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
1
```

With `pipefail`, Maven's `1` survives the pipeline. It holds for the terminal where you typed it (or for the script that has it at the beginning), and it does not change what matters: the safest thing is to run the tests **without filtering the output**, and read the `Tests run:` line from there. In all three cases the defense is the same, and it is the habit of this course: after running the tests, read that line and compare the number with the one you expect.

There is a fourth green, but that one does give notice: `mvn -B verify -DskipTests` compiles the tests, does not run them, says `Tests are skipped.`, and ends in `BUILD SUCCESS`. It is a legitimate option, used on purpose when you only want the `.jar` and you already ran the tests before; it is worth remembering that this green does not mean anything was tested, and that it is never the command by which you decide whether a change is good.

#### 7.5.3 The number is written down

In a project that already exists, the way to know how many to expect is to have it written down: in the course logbook, next to the date. Today's says 18. When you add a test, it will be 19, and you will write it down. It is a cheap way for an unexpected change in the count —upward or downward— to catch your attention.

### 7.6 The project step: the `revisor` with tests

You have all the pieces. This is the complete project, as it stands at the end of the lesson. Compared with Lesson 6, two things change: the `pom.xml` (version 0.7.0 and JUnit in test scope, in 7.1.2) and the `src/test/java` folder, which is no longer empty. **The program's code did not change by a single line**: the tests were written for the program that already existed. Everything is included so that the project can be assembled without looking at another lesson:

```text
revisor/
  pom.xml                 versión 0.7.0 · release 25 · JUnit 6.1.3 en alcance de pruebas
  servicios.txt           sin cambios
  src/main/java/mx/habil/revisor/                       (los trece, sin cambios)
    Revisor.java  Estado.java  Respuesta.java  Servicio.java  Resultado.java
    Clasificador.java  Formato.java  Simulador.java  Revision.java  Resumen.java
    Reporte.java  Configuracion.java  ErrorDeConfiguracion.java
  src/test/java/mx/habil/revisor/
    ClasificadorTest.java  FormatoTest.java  ConfiguracionTest.java  ResumenTest.java   NUEVOS
```

You have already seen the `pom.xml` and the four tests in full. Your `servicios.txt` is the usual one:

```text fig07_01/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

And these are the thirteen files of the program, identical to those of Lesson 6. If you followed it, you already have them; they are included so that the project of this lesson can be built without depending on anything else.

<details>
<summary>The thirteen files of the program, unchanged from Lesson 6</summary>

```java
// fig07_01/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.List;

/** El punto de entrada: lee servicios.txt, revisa cada servicio e imprime el reporte. */
public final class Revisor {

  private Revisor() {}

  /** Arranca el revisor; los errores de configuración salen por la salida de error. */
  public static void main(String[] args) throws InterruptedException {
    try {
      List<Servicio> servicios = Configuracion.leer(Path.of("servicios.txt"));
      Reporte reporte = Reporte.de(Revision.revisarTodos(servicios));
      System.out.println(Formato.tabla(reporte));
    } catch (ErrorDeConfiguracion e) {
      System.err.println("error de configuración: " + e.getMessage());
    }
  }
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Servicio.java

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
// fig07_01/src/main/java/mx/habil/revisor/Resultado.java

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
// fig07_01/src/main/java/mx/habil/revisor/Clasificador.java

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
// fig07_01/src/main/java/mx/habil/revisor/Simulador.java

package mx.habil.revisor;

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
// fig07_01/src/main/java/mx/habil/revisor/Revision.java

package mx.habil.revisor;

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

```java
// fig07_01/src/main/java/mx/habil/revisor/Resumen.java

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
// fig07_01/src/main/java/mx/habil/revisor/Reporte.java

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
// fig07_01/src/main/java/mx/habil/revisor/Formato.java

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
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Configuracion.java

package mx.habil.revisor;

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
    URI url;
    try {
      url = new URI(texto);
    } catch (URISyntaxException _) {
      // Sin la causa: su mensaje repite la dirección entera, con usuario, clave y token.
      throw new ErrorDeConfiguracion("renglón " + numero + ": la url no es válida");
    }
    if (url.getHost() == null) {
      throw new ErrorDeConfiguracion("renglón " + numero + ": la url no es válida");
    }
    if (url.getRawUserInfo() != null) {
      throw new ErrorDeConfiguracion(
          "renglón " + numero + ": la url no puede llevar usuario ni clave");
    }
    return url;
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

```java
// fig07_01/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

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

</details>

To finish the lesson: `mvn -B verify` must say `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`, and leave `target/revisor-0.7.0.jar`; `java -cp target/classes mx.habil.revisor.Revisor` must print the table with the summary from Lesson 4 (4.5) of 843 ms, just as in Lesson 6, because the program did not change. There is an article from Hábil about [which tests catch real errors and how to measure coverage](https://www.habil.mx/en/blog/tests-that-catch-errors-coverage-quality-gate/), which complements this lesson from the quality side; and another about [how a software house's continuous integration is set up](https://www.habil.mx/en/blog/cicd-devsecops-own-infrastructure/), the place where these 18 tests run on their own on every change.

## The error you will see

The lesson has one main error, that of the test that turns red, and several supporting messages that are worth knowing how to read at a glance.

**The main error.** It is the one in 7.3.2. You change `>=` to `>` in the threshold rule, run `mvn -B verify`, and the report says that one of the rows of the table of cases, the third, expected `LENTO` and got `ARRIBA`. It is read in four steps, always the same: **which class** failed (the line with `<<< FAILURE!`), **which test** (the name of the method and, in brackets, the row number), **what it expected and what it got** (`expected: <...> but was: <...>`), and **how many** in total (`Tests run: 18, Failures: 1`). With that you know what to change without opening a debugger, and you have the certainty that the test guards what it says it guards. Seeing it fail, on purpose, is the first use you give it.

**The supporting messages**, to recognize them at a glance:

- `org.opentest4j.AssertionFailedError: expected: <...> but was: <...>`: an assertion was not met. The report counts it in `Failures`. If the values seem backwards, check the order of `assertEquals`: the expected one goes first.
- A line with `Errors: 1` instead of `Failures: 1`: the test never got to assert anything because an exception it did not expect was thrown. It is a different kind of problem: not “the result is different”, but “the code crashed”.
- `Expected mx.habil.revisor.ErrorDeConfiguracion to be thrown, but nothing was thrown.`: it is the way JUnit says that an `assertThrows` did not see the exception it asked for. The reading of the file accepted something it should have rejected. (Measured by changing `timeoutMs > 0` to `timeoutMs >= 0` in `Configuracion.java`: a `timeout_ms` of 0 stops being rejected and row `[3]` of `rechazaUnArchivoInvalido` fails.)
- `[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`: an entire class was skipped. Yellow, not red, and the total drops: look for an assumption (7.5.2).
- `Tests are skipped.`: someone ran with `-DskipTests`. That green tested nothing.
- No `Tests run:` line and `BUILD SUCCESS`: *Surefire* found no classes named the way it looks for (7.5.2).

## What gets done wrong

**Trusting the `BUILD SUCCESS`.** You already saw the cost in 7.5: a project without a single effective test ends in green and with exit code zero. What counts is the `Tests run:` line and that the number is the expected one.

**Trusting a test you never saw in red.** A test that always passes cannot be told apart from one that guards nothing. Before accepting it as good, break what it guards, check that it fails with the message you expect, and undo the change.

**A test without an assertion.** A method with `@Test` that calls the code but never calls `assertEquals` or `assertThrows` always passes, unless the code throws an exception: it runs the code, it checks nothing. The green it gives is worth no more than that of a project without tests.

**Testing only the happy case, or only the rejections.** A class that only tests that a good file is read correctly does not know whether the bad ones are rejected; one that only tests rejections would pass with a reader that rejects everything. Both sides are needed, as in `ConfiguracionTest`.

**Choosing the cases at random.** Errors hide at the boundary: the 999 and the 1000, the 399 and the 400. A table of cases that does not have both sides of each boundary leaves the edge unguarded, and exercise 1 measures how costly that is.

**Tests that depend on something you do not control:** the network, the time of day, the order in which they run, the folder they are run from, or a file that “was already there”. A test has to give the same result tomorrow and on someone else's computer; for files, `@TempDir`; for data, let the case itself build it.

**Commenting out or deleting a test that fails “so that it passes”.** The test was saying something. Understand the failure: either the code is wrong, or the test describes a rule that changed, and then the test is updated (as in exercise 2), not silenced.

**Trimming the output of the tests with a pipeline.** `mvn -B verify | tail` swaps the exit code for that of `tail`, and the failure disappears (7.5.2). Tests are run without filtering, or with `set -o pipefail`.

## Exercises

**Each exercise starts from the project in 7.6, with its 18 tests.** If an exercise leaves you with extra tests or a change in the code, undo it before starting the next one; that way the counts you predict and those in the solutions are the same.

### Exercise 1 — The missing boundary

The table of cases of `ClasificadorTest` has two HTTP errors, 503 and 404, but neither is near the boundary between “responded fine” and “responded with an error”. The rule says that a code **greater than or equal to 400** leaves the service down. **Before touching anything, predict:** if someone changes the comparison `codigo >= 400` to `codigo > 400` in `Clasificador.java`, how many of the 18 tests turn red? Run it and check your prediction. Then add the cases `400, 50, CAIDO` and `399, 50, ARRIBA` to the table, repeat the change, and write down which row turns red and how many tests the report now says.

### Exercise 2 — The Monday question

Answer the question the lesson started with. Change `UMBRAL_LENTO_MS` to 800 in `Clasificador.java`. **Predict** which row of the table turns red and why, and run `mvn -B verify` to check it. Then update the **test**, not the code: change the two boundary rows so that they guard the new value (a value just below and the threshold value), and confirm that the report again says `Tests run: 18, Failures: 0`. Write in the logbook why the test had to change.

### Exercise 3 — A new test, by hand

`Formato.contar(long cantidad, String palabra)` adds an “s” to the word except when the quantity is 1 (`1 caído`, `3 caídos`; “caído” means “down”). Write in `FormatoTest` a parameterized test with three rows: `0, servicio` (it must give `0 servicios`), `1, caído`, and `3, caído`. **Before running it**, write down how many tests you expect the report to say (hint: there are now three more rows). Then make it fail on purpose by changing `cantidad == 1` to `cantidad <= 1` in `Formato.java`, and observe how many tests turn red and from which classes. Did you expect them to be from more than one class?

### Exercise 4 — The green that tested nothing

Rename the class `ResumenTest` to `ResumenPrueba`, in the file and in the class declaration. Run `mvn -B verify` and answer three questions: did it end in `BUILD SUCCESS`? How many tests does the report say now? What is the exit code, with `echo $?` right afterwards? Restore the name, confirm that the count goes back to 18 (if a count ever does not add up after renaming or deleting a class, `mvn -B clean verify` deletes `target/` and builds from scratch), and write in the logbook the sentence by which you will remember to read the `Tests run:` line.

## Solutions

### Solution 1

**Prediction.** None turns red: all 18 pass. The error cases in the table are 503 and 404, and both are greater than 400 with either comparison. The change from `>=` to `>` only matters for the exact value 400, and the table does not contain it. It is a real case of a change that the tests **do not detect**: the defect survives, and the green says everything is fine.

The two new cases are added at the end of the table:

```java
  @CsvSource({
    "200, 142,  ARRIBA",
    "200, 999,  ARRIBA",
    "200, 1000, LENTO",
    "200, 2300, LENTO",
    "503, 95,   CAIDO",
    "404, 50,   CAIDO",
    "400, 50,   CAIDO",
    "399, 50,   ARRIBA"
  })
```

With the two cases and the change from `>=` to `>` in `Clasificador.java`, the report says:

```text
$ mvn -B verify
...
[ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.053 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[7] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <CAIDO> but was: <ARRIBA>
...
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:28 expected: <CAIDO> but was: <ARRIBA>
[ERROR] Tests run: 20, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Row `[7]` is the one for `400, 50, CAIDO`: it expected `CAIDO` and got `ARRIBA`, because with `>` the 400 is no longer an error. And the total went up to 20, from the original 18 plus the two new rows. The row `399, 50, ARRIBA` passes in both versions; it is there so that the other side of the boundary is also guarded. Revert the change in `Clasificador.java`: with the two cases still in the table, the report must say `Tests run: 20, Failures: 0`. In a real project those two rows stay, because they guard a boundary that nobody guarded before; to continue with the exercises, remove them and go back to 18.

### Solution 2

With the threshold at 800, the rule guards a value that the table did not expect. Row `[2]` fails, `200, 999, ARRIBA`: a service of 999 milliseconds is no longer up, it is slow.

```text
$ mvn -B verify
...
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.050 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[2] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <ARRIBA> but was: <LENTO>
...
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:26 expected: <ARRIBA> but was: <LENTO>
[ERROR] Tests run: 18, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

The `expected: <ARRIBA> but was: <LENTO>` says it. The other rows keep passing (the 142 is still up, and the 1000 and the 2300 are still slow), and no other test class breaks, because no other depends on the threshold. What fails is the test, and the reason is that the **business rule changed**: the test described the old threshold. That is why the test is updated and not the code. The two boundary rows become `200, 799, ARRIBA` (just below) and `200, 800, LENTO` (the threshold):

```java
    "200, 142,  ARRIBA",
    "200, 799,  ARRIBA",
    "200, 800,  LENTO",
    "200, 2300, LENTO",
```

and the report goes back to green, with the same total as at the start, because the content of two rows was changed and none was added:

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

And Monday's question now has a written, executable, guarded answer: a service of exactly 800 milliseconds is slow.

### Solution 3

The new test, inside the `FormatoTest` class, next to the one it already has:

```java
  @ParameterizedTest
  @CsvSource({"0, servicio, 0 servicios", "1, caído, 1 caído", "3, caído, 3 caídos"})
  void pluralizaSalvoCuandoEsUno(long cantidad, String palabra, String esperado) {
    assertEquals(esperado, Formato.contar(cantidad, palabra));
  }
```

There are three rows, so the count goes from 18 to **21**:

```text
$ mvn -B verify
...
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

With `cantidad <= 1` instead of `cantidad == 1`, zero stops taking an “s” (`0 servicio`) and that case fails:

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in mx.habil.revisor.ClasificadorTest
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.007 s <<< FAILURE! -- in mx.habil.revisor.ResumenTest
[ERROR] mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lento · 1 caído · promedio —>
...
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.009 s <<< FAILURE! -- in mx.habil.revisor.FormatoTest
[ERROR] mx.habil.revisor.FormatoTest.pluralizaSalvoCuandoEsUno(long, String, String)[1] -- Time elapsed: 0.001 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <0 servicios> but was: <0 servicio>
...
[ERROR]   FormatoTest.pluralizaSalvoCuandoEsUno:22 expected: <0 servicios> but was: <0 servicio>
[ERROR]   ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya:41 expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lento · 1 caído · promedio —>
[ERROR] Tests run: 21, Failures: 2, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

**Two** tests fail, from **two classes**: the new one (`FormatoTest`, row `[1]`, the `0 servicios` one) and one that already existed, `ResumenTest`, whose “nobody responded” case expects `0 lentos` (“0 slow”). It is normal that the surprise is that: tests are written for one thing, but they guard everything that passes through there. A change in a shared function is easier to detect if several parts of the program depend on it. And it is a good reason not to delete a “redundant” test.

### Solution 4

Without the `ResumenTest` class, *Surefire* no longer finds it, and the report says:

```text
$ mvn -B verify
...
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
$ echo $?
0
```

`BUILD SUCCESS`, exit code `0`, and **16** tests instead of 18: the two from `ResumenTest` are missing. If you had not known that the right number was 18, nothing would have warned you. That is the sentence for the logbook: **“Tests run: 18, always; if the number changes and I did not change it, I stop.”** When you restore the name, the count goes back to 18. A measured detail: with Maven 3.10.0, a plain `mvn -B verify` already gives 16, because the compiler detects that a source changed, recompiles the entire module, and does not leave the old `ResumenTest.class` in `target/test-classes`; `mvn -B clean verify` also gives 16 and does not depend on that behavior, so it is the safe way when you rename or delete classes.

## How I know I got it

- [ ] `mvn -B verify`, from the root of `~/curso-java/revisor/`, ends with `BUILD SUCCESS` and says `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`, and you know where each of the 18 comes from (7 + 4 + 5 + 2).
- [ ] The `.jar` is now called `revisor-0.7.0.jar`, and `java -cp target/classes mx.habil.revisor.Revisor` still prints the table with `promedio 843ms` (“average 843ms”).
- [ ] You changed `>=` to `>` in `Clasificador.java`, saw row `[3]` in red with `expected: <LENTO> but was: <ARRIBA>`, and undid the change.
- [ ] You ran a single test with `-Dtest=Clase#metodo` and then all of them, and you know why everything is run at the end.
- [ ] You renamed `ResumenTest` to `ResumenPrueba`, saw `BUILD SUCCESS` with 16 tests, and wrote in the logbook how you will remember to read the count.
- [ ] You can explain what `<scope>test</scope>` is for and why JUnit does not go in the program.
- [ ] You wrote in your logbook the number the report says, with the date.

## Summary

Answer without looking at the lesson:

1. What are the three moments of a test? Which argument of `assertEquals` goes first, and what happens if you swap them?
2. What is JUnit's BOM for and what does `<scope>test</scope>` do?
3. Why does this lesson test `Clasificador`, `Formato`, `Resumen`, and `Configuracion`, and not `Revision` or `Simulador`?
4. Why is the code deliberately broken before accepting a test as good?
5. What does the `[3]` mean in the name of a parameterized test that fails? Why are the cases in the table chosen around the boundary?
6. What does `assertThrows` check, and what happens if the code throws nothing? What does `@TempDir` do?
7. What is the difference between `Failures` and `Errors` in the report?
8. Three ways in which Maven ends in green without having tested what you think: what are they, and how do you protect yourself from each?

## Further reading

- [JUnit 6, user guide](https://docs.junit.org/current/user-guide/): the annotations, the [assertions](https://docs.junit.org/current/writing-tests/assertions.html), the [assumptions](https://docs.junit.org/current/writing-tests/assumptions.html), and the [parameterized tests](https://docs.junit.org/current/writing-tests/parameterized-classes-and-tests.html) with all their data sources, beyond `@CsvSource`. Consulted on October 13, 2026.
- [Maven Surefire, “Inclusions and Exclusions of Tests”](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html): which class names it runs and how to change it, useful when a count does not add up. Consulted on October 7, 2026.
- [Maven, “Introduction to the Dependency Mechanism”](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html): the scopes (`test` among them), the BOM, and how it is imported with `<scope>import</scope>`. Consulted on October 13, 2026.
- [Bash Reference Manual, the `set` builtin](https://www.gnu.org/software/bash/manual/html_node/The-Set-Builtin.html): what `pipefail` does to the exit code of a pipeline. Consulted on October 13, 2026.

---

**Previous:** [Lesson 6 — A Maven project](06-proyecto-maven.md) ·
**Next:** [Lesson 8 — HTTP and JSON](08-http-y-json.md)
