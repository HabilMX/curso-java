# Lição 6 — Projeto com Maven

**Tempo:** 90 min (justos: inclui instalar o Maven, e o primeiro build baixa peças da internet; não tenha pressa)

**O que você constrói:** o `revisor` deixa de ser uma pasta de arquivos soltos e vira um projeto Maven, com pacotes, que é compilado, empacotado em um `.jar` e executado sempre com os mesmos dois comandos

**O que você aprende:** quando um arquivo solto deixa de bastar; pacotes e visibilidade; a volta de `public static void main(String[] args)`; instalar o Maven 3.10.0 com o SDKMAN; a estrutura padrão do Maven e o `pom.xml`; o ciclo `compile`, `test`, `package`, `verify`; o que há dentro de um `.jar` e por que ele ainda não se executa com `java -jar`

**De onde você vem.** Você traz o projeto da [Lição 5](05-errores-excepciones.md): uma pasta com vários arquivos `.java` (o modelo, a classificação, a formatação, o relatório, a leitura de `servicios.txt` com suas exceções próprias), que se executa com `java Revisor.java` e leva uns 2.5 segundos porque a simulação espera o que cada serviço indica. Funciona, e você o roda com um único comando. Mas esse comando tem limites que vão aparecer assim que o programa precisar de algo que você não escreveu: a próxima lição precisa de uma biblioteca de testes, e a seguinte, de uma para ler JSON. O lançador de arquivos-fonte não sabe usar bibliotecas de terceiros, não guarda nada do que compila e não tem uma forma reconhecível. Hoje você transforma o `revisor` em um projeto de verdade: com a estrutura que qualquer programador de Java reconhece e com uma ferramenta que o compila, o empacota e, a partir da próxima lição, o testa. Você trabalha na sua pasta `~/curso-java/revisor/`.

**O que esta lição não faz.** Não escreve testes ainda: isso é a [Lição 7](07-pruebas-junit.md), que se apoia no que você constrói hoje (o `pom.xml`, a pasta de testes que hoje fica vazia e o ciclo de vida do Maven). Não consulta nenhuma rede: o `Simulador` continua fazendo o papel dos serviços e isso não muda até a [Lição 8](08-http-y-json.md). Também não adiciona bibliotecas ao programa (a primeira dependência do programa chega na 8) nem verifica os serviços ao mesmo tempo (isso é a [Lição 9](09-concurrencia.md)). O que se resolve aqui é **como se organiza, se compila e se empacota um programa Java que já tem mais de dez arquivos**.

**Sobre as execuções.** Tudo nesta lição foi executado com Temurin 25 e Maven 3.10.0, em macOS, com o terminal em UTF-8; a instalação do Maven com o SDKMAN e o seu `mvn --version` foram medidos também em um Linux Mint 22.3, em 13 de outubro de 2026. As saídas do Maven são longas e mudam a cada execução (downloads, tempos, caminhos), então são mostradas abreviadas: conserva-se o que cada seção analisa, e as reticências (`...`) marcam o que foi retirado; os caminhos longos são mostrados a partir de `~/curso-java`. As mensagens do lançador do Java (as que começam com `Error:`) saem no idioma do sistema: aqui são mostradas em inglês, e se o seu Linux Mint estiver em espanhol você verá a primeira linha traduzida; o resto não muda. Os programas estão em [`programas/06-proyecto-maven/`](../programas/06-proyecto-maven), no repositório do curso.

## Ao terminar, você vai conseguir

- Explicar quais três coisas o lançador de arquivos-fonte não faz e o que um projeto Maven acrescenta ao seu programa.
- Declarar um pacote, colocar o arquivo na pasta que lhe corresponde e dizer quais classes cada uma enxerga segundo sua visibilidade.
- Instalar o Maven 3.10.0 com o SDKMAN e reconhecer em `mvn --version` que ele usa o seu Temurin 25.
- Ler um `pom.xml` e dizer para que serve cada elemento, incluindo o motivo de fixar a versão de cada *plugin*.
- Dizer quais fases `mvn -B verify` executa, o que cada uma deixa em `target/` e o que há dentro do `.jar`.
- Migrar o `revisor` para a estrutura do Maven e executá-lo a partir de `target/classes` e do seu `.jar`.

## O porquê antes do como

Até a lição passada, a sua ferramenta de build foi o próprio `java`: você entrega um arquivo `.java`, ele o compila na memória junto com os demais da pasta e o executa. Para aprender é perfeito. Para um programa que vai crescer tem três limites, e os três se resolvem com a mesma ferramenta:

1. **Não pode usar bibliotecas de terceiros.** O lançador da [Lição 1](01-instalacion.md) (`java Revisor.java`) compila os seus arquivos e nada mais. A próxima lição precisa de uma biblioteca de testes, e a Lição 8, de uma para ler JSON; sem uma forma de declarar dependências, não há caminho.
2. **Não deixa nada.** Compila a cada execução e não guarda o resultado: não existe um “programa compilado” que você possa entregar, nem uma forma de dizer “compile, mas ainda não execute”. Também não tem onde colocar testes nem quem os rode.
3. **Não tem forma.** Quando o programa tem dez arquivos, três pessoas e dois anos, “uma pasta com arquivos” deixa de ser uma estrutura. Todos os projetos Java do mundo compartilham uma convenção de pastas, e essa convenção é o que permite que uma ferramenta, ou uma pessoa nova, os entenda sem que ninguém os explique.

A ferramenta é o **Maven**, que compila, testa e empacota projetos Java a partir de um arquivo que os descreve. Ele se instala uma vez, e a partir daí cada projeto Java que use Maven se constrói com o mesmo comando. Para usá-lo é preciso dar dois passos prévios que a lição explica em ordem: colocar o código em **pacotes** (a forma que o Java tem de ordenar suas classes por nome e por pasta) e aprender quais classes cada uma enxerga das demais (a **visibilidade**).

A lição tem quatro partes: **pacotes e visibilidade** (6.1); **Maven**: instalá-lo, sua estrutura, seu `pom.xml`, seu ciclo de vida e a migração do `revisor` (6.2); **o ciclo completo e o `.jar`** (6.3); e **o passo do projeto** (6.4). Com esse destino em mente, isto é o que você vai ver no final, quando construir o projeto completo:

```text
$ mvn -B verify
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
...
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] No tests to run.
[INFO] 
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.6.0.jar
[INFO] BUILD SUCCESS
```

Treze arquivos compilados, um `.jar` construído e uma linha que passa despercebida: `No tests to run.`, “não há testes para rodar”. Ao terminar você saberá de onde sai cada linha, o que ficou dentro do `.jar`, e por que esse `BUILD SUCCESS` com zero testes é justamente a pergunta com que começa a Lição 7.

## Os conceitos

### 6.1 Pacotes e visibilidade

#### 6.1.1 O que o lançador não faz

