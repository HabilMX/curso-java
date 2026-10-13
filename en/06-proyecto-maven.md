# Lesson 6 — A Maven project

**Time:** 90 min (tight: it includes installing Maven, and the first build downloads pieces from the internet; do not rush it)

**What you build:** the `revisor` stops being a folder of loose files and becomes a Maven project, with packages, that is compiled, packaged into a `.jar` and always run with the same two commands

**What you learn:** when a loose file stops being enough; packages and visibility; the return of `public static void main(String[] args)`; installing Maven 3.10.0 with SDKMAN; the standard Maven layout and the `pom.xml`; the `compile`, `test`, `package`, `verify` cycle; what is inside a `.jar` and why it still does not run with `java -jar`

**Where you are coming from.** You bring the project from [Lesson 5](05-errores-excepciones.md): a folder with several `.java` files (the model, the classification, the formatting, the report, the reading of `servicios.txt` with its own exceptions), which runs with `java Revisor.java` and takes about 2.5 seconds because the simulation waits as long as each service says. It works, and you run it with a single command. But that command has limits that will show up as soon as the program needs something you did not write yourself: the next lesson needs a testing library, and the one after that, a library for reading JSON. The source-file launcher does not know how to use other people's libraries, does not save anything it compiles, and has no recognizable shape. Today you turn the `revisor` into a real project: with the structure that any Java programmer recognizes and with a tool that compiles it, packages it and, starting with the next lesson, tests it. You work in your `~/curso-java/revisor/` folder.

**What this lesson does not do.** It does not write tests yet: that is [Lesson 7](07-pruebas-junit.md), which builds on what you build today (the `pom.xml`, the tests folder that stays empty today, and Maven's lifecycle). It does not use any network: the `Simulador` keeps playing the role of the services, and that does not change until [Lesson 8](08-http-y-json.md). Nor does it add libraries to the program (the program's first dependency arrives in 8), or check the services at the same time (that is [Lesson 9](09-concurrencia.md)). What gets solved here is **how a Java program that already has more than ten files is organized, compiled and packaged**.

**About the runs.** Everything in this lesson was run with Temurin 25 and Maven 3.10.0, on macOS, with the terminal in UTF-8; the installation of Maven with SDKMAN and its `mvn --version` were also measured on Linux Mint 22.3, on October 13, 2026. Maven's output is long and changes on every run (downloads, times, paths), so it is shown abbreviated: what each section analyzes is kept, and the three dots (`...`) mark what was removed; long paths are shown starting from `~/curso-java`. The messages from the Java launcher (the ones that start with `Error:`) come out in the system's language: here they are shown in English, and if your Linux Mint is in Spanish you will see the first line translated; the rest does not change. The programs are in [`programas/06-proyecto-maven/`](../programas/06-proyecto-maven), in the course repository.

## By the end you will be able to

- Explain which three things the source-file launcher does not do and what a Maven project adds to your program.
- Declare a package, put the file in the folder that corresponds to it and say which classes each one sees according to its visibility.
- Install Maven 3.10.0 with SDKMAN and recognize in `mvn --version` that it uses your Temurin 25.
- Read a `pom.xml` and say what each element is for, including the reason for pinning the version of every *plugin*.
- Say which phases `mvn -B verify` runs, what each one leaves in `target/` and what is inside the `.jar`.
- Move the `revisor` to the Maven structure and run it from `target/classes` and from its `.jar`.

## The why before the how

Until the last lesson, your build tool was `java` itself: you hand it a `.java` file, it compiles it in memory together with the others in the folder and runs it. For learning, that is perfect. For a program that is going to grow it has three limits, and all three are fixed with the same tool:

1. **It cannot use other people's libraries.** The launcher from [Lesson 1](01-instalacion.md) (`java Revisor.java`) compiles your files and nothing else. The next lesson needs a testing library, and Lesson 8, one for reading JSON; without a way to declare dependencies, there is no path forward.
2. **It leaves nothing behind.** It compiles on every run and does not save the result: there is no “compiled program” you can hand over, and no way to say “compile, but do not run yet”. It also has nowhere to put tests and nobody to run them.
3. **It has no shape.** When the program has ten files, three people and two years, “a folder with files” stops being a structure. Every Java project in the world shares a folder convention, and that convention is what lets a tool, or a new person, understand them without anyone explaining.

