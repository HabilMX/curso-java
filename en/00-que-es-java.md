# Lesson 0 — What Java is and what it is NOT

**Time:** 90 min

**What you build:** nothing yet. It is a reading lesson: here you build the mental map that makes the rest of the course predictable

**What you learn:** the path from source code to a running program (`javac`, *bytecode*, the JVM); what the JDK is and what the JRE was; what LTS means and why the course uses Java 25; what a preview feature is; and what the Java compiler protects and what it lets through (types are checked at compile time, but generics are erased and `null` still exists)

**Where you are coming from.** You bring nothing from this course: you have not installed Java or written a single line. You do bring what you already know from another language —Go, TypeScript, Python or whichever—: what a variable, a loop, a function and an error are. That is exactly what the course takes for granted. What you do not bring is the map of **how the Java platform works**, which is different from that of any of those languages and which explains why Java's commands, error messages and habits are the way they are.

**What this lesson does not do.** It does not install anything (that is [Lesson 1](01-instalacion.md)) and it does not teach Java syntax (that starts in [Lesson 2](02-tipos-cadenas-control.md)). It includes four small programs, of four to eight lines each, only so that you can see with your own eyes what the lesson claims. You do not need to understand them line by line yet, although each one is explained: what matters is what they **print**. You run them after Lesson 1, once you have Java.

**About the runs.** The outputs in this lesson were obtained with Temurin 25.0.4 (a free distribution of Java, explained below) on macOS, with the locale set to UTF-8. On your Linux Mint they come out the same; the only thing that changes, and it is flagged where it happens, are the lines that mention the system. The programs are in [`programas/00-que-es-java/`](../programas/00-que-es-java), in the course repository.

## By the end you will be able to

- Describe in your own words the path of a `.java` file until it runs: what `javac` does, what a `.class` contains and what the JVM does with it.
- Tell the JDK, the JRE and the JVM apart, and say which one to install and why.
- Explain what it means that Java 25 is LTS, why Java 26 and Java 27 exist but the course does not use them, and what a preview feature is.
- Say, for a possible failure, whether the Java compiler catches it before running or whether it only shows up once the program runs.
- Explain what type erasure is and why `null` still exists in a language with types.
- Predict what `"142" + 1` prints in Java and why it is not a sum.

## The why before the how

Imagine someone asks you for the course program, the `revisor`: it reads a list of services, queries all of them at once and hands you a report. In Go you would write it and get an executable file that you copy to a server and it runs. In Python, a script that runs as long as the interpreter is installed. In TypeScript, code that Node runs after the compiler translates it to JavaScript. Three languages, three answers to the same question: **what, exactly, is what gets delivered, and who makes it run?**

Java has its own answer, and it is the first thing to understand, because everything else hangs from it. In Java you write `.java` files, a program called `javac` translates them to `.class` files, and another program called `java` runs them inside a virtual machine. Two programs, two file formats and a virtual machine: if you do not know what each one does, the installation in Lesson 1 is a list of commands obeyed blindly, and the errors in Lesson 2 are messages of mysterious origin.

There is also a practical reason. In Java's literature and tutorials, advice from twenty years ago lives alongside today's, and it almost never says what year it is from. Whoever arrives without the map cannot tell “this was so in 2008” from “this is so in 2026”. With this lesson's map, you can.

The lesson has five parts, in this order:

1. **The path of the code** (0.1): from `.java` to `.class` to the JVM.
2. **The pieces of the platform** (0.2): the JDK, the JRE and the distributions.
3. **The versions** (0.3): what LTS is, what preview is and why 25.
4. **What the compiler protects and what it does not** (0.4): the most useful part of the lesson, with four programs that you really run.
5. **Java compared with what you already know** (0.5) and **what comes in the rest of the course** (0.6).

With that destination in mind, this is what you will see in this lesson, in section 0.4.1, when you run the first program. It is a message from the compiler, not from the run, and the difference between the two is the central idea:

```text
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
```

## The concepts

### 0.1 The path of the code: source, *bytecode* and the JVM

#### 0.1.1 The three things that exist

When you write a program in Java, there are three distinct objects, and it pays to name them right away:

- The **source code**: the text file you write, with the extension `.java`. A person reads it.
- The ***bytecode***: the result of compiling the source code, saved in a file with the extension `.class`. It is a compact binary format that is not meant for a person to read and is not the language of any real processor. It is the language of an **imaginary** processor.
- The **JVM** (*Java Virtual Machine*): the program that runs the *bytecode*. It is the imaginary processor made real, a piece of software that exists for every operating system and every type of processor.

The `javac` program is the compiler: it converts the source code into *bytecode*. The `java` program is the launcher: it starts a JVM and asks it to run the *bytecode* of a class. The word “class” is going to matter, so here is a provisional definition: in Java, each `.class` contains **one** unit of code called a class (later there will be other variants, such as the `record`s of Lesson 3, which also end up in a `.class`). That is why people say “the class file”.

The complete picture of the path is this:

```text
Hola.java  ──javac──►  Hola.class  ──java──►  [ JVM ]  ──►  lo que el programa hace
(código fuente)        (bytecode)             (ejecuta)
```

