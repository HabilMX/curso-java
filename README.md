# Curso de Java — del lenguaje moderno a un programa que revisa muchos servicios a la vez

[![Verificar programas](https://github.com/HabilMX/curso-java/actions/workflows/verificar.yml/badge.svg)](https://github.com/HabilMX/curso-java/actions/workflows/verificar.yml)

**Por Dorian Chávez, fundador de Hábil y arquitecto de integración.**

**Los programas de este curso se compilan con Java 25 y se ejecutan automáticamente en cada cambio, y su salida real se compara con la que la lección documenta; los proyectos de Maven corren además sus pruebas. El sello verde lo comprueba y cualquiera puede ver la corrida.**
Haz clic en el sello para abrir la última corrida y ver, paso por paso, qué se ejecutó y qué salió.

> Por ahora el curso está en español, que es el original. Las traducciones (inglés, francés, portugués de Brasil y búlgaro) se agregan después, cada una en su carpeta.

## Dónde está el contenido

- **[Español — el curso completo](es/README.md)** ← empieza aquí
- **[`programas/`](programas/)** — todos los programas del curso, listos para ejecutar

## Para quién es

Quien **ya programa en otro lenguaje** —Go, TypeScript, Python o el que sea— y quiere aprender el Java de hoy, no el de hace quince años. **Es un curso de nivel intermedio:** no se explica qué es una variable, un bucle o una función; se explica lo que Java hace distinto, y por qué. Si nunca has programado, empieza por el [curso de Python](https://github.com/HabilMX/curso-python) o por el de [Go](https://www.habil.mx/es/cursos/go/) y vuelve después.

## El proyecto que crece

El **`revisor`**, el mismo de los cursos de Go, Rust y TypeScript, ahora en Java: un programa que lee una lista de servicios, los **consulta todos a la vez** por HTTP, mide cuánto tarda cada uno, aísla los fallos de cada servicio, imprime un reporte siempre en el mismo orden y sale con el código que un script espera. Al final lo expone también por un pequeño servidor HTTP y se empaqueta en un JAR que cualquiera corre con un comando. Convertirlo en un instalador del sistema con `jpackage` queda como «lo que viene».

Se repite el proyecto y no se inventa otro porque quien llega de otro lenguaje ve el mismo problema resuelto de otra manera, y de esa comparación se aprende más que de empezar de cero. **Lo que cambia es el lenguaje, no el problema.**

Hay además una razón propia de Java: de los cursos y libros de Java que se revisaron para armar éste (una muestra, no un censo), ninguno termina en un programa que consulta servicios HTTP en paralelo. Terminan en un juego, una máquina de café o un recomendador de películas. Aquí el producto final es lo que el lenguaje hoy hace especialmente bien.

## Las lecciones

Numeración única y continua: «Lección N», de la 0 a la 10.

| # | Lección | Qué construyes | Qué aprendes |
|---|---|---|---|
| 0 | Qué es Java y qué NO es | nada todavía (lectura) | código fuente, *bytecode* y JVM; JDK y JRE; qué significa LTS y por qué el curso usa la 25; qué protege el compilador y qué no (los genéricos se borran, `null` sigue existiendo) |
| 1 | Instalar Java 25 en tu Linux Mint | el entorno y tu primer programa | Temurin 25 con SDKMAN, `java --version` en una terminal nueva; **un programa sin clase ni `public static`** (`void main()`, `IO.println`); `java Hola.java`; `jshell`; `javac`; leer el primer error del compilador |
| 2 | Tipos, cadenas y control de flujo | las funciones base del `revisor` | primitivos y referencias; `var`; `String` es inmutable; **`==` contra `equals`**; `&&` contra `&`; bloques de texto; `switch` como expresión; `null` y los mensajes de `NullPointerException` |
| 3 | Records, enums y tipos sellados | el modelo `Servicio` / `Estado` | `record`; inmutabilidad (y por qué es superficial); `enum`; interfaces `sealed`; **`switch` con patrones, exhaustivo y sin `default`**; patrones de record |
| 4 | Colecciones, genéricos y streams | la lista de servicios y el reporte | `List`, `Map` y `Set`; colecciones inmutables y copias defensivas; genéricos y su borrado; `Comparator`; *streams* y `Collectors` para el resumen; `Optional` como valor de retorno |
| 5 | Errores y excepciones | el `revisor` que no se cae | excepciones verificadas y no verificadas; `try`/`catch`/`finally`; `try-with-resources`; excepciones propias con su causa; cuándo un resultado es mejor que una excepción; `InterruptedException` |
| 6 | Proyecto con Maven | el proyecto de verdad: compila, empaqueta y corre | cuándo un archivo suelto deja de alcanzar; paquetes y visibilidad; instalar Maven 3.10.0 con SDKMAN; estructura estándar de Maven; `pom.xml`; el ciclo de vida; qué hay dentro de un `.jar` |
| 7 | Pruebas con JUnit | las primeras pruebas del `revisor` | JUnit 6 con `@Test`, `@ParameterizedTest` (tabla de casos), `assertThrows` y `@TempDir`; ver la prueba en rojo antes que en verde; **el conteo de pruebas se lee del informe, no del código de salida** |
| 8 | HTTP y JSON | una petición real y la configuración | `HttpClient` (un cliente, tiempos límite, redirecciones); códigos de estado; Jackson 3 (`tools.jackson`) y qué cambia respecto a casi todo lo que hay en internet; validar lo que llega de fuera; **SSRF: la URL la escribe otra persona** |
| 9 | Concurrencia: revisar todo a la vez | el `revisor` concurrente | hilos de plataforma e hilos virtuales; **un hilo virtual por tarea, nunca en un *pool***; `invokeAll` y el orden estable; `Semaphore` para limitar; `ConcurrentHashMap`; una condición de carrera provocada y medida; valores con ámbito; la concurrencia estructurada, solo como «lo que viene» |
| 10 | El programa terminado | el `revisor` completo: línea de comandos, servidor y paquete | argumentos y código de salida; servidor HTTP del propio JDK con `/reporte`; cierre ordenado; Checkstyle y SpotBugs; repaso de seguridad; JAR ejecutable |

Lo que la tabla promete por lección es lo que la lección trae.

**Tres criterios que no son lecciones, son costumbres de todas.**

- **Los errores se enseñan por lo que cuestan, no por lo que parece.** Los estudios de Blackbox y BlueJ (Brown y Altadmri, con cientos de miles de estudiantes) muestran que los docentes se equivocan al adivinar cuáles errores son los difíciles. Por eso `==` contra `equals`, `&` contra `&&` y el valor de retorno ignorado —los que más tardan en arreglarse— aparecen cada vez que toca, y cada lección provoca a propósito el error que va a ver quien la sigue.
- **Las pruebas viven desde que hay un proyecto**, no en un capítulo final. Y se comprueban en rojo: una prueba que nunca se ha visto fallar no demuestra nada.
- **La seguridad se enseña donde aparece el riesgo del `revisor`**: la URL que alguien escribe en un archivo (lección 8), los datos de fuera que se validan antes de usarse, la serialización nativa de Java que no se usa con datos externos, y el repaso en la 10.

**Solo se enseña lo que ya es definitivo en Java 25.** La concurrencia estructurada sigue en vista previa en esa versión: aparece en un recuadro «lo que viene», con la bandera que exige, y se dice que no se use en producción. Java 27 ya salió y no es de soporte largo (LTS); el curso se queda a propósito en la 25, que sí lo es, para que lo que aprendes siga vigente años. `jpackage` y la concurrencia estructurada aparecen también en «lo que viene».

**Lo que este curso NO enseña, a propósito:** Spring y Jakarta EE (son un curso aparte, y se entienden mejor después de éste), Gradle, interfaces gráficas (Swing, JavaFX), programación reactiva, hilos a mano con `wait`/`notify`, la serialización nativa de Java y el sistema de módulos de la plataforma. Quien termine estas once lecciones los puede aprender cuando los necesite.

**Para seguir leyendo en el blog de Hábil:** [cuándo conviene (y cuándo no) compilar Java a nativo](https://www.habil.mx/es/blog/java-nativo-cuando-si-cuando-no/), [pruebas que atrapan errores](https://www.habil.mx/es/blog/pruebas-que-atrapan-errores-cobertura-quality-gate/) y [seguridad de APIs](https://www.habil.mx/es/blog/seguridad-apis-cto-identidad-gateway-tokens/).

## La plantilla de cada lección

Igual en todas:

1. **Encabezado:** «Lección N — título», tiempo (90 min o 2 × 90) y «Al terminar vas a poder…» (3 a 7 objetivos medibles).
2. **El porqué antes del cómo:** qué problema resuelve el concepto, con un ejemplo del `revisor`.
3. **Los conceptos**, uno por sección: explicación, ejemplo mínimo ejecutable y ejemplo en el `revisor`.
4. **El error que vas a ver:** el mensaje real del compilador o de la JVM, qué significa y cómo se arregla. Se provoca a propósito.
5. **Lo que se hace mal** (antipatrones), con su costo.
6. **Ejercicios** (2 a 4), de menor a mayor, con sus **soluciones** aparte.
7. **Cómo sé que lo logré:** medible. Tal comando da tal salida; las pruebas pasan y el informe dice cuántas fueron.
8. **Para leer más:** 2 a 4 fuentes, documentación oficial primero.
9. **Fuentes:** al final, los enlaces de lo que se afirmó (especificación del lenguaje, JEP, documentación del JDK, libros reconocidos).

## Cómo ver que los programas funcionan

Sin instalar nada: abre el sello de arriba. Cada corrida muestra los pasos que se ejecutaron y su resultado. Los programas de la lección 8 son la excepción: piden el laboratorio encendido y las bibliotecas descargadas, así que esa lección se sigue a mano, como ella misma explica.

En tu computadora, con un JDK 25 instalado (la lección 1 instala Temurin 25 paso a paso con SDKMAN):

```bash
java --version                      # debe decir 25
cd programas/03-records-enums-sellados
java fig03_01.java                  # compara lo que imprime con fig03_01.salida.txt
```

Cada programa es un archivo `figNN_NN.java` dentro de la carpeta de su lección, con su salida esperada al lado (`figNN_NN.salida.txt`). Los programas que a propósito no compilan traen `figNN_NN.error-esperado.txt`, con el error que la lección enseña a leer. Los de varios archivos traen los demás en la carpeta `figNN_NN/`. Los proyectos de Maven (desde la lección 6) son carpetas con su propio `pom.xml`: entra en la carpeta y corre `mvn -B verify`.

**Las lecciones son la fuente; `programas/` es la copia ejecutable de sus bloques de código.** En cada cambio la verificación hace dos cosas: comprueba que cada bloque de código de una lección sea idéntico a su archivo de `programas/`, y compila y ejecuta ese archivo comparando su salida con la que la lección documenta. Así lo que lees y lo que ejecutas no pueden separarse.

## Qué hay en el repositorio

| | |
|---|---|
| `es/` | el curso en español, una lección por archivo |
| `programas/` | los programas de las lecciones (uno por archivo, con su salida esperada) |
| `herramientas/` | los guiones que verifican el curso (ver abajo) |
| `.github/workflows/verificar.yml` | la verificación automática que muestra el sello |

| `LICENSE.md` | CC BY-SA 4.0 |

Dentro de `herramientas/`:

| | |
|---|---|
| `verificar-programas.sh` | compila y ejecuta los programas de las lecciones con Java 25 y compara la salida real contra la documentada; salta la lección 8, que necesita el laboratorio encendido y las bibliotecas en el *classpath* |
| `verificar-extractos.sh` | comprueba que cada bloque de código de las lecciones diga lo mismo que su archivo de `programas/`; con `--probar` siembra una diferencia y exige detectarla |
| `verificar-plantilla.sh` | comprueba que cada lección tenga las partes de la plantilla, objetivos, ejercicios y fuentes en el número pedido |
| `medir-profundidad.sh` | mide las palabras de explicación por lección |

## La profundidad se mide

Cada lección tiene al menos 3,000 palabras de explicación, como las de Go, Rust y TypeScript (hoy van de unas 7,000 a casi 11,000; el medidor da la cifra exacta). `herramientas/medir-profundidad.sh` cuenta las **palabras de explicación** (lo que está fuera de los bloques de código) y exige dos varas absolutas: **piso de 3,000 palabras por lección y mediana del curso de 4,000**. Sale con código 1 si no cumple, así que la verificación automática se pone roja.

Se cuentan palabras y no líneas porque las líneas se inflan: una frase por línea suma muchas líneas sin explicar más. Y las varas son absolutas y no «la mitad de la mediana del curso» porque ese criterio se muerde la cola: unas lecciones flacas bajan la mediana y entonces pasan. Un criterio relativo garantiza uniformidad, no profundidad.

## Qué es un programa y qué es un fragmento

**No todo bloque de código de este curso es un programa completo.** Un **programa** corre solo, de principio a fin: su bloque empieza con un comentario `// figNN_NN.java` y trae, justo después, la corrida real (`$ java ...` y la salida). Cada uno se compila y se ejecuta en cada cambio, y su salida documentada se compara contra la real. Un **fragmento** es una ilustración que no pretende correr por sí sola, y por eso no se verifica; se reconoce porque su bloque no empieza con ese comentario.

## Reglas del repositorio

- **Licencia CC BY-SA 4.0** (ver `LICENSE.md`): el material es público y se puede reusar citando la fuente.
- **Multiidioma:** `es/` es el original; las traducciones se agregan después y un guion avisa cuando alguna quedó vieja respecto al español.
- **El código no se traduce.** Los nombres de archivo, clases y datos quedan como están; se traducen las explicaciones, los ejercicios, los errores y el glosario.
- `./verificar-publicable.sh` antes de publicar, desde el repositorio de trabajo, que es donde vive su lista de patrones internos. Falla cerrado: si no pudo buscar, o si no llegó a revisar ni un patrón, sale en error y no en verde. Y trae autoprueba: siembra un patrón y exige detectarlo.
- Sin emojis en los títulos de las lecciones.
