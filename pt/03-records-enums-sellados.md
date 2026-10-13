# Lição 3 — Records, enums e tipos selados

**Tempo:** 2 × 90 min (duas sessões; o corte fica entre as seções 3.2 e 3.3)

**O que você constrói:** o modelo de dados do `revisor`: um `Servicio` com seu nome, seu endereço e seu tempo limite, um `Estado` que só pode valer três coisas, e uma `Respuesta` que é ou “o serviço respondeu” ou “não respondeu”, sem nenhum `null` no meio. O `revisor` passa de um arquivo para sete

**O que você aprende:** `record` (componentes, acessores, `equals`, `hashCode` e `toString` de graça, construtor compacto e construtor adicional); por que a imutabilidade de um `record` é superficial; `enum`; interfaces `sealed` com `permits`; `switch` com padrões, exaustivo e sem `default`; guardas com `when`; padrões de record e o padrão sem nome `_`; e como rodar um projeto de vários arquivos com `java Revisor.java`

**De onde você vem.** Você traz o `Revisor.java` da [Lição 2](02-tipos-cadenas-control.md): um único arquivo compacto com `codigoDe`, `tiempoDe`, `clasificar`, `formatearTiempo`, `detalle`, `linea`, `encabezado`, `revisar` e um `main` que imprime a tabela de quatro serviços. Funciona, e tem uma fraqueza que o exercício de projeto da lição deixou à vista: a informação de cada serviço vive **repartida** em dois `switch` paralelos, e um serviço que não respondeu é um `null` em cada um. Se você esquece de acrescentar um dado em um dos dois, o programa compila, roda e mostra uma linha errada, sem nenhuma queixa. Esta lição corrige isso com três ferramentas da linguagem: tipos que **agrupam** dados (`record`), tipos que **enumeram** possibilidades (`enum`), e tipos que dizem “é uma destas formas e nenhuma outra” (`sealed`).

**O que esta lição não faz.** Não usa listas ainda: os quatro serviços continuam escritos à mão no `main`, um por um, e sua verificação continua simulada com um `switch`. As coleções chegam na [Lição 4](04-colecciones-genericos-streams.md) e os erros de verdade, com `try` e `catch`, na 5. Aqui aparece `throw` uma vez, em um exemplo, e é explicado em um quadro; seu tratamento completo é da Lição 5. Também não há `public` nem `private` no código do projeto: a visibilidade é explicada na Lição 6, junto com os pacotes.

**Sobre as execuções.** As figuras soltas (`fig03_01.java` a `fig03_15.java`) são executadas com `java fig03_NN.java`. O projeto de vários arquivos, a figura `fig03_16`, é uma **pasta**: você entra nela e roda `java Revisor.java`. Você as tem em [`programas/03-records-enums-sellados/`](../programas/03-records-enums-sellados/). As saídas foram obtidas com o Temurin 25.0.4 e `LANG=C.UTF-8`. As figuras que falham de propósito trazem seu erro em um arquivo `fig03_NN.error-esperado.txt`.

## Ao terminar, você vai conseguir

- Declarar um `record` e explicar o que o compilador lhe dá de graça (acessores, `equals`, `hashCode`, `toString`).
- Escrever um construtor compacto que normalize um dado, e um construtor adicional que complete valores padrão.
- Explicar por que um `record` com um array dentro não é de verdade imutável.
- Declarar um `enum`, percorrer seus valores e escrever um `switch` sobre ele que o compilador verifica.
- Declarar uma interface `sealed` com suas formas permitidas e escrever um `switch` com padrões que cubra todas sem `default`.
- Usar uma guarda `when`, um padrão de `record` e o padrão sem nome `_`, e dizer em que ordem vão os casos.
- Mostrar o que acontece com o compilador quando você acrescenta um caso novo a um tipo selado, e listar os lugares que ele obriga você a mudar.

## O porquê antes do como

Até agora o `revisor` tem um problema de **representação**. Para dizer “catalogo respondeu 200 em 142 ms” usa duas funções que não sabem uma da outra; para dizer “reportes não respondeu” usa dois `null`. Um leitor do código não consegue ver a forma dos dados: tem de deduzi-la de como são usados. E o compilador tampouco a conhece, então não pode avisar quando alguém erra: você pode pedir o código de um serviço que de fato respondeu e o tempo de um que não, e o Java permite.

A ideia desta lição é uma frase: **faça o compilador conhecer a forma dos seus dados, e os estados impossíveis deixam de poder ser escritos**. Dois exemplos concretos do `revisor`:

- Um serviço que não respondeu **não tem** código HTTP nem tempo. Com um `null` em cada um, você pode acabar com um código sem tempo, ou o contrário. Se, em vez disso, “respondeu” e “não respondeu” são dois tipos diferentes, e só o primeiro tem código e tempo, o segundo não pode tê-los ainda que você queira.
- Um estado é `ARRIBA`, `LENTO` ou `CAIDO`. Com um `String` pode ser `"ARIBA"` (com um erro de digitação) e o programa aceita. Com um `enum`, não.

Se você vem de outras linguagens, isto vai soar familiar: em Rust é um `enum` com dados; em TypeScript, uma união discriminada; em Python moderno, um `dataclass` com `match`; em Go não há um equivalente direto, e se simula com interfaces. O Java chegou tarde a esta festa —os *records* são do Java 16, os tipos selados do Java 17 e os padrões no `switch` do Java 21—, mas chegou completo e com a verificação do compilador incluída.

O caminho tem duas sessões. A **primeira** cobre 3.1 e 3.2: os *records* e os *enums*. Ao terminá-la você poderá escrever tipos de dados em uma linha e saberá o que é e o que não é imutável. A **segunda** cobre 3.3 a 3.5: os tipos selados, o `switch` com padrões e o passo do projeto. Ao terminá-la, o `revisor` continuará imprimindo **exatamente a mesma tabela** de quando terminou a lição 2, mas construída com tipos e não com `null`. Essa é a prova: o comportamento não muda e o código é outro.

