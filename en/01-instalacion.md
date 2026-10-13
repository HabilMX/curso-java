# Lesson 1 — Installing Java 25 on your Linux Mint

**Time:** 90 min (tight: the installation depends on your connection, and if something fails in it, the rest of the lesson gets cut short; do not rush it)

**What you build:** the working environment (a working JDK 25) and your first program; you end up with the folder `~/curso-java/` containing the file `Hola.java` and the project's first file, `revisor/Revisor.java`

**What you learn:** installing SDKMAN and, with it, Temurin 25, and checking the version in a new terminal with `java --version` and `javac --version`; why `default-jdk` is not used; Java 25's compact source file (`void main()` and `IO.println`); `java Hola.java`; compiling with `javac` and running with `java` separately, and what the `.class` is; trying out ideas with `jshell`; and reading the first compiler error

**Where you are coming from.** From [Lesson 0](00-que-es-java.md), where you saw the path of the code: source, `javac`, *bytecode* and the JVM; the difference between JDK and JRE; and what LTS is. This lesson takes that theory and turns it into commands. You work on a Linux Mint 22 (any 22.x: 22.3, “Zena”, is the current one according to the [Linux Mint releases page](https://linuxmint.com/download_all.php), supported until April 2029), which is based on Ubuntu “Noble”.

**What this lesson does not do.** It does not teach Java syntax beyond what is needed for the first program: types, strings and control flow belong to [Lesson 2](02-tipos-cadenas-control.md). It does not install Maven ([Lesson 6](06-proyecto-maven.md) does that when it is needed). And it does not use a graphical development environment: everything is done with a text editor and a terminal, because that way you can see what each piece does, and because that is what you will have on a server.

**About the runs and what we could not verify.** The programs in this lesson and their error messages were run with Temurin 25.0.4 on macOS and compared character by character with what is shown here; the ones that show Java 21 and Java 17 were obtained with Temurin 21.0.12 and 17.0.20. On your Linux Mint they should give the same, because they are compiler and JVM messages and not the system's, but those programs were not run on Mint. **What does depend on your Linux Mint** is the installation, and that was measured on October 13, 2026 on a real Linux Mint 22.3: what the terminal answers when there is no Java, what SDKMAN's installer prints, which Temurin identifiers it offered that day, what `java --version` says afterward, and the trap of the terminal that does not notice the change. If something on your screen differs from what is shown here on those points, write it down in your [logbook](bitacora.md): it is useful information. The programs are in [`programas/01-instalacion/`](../programas/01-instalacion), in the course repository.

## By the end you will be able to

- Install SDKMAN and, with it, Temurin 25, choosing the right identifier from `sdk list java`, and check in a new terminal, with `java --version` and `javac --version`, that they are version 25.
- Explain why `sudo apt install default-jdk` gives you Java 21 on Mint 22, and what error that produces with the compact source file.
- Write, run and modify a single-file Java program with `void main()` and `IO.println`, and run it with `java Hola.java`.
- Compile with `javac`, run with `java`, and say what the `.class` file that appeared is and when you have to compile again.
- Try out loose expressions in `jshell` and exit it.
- Read a compiler error: the file, the line, the `^` arrow, and the `symbol` and `location` lines.
- Create the project's first file, `~/curso-java/revisor/Revisor.java`, and run it.

## The why before the how

There is a reason this lesson comes so early in the course: **an environment that does not work makes everything that follows impossible**, and many of the problems people have with Java in their first days are installation and version problems, not programming problems: a different version was installed from the one the tutorial assumes, or more than one is installed and the wrong one is used. That is why the first step of the lesson is not writing code but **finding out what you have**, and the last of the installation part is **checking which version you are using**. Everything else, the `Hola.java` file, `javac`, `jshell`, is built on the certainty that the `java` command in your terminal is Java 25's.

There is also a morale reason: whoever arrives at this course already programs, and the first thing they need is a quick success. The program you will write in section 1.2 has three lines, and you run it with a single command. Before Java 25, that same program required declaring a class and a `public static void main(String[] args)` method around it; since Java 25 it is what you see below. It is the best news in Java for someone coming from another language, and that is why the course takes advantage of it.

The lesson has seven parts: **installing the JDK** (1.1), **your first program** (1.2), **compiling and running separately** (1.3), **`jshell`** (1.4), **reading the first error** (1.5), **when the version is the wrong one** (1.6) and **code style** (1.7), and it ends with the first step of the **project** (1.8). At the end of the lesson, this is what you will see when running the `revisor`, which for now reviews nothing, but already exists (it prints `revisor: todavía no revisa nada`, “revisor: does not review anything yet”):

```text
revisor: todavía no revisa nada
```

## The concepts

### 1.1 Installing the JDK

#### 1.1.1 First: what you have

Open a terminal (on Mint: `Ctrl`+`Alt`+`T`) and ask two questions. The first, which system it is:

```bash
cat /etc/os-release
```

Among the lines it prints, look for `VERSION_CODENAME` and `UBUNTU_CODENAME`. On Linux Mint, the first is Mint's name (on 22.3, `zena`) and the second is that of the Ubuntu version it is based on (`noble`). To install Java with this course's method you will not need either, but it is good to know: many recipes on the internet were written for Ubuntu and find out “the version name” with the command `lsb_release -cs`, which on Mint 22.3 answers `zena`, not `noble` (measured on a Mint 22.3). A recipe that uses that answer to build a repository address leaves you pointing at an address that does not exist. If an Ubuntu recipe ever fails on your Mint, start there.

The second question, whether Java is already there:

```bash
java --version
```

One of three things can happen. If there is no Java, the terminal answers that it cannot find the command. On a freshly installed Mint 22.3 the result measured was `java: not found`, and something more useful than the text: the **exit code**, the number every program leaves when it finishes and that you see by typing `echo $?` right afterward. It was **127**, the number the terminal reserves for “that command does not exist”. The text of the notice can vary from one terminal to another; the 127 does not. (If you prefer to ask without running anything, `which java` looks for the program on your system: without Java it prints nothing and ends with code 1. They are two different codes for two different questions: “run this” and “where is this?”.) If it prints a version that starts with 21 (or 11, or 17), someone installed Java before, perhaps without realizing it, because many programs bring it as a dependency; it is not in the way, and in 1.1.7 you will see why it does not need to be removed. And if it prints 25 and says `Temurin`, you already have what the course uses and can skip to 1.1.6 to confirm that you also have the compiler.

#### 1.1.2 Why not `default-jdk`, and why SDKMAN

If you ask a search engine how to install Java on Ubuntu or Mint, the most frequent answer is this:

```bash
sudo apt install default-jdk
```

**Do not do it for this course.** The `default-jdk` package is not “the latest version of Java”: it is “the version of Java that the distribution chose as the default”, and that choice is frozen when the distribution is released. On Ubuntu Noble, the base of Mint 22, the `default-jdk` package depends on `openjdk-21-jdk` (according to its [page in the Ubuntu package archive](https://packages.ubuntu.com/noble/default-jdk), consulted on October 7, 2026, and measured on a Mint 22.3: `apt` offers `default-jdk` in its version `2:1.21-75`, the one for Java 21). That is, it installs **Java 21**. Java 21 is a respectable LTS version, but not the one this course teaches, and what matters for the lesson is what it cannot do: it **does not run the compact source file with `void main()`**. You will see it in 1.6 with its exact error message. Whoever installs `default-jdk` and follows this lesson sees the first program fail, and without knowing why, concludes that the course is wrong. It is not; the version is the wrong one.

The course uses **Eclipse Temurin 25**, the distribution with which all its outputs were measured (Lesson 0 explained what a distribution is), and installs it with **SDKMAN**, a version manager for Java tools. Three reasons decide it. The first: SDKMAN installs in your home folder (`~/.sdkman/`), **without `sudo`**, without touching the system's programs; if something goes wrong, you break nothing that Mint uses. The second: it lets you have several versions of Java at once and choose which to use with one command, something you will need the day someone else's project demands another version. The third: with the same tool you will install Maven in [Lesson 6](06-proyecto-maven.md), with the course's exact version. One method for everything, the same on every machine.

#### 1.1.3 What SDKMAN needs

SDKMAN is installed with a `bash` script that downloads and unpacks files, and for that it uses a few system tools. When run, the installer looks for `bash`, `unzip`, `zip`, `tar`, `find`, `curl` and `sed` (measured on October 13, 2026 on a Linux Mint 22.3). `bash`, `tar`, `find` and `sed` always come with Mint. The three that may be missing are `curl` (the program that downloads things from the terminal), `zip` and `unzip`. To check:

```bash
which curl zip unzip
```

`which` prints the path of each program it finds and stays silent for the one it does not. If you see all three paths, go on. If one is missing, install it with Mint's package manager (this is the only time the course uses `sudo`, because these are indeed system tools):

```bash
sudo apt install curl zip unzip
```

`sudo` runs what follows with administrator permissions and asks for your password (as you type it you will not see asterisks; that is normal). `apt install` installs the packages you name; if one was already there, it says so and continues with the others.

#### 1.1.4 Installing SDKMAN

The installer is a script that the project itself publishes on its official site. The [SDKMAN installation page](https://sdkman.io/install/) gives this command:

```bash
curl -s "https://get.sdkman.io" | bash
```

Read it in parts. `curl -s "https://get.sdkman.io"` downloads the installation script from the SDKMAN site, over `https` (the encrypted connection that guarantees you are talking to that site and that nobody changed the content along the way), and `-s` removes the progress bar. The pipe `|` hands that text to `bash`, which runs it. That is: you download a program and run it on the same line. That form has a bad reputation, and it pays to know exactly why. What is dangerous is not the form, but **where** what you run **comes from**: a script from a site you do not know, or pasted from a forum, can do anything with your user. Here the origin is the project's official site, over `https`, and the script does not ask for `sudo`: it works only in your home folder. And if you want to see it before running it, you can do it in two steps, which is a good habit with any installer: `curl -s "https://get.sdkman.io" -o instalar-sdkman.sh` saves it to a file, you read it with `less instalar-sdkman.sh`, and you run it with `bash instalar-sdkman.sh`.

This is what the installer printed on October 13, 2026 on a Linux Mint 22.3 (shortened to the lines that matter: the three dots mark what was removed, and the path of your home folder is shown as `~`):

```text
$ curl -s "https://get.sdkman.io" | bash
...
Looking for a previous installation of SDKMAN...
Checking Bash version...
Looking for unzip...
Looking for zip...
Looking for tar...
Looking for find...
Looking for curl...
Looking for sed...
Installing SDKMAN scripts...
...
Set version to 5.23.2 ...
...
Added sdkman init snippet to ~/.bashrc
...
All done!
...
Please open a new terminal, or run the following in the existing one:

    source "$HOME/.sdkman/bin/sdkman-init.sh"
```

It reads from top to bottom: it looks for a previous installation, checks that you have a version of `bash` that works for it, looks for the tools from 1.1.3 one by one, installs its scripts in `~/.sdkman/` and adds to your `~/.bashrc` file a snippet that loads SDKMAN every time a terminal is opened. The final line is the one to obey: **the terminal you are in does not yet know that SDKMAN exists**, because it read `~/.bashrc` when it opened, before the installation. Either you open a new terminal, or you ask this one to load SDKMAN now, with the `source` command it shows you. Afterward, check:

```text
$ sdk version

SDKMAN!
script: 5.23.2
native: 0.7.34 (linux x86_64)
```

If instead you see `sdk: command not found`, the terminal did not load SDKMAN. Open a new one, or load SDKMAN by hand in this one with `source "$HOME/.sdkman/bin/sdkman-init.sh"` and try `sdk version` again. If `source` says that file does not exist, the installation did not finish (almost always because of a tool from 1.1.3 that was missing or a dropped connection): read the installer's message, fix the cause and run the installation command again. The version numbers will be those of your day; what matters is that `sdk` answers.

#### 1.1.5 Choosing and installing Temurin 25

SDKMAN does not know which Java you want: you choose it by its **identifier**. To see the ones it offers:

```bash
sdk list java
```

It prints a long table, with a section for each distribution (Corretto, GraalVM, Liberica, Temurin, Zulu and others). Look for the **Temurin** one: its identifiers end in **`-tem`**. On October 13, 2026, on a Mint 22.3, that section had these: `27.0.0.0+35-tem`, `26.0.2+1.1-tem`, `25.0.3.0+9-tem`, `21.0.12+1.1-tem`, `17.0.20-tem`, `11.0.32+1.1-tem` and `8.0.504+1-tem`. The rule for choosing is one: **the one that starts with `25` and ends in `-tem`**. That day it was `25.0.3.0+9-tem`; on your screen it may be another, because the full number changes with every Temurin update. Do not copy the one in this lesson: copy the one from your list.

A measured detail that is worth knowing so as not to get confused: at the foot of the list, SDKMAN suggests a default identifier (that day it said `install the default: 25.0.4-tem`), and that identifier **did not appear in the list**. That is why the course does not ask you to use the suggestion, but to choose from the list what is actually available. With the identifier in hand:

```text
$ sdk install java 25.0.3.0+9-tem
...
Downloading: java 25.0.3.0+9-tem
...
Repackaging Java 25.0.3.0+9-tem...
...
Installing: java 25.0.3.0+9-tem
Done installing!

Setting java 25.0.3.0+9-tem as default.
```

It downloads the JDK, arranges it and installs it in `~/.sdkman/candidates/java/`, and the last line says something important: it sets it as the **default**, the one that will answer the `java` command in the terminals you open from now on. Depending on the case (for example, if you already had another Java installed with SDKMAN), instead of setting it just like that it may **ask you** whether you want it to be the default: answer yes, with `Enter` or `Y`. If you answered no, or are not sure, you fix it at any time with `sdk default java` and the identifier you installed (here, `sdk default java 25.0.3.0+9-tem`). The download takes from one to several minutes, depending on your connection.

#### 1.1.6 Checking the version, in a new terminal

Here is the trap that wastes the most time, and it was measured on the same Mint: **in the terminal where you installed, `java --version` kept giving the previous Java** (on that machine there was one from the system, in `/usr/bin/java`). It is not that the installation failed: that terminal decided where to look for programs when it opened, and does not notice the change. **Close the terminal and open a new one.** There:

```bash
java --version
javac --version
```

Both commands, and not only the first, because `java` is the program that runs and `javac` the one that compiles, and a half-done installation can give one without the other (it is exactly the difference between a JRE and a JDK from Lesson 0). This is what `java --version` printed on October 13, 2026 on the Mint 22.3, in the new terminal:

```text
$ java --version
openjdk 25.0.3 2026-04-21 LTS
OpenJDK Runtime Environment Temurin-25.0.3+9 (build 25.0.3+9-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.3+9 (build 25.0.3+9-LTS, mixed mode, sharing)
```

There are two things you should recognize in yours: **the first line starts with `openjdk 25`** and **the second says `Temurin`**. The rest changes with the update: `25.0.3` is the version (25 is the major one; `0.3` is the patch number), `2026-04-21` is the date of that update, and `LTS` confirms it is a long-term-support version. The outputs in the other lessons were measured on another machine with a later patch (Temurin 25.0.4); the patch does not change anything the course teaches. `javac --version` should say `javac 25` followed by its patch. And if you want to know where the `java` that answered comes from, `which java` says so: on the measured Mint, the path inside the SDKMAN folder ending in `.sdkman/candidates/java/current/bin/java`. (`which` prints the full path, with your home folder: `/home/<your user>/.sdkman/candidates/java/current/bin/java`; here it is shortened with `~`.) If `which java` gives you `/usr/bin/java`, you are in an old terminal.

#### 1.1.7 If you have more than one Java

> **Several JDKs at once.** If you already had a system Java, or if one day you install another version with SDKMAN, there is no need to uninstall anything: SDKMAN chooses which one answers the `java` command. `sdk list java` marks in its table the ones you have installed. `sdk use java <identifier>` switches versions **only in the current terminal**, to try something; `sdk default java <identifier>` sets it as the default **in all new terminals**. If `java --version` does not say 25 and Temurin, the first thing is to open a new terminal; the second, `sdk default java` with the identifier from 1.1.5. Afterward, repeat the check from 1.1.6, which is repeated whenever you change something.

### 1.2 Your first program

#### 1.2.1 The folder and the file

Create the course folder and go into it:

```bash
mkdir -p ~/curso-java/revisor
cd ~/curso-java
```

`mkdir -p` creates the folder, and also the intermediate ones if missing; without `-p`, it would fail if `~/curso-java` did not exist when `revisor` is requested inside it. `~` is a shortcut for your home folder. In `~/curso-java/` the whole course will live, and inside it `revisor/` will hold the project that grows lesson after lesson.

Create the file `Hola.java` with the editor of your choice (Mint comes with Xed, which opens with `xed Hola.java`) and type this. Respect the initial capital letter of the name and the extension. The first line, `// fig01_01.java`, is a comment: what comes after `//` until the end of the line is ignored by the compiler. In the course, that comment is the mark that says what the program is called in the repository; in your `Hola.java` you can copy it or not, it makes no difference:

```java
// fig01_01.java
void main() {
  IO.println("Hola desde Java");
}
```

Save it and, from the terminal, in the `~/curso-java` folder, run it with the name of **your** file:

```text
$ java Hola.java
Hola desde Java
```

(In the course repository the same program is called `fig01_01.java`, and `java fig01_01.java` prints the same.)

That is the whole program. Let us see what each part says.

#### 1.2.2 Line by line

**`void main() {`** declares the entry point: the first thing Java runs when the program runs. It has three pieces. `main` is the name Java looks for to begin (it is not a reserved word, it is a convention of the language that the launcher knows). `()` are the parentheses where the data the method receives would go; here it receives none. `void` means “returns nothing”: `main` does things, but does not hand a result to whoever called it. The braces `{ }` delimit the body: everything between the opening one and the closing one is what `main` does. This “method” with no class around it is new in Java 25: it is called a **compact source file** ([JEP 512](https://openjdk.org/jeps/512)).

**`IO.println("Hola desde Java");`** prints a line of text. `IO` is a class of the standard library, `java.lang.IO`, added in Java 25 for simple input and output ([documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/IO.html)); since it is in the `java.lang` package, which Java always imports, **there is nothing to import**. `println` (“print line”) writes what it is given and a line break. What goes between the parentheses, `"Hola desde Java"`, is a text, which in Java is written between double quotes. The semicolon `;` ends the statement: in Java it is mandatory (unlike Python or Go), and forgetting it is the most common error of the first day.

**`}`** closes the body.

Two things that are **not** there: a class around it, and the words `public static` or the `String[] args`. In the rest of the course that is the norm. Who created the class, then? The compiler, silently: it takes your file, gives it the file's name as the class name (`Hola`) and wraps your `main` in it. You will verify it in 1.3 with `javap`.

#### 1.2.3 How it used to be written

It is essential that you recognize the old form, because it is the one you will see in almost all the tutorials, books and internet answers that are not from the last two years. The same program, written as was required up to Java 24, is:

```java
public class Viejo {
  public static void main(String[] args) {
    System.out.println("Hola desde Java");
  }
}
```

It runs exactly the same on Java 25 (it was checked: it prints `Hola desde Java`, “Hello from Java”). Each extra word had a function, which Lesson 6 explains in depth, and which is only sketched here so they do not frighten you when you see them. `class Viejo { ... }` defines a class (`Viejo` means “old”), the unit in which Java organizes code; `public` makes it visible from outside the file. `static` means that the method belongs to the class and not to a particular object, so it can be called without creating anything first. `String[] args` is the list of arguments that the user types after the program's name on the command line. And `System.out.println` is the long way to print: `System` is a class, `out` is an object inside it (the standard output) and `println` is what is asked of it.

For a one-line program, all that is ceremony. Java 25 removes it **when it is not needed**, and it becomes necessary again when the program grows. The course uses the short form until it grows, and `IO.println` instead of `System.out.println`, which in this course is shorter to write and easier to read. If you read code that uses `System.out.println`, it does the same; there is no need to change it.

#### 1.2.4 The file name and other details that bite

There are four details worth knowing from now on. **One:** in a compact source file, the compiler uses the file name as the class name, so it must be a valid name for Java and, by convention, start with a capital letter (`Hola.java`, `Revisor.java`). **Two:** the program must have a `main` method; with another name, the compiler rejects it with a message that says so (you will see it in 1.5). **Three:** the text of the file must be in UTF-8, which is what Mint's editors produce by default. **Four:** `java Hola.java` and `java hola.java` are different files on Linux, where capital letters count; on macOS and Windows it sometimes makes no difference, and it is a classic of “it worked on my machine”.

And one more detail about what `java Hola.java` does: the `java` program sees that you gave it a `.java` file, **compiles it in memory** and runs it, leaving nothing on disk. It is called the source-file launch mode ([JEP 330](https://openjdk.org/jeps/330)) and has existed since Java 11. It is the one the course uses from Lesson 1 to 5.

### 1.3 `javac` and `java` separately

#### 1.3.1 Compiling

What `java Hola.java` does in one step can be done in two, and seeing it that way is the best way to understand what a `.class` is. In the folder where `Hola.java` is:

```bash
javac Hola.java
ls Hola.*
```

```text
$ ls Hola.*
Hola.class  Hola.java
```

`javac Hola.java` compiled the file and, if there were no errors, printed nothing (a compiler that says nothing is a happy compiler), but it left a new file: `Hola.class`. That file is the *bytecode* from Lesson 0. It weighs 320 bytes and starts with the eight bytes you already know (if your Mint does not come with `xxd`, skip this command: it is not used again):

```text
$ xxd -l 8 Hola.class
00000000: cafe babe 0000 0045                      .......E
```

To see what it contains without reading it byte by byte, the JDK has the `javap` disassembler:

```text
$ javap Hola
Compiled from "Hola.java"
final class Hola {
  Hola();
  void main();
}
```

There is the proof of what was said in 1.2.2: the compiler created a class called `Hola` (like the file), and put your `main` method inside it, plus a `Hola()` constructor that you did not write (a constructor is what creates an object of the class; Lesson 3 explains it). With `javap -c -p Hola` you would also see the instructions of `main`, among them a call to the `println` method of the `IO` class.

#### 1.3.2 Running

To run the `.class`, the launcher is no longer given the file but **the name of the class**:

```text
$ java Hola
Hola desde Java
```

No extension: `Hola`, not `Hola.class` or `Hola.java`. It is the most frequent confusion in this section. The three errors it produces were measured, and it pays to have them in view; the first is that of running the class **before** compiling, the second that of adding the extension, and the third that of forgetting the extension in the compiler:

```text
$ java Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola

$ java Hola.class
Error: Could not find or load main class Hola.class
Caused by: java.lang.ClassNotFoundException: Hola.class

$ javac Hola
error: Class names, 'Hola', are only accepted if annotation processing is explicitly requested
1 error
```

The first two were obtained with Java's language set to English; on a system with the locale set to Spanish, the same message comes out translated (for example, “Error: no se ha encontrado o cargado la clase principal Hola”, which means “Error: could not find or load main class Hola”). Learn to recognize them in both languages: “no se pudo encontrar o cargar la clase principal” (“could not find or load main class”) means **that `java` looked for a class with that name and did not find it**; almost always because compiling is missing, because you misspelled the name, or because you are in another folder. The third message is the most tangled of the three: `javac` interpreted `Hola` as the name of a class and not as a file, and its complaint talks about “annotation processing”, a topic that has nothing to do with what happened to you. The fix is the same as always: `javac` takes the name of the **file**, with its `.java`.

Why `java` looks for the class where it does: by default, in the current folder. There is a mechanism to tell it to look elsewhere (the *classpath*, which Lesson 6 introduces and Lesson 8 studies), but as long as you are standing in the folder of the `.class`, it finds it.

#### 1.3.3 The old `.class`

The `.class` is a frozen copy: **it does not update itself when you edit the `.java`**. It was measured like this. A program that prints `version uno` (“version one”) was compiled, run with `java V`, the file was changed so that it prints `version dos` (“version two”) and run again with `java V`, without compiling:

```text
$ java V
version uno
```

It still says `version uno`, even though the `.java` now says something else, because `java V` runs the `.class` and the `.class` has not found out. You have to compile again with `javac V.java`. In contrast, `java V.java` (with the extension) recompiles on every run and always reflects the current file, which is another reason the course uses it in the first lessons. The day the project grows, Maven will take care of recompiling when needed, which is one of the things it exists for (Lesson 6).

### 1.4 `jshell`: trying an idea without creating a file

Among the JDK's tools, there is one you will use every day: `jshell`, an interactive interpreter ([manual](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jshell.html)). You type an expression, Java evaluates it and shows you the result, with no file, no `main`, no compiling. It is the fastest way to answer “what does this do?”. In the terminal, type `jshell`:

```text
$ jshell
|  Welcome to JShell -- Version 25.0.4
|  For an introduction type: /help intro

jshell> 1 + 1
$1 ==> 2

jshell> String s = "hola"
s ==> "hola"

jshell> s.length()
$3 ==> 4

jshell> "142" + 1
$4 ==> "1421"

jshell> /vars
|    int $1 = 2
|    String s = "hola"
|    int $3 = 4
|    String $4 = "1421"

jshell> /exit
|  Goodbye
```

This session was put together with the real answers of a `jshell` run with those same commands. Each time you type something at the `jshell>` prompt, it evaluates it and responds with an arrow `==>` and the value. Expressions that have no name receive an automatic one: `$1`, `$3`, `$4`. (Where is `$2`? The statement `String s = "hola"` gave its value a name, `s`, and that is why the counter skipped.) The `/vars` command lists the variables that exist, with their type, and `/exit` leaves. `jshell` commands start with a slash, to tell them apart from Java code.

Notice the fourth entry: `"142" + 1` gave `"1421"`, the same result you saw in `fig00_04.java`. This is what is valuable about `jshell`: it serves to **check** a doubt in ten seconds. It also shows you errors without noise:

```text
jshell> int x = 5
x ==> 5

jshell> x = "a"
|  Error:
|  incompatible types: java.lang.String cannot be converted to int
|  x = "a"
|      ^-^

jshell> foo()
|  Error:
|  cannot find symbol
|    symbol:   method foo()
|  foo()
|  ^-^
```

With three limitations worth stating right away. `jshell` does not replace a program: what you try there is not saved (there is `/save` for that, but a real file is better). Some things in the language that need context, such as a `main` or a class with several methods, are not comfortable there. And the numbering `$1`, `$2`... is confusing at first. Use it for doubts about an expression or a library method, not to write the `revisor`.

### 1.5 Reading the first compiler error

#### 1.5.1 Provoking it

Compiler errors are the first thing you will read every day, so it is good to be at ease with them from now on. We provoke one on purpose. This program has a typo: the method is called `println`, and the program writes `prinln`. Save it in `~/curso-java` with the figure's name, `fig01_02.java`, so that the message you see says the same as the one below (the message names the file), and copy the comment on the first line too, because line numbers count it:

```java
// fig01_02.java
void main() {
  IO.prinln("hola");
}
```

```text
$ java fig01_02.java
fig01_02.java:3: error: cannot find symbol
  IO.prinln("hola");
    ^
  symbol:   method prinln(String)
  location: class IO
1 error
error: compilation failed
```

#### 1.5.2 Anatomy

It is a message of six parts, and all six repeat, with small variations, in any compile error you will see in your life.

1. **`fig01_02.java:3:`** is the file and the **line** number. Line 1 is the comment `// fig01_02.java`, 2 is `void main() {` and 3 is the one that fails. When the error gives a line, that is the place to look first; sometimes the real problem is a little earlier (on a previous line missing a `;`), but the compiler only notices here.
2. **`error: cannot find symbol`** is the **diagnosis**. A symbol is a name: of a variable, a method, a class. The compiler saw a name and does not know what it refers to.
3. **The quoted line**, `IO.prinln("hola");`, copied from the file so you do not have to go look for it.
4. **The `^` arrow**, which points to the column where the compiler detected the problem. Here it is under the dot, at the border between `IO` and what follows: the compiler knows `IO` exists, and what it does not find is what comes after the dot.
5. **`symbol:   method prinln(String)`** says **what** it did not find in as much detail as it can: a method called `prinln` that receives a `String` (the text). Notice that it deduced the type `String` from the argument you passed.
6. **`location: class IO`** says **where** it looked for it: in the `IO` class. Together, `symbol` and `location` read like a sentence: “I looked for the method `prinln(String)` in the `IO` class and it does not exist”. And that sentence tells you how to fix it: either it does not exist because you misspelled it (this is the case), or because it is not in that class, or because an `import` is missing.

At the end, `1 error` counts how many it found, and `error: compilation failed` is the `java` launcher warning that, because of that, it ran nothing. If you compile with `javac`, the message is the same but without that last line, and `javac` ends with exit code 1.

#### 1.5.3 How to read a compiler

There are three habits that save hours. **Read the first error, not the last.** Often a single problem produces a cascade: an unclosed brace can generate ten messages. Fix the first one, compile again and see how many remain. **Read the whole message before searching the internet.** `symbol` and `location` are usually enough. And **when you do not understand an error, shrink the program** until what remains is the line that produces it.

Five more errors, measured with Temurin 25.0.4 with three-line programs and described here so you recognize them when you see them. The messages are shown shortened to their first line:

| What you wrote | What the compiler says |
|---|---|
| the `;` at the end is missing: `IO.println("hola")` | `error: ';' expected` (with the arrow right after the parenthesis) |
| the closing quotes are missing: `IO.println("hola);` | `error: unclosed string literal` |
| `io.println("hola");` (lowercase) | `error: cannot find symbol` / `symbol:   variable io` / `location: class <name of the file>` |
| the final brace `}` is missing | `error: reached end of file while parsing` |
| the method is called `principal` and not `main` | `error: compact source file does not have main method in the form of void main() or void main(String[] args)` |

The third deserves a read: `io` in lowercase is not `IO`; Java distinguishes capital letters, so the compiler thinks `io` is a variable and does not find it. The last one says in so many words what Java expects for a compact source file: a `main` method, with no parameters or with just one, a `String[] args`.

### 1.6 When the version is the wrong one

This section explains one of the most common stumbles for someone starting with Java. The program in 1.2, `Hola.java`, only works on Java 25. With an earlier version, the errors are different, and it pays to see them so you can identify them at a glance. With **Java 21** (the one `default-jdk` installs on Mint 22), the same `Hola.java` gives the following. (These two blocks are fragments and not figures of the course: they were run with another JDK, 21 and 17, and that is why they are not compared with Java 25.)

```text
$ java --version
openjdk 21.0.12 2026-07-21 LTS
OpenJDK Runtime Environment Temurin-21.0.12+8 (build 21.0.12+8-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.12+8 (build 21.0.12+8-LTS, mixed mode, sharing)

$ java Hola.java
Hola.java:2: error: unnamed classes are a preview feature and are disabled by default.
void main() {
^
  (use --enable-preview to enable unnamed classes)
1 error
error: compilation failed
```

It says, in essence, that “unnamed classes are a preview feature and are disabled”. In Java 21 the compact source file existed, but as a preview (the idea from Lesson 0): you had to ask for it with `--enable-preview`, and with another name and another form. In Java 25 it is final. With **Java 17**:

```text
$ java --version
openjdk 17.0.20 2026-07-21
OpenJDK Runtime Environment Temurin-17.0.20+8 (build 17.0.20+8)
OpenJDK 64-Bit Server VM Temurin-17.0.20+8 (build 17.0.20+8, mixed mode, sharing)

$ java Hola.java
Hola.java:2: error: class, interface, enum, or record expected
void main() {
^
Hola.java:4: error: class, interface, enum, or record expected
}
^
2 errors
error: compilation failed
```

“A class, interface, enum or record was expected”: Java 17 does not know the compact source file, and expects everything in a file to be inside a class.

**The diagnosis is always the same:** `java --version`. If it says 21 or 17, it is not an error in your program, it is the version. To fix it, install Temurin 25 with SDKMAN (1.1.5), open a new terminal and, if you have several, choose the right one with `sdk default java` (1.1.7). The key: **do not change your program so that it compiles on an old version**. The whole course is written for Java 25.

### 1.7 Code style

One last detail that seems minor and is not. You will have noticed that this lesson's code uses **two spaces** of indentation, not four or tabs. It is not a whim: it is Google's style guide for Java ([Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)), one of the most widespread, and the one the Checkstyle tool verifies in Lesson 10. If the code of lessons 1 to 9 used another style, lesson 10 would have to confess that everything earlier was badly written. Much Java code on the internet uses four spaces; it works the same, and it is just a convention. The guide asks for three things from now on: **2-space** indentation, lines of at most **100 columns**, and braces always, even in a one-line `if` (when you get to it).

### 1.8 The project step: the `revisor`'s first file

The `revisor` starts today. It does not review anything yet; what matters is that it exists, in the right place, and that it runs. In the `~/curso-java/revisor/` folder that you created in 1.2.1, create the file `Revisor.java` with this content (you can omit the comment on the first line; it is there because in the repository the figure is called `fig01_03.java`):

```java
// fig01_03.java
void main() {
  IO.println("revisor: todavía no revisa nada");
}
```

From `~/curso-java/revisor/`, run it with the name of your file:

```text
$ java Revisor.java
revisor: todavía no revisa nada
```

(Notice the `í` in “todavía” (“still”): Java writes the accent without problem, as long as your terminal uses UTF-8, which is normal on Mint. If one day you see `?` in its place, it is the system's locale and not your program; Lesson 0 measured it.)

Now do it the old way as well, to see the project's `.class`:

```bash
javac Revisor.java
java Revisor
javap Revisor
```

It prints the same, and `javap` shows `final class Revisor`, with the `main` method inside. When you finish, **delete the `.class`** with `rm Revisor.class`: it is a file of yours, that you just generated, and is no use to you anymore, because from now on the way to run the project is `java Revisor.java`, and an old `.class` in the folder is a trap (1.3.3) for when you edit the `.java` and run the `.class` out of habit.

The project grows from here. In Lesson 2, `Revisor.java` learns to classify a service; in 3 it is split into several files; in 6 it becomes a Maven project. The `revisor` that finishes the course starts out as this three-line program.

## The error you will see

This lesson has two main errors. The first is the usual one: the compiler that does not find a name. The second is that of the wrong version, and it is the one that most disorients someone starting out.

**The first: `cannot find symbol`.** It is the one from `fig01_02.java`, and it is read with the six parts of 1.5.2:

```text
fig01_02.java:3: error: cannot find symbol
  IO.prinln("hola");
    ^
  symbol:   method prinln(String)
  location: class IO
```

It is almost always a typo, a wrong capital letter or a forgotten `import`. It is fixed by correcting the name and running again. What you do **not** do is delete lines until it disappears: the message is telling you exactly which name it does not find and where it looked for it.

**The second: the compact source file with an earlier version.** With Java 21:

```text
error: unnamed classes are a preview feature and are disabled by default.
  (use --enable-preview to enable unnamed classes)
```

With Java 17, `error: class, interface, enum, or record expected`. They are the two symptoms of a single disease: **a Java version earlier than 25**. It is not fixed by touching the program but the installation: `java --version` to confirm, and 1.1.5 and 1.1.7 to correct. It is the error of whoever installed `default-jdk` or follows an old tutorial.

**A third, more modest one:** `Error: Could not find or load main class Hola`. It means `java Hola` was run without having compiled, or from a folder where the `.class` is not. Compile with `javac Hola.java`, or run with `java Hola.java`.

## What gets done wrong

**Installing `default-jdk` out of habit.** It is the most popular answer and, on Mint 22, it installs Java 21, with which the compact source file fails. The course's path is Temurin 25 with SDKMAN (1.1.4 and 1.1.5).

**Not checking the version after installing.** “It installed without errors” is not “the `java` command points to 25”. The check in 1.1.6 is two commands and ten seconds, and it is repeated whenever you change something in the system.

**Checking in the same terminal where you installed.** It is the measured trap of 1.1.6: that terminal keeps using the Java it had when it opened, and you conclude that the installation failed. Open a new terminal before checking.

**Checking only `java` and not `javac`.** A JRE has the first and not the second. For this course you need both.

**Running scripts from origins you do not know.** `curl ... | bash` runs what it downloads, and that is why what matters is where it comes from: SDKMAN's installer comes from the project's official site, over `https`, and can be read before running it (1.1.4). A script pasted from a forum or any site offers neither of those two things. The same goes, all the more, for any command with `sudo`, which modifies your system with administrator permissions: if you do not know what each part does, do not run it.

**Running `java Hola` without having compiled, or editing and not recompiling.** It is the old `.class` from 1.3.3: the program does what the previous version did and you think your change did not work. While you are learning, use `java Hola.java`, which always compiles the current file.

**Putting the extension where it does not go, and removing it where it does.** `java` with a source file takes `.java` (`java Hola.java`); with a compiled class it takes no extension (`java Hola`); and `javac` always takes `.java`. They are three rules, and they are learned by heart with use.

**Blindly fixing several errors at once.** A compile error can generate others that do not really exist. Fix the first, compile again, and see what remains.

**Writing with tabs or with four spaces “because everyone does”.** It compiles the same, but in Lesson 10 the style tool will complain about every line. Configure your editor with two spaces (in Xed: Preferences, Editor tab, tab width 2 and insert spaces instead of tabs).

**Following a tutorial without looking at its date.** If the tutorial uses `public class` and `public static void main` for a single-file program, it is from before Java 25; it works, but it is not this course's style. And if it talks about `default-jdk` or an earlier JDK, the same.

## Exercises

### Exercise 1 — Your first program, your way

Modify `Hola.java` so that it prints three lines: your name, the name of the course and the phrase “Java 25”. Run it with `java Hola.java`. Then compile it with `javac Hola.java`, run it with `java Hola` and compare. Finally, change a word of the text, **without** recompiling, and run `java Hola` again. Write in your logbook what happened and why.

### Exercise 2 — Predict and check three errors

Write three broken versions of `Hola.java`, one at a time: (a) without the `;` at the end of the `IO.println`; (b) without closing the quotes of the text; (c) with `io.println` in lowercase. **Before running each one, write in your logbook what message you expect.** Run them and compare. For each one, indicate the line the compiler points to and where the arrow points.

### Exercise 3 — Ask `jshell`

Open `jshell` and evaluate: `"142" + 1`, `2300 / 1000` and `2300 / 1000.0`. For each one, write beforehand what you think it gives. Then type `/vars` and leave with `/exit`. One of the three answers will probably surprise you: write in the logbook which one and why you think it gives what it gives. (The full explanation is in Lesson 2; here what counts is having seen it.)

### Exercise 4 — Diagnose from the message

For each message, say what happened and how to fix it, without running anything:

1. `Error: Could not find or load main class Hola` / `Caused by: java.lang.ClassNotFoundException: Hola`, when typing `java Hola`.
2. `error: Class names, 'Hola', are only accepted if annotation processing is explicitly requested`, when typing `javac Hola`.
3. `error: unnamed classes are a preview feature and are disabled by default.`, when typing `java Hola.java`.

## Solutions

### Solution 1

A possible `Hola.java`:

```java
void main() {
  IO.println("Dorian Chávez");
  IO.println("Curso de Java de Hábil");
  IO.println("Java 25");
}
```

With `java Hola.java` it prints the three lines. After `javac Hola.java`, `java Hola` prints the same. If you now change the text of an `IO.println` in the `.java` and run `java Hola` **without** compiling again, it prints the **old** text: the `.class` is a frozen copy (1.3.3) and does not know you changed the source. To see the change you have to run `javac Hola.java` again, or use `java Hola.java`, which always recompiles. What you write in the logbook: the `.class` does not update itself.

### Solution 2

The three messages, measured (each is shown with its pointed line; the compiler always ends with `1 error` and `error: compilation failed`):

```text
a.java:2: error: ';' expected
  IO.println("hola")
                    ^

b.java:2: error: unclosed string literal
  IO.println("hola);
             ^

c.java:2: error: cannot find symbol
  io.println("hola");
  ^
  symbol:   variable io
  location: class c
```

In (a), the compiler points to line 2, not a line later, and puts the arrow **right after the last character** of the `IO.println("hola")`, because that is where the `;` was expected. In (b), the arrow points to the opening quote: it is where the text that was never closed begins. In (c), the arrow points to `io`, and the message says `variable io`, not `class io`: since the lowercase does not match any name Java knows, the compiler assumes it is the name of a variable. If typo (c) made you think “it should say that `io` does not exist”, that is exactly what it says, in the compiler's vocabulary. (In `location: class c`, the name `c` is that of the file `c.java` it was measured with; in your file it will say yours.)

### Solution 3

The `jshell` answers, measured:

```text
jshell> "142" + 1
$1 ==> "1421"

jshell> 2300 / 1000
$2 ==> 2

jshell> 2300 / 1000.0
$3 ==> 2.3
```

The one that surprises is the second: **`2300 / 1000` gives `2`, not `2.3`**. When both numbers are integers, Java does an integer division and discards the decimal part. To get `2.3`, one of the two has to be decimal, like the `1000.0` in the third. It is a matter that comes up later: the `revisor` converts 2300 milliseconds to “2.3s” and, if it divided integers, it would show `2s`. Lesson 2 explains it in depth. (Here you have seen it with your own eyes, which is what mattered. The numbering in your session may differ depending on what you evaluated before.)

### Solution 4

1. **The class was run without having been compiled** (or from another folder). `java Hola` looks for a file `Hola.class` and there is none. It is fixed by compiling first (`javac Hola.java`) or by running the source file directly (`java Hola.java`).
2. **`javac` was given the name of the class and not that of the file.** `javac` takes `.java`: `javac Hola.java`. The text about “annotation processing” is confusing, but it is the way `javac` interprets an argument without an extension.
3. **The Java version is earlier than 25**, probably 21: the compact source file without `class` is only final in Java 25. It is fixed by installing Temurin 25 with SDKMAN (1.1.5), leaving it as the default with `sdk default java` if there is another (1.1.7), and confirming with `java --version` in a new terminal.

## How I know I got it

- [ ] In a new terminal, `java --version` prints a first line that starts with `openjdk 25` and a second that says `Temurin`, and you know what the code 127 meant if you did not have Java before.
- [ ] `sdk version` answers, and you know which Temurin identifier you installed and why that one.
- [ ] `javac --version` prints `javac 25` followed by a patch number.
- [ ] `java Hola.java` prints what you wrote, and `javac Hola.java` leaves a `Hola.class` that is run with `java Hola`.
- [ ] You can explain why, if you edit the `.java` and run `java Hola`, you do not see your change.
- [ ] In `jshell`, `1 + 1` gives `$1 ==> 2` and you know how to leave with `/exit`.
- [ ] You provoked the error of `fig01_02.java` and can point out in it the line, the arrow, the `symbol` and the `location`.
- [ ] From `~/curso-java/revisor/`, `java Revisor.java` prints `revisor: todavía no revisa nada`.
- [ ] You deleted the `Revisor.class` that you generated, and you know why.
- [ ] You wrote in your logbook what `java --version` told you on your machine and what happened with Exercise 1.

## Summary

Answer without looking at the lesson:

1. Why is `default-jdk` on Mint 22 no good for this course? What message does the compact source file produce with that version?
2. What does `curl -s "https://get.sdkman.io" | bash` do, and what makes it acceptable here? How do you choose the Temurin identifier in `sdk list java`? Why do you have to open a new terminal after installing?
3. Which two commands are run to check the installation, and why both?
4. What is the compact source file? Which two things from the traditional program are no longer needed?
5. What is the difference between `java Hola.java` and `java Hola`? When does the `.class` become outdated?
6. What does `Hola.class` contain and how did you check it with `javap`?
7. What is `jshell` for and what is it not?
8. What do the six parts of a message like `cannot find symbol` say?
9. How do you tell, from the message alone, an error in a program from an error caused by the wrong version?

## Further reading

- [SDKMAN, installation](https://sdkman.io/install/) and [usage](https://sdkman.io/usage/): the requirements, the installer, and the `list`, `install`, `use` and `default` commands. Consulted on October 13, 2026.
- [JEP 512, “Compact Source Files and Instance Main Methods”](https://openjdk.org/jeps/512): the proposal that defines the compact source file, `void main()` and the `IO` class; it explains what the compiler creates for you and why. Consulted on October 7, 2026.
- [Oracle, `jshell` manual (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jshell.html): all the `jshell` commands, from `/vars` to `/save`. Consulted on October 7, 2026.
- [Oracle, `java` command manual (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html): the source-file launcher and flags such as `--enable-preview`. Consulted on October 7, 2026.

---

**Previous:** [Lesson 0 — What Java is and what it is NOT](00-que-es-java.md) ·
**Next:** [Lesson 2 — Types, strings and control flow](02-tipos-cadenas-control.md)
