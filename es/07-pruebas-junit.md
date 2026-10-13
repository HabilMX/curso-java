# Lección 7 — Pruebas con JUnit

**Tiempo:** 90 min (justo: cada prueba se ve primero en rojo y después en verde, y eso toma su tiempo; no lo saltes)

**Qué construyes:** el `revisor` gana 18 pruebas automáticas que se ejecutan con un solo comando y vigilan sus reglas: la clasificación, el formato del tiempo, la lectura de `servicios.txt` y el resumen

**Qué aprendes:** qué es una prueba automática y sus tres tiempos; JUnit 6 en el `pom.xml`, con su BOM y en alcance `test`; `@Test` y `assertEquals`; `@ParameterizedTest` con `@CsvSource` (la tabla de casos) y por qué los casos se eligen en la frontera; `assertThrows` y `@TempDir`; ver una prueba en rojo antes de verla en verde; y que **el número de pruebas se lee del informe, no del código de salida**

**De dónde vienes.** Traes el proyecto de Maven de la [Lección 6](06-proyecto-maven.md): trece archivos en el paquete `mx.habil.revisor`, un `pom.xml` con tres *plugins* de versión fija, y `mvn -B verify` que compila, empaqueta `revisor-0.6.0.jar` y termina en `BUILD SUCCESS`. En medio de esa salida había un renglón que decía `No tests to run.`: tu proyecto ya tiene dónde poner pruebas (la carpeta `src/test/java`, vacía) y quién las corra (el *plugin* *Surefire*), pero ni una sola prueba. Y eso deja una pregunta incómoda: ¿cómo sabes que el `revisor` funciona? Lo corriste y miraste la tabla. **La única prueba que tiene tu programa eres tú, mirando una pantalla.** Hoy lo cambias: pruebas que revisan por ti, en una fracción de segundo y en cada cambio, que cada regla sigue haciendo lo que dice. Trabajas en tu carpeta `~/curso-java/revisor/`.

**Qué no hace esta lección.** No prueba `Revision`, la clase que simula la red esperando con `Thread.sleep`: tardaría lo mismo que el programa, y lo que vigila (el `Simulador`) desaparece en la [Lección 8](08-http-y-json.md), que prueba `revisar` contra un servidor de verdad levantado dentro de la prueba. Tampoco usa bibliotecas para fingir objetos ni mide la cobertura de las pruebas: con funciones pequeñas y datos fijos no hacen falta. Y no agrega nada al programa: JUnit vive solo en las pruebas, y el `.jar` que se entrega no cambia de contenido.

**Sobre las corridas.** Todo lo de esta lección se ejecutó con Temurin 25 y Maven 3.10.0, en macOS, con la terminal en UTF-8. Las salidas de Maven se muestran abreviadas: se conserva lo que cada sección analiza, y los tres puntos (`...`) marcan lo que se quitó. Dos números de esas salidas van a ser distintos en tu pantalla y no importa: los `Time elapsed` (cambian en cada corrida) y, en las pruebas que fallan, el número de renglón de la traza (`ResumenTest.java:32`), que es el de tu archivo y solo coincide si lo escribiste igual que la lección, renglón por renglón. Todo lo demás, incluidos los conteos de pruebas, debe coincidir. Los programas están en [`programas/07-pruebas-junit/`](../programas/07-pruebas-junit), en el repositorio del curso.

## Al terminar vas a poder

- Explicar qué es una prueba automática, cuáles son sus tres tiempos y qué agrega un *framework* de pruebas como JUnit.
- Declarar JUnit 6 en el `pom.xml` con su BOM y en alcance `test`, y explicar por qué no forma parte del programa.
- Escribir pruebas con `@Test` y `assertEquals`, con el valor esperado primero, y una tabla de casos con `@ParameterizedTest` y `@CsvSource`, eligiendo los casos alrededor de la frontera.
- Probar un error con `assertThrows` y un archivo con `@TempDir`.
- Ver una prueba en rojo antes de verla en verde, y saber qué revertir para provocarlo.
- Correr todas las pruebas, una clase o una sola prueba, y leer del informe cuántas se ejecutaron.
- Explicar tres formas de obtener un verde que no probó nada, y cómo te proteges de cada una.

## El porqué antes del cómo

Es lunes y alguien te pide que el umbral de «lento» pase de un segundo a 800 milisegundos. Es un cambio de una línea: `UMBRAL_LENTO_MS`. Lo haces, corres el `revisor`, miras la tabla… y todo parece bien. ¿Lo está? Con cuatro servicios de datos fijos, `inventario` seguía siendo lento y los demás seguían arriba, así que la tabla se ve igual que ayer. Pero ¿qué pasó con un servicio que responde en exactamente 800 milisegundos? ¿Cuenta como lento o no? La tabla de hoy no lo sabe: ningún servicio de tus datos mide 800. Si te equivocaste por un `>` donde iba un `>=`, el error espera a que un servicio real caiga justo en la frontera, y ese día lo descubrirá quien use tu programa, y no tú.

Mirar la tabla tiene tres defectos, y los tres empeoran con el tiempo. **Solo ve los datos que tienes**, y los errores viven en los que no tienes (el 800 exacto, el archivo con un renglón mal escrito, el caso en que nadie responde). **Depende de que alguien mire**, con atención, cada vez, y nadie mira con atención la vigésima vez. Y **no deja constancia**: si mañana otra persona cambia el formato del tiempo, no hay nada que le avise que rompió lo que tú comprobaste ayer. Una prueba automática corrige las tres cosas a la vez: escribe el caso una vez, lo comprueba en cada cambio sin cansarse, y queda en el proyecto para la siguiente persona.

La lección tiene seis partes: **qué es una prueba y cómo entra JUnit al proyecto** (7.1); **la primera prueba**, y verla en rojo (7.2); **la tabla de casos** (7.3); **probar un archivo y su error** (7.4); **leer el informe**, que es la parte que más se descuida (7.5); y **el paso del proyecto** (7.6). Con ese destino en mente, esto es lo que vas a ver al final de la lección, donde la Lección 6 decía `No tests to run.`:

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

