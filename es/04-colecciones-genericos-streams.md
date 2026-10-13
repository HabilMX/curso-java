# Lección 4 — Colecciones, genéricos y streams

**Tiempo:** 2 × 90 min (dos sesiones; el corte está entre las secciones 4.2 y 4.3)

**Qué construyes:** la lista de servicios y el reporte del `revisor`: ahora el programa guarda sus servicios en una colección, resume los resultados con un *stream* y los entrega en una tabla con su línea de resumen

**Qué aprendes:** `List`, `Set` y `Map`; colecciones inmutables y copias defensivas; genéricos y su borrado; lambdas y *streams* con `Collectors`; `Comparator`; y `Optional` como valor de retorno

**De dónde vienes.** Traes el proyecto de la [Lección 3](03-records-enums-sellados.md): siete archivos que cumplen un trato claro. `Estado` es un `enum` con tres valores; `Respuesta` es una interfaz sellada que solo puede ser `Respondio` (con su código y su tiempo) o `SinRespuesta` (con su motivo); `Servicio` y `Resultado` son *records*; `Clasificador` decide el estado con un `switch` exhaustivo; `Formato` arma los renglones; y `Revisor.java` escribe a mano los cuatro servicios y los revisa uno por uno. La tabla sale bien, pero tiene tres huecos. No hay una lista: los servicios son cuatro variables sueltas. No hay resumen: ninguna línea dice cuántos están arriba. Y los datos simulados viven dentro de un `switch`, mezclados con la lógica. Esta lección llena los tres huecos con las herramientas que Java trae para guardar y transformar muchos valores. Trabajas en tu carpeta `~/curso-java/revisor/` y ejecutas todo con `java`.

**Qué no hace esta lección.** No lee archivos ni atrapa errores: la lista de servicios sigue escrita en `Revisor.java`, y quitarla de ahí es el trabajo de la [Lección 5](05-errores-excepciones.md). Tampoco consulta ninguna red: el `Simulador` que escribes hoy hace el papel de la red hasta la lección 8. Y no usa Maven: los archivos siguen sueltos en una carpeta, como en la lección 3.

**Sobre las corridas.** Para ejecutar una figura, entra a la carpeta de la lección en el repositorio (`programas/04-colecciones-genericos-streams/`) y corre `java fig04_01.java`, con el número de la figura que quieras. Las que viven en una carpeta propia (la del proyecto de la sección 4.5) se ejecutan desde dentro de ella. Todas las salidas se obtuvieron con Java 25 (Temurin 25.0.4) y el idioma de la terminal en UTF-8; en Linux Mint eso es lo normal; si ves signos de interrogación donde van acentos, `echo $LANG` te dice qué idioma tiene tu terminal. Las trazas de error traen renglones del propio JDK (`at java.base/...`) con números de línea que cambian de una actualización a otra: es normal que los tuyos difieran en esos números.

## Al terminar vas a poder

- Elegir entre `List`, `Set` y `Map` según lo que necesitas guardar, y decir qué orden de recorrido tiene cada implementación.
- Crear una colección inmutable con `List.of`, reconocer el `UnsupportedOperationException` que sale al modificarla y proteger un *record* con una copia defensiva.
- Explicar qué son los genéricos, qué garantiza el compilador con ellos y qué se pierde con el borrado de tipos.
- Escribir una lambda y una referencia a método, y armar con ellas un *stream* de `filter`, `map` y `collect`.
- Resumir una lista de resultados con `Collectors.groupingBy`, `counting` y `average`, y ordenarla con un `Comparator`.
- Devolver un `Optional` en lugar de `null` y leerlo sin llamar a `get()` a ciegas.

## El porqué antes del cómo

Abre tu `Revisor.java` de la lección 3. Los cuatro servicios están escritos como cuatro llamadas separadas a `revisar`, y ahí se acaba lo que el programa sabe de ellos: no puede decir cuántos son, no puede recorrerlos en un ciclo, no puede agregar uno sin escribir otro renglón. Ahora imagina que te piden la línea que cierra el reporte: «4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms». Para producirla con lo que tienes hasta hoy necesitarías cuatro contadores (`arriba`, `lento`, `caido`, `total`), una suma, un divisor, y actualizar todo cada vez que un servicio cambia de estado. Cada servicio nuevo, y cada estado nuevo, tocaría varios lugares del programa.

Todos los lenguajes resolvieron esto igual: con **colecciones**, estructuras que guardan muchos valores y saben recorrerse, buscar y contarse solas. Si vienes de Python conoces la lista, el conjunto y el diccionario; si vienes de Go, el *slice*, el mapa y la costumbre de usar un `map[string]struct{}` como conjunto; si vienes de TypeScript, el arreglo, el `Set` y el `Map`. Java tiene las tres, y las organiza de una forma que se parece menos a los otros lenguajes de lo que parece: son **interfaces** (`List`, `Set`, `Map`) con **varias implementaciones** intercambiables, y esa separación explica casi todo lo que vas a ver hoy, desde por qué hay tres mapas distintos hasta por qué existen las colecciones inmutables.

Hay un problema más. Una colección guarda valores, pero ¿de qué tipo? Si guarda cualquier cosa, el compilador no puede ayudarte: tendrías que verificar a mano que lo que sacas de la lista es el servicio que esperabas. Java resuelve esto con los **genéricos**: tú declaras que la lista es de `Servicio` y el compilador impide que entre otra cosa. Es una de las ideas más útiles del lenguaje y tiene un detalle que sorprende a casi todos los que llegan de otro lugar: los genéricos existen solo al compilar (se «borran» en la ejecución), y ya lo adelantó la lección 0.

Y queda la pregunta de cómo transformar una colección en otra: filtrar los lentos, convertir resultados en renglones de texto, contar por estado. Java lo hace con **streams**, una forma de describir la transformación como una cadena de pasos. Y como no todo servicio tiene un tiempo (el caído no respondió), también hay que decidir cómo se representa «no hay valor» sin volver a los `null` de la lección 2: para eso existe `Optional`.

El camino tiene dos sesiones. La **primera** cubre las secciones 4.1 y 4.2: las colecciones, las inmutables y los genéricos; al terminarla sabes guardar y proteger los servicios del `revisor`. La **segunda** cubre 4.3, 4.4 y 4.5: los *streams*, `Optional` y el reporte completo; al terminarla el programa imprime la tabla con su resumen. Este es el destino, para que tengas el punto de llegada a la vista:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

La tabla ya la conocías; lo nuevo es la línea del final, y que ahora sale de una lista de verdad y no de cuatro llamadas escritas a mano.

## Los conceptos

### 4.1 Colecciones

#### 4.1.1 Una lista: `List` y `ArrayList`

Una **lista** es una secuencia ordenada de valores donde cada uno tiene una posición, que en Java empieza en 0. En Java, `List` no es una clase sino una **interfaz**: un contrato que dice qué se puede hacer con una lista (agregar, buscar, quitar, recorrer) sin decir cómo se guarda por dentro. La implementación que vas a usar casi siempre se llama `ArrayList`, que guarda los valores en un arreglo interno que crece cuando se llena. El patrón de uso es declarar la variable con la interfaz y crear el objeto con la implementación:

```java
List<String> nombres = new ArrayList<>();
```

Esa línea tiene tres piezas que conviene separar. `List<String>` es el tipo de la variable: una lista de textos (lo que va entre los signos `<` y `>` es el tipo de sus elementos; la sección 4.2 explica por qué). `new ArrayList<>()` crea una lista vacía de la clase concreta. Y el `<>` vacío se llama **diamante**: le dice al compilador «el tipo de los elementos es el mismo que ya declaré a la izquierda, no me hagas repetirlo». ¿Por qué declarar la variable como `List` y no como `ArrayList`? Porque el resto del programa solo necesita el contrato. Si mañana cambias la implementación por otra, cambias una palabra en una línea y nada más. Es la misma razón por la que en la lección 3 `Respuesta` era una interfaz y no una clase.

Mira todo lo que se puede hacer con una lista en el programa `fig04_01`. Antes de leerlo, una precisión: es un archivo compacto, como los de la lección 2, y por eso no necesita escribir `import` para `List` ni `ArrayList`. Los archivos compactos importan solos todo el módulo `java.base`, que incluye `java.util`, donde viven las colecciones. Cuando una clase normal los necesite, como pasa en la sección 4.5, los importará con una línea al principio del archivo.

```java
// fig04_01.java
void main() {
  List<String> nombres = new ArrayList<>();
  nombres.add("catalogo");
  nombres.add("inventario");
  nombres.add("pagos");
  IO.println(nombres);
  IO.println("tamaño: " + nombres.size());
  IO.println("el segundo: " + nombres.get(1));
  IO.println("¿está pagos? " + nombres.contains("pagos"));
  IO.println("posición de pagos: " + nombres.indexOf("pagos"));
  IO.println("primero y último: " + nombres.getFirst() + ", " + nombres.getLast());

  nombres.add(1, "reportes");
  nombres.remove("pagos");
  nombres.set(0, "catalogo-v2");
  IO.println(nombres);

  for (String nombre : nombres) {
    IO.println("servicio: " + nombre);
  }
  for (int i = 0; i < nombres.size(); i++) {
    IO.println(i + " -> " + nombres.get(i));
  }
}
```

Corre el programa y compara con lo que predijiste:

```text
$ java fig04_01.java
[catalogo, inventario, pagos]
tamaño: 3
el segundo: inventario
¿está pagos? true
posición de pagos: 2
primero y último: catalogo, pagos
[catalogo-v2, reportes, inventario]
servicio: catalogo-v2
servicio: reportes
servicio: inventario
0 -> catalogo-v2
1 -> reportes
2 -> inventario
```