O lançador de arquivos-fonte (`java Revisor.java`) é uma comodidade que o Java acrescentou para que aprender não exigisse uma ferramenta de build. Ele compila em memória, a cada execução, os arquivos de que o programa precisa, roda o resultado e não deixa nada em disco ([JEP 458](https://openjdk.org/jeps/458), final desde o Java 22). É perfeito para o que você fez até aqui e tem três limites que agora incomodam. O primeiro pode ser provocado em duas linhas. Escreva um arquivo que use JUnit, a biblioteca de testes que você vai usar na Lição 7:

```java
// fig06_01.java
import org.junit.jupiter.api.Test;

void main() {
  IO.println("revisor: todavía no revisa nada");
}
```

É o programa de sempre, com uma linha a mais: um `import` de uma classe do JUnit. Um `import` (você o conhece dos arquivos que usaram `java.util`) diz ao compilador de qual pacote vem um nome. Execute-o:

```text
$ java fig06_01.java
fig06_01.java:2: error: package org.junit.jupiter.api does not exist
import org.junit.jupiter.api.Test;
                            ^
1 error
error: compilation failed
```

O compilador procura o pacote `org.junit.jupiter.api` e não o encontra, porque o lançador não sabe de onde tirá-lo: não há nenhum lugar onde declarar que o seu programa depende de uma biblioteca. Quando você vir `package ... does not exist` com um nome que não é seu, quase sempre significa isso: falta uma dependência, não é um erro de digitação.

O segundo limite não produz uma mensagem, e sim um hábito: como o lançador compila tudo a cada execução e não guarda nada, o “compilado” não existe como produto. Não há um `.class` para entregar, nem uma forma de dizer “compile, mas ainda não execute”. Com treze arquivos o custo em tempo é uma fração de segundo (medido: uns 0.3 segundos nesta máquina), mas o custo em ordem cresce com o projeto. O terceiro limite é o dos testes: o lançador não sabe o que é um, nem onde moram, nem como rodá-los todos juntos.

Antes do Maven há mais uma peça que o Java exige e que o seu projeto ainda não tem: os pacotes.

#### 6.1.2 Pacotes: um nome e uma pasta

Imagine que duas pessoas escrevem, cada uma por conta própria, uma classe chamada `Servicio`. Se as duas acabam no mesmo programa, o compilador não consegue saber qual é qual. O problema é antigo e a solução do Java também: cada classe vive em um **pacote**, que é um nome com pontos —`mx.habil.revisor`— e que faz parte do nome completo da classe. O seu `Servicio` passa a se chamar `mx.habil.revisor.Servicio`, e o da outra pessoa, `com.otraempresa.inventario.Servicio`. No JDK acontece o mesmo: existe `java.util.List` e existe `java.awt.List`, duas coisas diferentes com o mesmo nome curto.

A convenção para escolher o nome é usar o domínio de quem escreve o código ao contrário (`habil.mx` vira `mx.habil`) seguido do nome do projeto (`revisor`). Um domínio é único no mundo, então o nome também é. As regras da linguagem são três, e as três estão na [especificação, capítulo 7](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html):

1. **O arquivo declara o seu pacote na primeira linha** (sem contar comentários): `package mx.habil.revisor;`.
2. **A pasta tem o mesmo nome que o pacote**, com uma subpasta para cada ponto: o arquivo mora em `mx/habil/revisor/`.
3. **Para usar uma classe de outro pacote, importa-se**: `import mx.habil.revisor.Estado;`. As classes do mesmo pacote se enxergam sem `import`, e as de `java.lang` (como `String`) sempre.

Vejamos com o menor programa que se pode escrever dentro de um pacote. É uma classe, `Hola`, com um `main` que imprime o nome completo da sua própria classe:

```java
// fig06_02/src/mx/habil/revisor/Hola.java

package mx.habil.revisor;

/** Una clase mínima dentro de un paquete. */
public final class Hola {

  private Hola() {}

  /** Imprime el nombre completo de su propia clase. */
  public static void main(String[] args) {
    System.out.println("hola desde " + Hola.class.getName());
  }
}
```

Três coisas são novas. A primeira é o `package`. A segunda é que esta classe já não é um arquivo compacto: tem o seu `class` e o seu `main` completo, `public static void main(String[] args)`, que a seção 6.1.4 explica. A terceira, `Hola.class.getName()`, pede ao Java o nome da classe tal como ele a conhece; serve aqui para ver que o pacote faz parte dele. (Os comentários entre `/**` e `*/` são documentação; são explicados em 6.1.3.)

Compilar e executar um arquivo com pacote já não é `java Hola.java`. Faz-se em dois passos, e os dois têm um detalhe:

```text
$ cd fig06_02
$ javac -d out src/mx/habil/revisor/Hola.java
$ java -cp out mx.habil.revisor.Hola
hola desde mx.habil.revisor.Hola
```

`javac -d out` diz “deixe os `.class` na pasta `out`”, e o `javac` faz algo que não se vê: **cria as subpastas do pacote** (`out/mx/habil/revisor/Hola.class`). Os `.class` ficam em uma pasta com a mesma forma que a do código. Ao executar, `-cp out` é a abreviação de *classpath*, “o caminho de classes”: a lista de lugares onde a JVM procura os `.class`. Com isso a JVM sabe por onde começar a procurar, e `mx.habil.revisor.Hola` é o **nome completo** da classe, com o seu pacote: a JVM o converte no caminho `mx/habil/revisor/Hola.class` e o procura dentro de `out`.

Agora os dois erros que todo mundo comete na primeira vez. O primeiro é escrever só o nome curto:

```text
$ java -cp out Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola
```

A JVM procura `Hola.class` diretamente dentro de `out`, não o encontra (está dois níveis mais abaixo) e diz isso com uma exceção de nome longo, `ClassNotFoundException`: “não existe nenhuma classe chamada `Hola`”. A solução é escrever o nome completo. O segundo erro é “entrar na pasta da classe” e executar dali, pensando que, se você está parado ao lado do arquivo, ela o vai encontrar:

```text
$ cd out/mx/habil/revisor
$ java Hola
Error: Could not find or load main class Hola
Caused by: java.lang.NoClassDefFoundError: Hola (wrong name: mx/habil/revisor/Hola)
```

Aqui sim ela encontra um `Hola.class`, mas o seu nome verdadeiro é `mx/habil/revisor/Hola`, não `Hola`, e a JVM o rejeita com `(wrong name: mx/habil/revisor/Hola)`: “o arquivo diz que a classe se chama de outra forma”. É a mesma regra vista do outro lado. **O nome da classe e o caminho do seu arquivo têm que coincidir, e a JVM verifica isso.** Por isso os programas com pacotes são sempre executados a partir da raiz das classes (`out`, ou `target/classes` no Maven), com o nome completo.

Resta uma pergunta que o compilador tem direito de lhe fazer: o que acontece se o arquivo declara um pacote e está em outra pasta? A resposta é menos dramática do que parece, e convém conhecê-la como é. Moveu-se `Clasificador.java` uma pasta acima de onde deveria estar, deixando a sua linha `package mx.habil.revisor;` intacta, e pediu-se ao `javac` que compilasse `Revision.java` procurando as demais classes por pasta:

```text
$ javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java
src/main/java/mx/habil/revisor/Revision.java:14: error: cannot find symbol
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
                                ^
  symbol:   variable Clasificador
  location: class Revision
1 error
```

O compilador sai para procurar `mx/habil/revisor/Clasificador.java` porque `Revision` o menciona, não o encontra onde o nome do pacote diz que deveria estar, e conclui que não existe. O surpreendente é o contrário: **quando se entregam todos os arquivos explicitamente, o `javac` não reclama**, e o Maven faz assim (medido: o projeto compila e os `.class` ficam onde correspondem, mesmo que o `.java` esteja na pasta errada). A pasta que mente não quebra a compilação do Maven; quebra tudo o que procura por pasta: os editores, as buscas, a compilação incremental, e a pessoa que chegar depois de você. É um erro silencioso, dos piores: a regra é que a pasta diga sempre a verdade, ainda que o compilador, hoje, deixe você mentir.

Há um último erro dessa família, o do arquivo que não se chama como a sua classe pública. O arquivo `Revisor.java` deste exemplo declara uma classe com outro nome:

```java
package mx.habil.revisor;

public final class Principal {}
```

```text
$ javac -d out mx/habil/revisor/Revisor.java
mx/habil/revisor/Revisor.java:3: error: class Principal is public, should be declared in a file named Principal.java
public final class Principal {}
             ^
1 error
```

Uma classe `public` deve morar em um arquivo com o mesmo nome. É outra coincidência que o compilador de fato exige. Nos arquivos compactos das primeiras lições isso não se notava, porque a classe implícita toma o nome do arquivo (`fig02_04.java` define uma classe `fig02_04`).

#### 6.1.3 Visibilidade: o que cada classe enxerga das demais

Com pacotes surge uma pergunta que antes não fazia sentido: uma classe de `mx.habil.otro` pode usar uma de `mx.habil.revisor`? A resposta vem da **visibilidade**, que se escreve com uma palavra no início da declaração. O Java tem quatro níveis, e o curso usa três:

| Escreve-se | Quem a enxerga | Para que se usa |
|---|---|---|
| `private` | somente a classe onde é declarada | os detalhes internos: um campo, um construtor que ninguém deve chamar |
| *(nada)* | qualquer classe do **mesmo pacote** | quase todo o projeto: é a visibilidade “de pacote” |
| `public` | qualquer classe de qualquer pacote | o que se oferece ao mundo: o ponto de entrada |

(O quarto, `protected`, mistura a visibilidade de pacote com a herança entre classes; o curso não usa herança, e ele não aparece.)

A regra é: **a visibilidade mais estreita que funcione**. Não é uma questão de estilo. Cada coisa `public` é uma promessa a quem a usar: se amanhã você a mudar ou a retirar, quebra essa pessoa. Uma classe de pacote você só a promete ao seu próprio pacote, e pode mudá-la sem avisar ninguém. Até agora tudo era de pacote sem que você percebesse: as suas classes não levavam nenhuma palavra na frente. Agora que há mais de um pacote no mundo, o compilador faz isso ser cumprido. Dois arquivos de exemplo: um `Estado` de pacote em `mx.habil.revisor`, e uma classe de outro pacote, `mx.habil.otro`, que quer usá-lo.

```java
package mx.habil.revisor;

enum Estado {
  ARRIBA,
  CAIDO
}
```

```java
package mx.habil.otro;

import mx.habil.revisor.Estado;

class Intruso {
  Estado estado = Estado.ARRIBA;
}
```

Ao compilá-los juntos:

```text
$ javac -d out src/mx/habil/revisor/Estado.java src/mx/habil/otro/Intruso.java
src/mx/habil/otro/Intruso.java:3: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
import mx.habil.revisor.Estado;
                       ^
src/mx/habil/otro/Intruso.java:6: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
  Estado estado = Estado.ARRIBA;
  ^
src/mx/habil/otro/Intruso.java:6: error: Estado is not public in mx.habil.revisor; cannot be accessed from outside package
  Estado estado = Estado.ARRIBA;
                  ^
3 errors
```

Isso é o que acontece quando uma classe de `mx.habil.otro` tenta usar um `Estado` de visibilidade de pacote: `Estado is not public in mx.habil.revisor; cannot be accessed from outside package`. A mensagem é clara, e a solução quase nunca é “colocar `public`”: é perguntar-se se essa outra classe deveria estar usando `Estado`.

No `revisor`, **tudo é de visibilidade de pacote, menos uma classe: `Revisor`**, o ponto de entrada, que é a única coisa de que alguém de fora precisa. Assim fica, por exemplo, o `Estado` do seu projeto na nova casa:

```java
// fig06_03/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

É o mesmo `enum` da lição 3, com uma única linha a mais: o `package` de cima. Não leva nenhuma palavra de visibilidade na frente, então é de pacote, e todas as classes de `mx.habil.revisor` o enxergam e nenhuma de fora. No único lugar onde `public` de fato aparece (o `Revisor`, em 6.2.6) é necessário também um comentário de documentação, escrito entre `/**` e `*/`, que se chama **Javadoc**: é o comentário que as ferramentas do Java (e o seu editor) mostram quando alguém pergunta “o que é isto?”. A regra do projeto a partir desta lição é que toda classe e todo método `public` leva um Javadoc de pelo menos uma frase terminada em ponto; na [Lição 10](10-el-programa-terminado.md) uma ferramenta vai verificá-lo. Como quase tudo é de pacote, esse Javadoc obrigatório fica no mínimo.

#### 6.1.4 `public static void main(String[] args)`: a volta da forma longa

Nas lições 1 a 5 você escrevia `void main()` e nada mais. Agora o `Revisor` volta à forma que você viu em qualquer código Java alheio: uma classe e, dentro dela, um método `main` com três palavras a mais:

- `public`: a JVM o chama de fora, então precisa ser visível para todos.
- `static`: pertence à classe, não a um objeto. Pode ser chamado sem ter criado nenhum.
- `String[] args`: a lista de argumentos que a pessoa escreveu depois do nome do programa na linha de comando (`java ... Revisor um dois`). Hoje não é usada; as Lições 9 e 10 sim.

É justo perguntar por que voltar a uma forma mais longa se o Java 25 admite `void main()`. A resposta honesta tem duas partes. A primeira é que **a forma curta continua funcionando dentro de um pacote**: dá para comprovar, e o programa roda. A segunda é que ela tem uma condição que o `Revisor` não cumpre, e o erro é instrutivo. Uma classe que só guarda um `main` não tem por que deixar que alguém a instancie, então se coloca nela um construtor `private`, que é o que faz `private Revisor() {}`. Mas um `main` de instância precisa que a JVM possa criar o objeto. Esta classe, dentro de um pacote, tem o `main` curto e o construtor privado:

```java
package mx.x;

public final class Prueba {
  private Prueba() {}

  void main() {
    IO.println("hola");
  }
}
```

```text
$ javac -d out mx/x/Prueba.java
$ java -cp out mx.x.Prueba
Error: no non-private zero argument constructor found in class mx.x.Prueba
remove private from existing constructor or define as:
   public mx.x.Prueba()
```

`no non-private zero argument constructor found`: a JVM procura um construtor sem parâmetros que não seja privado, e não há nenhum. A forma clássica não depende disso, funciona com qualquer versão do JDK desde a primeira, e é a que reconhecem todas as ferramentas e todo o código alheio que você vai ler. Para um programa que vai viver anos, esse é o bom motivo: **não depender de uma comodidade nova se a forma antiga não custa nada.** `IO.println` também continua funcionando em qualquer classe, mas o projeto usa `System.out`, que você já conhece da lição 5, porque é o que você vai encontrar por toda parte.


### 6.2 Maven: instalá-lo, sua estrutura e seu `pom.xml`

#### 6.2.1 O que o Maven faz, em quatro trabalhos

O Maven é uma ferramenta de build: recebe um projeto Java e faz com ele, sempre na mesma ordem, o que você fazia à mão. Seus trabalhos são quatro. **Compilar** o código (chama o `javac` por você, com as opções corretas). **Rodar os testes** e parar se algum falhar. **Empacotar** o resultado em um arquivo `.jar` (um arquivo comprimido de classes, parecido com um `.zip`). E **obter as dependências**: quando o seu projeto diz “preciso da biblioteca X versão Y”, o Maven a baixa de um repositório público, o [Maven Central](https://central.sonatype.com/), a guarda no seu computador e a coloca ao alcance do compilador.

O que torna o Maven especial é uma ideia com nome: **convenção sobre configuração**. Em vez de cada projeto contar em um arquivo de configuração onde está o seu código, onde estão os seus testes e onde deixa os resultados, o Maven fixa uma estrutura de pastas que todos usam, e o arquivo do projeto só diz o que foge do habitual. A consequência é a que importa: quem conhece um projeto Maven conhece todos, e as ferramentas (o editor, o servidor de integração contínua, o analisador de código) sabem onde olhar sem que lhes digam. A [introdução oficial](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) conta o mesmo em cinco minutos de leitura.

O Maven não faz o trabalho ele mesmo. É um orquestrador: cada trabalho é feito por uma peça especializada chamada ***plugin*** (o compilador é um, o que roda os testes é outro, o que monta o `.jar` é outro), e o Maven decide quando chamar qual. Essa divisão tem uma consequência prática que você verá no `pom.xml`: cada *plugin* tem a sua própria versão.

#### 6.2.2 Instalar o Maven 3.10.0 com o SDKMAN

O Maven se instala com a mesma ferramenta com que você instalou o Java na [Lição 1](01-instalacion.md): o SDKMAN. Como com o Java, ele trabalha na sua pasta pessoal, sem `sudo` e sem tocar nos programas do sistema, e deixa você escolher a versão exata. O curso fixa o **Maven 3.10.0**, que a [página de download do Maven](https://maven.apache.org/download.cgi) marca como a versão atual e recomendada para todos (pede JDK 8 ou superior para executar; com o seu Temurin 25 sobra). Fixar a versão importa por um motivo que se vê na tela: medido com o Maven 3.9.11 sobre Java 25, ao iniciar ele imprime quatro avisos `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` (vêm de uma biblioteca interna do Maven, não do seu código); a 3.10.0 não os imprime. O que se quer é que a sua saída seja igual à desta lição.

Primeiro, confirme que o SDKMAN a oferece. `sdk list maven` mostra as versões disponíveis; em 13 de outubro de 2026, em um Linux Mint 22.3, a lista trazia a `3.10.0` (e também versões `4.0.0-rc-…`, candidatas da próxima versão maior, que ainda não é definitiva e que o curso não usa). Depois, instale-a com o número completo:

```text
$ sdk install maven 3.10.0
...
Done installing!

Setting maven 3.10.0 as default.
```

Diferentemente do Java, aqui você mesmo escreve o número tal qual: `3.10.0` é a versão do curso, e não muda ainda que saiam outras. A última linha diz que ela fica como padrão. Se, em vez disso, o SDKMAN **perguntar** se você quer que seja a padrão, responda que sim, com `Enter` ou `Y`; e se você respondeu que não, resolve com `sdk default maven 3.10.0`. Agora, como na Lição 1, **abra um terminal novo**, para que ele pegue as mudanças do SDKMAN, e confirme:

```text
$ mvn --version
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
Maven home: ~/.sdkman/candidates/maven/current
Java version: 25.0.3, vendor: Eclipse Adoptium, runtime: ~/.sdkman/candidates/java/25.0.3.0+9-tem
...
OS name: "linux", ... arch: "amd64"
```

Essa é a saída medida em 13 de outubro de 2026 no Mint 22.3 (abreviada: as reticências marcam o que foi retirado, e os caminhos foram encurtados com `~`; o Maven os imprime completos, como `/home/<seu usuário>/.sdkman/candidates/maven/current`). Na sua, você deve reconhecer três coisas. **`Apache Maven 3.10.0`** na primeira linha: a versão do curso. **`Java version: 25.x`**, com o patch que você tiver, e **`vendor: Eclipse Adoptium`**: o Maven está usando o seu Temurin 25 (Adoptium é o projeto que publica o Temurin). E os dois caminhos, `Maven home` e `runtime`, dentro de `~/.sdkman/candidates/`: as duas ferramentas vêm do SDKMAN. Se `Java version` disser algo diferente de 25, o Maven está usando outro JDK: ou o terminal é antigo, ou há uma variável `JAVA_HOME` apontando para outro lado. O SDKMAN mantém essa variável em dia quando você troca de versão com `sdk default` ou `sdk use` (o quadro da Lição 1, 1.1.7); confira com `echo $JAVA_HOME`.

#### 6.2.3 A estrutura padrão de pastas

Com o Maven instalado, falta dar a ele um projeto com a forma que espera. Esta é, e é igual em todos os projetos Maven do mundo:

```text
revisor/
  pom.xml                         ← la descripción del proyecto
  servicios.txt                   ← la configuración del revisor (sigue en la raíz)
  src/
    main/java/mx/habil/revisor/   ← el código del programa
    test/java/mx/habil/revisor/   ← el código de las pruebas
  target/                         ← lo que Maven genera; se puede borrar entero
```

Cada pasta tem um único papel. **`src/main/java`** é o código do programa, com uma subpasta para cada ponto do nome do pacote, como em `out` há pouco. **`src/test/java`** é o código dos testes, com **a mesma estrutura de pacotes** que o programa: o teste de `Clasificador`, que você escreverá na Lição 7, será `mx.habil.revisor.ClasificadorTest`. Que os testes morem no mesmo pacote não é coincidência nem enfeite: é o que lhes permite enxergar as classes de visibilidade de pacote que o programa tem fechadas para o mundo (6.1.3). Um teste que tivesse que pedir `public` para tudo obrigaria a abrir o programa só para poder testá-lo.

**`target/`** é o que o Maven produz: os `.class` do programa em `target/classes`, os dos testes em `target/test-classes`, os relatórios dos testes e o `.jar`. É uma pasta **descartável**: pode ser apagada inteira a qualquer momento e se regenera com um comando, porque tudo o que ela contém sai do que está em `src`. Daí duas regras: nunca se edita nada dentro de `target`, e nunca se sobe para um repositório (se algum dia você guardar este projeto com o Git, adicione `target/` ao seu arquivo `.gitignore`). `servicios.txt` fica na raiz porque o programa o procura por um caminho relativo, a partir da pasta onde é executado, e você sempre executará a partir da raiz do projeto.

#### 6.2.4 O `pom.xml`, linha por linha

O arquivo que descreve o projeto se chama `pom.xml` (*POM* é a sigla de “modelo de objetos do projeto”; `.xml` é o formato). **XML** é um formato de texto feito de tags aninhadas: cada elemento se abre com `<nome>` e se fecha com `</nome>`, e o que vai dentro é o seu valor ou os seus filhos. É mais barulhento que o JSON da Lição 8, mas é o que o Maven escolheu e você não vai escrevê-lo do zero com muita frequência; vai lê-lo, e por isso convém entendê-lo uma vez por completo. Este é o do projeto, completo:

```xml fig06_03/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.6.0</version>

  <properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

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

**Os dados do projeto.** `<modelVersion>4.0.0</modelVersion>` é a versão do formato do próprio `pom.xml`; é sempre `4.0.0` e se escreve sem pensar. Os três que se seguem, `groupId`, `artifactId` e `version`, são as **coordenadas** do projeto, o endereço com que qualquer projeto Maven do mundo pode se referir a este. O `groupId` é quem o publica (`mx.habil`, igual ao nome do pacote); o `artifactId` é o nome do projeto (`revisor`); e a `version` diz em que ponto ele está. Aqui é `0.6.0` porque este é o estado do curso na Lição 6; cada lição vai subi-la, e a 10 a deixará em `1.0.0`. As coordenadas parecem um trâmite até que na Lição 7 você declare a sua primeira dependência: uma dependência **é** um `groupId`, um `artifactId` e uma `version`. E decidem algo que você verá em 6.3: o nome do `.jar`.

**As propriedades.** `<properties>` agrupa valores com nome que outras partes do arquivo (ou dos *plugins*) leem. Duas são indispensáveis. `maven.compiler.release` em `25` diz ao compilador “compile como Java 25”, o que inclui uma garantia que o velho par `source`/`target` não dava: com `release`, o compilador não só gera o formato da versão pedida, como **rejeita qualquer classe do JDK que não existisse nessa versão**. E serve de aviso antecipado; isto é o que acontece se alguém tenta construir o projeto com um JDK 21 instalado:

```text
$ mvn -B compile
[ERROR] BUILD FAILURE
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile (default-compile) on project revisor: Fatal error compiling: error: release version 25 not supported -> [Help 1]
```

`release version 25 not supported`: o JDK que o Maven usa não sabe compilar para o Java 25. É o erro que vê quem tem instalado o `default-jdk` do Mint (Java 21, [Lição 1](01-instalacion.md)) e quem segue um tutorial com outra versão: em vez de falhar pela metade com um erro estranho, o projeto se recusa desde o primeiro segundo e diz por quê. A outra propriedade, `project.build.sourceEncoding`, em `UTF-8`, declara a codificação dos seus arquivos `.java`; escreve-se explícita para que o resultado não dependa do computador onde se compile, e para que os acentos de `"sin conexión"` (“sem conexão”) sejam sempre os mesmos.

**Os *plugins*.** Dentro de `<build>`, `<plugins>` declara os três de que o projeto precisa hoje: o compilador (`maven-compiler-plugin`), o que roda os testes (`maven-surefire-plugin`, que hoje não tem testes para rodar e que a Lição 7 põe para trabalhar) e o que monta o `.jar` (`maven-jar-plugin`). Cada um traz a sua `<version>` escrita, e é uma decisão, não um descuido. O Maven pode funcionar sem ela, porque traz uma versão por padrão de cada *plugin* essencial; isto é o que acontece se você tira a versão de um:

```text
$ mvn -B verify
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for mx.habil:revisor:jar:0.6.0
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-surefire-plugin is missing.
```

O Maven avisa que a versão falta e que “versões futuras do Maven poderiam deixar de aceitar projetos assim”. Há um motivo mais prático: **se você não fixa a versão, o seu build muda quando o Maven muda**, não quando você decide. O mesmo projeto, o mesmo comando, e um resultado diferente no dia em que alguém atualiza a ferramenta. Fixar a versão de cada peça é o que torna um build reproduzível: o guia do Maven sobre [builds reproduzíveis](https://maven.apache.org/guides/mini/guide-reproducible-builds.html) trata desse problema e começa pedindo que os *plugins* estejam em versões concretas e em dia. O mesmo valerá para as dependências.

Um detalhe a mais antes de usá-lo. O `pom.xml` de cima não leva a declaração `<?xml ... ?>` que aparece em quase todos os exemplos da internet. É opcional, e retirá-la evita um erro clássico: essa declaração tem que ser a primeira coisa do arquivo, sem um comentário nem um espaço antes, e quem a escreve e depois adiciona um comentário em cima obtém um arquivo inválido com uma mensagem que não ajuda.

#### 6.2.5 O ciclo de vida

O Maven não executa *plugins* soltos, executa **fases**, e cada fase chama os *plugins* que lhe correspondem. As fases formam uma cadeia em ordem fixa, o **ciclo de vida** ([documentação oficial](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)), e há uma regra simples que o governa: **ao pedir uma fase, o Maven executa primeiro todas as anteriores.** As que você usará no curso são quatro:

| Fase | O que faz | O que deixa |
|---|---|---|
| `compile` | compila o código de `src/main/java` | `target/classes` |
| `test` | compila os testes e os roda (até a Lição 7 não há nenhum) | `target/test-classes` e `target/surefire-reports` |
| `package` | monta o `.jar` com o que foi compilado | `target/revisor-0.6.0.jar` |
| `verify` | verificações finais sobre o pacote (por ora, nenhuma a mais) | — |

Pela regra de cima, `mvn verify` compila, testa e empacota; `mvn test` compila e testa; e `mvn compile` só compila. No curso se usa `verify` como “o comando completo”: na Lição 10 você acrescentará a ele as verificações de qualidade, e o comando que você já conhece não mudará. E sempre com `-B` (*batch*, “em lote”): diz ao Maven que não use cores nem barras de progresso nem faça perguntas, que é a forma de pedir uma saída limpa que se possa copiar, colar em uma mensagem ou ler em um servidor sem tela.

#### 6.2.6 A migração do `revisor` para um projeto

É hora de fazer o que a árvore de pastas mostra. São três passos e um aprimoramento opcional, e nenhum muda o que o programa faz. A partir de `~/curso-java/revisor/`:

```text
$ mkdir -p src/main/java/mx/habil/revisor src/test/java/mx/habil/revisor
$ mv *.java src/main/java/mx/habil/revisor/
```

`mkdir -p` cria uma pasta e todas as que faltarem no seu caminho. `mv` move os arquivos; tome cuidado de executá-lo a partir de `revisor/` e de que `servicios.txt` **não** seja movido (o `*.java` não o toca). Agora cada arquivo precisa da sua linha `package`. Fazer isso à mão em treze arquivos é um trabalho chato, então este laço do terminal o faz por você: para cada arquivo, escreve a linha nova e depois o conteúdo original em um arquivo temporário, e o renomeia por cima do original:

```text
$ for f in src/main/java/mx/habil/revisor/*.java; do { printf 'package mx.habil.revisor;\n\n'; cat "$f"; } > "$f.nuevo" && mv "$f.nuevo" "$f"; done
```

Confira abrindo qualquer um: a primeira linha deve ser `package mx.habil.revisor;`, seguida de uma linha em branco. Restam duas coisas que o terminal não pode fazer por você, porque exigem entender o código.

**A primeira é o `Revisor`.** O seu `Revisor.java` da lição 5 era um arquivo compacto: um `main` sem classe ao redor. Agora é uma classe, a única pública do projeto, com o seu construtor privado (6.1.3) e o `main` da forma longa (6.1.4). É curto, então vai completo:

```java
// fig06_03/src/main/java/mx/habil/revisor/Revisor.java

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

Repare no que **não** mudou: o corpo do `try` é o da lição 5, linha por linha. Lê a configuração, verifica os serviços na ordem da lista, monta o relatório, imprime a tabela; e se a configuração está errada, o `catch` imprime a mensagem em espanhol pela saída de erro. O único novo é o invólucro, e três coisas que mudam de significado: o arquivo compacto enxergava todo o módulo `java.base` sem pedir, e uma classe normal precisa dos seus próprios `import` (por isso aparecem `Path` e `List`); o `main` agora é o que a JVM chama (e por isso deve ser `public static`); e as classes que antes ele enxergava “porque estavam na mesma pasta” agora as enxerga porque estão no mesmo pacote.

**A segunda é uma possibilidade que o pacote abre, e é opcional.** Nas lições 3 a 5, sem pacote, cada vez que `Respondio` era necessário tinha que escrevê-lo como `Respuesta.Respondio`, porque **de um pacote sem nome não se pode importar nada** (se você tentar, o compilador diz `package R does not exist`). Com pacote, o `import mx.habil.revisor.Respuesta.Respondio;` funciona, sim, e o código pode dizer apenas `Respondio`. Os testes que você escreverá na Lição 7 o usam; o código do programa, tal como você o traz da lição 5, continua funcionando sem tocá-lo, e você pode deixá-lo assim.

#### 6.2.7 O primeiro build

Com o projeto migrado, este é o primeiro build. A partir da raiz (`~/curso-java/revisor/`, onde está o `pom.xml`), e com o `pom.xml` de cima, que ainda não tem testes:

```text
$ mvn -B compile
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------------------< mx.habil:revisor >--------------------------
[INFO] Building revisor 0.6.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.5.0:resources (default-resources) @ revisor ---
[INFO] skip non existing resourceDirectory ~/curso-java/revisor/src/main/resources
[INFO] 
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

Leia de cima para baixo, porque cada linha `--- plugin:versão:objetivo ---` diz qual peça trabalhou. `resources:resources` copia arquivos que não são código (aqui não há: “skip non existing resourceDirectory”). `compiler:compile` faz o trabalho: `Compiling 13 source files with javac [debug release 25] to target/classes` diz quantos arquivos compilou, com quais opções (`debug` significa que guarda os nomes das variáveis, o que explica o parágrafo seguinte) e onde deixou os `.class`. E `BUILD SUCCESS` é o aviso de que nenhuma fase falhou. Na primeira vez que você rodar o Maven verá também muitas linhas `Downloading` que aqui foram retiradas: o Maven baixa os seus próprios *plugins* do Maven Central e os guarda em `~/.m2/repository`, uma pasta oculta da sua casa que funciona como **cache local**. Isso explica duas coisas: a primeira execução é lenta e precisa de internet, e as seguintes são rápidas e funcionam sem conexão.

Com as classes em `target/classes`, o programa se executa com o mesmo `-cp` de há pouco, agora com a raiz das classes do Maven:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

É a tabela de sempre, com o mesmo resumo: `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms` (“4 serviços · 2 no ar · 1 lento · 1 fora do ar · média 843ms”). Esse número o programa calcula a cada vez, e os testes da Lição 7 vão vigiá-lo.

O `debug` da linha de `Compiling` tem um prêmio escondido que se conecta com a [Lição 2](02-tipos-cadenas-control.md): a mensagem de `NullPointerException` que com `java Revisor.java` dizia `"<local1>"` (o lançador compila sem essa informação) agora diz o nome da variável. Com uma classe de teste, compilada pelo Maven, que faz `detalle.isBlank()` sobre um `null`:

```text
$ java -cp target/classes mx.habil.revisor.Nulo
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.isBlank()" because "detalle" is null
	at mx.habil.revisor.Nulo.main(Nulo.java:6)
```

`because "detalle" is null`: o nome da variável que causou o erro. O Maven compila com informação de depuração por padrão, e por isso as mensagens dos programas do projeto são mais fáceis de ler que as dos arquivos soltos.

#### 6.2.8 Três verificações antes de seguir

Se você chegou até aqui, o projeto já compila e se executa. Confira três coisas, nesta ordem:

1. `mvn -B compile` termina em `BUILD SUCCESS` e diz `Compiling 13 source files`.
2. `java -cp target/classes mx.habil.revisor.Revisor` imprime a tabela de quatro serviços com o resumo de 843 ms.
3. Executado a partir de outra pasta, o mesmo comando não encontra `servicios.txt`: é o comportamento correto, e você vai vê-lo em “O erro que você vai ver”.

Se alguma falhar, o erro é quase sempre das mesmas três famílias: um arquivo sem a sua linha `package` (o compilador diz que não encontra uma classe), um arquivo na pasta errada (6.1.2), ou um comando executado fora da raiz do projeto, onde não há `pom.xml`:

```text
$ cd ~/curso-java
$ mvn -B verify
[ERROR] BUILD FAILURE
[ERROR] The goal you specified requires a project to execute but there is no POM in this directory (~/curso-java). Please verify you invoked Maven from the correct directory. -> [Help 1]
```

O Maven diz com todas as letras: não há POM na pasta onde você está. O que falta é fazer `cd` até a raiz do projeto.


### 6.3 O ciclo completo e o `.jar`

#### 6.3.1 O que `mvn -B verify` faz hoje

Até aqui você usou `compile`, a primeira fase. Agora peça a última, a que você vai escrever pelo resto do curso. A partir da raiz do projeto:

```text
$ mvn -B verify
[INFO] Building revisor 0.6.0
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] 
[INFO] --- resources:3.5.0:testResources (default-testResources) @ revisor ---
[INFO] skip non existing resourceDirectory ~/curso-java/revisor/src/test/resources
[INFO] 
[INFO] --- compiler:3.16.0:testCompile (default-testCompile) @ revisor ---
[INFO] No sources to compile
[INFO] 
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] No tests to run.
[INFO] 
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.6.0.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

Leia com a tabela de 6.2.5 ao lado, porque cada bloco `--- plugin:versão:objetivo ---` é uma fase trabalhando, na ordem da cadeia. Primeiro, a fase `compile`, igual a 6.2.7: treze arquivos para `target/classes`. Depois vêm três linhas que são da fase `test` e que ainda não têm trabalho. `testResources` procura arquivos auxiliares para os testes em `src/test/resources` e não há. `testCompile` é o mesmo compilador, mas aplicado a `src/test/java`, e diz `No sources to compile`: a pasta de testes que você criou em 6.2.6 existe e está vazia. E `surefire:test`, o *plugin* que roda os testes, diz `No tests to run.`, porque não há nenhum. Por último, a fase `package` chama `jar:jar`, que monta o arquivo `target/revisor-0.6.0.jar`. A fase `verify` não acrescenta nada ainda (a Lição 10 acrescentará a ela as verificações de qualidade), e o Maven fecha com `BUILD SUCCESS`.

Dois detalhes merecem um segundo olhar. O primeiro é o nome do `.jar`: `revisor-0.6.0.jar` você não escreveu em lugar nenhum, o Maven o monta com o `artifactId` e a `version` do `pom.xml`. Quando a versão subir, o nome do arquivo subirá com ela; por isso as coordenadas não são um trâmite. O segundo é a linha do *Surefire*. Repare no que aconteceu: o Maven não rodou nem um único teste e terminou em verde. Hoje isso é a verdade, porque você não escreveu testes, e o relatório diz isso com todas as letras. Na Lição 7 você vai ver que um verde sem testes também pode aparecer quando você **de fato** escreveu testes, sem esse aviso, e por que o número de testes se lê sempre do relatório e nunca do `BUILD SUCCESS`. Guarde esta imagem: `No tests to run.` é o que o Maven diz quando sabe que não há testes.

#### 6.3.2 O que há dentro do `.jar`

Um arquivo `.jar` (de *Java ARchive*) é um arquivo comprimido com o mesmo formato de um `.zip`, que guarda classes compiladas e um pouco de informação sobre elas. O JDK traz uma ferramenta para manuseá-lo, que se chama igual, `jar`, e sua opção `tf` lista o conteúdo (`t` é “lista”, `f` é “do arquivo que eu digo”):

```text
$ jar tf target/revisor-0.6.0.jar
META-INF/
META-INF/MANIFEST.MF
mx/
mx/habil/
mx/habil/revisor/
META-INF/maven/
META-INF/maven/mx.habil/
META-INF/maven/mx.habil/revisor/
mx/habil/revisor/Clasificador.class
mx/habil/revisor/Configuracion.class
mx/habil/revisor/ErrorDeConfiguracion.class
mx/habil/revisor/Estado.class
mx/habil/revisor/Formato.class
mx/habil/revisor/Reporte.class
mx/habil/revisor/Respuesta$Respondio.class
mx/habil/revisor/Respuesta$SinRespuesta.class
mx/habil/revisor/Respuesta.class
mx/habil/revisor/Resultado.class
mx/habil/revisor/Resumen.class
mx/habil/revisor/Revision.class
mx/habil/revisor/Revisor.class
mx/habil/revisor/Servicio.class
mx/habil/revisor/Simulador.class
META-INF/maven/mx.habil/revisor/pom.xml
META-INF/maven/mx.habil/revisor/pom.properties
```

Há três grupos. O maior são as suas classes, e vêm **com as pastas do seu pacote**: `mx/habil/revisor/Clasificador.class`, exatamente a forma que o `javac -d out` produziu em 6.1.2 e que a JVM exige para encontrar uma classe pelo seu nome completo. Um `.jar` é, no fundo, uma pasta `target/classes` metida em um único arquivo. Conte as classes: são quinze, de treze arquivos-fonte. As duas a mais são `Respuesta$Respondio.class` e `Respuesta$SinRespuesta.class`: os dois *records* que vivem **dentro** da interface `Respuesta` (Lição 3). Cada classe ou *record* aninhado é compilado para o seu próprio `.class`, com o nome da classe que o contém, um `$` e o seu nome.

O segundo grupo é `META-INF/MANIFEST.MF`, o **manifesto**: um arquivo de texto com dados sobre o `.jar` que a JVM lê. O terceiro, `META-INF/maven/...`, é uma cópia do `pom.xml` e um arquivo com as coordenadas; o Maven o adiciona para que quem encontrar o `.jar` solto saiba de qual projeto saiu e em qual versão. O que **não** está também importa: nenhum `.java` sequer (o `.jar` leva o compilado, não a fonte), nem `servicios.txt` (está na raiz do projeto, não em `src/main/resources`, então não faz parte do programa), nem nada de JUnit nem de testes.

#### 6.3.3 Executar a partir do `.jar`, e o que ainda falta

Como o `.jar` contém o mesmo que `target/classes`, serve igualmente como *classpath*:

```text
$ java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

A mesma tabela, agora a partir de um único arquivo que você poderia copiar para outro computador com Java 25. Existe uma forma mais curta, `java -jar arquivo.jar`, que é a que usam os programas Java que se entregam a outras pessoas. Experimente:

```text
$ java -jar target/revisor-0.6.0.jar
no main manifest attribute, in target/revisor-0.6.0.jar
```

“Não há atributo principal no manifesto”. Com `-jar`, a JVM não recebe o nome da classe que deve iniciar: ela o procura no manifesto, em uma linha chamada `Main-Class`. E o manifesto que o Maven montou não a traz. Você pode vê-lo, porque `unzip -p` imprime um arquivo de dentro do `.jar` sem extraí-lo:

```text
$ unzip -p target/revisor-0.6.0.jar META-INF/MANIFEST.MF
Manifest-Version: 1.0
Created-By: Maven JAR Plugin 3.5.1
Java-Version: 25
Build-Jdk-Spec: 25
```

Quatro linhas: a versão do formato, quem o criou, e com qual Java. Nada diz qual é a classe de entrada. (Se a sua JVM estiver em espanhol, a mensagem de erro sai traduzida; medido com a JVM em espanhol do México: `no hay ningún atributo de manifiesto principal en target/revisor-0.6.0.jar`. O código de saída é 1 nos dois idiomas.) Seria possível adicionar essa linha com a configuração do *plugin* `jar`, mas isso não resolveria o problema de fundo, que aparece na Lição 8: quando o programa usar uma biblioteca, o `.jar` terá as suas classes e não as da biblioteca, e a JVM não as encontrará. A [Lição 10](10-el-programa-terminado.md) resolve as duas coisas de uma vez com um `.jar` que leva tudo dentro e sabe por onde começar. Enquanto isso, o comando do curso é o do `-cp`, que funciona com `target/classes` e com o `.jar` igualmente.

#### 6.3.4 `target/` pode ser jogado fora

Você já tem à vista tudo o que vive em `target/`: `classes` com os `.class`, o `.jar`, e umas pastas de trabalho do Maven (`maven-status`, `maven-archiver`, `generated-sources`) que guardam dados para a próxima execução. **Tudo isso sai de `src/` e do `pom.xml`**, e por isso pode ser apagado inteiro a qualquer momento: o próximo `mvn -B verify` o cria de novo igual. Essa é a prova de que um projeto está bem montado. Se algum dia você apagar `target/` e o projeto já não construir, havia algo importante onde não devia estar.

Daí saem as duas regras que você já viu em 6.2.3, que convém repetir porque são as que mais se quebram. **Nunca se edita nada dentro de `target/`**: uma mudança ali se perde no próximo build, e o pior é que enquanto isso parece que funcionou. E **nunca se guarda `target/` em um repositório**: você guardaria o resultado e não a fonte, e cada pessoa que construísse o projeto veria mudanças em arquivos que não tocou. O que se guarda é `src/`, `pom.xml` e aquilo de que o programa precisa para rodar, como `servicios.txt`.


### 6.4 O passo do projeto: o `revisor` como projeto Maven

Você tem todas as peças. Este é o projeto completo, tal como fica ao terminar a lição. A maior parte você já conhece: do programa, só o `Revisor` (6.2.6) mudou de verdade em relação à Lição 5, e as demais classes apenas ganharam a sua linha `package`. Incluem-se todas para que o projeto possa ser montado sem olhar outra lição:

```text
revisor/
  pom.xml                 versión 0.6.0 · release 25 · tres plugins con versión fija
  servicios.txt           sin cambios
  src/main/java/mx/habil/revisor/
    Revisor.java          NUEVO como clase pública: el único punto de entrada
    Estado.java  Respuesta.java  Servicio.java  Resultado.java  Clasificador.java
    Formato.java  Simulador.java  Revision.java  Resumen.java  Reporte.java
    Configuracion.java  ErrorDeConfiguracion.java       (solo ganan su línea package)
  src/test/java/mx/habil/revisor/                       (vacía: la llena la Lección 7)
```

Você já viu completos o `pom.xml` (6.2.4), `Revisor.java` (6.2.6) e `Estado.java` (6.1.3). O seu `servicios.txt` é o de sempre:

```text fig06_03/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

E estes são os onze arquivos restantes. Se você acompanhou as lições 3 a 5, já os tem, e a única diferença é o que foi dito em 6.2.6; eu os incluo para que o projeto desta lição possa ser construído sem depender de mais nada.

<details>
<summary>Os onze arquivos do programa que só mudam na primeira linha</summary>

```java
// fig06_03/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig06_03/src/main/java/mx/habil/revisor/Servicio.java

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
// fig06_03/src/main/java/mx/habil/revisor/Resultado.java

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
// fig06_03/src/main/java/mx/habil/revisor/Clasificador.java

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
// fig06_03/src/main/java/mx/habil/revisor/Simulador.java

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
// fig06_03/src/main/java/mx/habil/revisor/Revision.java

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
// fig06_03/src/main/java/mx/habil/revisor/Resumen.java

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
// fig06_03/src/main/java/mx/habil/revisor/Reporte.java

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
// fig06_03/src/main/java/mx/habil/revisor/Formato.java

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
// fig06_03/src/main/java/mx/habil/revisor/Configuracion.java

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
// fig06_03/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

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

Para terminar a lição: `mvn -B verify` deve dizer `Compiling 13 source files`, `No tests to run.` e `BUILD SUCCESS`, e deixar `target/revisor-0.6.0.jar`; `java -cp target/classes mx.habil.revisor.Revisor` e `java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor` devem imprimir os dois a tabela com o resumo da Lição 4 (4.5) de 843 ms. Há um artigo da Hábil sobre [como se monta a integração contínua de uma casa de software](https://www.habil.mx/pt/blog/cicd-devsecops-infraestrutura-propria/), o lugar onde `mvn -B verify` se executa sozinho a cada mudança; vale a pena lê-lo agora que o comando já é seu.


## O erro que você vai ver

A lição tem um erro principal, o da classe que a JVM não encontra, e várias mensagens de apoio que convém saber ler num relance.

**O erro principal.** É o de 6.1.2, e você vai vê-lo assim que executar um programa com pacote da forma errada:

```text
$ java -cp out Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola
```

Lê-se em três passos, sempre os mesmos. **O que procurava**: o nome depois de `class`, aqui `Hola` simplesmente. **Onde procurou**: no *classpath* que você deu com `-cp`, aqui `out`. **O que encontrou**: nada (`ClassNotFoundException`), ou um arquivo com outro nome verdadeiro (`NoClassDefFoundError ... wrong name: mx/habil/revisor/Hola`). Com essas três respostas o conserto é sempre um de dois: escrever o nome completo da classe, com o seu pacote, ou executar a partir da raiz das classes (`out`, `target/classes` ou o `.jar`). No `revisor`, a forma correta é uma só e não muda no resto da lição: `java -cp target/classes mx.habil.revisor.Revisor`, a partir da raiz do projeto.

**As mensagens de apoio**, para reconhecê-las num relance:

- `package org.junit.jupiter.api does not exist` (`fig06_01.java`): falta uma dependência. O lançador não sabe de dependências; no Maven elas se declaram no `pom.xml`, e a Lição 7 declara justamente essa.
- `cannot find symbol` com `symbol: variable Clasificador`, ao compilar procurando por pasta: um arquivo está em uma pasta que não corresponde ao seu pacote. A pasta tem que dizer a verdade (6.1.2).
- `Estado is not public in mx.habil.revisor; cannot be accessed from outside package`: você pediu uma classe de visibilidade de pacote a partir de outro pacote. Antes de colocar `public` nela, pergunte-se se a outra classe deveria estar usando-a.
- `no non-private zero argument constructor found`: um `main` de instância em uma classe com construtor privado. A forma longa `public static void main(String[] args)` não tem esse problema (6.1.4).
- `The goal you specified requires a project to execute but there is no POM in this directory`: você executou `mvn` fora da raiz do projeto. Entre na pasta que contém o `pom.xml`.
- `release version 25 not supported`: o Maven usa um JDK mais antigo que o 25. Confira a linha `Java version` de `mvn --version`, a variável `JAVA_HOME` e, se houver vários JDK, o quadro da Lição 1 sobre `sdk default`.
- `no main manifest attribute`: você tentou `java -jar` com um `.jar` que não diz qual é a sua classe de entrada. Use `java -cp` com o nome da classe; a Lição 10 resolve isso.
- `error de configuración: no existe el archivo servicios.txt` (“erro de configuração: o arquivo servicios.txt não existe”) ao executar a partir de outra pasta, que é o último e o que mais se repete:

```text
$ cd ..
$ java -cp revisor/target/classes mx.habil.revisor.Revisor
error de configuración: no existe el archivo servicios.txt
```

O programa procura `servicios.txt` por um caminho **relativo**, `Path.of("servicios.txt")`, e um caminho relativo significa “o arquivo da pasta onde estou parado”, não “o que está ao lado do programa”. A partir da pasta de cima ele não existe. Repare no que o programa fez bem: não mostrou uma exceção de trinta linhas nem um `NoSuchFileException`, e sim a mensagem em espanhol da Lição 5. Para saber em que pasta você está, o comando `pwd` diz. Na Lição 9 o primeiro argumento da linha de comando deixará você indicar o arquivo sem depender de onde esteja.

## O que se faz errado

**Instalar o Maven por um caminho diferente do do Java.** Se o Java vem do SDKMAN e o Maven de outro lado (um pacote do sistema, um arquivo descompactado à mão), cada um traz suas próprias ideias de qual JDK usar, e no dia em que não coincidirem o erro não diz por quê. Com as duas ferramentas no SDKMAN, `mvn --version` e `java --version` falam do mesmo Temurin 25.

**Colocar `public` em tudo “para compilar”.** Cada coisa pública é uma promessa a quem a usar, e cada promessa é um custo quando você quiser mudá-la. O erro `is not public` quase sempre significa que algo está no lugar errado, não que falte uma palavra.

**Uma pasta que não coincide com o pacote, “porque compila”.** O Maven a compila (6.1.2), mas quebra tudo o que procura por pasta: o editor, as buscas, a compilação incremental e a próxima pessoa. É um erro que não avisa, e por isso se evita por costume e não pela mensagem.

**Dependências e *plugins* sem versão, ou com a mais recente.** Faz o seu build mudar no dia em que alguém publica uma versão nova ou atualiza o Maven, sem que você tenha tocado em nada. Cada peça leva a sua versão escrita, e se sobe de propósito.

**Editar ou guardar `target/`.** É o que o Maven gera e pode ser apagado e regenerado a qualquer momento (6.3.4). Uma mudança feita ali se perde no próximo build, e se você a sobe para um repositório, está guardando o resultado e não a fonte.

**Executar a partir de qualquer pasta e culpar o programa.** O Maven precisa estar na pasta do `pom.xml`, e o `revisor` precisa estar na pasta de `servicios.txt`: hoje são a mesma, a raiz do projeto. Antes de ler um erro de “não existe”, leia `pwd`.

## Exercícios

### Exercício 1 — A pasta que mente

Mova `Clasificador.java` uma pasta acima da que lhe cabe, para `src/main/java/mx/habil/`, sem tocar na sua linha `package mx.habil.revisor;`. **Antes de executar qualquer coisa, preveja** o que faz cada um destes dois comandos: `javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java` (que compila `Revision` procurando as demais classes por pasta) e `mvn -B compile`. Execute-os e compare. Onde ficou `Clasificador.class` dentro de `target/classes`? Devolva o arquivo à sua pasta e apague a pasta `out` que o `javac` criou.

### Exercício 2 — O que o Maven decide por você

Tire do `pom.xml` a linha `<version>3.5.1</version>` do *plugin* `maven-jar-plugin` e rode `mvn -B verify`. Leia as linhas `WARNING` do início: o que dizem que falta, e onde? Procure depois a linha `--- jar:...` da saída: qual versão do *plugin* o Maven usou? Com essa resposta, explique no seu diário de bordo por que o `pom.xml` fixa a versão mesmo que hoje dê o mesmo. Restaure a linha ao terminar.

### Exercício 3 — O JDK errado

Se você tem um JDK 21 instalado com o SDKMAN além do 25 (os JDK do SDKMAN vivem em `~/.sdkman/candidates/java/`, uma pasta por identificador; `ls ~/.sdkman/candidates/java/` mostra a você), aponte o Maven para ele só para um comando, colocando a variável na frente: `JAVA_HOME=~/.sdkman/candidates/java/<o identificador do 21> mvn -B compile`. **Antes, preveja** em qual fase falha e com qual mensagem. Se você não tem um 21, não o instale para isto: leia a solução. Nos dois casos, responda: qual propriedade do `pom.xml` produz o erro, e por que é melhor que falhe ali e não mais tarde?

### Exercício 4 — O que há no seu `.jar`

Rode `jar tf target/revisor-0.6.0.jar` e conte os arquivos que terminam em `.class`. **Antes de contar, preveja** quantos deveria haver, sabendo que o projeto tem treze arquivos `.java`. Se o número não coincidir com a sua previsão, encontre os que sobram e explique de onde saem. Depois tente `java -jar target/revisor-0.6.0.jar` e explique a mensagem com o que você viu no manifesto.

## Soluções

### Solução 1

Com `Clasificador.java` uma pasta acima, os dois comandos se comportam de forma diferente:

```text
$ javac -sourcepath src/main/java -d out src/main/java/mx/habil/revisor/Revision.java
src/main/java/mx/habil/revisor/Revision.java:14: error: cannot find symbol
        new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
                                ^
  symbol:   variable Clasificador
  location: class Revision
1 error
$ mvn -B compile
...
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] BUILD SUCCESS
```

O `javac` com `-sourcepath` procura cada classe de que precisa **por pasta**: para `mx.habil.revisor.Clasificador` olha em `src/main/java/mx/habil/revisor/Clasificador.java`, não o encontra e conclui que não existe. O Maven, em contrapartida, entrega ao `javac` a lista completa de arquivos de `src/main/java`, encontra os treze e compila sem reclamar. E `Clasificador.class` fica em `target/classes/mx/habil/revisor/`, **onde diz o seu pacote**, não onde estava o `.java`: o compilador acomoda a saída segundo a linha `package`. Por isso o erro não se vê no Maven e sim em qualquer ferramenta que procure por pasta. O programa roda igual; o projeto, em contrapartida, já mente.

### Solução 2

Sem a versão, o Maven constrói, mas avisa no início:

```text
$ mvn -B verify
[INFO] Scanning for projects...
[WARNING] 
[WARNING] Some problems were encountered while building the effective model for mx.habil:revisor:jar:0.6.0
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-jar-plugin is missing. @ line 25, column 15
[WARNING] 
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[WARNING] 
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.
...
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
...
[INFO] BUILD SUCCESS
```

Diz o que falta (a versão do *plugin* `maven-jar-plugin`) e onde (`@ line 25, column 15`, a linha do `pom.xml` onde começa esse *plugin*; se o seu arquivo tem outra forma, o número muda). E a resposta à segunda pergunta é a que torna o exercício interessante: o Maven usou **a mesma 3.5.1** que o seu `pom.xml` fixava. Hoje, com o Maven 3.10.0, a versão por padrão coincide com a do curso. Mas isso é uma casualidade desta versão do Maven, não uma propriedade do seu projeto: a versão por padrão é decidida por quem publica o Maven, e muda quando o Maven muda. Fixá-la é a diferença entre “hoje dá o mesmo” e “sempre dá o mesmo”. A frase para o diário de bordo: **a versão se fixa para que o resultado não dependa de qual Maven cada um tenha instalado.**

### Solução 3

Com um JDK 21, o Maven inicia, chega à fase `compile` e para ali:

```text
$ JAVA_HOME=<la carpeta del JDK 21> mvn -B compile
...
[INFO] --- compiler:3.16.0:compile (default-compile) @ revisor ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 13 source files with javac [debug release 25] to target/classes
[INFO] ------------------------------------------------------------------------
[ERROR] BUILD FAILURE
...
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile (default-compile) on project revisor: Fatal error compiling: error: release version 25 not supported -> [Help 1]
```

(Medido com um JDK 21 de outra distribuição; a mensagem é do compilador e não depende dela.) A propriedade responsável é `maven.compiler.release` em `25`: o compilador do JDK 21 não sabe produzir classes para o Java 25 e diz isso antes de compilar um único arquivo. É melhor que falhe ali porque a mensagem nomeia a causa (a versão) em vez de um sintoma (um método ou uma sintaxe que “não existe”), e porque o erro aparece na primeira fase, antes que alguém perca tempo com testes ou pacotes pela metade. Note também o que o exercício demonstra: o Maven **não** usa necessariamente o `java` do seu terminal, e sim o de `JAVA_HOME` se estiver definida. Por isso, quando o Maven e `java --version` não coincidem, a primeira coisa é olhar essa variável.

### Solução 4

Há **quinze** arquivos `.class`, não treze:

```text
$ jar tf target/revisor-0.6.0.jar
...
mx/habil/revisor/Respuesta$Respondio.class
mx/habil/revisor/Respuesta$SinRespuesta.class
mx/habil/revisor/Respuesta.class
...
```

Os dois a mais são os *records* aninhados na interface `Respuesta`: cada classe, *record* ou `enum` é compilado para o seu próprio `.class`, e se vive dentro de outra, o seu nome leva na frente o da que a contém e um `$`. Um arquivo `.java` pode produzir vários `.class`, e a relação “um arquivo, uma classe” só é obrigatória para as classes `public` (6.1.2). E o `java -jar` falha com `no main manifest attribute` porque o manifesto que você viu em 6.3.3 não tem a linha `Main-Class`: a JVM, com `-jar`, não sabe por qual classe começar. Com `java -cp target/revisor-0.6.0.jar mx.habil.revisor.Revisor` você diz isso a ela, e funciona.

## Como sei que consegui

- [ ] `mvn --version` diz `Apache Maven 3.10.0` e uma versão de Java que começa com 25.
- [ ] `mvn -B verify`, a partir da raiz de `~/curso-java/revisor/`, diz `Compiling 13 source files`, `No tests to run.` e `BUILD SUCCESS`, e deixa `target/revisor-0.6.0.jar`.
- [ ] `java -cp target/classes mx.habil.revisor.Revisor` imprime a tabela de quatro serviços com o resumo `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`, e o mesmo com o `.jar` no lugar de `target/classes`.
- [ ] A partir da pasta de cima, o mesmo comando com `revisor/target/classes` imprime `error de configuración: no existe el archivo servicios.txt`, e você sabe explicar por quê.
- [ ] Você sabe explicar o que acontece se um arquivo declara `package mx.habil.revisor;` e está em outra pasta, e por que o Maven deixa você fazer isso mas não é uma boa ideia.
- [ ] Você sabe explicar por que há quinze `.class` no `.jar` e por que o `java -jar` ainda não funciona.
- [ ] Você anotou no seu diário de bordo qual mensagem desta lição foi a mais difícil de ler.

## Resumo

Responda sem olhar a lição:

1. Quais três coisas o lançador de arquivos-fonte não pode fazer e que um projeto Maven faz?
2. Que relação tem que haver entre o nome de um pacote, a linha `package` e a pasta do arquivo? Quem verifica isso, e quem não?
3. O que uma classe de visibilidade de pacote enxerga, e o que não? Qual classe do `revisor` é `public`, e por que só essa?
4. Por que o `Revisor` usa `public static void main(String[] args)` e não `void main()`?
5. Com o que você instala o Maven e quais três coisas você reconhece em `mvn --version` para saber que ficou bem?
6. O que são as coordenadas de um projeto Maven? Para que serve fixar a versão de cada *plugin*?
7. Quais fases `mvn verify` executa e em que ordem? O que cada uma deixa em `target/`?
8. O que há dentro de um `.jar`, o que não há, e por que o `java -jar` ainda não o executa?

## Para ler mais

- [Maven, “Introduction to the Build Lifecycle”](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) e [“Introduction to the POM”](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html): as fases, qual *plugin* roda em cada uma, e tudo o que um `pom.xml` pode levar. Em inglês, como quase toda a documentação oficial. Consultado em 7 de outubro de 2026.
- [A especificação da linguagem, capítulo 7: pacotes e módulos](https://docs.oracle.com/javase/specs/jls/se25/html/jls-7.html) e [o tutorial da Oracle sobre pacotes](https://docs.oracle.com/javase/tutorial/java/package/index.html): as regras exatas de nomes, pastas e importações. Consultado em 7 de outubro de 2026.
- [Oracle, “JAR File Specification” (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html) e [o manual do comando `jar`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jar.html): o que um `.jar` leva, o que é o manifesto e para que serve `Main-Class`. Consultado em 13 de outubro de 2026.
- [SDKMAN, uso](https://sdkman.io/usage/): `list`, `install`, `use` e `default`, os mesmos comandos para Java e para Maven. Consultado em 13 de outubro de 2026.

---

**Anterior:** [Lição 5 — Erros e exceções](05-errores-excepciones.md) ·
**Próxima:** [Lição 7 — Testes com JUnit](07-pruebas-junit.md)
