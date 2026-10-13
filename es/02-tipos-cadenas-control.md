# Lección 2 — Tipos, cadenas y control de flujo

**Tiempo:** 2 × 90 min (dos sesiones; el corte está entre las secciones 2.2 y 2.3)

**Qué construyes:** las funciones base del `revisor`: decidir si un servicio está arriba, lento o caído, dar formato al tiempo y armar los renglones de la tabla, todo en un solo archivo compacto

**Qué aprendes:** tipos primitivos y de referencia, envoltorios y `var`; `String` es inmutable y por qué `==` no es `equals`; dar formato con `String.format` y `Locale.ROOT`; bloques de texto; `if`, `&&` contra `&` y el operador `? :`; `switch` como expresión; métodos y constantes en un archivo compacto; `null` y cómo leer el mensaje de un `NullPointerException`

**De dónde vienes.** Traes el entorno de la [Lección 1](01-instalacion.md): un JDK 25 que responde a `java --version`, el hábito de correr un archivo con `java Archivo.java` y un `~/curso-java/revisor/Revisor.java` de una sola línea que imprime «revisor: todavía no revisa nada». Sabes también leer el primer error del compilador: el archivo, el número de renglón, el `^` que señala el punto y las líneas `symbol` y `location`. Esta lección usa esa habilidad todo el tiempo, porque Java es un lenguaje que **te regaña antes de ejecutar**, y aprender a entender sus regaños es la mitad del oficio.

**Qué no hace esta lección.** No usa listas, ni bucles, ni clases tuyas, ni consulta ninguna red. Los datos de los cuatro servicios del `revisor` están escritos en dos `switch` paralelos, a propósito: es una solución incómoda, y la [Lección 3](03-records-enums-sellados.md) la reemplaza por tipos que expresan lo que significa «un servicio respondió» y «un servicio no respondió». Las colecciones llegan en la Lección 4; las excepciones, en la 5. Aquí, cuando un programa falla a propósito, lo usamos para **leer** el error, no para atraparlo.

**Sobre las corridas.** Cada figura es un archivo `fig02_NN.java` que se ejecuta con `java fig02_NN.java`, desde la carpeta donde lo guardaste. Las tienes ya escritas en el repositorio, en [`programas/02-tipos-cadenas-control/`](../programas/02-tipos-cadenas-control/), pero aprendes más si las tecleas. Todas las salidas de esta lección se obtuvieron con Temurin 25.0.4 y la variable `LANG` en `C.UTF-8`, que es lo que hace que la terminal imprima bien los acentos y la raya «—»; con `LANG=C`, Java imprime `?` en su lugar. Si tu parche de Java es otro, los mensajes de error son los mismos; lo único que cambia entre actualizaciones son los números de línea de los renglones `at java.base/...` de una traza, que son del JDK y no tuyos. Las figuras que fallan a propósito traen su error al lado, en un archivo `fig02_NN.error-esperado.txt`.

## Al terminar vas a poder

- Elegir entre `int`, `long`, `double` y `boolean` para un dato, y explicar qué le pasa a un `int` que se pasa de su máximo.
- Distinguir un valor primitivo de una referencia y de un envoltorio (`int` contra `Integer`), y decir cuándo una variable puede valer `null`.
- Usar `var` donde ayuda y saber qué tipo le dio el compilador.
- Explicar por qué `s.strip();` no cambia a `s`, y por qué `==` entre dos `String` puede dar `false` aunque se vean iguales.
- Dar formato a un texto con `String.format` y `Locale.ROOT`, y escribir un bloque de texto de varios renglones.
- Escribir un `switch` como expresión con flechas, y explicar por qué `&&` y `&` no son lo mismo.
- Leer el mensaje de un `NullPointerException`, saber qué variable era nula y por qué a veces el mensaje dice `<local1>` en lugar de su nombre.

## El porqué antes del cómo

Llegas a Java con un lenguaje a cuestas, y ese lenguaje te dejó hábitos. Algunos sirven aquí tal cual: un `if` es un `if`. Otros son trampas, porque Java se parece lo suficiente a lo que ya sabes como para que no sospeches. Piensa en tres expresiones que, en el lenguaje que traes, quizá signifiquen una cosa, y en Java significan otra:

- Comparar dos textos con `==`. En Python o en Go compara su contenido. En Java compara si son **el mismo objeto**, y a veces te dice `true` por casualidad.
- Llamar a `texto.strip()` y pensar que `texto` quedó limpio. En Java el texto **nunca cambia**: el método te devuelve otro, y si no lo guardas, se pierde sin avisar.
- Escribir `x != null & x.length() > 0` creyendo que es lo mismo que con `&&`. Revienta con un `NullPointerException` justo cuando `x` es nulo, o sea, justo cuando la comprobación servía.

