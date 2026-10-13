# Curso de Java — del lenguaje moderno a un programa que revisa muchos servicios a la vez

**Por Dorian Chávez, fundador de Hábil y arquitecto de integración.**

**Para quién es:** quien ya programa en otro lenguaje —Go, TypeScript, Python o el que sea— y quiere aprender el Java de hoy. Es un curso de nivel intermedio: no se explica qué es una variable ni un bucle, se explica lo que Java hace distinto y por qué existe. Cada concepto nuevo se explica cuando aparece. Si nunca has programado, empieza por el [curso de Python](https://github.com/HabilMX/curso-python) o el de [Go](https://www.habil.mx/es/cursos/go/).

**Qué vas a terminar sabiendo:** escribir un programa completo en Java 25, con el modelo de datos expresado en tipos, pruebas y concurrencia, entender lo que escribiste y poder explicárselo a alguien más. Y quedarte con el criterio para leer la documentación del lenguaje por tu cuenta.

**Lo que necesitas antes de empezar:** una computadora con Linux Mint, saber abrir una terminal y haber escrito programas con funciones, condicionales y bucles en cualquier lenguaje. La [Lección 1](01-instalacion.md) instala el JDK desde cero, y la [Lección 6](06-proyecto-maven.md), Maven.

## El proyecto que vas a construir

El **`revisor`**: un programa que lee una lista de servicios, los consulta **todos a la vez** por HTTP, mide cuánto tarda cada uno, aísla el fallo de cada servicio y entrega un reporte en el mismo orden siempre. Al final lo expone por un pequeño servidor y lo empaqueta en un JAR. Es el mismo problema de los cursos de Go, Rust y TypeScript, resuelto ahora en Java.

    $ java -jar revisor.jar --config servicios.json
    SERVICIO    ESTADO    TIEMPO  DETALLE
    catalogo    ARRIBA     182ms  200
    inventario  LENTO       2.3s  200
    pagos       ARRIBA     132ms  200
    reportes    CAIDO          —  sin conexión

    4 servicios · 2 arriba · 1 lento · 1 caído · promedio 883ms

(Una corrida real del programa terminado contra el servidor de práctica del curso, el 13 de octubre de 2026; los tiempos cambian en cada corrida.)

Parece pequeño y no lo es: para escribirlo bien necesitas tipos sellados, colecciones, excepciones, un proyecto de Maven con pruebas, el cliente HTTP, JSON, hilos virtuales y un paquete que se pueda entregar.

## Las once lecciones

| Lección | | Qué construyes | Qué aprendes |
|---|---|---|---|
| 0 | [Qué es Java y qué NO es](00-que-es-java.md) | nada todavía (lectura) | código fuente, *bytecode* y JVM; JDK y JRE; qué es LTS y por qué la 25; qué protege el compilador y qué no |
| 1 | [Instalar Java 25 en tu Linux Mint](01-instalacion.md) | el entorno y tu primer programa | JDK, `java --version`; `void main()` e `IO.println`; `java Hola.java`; `jshell`; `javac`; el primer error |
| 2 | [Tipos, cadenas y control de flujo](02-tipos-cadenas-control.md) | las funciones base del `revisor` | primitivos y referencias; `var`; `==` contra `equals`; `&&` contra `&`; bloques de texto; `switch` como expresión; `null` |
| 3 | [Records, enums y tipos sellados](03-records-enums-sellados.md) | el modelo `Servicio` / `Estado` | `record`; inmutabilidad; `enum`; `sealed`; `switch` con patrones exhaustivo; patrones de record |
| 4 | [Colecciones, genéricos y streams](04-colecciones-genericos-streams.md) | la lista de servicios y el reporte | `List`, `Map`, `Set`; colecciones inmutables; genéricos; `Comparator`; *streams*; `Optional` |
| 5 | [Errores y excepciones](05-errores-excepciones.md) | el `revisor` que no se cae | verificadas y no verificadas; `try-with-resources`; excepciones propias; resultado contra excepción |
| 6 | [Proyecto con Maven](06-proyecto-maven.md) | el proyecto de verdad: compila, empaqueta y corre | paquetes y visibilidad; instalar Maven; estructura de Maven; `pom.xml`; el ciclo de vida; qué hay en un `.jar` |
| 7 | [Pruebas con JUnit](07-pruebas-junit.md) | las primeras 18 pruebas del `revisor` | JUnit 6; `@Test`; pruebas con tabla de casos; `assertThrows` y `@TempDir`; verla en rojo; leer el informe |
| 8 | [HTTP y JSON](08-http-y-json.md) | una petición real y la configuración | `HttpClient`; tiempos límite; Jackson 3; validar datos de fuera; SSRF |
| 9 | [Concurrencia: revisar todo a la vez](09-concurrencia.md) | el `revisor` concurrente | hilos virtuales; un hilo por tarea; `invokeAll`; `Semaphore`; `ConcurrentHashMap`; una condición de carrera medida |
| 10 | [El programa terminado](10-el-programa-terminado.md) | el `revisor` completo, listo para entregar | línea de comandos y código de salida; servidor del JDK; cierre ordenado; calidad; JAR ejecutable |

Al final de cada lección hay ejercicios con sus soluciones, y una sección de fuentes con los enlaces de lo que se afirmó. Y la [bitácora](bitacora.md) es tuya: anota ahí lo que te costó.

## Tres criterios que atraviesan todo el curso

**Los errores se enseñan por lo que cuestan.** Los estudios con datos de cientos de miles de estudiantes de Java muestran que los docentes se equivocan al adivinar cuáles errores son los difíciles. Los que más tardan en arreglarse son confundir `==` con `equals`, `&` con `&&` y no usar el valor que devuelve un método. Esos aparecen cuando toca, y cada lección provoca a propósito el error que vas a ver.

**Las pruebas y la seguridad no son lecciones, son costumbres.** Hay pruebas desde que el proyecto puede correrlas (lección 7, justo después de convertirlo en un proyecto de Maven), y una prueba no cuenta hasta que la viste fallar. La seguridad aparece donde aparece el riesgo del `revisor`: la dirección que alguien escribe en un archivo, los datos de fuera que se validan, la serialización nativa que no se usa con datos externos.

**Solo se enseña lo que ya es definitivo en Java 25.** Lo que sigue en vista previa aparece en un recuadro «lo que viene», con la bandera que exige, y se dice que no se use en producción. Un curso que enseña lo que acaba de salir envejece en seis meses.

**Para seguir leyendo en el blog de Hábil:** [cuándo conviene (y cuándo no) compilar Java a nativo](https://www.habil.mx/es/blog/java-nativo-cuando-si-cuando-no/), [pruebas que atrapan errores](https://www.habil.mx/es/blog/pruebas-que-atrapan-errores-cobertura-quality-gate/) y [seguridad de APIs](https://www.habil.mx/es/blog/seguridad-apis-cto-identidad-gateway-tokens/). Y si te falta la base de otro lenguaje, el [curso de Python](https://github.com/HabilMX/curso-python) o el de [TypeScript](https://www.habil.mx/es/cursos/typescript/).

## Cómo sabes que terminaste

El curso no se acaba cuando leíste la lección 10, sino cuando tu `revisor` cumple estas seis cosas:

1. **`mvn -B verify` termina en verde y el informe dice cuántas pruebas corrieron.** Cuéntalas del informe, no del código de salida: un verde con cero pruebas se ve igual que un verde de verdad.
2. **Revisa 20 servicios a la vez en un tiempo cercano al del más lento**, no a la suma de todos. Lo mides tú, con el reloj, en serie y en paralelo.
3. **Un servicio caído, lento o que contesta basura no tumba el reporte:** aparece en su fila con su error, y el resto sale completo y en el mismo orden de siempre.
4. **Ninguna petición puede esperar para siempre:** todas tienen tiempo de conexión y tiempo límite.
5. **Rechaza las direcciones que no son `http` o `https` y las que no están en la lista permitida**, en vez de consultar lo que le escriban en el archivo.
6. **Se ejecuta con `java -jar revisor.jar` desde otra carpeta y sale con código 0 si todo está arriba y 1 si no.**