The tool is **Maven**, which compiles, tests and packages Java projects from a file that describes them. You install it once, and from then on every Java project that uses Maven is built with the same command. To use it you have to take two previous steps, which the lesson explains in order: put the code in **packages** (Java's way of ordering its classes by name and by folder), and learn which classes each one sees of the others (the **visibility**). 

The lesson has four parts: **packages and visibility** (6.1); **Maven**: installing it, its structure, its `pom.xml`, its lifecycle and the move of the `revisor` (6.2); **the full cycle and the `.jar`** (6.3); and **the project step** (6.4). With that destination in mind, this is what you will see at the end, when you build the complete project:

```text
$ mvn -B verify
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
...
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] No tests to run.
[INFO] 
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.6.0.jar
[INFO] BUILD SUCCESS
```

Thirteen files compiled, one `.jar` built and one line that goes unnoticed: `No tests to run.`, “there are no tests to run”. By the end you will know where each line comes from, what ended up inside the `.jar`, and why that `BUILD SUCCESS` with zero tests is exactly the question Lesson 7 starts with.

## The concepts

### 6.1 Packages and visibility

#### 6.1.1 What the launcher does not do

The source-file launcher (`java Revisor.java`) is a convenience that Java added so that learning does not require a build tool. On every run it compiles in memory the files the program needs, runs the result and leaves nothing on disk ([JEP 458](https://openjdk.org/jeps/458), final since Java 22). It is perfect for what you have done so far and it has three limits that now hurt. The first can be provoked in two lines. Write a file that uses JUnit, the testing library you will use in Lesson 7:

```java
// fig06_01.java
import org.junit.jupiter.api.Test;

void main() {
  IO.println("revisor: todavía no revisa nada");
}
```

It is the usual program, with one extra line: an `import` of a JUnit class. An `import` (you know it from the files that used `java.util`) tells the compiler which package a name comes from. Run it:

```text
$ java fig06_01.java
fig06_01.java:2: error: package org.junit.jupiter.api does not exist
import org.junit.jupiter.api.Test;
                            ^
1 error
error: compilation failed
```

The compiler looks for the package `org.junit.jupiter.api` and does not find it, because the launcher does not know where to get it from: there is no place to declare that your program depends on a library. When you see `package ... does not exist` with a name that is not yours, it almost always means that: a dependency is missing, not a typo.

The second limit produces no message, but a habit: since the launcher compiles everything on every run and saves nothing, “the compiled output” does not exist as a product. There is no `.class` to hand over, and no way to say “compile, but do not run yet”. With thirteen files the cost in time is a fraction of a second (measured: about 0.3 seconds on this machine), but the cost in order grows with the project. The third limit is about tests: the launcher does not know what one is, where it lives, or how to run them all together.

Before Maven there is one more piece that Java demands and that your project does not have yet: packages.

#### 6.1.2 Packages: a name and a folder

Imagine that two people write, each on their own, a class called `Servicio`. If both end up in the same program, the compiler cannot tell which is which. The problem is old and so is Java's solution: each class lives in a **package**, which is a dotted name —`mx.habil.revisor`— and is part of the class's full name. Your `Servicio` becomes `mx.habil.revisor.Servicio`, and the other person's, `com.otraempresa.inventario.Servicio`. The same happens in the JDK: `java.util.List` exists and `java.awt.List` exists, two different things with the same short name.

The convention for choosing the name is to use the domain of whoever writes the code, reversed (`habil.mx` becomes `mx.habil`), followed by the project name (`revisor`). A domain is unique in the world, so the name is too. The language rules are three, and all three are in the [specification, chapter 7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html):

1. **The file declares its package on the first line** (not counting comments): `package mx.habil.revisor;`.
2. **The folder has the same name as the package**, with one subfolder for each dot: the file lives in `mx/habil/revisor/`.
3. **To use a class from another package you import it**: `import mx.habil.revisor.Estado;`. Classes in the same package see each other without an `import`, and those in `java.lang` (like `String`) always.

Let us see it with the smallest program that can be written inside a package. It is a class, `Hola`, with a `main` that prints the full name of its own class:

```java
// fig06_02/src/mx/habil/revisor/Hola.java

package mx.habil.revisor;

/** Una clase mínima dentro de un paquete. */
public final class Hola {

  private Hola() {}

  /** Imprime el nombre completo de su propia clase. */
  public static void main(String[] args) {
    System.out.println("hola desde " + Hola.class.getName());
  }
}
```

Three things are new. The first is the `package`. The second is that this class is no longer a compact source file: it has its `class` and its full `main`, `public static void main(String[] args)`, which section 6.1.4 explains. The third, `Hola.class.getName()`, asks Java for the class's name as Java knows it; it is used here to see that the package is part of it. (The comments between `/**` and `*/` are documentation; they are explained in 6.1.3.)

Compiling and running a file with a package is no longer `java Hola.java`. It takes two steps, and both have a detail:

```text
$ cd fig06_02
$ javac -d out src/mx/habil/revisor/Hola.java
$ java -cp out mx.habil.revisor.Hola
hola desde mx.habil.revisor.Hola
```

`javac -d out` says “leave the `.class` files in the `out` folder”, and `javac` does something you do not see: it **creates the package's subfolders** (`out/mx/habil/revisor/Hola.class`). The `.class` files end up in a folder with the same shape as the code's. When running, `-cp out` is the abbreviation of *classpath*: the list of places where the JVM looks for `.class` files. With that the JVM knows where to start looking, and `mx.habil.revisor.Hola` is the class's **full name**, with its package: the JVM turns it into the path `mx/habil/revisor/Hola.class` and looks for it inside `out`.

Now the two mistakes everyone makes the first time. The first is typing only the short name:

```text
$ java -cp out Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola
```

The JVM looks for `Hola.class` directly inside `out`, does not find it (it is two levels further down) and says so with an exception with a long name, `ClassNotFoundException`: “there is no class called `Hola`”. The fix is to type the full name. The second mistake is “going into the class's folder” and running from there, thinking that if you are standing next to the file it will find it:

```text
$ cd out/mx/habil/revisor
$ java Hola
Error: Could not find or load main class Hola
Caused by: java.lang.NoClassDefFoundError: Hola (wrong name: mx/habil/revisor/Hola)
```

Here it does find a `Hola.class`, but its true name is `mx/habil/revisor/Hola`, not `Hola`, and the JVM rejects it with `(wrong name: mx/habil/revisor/Hola)`: “the file says the class is named something else”. It is the same rule seen from the other side. **The class's name and its file's path have to match, and the JVM checks it.** That is why programs with packages are always run from the root of the classes (`out`, or `target/classes` in Maven), with the full name.

One question remains that the compiler has the right to ask you: what happens if the file declares a package and sits in another folder? The answer is less dramatic than it seems, and it is worth knowing as it is. `Clasificador.java` was moved one folder above where it belonged, leaving its `package mx.habil.revisor;` line intact, and `javac` was asked to compile `Revision.java` looking for the other classes by folder:

```text
$ javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java
src/main/java/mx/habil/revisor/Revision.java:14: error: cannot find symbol
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
                                ^
  symbol:   variable Clasificador
  location: class Revision
1 error
```

The compiler goes out to look for `mx/habil/revisor/Clasificador.java` because `Revision` mentions it, does not find it where the package name says it should be, and concludes that it does not exist. The surprising part is the opposite: **when all the files are handed to it explicitly, `javac` does not complain**, and Maven does it that way (measured: the project compiles and the `.class` files end up where they belong even though the `.java` is in the wrong folder). The folder that lies does not break Maven's compilation; it breaks everything that searches by folder: editors, searches, incremental compilation, and the person who arrives after you. It is a silent error, one of the worst: the rule is that the folder always tells the truth, even though the compiler, today, lets you lie.

There is one last mistake in this family, that of the file that is not named like its public class. The file `Revisor.java` in this example declares a class with another name:

```java
package mx.habil.revisor;

public final class Principal {}
```

```text
$ javac -d out mx/habil/revisor/Revisor.java
mx/habil/revisor/Revisor.java:3: error: class Principal is public, should be declared in a file named Principal.java
public final class Principal {}
             ^
1 error
```

A `public` class must live in a file with the same name. It is another match that the compiler does demand. In the compact source files of the first lessons it went unnoticed, because the implicit class takes the file's name (`fig02_04.java` defines a class `fig02_04`).

#### 6.1.3 Visibility: what each class sees of the others

With packages a question appears that did not make sense before: can a class in `mx.habil.otro` use one in `mx.habil.revisor`? The answer is given by **visibility**, which is written with a word at the start of the declaration. Java has four levels, and the course uses three:

| You write | Who sees it | What it is for |
|---|---|---|
| `private` | only the class where it is declared | internal details: a field, a constructor nobody should call |
| *(nothing)* | any class in the **same package** | almost the whole project: this is “package” visibility |
| `public` | any class in any package | what is offered to the world: the entry point |

(The fourth, `protected`, mixes package visibility with inheritance between classes; the course does not use inheritance, and it does not appear.)

The rule is: **the narrowest visibility that works**. It is not a matter of style. Everything `public` is a promise to whoever uses it: if tomorrow you change it or remove it, you break that person. A package-level class is promised only to your own package, and you can change it without telling anyone. Until now everything was package-level without you noticing: your classes carried no word in front. Now that there is more than one package in the world, the compiler enforces it. Two example files: a package-level `Estado` in `mx.habil.revisor`, and a class from another package, `mx.habil.otro`, that wants to use it.

```java
package mx.habil.revisor;

enum Estado {
  ARRIBA,
  CAIDO
}
```

```java
package mx.habil.otro;

import mx.habil.revisor.Estado;

class Intruso {
  Estado estado = Estado.ARRIBA;
}
```

Compiling them together:

```text
$ javac -d out src/mx/habil/revisor/Estado.java src/mx/habil/otro/Intruso.java
src/mx/habil/otro/Intruso.java:3: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
import mx.habil.revisor.Estado;
                       ^
src/mx/habil/otro/Intruso.java:6: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
  Estado estado = Estado.ARRIBA;
  ^
src/mx/habil/otro/Intruso.java:6: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
  Estado estado = Estado.ARRIBA;
                  ^
3 errors
```

That is what happens when a class in `mx.habil.otro` tries to use a package-visibility `Estado` from `mx.habil.revisor`: `Estado is not public in mx.habil.revisor; cannot be accessed from outside package`. The message is clear, and the fix is almost never “put `public` on it”: it is to ask yourself whether that other class should be using `Estado` at all.

In the `revisor`, **everything has package visibility except one class: `Revisor`**, the entry point, which is the only thing someone from outside needs. This is how, for example, your project's `Estado` looks in its new home:

```java
// fig06_03/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

It is the same `enum` from lesson 3, with a single extra line: the `package` at the top. It carries no visibility word in front, so it is package-level, and every class in `mx.habil.revisor` sees it and none from outside does. In the only place where `public` does appear (the `Revisor`, in 6.2.6) a documentation comment is also required, written between `/**` and `*/`, called **Javadoc**: it is the comment that Java's tools (and your editor) show when someone asks “what is this?”. The project rule from this lesson on is that every `public` class and method carries a Javadoc of at least one sentence ending in a period; in [Lesson 10](10-el-programa-terminado.md) a tool will check it. Since almost everything is package-level, that mandatory Javadoc stays at the minimum.

#### 6.1.4 `public static void main(String[] args)`: the return of the long form

In lessons 1 to 5 you wrote `void main()` and nothing else. Now the `Revisor` goes back to the form you have seen in any outside Java code: a class, and inside it, a `main` method with three extra words:

- `public`: the JVM calls it from outside, so it has to be visible to everyone.
- `static`: it belongs to the class, not to an object. It can be called without having created one.
- `String[] args`: the list of arguments the person typed after the program's name on the command line (`java ... Revisor uno dos`). It is not used today; Lessons 9 and 10 do use it.

It is fair to ask why go back to a longer form if Java 25 accepts `void main()`. The honest answer has two parts. The first is that **the short form still works inside a package**: you can check it, and the program runs. The second is that it has a condition the `Revisor` does not meet, and the error is instructive. A class that only holds a `main` has no reason to let anyone instantiate it, so it gets a `private` constructor, which is what `private Revisor() {}` does. But an instance `main` needs the JVM to be able to create the object. This class, inside a package, has the short `main` and the private constructor:

```java
package mx.x;

public final class Prueba {
  private Prueba() {}

  void main() {
    IO.println("hola");
  }
}
```

```text
$ javac -d out mx/x/Prueba.java
$ java -cp out mx.x.Prueba
Error: no non-private zero argument constructor found in class mx.x.Prueba
remove private from existing constructor or define as:
   public mx.x.Prueba()
```

`no non-private zero argument constructor found`: the JVM looks for a constructor with no parameters that is not private, and there is none. The classic form does not depend on that, works with every JDK version since the first, and is the one recognized by all the tools and all the outside code you are going to read. For a program that will live for years, that is the good reason: **do not depend on a new convenience if the old form costs nothing.** `IO.println` also keeps working in any class, but the project uses `System.out`, which you already know from lesson 5, because it is what you will find everywhere.


### 6.2 Maven: installing it, its structure and its `pom.xml`

#### 6.2.1 What Maven does, in four jobs

Maven is a build tool: it receives a Java project and does with it, always in the same order, what you used to do by hand. Its jobs are four. **Compile** the code (it calls `javac` for you, with the right options). **Run the tests** and stop if any fails. **Package** the result into a `.jar` file (a compressed archive of classes, similar to a `.zip`). And **fetch the dependencies**: when your project says “I need library X version Y”, Maven downloads it from a public repository, [Maven Central](https://central.sonatype.com/), stores it on your computer and puts it within the compiler's reach.

What makes Maven special is an idea with a name: **convention over configuration**. Instead of every project telling, in a configuration file, where its code is, where its tests are and where it leaves the results, Maven fixes a folder structure that everyone uses, and the project's file says only what departs from the usual. The consequence is the one that matters: whoever knows one Maven project knows them all, and the tools (the editor, the continuous-integration server, the code analyzer) know where to look without being told. The [official introduction](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) tells the same thing in five minutes of reading.

Maven does not do the work itself. It is an orchestrator: each job is done by a specialized piece called a ***plugin*** (the compiler is one, the one that runs the tests is another, the one that assembles the `.jar` is another), and Maven decides when to call which. That division has a practical consequence you will see in the `pom.xml`: each *plugin* has its own version.

#### 6.2.2 Installing Maven 3.10.0 with SDKMAN

Maven is installed with the same tool you used to install Java in [Lesson 1](01-instalacion.md): SDKMAN. As with Java, it works in your home folder, without `sudo` and without touching the system's programs, and lets you choose the exact version. The course pins **Maven 3.10.0**, which the [Maven download page](https://maven.apache.org/download.cgi) marks as the current version, recommended for everyone (it requires JDK 8 or higher to run; with your Temurin 25 that is more than enough). Pinning the version matters for a reason you can see on screen: measured with Maven 3.9.11 on Java 25, at startup it prints four `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` warnings (they come from an internal Maven library, not from your code); 3.10.0 does not print them. What is wanted is for your output to be the same as this lesson's.

First, check that SDKMAN offers it. `sdk list maven` shows the available versions; on October 13, 2026, on a Linux Mint 22.3, the list included `3.10.0` (and also `4.0.0-rc-…` versions, candidates for the next major version that is not final yet and that the course does not use). Then install it with its full number:

```text
$ sdk install maven 3.10.0
...
Done installing!

Setting maven 3.10.0 as default.
```

Unlike with Java, here you do type the number as is: `3.10.0` is the course's version, and it does not change even if others come out. The last line says it is left as the default. If instead SDKMAN **asks** whether you want it to be the default, answer yes, with `Enter` or `Y`; and if you answered no, you fix it with `sdk default maven 3.10.0`. Now, as in Lesson 1, **open a new terminal**, so that it picks up SDKMAN's changes, and check:

```text
$ mvn --version
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
Maven home: ~/.sdkman/candidates/maven/current
Java version: 25.0.3, vendor: Eclipse Adoptium, runtime: ~/.sdkman/candidates/java/25.0.3.0+9-tem
...
OS name: "linux", ... arch: "amd64"
```

That is the output measured on October 13, 2026 on the Mint 22.3 (abbreviated: the three dots mark what was removed, and the paths were shortened with `~`; Maven prints them in full, such as `/home/<your user>/.sdkman/candidates/maven/current`). In yours you should recognize three things. **`Apache Maven 3.10.0`** on the first line: the course's version. **`Java version: 25.x`**, with whatever patch you have, and **`vendor: Eclipse Adoptium`**: Maven is using your Temurin 25 (Adoptium is the project that publishes Temurin). And the two paths, `Maven home` and `runtime`, inside `~/.sdkman/candidates/`: both tools come from SDKMAN. If `Java version` says anything other than 25, Maven is using another JDK: either the terminal is old, or a `JAVA_HOME` variable points elsewhere. SDKMAN keeps that variable up to date when you switch versions with `sdk default` or `sdk use` (the box in Lesson 1, 1.1.7); check it with `echo $JAVA_HOME`.

#### 6.2.3 The standard folder structure

With Maven installed, what is left is to give it a project with the shape it expects. This is it, and it is the same in every Maven project in the world:

```text
revisor/
  pom.xml                         ← la descripción del proyecto
  servicios.txt                   ← la configuración del revisor (sigue en la raíz)
  src/
    main/java/mx/habil/revisor/   ← el código del programa
    test/java/mx/habil/revisor/   ← el código de las pruebas
  target/                         ← lo que Maven genera; se puede borrar entero
```

Each folder has a single role. **`src/main/java`** is the program's code, with one subfolder for each dot in the package name, as in `out` a moment ago. **`src/test/java`** is the tests' code, with **the same package structure** as the program: the test for `Clasificador`, which you will write in Lesson 7, will be `mx.habil.revisor.ClasificadorTest`. That the tests live in the same package is neither a coincidence nor an ornament: it is what lets them see the package-visibility classes that the program keeps closed to the world (6.1.3). A test that had to ask for `public` on everything would force you to open up the program just to be able to test it.

**`target/`** is what Maven produces: the program's `.class` files in `target/classes`, the tests' in `target/test-classes`, the test reports and the `.jar`. It is a **disposable** folder: it can be deleted entirely at any moment and is regenerated with one command, because everything it contains comes from what is in `src`. Two rules follow: nothing inside `target` is ever edited, and it is never uploaded to a repository (if someday you keep this project with Git, add `target/` to its `.gitignore` file). `servicios.txt` stays at the root because the program looks for it by a relative path, from the folder where it is run, and you will always run from the project root.

#### 6.2.4 The `pom.xml`, line by line

The file that describes the project is called `pom.xml` (*POM* stands for “project object model”; `.xml` is the format). **XML** is a text format made of nested tags: each element is opened with `<name>` and closed with `</name>`, and what goes inside is its value or its children. It is noisier than Lesson 8's JSON, but it is the one Maven chose and you will not be writing it from scratch very often; you will be reading it, and that is why it is worth understanding it once in full. This is the project's, complete:

```xml fig06_03/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.6.0</version>

  <properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

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

**The project's data.** `<modelVersion>4.0.0</modelVersion>` is the version of the `pom.xml` format itself; it is always `4.0.0` and is written without thinking. The next three, `groupId`, `artifactId` and `version`, are the project's **coordinates**, the address by which any Maven project in the world can refer to this one. The `groupId` is who publishes it (`mx.habil`, same as the package name); the `artifactId` is the project's name (`revisor`); and the `version` says what point it is at. Here it is `0.6.0` because this is the course's state in Lesson 6; each lesson will raise it, and Lesson 10 will leave it at `1.0.0`. The coordinates look like a formality until in Lesson 7 you declare your first dependency: a dependency **is** a `groupId`, an `artifactId` and a `version`. And they decide something you will see in 6.3: the name of the `.jar`.

**The properties.** `<properties>` groups named values that other parts of the file (or of the *plugins*) read. Two are indispensable. `maven.compiler.release` set to `25` tells the compiler “compile as Java 25”, which includes a guarantee that the old `source`/`target` pair did not give: with `release`, the compiler not only generates the format of the requested version, but **rejects any JDK class that did not exist in that version**. And it works as an early warning; this is what happens if someone tries to build the project with a JDK 21 installed:

```text
$ mvn -B compile
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile (default-compile) on project revisor: Fatal error compiling: error: release version 25 not supported -> [Help 1]
```

`release version 25 not supported`: the JDK Maven uses does not know how to compile for Java 25. It is the error seen by whoever has Mint's `default-jdk` installed (Java 21, [Lesson 1](01-instalacion.md)) and whoever follows a tutorial with another version: instead of failing halfway with a strange error, the project refuses from the first second and says why. The other property, `project.build.sourceEncoding`, set to `UTF-8`, declares the encoding of your `.java` files; it is written explicitly so that the result does not depend on the computer where it is compiled, and so that the accents of `"sin conexión"` (“no connection”) are always the same.

**The *plugins*.** Inside `<build>`, `<plugins>` declares the three the project needs today: the compiler (`maven-compiler-plugin`), the one that runs the tests (`maven-surefire-plugin`, which today has no tests to run and which Lesson 7 puts to work) and the one that assembles the `.jar` (`maven-jar-plugin`). Each one carries its `<version>` written out, and that is a decision, not an oversight. Maven can work without it, because it ships a default version of each essential *plugin*; this is what happens if you remove the version from one:

```text
$ mvn -B verify
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for mx.habil:revisor:jar:0.6.0
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-surefire-plugin is missing.
```

Maven warns that the version is missing and that “future Maven versions might no longer support building such malformed projects”. There is a more practical reason: **if you do not pin the version, your build changes when Maven changes**, not when you decide. The same project, the same command, and a different result the day someone updates the tool. Pinning the version of every piece is what makes a build reproducible: Maven's guide on [reproducible builds](https://maven.apache.org/guides/mini/guide-reproducible-builds.html) deals with this problem and starts by asking for *plugins* to be at concrete, up-to-date versions. The same will hold for dependencies.

One more detail before using it. The `pom.xml` above does not carry the `<?xml ... ?>` declaration that appears in almost every example on the internet. It is optional, and leaving it out avoids a classic error: that declaration has to be the first thing in the file, with no comment or space before it, and whoever writes it and then adds a comment above gets an invalid file with an unhelpful message.

#### 6.2.5 The lifecycle

Maven does not run loose *plugins*, it runs **phases**, and each phase calls the *plugins* that correspond to it. The phases form a chain in fixed order, the **lifecycle** ([official documentation](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)), and one simple rule governs it: **when you ask for a phase, Maven first runs all the previous ones.** The ones you will use in the course are four:

| Phase | What it does | What it leaves |
|---|---|---|
| `compile` | compiles the code in `src/main/java` | `target/classes` |
| `test` | compiles the tests and runs them (there are none until Lesson 7) | `target/test-classes` and `target/surefire-reports` |
| `package` | assembles the `.jar` with what was compiled | `target/revisor-0.6.0.jar` |
| `verify` | final checks on the package (for now, no more) | — |

By the rule above, `mvn verify` compiles, tests and packages; `mvn test` compiles and tests; and `mvn compile` only compiles. In the course `verify` is used as “the full command”: in Lesson 10 you will hang the quality checks on it, and the command you already know will not change. And always with `-B` (*batch*): it tells Maven not to use colors or progress bars or ask questions, which is the way to ask it for clean output that you can copy, paste into a message or read on a server with no screen.

#### 6.2.6 Moving the `revisor` into a project

It is time to do what the folder tree shows. There are three steps and one optional improvement, and none changes what the program does. From `~/curso-java/revisor/`:

```text
$ mkdir -p src/main/java/mx/habil/revisor src/test/java/mx/habil/revisor
$ mv *.java src/main/java/mx/habil/revisor/
```

`mkdir -p` creates a folder and all the ones missing along its path. `mv` moves the files; be careful to run it from `revisor/` and to make sure `servicios.txt` does **not** get moved (the `*.java` does not touch it). Now each file needs its `package` line. Doing it by hand in thirteen files is boring work, so this terminal loop does it for you: for each file, it writes the new line and then the original content into a temporary file, and renames it over the original:

```text
$ for f in src/main/java/mx/habil/revisor/*.java; do { printf 'package mx.habil.revisor;\n\n'; cat "$f"; } > "$f.nuevo" && mv "$f.nuevo" "$f"; done
```

Check it by opening any of them: the first line must be `package mx.habil.revisor;`, followed by a blank line. Two things remain that the terminal cannot do for you, because they require understanding the code.

**The first is the `Revisor`.** Your `Revisor.java` from lesson 5 was a compact source file: a `main` with no class around it. Now it is a class, the only public one in the project, with its private constructor (6.1.3) and the long-form `main` (6.1.4). It is short, so it goes in full:

```java
// fig06_03/src/main/java/mx/habil/revisor/Revisor.java

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

Notice what did **not** change: the body of the `try` is lesson 5's, line by line. It reads the configuration, checks the services in the order of the list, builds the report, prints the table; and if the configuration is wrong, the `catch` prints the message in Spanish to the error output. The only new thing is the wrapper, and three things that change meaning: the compact source file saw the whole `java.base` module without asking, and a normal class needs its own `import` statements (that is why `Path` and `List` appear); the `main` is now the one the JVM calls (and that is why it must be `public static`); and the classes it used to see “because they were in the same folder” it now sees because they are in the same package.

**The second is a possibility the package opens, and it is optional.** In lessons 3 to 5, with no package, every time `Respondio` was needed it had to be written as `Respuesta.Respondio`, because **nothing can be imported from an unnamed package** (if you try, the compiler says `package R does not exist`). With a package, `import mx.habil.revisor.Respuesta.Respondio;` does work, and the code can say plain `Respondio`. The tests you will write in Lesson 7 use it; the program's code, as you bring it from lesson 5, keeps working untouched, and you can leave it as it is.

#### 6.2.7 The first build

With the project moved, this is the first build. From the root (`~/curso-java/revisor/`, where the `pom.xml` is), and with the `pom.xml` above, which still has no tests:

```text
$ mvn -B compile
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------------------< mx.habil:revisor >--------------------------
[INFO] Building revisor 0.6.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.5.0:resources (default-resources) @ revisor ---
[INFO] skip non existing resourceDirectory ~/curso-java/revisor/src/main/resources
[INFO] 
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

Read it from top to bottom, because each `--- plugin:version:goal ---` line tells you which piece worked. `resources:resources` copies files that are not code (there are none here: “skip non existing resourceDirectory”). `compiler:compile` does the work: `Compiling 13 source files with javac [debug release 25] to target/classes` says how many files it compiled, with which options (`debug` means it keeps the variables' names, which explains the next paragraph) and where it left the `.class` files. And `BUILD SUCCESS` is the notice that no phase failed. The first time you run Maven you will also see many `Downloading` lines that were removed here: Maven downloads its own *plugins* from Maven Central and stores them in `~/.m2/repository`, a hidden folder in your home that works as a **local cache**. That explains two things: the first run is slow and needs internet, and the following ones are fast and work offline.

With the classes in `target/classes`, the program runs with the same `-cp` as a while ago, now with Maven's classes root:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

It is the usual table, with the same summary: `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms` (4 services, 2 up, 1 slow, 1 down, average 843ms). The program calculates that number every time, and Lesson 7's tests are going to watch over it.

The `debug` in the `Compiling` line has a hidden prize that connects with [Lesson 2](02-tipos-cadenas-control.md): the `NullPointerException` message that with `java Revisor.java` said `"<local1>"` (the launcher compiles without that information) now says the variable's name. With a test class, compiled by Maven, that calls `detalle.isBlank()` on a `null`:

```text
$ java -cp target/classes mx.habil.revisor.Nulo
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.isBlank()" because "detalle" is null
	at mx.habil.revisor.Nulo.main(Nulo.java:6)
```

`because "detalle" is null`: the name of the variable that caused the error. Maven compiles with debugging information by default, and that is why the messages from the project's programs are easier to read than those from loose files.

#### 6.2.8 Three checks before moving on

If you got here, the project already compiles and runs. Check three things, in this order:

1. `mvn -B compile` ends in `BUILD SUCCESS` and says `Compiling 13 source files`.
2. `java -cp target/classes mx.habil.revisor.Revisor` prints the four-service table with the 843 ms summary.
3. Run from another folder, the same command does not find `servicios.txt`: that is the correct behavior, and you will see it in “The error you will see”.

If any fails, the error is almost always from the same three families: a file without its `package` line (the compiler says it cannot find a class), a file in the wrong folder (6.1.2), or a command run outside the project root, where there is no `pom.xml`:

```text
$ cd ~/curso-java
$ mvn -B verify
[ERROR] BUILD FAILURE
[ERROR] The goal you specified requires a project to execute but there is no POM in this directory (~/curso-java). Please verify you invoked Maven from the correct directory. -> [Help 1]
```

Maven says it in so many words: there is no POM in the folder where you are. What is missing is a `cd` to the project root.


### 6.3 The full cycle and the `.jar`

#### 6.3.1 What `mvn -B verify` does today

So far you used `compile`, the first phase. Now ask for the last one, the one you will type for the rest of the course. From the project root:

```text
$ mvn -B verify
[INFO] Building revisor 0.6.0
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] 
[INFO] --- resources:3.5.0:testResources (default-testResources) @ revisor ---
[INFO] skip non existing resourceDirectory ~/curso-java/revisor/src/test/resources
[INFO] 
[INFO] --- compiler:3.16.0:testCompile (default-testCompile) @ revisor ---
[INFO] No sources to compile
[INFO] 
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] No tests to run.
[INFO] 
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.6.0.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

Read it with the table from 6.2.5 beside you, because each `--- plugin:version:goal ---` block is a phase at work, in the order of the chain. First, the `compile` phase, the same as in 6.2.7: thirteen files into `target/classes`. Then come three lines that belong to the `test` phase and that have no work yet. `testResources` looks for auxiliary files for the tests in `src/test/resources` and there are none. `testCompile` is the same compiler, but applied to `src/test/java`, and says `No sources to compile`: the tests folder you created in 6.2.6 exists and is empty. And `surefire:test`, the *plugin* that runs the tests, says `No tests to run.`, because there are none. Finally, the `package` phase calls `jar:jar`, which assembles the file `target/revisor-0.6.0.jar`. The `verify` phase adds nothing yet (Lesson 10 will hang the quality checks on it), and Maven closes with `BUILD SUCCESS`.

Two details deserve a second look. The first is the name of the `.jar`: you did not write `revisor-0.6.0.jar` anywhere, Maven builds it from the `artifactId` and the `version` in the `pom.xml`. When the version goes up, the file's name will go up with it; that is why the coordinates are not a formality. The second is the *Surefire* line. Notice what happened: Maven did not run a single test and finished green. Today that is the truth, because you wrote no tests, and the report says so in so many words. In Lesson 7 you will see that a green with no tests can also appear when you **did** write tests, without that notice, and why the number of tests is always read from the report and never from the `BUILD SUCCESS`. Keep this image: `No tests to run.` is what Maven says when it knows there are no tests.

#### 6.3.2 What is inside the `.jar`

A `.jar` file (from *Java ARchive*) is a compressed file with the same format as a `.zip`, which stores compiled classes and a bit of information about them. The JDK comes with a tool to handle it, which is named the same, `jar`, and its `tf` option lists the contents (`t` is “list”, `f` is “of the file I tell you”):

```text
$ jar tf target/revisor-0.6.0.jar
META-INF/
META-INF/MANIFEST.MF
mx/
mx/habil/
mx/habil/revisor/
META-INF/maven/
META-INF/maven/mx.habil/
META-INF/maven/mx.habil/revisor/
mx/habil/revisor/Clasificador.class
mx/habil/revisor/Configuracion.class
mx/habil/revisor/ErrorDeConfiguracion.class
mx/habil/revisor/Estado.class
mx/habil/revisor/Formato.class
mx/habil/revisor/Reporte.class
mx/habil/revisor/Respuesta$Respondio.class
mx/habil/revisor/Respuesta$SinRespuesta.class
mx/habil/revisor/Respuesta.class
mx/habil/revisor/Resultado.class
mx/habil/revisor/Resumen.class
mx/habil/revisor/Revision.class
mx/habil/revisor/Revisor.class
mx/habil/revisor/Servicio.class
mx/habil/revisor/Simulador.class
META-INF/maven/mx.habil/revisor/pom.xml
META-INF/maven/mx.habil/revisor/pom.properties
```

There are three groups. The biggest one is your classes, and they come **with their package's folders**: `mx/habil/revisor/Clasificador.class`, exactly the shape that `javac -d out` produced in 6.1.2 and that the JVM requires in order to find a class by its full name. A `.jar` is, at bottom, a `target/classes` folder packed into a single file. Count the classes: there are fifteen, from thirteen source files. The two extra ones are `Respuesta$Respondio.class` and `Respuesta$SinRespuesta.class`: the two *records* that live **inside** the `Respuesta` interface (Lesson 3). Each nested class or *record* is compiled into its own `.class`, with the name of the class that contains it, a `$` and its own name.

The second group is `META-INF/MANIFEST.MF`, the **manifest**: a text file with data about the `.jar` that the JVM reads. The third, `META-INF/maven/...`, is a copy of the `pom.xml` and a file with the coordinates; Maven adds it so that whoever finds the loose `.jar` knows which project it came from and at what version. What is **not** there matters too: not a single `.java` (the `.jar` carries what was compiled, not the source), not `servicios.txt` (it is at the project root, not in `src/main/resources`, so it is not part of the program), and nothing from JUnit or from tests.

#### 6.3.3 Running from the `.jar`, and what is still missing

Since the `.jar` contains the same as `target/classes`, it works equally well as a *classpath*:

```text
$ java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

The same table, now from a single file that you could copy to another computer with Java 25. There is a shorter form, `java -jar file.jar`, which is the one used by Java programs that are handed to other people. Try it:

```text
$ java -jar target/revisor-0.6.0.jar
no main manifest attribute, in target/revisor-0.6.0.jar
```

“No main manifest attribute”. With `-jar`, the JVM is not given the name of the class it must start: it looks for it in the manifest, on a line called `Main-Class`. And the manifest Maven assembled does not have it. You can see it, because `unzip -p` prints a file from inside the `.jar` without extracting it:

```text
$ unzip -p target/revisor-0.6.0.jar META-INF/MANIFEST.MF
Manifest-Version: 1.0
Created-By: Maven JAR Plugin 3.5.1
Java-Version: 25
Build-Jdk-Spec: 25
```

Four lines: the format's version, who created it, and with which Java. Nothing says which is the entry class. (If your JVM is in Spanish, the error message comes out translated; measured with the JVM in Mexican Spanish: `no hay ningún atributo de manifiesto principal en target/revisor-0.6.0.jar` (“there is no main manifest attribute in target/revisor-0.6.0.jar”). The exit code is 1 in both languages.) That line could be added with the `jar` *plugin*'s configuration, but it would not solve the underlying problem, which appears in Lesson 8: when the program uses a library, the `.jar` will have your classes and not the library's, and the JVM will not find them. [Lesson 10](10-el-programa-terminado.md) fixes both things at once with a `.jar` that carries everything inside and knows where to start. Meanwhile, the course's command is the `-cp` one, which works with `target/classes` and with the `.jar` alike.

#### 6.3.4 `target/` can be thrown away

You now have in view everything that lives in `target/`: `classes` with the `.class` files, the `.jar`, and some Maven working folders (`maven-status`, `maven-archiver`, `generated-sources`) that keep data for the next run. **All of that comes from `src/` and the `pom.xml`**, and that is why it can be deleted entirely at any moment: the next `mvn -B verify` creates it again just the same. That is the proof that a project is well put together. If you ever delete `target/` and the project no longer builds, there was something important where it should not have been.

From there come the two rules you already saw in 6.2.3, worth repeating because they are the ones most often broken. **Nothing inside `target/` is ever edited**: a change there is lost on the next build, and the worst part is that in the meantime it looks like it worked. And **`target/` is never stored in a repository**: you would be storing the result and not the source, and every person who builds the project would see changes in files they did not touch. What is stored is `src/`, `pom.xml` and what the program needs in order to run, such as `servicios.txt`.


### 6.4 The project step: the `revisor` as a Maven project

You have all the pieces. This is the complete project, as it stands at the end of the lesson. You already know most of it: of the program, only the `Revisor` (6.2.6) really changed with respect to Lesson 5, and the other classes only gained their `package` line. All of them are included so that the project can be put together without looking at another lesson:

```text
revisor/
  pom.xml                 versión 0.6.0 · release 25 · tres plugins con versión fija
  servicios.txt           sin cambios
  src/main/java/mx/habil/revisor/
    Revisor.java          NUEVO como clase pública: el único punto de entrada
    Estado.java  Respuesta.java  Servicio.java  Resultado.java  Clasificador.java
    Formato.java  Simulador.java  Revision.java  Resumen.java  Reporte.java
    Configuracion.java  ErrorDeConfiguracion.java       (solo ganan su línea package)
  src/test/java/mx/habil/revisor/                       (vacía: la llena la Lección 7)
```

You have already seen the `pom.xml` (6.2.4), `Revisor.java` (6.2.6) and `Estado.java` (6.1.3) in full. Your `servicios.txt` is the usual one:

```text fig06_03/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

And these are the eleven remaining files. If you have followed lessons 3 to 5, you already have them, and the only difference is what was said in 6.2.6; I include them so that this lesson's project can be built without depending on anything else.

<details>
<summary>The eleven program files that only change in their first line</summary>

```java
// fig06_03/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig06_03/src/main/java/mx/habil/revisor/Servicio.java

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
// fig06_03/src/main/java/mx/habil/revisor/Resultado.java

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
// fig06_03/src/main/java/mx/habil/revisor/Clasificador.java

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
// fig06_03/src/main/java/mx/habil/revisor/Simulador.java

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
// fig06_03/src/main/java/mx/habil/revisor/Revision.java

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
// fig06_03/src/main/java/mx/habil/revisor/Resumen.java

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
// fig06_03/src/main/java/mx/habil/revisor/Reporte.java

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
// fig06_03/src/main/java/mx/habil/revisor/Formato.java

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
// fig06_03/src/main/java/mx/habil/revisor/Configuracion.java

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

```java
// fig06_03/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

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

To finish the lesson: `mvn -B verify` must say `Compiling 13 source files`, `No tests to run.` and `BUILD SUCCESS`, and leave `target/revisor-0.6.0.jar`; `java -cp target/classes mx.habil.revisor.Revisor` and `java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor` must both print the table with the summary from Lesson 4 (4.5) at 843 ms. There is a Hábil article on [how a software house's continuous integration is put together](https://www.habil.mx/en/blog/cicd-devsecops-own-infrastructure/), the place where `mvn -B verify` runs by itself on every change; it is worth reading now that the command is yours.


## The error you will see

The lesson has one main error, that of the class the JVM cannot find, and several supporting messages that are worth knowing how to read at a glance.

**The main error.** It is the one from 6.1.2, and you will see it as soon as you run a program with a package the wrong way:

```text
$ java -cp out Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola
```

It is read in three steps, always the same. **What it was looking for**: the name after `class`, here plain `Hola`. **Where it looked**: in the *classpath* you gave it with `-cp`, here `out`. **What it found**: nothing (`ClassNotFoundException`), or a file with another true name (`NoClassDefFoundError ... wrong name: mx/habil/revisor/Hola`). With those three answers the fix is always one of two: type the class's full name, with its package, or run from the root of the classes (`out`, `target/classes` or the `.jar`). In the `revisor`, the correct form is just one and does not change for the rest of the lesson: `java -cp target/classes mx.habil.revisor.Revisor`, from the project root.

**The supporting messages**, to recognize at a glance:

- `package org.junit.jupiter.api does not exist` (`fig06_01.java`): a dependency is missing. The launcher knows nothing about dependencies; in Maven they are declared in the `pom.xml`, and Lesson 7 declares exactly that one.
- `cannot find symbol` with `symbol: variable Clasificador`, when compiling by searching by folder: a file is in a folder that does not correspond to its package. The folder has to tell the truth (6.1.2).
- `Estado is not public in mx.habil.revisor; cannot be accessed from outside package`: you asked for a package-visibility class from another package. Before putting `public` on it, ask yourself whether that class should be using it.
- `no non-private zero argument constructor found`: an instance `main` in a class with a private constructor. The long form `public static void main(String[] args)` does not have that problem (6.1.4).
- `The goal you specified requires a project to execute but there is no POM in this directory`: you ran `mvn` outside the project root. Go into the folder that contains the `pom.xml`.
- `release version 25 not supported`: Maven uses a JDK older than 25. Check the `Java version` line of `mvn --version`, the `JAVA_HOME` variable and, if there are several JDKs, the box in Lesson 1 about `sdk default`.
- `no main manifest attribute`: you tried `java -jar` with a `.jar` that does not say which is its entry class. Use `java -cp` with the class's name; Lesson 10 solves it.
- `error de configuración: no existe el archivo servicios.txt` (“configuration error: the file servicios.txt does not exist”) when running from another folder, which is the last one and the one that repeats most:

```text
$ cd ..
$ java -cp revisor/target/classes mx.habil.revisor.Revisor
error de configuración: no existe el archivo servicios.txt
```

The program looks for `servicios.txt` by a **relative** path, `Path.of("servicios.txt")`, and a relative path means “the file in the folder where I am standing”, not “the one next to the program”. From the folder above it does not exist. Notice what the program did right: it did not show a thirty-line exception or a `NoSuchFileException`, but the Spanish message from Lesson 5. To know which folder you are in, the `pwd` command tells you. In Lesson 9 the command line's first argument will let you indicate the file without depending on where you are.

## What gets done wrong

**Installing Maven by a different route than Java.** If Java comes from SDKMAN and Maven from somewhere else (a system package, a file unpacked by hand), each one brings its own ideas about which JDK to use, and the day they do not match the error does not say why. With both tools in SDKMAN, `mvn --version` and `java --version` speak of the same Temurin 25.

**Putting `public` on everything “so that it compiles”.** Everything public is a promise to whoever uses it, and every promise is a cost when you want to change it. The `is not public` error almost always means that something is in the wrong place, not that a word is missing.

**A folder that does not match the package, “because it compiles”.** Maven compiles it (6.1.2), but it breaks everything that searches by folder: the editor, searches, incremental compilation and the next person. It is an error that gives no warning, and that is why it is avoided by habit and not by the message.

**Dependencies and *plugins* with no version, or with the latest one.** It makes your build change the day someone publishes a new version or updates Maven, without you having touched anything. Every piece carries its version written out, and it is raised on purpose.

**Editing or storing `target/`.** It is what Maven generates and it can be deleted and regenerated at any moment (6.3.4). A change made there is lost on the next build, and if you upload it to a repository, you are storing the result and not the source.

**Running from any folder and blaming the program.** Maven needs to be in the `pom.xml`'s folder, and the `revisor` needs to be in the `servicios.txt`'s folder: today they are the same, the project root. Before reading a “does not exist” error, read `pwd`.

## Exercises

### Exercise 1 — The folder that lies

Move `Clasificador.java` one folder above the one it belongs in, to `src/main/java/mx/habil/`, without touching its `package mx.habil.revisor;` line. **Before running anything, predict** what each of these two commands does: `javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java` (which compiles `Revision` looking for the other classes by folder) and `mvn -B compile`. Run them and compare. Where did `Clasificador.class` end up inside `target/classes`? Put the file back in its folder and delete the `out` folder that `javac` created.

### Exercise 2 — What Maven decides for you

Remove from the `pom.xml` the line `<version>3.5.1</version>` of the `maven-jar-plugin` *plugin* and run `mvn -B verify`. Read the `WARNING` lines at the start: what do they say is missing, and where? Then look for the `--- jar:...` line in the output: which version of the *plugin* did Maven use? With that answer, explain in your logbook why the `pom.xml` pins the version even if today it gives the same result. Restore the line when you finish.

### Exercise 3 — The wrong JDK

If you have a JDK 21 installed with SDKMAN besides the 25 (SDKMAN's JDKs live in `~/.sdkman/candidates/java/`, one folder per identifier; `ls ~/.sdkman/candidates/java/` shows them to you), point Maven to it for a single command, putting the variable in front: `JAVA_HOME=~/.sdkman/candidates/java/<the identifier of the 21> mvn -B compile`. **First, predict** in which phase it fails and with what message. If you do not have a 21, do not install it for this: read the solution. In both cases, answer: which `pom.xml` property produces the error, and why is it better for it to fail there and not later?

### Exercise 4 — What is in your `.jar`

Run `jar tf target/revisor-0.6.0.jar` and count the files that end in `.class`. **Before counting, predict** how many there should be, knowing that the project has thirteen `.java` files. If the number does not match your prediction, find the extra ones and explain where they come from. Then try `java -jar target/revisor-0.6.0.jar` and explain the message with what you saw in the manifest.

## Solutions

### Solution 1

With `Clasificador.java` one folder above, the two commands behave differently:

```text
$ javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java
src/main/java/mx/habil/revisor/Revision.java:14: error: cannot find symbol
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
                                ^
  symbol:   variable Clasificador
  location: class Revision
1 error
$ mvn -B compile
...
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] BUILD SUCCESS
```

`javac` with `-sourcepath` searches for each class it needs **by folder**: for `mx.habil.revisor.Clasificador` it looks in `src/main/java/mx/habil/revisor/Clasificador.java`, does not find it and concludes it does not exist. Maven, on the other hand, hands `javac` the complete list of files in `src/main/java`, finds all thirteen and compiles without complaint. And `Clasificador.class` ends up in `target/classes/mx/habil/revisor/`, **where its package says**, not where the `.java` was: the compiler arranges the output according to the `package` line. That is why the error is not seen in Maven and is seen in any tool that searches by folder. The program runs the same; the project, on the other hand, already lies.

### Solution 2

Without the version, Maven builds, but warns at the start:

```text
$ mvn -B verify
[INFO] Scanning for projects...
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for mx.habil:revisor:jar:0.6.0
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-jar-plugin is missing. @ line 25, column 15
[WARNING] 
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[WARNING] 
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.
...
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
...
[INFO] BUILD SUCCESS
```

It says what is missing (the version of the `maven-jar-plugin` *plugin*) and where (`@ line 25, column 15`, the line of the `pom.xml` where that *plugin* starts; if your file has another shape, the number changes). And the answer to the second question is what makes the exercise interesting: Maven used **the same 3.5.1** that your `pom.xml` pinned. Today, with Maven 3.10.0, the default version coincides with the course's. But that is a coincidence of this Maven version, not a property of your project: the default version is decided by whoever publishes Maven, and it changes when Maven changes. Pinning it is the difference between “today it gives the same result” and “it always gives the same result”. The sentence for the logbook: **the version is pinned so that the result does not depend on which Maven each person has installed.**

### Solution 3

With a JDK 21, Maven starts, reaches the `compile` phase and stops there:

```text
$ JAVA_HOME=<la carpeta del JDK 21> mvn -B compile
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] ------------------------------------------------------------------------
[ERROR] BUILD FAILURE
...
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile (default-compile) on project revisor: Fatal error compiling: error: release version 25 not supported -> [Help 1]
```

(Measured with a JDK 21 from another distribution; the message comes from the compiler and does not depend on it.) The responsible property is `maven.compiler.release` set to `25`: the JDK 21 compiler does not know how to produce classes for Java 25 and says so before compiling a single file. It is better for it to fail there because the message names the cause (the version) instead of a symptom (a method or a syntax that “does not exist”), and because the error appears in the first phase, before anyone wastes time with tests or half-built packages. Note also what the exercise shows: Maven does **not** necessarily use your terminal's `java`, but the one in `JAVA_HOME` if it is defined. That is why, when Maven and `java --version` do not match, the first thing is to look at that variable.

### Solution 4

There are **fifteen** `.class` files, not thirteen:

```text
$ jar tf target/revisor-0.6.0.jar
...
mx/habil/revisor/Respuesta$Respondio.class
mx/habil/revisor/Respuesta$SinRespuesta.class
mx/habil/revisor/Respuesta.class
...
```

The two extra ones are the *records* nested in the `Respuesta` interface: each class, *record* or `enum` is compiled into its own `.class`, and if it lives inside another, its name is preceded by the name of the one that contains it and a `$`. A `.java` file can produce several `.class` files, and the “one file, one class” relationship is only mandatory for `public` classes (6.1.2). And `java -jar` fails with `no main manifest attribute` because the manifest you saw in 6.3.3 does not have the `Main-Class` line: the JVM, with `-jar`, does not know which class to start from. With `java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor` you tell it yourself, and it works.

## How I know I got it

- [ ] `mvn --version` says `Apache Maven 3.10.0` and a Java version that starts with 25.
- [ ] `mvn -B verify`, from the root of `~/curso-java/revisor/`, says `Compiling 13 source files`, `No tests to run.` and `BUILD SUCCESS`, and leaves `target/revisor-0.6.0.jar`.
- [ ] `java -cp target/classes mx.habil.revisor.Revisor` prints the four-service table with the summary `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`, and the same with the `.jar` in place of `target/classes`.
- [ ] From the folder above, the same command with `revisor/target/classes` prints `error de configuración: no existe el archivo servicios.txt`, and you can explain why.
- [ ] You can explain what happens if a file declares `package mx.habil.revisor;` and is in another folder, and why Maven lets you do it but it is not a good idea.
- [ ] You can explain why there are fifteen `.class` files in the `.jar` and why `java -jar` does not work yet.
- [ ] You noted in your logbook which message from this lesson was hardest for you to read.

## Summary

Answer without looking at the lesson:

1. Which three things can the source-file launcher not do that a Maven project does?
2. What relationship must there be between a package's name, the `package` line and the file's folder? Who checks it, and who does not?
3. What does a package-visibility class see, and what does it not? Which class of the `revisor` is `public`, and why only that one?
4. Why does the `Revisor` use `public static void main(String[] args)` and not `void main()`?
5. What do you install Maven with and which three things do you recognize in `mvn --version` to know it turned out right?
6. What are a Maven project's coordinates? What is pinning the version of each *plugin* for?
7. Which phases does `mvn verify` run and in what order? What does each one leave in `target/`?
8. What is inside a `.jar`, what is not, and why does `java -jar` not run it yet?

## Further reading

- [Maven, “Introduction to the Build Lifecycle”](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) and [“Introduction to the POM”](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html): the phases, which *plugin* runs in each one, and everything a `pom.xml` can carry. Consulted on October 7, 2026.
- [The language specification, chapter 7: packages and modules](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html) and [Oracle's tutorial on packages](https://docs.oracle.com/javase/tutorial/java/package/index.html): the exact rules for names, folders and imports. Consulted on October 7, 2026.
- [Oracle, “JAR File Specification” (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html) and [the `jar` command manual](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jar.html): what a `.jar` carries, what the manifest is and what `Main-Class` is for. Consulted on October 13, 2026.
- [SDKMAN, usage](https://sdkman.io/usage/): `list`, `install`, `use` and `default`, the same commands for Java and for Maven. Consulted on October 13, 2026.

---

**Previous:** [Lesson 5 — Errors and exceptions](05-errores-excepciones.md) ·
**Next:** [Lesson 7 — Tests with JUnit](07-pruebas-junit.md)
