# Lição 5 — Erros e exceções

**Tempo:** 90 min (justos: a lição tem quatro ideias e um projeto, e convém não apressá-la)

**O que você constrói:** a leitura da configuração do `revisor`: a lista de serviços deixa de estar escrita no código e é lida de um arquivo `servicios.txt`, com uma mensagem clara para cada forma em que o arquivo pode estar errado

**O que você aprende:** exceções verificadas e não verificadas; `try`, `catch` e `finally`; `try-with-resources`; exceções próprias com sua causa; quando um resultado é melhor que uma exceção; e o que fazer com uma `InterruptedException`

**De onde você vem.** Você traz o projeto da [Lição 4](04-colecciones-genericos-streams.md): um `revisor` com seu `Simulador`, um `Resumen` que conta com *streams*, um `Reporte` com cópia defensiva, e uma tabela com sua linha de resumo. Funciona, mas tem duas falhas de fundo. A primeira é a que o exercício de projeto da lição 4 deixou: para acrescentar um serviço é preciso **abrir o código Java, editá-lo e recompilar**; os dados e o programa mudam por razões diferentes e não deveriam viver juntos. A segunda é mais sutil: a verificação é instantânea, porque o simulador devolve o tempo sem esperá-lo, e por isso você não tem uma forma de ver como o programa se comporta quando uma verificação **demora**. Esta lição corrige as duas: os dados vão para um arquivo que o programa lê e do qual desconfia, e a simulação aprende a esperar o que seu tempo diz. Você trabalha na sua pasta `~/curso-java/revisor/` e executa tudo com `java`.

**O que esta lição não faz.** Não consulta nenhuma rede: continua usando os dados simulados, e isso não muda até a [Lição 8](08-http-y-json.md). Não usa Maven nem testes: o Maven chega na [Lição 6](06-proyecto-maven.md) e os testes na [Lição 7](07-pruebas-junit.md). E o `revisor` ainda não tem opções de linha de comando nem código de saída: hoje o nome do arquivo está escrito no programa, e quando algo falha o programa imprime o motivo e termina com código 0. Isso é um defeito que a [Lição 10](10-el-programa-terminado.md) corrige, e esta lição o assinala quando você o vir.

**Sobre as execuções.** Para executar uma figura, entre na pasta da lição no repositório (`programas/05-errores-excepciones/`) e rode `java fig05_01.java`, com o número da figura que quiser. As que vivem em uma pasta própria (a da seção 5.3 e a do projeto da 5.6) são executadas de dentro dela. Todas as saídas foram obtidas com o Java 25 (Temurin 25.0.4) e o terminal em UTF-8. Os stack traces de erro trazem linhas do próprio JDK (`at java.base/...`) com números de linha que mudam de uma atualização para outra: é normal que os seus difiram nesses números.

## Ao terminar, você vai conseguir

- Dizer de um tipo de exceção se é verificada ou não verificada, e o que o compilador obriga em cada caso.
- Escrever um `try` com `catch` e `finally`, capturando só a exceção que você sabe tratar e na ordem que o compilador aceita.
- Abrir um arquivo de texto com `try-with-resources` e explicar o que essa forma garante diante de fechá-lo à mão.
- Definir uma exceção própria que conserve a causa original, e ler um stack trace com seu `Caused by`.
- Decidir se uma falha do seu programa é melhor representada por uma exceção ou por um valor, e defender a decisão.
- Tratar uma `InterruptedException` sem engoli-la: propagá-la, ou restaurar a flag de interrupção.

## O porquê antes do como

Até a lição 4, cada dado do `revisor` foi escrito por você, no mesmo arquivo que o código. Se estava errado, você sabia na hora, porque o compilador não deixava passar. Um arquivo de configuração muda essa situação por completo: é escrito por outra pessoa, em outro momento, com outro editor, e o programa fica sabendo de seus erros **na execução**. Um arquivo de fora pode falhar de quatro maneiras, e as quatro são normais, não excepcionais:

1. **Não existe**, ou existe em outra pasta diferente da que você pensa.
2. **Existe mas não pode ser lido**, porque está em outra codificação ou alguém o deixou sem permissões.
3. **Está completo na forma mas diz algo absurdo**: um endereço que não começa com `http`, um tempo limite negativo, dois serviços com o mesmo nome.
4. **Está vazio de conteúdo útil**: só comentários.

Um programa sério distingue esses casos e diz à pessoa **qual lhe aconteceu e como corrigir**, em uma linha, não com um relatório de trinta linhas escrito para programadores. Isso é o que esta lição faz.

Mas o `revisor` tem, além disso, outra classe de falha que não se parece com nenhuma das anteriores: um **serviço** que não responde. Verificar um serviço fora do ar não é um erro do programa; é justamente para o que ele existe. Se o programa tratasse um serviço fora do ar como uma emergência, um único serviço fora do ar derrubaria o relatório dos outros noventa e nove. Você já tomou essa decisão na lição 3, sem chamá-la assim: o `SinRespuesta` é um **valor**, não uma exceção. Esta lição a explica e a defende, porque entender quando algo é uma exceção e quando é um valor é a coisa mais importante que o Java tem a ensinar a você sobre erros.

Um comentário para quem chega de outras linguagens. Em Go, o erro é um valor que as funções devolvem e se verifica com `if err != nil`. Em Rust, é um `Result`. Em Python e TypeScript, é uma exceção que sobe até que alguém a capture. O Java faz as duas coisas: tem exceções, e tem valores como os `Optional` e os tipos *sealed* da lição 3. E acrescenta algo que os outros não têm: o compilador distingue **duas classes de exceções**, e obriga a tratar uma delas. Essa é a parte que parece estranha no começo e que tem seu sentido; a primeira seção vai por aí.

O caminho é de uma sessão. A seção 5.1 cobre as exceções verificadas e não verificadas e a sintaxe do `try`; a 5.2, `try-with-resources` e a leitura de arquivos; a 5.3, as exceções próprias; a 5.4, quando preferir um valor; a 5.5, `InterruptedException`; e a 5.6 junta tudo no projeto. Este é o destino, para que você o tenha à vista. Executado a partir de outra pasta que não a do arquivo de configuração, o `revisor` dirá:

```text
error de configuración: no existe el archivo servicios.txt
```

Uma mensagem de uma linha, em vez de um stack trace. Para chegar lá é preciso entender, primeiro, o que é uma exceção.

## Os conceitos

### 5.1 Exceções verificadas e não verificadas

#### 5.1.1 O que é uma exceção e que famílias há

Uma **exceção** é um objeto que descreve que algo deu errado. Quando uma linha do programa não consegue fazer o que lhe foi pedido, **lança** uma exceção (com a palavra `throw`, ou porque o faz por dentro um método que você chamou). A exceção sobe pelos métodos que estavam em andamento, do mais recente ao mais antigo, procurando alguém que a **capture**. Se ninguém a captura, a thread termina e a JVM imprime o stack trace que você já conhece. A lição 4 ensinou você a lê-lo de cima para baixo; agora vamos ver o que há por trás.

Todas as exceções herdam de uma classe: `Throwable`. Daí saem dois ramos. `Error` é para os problemas graves da própria JVM (acabou a memória, um arquivo de classes corrompido), e a regra é que **não se capturam**: você não tem como se recuperar. `Exception` é para o que um programa razoável pode querer tratar. E dentro de `Exception` há um subramo especial, `RuntimeException` (exceção em tempo de execução), com o qual o Java divide o mundo em dois:

- **Exceções não verificadas** (*unchecked*): `RuntimeException`, `Error` e tudo o que herda delas. O compilador **não obriga** você a fazer nada com elas. São os erros de programação: uma `NullPointerException`, uma `IllegalArgumentException`, a `UnsupportedOperationException` da lição 4.
- **Exceções verificadas** (*checked*): todas as demais, ou seja, `Exception` e suas filhas que não são `RuntimeException`. O compilador **obriga você a tratá-las**, e se você não o faz, o programa não compila. É a definição da [especificação da linguagem](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html): as verificadas são todas as que não são não verificadas. (No resto do curso uso “verificada” e “não verificada”; “checked” e “unchecked” são os nomes em inglês e você os verá em toda a documentação.)

