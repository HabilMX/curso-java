# Lección 1 — Instalar Java 25 en tu Linux Mint

**Tiempo:** 90 min (justo: la instalación depende de tu conexión, y si algo falla en ella, el resto de la lección se recorta; no la apures)

**Qué construyes:** el entorno de trabajo (un JDK 25 funcionando) y tu primer programa; dejas creada la carpeta `~/curso-java/` con el archivo `Hola.java` y el primer archivo del proyecto, `revisor/Revisor.java`

**Qué aprendes:** instalar SDKMAN y, con él, Temurin 25, y comprobar la versión en una terminal nueva con `java --version` y `javac --version`; por qué no se usa `default-jdk`; el archivo compacto de Java 25 (`void main()` e `IO.println`); `java Hola.java`; compilar con `javac` y ejecutar con `java` por separado, y qué es el `.class`; probar ideas con `jshell`; y leer el primer error del compilador

**De dónde vienes.** De la [Lección 0](00-que-es-java.md), donde viste el camino del código: fuente, `javac`, *bytecode* y la JVM; la diferencia entre JDK y JRE; y qué es LTS. Esta lección toma esa teoría y la convierte en comandos. Trabajas en un Linux Mint 22 (cualquier 22.x: la 22.3, «Zena», es la vigente según la [página de versiones de Linux Mint](https://linuxmint.com/download_all.php), con soporte hasta abril de 2029), que está basado en Ubuntu «Noble».

**Qué no hace esta lección.** No enseña la sintaxis de Java más allá de lo que hace falta para el primer programa: tipos, cadenas y control de flujo son de la [Lección 2](02-tipos-cadenas-control.md). No instala Maven (la [Lección 6](06-proyecto-maven.md) lo hace cuando hace falta). Y no usa un entorno de desarrollo gráfico: todo se hace con un editor de texto y una terminal, porque así se ve qué hace cada pieza, y porque eso es lo que tendrás en un servidor.

**Sobre las corridas y lo que no pudimos comprobar.** Los programas de esta lección y sus mensajes de error se ejecutaron con Temurin 25.0.4 en macOS y se compararon carácter por carácter con lo que aquí se muestra; los que muestran Java 21 y Java 17 se obtuvieron con Temurin 21.0.12 y 17.0.20. En tu Linux Mint deberían dar lo mismo, porque son mensajes del compilador y de la JVM y no del sistema, pero esos programas no se corrieron en Mint. **Lo que sí depende de tu Linux Mint** es la instalación, y esa se midió el 13 de octubre de 2026 en una Linux Mint 22.3 real: qué responde la terminal cuando no hay Java, qué imprime el instalador de SDKMAN, qué identificadores de Temurin ofrecía ese día, qué dice `java --version` después, y la trampa de la terminal que no se entera del cambio. Si algo de lo que ves en tu pantalla difiere de lo que aquí se muestra en esos puntos, anótalo en tu [bitácora](bitacora.md): es información útil. Los programas están en [`programas/01-instalacion/`](../programas/01-instalacion), en el repositorio del curso.

## Al terminar vas a poder

- Instalar SDKMAN y, con él, Temurin 25 eligiendo el identificador correcto de `sdk list java`, y comprobar en una terminal nueva, con `java --version` y `javac --version`, que son los de la versión 25.
- Explicar por qué `sudo apt install default-jdk` te da Java 21 en Mint 22, y qué error produce eso con el archivo compacto.
- Escribir, ejecutar y modificar un programa de Java de un solo archivo con `void main()` e `IO.println`, y correrlo con `java Hola.java`.
- Compilar con `javac`, ejecutar con `java`, y decir qué es el archivo `.class` que apareció y cuándo hay que volver a compilar.
- Probar expresiones sueltas en `jshell` y salir de él.
- Leer un error del compilador: el archivo, el renglón, la flecha `^`, y los renglones `symbol` y `location`.
- Crear el primer archivo del proyecto, `~/curso-java/revisor/Revisor.java`, y ejecutarlo.

## El porqué antes del cómo

Hay una razón para que esta lección vaya tan pronto en el curso: **un entorno que no funciona hace imposible todo lo que sigue**, y muchos de los problemas que las personas tienen con Java en sus primeros días son problemas de instalación y de versión, no de programación: se instaló una versión distinta de la que el tutorial supone, o se tiene más de una instalada y se usa la equivocada. Por eso el primer paso de la lección no es escribir código, sino **averiguar qué tienes**, y el último de la parte de instalación es **comprobar qué versión estás usando**. Todo lo demás, el archivo `Hola.java`, `javac`, `jshell`, se construye sobre la certeza de que el comando `java` de tu terminal es el de Java 25.

Hay también una razón de ánimo: quien llega a este curso ya programa, y lo primero que necesita es un éxito rápido. El programa que escribirás en la sección 1.2 tiene tres renglones, y lo ejecutas con un solo comando. Antes de Java 25, ese mismo programa pedía declarar una clase y un método `public static void main(String[] args)` a su alrededor; desde Java 25 es lo que ves abajo. Es la mejor noticia de Java para quien viene de otro lenguaje, y por eso el curso lo aprovecha.

La lección tiene siete partes: **instalar el JDK** (1.1), **tu primer programa** (1.2), **compilar y ejecutar por separado** (1.3), **`jshell`** (1.4), **leer el primer error** (1.5), **cuando la versión es la equivocada** (1.6) y **el estilo del código** (1.7), y termina con el primer paso del **proyecto** (1.8). Al final de la lección, esto es lo que verás al ejecutar el `revisor`, que por ahora no revisa nada, pero ya existe:

```text
revisor: todavía no revisa nada
```

## Los conceptos

### 1.1 Instalar el JDK

#### 1.1.1 Primero: qué tienes

Abre una terminal (en Mint: `Ctrl`+`Alt`+`T`) y haz dos preguntas. La primera, qué sistema es:

```bash
cat /etc/os-release
```

Entre los renglones que imprime busca `VERSION_CODENAME` y `UBUNTU_CODENAME`. En Linux Mint, el primero es el nombre de Mint (en la 22.3, `zena`) y el segundo es el de la versión de Ubuntu en que se basa (`noble`). Para instalar Java con el método de este curso no vas a necesitar ninguno de los dos, pero conviene saberlo: muchas recetas de internet se escribieron para Ubuntu y averiguan «el nombre de la versión» con el comando `lsb_release -cs`, que en Mint 22.3 contesta `zena`, no `noble` (medido en una Mint 22.3). Una receta que use esa respuesta para armar la dirección de un repositorio te deja apuntando a una dirección que no existe. Si alguna vez una receta de Ubuntu falla en tu Mint, empieza por ahí.

La segunda pregunta, si ya hay Java:

```bash
java --version
```

Puede pasar una de tres cosas. Si no hay Java, la terminal contesta que no encuentra el comando. En una Mint 22.3 recién instalada se midió `java: not found`, y algo más útil que el texto: el **código de salida**, el número que todo programa deja al terminar y que ves escribiendo `echo $?` justo después. Fue **127**, el número que la terminal reserva para «ese comando no existe». El texto del aviso puede variar de una terminal a otra; el 127, no. (Si prefieres preguntar sin ejecutar nada, `which java` busca el programa en tu sistema: sin Java no imprime nada y termina con código 1. Son dos códigos distintos para dos preguntas distintas: «ejecuta esto» y «¿dónde está esto?».) Si imprime una versión que empieza con 21 (u 11, u 17), alguien instaló Java antes, quizá sin darse cuenta, porque muchos programas lo traen como dependencia; no estorba, y en 1.1.7 verás por qué no hace falta quitarlo. Y si imprime 25 y dice `Temurin`, ya tienes lo que el curso usa y puedes saltar a 1.1.6 para confirmar que además tienes el compilador.

#### 1.1.2 Por qué no `default-jdk`, y por qué SDKMAN

Si preguntas en un buscador cómo instalar Java en Ubuntu o en Mint, la respuesta más frecuente es esta:

```bash
sudo apt install default-jdk
```

**No lo hagas para este curso.** El paquete `default-jdk` no es «la versión de Java más reciente»: es «la versión de Java que la distribución eligió como predeterminada», y esa elección se congela cuando sale la distribución. En Ubuntu Noble, la base de Mint 22, el paquete `default-jdk` depende de `openjdk-21-jdk` (según su [página en el archivo de paquetes de Ubuntu](https://packages.ubuntu.com/noble/default-jdk), consultada el 7 de octubre de 2026, y medido en una Mint 22.3: `apt` ofrece `default-jdk` en su versión `2:1.21-75`, la de Java 21). Es decir, instala **Java 21**. Java 21 es una versión LTS respetable, pero no la que enseña este curso, y lo que importa para la lección es lo que no puede hacer: **no ejecuta el archivo compacto con `void main()`**. Lo verás en 1.6 con su mensaje de error exacto. Quien instala `default-jdk` y sigue esta lección ve fallar el primer programa, y sin saber por qué, concluye que el curso está mal. No lo está; la versión es la equivocada.

El curso usa **Eclipse Temurin 25**, la distribución con la que se midieron todas sus salidas (la Lección 0 explicó qué es una distribución), y lo instala con **SDKMAN**, un administrador de versiones para herramientas de Java. Tres razones lo deciden. La primera: SDKMAN instala en tu carpeta personal (`~/.sdkman/`), **sin `sudo`**, sin tocar los programas del sistema; si algo sale mal, no rompes nada que use Mint. La segunda: permite tener varias versiones de Java a la vez y elegir cuál usar con un comando, algo que vas a necesitar el día que un proyecto ajeno exija otra versión. La tercera: con la misma herramienta instalarás Maven en la [Lección 6](06-proyecto-maven.md), con la versión exacta del curso. Un solo método para todo, el mismo en cada máquina.

#### 1.1.3 Lo que SDKMAN necesita

SDKMAN se instala con un guion de `bash` que descarga y descomprime archivos, y para eso usa unas cuantas herramientas del sistema. Al correrlo, el instalador busca `bash`, `unzip`, `zip`, `tar`, `find`, `curl` y `sed` (medido el 13 de octubre de 2026 en una Linux Mint 22.3). `bash`, `tar`, `find` y `sed` vienen siempre con Mint. Las tres que pueden faltar son `curl` (el programa que descarga cosas desde la terminal), `zip` y `unzip`. Para comprobarlo:

```bash
which curl zip unzip
```

`which` imprime la ruta de cada programa que encuentra y se calla con el que no. Si ves las tres rutas, sigue. Si falta alguna, instálala con el gestor de paquetes de Mint (esta es la única vez que el curso usa `sudo`, porque estas sí son herramientas del sistema):

```bash
sudo apt install curl zip unzip
```

`sudo` ejecuta lo que sigue con permisos de administrador y te pide tu contraseña (al teclearla no verás asteriscos; es normal). `apt install` instala los paquetes que nombras; si alguno ya estaba, lo dice y sigue con los demás.

#### 1.1.4 Instalar SDKMAN

El instalador es un guion que publica el propio proyecto en su sitio oficial. La [página de instalación de SDKMAN](https://sdkman.io/install/) da este comando:

```bash
curl -s "https://get.sdkman.io" | bash
```

Léelo por partes. `curl -s "https://get.sdkman.io"` descarga el guion de instalación desde el sitio de SDKMAN, por `https` (la conexión cifrada que garantiza que hablas con ese sitio y que nadie cambió el contenido en el camino), y `-s` le quita la barra de progreso. La tubería `|` le pasa ese texto a `bash`, que lo ejecuta. Es decir: descargas un programa y lo corres en el mismo renglón. Esa forma tiene mala fama, y conviene saber exactamente por qué. Lo peligroso no es la forma, sino **de dónde viene** lo que ejecutas: un guion de un sitio que no conoces, o pegado de un foro, puede hacer cualquier cosa con tu usuario. Aquí el origen es el sitio oficial del proyecto, por `https`, y el guion no pide `sudo`: trabaja solo en tu carpeta personal. Y si quieres verlo antes de ejecutarlo, puedes hacerlo en dos pasos, que es una buena costumbre con cualquier instalador: `curl -s "https://get.sdkman.io" -o instalar-sdkman.sh` lo guarda en un archivo, lo lees con `less instalar-sdkman.sh`, y lo ejecutas con `bash instalar-sdkman.sh`.

Esto es lo que imprimió el instalador el 13 de octubre de 2026 en una Linux Mint 22.3 (abreviado a los renglones que importan: los tres puntos marcan lo que se quitó, y la ruta de tu carpeta personal se muestra como `~`):

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

Se lee de arriba abajo: busca una instalación anterior, revisa que tengas una versión de `bash` que le sirva, busca las herramientas de 1.1.3 una por una, instala sus guiones en `~/.sdkman/` y agrega a tu archivo `~/.bashrc` un fragmento que carga SDKMAN cada vez que se abre una terminal. El renglón final es el que hay que obedecer: **la terminal en la que estás todavía no sabe que SDKMAN existe**, porque leyó `~/.bashrc` al abrirse, antes de la instalación. O abres una terminal nueva, o le pides a esta que cargue SDKMAN ya, con el comando `source` que te muestra. Después, comprueba:

```text
$ sdk version

SDKMAN!
script: 5.23.2
native: 0.7.34 (linux x86_64)
```

Si en lugar de eso ves `sdk: command not found`, es que la terminal no cargó SDKMAN. Abre una nueva, o carga SDKMAN a mano en esta con `source "$HOME/.sdkman/bin/sdkman-init.sh"` y vuelve a probar `sdk version`. Si `source` dice que ese archivo no existe, la instalación no terminó (casi siempre por una herramienta de 1.1.3 que faltaba o por un corte de conexión): lee el mensaje del instalador, corrige la causa y vuelve a correr el comando de instalación. Los números de versión serán los de tu día; lo que importa es que `sdk` responda.

#### 1.1.5 Elegir e instalar Temurin 25

SDKMAN no sabe qué Java quieres: lo eliges tú por su **identificador**. Para ver los que ofrece:

```bash
sdk list java
```

Imprime una tabla larga, con una sección por distribución (Corretto, GraalVM, Liberica, Temurin, Zulu y otras). Busca la de **Temurin**: sus identificadores terminan en **`-tem`**. El 13 de octubre de 2026, en una Mint 22.3, esa sección traía estos: `27.0.0.0+35-tem`, `26.0.2+1.1-tem`, `25.0.3.0+9-tem`, `21.0.12+1.1-tem`, `17.0.20-tem`, `11.0.32+1.1-tem` y `8.0.504+1-tem`. La regla para elegir es una: **el que empiece con `25` y termine en `-tem`**. Ese día era `25.0.3.0+9-tem`; en tu pantalla puede ser otro, porque el número completo cambia con cada actualización de Temurin. No copies el de esta lección: copia el de tu lista.

Un detalle medido que conviene conocer para no confundirse: al pie de la lista, SDKMAN sugiere un identificador por omisión (ese día decía `install the default: 25.0.4-tem`), y ese identificador **no aparecía en la lista**. Por eso el curso no te pide usar la sugerencia, sino elegir de la lista lo que sí está disponible. Con el identificador en la mano:

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

Descarga el JDK, lo acomoda y lo instala en `~/.sdkman/candidates/java/`, y el último renglón dice algo importante: lo deja como **predeterminado**, el que responderá al comando `java` en las terminales que abras desde ahora. Según el caso (por ejemplo, si ya tenías otro Java instalado con SDKMAN), en lugar de fijarlo solo puede **preguntarte** si quieres que sea el predeterminado: contesta que sí, con `Enter` o `Y`. Si contestaste que no, o no estás seguro, lo arreglas en cualquier momento con `sdk default java` y el identificador que instalaste (aquí, `sdk default java 25.0.3.0+9-tem`). La descarga toma de uno a varios minutos, según tu conexión.

#### 1.1.6 Comprobar la versión, en una terminal nueva

Aquí está la trampa que más tiempo hace perder, y se midió en la misma Mint: **en la terminal donde instalaste, `java --version` siguió dando el Java anterior** (en esa máquina había uno del sistema, en `/usr/bin/java`). No es que la instalación fallara: esa terminal decidió dónde buscar los programas cuando se abrió, y no se entera del cambio. **Cierra la terminal y abre una nueva.** Ahí:

```bash
java --version
javac --version
```

Los dos comandos, y no solo el primero, porque `java` es el programa que ejecuta y `javac` el que compila, y una instalación a medias puede dar uno sin el otro (es justo la diferencia entre un JRE y un JDK de la Lección 0). Esto es lo que imprimió `java --version` el 13 de octubre de 2026 en la Mint 22.3, en la terminal nueva:

```text
$ java --version
openjdk 25.0.3 2026-04-21 LTS
OpenJDK Runtime Environment Temurin-25.0.3+9 (build 25.0.3+9-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.3+9 (build 25.0.3+9-LTS, mixed mode, sharing)
```

Hay dos cosas que debes reconocer en la tuya: **el primer renglón empieza con `openjdk 25`** y **el segundo dice `Temurin`**. El resto cambia con la actualización: `25.0.3` es la versión (25 es la mayor; `0.3` es el número de parche), `2026-04-21` es la fecha de esa actualización, y `LTS` confirma que es una versión de soporte largo. Las salidas de las demás lecciones se midieron en otra máquina con un parche posterior (Temurin 25.0.4); el parche no cambia nada de lo que el curso enseña. `javac --version` debe decir `javac 25` seguido de su parche. Y si quieres saber de dónde sale el `java` que respondió, `which java` lo dice: en la Mint medida, la ruta dentro de la carpeta de SDKMAN que termina en `.sdkman/candidates/java/current/bin/java`. (`which` imprime la ruta completa, con tu carpeta personal: `/home/<tu usuario>/.sdkman/candidates/java/current/bin/java`; aquí se acorta con `~`.) Si `which java` te da `/usr/bin/java`, estás en una terminal vieja.

#### 1.1.7 Si tienes más de un Java

> **Varios JDK a la vez.** Si ya tenías un Java del sistema, o si algún día instalas otra versión con SDKMAN, no hace falta desinstalar nada: SDKMAN elige cuál responde al comando `java`. `sdk list java` marca en su tabla los que tienes instalados. `sdk use java <identificador>` cambia de versión **solo en la terminal actual**, para probar algo; `sdk default java <identificador>` la deja como predeterminada **en todas las terminales nuevas**. Si `java --version` no dice 25 y Temurin, lo primero es abrir una terminal nueva; lo segundo, `sdk default java` con el identificador de 1.1.5. Después, repite la comprobación de 1.1.6, que se repite siempre que cambias algo.

### 1.2 Tu primer programa

#### 1.2.1 La carpeta y el archivo

Crea la carpeta del curso y entra en ella:

```bash
mkdir -p ~/curso-java/revisor
cd ~/curso-java
```

`mkdir -p` crea la carpeta, y también las intermedias si faltan; sin `-p`, fallaría si `~/curso-java` no existiera cuando se pide `revisor` dentro de ella. `~` es un atajo para tu carpeta personal. En `~/curso-java/` vivirá todo el curso, y dentro de ella `revisor/` guardará el proyecto que crece lección tras lección.

Crea el archivo `Hola.java` con el editor de tu preferencia (en Mint viene Xed, que se abre con `xed Hola.java`) y escribe esto. Respeta la mayúscula inicial del nombre y la extensión. El primer renglón, `// fig01_01.java`, es un comentario: lo que va después de `//` hasta el final del renglón lo ignora el compilador. En el curso, ese comentario es la marca que dice cómo se llama el programa en el repositorio; en tu `Hola.java` puedes copiarlo o no, da lo mismo:

```java
// fig01_01.java
void main() {
  IO.println("Hola desde Java");
}
```

Guárdalo y, desde la terminal, en la carpeta `~/curso-java`, ejecútalo con el nombre de **tu** archivo:

```text
$ java Hola.java
Hola desde Java
```

(En el repositorio del curso el mismo programa se llama `fig01_01.java`, y `java fig01_01.java` imprime lo mismo.)

Ese es todo el programa. Veamos qué dice cada parte.

#### 1.2.2 Renglón por renglón

**`void main() {`** declara el punto de entrada: lo primero que Java ejecuta cuando corre el programa. Son tres piezas. `main` es el nombre que Java busca para empezar (no es una palabra reservada, es una convención del lenguaje que el lanzador conoce). `()` son los paréntesis donde irían los datos que el método recibe; aquí no recibe ninguno. `void` quiere decir «no devuelve nada»: `main` hace cosas, pero no entrega un resultado a quien lo llamó. Las llaves `{ }` delimitan el cuerpo: todo lo que va entre la de apertura y la de cierre es lo que `main` hace. Este «método» sin clase alrededor es una novedad de Java 25: se llama **archivo compacto** ([JEP 512](https://openjdk.org/jeps/512)).

**`IO.println("Hola desde Java");`** imprime un renglón de texto. `IO` es una clase de la biblioteca estándar, `java.lang.IO`, que se agregó en Java 25 para entrada y salida sencilla ([documentación](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/IO.html)); como está en el paquete `java.lang`, que Java importa siempre, **no hay que importar nada**. `println` («imprime una línea») escribe lo que se le pasa y un salto de línea. Lo que va entre los paréntesis, `"Hola desde Java"`, es un texto, que en Java se escribe entre comillas dobles. El punto y coma `;` termina la instrucción: en Java es obligatorio (a diferencia de Python o Go), y olvidarlo es el error más común del primer día.

**`}`** cierra el cuerpo.

Dos cosas que **no** hay: una clase alrededor, y las palabras `public static` ni los `String[] args`. En el resto del curso eso es lo normal. ¿Quién creó entonces la clase? El compilador, en silencio: toma tu archivo, le pone de nombre de clase el del archivo (`Hola`) y envuelve tu `main` en ella. Lo comprobarás en 1.3 con `javap`.

#### 1.2.3 Cómo se escribía antes

Es indispensable que reconozcas la forma antigua, porque es la que verás en casi todos los tutoriales, libros y respuestas de internet que no sean de los últimos dos años. El mismo programa, escrito como se exigía hasta Java 24, es:

```java
public class Viejo {
  public static void main(String[] args) {
    System.out.println("Hola desde Java");
  }
}
```

Corre exactamente igual en Java 25 (se comprobó: imprime `Hola desde Java`). Cada palabra extra tenía una función, que la Lección 6 explica a fondo, y que aquí solo se esboza para que no te asusten al verla. `class Viejo { ... }` define una clase, la unidad en que Java organiza el código; `public` la hace visible desde fuera del archivo. `static` significa que el método pertenece a la clase y no a un objeto particular, de modo que se puede llamar sin crear nada antes. `String[] args` es la lista de argumentos que el usuario escribe después del nombre del programa en la línea de comandos. Y `System.out.println` es el camino largo para imprimir: `System` es una clase, `out` es un objeto dentro de ella (la salida estándar) y `println` es lo que se le pide.

Para un programa de un renglón, todo eso es ceremonia. Java 25 la quita **cuando no hace falta**, y se vuelve necesaria otra vez cuando el programa crece. El curso usa la forma corta hasta que crece, y `IO.println` en lugar de `System.out.println`, que en este curso es más corto de escribir y más fácil de leer. Si lees código que usa `System.out.println`, hace lo mismo; no hay que cambiarlo.

#### 1.2.4 El nombre del archivo y otros detalles que muerden

Hay cuatro detalles que conviene saber desde ahora. **Uno:** en un archivo compacto, el compilador usa el nombre del archivo como nombre de la clase, así que debe ser un nombre válido para Java y, por convención, empezar con mayúscula (`Hola.java`, `Revisor.java`). **Dos:** el programa debe tener un método `main`; con otro nombre, el compilador lo rechaza con un mensaje que lo dice (lo verás en 1.5). **Tres:** el texto del archivo debe estar en UTF-8, que es lo que producen por omisión los editores de Mint. **Cuatro:** `java Hola.java` y `java hola.java` son archivos distintos en Linux, donde las mayúsculas cuentan; en macOS y Windows a veces da igual, y es un clásico de «en mi máquina funcionaba».

Y un detalle más sobre qué hace `java Hola.java`: el programa `java` ve que le diste un archivo `.java`, lo **compila en la memoria** y lo ejecuta, sin dejar nada en disco. Se llama modo de lanzamiento de un archivo fuente ([JEP 330](https://openjdk.org/jeps/330)) y existe desde Java 11. Es el que usa el curso de la Lección 1 a la 5.

### 1.3 `javac` y `java` por separado

#### 1.3.1 Compilar

Lo que `java Hola.java` hace en un solo paso se puede hacer en dos, y verlo así es la mejor manera de entender qué es un `.class`. En la carpeta donde está `Hola.java`:

```bash
javac Hola.java
ls Hola.*
```

```text
$ ls Hola.*
Hola.class  Hola.java
```

`javac Hola.java` compiló el archivo y, si no hubo errores, no imprimió nada (un compilador que no dice nada es un compilador contento), pero dejó un archivo nuevo: `Hola.class`. Ese archivo es el *bytecode* de la Lección 0. Pesa 320 bytes y empieza con los ocho bytes que ya conoces (si tu Mint no trae `xxd`, sáltate este comando: no se vuelve a usar):

```text
$ xxd -l 8 Hola.class
00000000: cafe babe 0000 0045                      .......E
```

Para ver qué contiene sin leerlo byte a byte, el JDK trae el desensamblador `javap`:

```text
$ javap Hola
Compiled from "Hola.java"
final class Hola {
  Hola();
  void main();
}
```

Ahí está la prueba de lo que se dijo en 1.2.2: el compilador creó una clase llamada `Hola` (como el archivo), y puso dentro tu método `main`, además de un constructor `Hola()` que tú no escribiste (un constructor es lo que crea un objeto de la clase; la Lección 3 lo explica). Con `javap -c -p Hola` verías además las instrucciones de `main`, entre ellas una llamada al método `println` de la clase `IO`.

#### 1.3.2 Ejecutar

Para ejecutar el `.class` ya no se le da el archivo al lanzador sino **el nombre de la clase**:

```text
$ java Hola
Hola desde Java
```

Sin extensión: `Hola`, no `Hola.class` ni `Hola.java`. Es la confusión más frecuente de esta sección. Los tres errores que produce se midieron, y conviene tenerlos a la vista; el primero es el de ejecutar la clase **antes** de compilar, el segundo el de ponerle la extensión, y el tercero el de olvidarse de la extensión en el compilador:

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

Los dos primeros se obtuvieron con el idioma de Java en inglés; en un sistema con la configuración regional en español, el mismo mensaje sale traducido (por ejemplo, «Error: no se ha encontrado o cargado la clase principal Hola»). Aprende a reconocerlos en ambos idiomas: «no se pudo encontrar o cargar la clase principal» quiere decir **que `java` buscó una clase con ese nombre y no la halló**; casi siempre se debe a que falta compilar, a que escribiste mal el nombre o a que estás en otra carpeta. El tercer mensaje es el más enredado de los tres: `javac` interpretó `Hola` como el nombre de una clase y no como un archivo, y su queja habla de «procesamiento de anotaciones», un tema que no tiene nada que ver con lo que te pasó. La corrección es la misma de siempre: `javac` lleva el nombre del **archivo**, con su `.java`.

Por qué `java` busca la clase donde la busca: por omisión, en la carpeta actual. Hay un mecanismo para decirle que busque en otras partes (el *classpath*, que la Lección 6 presenta y la Lección 8 estudia), pero mientras estés parado en la carpeta del `.class`, lo encuentra.

#### 1.3.3 El `.class` viejo

El `.class` es una copia congelada: **no se actualiza solo cuando editas el `.java`**. Se midió así. Se compiló un programa que imprime `version uno`, se corrió con `java V`, se cambió el archivo para que imprima `version dos` y se volvió a correr `java V`, sin compilar:

```text
$ java V
version uno
```

Sigue diciendo `version uno`, aunque el `.java` ya dice otra cosa, porque `java V` ejecuta el `.class` y el `.class` no se ha enterado. Hay que volver a compilar con `javac V.java`. En cambio, `java V.java` (con la extensión) recompila en cada ejecución y siempre refleja el archivo actual, que es otra razón por la que el curso lo usa en las primeras lecciones. El día que el proyecto crezca, Maven se encargará de recompilar cuando haga falta, que es una de las cosas para las que existe (Lección 6).

### 1.4 `jshell`: probar una idea sin crear un archivo

Entre las herramientas del JDK, hay una que usarás todos los días: `jshell`, un intérprete interactivo ([manual](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jshell.html)). Escribes una expresión, Java la evalúa y te muestra el resultado, sin archivo, sin `main`, sin compilar. Es la forma más rápida de contestar «¿qué hace esto?». En la terminal, escribe `jshell`:

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

Esta sesión se armó con las respuestas reales de una corrida de `jshell` con esos mismos comandos. Cada vez que escribes algo en el indicador `jshell>`, lo evalúa y responde con una flecha `==>` y el valor. Las expresiones que no tienen nombre reciben uno automático: `$1`, `$3`, `$4`. (¿Dónde está el `$2`? La instrucción `String s = "hola"` le puso nombre a su valor, `s`, y por eso el contador saltó.) El comando `/vars` lista las variables que existen, con su tipo, y `/exit` sale. Los comandos de `jshell` empiezan con una barra, para distinguirlos del código de Java.

Fíjate en la cuarta entrada: `"142" + 1` dio `"1421"`, el mismo resultado que viste en `fig00_04.java`. Esto es lo valioso de `jshell`: sirve para **comprobar** una duda en diez segundos. También te muestra los errores sin ruido:

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

Con tres limitaciones que conviene decir de una vez. `jshell` no sustituye a un programa: lo que pruebas ahí no queda guardado (hay `/save` para eso, pero un archivo de verdad es mejor). Algunas cosas del lenguaje que necesitan contexto, como un `main` o una clase con varios métodos, no son cómodas ahí. Y la numeración `$1`, `$2`... confunde al principio. Úsalo para dudas de una expresión y de un método de la biblioteca, no para escribir el `revisor`.

### 1.5 Leer el primer error del compilador

#### 1.5.1 Provocarlo

Los errores del compilador son lo primero que leerás cada día, así que conviene tenerles confianza desde ahora. Se provoca uno a propósito. Este programa tiene una errata: el método se llama `println`, y el programa escribe `prinln`. Guárdalo en `~/curso-java` con el nombre de la figura, `fig01_02.java`, para que el mensaje que veas diga lo mismo que el de abajo (el mensaje nombra al archivo), y copia también el comentario del primer renglón, porque los números de renglón cuentan con él:

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

#### 1.5.2 Anatomía

Es un mensaje de seis partes, y las seis se repiten, con pequeñas variantes, en cualquier error de compilación que veas en tu vida.

1. **`fig01_02.java:3:`** es el archivo y el número de **renglón**. El renglón 1 es el comentario `// fig01_02.java`, el 2 es `void main() {` y el 3 es el que falla. Cuando el error dice un renglón, ese es el lugar donde mirar primero; a veces el problema real está un poco antes (en un renglón anterior sin `;`), pero el compilador solo se da cuenta aquí.
2. **`error: cannot find symbol`** es el **diagnóstico**: «no se encuentra el símbolo». Un símbolo es un nombre: de una variable, de un método, de una clase. El compilador vio un nombre y no sabe a qué se refiere.
3. **El renglón citado**, `IO.prinln("hola");`, copiado del archivo para que no tengas que ir a buscarlo.
4. **La flecha `^`**, que apunta a la columna donde el compilador detectó el problema. Aquí está debajo del punto, en la frontera entre `IO` y lo que sigue: el compilador sabe que `IO` existe, y lo que no encuentra es lo que viene después del punto.
5. **`symbol:   method prinln(String)`** dice **qué** no encontró con tanto detalle como puede: un método llamado `prinln` que recibe un `String` (el texto). Observa que dedujo el tipo `String` del argumento que tú pasaste.
6. **`location: class IO`** dice **dónde** lo buscó: en la clase `IO`. Juntos, `symbol` y `location` se leen como una frase: «busqué el método `prinln(String)` en la clase `IO` y no existe». Y esa frase te dice cómo arreglarlo: o no existe porque lo escribiste mal (es el caso), o porque no está en esa clase, o porque falta un `import`.

Al final, `1 error` cuenta cuántos encontró, y `error: compilation failed` es el lanzador `java` avisando que, por eso, no ejecutó nada. Si compilas con `javac`, el mensaje es el mismo pero sin esa última línea, y `javac` termina con código de salida 1.

#### 1.5.3 Cómo se lee un compilador

Hay tres hábitos que ahorran horas. **Lee el primer error, no el último.** A menudo un solo problema produce una cascada: una llave sin cerrar puede generar diez mensajes. Arregla el primero, vuelve a compilar y mira cuántos siguen. **Lee el mensaje entero antes de buscar en internet.** `symbol` y `location` suelen ser suficientes. Y **cuando no entiendas un error, reduce el programa** hasta que quede el renglón que lo produce.

Cinco errores más, medidos con Temurin 25.0.4 con programas de tres renglones y descritos aquí para que los reconozcas cuando los veas. Los mensajes se muestran abreviados a su primer renglón:

| Lo que escribiste | Lo que dice el compilador |
|---|---|
| falta el `;` al final: `IO.println("hola")` | `error: ';' expected` (con la flecha justo después del paréntesis) |
| falta cerrar las comillas: `IO.println("hola);` | `error: unclosed string literal` |
| `io.println("hola");` (minúscula) | `error: cannot find symbol` / `symbol:   variable io` / `location: class <nombre del archivo>` |
| falta la llave final `}` | `error: reached end of file while parsing` |
| el método se llama `principal` y no `main` | `error: compact source file does not have main method in the form of void main() or void main(String[] args)` |

El tercero merece una lectura: `io` en minúscula no es `IO`; Java distingue las mayúsculas, así que el compilador piensa que `io` es una variable y no la encuentra. El último dice con todas sus letras lo que Java espera para un archivo compacto: un método `main`, sin parámetros o con uno solo, un `String[] args`.

### 1.6 Cuando la versión es la equivocada

Esta sección explica uno de los tropiezos más comunes de quien empieza con Java. El programa de 1.2, `Hola.java`, solo funciona en Java 25. Con una versión anterior, los errores son otros, y conviene verlos para identificarlos de un vistazo. Con **Java 21** (la que instala `default-jdk` en Mint 22), el mismo `Hola.java` da lo siguiente. (Estos dos bloques son fragmentos y no figuras del curso: se corrieron con otro JDK, el 21 y el 17, y por eso no se comparan con Java 25.)

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

Dice, en sustancia, que «las clases sin nombre son una función en vista previa y están desactivadas». En Java 21 el archivo compacto existía, pero como vista previa (la idea de la Lección 0): había que pedirlo con `--enable-preview`, y con otro nombre y otra forma. En Java 25 ya es definitivo. Con **Java 17**:

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

«Se esperaba una clase, una interfaz, una enumeración o un `record`»: Java 17 no conoce el archivo compacto, y espera que todo lo que haya en un archivo esté dentro de una clase.

**El diagnóstico es siempre el mismo:** `java --version`. Si dice 21 o 17, no es un error de tu programa, es la versión. Para arreglarlo, instala Temurin 25 con SDKMAN (1.1.5), abre una terminal nueva y, si tienes varios, elige el correcto con `sdk default java` (1.1.7). La clave: **no cambies tu programa para que compile en una versión vieja**. El curso entero está escrito para Java 25.

### 1.7 El estilo del código

Un último detalle que parece menor y no lo es. Habrás notado que el código de esta lección usa **dos espacios** de sangría, no cuatro ni tabulaciones. No es un capricho: es la guía de estilo de Google para Java ([Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)), una de las más difundidas, y es la que verifica la herramienta Checkstyle en la Lección 10. Si el código de las lecciones 1 a 9 usara otro estilo, la 10 tendría que confesar que todo lo anterior estaba mal escrito. Mucho código Java en internet usa cuatro espacios; funciona igual, y es solo una convención. La guía pide tres cosas desde ahora: sangría de **2 espacios**, renglones de **100 columnas** como máximo, y llaves siempre, incluso en un `if` de un solo renglón (cuando llegues a él).

### 1.8 El paso del proyecto: el primer archivo del `revisor`

El `revisor` empieza hoy. Todavía no revisa nada; lo que importa es que exista, en el lugar correcto, y que corra. En la carpeta `~/curso-java/revisor/` que creaste en 1.2.1, crea el archivo `Revisor.java` con este contenido (el comentario del primer renglón puedes omitirlo; está porque en el repositorio la figura se llama `fig01_03.java`):

```java
// fig01_03.java
void main() {
  IO.println("revisor: todavía no revisa nada");
}
```

Desde `~/curso-java/revisor/`, ejecútalo con el nombre de tu archivo:

```text
$ java Revisor.java
revisor: todavía no revisa nada
```

(Fíjate en la `í` de «todavía»: Java escribe el acento sin problema, siempre que tu terminal use UTF-8, que es lo normal en Mint. Si un día ves `?` en su lugar, es la configuración regional del sistema y no tu programa; lo midió la Lección 0.)

Ahora hazlo también a la antigua, para ver el `.class` del proyecto:

```bash
javac Revisor.java
java Revisor
javap Revisor
```

Imprime lo mismo, y `javap` muestra `final class Revisor`, con el método `main` dentro. Al terminar, **borra el `.class`** con `rm Revisor.class`: es un archivo tuyo, que acabas de generar, y no te sirve más, porque a partir de ahora la forma de correr el proyecto es `java Revisor.java`, y un `.class` viejo en la carpeta es una trampa (1.3.3) para cuando edites el `.java` y corras el `.class` por costumbre.

El proyecto crece desde aquí. En la Lección 2, `Revisor.java` aprende a clasificar un servicio; en la 3 se parte en varios archivos; en la 6 se convierte en un proyecto de Maven. El `revisor` que termina el curso arranca como este programa de tres renglones.

## El error que vas a ver

Esta lección tiene dos errores principales. El primero es el de siempre: el compilador que no encuentra un nombre. El segundo es el de la versión equivocada, y es el que más desorienta a quien empieza.

**El primero: `cannot find symbol`.** Es el de `fig01_02.java`, y se lee con las seis partes de 1.5.2:

```text
fig01_02.java:3: error: cannot find symbol
  IO.prinln("hola");
    ^
  symbol:   method prinln(String)
  location: class IO
```

Casi siempre es una errata, una mayúscula equivocada o un `import` olvidado. Se arregla corrigiendo el nombre y volviendo a ejecutar. Lo que **no** se hace es borrar renglones hasta que desaparezca: el mensaje te está diciendo exactamente qué nombre no encuentra y dónde lo buscó.

**El segundo: el archivo compacto con una versión anterior.** Con Java 21:

```text
error: unnamed classes are a preview feature and are disabled by default.
  (use --enable-preview to enable unnamed classes)
```

Con Java 17, `error: class, interface, enum, or record expected`. Son los dos síntomas de una sola enfermedad: **una versión de Java anterior a la 25**. No se arregla tocando el programa sino la instalación: `java --version` para confirmar, y 1.1.5 y 1.1.7 para corregir. Es el error de quien instaló `default-jdk` o sigue un tutorial viejo.

**Un tercero, más modesto:** `Error: Could not find or load main class Hola`. Quiere decir que se ejecutó `java Hola` sin haber compilado, o desde una carpeta donde no está el `.class`. Compila con `javac Hola.java`, o ejecuta con `java Hola.java`.

## Lo que se hace mal

**Instalar `default-jdk` por costumbre.** Es la respuesta más popular y, en Mint 22, instala Java 21, con lo que el archivo compacto falla. El camino del curso es Temurin 25 con SDKMAN (1.1.4 y 1.1.5).

**No comprobar la versión después de instalar.** «Se instaló sin errores» no es «el comando `java` apunta a la 25». La comprobación de 1.1.6 son dos comandos y diez segundos, y se repite cada vez que cambias algo del sistema.

**Comprobar en la misma terminal donde instalaste.** Es la trampa medida de 1.1.6: esa terminal sigue usando el Java que tenía al abrirse, y concluyes que la instalación falló. Abre una terminal nueva antes de comprobar.

**Comprobar solo `java` y no `javac`.** Un JRE tiene el primero y no el segundo. Para este curso necesitas los dos.

**Ejecutar guiones de orígenes que no conoces.** `curl ... | bash` ejecuta lo que descarga, y por eso lo que importa es de dónde viene: el instalador de SDKMAN sale del sitio oficial del proyecto, por `https`, y se puede leer antes de correrlo (1.1.4). Un guion pegado de un foro o de un sitio cualquiera no ofrece ninguna de esas dos cosas. Lo mismo vale, con más razón, para cualquier comando con `sudo`, que modifica tu sistema con permisos de administrador: si no sabes qué hace cada parte, no lo corras.

**Ejecutar `java Hola` sin haber compilado, o editar y no recompilar.** Es el `.class` viejo de 1.3.3: el programa hace lo de la versión anterior y crees que tu cambio no funcionó. Mientras aprendes, usa `java Hola.java`, que siempre compila el archivo actual.

**Poner la extensión donde no va, y quitarla donde sí.** `java` con un archivo fuente lleva `.java` (`java Hola.java`); con una clase compilada no lleva extensión (`java Hola`); y `javac` siempre lleva `.java`. Son tres reglas, y se aprenden de memoria con el uso.

**Corregir a ciegas varios errores a la vez.** Un error de compilación puede generar otros que no existen de verdad. Corrige el primero, vuelve a compilar, y mira qué queda.

**Escribir con tabuladores o con cuatro espacios «porque así lo hace todo el mundo».** Compila igual, pero en la Lección 10 la herramienta de estilo se quejará de cada renglón. Configura tu editor con dos espacios (en Xed: Preferencias, pestaña Editor, ancho de tabulación 2 e insertar espacios en lugar de tabuladores).

**Seguir un tutorial sin mirar su fecha.** Si el tutorial pone `public class` y `public static void main` para un programa de un solo archivo, es de antes de Java 25; funciona, pero no es el estilo de este curso. Y si habla de `default-jdk` o de un JDK anterior, también.

## Ejercicios

### Ejercicio 1 — Tu primer programa, a tu manera

Modifica `Hola.java` para que imprima tres renglones: tu nombre, el nombre del curso y la frase «Java 25». Ejecútalo con `java Hola.java`. Después compílalo con `javac Hola.java`, ejecútalo con `java Hola` y compara. Por último, cambia una palabra del texto, **sin** recompilar, y ejecuta otra vez `java Hola`. Anota en tu bitácora qué pasó y por qué.

### Ejercicio 2 — Predice y comprueba tres errores

Escribe tres versiones rotas de `Hola.java`, una a la vez: (a) sin el `;` al final del `IO.println`; (b) sin cerrar las comillas del texto; (c) con `io.println` en minúscula. **Antes de ejecutar cada una, escribe en tu bitácora qué mensaje esperas.** Ejecútalas y compara. Para cada una, indica el renglón que señala el compilador y a dónde apunta la flecha.

### Ejercicio 3 — Pregúntale a `jshell`

Abre `jshell` y evalúa: `"142" + 1`, `2300 / 1000` y `2300 / 1000.0`. Para cada una, escribe antes qué crees que da. Después teclea `/vars` y sal con `/exit`. Una de las tres respuestas probablemente te sorprenda: escribe en la bitácora cuál y por qué crees que da lo que da. (La explicación completa está en la Lección 2; aquí lo que cuenta es haberlo visto.)

### Ejercicio 4 — Diagnostica por el mensaje

Para cada mensaje, di qué pasó y cómo se arregla, sin ejecutar nada:

1. `Error: Could not find or load main class Hola` / `Caused by: java.lang.ClassNotFoundException: Hola`, al escribir `java Hola`.
2. `error: Class names, 'Hola', are only accepted if annotation processing is explicitly requested`, al escribir `javac Hola`.
3. `error: unnamed classes are a preview feature and are disabled by default.`, al escribir `java Hola.java`.

## Soluciones

### Solución 1

Un posible `Hola.java`:

```java
void main() {
  IO.println("Dorian Chávez");
  IO.println("Curso de Java de Hábil");
  IO.println("Java 25");
}
```

Con `java Hola.java` imprime los tres renglones. Tras `javac Hola.java`, `java Hola` imprime lo mismo. Si ahora cambias el texto de un `IO.println` en el `.java` y ejecutas `java Hola` **sin** volver a compilar, imprime el texto **viejo**: el `.class` es una copia congelada (1.3.3) y no sabe que cambiaste el fuente. Para ver el cambio hay que ejecutar `javac Hola.java` otra vez, o usar `java Hola.java`, que recompila siempre. Lo que anotas en la bitácora: el `.class` no se actualiza solo.

### Solución 2

Los tres mensajes, medidos (se muestra cada uno con su renglón señalado; el compilador siempre termina con `1 error` y `error: compilation failed`):

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

En (a), el compilador señala el renglón 2, no un renglón después, y pone la flecha **justo después del último carácter** del `IO.println("hola")`, porque ahí es donde se esperaba el `;`. En (b), la flecha apunta a la comilla de apertura: es donde empieza el texto que nunca se cerró. En (c), la flecha apunta a `io`, y el mensaje dice `variable io`, no `class io`: como la minúscula no coincide con ningún nombre que Java conozca, el compilador supone que es el nombre de una variable. Si la errata (c) te hizo pensar «debería decir que `io` no existe», es exactamente lo que dice, con el vocabulario del compilador. (En `location: class c`, el nombre `c` es el del archivo `c.java` con que se midió; en tu archivo dirá el tuyo.)

### Solución 3

Las respuestas de `jshell`, medidas:

```text
jshell> "142" + 1
$1 ==> "1421"

jshell> 2300 / 1000
$2 ==> 2

jshell> 2300 / 1000.0
$3 ==> 2.3
```

La que sorprende es la segunda: **`2300 / 1000` da `2`, no `2.3`**. Cuando los dos números son enteros, Java hace una división de enteros y descarta la parte decimal. Para obtener `2.3`, uno de los dos tiene que ser decimal, como el `1000.0` de la tercera. Es un asunto que viene a cuento más adelante: el `revisor` convierte 2300 milisegundos a «2.3s» y, si dividiera enteros, mostraría `2s`. La Lección 2 lo explica a fondo. (Aquí lo has visto con tus ojos, que es lo que importaba. La numeración en tu sesión puede diferir según lo que hayas evaluado antes.)

### Solución 4

1. **Se ejecutó la clase sin haberla compilado** (o desde otra carpeta). `java Hola` busca un archivo `Hola.class` y no lo hay. Se arregla compilando primero (`javac Hola.java`) o ejecutando el archivo fuente directamente (`java Hola.java`).
2. **Se le dio a `javac` el nombre de la clase y no el del archivo.** `javac` lleva `.java`: `javac Hola.java`. El texto sobre «procesamiento de anotaciones» confunde, pero es la forma en que `javac` interpreta un argumento sin extensión.
3. **La versión de Java es anterior a la 25**, probablemente la 21: el archivo compacto sin `class` solo es definitivo en Java 25. Se arregla instalando Temurin 25 con SDKMAN (1.1.5), dejándolo como predeterminado con `sdk default java` si hay otro (1.1.7), y confirmando con `java --version` en una terminal nueva.

## Cómo sé que lo logré

- [ ] En una terminal nueva, `java --version` imprime un primer renglón que empieza con `openjdk 25` y un segundo que dice `Temurin`, y sabes qué significaba el código 127 si antes no tenías Java.
- [ ] `sdk version` responde, y sabes qué identificador de Temurin instalaste y por qué ese.
- [ ] `javac --version` imprime `javac 25` seguido de un número de parche.
- [ ] `java Hola.java` imprime lo que escribiste, y `javac Hola.java` deja un `Hola.class` que se ejecuta con `java Hola`.
- [ ] Puedes explicar por qué, si editas el `.java` y ejecutas `java Hola`, no ves tu cambio.
- [ ] En `jshell`, `1 + 1` da `$1 ==> 2` y sabes salir con `/exit`.
- [ ] Provocaste el error de `fig01_02.java` y puedes señalar en él el renglón, la flecha, el `symbol` y el `location`.
- [ ] Desde `~/curso-java/revisor/`, `java Revisor.java` imprime `revisor: todavía no revisa nada`.
- [ ] Borraste el `Revisor.class` que generaste, y sabes por qué.
- [ ] Anotaste en tu bitácora qué te dijo `java --version` en tu máquina y qué pasó con el Ejercicio 1.

## Resumen

Responde sin mirar la lección:

1. ¿Por qué `default-jdk` en Mint 22 no sirve para este curso? ¿Qué mensaje produce el archivo compacto con esa versión?
2. ¿Qué hace `curl -s "https://get.sdkman.io" | bash`, y qué lo hace aceptable aquí? ¿Cómo eliges el identificador de Temurin en `sdk list java`? ¿Por qué hay que abrir una terminal nueva después de instalar?
3. ¿Qué dos comandos se ejecutan para comprobar la instalación, y por qué los dos?
4. ¿Qué es el archivo compacto? ¿Qué dos cosas del programa tradicional ya no hacen falta?
5. ¿Qué diferencia hay entre `java Hola.java` y `java Hola`? ¿Cuándo se queda desactualizado el `.class`?
6. ¿Qué contiene el `Hola.class` y cómo lo comprobaste con `javap`?
7. ¿Para qué sirve `jshell` y qué no es?
8. ¿Qué dicen las seis partes de un mensaje como `cannot find symbol`?
9. ¿Cómo distingues, solo por el mensaje, el error de un programa del error de una versión equivocada?

## Para leer más

- [SDKMAN, instalación](https://sdkman.io/install/) y [uso](https://sdkman.io/usage/): los requisitos, el instalador, y los comandos `list`, `install`, `use` y `default`. Consultado el 13 de octubre de 2026.
- [JEP 512, «Compact Source Files and Instance Main Methods»](https://openjdk.org/jeps/512): la propuesta que define el archivo compacto, `void main()` y la clase `IO`; explica qué crea el compilador por ti y por qué. Consultado el 7 de octubre de 2026.
- [Oracle, manual de `jshell` (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jshell.html): todos los comandos de `jshell`, de `/vars` a `/save`. Consultado el 7 de octubre de 2026.
- [Oracle, manual del comando `java` (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html): el lanzador de un archivo fuente y las banderas como `--enable-preview`. Consultado el 7 de octubre de 2026.

---

**Anterior:** [Lección 0 — Qué es Java y qué NO es](00-que-es-java.md) ·
**Siguiente:** [Lección 2 — Tipos, cadenas y control de flujo](02-tipos-cadenas-control.md)
