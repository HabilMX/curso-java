# Урок 10 — Завършената програма

**Време:** 2 × 90 мин (две сесии; разделянето е между раздели 10.3 и 10.4)

**Какво изграждаш:** пълният `revisor`, готов за предаване: управлява се от командния ред, излиза с код, който един скрипт може да прочете, може да предоставя отчета си по HTTP, минава през вратите за качество и се пакетира в един-единствен JAR

**Какво научаваш:** аргументи и кодове на изход (exit codes) като договор; HTTP сървърът на самия JDK; коректното спиране с кука за изключване; Checkstyle и SpotBugs вътре в `mvn verify` и как да четеш това, което намират; преглед на сигурността на `revisor`; и изпълнимият JAR с `maven-shade-plugin`

**Откъде идваш.** Носиш проекта от [Урок 9](09-concurrencia.md): конкурентен `revisor`, който проверява двайсет услуги за времето на най-бавната, с 28 теста в зелено. Работи, но още е проект на програмист, а не програма, която друг човек може да ползва. За да го стартираш, трябва да напишеш `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` и да стоиш в правилната папка; лимитът на конкурентността е константа, която се променя с прекомпилиране; няма начин да поискаш отчета в JSON; ако всичко пропадне, програмата завършва по същия начин, като че ли всичко е наред; и никой не ѝ е правил преглед на стила, нито автоматично търсене на дефекти. Днес тези пет неща се затварят. Работиш в папката си `~/curso-java/revisor/`.

**Какво не прави този урок.** Не ползва библиотека за командния ред (има добри, като *picocli*, но курсът избягва ненужни зависимости, а седем опции се четат добре на ръка). Не слага криптиране, нито автентикация на сървъра: той е предвиден да го пита друг процес на същата машина и това се обяснява в 10.5. И не превръща JAR в инсталатор: `jpackage` остава като „какво предстои“.

**За стартиранията.** Фигури 10.1 до 10.3 са компактни файлове и се стартират с `java figNN_MM.java`. Фигура 10.4 е проектът; нуждае се от лабораторията в друг терминал (`java Laboratorio.java`, порт 8089; 8088 остава празен нарочно) и, когато вдига собствен сървър, ползва порт 8090. Изходите са получени с Temurin 25.0.4, Maven 3.10.0 и *плъгините (plugin)*, фиксирани в `pom.xml`. Три предупреждения за честност: **времената са от моята машина** (твоите ще са други; пропорциите — не); **съобщенията на Checkstyle и на някои части на JVM излизат на езика на твоята система** (моите излязоха на испански; с `-Duser.language=en` излизат на английски); и командите, които съществуват само в Linux (като `ss`), ги пиша така, както ще ги ползваш на твоя Mint, но **не можах да ги изпълня на машината, където измервах**, така че точния им изход ще го видиш ти.

## След урока ще можеш да

- Четеш аргументите на командния ред на ръка, валидираш ги и върнеш ясна грешка за употреба, без да показваш следа от стека.
- Обясниш какво е код на изход, фиксираш по един за всяка ситуация на `revisor` (`0`, `1`, `2`) и го прочетеш в терминала с `$?`.
- Вдигнеш HTTP сървър с класовете на JDK, който слуша само на `127.0.0.1`, и го спреш коректно, когато някой го прекъсне.
- Стартираш Checkstyle и SpotBugs в `mvn verify`, четеш съобщенията им и обясниш защо един анализ в мълчание не доказва, че няма дефекти.
- Прегледаш `revisor` срещу списък от конкретни рискове (адреси, пренасочвания, сериализация, мрежови интерфейс) и кажеш какво покрива всяко решение.
- Пакетираш програмата в един-единствен JAR и я изпълниш с `java -jar` от коя да е папка.
- Провериш, едно по едно, шестте условия, с които програмата на курса казва, че курсът е завършен.

## Защо, преди как

Кога е завършена една програма? Не когато върви на твоя компютър: това си го постигнал още от урок 5 и до 9-тия всеки урок се проверяваше така. Една програма е завършена, когато **може да я ползва друг човек, друга програма или самият ти след шест месеца, без да четеш кода**. Това изисква неща, които нямат нищо общо с логиката на `revisor`.

Изисква **интерфейс**: начин да му кажеш какво да прави, без да редактираш нищо. Изисква **честен отговор към този, който го вика**: един скрипт, който стартира `revisor` на всеки пет минути, трябва да знае дали всичко е минало добре, без да чете таблицата, и за това съществува кодът на изход. Изисква **качество, което се проверява само**, защото „аз го прегледах“ не мащабира, а стил, който зависи само от дисциплината на хората, се ерозира. Изисква това, което **слуша в мрежата**, да го прави внимателно. И изисква проста **доставка**: един файл, една команда.

Този урок взима тези пет и ги затваря. **Първата** сесия (10.1 до 10.3) е интерфейсът: аргументи, кодове на изход и сървърът с неговото коректно спиране. **Втората** (10.4 до 10.6) е доставката: вратите за качество, прегледът на сигурността и JAR. А упражнение 1 е списъкът, който програмата на курса нарича „Как разбирам, че съм успял“: шест измерими проверки, които правиш ти, с часовника и командите, и които казват дали `revisor` е наистина завършен.

## Понятията

### 10.1 Аргументи и код на изход

#### 10.1.1 Аргументите на командния ред

Когато напишеш `java -jar revisor.jar --config otro.json`, всичко, което идва след името на програмата, пристига в `main` като масив от текстове: `String[] args`, който познаваш от урок 6. Java **не ги интерпретира**: не знае какво е `--config`, нито че `otro.json` е неговата стойност, нито че `--concurrencia` иска число. Само ги предава в реда, в който си ги написал. Фигура 10.1 ги отпечатва и показва и как се излиза от програма с код:

```java
// fig10_01.java
void main(String[] args) {
  IO.println("recibí " + args.length + " argumentos: " + Arrays.toString(args));
  if (args.length == 1 && args[0].equals("--falla")) {
    System.err.println("error de uso: pediste fallar");
    System.exit(2);
  }
  IO.println("termino bien");
}
```

Без аргументи програмата ги брои и завършва нормално:

```text
$ java fig10_01.java
recibí 0 argumentos: []
termino bien
```

Ако ѝ подадеш аргументи, пристигат такива, каквито са, като отделни текстове и в ред. Това е изпълнение с тези на един типичен `revisor` (стартерът на един файл, `java Archivo.java`, предава на `main` всичко, което идва след името на файла):

```text
$ java fig10_01.java --config servicios.json --concurrencia 3
recibí 4 argumentos: [--config, servicios.json, --concurrencia, 3]
termino bien
```

Забележи, че `3` е текстът `"3"`, а не число: да го превърнеш и да решиш какво да правиш, ако някой напише `tres`, е твоя работа. Това е, което ще прави класът `Opciones` в 10.1.3.

#### 10.1.2 Кодът на изход: договор с този, който те вика

