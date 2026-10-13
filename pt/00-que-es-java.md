# Lição 0 — O que é Java e o que NÃO é

**Tempo:** 90 min

**O que você constrói:** nada ainda. É uma lição de leitura: aqui se monta o mapa mental com o qual o resto do curso se torna previsível

**O que você aprende:** o caminho do código-fonte até o programa que roda (`javac`, *bytecode*, a JVM); o que é o JDK e o que foi o JRE; o que significa LTS e por que o curso usa o Java 25; o que é um recurso em versão prévia (*preview*); e o que o compilador do Java protege e o que deixa passar (os tipos são verificados na compilação, mas os genéricos são apagados e `null` continua existindo)

**De onde você vem.** Você não traz nada deste curso: não instalou o Java nem escreveu uma linha. Traz, sim, o que já sabe de outra linguagem —Go, TypeScript, Python ou qualquer outra—: o que é uma variável, um laço, uma função, um erro. Isso é justamente o que o curso dá por sabido. O que você não traz é o mapa de **como funciona a plataforma do Java**, que é diferente da de qualquer uma dessas linguagens e que explica por que os comandos, as mensagens de erro e os costumes do Java são como são.

**O que esta lição não faz.** Não instala nada (isso é a [Lição 1](01-instalacion.md)) e não ensina a sintaxe do Java (isso começa na [Lição 2](02-tipos-cadenas-control.md)). Traz quatro programas pequenos, de quatro a oito linhas, só para que você veja com os próprios olhos o que a lição afirma. Não é preciso entendê-los linha por linha ainda, embora cada um seja explicado: o que importa é o que eles **imprimem**. Eles são executados depois da Lição 1, quando você já tiver o Java.

**Sobre as execuções.** As saídas desta lição foram obtidas com o Temurin 25.0.4 (uma distribuição gratuita do Java, explicada mais abaixo) no macOS, com a configuração regional em UTF-8. No seu Linux Mint elas saem iguais; a única coisa que muda, e isso é avisado onde ocorre, são as linhas que mencionam o sistema. Os programas estão em [`programas/00-que-es-java/`](../programas/00-que-es-java), no repositório do curso.

## Ao terminar, você vai conseguir

- Descrever com suas palavras o caminho de um arquivo `.java` até rodar: o que o `javac` faz, o que um `.class` contém e o que a JVM faz com ele.
- Distinguir o JDK, o JRE e a JVM, e dizer qual instalar e por quê.
- Explicar o que significa o Java 25 ser LTS, por que o Java 26 e o Java 27 existem mas o curso não os usa, e o que é um recurso em versão prévia.
- Dizer, diante de uma possível falha, se o compilador do Java a pega antes de executar ou se ela só aparece quando o programa roda.
- Explicar o que é o apagamento de genéricos e por que `null` continua existindo em uma linguagem com tipos.
- Prever o que `"142" + 1` imprime em Java e por que não é uma soma.

## O porquê antes do como

Imagine que alguém lhe peça o programa do curso, o `revisor`: ele lê uma lista de serviços, consulta todos ao mesmo tempo e entrega um relatório. Em Go você o faria e obteria um arquivo executável que você copia para um servidor e ele roda. Em Python, um script que roda desde que o interpretador esteja instalado. Em TypeScript, código que o Node executa depois que o compilador o traduz para JavaScript. Três linguagens, três respostas à mesma pergunta: **o que é, exatamente, aquilo que se entrega e quem o faz rodar?**

O Java tem sua própria resposta, e é a primeira coisa que é preciso entender, porque todo o resto depende dela. No Java você escreve arquivos `.java`, um programa chamado `javac` os traduz para arquivos `.class`, e outro programa chamado `java` os executa dentro de uma máquina virtual. Dois programas, dois formatos de arquivo e uma máquina virtual: se você não sabe o que cada um faz, a instalação da Lição 1 é uma lista de comandos que se obedece às cegas, e os erros da Lição 2 são mensagens de origem misteriosa.

Além disso, há uma razão prática. Na bibliografia e nos tutoriais de Java convivem conselhos de vinte anos atrás com os de hoje, e quase nunca dizem de que ano são. Quem chega sem o mapa não consegue distinguir “isto era assim em 2008” de “isto é assim em 2026”. Com o mapa desta lição, consegue.

A lição tem cinco partes, nesta ordem:

1. **O caminho do código** (0.1): de `.java` a `.class` até a JVM.
2. **As peças da plataforma** (0.2): o JDK, o JRE e as distribuições.
3. **As versões** (0.3): o que é LTS, o que é a versão prévia e por que a 25.
4. **O que o compilador protege e o que não** (0.4): a parte mais útil da lição, com quatro programas que são executados de verdade.
5. **Java diante do que você já conhece** (0.5) e **o que vem no resto do curso** (0.6).

Com esse destino em mente, isto é o que você verá nesta lição, na seção 0.4.1, quando executar o primeiro programa. É uma mensagem do compilador, não da execução, e a diferença entre as duas coisas é a ideia central:

```text
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
```

## Os conceitos

### 0.1 O caminho do código: fonte, *bytecode* e JVM

#### 0.1.1 As três coisas que existem

Quando você escreve um programa em Java, há três objetos distintos e convém dar nome a eles desde já:

- O **código-fonte**: o arquivo de texto que você escreve, com extensão `.java`. Quem o lê é uma pessoa.
- O ***bytecode***: o resultado de compilar o código-fonte, guardado em um arquivo com extensão `.class`. É um formato binário, compacto, que não foi pensado para uma pessoa ler nem é a linguagem de nenhum processador real. É a linguagem de um processador **imaginário**.
- A **JVM** (*Java Virtual Machine*, “máquina virtual Java”): o programa que executa o *bytecode*. É o processador imaginário tornado realidade, uma peça de software que existe para cada sistema operacional e cada tipo de processador.

O programa `javac` é o compilador: converte o código-fonte em *bytecode*. O programa `java` é o lançador: inicia uma JVM e pede que ela execute o *bytecode* de uma classe. A palavra “classe” vai ser importante, então uma definição provisória: em Java, cada `.class` contém **uma** unidade de código chamada classe (mais adiante haverá outras variantes, como os `record` da Lição 3, que também terminam em um `.class`). Por isso se diz “o arquivo de classe”.