Hay un estudio con cientos de miles de estudiantes de Java (Brown y Altadmri, con los datos de BlueJ) que midió qué errores tardan más en arreglarse. Los docentes que participaron no acertaron: su intuición sobre cuáles eran los difíciles no coincidió con los datos. Los que más tardan son justo estos tres: confundir `==` con `equals`, confundir `&` con `&&` y no usar el valor que devuelve un método. Por eso esta lección no los menciona de pasada: **cada uno se provoca a propósito, se ve fallar y se explica**. Puedes leer el artículo en la [página de la Universidad de Kent](https://kar.kent.ac.uk/57219/).

El camino tiene dos sesiones. La **primera** cubre 2.1 y 2.2: los tipos y las cadenas. Al terminarla sabrás qué guarda cada variable y qué hace un texto cuando lo «modificas». La **segunda** cubre 2.3 a 2.5: el control de flujo, el `null` y el paso del proyecto. Al terminarla tendrás el `revisor` imprimiendo esta tabla, calculada por funciones tuyas:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Esa tabla es el hilo de todo el curso. Fíjate en el último renglón: `reportes` no respondió, así que no tiene tiempo ni código. Cómo representar «no hay dato» en un lenguaje con tipos estrictos es la pregunta que cruza toda la lección, y su mala respuesta —`null`— es la razón de la mitad de los errores que vas a ver.

## Los conceptos

### 2.1 Tipos

#### 2.1.1 Primitivos: valores sueltos de tamaño fijo

En Java toda variable tiene un **tipo** que escribes tú, antes del nombre, y que **no cambia nunca**. `int codigo = 200;` declara una variable llamada `codigo` que solo puede guardar enteros de ese tamaño. Si más adelante intentas guardar un texto en ella, el compilador se niega. Lo recordarás de Go o de Rust; si vienes de Python o de JavaScript, es el cambio más grande: los errores de tipo aparecen **antes** de ejecutar, no durante.

Los tipos de Java se dividen en dos familias. La primera son los **primitivos**: ocho tipos, incorporados al lenguaje, que guardan el valor directamente, sin objeto de por medio. En el `revisor` solo necesitas cuatro, pero conviene ver los ocho:

| Tipo | Qué guarda | Tamaño | Rango o ejemplo |
|---|---|---|---|
| `byte` | entero muy pequeño | 8 bits | de −128 a 127 |
| `short` | entero pequeño | 16 bits | de −32 768 a 32 767 |
| `int` | entero (el de uso común) | 32 bits | de −2 147 483 648 a 2 147 483 647 |
| `long` | entero grande | 64 bits | hasta unos 9.2 × 10¹⁸ |
| `float` | decimal de precisión sencilla | 32 bits | rara vez se usa |
| `double` | decimal de precisión doble | 64 bits | el decimal de uso común |
| `boolean` | verdadero o falso | no se fija | `true` o `false` |
| `char` | una unidad de texto UTF-16 | 16 bits | `'c'`, entre comillas sencillas |

Los rangos y los tamaños no los decide tu computadora ni tu sistema operativo: los fija el [lenguaje](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2), y por eso un `int` mide lo mismo en todas partes. Los nombres se escriben con minúscula; eso te avisa que no son clases.

Mira el programa `fig02_01.java`. Declara un dato de cada uno de los cuatro tipos que usará el `revisor` (más un `char`, por completar la vista) y hace un poco de aritmética. Una nota sobre la sintaxis: el `+` entre un texto y cualquier otra cosa **convierte esa cosa a texto y la pega**, que es como se arma cada renglón de salida. Y `IO.println` es la que viste en la Lección 1; su hermana `IO.print` hace lo mismo pero sin saltar de línea al final.

```java
// fig02_01.java
void main() {
  int codigo = 200;
  long tiempoMs = 2300L;
  double segundos = tiempoMs / 1000.0;
  boolean lento = tiempoMs >= 1000;
  char inicial = 'c';

  IO.println("código:    " + codigo);
  IO.println("tiempo:    " + tiempoMs);
  IO.println("segundos:  " + segundos);
  IO.println("lento:     " + lento);
  IO.println("inicial:   " + inicial);

  IO.println("7 / 2      = " + 7 / 2);
  IO.println("7 / 2.0    = " + 7 / 2.0);
  IO.println("2300 / 1000 = " + 2300 / 1000);
  IO.println("0.1 + 0.2  = " + (0.1 + 0.2));
}
```

```text
$ java fig02_01.java
código:    200
tiempo:    2300
segundos:  2.3
lento:     true
inicial:   c
7 / 2      = 3
7 / 2.0    = 3.5
2300 / 1000 = 2
0.1 + 0.2  = 0.30000000000000004
```

Hay tres cosas en esa salida que merecen que te detengas.

**La división de enteros descarta la parte decimal.** `7 / 2` da `3`, no `3.5`, porque los dos operandos son `int` y el resultado es un `int`. Para obtener `3.5` basta que uno de los dos sea `double`: `7 / 2.0`. Lo mismo pasa con `2300 / 1000`, que da `2`, no `2.3`. Esto sorprende a quien viene de Python 3 (donde `/` siempre da decimal) o de JavaScript (donde todo es decimal), y no a quien viene de Go o de Rust. Es el error silencioso más común en cuentas de tiempo: si calculas segundos como `tiempoMs / 1000` obtienes segundos **enteros**, y 999 ms son 0 s.

**La variable `tiempoMs` es un `long` y su valor termina en `L`.** `2300L` es un literal de tipo `long`; sin la `L`, `2300` sería un `int`. Para números chicos la diferencia no se nota, porque Java convierte un `int` en `long` solo (es una conversión que no pierde nada). Con números grandes la `L` es obligatoria, como verás en un momento.

**`0.1 + 0.2` da `0.30000000000000004`.** No es un defecto de Java: es la forma en que cualquier computadora guarda decimales binarios (el estándar IEEE 754), y te pasa igual en Python, JavaScript, Go y Rust. Lo importante es la consecuencia: **nunca compares decimales con `==`**, y nunca guardes dinero en un `double`. El `revisor` guarda sus tiempos en milisegundos **enteros** por esa razón: es un número que no necesita decimales.

Ahora, la trampa que sí es propia de Java y de los lenguajes que comparten su modelo de enteros de tamaño fijo. ¿Qué pasa cuando un número se pasa de su máximo? Mira `fig02_02.java`:

```java
// fig02_02.java
void main() {
  int maximo = Integer.MAX_VALUE;
  IO.println("máximo de un int: " + maximo);
  IO.println("máximo + 1:       " + (maximo + 1));

  long mayor = Long.MAX_VALUE;
  IO.println("máximo de un long: " + mayor);

  int milisegundosEnUnDia = 24 * 60 * 60 * 1000;
  IO.println("ms en un día:      " + milisegundosEnUnDia);
  int milisegundosEnUnMes = 30 * 24 * 60 * 60 * 1000;
  IO.println("ms en 30 días (int):  " + milisegundosEnUnMes);
  long enUnMes = 30L * 24 * 60 * 60 * 1000;
  IO.println("ms en 30 días (long): " + enUnMes);
}
```

```text
$ java fig02_02.java
máximo de un int: 2147483647
máximo + 1:       -2147483648
máximo de un long: 9223372036854775807
ms en un día:      86400000
ms en 30 días (int):  -1702967296
ms en 30 días (long): 2592000000
```

Un `int` que llega a su máximo y recibe uno más **da la vuelta** y se convierte en el mínimo, sin ningún aviso: no hay error, no hay excepción, solo un número negativo donde esperabas uno enorme. En el programa, treinta días en milisegundos no caben en un `int` (el resultado correcto es 2 592 000 000, que es más que el máximo de 2 147 483 647), y el valor que sale es `-1702967296`. El arreglo es hacer la cuenta en `long`: basta escribir una `L` en **uno** de los números, `30L * 24 * 60 * 60 * 1000`, y todo el cálculo se hace en `long`. Fíjate en la diferencia entre las dos últimas líneas: es exactamente una letra.

Un detalle de lectura de esa figura: `Integer.MAX_VALUE` y `Long.MAX_VALUE` son constantes que traen las clases `Integer` y `Long`, de las que hablamos enseguida. Y la primera línea del `main` de cada figura de esta lección es la que ya conoces: `void main()`, el archivo compacto de la Lección 1.

#### 2.1.2 Referencias, envoltorios y `null`

La segunda familia de tipos son los de **referencia**: todo lo que no es uno de los ocho primitivos. `String` es un tipo de referencia. También lo son `Integer`, `Long`, `URI` o cualquier clase que escribas. La diferencia con un primitivo es profunda, y se entiende con una imagen. Una variable primitiva es **una caja con el valor adentro**: `int codigo = 200` es una caja que dice 200. Una variable de referencia es **una caja con una dirección adentro**: la dirección de dónde está el objeto. Dos variables de referencia pueden tener la misma dirección (y entonces son el mismo objeto), o direcciones distintas que apuntan a objetos que se ven iguales. Esa distinción es la que hará falta entender en 2.2.2.

Y una variable de referencia puede tener, en lugar de una dirección, **ninguna**. Ese «ninguna» se escribe `null`. Los primitivos no pueden ser `null`: un `int` siempre vale algún número. Esta diferencia importa para el `revisor`, porque `reportes` no respondió y su código HTTP no es «cero» ni «menos uno»: **no existe**. Con un `int` no hay manera honesta de decir eso. Con una referencia sí.

Por eso Java trae un **envoltorio** para cada primitivo: `Integer` para `int`, `Long` para `long`, `Double` para `double`, `Boolean` para `boolean`, `Character` para `char`. Un envoltorio es un objeto cuyo único trabajo es contener un valor primitivo, y como es de referencia, **puede ser `null`**. Java convierte entre el primitivo y su envoltorio de forma automática, en las dos direcciones; se llama *autoboxing* al meter el valor en la caja (de `int` a `Integer`) y *unboxing* al sacarlo. Mira `fig02_03.java`, que junta lo que acabamos de decir:

```java
// fig02_03.java
void main() {
  int codigo = 200;
  Integer codigoCaja = 200;
  Integer sinRespuesta = null;

  IO.println("primitivo:  " + codigo);
  IO.println("envoltorio: " + codigoCaja);
  IO.println("nulo:       " + sinRespuesta);
  IO.println("¿iguales?   " + codigoCaja.equals(codigo));

  Integer a = 127;
  Integer b = 127;
  IO.println("127 == 127:   " + (a == b));
  Integer c = 2300;
  Integer d = 2300;
  IO.println("2300 == 2300: " + (c == d));
  IO.println("2300 equals:  " + c.equals(d));
}
```

```text
$ java fig02_03.java
primitivo:  200
envoltorio: 200
nulo:       null
¿iguales?   true
127 == 127:   true
2300 == 2300: false
2300 equals:  true
```

Tres líneas merecen comentario. `Integer sinRespuesta = null;` es una declaración perfectamente válida, y al imprimirla sale `null`. `codigoCaja.equals(codigo)` llama al método `equals` del envoltorio con un `int`, y Java lo mete en una caja por ti para compararlo: da `true`. Y las dos comparaciones con `==` son la **sorpresa de esta sección**: `127 == 127` da `true` y `2300 == 2300` da `false`, aunque en las dos líneas se comparan dos envoltorios con el mismo valor. Eso es porque `==` entre dos referencias pregunta «¿son el mismo objeto?», y no «¿valen lo mismo?». Java reutiliza los objetos `Integer` de −128 a 127 (es una garantía de la [especificación](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.1.7)), así que `127` y `127` son el mismo objeto y `2300` y `2300` son dos objetos distintos. Con `==` entre envoltorios tu programa funciona con los códigos HTTP chicos y falla con los tiempos grandes, que es el peor tipo de error: el que depende del dato.

La regla práctica es corta. **Entre primitivos, `==` compara valores. Entre referencias, compara identidad, y para comparar valores se usa `equals`.** Y una advertencia propia de los envoltorios: `Long.equals` compara también el tipo. Un `Long` con 142 nunca es igual a un `Integer` con 142; `equals` devuelve `false` si le pasas el otro tipo. Cuando compares un `Long` con un literal, escribe la `L`.

En el `revisor` vas a usar envoltorios exactamente para lo que sirven: `Integer codigoDe(String nombre)` devuelve el código HTTP del servicio, o `null` si el servicio no respondió; `Long tiempoDe(String nombre)` hace lo mismo con el tiempo. La Lección 3 reemplazará este truco por algo mejor, pero para entender por qué es mejor hay que haberlo sufrido.

#### 2.1.3 Los tipos se revisan antes de ejecutar

Como cada variable tiene su tipo fijo, el compilador revisa que lo que le asignas **quepa**. Va a regañarte con un mensaje que conviene reconocer, porque lo verás mucho. Mira `fig02_04.java`, donde un `long` intenta meterse en un `int`:

```java
// fig02_04.java
void main() {
  long tiempoMs = 2300L;
  int tiempoEntero = tiempoMs;
  IO.println(tiempoEntero);
}
```

```text
$ java fig02_04.java
fig02_04.java:4: error: incompatible types: possible lossy conversion from long to int
  int tiempoEntero = tiempoMs;
                     ^
1 error
error: compilation failed
```

Léelo con lo que aprendiste en la Lección 1. Primero el archivo y el renglón (`fig02_04.java:4`), luego la clase de error (`incompatible types`), después el detalle («una conversión que podría perder información, de `long` a `int`»), y el `^` que apunta al lugar. El lanzador cierra con `error: compilation failed`.

El mensaje dice algo importante con la palabra **posible**. Java no sabe si el valor cabe: ve el tipo, no el contenido. Un `long` *podría* valer 3 000 000 000, y un `int` no lo soporta, así que el compilador te obliga a decidir. Tienes tres salidas, y la correcta depende del caso. Cambiar el tipo de destino a `long` es casi siempre lo mejor. Escribir un **molde** (*cast*) como `(int) tiempoMs` le dice al compilador «yo sé lo que hago», y es peligroso: no hay protección, y `(int) 3_000_000_000L` da `-1294967296`, el mismo desbordamiento silencioso de 2.1.1. La tercera es no mezclar los tipos desde el principio. El molde se usa, con mucho cuidado, cuando de verdad quieres truncar, por ejemplo `(int) 2.9` da `2`.

En el sentido contrario no hay queja: un `int` entra en un `long` y un `long` en un `double`, porque ahí nunca se pierde el orden de magnitud. Por eso `double segundos = tiempoMs / 1000.0` de `fig02_01.java` compiló sin ceremonias.

#### 2.1.4 `var`: dejar que el compilador escriba el tipo

Escribir el tipo en cada variable da seguridad pero es repetitivo cuando el valor del lado derecho ya lo dice todo. Para eso existe `var`. Mira `fig02_05.java`:

```java
// fig02_05.java
void main() {
  var nombre = "catalogo";
  var codigo = 200;
  var tiempoMs = 2300L;
  var segundos = tiempoMs / 1000.0;

  IO.println(nombre + " " + codigo + " " + tiempoMs + " " + segundos);
}
```

```text
$ java fig02_05.java
catalogo 200 2300 2.3
```

Cada `var` es una variable de tipo fijo, igual que si lo hubieras escrito: `nombre` es un `String`, `codigo` es un `int`, `tiempoMs` es un `long` (por la `L`) y `segundos` es un `double`. **`var` no vuelve dinámico a Java**: el compilador deduce el tipo **una vez**, del valor con que inicializas, y desde ahí la variable no puede cambiarlo. Si intentas luego `nombre = 5;`, el compilador se queja igual que antes. La [propuesta que lo introdujo](https://openjdk.org/jeps/286) (JEP 286, Java 10) lo limita a variables locales con inicializador: no vale en campos ni en parámetros ni en tipos de retorno.

Sin valor del que deducir, no hay tipo. Mira qué pasa con `fig02_06.java`:

```java
// fig02_06.java
void main() {
  var respuesta = null;
  IO.println(respuesta);
}
```

```text
$ java fig02_06.java
fig02_06.java:3: error: cannot infer type for local variable respuesta
  var respuesta = null;
      ^
  (variable initializer is 'null')
1 error
error: compilation failed
```

`null` por sí solo no dice qué tipo es, y el compilador se rehúsa a adivinar. Es el mismo problema que con los envoltorios, visto desde otro lado: un valor «sin nada» necesita un tipo que le dé sentido.

**Cuándo usar `var`.** En este curso lo usamos cuando el tipo salta a la vista en el mismo renglón (`var nombre = "catalogo";`) y lo evitamos cuando oculta información (`var resultado = revisar(servicio);` no te dice qué es `resultado` sin ir a leer `revisar`). Es un criterio de lectura, no una regla del lenguaje: el código se lee muchas más veces de las que se escribe.

### 2.2 Cadenas

#### 2.2.1 `String` es un objeto, y es inmutable

`String` es una clase de la biblioteca estándar, así que cada texto es un objeto con métodos: se escriben detrás de un punto, igual que en Python o en JavaScript. Los más usados son `length()` (cuántos caracteres), `strip()` (quita los espacios de los extremos), `isBlank()` (¿está vacío o solo tiene espacios?), `toUpperCase()`, `contains(otro)`, `startsWith(prefijo)` y `substring(desde, hasta)` (un pedazo; el segundo número no se incluye). Ve cómo se usan en `fig02_07.java`, y fíjate sobre todo en los dos primeros renglones de salida:

```java
// fig02_07.java
void main() {
  String linea = "  catalogo  ";
  linea.strip();
  IO.println("[" + linea + "]");

  linea = linea.strip();
  IO.println("[" + linea + "]");

  IO.println("longitud:    " + linea.length());
  IO.println("mayúsculas:  " + linea.toUpperCase());
  IO.println("¿contiene 'log'? " + linea.contains("log"));
  IO.println("¿empieza con 'cat'? " + linea.startsWith("cat"));
  IO.println("recorte:     " + linea.substring(0, 3));
  IO.println("¿vacía?      " + "   ".isBlank());
}
```

```text
$ java fig02_07.java
[  catalogo  ]
[catalogo]
longitud:    8
mayúsculas:  CATALOGO
¿contiene 'log'? true
¿empieza con 'cat'? true
recorte:     cat
¿vacía?      true
```

**El primer renglón de salida es el error.** Escribimos `linea.strip();` y el texto siguió con sus espacios: `[  catalogo  ]`. No hubo advertencia ni error. Es **el valor de retorno ignorado**, uno de los tres errores que más tardan en arreglarse según el estudio que mencioné. La causa es que los `String` de Java son **inmutables**: una vez creado, un texto no cambia. La [documentación](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html) lo dice así: los textos son constantes y sus valores no cambian después de creados. Todos los métodos que parecen «modificar» (`strip`, `toUpperCase`, `substring`...) en realidad **devuelven un texto nuevo** y dejan el original intacto. Si no lo guardas, lo tiraste.

La línea correcta es la de abajo: `linea = linea.strip();`, que reasigna la **variable** para que apunte al texto nuevo. Fíjate en lo que cambió: no el objeto, sino a quién apunta la variable. Es el modelo de las cajas con direcciones de 2.1.2.

¿Por qué se diseñó así? Porque un texto inmutable se puede compartir sin miedo: si cien partes del programa guardan el mismo `String`, ninguna puede estropearlo para las demás. Es una propiedad que ya conoces de Python (`str`) y de JavaScript (los textos primitivos), y que volverás a ver en Java con los *records* de la Lección 3. El costo es que el programador debe recordar la regla.

Una pregunta natural: si cada «modificación» crea un texto nuevo, ¿no es carísimo armar un texto largo pegando pedazos? En un ciclo con miles de vueltas sí lo es, y para eso hay una clase aparte, `StringBuilder`. No la necesitas en este curso: el `revisor` arma líneas cortas, y `String.format` (2.2.3) ya hace el trabajo.

#### 2.2.2 `==` contra `equals`

Este es el error más importante de la lección. Mira `fig02_08.java`, que arma el texto `inventario` de dos maneras:

```java
// fig02_08.java
void main() {
  String prefijo = "inven";
  String armado = prefijo + "tario";
  String literal = "inventario";
  String constante = "inven" + "tario";

  IO.println("armado == literal:       " + (armado == literal));
  IO.println("armado.equals(literal):  " + armado.equals(literal));
  IO.println("constante == literal:    " + (constante == literal));
  IO.println("mayúsculas distintas:    " + "INVENTARIO".equals(literal));
  IO.println("ignorando mayúsculas:    " + "INVENTARIO".equalsIgnoreCase(literal));
}
```

```text
$ java fig02_08.java
armado == literal:       false
armado.equals(literal):  true
constante == literal:    true
mayúsculas distintas:    false
ignorando mayúsculas:    true
```

Los tres primeros renglones cuentan una historia que se lee mejor en orden.

`armado == literal` da **`false`**, aunque los dos textos contienen `inventario`. `armado` se construyó **en tiempo de ejecución**, pegando el valor de la variable `prefijo` con `"tario"`, así que es un objeto nuevo en una dirección nueva. `literal` es un objeto que el compilador creó una vez, y `==` pregunta si ambas variables apuntan al mismo. No lo hacen. `armado.equals(literal)` da `true`, porque `equals` compara el **contenido**, carácter por carácter.

La tercera línea es la trampa dentro de la trampa. `constante == literal` da **`true`**, aunque `constante` se escribió también con un `+`. La razón: `"inven" + "tario"` junta dos literales, y el compilador sabe el resultado antes de ejecutar, así que lo calcula él y lo trata como el literal `"inventario"` (la [especificación](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.29) lo llama «expresión constante»). Los textos literales con el mismo contenido son **el mismo objeto** en todo el programa. Por eso un `==` entre textos suele funcionar en una prueba chica de juguete y falla en cuanto el texto llega de **fuera**: de un archivo, de un teclado, de una red, de un `+` con una variable. Es el tipo de error que pasa todas las pruebas que escribes y falla en producción.

Los dos últimos renglones muestran lo que se escribe en su lugar. `equals` distingue mayúsculas, y `equalsIgnoreCase` no. Y una costumbre que vale oro: **pon el literal primero**. `"INVENTARIO".equals(literal)` no puede lanzar un `NullPointerException`, porque el literal nunca es nulo; en cambio, `variable.equals("inventario")` revienta si `variable` es `null`. Con el literal a la izquierda, si la variable es nula la respuesta es simplemente `false`.

**La regla de esta lección, la más importante:** entre referencias (textos, envoltorios, cualquier objeto), `==` pregunta si es el mismo objeto; `equals` pregunta si valen lo mismo. Casi siempre quieres lo segundo. Usa `==` entre primitivos, y entre `enum`s (Lección 3), donde sí es lo correcto.

¿Por qué Java no hace que `==` compare contenido en los textos, como Python? Porque Java no deja que el programador cambie el significado de un operador, y `==` significa lo mismo en todos los tipos de referencia. Es una decisión de diseño que lleva décadas dando guerra, y que no va a cambiar.

#### 2.2.3 Dar formato: `String.format` y `Locale.ROOT`

Cada renglón de la tabla del `revisor` tiene columnas alineadas: el nombre ocupa 12 espacios, el estado 10, y así. Armar eso con `+` y espacios a mano es una pesadilla, y para eso Java trae `String.format`: recibe una **plantilla** con marcas y los valores que las llenan. Las marcas que necesitas hoy:

- `%s` pone un texto (o cualquier valor, convertido a texto).
- `%d` pone un entero.
- `%.1f` pone un decimal con un decimal después del punto.
- `%-12s` pone un texto **ocupando 12 espacios, alineado a la izquierda** (sin el guion, a la derecha). `%6s` ocupa 6 y alinea a la derecha.

Y hay un problema que no verás hasta que tu programa corra en la computadora de otra persona: los decimales se escriben distinto según el idioma. En México el separador decimal es el punto (`2.3`), en España es la coma (`2,3`). Si le pides a `String.format` que formatee un decimal sin decirle el idioma, **usa el de la máquina donde corre**. Tu programa saldría con puntos en tu laptop y con comas en la de un colega. Para que no pase, se le pasa como primer argumento un `Locale`, el objeto que representa un idioma y una región. Mira `fig02_09.java`:

```java
// fig02_09.java
void main() {
  double segundos = 2.3;
  Locale mexico = Locale.of("es", "MX");
  Locale espana = Locale.of("es", "ES");

  IO.println(String.format(Locale.ROOT, "%.1fs", segundos));
  IO.println(String.format(mexico, "%.1fs", segundos));
  IO.println(String.format(espana, "%.1fs", segundos));
  IO.println(String.format(Locale.ROOT, "[%-12s][%6s][%d]", "catalogo", "142ms", 200));
  IO.println("%s tardó %d ms".formatted("pagos", 87));
}
```

```text
$ java fig02_09.java
2.3s
2.3s
2,3s
[catalogo    ][ 142ms][200]
pagos tardó 87 ms
```

`Locale.of("es", "MX")` construye el idioma español de México y `Locale.of("es", "ES")` el de España. Con el de España, el mismo `%.1f` da **`2,3s`**. `Locale.ROOT` es el «idioma neutro», que usa el formato más estable (punto decimal, sin separadores de miles), y **es el que se usa en el curso siempre que se formatea un número con decimales**. La razón no es nacionalista: es que la tabla del `revisor` debe salir idéntica en cualquier máquina, y un programa cuya salida depende de la configuración regional es un programa que una prueba no puede verificar.

La cuarta línea de salida es la plantilla de los renglones de la tabla en miniatura (`%-12s` para el nombre, `%6s` para el tiempo y `%d` para el código), con los corchetes para que veas cuánto espacio ocupa cada columna. La última usa `formatted`, una versión del mismo mecanismo que se llama sobre el propio texto: `"...".formatted(valores)` equivale a `String.format("...", valores)` (sin el `Locale`: úsala solo con textos y enteros).

#### 2.2.4 Bloques de texto

A veces necesitas un texto de varios renglones: una plantilla, un trozo de configuración, un JSON de ejemplo. Escribirlo con `"` y `\n` en cada renglón es horrible. Java tiene los **bloques de texto**, delimitados por tres comillas dobles. Se introdujeron como función definitiva en Java 15 ([JEP 378](https://openjdk.org/jeps/378)). Mira `fig02_10.java`:

```java
// fig02_10.java
void main() {
  String encabezado = """
      # nombre      url
      catalogo      https://catalogo.example
      pagos         https://pagos.example
      """;
  IO.print(encabezado);
  IO.println("renglones con salto final: " + encabezado.endsWith("\n"));

  String cita = """
      El servicio "%s" tardó %d ms.\
       Revísalo.""".formatted("inventario", 2300);
  IO.println(cita);
}
```

```text
$ java fig02_10.java
# nombre      url
catalogo      https://catalogo.example
pagos         https://pagos.example
renglones con salto final: true
El servicio "inventario" tardó 2300 ms. Revísalo.
```

La regla que lo hace útil es cómo trata la sangría. El texto del bloque empieza en el renglón de abajo de las tres comillas de apertura, y Java **quita la sangría que todos los renglones comparten** (el compilador la calcula con el renglón menos sangrado, contando también la línea del cierre `"""`). Por eso el texto impreso empieza en `# nombre` sin los seis espacios con que escribimos el bloque en el código. Si mueves el cierre `"""` hacia la izquierda, conservas más sangría; si lo dejas al final del último renglón de texto, no hay salto de línea al final.

Dos detalles más. Dentro de un bloque puedes escribir comillas dobles sin escaparlas (`"%s"` en el segundo bloque). Y una barra inversa `\` al final del renglón **no inserta el salto**: junta el renglón con el siguiente. En el segundo bloque, el `\` hace que la frase salga de un tirón, con un espacio que viene de la sangría extra del renglón siguiente (siete espacios, uno más que los seis comunes). Y fíjate en el uso de `.formatted(...)` justo después del cierre: un bloque de texto es un `String` como cualquier otro. La línea con `endsWith("\n")` comprueba que el primer bloque termina con un salto de línea, que en un texto normal se escribe `\n`; por eso la salida de `IO.print` queda con los tres renglones seguidos y el siguiente `IO.println` empieza en un renglón limpio.

Con esto termina la primera sesión. Antes de seguir, comprueba que puedes responder, sin mirar: ¿cuál es la diferencia entre `int` e `Integer`? ¿Qué imprime `armado == literal` en `fig02_08.java` y por qué? ¿Qué pasa si escribes `linea.strip();` y nada más? Si alguna de las tres te cuesta, relee la sección que corresponde antes de la segunda sesión.


### 2.3 Control de flujo

#### 2.3.1 `if`, condiciones y el operador `? :`

El `if` de Java es el que ya conoces, con dos particularidades. La primera: **la condición tiene que ser un `boolean`**, y nada más. En Python o en JavaScript, un número, un texto o una lista pueden hacer de condición (se les llama «verdaderos» o «falsos» según su contenido); en Java, `if (codigo)` con un `int` ni siquiera compila:

```text
Prueba.java:3: error: incompatible types: int cannot be converted to boolean
  if (codigo) {
      ^
```

Es una ventaja disfrazada de molestia: te obliga a escribir lo que de verdad quieres preguntar (`codigo >= 400`, `texto != null`, `!texto.isBlank()`), y se acabaron los errores de «el cero también cuenta como falso». La segunda particularidad es de estilo: **las llaves siempre**, aunque el `if` tenga una sola línea. Es lo que pide la guía de estilo de Google que sigue todo el código del curso (dos espacios de sangría, renglones de hasta 100 columnas), y la que verificará Checkstyle en la Lección 10; si las escribes desde hoy, no tendrás que corregirlas después. Un `if` sin llaves es la receta de un error clásico: alguien agrega un segundo renglón debajo, con la sangría correcta, y el segundo renglón no es parte del `if`.

Mira `fig02_11.java`. Junta un `if` con `else if`, una variable booleana, y el **operador ternario**, que es un `if` en forma de expresión: `condicion ? valorSiVerdadero : valorSiFalso`.

```java
// fig02_11.java
void main() {
  int codigo = 503;
  long tiempoMs = 95;

  if (codigo >= 400) {
    IO.println("CAIDO: el servicio contestó " + codigo);
  } else if (tiempoMs >= 1000) {
    IO.println("LENTO");
  } else {
    IO.println("ARRIBA");
  }

  boolean respondio = codigo != 0 && tiempoMs > 0;
  IO.println("¿respondió? " + respondio);
  String mensaje = respondio ? "con respuesta" : "sin respuesta";
  IO.println(mensaje);
}
```

```text
$ java fig02_11.java
CAIDO: el servicio contestó 503
¿respondió? true
con respuesta
```

La condición `codigo >= 400` es la primera regla del `revisor`: un servicio que contesta con un código de error (400 o más) está caído, aunque haya respondido. Fíjate en que el `else if` solo se evalúa si el primero fue falso, y que las ramas son excluyentes. El ternario se usa para elegir un valor, no para ejecutar acciones; cuando la expresión de cada lado se vuelve larga, un `if` normal se lee mejor.

#### 2.3.2 `&&` contra `&`

Aquí viene el segundo error de la lista de los lentos de arreglar. Java tiene dos operadores para «y»: `&&` y `&` (y dos para «o»: `||` y `|`). Los dos hacen lo mismo con booleanos, con una diferencia que importa: `&&` **se detiene en cuanto conoce la respuesta**. Si el lado izquierdo es falso, el resultado es falso sin importar el derecho, y el derecho **ni se evalúa**. Se le llama evaluación de cortocircuito. `&` evalúa **siempre los dos lados**. (Con números enteros, `&` es otra cosa: una operación sobre los bits. En este curso no la usamos, pero explica por qué existe.)

Mira el programa `fig02_12.java`. Quiere preguntar: «¿el detalle existe y además está en blanco?»:

```java
// fig02_12.java
void main() {
  String detalle = null;
  if (detalle != null & detalle.isBlank()) {
    IO.println("detalle vacío");
  }
  IO.println("fin");
}
```

```text
$ java fig02_12.java
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.isBlank()" because "<local1>" is null
	at fig02_12.main(fig02_12.java:4)
```

`detalle` vale `null`, así que `detalle != null` es falso... pero el `&` evaluó **de todos modos** el lado derecho, `detalle.isBlank()`, y llamar un método sobre `null` revienta. Es la comprobación que pusiste para evitar el problema, causando el problema. Con `&&` el lado derecho no se toca cuando el izquierdo falla. Es `fig02_13.java`:

```java
// fig02_13.java
void main() {
  String detalle = null;
  if (detalle != null && detalle.isBlank()) {
    IO.println("detalle vacío");
  }
  IO.println("fin sin errores");
}
```

```text
$ java fig02_13.java
fin sin errores
```

Este patrón, **comprobar que no sea nulo y, solo si no lo es, usarlo**, es la razón de ser de `&&` en el `revisor`. Lo usarás en `clasificar`, y lo vas a escribir cientos de veces en tu vida con Java. La regla es: en una condición, `&&` y `||`; el `&` y el `|` se dejan para operaciones sobre bits. Es una de esas faltas que el compilador no marca, porque ambos son válidos; los atrapa tu atención.

#### 2.3.3 `switch` como expresión

El `switch` clásico de Java, heredado de C, tiene un defecto célebre: **si olvidas el `break`, la ejecución sigue en el caso de abajo**. Mira `fig02_14.java`, que escribe el mismo selector de dos maneras:

```java
// fig02_14.java
void main() {
  String nombre = "inventario";
  int antiguo = 0;
  switch (nombre) {
    case "catalogo":
      antiguo = 1;
    case "inventario":
      antiguo = 2;
    case "pagos":
      antiguo = 3;
      break;
    default:
      antiguo = -1;
  }
  IO.println("switch con dos puntos y sin break: " + antiguo);

  int moderno = switch (nombre) {
    case "catalogo" -> 1;
    case "inventario" -> 2;
    case "pagos" -> 3;
    default -> -1;
  };
  IO.println("switch con flecha:                " + moderno);
}
```

```text
$ java fig02_14.java
switch con dos puntos y sin break: 3
switch con flecha:                2
```

Con la sintaxis vieja (los dos puntos), `inventario` entra al caso 2, **no encuentra `break`**, cae al caso 3 y termina dejando `antiguo` en 3. Con la nueva (la flecha `->`), cada caso es independiente: no hay `break` ni caída. Hay que reconocer la sintaxis vieja porque está en casi todo el código Java que encontrarás por ahí, pero **en este curso se escribe siempre con flecha**. Los `switch` con flecha entraron a Java en la versión 14 ([JEP 361](https://openjdk.org/jeps/361)).

La segunda diferencia es la que cambia cómo piensas: un `switch` con flechas es una **expresión**, o sea, **produce un valor**. Puedes asignarlo (`int moderno = switch ... ;`), devolverlo con `return switch ...` o pasarlo a un método. Tres reglas lo caracterizan. Un caso puede tener **varios valores** separados por comas (`case "catalogo", "inventario", "pagos" -> 200;`). El resultado de cada caso es lo que está a la derecha de la flecha. Y si necesitas más de una instrucción, el caso lleva un bloque entre llaves y entrega su valor con `yield`. Ve `fig02_15.java`:

```java
// fig02_15.java
void main() {
  IO.println(describir(200));
  IO.println(describir(301));
  IO.println(describir(404));
  IO.println(describir(503));
  IO.println(describir(0));
}

String describir(int codigo) {
  int familia = codigo / 100;
  return switch (familia) {
    case 2 -> "correcto";
    case 3 -> "redirección";
    case 4, 5 -> {
      String quien = familia == 4 ? "cliente" : "servidor";
      yield "error del " + quien;
    }
    default -> "sin clasificar";
  };
}
```

```text
$ java fig02_15.java
correcto
redirección
error del cliente
error del servidor
sin clasificar
```

Hay tres cosas nuevas en esa figura. `codigo / 100` aprovecha la división entera de 2.1.1 para obtener la «familia» del código (503 queda en 5). `case 4, 5 -> { ... yield ...; }` es el bloque con `yield`, que es a un caso lo que `return` a un método. Y el `default` atrapa lo demás. Una limitación: el `switch` con valores compara por **igualdad** con constantes; no sirve para rangos como «400 o más». Para eso se usa un `if`, o, desde la Lección 3, un `switch` con patrones y condiciones.

La regla que hace valioso al `switch` como expresión es que **tiene que cubrir todos los casos posibles**, porque tiene que producir un valor sea cual sea la entrada. Si falta algo, el compilador lo dice. `fig02_16.java` olvida el `default`:

```java
// fig02_16.java
void main() {
  String nombre = "reportes";
  int codigo = switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
  };
  IO.println(codigo);
}
```

```text
$ java fig02_16.java
fig02_16.java:4: error: the switch expression does not cover all possible input values
  int codigo = switch (nombre) {
               ^
1 error
error: compilation failed
```

El compilador sabe que hay infinitos textos distintos, así que sin `default` no puede garantizar que haya un valor. Más adelante, en la Lección 3, verás que con `enum` y con tipos sellados el compilador puede comprobar que **no falta ningún caso sin necesitar `default`**, y esa es una de las mejores razones para preferirlos.

Un `switch` sobre un texto compara con `equals` (por dentro), no con `==`, de modo que no cae en la trampa de 2.2.2. Y si el texto es `null`, el `switch` lanza un `NullPointerException` antes de mirar ningún caso.

#### 2.3.4 Métodos y constantes

Los métodos de Java se declaran escribiendo, en orden, el **tipo que devuelven**, el **nombre**, los **parámetros con su tipo** entre paréntesis y el cuerpo entre llaves. `String marcar(long tiempoMs)` es un método que recibe un `long` y devuelve un `String`. Si no devuelve nada, el tipo es `void` (como en `void main()`). Se devuelve con `return`, y el compilador exige que **todos los caminos** del método devuelvan algo: si un `if` sin `else` deja un camino sin `return`, es un error de compilación. Dentro de un archivo compacto los métodos se escriben a la misma altura que `main`, y `main` los llama por su nombre.

Dos particularidades respecto a otros lenguajes. Java **no tiene parámetros con valor por omisión**: si quieres una versión corta de un método, escribes otro con el mismo nombre y menos parámetros (la Lección 3 lo hará con los *records*). Y los argumentos se pasan **por valor**: el método recibe una copia. Mira `fig02_17.java`, que además estrena una constante:

```java
// fig02_17.java
static final int UMBRAL_LENTO_MS = 1000;

void main() {
  long tiempoMs = 2300;
  IO.println(marcar(tiempoMs));
  IO.println("después de la llamada, tiempoMs sigue valiendo " + tiempoMs);
  IO.println(marcar(87));
}

String marcar(long tiempoMs) {
  String marca = tiempoMs >= UMBRAL_LENTO_MS ? "lento" : "rápido";
  tiempoMs = 0;
  return marca;
}
```

```text
$ java fig02_17.java
lento
después de la llamada, tiempoMs sigue valiendo 2300
rápido
```

El método `marcar` pone su parámetro `tiempoMs` en 0, y al volver, la variable del `main` sigue valiendo 2300. Cambiar el parámetro cambió la copia, no la original. (Con objetos pasa lo mismo, pero lo que se copia es la **dirección**: el método puede cambiar el contenido de un objeto mutable, pero no puede hacer que la variable de quien lo llamó apunte a otro.)

La línea del principio, `static final int UMBRAL_LENTO_MS = 1000;`, es una **constante**. Cada palabra cuenta. `final` quiere decir «se asigna una sola vez»; un intento de reasignarla es un error de compilación. `static` quiere decir «pertenece a la clase y no a un objeto»; en un archivo compacto, la «clase» es el archivo entero. El nombre va en `MAYUSCULAS_CON_GUION_BAJO` (así lo pide la convención de Java), y lleva la unidad: `_MS`. Escribirlo en un solo lugar y usarlo por nombre, en lugar de escribir `1000` en cada comparación, tiene un motivo práctico: cuando el umbral cambie, cambia en un lugar; y uno de comunicación: `tiempoMs >= UMBRAL_LENTO_MS` se lee, y `tiempoMs >= 1000` obliga a recordar qué es mil.

### 2.4 `null` y los mensajes de `NullPointerException`

#### 2.4.1 Qué es `null` y cuándo revienta

Dijimos en 2.1.2 que una referencia puede no apuntar a nada, y que eso se escribe `null`. El problema no es que exista, sino lo que pasa cuando **usas** un `null` como si fuera un objeto: llamarle un método, leer uno de sus campos, o, en el caso de un envoltorio, sacarle el valor. En cualquiera de esos casos el programa se detiene con una excepción llamada `NullPointerException` (se le dice «NPE»). Tony Hoare, que inventó el concepto en 1965, lo llamó después [«mi error de mil millones de dólares»](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/). Java no lo quitó, y la mayor parte de este curso es la historia de cómo vivir sin depender de él.

Mira `fig02_18.java`. El método `codigoDe` devuelve `null` para un servicio desconocido, y el `main` intenta sumarle 1:

```java
// fig02_18.java
void main() {
  Integer codigo = codigoDe("reportes");
  IO.println("código: " + codigo);
  int sumado = codigo + 1;
  IO.println("sumado: " + sumado);
}

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}
```

```text
$ java fig02_18.java
código: null
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "<local1>" is null
	at fig02_18.main(fig02_18.java:5)
```

La primera línea sale bien, porque pegar un `null` a un texto con `+` solo escribe «null». La segunda es la que revienta: `codigo + 1` necesita el número que hay dentro del `Integer`, o sea, hacer el *unboxing* de 2.1.2, y no hay número. Es la trampa de los envoltorios: **la suma parece de dos enteros, pero uno de ellos puede no existir**. Si `codigo` fuera `int`, no habría nada que fallara, pero tampoco habría forma de decir «sin respuesta».

#### 2.4.2 Leer el mensaje

Desde hace varias versiones de Java, un `NullPointerException` no se limita a decir «es nulo»: dice **qué intentabas hacer** y **qué era nulo**. Es la función de los «mensajes útiles de NPE», descrita en el [JEP 358](https://openjdk.org/jeps/358). El de `fig02_18.java` dice:

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "<local1>" is null
	at fig02_18.main(fig02_18.java:5)
```

Se lee en dos mitades. **Antes de `because`: qué intentaba el programa.** «No se puede invocar `Integer.intValue()`», o sea, sacar el valor del envoltorio. **Después de `because`: qué era nulo.** «La variable `<local1>`». Y la línea de abajo, `at fig02_18.main(fig02_18.java:5)`, dice dónde: el método `main`, archivo `fig02_18.java`, renglón 5. Un NPE se arregla leyendo ese mensaje, no abriendo un depurador.

Ahora, el detalle que el temario promete aclarar. ¿Por qué dice `<local1>` y no `codigo`? Porque **el lanzador `java Archivo.java` compila el archivo en memoria sin guardar los nombres de las variables locales**. Esos nombres solo existen en el código fuente; para que la JVM los conozca, el compilador tiene que incluirlos, y se lo pides con la opción `-g`. Compílalo tú mismo, con `javac -g`, y ejecútalo con `java`:

```text
$ javac -g fig02_18.java
$ java fig02_18
código: null
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "codigo" is null
	at fig02_18.main(fig02_18.java:5)
```

Ahora sí: `because "codigo" is null`. Es el mismo programa, el mismo error y la misma línea, con un mensaje mucho más claro. Cuando te topes con un `<local3>` en el trabajo, ya sabes qué es: el número de la ranura que la JVM le dio a esa variable local (en un método de instancia la ranura 0 es `this`, y las demás se reparten en el orden en que se declaran; un `long` o un `double` ocupa dos). Y cuando quieras el nombre, compila con `-g`.

Hay más formas de mensaje. Si lo nulo no es una variable sino el **resultado de un método**, el mensaje lo dice. Es `fig02_19.java`:

```java
// fig02_19.java
void main() {
  String texto = codigoDe("reportes").toString();
  IO.println(texto);
}

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}
```

```text
$ java fig02_19.java
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.toString()" because the return value of "fig02_19.codigoDe(String)" is null
	at fig02_19.main(fig02_19.java:3)
```

«Because the return value of `fig02_19.codigoDe(String)` is null» se traduce: el método `codigoDe` devolvió nulo, y el programa le pidió `toString()` a ese valor. No hace falta adivinar cuál de las partes de la expresión `codigoDe("reportes").toString()` era nula: el mensaje señala la que fue. Cuando una cadena tiene varias llamadas encadenadas (`a.b().c().d()`), eso es lo que te ahorra media hora.

#### 2.4.3 Tres reglas para vivir con `null`

El `null` no desaparece del código real, pero hay maneras de domesticarlo, y el `revisor` las aplica desde hoy:

1. **Comprueba antes de usar, con `&&`.** `codigo != null && tiempoMs != null` es la condición del `revisor` para saber si un servicio respondió. Si falla la primera parte, la segunda no se evalúa.
2. **Pon el literal a la izquierda en `equals`.** `"pagos".equals(nombre)` no puede reventar; `nombre.equals("pagos")` sí.
3. **No devuelvas `null` cuando hay una forma mejor de decir «no hay dato».** Aquí lo hacemos porque todavía no tenemos otra, pero en la Lección 3 `null` desaparece del `revisor` y lo reemplaza un tipo sellado, y en la 4 verás `Optional`. Un método que puede devolver nulo es un método que obliga a todos los que lo llaman a acordarse de comprobarlo.

### 2.5 El paso del proyecto: el `revisor` con sus funciones base

Ahora juntas todo. Quieres que `Revisor.java` imprima la tabla de la que partió la lección, calculando cada celda con funciones. Las reglas son las cuatro de siempre del `revisor`:

1. Si el servicio no respondió, está **CAIDO**.
2. Si respondió con un código de 400 o más, está **CAIDO** (el detalle es el código).
3. Si respondió y tardó 1000 ms o más, está **LENTO**.
4. Si no, está **ARRIBA**.

Los datos de los cuatro servicios viven, por ahora, en dos `switch` paralelos: uno devuelve el código y otro el tiempo. Un servicio que no respondió tiene `null` en los dos. Tu `Revisor.java` queda así (guárdalo en `~/curso-java/revisor/`; la figura lo llama `fig02_20.java` para distinguirlo de las otras, y su contenido es el de tu archivo):

```java
// fig02_20.java
static final int UMBRAL_LENTO_MS = 1000;

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}

Long tiempoDe(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142L;
    case "inventario" -> 2300L;
    case "pagos" -> 87L;
    default -> null;
  };
}

String clasificar(Integer codigo, Long tiempoMs) {
  boolean respondio = codigo != null && tiempoMs != null;
  if (!respondio) {
    return "CAIDO";
  }
  if (codigo >= 400) {
    return "CAIDO";
  }
  if (tiempoMs >= UMBRAL_LENTO_MS) {
    return "LENTO";
  }
  return "ARRIBA";
}

String formatearTiempo(Long tiempoMs) {
  if (tiempoMs == null) {
    return "—";
  }
  if (tiempoMs < UMBRAL_LENTO_MS) {
    return tiempoMs + "ms";
  }
  return String.format(Locale.ROOT, "%.1fs", tiempoMs / 1000.0);
}

String detalle(Integer codigo) {
  if (codigo == null) {
    return "sin conexión";
  }
  return String.valueOf(codigo);
}

String linea(String nombre, String estado, String tiempo, String detalle) {
  return String.format(Locale.ROOT, "%-12s%-10s%6s  %s", nombre, estado, tiempo, detalle);
}

String encabezado() {
  return linea("SERVICIO", "ESTADO", "TIEMPO", "DETALLE");
}

String revisar(String nombre) {
  Integer codigo = codigoDe(nombre);
  Long tiempoMs = tiempoDe(nombre);
  return linea(
      nombre, clasificar(codigo, tiempoMs), formatearTiempo(tiempoMs), detalle(codigo));
}

void main() {
  IO.println(encabezado());
  IO.println(revisar("catalogo"));
  IO.println(revisar("inventario"));
  IO.println(revisar("pagos"));
  IO.println(revisar("reportes"));
}
```

```text
$ java fig02_20.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Recorre el programa con lo que sabes, de arriba abajo:

- **`UMBRAL_LENTO_MS`** es la constante de 2.3.4.
- **`codigoDe` y `tiempoDe`** son los dos `switch` con flecha de 2.3.3. Devuelven `Integer` y `Long`, no `int` y `long`, para poder devolver `null` (2.1.2). Los literales llevan la `L` en `tiempoDe` porque el resultado es un `Long`, y un `142` solo sería un `Integer`.
- **`clasificar`** implementa las cuatro reglas. La primera línea, `codigo != null && tiempoMs != null`, usa el `&&` de 2.3.2: de otro modo, las comparaciones de abajo desenvolverían un nulo. Las reglas se aplican por orden, cada una con su `return`; cuando una se cumple, las demás no se evalúan.
- **`formatearTiempo`** recibe un `Long`, que puede ser nulo (la raya «—» es la señal de «sin dato»), y de lo contrario decide entre `142ms` y `2.3s`. Fíjate que usa `Locale.ROOT` (2.2.3).
- **`detalle`** devuelve «sin conexión» para el nulo y, si no, el código como texto. `String.valueOf(codigo)` convierte un `Integer` en `String`.
- **`linea`** arma un renglón con la plantilla `%-12s%-10s%6s  %s`: 12 de ancho para el nombre, 10 para el estado, 6 alineado a la derecha para el tiempo, dos espacios y el detalle. `encabezado` reutiliza `linea` con los títulos, para que sean de las mismas medidas.
- **`revisar`** une todo: busca el código y el tiempo de un servicio, y entrega su renglón completo. La llamada a `linea` se parte en dos renglones porque pasaría de 100 columnas; la continuación va con cuatro espacios más.
- **`main`** imprime el encabezado y los cuatro renglones, uno por uno. No hay bucle porque no hay lista: los servicios no son todavía datos que se puedan recorrer.

Y la tabla que sale es **carácter por carácter** la que prometimos al principio. Observa que `reportes` pasó por todo el camino del `null`: `codigoDe` y `tiempoDe` le devuelven nulo, `clasificar` lo manda a `CAIDO` por la regla 1, `formatearTiempo` le pone la raya y `detalle` escribe «sin conexión». Cada pieza decidió sola lo que hacer con el vacío. Pero esa decisión se repitió **cuatro veces** (una por función), y de eso trata la siguiente lección.

Para correrlo desde tu carpeta de trabajo:

```text
$ cd ~/curso-java/revisor
$ java Revisor.java
```

## El error que vas a ver

La lección tiene un error principal y cuatro mensajes de apoyo que conviene saber leer de una mirada.

**El error principal: un `==` que da `false` cuando esperabas `true`.** Es el de `fig02_08.java`. Cuando un texto armado en tiempo de ejecución se compara con `==` contra un literal, Java pregunta si son el mismo objeto, y no lo son:

```text
armado == literal:       false
armado.equals(literal):  true
```

No hay mensaje de error; por eso es el que más cuesta. La señal es que **una comparación que debería ser verdadera da falsa** y todo lo demás parece estar bien. Si ves un `==` entre dos textos o dos envoltorios en un `if`, sospecha de él antes que de nada.

**Los cuatro mensajes de apoyo:**

- `error: incompatible types: possible lossy conversion from long to int`: asignaste un tipo grande a uno más chico. Cambia el tipo de destino, o haz la conversión explícita a sabiendas de lo que puedes perder (`fig02_04.java`).
- `error: cannot infer type for local variable respuesta`, con `(variable initializer is 'null')`: usaste `var` sin un valor del que deducir el tipo (`fig02_06.java`).
- `error: the switch expression does not cover all possible input values`: un `switch` que produce un valor y no cubre todos los casos. Agrega el `default` (o, con `enum` y tipos sellados, el caso que falta) (`fig02_16.java`).
- `NullPointerException: Cannot invoke "String.isBlank()" because "<local1>" is null`: usaste un nulo como si fuera un objeto. La parte posterior a `because` dice cuál era nulo; si dice `<localN>` en lugar del nombre, el archivo se compiló sin `-g` (`fig02_12.java`, `fig02_18.java`).

Y un efecto de fondo que no produce mensaje alguno: `2,3s` en lugar de `2.3s`. Pasa en una máquina con configuración regional de España cuando se formatea un decimal sin `Locale.ROOT` (`fig02_09.java`).

## Lo que se hace mal

**Comparar textos o envoltorios con `==`.** Funciona con los literales y con los números chicos, y falla con los datos que llegan de fuera. Con textos, `equals` (con el literal a la izquierda); con envoltorios, `equals` o, si son primitivos, `==`.

**Llamar a un método de `String` y no guardar el resultado.** `linea.strip();` suelto no hace nada. Los textos no cambian: los métodos devuelven otro.

**Usar `&` o `|` donde van `&&` o `||`.** Los dos son válidos, y el primero revienta justo cuando la comprobación servía.

**Un `int` para algo que puede crecer.** Los milisegundos de un mes no caben en un `int`. Si el dato es un tiempo, una cuenta o un tamaño, empieza con `long`.

**Un `double` para dinero o para comparar con `==`.** Los decimales binarios no son exactos (`0.1 + 0.2`). Los tiempos del `revisor` son enteros en milisegundos justamente para esquivarlo.

**Un molde `(int)` para callar al compilador.** Silencia la advertencia y deja el desbordamiento sin aviso. Antes de escribirlo, pregúntate si el tipo de destino debería ser más grande.

**`String.format` con decimales sin `Locale`.** Funciona en tu máquina y falla en la de otro. `Locale.ROOT` siempre.

**Un `switch` con la sintaxis de dos puntos.** Un `break` olvidado produce errores que no aparecen en el compilador. Con la flecha desaparece la clase entera de errores.

**Devolver `null` por costumbre.** Cada `null` que sale de un método obliga a todos los que lo llaman a acordarse de comprobarlo. Aquí lo usamos porque todavía no tenemos otra herramienta, y la Lección 3 la trae.

**`var` donde el tipo no se ve.** `var r = revisar(s);` obliga al lector a ir a buscar qué es `r`. Úsalo cuando el tipo salta a la vista.

## Ejercicios

### Ejercicio 1 — Predice, luego corre

Escribe en un archivo el `main` que imprime estas seis expresiones, **una por línea**, y antes de ejecutarlo anota en un papel qué crees que imprimirá cada una:

1. `"142" + 1`
2. `1 + 2 + "3"`
3. `"1" + 2 + 3`
4. `-7 / 2`
5. `0.1 + 0.2 == 0.3`
6. `'a' + 1`

Ejecútalo y compara. En cada caso donde te equivocaste, escribe una frase en tu [bitácora](bitacora.md) con la regla que no tenías presente.

### Ejercicio 2 — El valor de retorno ignorado

Escribe un método `String normalizar(String nombre)` que quite los espacios de los extremos y pase el texto a minúsculas, y pruébalo con `"  PAGOS "`. Escríbelo **primero de la manera equivocada** (llamando `strip()` y `toLowerCase()` sin guardar el resultado) y mira qué devuelve. Después arréglalo. ¿Se puede arreglar en una sola línea?

### Ejercicio 3 — Dos maneras de no reventar

Escribe dos métodos: `boolean vacio(String detalle)`, que devuelva `true` si el detalle es nulo **o** está en blanco, y `boolean contieneError(String detalle)`, que devuelva `true` si el detalle no es nulo **y** contiene la palabra `error`. Pruébalos con `null`, con `"   "`, con `"200"` y con `"error de red"`. Piensa antes de escribir: ¿cuál de los dos usa `&&` y cuál `||`? ¿Qué pasaría con el operador equivocado?

### Ejercicio 4 — El proyecto: un dato en un solo sitio

En el `Revisor.java` de la sección 2.5, agrega `facturacion`, que respondió con el código **503** en **95 ms**. Hazlo **en dos pasos**. Primero, agrégalo solo en `codigoDe` (y a `main`, para que se imprima su renglón), y corre. Predice antes qué renglón saldrá. Después agrégalo también en `tiempoDe` y vuelve a correr. Anota en la bitácora qué problema tiene mantener la misma información en dos sitios.

## Soluciones

### Solución 1

```text
1421
33
123
-3
false
98
```

1. `"142" + 1` es un texto pegado con un número: el `+` convierte el 1 en texto y los junta, `"1421"`. No es una suma.
2. `1 + 2 + "3"` se evalúa de izquierda a derecha: primero `1 + 2` (dos números) da 3, y luego `3 + "3"` pega, `"33"`.
3. `"1" + 2 + 3` empieza con un texto, así que todo lo que sigue se pega: `"123"`.
4. `-7 / 2` es una división entera, que descarta la parte decimal en dirección al cero: `-3`, no `-4`.
5. `0.1 + 0.2 == 0.3` es `false`, porque la suma es `0.30000000000000004`, como vimos en 2.1.1.
6. `'a' + 1` suma un `char` y un `int`; el `char` se convierte en su número (97), y da `98`. Para obtener el texto «a1» habría que partir de un texto: `"" + 'a' + 1`.

### Solución 2

La versión equivocada, que no cambia nada:

```text
[  PAGOS ]
false
```

La versión correcta se escribe en una sola línea, **encadenando** las llamadas, porque cada método devuelve un `String` sobre el que se puede llamar el siguiente:

```java
String normalizar(String nombre) {
  return nombre.strip().toLowerCase();
}
```

Con ella, la salida es:

```text
[pagos]
true
```

### Solución 3

`vacio` usa `||`: basta que **una** de las dos cosas se cumpla. `contieneError` usa `&&`: tienen que cumplirse **las dos**, y la primera protege a la segunda.

```java
boolean vacio(String detalle) {
  return detalle == null || detalle.isBlank();
}

boolean contieneError(String detalle) {
  return detalle != null && detalle.contains("error");
}
```

Las pruebas dan `true`, `true`, `false` para `vacio` con `null`, `"   "` y `"200"`, y `false`, `true` para `contieneError` con `null` y con `"error de red"`. Si escribes `|` en lugar de `||` en `vacio`, con `null` el lado derecho (`detalle.isBlank()`) se evalúa y revienta; con `&` en lugar de `&&` en `contieneError`, lo mismo. Y una tercera salida, sin condición: `"sin conexión".equals(detalle)` da `false` con un `detalle` nulo, porque el literal de la izquierda nunca es nulo.

### Solución 4

Con `facturacion` solo en `codigoDe`, el renglón que aparece es este:

```text
facturacion CAIDO          —  503
```

Sale CAIDO por una razón equivocada, que es el peligro de este diseño. `codigoDe` dice 503, pero `tiempoDe` dice `null`, así que `clasificar` aplica la regla 1 («no respondió») y no la 2 («respondió con un código de error»), y el tiempo sale como una raya aunque el servicio sí contestó en 95 ms. El detalle muestra el 503, que contradice a la raya. No hay ningún error del compilador ni de la JVM. Con el dato también en `tiempoDe`, el renglón correcto es:

```text
facturacion CAIDO       95ms  503
```

La lección que va a la bitácora: **un dato que vive en dos sitios acaba diciendo dos cosas**. Es la razón por la que la siguiente lección reúne código y tiempo en un solo valor, y la razón de que el compilador, y no tu memoria, sea quien te obligue a completarlo.

## Cómo sé que lo logré

- [ ] `java fig02_08.java` imprime `false` y `true` en las dos primeras líneas, y sabes explicar por qué sin mirar la lección.
- [ ] `java fig02_07.java` muestra `[  catalogo  ]` en la primera línea, y sabes qué cambiarías para que muestre `[catalogo]`.
- [ ] Si quitas la línea `default -> null;` de `codigoDe`, el lanzador responde con `the switch expression does not cover all possible input values`.
- [ ] En `fig02_12.java` ves el `NullPointerException` con `"<local1>"`, y con `javac -g` ves el nombre `"detalle"`.
- [ ] Desde `~/curso-java/revisor/`, `java Revisor.java` imprime el encabezado y los cuatro renglones de la tabla, con `reportes` en `CAIDO`, `—` y `sin conexión`.
- [ ] Puedes explicar por qué `2300 == 2300` da `false` con dos `Integer` y `true` con dos `int`.
- [ ] Los ejercicios 1 y 3 los predijiste antes de ejecutar, y anotaste en tu bitácora cuál de las seis expresiones del ejercicio 1 no acertaste.
- [ ] Anotaste en tu bitácora cuál de los mensajes de esta lección te costó más leer.

## Resumen

Responde sin mirar la lección:

1. ¿Qué diferencia hay entre `int` e `Integer`, y cuándo necesitas el segundo?
2. ¿Qué pasa cuando un `int` supera su valor máximo? ¿Cómo lo evitas en una cuenta de milisegundos?
3. ¿Qué tipo tiene una variable declarada con `var`, y cuándo se decide?
4. ¿Por qué `linea.strip();` no cambia `linea`? ¿Qué escribes en su lugar?
5. ¿Qué compara `==` entre dos referencias? ¿Por qué `armado == literal` da `false` y `constante == literal` da `true`?
6. ¿Por qué se pasa `Locale.ROOT` a `String.format` cuando hay decimales?
7. ¿Qué diferencia hay entre `&&` y `&` cuando el lado izquierdo es falso?
8. ¿Qué dos cosas distinguen a un `switch` con flechas del clásico? ¿Qué pasa si no cubre todos los casos?
9. ¿Qué significan `static` y `final` en una constante?
10. ¿Cómo se lee un `NullPointerException`: qué dice antes de `because` y qué después? ¿Por qué a veces aparece `<local1>`?

## Para leer más

- [Los tutoriales de Java, «Primitive Data Types»](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html): los ocho tipos primitivos con sus rangos y valores por omisión, en la documentación oficial (en inglés, como casi toda). Consultado el 7 de octubre de 2026.
- [Javadoc de `String`, Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html): todos los métodos de la clase, con la regla de que los textos son constantes. Consultado el 7 de octubre de 2026.
- [JEP 361, «Switch Expressions»](https://openjdk.org/jeps/361) y [JEP 378, «Text Blocks»](https://openjdk.org/jeps/378): el porqué y las reglas exactas de dos de las funciones que se usan en esta lección. Consultado el 7 de octubre de 2026.
- [JEP 358, «Helpful NullPointerExceptions»](https://openjdk.org/jeps/358): qué dice cada parte del mensaje y por qué faltan los nombres cuando se compila sin `-g`. Consultado el 7 de octubre de 2026.

---

**Anterior:** [Lección 1 — Instalar Java 25 en tu Linux Mint](01-instalacion.md) ·
**Siguiente:** [Lección 3 — Records, enums y tipos sellados](03-records-enums-sellados.md)