La línea que importa es `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. Al terminar sabrás de dónde sale cada uno de esos números, qué pasa cuando alguno se mueve, y por qué ninguno de ellos se lee del «éxito» que Maven anuncia al final.

## Los conceptos

### 7.1 Qué es una prueba automática y qué hace falta

#### 7.1.1 Los tres tiempos de una prueba

Vuelve al lunes del principio: el umbral pasó a 800 milisegundos y nadie sabe si un servicio de exactamente 800 cuenta como lento. Una **prueba automática** es la forma de contestar esa pregunta sin esperar a que pase en producción: un pequeño programa que prepara un dato conocido, llama a la parte del programa que se quiere vigilar, y compara lo que obtuvo con lo que debía obtener. Tiene siempre tres tiempos, que en inglés se abrevian AAA: **arreglar** (*arrange*) los datos de entrada, **actuar** (*act*) llamando al código, y **afirmar** (*assert*) que el resultado es el esperado. Si la afirmación se cumple, la prueba pasa; si no, falla y dice qué esperaba y qué obtuvo.

Escribir ese programa pequeño no es difícil. Lo que da trabajo es todo lo demás: encontrar todas las pruebas del proyecto, correrlas una por una sin que el fallo de una detenga a las otras, y reportar cuántas pasaron. Eso lo hace un ***framework* de pruebas**, y el de Java es **JUnit**, que va en su versión 6 (la 6.1.3 es la vigente a octubre de 2026; requiere Java 17 o superior, según su [guía de usuario](https://docs.junit.org/current/user-guide/)). Pone cuatro cosas a tu disposición: anotaciones como `@Test` para marcar qué métodos son pruebas, métodos como `assertEquals` para afirmar, un motor que descubre y ejecuta las pruebas, y un informe de lo que ocurrió. Una **anotación** es una etiqueta que se escribe con `@` antes de una declaración y que las herramientas leen; no cambia lo que el método hace, solo le dice a JUnit cómo tratarlo.

#### 7.1.2 JUnit en el `pom.xml`

Para tenerlo en el proyecto hay que declarar la dependencia en el `pom.xml`. Este es el archivo completo, ya con JUnit. La parte nueva está en `<dependencyManagement>` y `<dependencies>`, y la versión del proyecto sube a `0.7.0`, como prometió la Lección 6: el `.jar` se llamará `revisor-0.7.0.jar`.

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

JUnit no es un solo artefacto, sino una familia de ellos (el API para escribir pruebas, el motor que las ejecuta, el soporte para tablas de casos…) y todos deben ser de la misma versión para funcionar juntos. Para no escribir esa versión seis veces, y no equivocarse en una, Maven ofrece el **BOM** (*bill of materials*, «lista de materiales»): un `pom.xml` que no contiene código sino solo una lista de versiones compatibles. El bloque `<dependencyManagement>` con `<type>pom</type>` y `<scope>import</scope>` importa esa lista completa: a partir de ahí, cualquier artefacto de JUnit se declara **sin versión** y toma la que el BOM fija. La dependencia en sí es una sola, `junit-jupiter`, un artefacto «paraguas» que trae el API, el soporte para tablas de casos y el motor. Es la forma que la [guía de JUnit para Maven](https://docs.junit.org/current/running-tests/build-support.html) recomienda para alinear las versiones.

La línea que más importa es `<scope>test</scope>`. El **alcance** (*scope*) de una dependencia dice dónde está disponible: la de alcance `test` solo existe cuando se compilan y se corren las pruebas, y **no** forma parte del programa. Quien ejecute el `revisor` no necesita JUnit, y no se le obliga a descargarlo. Como el compilador ahora tiene a JUnit en `src/test/java` y no en `src/main/java`, el `import org.junit...` que falló con el lanzador en la Lección 6 (la figura `fig06_01`) funciona en las pruebas y seguiría fallando en el programa, que es exactamente lo que se quiere.

#### 7.1.3 Qué se prueba y qué no

Antes de escribir la primera, una decisión que vale para todo el curso: **qué partes del `revisor` merecen una prueba hoy**. La respuesta sale de mirar qué hace cada clase. `Clasificador`, `Formato` y `Resumen` son **reglas**: reciben un dato y devuelven otro, siempre el mismo para la misma entrada, sin tocar la red, el disco ni el reloj. Son las más fáciles de probar y las más valiosas, porque ahí viven las decisiones que alguien va a querer cambiar un lunes (el umbral, el formato del tiempo, cómo se cuenta un promedio). `Configuracion` es una regla con un borde hacia fuera: lee un archivo, y lo que hay que vigilar son sus mensajes de error, que son lo único que ve quien se equivoca al escribir `servicios.txt`. Las cuatro tienen prueba en esta lección.

`Revision` y `Simulador` no la tienen, y la razón no es pereza. `Simulador` es un andamio: inventa cuatro respuestas para que el programa tenga algo que revisar mientras no hay red, y en la Lección 8 desaparece. Una prueba de un andamio comprueba que el andamio dice lo que tú le escribiste, y se tira con él. `Revision.revisar`, por su parte, **espera** de verdad lo que dice cada servicio (`Thread.sleep`): una prueba de los cuatro servicios tardaría lo mismo que el programa, unos 2.5 segundos, y las 18 pruebas de esta lección corren completas en una fracción de segundo. Esa velocidad no es un lujo: es lo que hace que las corras en cada cambio, en vez de «al final». Cuando `revisar` haga una petición real, en la Lección 8, tendrá su propia prueba contra un servidor levantado dentro de la prueba misma, que contesta lo que la prueba quiere y cuando quiere.

Hay una tercera razón para empezar por las reglas, que se nota en la forma de las pruebas que vienen: las cuatro clases de prueba viven en el **mismo paquete** que lo que prueban, `mx.habil.revisor`. Gracias a eso ven las clases de visibilidad de paquete que la Lección 6 dejó cerradas al mundo, y no hubo que abrir nada para poder probarlo. Las pruebas usan el programa como lo usaría otra clase del paquete, y nada más.

### 7.2 La primera prueba: el resumen

#### 7.2.1 `ResumenTest`, renglón por renglón

Empecemos por la más sencilla del proyecto. El resumen de una revisión cuenta servicios, y su promedio incluye solo a los que respondieron. Dos pruebas lo vigilan: los cuatro servicios de siempre (el promedio tiene que ser 843), y el caso borde de que **nadie** conteste, donde el promedio no existe y el resumen escribe una raya.

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

Se lee de arriba abajo. La clase vive en el mismo paquete que lo que prueba y se llama `ResumenTest`: el nombre termina en `Test` porque así lo busca *Surefire*, el *plugin* que corre las pruebas (por omisión incluye los archivos que empiezan con `Test` o terminan en `Test`, `Tests` o `TestCase`, según su [documentación](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html); en 7.5 verás qué pasa con una prueba que no se llama así). La clase no es `public`: JUnit 6 no lo exige, y por la regla de la Lección 6 (6.1.3) se queda con la visibilidad más estrecha.

El `import static` es nuevo. Un `import` normal te deja usar una **clase** por su nombre corto; un `import static` te deja usar un **método estático** de una clase sin escribir la clase delante. Con `import static org.junit.jupiter.api.Assertions.assertEquals;` puedes escribir `assertEquals(...)` en lugar de `Assertions.assertEquals(...)`. Las pruebas lo usan en todas partes, así que se vuelve costumbre.

`private static Resultado resultado(String nombre, Respuesta respuesta)` es un **auxiliar**: una función que las dos pruebas comparten para armar un `Resultado` sin repetir tres líneas cada vez. Es privado porque solo le importa a esta clase. Y cada método con `@Test` es una prueba: tiene un nombre que **dice lo que se afirma** (`sinNingunaRespuestaElPromedioEsUnaRaya`, no `prueba2`), no recibe parámetros ni devuelve nada, y respeta los tres tiempos separados por líneas en blanco: se arma la lista, se llama a `Formato.resumen(Resumen.de(resultados))`, y se afirma. En `assertEquals(esperado, actual)` **el valor esperado va primero**; si los pones al revés la prueba pasa y falla igual, pero el mensaje de error dirá lo contrario de lo que ocurrió.

Desde la raíz del proyecto, con `-Dtest=ResumenTest` para correr solo esta clase:

```text
$ mvn -B test -Dtest=ResumenTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`. Dos pruebas, ninguna falló. Ya sabes que pasan. Falta saber si **pueden fallar**.

#### 7.2.2 Verla en rojo antes de verla en verde

Una prueba que nunca has visto fallar no demuestra nada. Puede estar comparando lo equivocado, o comparando algo que siempre es verdad, y su verde sería un adorno. La única forma de saber que una prueba vigila lo que dice vigilar es **romper a propósito el código que vigila y comprobar que se pone roja**. Hazlo ahora con las del resumen. En `Resumen.java`, el promedio se calcula solo con los servicios que respondieron: de eso se encarga el `filter(OptionalLong::isPresent)`. Reemplaza esas dos líneas (`.filter(OptionalLong::isPresent)` y `.mapToLong(OptionalLong::getAsLong)`) por una sola, `.mapToLong(t -> t.orElse(0))`: ahora el servicio que no respondió cuenta como **cero milisegundos**, y vuelve a correr:

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

Ahora sí hay mensajes, y son de los buenos. `Failures: 2`: fallaron las dos pruebas, cada una por una consecuencia distinta del mismo error. La primera dice `expected: <… promedio 843ms> but was: <… promedio 632ms>`: esperaba 843, obtuvo 632; y 632 es `2529 / 4`, la suma de los tiempos entre los cuatro servicios en lugar de entre los tres que contestaron. La segunda dice `expected: <… promedio —> but was: <… promedio 0ms>`: cuando nadie responde, el resumen debe decir que no hay promedio, y el cambio lo vuelve «cero milisegundos», un número que parece dato y no lo es. El renglón que empieza con `at` es la traza: dice en qué archivo y en qué renglón de la prueba estaba la afirmación que falló (`ResumenTest.java:32`), que es lo primero que abres. Con esos dos mensajes en la mano sabes dónde buscar, sin abrir el depurador. (Dos pruebas que caen por una misma causa también cuentan algo: ese defecto se notaba desde dos ángulos, y la segunda prueba existe justo por eso.)

Deshaz el cambio y confirma que vuelve a verde. Este ejercicio de **revertir y ver el rojo** es la costumbre que más barato te sale de todo el curso: toma un minuto y te dice si tus pruebas valen algo. En el curso se repetirá cada vez que se escriba una prueba nueva.

### 7.3 La tabla de casos: `@ParameterizedTest` y `@CsvSource`

#### 7.3.1 Una prueba, muchas filas

La clasificación de un servicio es una regla con una frontera: mil milisegundos o más es lento. Para vigilarla bien hay que probar varios casos, y todos tienen la misma forma (entra un código y un tiempo, sale un estado). Escribir seis métodos casi idénticos es un ruido que esconde lo interesante. JUnit tiene una forma mejor, la **prueba parametrizada**: se escribe el método una vez, con parámetros, y una **tabla de casos** con los valores:

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

`@ParameterizedTest` reemplaza a `@Test` y le dice a JUnit que este método se ejecutará varias veces. `@CsvSource` es la tabla: cada texto de la lista es una **fila**, y las comas separan las **columnas** (CSV viene de *comma-separated values*, «valores separados por comas»). JUnit toma cada fila, convierte el texto de cada columna al tipo del parámetro que le corresponde —`int`, `long`, y hasta `Estado`, que reconoce por el nombre de la constante— y llama al método una vez por fila. Son seis llamadas, así que el informe contará **seis pruebas** de esta sola declaración, más la séptima: la de `SinRespuesta`, que va como `@Test` aparte porque no tiene la misma forma (no hay código ni tiempo).

#### 7.3.2 Los casos se eligen en la frontera

La prueba del formato del tiempo es la misma idea con una columna de entrada y una de texto esperado. Sus cuatro filas son las que importan alrededor del cambio de unidad: 87 y 999 se escriben en milisegundos, 1000 y 2300 en segundos.

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

Los casos no se eligieron al azar. Se eligieron **alrededor de la frontera**: 142 es un servicio normal; 999 es el último valor que sigue arriba; 1000 es el primero que es lento (el caso que importa de todos: es el que detecta un `>` donde iba un `>=`); 2300 es claramente lento; 503 y 404 son errores del servidor y del cliente que, sin importar lo rápidos que contesten, dejan al servicio caído. Los **valores frontera** son donde se esconden los errores de uno-más-uno, y una tabla que no los incluya deja el borde sin vigilar. Compruébalo de la forma que ya sabes: en `Clasificador.java`, cambia `tiempoMs >= UMBRAL_LENTO_MS` por `tiempoMs > UMBRAL_LENTO_MS` y corre todo el proyecto:

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

Léelo con calma, porque es el informe de una falla real y tiene cuatro cosas que conviene reconocer a primera vista. Primero, **qué clase falló**: `ClasificadorTest`, con `Tests run: 7, Failures: 1`; las otras tres clases siguen en verde: el fallo de una no detiene a las demás. Segundo, **qué prueba**: `clasificaUnaRespuesta(int, long, Estado)[3]`, donde `[3]` es el **número de la fila de la tabla**: la tercera, `200, 1000, LENTO`. Tercero, **qué pasó**: `expected: <LENTO> but was: <ARRIBA>`. El servicio de 1000 milisegundos debía ser lento y salió arriba, porque con `>` el 1000 ya no cuenta. Y cuarto, la línea de cierre: `Tests run: 18, Failures: 1` y `BUILD FAILURE`, con el código de salida distinto de cero que hace que cualquier servidor de integración se ponga rojo. Un solo cambio de un carácter, una sola fila en rojo, y el informe te dice cuál: eso es lo que da la tabla. Revierte el cambio antes de seguir.

#### 7.3.3 Correr una sola prueba

Cuando una fila se pone roja, lo que quieres es repetir **esa** prueba mientras la arreglas, no las 18. Ya usaste `-Dtest=ResumenTest` para correr una clase; con un `#` y el nombre del método eliges una sola prueba de la clase:

```text
$ mvn -B test -Dtest=ClasificadorTest#sinRespuestaEsCaido
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 1`: de las siete pruebas de la clase corrió solo la que pediste. Fíjate también en la fase: `mvn -B test` y no `verify`. Como `test` va antes que `package` en el ciclo de vida, Maven se detiene después de las pruebas y no arma el `.jar`, que para repetir una prueba no hace falta. Es la forma de trabajar mientras arreglas algo: una prueba, rápido, muchas veces. Y es también una trampa si se te olvida que la usaste: un `Tests run: 1` en verde no dice nada de las otras 17. **Antes de dar algo por terminado se corre todo**, con `mvn -B verify` y sin `-Dtest`, y se lee el total.

### 7.4 `assertThrows` y `@TempDir`: probar un archivo y su error

La tercera familia de pruebas vigila lo que el `revisor` hace con el archivo de configuración. Aquí hay dos cosas nuevas. La primera es que probar un archivo exige tener un archivo, y no se quiere escribir en tu carpeta de verdad ni dejar basura. La segunda es que lo que se quiere afirmar no es un valor devuelto sino una **excepción**: que ciertos archivos hagan fallar a la lectura, y con el mensaje exacto.

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

**`@TempDir Path carpeta`** pide a JUnit una carpeta temporal: antes de cada prueba la crea nueva y vacía, y después la borra con todo lo que contenga. Las pruebas escriben ahí su `servicios.txt` con `Files.writeString` (la Lección 5 leía archivos con `Files`; esta es la otra mitad, escribirlos) y no se estorban entre sí ni ensucian el disco.

