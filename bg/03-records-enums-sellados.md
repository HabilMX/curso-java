# Урок 3 — Records, enums и запечатани типове

**Време:** 2 × 90 мин (две сесии; пауза между раздели 3.2 и 3.3)

**Какво изграждаш:** модела на данните на `revisor`: `Servicio` с неговото име, адрес и таймаут, `Estado`, който може да има само три стойности, и `Respuesta`, която е или „услугата отговори“, или „не отговори“, без нито един `null` по средата. `revisor` преминава от един файл към седем

**Какво научаваш:** `record` (компоненти, методи за достъп, `equals`, `hashCode` и `toString` наготово, компактен конструктор и допълнителен конструктор); защо неизменяемостта на един `record` е повърхностна; `enum`; интерфейси `sealed` с `permits`; `switch` с шаблони, изчерпателен и без `default`; защити с `when`; шаблони за record и безименният шаблон `_`; и как се стартира проект от няколко файла с `java Revisor.java`

**Откъде идваш.** Носиш `Revisor.java` от [Урок 2](02-tipos-cadenas-control.md): един-единствен компактен файл с `codigoDe`, `tiempoDe`, `clasificar`, `formatearTiempo`, `detalle`, `linea`, `encabezado`, `revisar` и един `main`, който отпечатва таблицата с четирите услуги. Работи и има една слабост, която упражнението по проекта от урока ти показа: информацията за всяка услуга е **разпиляна** в два паралелни `switch`, а услуга, която не е отговорила, е `null` във всеки от тях. Ако забравиш да добавиш една стойност в единия от двата, програмата се компилира, стартира и показва грешен ред, без никакво оплакване. Този урок поправя това с три средства на езика: типове, които **групират** данни (`record`), типове, които **изброяват** възможности (`enum`), и типове, които казват „това е една от тези форми и никоя друга“ (`sealed`).

**Какво не прави този урок.** Още не използва списъци: четирите услуги продължават да са написани на ръка в `main`, една по една, а проверката им продължава да е симулирана с `switch`. Колекциите идват в [Урок 4](04-colecciones-genericos-streams.md), а истинските грешки, с `try` и `catch`, в 5. Тук `throw` се появява веднъж, в един пример, и се обяснява в отделно каре; пълната му обработка е в Урок 5. Също така в кода на проекта няма `public` или `private`: видимостта се обяснява в Урок 6, заедно с пакетите.

**За изпълненията.** Отделните фигури (`fig03_01.java` до `fig03_15.java`) се стартират с `java fig03_NN.java`. Проектът от няколко файла, фигурата `fig03_16`, е **папка**: влизаш в нея и изпълняваш `java Revisor.java`. Ще ги намериш в [`programas/03-records-enums-sellados/`](../programas/03-records-enums-sellados/). Изходите са получени с Temurin 25.0.4 и `LANG=C.UTF-8`. Фигурите, които се провалят нарочно, носят грешката си във файл `fig03_NN.error-esperado.txt`.

## След урока ще можеш да

- Декларираш `record` и обясниш какво ти дава компилаторът наготово (методи за достъп, `equals`, `hashCode`, `toString`).
- Напишеш компактен конструктор, който нормализира една стойност, и допълнителен конструктор, който допълва стойностите по подразбиране.
- Обясниш защо `record` с масив вътре не е наистина неизменяем.
- Декларираш `enum`, обходиш стойностите му и напишеш `switch` върху него, който компилаторът проверява.
- Декларираш интерфейс `sealed` с позволените му форми и напишеш `switch` с шаблони, който ги покрива всичките без `default`.
- Използваш защита `when`, шаблон за record и безименния шаблон `_` и кажеш в какъв ред се подреждат случаите.
- Покажеш какво се случва с компилатора, когато добавиш нов случай към запечатан тип, и изброиш местата, които те принуждава да промениш.

## Защо, преди как

Досега `revisor` има проблем с **представянето**. За да каже „catalogo отговори 200 за 142 ms“, използва две функции, които не знаят една за друга; за да каже „reportes не отговори“, използва два `null`. Който чете кода, не може да види формата на данните: трябва да я заключи от начина, по който се използват. А и компилаторът не я познава, така че не може да те предупреди, когато някой сгреши: можеш да поискаш кода на услуга, която е отговорила, и времето на такава, която не е, и Java ти го позволява.

Идеята на този урок е едно изречение: **направи така, че компилаторът да познава формата на данните ти, и невъзможните състояния престават да могат да се напишат**. Два конкретни примера от `revisor`:

- Услуга, която не е отговорила, **няма** нито HTTP код, нито време. С по един `null` във всяко можеш да се озовеш с код без време или обратното. Ако вместо това „отговори“ и „не отговори“ са два различни типа и само първият има код и време, вторият не може да ги има, колкото и да искаш.
- Едно състояние е `ARRIBA`, `LENTO` или `CAIDO`. С `String` може да е `"ARIBA"` (с правописна грешка) и програмата го приема. С `enum` не.

Ако идваш от други езици, това ще ти звучи познато: в Rust е `enum` с данни; в TypeScript — дискриминирано обединение; в съвременния Python — `dataclass` с `match`; в Go няма пряк еквивалент и се симулира с интерфейси. Java дойде късно на това тържество — *records* са от Java 16, запечатаните типове от Java 17, а шаблоните в `switch` от Java 21 — но дойде завършена и с проверката на компилатора в комплекта.

