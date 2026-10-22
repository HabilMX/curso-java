# Урок 8 — HTTP и JSON

**Време:** 2 × 90 мин (две сесии; разделянето е между раздели 8.3 и 8.4)

**Какво изграждаш:** `revisor` престава да си измисля отговорите: проверява всяка услуга наистина по HTTP и чете списъка ѝ от файл `servicios.json`, който валидира, преди да му повярва

**Какво научаваш:** `HttpClient` на JDK (един клиент, време за свързване и таймаут за заявка, пренасочвания, кодове на състоянието); как да превърнеш всяка мрежова повреда в кратка подробност, без да изтече изключение; JSON и Jackson 3 (`tools.jackson`) и какво се променя спрямо почти всичко, което има в интернет; как да прочетеш JSON като дърво и да го валидираш на ръка; как да запишеш отчета с *record*-и; и **SSRF: адресът го пише друг човек**

**Откъде идваш.** Носиш проекта на Maven от [Урок 7](07-pruebas-junit.md): пакети, един `pom.xml`, 18 теста в зелено и един `Simulador`, който играе ролята на мрежата с четири измислени отговора. Тази измислица свършва днес. Проверяващ, който никога не говори с мрежата, не проверява нищо; а списък от услуги в обикновен текст, който не прави разлика между число и текст, не издържа на първия файл, написан от някой друг, а не от теб. Този урок решава и двете неща и между другото ти показва частта от работата, която най-често се подценява: **какво да правиш с това, което идва отвън**. Работиш в папката си `~/curso-java/revisor/`.

**Какво не прави този урок.** Не проверява услугите едновременно: `revisarTodos` ги проверява една след друга (последователно) и затова отчетът отнема около 2.5 секунди, сборът от времето на всяка услуга; проверката на всички наведнъж е [Урок 9](09-concurrencia.md). Не чете и тялото на отговорите (на `revisor` му трябват само кодът и колко е продължило), нито има още команден ред: отчетът в JSON съществува и се тества днес, но ще се иска с опция чак в [Урок 10](10-el-programa-terminado.md).

**За стартиранията.** Услугите, които се проверяват, са от **лабораторията** (8.1), локален учебен сървър, който отговаря винаги едно и също; затова СЪСТОЯНИЕТО и ПОДРОБНОСТТА на всеки ред (в програмата са отпечатани на испански като ESTADO и DETALLE) са идентични при всяко стартиране. Това, което се променя между стартиранията, е колоната ВРЕМЕ (TIEMPO), защото е наистина измерено време: ако твоята таблица казва `171ms` там, където тук казва `180ms`, това е нормално. Всичко е изпълнено с Temurin 25, Maven 3.10.0, JUnit 6.1.3 и Jackson 3.2.3. Примерите, които ползват Jackson, се изпълняват от корена на проекта, с `java -cp "target/lib/*" fig08_06.java` (8.5 обяснява защо), а програмите са в [`programas/08-http-y-json/`](../programas/08-http-y-json), в хранилището на курса.

## След урока ще можеш да

- Направиш заявка с `HttpClient`, прочетеш кода на състоянието и измериш колко е продължил отговорът.
- Обясниш разликата между времето за свързване и таймаута на заявката и защо никоя заявка не се изпраща без таймаут.
- Превърнеш всяка мрежова повреда (не се свързва, времето изтича, връзката е затворена) в кратка подробност, без изключение от мрежата да излиза от `revisar`.
- Декларираш зависимостта на Jackson 3, различаваш `tools.jackson` от `com.fasterxml.jackson` и обясниш какво е *classpath* и за какво служи `target/lib`.
- Прочетеш JSON като дърво (`JsonNode`) и го валидираш на ръка и обясниш защо директното му съпоставяне с *record* приема `1.5` и `"1000"` като таймаут.
- Запишеш отчета като JSON с *record*-и и ключове в `snake_case`.
- Обясниш какво е SSRF и приложиш защитите на `revisor`: разрешена схема, разрешен списък от сървъри и пренасочвания, които не се следват.

## Защо, преди как

Досега `revisor` беше актьор. Когато казваше, че `inventario` е отнела 2.3 секунди, го казваше, защото ти си го написал в `Simulador`; от другата страна нямаше никого. Да промениш това е повече от смяна на мястото, откъдето идва едно число, защото реалният свят има четири навика, които симулаторът никога не е имал. **Бави се, когато си поиска:** една услуга може да отговори за 80 милисекунди или за десет секунди. **Не винаги отговаря:** портът може да е затворен, връзката може да се прекъсне по средата, сървърът може да я приеме и да мълчи. **Отговаря странни неща:** една паднала услуга често не спира да отговаря, отговаря с 503. И **понякога пренасочва:** един отговор може да каже „това, което търсиш, вече не е тук, иди на този друг адрес“. `revisor` трябва да класифицира всичко това, без да се срине и без да чака вечно.

Втората половина на урока е друг вид проблем. Досега списъкът с услуги беше `servicios.txt`: име, адрес, незадължителен таймаут, разделени с интервали. Върши работа, докато всичко е текст и всичко е лесно. Но текстовият файл не различава числото `1000` от текста `"1000"`, не може да влага нищо и няма стандартен начин да каже „това поле го няма“. За това съществува **JSON**, най-използваният формат за обмен на данни в интернет, а Java не го носи в стандартната си библиотека: ползва се чужда библиотека и затова Урок 6 е съществувал.

А между двете половини има идея, която ги свързва и която е най-важната в урока: **програмата ти сега прави нещо в света с това, което казва един файл.** Ако файлът казва „провери този адрес“, `revisor` го проверява. Който може да пише в този файл, или във формуляра, който някой ден ще го захранва, контролира къде се свързва програмата ти от твоята мрежа. Този вид повреда има собствено име и е сред десетте най-сериозни на уеб приложенията: нарича се **SSRF** и последният раздел я атакува с четири защити.

Пътят има две сесии. **Първата** обхваща 8.1 до 8.3: лабораторията, `HttpClient`, таймаутите и повредите, и завършва с `revisor`, който наистина проверява. **Втората** обхваща 8.4 до 8.9: JSON, Jackson 3, валидиране, записване и защита. С тази цел пред очи, ето какво отпечатва `revisor` в края на урока, с включена лаборатория:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

СЪСТОЯНИЕТО (ESTADO) и ПОДРОБНОСТТА (DETALLE) са обичайните. Колоната ВРЕМЕ (TIEMPO) вече не са числа, написани от теб: това е, което е измерил компютърът ти.

## Понятията

### 8.1 Лабораторията и първата заявка

#### 8.1.1 HTTP в пет реда

**HTTP** е езикът, на който говорят почти всички услуги в интернет. Един разговор е винаги един и същ: **клиент** (твоята програма) изпраща **заявка**, а **сървър** изпраща **отговор**. Заявката носи **метод**, който казва какво се иска (`GET` е „дай ми това“), **маршрут**, който казва какво (`/pagos`), и няколко заглавни части (header) с допълнителни данни. Отговорът носи **код на състоянието**, който е трицифрено число с резултата; няколко заглавни части; и почти винаги **тяло** (страницата, данните, текстът „ok“).