Tres detalles de esa salida. Primero, `println` sabe imprimir una lista completa: la escribe entre corchetes con los elementos separados por comas, porque `ArrayList` define su propia representación como texto. Segundo, `remove("pagos")` quitó el elemento cuyo valor es «pagos»; con textos no hay duda posible, pero `remove` tiene dos versiones (una que recibe un valor y otra que recibe una posición) y con listas de números se confunden. La sección «Lo que se hace mal» muestra la trampa. Tercero, `getFirst()` y `getLast()` son relativamente nuevos: llegaron en Java 21 con las colecciones secuenciadas ([JEP 431](https://openjdk.org/jeps/431)) y evitan el viejo `get(lista.size() - 1)` para el último elemento.

Las dos formas de recorrer una lista del programa merecen explicación, porque el curso no las había usado hasta hoy. El **`for` mejorado** (`for (String nombre : nombres)`) se lee «para cada `nombre` en `nombres`»: en cada vuelta, `nombre` vale el siguiente elemento, y no hay índice que llevar. Es la forma habitual. El **`for` clásico** (`for (int i = 0; i < nombres.size(); i++)`) lleva el índice a mano: empieza en 0 (`int i = 0`), repite mientras la condición sea cierta (`i < nombres.size()`) y avanza al final de cada vuelta (`i++`, que significa «suma uno a `i`»). Úsalo solo cuando necesitas la posición. Si te equivocas con ella, la lista te lo dice con una excepción: pedir `nombres.get(10)` en una lista de tres elementos lanza `IndexOutOfBoundsException: Index 10 out of bounds for length 3`, y el mensaje trae el índice y el tamaño, que es todo lo que necesitas para entender qué pasó.

Una última comparación con los lenguajes que ya conoces, porque la pregunta aparece siempre. Java también tiene arreglos (`String[]`, de tamaño fijo, que sirven para pocas cosas y que verás en la lección 5 y en la 6 al escribir `String[] args`), pero en el código de todos los días se usan listas: crecen, se buscan, se pasan a funciones y se combinan con *streams*. Lo que en Go es un *slice* y en Python una `list`, en Java es un `List<T>`.

#### 4.1.2 Conjuntos y mapas

Un **conjunto** (`Set`) guarda valores **sin repetir**: agregar uno que ya está no hace nada. Un **mapa** (`Map`) guarda **pares** de llave y valor, y busca el valor por la llave. El programa `fig04_02` usa los dos:

```java
// fig04_02.java
void main() {
  Set<String> vistos = new LinkedHashSet<>();
  IO.println(vistos.add("catalogo"));
  IO.println(vistos.add("pagos"));
  IO.println(vistos.add("catalogo"));
  IO.println(vistos + ", tamaño " + vistos.size());

  Map<String, Long> tiempos = new LinkedHashMap<>();
  tiempos.put("catalogo", 142L);
  tiempos.put("inventario", 2300L);
  tiempos.put("pagos", 87L);
  IO.println(tiempos);
  IO.println(tiempos.get("pagos"));
  IO.println(tiempos.get("reportes"));
  IO.println(tiempos.getOrDefault("reportes", 0L));
  IO.println(tiempos.containsKey("reportes"));
  IO.println(tiempos.put("pagos", 90L));
  IO.println(tiempos);

  for (Map.Entry<String, Long> par : tiempos.entrySet()) {
    IO.println(par.getKey() + " tardó " + par.getValue() + " ms");
  }

  Map<String, Long> ordenado = new TreeMap<>(tiempos);
  IO.println(ordenado);
}
```

```text
$ java fig04_02.java
true
true
false
[catalogo, pagos], tamaño 2
{catalogo=142, inventario=2300, pagos=87}
87
null
0
false
87
{catalogo=142, inventario=2300, pagos=90}
catalogo tardó 142 ms
inventario tardó 2300 ms
pagos tardó 90 ms
{catalogo=142, inventario=2300, pagos=90}
```

Lee la salida con calma, porque tiene varias lecciones. En el conjunto, `add` **devuelve un `boolean`**: `true` si el valor entró y `false` si ya estaba. Por eso las tres primeras líneas dicen `true`, `true`, `false`: es una forma barata de preguntar «¿ya lo había visto?», y la vas a usar en la lección 5 para detectar nombres repetidos en la configuración.

En el mapa, `put` guarda un par y **devuelve el valor anterior** de esa llave, o `null` si no había. En la salida, `put("pagos", 90L)` imprimió `87`, el valor que acababa de reemplazar. `get` busca por llave y devuelve `null` si la llave no existe: `tiempos.get("reportes")` imprimió `null`. Ese `null` es la misma fuente de errores de la lección 2, y por eso `getOrDefault("reportes", 0L)` existe: devuelve el valor por omisión que tú indicas cuando la llave falta. `containsKey` responde a la pregunta «¿está esta llave?» sin tocar el valor.

Para recorrer un mapa se recorre el conjunto de sus pares con `entrySet()`; cada par es un `Map.Entry`, del que `getKey()` da la llave y `getValue()` el valor. Verás que el tipo se escribe largo (`Map.Entry<String, Long>`); en el código real se acorta con `var`, que ya conoces de la lección 2: `for (var par : tiempos.entrySet())`.

La pregunta que sigue es la más importante de la sección: **¿en qué orden salen los elementos?** La respuesta depende de la implementación, y por eso Java tiene varias de cada interfaz:

| Implementación | Orden de recorrido | Cuándo usarla |
|---|---|---|
| `HashSet`, `HashMap` | ninguno que puedas prometer | cuando solo importa buscar rápido |
| `LinkedHashSet`, `LinkedHashMap` | el orden en que se insertaron | cuando el reporte debe salir siempre igual |
| `TreeSet`, `TreeMap` | ordenado por la llave | cuando necesitas el orden natural (alfabético, numérico) |

La figura usó `LinkedHashSet` y `LinkedHashMap` para que la salida sea siempre la que ves, y terminó copiando el mapa a un `TreeMap`, que lo reordenó alfabéticamente (aquí coincide con el orden de inserción porque los nombres ya estaban en orden). Para el `revisor` esto no es un detalle: el temario del curso promete un reporte «en el mismo orden siempre», y una tabla que sale en otro orden cada vez que corres el programa es un reporte inútil. La regla práctica es: **si el orden importa para quien lee el resultado, escoge una implementación que lo garantice**, y no confíes en que un `HashMap` «parece» ordenado en tu máquina. De hecho, las colecciones inmutables que ves en la siguiente sección (`Set.of`, `Map.of`) tienen un orden que **cambia de una ejecución a otra** a propósito: medí la misma línea, `Set.of("catalogo", "inventario", "pagos", "reportes")`, en tres ejecuciones seguidas del mismo programa y salió `[pagos, reportes, catalogo, inventario]`, luego `[catalogo, inventario, pagos, reportes]` y luego `[catalogo, reportes, pagos, inventario]`. La documentación lo avisa: [«the iteration order of set elements is unspecified and is subject to change»](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Set.html). El orden no es parte del contrato, y el JDK no se molesta en mantenerlo estable.

Queda una pregunta: ¿cómo sabe un `Set` que dos valores son «el mismo»? Usa dos métodos: `equals`, que ya conoces, y `hashCode`, que devuelve un número entero que resume el contenido del objeto (dos objetos iguales deben dar el mismo número, y eso le permite al conjunto buscar sin comparar con todos). Con textos y números funciona sin que hagas nada. Con tus propios tipos, depende de cómo estén definidos, y aquí los *records* de la lección 3 pagan lo que prometieron: generan `equals` y `hashCode` a partir de todos sus componentes. El programa `fig04_03` lo muestra: dos `Direccion` con el mismo servidor y el mismo puerto son iguales aunque sean dos objetos distintos, y por eso el conjunto no repite y el mapa encuentra el valor con una llave recién construida.

```java
// fig04_03.java
record Direccion(String servidor, int puerto) {}

void main() {
  Set<Direccion> direcciones = new LinkedHashSet<>();
  direcciones.add(new Direccion("catalogo.example", 443));
  direcciones.add(new Direccion("pagos.example", 443));
  direcciones.add(new Direccion("catalogo.example", 443));
  IO.println(direcciones.size());
  IO.println(direcciones);

  Map<Direccion, String> duenos = new HashMap<>();
  duenos.put(new Direccion("pagos.example", 443), "equipo de cobranza");
  IO.println(duenos.get(new Direccion("pagos.example", 443)));
  IO.println(duenos.get(new Direccion("pagos.example", 8443)));
}
```

```text
$ java fig04_03.java
2
[Direccion[servidor=catalogo.example, puerto=443], Direccion[servidor=pagos.example, puerto=443]]
equipo de cobranza
null
```

El conjunto terminó con dos elementos y no tres, el mapa encontró al «equipo de cobranza» con una llave nueva, y la llave con otro puerto (`8443`) no existe, así que `get` devolvió `null`. Si en lugar de un *record* hubieras escrito una clase normal sin definir `equals` y `hashCode`, cada objeto sería igual solo a sí mismo, y el conjunto guardaría los tres. Esa es una de las razones por las que el curso prefiere *records* para los datos.

#### 4.1.3 Colecciones inmutables

Hasta aquí las colecciones se podían modificar: agregar, quitar, reemplazar. Muchas veces eso es justo lo que no quieres. La lista de servicios que lee el `revisor` no debería cambiar mientras alguien más la está recorriendo, y un reporte que ya se calculó no debería poder alterarse después. Java resuelve esto con las **colecciones inmutables**, que se crean con tres métodos: `List.of(...)`, `Set.of(...)` y `Map.of(...)`. (Se llaman métodos de fábrica estáticos: no necesitas `new`, solo el nombre de la interfaz, un punto y los valores.) Mira qué pasa cuando intentas modificar una:

```java
// fig04_04.java
void main() {
  List<String> nombres = List.of("catalogo", "inventario", "pagos");
  IO.println(nombres);
  nombres.add("reportes");
  IO.println(nombres);
}
```

```text
$ java fig04_04.java
[catalogo, inventario, pagos]
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_04.main(fig04_04.java:5)
```

El programa imprime la lista una vez, y el `add` lanza `java.lang.UnsupportedOperationException` («operación no soportada»). Es una excepción sin mensaje, y no lo necesita: la operación pedida simplemente no existe en esa lista. La traza se lee de **arriba hacia abajo**, al revés que la de Python: el primer renglón dice qué se lanzó, el segundo dónde nació dentro del JDK (`ImmutableCollections.uoe`, «unsupported operation exception»), el tercero qué método llamaste (`add`) y el último, `fig04_04.main(fig04_04.java:5)`, qué línea de **tu** programa lo hizo. Siempre busca el primer renglón que menciona tu archivo: ahí está tu error. (Los números `159` y `164` son de este JDK y cambiarán en el tuyo.)

La inmutabilidad de estas colecciones es real y completa en lo que respecta a la colección: no se pueden agregar, quitar ni reemplazar elementos. Los autores del JDK agregaron otras reglas, que aparecen en la documentación de [`List`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html) y que verifiqué al escribir esta lección:

- **No admiten `null`.** `List.of("a", null)` lanza `NullPointerException` al crearse. Es una decisión a favor de menos sorpresas: un `null` dentro de una colección es casi siempre un error.
- **`Set.of` y `Map.of` rechazan los repetidos.** `Map.of("a", 1, "a", 2)` lanza `IllegalArgumentException: duplicate key: a`, y `Set.of("a", "a")` lanza `IllegalArgumentException: duplicate element: a`. Una llave repetida en un mapa de datos fijos es casi seguro un descuido, y se descubre al arrancar, no cuando alguien consulta.
- **Su orden no es estable entre ejecuciones**, como se midió arriba.
- **`Map.of` acepta hasta diez pares.** Para más, existe `Map.ofEntries`.

Dos aclaraciones para no confundir lo que la inmutabilidad promete. La primera: **inmutable no significa que los elementos sean inmutables**. Una `List.of(new ArrayList<>(), new ArrayList<>())` no deja agregar ni quitar listas, pero cada lista de adentro se sigue pudiendo modificar. Con *records* de componentes simples (textos, números, otros *records*), como los del `revisor`, el problema no existe; con elementos mutables, sí. La segunda: la lección 3 ya mencionó la **inmutabilidad superficial** de los *records* (un componente arreglo sigue siendo mutable), y esto es lo mismo con otro nombre: el *record* no puede cambiar de lista, pero si la lista que guarda es mutable, la lista sí cambia. Eso lleva a la siguiente sección.

#### 4.1.4 Copias defensivas

Aquí hay una trampa que se aprende mejor viéndola. Mira el programa `fig04_05`. Define dos *records* con la misma forma: `ReporteIngenuo`, que guarda la lista tal como se la dan, y `ReporteSeguro`, que en su constructor compacto (el de la lección 3) reemplaza la lista por `List.copyOf(nombres)`, una **copia inmutable**. Luego les pasa la misma lista y la modifica después:

```java
// fig04_05.java
record ReporteIngenuo(List<String> nombres) {}

record ReporteSeguro(List<String> nombres) {
  ReporteSeguro {
    nombres = List.copyOf(nombres);
  }
}

void main() {
  List<String> nombres = new ArrayList<>(List.of("catalogo", "pagos"));
  ReporteIngenuo ingenuo = new ReporteIngenuo(nombres);
  ReporteSeguro seguro = new ReporteSeguro(nombres);
  nombres.add("reportes");
  IO.println("ingenuo: " + ingenuo);
  IO.println("seguro:  " + seguro);

  List<String> vista = Collections.unmodifiableList(nombres);
  List<String> copia = List.copyOf(nombres);
  nombres.add("facturacion");
  IO.println("vista: " + vista);
  IO.println("copia: " + copia);
}
```

```text
$ java fig04_05.java
ingenuo: ReporteIngenuo[nombres=[catalogo, pagos, reportes]]
seguro:  ReporteSeguro[nombres=[catalogo, pagos]]
vista: [catalogo, pagos, reportes, facturacion]
copia: [catalogo, pagos, reportes]
```

El reporte ingenuo cambió solo, sin que nadie lo tocara: guardó una **referencia** a la lista original, no una copia, y cuando el dueño original agregó `reportes`, el «reporte» ya calculado se alteró. El seguro conservó los dos nombres que tenía en el momento de crearse. Esto es una **copia defensiva**: el constructor se defiende de que alguien más siga teniendo una referencia a los datos que ahora son suyos. Es una de esas cosas que no fallan: devuelven un número equivocado una semana después.

Las dos últimas líneas de la figura muestran otra diferencia que confunde. `Collections.unmodifiableList(nombres)` no copia nada: es una **vista** que no permite modificar *a través de ella*, pero que refleja todo lo que el dueño original cambie (por eso `vista` terminó con `facturacion`). `List.copyOf(nombres)` sí hace una copia independiente (por eso `copia` no la tiene). Si lo que quieres es que nadie pueda alterar tus datos, necesitas la copia; la vista solo le impide *escribir* a quien la recibe.

Esta técnica no es un adorno de estilo. Lo que acabas de ver es exactamente la razón por la que el `Reporte` de la sección 4.5 lleva `resultados = List.copyOf(resultados);` en su constructor. Y no es una manía de este curso: los analizadores de código conocen el hueco. En la lección 10 conocerás SpotBugs, una herramienta que revisa el programa compilado buscando defectos probables, y que llama a este patrón `EI_EXPOSE_REP` y `EI_EXPOSE_REP2` (de *exposing internal representation*, «exponer la representación interna»). Con una condición que conviene saber desde ahora, porque se midió: SpotBugs solo lo reporta en las clases que se ven desde fuera de su paquete. El `Reporte` del `revisor` no es público, y si le quitas la copia la herramienta se queda callada. La copia no está para contentar a una herramienta: está porque la lista es del *record*, y lo que la herramienta no revisa no deja de ser un defecto.

Una nota de eficiencia: `List.copyOf` es inteligente. Si la lista que le pasas ya es inmutable (por ejemplo, la que creó `List.of`), no la copia, devuelve la misma. Tiene sentido: no hay nada que proteger. La documentación lo dice con la frase «if the given Collection is an unmodifiable List, calling copyOf will generally not create a copy».

### 4.2 Genéricos

#### 4.2.1 Qué son y qué te dan

Cada vez que escribiste `List<String>` o `Map<String, Long>` usaste **genéricos**: una clase o interfaz que está definida para «algún tipo», y que tú concretas al usarla. `List<E>` se define una sola vez, con un tipo `E` por llenar, y tú decides si es una lista de `String`, de `Integer` o de `Servicio`. Lo que ganas es verificación **al compilar**: el compilador sabe qué tipo de elemento tiene la lista y rechaza lo que no cabe. Veamos qué dice cuando intentas meter un texto en una lista de números:

```java
// fig04_06.java
void main() {
  List<Integer> tiempos = new ArrayList<>();
  tiempos.add(142);
  tiempos.add("87 ms");
  IO.println(tiempos);
}
```

```text
$ java fig04_06.java
fig04_06.java:5: error: no suitable method found for add(String)
  tiempos.add("87 ms");
         ^
    method List.add(Integer) is not applicable
      (argument mismatch; String cannot be converted to Integer)
    method List.add(int,Integer) is not applicable
      (actual and formal argument lists differ in length)
1 error
error: compilation failed
```

El mensaje es más largo de lo que esperarías. El compilador dice `no suitable method found for add(String)` («no encontré un método `add` que sirva para un `String`») porque `List` tiene dos métodos llamados `add`: uno que recibe un elemento (`add(Integer)`) y otro que recibe una posición y un elemento (`add(int, Integer)`). Los intentó los dos y explica por qué ninguno sirve: el primero porque `String` no se convierte en `Integer` (*argument mismatch*, «el argumento no coincide»), el segundo porque ni siquiera tiene la misma cantidad de parámetros. Lee siempre la **línea entre paréntesis**: ahí está la causa real. Si en cambio intentas sacar un valor y asignarlo a la variable equivocada (`String texto = tiempos.get(0);`) el mensaje es el más simple: `incompatible types: Integer cannot be converted to String`.

Sin genéricos, ninguno de los dos errores existiría al compilar: la lista aceptaría cualquier cosa, y el primer aviso llegaría en la ejecución, lejos de donde se cometió el error. Es la diferencia entre un lenguaje que te protege y uno que te deja escribir de todo. (La sección 4.2.3 muestra qué pasa cuando alguien se salta la protección a propósito.)

#### 4.2.2 Los genéricos solo aceptan referencias: los envoltorios

Escribe `List<int>` y mira qué dice el compilador:

```java
// fig04_07.java
void main() {
  List<int> tiempos = new ArrayList<>();
  tiempos.add(142);
  IO.println(tiempos);
}
```

```text
$ java fig04_07.java
fig04_07.java:3: error: unexpected type
  List<int> tiempos = new ArrayList<>();
       ^
  required: reference
  found:    int
1 error
error: compilation failed
```

El mensaje es `unexpected type; required: reference; found: int`: se pedía una referencia y se encontró un primitivo. Los genéricos solo trabajan con tipos por referencia, y `int` es un primitivo (lección 2). La solución es usar el **envoltorio** del primitivo: `Integer` para `int`, `Long` para `long`, `Double` para `double`, `Boolean` para `boolean`. Son los tipos que ya conoces de la lección 2 y que ahora tienen su razón de ser. Java convierte entre un primitivo y su envoltorio solo, en las dos direcciones, cuando hace falta: eso se llama ***autoboxing*** (meter el `142` en un `Integer`) y *unboxing* (sacarlo). Es cómodo, pero tiene dos trampas que ya conoces por otro nombre, y valen una figura cada una.

La primera: `==` entre envoltorios compara **referencias**, como con los `String`. El programa `fig04_08` lo mide:

```java
// fig04_08.java
void main() {
  List<Integer> tiempos = List.of(87, 87, 1000, 1000);
  IO.println(tiempos.get(0) == tiempos.get(1));
  IO.println(tiempos.get(2) == tiempos.get(3));
  IO.println(tiempos.get(2).equals(tiempos.get(3)));
}
```

```text
$ java fig04_08.java
true
false
true
```

Los dos `87` son «el mismo» objeto y los dos `1000` no. La razón es que el *autoboxing* usa `Integer.valueOf`, y [su documentación](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Integer.html) promete que «siempre guarda en caché los valores de −128 a 127, y puede guardar otros»: para los pequeños reutiliza el mismo objeto, y para el `1000` de esta figura, en la práctica, crea uno nuevo cada vez. Es un comportamiento documentado, no un error, y por eso es peligroso: un programa que compara con `==` funciona con los tiempos chicos de las pruebas y falla con los de producción. La regla es la de la lección 2, extendida: **compara valores con `equals`**, nunca con `==`, siempre que el tipo sea una referencia, y los envoltorios lo son.

La segunda trampa es el *unboxing* de un valor que no está. El programa `fig04_09` pide a un `Map<String, Long>` un tiempo y lo guarda en una variable `long`:

```java
// fig04_09.java
void main() {
  Map<String, Long> tiempos = Map.of("catalogo", 142L, "pagos", 87L);
  long tiempoPagos = tiempos.get("pagos");
  IO.println("pagos: " + tiempoPagos);
  long tiempoReportes = tiempos.get("reportes");
  IO.println("reportes: " + tiempoReportes);
}
```

```text
$ java fig04_09.java
pagos: 87
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Long.longValue()" because the return value of "java.util.Map.get(Object)" is null
	at fig04_09.main(fig04_09.java:6)
```

La primera búsqueda funciona. La segunda falla con un `NullPointerException` cuyo mensaje es una joya de diagnóstico: `Cannot invoke "java.lang.Long.longValue()" because the return value of "java.util.Map.get(Object)" is null`. Se lee así: «no pude llamar a `longValue()` porque lo que devolvió `Map.get` es `null`». Nadie escribió `longValue()`: lo insertó el compilador al convertir el `Long` en `long`, y falló porque `get("reportes")` devolvió `null` (la llave no existía). Los mensajes de `NullPointerException` te dicen siempre **qué** era nulo, y en este caso incluso qué método lo devolvió. Dos formas de evitar el error: pedir `getOrDefault("reportes", 0L)`, o guardar el resultado en un `Long` (no `long`) y preguntar `if (tiempo != null)`. La sección 4.4 trae una tercera, mejor.

#### 4.2.3 El borrado de tipos

La lección 0 ya lo adelantó: **los genéricos se borran**. El compilador usa `List<String>` para verificar tu código, y después de verificarlo quita los tipos: en el *bytecode*, una `List<String>` y una `List<Integer>` son la misma `List`. Así quedaron desde Java 5: el borrado asegura que no se crea una clase nueva por cada tipo parametrizado, de modo que los genéricos [no cuestan nada en la ejecución](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html), y el código escrito antes de que existieran (casi todas las colecciones lo eran) siguió funcionando. El programa `fig04_10` lo confirma y muestra lo que cuesta:

```java
// fig04_10.java
void main() {
  List<String> nombres = new ArrayList<>();
  List<Integer> tiempos = new ArrayList<>();
  IO.println(nombres.getClass() == tiempos.getClass());
  IO.println(nombres.getClass().getName());

  List crudo = nombres;
  crudo.add(142);
  IO.println("agregué un número a una lista de String y nadie se quejó");
  String primero = nombres.get(0);
  IO.println(primero);
}
```

```text
$ java fig04_10.java
fig04_10.java:9: warning: [unchecked] unchecked call to add(E) as a member of the raw type List
  crudo.add(142);
           ^
  where E is a type-variable:
    E extends Object declared in interface List
1 warning
true
java.util.ArrayList
agregué un número a una lista de String y nadie se quejó
Exception in thread "main" java.lang.ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String (java.lang.Integer and java.lang.String are in module java.base of loader 'bootstrap')
	at fig04_10.main(fig04_10.java:11)
```

Hay varias cosas que leer aquí. El compilador imprimió un aviso (`warning: [unchecked] unchecked call to add(E) as a member of the raw type List`) antes de ejecutar el programa: te avisa que en la línea 9 hiciste algo que él no puede verificar. La causa está en la línea 8: `List crudo = nombres;`. Una `List` sin el `<...>` es un **tipo crudo** (*raw type*), el mismo tipo de antes de Java 5. El compilador acepta guardar en él una lista de `String` y acepta agregarle un `142`, porque con el tipo crudo no verifica nada, solo avisa. Después, el programa imprime «agregué un número a una lista de String y nadie se quejó» y falla en la **línea 11**, no en la 9:

`ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String`

El error apareció al *sacar* el elemento, dos líneas después del que lo metió. En un programa real podrían ser dos clases diferentes y diez minutos de depuración. Es la consecuencia de que los genéricos se verifiquen solo al compilar: el compilador pudo prometerte que la lista era de `String` únicamente mientras nadie se brincara el sistema de tipos, y el tipo crudo es la brecha. La regla: **nunca uses tipos crudos en código nuevo**; si ves un aviso `[unchecked]`, es el compilador diciéndote que arregles algo.

Las dos primeras líneas de la salida son el otro lado del borrado. `nombres.getClass() == tiempos.getClass()` es `true` y el nombre de la clase es `java.util.ArrayList`, sin rastro del `<String>`: en tiempo de ejecución, la lista no sabe de qué es. De ahí salen tres límites que vas a encontrar en la práctica, y que conviene saber desde ahora para no perder una hora buscándolos: no puedes escribir `new T()` dentro de un método genérico, no puedes preguntar `if (lista instanceof List<String>)` (solo `List<?>`), y no puedes sobrecargar dos métodos que se distingan únicamente por el tipo genérico (`void f(List<String>)` y `void f(List<Integer>)` chocan porque, borrados, son el mismo método). El [tutorial oficial de Java sobre el borrado](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html) lista las reglas completas.

#### 4.2.4 Escribir un método genérico y leer una firma

Casi nunca vas a *definir* una clase genérica, pero sí vas a leer las firmas de las del JDK, y de vez en cuando escribirás un método genérico. Mira uno en `fig04_11`:

```java
// fig04_11.java
static <T> T primeroO(List<T> lista, T porOmision) {
  if (lista.isEmpty()) {
    return porOmision;
  }
  return lista.get(0);
}

void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<Long> tiempos = List.of();
  String primerNombre = primeroO(nombres, "ninguno");
  Long primerTiempo = primeroO(tiempos, 0L);
  IO.println(primerNombre);
  IO.println(primerTiempo);
}
```

```text
$ java fig04_11.java
catalogo
0
```

El `<T>` antes del tipo de retorno declara una **variable de tipo**: «en este método, `T` es un tipo por determinar». El compilador lo deduce de los argumentos: en la primera llamada `T` es `String` y en la segunda `Long`, y por eso el resultado se puede guardar en un `String` y en un `Long` sin ninguna conversión. Escribirlo con `Object` en lugar de `T` te obligaría a convertir a mano.

Y para leer la documentación, un símbolo más: `?`, el **comodín**. Verás firmas como `List.copyOf(Collection<? extends E> coll)`. Se lee «una colección de *algún tipo* que sea `E` o una subclase de `E`». La palabra `extends` aquí significa «es `E` o hereda de él», y el comodín permite que le pases, por ejemplo, una lista de `Respondio` donde se espera una colección de `Respuesta`. No necesitas escribir comodines todavía; necesitas poder leerlos sin asustarte, porque aparecen en `Collectors`, en `Comparator` y en casi toda la biblioteca de *streams* que sigue.

Con esto termina la primera sesión. Antes de seguir, comprueba que puedes responder sin mirar: ¿qué diferencia hay entre `List.copyOf` y `Collections.unmodifiableList`? ¿Por qué `Set.of` imprime en otro orden cada vez? ¿Qué falla al compilar con `List<int>` y qué falla al ejecutar con una lista cruda? Si dudas en alguna, vuelve a su figura antes de abrir la sesión dos.

### 4.3 Streams

Empieza la segunda sesión. Hasta ahora guardaste valores; ahora vas a transformarlos. La pregunta concreta del `revisor` es: dada la lista de resultados, ¿cuántos están arriba, cuántos lentos, cuántos caídos, y cuál es el promedio de los tiempos de los que respondieron? Con lo que sabes puedes escribir un `for` con cuatro contadores. Los *streams* son la otra respuesta, y para entenderlos primero hay que entender la herramienta con la que se escriben: las lambdas.

#### 4.3.1 Lambdas y referencias a método

Una **lambda** es una función pequeña, sin nombre, que escribes en el lugar donde la necesitas y pasas como si fuera un valor. Su forma es `parámetros -> cuerpo`. Ya viste la flecha en el `switch` de la lección 2, pero aquí significa otra cosa: no «si el caso coincide, devuelve esto», sino «dado este parámetro, calcula esto». Estas son las formas que existen:

- `medicion -> medicion.tiempoMs() >= 1000`: un parámetro, un cuerpo que es una expresión (el resultado es el valor de la expresión).
- `(a, b) -> a + b`: dos parámetros, entre paréntesis.
- `() -> "revisando..."`: ninguno, con paréntesis vacíos.
- `medicion -> { ...varias líneas...; return valor; }`: un bloque entre llaves, con `return` si debe devolver algo.

¿Y cuál es el *tipo* de una lambda? En Java, una lambda siempre se asigna a una **interfaz funcional**: una interfaz con un único método abstracto. La lambda es la implementación de ese método. El JDK trae varias que se usan sin parar: `Predicate<T>` (un método, `test`, que recibe un `T` y devuelve `boolean`), `Function<T, R>` (`apply`, recibe un `T`, devuelve un `R`), `Supplier<T>` (`get`, no recibe nada y devuelve un `T`) y `Comparator<T>` (`compare`, que recibe dos y devuelve un entero). El programa `fig04_12` las usa una por una:

```java
// fig04_12.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  Predicate<Medicion> esLenta = medicion -> medicion.tiempoMs() >= 1000;
  Function<Medicion, String> etiqueta = medicion -> medicion.nombre() + ":" + medicion.tiempoMs();
  Function<Medicion, String> soloNombre = Medicion::nombre;
  Supplier<String> saludo = () -> "revisando...";

  Medicion inventario = new Medicion("inventario", 2300);
  Medicion pagos = new Medicion("pagos", 87);
  IO.println(esLenta.test(inventario));
  IO.println(esLenta.test(pagos));
  IO.println(etiqueta.apply(pagos));
  IO.println(soloNombre.apply(inventario));
  IO.println(saludo.get());
}
```

```text
$ java fig04_12.java
true
false
pagos:87
inventario
revisando...
```

`esLenta` es un `Predicate<Medicion>`: se le pasa una medición con `test` y contesta `true` o `false`. `etiqueta` es una `Function<Medicion, String>`: convierte una medición en un texto. `saludo` es un `Supplier<String>`: no recibe nada y produce un valor. Cuando escribes una lambda dentro de un *stream* no nombras estos tipos; el compilador los deduce del método al que se la pasas. Pero conocerlos es lo que te permite leer la documentación: cuando `filter` pide un `Predicate`, está pidiendo «una función que recibe un elemento y contesta sí o no».

`soloNombre` muestra la segunda herramienta: la **referencia a método**. `Medicion::nombre` es la forma corta de `medicion -> medicion.nombre()`: «llama al método `nombre` del elemento que te den». Se lee mejor cuando la lambda solo llama a un método, y la vas a ver por todas partes (`Resultado::estado`, `Revision::revisar`, `Formato::linea`). Usa la que se lea más claro; el compilador las trata igual.

Hay una regla de las lambdas que sorprende a quien llega de JavaScript o de Python. Mira este programa, que intenta sumar los tiempos con una lambda que modifica una variable de fuera:

```java
// fig04_13.java
void main() {
  List<Long> tiempos = List.of(142L, 2300L, 87L);
  long suma = 0;
  tiempos.forEach(tiempo -> suma += tiempo);
  IO.println(suma);
}
```

```text
$ java fig04_13.java
fig04_13.java:5: error: local variables referenced from a lambda expression must be final or effectively final
  tiempos.forEach(tiempo -> suma += tiempo);
                            ^
1 error
error: compilation failed
```

El error dice `local variables referenced from a lambda expression must be final or effectively final`: una lambda solo puede usar variables locales de fuera si **nunca se reasignan** (son *efectivamente finales*). Aquí `suma` cambia en cada vuelta, y el compilador lo rechaza. La razón es que una lambda puede ejecutarse más tarde, o en otro hilo, y Java no quiere que la lambda y el código de afuera compartan una variable que cambia. En la lección 9 verás por qué es una protección valiosa. La salida de este problema no es esquivar la regla sino usar la herramienta correcta: `mapToLong(...).sum()`, que es justo lo que viene en la siguiente sección. Si te descubres escribiendo una lambda que modifica algo de afuera, es casi siempre una señal de que le falta un `collect` o un `sum`.

#### 4.3.2 Un *stream* es una tubería

Un ***stream*** (flujo) no es una colección: es una **descripción de una secuencia de pasos** que se aplican a los elementos de una fuente. Tiene tres partes. Una **fuente**, que casi siempre es una colección y se obtiene con `.stream()`. Cero o más **operaciones intermedias** (`filter`, `map`, `sorted`...), que transforman el flujo en otro flujo. Y una **operación terminal** (`toList`, `count`, `sum`, `collect`...), que produce el resultado final y es la que de verdad pone todo en marcha. Los pasos se escriben encadenados, uno por renglón, y se leen de arriba hacia abajo como una receta. Mira el programa `fig04_14`, que hace el mismo trabajo dos veces: primero con un bucle y luego con un *stream*.

```java
// fig04_14.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", 142),
          new Medicion("inventario", 2300),
          new Medicion("pagos", 87));

  List<String> conBucle = new ArrayList<>();
  for (Medicion medicion : mediciones) {
    if (medicion.tiempoMs() >= 100) {
      conBucle.add(medicion.nombre() + ":" + medicion.tiempoMs());
    }
  }

  List<String> conStream =
      mediciones.stream()
          .filter(medicion -> medicion.tiempoMs() >= 100)
          .map(medicion -> medicion.nombre() + ":" + medicion.tiempoMs())
          .toList();

  IO.println(conBucle);
  IO.println(conStream);
  IO.println(conBucle.equals(conStream));

  List<String> nombres = mediciones.stream().map(Medicion::nombre).toList();
  long suma = mediciones.stream().mapToLong(Medicion::tiempoMs).sum();
  long rapidos = mediciones.stream().filter(medicion -> medicion.tiempoMs() < 100).count();
  IO.println(nombres);
  IO.println("suma: " + suma + " ms");
  IO.println("rápidos: " + rapidos);
}
```

```text
$ java fig04_14.java
[catalogo:142, inventario:2300]
[catalogo:142, inventario:2300]
true
[catalogo, inventario, pagos]
suma: 2529 ms
rápidos: 1
```

El bucle y el *stream* producen la misma lista (la última línea comprueba con `equals` que son iguales). El *stream* lee así: «de las mediciones, **quédate** con las que cumplen `tiempoMs() >= 100` (`filter`), **convierte** cada una en un texto (`map`) y **junta** el resultado en una lista (`toList`)». Fíjate en lo que no hay: no hay una lista vacía que se llena, ni una condición envuelta en un `if`, ni una variable que cambia. Quien lee describe qué quiere, no cómo hacerlo.

Las operaciones que vas a usar con más frecuencia:

| Operación | Tipo | Qué hace |
|---|---|---|
| `filter(Predicate)` | intermedia | deja pasar solo los elementos que cumplen la condición |
| `map(Function)` | intermedia | transforma cada elemento en otro valor |
| `mapToLong(...)` | intermedia | transforma cada elemento en un `long` primitivo (sin envoltorios) |
| `sorted(Comparator)` | intermedia | ordena (sección 4.3.5) |
| `toList()` | terminal | junta los elementos en una lista **inmutable** |
| `count()` | terminal | cuenta los elementos |
| `sum()`, `average()`, `max()` | terminal | agregan; existen sobre los flujos de primitivos |
| `collect(Collector)` | terminal | junta los elementos con una receta (sección 4.3.4) |

Dos precisiones sobre la tabla. `mapToLong` produce un flujo especializado (`LongStream`) que sabe sumar y promediar sin envolver cada número en un `Long`; existen sus hermanos `IntStream` y `DoubleStream`. Y `toList()` devuelve una lista **inmutable**, como `List.of`: si más adelante la necesitas modificable, la copias con `new ArrayList<>(lista)`. La sección «El error que vas a ver» mostrará la excepción que sale cuando se olvida.

#### 4.3.3 Perezosos y de un solo uso

Hay dos propiedades de los *streams* que no se adivinan y que producen errores si las ignoras. La primera es que son **perezosos**: armar el flujo no hace nada. Según la [documentación del paquete `java.util.stream`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html), las operaciones intermedias «son siempre perezosas»: `filter` no filtra nada cuando la llamas, solo crea un nuevo flujo que filtrará cuando alguien lo recorra. El trabajo ocurre cuando llega la operación terminal. El programa `fig04_15` lo hace visible con `peek`, una operación que deja pasar cada elemento y de paso ejecuta una acción (su documentación dice que existe «principalmente para depurar», y solo para eso la usamos aquí):

```java
// fig04_15.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", 142),
          new Medicion("inventario", 2300),
          new Medicion("pagos", 87));

  Stream<String> armado =
      mediciones.stream()
          .peek(medicion -> IO.println("miro " + medicion.nombre()))
          .filter(medicion -> medicion.tiempoMs() >= 100)
          .peek(medicion -> IO.println("  pasa " + medicion.nombre()))
          .map(Medicion::nombre);
  IO.println("el stream está armado; todavía no pasó nada");

  List<String> resultado = armado.toList();
  IO.println(resultado);
}
```

```text
$ java fig04_15.java
el stream está armado; todavía no pasó nada
miro catalogo
  pasa catalogo
miro inventario
  pasa inventario
miro pagos
[catalogo, inventario]
```

Mira el orden. «el stream está armado; todavía no pasó nada» sale **primero**, antes de cualquier «miro». Los `peek` y el `filter` no ejecutaron una sola vez cuando se armó el flujo: esperaron a `toList()`. Y cuando por fin corren, no lo hacen paso por paso sobre toda la lista, sino **elemento por elemento**: `catalogo` pasa por `peek`, `filter`, `peek` y `map` antes de que `inventario` entre al flujo, y `pagos`, que no cumple el filtro, se queda en el primer `peek`. Esto tiene consecuencias prácticas: un flujo sin operación terminal es código muerto (el compilador no lo avisa), y una operación intermedia con un efecto secundario, como imprimir, se ejecuta en un orden que no es el de «primero todos, luego todos».

La segunda propiedad es que un flujo se puede recorrer **una sola vez**. La [documentación de `Stream`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Stream.html) dice que «un flujo debe operarse solo una vez». Lo que pasa si lo ignoras:

```java
// fig04_16.java
void main() {
  Stream<String> nombres = Stream.of("catalogo", "inventario", "pagos");
  IO.println(nombres.count());
  IO.println(nombres.count());
}
```

```text
$ java fig04_16.java
3
Exception in thread "main" java.lang.IllegalStateException: stream has already been operated upon or closed
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:260)
	at java.base/java.util.stream.ReferencePipeline.count(ReferencePipeline.java:750)
	at fig04_16.main(fig04_16.java:5)
```

La primera llamada a `count()` funciona (3) y consume el flujo. La segunda lanza `IllegalStateException: stream has already been operated upon or closed` («el flujo ya fue operado o cerrado»). El arreglo es crear un flujo nuevo cada vez que lo necesites, desde la colección: `nombres.stream()` es barato. Por eso las variables del curso guardan la **lista**, y el flujo se arma y se consume en la misma expresión.

#### 4.3.4 `Collectors`: juntar el resultado de formas distintas

`toList()` cubre un caso. Para los demás está `collect`, que recibe un **colector** (*collector*): una receta para juntar los elementos. La clase `Collectors` trae las recetas más comunes. El programa `fig04_17` usa las que necesita el resumen del `revisor`:

```java
// fig04_17.java
record Medicion(String nombre, String estado, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", "ARRIBA", 142),
          new Medicion("inventario", "LENTO", 2300),
          new Medicion("pagos", "ARRIBA", 87),
          new Medicion("reportes", "CAIDO", 0));

  Map<String, Long> porEstado =
      mediciones.stream()
          .collect(Collectors.groupingBy(Medicion::estado, TreeMap::new, Collectors.counting()));
  IO.println(porEstado);

  Map<String, List<String>> nombresPorEstado =
      mediciones.stream()
          .collect(
              Collectors.groupingBy(
                  Medicion::estado,
                  TreeMap::new,
                  Collectors.mapping(Medicion::nombre, Collectors.toList())));
  IO.println(nombresPorEstado);

  String todos = mediciones.stream().map(Medicion::nombre).collect(Collectors.joining(", "));
  IO.println(todos);

  OptionalDouble promedio =
      mediciones.stream()
          .filter(medicion -> !medicion.estado().equals("CAIDO"))
          .mapToLong(Medicion::tiempoMs)
          .average();
  IO.println(promedio);
  IO.println(Math.round(promedio.getAsDouble()));

  OptionalDouble sinDatos =
      mediciones.stream()
          .filter(medicion -> medicion.tiempoMs() > 9999)
          .mapToLong(Medicion::tiempoMs)
          .average();
  IO.println(sinDatos);
}
```

```text
$ java fig04_17.java
{ARRIBA=2, CAIDO=1, LENTO=1}
{ARRIBA=[catalogo, pagos], CAIDO=[reportes], LENTO=[inventario]}
catalogo, inventario, pagos, reportes
OptionalDouble[843.0]
843
OptionalDouble.empty
```

La primera es la más importante: `groupingBy(Medicion::estado, TreeMap::new, Collectors.counting())`. `groupingBy` **agrupa** los elementos por el valor que devuelve la función (aquí, el estado) y a cada grupo le aplica otro colector (aquí, `counting()`, que cuenta). El resultado es un mapa de estado a cantidad: `{ARRIBA=2, CAIDO=1, LENTO=1}`. Sin el segundo argumento, `groupingBy` devolvería un `HashMap` de orden no prometido; con `TreeMap::new` (una referencia a constructor: «llama a `new TreeMap()` cuando necesites crear el mapa») el resultado sale ordenado alfabéticamente. Eso se parece a la columna de «cuántos hay de cada estado» del resumen del `revisor`.

La segunda receta agrupa lo mismo pero, en lugar de contar, **guarda los nombres** de cada grupo: `Collectors.mapping(Medicion::nombre, Collectors.toList())` transforma cada elemento antes de juntarlo. Es el patrón «agrupa y luego reduce cada grupo» que verás con distintos reductores. `Collectors.joining(", ")` junta textos con un separador; es lo que arma el cuerpo de la tabla en la sección 4.5.

La última parte de la figura es la que más enseña. `average()` calcula el promedio de un flujo de `long` y devuelve un `OptionalDouble`, no un `double`. Se imprime como `OptionalDouble[843.0]`, y `getAsDouble()` saca el número. Cuando el flujo está vacío (el último caso, donde el filtro no deja pasar a nadie), el promedio **no existe** y el resultado es `OptionalDouble.empty`. Es una decisión inteligente: otros lenguajes devolverían cero o `NaN` (un número que significa «no es un número»), y un cero mentiría, porque el promedio de nada no es cero. Java te obliga a decidir qué hacer cuando no hay datos. La sección 4.4 explica `Optional` por completo.

Un detalle de `Math.round(843.0)`: redondea un `double` al entero más cercano y devuelve un `long`. (Si quieres el promedio sin redondear, deja el `double`.) Para el `revisor`, que muestra milisegundos enteros, el redondeo es lo correcto.

#### 4.3.5 Ordenar: `Comparator`

Ordenar requiere saber **comparar** dos elementos. Para los textos y los números hay un orden natural (el alfabético, el numérico) y `sorted()` sin argumentos lo usa. Para tus propios tipos, tú dices cómo comparar con un `Comparator`, y el JDK trae una caja de herramientas para armarlos sin escribir el método `compare` a mano. El programa `fig04_18` las ejercita con cuatro mediciones, dos de ellas con el mismo tiempo para que se vea el desempate:

```java
// fig04_18.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("pagos", 87),
          new Medicion("catalogo", 142),
          new Medicion("busqueda", 142),
          new Medicion("inventario", 2300));

  Comparator<Medicion> porTiempo = Comparator.comparingLong(Medicion::tiempoMs);

  List<Medicion> ascendente = mediciones.stream().sorted(porTiempo).toList();
  List<Medicion> descendente = mediciones.stream().sorted(porTiempo.reversed()).toList();
  List<Medicion> desempatado =
      mediciones.stream().sorted(porTiempo.thenComparing(Medicion::nombre)).toList();

  ascendente.forEach(medicion -> IO.println("asc  " + medicion));
  descendente.forEach(medicion -> IO.println("desc " + medicion));
  desempatado.forEach(medicion -> IO.println("tiempo y nombre " + medicion));

  Optional<Medicion> masLenta = mediciones.stream().max(porTiempo);
  IO.println(masLenta);
  IO.println(mediciones);
}
```

```text
$ java fig04_18.java
asc  Medicion[nombre=pagos, tiempoMs=87]
asc  Medicion[nombre=catalogo, tiempoMs=142]
asc  Medicion[nombre=busqueda, tiempoMs=142]
asc  Medicion[nombre=inventario, tiempoMs=2300]
desc Medicion[nombre=inventario, tiempoMs=2300]
desc Medicion[nombre=catalogo, tiempoMs=142]
desc Medicion[nombre=busqueda, tiempoMs=142]
desc Medicion[nombre=pagos, tiempoMs=87]
tiempo y nombre Medicion[nombre=pagos, tiempoMs=87]
tiempo y nombre Medicion[nombre=busqueda, tiempoMs=142]
tiempo y nombre Medicion[nombre=catalogo, tiempoMs=142]
tiempo y nombre Medicion[nombre=inventario, tiempoMs=2300]
Optional[Medicion[nombre=inventario, tiempoMs=2300]]
[Medicion[nombre=pagos, tiempoMs=87], Medicion[nombre=catalogo, tiempoMs=142], Medicion[nombre=busqueda, tiempoMs=142], Medicion[nombre=inventario, tiempoMs=2300]]
```

Qué se leyó en la salida:

- `Comparator.comparingLong(Medicion::tiempoMs)` construye un comparador a partir de una función que extrae el valor a comparar. Hay versiones para `comparing` (cualquier tipo con orden natural), `comparingInt`, `comparingLong` y `comparingDouble`.
- `.reversed()` invierte el orden. `.thenComparing(...)` agrega un **desempate**: se usa solo cuando los dos elementos empatan en el primer criterio. Por eso, en el tercer bloque, `busqueda` queda antes que `catalogo`: tienen los mismos 142 ms y el desempate los ordena alfabéticamente.
- Sin desempate, en los dos primeros bloques `catalogo` queda antes que `busqueda` en los dos sentidos. No es casualidad: la [documentación de `Stream.sorted`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Stream.html) garantiza que «para flujos ordenados, la ordenación es estable»: los elementos que empatan conservan el orden que traían.
- **`sorted` no toca la lista original.** Las últimas líneas lo prueban: `mediciones` sigue en el orden en que se creó. Si quieres ordenar la lista misma, `lista.sort(comparador)` lo hace en el lugar, pero solo funciona con una lista modificable: sobre una `List.of` lanza otra vez `UnsupportedOperationException`.
- `max(porTiempo)` devuelve un `Optional<Medicion>` y no una `Medicion`, y se imprime como `Optional[Medicion[nombre=inventario, tiempoMs=2300]]`. Preguntar por el máximo de una lista vacía no tiene respuesta, y el tipo lo dice.

Un `forEach` apareció dos veces en el programa: `ascendente.forEach(medicion -> IO.println(...))`. Recibe una lambda que no devuelve nada (un `Consumer`, la interfaz funcional del «haz esto con cada elemento») y está disponible en listas y en flujos. Para imprimir está bien; para llenar otra colección, no (ver «Lo que se hace mal»).

#### 4.3.6 ¿Flujo o bucle?

El mensaje equivocado sobre los *streams* es que «son mejores». No lo son: son otra herramienta. Prefiere un **flujo** cuando transformas o resumes una colección (filtrar, convertir, contar, agrupar, ordenar), porque el código dice qué se quiere. Prefiere un **bucle** cuando necesitas salir antes con `break`, llevar un índice, modificar algo de afuera, o cuando el cuerpo puede lanzar una excepción verificada (una lambda no puede lanzar más excepciones verificadas que las que declara su interfaz funcional, y la `Function` que recibe `map` no declara ninguna; la lección 5 lo explica, y por eso `revisarTodos` volverá a ser un `for` en ese momento: no es un retroceso, es la herramienta adecuada). Y una regla de lectura que ahorra discusiones: si un flujo ya no cabe en seis o siete renglones, o si tiene una lambda de varias líneas, extrae esa lambda a un método con nombre y usa la referencia (`Revision::revisar` lee mejor que cinco renglones adentro de un `map`).

### 4.4 `Optional`: un valor que puede no estar

El `revisor` tiene un problema de representación que se ha ido repitiendo: ¿qué devuelve una función cuando no hay valor? `Map.get("reportes")` devolvía `null`; un caído no tiene tiempo de respuesta; el promedio de nadie no existe. La lección 2 mostró el costo de `null`: el compilador no lo distingue de un valor, y el error aparece lejos, como un `NullPointerException`. `Optional<T>` es la respuesta del JDK: una cajita que **tiene un valor o está vacía**, y cuyo tipo avisa a quien la recibe que tiene que decidir qué hacer en los dos casos.

El programa `fig04_19` recorre el vocabulario:

```java
// fig04_19.java
Optional<Long> tiempoDe(Map<String, Long> tiempos, String nombre) {
  return Optional.ofNullable(tiempos.get(nombre));
}

void main() {
  Map<String, Long> tiempos = Map.of("catalogo", 142L, "pagos", 87L);

  Optional<Long> hay = tiempoDe(tiempos, "pagos");
  Optional<Long> noHay = tiempoDe(tiempos, "reportes");
  IO.println(hay);
  IO.println(noHay);
  IO.println(hay.isPresent() + " " + noHay.isEmpty());

  IO.println(hay.orElse(0L));
  IO.println(noHay.orElse(0L));
  IO.println(hay.map(tiempo -> tiempo + " ms").orElse("sin tiempo"));
  IO.println(noHay.map(tiempo -> tiempo + " ms").orElse("sin tiempo"));
  hay.ifPresent(tiempo -> IO.println("pagos respondió en " + tiempo));
  noHay.ifPresent(tiempo -> IO.println("esto no se imprime"));

  OptionalLong mayor = tiempos.values().stream().mapToLong(Long::longValue).max();
  IO.println(mayor);
  IO.println(mayor.getAsLong());
}
```

```text
$ java fig04_19.java
Optional[87]
Optional.empty
true true
87
0
87 ms
sin tiempo
pagos respondió en 87
OptionalLong[142]
142
```

Las tres formas de **crear** un `Optional`: `Optional.of(valor)` (el valor no puede ser `null`; si lo es, lanza `NullPointerException`), `Optional.ofNullable(valor)` (si el valor es `null`, queda vacío; es la que usa `tiempoDe` para adaptar un `get` que podía devolver `null`) y `Optional.empty()`. Las formas de **consumirlo**; la última es la que conviene evitar:

- `orElse(porOmision)` devuelve el valor, o el que tú das si está vacío. `orElseGet(proveedor)` hace lo mismo pero recibe una lambda que solo se ejecuta si hace falta. `orElseThrow()` lanza una excepción si está vacío (la lección 5 explica cuáles).
- `map(función)` transforma el valor si existe y deja el vacío como está: `hay.map(tiempo -> tiempo + " ms")` produce un `Optional<String>` con «87 ms» dentro, que `orElse` desenvuelve; con el vacío, `map` no hace nada, el resultado sigue vacío y `orElse` da «sin tiempo». Es lo que evita escribir `if (x != null)` por todas partes.
- `ifPresent(acción)` ejecuta la lambda solo si hay valor; `isPresent()` e `isEmpty()` preguntan.
- `get()` saca el valor, **y lanza una excepción si no hay**. Existe, pero es la puerta de entrada al mismo problema que `Optional` quería resolver.

Veamos el costo de `get()` sin comprobar:

```java
// fig04_20.java
void main() {
  Optional<String> nombre = Optional.empty();
  IO.println("a punto de pedir el valor");
  IO.println(nombre.get());
}
```

```text
$ java fig04_20.java
a punto de pedir el valor
Exception in thread "main" java.util.NoSuchElementException: No value present
	at java.base/java.util.Optional.get(Optional.java:143)
	at fig04_20.main(fig04_20.java:5)
```

`NoSuchElementException: No value present` («no hay valor»). El error es honesto y es el mismo que el `NullPointerException` de siempre, con otro nombre: alguien asumió que había valor. Ahora, con la diferencia de que el tipo `Optional` hizo evidente el riesgo en el punto exacto donde se tomó la decisión. La regla práctica: **no llames `get()` a menos que lo hayas comprobado en el mismo bloque**, y casi siempre hay un `orElse` o un `map` que dice lo mismo mejor.

También existe `orElse` con un detalle que cuesta caro: su argumento se calcula **siempre**, aunque no haga falta. El programa `fig04_21` lo muestra con un valor por omisión que avisa cuando se calcula:

```java
// fig04_21.java
String calcular(String quien) {
  IO.println("  (calculando el valor por omisión para " + quien + ")");
  return "ninguno";
}

void main() {
  Optional<String> hay = Optional.of("catalogo");
  Optional<String> noHay = Optional.empty();

  IO.println("orElse con valor presente:");
  IO.println(hay.orElse(calcular("orElse")));
  IO.println("orElseGet con valor presente:");
  IO.println(hay.orElseGet(() -> calcular("orElseGet")));
  IO.println("orElseGet con valor ausente:");
  IO.println(noHay.orElseGet(() -> calcular("orElseGet")));
}
```

```text
$ java fig04_21.java
orElse con valor presente:
  (calculando el valor por omisión para orElse)
catalogo
orElseGet con valor presente:
catalogo
orElseGet con valor ausente:
  (calculando el valor por omisión para orElseGet)
ninguno
```

Con el valor presente, `orElse` calculó el valor por omisión de todos modos (el mensaje «calculando...» salió) y lo tiró; `orElseGet` no lo calculó. Para un `"ninguno"` literal da igual; para algo caro, como una consulta o un objeto grande, usa `orElseGet`. Es la misma idea de la lección 2 con `&&` contra `&`: no evalúes lo que no necesitas.

Y la última parte, la que trae reglas de diseño. La documentación de [`Optional`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html) dice, en una nota de API, que «está pensado principalmente para usarse como tipo de retorno de un método donde hay una necesidad clara de representar *ningún resultado*, y donde usar `null` probablemente cause errores». De ahí salen tres reglas:

1. **Úsalo como tipo de retorno**, no como parámetro (obliga a quien llama a envolver un valor que ya tiene) ni como lista de elementos (`List<Optional<T>>` casi siempre es un `filter` que falta).
2. **Una variable `Optional` jamás debe ser `null`**: la documentación lo dice con esas palabras. Un `null` donde se esperaba un `Optional` es peor que cualquiera de los dos por separado.
3. **Para primitivos, `OptionalInt`, `OptionalLong` y `OptionalDouble`**: sin envoltorios, con `getAsLong()` en lugar de `get()`. Los usan `average()` y `max()` de los flujos numéricos, y el `revisor`, para el tiempo.

El proyecto rompe la regla de «no como campo» en un lugar, y conviene decirlo de frente: el *record* `Resumen` de la sección 4.5 guarda un `OptionalLong promedioMs`. Es un *record* inmutable, que se crea una vez y se lee varias; el valor ausente es parte de los datos (cuando nadie respondió, no hay promedio), y las alternativas son peores: un `Long` que puede ser `null` es el problema que acabamos de dejar atrás, y un `0` mentiría. Es una excepción consciente a una guía, no un descuido. En la lección 8 verás cómo ese valor ausente cruza la frontera hacia el JSON, donde el formato sí pide un `null`.

### 4.5 El paso del proyecto: el reporte con resumen

Ahora a juntar todo. La lección 3 dejó un `Revisor.java` que revisaba cuatro servicios escritos a mano y un `simular` con un solo `switch`, que juntaba los datos de los cuatro servicios. Hoy el proyecto cambia así:

- Los **datos simulados** salen del `switch` y se van a un mapa en una clase nueva, `Simulador`. Se busca por nombre y devuelve un `Optional`.
- **`Revision`** revisa un servicio (consultando al simulador) y revisa una lista entera con un *stream*.
- **`Resumen`** cuenta y promedia con `groupingBy` y `average`; **`Reporte`** junta los resultados y su resumen, con una copia defensiva.
- **`Resultado`** aprende a dar su tiempo como `OptionalLong`; **`Formato`** aprende a armar el resumen y la tabla completa.
- **`Revisor.java`** deja de llamar a `revisar` cuatro veces: tiene una lista y pide el reporte de una vez.

Cuatro archivos no cambian: `Estado`, `Respuesta`, `Servicio` y `Clasificador`. Los incluyo igual porque la carpeta de la figura es el proyecto **completo**, y así puedes correr todo desde ella. Cópialos tal cual de la lección 3. Empieza por ellos:

```java
// fig04_22/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig04_22/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig04_22/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

```java
// fig04_22/Clasificador.java
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

Una nota sobre `import`. Los cuatro archivos de arriba y los que siguen son clases normales (no archivos compactos), y por eso empiezan, cuando lo necesitan, con líneas `import java.util.List;`. Un `import` le dice al compilador de dónde viene un nombre que no es de `java.lang` (el paquete que Java importa solo, con `String`, `Math` y `Thread`). Se escribe uno por clase, sin comodines, como indica la guía de estilo de Google que el curso sigue desde la lección 1. El archivo compacto `Revisor.java`, en cambio, importa solo todo `java.base` y no necesita ninguno.

Sigue `Resultado`, que gana un método. `tiempoMs()` devuelve un `OptionalLong`: con valor cuando hubo respuesta y vacío cuando no. Es el mismo `switch` exhaustivo de la lección 3: si algún día alguien agrega un tercer tipo de `Respuesta`, el compilador marcará este método y `detalle()` como incompletos.

```java
// fig04_22/Resultado.java
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

`Simulador` es la clase que hace de red hasta la lección 8. Su `RESPUESTAS` es un `Map` inmutable (`Map.of`) de nombre a respuesta, con las tres que responden y la que no. `buscar` envuelve el `get` del mapa en un `Optional.ofNullable`, y es el único punto donde la ausencia de un nombre se convierte en un tipo que obliga a decidir.

```java
// fig04_22/Simulador.java
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

`Revision` tiene los dos métodos que producen resultados. `revisar` pide la respuesta al simulador y, si el nombre no está, usa un `SinRespuesta("sin conexión")` (una `orElse` con valor barato). `revisarTodos` es la línea que justifica toda la lección: un *stream* sobre la lista de servicios, con `Revision::revisar` como `map` y `toList()` al final.

```java
// fig04_22/Revision.java
import java.util.List;

final class Revision {
  static Resultado revisar(Servicio servicio) {
    Respuesta respuesta =
        Simulador.buscar(servicio.nombre()).orElse(new Respuesta.SinRespuesta("sin conexión"));
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  static List<Resultado> revisarTodos(List<Servicio> servicios) {
    return servicios.stream().map(Revision::revisar).toList();
  }
}
```

`Resumen` es donde se juntan los *streams* y los colectores. `groupingBy` cuenta cuántos hay de cada estado y `average` calcula el promedio de los tiempos de los que respondieron. Dos detalles. Uno: `groupingBy` solo crea una llave para los estados que **aparecen**; si ningún servicio está caído, `porEstado.get(Estado.CAIDO)` sería `null`. Por eso se usa `getOrDefault(..., 0L)`: sin él, el primer reporte sin caídos tumbaría el programa con el `NullPointerException` de la sección 4.2.2. `intValue()` convierte el `Long` del conteo en el `int` que pide el *record* (los conteos caben de sobra). Dos: el promedio cuenta a todos los que respondieron, sea cual sea su código (un 503 respondió, aunque haya respondido mal), y por eso se calcula sobre `tiempoMs()` y no sobre el estado.

```java
// fig04_22/Resumen.java
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

`Reporte` es el *record* de la copia defensiva de la sección 4.1.4. Su fábrica `de` junta la lista de resultados con su resumen:

```java
// fig04_22/Reporte.java
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

`Formato` crece con tres métodos: `contar`, que pone una palabra en plural (la única regla de plural del curso: se agrega una «s» si la cantidad no es 1), `resumen`, que arma la línea del final, y `tabla`, que junta el encabezado, los renglones (unidos con `Collectors.joining("\n")`), una línea en blanco y el resumen. El `? :` de `contar` es el **operador condicional** de la lección 2 (2.3.1): `cantidad == 1 ? "" : "s"` se lee «si la cantidad es 1, nada; si no, una "s"». (El mismo operador aparece en `Resumen`, para decidir si hay promedio.) Y hay un cambio pequeño en un método que ya tenías, que conviene decir en voz alta: `formatearTiempo` deja de comparar contra `Clasificador.UMBRAL_LENTO_MS` y compara contra `1000`. Son dos ideas distintas que hoy valen lo mismo: el umbral de «lento» es una regla del negocio, y el paso de milisegundos a segundos es una regla de unidades. Si mañana el umbral de lento baja a 800 ms, la tabla tiene que seguir escribiendo `999ms` y no `1.0s`; con la constante compartida, cambiar una regla habría cambiado la otra sin que nadie lo pidiera. (La lección 7 hace justo ese cambio de umbral, y una prueba lo vigila.) El resto del archivo, `PATRON_LINEA`, `tiempo`, `encabezado` y `linea`, queda como en la lección 3.

```java
// fig04_22/Formato.java
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

Y `Revisor.java` queda corto. Tiene dos novedades que se explican solas: una **lista** de servicios (un campo del archivo compacto, es decir, una variable que está fuera de cualquier método y que todos los métodos del archivo pueden usar) marcada con `final`, que significa «esta variable no se puede reasignar», y un `main` de una sola línea:

```java
// fig04_22/Revisor.java
final List<Servicio> servicios =
    List.of(
        new Servicio("catalogo", URI.create("https://catalogo.example")),
        new Servicio("inventario", URI.create("https://inventario.example")),
        new Servicio("pagos", URI.create("https://pagos.example")),
        new Servicio("reportes", URI.create("https://reportes.example")));

void main() {
  IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
}
```

Corre el proyecto desde su carpeta:

```text
$ cd fig04_22
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Esa es la tabla de la lección 3, con lo que le faltaba: la línea en blanco y el resumen. Mira cómo se lee el resumen frente al código: `4 servicios` es `contar(4, "servicio")`, `2 arriba` es el conteo de `ARRIBA`, `1 lento` y `1 caído` son `contar(...)` con sus palabras, y `843ms` es el `Math.round` de 2529 / 3, formateado con `formatearTiempo` (que ya sabe que 843 < 1000 se escribe con «ms»). Cuando el resumen te dé un número que no esperas, empieza por el `Resumen`: ahí se calcula todo.

## El error que vas a ver

El error de esta lección es `UnsupportedOperationException`, y es el que casi todos encuentran la primera semana con colecciones. Ya lo viste con `List.of`. Esta es la misma traza, para que la leas renglón por renglón:

```text
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_04.main(fig04_04.java:5)
```

**Primer renglón: qué.** `UnsupportedOperationException` es el tipo; no trae mensaje, porque la operación no está soportada y no hay más que decir. **Segundo y tercero: dónde, dentro del JDK.** Hablan de `ImmutableCollections`, la clase que implementa las colecciones de `List.of`, `Set.of` y `Map.of` (y de `toList()`). Cuando veas ese nombre, ya sabes que intentaste modificar una colección inmutable. **Cuarto: dónde, en tu código.** El primer renglón que menciona tu archivo es el que debes mirar.

El mismo error aparece por otros caminos que conviene reconocer. Ocurre cuando modificas lo que devolvió `toList()` de un flujo:

```java
// fig04_23.java
void main() {
  List<String> nombres = Stream.of("catalogo", "inventario", "pagos").toList();
  IO.println(nombres);
  nombres.add("reportes");
}
```

```text
$ java fig04_23.java
[catalogo, inventario, pagos]
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_23.main(fig04_23.java:5)
```

La traza es idéntica, con otro número de línea de tu programa. Y ocurre cuando llamas `sort` sobre una lista inmutable. Hay dos soluciones, según lo que quieras. Si de verdad necesitas una lista modificable, hazla explícita con una copia: `new ArrayList<>(nombres)`. Si lo que querías era una lista ordenada, no la ordenes en el lugar: usa `stream().sorted(...).toList()` y recibe otra lista. **La mejor reacción al error no es hacer la lista modificable por reflejo, sino preguntarte si la operación que intentas debía existir.**

## Lo que se hace mal

- **Modificar una lista mientras la recorres con `for`.** Es el error clásico, y la trampa es que a veces ni siquiera falla. Si quitas un elemento dentro del `for` mejorado, la mayoría de las veces el programa se detiene con una excepción:

```java
// fig04_24.java
void main() {
  List<String> nombres = new ArrayList<>(List.of("catalogo", "inventario", "pagos", "reportes"));
  for (String nombre : nombres) {
    if (nombre.startsWith("i")) {
      nombres.remove(nombre);
    }
  }
  IO.println(nombres);
}
```

```text
$ java fig04_24.java
Exception in thread "main" java.util.ConcurrentModificationException
	at java.base/java.util.ArrayList$Itr.checkForComodification(ArrayList.java:1096)
	at java.base/java.util.ArrayList$Itr.next(ArrayList.java:1050)
	at fig04_24.main(fig04_24.java:4)
```

  `ConcurrentModificationException` («modificación concurrente»; el nombre engaña, porque no hay hilos de por medio: la lista detectó que cambió mientras el recorrido seguía en marcha). Lo peor es el caso silencioso: medí que si el elemento que quitas es el **penúltimo**, el recorrido termina sin quejarse y se salta el último. Con `["catalogo", "inventario", "pagos"]` quitando `"inventario"`, la salida es `[catalogo, pagos]` y el programa nunca miró a `pagos`. El costo es un programa que a veces pierde un elemento y no lo dice. La solución: `lista.removeIf(nombre -> nombre.startsWith("i"))`, que existe justo para esto, o un flujo con `filter` que produzca una lista nueva.

- **Confundir `remove(int)` con `remove(Object)`.** En una `List<Integer>`, `lista.remove(87)` interpreta el 87 como una **posición**, no como un valor.

```java
// fig04_25.java
void main() {
  List<Integer> tiempos = new ArrayList<>(List.of(142, 2300, 87));
  tiempos.remove(Integer.valueOf(87));
  IO.println(tiempos);
  tiempos.remove(87);
  IO.println(tiempos);
}
```

```text
$ java fig04_25.java
[142, 2300]
Exception in thread "main" java.lang.IndexOutOfBoundsException: Index 87 out of bounds for length 2
	at java.base/jdk.internal.util.Preconditions.outOfBounds(Preconditions.java:100)
	at java.base/jdk.internal.util.Preconditions.outOfBoundsCheckIndex(Preconditions.java:106)
	at java.base/jdk.internal.util.Preconditions.checkIndex(Preconditions.java:302)
	at java.base/java.util.Objects.checkIndex(Objects.java:365)
	at java.base/java.util.ArrayList.remove(ArrayList.java:552)
	at fig04_25.main(fig04_25.java:6)
```

  La primera llamada (`Integer.valueOf(87)`, que pide quitar el valor) funcionó; la segunda buscó la posición 87 en una lista de dos elementos y lanzó `IndexOutOfBoundsException`. Es un caso donde la comodidad del *autoboxing* de la sección 4.2.2 se vuelve contra ti: el compilador elige el método `remove(int)` porque `87` es un `int`. Con listas de textos no hay ambigüedad. Cuando necesites quitar un número por su valor, envuélvelo a propósito, como en la primera llamada.

- **Usar tipos crudos (`List` sin `<...>`).** Como mostró la figura del borrado, le quita al compilador la capacidad de avisarte, y el error aparece lejos de su causa, como `ClassCastException`. Costo: depurar un error de tipo en la ejecución, que el compilador te habría marcado en la línea del descuido. Cualquier aviso `[unchecked]` es esto.

- **Llamar a `Optional.get()` sin comprobar**, o declarar un `Optional` como campo o parámetro. La documentación lo desaconseja, y las dos prácticas devuelven los problemas del `null` con otro nombre. Costo: `NoSuchElementException` en producción, y firmas que obligan a quien llama a envolver y desenvolver valores sin necesidad.

- **Llenar una colección de afuera desde un `forEach` o un `map`.** Escribir `mediciones.stream().forEach(m -> destino.add(m.nombre()))` modifica un estado externo desde una lambda; funciona con un flujo secuencial y se rompe de maneras sutiles con uno paralelo. Costo: código frágil que parece un `map` mal escrito; la herramienta correcta es `collect` o `toList()`.

- **Dar por hecho el orden de un `HashMap`, un `HashSet` o un `Set.of`.** Funciona en tu máquina y cambia en otra, o en la siguiente ejecución. Costo: un reporte que a veces sale en otro orden y que ninguna prueba reproduce a la primera. Si el orden importa para quien lee, elige `LinkedHashMap`, `TreeMap` o una lista.

- **Devolver o guardar la lista interna sin copiarla.** Es la trampa de la sección 4.1.4: otro código conserva la referencia y modifica tus datos sin que lo notes. Costo: valores que cambian solos, el tipo de error más difícil de localizar porque ocurre lejos de donde se ve el efecto.

## Ejercicios

### Ejercicio 1 — Agrega dos servicios al proyecto

En tu `Revisor.java`, agrega a la lista `facturacion` (`https://facturacion.example`) y `notificaciones` (`https://notificaciones.example`). **Antes de correr**, predice qué estado tendrán y cuál será el resumen. Después corre. Luego agrega los dos a `Simulador` (`facturacion` responde con el código 503 en 95 ms; `notificaciones` no responde, con el motivo «tiempo agotado»), predice de nuevo el resumen y corre. Anota en tu bitácora qué le pasó al renglón de `notificaciones`.

### Ejercicio 2 — Ordena el reporte

Haz que la tabla salga ordenada del servicio más lento al más rápido, con los que no respondieron al final, y que los empates se desempaten por nombre. Pista: `Comparator.comparingLong`, `reversed` y `thenComparing`; el tiempo de quien no respondió puede sustituirse con `orElse(-1)`. Pasa la lista ya ordenada a `Reporte.de`.

### Ejercicio 3 — ¿Cuál lanza una excepción?

Para cada línea, decide sin ejecutar si lanza una excepción o no, y cuál. Después compruébalo en un archivo compacto, ejecutando cada línea sola (una por corrida: la primera excepción detiene el programa):

1. `List.of("catalogo", null)`
2. `Map.of("a", 1, "a", 2)`
3. `new ArrayList<>(List.of("a")).get(5)`
4. `Set.of("a").add("b")`
5. `new HashMap<String, Integer>().put(null, 1)`
6. `new TreeMap<String, Integer>().put(null, 1)`

### Ejercicio 4 — Los nombres por estado

Imprime, a partir de los resultados del `revisor`, un mapa de cada `Estado` a la lista de los nombres de servicio que tienen ese estado, **en el orden en que se declaran los estados** (`ARRIBA`, `LENTO`, `CAIDO`). Pista: `groupingBy` acepta como segundo argumento una función que fabrica el mapa, y `EnumMap` es un mapa especializado para llaves de un `enum` que las ordena por su declaración.

## Soluciones

### Solución 1

Sin tocar `Simulador`, los dos servicios nuevos no están en el mapa de respuestas, así que `buscar` devuelve un `Optional` vacío y `revisar` usa el `SinRespuesta("sin conexión")` por omisión: los dos salen `CAIDO`. Con el cambio en `Revisor.java` (agregar dos `new Servicio(...)` a la lista), la tabla es:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO          —  sin conexión
notificacionesCAIDO          —  sin conexión

6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 843ms
```

Son 6 servicios, 2 arriba, 1 lento y 3 caídos; el promedio no cambia (843 ms) porque ninguno de los dos nuevos responde. Después, en `Simulador`, se agregan dos pares al `Map.of` (que acepta hasta diez; para más habría que usar `Map.ofEntries`):

```java
"facturacion", new Respuesta.Respondio(503, 95),
"notificaciones", new Respuesta.SinRespuesta("tiempo agotado"));
```

y el resultado es:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO       95ms  503
notificacionesCAIDO          —  tiempo agotado

6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 656ms
```

`facturacion` es `CAIDO` porque su código es 503 (≥ 400), pero **sí respondió**: tiene tiempo (95 ms) y cuenta para el promedio. El promedio ahora es (142 + 2300 + 87 + 95) / 4 = 656. Es la diferencia entre «no respondió» y «respondió mal», y la razón de que `Resumen` promedie sobre `tiempoMs()` y no sobre el estado.

En el renglón de `notificaciones` hay un defecto visible: el nombre mide 14 caracteres y la columna, 12. `%-12s` no recorta, así que el estado queda pegado al nombre (`notificacionesCAIDO`). El formato rellena pero no inventa espacio. Calcular el ancho de la columna a partir del nombre más largo es un ejercicio opcional de la lección 10; por ahora, ya sabes que un formato fijo es un contrato que los datos pueden romper.

### Solución 2

```java
void main() {
  List<Resultado> resultados = Revision.revisarTodos(servicios);
  Comparator<Resultado> porTiempoDescendente =
      Comparator.comparingLong((Resultado resultado) -> resultado.tiempoMs().orElse(-1))
          .reversed()
          .thenComparing(resultado -> resultado.servicio().nombre());
  List<Resultado> ordenados = resultados.stream().sorted(porTiempoDescendente).toList();
  IO.println(Formato.tabla(Reporte.de(ordenados)));
}
```

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
inventario  LENTO       2.3s  200
catalogo    ARRIBA     142ms  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

Fíjate en el tipo escrito a mano en la lambda: `(Resultado resultado) -> ...`. No es adorno. Si lo omites y escribes `Comparator.comparingLong(resultado -> resultado.tiempoMs().orElse(-1)).reversed()`, el compilador no puede deducir el tipo de `resultado` (porque `reversed()` se encadena antes de que sepa a qué se asignará) y lo toma como `Object`:

```text
Revisor.java:4: error: cannot find symbol
      Comparator.comparingLong(resultado -> resultado.tiempoMs().orElse(-1)).reversed();
                                                     ^
  symbol:   method tiempoMs()
  location: variable resultado of type Object
```

Es un error de inferencia conocido: el compilador deduce los tipos de una lambda mirando a dónde va, y una cadena de métodos le oculta el destino. Con el tipo explícito en el primer comparador, la cadena funciona. El `thenComparing` final no necesita tipo porque ya sabe que es sobre `Resultado`. Los servicios que no respondieron (`reportes`) quedan al final porque su `-1` es el menor y el orden es descendente. Con los empates entre dos que no respondieron, `thenComparing` los desempata por nombre.

### Solución 3

| Línea | Qué pasa |
|---|---|
| `List.of("catalogo", null)` | `NullPointerException`: las colecciones inmutables no admiten `null` |
| `Map.of("a", 1, "a", 2)` | `IllegalArgumentException: duplicate key: a` |
| `new ArrayList<>(List.of("a")).get(5)` | `IndexOutOfBoundsException: Index 5 out of bounds for length 1` |
| `Set.of("a").add("b")` | `UnsupportedOperationException` |
| `new HashMap<String, Integer>().put(null, 1)` | no lanza nada: un `HashMap` acepta una llave `null` y queda `{null=1}` |
| `new TreeMap<String, Integer>().put(null, 1)` | `NullPointerException`: un `TreeMap` necesita comparar las llaves y no puede con `null` |

Medí las seis con JDK 25. Dos conclusiones útiles. Las colecciones inmutables son estrictas desde el momento de crearlas, no al usarlas: `List.of("catalogo", null)` ya falla en la creación. Y que `HashMap` acepte `null` como llave y `TreeMap` no es un ejemplo de que las implementaciones de una misma interfaz **no son intercambiables en todo**: el contrato común (`Map`) cubre lo básico, y los detalles de cada una los dice su documentación.

### Solución 4

```java
void main() {
  List<Resultado> resultados = Revision.revisarTodos(servicios);
  Map<Estado, List<String>> nombresPorEstado =
      resultados.stream()
          .collect(
              Collectors.groupingBy(
                  Resultado::estado,
                  () -> new EnumMap<>(Estado.class),
                  Collectors.mapping(
                      resultado -> resultado.servicio().nombre(), Collectors.toList())));
  IO.println(nombresPorEstado);
}
```

```text
{ARRIBA=[catalogo, pagos], LENTO=[inventario], CAIDO=[reportes]}
```

`groupingBy` con tres argumentos: la función que clasifica (`Resultado::estado`), la fábrica del mapa (`() -> new EnumMap<>(Estado.class)`, una lambda que no recibe nada y devuelve un mapa nuevo) y el colector de cada grupo (`mapping` extrae el nombre y `toList` los junta). `EnumMap` recibe la clase del `enum` (`Estado.class`) y recorre sus llaves en el orden de la declaración: `ARRIBA`, `LENTO`, `CAIDO`. Con un `HashMap` el orden no estaría prometido. `EnumMap` está hecho para llaves de un `enum` y deja claro, a quien lee, que lo son.

## Cómo sé que lo logré

- [ ] `java fig04_01.java` imprime la lista, `primero y último: catalogo, pagos`, y puedes decir por qué `remove("pagos")` quita un valor y no una posición.
- [ ] `java fig04_04.java` termina con `UnsupportedOperationException`, y puedes señalar en la traza qué renglón es de tu programa y cuáles son del JDK.
- [ ] `java fig04_05.java` imprime que el reporte ingenuo cambió y el seguro no, y sabes explicar por qué el segundo se protege con `List.copyOf`.
- [ ] `java fig04_10.java` falla con un `ClassCastException` en una línea que no es la que cometió el error, y sabes qué lo hizo posible (un tipo crudo).
- [ ] `java fig04_15.java` imprime «el stream está armado; todavía no pasó nada» antes de cualquier «miro», y puedes explicar por qué.
- [ ] Desde la carpeta del proyecto, `java Revisor.java` imprime la tabla de cuatro servicios con `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`.
- [ ] Con los dos servicios del ejercicio 1 en `Simulador`, el resumen dice `6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 656ms`, y sabes por qué `facturacion` cuenta para el promedio aunque esté caído.
- [ ] Puedes explicar por qué `Resumen.de` usa `getOrDefault(..., 0L)` y qué pasaría sin él.
- [ ] Anotaste en tu bitácora un *stream* que reescribiste como bucle, o al revés, y cuál se leía mejor.

## Resumen

Responde sin mirar la lección:

1. ¿Por qué se declara `List<String> x = new ArrayList<>()` y no `ArrayList<String> x = ...`?
2. ¿Qué orden de recorrido tiene un `HashMap`, un `LinkedHashMap` y un `TreeMap`? ¿Cuál usarías para un reporte?
3. ¿Qué dos cosas devuelve `Set.add` y `Map.put`, y para qué sirven?
4. ¿Qué lanza `List.of("a").add("b")`? ¿Y `List.of("a", null)`?
5. ¿Qué diferencia hay entre `List.copyOf(x)` y `Collections.unmodifiableList(x)`?
6. ¿Por qué no existe `List<int>`? ¿Qué trampa trae comparar dos `Integer` con `==`?
7. ¿Qué significa que los genéricos se «borran»? Da una consecuencia.
8. ¿Qué tres partes tiene un *stream*? ¿Cuándo se ejecuta el trabajo?
9. ¿Qué pasa si recorres el mismo *stream* dos veces?
10. ¿Qué devuelve `average()` y por qué no un `double`?
11. ¿Cuándo prefieres un bucle a un *stream*?
12. ¿Por qué no se llama `get()` sin comprobar en un `Optional`? ¿Qué usas en su lugar?

## Para leer más

- [Oracle, «Java Collections Framework»](https://docs.oracle.com/en/java/javase/25/core/java-collections-framework.html): la guía oficial de las interfaces y las implementaciones, con la tabla de cuál usar cuándo. Consultado el 7 de octubre de 2026.
- [Oracle, «Generics» (tutorial de Java)](https://docs.oracle.com/javase/tutorial/java/generics/index.html): desde los tipos genéricos hasta los comodines y el borrado, con ejemplos más extensos que esta lección. Consultado el 7 de octubre de 2026.
- [Oracle, «Stream API»](https://docs.oracle.com/en/java/javase/25/core/stream-api.html): la guía de los flujos, con la diferencia entre operaciones intermedias y terminales y los colectores. Consultado el 7 de octubre de 2026.
- [Documentación de `Optional`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html): incluye la nota de API sobre cuándo usarlo. Consultado el 7 de octubre de 2026.

---

**Anterior:** [Lección 3 — Records, enums y tipos sellados](03-records-enums-sellados.md) ·
**Siguiente:** [Lección 5 — Errores y excepciones](05-errores-excepciones.md)
