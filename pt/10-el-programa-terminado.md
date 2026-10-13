# Lição 10 — O programa terminado

**Tempo:** 2 × 90 min (duas sessões; o corte fica entre as seções 10.3 e 10.4)

**O que você constrói:** o `revisor` completo, pronto para entregar: é controlado pela linha de comando, termina com um código que um script consegue ler, pode servir o seu relatório por HTTP, passa pelos portões de qualidade e é empacotado em um único JAR

**O que você aprende:** argumentos e códigos de saída como contrato; o servidor HTTP do próprio JDK; o encerramento ordenado com um gancho de encerramento; Checkstyle e SpotBugs dentro do `mvn verify`, e como ler o que eles encontram; uma revisão da segurança do `revisor`; e o JAR executável com `maven-shade-plugin`

**De onde você vem.** Você traz o projeto da [Lição 9](09-concurrencia.md): um `revisor` concorrente que verifica vinte serviços no tempo do mais lento, com 28 testes em verde. Funciona, mas ainda é um projeto de programador e não um programa que outra pessoa possa usar. Para executá-lo é preciso digitar `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` e estar parado na pasta certa; o limite de concorrência é uma constante que se muda recompilando; não há como pedir o relatório em JSON; se tudo falha, o programa termina igual a quando tudo está bem; e ninguém fez nele uma revisão de estilo nem uma busca automática de defeitos. Hoje essas cinco coisas se fecham. Você trabalha na sua pasta `~/curso-java/revisor/`.

**O que esta lição não faz.** Não usa uma biblioteca para a linha de comando (há boas, como *picocli*, mas o curso evita dependências desnecessárias e sete opções se leem bem à mão). Não coloca criptografia nem autenticação no servidor: ele foi pensado para ser consultado por outro processo da mesma máquina, e isso é explicado em 10.5. E não transforma o JAR em um instalador: `jpackage` fica como “o que vem aí”.

**Sobre as execuções.** As figuras 10.1 a 10.3 são arquivos compactos e são executadas com `java figNN_MM.java`. A figura 10.4 é o projeto; ela precisa do laboratório em outro terminal (`java Laboratorio.java`, porta 8089; a 8088 fica vazia de propósito) e, quando sobe o seu próprio servidor, usa a porta 8090. As saídas foram obtidas com Temurin 25.0.4, Maven 3.10.0 e os *plugins* fixados no `pom.xml`. Três avisos de honestidade: **os tempos são da minha máquina** (os seus serão outros; as proporções, não); **as mensagens do Checkstyle e de algumas partes da JVM saem no idioma do seu sistema** (as minhas saíram em espanhol; com `-Duser.language=en` saem em inglês); e os comandos que só existem no Linux (como `ss`) eu os escrevo do jeito que você vai usá-los no seu Mint, mas **não consegui executá-los na máquina onde medi**, então a saída exata quem vai ver é você.

## Ao terminar, você vai conseguir

- Ler à mão os argumentos da linha de comando, validá-los e devolver um erro de uso claro sem mostrar um stack trace.
- Explicar o que é um código de saída, fixar um para cada situação do `revisor` (`0`, `1`, `2`) e lê-lo no terminal com `$?`.
- Subir um servidor HTTP com as classes do JDK, escutando apenas em `127.0.0.1`, e desligá-lo com ordem quando alguém o interrompe.
- Executar Checkstyle e SpotBugs no `mvn verify`, ler as suas mensagens e explicar por que uma análise em silêncio não prova que não haja defeitos.
- Revisar o `revisor` contra uma lista de riscos concretos (endereços, redirecionamentos, serialização, interface de rede) e dizer o que cada decisão cobre.
- Empacotar o programa em um único JAR e executá-lo com `java -jar` a partir de qualquer pasta.
- Verificar, uma por uma, as seis condições com as quais o temário diz que o curso terminou.

## O porquê antes do como

Quando um programa está terminado? Não quando ele roda no seu computador: isso você já conseguiu desde a lição 5, e até a 9 cada lição foi testada assim. Um programa está terminado quando **outra pessoa, outro programa ou você mesmo daqui a seis meses consegue usá-lo sem ler o código**. Isso exige coisas que nada têm a ver com a lógica do `revisor`.

Exige uma **interface**: uma maneira de dizer a ele o que fazer sem editar nada. Exige uma **resposta honesta a quem o chama**: um script que roda o `revisor` a cada cinco minutos precisa saber se tudo correu bem sem ter de ler a tabela, e para isso existe o código de saída. Exige **qualidade que se verifica sozinha**, porque “eu revisei” não escala e um estilo que depende apenas da disciplina das pessoas se corrói. Exige que aquilo que **escuta na rede** o faça com cuidado. E exige uma **entrega** simples: um arquivo, um comando.

Esta lição pega essas cinco e as fecha. A **primeira** sessão (10.1 a 10.3) é a interface: argumentos, códigos de saída e o servidor com o seu encerramento ordenado. A **segunda** (10.4 a 10.6) é a entrega: os portões de qualidade, a revisão de segurança e o JAR. E o exercício 1 é a lista que o temário do curso chama de “Como você sabe que terminou”: seis verificações mensuráveis, que você mesmo faz, com o relógio e os comandos, e que dizem se o `revisor` está de fato terminado.

## Os conceitos

### 10.1 Argumentos e código de saída

#### 10.1.1 Os argumentos da linha de comando

Quando você digita `java -jar revisor.jar --config otro.json`, tudo o que vem depois do nome do programa chega ao `main` como um array de textos: o `String[] args` que você já conhece desde a lição 6. O Java **não os interpreta**: não sabe o que é `--config`, nem que `otro.json` é o seu valor, nem que `--concurrencia` pede um número. Apenas os entrega na ordem em que você os digitou. A figura 10.1 os imprime, e também mostra como se sai de um programa com um código:

```java
// fig10_01.java
void main(String[] args) {
  IO.println("recibí " + args.length + " argumentos: " + Arrays.toString(args));
  if (args.length == 1 && args[0].equals("--falla")) {
    System.err.println("error de uso: pediste fallar");
    System.exit(2);
  }
  IO.println("termino bien");
}
```

Sem argumentos, o programa os conta e termina normalmente:

```text
$ java fig10_01.java
recibí 0 argumentos: []
termino bien
```

Se você passar argumentos, eles chegam como estão, como textos soltos e em ordem. Esta é uma execução com os de um `revisor` típico (o lançador de arquivo único, `java Archivo.java`, entrega ao `main` tudo o que vai depois do nome do arquivo):

```text
$ java fig10_01.java --config servicios.json --concurrencia 3
recibí 4 argumentos: [--config, servicios.json, --concurrencia, 3]
termino bien
```

Repare que o `3` é o texto `"3"`, não um número: convertê-lo, e decidir o que fazer se alguém escrever `tres`, é trabalho seu. Isso é o que a classe `Opciones` fará em 10.1.3.

#### 10.1.2 O código de saída: um contrato com quem chama você