От кода на състоянието на `revisor` му трябва само първата цифра, която е семейство, според [спецификацията на HTTP](https://www.rfc-editor.org/rfc/rfc9110): `2xx` е успех (`200` е „заповядай“), `3xx` е пренасочване („търси другаде“), `4xx` е грешка на този, който пита (`404` е „това не съществува“), а `5xx` е грешка на сървъра (`503` е „в момента не мога да те обслужа“). Правилото за класификация на `revisor`, което вече имаш, „код 400 или по-голям оставя услугата паднала“, ползва точно това деление: да отговориш `503` е да си паднал, дори да отговориш бързо.

Ето какво наистина пътува. С `curl`, програмата от терминала, която прави заявка и показва отговора, опцията `-i` позволява да видиш кода и заглавните части преди тялото (датата се променя при всяко стартиране):

```text
$ curl -si http://127.0.0.1:8089/pagos
HTTP/1.1 200 OK
Date: Thu, 08 Oct 2026 03:42:51 GMT
Content-length: 3

ok
```

Първият ред: `HTTP/1.1 200 OK`, версията на протокола, кодът и името му. После заглавните части, по една на ред, и празен ред; и след това тялото, което тук е текстът `ok`. Няма нищо повече. Двете неща, които интересуват `revisor`, са `200` и колко е отнело да дойде.

#### 8.1.2 Лабораторията

За да упражняваш с мрежа, има два пътя и двата имат проблем. Да проверяваш публични услуги в интернет прави всяко стартиране различно (днес отговарят, утре не, а времената им зависят от връзката ти) и не е възпитано да упражняваш срещу чужди сървъри. Да напишеш собствен сървър изисква понятия, които още нямаш. Изходът на този курс е една **лаборатория**: малка програма, която вдига HTTP сървър на твоя собствен компютър (`127.0.0.1`, което е името на „този същия компютър“), с услуги, които винаги отговарят едно и също и винаги се бавят еднакво. Така резултатите могат да се сравняват с тези на този урок, ред по ред.

Засега лабораторията е **черна кутия**: ще я стартираш, без да я пишеш. Ползва инструменти, които още не са обяснени (HTTP сървър, който идва в самия JDK, и по една нишка за всяка заявка), и е изключение от правилото да не се ползва нищо, преди да е обяснено (има още едно малко в 8.3.4); [Урок 10](10-el-programa-terminado.md) я отваря и я ползва наистина. Запази я като `~/curso-java/Laboratorio.java`, **едно ниво над папката на проекта**, защото не е част от него:

```java
// fig08_01/Laboratorio.java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Contesta "ok" con el código dado, después de esperar el tiempo dado.
void responder(HttpExchange intercambio, int codigo, long esperaMs) throws IOException {
  try {
    Thread.sleep(esperaMs);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
  byte[] cuerpo = "ok\n".getBytes(StandardCharsets.UTF_8);
  intercambio.sendResponseHeaders(codigo, cuerpo.length);
  try (var salida = intercambio.getResponseBody()) {
    salida.write(cuerpo);
  }
}

// Atiende /espera/<ms>: contesta 200 después de <ms> milisegundos.
void responderEspera(HttpExchange intercambio) throws IOException {
  String ruta = intercambio.getRequestURI().getPath();
  try {
    responder(intercambio, 200, Long.parseLong(ruta.substring("/espera/".length())));
  } catch (NumberFormatException e) {
    responder(intercambio, 400, 0);
  }
}

void main() throws IOException {
  HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 8089), 128);
  servidor.createContext("/catalogo", i -> responder(i, 200, 142));
  servidor.createContext("/inventario", i -> responder(i, 200, 2300));
  servidor.createContext("/pagos", i -> responder(i, 200, 87));
  servidor.createContext("/facturacion", i -> responder(i, 503, 95));
  servidor.createContext("/notificaciones", i -> responder(i, 200, 10_000));
  servidor.createContext("/espera/", this::responderEspera);
  servidor.createContext("/basura", HttpExchange::close);
  servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
  servidor.start();
  IO.println("laboratorio escuchando en http://127.0.0.1:8089");
}
```

И я стартирай **в друг терминал**, който оставяш отворен, докато упражняваш. Остава да работи и чака заявки, докато я спреш с `Ctrl`+`C`:

```text
$ java Laboratorio.java
laboratorio escuchando en http://127.0.0.1:8089
```

Ако вместо това видиш `java.net.BindException: Address already in use`, значи друга програма на компютъра ти вече ползва порт 8089 (**портът** е номерът на „вратата“, през която един сървър слуша). Портът го освобождава **спирането на процеса**, който го държи, а не отварянето на друг терминал: почти винаги е предишна лаборатория, която още работи, и се спира с `Ctrl`+`C` в терминала, където си я стартирал. Ако не намираш този терминал, `ss -ltnp 'sport = :8089'` показва кой процес слуша на 8089 (с името и номера на процеса) и с този номер го спираш. Ето маршрутите ѝ:

| Маршрут | Какво отговаря | И какво ще трябва да заключи `revisor` |
|---|---|---|
| `/catalogo` | `200` след 142 ms | ARRIBA |
| `/inventario` | `200` след 2300 ms | LENTO |
| `/pagos` | `200` след 87 ms | ARRIBA |
| `/facturacion` | `503` след 95 ms | CAIDO (`503`) |
| `/notificaciones` | `200`, но след 10 секунди | CAIDO, ако му се зададе таймаут от 1 s |
| `/basura` | затваря връзката без да отговори | CAIDO (`error de red`) |
| `/espera/<ms>` | `200` след `<ms>` милисекунди | според времето |

(Състоянията ARRIBA, LENTO и CAIDO са думите, които програмата отпечатва: „работи“, „бавна“ и „паднала“; а `error de red` е подробността „мрежова грешка“.)

Липсва една услуга от обичайните четири и не е в лабораторията нарочно: `reportes` сочи към `http://127.0.0.1:8088/`, където **никой не слуша**, за да има услуга, чиято връзка е отказана. Четирите от обичайните таблици са `catalogo`, `inventario`, `pagos` и `reportes`; останалите са за упражнения и тестове.

#### 8.1.3 Първата заявка

С включена лаборатория, това е най-късата заявка, която може да се направи с Java 25:

```java
// fig08_02.java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

void main() throws Exception {
  try (HttpClient cliente = HttpClient.newHttpClient()) {
    HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8089/pagos")).build();
    long inicio = System.nanoTime();
    HttpResponse<String> respuesta = cliente.send(peticion, BodyHandlers.ofString());
    long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
    IO.println("código de estado: " + respuesta.statusCode());
    IO.println("cuerpo: " + respuesta.body().strip());
    IO.println("tardó al menos 87 ms: " + (tiempoMs >= 87));
  }
}
```

Чете се отгоре надолу. **`HttpClient`** е обектът, който умее да говори HTTP; намира се в пакета `java.net.http` на JDK и не се нуждае от никаква зависимост. Отваря се с `try (...)`, `try-with-resources` от Урок 5: един `HttpClient` пази връзки и вътрешна работа и затварянето му ги освобождава; затваряем е (`AutoCloseable`) от Java 21, според [документацията му](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html). **`HttpRequest.newBuilder(URI.create(...)).build()`** сглобява заявката: обект, който вече не се променя, с адреса, към който отива. Ако не кажеш друго, методът е `GET`. **`cliente.send(peticion, BodyHandlers.ofString())`** я изпраща и **чака** отговора; докато не дойде, програмата не напредва. Вторият параметър казва какво да се прави с тялото: `ofString()` го превръща в текст, а резултатът е `HttpResponse<String>`, отговорът, от който се искат `statusCode()` и `body()`.

За да се измери колко е отнело, се взема часовникът преди и след `send`, със `System.nanoTime()`, и се изважда. Ползва се `nanoTime`, а не `currentTimeMillis`, защото [документацията](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/System.html) на `nanoTime` го казва без заобикалки: служи да се измерват **интервали** от време и няма връзка с часа на деня. Часът на един часовник може да скочи (от автоматична корекция, от лятно часово време) по средата на измерване и интервалът би излязъл отрицателен или огромен. `nanoTime` връща наносекунди, а `/ 1_000_000` ги превръща в милисекунди; долната черта в числото е разделител, който Java игнорира, за да е по-четимо.

```text
$ java fig08_02.java
código de estado: 200
cuerpo: ok
tardó al menos 87 ms: true
```

Кодът `200`, тялото `ok`, което лабораторията изпраща, и една проверка: времето е било най-малко 87-те милисекунди, които `/pagos` се бави. (Отпечатва се проверката, а не числото, защото числото се променя при всяко стартиране.) `throws Exception` в `main` е пряк път на този пример, който пропуска всяко изключение; в проекта всяко ще има своя обработка.

### 8.2 `HttpClient` на `revisor`

#### 8.2.1 Един клиент, много заявки

Един `HttpClient` не е само функцията `send`: той е обект, който се сглобява веднъж и пази неща, които си струва да се използват повторно. Документацията го казва: след като е изграден, **е неизменяем** и може да се ползва за изпращане на много заявки, и споделя ресурси между всички, които минават през него (например вече отворените връзки, които не е нужно да се договарят отново). Затова `revisor` създава **един клиент за проверка** и го ползва за всички услуги, а не един за услуга. И се създава с *builder*, когато се иска да се конфигурира нещо, което `newHttpClient()` не конфигурира, а тук е така: трябват време за свързване и изрична политика за пренасочвания.

```java
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }
```

Две конфигурации и двете са решения, не украшения. `connectTimeout` определя колко се чака за **установяването на връзката** (повече в 8.3). `followRedirects(HttpClient.Redirect.NEVER)` казва „ако сървърът отговори с пренасочване, не го следвай“; това е стойността, която клиентът вече носи по подразбиране, и се пише пак, **изрично**, защото е решение за сигурност (8.8), а решение, което някой е написал, е решение, което някой може да прегледа. Стойността по подразбиране, според документацията, е `NEVER`; ако я оставиш имплицитна, в деня, в който някой я промени, никой няма да го забележи.

#### 8.2.2 Какво да се прави с тялото: `BodyHandlers.discarding()`

`send` трябва да знае какво да прави с тялото на отговора и го пита с втори параметър, **обработчика на тялото** (body handler). В предишния пример се ползва `ofString()`, който го чете цялото и го връща като текст. На `revisor` тялото не му трябва: само кодът и времето. За това съществува `BodyHandlers.discarding()`: чете тялото и го **изхвърля**, без да го пази. Зад това има практична причина: ако една услуга отговори с тяло от сто мегабайта, `ofString()` би ги вкарал в паметта на програмата ти; `discarding()` ги чете и не пази нищо. Типът на отговора се променя в съответствие: `HttpResponse<Void>`, отговор, чието тяло е „нищо“ (`Void`, с главна буква, е клас на JDK, който няма никаква възможна стойност освен `null`; ползва се именно в генеричните типове, където параметърът на тип трябва да е клас и не може да е думата `void`, за да се каже „тук няма нищо“).

### 8.3 Когато нещо пропадне: таймаути, изключения и пренасочвания

#### 8.3.1 Два таймаута и защо никоя заявка не се изпраща без тях

Това е разделът, който най-много отделя една учебна програма от такава, която може да се остави да работи. [Документацията на `HttpRequest.Builder`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html) казва за таймаута на една заявка: „ефектът от това да не зададеш таймаут е същият като да зададеш безкраен, тоест **да чакаш завинаги**“. Ако сървърът приеме връзката и мълчи, програмата ти остава да чака, днес, утре и догодина. `revisor`, който проверява услуги, за да знае дали са паднали, и който се вкарва в безкрайно чакане заради една от тях, е по-лош от това да нямаш `revisor`.

Затова има **два** таймаута и всеки покрива различен етап:

| Време | Къде се конфигурира | Какво покрива | Какво хвърля, ако изтече |
|---|---|---|---|
| **За свързване** | в клиента (`connectTimeout`) | установяване на връзката със сървъра | `HttpConnectTimeoutException` |
| **На заявката** | във всяка заявка (`timeout`) | чакане отговорът да дойде | `HttpTimeoutException` |

Връзката е етапът „отговаря ли някой на този адрес?“ и `revisor` ѝ дава две секунди (`TIEMPO_CONEXION_MS = 2000`, константата на `Revision`). Това е решение на програмата, не закон на мрежата: в бавна или претоварена мрежа една връзка може да се установи и по-късно. Константата казва: „ако за две секунди няма връзка, `revisor` спира да чака и отчита `sin conexión`“, което за услуги, които живеят наблизо, като тези на лабораторията, е широк марж. Заявката е етапът „вече ме обслужиха, кога ще ми отговорят?“ и нейното време е **на услугата**: идва от `servicio.timeoutMs()`, което по подразбиране е 5000 и което конфигурационният файл може да промени за услуга, за която се знае, че е бавна. Времето за свързване на един клиент [има ефект само когато трябва да се отвори нова връзка](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.Builder.html); ако вече има отворена от предишна заявка, тя се преизползва и не се прилага.

#### 8.3.2 Какви изключения идват и каква подробност им съответства

`send` декларира две проверявани изключения: `InterruptedException`, което вече познаваш от Урок 5, и `IOException`, което обединява **всичко, което може да се обърка в мрежата**. За да видиш какъв конкретен тип идва във всеки случай, тази програма прави четири заявки към маршрути на лабораторията и записва класа на изключението и неговото съобщение:

```java
// fig08_03.java
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

void intentar(HttpClient cliente, String url, int timeoutMs) throws InterruptedException {
  HttpRequest peticion =
      HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMillis(timeoutMs)).build();
  try {
    int codigo = cliente.send(peticion, BodyHandlers.discarding()).statusCode();
    IO.println(url + " -> contestó " + codigo);
  } catch (IOException e) {
    IO.println(url + " -> " + e.getClass().getName() + ", mensaje: " + e.getMessage());
  }
}

void main() throws InterruptedException {
  try (HttpClient cliente = HttpClient.newHttpClient()) {
    intentar(cliente, "http://127.0.0.1:8089/facturacion", 5000);
    intentar(cliente, "http://127.0.0.1:8088/", 5000);
    intentar(cliente, "http://127.0.0.1:8089/notificaciones", 1000);
    intentar(cliente, "http://127.0.0.1:8089/basura", 5000);
  }
}
```

```text
$ java fig08_03.java
http://127.0.0.1:8089/facturacion -> contestó 503
http://127.0.0.1:8088/ -> java.net.ConnectException, mensaje: null
http://127.0.0.1:8089/notificaciones -> java.net.http.HttpTimeoutException, mensaje: request timed out
http://127.0.0.1:8089/basura -> java.io.IOException, mensaje: HTTP/1.1 header parser received no bytes
```

Четири пътя, четири различни резултата и по един урок във всеки. `/facturacion` **отговори**: отговорът е `503` и няма изключение: един паднал сървър често не изглежда така, защото отговаря, че не може. Портът 8088, където никой не слуша, даде `ConnectException` („не можа да се свърже“) и съобщението му е **`null`**: най-честото мрежово изключение не носи текст, който да е полезен. `/notificaciones` кара клиента да чака 10 секунди, а таймаутът беше 1: `HttpTimeoutException`. А `/basura` отваря връзката и я затваря, без да каже нищо: `IOException`, просто така, със съобщение, което говори за вътрешността на клиента (`HTTP/1.1 header parser received no bytes`).

От тук излизат две правила. Първото е, че `revisor` превежда всеки тип в **една от три фиксирани подробности** и никога не показва съобщението на изключението, по две причини: понякога е `null`, а когато съществува, се променя между версиите на JDK и между операционните системи. Таблицата за превод е онази, която познаваш от първите уроци:

| Изключението | Подробността | Какво се е случило |
|---|---|---|
| `HttpTimeoutException` (включва `HttpConnectTimeoutException`) | `tiempo agotado` (времето изтече) | времето е изтекло, на свързването или на заявката |
| `ConnectException` | `sin conexión` (без връзка) | портът е затворен или името не съществува |
| всяко друго `IOException` | `error de red` (мрежова грешка) | връзката е прекъсната или отговорът не е разбран |

Второто правило е за **реда**. `HttpTimeoutException` и `ConnectException` са и двата типове `IOException`: частни случаи на него. Когато едно изключение може да бъде уловено по частния си тип или по общия, Java ползва първия `catch`, който му върши работа, така че **частният тип е преди общия**. Ако ги поставиш наобратно, компилаторът не те оставя:

```java
// orden/Orden.java
import java.io.IOException;
import java.net.ConnectException;

void probar() {
  try {
    throw new ConnectException();
  } catch (IOException e) {
    IO.println("general");
  } catch (ConnectException e) {
    IO.println("particular");
  }
}

void main() {
  probar();
}
```

```text
$ java Orden.java
Orden.java:9: error: exception ConnectException has already been caught
  } catch (ConnectException e) {
    ^
1 error
error: compilation failed
```

`exception ConnectException has already been caught`: `catch (IOException)` отгоре вече улавя всички `ConnectException`, така че този отдолу никога не би се изпълнил. Затова винаги се подрежда от частното към общото. С тази таблица и това правило централният метод на `revisor` изглежда така:

```java
  static Resultado revisar(HttpClient cliente, Servicio servicio) throws InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(servicio.url())
            .timeout(Duration.ofMillis(servicio.timeoutMs()))
            .GET()
            .build();
    long inicio = System.nanoTime();
    Respuesta respuesta;
    try {
      HttpResponse<Void> http = cliente.send(peticion, BodyHandlers.discarding());
      long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
      respuesta = new Respondio(http.statusCode(), tiempoMs);
    } catch (HttpTimeoutException e) {
      respuesta = new SinRespuesta("tiempo agotado");
    } catch (ConnectException e) {
      respuesta = new SinRespuesta("sin conexión");
    } catch (IOException e) {
      respuesta = new SinRespuesta("error de red");
    }
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }
```

Чети го по етапи. Сглобява се заявката, с таймаута **на услугата** (`timeout`). Взема се часовникът. Изпраща се; ако е имало отговор, измерва се времето и се изгражда `Respondio` с кода и милисекундите (`/ 1_000_000` превръща наносекундите). И всяка повреда става `SinRespuesta` със своята подробност. Накрая състоянието не се решава тук: отговорът, какъвто и да е, се подава на `Clasificador` от Урок 3, който не знае нищо за мрежи. Две свойства имат по-голямо значение от останалите. Първото: **`revisar` не оставя да излезе никое `IOException`**; мрежовата повреда е резултат, а не грешка на програмата (същата идея, която Урок 5 нарече „резултат срещу изключение“). Второто: `InterruptedException` **се предава нагоре**, както се научи, защото който прекъсва нишката, има право програмата да спре.

#### 8.3.3 Пренасочвания: `3xx` е отговор

Остава един от четирите навика на реалния свят. Понякога един сървър отговаря с код `3xx` и заглавна част `Location`, която казва „сега съм на този друг адрес“. Един браузър го следва, без да забележиш. Един HTTP клиент може да направи същото, ако му се поиска. Тази програма вдига собствен сървър, на свободен порт, който системата избира, с един маршрут, който пренасочва към друг, и пита с два клиента:

```java
// fig08_04.java
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

int consultar(HttpClient cliente, String url) throws IOException, InterruptedException {
  HttpRequest peticion = HttpRequest.newBuilder(URI.create(url)).build();
  return cliente.send(peticion, BodyHandlers.discarding()).statusCode();
}

void main() throws IOException, InterruptedException {
  HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  String base = "http://127.0.0.1:" + servidor.getAddress().getPort();
  servidor.createContext("/viejo", intercambio -> {
    intercambio.getResponseHeaders().add("Location", base + "/nuevo");
    intercambio.sendResponseHeaders(302, -1);
    intercambio.close();
  });
  servidor.createContext("/nuevo", intercambio -> {
    intercambio.sendResponseHeaders(200, -1);
    intercambio.close();
  });
  servidor.start();
  try (HttpClient porOmision = HttpClient.newHttpClient();
      HttpClient siguiendo =
          HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
    IO.println("cliente por omisión:      " + consultar(porOmision, base + "/viejo"));
    IO.println("cliente que las sigue:    " + consultar(siguiendo, base + "/viejo"));
  } finally {
    servidor.stop(0);
  }
}
```

```text
$ java fig08_04.java
cliente por omisión:      302
cliente que las sigue:    200
```

Клиентът по подразбиране **не** последва пренасочването: отговори `302`, кода на отговора, който получи. Този, който беше конфигуриран с `Redirect.NORMAL`, отиде на новия адрес и върна крайния `200`. `revisor` е от първия вид и правилото за класификация го казва недвусмислено: `3xx` е **отговор**, не се следва и не е грешка. Услуга, която пренасочва, е жива. Има и по-сериозна причина за сигурност, която се обяснява в 8.8: адрес, който файлът е разрешил, не бива непременно да води до такъв, който файлът не е разрешил.

#### 8.3.4 `revisarTodos` и тестът срещу истински сървър

С готов `revisar`, `revisarTodos` става къс: един клиент, един `for`, редът на списъка.

```java
// fig08_10/src/main/java/mx/habil/revisor/Revision.java

package mx.habil.revisor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;

/** Consulta los servicios por HTTP y arma sus resultados. */
final class Revision {

  /** Cuánto se espera para establecer la conexión, antes de decir que no hay respuesta. */
  static final long TIEMPO_CONEXION_MS = 2000;

  private Revision() {}

  /** Un cliente HTTP con tiempo de conexión y sin seguir redirecciones. */
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  /** Consulta un servicio y mide cuánto tarda; ninguna falla de red sale de este método. */
  static Resultado revisar(HttpClient cliente, Servicio servicio) throws InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(servicio.url())
            .timeout(Duration.ofMillis(servicio.timeoutMs()))
            .GET()
            .build();
    long inicio = System.nanoTime();
    Respuesta respuesta;
    try {
      HttpResponse<Void> http = cliente.send(peticion, BodyHandlers.discarding());
      long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
      respuesta = new Respondio(http.statusCode(), tiempoMs);
    } catch (HttpTimeoutException e) {
      respuesta = new SinRespuesta("tiempo agotado");
    } catch (ConnectException e) {
      respuesta = new SinRespuesta("sin conexión");
    } catch (IOException e) {
      respuesta = new SinRespuesta("error de red");
    }
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  /** Revisa todos, uno tras otro (en serie), y conserva el orden de la lista. */
  static List<Resultado> revisarTodos(List<Servicio> servicios) throws InterruptedException {
    List<Resultado> resultados = new ArrayList<>();
    try (HttpClient cliente = nuevoCliente()) {
      for (Servicio servicio : servicios) {
        resultados.add(revisar(cliente, servicio));
      }
    }
    return resultados;
  }
}
```

Нарочно е **последователно**: всяка услуга се проверява, когато предишната е приключила. Причината да има `try (HttpClient cliente = nuevoCliente())` около това е затварянето: когато блокът свърши, `close()` изчаква да няма заявки в ход и освобождава всичко. А цената на последователността се вижда и е темата на следващия урок: 142 + 2300 + 87 милисекунди е, грубо казано, колкото отнема отчетът.

Остава да се следи `revisar` с тестове. Тук има трудност, която Урок 7 нямаше: тестът се нуждае от **сървър, който да се държи, както поискаме** (да отговаря бързо, да пропада, да мълчи), и не може да се разчита на лабораторията, която може да е изключена на друг компютър. Решението е тестът да вдигне свой собствен, със същия `HttpServer` на JDK от лабораторията, **вътре в теста**, на порт, който системата избира:

```java
// fig08_10/src/test/java/mx/habil/revisor/RevisionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pruebas de la revisión contra un servidor de verdad, levantado dentro de la prueba. */
class RevisionTest {

  private static HttpServer servidor;
  private static String base;

  @BeforeAll
  static void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/rapido", intercambio -> responder(intercambio, 200, 0));
    servidor.createContext("/falla", intercambio -> responder(intercambio, 503, 0));
    servidor.createContext("/lento", intercambio -> responder(intercambio, 200, 2000));
    servidor.createContext("/basura", HttpExchange::close);
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    base = "http://127.0.0.1:" + servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, int codigo, long esperaMs)
      throws IOException {
    try {
      Thread.sleep(esperaMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    intercambio.sendResponseHeaders(codigo, -1);
    intercambio.close();
  }

  private static Resultado revisar(String url, int timeoutMs) throws InterruptedException {
    Servicio servicio = new Servicio("prueba", URI.create(url), timeoutMs);
    try (HttpClient cliente = Revision.nuevoCliente()) {
      return Revision.revisar(cliente, servicio);
    }
  }

  @Test
  void unaRespuestaRapidaEsArriba() throws Exception {
    Resultado resultado = revisar(base + "/rapido", 5000);

    assertEquals(Estado.ARRIBA, resultado.estado());
    assertEquals("200", resultado.detalle());
  }

  @Test
  void unCodigo503EsCaido() throws Exception {
    Resultado resultado = revisar(base + "/falla", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("503", resultado.detalle());
  }

  @Test
  void unPuertoCerradoEsSinConexion() throws Exception {
    int puertoCerrado;
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      puertoCerrado = socket.getLocalPort();
    }

    Resultado resultado = revisar("http://127.0.0.1:" + puertoCerrado + "/", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("sin conexión", resultado.detalle());
  }

  @Test
  void unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado() throws Exception {
    Resultado resultado = revisar(base + "/lento", 200);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("tiempo agotado", resultado.detalle());
  }

  @Test
  void unaConexionCerradaSinRespuestaEsErrorDeRed() throws Exception {
    Resultado resultado = revisar(base + "/basura", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("error de red", resultado.detalle());
  }
}
```

Три нови идеи. **Порт 0.** Да поискаш `InetSocketAddress("127.0.0.1", 0)` означава „който и да е свободен порт“; системата избира един, а тестът го чете после със `servidor.getAddress().getPort()`. Така никога не се сблъсква с друга програма. **`@BeforeAll` и `@AfterAll`** са анотации на JUnit за код, който се изпълнява **веднъж** преди всички тестове на класа и веднъж след всички; вдигането и спирането на сървъра при всеки тест би било бавно. Методите са `static` заради правило на JUnit. И **пет теста, по един за всеки път**: бърз отговор (ARRIBA), `503` (CAIDO), затворен порт (`sin conexión`), маршрут, който отнема 2 секунди, с таймаут от 200 милисекунди (`tiempo agotado`), и връзка, която се затваря без да отговори (`error de red`). Затвореният порт се получава находчиво: отваря се сокет на порт 0, записва се кой порт му се е паднал и се затваря; за това число вече знаем, че е свободно. (Случаят LENTO не се тества тук; вече го следи `ClasificadorTest`, а да чакаш една секунда наистина в един тест би го направило бавен, без да показва нищо ново.) Един ред на сървъра заслужава бележка: `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` иска всяка заявка да се обслужва в собствена нишка, за да не блокира бавният маршрут останалите; понятието се обяснява в Урок 9, а засега е достатъчно да знаеш, че без този ред маршрутите щяха да се обслужват един по един.

Както винаги, **първо се вижда в червено**. В `Revision.java` махни реда с `.timeout(...)` на заявката и изпълни `mvn -B verify`:

```text
$ mvn -B verify
...
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0 <<< FAILURE! -- in mx.habil.revisor.RevisionTest
[ERROR] mx.habil.revisor.RevisionTest.unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <CAIDO> but was: <LENTO>
[ERROR]   RevisionTest.unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado:92 expected: <CAIDO> but was: <LENTO>
[ERROR] Tests run: 23, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Без таймаут маршрутът `/lento` кара клиента да чака цели 2 секунди, отговорът идва (един `200`, закъснял) и се класифицира LENTO, вместо CAIDO. Тестът очаква `CAIDO` и става червен със съобщение, което казва точно какво не е наред. Това е същата повреда, която в продукция не би станала при бърз тест, а при истинска увиснала услуга и която би се видяла като отчет, който никога не свършва. Върни реда.

#### 8.3.5 Краят на първата сесия

Имаш всичко, за да може `revisor` да проверява наистина. В папката си промени две неща: изтрий `Simulador.java` (работата му я върши мрежата) и направи така, че адресите в `servicios.txt` да сочат към лабораторията (`http://127.0.0.1:8089/catalogo` и така за останалите; `reportes` отива на `http://127.0.0.1:8088/`). Копирай `Revision.java` и `RevisionTest.java` отгоре. С включена лаборатория в друг терминал:

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Те са 23 теста: 18-те от предишния урок плюс петте нови на `RevisionTest`. Тези на `ConfiguracionTest` все още са тези на текстовия файл; във втората сесия се пренаписват. И `revisor` вече проверява наистина:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      92ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 861ms
```

Две неща за забелязване. СЪСТОЯНИЕТО и ПОДРОБНОСТТА са идентични с тези от предишния урок, а колоната ВРЕМЕ вече не е тази, която ти написа: измерил я е компютърът ти. И програмата отне, грубо казано, сбора от всички (малко над 2.5 секунди), а не най-бавната: това е цената на проверката последователно. Има още едно доказателство, че проверява наистина: изключи лабораторията и стартирай отново; и четирите услуги излизат CAIDO със `sin conexión`, а средното е тире, защото никой не е отговорил. Това затваря първата сесия.


### 8.4 JSON: защо обикновеният текст вече не стига

#### 8.4.1 Какво не може да каже един текстов файл

`servicios.txt` от Урок 5 имаше една услуга на ред, с полета, разделени с интервали, а третото, незадължително, беше таймаутът. За четири услуги е удобно. Но му задай три въпроса. Как различава числото `1000` от текста `"1000"`? Не го различава: това са едни и същи четири знака и програмата ги превръща, защото *знае*, че там отива число. Как добавя поле, което някои услуги имат, а други нямат, по средата? Не може, без да счупи позицията на останалите. Как казва, че една услуга има списък от неща, като етикети или няколко отговорници? Също не може. Всеки път, когато файлът трябва да каже нещо ново, трябва да се измисли конвенция и да се пренапише четецът.

**JSON** (*JavaScript Object Notation*, макар че днес го ползват всички езици) решава това с проста идея: текстов формат, който **носи изписани типовете си**. Според [нормата му, RFC 8259](https://www.rfc-editor.org/rfc/rfc8259), една JSON стойност е едно от следните неща: **обект** (`{"nombre": "pagos"}`, двойки ключ-стойност между фигурни скоби), **масив** (`[1, 2, 3]`, списък между квадратни скоби), **текст** между двойни кавички, **число** или една от три фиксирани стойности: `true`, `false` и `null` („няма стойност“). Това е целият формат и затова може да се чете от всеки език. И забележи какво се промени: `1000` (без кавички) е число, а `"1000"` (с кавички) е текст, и форматът ги различава от самото начало.

Ето конфигурационния файл на `revisor` от днес нататък, `servicios.json`, с обичайните четири услуги, които сега сочат към лабораторията:

```json fig08_10/servicios.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "inventario", "url": "http://127.0.0.1:8089/inventario"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"},
    {"nombre": "reportes", "url": "http://127.0.0.1:8088/"}
  ]
}
```

Това е обект с един ключ, `servicios`, чиято стойност е масив от обекти, и всеки обект носи `nombre` и `url`. Има трето възможно поле, `timeout_ms`, таймаутът на заявката в милисекунди: **незадължително** е и който го пропусне, получава обичайните 5000 милисекунди. Ключовете са в `snake_case` (думи с малки букви, свързани с долна черта, като в `timeout_ms`), защото са същите, които ползва [курсът по Python](https://github.com/HabilMX/curso-python): файлът и отчетът изглеждат еднакво и в двата. С този файл **`servicios.txt` се изтрива**: вече не се ползва.

Накрая JSON заема място и в **изхода** на `revisor`: отчетът, който програмата ще предаде, за да го прочете друга програма (курсът го фиксира и оттук нататък не се променя). Ще го видиш в 8.7.

### 8.5 Jackson 3: зависимостта, пакетите и *classpath*

#### 8.5.1 Какво е Jackson и защо са два

Java не носи четец на JSON в стандартната си библиотека; ползва се библиотека, а най-използваната е **Jackson**. Тук има капан, който се плаща веднъж: Jackson имаше версия 2, която властва над почти всичко, написано в интернет (стари отговори, уроци, книги), а от 2025 г. има **версия 3**, която смени името на пакета си. Версия 2 живее в `com.fasterxml.jackson...`; версия 3 — в **`tools.jackson...`**. Ако копираш пример от интернет и неговият `import` започва с `com.fasterxml.jackson.databind`, той е от версия 2 и с проекта на този курс не се компилира. Може да се види на два реда:

```java
// fig08_05.java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

