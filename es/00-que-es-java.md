# Lección 0 — Qué es Java y qué NO es

**Tiempo:** 90 min

**Qué construyes:** nada todavía. Es una lección de lectura: aquí se arma el mapa mental con el que el resto del curso se vuelve predecible

**Qué aprendes:** el camino del código fuente al programa que corre (`javac`, *bytecode*, la JVM); qué es el JDK y qué fue el JRE; qué significa LTS y por qué el curso usa Java 25; qué es una función en vista previa; y qué protege el compilador de Java y qué deja pasar (los tipos se revisan al compilar, pero los genéricos se borran y `null` sigue existiendo)

**De dónde vienes.** No traes nada de este curso: no has instalado Java ni escrito una línea. Traes, eso sí, lo que ya sabes de otro lenguaje —Go, TypeScript, Python o el que sea—: qué es una variable, un bucle, una función, un error. Eso es justo lo que el curso da por sabido. Lo que no traes es el mapa de **cómo funciona la plataforma de Java**, que es distinta de la de cualquiera de esos lenguajes y que explica por qué los comandos, los mensajes de error y las costumbres de Java son como son.

**Qué no hace esta lección.** No instala nada (eso es la [Lección 1](01-instalacion.md)) y no enseña la sintaxis de Java (eso empieza en la [Lección 2](02-tipos-cadenas-control.md)). Trae cuatro programas pequeños, de entre cuatro y ocho renglones, solo para que veas con tus ojos lo que la lección afirma. No hace falta que los entiendas renglón por renglón todavía, aunque cada uno se explica: lo que importa es lo que **imprimen**. Se corren después de la Lección 1, cuando ya tengas Java.

**Sobre las corridas.** Las salidas de esta lección se obtuvieron con Temurin 25.0.4 (una distribución gratuita de Java, que se explica más abajo) en macOS, con la configuración regional en UTF-8. En tu Linux Mint salen iguales; lo único que cambia, y se avisa donde ocurre, son los renglones que mencionan el sistema. Los programas están en [`programas/00-que-es-java/`](../programas/00-que-es-java), en el repositorio del curso.

## Al terminar vas a poder

- Describir con tus palabras el camino de un archivo `.java` hasta que corre: qué hace `javac`, qué contiene un `.class` y qué hace la JVM con él.
- Distinguir el JDK, el JRE y la JVM, y decir cuál instalar y por qué.
- Explicar qué significa que Java 25 sea LTS, por qué Java 26 y Java 27 existen pero el curso no los usa, y qué es una función en vista previa.
- Decir, ante un fallo posible, si el compilador de Java lo atrapa antes de ejecutar o si aparece hasta que el programa corre.
- Explicar qué es el borrado de genéricos y por qué `null` sigue existiendo en un lenguaje con tipos.
- Predecir qué imprime `"142" + 1` en Java y por qué no es una suma.

## El porqué antes del cómo

Imagina que alguien te pide el programa del curso, el `revisor`: lee una lista de servicios, los consulta a todos a la vez y te entrega un reporte. En Go lo harías y obtendrías un archivo ejecutable que copias a un servidor y corre. En Python, un script que corre mientras el intérprete esté instalado. En TypeScript, código que Node ejecuta después de que el compilador lo traduce a JavaScript. Tres lenguajes, tres respuestas a la misma pregunta: **¿qué es, exactamente, lo que se entrega y quién lo hace correr?**

Java tiene su propia respuesta, y es la primera cosa que hay que entender, porque todo lo demás cuelga de ella. En Java escribes archivos `.java`, un programa llamado `javac` los traduce a archivos `.class`, y otro programa llamado `java` los ejecuta dentro de una máquina virtual. Dos programas, dos formatos de archivo y una máquina virtual: si no sabes qué hace cada uno, la instalación de la Lección 1 es una lista de comandos que se obedece a ciegas, y los errores de la Lección 2 son mensajes de origen misterioso.

Además, hay una razón práctica. En la bibliografía y en los tutoriales de Java conviven consejos de hace veinte años con los de hoy, y casi nunca dicen de qué año son. Quien llega sin el mapa no puede distinguir «esto era así en 2008» de «esto es así en 2026». Con el mapa de esta lección, sí.

La lección tiene cinco partes, en este orden:

1. **El camino del código** (0.1): de `.java` a `.class` a la JVM.
2. **Las piezas de la plataforma** (0.2): el JDK, el JRE y las distribuciones.
3. **Las versiones** (0.3): qué es LTS, qué es la vista previa y por qué la 25.
4. **Qué protege el compilador y qué no** (0.4): la parte más útil de la lección, con cuatro programas que se corren de verdad.
5. **Java frente a lo que ya conoces** (0.5) y **lo que viene en el resto del curso** (0.6).

Con ese destino en mente, esto es lo que verás en esta lección, en la sección 0.4.1, cuando corras el primer programa. Es un mensaje del compilador, no de la ejecución, y la diferencia entre las dos cosas es la idea central:

```text
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
```

## Los conceptos

### 0.1 El camino del código: fuente, *bytecode* y JVM

#### 0.1.1 Las tres cosas que existen

Cuando escribes un programa en Java, hay tres objetos distintos y conviene ponerles nombre desde ya:

- El **código fuente**: el archivo de texto que escribes, con extensión `.java`. Lo lee una persona.
- El ***bytecode***: el resultado de compilar el código fuente, guardado en un archivo con extensión `.class`. Es un formato binario, compacto, que no está pensado para que lo lea una persona ni es el lenguaje de ningún procesador real. Es el lenguaje de un procesador **imaginario**.
- La **JVM** (*Java Virtual Machine*, «máquina virtual de Java»): el programa que ejecuta el *bytecode*. Es el procesador imaginario hecho realidad, una pieza de software que existe para cada sistema operativo y cada tipo de procesador.

