# Lição 9 — Concorrência: verificar tudo ao mesmo tempo

**Tempo:** 2 × 90 min (duas sessões; o corte fica entre as seções 9.4 e 9.5)

**O que você constrói:** o `revisor` concorrente: consulta todos os serviços ao mesmo tempo, com um limite que você define, e entrega o relatório na mesma ordem de sempre

**O que você aprende:** threads de plataforma e threads virtuais; uma thread virtual por tarefa e nunca em um *pool*; `ExecutorService` como recurso que se fecha; `invokeAll`, a ordem estável e `ExecutionException`; `Semaphore` para limitar; uma condição de corrida provocada e medida; `ConcurrentHashMap` e os tipos atômicos; `ScopedValue`; e a concorrência estruturada como “o que vem aí”

**De onde você vem.** Você traz o projeto da [Lição 8](08-http-y-json.md): um `revisor` em Maven, com `HttpClient`, Jackson, a configuração validada em `servicios.json` e 25 testes em verde. Ele faz uma requisição real por serviço, mede quanto demora e não cai quando um serviço não responde. Mas as faz **uma depois da outra**: com os quatro serviços canônicos demora o que somam os seus tempos, uns 2.5 segundos, e com vinte serviços de meio segundo cada um demoraria dez. Essa lentidão não é do programa, é da sua forma: o `for` de `revisarTodos` espera a resposta de `catalogo` antes de perguntar a `inventario`, embora nenhum dos dois precise do outro. Hoje você tira essa lentidão. Você trabalha na sua pasta `~/curso-java/revisor/`.

**O que esta lição não faz.** Não adiciona opções de linha de comando: o limite de concorrência é uma constante até a [Lição 10](10-el-programa-terminado.md), onde vira `--concurrencia`. Também não muda como se verifica um serviço: `revisar` fica como está, com os seus tempos limite e os seus três motivos de queda. A única coisa que muda é **quem chama `revisar` e quantas vezes ao mesmo tempo**.

**Sobre as execuções.** As figuras 9.1 a 9.8 são arquivos compactos e rodam com `java figNN_MM.java` a partir da pasta da lição; não precisam do laboratório. A figura 9.9 é o projeto, e essa sim precisa dele: em outro terminal, a partir de `~/curso-java/`, deixe rodando `java Laboratorio.java` (escuta na porta 8089; a porta 8088 fica vazia de propósito, é a de `reportes`). Todas as saídas foram obtidas com Temurin 25.0.4 em uma máquina de 10 núcleos. **Os tempos que você vê nesta lição dependem da sua máquina**: o que se repete em todas são as proporções, não os milissegundos. Os números que mudam de uma execução para outra (como o resultado de uma corrida) são apresentados como trecho, com uma execução real, e não como saída verificada.

## Ao terminar, você vai conseguir

- Explicar o que é uma thread virtual, em que ela se distingue de uma thread de plataforma e por que um programa que espera muito (como o `revisor`) as aproveita.
- Lançar uma tarefa para cada serviço com `Executors.newVirtualThreadPerTaskExecutor()` dentro de um `try-with-resources`, e dizer por que não se usa um *pool* de threads virtuais.
- Usar `invokeAll` para esperar todas as tarefas e recuperar os seus resultados na ordem de entrada, e ler uma `ExecutionException` até a sua causa.
- Limitar quantas tarefas avançam ao mesmo tempo com um `Semaphore`, liberando-o sempre em um `finally`.
- Provocar uma condição de corrida, medir quanto se perde e corrigi-la com `ConcurrentHashMap.merge` ou com um contador atômico.
- Explicar por que o `revisor` não tem corridas (não compartilha estado mutável), e para que servem `ScopedValue` e a concorrência estruturada.
- Medir com o relógio o efeito do limite sobre vinte serviços e explicar por que um limite de 10 demora o dobro de um de 20.

## O porquê antes do como

Imagine uma cozinha com um único cozinheiro que prepara quatro pratos. Ele coloca o primeiro no forno e fica olhando para ele por vinte minutos até ficar pronto; só então começa o segundo. Ninguém diria que esse cozinheiro é lento com as mãos. Ele é lento **porque espera sem fazer mais nada**. O `revisor` da lição 8 é esse cozinheiro: quando pergunta a `inventario`, que demora 2.3 segundos para responder, o programa fica parado 2.3 segundos, com o processador quase dormindo, enquanto `pagos` e `catalogo` poderiam estar respondendo ao mesmo tempo.

A solução também é de cozinha: colocar vários pratos no forno ao mesmo tempo. Em programação isso se chama **concorrência**: várias tarefas em andamento ao mesmo tempo, de modo que o tempo que uma passa esperando seja aproveitado por outra. (Não é o mesmo que o **paralelismo**, que é ter vários processadores trabalhando literalmente ao mesmo tempo. A concorrência é uma forma de organizar o programa; o paralelismo, algo que o equipamento pode ou não oferecer. Nesta lição você vai ver as duas, mas o benefício do `revisor` vem sobretudo da primeira.)

Fazer isso em Java sempre teve um obstáculo, e esta lição existe pelo modo como o Java o removeu. Durante mais de vinte anos, a unidade de concorrência foi a **thread do sistema operacional**: uma thread é uma linha de execução independente dentro do programa, e cada thread de Java era uma thread do sistema, com a sua memória própria e um custo real. Criar dez mil delas para dez mil requisições não era viável, então a prática padrão foi ter algumas poucas threads “de plantão” (um *pool*) e revezar as tarefas, com filas, limites e muito código para que ninguém ficasse esperando além do necessário. Desde o Java 21, a plataforma oferece outra coisa: **threads virtuais**, tão baratas que cada tarefa pode ter a sua, e com elas o código volta a ser o que era na cozinha de um só cozinheiro —uma linha atrás da outra, cada passo esperando o anterior—, só que agora são muitos cozinheiros baratos.

Esta lição tem duas sessões. A **primeira** (9.1 a 9.4) ensina as threads virtuais, `invokeAll` e o semáforo, e termina com uma medição: o `revisor` verificando vinte serviços ao mesmo tempo. A **segunda** (9.5 a 9.8) ensina o que é preciso saber para que a concorrência não traia você: o estado compartilhado, a condição de corrida que o torna perigoso, os valores com escopo e o que vem aí na plataforma. É a lição que mais muda o que você sabe de Java, e a que mais vai ser útil para você: qualquer servidor moderno em Java se escreve com estas ideias.

## Os conceitos

### 9.1 Duas classes de thread

#### 9.1.1 O que é uma thread e quanto custa

Uma **thread** (*thread*) é uma sequência de instruções que avança por conta própria. O seu programa sempre tem pelo menos uma: a que executa `main`. Com mais threads, várias sequências avançam “ao mesmo tempo”: o sistema operacional reparte o tempo dos processadores entre elas, e quando uma se bloqueia —esperando uma resposta da rede, por exemplo— dá a vez a outra.

Até o Java 20 cada `Thread` era exatamente isso: **uma thread do sistema operacional**, com a sua própria pilha de chamadas e o seu próprio registro nas tabelas do sistema. A essas se chama agora **threads de plataforma** (*platform threads*). São poderosas e caras: o sistema impõe um teto ao número de threads, cada uma reserva memória para a sua pilha, e trocar de uma para outra custa. Por isso um programa de servidor não podia se dar ao luxo de ter uma thread por requisição quando havia milhares de requisições, e a técnica foi compartilhá-las com um *pool*.

