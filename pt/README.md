# Curso de Java — da linguagem moderna a um programa que verifica muitos serviços ao mesmo tempo

**Por Dorian Chávez, fundador da Hábil e arquiteto de integração.**

**Para quem é:** quem já programa em outra linguagem —Go, TypeScript, Python ou qualquer outra— e quer aprender o Java de hoje. É um curso de nível intermediário: não se explica o que é uma variável nem um laço, explica-se o que o Java faz de diferente e por que isso existe. Cada conceito novo é explicado quando aparece. Se você nunca programou, comece pelo [curso de Python](https://github.com/HabilMX/curso-python) ou pelo de [Go](https://www.habil.mx/pt/cursos/go/).

**O que você vai saber ao terminar:** escrever um programa completo em Java 25, com o modelo de dados expresso em tipos, testes e concorrência, entender o que escreveu e conseguir explicá-lo a outra pessoa. E ficar com o critério para ler a documentação da linguagem por conta própria.

**O que você precisa antes de começar:** um computador com Linux Mint, saber abrir um terminal e ter escrito programas com funções, condicionais e laços em qualquer linguagem. A [Lição 1](01-instalacion.md) instala o JDK do zero, e a [Lição 6](06-proyecto-maven.md), o Maven.

## O projeto que você vai construir

O **`revisor`**: um programa que lê uma lista de serviços, consulta **todos ao mesmo tempo** por HTTP, mede quanto cada um demora, isola a falha de cada serviço e entrega um relatório sempre na mesma ordem. No final, ele o expõe por um pequeno servidor e o empacota em um JAR. É o mesmo problema dos cursos de Go, Rust e TypeScript, resolvido agora em Java.

    $ java -jar revisor.jar --config servicios.json
    SERVICIO    ESTADO    TIEMPO  DETALLE
    catalogo    ARRIBA     182ms  200
    inventario  LENTO       2.3s  200
    pagos       ARRIBA     132ms  200
    reportes    CAIDO          —  sin conexión

    4 servicios · 2 arriba · 1 lento · 1 caído · promedio 883ms

(Uma execução real do programa terminado contra o servidor de prática do curso, em 13 de outubro de 2026; os tempos mudam a cada execução.)

Parece pequeno e não é: para escrevê-lo bem você precisa de tipos selados, coleções, exceções, um projeto Maven com testes, o cliente HTTP, JSON, threads virtuais e um pacote que possa ser entregue.

## As onze lições

| Lição | | O que você constrói | O que você aprende |
|---|---|---|---|
| 0 | [O que é Java e o que NÃO é](00-que-es-java.md) | nada ainda (leitura) | código-fonte, *bytecode* e JVM; JDK e JRE; o que é LTS e por que a 25; o que o compilador protege e o que não |
| 1 | [Instalar o Java 25 no seu Linux Mint](01-instalacion.md) | o ambiente e o seu primeiro programa | JDK, `java --version`; `void main()` e `IO.println`; `java Hola.java`; `jshell`; `javac`; o primeiro erro |
| 2 | [Tipos, strings e controle de fluxo](02-tipos-cadenas-control.md) | as funções base do `revisor` | primitivos e referências; `var`; `==` contra `equals`; `&&` contra `&`; blocos de texto; `switch` como expressão; `null` |
| 3 | [Records, enums e tipos selados](03-records-enums-sellados.md) | o modelo `Servicio` / `Estado` | `record`; imutabilidade; `enum`; `sealed`; `switch` com padrões exaustivo; padrões de record |
| 4 | [Coleções, genéricos e streams](04-colecciones-genericos-streams.md) | a lista de serviços e o relatório | `List`, `Map`, `Set`; coleções imutáveis; genéricos; `Comparator`; *streams*; `Optional` |
| 5 | [Erros e exceções](05-errores-excepciones.md) | o `revisor` que não cai | verificadas e não verificadas; `try-with-resources`; exceções próprias; resultado contra exceção |
| 6 | [Projeto com Maven](06-proyecto-maven.md) | o projeto de verdade: compila, empacota e roda | pacotes e visibilidade; instalar o Maven; estrutura do Maven; `pom.xml`; o ciclo de vida; o que há em um `.jar` |
| 7 | [Testes com JUnit](07-pruebas-junit.md) | os primeiros 18 testes do `revisor` | JUnit 6; `@Test`; testes com tabela de casos; `assertThrows` e `@TempDir`; vê-lo em vermelho; ler o relatório |
| 8 | [HTTP e JSON](08-http-y-json.md) | uma requisição real e a configuração | `HttpClient`; tempos limite; Jackson 3; validar dados de fora; SSRF |
| 9 | [Concorrência: verificar tudo ao mesmo tempo](09-concurrencia.md) | o `revisor` concorrente | threads virtuais; uma thread por tarefa; `invokeAll`; `Semaphore`; `ConcurrentHashMap`; uma condição de corrida medida |
| 10 | [O programa terminado](10-el-programa-terminado.md) | o `revisor` completo, pronto para entregar | linha de comando e código de saída; servidor do JDK; encerramento ordenado; qualidade; JAR executável |

No final de cada lição há exercícios com suas soluções, e uma seção de fontes com os links do que foi afirmado. E o [diário de bordo](bitacora.md) é seu: anote ali o que foi difícil para você.

## Três critérios que atravessam todo o curso

**Os erros são ensinados pelo que custam.** Os estudos com dados de centenas de milhares de estudantes de Java mostram que os docentes erram ao adivinhar quais erros são os difíceis. Os que mais demoram para ser corrigidos são confundir `==` com `equals`, `&` com `&&` e não usar o valor que um método devolve. Esses aparecem quando chega a hora, e cada lição provoca de propósito o erro que você vai ver.

**Os testes e a segurança não são lições, são costumes.** Há testes desde que o projeto pode executá-los (lição 7, logo depois de convertê-lo em um projeto Maven), e um teste não conta até que você o tenha visto falhar. A segurança aparece onde aparece o risco do `revisor`: o endereço que alguém escreve em um arquivo, os dados de fora que são validados, a serialização nativa que não se usa com dados externos.

**Só se ensina o que já é definitivo no Java 25.** O que ainda está em versão prévia aparece em um quadro “o que vem aí”, com a flag que exige, e diz-se que não deve ser usado em produção. Um curso que ensina o que acabou de sair envelhece em seis meses.

**Para continuar lendo no blog da Hábil:** [quando vale a pena (e quando não) compilar Java para nativo](https://www.habil.mx/pt/blog/java-nativo-quando-sim-quando-nao/), [testes que detectam erros](https://www.habil.mx/pt/blog/testes-que-detectam-erros-cobertura-quality-gate/) e [segurança de APIs](https://www.habil.mx/pt/blog/seguranca-de-apis-cto-identidade-gateway-tokens/). E se faltar a base de outra linguagem, o [curso de Python](https://github.com/HabilMX/curso-python) ou o de [TypeScript](https://www.habil.mx/pt/cursos/typescript/).

## Como você sabe que terminou

O curso não termina quando você leu a lição 10, mas quando o seu `revisor` cumpre estas seis coisas:

1. **`mvn -B verify` termina em verde e o relatório diz quantos testes rodaram.** Conte-os pelo relatório, não do código de saída: um verde com zero testes se parece com um verde de verdade.
2. **Verifica 20 serviços ao mesmo tempo em um tempo próximo ao do mais lento**, não ao da soma de todos. Você mede isso, com o relógio, em série e em paralelo.
3. **Um serviço fora do ar, lento ou que responde lixo não derruba o relatório:** aparece na sua linha com o seu erro, e o resto sai completo e na mesma ordem de sempre.
4. **Nenhuma requisição pode esperar para sempre:** todas têm tempo de conexão e tempo limite.
5. **Rejeita os endereços que não são `http` ou `https` e os que não estão na lista permitida**, em vez de consultar o que escreverem no arquivo.
6. **Roda com `java -jar revisor.jar` a partir de outra pasta e sai com código 0 se tudo está no ar e 1 se não.**