El programa `javac` es el compilador: convierte el código fuente en *bytecode*. El programa `java` es el lanzador: arranca una JVM y le pide que ejecute el *bytecode* de una clase. La palabra «clase» va a ser importante, así que una definición provisional: en Java, cada `.class` contiene **una** unidad de código llamada clase (más adelante habrá otras variantes, como los `record` de la Lección 3, que también terminan en un `.class`). Por eso se dice «el archivo de clase».

El dibujo completo del camino es este:

```text
Hola.java  ──javac──►  Hola.class  ──java──►  [ JVM ]  ──►  lo que el programa hace
(código fuente)        (bytecode)             (ejecuta)
```

¿Por qué dos pasos y no uno? Porque cada paso resuelve un problema distinto y los resuelve **en momentos distintos**. El compilador trabaja una vez, en tu máquina de desarrollo, y es el que revisa que lo que escribiste tenga sentido (por ejemplo, que no intentes meter un texto donde va un número, como verás en 0.4). La JVM trabaja cada vez que el programa corre, en la máquina donde corre, y es la que lo hace funcionar con ese sistema operativo y ese procesador. El *bytecode* es el contrato entre las dos mitades: el compilador promete producir *bytecode* válido, y la JVM promete ejecutarlo.

#### 0.1.2 Un `.class` por dentro

Todo esto suena abstracto hasta que se ve. En la Lección 1 compilarás tu primer programa y verás el `.class` aparecer en tu carpeta. Mientras tanto, mira qué contiene uno pequeño. Este fragmento no es un programa de la lección: es una clase mínima, `Suma`, con un método que suma dos enteros, que se compiló con `javac Suma.java`. Después se le pidió al desensamblador del JDK, `javap`, que muestre el *bytecode* de ese método:

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

No necesitas entender cada renglón, pero fíjate en el método `sumar`, el de abajo. Sus instrucciones son `iload_0`, `iload_1`, `iadd` e `ireturn`: «carga el primer argumento entero», «carga el segundo», «suma dos enteros», «devuelve un entero». Son las instrucciones de un procesador sencillo, de los que se programan con una pila: se meten dos números en la pila, se suman, el resultado queda arriba. Ese procesador no existe en ninguna computadora real. Existe solo dentro de la JVM. Las instrucciones y el formato del archivo están definidos por una especificación pública, la [Especificación de la Máquina Virtual de Java](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html), que es la que cumplen todas las JVM.

Dos datos más del archivo, que se midieron con `xxd` y con `javap -v`. Los primeros ocho bytes de un `.class` de Java 25 son `cafe babe 0000 0045`. Los cuatro primeros, `cafebabe`, son un número mágico que identifica cualquier archivo de clase, de cualquier versión: la especificación lo fija así. Los últimos dos bytes (`0045` en hexadecimal) son la **versión mayor** del formato, 69 en decimal, que es la que corresponde a Java 25. Cada versión nueva de Java sube ese número, y una JVM vieja se niega a ejecutar un `.class` con un número más alto que el que entiende. Cuando veas más adelante un mensaje que habla de «class file version», es esto.

#### 0.1.3 Qué hace la JVM con el *bytecode*

La JVM no solo «interpreta» el *bytecode* instrucción por instrucción. Hace tres cosas, y vale la pena saber cuáles, aunque ninguna la programarás tú:

1. **Carga y verifica.** Lee el `.class`, comprueba que el *bytecode* sea coherente (que no intente sacar de la pila lo que nunca metió, por ejemplo) y recién entonces lo deja ejecutarse. Es una red de seguridad contra archivos corruptos o mal hechos.
2. **Ejecuta, y mientras ejecuta, observa.** Empieza interpretando el *bytecode*. A medida que el programa corre, detecta los métodos que se ejecutan muchas veces.
3. **Compila los métodos calientes a código de máquina real**, en caliente, mientras el programa sigue funcionando. Esa pieza se llama compilador JIT (*just in time*, «justo a tiempo»). Es la razón por la que un programa de Java, tras unos instantes de calentamiento, puede correr a velocidades comparables con las de un programa compilado desde el principio.

