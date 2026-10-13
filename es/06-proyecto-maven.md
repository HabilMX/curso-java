# Lección 6 — Proyecto con Maven

**Tiempo:** 90 min (justo: incluye instalar Maven, y la primera construcción descarga piezas de internet; no la apures)

**Qué construyes:** el `revisor` deja de ser una carpeta de archivos sueltos y se convierte en un proyecto de Maven, con paquetes, que se compila, se empaqueta en un `.jar` y se ejecuta siempre con los mismos dos comandos

**Qué aprendes:** cuándo un archivo suelto deja de alcanzar; paquetes y visibilidad; el regreso de `public static void main(String[] args)`; instalar Maven 3.10.0 con SDKMAN; la estructura estándar de Maven y el `pom.xml`; el ciclo `compile`, `test`, `package`, `verify`; qué hay dentro de un `.jar` y por qué todavía no se ejecuta con `java -jar`

**De dónde vienes.** Traes el proyecto de la [Lección 5](05-errores-excepciones.md): una carpeta con varios archivos `.java` (el modelo, la clasificación, el formato, el reporte, la lectura de `servicios.txt` con sus excepciones propias), que se ejecuta con `java Revisor.java` y tarda unos 2.5 segundos porque la simulación espera lo que dice cada servicio. Funciona, y lo corres con un solo comando. Pero ese comando tiene límites que se van a notar en cuanto el programa pida algo que no escribiste tú: la próxima lección necesita una biblioteca de pruebas, y la siguiente, una para leer JSON. El lanzador de archivos fuente no sabe usar bibliotecas de otros, no guarda nada de lo que compila y no tiene una forma reconocible. Hoy conviertes el `revisor` en un proyecto de verdad: con la estructura que cualquier programador de Java reconoce y con una herramienta que lo compila, lo empaqueta y, desde la próxima lección, lo prueba. Trabajas en tu carpeta `~/curso-java/revisor/`.

**Qué no hace esta lección.** No escribe pruebas todavía: eso es la [Lección 7](07-pruebas-junit.md), que se apoya en lo que construyes hoy (el `pom.xml`, la carpeta de pruebas que hoy queda vacía y el ciclo de vida de Maven). No consulta ninguna red: el `Simulador` sigue haciendo el papel de los servicios y eso no cambia hasta la [Lección 8](08-http-y-json.md). Tampoco agrega bibliotecas al programa (la primera dependencia del programa llega en la 8) ni revisa los servicios a la vez (eso es la [Lección 9](09-concurrencia.md)). Lo que se resuelve aquí es **cómo se organiza, se compila y se empaqueta un programa de Java que ya tiene más de diez archivos**.

**Sobre las corridas.** Todo lo de esta lección se ejecutó con Temurin 25 y Maven 3.10.0, en macOS, con la terminal en UTF-8; la instalación de Maven con SDKMAN y su `mvn --version` se midieron además en una Linux Mint 22.3, el 13 de octubre de 2026. Las salidas de Maven son largas y cambian en cada corrida (descargas, tiempos, rutas), así que se muestran abreviadas: se conserva lo que cada sección analiza, y los tres puntos (`...`) marcan lo que se quitó; las rutas largas se muestran a partir de `~/curso-java`. Los mensajes del lanzador de Java (los que empiezan con `Error:`) salen en el idioma del sistema: aquí se muestran en inglés, y si tu Linux Mint está en español verás el primer renglón traducido; el resto no cambia. Los programas están en [`programas/06-proyecto-maven/`](../programas/06-proyecto-maven), en el repositorio del curso.

## Al terminar vas a poder

- Explicar qué tres cosas no hace el lanzador de archivos fuente y qué le agrega a tu programa un proyecto de Maven.
- Declarar un paquete, poner el archivo en la carpeta que le corresponde y decir qué clases ve cada una según su visibilidad.
- Instalar Maven 3.10.0 con SDKMAN y reconocer en `mvn --version` que usa tu Temurin 25.
- Leer un `pom.xml` y decir para qué sirve cada elemento, incluida la razón de fijar la versión de cada *plugin*.
- Decir qué fases ejecuta `mvn -B verify`, qué deja cada una en `target/` y qué hay dentro del `.jar`.
- Mudar el `revisor` a la estructura de Maven y ejecutarlo desde `target/classes` y desde su `.jar`.

## El porqué antes del cómo

Hasta la lección pasada, tu herramienta de construcción fue el propio `java`: le das un archivo `.java`, lo compila en la memoria junto con los demás de la carpeta y lo ejecuta. Para aprender es perfecto. Para un programa que va a crecer tiene tres límites, y los tres se arreglan con la misma herramienta:

1. **No puede usar bibliotecas de otros.** El lanzador de la [Lección 1](01-instalacion.md) (`java Revisor.java`) compila tus archivos y nada más. La próxima lección necesita una biblioteca de pruebas, y la Lección 8, una para leer JSON; sin una forma de declarar dependencias, no hay camino.
2. **No deja nada.** Compila en cada ejecución y no guarda el resultado: no existe un «programa compilado» que puedas entregar, ni una forma de decir «compila, pero todavía no ejecutes». Tampoco tiene dónde poner pruebas ni quién las corra.
3. **No tiene forma.** Cuando el programa tiene diez archivos, tres personas y dos años, «una carpeta con archivos» deja de ser una estructura. Todos los proyectos de Java del mundo comparten una convención de carpetas, y esa convención es lo que permite que una herramienta, o una persona nueva, los entienda sin que nadie se los explique.

La herramienta es **Maven**, que compila, prueba y empaqueta proyectos de Java a partir de un archivo que los describe. Se instala una vez, y desde ahí cada proyecto de Java que use Maven se construye con el mismo comando. Para usarla hay que dar dos pasos previos que la lección explica en orden: poner el código en **paquetes** (la forma que tiene Java de ordenar sus clases por nombre y por carpeta), y aprender qué clases ve cada una de las demás (la **visibilidad**).

La lección tiene cuatro partes: **paquetes y visibilidad** (6.1); **Maven**: instalarlo, su estructura, su `pom.xml`, su ciclo de vida y la mudanza del `revisor` (6.2); **el ciclo completo y el `.jar`** (6.3); y **el paso del proyecto** (6.4). Con ese destino en mente, esto es lo que vas a ver al final, cuando construyas el proyecto completo:

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

Trece archivos compilados, un `.jar` construido y un renglón que pasa desapercibido: `No tests to run.`, «no hay pruebas que correr». Al terminar sabrás de dónde sale cada renglón, qué quedó dentro del `.jar`, y por qué ese `BUILD SUCCESS` con cero pruebas es justo la pregunta con que empieza la Lección 7.

## Los conceptos

### 6.1 Paquetes y visibilidad

#### 6.1.1 Lo que el lanzador no hace