void main() throws Exception {
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
  IO.println(raiz.get("servicios").size());
}
```

```text
$ java -cp "target/lib/*" fig08_05.java
fig08_05.java:2: error: package com.fasterxml.jackson.databind does not exist
import com.fasterxml.jackson.databind.JsonNode;
                                     ^
fig08_05.java:3: error: package com.fasterxml.jackson.databind does not exist
import com.fasterxml.jackson.databind.ObjectMapper;
                                     ^
fig08_05.java:6: error: cannot find symbol
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
  ^
  symbol:   class JsonNode
  location: class fig08_05
fig08_05.java:6: error: cannot find symbol
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
                      ^
  symbol:   class ObjectMapper
  location: class fig08_05
4 errors
error: compilation failed
```

`package com.fasterxml.jackson.databind does not exist` и след него каскада от `cannot find symbol`, които са само следствие от първото. Съобщението е обичайното, когато липсва зависимост (видя го в Урок 6), но тук зависимостта на Jackson **е** декларирана; това, което не съществува в проекта ти, е версия 2. Когато пример с Jackson не се компилира, провери първо кой пакет импортира. [Официалното ръководство за миграция към Jackson 3](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) изброява промените: новият пакет, минималният JDK (Java 17), неизменяемите съпоставители (mapper), които се изграждат с *builder*, и че изключенията вече не са проверявани. Остава една подробност наполовина: **анотациите** (`@JsonProperty` и компания) остават в `com.fasterxml.jackson.annotation`, с версия 2.x; това е причината по-долу да видиш файл `jackson-annotations-2.22.jar` до тези на версия 3. Курсът ползва 3.2.3, последната към октомври 2026 г.

#### 8.5.2 Декларирането на зависимостта

**Зависимост** е библиотека, която проектът ти ползва и не си писал. Декларира се в `pom.xml` с нейните координати, както декларира JUnit, но с две разлики: без обхват (обхватът по подразбиране е `compile`, „програмата се нуждае от нея, за да се компилира и да върви“) и с изписана версия. Това е `pom.xml` на проекта от урока, цял; новото е зависимостта на Jackson и един втори *плъгин (plugin)*, който се обяснява веднага:

```xml fig08_10/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.8.0</version>

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
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.11.0</version>
        <executions>
          <execution>
            <id>copiar-dependencias</id>
            <phase>package</phase>
            <goals>
              <goal>copy-dependencies</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.build.directory}/lib</outputDirectory>
              <includeScope>runtime</includeScope>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