Пътят е в две сесии. **Първата** покрива 3.1 и 3.2: *records* и *enums*. Като я завършиш, ще можеш да пишеш типове данни в един ред и ще знаеш кое е и кое не е неизменяемо. **Втората** покрива 3.3 до 3.5: запечатаните типове, `switch` с шаблони и стъпката на проекта. Като я завършиш, `revisor` ще продължава да отпечатва **точно същата таблица** като в края на урок 2, но изградена с типове, а не с `null`. Това е доказателството: поведението не се променя, а кодът е друг.

## Понятията

### 3.1 Records

#### 3.1.1 Проблемът: клас само за да носи данни

Да речем, че искаш да запазиш заедно името на една услуга и нейното време. В Java обичайният път е да се напише клас. Виж как изглежда този път, само за да видиш **размера** (не е нужно да разбираш всеки ред):

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

Това са тридесет и девет реда, за да се каже „име и време“. А това, което прави всеки от тях, е механично: запазване на двете стойности, връщането им, сравняването им и показването им. Това е вид код, който се пише еднакво всеки път и всеки път може да съдържа грешка: да забравиш поле в `equals`, в `hashCode` или в `toString`. (Вече научи защо `equals` е важен: `==` между обекти пита дали са един и същ, а не дали струват едно и също.)