Uma **thread virtual** é outra coisa, embora se use do mesmo jeito. É também uma `Thread` —o mesmo tipo, os mesmos métodos, `join`, `interrupt`—, mas quem a administra é a máquina virtual de Java, em vez do sistema operacional. Uma thread virtual não está presa a uma thread do sistema: a JVM tem um grupo pequeno de threads de plataforma, chamadas **threads portadoras** (*carrier threads*), e “monta” sobre elas as threads virtuais que têm algo a fazer. Quando uma thread virtual chega a uma operação que bloqueia —ler de um socket, dormir, esperar um semáforo—, a JVM a **desmonta**: guarda o seu estado na memória do heap, deixa a portadora livre e monta outra thread virtual que pode avançar. Quando a resposta chega, a thread virtual volta a se montar em alguma portadora. É a mesma ideia do cozinheiro que não fica olhando o forno, feita pela plataforma em vez de por você. A [documentação oficial das threads virtuais](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html) a descreve com estes termos, e a especificação da mudança é o [JEP 444](https://openjdk.org/jeps/444), final desde o Java 21.

A figura 9.1 as coloca lado a lado. Ela pede uma thread de plataforma e uma virtual com o mesmo construtor de threads (`Thread.ofPlatform()` e `Thread.ofVirtual()`), dá um nome a elas, as inicia, e cada uma diz o que é:

```java
// fig09_01.java
void main() throws InterruptedException {
  Thread plataforma =
      Thread.ofPlatform()
          .name("de-plataforma")
          .start(() -> describir("de-plataforma"));
  plataforma.join();

  Thread virtual =
      Thread.ofVirtual()
          .name("virtual-1")
          .start(() -> describir("virtual-1"));
  virtual.join();
}

void describir(String nombre) {
  Thread hilo = Thread.currentThread();
  IO.println(nombre + ": virtual=" + hilo.isVirtual() + ", demonio=" + hilo.isDaemon());
}
```

As figuras desta lição não levam `import`: um arquivo compacto importa sozinho tudo o que `java.base` exporta, o que inclui `java.util.concurrent`. O método `join()` faz com que `main` espere a thread terminar; sem ele, o programa poderia terminar antes de a thread imprimir. Isto é o que ele imprime:

```text
$ java fig09_01.java
de-plataforma: virtual=false, demonio=false
virtual-1: virtual=true, demonio=true
```

Três dados em duas linhas. A thread de plataforma é `virtual=false`; a outra, `virtual=true`. E a virtual é **daemon** (`demonio=true`, “demonio” é o daemon em espanhol): uma thread daemon não mantém a JVM viva, de modo que se `main` termina e só restam threads virtuais, o programa acaba sem esperá-las. É uma diferença que morde: se você lança uma thread virtual “para que trabalhe em segundo plano” e `main` termina, você a corta. (Por isso o executor de 9.2 se fecha esperando as suas tarefas.)

Você pode ver a mecânica das portadoras em qualquer thread virtual. Imprimir `Thread.currentThread()` a partir de uma dá algo assim:

```text
VirtualThread[#26]/runnable@ForkJoinPool-1-worker-1
```

Depois da arroba está a thread portadora sobre a qual ela rodava naquele momento. Ao lançar vinte mil threads virtuais e anotar a sua portadora, na minha máquina apareceram exatamente dez nomes, de `worker-1` a `worker-10`: tantos quanto os núcleos que ela tem. Vinte mil threads “de Java” repartidas em dez threads “do sistema”.

#### 9.1.2 O que se ganha: escala, não velocidade

A figura 9.2 coloca a diferença em números, com uma tarefa que não faz nada além de esperar 200 milissegundos (um serviço que demora para responder). Ela lança mil delas duas vezes: uma em um *pool* clássico de 50 threads de plataforma e outra com uma thread virtual por tarefa. Mede cada uma com o relógio do sistema (`System.nanoTime()`):

```java
// fig09_02.java
static final int TAREAS = 1_000;
static final int ESPERA_MS = 200;

void esperar() {
  try {
    Thread.sleep(ESPERA_MS);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
}

long medirMs(ExecutorService ejecutor) {
  long inicio = System.nanoTime();
  try (ejecutor) {
    for (int i = 0; i < TAREAS; i++) {
      ejecutor.submit(this::esperar);
    }
  }
  return (System.nanoTime() - inicio) / 1_000_000;
}

void main() {
  long conPool = medirMs(Executors.newFixedThreadPool(50));
  long conVirtuales = medirMs(Executors.newVirtualThreadPerTaskExecutor());
  IO.println("pool de 50 hilos de plataforma, más de 3 s: " + (conPool > 3000));
  IO.println("un hilo virtual por tarea, menos de 1 s: " + (conVirtuales < 1000));
}
```

Alguns detalhes de leitura antes de rodá-la. `try (ejecutor) { ... }` usa uma variável que já existe como recurso do `try-with-resources` (o Java permite isso desde a versão 9, enquanto a variável não for reatribuída); é a mesma ideia da lição 5, e ao sair do bloco se chama `close()`. `this::esperar` é uma referência a um método desta mesma classe, que o Java converte na tarefa. E as duas linhas finais imprimem um booleano, não os milissegundos, para que a saída seja sempre a mesma:

```text
$ java fig09_02.java
pool de 50 hilos de plataforma, más de 3 s: true
un hilo virtual por tarea, menos de 1 s: true
```

Os milissegundos reais você os vê se imprimir as variáveis `conPool` e `conVirtuales`. Em três execuções seguidas me deram 4,152, 4,148 e 4,161 ms para o *pool*, e 221, 214 e 206 ms para as threads virtuais. A aritmética explica o primeiro: mil tarefas entre 50 threads são 20 rodadas, e 20 rodadas de 200 ms são 4 segundos. Com uma thread virtual por tarefa não há rodadas: as mil esperam juntas, e o tempo total é o de uma única espera, mais o custo de iniciar. Em outra medição, cem mil threads virtuais dormindo um segundo cada uma terminaram em 1.4 segundo.

Aqui convém frear um mal-entendido que estraga mais de um design. A documentação da Oracle diz sem rodeios: as threads virtuais **não são threads mais rápidas**; não executam o código mais depressa que uma de plataforma. Elas dão **escala** (mais tarefas em andamento ao mesmo tempo), não **velocidade** (menos tempo por tarefa). E servem para tarefas que passam quase todo o seu tempo **esperando**, não para trabalho de processador: se cada tarefa fosse uma fatoração longa, dez mil threads virtuais não terminariam antes que dez, porque os núcleos são os mesmos. O `revisor` é o caso ideal: cada tarefa manda uma requisição e espera.

Mais um dado, que poupa você de cuidados sobre os quais vai ler em livros anteriores: com o Java 21 havia um problema chamado *pinning* (uma thread virtual que bloqueava dentro de um bloco `synchronized` ficava presa à sua portadora e não a liberava). Desde o Java 24 [isso já não acontece](https://openjdk.org/jeps/491) nesses blocos; o conselho de “evite `synchronized` com threads virtuais” que ainda circula é de outra época.

### 9.2 Uma thread virtual por tarefa

#### 9.2.1 O executor que fecha

Criar threads à mão com `Thread.ofVirtual().start(...)` serve para entender, não para organizar trabalho. Para organizá-lo se usa um **executor**, um objeto ao qual você entrega tarefas e que se encarrega de rodá-las. A fábrica que nos interessa é uma linha:

```java
try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
  // aquí se mandan tareas con ejecutor.submit(...) o ejecutor.invokeAll(...)
}
```

O seu nome diz tudo: *virtual thread per task executor*, “executor de uma thread virtual por tarefa”. Cada tarefa que você manda recebe uma thread virtual nova, que morre quando a tarefa termina. Não há fila, não há tamanho de *pool*, não há nada para configurar. E como `ExecutorService` é `AutoCloseable` desde o Java 19, o `try-with-resources` faz o que importa: ao sair do bloco chama `close()`, que **espera todas as tarefas terminarem** antes de continuar. É a garantia que os executores antigos não tinham, em que esquecer o `shutdown()` deixava threads vivas e um programa que não terminava. Aqui o bloco `try` é uma cerca: o que você começa dentro termina dentro.

A regra desta seção cabe em uma frase, e é a que o temário do curso põe em negrito: **uma thread virtual por tarefa, nunca em um *pool***. Um *pool* existe para repartir um recurso escasso —as threads de plataforma— entre muitas tarefas. As threads virtuais não são escassas: pode-se ter milhões. Se você coloca um *pool* em cima delas, volta a criar uma escassez artificial, com as mesmas filas e os mesmos tetos, e perde justamente o que as tornava úteis. O guia oficial resume assim: as threads virtuais não devem representar um recurso compartilhado, mas **uma tarefa**. Se o que você quer é limitar quantas tarefas avançam ao mesmo tempo, existe uma ferramenta feita para isso, e você a vê em 9.4.

#### 9.2.2 Por que um `send` que bloqueia está bem

Olhe de novo como o `revisor` verifica um serviço desde a lição 8: `cliente.send(peticion, BodyHandlers.discarding())` **bloqueia**: a thread que o chama fica esperando até que chegue a resposta ou se esgote o tempo. Em uma thread de plataforma isso era um luxo caro; por isso a programação concorrente em Java passou anos buscando formas de não bloquear (*callbacks*, `CompletableFuture`, bibliotecas reativas), com código que se lê de cabeça para baixo. Em uma thread virtual o bloqueio **não custa nada**: a JVM desmonta a thread enquanto ela espera e usa a portadora para outra. Por isso `revisar` não muda nem uma linha nesta lição. Continua sendo o método linear da lição 8: monta a requisição, a manda, espera, classifica. A concorrência não está dentro de `revisar`; está em como ele é chamado.

Duas coisas dessa chamada é preciso ter presentes. Primeiro, o `HttpClient` **é compartilhado**: a documentação da classe diz que uma instância é imutável e serve para mandar muitas requisições, então o `revisor` cria um só e todas as tarefas o usam; criar um cliente por requisição seria jogar fora as suas conexões reutilizáveis. Segundo, o cliente também é `AutoCloseable`, e a ordem de fechamento importa: em um `try-with-resources` com dois recursos eles se fecham **ao contrário de como foram declarados**. Por isso o cliente se declara primeiro e o executor depois: ao sair, fecha-se o executor (espera todas as tarefas) e *depois* o cliente. Ao contrário, você fecharia o cliente com requisições em andamento.

### 9.3 `invokeAll` e a ordem estável

#### 9.3.1 `Callable`, `Future` e esperar todas

`submit` aceita duas classes de tarefa. Um `Runnable` não devolve nada nem pode lançar uma exceção verificada. Um **`Callable<T>`** devolve um valor de tipo `T` e pode lançar qualquer exceção: é do que `revisar` precisa, que devolve um `Resultado` e pode lançar `InterruptedException` (a lição 5 explicou por que essa nunca se engole). O que você recebe ao mandar uma tarefa é um **`Future<T>`**: uma promessa de resultado. `futuro.get()` espera —se for preciso— a tarefa terminar e entrega o seu valor.

O método que une as duas coisas é `invokeAll`: recebe uma coleção de `Callable`, executa todas, **espera todas terminarem** e devolve uma `List<Future<T>>`. O importante está na documentação e é o que o torna adequado para o `revisor`: a lista que devolve tem **a mesma ordem da que você lhe deu**, não importa qual terminou primeiro. A figura 9.3 comprova isso com três tarefas que demoram 300, 50 e 150 ms; a mais rápida é a segunda da lista, e mesmo assim sai em segundo:

```java
// fig09_03.java
String tarea(String nombre, long esperaMs) throws InterruptedException {
  Thread.sleep(esperaMs);
  return nombre;
}

void main() throws Exception {
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Callable<String>> tareas =
        List.of(
            () -> tarea("lenta", 300),
            () -> tarea("rapida", 50),
            () -> tarea("mediana", 150));

    for (Future<String> futuro : ejecutor.invokeAll(tareas)) {
      IO.println("en el orden de la lista: " + futuro.get());
    }

    List<Callable<String>> conFallo =
        List.of(
            () -> tarea("buena", 10),
            () -> {
              throw new IllegalStateException("fallé a propósito");
            });
    List<Future<String>> futuros = ejecutor.invokeAll(conFallo);
    IO.println("invokeAll regresó; la tarea 2 terminó: " + futuros.get(1).isDone());
    try {
      futuros.get(1).get();
    } catch (ExecutionException e) {
      IO.println("get() lanza " + e.getClass().getSimpleName());
      IO.println("la causa es " + e.getCause());
    }
  }
}
```

Acompanhe a figura por partes. `tarea` dorme e devolve o seu nome. A lista de três `Callable` se escreve com lambdas, e `invokeAll` as roda todas com threads virtuais. O primeiro `for` percorre os `Future` em ordem. Depois se monta uma segunda lista em que uma tarefa **lança uma exceção**, e vê-se algo importante: `invokeAll` retorna normalmente, a tarefa aparece como terminada, e a falha não explode até que você peça o seu valor com `get()`. Esta é a execução:

```text
$ java fig09_03.java
en el orden de la lista: lenta
en el orden de la lista: rapida
en el orden de la lista: mediana
invokeAll regresó; la tarea 2 terminó: true
get() lanza ExecutionException
la causa es java.lang.IllegalStateException: fallé a propósito
```

As três primeiras linhas saem na ordem da lista (`lenta`, `rapida`, `mediana`), não na ordem em que as tarefas terminaram (que foi `rapida`, `mediana`, `lenta`). Para o `revisor` isto é o que garante o ponto 3 de “Como você sabe que terminou”: o relatório sai **sempre na mesma ordem**, a da configuração, embora `inventario` demore dois segundos e `pagos` oitenta e sete milissegundos. Se você recolhesse os resultados conforme chegam, a ordem mudaria a cada execução e comparar dois relatórios seria uma dor.

#### 9.3.2 `ExecutionException`: a exceção de outra thread

Quando uma tarefa lança uma exceção na sua thread, essa exceção não pode subir pela pilha de quem a mandou —são pilhas diferentes—. O Java a guarda dentro do `Future` e, ao chamar `get()`, a entrega **embrulhada** em uma `ExecutionException`. As duas últimas linhas da figura mostram isso: o tipo que você captura é `ExecutionException`, mas o que importa é a sua **causa**, `getCause()`, que é a exceção original (`IllegalStateException: fallé a propósito`, ou seja, “falhei de propósito”). Quem a captura e só imprime a mensagem do invólucro perde justamente a informação de que precisa. É o mesmo princípio da lição 5, o das exceções próprias com a sua causa: uma exceção embrulhada se desembrulha, não se descarta.

Isso obriga você a decidir o que fazer com ela, e a decisão do `revisor` é deliberada. Há duas classes de problema. Um **serviço fora do ar** é um resultado esperado: `revisar` o captura e o converte em um `Resultado` com estado `CAIDO` e o seu motivo, e nunca deixa escapar uma `IOException`. Por isso nenhuma tarefa falha por culpa da rede, e o relatório sai completo mesmo que meio mundo esteja fora do ar. Um **erro do programa** (uma `NullPointerException`, um erro de lógica) é outra coisa: se uma tarefa falha assim, algo está errado no código, e escondê-lo atrás de um “serviço fora do ar” seria mentir. O `revisor` o relança como `IllegalStateException` com a causa original anexada. Em uma frase: **um serviço fora do ar não derruba o relatório; um erro do programa não se esconde**.

Um último detalhe do contrato: se a thread que espera em `invokeAll` é interrompida, o método **cancela as tarefas que não terminaram** e lança `InterruptedException`. Por isso a assinatura de `revisarTodos` declara `throws InterruptedException` e não a engole.

### 9.4 Limitar com `Semaphore`

#### 9.4.1 Para que limitar se as threads são baratas

Se as threads virtuais são quase de graça, para que limitar? Porque o recurso escasso já não é a thread; é **o que há do outro lado**. Vinte serviços consultados ao mesmo tempo é uma cortesia razoável. Vinte mil serviços consultados ao mesmo tempo —ou vinte requisições simultâneas contra o mesmo servidor— podem derrubar um serviço frágil, esgotar as portas do sistema ou disparar o alarme de um provedor. O limite protege o **destino**, não a JVM. E essa decisão não deveria depender do número de threads que por acaso tenha um *pool*.

O Java tem uma ferramenta pensada para isto há vinte anos: o **`Semaphore`**. Um semáforo guarda um número de **permissões**. Quem quer avançar chama `acquire()` (pegar uma permissão); se não há, **espera** até que outro a solte com `release()`. Com `new Semaphore(2)`, no máximo duas tarefas estão dentro da zona protegida; as demais fazem fila. A figura 9.4 lança seis tarefas de 100 ms com um semáforo de duas permissões, e registra o máximo de tarefas simultâneas com um `AtomicInteger` (um inteiro que pode ser modificado a partir de várias threads sem perder contagens; volta em 9.5):

```java
// fig09_04.java
final AtomicInteger activas = new AtomicInteger();
final AtomicInteger maximo = new AtomicInteger();

void trabajar() throws InterruptedException {
  int ahora = activas.incrementAndGet();
  maximo.accumulateAndGet(ahora, Math::max);
  Thread.sleep(100);
  activas.decrementAndGet();
}

void main() throws Exception {
  var semaforo = new Semaphore(2);
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 6; i++) {
      ejecutor.submit(
          () -> {
            semaforo.acquire();
            try {
              trabajar();
            } finally {
              semaforo.release();
            }
            return null;
          });
    }
  }
  IO.println("tareas lanzadas: 6, máximo al mismo tiempo: " + maximo.get());
}
```

Observe a forma da tarefa: `acquire()`, depois um `try` com o trabalho, e `release()` no **`finally`**. Não é estilo: é uma condição de correção. Se o trabalho lança uma exceção e o `release()` não está em um `finally`, essa permissão se perde para sempre; depois de falhas suficientes, todas as permissões estão perdidas e as tarefas que sobram esperam eternamente, um programa travado sem nenhum erro à vista. A execução:

```text
$ java fig09_04.java
tareas lanzadas: 6, máximo al mismo tiempo: 2
```

Seis tarefas lançadas, nunca mais de duas ao mesmo tempo. E repare no que o semáforo **não** fez: não limitou as threads. As seis tarefas têm a sua thread virtual desde o início; quatro estão dormindo em `acquire()` e não custam nada. O guia oficial da Oracle recomenda justamente isto —um semáforo para limitar— e explica que o seu efeito se parece com o de um *pool* de tamanho fixo, porque em ambos os casos as tarefas que sobram fazem fila, mas sem reservar threads para isso.

#### 9.4.2 Por que não um *pool* de threads virtuais

A tentação é resolver o limite com o que você já conhece: `Executors.newFixedThreadPool(2, fabrica)`, em que a fábrica produz threads virtuais. Funciona, e essa é a armadilha. A figura 9.5 mede o máximo de tarefas simultâneas das duas formas:

```java
// fig09_05.java
final AtomicInteger activas = new AtomicInteger();
final AtomicInteger maximo = new AtomicInteger();

void trabajar() {
  int ahora = activas.incrementAndGet();
  maximo.accumulateAndGet(ahora, Math::max);
  try {
    Thread.sleep(100);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
  activas.decrementAndGet();
}

int maximoCon(ExecutorService ejecutor) {
  maximo.set(0);
  try (ejecutor) {
    for (int i = 0; i < 6; i++) {
      ejecutor.submit(this::trabajar);
    }
  }
  return maximo.get();
}

void main() {
  var fabrica = Thread.ofVirtual().factory();
  int conPool = maximoCon(Executors.newFixedThreadPool(2, fabrica));
  int sinPool = maximoCon(Executors.newVirtualThreadPerTaskExecutor());
  IO.println("pool fijo de 2 hilos virtuales: " + conPool);
  IO.println("un hilo virtual por tarea: " + sinPool);
}
```

```text
$ java fig09_05.java
pool fijo de 2 hilos virtuales: 2
un hilo virtual por tarea: 6
```

O *pool* fixo limita a 2, o executor por tarefa deixa passar as 6. Há dois problemas em usar o primeiro para limitar. Um é de clareza: o limite ficou escondido em um argumento numérico de uma fábrica, e ninguém que leia o código vai entender que “2” significa “requisições simultâneas permitidas”. O outro é de engenharia: você acabou de reintroduzir uma fila, um tamanho para ajustar e uma thread para reservar, para obter o que `Semaphore(2)` dá em uma linha e com o seu propósito escrito no nome. O guia oficial é taxativo: as threads virtuais **nunca devem ser agrupadas**. Menos concorrência, mais código, nenhuma vantagem.

#### 9.4.3 O passo do projeto: `revisarTodos` concorrente

Você já tem as peças. Assim fica `Revision.java`; o novo é a constante `LIMITE_POR_OMISION` e o método `revisarTodos`, e todo o resto (`revisar`, `nuevoCliente`) é o da lição 8. Leia `revisarTodos` em ordem: primeiro valida o limite; cria o semáforo; abre o cliente e o executor (nessa ordem, como você viu em 9.2.2); monta uma tarefa por serviço, que pega uma permissão, verifica e a solta em um `finally`; manda todas com `invokeAll`; e recolhe os resultados **em ordem**, desembrulhando qualquer `ExecutionException`:

```java
// fig09_09/src/main/java/mx/habil/revisor/Revision.java

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;

/** Consulta los servicios por HTTP y arma sus resultados. */
final class Revision {

  /** Cuánto se espera para establecer la conexión, antes de decir que no hay respuesta. */
  static final long TIEMPO_CONEXION_MS = 2000;

  /** Cuántas peticiones puede haber a la vez si nadie dice otra cosa. */
  static final int LIMITE_POR_OMISION = 10;

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

  /**
   * Revisa todos los servicios a la vez, con a lo más {@code limite} peticiones en vuelo.
   *
   * @param servicios los servicios, en el orden en que debe salir el reporte
   * @param limite cuántas peticiones puede haber al mismo tiempo; al menos 1
   * @return los resultados, en el mismo orden que {@code servicios}
   * @throws InterruptedException si alguien interrumpe al programa mientras espera
   */
  static List<Resultado> revisarTodos(List<Servicio> servicios, int limite)
      throws InterruptedException {
    if (limite < 1) {
      throw new IllegalArgumentException("el límite debe ser al menos 1");
    }
    var semaforo = new Semaphore(limite);
    try (var cliente = nuevoCliente();
        var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
      var tareas = new ArrayList<Callable<Resultado>>();
      for (Servicio servicio : servicios) {
        tareas.add(
            () -> {
              semaforo.acquire();
              try {
                return revisar(cliente, servicio);
              } finally {
                semaforo.release();
              }
            });
      }
      var resultados = new ArrayList<Resultado>();
      for (Future<Resultado> futuro : ejecutor.invokeAll(tareas)) {
        try {
          resultados.add(futuro.get());
        } catch (ExecutionException e) {
          throw new IllegalStateException("la revisión de un servicio falló", e.getCause());
        }
      }
      return resultados;
    }
  }
}
```

Três decisões desta função que convém ver de perto. A primeira: **o semáforo é pego dentro da tarefa, não antes de lançá-la**. Todas as tarefas existem desde o início, e cada uma espera a sua vez por conta própria; o `invokeAll` fica simples. A segunda: o tempo que `revisar` mede com `System.nanoTime()` começa **depois** de pegar a permissão, de modo que a coluna TIEMPO (“tempo”) continua dizendo quanto demorou o serviço e não quanto esperou na fila. A terceira: `limite < 1` é rejeitado logo de entrada com uma `IllegalArgumentException`, porque um semáforo com zero permissões deixaria todas as tarefas esperando para sempre.

A constante `LIMITE_POR_OMISION` vale 10, e `Revisor` a usa por enquanto. `Revisor` muda em duas coisas: o primeiro argumento, se houver, é o arquivo de configuração (assim você pode verificar outra lista sem editar nada), e chama `revisarTodos` com o limite padrão:

```java
// fig09_09/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.List;

/** El revisor: lee servicios.json, consulta cada servicio y escribe la tabla. */
public final class Revisor {

  private Revisor() {}

  /**
   * Punto de entrada del programa.
   *
   * @param args el primero, si lo hay, es el archivo de configuración
   * @throws InterruptedException si alguien interrumpe al programa mientras espera
   */
  public static void main(String[] args) throws InterruptedException {
    List<Servicio> servicios;
    try {
      Path ruta = args.length > 0 ? Path.of(args[0]) : Path.of("servicios.json");
      servicios = Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS);
    } catch (ErrorDeConfiguracion e) {
      System.err.println("error de configuración: " + e.getMessage());
      return;
    }
    var resultados = Revision.revisarTodos(servicios, Revision.LIMITE_POR_OMISION);
    System.out.println(Formato.tabla(Reporte.de(resultados)));
  }
}
```

O arquivo `servicios-20.json` tem vinte serviços, `servicio01` a `servicio20`, todos apontados para a rota `/espera/500` do laboratório, que responde 200 depois de meio segundo. Assim ele é completo:

```json fig09_09/servicios-20.json
{
  "servicios": [
    {"nombre": "servicio01", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio02", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio03", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio04", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio05", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio06", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio07", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio08", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio09", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio10", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio11", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio12", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio13", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio14", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio15", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio16", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio17", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio18", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio19", "url": "http://127.0.0.1:8089/espera/500"},
    {"nombre": "servicio20", "url": "http://127.0.0.1:8089/espera/500"}
  ]
}
```

### Medir: o relógio decide

Com o laboratório ligado, compile (`mvn -B verify`) e rode o `revisor` contra esses vinte serviços, medindo quanto demora o programa completo. No Linux, em um terminal com `bash`, a forma limpa de medir é a palavra reservada `time` com um formato curto:

```text
$ TIMEFORMAT='%R s'
$ time java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor servicios-20.json > /dev/null
1.258 s
```

Mude a constante `LIMITE_POR_OMISION` em `Revision.java` (por enquanto não há outra forma; na Lição 10 será `--concurrencia`), recompile e repita. Estas são as minhas medições, duas execuções por limite, sempre com os vinte serviços de 500 ms:

| Limite | Rodadas | O que a aritmética prevê | O que eu medi (ambas as execuções) |
|---|---|---|---|
| 1 (equivale a **em série**) | 20 | 20 × 0.5 = 10 s | 10.413 s e 10.387 s |
| 3 | 7 | 7 × 0.5 = 3.5 s | 3.808 s e 3.799 s |
| 10 | 2 | 2 × 0.5 = 1 s | 1.258 s e 1.298 s |
| 20 | 1 | 1 × 0.5 = 0.5 s | 0.751 s e 0.779 s |

Duas coisas para ler a tabela sem se enganar. **Uma:** o medido sempre passa do previsto em uns 0.2 segundo, e esse é o custo fixo de iniciar a JVM e o cliente HTTP. Eu comprovei: um único serviço com espera zero demora 0.2 segundo no total. Subtraia 0.2 de cada medição e a aritmética se cumpre com um ou dois décimos de diferença. **Duas:** a última coluna responde à pergunta do temário, “por que um limite de 10 demora o dobro de um de 20”. Com limite 10 e vinte serviços há **duas rodadas**: os primeiros dez pegam permissão, esperam meio segundo; quando soltam, entram os outros dez e esperam outro meio segundo. Com limite 20 entram todos de uma vez. O tempo total é o número de rodadas vezes o que dura cada espera, não o número de serviços.

Com os quatro serviços canônicos, em contrapartida, a melhora é modesta: passam de uns 2.8 segundos em série para uns 2.5. Não é um defeito: o tempo já não é a soma (2.5 s) mas **o do mais lento** (`inventario`, 2.3 s), e como esse manda, há pouco a ganhar. A concorrência ajuda quando há muitas esperas parecidas; com uma dominante, quase não ajuda. É importante que você saiba isso, porque “ficou concorrente” não significa “ficou rápido”: mede-se.

E com os quatro serviços canônicos de `servicios.json`, a partir da pasta do projeto e com o laboratório ligado, uma execução completa se vê assim:

```text
$ cd fig09_09
$ java -cp "target/classes:target/lib/*" mx.habil.revisor.Revisor
SERVICIO    ESTADO    TIEMPO  DETALLE
catalogo    ARRIBA     176ms  200
inventario  LENTO       2.3s  200
pagos       ARRIBA     122ms  200
reportes    CAIDO          —  sin conexión

4 servicios · 2 arriba · 1 lento · 1 caído · promedio 878ms
```

Os tempos da coluna TIEMPO são os de uma execução minha; os seus serão diferentes, mas ESTADO (“estado”) e DETALLE (“detalhe”) nunca mudam.

*(Aqui termina a primeira sessão.)*

### 9.5 Estado compartilhado: onde a concorrência morde

#### 9.5.1 Uma condição de corrida, provocada e medida

Tudo o que veio antes foi fácil porque as tarefas não se tocam. Assim que duas threads **escrevem o mesmo dado**, aparece o problema que dá má fama à concorrência. Vê-se melhor provocando-o. Este programa lança 10,000 tarefas, e cada uma soma 100 vezes um a um contador que elas compartilham. Se tudo saísse bem, o total seria exatamente um milhão. Salve-o como `Carrera.java`:

```java
int contador = 0;

void main() {
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      ejecutor.submit(
          () -> {
            for (int j = 0; j < 100; j++) {
              contador++;
            }
          });
    }
  }
  IO.println("esperado 1000000, contado " + contador);
}
```

Não leva marcador de figura de propósito: o seu resultado muda a cada execução, então não pode ser verificado contra uma saída fixa. Rodei o programa quatro vezes seguidas e isto foi o que contou:

```text
esperado 1000000, contado 841360
esperado 1000000, contado 852985
esperado 1000000, contado 773685
esperado 1000000, contado 855739
```

Não há uma única exceção, não há um aviso do compilador, e o programa “funciona”: termina, imprime um número, sai com código 0. Mas **perdeu entre 144,000 e 226,000 somas**, e não perde as mesmas duas vezes. Este é o traço que torna uma corrida perigosa: ela não se anuncia. Um programa com uma corrida passa nos seus testes em 99 % das vezes e falha em produção, numa terça-feira, com carga.

Por que acontece é mais simples do que parece. `contador++` parece uma operação, mas são três: **ler** o valor, **somar um** e **escrever** o resultado. Se duas threads leem o 41 ao mesmo tempo, as duas calculam 42 e as duas escrevem 42: somou-se uma vez em vez de duas, e uma soma desapareceu. Com dez portadoras trabalhando em paralelo de verdade, essa sobreposição ocorre centenas de milhares de vezes. (Há além disso uma segunda causa, a da **visibilidade**: sem uma instrução que o ordene, a memória de uma thread pode não ver o que outra escreveu. O [modelo de memória de Java](https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html), no capítulo 17 da especificação da linguagem, define quando uma escrita fica garantida de ser vista: a regra se chama *happens-before*.)

Não é um problema exclusivo dos números. Com uma coleção compartilhada é pior. O mesmo experimento com um `HashMap` —guarda 10,000 chaves distintas, cada uma a partir da sua tarefa— deveria terminar com 10,000 entradas. Salve-o como `MapaCompartido.java`:

```java
void main() {
  var mapa = new HashMap<Integer, Integer>();
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      int clave = i;
      ejecutor.submit(() -> mapa.put(clave, clave));
    }
  }
  IO.println("esperadas 10000, hay " + mapa.size());
}
```

As minhas três execuções terminaram com 8,339, 7,195 e 7,563 entradas. Faltavam entre 1,600 e 2,800 entradas. Um `HashMap` não foi projetado para ser escrito a partir de várias threads, e a sua estrutura interna se corrompe: além disso pode lançar exceções e, em versões antigas de Java, ficar em um ciclo infinito. Que às vezes “saia certo” não é consolo; é o que o torna difícil de caçar.

#### 9.5.2 Três maneiras de corrigir, em ordem de preferência

**A primeira, e a melhor: não compartilhar.** Se cada tarefa trabalha com os seus próprios dados e devolve um resultado, não há nada a proteger. É o que o `revisor` faz, e explico isso em 9.5.3.

**A segunda: tipos feitos para serem compartilhados.** O pacote [`java.util.concurrent`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/package-summary.html) traz tipos cujas operações são atômicas, isto é, indivisíveis. `AtomicInteger` e `AtomicLong` são inteiros que podem ser somados a partir de várias threads sem perder contagens; `LongAdder`, pensado para contadores muito disputados, é mais rápido quando há muitas threads. E `ConcurrentHashMap` é o mapa para escritas concorrentes. O seu método `merge` é o que corrige o caso típico de contar por categoria: `mapa.merge("ARRIBA", 1, Integer::sum)` significa “se a chave não existe, coloque 1; se existe, substitua pelo resultado de somar”; e segundo a documentação, toda a invocação se faz **de forma atômica**. A figura 9.6 repete o experimento de contar estados com 10,000 tarefas, agora com `merge`, e soma também com um contador atômico:

```java
// fig09_06.java
void main() {
  var porEstado = new ConcurrentHashMap<String, Integer>();
  var total = new AtomicInteger();
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      ejecutor.submit(
          () -> {
            porEstado.merge("ARRIBA", 1, Integer::sum);
            total.incrementAndGet();
          });
    }
  }
  IO.println(porEstado);
  IO.println("total atómico: " + total.get());
}
```

```text
$ java fig09_06.java
{ARRIBA=10000}
total atómico: 10000
```

Diferentemente da corrida, este resultado sai **sempre** igual, as vezes que você o rodar: 10,000 no mapa e 10,000 no contador. Isso, sim, pode ser verificado, e por isso esta figura leva marcador.

**A terceira: locks.** O Java tem `synchronized` e os locks de `java.util.concurrent.locks` para proteger um trecho de código de modo que só uma thread por vez o execute. Funcionam, e às vezes são a única coisa que serve; mas são a ferramenta mais fácil de usar mal (deadlocks, locks que se esquecem, seções longas demais), e o curso não os usa. Com as duas opções anteriores quase nunca fazem falta.

#### 9.5.3 Por que o `revisor` não tem corridas

Revise cada dado que as tarefas de `revisarTodos` tocam, um por um, porque aqui se cobra o que você aprendeu na lição 3. O `Servicio` que cada tarefa recebe é um *record*: **imutável**, ninguém o muda. O `HttpClient` é seguro para ser compartilhado (a documentação garante). A tarefa não escreve em nenhuma variável de fora: **devolve** o seu `Resultado`, que é outro *record* imutável. O único objeto mutável que as tarefas compartilham é o `Semaphore`, que é feito justamente para isso. E a lista `resultados`, que é, sim, uma `ArrayList` normal (não segura para várias threads), só é tocada pela thread que chamou `revisarTodos`, **depois** de `invokeAll` terminar; o próprio pacote `java.util.concurrent` documenta que o que uma tarefa fez antes de terminar fica garantido de ser visto por quem recolhe o seu resultado com `get()`.

A regra de design que sai daqui vale mais que qualquer ferramenta: **comunique devolvendo valores, não compartilhando memória**. Quando uma tarefa precisa dizer algo a quem a lançou, que o devolva. A imutabilidade que o curso vem cuidando desde a lição 3 não era um enfeite de estilo: é o que faz com que uma peça possa rodar em cem threads sem pensar nelas.

### 9.6 Valores com escopo: `ScopedValue`

Há um problema que as tarefas concorrentes agravam: o **contexto**. Imagine que cada verificação leva um identificador de execução, `r-001`, e que você quer que qualquer método —o que classifica, o que monta a requisição, o que escreve uma mensagem de diagnóstico— possa lê-lo. A saída óbvia é passá-lo como parâmetro por cada método da cadeia, e vira um ruído que ninguém quer. A saída de sempre em Java foi `ThreadLocal`, uma variável que tem um valor diferente em cada thread. Mas tem três defeitos conhecidos: é **mutável** (qualquer um a muda, e ninguém sabe quem), vive **até que alguém a limpe** (esquecê-la é um vazamento de memória), e copiá-la para threads filhas é **caro**, o que pesa quando são milhões de threads virtuais.

Para isso existe `ScopedValue`, final desde o Java 25 ([JEP 506](https://openjdk.org/jeps/506)). Um valor com escopo é **imutável** e existe **só enquanto dura um bloco de código**: você o vincula com `ScopedValue.where(CLAVE, valor).run(...)` (ou `.call(...)` se o bloco devolve algo ou lança exceções), e qualquer método chamado de dentro pode lê-lo com `CLAVE.get()`. Ao sair do bloco, o valor desaparece sozinho. A figura 9.7 o mostra, e mostra também o seu limite:

```java
// fig09_07.java
static final ScopedValue<String> CORRIDA = ScopedValue.newInstance();

void anotar(String mensaje) {
  IO.println("[corrida " + CORRIDA.get() + "] " + mensaje);
}

void main() throws Exception {
  ScopedValue.where(CORRIDA, "r-001")
      .call(
          () -> {
            anotar("revisando 4 servicios");
            Thread otro =
                Thread.ofVirtual()
                    .start(() -> IO.println("en un hilo nuevo, ligado = " + CORRIDA.isBound()));
            otro.join();
            return null;
          });
  IO.println("fuera del bloque, ligado = " + CORRIDA.isBound());
}
```

```text
$ java fig09_07.java
[corrida r-001] revisando 4 servicios
en un hilo nuevo, ligado = false
fuera del bloque, ligado = false
```

O método `anotar` não recebe o identificador como parâmetro e o lê do mesmo jeito: esse é o serviço que ele presta. A segunda linha é a parte que surpreende: a thread virtual que se cria **dentro** do bloco à mão **não vê o valor** (`ligado = false`; *ligado* significa “vinculado” em espanhol). O valor viaja para as chamadas de uma mesma thread, não para as threads que você lança por conta própria; só a concorrência estruturada de 9.7 o herda para os seus filhos. E a terceira linha confirma que fora do bloco não sobra nada.

O `revisor` deste curso **não usa** `ScopedValue`: não tem nenhum contexto para passar. Você vai vê-lo assim que escrever um servidor que atenda requisições de vários usuários e precisar saber, em qualquer ponto do código, para quem está trabalhando. Está aqui porque é o que substitui `ThreadLocal` no Java que você vai escrever de agora em diante, e porque estreou nesta versão.

### 9.7 O que vem aí: a concorrência estruturada

Em 9.3 você viu que `invokeAll` espera todas as tarefas, aconteça o que acontecer. Falta uma forma que muita gente quer: “lance estas tarefas **como uma unidade**: se uma falha, cancele as demais; se o bloco termina, nenhuma continua viva”. A isso se chama **concorrência estruturada**, e o JDK 25 a traz... **em versão prévia**. Uma versão prévia (*preview*) é um recurso completo mas ainda sujeito a mudanças, que só se ativa com uma flag. Esta mudou de forma a cada versão desde a 21; a quinta versão prévia é o [JEP 505](https://openjdk.org/jeps/505).

A figura 9.8 usa a API (`StructuredTaskScope.open()`, com `fork` para lançar e `join` para esperar). Compilada tal qual, **não roda**:

```java
// fig09_08.java
String consultar(String nombre) {
  return "respuesta de " + nombre;
}

void main() throws Exception {
  try (var alcance = StructuredTaskScope.open()) {
    var catalogo = alcance.fork(() -> consultar("catalogo"));
    var pagos = alcance.fork(() -> consultar("pagos"));
    alcance.join();
    IO.println(catalogo.get() + " / " + pagos.get());
  }
}
```

```text
$ java fig09_08.java
fig09_08.java:7: error: StructuredTaskScope is a preview API and is disabled by default.
  try (var alcance = StructuredTaskScope.open()) {
                     ^
  (use --enable-preview to enable preview APIs)
1 error
error: compilation failed
```

A mensagem diz tudo: a classe existe, mas é uma API em versão prévia e está desligada. Com a flag que a própria mensagem pede, funciona, sim:

```text
$ java --enable-preview --source 25 fig09_08.java
respuesta de catalogo / respuesta de pagos
```

**Não a use no `revisor` nem em produção**: a API pode mudar entre versões do JDK, e um programa que dependa dela teria que ser reescrito quando mudar. Por isso este curso a deixa como curiosidade. O que você leva é a ideia: quando se estabilizar, será a forma natural de dizer “estas três consultas são um único trabalho”, e `invokeAll` com um `Semaphore` terá sido o seu jeito de chegar até lá. Enquanto isso, o que você acabou de aprender funciona no Java 21, 25 e nas que vierem.

### 9.8 Os testes do `revisor` concorrente

O `revisor` concorrente traz três testes novos, na classe `RevisionConcurrenteTest`. Cada um testa uma das promessas de 9.3 e 9.4, e nenhum depende do relógio mais que o necessário. Isto é o que eles verificam: **a ordem** (o serviço mais lento, colocado primeiro na lista, continua sendo o primeiro do resultado); **o limite** (com limite 2 e oito requisições, o servidor do teste nunca vê mais de duas ao mesmo tempo, quem conta é um `AtomicInteger` dentro do próprio servidor); e **a validação** (um limite de 0 é uma `IllegalArgumentException`). Repare que o servidor do teste é criado com `Executors.newVirtualThreadPerTaskExecutor()`: o servidor de `com.sun.net.httpserver`, se não recebe um executor, atende as requisições **uma por uma** em uma única thread, e o teste do limite mediria 1 em vez de 2 sem que o semáforo tivesse nada a ver com isso.

```java
// fig09_09/src/test/java/mx/habil/revisor/RevisionConcurrenteTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RevisionConcurrenteTest {

  private static HttpServer servidor;
  private static int puerto;
  private static final AtomicInteger ACTIVAS = new AtomicInteger();
  private static final AtomicInteger MAXIMO = new AtomicInteger();

  @BeforeAll
  static void levantarServidor() throws IOException {
    servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext("/lento", i -> responder(i, 400));
    servidor.createContext("/rapido", i -> responder(i, 0));
    servidor.createContext("/contado", RevisionConcurrenteTest::responderContando);
    servidor.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    servidor.start();
    puerto = servidor.getAddress().getPort();
  }

  @AfterAll
  static void apagarServidor() {
    servidor.stop(0);
  }

  private static void responder(HttpExchange intercambio, long esperaMs) throws IOException {
    try {
      Thread.sleep(esperaMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    intercambio.sendResponseHeaders(200, -1);
    intercambio.close();
  }

  private static void responderContando(HttpExchange intercambio) throws IOException {
    int ahora = ACTIVAS.incrementAndGet();
    MAXIMO.accumulateAndGet(ahora, Math::max);
    try {
      responder(intercambio, 200);
    } finally {
      ACTIVAS.decrementAndGet();
    }
  }

  private static Servicio servicio(String nombre, String ruta) {
    return new Servicio(nombre, URI.create("http://127.0.0.1:" + puerto + ruta));
  }

  @Test
  void elMasLentoPrimeroEnLaListaSigueSiendoElPrimeroEnElResultado() throws Exception {
    var servicios = List.of(servicio("lento", "/lento"), servicio("rapido", "/rapido"));
    List<Resultado> resultados = Revision.revisarTodos(servicios, 10);
    assertEquals("lento", resultados.get(0).servicio().nombre());
    assertEquals("rapido", resultados.get(1).servicio().nombre());
    assertEquals(Estado.ARRIBA, resultados.get(1).estado());
  }

  @Test
  void conLimiteDosElServidorNuncaVeMasDeDosPeticionesALaVez() throws Exception {
    var servicios = new ArrayList<Servicio>();
    for (int i = 1; i <= 8; i++) {
      servicios.add(servicio("s" + i, "/contado"));
    }
    MAXIMO.set(0);
    Revision.revisarTodos(servicios, 2);
    assertEquals(2, MAXIMO.get());
  }

  @Test
  void unLimiteMenorQueUnoSeRechaza() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Revision.revisarTodos(List.of(servicio("s", "/rapido")), 0));
  }
}
```

Um teste que você nunca viu falhar não demonstra nada, então faça o experimento. Em `Revision.java`, troque `new Semaphore(limite)` por `new Semaphore(Integer.MAX_VALUE)` (um semáforo com tantas permissões que nunca freia) e rode os testes. Isto é o que diz o relatório do *surefire*:

```text
[ERROR] Tests run: 3, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.737 s <<< FAILURE! -- in mx.habil.revisor.RevisionConcurrenteTest
[ERROR] mx.habil.revisor.RevisionConcurrenteTest.conLimiteDosElServidorNuncaVeMasDeDosPeticionesALaVez -- Time elapsed: 0.220 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <2> but was: <8>
```

Ficou vermelho justamente o teste do limite, e com o número que devia: sem freio, as **oito** requisições chegaram ao mesmo tempo. Devolva o semáforo ao seu valor, e o relatório volta a verde. Para contar os testes sem confiar no código de saída do Maven, some-os do relatório, de duas formas distintas (uma contagem que decide algo se faz duas vezes):

```text
$ grep -h -o ' tests="[0-9]*"' target/surefire-reports/TEST-*.xml | awk -F'"' '{s+=$2} END {print s}'
28
$ cat target/surefire-reports/TEST-*.xml | grep -c '<testcase '
28
```

Vinte e cinco da lição 8 mais as três de hoje: 28, e nenhuma ignorada.

O `pom.xml` só muda em uma coisa: a versão passa a `0.9.0`. Fora dos arquivos que você viu completos nesta lição (`Revision.java`, `Revisor.java`, `RevisionConcurrenteTest.java` e `servicios-20.json`), todo o projeto fica exatamente como estava na lição 8, sem tocar uma linha.

## O erro que você vai ver

O erro desta lição não é uma mensagem, é uma **ausência de mensagem**: a corrida de 9.5.1. O programa termina sem exceções e com um número errado (`esperado 1000000, contado 773685`, ou seja, “esperado 1000000, contados 773685”). Se em algum momento um programa seu der resultados diferentes em execuções iguais, suspeite de uma corrida antes de qualquer outra coisa, e procure que dado duas threads escrevem ao mesmo tempo. Quatro mensagens de apoio, para reconhecê-las de relance:

- `java.util.concurrent.ExecutionException: java.lang.IllegalStateException: fallé a propósito`: uma tarefa falhou em outra thread. A causa está depois dos dois pontos; olhe `getCause()` (`fig09_03.java`).
- `AssertionFailedError: expected: <2> but was: <8>`: o teste do limite diz que o semáforo não freou (9.8).
- Um `HashMap` compartilhado que termina com menos entradas do que você escreveu (`esperadas 10000, hay 7195`): o mapa se corrompeu; use `ConcurrentHashMap`.
- `error: StructuredTaskScope is a preview API and is disabled by default.`: você usou um recurso em versão prévia sem a flag (`fig09_08.java`).

## O que se faz errado

**Colocar as threads virtuais em um *pool*.** `newFixedThreadPool(10, Thread.ofVirtual().factory())` limita, mas escondido, com uma fila e um tamanho que alguém terá que ajustar. Uma thread virtual por tarefa, e o limite com um `Semaphore` (9.4.2).

**Soltar a permissão fora de um `finally`.** Uma exceção entre `acquire()` e `release()` perde a permissão. Depois de algumas falhas todas as tarefas esperam para sempre, e o programa trava sem explicação (9.4.1).

**Engolir a `InterruptedException`.** Um `catch (InterruptedException e) {}` vazio esconde o sinal de que alguém quer que a tarefa pare. Se você não puder propagá-la, restaure a flag com `Thread.currentThread().interrupt()` (lição 5).

**Compartilhar uma coleção que não é concorrente.** `ArrayList`, `HashMap` e `HashSet` não são feitas para serem escritas a partir de várias threads. Use as de `java.util.concurrent`, ou melhor, não compartilhe (9.5).

**Usar threads virtuais para trabalho de processador.** Não dão velocidade, dão escala. Uma tarefa de cálculo longo ocupa a sua portadora o tempo todo e não cede a vez; para isso, o que conta é o número de núcleos (9.1.2).

**Lançar sem limite contra um serviço alheio.** Que as threads sejam baratas não quer dizer que o destino aguente. O limite se define pensando no serviço que recebe as requisições, não na JVM (9.4.1).

**Guardar contexto em um `ThreadLocal` mutável e esquecer de limpá-lo.** Para o que dura um bloco, `ScopedValue` (9.6).

**Testar concorrência com `Thread.sleep` para “dar tempo”.** Um teste que espera um tempo fixo e assume que já aconteceu o que esperava quebra em uma máquina lenta. Espere o que de verdade há que esperar (`join`, `invokeAll`, `close`), como fazem os testes de 9.8.

## Exercícios

### Exercício 1 — Meça os três tempos

Com o laboratório ligado e `servicios-20.json`, meça o `revisor` com `LIMITE_POR_OMISION` em 1, em 10 e em 20. Antes de rodar, escreva a sua previsão de cada uma e explique com as suas palavras por que o limite de 10 demora o dobro do de 20. Anote os três tempos reais no seu diário de bordo, na linha da lição 9.

### Exercício 2 — Preveja o 3

Sem rodar ainda: quanto deve demorar o `revisor` com limite 3 sobre esses mesmos vinte serviços? Escreva a conta (quantas rodadas, quanto dura cada uma). Depois meça, subtraia o que custa iniciar a JVM e compare. Se a sua previsão falhou por mais de alguns décimos, procure o que você deixou de fora.

### Exercício 3 — Corrija as duas corridas

Pegue `Carrera.java` e `MapaCompartido.java` de 9.5.1 e corrija-os, cada um com uma ferramenta diferente de 9.5.2. Você deve conseguir que o contador dê exatamente 1,000,000 e que o mapa tenha exatamente 10,000 entradas, **em cada execução**. Rode a sua versão três vezes para ver que não muda.

### Exercício 4 — Uma permissão que se perde, e o teste que a pega

Os três testes de 9.8 não detectam um `release()` mal posicionado. Escreva um quarto que detecte: dois serviços, limite 1, e o primeiro com um endereço que faça `revisar` falhar com uma exceção que **não** é de rede (por exemplo `ftp://127.0.0.1/x`, que `HttpRequest` rejeita ao construir a requisição). Antes de escrevê-lo, preveja o que aconteceria se `semaforo.release()` estivesse fora do `finally`. Depois tire-o do `finally`, veja o teste em vermelho e restaure-o.

## Soluções

### Solução 1

Os meus tempos foram 10.4 s, 1.27 s e 0.76 s (veja a tabela de 9.4.3). As previsões corretas eram 10, 1 e 0.5 segundo mais uns 0.2 de inicialização. Com limite 10 há duas rodadas de meio segundo, com limite 20 uma; o tempo é *rodadas × espera*, não *serviços × espera*. Se o seu número para o limite 20 é muito maior que 0.7 s, veja se o laboratório estava atendendo outra coisa.

### Solução 2

Vinte serviços entre três permissões são sete rodadas (seis cheias e uma de duas): 7 × 0.5 = 3.5 s, mais uns 0.2 s de inicialização, ou seja, 3.7 s. Medi 3.808 s e 3.799 s. O décimo que sobra são custos de operação (abrir conexões, repartir threads) que não medi separadamente; o importante é que a conta de rodadas acertou. Repare na última rodada: embora tenha só dois serviços, espera o meio segundo completo, porque uma rodada dura o que dura o seu serviço mais lento, não o que dura cada um dos seus membros.

### Solução 3

Para o contador, um `LongAdder` (também serve `AtomicLong`); para o mapa, um `ConcurrentHashMap`. As duas mudanças cabem em um programa:

```java
void main() {
  var contador = new LongAdder();
  var mapa = new ConcurrentHashMap<Integer, Integer>();
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      int clave = i;
      ejecutor.submit(
          () -> {
            for (int j = 0; j < 100; j++) {
              contador.increment();
            }
            mapa.put(clave, clave);
          });
    }
  }
  IO.println("esperado 1000000, contado " + contador.sum());
  IO.println("esperadas 10000 entradas, hay " + mapa.size());
}
```

As três execuções deram:

```text
esperado 1000000, contado 1000000
esperadas 10000 entradas, hay 10000
```

A lição é que a correção não é “ter mais cuidado”: é trocar o tipo do dado compartilhado por um feito para ser compartilhado.

### Solução 4

A previsão: com limite 1, o primeiro serviço entra, `revisar` lança a exceção, e como o `release()` ficou fora do `finally`, **nunca é executado**: a única permissão se perdeu. O segundo serviço fica esperando em `acquire()` para sempre, `invokeAll` nunca retorna e o programa trava. O teste, com um teto de tempo do JUnit para que o travamento se converta em uma falha com mensagem:

```java
  @Test
  @Timeout(5)
  void unErrorDelProgramaNoDejaPermisosPerdidos() {
    var servicios =
        List.of(
            new Servicio("malo", URI.create("ftp://127.0.0.1/x")), servicio("rapido", "/rapido"));
    assertThrows(IllegalStateException.class, () -> Revision.revisarTodos(servicios, 1));
  }
```

(Precisa de `import org.junit.jupiter.api.Timeout;`.) Com o `finally` no lugar, o teste passa: o primeiro serviço falha e solta a sua permissão, o segundo é verificado, e `revisarTodos` lança a `IllegalStateException` que você já conhece. Com o `release()` fora do `finally`, o relatório diz:

```text
[ERROR] mx.habil.revisor.RevisionConcurrenteTest.unErrorDelProgramaNoDejaPermisosPerdidos -- Time elapsed: 5.013 s <<< ERROR!
java.util.concurrent.TimeoutException: unErrorDelProgramaNoDejaPermisosPerdidos() timed out after 5 seconds
```

Sem o `@Timeout`, este mesmo teste **não falharia: travaria**, e um travamento em integração contínua custa muito mais que uma falha. Quando você testar concorrência, ponha um teto de tempo em tudo o que poderia esperar para sempre. Restaure o `finally` e o teste volta a verde (com os quatro testes desta classe, o total seria 29; a figura da lição fica com os três originais).

## Como sei que consegui

- [ ] `java fig09_01.java` imprime `virtual=true, demonio=true` para a thread virtual, e você sabe explicar por que uma thread virtual que não se espera é cortada quando `main` termina.
- [ ] `java fig09_02.java` imprime `true` nas suas duas linhas, e você sabe calcular à mão por que um *pool* de 50 demora quatro segundos com mil tarefas de 200 ms.
- [ ] Em `fig09_03.java` os três resultados saem na ordem da lista e não na de término, e você sabe dizer o que é a causa de uma `ExecutionException`.
- [ ] `java fig09_04.java` reporta que o máximo simultâneo é 2, e você sabe por que o `release()` vai em um `finally`.
- [ ] Você rodou `Carrera.java` várias vezes e viu que o número muda e nunca chega a 1,000,000.
- [ ] `java fig09_06.java` imprime `{ARRIBA=10000}` em cada execução.
- [ ] `mvn -B verify` termina em verde, e a soma dos testes do relatório, contada de duas formas, é 28.
- [ ] Com o laboratório ligado, `servicios-20.json` demora uns 10 s com limite 1, perto de 1 s com limite 10 e perto de 0.5 s com limite 20 (mais o que custa iniciar a JVM), e você os anotou no diário de bordo.
- [ ] Com o semáforo sem freio, o teste do limite fica vermelho com `expected: <2> but was: <8>`.

## Resumo

Responda sem olhar a lição:

1. O que é uma thread portadora, e o que a JVM faz quando uma thread virtual chega a uma operação que bloqueia?
2. Que diferença há entre dar “escala” e dar “velocidade”? Para que classe de tarefa servem as threads virtuais e para qual não?
3. Por que uma thread virtual por tarefa e não um *pool*? O que se usa para limitar quantas tarefas avançam ao mesmo tempo?
4. Em que ordem o método `invokeAll` devolve os `Future`, e por que isso importa para o relatório?
5. O que contém uma `ExecutionException` e por que é preciso olhar a sua causa? O que o `revisor` decide fazer com ela e por quê?
6. Por que o `release()` de um semáforo vai em um `finally`? O que acontece se não for?
7. Por que `contador++` perde somas quando várias threads o executam? Que três maneiras de corrigir você conhece e qual é a preferida?
8. Por que o `revisor` não tem condições de corrida embora rode dez tarefas ao mesmo tempo?
9. O que `ScopedValue` tem que `ThreadLocal` não tenha, e por que uma thread criada à mão dentro do bloco não vê o valor?
10. Por que a concorrência estruturada fica como “o que vem aí” neste curso?

**Para comparar com outras linguagens.** Se você vem de Go, a tarefa com uma thread virtual por serviço é o equivalente a lançar uma *goroutine* por serviço e esperar com um [`sync.WaitGroup`](https://pkg.go.dev/sync#WaitGroup); a diferença é que em Java os resultados voltam em `Future` e em ordem, e não por um canal. Se você vem de TypeScript, [`Promise.all`](https://developer.mozilla.org/pt-BR/docs/Web/JavaScript/Reference/Global_Objects/Promise/all) é a comparação natural, com uma diferença que importa: `Promise.all` rejeita assim que uma promessa falha, enquanto `invokeAll` espera sempre todas. E em Python, [`asyncio.TaskGroup`](https://docs.python.org/3/library/asyncio-task.html#task-groups) cancela as demais tarefas se uma falha, que é justamente o que a concorrência estruturada de 9.7 promete. Os cursos de [Go](https://www.habil.mx/pt/cursos/go/), [TypeScript](https://www.habil.mx/pt/cursos/typescript/) e Python resolvem este mesmo `revisor` com as suas ferramentas. E se você se interessa por quando convém consultar online, como faz o `revisor`, e quando convém uma fila ou um evento, o artigo [estilos de integração: online, fila ou evento](https://www.habil.mx/pt/blog/estilos-de-integracao-online-fila-evento/) o discute com os seus custos.

## Para ler mais

- [Oracle, “Virtual Threads” (guia do JDK 25)](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html): o que são, para que servem, por que não se agrupam e por que um semáforo limita melhor que um *pool*. Em inglês. Consultado em 7 de outubro de 2026.
- [JEP 444, “Virtual Threads”](https://openjdk.org/jeps/444) e [JEP 491, “Synchronize Virtual Threads without Pinning”](https://openjdk.org/jeps/491): a especificação das threads virtuais e a correção do *pinning* no Java 24. Consultado em 7 de outubro de 2026.
- [`ExecutorService`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ExecutorService.html) e [`ConcurrentHashMap`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) na API do Java 25: o contrato exato de `invokeAll`, `close` e `merge`. Consultado em 7 de outubro de 2026.
- [JEP 506, “Scoped Values”](https://openjdk.org/jeps/506) e [JEP 505, “Structured Concurrency (Fifth Preview)”](https://openjdk.org/jeps/505): os valores com escopo (final no 25) e a concorrência estruturada (ainda em versão prévia). Consultado em 7 de outubro de 2026.

---

**Anterior:** [Lição 8 — HTTP e JSON](08-http-y-json.md) ·
**Próxima:** [Lição 10 — O programa terminado](10-el-programa-terminado.md)