Зависимостта, `tools.jackson.core:jackson-databind`, носи други сама: Maven чете `pom.xml` на Jackson, вижда, че се нуждае от `jackson-core` и `jackson-annotations`, и сваля и тях. Това се нарича **транзитивни зависимости** и е причината да напишеш един ред и да се свалят три JAR-а. Версията се вдигна на `0.8.0`, а тази на JUnit продължава да се импортира от същия BOM.

#### 8.5.3 *Classpath* и защо `target/lib`

Да компилираш с Maven работи, защото Maven знае какви зависимости има и ги подава на `javac`. Но **изпълнението** е друго нещо и тук се появява най-типичната грешка на урока. С командата от предишния урок:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError: tools/jackson/core/JacksonException`. JVM започна да изпълнява програмата, стигна до ред, който се нуждае от клас на Jackson, и не го намери. Спомни си какво значеше `-cp`: списъкът от места, където JVM търси класове, ***classpath***. В Уроци 6 и 7 стигаше `target/classes`, защото целият код беше твой. Сега трябва да добавиш JAR-овете на Jackson. Maven ги пази в `~/.m2/repository`, но този път е дълъг и зависи от версията; за да не го пишеш, `pom.xml` добавя *плъгина* `maven-dependency-plugin`, който във фазата `package` **копира зависимостите на програмата в `target/lib`**:

```text
$ ls target/lib
jackson-annotations-2.22.jar
jackson-core-3.2.3.jar
jackson-databind-3.2.3.jar
```

И се изпълнява с двете неща в *classpath*, `target/classes` (твоят код) и `target/lib/*` (JAR-овете; `*` иска „всички `.jar` от тази папка“), разделени с двоеточие, което е разделителят в Linux:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
```

Кавичките са, за да не се опита терминалът да разшири `*` сам. И тъй като JAR-овете се копират във фазата `package`, трябва да си изпълнил `mvn -B verify` (или `package`) поне веднъж, за да съществува `target/lib`. Това е, което пречи при един *classpath* на ръка, и затова [Урок 10](10-el-programa-terminado.md) го решава с един-единствен `.jar`, който носи всичко вътре: вече си го изпитал, така че ще знаеш какъв проблем решава.

### 8.6 Четене на JSON като дърво и валидиране на ръка

#### 8.6.1 `JsonNode`: дървото

Jackson предлага два начина да прочетеш един JSON. Първият го превръща в **дърво** от обекти `JsonNode`, по един за всяка стойност, които можеш да обхождаш и да питаш. Вторият го съпоставя директно с твой *record*. Да започнем с първия, който е този, който ползва конфигурацията:

```java
// fig08_06.java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

void main() {
  String texto = """
      {
        "servicios": [
          {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
          {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos", "timeout_ms": 1000}
        ]
      }
      """;
  JsonMapper mapeador = JsonMapper.builder().build();
  JsonNode raiz = mapeador.readTree(texto);

  JsonNode lista = raiz.get("servicios");
  IO.println("¿es lista? " + lista.isArray() + ", elementos: " + lista.size());

  JsonNode segundo = lista.get(1);
  IO.println("nombre: " + segundo.get("nombre").stringValue());
  IO.println("timeout_ms: " + segundo.get("timeout_ms").intValue());

  IO.println("llave que no existe -> " + lista.get(0).get("timeout_ms"));
  IO.println("la misma con path()  -> " + lista.get(0).path("timeout_ms").isMissingNode());
}
```

```text
$ java -cp "target/lib/*" fig08_06.java
¿es lista? true, elementos: 2
nombre: pagos
timeout_ms: 1000
llave que no existe -> null
la misma con path()  -> true
```

Имената на методите са тези на Jackson 3 (и си струва да се вгледаш, защото почти всичко, което намериш в интернет, ползва тези на 2): `readTree` превръща текста в дървото и връща корена му; `get("servicios")` иска стойността на ключ от обект, а `get(1)` — елемента на масив; `isArray()` пита какъв вид стойност е; `size()` брои елементите; `stringValue()` връща текста на текстов възел, а `intValue()` — цялото число на числов. Старите имена (`asText`, `isTextual`) продължават да съществуват за този, който мигрира, но новият код ползва `stringValue` и `isString`. И има една подробност, която решава много грешки: `get("timeout_ms")` върху ключ, който **не съществува**, връща Java `null`, а не възел; за да питаш без риск от `NullPointerException`, съществува `path("timeout_ms")`, което връща „липсващ“ възел (`isMissingNode()`) вместо `null`. Това, което `revisor` прави с това, е винаги първо да провери за `null` и после да пита за типа.

#### 8.6.2 Капанът: директното съпоставяне с *record* приема това, което не бива

Вторият начин е примамлив, защото е два реда и типовете на Java се грижат за всичко: декларираш *record* с полетата, които искаш, и Jackson го пълни.

```java
// fig08_07.java
import tools.jackson.core.JacksonException;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

record Config(String nombre, int timeoutMs) {}

void probar(JsonMapper mapeador, String valor) {
  String json = "{\"nombre\": \"pagos\", \"timeout_ms\": " + valor + "}";
  try {
    Config config = mapeador.readValue(json, Config.class);
    IO.println("timeout_ms: " + valor + " -> entra como " + config.timeoutMs());
  } catch (JacksonException e) {
    IO.println("timeout_ms: " + valor + " -> rechazado (" + e.getClass().getSimpleName() + ")");
  }
}

void main() {
  JsonMapper mapeador =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
  probar(mapeador, "1000");
  probar(mapeador, "1.5");
  probar(mapeador, "\"1000\"");
  probar(mapeador, "true");
  probar(mapeador, "-5");
}
```

```text
$ java -cp "target/lib/*" fig08_07.java
timeout_ms: 1000 -> entra como 1000
timeout_ms: 1.5 -> entra como 1
timeout_ms: "1000" -> entra como 1000
timeout_ms: true -> rechazado (MismatchedInputException)
timeout_ms: -5 -> entra como -5
```

Погледни петте реда. `1000` влиза като 1000, добре. Но **`1.5` влиза като `1`**: Jackson отрязва десетичната част мълчаливо. **`"1000"` влиза като `1000`**: превръща текста в число без да предупреди. **`-5` влиза като `-5`**: отрицателен таймаут, който `HttpRequest` би отхвърлил с изключение, когато програмата го използва, далеч от мястото, където е написан. Отхвърлен е само `true`, с изключение на Jackson (`MismatchedInputException`). Въпросът не е, че Jackson греши: той прави това, което му е поискано, а именно да *превръща*, а не това, което ти трябва, а именно да *проверява*. Конфигурационен файл с `"timeout_ms": "1000"` почти сигурно е грешка на този, който го е писал, и искаш да го знаеш.

Урокът е общ: **това, което идва отвън, се валидира там, където влиза**, веднъж, с изрични правила, и след тази валидация остатъкът от програмата работи с типове, на които вече може да се вярва (`Servicio` има `int` и `URI`, а не текст, който може да е бил число). Затова конфигурацията се чете като **дърво** и се валидира на ръка с `isInt()`, `isString()` и `isObject()`, а директното съпоставяне с *record*-и остава за **писането** (8.7), където данните ги контролира програмата, а не човек.

#### 8.6.3 Конфигурацията, валидирана

Класът `Configuracion` се пренаписва, за да чете JSON. Чети го като каскада: `leer` отваря файла и валидира общата форма, `leerServicio` валидира една услуга, а всяко поле има своя малък метод.

```java
// fig08_10/src/main/java/mx/habil/revisor/Configuracion.java

package mx.habil.revisor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.JsonNode;

/** Lee la lista de servicios de servicios.json y desconfía de todo lo que trae. */
final class Configuracion {