**`assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta))`** ejecuta lo que le das (una expresión escrita como `() -> ...`: una *lambda*, la misma forma que usaste en los *streams* de la Lección 4) y comprueba que lance **esa** excepción. Si no lanza nada, o lanza otra, la prueba falla; si lanza la esperada, `assertThrows` te la devuelve para que la examines: la prueba compara su `getMessage()` con el mensaje exacto de la tabla. Fíjate por qué se afirma el mensaje y no solo el tipo: ese texto es lo único que la persona que edita `servicios.txt` ve cuando se equivoca; es el contrato con ella. Si alguien lo reescribe sin darse cuenta, la prueba lo detecta.

La tabla de casos de esta clase usa una variante de `@CsvSource` que acepta un bloque de texto (`textBlock`), con el delimitador `|` en lugar de la coma para que cada fila se vea como una tabla. Hay una sola complicación: cada fila ocupa un renglón, pero un archivo de configuración tiene varios. Para eso la primera columna escribe los renglones del archivo separados por `;` (`a https://a.x;b ftp://b.x` son dos renglones), y la prueba los convierte en saltos de línea reales antes de escribir el archivo. Las cuatro filas son cuatro de los motivos de rechazo que definió la Lección 5 (los que más se cometen): faltan campos, la dirección no es `http` ni `https`, el tiempo límite no es mayor que cero, y hay nombres repetidos. La quinta prueba de la clase, `leeUnArchivoValido`, es el caso feliz: dos servicios, uno con el tiempo límite por omisión y otro con 1000. **Siempre hace falta uno:** una clase que solo prueba rechazos podría pasar con un lector que rechaza todo.

### 7.5 Leer el informe: el conteo de pruebas

#### 7.5.1 Dónde está el número, y dónde no

Corre todo el proyecto, completo, con la orden de siempre:

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

