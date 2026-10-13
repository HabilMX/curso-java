# Lição 1 — Instalar o Java 25 no seu Linux Mint

**Tempo:** 90 min (justos: a instalação depende da sua conexão, e se algo falhar nela, sobra menos tempo para o resto da lição; não tenha pressa)

**O que você constrói:** o ambiente de trabalho (um JDK 25 funcionando) e o seu primeiro programa; você deixa criada a pasta `~/curso-java/` com o arquivo `Hola.java` e o primeiro arquivo do projeto, `revisor/Revisor.java`

**O que você aprende:** instalar o SDKMAN e, com ele, o Temurin 25, e conferir a versão em um terminal novo com `java --version` e `javac --version`; por que não se usa o `default-jdk`; o arquivo compacto do Java 25 (`void main()` e `IO.println`); `java Hola.java`; compilar com `javac` e executar com `java` separadamente, e o que é o `.class`; testar ideias com o `jshell`; e ler o primeiro erro do compilador

**De onde você vem.** Da [Lição 0](00-que-es-java.md), onde você viu o caminho do código: fonte, `javac`, *bytecode* e a JVM; a diferença entre JDK e JRE; e o que é LTS. Esta lição pega essa teoria e a transforma em comandos. Você trabalha em um Linux Mint 22 (qualquer 22.x: a 22.3, “Zena”, é a vigente segundo a [página de versões do Linux Mint](https://linuxmint.com/download_all.php), com suporte até abril de 2029), que é baseado no Ubuntu “Noble”.

**O que esta lição não faz.** Não ensina a sintaxe do Java além do que é preciso para o primeiro programa: tipos, strings e controle de fluxo são da [Lição 2](02-tipos-cadenas-control.md). Não instala o Maven (a [Lição 6](06-proyecto-maven.md) faz isso quando for necessário). E não usa um ambiente de desenvolvimento gráfico: tudo é feito com um editor de texto e um terminal, porque assim se vê o que cada peça faz, e porque é isso que você terá em um servidor.

**Sobre as execuções e o que não pudemos conferir.** Os programas desta lição e suas mensagens de erro foram executados com o Temurin 25.0.4 no macOS e comparados caractere por caractere com o que se mostra aqui; os que mostram o Java 21 e o Java 17 foram obtidos com o Temurin 21.0.12 e 17.0.20. No seu Linux Mint devem dar o mesmo, porque são mensagens do compilador e da JVM e não do sistema, mas esses programas não foram executados no Mint. **O que depende do seu Linux Mint** é a instalação, e essa foi medida em 13 de outubro de 2026 em um Linux Mint 22.3 real: o que o terminal responde quando não há Java, o que o instalador do SDKMAN imprime, quais identificadores do Temurin ele oferecia naquele dia, o que o `java --version` diz depois, e a armadilha do terminal que não fica sabendo da mudança. Se algo do que você vê na sua tela difere do que se mostra aqui nesses pontos, anote no seu [diário de bordo](bitacora.md): é informação útil. Os programas estão em [`programas/01-instalacion/`](../programas/01-instalacion), no repositório do curso.

## Ao terminar, você vai conseguir

- Instalar o SDKMAN e, com ele, o Temurin 25, escolhendo o identificador correto do `sdk list java`, e conferir em um terminal novo, com `java --version` e `javac --version`, que são os da versão 25.
- Explicar por que `sudo apt install default-jdk` lhe dá o Java 21 no Mint 22, e que erro isso produz com o arquivo compacto.
- Escrever, executar e modificar um programa Java de um único arquivo com `void main()` e `IO.println`, e rodá-lo com `java Hola.java`.
- Compilar com `javac`, executar com `java`, e dizer o que é o arquivo `.class` que apareceu e quando é preciso compilar de novo.
- Testar expressões soltas no `jshell` e sair dele.
- Ler um erro do compilador: o arquivo, a linha, a seta `^`, e as linhas `symbol` e `location`.
- Criar o primeiro arquivo do projeto, `~/curso-java/revisor/Revisor.java`, e executá-lo.

## O porquê antes do como

Há uma razão para esta lição vir tão cedo no curso: **um ambiente que não funciona torna impossível tudo o que vem depois**, e muitos dos problemas que as pessoas têm com Java nos primeiros dias são problemas de instalação e de versão, não de programação: instalou-se uma versão diferente da que o tutorial supõe, ou há mais de uma instalada e usa-se a errada. Por isso o primeiro passo da lição não é escrever código, mas **descobrir o que você tem**, e o último da parte de instalação é **conferir que versão você está usando**. Todo o resto, o arquivo `Hola.java`, o `javac`, o `jshell`, se constrói sobre a certeza de que o comando `java` do seu terminal é o do Java 25.

Há também uma razão de ânimo: quem chega a este curso já programa, e a primeira coisa de que precisa é um sucesso rápido. O programa que você escreverá na seção 1.2 tem três linhas, e você o executa com um único comando. Antes do Java 25, esse mesmo programa pedia que se declarasse uma classe e um método `public static void main(String[] args)` ao redor; desde o Java 25 é o que você vê abaixo. É a melhor notícia do Java para quem vem de outra linguagem, e por isso o curso a aproveita.

A lição tem sete partes: **instalar o JDK** (1.1), **o seu primeiro programa** (1.2), **compilar e executar separadamente** (1.3), **o `jshell`** (1.4), **ler o primeiro erro** (1.5), **quando a versão é a errada** (1.6) e **o estilo do código** (1.7), e termina com o primeiro passo do **projeto** (1.8). Ao final da lição, isto é o que você verá ao executar o `revisor`, que por ora não verifica nada, mas já existe:

```text
revisor: todavía no revisa nada
```

## Os conceitos

### 1.1 Instalar o JDK

#### 1.1.1 Primeiro: o que você tem

Abra um terminal (no Mint: `Ctrl`+`Alt`+`T`) e faça duas perguntas. A primeira, que sistema é:

```bash
cat /etc/os-release
```

Entre as linhas que ele imprime, procure `VERSION_CODENAME` e `UBUNTU_CODENAME`. No Linux Mint, o primeiro é o nome do Mint (na 22.3, `zena`) e o segundo é o da versão do Ubuntu em que ele se baseia (`noble`). Para instalar o Java com o método deste curso você não vai precisar de nenhum dos dois, mas convém saber: muitas receitas da internet foram escritas para o Ubuntu e descobrem “o nome da versão” com o comando `lsb_release -cs`, que no Mint 22.3 responde `zena`, não `noble` (medido em um Mint 22.3). Uma receita que use essa resposta para montar o endereço de um repositório deixa você apontando para um endereço que não existe. Se algum dia uma receita do Ubuntu falhar no seu Mint, comece por aí.

A segunda pergunta, se já existe Java:

```bash
java --version
```

Pode acontecer uma de três coisas. Se não há Java, o terminal responde que não encontra o comando. Em um Mint 22.3 recém-instalado foi medido `java: not found`, e algo mais útil que o texto: o **código de saída**, o número que todo programa deixa ao terminar e que você vê digitando `echo $?` logo depois. Foi **127**, o número que o terminal reserva para “esse comando não existe”. O texto do aviso pode variar de um terminal para outro; o 127, não. (Se você prefere perguntar sem executar nada, `which java` procura o programa no seu sistema: sem Java não imprime nada e termina com código 1. São dois códigos diferentes para duas perguntas diferentes: “execute isto” e “onde está isto?”.) Se imprimir uma versão que começa com 21 (ou 11, ou 17), alguém instalou o Java antes, talvez sem perceber, porque muitos programas o trazem como dependência; não atrapalha, e em 1.1.7 você verá por que não é preciso removê-lo. E se imprimir 25 e disser `Temurin`, você já tem o que o curso usa e pode pular para 1.1.6 para confirmar que também tem o compilador.

#### 1.1.2 Por que não o `default-jdk`, e por que o SDKMAN

Se você perguntar a um buscador como instalar o Java no Ubuntu ou no Mint, a resposta mais frequente é esta:

```bash
sudo apt install default-jdk
```

**Não faça isso para este curso.** O pacote `default-jdk` não é “a versão mais recente do Java”: é “a versão do Java que a distribuição escolheu como padrão”, e essa escolha congela quando a distribuição é lançada. No Ubuntu Noble, a base do Mint 22, o pacote `default-jdk` depende de `openjdk-21-jdk` (segundo sua [página no arquivo de pacotes do Ubuntu](https://packages.ubuntu.com/noble/default-jdk), consultada em 7 de outubro de 2026, e medido em um Mint 22.3: o `apt` oferece o `default-jdk` em sua versão `2:1.21-75`, a do Java 21). Ou seja, instala o **Java 21**. O Java 21 é uma versão LTS respeitável, mas não a que este curso ensina, e o que importa para a lição é o que ela não consegue fazer: **não executa o arquivo compacto com `void main()`**. Você verá isso em 1.6 com a sua mensagem de erro exata. Quem instala o `default-jdk` e segue esta lição vê o primeiro programa falhar e, sem saber por quê, conclui que o curso está errado. Não está; a versão é a errada.

O curso usa o **Eclipse Temurin 25**, a distribuição com a qual foram medidas todas as suas saídas (a Lição 0 explicou o que é uma distribuição), e o instala com o **SDKMAN**, um gerenciador de versões para ferramentas Java. Três razões pesam na escolha. A primeira: o SDKMAN instala na sua pasta pessoal (`~/.sdkman/`), **sem `sudo`**, sem tocar nos programas do sistema; se algo der errado, você não quebra nada que o Mint use. A segunda: permite ter várias versões do Java ao mesmo tempo e escolher qual usar com um comando, algo de que você vai precisar no dia em que um projeto alheio exigir outra versão. A terceira: com a mesma ferramenta você instalará o Maven na [Lição 6](06-proyecto-maven.md), com a versão exata do curso. Um único método para tudo, o mesmo em cada máquina.

#### 1.1.3 O que o SDKMAN precisa

O SDKMAN é instalado com um script de `bash` que baixa e descompacta arquivos, e para isso usa algumas ferramentas do sistema. Ao rodar, o instalador procura `bash`, `unzip`, `zip`, `tar`, `find`, `curl` e `sed` (medido em 13 de outubro de 2026 em um Linux Mint 22.3). `bash`, `tar`, `find` e `sed` vêm sempre com o Mint. As três que podem faltar são `curl` (o programa que baixa coisas pelo terminal), `zip` e `unzip`. Para conferir:

```bash
which curl zip unzip
```

`which` imprime o caminho de cada programa que encontra e fica calado com o que não encontra. Se você vir os três caminhos, siga em frente. Se faltar algum, instale-o com o gerenciador de pacotes do Mint (esta é a única vez que o curso usa `sudo`, porque estas sim são ferramentas do sistema):

```bash
sudo apt install curl zip unzip
```

`sudo` executa o que vem a seguir com permissões de administrador e pede a sua senha (ao digitá-la você não verá asteriscos; é normal). `apt install` instala os pacotes que você nomeia; se algum já estava instalado, ele avisa e segue com os demais.

#### 1.1.4 Instalar o SDKMAN

O instalador é um script que o próprio projeto publica em seu site oficial. A [página de instalação do SDKMAN](https://sdkman.io/install/) dá este comando:

```bash
curl -s "https://get.sdkman.io" | bash
```

Leia por partes. `curl -s "https://get.sdkman.io"` baixa o script de instalação do site do SDKMAN, por `https` (a conexão criptografada que garante que você fala com esse site e que ninguém mudou o conteúdo no caminho), e `-s` tira a barra de progresso. O pipe `|` passa esse texto para o `bash`, que o executa. Ou seja: você baixa um programa e o roda na mesma linha. Essa forma tem má fama, e convém saber exatamente por quê. O perigoso não é a forma, mas **de onde vem** o que você executa: um script de um site que você não conhece, ou colado de um fórum, pode fazer qualquer coisa com o seu usuário. Aqui a origem é o site oficial do projeto, por `https`, e o script não pede `sudo`: trabalha apenas na sua pasta pessoal. E se você quiser vê-lo antes de executá-lo, pode fazer em dois passos, o que é um bom costume com qualquer instalador: `curl -s "https://get.sdkman.io" -o instalar-sdkman.sh` o guarda em um arquivo, você o lê com `less instalar-sdkman.sh`, e o executa com `bash instalar-sdkman.sh`.

Isto é o que o instalador imprimiu em 13 de outubro de 2026 em um Linux Mint 22.3 (abreviado às linhas que importam: as reticências marcam o que foi retirado, e o caminho da sua pasta pessoal aparece como `~`):

```text
$ curl -s "https://get.sdkman.io" | bash
...
Looking for a previous installation of SDKMAN...
Checking Bash version...
Looking for unzip...
Looking for zip...
Looking for tar...
Looking for find...
Looking for curl...
Looking for sed...
Installing SDKMAN scripts...
...
Set version to 5.23.2 ...
...
Added sdkman init snippet to ~/.bashrc
...
All done!
...
Please open a new terminal, or run the following in the existing one:

    source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Lê-se de cima para baixo: procura uma instalação anterior, confere se você tem uma versão do `bash` que sirva, procura as ferramentas de 1.1.3 uma por uma, instala seus scripts em `~/.sdkman/` e acrescenta ao seu arquivo `~/.bashrc` um trecho que carrega o SDKMAN toda vez que um terminal é aberto. A linha final é a que deve ser obedecida: **o terminal em que você está ainda não sabe que o SDKMAN existe**, porque leu o `~/.bashrc` ao abrir, antes da instalação. Ou você abre um terminal novo, ou pede a este que carregue o SDKMAN agora, com o comando `source` que ele mostra. Depois, confira:

```text
$ sdk version

SDKMAN!
script: 5.23.2
native: 0.7.34 (linux x86_64)
```

Se em vez disso você vir `sdk: command not found`, é que o terminal não carregou o SDKMAN. Abra um novo, ou carregue o SDKMAN à mão neste com `source "$HOME/.sdkman/bin/sdkman-init.sh"` e tente de novo `sdk version`. Se o `source` disser que esse arquivo não existe, a instalação não terminou (quase sempre por uma ferramenta de 1.1.3 que faltava ou por uma queda de conexão): leia a mensagem do instalador, corrija a causa e rode de novo o comando de instalação. Os números de versão serão os do seu dia; o que importa é que o `sdk` responda.

#### 1.1.5 Escolher e instalar o Temurin 25

O SDKMAN não sabe qual Java você quer: você o escolhe pelo seu **identificador**. Para ver os que ele oferece:

```bash
sdk list java
```

Imprime uma tabela longa, com uma seção por distribuição (Corretto, GraalVM, Liberica, Temurin, Zulu e outras). Procure a do **Temurin**: seus identificadores terminam em **`-tem`**. Em 13 de outubro de 2026, em um Mint 22.3, essa seção trazia estes: `27.0.0.0+35-tem`, `26.0.2+1.1-tem`, `25.0.3.0+9-tem`, `21.0.12+1.1-tem`, `17.0.20-tem`, `11.0.32+1.1-tem` e `8.0.504+1-tem`. A regra para escolher é uma: **o que começa com `25` e termina em `-tem`**. Naquele dia era `25.0.3.0+9-tem`; na sua tela pode ser outro, porque o número completo muda a cada atualização do Temurin. Não copie o desta lição: copie o da sua lista.

Um detalhe medido que convém conhecer para não se confundir: no pé da lista, o SDKMAN sugere um identificador por padrão (naquele dia dizia `install the default: 25.0.4-tem`), e esse identificador **não aparecia na lista**. Por isso o curso não pede que você use a sugestão, mas que escolha da lista o que de fato está disponível. Com o identificador em mãos:

```text
$ sdk install java 25.0.3.0+9-tem
...
Downloading: java 25.0.3.0+9-tem
...
Repackaging Java 25.0.3.0+9-tem...
...
Installing: java 25.0.3.0+9-tem
Done installing!

Setting java 25.0.3.0+9-tem as default.
```

Baixa o JDK, o acomoda e o instala em `~/.sdkman/candidates/java/`, e a última linha diz algo importante: deixa-o como **padrão**, o que responderá ao comando `java` nos terminais que você abrir a partir de agora. Conforme o caso (por exemplo, se você já tinha outro Java instalado com o SDKMAN), em vez de fixá-lo ele pode apenas **perguntar** se você quer que seja o padrão: responda que sim, com `Enter` ou `Y`. Se você respondeu que não, ou não tem certeza, corrige a qualquer momento com `sdk default java` e o identificador que você instalou (aqui, `sdk default java 25.0.3.0+9-tem`). O download leva de um a vários minutos, conforme a sua conexão.

#### 1.1.6 Conferir a versão, em um terminal novo

Aqui está a armadilha que mais tempo faz perder, e foi medida no mesmo Mint: **no terminal onde você instalou, o `java --version` continuou dando o Java anterior** (naquela máquina havia um do sistema, em `/usr/bin/java`). Não é que a instalação tenha falhado: esse terminal decidiu onde procurar os programas quando foi aberto, e não fica sabendo da mudança. **Feche o terminal e abra um novo.** Nele:

```bash
java --version
javac --version
```

Os dois comandos, e não só o primeiro, porque `java` é o programa que executa e `javac` o que compila, e uma instalação pela metade pode dar um sem o outro (é justamente a diferença entre um JRE e um JDK da Lição 0). Isto é o que o `java --version` imprimiu em 13 de outubro de 2026 no Mint 22.3, no terminal novo:

```text
$ java --version
openjdk 25.0.3 2026-04-21 LTS
OpenJDK Runtime Environment Temurin-25.0.3+9 (build 25.0.3+9-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.3+9 (build 25.0.3+9-LTS, mixed mode, sharing)
```

Há duas coisas que você deve reconhecer na sua: **a primeira linha começa com `openjdk 25`** e **a segunda diz `Temurin`**. O resto muda com a atualização: `25.0.3` é a versão (25 é a maior; `0.3` é o número do patch), `2026-04-21` é a data dessa atualização, e `LTS` confirma que é uma versão de suporte longo. As saídas das demais lições foram medidas em outra máquina com um patch posterior (Temurin 25.0.4); o patch não muda nada do que o curso ensina. O `javac --version` deve dizer `javac 25` seguido de seu patch. E se você quiser saber de onde sai o `java` que respondeu, `which java` diz: no Mint medido, o caminho dentro da pasta do SDKMAN que termina em `.sdkman/candidates/java/current/bin/java`. (`which` imprime o caminho completo, com a sua pasta pessoal: `/home/<seu usuário>/.sdkman/candidates/java/current/bin/java`; aqui ele é encurtado com `~`.) Se `which java` lhe der `/usr/bin/java`, você está em um terminal velho.

#### 1.1.7 Se você tem mais de um Java

> **Vários JDKs ao mesmo tempo.** Se você já tinha um Java do sistema, ou se algum dia instalar outra versão com o SDKMAN, não é preciso desinstalar nada: o SDKMAN escolhe qual responde ao comando `java`. `sdk list java` marca em sua tabela os que você tem instalados. `sdk use java <identificador>` muda de versão **somente no terminal atual**, para testar algo; `sdk default java <identificador>` a deixa como padrão **em todos os terminais novos**. Se `java --version` não disser 25 e Temurin, o primeiro passo é abrir um terminal novo; o segundo, `sdk default java` com o identificador de 1.1.5. Depois, repita a verificação de 1.1.6, que se repete sempre que você muda algo.

### 1.2 O seu primeiro programa

#### 1.2.1 A pasta e o arquivo

Crie a pasta do curso e entre nela:

```bash
mkdir -p ~/curso-java/revisor
cd ~/curso-java
```

`mkdir -p` cria a pasta, e também as intermediárias se faltarem; sem `-p`, falharia se `~/curso-java` não existisse quando se pede `revisor` dentro dela. `~` é um atalho para a sua pasta pessoal. Em `~/curso-java/` viverá todo o curso, e dentro dela `revisor/` guardará o projeto que cresce lição após lição.

Crie o arquivo `Hola.java` com o editor de sua preferência (no Mint vem o Xed, que se abre com `xed Hola.java`) e escreva isto. Respeite a maiúscula inicial do nome e a extensão. A primeira linha, `// fig01_01.java`, é um comentário: o que vem depois de `//` até o fim da linha o compilador ignora. No curso, esse comentário é a marca que diz como o programa se chama no repositório; no seu `Hola.java` você pode copiá-lo ou não, tanto faz:

```java
// fig01_01.java
void main() {
  IO.println("Hola desde Java");
}
```

Salve e, no terminal, na pasta `~/curso-java`, execute com o nome do **seu** arquivo:

```text
$ java Hola.java
Hola desde Java
```

(No repositório do curso o mesmo programa se chama `fig01_01.java`, e `java fig01_01.java` imprime o mesmo.)

Esse é todo o programa. Vejamos o que diz cada parte.

#### 1.2.2 Linha por linha

**`void main() {`** declara o ponto de entrada: a primeira coisa que o Java executa quando o programa roda. São três peças. `main` é o nome que o Java procura para começar (não é uma palavra reservada, é uma convenção da linguagem que o lançador conhece). `()` são os parênteses onde iriam os dados que o método recebe; aqui ele não recebe nenhum. `void` quer dizer “não devolve nada”: `main` faz coisas, mas não entrega um resultado a quem o chamou. As chaves `{ }` delimitam o corpo: tudo o que vai entre a de abertura e a de fechamento é o que `main` faz. Este “método” sem classe ao redor é uma novidade do Java 25: chama-se **arquivo compacto** ([JEP 512](https://openjdk.org/jeps/512)).

**`IO.println("Hola desde Java");`** imprime uma linha de texto (“Olá do Java”). `IO` é uma classe da biblioteca padrão, `java.lang.IO`, que foi acrescentada no Java 25 para entrada e saída simples ([documentação](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/IO.html)); como está no pacote `java.lang`, que o Java importa sempre, **não é preciso importar nada**. `println` (“imprime uma linha”) escreve o que lhe é passado e uma quebra de linha. O que vai entre os parênteses, `"Hola desde Java"`, é um texto, que em Java se escreve entre aspas duplas. O ponto e vírgula `;` termina a instrução: em Java é obrigatório (diferente de Python ou Go), e esquecê-lo é o erro mais comum do primeiro dia.

**`}`** fecha o corpo.

Duas coisas que **não** aparecem: uma classe ao redor, nem as palavras `public static` nem o `String[] args`. No resto do curso isso é o normal. Quem criou então a classe? O compilador, em silêncio: pega o seu arquivo, dá à classe o nome do arquivo (`Hola`) e envolve o seu `main` nela. Você comprovará isso em 1.3 com o `javap`.

#### 1.2.3 Como se escrevia antes

É indispensável que você reconheça a forma antiga, porque é a que verá em quase todos os tutoriais, livros e respostas da internet que não sejam dos últimos dois anos. O mesmo programa, escrito como era exigido até o Java 24, é:

```java
public class Viejo {
  public static void main(String[] args) {
    System.out.println("Hola desde Java");
  }
}
```

Roda exatamente igual no Java 25 (foi conferido: imprime `Hola desde Java`). Cada palavra extra tinha uma função, que a Lição 6 explica a fundo, e que aqui só é esboçada para que você não se assuste ao vê-la. `class Viejo { ... }` define uma classe, a unidade em que o Java organiza o código; `public` a torna visível de fora do arquivo. `static` significa que o método pertence à classe e não a um objeto particular, de modo que pode ser chamado sem criar nada antes. `String[] args` é a lista de argumentos que o usuário escreve depois do nome do programa na linha de comando. E `System.out.println` é o caminho longo para imprimir: `System` é uma classe, `out` é um objeto dentro dela (a saída padrão) e `println` é o que se pede a ele.

Para um programa de uma linha, tudo isso é cerimônia. O Java 25 a remove **quando não é necessária**, e ela volta a ser necessária quando o programa cresce. O curso usa a forma curta até que ele cresça, e `IO.println` em vez de `System.out.println`, que neste curso é mais curto de escrever e mais fácil de ler. Se você ler código que usa `System.out.println`, faz o mesmo; não é preciso mudá-lo.

#### 1.2.4 O nome do arquivo e outros detalhes que mordem

Há quatro detalhes que convém saber desde já. **Um:** em um arquivo compacto, o compilador usa o nome do arquivo como nome da classe, então deve ser um nome válido para o Java e, por convenção, começar com maiúscula (`Hola.java`, `Revisor.java`). **Dois:** o programa deve ter um método `main`; com outro nome, o compilador o rejeita com uma mensagem que diz isso (você verá em 1.5). **Três:** o texto do arquivo deve estar em UTF-8, que é o que os editores do Mint produzem por padrão. **Quatro:** `java Hola.java` e `java hola.java` são arquivos distintos no Linux, onde as maiúsculas contam; no macOS e no Windows às vezes dá no mesmo, e é um clássico do “na minha máquina funcionava”.

E mais um detalhe sobre o que `java Hola.java` faz: o programa `java` vê que você deu um arquivo `.java`, o **compila na memória** e o executa, sem deixar nada no disco. Chama-se modo de lançamento de um arquivo-fonte ([JEP 330](https://openjdk.org/jeps/330)) e existe desde o Java 11. É o que o curso usa da Lição 1 à 5.

### 1.3 `javac` e `java` separadamente

#### 1.3.1 Compilar

O que `java Hola.java` faz em um só passo pode ser feito em dois, e vê-lo assim é a melhor maneira de entender o que é um `.class`. Na pasta onde está o `Hola.java`:

```bash
javac Hola.java
ls Hola.*
```

```text
$ ls Hola.*
Hola.class  Hola.java
```

`javac Hola.java` compilou o arquivo e, se não houve erros, não imprimiu nada (um compilador que não diz nada é um compilador contente), mas deixou um arquivo novo: `Hola.class`. Esse arquivo é o *bytecode* da Lição 0. Pesa 320 bytes e começa com os oito bytes que você já conhece (se o seu Mint não trouxer o `xxd`, pule este comando: não é usado de novo):

```text
$ xxd -l 8 Hola.class
00000000: cafe babe 0000 0045                      .......E
```

Para ver o que ele contém sem lê-lo byte a byte, o JDK traz o desassemblador `javap`:

```text
$ javap Hola
Compiled from "Hola.java"
final class Hola {
  Hola();
  void main();
}
```

Aí está a prova do que foi dito em 1.2.2: o compilador criou uma classe chamada `Hola` (como o arquivo), e colocou dentro o seu método `main`, além de um construtor `Hola()` que você não escreveu (um construtor é o que cria um objeto da classe; a Lição 3 explica). Com `javap -c -p Hola` você veria também as instruções de `main`, entre elas uma chamada ao método `println` da classe `IO`.

#### 1.3.2 Executar

Para executar o `.class`, já não se dá o arquivo ao lançador, mas **o nome da classe**:

```text
$ java Hola
Hola desde Java
```

Sem extensão: `Hola`, não `Hola.class` nem `Hola.java`. É a confusão mais frequente desta seção. Os três erros que isso produz foram medidos, e convém tê-los à vista; o primeiro é o de executar a classe **antes** de compilar, o segundo o de pôr a extensão, e o terceiro o de esquecer a extensão no compilador:

```text
$ java Hola
Error: Could not find or load main class Hola
Caused by: java.lang.ClassNotFoundException: Hola

$ java Hola.class
Error: Could not find or load main class Hola.class
Caused by: java.lang.ClassNotFoundException: Hola.class

$ javac Hola
error: Class names, 'Hola', are only accepted if annotation processing is explicitly requested
1 error
```

Os dois primeiros foram obtidos com o idioma do Java em inglês; em um sistema com a configuração regional em espanhol, a mesma mensagem sai traduzida (por exemplo, “Error: no se ha encontrado o cargado la clase principal Hola”, “não foi encontrada ou carregada a classe principal Hola”). Aprenda a reconhecê-los nos dois idiomas: “no se pudo encontrar o cargar la clase principal” (“não foi possível encontrar ou carregar a classe principal”) quer dizer **que o `java` procurou uma classe com esse nome e não a achou**; quase sempre se deve a que falta compilar, a que você escreveu o nome errado ou a que você está em outra pasta. A terceira mensagem é a mais enrolada das três: o `javac` interpretou `Hola` como o nome de uma classe e não como um arquivo, e sua queixa fala de “processamento de anotações”, um tema que não tem nada a ver com o que aconteceu com você. A correção é a de sempre: o `javac` leva o nome do **arquivo**, com seu `.java`.

Por que o `java` procura a classe onde a procura: por padrão, na pasta atual. Há um mecanismo para dizer a ele que procure em outras partes (o *classpath*, que a Lição 6 apresenta e a Lição 8 estuda), mas enquanto você estiver parado na pasta do `.class`, ele o encontra.

#### 1.3.3 O `.class` velho

O `.class` é uma cópia congelada: **não se atualiza sozinho quando você edita o `.java`**. Isso foi medido assim. Compilou-se um programa que imprime `version uno` (“versão um”), executou-se com `java V`, mudou-se o arquivo para que imprima `version dos` (“versão dois”) e executou-se de novo `java V`, sem compilar:

```text
$ java V
version uno
```

Continua dizendo `version uno`, ainda que o `.java` já diga outra coisa, porque `java V` executa o `.class` e o `.class` não ficou sabendo. É preciso compilar de novo com `javac V.java`. Em compensação, `java V.java` (com a extensão) recompila a cada execução e sempre reflete o arquivo atual, que é outra razão pela qual o curso o usa nas primeiras lições. No dia em que o projeto crescer, o Maven se encarregará de recompilar quando for preciso, que é uma das coisas para as quais ele existe (Lição 6).

### 1.4 `jshell`: testar uma ideia sem criar um arquivo

Entre as ferramentas do JDK, há uma que você usará todos os dias: o `jshell`, um interpretador interativo ([manual](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jshell.html)). Você escreve uma expressão, o Java a avalia e mostra o resultado, sem arquivo, sem `main`, sem compilar. É a forma mais rápida de responder “o que isto faz?”. No terminal, escreva `jshell`:

```text
$ jshell
|  Welcome to JShell -- Version 25.0.4
|  For an introduction type: /help intro

jshell> 1 + 1
$1 ==> 2

jshell> String s = "hola"
s ==> "hola"

jshell> s.length()
$3 ==> 4

jshell> "142" + 1
$4 ==> "1421"

jshell> /vars
|    int $1 = 2
|    String s = "hola"
|    int $3 = 4
|    String $4 = "1421"

jshell> /exit
|  Goodbye
```

Esta sessão foi montada com as respostas reais de uma execução do `jshell` com esses mesmos comandos. Cada vez que você escreve algo no indicador `jshell>`, ele avalia e responde com uma seta `==>` e o valor. As expressões que não têm nome recebem um automático: `$1`, `$3`, `$4`. (Onde está o `$2`? A instrução `String s = "hola"` deu nome ao seu valor, `s`, e por isso o contador saltou.) O comando `/vars` lista as variáveis que existem, com seu tipo, e `/exit` sai. Os comandos do `jshell` começam com uma barra, para distingui-los do código Java.

Repare na quarta entrada: `"142" + 1` deu `"1421"`, o mesmo resultado que você viu no `fig00_04.java`. Isto é o valioso do `jshell`: serve para **conferir** uma dúvida em dez segundos. Ele também mostra os erros sem ruído:

```text
jshell> int x = 5
x ==> 5

jshell> x = "a"
|  Error:
|  incompatible types: java.lang.String cannot be converted to int
|  x = "a"
|      ^-^

jshell> foo()
|  Error:
|  cannot find symbol
|    symbol:   method foo()
|  foo()
|  ^-^
```

Com três limitações que convém dizer de uma vez. O `jshell` não substitui um programa: o que você testa ali não fica guardado (existe `/save` para isso, mas um arquivo de verdade é melhor). Algumas coisas da linguagem que precisam de contexto, como um `main` ou uma classe com vários métodos, não são cômodas ali. E a numeração `$1`, `$2`... confunde no início. Use-o para dúvidas sobre uma expressão e um método da biblioteca, não para escrever o `revisor`.

### 1.5 Ler o primeiro erro do compilador

#### 1.5.1 Provocá-lo

Os erros do compilador são a primeira coisa que você lerá todos os dias, então convém ter confiança neles desde já. Provoca-se um de propósito. Este programa tem um erro de digitação: o método se chama `println`, e o programa escreve `prinln`. Salve-o em `~/curso-java` com o nome da figura, `fig01_02.java`, para que a mensagem que você vir diga o mesmo que a de baixo (a mensagem nomeia o arquivo), e copie também o comentário da primeira linha, porque os números de linha contam com ele:

```java
// fig01_02.java
void main() {
  IO.prinln("hola");
}
```

```text
$ java fig01_02.java
fig01_02.java:3: error: cannot find symbol
  IO.prinln("hola");
    ^
  symbol:   method prinln(String)
  location: class IO
1 error
error: compilation failed
```

#### 1.5.2 Anatomia

É uma mensagem de seis partes, e as seis se repetem, com pequenas variantes, em qualquer erro de compilação que você vir na vida.

1. **`fig01_02.java:3:`** é o arquivo e o número da **linha**. A linha 1 é o comentário `// fig01_02.java`, a 2 é `void main() {` e a 3 é a que falha. Quando o erro diz uma linha, esse é o lugar onde olhar primeiro; às vezes o problema real está um pouco antes (em uma linha anterior sem `;`), mas o compilador só percebe aqui.
2. **`error: cannot find symbol`** é o **diagnóstico**: “não se encontra o símbolo”. Um símbolo é um nome: de uma variável, de um método, de uma classe. O compilador viu um nome e não sabe a que ele se refere.
3. **A linha citada**, `IO.prinln("hola");`, copiada do arquivo para que você não precise ir procurá-la.
4. **A seta `^`**, que aponta para a coluna onde o compilador detectou o problema. Aqui está embaixo do ponto, na fronteira entre `IO` e o que vem depois: o compilador sabe que `IO` existe, e o que não encontra é o que vem depois do ponto.
5. **`symbol:   method prinln(String)`** diz **o que** não encontrou com o máximo de detalhe que consegue: um método chamado `prinln` que recebe um `String` (o texto). Observe que ele deduziu o tipo `String` a partir do argumento que você passou.
6. **`location: class IO`** diz **onde** procurou: na classe `IO`. Juntos, `symbol` e `location` se leem como uma frase: “procurei o método `prinln(String)` na classe `IO` e ele não existe”. E essa frase diz como corrigir: ou não existe porque você o escreveu errado (é o caso), ou porque não está nessa classe, ou porque falta um `import`.

No final, `1 error` conta quantos encontrou, e `error: compilation failed` é o lançador `java` avisando que, por isso, não executou nada. Se você compila com `javac`, a mensagem é a mesma mas sem essa última linha, e o `javac` termina com código de saída 1.

#### 1.5.3 Como se lê um compilador

Há três hábitos que poupam horas. **Leia o primeiro erro, não o último.** Muitas vezes um único problema produz uma cascata: uma chave sem fechar pode gerar dez mensagens. Corrija o primeiro, compile de novo e veja quantos restam. **Leia a mensagem inteira antes de buscar na internet.** `symbol` e `location` costumam ser suficientes. E **quando você não entender um erro, reduza o programa** até que fique a linha que o produz.

Mais cinco erros, medidos com o Temurin 25.0.4 com programas de três linhas e descritos aqui para que você os reconheça quando os vir. As mensagens são mostradas abreviadas à sua primeira linha:

| O que você escreveu | O que o compilador diz |
|---|---|
| falta o `;` no final: `IO.println("hola")` | `error: ';' expected` (com a seta logo depois do parêntese) |
| falta fechar as aspas: `IO.println("hola);` | `error: unclosed string literal` |
| `io.println("hola");` (minúscula) | `error: cannot find symbol` / `symbol:   variable io` / `location: class <nome do arquivo>` |
| falta a chave final `}` | `error: reached end of file while parsing` |
| o método se chama `principal` e não `main` | `error: compact source file does not have main method in the form of void main() or void main(String[] args)` |

O terceiro merece uma leitura: `io` em minúscula não é `IO`; o Java distingue as maiúsculas, então o compilador pensa que `io` é uma variável e não a encontra. O último diz com todas as letras o que o Java espera de um arquivo compacto: um método `main`, sem parâmetros ou com um só, um `String[] args`.

### 1.6 Quando a versão é a errada

Esta seção explica um dos tropeços mais comuns de quem começa com Java. O programa de 1.2, `Hola.java`, só funciona no Java 25. Com uma versão anterior, os erros são outros, e convém vê-los para identificá-los de relance. Com o **Java 21** (o que o `default-jdk` instala no Mint 22), o mesmo `Hola.java` dá o seguinte. (Estes dois blocos são fragmentos e não figuras do curso: foram executados com outro JDK, o 21 e o 17, e por isso não são comparados com o Java 25.)

```text
$ java --version
openjdk 21.0.12 2026-07-21 LTS
OpenJDK Runtime Environment Temurin-21.0.12+8 (build 21.0.12+8-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.12+8 (build 21.0.12+8-LTS, mixed mode, sharing)

$ java Hola.java
Hola.java:2: error: unnamed classes are a preview feature and are disabled by default.
void main() {
^
  (use --enable-preview to enable unnamed classes)
1 error
error: compilation failed
```

Diz, em essência, que “as classes sem nome são um recurso em versão prévia e estão desativadas”. No Java 21 o arquivo compacto existia, mas como versão prévia (a ideia da Lição 0): era preciso pedi-lo com `--enable-preview`, e com outro nome e outra forma. No Java 25 já é definitivo. Com o **Java 17**:

```text
$ java --version
openjdk 17.0.20 2026-07-21
OpenJDK Runtime Environment Temurin-17.0.20+8 (build 17.0.20+8)
OpenJDK 64-Bit Server VM Temurin-17.0.20+8 (build 17.0.20+8, mixed mode, sharing)

$ java Hola.java
Hola.java:2: error: class, interface, enum, or record expected
void main() {
^
Hola.java:4: error: class, interface, enum, or record expected
}
^
2 errors
error: compilation failed
```

“Esperava-se uma classe, uma interface, uma enumeração ou um `record`”: o Java 17 não conhece o arquivo compacto, e espera que tudo o que haja em um arquivo esteja dentro de uma classe.

**O diagnóstico é sempre o mesmo:** `java --version`. Se disser 21 ou 17, não é um erro do seu programa, é a versão. Para corrigir, instale o Temurin 25 com o SDKMAN (1.1.5), abra um terminal novo e, se você tiver vários, escolha o correto com `sdk default java` (1.1.7). A chave: **não mude o seu programa para que compile em uma versão velha**. O curso inteiro está escrito para o Java 25.

### 1.7 O estilo do código

Um último detalhe que parece menor e não é. Você terá notado que o código desta lição usa **dois espaços** de indentação, não quatro nem tabulações. Não é um capricho: é o guia de estilo do Google para Java ([Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)), um dos mais difundidos, e é o que a ferramenta Checkstyle verifica na Lição 10. Se o código das lições 1 a 9 usasse outro estilo, a 10 teria de confessar que tudo o que veio antes estava mal escrito. Muito código Java na internet usa quatro espaços; funciona igual, e é apenas uma convenção. O guia pede três coisas desde já: indentação de **2 espaços**, linhas de **100 colunas** no máximo, e chaves sempre, mesmo em um `if` de uma só linha (quando você chegar a ele).

### 1.8 O passo do projeto: o primeiro arquivo do `revisor`

O `revisor` começa hoje. Ainda não verifica nada; o que importa é que exista, no lugar correto, e que rode. Na pasta `~/curso-java/revisor/` que você criou em 1.2.1, crie o arquivo `Revisor.java` com este conteúdo (o comentário da primeira linha você pode omitir; está aí porque no repositório a figura se chama `fig01_03.java`):

```java
// fig01_03.java
void main() {
  IO.println("revisor: todavía no revisa nada");
}
```

A partir de `~/curso-java/revisor/`, execute com o nome do seu arquivo:

```text
$ java Revisor.java
revisor: todavía no revisa nada
```

(Repare no `í` de “todavía” (“ainda”): o Java escreve o acento sem problema, desde que o seu terminal use UTF-8, o que é normal no Mint. Se um dia você vir `?` no lugar, é a configuração regional do sistema e não o seu programa; a Lição 0 mediu isso.)

Agora faça também à moda antiga, para ver o `.class` do projeto:

```bash
javac Revisor.java
java Revisor
javap Revisor
```

Imprime o mesmo, e o `javap` mostra `final class Revisor`, com o método `main` dentro. Ao terminar, **apague o `.class`** com `rm Revisor.class`: é um arquivo seu, que você acabou de gerar, e não serve mais, porque a partir de agora a forma de rodar o projeto é `java Revisor.java`, e um `.class` velho na pasta é uma armadilha (1.3.3) para quando você editar o `.java` e rodar o `.class` por costume.

O projeto cresce a partir daqui. Na Lição 2, o `Revisor.java` aprende a classificar um serviço; na 3 ele se divide em vários arquivos; na 6 se converte em um projeto Maven. O `revisor` que termina o curso começa como este programa de três linhas.

## O erro que você vai ver

Esta lição tem dois erros principais. O primeiro é o de sempre: o compilador que não encontra um nome. O segundo é o da versão errada, e é o que mais desorienta quem começa.

**O primeiro: `cannot find symbol`.** É o do `fig01_02.java`, e se lê com as seis partes de 1.5.2:

```text
fig01_02.java:3: error: cannot find symbol
  IO.prinln("hola");
    ^
  symbol:   method prinln(String)
  location: class IO
```

Quase sempre é um erro de digitação, uma maiúscula errada ou um `import` esquecido. Corrige-se acertando o nome e executando de novo. O que **não** se faz é apagar linhas até que desapareça: a mensagem está dizendo exatamente que nome não encontra e onde o procurou.

**O segundo: o arquivo compacto com uma versão anterior.** Com o Java 21:

```text
error: unnamed classes are a preview feature and are disabled by default.
  (use --enable-preview to enable unnamed classes)
```

Com o Java 17, `error: class, interface, enum, or record expected`. São os dois sintomas de uma única doença: **uma versão do Java anterior à 25**. Não se corrige mexendo no programa, mas na instalação: `java --version` para confirmar, e 1.1.5 e 1.1.7 para corrigir. É o erro de quem instalou o `default-jdk` ou segue um tutorial velho.

**Um terceiro, mais modesto:** `Error: Could not find or load main class Hola`. Quer dizer que se executou `java Hola` sem ter compilado, ou a partir de uma pasta onde não está o `.class`. Compile com `javac Hola.java`, ou execute com `java Hola.java`.

## O que se faz errado

**Instalar o `default-jdk` por costume.** É a resposta mais popular e, no Mint 22, instala o Java 21, com o que o arquivo compacto falha. O caminho do curso é o Temurin 25 com o SDKMAN (1.1.4 e 1.1.5).

**Não conferir a versão depois de instalar.** “Instalou sem erros” não é “o comando `java` aponta para a 25”. A verificação de 1.1.6 são dois comandos e dez segundos, e se repete cada vez que você muda algo no sistema.

**Conferir no mesmo terminal onde você instalou.** É a armadilha medida de 1.1.6: esse terminal continua usando o Java que tinha ao abrir, e você conclui que a instalação falhou. Abra um terminal novo antes de conferir.

**Conferir só o `java` e não o `javac`.** Um JRE tem o primeiro e não o segundo. Para este curso você precisa dos dois.

**Executar scripts de origens que você não conhece.** `curl ... | bash` executa o que baixa, e por isso o que importa é de onde vem: o instalador do SDKMAN sai do site oficial do projeto, por `https`, e pode ser lido antes de rodá-lo (1.1.4). Um script colado de um fórum ou de um site qualquer não oferece nenhuma dessas duas coisas. O mesmo vale, com mais razão, para qualquer comando com `sudo`, que modifica o seu sistema com permissões de administrador: se você não sabe o que cada parte faz, não o rode.

**Executar `java Hola` sem ter compilado, ou editar e não recompilar.** É o `.class` velho de 1.3.3: o programa faz o que a versão anterior fazia e você acha que a sua mudança não funcionou. Enquanto aprende, use `java Hola.java`, que sempre compila o arquivo atual.

**Pôr a extensão onde não vai, e tirá-la onde vai.** `java` com um arquivo-fonte leva `.java` (`java Hola.java`); com uma classe compilada não leva extensão (`java Hola`); e `javac` sempre leva `.java`. São três regras, e se aprendem de cor com o uso.

**Corrigir às cegas vários erros de uma vez.** Um erro de compilação pode gerar outros que não existem de verdade. Corrija o primeiro, compile de novo, e veja o que sobra.

**Escrever com tabulações ou com quatro espaços “porque todo mundo faz assim”.** Compila igual, mas na Lição 10 a ferramenta de estilo vai reclamar de cada linha. Configure o seu editor com dois espaços (no Xed: Preferências, aba Editor, largura da tabulação 2 e inserir espaços em vez de tabulações).

**Seguir um tutorial sem olhar a data.** Se o tutorial põe `public class` e `public static void main` para um programa de um único arquivo, é de antes do Java 25; funciona, mas não é o estilo deste curso. E se fala do `default-jdk` ou de um JDK anterior, também.

## Exercícios

### Exercício 1 — O seu primeiro programa, do seu jeito

Modifique o `Hola.java` para que imprima três linhas: o seu nome, o nome do curso e a frase “Java 25”. Execute com `java Hola.java`. Depois compile com `javac Hola.java`, execute com `java Hola` e compare. Por último, mude uma palavra do texto, **sem** recompilar, e execute outra vez `java Hola`. Anote no seu diário de bordo o que aconteceu e por quê.

### Exercício 2 — Preveja e confira três erros

Escreva três versões quebradas do `Hola.java`, uma de cada vez: (a) sem o `;` no final do `IO.println`; (b) sem fechar as aspas do texto; (c) com `io.println` em minúscula. **Antes de executar cada uma, escreva no seu diário de bordo que mensagem você espera.** Execute-as e compare. Para cada uma, indique a linha que o compilador aponta e para onde a seta aponta.

### Exercício 3 — Pergunte ao `jshell`

Abra o `jshell` e avalie: `"142" + 1`, `2300 / 1000` e `2300 / 1000.0`. Para cada uma, escreva antes o que você acha que dá. Depois digite `/vars` e saia com `/exit`. Uma das três respostas provavelmente vai surpreender você: escreva no diário de bordo qual e por que você acha que dá o que dá. (A explicação completa está na Lição 2; aqui o que conta é ter visto.)

### Exercício 4 — Diagnostique pela mensagem

Para cada mensagem, diga o que aconteceu e como se corrige, sem executar nada:

1. `Error: Could not find or load main class Hola` / `Caused by: java.lang.ClassNotFoundException: Hola`, ao digitar `java Hola`.
2. `error: Class names, 'Hola', are only accepted if annotation processing is explicitly requested`, ao digitar `javac Hola`.
3. `error: unnamed classes are a preview feature and are disabled by default.`, ao digitar `java Hola.java`.

## Soluções

### Solução 1

Um possível `Hola.java`:

```java
void main() {
  IO.println("Dorian Chávez");
  IO.println("Curso de Java de Hábil");
  IO.println("Java 25");
}
```

Com `java Hola.java` imprime as três linhas. Depois de `javac Hola.java`, `java Hola` imprime o mesmo. Se agora você mudar o texto de um `IO.println` no `.java` e executar `java Hola` **sem** compilar de novo, imprime o texto **velho**: o `.class` é uma cópia congelada (1.3.3) e não sabe que você mudou o fonte. Para ver a mudança é preciso executar `javac Hola.java` outra vez, ou usar `java Hola.java`, que sempre recompila. O que você anota no diário de bordo: o `.class` não se atualiza sozinho.

### Solução 2

As três mensagens, medidas (mostra-se cada uma com sua linha apontada; o compilador sempre termina com `1 error` e `error: compilation failed`):

```text
a.java:2: error: ';' expected
  IO.println("hola")
                    ^

b.java:2: error: unclosed string literal
  IO.println("hola);
             ^

c.java:2: error: cannot find symbol
  io.println("hola");
  ^
  symbol:   variable io
  location: class c
```

Em (a), o compilador aponta a linha 2, não uma linha depois, e põe a seta **logo depois do último caractere** do `IO.println("hola")`, porque ali é onde se esperava o `;`. Em (b), a seta aponta para a aspa de abertura: é onde começa o texto que nunca foi fechado. Em (c), a seta aponta para `io`, e a mensagem diz `variable io`, não `class io`: como a minúscula não coincide com nenhum nome que o Java conheça, o compilador supõe que é o nome de uma variável. Se o erro (c) fez você pensar “deveria dizer que `io` não existe”, é exatamente o que ele diz, com o vocabulário do compilador. (Em `location: class c`, o nome `c` é o do arquivo `c.java` com que foi medido; no seu arquivo aparecerá o nome dele.)

### Solução 3

As respostas do `jshell`, medidas:

```text
jshell> "142" + 1
$1 ==> "1421"

jshell> 2300 / 1000
$2 ==> 2

jshell> 2300 / 1000.0
$3 ==> 2.3
```

A que surpreende é a segunda: **`2300 / 1000` dá `2`, não `2.3`**. Quando os dois números são inteiros, o Java faz uma divisão de inteiros e descarta a parte decimal. Para obter `2.3`, um dos dois tem de ser decimal, como o `1000.0` da terceira. É um assunto que vai importar mais adiante: o `revisor` converte 2300 milissegundos em “2.3s” e, se dividisse inteiros, mostraria `2s`. A Lição 2 explica a fundo. (Aqui você viu com os próprios olhos, que era o que importava. A numeração na sua sessão pode diferir conforme o que você tenha avaliado antes.)

### Solução 4

1. **Executou-se a classe sem tê-la compilado** (ou a partir de outra pasta). `java Hola` procura um arquivo `Hola.class` e ele não existe. Corrige-se compilando primeiro (`javac Hola.java`) ou executando diretamente o arquivo-fonte (`java Hola.java`).
2. **Deu-se ao `javac` o nome da classe e não o do arquivo.** O `javac` leva `.java`: `javac Hola.java`. O texto sobre “processamento de anotações” confunde, mas é a forma como o `javac` interpreta um argumento sem extensão.
3. **A versão do Java é anterior à 25**, provavelmente a 21: o arquivo compacto sem `class` só é definitivo no Java 25. Corrige-se instalando o Temurin 25 com o SDKMAN (1.1.5), deixando-o como padrão com `sdk default java` se houver outro (1.1.7), e confirmando com `java --version` em um terminal novo.

## Como sei que consegui

- [ ] Em um terminal novo, `java --version` imprime uma primeira linha que começa com `openjdk 25` e uma segunda que diz `Temurin`, e você sabe o que significava o código 127 se antes não tinha Java.
- [ ] `sdk version` responde, e você sabe que identificador do Temurin instalou e por que esse.
- [ ] `javac --version` imprime `javac 25` seguido de um número de patch.
- [ ] `java Hola.java` imprime o que você escreveu, e `javac Hola.java` deixa um `Hola.class` que se executa com `java Hola`.
- [ ] Você consegue explicar por que, se edita o `.java` e executa `java Hola`, não vê a sua mudança.
- [ ] No `jshell`, `1 + 1` dá `$1 ==> 2` e você sabe sair com `/exit`.
- [ ] Você provocou o erro do `fig01_02.java` e consegue apontar nele a linha, a seta, o `symbol` e o `location`.
- [ ] A partir de `~/curso-java/revisor/`, `java Revisor.java` imprime `revisor: todavía no revisa nada` (“revisor: ainda não verifica nada”).
- [ ] Você apagou o `Revisor.class` que gerou, e sabe por quê.
- [ ] Você anotou no seu diário de bordo o que o `java --version` disse na sua máquina e o que aconteceu com o Exercício 1.

## Resumo

Responda sem olhar a lição:

1. Por que o `default-jdk` no Mint 22 não serve para este curso? Que mensagem o arquivo compacto produz com essa versão?
2. O que faz `curl -s "https://get.sdkman.io" | bash`, e o que o torna aceitável aqui? Como você escolhe o identificador do Temurin no `sdk list java`? Por que é preciso abrir um terminal novo depois de instalar?
3. Que dois comandos se executam para conferir a instalação, e por que os dois?
4. O que é o arquivo compacto? Que duas coisas do programa tradicional já não são necessárias?
5. Que diferença há entre `java Hola.java` e `java Hola`? Quando o `.class` fica desatualizado?
6. O que o `Hola.class` contém e como você comprovou com o `javap`?
7. Para que serve o `jshell` e o que ele não é?
8. O que dizem as seis partes de uma mensagem como `cannot find symbol`?
9. Como você distingue, só pela mensagem, o erro de um programa do erro de uma versão errada?

## Para ler mais

- [SDKMAN, instalação](https://sdkman.io/install/) e [uso](https://sdkman.io/usage/): os requisitos, o instalador, e os comandos `list`, `install`, `use` e `default`. Consultado em 13 de outubro de 2026.
- [JEP 512, “Compact Source Files and Instance Main Methods”](https://openjdk.org/jeps/512): a proposta que define o arquivo compacto, `void main()` e a classe `IO`; explica o que o compilador cria por você e por quê. Consultado em 7 de outubro de 2026.
- [Oracle, manual do `jshell` (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jshell.html): todos os comandos do `jshell`, de `/vars` a `/save`. Consultado em 7 de outubro de 2026.
- [Oracle, manual do comando `java` (JDK 25)](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html): o lançador de um arquivo-fonte e as flags como `--enable-preview`. Consultado em 7 de outubro de 2026.

---

**Anterior:** [Lição 0 — O que é Java e o que NÃO é](00-que-es-java.md) ·
**Próxima:** [Lição 2 — Tipos, strings e controle de fluxo](02-tipos-cadenas-control.md)