Why two steps and not one? Because each step solves a different problem and solves it **at a different time**. The compiler works once, on your development machine, and it is the one that checks that what you wrote makes sense (for example, that you do not try to put a text where a number goes, as you will see in 0.4). The JVM works every time the program runs, on the machine where it runs, and it is the one that makes it work with that operating system and that processor. The *bytecode* is the contract between the two halves: the compiler promises to produce valid *bytecode*, and the JVM promises to run it.

#### 0.1.2 A `.class` on the inside

All this sounds abstract until you see it. In Lesson 1 you will compile your first program and see the `.class` appear in your folder. In the meantime, look at what a small one contains. This fragment is not a program from the lesson: it is a minimal class, `Suma` (“sum”), with a method that adds two integers, compiled with `javac Suma.java`. Then the JDK's disassembler, `javap`, was asked to show the *bytecode* of that method:

```text
$ javap -c Suma
Compiled from "Suma.java"
class Suma {
  Suma();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  static int sumar(int, int);
    Code:
       0: iload_0
       1: iload_1
       2: iadd
       3: ireturn
}
```

You do not need to understand every line, but look at the `sumar` (“add”) method, the one at the bottom. Its instructions are `iload_0`, `iload_1`, `iadd` and `ireturn`: “load the first integer argument”, “load the second”, “add two integers”, “return an integer”. They are the instructions of a simple processor, of the kind programmed with a stack: two numbers are pushed onto the stack, they are added, the result stays on top. That processor does not exist in any real computer. It exists only inside the JVM. The instructions and the file format are defined by a public specification, the [Java Virtual Machine Specification](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html), which every JVM complies with.

Two more facts about the file, measured with `xxd` and with `javap -v`. The first eight bytes of a Java 25 `.class` are `cafe babe 0000 0045`. The first four, `cafebabe`, are a magic number that identifies any class file, of any version: the specification fixes it that way. The last two bytes (`0045` in hexadecimal) are the **major version** of the format, 69 in decimal, which is the one that corresponds to Java 25. Each new version of Java raises that number, and an old JVM refuses to run a `.class` with a number higher than the one it understands. When you later see a message that talks about “class file version”, this is it.

#### 0.1.3 What the JVM does with the *bytecode*

The JVM does not just “interpret” the *bytecode* instruction by instruction. It does three things, and it is worth knowing which, even though you will not program any of them:

1. **It loads and verifies.** It reads the `.class`, checks that the *bytecode* is coherent (that it does not try to pop from the stack what it never pushed, for example) and only then lets it run. It is a safety net against corrupt or badly made files.
2. **It runs, and while it runs, it observes.** It starts by interpreting the *bytecode*. As the program runs, it detects the methods that execute many times.
3. **It compiles the hot methods to real machine code**, on the fly, while the program keeps working. That piece is called the JIT compiler (*just in time*). It is the reason a Java program, after a few moments of warm-up, can run at speeds comparable to those of a program compiled from the start.

