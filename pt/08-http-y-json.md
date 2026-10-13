# Lição 8 — HTTP e JSON

**Tempo:** 2 × 90 min (duas sessões; o corte fica entre as seções 8.3 e 8.4)

**O que você constrói:** o `revisor` deixa de inventar as suas respostas: consulta cada serviço por HTTP de verdade e lê a sua lista de um arquivo `servicios.json` que valida antes de acreditar nele

**O que você aprende:** o `HttpClient` do JDK (um cliente, tempo de conexão e tempo limite por requisição, redirecionamentos, códigos de status); como converter cada falha de rede em um detalhe curto sem que uma exceção escape; JSON e Jackson 3 (`tools.jackson`), e o que muda em relação a quase tudo o que existe na internet; ler um JSON como árvore e validá-lo à mão; escrever o relatório com *records*; e **SSRF: a URL é escrita por outra pessoa**

**De onde você vem.** Você traz o projeto Maven da [Lição 7](07-pruebas-junit.md): pacotes, um `pom.xml`, 18 testes passando e um `Simulador` que faz o papel da rede com quatro respostas inventadas. Essa invenção é o que acaba hoje. Um revisor que nunca fala com a rede não verifica nada; e uma lista de serviços em texto simples, que não distingue um número de um texto, não aguenta o primeiro arquivo escrito por alguém diferente de você. Esta lição resolve as duas coisas e, de quebra, ensina a você a parte do trabalho mais subestimada: **o que fazer com o que vem de fora**. Você trabalha na sua pasta `~/curso-java/revisor/`.

**O que esta lição não faz.** Não consulta os serviços ao mesmo tempo: `revisarTodos` os verifica um depois do outro (em série), e por isso o relatório demora uns 2.5 segundos, a soma do que cada serviço demora; verificá-los todos ao mesmo tempo é a [Lição 9](09-concurrencia.md). Também não lê o corpo das respostas (ao `revisor` só importa o código e quanto demorou), nem tem ainda uma linha de comando: o relatório em JSON existe e é testado hoje, mas só será pedido com uma opção na [Lição 10](10-el-programa-terminado.md).

**Sobre as execuções.** Os serviços consultados são do **laboratório** (8.1), um servidor local de prática que sempre responde o mesmo; por isso o ESTADO (“estado”) e o DETALLE (“detalhe”) de cada linha são idênticos em cada execução. O que muda entre execuções é a coluna TIEMPO (“tempo”), porque é um tempo medido de verdade: se a sua tabela diz `171ms` onde aqui diz `180ms`, é normal. Tudo foi executado com Temurin 25, Maven 3.10.0, JUnit 6.1.3 e Jackson 3.2.3. Os exemplos que usam Jackson são executados a partir da raiz do projeto, com `java -cp "target/lib/*" fig08_06.java` (8.5 explica por quê), e os programas estão em [`programas/08-http-y-json/`](../programas/08-http-y-json), no repositório do curso.

## Ao terminar, você vai conseguir

- Fazer uma requisição com `HttpClient`, ler o código de status e medir quanto a resposta demorou.
- Explicar a diferença entre o tempo de conexão e o tempo limite de uma requisição, e por que nenhuma requisição é enviada sem tempo limite.
- Converter cada falha de rede (não conecta, tempo esgotado, conexão fechada) em um detalhe curto, sem que uma exceção de rede escape de `revisar`.
- Declarar a dependência do Jackson 3, distinguir `tools.jackson` de `com.fasterxml.jackson`, e explicar o que é o *classpath* e para que serve `target/lib`.
- Ler um JSON como árvore (`JsonNode`) e validá-lo à mão, e explicar por que mapeá-lo direto para um *record* aceita `1.5` e `"1000"` como tempo limite.
- Escrever o relatório como JSON com *records* e chaves em `snake_case`.
- Explicar o que é SSRF e aplicar as defesas do `revisor`: esquema permitido, lista permitida de servidores e redirecionamentos sem seguir.

## O porquê antes do como

Até agora o `revisor` tem sido um ator. Quando dizia que `inventario` demorava 2.3 segundos, dizia isso porque você o escreveu no `Simulador`; não havia ninguém do outro lado. Mudar isso é mais do que mudar de onde sai um número, porque o mundo real tem quatro costumes que o simulador nunca teve. **Demora quando quer:** um serviço pode responder em 80 milissegundos ou em dez segundos. **Nem sempre responde:** a porta pode estar fechada, a conexão pode ser cortada no meio, o servidor pode aceitá-la e ficar calado. **Responde coisas estranhas:** um serviço fora do ar muitas vezes não deixa de responder, responde com um 503. E **às vezes manda para outro lugar:** uma resposta pode dizer “o que você procura não está mais aqui, vá para este outro endereço”. O `revisor` precisa classificar tudo isso, sem cair e sem ficar esperando para sempre.

A segunda metade da lição é outro tipo de problema. Até agora a lista de serviços era um `servicios.txt`: nome, endereço, um tempo limite opcional, separados por espaços. Serve enquanto tudo for texto e tudo for fácil. Mas um arquivo de texto não distingue o número `1000` do texto `"1000"`, não pode aninhar nada e não tem uma forma padrão de dizer “este campo não está”. Para isso existe o **JSON**, o formato de intercâmbio de dados mais usado da internet, e o Java não o traz na sua biblioteca padrão: usa-se uma biblioteca de terceiros, e por isso a Lição 6 existiu.

E entre as duas metades há uma ideia que as une e que é a mais importante da lição: **o seu programa agora faz, com o que diz um arquivo, algo no mundo.** Se o arquivo diz “consulte este endereço”, o `revisor` o consulta. Quem puder escrever nesse arquivo, ou no formulário que algum dia o alimente, controla a que o seu programa se conecta a partir da sua rede. Esse tipo de falha tem nome próprio e está entre as dez mais sérias das aplicações web: chama-se **SSRF**, e a última seção a ataca com quatro defesas.

O caminho tem duas sessões. A **primeira** cobre 8.1 a 8.3: o laboratório, o `HttpClient`, os tempos limite e as falhas, e termina com um `revisor` que consulta de verdade. A **segunda** cobre 8.4 a 8.9: JSON, Jackson 3, validar, escrever e proteger. Com esse destino em mente, isto é o que o `revisor` imprime no final da lição, com o laboratório ligado:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

O ESTADO e o DETALLE são os de sempre. A coluna TIEMPO já não são números escritos por você: é o que o seu computador mediu.

## Os conceitos

### 8.1 O laboratório e a primeira requisição

#### 8.1.1 HTTP em cinco linhas

**HTTP** é a língua com que quase todos os serviços da internet falam. Uma conversa é sempre a mesma: um **cliente** (o seu programa) envia uma **requisição** e um **servidor** envia uma **resposta**. A requisição leva um **método** que diz o que se quer (`GET` é “me dê isto”), um **caminho** que diz o quê (`/pagos`) e alguns cabeçalhos com dados extras. A resposta leva um **código de status**, que é um número de três dígitos com o resultado; alguns cabeçalhos; e, quase sempre, um **corpo** (a página, os dados, o texto “ok”).

