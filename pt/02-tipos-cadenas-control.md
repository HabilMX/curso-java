# Lição 2 — Tipos, strings e controle de fluxo

**Tempo:** 2 × 90 min (duas sessões; o corte fica entre as seções 2.2 e 2.3)

**O que você constrói:** as funções base do `revisor`: decidir se um serviço está no ar, lento ou fora do ar, formatar o tempo e montar as linhas da tabela, tudo em um único arquivo compacto

**O que você aprende:** tipos primitivos e de referência, wrappers e `var`; `String` é imutável e por que `==` não é `equals`; formatar com `String.format` e `Locale.ROOT`; blocos de texto; `if`, `&&` contra `&` e o operador `? :`; `switch` como expressão; métodos e constantes em um arquivo compacto; `null` e como ler a mensagem de uma `NullPointerException`

**De onde você vem.** Você traz o ambiente da [Lição 1](01-instalacion.md): um JDK 25 que responde a `java --version`, o hábito de rodar um arquivo com `java Archivo.java` e um `~/curso-java/revisor/Revisor.java` de uma única linha que imprime “revisor: todavía no revisa nada” (“revisor: ainda não verifica nada”). Você também sabe ler o primeiro erro do compilador: o arquivo, o número da linha, o `^` que aponta o ponto e as linhas `symbol` e `location`. Esta lição usa essa habilidade o tempo todo, porque o Java é uma linguagem que **dá bronca antes de executar**, e aprender a entender suas broncas é metade do ofício.

**O que esta lição não faz.** Não usa listas, nem laços, nem classes suas, nem consulta nenhuma rede. Os dados dos quatro serviços do `revisor` estão escritos em dois `switch` paralelos, de propósito: é uma solução incômoda, e a [Lição 3](03-records-enums-sellados.md) a substitui por tipos que expressam o que significa “um serviço respondeu” e “um serviço não respondeu”. As coleções chegam na Lição 4; as exceções, na 5. Aqui, quando um programa falha de propósito, usamos isso para **ler** o erro, não para capturá-lo.

**Sobre as execuções.** Cada figura é um arquivo `fig02_NN.java` que se executa com `java fig02_NN.java`, a partir da pasta onde você o guardou. Você as tem já escritas no repositório, em [`programas/02-tipos-cadenas-control/`](../programas/02-tipos-cadenas-control/), mas aprende mais se as digitar. Todas as saídas desta lição foram obtidas com o Temurin 25.0.4 e a variável `LANG` em `C.UTF-8`, que é o que faz o terminal imprimir bem os acentos e o traço “—”; com `LANG=C`, o Java imprime `?` no lugar. Se o seu patch do Java for outro, as mensagens de erro são as mesmas; a única coisa que muda entre atualizações são os números de linha das linhas `at java.base/...` de um stack trace, que são do JDK e não suas. As figuras que falham de propósito trazem seu erro ao lado, em um arquivo `fig02_NN.error-esperado.txt`.

## Ao terminar, você vai conseguir

- Escolher entre `int`, `long`, `double` e `boolean` para um dado, e explicar o que acontece com um `int` que passa do seu máximo.
- Distinguir um valor primitivo de uma referência e de um wrapper (`int` contra `Integer`), e dizer quando uma variável pode valer `null`.
- Usar `var` onde ajuda e saber que tipo o compilador deu a ela.
- Explicar por que `s.strip();` não muda `s`, e por que `==` entre dois `String` pode dar `false` ainda que pareçam iguais.
- Formatar um texto com `String.format` e `Locale.ROOT`, e escrever um bloco de texto de várias linhas.
- Escrever um `switch` como expressão com setas, e explicar por que `&&` e `&` não são a mesma coisa.
- Ler a mensagem de uma `NullPointerException`, saber que variável era nula e por que às vezes a mensagem diz `<local1>` em vez do seu nome.

## O porquê antes do como

Você chega ao Java com uma linguagem nas costas, e essa linguagem lhe deixou hábitos. Alguns servem aqui tal qual: um `if` é um `if`. Outros são armadilhas, porque o Java se parece o bastante com o que você já sabe para que você não desconfie. Pense em três expressões que, na linguagem que você traz, talvez signifiquem uma coisa, e em Java significam outra:

- Comparar dois textos com `==`. Em Python ou em Go compara o seu conteúdo. Em Java compara se são **o mesmo objeto**, e às vezes diz `true` por acaso.
- Chamar `texto.strip()` e achar que `texto` ficou limpo. Em Java o texto **nunca muda**: o método devolve outro, e se você não o guardar, ele se perde sem avisar.
- Escrever `x != null & x.length() > 0` achando que é o mesmo que com `&&`. Estoura com uma `NullPointerException` justamente quando `x` é nulo, ou seja, justamente quando a verificação servia.