## Os conceitos

### 3.1 Records

#### 3.1.1 O problema: uma classe só para carregar dados

Suponha que você queira guardar juntos o nome de um serviço e seu tempo. Em Java, o caminho de sempre é escrever uma classe. Veja como é esse caminho, só para ver o **tamanho** (não é preciso entender cada linha):

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

São trinta e nove linhas para dizer “um nome e um tempo”. E o que cada uma faz é mecânico: guardar os dois dados, devolvê-los, compará-los e mostrá-los. É o tipo de código que se escreve igual toda vez, e que toda vez pode ter um erro: esquecer um campo no `equals`, ou no `hashCode`, ou no `toString`. (Você já aprendeu por que o `equals` importa: `==` entre objetos pergunta se são o mesmo, e não se valem o mesmo.)

O Java 16 deu nome a esse padrão: o ***record***. Sua [especificação](https://openjdk.org/jeps/395) o descreve como um portador transparente de dados imutáveis, e a classe de cima, completa, se escreve assim:

```java
record Punto(String nombre, long tiempoMs) {}
```

Uma linha. Vejamos em ação.

#### 3.1.2 O que um `record` lhe dá

Entre os parênteses vão os **componentes**: o tipo e o nome de cada dado. O compilador usa essa lista para gerar todo o resto. Veja o `fig03_01.java`, que usa o `record` `Punto` e o testa:

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

Cada linha de saída mostra algo que o compilador escreveu por você:

- **Um construtor**, com um parâmetro por componente, na mesma ordem. `new Punto("catalogo", 142)` cria um. (O 142 é um `int`, e entra no componente `long` pela conversão sem perda da lição anterior.)
- **Um acessor por componente**, com o mesmo nome do componente: `a.nombre()` e `a.tiempoMs()`. Repare que **não se chamam `getNombre()`**: é uma das poucas convenções do Java em que o *record* se afasta da tradição.
- **`toString`**, com o formato `Punto[nombre=catalogo, tiempoMs=142]`. É o que sai ao imprimir o objeto.
- **`equals`**, que compara **componente por componente**: `a.equals(b)` é `true` porque os dois valem o mesmo, ainda que sejam dois objetos diferentes (`a == b` é `false`, como você esperava depois da Lição 2). Para um `record`, `equals` faz o que você queria que o `==` fizesse.
- **`hashCode`**, coerente com `equals`: se dois objetos são iguais, seu `hashCode` (um número que resume o conteúdo) coincide. Você ainda não o usa; a Lição 4 precisa dele para os conjuntos e os dicionários.

E há mais regras, que se aprendem com o compilador. Os componentes são **finais**: uma vez criado o *record*, nenhum muda. Veja o `fig03_02.java`, que tenta mudar um:

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

O compilador o rejeita: “não é possível atribuir um valor à variável final `tiempoMs`”. Um *record* é, em sua forma básica, um valor que se cria completo e não se toca mais. Também não se pode acrescentar um campo solto (`int extra;` dentro do *record* dá o erro “field declaration must be static”, com a sugestão de convertê-lo em um componente), e um *record* não pode estender outra classe. O que ele pode: ter **métodos**, implementar **interfaces** (você usará isso em 3.3) e ter **constantes `static`**.

Essa restrição é o ponto. Um `record` diz com sua forma: “sou um conjunto de dados fixos, e minha identidade é meu conteúdo”. Quem o lê sabe o que esperar sem ler uma única linha de lógica, e quem o usa não pode quebrá-lo por acidente.

#### 3.1.3 Um construtor adicional e os valores padrão

O `revisor` precisa de um `Servicio` com três dados: um `nombre`, uma `url` e um `timeoutMs`, o tempo máximo que se espera por ele (a lição 8 o usa de verdade). Quase todos os serviços usam o valor padrão de 5000 ms; uns poucos precisam de outro. O Java não tem parâmetros com valor padrão (dissemos isso na Lição 2), mas um *record* pode ter um **construtor adicional**, mais curto, que chama o principal. Veja o `fig03_03.java`:

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

Há quatro novidades nessa figura.

**`URI`** é uma classe do JDK que representa um endereço. `URI.create("https://catalogo.example")` cria uma a partir de um texto, e `getHost()` devolve o servidor (`catalogo.example`). Está no pacote `java.net`. Para usar uma classe de outro pacote em um arquivo normal, é preciso **importá-la** com uma linha `import java.net.URI;` no começo (você verá isso no projeto de 3.5); o arquivo compacto importa por você tudo o que o módulo `java.base` traz, e `URI` faz parte dele, então aqui não foi necessário.

**Uma constante dentro de um *record*.** `static final int TIMEOUT_POR_OMISION_MS = 5000;` é uma constante como a da lição anterior; `static` faz com que ela pertença ao tipo e não a cada objeto, que é a única maneira de um *record* ter algo solto.

**O construtor adicional.** `Servicio(String nombre, URI url)` tem dois parâmetros; seu corpo, `this(nombre, url, TIMEOUT_POR_OMISION_MS)`, **chama o construtor principal** com o terceiro dado completado. Com isso, `new Servicio("catalogo", direccion)` e `new Servicio("notificaciones", ..., 1000)` são chamadas válidas e saem com o tempo que corresponde: 5000 na primeira, 1000 na segunda. Repare também no nome `timeoutMs`: leva a unidade, como na lição anterior.

**A chamada partida em duas linhas.** Quando uma chamada passa de 100 colunas, corta-se depois do `=` e a continuação entra com quatro espaços a mais. É o guia de estilo de que falamos.

#### 3.1.4 O construtor compacto: validar e normalizar

Às vezes você quer que o *record* **limpe ou verifique** seus dados ao ser criado: tirar espaços de um nome, rejeitar um tempo impossível. Para isso existe o **construtor compacto**: escreve-se com o nome do *record* e **sem parênteses**, e seu corpo é executado antes que os componentes sejam atribuídos. Dentro você pode reatribuir os parâmetros (que têm o nome dos componentes) e, se algo está errado, interromper o programa com um erro. Veja o `fig03_04.java`:

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

A primeira criação passa pelo construtor compacto: `nombre = nombre.strip();` reatribui o parâmetro com sua versão limpa (a regra de 2.2.1: `strip` devolve um texto novo, e aqui, sim, o guardamos), e o *record* fica como `Umbral[nombre=lento, limiteMs=1000]`, com o nome sem os espaços com que chegou. A segunda criação tem `limiteMs` em 0, e a instrução `throw` interrompe a execução.

> **Um quadro sobre `throw`.** `throw new IllegalArgumentException("...")` significa “aqui há um problema que não sei resolver; interrompa o que está fazendo e avise”. `IllegalArgumentException` é uma classe do JDK cujo nome quer dizer “argumento ilegal”. Quando ninguém a captura, o programa termina e a JVM imprime a mensagem e o **stack trace** (a lista de onde ocorreu: aqui, o construtor `Umbral.<init>` chamado a partir do `main`). Como capturar uma exceção, quando convém e quando não, é a Lição 5. Por ora você só precisa saber que um construtor compacto é o lugar para rejeitar dados que não fazem sentido.

O stack trace é uma boa oportunidade de praticar a leitura. O que ele diz, de cima para baixo: o erro foi `IllegalArgumentException` com a mensagem que escrevemos; ocorreu em `fig03_04$Umbral.<init>` (o construtor do *record*, dentro da classe do arquivo compacto) na linha 6, e essa chamada foi feita pelo `main` na 14. O `$` separa a classe implícita do arquivo da classe `Umbral` que vive dentro dela.

No `Servicio` do `revisor` **não validamos** no construtor compacto: a validação do que chega de um arquivo de configuração é uma tarefa que tem seu lugar próprio, que chegará com as exceções próprias (Lição 5) e o JSON (Lição 8).

#### 3.1.5 A imutabilidade é superficial

Um *record* é imutável no sentido de que **seus componentes não são reatribuídos**. Mas se um componente é, por sua vez, um objeto que pode ser modificado, esse objeto continua modificável. A imutabilidade chega só até a primeira camada. Para ver isso é preciso um tipo mutável, e o mais simples do Java é o **array**.

> **Um array em três linhas.** Um array é uma sequência de tamanho fixo de elementos do mesmo tipo. `String[] etapas = {"conectar", "esperar"};` cria um de dois textos. `etapas[0]` é o primeiro elemento (conta-se a partir de 0), e `etapas[0] = "otro";` **muda** esse elemento: os arrays são mutáveis. Para imprimir seu conteúdo usa-se `Arrays.toString(etapas)`; sem isso, imprime um endereço ilegível. Os arrays quase não são usados no Java do dia a dia; na Lição 4 as listas os substituem. Mas são o exemplo mais claro de um objeto mutável.

Veja o `fig03_05.java`:

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

Há dois buracos. O primeiro: `etapas[0] = "CAMBIADO DESDE FUERA"` modifica o array **original** que passamos ao construtor, e como o *record* guarda **a mesma referência** (não uma cópia), o *record* mudou sem que ninguém o tocasse. O segundo: `ruta.etapas()[1] = ...` modifica o array por meio do acessor, que devolve a referência que guarda. Por isso o programa imprime duas mudanças na segunda linha. O *record* não conseguiu evitar nenhum dos dois, porque o que é final é a **referência** `etapas`, não o conteúdo do array.

A última linha tem outra surpresa. Duas `Ruta` criadas com arrays de igual conteúdo **não são iguais** segundo `equals`. O `equals` do *record* compara componente por componente, e para comparar os arrays usa o `equals` dos arrays, que pergunta por identidade, como o `==`. É o mesmo problema de 2.2.2, outra vez.

A lição prática: **um *record* é tão imutável quanto seus componentes.** Com textos, números, `enum`s e outros *records* imutáveis não há problema. Com um array, ou com qualquer objeto que possa ser modificado, a imutabilidade é uma ilusão. Na Lição 4 você verá a solução habitual para as listas: guardar uma **cópia não modificável** no construtor compacto (`List.copyOf`). Atenção ao que ela fecha e ao que não fecha: ninguém poderá adicionar, remover nem alterar elementos **da lista**, mas `List.copyOf` não copia os elementos nem os torna imutáveis. Basta quando os elementos já são imutáveis, como textos ou *records* de componentes imutáveis (o caso do `revisor`); se fossem objetos modificáveis, seria preciso copiar também cada elemento ou projetá-los como imutáveis.

### 3.2 Enums

O `revisor` classifica cada serviço em um de três estados: `ARRIBA`, `LENTO` ou `CAIDO`. Na Lição 2 o estado era um `String`, o que significa que qualquer texto era válido: `"ARIBA"`, `"caído"`, `""`. Um ***enum*** (de “enumeração”) é um tipo cujos valores possíveis são **uma lista fixa, escrita por você**, e nada mais. Veja o `fig03_06.java`:

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

A primeira linha, `enum Estado { ARRIBA, LENTO, CAIDO }`, declara o tipo com suas três constantes. As constantes se escrevem em maiúsculas, sem acento (assim o curso fixa as do `revisor`: `CAIDO`, não `CAÍDO`), e se usam como `Estado.LENTO`. O que ocorre no `main`:

- Imprimir um valor do *enum* escreve seu nome: `LENTO`. `name()` devolve esse mesmo nome como texto.
- `ordinal()` diz em que posição foi declarado (`ARRIBA` é 0, `LENTO` é 1, `CAIDO` é 2). É pouco usado, e quando é usado, quase sempre é um sintoma: se você depende da ordem, mudar a lista quebra o programa.
- **`==` é a comparação correta entre valores de um *enum*.** Cada constante existe uma única vez em todo o programa, então perguntar se duas variáveis apontam para a mesma é exatamente perguntar se valem o mesmo. Com os *enums*, `==` é justamente o que você quer.
- `valueOf("CAIDO")` converte um texto no valor (e lança um erro se o texto não for o nome de nenhum), e `values()` devolve um array com todos, na ordem em que foram declarados.

Um *enum* pode ter **dados e métodos próprios**. `Severidad` tem um campo `peso`, um construtor que o recebe, e um método `urgente()`; cada constante leva seu dado entre parênteses: `BAJA(1)`, `MEDIA(5)`, `ALTA(10)`. Serve para que a informação que corresponde a cada valor viva **junto ao valor**, e não em um `switch` à parte.

A melhor razão para usar um *enum* se vê em `aviso`: é um `switch` com setas, como os da lição anterior, **sobre um *enum*** e sem `default`. O compilador sabe quantos valores `Estado` tem, e se o `switch` não os cobre todos, rejeita. Veja o `fig03_07.java`, onde tiramos o caso `CAIDO`:

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

É a mesma mensagem da lição anterior (“o `switch` não cobre todos os valores possíveis”), mas com uma diferença enorme. Com textos, o compilador exigia um `default` porque há infinitos textos. Com um *enum* **você não precisa de `default`**: o compilador sabe que são três, e se alguém acrescentar um quarto estado ao *enum* no futuro, cada `switch` que não o cubra deixa de compilar. Os erros de “esqueci um caso” passam de erros de execução a erros de compilação. É uma das melhores notícias da lição, e a segunda parte a leva a outro nível.

Com isso termina a primeira sessão. Antes de seguir, confira se você consegue responder sem olhar: que quatro coisas um `record` lhe dá sem você escrevê-las? Por que a `Ruta` do `fig03_05.java` não é imutável? Por que um `switch` sobre um *enum* não precisa de `default`?


### 3.3 Interfaces seladas

#### 3.3.1 Uma interface: um tipo com várias formas

Um serviço pode responder ou não responder, e cada caso precisa de **dados diferentes**: se respondeu, tem um código e um tempo; se não, tem um motivo. Um `record` só descreve uma forma. Para dizer “um valor pode ser de uma forma ou de outra” é preciso um tipo que reúna as duas, e em Java esse tipo é uma **interface**.

Uma interface é um tipo que declara **o que se pode fazer** sem dizer como; qualquer classe ou *record* que declare `implements NombreDeLaInterfaz` é, ao mesmo tempo, um valor desse tipo. Uma variável de tipo `Respuesta` pode guardar qualquer coisa que implemente `Respuesta`. É a ferramenta que o Java oferece desde sempre para o “é um de vários”, e tem um defeito: **qualquer pessoa pode acrescentar uma forma nova** escrevendo outro tipo que diga `implements Respuesta`, em qualquer arquivo, e o compilador não tem como saber quantas existem.

Para o `revisor` isso é um problema. A classificação precisa decidir o que fazer com cada forma de resposta, e quer que o compilador garanta que **nenhuma ficou de fora**. Essa garantia é o trabalho das interfaces **seladas**.

#### 3.3.2 `sealed` e `permits`

Uma interface selada é uma interface que lista, de antemão, **todos os tipos que podem implementá-la**. Escreve-se com a palavra `sealed` e uma cláusula `permits`. Veja o `fig03_08.java`, que define a `Respuesta` do `revisor`:

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

Leia a primeira linha: `sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta`. Diz: “`Respuesta` é uma interface; só estas duas formas são permitidas”. Dentro, entre as chaves da interface, estão definidas as duas formas como *records* **aninhados**: `Respondio(int codigo, long tiempoMs)` e `SinRespuesta(String motivo)`, cada um com `implements Respuesta`. Um tipo definido dentro de outro é nomeado com seu prefixo, e daí os nomes `Respuesta.Respondio` e `Respuesta.SinRespuesta` no resto do programa. (Em um projeto de vários arquivos sem pacote, não se pode escrever um `import` para encurtá-los; você usará o nome completo.)

Agora veja o que o modelo expressa. **Um serviço que respondeu** tem um código e um tempo, sempre os dois. **Um que não respondeu** tem um motivo, e não tem código nem tempo, nem sequer um `null`: simplesmente esses dados não existem nessa forma. Acabou o estado impossível da Lição 2 (código sem tempo, ou o contrário). É uma união discriminada do TypeScript, um `enum` com dados do Rust, um tipo soma do Haskell: a mesma ideia com a sintaxe do Java.

As formas permitidas têm de ser `final`, `sealed` ou `non-sealed`; os *records* são `final` automaticamente, de modo que aqui não é preciso escrever nada. E têm de viver no mesmo pacote (no nosso caso, o pacote sem nome em que está todo o projeto) ou módulo. Se alguém tenta acrescentar uma forma que não está na lista, o compilador o impede. É o `fig03_09.java`, que acrescenta `Redirigida` fora do `permits`:

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

A mensagem é clara: “a classe não pode estender uma classe selada: `Respuesta` (porque não está em seu `permits`)”. Para acrescentar uma forma nova é preciso **editar a interface**, e essa edição é o que ativa os avisos do compilador no resto do programa. Em 3.4 você verá quais.

Um detalhe do `fig03_08.java`: o `switch` de `describir` tem dois casos, `case Respuesta.Respondio r ->` e `case Respuesta.SinRespuesta s ->`, e não tem `default`. Isso é o que a seção seguinte explica.

### 3.4 `switch` com padrões

#### 3.4.1 Padrões de tipo e exaustividade

Na Lição 2 o `switch` comparava um valor contra constantes (`case "catalogo"`). Desde o Java 21, um `switch` pode comparar contra **padrões**: em vez de “vale isto?”, pergunta “tem esta forma?”. O padrão mais simples é o **padrão de tipo**: `case Respuesta.Respondio r` quer dizer “se a resposta é um `Respondio`, chame-o de `r` dentro deste caso”. Lá dentro, `r` já tem o tipo preciso, então se pode escrever `r.codigo()` e `r.tiempoMs()` sem nenhum cast. O recurso foi padronizado no [JEP 441](https://openjdk.org/jeps/441).

O importante é o que o compilador faz com um `switch` sobre um tipo selado: **sabe quantas formas há**. Se os casos cobrem todas, o `switch` é **exaustivo** e não precisa de `default`. Se falta alguma, não compila. Veja o `fig03_10.java`, que ilustra isso tirando a forma `SinRespuesta`:

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

A mensagem é a mesma do *enum* de 3.2: “o `switch` não cobre todos os valores possíveis”. É a mesma garantia, agora para formas com dados.

E o contrário é um risco igualmente real. Um `default` em um `switch` sobre um tipo selado desliga essa garantia. Veja o `fig03_11.java`. Foi acrescentada uma terceira forma, `Redirigida`, e o `switch` de `clasificar` tem um `default`:

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

O programa compila, roda e diz que um redirecionamento é `CAIDO`. Ninguém decidiu isso: o `default` engoliu a forma nova e respondeu por todas as que não menciona. É um erro silencioso, o pior tipo, e o compilador não pôde avisar porque o `default` lhe disse “já cobri tudo”. **Com um tipo selado, ou com um *enum*, não escreva `default`.** Se você o omitir, cada forma nova quebra a compilação em cada `switch` que deve considerá-la, que é exatamente o que você quer.

#### 3.4.2 Guardas, padrões de record e `_`

O `revisor` precisa de mais do que distinguir entre “respondeu” e “não respondeu”: dentro de “respondeu” há três casos (código de erro, lento, normal). Os padrões têm duas ferramentas para isso.

A primeira é a **guarda** `when`: uma condição adicional que o caso deve cumprir. `case Respuesta.Respondio r when r.codigo() >= 400` entra somente se a resposta é um `Respondio` **e** além disso seu código é 400 ou mais. É o `&&` da lição anterior, aplicado ao padrão.

A segunda é o **padrão de record**: em vez de nomear o objeto e depois pedir seus componentes, o padrão os **desmonta** no mesmo caso. `case Respuesta.Respondio(int codigo, long tiempoMs)` quer dizer “se é um `Respondio`, extraia seu código na variável `codigo` e seu tempo em `tiempoMs`”. Está definido no [JEP 440](https://openjdk.org/jeps/440).

E um detalhe de comodidade: se de um componente você não precisa do valor, substitua-o pelo **padrão sem nome** `_` (final desde o Java 22, [JEP 456](https://openjdk.org/jeps/456)). Diz “aqui há algo, e não me importa”. Veja o `fig03_12.java`, que implementa a classificação do `revisor` de ponta a ponta:

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

Quatro casos, nesta ordem, cada um uma regra do `revisor`:

1. `Respondio(int codigo, long _) when codigo >= 400` é a regra 2: respondeu com erro. Usa o código e não se importa com o tempo.
2. `Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS` é a regra 3: respondeu e demorou. Agora usa o tempo e não o código.
3. `Respondio _` é a regra 4, a que sobra: qualquer outra resposta é ARRIBA. Com `_` em vez de um nome, nem sequer se dá nome à variável, porque não é usada.
4. `SinRespuesta _` é a regra 1: sem resposta, CAIDO.

Os casos são avaliados **de cima para baixo, e ganha o primeiro que se cumpre**. Por isso a ordem importa, e o compilador protege você de uma ordem impossível. Veja o `fig03_13.java`, onde o caso geral `Respondio _` foi escrito antes do específico:

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

“Este caso está dominado por um caso anterior”. O primeiro caso já pega **todos** os `Respondio`, de modo que os que vêm depois, com guarda, nunca poderiam ser executados. O Java marca isso como erro, não como aviso. A regra: **do mais específico ao mais geral**, e o caso sem guarda de um tipo vai depois dos que a têm.

Antes de seguir, um contraste com a Lição 2. Ali `clasificar` recebia um `Integer` e um `Long` que podiam ser nulos, e usava `&&` para não desembrulhar um vazio. Aqui `clasificar` recebe **uma** `Respuesta`, e suas formas possíveis já dizem que dados há. Não há nulos a verificar, porque não há como pedir o código a uma resposta que não o tem. A verificação que antes você fazia, à mão, agora o compilador faz.

#### 3.4.3 `null` e `instanceof`

Há mais duas coisas que convém conhecer, porque aparecem no código que você lerá.

**Um `switch` sobre uma referência nula estoura**, a menos que você o previna. Um `case null` captura o caso, e se você o omite, o `switch` lança uma `NullPointerException` antes de olhar qualquer outro caso. Veja o `fig03_14.java`:

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

`describir` inclui `case null`, e por isso responde ao `null` com um texto. `sinCasoNulo` não o inclui, e o mesmo `null` termina na `NullPointerException` que se vê no final. O stack trace traz uma linha `at java.base/java.util.Objects.requireNonNull(Objects.java:220)`: o número 220 é uma linha do próprio JDK e pode ser outro na sua versão. O que importa é ler as duas linhas seguintes, que são suas: `sinCasoNulo` na linha 17, chamada a partir do `main` na 26. No `revisor` não há nulos que cheguem a um `switch`, porque o tipo selado torna impossível que existam; mas convém saber o que acontece quando existem.

**`instanceof` com padrão** é o irmão do `switch` para uma única pergunta. `respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400` pergunta “é um `Respondio`?”, chama-o de `r` se for, e segue com a condição. Veja o `fig03_15.java`:

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

É uma expressão que dá `true` ou `false`. Repare como o `&&` faz curto-circuito: `r` só existe, e `r.codigo()` só é avaliado, se o `instanceof` foi verdadeiro. Use-o quando tiver uma única pergunta sobre um tipo; se há duas ou mais formas a tratar, um `switch` exaustivo é melhor, porque o compilador cuida dos casos que faltem.

### 3.5 O passo do projeto: o `revisor` em vários arquivos

Agora você organiza o `revisor` com tudo o que aprendeu. Há uma mudança de forma e uma de conteúdo.

**A forma.** O `Revisor.java` de uma peça só é repartido em sete arquivos, **todos na mesma pasta** `~/curso-java/revisor/`, sem nenhuma subpasta. Pergunta natural: como se roda algo de vários arquivos sem Maven? Com o mesmo comando de sempre, `java Revisor.java`. Desde o Java 22, o lançador compila o arquivo que você dá e procura na mesma pasta os demais arquivos `.java` de que ele precisa ([JEP 458](https://openjdk.org/jeps/458)). O arquivo de entrada continua sendo compacto, com `void main()`; os demais são tipos normais, cada um no arquivo que leva seu nome. É o que torna possível um projeto com várias peças **antes** de falar de Maven, que chega na Lição 6.

**O conteúdo.** Fazem-se quatro mudanças, e vale a pena enumerá-las porque são a lição em miniatura:

1. O `String` de estado vira um `enum`.
2. Os dois `null` de “sem resposta” viram uma interface selada com duas formas.
3. Os dados de um serviço são reunidos em um `record`.
4. Os dois `switch` paralelos (código e tempo) viram **um** só, que devolve a forma completa.

As peças, uma por uma:

**`Estado.java`** é o *enum* de 3.2, com as três constantes. O guia de estilo do Google escreve as constantes uma por linha.

```java
// fig03_16/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

**`Respuesta.java`** é a interface selada de 3.3, tal qual.

```java
// fig03_16/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

**`Servicio.java`** é o *record* de 3.1: o nome, o endereço e o tempo limite, com o construtor adicional que completa o valor padrão. É a primeira vez que você vê um `import` de verdade: um arquivo normal não importa nada por si mesmo, então `URI` é trazido com uma linha.

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

**`Resultado.java`** junta o que o `revisor` sabe de um serviço já verificado: qual é, em que `Estado` ficou e qual foi sua `Respuesta`. Tem um método, `detalle()`, que usa um `switch` com padrões de record: se respondeu, o detalhe é o código como texto; se não, é o motivo. Sem `default`, e sem nulos.

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

**`Clasificador.java`** contém a classificação de 3.4.2. É uma classe `final` (não pode ser estendida) cujo único trabalho é agrupar uma função e a constante que ela usa. Os métodos e as constantes `static` pertencem à classe e não a um objeto, e são chamados com o nome da classe na frente: `Clasificador.clasificar(respuesta)`. (A Lição 6 explica `private`, a palavra que fecha essa porta para que ninguém crie um `Clasificador` que não serve para nada.) Vê-se que `clasificar` agora **devolve um `Estado`**, e não um `String`.

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

**`Formato.java`** reúne o que formata. Há uma mudança de assinatura importante em relação à lição anterior: `formatearTiempo` agora recebe um `long`, não um `Long`. Já não tem de decidir o que fazer com um nulo, porque o caso “sem tempo” não chega até ela. Quem decide é a função `tiempo(Respuesta)`, com um `switch` exaustivo: se há tempo o formata, e se não, escreve o traço. O tipo fez o trabalho que antes fazia a verificação `if (tiempoMs == null)`. O modelo das colunas, que antes estava escrito duas vezes (em `linea` e em `encabezado`), agora é a constante `PATRON_LINEA`. Usa-se `import java.util.Locale;` porque o arquivo não é compacto.

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

**`Revisor.java`** é o único arquivo compacto, e o ponto de entrada. `simular` é o **único `switch`** que substitui `codigoDe` e `tiempoDe`: devolve um `Respondio` com o código e o tempo juntos, ou um `SinRespuesta` para todo serviço que não conheça. `revisar` recebe um `Servicio`, simula a resposta, a classifica e devolve o `Resultado`. O `main` cria os quatro serviços com `URI.create(...)` e os imprime. (Os quatro escritos um por um, porque ainda não há listas; a Lição 4 resolve isso.)

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

Para executá-lo:

```text
$ cd fig03_16
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

É **caractere por caractere a mesma tabela** de quando terminou a Lição 2, com uma diferença que não se vê e que importa: agora, para que `reportes` saia como saiu, ninguém escreveu um `null`. O tipo `SinRespuesta` é o que faz com que ele não tenha tempo, que seu detalhe seja o motivo e que seu estado seja `CAIDO`.

O bom do design aparece quando você o muda. O exercício 4 desta lição é isso.

## O erro que você vai ver

A lição tem uma mensagem principal e quatro de apoio.

**O erro principal: “the switch expression does not cover all possible input values”.** É o que você vê quando falta um caso a um `switch` sobre um `enum` ou uma interface selada, e é **o mesmo texto nos dois casos**. Aparece no `fig03_07.java` (o *enum*) e no `fig03_10.java` (a interface selada):

```text
fig03_10.java:9: error: the switch expression does not cover all possible input values
  return switch (respuesta) {
         ^
```

É o sinal de que o compilador **está ajudando você**: você tirou um caso, ou acrescentou uma forma, e há um lugar que deve revisar. Não o silencie com um `default`. Leia a linha que o `^` aponta e decida, para essa forma, o que você quer que aconteça.

**As quatro mensagens de apoio:**

- `error: cannot assign a value to final variable tiempoMs`: você tentou mudar um componente de um *record*. Não se pode. Se precisa de outro valor, crie outro *record* (`fig03_02.java`).
- `error: class is not allowed to extend sealed class: Respuesta (as it is not listed in its 'permits' clause)`: você quis implementar uma interface selada com um tipo que não está na lista. Acrescente-o ao `permits` (`fig03_09.java`).
- `error: this case label is dominated by a preceding case label`: um caso geral está antes de um específico. Reordene do mais específico ao mais geral (`fig03_13.java`).
- `IllegalArgumentException` com a sua mensagem, vinda de um construtor compacto: alguém tentou criar um *record* com dados que o seu construtor rejeita. O stack trace diz quem tentou (`fig03_04.java`).

## O que se faz errado

**Um `default` em um `switch` sobre um tipo selado ou um *enum*.** Silencia a verificação que justifica tê-los escrito, como mostrou o `fig03_11.java`: o programa compilou e classificou mal um redirecionamento. Se a forma nova merece o mesmo tratamento que outra, escreva seu caso à parte (ou junte os dois com uma vírgula).

**Um `record` com um array, ou com qualquer objeto que possa ser modificado.** Parece imutável e não é: muda-se pelo acessor e pela referência original, e seu `equals` compara arrays por identidade. Na Lição 4 você verá `List.copyOf`.

**Um `String` onde cabe um `enum`.** Um estado, um tipo, um nível. Todo valor que sai de uma lista fixa deve ser um *enum*, para que um erro de digitação não seja um estado novo.

**Um `null` onde cabe uma forma.** Dois `null` paralelos (código, tempo) permitem estados impossíveis. Se “não há dado” é uma possibilidade com significado próprio, dê a ela sua própria forma em um tipo selado, com os dados que de fato tem (aqui, o motivo).

**Usar `ordinal()` para algo que não seja depuração.** O número de uma constante muda se alguém reordena o *enum*.

**Comparar `enum`s com `equals`.** Funciona, mas `==` é o idiomático, e evita uma `NullPointerException` se a variável é nula (`estado.equals(...)` estoura com `null`, `estado == ...` não).

**Um caso geral antes de um específico.** O compilador detecta, mas é um erro que convém entender e não só corrigir: a ordem dos casos faz parte do significado.

**Pôr validações longas no construtor compacto.** É o lugar de normalizar e de rejeitar o claramente absurdo. A lógica de negócio vai em outra parte.

## Exercícios

### Exercício 1 — Que igualdade um `record` lhe dá

Declare `record Servicio(String nombre, URI url, int timeoutMs) {}` em um arquivo compacto e crie três objetos: `a` e `b` com os mesmos valores (5000 de tempo limite) e `c` igual a eles mas com 1000. **Antes de executar**, preveja o resultado de `a == b`, `a.equals(b)`, `a.equals(c)`, `a.hashCode() == b.hashCode()` e de imprimir `c`. Execute e compare.

### Exercício 2 — Fechar uma porta da imutabilidade

Pegue o `fig03_05.java` e acrescente à `Ruta` um construtor compacto que guarde uma **cópia** do array (os arrays têm um método `clone()` que devolve uma cópia). Execute de novo. Qual das duas mudanças deixou de afetar o *record*? Qual continua acontecendo? O que falta para fechar a outra porta?

### Exercício 3 — Um estado novo no `enum`

No `enum` do `fig03_06.java`, acrescente um quarto valor, `DEGRADADO`, entre `LENTO` e `CAIDO`, e não toque em mais nada. Tente executar. O que diz o compilador e em qual função? Depois corrija com a mensagem que lhe pareça razoável para o estado novo, e escreva no seu diário de bordo o que teria acontecido se `Estado` fosse um `String`.

### Exercício 4 — O projeto: mais uma forma de resposta

No projeto de 3.5, acrescente uma terceira forma a `Respuesta`: `record Redirigida(int codigo, String destino)`, que representa um serviço que respondeu com um código 3xx e diz para onde mandar. Faça isso em `Respuesta.java` (a forma e o `permits`) e **não toque em mais nada**. Rode `java Revisor.java`. Anote **cada lugar** que o compilador obriga você a corrigir, um por um: corrija-os e rode de novo até compilar. Decida o que você quer que aconteça com um redirecionamento: que estado corresponde a ele, que tempo mostra, que detalhe?

## Soluções

### Solução 1

```text
false
true
false
true
Servicio[nombre=pagos, url=https://pagos.example, timeoutMs=1000]
```

`a == b` é `false`: são dois objetos diferentes. `a.equals(b)` é `true`: o `equals` do *record* compara componente por componente (e o `equals` de `URI`, por sua vez, compara o conteúdo). `a.equals(c)` é `false`: diferem em `timeoutMs`. Os `hashCode` de `a` e `b` coincidem porque `equals` os declara iguais, e o *record* garante essa coerência. E `c` é impresso com o formato de sempre.

### Solução 2

O construtor compacto que guarda a cópia:

```java
record Ruta(String nombre, String[] etapas) {
  Ruta {
    etapas = etapas.clone();
  }
}
```

Com ele, a saída é:

```text
tras cambiar el original: [conectar, esperar]
tras cambiar por el accesor: [conectar, CAMBIADO DESDE EL ACCESOR]
```

O primeiro buraco se fechou: mudar o array original já não afeta o *record*, porque este guarda sua própria cópia. O segundo continua aberto: o acessor devolve a referência à cópia que guarda, e mudá-la de fora a modifica. Para fechá-lo seria preciso sobrescrever o acessor para que devolva outra cópia; em um *record*, um acessor escrito à mão tem de ser `public` (a Lição 6 explica o que isso significa), e mesmo assim `equals` continuaria comparando arrays por identidade. A conclusão prática é que **um array é um mau componente**. Na lição seguinte você verá `List.copyOf`, que fecha, sim, as duas portas.

### Solução 3

O compilador rejeita o programa em `aviso`, a função com o `switch`:

```text
error: the switch expression does not cover all possible input values
  return switch (estado) {
         ^
```

Falta o caso `DEGRADADO`. Corrige-se acrescentando `case DEGRADADO -> "revisar ya";` (ou a mensagem que você escolher). Com um `String`, nada teria avisado: `"DEGRADADO"` seria um texto a mais, `aviso` cairia no `default` (que teria sido obrigatório) e o serviço degradado receberia o aviso dos desconhecidos. O *enum* converteu esse descuido em um erro de compilação.

### Solução 4

Com a forma nova acrescentada só em `Respuesta.java`:

```java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}
```

o compilador reclama primeiro do `Clasificador.java` (o caminho completo da sua pasta aparece na frente do nome do arquivo; aqui o encurto):

```text
Clasificador.java:6: error: the switch expression does not cover all possible input values
    return switch (respuesta) {
           ^
```

Acrescenta-se um caso (por exemplo, `case Respuesta.Redirigida _ -> Estado.ARRIBA;`: um redirecionamento é uma resposta válida), e ao rodar de novo, o seguinte é `Formato.java:15` (a função `tiempo`), e depois `Resultado.java:4` (o método `detalle`). **São três lugares**: a classificação, o tempo mostrado e o detalhe. Na minha execução o compilador os mostrou um de cada vez, nessa ordem. Você pode decidir diferente o que cada um faz: um estado `ARRIBA` é razoável porque o serviço respondeu, o tempo pode ser o traço (não temos um na forma) e o detalhe pode ser o código com o destino. O importante não é a decisão, mas que o compilador **obrigou você a tomá-la** em cada lugar. Com dois `null` paralelos, nada teria avisado.

## Como sei que consegui

- [ ] `java fig03_01.java` imprime `Punto[nombre=catalogo, tiempoMs=142]` e você consegue explicar por que `a == b` é `false` e `a.equals(b)` é `true`.
- [ ] `java fig03_05.java` mostra as duas modificações à `Ruta`, e você consegue dizer por que ocorrem.
- [ ] No `fig03_10.java` você vê o erro “does not cover all possible input values”, e sabe qual caso faltou.
- [ ] No `fig03_11.java` você vê que o `default` classifica um redirecionamento como `CAIDO`, e consegue dizer por que isso é pior que o erro anterior.
- [ ] A partir de `fig03_16/`, `java Revisor.java` imprime exatamente a tabela de quatro linhas da Lição 2: `catalogo` ARRIBA 142ms, `inventario` LENTO 2.3s, `pagos` ARRIBA 87ms, `reportes` CAIDO — “sin conexión” (“sem conexão”).
- [ ] Depois de acrescentar `Redirigida`, o compilador mostrou três lugares e você os corrigiu um por um.
- [ ] Você consegue explicar com suas palavras por que o `revisor` desta lição não tem um único `null`.
- [ ] Você anotou no seu diário de bordo o estado impossível que o tipo selado impediu você de escrever, e o que aconteceu quando acrescentou um caso novo.

## Resumo

Responda sem olhar a lição:

1. Que quatro coisas o compilador gera para um `record`? O que seu `equals` compara?
2. Que diferença há entre um construtor compacto e um adicional, e para que serve cada um?
3. Por que um `record` com um array dentro não é imutável? O que se faz na Lição 4 para corrigir isso?
4. Por que se compara um `enum` com `==`? Que vantagem dá escrever um `switch` sobre um `enum` sem `default`?
5. O que diz `sealed ... permits` e o que acontece se você tenta implementar a interface com um tipo que não está na lista?
6. O que é uma guarda `when`, um padrão de record e o padrão `_`? Dê um exemplo de cada um com o `revisor`.
7. Por que a ordem dos casos importa? O que acontece se você põe o caso geral antes?
8. O que faz um `switch` sobre uma referência nula se não tem `case null`?
9. O que mudou na assinatura de `formatearTiempo` entre a Lição 2 e a 3, e o que tornou isso possível?
10. Por que um `default` em um `switch` sobre um tipo selado é um risco?

## Para ler mais

- [Os tutoriais do Java, “Record Classes”](https://docs.oracle.com/en/java/javase/25/language/records.html): os *records* explicados com mais exemplos, da documentação oficial (em inglês). Consultado em 7 de outubro de 2026.
- [JEP 409, “Sealed Classes”](https://openjdk.org/jeps/409): o porquê e as regras completas de `sealed`, `permits`, `final` e `non-sealed`. Consultado em 7 de outubro de 2026.
- [JEP 441, “Pattern Matching for switch”](https://openjdk.org/jeps/441) e [JEP 440, “Record Patterns”](https://openjdk.org/jeps/440): as regras exatas da exaustividade, do domínio e das guardas. Consultado em 7 de outubro de 2026.
- [Os tutoriais do Java, “Enum Types”](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html): os *enums* com campos, construtores e métodos. Consultado em 7 de outubro de 2026.

---

**Anterior:** [Lição 2 — Tipos, strings e controle de fluxo](02-tipos-cadenas-control.md) ·
**Próxima:** [Lição 4 — Coleções, genéricos e streams](04-colecciones-genericos-streams.md)
