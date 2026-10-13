# Lección 5 — Errores y excepciones

**Tiempo:** 90 min (justo: la lección tiene cuatro ideas y un proyecto, y conviene no apurarla)

**Qué construyes:** la lectura de la configuración del `revisor`: la lista de servicios deja de estar escrita en el código y se lee de un archivo `servicios.txt`, con un mensaje claro por cada forma en que el archivo puede estar mal

**Qué aprendes:** excepciones verificadas y no verificadas; `try`, `catch` y `finally`; `try-with-resources`; excepciones propias con su causa; cuándo un resultado es mejor que una excepción; y qué hacer con una `InterruptedException`

**De dónde vienes.** Traes el proyecto de la [Lección 4](04-colecciones-genericos-streams.md): un `revisor` con su `Simulador`, un `Resumen` que cuenta con *streams*, un `Reporte` con copia defensiva, y una tabla con su línea de resumen. Funciona, pero tiene dos fallas de fondo. La primera es la que te dejó el ejercicio de proyecto de la lección 4: para agregar un servicio hay que **abrir el código Java, editarlo y recompilar**; los datos y el programa cambian por razones distintas y no deberían vivir juntos. La segunda es más sutil: la revisión es instantánea, porque el simulador devuelve el tiempo sin esperarlo, y por eso no tienes una forma de ver cómo se comporta el programa cuando una revisión **tarda**. Esta lección arregla las dos: los datos se van a un archivo que el programa lee y del que desconfía, y la simulación aprende a esperar lo que dice su tiempo. Trabajas en tu carpeta `~/curso-java/revisor/` y ejecutas todo con `java`.

**Qué no hace esta lección.** No consulta ninguna red: sigue usando los datos simulados, y eso no cambia hasta la [Lección 8](08-http-y-json.md). No usa Maven ni pruebas: Maven llega en la [Lección 6](06-proyecto-maven.md) y las pruebas en la [Lección 7](07-pruebas-junit.md). Y el `revisor` todavía no tiene opciones de línea de comandos ni código de salida: hoy el nombre del archivo está escrito en el programa, y cuando algo falla el programa imprime el motivo y termina con código 0. Eso es un defecto que la [Lección 10](10-el-programa-terminado.md) corrige, y esta lección lo señala cuando lo veas.

**Sobre las corridas.** Para ejecutar una figura, entra a la carpeta de la lección en el repositorio (`programas/05-errores-excepciones/`) y corre `java fig05_01.java`, con el número de la figura que quieras. Las que viven en una carpeta propia (la de la sección 5.3 y la del proyecto de la 5.6) se ejecutan desde dentro de ella. Todas las salidas se obtuvieron con Java 25 (Temurin 25.0.4) y la terminal en UTF-8. Las trazas de error traen renglones del propio JDK (`at java.base/...`) con números de línea que cambian de una actualización a otra: es normal que los tuyos difieran en esos números.

## Al terminar vas a poder

- Decir de un tipo de excepción si es verificada o no verificada, y qué obliga el compilador en cada caso.
- Escribir un `try` con `catch` y `finally`, atrapando solo la excepción que sabes manejar y en el orden que el compilador acepta.
- Abrir un archivo de texto con `try-with-resources` y explicar qué garantiza esa forma frente a cerrarlo a mano.
- Definir una excepción propia que conserve la causa original, y leer una traza con su `Caused by`.
- Decidir si un fallo de tu programa se representa mejor con una excepción o con un valor, y defender la decisión.
- Manejar una `InterruptedException` sin tragarla: propagarla, o restaurar la bandera de interrupción.

## El porqué antes del cómo

Hasta la lección 4, cada dato del `revisor` lo escribiste tú, en el mismo archivo que el código. Si estaba mal, lo sabías al instante, porque el compilador no te dejaba pasar. Un archivo de configuración cambia esa situación por completo: lo escribe otra persona, en otro momento, con otro editor, y el programa se entera de sus errores **en la ejecución**. Un archivo de fuera puede fallar de cuatro maneras, y las cuatro son normales, no excepcionales:

1. **No existe**, o existe en otra carpeta distinta de la que tú crees.
2. **Existe pero no se puede leer**, porque está en otra codificación o alguien lo dejó sin permisos.
3. **Está completo en la forma pero dice algo absurdo**: una dirección que no empieza con `http`, un tiempo límite negativo, dos servicios con el mismo nombre.
4. **Está vacío de contenido útil**: solo comentarios.

Un programa serio distingue estos casos y le dice a la persona **cuál le pasó y cómo arreglarlo**, en una línea, no con un informe de treinta renglones escrito para programadores. Eso es lo que hace esta lección.

Pero el `revisor` tiene, además, otra clase de fallo que no se parece a ninguna de las anteriores: un **servicio** que no contesta. Revisar un servicio caído no es un error del programa; es justo para lo que existe. Si el programa tratara un servicio caído como una emergencia, un solo caído tumbaría el reporte de los otros noventa y nueve. Ya tomaste esa decisión en la lección 3, sin llamarla así: el `SinRespuesta` es un **valor**, no una excepción. Esta lección la explica y la defiende, porque entender cuándo algo es una excepción y cuándo es un valor es lo más importante que Java tiene que enseñarte sobre errores.

Un comentario para quien llega de otros lenguajes. En Go, el error es un valor que las funciones devuelven y se revisa con `if err != nil`. En Rust, es un `Result`. En Python y TypeScript, es una excepción que sube hasta que alguien la atrapa. Java hace las dos cosas: tiene excepciones, y tiene valores como los `Optional` y los *sealed* de la lección 3. Y añade algo que los otros no tienen: el compilador distingue **dos clases de excepciones**, y a una de ellas la obliga a ser tratada. Esa es la parte que se siente rara al principio y que tiene su sentido; la primera sección va por ahí.

El camino es de una sesión. La sección 5.1 cubre las excepciones verificadas y no verificadas y la sintaxis de `try`; la 5.2, `try-with-resources` y la lectura de archivos; la 5.3, las excepciones propias; la 5.4, cuándo preferir un valor; la 5.5, `InterruptedException`; y la 5.6 junta todo en el proyecto. Este es el destino, para que lo tengas a la vista. Corrido desde otra carpeta que la del archivo de configuración, el `revisor` dirá:

```text
error de configuración: no existe el archivo servicios.txt
```

Un mensaje de una línea, en lugar de una traza. Para llegar ahí hace falta entender, primero, qué es una excepción.

## Los conceptos

### 5.1 Excepciones verificadas y no verificadas

#### 5.1.1 Qué es una excepción y de qué familias hay

Una **excepción** es un objeto que describe que algo salió mal. Cuando una línea del programa no puede hacer lo que se le pidió, **lanza** una excepción (con la palabra `throw`, o porque lo hace por dentro un método que llamaste). La excepción sube por los métodos que estaban en marcha, del más reciente al más antiguo, buscando alguien que la **atrape**. Si nadie la atrapa, el hilo termina y la JVM imprime la traza que ya conoces. La lección 4 te enseñó a leerla de arriba hacia abajo; ahora vamos a ver qué hay detrás.

Todas las excepciones heredan de una clase: `Throwable`. De ahí salen dos ramas. `Error` es para los problemas graves de la JVM misma (se acabó la memoria, un archivo de clases corrupto), y la regla es que **no se atrapan**: no tienes forma de recuperarte. `Exception` es para lo que un programa razonable puede querer manejar. Y dentro de `Exception` hay una subrama especial, `RuntimeException` (excepción en tiempo de ejecución), con la que Java divide el mundo en dos:

- **Excepciones no verificadas** (*unchecked*): `RuntimeException`, `Error` y todo lo que hereda de ellas. El compilador **no te obliga** a hacer nada con ellas. Son los errores de programación: un `NullPointerException`, un `IllegalArgumentException`, el `UnsupportedOperationException` de la lección 4.
- **Excepciones verificadas** (*checked*): todas las demás, o sea, `Exception` y sus hijas que no son `RuntimeException`. El compilador **te obliga a tratarlas**, y si no lo haces, el programa no compila. Es la definición de la [especificación del lenguaje](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html): las verificadas son todas las que no son no verificadas. (En el resto del curso uso «verificada» y «no verificada»; «checked» y «unchecked» son los nombres en inglés y los vas a ver en toda la documentación.)

