# Lección 3 — Records, enums y tipos sellados

**Tiempo:** 2 × 90 min (dos sesiones; el corte está entre las secciones 3.2 y 3.3)

**Qué construyes:** el modelo de datos del `revisor`: un `Servicio` con su nombre, su dirección y su tiempo límite, un `Estado` que solo puede valer tres cosas, y una `Respuesta` que es o «el servicio respondió» o «no respondió», sin ningún `null` de por medio. El `revisor` pasa de un archivo a siete

**Qué aprendes:** `record` (componentes, accesores, `equals`, `hashCode` y `toString` gratis, constructor compacto y constructor adicional); por qué la inmutabilidad de un `record` es superficial; `enum`; interfaces `sealed` con `permits`; `switch` con patrones, exhaustivo y sin `default`; guardas con `when`; patrones de record y el patrón sin nombre `_`; y cómo correr un proyecto de varios archivos con `java Revisor.java`

**De dónde vienes.** Traes el `Revisor.java` de la [Lección 2](02-tipos-cadenas-control.md): un solo archivo compacto con `codigoDe`, `tiempoDe`, `clasificar`, `formatearTiempo`, `detalle`, `linea`, `encabezado`, `revisar` y un `main` que imprime la tabla de cuatro servicios. Funciona, y tiene una debilidad que el ejercicio de proyecto de la lección te dejó a la vista: la información de cada servicio vive **repartida** en dos `switch` paralelos, y un servicio que no respondió es un `null` en cada uno. Si olvidas agregar un dato en uno de los dos, el programa compila, corre y muestra un renglón equivocado, sin ninguna queja. Esta lección arregla eso con tres herramientas del lenguaje: tipos que **agrupan** datos (`record`), tipos que **enumeran** posibilidades (`enum`), y tipos que dicen «es una de estas formas y ninguna otra» (`sealed`).

**Qué no hace esta lección.** No usa listas todavía: los cuatro servicios siguen escritos a mano en el `main`, uno por uno, y su revisión sigue simulada con un `switch`. Las colecciones llegan en la [Lección 4](04-colecciones-genericos-streams.md) y los errores de verdad, con `try` y `catch`, en la 5. Aquí aparece `throw` una vez, en un ejemplo, y se explica en una caja; su manejo completo es de la Lección 5. Tampoco hay `public` ni `private` en el código del proyecto: la visibilidad se explica en la Lección 6, junto con los paquetes.

**Sobre las corridas.** Las figuras sueltas (`fig03_01.java` a `fig03_15.java`) se ejecutan con `java fig03_NN.java`. El proyecto de varios archivos, la figura `fig03_16`, es una **carpeta**: entras en ella y corres `java Revisor.java`. Las tienes en [`programas/03-records-enums-sellados/`](../programas/03-records-enums-sellados/). Las salidas se obtuvieron con Temurin 25.0.4 y `LANG=C.UTF-8`. Las figuras que fallan a propósito traen su error en un archivo `fig03_NN.error-esperado.txt`.

## Al terminar vas a poder

- Declarar un `record` y explicar qué le da el compilador gratis (accesores, `equals`, `hashCode`, `toString`).
- Escribir un constructor compacto que normalice un dato, y un constructor adicional que complete valores por omisión.
- Explicar por qué un `record` con un arreglo adentro no es de verdad inmutable.
- Declarar un `enum`, recorrer sus valores y escribir un `switch` sobre él que el compilador revisa.
- Declarar una interfaz `sealed` con sus formas permitidas y escribir un `switch` con patrones que cubra todas sin `default`.
- Usar una guarda `when`, un patrón de `record` y el patrón sin nombre `_`, y decir en qué orden van los casos.
- Mostrar qué le pasa al compilador cuando agregas un caso nuevo a un tipo sellado, y listar los lugares que te obliga a cambiar.

## El porqué antes del cómo

Hasta ahora el `revisor` tiene un problema de **representación**. Para decir «catalogo respondió 200 en 142 ms» usa dos funciones que no saben una de la otra; para decir «reportes no respondió» usa dos `null`. Un lector del código no puede ver la forma de los datos: tiene que deducirla de cómo se usan. Y el compilador tampoco la conoce, así que no te puede avisar cuando alguien se equivoca: puedes pedir el código de un servicio que sí respondió y el tiempo de uno que no, y Java lo permite.

La idea de esta lección es una frase: **haz que el compilador conozca la forma de tus datos, y los estados imposibles dejan de poderse escribir**. Dos ejemplos concretos del `revisor`:

- Un servicio que no respondió **no tiene** código HTTP ni tiempo. Con un `null` en cada uno puedes acabar con un código sin tiempo, o al revés. Si en cambio «respondió» y «no respondió» son dos tipos distintos, y solo el primero tiene código y tiempo, el segundo no puede tenerlos aunque quieras.
- Un estado es `ARRIBA`, `LENTO` o `CAIDO`. Con un `String` puede ser `"ARIBA"` (con una errata) y el programa lo acepta. Con un `enum` no.

Si vienes de otros lenguajes, esto te sonará: en Rust es un `enum` con datos; en TypeScript, una unión discriminada; en Python moderno, un `dataclass` con `match`; en Go no hay un equivalente directo, y se simula con interfaces. Java llegó tarde a esta fiesta —los *records* son de Java 16, los tipos sellados de Java 17 y los patrones en el `switch` de Java 21—, pero llegó completo y con la revisión del compilador incluida.

El camino tiene dos sesiones. La **primera** cubre 3.1 y 3.2: los *records* y los *enums*. Al terminarla podrás escribir tipos de datos en una línea y sabrás qué es y qué no es inmutable. La **segunda** cubre 3.3 a 3.5: los tipos sellados, el `switch` con patrones y el paso del proyecto. Al terminarla, el `revisor` seguirá imprimiendo **exactamente la misma tabla** que al terminar la lección 2, pero construida con tipos y no con `null`. Esa es la prueba: el comportamiento no cambia y el código es otro.

## Los conceptos

### 3.1 Records

#### 3.1.1 El problema: una clase solo para llevar datos

Supón que quieres guardar juntos el nombre de un servicio y su tiempo. En Java, el camino de siempre es escribir una clase. Mira cómo es ese camino, solo para ver el **tamaño** (no hace falta que entiendas cada renglón):

```java
final class PuntoClasico {
  final String nombre;
  final long tiempoMs;

  PuntoClasico(String nombre, long tiempoMs) {
    this.nombre = nombre;
    this.tiempoMs = tiempoMs;
  }

  String nombre() {
    return nombre;
  }

  long tiempoMs() {
    return tiempoMs;
  }

  @Override
  public boolean equals(Object otro) {
    if (this == otro) {
      return true;
    }
    if (otro == null || getClass() != otro.getClass()) {
      return false;
    }
    PuntoClasico p = (PuntoClasico) otro;
    return tiempoMs == p.tiempoMs && Objects.equals(nombre, p.nombre);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nombre, tiempoMs);
  }

  @Override
  public String toString() {
    return "PuntoClasico[nombre=" + nombre + ", tiempoMs=" + tiempoMs + "]";
  }
}
```

