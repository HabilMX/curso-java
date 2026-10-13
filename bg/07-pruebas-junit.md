# Урок 7 — Тестове с JUnit

**Време:** 90 мин (точно колкото трябва: всеки тест първо се вижда в червено и после в зелено, а това отнема време; не го прескачай)

**Какво изграждаш:** `revisor` получава 18 автоматични теста, които се изпълняват с една команда и пазят правилата му: класификацията, формата на времето, четенето на `servicios.txt` и резюмето

**Какво научаваш:** какво е автоматичен тест и трите му момента; JUnit 6 в `pom.xml`, с неговия BOM и в обхват `test`; `@Test` и `assertEquals`; `@ParameterizedTest` с `@CsvSource` (таблицата от случаи) и защо случаите се избират на границата; `assertThrows` и `@TempDir`; да видиш един тест в червено, преди да го видиш в зелено; и че **броят на тестовете се чете от отчета, а не от кода на изход (exit code)**

**Откъде идваш.** Носиш проекта на Maven от [Урок 6](06-proyecto-maven.md): тринайсет файла в пакета `mx.habil.revisor`, `pom.xml` с три *плъгина (plugin)* с фиксирана версия и `mvn -B verify`, което компилира, пакетира `revisor-0.6.0.jar` и завършва с `BUILD SUCCESS`. По средата на този изход имаше един ред, който казваше `No tests to run.`: проектът ти вече има къде да постави тестове (папката `src/test/java`, празна) и кой да ги изпълнява (*плъгинът* *Surefire*), но нито един тест. А това оставя един неудобен въпрос: откъде знаеш, че `revisor` работи? Стартира го и погледна таблицата. **Единственият тест, който има програмата ти, си ти, докато гледаш един екран.** Днес това се променя: тестове, които проверяват вместо теб, за част от секундата и при всяка промяна, че всяко правило продължава да прави това, което казва. Работиш в папката си `~/curso-java/revisor/`.

**Какво не прави този урок.** Не тества `Revision`, класа, който симулира мрежата, като изчаква с `Thread.sleep`: би отнел колкото програмата, а това, което следи (`Simulador`), изчезва в [Урок 8](08-http-y-json.md), който тества `revisar` срещу истински сървър, вдигнат вътре в самия тест. Не ползва и библиотеки за преструване на обекти, нито измерва покритието на тестовете: с малки функции и фиксирани данни не са нужни. И не добавя нищо към програмата: JUnit живее само в тестовете, а `.jar`, който се предава, не променя съдържанието си.

**За стартиранията.** Всичко в този урок е изпълнено с Temurin 25 и Maven 3.10.0, под macOS, с терминал в UTF-8. Изходите на Maven се показват съкратено: запазва се това, което анализира всеки раздел, а трите точки (`...`) отбелязват какво е изрязано. Две числа от тези изходи ще са различни на твоя екран и няма значение: `Time elapsed` (променя се при всяко стартиране) и, в тестовете, които пропадат, номерът на реда от следата (`ResumenTest.java:32`), който е този на твоя файл и съвпада само ако си го написал точно като урока, ред по ред. Всичко останало, включително броенето на тестовете, трябва да съвпада. Програмите са в [`programas/07-pruebas-junit/`](../programas/07-pruebas-junit), в хранилището на курса.

## След урока ще можеш да

- Обясниш какво е автоматичен тест, кои са трите му момента и какво добавя един *фреймуърк (framework)* за тестове като JUnit.
- Декларираш JUnit 6 в `pom.xml` с неговия BOM и в обхват `test` и обясниш защо не е част от програмата.
- Пишеш тестове с `@Test` и `assertEquals`, с очакваната стойност на първо място, и таблица от случаи с `@ParameterizedTest` и `@CsvSource`, като избираш случаите около границата.
- Тестваш грешка с `assertThrows` и файл с `@TempDir`.
- Видиш един тест в червено, преди да го видиш в зелено, и знаеш какво да върнеш назад, за да го предизвикаш.
- Изпълниш всички тестове, един клас или един-единствен тест и прочетеш от отчета колко са се изпълнили.
- Обясниш три начина да получиш зелено, което не е тествало нищо, и как се предпазваш от всеки.

## Защо, преди как

Понеделник е и някой те моли прагът за „бавна“ да мине от една секунда на 800 милисекунди. Това е промяна от един ред: `UMBRAL_LENTO_MS`. Правиш я, стартираш `revisor`, поглеждаш таблицата… и всичко изглежда наред. Наред ли е? С четири услуги с фиксирани данни `inventario` си остава бавна, а останалите си остават горе, така че таблицата изглежда като вчера. Но какво става с услуга, която отговаря точно за 800 милисекунди? Брои ли се като бавна, или не? Днешната таблица не знае: нито една услуга от твоите данни не мери 800. Ако си сбъркал с `>` там, където трябва да е `>=`, грешката чака някоя истинска услуга да падне точно на границата и в този ден ще я открие този, който ползва програмата ти, а не ти.

Гледането на таблицата има три недостатъка и трите се влошават с времето. **Вижда само данните, които имаш**, а грешките живеят в онези, които нямаш (точно 800, файлът с неправилно написан ред, случаят, в който никой не отговаря). **Зависи от това някой да гледа**, внимателно, всеки път, а двайсетият път никой не гледа внимателно. И **не оставя следа**: ако утре друг човек промени формата на времето, няма нищо, което да го предупреди, че е счупил онова, което ти си проверил вчера. Един автоматичен тест поправя и трите неща едновременно: пишеш случая веднъж, той го проверява при всяка промяна, без да се уморява, и остава в проекта за следващия човек.

Урокът има шест части: **какво е тест и как JUnit влиза в проекта** (7.1); **първият тест** и как да го видиш в червено (7.2); **таблицата от случаи** (7.3); **тестване на файл и на неговата грешка** (7.4); **четене на отчета**, която е най-занемарената част (7.5); и **стъпката на проекта** (7.6). С тази цел пред очи, ето какво ще видиш в края на урока, там където Урок 6 казваше `No tests to run.`:

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