O desenho completo do caminho é este:

```text
Hola.java  ──javac──►  Hola.class  ──java──►  [ JVM ]  ──►  lo que el programa hace
(código fuente)        (bytecode)             (ejecuta)
```

Por que dois passos e não um? Porque cada passo resolve um problema diferente e os resolve **em momentos diferentes**. O compilador trabalha uma vez, na sua máquina de desenvolvimento, e é ele quem verifica se o que você escreveu faz sentido (por exemplo, que você não tente colocar um texto onde vai um número, como verá em 0.4). A JVM trabalha toda vez que o programa roda, na máquina onde ele roda, e é ela quem o faz funcionar com aquele sistema operacional e aquele processador. O *bytecode* é o contrato entre as duas metades: o compilador promete produzir *bytecode* válido, e a JVM promete executá-lo.

#### 0.1.2 Um `.class` por dentro

Tudo isso soa abstrato até que se veja. Na Lição 1 você compilará seu primeiro programa e verá o `.class` aparecer na sua pasta. Por enquanto, veja o que contém um pequeno. Este fragmento não é um programa da lição: é uma classe mínima, `Suma`, com um método que soma dois inteiros, que foi compilada com `javac Suma.java`. Depois pediu-se ao desassemblador do JDK, o `javap`, que mostrasse o *bytecode* desse método:

```text
$ javap -c Suma
Compiled from "Suma.java"
class Suma {
  Suma();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  static int sumar(int, int);
    Code:
       0: iload_0
       1: iload_1
       2: iadd
       3: ireturn
}
```

Você não precisa entender cada linha, mas repare no método `sumar`, o de baixo. Suas instruções são `iload_0`, `iload_1`, `iadd` e `ireturn`: “carrega o primeiro argumento inteiro”, “carrega o segundo”, “soma dois inteiros”, “devolve um inteiro”. São as instruções de um processador simples, dos que se programam com uma pilha: colocam-se dois números na pilha, somam-se, o resultado fica no topo. Esse processador não existe em nenhum computador real. Existe apenas dentro da JVM. As instruções e o formato do arquivo são definidos por uma especificação pública, a [Especificação da Máquina Virtual Java](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html), que é a que todas as JVMs cumprem.

Mais dois dados do arquivo, medidos com `xxd` e com `javap -v`. Os primeiros oito bytes de um `.class` do Java 25 são `cafe babe 0000 0045`. Os quatro primeiros, `cafebabe`, são um número mágico que identifica qualquer arquivo de classe, de qualquer versão: a especificação o fixa assim. Os dois últimos bytes (`0045` em hexadecimal) são a **versão maior** do formato, 69 em decimal, que é a que corresponde ao Java 25. Cada versão nova do Java aumenta esse número, e uma JVM antiga se recusa a executar um `.class` com um número maior do que o que ela entende. Quando você vir mais adiante uma mensagem que fala de “class file version”, é isto.

#### 0.1.3 O que a JVM faz com o *bytecode*

A JVM não apenas “interpreta” o *bytecode* instrução por instrução. Ela faz três coisas, e vale a pena saber quais, embora você não vá programar nenhuma delas:

1. **Carrega e verifica.** Lê o `.class`, confirma que o *bytecode* é coerente (que não tente tirar da pilha o que nunca colocou, por exemplo) e só então o deixa executar. É uma rede de segurança contra arquivos corrompidos ou mal feitos.
2. **Executa e, enquanto executa, observa.** Começa interpretando o *bytecode*. À medida que o programa roda, detecta os métodos que são executados muitas vezes.
3. **Compila os métodos quentes para código de máquina real**, a quente, enquanto o programa continua funcionando. Essa peça se chama compilador JIT (*just in time*, “na hora certa”). É a razão pela qual um programa Java, depois de alguns instantes de aquecimento, pode rodar em velocidades comparáveis às de um programa compilado desde o início.