O programa `fig05_01` imprime a cadeia de herança de cinco exceções e diz a que classe cada uma pertence. Para escrevê-lo é preciso um `for` com uma condição em vez de um percurso (`for (inicio; condición; paso)`: começa em `inicio`, repete enquanto a `condición` for verdadeira, e executa o `paso` no fim de cada volta), uma `Class<?>` (o tipo que representa uma classe; o `?` é o curinga da lição 4) e os métodos `getSuperclass()`, que dá o pai, e `isAssignableFrom`, que responde “este tipo herda daquele?”:

```java
// fig05_01.java
void cadena(Class<?> tipo) {
  String texto = tipo.getSimpleName();
  for (Class<?> padre = tipo.getSuperclass(); padre != null; padre = padre.getSuperclass()) {
    texto = texto + " -> " + padre.getSimpleName();
  }
  boolean esExcepcion = Exception.class.isAssignableFrom(tipo);
  boolean esNoVerificada = RuntimeException.class.isAssignableFrom(tipo);
  String clase;
  if (Error.class.isAssignableFrom(tipo)) {
    clase = "error de la JVM";
  } else if (esExcepcion && !esNoVerificada) {
    clase = "verificada";
  } else {
    clase = "no verificada";
  }
  IO.println(texto);
  IO.println("    " + clase);
}

void main() {
  cadena(NoSuchFileException.class);
  cadena(InterruptedException.class);
  cadena(NumberFormatException.class);
  cadena(NullPointerException.class);
  cadena(OutOfMemoryError.class);
}
```

```text
$ java fig05_01.java
NoSuchFileException -> FileSystemException -> IOException -> Exception -> Throwable -> Object
    verificada
InterruptedException -> Exception -> Throwable -> Object
    verificada
NumberFormatException -> IllegalArgumentException -> RuntimeException -> Exception -> Throwable -> Object
    no verificada
NullPointerException -> RuntimeException -> Exception -> Throwable -> Object
    no verificada
OutOfMemoryError -> VirtualMachineError -> Error -> Throwable -> Object
    error de la JVM
```

A cadeia se lê da esquerda para a direita como “é um... que é um...”. `NoSuchFileException`, a que o Java lança quando um arquivo não existe, é uma `FileSystemException`, que é uma `IOException`, que é uma `Exception`: verificada. `InterruptedException` é uma filha direta de `Exception`: verificada. `NumberFormatException` passa por `IllegalArgumentException` e por `RuntimeException`: não verificada. `NullPointerException` também. E `OutOfMemoryError` está no ramo de `Error`, que é de outra família: você nunca a capturará. Observe também que **a verificada da primeira linha é uma `IOException`**: toda a família de problemas de entrada e saída (arquivos, rede) é verificada. É a classe de erro que o programa não pode evitar escrevendo melhor código, porque depende de um mundo exterior que pode falhar.

#### 5.1.2 O que o compilador obriga: capture-a ou declare-a

Vejamos o que significa “o compilador obriga”. Este programa lê um arquivo de texto e conta suas linhas. Parece inofensivo:

```java
// fig05_02.java
void main() {
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
  IO.println(renglones.size());
}
```

```text
$ java fig05_02.java
fig05_02.java:3: error: unreported exception IOException; must be caught or declared to be thrown
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
                                             ^
1 error
error: compilation failed
```

`unreported exception IOException; must be caught or declared to be thrown`: “exceção `IOException` não reportada; deve ser capturada ou declarada”. A seta `^` aponta para a chamada a `readAllLines`, a que pode lançá-la. Para que o programa compile você tem **duas opções**, e uma das duas é obrigatória:

1. **Capturá-la** onde você possa tratá-la, com `try` e `catch` (a próxima seção).
2. **Declará-la**: avisar na assinatura do método que ele pode lançá-la, com `throws`. Por exemplo `void main() throws IOException`. Declará-la não trata o erro; **repassa-o** a quem chamar o método, que por sua vez terá de capturá-la ou declará-la. A exceção sobe até que alguém a capture; se chega ao `main` e o `main` também a declara, a JVM imprime o stack trace e o programa morre.

A ideia por trás da regra é que certas falhas não são erros do programador mas da situação (o arquivo pode não existir), e que seria injusto que o código as ignorasse por descuido. O compilador converte o “esqueci que isto pode falhar” em um erro de compilação. Se lhe parece um peso, reserve o juízo até a seção 5.4: é uma ferramenta com um uso preciso.

#### 5.1.3 `try`, `catch` e `finally`

Capturar uma exceção é dizer: “tente isto; se falhar *desta* maneira, faça *esta outra coisa* em vez de morrer”. Escreve-se com `try` (tente), `catch` (capture) e, opcionalmente, `finally` (finalmente). O programa `fig05_03` converte um texto em um número, que é o que a configuração precisa para ler um `timeout_ms`:

```java
// fig05_03.java
int leerTimeout(String texto) {
  try {
    int valor = Integer.parseInt(texto);
    IO.println("  convertí " + valor);
    return valor;
  } catch (NumberFormatException e) {
    IO.println("  no sirve: " + e.getMessage());
    return -1;
  } finally {
    IO.println("  (fin de la conversión)");
  }
}

void main() {
  IO.println("leyendo 5000");
  IO.println("  resultado: " + leerTimeout("5000"));
  IO.println("leyendo 5 s");
  IO.println("  resultado: " + leerTimeout("5 s"));
}
```

```text
$ java fig05_03.java
leyendo 5000
  convertí 5000
  (fin de la conversión)
  resultado: 5000
leyendo 5 s
  no sirve: For input string: "5 s"
  (fin de la conversión)
  resultado: -1
```

`Integer.parseInt(texto)` converte um texto em um `int`. Com “5000” funciona; com “5 s” lança `NumberFormatException` com a mensagem `For input string: "5 s"` (o texto que não pôde converter). Os três blocos:

- **`try { ... }`** contém a linha que pode falhar, e de preferência **somente essa** e as que dependem dela. Se não há erro, o programa segue até o fim do bloco. Se há, o Java abandona o `try` nessa mesma linha e salta para o `catch` que corresponda.
- **`catch (NumberFormatException e) { ... }`** é executado se o erro foi desse tipo. O `e` é a exceção capturada: `e.getMessage()` dá sua mensagem. Qualquer outro tipo de exceção passaria direto.
- **`finally { ... }`** é executado ao sair do `try`, seja como for que se saia: tenha havido erro ou não, e ainda que o `try` ou o `catch` tenham feito `return`. A única exceção é que a JVM termine de repente enquanto está dentro do `try` ou do `catch`: uma chamada a `System.exit` (que a lição 10 explica), uma queda do processo ou um desligamento forçado. Veja a saída: com “5000”, imprime-se “convertí 5000” (“converti 5000”), depois o `finally`, e **depois** o resultado chega a quem chamou, ainda que o `return valor` apareça antes no código. Usa-se para limpar. A [documentação da Oracle](https://docs.oracle.com/javase/tutorial/essential/exceptions/finally.html) resume: “o bloco `finally` sempre é executado quando o `try` termina”, e a mesma página registra essa exceção.

Uma nota de forma: devolver `-1` para dizer “não válido” é um mau hábito, porque é um valor mágico (nada impede que alguém o use como se fosse um tempo de verdade). Aqui está apenas porque o exemplo é mínimo; na seção 5.3 o `catch` fará algo melhor.

Há três regras para capturar bem, e as três são quebradas com tanta frequência que convém escrevê-las:

1. **Capture a exceção concreta que você sabe tratar.** Um `catch (Exception e)` também captura a `NullPointerException` produto de um erro de digitação, e o programa seguirá funcionando com dados errados. Melhor que pare com seu stack trace.
2. **Se você não sabe o que fazer com um erro, não o capture.** Deixe-o subir. Um programa que para com um stack trace claro é melhor que um que continua às cegas.
3. **Um único `catch` pode cobrir dois tipos se a resposta é a mesma**, com uma barra vertical: `catch (NumberFormatException | NullPointerException e)`. Os tipos não podem ser um filho do outro.

A **ordem** dos `catch` importa, e o compilador a vigia. Se o mais geral vai primeiro, os demais nunca poderiam ser executados:

```java
// fig05_04.java
void main() {
  try {
    IO.println(Integer.parseInt("5 s"));
  } catch (Exception e) {
    IO.println("algo falló");
  } catch (NumberFormatException e) {
    IO.println("no es un número");
  }
}
```

```text
$ java fig05_04.java
fig05_04.java:7: error: exception NumberFormatException has already been caught
  } catch (NumberFormatException e) {
    ^
1 error
error: compilation failed
```

`exception NumberFormatException has already been caught`: “já foi capturada”. O primeiro `catch (Exception e)` engole tudo, incluída a `NumberFormatException`, e o segundo bloco seria código morto. A regra: **do particular ao geral**, igual a quando o `switch` da lição 3 pedia ordenar os casos.

#### 5.1.4 Por que uma verificada não atravessa a lambda de um `map`

Há um lugar onde a regra das verificadas parece um estorvo, e convém conhecê-lo agora porque você topará com ele no `revisor`. É dentro de uma lambda. Este programa tenta esperar um pouco dentro de um `map`:

```java
// fig05_05.java
void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<String> revisados =
      nombres.stream()
          .map(
              nombre -> {
                Thread.sleep(10);
                return nombre + " revisado";
              })
          .toList();
  IO.println(revisados);
}
```

```text
$ java fig05_05.java
fig05_05.java:8: error: unreported exception InterruptedException; must be caught or declared to be thrown
                Thread.sleep(10);
                            ^
1 error
error: compilation failed
```

O mesmo erro de antes, mas agora dentro da lambda: `Thread.sleep` lança `InterruptedException` (verificada) e a lambda do `map` não a declara nem a captura. A razão é que `map` espera uma `Function`, a interface funcional da lição 4, e o método `apply` de uma `Function` **não declara nenhuma exceção verificada** em sua assinatura. Uma lambda não pode lançar mais do que sua interface declara. (Com outras interfaces pode: o método `call` de `Callable` declara `throws Exception`, e na lição 9 você vai aproveitar isso.) As saídas são três, e nenhuma é agradável: capturar a exceção dentro da lambda (o corpo se enche de `try`), embrulhá-la em uma não verificada (perde-se a vantagem da verificada), ou **não usar um *stream* ali e voltar a um laço `for`**, onde o método que o contém pode, sim, declarar o `throws`. No projeto de hoje tomaremos esta última: `revisarTodos` passa de *stream* a `for`. Não é um retrocesso, é escolher a ferramenta que permite o que o problema pede.

Se você prefere uma referência a método (`.map(Revision::revisar)`) a mensagem muda mas a causa é a mesma: `incompatible thrown types InterruptedException in functional expression`. O exercício 4 pede que você a provoque.

#### 5.1.5 Quando verificada e quando não?

Quando você projeta suas próprias exceções tem de decidir de que família são, e o guia da Oracle dá uma regra de uma frase: [“se for razoável esperar que quem chama se recupere da exceção, torne-a verificada; se não pode fazer nada para se recuperar, torne-a não verificada”](https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html). Um arquivo de configuração que não existe é um bom exemplo da primeira: quem chama pode fazer algo útil (avisar o usuário como corrigir). Um `null` onde nunca deveria haver é o exemplo da segunda: não há nada a "recuperar", há um defeito a corrigir.

É honesto dizer que essa decisão do Java é discutida. As verificadas obrigam a uma disciplina útil, mas em projetos grandes produzem uma cascata de `throws` em todas as assinaturas e de `catch` que só repassam, e outras linguagens da mesma família, como C# e Kotlin, decidiram não tê-las. O Java as conserva, e seu trabalho não é ter uma opinião sobre elas mas saber que o compilador as trata de forma diferente e por quê.

### 5.2 `try-with-resources`: o que se abre se fecha

#### 5.2.1 Recursos e a forma que os fecha sozinha

Um **recurso** é algo que se abre e que é preciso fechar: um arquivo, uma conexão de rede. Se você não o fecha, o sistema operacional o mantém aberto: com um arquivo, isso se traduz em um descritor que se esgota, e com uma conexão, em um servidor que continua esperando. A forma de sempre de fechá-lo bem era um `try` com um `finally` que fecha o recurso, e tem um defeito de família: se o `try` lança uma exceção e o `close()` do `finally` também, a segunda **tapa** a primeira, e você perde o erro que de verdade importava.

O Java 7 acrescentou uma forma que resolve isso: **`try-with-resources`** (*try com recursos*). Você põe os recursos entre parênteses logo depois do `try`, e o Java os fecha **sozinho**, em ordem inversa à que os abriu, ainda que o bloco termine bem ou com uma exceção. Um recurso é qualquer objeto cujo tipo implementa a interface `AutoCloseable`, que tem um único método, `close()`. O programa `fig05_06` demonstra isso com um recurso inventado que avisa quando é aberto e quando é fechado:

```java
// fig05_06.java
record Recurso(String nombre, boolean falla) implements AutoCloseable {
  Recurso {
    IO.println("abro " + nombre);
  }

  @Override
  public void close() {
    IO.println("cierro " + nombre);
    if (falla) {
      throw new IllegalStateException("no pude cerrar " + nombre);
    }
  }
}

void main() {
  try (Recurso entrada = new Recurso("entrada", false);
      Recurso salida = new Recurso("salida", false)) {
    IO.println("trabajo con " + entrada.nombre() + " y " + salida.nombre());
  }
  IO.println("--");
  try (Recurso fragil = new Recurso("frágil", true)) {
    IO.println("trabajo con " + fragil.nombre());
    throw new IllegalArgumentException("falló el trabajo");
  } catch (IllegalArgumentException e) {
    IO.println("atrapé: " + e.getMessage());
    for (Throwable suprimida : e.getSuppressed()) {
      IO.println("  suprimida: " + suprimida.getMessage());
    }
  }
}
```

```text
$ java fig05_06.java
abro entrada
abro salida
trabajo con entrada y salida
cierro salida
cierro entrada
--
abro frágil
trabajo con frágil
cierro frágil
atrapé: falló el trabajo
  suprimida: no pude cerrar frágil
```

Antes de ler a saída, três detalhes da declaração que você ainda não tinha visto. `implements AutoCloseable` é o mesmo `implements` da lição 3: o *record* se compromete a cumprir o contrato da interface, que exige um método `close()`. `@Override` é uma **anotação**: diz ao compilador “este método substitui o da interface”, e se você erra o nome, falha ao compilar em vez de criar um método diferente sem que você perceba. E `public` é uma palavra de que você ainda não tinha precisado: os métodos de uma interface são sempre públicos, e por isso quem os implementa deve escrevê-lo. Na lição 6 você verá a palavra completa, com seu significado e suas irmãs.

Agora a saída. No primeiro bloco você abre dois recursos (`entrada` e `salida`) e eles são fechados **ao contrário**: primeiro `salida`, depois `entrada`. É a ordem correta: o último que você abriu costuma depender do anterior. No segundo bloco o `close()` de `frágil` lança uma exceção, e o corpo do `try` também (ele a lança com `throw new IllegalArgumentException(...)`, que é a forma de lançar uma à mão). O resultado: o `catch` recebe a exceção **do corpo**, a que importava (`falló el trabajo` (“o trabalho falhou”)), e a que o `close()` lançou não se perde nem a tapa: fica **anexada** à primeira como exceção *suprimida*, e se recupera com `e.getSuppressed()`. O [guia da Oracle](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) explica com outro exemplo: com um `finally`, a exceção que sai é a do `finally` e a do `try` é suprimida; com `try-with-resources`, é ao contrário, e é a ordem que você quer.

#### 5.2.2 Ler um arquivo de texto

Para ler o `servicios.txt` do `revisor` são necessárias três peças que você não tinha usado. **`Path`** representa um caminho de arquivo como um valor, e se cria com `Path.of("servicios.txt")`; `ruta.getFileName()` dá a última parte. **`Files`** é uma classe com métodos estáticos para operar sobre um `Path`. E **`BufferedReader`** é um objeto que lê texto linha por linha. `Files.newBufferedReader(ruta)` abre o arquivo e devolve um `BufferedReader`, e como é um recurso, vai em um `try-with-resources`:

```java
try (BufferedReader lector = Files.newBufferedReader(ruta)) {
  String renglon;
  while ((renglon = lector.readLine()) != null) {
    // ...usar el renglón...
  }
}
```

`lector.readLine()` devolve a próxima linha como `String`, ou `null` quando o arquivo acabou. Aqui o `null` é a forma que o Java escolheu desde suas primeiras versões para dizer “não há mais”; `Optional` não existia. O `while` (*enquanto*) é um laço que repete seu corpo enquanto a condição for verdadeira, e a condição tem um truque muito comum: `(renglon = lector.readLine()) != null` **atribui e compara ao mesmo tempo**, porque uma atribuição vale o que foi atribuído. Lê-se “leia uma linha e guarde-a em `renglon`; se o que foi lido não é `null`, entre no corpo”.

Três coisas de `Files` que convém saber, todas da [documentação da classe](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html):

- **Lê em UTF-8.** `Files.newBufferedReader(ruta)` e `Files.readAllLines(ruta)` decodificam o arquivo com UTF-8, que é o que você quer para um arquivo com acentos. (Outras partes do Java, como `FileReader`, usavam a codificação da máquina, e por isso um programa lia bem no seu computador e mal em outro; desde o Java 18, [JEP 400](https://openjdk.org/jeps/400), a codificação padrão de toda a plataforma também é UTF-8.)
- **O caminho relativo é resolvido a partir da pasta onde você roda `java`**, não de onde vive o arquivo `.java`. `Path.of("servicios.txt")` procura `servicios.txt` na pasta atual do terminal. É a causa de quase todos os “não encontra o arquivo” que você vai ver, e o projeto da seção 5.6 mostrará isso.
- **Um arquivo que não existe lança `NoSuchFileException`**, que como você viu na cadeia de herança é uma `IOException`: verificada. Um arquivo que existe mas tem bytes que não são UTF-8 válido lança `MalformedInputException`, outra `IOException`, ao ler.

Uma variante a ter presente: `Files.readAllLines(ruta)` lê todo o arquivo para uma lista de linhas e o fecha por você (você não precisa de `try-with-resources`), e serve para arquivos pequenos. `Files.lines(ruta)` devolve um *stream* de linhas, mas **esse sim precisa ser fechado**: o fluxo mantém o arquivo aberto, e sua documentação diz isso com essas palavras. Para o `revisor` o `BufferedReader` tem uma vantagem decisiva: você sabe em que linha está sem escrever um índice à parte, e o número da linha é o que uma mensagem de erro útil precisa.

### 5.3 Exceções próprias, com sua causa

#### 5.3.1 Por que uma própria

A configuração pode falhar por uma dúzia de razões, e todas aterrissam no mesmo lugar: o `main`, que tem de dizer algo a uma pessoa. Se cada razão fosse uma exceção diferente do JDK, o `main` teria de capturar `NoSuchFileException`, `MalformedInputException`, `URISyntaxException`, `NumberFormatException` e meia dúzia mais, e conhecer os detalhes de cada uma. Pior: se todas fossem `IllegalArgumentException`, o `main` não poderia distinguir “a configuração está errada” de “meu programa tem um defeito”. A solução é uma exceção com nome próprio, que significa algo do domínio: **`ErrorDeConfiguracion`**. Terá um único significado (“o arquivo de configuração não pode ser usado”), o `main` capturará só essa, e qualquer outra que escape será, com razão, uma falha do programa que deve pará-lo com seu stack trace.

Uma exceção própria se define com uma linha de **herança**. Veja a figura `fig05_07`, uma pasta com dois arquivos: a definição da exceção e um programa que a usa. A palavra nova é **`extends`**: uma classe que `extends` outra *herda* tudo o que a outra tem. É como `implements` mas para uma classe em vez de uma interface: `ErrorDeConfiguracion` *é uma* `Exception` e traz todos os seus métodos (`getMessage`, `getCause`, `printStackTrace`) sem que você os escreva. A única coisa necessária é dizer como se constrói, e para isso se chama o construtor da classe mãe com **`super`**: `super(mensaje, causa)` significa “construa-me como uma `Exception` com esta mensagem e esta causa”.

```java
// fig05_07/ErrorDeConfiguracion.java
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
// fig05_07/Demo.java
void leer(Path ruta) throws ErrorDeConfiguracion {
  try {
    Files.readAllLines(ruta);
  } catch (NoSuchFileException e) {
    throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e);
  } catch (IOException e) {
    throw new ErrorDeConfiguracion("no se pudo leer el archivo " + ruta.getFileName(), e);
  }
}

void main() {
  try {
    leer(Path.of("servicios-que-no-existe.txt"));
  } catch (ErrorDeConfiguracion e) {
    IO.println("mensaje: " + e.getMessage());
    IO.println("causa:   " + e.getCause());
    e.printStackTrace();
  }
}
```

O programa `Demo` define um método `leer` que tenta ler um arquivo que não existe e, se falha, lança **`ErrorDeConfiguracion` com uma causa**: `throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e)`. O segundo argumento, `e`, é a exceção original. Os dois `catch` estão na ordem correta: primeiro a `NoSuchFileException` (mais particular), depois a `IOException` geral. Rode a figura a partir da sua pasta:

```text
$ cd fig05_07
$ java Demo.java
mensaje: no existe el archivo servicios-que-no-existe.txt
causa:   java.nio.file.NoSuchFileException: servicios-que-no-existe.txt
ErrorDeConfiguracion: no existe el archivo servicios-que-no-existe.txt
	at Demo.leer(Demo.java:6)
	at Demo.main(Demo.java:14)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:565)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.execute(SourceLauncher.java:256)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.run(SourceLauncher.java:138)
	at jdk.compiler/com.sun.tools.javac.launcher.SourceLauncher.main(SourceLauncher.java:76)
Caused by: java.nio.file.NoSuchFileException: servicios-que-no-existe.txt
	at java.base/sun.nio.fs.UnixException.translateToIOException(UnixException.java:92)
	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:106)
	at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:111)
	at java.base/sun.nio.fs.UnixFileSystemProvider.newFileChannel(UnixFileSystemProvider.java:213)
	at java.base/sun.nio.fs.UnixFileSystemProvider.newByteChannel(UnixFileSystemProvider.java:244)
	at java.base/java.nio.file.Files.newByteChannel(Files.java:357)
	at java.base/java.nio.file.Files.newByteChannel(Files.java:399)
	at java.base/java.nio.file.spi.FileSystemProvider.newInputStream(FileSystemProvider.java:371)
	at java.base/java.nio.file.Files.newInputStream(Files.java:154)
	at java.base/java.nio.file.Files.newBufferedReader(Files.java:2645)
	at java.base/java.nio.file.Files.readAllLines(Files.java:3085)
	at java.base/java.nio.file.Files.readAllLines(Files.java:3122)
	at Demo.leer(Demo.java:4)
	... 6 more
```

Leia a saída com calma, porque é o primeiro stack trace com **duas** partes. As duas primeiras linhas são as que o programa imprimiu: `mensaje:` é o que você diria à pessoa (“no existe el archivo servicios-que-no-existe.txt” (“não existe o arquivo servicios-que-no-existe.txt”)), e `causa:` é o que **de verdade** aconteceu, com seu tipo completo: `java.nio.file.NoSuchFileException: servicios-que-no-existe.txt`. O resto é o que `e.printStackTrace()` imprime, que escreve o stack trace completo na saída de erro. Tem dois blocos. O primeiro é a exceção que você capturou, com seu stack trace: `Demo.leer(Demo.java:6)` é a linha do `throw`, e `Demo.main(Demo.java:14)` é quem chamou. As linhas que seguem, de `jdk.internal.reflect` e `SourceLauncher`, são a maquinaria com que `java Archivo.java` inicia o seu programa. O segundo bloco começa com **`Caused by:`** (“causada por”): é a causa, com seu próprio stack trace, que desce até o sistema operacional (`sun.nio.fs.UnixException...`) e sobe por `Files.readAllLines` até a linha `Demo.leer(Demo.java:4)`, que é a linha onde de verdade se tentou abrir o arquivo. A última linha, `... 6 more` (“e mais 6”), quer dizer que as seis linhas restantes são as mesmas que você já viu acima.

Isso é o que dá valor a conservar a causa. A **mensagem** é para a pessoa que usa o programa: curta, em espanhol, sem jargão. A **causa** é para quem tem de corrigir o problema: a exceção original com seu stack trace completo. Perder a causa é perder o rastro. Duas regras da casa que daqui se derivam:

1. **Quando você converter uma exceção em outra, passe a original como causa**, sempre.
2. **A mensagem diz o que aconteceu e o que fazer; a causa diz por quê.** Na mensagem do `revisor` nunca aparece o texto da exceção original, porque muda entre sistemas e versões; está na causa.

#### 5.3.2 A pior forma de tratar um erro: ignorá-lo

Se há algo que nunca se deve fazer com uma exceção, é isto:

```java
// fig05_08.java
int leerTimeout(String texto) {
  try {
    return Integer.parseInt(texto);
  } catch (Exception e) {
  }
  return 5000;
}

void main() {
  IO.println("timeout_ms = " + leerTimeout("2000"));
  IO.println("timeout_ms = " + leerTimeout("dos mil"));
}
```

```text
$ java fig05_08.java
timeout_ms = 2000
timeout_ms = 5000
```

A segunda linha diz `timeout_ms = 5000` quando o valor escrito era “dos mil” (“dois mil”), sem nenhum sinal de que algo deu errado. O `catch (Exception e)` **vazio** capturou a `NumberFormatException`, não fez nada com ela e a função devolveu o valor padrão como se nada tivesse acontecido. É o equivalente em Java do `except: pass` do Python. A pessoa que escreveu “dos mil” acredita que o programa lhe obedeceu, e o programa funciona com outro valor. Quando algo falhar de verdade, o motivo já desapareceu e não há um dado a investigar. Um `catch` vazio é quase sempre um erro, e há três razões pelas quais convém lembrar disso: apaga a causa, converte um erro que o compilador lhe assinalou em um que ninguém vê, e ocorre justamente quando se tem pressa de “fazer compilar”.

Se você de verdade não sabe o que fazer com uma exceção, não a capture; se a captura, **faça algo com ela**: corrija-a, traduza-a para outra com causa, ou registre-a no log e deixe claro por que a ignora. Uma nota sobre `printStackTrace()`: está bem para aprender e para depurar, mas não é uma maneira de tratar um erro; apenas o mostra.

#### 5.3.3 Dois canais de saída

Uma pergunta prática que surge aqui: para onde o `revisor` escreve quando algo falha? O programa tem **dois canais** de saída de texto, que o sistema operacional trata separadamente. `System.out` é a saída normal, e é onde `IO.println` escreve. `System.err` é a **saída de erro**, para os avisos e as falhas. Os dois aparecem no seu terminal, mas um script ou um pipe pode separá-los: `java Revisor.java 2> errores.txt` manda só os erros para um arquivo, e `java Revisor.java > reporte.txt` guarda só o relatório. Por isso a mensagem de configuração é impressa com `System.err.println(...)`. Quem usar o programa em um script deve poder guardar o relatório limpo e ver o erro à parte. `IO.println` não tem versão para a saída de erro, e por isso daqui em diante se usam `System.out` e `System.err` explicitamente quando a diferença importa.

### 5.4 Uma exceção ou um valor?

Você já tem as duas ferramentas para representar que algo não saiu como se esperava: a **exceção** e o **valor** (um `Optional`, ou um tipo selado como `Respuesta`). E a pergunta de projeto que mais importa na lição: qual uso quando? O programa `fig05_09` verifica os mesmos três nomes das duas maneiras, com um serviço que não responde no meio:

```java
// fig05_09.java
long tiempoOLanza(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142;
    case "pagos" -> 87;
    default -> throw new IllegalStateException(nombre + ": sin conexión");
  };
}

OptionalLong tiempoO(String nombre) {
  return switch (nombre) {
    case "catalogo" -> OptionalLong.of(142);
    case "pagos" -> OptionalLong.of(87);
    default -> OptionalLong.empty();
  };
}

void main() {
  List<String> nombres = List.of("catalogo", "reportes", "pagos");

  IO.println("con una excepción:");
  try {
    for (String nombre : nombres) {
      IO.println("  " + nombre + ": " + tiempoOLanza(nombre) + " ms");
    }
  } catch (IllegalStateException e) {
    IO.println("  se cayó el lote: " + e.getMessage());
  }

  IO.println("con un valor:");
  for (String nombre : nombres) {
    OptionalLong tiempo = tiempoO(nombre);
    String texto = tiempo.isPresent() ? tiempo.getAsLong() + " ms" : "sin respuesta";
    IO.println("  " + nombre + ": " + texto);
  }
}
```

```text
$ java fig05_09.java
con una excepción:
  catalogo: 142 ms
  se cayó el lote: reportes: sin conexión
con un valor:
  catalogo: 142 ms
  reportes: sin respuesta
  pagos: 87 ms
```

Com a exceção, no segundo serviço o programa abandonou o percurso inteiro: `pagos`, que estava perfeitamente bem, nunca foi verificado. Com o valor, cada serviço produziu sua própria resposta, a ausência de um não afetou os outros, e o relatório saiu completo. Essa é a razão de o `Respuesta.SinRespuesta` da lição 3 ser um valor.

Seria possível corrigir a primeira versão pondo um `try` dentro do laço, para capturar cada serviço separadamente. Funcionaria, mas repare no que você fez: converteu a exceção em um valor a cada volta, ou seja, **reinventou o valor**, com mais código. A diferença entre as duas ferramentas é de intenção, e daí sai uma regra para decidir:

| Pergunta | Se a resposta é sim |
|---|---|
| É **um dos resultados normais** da operação? (“este serviço está fora do ar”). | **Um valor.** |
| Quem chama tem um caminho normal para **seguir** com o próximo? (um lote de cem serviços). | **Um valor.** |
| A operação **não pôde ser feita** e seguir seria inventar? (o arquivo de configuração não existe). | **Uma exceção.** |
| É um **defeito do programa**, de quem o escreveu? (um `null` inesperado). | **Uma exceção não verificada.** |

Para o `revisor`, então: um serviço fora do ar é um valor (`SinRespuesta`) porque a pergunta que o programa responde é “está no ar?”, e “não” é uma resposta. Um arquivo de configuração ilegível é uma exceção (`ErrorDeConfiguracion`), porque sem lista de serviços o programa não tem nada a verificar, e seguir seria inventar uma lista. É uma decisão de projeto e quem a toma é você, mas não é arbitrária: a lição inteira se reduz a fazer a si mesmo essa pergunta cada vez que algo pode dar errado.

### 5.5 `InterruptedException`: não se engole

A última exceção da lição é a mais mal-entendida. O `revisor` de hoje vai **esperar**: para simular que um serviço demora o que seu tempo diz, a verificação chama `Thread.sleep(ms)`, que detém a thread esse número de milissegundos. E `Thread.sleep` declara **`throws InterruptedException`**, uma exceção verificada. Por quê?

Um programa Java pode ter várias threads de execução ao mesmo tempo (a lição 9 é toda sobre isso, e hoje basta saber que uma *thread* é uma linha de execução independente e que o seu programa já tem uma, a thread principal). Uma thread pode pedir a outra que **pare**: isso se chama **interrompê-la**, e é um pedido cortês, não uma ordem: a thread interrompida decide como e quando parar. Mecanicamente, interromper uma thread acende uma **flag** nela (seu “estado de interrupção”). Se a thread está dormindo em um `sleep` quando isso acontece, o `sleep` acorda de imediato e lança `InterruptedException`. Segundo [a documentação do tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html): uma interrupção é “uma indicação a uma thread de que ela deve deixar o que está fazendo e fazer outra coisa; cabe ao programador decidir como ela responde, mas é muito comum que a thread termine”.

É verificada para que você não possa esquecê-la: alguém pediu ao seu código que parasse, e ignorar isso quase nunca é o correto. Para ver a mecânica sem precisar de duas threads, o programa `fig05_10` se interrompe a si mesmo: `Thread.currentThread()` devolve “a thread que executa este código”, e `interrupt()` acende sua flag.

```java
// fig05_10.java
void main() {
  Thread.currentThread().interrupt();
  IO.println("¿marcado como interrumpido? " + Thread.currentThread().isInterrupted());
  try {
    Thread.sleep(1000);
    IO.println("esto no se imprime");
  } catch (InterruptedException e) {
    IO.println("sleep lanzó: " + e.getMessage());
    IO.println("¿sigue marcado? " + Thread.currentThread().isInterrupted());
    Thread.currentThread().interrupt();
    IO.println("¿y después de restaurarlo? " + Thread.currentThread().isInterrupted());
  }
}
```

```text
$ java fig05_10.java
¿marcado como interrumpido? true
sleep lanzó: sleep interrupted
¿sigue marcado? false
¿y después de restaurarlo? true
```

A primeira linha confirma que a flag está acesa. O `sleep(1000)` **não dormiu nada**: viu a flag, lançou `InterruptedException` com a mensagem `sleep interrupted` e, e isto é o importante, **apagou a flag** ao lançá-la: a terceira linha diz `false`. A [documentação de `Thread`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html) diz assim: “o estado de interrupção da thread atual é limpo quando esta exceção é lançada”. Isso cria um problema: se você captura a exceção e segue como se nada tivesse acontecido, o pedido de parar **desaparece**, e ninguém mais (por exemplo, o código que chamou o seu) fica sabendo de que alguém queria parar. Daí as duas regras, e **não há uma terceira**:

1. **Propague-a**: declare `throws InterruptedException` e deixe-a subir. É o que o `revisor` faz.
2. **Se você precisa capturá-la**, restaure a flag antes de seguir: `Thread.currentThread().interrupt()`. É a quarta linha da figura: deixou a flag acesa outra vez (`true`) para quem vier atrás.

O que não se faz é capturá-la e calar (`catch (InterruptedException e) {}`): é a versão da seção 5.3.2 para um caso em que o custo é pior, porque um programa que não obedece quando lhe pedem que pare não pode ser desligado com elegância. Na lição 9 você verá quem interrompe e por quê.

### 5.6 O passo do projeto: o `revisor` lê sua configuração

Agora é juntar tudo. O `revisor` muda assim:

- **`servicios.txt`**, um arquivo de texto, substitui a lista escrita à mão. Tem um serviço por linha, com o nome, o endereço e, opcionalmente, o tempo limite em milissegundos. As linhas em branco são ignoradas e as que começam com `#` são comentários.
- **`ErrorDeConfiguracion`** é a exceção da seção 5.3.
- **`Configuracion`** lê o arquivo e o converte em uma lista de `Servicio`, e é onde vivem as mensagens de erro.
- **`Revision`** muda: a verificação espera o que diz o tempo simulado, e por isso `revisar` e `revisarTodos` declaram `throws InterruptedException`. `revisarTodos` passa de *stream* a `for`, pelo que você viu em 5.1.4.
- **`Revisor.java`** lê a configuração, captura `ErrorDeConfiguracion` e a imprime na saída de erro.

Nove arquivos não mudam em relação à lição 4: `Estado`, `Respuesta`, `Servicio`, `Resultado`, `Clasificador`, `Simulador`, `Resumen`, `Reporte` e `Formato`. Estão aqui porque a pasta da figura é o projeto completo. Copie-os tal qual da lição 4:

```java
// fig05_11/Estado.java
enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig05_11/Respuesta.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig05_11/Servicio.java
import java.net.URI;

record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}
```

```java
// fig05_11/Resultado.java
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
// fig05_11/Clasificador.java
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
// fig05_11/Simulador.java
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
// fig05_11/Resumen.java
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
// fig05_11/Reporte.java
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
// fig05_11/Formato.java
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

O arquivo de dados é o que descreve o início desta seção: o nome, o endereço e, se você quiser, o tempo limite. Os endereços continuam terminando em `.example`, um domínio reservado que nunca resolve: nenhuma requisição do curso sai para a internet.

```text fig05_11/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

A exceção própria é a da seção 5.3.1, com seus dois construtores:

```java
// fig05_11/ErrorDeConfiguracion.java
final class ErrorDeConfiguracion extends Exception {
  ErrorDeConfiguracion(String mensaje) {
    super(mensaje);
  }

  ErrorDeConfiguracion(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
```

`Configuracion` é a peça grande, e vale a pena lê-la por partes. Veja o código completo e depois seguimos:

```java
// fig05_11/Configuracion.java
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

**`leer`** é o método público. Abre o arquivo com `try-with-resources` e percorre as linhas com o `while` da seção 5.2.2, mantendo a contagem em `numero`. Para cada linha, `strip()` tira os espaços das pontas; se o resultado está vazio (`isEmpty()`) ou começa com `#` (`startsWith("#")`), `continue` salta de imediato para a linha seguinte (a palavra `continue` significa “termine esta volta do laço e passe à seguinte”). Qualquer outra linha é convertida em um `Servicio` com `leerRenglon`. Os dois `catch` finais convertem as exceções do JDK em `ErrorDeConfiguracion`, **passando a original como causa**: primeiro `NoSuchFileException`, que dá “no existe el archivo” (“não existe o arquivo”), e depois a `IOException` geral, que dá “no se pudo leer el archivo” (“não foi possível ler o arquivo”). O nome do arquivo sai de `ruta.getFileName().toString()`, e isso explica a mensagem da figura de cima: diz `servicios.txt`, não o caminho completo. Repare também em que `numero` conta as linhas **do arquivo**, incluídos os comentários e os vazios: se o erro está na linha 4 do arquivo, a mensagem diz 4, e é o número que você verá no seu editor.

**`leerRenglon`** parte a linha em campos com `renglon.split("\\s+")`. `split` recebe uma **expressão regular** (uma descrição de um padrão de texto) e devolve um **array** de textos: `\s+` significa “um ou mais espaços ou tabulações”, e se escreve com barra invertida dupla porque em um texto de Java a barra invertida tem um uso próprio (`\n`, `\"`) e é preciso escrevê-la duas vezes para que chegue uma. Um **array** (`String[]`) é uma sequência de tamanho fixo: consulta-se com `campos[0]` (o primeiro) e seu tamanho é `campos.length`, sem parênteses, porque é um dado e não um método. É o mesmo tipo de `String[] args` que você verá na lição 6. Se há menos de dois campos ou mais de três, a mensagem diz quantos há: `renglón 3: se esperaban 2 o 3 campos y hay 1` (“linha 3: eram esperados 2 ou 3 campos e há 1”).

**`leerUrl`** e **`leerTimeout`** são os dois validadores. `leerUrl` rejeita o que não comece com `http://` ou `https://` (primeiro filtro, com sua mensagem) e depois tenta construir um `URI` com `new URI(texto)`. Essa forma lança `URISyntaxException`, verificada, quando o endereço está mal escrito, e `leerUrl` a converte em `ErrorDeConfiguracion` com a `URISyntaxException` como causa. (É a razão de usar `new URI(...)` e não `URI.create(...)`, que você usava na lição 3: `URI.create` embrulha a mesma exceção em uma não verificada, e aqui convém vê-la tal qual.) `leerTimeout` converte o texto com `Integer.parseInt` e rejeita o que não seja um inteiro maior que zero. Repare em como está escrito, porque é um padrão útil: o `try` tenta a conversão e, se o número é válido, **devolve ali mesmo**; se a conversão falha, o `catch` lança o erro com a `NumberFormatException` como causa; e se a conversão funciona mas o número não é maior que zero, o `try` não devolve nada e o fluxo cai na última linha, que lança a mesma mensagem sem causa. Uma mensagem, dois caminhos de erro.

**`verificarRepetidos`** usa o truque da seção 4.1.2: `vistos.add(nombre)` devolve `false` se o nome já estava, e esses nomes são reunidos em um `LinkedHashSet` (que conserva a ordem em que foram encontrados) e `String.join(", ", repetidos)`, que une os elementos com um separador, os lista todos na mensagem: `nombres repetidos: catalogo` (“nomes repetidos: catalogo”). Um detalhe sobre o que este método faz: **o programa para no primeiro erro**, e não reúne todos os erros do arquivo para dizê-los de uma vez. É mais simples, e suficiente para um arquivo de quatro linhas.

Agora `Revision`, que muda de duas maneiras. Primeiro, `revisar` espera o tempo da resposta com `Thread.sleep(tiempo.getAsLong())` se houver (o `OptionalLong` da lição 4), e por isso declara `throws InterruptedException`. Segundo, `revisarTodos` é um `for` que preenche uma lista, porque a lambda de um `map` não pode lançar a exceção verificada (a sua `Function` não a declara), como se viu na seção 5.1.4:

```java
// fig05_11/Revision.java
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

E `Revisor.java` fecha o círculo. Lê a configuração, verifica e monta o relatório, e captura `ErrorDeConfiguracion` para imprimi-la na saída de erro com o prefixo `error de configuración:` (“erro de configuração:”). Seu `main` declara `throws InterruptedException`, que é a regra 1 da seção 5.5: não a trata porque não saberia o que fazer, apenas a deixa subir.

```java
// fig05_11/Revisor.java
void main() throws InterruptedException {
  Path ruta = Path.of("servicios.txt");
  try {
    List<Servicio> servicios = Configuracion.leer(ruta);
    IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
  } catch (ErrorDeConfiguracion e) {
    System.err.println("error de configuración: " + e.getMessage());
  }
}
```

Rode o projeto a partir da sua pasta. A verificação **já espera**: a tabela leva uns dois segundos e meio para aparecer (a soma de 142, 2300 e 87 ms; medi uns três segundos no total com a inicialização do Java incluída). É o que aconteceria em série com serviços de verdade, e é o problema que a lição 9 resolverá.

```text
$ cd fig05_11
$ java Revisor.java
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     142ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      87ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms
```

E agora, o destino da lição. Rode o `revisor` **a partir de outra pasta**, sem passar por onde está o arquivo de configuração. Depois execute-o com a saída de erro descartada (`2>/dev/null`, que significa “mande o canal 2, a saída de erro, para o nada”) e peça o código de saída do último comando com `$?`:

```text
$ java fig05_11/Revisor.java
error de configuración: no existe el archivo servicios.txt
$ java fig05_11/Revisor.java 2>/dev/null
$ echo "código de salida: $?"
código de salida: 0
```

Três coisas ocorreram. Primeiro, o programa disse `error de configuración: no existe el archivo servicios.txt` (“erro de configuração: o arquivo servicios.txt não existe”): o `Path.of("servicios.txt")` é relativo, foi resolvido contra a pasta onde você está parado, e ali o arquivo não existe, ainda que exista, sim, na pasta do projeto. Segundo, com `2>/dev/null` **não apareceu nada**: a mensagem ia pelo canal de erro, e ao descartá-lo não resta saída. É exatamente a separação da seção 5.3.3. Terceiro, o código de saída foi `0`: o programa falhou, e o sistema operacional acredita que tudo saiu bem. Para um script que executa o `revisor` isso é mentira, e a lição 10 corrige com `System.exit`. Por ora, anote como defeito conhecido.

## O erro que você vai ver

O erro desta lição é o da seção 5.1.2, e quase todos o encontram na primeira vez que usam um método de entrada e saída:

```text
fig05_02.java:3: error: unreported exception IOException; must be caught or declared to be thrown
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
                                             ^
```

**Primeira linha: onde e o quê.** Traz o arquivo e a linha (`fig05_02.java:3`), a palavra `error` e o diagnóstico: `unreported exception IOException`. “Não reportada” quer dizer que o método pode lançar essa exceção e ninguém a capturou nem a declarou. **Segunda e terceira: a linha do código e a seta.** A `^` aponta para o parêntese da chamada que pode lançá-la, que é o que você deve olhar. **O que fazer.** Há duas correções, e a escolha é de projeto. Se nesse lugar você sabe o que fazer quando falha, **capture-a** (e faça algo útil, não um `catch` vazio). Se não sabe, **declare-a com `throws`** e deixe que quem chamou a trate. Má correção, que às vezes se vê: declarar `throws Exception` para calar o compilador. Funciona, e esconde das assinaturas o que pode dar errado.

O segundo erro da lição não é do compilador: é o `revisor` a partir de outra pasta, e você já o viu na seção 5.6. A mensagem `no existe el archivo servicios.txt` não diz a você **onde** ele o procurou. Se você topar com ela e o arquivo existir, a primeira coisa que deve revisar é a partir de que pasta você rodou `java`.

## O que se faz errado

- **Um `catch` vazio**, ou com apenas `printStackTrace()`. É o da seção 5.3.2: o erro desaparece, ou é impresso e o programa segue como se nada tivesse acontecido. Custo: valores errados que ninguém vê, e quando por fim algo falha, um rastro que já não existe.

- **Capturar `Exception` ou `Throwable` por via das dúvidas.** Captura também os defeitos de programação (uma `NullPointerException`) e os converte em uma mensagem enganosa. Custo: o defeito real fica enterrado. O único lugar para um `catch (Exception e)` é a última fronteira do programa, para registrar e terminar com elegância, não para continuar.

- **Converter uma exceção em outra sem passar a causa.** É escrever `throw new ErrorDeConfiguracion("el archivo no sirve")` sem o segundo argumento. Custo: a mensagem diz que falhou e a causa, que era o que permitia consertar, já não está.

- **Devolver um valor mágico** (`-1`, `null`, uma string vazia) em vez de lançar ou de devolver um valor com tipo. Custo: quem chama tem de lembrar de verificar, e o compilador não o ajuda.

- **Um `return` dentro de um `finally`.** Se o `finally` devolve um valor, **descarta** a exceção que estava subindo, e o erro se perde sem um único stack trace. Custo: o mesmo de um `catch` vazio, com a diferença de que o código parece mais inocente. O `finally` é para limpar, não para decidir o que se devolve.

- **Engolir uma `InterruptedException`.** Custo: um programa que não obedece quando lhe pedem que pare (seção 5.5). Propague-a ou restaure a flag.

- **Usar uma exceção para o fluxo normal.** Por exemplo, chamar `Integer.parseInt` em um laço e capturar `NumberFormatException` para decidir se um texto “é um número”, quando é o esperado, não o excepcional. Custo: código mais lento (criar o stack trace custa) e a decisão de projeto da seção 5.4 ao contrário: se a ausência de um resultado é normal, é um valor.

## Exercícios

### Exercício 1 — Um arquivo quebrado para cada erro

`Configuracion` sabe falhar de várias maneiras, cada uma com sua mensagem. Para cada um dos oito casos abaixo, **preveja a mensagem antes de rodar** e depois confira. Crie uma pasta nova por caso (por exemplo, `roto-1/`), ponha dentro um `servicios.txt` com o conteúdo indicado e rode o `revisor` **a partir dessa pasta**: `java ../revisor/Revisor.java` (ajuste o caminho para onde você tiver o seu projeto). Como o programa procura `servicios.txt` na pasta atual, você não precisa tocar no código.

1. `catalogo https://catalogo.example` e, na linha seguinte, apenas `pagos`.
2. Um comentário (`# x`) na linha 1 e, na linha 2, `catalogo ftp://catalogo.example`.
3. `catalogo https://c.example` e `pagos http://pa|gos.example`.
4. `catalogo https://c.example 5000` e `pagos https://p.example abc`.
5. Duas linhas, as duas com o nome `catalogo`.
6. Um arquivo com apenas comentários e linhas em branco.
7. `catalogo https://c.example 0`.
8. `catalogo https://c.example 1 2 3`.

### Exercício 2 — Faça o motivo desaparecer

Mude `leerTimeout` em `Configuracion.java` para que o `catch (NumberFormatException e)` não lance nada e simplesmente devolva `5000`. Em uma pasta nova, ponha um `servicios.txt` com a única linha `pagos https://p.example abc` e rode a partir dali (`java ../revisor/Revisor.java`). O que você vê? Depois restaure o código. Anote no seu diário de bordo quanto tempo você levaria para descobrir o problema se o arquivo tivesse cem linhas.

### Exercício 3 — Um arquivo em outra codificação

Em uma pasta nova (`mkdir latin1 && cd latin1`), crie um arquivo com um acento escrito na codificação antiga ISO-8859-1 (o *latin1* dos sistemas velhos), que não é UTF-8, com este comando:

```bash
printf 'catalogo https://catalogo.example\n# caf\xe9\n' > servicios.txt
```

Rode o `revisor` a partir dessa pasta (`java ../revisor/Revisor.java`) e leia a mensagem. Depois, acrescente uma linha ao `catch` de `Revisor.java` para imprimir a causa (`e.getCause()`) e descubra que exceção do JDK está por trás da mensagem. Restaure o código.

### Exercício 4 — Converta `revisarTodos` de novo em um *stream*

Substitua o `for` de `revisarTodos` por `servicios.stream().map(Revision::revisar).toList()`. **Preveja** se compila e o que o compilador dirá. Depois confira. E responda: o que você teria de mudar em `revisar` para que essa versão compile?

## Soluções

### Solução 1

| Conteúdo de `servicios.txt` | Mensagem |
|---|---|
| `catalogo https://catalogo.example` e `pagos` | `error de configuración: renglón 2: se esperaban 2 o 3 campos y hay 1` (“erro de configuração: linha 2: eram esperados 2 ou 3 campos e há 1”) |
| `catalogo ftp://catalogo.example` (depois de um comentário) | `error de configuración: renglón 2: la url debe empezar con http:// o https://` (“erro de configuração: linha 2: a url deve começar com http:// ou https://”) |
| `pagos http://pa\|gos.example` (linha 2) | `error de configuración: renglón 2: la url no es válida` (“erro de configuração: linha 2: a url não é válida”) |
| `pagos https://p.example abc` (linha 2) | `error de configuración: renglón 2: timeout_ms debe ser un entero mayor que 0` (“erro de configuração: linha 2: timeout_ms deve ser um inteiro maior que 0”) |
| duas linhas com `catalogo` | `error de configuración: nombres repetidos: catalogo` (“erro de configuração: nomes repetidos: catalogo”) |
| apenas comentários e linhas em branco | `error de configuración: el archivo no tiene servicios` (“erro de configuração: o arquivo não tem serviços”) |
| `catalogo https://c.example 0` | `error de configuración: renglón 1: timeout_ms debe ser un entero mayor que 0` (“erro de configuração: linha 1: timeout_ms deve ser um inteiro maior que 0”) |
| `catalogo https://c.example 1 2 3` | `error de configuración: renglón 1: se esperaban 2 o 3 campos y hay 5` (“erro de configuração: linha 1: eram esperados 2 ou 3 campos e há 5”) |

Medi as oito mensagens com o projeto da seção 5.6. Há duas coisas a olhar com cuidado. No caso 2, o comentário da linha 1 conta: o erro está na linha 2 **do arquivo**, não no segundo serviço. E nos casos 4 e 7 a mensagem é a mesma ainda que o motivo seja diferente (um texto que não é número, e um número que não é positivo): para quem escreve o arquivo, a correção é a mesma. A diferença está na causa, que só existe no primeiro (a `NumberFormatException`), e quem a vê é quem depura.

### Solução 2

```java
static int leerTimeout(String texto, int numero) throws ErrorDeConfiguracion {
  String mensaje = "renglón " + numero + ": timeout_ms debe ser un entero mayor que 0";
  try {
    int timeoutMs = Integer.parseInt(texto);
    if (timeoutMs > 0) {
      return timeoutMs;
    }
  } catch (NumberFormatException e) {
    return 5000;
  }
  throw new ErrorDeConfiguracion(mensaje);
}
```

Com um `servicios.txt` que só tenha `pagos https://p.example abc`, o `revisor` imprime a tabela de `pagos` como `ARRIBA`, sem nenhum aviso: `abc` desapareceu e o serviço ficou com o tempo limite padrão. Com cem linhas, você não teria como saber qual delas tinha um tempo inválido: o programa “funciona”. É o problema da seção 5.3.2 em pequeno. A mensagem de erro original (`timeout_ms debe ser un entero mayor que 0`) não era um incômodo: era o único aviso que a pessoa que escreve o arquivo ia receber.

### Solução 3

A mensagem é `error de configuración: no se pudo leer el archivo servicios.txt` (“erro de configuração: não foi possível ler o arquivo servicios.txt”): não diz por quê. Com `System.err.println(e.getCause());` no `catch` de `Revisor.java`, é impresso:

```text
java.nio.charset.MalformedInputException: Input length = 1
```

O byte `0xE9` é o `é` em ISO-8859-1, mas como sequência UTF-8 é inválida (em UTF-8 o `é` ocupa dois bytes). `Files.newBufferedReader` decodifica em UTF-8 e, ao topar com o byte, lança `MalformedInputException`, uma `IOException` que `Configuracion.leer` converte em “não foi possível ler”. Observe que a mensagem para a pessoa é correta mas pouco informativa, e que a **causa** tem o dado que faz falta. É a utilidade de conservá-la: mensagem curta para o usuário, causa completa para quem depura.

### Solução 4

Não compila. O compilador diz (encurtei o caminho, que o lançador imprime completo):

```text
Revision.java:20: error: incompatible thrown types InterruptedException in functional expression
    return servicios.stream().map(Revision::revisar).toList();
                                  ^
```

`Revision::revisar` declara `throws InterruptedException` e a `Function` que `map` espera não pode lançá-la. Para que compile você teria de tirar o `throws` de `revisar`, e isso significa **capturar a `InterruptedException` dentro de `revisar`**, onde você não sabe o que fazer com ela; o correto seria restaurar a flag com `Thread.currentThread().interrupt()` e devolver algo, o que esconde a interrupção de quem chamou. Por isso o curso conserva o `for`: é o que deixa a exceção passar até quem pode, de fato, decidir.

## Como sei que consegui

- [ ] `java fig05_01.java` imprime as cinco cadeias, e você consegue dizer por que `NoSuchFileException` é verificada e `NumberFormatException` não.
- [ ] `java fig05_02.java` falha com `unreported exception IOException`, e você sabe as duas correções que o compilador aceita.
- [ ] `java fig05_04.java` falha com `has already been caught`, e você sabe por que a ordem dos `catch` vai do particular ao geral.
- [ ] `java fig05_06.java` imprime os fechamentos em ordem inversa, e você encontra na saída a exceção suprimida.
- [ ] A partir da pasta da figura das exceções próprias, `java Demo.java` imprime um `Caused by:`, e você sabe que exceção do JDK está por trás.
- [ ] `java fig05_10.java` imprime `true`, `false` e `true`, e você sabe explicar o que cada um fez.
- [ ] A partir da pasta do projeto, `java Revisor.java` imprime a tabela com `4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms`, e leva uns dois segundos e meio.
- [ ] A partir de outra pasta, o `revisor` imprime `error de configuración: no existe el archivo servicios.txt`, e você sabe explicar por que o arquivo existe e mesmo assim não o encontra.
- [ ] Os oito arquivos quebrados do exercício 1 produzem as oito mensagens da solução, e você as previu antes de rodar.
- [ ] Você anotou no seu diário de bordo uma exceção verificada que obrigou você a decidir algo.

## Resumo

Responda sem olhar a lição:

1. Que diferença há entre uma exceção verificada e uma não verificada? Qual o compilador obriga a tratar?
2. O que significa `unreported exception IOException; must be caught or declared to be thrown`, e quais são as duas correções?
3. O que um `finally` garante? E por que não deve levar um `return`?
4. Por que a ordem dos `catch` vai do particular ao geral?
5. O que `try-with-resources` garante diante de fechar à mão? Em que ordem fecha?
6. Por que uma exceção verificada não pode ser lançada de dentro da lambda de um `map`, e o que o `revisor` usa no lugar?
7. O que é a causa de uma exceção, e o que acontece se você não a passa ao convertê-la em outra?
8. Por que um serviço fora do ar é um valor e um arquivo de configuração ilegível é uma exceção?
9. O que `Thread.sleep` faz quando alguém interrompe a thread, e o que acontece com a flag de interrupção?
10. Quais são as duas formas corretas de tratar uma `InterruptedException`?

## Para ler mais

- [Oracle, “Lesson: Exceptions” (tutorial do Java)](https://docs.oracle.com/javase/tutorial/essential/exceptions/index.html): o tema completo, desde o que é uma exceção até encadeá-las, com a discussão sobre as não verificadas. Consultado em 7 de outubro de 2026.
- [The Java Language Specification, capítulo 11, “Exceptions”](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html): a definição exata de verificada e não verificada e do que cada método deve declarar. Consultado em 7 de outubro de 2026.
- [Oracle, “The try-with-resources Statement”](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html): a ordem de fechamento e as exceções suprimidas, com exemplos. Consultado em 7 de outubro de 2026.
- [Oracle, “Interrupts” (tutorial de concorrência)](https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html): como se detém uma thread com cortesia, e por que `InterruptedException` não é ignorada. Consultado em 7 de outubro de 2026.

---

**Anterior:** [Lição 4 — Coleções, genéricos e streams](04-colecciones-genericos-streams.md) ·
**Próxima:** [Lição 6 — Projeto com Maven](06-proyecto-maven.md)