Редът, който има значение, е `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. Накрая ще знаеш откъде идва всяко от тези числа, какво става, когато някое се премести, и защо нито едно от тях не се чете от „успеха“, който Maven обявява накрая.

## Понятията

### 7.1 Какво е автоматичен тест и какво е нужно

#### 7.1.1 Трите момента на един тест

Върни се към понеделника от началото: прагът мина на 800 милисекунди и никой не знае дали услуга от точно 800 се брои за бавна. **Автоматичният тест** е начинът да се отговори на този въпрос, без да се чака да се случи в продукция: малка програма, която подготвя известни данни, вика частта от програмата, която се иска да се следи, и сравнява полученото с това, което е трябвало да получи. Винаги има три момента, които на английски се съкращават AAA: **подготвяш (arrange)** входните данни, **действаш (act)**, като викаш кода, и **твърдиш (assert)**, че резултатът е очакваният. Ако твърдението се изпълни, тестът минава; ако не, пропада и казва какво е очаквал и какво е получил.

Да напишеш тази малка програма не е трудно. Трудното е всичко останало: да намериш всички тестове на проекта, да ги изпълниш един по един, без пропадането на един да спре другите, и да отчетеш колко са минали. Това го прави един ***фреймуърк (framework)* за тестове**, а този на Java е **JUnit**, който е във версия 6 (6.1.3 е актуалната към октомври 2026 г.; изисква Java 17 или по-нова, според [ръководството за потребителя](https://docs.junit.org/current/user-guide/)). Той ти дава четири неща: анотации като `@Test`, за да отбелязваш кои методи са тестове, методи като `assertEquals`, за да твърдиш, двигател, който открива и изпълнява тестовете, и отчет за случилото се. **Анотацията** е етикет, който се пише с `@` преди една декларация и който инструментите четат; не променя това, което методът прави, а само казва на JUnit как да се отнася с него.

#### 7.1.2 JUnit в `pom.xml`

За да го имаш в проекта, трябва да декларираш зависимостта в `pom.xml`. Ето целия файл, вече с JUnit. Новата част е в `<dependencyManagement>` и `<dependencies>`, а версията на проекта се вдига на `0.7.0`, както обеща Урок 6: `.jar` ще се казва `revisor-0.7.0.jar`.

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

JUnit не е един-единствен артефакт, а семейство от тях (API за писане на тестове, двигателят, който ги изпълнява, поддръжката за таблици от случаи…) и всички трябва да са от една и съща версия, за да работят заедно. За да не пишеш тази версия шест пъти и да не сбъркаш в някоя, Maven предлага **BOM** (*bill of materials*, „списък на материалите“): `pom.xml`, който не съдържа код, а само списък от съвместими версии. Блокът `<dependencyManagement>` с `<type>pom</type>` и `<scope>import</scope>` импортира целия този списък: оттам нататък всеки артефакт на JUnit се декларира **без версия** и взема тази, която BOM фиксира. Самата зависимост е само една, `junit-jupiter`, „чадърен“ артефакт, който носи API, поддръжката за таблици от случаи и двигателя. Това е формата, която [ръководството на JUnit за Maven](https://docs.junit.org/current/running-tests/build-support.html) препоръчва за изравняване на версиите.

Редът, който има най-голямо значение, е `<scope>test</scope>`. **Обхватът** (*scope*) на една зависимост казва къде е налична: тази с обхват `test` съществува само когато се компилират и изпълняват тестовете и **не** е част от програмата. Който изпълнява `revisor`, няма нужда от JUnit и не е задължен да го сваля. Тъй като компилаторът вече има JUnit в `src/test/java`, а не в `src/main/java`, `import org.junit...`, който пропадна със стартера в Урок 6 (фигура `fig06_01`), работи в тестовете и би продължил да пропада в програмата, което е точно това, което се иска.

#### 7.1.3 Какво се тества и какво не

Преди да напишеш първия, едно решение, което важи за целия курс: **кои части на `revisor` заслужават тест днес**. Отговорът идва от това да погледнеш какво прави всеки клас. `Clasificador`, `Formato` и `Resumen` са **правила**: получават една стойност и връщат друга, винаги една и съща за същия вход, без да пипат мрежата, диска или часовника. Те са най-лесните за тестване и най-ценните, защото в тях живеят решенията, които някой ще поиска да промени един понеделник (прагът, формата на времето, как се брои средното). `Configuracion` е правило с ръб навън: чете файл и това, което трябва да се следи, са съобщенията му за грешки, които са единственото, което вижда този, който сгреши при писането на `servicios.txt`. И четирите имат тест в този урок.

`Revision` и `Simulador` нямат, и причината не е мързел. `Simulador` е скеле: измисля четири отговора, за да има програмата какво да проверява, докато няма мрежа, и в Урок 8 изчезва. Тест на едно скеле проверява, че скелето казва това, което ти си му написал, и се изхвърля заедно с него. `Revision.revisar`, от своя страна, **изчаква** наистина това, което казва всяка услуга (`Thread.sleep`): тест на четирите услуги би отнел колкото програмата, около 2.5 секунди, а 18-те теста на този урок се изпълняват изцяло за част от секундата. Тази бързина не е лукс: тя е това, което те кара да ги пускаш при всяка промяна, а не „накрая“. Когато `revisar` прави истинска заявка, в Урок 8, ще има собствен тест срещу сървър, вдигнат вътре в самия тест, който отговаря каквото тестът иска и когато иска.

Има трета причина да започнем с правилата, която си личи по формата на тестовете, които идват: четирите тестови класа живеят в **същия пакет** като това, което тестват, `mx.habil.revisor`. Благодарение на това виждат класовете с видимост на ниво пакет, които Урок 6 остави затворени за света, и не се наложи да се отваря нищо, за да може да се тества. Тестовете ползват програмата така, както би я ползвал друг клас от пакета, и нищо повече.

### 7.2 Първият тест: резюмето

#### 7.2.1 `ResumenTest`, ред по ред

Да започнем с най-простия от проекта. Резюмето на една проверка брои услуги, а средното му включва само онези, които са отговорили. Два теста го следят: четирите услуги, както винаги (средното трябва да е 843), и граничният случай, в който **никой** не отговаря, където средно няма и резюмето пише тире.

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

Чете се отгоре надолу. Класът живее в същия пакет като това, което тества, и се казва `ResumenTest`: името завършва на `Test`, защото така го търси *Surefire*, *плъгинът*, който изпълнява тестовете (по подразбиране включва файловете, които започват с `Test` или завършват на `Test`, `Tests` или `TestCase`, според неговата [документация](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html); в 7.5 ще видиш какво става с тест, който не се казва така). Класът не е `public`: JUnit 6 не го изисква и по правилото на Урок 6 (6.1.3) остава с най-тясната видимост.

`import static` е ново. Един обикновен `import` ти позволява да ползваш един **клас** с късото му име; един `import static` ти позволява да ползваш **статичен метод** на един клас, без да пишеш класа отпред. С `import static org.junit.jupiter.api.Assertions.assertEquals;` можеш да пишеш `assertEquals(...)` вместо `Assertions.assertEquals(...)`. Тестовете го ползват навсякъде, така че се превръща в навик.

`private static Resultado resultado(String nombre, Respuesta respuesta)` е **помощен метод**: функция, която двата теста споделят, за да сглобят един `Resultado`, без да повтарят три реда всеки път. Частен е, защото интересува само този клас. А всеки метод с `@Test` е тест: има име, което **казва какво се твърди** (`sinNingunaRespuestaElPromedioEsUnaRaya`, а не `prueba2`), не получава параметри и не връща нищо, и спазва трите момента, разделени с празни редове: сглобява се списъкът, вика се `Formato.resumen(Resumen.de(resultados))` и се твърди. В `assertEquals(esperado, actual)` **очакваната стойност е първа**; ако ги поставиш наобратно, тестът минава и пропада по същия начин, но съобщението за грешка ще казва обратното на това, което се е случило.

От корена на проекта, с `-Dtest=ResumenTest`, за да се изпълни само този клас:

```text
$ mvn -B test -Dtest=ResumenTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`. Два теста, нито един не пропадна. Вече знаеш, че минават. Остава да разбереш дали **могат да пропаднат**.

#### 7.2.2 Да го видиш в червено, преди да го видиш в зелено

Тест, който никога не си виждал да пропада, не доказва нищо. Може да сравнява грешното или да сравнява нещо, което винаги е вярно, и зеленото му да е украшение. Единственият начин да знаеш, че един тест следи това, което казва, че следи, е **да счупиш нарочно кода, който следи, и да провериш, че става червен**. Направи го сега с тези на резюмето. В `Resumen.java` средното се изчислява само със услугите, които са отговорили: за това се грижи `filter(OptionalLong::isPresent)`. Замени тези два реда (`.filter(OptionalLong::isPresent)` и `.mapToLong(OptionalLong::getAsLong)`) с един, `.mapToLong(t -> t.orElse(0))`: сега услугата, която не е отговорила, се брои за **нула милисекунди**, и изпълни отново:

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

Сега вече има съобщения и са от добрите. `Failures: 2`: пропаднаха и двата теста, всеки от различна последица на същата грешка. Първият казва `expected: <… promedio 843ms> but was: <… promedio 632ms>`: очаквал е 843, получил е 632; а 632 е `2529 / 4`, сборът на времената, разделен на четирите услуги, вместо на трите, които са отговорили. Вторият казва `expected: <… promedio —> but was: <… promedio 0ms>`: когато никой не отговаря, резюмето трябва да каже, че няма средно, а промяната го превръща в „нула милисекунди“, число, което прилича на данни, а не е. Редът, който започва с `at`, е следата: казва в кой файл и на кой ред от теста е било твърдението, което е пропаднало (`ResumenTest.java:32`), и това е първото, което отваряш. С тези две съобщения в ръка знаеш къде да търсиш, без да отваряш дебъгер. (Два теста, които падат по една и съща причина, също разказват нещо: този дефект се забелязваше от два ъгъла и вторият тест съществува именно затова.)

Върни промяната и потвърди, че се връща към зелено. Това упражнение **да върнеш назад и да видиш червеното** е навикът, който най-евтино ти излиза от целия курс: отнема минута и ти казва дали тестовете ти струват нещо. В курса ще се повтаря всеки път, когато се пише нов тест.

### 7.3 Таблицата от случаи: `@ParameterizedTest` и `@CsvSource`

#### 7.3.1 Един тест, много редове

Класификацията на една услуга е правило с граница: хиляда милисекунди или повече е бавна. За да я следиш добре, трябва да тестваш няколко случая и всички имат една и съща форма (влиза код и време, излиза състояние). Да напишеш шест почти еднакви метода е шум, който скрива интересното. JUnit има по-добър начин, **параметризирания тест**: методът се пише веднъж, с параметри, и **таблица от случаи** със стойностите:

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

`@ParameterizedTest` заменя `@Test` и казва на JUnit, че този метод ще се изпълни няколко пъти. `@CsvSource` е таблицата: всеки текст от списъка е **ред**, а запетаите разделят **колоните** (CSV идва от *comma-separated values*, „стойности, разделени със запетаи“). JUnit взема всеки ред, превръща текста на всяка колона в типа на съответния параметър —`int`, `long`, и дори `Estado`, което разпознава по името на константата— и вика метода веднъж за ред. Това са шест извиквания, така че отчетът ще преброи **шест теста** от тази единствена декларация, плюс седмия: този на `SinRespuesta`, който е отделен `@Test`, защото няма същата форма (няма код, нито време).

#### 7.3.2 Случаите се избират на границата

Тестът на формата на времето е същата идея с една колона на входа и една с очаквания текст. Четирите му реда са тези, които имат значение около смяната на единицата: 87 и 999 се пишат в милисекунди, 1000 и 2300 в секунди.

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

Случаите не са избрани наслуки. Избрани са **около границата**: 142 е нормална услуга; 999 е последната стойност, която остава горе; 1000 е първата, която е бавна (случаят, който има най-голямо значение от всички: той улавя `>` там, където трябва да е `>=`); 2300 е ясно бавна; 503 и 404 са грешки на сървъра и на клиента, които, колкото и бързо да отговорят, оставят услугата паднала. **Граничните стойности** са мястото, където се крият грешките с отклонение с единица (off-by-one), а таблица, която не ги включва, оставя ръба без наблюдение. Провери го по начина, който вече знаеш: в `Clasificador.java` смени `tiempoMs >= UMBRAL_LENTO_MS` с `tiempoMs > UMBRAL_LENTO_MS` и изпълни целия проект:

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

Чети го спокойно, защото е отчет на истинска грешка и има четири неща, които си струва да разпознаеш от пръв поглед. Първо, **кой клас е пропаднал**: `ClasificadorTest`, с `Tests run: 7, Failures: 1`; другите три класа остават в зелено: пропадането на един не спира останалите. Второ, **кой тест**: `clasificaUnaRespuesta(int, long, Estado)[3]`, където `[3]` е **номерът на реда от таблицата**: третият, `200, 1000, LENTO`. Трето, **какво се случи**: `expected: <LENTO> but was: <ARRIBA>`. Услугата от 1000 милисекунди е трябвало да е бавна, а е излязла горе, защото с `>` 1000 вече не се брои. И четвърто, заключителният ред: `Tests run: 18, Failures: 1` и `BUILD FAILURE`, с ненулевия код на изход, който кара всеки сървър за интеграция да стане червен. Една промяна от един знак, един ред в червено, и отчетът ти казва кой: това дава таблицата. Върни промяната, преди да продължиш.

#### 7.3.3 Изпълнение на един-единствен тест

Когато един ред стане червен, искаш да повториш **този** тест, докато го поправяш, а не 18-те. Вече ползва `-Dtest=ResumenTest`, за да изпълниш един клас; с `#` и името на метода избираш един-единствен тест от класа:

```text
$ mvn -B test -Dtest=ClasificadorTest#sinRespuestaEsCaido
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 1`: от седемте теста на класа се изпълни само този, който поиска. Забележи и фазата: `mvn -B test`, а не `verify`. Тъй като `test` е преди `package` в жизнения цикъл, Maven спира след тестовете и не сглобява `.jar`, който не е нужен, за да повториш един тест. Това е начинът да работиш, докато поправяш нещо: един тест, бързо, много пъти. И е също капан, ако забравиш, че си го ползвал: един `Tests run: 1` в зелено не казва нищо за другите 17. **Преди да сметнеш нещо за готово, се изпълнява всичко**, с `mvn -B verify` и без `-Dtest`, и се чете общият брой.

### 7.4 `assertThrows` и `@TempDir`: тестване на файл и на неговата грешка

Третото семейство тестове следи какво прави `revisor` с конфигурационния файл. Тук има две нови неща. Първото е, че за да тестваш файл, трябва да имаш файл, а не искаш да пишеш в истинската си папка, нито да оставяш боклук. Второто е, че това, което искаш да твърдиш, не е върната стойност, а **изключение**: че определени файлове карат четенето да пропадне, и то с точното съобщение.

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

**`@TempDir Path carpeta`** иска от JUnit временна папка: преди всеки тест я създава нова и празна, а после я изтрива с всичко, което съдържа. Тестовете записват там своя `servicios.txt` с `Files.writeString` (Урок 5 четеше файлове с `Files`; това е другата половина, записването им) и не си пречат, нито замърсяват диска.