Всяка програма, когато завърши, предава на операционната система число между 0 и 255: своя **код на изход** (*exit status*). Това е най-краткият възможен отговор на въпроса „как мина?“ и е това, което четат скриптовете, планировчиците на задачи и инструментите за непрекъсната интеграция. Универсалната конвенция в системите от типа на Unix е: **`0` означава успех, всяко друго число означава, че нещо е тръгнало зле**. В терминал с `bash` кодът на последната команда е в променливата `$?`, а [наръчникът на `bash`](https://www.gnu.org/software/bash/manual/html_node/Exit-Status.html) документира тази конвенция, която е тази, която гледат `if`, `&&` и `set -e`.

Последната част на `fig10_01.java` вече я ползва: с единствения аргумент `--falla` програмата пише грешка в изхода за грешки и завършва със `System.exit(2)`:

```text
$ java fig10_01.java --falla
recibí 1 argumentos: [--falla]
error de uso: pediste fallar
$ echo $?
2
```

Две подробности, които изглеждат дребни и не са. Първата: грешката е записана със `System.err`, а не с `IO.println`. Една програма има **два различни изхода**: **стандартния изход** (`System.out`, където отива резултатът) и **изхода за грешки** (`System.err`, където отиват предупрежденията за човек). Ако някой пренасочи резултата към файл —`revisor --formato json > reporte.json`— иска JSON във файла и грешките на екрана, а не смесени. Втората: `System.exit` **прекратява цялата JVM и не се връща**: редът след извикването никога не се изпълнява, нито остатъкът от програмата, в никоя нишка. Не е рязко прекъсване: започва коректното изключване на JVM, което изпълнява куките за изключване (10.3) и чака да свършат, преди да излезе. Но от гледна точка на твоя код всичко, което идваше след това, вече не се случва. Затова се вика на едно-единствено място, `main`, и му се подава число, изчислено от друг код.

`revisor` фиксира кодовете си така и това е **договор**: това, което други програми могат да приемат за дадено и което затова не се променя, без да се предупреди. (`ARRIBA`, `LENTO` и `CAIDO` са отпечатваните състояния „работи“, „бавна“ и „паднала“; `--ayuda` е „помощ“.)

| Код | Кога | Какво се пише |
|---|---|---|
| `0` | всички услуги са `ARRIBA` (или е поискано `--ayuda` или `--version`) | отчетът, в стандартния изход |
| `1` | поне една услуга е `LENTO` или `CAIDO` | отчетът, в стандартния изход |
| `2` | опциите не се разбират или конфигурацията не става | съобщение в изхода за грешки |

Разликата между `1` и `2` е онази, от която един скрипт има нужда: `1` значи „програмата работи и намери проблем, на който трябва да обърнеш внимание“, а `2` значи „програмата не можа да си свърши работата; проблемът е на този, който я стартира“. Един мониторинг може да алармира различно за всеки.

Липсва една част, за да може да се **тества** този договор. Ако целият поток на програмата живее в `main` и `main` завършва със `System.exit`, тест, който вика `main`, би отнесъл със себе си JVM на тестовете. Решението е обичайното, да се отдели това, което решава, от това, което действа: `main` е един ред, който само вика `System.exit(ejecutar(args, System.out, System.err))`, а всичко останало живее в `ejecutar`, метод, който **получава** двата изхода като параметри и **връща** кода. Един тест му подава два потока в паметта, чете какво е записал и проверява числото, което е върнал, без да прекратява нищо. Това е същата идея от урок 9: комуникирай, като връщаш стойности.

#### 10.1.3 `Opciones`: четене на аргументите на ръка

Това е класът, който превръща масива от текстове в стойност, която вече е валидирана. Преди да го прочетеш, обобщение на седемте флага, които разпознава: `--config RUTA` (по подразбиране `servicios.json`), `--formato tabla|json` (по подразбиране `tabla`), `--concurrencia N` (по подразбиране 10, поне 1), `--permitir h1,h2` (по подразбиране `127.0.0.1,localhost`), `--servir PUERTO`, `--version` и `--ayuda`. Всичко друго е грешка за употреба. Грешката се моделира със собствено проверявано изключение, както `ErrorDeConfiguracion` в урок 5:

```java
// fig10_04/src/main/java/mx/habil/revisor/ErrorDeUso.java

package mx.habil.revisor;

/** Los argumentos de la línea de comandos no se entienden; el mensaje dice cuál falló. */
final class ErrorDeUso extends Exception {

  private static final long serialVersionUID = 1L;

  ErrorDeUso(String mensaje) {
    super(mensaje);
  }
}
```

```java
// fig10_04/src/main/java/mx/habil/revisor/Opciones.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/** Lo que la persona pidió en la línea de comandos, ya validado. */
record Opciones(
    Path config,
    String formato,
    int concurrencia,
    Set<String> permitidos,
    OptionalInt puerto,
    boolean ayuda,
    boolean version) {

  Opciones {
    permitidos = Set.copyOf(permitidos);
  }

  static Opciones leer(String[] args) throws ErrorDeUso {
    Path config = Path.of("servicios.json");
    String formato = "tabla";
    int concurrencia = Revision.LIMITE_POR_OMISION;
    Set<String> permitidos = Configuracion.SERVIDORES_PERMITIDOS;
    OptionalInt puerto = OptionalInt.empty();
    boolean ayuda = false;
    boolean version = false;

    Deque<String> pendientes = new ArrayDeque<>(List.of(args));
    while (!pendientes.isEmpty()) {
      String bandera = pendientes.removeFirst();
      switch (bandera) {
        case "--ayuda" -> ayuda = true;
        case "--version" -> version = true;
        case "--config" -> config = Path.of(valor(pendientes, bandera));
        case "--formato" -> formato = formato(valor(pendientes, bandera));
        case "--concurrencia" ->
            concurrencia = entero(valor(pendientes, bandera), bandera, 1, Integer.MAX_VALUE);
        case "--permitir" -> permitidos = servidores(valor(pendientes, bandera));
        case "--servir" ->
            puerto = OptionalInt.of(entero(valor(pendientes, bandera), bandera, 1, 65535));
        default -> throw new ErrorDeUso("opción desconocida: " + bandera);
      }
    }
    return new Opciones(config, formato, concurrencia, permitidos, puerto, ayuda, version);
  }

  private static String valor(Deque<String> pendientes, String bandera) throws ErrorDeUso {
    if (pendientes.isEmpty()) {
      throw new ErrorDeUso("falta el valor de " + bandera);
    }
    return pendientes.removeFirst();
  }

  private static String formato(String texto) throws ErrorDeUso {
    if (!texto.equals("tabla") && !texto.equals("json")) {
      throw new ErrorDeUso("--formato debe ser tabla o json, no " + texto);
    }
    return texto;
  }

  private static int entero(String texto, String bandera, int minimo, int maximo)
      throws ErrorDeUso {
    try {
      int numero = Integer.parseInt(texto);
      if (numero >= minimo && numero <= maximo) {
        return numero;
      }
    } catch (NumberFormatException e) {
      // cae al error de abajo: un texto que no es número y un número fuera de rango dicen lo mismo
    }
    String rango =
        maximo == Integer.MAX_VALUE
            ? "mayor o igual que " + minimo
            : "entre " + minimo + " y " + maximo;
    throw new ErrorDeUso(bandera + " debe ser un entero " + rango + ", no " + texto);
  }

  private static Set<String> servidores(String texto) throws ErrorDeUso {
    Set<String> servidores =
        Arrays.stream(texto.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    if (servidores.isEmpty()) {
      throw new ErrorDeUso("--permitir necesita al menos un servidor");
    }
    return servidores;
  }
}
```

Четири идеи на класа заслужават по един абзац.

**Аргументите се изяждат от опашка.** Вместо да обхождаш масива с индекс и да прескачаш позиции, те се копират в `ArrayDeque` и първият се вади с `removeFirst()`. Когато един флаг се нуждае от стойността си, вика `valor(pendientes, bandera)`, който вади следващия или хвърля `ErrorDeUso("falta el valor de --config")` („липсва стойността на --config“), ако няма повече. Така кодът няма аритметика с индекси, в която да се сбърка.

**`switch` решава със стрелка.** Това е формата, която вече познаваш от урок 2, но използвана като инструкция (всеки случай присвоява променлива), а не като израз. Със стрелки няма *fall-through*: всеки случай завършва сам. А `default` хвърля грешката, така че лошо написан флаг не минава мълчаливо.

**Число извън обхвата и текст, който не е число, казват едно и също.** `entero` се опитва да превърне с `Integer.parseInt`; ако пропадне или стойността е извън обхвата, съобщението е едно-единствено („трябва да е цяло число, по-голямо или равно на 1, а не tres“). На човека, който е написал флага погрешно, не му върши работа да различава двете причини.

***Record*-ът се защитава.** Компактният му конструктор прави `permitidos = Set.copyOf(permitidos)`: същото защитно копие от урок 4 и по същата причина, колекцията, която *record*-ът пази, да не е такава, която някой друг може да промени после. И една анотация за `OptionalInt puerto`: урок 4 каза, че `Optional` се ползва като върната стойност, а не като поле. Тук има съзнателно изключение: `Opciones` е стойност само за четене, която **описва какво е поискано**, а „не е поискан сървър“ няма число, което да го представя (за сървъра 0 означава нещо: „избери свободен порт“). `OptionalInt` казва точно това.

С решените опции `ejecutar` ги ползва; виждаш го цял в 10.3.3. Преди това трябва да се говори за другия начин, по който `revisor` предава отчета си.

### 10.2 HTTP сървърът на JDK

#### 10.2.1 За какво сървър

Досега, за да видиш отчета, трябва да стартираш програмата, която проверява и завършва. Върши работа за човек в терминал; не върши работа за друг инструмент, който иска да пита за състоянието на всяка минута, или за табло, което показва резултата. За това `revisor` може да остане да **слуша**: с `--servir 8090` отваря HTTP сървър и, всеки път когато някой поиска `/reporte`, прави пълна проверка и отговаря с JSON от урок 8.

Хубавото е, че не е нужна никаква библиотека, за да се постигне. JDK носи прост HTTP сървър, пакетът [`com.sun.net.httpserver`](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html), част от модула `jdk.httpserver`. Това е документиран и поддържан API (името `com.sun` е историческо) и е **същият клас, който ползва `Laboratorio.java`**, програмата, която стартираш от урок 8: ако я отвориш, ще намериш `HttpServer.create`, списък от `createContext` и изпълнител с виртуални нишки. Днес сам пишеш това, което досега само ползваше. (За да се предоставят статични файлове, JDK носи и инструмент за командния ред, `jwebserver`, описан в [JEP 408](https://openjdk.org/jeps/408); не ни е нужен.)

#### 10.2.2 Минимален сървър, от край до край

Фигура 10.2 е най-малкият сървър, който си струва да се прочете. Вдига сървър, регистрира му един маршрут, който отговаря с JSON от един ред, прави заявка към самия себе си с `HttpClient` от урок 8, отпечатва полученото и се изключва. Тъй като е програма, която стартира и завършва сама, може да се провери. (Бележка за четенето: четирите `import` в началото са необходими, защото един компактен файл импортира сам само `java.base`, а нито сървърът, нито HTTP клиентът живеят там; ако ги махнеш, компилаторът ти казва `cannot find symbol` за `HttpClient` и останалите.)

```java
// fig10_02.java
import com.sun.net.httpserver.HttpServer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {
  var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  servidor.createContext(
      "/hola",
      intercambio -> {
        byte[] cuerpo = "{\"hola\": \"mundo\"}".getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json");
        intercambio.sendResponseHeaders(200, cuerpo.length);
        try (var salida = intercambio.getResponseBody()) {
          salida.write(cuerpo);
        }
      });
  servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
  servidor.start();

  InetSocketAddress direccion = servidor.getAddress();
  IO.println("dirección: " + direccion.getAddress().getHostAddress());
  IO.println("solo la interfaz local: " + direccion.getAddress().isLoopbackAddress());

  URI url = URI.create("http://127.0.0.1:" + direccion.getPort() + "/hola");
  try (var cliente = HttpClient.newHttpClient()) {
    var respuesta =
        cliente.send(HttpRequest.newBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
    IO.println("código " + respuesta.statusCode() + ", cuerpo " + respuesta.body());
  }
  servidor.stop(0);
  IO.println("servidor detenido");
}
```

```text
$ java fig10_02.java
dirección: 127.0.0.1
solo la interfaz local: true
código 200, cuerpo {"hola": "mundo"}
servidor detenido
```

Петте стъпки, една по една. **`HttpServer.create(direccion, 0)`** създава сървъра без да го стартира; вторият аргумент е размерът на опашката от чакащи връзки, а 0 значи „който реши системата“. **`createContext("/hola", manejador)`** регистрира какво да се прави със заявките, чийто път започва с `/hola`. **`setExecutor(...)`** решава кой обслужва заявките; **`start()`** го пуска да слуша; **`stop(0)`** го спира (числото са секундите, които чака заявките в полет, и се връща в 10.3).

Три решения на тази фигура, които се повтарят в сървъра на `revisor`:

**Адресът е `127.0.0.1`, а не „всички“.** `new InetSocketAddress("127.0.0.1", puerto)` кара сървъра да слуша **само на локалния интерфейс**: само програмите на същата тази машина могат да му говорят. Ако вместо това напишеш `new InetSocketAddress(puerto)`, без адрес, Java ползва адреса-жокер и слуша на **всички интерфейси**, включително мрежата: всеки компютър, който стига до твоята машина, би могъл да поиска отчета. За вътрешен инструмент, който пита друг локален процес, подходящият е първият; да отвориш втория е решение, което се взема нарочно, с автентикация и криптиране, а не по подразбиране.

**Порт 0 значи „избери свободен“.** Системата присвоява който и да е, който не е зает, а `servidor.getAddress().getPort()` казва кой е бил. Това е, което ползват тестовете от урок 8 и днешните, за да не се сблъскват с нещо, което вече върви.

**Една виртуална нишка за заявка.** `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` е урок 9, приложен. Без изпълнител сървърът обслужва **една заявка наведнъж** в една нишка; измерих го: шест едновременни заявки от по 100 ms стигнаха до манипулатора една по една (максимум едновременно: 1). С една виртуална нишка за заявка бавната заявка не пречи на останалите.

#### 10.2.3 `Servidor`: маршрутът `/reporte`

Класът, който `revisor` ползва, получава порта и **функция, която произвежда `Reporte`** (`Callable<Reporte>`). Това, че получава функцията, а не вече свършената работа, има причина: проверката трябва да се случва **всеки път, когато някой поиска отчета**, а не веднъж при стартиране. И има друга, която ще видиш в тестовете: може да му се даде фиксиран отчет и да се провери какво отговаря, без да се пипа никаква мрежа.

```java
// fig10_04/src/main/java/mx/habil/revisor/Servidor.java

package mx.habil.revisor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** El servidor del propio JDK que entrega el reporte en {@code /reporte}. */
final class Servidor {

  private Servidor() {}

  static HttpServer iniciar(int puerto, Callable<Reporte> revisar) throws IOException {
    HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
    servidor.createContext("/", intercambio -> atender(intercambio, revisar));
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    return servidor;
  }

  private static void atender(HttpExchange intercambio, Callable<Reporte> revisar)
      throws IOException {
    try (intercambio) {
      if (!"/reporte".equals(intercambio.getRequestURI().getPath())) {
        responder(intercambio, 404, "text/plain; charset=utf-8", "no existe");
      } else if (!"GET".equals(intercambio.getRequestMethod())) {
        responder(intercambio, 405, "text/plain; charset=utf-8", "solo GET");
      } else {
        try {
          responder(intercambio, 200, "application/json", Formato.json(revisar.call()));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          responder(intercambio, 500, "text/plain; charset=utf-8", "error interno");
        } catch (Exception e) {
          responder(intercambio, 500, "text/plain; charset=utf-8", "error interno");
        }
      }
    }
  }

  private static void responder(HttpExchange intercambio, int codigo, String tipo, String texto)
      throws IOException {
    byte[] cuerpo = texto.getBytes(StandardCharsets.UTF_8);
    intercambio.getResponseHeaders().set("Content-Type", tipo);
    intercambio.sendResponseHeaders(codigo, cuerpo.length);
    intercambio.getResponseBody().write(cuerpo);
  }
}
```

Какво отговаря всяка ситуация, за да не се налага да го отгатваш от кода: `GET /reporte` отговаря `200` с JSON и `Content-Type: application/json`; всеки друг път, `404`; всеки друг метод (например `POST`), `405`; а ако проверката пропадне от грешка на програмата, `500` с общия текст `error interno` („вътрешна грешка“). Тази последна точка е нарочна: съобщението на едно изключение може да съдържа пътища, имена на класове или данни, които не бива да излизат по мрежата, така че на питащия му се казва, че нещо е пропаднало, и нищо повече. (На този, който оперира програмата, би се казвало през журнал (log), който този курс не изгражда.) `try (intercambio)` затваря разговора с клиента каквото и да стане, а тялото се записва като байтове в UTF-8, защото JSON съдържа ударения („sin conexión“, т.е. „без връзка“).

Една бележка за дизайна, която е добре да се каже. Всяко `GET /reporte` прави **пълна проверка**: пита всички услуги и отнема колкото най-бавната (около 2.5 секунди с четирите канонични). Не е кеш. Ако десет души поискат отчета едновременно, `revisor` прави десет проверки едновременно. Това е следствие, което раздел 10.5 подема, защото има страна, свързана със сигурността.

### 10.3 Коректното спиране и пълният `revisor`

#### 10.3.1 Проблемът: сървър, който не умее да се изключи

Сървърът е програма, която не завършва сама; завършва, когато някой я прекъсне: ти с `Ctrl`+`C`, системата при изключване или инструмент, който ѝ праща сигнала за прекратяване. Ако в този момент има заявка по средата, внезапното прекъсване на сървъра я реже. Решението е **коректното спиране (graceful shutdown)**: да спреш да приемаш нови заявки, да дадеш няколко секунди на тези, които вече са на път, и едва тогава да излезеш.

Java предлага механизъм за това: **куката за изключване (shutdown hook)**. Тя е нишка, която регистрираш с `Runtime.getRuntime().addShutdownHook(hilo)` и която JVM стартира, когато започне да се изключва, по каквато и да е причина: защото последната обикновена нишка е завършила, защото някой е извикал `System.exit` или защото системата ѝ е пратила сигнал за прекратяване като този на `Ctrl`+`C` ([документацията на `Runtime`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html) изброява случаите). Не се изпълнява, ако процесът умре внезапно, като при `kill -9` или спиране на тока; там няма възможно спиране. Фигура 10.3 я показва в най-малката ѝ форма:

```java
// fig10_03.java
void main() throws InterruptedException {
  Runtime.getRuntime()
      .addShutdownHook(new Thread(() -> IO.println("gancho: cerrando con orden")));
  IO.println("trabajando...");
  Thread.sleep(300);
  IO.println("main termina");
}
```

```text
$ java fig10_03.java
trabajando...
main termina
gancho: cerrando con orden
```

Програмата завършва сама (`main termina`, т.е. „main завършва“) и **след това** се изпълнява куката. С `Ctrl`+`C` би станало същото. Две правила за писане на добра кука: да е **къса** и да не остава блокирана (JVM чака всички куки да свършат, така че кука, която никога не завършва, оставя програмата без възможност да излезе; ако има лимит, идва отвън, например от операционната система, която при изключване прекратява процеса рязко), и **никога да не вика `System.exit`** (изключването вече е в ход и да го извикаш отвътре го оставя да чака себе си завинаги).

#### 10.3.2 Как я ползва `revisor`

В режим сървър `revisor` прави три неща, в този ред: стартира сървъра; регистрира кука, която вика `servidor.stop(1)` (спира да приема, чака до 1 секунда заявките в ход да свършат и затваря) и пише `servidor detenido` („сървърът е спрян“); и **чака**. Това чакане е измамната част. Ако `ejecutar` върнеше число и `main` извикаше `System.exit`, сървърът би умрял миг след раждането си. Затова главната нишка остава да чака с `new CountDownLatch(1).await()`: резе, което никой няма да отвори, така че `await()` **не се връща никога** и програмата живее, докато JVM не бъде изключена отвън, с което куката си върши работата.

Ще го изпробваш на ръка в 10.3.5, веднага щом имаш пълния `Revisor` от следващия раздел и тестовете му в зелено.

#### 10.3.3 Пълният `revisor`

Сега вече може да се види целият `Revisor.java`. Чети го като рецепта от шест стъпки, които са тези на `ejecutar`: **1** чете опциите (ако пропаднат: съобщение за употреба и код 2); **2** ако са поискали `--ayuda` или `--version`, отговаря и излиза с 0; **3** чете конфигурацията, валидирана с разрешения списък, който е избран (ако пропадне: съобщение и код 2); **4** ако са поискали `--servir`, предава работата на `servir` и не се връща; **5** ако не, проверява всичко с поискания лимит на конкурентността; **6** пише таблицата или JSON и връща 0, ако всички услуги са горе, и 1, ако не.

```java
// fig10_04/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/** El revisor: consulta los servicios de la configuración y entrega el reporte. */
public final class Revisor {

  static final int CODIGO_TODO_ARRIBA = 0;
  static final int CODIGO_HAY_PROBLEMAS = 1;
  static final int CODIGO_USO_INVALIDO = 2;

  private static final String AYUDA =
      """
      uso: revisor [opciones]

        --config RUTA      archivo de servicios (por omisión servicios.json)
        --formato F        tabla o json (por omisión tabla)
        --concurrencia N   revisiones a la vez, al menos 1 (por omisión {limite})
        --permitir H1,H2   servidores permitidos (por omisión 127.0.0.1,localhost)
        --servir PUERTO    en vez de imprimir, sirve /reporte en 127.0.0.1:PUERTO
        --version          imprime la versión
        --ayuda            imprime esta ayuda

      códigos de salida: 0 todo arriba · 1 hay un servicio lento o caído · 2 uso o configuración
      inválidos
      """;

  private Revisor() {}

  /**
   * Punto de entrada: corre el revisor y sale con su código.
   *
   * @param args las opciones de la línea de comandos; {@code --ayuda} las lista
   */
  public static void main(String[] args) {
    System.exit(ejecutar(args, System.out, System.err));
  }

  static int ejecutar(String[] args, PrintStream salida, PrintStream errores) {
    Opciones opciones;
    try {
      opciones = Opciones.leer(args);
    } catch (ErrorDeUso e) {
      errores.println("error de uso: " + e.getMessage());
      errores.println("prueba: revisor --ayuda");
      return CODIGO_USO_INVALIDO;
    }
    if (opciones.ayuda()) {
      salida.print(ayuda());
      return CODIGO_TODO_ARRIBA;
    }
    if (opciones.version()) {
      salida.println("revisor " + version());
      return CODIGO_TODO_ARRIBA;
    }
    List<Servicio> servicios;
    try {
      servicios = Configuracion.leer(opciones.config(), opciones.permitidos());
    } catch (ErrorDeConfiguracion e) {
      errores.println("error de configuración: " + e.getMessage());
      return CODIGO_USO_INVALIDO;
    }
    try {
      if (opciones.puerto().isPresent()) {
        return servir(opciones.puerto().getAsInt(), servicios, opciones, salida);
      }
      Reporte reporte = revisar(servicios, opciones);
      boolean comoJson = opciones.formato().equals("json");
      salida.println(comoJson ? Formato.json(reporte) : Formato.tabla(reporte));
      boolean todoArriba = reporte.resumen().arriba() == reporte.resumen().total();
      return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;
    } catch (IOException e) {
      errores.println("no se pudo abrir el puerto: " + e.getMessage());
      return CODIGO_USO_INVALIDO;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      errores.println("revisión interrumpida");
      return CODIGO_HAY_PROBLEMAS;
    }
  }

  private static Reporte revisar(List<Servicio> servicios, Opciones opciones)
      throws InterruptedException {
    return Reporte.de(Revision.revisarTodos(servicios, opciones.concurrencia()));
  }

  private static int servir(
      int puerto, List<Servicio> servicios, Opciones opciones, PrintStream salida)
      throws IOException, InterruptedException {
    HttpServer servidor = Servidor.iniciar(puerto, () -> revisar(servicios, opciones));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  servidor.stop(1);
                  salida.println("servidor detenido");
                }));
    salida.println("sirviendo http://127.0.0.1:" + puerto + "/reporte (Ctrl+C para detener)");
    new CountDownLatch(1).await();
    return CODIGO_TODO_ARRIBA;
  }

  static String ayuda() {
    return AYUDA.replace("{limite}", String.valueOf(Revision.LIMITE_POR_OMISION));
  }

  static String version() {
    Properties propiedades = new Properties();
    try (InputStream flujo = Revisor.class.getResourceAsStream("/revisor.properties")) {
      if (flujo != null) {
        propiedades.load(flujo);
      }
    } catch (IOException e) {
      return "desconocida";
    }
    return propiedades.getProperty("version", "desconocida");
  }
}
```

Някои неща, които не са на показ. Версията, която отпечатва `--version`, **не е написана в кода**: идва от файла `revisor.properties`, който Maven пълни с версията от `pom.xml`, така че числото да не се повтаря на две места, които да се разсинхронизират (сглобяваш го точно отдолу). Ако портът е зает, `HttpServer.create` хвърля `IOException`, а `ejecutar` го улавя и казва `no se pudo abrir el puerto: Address already in use` („не можа да се отвори портът: адресът вече се използва“), с код 2: това е проблем на този, който я стартира. А `catch (InterruptedException)` възстановява флага за прекъсване, както учи урок 5, преди да излезе с 1.

За да работи това, са нужни три промени извън кода и ги правиш сега, преди тестовете, защото един от тях проверява именно версията. Първата е файлът със свойства, в нова папка, `src/main/resources/`, тази за файловете, които придружават програмата без да са код (Maven ги копира в `target/classes`). Състои се от един ред:

```properties fig10_04/src/main/resources/revisor.properties
version=${project.version}
```

Другите две са в твоя `pom.xml`. Версията минава от `0.9.0` на **`1.0.0`**, тази на доставката. А вътре в `<build>`, преди `<plugins>`, се добавя блокът, който иска от Maven да **филтрира** тази папка, тоест да замени `${project.version}` с версията от `pom.xml` при копирането на файла:

```xml
  <version>1.0.0</version>
  ...
  <build>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
      </resource>
    </resources>

    <plugins>
      ...
```

Това е фрагмент: останалото от `pom.xml` остава такова, каквото го носиш от урок 9, включително `maven-dependency-plugin`, който още ползваш, за да стартираш програмата. Пълният `pom.xml` на доставката се появява в 10.4.2, когато дойдат вратите за качество и JAR.

Конфигурационният файл със седемте услуги (четирите канонични и трите допълнителни на лабораторията) виси от папката на проекта и ще го ползваш по-надолу:

```json fig10_04/servicios-extra.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "inventario", "url": "http://127.0.0.1:8089/inventario"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"},
    {"nombre": "reportes", "url": "http://127.0.0.1:8088/"},
    {"nombre": "facturacion", "url": "http://127.0.0.1:8089/facturacion"},
    {"nombre": "notificaciones", "url": "http://127.0.0.1:8089/notificaciones", "timeout_ms": 1000},
    {"nombre": "basura", "url": "http://127.0.0.1:8089/basura"}
  ]
}
```

#### 10.3.4 Тестовете на договора

`RevisorTest` носи шест теста, всеки за едно обещание на раздела: версията излиза от `pom.xml`, невалидна конкурентност е грешка за употреба с код 2, всички горе дава 0, една паднала дава 1, JSON на известен отчет е **точно** договорът от урок 8 и сървърът отговаря `200` с `application/json`.

```java
// fig10_04/src/test/java/mx/habil/revisor/RevisorTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RevisorTest {

  private static HttpServer servidor;
  private static int puerto;

  @TempDir Path carpeta;

  @BeforeAll
  static void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/bien", i -> responder(i, 200));
    servidor.createContext("/mal", i -> responder(i, 503));
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    puerto = servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, int codigo) throws IOException {
    intercambio.sendResponseHeaders(codigo, -1);
    intercambio.close();
  }

  /** Lo que dejó una corrida de {@code ejecutar}: el código de salida y los dos flujos. */
  private record Corrida(int codigo, String salida, String errores) {}

  private static Corrida correr(String... args) {
    var salida = new ByteArrayOutputStream();
    var errores = new ByteArrayOutputStream();
    int codigo =
        Revisor.ejecutar(
            args,
            new PrintStream(salida, true, StandardCharsets.UTF_8),
            new PrintStream(errores, true, StandardCharsets.UTF_8));
    return new Corrida(
        codigo,
        salida.toString(StandardCharsets.UTF_8),
        errores.toString(StandardCharsets.UTF_8));
  }

  private Path configuracionCon(String... rutas) throws IOException {
    var servicios = new StringBuilder();
    for (int i = 0; i < rutas.length; i++) {
      servicios.append(i == 0 ? "" : ",");
      String url = "http://127.0.0.1:" + puerto + rutas[i];
      servicios.append("{\"nombre\": \"s%d\", \"url\": \"%s\"}".formatted(i, url));
    }
    Path archivo = carpeta.resolve("servicios.json");
    Files.writeString(archivo, "{\"servicios\": [" + servicios + "]}", StandardCharsets.UTF_8);
    return archivo;
  }

  private static Reporte reporteConocido() {
    var servicios =
        List.of(
            resultado("catalogo", "http://127.0.0.1:8089/catalogo", new Respondio(200, 142)),
            resultado("inventario", "http://127.0.0.1:8089/inventario", new Respondio(200, 2300)),
            resultado("pagos", "http://127.0.0.1:8089/pagos", new Respondio(200, 87)),
            resultado("reportes", "http://127.0.0.1:8088/", new SinRespuesta("sin conexión")));
    return Reporte.de(servicios);
  }

  private static Resultado resultado(String nombre, String url, Respuesta respuesta) {
    return new Resultado(
        new Servicio(nombre, URI.create(url)), Clasificador.clasificar(respuesta), respuesta);
  }

  @Test
  void laVersionSaleDelPomYElCodigoEsCero() {
    Corrida corrida = correr("--version");
    assertEquals(0, corrida.codigo());
    assertEquals("revisor 1.0.0", corrida.salida().strip());
  }

  @Test
  void unaConcurrenciaInvalidaEsUnErrorDeUsoConCodigoDos() {
    Corrida corrida = correr("--concurrencia", "0");
    assertEquals(2, corrida.codigo());
    assertTrue(corrida.errores().contains("--concurrencia debe ser un entero"), corrida.errores());
    assertEquals("", corrida.salida());
  }

  @Test
  void conTodosArribaElCodigoEsCero() throws Exception {
    Path config = configuracionCon("/bien", "/bien");
    Corrida corrida = correr("--config", config.toString());
    assertEquals(0, corrida.codigo());
    assertTrue(corrida.salida().contains("2 servicios · 2 arriba"), corrida.salida());
  }

  @Test
  void conUnServicioCaidoElCodigoEsUno() throws Exception {
    Path config = configuracionCon("/bien", "/mal");
    Corrida corrida = correr("--config", config.toString());
    assertEquals(1, corrida.codigo());
    assertTrue(corrida.salida().contains("1 caído"), corrida.salida());
  }

  @Test
  void elJsonDeUnReporteConocidoEsElContrato() {
    String esperado =
        """
        {
          "servicios" : [ {
            "nombre" : "catalogo",
            "url" : "http://127.0.0.1:8089/catalogo",
            "estado" : "ARRIBA",
            "tiempo_ms" : 142,
            "detalle" : "200"
          }, {
            "nombre" : "inventario",
            "url" : "http://127.0.0.1:8089/inventario",
            "estado" : "LENTO",
            "tiempo_ms" : 2300,
            "detalle" : "200"
          }, {
            "nombre" : "pagos",
            "url" : "http://127.0.0.1:8089/pagos",
            "estado" : "ARRIBA",
            "tiempo_ms" : 87,
            "detalle" : "200"
          }, {
            "nombre" : "reportes",
            "url" : "http://127.0.0.1:8088/",
            "estado" : "CAIDO",
            "tiempo_ms" : null,
            "detalle" : "sin conexión"
          } ],
          "resumen" : {
            "total" : 4,
            "arriba" : 2,
            "lento" : 1,
            "caido" : 1,
            "promedio_ms" : 843
          }
        }""";
    assertEquals(esperado, Formato.json(reporteConocido()));
  }

  @Test
  void elServidorContestaElReporteComoJson() throws Exception {
    HttpServer propio = Servidor.iniciar(0, RevisorTest::reporteConocido);
    try (HttpClient cliente = HttpClient.newHttpClient()) {
      URI direccion = URI.create("http://127.0.0.1:" + propio.getAddress().getPort() + "/reporte");
      var respuesta =
          cliente.send(HttpRequest.newBuilder(direccion).GET().build(), BodyHandlers.ofString());
      assertEquals(200, respuesta.statusCode());
      assertEquals("application/json", respuesta.headers().firstValue("Content-Type").orElse(""));
      assertEquals(Formato.json(reporteConocido()), respuesta.body());
    } finally {
      propio.stop(0);
    }
  }
}
```

Три неща, за да я четеш с полза. Класът `Corrida` („изпълнение“) е частен *record*, който групира това, което една изпълнена програма е оставила: кода и двата текста. `correr(...)` вика `Revisor.ejecutar` с два потока в паметта и ги декодира в UTF-8; не прекратява никаква JVM. А тестът на JSON сравнява с текстов блок, написан на ръка, **знак по знак** (включително интервалите около двоеточията, които са тези на Jackson): ако някой смени името на ключ, реда на полетата или формата, този тест става червен. Това значи, че JSON е договор, а не детайл на имплементацията.

Както винаги, преди да се довериш на теста, го направи червен. В `Revisor.java` смени `return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;` с `return CODIGO_TODO_ARRIBA;` (програмата „винаги казва, че всичко е наред“) и стартирай тестовете:

```text
[ERROR] mx.habil.revisor.RevisorTest.conUnServicioCaidoElCodigoEsUno -- Time elapsed: 0.154 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
[ERROR] Tests run: 34, Failures: 1, Errors: 0, Skipped: 0
```

Единственият, който става червен, е този за кода при паднала услуга, с числата, които трябваше: очакваше 1 и получи 0. Върни реда и се връща към зелено: това са **34** теста, 28 от урок 9 и 6 от днес.

#### 10.3.5 Изпробване на ръка

С 34-те теста в зелено `mvn -B verify` вече остави и библиотеките в `target/lib/` (копието от урок 8), така че `revisor` върви с обичайния *classpath*. С включена лаборатория, в един терминал:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor --servir 8090
sirviendo http://127.0.0.1:8090/reporte (Ctrl+C para detener)
```

а в друг, `curl -i http://127.0.0.1:8090/reporte` връща (съкратено):

```text
HTTP/1.1 200 OK
Date: Tue, 13 Oct 2026 21:12:08 GMT
Content-type: application/json
Content-length: 736

{
  "servicios" : [ {
    "nombre" : "catalogo",
    "url" : "http://127.0.0.1:8089/catalogo",
    "estado" : "ARRIBA",
    "tiempo_ms" : 176,
    "detalle" : "200"
  }, {
```

Заявка към друг път връща `404`, а `POST` към същия `/reporte` — `405`. За да провериш, че слуша **само** на локалния интерфейс, на твоя Mint можеш да поискаш от `ss` портовете, които слушат, филтрирайки 8090 (`ss -ltn 'sport = :8090'`): локалният адрес трябва да е `127.0.0.1:8090`, а не `0.0.0.0:8090`, нито `*:8090`. На машината, където измервах, която не е Linux, еквивалентът `lsof -nP -iTCP:8090 -sTCP:LISTEN` каза `TCP 127.0.0.1:8090 (LISTEN)`.

И затварянето. Когато му изпратих сигнала, който праща `Ctrl`+`C` (`SIGINT`), терминалът показа:

```text
servidor detenido
```

а кодът на изход беше **130**. С `kill -TERM`, сигналът, който пращат управителите на услуги, също се появи `servidor detenido` и кодът беше **143**. Не са грешки на `revisor`: това е конвенцията на терминала, който, когато един процес умре от сигнал, съобщава `128 + номера на сигнала` (2 за `SIGINT`, 15 за `SIGTERM`), както документира същият наръчник на `bash`. Ако в един скрипт твоят `revisor` в режим сървър завърши с 143, значи са го спрели, а не че е пропаднал. След като го спреш, порт 8090 остава свободен: проверих го със същата команда, която не връща нито един ред. Във втората сесия същата команда се скъсява до `java -jar target/revisor.jar --servir 8090` (10.6).

*(Тук свършва първата сесия.)*

### 10.4 Качество: Checkstyle и SpotBugs вътре във `verify`

#### 10.4.1 Две различни врати

Това, че една програма работи, не значи, че е добре направена. Има два вида проблем, които тестовете не виждат и които два автоматични инструмента виждат.

**Checkstyle** проверява **формата** на изходния код: отстъпът да е от два интервала, редовете да не надхвърлят 100 колони, имената да следват конвенцията, всеки публичен клас и метод да има своя Javadoc коментар. Прави го срещу конфигурация; тази, която ползва курсът, е `google_checks.xml`, която идва вътре в Checkstyle и кодира [стиловото ръководство на Google за Java](https://google.github.io/styleguide/javaguide.html). Затова от урок 1 целият код на курса носи отстъп от два интервала: за да се отвори вратата днес, без да се пренаписва нищо. Общият стил не е естетика, а това, което позволява да четеш кода на друг човек, без да се спъваш, и една разлика в преглед да е истинска разлика, а не на интервали.

**SpotBugs** проверява **вероятното поведение**: не чете текста, а вече компилирания *байткод (bytecode)*, и търси шаблони, които обикновено са дефекти: ресурс, който не се затваря, `null`, който се дереферира, променлива колекция, изложена навън, неправилен форматен низ. Всеки шаблон има код с главни букви (като `OS_OPEN_STREAM`), който може да се потърси в [каталога му](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html). Не замества тестовете: покрива друго. Един тест казва „този случай излиза добре“; един статичен анализ казва „този вид грешка не е никъде в кода“, дори по пътища, които нито един тест не обхожда.

#### 10.4.2 `pom.xml` на доставката

Двата инструмента се свързват с цикъла на Maven във фазата `verify` (последната, която ползваш от урок 6), така че `mvn -B verify` вече не значи „компилирай и мини тестовете“, а „мини всички врати“. Това е пълният `pom.xml` на версия 1.0.0:

```xml fig10_04/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>1.0.0</version>

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
      <groupId>tools.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>3.2.3</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
      </resource>
    </resources>

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

      <plugin>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.6.2</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <finalName>revisor</finalName>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>mx.habil.revisor.Revisor</mainClass>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>3.6.0</version>
        <dependencies>
          <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>14.3.0</version>
          </dependency>
        </dependencies>
        <configuration>
          <configLocation>google_checks.xml</configLocation>
          <violationSeverity>warning</violationSeverity>
        </configuration>
        <executions>
          <execution>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <groupId>com.github.spotbugs</groupId>
        <artifactId>spotbugs-maven-plugin</artifactId>
        <version>4.10.4.1</version>
        <executions>
          <execution>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

Пет неща в `pom.xml` имат значение, в този ред:

1. **Версията е `1.0.0`** и е единственото място, където се пише. Блокът `<resources>` с `<filtering>true</filtering>`, който добави в 10.3.3, кара Maven да замени `${project.version}` във файловете на `src/main/resources`; така `revisor.properties` завършва, казвайки `version=1.0.0`, без никой да го пише два пъти.
2. **`maven-dependency-plugin` изчезна.** Зависимостите вече не се копират в `target/lib/`; пакетирането от 10.6 ги включва вътре в JAR.
3. **`maven-checkstyle-plugin` 3.6.0 носи собствена зависимост: Checkstyle 14.3.0.** Не е украшение. *Плъгинът* носи по подразбиране по-стар Checkstyle, който не разбира синтаксиса на Java 25 и пропада при четене на всеки файл с шаблоните от уроци 3 и 4. Проверих го, като махнах тази зависимост:

   ```text
   [ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: Failed during checkstyle configuration: Exception was thrown while processing .../Clasificador.java: IllegalStateException occurred while parsing file .../Clasificador.java. 15:41: mismatched input 'when' expecting '->': InputMismatchException
   ```

   Това е съобщение, което не споменава никаква версия и сочи ред от твоя код, който е наред: `when` на `switch` с шаблони. Причината е винаги една и съща, анализатор, който е по-стар от езика. Ако видиш грешка от „анализ“ във файл, който се компилира, подозирай версията на инструмента преди кода си.
4. **`<violationSeverity>warning</violationSeverity>`.** Проблемите, които Checkstyle намира с `google_checks.xml`, са предупреждения (*warning*). По подразбиране *плъгинът* чупи изграждането само при грешки и тогава вратата би била отворена, дори да има сто нарушения. С `warning` всяко нарушение чупи `verify`.
5. **`spotbugs-maven-plugin` 4.10.4.1** с целта `check` във `verify`: пуска анализа и чупи изграждането, ако намери нещо.

Когато всичко е наред, `mvn -B verify` завършва така (съкращавам това, което не анализираме):

```text
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] --- shade:3.6.2:shade (default) @ revisor ---
[INFO] --- checkstyle:3.6.0:check (default) @ revisor ---
[INFO] You have 0 Checkstyle violations.
[INFO] --- spotbugs:4.10.4.1:check (default) @ revisor ---
[INFO] BugInstance size is 0
[INFO] BUILD SUCCESS
```

Чети **числата**, а не `BUILD SUCCESS`: 34 теста, 0 нарушения, 0 дефекта. Една врата, която не се е изпълнила, също завършва с успех.

#### 10.4.3 Какво намериха в моя собствен код

Разказвам това, защото е най-полезното в раздела. Първия път, когато пуснах `verify` върху `revisor` от този урок, **Checkstyle отбеляза пет нарушения на формата** (редове над 100 колони и затварящите кавички на един текстов блок, които ръководството на Google иска на собствен ред), които поправих за минути. А когато стана чисто, **SpotBugs намери две неща** в код, който мислех за добър. Това бяха съобщенията му, такива каквито са:

```text
[ERROR] Medium: mx.habil.revisor.Revisor.version() may fail to close stream [mx.habil.revisor.Revisor] At Revisor.java:[line 118] OS_OPEN_STREAM
[ERROR] Medium: Format string should use %n rather than \n in mx.habil.revisor.Revisor.<static initializer for Revisor>() [mx.habil.revisor.Revisor] At Revisor.java:[line 35] VA_FORMAT_STRING_USES_NEWLINE
```

Чете се така: ниво на сериозност (`Medium`), описание, класът и методът, редът и накрая кодът на шаблона. И двете бяха истина, по различни начини. Първото идваше от това, че четях свойствата с `new InputStreamReader(flujo, UTF_8)`: макар `flujo` да беше в `try-with-resources`, четецът, който го обгръща, беше втори обект, който SpotBugs не виждаше затворен. Простият изход беше да се четат свойствата с `propiedades.load(flujo)`, без обвивка (файл със свойства се чете като ISO-8859-1, а номерът на версия е ASCII). Второто беше по-фино: сглобявах текста за помощ с текстов блок и `.formatted(...)`, а блокът носи нови редове (`\n`) вътре във форматен низ, където Java очаква `%n`, новия ред на платформата. Не беше грешка в Linux, но SpotBugs е прав, че е крехкост. Решението беше да махна форматирането: текстът носи маркер `{limite}` и един метод го замества. Двете поправки са в `Revisor.java`, който виждаш в 10.3.3.

Урокът не е, че SpotBugs е по-умен от теб. Урокът е, че **инструментът намира това, което не си търсил**, и че поправянето на находките му почти винаги оставя кода по-прост, а не по-дълъг.

#### 10.4.4 Да четеш какво казват и да не ги изключваш

Когато Checkstyle чупи изграждането, съобщението носи файла, реда и колоната, категорията и името на правилото. Ако махнеш Javadoc коментара на `main`, ето какво казва с JVM на испански:

```text
[WARNING] src/main/java/mx/habil/revisor/Revisor.java:[38,3] (javadoc) MissingJavadocMethod: Falta el comentario Javadoc para 'main'.
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: You have 1 Checkstyle violation.
```

(Текстът излиза на езика на JVM: с `MAVEN_OPTS="-Duser.language=en"` същото съобщение казва `Missing a Javadoc comment for 'main'.`. Името на правилото, `MissingJavadocMethod`, е същото на всеки език и е това, което трябва да търсиш в [списъка с правила](https://checkstyle.org/checks.html).) Затова `main` на `revisor` има Javadoc и затова го има всеки **публичен** клас и метод; останалите класове са на ниво пакет и не го изискват, което е една от причините да се държи всичко затворено (урок 6).

Има второ изкушение, което трябва да се назове и да се отхвърли: **да изключиш правилото**. Всички тези инструменти предлагат начини да замълчат (`@SuppressWarnings`, файлове за изключения, понижаване на сериозността). Ползвай ги само когато находката е фалшива и можеш да кажеш защо; никога за да мине изграждането. Един `verify` в зелено, защото си го накарал да замълчи, не струва повече от такъв, който не се е изпълнил.

И едно предупреждение за мълчанието на SpotBugs, измерено. В урок 4 сложи `List.copyOf` в конструктора на `Reporte`, за да има *record*-ът собствено копие на списъка, и същият урок измери, че SpotBugs мълчи за него в клас на ниво пакет. Тук се вижда защо: **зависи от видимостта.** Ако махна този ред от `Reporte`, който е клас на ниво пакет (без `public`), SpotBugs **не казва нищо**: `BugInstance size is 0`. Ако направя `Reporte` публичен и пак махна реда, тогава казва:

```text
[ERROR] Medium: mx.habil.revisor.Reporte.resultados() may expose internal representation by returning Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 6] EI_EXPOSE_REP
[ERROR] Medium: new mx.habil.revisor.Reporte(List, Resumen) may expose internal representation by storing an externally mutable object into Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 8] EI_EXPOSE_REP2
```

SpotBugs се тревожи за това, което **се вижда отвън на пакета**, а клас на ниво пакет не се вижда. Това учи двойно. Защитното копие си остава правилно, дори инструментът да не го изисква (би го изисквал в деня, в който `Reporte` стане публичен, и тогава вече ще е късно да мислиш за това). И мълчанието на един инструмент е информация за това, което е **проверил**, а не гаранция за всичко останало.

*(За да научиш повече как тези врати се вграждат в поток на доставка, има две статии в блога: [тестове, които улавят грешки](https://www.habil.mx/bg/blog/tests-that-catch-errors-coverage-quality-gate/) и [CI/CD и DevSecOps](https://www.habil.mx/bg/blog/cicd-devsecops-own-infrastructure/).)*

### 10.5 Преглед на сигурността

Сигурността на `revisor` не е добавена накрая: градена е там, където се е появявал рискът. Този раздел събира това, което вече съществува, и го преглежда срещу пълната повърхност за атака, която са четири неща: **това, което влиза** (конфигурацията и опциите), **накъде излиза** (заявките, които прави), **това, което слуша** (сървърът) и **това, което показва** (отчетът).

**1. Това, което влиза: адресът на всяка услуга го пише друг човек.** Това е рискът от урок 8, този на фалшифицираните заявки от страната на сървъра (SSRF, от *server-side request forgery*): ако `revisor` проверяваше всеки адрес, който се появи в `servicios.json`, този, който редактира файла, би го превърнал в посредник, за да стигне до неща, до които той не стига, а `revisor` стига (като услугата за метаданни на един облак, която живее на `169.254.169.254`). Защитите са три, всички от списък на разрешеното и нито една от забраненото: само схеми `http` и `https`; само сървъри от **разрешен списък**; и нито едно последвано пренасочване. [Ръководството на OWASP за SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) съвпада: валидирай срещу разрешеното, не срещу познатото като лошо.

В урок 8 разрешеният списък беше константа. Днес е **конфигурируем**, с `--permitir h1,h2`, и това променя въпроса: кой контролира тази опция? На този, който **стартира** програмата, а не на този, който редактира файла с услугите. Това разделение е причината списъкът да не е вътре в `servicios.json`: един файл не може да си даде разрешение сам. Ако някой ден ти хрумне да четеш разрешения списък от същия файл, който валидира, или от мрежата, си развалил защитата. Може да се провери с трите файла, които придружават урока: един с `ftp://`, един с `file:///etc/passwd` и един с `http://169.254.169.254/latest/meta-data/`. И трите се отхвърлят, преди да се направи една-единствена заявка, с код 2 (виж упражнение 1).

**2. Накъде излиза: пренасочванията.** `HttpClient` има `followRedirects(HttpClient.Redirect.NEVER)` изписано в `Revision.nuevoCliente()`. Това е стойността по подразбиране и въпреки това се изписва нарочно: един разрешен сървър би могъл да отговори „преместих се на друг адрес“, сочейки към такъв, който не е разрешен, а следването на това пренасочване без проверка би заобиколило списъка. `revisor` третира `3xx` като един отговор повече и го класифицира; не го следва.

**3. Това, което слуша: само локалният интерфейс.** Сървърът от 10.2 слуша на `127.0.0.1`. Има риск, присъщ на дизайна, който вече беше обявен: всяко `GET /reporte` задейства пълна проверка, със заявка за услуга. Един злонамерен локален процес или просто небрежен, който иска отчета в цикъл, кара `revisor` да пита всички тези услуги без почивка. Непосредственото смекчаване е локалният интерфейс, който намалява кой може да го направи до програмите на същата машина; следващите (кеш от няколко секунди, лимит на заявките в секунда) не са направени и този урок не ги симулира: казва се, че липсват, и това е първото място, където би трябвало да се продължи да се работи.

**4. Това, което показва: отчетът е публичен за всеки, който го поиска.** JSON включва полето `url` на всяка услуга, такова каквото е във файла. Ако някой ден един адрес носи идентификационни данни (`http://usuario:clave@...`) или `token` в текста на запитването, `/reporte` ще ги покаже. Днес курсът не ги ползва и затова няма филтър; но това е вид решение, което се взема **преди** данните да съществуват. Една вътрешна грешка, обратно, вече е решена: връща `error interno` и нищо от изключението.

**5. Нативната сериализация, която не се ползва.** Java има стар механизъм за запазване и възстановяване на обекти директно, `ObjectOutputStream` и `ObjectInputStream`. Да четеш обекти с `ObjectInputStream` от източник, на който не вярваш, е една от най-сериозните и най-добре документирани уязвимости на платформата, защото четенето може да изпълни код, избран от този, който е написал байтовете ([OWASP го обяснява с примери](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html), а JDK предлага [филтри](https://docs.oracle.com/en/java/javase/25/core/serialization-filtering1.html) от [JEP 290](https://openjdk.org/jeps/290), за да се смекчи). `revisor` никога не я ползва и сигурността му тук не идва от това, че форматът е JSON (има и JSON библиотеки, които, лошо конфигурирани, изграждат обекта, който текстът им поиска), а от три конкретни решения: **не ползва нативната сериализация** никъде; **не включва полиморфната десериализация** на Jackson, опцията, която оставя самия текст да казва от кой клас да се създаде всеки обект; и **валидира конфигурацията поле по поле срещу формата, която очаква** (урок 8 я чете като дърво от `JsonNode` и проверява всяка стойност, без да я съпоставя директно с обекти). Правилото на курса е просто: **данните отвън влизат като структуриран текст и се валидират на ръка, никога като обекти.**

За да продължиш темата, статията в блога за [сигурността на API](https://www.habil.mx/bg/blog/api-security-cto-identity-gateway-tokens/) разглежда идентичността и *токените*, което би било нужно, ако сървърът излезеше от локалната машина.

### 10.6 Изпълнимият JAR

#### 10.6.1 Проблемът с дългия *classpath*

От урок 8 стартираш `revisor` с `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor`. Това е дълъг ред, който изисква да знаеш три неща: къде са твоите класове, къде са библиотеките и кой клас има `main`. И пропада мълчаливо по интересни начини. Ако ти липсва частта с библиотеките, ето какво виждаш:

```text
$ java -cp target/classes mx.habil.revisor.Revisor --config servicios-arriba.json
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
	at mx.habil.revisor.Opciones.leer(Opciones.java:32)
	at mx.habil.revisor.Revisor.ejecutar(Revisor.java:50)
	at mx.habil.revisor.Revisor.main(Revisor.java:44)
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

Чете се отдолу нагоре: клас на Jackson не е в *classpath* и JVM го открива точно когато ѝ трябва. Решението е това, което се дава на този, който получава програмата: **един-единствен файл** с всичко вътре.

#### 10.6.2 Какво е JAR и какво прави `shade`

**JAR** (*Java archive*) е ZIP файл с класове вътре и един специален файл, `META-INF/MANIFEST.MF`, с метаданни. Ако манифестът носи реда `Main-Class: mx.habil.revisor.Revisor`, `java -jar archivo.jar` знае кой клас да стартира. Това е [спецификацията на JAR](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html).

`maven-jar-plugin`, който върви от урок 6, генерира JAR на *проекта*: само твоите класове, без библиотеки и без `Main-Class`. Затова, ако го изпълниш, става това:

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

(На английски, с JVM на този език: `no main manifest attribute, in target/revisor-1.0.0.jar`.) Това е грешката на „JAR без вход“: твоят JAR съществува и е наред, но не казва откъде да започне. И има 32 записа: твоите класове и малко повече.

Това, което е нужно, е [`maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/), който върви после и произвежда **друг** JAR, съдържащ твоите класове *и* тези на всички зависимости, с манифест, който носи `Main-Class`. В `pom.xml` от 10.4 са три реда конфигурация: `finalName` кара файла да се казва `revisor.jar` (без версията, така че командата за употреба да не се променя с всяка версия); `ManifestResourceTransformer` с `mainClass` пише `Main-Class`; а `createDependencyReducedPom` на `false` не позволява на *плъгина* да пренапише `pom.xml` на проекта, страничен ефект, който не ни е нужен.

С него `mvn -B verify` оставя в `target/` пълния JAR. Това са цифрите, които измерих:

```text
$ unzip -p target/revisor.jar META-INF/MANIFEST.MF | grep Main
Main-Class: mx.habil.revisor.Revisor
$ unzip -l target/revisor.jar | tail -1
  6359578                     1337 files
$ unzip -p target/revisor.jar revisor.properties
version=1.0.0
```

1,337 записа и около 2.6 MB, от които 1,176 са под `tools/jackson`. А версията излиза от `pom.xml`, както беше сглобено в 10.3.3.

По време на изграждането `shade` отпечатва предупреждения, които на пръв поглед тревожат: `Discovered module-info.class. Shading will break its strong encapsulation.` и `define 2 overlapping resources: META-INF/LICENSE, META-INF/NOTICE`. И двете са верни и тук безвредни: Jackson публикува описание на модул, което, когато всичко се събере в един JAR, престава да се ползва (ние не ползваме системата от модули и тя не се учи в този курс), а няколко JAR носят собствен лицензен файл със същото име, от който `shade` запазва един. Когато не разбираш едно предупреждение, не го игнорирай по навик: прочети го и реши с подробностите. Това е от тези, които могат да се оставят.

#### 10.6.3 Тестът, който се брои: друга папка

Един JAR е независим само ако можеш да го стартираш **от коя да е папка**. Опитай това, с пълния път на JAR и на конфигурационния файл:

```text
$ cd /tmp
$ java -jar ~/curso-java/revisor/target/revisor.jar --config ~/curso-java/revisor/servicios.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA     128ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 883ms
$ echo $?
1
```

Работи и излиза с 1, защото има една бавна и една паднала услуга. Забележи и какво става **без** `--config`: `servicios.json` е относителен път и от `/tmp` не съществува, така че програмата го казва със своята грешка и своя код:

```text
$ java -jar ~/curso-java/revisor/target/revisor.jar
error de configuración: no existe el archivo servicios.json
$ echo $?
2
```

Това е грешката, която урок 5 обяви, тази на относителния път, който зависи от това къде стоиш, сега със своя код на изход. Това е правилен код 2: не е услуга, която е пропаднала, а неправилна употреба.

### 10.7 Какво предстои: `jpackage`

Един JAR решава доставката на *програмата*, не тази на средата: който го получи, пак се нуждае от инсталирана Java. За този, който няма, JDK носи два инструмента, които остават като любопитство и не се учат: [`jlink`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jlink.html) сглобява съкратена версия на JVM само с модулите, от които програмата ти се нуждае, а [`jpackage`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html) пакетира твоя JAR заедно с тази JVM в нативен инсталатор (например `.deb` за твоя Mint), който не изисква нищо предварително. Те са следващата естествена стъпка, когато предаваш програмата на хора, които не са програмисти. Друг изход, различен, е да се компилира програмата до нативен код, което има свои цени и предимства; статията [кога си струва да се компилира Java до нативен код](https://www.habil.mx/bg/blog/native-java-when-to-use-it-when-not/) ги обсъжда.

## Грешката, която ще видиш

Грешката на урока е тази на JAR без вход, която току-що видя в 10.6.2:

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

Значи, че JAR не декларира кой клас да стартира: изпълнил си тънкия JAR на проекта вместо пълния (`revisor.jar`). Поправя се, като изпълниш другия или като добавиш конфигурацията на `shade` към `pom.xml`. Другите съобщения, които е добре да разпознаваш с един поглед (първите три са на испански, защото ги отпечатва програмата: „грешка при употреба: --concurrencia трябва да е цяло число, по-голямо или равно на 1, а не 0“; „грешка в конфигурацията: файлът servicios.json не съществува“; „не можа да се отвори портът: адресът вече се използва“):

- `error de uso: --concurrencia debe ser un entero mayor o igual que 1, no 0` и код 2: лошо написана опция; това е очакваният изход на `revisor`, а не повреда.
- `error de configuración: no existe el archivo servicios.json` и код 2: изпълнил си от друга папка без `--config` (10.6.3).
- `no se pudo abrir el puerto: Address already in use` и код 2: вече има нещо, което слуша на този порт, може би друг `revisor`, който не си спрял. Проверих го, като стартирах два с `--servir 8090`.
- `NoClassDefFoundError: tools/jackson/core/JacksonException`: липсва Jackson в *classpath*; ползвай пълния JAR (10.6.1).
- `MissingJavadocMethod` на Checkstyle и `EI_EXPOSE_REP` на SpotBugs: вратите за качество от 10.4; съобщението носи правилото и реда.

## Какво се прави погрешно

**Викане на `System.exit` по средата на програмата.** Убива JVM, без никой да може да почисти нищо, и прави невъзможно тестването на това, което го заобикаля. Вика се веднъж, в `main`, с число, изчислено от друг код (10.1.2).

**Смесване на резултата с предупрежденията.** Ако JSON и грешките отиват в същия изход, който пренасочи JSON към файл, ще намери съобщение за грешка по средата. Резултатът в стандартния изход, предупрежденията в изхода за грешки.

**Код на изход, който нищо не различава.** Да завършваш винаги с 0 (или винаги с 1) отнема на програмата полезността ѝ в един скрипт. Три кода с фиксирани и документирани значения струват повече от остроумно решение.

**Слушане на всички интерфейси по небрежност.** `new InetSocketAddress(puerto)` без адрес излага услугата на цялата мрежа. Адресът се пише винаги (10.2.2).

**Дълга кука за изключване или такава, която вика `System.exit`.** Остава да чака себе си. Една кука прави едно късо нещо и завършва (10.3.1).

**Връщане на съобщението на изключение по мрежата.** Това е предаване на пътища, класове и понякога данни. Връща се общ текст, а подробностите отиват в журнал (10.2.3).

**Заглушаване на правило, за да мине `verify`.** Статичният анализ струва, докато му обръщат внимание. Заглушава се само когато находката е фалшива и можеш да обясниш защо (10.4.4).

**Нефиксиране на версията на *плъгин*.** Който я няма, остава на милостта на това, което Maven носи този ден; две машини могат да изградят различни неща. Всички *плъгини* на `pom.xml` носят версия.

**Пакетиране без изпробване от друга папка.** Това, че JAR върви в папката на проекта, не доказва, че е независим (10.6.3).

## Упражнения

### Упражнение 1 — Шестте проверки на курса

Списъкът „Как разбирам, че съм успял“ от програмата са шест условия. Провери ги **едно по едно**, с включена лаборатория, като записваш командата и това, което си видял:

1. `mvn -B verify` завършва в зелено и отчетът казва колко теста са се изпълнили.
2. Проверява 20 услуги едновременно за време, близко до това на най-бавната, а не до сбора (ползвай `servicios-20.json` и `--concurrencia`).
3. Паднала, бавна или отговаряща боклук услуга не събаря отчета (ползвай `servicios-extra.json`, който добавя `facturacion`, `notificaciones` и `basura`).
4. Никоя заявка не може да чака завинаги.
5. Отхвърля адресите, които не са `http` или `https`, и тези, които не са в разрешения списък.
6. Изпълнява се с `java -jar` от друга папка и излиза с 0, ако всичко е горе, и с 1, ако не.

Преди да изпълниш всяка, запиши какво очакваш да видиш. Освен `servicios-extra.json` (10.3.3) и `servicios-20.json` (урок 9) са ти нужни четири малки файла в папката на проекта. Първият има две услуги, които наистина са горе:

```json fig10_04/servicios-arriba.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"}
  ]
}
```

А тези три са адреси, които `revisor` не бива да проверява никога: схема, която не е на мрежата, локален файл и адресът на услугата за метаданни на един облак:

```json fig10_04/servicios-ftp.json
{
  "servicios": [
    {"nombre": "archivo", "url": "ftp://127.0.0.1/datos"}
  ]
}
```

```json fig10_04/servicios-archivo.json
{
  "servicios": [
    {"nombre": "secreto", "url": "file:///etc/passwd"}
  ]
}
```

```json fig10_04/servicios-nube.json
{
  "servicios": [
    {"nombre": "metadatos", "url": "http://169.254.169.254/latest/meta-data/"}
  ]
}
```

### Упражнение 2 — `notificaciones` да не прелива колоната (незадължително)

Погледни таблицата на `servicios-extra.json`: името `notificaciones` има 14 знака, а колоната SERVICIO мери 12, така че името се залепя за състоянието. Промени `Formato`, така че ширината на колоната да се изчислява от най-дългото име, с 12 като минимум, и така че таблицата на четирите канонични услуги да излиза **идентична** с обичайната.

### Упражнение 3 — Отвори всяка врата нарочно

Счупи, едно по едно, тези три компонента на качеството, като **предсказваш преди това** какво ще каже изграждането: (а) махни Javadoc коментара на `main`; (б) махни зависимостта на Checkstyle 14.3.0 от `pom.xml`; (в) махни реда `resultados = List.copyOf(resultados);` от `Reporte`. Във всеки случай запиши кой инструмент е проговорил, какво е казал и дали те е изненадало, че не е проговорил.

### Упражнение 4 — Спри сървъра по два начина

Вдигни `revisor` с `--servir 8090`. Спри го първо с `Ctrl`+`C` и прочети кода на изход; вдигни го отново и го спри сега от друг терминал с `kill -TERM`. Обясни двете числа и провери, че портът е останал свободен.

## Решения

### Решение 1

**1. `mvn -B verify` в зелено, с броя, прочетен от отчета.** Важните са числата, а не кодът на изход (затова се броят два пъти, с два метода):

```text
$ mvn -B verify
...
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[INFO] You have 0 Checkstyle violations.
[INFO] BugInstance size is 0
[INFO] BUILD SUCCESS
$ grep -h -o ' tests="[0-9]*"' target/surefire-reports/TEST-*.xml | awk -F'"' '{s+=$2} END {print s}'
34
$ cat target/surefire-reports/TEST-*.xml | grep -c '<testcase '
34
$ grep -h -o ' skipped="[0-9]*"' target/surefire-reports/TEST-*.xml | sort | uniq -c
   7  skipped="0"
```

Седем тестови класа, 34 теста, нито един пропуснат.

**2. Двайсет услуги, последователно срещу едновременно.** Двайсетте отнемат по половин секунда. С `--concurrencia 1` (равносилно на последователно), с 10 и с 20, като се мери цялата програма с `time`:

```text
$ TIMEFORMAT='%R s'
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 1 > /dev/null
10.398 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 10 > /dev/null
1.245 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 20 > /dev/null
0.738 s
```

(Вторите стартирания: 10.385, 1.253 и 0.747.) Сборът на времената е 10 s; с лимит 20 програмата отнема колкото най-бавната, плюс около 0.2 s за стартиране на JVM. Това, което програмата на курса иска, „близо до най-бавната, а не до сбора“, се изпълнява: 0.74 s срещу 10.4 s.

**3. Това, което пропада, не събаря отчета.** С трите допълнителни услуги таблицата излиза пълна и в същия ред; `facturacion` отговаря 503, `notificaciones` изчерпва таймаута си от 1000 ms (маршрутът на лабораторията отнема 10 секунди) и `basura` затваря връзката без да отговори:

```text
$ java -jar target/revisor.jar --config servicios-extra.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA     126ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      128ms  503
notificacionesCAIDO          —  tiempo agotado
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 694ms
$ echo $?
1
```

(Името `notificaciones` прелива колоната: това поправя упражнение 2.) За да се види, че **редът** не се променя дори с различна конкурентност, имената се изваждат от JSON с три различни лимита:

```text
$ for c in 1 3 10; do java -jar target/revisor.jar --config servicios-extra.json --concurrencia $c --formato json | grep -o '"nombre" : "[a-z]*"' | cut -d'"' -f4 | tr '\n' ' '; echo; done
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
```

(Времената в колоната ВРЕМЕ (TIEMPO) се променят при всяко стартиране и затова средното на тази таблица не е 656, което би излязло с фиксираните числа от урок 4: то е на измерените времена.)

**4. Никоя заявка не чака завинаги.** Проверява се по два начина. По кода: има само едно място, където `revisor` изпраща заявка, и това място фиксира двата таймаута.

```text
$ grep -n "connectTimeout\|\.timeout(" src/main/java/mx/habil/revisor/Revision.java
31:        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
39:            .timeout(Duration.ofMillis(servicio.timeoutMs()))
$ grep -rn "\.send(" src/main
src/main/java/mx/habil/revisor/Revision.java:44:      var respuesta = cliente.send(peticion, BodyHandlers.discarding());
```

И по часовника: маршрутът `/notificaciones` на лабораторията отнема 10 секунди да отговори и въпреки това целият отчет със седемте услуги завърши за **2.5 секунди** (`time` даде 2.525 s): лимитът от 1000 ms на услугата го отряза, а най-бавната от останалите, `inventario`, определя остатъка.

**5. Отхвърля това, което не е `http`, нито е разрешено.** Три файла, три отхвърляния, нито един не прави заявка, и трите с код 2:

```text
$ java -jar target/revisor.jar --config servicios-ftp.json
error de configuración: servicio 1: 'url' debe empezar con http:// o https://
$ java -jar target/revisor.jar --config servicios-archivo.json
error de configuración: servicio 1: 'url' debe empezar con http:// o https://
$ java -jar target/revisor.jar --config servicios-nube.json
error de configuración: servicio 1: el servidor 169.254.169.254 no está en la lista permitida
$ echo $?
2
```

**6. От друга папка, с двата кода.** Случая с код 1 вече го видя в 10.6.3. За 0 е нужна конфигурация, в която всичко е горе (`servicios-arriba.json`, с `catalogo` и `pagos`):

```text
$ cd /tmp
$ java -jar ~/curso-java/revisor/target/revisor.jar --config ~/curso-java/revisor/servicios-arriba.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     166ms  200
pagos       ARRIBA     114ms  200

2 servicios · 2 arriba · 0 lentos · 0 caídos · promedio 140ms
$ echo $?
0
```

Шестте се изпълняват. Запиши в дневника датата и броя на тестовете от първата точка: това е доказателството, че си завършил.

### Решение 2

Шаблонът се отделя от формата на ширината, а `tabla` изчислява ширината от най-дългото име (плюс два интервала разделител), с 12 като минимум. С четирите канонични услуги най-дългото име мери 10 и изчислението дава 12: таблицата не се променя. Това са частите на `Formato.java`, които се променят (функциите `encabezado()` и `linea(Resultado)` без ширина се заместват с тези отдолу, които я получават):

```java
  private static final int ANCHO_NOMBRE_MINIMO = 12;

  private static String patron(int anchoNombre) {
    return "%-" + anchoNombre + "s%-10s%6s  %s";
  }

  static String encabezado(int anchoNombre) {
    return String.format(
        Locale.ROOT, patron(anchoNombre), "SERVICIO", "ESTADO", "TIEMPO", "DETALLE");
  }

  static String linea(Resultado resultado, int anchoNombre) {
    return String.format(
        Locale.ROOT,
        patron(anchoNombre),
        resultado.servicio().nombre(),
        resultado.estado(),
        tiempo(resultado.respuesta()),
        resultado.detalle());
  }

  static String tabla(Reporte reporte) {
    int masLargo =
        reporte.resultados().stream()
            .mapToInt(r -> r.servicio().nombre().length())
            .max()
            .orElse(0);
    int ancho = Math.max(ANCHO_NOMBRE_MINIMO, masLargo + 2);
    String renglones =
        reporte.resultados().stream()
            .map(r -> linea(r, ancho))
            .collect(Collectors.joining("\n"));
    return encabezado(ancho) + "\n" + renglones + "\n\n" + resumen(reporte.resumen());
  }
```

С тази промяна 34-те теста остават зелени, Checkstyle дава 0 нарушения, а SpotBugs 0 дефекта, и таблицата на `servicios-extra.json` излиза подравнена:

```text
SERVICIO        ESTADO    TIEMPO  DETALLE
catalogo        ARRIBA     175ms  200
inventario      LENTO       2.3s  200
pagos           ARRIBA     122ms  200
reportes        CAIDO          —  sin conexión
facturacion     CAIDO      125ms  503
notificaciones  CAIDO          —  tiempo agotado
basura          CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 690ms
```

### Решение 3

(а) Без Javadoc на `main` проговаря **Checkstyle**: `MissingJavadocMethod: Falta el comentario Javadoc para 'main'.` („Липсва Javadoc коментарът за 'main'“), едно нарушение, и `verify` пропада. Това е очакваното: `main` е публичен.

(б) Без зависимостта на Checkstyle 14.3.0 проговаря **Checkstyle, но с грешка при анализ, а не на стил**: `mismatched input 'when' expecting '->'` в `Clasificador.java`. Тази грешка изненадва най-много, защото не споменава версията и сочи към правилен код (10.4.2).

(в) Без защитното копие **не проговаря никой**: `BugInstance size is 0`. Това е изненадата, която се обяви: `Reporte` е клас на ниво пакет, а SpotBugs се оплаква само от това, което се вижда отвън. Копието трябва да остане; инструментът мълчи, но причината, поради която съществува, не се е променила. Ако направиш `Reporte` публичен (с конструктора му), тогава се появяват `EI_EXPOSE_REP` и `EI_EXPOSE_REP2` (10.4.4).

### Решение 4

С `Ctrl`+`C` се появява `servidor detenido` и кодът е **130**; с `kill -TERM` (например `kill -TERM $(pgrep -f 'revisor.jar --servir')` от друг терминал) също се появява `servidor detenido` и кодът е **143**. Двете числа са `128 + сигналът` (2 и 15), конвенцията на терминала за процесите, които умират от сигнал; не са повреди. След като го спреш, на твоя Mint `ss -ltn 'sport = :8090'` не бива да показва нито един ред: коректното спиране е освободило порта. (Измерих кодовете, като пращах сигналите от скрипт; командата `ss` не я изпълних на машината за измерване.)

## Как разбирам, че съм успял

- [ ] `java fig10_01.java --falla` отпечатва грешката за употреба и `echo $?` казва 2, и мога да кажа защо грешката се пише със `System.err`.
- [ ] `java fig10_02.java` и `java fig10_03.java` завършват с изходите на урока и мога да обясня за какво служи всяка от петте стъпки на минималния сървър и кога се изпълнява една кука за изключване.
- [ ] **(1)** `mvn -B verify` казва `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`, `You have 0 Checkstyle violations` и `BugInstance size is 0`, и броят на тестовете, преброен в XML на `surefire-reports`, също дава 34.
- [ ] **(2)** С `servicios-20.json`, `--concurrencia 1` отнема около 10 s, `--concurrencia 10` около 1.2 s и `--concurrencia 20` около 0.7 s (приблизително, според моята машина), и ги измерих аз с `time`.
- [ ] **(3)** С `servicios-extra.json` таблицата излиза пълна, със седемте имена в същия ред при коя да е `--concurrencia`, и кодът на изход е 1.
- [ ] **(4)** Мога да посоча в `Revision.java` двата реда, които фиксират времето за свързване и таймаута, и проверих, че отчетът с `notificaciones` (маршрут от 10 s) завършва за около 2.5 s.
- [ ] **(5)** Трите файла `servicios-ftp.json`, `servicios-archivo.json` и `servicios-nube.json` се отхвърлят с очакваното съобщение и кода 2.
- [ ] **(6)** `java -jar` от `/tmp` или от личната ми папка, с пълните пътища, отпечатва таблицата и излиза с 1; със `servicios-arriba.json` излиза с 0.
- [ ] Изпробвах поне една от вратите за качество в червено и разбрах съобщението (Решение 3), включително мълчанието на SpotBugs за `Reporte` без копие.
- [ ] Записах в дневника си коя находка на Checkstyle или SpotBugs не очаквах.

## Резюме

Отговори, без да гледаш урока:

1. Какво получава `main` в `args` и какво Java *не* прави с това?
2. Какво казва по конвенция код на изход `0` и какво различават помежду си `1` и `2` на `revisor`?
3. Защо `main` е един ред, а логиката живее в `ejecutar`? Какво печелят тестовете от това?
4. Защо резултатът отива в стандартния изход, а предупрежденията в изхода за грешки?
5. Каква е разликата между слушането на `127.0.0.1` и липсата на адрес? Какво значи порт 0?
6. Какво е кука за изключване, кога се изпълнява и кога не и защо не бива да вика `System.exit`?
7. Защо главната нишка на режима сървър остава да чака в `CountDownLatch`, който никой не отваря?
8. Какво проверява Checkstyle и какво проверява SpotBugs? Защо мълчанието на SpotBugs за променлива колекция в клас на ниво пакет не доказва, че е наред?
9. Защо разрешеният списък не е вътре във файла с услугите? Защо не се следват пренасочванията?
10. Какво добавя `maven-shade-plugin`, което `maven-jar-plugin` не прави, и как се проверява, че JAR е независим?

## За допълнително четене

- [Oracle, `com.sun.net.httpserver.HttpServer` (Java 25)](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html) и [`Runtime.addShutdownHook`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html): сървърът на JDK и жизненият цикъл на едно изключване. На английски. Посетено на 7 октомври 2026 г.
- [Checkstyle, „Google's Style“](https://checkstyle.org/google_style.html) и [SpotBugs, каталог на шаблоните](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html): какво проверява всяко правило и какво означава всеки код. Посетено на 7 октомври 2026 г.
- [Apache Maven, `maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/) и [спецификацията на JAR](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html): как се сглобява JAR с всичко вътре и какво казва манифестът му. Посетено на 7 октомври 2026 г.
- [OWASP, „Server-Side Request Forgery Prevention“](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) и [„Deserialization“](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html): двата риска от прегледа на сигурността, с техните защити. Посетено на 7 октомври 2026 г.

---

**Предишен:** [Урок 9 — Конкурентност: проверка на всичко наведнъж](09-concurrencia.md) ·
**Индекс на курса:** [Курс по Java](README.md)