Todo programa, ao terminar, entrega ao sistema operacional um número entre 0 e 255: o seu **código de saída** (*exit status*). É a resposta mais curta possível à pergunta “como foi?”, e é o que leem os scripts, os agendadores de tarefas e as ferramentas de integração contínua. A convenção universal em sistemas do tipo Unix é: **`0` quer dizer sucesso, qualquer outro número quer dizer que algo deu errado**. Em um terminal com `bash`, o código do último comando está na variável `$?`, e o [manual do `bash`](https://www.gnu.org/software/bash/manual/html_node/Exit-Status.html) documenta essa convenção, que é a que `if`, `&&` e `set -e` observam.

A última parte de `fig10_01.java` já faz uso disso: com o único argumento `--falla`, o programa escreve um erro na saída de erro e termina com `System.exit(2)`:

```text
$ java fig10_01.java --falla
recibí 1 argumentos: [--falla]
error de uso: pediste fallar
$ echo $?
2
```

Dois detalhes que parecem menores e não são. O primeiro: o erro foi escrito com `System.err`, não com `IO.println`. Um programa tem **duas saídas** distintas: a **saída padrão** (`System.out`, para onde vai o resultado) e a **saída de erro** (`System.err`, para onde vão os avisos para uma pessoa). Se alguém redireciona o resultado para um arquivo —`revisor --formato json > reporte.json`— quer o JSON no arquivo e os erros na tela, sem misturá-los. O segundo: `System.exit` **termina toda a JVM e não retorna**: a linha que vem depois da chamada nunca é executada, nem o resto do programa, em nenhuma thread. Não é um corte brusco: ele inicia o desligamento ordenado da JVM, que executa os ganchos de encerramento (10.3) e espera que terminem antes de sair. Mas, do ponto de vista do seu código, tudo o que vinha depois já não acontece. Por isso ele é chamado em um único lugar, o `main`, e recebe um número que outro código calculou.

O `revisor` fixa assim os seus códigos, e isso é um **contrato**: aquilo que outros programas podem assumir e que, por isso, não se muda sem avisar.

| Código | Quando | O que se escreve |
|---|---|---|
| `0` | todos os serviços estão `ARRIBA` (ou foi pedido `--ayuda` ou `--version`) | o relatório, na saída padrão |
| `1` | pelo menos um serviço está `LENTO` ou `CAIDO` | o relatório, na saída padrão |
| `2` | as opções não são entendidas, ou a configuração não serve | uma mensagem na saída de erro |

A diferença entre `1` e `2` é a de que um script precisa: `1` quer dizer “o programa funcionou e encontrou um problema que você deve tratar”, e `2` quer dizer “o programa não conseguiu fazer o seu trabalho; o problema é de quem o executa”. Um monitoramento pode alertar de forma diferente para cada um.

Falta uma peça para poder **testar** esse contrato. Se todo o fluxo do programa vive no `main` e o `main` termina com `System.exit`, um teste que chamasse o `main` levaria consigo a JVM dos testes. A solução é a de sempre, separar o que decide do que age: o `main` é uma linha que apenas chama `System.exit(ejecutar(args, System.out, System.err))`, e todo o resto vive em `ejecutar`, um método que **recebe** as duas saídas como parâmetros e **devolve** o código. Um teste passa a ele dois streams em memória, lê o que ele escreveu e verifica o número que ele devolveu, sem terminar nada. É a mesma ideia da lição 9: comunique-se devolvendo valores.

#### 10.1.3 `Opciones`: ler os argumentos à mão

Esta é a classe que converte o array de textos em um valor já validado. Antes de lê-la, um resumo das sete flags que ela reconhece: `--config RUTA` (por padrão `servicios.json`), `--formato tabla|json` (por padrão `tabla`), `--concurrencia N` (por padrão 10, no mínimo 1), `--permitir h1,h2` (por padrão `127.0.0.1,localhost`), `--servir PUERTO`, `--version` e `--ayuda`. Qualquer outra coisa é um erro de uso. O erro é modelado com a sua própria exceção verificada, igual a `ErrorDeConfiguracion` na lição 5:

```java
// fig10_04/src/main/java/mx/habil/revisor/ErrorDeUso.java

package mx.habil.revisor;

/** Los argumentos de la línea de comandos no se entienden; el mensaje dice cuál falló. */
final class ErrorDeUso extends Exception {

  private static final long serialVersionUID = 1L;

  ErrorDeUso(String mensaje) {
    super(mensaje);
  }
}
```

```java
// fig10_04/src/main/java/mx/habil/revisor/Opciones.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/** Lo que la persona pidió en la línea de comandos, ya validado. */
record Opciones(
    Path config,
    String formato,
    int concurrencia,
    Set<String> permitidos,
    OptionalInt puerto,
    boolean ayuda,
    boolean version) {

  Opciones {
    permitidos = Set.copyOf(permitidos);
  }

  static Opciones leer(String[] args) throws ErrorDeUso {
    Path config = Path.of("servicios.json");
    String formato = "tabla";
    int concurrencia = Revision.LIMITE_POR_OMISION;
    Set<String> permitidos = Configuracion.SERVIDORES_PERMITIDOS;
    OptionalInt puerto = OptionalInt.empty();
    boolean ayuda = false;
    boolean version = false;

    Deque<String> pendientes = new ArrayDeque<>(List.of(args));
    while (!pendientes.isEmpty()) {
      String bandera = pendientes.removeFirst();
      switch (bandera) {
        case "--ayuda" -> ayuda = true;
        case "--version" -> version = true;
        case "--config" -> config = Path.of(valor(pendientes, bandera));
        case "--formato" -> formato = formato(valor(pendientes, bandera));
        case "--concurrencia" ->
            concurrencia = entero(valor(pendientes, bandera), bandera, 1, Integer.MAX_VALUE);
        case "--permitir" -> permitidos = servidores(valor(pendientes, bandera));
        case "--servir" ->
            puerto = OptionalInt.of(entero(valor(pendientes, bandera), bandera, 1, 65535));
        default -> throw new ErrorDeUso("opción desconocida: " + bandera);
      }
    }
    return new Opciones(config, formato, concurrencia, permitidos, puerto, ayuda, version);
  }

  private static String valor(Deque<String> pendientes, String bandera) throws ErrorDeUso {
    if (pendientes.isEmpty()) {
      throw new ErrorDeUso("falta el valor de " + bandera);
    }
    return pendientes.removeFirst();
  }

  private static String formato(String texto) throws ErrorDeUso {
    if (!texto.equals("tabla") && !texto.equals("json")) {
      throw new ErrorDeUso("--formato debe ser tabla o json, no " + texto);
    }
    return texto;
  }

  private static int entero(String texto, String bandera, int minimo, int maximo)
      throws ErrorDeUso {
    try {
      int numero = Integer.parseInt(texto);
      if (numero >= minimo && numero <= maximo) {
        return numero;
      }
    } catch (NumberFormatException e) {
      // cae al error de abajo: un texto que no es número y un número fuera de rango dicen lo mismo
    }
    String rango =
        maximo == Integer.MAX_VALUE
            ? "mayor o igual que " + minimo
            : "entre " + minimo + " y " + maximo;
    throw new ErrorDeUso(bandera + " debe ser un entero " + rango + ", no " + texto);
  }

  private static Set<String> servidores(String texto) throws ErrorDeUso {
    Set<String> servidores =
        Arrays.stream(texto.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    if (servidores.isEmpty()) {
      throw new ErrorDeUso("--permitir necesita al menos un servidor");
    }
    return servidores;
  }
}
```

Quatro ideias da classe merecem um parágrafo.

**Os argumentos são consumidos de uma fila.** Em vez de percorrer o array com um índice e pular posições, eles são copiados para uma `ArrayDeque` e o primeiro é retirado com `removeFirst()`. Quando uma flag precisa do seu valor, ela chama `valor(pendientes, bandera)`, que retira o seguinte ou lança `ErrorDeUso("falta el valor de --config")` (“falta o valor de --config”) se não houver mais. Assim o código não tem aritmética de índices em que se possa errar.

**O `switch` decide com uma seta.** É a forma que você já conhece da lição 2, mas usada como instrução (cada caso atribui uma variável) e não como expressão. Com setas não há *fall-through*: cada caso termina em si mesmo. E o `default` lança o erro, de modo que uma flag escrita errada não passa em silêncio.

**Um número fora do intervalo e um texto que não é número dizem a mesma coisa.** `entero` tenta converter com `Integer.parseInt`; se falha, ou se o valor fica fora do intervalo, a mensagem é uma só (“debe ser un entero mayor o igual que 1, no tres”, “deve ser um inteiro maior ou igual a 1, e não tres”). Para a pessoa que escreveu a flag errada, não adianta distinguir as duas causas.

**O *record* se protege.** O seu construtor compacto faz `permitidos = Set.copyOf(permitidos)`: a mesma cópia defensiva da lição 4, e pela mesma razão, para que a coleção que o *record* guarda não seja uma que outra pessoa possa mudar depois. E uma anotação sobre `OptionalInt puerto`: a lição 4 disse que um `Optional` se usa como valor de retorno e não como campo. Aqui há uma exceção consciente: `Opciones` é um valor somente leitura que **descreve o que foi pedido**, e “não foi pedido servidor” não tem um número que o represente (para o servidor, o 0, sim, significa algo: “escolha uma porta livre”). O `OptionalInt` diz exatamente isso.

Com as opções já resolvidas, `ejecutar` as usa; você o vê completo em 10.3.3. Antes falta falar do outro modo como o `revisor` entrega o seu relatório.

### 10.2 O servidor HTTP do JDK

#### 10.2.1 Para que um servidor

Até agora, para ver o relatório é preciso executar o programa, que verifica e termina. Serve para uma pessoa em um terminal; não serve para outra ferramenta que queira consultar o estado a cada minuto, ou para um painel que mostre o resultado. Para isso, o `revisor` pode ficar **escutando**: com `--servir 8090` ele abre um servidor HTTP e, cada vez que alguém pede `/reporte`, faz uma verificação completa e responde o JSON da lição 8.

O bom é que não é preciso nenhuma biblioteca para conseguir isso. O JDK traz um servidor HTTP simples, o pacote [`com.sun.net.httpserver`](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html), parte do módulo `jdk.httpserver`. É uma API documentada e suportada (o nome `com.sun` é histórico), e é **a mesma classe que `Laboratorio.java` usa**, o programa que você vem executando desde a lição 8: se você o abrir, encontrará `HttpServer.create`, uma lista de `createContext` e um executor de threads virtuais. Hoje você escreve o que antes só usava. (Para servir arquivos estáticos, o JDK traz ainda uma ferramenta de linha de comando, `jwebserver`, descrita no [JEP 408](https://openjdk.org/jeps/408); não precisamos dela.)

#### 10.2.2 Um servidor mínimo, do começo ao fim

A figura 10.2 é o menor servidor que vale a pena ler. Ela sobe um servidor, registra nele uma rota que responde um JSON de uma linha, faz uma requisição a si mesma com o `HttpClient` da lição 8, imprime o que recebeu e se desliga. Como é um programa que começa e termina sozinho, pode ser verificado. (Uma nota de leitura: os quatro `import` do início são necessários porque um arquivo compacto só importa por si mesmo `java.base`, e nem o servidor nem o cliente HTTP vivem ali; se você os tirar, o compilador diz `cannot find symbol` para `HttpClient` e os demais.)

```java
// fig10_02.java
import com.sun.net.httpserver.HttpServer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

void main() throws Exception {
  var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  servidor.createContext(
      "/hola",
      intercambio -> {
        byte[] cuerpo = "{\"hola\": \"mundo\"}".getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json");
        intercambio.sendResponseHeaders(200, cuerpo.length);
        try (var salida = intercambio.getResponseBody()) {
          salida.write(cuerpo);
        }
      });
  servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
  servidor.start();

  InetSocketAddress direccion = servidor.getAddress();
  IO.println("dirección: " + direccion.getAddress().getHostAddress());
  IO.println("solo la interfaz local: " + direccion.getAddress().isLoopbackAddress());

  URI url = URI.create("http://127.0.0.1:" + direccion.getPort() + "/hola");
  try (var cliente = HttpClient.newHttpClient()) {
    var respuesta =
        cliente.send(HttpRequest.newBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
    IO.println("código " + respuesta.statusCode() + ", cuerpo " + respuesta.body());
  }
  servidor.stop(0);
  IO.println("servidor detenido");
}
```

```text
$ java fig10_02.java
dirección: 127.0.0.1
solo la interfaz local: true
código 200, cuerpo {"hola": "mundo"}
servidor detenido
```

Os cinco passos, um por um. **`HttpServer.create(direccion, 0)`** cria o servidor sem iniciá-lo; o segundo argumento é o tamanho da fila de conexões pendentes, e 0 significa “o que o sistema decidir”. **`createContext("/hola", manejador)`** registra o que fazer com as requisições cujo caminho começa com `/hola`. **`setExecutor(...)`** decide quem atende as requisições; **`start()`** o põe para escutar; **`stop(0)`** o detém (o número são os segundos que ele espera pelas requisições que estão em andamento, e volta em 10.3).

Três decisões desta figura que se repetem no servidor do `revisor`:

**O endereço é `127.0.0.1`, não “todos”.** `new InetSocketAddress("127.0.0.1", puerto)` faz o servidor escutar **somente na interface local**: apenas os programas desta mesma máquina podem falar com ele. Se em vez disso você escrever `new InetSocketAddress(puerto)`, sem endereço, o Java usa o endereço curinga e escuta em **todas as interfaces**, inclusive a rede: qualquer equipamento que alcance a sua máquina poderia pedir o relatório. A figura comprova isso ao imprimir `isLoopbackAddress()`. Para uma ferramenta interna que outro processo local consulta, a primeira é a certa; abrir a segunda é uma decisão que se toma de propósito, com autenticação e criptografia, não por padrão.

**A porta 0 significa “escolha uma livre”.** O sistema atribui qualquer uma que não esteja ocupada, e `servidor.getAddress().getPort()` diz qual foi. É o que os testes da lição 8 e os de hoje usam, para não colidir com o que estiver rodando.

**Uma thread virtual por requisição.** `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` é a lição 9 aplicada. Sem executor, o servidor atende **uma requisição por vez** em uma única thread; eu medi: seis requisições de 100 ms simultâneas chegaram ao manipulador uma de cada vez (máximo simultâneo: 1). Com uma thread virtual por requisição, uma requisição lenta não atrapalha as demais.

#### 10.2.3 `Servidor`: a rota `/reporte`

A classe que o `revisor` usa recebe a porta e **uma função que produz um `Reporte`** (um `Callable<Reporte>`). Que ela receba a função e não o trabalho já feito tem um motivo: a verificação deve acontecer **cada vez que alguém pede o relatório**, não uma única vez ao iniciar. E tem outro, que você verá nos testes: pode-se dar a ela um relatório fixo e verificar o que ela responde, sem tocar em nenhuma rede.

```java
// fig10_04/src/main/java/mx/habil/revisor/Servidor.java

package mx.habil.revisor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** El servidor del propio JDK que entrega el reporte en {@code /reporte}. */
final class Servidor {

  private Servidor() {}

  static HttpServer iniciar(int puerto, Callable<Reporte> revisar) throws IOException {
    HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
    servidor.createContext("/", intercambio -> atender(intercambio, revisar));
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    return servidor;
  }

  private static void atender(HttpExchange intercambio, Callable<Reporte> revisar)
      throws IOException {
    try (intercambio) {
      if (!"/reporte".equals(intercambio.getRequestURI().getPath())) {
        responder(intercambio, 404, "text/plain; charset=utf-8", "no existe");
      } else if (!"GET".equals(intercambio.getRequestMethod())) {
        responder(intercambio, 405, "text/plain; charset=utf-8", "solo GET");
      } else {
        try {
          responder(intercambio, 200, "application/json", Formato.json(revisar.call()));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          responder(intercambio, 500, "text/plain; charset=utf-8", "error interno");
        } catch (Exception e) {
          responder(intercambio, 500, "text/plain; charset=utf-8", "error interno");
        }
      }
    }
  }

  private static void responder(HttpExchange intercambio, int codigo, String tipo, String texto)
      throws IOException {
    byte[] cuerpo = texto.getBytes(StandardCharsets.UTF_8);
    intercambio.getResponseHeaders().set("Content-Type", tipo);
    intercambio.sendResponseHeaders(codigo, cuerpo.length);
    intercambio.getResponseBody().write(cuerpo);
  }
}
```

O que responde cada situação, para que você não tenha de adivinhar pelo código: um `GET /reporte` responde `200` com o JSON e `Content-Type: application/json`; qualquer outro caminho, `404`; qualquer outro método (um `POST`, por exemplo), `405`; e se a verificação falha por um erro do programa, `500` com o texto genérico `error interno` (“erro interno”). Esse último ponto é deliberado: a mensagem de uma exceção pode conter caminhos, nomes de classes ou dados que não devem sair pela rede, então a quem pergunta se diz que algo falhou e nada mais. (A quem opera o programa se diria por um registro, que este curso não constrói.) O `try (intercambio)` fecha a conversa com o cliente aconteça o que acontecer, e o corpo é escrito como bytes em UTF-8, porque o JSON leva acentos (“sin conexión”, “sem conexão”).

Um aviso de design que convém deixar dito. Cada `GET /reporte` faz **uma verificação completa**: consulta todos os serviços e demora o que o mais lento demorar (uns 2.5 segundos com os quatro canônicos). Não é um cache. Se dez pessoas pedem o relatório ao mesmo tempo, o `revisor` faz dez verificações ao mesmo tempo. É uma consequência que a seção 10.5 retoma, porque tem um lado de segurança.

### 10.3 O encerramento ordenado e o `revisor` completo

#### 10.3.1 O problema: um servidor que não sabe se desligar

Um servidor é um programa que não termina por si mesmo; termina quando alguém o interrompe: você com `Ctrl`+`C`, o sistema ao se desligar, ou uma ferramenta que lhe manda o sinal de término. Se naquele instante há uma requisição pela metade, interrompê-lo de repente a corta. A solução é o **encerramento ordenado** (*graceful shutdown*): deixar de aceitar requisições novas, dar alguns segundos às que já estão a caminho, e só então sair.

O Java oferece um mecanismo para isso: o **gancho de encerramento** (*shutdown hook*). É uma thread que você registra com `Runtime.getRuntime().addShutdownHook(hilo)` e que a JVM inicia quando começa a se desligar, seja qual for a razão: porque a última thread normal terminou, porque alguém chamou `System.exit`, ou porque o sistema lhe mandou um sinal de término como o de `Ctrl`+`C` (a [documentação de `Runtime`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html) enumera os casos). Não roda se o processo morre de repente, como com `kill -9` ou uma queda de energia; ali não há encerramento possível. A figura 10.3 o mostra na sua forma mais simples:

```java
// fig10_03.java
void main() throws InterruptedException {
  Runtime.getRuntime()
      .addShutdownHook(new Thread(() -> IO.println("gancho: cerrando con orden")));
  IO.println("trabajando...");
  Thread.sleep(300);
  IO.println("main termina");
}
```

```text
$ java fig10_03.java
trabajando...
main termina
gancho: cerrando con orden
```

O programa termina por conta própria (`main termina`, “o main termina”), e **depois** o gancho é executado. Com `Ctrl`+`C` aconteceria o mesmo. Duas regras para escrever um bom gancho: que seja **curto** e não fique bloqueado (a JVM espera que todos os ganchos terminem, então um gancho que nunca termina deixa o programa sem poder sair; se há um limite, ele vem de fora, por exemplo do sistema operacional que, ao se desligar, termina o processo de repente), e que **nunca chame `System.exit`** (o desligamento já está em andamento, e chamá-lo de dentro o deixa esperando por si mesmo para sempre).

#### 10.3.2 Como o `revisor` o usa

No modo servidor, o `revisor` faz três coisas, nesta ordem: inicia o servidor; registra um gancho que chama `servidor.stop(1)` (deixa de aceitar, espera até 1 segundo que as requisições em curso terminem, e fecha) e escreve `servidor detenido` (“servidor encerrado”); e **espera**. Essa espera é a parte enganosa. Se `ejecutar` devolvesse um número e o `main` chamasse `System.exit`, o servidor morreria um instante depois de nascer. Por isso a thread principal fica esperando com `new CountDownLatch(1).await()`: um trinco que ninguém vai abrir, de modo que `await()` **nunca retorna** e o programa vive até que a JVM seja desligada por fora, e com isso o gancho faz o seu trabalho.

Você vai testá-lo à mão em 10.3.5, assim que tiver o `Revisor` completo da seção seguinte e os seus testes em verde.

#### 10.3.3 O `revisor` completo

Agora sim se pode ver `Revisor.java` inteiro. Leia-o como uma receita de seis passos, que são os de `ejecutar`: **1** ler as opções (se falham: mensagem de uso e código 2); **2** se pediram `--ayuda` ou `--version`, responder e sair com 0; **3** ler a configuração, validada com a lista permitida que foi escolhida (se falha: mensagem e código 2); **4** se pediram `--servir`, entregar o trabalho a `servir` e não retornar; **5** se não, verificar tudo com o limite de concorrência pedido; **6** escrever a tabela ou o JSON e devolver 0 se todos os serviços estão no ar, e 1 se não.

```java
// fig10_04/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/** El revisor: consulta los servicios de la configuración y entrega el reporte. */
public final class Revisor {

  static final int CODIGO_TODO_ARRIBA = 0;
  static final int CODIGO_HAY_PROBLEMAS = 1;
  static final int CODIGO_USO_INVALIDO = 2;

  private static final String AYUDA =
      """
      uso: revisor [opciones]

        --config RUTA      archivo de servicios (por omisión servicios.json)
        --formato F        tabla o json (por omisión tabla)
        --concurrencia N   revisiones a la vez, al menos 1 (por omisión {limite})
        --permitir H1,H2   servidores permitidos (por omisión 127.0.0.1,localhost)
        --servir PUERTO    en vez de imprimir, sirve /reporte en 127.0.0.1:PUERTO
        --version          imprime la versión
        --ayuda            imprime esta ayuda

      códigos de salida: 0 todo arriba · 1 hay un servicio lento o caído · 2 uso o configuración
      inválidos
      """;

  private Revisor() {}

  /**
   * Punto de entrada: corre el revisor y sale con su código.
   *
   * @param args las opciones de la línea de comandos; {@code --ayuda} las lista
   */
  public static void main(String[] args) {
    System.exit(ejecutar(args, System.out, System.err));
  }

  static int ejecutar(String[] args, PrintStream salida, PrintStream errores) {
    Opciones opciones;
    try {
      opciones = Opciones.leer(args);
    } catch (ErrorDeUso e) {
      errores.println("error de uso: " + e.getMessage());
      errores.println("prueba: revisor --ayuda");
      return CODIGO_USO_INVALIDO;
    }
    if (opciones.ayuda()) {
      salida.print(ayuda());
      return CODIGO_TODO_ARRIBA;
    }
    if (opciones.version()) {
      salida.println("revisor " + version());
      return CODIGO_TODO_ARRIBA;
    }
    List<Servicio> servicios;
    try {
      servicios = Configuracion.leer(opciones.config(), opciones.permitidos());
    } catch (ErrorDeConfiguracion e) {
      errores.println("error de configuración: " + e.getMessage());
      return CODIGO_USO_INVALIDO;
    }
    try {
      if (opciones.puerto().isPresent()) {
        return servir(opciones.puerto().getAsInt(), servicios, opciones, salida);
      }
      Reporte reporte = revisar(servicios, opciones);
      boolean comoJson = opciones.formato().equals("json");
      salida.println(comoJson ? Formato.json(reporte) : Formato.tabla(reporte));
      boolean todoArriba = reporte.resumen().arriba() == reporte.resumen().total();
      return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;
    } catch (IOException e) {
      errores.println("no se pudo abrir el puerto: " + e.getMessage());
      return CODIGO_USO_INVALIDO;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      errores.println("revisión interrumpida");
      return CODIGO_HAY_PROBLEMAS;
    }
  }

  private static Reporte revisar(List<Servicio> servicios, Opciones opciones)
      throws InterruptedException {
    return Reporte.de(Revision.revisarTodos(servicios, opciones.concurrencia()));
  }

  private static int servir(
      int puerto, List<Servicio> servicios, Opciones opciones, PrintStream salida)
      throws IOException, InterruptedException {
    HttpServer servidor = Servidor.iniciar(puerto, () -> revisar(servicios, opciones));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  servidor.stop(1);
                  salida.println("servidor detenido");
                }));
    salida.println("sirviendo http://127.0.0.1:" + puerto + "/reporte (Ctrl+C para detener)");
    new CountDownLatch(1).await();
    return CODIGO_TODO_ARRIBA;
  }

  static String ayuda() {
    return AYUDA.replace("{limite}", String.valueOf(Revision.LIMITE_POR_OMISION));
  }

  static String version() {
    Properties propiedades = new Properties();
    try (InputStream flujo = Revisor.class.getResourceAsStream("/revisor.properties")) {
      if (flujo != null) {
        propiedades.load(flujo);
      }
    } catch (IOException e) {
      return "desconocida";
    }
    return propiedades.getProperty("version", "desconocida");
  }
}
```

Algumas coisas que não estão à vista. A versão que `--version` imprime **não está escrita no código**: vem do arquivo `revisor.properties`, que o Maven preenche com a versão do `pom.xml`, de modo que o número não se repete em dois lugares que se dessincronizem (você o monta logo abaixo). Se a porta está ocupada, `HttpServer.create` lança uma `IOException` e `ejecutar` a captura e diz `no se pudo abrir el puerto: Address already in use` (“não foi possível abrir a porta: Address already in use”), com código 2: é um problema de quem o executa. E o `catch (InterruptedException)` restaura a flag de interrupção, como ensinou a lição 5, antes de sair com 1.

Para que isso funcione são necessárias três mudanças fora do código, e você as faz agora, antes dos testes, porque um deles verifica justamente a versão. A primeira é o arquivo de propriedades, em uma pasta nova, `src/main/resources/`, a dos arquivos que acompanham o programa sem ser código (o Maven os copia para `target/classes`). É de uma única linha:

```properties fig10_04/src/main/resources/revisor.properties
version=${project.version}
```

As outras duas vão no seu `pom.xml`. A versão passa de `0.9.0` para **`1.0.0`**, a da entrega. E dentro de `<build>`, antes de `<plugins>`, acrescenta-se o bloco que pede ao Maven para **filtrar** essa pasta, isto é, substituir `${project.version}` pela versão do `pom.xml` ao copiar o arquivo:

```xml
  <version>1.0.0</version>
  ...
  <build>
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
      </resource>
    </resources>

    <plugins>
      ...
```

É um fragmento: o restante do `pom.xml` fica como você o traz da lição 9, com o `maven-dependency-plugin` incluído, que você ainda usa para executar o programa. O `pom.xml` completo da entrega aparece em 10.4.2, quando chegarem os portões de qualidade e o JAR.

O arquivo de configuração com os sete serviços (os quatro canônicos e os três extras do laboratório) fica na pasta do projeto, e você o usará mais abaixo:

```json fig10_04/servicios-extra.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "inventario", "url": "http://127.0.0.1:8089/inventario"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"},
    {"nombre": "reportes", "url": "http://127.0.0.1:8088/"},
    {"nombre": "facturacion", "url": "http://127.0.0.1:8089/facturacion"},
    {"nombre": "notificaciones", "url": "http://127.0.0.1:8089/notificaciones", "timeout_ms": 1000},
    {"nombre": "basura", "url": "http://127.0.0.1:8089/basura"}
  ]
}
```

#### 10.3.4 Os testes do contrato

`RevisorTest` traz seis testes, cada um sobre uma promessa da seção: a versão sai do `pom.xml`, uma concorrência inválida é um erro de uso com código 2, todos no ar dá 0, um fora do ar dá 1, o JSON de um relatório conhecido é **exatamente** o contrato da lição 8, e o servidor responde `200` com `application/json`.

```java
// fig10_04/src/test/java/mx/habil/revisor/RevisorTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RevisorTest {

  private static HttpServer servidor;
  private static int puerto;

  @TempDir Path carpeta;

  @BeforeAll
  static void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/bien", i -> responder(i, 200));
    servidor.createContext("/mal", i -> responder(i, 503));
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    puerto = servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, int codigo) throws IOException {
    intercambio.sendResponseHeaders(codigo, -1);
    intercambio.close();
  }

  /** Lo que dejó una corrida de {@code ejecutar}: el código de salida y los dos flujos. */
  private record Corrida(int codigo, String salida, String errores) {}

  private static Corrida correr(String... args) {
    var salida = new ByteArrayOutputStream();
    var errores = new ByteArrayOutputStream();
    int codigo =
        Revisor.ejecutar(
            args,
            new PrintStream(salida, true, StandardCharsets.UTF_8),
            new PrintStream(errores, true, StandardCharsets.UTF_8));
    return new Corrida(
        codigo,
        salida.toString(StandardCharsets.UTF_8),
        errores.toString(StandardCharsets.UTF_8));
  }

  private Path configuracionCon(String... rutas) throws IOException {
    var servicios = new StringBuilder();
    for (int i = 0; i < rutas.length; i++) {
      servicios.append(i == 0 ? "" : ",");
      String url = "http://127.0.0.1:" + puerto + rutas[i];
      servicios.append("{\"nombre\": \"s%d\", \"url\": \"%s\"}".formatted(i, url));
    }
    Path archivo = carpeta.resolve("servicios.json");
    Files.writeString(archivo, "{\"servicios\": [" + servicios + "]}", StandardCharsets.UTF_8);
    return archivo;
  }

  private static Reporte reporteConocido() {
    var servicios =
        List.of(
            resultado("catalogo", "http://127.0.0.1:8089/catalogo", new Respondio(200, 142)),
            resultado("inventario", "http://127.0.0.1:8089/inventario", new Respondio(200, 2300)),
            resultado("pagos", "http://127.0.0.1:8089/pagos", new Respondio(200, 87)),
            resultado("reportes", "http://127.0.0.1:8088/", new SinRespuesta("sin conexión")));
    return Reporte.de(servicios);
  }

  private static Resultado resultado(String nombre, String url, Respuesta respuesta) {
    return new Resultado(
        new Servicio(nombre, URI.create(url)), Clasificador.clasificar(respuesta), respuesta);
  }

  @Test
  void laVersionSaleDelPomYElCodigoEsCero() {
    Corrida corrida = correr("--version");
    assertEquals(0, corrida.codigo());
    assertEquals("revisor 1.0.0", corrida.salida().strip());
  }

  @Test
  void unaConcurrenciaInvalidaEsUnErrorDeUsoConCodigoDos() {
    Corrida corrida = correr("--concurrencia", "0");
    assertEquals(2, corrida.codigo());
    assertTrue(corrida.errores().contains("--concurrencia debe ser un entero"), corrida.errores());
    assertEquals("", corrida.salida());
  }

  @Test
  void conTodosArribaElCodigoEsCero() throws Exception {
    Path config = configuracionCon("/bien", "/bien");
    Corrida corrida = correr("--config", config.toString());
    assertEquals(0, corrida.codigo());
    assertTrue(corrida.salida().contains("2 servicios · 2 arriba"), corrida.salida());
  }

  @Test
  void conUnServicioCaidoElCodigoEsUno() throws Exception {
    Path config = configuracionCon("/bien", "/mal");
    Corrida corrida = correr("--config", config.toString());
    assertEquals(1, corrida.codigo());
    assertTrue(corrida.salida().contains("1 caído"), corrida.salida());
  }

  @Test
  void elJsonDeUnReporteConocidoEsElContrato() {
    String esperado =
        """
        {
          "servicios" : [ {
            "nombre" : "catalogo",
            "url" : "http://127.0.0.1:8089/catalogo",
            "estado" : "ARRIBA",
            "tiempo_ms" : 142,
            "detalle" : "200"
          }, {
            "nombre" : "inventario",
            "url" : "http://127.0.0.1:8089/inventario",
            "estado" : "LENTO",
            "tiempo_ms" : 2300,
            "detalle" : "200"
          }, {
            "nombre" : "pagos",
            "url" : "http://127.0.0.1:8089/pagos",
            "estado" : "ARRIBA",
            "tiempo_ms" : 87,
            "detalle" : "200"
          }, {
            "nombre" : "reportes",
            "url" : "http://127.0.0.1:8088/",
            "estado" : "CAIDO",
            "tiempo_ms" : null,
            "detalle" : "sin conexión"
          } ],
          "resumen" : {
            "total" : 4,
            "arriba" : 2,
            "lento" : 1,
            "caido" : 1,
            "promedio_ms" : 843
          }
        }""";
    assertEquals(esperado, Formato.json(reporteConocido()));
  }

  @Test
  void elServidorContestaElReporteComoJson() throws Exception {
    HttpServer propio = Servidor.iniciar(0, RevisorTest::reporteConocido);
    try (HttpClient cliente = HttpClient.newHttpClient()) {
      URI direccion = URI.create("http://127.0.0.1:" + propio.getAddress().getPort() + "/reporte");
      var respuesta =
          cliente.send(HttpRequest.newBuilder(direccion).GET().build(), BodyHandlers.ofString());
      assertEquals(200, respuesta.statusCode());
      assertEquals("application/json", respuesta.headers().firstValue("Content-Type").orElse(""));
      assertEquals(Formato.json(reporteConocido()), respuesta.body());
    } finally {
      propio.stop(0);
    }
  }
}
```

Três coisas para ler com proveito. A classe `Corrida` é um *record* privado que agrupa o que uma execução deixou: o código e os dois textos. `correr(...)` chama `Revisor.ejecutar` com dois streams em memória e os decodifica em UTF-8; não termina nenhuma JVM. E o teste do JSON compara com um bloco de texto escrito à mão **caractere por caractere** (incluídos os espaços ao redor dos dois-pontos, que são os do Jackson): se alguém muda o nome de uma chave, a ordem dos campos ou o formato, esse teste fica vermelho. É isso que significa que o JSON seja um contrato, e não um detalhe de implementação.

Como sempre, antes de confiar no teste, coloque-o em vermelho. Em `Revisor.java`, troque `return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;` por `return CODIGO_TODO_ARRIBA;` (o programa “sempre diz que tudo vai bem”) e execute os testes:

```text
[ERROR] mx.habil.revisor.RevisorTest.conUnServicioCaidoElCodigoEsUno -- Time elapsed: 0.154 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
[ERROR] Tests run: 34, Failures: 1, Errors: 0, Skipped: 0
```

O único que fica vermelho é o do código para um serviço fora do ar, com os números que devia: esperava 1 e recebeu 0. Restaure a linha e volte ao verde: são **34** testes, 28 da lição 9 e 6 de hoje.

#### 10.3.5 Testá-lo à mão

Com os 34 testes em verde, o `mvn -B verify` já deixou também as bibliotecas em `target/lib/` (a cópia da lição 8), então o `revisor` roda com o *classpath* de sempre. Com o laboratório ligado, em um terminal:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor --servir 8090
sirviendo http://127.0.0.1:8090/reporte (Ctrl+C para detener)
```

e em outro, `curl -i http://127.0.0.1:8090/reporte` devolve (recortado):

```text
HTTP/1.1 200 OK
Date: Tue, 13 Oct 2026 21:12:08 GMT
Content-type: application/json
Content-length: 736

{
  "servicios" : [ {
    "nombre" : "catalogo",
    "url" : "http://127.0.0.1:8089/catalogo",
    "estado" : "ARRIBA",
    "tiempo_ms" : 176,
    "detalle" : "200"
  }, {
```

Uma requisição a outro caminho devolve `404`, e um `POST` ao mesmo `/reporte`, `405`. Para comprovar que ele escuta **somente** na interface local, no seu Mint você pode pedir ao `ss` as portas que estão em escuta, filtrando a 8090 (`ss -ltn 'sport = :8090'`): o endereço local deve ser `127.0.0.1:8090`, não `0.0.0.0:8090` nem `*:8090`. Na máquina onde medi, que não é Linux, o equivalente `lsof -nP -iTCP:8090 -sTCP:LISTEN` disse `TCP 127.0.0.1:8090 (LISTEN)`.

E o encerramento. Ao mandar a ele o sinal que `Ctrl`+`C` manda (`SIGINT`), o terminal mostrou:

```text
servidor detenido
```

e o código de saída foi **130**. Com `kill -TERM`, o sinal que os administradores de serviços mandam, também apareceu `servidor detenido` e o código foi **143**. Não são erros do `revisor`: é a convenção do terminal, que, quando um processo morre por um sinal, informa `128 + el número de la señal` (2 para `SIGINT`, 15 para `SIGTERM`), como o mesmo manual do `bash` documenta. Se em um script o seu `revisor` em modo servidor termina com 143, significa que o detiveram, não que falhou. Depois de detê-lo, a porta 8090 fica livre: comprovei com o mesmo comando, que não devolve nenhuma linha. Na segunda sessão este mesmo comando se encurta para `java -jar target/revisor.jar --servir 8090` (10.6).

*(Aqui termina a primeira sessão.)*

### 10.4 Qualidade: Checkstyle e SpotBugs dentro do `verify`

#### 10.4.1 Dois portões distintos

Que um programa funcione não quer dizer que esteja bem feito. Há duas classes de problema que os testes não veem, e que duas ferramentas automáticas veem.

**Checkstyle** revisa a **forma** do código-fonte: que a indentação seja de dois espaços, que as linhas não passem de 100 colunas, que os nomes sigam a convenção, que toda classe e todo método públicos tenham o seu comentário Javadoc. Ele o faz contra uma configuração; a que o curso usa é `google_checks.xml`, que vem dentro do Checkstyle e codifica o [guia de estilo do Google para Java](https://google.github.io/styleguide/javaguide.html). Por isso, desde a lição 1 todo o código do curso leva dois espaços de indentação: para que hoje o portão se abra sem reescrever nada. Um estilo comum não é estética: é o que permite ler o código de outra pessoa sem tropeçar, e que uma diferença em uma revisão seja uma diferença de verdade e não de espaços.

**SpotBugs** revisa o **comportamento provável**: não lê o texto, mas o *bytecode* já compilado, e procura padrões que costumam ser defeitos: um recurso que não é fechado, um `null` que é desreferenciado, uma coleção mutável exposta, uma string de formato incorreta. Cada padrão tem um código em maiúsculas (como `OS_OPEN_STREAM`) que pode ser buscado no seu [catálogo](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html). Não substitui os testes: cobre outra coisa. Um teste diz “este caso sai bem”; uma análise estática diz “este tipo de erro não está em nenhuma parte do código”, inclusive nos caminhos que nenhum teste percorre.

#### 10.4.2 O `pom.xml` da entrega

As duas ferramentas se conectam ao ciclo do Maven com a fase `verify` (a última que você vem usando desde a lição 6), de modo que `mvn -B verify` já não significa “compila e passa nos testes” mas “passa por todos os portões”. Este é o `pom.xml` completo da versão 1.0.0:

```xml fig10_04/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>1.0.0</version>

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
    <resources>
      <resource>
        <directory>src/main/resources</directory>
        <filtering>true</filtering>
      </resource>
    </resources>

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
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.6.2</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <createDependencyReducedPom>false</createDependencyReducedPom>
              <finalName>revisor</finalName>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>mx.habil.revisor.Revisor</mainClass>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>3.6.0</version>
        <dependencies>
          <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>14.3.0</version>
          </dependency>
        </dependencies>
        <configuration>
          <configLocation>google_checks.xml</configLocation>
          <violationSeverity>warning</violationSeverity>
        </configuration>
        <executions>
          <execution>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <groupId>com.github.spotbugs</groupId>
        <artifactId>spotbugs-maven-plugin</artifactId>
        <version>4.10.4.1</version>
        <executions>
          <execution>
            <phase>verify</phase>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

Cinco coisas do `pom.xml` importam, nesta ordem:

1. **A versão é `1.0.0`**, e é o único lugar onde ela se escreve. O bloco `<resources>` com `<filtering>true</filtering>`, que você acrescentou em 10.3.3, faz o Maven substituir `${project.version}` dentro dos arquivos de `src/main/resources`; assim `revisor.properties` acaba dizendo `version=1.0.0` sem que ninguém o escreva duas vezes.
2. **`maven-dependency-plugin` desapareceu.** Já não se copiam as dependências para `target/lib/`; o empacotamento de 10.6 as inclui dentro do JAR.
3. **`maven-checkstyle-plugin` 3.6.0 leva uma dependência própria: Checkstyle 14.3.0.** Não é enfeite. O *plugin* traz por padrão um Checkstyle mais velho que não entende a sintaxe do Java 25, e falha ao ler qualquer arquivo com os padrões das lições 3 e 4. Comprovei isso tirando essa dependência:

   ```text
   [ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: Failed during checkstyle configuration: Exception was thrown while processing .../Clasificador.java: IllegalStateException occurred while parsing file .../Clasificador.java. 15:41: mismatched input 'when' expecting '->': InputMismatchException
   ```

   É uma mensagem que não menciona nenhuma versão, e aponta para uma linha do seu código que está certa: o `when` de um `switch` com padrões. A causa é sempre a mesma, um analisador que é mais velho que a linguagem. Se você vir um erro de “análise” (parsing) em um arquivo que compila, suspeite da versão da ferramenta antes de suspeitar do seu código.
4. **`<violationSeverity>warning</violationSeverity>`.** Os problemas que o Checkstyle encontra com `google_checks.xml` são avisos (*warning*). Por padrão o *plugin* só quebra o build por erros, e então o portão ficaria aberto ainda que houvesse cem infrações. Com `warning`, qualquer infração quebra o `verify`.
5. **`spotbugs-maven-plugin` 4.10.4.1** com o objetivo `check` no `verify`: executa a análise e quebra o build se encontrar algo.

Quando tudo está em ordem, `mvn -B verify` termina assim (corto o que não analisamos):

```text
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] --- shade:3.6.2:shade (default) @ revisor ---
[INFO] --- checkstyle:3.6.0:check (default) @ revisor ---
[INFO] You have 0 Checkstyle violations.
[INFO] --- spotbugs:4.10.4.1:check (default) @ revisor ---
[INFO] BugInstance size is 0
[INFO] BUILD SUCCESS
```

Leia os **números** e não o `BUILD SUCCESS`: 34 testes, 0 infrações, 0 defeitos. Um portão que não rodou também termina em sucesso.

#### 10.4.3 O que encontraram no meu próprio código

Conto isto porque é o mais útil da seção. Na primeira vez que executei o `verify` sobre o `revisor` desta lição, **o Checkstyle marcou cinco infrações de formato** (linhas de mais de 100 colunas e as aspas de fechamento de um bloco de texto, que o guia do Google pede na sua própria linha), que corrigi em minutos. E quando ficou limpo, **o SpotBugs encontrou duas coisas** em código que eu achava certo. Estas foram as suas mensagens, exatamente como saíram:

```text
[ERROR] Medium: mx.habil.revisor.Revisor.version() may fail to close stream [mx.habil.revisor.Revisor] At Revisor.java:[line 118] OS_OPEN_STREAM
[ERROR] Medium: Format string should use %n rather than \n in mx.habil.revisor.Revisor.<static initializer for Revisor>() [mx.habil.revisor.Revisor] At Revisor.java:[line 35] VA_FORMAT_STRING_USES_NEWLINE
```

Lê-se assim: um nível de gravidade (`Medium`), uma descrição, a classe e o método, a linha, e no final o código do padrão. Os dois eram verdade, de maneiras distintas. O primeiro vinha de eu ler as propriedades com `new InputStreamReader(flujo, UTF_8)`: embora o `flujo` estivesse em um `try-with-resources`, o leitor que o envolve era um segundo objeto que o SpotBugs não via fechado. A saída simples foi ler as propriedades com `propiedades.load(flujo)`, sem envoltório (um arquivo de propriedades é lido como ISO-8859-1, e um número de versão é ASCII). O segundo era mais sutil: eu montava o texto de ajuda com um bloco de texto e `.formatted(...)`, e o bloco traz quebras de linha (`\n`) dentro de uma string de formato, onde o Java espera `%n`, a quebra de linha da plataforma. Não era um erro no Linux, mas o SpotBugs tem razão em que é uma fragilidade. Resolvi tirando o formato: o texto leva um marcador `{limite}` e um método o substitui. As duas correções estão no `Revisor.java` que você vê em 10.3.3.

A lição não é que o SpotBugs seja mais esperto que você. É que **a ferramenta encontra o que você não estava procurando**, e que corrigir os seus achados quase sempre deixa o código mais simples, não mais longo.

#### 10.4.4 Ler o que dizem e não os desligar

Quando o Checkstyle quebra o build, a mensagem traz o arquivo, a linha e a coluna, a categoria e o nome da regra. Se você tirar o comentário Javadoc de `main`, isto é o que ele diz com a JVM em espanhol:

```text
[WARNING] src/main/java/mx/habil/revisor/Revisor.java:[38,3] (javadoc) MissingJavadocMethod: Falta el comentario Javadoc para 'main'.
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check (default) on project revisor: You have 1 Checkstyle violation.
```

(O texto sai no idioma da JVM: com `MAVEN_OPTS="-Duser.language=en"` a mesma mensagem diz `Missing a Javadoc comment for 'main'.`. O nome da regra, `MissingJavadocMethod`, é o mesmo em qualquer idioma, e é o que você deve procurar na [lista de regras](https://checkstyle.org/checks.html).) Por isso o `main` do `revisor` tem um Javadoc, e por isso o tem toda classe e método **públicos**; as demais classes são de pacote e não o exigem, que é uma das razões para deixar tudo fechado (lição 6).

Há uma segunda tentação que convém nomear e rejeitar: **desligar a regra**. Todas estas ferramentas oferecem formas de se calar (`@SuppressWarnings`, arquivos de exclusão, baixar a severidade). Use-as apenas quando o achado é falso e você consegue dizer por quê; nunca para que o build passe. Um `verify` verde porque você o calou não vale mais do que um que não rodou.

E um aviso sobre o silêncio do SpotBugs, medido. Na lição 4 você colocou `List.copyOf` no construtor de `Reporte` para que o *record* tivesse a sua própria cópia da lista, e a mesma lição mediu que o SpotBugs se cala sobre ela em uma classe de pacote. Aqui se vê por quê: **depende da visibilidade.** Se eu tiro essa linha de `Reporte`, que é uma classe de pacote (sem `public`), o SpotBugs **não diz nada**: `BugInstance size is 0`. Se eu torno `Reporte` público e volto a tirar a linha, então sim:

```text
[ERROR] Medium: mx.habil.revisor.Reporte.resultados() may expose internal representation by returning Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 6] EI_EXPOSE_REP
[ERROR] Medium: new mx.habil.revisor.Reporte(List, Resumen) may expose internal representation by storing an externally mutable object into Reporte.resultados [mx.habil.revisor.Reporte] At Reporte.java:[line 8] EI_EXPOSE_REP2
```

O SpotBugs se preocupa com o que **se vê de fora do pacote**, e uma classe de pacote não se vê. O que isso ensina é duplo. A cópia defensiva continua correta ainda que a ferramenta não a exija (ela a exigiria no dia em que `Reporte` se tornasse público, e então já seria tarde para pensar nisso). E o silêncio de uma ferramenta é informação sobre o que ela **revisou**, não uma garantia sobre todo o resto.

*(Para saber mais sobre como esses portões se integram em um fluxo de entrega, há dois artigos do blog: [testes que detectam erros](https://www.habil.mx/pt/blog/testes-que-detectam-erros-cobertura-quality-gate/) e [CI/CD e DevSecOps](https://www.habil.mx/pt/blog/cicd-devsecops-infraestrutura-propria/).)*

### 10.5 Revisão de segurança

A segurança do `revisor` não foi acrescentada no final: foi sendo construída onde o risco aparecia. Esta seção junta o que já existe e o revisa contra a superfície de ataque completa, que são quatro coisas: **o que entra** (a configuração e as opções), **para onde sai** (as requisições que ele faz), **o que escuta** (o servidor) e **o que mostra** (o relatório).

**1. O que entra: o endereço de cada serviço é escrito por outra pessoa.** É o risco da lição 8, o das requisições forjadas do lado do servidor (SSRF, de *server-side request forgery*): se o `revisor` consultasse qualquer endereço que apareça em `servicios.json`, quem editar esse arquivo o converteria em um intermediário para chegar a coisas que ele não alcança mas o `revisor` sim (como o serviço de metadados de uma nuvem, que vive em `169.254.169.254`). As defesas são três, todas de uma lista do que é permitido e nenhuma do que é proibido: apenas esquemas `http` e `https`; apenas servidores de uma **lista permitida**; e nenhum redirecionamento seguido. O [guia da OWASP sobre SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) concorda: validar contra o permitido, não contra o conhecido como ruim.

Na lição 8 a lista permitida era uma constante. Hoje é **configurável**, com `--permitir h1,h2`, e isso muda a pergunta: quem controla essa opção? É a de quem **executa** o programa, não a de quem edita o arquivo de serviços. Essa separação é a razão pela qual a lista não vai dentro de `servicios.json`: um arquivo não pode dar permissão a si mesmo. Se algum dia lhe ocorrer ler a lista permitida do mesmo arquivo que valida, ou da rede, você desfez a defesa. Pode-se comprovar com os três arquivos que acompanham a lição: um com `ftp://`, um com `file:///etc/passwd` e um com `http://169.254.169.254/latest/meta-data/`. Os três são rejeitados antes de fazer uma única requisição, com o código 2 (veja o exercício 1).

**2. Para onde sai: os redirecionamentos.** `HttpClient` tem `followRedirects(HttpClient.Redirect.NEVER)` escrito em `Revision.nuevoCliente()`. É o valor padrão, e é escrito igual de propósito: um servidor permitido poderia responder “me mudei para outro endereço” apontando para um que não o é, e seguir esse redirecionamento sem revisá-lo burlaria a lista. O `revisor` trata um `3xx` como mais uma resposta e a classifica; não a segue.

**3. O que escuta: somente a interface local.** O servidor de 10.2 escuta em `127.0.0.1`. Há um risco próprio do design que já foi anunciado: cada `GET /reporte` dispara uma verificação completa, com uma requisição por serviço. Um processo local malintencionado, ou simplesmente descuidado, que peça o relatório em um laço faz o `revisor` consultar todos esses serviços sem descanso. A mitigação imediata é a interface local, que reduz quem pode fazê-lo aos programas da mesma máquina; as seguintes (um cache de alguns segundos, um limite de requisições por segundo) não estão feitas, e esta lição não as simula: fica dito que faltam, e é o primeiro lugar onde caberia continuar trabalhando.

**4. O que mostra: o relatório é público para quem o pedir.** O JSON inclui o campo `url` de cada serviço tal como está no arquivo. Se algum dia um endereço levar credenciais (`http://usuario:clave@...`) ou um `token` no texto da consulta, `/reporte` vai mostrá-las. Hoje o curso não as usa, e por isso não há um filtro; mas é o tipo de decisão que se toma **antes** de o dado existir. Um erro interno, em contrapartida, já está resolvido: devolve `error interno` e nada da exceção.

**5. A serialização nativa, que não se usa.** O Java tem um mecanismo antigo para guardar e recuperar objetos diretamente, `ObjectOutputStream` e `ObjectInputStream`. Ler objetos com `ObjectInputStream` de uma fonte em que você não confia é uma das vulnerabilidades mais graves e mais bem documentadas da plataforma, porque a leitura pode executar código escolhido por quem escreveu os bytes ([a OWASP o explica com exemplos](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html), e o JDK oferece [filtros](https://docs.oracle.com/en/java/javase/25/core/serialization-filtering1.html) desde o [JEP 290](https://openjdk.org/jeps/290) para mitigá-lo). O `revisor` jamais a usa, e a sua segurança aqui não vem de o formato ser JSON (também há bibliotecas de JSON que, mal configuradas, constroem o objeto que o texto lhes pede), mas de três decisões concretas: **não usa a serialização nativa** em lugar nenhum; **não habilita a desserialização polimórfica** do Jackson, a opção que deixa o próprio texto dizer de que classe criar cada objeto; e **valida a configuração campo por campo contra a forma que espera** (a lição 8 a lê como árvore de `JsonNode` e verifica cada valor, sem mapeá-la direto para objetos). A regra do curso é simples: **dados de fora entram como texto estruturado e se validam à mão, nunca como objetos.**

Para continuar no tema, o artigo do blog sobre [segurança de APIs](https://www.habil.mx/pt/blog/seguranca-de-apis-cto-identidade-gateway-tokens/) trata da identidade e dos *tokens*, que é o que faria falta se o servidor saísse da máquina local.

### 10.6 O JAR executável

#### 10.6.1 O problema do *classpath* longo

Desde a lição 8 você executa o `revisor` com `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor`. É uma linha longa que obriga a saber três coisas: onde estão as suas classes, onde estão as bibliotecas, e qual classe tem o `main`. E falha em silêncio de maneiras interessantes. Se faltar a parte das bibliotecas, isto é o que você vê:

```text
$ java -cp target/classes mx.habil.revisor.Revisor --config servicios-arriba.json
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
	at mx.habil.revisor.Opciones.leer(Opciones.java:32)
	at mx.habil.revisor.Revisor.ejecutar(Revisor.java:50)
	at mx.habil.revisor.Revisor.main(Revisor.java:44)
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

Lê-se de baixo para cima: uma classe do Jackson não está no *classpath*, e a JVM só descobre isso justo quando precisa dela. A solução é o que se dá a quem recebe o programa: **um único arquivo** com tudo dentro.

#### 10.6.2 O que é um JAR, e o que faz o `shade`

Um **JAR** (*Java archive*) é um arquivo ZIP com classes dentro e um arquivo especial, `META-INF/MANIFEST.MF`, com metadados. Se o manifesto traz a linha `Main-Class: mx.habil.revisor.Revisor`, `java -jar archivo.jar` sabe qual classe iniciar. É a [especificação do JAR](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html).

O `maven-jar-plugin`, que já roda desde a lição 6, gera o JAR do *projeto*: apenas as suas classes, sem bibliotecas e sem `Main-Class`. Por isso, se você o executar, acontece isto:

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

(Em inglês, com a JVM nesse idioma: `no main manifest attribute, in target/revisor-1.0.0.jar`.) É o erro do “JAR sem entrada”: o seu JAR existe e está certo, mas não diz por onde começar. E tem 32 entradas: as suas classes e pouco mais.

O que faz falta é o [`maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/), que roda depois e produz **outro** JAR que contém as suas classes *e* as de todas as dependências, com um manifesto que traz, sim, `Main-Class`. No `pom.xml` de 10.4 são três linhas de configuração: `finalName` faz o arquivo se chamar `revisor.jar` (sem a versão, para que o comando de uso não mude a cada versão); o `ManifestResourceTransformer` com `mainClass` escreve o `Main-Class`; e `createDependencyReducedPom` em `false` evita que o *plugin* reescreva o `pom.xml` do projeto, um efeito colateral de que não precisamos.

Com ele, `mvn -B verify` deixa em `target/` o JAR completo. Estes são os números que medi:

```text
$ unzip -p target/revisor.jar META-INF/MANIFEST.MF | grep Main
Main-Class: mx.habil.revisor.Revisor
$ unzip -l target/revisor.jar | tail -1
  6359578                     1337 files
$ unzip -p target/revisor.jar revisor.properties
version=1.0.0
```

1,337 entradas e uns 2.6 MB, das quais 1,176 estão sob `tools/jackson`. E a versão sai do `pom.xml`, como foi montado em 10.3.3.

Durante o build, o `shade` imprime avisos que à primeira vista alarmam: `Discovered module-info.class. Shading will break its strong encapsulation.` e `define 2 overlapping resources: META-INF/LICENSE, META-INF/NOTICE`. Os dois são verdadeiros e aqui inofensivos: o Jackson publica uma descrição de módulo que, ao juntar tudo em um JAR, deixa de ser usada (nós não usamos o sistema de módulos, e ele não é ensinado neste curso), e vários JARs trazem o seu próprio arquivo de licença com o mesmo nome, do qual o `shade` conserva um. Quando você não entender um aviso, não o ignore por costume: leia-o, e decida com o detalhe. Este é dos que se podem deixar.

#### 10.6.3 O teste que conta: outra pasta

Um JAR só é independente se você consegue executá-lo **a partir de qualquer pasta**. Teste isto, com o caminho completo do JAR e do arquivo de configuração:

```text
$ cd /tmp
$ java -jar ~/curso-java/revisor/target/revisor.jar --config ~/curso-java/revisor/servicios.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA     128ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 883ms
$ echo $?
1
```

Funciona, e sai com 1 porque há um serviço lento e um fora do ar. Repare também no que acontece **sem** `--config`: `servicios.json` é um caminho relativo, e a partir de `/tmp` ele não existe, então o programa diz isso com o seu erro e o seu código:

```text
$ java -jar ~/curso-java/revisor/target/revisor.jar
error de configuración: no existe el archivo servicios.json
$ echo $?
2
```

É o erro que a lição 5 anunciou, o do caminho relativo que depende de onde você está parado, agora com o seu código de saída. É um código 2 correto: não é um serviço que falhou, é um uso incorreto.

### 10.7 O que vem aí: `jpackage`

Um JAR resolve a entrega do *programa*, não a do ambiente: quem o receber continua precisando de um Java instalado. Para quem não o tem, o JDK traz duas ferramentas que ficam como curiosidade e não são ensinadas: o [`jlink`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jlink.html) monta uma versão reduzida da JVM com apenas os módulos de que o seu programa precisa, e o [`jpackage`](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html) empacota o seu JAR junto com essa JVM em um instalador nativo (um `.deb` para o seu Mint, por exemplo) que não pede nada prévio. São o passo seguinte natural quando você entrega o programa a pessoas que não são programadoras. Outra saída, distinta, é compilar o programa para código nativo, que tem custos e vantagens próprios; o artigo [quando vale a pena compilar Java para nativo](https://www.habil.mx/pt/blog/java-nativo-quando-sim-quando-nao/) as discute.

## O erro que você vai ver

O erro da lição é o do JAR sem entrada, que você acabou de ver em 10.6.2:

```text
$ java -jar target/revisor-1.0.0.jar
no hay ningún atributo de manifiesto principal en target/revisor-1.0.0.jar
```

Significa que o JAR não declara qual classe iniciar: você executou o JAR enxuto do projeto em vez do completo (`revisor.jar`). Resolve-se executando o outro, ou acrescentando a configuração do `shade` ao `pom.xml`. As outras mensagens que convém reconhecer de relance:

- `error de uso: --concurrencia debe ser un entero mayor o igual que 1, no 0` (“erro de uso: --concurrencia deve ser um inteiro maior ou igual a 1, e não 0”) e código 2: uma opção mal escrita; é a saída esperada do `revisor`, não uma falha.
- `error de configuración: no existe el archivo servicios.json` (“erro de configuração: o arquivo servicios.json não existe”) e código 2: você executou a partir de outra pasta sem `--config` (10.6.3).
- `no se pudo abrir el puerto: Address already in use` e código 2: já há algo escutando nessa porta, talvez outro `revisor` que você não desligou. Comprovei isso iniciando dois com `--servir 8090`.
- `NoClassDefFoundError: tools/jackson/core/JacksonException`: falta o Jackson no *classpath*; use o JAR completo (10.6.1).
- `MissingJavadocMethod` do Checkstyle e `EI_EXPOSE_REP` do SpotBugs: os portões de qualidade de 10.4; a mensagem traz a regra e a linha.

## O que se faz errado

**Chamar `System.exit` no meio do programa.** Mata a JVM sem que ninguém possa limpar nada e torna impossível de testar o que o rodeia. Chama-se uma vez, no `main`, com um número calculado por outro código (10.1.2).

**Misturar o resultado com os avisos.** Se o JSON e os erros vão para a mesma saída, quem redirecionar o JSON para um arquivo vai encontrar uma mensagem de erro no meio. O resultado vai para a saída padrão, os avisos para a de erro.

**Um código de saída que não distingue nada.** Terminar sempre com 0 (ou sempre com 1) tira do programa a sua utilidade em um script. Três códigos com significados fixos e documentados valem mais que um arranjo engenhoso.

**Escutar em todas as interfaces por descuido.** `new InetSocketAddress(puerto)` sem endereço expõe o serviço a toda a rede. O endereço se escreve, sempre (10.2.2).

**Um gancho de encerramento longo ou que chama `System.exit`.** Fica esperando por si mesmo. Um gancho faz uma coisa curta e termina (10.3.1).

**Devolver a mensagem de uma exceção pela rede.** É entregar caminhos, classes e às vezes dados. Devolve-se um texto genérico e o detalhe vai para um registro (10.2.3).

**Calar uma regra para que o `verify` passe.** A análise estática vale enquanto se dá ouvidos a ela. Calá-la somente quando o achado é falso e você consegue explicar por quê (10.4.4).

**Não fixar a versão de um *plugin*.** O que não a leva fica à mercê do que o Maven trouxer naquele dia; duas máquinas podem construir coisas distintas. Todos os *plugins* do `pom.xml` levam versão.

**Empacotar sem testar a partir de outra pasta.** Que o JAR rode na pasta do projeto não prova que seja independente (10.6.3).

## Exercícios

### Exercício 1 — As seis verificações do curso

A lista de “Como você sabe que terminou” do temário são seis condições. Verifique-as **uma por uma**, com o laboratório ligado, anotando o comando e o que você viu:

1. `mvn -B verify` termina em verde e o relatório diz quantos testes rodaram.
2. Verifica 20 serviços de uma vez em um tempo próximo ao do mais lento, não ao da soma (use `servicios-20.json` e `--concurrencia`).
3. Um serviço fora do ar, lento ou que responde lixo não derruba o relatório (use `servicios-extra.json`, que acrescenta `facturacion`, `notificaciones` e `basura`).
4. Nenhuma requisição pode esperar para sempre.
5. Rejeita os endereços que não são `http` ou `https` e os que não estão na lista permitida.
6. Executa-se com `java -jar` a partir de outra pasta e sai com 0 se tudo está no ar e com 1 se não.

Antes de executar cada uma, escreva o que você espera ver. Além de `servicios-extra.json` (10.3.3) e `servicios-20.json` (lição 9), você precisa de quatro arquivos pequenos na pasta do projeto. O primeiro tem dois serviços que de fato estão no ar:

```json fig10_04/servicios-arriba.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"}
  ]
}
```

E estes três são endereços que o `revisor` nunca deve consultar: um esquema que não é da web, um arquivo local e o endereço do serviço de metadados de uma nuvem:

```json fig10_04/servicios-ftp.json
{
  "servicios": [
    {"nombre": "archivo", "url": "ftp://127.0.0.1/datos"}
  ]
}
```

```json fig10_04/servicios-archivo.json
{
  "servicios": [
    {"nombre": "secreto", "url": "file:///etc/passwd"}
  ]
}
```

```json fig10_04/servicios-nube.json
{
  "servicios": [
    {"nombre": "metadatos", "url": "http://169.254.169.254/latest/meta-data/"}
  ]
}
```

### Exercício 2 — Que `notificaciones` não transborde a coluna (opcional)

Olhe a tabela de `servicios-extra.json`: o nome `notificaciones` tem 14 caracteres e a coluna SERVICIO mede 12, então o nome fica colado ao estado. Mude `Formato` para que a largura da coluna seja calculada a partir do nome mais longo, com 12 como mínimo, e de modo que a tabela dos quatro serviços canônicos saia **idêntica** à de sempre.

### Exercício 3 — Abra cada portão de propósito

Quebre, um por um, estes três componentes da qualidade, **prevendo antes** o que dirá o build: (a) tire o comentário Javadoc de `main`; (b) tire a dependência do Checkstyle 14.3.0 do `pom.xml`; (c) tire a linha `resultados = List.copyOf(resultados);` de `Reporte`. Em cada caso anote qual ferramenta falou, o que disse, e se você se surpreendeu por ela não falar.

### Exercício 4 — Desligue o servidor de duas maneiras

Suba o `revisor` com `--servir 8090`. Desligue-o primeiro com `Ctrl`+`C` e leia o código de saída; suba-o de novo e desligue-o agora a partir de outro terminal com `kill -TERM`. Explique os dois números e comprove que a porta ficou livre.

## Soluções

### Solução 1

**1. `mvn -B verify` em verde, com a contagem lida do relatório.** O que importa são os números, não o código de saída (por isso se contam duas vezes, com dois métodos):

```text
$ mvn -B verify
...
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[INFO] You have 0 Checkstyle violations.
[INFO] BugInstance size is 0
[INFO] BUILD SUCCESS
$ grep -h -o ' tests="[0-9]*"' target/surefire-reports/TEST-*.xml | awk -F'"' '{s+=$2} END {print s}'
34
$ cat target/surefire-reports/TEST-*.xml | grep -c '<testcase '
34
$ grep -h -o ' skipped="[0-9]*"' target/surefire-reports/TEST-*.xml | sort | uniq -c
   7  skipped="0"
```

Sete classes de teste, 34 testes, nenhum ignorado.

**2. Vinte serviços, em série contra de uma vez.** Os vinte demoram meio segundo cada um. Com `--concurrencia 1` (equivale a fazê-los em série), com 10 e com 20, medindo o programa completo com `time`:

```text
$ TIMEFORMAT='%R s'
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 1 > /dev/null
10.398 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 10 > /dev/null
1.245 s
$ time java -jar target/revisor.jar --config servicios-20.json --concurrencia 20 > /dev/null
0.738 s
```

(Segundas execuções: 10.385, 1.253 e 0.747.) A soma dos tempos é 10 s; com o limite em 20, o programa demora o que o mais lento demora, mais uns 0.2 s de inicialização da JVM. O que o temário pede, “próximo ao do mais lento, não ao da soma”, é cumprido: 0.74 s contra 10.4 s.

**3. O que falha não derruba o relatório.** Com os três serviços extras, a tabela sai completa e na mesma ordem; `facturacion` responde 503, `notificaciones` esgota o seu tempo limite de 1000 ms (a rota do laboratório demora 10 segundos) e `basura` fecha a conexão sem responder:

```text
$ java -jar target/revisor.jar --config servicios-extra.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA     126ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      128ms  503
notificacionesCAIDO          —  tiempo agotado
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 694ms
$ echo $?
1
```

(O nome `notificaciones` transborda a coluna: isso o exercício 2 corrige.) Para ver que a **ordem** não muda nem com concorrência diferente, extraem-se os nomes do JSON com três limites distintos:

```text
$ for c in 1 3 10; do java -jar target/revisor.jar --config servicios-extra.json --concurrencia $c --formato json | grep -o '"nombre" : "[a-z]*"' | cut -d'"' -f4 | tr '\n' ' '; echo; done
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
catalogo inventario pagos reportes facturacion notificaciones basura 
```

(Os tempos da coluna TIEMPO mudam a cada execução, e por isso a média desta tabela não é o 656 que sairia com os números fixos da lição 4: é a dos tempos medidos.)

**4. Nenhuma requisição espera para sempre.** Comprova-se de duas formas. Pelo código: só há um lugar onde o `revisor` manda uma requisição, e esse lugar fixa os dois tempos.

```text
$ grep -n "connectTimeout\|\.timeout(" src/main/java/mx/habil/revisor/Revision.java
31:        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
39:            .timeout(Duration.ofMillis(servicio.timeoutMs()))
$ grep -rn "\.send(" src/main
src/main/java/mx/habil/revisor/Revision.java:44:      var respuesta = cliente.send(peticion, BodyHandlers.discarding());
```

E pelo relógio: a rota `/notificaciones` do laboratório demora 10 segundos para responder, e mesmo assim o relatório completo com os sete serviços terminou em **2.5 segundos** (`time` deu 2.525 s): o limite de 1000 ms do serviço a cortou, e o mais lento dos demais, `inventario`, fixa o resto.

**5. Rejeita o que não é `http` nem está permitido.** Três arquivos, três rejeições, nenhum faz uma única requisição, e os três com código 2:

```text
$ java -jar target/revisor.jar --config servicios-ftp.json
error de configuración: servicio 1: 'url' debe empezar con http:// o https://
$ java -jar target/revisor.jar --config servicios-archivo.json
error de configuración: servicio 1: 'url' debe empezar con http:// o https://
$ java -jar target/revisor.jar --config servicios-nube.json
error de configuración: servicio 1: el servidor 169.254.169.254 no está en la lista permitida
$ echo $?
2
```

**6. A partir de outra pasta, com os dois códigos.** Você já viu o caso do código 1 em 10.6.3. Para o 0 é preciso uma configuração em que tudo esteja no ar (`servicios-arriba.json`, com `catalogo` e `pagos`):

```text
$ cd /tmp
$ java -jar ~/curso-java/revisor/target/revisor.jar --config ~/curso-java/revisor/servicios-arriba.json
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     166ms  200
pagos       ARRIBA     114ms  200

2 servicios · 2 arriba · 0 lentos · 0 caídos · promedio 140ms
$ echo $?
0
```

As seis se cumprem. Anote no seu diário de bordo a data e a contagem de testes do primeiro ponto: é a prova de que você terminou.

### Solução 2

Separa-se o padrão do formato da largura, e `tabla` calcula a largura a partir do nome mais longo (mais dois espaços de separação), com 12 como mínimo. Com os quatro serviços canônicos, o nome mais longo mede 10 e o cálculo dá 12: a tabela não muda. Estas são as partes de `Formato.java` que se modificam (as funções `encabezado()` e `linea(Resultado)` sem largura são substituídas pelas de baixo, que a recebem):

```java
  private static final int ANCHO_NOMBRE_MINIMO = 12;

  private static String patron(int anchoNombre) {
    return "%-" + anchoNombre + "s%-10s%6s  %s";
  }

  static String encabezado(int anchoNombre) {
    return String.format(
        Locale.ROOT, patron(anchoNombre), "SERVICIO", "ESTADO", "TIEMPO", "DETALLE");
  }

  static String linea(Resultado resultado, int anchoNombre) {
    return String.format(
        Locale.ROOT,
        patron(anchoNombre),
        resultado.servicio().nombre(),
        resultado.estado(),
        tiempo(resultado.respuesta()),
        resultado.detalle());
  }

  static String tabla(Reporte reporte) {
    int masLargo =
        reporte.resultados().stream()
            .mapToInt(r -> r.servicio().nombre().length())
            .max()
            .orElse(0);
    int ancho = Math.max(ANCHO_NOMBRE_MINIMO, masLargo + 2);
    String renglones =
        reporte.resultados().stream()
            .map(r -> linea(r, ancho))
            .collect(Collectors.joining("\n"));
    return encabezado(ancho) + "\n" + renglones + "\n\n" + resumen(reporte.resumen());
  }
```

Com essa mudança os 34 testes continuam verdes, o Checkstyle dá 0 infrações e o SpotBugs 0 defeitos, e a tabela de `servicios-extra.json` fica alinhada:

```text
SERVICIO        ESTADO    TIEMPO  DETALLE
catalogo        ARRIBA     175ms  200
inventario      LENTO       2.3s  200
pagos           ARRIBA     122ms  200
reportes        CAIDO          —  sin conexión
facturacion     CAIDO      125ms  503
notificaciones  CAIDO          —  tiempo agotado
basura          CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 690ms
```

### Solução 3

(a) Sem Javadoc em `main`, fala o **Checkstyle**: `MissingJavadocMethod: Falta el comentario Javadoc para 'main'.` (“Falta o comentário Javadoc para 'main'.”), uma infração, e o `verify` falha. É o esperado: `main` é público.

(b) Sem a dependência do Checkstyle 14.3.0, fala o **Checkstyle, mas com um erro de análise e não de estilo**: `mismatched input 'when' expecting '->'` em `Clasificador.java`. Este é o que mais surpreende, porque não menciona a versão e aponta para código correto (10.4.2).

(c) Sem a cópia defensiva, **ninguém fala**: `BugInstance size is 0`. É a surpresa que se anunciava: `Reporte` é uma classe de pacote e o SpotBugs só reclama do que se vê de fora. A cópia deve continuar ali; a ferramenta cala, mas a razão pela qual ela existe não mudou. Se você tornar `Reporte` público (com o seu construtor), aí, sim, aparecem `EI_EXPOSE_REP` e `EI_EXPOSE_REP2` (10.4.4).

### Solução 4

Com `Ctrl`+`C` aparece `servidor detenido` e o código é **130**; com `kill -TERM` (por exemplo, `kill -TERM $(pgrep -f 'revisor.jar --servir')` a partir de outro terminal) também aparece `servidor detenido`, e o código é **143**. Os dois números são `128 + la señal` (2 e 15), a convenção do terminal para os processos que morrem por um sinal; não são falhas. Depois de desligá-lo, no seu Mint `ss -ltn 'sport = :8090'` não deve mostrar nenhuma linha: o encerramento ordenado liberou a porta. (Medi os códigos mandando os sinais a partir de um script; o comando `ss` eu não executei na máquina de medição.)

## Como sei que consegui

- [ ] `java fig10_01.java --falla` imprime o erro de uso e `echo $?` diz 2, e sei dizer por que o erro é escrito com `System.err`.
- [ ] `java fig10_02.java` e `java fig10_03.java` terminam com as saídas da lição, e consigo explicar para que serve cada um dos cinco passos do servidor mínimo e quando um gancho de encerramento roda.
- [ ] **(1)** `mvn -B verify` diz `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`, `You have 0 Checkstyle violations` e `BugInstance size is 0`, e a contagem de testes contada nos XML de `surefire-reports` também dá 34.
- [ ] **(2)** Com `servicios-20.json`, `--concurrencia 1` demora uns 10 s, `--concurrencia 10` uns 1.2 s e `--concurrencia 20` uns 0.7 s (mais ou menos, conforme a minha máquina), e eu mesmo os medi com `time`.
- [ ] **(3)** Com `servicios-extra.json` a tabela sai completa, com os sete nomes na mesma ordem com qualquer `--concurrencia`, e o código de saída é 1.
- [ ] **(4)** Sei apontar em `Revision.java` as duas linhas que fixam o tempo de conexão e o tempo limite, e comprovei que o relatório com `notificaciones` (uma rota de 10 s) termina em uns 2.5 s.
- [ ] **(5)** Os três arquivos `servicios-ftp.json`, `servicios-archivo.json` e `servicios-nube.json` são rejeitados com a mensagem esperada e o código 2.
- [ ] **(6)** `java -jar` a partir de `/tmp` ou da minha pasta pessoal, com os caminhos completos, imprime a tabela e sai com 1; com `servicios-arriba.json`, sai com 0.
- [ ] Testei pelo menos um dos portões de qualidade em vermelho e entendi a mensagem (Solução 3), incluindo o silêncio do SpotBugs diante de `Reporte` sem cópia.
- [ ] Anotei no meu diário de bordo qual achado do Checkstyle ou do SpotBugs eu não esperava.

## Resumo

Responda sem olhar a lição:

1. O que o `main` recebe em `args`, e o que o Java *não* faz com isso?
2. O que diz por convenção um código de saída `0`, e o que o `1` e o `2` do `revisor` distinguem entre si?
3. Por que o `main` é uma única linha e a lógica vive em `ejecutar`? O que os testes ganham com isso?
4. Por que o resultado vai para a saída padrão e os avisos para a de erro?
5. Que diferença há entre escutar em `127.0.0.1` e não colocar endereço? O que significa a porta 0?
6. O que é um gancho de encerramento, quando roda e quando não, e por que não deve chamar `System.exit`?
7. Por que a thread principal do modo servidor fica esperando em um `CountDownLatch` que ninguém abre?
8. O que o Checkstyle revisa e o que o SpotBugs revisa? Por que o silêncio do SpotBugs diante de uma coleção mutável em uma classe de pacote não prova que esteja certo?
9. Por que a lista permitida não vai dentro do arquivo de serviços? Por que não se seguem os redirecionamentos?
10. O que o `maven-shade-plugin` acrescenta que o `maven-jar-plugin` não faz, e como se comprova que o JAR é independente?

## Para ler mais

- [Oracle, `com.sun.net.httpserver.HttpServer` (Java 25)](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html) e [`Runtime.addShutdownHook`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Runtime.html): o servidor do JDK e o ciclo de vida de um desligamento. Em inglês. Consultado em 7 de outubro de 2026.
- [Checkstyle, “Google's Style”](https://checkstyle.org/google_style.html) e [SpotBugs, catálogo de padrões](https://spotbugs.readthedocs.io/en/latest/bugDescriptions.html): o que cada regra verifica e o que significa cada código. Consultado em 7 de outubro de 2026.
- [Apache Maven, `maven-shade-plugin`](https://maven.apache.org/plugins/maven-shade-plugin/) e [a especificação do JAR](https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html): como se monta um JAR com tudo dentro e o que diz o seu manifesto. Consultado em 7 de outubro de 2026.
- [OWASP, “Server-Side Request Forgery Prevention”](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) e [“Deserialization”](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html): os dois riscos da revisão de segurança, com as suas defesas. Consultado em 7 de outubro de 2026.

---

**Anterior:** [Lição 9 — Concorrência: verificar tudo ao mesmo tempo](09-concurrencia.md) ·
**Índice do curso:** [Curso de Java](README.md)