**`assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta))`** изпълнява това, което му подадеш (израз, написан като `() -> ...`: *ламбда*, същата форма, която ползва в *стриймовете (stream)* на Урок 4), и проверява дали хвърля **това** изключение. Ако не хвърли нищо или хвърли друго, тестът пропада; ако хвърли очакваното, `assertThrows` ти го връща, за да го прегледаш: тестът сравнява неговото `getMessage()` с точното съобщение от таблицата. Забележи защо се твърди съобщението, а не само типът: този текст е единственото, което вижда човекът, който редактира `servicios.txt`, когато сгреши; той е договорът с него. Ако някой го пренапише, без да се усети, тестът го открива.

Таблицата от случаи на този клас ползва вариант на `@CsvSource`, който приема текстов блок (`textBlock`), с разделител `|` вместо запетая, за да изглежда всеки ред като таблица. Има само един проблем: всеки ред от таблицата заема един ред текст, а конфигурационният файл има няколко. За това първата колона пише редовете на файла, разделени с `;` (`a https://a.x;b ftp://b.x` са два реда), а тестът ги превръща в истински нови редове, преди да запише файла. Четирите реда са четири от причините за отхвърляне, които определи Урок 5 (онези, които най-често се допускат): липсват полета, адресът не е `http`, нито `https`, таймаутът не е по-голям от нула и има повтарящи се имена. Петият тест на класа, `leeUnArchivoValido`, е щастливият случай: две услуги, едната с таймаут по подразбиране и другата с 1000. **Винаги е нужен един такъв:** клас, който тества само отхвърляния, би могъл да мине с четец, който отхвърля всичко.

### 7.5 Четене на отчета: броят на тестовете

#### 7.5.1 Къде е числото и къде не

Изпълни целия проект, пълен, със стандартната команда:

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