  /** Los únicos servidores a los que el revisor acepta consultar. */
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");

  private Configuracion() {}

  /** Lee y valida el archivo completo; cualquier problema sale como un solo tipo de error. */
  static List<Servicio> leer(Path ruta, Set<String> permitidos) throws ErrorDeConfiguracion {
    String nombreArchivo = String.valueOf(ruta.getFileName());
    JsonNode raiz = leerArbol(ruta, nombreArchivo);
    JsonNode lista = raiz.get("servicios");
    if (lista == null || !lista.isArray()) {
      throw new ErrorDeConfiguracion("falta la lista 'servicios'");
    }
    List<Servicio> servicios = new ArrayList<>();
    for (int i = 0; i < lista.size(); i++) {
      servicios.add(leerServicio(lista.get(i), i + 1, permitidos));
    }
    verificarNombresUnicos(servicios);
    return servicios;
  }

  private static JsonNode leerArbol(Path ruta, String nombreArchivo) throws ErrorDeConfiguracion {
    String texto;
    try {
      texto = Files.readString(ruta, StandardCharsets.UTF_8);
    } catch (NoSuchFileException e) {
      throw new ErrorDeConfiguracion("no existe el archivo " + nombreArchivo, e);
    } catch (IOException e) {
      throw new ErrorDeConfiguracion("no se pudo leer el archivo " + nombreArchivo, e);
    }
    try {
      return Json.MAPEADOR.readTree(texto);
    } catch (JacksonException e) {
      TokenStreamLocation lugar = e.getLocation();
      String donde =
          lugar == null
              ? ""
              : ": renglón %d, columna %d".formatted(lugar.getLineNr(), lugar.getColumnNr());
      throw new ErrorDeConfiguracion(nombreArchivo + " no es JSON válido" + donde, e);
    }
  }

  /** Valida un elemento de la lista y lo convierte en un servicio. */
  static Servicio leerServicio(JsonNode nodo, int posicion, Set<String> permitidos)
      throws ErrorDeConfiguracion {
    String prefijo = "servicio " + posicion + ": ";
    if (!nodo.isObject()) {
      throw new ErrorDeConfiguracion(prefijo + "debe ser un objeto");
    }
    String nombre = leerNombre(nodo, prefijo);
    URI url = leerUrl(nodo, prefijo, permitidos);
    JsonNode timeout = nodo.get("timeout_ms");
    if (timeout == null) {
      return new Servicio(nombre, url);
    }
    if (!timeout.isInt() || timeout.intValue() <= 0) {
      throw new ErrorDeConfiguracion(prefijo + "'timeout_ms' debe ser un entero mayor que 0");
    }
    return new Servicio(nombre, url, timeout.intValue());
  }

  private static String leerNombre(JsonNode nodo, String prefijo) throws ErrorDeConfiguracion {
    JsonNode nombre = nodo.get("nombre");
    if (nombre == null) {
      throw new ErrorDeConfiguracion(prefijo + "falta 'nombre'");
    }
    if (!nombre.isString() || nombre.stringValue().isBlank()) {
      throw new ErrorDeConfiguracion(
          prefijo + "'nombre' debe ser un texto que no esté vacío");
    }
    return nombre.stringValue();
  }

  private static URI leerUrl(JsonNode nodo, String prefijo, Set<String> permitidos)
      throws ErrorDeConfiguracion {
    JsonNode campo = nodo.get("url");
    if (campo == null) {
      throw new ErrorDeConfiguracion(prefijo + "falta 'url'");
    }
    if (!campo.isString()) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    URI url;
    try {
      url = new URI(campo.stringValue());
    } catch (URISyntaxException _) {
      // Sin la causa: su mensaje repite la dirección entera, con usuario, clave y token.
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    String esquema = url.getScheme();
    if (!"http".equals(esquema) && !"https".equals(esquema)) {
      throw new ErrorDeConfiguracion(prefijo + "'url' debe empezar con http:// o https://");
    }
    String servidor = url.getHost();
    if (servidor == null) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    if (!permitidos.contains(servidor)) {
      throw new ErrorDeConfiguracion(
          prefijo + "el servidor " + servidor + " no está en la lista permitida");
    }
    if (url.getRawUserInfo() != null) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no puede llevar usuario ni clave");
    }
    return url;
  }