Son treinta y nueve renglones para decir «un nombre y un tiempo». Y lo que hace cada uno es mecánico: guardar los dos datos, devolverlos, compararlos y mostrarlos. Es el tipo de código que se escribe igual cada vez, y que cada vez puede tener un error: olvidar un campo en `equals`, o en `hashCode`, o en `toString`. (Ya aprendiste por qué el `equals` importa: `==` entre objetos pregunta si son el mismo, y no si valen lo mismo.)

Java 16 le puso nombre a este patrón: el ***record***. Su [especificación](https://openjdk.org/jeps/395) lo describe como un portador transparente de datos inmutables, y la clase de arriba, completa, se escribe así:

```java
record Punto(String nombre, long tiempoMs) {}
```

Una línea. Veámoslo en acción.

#### 3.1.2 Qué te da un `record`

Entre los paréntesis van los **componentes**: el tipo y el nombre de cada dato. El compilador usa esa lista para generar todo lo demás. Mira `fig03_01.java`, que usa el `record` `Punto` y lo prueba:

```java
// fig03_01.java
record Punto(String nombre, long tiempoMs) {}

void main() {
  Punto a = new Punto("catalogo", 142);
  Punto b = new Punto("catalogo", 142);
  Punto c = new Punto("pagos", 87);

  IO.println(a);
  IO.println("nombre:     " + a.nombre());
  IO.println("tiempoMs:   " + a.tiempoMs());
  IO.println("a.equals(b): " + a.equals(b));
  IO.println("a == b:      " + (a == b));
  IO.println("a.equals(c): " + a.equals(c));
  IO.println("mismo hash:  " + (a.hashCode() == b.hashCode()));
}
```

```text
$ java fig03_01.java
Punto[nombre=catalogo, tiempoMs=142]
nombre:     catalogo
tiempoMs:   142
a.equals(b): true
a == b:      false
a.equals(c): false
mismo hash:  true
```

Cada renglón de salida muestra algo que el compilador escribió por ti:

- **Un constructor**, con un parámetro por componente, en el mismo orden. `new Punto("catalogo", 142)` crea uno. (El 142 es un `int`, y entra en el componente `long` por la conversión sin pérdida de la lección anterior.)
- **Un accesor por componente**, con el mismo nombre del componente: `a.nombre()` y `a.tiempoMs()`. Fíjate en que **no se llaman `getNombre()`**: es una de las pocas convenciones de Java donde el *record* se aparta de la tradición.
- **`toString`**, con el formato `Punto[nombre=catalogo, tiempoMs=142]`. Es lo que sale al imprimir el objeto.
- **`equals`**, que compara **componente por componente**: `a.equals(b)` es `true` porque los dos valen lo mismo, aunque sean dos objetos distintos (`a == b` es `false`, como esperabas después de la Lección 2). Para un `record`, `equals` hace lo que querías que hiciera `==`.
- **`hashCode`**, coherente con `equals`: si dos objetos son iguales, su `hashCode` (un número que resume el contenido) coincide. No lo usas todavía; la Lección 4 lo necesita para los conjuntos y los diccionarios.

Y hay más reglas, que se aprenden con el compilador. Los componentes son **finales**: una vez creado el *record*, ninguno cambia. Mira `fig03_02.java`, que intenta cambiar uno:

```java
// fig03_02.java
record Punto(String nombre, long tiempoMs) {}

void main() {
  Punto punto = new Punto("catalogo", 142);
  punto.tiempoMs = 5;
}
```

```text
$ java fig03_02.java
fig03_02.java:6: error: cannot assign a value to final variable tiempoMs
  punto.tiempoMs = 5;
       ^
1 error
error: compilation failed
```

El compilador lo rechaza: «no se puede asignar un valor a la variable final `tiempoMs`». Un *record* es, en su forma básica, un valor que se crea completo y no se toca más. Tampoco se puede agregar un campo suelto (`int extra;` dentro del *record* da el error «field declaration must be static», con la sugerencia de convertirlo en un componente), y un *record* no puede extender otra clase. Lo que sí puede: tener **métodos**, implementar **interfaces** (lo usarás en 3.3) y tener **constantes `static`**.

Esa restricción es el punto. Un `record` dice con su forma: «soy un conjunto de datos fijos, y mi identidad es mi contenido». Quien lo lee sabe qué esperar sin leer una sola línea de lógica, y quien lo usa no puede romperlo por accidente.

#### 3.1.3 Un constructor adicional y los valores por omisión

El `revisor` necesita un `Servicio` con tres datos: un `nombre`, una `url` y un `timeoutMs`, el tiempo máximo que se le espera (la lección 8 lo usa de verdad). Casi todos los servicios usan el valor por omisión de 5000 ms; unos pocos necesitan otro. Java no tiene parámetros con valor por omisión (lo dijimos en la Lección 2), pero un *record* puede tener un **constructor adicional**, más corto, que llama al principal. Mira `fig03_03.java`:

```java
// fig03_03.java
record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}

void main() {
  URI direccion = URI.create("https://catalogo.example");
  Servicio catalogo = new Servicio("catalogo", direccion);
  Servicio notificaciones =
      new Servicio("notificaciones", URI.create("https://notificaciones.example"), 1000);

  IO.println(catalogo);
  IO.println(notificaciones);
  IO.println("host de catalogo: " + catalogo.url().getHost());
}
```

```text
$ java fig03_03.java
Servicio[nombre=catalogo, url=https://catalogo.example, timeoutMs=5000]
Servicio[nombre=notificaciones, url=https://notificaciones.example, timeoutMs=1000]
host de catalogo: catalogo.example
```

Hay cuatro novedades en esa figura.

**`URI`** es una clase del JDK que representa una dirección. `URI.create("https://catalogo.example")` crea una a partir de un texto, y `getHost()` devuelve el servidor (`catalogo.example`). Está en el paquete `java.net`. Para usar una clase de otro paquete en un archivo normal, hay que **importarla** con una línea `import java.net.URI;` al principio (lo verás en el proyecto de 3.5); el archivo compacto importa por ti todo lo que trae el módulo `java.base`, y `URI` es parte de él, así que aquí no hizo falta.

**Una constante dentro de un *record*.** `static final int TIMEOUT_POR_OMISION_MS = 5000;` es una constante como la de la lección anterior; `static` la hace pertenecer al tipo y no a cada objeto, que es la única manera de que un *record* tenga algo suelto.

**El constructor adicional.** `Servicio(String nombre, URI url)` tiene dos parámetros; su cuerpo, `this(nombre, url, TIMEOUT_POR_OMISION_MS)`, **llama al constructor principal** con el tercer dato completado. Con eso, `new Servicio("catalogo", direccion)` y `new Servicio("notificaciones", ..., 1000)` son llamadas válidas y salen con el tiempo que corresponde: 5000 en el primero, 1000 en el segundo. Fíjate también en el nombre `timeoutMs`: lleva la unidad, igual que en la lección anterior.

**La llamada partida en dos renglones.** Cuando una llamada pasa de 100 columnas, se corta después del `=` y la continuación entra con cuatro espacios más. Es la guía de estilo de la que hablamos.

#### 3.1.4 El constructor compacto: validar y normalizar

A veces quieres que el *record* **limpie o revise** sus datos al crearse: quitar espacios de un nombre, rechazar un tiempo imposible. Para eso existe el **constructor compacto**: se escribe con el nombre del *record* y **sin paréntesis**, y su cuerpo se ejecuta antes de que los componentes se asignen. Dentro puedes reasignar los parámetros (que tienen el nombre de los componentes) y, si algo está mal, detener el programa con un error. Mira `fig03_04.java`:

```java
// fig03_04.java
record Umbral(String nombre, long limiteMs) {
  Umbral {
    nombre = nombre.strip();
    if (limiteMs <= 0) {
      throw new IllegalArgumentException("limiteMs debe ser mayor que 0 y llegó " + limiteMs);
    }
  }
}

void main() {
  Umbral lento = new Umbral("  lento ", 1000);
  IO.println(lento);
  Umbral roto = new Umbral("roto", 0);
  IO.println(roto);
}
```

```text
$ java fig03_04.java
Umbral[nombre=lento, limiteMs=1000]
Exception in thread "main" java.lang.IllegalArgumentException: limiteMs debe ser mayor que 0 y llegó 0
	at fig03_04$Umbral.<init>(fig03_04.java:6)
	at fig03_04.main(fig03_04.java:14)
```

La primera creación pasa por el constructor compacto: `nombre = nombre.strip();` reasigna el parámetro con su versión limpia (la regla de 2.2.1: `strip` devuelve un texto nuevo, y aquí sí lo guardamos), y el *record* queda como `Umbral[nombre=lento, limiteMs=1000]`, con el nombre sin los espacios con que llegó. La segunda creación tiene `limiteMs` en 0, y la instrucción `throw` detiene la ejecución.

> **Una caja sobre `throw`.** `throw new IllegalArgumentException("...")` significa «aquí hay un problema que no sé resolver; detén lo que estás haciendo y avisa». `IllegalArgumentException` es una clase del JDK cuyo nombre quiere decir «argumento ilegal». Cuando nadie la atrapa, el programa termina y la JVM imprime el mensaje y la **traza** (la lista de dónde ocurrió: aquí, el constructor `Umbral.<init>` llamado desde `main`). Cómo atrapar una excepción, cuándo conviene y cuándo no, es la Lección 5. Por ahora solo necesitas saber que un constructor compacto es el lugar para rechazar datos que no tienen sentido.

La traza es una buena oportunidad de practicar lectura. Lo que dice, de arriba abajo: el error fue `IllegalArgumentException` con el mensaje que escribimos; ocurrió en `fig03_04$Umbral.<init>` (el constructor del *record*, dentro de la clase del archivo compacto) en la línea 6, y esa llamada la hizo `main` en la 14. El `$` separa la clase implícita del archivo de la clase `Umbral` que vive dentro.

En el `Servicio` del `revisor` **no validamos** en el constructor compacto: la validación de lo que llega de un archivo de configuración es una tarea que tiene su lugar propio, que llegará con las excepciones propias (Lección 5) y el JSON (Lección 8).

#### 3.1.5 La inmutabilidad es superficial

Un *record* es inmutable en el sentido de que **sus componentes no se reasignan**. Pero si un componente es, a su vez, un objeto que se puede modificar, ese objeto sigue siendo modificable. La inmutabilidad llega solo hasta la primera capa. Para verlo hace falta un tipo mutable, y el más simple de Java es el **arreglo**.

> **Un arreglo en tres líneas.** Un arreglo es una secuencia de tamaño fijo de elementos del mismo tipo. `String[] etapas = {"conectar", "esperar"};` crea uno de dos textos. `etapas[0]` es el primer elemento (se cuenta desde 0), y `etapas[0] = "otro";` **cambia** ese elemento: los arreglos son mutables. Para imprimir su contenido se usa `Arrays.toString(etapas)`; sin eso, imprime una dirección ilegible. Los arreglos casi no se usan en Java de todos los días; en la Lección 4 los reemplazan las listas. Pero son el ejemplo más claro de un objeto mutable.

Mira `fig03_05.java`:

```java
// fig03_05.java
record Ruta(String nombre, String[] etapas) {}

void main() {
  String[] etapas = {"conectar", "esperar"};
  Ruta ruta = new Ruta("catalogo", etapas);
  IO.println("antes:   " + Arrays.toString(ruta.etapas()));

  etapas[0] = "CAMBIADO DESDE FUERA";
  ruta.etapas()[1] = "CAMBIADO DESDE EL ACCESOR";
  IO.println("después: " + Arrays.toString(ruta.etapas()));

  Ruta copia = new Ruta("catalogo", new String[] {"conectar", "esperar"});
  Ruta otra = new Ruta("catalogo", new String[] {"conectar", "esperar"});
  IO.println("copia.equals(otra): " + copia.equals(otra));
}
```

```text
$ java fig03_05.java
antes:   [conectar, esperar]
después: [CAMBIADO DESDE FUERA, CAMBIADO DESDE EL ACCESOR]
copia.equals(otra): false
```

Hay dos agujeros. El primero: `etapas[0] = "CAMBIADO DESDE FUERA"` modifica el arreglo **original** que le pasamos al constructor, y como el *record* guarda **la misma referencia** (no una copia), el *record* cambió sin que nadie lo tocara. El segundo: `ruta.etapas()[1] = ...` modifica el arreglo a través del accesor, que devuelve la referencia que guarda. Por eso el programa imprime dos cambios en la segunda línea. El *record* no pudo evitar ninguno de los dos, porque lo que es final es la **referencia** `etapas`, no el contenido del arreglo.

La última línea tiene otra sorpresa. Dos `Ruta` creadas con arreglos de igual contenido **no son iguales** según `equals`. El `equals` del *record* compara componente por componente, y para comparar los arreglos usa el `equals` de los arreglos, que pregunta por identidad, como `==`. Es el mismo problema de 2.2.2, otra vez.

La lección práctica: **un *record* es tan inmutable como sus componentes.** Con textos, números, `enum`s y otros *records* inmutables no hay problema. Con un arreglo, o con cualquier objeto que se pueda modificar, la inmutabilidad es una ilusión. En la Lección 4 verás la solución habitual para las listas: guardar una **copia inmodificable** en el constructor compacto (`List.copyOf`). Ojo con lo que cierra y lo que no: nadie podrá agregar, quitar ni cambiar elementos **de la lista**, pero `List.copyOf` no copia los elementos ni los vuelve inmutables. Basta cuando los elementos ya son inmutables, como textos o *records* de componentes inmutables (el caso del `revisor`); si fueran objetos modificables, habría que copiar también cada elemento o diseñarlos inmutables.

### 3.2 Enums

El `revisor` clasifica cada servicio en uno de tres estados: `ARRIBA`, `LENTO` o `CAIDO`. En la Lección 2 el estado era un `String`, lo cual significa que cualquier texto era válido: `"ARIBA"`, `"caído"`, `""`. Un ***enum*** (de «enumeración») es un tipo cuyos valores posibles son **una lista fija, escrita por ti**, y nada más. Mira `fig03_06.java`:

```java
// fig03_06.java
enum Estado { ARRIBA, LENTO, CAIDO }

enum Severidad {
  BAJA(1), MEDIA(5), ALTA(10);

  final int peso;

  Severidad(int peso) {
    this.peso = peso;
  }

  boolean urgente() {
    return peso >= 5;
  }
}

String aviso(Estado estado) {
  return switch (estado) {
    case ARRIBA -> "todo bien";
    case LENTO -> "revisar pronto";
    case CAIDO -> "avisar ya";
  };
}

void main() {
  Estado estado = Estado.LENTO;
  IO.println(estado);
  IO.println("name():    " + estado.name());
  IO.println("ordinal(): " + estado.ordinal());
  IO.println("== :       " + (estado == Estado.LENTO));
  IO.println("valueOf:   " + Estado.valueOf("CAIDO"));
  IO.println("values():  " + Arrays.toString(Estado.values()));
  IO.println(aviso(estado));
  IO.println("ALTA pesa " + Severidad.ALTA.peso + " y urgente = " + Severidad.ALTA.urgente());
  IO.println("BAJA urgente = " + Severidad.BAJA.urgente());
}
```

```text
$ java fig03_06.java
LENTO
name():    LENTO
ordinal(): 1
== :       true
valueOf:   CAIDO
values():  [ARRIBA, LENTO, CAIDO]
revisar pronto
ALTA pesa 10 y urgente = true
BAJA urgente = false
```

El primer renglón, `enum Estado { ARRIBA, LENTO, CAIDO }`, declara el tipo con sus tres constantes. Las constantes se escriben en mayúsculas, sin acento (así lo fija el curso para las del `revisor`: `CAIDO`, no `CAÍDO`), y se usan como `Estado.LENTO`. Lo que ocurre en `main`:

- Imprimir un valor del *enum* escribe su nombre: `LENTO`. `name()` devuelve ese mismo nombre como texto.
- `ordinal()` dice en qué posición se declaró (`ARRIBA` es 0, `LENTO` es 1, `CAIDO` es 2). Se usa poco, y cuando se usa, casi siempre es un síntoma: si dependes del orden, cambiar la lista rompe el programa.
- **`==` es la comparación correcta entre valores de un *enum*.** Cada constante existe una sola vez en todo el programa, así que preguntar si dos variables apuntan a la misma es exactamente preguntar si valen lo mismo. Con los *enums*, `==` es justo lo que quieres.
- `valueOf("CAIDO")` convierte un texto en el valor (y lanza un error si el texto no es el nombre de ninguno), y `values()` devuelve un arreglo con todos, en el orden en que se declararon.

Un *enum* puede tener **datos y métodos propios**. `Severidad` tiene un campo `peso`, un constructor que lo recibe, y un método `urgente()`; cada constante lleva su dato entre paréntesis: `BAJA(1)`, `MEDIA(5)`, `ALTA(10)`. Sirve para que la información que corresponde a cada valor viva **junto al valor**, y no en un `switch` aparte.

La mejor razón para usar un *enum* se ve en `aviso`: es un `switch` con flechas, como los de la lección anterior, **sobre un *enum*** y sin `default`. El compilador sabe cuántos valores tiene `Estado`, y si el `switch` no los cubre todos, lo rechaza. Mira `fig03_07.java`, donde quitamos el caso `CAIDO`:

```java
// fig03_07.java
enum Estado { ARRIBA, LENTO, CAIDO }

String aviso(Estado estado) {
  return switch (estado) {
    case ARRIBA -> "todo bien";
    case LENTO -> "revisar pronto";
  };
}

void main() {
  IO.println(aviso(Estado.CAIDO));
}
```

```text
$ java fig03_07.java
fig03_07.java:5: error: the switch expression does not cover all possible input values
  return switch (estado) {
         ^
1 error
error: compilation failed
```

Es el mismo mensaje de la lección anterior («el `switch` no cubre todos los valores posibles»), pero con una diferencia enorme. Con textos, el compilador exigía un `default` porque hay infinitos textos. Con un *enum* **tú no necesitas `default`**: el compilador sabe que son tres, y si alguien agrega un cuarto estado al *enum* en el futuro, cada `switch` que no lo cubra deja de compilar. Los errores de «me faltó un caso» pasan de ser errores de ejecución a errores de compilación. Es una de las mejores noticias de la lección, y la segunda parte la lleva a un nivel más.

Con esto termina la primera sesión. Antes de seguir, comprueba que puedes responder sin mirar: ¿qué cuatro cosas te da un `record` sin escribirlas? ¿Por qué `Ruta` de `fig03_05.java` no es inmutable? ¿Por qué un `switch` sobre un *enum* no necesita `default`?


### 3.3 Interfaces selladas

#### 3.3.1 Una interfaz: un tipo con varias formas

Un servicio puede responder o no responder, y cada caso necesita **datos distintos**: si respondió, tiene un código y un tiempo; si no, tiene un motivo. Un `record` solo describe una forma. Para decir «un valor puede ser de una forma o de otra» hace falta un tipo que reúna a las dos, y en Java ese tipo es una **interfaz**.

Una interfaz es un tipo que declara **qué se puede hacer** sin decir cómo; cualquier clase o *record* que declare `implements NombreDeLaInterfaz` es, a la vez, un valor de ese tipo. Una variable de tipo `Respuesta` puede guardar cualquier cosa que implemente `Respuesta`. Es la herramienta que Java ofrece desde siempre para el «es uno de varios», y tiene un defecto: **cualquiera puede agregar una forma nueva** escribiendo otro tipo que diga `implements Respuesta`, en cualquier archivo, y el compilador no tiene forma de saber cuántas hay.

Para el `revisor` eso es un problema. La clasificación necesita decidir qué hacer con cada forma de respuesta, y quiere que el compilador le garantice que **no se le quedó ninguna**. Esa garantía es el trabajo de las interfaces **selladas**.

#### 3.3.2 `sealed` y `permits`

Una interfaz sellada es una interfaz que lista, por adelantado, **todos los tipos que pueden implementarla**. Se escribe con la palabra `sealed` y una cláusula `permits`. Mira `fig03_08.java`, que define la `Respuesta` del `revisor`:

```java
// fig03_08.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio r -> "respondió " + r.codigo() + " en " + r.tiempoMs() + " ms";
    case Respuesta.SinRespuesta s -> "no respondió: " + s.motivo();
  };
}

void main() {
  Respuesta uno = new Respuesta.Respondio(200, 142);
  Respuesta otro = new Respuesta.SinRespuesta("sin conexión");
  IO.println(describir(uno));
  IO.println(describir(otro));
}
```

```text
$ java fig03_08.java
respondió 200 en 142 ms
no respondió: sin conexión
```

Lee la primera línea: `sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta`. Dice: «`Respuesta` es una interfaz; solo se permiten estas dos formas». Adentro, entre las llaves de la interfaz, están definidas las dos formas como *records* **anidados**: `Respondio(int codigo, long tiempoMs)` y `SinRespuesta(String motivo)`, cada uno con `implements Respuesta`. Un tipo definido dentro de otro se nombra con su prefijo, y de ahí los nombres `Respuesta.Respondio` y `Respuesta.SinRespuesta` en el resto del programa. (En un proyecto de varios archivos sin paquete, no se puede escribir un `import` para acortarlos; usarás el nombre completo.)

Ahora mira lo que expresa el modelo. **Un servicio que respondió** tiene un código y un tiempo, siempre los dos. **Uno que no respondió** tiene un motivo, y no tiene código ni tiempo, ni siquiera un `null`: simplemente esos datos no existen en esa forma. Se acabó el estado imposible de la Lección 2 (código sin tiempo, o al revés). Es una unión discriminada de TypeScript, un `enum` con datos de Rust, un tipo suma de Haskell: la misma idea con la sintaxis de Java.

Las formas permitidas tienen que ser `final`, `sealed` o `non-sealed`; los *records* son `final` automáticamente, de modo que aquí no hay que escribir nada. Y tienen que vivir en el mismo paquete (en nuestro caso, el paquete sin nombre en que está todo el proyecto) o módulo. Si alguien intenta sumar una forma que no está en la lista, el compilador lo detiene. Es `fig03_09.java`, que agrega `Redirigida` fuera del `permits`:

```java
// fig03_09.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

record Redirigida(int codigo, String destino) implements Respuesta {}

void main() {
  Respuesta respuesta = new Redirigida(301, "https://catalogo.example/nuevo");
  IO.println(respuesta);
}
```

```text
$ java fig03_09.java
fig03_09.java:8: error: class is not allowed to extend sealed class: Respuesta (as it is not listed in its 'permits' clause)
record Redirigida(int codigo, String destino) implements Respuesta {}
^
1 error
error: compilation failed
```

El mensaje es claro: «la clase no puede extender una clase sellada: `Respuesta` (porque no está en su `permits`)». Para agregar una forma nueva hay que **editar la interfaz**, y esa edición es lo que activa los avisos del compilador en el resto del programa. En 3.4 verás cuáles.

Un detalle de `fig03_08.java`: el `switch` de `describir` tiene dos casos, `case Respuesta.Respondio r ->` y `case Respuesta.SinRespuesta s ->`, y no tiene `default`. Eso es lo que explica la sección siguiente.

### 3.4 `switch` con patrones

#### 3.4.1 Patrones de tipo y exhaustividad

En la Lección 2 el `switch` comparaba un valor contra constantes (`case "catalogo"`). Desde Java 21, un `switch` puede comparar contra **patrones**: en lugar de «¿vale esto?», pregunta «¿tiene esta forma?». El patrón más simple es el **patrón de tipo**: `case Respuesta.Respondio r` quiere decir «si la respuesta es un `Respondio`, llámalo `r` dentro de este caso». Adentro, `r` ya tiene el tipo preciso, así que se puede escribir `r.codigo()` y `r.tiempoMs()` sin ningún molde. La función se estandarizó en el [JEP 441](https://openjdk.org/jeps/441).

Lo importante es lo que hace el compilador con un `switch` sobre un tipo sellado: **sabe cuántas formas hay**. Si los casos cubren todas, el `switch` es **exhaustivo** y no necesita `default`. Si falta alguna, no compila. Mira `fig03_10.java`, que lo ilustra quitando la forma `SinRespuesta`:

```java
// fig03_10.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio r -> "respondió " + r.codigo();
  };
}

void main() {
  IO.println(describir(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_10.java
fig03_10.java:9: error: the switch expression does not cover all possible input values
  return switch (respuesta) {
         ^
1 error
error: compilation failed
```

El mensaje es el mismo que con el *enum* de 3.2: «el `switch` no cubre todos los valores posibles». Es la misma garantía, ahora para formas con datos.

Y el contrario es un riesgo igual de real. Un `default` en un `switch` sobre un tipo sellado apaga esa garantía. Mira `fig03_11.java`. Se agregó una tercera forma, `Redirigida`, y el `switch` de `clasificar` tiene un `default`:

```java
// fig03_11.java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.Respondio _ -> "ARRIBA";
    default -> "CAIDO";
  };
}

void main() {
  Respuesta redirigida = new Respuesta.Redirigida(301, "https://catalogo.example/nuevo");
  IO.println(clasificar(redirigida));
}
```

```text
$ java fig03_11.java
CAIDO
```

El programa compila, corre y dice que una redirección es `CAIDO`. Nadie decidió eso: el `default` se tragó la forma nueva y respondió por todas las que no menciona. Es un error silencioso, el peor tipo, y el compilador no pudo avisar porque el `default` le dijo «ya cubrí todo». **Con un tipo sellado, o con un *enum*, no escribas `default`.** Si lo omites, cada forma nueva rompe la compilación en cada `switch` que debe considerarla, que es exactamente lo que quieres.

#### 3.4.2 Guardas, patrones de record y `_`

El `revisor` necesita más que distinguir entre «respondió» y «no respondió»: dentro de «respondió» hay tres casos (código de error, lento, normal). Los patrones tienen dos herramientas para eso.

La primera es la **guarda** `when`: una condición adicional que el caso debe cumplir. `case Respuesta.Respondio r when r.codigo() >= 400` entra solo si la respuesta es un `Respondio` **y** además su código es 400 o más. Ya es el `&&` de la lección anterior, aplicado al patrón.

La segunda es el **patrón de record**: en lugar de nombrar el objeto y luego pedirle sus componentes, el patrón los **desarma** en el mismo caso. `case Respuesta.Respondio(int codigo, long tiempoMs)` quiere decir «si es un `Respondio`, saca su código en la variable `codigo` y su tiempo en `tiempoMs`». Está definido en el [JEP 440](https://openjdk.org/jeps/440).

Y un detalle de comodidad: si de un componente no necesitas el valor, lo reemplazas con el **patrón sin nombre** `_` (final desde Java 22, [JEP 456](https://openjdk.org/jeps/456)). Dice «aquí hay algo, y no me importa». Mira `fig03_12.java`, que implementa la clasificación del `revisor` de punta a punta:

```java
// fig03_12.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

static final long UMBRAL_LENTO_MS = 1000;

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS -> "LENTO";
    case Respuesta.Respondio _ -> "ARRIBA";
    case Respuesta.SinRespuesta _ -> "CAIDO";
  };
}

void main() {
  IO.println(clasificar(new Respuesta.Respondio(200, 142)));
  IO.println(clasificar(new Respuesta.Respondio(200, 2300)));
  IO.println(clasificar(new Respuesta.Respondio(503, 95)));
  IO.println(clasificar(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_12.java
ARRIBA
LENTO
CAIDO
CAIDO
```

Cuatro casos, en este orden, cada uno una regla del `revisor`:

1. `Respondio(int codigo, long _) when codigo >= 400` es la regla 2: respondió con error. Usa el código y no le importa el tiempo.
2. `Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS` es la regla 3: respondió y tardó. Ahora usa el tiempo y no el código.
3. `Respondio _` es la regla 4, la que queda: cualquier otra respuesta es ARRIBA. Con `_` en lugar de un nombre, ni siquiera se pone nombre a la variable, porque no se usa.
4. `SinRespuesta _` es la regla 1: sin respuesta, CAIDO.

Los casos se evalúan **de arriba abajo, y gana el primero que se cumple**. Por eso el orden importa, y el compilador te cuida de un orden imposible. Mira `fig03_13.java`, donde el caso general `Respondio _` se escribió antes que el específico:

```java
// fig03_13.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio _ -> "ARRIBA";
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.SinRespuesta _ -> "CAIDO";
  };
}

void main() {
  IO.println(clasificar(new Respuesta.Respondio(503, 95)));
}
```

```text
$ java fig03_13.java
fig03_13.java:11: error: this case label is dominated by a preceding case label
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
         ^
1 error
error: compilation failed
```

«Este caso está dominado por un caso anterior». El primer caso ya atrapa **todos** los `Respondio`, de modo que los que vienen después, con guarda, nunca podrían ejecutarse. Java lo marca como error, no como advertencia. La regla: **de lo más específico a lo más general**, y el caso sin guarda de un tipo va después de los que sí la tienen.

Antes de seguir, un contraste con la Lección 2. Allí `clasificar` recibía un `Integer` y un `Long` que podían ser nulos, y usaba `&&` para no desenvolver un vacío. Aquí `clasificar` recibe **una** `Respuesta`, y sus formas posibles ya dicen qué datos hay. No hay nulos que comprobar, porque no hay forma de pedirle el código a una respuesta que no lo tiene. La comprobación que antes hacías tú, a mano, ahora la hace el compilador.

#### 3.4.3 `null` y `instanceof`

Hay dos cosas más que conviene conocer, porque aparecen en el código que leerás.

**Un `switch` sobre una referencia nula revienta**, a menos que lo previenes. Un `case null` captura el caso, y si lo omites, el `switch` lanza un `NullPointerException` antes de mirar ningún otro caso. Mira `fig03_14.java`:

```java
// fig03_14.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case null -> "no hay respuesta registrada";
    case Respuesta.Respondio(int codigo, long _) -> "respondió " + codigo;
    case Respuesta.SinRespuesta(String motivo) -> "no respondió: " + motivo;
  };
}

String sinCasoNulo(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio _ -> "respondió";
    case Respuesta.SinRespuesta _ -> "no respondió";
  };
}

void main() {
  IO.println(describir(new Respuesta.Respondio(200, 142)));
  IO.println(describir(null));
  IO.println(sinCasoNulo(null));
}
```

```text
$ java fig03_14.java
respondió 200
no hay respuesta registrada
Exception in thread "main" java.lang.NullPointerException
	at java.base/java.util.Objects.requireNonNull(Objects.java:220)
	at fig03_14.sinCasoNulo(fig03_14.java:17)
	at fig03_14.main(fig03_14.java:26)
```

`describir` incluye `case null`, y por eso responde a `null` con un texto. `sinCasoNulo` no lo incluye, y el mismo `null` termina en el `NullPointerException` que se ve al final. La traza trae un renglón `at java.base/java.util.Objects.requireNonNull(Objects.java:220)`: el número 220 es una línea del propio JDK y puede ser otro en tu versión. Lo que importa es leer las dos líneas siguientes, que son tuyas: `sinCasoNulo` en el renglón 17, llamada desde `main` en el 26. En el `revisor` no hay nulos que lleguen a un `switch`, porque el tipo sellado hace imposible que existan; pero conviene saber qué pasa cuando sí.

**`instanceof` con patrón** es el hermano del `switch` para una sola pregunta. `respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400` pregunta «¿es un `Respondio`?», lo llama `r` si lo es, y sigue con la condición. Mira `fig03_15.java`:

```java
// fig03_15.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

boolean esError(Respuesta respuesta) {
  return respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400;
}

void main() {
  IO.println(esError(new Respuesta.Respondio(503, 95)));
  IO.println(esError(new Respuesta.Respondio(200, 142)));
  IO.println(esError(new Respuesta.SinRespuesta("sin conexión")));
}
```

```text
$ java fig03_15.java
true
false
false
```

Es una expresión que da `true` o `false`. Fíjate cómo el `&&` hace de cortocircuito: `r` solo existe, y `r.codigo()` solo se evalúa, si el `instanceof` fue verdadero. Úsalo cuando tengas una sola pregunta sobre un tipo; si hay dos o más formas que tratar, un `switch` exhaustivo es mejor, porque el compilador te cuida los casos que falten.

### 3.5 El paso del proyecto: el `revisor` en varios archivos

Ahora ordenas el `revisor` con todo lo que aprendiste. Hay un cambio de forma, y uno de contenido.

**La forma.** El `Revisor.java` de una sola pieza se reparte en siete archivos, **todos en la misma carpeta** `~/curso-java/revisor/`, sin ninguna subcarpeta. Pregunta natural: ¿cómo se corre algo de varios archivos sin Maven? Con el mismo comando de siempre, `java Revisor.java`. Desde Java 22, el lanzador compila el archivo que le das y busca en la misma carpeta los demás archivos `.java` que ese necesita ([JEP 458](https://openjdk.org/jeps/458)). El archivo de entrada sigue siendo compacto, con `void main()`; los demás son tipos normales, cada uno en el archivo que lleva su nombre. Es lo que hace posible un proyecto con varias piezas **antes** de hablar de Maven, que llega en la Lección 6.

**El contenido.** Se hacen cuatro cambios, y vale la pena enumerarlos porque son la lección en miniatura:

1. El `String` de estado se vuelve un `enum`.
2. Los dos `null` de «sin respuesta» se vuelven una interfaz sellada con dos formas.
3. Los datos de un servicio se reúnen en un `record`.
4. Los dos `switch` paralelos (código y tiempo) se vuelven **uno** solo, que devuelve la forma completa.

Las piezas, una por una:

**`Estado.java`** es el *enum* de 3.2, con las tres constantes. La guía de estilo de Google escribe las constantes una por línea.

```java
// fig03_16/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

**`Respuesta.java`** es la interfaz sellada de 3.3, tal cual.

```java
// fig03_16/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

**`Servicio.java`** es el *record* de 3.1: el nombre, la dirección y el tiempo límite, con el constructor adicional que completa el valor por omisión. Es la primera vez que ves un `import` de verdad: un archivo normal no importa nada por sí mismo, así que `URI` se trae con una línea.

```java
// fig03_16/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

**`Resultado.java`** junta lo que el `revisor` sabe de un servicio ya revisado: cuál es, en qué `Estado` quedó y cuál fue su `Respuesta`. Tiene un método, `detalle()`, que usa un `switch` con patrones de record: si respondió, el detalle es el código como texto; si no, es el motivo. Sin `default`, y sin nulos.

```java
// fig03_16/Resultado.java
record Resultado(Servicio servicio, Estado estado, Respuesta respuesta) {
  String detalle() {
    return switch (respuesta) {
      case Respuesta.Respondio(int codigo, long _) -> String.valueOf(codigo);
      case Respuesta.SinRespuesta(String motivo) -> motivo;
    };
  }
}
```

**`Clasificador.java`** contiene la clasificación de 3.4.2. Es una clase `final` (no se puede extender) cuyo único trabajo es agrupar una función y la constante que usa. Los métodos y las constantes `static` pertenecen a la clase y no a un objeto, y se llaman con el nombre de la clase por delante: `Clasificador.clasificar(respuesta)`. (La Lección 6 explica `private`, la palabra que cierra esa puerta para que nadie cree un `Clasificador` que no sirve para nada.) Se ve que `clasificar` ahora **devuelve un `Estado`**, y no un `String`.

```java
// fig03_16/Clasificador.java
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

**`Formato.java`** reúne lo que da formato. Hay un cambio de firma importante respecto a la lección anterior: `formatearTiempo` ahora recibe un `long`, no un `Long`. Ya no tiene que decidir qué hacer con un nulo, porque el caso «sin tiempo» no llega hasta ella. Quien decide es la función `tiempo(Respuesta)`, con un `switch` exhaustivo: si hay tiempo lo formatea, y si no, escribe la raya. El tipo hizo el trabajo que antes hacía la comprobación `if (tiempoMs == null)`. La plantilla de las columnas, que antes estaba escrita dos veces (en `linea` y en `encabezado`), ahora es la constante `PATRON_LINEA`. Se usa `import java.util.Locale;` porque el archivo no es compacto.

```java
// fig03_16/Formato.java
import java.util.Locale;

final class Formato {
  static final String PATRON_LINEA = "%-12s%-10s%6s  %s";

  static String formatearTiempo(long tiempoMs) {
    if (tiempoMs < Clasificador.UMBRAL_LENTO_MS) {
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
}
```

**`Revisor.java`** es el único archivo compacto, y el punto de entrada. `simular` es el **único `switch`** que sustituye a `codigoDe` y `tiempoDe`: devuelve un `Respondio` con el código y el tiempo juntos, o un `SinRespuesta` para todo servicio que no conozca. `revisar` recibe un `Servicio`, simula la respuesta, la clasifica y devuelve el `Resultado`. El `main` crea los cuatro servicios con `URI.create(...)` y los imprime. (Los cuatro escritos uno por uno, porque aún no hay listas; la Lección 4 lo arregla.)

```java
// fig03_16/Revisor.java
Respuesta simular(String nombre) {
  return switch (nombre) {
    case "catalogo" -> new Respuesta.Respondio(200, 142);
    case "inventario" -> new Respuesta.Respondio(200, 2300);
    case "pagos" -> new Respuesta.Respondio(200, 87);
    default -> new Respuesta.SinRespuesta("sin conexión");
  };
}

Resultado revisar(Servicio servicio) {
  Respuesta respuesta = simular(servicio.nombre());
  return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
}

void main() {
  Servicio catalogo = new Servicio("catalogo", URI.create("https://catalogo.example"));
  Servicio inventario = new Servicio("inventario", URI.create("https://inventario.example"));
  Servicio pagos = new Servicio("pagos", URI.create("https://pagos.example"));
  Servicio reportes = new Servicio("reportes", URI.create("https://reportes.example"));

  IO.println(Formato.encabezado());
  IO.println(Formato.linea(revisar(catalogo)));
  IO.println(Formato.linea(revisar(inventario)));
  IO.println(Formato.linea(revisar(pagos)));
  IO.println(Formato.linea(revisar(reportes)));
}
```

Para correrlo:

```text
$ cd fig03_16
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Es **carácter por carácter la misma tabla** que al terminar la Lección 2, con una diferencia que no se ve y que importa: ahora, para que `reportes` salga como salió, nadie escribió un `null`. El tipo `SinRespuesta` es lo que hace que no tenga tiempo, que su detalle sea el motivo y que su estado sea `CAIDO`.

Lo bueno del diseño aparece cuando lo cambias. El ejercicio 4 de esta lección es ése.

## El error que vas a ver

La lección tiene un mensaje principal y cuatro de apoyo.

**El error principal: «the switch expression does not cover all possible input values».** Es el que ves cuando le falta un caso a un `switch` sobre un `enum` o una interfaz sellada, y es **el mismo texto en los dos casos**. Aparece en `fig03_07.java` (el *enum*) y en `fig03_10.java` (la interfaz sellada):

```text
fig03_10.java:9: error: the switch expression does not cover all possible input values
  return switch (respuesta) {
         ^
```

Es la señal de que el compilador **te está ayudando**: quitaste un caso, o agregaste una forma, y hay un lugar que debes revisar. No lo silencies con un `default`. Lee el renglón que señala el `^` y decide, para esa forma, qué quieres que pase.

**Los cuatro mensajes de apoyo:**

- `error: cannot assign a value to final variable tiempoMs`: intentaste cambiar un componente de un *record*. No se puede. Si necesitas otro valor, crea otro *record* (`fig03_02.java`).
- `error: class is not allowed to extend sealed class: Respuesta (as it is not listed in its 'permits' clause)`: quisiste implementar una interfaz sellada con un tipo que no está en la lista. Agrégalo al `permits` (`fig03_09.java`).
- `error: this case label is dominated by a preceding case label`: un caso general está antes que uno específico. Reordena de lo más específico a lo más general (`fig03_13.java`).
- `IllegalArgumentException` con tu mensaje, desde un constructor compacto: alguien intentó crear un *record* con datos que tu constructor rechaza. La traza dice quién lo intentó (`fig03_04.java`).

## Lo que se hace mal

**Un `default` en un `switch` sobre un tipo sellado o un *enum*.** Silencia la comprobación que justifica haberlos escrito, como mostró `fig03_11.java`: el programa compiló y clasificó mal una redirección. Si la forma nueva merece el mismo trato que otra, escribe su caso aparte (o junta los dos con una coma).

**Un `record` con un arreglo, o con cualquier objeto que se pueda modificar.** Parece inmutable y no lo es: se cambia por el accesor y por la referencia original, y su `equals` compara arreglos por identidad. En la Lección 4 verás `List.copyOf`.

**Un `String` donde cabe un `enum`.** Un estado, un tipo, un nivel. Todo valor que sale de una lista fija debe ser un *enum*, para que una errata no sea un estado nuevo.

**Un `null` donde cabe una forma.** Dos `null` paralelos (código, tiempo) permiten estados imposibles. Si «no hay dato» es una posibilidad con significado propio, dale su propia forma en un tipo sellado, con los datos que sí tiene (aquí, el motivo).

**Usar `ordinal()` para algo que no sea depuración.** El número de una constante cambia si alguien reordena el *enum*.

**Comparar `enum`s con `equals`.** Funciona, pero `==` es lo idiomático, y evita un `NullPointerException` si la variable es nula (`estado.equals(...)` revienta con `null`, `estado == ...` no).

**Un caso general antes de uno específico.** El compilador lo detecta, pero es un error que te conviene entender y no solo arreglar: el orden de los casos es parte del significado.

**Poner validaciones largas en el constructor compacto.** Es el lugar de normalizar y de rechazar lo claramente absurdo. La lógica de negocio va en otra parte.

## Ejercicios

### Ejercicio 1 — Qué igualdad te da un `record`

Declara `record Servicio(String nombre, URI url, int timeoutMs) {}` en un archivo compacto y crea tres objetos: `a` y `b` con los mismos valores (5000 de tiempo límite) y `c` igual que ellos pero con 1000. **Antes de ejecutar**, predice el resultado de `a == b`, `a.equals(b)`, `a.equals(c)`, `a.hashCode() == b.hashCode()` y de imprimir `c`. Ejecuta y compara.

### Ejercicio 2 — Cerrar una puerta de la inmutabilidad

Toma `fig03_05.java` y agrega a `Ruta` un constructor compacto que guarde una **copia** del arreglo (los arreglos tienen un método `clone()` que devuelve una copia). Ejecuta de nuevo. ¿Cuál de los dos cambios dejó de afectar al *record*? ¿Cuál sigue pasando? ¿Qué te hace falta para cerrar la otra puerta?

### Ejercicio 3 — Un estado nuevo en el `enum`

En el `enum` de `fig03_06.java`, agrega un cuarto valor, `DEGRADADO`, entre `LENTO` y `CAIDO`, y no toques nada más. Intenta ejecutar. ¿Qué dice el compilador y en qué función? Después arréglalo con el mensaje que te parezca razonable para el estado nuevo, y escribe en tu bitácora qué habría pasado si `Estado` hubiera sido un `String`.

### Ejercicio 4 — El proyecto: una forma más de respuesta

En el proyecto de 3.5, agrega una tercera forma a `Respuesta`: `record Redirigida(int codigo, String destino)`, que representa un servicio que contestó con un código 3xx y dice a dónde mandar. Hazlo en `Respuesta.java` (la forma y el `permits`) y **no toques nada más**. Corre `java Revisor.java`. Anota **cada lugar** que el compilador te obliga a corregir, uno por uno: corrígelos y vuelve a correr hasta que compile. Decide qué quieres que pase con una redirección: ¿qué estado le corresponde, qué tiempo muestra, qué detalle?

## Soluciones

### Solución 1

```text
false
true
false
true
Servicio[nombre=pagos, url=https://pagos.example, timeoutMs=1000]
```

`a == b` es `false`: son dos objetos distintos. `a.equals(b)` es `true`: el `equals` del *record* compara componente por componente (y el `equals` de `URI`, a su vez, compara el contenido). `a.equals(c)` es `false`: difieren en `timeoutMs`. Los `hashCode` de `a` y `b` coinciden porque `equals` los declara iguales, y el *record* garantiza esa coherencia. Y `c` se imprime con el formato de siempre.

### Solución 2

El constructor compacto que guarda la copia:

```java
record Ruta(String nombre, String[] etapas) {
  Ruta {
    etapas = etapas.clone();
  }
}
```

Con él, la salida es:

```text
tras cambiar el original: [conectar, esperar]
tras cambiar por el accesor: [conectar, CAMBIADO DESDE EL ACCESOR]
```

El primer agujero se cerró: cambiar el arreglo original ya no afecta al *record*, porque este guarda su propia copia. El segundo sigue abierto: el accesor devuelve la referencia a la copia que guarda, y cambiarla desde fuera la modifica. Para cerrarlo habría que sobrescribir el accesor para que devuelva otra copia; en un *record*, un accesor escrito a mano tiene que ser `public` (la Lección 6 explica qué significa), y aun así `equals` seguiría comparando arreglos por identidad. La conclusión práctica es que **un arreglo es un mal componente**. En la siguiente lección verás `List.copyOf`, que sí cierra las dos puertas.

### Solución 3

El compilador rechaza el programa en `aviso`, la función con el `switch`:

```text
error: the switch expression does not cover all possible input values
  return switch (estado) {
         ^
```

Falta el caso `DEGRADADO`. Se arregla agregando `case DEGRADADO -> "revisar ya";` (o el mensaje que elijas). Con un `String`, nada habría avisado: `"DEGRADADO"` sería un texto más, `aviso` caería en el `default` (que habría sido obligatorio) y el servicio degradado recibiría el aviso de los desconocidos. El *enum* convirtió ese descuido en un error de compilación.

### Solución 4

Con la forma nueva agregada solo en `Respuesta.java`:

```java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}
```

el compilador se queja primero de `Clasificador.java` (la ruta completa de tu carpeta aparece delante del nombre del archivo; aquí la acorto):

```text
Clasificador.java:6: error: the switch expression does not cover all possible input values
    return switch (respuesta) {
           ^
```

Se agrega un caso (por ejemplo, `case Respuesta.Redirigida _ -> Estado.ARRIBA;`: una redirección es una respuesta válida), y al volver a correr, el siguiente es `Formato.java:15` (la función `tiempo`), y después `Resultado.java:4` (el método `detalle`). **Son tres lugares**: la clasificación, el tiempo mostrado y el detalle. En mi corrida el compilador los mostró de uno en uno, en ese orden. Puedes decidir distinto qué hace cada uno: un estado `ARRIBA` es razonable porque el servicio contestó, el tiempo puede ser la raya (no tenemos uno en la forma) y el detalle puede ser el código con el destino. Lo importante no es la decisión, sino que el compilador te **obligó a tomarla** en cada sitio. Con dos `null` paralelos, nada te habría avisado.

## Cómo sé que lo logré

- [ ] `java fig03_01.java` imprime `Punto[nombre=catalogo, tiempoMs=142]` y puedes explicar por qué `a == b` es `false` y `a.equals(b)` es `true`.
- [ ] `java fig03_05.java` muestra las dos modificaciones a la `Ruta`, y puedes decir por qué ocurren.
- [ ] En `fig03_10.java` ves el error «does not cover all possible input values», y sabes qué caso faltó.
- [ ] En `fig03_11.java` ves que el `default` clasifica una redirección como `CAIDO`, y puedes decir por qué es peor que el error anterior.
- [ ] Desde `fig03_16/`, `java Revisor.java` imprime exactamente la tabla de cuatro renglones de la Lección 2: `catalogo` ARRIBA 142ms, `inventario` LENTO 2.3s, `pagos` ARRIBA 87ms, `reportes` CAIDO — «sin conexión».
- [ ] Después de agregar `Redirigida`, el compilador te mostró tres lugares y los corregiste uno por uno.
- [ ] Puedes explicar con tus palabras por qué el `revisor` de esta lección no tiene un solo `null`.
- [ ] Anotaste en tu bitácora el estado imposible que el tipo sellado te impidió escribir, y qué pasó cuando agregaste un caso nuevo.

## Resumen

Responde sin mirar la lección:

1. ¿Qué cuatro cosas genera el compilador para un `record`? ¿Qué compara su `equals`?
2. ¿Qué diferencia hay entre un constructor compacto y uno adicional, y para qué sirve cada uno?
3. ¿Por qué un `record` con un arreglo adentro no es inmutable? ¿Qué se hace en la Lección 4 para arreglarlo?
4. ¿Por qué se compara un `enum` con `==`? ¿Qué ventaja da escribir un `switch` sobre un `enum` sin `default`?
5. ¿Qué dice `sealed ... permits` y qué pasa si intentas implementar la interfaz con un tipo que no está en la lista?
6. ¿Qué es una guarda `when`, un patrón de record y el patrón `_`? Da un ejemplo de cada uno con el `revisor`.
7. ¿Por qué el orden de los casos importa? ¿Qué pasa si pones el caso general antes?
8. ¿Qué hace un `switch` sobre una referencia nula si no tiene `case null`?
9. ¿Qué cambió en la firma de `formatearTiempo` entre la Lección 2 y la 3, y qué lo hizo posible?
10. ¿Por qué un `default` en un `switch` sobre un tipo sellado es un riesgo?

## Para leer más

- [Los tutoriales de Java, «Record Classes»](https://docs.oracle.com/en/java/javase/25/language/records.html): los *records* explicados con más ejemplos, de la documentación oficial (en inglés). Consultado el 7 de octubre de 2026.
- [JEP 409, «Sealed Classes»](https://openjdk.org/jeps/409): el porqué y las reglas completas de `sealed`, `permits`, `final` y `non-sealed`. Consultado el 7 de octubre de 2026.
- [JEP 441, «Pattern Matching for switch»](https://openjdk.org/jeps/441) y [JEP 440, «Record Patterns»](https://openjdk.org/jeps/440): las reglas exactas de la exhaustividad, el dominio y las guardas. Consultado el 7 de octubre de 2026.
- [Los tutoriales de Java, «Enum Types»](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html): los *enums* con campos, constructores y métodos. Consultado el 7 de octubre de 2026.

---

**Anterior:** [Lección 2 — Tipos, cadenas y control de flujo](02-tipos-cadenas-control.md) ·
**Siguiente:** [Lección 4 — Colecciones, genéricos y streams](04-colecciones-genericos-streams.md)