Do código de status o `revisor` só precisa do primeiro dígito, que é uma família, segundo a [especificação do HTTP](https://www.rfc-editor.org/rfc/rfc9110): `2xx` é sucesso (`200` é “aqui está”), `3xx` é redirecionamento (“procure em outro lugar”), `4xx` é um erro de quem pergunta (`404` é “isso não existe”), e `5xx` é um erro do servidor (`503` é “agora não posso atender”). A regra de classificação do `revisor` que você já tem, “um código de 400 ou mais deixa o serviço fora do ar”, usa exatamente essa divisão: responder `503` é estar fora do ar mesmo que se responda rápido.

Isto é o que viaja de verdade. Com o `curl`, o programa de terminal que faz uma requisição e mostra a resposta, a opção `-i` deixa ver o código e os cabeçalhos antes do corpo (a data muda a cada execução):

```text
$ curl -si http://127.0.0.1:8089/pagos
HTTP/1.1 200 OK
Date: Thu, 08 Oct 2026 03:42:51 GMT
Content-length: 3

ok
```

Primeira linha: `HTTP/1.1 200 OK`, a versão do protocolo, o código e o seu nome. Depois os cabeçalhos, um por linha, e uma linha em branco; e depois o corpo, que aqui é o texto `ok`. Não há mais nada. As duas coisas que importam ao `revisor` são o `200` e quanto demorou para chegar.

#### 8.1.2 O laboratório

Para praticar com uma rede há dois caminhos, e os dois têm um problema. Consultar serviços públicos da internet torna cada execução diferente (hoje respondem, amanhã não, e os seus tempos dependem da sua conexão), e não é de boa educação praticar contra servidores alheios. Escrever um servidor próprio exige conceitos que você ainda não tem. A saída deste curso é um **laboratório**: um programa pequeno que levanta um servidor HTTP no seu próprio computador (`127.0.0.1`, que é o nome de “este mesmo computador”), com serviços que sempre respondem o mesmo e sempre demoram o mesmo. Assim os resultados podem ser comparados com os desta lição, linha por linha.

Por enquanto o laboratório é uma **caixa-preta**: você vai executá-lo, não escrevê-lo. Ele usa ferramentas que ainda não foram explicadas (um servidor HTTP que vem no próprio JDK e uma thread para cada requisição), e é uma exceção à regra de não usar nada antes de explicá-lo (há outra pequena em 8.3.4); a [Lição 10](10-el-programa-terminado.md) o abre e o usa de verdade. Salve-o como `~/curso-java/Laboratorio.java`, **um nível acima da pasta do projeto**, porque ele não faz parte dele:

```java
// fig08_01/Laboratorio.java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Contesta "ok" con el código dado, después de esperar el tiempo dado.
void responder(HttpExchange intercambio, int codigo, long esperaMs) throws IOException {
  try {
    Thread.sleep(esperaMs);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
  byte[] cuerpo = "ok\n".getBytes(StandardCharsets.UTF_8);
  intercambio.sendResponseHeaders(codigo, cuerpo.length);
  try (var salida = intercambio.getResponseBody()) {
    salida.write(cuerpo);
  }
}

// Atiende /espera/<ms>: contesta 200 después de <ms> milisegundos.
void responderEspera(HttpExchange intercambio) throws IOException {
  String ruta = intercambio.getRequestURI().getPath();
  try {
    responder(intercambio, 200, Long.parseLong(ruta.substring("/espera/".length())));
  } catch (NumberFormatException e) {
    responder(intercambio, 400, 0);
  }
}

void main() throws IOException {
  HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 8089), 128);
  servidor.createContext("/catalogo", i -> responder(i, 200, 142));
  servidor.createContext("/inventario", i -> responder(i, 200, 2300));
  servidor.createContext("/pagos", i -> responder(i, 200, 87));
  servidor.createContext("/facturacion", i -> responder(i, 503, 95));
  servidor.createContext("/notificaciones", i -> responder(i, 200, 10_000));
  servidor.createContext("/espera/", this::responderEspera);
  servidor.createContext("/basura", HttpExchange::close);
  servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
  servidor.start();
  IO.println("laboratorio escuchando en http://127.0.0.1:8089");
}
```

E execute-o **em outro terminal**, que você deixa aberto enquanto pratica. Ele fica rodando, esperando requisições, até você pará-lo com `Ctrl`+`C`:

```text
$ java Laboratorio.java
laboratorio escuchando en http://127.0.0.1:8089
```

Se, em vez disso, você vir `java.net.BindException: Address already in use`, é que outro programa do seu computador já usa a porta 8089 (uma **porta** é o número da “entrada” por onde um servidor escuta). A porta é liberada **parando o processo** que a ocupa, não abrindo outro terminal: quase sempre é um laboratório anterior que continua rodando, e ele é parado com `Ctrl`+`C` no terminal onde você o iniciou. Se você não encontra esse terminal, `ss -ltnp 'sport = :8089'` mostra qual processo escuta na 8089 (com seu nome e seu número de processo), e com esse número você o para. Estas são as suas rotas:

| Caminho | O que responde | E o que o `revisor` terá de concluir |
|---|---|---|
| `/catalogo` | `200` após 142 ms | ARRIBA (“no ar”) |
| `/inventario` | `200` após 2300 ms | LENTO |
| `/pagos` | `200` após 87 ms | ARRIBA |
| `/facturacion` | `503` após 95 ms | CAIDO (“fora do ar”) (`503`) |
| `/notificaciones` | `200`, mas após 10 segundos | CAIDO se receber um tempo limite de 1 s |
| `/basura` | fecha a conexão sem responder | CAIDO (`error de red`, “erro de rede”) |
| `/espera/<ms>` | `200` após `<ms>` milissegundos | conforme o tempo |

Falta um serviço dos quatro de sempre, e ele não está no laboratório de propósito: `reportes` aponta para `http://127.0.0.1:8088/`, onde **ninguém escuta**, para ter um serviço cuja conexão é recusada. Os quatro das tabelas de sempre são `catalogo`, `inventario`, `pagos` e `reportes`; os outros são para exercícios e testes.

#### 8.1.3 A primeira requisição

Com o laboratório ligado, esta é a requisição mais curta que se pode fazer com o Java 25:

```java
// fig08_02.java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

void main() throws Exception {
  try (HttpClient cliente = HttpClient.newHttpClient()) {
    HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8089/pagos")).build();
    long inicio = System.nanoTime();
    HttpResponse<String> respuesta = cliente.send(peticion, BodyHandlers.ofString());
    long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
    IO.println("código de estado: " + respuesta.statusCode());
    IO.println("cuerpo: " + respuesta.body().strip());
    IO.println("tardó al menos 87 ms: " + (tiempoMs >= 87));
  }
}
```

Lê-se de cima para baixo. **`HttpClient`** é o objeto que sabe falar HTTP; está no pacote `java.net.http` do JDK e não precisa de nenhuma dependência. É aberto com `try (...)`, o `try-with-resources` da Lição 5: um `HttpClient` guarda conexões e trabalho interno, e fechá-lo os libera; é fechável (`AutoCloseable`) desde o Java 21, segundo a sua [documentação](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.html). **`HttpRequest.newBuilder(URI.create(...)).build()`** monta a requisição: um objeto que já não muda, com o endereço para onde vai. Se você não disser outra coisa, o método é `GET`. **`cliente.send(peticion, BodyHandlers.ofString())`** a envia e **espera** a resposta; até ela chegar, o programa não avança. O segundo parâmetro diz o que fazer com o corpo: `ofString()` o converte em um texto, e o resultado é um `HttpResponse<String>`, a resposta, da qual se pedem o `statusCode()` e o `body()`.

Para medir quanto demorou, toma-se o relógio antes e depois de `send`, com `System.nanoTime()`, e subtrai-se. Usa-se `nanoTime` e não `currentTimeMillis` porque a [documentação](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/System.html) de `nanoTime` diz sem rodeios: serve para medir **intervalos** de tempo e não tem relação com a hora do dia. A hora de um relógio pode dar um salto (por um ajuste automático, pelo horário de verão) no meio de uma medição, e o intervalo sairia negativo ou enorme. `nanoTime` entrega nanossegundos, e `/ 1_000_000` os transforma em milissegundos; o sublinhado no número é um separador que o Java ignora, para facilitar a leitura.

```text
$ java fig08_02.java
código de estado: 200
cuerpo: ok
tardó al menos 87 ms: true
```

O código `200`, o corpo `ok` que o laboratório envia, e uma verificação: o tempo foi, no mínimo, os 87 milissegundos que `/pagos` se faz esperar. (Imprime-se a verificação e não o número porque o número muda a cada execução.) `throws Exception` no `main` é um atalho deste exemplo, que deixa passar qualquer exceção; no projeto cada uma terá o seu tratamento.

### 8.2 O `HttpClient` do `revisor`

#### 8.2.1 Um cliente, muitas requisições

Um `HttpClient` não é só a função `send`: é um objeto que se monta uma vez e fica com coisas que vale a pena reutilizar. A documentação diz: uma vez construído, **é imutável** e pode ser usado para enviar muitas requisições, e compartilha recursos entre todas as que passam por ele (as conexões já abertas, por exemplo, que não é preciso negociar de novo). Por isso o `revisor` cria **um cliente por verificação** e o usa para todos os serviços, não um por serviço. E ele é criado com o *builder* quando se quer configurar algo que `newHttpClient()` não configura, que é o caso: precisa-se de um tempo de conexão e de uma política de redirecionamentos explícita.

```java
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }
```

Duas configurações, e as duas são decisões, não enfeites. `connectTimeout` fixa quanto se espera para **estabelecer a conexão** (mais em 8.3). `followRedirects(HttpClient.Redirect.NEVER)` diz “se o servidor responder com um redirecionamento, não o siga”; é o valor que o cliente já traz por padrão, e é escrito mesmo assim, **explícito**, porque é uma decisão de segurança (8.8) e uma decisão que alguém escreveu é uma decisão que alguém pode revisar. O valor padrão, segundo a documentação, é `NEVER`; deixá-lo implícito significa que no dia em que alguém o mudar, ninguém vai notar.

#### 8.2.2 O que fazer com o corpo: `BodyHandlers.discarding()`

`send` precisa saber o que fazer com o corpo da resposta, e pergunta isso com um segundo parâmetro, o **manipulador do corpo**. No exemplo anterior usou-se `ofString()`, que o lê completo e o entrega como texto. O `revisor` não precisa do corpo: só do código e do tempo. Para isso existe `BodyHandlers.discarding()`: lê o corpo e o **descarta**, sem guardá-lo. Há uma razão prática por trás: se um serviço responde com um corpo de cem megabytes, `ofString()` os colocaria na memória do seu programa; `discarding()` os lê e não guarda nada. O tipo da resposta muda em consequência: `HttpResponse<Void>`, uma resposta cujo corpo é “nada” (`Void`, com maiúscula, é uma classe do JDK que não tem nenhum valor possível a não ser `null`; usa-se justamente nos genéricos, onde um parâmetro de tipo tem que ser uma classe e não pode ser a palavra `void`, para dizer “aqui não há nada”).

### 8.3 Quando algo falha: tempos limite, exceções e redirecionamentos

#### 8.3.1 Dois tempos limite, e por que nenhuma requisição é enviada sem eles

Esta é a seção que mais separa um programa de prática de um que pode ser deixado rodando. A [documentação de `HttpRequest.Builder`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html) diz sobre o tempo limite de uma requisição: “o efeito de não definir um tempo limite é o mesmo que definir um infinito, ou seja, **esperar para sempre**”. Se o servidor aceita a conexão e fica calado, o seu programa fica esperando, hoje, amanhã e no ano que vem. Um `revisor` que verifica serviços para saber se estão fora do ar e que trava por culpa de um deles é pior do que não ter `revisor`.

Por isso há **dois** tempos limite, e cada um cobre uma etapa diferente:

| Tempo | Onde se configura | O que cobre | O que lança se acabar |
|---|---|---|---|
| **De conexão** | no cliente (`connectTimeout`) | estabelecer a conexão com o servidor | `HttpConnectTimeoutException` |
| **Da requisição** | em cada requisição (`timeout`) | esperar a resposta chegar | `HttpTimeoutException` |

A conexão é a etapa do “alguém responde nesse endereço?”, e o `revisor` lhe dá dois segundos (`TIEMPO_CONEXION_MS = 2000`, a constante de `Revision`). É uma decisão do programa, não uma lei da rede: em uma rede lenta ou congestionada, uma conexão poderia se estabelecer depois. O que a constante diz é “se em dois segundos não há conexão, o `revisor` deixa de esperar e reporta `sin conexión`” (“sem conexão”), o que, para serviços que vivem perto, como os do laboratório, é uma margem ampla. A requisição é a etapa do “já me atenderam, quando me respondem?”, e o seu tempo é **do serviço**: vem de `servicio.timeoutMs()`, que por padrão vale 5000 e que o arquivo de configuração pode mudar para um serviço que se sabe lento. O tempo de conexão de um cliente [só tem efeito quando é preciso abrir uma conexão nova](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/HttpClient.Builder.html); se já há uma aberta de uma requisição anterior, ela é reutilizada e o tempo não se aplica.

#### 8.3.2 Que exceções chegam, e que detalhe cabe a elas

`send` declara duas exceções verificadas: `InterruptedException`, que você já conhece da Lição 5, e `IOException`, que agrupa **tudo o que pode dar errado na rede**. Para ver que tipo concreto chega em cada caso, este programa faz quatro requisições a rotas do laboratório e escreve a classe da exceção e a sua mensagem:

```java
// fig08_03.java
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

void intentar(HttpClient cliente, String url, int timeoutMs) throws InterruptedException {
  HttpRequest peticion =
      HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMillis(timeoutMs)).build();
  try {
    int codigo = cliente.send(peticion, BodyHandlers.discarding()).statusCode();
    IO.println(url + " -> contestó " + codigo);
  } catch (IOException e) {
    IO.println(url + " -> " + e.getClass().getName() + ", mensaje: " + e.getMessage());
  }
}

void main() throws InterruptedException {
  try (HttpClient cliente = HttpClient.newHttpClient()) {
    intentar(cliente, "http://127.0.0.1:8089/facturacion", 5000);
    intentar(cliente, "http://127.0.0.1:8088/", 5000);
    intentar(cliente, "http://127.0.0.1:8089/notificaciones", 1000);
    intentar(cliente, "http://127.0.0.1:8089/basura", 5000);
  }
}
```

```text
$ java fig08_03.java
http://127.0.0.1:8089/facturacion -> contestó 503
http://127.0.0.1:8088/ -> java.net.ConnectException, mensaje: null
http://127.0.0.1:8089/notificaciones -> java.net.http.HttpTimeoutException, mensaje: request timed out
http://127.0.0.1:8089/basura -> java.io.IOException, mensaje: HTTP/1.1 header parser received no bytes
```

Quatro caminhos, quatro resultados diferentes, e uma lição em cada um. `/facturacion` **respondeu, sim**: a resposta é um `503`, e não há exceção: um servidor fora do ar muitas vezes não parece estar, porque responde que não pode. A porta 8088, onde ninguém escuta, deu `ConnectException` (“não foi possível conectar”) e a sua mensagem é **`null`**: a exceção mais comum da rede não traz um texto que sirva. `/notificaciones` faz o cliente esperar 10 segundos e o tempo limite era de 1: `HttpTimeoutException`. E `/basura` abre a conexão e a fecha sem dizer nada: `IOException`, simplesmente, com uma mensagem que fala do interior do cliente (`HTTP/1.1 header parser received no bytes`).

Daí saem duas regras. A primeira, que o `revisor` traduz cada tipo para **um de três detalhes fixos** e nunca mostra a mensagem da exceção, por duas razões: às vezes é `null`, e quando existe muda entre versões do JDK e entre sistemas operacionais. A tabela de tradução é a que você já conhece desde as primeiras lições:

| A exceção | O detalhe | O que aconteceu |
|---|---|---|
| `HttpTimeoutException` (inclui `HttpConnectTimeoutException`) | `tiempo agotado` (“tempo esgotado”) | acabou o tempo, de conexão ou de requisição |
| `ConnectException` | `sin conexión` (“sem conexão”) | porta fechada, ou nome que não existe |
| qualquer outra `IOException` | `error de red` | a conexão foi cortada ou a resposta não foi entendida |

A segunda regra é de **ordem**. `HttpTimeoutException` e `ConnectException` são, as duas, tipos de `IOException`: são casos particulares dela. Quando uma exceção pode ser capturada pelo seu tipo particular ou pelo geral, o Java usa o primeiro `catch` que lhe serve, então **o tipo particular vem antes do geral**. Se você os colocar ao contrário, o compilador não deixa:

```java
// orden/Orden.java
import java.io.IOException;
import java.net.ConnectException;

void probar() {
  try {
    throw new ConnectException();
  } catch (IOException e) {
    IO.println("general");
  } catch (ConnectException e) {
    IO.println("particular");
  }
}

void main() {
  probar();
}
```

```text
$ java Orden.java
Orden.java:9: error: exception ConnectException has already been caught
  } catch (ConnectException e) {
    ^
1 error
error: compilation failed
```

`exception ConnectException has already been caught`: o `catch (IOException)` de cima já captura todas as `ConnectException`, de modo que o de baixo nunca seria executado. Por isso se ordena sempre do particular para o geral. Com esta tabela e esta regra, o método central do `revisor` fica assim:

```java
  static Resultado revisar(HttpClient cliente, Servicio servicio) throws InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(servicio.url())
            .timeout(Duration.ofMillis(servicio.timeoutMs()))
            .GET()
            .build();
    long inicio = System.nanoTime();
    Respuesta respuesta;
    try {
      HttpResponse<Void> http = cliente.send(peticion, BodyHandlers.discarding());
      long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
      respuesta = new Respondio(http.statusCode(), tiempoMs);
    } catch (HttpTimeoutException e) {
      respuesta = new SinRespuesta("tiempo agotado");
    } catch (ConnectException e) {
      respuesta = new SinRespuesta("sin conexión");
    } catch (IOException e) {
      respuesta = new SinRespuesta("error de red");
    }
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }
```

Leia por etapas. Monta-se a requisição, com o tempo limite **do serviço** (`timeout`). Toma-se o relógio. Envia-se; se houve resposta, mede-se o tempo e constrói-se um `Respondio` com o código e os milissegundos (o `/ 1_000_000` converte os nanossegundos). E cada falha se transforma em um `SinRespuesta` com o seu detalhe. No final, o estado não se decide aqui: passa-se a resposta, seja qual for, ao `Clasificador` da Lição 3, que não sabe nada de redes. Duas propriedades importam mais que as outras. A primeira: **`revisar` não deixa escapar nenhuma `IOException`**; uma falha da rede é um resultado, não um erro do programa (a mesma ideia que a Lição 5 chamou de “resultado contra exceção”). A segunda: a `InterruptedException` **sobe, sim**, como se aprendeu, porque quem interrompe a thread tem direito a que o programa pare.

#### 8.3.3 Redirecionamentos: `3xx` é uma resposta

Falta um dos quatro costumes do mundo real. Às vezes um servidor responde com um código `3xx` e um cabeçalho `Location` que diz “agora estou neste outro endereço”. Um navegador o segue sem que você perceba. Um cliente HTTP pode fazer o mesmo se for pedido. Este programa levanta o seu próprio servidor, em uma porta livre que o sistema escolhe, com uma rota que redireciona para outra, e consulta com dois clientes:

```java
// fig08_04.java
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

int consultar(HttpClient cliente, String url) throws IOException, InterruptedException {
  HttpRequest peticion = HttpRequest.newBuilder(URI.create(url)).build();
  return cliente.send(peticion, BodyHandlers.discarding()).statusCode();
}

void main() throws IOException, InterruptedException {
  HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  String base = "http://127.0.0.1:" + servidor.getAddress().getPort();
  servidor.createContext("/viejo", intercambio -> {
    intercambio.getResponseHeaders().add("Location", base + "/nuevo");
    intercambio.sendResponseHeaders(302, -1);
    intercambio.close();
  });
  servidor.createContext("/nuevo", intercambio -> {
    intercambio.sendResponseHeaders(200, -1);
    intercambio.close();
  });
  servidor.start();
  try (HttpClient porOmision = HttpClient.newHttpClient();
      HttpClient siguiendo =
          HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
    IO.println("cliente por omisión:      " + consultar(porOmision, base + "/viejo"));
    IO.println("cliente que las sigue:    " + consultar(siguiendo, base + "/viejo"));
  } finally {
    servidor.stop(0);
  }
}
```

```text
$ java fig08_04.java
cliente por omisión:      302
cliente que las sigue:    200
```

O cliente padrão **não** seguiu o redirecionamento: respondeu `302`, o código da resposta que recebeu. O que foi configurado com `Redirect.NORMAL` foi ao endereço novo e entregou o `200` final. O `revisor` é do primeiro tipo, e a regra de classificação diz isso sem ambiguidade: um `3xx` é uma **resposta**, não se segue, e não é um erro. Um serviço que redireciona está vivo. Há ainda uma razão de segurança mais séria que se explica em 8.8: um endereço que o arquivo permitiu não tem por que levar a um que o arquivo não permitiu.

#### 8.3.4 `revisarTodos` e o teste contra um servidor de verdade

Com `revisar` pronta, `revisarTodos` fica curto: um cliente, um `for`, a ordem da lista.

```java
// fig08_10/src/main/java/mx/habil/revisor/Revision.java

package mx.habil.revisor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;

/** Consulta los servicios por HTTP y arma sus resultados. */
final class Revision {

  /** Cuánto se espera para establecer la conexión, antes de decir que no hay respuesta. */
  static final long TIEMPO_CONEXION_MS = 2000;

  private Revision() {}

  /** Un cliente HTTP con tiempo de conexión y sin seguir redirecciones. */
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  /** Consulta un servicio y mide cuánto tarda; ninguna falla de red sale de este método. */
  static Resultado revisar(HttpClient cliente, Servicio servicio) throws InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(servicio.url())
            .timeout(Duration.ofMillis(servicio.timeoutMs()))
            .GET()
            .build();
    long inicio = System.nanoTime();
    Respuesta respuesta;
    try {
      HttpResponse<Void> http = cliente.send(peticion, BodyHandlers.discarding());
      long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
      respuesta = new Respondio(http.statusCode(), tiempoMs);
    } catch (HttpTimeoutException e) {
      respuesta = new SinRespuesta("tiempo agotado");
    } catch (ConnectException e) {
      respuesta = new SinRespuesta("sin conexión");
    } catch (IOException e) {
      respuesta = new SinRespuesta("error de red");
    }
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  /** Revisa todos, uno tras otro (en serie), y conserva el orden de la lista. */
  static List<Resultado> revisarTodos(List<Servicio> servicios) throws InterruptedException {
    List<Resultado> resultados = new ArrayList<>();
    try (HttpClient cliente = nuevoCliente()) {
      for (Servicio servicio : servicios) {
        resultados.add(revisar(cliente, servicio));
      }
    }
    return resultados;
  }
}
```

É **em série** de propósito: cada serviço é verificado quando o anterior terminou. A razão de haver um `try (HttpClient cliente = nuevoCliente())` em volta é o fechamento: ao terminar o bloco, `close()` espera que não haja requisições em andamento e libera tudo. E o custo da série é visível, e é o tema da próxima lição: 142 + 2300 + 87 milissegundos é, grosso modo, o que o relatório demora.

Falta vigiar `revisar` com testes. Aqui há uma dificuldade que a Lição 7 não tinha: o teste precisa de **um servidor que se comporte como quisermos** (que responda rápido, que falhe, que fique calado), e não se pode depender do laboratório, que pode estar desligado em outro computador. A solução é que o teste levante o seu, com o mesmo `HttpServer` do JDK do laboratório, **dentro do teste**, em uma porta que o sistema escolhe:

```java
// fig08_10/src/test/java/mx/habil/revisor/RevisionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pruebas de la revisión contra un servidor de verdad, levantado dentro de la prueba. */
class RevisionTest {

  private static HttpServer servidor;
  private static String base;

  @BeforeAll
  static void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/rapido", intercambio -> responder(intercambio, 200, 0));
    servidor.createContext("/falla", intercambio -> responder(intercambio, 503, 0));
    servidor.createContext("/lento", intercambio -> responder(intercambio, 200, 2000));
    servidor.createContext("/basura", HttpExchange::close);
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    base = "http://127.0.0.1:" + servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, int codigo, long esperaMs)
      throws IOException {
    try {
      Thread.sleep(esperaMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    intercambio.sendResponseHeaders(codigo, -1);
    intercambio.close();
  }

  private static Resultado revisar(String url, int timeoutMs) throws InterruptedException {
    Servicio servicio = new Servicio("prueba", URI.create(url), timeoutMs);
    try (HttpClient cliente = Revision.nuevoCliente()) {
      return Revision.revisar(cliente, servicio);
    }
  }

  @Test
  void unaRespuestaRapidaEsArriba() throws Exception {
    Resultado resultado = revisar(base + "/rapido", 5000);

    assertEquals(Estado.ARRIBA, resultado.estado());
    assertEquals("200", resultado.detalle());
  }

  @Test
  void unCodigo503EsCaido() throws Exception {
    Resultado resultado = revisar(base + "/falla", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("503", resultado.detalle());
  }

  @Test
  void unPuertoCerradoEsSinConexion() throws Exception {
    int puertoCerrado;
    try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      puertoCerrado = socket.getLocalPort();
    }

    Resultado resultado = revisar("http://127.0.0.1:" + puertoCerrado + "/", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("sin conexión", resultado.detalle());
  }

  @Test
  void unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado() throws Exception {
    Resultado resultado = revisar(base + "/lento", 200);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("tiempo agotado", resultado.detalle());
  }

  @Test
  void unaConexionCerradaSinRespuestaEsErrorDeRed() throws Exception {
    Resultado resultado = revisar(base + "/basura", 5000);

    assertEquals(Estado.CAIDO, resultado.estado());
    assertEquals("error de red", resultado.detalle());
  }
}
```

Três ideias novas. **A porta 0.** Pedir `InetSocketAddress("127.0.0.1", 0)` significa “qualquer porta livre”; o sistema escolhe uma, e o teste a lê depois com `servidor.getAddress().getPort()`. Assim nunca colide com outro programa. **`@BeforeAll` e `@AfterAll`** são anotações do JUnit para código que roda **uma vez** antes de todos os testes da classe e uma vez depois de todos; levantar e desligar o servidor em cada teste seria lento. Os métodos são `static` por uma regra do JUnit. E **cinco testes, um por caminho**: resposta rápida (ARRIBA), um `503` (CAIDO), uma porta fechada (`sin conexión`), uma rota que demora 2 segundos com um tempo limite de 200 milissegundos (`tiempo agotado`), e uma conexão que se fecha sem responder (`error de red`). A porta fechada é obtida de forma engenhosa: abre-se um socket na porta 0, anota-se qual porta lhe coube, e fecha-se; esse número já sabemos que está livre. (O caso LENTO não é testado aqui; o `ClasificadorTest` já o vigia, e esperar um segundo de verdade em um teste o tornaria lento sem ensinar nada novo.) Uma linha do servidor merece uma nota: `setExecutor(Executors.newVirtualThreadPerTaskExecutor())` pede que cada requisição seja atendida na sua própria thread, para que a rota lenta não bloqueie as outras; o conceito é explicado na Lição 9, e por enquanto basta saber que sem essa linha as rotas seriam atendidas uma por uma.

Como sempre, **primeiro se vê em vermelho**. Em `Revision.java`, tire a linha do `.timeout(...)` da requisição e execute `mvn -B verify`:

```text
$ mvn -B verify
...
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0 <<< FAILURE! -- in mx.habil.revisor.RevisionTest
[ERROR] mx.habil.revisor.RevisionTest.unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <CAIDO> but was: <LENTO>
[ERROR]   RevisionTest.unaRespuestaMasLentaQueElTiempoLimiteEsTiempoAgotado:92 expected: <CAIDO> but was: <LENTO>
[ERROR] Tests run: 23, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Sem o tempo limite, a rota `/lento` faz o cliente esperar os seus 2 segundos completos, a resposta chega (um `200`, tardio) e é classificada LENTO, em vez de CAIDO. O teste espera `CAIDO`, e fica vermelho com uma mensagem que diz justamente o que está errado. É a mesma falha que, em produção, não aconteceria em um teste rápido e sim com um serviço de verdade travado, e que se veria como um relatório que nunca termina. Restaure a linha.

#### 8.3.5 O fim da primeira sessão

Você tem tudo para que o `revisor` consulte de verdade. Na sua pasta, mude duas coisas: apague `Simulador.java` (o seu trabalho agora é feito pela rede), e faça os endereços de `servicios.txt` apontarem para o laboratório (`http://127.0.0.1:8089/catalogo`, e assim com os demais; `reportes` vai para `http://127.0.0.1:8088/`). Copie `Revision.java` e `RevisionTest.java` de cima. Com o laboratório ligado em outro terminal:

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

São 23 testes: os 18 da lição anterior mais os cinco novos de `RevisionTest`. Os de `ConfiguracionTest` continuam sendo os do arquivo de texto; na segunda sessão são reescritos. E o `revisor` já consulta de verdade:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     181ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      92ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 861ms
```

Duas coisas para notar. O ESTADO e o DETALLE são idênticos aos da lição anterior, e a coluna TIEMPO já não é a que você escreveu: foi medida pelo seu computador. E o programa demorou, grosso modo, a soma de todos (um pouco mais de 2.5 segundos), não o mais lento: é o custo de verificar em série. Há mais uma prova de que ele consulta de verdade: desligue o laboratório e execute de novo; os quatro serviços saem CAIDO com `sin conexión`, e o tempo médio é um traço, porque ninguém respondeu. Isto encerra a primeira sessão.


### 8.4 JSON: por que o texto simples já não basta

#### 8.4.1 O que um arquivo de texto não pode dizer

O `servicios.txt` da Lição 5 tinha um serviço por linha, com campos separados por espaços, e o terceiro, opcional, era o tempo limite. Para quatro serviços é cômodo. Mas faça-lhe três perguntas. Como ele distingue o número `1000` do texto `"1000"`? Não distingue: são os mesmos quatro caracteres, e o programa os converte porque *sabe* que ali vai um número. Como adiciona um campo que alguns serviços têm e outros não, no meio? Não pode, sem quebrar a posição dos demais. Como diz que um serviço tem uma lista de coisas, como etiquetas ou vários responsáveis? Também não. Cada vez que o arquivo precisa dizer algo novo é preciso inventar uma convenção e reescrever o leitor.

O **JSON** (*JavaScript Object Notation*, embora hoje todas as linguagens o usem) resolve isso com uma ideia simples: um formato de texto que **leva os seus tipos escritos**. Segundo a sua [norma, a RFC 8259](https://www.rfc-editor.org/rfc/rfc8259), um valor JSON é uma destas coisas: um **objeto** (`{"nombre": "pagos"}`, pares chave-valor entre chaves), um **array** (`[1, 2, 3]`, uma lista entre colchetes), um **texto** entre aspas duplas, um **número**, ou um de três valores fixos: `true`, `false` e `null` (“não há valor”). Isso é todo o formato, e daí vem que possa ser lido de qualquer linguagem. E note o que mudou: `1000` (sem aspas) é um número e `"1000"` (com aspas) é um texto, e o formato os distingue desde o início.

Este é o arquivo de configuração do `revisor` a partir de hoje, o `servicios.json`, com os mesmos quatro serviços de sempre apontando agora para o laboratório:

```json fig08_10/servicios.json
{
  "servicios": [
    {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
    {"nombre": "inventario", "url": "http://127.0.0.1:8089/inventario"},
    {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos"},
    {"nombre": "reportes", "url": "http://127.0.0.1:8088/"}
  ]
}
```

É um objeto com uma chave, `servicios`, cujo valor é um array de objetos, e cada objeto leva `nombre` e `url`. Há um terceiro campo possível, `timeout_ms`, o tempo limite da requisição em milissegundos: é **opcional**, e quem o omitir obtém os 5000 milissegundos de sempre. As chaves vão em `snake_case` (palavras em minúscula unidas por sublinhado, como em `timeout_ms`) porque são as mesmas que usa o [curso de Python](https://github.com/HabilMX/curso-python): o arquivo e o relatório ficam iguais nos dois. Com este arquivo, **`servicios.txt` é apagado**: já não se usa.

Por último, o JSON também ocupa um lugar na **saída** do `revisor`: o relatório que o programa entregará para que outro programa o leia (o curso o fixa e ele não muda daqui em diante). Você o verá em 8.7.

### 8.5 Jackson 3: a dependência, os pacotes e o *classpath*

#### 8.5.1 O que é o Jackson, e por que há dois

O Java não traz um leitor de JSON na sua biblioteca padrão; usa-se uma biblioteca, e a mais usada é o **Jackson**. Aqui há uma armadilha que se paga uma vez: o Jackson teve uma versão 2, que domina quase tudo o que existe escrito na internet (respostas antigas, tutoriais, livros), e desde 2025 tem uma **versão 3**, que mudou o nome do seu pacote. A 2 vive em `com.fasterxml.jackson...`; a 3, em **`tools.jackson...`**. Se você copia um exemplo da internet e o seu `import` começa com `com.fasterxml.jackson.databind`, é da versão 2, e com o projeto deste curso não compila. Dá para ver em duas linhas:

```java
// fig08_05.java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

void main() throws Exception {
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
  IO.println(raiz.get("servicios").size());
}
```

```text
$ java -cp "target/lib/*" fig08_05.java
fig08_05.java:2: error: package com.fasterxml.jackson.databind does not exist
import com.fasterxml.jackson.databind.JsonNode;
                                     ^
fig08_05.java:3: error: package com.fasterxml.jackson.databind does not exist
import com.fasterxml.jackson.databind.ObjectMapper;
                                     ^
fig08_05.java:6: error: cannot find symbol
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
  ^
  symbol:   class JsonNode
  location: class fig08_05
fig08_05.java:6: error: cannot find symbol
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
                      ^
  symbol:   class ObjectMapper
  location: class fig08_05
4 errors
error: compilation failed
```

`package com.fasterxml.jackson.databind does not exist`, e atrás uma cascata de `cannot find symbol` que não são mais que a consequência do primeiro. A mensagem é a de sempre quando falta uma dependência (você a viu na Lição 6), mas aqui a dependência do Jackson **está declarada**; o que não existe no seu projeto é a versão 2. Quando um exemplo de Jackson não compilar, revise primeiro qual pacote ele importa. O [guia oficial de migração para o Jackson 3](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) lista as mudanças: o pacote novo, o JDK mínimo (Java 17), os mapeadores imutáveis que se constroem com um *builder*, e que as exceções já não são verificadas. Um único detalhe fica pela metade: as **anotações** (`@JsonProperty` e companhia) continuam em `com.fasterxml.jackson.annotation`, com versão 2.x; essa é a razão de mais abaixo você ver um arquivo `jackson-annotations-2.22.jar` junto aos da versão 3. O curso usa a 3.2.3, a última em outubro de 2026.

#### 8.5.2 Declarar a dependência

Uma **dependência** é uma biblioteca que o seu projeto usa e que você não escreveu. É declarada no `pom.xml` com as suas coordenadas, igual a como você declarou o JUnit, mas com duas diferenças: sem escopo (o escopo padrão é `compile`, “o programa precisa dela para compilar e para rodar”) e com versão escrita. Este é o `pom.xml` do projeto da lição, completo; o novo é a dependência do Jackson e um segundo *plugin*, que se explica logo a seguir:

```xml fig08_10/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.8.0</version>

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
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.11.0</version>
        <executions>
          <execution>
            <id>copiar-dependencias</id>
            <phase>package</phase>
            <goals>
              <goal>copy-dependencies</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.build.directory}/lib</outputDirectory>
              <includeScope>runtime</includeScope>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

A dependência, `tools.jackson.core:jackson-databind`, traz outras por conta própria: o Maven lê o `pom.xml` do Jackson, vê que precisa de `jackson-core` e `jackson-annotations`, e as baixa também. A isso se chama **dependências transitivas**, e é o motivo pelo qual se escreve uma linha e se baixam três JARs. A versão subiu para `0.8.0`, e a do JUnit continua importada do mesmo BOM.

#### 8.5.3 O *classpath* e por que `target/lib`

Compilar com o Maven funciona, porque o Maven sabe quais dependências existem e as passa ao `javac`. Mas **executar** é outra coisa, e é onde aparece o erro mais típico da lição. Com o comando da lição anterior:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError: tools/jackson/core/JacksonException`. A JVM começou a executar o programa, chegou a uma linha que precisa de uma classe do Jackson, e não a encontrou. Lembre o que significava `-cp`: a lista de lugares onde a JVM procura classes, o ***classpath***. Nas Lições 6 e 7 bastava `target/classes`, porque todo o código era seu. Agora é preciso adicionar os JARs do Jackson. O Maven os tem guardados em `~/.m2/repository`, mas esse caminho é longo e depende da versão; para não escrevê-lo, o `pom.xml` adiciona o *plugin* `maven-dependency-plugin`, que na fase `package` **copia as dependências do programa para `target/lib`**:

```text
$ ls target/lib
jackson-annotations-2.22.jar
jackson-core-3.2.3.jar
jackson-databind-3.2.3.jar
```

E executa-se com as duas coisas no *classpath*, `target/classes` (o seu código) e `target/lib/*` (os JARs; o `*` pede “todos os `.jar` dessa pasta”), separados por dois-pontos, que é o separador no Linux:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
```

As aspas são para que o terminal não tente expandir o `*` ele mesmo. E como os JARs são copiados na fase `package`, é preciso ter executado `mvn -B verify` (ou `package`) pelo menos uma vez para que `target/lib` exista. Isto é o que incomoda em um *classpath* manual, e por isso a [Lição 10](10-el-programa-terminado.md) o resolve com um único `.jar` que leva tudo dentro: você já sofreu com isso, então saberá que problema ele resolve.

### 8.6 Ler um JSON como árvore e validar à mão

#### 8.6.1 `JsonNode`: a árvore

O Jackson oferece duas formas de ler um JSON. A primeira o converte em uma **árvore** de objetos `JsonNode`, um por cada valor, que você pode percorrer e consultar. A segunda o mapeia direto para um *record* seu. Comecemos pela primeira, que é a que a configuração usa:

```java
// fig08_06.java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

void main() {
  String texto = """
      {
        "servicios": [
          {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
          {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos", "timeout_ms": 1000}
        ]
      }
      """;
  JsonMapper mapeador = JsonMapper.builder().build();
  JsonNode raiz = mapeador.readTree(texto);

  JsonNode lista = raiz.get("servicios");
  IO.println("¿es lista? " + lista.isArray() + ", elementos: " + lista.size());

  JsonNode segundo = lista.get(1);
  IO.println("nombre: " + segundo.get("nombre").stringValue());
  IO.println("timeout_ms: " + segundo.get("timeout_ms").intValue());

  IO.println("llave que no existe -> " + lista.get(0).get("timeout_ms"));
  IO.println("la misma con path()  -> " + lista.get(0).path("timeout_ms").isMissingNode());
}
```

```text
$ java -cp "target/lib/*" fig08_06.java
¿es lista? true, elementos: 2
nombre: pagos
timeout_ms: 1000
llave que no existe -> null
la misma con path()  -> true
```

Os nomes dos métodos são os do Jackson 3 (e vale a pena prestar atenção, porque quase tudo o que você encontrar na internet usa os da 2): `readTree` converte o texto na árvore e devolve a sua raiz; `get("servicios")` pede o valor de uma chave de um objeto, e `get(1)` o elemento de um array; `isArray()` pergunta que tipo de valor é; `size()` conta os elementos; `stringValue()` entrega o texto de um nó de texto e `intValue()` o número inteiro de um numérico. Os nomes antigos (`asText`, `isTextual`) continuam existindo para quem migra, mas o código novo usa `stringValue` e `isString`. E há um detalhe que decide muitos erros: `get("timeout_ms")` sobre uma chave que **não existe** devolve `null` do Java, não um nó; para perguntar sem risco de `NullPointerException` existe `path("timeout_ms")`, que devolve um nó “ausente” (`isMissingNode()`) em vez de `null`. O que o `revisor` faz com isso é sempre verificar primeiro se é `null` e depois perguntar o tipo.

#### 8.6.2 A armadilha: mapear direto para um *record* aceita o que não deve

A segunda forma é tentadora, porque são duas linhas e os tipos do Java cuidam de tudo: você declara um *record* com os campos que quer e o Jackson o preenche.

```java
// fig08_07.java
import tools.jackson.core.JacksonException;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

record Config(String nombre, int timeoutMs) {}

void probar(JsonMapper mapeador, String valor) {
  String json = "{\"nombre\": \"pagos\", \"timeout_ms\": " + valor + "}";
  try {
    Config config = mapeador.readValue(json, Config.class);
    IO.println("timeout_ms: " + valor + " -> entra como " + config.timeoutMs());
  } catch (JacksonException e) {
    IO.println("timeout_ms: " + valor + " -> rechazado (" + e.getClass().getSimpleName() + ")");
  }
}

void main() {
  JsonMapper mapeador =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
  probar(mapeador, "1000");
  probar(mapeador, "1.5");
  probar(mapeador, "\"1000\"");
  probar(mapeador, "true");
  probar(mapeador, "-5");
}
```

```text
$ java -cp "target/lib/*" fig08_07.java
timeout_ms: 1000 -> entra como 1000
timeout_ms: 1.5 -> entra como 1
timeout_ms: "1000" -> entra como 1000
timeout_ms: true -> rechazado (MismatchedInputException)
timeout_ms: -5 -> entra como -5
```

Olhe as cinco linhas. `1000` entra como 1000, bem. Mas **`1.5` entra como `1`**: o Jackson corta o decimal em silêncio. **`"1000"` entra como `1000`**: converte o texto em número sem avisar. **`-5` entra como `-5`**: um tempo limite negativo, que `HttpRequest` rejeitaria com uma exceção quando o programa o usar, longe de onde foi escrito. Só `true` foi rejeitado, com uma exceção do Jackson (`MismatchedInputException`). O ponto não é que o Jackson esteja errado: é que ele faz o que lhe foi pedido, que era *converter*, e não o que se precisa, que é *verificar*. Um arquivo de configuração com `"timeout_ms": "1000"` quase com certeza é um erro de quem o escreveu, e convém saber disso.

A lição é geral: **o que chega de fora é validado onde entra**, uma vez, com regras explícitas, e depois dessa validação o resto do programa trabalha com tipos que já são de confiança (`Servicio` tem um `int` e um `URI`, não um texto que talvez fosse um número). Por isso a configuração é lida como **árvore** e validada à mão com `isInt()`, `isString()` e `isObject()`, e o mapeamento direto para *records* fica para **escrever** (8.7), onde os dados são controlados pelo programa e não por uma pessoa.

#### 8.6.3 A configuração, validada

A classe `Configuracion` é reescrita para ler JSON. Leia-a como uma cascata: `leer` abre o arquivo e valida a forma geral, `leerServicio` valida um serviço, e cada campo tem o seu pequeno método.

```java
// fig08_10/src/main/java/mx/habil/revisor/Configuracion.java

package mx.habil.revisor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.JsonNode;

/** Lee la lista de servicios de servicios.json y desconfía de todo lo que trae. */
final class Configuracion {

  /** Los únicos servidores a los que el revisor acepta consultar. */
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");

  private Configuracion() {}

  /** Lee y valida el archivo completo; cualquier problema sale como un solo tipo de error. */
  static List<Servicio> leer(Path ruta, Set<String> permitidos) throws ErrorDeConfiguracion {
    String nombreArchivo = String.valueOf(ruta.getFileName());
    JsonNode raiz = leerArbol(ruta, nombreArchivo);
    JsonNode lista = raiz.get("servicios");
    if (lista == null || !lista.isArray()) {
      throw new ErrorDeConfiguracion("falta la lista 'servicios'");
    }
    List<Servicio> servicios = new ArrayList<>();
    for (int i = 0; i < lista.size(); i++) {
      servicios.add(leerServicio(lista.get(i), i + 1, permitidos));
    }
    verificarNombresUnicos(servicios);
    return servicios;
  }

  private static JsonNode leerArbol(Path ruta, String nombreArchivo) throws ErrorDeConfiguracion {
    String texto;
    try {
      texto = Files.readString(ruta, StandardCharsets.UTF_8);
    } catch (NoSuchFileException e) {
      throw new ErrorDeConfiguracion("no existe el archivo " + nombreArchivo, e);
    } catch (IOException e) {
      throw new ErrorDeConfiguracion("no se pudo leer el archivo " + nombreArchivo, e);
    }
    try {
      return Json.MAPEADOR.readTree(texto);
    } catch (JacksonException e) {
      TokenStreamLocation lugar = e.getLocation();
      String donde =
          lugar == null
              ? ""
              : ": renglón %d, columna %d".formatted(lugar.getLineNr(), lugar.getColumnNr());
      throw new ErrorDeConfiguracion(nombreArchivo + " no es JSON válido" + donde, e);
    }
  }

  /** Valida un elemento de la lista y lo convierte en un servicio. */
  static Servicio leerServicio(JsonNode nodo, int posicion, Set<String> permitidos)
      throws ErrorDeConfiguracion {
    String prefijo = "servicio " + posicion + ": ";
    if (!nodo.isObject()) {
      throw new ErrorDeConfiguracion(prefijo + "debe ser un objeto");
    }
    String nombre = leerNombre(nodo, prefijo);
    URI url = leerUrl(nodo, prefijo, permitidos);
    JsonNode timeout = nodo.get("timeout_ms");
    if (timeout == null) {
      return new Servicio(nombre, url);
    }
    if (!timeout.isInt() || timeout.intValue() <= 0) {
      throw new ErrorDeConfiguracion(prefijo + "'timeout_ms' debe ser un entero mayor que 0");
    }
    return new Servicio(nombre, url, timeout.intValue());
  }

  private static String leerNombre(JsonNode nodo, String prefijo) throws ErrorDeConfiguracion {
    JsonNode nombre = nodo.get("nombre");
    if (nombre == null) {
      throw new ErrorDeConfiguracion(prefijo + "falta 'nombre'");
    }
    if (!nombre.isString() || nombre.stringValue().isBlank()) {
      throw new ErrorDeConfiguracion(
          prefijo + "'nombre' debe ser un texto que no esté vacío");
    }
    return nombre.stringValue();
  }

  private static URI leerUrl(JsonNode nodo, String prefijo, Set<String> permitidos)
      throws ErrorDeConfiguracion {
    JsonNode campo = nodo.get("url");
    if (campo == null) {
      throw new ErrorDeConfiguracion(prefijo + "falta 'url'");
    }
    if (!campo.isString()) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    URI url;
    try {
      url = new URI(campo.stringValue());
    } catch (URISyntaxException e) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida", e);
    }
    String esquema = url.getScheme();
    if (!"http".equals(esquema) && !"https".equals(esquema)) {
      throw new ErrorDeConfiguracion(prefijo + "'url' debe empezar con http:// o https://");
    }
    String servidor = url.getHost();
    if (servidor == null) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    if (!permitidos.contains(servidor)) {
      throw new ErrorDeConfiguracion(
          prefijo + "el servidor " + servidor + " no está en la lista permitida");
    }
    return url;
  }

  private static void verificarNombresUnicos(List<Servicio> servicios)
      throws ErrorDeConfiguracion {
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

Três decisões merecem um comentário. **A primeira: cada falha é um `ErrorDeConfiguracion` com uma mensagem que diz o que consertar e onde.** `servicio 2: falta 'nombre'` (“serviço 2: falta 'nombre'”) é uma mensagem que uma pessoa pode usar; um `NullPointerException` na linha 57 não. A posição (`servicio 2`) conta a partir de 1, como uma pessoa conta. **A segunda: os erros do Jackson são traduzidos.** `JacksonException` é uma exceção **não verificada** na versão 3 (uma das diferenças em relação à 2), então o compilador não obriga você a capturá-la, e por isso é fácil esquecê-la e deixar um arquivo mal escrito derrubar o programa com um stack trace. O `catch` a converte em `ErrorDeConfiguracion`, e de passagem aproveita que a exceção sabe em que linha e coluna do arquivo quebrou (`getLocation()`). **A terceira: o arquivo é lido primeiro como texto com `Files.readString`**, com o tratamento de erros da Lição 5 (`NoSuchFileException` vira `no existe el archivo` (“o arquivo não existe”)), e só então se passa o texto ao Jackson. Cada erro tem uma única origem, e cada um é tratado no seu lugar.

Estas são as mensagens que ele produz. A posição conta a partir de 1. Os testes mais abaixo vigiam as mais perigosas; as demais, você as provoca no exercício 3:

| Caso | Mensagem |
|---|---|
| o arquivo não existe | `no existe el archivo servicios.json` |
| JSON mal escrito | `servicios.json no es JSON válido: renglón 4, columna 5` (“servicios.json não é JSON válido: linha 4, coluna 5”) |
| falta a lista | `falta la lista 'servicios'` (“falta a lista 'servicios'”) |
| o serviço não é um objeto | `servicio 2: debe ser un objeto` (“serviço 2: deve ser um objeto”) |
| falta um campo | `servicio 2: falta 'nombre'` |
| nome vazio ou que não é texto | `servicio 2: 'nombre' debe ser un texto que no esté vacío` (“serviço 2: 'nombre' deve ser um texto que não esteja vazio”) |
| url que não é `http`/`https` | `servicio 2: 'url' debe empezar con http:// o https://` (“serviço 2: 'url' deve começar com http:// ou https://”) |
| url sem servidor ou mal escrita | `servicio 2: 'url' no es válida` (“serviço 2: 'url' não é válida”) |
| servidor fora da lista permitida | `servicio 2: el servidor 169.254.169.254 no está en la lista permitida` (“serviço 2: o servidor 169.254.169.254 não está na lista permitida”) |
| tempo limite inválido | `servicio 3: 'timeout_ms' debe ser un entero mayor que 0` (“serviço 3: 'timeout_ms' deve ser um inteiro maior que 0”) |
| nomes repetidos | `nombres repetidos: catalogo` (“nomes repetidos: catalogo”) |

Com um arquivo ao qual falta uma vírgula entre dois serviços (aqui, a do final da linha de `catalogo`), o `revisor` diz exatamente onde:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

`renglón 4, columna 5`: o ponto onde o Jackson esperava uma vírgula e encontrou a `{` que abre o serviço seguinte. Com um arquivo vazio, diz o que falta e não um stack trace; e com uma lista de textos onde deveria haver objetos, diz qual serviço é o que está errado:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

A classe de testes é reescrita com os casos do JSON. É a mesma estrutura da Lição 7, com uma novidade: a tabela de casos agora são arquivos JSON de várias linhas, e uma tabela de `@CsvSource` com isso seria ilegível. O JUnit tem outra fonte de dados, **`@MethodSource`**, que aponta para um método (aqui `archivosInvalidos`) que devolve um `Stream<Arguments>`, uma lista de linhas onde cada `Arguments.of(...)` traz os valores de uma. Os blocos de texto (`"""`) da Lição 2 permitem escrever cada JSON como você o vê em um arquivo.

```java
// fig08_10/src/test/java/mx/habil/revisor/ConfiguracionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Pruebas de la lectura y validación de servicios.json. */
class ConfiguracionTest {

  @TempDir Path carpeta;

  private Path escribir(String json) throws IOException {
    Path ruta = carpeta.resolve("servicios.json");
    Files.writeString(ruta, json);
    return ruta;
  }

  @Test
  void leeUnArchivoValido() throws Exception {
    Path ruta =
        escribir(
            """
            {"servicios": [
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
              {"nombre": "pagos", "url": "http://localhost:8089/pagos", "timeout_ms": 1000}
            ]}
            """);

    List<Servicio> servicios = Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS);

    assertEquals(2, servicios.size());
    assertEquals("catalogo", servicios.get(0).nombre());
    assertEquals(Servicio.TIMEOUT_POR_OMISION_MS, servicios.get(0).timeoutMs());
    assertEquals(1000, servicios.get(1).timeoutMs());
  }

  static Stream<Arguments> archivosInvalidos() {
    return Stream.of(
        Arguments.of(
            """
            {"servicios": [{"nombre": "a", "url": "ftp://127.0.0.1/a"}]}
            """,
            "servicio 1: 'url' debe empezar con http:// o https://"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a"},
              {"nombre": "b", "url": "http://169.254.169.254/latest/meta-data/"}
            ]}
            """,
            "servicio 2: el servidor 169.254.169.254 no está en la lista permitida"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": 1.5}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": "1000"}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [{"url": "http://127.0.0.1:8089/a"}]}
            """,
            "servicio 1: falta 'nombre'"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/a"},
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/b"}
            ]}
            """,
            "nombres repetidos: catalogo"));
  }

  @ParameterizedTest
  @MethodSource("archivosInvalidos")
  void rechazaUnArchivoInvalido(String json, String mensajeEsperado) throws IOException {
    Path ruta = escribir(json);

    ErrorDeConfiguracion error =
        assertThrows(
            ErrorDeConfiguracion.class,
            () -> Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS));

    assertEquals(mensajeEsperado, error.getMessage());
  }
}
```

Sete testes: o caso feliz e as seis rejeições mais perigosas: uma URL que não é `http`, um servidor fora da lista (o `169.254.169.254` de 8.8), os dois tempos limite que o mapeamento direto teria deixado passar (`1.5` e `"1000"`), um serviço sem nome e nomes repetidos. Os dois do tempo limite são a razão de ser da seção anterior: se alguém “simplifica” a validação para um mapeamento direto, essas duas linhas ficam vermelhas.

### 8.7 Escrever o relatório como JSON

Escrever é a outra direção, e aqui sim convém que o Jackson trabalhe por você, porque os dados são produzidos pelo programa e não por uma pessoa. Entrega-se a ele um *record* e ele devolve o texto. Este é o exemplo mínimo, com os nomes que se mostraram antes da validação:

```java
// fig08_08.java
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

record Fila(String nombre, String estado, Long tiempoMs, String detalle) {}

record Resumen(int total, Long promedioMs) {}

record Reporte(List<Fila> servicios, Resumen resumen) {}

void main() {
  JsonMapper mapeador =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
  Reporte reporte =
      new Reporte(
          List.of(
              new Fila("pagos", "ARRIBA", 87L, "200"),
              new Fila("reportes", "CAIDO", null, "sin conexión")),
          new Resumen(2, 87L));
  IO.println(mapeador.writerWithDefaultPrettyPrinter().writeValueAsString(reporte));
}
```

```text
$ java -cp "target/lib/*" fig08_08.java
{
  "servicios" : [ {
    "nombre" : "pagos",
    "estado" : "ARRIBA",
    "tiempo_ms" : 87,
    "detalle" : "200"
  }, {
    "nombre" : "reportes",
    "estado" : "CAIDO",
    "tiempo_ms" : null,
    "detalle" : "sin conexión"
  } ],
  "resumen" : {
    "total" : 2,
    "promedio_ms" : 87
  }
}
```

`writerWithDefaultPrettyPrinter()` produz o texto com indentação; sem ele sai tudo em uma única linha (que é o que uma máquina prefere). A opção `SNAKE_CASE`, que se coloca **uma única vez** ao construir o mapeador, faz com que `tiempoMs` saia como `tiempo_ms`: o *record* é escrito com o estilo do Java (`camelCase`) e o JSON com o do contrato. E olhe o serviço que não respondeu: o seu `tiempo_ms` é `null`, não zero e não ausente. Isso é informação: significa “não foi medido”, e é o campo do tipo `Long` (com maiúscula, o wrapper da Lição 2, que admite `null`) e não `long` que o permite. Os espaços ao redor dos dois-pontos (`"nombre" : "pagos"`) são os do formatador do Jackson, que não são os de outras linguagens; são válidos e qualquer leitor de JSON os aceita.

No `revisor` há três peças. A primeira, `Json`, é **o** mapeador do programa, um só, configurado uma vez (são objetos pensados para serem reutilizados):

```java
// fig08_10/src/main/java/mx/habil/revisor/Json.java

package mx.habil.revisor;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/** El lector y escritor de JSON del revisor: uno solo, configurado una vez. */
final class Json {

  /** Escribe los nombres en snake_case: {@code tiempoMs} sale como {@code tiempo_ms}. */
  static final JsonMapper MAPEADOR =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

  private Json() {}
}
```

A segunda, `ReporteJson`, é a **forma** do JSON, separada de propósito dos tipos internos do programa. `Reporte`, `Resultado` e `Resumen` são as classes com que o programa trabalha; `ReporteJson` é o contrato com quem ler o relatório. Se amanhã você mudar algo interno (como os tempos são guardados, um nome), o JSON não muda a menos que você decida, e não por acidente:

```java
// fig08_10/src/main/java/mx/habil/revisor/ReporteJson.java

package mx.habil.revisor;

import java.util.List;
import java.util.OptionalLong;

/** La forma del reporte en JSON: es el contrato con quien lo consuma. */
record ReporteJson(List<Fila> servicios, ResumenJson resumen) {

  ReporteJson {
    servicios = List.copyOf(servicios);
  }

  /** Un servicio en el JSON; {@code tiempoMs} es nulo si no hubo respuesta. */
  record Fila(String nombre, String url, Estado estado, Long tiempoMs, String detalle) {}

  /** Las cuentas del JSON; {@code promedioMs} es nulo si nadie respondió. */
  record ResumenJson(int total, int arriba, int lento, int caido, Long promedioMs) {}

  /** Convierte el reporte del programa en su forma de JSON. */
  static ReporteJson de(Reporte reporte) {
    List<Fila> filas =
        reporte.resultados().stream()
            .map(
                r ->
                    new Fila(
                        r.servicio().nombre(),
                        r.servicio().url().toString(),
                        r.estado(),
                        nulo(r.tiempoMs()),
                        r.detalle()))
            .toList();
    Resumen resumen = reporte.resumen();
    return new ReporteJson(
        filas,
        new ResumenJson(
            resumen.total(),
            resumen.arriba(),
            resumen.lento(),
            resumen.caido(),
            nulo(resumen.promedioMs())));
  }

  private static Long nulo(OptionalLong valor) {
    return valor.isPresent() ? valor.getAsLong() : null;
  }
}
```

O método `de` converte um `Reporte` na sua forma de JSON, e a conversão de `OptionalLong` para `Long` (`nulo`) é a que produz o `null` dos serviços sem resposta. O construtor compacto com `List.copyOf` é a cópia defensiva da Lição 4; a Lição 10 mostra o que uma ferramenta de análise diz (e o que não diz) sobre ela. A terceira peça é `Formato`, que ganha um método, `json`, e é a única mudança de um arquivo cujo resto você já conhece:

```java
// fig08_10/src/main/java/mx/habil/revisor/Formato.java

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

  /** El reporte como JSON con sangría, listo para que lo lea otro programa. */
  static String json(Reporte reporte) {
    return Json.MAPEADOR
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(ReporteJson.de(reporte));
  }
}
```

O `revisor` ainda não oferece uma opção para pedir esse JSON (chega com a linha de comando da Lição 10), mas você pode vê-lo mudando temporariamente em `Revisor.java` a linha que imprime a tabela por `System.out.println(Formato.json(reporte));`. Assim começa o que ele imprime:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
{
  "servicios" : [ {
    "nombre" : "catalogo",
    "url" : "http://127.0.0.1:8089/catalogo",
    "estado" : "ARRIBA",
```

É o JSON que o curso fixa para o relatório do `revisor`, o mesmo do [curso de Python](https://github.com/HabilMX/curso-python): `servicios` é uma lista com uma entrada por serviço, `tiempo_ms` é `null` para o que não respondeu, e `resumen` repete as contagens. Restaure a linha da tabela antes de seguir.

### 8.8 SSRF: a URL é escrita por outra pessoa

#### 8.8.1 O problema, com um exemplo concreto

Olhe de novo o `servicios.json`. Cada `url` é um endereço ao qual **o seu programa se conecta, a partir da sua rede, com as permissões da sua máquina**. Imagine que hoje o arquivo é escrito por você, mas que amanhã alguém monta uma tela onde outra pessoa adiciona serviços, ou que um arquivo de outra equipe é copiado para esse lugar. Essa pessoa já não escreve um dado: escreve **uma instrução para o seu servidor**. Que endereço um atacante colocaria? Um ao qual ele não consegue chegar de fora mas o seu servidor sim. Na nuvem existe um muito conhecido: `http://169.254.169.254/latest/meta-data/`. É um endereço “de link local”, dos que não saem da rede da própria máquina, que na AWS (e com mecanismos parecidos em outras nuvens) responde com os dados do servidor e, em algumas configurações, com credenciais temporárias; a [documentação da AWS](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html) o descreve. Um `revisor` sem defesas se conectaria, diria `ARRIBA`, e teria feito o que o atacante queria sem que ninguém tocasse no servidor.

A esse tipo de falha se chama **SSRF** (*Server-Side Request Forgery*, “falsificação de requisições a partir do servidor”): conseguir que um servidor faça uma requisição para onde não devia. É a décima posição da [lista OWASP Top 10 de 2021](https://owasp.org/Top10/A10_2021-Server-Side_Request_Forgery_%28SSRF%29/), o catálogo de riscos de aplicações web mais citado, e tem a sua entrada no catálogo de fraquezas de software, [CWE-918](https://cwe.mitre.org/data/definitions/918.html). O `revisor` não lê o corpo das respostas, o que reduz o dano (o atacante saberia que algo respondeu, não o que disse), mas mesmo assim revela o que existe dentro da rede, e um programa que algum dia lesse esse corpo o entregaria.

A defesa não é uma coisa só, é uma pilha de quatro, e cada uma cobre o que a anterior deixa passar. É o que se chama de *defesa em profundidade*.

#### 8.8.2 Primeira defesa: o esquema

Uma URL começa com um **esquema**, a parte antes dos dois-pontos: `http`, `https`, `ftp`, `file`. O `revisor` só fala HTTP, então aceita dois, e o verifica com `URI.getScheme()`, como você viu em `Configuracion`. Os demais esquemas, alguns perigosos (`file:///etc/passwd` aponta para arquivos da própria máquina), são rejeitados. O `HttpClient` já é cauteloso por conta própria, e mostra isso a última linha deste programa, que também ensina como o Java lê um endereço:

```java
// fig08_09.java
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;

void mostrar(String texto) {
  try {
    URI url = new URI(texto);
    IO.println(texto + " -> esquema " + url.getScheme() + ", servidor " + url.getHost());
  } catch (URISyntaxException e) {
    IO.println(texto + " -> no es una URI válida");
  }
}

void main() {
  mostrar("http://127.0.0.1:8089/pagos");
  mostrar("http://LOCALHOST:8089/pagos");
  mostrar("http://127.0.0.1@169.254.169.254/latest/meta-data/");
  mostrar("http://127.0.0.1.otro.example/pagos");
  mostrar("http://[::1]:8089/pagos");
  mostrar("http://127.0.0.1:puerto/pagos");
  mostrar("file:///etc/passwd");
  mostrar("pagos");
  try {
    HttpRequest.newBuilder(URI.create("file:///etc/passwd"));
  } catch (IllegalArgumentException e) {
    IO.println("HttpRequest rechaza file: -> " + e.getMessage());
  }
}
```

```text
$ java fig08_09.java
http://127.0.0.1:8089/pagos -> esquema http, servidor 127.0.0.1
http://LOCALHOST:8089/pagos -> esquema http, servidor LOCALHOST
http://127.0.0.1@169.254.169.254/latest/meta-data/ -> esquema http, servidor 169.254.169.254
http://127.0.0.1.otro.example/pagos -> esquema http, servidor 127.0.0.1.otro.example
http://[::1]:8089/pagos -> esquema http, servidor [::1]
http://127.0.0.1:puerto/pagos -> esquema http, servidor null
file:///etc/passwd -> esquema file, servidor null
pagos -> esquema null, servidor null
HttpRequest rechaza file: -> invalid URI scheme file
```

`file:` não passa nem pelo construtor da requisição (`invalid URI scheme file`). Mas não confie só nisso: o `revisor` rejeita o esquema antes, com uma mensagem clara, na configuração.

#### 8.8.3 Segunda defesa: a lista permitida de servidores

Com o esquema `http` ainda sobra `http://169.254.169.254/`. A segunda defesa é a que mais pesa: uma **lista permitida** de servidores. Em vez de tentar adivinhar quais endereços são perigosos (há demais, e sempre se esquece um), enumeram-se os únicos que se aceitam e rejeita-se todo o resto:

```java
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");
```

Hoje a lista é `127.0.0.1` e `localhost`: a própria máquina, onde vive o laboratório. Na Lição 10 a lista passa a ser configurável pela linha de comando. Verifique a rejeição com um arquivo que aponta para o servidor perigoso:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

Antes de o programa enviar um único pacote, a configuração já se recusou, com uma mensagem que diz qual servidor e por quê. Preste atenção em como se compara: **com `url.getHost()`, o servidor que o analisador de URI extraiu, e por igualdade exata**. Veja o que essa função devolve no programa anterior para endereços travessos: `http://127.0.0.1@169.254.169.254/latest/meta-data/` parece, à primeira vista, um endereço de `127.0.0.1`; mas para o HTTP, o que vem antes de uma arroba é um usuário (o usuário `127.0.0.1`!), e o servidor é `169.254.169.254`. Uma validação feita com `texto.startsWith("http://127.0.0.1")` deixaria passar justamente esse ataque; a feita com `getHost()` o rejeita. E `http://127.0.0.1.otro.example/` começa com o IP mas é outro servidor, e `http://127.0.0.1:puerto/` nem sequer tem servidor reconhecível (o `revisor` o rejeita como “no es válida” (“não é válida”)). A regra: **nunca valide um endereço com operações de texto; analise-o com o analisador de URI e compare as suas partes.**

A lista distingue maiúsculas de minúsculas, e rejeita coisas válidas, como `http://LOCALHOST:8089/` ou a forma IPv6 da própria máquina, `http://[::1]:8089/`:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

Isso é deliberado: uma lista permitida deve ser **estrita por padrão**. Se algum dia for preciso aceitar outra forma, ela é adicionada à lista de propósito, e a comparação não é relaxada.

#### 8.8.4 Terceira defesa: não seguir redirecionamentos

Um servidor que está na lista pode responder com um redirecionamento para um que não está. Se o cliente o seguisse, a lista permitida seria contornada pela porta dos fundos: você autorizou `127.0.0.1`, e o cliente acabaria em `169.254.169.254`. Por isso o cliente do `revisor` usa `Redirect.NEVER` explícito (8.2.1) e trata um `3xx` como mais uma resposta (8.3.3): o que não se segue não pode ser aproveitado contra você. Aviso honesto: **nenhum dos 25 testes vigia esta defesa**. Foi medido: trocar `NEVER` por `ALWAYS` em `Revision.java` deixa os 25 em verde. Quem a vigia é o programa de 8.3.3, que não faz parte do projeto. Escrever esse teste (um servidor de teste que redirecione para outro) é um bom exercício de fechamento.

#### 8.8.5 Quarta defesa, e o que isto NÃO resolve

A quarta é mínima: o tempo limite de cada requisição. Um servidor hostil pode responder muito devagar de propósito para manter o seu programa ocupado; o tempo limite delimita quanto ele pode abusar.

E agora o honesto, porque a segurança que se esconde se torna falsa: **estas quatro defesas são um bom começo, não um muro.** A lista permitida compara *nomes*, não endereços reais. `localhost` é um nome que o seu sistema traduz para um endereço consultando o arquivo `/etc/hosts` ou um servidor de nomes; um servidor de nomes hostil pode responder um endereço diferente do esperado (o ataque se chama *DNS rebinding*). Para um serviço que sai para a internet com um servidor de nomes que você não controla, a defesa completa consiste em resolver o endereço, verificar que ele não pertence a uma faixa interna ou especial, e conectar-se a *esse* endereço e não ao nome, além de isolar a rede do servidor para que o interno não seja alcançável nem se a validação falhar. Essa é a última linha de defesa e é de infraestrutura, não de código. A [cheat sheet da OWASP sobre SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html) descreve as duas camadas e por que nenhuma basta sozinha. O `revisor` deste curso trabalha em um ambiente local que você controla: a sua lista permite `127.0.0.1` e `localhost`, e na sua máquina o `localhost` é traduzido pelo seu arquivo `/etc/hosts` para o mesmo endereço. Ali, a lista de nomes basta; fora de um ambiente assim, aceitar um nome como `localhost` não é uma defesa completa, pelo que se acabou de explicar; o importante da lição é o hábito: **todo endereço que outra pessoa escreveu é suspeito até passar por uma lista do permitido.** Há um artigo da Hábil sobre [segurança de APIs: identidade, gateway e tokens](https://www.habil.mx/pt/blog/seguranca-de-apis-cto-identidade-gateway-tokens/) que segue o tema a partir da arquitetura.

### 8.9 O passo do projeto: o `revisor` consulta de verdade

Todas as peças estão aqui. Este é o projeto como fica ao terminar a lição:

```text
revisor/
  pom.xml                  versión 0.8.0 · agrega Jackson 3.2.3 y copy-dependencies
  servicios.json           NUEVO (reemplaza a servicios.txt)
  src/main/java/mx/habil/revisor/
    Revisor.java           lee servicios.json con la lista permitida
    Json.java              NUEVO: el mapeador de Jackson
    Configuracion.java     REESCRITA para JSON y con validación
    Revision.java          REESCRITA: HttpClient en lugar del simulador
    ReporteJson.java       NUEVO: la forma del JSON
    Formato.java           agrega json()
    Estado.java  Respuesta.java  Servicio.java  Resultado.java  Clasificador.java
    Resumen.java  Reporte.java  ErrorDeConfiguracion.java       (sin cambios)
                           (Simulador.java se borró)
  src/test/java/mx/habil/revisor/
    ConfiguracionTest.java  REESCRITA: 7 pruebas
    RevisionTest.java       NUEVA: 5 pruebas contra un servidor de verdad
    ClasificadorTest.java  FormatoTest.java  ResumenTest.java    (sin cambios)
```

Você já viu completos `pom.xml`, `servicios.json`, `Json`, `Configuracion`, `Revision`, `ReporteJson`, `Formato`, `ConfiguracionTest` e `RevisionTest`. O único que falta é o `Revisor`, que muda duas coisas: lê `servicios.json` e passa a `Configuracion.leer` a lista permitida.

```java
// fig08_10/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.List;

/** El punto de entrada: lee servicios.json, revisa cada servicio e imprime el reporte. */
public final class Revisor {

  private Revisor() {}

  /** Arranca el revisor; los errores de configuración salen por la salida de error. */
  public static void main(String[] args) throws InterruptedException {
    try {
      List<Servicio> servicios =
          Configuracion.leer(Path.of("servicios.json"), Configuracion.SERVIDORES_PERMITIDOS);
      Reporte reporte = Reporte.de(Revision.revisarTodos(servicios));
      System.out.println(Formato.tabla(reporte));
    } catch (ErrorDeConfiguracion e) {
      System.err.println("error de configuración: " + e.getMessage());
    }
  }
}
```

Os onze arquivos que não mudaram em relação à Lição 7 são incluídos para que o projeto possa ser montado sem olhar outra lição:

<details>
<summary>Os onze arquivos que não mudaram</summary>

```java
// fig08_10/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig08_10/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig08_10/src/main/java/mx/habil/revisor/Servicio.java

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
// fig08_10/src/main/java/mx/habil/revisor/Resultado.java

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
// fig08_10/src/main/java/mx/habil/revisor/Clasificador.java

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
// fig08_10/src/main/java/mx/habil/revisor/Resumen.java

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
// fig08_10/src/main/java/mx/habil/revisor/Reporte.java

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
// fig08_10/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/ClasificadorTest.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/FormatoTest.java

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

```java
// fig08_10/src/test/java/mx/habil/revisor/ResumenTest.java

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

</details>

Construa e confira a contagem. Os testes do projeto não precisam do laboratório (`RevisionTest` levanta o seu próprio servidor), mas a execução seguinte sim:

```text
$ mvn -B verify
...
[INFO] Recompiling the module because of changed dependency.
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.RevisionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.RevisionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... s -- in mx.habil.revisor.FormatoTest
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- dependency:3.11.0:copy-dependencies (copiar-dependencias) @ revisor ---
[INFO] BUILD SUCCESS
```

`Tests run: 25`: 18 da Lição 7, menos os 5 de `ConfiguracionTest` que liam texto, mais os 7 novos que leem JSON, mais os 5 de `RevisionTest` (18 − 5 + 7 + 5). Anote no seu diário de bordo. Se não der 25, algo se moveu, e é uma pergunta que vale a pena responder antes de seguir. E a execução, com o *classpath* completo:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     172ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      95ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 857ms
```

A tabela de sempre, agora com tempos medidos, e o resumo com uma média que depende deles.

**O que estes 25 testes não vigiam.** Um número verde não diz o que cobre. Para saber, quebrou-se, de um em um, o código que segue, e mediu-se se algum teste ficava vermelho. Ficaram vermelhos, e por isso estão vigiados: a rejeição de esquemas, de servidores fora da lista, de `timeout_ms` inválido e de nomes repetidos; a tradução de `ConnectException` e de `HttpTimeoutException`; e o tempo limite da requisição. **Continuaram em verde**, ou seja, sem vigilância: a mensagem `'nombre' debe ser un texto que no esté vacío`, a de `falta la lista 'servicios'`, o `connectTimeout` do cliente, a política `Redirect.NEVER` e a conversão para `null` do tempo em `ReporteJson` (esta última é vigiada pelo contrato da Lição 10). Não é um defeito da lição, é a regra: cada teste novo que você adicionar fecha um desses buracos, e os exercícios 3 e 4 começam por aí.


## O erro que você vai ver

A lição tem um erro principal, o da classe do Jackson que não aparece, e várias mensagens de apoio que convém saber ler de relance.

**O erro principal.** Você executa o `revisor` com o comando das Lições 6 e 7, que não inclui os JARs do Jackson:

```text
$ java -cp target/classes mx.habil.revisor.Revisor
Exception in thread "main" java.lang.NoClassDefFoundError: tools/jackson/core/JacksonException
Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException
```

`NoClassDefFoundError` (o nome da classe que falta aparece com barras, `tools/jackson/core/JacksonException`) não é um erro de compilação. O programa **compilou** bem, porque o Maven conhecia a dependência; falha ao executar, porque a JVM não sabe onde procurá-la. A causa que segue (`Caused by: java.lang.ClassNotFoundException: tools.jackson.core.JacksonException`) confirma: essa classe não está em nenhum dos lugares do *classpath*. A solução é executar com `-cp "target/classes:target/lib/*"` (e ter executado antes `mvn -B verify`, que é quem copia os JARs para `target/lib`). Se alguém perguntar a você “compila mas não roda?”, a resposta quase sempre é o *classpath*. E o seu parente próximo, o de 8.5.1, é copiar um exemplo de Jackson 2: `package com.fasterxml.jackson.databind does not exist`. Resolve-se trocando `com.fasterxml.jackson` por `tools.jackson` no `import` e revisando os nomes dos métodos.

**As mensagens de apoio**, para reconhecê-las de relance:

- `java.net.BindException: Address already in use` ao iniciar o laboratório: outro programa usa a porta 8089, ou você deixou um laboratório anterior rodando. Feche-o com `Ctrl`+`C` no terminal dele.
- Todos os serviços com `sin conexión`: o laboratório não está ligado. Ligue-o em outro terminal e execute de novo.
- `exception ConnectException has already been caught`: você colocou um `catch` do tipo geral antes do particular. Do particular para o geral.
- `ConnectException` com mensagem `null` (`fig08_03.java`): normal. Por isso o `revisor` nunca mostra a mensagem de uma exceção de rede.
- `error de configuración: servicios.json no es JSON válido: renglón 4, columna 5` (“erro de configuração: servicios.json não é JSON válido: linha 4, coluna 5”): falta ou sobra algo perto desse ponto; quase sempre é uma vírgula. Olhe a linha anterior à que é indicada.
- `error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida` (“erro de configuração: serviço 2: o servidor 169.254.169.254 não está na lista permitida”): a defesa funcionando. Não é uma falha: é o aviso de que alguém pediu um endereço que o `revisor` não consulta.
- `invalid URI scheme file` (`fig08_09.java`): o `HttpClient` rejeita esquemas que não são HTTP; o `revisor` os rejeita antes, na configuração.

## O que se faz errado

**Enviar uma requisição sem tempo limite.** É o erro mais caro da lição porque não falha nos testes: funciona no dia do exemplo e trava o programa no dia em que um serviço fica calado. Toda requisição leva o seu `timeout`, e o cliente o seu `connectTimeout`.

**Criar um `HttpClient` por requisição, ou não fechá-lo.** Cada cliente guarda conexões e recursos; um por requisição joga fora o que o cliente economiza ao reutilizar, e não fechá-lo os deixa ocupados. Um por verificação, com `try-with-resources`.

**Mostrar `e.getMessage()` de uma exceção de rede.** Às vezes é `null`, às vezes é um texto interno da implementação, e muda entre versões e sistemas. Traduza o **tipo** para um detalhe fixo e deixe a mensagem de fora.

**Capturar `Exception` “por via das dúvidas” em volta da rede.** Engole também os erros de programação (um `NullPointerException` da sua própria lógica) e os disfarça de “erro de rede”. Capturam-se os tipos concretos: `HttpTimeoutException`, `ConnectException`, `IOException`, nessa ordem.

**Confiar em um JSON porque “eu que escrevo”.** O arquivo será editado por outra pessoa, ou por você com pressa. Mapear direto para um *record* deixa passar `1.5` e `"1000"`, e um `-5` chega até uma exceção longe de onde foi escrito. Valida-se na entrada, com mensagens que dizem o que consertar.

**Validar um endereço com operações de texto.** `startsWith("http://127.0.0.1")` deixa passar `http://127.0.0.1@servidor-del-atacante/`. Analisa-se com `URI` e compara-se `getHost()` com uma lista permitida.

**Uma lista do proibido em vez de uma do permitido.** Os endereços perigosos são demais e novos a cada ano; aqueles de que você precisa são poucos e você os conhece. O que não está na lista é rejeitado.

**Seguir os redirecionamentos às cegas.** Um endereço autorizado que leva a um não autorizado anula a lista. `Redirect.NEVER`, escrito.

**Testes que dependem de um serviço que você não controla.** Se `RevisionTest` consultasse o laboratório, falharia em qualquer computador onde ele não estivesse ligado. O teste levanta o seu próprio servidor, com comportamento conhecido, em uma porta livre.

**Medir tempos com a hora do relógio.** `System.currentTimeMillis()` pode dar um salto com um ajuste do relógio; `System.nanoTime()` é a que serve para medir intervalos.

## Exercícios

### Exercício 1 — O endereço que não deve passar, e suas primas

No seu `servicios.json`, mude o segundo serviço para cada um destes endereços, **um de cada vez**, e para cada um **preveja primeiro** o que o `revisor` imprime: (a) `http://169.254.169.254/latest/meta-data/`; (b) `http://localhost:8089/pagos`; (c) `http://[::1]:8089/pagos` (o endereço de “este mesmo computador” na notação IPv6); (d) `http://LOCALHOST:8089/pagos`. Depois execute cada um e compare. Anote no diário de bordo aquele em que você errou a previsão, e por que a lista permitida de 8.8.3 se comporta como se comporta com ele.

### Exercício 2 — Um serviço que nunca responde

Adicione ao seu `servicios.json` três serviços do laboratório: `facturacion` (`/facturacion`), `notificaciones` (`/notificaciones`, com `"timeout_ms": 1000`) e `basura` (`/basura`). **Antes de executar**, preveja o estado e o detalhe de cada um, e quantos no ar, lentos e fora do ar o resumo dirá. Depois execute. Por último, tire de `Revision.java` a linha do `.timeout(...)`, execute de novo e explique o que aconteceu com `notificaciones`, quanto o programa demorou, e o que teria acontecido se o serviço nunca tivesse respondido.

### Exercício 3 — Três arquivos mal escritos

Com o `servicios.json` original, quebre o arquivo de três maneiras, uma por vez, e **preveja a mensagem** antes de executar: (a) tire a vírgula do final da linha do serviço `catalogo`; (b) deixe o arquivo completamente vazio; (c) mude o valor de `servicios` para `["catalogo"]`, uma lista de textos em vez de objetos. Para (a), antes de executar, anote em que linha e coluna você espera que falhe.

### Exercício 4 — Duas linhas que o mapeamento direto teria deixado passar

Adicione dois casos à tabela `archivosInvalidos` de `ConfiguracionTest`: um serviço com `"timeout_ms": -5` e outro com `"timeout_ms": true`. **Preveja** a mensagem de cada um e quantos testes o relatório dirá (dica: cada `Arguments.of` é um teste). Execute `mvn -B verify`. Foi preciso mudar algo em `Configuracion.java` para que passassem?

## Soluções

### Solução 1

(a) O endereço do serviço de metadados é rejeitado antes de enviar qualquer coisa:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor 169.254.169.254 no está en la lista permitida
```

(b) `localhost` está na lista permitida, então `http://localhost:8089/pagos` passa, e o `revisor` consulta o laboratório como se fosse `127.0.0.1`. Repare no que mudou e no que não mudou: o segundo serviço continua se chamando `inventario` (você só mudou o endereço), mas agora pergunta pela rota de `pagos`, que responde rápido, então sai ARRIBA e não LENTO:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     180ms  200
inventario  ARRIBA      92ms  200
pagos       ARRIBA      91ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 3 arriba · 0 lentos · 1 caído · promedio 121ms
```

É uma pequena lição sobre nomes: o nome de um serviço é uma etiqueta que uma pessoa escreve, e o endereço é o que de fato se consulta. O `revisor` não verifica se coincidem.

(c) e (d): **nenhuma das duas passa**, e é deliberado. `[::1]` é o servidor `[::1]` para `getHost()`, e não está na lista; `LOCALHOST` em maiúsculas também não, porque a comparação é exata.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor [::1] no está en la lista permitida
```

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 2: el servidor LOCALHOST no está en la lista permitida
```

Se você esperava que (c) ou (d) passassem, é que pensou no que elas *significam* (o mesmo computador) e não no que *dizem* (outro texto). Uma lista permitida compara textos, e se mantém estrita de propósito: aceitar outra forma é questão de adicioná-la à lista, com a cabeça fria, e não de relaxar a comparação para que “quase coincida”.

### Solução 2

**Previsão.** Os quatro de sempre ficam iguais: `catalogo` e `pagos` ARRIBA, `inventario` LENTO, `reportes` CAIDO com `sin conexión`. Dos três novos, `facturacion` responde `503`, CAIDO com detalhe `503`; `notificaciones` espera mais que o seu tempo limite de um segundo, CAIDO com `tiempo agotado`; `basura` fecha a conexão sem responder, CAIDO com `error de red`. Resumo: 7 serviços, 2 no ar, 1 lento, 4 fora do ar, e a média inclui só os que responderam (`catalogo`, `inventario`, `pagos` e `facturacion`, que respondeu embora com erro): uns 670 milissegundos, porque os 2.3 segundos de `inventario` pesam muito entre quatro.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     179ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      93ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      100ms  503
notificacionesCAIDO          —  tiempo agotado
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 1 lento · 4 caídos · promedio 669ms
```

A média é a soma dos quatro que responderam dividida por quatro. (Os nomes longos transbordam a coluna, um defeito que o formato da lição 2 arrasta e que o exercício opcional da Lição 10 resolve.) Agora, sem o `.timeout(...)`:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     174ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA      91ms  200
reportes    CAIDO          —  sin conexión
facturacion CAIDO      102ms  503
notificacionesLENTO      10.0s  200
basura      CAIDO          —  error de red

7 servicios · 2 arriba · 2 lentos · 3 caídos · promedio 2.5s
```

`notificaciones` já não é `tiempo agotado`: sem tempo limite, o cliente esperou os 10 segundos que o laboratório demora, recebeu o `200` e o classificou LENTO (`10.0s`). O programa completo demorou uns 13 segundos (medido com `time`: 12.9) e a média disparou. Mas o laboratório é amável: responde ao cabo de dez segundos. **Um serviço real que fica calado não responde nunca**, e então `send` não retorna, o programa trava e nenhuma outra verificação se completa. Por isso o tempo limite da requisição nunca é retirado: é a única coisa que delimita o pior caso. Coloque a linha de volta.

### Solução 3

(a) Sem a vírgula, o Jackson chega ao `{` de `inventario` esperando uma vírgula ou o fechamento do array, e falha ali; a mensagem cai na linha 4 (a de `inventario`), coluna 5, que é onde começa essa chave, depois da indentação de quatro espaços. Quem errou deixou o defeito na linha 3, mas o analisador só percebe quando vê o que vem depois.

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicios.json no es JSON válido: renglón 4, columna 5
```

(b) Um arquivo vazio é JSON inválido, mas a árvore que o Jackson entrega para um texto vazio não é um erro: é um nó “ausente”, e o `revisor` o trata como “não tem a lista”:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: falta la lista 'servicios'
```

(c) Uma lista de textos tem a forma de lista, mas cada elemento deveria ser um objeto:

```text
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
error de configuración: servicio 1: debe ser un objeto
```

### Solução 4

As duas linhas são adicionadas ao final da tabela: a última que já existia passa a terminar com vírgula, e depois vão as duas novas, com o mesmo formato das demais:

```java
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": -5}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": true}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"));
```

As duas mensagens são a de sempre, `servicio 1: 'timeout_ms' debe ser un entero mayor que 0`: `-5` é um inteiro mas não é maior que 0, e `true` não é um inteiro (`isInt()` é falso). Sem tocar em `Configuracion.java`, porque a validação já cobria esses dois casos desde o início. Com o mapeamento direto, `-5` teria passado em silêncio (você viu em 8.6.2) e `true` teria sido rejeitado, mas com um erro do Jackson e não com a sua mensagem. A contagem sobe de 25 para 27:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, ... in mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Como sei que consegui

- [ ] `mvn -B verify`, a partir da raiz de `~/curso-java/revisor/`, termina com `BUILD SUCCESS` e diz `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`, e você sabe de onde sai cada número (18 − 5 + 7 + 5).
- [ ] Com o laboratório ligado, `java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor` imprime a tabela de quatro serviços com ESTADO e DETALLE idênticos aos desta lição, e um resumo com `2 arriba · 1 lento · 1 caído`.
- [ ] Sem o laboratório ligado, os quatro serviços saem CAIDO com `sin conexión`.
- [ ] Um serviço apontado para `http://169.254.169.254/latest/meta-data/` faz o `revisor` imprimir `el servidor 169.254.169.254 no está en la lista permitida` sem ter enviado nenhuma requisição.
- [ ] Você tirou o `.timeout(...)` de `Revision.java` e viu o teste de `tiempo agotado` ficar vermelho; você o restaurou.
- [ ] Um `servicios.json` com uma vírgula a menos produz `no es JSON válido: renglón N, columna M`, e você sabe em que linha olhar.
- [ ] Você consegue explicar por que `"timeout_ms": "1000"` entra como 1000 com o mapeamento direto e é rejeitado com a validação à mão.
- [ ] Você anotou no seu diário de bordo qual mensagem desta lição foi mais difícil de ler.

## Resumo

Responda sem olhar a lição:

1. Que dois tempos limite há em uma requisição com `HttpClient`, onde se configura cada um e o que cada um lança ao acabar?
2. Por que `HttpRequest` sem `timeout` é perigoso?
3. A que detalhe do `revisor` corresponde cada uma destas exceções: `HttpTimeoutException`, `ConnectException` e qualquer outra `IOException`? Por que são capturadas nessa ordem?
4. Por que um `503` não é uma exceção? E um `302`, com a política de redirecionamentos do `revisor`?
5. Por que se mede com `System.nanoTime()` e não com `currentTimeMillis()`?
6. Que diferença há entre `tools.jackson` e `com.fasterxml.jackson`? Que anotações não mudaram de pacote?
7. O que é o *classpath* e por que falta `target/lib` em `java -cp target/classes` embora o programa compile?
8. Por que a configuração é lida como árvore e validada à mão em vez de mapeá-la direto para um *record*? Nomeie dois valores que o mapeamento direto deixa passar.
9. O que é SSRF e quais são as quatro defesas do `revisor`? Por que `startsWith("http://127.0.0.1")` não é uma defesa?
10. O que a lista permitida de nomes não resolve? O que mais seria necessário em um serviço que sai para a internet?

## Para ler mais

- [Java SE 25, pacote `java.net.http`](https://docs.oracle.com/en/java/javase/25/docs/api/java.net.http/java/net/http/package-summary.html): o cliente HTTP do JDK completo, com as requisições assíncronas, HTTP/2 e WebSocket que esta lição não usa. Em inglês, como quase toda a documentação oficial. Consultado em 7 de outubro de 2026.
- [RFC 9110, “HTTP Semantics”](https://www.rfc-editor.org/rfc/rfc9110): a norma dos métodos, dos códigos de status e dos redirecionamentos; a seção 15 é a lista de códigos de status. Consultado em 7 de outubro de 2026.
- [Guia de migração para o Jackson 3](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md) e [o repositório do `jackson-databind`](https://github.com/FasterXML/jackson-databind): o que mudou em relação à versão 2 e como se usa a biblioteca. Consultado em 7 de outubro de 2026.
- [OWASP, “Server-Side Request Forgery Prevention Cheat Sheet”](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html): as defesas em camadas contra SSRF e por que uma só não basta. Consultado em 7 de outubro de 2026.

---

**Anterior:** [Lição 7 — Testes com JUnit](07-pruebas-junit.md) ·
**Próxima:** [Lição 9 — Concorrência: verificar tudo ao mesmo tempo](09-concurrencia.md)