Java 16 даде име на този шаблон: ***record***. Неговата [спецификация](https://openjdk.org/jeps/395) го описва като прозрачен носител на неизменяеми данни, а класът по-горе, цялостен, се записва така:

```java
record Punto(String nombre, long tiempoMs) {}
```

Един ред. Да го видим в действие.

#### 3.1.2 Какво ти дава един `record`

Между скобите са **компонентите**: типът и името на всяка стойност. Компилаторът използва този списък, за да генерира всичко останало. Виж `fig03_01.java`, който използва `record` `Punto` и го изпробва:

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

Всеки ред от изхода показва нещо, което компилаторът е написал вместо теб:

- **Конструктор**, с един параметър за компонент, в същия ред. `new Punto("catalogo", 142)` създава един. (142 е `int` и влиза в компонента `long` чрез преобразуването без загуба от предишния урок.)
- **Един метод за достъп (accessor) за компонент**, със същото име като компонента: `a.nombre()` и `a.tiempoMs()`. Обърни внимание, че **не се казват `getNombre()`**: това е една от малкото конвенции в Java, при които *record* се отклонява от традицията.
- **`toString`**, с формат `Punto[nombre=catalogo, tiempoMs=142]`. Това излиза, когато отпечаташ обекта.
- **`equals`**, който сравнява **компонент по компонент**: `a.equals(b)` е `true`, защото двата струват едно и също, макар да са два различни обекта (`a == b` е `false`, както очакваше след Урок 2). За един `record` `equals` прави това, което ти искаше да прави `==`.
- **`hashCode`**, съгласуван с `equals`: ако два обекта са равни, техният `hashCode` (число, което обобщава съдържанието) съвпада. Още не го използваш; Урок 4 има нужда от него за множествата и речниците.

И има още правила, които се научават с компилатора. Компонентите са **финални**: след като *record* е създаден, никой не се променя. Виж `fig03_02.java`, който се опитва да промени един:

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

Компилаторът го отхвърля: „не може да се присвои стойност на финалната променлива `tiempoMs`“. *Record* е, в основната си форма, стойност, която се създава цялостна и повече не се пипа. Не може да се добави и самостоятелно поле (`int extra;` вътре в *record* дава грешката „field declaration must be static“, с подсказка да се превърне в компонент), а *record* не може да разширява друг клас. Което може: да има **методи**, да реализира **интерфейси** (ще го използваш в 3.3) и да има **константи `static`**.

Това ограничение е смисълът. Един `record` казва със своята форма: „аз съм съвкупност от фиксирани данни и моята идентичност е моето съдържание“. Който го чете, знае какво да очаква, без да прочете нито един ред логика, а който го използва, не може да го счупи случайно.

#### 3.1.3 Допълнителен конструктор и стойностите по подразбиране

`revisor` има нужда от `Servicio` с три стойности: `nombre`, `url` и `timeoutMs`, максималното време, което се чака услугата (урок 8 го използва наистина). Почти всички услуги използват стойността по подразбиране от 5000 ms; няколко имат нужда от друга. Java няма параметри със стойност по подразбиране (казахме го в Урок 2), но един *record* може да има **допълнителен конструктор**, по-къс, който извиква основния. Виж `fig03_03.java`:

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

В тази фигура има четири новости.

**`URI`** е клас от JDK, който представлява адрес. `URI.create("https://catalogo.example")` създава такъв от текст, а `getHost()` връща сървъра (`catalogo.example`). Намира се в пакета `java.net`. За да използваш клас от друг пакет в обикновен файл, трябва да го **импортираш** с ред `import java.net.URI;` в началото (ще го видиш в проекта в 3.5); компактният файл импортира вместо теб всичко, което носи модулът `java.base`, а `URI` е част от него, така че тук не беше нужно.

**Константа вътре в *record*.** `static final int TIMEOUT_POR_OMISION_MS = 5000;` е константа като тази от предишния урок; `static` я кара да принадлежи на типа, а не на всеки обект, което е единственият начин *record* да има нещо самостоятелно.

**Допълнителният конструктор.** `Servicio(String nombre, URI url)` има два параметъра; тялото му, `this(nombre, url, TIMEOUT_POR_OMISION_MS)`, **извиква основния конструктор** с допълнена третата стойност. С това `new Servicio("catalogo", direccion)` и `new Servicio("notificaciones", ..., 1000)` са валидни извиквания и излизат с подходящото време: 5000 в първото, 1000 във второто. Обърни внимание и на името `timeoutMs`: носи мерната единица, както в предишния урок.

**Извикването, разчупено на два реда.** Когато едно извикване надхвърли 100 колони, то се къса след `=` и продължението влиза с четири интервала повече. Това е ръководството за стил, за което говорихме.

#### 3.1.4 Компактният конструктор: валидиране и нормализиране

Понякога искаш *record* да **изчисти или провери** данните си при създаването: да махне интервалите от едно име, да откаже невъзможен таймаут. За това съществува **компактният конструктор**: пише се с името на *record* и **без скоби**, а тялото му се изпълнява, преди компонентите да бъдат присвоени. Вътре можеш да преприсвояваш параметрите (които носят името на компонентите) и, ако нещо не е наред, да спреш програмата с грешка. Виж `fig03_04.java`:

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

Първото създаване минава през компактния конструктор: `nombre = nombre.strip();` преприсвоява параметъра с почистената му версия (правилото от 2.2.1: `strip` връща нов текст, а тук го запазваме), и *record* остава като `Umbral[nombre=lento, limiteMs=1000]`, с името без интервалите, с които е пристигнало. Второто създаване има `limiteMs` със стойност 0, а инструкцията `throw` спира изпълнението.

> **Каре за `throw`.** `throw new IllegalArgumentException("...")` означава „тук има проблем, който не знам как да реша; спри каквото правиш и предупреди“. `IllegalArgumentException` е клас от JDK, чието име значи „недопустим аргумент“ (illegal argument). Когато никой не прихване изключението, програмата завършва, а JVM отпечатва съобщението и **следата на стека (stack trace)** (списъкът с мястото, където е станало: тук, конструкторът `Umbral.<init>`, извикан от `main`). Как се прихваща изключение, кога е уместно и кога не, е Урок 5. Засега трябва да знаеш само, че компактният конструктор е мястото, където се отхвърлят данни, които нямат смисъл.

Следата е добра възможност да упражниш четенето. Какво казва, отгоре надолу: грешката е `IllegalArgumentException` със съобщението, което написахме; станала е в `fig03_04$Umbral.<init>` (конструкторът на *record*, вътре в класа на компактния файл) на ред 6, а това извикване е направено от `main` на 14. Знакът `$` отделя имплицитния клас на файла от класа `Umbral`, който живее вътре в него.

В `Servicio` на `revisor` **не валидираме** в компактния конструктор: валидирането на онова, което идва от конфигурационен файл, е задача със собствено място, което ще дойде със собствените изключения (Урок 5) и JSON (Урок 8).

#### 3.1.5 Неизменяемостта е повърхностна

*Record* е неизменяем в смисъл, че **компонентите му не се преприсвояват**. Но ако един компонент е на свой ред обект, който може да се променя, този обект си остава променяем. Неизменяемостта стига само до първото ниво. За да се види, е нужен променяем тип, а най-простият в Java е **масивът**.

> **Масив в три реда.** Масивът е последователност с фиксиран размер от елементи от един и същ тип. `String[] etapas = {"conectar", "esperar"};` създава такъв от два текста. `etapas[0]` е първият елемент (брои се от 0), а `etapas[0] = "otro";` **променя** този елемент: масивите са променяеми. За да отпечаташ съдържанието му, се използва `Arrays.toString(etapas)`; без това отпечатва нечетим адрес. Масивите почти не се използват в ежедневната Java; в Урок 4 ги заменят списъците. Но са най-ясният пример за променяем обект.

Виж `fig03_05.java`:

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

Има две дупки. Първата: `etapas[0] = "CAMBIADO DESDE FUERA"` променя **оригиналния** масив, който подадохме на конструктора, и тъй като *record* пази **същата референция** (не копие), *record* се промени, без никой да го е пипал. Втората: `ruta.etapas()[1] = ...` променя масива през метода за достъп, който връща референцията, която пази. Затова програмата отпечатва две промени във втория ред. *Record* не можа да предотврати нито едната, защото финална е **референцията** `etapas`, а не съдържанието на масива.

Последният ред крие още една изненада. Две `Ruta`, създадени с масиви с еднакво съдържание, **не са равни** според `equals`. `equals` на *record* сравнява компонент по компонент, а за да сравни масивите, използва `equals` на масивите, който пита за идентичност, като `==`. Това е същият проблем от 2.2.2, пак.

Практическият урок: **един *record* е толкова неизменяем, колкото са неговите компоненти.** С текстове, числа, `enum`-и и други неизменяеми *records* няма проблем. С масив или с който и да е обект, който може да се променя, неизменяемостта е илюзия. В Урок 4 ще видиш обичайното решение: да запазиш **непроменяемо копие** на списъка в компактния конструктор (`List.copyOf`) и вратата ще остане затворена.

### 3.2 Enums

`revisor` класифицира всяка услуга в едно от три състояния: `ARRIBA`, `LENTO` или `CAIDO`. В Урок 2 състоянието беше `String`, което значи, че всеки текст беше валиден: `"ARIBA"`, `"caído"`, `""`. Един ***enum*** (от „изброяване“, enumeration) е тип, чиито възможни стойности са **един фиксиран списък, написан от теб**, и нищо повече. Виж `fig03_06.java`:

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

Първият ред, `enum Estado { ARRIBA, LENTO, CAIDO }`, декларира типа с трите му константи. Константите се пишат с главни букви, без ударение (така го фиксира курсът за тези на `revisor`: `CAIDO`, не `CAÍDO`), и се използват като `Estado.LENTO`. Какво става в `main`:

- Отпечатването на стойност от *enum* пише името ѝ: `LENTO`. `name()` връща същото име като текст.
- `ordinal()` казва на коя позиция е декларирана (`ARRIBA` е 0, `LENTO` е 1, `CAIDO` е 2). Използва се рядко и когато се използва, почти винаги е симптом: ако зависиш от реда, промяната на списъка чупи програмата.
- **`==` е правилното сравнение между стойности на *enum*.** Всяка константа съществува само веднъж в цялата програма, така че да попиташ дали две променливи сочат към същата, е точно като да попиташ дали струват едно и също. При *enums* `==` е точно това, което искаш.
- `valueOf("CAIDO")` превръща текст в стойността (и хвърля грешка, ако текстът не е името на никоя), а `values()` връща масив с всичките, в реда, в който са декларирани.

Един *enum* може да има **собствени данни и методи**. `Severidad` има поле `peso`, конструктор, който го получава, и метод `urgente()`; всяка константа носи своята стойност в скоби: `BAJA(1)`, `MEDIA(5)`, `ALTA(10)`. Служи за това информацията, която съответства на всяка стойност, да живее **до стойността**, а не в отделен `switch`.

Най-добрата причина да използваш *enum* се вижда в `aviso`: това е `switch` със стрелки, като тези от предишния урок, **върху *enum*** и без `default`. Компилаторът знае колко стойности има `Estado` и ако `switch` не ги покрива всичките, го отхвърля. Виж `fig03_07.java`, където махаме случая `CAIDO`:

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

Това е същото съобщение като в предишния урок („`switch` не покрива всички възможни стойности“), но с огромна разлика. При текстовете компилаторът изискваше `default`, защото текстовете са безброй. При *enum* **не ти трябва `default`**: компилаторът знае, че са три, и ако някой в бъдеще добави четвърто състояние към *enum*, всеки `switch`, който не го покрива, спира да се компилира. Грешките от типа „пропуснах един случай“ се превръщат от грешки при изпълнение в грешки при компилация. Това е една от най-добрите новини в урока, а втората част я издига на още едно ниво.

С това първата сесия приключва. Преди да продължиш, провери дали можеш да отговориш, без да гледаш: кои четири неща ти дава един `record`, без да ги пишеш? Защо `Ruta` от `fig03_05.java` не е неизменяема? Защо един `switch` върху *enum* не се нуждае от `default`?


### 3.3 Запечатани интерфейси

#### 3.3.1 Интерфейс: тип с няколко форми

Една услуга може да отговори или да не отговори и всеки случай има нужда от **различни данни**: ако е отговорила, има код и време; ако не, има причина. Един `record` описва само една форма. За да се каже „една стойност може да е от една форма или от друга“, е нужен тип, който обединява двете, и в Java този тип е **интерфейсът**.

Интерфейсът е тип, който декларира **какво може да се прави**, без да казва как; всеки клас или *record*, който декларира `implements ИмеНаИнтерфейса`, е едновременно стойност от този тип. Променлива от тип `Respuesta` може да пази всичко, което реализира `Respuesta`. Това е средството, което Java предлага отдавна за „е едно от няколко“, и има един недостатък: **всеки може да добави нова форма**, като напише друг тип, който казва `implements Respuesta`, във всеки файл, и компилаторът няма как да знае колко са.

За `revisor` това е проблем. Класификацията трябва да реши какво да прави с всяка форма на отговор и иска компилаторът да ѝ гарантира, че **не е останала нито една**. Тази гаранция е работата на **запечатаните** интерфейси.

#### 3.3.2 `sealed` и `permits`

Запечатаният интерфейс е интерфейс, който изброява предварително **всички типове, които могат да го реализират**. Пише се с думата `sealed` и клауза `permits`. Виж `fig03_08.java`, който дефинира `Respuesta` на `revisor`:

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

Прочети първия ред: `sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta`. Той казва: „`Respuesta` е интерфейс; позволени са само тези две форми“. Вътре, между фигурните скоби на интерфейса, двете форми са дефинирани като **вложени** *records*: `Respondio(int codigo, long tiempoMs)` и `SinRespuesta(String motivo)`, всеки с `implements Respuesta`. Тип, дефиниран вътре в друг, се назовава с неговия префикс, и оттам имената `Respuesta.Respondio` и `Respuesta.SinRespuesta` в останалата част от програмата. (В проект от няколко файла без пакет не може да се напише `import`, за да се съкратят; ще използваш пълното име.)

Сега виж какво изразява моделът. **Услуга, която е отговорила,** има код и време, винаги и двете. **Такава, която не е отговорила,** има причина и няма нито код, нито време, дори `null`: просто тези данни не съществуват в тази форма. Свърши се с невъзможното състояние от Урок 2 (код без време или обратното). Това е дискриминирано обединение от TypeScript, `enum` с данни от Rust, тип сума от Haskell: същата идея със синтаксиса на Java.

Позволените форми трябва да са `final`, `sealed` или `non-sealed`; *records* са автоматично `final`, така че тук не е нужно да пишем нищо. И трябва да живеят в същия пакет (в нашия случай безименния пакет, в който е целият проект) или модул. Ако някой се опита да добави форма, която я няма в списъка, компилаторът го спира. Това е `fig03_09.java`, който добавя `Redirigida` извън `permits`:

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

Съобщението е ясно: „класът не може да разширява запечатан клас: `Respuesta` (защото не е в неговия `permits`)“. За да се добави нова форма, трябва да се **редактира интерфейсът**, а това редактиране задейства предупрежденията на компилатора в останалата част от програмата. В 3.4 ще видиш кои.

Една подробност във `fig03_08.java`: `switch` на `describir` има два случая, `case Respuesta.Respondio r ->` и `case Respuesta.SinRespuesta s ->`, и няма `default`. Това се обяснява в следващия раздел.

### 3.4 `switch` с шаблони

#### 3.4.1 Шаблони за тип и изчерпателност

В Урок 2 `switch` сравняваше стойност с константи (`case "catalogo"`). От Java 21 един `switch` може да сравнява с **шаблони**: вместо „това струва ли X?“, пита „има ли тази форма?“. Най-простият шаблон е **шаблонът за тип**: `case Respuesta.Respondio r` значи „ако отговорът е `Respondio`, наречи го `r` вътре в този случай“. Вътре `r` вече има точния тип, така че може да се пише `r.codigo()` и `r.tiempoMs()` без никакво преобразуване (cast). Функцията е стандартизирана в [JEP 441](https://openjdk.org/jeps/441).

Важното е какво прави компилаторът със `switch` върху запечатан тип: **знае колко форми има**. Ако случаите покриват всичките, `switch` е **изчерпателен** и не се нуждае от `default`. Ако липсва някоя, не се компилира. Виж `fig03_10.java`, който го илюстрира, като махне формата `SinRespuesta`:

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

Съобщението е същото като при *enum* от 3.2: „`switch` не покрива всички възможни стойности“. Същата гаранция, сега за форми с данни.

А обратното е също толкова реален риск. Един `default` в `switch` върху запечатан тип изключва тази гаранция. Виж `fig03_11.java`. Добавена е трета форма, `Redirigida`, и `switch` на `clasificar` има `default`:

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

Програмата се компилира, стартира и казва, че пренасочването е `CAIDO`. Никой не е решавал това: `default` погълна новата форма и отговори за всички, които не споменава. Това е тиха грешка, най-лошият вид, и компилаторът не можа да предупреди, защото `default` му каза „вече покрих всичко“. **При запечатан тип или при *enum* не пиши `default`.** Ако го пропуснеш, всяка нова форма чупи компилацията във всеки `switch`, който трябва да я вземе предвид, а това е точно каквото искаш.

#### 3.4.2 Защити, шаблони за record и `_`

`revisor` има нужда от повече от разграничаването между „отговори“ и „не отговори“: вътре в „отговори“ има три случая (код за грешка, бавен, нормален). Шаблоните имат две средства за това.

Първото е **защитата** `when`: допълнително условие, което случаят трябва да изпълни. `case Respuesta.Respondio r when r.codigo() >= 400` влиза само ако отговорът е `Respondio` **и** освен това кодът му е 400 или повече. Това вече е `&&` от предишния урок, приложено към шаблона.

Второто е **шаблонът за record**: вместо да назоваваш обекта и после да искаш компонентите му, шаблонът ги **разглобява** в същия случай. `case Respuesta.Respondio(int codigo, long tiempoMs)` значи „ако е `Respondio`, извади кода му в променливата `codigo` и времето му в `tiempoMs`“. Дефиниран е в [JEP 440](https://openjdk.org/jeps/440).

И една подробност за удобство: ако не ти трябва стойността на някой компонент, го заместваш с **безименния шаблон** `_` (окончателен от Java 22, [JEP 456](https://openjdk.org/jeps/456)). Казва „тук има нещо и не ме интересува“. Виж `fig03_12.java`, който реализира класификацията на `revisor` от край до край:

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

Четири случая, в този ред, всеки е правило на `revisor`:

1. `Respondio(int codigo, long _) when codigo >= 400` е правило 2: отговори с грешка. Използва кода и не го интересува времето.
2. `Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS` е правило 3: отговори и се забави. Сега използва времето, а не кода.
3. `Respondio _` е правило 4, това, което остава: всеки друг отговор е ARRIBA. С `_` вместо име на променливата дори не се дава име, защото не се използва.
4. `SinRespuesta _` е правило 1: без отговор, CAIDO.

Случаите се оценяват **отгоре надолу и печели първият, който се изпълни**. Затова редът има значение и компилаторът те пази от невъзможен ред. Виж `fig03_13.java`, където общият случай `Respondio _` е написан преди конкретния:

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

„Този случай е доминиран от предишен случай“ (this case label is dominated by a preceding case label). Първият случай вече хваща **всички** `Respondio`, така че следващите, със защита, никога не биха могли да се изпълнят. Java го отбелязва като грешка, не като предупреждение. Правилото: **от най-конкретното към най-общото**, а случаят без защита за даден тип отива след тези, които я имат.

Преди да продължим, контраст с Урок 2. Там `clasificar` получаваше `Integer` и `Long`, които можеха да са null, и използваше `&&`, за да не разопакова празното. Тук `clasificar` получава **една** `Respuesta` и възможните ѝ форми вече казват какви данни има. Няма null за проверяване, защото няма начин да поискаш кода от отговор, който го няма. Проверката, която преди правеше ти на ръка, сега я прави компилаторът.

#### 3.4.3 `null` и `instanceof`

Има още две неща, които е добре да познаваш, защото се появяват в кода, който ще четеш.

**Един `switch` върху null референция гърми**, освен ако не го предотвратиш. Един `case null` хваща случая, а ако го пропуснеш, `switch` хвърля `NullPointerException`, преди да погледне който и да е друг случай. Виж `fig03_14.java`:

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

`describir` включва `case null` и затова отговаря на `null` с текст. `sinCasoNulo` не го включва и същият `null` завършва в `NullPointerException`, който се вижда накрая. Следата носи ред `at java.base/java.util.Objects.requireNonNull(Objects.java:220)`: числото 220 е ред от самия JDK и в твоята версия може да е друго. Важното е да прочетеш двата следващи реда, които са твои: `sinCasoNulo` на ред 17, извикана от `main` на 26. В `revisor` няма null, които да стигат до `switch`, защото запечатаният тип прави невъзможно да съществуват; но е добре да знаеш какво става, когато има.

**`instanceof` с шаблон** е братът на `switch` за един-единствен въпрос. `respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400` пита „`Respondio` ли е?“, нарича го `r`, ако е, и продължава с условието. Виж `fig03_15.java`:

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

Това е израз, който дава `true` или `false`. Обърни внимание как `&&` действа със съкратено оценяване (short-circuit): `r` съществува, а `r.codigo()` се оценява само ако `instanceof` е бил истина. Използвай го, когато имаш един-единствен въпрос за даден тип; ако има две или повече форми за обработване, изчерпателният `switch` е по-добър, защото компилаторът те пази от липсващите случаи.

### 3.5 Стъпката на проекта: `revisor` в няколко файла

Сега подреждаш `revisor` с всичко, което научи. Има промяна във формата и една в съдържанието.

**Формата.** `Revisor.java`, който беше едно цяло, се разпределя в седем файла, **всички в една и съща папка** `~/curso-java/revisor/`, без никакви подпапки. Естествен въпрос: как се стартира нещо от няколко файла без Maven? С обичайната команда, `java Revisor.java`. От Java 22 стартерът компилира файла, който му даваш, и търси в същата папка останалите файлове `.java`, от които този има нужда ([JEP 458](https://openjdk.org/jeps/458)). Входният файл остава компактен, с `void main()`; останалите са обикновени типове, всеки във файла, който носи неговото име. Това прави възможен проект с няколко части **преди** да говорим за Maven, който идва в Урок 6.

**Съдържанието.** Правят се четири промени и си струва да се изброят, защото са урокът в миниатюра:

1. `String` за състоянието става `enum`.
2. Двата `null` за „без отговор“ стават запечатан интерфейс с две форми.
3. Данните на една услуга се събират в `record`.
4. Двата паралелни `switch` (код и време) стават **един-единствен**, който връща цялата форма.

Частите, една по една:

**`Estado.java`** е *enum* от 3.2, с трите константи. Ръководството за стил на Google пише константите по една на ред.

```java
// fig03_16/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

**`Respuesta.java`** е запечатаният интерфейс от 3.3, такъв какъвто е.

```java
// fig03_16/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

**`Servicio.java`** е *record* от 3.1: името, адресът и таймаутът, с допълнителния конструктор, който допълва стойността по подразбиране. Това е първият път, когато виждаш истински `import`: обикновеният файл не импортира нищо сам по себе си, така че `URI` се взима с един ред.

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

**`Resultado.java`** събира онова, което `revisor` знае за една вече проверена услуга: коя е, в какво `Estado` е останала и какъв е бил нейният `Respuesta`. Има един метод, `detalle()`, който използва `switch` с шаблони за record: ако е отговорила, детайлът е кодът като текст; ако не, е причината. Без `default` и без null.

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

**`Clasificador.java`** съдържа класификацията от 3.4.2. Това е `final` клас (не може да се разширява), чиято единствена работа е да групира една функция и константата, която използва. Методите и константите `static` принадлежат на класа, а не на обект, и се извикват с името на класа отпред: `Clasificador.clasificar(respuesta)`. (Урок 6 обяснява `private`, думата, която затваря тази врата, за да не създава никой `Clasificador`, който не служи за нищо.) Вижда се, че `clasificar` сега **връща `Estado`**, а не `String`.

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

**`Formato.java`** събира онова, което форматира. Има важна промяна в сигнатурата спрямо предишния урок: `formatearTiempo` сега получава `long`, не `Long`. Вече не трябва да решава какво да прави с null, защото случаят „без време“ не стига до нея. Който решава, е функцията `tiempo(Respuesta)`, с изчерпателен `switch`: ако има време, го форматира, а ако не, пише чертата. Типът свърши работата, която преди вършеше проверката `if (tiempoMs == null)`. Шаблонът на колоните, който преди беше написан два пъти (в `linea` и в `encabezado`), сега е константата `PATRON_LINEA`. Използва се `import java.util.Locale;`, защото файлът не е компактен.

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

**`Revisor.java`** е единственият компактен файл и входната точка. `simular` е **единственият `switch`**, който замества `codigoDe` и `tiempoDe`: връща `Respondio` с кода и времето заедно или `SinRespuesta` за всяка услуга, която не познава. `revisar` получава `Servicio`, симулира отговора, класифицира го и връща `Resultado`. `main` създава четирите услуги с `URI.create(...)` и ги отпечатва. (Четирите са написани една по една, защото още няма списъци; Урок 4 го поправя.)

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

За да го стартираш:

```text
$ cd fig03_16
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Това е **знак по знак същата таблица** като в края на Урок 2, с една разлика, която не се вижда и е важна: сега, за да излезе `reportes` така, както излезе, никой не е написал `null`. Типът `SinRespuesta` е това, което прави така, че няма време, че детайлът му е причината и че състоянието му е `CAIDO`.

Доброто в дизайна се проявява, когато го промениш. Упражнение 4 от този урок е точно това.

## Грешката, която ще видиш

Урокът има едно основно съобщение и четири спомагателни.

**Основната грешка: „the switch expression does not cover all possible input values“.** Това е, което виждаш, когато на един `switch` върху `enum` или запечатан интерфейс му липсва случай, и е **същият текст и в двата случая**. Появява се във `fig03_07.java` (*enum*) и във `fig03_10.java` (запечатаният интерфейс):

```text
fig03_10.java:9: error: the switch expression does not cover all possible input values
  return switch (respuesta) {
         ^
```

Това е знакът, че компилаторът **ти помага**: махна случай или добави форма и има място, което трябва да прегледаш. Не го заглушавай с `default`. Прочети реда, който сочи `^`, и реши за тази форма какво искаш да се случи.

**Четирите спомагателни съобщения:**

- `error: cannot assign a value to final variable tiempoMs`: опита се да промениш компонент на *record*. Не може. Ако ти трябва друга стойност, създай друг *record* (`fig03_02.java`).
- `error: class is not allowed to extend sealed class: Respuesta (as it is not listed in its 'permits' clause)`: поиска да реализираш запечатан интерфейс с тип, който не е в списъка. Добави го в `permits` (`fig03_09.java`).
- `error: this case label is dominated by a preceding case label`: общ случай е преди конкретен. Подреди от най-конкретното към най-общото (`fig03_13.java`).
- `IllegalArgumentException` с твоето съобщение, от компактен конструктор: някой се опита да създаде *record* с данни, които твоят конструктор отхвърля. Следата казва кой се е опитал (`fig03_04.java`).

## Какво се прави погрешно

**`default` в `switch` върху запечатан тип или *enum*.** Заглушава проверката, която оправдава написването им, както показа `fig03_11.java`: програмата се компилира и класифицира грешно едно пренасочване. Ако новата форма заслужава същото третиране като друга, напиши отделен случай за нея (или събери двете със запетая).

**`record` с масив или с който и да е обект, който може да се променя.** Изглежда неизменяем и не е: променя се през метода за достъп и през оригиналната референция, а неговият `equals` сравнява масиви по идентичност. В Урок 4 ще видиш `List.copyOf`.

**`String`, където се побира `enum`.** Състояние, вид, ниво. Всяка стойност, която идва от фиксиран списък, трябва да е *enum*, за да не е правописната грешка ново състояние.

**`null`, където се побира форма.** Два паралелни `null` (код, време) допускат невъзможни състояния. Ако „няма данни“ е възможност със собствен смисъл, дай ѝ собствена форма в запечатан тип, с данните, които наистина има (тук, причината).

**Използване на `ordinal()` за нещо, което не е дебъгване.** Номерът на една константа се променя, ако някой пренареди *enum*.

**Сравняване на `enum`-и с `equals`.** Работи, но `==` е идиоматичното и избягва `NullPointerException`, ако променливата е null (`estado.equals(...)` гърми с `null`, `estado == ...` не).

**Общ случай преди конкретен.** Компилаторът го открива, но е грешка, която ти е добре да разбереш, а не само да поправиш: редът на случаите е част от значението.

**Дълги валидации в компактния конструктор.** Той е мястото за нормализиране и за отхвърляне на очевидно абсурдното. Бизнес логиката отива другаде.

## Упражнения

### Упражнение 1 — Какво равенство ти дава един `record`

Декларирай `record Servicio(String nombre, URI url, int timeoutMs) {}` в компактен файл и създай три обекта: `a` и `b` с еднакви стойности (таймаут 5000) и `c` като тях, но с 1000. **Преди да стартираш**, предвиди резултата от `a == b`, `a.equals(b)`, `a.equals(c)`, `a.hashCode() == b.hashCode()` и от отпечатването на `c`. Стартирай и сравни.

### Упражнение 2 — Затваряне на една врата на неизменяемостта

Вземи `fig03_05.java` и добави към `Ruta` компактен конструктор, който запазва **копие** на масива (масивите имат метод `clone()`, който връща копие). Стартирай отново. Коя от двете промени престана да засяга *record*? Коя продължава да се случва? Какво ти трябва, за да затвориш другата врата?

### Упражнение 3 — Ново състояние в `enum`

В `enum` на `fig03_06.java` добави четвърта стойност, `DEGRADADO`, между `LENTO` и `CAIDO`, и не пипай нищо друго. Опитай да стартираш. Какво казва компилаторът и в коя функция? После го поправи със съобщение, което ти се струва разумно за новото състояние, и запиши в дневника си какво би се случило, ако `Estado` беше `String`.

### Упражнение 4 — Проектът: още една форма на отговор

В проекта от 3.5 добави трета форма към `Respuesta`: `record Redirigida(int codigo, String destino)`, която представлява услуга, отговорила с код 3xx и казва накъде да се насочи. Направи го в `Respuesta.java` (формата и `permits`) и **не пипай нищо друго**. Стартирай `java Revisor.java`. Запиши **всяко място**, което компилаторът те принуждава да поправиш, едно по едно: поправи ги и стартирай отново, докато се компилира. Реши какво искаш да се случи с едно пренасочване: какво състояние му съответства, какво време показва, какъв детайл?

## Решения

### Решение 1

```text
false
true
false
true
Servicio[nombre=pagos, url=https://pagos.example, timeoutMs=1000]
```

`a == b` е `false`: това са два различни обекта. `a.equals(b)` е `true`: `equals` на *record* сравнява компонент по компонент (а `equals` на `URI`, на свой ред, сравнява съдържанието). `a.equals(c)` е `false`: различават се по `timeoutMs`. `hashCode` на `a` и `b` съвпадат, защото `equals` ги обявява за равни, а *record* гарантира тази съгласуваност. А `c` се отпечатва с обичайния формат.

### Решение 2

Компактният конструктор, който запазва копието:

```java
record Ruta(String nombre, String[] etapas) {
  Ruta {
    etapas = etapas.clone();
  }
}
```

С него изходът е:

```text
tras cambiar el original: [conectar, esperar]
tras cambiar por el accesor: [conectar, CAMBIADO DESDE EL ACCESOR]
```

Първата дупка се затвори: промяната на оригиналния масив вече не засяга *record*, защото той пази собствено копие. Втората остава отворена: методът за достъп връща референцията към копието, което пази, и промяната му отвън го променя. За да се затвори, трябва да се презапише методът за достъп, така че да връща друго копие; в един *record* методът за достъп, написан на ръка, трябва да е `public` (Урок 6 обяснява какво значи това), и дори тогава `equals` пак би сравнявал масиви по идентичност. Практическият извод е, че **масивът е лош компонент**. В следващия урок ще видиш `List.copyOf`, който затваря и двете врати.

### Решение 3

Компилаторът отхвърля програмата в `aviso`, функцията със `switch`:

```text
error: the switch expression does not cover all possible input values
  return switch (estado) {
         ^
```

Липсва случаят `DEGRADADO`. Поправя се с добавянето на `case DEGRADADO -> "revisar ya";` (или съобщението, което избереш). При `String` нищо нямаше да предупреди: `"DEGRADADO"` щеше да е просто още един текст, `aviso` щеше да падне в `default` (който щеше да е задължителен) и деградиралата услуга щеше да получи предупреждението за непознатите. *Enum* превърна този пропуск в грешка при компилация.

### Решение 4

С новата форма, добавена само в `Respuesta.java`:

```java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}
```

компилаторът се оплаква първо от `Clasificador.java` (пълният път до твоята папка се появява пред името на файла; тук го съкращавам):

```text
Clasificador.java:6: error: the switch expression does not cover all possible input values
    return switch (respuesta) {
           ^
```

Добавя се един случай (например `case Respuesta.Redirigida _ -> Estado.ARRIBA;`: пренасочването е валиден отговор) и при ново стартиране следващото е `Formato.java:15` (функцията `tiempo`), а след него `Resultado.java:4` (методът `detalle`). **Те са три места**: класификацията, показаното време и детайлът. При моето изпълнение компилаторът ги показа един по един, в този ред. Можеш да решиш различно какво прави всяко: състояние `ARRIBA` е разумно, защото услугата е отговорила, времето може да е чертата (нямаме такова във формата) и детайлът може да е кодът с местоназначението. Важното не е решението, а че компилаторът те **принуди да го вземеш** на всяко място. С два паралелни `null` нищо нямаше да те предупреди.

## Как разбирам, че съм успял

- [ ] `java fig03_01.java` отпечатва `Punto[nombre=catalogo, tiempoMs=142]` и можеш да обясниш защо `a == b` е `false`, а `a.equals(b)` е `true`.
- [ ] `java fig03_05.java` показва двете промени на `Ruta` и можеш да кажеш защо се случват.
- [ ] Във `fig03_10.java` виждаш грешката „does not cover all possible input values“ и знаеш кой случай е липсвал.
- [ ] Във `fig03_11.java` виждаш, че `default` класифицира едно пренасочване като `CAIDO`, и можеш да кажеш защо това е по-лошо от предишната грешка.
- [ ] От `fig03_16/` `java Revisor.java` отпечатва точно таблицата с четирите реда от Урок 2: `catalogo` ARRIBA 142ms, `inventario` LENTO 2.3s, `pagos` ARRIBA 87ms, `reportes` CAIDO — „sin conexión“ (без връзка).
- [ ] След добавянето на `Redirigida` компилаторът ти показа три места и ти ги поправи едно по едно.
- [ ] Можеш да обясниш със свои думи защо `revisor` от този урок няма нито един `null`.
- [ ] Записа в дневника си невъзможното състояние, което запечатаният тип те възпря да напишеш, и какво се случи, когато добави нов случай.

## Обобщение

Отговори, без да гледаш урока:

1. Кои четири неща генерира компилаторът за един `record`? Какво сравнява неговият `equals`?
2. Каква е разликата между компактен и допълнителен конструктор и за какво служи всеки?
3. Защо `record` с масив вътре не е неизменяем? Какво се прави в Урок 4, за да се поправи?
4. Защо един `enum` се сравнява с `==`? Какво предимство дава писането на `switch` върху `enum` без `default`?
5. Какво казва `sealed ... permits` и какво става, ако се опиташ да реализираш интерфейса с тип, който не е в списъка?
6. Какво е защита `when`, шаблон за record и шаблонът `_`? Дай по един пример за всеки с `revisor`.
7. Защо редът на случаите има значение? Какво става, ако поставиш общия случай пред конкретните?
8. Какво прави един `switch` върху null референция, ако няма `case null`?
9. Какво се промени в сигнатурата на `formatearTiempo` между Урок 2 и 3 и какво го направи възможно?
10. Защо `default` в `switch` върху запечатан тип е риск?

## За допълнително четене

- [Java Tutorials, „Record Classes“](https://docs.oracle.com/en/java/javase/25/language/records.html): *records*, обяснени с повече примери, от официалната документация (на английски). Посетено на 7 октомври 2026 г.
- [JEP 409, „Sealed Classes“](https://openjdk.org/jeps/409): защо и пълните правила за `sealed`, `permits`, `final` и `non-sealed`. Посетено на 7 октомври 2026 г.
- [JEP 441, „Pattern Matching for switch“](https://openjdk.org/jeps/441) и [JEP 440, „Record Patterns“](https://openjdk.org/jeps/440): точните правила за изчерпателността, доминирането и защитите. Посетено на 7 октомври 2026 г.
- [Java Tutorials, „Enum Types“](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html): *enums* с полета, конструктори и методи. Посетено на 7 октомври 2026 г.

---

**Предишен:** [Урок 2 — Типове, низове и управление на потока](02-tipos-cadenas-control.md) ·
**Следващ:** [Урок 4 — Колекции, генерични типове и стриймове](04-colecciones-genericos-streams.md)