Lo que importa de este informe es una sola línea: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. Los cuatro números dicen cuántas pruebas se **ejecutaron**, cuántas **fallaron** una afirmación, cuántas terminaron con un **error** inesperado (una excepción que la prueba no esperaba) y cuántas se **saltaron** (están marcadas para no correr). Y los 18 salen, exactamente, de lo que ya sabes: `ClasificadorTest` 7 (seis filas y la de `SinRespuesta`), `FormatoTest` 4 (cuatro filas), `ConfiguracionTest` 5 (cuatro filas y el caso feliz) y `ResumenTest` 2. Súmalos: 7 + 4 + 5 + 2. Si alguna vez el 18 cambia sin que tú agregaras o quitaras una prueba, algo se movió, y es una pregunta que vale la pena contestar.

Cada clase de pruebas deja además su propio informe en la carpeta `target/surefire-reports`, en dos formatos:

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

Los `.txt` son para leerlos tú, y los `TEST-*.xml` son para las máquinas: el servidor de integración continua, el editor, o un guion tuyo leen el XML para contar. Un informe de texto de una clase en verde es de tres renglones, lo que viste arriba pero de una sola clase:

```text
$ cat target/surefire-reports/mx.habil.revisor.ConfiguracionTest.txt
-------------------------------------------------------------------------------
Test set: mx.habil.revisor.ConfiguracionTest
-------------------------------------------------------------------------------
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
```

#### 7.5.2 Tres verdes que no probaron nada

Y ahora, la razón por la que esta sección existe. **El número se lee del informe, jamás del mensaje de éxito ni del código de salida.** Hay una forma de que Maven termine en `BUILD SUCCESS` sin haber corrido ni una sola prueba, y es más fácil de provocar de lo que parece: basta con que los nombres de las clases de prueba no sean los que *Surefire* busca. Para verlo se renombraron las cuatro clases de este proyecto de `...Test` a `...Prueba` (por ejemplo `ClasificadorPrueba`), lo que en español suena a lo mismo, y se corrió el mismo comando:

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

No hay ningún renglón `T E S T S`, ni `Running`, ni `Tests run`. Y sin embargo: `BUILD SUCCESS`, y el código de salida es `0`, el que significa «todo bien». Maven no encontró pruebas, no tuvo nada que fallar y se declaró exitoso. Si el único criterio hubiera sido «¿terminó en verde?», este proyecto sin una sola prueba efectiva habría pasado cualquier revisión. Tampoco se crea la carpeta `target/surefire-reports`, que es otra pista: el informe no existe porque no hubo qué reportar. Compáralo con lo que viste en la Lección 6, cuando la carpeta de pruebas estaba vacía: ahí *Surefire* decía `No tests to run.`, un aviso honesto. Aquí hay cuatro clases de prueba compiladas en `target/test-classes`, y como ninguna se llama como él busca, no dice nada. **El silencio es peor que el aviso**, y por eso el número se busca activamente: no aparecer también es un dato.

Hay otras dos variantes del mismo mecanismo, también medidas. La primera: una clase de pruebas que **se salta sola**. JUnit permite que una prueba diga «si no se cumple tal condición, no me ejecutes» (se llama *suposición*, `Assumptions`, y sirve de verdad para pruebas que necesitan algo que no siempre está, como una conexión). Pero un desliz en esa condición apaga todas las pruebas de la clase sin que nada se ponga rojo. Se agregó una suposición falsa al inicio de `FormatoTest` para verlo. Es este fragmento, puesto dentro de la clase, antes de la prueba que ya tenía (con sus dos `import`: `org.junit.jupiter.api.Assumptions` y `org.junit.jupiter.api.BeforeAll`):

```java
  @BeforeAll
  static void soloSiHayRed() {
    Assumptions.assumeTrue(false, "sin red");
  }
```

`@BeforeAll` marca un método que JUnit corre una vez, antes de todas las pruebas de la clase (por eso es `static`: todavía no hay ningún objeto de la clase). `assumeTrue(condición, motivo)` dice «si la condición es falsa, no sigas». Aquí la condición es `false` a la vista, para que el efecto sea obvio; en un proyecto real sería algo como «hay conexión a la base de datos», y el desliz, una condición que nunca se cumple en la máquina de integración. El resultado:

```text
$ mvn -B verify
...
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.002 s -- in mx.habil.revisor.FormatoTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
$ echo $?
0
```

Maven imprimió un `WARNING` (amarillo, no rojo) y siguió: la clase entera quedó en `Skipped: 1`, el total bajó de 18 a 15, `BUILD SUCCESS` y código de salida `0`. Quien solo mire el verde nunca se entera de que cuatro pruebas dejaron de vigilar el formato del tiempo. La segunda variante es de la terminal y no de Maven: **el código de salida de una tubería es el del último programa de la tubería.** Si por comodidad recortas la salida con `| tail` y revisas `$?`, lo que revisas es el código de `tail`, que siempre sale bien:

```text
$ mvn -B verify | tail -2
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
0
```

Aquí Maven falló (esta es la corrida con el `>` roto de 7.3.2, y su propio código de salida fue 1), pero el `0` que ves es el de `tail`. Si de verdad necesitas una tubería, la terminal tiene una opción para que el código de salida sea el del primer programa que falló y no el del último:

```text
$ set -o pipefail
$ mvn -B verify | tail -1
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
1
```

Con `pipefail`, el `1` de Maven sobrevive a la tubería. Vale para la terminal donde lo escribiste (o para el guion que lo tenga al principio), y no cambia lo importante: lo más seguro es correr las pruebas **sin filtrar la salida**, y leer de ahí el renglón `Tests run:`. En los tres casos la defensa es la misma, y es la costumbre de este curso: después de correr las pruebas, lee ese renglón y compara el número con el que esperas.

Hay un cuarto verde, pero ese sí avisa: `mvn -B verify -DskipTests` compila las pruebas, no las corre, dice `Tests are skipped.` y termina en `BUILD SUCCESS`. Es una opción legítima, que se usa a propósito cuando solo quieres el `.jar` y ya corriste las pruebas antes; conviene recordar que ese verde no significa que algo se haya probado, y que nunca es el comando con que se decide si un cambio está bien.