От този отчет има значение един-единствен ред: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. Четирите числа казват колко теста са се **изпълнили**, колко са **пропаднали** по твърдение, колко са завършили с неочаквана **грешка** (изключение, което тестът не е очаквал) и колко са били **пропуснати** (отбелязани да не се изпълняват). А 18-те идват, точно, от това, което вече знаеш: `ClasificadorTest` 7 (шест реда и този на `SinRespuesta`), `FormatoTest` 4 (четири реда), `ConfiguracionTest` 5 (четири реда и щастливият случай) и `ResumenTest` 2. Събери ги: 7 + 4 + 5 + 2. Ако някога 18 се промени, без ти да си добавил или махнал тест, нещо се е преместило и това е въпрос, на който си струва да се отговори.

Всеки тестов клас оставя и свой собствен отчет в папката `target/surefire-reports`, в два формата:

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

Файловете `.txt` са за да ги четеш ти, а `TEST-*.xml` са за машините: сървърът за непрекъсната интеграция, редакторът или твой скрипт четат XML, за да броят. Текстовият отчет на един клас в зелено е от три реда, това, което видя горе, но само за един клас:

```text
$ cat target/surefire-reports/mx.habil.revisor.ConfiguracionTest.txt
-------------------------------------------------------------------------------
Test set: mx.habil.revisor.ConfiguracionTest
-------------------------------------------------------------------------------
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
```

#### 7.5.2 Три зелени, които не са тествали нищо

А сега причината, поради която този раздел съществува. **Числото се чете от отчета, никога от съобщението за успех, нито от кода на изход.** Има начин Maven да завърши с `BUILD SUCCESS`, без да е изпълнил нито един тест, и е по-лесен за предизвикване, отколкото изглежда: достатъчно е имената на тестовите класове да не са тези, които *Surefire* търси. За да се види, четирите класа на този проект бяха преименувани от `...Test` на `...Prueba` (например `ClasificadorPrueba`), което на испански означава същото (prueba = тест), и беше изпълнена същата команда:

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

Няма нито ред `T E S T S`, нито `Running`, нито `Tests run`. И въпреки това: `BUILD SUCCESS` и кодът на изход е `0`, който означава „всичко е наред“. Maven не е намерил тестове, не е имал какво да провали и се е обявил за успешен. Ако единственият критерий беше „завърши ли в зелено?“, този проект без нито един ефективен тест би минал всяка проверка. Не се създава и папката `target/surefire-reports`, което е още една следа: отчетът не съществува, защото не е имало какво да се отчита. Сравни го с това, което видя в Урок 6, когато папката за тестове беше празна: там *Surefire* казваше `No tests to run.`, честно предупреждение. Тук има четири компилирани тестови класа в `target/test-classes`, а тъй като нито един не се казва, както той търси, не казва нищо. **Мълчанието е по-лошо от предупреждението** и затова числото се търси активно: да не се появи също е информация.

Има още два варианта на същия механизъм, също измерени. Първият: тестов клас, който **сам се прескача**. JUnit позволява един тест да каже „ако не е изпълнено еди-какво условие, не ме изпълнявай“ (нарича се *допускане*, `Assumptions`, и наистина служи за тестове, които се нуждаят от нещо, което не винаги го има, като връзка). Но една грешка в това условие изключва всички тестове на класа, без нищо да стане червено. Беше добавено фалшиво допускане в началото на `FormatoTest`, за да се види. Това е фрагментът, поставен вътре в класа, преди теста, който вече имаше (с двата му `import`: `org.junit.jupiter.api.Assumptions` и `org.junit.jupiter.api.BeforeAll`):

```java
  @BeforeAll
  static void soloSiHayRed() {
    Assumptions.assumeTrue(false, "sin red");
  }
```

`@BeforeAll` отбелязва метод, който JUnit изпълнява веднъж, преди всички тестове на класа (затова е `static`: още няма никакъв обект от класа). `assumeTrue(условие, причина)` казва „ако условието е невярно, не продължавай“. Тук условието е явно `false`, за да е очевиден ефектът; в истински проект би било нещо като „има връзка с базата данни“, а грешката — условие, което никога не се изпълнява на машината за интеграция. Резултатът:

```text
$ mvn -B verify
...
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.002 s -- in mx.habil.revisor.FormatoTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
$ echo $?
0
```

Maven отпечата `WARNING` (жълто, не червено) и продължи: целият клас остана в `Skipped: 1`, общият брой падна от 18 на 15, `BUILD SUCCESS` и код на изход `0`. Който гледа само зеленото, никога не разбира, че четири теста са престанали да следят формата на времето. Вторият вариант е на терминала, а не на Maven: **кодът на изход на един конвейер (pipe) е този на последната програма в конвейера.** Ако за удобство орязваш изхода с `| tail` и проверяваш `$?`, това, което проверяваш, е кодът на `tail`, който винаги излиза успешно:

```text
$ mvn -B verify | tail -2
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
0
```

Тук Maven пропадна (това е изпълнението със счупеното `>` от 7.3.2 и неговият собствен код на изход беше 1), но `0`, което виждаш, е на `tail`. Ако наистина имаш нужда от конвейер, терминалът има опция, за да бъде кодът на изход този на първата програма, която е пропаднала, а не на последната:

```text
$ set -o pipefail
$ mvn -B verify | tail -1
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
1
```

С `pipefail` `1` на Maven оцелява след конвейера. Важи за терминала, в който си го написал (или за скрипта, който го има в началото), и не променя най-важното: най-сигурно е да изпълняваш тестовете **без да филтрираш изхода** и да четеш оттам реда `Tests run:`. И в трите случая защитата е една и съща и е навикът на този курс: след като изпълниш тестовете, прочети този ред и сравни числото с това, което очакваш.