Além disso, a JVM administra a memória: reserva a que o programa pede e a libera sozinha quando ninguém mais a usa, com uma peça chamada coletor de lixo. Se você vem de Go, de Python ou de TypeScript, já conhece a ideia. O [guia da JVM do Java 25](https://docs.oracle.com/en/java/javase/25/vm/) e o [guia do coletor de lixo](https://docs.oracle.com/en/java/javase/25/gctuning/) contam os detalhes; para este curso basta saber que eles existem e que você não vai liberar memória à mão.

A consequência prática de tudo isso é uma promessa famosa do Java: **“escreva uma vez, execute em qualquer lugar”**. O mesmo `.class` roda em Linux, em macOS e em Windows, porque cada sistema tem sua própria JVM que sabe executá-lo. A promessa é real, mas tem limites, e convém conhecê-los desde já para não acreditar demais nela. Dois exemplos medidos: o nome dos caminhos, a codificação do texto e as quebras de linha mudam de um sistema para outro, e o código que os assume quebra mesmo com o `.class` sendo o mesmo. E a **saída de texto** depende da configuração do sistema: com a configuração regional mínima (`LC_ALL=C`), um programa que imprime `sin conexión` (“sem conexão”) escreveu `sin conexi?n`, porque o sistema disse ao Java que sua saída era ASCII; com `LC_ALL=C.UTF-8` imprimiu o texto correto. Desde o Java 18, o texto dos arquivos é lido e escrito em UTF-8 por padrão ([JEP 400](https://openjdk.org/jeps/400)), mas a saída padrão segue a configuração do sistema. Portátil não significa idêntico.

#### 0.1.4 E `java Hola.java`, qual dos dois passos é?

Mais adiante você verá que dá para executar um programa com um único comando, `java Hola.java`, sem passar por um `javac` visível. Isso não contradiz nada do anterior. Esse modo, chamado lançador de um arquivo-fonte ([JEP 330](https://openjdk.org/jeps/330)), compila o arquivo **na memória**, sem deixar um `.class` no disco, e o executa de imediato. Os dois passos acontecem; só que um deles não deixa rastro. É cômodo para aprender e para programas de um arquivo; para projetos de verdade se compila separadamente, e é o que você faz desde a Lição 6.

### 0.2 As peças da plataforma: JDK, JRE e distribuições

#### 0.2.1 JDK e JRE

Há duas siglas que você verá em todos os tutoriais, e cujo significado mudou com os anos:

- O **JRE** (*Java Runtime Environment*) é o necessário para **executar** programas Java: a JVM e as bibliotecas padrão. Com um JRE você pode rodar um `.class` ou um JAR, mas não pode compilar.
- O **JDK** (*Java Development Kit*) é o JRE **mais** as ferramentas para desenvolver: o compilador `javac`, o desassemblador `javap`, o interpretador interativo `jshell` que você usará na Lição 1, o empacotador `jar` e outras. A pasta `bin` de um Temurin 25.0.4 traz 30 programas (contados com `ls | wc -l` e recontados um por um na lista: `jar`, `jarsigner`, `java`, `javac`, `javadoc`, `javap`, `jcmd`, `jconsole`, `jdb`, `jdeprscan`, `jdeps`, `jfr`, `jhsdb`, `jimage`, `jinfo`, `jlink`, `jmap`, `jmod`, `jnativescan`, `jpackage`, `jps`, `jrunscript`, `jshell`, `jstack`, `jstat`, `jstatd`, `jwebserver`, `keytool`, `rmiregistry`, `serialver`).

Este curso precisa compilar, então instale **o JDK**. A confusão histórica é que, durante anos, as pessoas instalavam “Java” e obtinham o JRE (o que bastava para rodar programas alheios, como um instalador ou um aplicativo de desktop), e para programar tinham de ir procurar “o JDK”. Hoje essa distinção quase não importa no seu computador de desenvolvimento, porque você instala o JDK e pronto; mas continua importando em um servidor ou em uma imagem de contêiner, onde convém instalar apenas o necessário para executar. Algumas distribuições ainda publicam pacotes somente de execução; por exemplo, a página de instalação do Temurin para Linux os oferece como `temurin-<versão>-jre`.

E uma terceira sigla que convém não misturar: a **JVM** é apenas a máquina virtual, um componente que vive dentro dos dois. “Instalar a JVM” não é algo que se faça separadamente.

#### 0.2.2 Uma especificação, um projeto e muitas distribuições

A parte que mais desconcerta quem chega de outras linguagens: **não existe um único “Java” que se baixe de um único lugar**. Há três camadas.

1. **A especificação.** A linguagem e a JVM são definidas por documentos públicos: a [Especificação da Linguagem Java](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html) (JLS) e a da JVM (JVMS) que você viu em 0.1. Qualquer pessoa pode implementá-las.
2. **OpenJDK.** É o projeto de código aberto onde se desenvolve a implementação de referência. Cada versão nova tem sua página em [openjdk.org](https://openjdk.org/projects/jdk/25/) com os recursos que traz e seu calendário. O OpenJDK publica código-fonte; não é o que você instala.
3. **As distribuições.** São os binários que alguém constrói a partir do OpenJDK, testa e publica com sua própria política de atualizações: Eclipse Temurin (da fundação Eclipse, por meio do projeto Adoptium), os da Oracle, os da Azul, os da Amazon, os que o Ubuntu inclui em seu repositório, e outros. Todas executam a mesma linguagem; diferem em quem as constrói, por quanto tempo as mantém e sob qual licença.

Este curso usa o **Eclipse Temurin**, a distribuição gratuita do projeto Adoptium, da fundação Eclipse, que não tem um dono comercial e publica um calendário de atualizações claro: sua [página de suporte](https://adoptium.net/support/) diz por quanto tempo cada versão recebe correções, e daí sai a tabela da próxima seção. Todas as saídas do curso foram medidas com o Temurin 25. A Lição 1 o instala com o **SDKMAN**, um gerenciador de versões que trabalha na sua pasta pessoal, sem `sudo`, e que na Lição 6 instala também o Maven. E explica de passagem por que **não** se usa `default-jdk`, o pacote que o Linux Mint sugere. Antecipo o motivo: no Mint 22 esse pacote instala o Java 21, não o Java 25.

### 0.3 As versões: ritmo, LTS e versão prévia

#### 0.3.1 Uma versão nova a cada seis meses

O Java deixou de lançar uma versão grande a cada vários anos e adotou um ritmo fixo: **uma versão nova a cada seis meses**, em março e em setembro. Quem diz isso é a página de suporte do Temurin: “OpenJDK provide a new feature release every six months”. Cada uma traz poucos recursos, pensados para que a mudança seja pequena.

A consequência é que há muitas versões, e a maioria **deixa de receber atualizações depois de seis meses**. Seria impossível de administrar para quem mantém um programa em produção. Por isso existe o conceito de LTS.

#### 0.3.2 LTS: a versão que é mantida por anos

LTS é a sigla de *Long-Term Support*, “suporte de longo prazo”. Uma versão LTS é a que os fabricantes se comprometem a manter, com correções de segurança, por anos. A página de suporte do Temurin descreve assim: a cada dois anos, desde 2021, uma versão é designada LTS, e o Temurin se compromete a mantê-la por pelo menos quatro anos. As contas que ali são publicadas (consultadas em 7 de outubro de 2026):

| Versão | Lançada | Tipo | O Temurin a mantém |
|---|---|---|---|
| Java 17 | setembro de 2021 | LTS | pelo menos até outubro de 2027 |
| Java 21 | setembro de 2023 | LTS | pelo menos até dezembro de 2029 |
| **Java 25** | **setembro de 2025** | **LTS** | **pelo menos até setembro de 2031** |
| Java 26 | março de 2026 | não LTS | terminou em setembro de 2026 |
| Java 27 | 15 de setembro de 2026 | não LTS | até março de 2027 |

A tabela responde à pergunta “por que Java 25 se já existe uma 27?”. Porque a 27 **não é LTS**: dentro de seis meses deixará de receber correções e será preciso saltar para outra. Um curso que é publicado hoje e lido durante anos não deve ensinar sobre uma base que caduca em meio ano. A 25 é a LTS vigente, e é a que qualquer equipe séria terá em produção nos próximos anos. Se o ritmo de dois anos se mantiver, a próxima LTS será a 29, em setembro de 2027; isso é uma extrapolação desse ritmo, não uma promessa de ninguém.

Dois esclarecimentos que evitam confusões. Primeiro, “LTS” não quer dizer “sem mudanças”: a 25 recebe correções de segurança e de erros, mas não recursos novos. Segundo, os números de patch —o que vem depois de `25.` no que `java --version` imprime, que começa com 25— mudam a cada três meses; por isso no curso se diz “Java 25” e nunca um número de patch.

#### 0.3.3 A versão prévia: o que existe mas ainda não é definitivo

O Java incorpora recursos novos por uma etapa intermediária: a **versão prévia** (*preview*), descrita no [JEP 12](https://openjdk.org/jeps/12). Um recurso em versão prévia já está especificado e programado, mas a equipe do Java se reserva o direito de **mudá-lo ou removê-lo** na versão seguinte, conforme o que aprender com quem o testar. Para impedir que alguém o use por acidente, o compilador o rejeita, a menos que seja pedido expressamente com a flag `--enable-preview`.

A versão prévia tem uma consequência concreta que convém ver com um caso real, o da **concorrência estruturada**, que no Java 25 continua em versão prévia ([JEP 505](https://openjdk.org/jeps/505)). Este programa a usa:

```java
import java.util.concurrent.StructuredTaskScope;

void main() throws Exception {
  try (var alcance = StructuredTaskScope.open()) {
    var tarea = alcance.fork(() -> "hola");
    alcance.join();
    IO.println(tarea.get());
  }
}
```

Sem a flag, o compilador se recusa. Isto é o que ele imprime, tal qual (é um fragmento, não uma figura do curso, porque não compila de propósito):

```text
$ java Preview.java
Preview.java:1: error: StructuredTaskScope is a preview API and is disabled by default.
import java.util.concurrent.StructuredTaskScope;
                           ^
  (use --enable-preview to enable preview APIs)
Preview.java:4: error: StructuredTaskScope is a preview API and is disabled by default.
  try (var alcance = StructuredTaskScope.open()) {
                     ^
  (use --enable-preview to enable preview APIs)
2 errors
error: compilation failed
```

A mensagem é clara: “é uma API em versão prévia e está desativada por padrão”. Com `java --enable-preview --source 25 Preview.java` o mesmo programa imprime `hola` (“olá”). Mas há uma armadilha que não se vê na mensagem: um `.class` compilado com recursos em versão prévia leva em seu cabeçalho a marca de “versão prévia”, que foi medida com `javap -v`: `minor version: 65535`, em vez do `0` de sempre. Essa marca faz com que **somente a versão exata do Java com a qual foi compilado possa executá-lo**. Um programa construído sobre um recurso em versão prévia da 25 pode deixar de funcionar ao passar para a 26. Por isso o curso fixa uma regra que não se discute: **nada em versão prévia é usado no código do curso**. A concorrência estruturada aparece na Lição 9 como um quadro de “o que vem aí”, com sua flag e com o aviso de não usá-la em produção; o resto, somente o que já é definitivo na 25.

### 0.4 O que o compilador protege e o que não

Esta é a seção mais importante da lição, e a que mais se parece com um mapa de minas. O Java se gaba de ser uma linguagem com tipos estáticos, ou seja, em que o compilador verifica os tipos **antes** de o programa rodar. É verdade, e é uma vantagem enorme. Mas “tipos estáticos” não significa “sem erros em execução”, e há quatro buracos que convém conhecer desde o primeiro dia. Cada um vem com um programa para executar depois da Lição 1.

#### 0.4.1 O que o compilador pega: os tipos

Comecemos pelo que ele protege. O programa `fig00_01.java` declara uma variável que deve conter um número inteiro e atribui a ela um texto:

```java
// fig00_01.java
void main() {
  int tiempoMs = "142";
  IO.println(tiempoMs);
}
```

Este programa **não roda**: o compilador o rejeita antes. Execute-o com `java fig00_01.java` e você obtém:

```text
$ java fig00_01.java
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
1 error
error: compilation failed
```

Leia de cima para baixo, que é como você vai ler centenas de vezes. A primeira linha traz três dados: o arquivo (`fig00_01.java`), o número da linha (`3`) e o diagnóstico (`incompatible types: String cannot be converted to int`, “tipos incompatíveis: um `String` não pode ser convertido em `int`”). Abaixo, o compilador copia a linha conflitante e põe uma seta `^` no ponto em que percebeu. No final, `1 error` conta os problemas e `error: compilation failed` é o lançador dizendo que não chegou a executar nada. Observe também o que não aconteceu: o programa não imprimiu nada, nem sequer um resultado parcial. Um erro de compilação interrompe tudo **antes** da primeira instrução.

As linhas do programa, para que não fiquem em mistério: `void main()` é a forma de iniciar um programa (a Lição 1 a explica a fundo), `int tiempoMs` declara uma variável de tipo inteiro, e `IO.println` imprime uma linha. O importante é a comparação com suas outras linguagens. Em Python, o equivalente (`tiempo_ms: int = "142"`) roda sem reclamar, porque as anotações não são verificadas na execução. Em TypeScript, o compilador o pega igual ao Java. Em Go, também. O Java está no grupo que o pega antes de rodar.

#### 0.4.2 Primeiro buraco: os genéricos são apagados

Os **genéricos** são a sintaxe do Java para dizer “uma lista *de textos*”, “uma lista *de números*”: `ArrayList<String>`, `ArrayList<Integer>`. (O tema é da [Lição 4](04-colecciones-genericos-streams.md); aqui só interessa uma consequência.) O compilador usa essa informação para verificar seu código. Mas depois de compilar, **a informação desaparece**: o compilador a apaga e deixa o *bytecode* sem ela. Isso se chama **apagamento de tipos** (*type erasure*).

Quem mostra isso é o `fig00_02.java`, que cria uma lista de textos e uma de números e pergunta se são da **mesma classe**:

```java
// fig00_02.java
import java.util.ArrayList;

void main() {
  var palabras = new ArrayList<String>();
  var numeros = new ArrayList<Integer>();
  IO.println(palabras.getClass() == numeros.getClass());
  IO.println(palabras.getClass().getName());
}
```

```text
$ java fig00_02.java
true
java.util.ArrayList
```

O programa, linha por linha. A primeira linha, `import`, diz ao compilador onde encontrar `ArrayList`: está no pacote `java.util` das bibliotecas padrão. `var` pede ao compilador que deduza o tipo da variável a partir do valor atribuído (a Lição 2 explica). Os dois `new ArrayList<...>()` criam uma lista vazia, uma anunciada como de textos e outra como de números. `getClass()` devolve a classe de um objeto **em tempo de execução**, e `==` compara se são a mesma. A resposta é `true`: para a JVM, as duas listas são um `java.util.ArrayList`, e nada mais. O `<String>` e o `<Integer>` já não existem quando o programa roda.

Por que foi projetado assim? Por compatibilidade. Os genéricos chegaram ao Java na versão 5, e até então existiam milhões de linhas de código e bibliotecas sem eles. Apagar a informação permitiu que o código antigo e o novo convivessem. A própria especificação reconhece essa herança ao falar dos “tipos brutos”, que são permitidos somente “como uma concessão à compatibilidade com código antigo” ([JLS, seção 4.8](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.8)). Uma vantagem colateral, que o [tutorial oficial](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html) assinala, é que os genéricos não custam nada em execução: não são criadas classes novas para cada combinação.

A consequência para você é que algumas coisas que você esperaria de uma linguagem com tipos genéricos **não podem ser feitas** em Java: perguntar em execução “isto é uma lista de textos?” não tem resposta, porque essa informação não existe. Quando você chegar à Lição 4 e encontrar restrições estranhas com os genéricos, a causa quase sempre é esse apagamento. Compare com o TypeScript, onde **todos** os tipos são apagados ao gerar o JavaScript. No Java são apagados apenas os argumentos dos genéricos; a classe do objeto sobrevive, e por isso o programa pôde imprimir `java.util.ArrayList`.

#### 0.4.3 Segundo buraco: `null` continua existindo

O Java tem tipos, mas não impede que uma variável de um tipo de objeto, como `String`, valha **“nada”**. Esse “nada” se escreve `null`. O compilador aceita que você o atribua e não avisa quando você o usa mal. O `fig00_03.java` declara um detalhe de serviço, deixa-o em `null` e pede seu comprimento:

```java
// fig00_03.java
void main() {
  String detalle = null;
  IO.println("antes de usar el detalle");
  IO.println(detalle.length());
  IO.println("esto no se imprime");
}
```

```text
$ java fig00_03.java
antes de usar el detalle
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "<local1>" is null
	at fig00_03.main(fig00_03.java:5)
```

Este programa **compila sem problemas**. O erro só aparece quando a quinta linha é executada, e por isso o primeiro `IO.println` chegou a imprimir. É a diferença entre um erro de compilação (0.4.1: nada é executado) e um erro de execução: o programa roda até topar com o problema, e para ali. O nome do erro, `NullPointerException`, significa “exceção de ponteiro nulo”; uma **exceção** é a forma que o Java tem de avisar que algo deu errado durante a execução (a [Lição 5](05-errores-excepciones.md) a estuda inteira). Sua mensagem diz o que você tentou fazer (`Cannot invoke "String.length()"`, “não é possível invocar `String.length()`”) e por que falhou (`because "<local1>" is null`, “porque `<local1>` é nulo”). Desde o Java 15, a JVM dá mensagens detalhadas como essa por padrão, graças ao [JEP 358](https://openjdk.org/jeps/358).

Talvez você estranhe que a variável se chame `<local1>` em vez de `detalle`. Não é um erro da lição: quando você executa um arquivo diretamente com `java Archivo.java`, o compilador interno não conserva os nomes das variáveis locais. A [Lição 2](02-tipos-cadenas-control.md) mostra como recuperá-los com outra forma de compilar. O importante hoje é a ideia: **o compilador do Java não protege você de `null`**.

A invenção do `null` foi lamentada por seu próprio autor, Tony Hoare, que em 2009 a chamou de “meu erro de um bilhão de dólares” ([palestra](https://www.infoq.com/presentations/Null-References-The-Billion-Dollar-Mistake-Tony-Hoare/)), pelas falhas que provocou durante décadas. O Java não o removeu, mas com os anos oferece ferramentas para não depender dele: na Lição 3 os tipos selados substituem os `null` do `revisor` por um tipo que diz explicitamente “houve resposta” ou “não houve”, e na Lição 4 o `Optional` cobre os casos de “às vezes há dado”.

#### 0.4.4 Terceiro buraco: o `+` nem sempre soma

O terceiro é uma armadilha para quem vem de outra linguagem. O `fig00_04.java` usa o operador `+` de três formas:

```java
// fig00_04.java
void main() {
  String tiempo = "142";
  IO.println(tiempo + 1);
  IO.println(1 + 42 + "ms");
  IO.println("ms" + 1 + 42);
}
```

```text
$ java fig00_04.java
1421
43ms
ms142
```

Os três resultados saem de uma única regra: **se algum dos dois lados do `+` é um texto, o `+` concatena; se os dois são números, soma**; e avalia-se da esquerda para a direita. A primeira linha junta o texto `"142"` com o número `1` e dá `"1421"`, um texto de quatro caracteres, não o número 143. A segunda soma primeiro `1 + 42` (os dois são números) e dá `43`, e só então o junta com `"ms"`. A terceira começa com um texto, então `"ms" + 1` já é concatenação e dá `"ms1"`, e depois `"ms1" + 42` dá `"ms142"`. A regra está na [especificação da linguagem](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.18.1).

O que torna este caso perigoso não é ser ilegal, mas **compilar sem um único aviso** e dar um resultado plausível. Se no `revisor` um tempo chega como texto, `"142" + 1` não avisará nada: produzirá `"1421"`, e esse dado falso viajará até o relatório. Em Python, `"142" + 1` é um `TypeError` na hora; em TypeScript, dá `"1421"` igual ao Java. O Java está do lado do TypeScript neste ponto, e por isso a Lição 2 insiste em que cada dado do `revisor` tenha o tipo correto desde que entra.

#### 0.4.5 Quarto buraco: o que o compilador nunca verá

Os três anteriores podem ser provocados com programas de poucas linhas. Há um quarto grupo, mais amplo: **os valores que só são conhecidos em execução**. O compilador verifica tipos; não verifica valores. Estes fragmentos foram executados e isto é o que imprimem:

```java
void main() {
  int maximo = Integer.MAX_VALUE;
  IO.println(maximo + 1);
}
```

```text
$ java Desborde.java
-2147483648
```

Um inteiro do Java tem tamanho fixo (quatro bytes), e se uma soma passa do máximo, o resultado **dá a volta** até o mínimo, sem erro. A especificação diz isso de frente: “os operadores inteiros não indicam estouro de nenhuma maneira” ([JLS, seção 4.2.2](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.2.2)). Outros dois casos, medidos igualmente, cada um em seu próprio arquivo com um `void main()` que contém apenas a linha indicada:

```java
IO.println(1 / 0);
```

```text
$ java Division.java
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at Division.main(Division.java:2)
```

```java
IO.println("hola".charAt(10));
```

```text
$ java Caracter.java
Exception in thread "main" java.lang.StringIndexOutOfBoundsException: Index 10 out of bounds for length 4
	at java.base/jdk.internal.util.Preconditions$1.apply(Preconditions.java:55)
	...
	at Caracter.main(Caracter.java:2)
```

As reticências substituem nove linhas internas do JDK, cujos números mudam a cada atualização. Dividir um inteiro por zero é uma exceção em execução; pedir o caractere número 10 de um texto de quatro caracteres também. Em nenhum dos dois casos o compilador poderia saber sem executar o programa. O resumo da seção cabe em uma tabela:

| O quê | Quando é detectado? | Exemplo desta lição |
|---|---|---|
| Um texto onde vai um número | Ao compilar | `int tiempoMs = "142";` (fig00_01) |
| Um `null` usado como se fosse um objeto | Ao executar | `detalle.length()` (fig00_03) |
| Um `+` que concatena em vez de somar | Nunca: o resultado é válido | `"142" + 1` (fig00_04) |
| O tipo dos elementos de uma lista | Ao compilar, e depois é apagado | `ArrayList<String>` (fig00_02) |
| Um inteiro que passa do seu máximo | Nunca: dá a volta | `Integer.MAX_VALUE + 1` |
| Divisão inteira por zero | Ao executar | `1 / 0` |

O resto do curso procura fazer com que essa coluna do meio seja a mais cedo possível: mover erros de “nunca” ou “ao executar” para “ao compilar”. Os tipos selados da Lição 3 fazem com que esquecer um caso seja um erro do compilador. As coleções imutáveis da Lição 4 impedem mudanças acidentais. Os testes da Lição 7 pegam o que o compilador não consegue. Não é casualidade: é o método.

### 0.5 Java diante do que você já conhece

Com o mapa anterior, a comparação com as outras linguagens do curso deixa de ser vaga. Isto é o que muda, e o que não:

| | Go | TypeScript | Python | **Java** |
|---|---|---|---|---|
| O que o compilador produz? | um executável para um sistema e um processador | JavaScript (os tipos desaparecem) | *bytecode* do interpretador, de forma automática | *bytecode* da JVM, em um passo explícito |
| Quando os tipos são verificados? | ao compilar | ao compilar (com `tsc`) | não são verificados ao executar | ao compilar |
| O que executa o programa? | o processador diretamente | Node ou um navegador | o interpretador do Python | a JVM |
| Há coletor de lixo? | sim | sim (o motor do JavaScript) | sim | sim |
| Como é entregue? | copiar o executável | copiar o JavaScript e o Node | copiar o código e o interpretador | copiar um JAR (um arquivo com `.class` dentro) e ter uma JVM |

Duas coisas saltam aos olhos. A primeira: se você vem do Python, o modelo de “código-fonte, *bytecode*, máquina virtual” **você já conhece**; o Python também compila para *bytecode* e o executa em uma máquina virtual. O que muda no Java é que o passo de compilar é explícito e visível, e que no meio ele verifica os tipos. A segunda: se você vem do Go, a diferença é a JVM como intermediária. Um executável do Go leva tudo dentro; um programa Java precisa de uma JVM instalada onde roda, em troca de ser portátil entre sistemas com o mesmo arquivo.

Existe, além disso, um caminho para produzir um executável nativo a partir de um programa Java (compilação antecipada com GraalVM). Não é usado neste curso. O blog da Hábil explica [quando vale a pena e quando não compilar Java para nativo](https://www.habil.mx/pt/blog/java-nativo-quando-sim-quando-nao/); leia quando terminar a Lição 10, não antes.

### 0.6 O que vem no resto do curso

Tudo o que foi dito é posto em prática nas lições seguintes. O curso constrói um único programa, o `revisor`, e cada lição acrescenta uma peça:

| Lição | O que é acrescentado | Que buraco desta lição ajuda a fechar |
|---|---|---|
| 1 | Você instala o Java 25 e escreve e compila seu primeiro programa | o caminho de 0.1, no seu terminal |
| 2 | Tipos, strings e controle de fluxo | `null`, `==` contra `equals`, o `+` que concatena |
| 3 | `record`, `enum` e tipos selados | `null` substituído por tipos que dizem todos os seus casos |
| 4 | Coleções, genéricos e *streams* | o apagamento de tipos, listas que não mudam |
| 5 | Erros e exceções | os erros de execução, tratados em vez de tolerados |
| 6 | Projeto com Maven (a ferramenta que constrói projetos Java) | o compilado como produto: pacotes, um `pom.xml` e um `.jar` |
| 7 | Testes com JUnit | o que o compilador não vê, pego por testes |
| 8 | HTTP e JSON | dados que chegam de fora e precisam ser validados |
| 9 | Concorrência | verificar todos os serviços ao mesmo tempo |
| 10 | O programa terminado | linha de comando, servidor, qualidade e um JAR |

Há mais um acordo prático. Os programas das Lições 1 a 5 são executados com `java Archivo.java`, sem ferramentas adicionais. A partir da Lição 6 o projeto vive em uma estrutura padrão, com Maven. O curso usa **indentação de dois espaços** em todo o código, a do guia de estilo do Google para Java, porque na Lição 10 uma ferramenta verifica esse estilo e seria absurdo confessar então que tudo o que veio antes estava escrito de outra maneira. Muito código da internet usa quatro espaços; funciona igual.

## O erro que você vai ver

Esta lição tem um erro principal, o do compilador que se recusa, e um de execução que convém reconhecer à primeira vista. Ambos são das seções anteriores.

**O erro principal: `incompatible types`.** É o do `fig00_01.java`:

```text
fig00_01.java:3: error: incompatible types: String cannot be converted to int
  int tiempoMs = "142";
                 ^
```

Lê-se assim: o arquivo e a linha (`fig00_01.java:3`), o que ocorreu (um `String` não pode ser convertido em `int`), a linha conflitante, e a seta no ponto exato. Corrige-se de duas formas, conforme o que você queria: se o dado era um texto, a variável devia ser do tipo `String`; se você queria o número, é preciso converter o texto explicitamente (disso se ocupa a Lição 2). O que **não** se faz é ignorá-lo: um erro de compilação nunca é “deixado passar”, porque o programa simplesmente não roda.

**Um erro de execução que você já consegue reconhecer: `NullPointerException`.**

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.length()" because "<local1>" is null
```

Lê-se da esquerda para a direita: na thread principal (`main`) ocorreu uma `NullPointerException`; você tentou invocar `String.length()` e não foi possível porque o que havia na variável era `null`. Abaixo, a linha que começa com `at` diz **onde**: `fig00_03.main(fig00_03.java:5)`, ou seja, o método `main` do arquivo, linha 5. Revise essa linha e pergunte-se de onde pode ter vindo um `null`.

## O que se faz errado

**Instalar “Java” sem saber o que se instala.** Quem digita `sudo apt install default-jre` obtém apenas a parte de execução, e no Mint 22 de uma versão anterior à que o curso usa. Para programar, o JDK; para o curso, o do Java 25.

**Usar a versão mais recente por ser a mais recente.** O Java 27 foi lançado em 15 de setembro de 2026 e não é LTS. Para aprender, para um servidor ou para um programa que vai viver anos, a LTS vigente. Se você já tem a 27 instalada, não acontece nada grave com este curso, mas a Lição 1 diz como se certificar de qual versão você está usando.

**Acreditar que “tem tipos” significa “não falha em execução”.** Os tipos pegam uma família de erros, não todos. Os de 0.4 existem em qualquer programa Java, feito por quem for.

**Copiar um tutorial sem olhar a data.** Na internet circulam versões de “Olá, mundo” de vinte anos atrás que obrigam a escrever uma classe e um método `public static void main(String[] args)`. Funcionam, mas para um arquivo de um único programa já não é preciso nada disso, como você verá na Lição 1. O mesmo vale para as bibliotecas: quase tudo o que está publicado sobre JSON ou testes foi escrito para versões anteriores às do curso. Quando uma receita não compilar, a primeira coisa a revisar é a versão.

**Usar um recurso em versão prévia “porque funciona”.** Funciona hoje, com a flag, naquela versão exata. Segundo o JEP 12, pode mudar ou desaparecer na seguinte. Não em código que outra pessoa vai manter.

**Confundir Java com JavaScript.** Compartilham letras do nome e metade das chaves, e nada mais. São linguagens diferentes, com modelos de execução diferentes e com uma relação histórica que ninguém precisa saber para usar uma das duas.

**Esperar que “escreva uma vez, execute em qualquer lugar” inclua o ambiente.** O `.class` é portátil; a codificação da saída, os caminhos e as quebras de linha não o são por si sós. Mede-se, não se supõe.

## Exercícios

Os exercícios desta lição não requerem Java: o primeiro é de leitura, o segundo é respondido **antes** de rodar qualquer coisa e conferido depois da Lição 1, e o terceiro e o quarto são respondidos por escrito, com suas palavras. A melhor maneira de aproveitar o segundo é escrever sua previsão no [diário de bordo](bitacora.md) e compará-la depois.

### Exercício 1 — Os dados do `revisor` (projeto)

Esta é a tabela que o `revisor` produzirá ao terminar o curso. Para cada coluna, indique se o dado é um **número**, um **texto** ou pode ser **nada**. Repare sobretudo no tempo de `reportes`.

```text
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión
```

Depois responda: o que o Java faria com `"142" + 1`, se um tempo chegasse como texto? E que problema haveria se um programa guardasse o tempo de `reportes` como `null` e depois tentasse formatá-lo?

### Exercício 2 — Quando o programa fica sabendo?

Para cada um destes seis fragmentos, preveja se (a) o compilador o rejeita, (b) o programa compila e falha ao executar, ou (c) compila, roda e dá um resultado sem avisar. Se for (b), diga que exceção você espera.

1. `int tiempoMs = "142";`
2. `String detalle = null;` seguido de `detalle.length()`.
3. `Integer.MAX_VALUE + 1`
4. `var palabras = new java.util.ArrayList<String>();` seguido de `palabras.add(5);`
5. `1 / 0` (entre inteiros).
6. `"hola".charAt(10)`

### Exercício 3 — Escolha a versão

Decida que versão do Java você usaria em cada caso e por quê: (a) um programa novo que sua equipe vai manter por vários anos; (b) você tem o Java 27 instalado e quer seguir este curso; (c) um artigo usa `StructuredTaskScope` e você quer testá-lo na sua máquina.

### Exercício 4 — Explique em cinco frases

Um colega que programa em Go lhe pergunta o que acontece entre “escrevi um `.java`” e “o programa roda”. Responda em no máximo cinco frases, usando as palavras *código-fonte*, `javac`, *bytecode*, JVM e compilador JIT. Sem olhar a lição.

## Soluções

### Solução 1

As colunas, em ordem:

- **SERVICIO** é **texto** (`catalogo`, `pagos`...). **ESTADO** é **texto** que só pode valer uma de três coisas (`ARRIBA`, `LENTO`, `CAIDO`); na Lição 3 vira um tipo próprio que não permite outras.
- **TIEMPO** é um **número** (142, 2300, 87) que é mostrado com uma unidade (`142ms`, `2.3s`) e **pode ser nada**: `reportes` não respondeu e não tem tempo. Esse traço `—` representa um dado que não existe.
- **DETALLE** é **texto** ainda que pareça um número: o `200` é o código de status, mas na tabela é mostrado como texto, igual a `sin conexión` (“sem conexão”).

Com `"142" + 1`, o Java **não soma**: concatena e dá o texto `"1421"` (fig00_04). Como compila sem avisar, um dado falso entraria no relatório sem que ninguém percebesse. E com o tempo de `reportes` guardado como `null`, formatá-lo com algo como `tiempo.length()` lança uma `NullPointerException` em execução (fig00_03), porque o compilador não protege disso. As duas situações são a razão de as Lições 2 e 3 existirem: na 2 se aprende a escrever o código com cuidado diante de `null`, e na 3 muda-se o design para que “não houve resposta” seja um caso explícito do tipo e não um `null` escondido.

### Solução 2

Depois da Lição 1, você pode conferi-las com o `jshell` ou com um arquivo de programa. As respostas, medidas com o Temurin 25.0.4:

1. **(a)** O compilador o rejeita: `incompatible types: String cannot be converted to int` (fig00_01).
2. **(b)** Compila e falha ao executar com `java.lang.NullPointerException` (fig00_03).
3. **(c)** Compila, roda e dá `-2147483648`, sem aviso: o inteiro dá a volta.
4. **(a)** O compilador o rejeita. É o valor do genérico: a lista é de textos e `5` não é. A mensagem começa com `error: no suitable method found for add(int)` e segue com uma lista das variantes de `add` que tentou, cada uma com sua razão (a que importa diz `argument mismatch; int cannot be converted to String`). É um exemplo de que o compilador de fato usa os genéricos, ainda que depois os apague.
5. **(b)** Compila e falha ao executar com `java.lang.ArithmeticException: / by zero`.
6. **(b)** Compila e falha ao executar com `java.lang.StringIndexOutOfBoundsException: Index 10 out of bounds for length 4`.

Se você errou no 3, é normal: é o único em que o programa “funciona” e dá um número errado, e por isso é o mais perigoso dos seis.

### Solução 3

(a) **Java 25**, a LTS vigente: terá correções de segurança pelo menos até setembro de 2031. Uma versão não LTS deixaria de ser mantida depois de seis meses.

(b) **Não é preciso desinstalar nada, mas também não convém usá-la para o curso.** O Java 27 não é LTS e o curso verifica seus programas com a 25; o seguro é instalar o Temurin 25 (com o SDKMAN, a Lição 1, as duas convivem e você escolhe qual usar) e conferir com `java --version` qual está ativa. O que não se deve fazer é supor que “a mais nova serve para tudo”.

(c) **Na versão 25, com `--enable-preview`**, sabendo que é um recurso em versão prévia: a API pode mudar na versão seguinte (de fato mudou entre a 25 e a 26, segundo o [JEP 525](https://openjdk.org/jeps/525)), o `.class` resultante só roda nessa versão exata e não deve ser usado em um programa que outra pessoa vá manter.

### Solução 4

Uma resposta possível: “Você escreve o *código-fonte* em um `.java`. O compilador `javac` o verifica (sobretudo os tipos) e o traduz para *bytecode*, que vai em um `.class`. Esse *bytecode* não é código de um processador real: quem o executa é a JVM, que existe para cada sistema operacional. Enquanto o programa roda, a JVM detecta os métodos muito usados e os converte em código de máquina com o compilador JIT. Por isso o mesmo `.class` roda em qualquer sistema que tenha uma JVM, ao contrário de um executável do Go, que já traz o código do seu processador”.

## Como sei que consegui

- [ ] Você desenha de memória o caminho `.java` → `javac` → `.class` → `java` → JVM, e diz o que cada seta faz.
- [ ] Você diz a diferença entre JDK, JRE e JVM, e qual se instala para este curso.
- [ ] Você explica por que existe um Java 27 e o curso usa o 25, com a palavra LTS.
- [ ] Você sabe o que é um recurso em versão prévia e que flag ele exige.
- [ ] Para cada linha da tabela de 0.4.5, você diz quando o problema é detectado.
- [ ] Depois da Lição 1, você executa `java fig00_01.java`, `fig00_02.java`, `fig00_03.java` e `fig00_04.java` e suas saídas coincidem com as desta lição, exceto o caminho.
- [ ] Você prevê no diário de bordo o que `"ms" + 1 + 42` imprime antes de executá-lo, e confere.
- [ ] Você anotou no seu diário de bordo uma coisa que acreditava sobre a JVM e que se mostrou diferente.

## Resumo

Responda sem olhar a lição:

1. O que um `.class` contém e quem o executa?
2. O que o JDK acrescenta ao JRE? Por que o JDK é instalado neste curso?
3. O que é o OpenJDK e o que é uma distribuição como o Temurin?
4. O que significa LTS? Por que o Java 27 não é a versão do curso?
5. O que acontece com um `.class` compilado com recursos em versão prévia?
6. Em que se diferenciam um erro de compilação e um de execução? Dê um exemplo de cada um.
7. O que é o apagamento de genéricos e o que o `fig00_02.java` prova?
8. Por que `"142" + 1` é `"1421"` e não `143`?
9. Por que o compilador do Java não avisa de um `null` mal usado?

## Para ler mais

- [JEP 512, “Compact Source Files and Instance Main Methods”](https://openjdk.org/jeps/512): a proposta que faz com que no Java 25 um programa possa ser escrito sem classe nem `public static`, que é o estilo deste curso. Lê-se melhor depois da Lição 1. Consultado em 7 de outubro de 2026.
- [JEP 12, “Preview Features”](https://openjdk.org/jeps/12): o que é a versão prévia, por que existe e que garantias não dá. Consultado em 7 de outubro de 2026.
- [Especificação da Máquina Virtual Java, SE 25, capítulo 4](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html): o formato exato do arquivo `.class`, incluindo o número mágico e a versão. Consultado em 7 de outubro de 2026.
- [Temurin Support, Adoptium](https://adoptium.net/support/): o calendário de versões e o fim do suporte de cada uma, de onde sai a tabela de 0.3. Consultado em 7 de outubro de 2026.

---

**Próxima:** [Lição 1 — Instalar o Java 25 no seu Linux Mint](01-instalacion.md)