  private static void verificarNombresUnicos(List<Servicio> servicios)
      throws ErrorDeConfiguracion {
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

Три решения заслужават коментар. **Първото: всяка повреда е `ErrorDeConfiguracion` със съобщение, което казва какво да се поправи и къде.** `servicio 2: falta 'nombre'` е съобщение, което човек може да използва; `NullPointerException` на ред 57 не е. Позицията (`servicio 2`) се брои от 1, както я брои човек. **Второто: грешките на Jackson се превеждат.** `JacksonException` е **непроверявано** изключение във версия 3 (една от разликите с 2), така че компилаторът не те задължава да го улавяш и затова е лесно да го забравиш и да оставиш един лошо написан файл да събори програмата със следа от стека. `catch` го превръща в `ErrorDeConfiguracion` и между другото се възползва от това, че изключението знае на кой ред и колона от файла се е счупило (`getLocation()`). **Третото: файлът се чете първо като текст с `Files.readString`**, с обработката на грешки от Урок 5 (`NoSuchFileException` става „no existe el archivo“, „файлът не съществува“), и едва тогава текстът се подава на Jackson. Всяка грешка има един-единствен произход и всяка се обработва на своето място.

**Четвърто, за сигурността: `url` с потребител или парола се отхвърля при зареждане и никое съобщение не повтаря адреса.** `http://usuario:clave@127.0.0.1:1/estado` има схема `http`, има сървър и сървърът е в разрешения списък; но това, което стои преди кльомбата (`getRawUserInfo()`), е идентификационни данни и `revisor` би ги пренесъл във всичко, което пише. Затова се отхвърля с `'url' no puede llevar usuario ni clave`, съобщение, което не го повтаря, и затова `catch` за `URISyntaxException` вече не подава изключението като причина: съобщението му носи целия адрес, с всичко в него. Проверката идва след разрешения списък нарочно: `http://127.0.0.1@169.254.169.254/` продължава да се отхвърля заради сървъра си, както ще видиш в 8.8.3.

Ето съобщенията, които произвежда. Позицията се брои от 1. Тестовете по-долу следят най-опасните; с останалите ще се занимаеш ти в упражнение 3:

| Случай | Съобщение |
|---|---|
| файлът не съществува | `no existe el archivo servicios.json` |
| лошо написан JSON | `servicios.json no es JSON válido: renglón 4, columna 5` |
| липсва списъкът | `falta la lista 'servicios'` |
| услугата не е обект | `servicio 2: debe ser un objeto` |
| липсва поле | `servicio 2: falta 'nombre'` |
| празно име или такова, което не е текст | `servicio 2: 'nombre' debe ser un texto que no esté vacío` |
| url, който не е `http`/`https` | `servicio 2: 'url' debe empezar con http:// o https://` |
| url без сървър или лошо написан | `servicio 2: 'url' no es válida` |
| сървър извън разрешения списък | `servicio 2: el servidor 169.254.169.254 no está en la lista permitida` |
| url с потребител или парола | `servicio 2: 'url' no puede llevar usuario ni clave` |
| невалиден таймаут | `servicio 3: 'timeout_ms' debe ser un entero mayor que 0` |
| повтарящи се имена | `nombres repetidos: catalogo` |

С файл, на който му липсва запетая между две услуги (тук тази в края на реда на `catalogo`), `revisor` казва точно къде:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

`renglón 4, columna 5` („ред 4, колона 5“): точката, в която Jackson е очаквал запетая и е намерил `{`, което отваря следващата услуга. С празен файл казва какво липсва, а не следа от стека; а със списък от текстове там, където трябва да има обекти, казва коя услуга е лоша:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

Класът с тестове се пренаписва със случаите на JSON. Структурата е същата като в Урок 7, с една новост: таблицата от случаи сега са JSON файлове от няколко реда и таблица с `@CsvSource` с това би била нечетима. JUnit има друг източник на данни, **`@MethodSource`**, който сочи към метод (тук `archivosInvalidos`), който връща `Stream<Arguments>`, списък от редове, където всяко `Arguments.of(...)` носи стойностите на един. Текстовите блокове (`"""`) от Урок 2 позволяват да се напише всеки JSON така, както го виждаш във файл.

```java
// fig08_10/src/test/java/mx/habil/revisor/ConfiguracionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Pruebas de la lectura y validación de servicios.json. */
class ConfiguracionTest {

  @TempDir Path carpeta;

  private Path escribir(String json) throws IOException {
    Path ruta = carpeta.resolve("servicios.json");
    Files.writeString(ruta, json);
    return ruta;
  }

  @Test
  void leeUnArchivoValido() throws Exception {
    Path ruta =
        escribir(
            """
            {"servicios": [
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
              {"nombre": "pagos", "url": "http://localhost:8089/pagos", "timeout_ms": 1000}
            ]}
            """);

    List<Servicio> servicios = Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS);

    assertEquals(2, servicios.size());
    assertEquals("catalogo", servicios.get(0).nombre());
    assertEquals(Servicio.TIMEOUT_POR_OMISION_MS, servicios.get(0).timeoutMs());
    assertEquals(1000, servicios.get(1).timeoutMs());
  }

  static Stream<Arguments> archivosInvalidos() {
    return Stream.of(
        Arguments.of(
            """
            {"servicios": [{"nombre": "a", "url": "ftp://127.0.0.1/a"}]}
            """,
            "servicio 1: 'url' debe empezar con http:// o https://"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a"},
              {"nombre": "b", "url": "http://169.254.169.254/latest/meta-data/"}
            ]}
            """,
            "servicio 2: el servidor 169.254.169.254 no está en la lista permitida"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": 1.5}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": "1000"}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [{"url": "http://127.0.0.1:8089/a"}]}
            """,
            "servicio 1: falta 'nombre'"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/a"},
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/b"}
            ]}
            """,
            "nombres repetidos: catalogo"));
  }

  @ParameterizedTest
  @MethodSource("archivosInvalidos")
  void rechazaUnArchivoInvalido(String json, String mensajeEsperado) throws IOException {
    Path ruta = escribir(json);

    ErrorDeConfiguracion error =
        assertThrows(
            ErrorDeConfiguracion.class,
            () -> Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS));

    assertEquals(mensajeEsperado, error.getMessage());
  }
}
```

Седем теста: щастливият случай и шестте най-опасни отхвърляния: URL, който не е `http`, сървър извън списъка (`169.254.169.254` от 8.8), двата таймаута, които директното съпоставяне би оставило да минат (`1.5` и `"1000"`), услуга без име и повтарящи се имена. Двата на таймаута са причината за съществуването на предишния раздел: ако някой „опрости“ валидацията до директно съпоставяне, тези два реда стават червени.

### 8.7 Записване на отчета като JSON

Писането е другата посока и тук наистина е удобно Jackson да работи вместо теб, защото данните ги произвежда програмата, а не човек. Подава му се *record* и той връща текста. Това е минималният пример, с имената, показани преди валидацията:

```java
// fig08_08.java
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

record Fila(String nombre, String estado, Long tiempoMs, String detalle) {}

record Resumen(int total, Long promedioMs) {}

record Reporte(List<Fila> servicios, Resumen resumen) {}

void main() {
  JsonMapper mapeador =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
  Reporte reporte =
      new Reporte(
          List.of(
              new Fila("pagos", "ARRIBA", 87L, "200"),
              new Fila("reportes", "CAIDO", null, "sin conexión")),
          new Resumen(2, 87L));
  IO.println(mapeador.writerWithDefaultPrettyPrinter().writeValueAsString(reporte));
}
```

```text
$ java -cp "target/lib/*" fig08_08.java
{
  "servicios" : [ {
    "nombre" : "pagos",
    "estado" : "ARRIBA",
    "tiempo_ms" : 87,
    "detalle" : "200"
  }, {
    "nombre" : "reportes",
    "estado" : "CAIDO",
    "tiempo_ms" : null,
    "detalle" : "sin conexión"
  } ],
  "resumen" : {
    "total" : 2,
    "promedio_ms" : 87
  }
}
```

`writerWithDefaultPrettyPrinter()` произвежда текста с отстъпи; без него всичко излиза на един ред (което е предпочитанието на машината). Опцията `SNAKE_CASE`, която се задава **само веднъж** при изграждането на съпоставителя, кара `tiempoMs` да излезе като `tiempo_ms`: *record*-ът се пише в стила на Java (`camelCase`), а JSON — в този на договора. И виж услугата, която не отговори: нейното `tiempo_ms` е `null`, не нула и не липсващо. Това е информация: означава „не е измерено“ и полето от тип `Long` (с главна буква, обвивката от Урок 2, която допуска `null`), а не `long` е това, което го позволява. Интервалите около двоеточията (`"nombre" : "pagos"`) са на форматиращия инструмент на Jackson, които не са тези на други езици; валидни са и всеки четец на JSON ги приема.

В `revisor` има три части. Първата, `Json`, е **съпоставителят** на програмата, един-единствен, конфигуриран веднъж (това са обекти, предвидени да се преизползват):

```java
// fig08_10/src/main/java/mx/habil/revisor/Json.java

package mx.habil.revisor;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/** El lector y escritor de JSON del revisor: uno solo, configurado una vez. */
final class Json {

  /** Escribe los nombres en snake_case: {@code tiempoMs} sale como {@code tiempo_ms}. */
  static final JsonMapper MAPEADOR =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

  private Json() {}
}
```

Втората, `ReporteJson`, е **формата** на JSON, отделена нарочно от вътрешните типове на програмата. `Reporte`, `Resultado` и `Resumen` са класовете, с които програмата работи; `ReporteJson` е договорът с този, който чете отчета. Ако утре промениш нещо вътрешно (как се пазят времената, едно име), JSON не се променя, освен ако ти не решиш, а не случайно:

```java
// fig08_10/src/main/java/mx/habil/revisor/ReporteJson.java

package mx.habil.revisor;

import java.net.URI;
import java.util.List;
import java.util.OptionalLong;

/** La forma del reporte en JSON: es el contrato con quien lo consuma. */
record ReporteJson(List<Fila> servicios, ResumenJson resumen) {

  ReporteJson {
    servicios = List.copyOf(servicios);
  }

  /** Un servicio en el JSON; {@code tiempoMs} es nulo si no hubo respuesta. */
  record Fila(String nombre, String url, Estado estado, Long tiempoMs, String detalle) {}

  /** Las cuentas del JSON; {@code promedioMs} es nulo si nadie respondió. */
  record ResumenJson(int total, int arriba, int lento, int caido, Long promedioMs) {}

  /** Convierte el reporte del programa en su forma de JSON. */
  static ReporteJson de(Reporte reporte) {
    List<Fila> filas =
        reporte.resultados().stream()
            .map(
                r ->
                    new Fila(
                        r.servicio().nombre(),
                        sinSecretos(r.servicio().url()),
                        r.estado(),
                        nulo(r.tiempoMs()),
                        r.detalle()))
            .toList();
    Resumen resumen = reporte.resumen();
    return new ReporteJson(
        filas,
        new ResumenJson(
            resumen.total(),
            resumen.arriba(),
            resumen.lento(),
            resumen.caido(),
            nulo(resumen.promedioMs())));
  }

  /**
   * La dirección sin usuario, clave, consulta ni fragmento: solo esquema, servidor, puerto y
   * ruta. El reporte lo lee quien lo pida, y una consulta puede traer un token.
   */
  static String sinSecretos(URI url) {
    String puerto = url.getPort() == -1 ? "" : ":" + url.getPort();
    String ruta = url.getRawPath() == null ? "" : url.getRawPath();
    return url.getScheme() + "://" + url.getHost() + puerto + ruta;
  }

  private static Long nulo(OptionalLong valor) {
    return valor.isPresent() ? valor.getAsLong() : null;
  }
}
```

Методът `de` превръща `Reporte` във формата му на JSON, а превръщането на `OptionalLong` в `Long` (`nulo`) е това, което произвежда `null` за услугите без отговор. Полето `url` също не е `url().toString()`: то е `sinSecretos(url)`, което сглобява адреса наново със схема, сървър, порт и път и оставя навън потребител, парола, запитване (`?token=...`) и фрагмент (`#...`). JSON е договор с този, който го чете, затова полето остава и запазва формата си; маха се това, което този читател не бива да вижда. С `http://127.0.0.1:1/estado?token=SECRETO#x` в `servicios.json` JSON казва `"url" : "http://127.0.0.1:1/estado"`. Компактният конструктор с `List.copyOf` е защитното копие от Урок 4; Урок 10 показва какво казва (и какво не казва) един инструмент за анализ за него. Третата част е `Formato`, която получава един метод, `json`, и е единствената промяна във файл, чийто остатък вече познаваш:

```java
// fig08_10/src/main/java/mx/habil/revisor/Formato.java

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

  /** El reporte como JSON con sangría, listo para que lo lea otro programa. */
  static String json(Reporte reporte) {
    return Json.MAPEADOR
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(ReporteJson.de(reporte));
  }
}
```

`revisor` още не предлага опция за поискване на този JSON (идва с командния ред на Урок 10), но можеш да го видиш, като временно смениш в `Revisor.java` реда, който отпечатва таблицата, със `System.out.println(Formato.json(reporte));`. Така започва това, което отпечатва:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
{
  "servicios" : [ {
    "nombre" : "catalogo",
    "url" : "http://127.0.0.1:8089/catalogo",
    "estado" : "ARRIBA",
```

Това е JSON, който курсът фиксира за отчета на `revisor`, същият като в [курса по Python](https://github.com/HabilMX/curso-python): `servicios` е списък с един запис за услуга, `tiempo_ms` е `null` за тази, която не е отговорила, а `resumen` повтаря сборовете. Върни реда на таблицата, преди да продължиш.

### 8.8 SSRF: адресът го пише друг човек

#### 8.8.1 Проблемът, с конкретен пример

Погледни отново `servicios.json`. Всеки `url` е адрес, към който **програмата ти се свързва, от твоята мрежа, с правата на твоята машина**. Представи си, че днес файлът го пишеш ти, но утре някой изгражда екран, където друг човек добавя услуги, или файл на друг екип се копира на това място. Този човек вече не пише данни: пише **инструкция за твоя сървър**. Какъв адрес би сложил един нападател? Такъв, до който той не може да стигне отвън, а твоят сървър може. В облака има един много известен: `http://169.254.169.254/latest/meta-data/`. Това е адрес „за локална връзка“, от онези, които не излизат от мрежата на самата машина, и който в AWS (и с подобни механизми в други облаци) отговаря с данни за сървъра и, в някои конфигурации, с временни идентификационни данни; [документацията на AWS](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html) го описва. Един `revisor` без защити би се свързал, би казал `ARRIBA` и би направил това, което нападателят е искал, без никой да пипне сървъра.

Този вид повреда се нарича **SSRF** (*Server-Side Request Forgery*, „фалшифициране на заявки от страната на сървъра“): да накараш един сървър да направи заявка там, където не е трябвало. Тя е на десето място в [списъка OWASP Top 10 от 2021 г.](https://owasp.org/Top10/A10_2021-Server-Side_Request_Forgery_%28SSRF%29/), най-цитирания каталог на рискове на уеб приложенията, и има своя запис в каталога на слабостите в софтуера, [CWE-918](https://cwe.mitre.org/data/definitions/918.html). `revisor` не чете тялото на отговорите, което намалява вредата (нападателят би узнал, че нещо е отговорило, а не какво е казало), но все пак разкрива какво има в мрежата, а програма, която някой ден прочете това тяло, би го предала.

Защитата не е едно нещо, а купчина от четири и всяка покрива това, което предишната оставя да мине. Това се нарича *защита в дълбочина (defense in depth)*.

#### 8.8.2 Първа защита: схемата

Един URL започва със **схема**, частта преди двоеточието: `http`, `https`, `ftp`, `file`. `revisor` говори само HTTP, така че приема две, и го проверява с `URI.getScheme()`, както видя в `Configuracion`. Останалите схеми, някои опасни (`file:///etc/passwd` сочи към файлове на самата машина), се отхвърлят. `HttpClient` вече е предпазлив сам по себе си и това показва последният ред на тази програма, която освен това показва как Java чете един адрес:

```java
// fig08_09.java
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;

void mostrar(String texto) {
  try {
    URI url = new URI(texto);
    IO.println(texto + " -> esquema " + url.getScheme() + ", servidor " + url.getHost());
  } catch (URISyntaxException e) {
    IO.println(texto + " -> no es una URI válida");
  }
}

void main() {
  mostrar("http://127.0.0.1:8089/pagos");
  mostrar("http://LOCALHOST:8089/pagos");
  mostrar("http://127.0.0.1@169.254.169.254/latest/meta-data/");
  mostrar("http://127.0.0.1.otro.example/pagos");
  mostrar("http://[::1]:8089/pagos");
  mostrar("http://127.0.0.1:puerto/pagos");
  mostrar("file:///etc/passwd");
  mostrar("pagos");
  try {
    HttpRequest.newBuilder(URI.create("file:///etc/passwd"));
  } catch (IllegalArgumentException e) {
    IO.println("HttpRequest rechaza file: -> " + e.getMessage());
  }
}
```

```text
$ java fig08_09.java
http://127.0.0.1:8089/pagos -> esquema http, servidor 127.0.0.1
http://LOCALHOST:8089/pagos -> esquema http, servidor LOCALHOST
http://127.0.0.1@169.254.169.254/latest/meta-data/ -> esquema http, servidor 169.254.169.254
http://127.0.0.1.otro.example/pagos -> esquema http, servidor 127.0.0.1.otro.example
http://[::1]:8089/pagos -> esquema http, servidor [::1]
http://127.0.0.1:puerto/pagos -> esquema http, servidor null
file:///etc/passwd -> esquema file, servidor null
pagos -> esquema null, servidor null
HttpRequest rechaza file: -> invalid URI scheme file
```

`file:` не минава дори конструктора на заявката (`invalid URI scheme file`). Но не разчитай само на това: `revisor` отхвърля схемата по-рано, с ясно съобщение, в конфигурацията.

#### 8.8.3 Втора защита: разрешеният списък от сървъри

Със схема `http` още остава `http://169.254.169.254/`. Втората защита е тази, която тежи най-много: **разрешен списък (allowlist)** от сървъри. Вместо да се опитваш да налучкаш кои адреси са опасни (има твърде много и винаги забравяш един), изброяваш единствените, които се приемат, и отхвърляш всичко останало:

```java
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");
```

Днес списъкът е `127.0.0.1` и `localhost`: самата машина, където живее лабораторията. В Урок 10 списъкът става конфигурируем от командния ред. Провери отхвърлянето с файл, който сочи към опасния сървър:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

Преди програмата да изпрати един-единствен пакет, конфигурацията вече е отказала, със съобщение, което казва кой сървър и защо. Забележи как се сравнява: **с `url.getHost()`, сървъра, който анализаторът на URI е извлякъл, и по точно равенство**. Виж какво връща тази функция в предишната програма за хитри адреси: `http://127.0.0.1@169.254.169.254/latest/meta-data/` изглежда, на пръв поглед, като адрес на `127.0.0.1`; но за HTTP това, което е преди знака @, е потребител (потребителят `127.0.0.1`!), а сървърът е `169.254.169.254`. Валидация, направена с `texto.startsWith("http://127.0.0.1")`, би пропуснала точно тази атака; направената с `getHost()` я отхвърля. А `http://127.0.0.1.otro.example/` започва с IP адреса, но е друг сървър, и `http://127.0.0.1:puerto/` дори няма разпознаваем сървър (`revisor` го отхвърля като „no es válida“, „не е валиден“). Правилото: **никога не валидирай адрес с текстови операции; анализирай го с анализатора на URI и сравнявай частите му.**

Списъкът различава главни и малки букви и отхвърля валидни неща, като `http://LOCALHOST:8089/` или формата на самата машина в IPv6, `http://[::1]:8089/`:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

Това е нарочно: един разрешен списък трябва да е **строг по подразбиране**. Ако някой ден се наложи да се приеме друга форма, тя се добавя към списъка нарочно, а не се отпуска сравнението.

#### 8.8.4 Трета защита: да не се следват пренасочвания

Сървър, който е в списъка, може да отговори с пренасочване към такъв, който не е. Ако клиентът го последва, разрешеният списък би бил заобиколен през задната врата: ти си разрешил `127.0.0.1`, а клиентът би завършил в `169.254.169.254`. Затова клиентът на `revisor` ползва изрично `Redirect.NEVER` (8.2.1) и третира `3xx` като един отговор повече (8.3.3): това, което не се следва, не може да се използва срещу теб. Честно предупреждение: **нито един от 25-те теста не следи тази защита**. Измерено е: смяната на `NEVER` с `ALWAYS` в `Revision.java` оставя 25-те в зелено. Следи я програмата от 8.3.3, която не е част от проекта. Да напишеш този тест (тестов сървър, който пренасочва към друг) е добро заключително упражнение.

#### 8.8.5 Четвърта защита и какво това НЕ решава

Четвъртата е минимална: таймаутът на всяка заявка. Един враждебен сървър може да отговаря много бавно нарочно, за да държи програмата ти заета; таймаутът ограничава колко може да злоупотребява с нея.

А сега честното, защото сигурността, която се крие, става фалшива: **тези четири защити са добро начало, не стена.** Разрешеният списък сравнява *имена*, а не истински адреси. `localhost` е име, което системата ти превежда на адрес, като поглежда във файла `/etc/hosts` или в сървър за имена; един враждебен сървър за имена може да отговори с адрес, различен от очаквания (атаката се нарича *DNS rebinding*). За услуга, която излиза в интернет със сървър за имена, който не контролираш, пълната защита се състои в това да се разреши адресът, да се провери, че не принадлежи на вътрешен или специален диапазон, и да се свърже с *този* адрес, а не с името, освен това да се изолира мрежата на сървъра, така че вътрешното да не е достъпно дори ако валидацията пропадне. Това е последната линия на защита и е инфраструктурна, не кодова. [Наръчникът на OWASP за SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) описва двата слоя и защо нито един не стига сам. `revisor` на този курс работи в локална среда, която ти контролираш: списъкът му разрешава `127.0.0.1` и `localhost`, а на твоята машина `localhost` се превежда от файла ти `/etc/hosts` на същия адрес. Там списъкът от имена стига; извън такава среда приемането на име като `localhost` не е пълна защита, по причината, обяснена току-що; важното на урока е навикът: **всеки адрес, който е написал друг човек, е подозрителен, докато не мине през списък на разрешеното.** Има статия на Hábil за [сигурността на API: идентичност, шлюз и токени](https://www.habil.mx/bg/blog/api-security-cto-identity-gateway-tokens/), която следва темата от архитектурата.

### 8.9 Стъпката на проекта: `revisor` проверява наистина

Всички части са налице. Ето проекта, такъв какъвто остава в края на урока:

```text
revisor/
  pom.xml                  versión 0.8.0 · agrega Jackson 3.2.3 y copy-dependencies
  servicios.json           NUEVO (reemplaza a servicios.txt)
  src/main/java/mx/habil/revisor/
    Revisor.java           lee servicios.json con la lista permitida
    Json.java              NUEVO: el mapeador de Jackson
    Configuracion.java     REESCRITA para JSON y con validación
    Revision.java          REESCRITA: HttpClient en lugar del simulador
    ReporteJson.java       NUEVO: la forma del JSON
    Formato.java           agrega json()
    Estado.java  Respuesta.java  Servicio.java  Resultado.java  Clasificador.java
    Resumen.java  Reporte.java  ErrorDeConfiguracion.java       (sin cambios)
                           (Simulador.java se borró)
  src/test/java/mx/habil/revisor/
    ConfiguracionTest.java  REESCRITA: 7 pruebas
    RevisionTest.java       NUEVA: 5 pruebas contra un servidor de verdad
    ClasificadorTest.java  FormatoTest.java  ResumenTest.java    (sin cambios)
```

Вече видя цели `pom.xml`, `servicios.json`, `Json`, `Configuracion`, `Revision`, `ReporteJson`, `Formato`, `ConfiguracionTest` и `RevisionTest`. Единственият, който липсва, е `Revisor`, който променя две неща: чете `servicios.json` и подава на `Configuracion.leer` разрешения списък.

```java
// fig08_10/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.List;

/** El punto de entrada: lee servicios.json, revisa cada servicio e imprime el reporte. */
public final class Revisor {

  private Revisor() {}

  /** Arranca el revisor; los errores de configuración salen por la salida de error. */
  public static void main(String[] args) throws InterruptedException {
    try {
      List<Servicio> servicios =
          Configuracion.leer(Path.of("servicios.json"), Configuracion.SERVIDORES_PERMITIDOS);
      Reporte reporte = Reporte.de(Revision.revisarTodos(servicios));
      System.out.println(Formato.tabla(reporte));
    } catch (ErrorDeConfiguracion e) {
      System.err.println("error de configuración: " + e.getMessage());
    }
  }
}
```

Единайсетте файла, които не се променят спрямо Урок 7, са включени, за да може проектът да се сглоби, без да се гледа друг урок:

<details>
<summary>Единайсетте файла, които не се променят</summary>

```java
// fig08_10/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig08_10/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig08_10/src/main/java/mx/habil/revisor/Servicio.java

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
// fig08_10/src/main/java/mx/habil/revisor/Resultado.java

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
// fig08_10/src/main/java/mx/habil/revisor/Clasificador.java

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
// fig08_10/src/main/java/mx/habil/revisor/Resumen.java

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
// fig08_10/src/main/java/mx/habil/revisor/Reporte.java

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
// fig08_10/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/ClasificadorTest.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/FormatoTest.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/ResumenTest.java

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

</details>

Изгради и провери броя. Тестовете на проекта не се нуждаят от лабораторията (`RevisionTest` вдига свой собствен сървър), но следващото изпълнение се нуждае:

```text
$ mvn -B verify
...
[INFO] Recompiling the module because of changed dependency.
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.RevisionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.RevisionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.FormatoTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- dependency:3.11.0:copy-dependencies (copiar-dependencias) @ revisor ---
[INFO] BUILD SUCCESS
```

`Tests run: 25`: 18 от Урок 7, минус 5-те на `ConfiguracionTest`, които четяха текст, плюс 7-те нови, които четат JSON, плюс 5-те на `RevisionTest` (18 − 5 + 7 + 5). Запиши го в дневника си. Ако не излезе 25, нещо се е преместило и това е въпрос, на който си струва да се отговори, преди да продължиш. И изпълнението, с пълния *classpath*:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

Обичайната таблица, сега с измерени времена, и резюмето със средно, което зависи от тях.

**Какво не следят тези 25 теста.** Едно зелено число не казва какво покрива. За да се разбере, се чупеше, едно по едно, кодът, който следва, и се измерваше дали някой тест става червен. Станаха червени и затова са следени: отхвърлянето на схеми, на сървъри извън списъка, на невалиден `timeout_ms` и на повтарящи се имена; превеждането на `ConnectException` и на `HttpTimeoutException`; и таймаутът на заявката. **Останаха в зелено**, тоест без наблюдение: съобщението `'nombre' debe ser un texto que no esté vacío`, това на `falta la lista 'servicios'`, `connectTimeout` на клиента, политиката `Redirect.NEVER` и превръщането в `null` на времето в `ReporteJson` (последното го следи договорът от Урок 10). Не е дефект на урока, а правило: всеки нов тест, който добавиш, затваря една от тези дупки, а упражнения 3 и 4 започват оттам.


## Грешката, която ще видиш

Урокът има една основна грешка, тази на класа на Jackson, който не се появява, и няколко спомагателни съобщения, които е добре да се четат с един поглед.

**Основната грешка.** Стартираш `revisor` с командата от Уроци 6 и 7, която не включва JAR-овете на Jackson:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError` (името на липсващия клас се появява с наклонени черти, `tools/jackson/core/JacksonException`) не е грешка при компилиране. Програмата **се компилира** добре, защото Maven познаваше зависимостта; пропада при изпълнение, защото JVM не знае къде да я търси. Следващата причина (`Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException`) го потвърждава: този клас не е в нито едно от местата на *classpath*. Решението е да изпълниш с `-cp "target/classes:target/lib/*"` (и преди това да си изпълнил `mvn -B verify`, който копира JAR-овете в `target/lib`). Ако някой те попита „компилира се, но не върви?“, отговорът почти винаги е *classpath*. А близкият му роднина, този от 8.5.1, е да копираш пример на Jackson 2: `package com.fasterxml.jackson.databind does not exist`. Поправя се със смяна на `com.fasterxml.jackson` с `tools.jackson` в `import` и преглед на имената на методите.

**Спомагателните съобщения**, за да ги разпознаваш с един поглед:

- `java.net.BindException: Address already in use` при стартиране на лабораторията: друга програма ползва порт 8089 или си оставил предишна лаборатория да върви. Затвори я с `Ctrl`+`C` в нейния терминал.
- Всички услуги със `sin conexión`: лабораторията не е включена. Включи я в друг терминал и стартирай отново.
- `exception ConnectException has already been caught`: поставил си `catch` на общия тип преди частния. От частното към общото.
- `ConnectException` със съобщение `null` (`fig08_03.java`): нормално. Затова `revisor` никога не показва съобщението на мрежово изключение.
- `error de configuración: servicios.json no es JSON válido: renglón 4, columna 5`: нещо липсва или е в повече близо до тази точка; почти винаги е запетая. Погледни реда преди посочения.
- `error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida`: защитата в действие. Не е повреда, а предупреждение, че някой е поискал адрес, който `revisor` не проверява.
- `invalid URI scheme file` (`fig08_09.java`): `HttpClient` отхвърля схеми, които не са HTTP; `revisor` ги отхвърля по-рано, в конфигурацията.

## Какво се прави погрешно

**Изпращане на заявка без таймаут.** Това е най-скъпата грешка на урока, защото не пропада в тестовете: работи в деня на примера и увисва програмата в деня, в който една услуга замълчи. Всяка заявка носи своя `timeout`, а клиентът — своя `connectTimeout`.

**Създаване на `HttpClient` за заявка или негово незатваряне.** Всеки клиент пази връзки и ресурси; един за заявка хвърля на боклука това, което клиентът спестява при повторно използване, а незатварянето ги оставя заети. Един за проверка, с `try-with-resources`.

**Показване на `e.getMessage()` на мрежово изключение.** Понякога е `null`, понякога е вътрешен текст на имплементацията и се променя между версии и системи. Превеждай **типа** в фиксирана подробност и оставяй съобщението вън.

**Улавяне на `Exception` „за всеки случай“ около мрежата.** Поглъща и програмните грешки (`NullPointerException` от твоята собствена логика) и ги маскира като „мрежова грешка“. Улавят се конкретните типове: `HttpTimeoutException`, `ConnectException`, `IOException`, в този ред.

**Доверие в JSON, защото „го пиша аз“.** Файлът ще го редактира някой друг или ти, забързан. Директното съпоставяне с *record* оставя да минат `1.5` и `"1000"`, а `-5` стига до изключение далеч от мястото, където е написан. Валидира се на входа, със съобщения, които казват какво да се поправи.

**Валидиране на адрес с текстови операции.** `startsWith("http://127.0.0.1")` оставя да мине `http://127.0.0.1@servidor-del-atacante/`. Анализира се с `URI` и се сравнява `getHost()` с разрешен списък.

**Списък на забраненото вместо на разрешеното.** Опасните адреси са твърде много и всяка година има нови; тези, от които имаш нужда, са малко и ги познаваш. Което не е в списъка, се отхвърля.

**Сляпо следване на пренасочвания.** Разрешен адрес, който води към неразрешен, обезсилва списъка. `Redirect.NEVER`, изписано.

**Тестове, които зависят от услуга, която не контролираш.** Ако `RevisionTest` проверяваше лабораторията, би пропаднал на всеки компютър, където не е включена. Тестът вдига собствен сървър, с известно поведение, на свободен порт.

**Измерване на времена с часа на часовника.** `System.currentTimeMillis()` може да скочи при корекция на часовника; `System.nanoTime()` е тази, която служи за измерване на интервали.

## Упражнения

### Упражнение 1 — Адресът, който не бива да минава, и неговите братовчеди

В твоя `servicios.json` смени втората услуга на всеки от тези адреси, **един по един**, и за всеки **първо предскажи** какво отпечатва `revisor`: (а) `http://169.254.169.254/latest/meta-data/`; (б) `http://localhost:8089/pagos`; (в) `http://[::1]:8089/pagos` (адресът на „този същия компютър“ в нотацията IPv6); (г) `http://LOCALHOST:8089/pagos`. После изпълни всеки и сравни. Запиши в дневника онзи, който не си познал, и защо разрешеният списък от 8.8.3 се държи така, както се държи с него.

### Упражнение 2 — Услуга, която никога не отговаря

Добави към своя `servicios.json` три услуги на лабораторията: `facturacion` (`/facturacion`), `notificaciones` (`/notificaciones`, с `"timeout_ms": 1000`) и `basura` (`/basura`). **Преди да изпълниш**, предскажи състоянието и подробността на всяка и колко работещи, бавни и паднали ще каже резюмето. После изпълни. Накрая махни от `Revision.java` реда с `.timeout(...)`, изпълни отново и обясни какво стана с `notificaciones`, колко отне програмата и какво би станало, ако услугата изобщо не беше отговорила.

### Упражнение 3 — Три лошо написани файла

С оригиналния `servicios.json` счупи файла по три начина, един по един, и **предскажи съобщението**, преди да изпълниш: (а) махни запетаята в края на реда на услугата `catalogo`; (б) остави файла напълно празен; (в) смени стойността на `servicios` с `["catalogo"]`, списък от текстове вместо обекти. За (а), преди да изпълниш, запиши на кой ред и колона очакваш да пропадне.

### Упражнение 4 — Два реда, които директното съпоставяне би оставило да минат

Добави два случая към таблицата `archivosInvalidos` на `ConfiguracionTest`: услуга с `"timeout_ms": -5` и друга с `"timeout_ms": true`. **Предскажи** съобщението на всеки и колко теста ще каже отчетът (подсказка: всяко `Arguments.of` е тест). Изпълни `mvn -B verify`. Трябваше ли да променяш нещо в `Configuracion.java`, за да минат?

## Решения

### Решение 1

(а) Адресът на услугата за метаданни се отхвърля, преди да се изпрати каквото и да било:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

(б) `localhost` е в разрешения списък, така че `http://localhost:8089/pagos` минава и `revisor` проверява лабораторията, все едно е `127.0.0.1`. Забележи какво се промени и какво не: втората услуга продължава да се казва `inventario` (променил си само адреса ѝ), но сега пита за маршрута на `pagos`, който отговаря бързо, така че излиза ARRIBA, а не LENTO:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     180ms  200
inventario  ARRIBA      92ms  200
pagos       ARRIBA      91ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 3 arriba · 0 lentos · 1 caído · promedio 121ms
```

Това е малък урок за имената: името на една услуга е етикет, който пише човек, а адресът е това, което наистина се проверява. `revisor` не проверява дали съвпадат.

(в) и (г): **нито един от двата не минава** и е нарочно. `[::1]` е сървърът `[::1]` за `getHost()` и не е в списъка; `LOCALHOST` с главни букви също не, защото сравнението е точно.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor LOCALHOST no está en la lista permitida
```

Ако си очаквал (в) или (г) да минат, значи си мислил за това, което *означават* (същият компютър), а не за това, което *казват* (друг текст). Един разрешен списък сравнява текстове и се пази строг нарочно: да приемеш друга форма е въпрос на добавянето ѝ към списъка, със студена глава, а не на отпускане на сравнението, за да „почти съвпада“.

### Решение 2

**Прогноза.** Четирите обичайни остават същите: `catalogo` и `pagos` ARRIBA, `inventario` LENTO, `reportes` CAIDO със `sin conexión`. От трите нови `facturacion` отговаря `503`, CAIDO с подробност `503`; `notificaciones` чака повече от своя таймаут от една секунда, CAIDO с `tiempo agotado`; `basura` затваря връзката без да отговори, CAIDO с `error de red`. Резюме: 7 услуги, 2 работят, 1 бавна, 4 паднали, а средното включва само онези, които са отговорили (`catalogo`, `inventario`, `pagos` и `facturacion`, която отговори, макар и с грешка): около 670 милисекунди, защото 2.3 секунди на `inventario` тежат много сред четири.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     179ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      93ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      100ms  503
notificacionesCAIDO          —  tiempo agotado
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 669ms
```

Средното е сборът на четирите, които са отговорили, делен на четири. (Дългите имена преливат колоната, дефект, който форматът от урок 2 носи със себе си и който незадължителното упражнение на Урок 10 решава.) Сега, без `.timeout(...)`:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     174ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      91ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      102ms  503
notificacionesLENTO      10.0s  200
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 2 lentos · 3 caídos · promedio 2.5s
```

`notificaciones` вече не е `tiempo agotado`: без таймаут клиентът изчака 10-те секунди, които лабораторията се бави, получи `200` и го класифицира LENTO (`10.0s`). Цялата програма отне около 13 секунди (измерено с `time`: 12.9) и средното скочи. Но лабораторията е любезна: отговаря след десет секунди. **Една истинска услуга, която замълчи, не отговаря никога**, и тогава `send` не се връща, програмата се вкарва в безкрайно чакане и никоя друга проверка не се завършва. Затова таймаутът на заявката никога не се маха: той е единственото, което ограничава най-лошия случай. Върни реда.

### Решение 3

(а) Без запетаята Jackson стига до `{` на `inventario`, очаквайки запетая или края на масива, и пропада там; съобщението пада на ред 4 (този на `inventario`), колона 5, което е мястото, където започва тази скоба, след отстъпа от четири интервала. Който е сгрешил, е оставил дефекта на ред 3, но анализаторът се усеща едва когато види онова, което следва.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

(б) Празният файл е невалиден JSON, но дървото, което Jackson връща за празен текст, не е грешка: това е „липсващ“ възел и `revisor` го третира като „няма списъка“:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

(в) Списък от текстове има формата на списък, но всеки елемент трябва да е обект:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

### Решение 4

Двата реда се добавят в края на таблицата: последният, който вече съществуваше, сега завършва със запетая, а след него идват двата нови, със същия формат като останалите:

```java
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": -5}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": true}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"));
```

Двете съобщения са обичайното, `servicio 1: 'timeout_ms' debe ser un entero mayor que 0`: `-5` е цяло число, но не е по-голямо от 0, а `true` не е цяло число (`isInt()` е невярно). Без да се пипа `Configuracion.java`, защото валидацията вече покриваше тези два случая от самото начало. С директното съпоставяне `-5` би минало мълчаливо (видя го в 8.6.2), а `true` би било отхвърлено, но с грешка на Jackson, а не с твоето съобщение. Броят се вдига от 25 на 27:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, ... in mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Как разбирам, че съм успял

- [ ] `mvn -B verify`, от корена на `~/curso-java/revisor/`, завършва с `BUILD SUCCESS` и казва `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`, и знаеш откъде идва всяко число (18 − 5 + 7 + 5).
- [ ] С включена лаборатория `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` отпечатва таблицата с четирите услуги със СЪСТОЯНИЕ и ПОДРОБНОСТ, идентични с тези на този урок, и резюме с `2 arriba · 1 lento · 1 caído`.
- [ ] Без включена лаборатория четирите услуги излизат CAIDO със `sin conexión`.
- [ ] Услуга, насочена към `http://169.254.169.254/latest/meta-data/`, кара `revisor` да отпечата `el servidor 169.254.169.254 no está en la lista permitida`, без да е изпратил никаква заявка.
- [ ] Махна `.timeout(...)` от `Revision.java` и видя, че тестът за `tiempo agotado` става червен; върна го.
- [ ] Един `servicios.json` с една запетая по-малко произвежда `no es JSON válido: renglón N, columna M` и знаеш на кой ред да гледаш.
- [ ] Можеш да обясниш защо `"timeout_ms": "1000"` влиза като 1000 при директното съпоставяне и се отхвърля при валидацията на ръка.
- [ ] Записа в дневника си кое съобщение от този урок ти беше най-трудно за четене.

## Резюме

Отговори, без да гледаш урока:

1. Кои два таймаута има в една заявка с `HttpClient`, къде се конфигурира всеки и какво хвърля всеки, когато изтече?
2. Защо `HttpRequest` без `timeout` е опасен?
3. На коя подробност на `revisor` съответства всяко от тези изключения: `HttpTimeoutException`, `ConnectException` и всяко друго `IOException`? Защо се улавят в този ред?
4. Защо `503` не е изключение? А `302`, с политиката за пренасочвания на `revisor`?
5. Защо се мери със `System.nanoTime()`, а не с `currentTimeMillis()`?
6. Каква е разликата между `tools.jackson` и `com.fasterxml.jackson`? Кои анотации не са сменили пакета си?
7. Какво е *classpath* и защо липсва `target/lib` в `java -cp target/classes`, макар програмата да се компилира?
8. Защо конфигурацията се чете като дърво и се валидира на ръка, вместо да се съпостави директно с *record*? Назови две стойности, които директното съпоставяне оставя да минат.
9. Какво е SSRF и кои са четирите защити на `revisor`? Защо `startsWith("http://127.0.0.1")` не е защита?
10. Какво не решава разрешеният списък от имена? Какво още би трябвало в услуга, която излиза в интернет?

## За допълнително четене

- [Java SE 25, пакет `java.net.http`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/package-summary.html): целият HTTP клиент на JDK, с асинхронните заявки, HTTP/2 и WebSocket, които този урок не ползва. На английски, като почти цялата официална документация. Посетено на 7 октомври 2026 г.
- [RFC 9110, „HTTP Semantics“](https://www.rfc-editor.org/rfc/rfc9110): нормата на методите, кодовете на състоянието и пренасочванията; раздел 15 е списъкът на кодовете на състоянието. Посетено на 7 октомври 2026 г.
- [Ръководство за миграция към Jackson 3](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) и [хранилището на `jackson-databind`](https://github.com/FasterXML/jackson-databind): какво се е променило спрямо версия 2 и как се ползва библиотеката. Посетено на 7 октомври 2026 г.
- [OWASP, „Server-Side Request Forgery Prevention Cheat Sheet“](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html): защитите по слоеве срещу SSRF и защо една не стига. Посетено на 7 октомври 2026 г.

---

**Предишен:** [Урок 7 — Тестове с JUnit](07-pruebas-junit.md) ·
**Следващ:** [Урок 9 — Конкурентност: проверка на всичко наведнъж](09-concurrencia.md)