In addition, the JVM manages memory: it reserves what the program asks for and frees it by itself when nobody uses it anymore, with a piece called the garbage collector. You already know the idea if you come from Go, Python or TypeScript. The [Java 25 JVM guide](https://docs.oracle.com/en/java/javase/25/vm/) and the [garbage collector guide](https://docs.oracle.com/en/java/javase/25/gctuning/) give the details; for this course it is enough to know that they exist and that you will not free memory by hand.

The practical consequence of all this is a famous promise of Java: **“write once, run anywhere”**. The same `.class` runs on Linux, on macOS and on Windows, because each system has its own JVM that knows how to run it. The promise is real, but it has edges, and it is worth knowing them from the start so you do not believe it too much. Two measured examples: the form of paths, the text encoding and line breaks change from one system to another, and code that assumes them breaks even though the `.class` is the same. And the **text output** depends on the system's configuration: with the minimal locale (`LC_ALL=C`), a program that prints `sin conexión` (“no connection”) wrote `sin conexi?n`, because the system told Java its output was ASCII; with `LC_ALL=C.UTF-8` it printed the correct text. Since Java 18, the text of files is read and written in UTF-8 by default ([JEP 400](https://openjdk.org/jeps/400)), but standard output still follows the system's configuration. Portable does not mean identical.

#### 0.1.4 And `java Hola.java`, which of the two steps is it?

Later you will see that a program can be run with a single command, `java Hola.java`, without a visible `javac`. It does not contradict anything above. That mode, called the source-file launcher ([JEP 330](https://openjdk.org/jeps/330)), compiles the file **in memory**, without leaving a `.class` on disk, and runs it immediately. Both steps happen; it is just that one of them leaves no trace. It is convenient for learning and for one-file programs; for real projects you compile separately, and that is what you do from Lesson 6 on.

### 0.2 The pieces of the platform: JDK, JRE and distributions

#### 0.2.1 JDK and JRE

There are two acronyms you will see in every tutorial, and their meaning has changed over the years:

- The **JRE** (*Java Runtime Environment*) is what is needed to **run** Java programs: the JVM and the standard libraries. With a JRE you can run a `.class` or a JAR, but you cannot compile.
- The **JDK** (*Java Development Kit*) is the JRE **plus** the tools for developing: the `javac` compiler, the `javap` disassembler, the `jshell` interactive interpreter that you will use in Lesson 1, the `jar` packager and others. The `bin` folder of a Temurin 25.0.4 has 30 programs (counted with `ls | wc -l` and counted again one by one in the list: `jar`, `jarsigner`, `java`, `javac`, `javadoc`, `javap`, `jcmd`, `jconsole`, `jdb`, `jdeprscan`, `jdeps`, `jfr`, `jhsdb`, `jimage`, `jinfo`, `jlink`, `jmap`, `jmod`, `jnativescan`, `jpackage`, `jps`, `jrunscript`, `jshell`, `jstack`, `jstat`, `jstatd`, `jwebserver`, `keytool`, `rmiregistry`, `serialver`).

This course needs to compile, so install **the JDK**. The historical confusion is that for years people installed “Java” and got the JRE (which was enough to run other people's programs, such as an installer or a desktop application), and to program they had to go looking for “the JDK”. Today that distinction hardly matters on your development computer, because you install the JDK and that is it; but it still matters on a server or in a container image, where it pays to install only what is needed to run. Some distributions still publish run-only packages; for example, Temurin's installation page for Linux offers them as `temurin-<version>-jre`.

And a third acronym that is best not to mix up: the **JVM** is only the virtual machine, a component that lives inside both of them. “Installing the JVM” is not something you do separately.

#### 0.2.2 One specification, one project and many distributions

The part that most bewilders someone arriving from other languages: **there is no single “Java” downloaded from a single place**. There are three layers.

1. **The specification.** The language and the JVM are defined by public documents: the [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html) (JLS) and the JVM one (JVMS) you saw in 0.1. Anyone can implement them.
2. **OpenJDK.** It is the open-source project where the reference implementation is developed. Each new version has its page at [openjdk.org](https://openjdk.org/projects/jdk/25/) with the features it brings and its schedule. OpenJDK publishes source code; it is not what you install.
3. **The distributions.** These are the binaries that someone builds from OpenJDK, tests and publishes with their own update policy: Eclipse Temurin (from the Eclipse Foundation, through the Adoptium project), Oracle's, Azul's, Amazon's, the ones Ubuntu includes in its repository, and others. They all run the same language; they differ in who builds them, how long they maintain them and under what license.

This course uses **Eclipse Temurin**, the free distribution of the Adoptium project, of the Eclipse Foundation, which has no commercial owner and publishes a clear update schedule: its [support page](https://adoptium.net/support/) says how long each version receives fixes, and the table in the next section comes from there. All the course's outputs were measured with Temurin 25. Lesson 1 installs it with **SDKMAN**, a version manager that works in your home folder, without `sudo`, and which in Lesson 6 also installs Maven. And it explains in passing why `default-jdk`, the package Linux Mint suggests to you, is **not** used. A preview of the reason: on Mint 22 that package installs Java 21, not Java 25.

### 0.3 The versions: rhythm, LTS and preview

#### 0.3.1 A new version every six months

Java stopped releasing a big version every several years and adopted a fixed rhythm: **a new version every six months**, in March and in September. The Temurin support page says so: “OpenJDK provide a new feature release every six months”. Each one brings few features, designed so that the change is small.

The consequence is that there are many versions, and most of them **stop receiving updates after six months**. That would be unmanageable for anyone maintaining a program in production. That is why the concept of LTS exists.

#### 0.3.2 LTS: the version that is maintained for years

LTS stands for *Long-Term Support*. An LTS version is one that vendors commit to maintaining, with security fixes, for years. The Temurin support page describes it like this: every two years, since 2021, a version is designated LTS, and Temurin commits to maintaining it for at least four years. The figures published there (consulted on October 7, 2026):

| Version | Released | Type | Temurin maintains it |
|---|---|---|---|
| Java 17 | September 2021 | LTS | at least until October 2027 |
| Java 21 | September 2023 | LTS | at least until December 2029 |
| **Java 25** | **September 2025** | **LTS** | **at least until September 2031** |
| Java 26 | March 2026 | not LTS | ended in September 2026 |
| Java 27 | September 15, 2026 | not LTS | until March 2027 |

The table answers the question “why Java 25 if there is already a 27?”. Because 27 **is not LTS**: in six months it will stop receiving fixes and you will have to jump to another. A course that is published today and read for years should not teach on a foundation that expires in half a year. 25 is the current LTS, and it is the one any serious team will have in production in the coming years. If the two-year rhythm holds, the next LTS will be 29, in September 2027; that is an extrapolation of that rhythm, not anyone's promise.

Two clarifications that avoid confusion. First, “LTS” does not mean “no changes”: 25 receives security and bug fixes, but not new features. Second, the patch numbers —what comes after `25.` in what `java --version` prints, which starts with 25— change every three months; that is why the course says “Java 25” and never a patch number.

#### 0.3.3 Preview: what exists but is not yet final

Java brings in new features through an intermediate stage: **preview**, described in [JEP 12](https://openjdk.org/jeps/12). A preview feature is already specified and programmed, but the Java team reserves the right to **change or remove it** in the next version, depending on what it learns from those who try it. To keep anyone from using it by accident, the compiler rejects it unless it is expressly asked for with the `--enable-preview` flag.

Preview has a concrete consequence that is worth seeing with a real case, that of **structured concurrency**, which in Java 25 is still in preview ([JEP 505](https://openjdk.org/jeps/505)). This program uses it:

```java
import java.util.concurrent.StructuredTaskScope;

void main() throws Exception {
  try (var alcance = StructuredTaskScope.open()) {
    var tarea = alcance.fork(() -> "hola");
    alcance.join();
    IO.println(tarea.get());
  }
}
```

Without the flag, the compiler refuses. This is what it prints, verbatim (it is a fragment, not a figure of the course, because it deliberately does not compile):

```text
$ java Preview.java
Preview.java:1: error: StructuredTaskScope is a preview API and is disabled by default.
import java.util.concurrent.StructuredTaskScope;
                           ^
  (use --enable-preview to enable preview APIs)
Preview.java:4: error: StructuredTaskScope is a preview API and is disabled by default.
  try (var alcance = StructuredTaskScope.open()) {
                     ^
  (use --enable-preview to enable preview APIs)
2 errors
error: compilation failed
```

The message is clear: “it is a preview API and is disabled by default”. With `java --enable-preview --source 25 Preview.java` the same program prints `hola` (“hello”). But there is a trap that the message does not show: a `.class` compiled with preview features carries the “preview” mark in its header, measured with `javap -v`: `minor version: 65535`, instead of the usual `0`. That mark means **only the exact version of Java it was compiled with can run it**. A program built on a preview feature of 25 can stop working when moving to 26. That is why the course sets a rule that is not up for discussion: **nothing in preview is used in the course's code**. Structured concurrency appears in Lesson 9 as a “what is coming” box, with its flag and with the warning not to use it in production; the rest, only what is already final in 25.

### 0.4 What the compiler protects and what it does not

This is the most important section of the lesson, and the one that most resembles a minefield map. Java prides itself on being a statically typed language, that is, one where the compiler checks types **before** the program runs. It is true, and it is an enormous advantage. But “static types” does not mean “no runtime errors”, and there are four holes worth knowing from day one. Each one comes with a program to run after Lesson 1.

#### 0.4.1 What the compiler does catch: types

Let us start with what it does protect. The program `fig00_01.java` declares a variable that must contain an integer and assigns it a text:

```java
// fig00_01.java
void main() {
  int tiempoMs = "142";
  IO.println(tiempoMs);
}
```

This program **does not run**: the compiler rejects it beforehand. Run it with `java fig00_01.java` and you get:

```text
$ java fig00_01.java
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
1 error
error: compilation failed
```

Read it from top to bottom, which is how you will read it hundreds of times. The first line carries three pieces of data: the file (`fig00_01.java`), the line number (`3`) and the diagnosis (`incompatible types: String cannot be converted to int`). Below, the compiler copies the offending line and puts an arrow `^` at the point where it noticed. At the end, `1 error` counts the problems and `error: compilation failed` is the launcher saying it never got to run anything. Notice also what did not happen: the program printed nothing, not even a partial result. A compile error stops everything **before** the first instruction.

The program's lines, so they are not left a mystery: `void main()` is the way to start a program (Lesson 1 explains it in depth), `int tiempoMs` declares an integer variable (`tiempoMs` is “time in ms”), and `IO.println` prints a line. What matters is the comparison with your other languages. In Python, the equivalent (`tiempo_ms: int = "142"`) runs without complaint, because annotations are not checked at run time. In TypeScript, the compiler catches it just like Java. In Go, too. Java is in the group that catches it before running.

#### 0.4.2 First hole: generics are erased

**Generics** are Java's syntax for saying “a list *of texts*”, “a list *of numbers*”: `ArrayList<String>`, `ArrayList<Integer>`. (The topic belongs to [Lesson 4](04-colecciones-genericos-streams.md); here only one consequence matters.) The compiler uses that information to check your code. But after compiling, **the information disappears**: the compiler erases it and leaves the *bytecode* without it. This is called **type erasure**.

`fig00_02.java` shows it, creating a list of texts and a list of numbers and asking whether they are of the **same class**:

```java
// fig00_02.java
import java.util.ArrayList;

void main() {
  var palabras = new ArrayList<String>();
  var numeros = new ArrayList<Integer>();
  IO.println(palabras.getClass() == numeros.getClass());
  IO.println(palabras.getClass().getName());
}
```

```text
$ java fig00_02.java
true
java.util.ArrayList
```

The program, line by line. The first line, `import`, tells the compiler where to find `ArrayList`: it is in the `java.util` package of the standard libraries. `var` asks the compiler to deduce the type of the variable from the value assigned to it (Lesson 2 explains it). The two `new ArrayList<...>()` create an empty list, one announced as being of texts and the other of numbers. `getClass()` returns the class of an object **at run time**, and `==` compares whether they are the same. The answer is `true`: for the JVM, both lists are a `java.util.ArrayList`, nothing more. The `<String>` and the `<Integer>` no longer exist when the program runs.

Why was it designed this way? For compatibility. Generics arrived in Java in version 5, and by then millions of lines of code and libraries existed without them. Erasing the information let old and new code coexist. The specification itself acknowledges that inheritance when it talks about “raw types”, which are allowed only “as a concession to compatibility of legacy code” ([JLS, section 4.8](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.8)). A side benefit, which the [official tutorial](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html) points out, is that generics cost nothing at run time: no new classes are created for each combination.

The consequence for you is that some things you would expect from a language with generic types **cannot be done** in Java: asking at run time “is this a list of texts?” has no answer, because that information does not exist. When you reach Lesson 4 and find odd restrictions with generics, the cause is almost always this erasure. Compare it with TypeScript, where **all** types are erased when generating JavaScript. In Java only the generics' arguments are erased; the object's class does survive, and that is why the program could print `java.util.ArrayList`.

#### 0.4.3 Second hole: `null` still exists

Java has types, but it does not prevent a variable of an object type, such as `String`, from being worth **“nothing”**. That “nothing” is written `null`. The compiler accepts that you assign it and does not warn you when you use it badly. `fig00_03.java` declares a service detail, leaves it as `null` and asks for its length:

```java
// fig00_03.java
void main() {
  String detalle = null;
  IO.println("antes de usar el detalle");
  IO.println(detalle.length());
  IO.println("esto no se imprime");
}
```

```text
$ java fig00_03.java
antes de usar el detalle
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "<local1>" is null
	at fig00_03.main(fig00_03.java:5)
```

This program **compiles without problems**. The error appears only when the fifth line is executed, and that is why the first `IO.println` did get to print. It is the difference between a compile error (0.4.1: nothing runs) and a run-time error: the program runs until it hits the problem, and stops there. The name of the error, `NullPointerException`, means “exception of a null pointer”; an **exception** is Java's way of reporting that something went wrong during execution ([Lesson 5](05-errores-excepciones.md) studies it in full). Its message says what you tried to do (`Cannot invoke "String.length()"`) and why it failed (`because "<local1>" is null`). Since Java 15, the JVM gives detailed messages like this by default, thanks to [JEP 358](https://openjdk.org/jeps/358).

You may be surprised that the variable is called `<local1>` instead of `detalle` (“detail”). It is not a mistake in the lesson: when you run a file directly with `java Archivo.java`, the internal compiler does not keep the names of local variables. [Lesson 2](02-tipos-cadenas-control.md) shows how to recover them with another way of compiling. What matters today is the idea: **the Java compiler does not protect you from `null`**.

The invention of `null` was lamented by its own author, Tony Hoare, who in 2009 called it “my billion-dollar mistake” ([talk](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/)), because of the failures it caused for decades. Java did not remove it, but over the years it offers tools so you do not depend on it: in Lesson 3 sealed types replace the `revisor`'s `null`s with a type that says explicitly “there was a response” or “there was not”, and in Lesson 4 `Optional` covers the “sometimes there is data” cases.

#### 0.4.4 Third hole: `+` does not always add

The third is a trap for someone coming from another language. `fig00_04.java` uses the `+` operator in three ways:

```java
// fig00_04.java
void main() {
  String tiempo = "142";
  IO.println(tiempo + 1);
  IO.println(1 + 42 + "ms");
  IO.println("ms" + 1 + 42);
}
```

```text
$ java fig00_04.java
1421
43ms
ms142
```

All three results come from a single rule: **if either side of the `+` is a text, the `+` concatenates; if both are numbers, it adds**; and it is evaluated from left to right. The first line joins the text `"142"` with the number `1` and gives `"1421"`, a four-character text, not the number 143. The second adds `1 + 42` first (both are numbers) and gives `43`, and only then joins it with `"ms"`. The third starts with a text, so `"ms" + 1` is already concatenation and gives `"ms1"`, and then `"ms1" + 42` gives `"ms142"`. The rule is in the [language specification](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.18.1).

What makes this case dangerous is not that it is illegal, but that it **compiles without a single warning** and gives a plausible result. If in the `revisor` a time arrives as text, `"142" + 1` will not warn you of anything: it will produce `"1421"`, and that false datum will travel all the way to the report. In Python, `"142" + 1` is a `TypeError` on the spot; in TypeScript, it gives `"1421"` just like Java. Java is on TypeScript's side on this point, and that is why Lesson 2 insists that each datum in the `revisor` have the right type from the moment it enters.

#### 0.4.5 Fourth hole: what the compiler will never see

The three earlier ones can be provoked with programs of a few lines. There is a fourth, broader group: **values that are only known at run time**. The compiler checks types; it does not check values. These fragments were run and this is what they print:

```java
void main() {
  int maximo = Integer.MAX_VALUE;
  IO.println(maximo + 1);
}
```

```text
$ java Desborde.java
-2147483648
```

A Java integer has a fixed size (four bytes), and if a sum exceeds the maximum, the result **wraps around** to the minimum, with no error. The specification says it head on: “the integer operators do not indicate overflow or underflow in any way” ([JLS, section 4.2.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2.2)). Two other cases, measured the same way, each in its own file with a `void main()` that contains only the indicated line:

```java
IO.println(1 / 0);
```

```text
$ java Division.java
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at Division.main(Division.java:2)
```

```java
IO.println("hola".charAt(10));
```

```text
$ java Caracter.java
Exception in thread "main" java.lang.StringIndexOutOfBoundsException: Index 10 out of bounds for length 4
	at java.base/jdk.internal.util.Preconditions$1.apply(Preconditions.java:55)
	...
	at Caracter.main(Caracter.java:2)
```

The ellipsis stands in for nine internal JDK lines, whose numbers change with every update. Dividing an integer by zero is a run-time exception; asking for character number 10 of a four-character text is too. In neither case could the compiler know without running the program. The summary of the section fits in a table:

| What | When is it detected? | Example from this lesson |
|---|---|---|
| A text where a number goes | At compile time | `int tiempoMs = "142";` (fig00_01) |
| A `null` used as if it were an object | At run time | `detalle.length()` (fig00_03) |
| A `+` that concatenates instead of adding | Never: the result is valid | `"142" + 1` (fig00_04) |
| The type of a list's elements | At compile time, and then it is erased | `ArrayList<String>` (fig00_02) |
| An integer that exceeds its maximum | Never: it wraps around | `Integer.MAX_VALUE + 1` |
| Integer division by zero | At run time | `1 / 0` |

The rest of the course is about making that middle column as early as possible: moving errors from “never” or “at run time” to “at compile time”. The sealed types of Lesson 3 make forgetting a case a compiler error. The immutable collections of Lesson 4 prevent accidental changes. The tests of Lesson 7 catch what the compiler cannot. It is no accident: it is the method.

### 0.5 Java compared with what you already know

With the map above, the comparison with the course's other languages stops being vague. This is what changes, and what does not:

| | Go | TypeScript | Python | **Java** |
|---|---|---|---|---|
| What does the compiler produce? | an executable for one system and one processor | JavaScript (the types disappear) | interpreter *bytecode*, automatically | JVM *bytecode*, in an explicit step |
| When are types checked? | at compile time | at compile time (with `tsc`) | not checked at run time | at compile time |
| What runs the program? | the processor directly | Node or a browser | the Python interpreter | the JVM |
| Is there a garbage collector? | yes | yes (the JavaScript engine) | yes | yes |
| How is it delivered? | copy the executable | copy the JavaScript and Node | copy the code and the interpreter | copy a JAR (a file with `.class` files inside) and have a JVM |

Two things stand out. The first: if you come from Python, the “source code, *bytecode*, virtual machine” model is **one you already know**; Python also compiles to *bytecode* and runs it on a virtual machine. What changes in Java is that the compile step is explicit and visible, and that in the middle it checks types. The second: if you come from Go, the difference is the JVM as an intermediary. A Go executable carries everything inside; a Java program needs a JVM installed where it runs, in exchange for being portable across systems with the same file.

There is also a way to produce a native executable from a Java program (ahead-of-time compilation with GraalVM). It is not used in this course. The Hábil blog explains [when it makes sense and when it does not to compile Java to native](https://www.habil.mx/en/blog/native-java-when-to-use-it-when-not/); read it when you finish Lesson 10, not before.

### 0.6 What comes in the rest of the course

All of the above is put into practice in the following lessons. The course builds a single program, the `revisor`, and each lesson adds a piece:

| Lesson | What is added | Which hole from this lesson it helps close |
|---|---|---|
| 1 | You install Java 25 and write and compile your first program | the path of 0.1, in your terminal |
| 2 | Types, strings and control flow | `null`, `==` versus `equals`, the `+` that concatenates |
| 3 | `record`, `enum` and sealed types | `null` replaced by types that state all their cases |
| 4 | Collections, generics and *streams* | type erasure, lists that do not change |
| 5 | Errors and exceptions | run-time errors, handled instead of tolerated |
| 6 | A Maven project (the tool that builds Java projects) | the compiled output as a product: packages, a `pom.xml` and a `.jar` |
| 7 | Tests with JUnit | what the compiler does not see, caught by tests |
| 8 | HTTP and JSON | data that arrives from outside and has to be validated |
| 9 | Concurrency | checking all the services at once |
| 10 | The finished program | command line, server, quality and a JAR |

There is one more practical agreement. The programs of Lessons 1 to 5 are run with `java Archivo.java`, with no additional tools. From Lesson 6 on, the project lives in a standard structure, with Maven. The course uses **two-space indentation** in all the code, the one from Google's Java style guide, because in Lesson 10 a tool checks that style and it would be absurd to confess then that everything earlier was written another way. Much code on the internet uses four spaces; it works the same.

## The error you will see

This lesson has one main error, the compiler that refuses, and one run-time error worth recognizing at a glance. Both are from the earlier sections.

**The main error: `incompatible types`.** It is the one from `fig00_01.java`:

```text
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
```

It reads like this: the file and the line (`fig00_01.java:3`), what happened (a `String` cannot be converted to `int`), the offending line, and the arrow at the exact point. It is fixed in two ways, depending on what you wanted: if the datum was a text, the variable should have been of type `String`; if you wanted the number, you have to convert the text explicitly (Lesson 2 deals with that). What you do **not** do is ignore it: a compile error is never “skipped”, because the program simply does not run.

**A run-time error you can already recognize: `NullPointerException`.**

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "<local1>" is null
```

It reads from left to right: in the main thread (`main`) a `NullPointerException` occurred; you tried to invoke `String.length()` and it could not be done because what was in the variable was `null`. Below, the line that starts with `at` says **where**: `fig00_03.main(fig00_03.java:5)`, that is, the file's `main` method, line 5. Check that line and ask yourself where a `null` could have come from.

## What gets done wrong

**Installing “Java” without knowing what is being installed.** Whoever types `sudo apt install default-jre` gets only the run-time part, and on Mint 22 of a version older than the one the course uses. To program, the JDK; for the course, the one for Java 25.

**Using the latest version just because it is the latest.** Java 27 came out on September 15, 2026 and is not LTS. To learn, for a server or for a program that is going to live for years, the current LTS. If you already have 27 installed, nothing serious happens with this course, but Lesson 1 tells you how to make sure which version you are using.

**Believing that “it has types” means “it does not fail at run time”.** Types catch one family of errors, not all of them. Those in 0.4 exist in any Java program, made by anyone.

**Copying a tutorial without looking at its date.** On the internet there are circulating versions of “Hello world” from twenty years ago that make you write a class and a `public static void main(String[] args)` method. They work, but for a single-program file none of that is needed anymore, as you will see in Lesson 1. The same goes for libraries: almost everything published about JSON or tests is written for versions earlier than the course's. When a recipe does not compile, the first thing to check is the version.

**Using a preview feature “because it works”.** It works today, with the flag, on that exact version. According to JEP 12, it can change or disappear in the next one. Not in code that someone else is going to maintain.

**Confusing Java with JavaScript.** They share letters of the name and half the curly braces, and nothing else. They are different languages, with different execution models and a historical relationship that nobody needs to know in order to use either of them.

**Expecting “write once, run anywhere” to include the environment.** The `.class` is portable; the output encoding, the paths and the line breaks are not by themselves. It is measured, not assumed.

## Exercises

The exercises in this lesson do not require Java: the first is for reading, the second is answered **before** running anything and checked after Lesson 1, and the third and fourth are answered in writing, in your own words. The best way to take advantage of the second is to write your prediction in the [logbook](bitacora.md) and compare it afterward.

### Exercise 1 — The `revisor`'s data (project)

This is the table the `revisor` will produce at the end of the course. For each column, indicate whether the datum is a **number**, a **text** or can be **nothing**. Pay special attention to the time of `reportes`.

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Then answer: what would Java do with `"142" + 1`, if a time arrived as text? And what problem would there be if a program stored the time of `reportes` as `null` and then tried to format it?

### Exercise 2 — When does the program find out?

For each of these six fragments, predict whether (a) the compiler rejects it, (b) the program compiles and fails when run, or (c) it compiles, runs and gives a result without warning. If it is (b), say which exception you expect.

1. `int tiempoMs = "142";`
2. `String detalle = null;` followed by `detalle.length()`.
3. `Integer.MAX_VALUE + 1`
4. `var palabras = new java.util.ArrayList<String>();` followed by `palabras.add(5);`
5. `1 / 0` (between integers).
6. `"hola".charAt(10)`

### Exercise 3 — Choose the version

Decide which version of Java you would use in each case and why: (a) a new program that your team is going to maintain for several years; (b) you have Java 27 installed and want to follow this course; (c) an article uses `StructuredTaskScope` and you want to try it on your machine.

### Exercise 4 — Explain it in five sentences

A colleague who programs in Go asks you what happens between “I wrote a `.java`” and “the program runs”. Answer in five sentences at most, using the words *source code*, `javac`, *bytecode*, JVM and JIT compiler. Without looking at the lesson.

## Solutions

### Solution 1

The columns, in order:

- **SERVICIO** (“service”) is **text** (`catalogo`, `pagos`...). **ESTADO** (“status”) is **text** that can only be one of three things (`ARRIBA`, `LENTO`, `CAIDO`: “up”, “slow”, “down”); in Lesson 3 it becomes a type of its own that allows no others.
- **TIEMPO** (“time”) is a **number** (142, 2300, 87) that is shown with a unit (`142ms`, `2.3s`) and **can be nothing**: `reportes` did not respond and has no time. That dash `—` represents a datum that does not exist.
- **DETALLE** (“detail”) is **text** even though it looks like a number: the `200` is the status code, but in the table it is shown as text, just like `sin conexión` (“no connection”).

With `"142" + 1`, Java **does not add**: it concatenates and gives the text `"1421"` (fig00_04). Since it compiles without warning, a false datum would enter the report without anyone finding out. And with the time of `reportes` stored as `null`, formatting it with something like `tiempo.length()` throws a `NullPointerException` at run time (fig00_03), because the compiler does not protect against that. Both situations are the reason Lessons 2 and 3 exist: in 2 you learn to write the code carefully around `null`, and in 3 the design is changed so that “there was no response” is an explicit case of the type and not a hidden `null`.

### Solution 2

After Lesson 1, you can check them with `jshell` or with a program file. The answers, measured with Temurin 25.0.4:

1. **(a)** The compiler rejects it: `incompatible types: String cannot be converted to int` (fig00_01).
2. **(b)** It compiles and fails at run time with `java.lang.NullPointerException` (fig00_03).
3. **(c)** It compiles, runs and gives `-2147483648`, without warning: the integer wraps around.
4. **(a)** The compiler rejects it. This is the value of generics: the list is of texts and `5` is not one. The message starts with `error: no suitable method found for add(int)` and continues with a list of the variants of `add` it tried, each with its reason (the one that matters says `argument mismatch; int cannot be converted to String`). It is an example that the compiler does use generics, even though it later erases them.
5. **(b)** It compiles and fails at run time with `java.lang.ArithmeticException: / by zero`.
6. **(b)** It compiles and fails at run time with `java.lang.StringIndexOutOfBoundsException: Index 10 out of bounds for length 4`.

If you got 3 wrong, that is normal: it is the only one where the program “works” and gives a wrong number, and that is why it is the most dangerous of the six.

### Solution 3

(a) **Java 25**, the current LTS: it will have security fixes at least until September 2031. A non-LTS version would stop being maintained after six months.

(b) **There is no need to uninstall anything, but it is not advisable to use it for the course either.** Java 27 is not LTS and the course verifies its programs with 25; the safe thing is to install Temurin 25 (with SDKMAN, Lesson 1, the two coexist and you choose which to use) and check with `java --version` which one is active. What you must not do is assume that “the newest works for everything”.

(c) **On version 25, with `--enable-preview`**, knowing that it is a preview feature: the API can change in the next version (in fact it changed between 25 and 26, according to [JEP 525](https://openjdk.org/jeps/525)), the resulting `.class` only runs on that exact version, and it must not be used in a program that someone else is going to maintain.

### Solution 4

One possible answer: “You write the *source code* in a `.java`. The `javac` compiler checks it (mostly the types) and translates it to *bytecode*, which goes in a `.class`. That *bytecode* is not the code of a real processor: it is run by the JVM, which exists for each operating system. While the program runs, the JVM detects the methods that are used a lot and converts them to machine code with the JIT compiler. That is why the same `.class` runs on any system that has a JVM, unlike a Go executable, which already carries the code of its processor.”

## How I know I got it

- [ ] You draw from memory the path `.java` → `javac` → `.class` → `java` → JVM, and say what each arrow does.
- [ ] You state the difference between JDK, JRE and JVM, and which one is installed for this course.
- [ ] You explain why there is a Java 27 and the course uses 25, with the word LTS.
- [ ] You know what a preview feature is and what flag it requires.
- [ ] For each row of the table in 0.4.5, you say when the problem is detected.
- [ ] After Lesson 1, you run `java fig00_01.java`, `fig00_02.java`, `fig00_03.java` and `fig00_04.java` and your outputs match those in this lesson, except for the path.
- [ ] You predict in the logbook what `"ms" + 1 + 42` prints before running it, and you check.
- [ ] You wrote down in your logbook one thing you believed about the JVM that turned out different.

## Summary

Answer without looking at the lesson:

1. What does a `.class` contain and who runs it?
2. What does the JDK add to the JRE? Why is the JDK installed in this course?
3. What is OpenJDK and what is a distribution like Temurin?
4. What does LTS mean? Why is Java 27 not the course's version?
5. What happens with a `.class` compiled with preview features?
6. How do a compile error and a run-time error differ? Give an example of each.
7. What is type erasure and what does `fig00_02.java` prove?
8. Why is `"142" + 1` equal to `"1421"` and not `143`?
9. Why does the Java compiler not warn you about a misused `null`?

## Further reading

- [JEP 512, “Compact Source Files and Instance Main Methods”](https://openjdk.org/jeps/512): the proposal that makes it possible in Java 25 to write a program without a class or `public static`, which is the style of this course. It reads better after Lesson 1. Consulted on October 7, 2026.
- [JEP 12, “Preview Features”](https://openjdk.org/jeps/12): what preview is, why it exists and what guarantees it does not give. Consulted on October 7, 2026.
- [Java Virtual Machine Specification, SE 25, chapter 4](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html): the exact format of the `.class` file, including the magic number and the version. Consulted on October 7, 2026.
- [Temurin Support, Adoptium](https://adoptium.net/support/): the release calendar and the end of support for each one, where the table in 0.3 comes from. Consulted on October 7, 2026.

---

**Next:** [Lesson 1 — Installing Java 25 on your Linux Mint](01-instalacion.md)