El lanzador de archivos fuente (`java Revisor.java`) es una comodidad que Java agregó para que aprender no exija una herramienta de construcción. Compila en memoria, en cada ejecución, los archivos que el programa necesita, corre el resultado y no deja nada en disco ([JEP 458](https://openjdk.org/jeps/458), final desde Java 22). Es perfecto para lo que hiciste hasta aquí y tiene tres límites que ahora duelen. El primero se puede provocar en dos líneas. Escribe un archivo que use JUnit, la biblioteca de pruebas que vas a usar en la Lección 7:

```java
// fig06_01.java
import org.junit.jupiter.api.Test;

void main() {
  IO.println("revisor: todavía no revisa nada");
}
```

Es el programa de siempre, con una línea de más: un `import` de una clase de JUnit. Un `import` (lo conoces de los archivos que usaron `java.util`) le dice al compilador de qué paquete viene un nombre. Ejecútalo:

```text
$ java fig06_01.java
fig06_01.java:2: error: package org.junit.jupiter.api does not exist
import org.junit.jupiter.api.Test;
                            ^
1 error
error: compilation failed
```

El compilador busca el paquete `org.junit.jupiter.api` y no lo encuentra, porque el lanzador no sabe de dónde sacarlo: no hay ningún lugar donde declarar que tu programa depende de una biblioteca. Cuando veas `package ... does not exist` con un nombre que no es tuyo, casi siempre significa eso: falta una dependencia, no un error de escritura.

El segundo límite no produce un mensaje, sino un hábito: como el lanzador compila todo en cada ejecución y no guarda nada, no existe el «compilado» como producto. No hay un `.class` que entregar, ni una forma de decir «compila, pero todavía no ejecutes». Con trece archivos el costo en tiempo es una fracción de segundo (medido: unos 0.3 segundos en esta máquina), pero el costo en orden crece con el proyecto. El tercer límite es el de las pruebas: el lanzador no sabe qué es una, ni dónde viven, ni cómo se corren todas juntas.

Antes de Maven hay una pieza más que Java exige y que tu proyecto todavía no tiene: los paquetes.

#### 6.1.2 Paquetes: un nombre y una carpeta

Imagina que dos personas escriben, cada una por su lado, una clase llamada `Servicio`. Si las dos acaban en el mismo programa, el compilador no puede saber cuál es cuál. El problema es antiguo y la solución de Java también: cada clase vive en un **paquete**, que es un nombre con puntos —`mx.habil.revisor`— y que forma parte del nombre completo de la clase. Tu `Servicio` pasa a llamarse `mx.habil.revisor.Servicio`, y el de la otra persona, `com.otraempresa.inventario.Servicio`. En el JDK pasa lo mismo: existe `java.util.List` y existe `java.awt.List`, dos cosas distintas con el mismo nombre corto.

La convención para elegir el nombre es usar el dominio de quien escribe el código al revés (`habil.mx` se vuelve `mx.habil`) seguido del nombre del proyecto (`revisor`). Un dominio es único en el mundo, así que el nombre también lo es. Las reglas del lenguaje son tres, y las tres están en la [especificación, capítulo 7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html):

1. **El archivo declara su paquete en la primera línea** (sin contar comentarios): `package mx.habil.revisor;`.
2. **La carpeta tiene el mismo nombre que el paquete**, con una subcarpeta por cada punto: el archivo vive en `mx/habil/revisor/`.
3. **Para usar una clase de otro paquete se importa**: `import mx.habil.revisor.Estado;`. Las clases del mismo paquete se ven entre sí sin `import`, y las de `java.lang` (como `String`) siempre.

Veámoslo con el programa más pequeño que se puede escribir dentro de un paquete. Es una clase, `Hola`, con un `main` que imprime el nombre completo de su propia clase:

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

Tres cosas son nuevas. La primera es el `package`. La segunda es que esta clase ya no es un archivo compacto: tiene su `class` y su `main` completo, `public static void main(String[] args)`, que la sección 6.1.4 explica. La tercera, `Hola.class.getName()`, pide a Java el nombre de la clase tal como la conoce; sirve aquí para ver que el paquete forma parte de él. (Los comentarios entre `/**` y `*/` son documentación; se explican en 6.1.3.)

Compilar y ejecutar un archivo con paquete ya no es `java Hola.java`. Se hace en dos pasos, y los dos tienen un detalle:

```text
$ cd fig06_02
$ javac -d out src/mx/habil/revisor/Hola.java
$ java -cp out mx.habil.revisor.Hola
hola desde mx.habil.revisor.Hola
```

`javac -d out` dice «deja los `.class` en la carpeta `out`», y `javac` hace algo que no se ve: **crea las subcarpetas del paquete** (`out/mx/habil/revisor/Hola.class`). Los `.class` quedan en una carpeta con la misma forma que la del código. Al ejecutar, `-cp out` es la abreviatura de *classpath*, «la ruta de clases»: la lista de lugares donde la JVM busca los `.class`. Con eso la JVM sabe dónde empezar a buscar, y `mx.habil.revisor.Hola` es el **nombre completo** de la clase, con su paquete: la JVM lo convierte en la ruta `mx/habil/revisor/Hola.class` y la busca dentro de `out`.

Ahora los dos errores que todo el mundo comete la primera vez. El primero es escribir solo el nombre corto:

```text
$ java -cp out Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola
```

La JVM busca `Hola.class` directamente dentro de `out`, no lo encuentra (está dos niveles más abajo) y lo dice con una excepción de nombre largo, `ClassNotFoundException`: «no hay ninguna clase llamada `Hola`». La solución es escribir el nombre completo. El segundo error es «entrar a la carpeta de la clase» y ejecutar desde ahí, pensando que si estás parado junto al archivo lo va a encontrar:

```text
$ cd out/mx/habil/revisor
$ java Hola
Error: Could not find or load main class Hola
Caused by: java.lang.NoClassDefFoundError: Hola (wrong name: mx/habil/revisor/Hola)
```

Aquí sí encuentra un `Hola.class`, pero su nombre verdadero es `mx/habil/revisor/Hola`, no `Hola`, y la JVM lo rechaza con `(wrong name: mx/habil/revisor/Hola)`: «el archivo dice que la clase se llama de otra forma». Es la misma regla vista desde el otro lado. **El nombre de la clase y la ruta de su archivo tienen que coincidir, y la JVM lo comprueba.** Por eso a los programas con paquetes se les ejecuta siempre desde la raíz de las clases (`out`, o `target/classes` en Maven), con el nombre completo.

Queda una pregunta que el compilador tiene derecho a hacerte: ¿qué pasa si el archivo declara un paquete y está en otra carpeta? La respuesta es menos dramática de lo que parece, y conviene saberla tal como es. Se movió `Clasificador.java` una carpeta arriba de donde le tocaba, dejando su línea `package mx.habil.revisor;` intacta, y se le pidió a `javac` que compilara `Revision.java` buscando las demás clases por carpeta:

```text
$ javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java
src/main/java/mx/habil/revisor/Revision.java:14: error: cannot find symbol
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
                                ^
  symbol:   variable Clasificador
  location: class Revision
1 error
```

El compilador sale a buscar `mx/habil/revisor/Clasificador.java` porque `Revision` lo menciona, no lo encuentra donde el nombre del paquete dice que debería estar, y concluye que no existe. Lo sorprendente es lo contrario: **cuando se le entregan todos los archivos explícitamente, `javac` no se queja**, y Maven lo hace así (medido: el proyecto compila y los `.class` quedan donde corresponde aunque el `.java` esté en la carpeta equivocada). La carpeta que miente no rompe la compilación de Maven; rompe todo lo que busca por carpeta: los editores, las búsquedas, el compilado incremental, y a la persona que llegue después de ti. Es un error silencioso, de los peores: la regla es que la carpeta diga siempre la verdad, aunque el compilador, hoy, te deje mentir.

Hay un último error de esta familia, el del archivo que no se llama como su clase pública. El archivo `Revisor.java` de este ejemplo declara una clase con otro nombre:

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

Una clase `public` debe vivir en un archivo con su mismo nombre. Es otra coincidencia que el compilador sí exige. En los archivos compactos de las primeras lecciones no se notaba, porque la clase implícita toma el nombre del archivo (`fig02_04.java` define una clase `fig02_04`).

#### 6.1.3 Visibilidad: qué ve cada clase de las demás

Con paquetes aparece una pregunta que antes no tenía sentido: ¿puede una clase de `mx.habil.otro` usar una de `mx.habil.revisor`? La respuesta la da la **visibilidad**, que se escribe con una palabra al principio de la declaración. Java tiene cuatro niveles, y el curso usa tres:

| Se escribe | Quién la ve | Para qué se usa |
|---|---|---|
| `private` | solo la clase donde se declara | los detalles internos: un campo, un constructor que nadie debe llamar |
| *(nada)* | cualquier clase del **mismo paquete** | casi todo el proyecto: es la visibilidad «de paquete» |
| `public` | cualquier clase de cualquier paquete | lo que se ofrece al mundo: el punto de entrada |

(El cuarto, `protected`, mezcla la visibilidad de paquete con la herencia entre clases; el curso no usa herencia, y no aparece.)

La regla es: **la visibilidad más estrecha que funcione**. No es una cuestión de estilo. Cada cosa `public` es una promesa a quien la use: si mañana la cambias o la quitas, rompes a esa persona. Una clase de paquete solo se la prometes a tu propio paquete, y la puedes cambiar sin avisarle a nadie. Hasta ahora todo era de paquete sin que lo notaras: tus clases no llevaban ninguna palabra delante. Ahora que hay más de un paquete en el mundo, el compilador lo hace cumplir. Dos archivos de ejemplo: un `Estado` de paquete en `mx.habil.revisor`, y una clase de otro paquete, `mx.habil.otro`, que lo quiere usar.

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

Al compilarlos juntos:

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

Eso es lo que pasa cuando una clase de `mx.habil.otro` intenta usar un `Estado` de visibilidad de paquete: `Estado is not public in mx.habil.revisor; cannot be accessed from outside package`. El mensaje es claro, y la solución casi nunca es «ponerle `public`»: es preguntarse si esa otra clase debería estar usando `Estado`.

En el `revisor`, **todo es de visibilidad de paquete salvo una clase: `Revisor`**, el punto de entrada, que es lo único que alguien de fuera necesita. Así queda, por ejemplo, el `Estado` de tu proyecto en su nueva casa:

```java
// fig06_03/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

Es el mismo `enum` de la lección 3, con una sola línea más: el `package` de arriba. No lleva ninguna palabra de visibilidad delante, así que es de paquete, y lo ven todas las clases de `mx.habil.revisor` y ninguna de fuera. En el único lugar donde sí aparece `public` (el `Revisor`, en 6.2.6) hace falta además un comentario de documentación, escrito entre `/**` y `*/`, que se llama **Javadoc**: es el comentario que las herramientas de Java (y tu editor) muestran cuando alguien pregunta «¿qué es esto?». La regla del proyecto desde esta lección es que toda clase y todo método `public` lleva un Javadoc de al menos una frase terminada en punto; en la [Lección 10](10-el-programa-terminado.md) una herramienta lo comprobará. Como casi todo es de paquete, ese Javadoc obligatorio queda en lo mínimo.

#### 6.1.4 `public static void main(String[] args)`: el regreso de la forma larga

En las lecciones 1 a 5 escribías `void main()` y nada más. Ahora el `Revisor` vuelve a la forma que has visto en cualquier código de Java ajeno: una clase, y dentro, un método `main` con tres palabras de más:

- `public`: la JVM lo llama desde fuera, así que tiene que ser visible para todos.
- `static`: pertenece a la clase, no a un objeto. Se puede llamar sin haber creado ninguno.
- `String[] args`: la lista de argumentos que la persona escribió después del nombre del programa en la línea de comandos (`java ... Revisor uno dos`). Hoy no se usa; las Lecciones 9 y 10 sí.

Es justo preguntarse por qué volver a una forma más larga si Java 25 admite `void main()`. La respuesta honesta tiene dos partes. La primera es que **la forma corta sigue funcionando dentro de un paquete**: se puede comprobar, y el programa corre. La segunda es que tiene una condición que el `Revisor` no cumple, y el error es instructivo. Una clase que solo guarda un `main` no tiene por qué dejar que alguien la instancie, así que se le pone un constructor `private`, que es lo que hace `private Revisor() {}`. Pero un `main` de instancia necesita que la JVM pueda crear el objeto. Esta clase, dentro de un paquete, tiene el `main` corto y el constructor privado:

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

`no non-private zero argument constructor found`: la JVM busca un constructor sin parámetros que no sea privado, y no lo hay. La forma clásica no depende de eso, funciona con cualquier versión del JDK desde la primera, y es la que reconocen todas las herramientas y todo el código ajeno que vas a leer. Para un programa que va a vivir años, ese es el motivo bueno: **no depender de una comodidad nueva si la forma vieja no cuesta nada.** `IO.println` también sigue funcionando en cualquier clase, pero el proyecto usa `System.out`, que ya conoces de la lección 5, porque es lo que vas a encontrar en todas partes.


### 6.2 Maven: instalarlo, su estructura y su `pom.xml`

#### 6.2.1 Qué hace Maven, en cuatro trabajos

Maven es una herramienta de construcción: recibe un proyecto de Java y hace con él, siempre en el mismo orden, lo que tú hacías a mano. Sus trabajos son cuatro. **Compilar** el código (llama a `javac` por ti, con las opciones correctas). **Correr las pruebas** y detenerse si alguna falla. **Empaquetar** el resultado en un archivo `.jar` (un archivo comprimido de clases, parecido a un `.zip`). Y **conseguir las dependencias**: cuando tu proyecto dice «necesito la biblioteca X versión Y», Maven la descarga de un repositorio público, [Maven Central](https://central.sonatype.com/), la guarda en tu computadora y la pone al alcance del compilador.

Lo que hace especial a Maven es una idea con nombre: **convención sobre configuración**. En lugar de que cada proyecto cuente en un archivo de configuración dónde está su código, dónde están sus pruebas y dónde deja los resultados, Maven fija una estructura de carpetas que usan todos, y el archivo del proyecto solo dice lo que se sale de lo habitual. La consecuencia es la que importa: quien conoce un proyecto de Maven conoce todos, y las herramientas (el editor, el servidor de integración continua, el analizador de código) saben dónde mirar sin que se les diga. La [introducción oficial](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) cuenta lo mismo en cinco minutos de lectura.

Maven no hace el trabajo él mismo. Es un orquestador: cada trabajo lo hace una pieza especializada llamada ***plugin*** (el compilador es uno, el que corre las pruebas es otro, el que arma el `.jar` es otro), y Maven decide cuándo llamar a cuál. Esa división tiene una consecuencia práctica que verás en el `pom.xml`: cada *plugin* tiene su propia versión.

#### 6.2.2 Instalar Maven 3.10.0 con SDKMAN

Maven se instala con la misma herramienta con que instalaste Java en la [Lección 1](01-instalacion.md): SDKMAN. Igual que con Java, trabaja en tu carpeta personal, sin `sudo` y sin tocar los programas del sistema, y te deja elegir la versión exacta. El curso fija **Maven 3.10.0**, que la [página de descarga de Maven](https://maven.apache.org/download.cgi) marca como la versión actual y recomendada para todos (pide JDK 8 o superior para ejecutarse; con tu Temurin 25 sobra). Fijar la versión importa por una razón que se ve en la pantalla: medido con Maven 3.9.11 sobre Java 25, al arrancar imprime cuatro avisos `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` (vienen de una biblioteca interna de Maven, no de tu código); la 3.10.0 no los imprime. Lo que se quiere es que tu salida sea la misma que la de esta lección.

Primero, comprueba que SDKMAN la ofrece. `sdk list maven` muestra las versiones disponibles; el 13 de octubre de 2026, en una Linux Mint 22.3, la lista traía la `3.10.0` (y también versiones `4.0.0-rc-…`, candidatas de la siguiente versión mayor que todavía no es definitiva y que el curso no usa). Después, instálala con su número completo:

```text
$ sdk install maven 3.10.0
...
Done installing!

Setting maven 3.10.0 as default.
```

A diferencia de Java, aquí el número sí lo escribes tú tal cual: `3.10.0` es la versión del curso, y no cambia aunque salgan otras. El último renglón dice que la deja como predeterminada. Si en lugar de eso SDKMAN te **pregunta** si quieres que sea la predeterminada, contesta que sí, con `Enter` o `Y`; y si contestaste que no, lo arreglas con `sdk default maven 3.10.0`. Ahora, como en la Lección 1, **abre una terminal nueva**, para que tome los cambios de SDKMAN, y comprueba:

```text
$ mvn --version
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
Maven home: ~/.sdkman/candidates/maven/current
Java version: 25.0.3, vendor: Eclipse Adoptium, runtime: ~/.sdkman/candidates/java/25.0.3.0+9-tem
...
OS name: "linux", ... arch: "amd64"
```

Esa es la salida medida el 13 de octubre de 2026 en la Mint 22.3 (abreviada: los tres puntos marcan lo que se quitó, y las rutas se acortaron con `~`; Maven las imprime completas, como `/home/<tu usuario>/.sdkman/candidates/maven/current`). En la tuya debes reconocer tres cosas. **`Apache Maven 3.10.0`** en el primer renglón: la versión del curso. **`Java version: 25.x`**, con el parche que tengas, y **`vendor: Eclipse Adoptium`**: Maven está usando tu Temurin 25 (Adoptium es el proyecto que publica Temurin). Y las dos rutas, `Maven home` y `runtime`, dentro de `~/.sdkman/candidates/`: las dos herramientas vienen de SDKMAN. Si `Java version` dice otra cosa que 25, Maven está usando otro JDK: o la terminal es vieja, o hay una variable `JAVA_HOME` que apunta a otro lado. SDKMAN mantiene esa variable al día cuando cambias de versión con `sdk default` o `sdk use` (la caja de la Lección 1, 1.1.7); revísala con `echo $JAVA_HOME`.

#### 6.2.3 La estructura estándar de carpetas

Con Maven instalado, falta darle un proyecto con la forma que espera. Esta es, y es igual en todos los proyectos de Maven del mundo:

```text
revisor/
  pom.xml                         ← la descripción del proyecto
  servicios.txt                   ← la configuración del revisor (sigue en la raíz)
  src/
    main/java/mx/habil/revisor/   ← el código del programa
    test/java/mx/habil/revisor/   ← el código de las pruebas
  target/                         ← lo que Maven genera; se puede borrar entero
```

Cada carpeta tiene un único papel. **`src/main/java`** es el código del programa, con una subcarpeta por cada punto del nombre del paquete, como en `out` hace un momento. **`src/test/java`** es el código de las pruebas, con **la misma estructura de paquetes** que el programa: la prueba de `Clasificador`, que escribirás en la Lección 7, será `mx.habil.revisor.ClasificadorTest`. Que las pruebas vivan en el mismo paquete no es una casualidad ni un adorno: es lo que les permite ver las clases de visibilidad de paquete que el programa tiene cerradas al mundo (6.1.3). Una prueba que tuviera que pedir `public` para todo obligaría a abrir el programa solo para poder probarlo.

**`target/`** es lo que Maven produce: los `.class` del programa en `target/classes`, los de las pruebas en `target/test-classes`, los informes de las pruebas y el `.jar`. Es una carpeta **desechable**: se puede borrar entera en cualquier momento y se regenera con un comando, porque todo lo que contiene sale de lo que está en `src`. De ahí dos reglas: nunca se edita nada dentro de `target`, y nunca se sube a un repositorio (si algún día guardas este proyecto con Git, agrega `target/` a su archivo `.gitignore`). `servicios.txt` se queda en la raíz porque el programa lo busca por una ruta relativa, desde la carpeta donde se ejecuta, y ejecutarás siempre desde la raíz del proyecto.

#### 6.2.4 El `pom.xml`, renglón por renglón

El archivo que describe el proyecto se llama `pom.xml` (*POM* son las siglas de «modelo de objetos del proyecto»; `.xml` es el formato). **XML** es un formato de texto hecho de etiquetas anidadas: cada elemento se abre con `<nombre>` y se cierra con `</nombre>`, y lo que va dentro es su valor o sus hijos. Es más ruidoso que el JSON de la Lección 8, pero es el que Maven eligió y no vas a escribirlo desde cero muy seguido; vas a leerlo, y por eso conviene entenderlo una vez completo. Este es el del proyecto, completo:

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

**Los datos del proyecto.** `<modelVersion>4.0.0</modelVersion>` es la versión del formato del propio `pom.xml`; es siempre `4.0.0` y se escribe sin pensar. Los tres que siguen, `groupId`, `artifactId` y `version`, son las **coordenadas** del proyecto, la dirección con la que cualquier proyecto de Maven del mundo puede referirse a este. El `groupId` es quien lo publica (`mx.habil`, igual que el nombre del paquete); el `artifactId` es el nombre del proyecto (`revisor`); y la `version` dice en qué punto está. Aquí es `0.6.0` porque este es el estado del curso en la Lección 6; cada lección lo subirá, y la 10 lo dejará en `1.0.0`. Las coordenadas parecen un trámite hasta que en la Lección 7 declares tu primera dependencia: una dependencia **es** un `groupId`, un `artifactId` y una `version`. Y deciden algo que verás en 6.3: el nombre del `.jar`.

**Las propiedades.** `<properties>` agrupa valores con nombre que otras partes del archivo (o de los *plugins*) leen. Dos son indispensables. `maven.compiler.release` en `25` le dice al compilador «compila como Java 25», lo que incluye una garantía que el viejo par `source`/`target` no daba: con `release`, el compilador no solo genera el formato de la versión pedida, sino que **rechaza cualquier clase del JDK que no existiera en esa versión**. Y sirve de aviso temprano; esto es lo que ocurre si alguien intenta construir el proyecto con un JDK 21 instalado:

```text
$ mvn -B compile
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile (default-compile) on project revisor: Fatal error compiling: error: release version 25 not supported -> [Help 1]
```

`release version 25 not supported`: el JDK que usa Maven no sabe compilar para Java 25. Es el error que ve quien tiene instalado el `default-jdk` de Mint (Java 21, [Lección 1](01-instalacion.md)) y quien sigue un tutorial con otra versión: en vez de fallar a medias con un error raro, el proyecto se niega desde el primer segundo y dice por qué. La otra propiedad, `project.build.sourceEncoding`, en `UTF-8`, declara la codificación de tus archivos `.java`; se escribe explícita para que el resultado no dependa de la computadora donde se compile, y para que los acentos de `"sin conexión"` sean siempre los mismos.

**Los *plugins*.** Dentro de `<build>`, `<plugins>` declara los tres que el proyecto necesita hoy: el compilador (`maven-compiler-plugin`), el que corre las pruebas (`maven-surefire-plugin`, que hoy no tiene pruebas que correr y que la Lección 7 pone a trabajar) y el que arma el `.jar` (`maven-jar-plugin`). Cada uno trae su `<version>` escrita, y es una decisión, no un descuido. Maven sí puede funcionar sin ella, porque trae una versión por omisión de cada *plugin* esencial; esto es lo que pasa si quitas la versión de uno:

```text
$ mvn -B verify
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for mx.habil:revisor:jar:0.6.0
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-surefire-plugin is missing.
```

Maven avisa que la versión falta y que «futuras versiones de Maven podrían dejar de aceptar proyectos así». Hay una razón más práctica: **si no fijas la versión, tu construcción cambia cuando cambia Maven**, no cuando tú decides. El mismo proyecto, el mismo comando, y un resultado distinto el día que alguien actualiza la herramienta. Fijar la versión de cada pieza es lo que hace que una construcción sea reproducible: la guía de Maven sobre [construcciones reproducibles](https://maven.apache.org/guides/mini/guide-reproducible-builds.html) trata de este problema y empieza por pedir que los *plugins* estén en versiones concretas y al día. Lo mismo valdrá para las dependencias.

Un detalle más antes de usarlo. El `pom.xml` de arriba no lleva la declaración `<?xml ... ?>` que aparece en casi todos los ejemplos de internet. Es opcional, y quitarla evita un error clásico: esa declaración tiene que ser lo primero del archivo, sin un comentario ni un espacio antes, y quien la escribe y luego agrega un comentario arriba obtiene un archivo inválido con un mensaje que no ayuda.

#### 6.2.5 El ciclo de vida

Maven no ejecuta *plugins* sueltos, ejecuta **fases**, y cada fase llama a los *plugins* que le corresponden. Las fases forman una cadena en orden fijo, el **ciclo de vida** ([documentación oficial](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)), y hay una regla simple que lo gobierna: **al pedir una fase, Maven ejecuta primero todas las anteriores.** Las que usarás en el curso son cuatro:

| Fase | Qué hace | Qué deja |
|---|---|---|
| `compile` | compila el código de `src/main/java` | `target/classes` |
| `test` | compila las pruebas y las corre (hasta la Lección 7 no hay ninguna) | `target/test-classes` y `target/surefire-reports` |
| `package` | arma el `.jar` con lo compilado | `target/revisor-0.6.0.jar` |
| `verify` | revisiones finales sobre el paquete (de momento, ninguna más) | — |

Por la regla de arriba, `mvn verify` compila, prueba y empaqueta; `mvn test` compila y prueba; y `mvn compile` solo compila. En el curso se usa `verify` como «la orden completa»: en la Lección 10 le colgarás las revisiones de calidad, y el comando que ya sabes no cambiará. Y siempre con `-B` (*batch*, «por lotes»): le dice a Maven que no use colores ni barras de progreso ni haga preguntas, que es la forma de pedirle una salida limpia que se pueda copiar, pegar en un mensaje o leer en un servidor sin pantalla.

#### 6.2.6 La mudanza del `revisor` a un proyecto

Es hora de hacer lo que el árbol de carpetas muestra. Son tres pasos y una mejora opcional, y ninguno cambia lo que el programa hace. Desde `~/curso-java/revisor/`:

```text
$ mkdir -p src/main/java/mx/habil/revisor src/test/java/mx/habil/revisor
$ mv *.java src/main/java/mx/habil/revisor/
```

`mkdir -p` crea una carpeta y todas las que falten en su ruta. `mv` mueve los archivos; ten cuidado de ejecutarlo desde `revisor/` y de que `servicios.txt` **no** se mueva (el `*.java` no lo toca). Ahora cada archivo necesita su línea `package`. Hacerlo a mano en trece archivos es un trabajo aburrido, así que este ciclo de la terminal lo hace por ti: para cada archivo, escribe la línea nueva y después el contenido original en un archivo temporal, y lo renombra encima del original:

```text
$ for f in src/main/java/mx/habil/revisor/*.java; do { printf 'package mx.habil.revisor;\n\n'; cat "$f"; } > "$f.nuevo" && mv "$f.nuevo" "$f"; done
```

Compruébalo abriendo cualquiera: la primera línea debe ser `package mx.habil.revisor;`, seguida de una línea en blanco. Quedan dos cosas que la terminal no puede hacer por ti, porque piden entender el código.

**La primera es el `Revisor`.** Tu `Revisor.java` de la lección 5 era un archivo compacto: un `main` sin clase alrededor. Ahora es una clase, la única pública del proyecto, con su constructor privado (6.1.3) y el `main` de la forma larga (6.1.4). Es corto, así que va completo:

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

Fíjate en lo que **no** cambió: el cuerpo del `try` es el de la lección 5, renglón por renglón. Lee la configuración, revisa los servicios en el orden de la lista, arma el reporte, imprime la tabla; y si la configuración está mal, el `catch` imprime el mensaje en español por la salida de error. Lo único nuevo es la envoltura, y tres cosas que cambian de significado: el archivo compacto veía todo el módulo `java.base` sin pedirlo, y una clase normal necesita sus propios `import` (por eso aparecen `Path` y `List`); el `main` es ahora el que la JVM llama (y por eso debe ser `public static`); y las clases que antes veía «porque estaban en la misma carpeta» ahora las ve porque están en el mismo paquete.

**La segunda es una posibilidad que el paquete abre, y es opcional.** En las lecciones 3 a 5, sin paquete, cada vez que hacía falta `Respondio` había que escribirlo como `Respuesta.Respondio`, porque **de un paquete sin nombre no se puede importar nada** (si lo intentas, el compilador dice `package R does not exist`). Con paquete, el `import mx.habil.revisor.Respuesta.Respondio;` sí funciona, y el código puede decir `Respondio` a secas. Las pruebas que escribirás en la Lección 7 lo usan; el código del programa, tal como lo traes de la lección 5, sigue funcionando sin tocarlo, y puedes dejarlo así.

#### 6.2.7 La primera construcción

Con el proyecto mudado, esta es la primera construcción. Desde la raíz (`~/curso-java/revisor/`, donde está el `pom.xml`), y con el `pom.xml` de arriba, que todavía no tiene pruebas:

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

Léelo de arriba abajo, porque cada renglón `--- plugin:versión:objetivo ---` te dice qué pieza trabajó. `resources:resources` copia archivos que no son código (aquí no hay: «skip non existing resourceDirectory»). `compiler:compile` hace el trabajo: `Compiling 13 source files with javac [debug release 25] to target/classes` dice cuántos archivos compiló, con qué opciones (`debug` significa que guarda los nombres de las variables, lo que explica el siguiente párrafo) y adónde dejó los `.class`. Y `BUILD SUCCESS` es el aviso de que ninguna fase falló. La primera vez que corras Maven verás además muchos renglones `Downloading` que aquí se quitaron: Maven descarga sus propios *plugins* de Maven Central y los guarda en `~/.m2/repository`, una carpeta oculta de tu casa que funciona como **caché local**. Eso explica dos cosas: la primera corrida es lenta y necesita internet, y las siguientes son rápidas y funcionan sin conexión.

Con las clases en `target/classes`, el programa se ejecuta con el mismo `-cp` de hace un rato, ahora con la raíz de las clases de Maven:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Es la tabla de siempre, con el mismo resumen: `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`. Ese número lo calcula el programa cada vez, y las pruebas de la Lección 7 lo van a vigilar.

El `debug` del renglón de `Compiling` tiene un premio escondido que conecta con la [Lección 2](02-tipos-cadenas-control.md): el mensaje de `NullPointerException` que con `java Revisor.java` decía `"<local1>"` (el lanzador compila sin esa información) ahora dice el nombre de la variable. Con una clase de prueba, compilada por Maven, que hace `detalle.isBlank()` sobre un `null`:

```text
$ java -cp target/classes mx.habil.revisor.Nulo
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.isBlank()" because "detalle" is null
	at mx.habil.revisor.Nulo.main(Nulo.java:6)
```

`because "detalle" is null`: el nombre de la variable que causó el error. Maven compila con información de depuración por omisión, y por eso los mensajes de los programas del proyecto son más fáciles de leer que los de los archivos sueltos.

#### 6.2.8 Tres comprobaciones antes de seguir

Si llegaste aquí, el proyecto ya compila y se ejecuta. Comprueba tres cosas, en este orden:

1. `mvn -B compile` termina en `BUILD SUCCESS` y dice `Compiling 13 source files`.
2. `java -cp target/classes mx.habil.revisor.Revisor` imprime la tabla de cuatro servicios con el resumen de 843 ms.
3. Ejecutado desde otra carpeta, el mismo comando no encuentra `servicios.txt`: es el comportamiento correcto, y lo vas a ver en «El error que vas a ver».

Si alguna falla, el error es casi siempre de las tres mismas familias: un archivo sin su línea `package` (el compilador dice que no encuentra una clase), un archivo en la carpeta equivocada (6.1.2), o un comando ejecutado fuera de la raíz del proyecto, donde no hay `pom.xml`:

```text
$ cd ~/curso-java
$ mvn -B verify
[ERROR] BUILD FAILURE
[ERROR] The goal you specified requires a project to execute but there is no POM in this directory (~/curso-java). Please verify you invoked Maven from the correct directory. -> [Help 1]
```

Maven lo dice con todas sus letras: no hay POM en la carpeta donde estás. Lo que falta es `cd` hasta la raíz del proyecto.


### 6.3 El ciclo completo y el `.jar`

#### 6.3.1 Lo que hace `mvn -B verify` hoy

Hasta aquí usaste `compile`, la primera fase. Ahora pide la última, la que vas a escribir el resto del curso. Desde la raíz del proyecto:

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

Léelo con la tabla de 6.2.5 al lado, porque cada bloque `--- plugin:versión:objetivo ---` es una fase trabajando, en el orden de la cadena. Primero, la fase `compile`, igual que en 6.2.7: trece archivos a `target/classes`. Después vienen tres renglones que son de la fase `test` y que todavía no tienen trabajo. `testResources` busca archivos auxiliares para las pruebas en `src/test/resources` y no los hay. `testCompile` es el mismo compilador, pero aplicado a `src/test/java`, y dice `No sources to compile`: la carpeta de pruebas que creaste en 6.2.6 existe y está vacía. Y `surefire:test`, el *plugin* que corre las pruebas, dice `No tests to run.`, porque no hay ninguna. Por último, la fase `package` llama a `jar:jar`, que arma el archivo `target/revisor-0.6.0.jar`. La fase `verify` no agrega nada todavía (la Lección 10 le colgará las revisiones de calidad), y Maven cierra con `BUILD SUCCESS`.

Dos detalles merecen una segunda mirada. El primero es el nombre del `.jar`: `revisor-0.6.0.jar` no lo escribiste en ninguna parte, Maven lo arma con el `artifactId` y la `version` del `pom.xml`. Cuando la versión suba, el nombre del archivo subirá con ella; por eso las coordenadas no son un trámite. El segundo es el renglón de *Surefire*. Fíjate en lo que pasó: Maven no corrió ni una sola prueba y terminó en verde. Hoy eso es la verdad, porque no escribiste pruebas, y el informe lo dice con todas sus letras. En la Lección 7 vas a ver que un verde sin pruebas también puede aparecer cuando **sí** escribiste pruebas, sin ese aviso, y por qué el número de pruebas se lee siempre del informe y nunca del `BUILD SUCCESS`. Guarda esta imagen: `No tests to run.` es lo que dice Maven cuando sabe que no hay pruebas.

#### 6.3.2 Qué hay dentro del `.jar`

Un archivo `.jar` (de *Java ARchive*) es un archivo comprimido con el mismo formato que un `.zip`, que guarda clases compiladas y un poco de información sobre ellas. El JDK trae una herramienta para manejarlo, que se llama igual, `jar`, y su opción `tf` lista el contenido (`t` es «lista», `f` es «del archivo que te digo»):

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

Hay tres grupos. El más grande son tus clases, y vienen **con las carpetas de su paquete**: `mx/habil/revisor/Clasificador.class`, exactamente la forma que `javac -d out` produjo en 6.1.2 y que la JVM exige para encontrar una clase por su nombre completo. Un `.jar` es, en el fondo, una carpeta `target/classes` metida en un solo archivo. Cuenta las clases: son quince, de trece archivos fuente. Las dos de más son `Respuesta$Respondio.class` y `Respuesta$SinRespuesta.class`: los dos *records* que viven **dentro** de la interfaz `Respuesta` (Lección 3). Cada clase o *record* anidado se compila a su propio `.class`, con el nombre de la clase que lo contiene, un `$` y su nombre.

El segundo grupo es `META-INF/MANIFEST.MF`, el **manifiesto**: un archivo de texto con datos sobre el `.jar` que lee la JVM. El tercero, `META-INF/maven/...`, es una copia del `pom.xml` y un archivo con las coordenadas; lo agrega Maven para que quien encuentre el `.jar` suelto sepa de qué proyecto salió y en qué versión. Lo que **no** está también importa: ni un solo `.java` (el `.jar` lleva lo compilado, no la fuente), ni `servicios.txt` (está en la raíz del proyecto, no en `src/main/resources`, así que no es parte del programa), ni nada de JUnit ni de pruebas.

#### 6.3.3 Ejecutar desde el `.jar`, y lo que todavía falta

Como el `.jar` contiene lo mismo que `target/classes`, sirve igual como *classpath*:

```text
$ java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

La misma tabla, ahora desde un solo archivo que podrías copiar a otra computadora con Java 25. Existe una forma más corta, `java -jar archivo.jar`, que es la que usan los programas de Java que se entregan a otras personas. Pruébala:

```text
$ java -jar target/revisor-0.6.0.jar
no main manifest attribute, in target/revisor-0.6.0.jar
```

«No hay atributo principal en el manifiesto». Con `-jar`, la JVM no recibe el nombre de la clase que debe arrancar: lo busca en el manifiesto, en un renglón llamado `Main-Class`. Y el manifiesto que armó Maven no lo trae. Puedes verlo, porque `unzip -p` imprime un archivo de dentro del `.jar` sin extraerlo:

```text
$ unzip -p target/revisor-0.6.0.jar META-INF/MANIFEST.MF
Manifest-Version: 1.0
Created-By: Maven JAR Plugin 3.5.1
Java-Version: 25
Build-Jdk-Spec: 25
```

Cuatro renglones: la versión del formato, quién lo creó, y con qué Java. Nada dice cuál es la clase de entrada. (Si tu JVM está en español, el mensaje de error sale traducido; medido con la JVM en español de México: `no hay ningún atributo de manifiesto principal en target/revisor-0.6.0.jar`. El código de salida es 1 en los dos idiomas.) Se podría agregar ese renglón con la configuración del *plugin* `jar`, pero no resolvería el problema de fondo, que aparece en la Lección 8: cuando el programa use una biblioteca, el `.jar` tendrá tus clases y no las de la biblioteca, y la JVM no las encontrará. La [Lección 10](10-el-programa-terminado.md) arregla las dos cosas a la vez con un `.jar` que lleva todo dentro y sabe por dónde empezar. Mientras tanto, el comando del curso es el de `-cp`, que funciona con `target/classes` y con el `.jar` por igual.

#### 6.3.4 `target/` se puede tirar

Ya tienes a la vista todo lo que vive en `target/`: `classes` con los `.class`, el `.jar`, y unas carpetas de trabajo de Maven (`maven-status`, `maven-archiver`, `generated-sources`) que guardan datos para la siguiente corrida. **Todo eso sale de `src/` y del `pom.xml`**, y por eso se puede borrar entero en cualquier momento: la siguiente `mvn -B verify` lo vuelve a crear igual. Esa es la prueba de que un proyecto está bien armado. Si alguna vez borras `target/` y el proyecto ya no construye, había algo importante donde no debía estar.

De ahí salen las dos reglas que ya viste en 6.2.3, que conviene repetir porque son las que más se rompen. **Nunca se edita nada dentro de `target/`**: un cambio ahí se pierde en la siguiente construcción, y lo peor es que mientras tanto parece que funcionó. Y **nunca se guarda `target/` en un repositorio**: guardarías el resultado y no la fuente, y cada persona que construya el proyecto vería cambios en archivos que no tocó. Lo que se guarda es `src/`, `pom.xml` y lo que el programa necesita para correr, como `servicios.txt`.


### 6.4 El paso del proyecto: el `revisor` como proyecto de Maven

Tienes todas las piezas. Este es el proyecto completo, tal como queda al terminar la lección. La mayor parte ya la conoces: del programa, solo el `Revisor` (6.2.6) cambió de verdad respecto a la Lección 5, y las demás clases solo ganaron su línea `package`. Se incluyen todas para que el proyecto se pueda armar sin mirar otra lección:

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

Ya viste completos el `pom.xml` (6.2.4), `Revisor.java` (6.2.6) y `Estado.java` (6.1.3). Tu `servicios.txt` es el de siempre:

```text fig06_03/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

Y estos son los once archivos restantes. Si has seguido las lecciones 3 a 5, ya los tienes, y la única diferencia es lo que se dijo en 6.2.6; los incluyo para que el proyecto de esta lección se pueda construir sin depender de nada más.

<details>
<summary>Los once archivos del programa que solo cambian en su primera línea</summary>

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

Para terminar la lección: `mvn -B verify` debe decir `Compiling 13 source files`, `No tests to run.` y `BUILD SUCCESS`, y dejar `target/revisor-0.6.0.jar`; `java -cp target/classes mx.habil.revisor.Revisor` y `java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor` deben imprimir los dos la tabla con resumen de la Lección 4 (4.5) de 843 ms. Hay un artículo de Hábil sobre [cómo se arma la integración continua de una casa de software](https://www.habil.mx/es/blog/cicd-devsecops-infraestructura-propia/), el lugar donde `mvn -B verify` se ejecuta solo en cada cambio; vale la pena leerlo ahora que el comando ya es tuyo.


## El error que vas a ver

La lección tiene un error principal, el de la clase que la JVM no encuentra, y varios mensajes de apoyo que conviene saber leer de una mirada.

**El error principal.** Es el de 6.1.2, y lo vas a ver en cuanto ejecutes un programa con paquete de la forma equivocada:

```text
$ java -cp out Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola
```

Se lee en tres pasos, siempre los mismos. **Qué buscaba**: el nombre después de `class`, aquí `Hola` a secas. **Dónde lo buscó**: en el *classpath* que le diste con `-cp`, aquí `out`. **Qué encontró**: nada (`ClassNotFoundException`), o un archivo con otro nombre verdadero (`NoClassDefFoundError ... wrong name: mx/habil/revisor/Hola`). Con esas tres respuestas el arreglo es siempre uno de dos: escribir el nombre completo de la clase, con su paquete, o ejecutar desde la raíz de las clases (`out`, `target/classes` o el `.jar`). En el `revisor`, la forma correcta es una sola y no cambia en el resto de la lección: `java -cp target/classes mx.habil.revisor.Revisor`, desde la raíz del proyecto.

**Los mensajes de apoyo**, para reconocerlos de una mirada:

- `package org.junit.jupiter.api does not exist` (`fig06_01.java`): falta una dependencia. El lanzador no sabe de dependencias; en Maven se declaran en el `pom.xml`, y la Lección 7 declara justamente esa.
- `cannot find symbol` con `symbol: variable Clasificador`, al compilar buscando por carpeta: un archivo está en una carpeta que no corresponde a su paquete. La carpeta tiene que decir la verdad (6.1.2).
- `Estado is not public in mx.habil.revisor; cannot be accessed from outside package`: pediste una clase de visibilidad de paquete desde otro paquete. Antes de ponerle `public`, pregúntate si esa clase debería estar usándola.
- `no non-private zero argument constructor found`: un `main` de instancia en una clase con constructor privado. La forma larga `public static void main(String[] args)` no tiene ese problema (6.1.4).
- `The goal you specified requires a project to execute but there is no POM in this directory`: ejecutaste `mvn` fuera de la raíz del proyecto. Entra a la carpeta que contiene el `pom.xml`.
- `release version 25 not supported`: Maven usa un JDK más viejo que el 25. Revisa el renglón `Java version` de `mvn --version`, la variable `JAVA_HOME` y, si hay varios JDK, la caja de la Lección 1 sobre `sdk default`.
- `no main manifest attribute`: intentaste `java -jar` con un `.jar` que no dice cuál es su clase de entrada. Usa `java -cp` con el nombre de la clase; la Lección 10 lo resuelve.
- `error de configuración: no existe el archivo servicios.txt` al ejecutar desde otra carpeta, que es el último y el que más se repite:

```text
$ cd ..
$ java -cp revisor/target/classes mx.habil.revisor.Revisor
error de configuración: no existe el archivo servicios.txt
```

El programa busca `servicios.txt` por una ruta **relativa**, `Path.of("servicios.txt")`, y una ruta relativa significa «el archivo de la carpeta donde estoy parado», no «el que está junto al programa». Desde la carpeta de arriba no existe. Fíjate en lo que hizo bien el programa: no mostró una excepción de treinta renglones ni un `NoSuchFileException`, sino el mensaje en español de la Lección 5. Para saber en qué carpeta estás, el comando `pwd` te lo dice. En la Lección 9 el primer argumento de la línea de comandos te dejará indicar el archivo sin depender de dónde estés.

## Lo que se hace mal

**Instalar Maven por un camino distinto del de Java.** Si Java viene de SDKMAN y Maven de otro lado (un paquete del sistema, un archivo descomprimido a mano), cada uno trae sus propias ideas de qué JDK usar, y el día que no coinciden el error no dice por qué. Con las dos herramientas en SDKMAN, `mvn --version` y `java --version` hablan del mismo Temurin 25.

**Ponerle `public` a todo «para que compile».** Cada cosa pública es una promesa a quien la use, y cada promesa es un costo cuando quieras cambiarla. El error `is not public` casi siempre significa que algo está en el lugar equivocado, no que falte una palabra.

**Una carpeta que no coincide con el paquete, «porque compila».** Maven lo compila (6.1.2), pero rompe todo lo que busca por carpeta: el editor, las búsquedas, la compilación incremental y a la siguiente persona. Es un error que no avisa, y por eso se evita por costumbre y no por el mensaje.

**Dependencias y *plugins* sin versión, o con la más reciente.** Hace que tu construcción cambie el día que alguien publica una versión nueva o actualiza Maven, sin que hayas tocado nada. Cada pieza lleva su versión escrita, y se sube a propósito.

**Editar o guardar `target/`.** Es lo que Maven genera y se puede borrar y regenerar en cualquier momento (6.3.4). Un cambio hecho ahí se pierde en la siguiente construcción, y si lo subes a un repositorio, estás guardando el resultado y no la fuente.

**Ejecutar desde cualquier carpeta y culpar al programa.** Maven necesita estar en la carpeta del `pom.xml`, y el `revisor` necesita estar en la carpeta de `servicios.txt`: hoy son la misma, la raíz del proyecto. Antes de leer un error de «no existe», lee `pwd`.

## Ejercicios

### Ejercicio 1 — La carpeta que miente

Mueve `Clasificador.java` una carpeta arriba de la que le toca, a `src/main/java/mx/habil/`, sin tocar su línea `package mx.habil.revisor;`. **Antes de ejecutar nada, predice** qué hace cada uno de estos dos comandos: `javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java` (que compila `Revision` buscando las demás clases por carpeta) y `mvn -B compile`. Ejecútalos y compara. ¿Dónde quedó `Clasificador.class` dentro de `target/classes`? Regresa el archivo a su carpeta y borra la carpeta `out` que creó `javac`.

### Ejercicio 2 — Lo que Maven decide por ti

Quita del `pom.xml` el renglón `<version>3.5.1</version>` del *plugin* `maven-jar-plugin` y corre `mvn -B verify`. Lee los renglones `WARNING` del principio: ¿qué dicen que falta, y dónde? Busca después el renglón `--- jar:...` de la salida: ¿qué versión del *plugin* usó Maven? Con esa respuesta, explica en tu bitácora por qué el `pom.xml` fija la versión aunque hoy dé lo mismo. Restaura el renglón al terminar.

### Ejercicio 3 — El JDK equivocado

Si tienes un JDK 21 instalado con SDKMAN además del 25 (los JDK de SDKMAN viven en `~/.sdkman/candidates/java/`, una carpeta por identificador; `ls ~/.sdkman/candidates/java/` te los muestra), apunta Maven a él solo para un comando, poniendo la variable delante: `JAVA_HOME=~/.sdkman/candidates/java/<el identificador del 21> mvn -B compile`. **Antes, predice** en qué fase falla y con qué mensaje. Si no tienes un 21, no lo instales para esto: lee la solución. En los dos casos, contesta: ¿qué propiedad del `pom.xml` produce el error, y por qué es mejor que falle ahí y no más tarde?

### Ejercicio 4 — Qué hay en tu `.jar`

Corre `jar tf target/revisor-0.6.0.jar` y cuenta los archivos que terminan en `.class`. **Antes de contar, predice** cuántos debería haber, sabiendo que el proyecto tiene trece archivos `.java`. Si el número no coincide con tu predicción, encuentra los que sobran y explica de dónde salen. Luego intenta `java -jar target/revisor-0.6.0.jar` y explica el mensaje con lo que viste en el manifiesto.

## Soluciones

### Solución 1

Con `Clasificador.java` una carpeta arriba, los dos comandos se portan distinto:

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

`javac` con `-sourcepath` busca cada clase que necesita **por carpeta**: para `mx.habil.revisor.Clasificador` mira en `src/main/java/mx/habil/revisor/Clasificador.java`, no lo encuentra y concluye que no existe. Maven, en cambio, le entrega a `javac` la lista completa de archivos de `src/main/java`, encuentra los trece y compila sin quejarse. Y `Clasificador.class` queda en `target/classes/mx/habil/revisor/`, **donde dice su paquete**, no donde estaba el `.java`: el compilador acomoda la salida según la línea `package`. Por eso el error no se ve en Maven y sí en cualquier herramienta que busque por carpeta. El programa corre igual; el proyecto, en cambio, ya miente.

### Solución 2

Sin la versión, Maven construye, pero avisa al principio:

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

Dice qué falta (la versión del *plugin* `maven-jar-plugin`) y dónde (`@ line 25, column 15`, el renglón del `pom.xml` donde empieza ese *plugin*; si tu archivo tiene otra forma, el número cambia). Y la respuesta a la segunda pregunta es la que hace interesante el ejercicio: Maven usó **la misma 3.5.1** que fijaba tu `pom.xml`. Hoy, con Maven 3.10.0, la versión por omisión coincide con la del curso. Pero eso es una casualidad de esta versión de Maven, no una propiedad de tu proyecto: la versión por omisión la decide quien publica Maven, y cambia cuando cambia Maven. Fijarla es la diferencia entre «hoy da lo mismo» y «siempre da lo mismo». La frase para la bitácora: **la versión se fija para que el resultado no dependa de qué Maven tenga instalado cada quien.**

### Solución 3

Con un JDK 21, Maven arranca, llega a la fase `compile` y se detiene ahí:

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

(Medido con un JDK 21 de otra distribución; el mensaje es del compilador y no depende de ella.) La propiedad responsable es `maven.compiler.release` en `25`: el compilador del JDK 21 no sabe producir clases para Java 25 y lo dice antes de compilar un solo archivo. Es mejor que falle ahí porque el mensaje nombra la causa (la versión) en lugar de un síntoma (un método o una sintaxis que «no existe»), y porque el error aparece en la primera fase, antes de que alguien pierda tiempo con pruebas o paquetes a medias. Nota también lo que demuestra el ejercicio: Maven **no** usa necesariamente el `java` de tu terminal, sino el de `JAVA_HOME` si está definida. Por eso, cuando Maven y `java --version` no coinciden, lo primero es mirar esa variable.

### Solución 4

Hay **quince** archivos `.class`, no trece:

```text
$ jar tf target/revisor-0.6.0.jar
...
mx/habil/revisor/Respuesta$Respondio.class
mx/habil/revisor/Respuesta$SinRespuesta.class
mx/habil/revisor/Respuesta.class
...
```

Los dos de más son los *records* anidados en la interfaz `Respuesta`: cada clase, *record* o `enum` se compila a su propio `.class`, y si vive dentro de otra, su nombre lleva delante el de la que la contiene y un `$`. Un archivo `.java` puede producir varios `.class`, y la relación «un archivo, una clase» solo es obligatoria para las clases `public` (6.1.2). Y `java -jar` falla con `no main manifest attribute` porque el manifiesto que viste en 6.3.3 no tiene el renglón `Main-Class`: la JVM, con `-jar`, no sabe por qué clase empezar. Con `java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor` se lo dices tú, y funciona.

## Cómo sé que lo logré

- [ ] `mvn --version` dice `Apache Maven 3.10.0` y una versión de Java que empieza con 25.
- [ ] `mvn -B verify`, desde la raíz de `~/curso-java/revisor/`, dice `Compiling 13 source files`, `No tests to run.` y `BUILD SUCCESS`, y deja `target/revisor-0.6.0.jar`.
- [ ] `java -cp target/classes mx.habil.revisor.Revisor` imprime la tabla de cuatro servicios con el resumen `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`, y lo mismo con el `.jar` en lugar de `target/classes`.
- [ ] Desde la carpeta de arriba, el mismo comando con `revisor/target/classes` imprime `error de configuración: no existe el archivo servicios.txt`, y puedes explicar por qué.
- [ ] Puedes explicar qué pasa si un archivo declara `package mx.habil.revisor;` y está en otra carpeta, y por qué Maven te deja hacerlo pero no es una buena idea.
- [ ] Puedes explicar por qué hay quince `.class` en el `.jar` y por qué `java -jar` todavía no funciona.
- [ ] Anotaste en tu bitácora qué mensaje de esta lección te costó más leer.

## Resumen

Responde sin mirar la lección:

1. ¿Qué tres cosas no puede hacer el lanzador de archivos fuente que sí hace un proyecto de Maven?
2. ¿Qué relación tiene que haber entre el nombre de un paquete, la línea `package` y la carpeta del archivo? ¿Quién lo comprueba, y quién no?
3. ¿Qué ve una clase de visibilidad de paquete, y qué no? ¿Qué clase del `revisor` es `public`, y por qué solo esa?
4. ¿Por qué el `Revisor` usa `public static void main(String[] args)` y no `void main()`?
5. ¿Con qué instalas Maven y qué tres cosas reconoces en `mvn --version` para saber que quedó bien?
6. ¿Qué son las coordenadas de un proyecto de Maven? ¿Para qué sirve fijar la versión de cada *plugin*?
7. ¿Qué fases ejecuta `mvn verify` y en qué orden? ¿Qué deja cada una en `target/`?
8. ¿Qué hay dentro de un `.jar`, qué no hay, y por qué `java -jar` todavía no lo ejecuta?

## Para leer más

- [Maven, «Introduction to the Build Lifecycle»](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) y [«Introduction to the POM»](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html): las fases, qué *plugin* corre en cada una, y todo lo que puede llevar un `pom.xml`. En inglés, como casi toda la documentación oficial. Consultado el 7 de octubre de 2026.
- [La especificación del lenguaje, capítulo 7: paquetes y módulos](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html) y [el tutorial de Oracle sobre paquetes](https://docs.oracle.com/javase/tutorial/java/package/index.html): las reglas exactas de nombres, carpetas e importaciones. Consultado el 7 de octubre de 2026.
- [Oracle, «JAR File Specification» (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html) y [el manual del comando `jar`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jar.html): qué lleva un `.jar`, qué es el manifiesto y para qué sirve `Main-Class`. Consultado el 13 de octubre de 2026.
- [SDKMAN, uso](https://sdkman.io/usage/): `list`, `install`, `use` y `default`, los mismos comandos para Java y para Maven. Consultado el 13 de octubre de 2026.

---

**Anterior:** [Lección 5 — Errores y excepciones](05-errores-excepciones.md) ·
**Siguiente:** [Lección 7 — Pruebas con JUnit](07-pruebas-junit.md)