#### 7.5.3 El número se anota

En un proyecto que ya existe la forma de saber cuántas esperar es tenerlo anotado: en la bitácora del curso, junto a la fecha. La de hoy dice 18. Cuando agregues una prueba, será 19, y lo anotarás. Es una manera barata de que un cambio inesperado en el conteo —hacia arriba o hacia abajo— te llame la atención.

### 7.6 El paso del proyecto: el `revisor` con pruebas

Tienes todas las piezas. Este es el proyecto completo, tal como queda al terminar la lección. Respecto a la Lección 6 cambian dos cosas: el `pom.xml` (versión 0.7.0 y JUnit en alcance de pruebas, en 7.1.2) y la carpeta `src/test/java`, que deja de estar vacía. **El código del programa no cambió ni un renglón**: las pruebas se escribieron para el programa que ya había. Se incluye todo para que el proyecto se pueda armar sin mirar otra lección:

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

Ya viste completos el `pom.xml` y las cuatro pruebas. Tu `servicios.txt` es el de siempre:

```text fig07_01/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

Y estos son los trece archivos del programa, idénticos a los de la Lección 6. Si la seguiste, ya los tienes; se incluyen para que el proyecto de esta lección se pueda construir sin depender de nada más.

<details>
<summary>Los trece archivos del programa, sin cambios respecto a la Lección 6</summary>

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

Para terminar la lección: `mvn -B verify` debe decir `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`, y dejar `target/revisor-0.7.0.jar`; `java -cp target/classes mx.habil.revisor.Revisor` debe imprimir la tabla con resumen de la Lección 4 (4.5) de 843 ms, igual que en la Lección 6, porque el programa no cambió. Hay un artículo de Hábil sobre [qué pruebas atrapan errores de verdad y cómo medir la cobertura](https://www.habil.mx/es/blog/pruebas-que-atrapan-errores-cobertura-quality-gate/), que complementa esta lección desde el lado de la calidad; y otro sobre [cómo se arma la integración continua de una casa de software](https://www.habil.mx/es/blog/cicd-devsecops-infraestructura-propia/), el lugar donde estas 18 pruebas se ejecutan solas en cada cambio.


## El error que vas a ver

La lección tiene un error principal, el de la prueba que se pone roja, y varios mensajes de apoyo que conviene saber leer de una mirada.

**El error principal.** Es el de 7.3.2. Cambias `>=` por `>` en la regla del umbral, corres `mvn -B verify`, y el informe dice que una de las filas de la tabla de casos, la tercera, esperaba `LENTO` y obtuvo `ARRIBA`. Se lee en cuatro pasos, siempre los mismos: **qué clase** falló (la línea con `<<< FAILURE!`), **qué prueba** (el nombre del método y, entre corchetes, el número de la fila), **qué esperaba y qué obtuvo** (`expected: <...> but was: <...>`), y **cuántas** en total (`Tests run: 18, Failures: 1`). Con eso sabes qué cambiar sin abrir un depurador, y tienes la certeza de que la prueba vigila lo que dice vigilar. Verla fallar, a propósito, es el primer uso que le das.

**Los mensajes de apoyo**, para reconocerlos de una mirada:

- `org.opentest4j.AssertionFailedError: expected: <...> but was: <...>`: una afirmación no se cumplió. El informe la cuenta en `Failures`. Si los valores parecen al revés, revisa el orden de `assertEquals`: el esperado va primero.
- Un renglón con `Errors: 1` en lugar de `Failures: 1`: la prueba no llegó a afirmar nada porque saltó una excepción que no esperaba. Es otro tipo de problema: no «el resultado es otro», sino «el código se cayó».
- `Expected mx.habil.revisor.ErrorDeConfiguracion to be thrown, but nothing was thrown.`: es la forma en que JUnit dice que un `assertThrows` no vio la excepción que pedía. La lectura del archivo aceptó algo que debía rechazar. (Medido cambiando en `Configuracion.java` el `timeoutMs > 0` por `timeoutMs >= 0`: un `timeout_ms` de 0 deja de rechazarse y cae la fila `[3]` de `rechazaUnArchivoInvalido`.)
- `[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`: una clase entera se saltó. Amarillo, no rojo, y el total baja: busca una suposición (7.5.2).
- `Tests are skipped.`: alguien corrió con `-DskipTests`. Ese verde no probó nada.
- Ningún renglón `Tests run:` y `BUILD SUCCESS`: *Surefire* no encontró clases que se llamen como él busca (7.5.2).

## Lo que se hace mal

**Fiarse del `BUILD SUCCESS`.** Ya viste el costo en 7.5: un proyecto sin una sola prueba efectiva termina en verde y con código de salida cero. Lo que cuenta es el renglón `Tests run:` y que el número sea el esperado.

**Confiar en una prueba que nunca viste en rojo.** Una prueba que siempre pasa no se distingue de una que no vigila nada. Antes de darla por buena, rompe lo que vigila, comprueba que falla con el mensaje que esperas y deshaz el cambio.

**Una prueba sin afirmación.** Un método con `@Test` que llama al código pero nunca llama a `assertEquals` ni a `assertThrows` pasa siempre, salvo que el código lance una excepción: ejecuta el código, no comprueba nada. El verde que da no vale más que el de un proyecto sin pruebas.

**Probar solo el caso feliz, o solo los rechazos.** Una clase que solo prueba que un archivo bueno se lee bien no sabe si los malos se rechazan; una que solo prueba rechazos pasaría con un lector que rechaza todo. Hacen falta los dos lados, como en `ConfiguracionTest`.

**Elegir los casos al azar.** Los errores se esconden en la frontera: el 999 y el 1000, el 399 y el 400. Una tabla de casos que no tiene los dos lados de cada frontera deja el borde sin vigilar, y el ejercicio 1 mide qué tan caro sale.

**Pruebas que dependen de algo que no controlas:** la red, la hora del día, el orden en que corren, la carpeta desde donde se ejecutan o un archivo que «ya estaba ahí». Una prueba tiene que dar el mismo resultado mañana y en la computadora de otra persona; para archivos, `@TempDir`; para datos, que el propio caso los arme.

**Comentar o borrar una prueba que falla «para que pase».** La prueba estaba diciendo algo. Entiende el fallo: o el código está mal, o la prueba describe una regla que cambió, y entonces se actualiza la prueba (como en el ejercicio 2), no se silencia.

**Recortar la salida de las pruebas con una tubería.** `mvn -B verify | tail` cambia el código de salida por el de `tail`, y el fallo desaparece (7.5.2). Las pruebas se corren sin filtrar, o con `set -o pipefail`.

## Ejercicios

**Cada ejercicio parte del proyecto de 7.6, con sus 18 pruebas.** Si un ejercicio te deja pruebas de más o un cambio en el código, deshazlo antes de empezar el siguiente; así los conteos que predices y los de las soluciones son los mismos.

### Ejercicio 1 — La frontera que falta

La tabla de casos de `ClasificadorTest` tiene dos errores HTTP, 503 y 404, pero ninguno cerca de la frontera entre «respondió bien» y «respondió con error». La regla dice que un código **mayor o igual a 400** deja al servicio caído. **Antes de tocar nada, predice:** si alguien cambia en `Clasificador.java` la comparación `codigo >= 400` por `codigo > 400`, ¿cuántas de las 18 pruebas se ponen rojas? Ejecútalo y comprueba tu predicción. Luego agrega a la tabla los casos `400, 50, CAIDO` y `399, 50, ARRIBA`, repite el cambio y anota qué fila se pone roja y cuántas pruebas dice ahora el informe.

### Ejercicio 2 — La pregunta del lunes

Responde la pregunta con la que empezó la lección. Cambia `UMBRAL_LENTO_MS` a 800 en `Clasificador.java`. **Predice** qué fila de la tabla se pone roja y por qué, y ejecuta `mvn -B verify` para comprobarlo. Después actualiza la **prueba**, no el código: cambia las dos filas de la frontera para que vigilen el nuevo valor (un valor justo debajo y el valor del umbral), y confirma que el informe vuelve a decir `Tests run: 18, Failures: 0`. Anota en la bitácora por qué la prueba tuvo que cambiar.

### Ejercicio 3 — Una prueba nueva, a mano

`Formato.contar(long cantidad, String palabra)` agrega una «s» a la palabra salvo cuando la cantidad es 1 (`1 caído`, `3 caídos`). Escribe en `FormatoTest` una prueba parametrizada con tres filas: `0, servicio` (debe dar `0 servicios`), `1, caído` y `3, caído`. **Antes de ejecutar**, anota cuántas pruebas esperas que diga el informe (pista: ahora hay tres filas más). Después hazla fallar a propósito cambiando `cantidad == 1` por `cantidad <= 1` en `Formato.java`, y observa cuántas pruebas se ponen rojas y de qué clases. ¿Esperabas que fueran de más de una clase?

### Ejercicio 4 — El verde que no probó nada

Renombra la clase `ResumenTest` a `ResumenPrueba`, en el archivo y en la declaración de la clase. Ejecuta `mvn -B verify` y contesta tres preguntas: ¿terminó en `BUILD SUCCESS`? ¿Cuántas pruebas dice el informe ahora? ¿Cuál es el código de salida, con `echo $?` justo después? Restaura el nombre, confirma que el conteo vuelve a 18 (si alguna vez un conteo no cuadra después de renombrar o borrar una clase, `mvn -B clean verify` borra `target/` y construye desde cero) y escribe en la bitácora la frase con que te vas a acordar de leer el renglón `Tests run:`.

## Soluciones

### Solución 1

**Predicción.** Ninguna se pone roja: las 18 pasan. Los casos de error de la tabla son 503 y 404, y ambos son mayores que 400 con cualquiera de las dos comparaciones. El cambio de `>=` a `>` solo importa para el valor 400 exacto, y la tabla no lo contiene. Es un caso real de un cambio que las pruebas **no detectan**: el defecto sobrevive, y el verde dice que todo está bien.

Los dos casos nuevos se agregan al final de la tabla:

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

Con los dos casos y el cambio de `>=` a `>` en `Clasificador.java`, el informe dice:

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

La fila `[7]` es la de `400, 50, CAIDO`: esperaba `CAIDO` y obtuvo `ARRIBA`, porque con `>` el 400 ya no es error. Y el total subió a 20, de las 18 originales más las dos filas nuevas. La fila `399, 50, ARRIBA` pasa en las dos versiones; está ahí para que el otro lado de la frontera quede también vigilado. Revierte el cambio de `Clasificador.java`: con los dos casos todavía en la tabla, el informe debe decir `Tests run: 20, Failures: 0`. En un proyecto real esas dos filas se quedan, porque vigilan una frontera que antes nadie vigilaba; para seguir con los ejercicios, quítalas y vuelve a las 18.

### Solución 2

Con el umbral en 800, la regla vigila un valor que la tabla no esperaba. Falla la fila `[2]`, `200, 999, ARRIBA`: un servicio de 999 milisegundos ya no está arriba, está lento.

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

El `expected: <ARRIBA> but was: <LENTO>` lo dice. Las demás filas siguen pasando (el 142 sigue arriba, y el 1000 y el 2300 siguen lentos), y no se rompe ninguna otra clase de pruebas, porque ninguna otra depende del umbral. Lo que falla es la prueba, y la razón es que la **regla de negocio cambió**: la prueba describía el umbral viejo. Por eso se actualiza ella y no el código. Las dos filas de la frontera pasan a ser `200, 799, ARRIBA` (justo debajo) y `200, 800, LENTO` (el umbral):

```java
    "200, 142,  ARRIBA",
    "200, 799,  ARRIBA",
    "200, 800,  LENTO",
    "200, 2300, LENTO",
```

y el informe vuelve a verde, con el mismo total que al principio, porque se cambió el contenido de dos filas y no se agregó ninguna:

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Y la pregunta del lunes ya tiene respuesta escrita, ejecutable y vigilada: un servicio de exactamente 800 milisegundos es lento.

### Solución 3

La prueba nueva, dentro de la clase `FormatoTest`, junto a la que ya tiene:

```java
  @ParameterizedTest
  @CsvSource({"0, servicio, 0 servicios", "1, caído, 1 caído", "3, caído, 3 caídos"})
  void pluralizaSalvoCuandoEsUno(long cantidad, String palabra, String esperado) {
    assertEquals(esperado, Formato.contar(cantidad, palabra));
  }
```

Son tres filas, así que el conteo pasa de 18 a **21**:

```text
$ mvn -B verify
...
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Con `cantidad <= 1` en lugar de `cantidad == 1`, el cero deja de llevar «s» (`0 servicio`) y ese caso falla:

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

Fallan **dos** pruebas, de **dos clases**: la nueva (`FormatoTest`, fila `[1]`, la de `0 servicios`) y una que ya existía, `ResumenTest`, cuyo caso de «nadie respondió» espera `0 lentos`. Es normal que la sorpresa sea esa: las pruebas se escriben para una cosa, pero vigilan todo lo que pasa por ahí. Un cambio en una función compartida es más fácil de detectar si varias partes del programa dependen de ella. Y es una buena razón para no borrar una prueba «redundante».

### Solución 4

Sin la clase `ResumenTest`, *Surefire* ya no la encuentra, y el informe dice:

```text
$ mvn -B verify
...
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
$ echo $?
0
```

`BUILD SUCCESS`, código de salida `0`, y **16** pruebas en lugar de 18: faltan las dos de `ResumenTest`. Si no sabías que el número correcto era 18, nada te habría avisado. Esa es la frase para la bitácora: **«Tests run: 18, siempre; si el número cambia y yo no lo cambié, me detengo».** Al restaurar el nombre, el conteo vuelve a 18. Un detalle medido: con Maven 3.10.0, `mvn -B verify` a secas ya da 16, porque el compilador detecta que cambió una fuente, recompila el módulo entero y no deja en `target/test-classes` el `ResumenTest.class` viejo; `mvn -B clean verify` da también 16 y no depende de ese comportamiento, así que es la forma segura cuando renombras o borras clases.

## Cómo sé que lo logré

- [ ] `mvn -B verify`, desde la raíz de `~/curso-java/revisor/`, termina con `BUILD SUCCESS` y dice `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`, y sabes de dónde sale cada uno de los 18 (7 + 4 + 5 + 2).
- [ ] El `.jar` se llama ahora `revisor-0.7.0.jar`, y `java -cp target/classes mx.habil.revisor.Revisor` sigue imprimiendo la tabla con `promedio 843ms`.
- [ ] Cambiaste `>=` por `>` en `Clasificador.java`, viste la fila `[3]` en rojo con `expected: <LENTO> but was: <ARRIBA>`, y deshiciste el cambio.
- [ ] Corriste una sola prueba con `-Dtest=Clase#metodo` y después todas, y sabes por qué al final se corren todas.
- [ ] Renombraste `ResumenTest` a `ResumenPrueba`, viste `BUILD SUCCESS` con 16 pruebas, y escribiste en la bitácora cómo te vas a acordar de leer el conteo.
- [ ] Puedes explicar para qué sirve `<scope>test</scope>` y por qué JUnit no va en el programa.
- [ ] Anotaste en tu bitácora el número que dice el informe, con la fecha.

## Resumen

Responde sin mirar la lección:

1. ¿Cuáles son los tres tiempos de una prueba? ¿Qué argumento de `assertEquals` va primero, y qué pasa si los inviertes?
2. ¿Para qué sirve el BOM de JUnit y qué hace `<scope>test</scope>`?
3. ¿Por qué esta lección prueba `Clasificador`, `Formato`, `Resumen` y `Configuracion`, y no `Revision` ni `Simulador`?
4. ¿Por qué se rompe a propósito el código antes de dar por buena una prueba?
5. ¿Qué significa el `[3]` en el nombre de una prueba parametrizada que falla? ¿Por qué los casos de la tabla se eligen alrededor de la frontera?
6. ¿Qué comprueba `assertThrows` y qué pasa si el código no lanza nada? ¿Qué hace `@TempDir`?
7. ¿Qué diferencia hay entre `Failures` y `Errors` en el informe?
8. Tres formas en que Maven termina en verde sin haber probado lo que crees: ¿cuáles son y cómo te proteges de cada una?

## Para leer más

- [JUnit 6, guía de usuario](https://docs.junit.org/current/user-guide/): las anotaciones, las [afirmaciones](https://docs.junit.org/current/writing-tests/assertions.html), las [suposiciones](https://docs.junit.org/current/writing-tests/assumptions.html) y las [pruebas parametrizadas](https://docs.junit.org/current/writing-tests/parameterized-classes-and-tests.html) con todas sus fuentes de datos, más allá de `@CsvSource`. Consultado el 13 de octubre de 2026.
- [Maven Surefire, «Inclusions and Exclusions of Tests»](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html): qué nombres de clase corre y cómo cambiarlo, útil cuando un conteo no cuadra. Consultado el 7 de octubre de 2026.
- [Maven, «Introduction to the Dependency Mechanism»](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html): los alcances (`test` entre ellos), el BOM y cómo se importa con `<scope>import</scope>`. Consultado el 13 de octubre de 2026.
- [Manual de Bash, la orden `set`](https://www.gnu.org/software/bash/manual/html_node/The-Set-Builtin.html): qué hace `pipefail` con el código de salida de una tubería. Consultado el 13 de octubre de 2026.

---

**Anterior:** [Lección 6 — Proyecto con Maven](06-proyecto-maven.md) ·
**Siguiente:** [Lección 8 — HTTP y JSON](08-http-y-json.md)
