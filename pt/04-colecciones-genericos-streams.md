# Lição 4 — Coleções, genéricos e streams

**Tempo:** 2 × 90 min (duas sessões; o corte fica entre as seções 4.2 e 4.3)

**O que você constrói:** a lista de serviços e o relatório do `revisor`: agora o programa guarda seus serviços em uma coleção, resume os resultados com um *stream* e os entrega em uma tabela com sua linha de resumo

**O que você aprende:** `List`, `Set` e `Map`; coleções imutáveis e cópias defensivas; genéricos e seu apagamento; lambdas e *streams* com `Collectors`; `Comparator`; e `Optional` como valor de retorno

**De onde você vem.** Você traz o projeto da [Lição 3](03-records-enums-sellados.md): sete arquivos que cumprem um trato claro. `Estado` é um `enum` com três valores; `Respuesta` é uma interface selada que só pode ser `Respondio` (com seu código e seu tempo) ou `SinRespuesta` (com seu motivo); `Servicio` e `Resultado` são *records*; `Clasificador` decide o estado com um `switch` exaustivo; `Formato` monta as linhas; e `Revisor.java` escreve à mão os quatro serviços e os verifica um por um. A tabela sai bem, mas tem três buracos. Não há uma lista: os serviços são quatro variáveis soltas. Não há resumo: nenhuma linha diz quantos estão no ar. E os dados simulados vivem dentro de um `switch`, misturados com a lógica. Esta lição preenche os três buracos com as ferramentas que o Java traz para guardar e transformar muitos valores. Você trabalha na sua pasta `~/curso-java/revisor/` e executa tudo com `java`.

**O que esta lição não faz.** Não lê arquivos nem captura erros: a lista de serviços continua escrita em `Revisor.java`, e tirá-la dali é o trabalho da [Lição 5](05-errores-excepciones.md). Tampouco consulta nenhuma rede: o `Simulador` que você escreve hoje faz o papel da rede até a lição 8. E não usa Maven: os arquivos continuam soltos em uma pasta, como na lição 3.

**Sobre as execuções.** Para executar uma figura, entre na pasta da lição no repositório (`programas/04-colecciones-genericos-streams/`) e rode `java fig04_01.java`, com o número da figura que quiser. As que vivem em uma pasta própria (a do projeto da seção 4.5) são executadas de dentro dela. Todas as saídas foram obtidas com o Java 25 (Temurin 25.0.4) e o idioma do terminal em UTF-8; no Linux Mint isso é o normal; se você vir pontos de interrogação onde vão acentos, `echo $LANG` diz que idioma o seu terminal tem. Os stack traces de erro trazem linhas do próprio JDK (`at java.base/...`) com números de linha que mudam de uma atualização para outra: é normal que os seus difiram nesses números.

## Ao terminar, você vai conseguir

- Escolher entre `List`, `Set` e `Map` conforme o que você precisa guardar, e dizer que ordem de percurso cada implementação tem.
- Criar uma coleção imutável com `List.of`, reconhecer a `UnsupportedOperationException` que sai ao modificá-la e proteger um *record* com uma cópia defensiva.
- Explicar o que são os genéricos, o que o compilador garante com eles e o que se perde com o apagamento de tipos.
- Escrever uma lambda e uma referência a método, e montar com elas um *stream* de `filter`, `map` e `collect`.
- Resumir uma lista de resultados com `Collectors.groupingBy`, `counting` e `average`, e ordená-la com um `Comparator`.
- Devolver um `Optional` em vez de `null` e lê-lo sem chamar `get()` às cegas.

## O porquê antes do como

Abra o seu `Revisor.java` da lição 3. Os quatro serviços estão escritos como quatro chamadas separadas a `revisar`, e aí acaba o que o programa sabe deles: não pode dizer quantos são, não pode percorrê-los em um laço, não pode acrescentar um sem escrever outra linha. Agora imagine que lhe peçam a linha que fecha o relatório: “4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms” (“4 serviços · 2 no ar · 1 lento · 1 fora do ar · média 843ms”). Para produzi-la com o que você tem até hoje, precisaria de quatro contadores (`arriba`, `lento`, `caido`, `total`), uma soma, um divisor, e atualizar tudo cada vez que um serviço mudasse de estado. Cada serviço novo, e cada estado novo, tocaria vários lugares do programa.

Todas as linguagens resolveram isso igual: com **coleções**, estruturas que guardam muitos valores e sabem se percorrer, se buscar e se contar sozinhas. Se você vem do Python conhece a lista, o conjunto e o dicionário; se vem do Go, o *slice*, o mapa e o costume de usar um `map[string]struct{}` como conjunto; se vem do TypeScript, o array, o `Set` e o `Map`. O Java tem as três, e as organiza de uma forma que se parece menos com as outras linguagens do que parece: são **interfaces** (`List`, `Set`, `Map`) com **várias implementações** intercambiáveis, e essa separação explica quase tudo o que você vai ver hoje, desde por que há três mapas diferentes até por que existem as coleções imutáveis.

Há mais um problema. Uma coleção guarda valores, mas de que tipo? Se guarda qualquer coisa, o compilador não pode ajudar: você teria de verificar à mão que o que tira da lista é o serviço que esperava. O Java resolve isso com os **genéricos**: você declara que a lista é de `Servicio` e o compilador impede que entre outra coisa. É uma das ideias mais úteis da linguagem e tem um detalhe que surpreende quase todos os que chegam de outro lugar: os genéricos existem só na compilação (são “apagados” na execução), e a lição 0 já adiantou isso.

E resta a pergunta de como transformar uma coleção em outra: filtrar os lentos, converter resultados em linhas de texto, contar por estado. O Java faz isso com **streams**, uma forma de descrever a transformação como uma cadeia de passos. E como nem todo serviço tem um tempo (o que está fora do ar não respondeu), também é preciso decidir como se representa “não há valor” sem voltar aos `null` da lição 2: para isso existe o `Optional`.

O caminho tem duas sessões. A **primeira** cobre as seções 4.1 e 4.2: as coleções, as imutáveis e os genéricos; ao terminá-la você sabe guardar e proteger os serviços do `revisor`. A **segunda** cobre 4.3, 4.4 e 4.5: os *streams*, o `Optional` e o relatório completo; ao terminá-la o programa imprime a tabela com seu resumo. Este é o destino, para que você tenha o ponto de chegada à vista:

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

A tabela você já conhecia; o novo é a linha do final, e que agora ela sai de uma lista de verdade e não de quatro chamadas escritas à mão.

## Os conceitos

### 4.1 Coleções

#### 4.1.1 Uma lista: `List` e `ArrayList`

Uma **lista** é uma sequência ordenada de valores em que cada um tem uma posição, que em Java começa em 0. Em Java, `List` não é uma classe mas uma **interface**: um contrato que diz o que se pode fazer com uma lista (acrescentar, buscar, remover, percorrer) sem dizer como ela é guardada por dentro. A implementação que você vai usar quase sempre se chama `ArrayList`, que guarda os valores em um array interno que cresce quando enche. O padrão de uso é declarar a variável com a interface e criar o objeto com a implementação:

```java
List<String> nombres = new ArrayList<>();
```

Essa linha tem três peças que convém separar. `List<String>` é o tipo da variável: uma lista de textos (o que vai entre os sinais `<` e `>` é o tipo de seus elementos; a seção 4.2 explica por quê). `new ArrayList<>()` cria uma lista vazia da classe concreta. E o `<>` vazio se chama **diamante**: diz ao compilador “o tipo dos elementos é o mesmo que já declarei à esquerda, não me faça repeti-lo”. Por que declarar a variável como `List` e não como `ArrayList`? Porque o resto do programa só precisa do contrato. Se amanhã você trocar a implementação por outra, muda uma palavra em uma linha e nada mais. É a mesma razão pela qual na lição 3 `Respuesta` era uma interface e não uma classe.

Veja tudo o que se pode fazer com uma lista no programa `fig04_01`. Antes de lê-lo, uma precisão: é um arquivo compacto, como os da lição 2, e por isso não precisa escrever `import` para `List` nem `ArrayList`. Os arquivos compactos importam sozinhos todo o módulo `java.base`, que inclui `java.util`, onde vivem as coleções. Quando uma classe normal precisar deles, como acontece na seção 4.5, ela os importará com uma linha no começo do arquivo.

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

Rode o programa e compare com o que você previu:

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