El programa `fig05_01` imprime la cadena de herencia de cinco excepciones y dice a qué clase pertenece cada una. Para escribirlo hace falta un `for` con una condición en lugar de un recorrido (`for (inicio; condición; paso)`: empieza en `inicio`, repite mientras la `condición` sea cierta, y ejecuta el `paso` al final de cada vuelta), una `Class<?>` (el tipo que representa a una clase; el `?` es el comodín de la lección 4) y los métodos `getSuperclass()`, que da el padre, e `isAssignableFrom`, que contesta «¿este tipo hereda de aquel?»:

```java
// fig05_01.java
void cadena(Class<?> tipo) {
  String texto = tipo.getSimpleName();
  for (Class<?> padre = tipo.getSuperclass(); padre != null; padre = padre.getSuperclass()) {
    texto = texto + " -> " + padre.getSimpleName();
  }
  boolean esExcepcion = Exception.class.isAssignableFrom(tipo);
  boolean esNoVerificada = RuntimeException.class.isAssignableFrom(tipo);
  String clase;
  if (Error.class.isAssignableFrom(tipo)) {
    clase = "error de la JVM";
  } else if (esExcepcion && !esNoVerificada) {
    clase = "verificada";
  } else {
    clase = "no verificada";
  }
  IO.println(texto);
  IO.println("    " + clase);
}

void main() {
  cadena(NoSuchFileException.class);
  cadena(InterruptedException.class);
  cadena(NumberFormatException.class);
  cadena(NullPointerException.class);
  cadena(OutOfMemoryError.class);
}
```

```text
$ java fig05_01.java
NoSuchFileException -> FileSystemException -> IOException -> Exception -> Throwable -> Object
    verificada
InterruptedException -> Exception -> Throwable -> Object
    verificada
NumberFormatException -> IllegalArgumentException -> RuntimeException -> Exception -> Throwable -> Object
    no verificada
NullPointerException -> RuntimeException -> Exception -> Throwable -> Object
    no verificada
OutOfMemoryError -> VirtualMachineError -> Error -> Throwable -> Object
    error de la JVM
```

La cadena se lee de izquierda a derecha como «es un... que es un...». `NoSuchFileException`, la que lanza Java cuando un archivo no existe, es una `FileSystemException`, que es una `IOException`, que es una `Exception`: verificada. `InterruptedException` es una hija directa de `Exception`: verificada. `NumberFormatException` pasa por `IllegalArgumentException` y por `RuntimeException`: no verificada. `NullPointerException` también. Y `OutOfMemoryError` está en la rama de `Error`, que es de otra familia: nunca la atraparás. Observa también que **la verificada de la primera línea es una `IOException`**: toda la familia de problemas de entrada y salida (archivos, red) es verificada. Es la clase de error que el programa no puede evitar escribiendo mejor código, porque depende de un mundo exterior que puede fallar.

#### 5.1.2 Lo que obliga el compilador: atrápala o declárala

Veamos qué significa «el compilador te obliga». Este programa lee un archivo de texto y cuenta sus renglones. Parece inofensivo:

```java
// fig05_02.java
void main() {
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
  IO.println(renglones.size());
}
```

```text
$ java fig05_02.java
fig05_02.java:3: error: unreported exception IOException; must be caught or declared to be thrown
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
                                             ^
1 error
error: compilation failed
```

`unreported exception IOException; must be caught or declared to be thrown`: «excepción `IOException` no reportada; debe atraparse o declararse». La flecha `^` apunta a la llamada a `readAllLines`, la que puede lanzarla. Para que el programa compile tienes **dos opciones**, y una de las dos es obligatoria:

1. **Atraparla** donde la puedas manejar, con `try` y `catch` (la sección siguiente).
2. **Declararla**: avisar en la firma del método que puede lanzarla, con `throws`. Por ejemplo `void main() throws IOException`. Declararla no maneja el error; lo **traspasa** a quien llame al método, que a su vez tendrá que atraparla o declararla. La excepción sube hasta que alguien la atrapa; si llega a `main` y `main` también la declara, la JVM imprime la traza y el programa muere.

La idea detrás de la regla es que ciertos fallos no son errores del programador sino de la situación (el archivo pudo no existir), y que sería injusto que el código los ignorara por descuido. El compilador convierte el «se me olvidó que esto puede fallar» en un error de compilación. Si te parece una carga, reserva el juicio hasta la sección 5.4: es una herramienta con un uso preciso.

#### 5.1.3 `try`, `catch` y `finally`

Atrapar una excepción es decir: «intenta esto; si falla de *esta* manera, haz *esto otro* en lugar de morir». Se escribe con `try` (intenta), `catch` (atrapa) y, opcionalmente, `finally` (finalmente). El programa `fig05_03` convierte un texto en un número, que es lo que la configuración necesita para leer un `timeout_ms`:

```java
// fig05_03.java
int leerTimeout(String texto) {
  try {
    int valor = Integer.parseInt(texto);
    IO.println("  convertí " + valor);
    return valor;
  } catch (NumberFormatException e) {
    IO.println("  no sirve: " + e.getMessage());
    return -1;
  } finally {
    IO.println("  (fin de la conversión)");
  }
}

void main() {
  IO.println("leyendo 5000");
  IO.println("  resultado: " + leerTimeout("5000"));
  IO.println("leyendo 5 s");
  IO.println("  resultado: " + leerTimeout("5 s"));
}
```

```text
$ java fig05_03.java
leyendo 5000
  convertí 5000
  (fin de la conversión)
  resultado: 5000
leyendo 5 s
  no sirve: For input string: "5 s"
  (fin de la conversión)
  resultado: -1
```

`Integer.parseInt(texto)` convierte un texto en un `int`. Con «5000» funciona; con «5 s» lanza `NumberFormatException` con el mensaje `For input string: "5 s"` (el texto que no pudo convertir). Los tres bloques:

- **`try { ... }`** contiene la línea que puede fallar, y de preferencia **solo esa** y las que dependen de ella. Si no hay error, el programa sigue hasta el final del bloque. Si lo hay, Java abandona el `try` en esa misma línea y salta al `catch` que corresponda.
- **`catch (NumberFormatException e) { ... }`** se ejecuta si el error fue de ese tipo. El `e` es la excepción atrapada: `e.getMessage()` da su mensaje. Cualquier otro tipo de excepción pasaría de largo.
- **`finally { ... }`** se ejecuta al salir del `try`, como sea que se salga: haya habido error o no, y aunque el `try` o el `catch` hayan hecho `return`. La única excepción es que la JVM termine de golpe mientras está dentro del `try` o del `catch`: una llamada a `System.exit` (que la lección 10 explica), una caída del proceso o un apagado forzado. Mira la salida: con «5000», se imprime «convertí 5000», luego el `finally`, y **después** llega el resultado a quien llamó, aunque el `return valor` aparece antes en el código. Se usa para limpiar. La [documentación de Oracle](https://docs.oracle.com/javase/tutorial/essential/exceptions/finally.html) lo resume: «el bloque `finally` siempre se ejecuta cuando el `try` termina», y la misma página anota esa excepción.

Una nota de forma: devolver `-1` para decir «no válido» es un mal hábito, porque es un valor mágico (nada impide que alguien lo use como si fuera un tiempo de verdad). Aquí está solo porque el ejemplo es mínimo; en la sección 5.3 el `catch` hará algo mejor.

Hay tres reglas para atrapar bien, y las tres se rompen con tanta frecuencia que conviene escribirlas:

1. **Atrapa la excepción concreta que sabes manejar.** Un `catch (Exception e)` también atrapa el `NullPointerException` producto de una errata, y el programa seguirá funcionando con datos equivocados. Mejor que se detenga con su traza.
2. **Si no sabes qué hacer con un error, no lo atrapes.** Déjalo subir. Un programa que se detiene con una traza clara es mejor que uno que continúa a ciegas.
3. **Un solo `catch` puede cubrir dos tipos si la respuesta es la misma**, con una barra vertical: `catch (NumberFormatException | NullPointerException e)`. Los tipos no pueden ser uno hijo del otro.

El **orden** de los `catch` importa, y el compilador lo vigila. Si el más general va primero, los demás nunca podrían ejecutarse:

```java
// fig05_04.java
void main() {
  try {
    IO.println(Integer.parseInt("5 s"));
  } catch (Exception e) {
    IO.println("algo falló");
  } catch (NumberFormatException e) {
    IO.println("no es un número");
  }
}
```

```text
$ java fig05_04.java
fig05_04.java:7: error: exception NumberFormatException has already been caught
  } catch (NumberFormatException e) {
    ^
1 error
error: compilation failed
```

`exception NumberFormatException has already been caught`: «ya fue atrapada». El primer `catch (Exception e)` se come todo, incluida la `NumberFormatException`, y el segundo bloque sería código muerto. La regla: **de lo particular a lo general**, igual que cuando el `switch` de la lección 3 pedía ordenar los casos.

#### 5.1.4 Por qué una verificada no cruza la lambda de un `map`

Hay un lugar donde la regla de las verificadas se siente como un estorbo, y conviene conocerlo ahora porque te toparás con él en el `revisor`. Es dentro de una lambda. Este programa intenta esperar un poco dentro de un `map`:

```java
// fig05_05.java
void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<String> revisados =
      nombres.stream()
          .map(
              nombre -> {
                Thread.sleep(10);
                return nombre + " revisado";
              })
          .toList();
  IO.println(revisados);
}
```

```text
$ java fig05_05.java
fig05_05.java:8: error: unreported exception InterruptedException; must be caught or declared to be thrown
                Thread.sleep(10);
                            ^
1 error
error: compilation failed
```

El mismo error de antes, pero ahora dentro de la lambda: `Thread.sleep` lanza `InterruptedException` (verificada) y la lambda del `map` no la declara ni la atrapa. La razón es que `map` espera una `Function`, la interfaz funcional de la lección 4, y el método `apply` de una `Function` **no declara ninguna excepción verificada** en su firma. Una lambda no puede lanzar más de lo que su interfaz declara. (Con otras interfaces sí puede: el método `call` de `Callable` declara `throws Exception`, y en la lección 9 lo vas a aprovechar.) Las salidas son tres, y ninguna es agradable: atrapar la excepción dentro de la lambda (el cuerpo se llena de `try`), envolverla en una no verificada (se pierde la ventaja de la verificada), o **no usar un *stream* ahí y volver a un bucle `for`**, donde el método que lo contiene sí puede declarar el `throws`. En el proyecto de hoy tomaremos esta última: `revisarTodos` pasa de *stream* a `for`. No es un retroceso, es elegir la herramienta que permite lo que el problema pide.

Si prefieres una referencia a método (`.map(Revision::revisar)`) el mensaje cambia pero la causa es la misma: `incompatible thrown types InterruptedException in functional expression`. El ejercicio 4 te pide provocarlo.

#### 5.1.5 ¿Cuándo verificada y cuándo no?

Cuando diseñas tus propias excepciones tienes que decidir de qué familia son, y la guía de Oracle da una regla de una oración: [«si se puede esperar razonablemente que quien llama se recupere de la excepción, hazla verificada; si no puede hacer nada para recuperarse, hazla no verificada»](https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html). Un archivo de configuración que no existe es un buen ejemplo de la primera: quien llama puede hacer algo útil (avisar al usuario cómo arreglarlo). Un `null` donde nunca debió haberlo es el ejemplo de la segunda: no hay nada que "recuperar", hay un defecto que corregir.

Es honesto decir que esta decisión de Java es discutida. Las verificadas obligan a una disciplina útil, pero en proyectos grandes producen una cascada de `throws` en todas las firmas y de `catch` que solo reenvían, y otros lenguajes de la misma familia, como C# y Kotlin, decidieron no tenerlas. Java las conserva, y tu trabajo no es tener una opinión sobre ellas sino saber que el compilador las trata distinto y por qué.

### 5.2 `try-with-resources`: lo que se abre se cierra

#### 5.2.1 Recursos y la forma que los cierra sola

Un **recurso** es algo que se abre y que hay que cerrar: un archivo, una conexión de red. Si no lo cierras, el sistema operativo lo mantiene abierto: con un archivo, eso se traduce a un descriptor que se acaba, y con una conexión, a un servidor que sigue esperando. La forma de siempre de cerrarlo bien era un `try` con un `finally` que cierra el recurso, y tiene un defecto de familia: si el `try` lanza una excepción y el `close()` del `finally` también, la segunda **tapa** a la primera, y pierdes el error que de verdad importaba.

Java 7 añadió una forma que lo resuelve: **`try-with-resources`** (*try con recursos*). Pones los recursos entre paréntesis justo después del `try`, y Java los cierra **solo**, en orden inverso al que los abrió, aunque el bloque termine bien o con una excepción. Un recurso es cualquier objeto cuyo tipo implementa la interfaz `AutoCloseable`, que tiene un único método, `close()`. El programa `fig05_06` lo demuestra con un recurso inventado que avisa cuando se abre y cuando se cierra:

```java
// fig05_06.java
record Recurso(String nombre, boolean falla) implements AutoCloseable {
  Recurso {
    IO.println("abro " + nombre);
  }

  @Override
  public void close() {
    IO.println("cierro " + nombre);
    if (falla) {
      throw new IllegalStateException("no pude cerrar " + nombre);
    }
  }
}

void main() {
  try (Recurso entrada = new Recurso("entrada", false);
      Recurso salida = new Recurso("salida", false)) {
    IO.println("trabajo con " + entrada.nombre() + " y " + salida.nombre());
  }
  IO.println("--");
  try (Recurso fragil = new Recurso("frágil", true)) {
    IO.println("trabajo con " + fragil.nombre());
    throw new IllegalArgumentException("falló el trabajo");
  } catch (IllegalArgumentException e) {
    IO.println("atrapé: " + e.getMessage());
    for (Throwable suprimida : e.getSuppressed()) {
      IO.println("  suprimida: " + suprimida.getMessage());
    }
  }
}
```

```text
$ java fig05_06.java
abro entrada
abro salida
trabajo con entrada y salida
cierro salida
cierro entrada
--
abro frágil
trabajo con frágil
cierro frágil
atrapé: falló el trabajo
  suprimida: no pude cerrar frágil
```

Antes de leer la salida, tres detalles de la declaración que todavía no habías visto. `implements AutoCloseable` es el mismo `implements` de la lección 3: el *record* se compromete a cumplir el contrato de la interfaz, que exige un método `close()`. `@Override` es una **anotación**: le dice al compilador «este método reemplaza el de la interfaz», y si te equivocas de nombre, falla al compilar en lugar de crear un método distinto sin que lo notes. Y `public` es una palabra que todavía no habías necesitado: los métodos de una interfaz son siempre públicos, y por eso quien los implementa debe escribirlo. En la lección 6 verás la palabra completa, con su significado y sus hermanas.

Ahora la salida. En el primer bloque abres dos recursos (`entrada` y `salida`) y se cierran **al revés**: primero `salida`, luego `entrada`. Es el orden correcto: lo último que abriste suele depender de lo anterior. En el segundo bloque el `close()` de `frágil` lanza una excepción, y el cuerpo del `try` también (la lanza `throw new IllegalArgumentException(...)`, que es la forma de lanzar una a mano). El resultado: el `catch` recibe la excepción **del cuerpo**, la que importaba (`falló el trabajo`), y la que lanzó `close()` no se pierde ni la tapa: queda **adjunta** a la primera como excepción *suprimida*, y se recupera con `e.getSuppressed()`. La [guía de Oracle](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) lo explica con otro ejemplo: con un `finally`, la excepción que sale es la del `finally` y la del `try` se suprime; con `try-with-resources`, es al revés, y es el orden que quieres.

#### 5.2.2 Leer un archivo de texto

Para leer el `servicios.txt` del `revisor` hacen falta tres piezas que no habías usado. **`Path`** representa una ruta de archivo como un valor, y se crea con `Path.of("servicios.txt")`; `ruta.getFileName()` da la última parte. **`Files`** es una clase con métodos estáticos para operar sobre un `Path`. Y **`BufferedReader`** es un objeto que lee texto renglón por renglón. `Files.newBufferedReader(ruta)` abre el archivo y devuelve un `BufferedReader`, y como es un recurso, va en un `try-with-resources`:

```java
try (BufferedReader lector = Files.newBufferedReader(ruta)) {
  String renglon;
  while ((renglon = lector.readLine()) != null) {
    // ...usar el renglón...
  }
}
```

`lector.readLine()` devuelve el siguiente renglón como `String`, o `null` cuando se acabó el archivo. Aquí el `null` es la forma que Java eligió desde sus primeras versiones para decir «no hay más»; `Optional` no existía. El `while` (*mientras*) es un bucle que repite su cuerpo mientras la condición sea cierta, y la condición tiene un truco muy común: `(renglon = lector.readLine()) != null` **asigna y compara a la vez**, porque una asignación vale lo que se asignó. Se lee «lee un renglón y guárdalo en `renglon`; si lo que se leyó no es `null`, entra al cuerpo».

Tres cosas de `Files` que conviene saber, todas de la [documentación de la clase](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html):

- **Lee en UTF-8.** `Files.newBufferedReader(ruta)` y `Files.readAllLines(ruta)` decodifican el archivo con UTF-8, que es lo que quieres para un archivo con acentos. (Otras partes de Java, como `FileReader`, usaban la codificación de la máquina, y por eso un programa leía bien en tu computadora y mal en otra; desde Java 18, [JEP 400](https://openjdk.org/jeps/400), la codificación por omisión de toda la plataforma también es UTF-8.)
- **La ruta relativa se resuelve desde la carpeta donde corres `java`**, no desde donde vive el archivo `.java`. `Path.of("servicios.txt")` busca `servicios.txt` en la carpeta actual de la terminal. Es la causa de casi todos los «no encuentra el archivo» que vas a ver, y el proyecto de la sección 5.6 lo mostrará.
- **Un archivo que no existe lanza `NoSuchFileException`**, que como viste en la cadena de herencia es una `IOException`: verificada. Un archivo que existe pero tiene bytes que no son UTF-8 válido lanza `MalformedInputException`, otra `IOException`, al leer.

Una variante a tener presente: `Files.readAllLines(ruta)` lee todo el archivo a una lista de renglones y lo cierra por ti (no necesitas `try-with-resources`), y sirve para archivos chicos. `Files.lines(ruta)` devuelve un *stream* de renglones, pero **ese sí hay que cerrarlo**: el flujo mantiene el archivo abierto, y su documentación lo dice con esas palabras. Para el `revisor` el `BufferedReader` tiene una ventaja decisiva: sabes en qué renglón vas sin escribir un índice aparte, y el número de renglón es lo que necesita un mensaje de error útil.

### 5.3 Excepciones propias, con su causa

#### 5.3.1 Por qué una propia

La configuración puede fallar por una docena de razones, y todas aterrizan en el mismo lugar: `main`, que tiene que decirle algo a una persona. Si cada razón fuera una excepción distinta del JDK, `main` tendría que atrapar `NoSuchFileException`, `MalformedInputException`, `URISyntaxException`, `NumberFormatException` y media docena más, y conocer los detalles de cada una. Peor: si todas fueran `IllegalArgumentException`, `main` no podría distinguir «la configuración está mal» de «mi programa tiene un defecto». La solución es una excepción con nombre propio, que significa algo del dominio: **`ErrorDeConfiguracion`**. Tendrá un solo significado («el archivo de configuración no se puede usar»), `main` atrapará solo esa, y cualquier otra que escape será, con razón, una falla del programa que debe detenerlo con su traza.

Una excepción propia se define con una línea de **herencia**. Mira la figura `fig05_07`, una carpeta con dos archivos: la definición de la excepción y un programa que la usa. La palabra nueva es **`extends`**: una clase que `extends` otra *hereda* todo lo que la otra tiene. Es como `implements` pero para una clase en lugar de una interfaz: `ErrorDeConfiguracion` *es una* `Exception` y trae todos sus métodos (`getMessage`, `getCause`, `printStackTrace`) sin escribirlos. Lo único que hace falta es decir cómo se construye, y para eso se llama al constructor de la clase madre con **`super`**: `super(mensaje, causa)` significa «constrúyeme como una `Exception` con este mensaje y esta causa».

```java
// fig05_07/ErrorDeConfiguracion.java
final class ErrorDeConfiguracion extends Exception {
  ErrorDeConfiguracion(String mensaje) {
    super(mensaje);
  }

  ErrorDeConfiguracion(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
```

```java
// fig05_07/Demo.java
void leer(Path ruta) throws ErrorDeConfiguracion {
  try {
    Files.readAllLines(ruta);
  } catch (NoSuchFileException e) {
    throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e);
  } catch (IOException e) {
    throw new ErrorDeConfiguracion("no se pudo leer el archivo " + ruta.getFileName(), e);
  }
}

void main() {
  try {
    leer(Path.of("servicios-que-no-existe.txt"));
  } catch (ErrorDeConfiguracion e) {
    IO.println("mensaje: " + e.getMessage());
    IO.println("causa:   " + e.getCause());
    e.printStackTrace();
  }
}
```

El programa `Demo` define un método `leer` que intenta leer un archivo que no existe y, si falla, lanza **`ErrorDeConfiguracion` con una causa**: `throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e)`. El segundo argumento, `e`, es la excepción original. Los dos `catch` están en el orden correcto: primero la `NoSuchFileException` (más particular), después la `IOException` general. Corre la figura desde su carpeta:

```text
$ cd fig05_07
$ java Demo.java
mensaje: no existe el archivo servicios-que-no-existe.txt
causa:   java.nio.file.NoSuchFileException: servicios-que-no-existe.txt
ErrorDeConfiguracion: no existe el archivo servicios-que-no-existe.txt
	at Demo.leer(Demo.java:6)
	at Demo.main(Demo.java:14)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.execute(SourceLauncher.java:256)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.run(SourceLauncher.java:138)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.main(SourceLauncher.java:76)
Caused by: java.nio.file.NoSuchFileException: servicios-que-no-existe.txt
	at java.base/sun.nio.fs.UnixException.translateToIOException(UnixException.java:92)
	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:106)
	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:111)
	at java.base/sun.nio.fs.UnixFileSystemProvider.newFileChannel(UnixFileSystemProvider.java:213)
	at java.base/sun.nio.fs.UnixFileSystemProvider.newByteChannel(UnixFileSystemProvider.java:244)
	at java.base/java.nio.file.Files.newByteChannel(Files.java:357)
	at java.base/java.nio.file.Files.newByteChannel(Files.java:399)
	at java.base/java.nio.file.spi.FileSystemProvider.newInputStream(FileSystemProvider.java:371)
	at java.base/java.nio.file.Files.newInputStream(Files.java:154)
	at java.base/java.nio.file.Files.newBufferedReader(Files.java:2645)
	at java.base/java.nio.file.Files.readAllLines(Files.java:3085)
	at java.base/java.nio.file.Files.readAllLines(Files.java:3122)
	at Demo.leer(Demo.java:4)
	... 6 more
```

Lee la salida con calma, porque es la primera traza con **dos** partes. Las dos primeras líneas son las que imprimió el programa: `mensaje:` es lo que le dirías a la persona («no existe el archivo servicios-que-no-existe.txt»), y `causa:` es lo que **de verdad** pasó, con su tipo completo: `java.nio.file.NoSuchFileException: servicios-que-no-existe.txt`. El resto es lo que imprime `e.printStackTrace()`, que escribe la traza completa en la salida de error. Tiene dos bloques. El primero es la excepción que atrapaste, con su traza: `Demo.leer(Demo.java:6)` es el renglón del `throw`, y `Demo.main(Demo.java:14)` es quien llamó. Los renglones que siguen, de `jdk.internal.reflect` y `SourceLauncher`, son la maquinaria con la que `java Archivo.java` arranca tu programa. El segundo bloque empieza con **`Caused by:`** («causada por»): es la causa, con su propia traza, que baja hasta el sistema operativo (`sun.nio.fs.UnixException...`) y sube por `Files.readAllLines` hasta la línea `Demo.leer(Demo.java:4)`, que es el renglón donde de verdad se intentó abrir el archivo. La última línea, `... 6 more` («y 6 más»), quiere decir que los seis renglones restantes son los mismos que ya viste arriba.

Eso es lo que le da el valor a conservar la causa. El **mensaje** es para la persona que usa el programa: corto, en español, sin jerga. La **causa** es para quien tiene que arreglar el problema: la excepción original con su traza completa. Perder la causa es perder el rastro. Dos reglas de la casa que de aquí se derivan:

1. **Cuando conviertas una excepción en otra, pasa la original como causa**, siempre.
2. **El mensaje dice qué pasó y qué hacer; la causa dice por qué.** En el mensaje del `revisor` no aparece nunca el texto de la excepción original, porque cambia entre sistemas y versiones; está en la causa.

#### 5.3.2 La peor forma de manejar un error: ignorarlo

Si algo hay que no hacer nunca con una excepción, es esto:

```java
// fig05_08.java
int leerTimeout(String texto) {
  try {
    return Integer.parseInt(texto);
  } catch (Exception e) {
  }
  return 5000;
}

void main() {
  IO.println("timeout_ms = " + leerTimeout("2000"));
  IO.println("timeout_ms = " + leerTimeout("dos mil"));
}
```

```text
$ java fig05_08.java
timeout_ms = 2000
timeout_ms = 5000
```

La segunda línea dice `timeout_ms = 5000` cuando el valor escrito era «dos mil», sin ninguna señal de que algo salió mal. El `catch (Exception e)` **vacío** atrapó la `NumberFormatException`, no hizo nada con ella y la función devolvió el valor por omisión como si nada. Es el equivalente en Java del `except: pass` de Python. La persona que escribió «dos mil» cree que el programa le obedeció, y el programa funciona con otro valor. Cuando algo falle de verdad, el motivo ya desapareció y no hay un dato que investigar. Un `catch` vacío es casi siempre un error, y hay tres razones por las que conviene recordarlo: borra la causa, convierte un error que el compilador te señaló en uno que nadie ve, y ocurre justamente cuando uno tiene prisa por «hacer que compile».

Si de verdad no sabes qué hacer con una excepción, no la atrapes; si la atrapas, **haz algo con ella**: arréglala, tradúcela a otra con causa, o registrala y deja constancia de por qué la ignoras. Una nota sobre `printStackTrace()`: está bien para aprender y para depurar, pero no es una manera de manejar un error; solo lo enseña.

#### 5.3.3 Dos canales de salida

Una pregunta práctica que surge aquí: ¿adónde escribe el `revisor` cuando algo falla? El programa tiene **dos canales** de salida de texto, que el sistema operativo trata por separado. `System.out` es la salida normal, y es donde escribe `IO.println`. `System.err` es la **salida de error**, para los avisos y los fallos. Los dos aparecen en tu terminal, pero un script o una tubería puede separarlos: `java Revisor.java 2> errores.txt` manda solo los errores a un archivo, y `java Revisor.java > reporte.txt` guarda solo el reporte. Por eso el mensaje de configuración se imprime con `System.err.println(...)`. Quien use el programa en un script debe poder guardar el reporte limpio y ver el error aparte. `IO.println` no tiene versión para la salida de error, y por eso desde aquí se usa `System.out` y `System.err` explícitamente cuando importa la diferencia.

### 5.4 ¿Una excepción o un valor?

Ya tienes las dos herramientas para representar que algo no salió como se esperaba: la **excepción** y el **valor** (un `Optional`, o un tipo sellado como `Respuesta`). Y la pregunta de diseño que más importa de la lección: ¿cuál uso cuándo? El programa `fig05_09` revisa los mismos tres nombres de las dos maneras, con un servicio que no responde en medio:

```java
// fig05_09.java
long tiempoOLanza(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142;
    case "pagos" -> 87;
    default -> throw new IllegalStateException(nombre + ": sin conexión");
  };
}

OptionalLong tiempoO(String nombre) {
  return switch (nombre) {
    case "catalogo" -> OptionalLong.of(142);
    case "pagos" -> OptionalLong.of(87);
    default -> OptionalLong.empty();
  };
}

void main() {
  List<String> nombres = List.of("catalogo", "reportes", "pagos");

  IO.println("con una excepción:");
  try {
    for (String nombre : nombres) {
      IO.println("  " + nombre + ": " + tiempoOLanza(nombre) + " ms");
    }
  } catch (IllegalStateException e) {
    IO.println("  se cayó el lote: " + e.getMessage());
  }

  IO.println("con un valor:");
  for (String nombre : nombres) {
    OptionalLong tiempo = tiempoO(nombre);
    String texto = tiempo.isPresent() ? tiempo.getAsLong() + " ms" : "sin respuesta";
    IO.println("  " + nombre + ": " + texto);
  }
}
```

```text
$ java fig05_09.java
con una excepción:
  catalogo: 142 ms
  se cayó el lote: reportes: sin conexión
con un valor:
  catalogo: 142 ms
  reportes: sin respuesta
  pagos: 87 ms
```

Con la excepción, en el segundo servicio el programa abandonó el recorrido completo: `pagos`, que estaba perfectamente bien, nunca se revisó. Con el valor, cada servicio produjo su propia respuesta, la ausencia de una no afectó a las otras, y el reporte salió completo. Esa es la razón de que el `Respuesta.SinRespuesta` de la lección 3 sea un valor.

Se podría arreglar la primera versión poniendo un `try` dentro del bucle, para atrapar cada servicio por separado. Funcionaría, pero fíjate en lo que hiciste: convertiste la excepción en un valor en cada vuelta, es decir, **reinventaste el valor**, con más código. La diferencia entre las dos herramientas es de intención, y de ahí sale una regla para decidir:

| Pregunta | Si la respuesta es sí |
|---|---|
| ¿Es **uno de los resultados normales** de la operación? («este servicio está caído»). | **Un valor.** |
| ¿Quien llama tiene un camino normal para **seguir** con el siguiente? (un lote de cien servicios). | **Un valor.** |
| ¿La operación **no pudo hacerse** y seguir sería inventar? (el archivo de configuración no existe). | **Una excepción.** |
| ¿Es un **defecto del programa**, de quien lo escribió? (un `null` inesperado). | **Una excepción no verificada.** |

Para el `revisor`, entonces: un servicio caído es un valor (`SinRespuesta`) porque la pregunta que el programa responde es «¿está arriba?», y «no» es una respuesta. Un archivo de configuración ilegible es una excepción (`ErrorDeConfiguracion`), porque sin lista de servicios el programa no tiene nada que revisar, y seguir sería inventar una lista. Es una decisión de diseño y la tomas tú, pero no es arbitraria: toda la lección se reduce a hacerte esa pregunta cada vez que algo puede salir mal.

### 5.5 `InterruptedException`: no se traga

La última excepción de la lección es la más malentendida. El `revisor` de hoy va a **esperar**: para simular que un servicio tarda lo que dice su tiempo, la revisión llama a `Thread.sleep(ms)`, que detiene el hilo ese número de milisegundos. Y `Thread.sleep` declara **`throws InterruptedException`**, una excepción verificada. ¿Por qué?

Un programa de Java puede tener varios hilos de ejecución al mismo tiempo (la lección 9 es toda sobre eso, y hoy basta con saber que un *hilo* es una línea de ejecución independiente y que tu programa ya tiene una, el hilo principal). Un hilo puede pedirle a otro que **se detenga**: eso se llama **interrumpirlo**, y es una petición cortés, no una orden: el hilo interrumpido decide cómo y cuándo parar. Mecánicamente, interrumpir un hilo enciende una **bandera** en él (su «estado de interrupción»). Si el hilo está dormido en un `sleep` cuando eso pasa, el `sleep` se despierta de inmediato y lanza `InterruptedException`. Según [la documentación del tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html): una interrupción es «una indicación a un hilo de que debe dejar lo que está haciendo y hacer otra cosa; depende del programador decidir cómo responde, pero es muy común que el hilo termine».

Es verificada para que no puedas olvidarla: alguien le pidió a tu código que se detuviera, y ignorarlo casi nunca es lo correcto. Para ver la mecánica sin necesidad de dos hilos, el programa `fig05_10` se interrumpe a sí mismo: `Thread.currentThread()` devuelve «el hilo que ejecuta este código», y `interrupt()` enciende su bandera.

```java
// fig05_10.java
void main() {
  Thread.currentThread().interrupt();
  IO.println("¿marcado como interrumpido? " + Thread.currentThread().isInterrupted());
  try {
    Thread.sleep(1000);
    IO.println("esto no se imprime");
  } catch (InterruptedException e) {
    IO.println("sleep lanzó: " + e.getMessage());
    IO.println("¿sigue marcado? " + Thread.currentThread().isInterrupted());
    Thread.currentThread().interrupt();
    IO.println("¿y después de restaurarlo? " + Thread.currentThread().isInterrupted());
  }
}
```

```text
$ java fig05_10.java
¿marcado como interrumpido? true
sleep lanzó: sleep interrupted
¿sigue marcado? false
¿y después de restaurarlo? true
```

La primera línea confirma que la bandera está encendida. El `sleep(1000)` **no durmió nada**: vio la bandera, lanzó `InterruptedException` con el mensaje `sleep interrupted` y, y esto es lo importante, **apagó la bandera** al lanzarla: la tercera línea dice `false`. La [documentación de `Thread`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html) lo dice así: «el estado de interrupción del hilo actual se limpia cuando se lanza esta excepción». Eso crea un problema: si atrapas la excepción y sigues como si nada, la petición de detenerse **desaparece**, y nadie más (por ejemplo, el código que llamó al tuyo) se entera de que alguien quería parar. De ahí las dos reglas, y **no hay una tercera**:

1. **Propágala**: declara `throws InterruptedException` y deja que suba. Es lo que hace el `revisor`.
2. **Si debes atraparla**, restaura la bandera antes de seguir: `Thread.currentThread().interrupt()`. Es la cuarta línea de la figura: dejó la bandera encendida otra vez (`true`) para quien venga detrás.

Lo que no se hace es atraparla y callar (`catch (InterruptedException e) {}`): es la versión de la sección 5.3.2 para un caso donde el costo es peor, porque un programa que no obedece cuando le piden que se detenga no se puede apagar con elegancia. En la lección 9 verás quién interrumpe y por qué.

### 5.6 El paso del proyecto: el `revisor` lee su configuración

Ahora a juntar todo. El `revisor` cambia así:

- **`servicios.txt`**, un archivo de texto, reemplaza a la lista escrita a mano. Tiene un servicio por renglón, con el nombre, la dirección y, opcionalmente, el tiempo límite en milisegundos. Los renglones en blanco se ignoran y los que empiezan con `#` son comentarios.
- **`ErrorDeConfiguracion`** es la excepción de la sección 5.3.
- **`Configuracion`** lee el archivo y lo convierte en una lista de `Servicio`, y es donde viven los mensajes de error.
- **`Revision`** cambia: la revisión espera lo que dice el tiempo simulado, y por eso `revisar` y `revisarTodos` declaran `throws InterruptedException`. `revisarTodos` pasa de *stream* a `for`, por lo que viste en 5.1.4.
- **`Revisor.java`** lee la configuración, atrapa `ErrorDeConfiguracion` y la imprime en la salida de error.

Nueve archivos no cambian respecto a la lección 4: `Estado`, `Respuesta`, `Servicio`, `Resultado`, `Clasificador`, `Simulador`, `Resumen`, `Reporte` y `Formato`. Van aquí porque la carpeta de la figura es el proyecto completo. Cópialos tal cual de la lección 4:

```java
// fig05_11/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig05_11/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig05_11/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

```java
// fig05_11/Resultado.java
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
// fig05_11/Clasificador.java
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
// fig05_11/Simulador.java
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
// fig05_11/Resumen.java
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
// fig05_11/Reporte.java
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
// fig05_11/Formato.java
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

El archivo de datos es el que describe el inicio de esta sección: el nombre, la dirección y, si quieres, el tiempo límite. Las direcciones siguen terminando en `.example`, un dominio reservado que nunca resuelve: ninguna petición del curso sale a internet.

```text fig05_11/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

La excepción propia es la de la sección 5.3.1, con sus dos constructores:

```java
// fig05_11/ErrorDeConfiguracion.java
final class ErrorDeConfiguracion extends Exception {
  ErrorDeConfiguracion(String mensaje) {
    super(mensaje);
  }

  ErrorDeConfiguracion(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
```

`Configuracion` es la pieza grande, y vale la pena leerla por partes. Mira el código completo y después seguimos:

```java
// fig05_11/Configuracion.java
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

**`leer`** es el método público. Abre el archivo con `try-with-resources` y recorre los renglones con el `while` de la sección 5.2.2, llevando la cuenta en `numero`. Para cada renglón, `strip()` le quita los espacios de los extremos; si el resultado está vacío (`isEmpty()`) o empieza con `#` (`startsWith("#")`), `continue` salta de inmediato al siguiente renglón (la palabra `continue` significa «termina esta vuelta del bucle y pasa a la siguiente»). Cualquier otro renglón se convierte en un `Servicio` con `leerRenglon`. Los dos `catch` finales convierten las excepciones del JDK en `ErrorDeConfiguracion`, **pasando la original como causa**: primero `NoSuchFileException`, que da «no existe el archivo», y luego la `IOException` general, que da «no se pudo leer el archivo». El nombre del archivo sale de `ruta.getFileName().toString()`, y eso explica el mensaje de la figura de arriba: dice `servicios.txt`, no la ruta completa. Fíjate también en que `numero` cuenta los renglones **del archivo**, incluidos los comentarios y los vacíos: si el error está en el renglón 4 del archivo, el mensaje dice 4, y es el número que verás en tu editor.

**`leerRenglon`** parte el renglón en campos con `renglon.split("\\s+")`. `split` recibe una **expresión regular** (una descripción de un patrón de texto) y devuelve un **arreglo** de textos: `\s+` significa «uno o más espacios o tabuladores», y se escribe con doble barra invertida porque en un texto de Java la barra invertida tiene un uso propio (`\n`, `\"`) y hay que escribirla dos veces para que llegue una. Un **arreglo** (`String[]`) es una secuencia de tamaño fijo: se consulta con `campos[0]` (el primero) y su tamaño es `campos.length`, sin paréntesis, porque es un dato y no un método. Es el mismo tipo de `String[] args` que verás en la lección 6. Si hay menos de dos campos o más de tres, el mensaje dice cuántos hay: `renglón 3: se esperaban 2 o 3 campos y hay 1`.

**`leerUrl`** y **`leerTimeout`** son los dos validadores. `leerUrl` rechaza lo que no empiece con `http://` o `https://` (primer filtro, con su mensaje) y luego intenta construir un `URI` con `new URI(texto)`. Esa forma lanza `URISyntaxException`, verificada, cuando la dirección está mal escrita, y `leerUrl` la convierte en `ErrorDeConfiguracion` con la `URISyntaxException` como causa. (Es la razón de usar `new URI(...)` y no `URI.create(...)`, que usabas en la lección 3: `URI.create` envuelve la misma excepción en una no verificada, y aquí conviene verla tal cual.) `leerTimeout` convierte el texto con `Integer.parseInt` y rechaza lo que no sea un entero mayor que cero. Fíjate en cómo está escrito, porque es un patrón útil: el `try` intenta la conversión y, si el número es válido, **devuelve ahí mismo**; si la conversión falla, el `catch` lanza el error con la `NumberFormatException` como causa; y si la conversión funciona pero el número no es mayor que cero, el `try` no devuelve nada y el flujo cae a la última línea, que lanza el mismo mensaje sin causa. Un mensaje, dos caminos de error.

**`verificarRepetidos`** usa el truco de la sección 4.1.2: `vistos.add(nombre)` devuelve `false` si el nombre ya estaba, y esos nombres se juntan en un `LinkedHashSet` (que conserva el orden en que se encontraron) y `String.join(", ", repetidos)`, que une los elementos con un separador, los lista todos en el mensaje: `nombres repetidos: catalogo`. Un detalle sobre qué hace este método: **el programa se detiene en el primer error**, y no junta todos los errores del archivo para decirlos de una vez. Es más simple, y suficiente para un archivo de cuatro renglones.

Ahora `Revision`, que cambia de dos maneras. Primero, `revisar` espera el tiempo de la respuesta con `Thread.sleep(tiempo.getAsLong())` si lo hay (el `OptionalLong` de la lección 4), y por eso declara `throws InterruptedException`. Segundo, `revisarTodos` es un `for` que llena una lista, porque la lambda de un `map` no puede lanzar la excepción verificada (su `Function` no la declara), como se vio en la sección 5.1.4:

```java
// fig05_11/Revision.java
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

Y `Revisor.java` cierra el círculo. Lee la configuración, revisa y arma el reporte, y atrapa `ErrorDeConfiguracion` para imprimirla en la salida de error con el prefijo `error de configuración:`. Su `main` declara `throws InterruptedException`, que es la regla 1 de la sección 5.5: no la maneja porque no sabría qué hacer, solo la deja subir.

```java
// fig05_11/Revisor.java
void main() throws InterruptedException {
  Path ruta = Path.of("servicios.txt");
  try {
    List<Servicio> servicios = Configuracion.leer(ruta);
    IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
  } catch (ErrorDeConfiguracion e) {
    System.err.println("error de configuración: " + e.getMessage());
  }
}
```

Corre el proyecto desde su carpeta. La revisión **ya espera**: la tabla tarda unos dos segundos y medio en aparecer (la suma de 142, 2300 y 87 ms; medí unos tres segundos en total con el arranque de Java incluido). Es lo que pasaría en serie con servicios de verdad, y es el problema que resolverá la lección 9.

```text
$ cd fig05_11
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Y ahora, el destino de la lección. Corre el `revisor` **desde otra carpeta**, sin pasar por donde está el archivo de configuración. Después ejecútalo con la salida de error descartada (`2>/dev/null`, que significa «manda el canal 2, la salida de error, a la nada») y pide el código de salida del último comando con `$?`:

```text
$ java fig05_11/Revisor.java
error de configuración: no existe el archivo servicios.txt
$ java fig05_11/Revisor.java 2>/dev/null
$ echo "código de salida: $?"
código de salida: 0
```

Tres cosas ocurrieron. Primero, el programa dijo `error de configuración: no existe el archivo servicios.txt`: el `Path.of("servicios.txt")` es relativo, se resolvió contra la carpeta donde estás parado, y ahí el archivo no existe, aunque sí existe en la carpeta del proyecto. Segundo, con `2>/dev/null` **no apareció nada**: el mensaje iba por el canal de error, y al descartarlo no queda salida. Es exactamente la separación de la sección 5.3.3. Tercero, el código de salida fue `0`: el programa falló, y el sistema operativo cree que todo salió bien. Para un script que ejecuta el `revisor` eso es mentira, y la lección 10 lo arregla con `System.exit`. Por ahora, anótalo como defecto conocido.

## El error que vas a ver

El error de esta lección es el de la sección 5.1.2, y casi todos lo encuentran la primera vez que usan un método de entrada y salida:

```text
fig05_02.java:3: error: unreported exception IOException; must be caught or declared to be thrown
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
                                             ^
```

**Primer renglón: dónde y qué.** Trae el archivo y la línea (`fig05_02.java:3`), la palabra `error` y el diagnóstico: `unreported exception IOException`. «No reportada» quiere decir que el método puede lanzar esa excepción y nadie la atrapó ni la declaró. **Segundo y tercero: la línea del código y la flecha.** La `^` apunta al paréntesis de la llamada que puede lanzarla, que es lo que debes mirar. **Qué hacer.** Hay dos arreglos, y la elección es de diseño. Si en ese lugar sabes qué hacer cuando falla, **atrápala** (y haz algo útil, no un `catch` vacío). Si no lo sabes, **decláralas con `throws`** y deja que la maneje quien llamó. Mal arreglo, que a veces se ve: declarar `throws Exception` para callar al compilador. Funciona, y esconde de las firmas qué puede salir mal.

El segundo error de la lección no es del compilador: es el `revisor` desde otra carpeta, y ya lo viste en la sección 5.6. El mensaje `no existe el archivo servicios.txt` no te dice **dónde** lo buscó. Si te topas con él y el archivo existe, lo primero que debes revisar es desde qué carpeta corriste `java`.

## Lo que se hace mal

- **Un `catch` vacío**, o con solo `printStackTrace()`. Es lo de la sección 5.3.2: el error desaparece, o se imprime y el programa sigue como si nada. Costo: valores equivocados que nadie ve, y cuando por fin algo falla, un rastro que ya no existe.

- **Atrapar `Exception` o `Throwable` por si acaso.** Atrapa también los defectos de programación (un `NullPointerException`) y los convierte en un mensaje engañoso. Costo: el defecto real queda enterrado. El único lugar para un `catch (Exception e)` es la última frontera del programa, para registrar y terminar con elegancia, no para continuar.

- **Convertir una excepción en otra sin pasar la causa.** Es escribir `throw new ErrorDeConfiguracion("el archivo no sirve")` sin el segundo argumento. Costo: el mensaje dice que falló y la causa, que era lo que permitía arreglar, ya no está.

- **Devolver un valor mágico** (`-1`, `null`, una cadena vacía) en lugar de lanzar o de devolver un valor con tipo. Costo: el que llama tiene que acordarse de revisar, y el compilador no lo ayuda.

- **Un `return` dentro de un `finally`.** Si el `finally` devuelve un valor, **descarta** la excepción que estaba subiendo, y el error se pierde sin una sola traza. Costo: el mismo de un `catch` vacío, con la diferencia de que el código parece más inocente. El `finally` es para limpiar, no para decidir qué se devuelve.

- **Tragarse una `InterruptedException`.** Costo: un programa que no obedece cuando se le pide que se detenga (sección 5.5). Propágala o restaura la bandera.

- **Usar una excepción para el flujo normal.** Por ejemplo, llamar a `Integer.parseInt` en un bucle y atrapar `NumberFormatException` para decidir si un texto «es un número», cuando es lo esperado, no lo excepcional. Costo: código más lento (crear la traza cuesta) y la decisión de diseño de la sección 5.4 al revés: si la ausencia de un resultado es normal, es un valor.

## Ejercicios

### Ejercicio 1 — Un archivo roto por cada error

`Configuracion` sabe fallar de varias maneras, cada una con su mensaje. Para cada uno de los ocho casos de abajo, **predice el mensaje antes de correr** y luego compruébalo. Crea una carpeta nueva por caso (por ejemplo, `roto-1/`), pon dentro un `servicios.txt` con el contenido indicado y corre el `revisor` **desde esa carpeta**: `java ../revisor/Revisor.java` (ajusta la ruta a donde tengas tu proyecto). Como el programa busca `servicios.txt` en la carpeta actual, no necesitas tocar el código.

1. `catalogo https://catalogo.example` y, en el renglón siguiente, solo `pagos`.
2. Un comentario (`# x`) en el renglón 1 y, en el renglón 2, `catalogo ftp://catalogo.example`.
3. `catalogo https://c.example` y `pagos http://pa|gos.example`.
4. `catalogo https://c.example 5000` y `pagos https://p.example abc`.
5. Dos renglones, los dos con el nombre `catalogo`.
6. Un archivo con solo comentarios y renglones en blanco.
7. `catalogo https://c.example 0`.
8. `catalogo https://c.example 1 2 3`.

### Ejercicio 2 — Haz desaparecer el motivo

Cambia `leerTimeout` en `Configuracion.java` para que el `catch (NumberFormatException e)` no lance nada y simplemente devuelva `5000`. En una carpeta nueva, pon un `servicios.txt` con el único renglón `pagos https://p.example abc` y corre desde ahí (`java ../revisor/Revisor.java`). ¿Qué ves? Después restaura el código. Anota en tu bitácora cuánto tardarías en descubrir el problema si el archivo tuviera cien renglones.

### Ejercicio 3 — Un archivo en otra codificación

En una carpeta nueva (`mkdir latin1 && cd latin1`), crea un archivo con un acento escrito en la codificación antigua ISO-8859-1 (el *latin1* de los sistemas viejos), que no es UTF-8, con este comando:

```bash
printf 'catalogo https://catalogo.example\n# caf\xe9\n' > servicios.txt
```

Corre el `revisor` desde esa carpeta (`java ../revisor/Revisor.java`) y lee el mensaje. Después, agrega un renglón al `catch` de `Revisor.java` para imprimir la causa (`e.getCause()`) y descubre qué excepción del JDK está detrás del mensaje. Restaura el código.

### Ejercicio 4 — Vuelve a convertir `revisarTodos` en un *stream*

Reemplaza el `for` de `revisarTodos` por `servicios.stream().map(Revision::revisar).toList()`. **Predice** si compila y qué dirá el compilador. Después comprueba. Y responde: ¿qué tendrías que cambiar en `revisar` para que esa versión compile?

## Soluciones

### Solución 1

| Contenido de `servicios.txt` | Mensaje |
|---|---|
| `catalogo https://catalogo.example` y `pagos` | `error de configuración: renglón 2: se esperaban 2 o 3 campos y hay 1` |
| `catalogo ftp://catalogo.example` (después de un comentario) | `error de configuración: renglón 2: la url debe empezar con http:// o https://` |
| `pagos http://pa\|gos.example` (renglón 2) | `error de configuración: renglón 2: la url no es válida` |
| `pagos https://p.example abc` (renglón 2) | `error de configuración: renglón 2: timeout_ms debe ser un entero mayor que 0` |
| dos renglones con `catalogo` | `error de configuración: nombres repetidos: catalogo` |
| solo comentarios y renglones en blanco | `error de configuración: el archivo no tiene servicios` |
| `catalogo https://c.example 0` | `error de configuración: renglón 1: timeout_ms debe ser un entero mayor que 0` |
| `catalogo https://c.example 1 2 3` | `error de configuración: renglón 1: se esperaban 2 o 3 campos y hay 5` |

Los ocho mensajes los medí con el proyecto de la sección 5.6. Hay dos cosas que mirar con cuidado. En el caso 2, el comentario del renglón 1 cuenta: el error está en el renglón 2 **del archivo**, no en el segundo servicio. Y en los casos 4 y 7 el mensaje es el mismo aunque el motivo es distinto (un texto que no es número, y un número que no es positivo): para quien escribe el archivo, el arreglo es el mismo. La diferencia está en la causa, que solo existe en el primero (la `NumberFormatException`), y la ve quien depura.

### Solución 2

```java
static int leerTimeout(String texto, int numero) throws ErrorDeConfiguracion {
  String mensaje = "renglón " + numero + ": timeout_ms debe ser un entero mayor que 0";
  try {
    int timeoutMs = Integer.parseInt(texto);
    if (timeoutMs > 0) {
      return timeoutMs;
    }
  } catch (NumberFormatException e) {
    return 5000;
  }
  throw new ErrorDeConfiguracion(mensaje);
}
```

Con un `servicios.txt` que solo tenga `pagos https://p.example abc`, el `revisor` imprime la tabla de `pagos` como `ARRIBA`, sin ningún aviso: `abc` desapareció y el servicio quedó con el tiempo límite por omisión. Con cien renglones, no tendrías forma de saber cuál de ellos tenía un tiempo inválido: el programa «funciona». Es el problema de la sección 5.3.2 en pequeño. El mensaje de error original (`timeout_ms debe ser un entero mayor que 0`) no era una molestia: era el único aviso que la persona que escribe el archivo iba a recibir.

### Solución 3

El mensaje es `error de configuración: no se pudo leer el archivo servicios.txt`: no dice por qué. Con `System.err.println(e.getCause());` en el `catch` de `Revisor.java`, se imprime:

```text
java.nio.charset.MalformedInputException: Input length = 1
```

El byte `0xE9` es la `é` en ISO-8859-1, pero como secuencia UTF-8 es inválida (en UTF-8 la `é` ocupa dos bytes). `Files.newBufferedReader` decodifica en UTF-8 y, al toparse con el byte, lanza `MalformedInputException`, una `IOException` que `Configuracion.leer` convierte en «no se pudo leer». Observa que el mensaje para la persona es correcto pero poco informativo, y que la **causa** tiene el dato que hace falta. Es la utilidad de conservarla: mensaje corto para el usuario, causa completa para quien depura.

### Solución 4

No compila. El compilador dice (acorté la ruta, que el lanzador imprime completa):

```text
Revision.java:20: error: incompatible thrown types InterruptedException in functional expression
    return servicios.stream().map(Revision::revisar).toList();
                                  ^
```

`Revision::revisar` declara `throws InterruptedException` y la `Function` que espera `map` no puede lanzarla. Para que compile tendrías que quitar el `throws` de `revisar`, y eso significa **atrapar la `InterruptedException` dentro de `revisar`**, donde no sabes qué hacer con ella; lo correcto sería restaurar la bandera con `Thread.currentThread().interrupt()` y devolver algo, lo que esconde la interrupción a quien llamó. Por eso el curso conserva el `for`: es el que deja pasar la excepción hasta quien sí puede decidir.

## Cómo sé que lo logré

- [ ] `java fig05_01.java` imprime las cinco cadenas, y puedes decir por qué `NoSuchFileException` es verificada y `NumberFormatException` no.
- [ ] `java fig05_02.java` falla con `unreported exception IOException`, y sabes los dos arreglos que acepta el compilador.
- [ ] `java fig05_04.java` falla con `has already been caught`, y sabes por qué el orden de los `catch` va de lo particular a lo general.
- [ ] `java fig05_06.java` imprime los cierres en orden inverso, y encuentras en la salida la excepción suprimida.
- [ ] Desde la carpeta de la figura de las excepciones propias, `java Demo.java` imprime un `Caused by:`, y sabes qué excepción del JDK está detrás.
- [ ] `java fig05_10.java` imprime `true`, `false` y `true`, y sabes explicar qué hizo cada uno.
- [ ] Desde la carpeta del proyecto, `java Revisor.java` imprime la tabla con `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`, y tarda unos dos segundos y medio.
- [ ] Desde otra carpeta, el `revisor` imprime `error de configuración: no existe el archivo servicios.txt`, y sabes explicar por qué el archivo existe y aun así no lo encuentra.
- [ ] Los ocho archivos rotos del ejercicio 1 producen los ocho mensajes de la solución, y los predijiste antes de correr.
- [ ] Anotaste en tu bitácora una excepción verificada que te obligó a decidir algo.

## Resumen

Responde sin mirar la lección:

1. ¿Qué diferencia hay entre una excepción verificada y una no verificada? ¿Cuál obliga el compilador a tratar?
2. ¿Qué significa `unreported exception IOException; must be caught or declared to be thrown`, y cuáles son los dos arreglos?
3. ¿Qué garantiza un `finally`? ¿Y por qué no debe llevar un `return`?
4. ¿Por qué el orden de los `catch` va de lo particular a lo general?
5. ¿Qué garantiza `try-with-resources` frente a cerrar a mano? ¿En qué orden cierra?
6. ¿Por qué una excepción verificada no puede lanzarse desde dentro de la lambda de un `map`, y qué usa el `revisor` en su lugar?
7. ¿Qué es la causa de una excepción, y qué pasa si no la pasas al convertirla en otra?
8. ¿Por qué un servicio caído es un valor y un archivo de configuración ilegible es una excepción?
9. ¿Qué hace `Thread.sleep` cuando alguien interrumpe el hilo, y qué le pasa a la bandera de interrupción?
10. ¿Cuáles son las dos formas correctas de manejar una `InterruptedException`?

## Para leer más

- [Oracle, «Lesson: Exceptions» (tutorial de Java)](https://docs.oracle.com/javase/tutorial/essential/exceptions/index.html): el tema completo, desde qué es una excepción hasta encadenarlas, con la discusión sobre las no verificadas. Consultado el 7 de octubre de 2026.
- [The Java Language Specification, capítulo 11, «Exceptions»](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html): la definición exacta de verificada y no verificada y de qué debe declarar cada método. Consultado el 7 de octubre de 2026.
- [Oracle, «The try-with-resources Statement»](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html): el orden de cierre y las excepciones suprimidas, con ejemplos. Consultado el 7 de octubre de 2026.
- [Oracle, «Interrupts» (tutorial de concurrencia)](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html): cómo se detiene un hilo con cortesía, y por qué `InterruptedException` no se ignora. Consultado el 7 de octubre de 2026.

---

**Anterior:** [Lección 4 — Colecciones, genéricos y streams](04-colecciones-genericos-streams.md) ·
**Siguiente:** [Lección 6 — Proyecto con Maven](06-proyecto-maven.md)