Há um estudo com centenas de milhares de estudantes de Java (Brown e Altadmri, com os dados do BlueJ) que mediu quais erros demoram mais para ser corrigidos. Os professores que participaram não acertaram: sua intuição sobre quais eram os difíceis não coincidiu com os dados. Os que mais demoram são justamente estes três: confundir `==` com `equals`, confundir `&` com `&&` e não usar o valor que um método devolve. Por isso esta lição não os menciona de passagem: **cada um é provocado de propósito, visto falhar e explicado**. Você pode ler o artigo na [página da Universidade de Kent](https://kar.kent.ac.uk/57219/).

O caminho tem duas sessões. A **primeira** cobre 2.1 e 2.2: os tipos e as strings. Ao terminá-la você saberá o que cada variável guarda e o que um texto faz quando você o “modifica”. A **segunda** cobre 2.3 a 2.5: o controle de fluxo, o `null` e o passo do projeto. Ao terminá-la você terá o `revisor` imprimindo esta tabela, calculada por funções suas:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Essa tabela é o fio de todo o curso. Repare na última linha: `reportes` não respondeu, então não tem tempo nem código. Como representar “não há dado” em uma linguagem com tipos estritos é a pergunta que atravessa toda a lição, e sua má resposta —`null`— é a razão de metade dos erros que você vai ver.

## Os conceitos

### 2.1 Tipos

#### 2.1.1 Primitivos: valores soltos de tamanho fixo

Em Java toda variável tem um **tipo** que você escreve, antes do nome, e que **nunca muda**. `int codigo = 200;` declara uma variável chamada `codigo` que só pode guardar inteiros desse tamanho. Se mais adiante você tentar guardar um texto nela, o compilador se recusa. Você vai reconhecer isso do Go ou do Rust; se você vem do Python ou do JavaScript, é a maior mudança: os erros de tipo aparecem **antes** de executar, não durante.

Os tipos do Java se dividem em duas famílias. A primeira são os **primitivos**: oito tipos, incorporados à linguagem, que guardam o valor diretamente, sem objeto no meio. No `revisor` você só precisa de quatro, mas convém ver os oito:

| Tipo | O que guarda | Tamanho | Faixa ou exemplo |
|---|---|---|---|
| `byte` | inteiro muito pequeno | 8 bits | de −128 a 127 |
| `short` | inteiro pequeno | 16 bits | de −32 768 a 32 767 |
| `int` | inteiro (o de uso comum) | 32 bits | de −2 147 483 648 a 2 147 483 647 |
| `long` | inteiro grande | 64 bits | até cerca de 9.2 × 10¹⁸ |
| `float` | decimal de precisão simples | 32 bits | raramente é usado |
| `double` | decimal de precisão dupla | 64 bits | o decimal de uso comum |
| `boolean` | verdadeiro ou falso | não é fixado | `true` ou `false` |
| `char` | uma unidade de texto UTF-16 | 16 bits | `'c'`, entre aspas simples |

As faixas e os tamanhos não são decididos pelo seu computador nem pelo seu sistema operacional: são fixados pela [linguagem](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2), e por isso um `int` mede o mesmo em toda parte. Os nomes se escrevem com minúscula; isso avisa que não são classes.

Veja o programa `fig02_01.java`. Ele declara um dado de cada um dos quatro tipos que o `revisor` usará (mais um `char`, para completar a visão) e faz um pouco de aritmética. Uma nota sobre a sintaxe: o `+` entre um texto e qualquer outra coisa **converte essa coisa em texto e a cola**, que é como se monta cada linha de saída. E `IO.println` é a que você viu na Lição 1; sua irmã `IO.print` faz o mesmo mas sem pular de linha no final.

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

Há três coisas nessa saída que merecem que você pare.

**A divisão de inteiros descarta a parte decimal.** `7 / 2` dá `3`, não `3.5`, porque os dois operandos são `int` e o resultado é um `int`. Para obter `3.5` basta que um dos dois seja `double`: `7 / 2.0`. O mesmo acontece com `2300 / 1000`, que dá `2`, não `2.3`. Isso surpreende quem vem do Python 3 (onde `/` sempre dá decimal) ou do JavaScript (onde tudo é decimal), e não quem vem do Go ou do Rust. É o erro silencioso mais comum em contas de tempo: se você calcula segundos como `tiempoMs / 1000` obtém segundos **inteiros**, e 999 ms são 0 s.

**A variável `tiempoMs` é um `long` e seu valor termina em `L`.** `2300L` é um literal de tipo `long`; sem o `L`, `2300` seria um `int`. Para números pequenos a diferença não se nota, porque o Java converte um `int` em `long` sozinho (é uma conversão que não perde nada). Com números grandes o `L` é obrigatório, como você verá daqui a pouco.

**`0.1 + 0.2` dá `0.30000000000000004`.** Não é um defeito do Java: é a forma como qualquer computador guarda decimais binários (o padrão IEEE 754), e acontece igual no Python, JavaScript, Go e Rust. O importante é a consequência: **nunca compare decimais com `==`**, e nunca guarde dinheiro em um `double`. O `revisor` guarda seus tempos em milissegundos **inteiros** por essa razão: é um número que não precisa de decimais.

Agora, a armadilha que é de fato própria do Java e das linguagens que compartilham seu modelo de inteiros de tamanho fixo. O que acontece quando um número passa do seu máximo? Veja o `fig02_02.java`:

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

Um `int` que chega ao seu máximo e recebe mais um **dá a volta** e se converte no mínimo, sem nenhum aviso: não há erro, não há exceção, apenas um número negativo onde você esperava um enorme. No programa, trinta dias em milissegundos não cabem em um `int` (o resultado correto é 2 592 000 000, que é mais que o máximo de 2 147 483 647), e o valor que sai é `-1702967296`. A correção é fazer a conta em `long`: basta escrever um `L` em **um** dos números, `30L * 24 * 60 * 60 * 1000`, e todo o cálculo é feito em `long`. Repare na diferença entre as duas últimas linhas: é exatamente uma letra.

Um detalhe de leitura dessa figura: `Integer.MAX_VALUE` e `Long.MAX_VALUE` são constantes que as classes `Integer` e `Long` trazem, das quais falamos logo a seguir. E a primeira linha do `main` de cada figura desta lição é a que você já conhece: `void main()`, o arquivo compacto da Lição 1.

#### 2.1.2 Referências, wrappers e `null`

A segunda família de tipos são os de **referência**: tudo o que não é um dos oito primitivos. `String` é um tipo de referência. Também são `Integer`, `Long`, `URI` ou qualquer classe que você escrever. A diferença para um primitivo é profunda, e se entende com uma imagem. Uma variável primitiva é **uma caixa com o valor dentro**: `int codigo = 200` é uma caixa que diz 200. Uma variável de referência é **uma caixa com um endereço dentro**: o endereço de onde está o objeto. Duas variáveis de referência podem ter o mesmo endereço (e então são o mesmo objeto), ou endereços diferentes que apontam para objetos que parecem iguais. Essa distinção é a que será preciso entender em 2.2.2.

E uma variável de referência pode ter, em vez de um endereço, **nenhum**. Esse “nenhum” se escreve `null`. Os primitivos não podem ser `null`: um `int` sempre vale algum número. Essa diferença importa para o `revisor`, porque `reportes` não respondeu e seu código HTTP não é “zero” nem “menos um”: **não existe**. Com um `int` não há maneira honesta de dizer isso. Com uma referência há.

Por isso o Java traz um **wrapper** (classe envoltória) para cada primitivo: `Integer` para `int`, `Long` para `long`, `Double` para `double`, `Boolean` para `boolean`, `Character` para `char`. Um wrapper é um objeto cujo único trabalho é conter um valor primitivo, e como é de referência, **pode ser `null`**. O Java converte entre o primitivo e seu wrapper de forma automática, nas duas direções; chama-se *autoboxing* colocar o valor na caixa (de `int` para `Integer`) e *unboxing* tirá-lo. Veja o `fig02_03.java`, que junta o que acabamos de dizer:

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

Três linhas merecem comentário. `Integer sinRespuesta = null;` é uma declaração perfeitamente válida, e ao imprimi-la sai `null`. `codigoCaja.equals(codigo)` chama o método `equals` do wrapper com um `int`, e o Java o coloca em uma caixa por você para compará-lo: dá `true`. E as duas comparações com `==` são a **surpresa desta seção**: `127 == 127` dá `true` e `2300 == 2300` dá `false`, ainda que nas duas linhas se comparem dois wrappers com o mesmo valor. Isso é porque `==` entre duas referências pergunta “são o mesmo objeto?”, e não “valem o mesmo?”. O Java reutiliza os objetos `Integer` de −128 a 127 (é uma garantia da [especificação](https://docs.oracle.com/javase/specs/jls/se25/html/jls-5.html#jls-5.1.7)), então `127` e `127` são o mesmo objeto e `2300` e `2300` são dois objetos diferentes. Com `==` entre wrappers o seu programa funciona com os códigos HTTP pequenos e falha com os tempos grandes, que é o pior tipo de erro: o que depende do dado.

A regra prática é curta. **Entre primitivos, `==` compara valores. Entre referências, compara identidade, e para comparar valores se usa `equals`.** E um aviso próprio dos wrappers: `Long.equals` compara também o tipo. Um `Long` com 142 nunca é igual a um `Integer` com 142; `equals` devolve `false` se você passar o outro tipo. Quando comparar um `Long` com um literal, escreva o `L`.

No `revisor` você vai usar wrappers exatamente para o que servem: `Integer codigoDe(String nombre)` devolve o código HTTP do serviço, ou `null` se o serviço não respondeu; `Long tiempoDe(String nombre)` faz o mesmo com o tempo. A Lição 3 substituirá esse truque por algo melhor, mas para entender por que é melhor é preciso tê-lo sofrido.

#### 2.1.3 Os tipos são verificados antes de executar

Como cada variável tem seu tipo fixo, o compilador verifica que o que você atribui **caiba**. Ele vai dar bronca com uma mensagem que convém reconhecer, porque você a verá muito. Veja o `fig02_04.java`, onde um `long` tenta entrar em um `int`:

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

Leia com o que você aprendeu na Lição 1. Primeiro o arquivo e a linha (`fig02_04.java:4`), depois a classe de erro (`incompatible types`), depois o detalhe (“uma conversão que poderia perder informação, de `long` para `int`”), e o `^` que aponta o lugar. O lançador fecha com `error: compilation failed`.

A mensagem diz algo importante com a palavra **possível**. O Java não sabe se o valor cabe: vê o tipo, não o conteúdo. Um `long` *poderia* valer 3 000 000 000, e um `int` não suporta isso, então o compilador obriga você a decidir. Você tem três saídas, e a correta depende do caso. Mudar o tipo de destino para `long` é quase sempre o melhor. Escrever um **cast** (conversão explícita) como `(int) tiempoMs` diz ao compilador “eu sei o que faço”, e é perigoso: não há proteção, e `(int) 3_000_000_000L` dá `-1294967296`, o mesmo estouro silencioso de 2.1.1. A terceira é não misturar os tipos desde o princípio. O cast é usado, com muito cuidado, quando você realmente quer truncar, por exemplo `(int) 2.9` dá `2`.

No sentido contrário não há queixa: um `int` entra em um `long` e um `long` em um `double`, porque ali nunca se perde a ordem de grandeza. Por isso `double segundos = tiempoMs / 1000.0` do `fig02_01.java` compilou sem cerimônias.

#### 2.1.4 `var`: deixar o compilador escrever o tipo

Escrever o tipo em cada variável dá segurança, mas é repetitivo quando o valor do lado direito já diz tudo. Para isso existe o `var`. Veja o `fig02_05.java`:

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

Cada `var` é uma variável de tipo fixo, exatamente como se você o tivesse escrito: `nombre` é um `String`, `codigo` é um `int`, `tiempoMs` é um `long` (pelo `L`) e `segundos` é um `double`. **O `var` não torna o Java dinâmico**: o compilador deduz o tipo **uma única vez**, a partir do valor com que você inicializa, e daí em diante a variável não pode mudá-lo. Se você tentar depois `nombre = 5;`, o compilador reclama como antes. A [proposta que o introduziu](https://openjdk.org/jeps/286) (JEP 286, Java 10) o limita a variáveis locais com inicializador: não vale em campos, nem em parâmetros, nem em tipos de retorno.

Sem valor do qual deduzir, não há tipo. Veja o que acontece com o `fig02_06.java`:

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

`null` por si só não diz de que tipo é, e o compilador se recusa a adivinhar. É o mesmo problema dos wrappers, visto de outro lado: um valor “sem nada” precisa de um tipo que lhe dê sentido.

**Quando usar `var`.** Neste curso o usamos quando o tipo salta aos olhos na mesma linha (`var nombre = "catalogo";`) e o evitamos quando esconde informação (`var resultado = revisar(servicio);` não diz o que é `resultado` sem ir ler `revisar`). É um critério de leitura, não uma regra da linguagem: o código é lido muito mais vezes do que é escrito.

### 2.2 Strings

#### 2.2.1 `String` é um objeto, e é imutável

`String` é uma classe da biblioteca padrão, então cada texto é um objeto com métodos: escrevem-se depois de um ponto, como no Python ou ao JavaScript. Os mais usados são `length()` (quantos caracteres), `strip()` (tira os espaços das pontas), `isBlank()` (está vazio ou só tem espaços?), `toUpperCase()`, `contains(otro)`, `startsWith(prefijo)` e `substring(desde, hasta)` (um pedaço; o segundo número não é incluído). Veja como são usados no `fig02_07.java`, e repare sobretudo nas duas primeiras linhas de saída:

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

**A primeira linha de saída é o erro.** Escrevemos `linea.strip();` e o texto continuou com seus espaços: `[  catalogo  ]`. Não houve aviso nem erro. É o **valor de retorno ignorado**, um dos três erros que mais demoram para ser corrigidos segundo o estudo que mencionei. A causa é que os `String` do Java são **imutáveis**: uma vez criado, um texto não muda. A [documentação](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html) diz assim: os textos são constantes e seus valores não mudam depois de criados. Todos os métodos que parecem “modificar” (`strip`, `toUpperCase`, `substring`...) na verdade **devolvem um texto novo** e deixam o original intacto. Se você não o guarda, você o jogou fora.

A linha correta é a de baixo: `linea = linea.strip();`, que reatribui a **variável** para que aponte para o texto novo. Repare no que mudou: não o objeto, mas para quem a variável aponta. É o modelo das caixas com endereços de 2.1.2.

Por que foi projetado assim? Porque um texto imutável pode ser compartilhado sem medo: se cem partes do programa guardam o mesmo `String`, nenhuma pode estragá-lo para as demais. É uma propriedade que você já conhece do Python (`str`) e do JavaScript (os textos primitivos), e que voltará a ver em Java com os *records* da Lição 3. O custo é que o programador deve lembrar a regra.

Uma pergunta natural: se cada “modificação” cria um texto novo, não é carríssimo montar um texto longo colando pedaços? Em um laço com milhares de voltas é, e para isso há uma classe à parte, `StringBuilder`. Você não precisa dela neste curso: o `revisor` monta linhas curtas, e `String.format` (2.2.3) já faz o trabalho.

#### 2.2.2 `==` contra `equals`

Este é o erro mais importante da lição. Veja o `fig02_08.java`, que monta o texto `inventario` de duas maneiras:

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

As três primeiras linhas contam uma história que se lê melhor em ordem.

`armado == literal` dá **`false`**, ainda que os dois textos contenham `inventario`. `armado` foi construído **em tempo de execução**, colando o valor da variável `prefijo` com `"tario"`, então é um objeto novo em um endereço novo. `literal` é um objeto que o compilador criou uma vez, e `==` pergunta se ambas as variáveis apontam para o mesmo. Não apontam. `armado.equals(literal)` dá `true`, porque `equals` compara o **conteúdo**, caractere por caractere.

A terceira linha é a armadilha dentro da armadilha. `constante == literal` dá **`true`**, ainda que `constante` também tenha sido escrito com um `+`. A razão: `"inven" + "tario"` junta dois literais, e o compilador sabe o resultado antes de executar, então ele mesmo o calcula e o trata como o literal `"inventario"` (a [especificação](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.29) o chama de “expressão constante”). Os textos literais com o mesmo conteúdo são **o mesmo objeto** em todo o programa. Por isso um `==` entre textos costuma funcionar em um teste pequeno de brinquedo e falha assim que o texto chega de **fora**: de um arquivo, de um teclado, de uma rede, de um `+` com uma variável. É o tipo de erro que passa em todos os testes que você escreve e falha em produção.

As duas últimas linhas mostram o que se escreve no lugar. `equals` distingue maiúsculas, e `equalsIgnoreCase` não. E um costume que vale ouro: **ponha o literal primeiro**. `"INVENTARIO".equals(literal)` não pode lançar uma `NullPointerException`, porque o literal nunca é nulo; já `variable.equals("inventario")` estoura se `variable` for `null`. Com o literal à esquerda, se a variável for nula a resposta é simplesmente `false`.

**A regra desta lição, a mais importante:** entre referências (textos, wrappers, qualquer objeto), `==` pergunta se é o mesmo objeto; `equals` pergunta se valem o mesmo. Quase sempre você quer o segundo. Use `==` entre primitivos, e entre `enum`s (Lição 3), onde aí sim é o correto.

Por que o Java não faz `==` comparar o conteúdo nos textos, como o Python? Porque o Java não deixa o programador mudar o significado de um operador, e `==` significa o mesmo em todos os tipos de referência. É uma decisão de projeto que há décadas dá trabalho, e que não vai mudar.

#### 2.2.3 Formatar: `String.format` e `Locale.ROOT`

Cada linha da tabela do `revisor` tem colunas alinhadas: o nome ocupa 12 espaços, o estado 10, e assim por diante. Montar isso com `+` e espaços à mão é um pesadelo, e para isso o Java traz `String.format`: recebe um **modelo** com marcas e os valores que as preenchem. As marcas de que você precisa hoje:

- `%s` põe um texto (ou qualquer valor, convertido em texto).
- `%d` põe um inteiro.
- `%.1f` põe um decimal com uma casa depois do ponto.
- `%-12s` põe um texto **ocupando 12 espaços, alinhado à esquerda** (sem o hífen, à direita). `%6s` ocupa 6 e alinha à direita.

E há um problema que você não verá até que seu programa rode no computador de outra pessoa: os decimais se escrevem diferente conforme o idioma. No México o separador decimal é o ponto (`2.3`), na Espanha é a vírgula (`2,3`). Se você pede ao `String.format` que formate um decimal sem dizer o idioma, **ele usa o da máquina onde roda**. Seu programa sairia com pontos no seu laptop e com vírgulas no de um colega. Para que isso não aconteça, passa-se como primeiro argumento um `Locale`, o objeto que representa um idioma e uma região. Veja o `fig02_09.java`:

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

`Locale.of("es", "MX")` constrói o idioma espanhol do México e `Locale.of("es", "ES")` o da Espanha. Com o da Espanha, o mesmo `%.1f` dá **`2,3s`**. `Locale.ROOT` é o “idioma neutro”, que usa o formato mais estável (ponto decimal, sem separadores de milhar), e **é o que se usa no curso sempre que se formata um número com decimais**. A razão não é nacionalista: é que a tabela do `revisor` deve sair idêntica em qualquer máquina, e um programa cuja saída depende da configuração regional é um programa que um teste não consegue verificar.

A quarta linha de saída é o modelo das linhas da tabela em miniatura (`%-12s` para o nome, `%6s` para o tempo e `%d` para o código), com os colchetes para que você veja quanto espaço cada coluna ocupa. A última usa `formatted`, uma versão do mesmo mecanismo que se chama sobre o próprio texto: `"...".formatted(valores)` equivale a `String.format("...", valores)` (sem o `Locale`: use-a só com textos e inteiros).

#### 2.2.4 Blocos de texto

Às vezes você precisa de um texto de várias linhas: um modelo, um trecho de configuração, um JSON de exemplo. Escrevê-lo com `"` e `\n` em cada linha é horrível. O Java tem os **blocos de texto**, delimitados por três aspas duplas. Foram introduzidos como recurso definitivo no Java 15 ([JEP 378](https://openjdk.org/jeps/378)). Veja o `fig02_10.java`:

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

A regra que o torna útil é como ele trata a indentação. O texto do bloco começa na linha de baixo das três aspas de abertura, e o Java **tira a indentação que todas as linhas compartilham** (o compilador a calcula com a linha menos indentada, contando também a linha do fechamento `"""`). Por isso o texto impresso começa em `# nombre` sem os seis espaços com que escrevemos o bloco no código. Se você move o fechamento `"""` para a esquerda, conserva mais indentação; se o deixa no final da última linha de texto, não há quebra de linha no final.

Mais dois detalhes. Dentro de um bloco você pode escrever aspas duplas sem escapá-las (`"%s"` no segundo bloco). E uma barra invertida `\` no final da linha **não insere a quebra**: junta a linha com a seguinte. No segundo bloco, o `\` faz com que a frase saia de uma só vez, com um espaço que vem da indentação extra da linha seguinte (sete espaços, um a mais que os seis comuns). E repare no uso de `.formatted(...)` logo depois do fechamento: um bloco de texto é um `String` como qualquer outro. A linha com `endsWith("\n")` confere que o primeiro bloco termina com uma quebra de linha, que em um texto normal se escreve `\n`; por isso a saída do `IO.print` fica com as três linhas seguidas e o `IO.println` seguinte começa em uma linha limpa.

Com isso termina a primeira sessão. Antes de seguir, confira se você consegue responder, sem olhar: qual é a diferença entre `int` e `Integer`? O que `armado == literal` imprime no `fig02_08.java` e por quê? O que acontece se você escrever `linea.strip();` e nada mais? Se alguma das três for difícil, releia a seção correspondente antes da segunda sessão.


### 2.3 Controle de fluxo

#### 2.3.1 `if`, condições e o operador `? :`

O `if` do Java é o que você já conhece, com duas particularidades. A primeira: **a condição tem de ser um `boolean`**, e nada mais. Em Python ou em JavaScript, um número, um texto ou uma lista podem servir de condição (chamam-se “verdadeiros” ou “falsos” conforme o conteúdo); em Java, `if (codigo)` com um `int` nem sequer compila:

```text
Prueba.java:3: error: incompatible types: int cannot be converted to boolean
  if (codigo) {
      ^
```

É uma vantagem disfarçada de incômodo: obriga você a escrever o que de fato quer perguntar (`codigo >= 400`, `texto != null`, `!texto.isBlank()`), e acabam-se os erros de “o zero também conta como falso”. A segunda particularidade é de estilo: **as chaves sempre**, ainda que o `if` tenha uma única linha. É o que pede o guia de estilo do Google que todo o código do curso segue (dois espaços de indentação, linhas de até 100 colunas), e o que o Checkstyle verificará na Lição 10; se você as escrever desde hoje, não precisará corrigi-las depois. Um `if` sem chaves é a receita de um erro clássico: alguém acrescenta uma segunda linha embaixo, com a indentação correta, e a segunda linha não faz parte do `if`.

Veja o `fig02_11.java`. Ele junta um `if` com `else if`, uma variável booleana, e o **operador ternário**, que é um `if` em forma de expressão: `condicion ? valorSiVerdadero : valorSiFalso`.

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

A condição `codigo >= 400` é a primeira regra do `revisor`: um serviço que responde com um código de erro (400 ou mais) está fora do ar, ainda que tenha respondido. Repare que o `else if` só é avaliado se o primeiro foi falso, e que os ramos são excludentes. O ternário é usado para escolher um valor, não para executar ações; quando a expressão de cada lado fica longa, um `if` normal se lê melhor.

#### 2.3.2 `&&` contra `&`

Aqui vem o segundo erro da lista dos lentos de corrigir. O Java tem dois operadores para “e”: `&&` e `&` (e dois para “ou”: `||` e `|`). Os dois fazem o mesmo com booleanos, com uma diferença que importa: `&&` **para assim que conhece a resposta**. Se o lado esquerdo é falso, o resultado é falso independentemente do direito, e o direito **nem é avaliado**. Chama-se avaliação de curto-circuito. `&` avalia **sempre os dois lados**. (Com números inteiros, `&` é outra coisa: uma operação sobre os bits. Neste curso não a usamos, mas isso explica por que ele existe.)

Veja o programa `fig02_12.java`. Ele quer perguntar: “o detalhe existe e além disso está em branco?”:

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

`detalle` vale `null`, então `detalle != null` é falso... mas o `&` avaliou **de qualquer jeito** o lado direito, `detalle.isBlank()`, e chamar um método sobre `null` estoura. É a verificação que você colocou para evitar o problema, causando o problema. Com `&&` o lado direito não é tocado quando o esquerdo falha. É o `fig02_13.java`:

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

Esse padrão, **verificar que não seja nulo e, somente se não for, usá-lo**, é a razão de ser do `&&` no `revisor`. Você o usará em `clasificar`, e vai escrevê-lo centenas de vezes na sua vida com Java. A regra é: em uma condição, `&&` e `||`; o `&` e o `|` ficam para operações sobre bits. É uma dessas faltas que o compilador não marca, porque ambos são válidos; quem as pega é a sua atenção.

#### 2.3.3 `switch` como expressão

O `switch` clássico do Java, herdado do C, tem um defeito célebre: **se você esquece o `break`, a execução segue no caso de baixo**. Veja o `fig02_14.java`, que escreve o mesmo seletor de duas maneiras:

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

Com a sintaxe velha (os dois-pontos), `inventario` entra no caso 2, **não encontra `break`**, cai no caso 3 e termina deixando `antiguo` em 3. Com a nova (a seta `->`), cada caso é independente: não há `break` nem queda. É preciso reconhecer a sintaxe velha porque está em quase todo o código Java que você encontrará por aí, mas **neste curso se escreve sempre com seta**. Os `switch` com seta entraram no Java na versão 14 ([JEP 361](https://openjdk.org/jeps/361)).

A segunda diferença é a que muda como você pensa: um `switch` com setas é uma **expressão**, ou seja, **produz um valor**. Você pode atribuí-lo (`int moderno = switch ... ;`), devolvê-lo com `return switch ...` ou passá-lo a um método. Três regras o caracterizam. Um caso pode ter **vários valores** separados por vírgulas (`case "catalogo", "inventario", "pagos" -> 200;`). O resultado de cada caso é o que está à direita da seta. E se você precisa de mais de uma instrução, o caso leva um bloco entre chaves e entrega seu valor com `yield`. Veja o `fig02_15.java`:

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

Há três coisas novas nessa figura. `codigo / 100` aproveita a divisão inteira de 2.1.1 para obter a “família” do código (503 fica em 5). `case 4, 5 -> { ... yield ...; }` é o bloco com `yield`, que é para um caso o que `return` é para um método. E o `default` pega o resto. Uma limitação: o `switch` com valores compara por **igualdade** com constantes; não serve para faixas como “400 ou mais”. Para isso se usa um `if`, ou, a partir da Lição 3, um `switch` com padrões e condições.

A regra que torna valioso o `switch` como expressão é que **ele tem de cobrir todos os casos possíveis**, porque tem de produzir um valor seja qual for a entrada. Se falta algo, o compilador diz. O `fig02_16.java` esquece o `default`:

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

O compilador sabe que há infinitos textos diferentes, então sem `default` não pode garantir que haja um valor. Mais adiante, na Lição 3, você verá que com `enum` e com tipos selados o compilador pode verificar que **não falta nenhum caso sem precisar de `default`**, e essa é uma das melhores razões para preferi-los.

Um `switch` sobre um texto compara com `equals` (por dentro), não com `==`, de modo que não cai na armadilha de 2.2.2. E se o texto é `null`, o `switch` lança uma `NullPointerException` antes de olhar qualquer caso.

#### 2.3.4 Métodos e constantes

Os métodos do Java são declarados escrevendo, em ordem, o **tipo que devolvem**, o **nome**, os **parâmetros com seu tipo** entre parênteses e o corpo entre chaves. `String marcar(long tiempoMs)` é um método que recebe um `long` e devolve um `String`. Se não devolve nada, o tipo é `void` (como em `void main()`). Devolve-se com `return`, e o compilador exige que **todos os caminhos** do método devolvam algo: se um `if` sem `else` deixa um caminho sem `return`, é um erro de compilação. Dentro de um arquivo compacto os métodos se escrevem na mesma altura que o `main`, e o `main` os chama pelo nome.

Duas particularidades em relação a outras linguagens. O Java **não tem parâmetros com valor padrão**: se você quer uma versão curta de um método, escreve outro com o mesmo nome e menos parâmetros (a Lição 3 fará isso com os *records*). E os argumentos são passados **por valor**: o método recebe uma cópia. Veja o `fig02_17.java`, que além disso estreia uma constante:

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

O método `marcar` põe seu parâmetro `tiempoMs` em 0, e ao voltar, a variável do `main` continua valendo 2300. Mudar o parâmetro mudou a cópia, não a original. (Com objetos acontece o mesmo, mas o que se copia é o **endereço**: o método pode mudar o conteúdo de um objeto mutável, mas não pode fazer com que a variável de quem o chamou aponte para outro.)

A linha do começo, `static final int UMBRAL_LENTO_MS = 1000;`, é uma **constante**. Cada palavra conta. `final` quer dizer “é atribuída uma única vez”; uma tentativa de reatribuí-la é um erro de compilação. `static` quer dizer “pertence à classe e não a um objeto”; em um arquivo compacto, a “classe” é o arquivo inteiro. O nome vai em `MAIUSCULAS_COM_SUBLINHADO` (assim pede a convenção do Java), e leva a unidade: `_MS`. Escrevê-lo em um único lugar e usá-lo pelo nome, em vez de escrever `1000` em cada comparação, tem um motivo prático: quando o limite mudar, muda em um lugar; e um de comunicação: `tiempoMs >= UMBRAL_LENTO_MS` se lê, e `tiempoMs >= 1000` obriga a lembrar o que é mil.

### 2.4 `null` e as mensagens de `NullPointerException`

#### 2.4.1 O que é `null` e quando estoura

Dissemos em 2.1.2 que uma referência pode não apontar para nada, e que isso se escreve `null`. O problema não é que exista, mas o que acontece quando você **usa** um `null` como se fosse um objeto: chamar um método dele, ler um de seus campos, ou, no caso de um wrapper, tirar o valor dele. Em qualquer desses casos o programa para com uma exceção chamada `NullPointerException` (diz-se “NPE”). Tony Hoare, que inventou o conceito em 1965, depois o chamou de [“meu erro de um bilhão de dólares”](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/). O Java não o removeu, e a maior parte deste curso é a história de como viver sem depender dele.

Veja o `fig02_18.java`. O método `codigoDe` devolve `null` para um serviço desconhecido, e o `main` tenta somar 1 a ele:

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

A primeira linha sai bem, porque colar um `null` a um texto com `+` apenas escreve “null”. A segunda é a que estoura: `codigo + 1` precisa do número que há dentro do `Integer`, ou seja, fazer o *unboxing* de 2.1.2, e não há número. É a armadilha dos wrappers: **a soma parece de dois inteiros, mas um deles pode não existir**. Se `codigo` fosse `int`, não haveria nada que falhasse, mas também não haveria como dizer “sem resposta”.

#### 2.4.2 Ler a mensagem

Já há várias versões do Java, uma `NullPointerException` não se limita a dizer “é nulo”: diz **o que você tentava fazer** e **o que era nulo**. É o recurso das “mensagens úteis de NPE”, descrito no [JEP 358](https://openjdk.org/jeps/358). A do `fig02_18.java` diz:

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "<local1>" is null
	at fig02_18.main(fig02_18.java:5)
```

Lê-se em duas metades. **Antes de `because`: o que o programa tentava.** “Não é possível invocar `Integer.intValue()`”, ou seja, tirar o valor do wrapper. **Depois de `because`: o que era nulo.** “A variável `<local1>`”. E a linha de baixo, `at fig02_18.main(fig02_18.java:5)`, diz onde: o método `main`, arquivo `fig02_18.java`, linha 5. Um NPE se corrige lendo essa mensagem, não abrindo um depurador.

Agora, o detalhe que o temário prometeu esclarecer. Por que diz `<local1>` e não `codigo`? Porque **o lançador `java Archivo.java` compila o arquivo na memória sem guardar os nomes das variáveis locais**. Esses nomes só existem no código-fonte; para que a JVM os conheça, o compilador tem de incluí-los, e você pede isso com a opção `-g`. Compile você mesmo, com `javac -g`, e execute com `java`:

```text
$ javac -g fig02_18.java
$ java fig02_18
código: null
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "codigo" is null
	at fig02_18.main(fig02_18.java:5)
```

Agora sim: `because "codigo" is null`. É o mesmo programa, o mesmo erro e a mesma linha, com uma mensagem muito mais clara. Quando você topar com um `<local3>` no trabalho, já sabe o que é: o número do slot que a JVM deu a essa variável local (em um método de instância o slot 0 é `this`, e os demais se distribuem na ordem em que são declarados; um `long` ou um `double` ocupa dois). E quando quiser o nome, compile com `-g`.

Há mais formas de mensagem. Se o nulo não é uma variável mas o **resultado de um método**, a mensagem diz. É o `fig02_19.java`:

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

“Because the return value of `fig02_19.codigoDe(String)` is null” se traduz: o método `codigoDe` devolveu nulo, e o programa pediu `toString()` a esse valor. Não é preciso adivinhar qual das partes da expressão `codigoDe("reportes").toString()` era nula: a mensagem aponta a que foi. Quando uma cadeia tem várias chamadas encadeadas (`a.b().c().d()`), isso é o que poupa meia hora.

#### 2.4.3 Três regras para viver com `null`

O `null` não desaparece do código real, mas há maneiras de domesticá-lo, e o `revisor` as aplica desde hoje:

1. **Verifique antes de usar, com `&&`.** `codigo != null && tiempoMs != null` é a condição do `revisor` para saber se um serviço respondeu. Se a primeira parte falha, a segunda não é avaliada.
2. **Ponha o literal à esquerda no `equals`.** `"pagos".equals(nombre)` não pode estourar; `nombre.equals("pagos")` sim.
3. **Não devolva `null` quando há uma forma melhor de dizer “não há dado”.** Aqui o fazemos porque ainda não temos outra, mas na Lição 3 o `null` desaparece do `revisor` e um tipo selado o substitui, e na 4 você verá `Optional`. Um método que pode devolver nulo é um método que obriga todos os que o chamam a lembrar de verificá-lo.

### 2.5 O passo do projeto: o `revisor` com suas funções base

Agora você junta tudo. Você quer que o `Revisor.java` imprima a tabela da qual a lição partiu, calculando cada célula com funções. As regras são as quatro de sempre do `revisor`:

1. Se o serviço não respondeu, está **CAIDO**.
2. Se respondeu com um código de 400 ou mais, está **CAIDO** (o detalhe é o código).
3. Se respondeu e demorou 1000 ms ou mais, está **LENTO**.
4. Se não, está **ARRIBA**.

Os dados dos quatro serviços vivem, por ora, em dois `switch` paralelos: um devolve o código e outro o tempo. Um serviço que não respondeu tem `null` nos dois. O seu `Revisor.java` fica assim (guarde-o em `~/curso-java/revisor/`; a figura o chama `fig02_20.java` para distingui-lo das outras, e seu conteúdo é o do seu arquivo):

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

Percorra o programa com o que você sabe, de cima para baixo:

- **`UMBRAL_LENTO_MS`** é a constante de 2.3.4.
- **`codigoDe` e `tiempoDe`** são os dois `switch` com seta de 2.3.3. Devolvem `Integer` e `Long`, não `int` e `long`, para poder devolver `null` (2.1.2). Os literais levam o `L` em `tiempoDe` porque o resultado é um `Long`, e um `142` sozinho seria um `Integer`.
- **`clasificar`** implementa as quatro regras. A primeira linha, `codigo != null && tiempoMs != null`, usa o `&&` de 2.3.2: de outro modo, as comparações de baixo desembrulhariam um nulo. As regras se aplicam por ordem, cada uma com seu `return`; quando uma é cumprida, as demais não são avaliadas.
- **`formatearTiempo`** recebe um `Long`, que pode ser nulo (o traço “—” é o sinal de “sem dado”), e senão decide entre `142ms` e `2.3s`. Repare que usa `Locale.ROOT` (2.2.3).
- **`detalle`** devolve “sin conexión” (“sem conexão”) para o nulo e, senão, o código como texto. `String.valueOf(codigo)` converte um `Integer` em `String`.
- **`linea`** monta uma linha com o modelo `%-12s%-10s%6s  %s`: 12 de largura para o nome, 10 para o estado, 6 alinhado à direita para o tempo, dois espaços e o detalhe. `encabezado` reutiliza `linea` com os títulos, para que tenham as mesmas medidas.
- **`revisar`** une tudo: busca o código e o tempo de um serviço, e entrega sua linha completa. A chamada a `linea` é partida em duas linhas porque passaria de 100 colunas; a continuação vai com quatro espaços a mais.
- **`main`** imprime o cabeçalho e as quatro linhas, uma por uma. Não há laço porque não há lista: os serviços ainda não são dados que se possam percorrer.

E a tabela que sai é **caractere por caractere** a que prometemos no começo. Observe que `reportes` passou por todo o caminho do `null`: `codigoDe` e `tiempoDe` devolvem nulo para ele, `clasificar` o manda para `CAIDO` pela regra 1, `formatearTiempo` põe o traço e `detalle` escreve “sin conexión”. Cada peça decidiu sozinha o que fazer com o vazio. Mas essa decisão se repetiu **quatro vezes** (uma por função), e disso trata a próxima lição.

Para executá-lo a partir da sua pasta de trabalho:

```text
$ cd ~/curso-java/revisor
$ java Revisor.java
```

## O erro que você vai ver

A lição tem um erro principal e quatro mensagens de apoio que convém saber ler de relance.

**O erro principal: um `==` que dá `false` quando você esperava `true`.** É o do `fig02_08.java`. Quando um texto montado em tempo de execução é comparado com `==` contra um literal, o Java pergunta se são o mesmo objeto, e não são:

```text
armado == literal:       false
armado.equals(literal):  true
```

Não há mensagem de erro; por isso é o que mais custa. O sinal é que **uma comparação que deveria ser verdadeira dá falsa** e todo o resto parece estar bem. Se você vir um `==` entre dois textos ou dois wrappers em um `if`, desconfie dele antes de qualquer outra coisa.

**As quatro mensagens de apoio:**

- `error: incompatible types: possible lossy conversion from long to int`: você atribuiu um tipo grande a um menor. Mude o tipo de destino, ou faça a conversão explícita sabendo o que pode perder (`fig02_04.java`).
- `error: cannot infer type for local variable respuesta`, com `(variable initializer is 'null')`: você usou `var` sem um valor do qual deduzir o tipo (`fig02_06.java`).
- `error: the switch expression does not cover all possible input values`: um `switch` que produz um valor e não cobre todos os casos. Acrescente o `default` (ou, com `enum` e tipos selados, o caso que falta) (`fig02_16.java`).
- `NullPointerException: Cannot invoke "String.isBlank()" because "<local1>" is null`: você usou um nulo como se fosse um objeto. A parte posterior a `because` diz qual era nulo; se diz `<localN>` em vez do nome, o arquivo foi compilado sem `-g` (`fig02_12.java`, `fig02_18.java`).

E um efeito de fundo que não produz mensagem alguma: `2,3s` em vez de `2.3s`. Acontece em uma máquina com configuração regional da Espanha quando se formata um decimal sem `Locale.ROOT` (`fig02_09.java`).

## O que se faz errado

**Comparar textos ou wrappers com `==`.** Funciona com os literais e com os números pequenos, e falha com os dados que chegam de fora. Com textos, `equals` (com o literal à esquerda); com wrappers, `equals` ou, se são primitivos, `==`.

**Chamar um método de `String` e não guardar o resultado.** `linea.strip();` solto não faz nada. Os textos não mudam: os métodos devolvem outro.

**Usar `&` ou `|` onde vão `&&` ou `||`.** Os dois são válidos, e o primeiro estoura justamente quando a verificação servia.

**Um `int` para algo que pode crescer.** Os milissegundos de um mês não cabem em um `int`. Se o dado é um tempo, uma contagem ou um tamanho, comece com `long`.

**Um `double` para dinheiro ou para comparar com `==`.** Os decimais binários não são exatos (`0.1 + 0.2`). Os tempos do `revisor` são inteiros em milissegundos justamente para escapar disso.

**Um cast `(int)` para calar o compilador.** Silencia o aviso e deixa o estouro sem aviso. Antes de escrevê-lo, pergunte-se se o tipo de destino não deveria ser maior.

**`String.format` com decimais sem `Locale`.** Funciona na sua máquina e falha na de outra pessoa. `Locale.ROOT` sempre.

**Um `switch` com a sintaxe de dois-pontos.** Um `break` esquecido produz erros que não aparecem no compilador. Com a seta desaparece toda essa classe de erros.

**Devolver `null` por costume.** Cada `null` que sai de um método obriga todos os que o chamam a lembrar de verificá-lo. Aqui o usamos porque ainda não temos outra ferramenta, e a Lição 3 a traz.

**`var` onde o tipo não se vê.** `var r = revisar(s);` obriga o leitor a ir procurar o que é `r`. Use-o quando o tipo salta aos olhos.

## Exercícios

### Exercício 1 — Preveja, depois execute

Escreva em um arquivo o `main` que imprime estas seis expressões, **uma por linha**, e antes de executá-lo anote num papel o que você acha que cada uma imprimirá:

1. `"142" + 1`
2. `1 + 2 + "3"`
3. `"1" + 2 + 3`
4. `-7 / 2`
5. `0.1 + 0.2 == 0.3`
6. `'a' + 1`

Execute e compare. Em cada caso em que você errou, escreva uma frase no seu [diário de bordo](bitacora.md) com a regra que você não tinha presente.

### Exercício 2 — O valor de retorno ignorado

Escreva um método `String normalizar(String nombre)` que tire os espaços das pontas e passe o texto para minúsculas, e teste-o com `"  PAGOS "`. Escreva-o **primeiro da maneira errada** (chamando `strip()` e `toLowerCase()` sem guardar o resultado) e veja o que devolve. Depois corrija. Dá para corrigir em uma única linha?

### Exercício 3 — Duas maneiras de não estourar

Escreva dois métodos: `boolean vacio(String detalle)`, que devolva `true` se o detalhe é nulo **ou** está em branco, e `boolean contieneError(String detalle)`, que devolva `true` se o detalhe não é nulo **e** contém a palavra `error`. Teste-os com `null`, com `"   "`, com `"200"` e com `"error de red"`. Pense antes de escrever: qual dos dois usa `&&` e qual `||`? O que aconteceria com o operador errado?

### Exercício 4 — O projeto: um dado em um único lugar

No `Revisor.java` da seção 2.5, acrescente `facturacion`, que respondeu com o código **503** em **95 ms**. Faça isso **em dois passos**. Primeiro, acrescente-o só em `codigoDe` (e ao `main`, para que sua linha seja impressa), e execute. Preveja antes que linha vai sair. Depois acrescente-o também em `tiempoDe` e execute de novo. Anote no diário de bordo que problema há em manter a mesma informação em dois lugares.

## Soluções

### Solução 1

```text
1421
33
123
-3
false
98
```

1. `"142" + 1` é um texto colado com um número: o `+` converte o 1 em texto e os junta, `"1421"`. Não é uma soma.
2. `1 + 2 + "3"` é avaliado da esquerda para a direita: primeiro `1 + 2` (dois números) dá 3, e depois `3 + "3"` cola, `"33"`.
3. `"1" + 2 + 3` começa com um texto, então tudo o que vem depois é colado: `"123"`.
4. `-7 / 2` é uma divisão inteira, que descarta a parte decimal em direção ao zero: `-3`, não `-4`.
5. `0.1 + 0.2 == 0.3` é `false`, porque a soma é `0.30000000000000004`, como vimos em 2.1.1.
6. `'a' + 1` soma um `char` e um `int`; o `char` é convertido em seu número (97), e dá `98`. Para obter o texto “a1” seria preciso partir de um texto: `"" + 'a' + 1`.

### Solução 2

A versão errada, que não muda nada:

```text
[  PAGOS ]
false
```

A versão correta se escreve em uma única linha, **encadeando** as chamadas, porque cada método devolve um `String` sobre o qual se pode chamar o seguinte:

```java
String normalizar(String nombre) {
  return nombre.strip().toLowerCase();
}
```

Com ela, a saída é:

```text
[pagos]
true
```

### Solução 3

`vacio` usa `||`: basta que **uma** das duas coisas se cumpra. `contieneError` usa `&&`: têm de se cumprir **as duas**, e a primeira protege a segunda.

```java
boolean vacio(String detalle) {
  return detalle == null || detalle.isBlank();
}

boolean contieneError(String detalle) {
  return detalle != null && detalle.contains("error");
}
```

Os testes dão `true`, `true`, `false` para `vacio` com `null`, `"   "` e `"200"`, e `false`, `true` para `contieneError` com `null` e com `"error de red"`. Se você escrever `|` em vez de `||` em `vacio`, com `null` o lado direito (`detalle.isBlank()`) é avaliado e estoura; com `&` em vez de `&&` em `contieneError`, o mesmo. E uma terceira saída, sem condição: `"sin conexión".equals(detalle)` dá `false` com um `detalle` nulo, porque o literal da esquerda nunca é nulo.

### Solução 4

Com `facturacion` só em `codigoDe`, a linha que aparece é esta:

```text
facturacion CAIDO          —  503
```

Sai CAIDO por uma razão errada, que é o perigo deste design. `codigoDe` diz 503, mas `tiempoDe` diz `null`, então `clasificar` aplica a regra 1 (“não respondeu”) e não a 2 (“respondeu com um código de erro”), e o tempo sai como um traço ainda que o serviço tenha de fato respondido em 95 ms. O detalhe mostra o 503, que contradiz o traço. Não há nenhum erro do compilador nem da JVM. Com o dado também em `tiempoDe`, a linha correta é:

```text
facturacion CAIDO       95ms  503
```

A lição que vai para o diário de bordo: **um dado que vive em dois lugares acaba dizendo duas coisas**. É a razão pela qual a próxima lição reúne código e tempo em um único valor, e a razão de que o compilador, e não a sua memória, seja quem obriga você a completá-lo.

## Como sei que consegui

- [ ] `java fig02_08.java` imprime `false` e `true` nas duas primeiras linhas, e você sabe explicar por quê sem olhar a lição.
- [ ] `java fig02_07.java` mostra `[  catalogo  ]` na primeira linha, e você sabe o que mudaria para que mostre `[catalogo]`.
- [ ] Se você tirar a linha `default -> null;` de `codigoDe`, o lançador responde com `the switch expression does not cover all possible input values`.
- [ ] No `fig02_12.java` você vê a `NullPointerException` com `"<local1>"`, e com `javac -g` vê o nome `"detalle"`.
- [ ] A partir de `~/curso-java/revisor/`, `java Revisor.java` imprime o cabeçalho e as quatro linhas da tabela, com `reportes` em `CAIDO`, `—` e `sin conexión`.
- [ ] Você consegue explicar por que `2300 == 2300` dá `false` com dois `Integer` e `true` com dois `int`.
- [ ] Os exercícios 1 e 3 você previu antes de executar, e anotou no seu diário de bordo qual das seis expressões do exercício 1 você não acertou.
- [ ] Você anotou no seu diário de bordo qual das mensagens desta lição foi a mais difícil de ler.

## Resumo

Responda sem olhar a lição:

1. Que diferença há entre `int` e `Integer`, e quando você precisa do segundo?
2. O que acontece quando um `int` supera seu valor máximo? Como você evita isso em uma conta de milissegundos?
3. Que tipo tem uma variável declarada com `var`, e quando isso é decidido?
4. Por que `linea.strip();` não muda `linea`? O que você escreve no lugar?
5. O que o `==` compara entre duas referências? Por que `armado == literal` dá `false` e `constante == literal` dá `true`?
6. Por que se passa `Locale.ROOT` ao `String.format` quando há decimais?
7. Que diferença há entre `&&` e `&` quando o lado esquerdo é falso?
8. Que duas coisas distinguem um `switch` com setas do clássico? O que acontece se ele não cobre todos os casos?
9. O que significam `static` e `final` em uma constante?
10. Como se lê uma `NullPointerException`: o que ela diz antes de `because` e o que depois? Por que às vezes aparece `<local1>`?

## Para ler mais

- [Os tutoriais do Java, “Primitive Data Types”](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html): os oito tipos primitivos com suas faixas e valores padrão, na documentação oficial (em inglês, como quase toda). Consultado em 7 de outubro de 2026.
- [Javadoc de `String`, Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html): todos os métodos da classe, com a regra de que os textos são constantes. Consultado em 7 de outubro de 2026.
- [JEP 361, “Switch Expressions”](https://openjdk.org/jeps/361) e [JEP 378, “Text Blocks”](https://openjdk.org/jeps/378): o porquê e as regras exatas de dois dos recursos usados nesta lição. Consultado em 7 de outubro de 2026.
- [JEP 358, “Helpful NullPointerExceptions”](https://openjdk.org/jeps/358): o que diz cada parte da mensagem e por que faltam os nomes quando se compila sem `-g`. Consultado em 7 de outubro de 2026.

---

**Anterior:** [Lição 1 — Instalar o Java 25 no seu Linux Mint](01-instalacion.md) ·
**Próxima:** [Lição 3 — Records, enums e tipos selados](03-records-enums-sellados.md)