Además, la JVM administra la memoria: reserva la que el programa pide y la libera sola cuando ya nadie la usa, con una pieza llamada recolector de basura. Ya conoces la idea si vienes de Go, de Python o de TypeScript. La [guía de la JVM de Java 25](https://docs.oracle.com/en/java/javase/25/vm/) y la [guía del recolector de basura](https://docs.oracle.com/en/java/javase/25/gctuning/) cuentan los detalles; para este curso basta con saber que existen y que no vas a liberar memoria a mano.

La consecuencia práctica de todo esto es una promesa famosa de Java: **«escribe una vez, ejecuta donde sea»**. El mismo `.class` corre en Linux, en macOS y en Windows, porque cada sistema tiene su propia JVM que sabe ejecutarlo. La promesa es real, pero tiene bordes, y conviene saberlos desde ya para no creerla de más. Dos ejemplos medidos: el nombre de las rutas, la codificación del texto y los saltos de línea cambian de un sistema a otro, y el código que los asume se rompe aunque el `.class` sea el mismo. Y la **salida de texto** depende de la configuración del sistema: con la configuración regional mínima (`LC_ALL=C`), un programa que imprime `sin conexión` escribió `sin conexi?n`, porque el sistema le dijo a Java que su salida era ASCII; con `LC_ALL=C.UTF-8` imprimió el texto correcto. Desde Java 18, el texto de los archivos se lee y se escribe en UTF-8 por omisión ([JEP 400](https://openjdk.org/jeps/400)), pero la salida estándar sigue la configuración del sistema. Portable no significa idéntico.

#### 0.1.4 Y `java Hola.java`, ¿cuál de los dos pasos es?

Más adelante verás que se puede ejecutar un programa con un solo comando, `java Hola.java`, sin pasar por un `javac` visible. No contradice nada de lo anterior. Ese modo, llamado lanzador de un archivo fuente ([JEP 330](https://openjdk.org/jeps/330)), compila el archivo **en la memoria**, sin dejar un `.class` en disco, y lo ejecuta de inmediato. Los dos pasos ocurren; solo que uno de ellos no deja huella. Es cómodo para aprender y para programas de un archivo; para proyectos de verdad se compila por separado, y es lo que haces desde la Lección 6.

### 0.2 Las piezas de la plataforma: JDK, JRE y distribuciones

#### 0.2.1 JDK y JRE

Hay dos siglas que verás en todos los tutoriales, y cuyo significado cambió con los años:

- El **JRE** (*Java Runtime Environment*) es lo necesario para **ejecutar** programas de Java: la JVM y las bibliotecas estándar. Con un JRE puedes correr un `.class` o un JAR, pero no puedes compilar.
- El **JDK** (*Java Development Kit*) es el JRE **más** las herramientas para desarrollar: el compilador `javac`, el desensamblador `javap`, el intérprete interactivo `jshell` que usarás en la Lección 1, el empaquetador `jar` y otras. La carpeta `bin` de un Temurin 25.0.4 trae 30 programas (contados con `ls | wc -l` y vueltos a contar uno por uno en la lista: `jar`, `jarsigner`, `java`, `javac`, `javadoc`, `javap`, `jcmd`, `jconsole`, `jdb`, `jdeprscan`, `jdeps`, `jfr`, `jhsdb`, `jimage`, `jinfo`, `jlink`, `jmap`, `jmod`, `jnativescan`, `jpackage`, `jps`, `jrunscript`, `jshell`, `jstack`, `jstat`, `jstatd`, `jwebserver`, `keytool`, `rmiregistry`, `serialver`).

Este curso necesita compilar, así que instala **el JDK**. La confusión histórica es que durante años la gente instalaba «Java» y obtenía el JRE (lo que bastaba para correr programas ajenos, como un instalador o una aplicación de escritorio), y para programar tenía que ir a buscar «el JDK». Hoy esa distinción casi no importa en tu computadora de desarrollo, porque instalas el JDK y listo; pero sigue importando en un servidor o en una imagen de contenedor, donde conviene instalar solo lo necesario para ejecutar. Algunas distribuciones todavía publican paquetes de solo ejecución; por ejemplo, la página de instalación de Temurin para Linux los ofrece como `temurin-<versión>-jre`.

Y una tercera sigla que conviene no mezclar: la **JVM** es solo la máquina virtual, un componente que vive dentro de los dos. «Instalar la JVM» no es algo que se haga por separado.

#### 0.2.2 Una especificación, un proyecto y muchas distribuciones

La parte que más desconcierta a quien llega de otros lenguajes: **no hay un solo «Java» que se descargue de un solo lugar**. Hay tres capas.

1. **La especificación.** El lenguaje y la JVM están definidos por documentos públicos: la [Especificación del Lenguaje Java](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html) (JLS) y la de la JVM (JVMS) que viste en 0.1. Cualquiera puede implementarlas.
2. **OpenJDK.** Es el proyecto de código abierto donde se desarrolla la implementación de referencia. Cada versión nueva tiene su página en [openjdk.org](https://openjdk.org/projects/jdk/25/) con las funciones que trae y su calendario. OpenJDK publica código fuente; no es lo que instalas.
3. **Las distribuciones.** Son los binarios que alguien construye a partir de OpenJDK, los prueba y los publica con su propia política de actualizaciones: Eclipse Temurin (de la fundación Eclipse, a través del proyecto Adoptium), los de Oracle, los de Azul, los de Amazon, los que Ubuntu incluye en su repositorio, y otros. Todas ejecutan el mismo lenguaje; difieren en quién las construye, cuánto tiempo las mantiene y bajo qué licencia.

Este curso usa **Eclipse Temurin**, la distribución gratuita del proyecto Adoptium, de la fundación Eclipse, que no tiene un dueño comercial y publica un calendario de actualizaciones claro: su [página de soporte](https://adoptium.net/support/) dice cuánto tiempo recibe correcciones cada versión, y de ahí sale la tabla de la sección siguiente. Todas las salidas del curso se midieron con Temurin 25. La Lección 1 la instala con **SDKMAN**, un administrador de versiones que trabaja en tu carpeta personal, sin `sudo`, y que en la Lección 6 instala también Maven. Y explica de paso por qué **no** se usa `default-jdk`, el paquete que Linux Mint te sugiere. Anticipo del motivo: en Mint 22 ese paquete instala Java 21, no Java 25.

### 0.3 Las versiones: ritmo, LTS y vista previa

#### 0.3.1 Una versión nueva cada seis meses

Java dejó de sacar una versión grande cada varios años y adoptó un ritmo fijo: **una versión nueva cada seis meses**, en marzo y en septiembre. Lo dice la página de soporte de Temurin: «OpenJDK provide a new feature release every six months». Cada una trae pocas funciones, pensadas para que el cambio sea pequeño.

La consecuencia es que hay muchas versiones, y la mayoría **dejan de recibir actualizaciones a los seis meses**. Sería inmanejable para quien mantiene un programa en producción. Por eso existe el concepto de LTS.

#### 0.3.2 LTS: la versión que se mantiene años

LTS son las siglas de *Long-Term Support*, «soporte a largo plazo». Una versión LTS es la que los fabricantes se comprometen a mantener, con correcciones de seguridad, durante años. La página de soporte de Temurin lo describe así: cada dos años, desde 2021, una versión se designa LTS, y Temurin se compromete a mantenerla por lo menos cuatro años. Las cuentas que ahí se publican (consultadas el 7 de octubre de 2026):

| Versión | Salió | Tipo | Temurin la mantiene |
|---|---|---|---|
| Java 17 | septiembre de 2021 | LTS | por lo menos hasta octubre de 2027 |
| Java 21 | septiembre de 2023 | LTS | por lo menos hasta diciembre de 2029 |
| **Java 25** | **septiembre de 2025** | **LTS** | **por lo menos hasta septiembre de 2031** |
| Java 26 | marzo de 2026 | no LTS | terminó en septiembre de 2026 |
| Java 27 | 15 de septiembre de 2026 | no LTS | hasta marzo de 2027 |

La tabla responde la pregunta «¿por qué Java 25 si ya hay una 27?». Porque la 27 **no es LTS**: dentro de seis meses dejará de recibir correcciones y habrá que saltar a otra. Un curso que se publica hoy y se lee durante años no debe enseñar sobre una base que caduca en medio año. La 25 es la LTS vigente, y es la que cualquier equipo serio tendrá en producción en los próximos años. Si el ritmo de dos años se mantiene, la siguiente LTS será la 29, en septiembre de 2027; eso es una extrapolación de ese ritmo, no una promesa de nadie.

Dos aclaraciones que evitan confusiones. Primero, «LTS» no quiere decir «sin cambios»: la 25 recibe correcciones de seguridad y de errores, pero no funciones nuevas. Segundo, los números de parche —lo que va después de `25.` en lo que imprime `java --version`, que empieza con 25— cambian cada tres meses; por eso en el curso se dice «Java 25» y nunca un número de parche.

#### 0.3.3 La vista previa: lo que existe pero todavía no es definitivo

Java incorpora funciones nuevas por una etapa intermedia: la **vista previa** (*preview*), descrita en el [JEP 12](https://openjdk.org/jeps/12). Una función en vista previa ya está especificada y programada, pero el equipo de Java se reserva el derecho de **cambiarla o quitarla** en la versión siguiente, según lo que aprenda de quienes la prueben. Para impedir que alguien la use por accidente, el compilador la rechaza a menos que se le pida expresamente con la bandera `--enable-preview`.

La vista previa tiene una consecuencia concreta que conviene ver con un caso real, el de la **concurrencia estructurada**, que en Java 25 sigue en vista previa ([JEP 505](https://openjdk.org/jeps/505)). Este programa la usa:

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

Sin la bandera, el compilador se niega. Esto es lo que imprime, tal cual (es un fragmento, no una figura del curso, porque no compila a propósito):

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

El mensaje es claro: «es una API en vista previa y está desactivada por omisión». Con `java --enable-preview --source 25 Preview.java` el mismo programa imprime `hola`. Pero hay una trampa que no se ve en el mensaje: un `.class` compilado con funciones en vista previa lleva en su encabezado la marca de «vista previa», que se midió con `javap -v`: `minor version: 65535`, en lugar del `0` de siempre. Esa marca hace que **solo la versión exacta de Java con la que se compiló pueda ejecutarlo**. Un programa construido sobre una función en vista previa de la 25 puede dejar de funcionar al pasar a la 26. Por eso el curso fija una regla que no se discute: **nada en vista previa se usa en el código del curso**. La concurrencia estructurada aparece en la Lección 9 como un recuadro de «lo que viene», con su bandera y con la advertencia de no usarla en producción; el resto, solo lo que ya es definitivo en la 25.

### 0.4 Qué protege el compilador y qué no

Esta es la sección más importante de la lección, y la que más se parece a un mapa de minas. Java presume de ser un lenguaje con tipos estáticos, es decir, que el compilador revisa los tipos **antes** de que el programa corra. Es cierto, y es una ventaja enorme. Pero «tipos estáticos» no significa «sin errores en ejecución», y hay cuatro huecos que conviene conocer desde el primer día. Cada uno viene con un programa para correr después de la Lección 1.

#### 0.4.1 Lo que el compilador sí atrapa: los tipos

Empecemos por lo que sí protege. El programa `fig00_01.java` declara una variable que debe contener un número entero y le asigna un texto:

```java
// fig00_01.java
void main() {
  int tiempoMs = "142";
  IO.println(tiempoMs);
}
```

Este programa **no corre**: el compilador lo rechaza antes. Ejecútalo con `java fig00_01.java` y obtienes:

```text
$ java fig00_01.java
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
1 error
error: compilation failed
```

Léelo de arriba abajo, que es como lo leerás cientos de veces. La primera línea trae tres datos: el archivo (`fig00_01.java`), el número de renglón (`3`) y el diagnóstico (`incompatible types: String cannot be converted to int`, «tipos incompatibles: un `String` no puede convertirse en `int`»). Debajo, el compilador copia el renglón conflictivo y pone una flecha `^` en el punto donde se dio cuenta. Al final, `1 error` cuenta los problemas y `error: compilation failed` es el lanzador diciendo que no llegó a ejecutar nada. Observa también lo que no pasó: el programa no imprimió nada, ni siquiera un resultado parcial. Un error de compilación detiene todo **antes** de la primera instrucción.

Los renglones del programa, para que no queden en misterio: `void main()` es la forma de empezar un programa (la Lección 1 la explica a fondo), `int tiempoMs` declara una variable de tipo entero, y `IO.println` imprime un renglón. Lo importante es la comparación con tus otros lenguajes. En Python, el equivalente (`tiempo_ms: int = "142"`) corre sin quejarse, porque las anotaciones no se verifican al ejecutar. En TypeScript, el compilador lo atrapa igual que Java. En Go, también. Java está en el grupo que lo atrapa antes de correr.

#### 0.4.2 Primer hueco: los genéricos se borran

Los **genéricos** son la sintaxis de Java para decir «una lista *de textos*», «una lista *de números*»: `ArrayList<String>`, `ArrayList<Integer>`. (El tema es de la [Lección 4](04-colecciones-genericos-streams.md); aquí solo interesa una consecuencia.) El compilador usa esa información para revisar tu código. Pero después de compilar, **la información desaparece**: el compilador la borra y deja el *bytecode* sin ella. A eso se le llama **borrado de tipos** (*type erasure*).

Lo muestra `fig00_02.java`, que crea una lista de textos y una de números y pregunta si son de la **misma clase**:

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

El programa, renglón por renglón. La primera línea, `import`, le dice al compilador dónde encontrar `ArrayList`: está en el paquete `java.util` de las bibliotecas estándar. `var` pide al compilador que deduzca el tipo de la variable del valor que se le asigna (la Lección 2 lo explica). Los dos `new ArrayList<...>()` crean una lista vacía, una anunciada como de textos y otra como de números. `getClass()` devuelve la clase de un objeto **en tiempo de ejecución**, y `==` compara si son la misma. La respuesta es `true`: para la JVM, las dos listas son un `java.util.ArrayList`, sin más. El `<String>` y el `<Integer>` ya no existen cuando el programa corre.

¿Por qué se diseñó así? Por compatibilidad. Los genéricos llegaron a Java en la versión 5, y para entonces existían millones de líneas de código y bibliotecas sin ellos. Borrar la información permitió que el código viejo y el nuevo convivieran. La propia especificación reconoce esa herencia al hablar de los «tipos crudos», que se permiten solo «como una concesión a la compatibilidad con código antiguo» ([JLS, sección 4.8](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.8)). Una ventaja colateral, que señala el [tutorial oficial](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html), es que los genéricos no cuestan nada en ejecución: no se crean clases nuevas por cada combinación.

La consecuencia para ti es que algunas cosas que esperarías de un lenguaje con tipos genéricos **no se pueden hacer** en Java: preguntar en ejecución «¿esto es una lista de textos?» no tiene respuesta, porque esa información no existe. Cuando llegues a la Lección 4 y encuentres restricciones raras con los genéricos, la causa casi siempre es este borrado. Compáralo con TypeScript, donde **todos** los tipos se borran al generar JavaScript. En Java se borran solo los argumentos de los genéricos; la clase del objeto sí sobrevive, y por eso el programa pudo imprimir `java.util.ArrayList`.

#### 0.4.3 Segundo hueco: `null` sigue existiendo

Java tiene tipos, pero no impide que una variable de un tipo de objeto, como `String`, valga **«nada»**. Ese «nada» se escribe `null`. El compilador acepta que lo asignes y no te avisa cuando lo usas mal. `fig00_03.java` declara un detalle de servicio, lo deja en `null` y le pide su longitud:

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

Este programa **compila sin problemas**. El error aparece solo cuando se ejecuta el quinto renglón, y por eso el primer `IO.println` sí alcanzó a imprimir. Es la diferencia entre un error de compilación (0.4.1: no se ejecuta nada) y un error de ejecución: el programa corre hasta que topa con el problema, y se detiene ahí. El nombre del error, `NullPointerException`, significa «excepción de puntero nulo»; una **excepción** es la forma que tiene Java de avisar que algo salió mal durante la ejecución (la [Lección 5](05-errores-excepciones.md) la estudia entera). Su mensaje dice qué intentaste hacer (`Cannot invoke "String.length()"`, «no se puede invocar `String.length()`») y por qué falló (`because "<local1>" is null`, «porque `<local1>` es nulo»). Desde Java 15, la JVM da mensajes detallados como este por omisión, gracias al [JEP 358](https://openjdk.org/jeps/358).

Quizá te sorprenda que la variable se llame `<local1>` en lugar de `detalle`. No es un error de la lección: cuando ejecutas un archivo directamente con `java Archivo.java`, el compilador interno no conserva los nombres de las variables locales. La [Lección 2](02-tipos-cadenas-control.md) muestra cómo recuperarlos con otra forma de compilar. Lo importante hoy es la idea: **el compilador de Java no te protege de `null`**.

La invención de `null` la lamentó su propio autor, Tony Hoare, que en 2009 la llamó «mi error de mil millones de dólares» ([conferencia](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/)), por los fallos que provocó durante décadas. Java no la quitó, pero con los años ofrece herramientas para no depender de ella: en la Lección 3 los tipos sellados sustituyen los `null` del `revisor` por un tipo que dice explícitamente «hubo respuesta» o «no la hubo», y en la Lección 4 `Optional` cubre los casos de «a veces hay dato».

#### 0.4.4 Tercer hueco: el `+` no siempre suma

El tercero es una trampa para quien viene de otro lenguaje. `fig00_04.java` usa el operador `+` de tres formas:

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

Los tres resultados salen de una sola regla: **si alguno de los dos lados del `+` es un texto, el `+` concatena; si los dos son números, suma**; y se evalúa de izquierda a derecha. El primer renglón junta el texto `"142"` con el número `1` y da `"1421"`, un texto de cuatro caracteres, no el número 143. El segundo suma primero `1 + 42` (los dos son números) y da `43`, y recién entonces lo junta con `"ms"`. El tercero empieza con un texto, así que `"ms" + 1` ya es concatenación y da `"ms1"`, y luego `"ms1" + 42` da `"ms142"`. La regla está en la [especificación del lenguaje](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.18.1).

Lo que hace peligroso este caso no es que sea ilegal, sino que **compila sin una sola advertencia** y da un resultado plausible. Si en el `revisor` un tiempo llega como texto, `"142" + 1` no te avisará de nada: producirá `"1421"`, y ese dato falso viajará hasta el reporte. En Python, `"142" + 1` es un `TypeError` en el acto; en TypeScript, da `"1421"` igual que en Java. Java está del lado de TypeScript en este punto, y por eso la Lección 2 insiste en que cada dato del `revisor` tenga el tipo correcto desde que entra.

#### 0.4.5 Cuarto hueco: lo que el compilador nunca verá

Los tres anteriores se pueden provocar con programas de pocos renglones. Hay un cuarto grupo, más amplio: **los valores que solo se conocen en ejecución**. El compilador revisa tipos; no revisa valores. Estos fragmentos se corrieron y esto es lo que imprimen:

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

Un entero de Java tiene tamaño fijo (cuatro bytes), y si una suma se pasa del máximo, el resultado **da la vuelta** al mínimo, sin error. La especificación lo dice de frente: «los operadores enteros no indican desbordamiento de ninguna manera» ([JLS, sección 4.2.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2.2)). Otros dos casos, medidos igual, cada uno en su propio archivo con un `void main()` que solo contiene el renglón indicado:

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

Los puntos suspensivos sustituyen nueve renglones internos del JDK, cuyos números cambian con cada actualización. Dividir un entero entre cero es una excepción en ejecución; pedir el carácter número 10 de un texto de cuatro caracteres también. En ninguno de los dos casos el compilador podía saberlo sin ejecutar el programa. El resumen de la sección cabe en una tabla:

| Qué | ¿Cuándo se detecta? | Ejemplo de esta lección |
|---|---|---|
| Un texto donde va un número | Al compilar | `int tiempoMs = "142";` (fig00_01) |
| Un `null` usado como si fuera un objeto | Al ejecutar | `detalle.length()` (fig00_03) |
| Un `+` que concatena en vez de sumar | Nunca: el resultado es válido | `"142" + 1` (fig00_04) |
| El tipo de los elementos de una lista | Al compilar, y luego se borra | `ArrayList<String>` (fig00_02) |
| Un entero que se pasa de su máximo | Nunca: da la vuelta | `Integer.MAX_VALUE + 1` |
| División entera entre cero | Al ejecutar | `1 / 0` |

El resto del curso trata de hacer que esa columna del medio sea lo más temprana posible: mover errores de «nunca» o «al ejecutar» a «al compilar». Los tipos sellados de la Lección 3 hacen que olvidar un caso sea un error del compilador. Las colecciones inmutables de la Lección 4 impiden cambios accidentales. Las pruebas de la Lección 7 atrapan lo que el compilador no puede. No es casualidad: es el método.

### 0.5 Java frente a lo que ya conoces

Con el mapa anterior, la comparación con los otros lenguajes del curso deja de ser vaga. Esto es lo que cambia, y lo que no:

| | Go | TypeScript | Python | **Java** |
|---|---|---|---|---|
| ¿Qué produce el compilador? | un ejecutable para un sistema y un procesador | JavaScript (los tipos desaparecen) | *bytecode* del intérprete, de forma automática | *bytecode* de la JVM, en un paso explícito |
| ¿Cuándo se revisan los tipos? | al compilar | al compilar (con `tsc`) | no se revisan al ejecutar | al compilar |
| ¿Qué ejecuta el programa? | el procesador directamente | Node o un navegador | el intérprete de Python | la JVM |
| ¿Hay recolector de basura? | sí | sí (el motor de JavaScript) | sí | sí |
| ¿Cómo se entrega? | copiar el ejecutable | copiar el JavaScript y Node | copiar el código y el intérprete | copiar un JAR (un archivo con `.class` dentro) y tener una JVM |

Dos cosas saltan a la vista. La primera: si vienes de Python, el modelo de «código fuente, *bytecode*, máquina virtual» **ya lo conoces**; Python también compila a *bytecode* y lo ejecuta en una máquina virtual. Lo que cambia en Java es que el paso de compilar es explícito y visible, y que en medio revisa los tipos. La segunda: si vienes de Go, la diferencia es la JVM como intermediaria. Un ejecutable de Go lleva todo dentro; un programa de Java necesita una JVM instalada donde corra, a cambio de ser portable entre sistemas con el mismo archivo.

Existe, además, una vía para producir un ejecutable nativo a partir de un programa de Java (compilación anticipada con GraalVM). No se usa en este curso. El blog de Hábil explica [cuándo conviene y cuándo no compilar Java a nativo](https://www.habil.mx/es/blog/java-nativo-cuando-si-cuando-no/); léelo cuando termines la Lección 10, no antes.

### 0.6 Lo que viene en el resto del curso

Todo lo anterior se pone en práctica en las lecciones siguientes. El curso construye un solo programa, el `revisor`, y cada lección agrega una pieza:

| Lección | Qué se agrega | Qué hueco de esta lección ayuda a cerrar |
|---|---|---|
| 1 | Instalas Java 25 y escribes y compilas tu primer programa | el camino de 0.1, en tu terminal |
| 2 | Tipos, cadenas y control de flujo | `null`, `==` contra `equals`, el `+` que concatena |
| 3 | `record`, `enum` y tipos sellados | `null` sustituido por tipos que dicen todos sus casos |
| 4 | Colecciones, genéricos y *streams* | el borrado de tipos, listas que no cambian |
| 5 | Errores y excepciones | los errores de ejecución, tratados en vez de tolerados |
| 6 | Proyecto con Maven (la herramienta que construye proyectos de Java) | el compilado como producto: paquetes, un `pom.xml` y un `.jar` |
| 7 | Pruebas con JUnit | lo que el compilador no ve, atrapado por pruebas |
| 8 | HTTP y JSON | datos que llegan de fuera y hay que validar |
| 9 | Concurrencia | revisar todos los servicios a la vez |
| 10 | El programa terminado | línea de comandos, servidor, calidad y un JAR |

Hay un acuerdo práctico más. Los programas de las Lecciones 1 a 5 se corren con `java Archivo.java`, sin herramientas adicionales. Desde la Lección 6 el proyecto vive en una estructura estándar, con Maven. El curso usa **sangría de dos espacios** en todo el código, la de la guía de estilo de Google para Java, porque en la Lección 10 una herramienta verifica ese estilo y sería absurdo confesar entonces que todo lo anterior estaba escrito de otra manera. Mucho código de internet usa cuatro espacios; funciona igual.

## El error que vas a ver

Esta lección tiene un error principal, el del compilador que se niega, y uno de ejecución que conviene reconocer a primera vista. Ambos son de las secciones anteriores.

**El error principal: `incompatible types`.** Es el de `fig00_01.java`:

```text
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
```

Se lee así: el archivo y el renglón (`fig00_01.java:3`), qué ocurrió (un `String` no se puede convertir en `int`), el renglón conflictivo, y la flecha en el punto exacto. Se arregla de dos formas, según lo que quisieras: si el dato era un texto, la variable debía ser de tipo `String`; si querías el número, hay que convertir el texto explícitamente (de eso se ocupa la Lección 2). Lo que **no** se hace es ignorarlo: un error de compilación nunca se «pasa por alto», porque el programa simplemente no corre.

**Un error de ejecución que ya puedes reconocer: `NullPointerException`.**

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "<local1>" is null
```

Se lee de izquierda a derecha: en el hilo principal (`main`) se produjo una `NullPointerException`; intentaste invocar `String.length()` y no se pudo porque lo que había en la variable era `null`. Debajo, la línea que empieza con `at` dice **dónde**: `fig00_03.main(fig00_03.java:5)`, o sea, el método `main` del archivo, renglón 5. Revisa ese renglón y pregúntate de dónde pudo llegar un `null`.

## Lo que se hace mal

**Instalar «Java» sin saber qué se instala.** Quien teclea `sudo apt install default-jre` obtiene solo la parte de ejecución, y en Mint 22 de una versión anterior a la que usa el curso. Para programar, el JDK; para el curso, el de Java 25.

**Usar la versión más reciente por ser la más reciente.** Java 27 salió el 15 de septiembre de 2026 y no es LTS. Para aprender, para un servidor o para un programa que va a vivir años, la LTS vigente. Si ya tienes la 27 instalada, no pasa nada grave con este curso, pero la Lección 1 te dice cómo asegurarte de qué versión estás usando.

**Creer que «tiene tipos» significa «no falla en ejecución».** Los tipos atrapan una familia de errores, no todos. Los de 0.4 existen en cualquier programa de Java, hecho por quien sea.

**Copiar un tutorial sin mirar su fecha.** En internet circulan versiones de «Hola mundo» de hace veinte años que obligan a escribir una clase y un método `public static void main(String[] args)`. Funcionan, pero para un archivo de un solo programa ya no hace falta nada de eso, como verás en la Lección 1. Lo mismo vale para las bibliotecas: casi todo lo que hay publicado sobre JSON o pruebas está escrito para versiones anteriores a las del curso. Cuando una receta no compile, lo primero que se revisa es la versión.

**Usar una función en vista previa «porque funciona».** Funciona hoy, con la bandera, en esa versión exacta. Según el JEP 12, puede cambiar o desaparecer en la siguiente. No en código que alguien más va a mantener.

**Confundir Java con JavaScript.** Comparten letras del nombre y la mitad de las llaves, y nada más. Son lenguajes distintos, con modelos de ejecución distintos y con una relación histórica que nadie necesita saber para usar uno de los dos.

**Esperar que «escribe una vez, ejecuta donde sea» incluya el entorno.** El `.class` es portable; la codificación de la salida, las rutas y los saltos de línea no lo son por sí solos. Se mide, no se supone.

## Ejercicios

Los ejercicios de esta lección no requieren Java: el primero es de lectura, el segundo se contesta **antes** de correr nada y se comprueba después de la Lección 1, y el tercero y el cuarto se contestan por escrito, con tus palabras. La mejor manera de aprovechar el segundo es escribir tu predicción en la [bitácora](bitacora.md) y compararla después.

### Ejercicio 1 — Los datos del `revisor` (proyecto)

Esta es la tabla que producirá el `revisor` al terminar el curso. Para cada columna, indica si el dato es un **número**, un **texto** o puede ser **nada**. Fíjate sobre todo en el tiempo de `reportes`.

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Después responde: ¿qué haría Java con `"142" + 1`, si un tiempo llegara como texto? ¿Y qué problema habría si un programa guardara el tiempo de `reportes` como `null` y luego intentara formatearlo?

### Ejercicio 2 — ¿Cuándo se entera el programa?

Para cada uno de estos seis fragmentos, predice si (a) el compilador lo rechaza, (b) el programa compila y falla al ejecutarse, o (c) compila, corre y da un resultado sin avisar. Si es (b), di qué excepción esperas.

1. `int tiempoMs = "142";`
2. `String detalle = null;` seguido de `detalle.length()`.
3. `Integer.MAX_VALUE + 1`
4. `var palabras = new java.util.ArrayList<String>();` seguido de `palabras.add(5);`
5. `1 / 0` (entre enteros).
6. `"hola".charAt(10)`

### Ejercicio 3 — Elige la versión

Decide qué versión de Java usarías en cada caso y por qué: (a) un programa nuevo que tu equipo va a mantener varios años; (b) tienes instalado Java 27 y quieres seguir este curso; (c) un artículo usa `StructuredTaskScope` y quieres probarlo en tu máquina.

### Ejercicio 4 — Explícalo en cinco frases

Un compañero que programa en Go te pregunta qué pasa entre «escribí un `.java`» y «el programa corre». Contéstale en cinco frases como máximo, usando las palabras *código fuente*, `javac`, *bytecode*, JVM y compilador JIT. Sin mirar la lección.

## Soluciones

### Solución 1

Las columnas, en orden:

- **SERVICIO** es **texto** (`catalogo`, `pagos`...). **ESTADO** es **texto** que solo puede valer una de tres cosas (`ARRIBA`, `LENTO`, `CAIDO`); en la Lección 3 se vuelve un tipo propio que no permite otras.
- **TIEMPO** es un **número** (142, 2300, 87) que se muestra con una unidad (`142ms`, `2.3s`) y **puede ser nada**: `reportes` no respondió y no tiene tiempo. Esa raya `—` representa un dato que no existe.
- **DETALLE** es **texto** aunque se vea un número: el `200` es el código de estado, pero en la tabla se muestra como texto, igual que `sin conexión`.

Con `"142" + 1`, Java **no suma**: concatena y da el texto `"1421"` (fig00_04). Como compila sin avisar, un dato falso entraría al reporte sin que nadie se enterara. Y con el tiempo de `reportes` guardado como `null`, formatearlo con algo como `tiempo.length()` lanza una `NullPointerException` en ejecución (fig00_03), porque el compilador no protege de eso. Las dos situaciones son la razón de que las Lecciones 2 y 3 existan: en la 2 se aprende a escribir el código con cuidado ante `null`, y en la 3 se cambia el diseño para que «no hubo respuesta» sea un caso explícito del tipo y no un `null` escondido.

### Solución 2

Tras la Lección 1, puedes comprobarlas con `jshell` o con un archivo de programa. Las respuestas, medidas con Temurin 25.0.4:

1. **(a)** El compilador lo rechaza: `incompatible types: String cannot be converted to int` (fig00_01).
2. **(b)** Compila y falla al ejecutar con `java.lang.NullPointerException` (fig00_03).
3. **(c)** Compila, corre y da `-2147483648`, sin aviso: el entero da la vuelta.
4. **(a)** El compilador lo rechaza. Es el valor del genérico: la lista es de textos y `5` no lo es. El mensaje empieza con `error: no suitable method found for add(int)` y sigue con una lista de las variantes de `add` que probó, cada una con su razón (la que importa dice `argument mismatch; int cannot be converted to String`). Es un ejemplo de que el compilador sí usa los genéricos, aunque luego los borre.
5. **(b)** Compila y falla al ejecutar con `java.lang.ArithmeticException: / by zero`.
6. **(b)** Compila y falla al ejecutar con `java.lang.StringIndexOutOfBoundsException: Index 10 out of bounds for length 4`.

Si fallaste en el 3, es normal: es el único donde el programa «funciona» y da un número equivocado, y por eso es el más peligroso de los seis.

### Solución 3

(a) **Java 25**, la LTS vigente: tendrá correcciones de seguridad por lo menos hasta septiembre de 2031. Una versión no LTS dejaría de mantenerse a los seis meses.

(b) **No hace falta desinstalar nada, pero tampoco conviene usarla para el curso.** Java 27 no es LTS y el curso verifica sus programas con la 25; lo seguro es instalar Temurin 25 (con SDKMAN, la Lección 1, conviven las dos y eliges cuál usar) y comprobar con `java --version` cuál está activa. Lo que no hay que hacer es suponer que «la más nueva sirve para todo».

(c) **En la versión 25, con `--enable-preview`**, sabiendo que es una función en vista previa: la API puede cambiar en la siguiente versión (de hecho cambió entre la 25 y la 26, según el [JEP 525](https://openjdk.org/jeps/525)), el `.class` resultante solo corre en esa versión exacta y no se debe usar en un programa que otro vaya a mantener.

### Solución 4

Una respuesta posible: «Escribes el *código fuente* en un `.java`. El compilador `javac` lo revisa (sobre todo los tipos) y lo traduce a *bytecode*, que va en un `.class`. Ese *bytecode* no es código de un procesador real: lo ejecuta la JVM, que existe para cada sistema operativo. Mientras el programa corre, la JVM detecta los métodos que se usan mucho y los convierte a código de máquina con el compilador JIT. Por eso el mismo `.class` corre en cualquier sistema que tenga una JVM, a diferencia de un ejecutable de Go, que ya trae el código de su procesador».

## Cómo sé que lo logré

- [ ] Dibujas de memoria el camino `.java` → `javac` → `.class` → `java` → JVM, y dices qué hace cada flecha.
- [ ] Dices la diferencia entre JDK, JRE y JVM, y cuál se instala para este curso.
- [ ] Explicas por qué hay una Java 27 y el curso usa la 25, con la palabra LTS.
- [ ] Sabes qué es una función en vista previa y qué bandera exige.
- [ ] Para cada fila de la tabla de 0.4.5, dices cuándo se detecta el problema.
- [ ] Después de la Lección 1, corres `java fig00_01.java`, `fig00_02.java`, `fig00_03.java` y `fig00_04.java` y tus salidas coinciden con las de esta lección, salvo la ruta.
- [ ] Predices en la bitácora qué imprime `"ms" + 1 + 42` antes de ejecutarlo, y compruebas.
- [ ] Anotaste en tu bitácora una cosa que creías de la JVM y resultó distinta.

## Resumen

Responde sin mirar la lección:

1. ¿Qué contiene un `.class` y quién lo ejecuta?
2. ¿Qué agrega el JDK al JRE? ¿Por qué se instala el JDK en este curso?
3. ¿Qué es OpenJDK y qué es una distribución como Temurin?
4. ¿Qué significa LTS? ¿Por qué Java 27 no es la versión del curso?
5. ¿Qué pasa con un `.class` compilado con funciones en vista previa?
6. ¿En qué se diferencian un error de compilación y uno de ejecución? Da un ejemplo de cada uno.
7. ¿Qué es el borrado de genéricos y qué prueba `fig00_02.java`?
8. ¿Por qué `"142" + 1` es `"1421"` y no `143`?
9. ¿Por qué el compilador de Java no te avisa de un `null` mal usado?

## Para leer más

- [JEP 512, «Compact Source Files and Instance Main Methods»](https://openjdk.org/jeps/512): la propuesta que hace que en Java 25 un programa pueda escribirse sin clase ni `public static`, que es el estilo de este curso. Se lee mejor después de la Lección 1. Consultado el 7 de octubre de 2026.
- [JEP 12, «Preview Features»](https://openjdk.org/jeps/12): qué es la vista previa, por qué existe y qué garantías no da. Consultado el 7 de octubre de 2026.
- [Especificación de la Máquina Virtual de Java, SE 25, capítulo 4](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html): el formato exacto del archivo `.class`, incluido el número mágico y la versión. Consultado el 7 de octubre de 2026.
- [Temurin Support, Adoptium](https://adoptium.net/support/): el calendario de versiones y el fin de soporte de cada una, de donde sale la tabla de 0.3. Consultado el 7 de octubre de 2026.

---

**Siguiente:** [Lección 1 — Instalar Java 25 en tu Linux Mint](01-instalacion.md)
