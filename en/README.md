# Java course — from modern Java to a program that checks many services at once

**By Dorian Chávez, founder of Hábil and integration architect.**

**Who it is for:** anyone who already programs in another language —Go, TypeScript, Python, or whichever— and wants to learn today's Java. It is an intermediate-level course: it does not explain what a variable or a loop is; it explains what Java does differently and why it exists. Every new concept is explained when it appears. If you have never programmed, start with the [Python course](https://github.com/HabilMX/curso-python) or the [Go](https://www.habil.mx/en/courses/go/) one.

**What you will know by the end:** how to write a complete program in Java 25, with the data model expressed in types, tests and concurrency, understand what you wrote and be able to explain it to someone else. And you will keep the judgment to read the language's documentation on your own.

**What you need before you start:** a computer with Linux Mint, knowing how to open a terminal, and having written programs with functions, conditionals and loops in any language. [Lesson 1](01-instalacion.md) installs the JDK from scratch, and [Lesson 6](06-proyecto-maven.md) installs Maven.

## The project you will build

The **`revisor`** (Spanish for “reviewer”; it is the name of the course program and it is not translated): a program that reads a list of services, queries **all of them at once** over HTTP, measures how long each one takes, isolates the failure of each service and delivers a report in the same order every time. At the end it exposes the report through a small server and packages it in a JAR. It is the same problem as in the Go, Rust and TypeScript courses, now solved in Java.

    $ java -jar revisor.jar --config servicios.json
    SERVICIO    ESTADO    TIEMPO  DETALLE
    catalogo    ARRIBA     182ms  200
    inventario  LENTO       2.3s  200
    pagos       ARRIBA     132ms  200
    reportes    CAIDO          —  sin conexión

    4 servicios · 2 arriba · 1 lento · 1 caído · promedio 883ms

(A real run of the finished program against the course's practice server, on October 13, 2026; the times change on every run. The program prints its labels in Spanish: SERVICIO is “service”, ESTADO “status”, TIEMPO “time”, DETALLE “detail”, ARRIBA “up”, LENTO “slow”, CAIDO “down”, “sin conexión” “no connection”, and the last line reads “4 services · 2 up · 1 slow · 1 down · average 883ms”.)

It looks small and it is not: to write it well you need sealed types, collections, exceptions, a Maven project with tests, the HTTP client, JSON, virtual threads and a package that can be delivered.

## The eleven lessons

| Lesson | | What you build | What you learn |
|---|---|---|---|
| 0 | [What Java is and what it is NOT](00-que-es-java.md) | nothing yet (reading) | source code, *bytecode* and the JVM; JDK and JRE; what LTS is and why 25; what the compiler protects and what it does not |
| 1 | [Installing Java 25 on your Linux Mint](01-instalacion.md) | the environment and your first program | JDK, `java --version`; `void main()` and `IO.println`; `java Hola.java`; `jshell`; `javac`; the first error |
| 2 | [Types, strings and control flow](02-tipos-cadenas-control.md) | the `revisor`'s base functions | primitives and references; `var`; `==` versus `equals`; `&&` versus `&`; text blocks; `switch` as an expression; `null` |
| 3 | [Records, enums and sealed types](03-records-enums-sellados.md) | the `Servicio` / `Estado` model | `record`; immutability; `enum`; `sealed`; exhaustive `switch` with patterns; record patterns |
| 4 | [Collections, generics and streams](04-colecciones-genericos-streams.md) | the list of services and the report | `List`, `Map`, `Set`; immutable collections; generics; `Comparator`; *streams*; `Optional` |
| 5 | [Errors and exceptions](05-errores-excepciones.md) | the `revisor` that does not crash | checked and unchecked; `try-with-resources`; custom exceptions; result versus exception |
| 6 | [A Maven project](06-proyecto-maven.md) | the real project: it compiles, packages and runs | packages and visibility; installing Maven; Maven's structure; `pom.xml`; the lifecycle; what is inside a `.jar` |
| 7 | [Tests with JUnit](07-pruebas-junit.md) | the `revisor`'s first 18 tests | JUnit 6; `@Test`; table-driven tests; `assertThrows` and `@TempDir`; seeing it fail; reading the report |
| 8 | [HTTP and JSON](08-http-y-json.md) | a real request and the configuration | `HttpClient`; timeouts; Jackson 3; validating outside data; SSRF |
| 9 | [Concurrency: checking everything at once](09-concurrencia.md) | the concurrent `revisor` | virtual threads; one thread per task; `invokeAll`; `Semaphore`; `ConcurrentHashMap`; a measured race condition |
| 10 | [The finished program](10-el-programa-terminado.md) | the complete `revisor`, ready to deliver | command line and exit code; the JDK's server; graceful shutdown; quality; executable JAR |

At the end of each lesson there are exercises with their solutions, and a sources section with the links for what was claimed. And the [logbook](bitacora.md) is yours: write down there what was hard for you.

## Three criteria that run through the whole course

**Errors are taught by what they cost.** Studies with data from hundreds of thousands of Java students show that teachers guess wrong about which errors are the hard ones. The ones that take longest to fix are confusing `==` with `equals`, `&` with `&&`, and not using the value a method returns. Those appear when their turn comes, and each lesson deliberately provokes the error you are going to see.

**Tests and security are not lessons, they are habits.** There are tests from the moment the project can run them (lesson 7, right after turning it into a Maven project), and a test does not count until you have seen it fail. Security appears where the `revisor`'s risk appears: the address someone writes in a file, the outside data that gets validated, the native serialization that is not used with external data.

**Only what is already final in Java 25 is taught.** What is still in preview appears in a “what is coming” box, with the flag it requires, and it says not to use it in production. A course that teaches what has just come out gets old in six months.

**To keep reading on the Hábil blog:** [when it makes sense (and when it does not) to compile Java to native](https://www.habil.mx/en/blog/native-java-when-to-use-it-when-not/), [tests that catch errors](https://www.habil.mx/en/blog/tests-that-catch-errors-coverage-quality-gate/) and [API security](https://www.habil.mx/en/blog/api-security-cto-identity-gateway-tokens/). And if you are missing the foundation of another language, the [Python course](https://github.com/HabilMX/curso-python) or the [TypeScript](https://www.habil.mx/en/courses/typescript/) one.

## How you know you are done

The course does not end when you have read lesson 10, but when your `revisor` meets these six things:

1. **`mvn -B verify` ends green and the report says how many tests ran.** Count them from the report, not from the exit code: a green with zero tests looks the same as a real green.
2. **It checks 20 services at once in a time close to that of the slowest one**, not to the sum of all of them. You measure it yourself, with the clock, in series and in parallel.
3. **A service that is down, slow or answers garbage does not bring down the report:** it shows up in its row with its error, and the rest comes out complete and in the same order as always.
4. **No request can wait forever:** all of them have a connection timeout and a request timeout.
5. **It rejects addresses that are not `http` or `https` and those that are not on the allowed list**, instead of querying whatever is written in the file.
6. **It runs with `java -jar revisor.jar` from another folder and exits with code 0 if everything is up and 1 if not.**