Três detalhes dessa saída. Primeiro, `println` sabe imprimir uma lista inteira: escreve-a entre colchetes com os elementos separados por vírgulas, porque `ArrayList` define sua própria representação como texto. Segundo, `remove("pagos")` removeu o elemento cujo valor é “pagos”; com textos não há dúvida possível, mas `remove` tem duas versões (uma que recebe um valor e outra que recebe uma posição) e com listas de números elas se confundem. A seção “O que se faz errado” mostra a armadilha. Terceiro, `getFirst()` e `getLast()` são relativamente novos: chegaram no Java 21 com as coleções sequenciadas ([JEP 431](https://openjdk.org/jeps/431)) e evitam o velho `get(lista.size() - 1)` para o último elemento.

As duas formas de percorrer uma lista do programa merecem explicação, porque o curso não as tinha usado até hoje. O **`for` aprimorado** (`for (String nombre : nombres)`) se lê “para cada `nombre` em `nombres`”: a cada volta, `nombre` vale o próximo elemento, e não há índice a controlar. É a forma habitual. O **`for` clássico** (`for (int i = 0; i < nombres.size(); i++)`) controla o índice à mão: começa em 0 (`int i = 0`), repete enquanto a condição for verdadeira (`i < nombres.size()`) e avança no fim de cada volta (`i++`, que significa “some um a `i`”). Use-o somente quando precisar da posição. Se você errar com ela, a lista avisa com uma exceção: pedir `nombres.get(10)` em uma lista de três elementos lança `IndexOutOfBoundsException: Index 10 out of bounds for length 3`, e a mensagem traz o índice e o tamanho, que é tudo de que você precisa para entender o que aconteceu.

Uma última comparação com as linguagens que você já conhece, porque a pergunta aparece sempre. O Java também tem arrays (`String[]`, de tamanho fixo, que servem para poucas coisas e que você verá na lição 5 e na 6 ao escrever `String[] args`), mas no código do dia a dia se usam listas: crescem, são buscadas, são passadas a funções e combinadas com *streams*. O que em Go é um *slice* e em Python uma `list`, em Java é um `List<T>`.

#### 4.1.2 Conjuntos e mapas

Um **conjunto** (`Set`) guarda valores **sem repetir**: acrescentar um que já está não faz nada. Um **mapa** (`Map`) guarda **pares** de chave e valor, e busca o valor pela chave. O programa `fig04_02` usa os dois:

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

Leia a saída com calma, porque tem várias lições. No conjunto, `add` **devolve um `boolean`**: `true` se o valor entrou e `false` se já estava. Por isso as três primeiras linhas dizem `true`, `true`, `false`: é uma forma barata de perguntar “já tinha visto isso?”, e você a usará na lição 5 para detectar nomes repetidos na configuração.

No mapa, `put` guarda um par e **devolve o valor anterior** dessa chave, ou `null` se não havia. Na saída, `put("pagos", 90L)` imprimiu `87`, o valor que acabava de substituir. `get` busca pela chave e devolve `null` se a chave não existe: `tiempos.get("reportes")` imprimiu `null`. Esse `null` é a mesma fonte de erros da lição 2, e por isso existe `getOrDefault("reportes", 0L)`: devolve o valor padrão que você indicar quando a chave falta. `containsKey` responde à pergunta “esta chave está aqui?” sem tocar no valor.

Para percorrer um mapa percorre-se o conjunto de seus pares com `entrySet()`; cada par é um `Map.Entry`, do qual `getKey()` dá a chave e `getValue()` o valor. Você verá que o tipo se escreve longo (`Map.Entry<String, Long>`); no código real se encurta com `var`, que você já conhece da lição 2: `for (var par : tiempos.entrySet())`.

A pergunta seguinte é a mais importante da seção: **em que ordem saem os elementos?** A resposta depende da implementação, e por isso o Java tem várias de cada interface:

| Implementação | Ordem de percurso | Quando usá-la |
|---|---|---|
| `HashSet`, `HashMap` | nenhuma que você possa prometer | quando só importa buscar rápido |
| `LinkedHashSet`, `LinkedHashMap` | a ordem em que foram inseridos | quando o relatório deve sair sempre igual |
| `TreeSet`, `TreeMap` | ordenado pela chave | quando você precisa da ordem natural (alfabética, numérica) |

A figura usou `LinkedHashSet` e `LinkedHashMap` para que a saída seja sempre a que você vê, e terminou copiando o mapa para um `TreeMap`, que o reordenou alfabeticamente (aqui coincide com a ordem de inserção porque os nomes já estavam em ordem). Para o `revisor` isso não é um detalhe: o temário do curso promete um relatório “na mesma ordem sempre”, e uma tabela que sai em outra ordem cada vez que você roda o programa é um relatório inútil. A regra prática é: **se a ordem importa para quem lê o resultado, escolha uma implementação que a garanta**, e não confie em que um `HashMap` “parece” ordenado na sua máquina. Na verdade, as coleções imutáveis que você vê na próxima seção (`Set.of`, `Map.of`) têm uma ordem que **muda de uma execução para outra** de propósito: medi a mesma linha, `Set.of("catalogo", "inventario", "pagos", "reportes")`, em três execuções seguidas do mesmo programa e saiu `[pagos, reportes, catalogo, inventario]`, depois `[catalogo, inventario, pagos, reportes]` e depois `[catalogo, reportes, pagos, inventario]`. A documentação avisa: [“the iteration order of set elements is unspecified and is subject to change”](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Set.html). A ordem não faz parte do contrato, e o JDK não se dá ao trabalho de mantê-la estável.

Resta uma pergunta: como um `Set` sabe que dois valores são “o mesmo”? Usa dois métodos: `equals`, que você já conhece, e `hashCode`, que devolve um número inteiro que resume o conteúdo do objeto (dois objetos iguais devem dar o mesmo número, e isso permite ao conjunto buscar sem comparar com todos). Com textos e números funciona sem você fazer nada. Com seus próprios tipos, depende de como estão definidos, e aqui os *records* da lição 3 pagam o que prometeram: geram `equals` e `hashCode` a partir de todos os seus componentes. O programa `fig04_03` mostra: dois `Direccion` com o mesmo servidor e a mesma porta são iguais ainda que sejam dois objetos diferentes, e por isso o conjunto não repete e o mapa encontra o valor com uma chave recém-construída.

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

O conjunto terminou com dois elementos e não três, o mapa encontrou o “equipo de cobranza” (“equipe de cobrança”) com uma chave nova, e a chave com outra porta (`8443`) não existe, então `get` devolveu `null`. Se em vez de um *record* você tivesse escrito uma classe normal sem definir `equals` e `hashCode`, cada objeto seria igual somente a si mesmo, e o conjunto guardaria os três. Essa é uma das razões pelas quais o curso prefere *records* para os dados.

#### 4.1.3 Coleções imutáveis

Até aqui as coleções podiam ser modificadas: acrescentar, remover, substituir. Muitas vezes isso é justamente o que você não quer. A lista de serviços que o `revisor` lê não deveria mudar enquanto outra pessoa a está percorrendo, e um relatório que já foi calculado não deveria poder ser alterado depois. O Java resolve isso com as **coleções imutáveis**, que são criadas com três métodos: `List.of(...)`, `Set.of(...)` e `Map.of(...)`. (Chamam-se métodos de fábrica estáticos: você não precisa de `new`, só do nome da interface, um ponto e os valores.) Veja o que acontece quando você tenta modificar uma:

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

O programa imprime a lista uma vez, e o `add` lança `java.lang.UnsupportedOperationException` (“operação não suportada”). É uma exceção sem mensagem, e não precisa dela: a operação pedida simplesmente não existe nessa lista. O stack trace se lê **de cima para baixo**, ao contrário do do Python: a primeira linha diz o que foi lançado, a segunda onde nasceu dentro do JDK (`ImmutableCollections.uoe`, “unsupported operation exception”), a terceira que método você chamou (`add`) e a última, `fig04_04.main(fig04_04.java:5)`, que linha do **seu** programa fez isso. Procure sempre a primeira linha que menciona o seu arquivo: ali está o seu erro. (Os números `159` e `164` são deste JDK e mudarão no seu.)

A imutabilidade dessas coleções é real e completa no que diz respeito à coleção: não se podem acrescentar, remover nem substituir elementos. Os autores do JDK acrescentaram outras regras, que aparecem na documentação de [`List`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/List.html) e que verifiquei ao escrever esta lição:

- **Não admitem `null`.** `List.of("a", null)` lança `NullPointerException` ao ser criada. É uma decisão em favor de menos surpresas: um `null` dentro de uma coleção é quase sempre um erro.
- **`Set.of` e `Map.of` rejeitam os repetidos.** `Map.of("a", 1, "a", 2)` lança `IllegalArgumentException: duplicate key: a`, e `Set.of("a", "a")` lança `IllegalArgumentException: duplicate element: a`. Uma chave repetida em um mapa de dados fixos é quase certamente um descuido, e é descoberta ao iniciar, não quando alguém consulta.
- **Sua ordem não é estável entre execuções**, como se mediu acima.
- **`Map.of` aceita até dez pares.** Para mais, existe `Map.ofEntries`.

Dois esclarecimentos para não confundir o que a imutabilidade promete. O primeiro: **imutável não significa que os elementos sejam imutáveis**. Uma `List.of(new ArrayList<>(), new ArrayList<>())` não deixa acrescentar nem remover listas, mas cada lista de dentro continua podendo ser modificada. Com *records* de componentes simples (textos, números, outros *records*), como os do `revisor`, o problema não existe; com elementos mutáveis, sim. O segundo: a lição 3 já mencionou a **imutabilidade superficial** dos *records* (um componente array continua sendo mutável), e isso é o mesmo com outro nome: o *record* não pode mudar de lista, mas se a lista que ele guarda é mutável, a lista sim muda. Isso leva à seção seguinte.

#### 4.1.4 Cópias defensivas

Aqui há uma armadilha que se aprende melhor vendo-a. Veja o programa `fig04_05`. Ele define dois *records* com a mesma forma: `ReporteIngenuo`, que guarda a lista tal como lhe dão, e `ReporteSeguro`, que em seu construtor compacto (o da lição 3) substitui a lista por `List.copyOf(nombres)`, uma **cópia imutável**. Depois passa a mesma lista a eles e a modifica em seguida:

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

O relatório ingênuo mudou sozinho, sem que ninguém o tocasse: guardou uma **referência** à lista original, não uma cópia, e quando o dono original acrescentou `reportes`, o “relatório” já calculado se alterou. O seguro conservou os dois nomes que tinha no momento de ser criado. Isso é uma **cópia defensiva**: o construtor se defende de que outra pessoa continue tendo uma referência aos dados que agora são seus. É uma dessas coisas que não falham: devolvem um número errado uma semana depois.

As duas últimas linhas da figura mostram outra diferença que confunde. `Collections.unmodifiableList(nombres)` não copia nada: é uma **visão** que não permite modificar *através dela*, mas que reflete tudo o que o dono original mudar (por isso `vista` terminou com `facturacion`). Já `List.copyOf(nombres)` faz uma cópia independente (por isso `copia` não a tem). Se o que você quer é que ninguém possa alterar os seus dados, precisa da cópia; a visão só impede de *escrever* quem a recebe.

Essa técnica não é um enfeite de estilo. O que você acabou de ver é exatamente a razão pela qual o `Reporte` da seção 4.5 leva `resultados = List.copyOf(resultados);` em seu construtor. E não é uma mania deste curso: os analisadores de código conhecem o buraco. Na lição 10 você conhecerá o SpotBugs, uma ferramenta que examina o programa compilado procurando defeitos prováveis, e que chama esse padrão de `EI_EXPOSE_REP` e `EI_EXPOSE_REP2` (de *exposing internal representation*, “expor a representação interna”). Com uma condição que convém saber desde já, porque foi medida: o SpotBugs só o reporta nas classes que se veem de fora do seu pacote. O `Reporte` do `revisor` não é público, e se você tirar a cópia a ferramenta fica calada. A cópia não está aí para contentar uma ferramenta: está porque a lista é do *record*, e o que a ferramenta não examina não deixa de ser um defeito.

Uma nota de eficiência: `List.copyOf` é inteligente. Se a lista que você passa já é imutável (por exemplo, a que `List.of` criou), não a copia, devolve a mesma. Faz sentido: não há nada a proteger. A documentação diz isso com a frase “if the given Collection is an unmodifiable List, calling copyOf will generally not create a copy”.

### 4.2 Genéricos

#### 4.2.1 O que são e o que lhe dão

Cada vez que você escreveu `List<String>` ou `Map<String, Long>` usou **genéricos**: uma classe ou interface que está definida para “algum tipo”, e que você concretiza ao usá-la. `List<E>` é definida uma única vez, com um tipo `E` a preencher, e você decide se é uma lista de `String`, de `Integer` ou de `Servicio`. O que você ganha é verificação **na compilação**: o compilador sabe que tipo de elemento a lista tem e rejeita o que não cabe. Vejamos o que ele diz quando você tenta pôr um texto em uma lista de números:

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

A mensagem é mais longa do que você esperaria. O compilador diz `no suitable method found for add(String)` (“não encontrei um método `add` que sirva para um `String`”) porque `List` tem dois métodos chamados `add`: um que recebe um elemento (`add(Integer)`) e outro que recebe uma posição e um elemento (`add(int, Integer)`). Tentou os dois e explica por que nenhum serve: o primeiro porque `String` não se converte em `Integer` (*argument mismatch*, “o argumento não coincide”), o segundo porque nem sequer tem a mesma quantidade de parâmetros. Leia sempre a **linha entre parênteses**: ali está a causa real. Se em vez disso você tenta tirar um valor e atribuí-lo à variável errada (`String texto = tiempos.get(0);`) a mensagem é a mais simples: `incompatible types: Integer cannot be converted to String`.

Sem genéricos, nenhum dos dois erros existiria na compilação: a lista aceitaria qualquer coisa, e o primeiro aviso chegaria na execução, longe de onde o erro foi cometido. É a diferença entre uma linguagem que protege você e uma que deixa você escrever de tudo. (A seção 4.2.3 mostra o que acontece quando alguém pula a proteção de propósito.)

#### 4.2.2 Os genéricos só aceitam referências: os wrappers

Escreva `List<int>` e veja o que o compilador diz:

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

A mensagem é `unexpected type; required: reference; found: int`: pedia-se uma referência e encontrou-se um primitivo. Os genéricos só trabalham com tipos por referência, e `int` é um primitivo (lição 2). A solução é usar o **wrapper** do primitivo: `Integer` para `int`, `Long` para `long`, `Double` para `double`, `Boolean` para `boolean`. São os tipos que você já conhece da lição 2 e que agora têm sua razão de ser. O Java converte entre um primitivo e seu wrapper sozinho, nas duas direções, quando é preciso: isso se chama ***autoboxing*** (colocar o `142` em um `Integer`) e *unboxing* (tirá-lo). É cômodo, mas tem duas armadilhas que você já conhece por outro nome, e valem uma figura cada uma.

A primeira: `==` entre wrappers compara **referências**, como com os `String`. O programa `fig04_08` mede isso:

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

Os dois `87` são “o mesmo” objeto e os dois `1000` não. A razão é que o *autoboxing* usa `Integer.valueOf`, e [sua documentação](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Integer.html) promete que “sempre guarda em cache os valores de −128 a 127, e pode guardar outros”: para os pequenos reutiliza o mesmo objeto, e para o `1000` desta figura, na prática, cria um novo cada vez. É um comportamento documentado, não um erro, e por isso é perigoso: um programa que compara com `==` funciona com os tempos pequenos dos testes e falha com os de produção. A regra é a da lição 2, estendida: **compare valores com `equals`**, nunca com `==`, sempre que o tipo seja uma referência, e os wrappers são.

A segunda armadilha é o *unboxing* de um valor que não está. O programa `fig04_09` pede a um `Map<String, Long>` um tempo e o guarda em uma variável `long`:

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

A primeira busca funciona. A segunda falha com uma `NullPointerException` cuja mensagem é uma joia de diagnóstico: `Cannot invoke "java.lang.Long.longValue()" because the return value of "java.util.Map.get(Object)" is null`. Lê-se assim: “não consegui chamar `longValue()` porque o que `Map.get` devolveu é `null`”. Ninguém escreveu `longValue()`: foi inserido pelo compilador ao converter o `Long` em `long`, e falhou porque `get("reportes")` devolveu `null` (a chave não existia). As mensagens de `NullPointerException` dizem sempre **o que** era nulo, e neste caso até que método o devolveu. Duas formas de evitar o erro: pedir `getOrDefault("reportes", 0L)`, ou guardar o resultado em um `Long` (não `long`) e perguntar `if (tiempo != null)`. A seção 4.4 traz uma terceira, melhor.

#### 4.2.3 O apagamento de tipos

A lição 0 já adiantou: **os genéricos são apagados**. O compilador usa `List<String>` para verificar o seu código, e depois de verificá-lo retira os tipos: no *bytecode*, uma `List<String>` e uma `List<Integer>` são a mesma `List`. Assim ficou desde o Java 5: o apagamento assegura que não se cria uma classe nova para cada tipo parametrizado, de modo que os genéricos [não custam nada na execução](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html), e o código escrito antes que existissem (quase todas as coleções eram) continuou funcionando. O programa `fig04_10` confirma isso e mostra o que custa:

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

Há várias coisas a ler aqui. O compilador imprimiu um aviso (`warning: [unchecked] unchecked call to add(E) as a member of the raw type List`) antes de executar o programa: avisa que na linha 9 você fez algo que ele não pode verificar. A causa está na linha 8: `List crudo = nombres;`. Uma `List` sem o `<...>` é um **tipo bruto** (*raw type*), o mesmo tipo de antes do Java 5. O compilador aceita guardar nele uma lista de `String` e aceita acrescentar-lhe um `142`, porque com o tipo bruto não verifica nada, só avisa. Depois, o programa imprime “agregué un número a una lista de String y nadie se quejó” (“acrescentei um número a uma lista de String e ninguém reclamou”) e falha na **linha 11**, não na 9:

`ClassCastException: class java.lang.Integer cannot be cast to class java.lang.String`

O erro apareceu ao *tirar* o elemento, duas linhas depois do que o colocou. Em um programa real poderiam ser duas classes diferentes e dez minutos de depuração. É a consequência de os genéricos serem verificados só na compilação: o compilador só pôde prometer que a lista era de `String` enquanto ninguém pulasse o sistema de tipos, e o tipo bruto é a brecha. A regra: **nunca use tipos brutos em código novo**; se você vir um aviso `[unchecked]`, é o compilador pedindo que você conserte algo.

As duas primeiras linhas da saída são o outro lado do apagamento. `nombres.getClass() == tiempos.getClass()` é `true` e o nome da classe é `java.util.ArrayList`, sem rastro do `<String>`: em tempo de execução, a lista não sabe do que é. Daí saem três limites que você vai encontrar na prática, e que convém saber desde já para não perder uma hora procurando-os: você não pode escrever `new T()` dentro de um método genérico, não pode perguntar `if (lista instanceof List<String>)` (só `List<?>`), e não pode sobrecarregar dois métodos que se distingam somente pelo tipo genérico (`void f(List<String>)` e `void f(List<Integer>)` colidem porque, apagados, são o mesmo método). O [tutorial oficial do Java sobre o apagamento](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html) lista as regras completas.

#### 4.2.4 Escrever um método genérico e ler uma assinatura

Quase nunca você vai *definir* uma classe genérica, mas vai ler as assinaturas das do JDK, e de vez em quando escreverá um método genérico. Veja um no `fig04_11`:

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

O `<T>` antes do tipo de retorno declara uma **variável de tipo**: “neste método, `T` é um tipo a determinar”. O compilador o deduz dos argumentos: na primeira chamada `T` é `String` e na segunda `Long`, e por isso o resultado pode ser guardado em um `String` e em um `Long` sem nenhuma conversão. Escrevê-lo com `Object` em vez de `T` obrigaria você a converter à mão.

E para ler a documentação, mais um símbolo: `?`, o **curinga** (*wildcard*). Você verá assinaturas como `List.copyOf(Collection<? extends E> coll)`. Lê-se “uma coleção de *algum tipo* que seja `E` ou uma subclasse de `E`”. A palavra `extends` aqui significa “é `E` ou herda dele”, e o curinga permite que você passe, por exemplo, uma lista de `Respondio` onde se espera uma coleção de `Respuesta`. Você não precisa escrever curingas ainda; precisa conseguir lê-los sem se assustar, porque aparecem em `Collectors`, em `Comparator` e em quase toda a biblioteca de *streams* que vem a seguir.

Com isso termina a primeira sessão. Antes de seguir, confira se você consegue responder sem olhar: que diferença há entre `List.copyOf` e `Collections.unmodifiableList`? Por que `Set.of` imprime em outra ordem cada vez? O que falha na compilação com `List<int>` e o que falha na execução com uma lista bruta? Se tiver dúvida em alguma, volte à sua figura antes de abrir a sessão dois.

### 4.3 Streams

Começa a segunda sessão. Até agora você guardou valores; agora vai transformá-los. A pergunta concreta do `revisor` é: dada a lista de resultados, quantos estão no ar, quantos lentos, quantos fora do ar, e qual é a média dos tempos dos que responderam? Com o que você sabe, pode escrever um `for` com quatro contadores. Os *streams* são a outra resposta, e para entendê-los primeiro é preciso entender a ferramenta com que se escrevem: as lambdas.

#### 4.3.1 Lambdas e referências a método

Uma **lambda** é uma função pequena, sem nome, que você escreve no lugar onde precisa dela e passa como se fosse um valor. Sua forma é `parâmetros -> corpo`. Você já viu a seta no `switch` da lição 2, mas aqui significa outra coisa: não “se o caso coincide, devolva isto”, mas “dado este parâmetro, calcule isto”. Estas são as formas que existem:

- `medicion -> medicion.tiempoMs() >= 1000`: um parâmetro, um corpo que é uma expressão (o resultado é o valor da expressão).
- `(a, b) -> a + b`: dois parâmetros, entre parênteses.
- `() -> "revisando..."`: nenhum, com parênteses vazios.
- `medicion -> { ...varias líneas...; return valor; }`: um bloco entre chaves, com `return` se deve devolver algo.

E qual é o *tipo* de uma lambda? Em Java, uma lambda é sempre atribuída a uma **interface funcional**: uma interface com um único método abstrato. A lambda é a implementação desse método. O JDK traz várias que se usam o tempo todo: `Predicate<T>` (um método, `test`, que recebe um `T` e devolve `boolean`), `Function<T, R>` (`apply`, recebe um `T`, devolve um `R`), `Supplier<T>` (`get`, não recebe nada e devolve um `T`) e `Comparator<T>` (`compare`, que recebe dois e devolve um inteiro). O programa `fig04_12` as usa uma por uma:

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

`esLenta` é um `Predicate<Medicion>`: passa-se uma medição a ele com `test` e responde `true` ou `false`. `etiqueta` é uma `Function<Medicion, String>`: converte uma medição em um texto. `saludo` é um `Supplier<String>`: não recebe nada e produz um valor. Quando você escreve uma lambda dentro de um *stream* não nomeia esses tipos; o compilador os deduz do método ao qual você a passa. Mas conhecê-los é o que permite ler a documentação: quando `filter` pede um `Predicate`, está pedindo “uma função que recebe um elemento e responde sim ou não”.

`soloNombre` mostra a segunda ferramenta: a **referência a método**. `Medicion::nombre` é a forma curta de `medicion -> medicion.nombre()`: “chame o método `nombre` do elemento que lhe derem”. Lê-se melhor quando a lambda só chama um método, e você a verá por toda parte (`Resultado::estado`, `Revision::revisar`, `Formato::linea`). Use a que se leia mais claramente; o compilador as trata igual.

Há uma regra das lambdas que surpreende quem chega do JavaScript ou do Python. Veja este programa, que tenta somar os tempos com uma lambda que modifica uma variável de fora:

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

O erro diz `local variables referenced from a lambda expression must be final or effectively final`: uma lambda só pode usar variáveis locais de fora se elas **nunca são reatribuídas** (são *efetivamente finais*). Aqui `suma` muda a cada volta, e o compilador rejeita. A razão é que uma lambda pode ser executada mais tarde, ou em outra thread, e o Java não quer que a lambda e o código de fora compartilhem uma variável que muda. Na lição 9 você verá por que é uma proteção valiosa. A saída para esse problema não é contornar a regra mas usar a ferramenta correta: `mapToLong(...).sum()`, que é justamente o que vem na próxima seção. Se você se pegar escrevendo uma lambda que modifica algo de fora, quase sempre é sinal de que falta um `collect` ou um `sum`.

#### 4.3.2 Um *stream* é uma tubulação

Um ***stream*** (fluxo) não é uma coleção: é uma **descrição de uma sequência de passos** que se aplicam aos elementos de uma fonte. Tem três partes. Uma **fonte**, que quase sempre é uma coleção e se obtém com `.stream()`. Zero ou mais **operações intermediárias** (`filter`, `map`, `sorted`...), que transformam o fluxo em outro fluxo. E uma **operação terminal** (`toList`, `count`, `sum`, `collect`...), que produz o resultado final e é a que de fato põe tudo em marcha. Os passos se escrevem encadeados, um por linha, e se leem de cima para baixo como uma receita. Veja o programa `fig04_14`, que faz o mesmo trabalho duas vezes: primeiro com um laço e depois com um *stream*.

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

O laço e o *stream* produzem a mesma lista (a última linha confere com `equals` que são iguais). O *stream* se lê assim: “das medições, **fique** com as que cumprem `tiempoMs() >= 100` (`filter`), **converta** cada uma em um texto (`map`) e **junte** o resultado em uma lista (`toList`)”. Repare no que não há: não há uma lista vazia que se preenche, nem uma condição envolta em um `if`, nem uma variável que muda. Quem lê descreve o que quer, não como fazê-lo.

As operações que você vai usar com mais frequência:

| Operação | Tipo | O que faz |
|---|---|---|
| `filter(Predicate)` | intermediária | deixa passar apenas os elementos que cumprem a condição |
| `map(Function)` | intermediária | transforma cada elemento em outro valor |
| `mapToLong(...)` | intermediária | transforma cada elemento em um `long` primitivo (sem wrappers) |
| `sorted(Comparator)` | intermediária | ordena (seção 4.3.5) |
| `toList()` | terminal | junta os elementos em uma lista **imutável** |
| `count()` | terminal | conta os elementos |
| `sum()`, `average()`, `max()` | terminal | agregam; existem sobre os fluxos de primitivos |
| `collect(Collector)` | terminal | junta os elementos com uma receita (seção 4.3.4) |

Duas precisões sobre a tabela. `mapToLong` produz um fluxo especializado (`LongStream`) que sabe somar e tirar a média sem embrulhar cada número em um `Long`; existem seus irmãos `IntStream` e `DoubleStream`. E `toList()` devolve uma lista **imutável**, como `List.of`: se mais adiante precisar dela modificável, copie-a com `new ArrayList<>(lista)`. A seção “O erro que você vai ver” mostrará a exceção que sai quando isso é esquecido.

#### 4.3.3 Preguiçosos e de uso único

Há duas propriedades dos *streams* que não se adivinham e que produzem erros se você as ignora. A primeira é que são **preguiçosos**: montar o fluxo não faz nada. Segundo a [documentação do pacote `java.util.stream`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html), as operações intermediárias “são sempre preguiçosas”: `filter` não filtra nada quando você o chama, só cria um novo fluxo que filtrará quando alguém o percorrer. O trabalho ocorre quando chega a operação terminal. O programa `fig04_15` torna isso visível com `peek`, uma operação que deixa passar cada elemento e de passagem executa uma ação (sua documentação diz que existe “principalmente para depurar”, e só para isso a usamos aqui):

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

Veja a ordem. “el stream está armado; todavía no pasó nada” (“o stream está montado; ainda não aconteceu nada”) sai **primeiro**, antes de qualquer “miro” (“olho”). Os `peek` e o `filter` não executaram nem uma vez quando o fluxo foi montado: esperaram por `toList()`. E quando por fim rodam, não o fazem passo a passo sobre toda a lista, mas **elemento por elemento**: `catalogo` passa por `peek`, `filter`, `peek` e `map` antes que `inventario` entre no fluxo, e `pagos`, que não cumpre o filtro, fica no primeiro `peek`. Isso tem consequências práticas: um fluxo sem operação terminal é código morto (o compilador não avisa), e uma operação intermediária com efeito colateral, como imprimir, é executada em uma ordem que não é a de “primeiro todos, depois todos”.

A segunda propriedade é que um fluxo só pode ser percorrido **uma única vez**. A [documentação de `Stream`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Stream.html) diz que “um fluxo deve ser operado somente uma vez”. O que acontece se você a ignora:

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

A primeira chamada a `count()` funciona (3) e consome o fluxo. A segunda lança `IllegalStateException: stream has already been operated upon or closed` (“o fluxo já foi operado ou fechado”). A correção é criar um fluxo novo cada vez que você precisar, a partir da coleção: `nombres.stream()` é barato. Por isso as variáveis do curso guardam a **lista**, e o fluxo é montado e consumido na mesma expressão.

#### 4.3.4 `Collectors`: juntar o resultado de formas diferentes

`toList()` cobre um caso. Para os demais existe `collect`, que recebe um **coletor** (*collector*): uma receita para juntar os elementos. A classe `Collectors` traz as receitas mais comuns. O programa `fig04_17` usa as de que o resumo do `revisor` precisa:

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

A primeira é a mais importante: `groupingBy(Medicion::estado, TreeMap::new, Collectors.counting())`. `groupingBy` **agrupa** os elementos pelo valor que a função devolve (aqui, o estado) e a cada grupo aplica outro coletor (aqui, `counting()`, que conta). O resultado é um mapa de estado a quantidade: `{ARRIBA=2, CAIDO=1, LENTO=1}`. Sem o segundo argumento, `groupingBy` devolveria um `HashMap` de ordem não prometida; com `TreeMap::new` (uma referência a construtor: “chame `new TreeMap()` quando precisar criar o mapa”) o resultado sai ordenado alfabeticamente. Isso se parece com a coluna de “quantos há de cada estado” do resumo do `revisor`.

A segunda receita agrupa o mesmo mas, em vez de contar, **guarda os nomes** de cada grupo: `Collectors.mapping(Medicion::nombre, Collectors.toList())` transforma cada elemento antes de juntá-lo. É o padrão “agrupe e depois reduza cada grupo” que você verá com diferentes redutores. `Collectors.joining(", ")` junta textos com um separador; é o que monta o corpo da tabela na seção 4.5.

A última parte da figura é a que mais ensina. `average()` calcula a média de um fluxo de `long` e devolve um `OptionalDouble`, não um `double`. É impresso como `OptionalDouble[843.0]`, e `getAsDouble()` tira o número. Quando o fluxo está vazio (o último caso, em que o filtro não deixa ninguém passar), a média **não existe** e o resultado é `OptionalDouble.empty`. É uma decisão inteligente: outras linguagens devolveriam zero ou `NaN` (um número que significa “não é um número”), e um zero mentiria, porque a média de nada não é zero. O Java obriga você a decidir o que fazer quando não há dados. A seção 4.4 explica o `Optional` por completo.

Um detalhe de `Math.round(843.0)`: arredonda um `double` ao inteiro mais próximo e devolve um `long`. (Se você quer a média sem arredondar, deixe o `double`.) Para o `revisor`, que mostra milissegundos inteiros, o arredondamento é o correto.

#### 4.3.5 Ordenar: `Comparator`

Ordenar requer saber **comparar** dois elementos. Para os textos e os números há uma ordem natural (a alfabética, a numérica) e `sorted()` sem argumentos a usa. Para os seus próprios tipos, você diz como comparar com um `Comparator`, e o JDK traz uma caixa de ferramentas para montá-los sem escrever o método `compare` à mão. O programa `fig04_18` as exercita com quatro medições, duas delas com o mesmo tempo para que se veja o desempate:

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

O que se leu na saída:

- `Comparator.comparingLong(Medicion::tiempoMs)` constrói um comparador a partir de uma função que extrai o valor a comparar. Há versões para `comparing` (qualquer tipo com ordem natural), `comparingInt`, `comparingLong` e `comparingDouble`.
- `.reversed()` inverte a ordem. `.thenComparing(...)` acrescenta um **desempate**: é usado somente quando os dois elementos empatam no primeiro critério. Por isso, no terceiro bloco, `busqueda` fica antes de `catalogo`: têm os mesmos 142 ms e o desempate os ordena alfabeticamente.
- Sem desempate, nos dois primeiros blocos `catalogo` fica antes de `busqueda` nos dois sentidos. Não é casualidade: a [documentação de `Stream.sorted`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Stream.html) garante que “para fluxos ordenados, a ordenação é estável”: os elementos que empatam conservam a ordem que traziam.
- **`sorted` não toca a lista original.** As últimas linhas provam isso: `mediciones` continua na ordem em que foi criada. Se você quer ordenar a própria lista, `lista.sort(comparador)` faz isso no lugar, mas só funciona com uma lista modificável: sobre uma `List.of` lança de novo `UnsupportedOperationException`.
- `max(porTiempo)` devolve um `Optional<Medicion>` e não uma `Medicion`, e é impresso como `Optional[Medicion[nombre=inventario, tiempoMs=2300]]`. Perguntar pelo máximo de uma lista vazia não tem resposta, e o tipo diz isso.

Um `forEach` apareceu duas vezes no programa: `ascendente.forEach(medicion -> IO.println(...))`. Recebe uma lambda que não devolve nada (um `Consumer`, a interface funcional do “faça isto com cada elemento”) e está disponível em listas e em fluxos. Para imprimir está bem; para preencher outra coleção, não (ver “O que se faz errado”).

#### 4.3.6 Fluxo ou laço?

A mensagem errada sobre os *streams* é que “são melhores”. Não são: são outra ferramenta. Prefira um **fluxo** quando você transforma ou resume uma coleção (filtrar, converter, contar, agrupar, ordenar), porque o código diz o que se quer. Prefira um **laço** quando precisa sair antes com `break`, controlar um índice, modificar algo de fora, ou quando o corpo pode lançar uma exceção verificada (a lição 5 explica por que uma lambda não pode, e por isso `revisarTodos` voltará a ser um `for` nesse momento: não é um retrocesso, é a ferramenta adequada). E uma regra de leitura que poupa discussões: se um fluxo já não cabe em seis ou sete linhas, ou se tem uma lambda de várias linhas, extraia essa lambda para um método com nome e use a referência (`Revision::revisar` se lê melhor que cinco linhas dentro de um `map`).

### 4.4 `Optional`: um valor que pode não estar

O `revisor` tem um problema de representação que vem se repetindo: o que uma função devolve quando não há valor? `Map.get("reportes")` devolvia `null`; um serviço fora do ar não tem tempo de resposta; a média de ninguém não existe. A lição 2 mostrou o custo do `null`: o compilador não o distingue de um valor, e o erro aparece longe, como uma `NullPointerException`. `Optional<T>` é a resposta do JDK: uma caixinha que **tem um valor ou está vazia**, e cujo tipo avisa a quem a recebe que precisa decidir o que fazer nos dois casos.

O programa `fig04_19` percorre o vocabulário:

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

As três formas de **criar** um `Optional`: `Optional.of(valor)` (o valor não pode ser `null`; se for, lança `NullPointerException`), `Optional.ofNullable(valor)` (se o valor é `null`, fica vazio; é a que `tiempoDe` usa para adaptar um `get` que podia devolver `null`) e `Optional.empty()`. As formas de **consumi-lo**; a última é a que convém evitar:

- `orElse(padrão)` devolve o valor, ou o que você der se estiver vazio. `orElseGet(fornecedor)` faz o mesmo mas recebe uma lambda que só é executada se for preciso. `orElseThrow()` lança uma exceção se estiver vazio (a lição 5 explica quais).
- `map(função)` transforma o valor se existe e deixa o vazio como está: `hay.map(tiempo -> tiempo + " ms")` produz um `Optional<String>` com “87 ms” dentro, que `orElse` desembrulha; com o vazio, `map` não faz nada, o resultado continua vazio e `orElse` dá “sin tiempo” (“sem tempo”). É o que evita escrever `if (x != null)` por toda parte.
- `ifPresent(ação)` executa a lambda somente se há valor; `isPresent()` e `isEmpty()` perguntam.
- `get()` tira o valor, **e lança uma exceção se não há**. Existe, mas é a porta de entrada para o mesmo problema que o `Optional` queria resolver.

Vejamos o custo de `get()` sem verificar:

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

`NoSuchElementException: No value present` (“não há valor”). O erro é honesto e é o mesmo que a `NullPointerException` de sempre, com outro nome: alguém assumiu que havia valor. Agora, com a diferença de que o tipo `Optional` tornou evidente o risco no ponto exato onde a decisão foi tomada. A regra prática: **não chame `get()` a menos que tenha verificado no mesmo bloco**, e quase sempre há um `orElse` ou um `map` que diz o mesmo melhor.

Também existe `orElse` com um detalhe que custa caro: seu argumento é calculado **sempre**, ainda que não faça falta. O programa `fig04_21` mostra isso com um valor padrão que avisa quando é calculado:

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

Com o valor presente, `orElse` calculou o valor padrão de qualquer jeito (a mensagem “calculando...” saiu) e o jogou fora; `orElseGet` não o calculou. Para um `"ninguno"` literal tanto faz; para algo caro, como uma consulta ou um objeto grande, use `orElseGet`. É a mesma ideia da lição 2 com `&&` contra `&`: não avalie o que você não precisa.

E a última parte, a que traz regras de projeto. A documentação de [`Optional`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html) diz, em uma nota de API, que “foi pensado principalmente para ser usado como tipo de retorno de um método onde há uma necessidade clara de representar *nenhum resultado*, e onde usar `null` provavelmente cause erros”. Daí saem três regras:

1. **Use-o como tipo de retorno**, não como parâmetro (obriga quem chama a embrulhar um valor que já tem) nem como lista de elementos (`List<Optional<T>>` quase sempre é um `filter` que falta).
2. **Uma variável `Optional` jamais deve ser `null`**: a documentação diz isso com essas palavras. Um `null` onde se esperava um `Optional` é pior que qualquer um dos dois por separado.
3. **Para primitivos, `OptionalInt`, `OptionalLong` e `OptionalDouble`**: sem wrappers, com `getAsLong()` em vez de `get()`. São usados por `average()` e `max()` dos fluxos numéricos, e pelo `revisor`, para o tempo.

O projeto quebra a regra de “não como campo” em um lugar, e convém dizer de frente: o *record* `Resumen` da seção 4.5 guarda um `OptionalLong promedioMs`. É um *record* imutável, que se cria uma vez e se lê várias; o valor ausente faz parte dos dados (quando ninguém respondeu, não há média), e as alternativas são piores: um `Long` que pode ser `null` é o problema que acabamos de deixar para trás, e um `0` mentiria. É uma exceção consciente a uma diretriz, não um descuido. Na lição 8 você verá como esse valor ausente cruza a fronteira rumo ao JSON, onde o formato, aí sim, pede um `null`.

### 4.5 O passo do projeto: o relatório com resumo

Agora é juntar tudo. A lição 3 deixou um `Revisor.java` que verificava quatro serviços escritos à mão e um `simular` com um único `switch`, que reunia os dados dos quatro serviços. Hoje o projeto muda assim:

- Os **dados simulados** saem do `switch` e vão para um mapa em uma classe nova, `Simulador`. Busca-se por nome e devolve um `Optional`.
- **`Revision`** verifica um serviço (consultando o simulador) e verifica uma lista inteira com um *stream*.
- **`Resumen`** conta e tira a média com `groupingBy` e `average`; **`Reporte`** junta os resultados e seu resumo, com uma cópia defensiva.
- **`Resultado`** aprende a dar seu tempo como `OptionalLong`; **`Formato`** aprende a montar o resumo e a tabela completa.
- **`Revisor.java`** deixa de chamar `revisar` quatro vezes: tem uma lista e pede o relatório de uma vez.

Quatro arquivos não mudam: `Estado`, `Respuesta`, `Servicio` e `Clasificador`. Incluo-os igual porque a pasta da figura é o projeto **completo**, e assim você pode rodar tudo a partir dela. Copie-os tal qual da lição 3. Comece por eles:

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

Uma nota sobre `import`. Os quatro arquivos de cima e os que seguem são classes normais (não arquivos compactos), e por isso começam, quando precisam, com linhas `import java.util.List;`. Um `import` diz ao compilador de onde vem um nome que não é de `java.lang` (o pacote que o Java importa sozinho, com `String`, `Math` e `Thread`). Escreve-se um por classe, sem curingas, como indica o guia de estilo do Google que o curso segue desde a lição 1. O arquivo compacto `Revisor.java`, em compensação, importa sozinho todo o `java.base` e não precisa de nenhum.

Segue `Resultado`, que ganha um método. `tiempoMs()` devolve um `OptionalLong`: com valor quando houve resposta e vazio quando não. É o mesmo `switch` exaustivo da lição 3: se algum dia alguém acrescentar um terceiro tipo de `Respuesta`, o compilador marcará este método e `detalle()` como incompletos.

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

`Simulador` é a classe que faz de rede até a lição 8. Seu `RESPUESTAS` é um `Map` imutável (`Map.of`) de nome a resposta, com as três que respondem e a que não responde. `buscar` embrulha o `get` do mapa em um `Optional.ofNullable`, e é o único ponto onde a ausência de um nome se converte em um tipo que obriga a decidir.

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

`Revision` tem os dois métodos que produzem resultados. `revisar` pede a resposta ao simulador e, se o nome não está, usa um `SinRespuesta("sin conexión")` (“sem conexão”) (um `orElse` com valor barato). `revisarTodos` é a linha que justifica toda a lição: um *stream* sobre a lista de serviços, com `Revision::revisar` como `map` e `toList()` no final.

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

`Resumen` é onde se juntam os *streams* e os coletores. `groupingBy` conta quantos há de cada estado e `average` calcula a média dos tempos dos que responderam. Dois detalhes. Um: `groupingBy` só cria uma chave para os estados que **aparecem**; se nenhum serviço está fora do ar, `porEstado.get(Estado.CAIDO)` seria `null`. Por isso se usa `getOrDefault(..., 0L)`: sem ele, o primeiro relatório sem serviços fora do ar derrubaria o programa com a `NullPointerException` da seção 4.2.2. `intValue()` converte o `Long` da contagem no `int` que o *record* pede (as contagens cabem de sobra). Dois: a média conta todos os que responderam, seja qual for o seu código (um 503 respondeu, ainda que tenha respondido mal), e por isso é calculada sobre `tiempoMs()` e não sobre o estado.

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

`Reporte` é o *record* da cópia defensiva da seção 4.1.4. Sua fábrica `de` junta a lista de resultados com seu resumo:

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

`Formato` cresce com três métodos: `contar`, que põe uma palavra no plural (a única regra de plural do curso: acrescenta-se um “s” se a quantidade não é 1), `resumen`, que monta a linha do final, e `tabla`, que junta o cabeçalho, as linhas (unidas com `Collectors.joining("\n")`), uma linha em branco e o resumo. O `? :` de `contar` é o **operador condicional** da lição 2 (2.3.1): `cantidad == 1 ? "" : "s"` se lê “se a quantidade é 1, nada; se não, um "s"”. (O mesmo operador aparece em `Resumen`, para decidir se há média.) E há uma pequena mudança em um método que você já tinha, que convém dizer em voz alta: `formatearTiempo` deixa de comparar contra `Clasificador.UMBRAL_LENTO_MS` e compara contra `1000`. São duas ideias diferentes que hoje valem o mesmo: o limite de “lento” é uma regra do negócio, e a passagem de milissegundos a segundos é uma regra de unidades. Se amanhã o limite de lento baixar para 800 ms, a tabela tem de continuar escrevendo `999ms` e não `1.0s`; com a constante compartilhada, mudar uma regra teria mudado a outra sem que ninguém pedisse. (A lição 7 faz justamente essa mudança de limite, e um teste a vigia.) O resto do arquivo, `PATRON_LINEA`, `tiempo`, `encabezado` e `linea`, fica como na lição 3.

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

E o `Revisor.java` fica curto. Tem duas novidades que se explicam sozinhas: uma **lista** de serviços (um campo do arquivo compacto, ou seja, uma variável que está fora de qualquer método e que todos os métodos do arquivo podem usar) marcada com `final`, que significa “esta variável não pode ser reatribuída”, e um `main` de uma única linha:

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

Rode o projeto a partir da sua pasta:

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

Essa é a tabela da lição 3, com o que lhe faltava: a linha em branco e o resumo. Veja como o resumo se lê diante do código: `4 servicios` é `contar(4, "servicio")`, `2 arriba` é a contagem de `ARRIBA`, `1 lento` e `1 caído` são `contar(...)` com suas palavras, e `843ms` é o `Math.round` de 2529 / 3, formatado com `formatearTiempo` (que já sabe que 843 < 1000 se escreve com “ms”). Quando o resumo lhe der um número que você não espera, comece pelo `Resumen`: ali se calcula tudo.

## O erro que você vai ver

O erro desta lição é `UnsupportedOperationException`, e é o que quase todos encontram na primeira semana com coleções. Você já o viu com `List.of`. Este é o mesmo stack trace, para que você o leia linha por linha:

```text
Exception in thread "main" java.lang.UnsupportedOperationException
	at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
	at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
	at fig04_04.main(fig04_04.java:5)
```

**Primeira linha: o quê.** `UnsupportedOperationException` é o tipo; não traz mensagem, porque a operação não é suportada e não há mais o que dizer. **Segunda e terceira: onde, dentro do JDK.** Falam de `ImmutableCollections`, a classe que implementa as coleções de `List.of`, `Set.of` e `Map.of` (e de `toList()`). Quando você vir esse nome, já sabe que tentou modificar uma coleção imutável. **Quarta: onde, no seu código.** A primeira linha que menciona o seu arquivo é a que você deve olhar.

O mesmo erro aparece por outros caminhos que convém reconhecer. Ocorre quando você modifica o que `toList()` de um fluxo devolveu:

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

O stack trace é idêntico, com outro número de linha do seu programa. E ocorre quando você chama `sort` sobre uma lista imutável. Há duas soluções, conforme o que você queira. Se de verdade precisa de uma lista modificável, torne-o explícito com uma cópia: `new ArrayList<>(nombres)`. Se o que você queria era uma lista ordenada, não a ordene no lugar: use `stream().sorted(...).toList()` e receba outra lista. **A melhor reação ao erro não é tornar a lista modificável por reflexo, mas perguntar-se se a operação que você tenta devia existir.**

## O que se faz errado

- **Modificar uma lista enquanto a percorre com `for`.** É o erro clássico, e a armadilha é que às vezes nem sequer falha. Se você remove um elemento dentro do `for` aprimorado, na maioria das vezes o programa para com uma exceção:

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

  `ConcurrentModificationException` (“modificação concorrente”; o nome engana, porque não há threads no meio: a lista detectou que mudou enquanto o percurso seguia em andamento). O pior é o caso silencioso: medi que se o elemento que você remove é o **penúltimo**, o percurso termina sem reclamar e pula o último. Com `["catalogo", "inventario", "pagos"]` removendo `"inventario"`, a saída é `[catalogo, pagos]` e o programa nunca olhou para `pagos`. O custo é um programa que às vezes perde um elemento e não diz. A solução: `lista.removeIf(nombre -> nombre.startsWith("i"))`, que existe justamente para isso, ou um fluxo com `filter` que produza uma lista nova.

- **Confundir `remove(int)` com `remove(Object)`.** Em uma `List<Integer>`, `lista.remove(87)` interpreta o 87 como uma **posição**, não como um valor.

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

  A primeira chamada (`Integer.valueOf(87)`, que pede para remover o valor) funcionou; a segunda procurou a posição 87 em uma lista de dois elementos e lançou `IndexOutOfBoundsException`. É um caso em que a comodidade do *autoboxing* da seção 4.2.2 se volta contra você: o compilador escolhe o método `remove(int)` porque `87` é um `int`. Com listas de textos não há ambiguidade. Quando precisar remover um número pelo seu valor, embrulhe-o de propósito, como na primeira chamada.

- **Usar tipos brutos (`List` sem `<...>`).** Como mostrou a figura do apagamento, tira do compilador a capacidade de avisar você, e o erro aparece longe de sua causa, como `ClassCastException`. Custo: depurar um erro de tipo na execução, que o compilador teria marcado na linha do descuido. Qualquer aviso `[unchecked]` é isto.

- **Chamar `Optional.get()` sem verificar**, ou declarar um `Optional` como campo ou parâmetro. A documentação desaconselha, e as duas práticas devolvem os problemas do `null` com outro nome. Custo: `NoSuchElementException` em produção, e assinaturas que obrigam quem chama a embrulhar e desembrulhar valores sem necessidade.

- **Preencher uma coleção de fora a partir de um `forEach` ou de um `map`.** Escrever `mediciones.stream().forEach(m -> destino.add(m.nombre()))` modifica um estado externo a partir de uma lambda; funciona com um fluxo sequencial e quebra de maneiras sutis com um paralelo. Custo: código frágil que parece um `map` mal escrito; a ferramenta correta é `collect` ou `toList()`.

- **Dar por certa a ordem de um `HashMap`, um `HashSet` ou um `Set.of`.** Funciona na sua máquina e muda em outra, ou na execução seguinte. Custo: um relatório que às vezes sai em outra ordem e que nenhum teste reproduz de primeira. Se a ordem importa para quem lê, escolha `LinkedHashMap`, `TreeMap` ou uma lista.

- **Devolver ou guardar a lista interna sem copiá-la.** É a armadilha da seção 4.1.4: outro código conserva a referência e modifica os seus dados sem que você perceba. Custo: valores que mudam sozinhos, o tipo de erro mais difícil de localizar porque ocorre longe de onde se vê o efeito.

## Exercícios

### Exercício 1 — Acrescente dois serviços ao projeto

No seu `Revisor.java`, acrescente à lista `facturacion` (`https://facturacion.example`) e `notificaciones` (`https://notificaciones.example`). **Antes de rodar**, preveja que estado terão e qual será o resumo. Depois rode. Em seguida acrescente os dois ao `Simulador` (`facturacion` responde com o código 503 em 95 ms; `notificaciones` não responde, com o motivo “tiempo agotado” (“tempo esgotado”)), preveja de novo o resumo e rode. Anote no seu diário de bordo o que aconteceu com a linha de `notificaciones`.

### Exercício 2 — Ordene o relatório

Faça a tabela sair ordenada do serviço mais lento ao mais rápido, com os que não responderam no final, e que os empates sejam desempatados pelo nome. Dica: `Comparator.comparingLong`, `reversed` e `thenComparing`; o tempo de quem não respondeu pode ser substituído por `orElse(-1)`. Passe a lista já ordenada para `Reporte.de`.

### Exercício 3 — Qual delas lança uma exceção?

Para cada linha, decida sem executar se lança uma exceção ou não, e qual. Depois confira em um arquivo compacto, executando cada linha sozinha (uma por execução: a primeira exceção interrompe o programa):

1. `List.of("catalogo", null)`
2. `Map.of("a", 1, "a", 2)`
3. `new ArrayList<>(List.of("a")).get(5)`
4. `Set.of("a").add("b")`
5. `new HashMap<String, Integer>().put(null, 1)`
6. `new TreeMap<String, Integer>().put(null, 1)`

### Exercício 4 — Os nomes por estado

Imprima, a partir dos resultados do `revisor`, um mapa de cada `Estado` à lista dos nomes de serviço que têm esse estado, **na ordem em que os estados são declarados** (`ARRIBA`, `LENTO`, `CAIDO`). Dica: `groupingBy` aceita como segundo argumento uma função que fabrica o mapa, e `EnumMap` é um mapa especializado para chaves de um `enum` que as ordena pela sua declaração.

## Soluções

### Solução 1

Sem tocar no `Simulador`, os dois serviços novos não estão no mapa de respostas, então `buscar` devolve um `Optional` vazio e `revisar` usa o `SinRespuesta("sin conexión")` padrão: os dois saem `CAIDO`. Com a mudança em `Revisor.java` (acrescentar dois `new Servicio(...)` à lista), a tabela é:

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

São 6 serviços, 2 no ar, 1 lento e 3 fora do ar; a média não muda (843 ms) porque nenhum dos dois novos responde. Depois, no `Simulador`, acrescentam-se dois pares ao `Map.of` (que aceita até dez; para mais seria preciso usar `Map.ofEntries`):

```java
"facturacion", new Respuesta.Respondio(503, 95),
"notificaciones", new Respuesta.SinRespuesta("tiempo agotado"));
```

e o resultado é:

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

`facturacion` é `CAIDO` porque seu código é 503 (≥ 400), mas **respondeu, sim**: tem tempo (95 ms) e conta para a média. A média agora é (142 + 2300 + 87 + 95) / 4 = 656. É a diferença entre “não respondeu” e “respondeu mal”, e a razão de `Resumen` tirar a média sobre `tiempoMs()` e não sobre o estado.

Na linha de `notificaciones` há um defeito visível: o nome mede 14 caracteres e a coluna, 12. `%-12s` não corta, então o estado fica colado ao nome (`notificacionesCAIDO`). O formato preenche mas não inventa espaço. Calcular a largura da coluna a partir do nome mais longo é um exercício opcional da lição 10; por ora, você já sabe que um formato fixo é um contrato que os dados podem quebrar.

### Solução 2

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

Repare no tipo escrito à mão na lambda: `(Resultado resultado) -> ...`. Não é enfeite. Se você o omitir e escrever `Comparator.comparingLong(resultado -> resultado.tiempoMs().orElse(-1)).reversed()`, o compilador não consegue deduzir o tipo de `resultado` (porque `reversed()` é encadeado antes que ele saiba a que será atribuído) e o toma como `Object`:

```text
Revisor.java:4: error: cannot find symbol
      Comparator.comparingLong(resultado -> resultado.tiempoMs().orElse(-1)).reversed();
                                                     ^
  symbol:   method tiempoMs()
  location: variable resultado of type Object
```

É um erro de inferência conhecido: o compilador deduz os tipos de uma lambda olhando para onde ela vai, e uma cadeia de métodos esconde o destino dele. Com o tipo explícito no primeiro comparador, a cadeia funciona. O `thenComparing` final não precisa de tipo porque já sabe que é sobre `Resultado`. Os serviços que não responderam (`reportes`) ficam no final porque seu `-1` é o menor e a ordem é descendente. Com os empates entre dois que não responderam, `thenComparing` os desempata pelo nome.

### Solução 3

| Linha | O que acontece |
|---|---|
| `List.of("catalogo", null)` | `NullPointerException`: as coleções imutáveis não admitem `null` |
| `Map.of("a", 1, "a", 2)` | `IllegalArgumentException: duplicate key: a` |
| `new ArrayList<>(List.of("a")).get(5)` | `IndexOutOfBoundsException: Index 5 out of bounds for length 1` |
| `Set.of("a").add("b")` | `UnsupportedOperationException` |
| `new HashMap<String, Integer>().put(null, 1)` | não lança nada: um `HashMap` aceita uma chave `null` e fica `{null=1}` |
| `new TreeMap<String, Integer>().put(null, 1)` | `NullPointerException`: um `TreeMap` precisa comparar as chaves e não consegue com `null` |

Medi as seis com o JDK 25. Duas conclusões úteis. As coleções imutáveis são estritas desde o momento de criá-las, não ao usá-las: `List.of("catalogo", null)` já falha na criação. E que `HashMap` aceite `null` como chave e `TreeMap` não é um exemplo de que as implementações de uma mesma interface **não são intercambiáveis em tudo**: o contrato comum (`Map`) cobre o básico, e os detalhes de cada uma são ditos pela sua documentação.

### Solução 4

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

`groupingBy` com três argumentos: a função que classifica (`Resultado::estado`), a fábrica do mapa (`() -> new EnumMap<>(Estado.class)`, uma lambda que não recebe nada e devolve um mapa novo) e o coletor de cada grupo (`mapping` extrai o nome e `toList` os junta). `EnumMap` recebe a classe do `enum` (`Estado.class`) e percorre suas chaves na ordem da declaração: `ARRIBA`, `LENTO`, `CAIDO`. Com um `HashMap` a ordem não estaria prometida. `EnumMap` é feito para chaves de um `enum` e deixa claro, a quem lê, que são.

## Como sei que consegui

- [ ] `java fig04_01.java` imprime a lista, `primero y último: catalogo, pagos` (“primeiro e último: catalogo, pagos”), e você consegue dizer por que `remove("pagos")` remove um valor e não uma posição.
- [ ] `java fig04_04.java` termina com `UnsupportedOperationException`, e você consegue apontar no stack trace qual linha é do seu programa e quais são do JDK.
- [ ] `java fig04_05.java` imprime que o relatório ingênuo mudou e o seguro não, e você sabe explicar por que o segundo se protege com `List.copyOf`.
- [ ] `java fig04_10.java` falha com uma `ClassCastException` em uma linha que não é a que cometeu o erro, e você sabe o que tornou isso possível (um tipo bruto).
- [ ] `java fig04_15.java` imprime “el stream está armado; todavía no pasó nada” antes de qualquer “miro”, e você consegue explicar por quê.
- [ ] A partir da pasta do projeto, `java Revisor.java` imprime a tabela de quatro serviços com `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`.
- [ ] Com os dois serviços do exercício 1 no `Simulador`, o resumo diz `6 servicios · 2 arriba · 1 lento · 3 caídos · promedio 656ms`, e você sabe por que `facturacion` conta para a média ainda que esteja fora do ar.
- [ ] Você consegue explicar por que `Resumen.de` usa `getOrDefault(..., 0L)` e o que aconteceria sem ele.
- [ ] Você anotou no seu diário de bordo um *stream* que reescreveu como laço, ou o contrário, e qual se lia melhor.

## Resumo

Responda sem olhar a lição:

1. Por que se declara `List<String> x = new ArrayList<>()` e não `ArrayList<String> x = ...`?
2. Que ordem de percurso têm um `HashMap`, um `LinkedHashMap` e um `TreeMap`? Qual você usaria para um relatório?
3. Que duas coisas devolvem `Set.add` e `Map.put`, e para que servem?
4. O que lança `List.of("a").add("b")`? E `List.of("a", null)`?
5. Que diferença há entre `List.copyOf(x)` e `Collections.unmodifiableList(x)`?
6. Por que não existe `List<int>`? Que armadilha traz comparar dois `Integer` com `==`?
7. O que significa os genéricos serem “apagados”? Dê uma consequência.
8. Que três partes tem um *stream*? Quando o trabalho é executado?
9. O que acontece se você percorre o mesmo *stream* duas vezes?
10. O que `average()` devolve e por que não um `double`?
11. Quando você prefere um laço a um *stream*?
12. Por que não se chama `get()` sem verificar em um `Optional`? O que você usa no lugar?

## Para ler mais

- [Oracle, “Java Collections Framework”](https://docs.oracle.com/en/java/javase/25/core/java-collections-framework.html): o guia oficial das interfaces e das implementações, com a tabela de qual usar quando. Consultado em 7 de outubro de 2026.
- [Oracle, “Generics” (tutorial do Java)](https://docs.oracle.com/javase/tutorial/java/generics/index.html): dos tipos genéricos aos curingas e ao apagamento, com exemplos mais extensos que esta lição. Consultado em 7 de outubro de 2026.
- [Oracle, “Stream API”](https://docs.oracle.com/en/java/javase/25/core/stream-api.html): o guia dos fluxos, com a diferença entre operações intermediárias e terminais e os coletores. Consultado em 7 de outubro de 2026.
- [Documentação de `Optional`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html): inclui a nota de API sobre quando usá-lo. Consultado em 7 de outubro de 2026.

---

**Anterior:** [Lição 3 — Records, enums e tipos selados](03-records-enums-sellados.md) ·
**Próxima:** [Lição 5 — Erros e exceções](05-errores-excepciones.md)