Има и четвърто зелено, но то предупреждава: `mvn -B verify -DskipTests` компилира тестовете, не ги изпълнява, казва `Tests are skipped.` и завършва с `BUILD SUCCESS`. Това е легитимна опция, която се ползва нарочно, когато искаш само `.jar` и вече си изпълнил тестовете преди; добре е да помниш, че това зелено не означава, че нещо е тествано, и че никога не е командата, с която се решава дали една промяна е наред.

#### 7.5.3 Числото се записва

В проект, който вече съществува, начинът да знаеш колко да очакваш, е да го имаш записано: в дневника на курса, до датата. Днешният запис казва 18. Когато добавиш тест, ще бъде 19 и ще го запишеш. Това е евтин начин една неочаквана промяна в броя —нагоре или надолу— да привлече вниманието ти.

### 7.6 Стъпката на проекта: `revisor` с тестове

Имаш всички части. Ето целия проект, такъв какъвто остава в края на урока. Спрямо Урок 6 се променят две неща: `pom.xml` (версия 0.7.0 и JUnit в обхват за тестове, в 7.1.2) и папката `src/test/java`, която престава да е празна. **Кодът на програмата не се промени дори с един ред**: тестовете бяха написани за програмата, която вече съществуваше. Включено е всичко, за да може проектът да се сглоби, без да се гледа друг урок:

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

Вече видя цели `pom.xml` и четирите теста. Твоят `servicios.txt` е обичайният:

```text fig07_01/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

А това са тринайсетте файла на програмата, идентични с тези от Урок 6. Ако си го следвал, вече ги имаш; включени са, за да може проектът на този урок да се изгради, без да зависи от нищо друго.

<details>
<summary>Тринайсетте файла на програмата, без промени спрямо Урок 6</summary>

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

За да завършиш урока: `mvn -B verify` трябва да каже `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` и `BUILD SUCCESS` и да остави `target/revisor-0.7.0.jar`; `java -cp target/classes mx.habil.revisor.Revisor` трябва да отпечата таблицата с резюмето от Урок 4 (4.5) от 843 ms, както в Урок 6, защото програмата не се е променила. Има статия на Hábil за [кои тестове улавят истински грешки и как се мери покритието](https://www.habil.mx/bg/blog/tests-that-catch-errors-coverage-quality-gate/), която допълва този урок от страната на качеството; и друга за [това как се изгражда непрекъснатата интеграция на една софтуерна къща](https://www.habil.mx/bg/blog/cicd-devsecops-own-infrastructure/), мястото, където тези 18 теста се изпълняват сами при всяка промяна.


## Грешката, която ще видиш

Урокът има една основна грешка, тази на теста, който става червен, и няколко спомагателни съобщения, които е добре да се четат с един поглед.

**Основната грешка.** Тя е от 7.3.2. Сменяш `>=` с `>` в правилото за прага, изпълняваш `mvn -B verify` и отчетът казва, че един от редовете на таблицата от случаи, третият, е очаквал `LENTO` и е получил `ARRIBA`. Чете се на четири стъпки, винаги едни и същи: **кой клас** е пропаднал (редът с `<<< FAILURE!`), **кой тест** (името на метода и, в квадратни скоби, номерът на реда), **какво е очаквал и какво е получил** (`expected: <...> but was: <...>`) и **колко** общо (`Tests run: 18, Failures: 1`). С това знаеш какво да смениш, без да отваряш дебъгер, и имаш увереността, че тестът следи това, което казва, че следи. Да го видиш как пропада, нарочно, е първата употреба, която му даваш.

**Спомагателните съобщения**, за да ги разпознаваш с един поглед:

- `org.opentest4j.AssertionFailedError: expected: <...> but was: <...>`: едно твърдение не е изпълнено. Отчетът го брои в `Failures`. Ако стойностите изглеждат наобратно, провери реда на `assertEquals`: очакваната е първа.
- Ред с `Errors: 1` вместо `Failures: 1`: тестът не е стигнал да твърди нищо, защото се е хвърлило изключение, което не е очаквал. Това е друг вид проблем: не „резултатът е друг“, а „кодът се е сринал“.
- `Expected mx.habil.revisor.ErrorDeConfiguracion to be thrown, but nothing was thrown.`: така JUnit казва, че един `assertThrows` не е видял изключението, което е искал. Четенето на файла е приело нещо, което е трябвало да отхвърли. (Измерено като в `Configuracion.java` `timeoutMs > 0` е сменено с `timeoutMs >= 0`: `timeout_ms` със стойност 0 престава да се отхвърля и пада редът `[3]` на `rechazaUnArchivoInvalido`.)
- `[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`: цял клас е бил пропуснат. Жълто, не червено, и общият брой пада: търси допускане (7.5.2).
- `Tests are skipped.`: някой е изпълнил с `-DskipTests`. Това зелено не е тествало нищо.
- Нито един ред `Tests run:` и `BUILD SUCCESS`: *Surefire* не е намерил класове, които да се казват, както той търси (7.5.2).

## Какво се прави погрешно

**Да се доверяваш на `BUILD SUCCESS`.** Вече видя цената в 7.5: проект без нито един ефективен тест завършва в зелено и с код на изход нула. Важното е редът `Tests run:` и числото да е очакваното.

**Да вярваш на тест, който никога не си виждал в червено.** Тест, който винаги минава, не се различава от такъв, който не следи нищо. Преди да го сметнеш за добър, счупи това, което следи, провери, че пропада със съобщението, което очакваш, и върни промяната.

**Тест без твърдение.** Метод с `@Test`, който вика кода, но никога не вика `assertEquals`, нито `assertThrows`, минава винаги, освен ако кодът хвърли изключение: изпълнява кода, не проверява нищо. Зеленото, което дава, не струва повече от това на проект без тестове.

**Да тестваш само щастливия случай или само отхвърлянията.** Клас, който тества само че един добър файл се чете добре, не знае дали лошите се отхвърлят; такъв, който тества само отхвърляния, би минал с четец, който отхвърля всичко. Нужни са двете страни, както в `ConfiguracionTest`.

**Да избираш случаите наслуки.** Грешките се крият на границата: 999 и 1000, 399 и 400. Таблица от случаи, която няма двете страни на всяка граница, оставя ръба без наблюдение, а упражнение 1 измерва колко скъпо излиза това.

**Тестове, които зависят от нещо, което не контролираш:** мрежата, часът на деня, редът, в който се изпълняват, папката, от която се стартират, или файл, който „вече е бил там“. Един тест трябва да дава същия резултат утре и на компютъра на друг човек; за файлове, `@TempDir`; за данни, самият случай да ги сглобява.

**Да коментираш или изтриваш тест, който пропада, „за да мине“.** Тестът е казвал нещо. Разбери пропадането: или кодът е лош, или тестът описва правило, което се е променило, и тогава се обновява тестът (както в упражнение 2), не се заглушава.

**Да орязваш изхода на тестовете с конвейер.** `mvn -B verify | tail` сменя кода на изход с този на `tail` и пропадането изчезва (7.5.2). Тестовете се изпълняват без филтриране или със `set -o pipefail`.

## Упражнения

**Всяко упражнение тръгва от проекта на 7.6, с неговите 18 теста.** Ако едно упражнение ти остави допълнителни тестове или промяна в кода, върни ги назад, преди да започнеш следващото; така броевете, които предсказваш, и тези от решенията са едни и същи.

### Упражнение 1 — Границата, която липсва

Таблицата от случаи на `ClasificadorTest` има две HTTP грешки, 503 и 404, но нито една близо до границата между „отговори добре“ и „отговори с грешка“. Правилото казва, че код **по-голям или равен на 400** оставя услугата паднала. **Преди да пипаш каквото и да било, предскажи:** ако някой смени в `Clasificador.java` сравнението `codigo >= 400` с `codigo > 400`, колко от 18-те теста стават червени? Изпълни го и провери предсказанието си. После добави в таблицата случаите `400, 50, CAIDO` и `399, 50, ARRIBA`, повтори промяната и запиши кой ред става червен и колко теста казва сега отчетът.

### Упражнение 2 — Въпросът от понеделника

Отговори на въпроса, с който започна урокът. Смени `UMBRAL_LENTO_MS` на 800 в `Clasificador.java`. **Предскажи** кой ред от таблицата става червен и защо, и изпълни `mvn -B verify`, за да го провериш. После обнови **теста**, не кода: смени двата реда на границата, така че да следят новата стойност (стойност точно под нея и стойността на прага), и потвърди, че отчетът отново казва `Tests run: 18, Failures: 0`. Запиши в дневника защо тестът трябваше да се промени.

### Упражнение 3 — Нов тест, на ръка

`Formato.contar(long cantidad, String palabra)` добавя „s“ към думата, освен когато количеството е 1 (`1 caído`, `3 caídos`). Напиши в `FormatoTest` параметризиран тест с три реда: `0, servicio` (трябва да даде `0 servicios`), `1, caído` и `3, caído`. **Преди да изпълниш**, запиши колко теста очакваш да каже отчетът (подсказка: сега има три реда повече). После го накарай да пропадне нарочно, като смениш `cantidad == 1` с `cantidad <= 1` във `Formato.java`, и наблюдавай колко теста стават червени и от кои класове. Очакваше ли да са от повече от един клас?

### Упражнение 4 — Зелено, което не е тествало нищо

Преименувай класа `ResumenTest` на `ResumenPrueba`, във файла и в декларацията на класа. Изпълни `mvn -B verify` и отговори на три въпроса: завърши ли с `BUILD SUCCESS`? Колко теста казва сега отчетът? Какъв е кодът на изход, с `echo $?` веднага след това? Възстанови името, потвърди, че броят се връща на 18 (ако някога броят не съвпада след преименуване или изтриване на клас, `mvn -B clean verify` изтрива `target/` и изгражда отначало) и запиши в дневника изречението, с което ще се сещаш да четеш реда `Tests run:`.

## Решения

### Решение 1

**Прогноза.** Нито един не става червен: и 18-те минават. Случаите на грешка в таблицата са 503 и 404 и двата са по-големи от 400 и с двете сравнения. Промяната от `>=` на `>` има значение само за точната стойност 400, а таблицата не я съдържа. Това е истински случай на промяна, която тестовете **не откриват**: дефектът оцелява, а зеленото казва, че всичко е наред.

Двата нови случая се добавят в края на таблицата:

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

С двата случая и промяната от `>=` на `>` в `Clasificador.java` отчетът казва:

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

Редът `[7]` е този на `400, 50, CAIDO`: очаквал е `CAIDO` и е получил `ARRIBA`, защото с `>` 400 вече не е грешка. А общият брой се вдигна на 20, от оригиналните 18 плюс двата нови реда. Редът `399, 50, ARRIBA` минава и в двете версии; там е, за да е следена и другата страна на границата. Върни промяната в `Clasificador.java`: с двата случая още в таблицата отчетът трябва да каже `Tests run: 20, Failures: 0`. В истински проект тези два реда остават, защото следят граница, която преди никой не следеше; за да продължиш с упражненията, махни ги и се върни на 18.

### Решение 2

С прага на 800 правилото следи стойност, която таблицата не е очаквала. Пропада редът `[2]`, `200, 999, ARRIBA`: услуга от 999 милисекунди вече не е горе, а е бавна.

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

`expected: <ARRIBA> but was: <LENTO>` го казва. Останалите редове продължават да минават (142 остава горе, а 1000 и 2300 остават бавни) и не се чупи никой друг тестов клас, защото никой друг не зависи от прага. Пропада тестът и причината е, че **бизнес правилото се е променило**: тестът е описвал стария праг. Затова се обновява той, а не кодът. Двата реда на границата стават `200, 799, ARRIBA` (точно под) и `200, 800, LENTO` (прагът):

```java
    "200, 142,  ARRIBA",
    "200, 799,  ARRIBA",
    "200, 800,  LENTO",
    "200, 2300, LENTO",
```

и отчетът се връща към зелено, със същия общ брой като в началото, защото е променено съдържанието на два реда, без да е добавен нито един:

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

И въпросът от понеделника вече има отговор, написан, изпълним и следен: услуга от точно 800 милисекунди е бавна.

### Решение 3

Новият тест, вътре в класа `FormatoTest`, до онзи, който вече има:

```java
  @ParameterizedTest
  @CsvSource({"0, servicio, 0 servicios", "1, caído, 1 caído", "3, caído, 3 caídos"})
  void pluralizaSalvoCuandoEsUno(long cantidad, String palabra, String esperado) {
    assertEquals(esperado, Formato.contar(cantidad, palabra));
  }
```

Три реда са, така че броят минава от 18 на **21**:

```text
$ mvn -B verify
...
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

С `cantidad <= 1` вместо `cantidad == 1` нулата престава да носи „s“ (`0 servicio`) и този случай пропада:

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

Пропадат **два** теста, от **два класа**: новият (`FormatoTest`, ред `[1]`, този на `0 servicios`) и един, който вече съществуваше, `ResumenTest`, чийто случай „никой не отговори“ очаква `0 lentos`. Нормално е изненадата да е тази: тестовете се пишат за едно нещо, но следят всичко, което минава оттам. Една промяна в споделена функция се открива по-лесно, ако няколко части на програмата зависят от нея. И това е добра причина да не изтриваш „излишен“ тест.

### Решение 4

Без класа `ResumenTest` *Surefire* вече не го намира и отчетът казва:

```text
$ mvn -B verify
...
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
$ echo $?
0
```

`BUILD SUCCESS`, код на изход `0` и **16** теста вместо 18: липсват двата на `ResumenTest`. Ако не знаеше, че правилното число е 18, нищо нямаше да те предупреди. Това е изречението за дневника: **„Tests run: 18, винаги; ако числото се промени, а аз не съм го променил, спирам.“** Когато възстановиш името, броят се връща на 18. Една измерена подробност: с Maven 3.10.0 `mvn -B verify` без `clean` дава вече 16, защото компилаторът открива, че един изходен файл се е променил, прекомпилира целия модул и не оставя в `target/test-classes` стария `ResumenTest.class`; `mvn -B clean verify` също дава 16 и не зависи от това поведение, така че е сигурният начин, когато преименуваш или изтриеш класове.

## Как разбирам, че съм успял

- [ ] `mvn -B verify`, от корена на `~/curso-java/revisor/`, завършва с `BUILD SUCCESS` и казва `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`, и знаеш откъде идва всяко от 18-те (7 + 4 + 5 + 2).
- [ ] `.jar` вече се казва `revisor-0.7.0.jar` и `java -cp target/classes mx.habil.revisor.Revisor` продължава да отпечатва таблицата с `promedio 843ms`.
- [ ] Смени `>=` с `>` в `Clasificador.java`, видя реда `[3]` в червено с `expected: <LENTO> but was: <ARRIBA>` и върна промяната.
- [ ] Изпълни един-единствен тест с `-Dtest=Clase#metodo` и после всички и знаеш защо накрая се изпълняват всички.
- [ ] Преименува `ResumenTest` на `ResumenPrueba`, видя `BUILD SUCCESS` с 16 теста и записа в дневника как ще се сещаш да четеш броя.
- [ ] Можеш да обясниш за какво служи `<scope>test</scope>` и защо JUnit не е в програмата.
- [ ] Записа в дневника си числото, което казва отчетът, с датата.

## Резюме

Отговори, без да гледаш урока:

1. Кои са трите момента на един тест? Кой аргумент на `assertEquals` е първи и какво става, ако ги размениш?
2. За какво служи BOM на JUnit и какво прави `<scope>test</scope>`?
3. Защо този урок тества `Clasificador`, `Formato`, `Resumen` и `Configuracion`, а не `Revision`, нито `Simulador`?
4. Защо кодът се чупи нарочно, преди един тест да бъде сметнат за добър?
5. Какво означава `[3]` в името на параметризиран тест, който пропада? Защо случаите от таблицата се избират около границата?
6. Какво проверява `assertThrows` и какво става, ако кодът не хвърли нищо? Какво прави `@TempDir`?
7. Каква е разликата между `Failures` и `Errors` в отчета?
8. Три начина, по които Maven завършва в зелено, без да е тествал това, което мислиш: кои са и как се предпазваш от всеки?

## За допълнително четене

- [JUnit 6, ръководство за потребителя](https://docs.junit.org/current/user-guide/): анотациите, [твърденията](https://docs.junit.org/current/writing-tests/assertions.html), [допусканията](https://docs.junit.org/current/writing-tests/assumptions.html) и [параметризираните тестове](https://docs.junit.org/current/writing-tests/parameterized-classes-and-tests.html) с всичките им източници на данни, отвъд `@CsvSource`. Посетено на 13 октомври 2026 г.
- [Maven Surefire, „Inclusions and Exclusions of Tests“](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html): кои имена на класове изпълнява и как да се промени, полезно, когато броят не съвпада. Посетено на 7 октомври 2026 г.
- [Maven, „Introduction to the Dependency Mechanism“](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html): обхватите (`test` сред тях), BOM и как се импортира със `<scope>import</scope>`. Посетено на 13 октомври 2026 г.
- [Наръчник на Bash, командата `set`](https://www.gnu.org/software/bash/manual/html_node/The-Set-Builtin.html): какво прави `pipefail` с кода на изход на един конвейер. Посетено на 13 октомври 2026 г.

---

**Предишен:** [Урок 6 — Проект с Maven](06-proyecto-maven.md) ·
**Следващ:** [Урок 8 — HTTP и JSON](08-http-y-json.md)
