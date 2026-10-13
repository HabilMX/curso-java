# Lição 7 — Testes com JUnit

**Tempo:** 90 min (justos: cada teste aparece primeiro em vermelho e depois em verde, e isso leva tempo; não pule)

**O que você constrói:** o `revisor` ganha 18 testes automatizados que rodam com um único comando e vigiam as suas regras: a classificação, o formato do tempo, a leitura de `servicios.txt` e o resumo

**O que você aprende:** o que é um teste automatizado e os seus três tempos; JUnit 6 no `pom.xml`, com o seu BOM e no escopo `test`; `@Test` e `assertEquals`; `@ParameterizedTest` com `@CsvSource` (a tabela de casos) e por que os casos são escolhidos na fronteira; `assertThrows` e `@TempDir`; ver um teste em vermelho antes de vê-lo em verde; e que **o número de testes se lê do relatório, não do código de saída**

**De onde você vem.** Você traz o projeto Maven da [Lição 6](06-proyecto-maven.md): treze arquivos no pacote `mx.habil.revisor`, um `pom.xml` com três *plugins* de versão fixa, e `mvn -B verify`, que compila, empacota `revisor-0.6.0.jar` e termina em `BUILD SUCCESS`. No meio dessa saída havia uma linha que dizia `No tests to run.`: o seu projeto já tem onde colocar testes (a pasta `src/test/java`, vazia) e quem os rode (o *plugin* *Surefire*), mas nenhum teste. E isso deixa uma pergunta incômoda: como você sabe que o `revisor` funciona? Você o executou e olhou a tabela. **O único teste que o seu programa tem é você, olhando uma tela.** Hoje isso muda: testes que verificam por você, em uma fração de segundo e a cada alteração, que cada regra continua fazendo o que diz. Você trabalha na sua pasta `~/curso-java/revisor/`.

**O que esta lição não faz.** Não testa `Revision`, a classe que simula a rede esperando com `Thread.sleep`: levaria o mesmo tempo que o programa, e o que ela vigia (o `Simulador`) desaparece na [Lição 8](08-http-y-json.md), que testa `revisar` contra um servidor de verdade iniciado dentro do próprio teste. Também não usa bibliotecas para simular objetos nem mede a cobertura dos testes: com funções pequenas e dados fixos, não são necessárias. E não acrescenta nada ao programa: o JUnit vive apenas nos testes, e o `.jar` entregue não muda de conteúdo.

**Sobre as execuções.** Tudo nesta lição foi executado com Temurin 25 e Maven 3.10.0, no macOS, com o terminal em UTF-8. As saídas do Maven aparecem abreviadas: conserva-se o que cada seção analisa, e as reticências (`...`) marcam o que foi removido. Dois números dessas saídas serão diferentes na sua tela e não importa: os `Time elapsed` (mudam a cada execução) e, nos testes que falham, o número da linha do stack trace (`ResumenTest.java:32`), que é o do seu arquivo e só coincide se você o escreveu igual à lição, linha por linha. Todo o resto, incluindo as contagens de testes, deve coincidir. Os programas estão em [`programas/07-pruebas-junit/`](../programas/07-pruebas-junit), no repositório do curso.

## Ao terminar, você vai conseguir

- Explicar o que é um teste automatizado, quais são os seus três tempos e o que um *framework* de testes como o JUnit acrescenta.
- Declarar o JUnit 6 no `pom.xml` com o seu BOM e no escopo `test`, e explicar por que ele não faz parte do programa.
- Escrever testes com `@Test` e `assertEquals`, com o valor esperado primeiro, e uma tabela de casos com `@ParameterizedTest` e `@CsvSource`, escolhendo os casos em torno da fronteira.
- Testar um erro com `assertThrows` e um arquivo com `@TempDir`.
- Ver um teste em vermelho antes de vê-lo em verde, e saber o que reverter para provocá-lo.
- Rodar todos os testes, uma classe ou um único teste, e ler no relatório quantos foram executados.
- Explicar três formas de obter um verde que não testou nada, e como você se protege de cada uma.

## O porquê antes do como

É segunda-feira e alguém pede que o limite de “lento” passe de um segundo para 800 milissegundos. É uma mudança de uma linha: `UMBRAL_LENTO_MS`. Você a faz, roda o `revisor`, olha a tabela… e tudo parece bem. Está mesmo? Com quatro serviços de dados fixos, `inventario` continuava lento e os demais continuavam no ar, então a tabela fica igual à de ontem. Mas o que aconteceu com um serviço que responde em exatamente 800 milissegundos? Conta como lento ou não? A tabela de hoje não sabe: nenhum serviço dos seus dados mede 800. Se você errou por um `>` onde ia um `>=`, o erro espera que um serviço real caia justo na fronteira, e nesse dia quem o descobrirá será quem usa o seu programa, e não você.

Olhar a tabela tem três defeitos, e os três pioram com o tempo. **Só vê os dados que você tem**, e os erros vivem nos que você não tem (os 800 exatos, o arquivo com uma linha mal escrita, o caso em que ninguém responde). **Depende de alguém olhar**, com atenção, toda vez, e ninguém olha com atenção a vigésima vez. E **não deixa registro**: se amanhã outra pessoa mudar o formato do tempo, não há nada que avise que ela quebrou o que você verificou ontem. Um teste automatizado corrige as três coisas de uma vez: você escreve o caso uma vez, ele o verifica a cada alteração sem se cansar, e fica no projeto para a próxima pessoa.

A lição tem seis partes: **o que é um teste e como o JUnit entra no projeto** (7.1); **o primeiro teste**, e vê-lo em vermelho (7.2); **a tabela de casos** (7.3); **testar um arquivo e o seu erro** (7.4); **ler o relatório**, que é a parte mais negligenciada (7.5); e **o passo do projeto** (7.6). Com esse destino em mente, isto é o que você verá no final da lição, onde a Lição 6 dizia `No tests to run.`:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
```

A linha que importa é `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. Ao terminar você saberá de onde sai cada um desses números, o que acontece quando algum deles se move, e por que nenhum deles se lê do “sucesso” que o Maven anuncia no final.

## Os conceitos

### 7.1 O que é um teste automatizado e o que é preciso

#### 7.1.1 Os três tempos de um teste

Volte à segunda-feira do início: o limite passou para 800 milissegundos e ninguém sabe se um serviço de exatamente 800 conta como lento. Um **teste automatizado** é a forma de responder a essa pergunta sem esperar que aconteça em produção: um pequeno programa que prepara um dado conhecido, chama a parte do programa que se quer vigiar e compara o que obteve com o que deveria obter. Tem sempre três tempos, que em inglês se abreviam AAA: **organizar** (*arrange*) os dados de entrada, **agir** (*act*) chamando o código e **afirmar** (*assert*) que o resultado é o esperado. Se a asserção se cumpre, o teste passa; se não, falha e diz o que esperava e o que obteve.

Escrever esse programa pequeno não é difícil. O que dá trabalho é todo o resto: encontrar todos os testes do projeto, rodá-los um por um sem que a falha de um interrompa os outros, e informar quantos passaram. Isso é feito por um ***framework* de testes**, e o do Java é o **JUnit**, que está na sua versão 6 (a 6.1.3 é a vigente em outubro de 2026; requer Java 17 ou superior, segundo o seu [guia do usuário](https://docs.junit.org/current/user-guide/)). Ele coloca quatro coisas à sua disposição: anotações como `@Test` para marcar quais métodos são testes, métodos como `assertEquals` para afirmar, um motor que descobre e executa os testes, e um relatório do que aconteceu. Uma **anotação** é um rótulo que se escreve com `@` antes de uma declaração e que as ferramentas leem; não muda o que o método faz, apenas diz ao JUnit como tratá-lo.

#### 7.1.2 O JUnit no `pom.xml`

Para tê-lo no projeto, é preciso declarar a dependência no `pom.xml`. Este é o arquivo completo, já com o JUnit. A parte nova está em `<dependencyManagement>` e `<dependencies>`, e a versão do projeto sobe para `0.7.0`, como prometeu a Lição 6: o `.jar` se chamará `revisor-0.7.0.jar`.

```xml fig07_01/pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>mx.habil</groupId>
  <artifactId>revisor</artifactId>
  <version>0.7.0</version>

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
    </plugins>
  </build>
</project>
```

O JUnit não é um único artefato, mas uma família deles (a API para escrever testes, o motor que os executa, o suporte para tabelas de casos…) e todos devem ter a mesma versão para funcionar juntos. Para não escrever essa versão seis vezes, e não errar em nenhuma, o Maven oferece o **BOM** (*bill of materials*, “lista de materiais”): um `pom.xml` que não contém código, apenas uma lista de versões compatíveis. O bloco `<dependencyManagement>` com `<type>pom</type>` e `<scope>import</scope>` importa essa lista completa: a partir daí, qualquer artefato do JUnit se declara **sem versão** e assume a que o BOM fixa. A dependência em si é uma só, `junit-jupiter`, um artefato “guarda-chuva” que traz a API, o suporte para tabelas de casos e o motor. É a forma que o [guia do JUnit para Maven](https://docs.junit.org/current/running-tests/build-support.html) recomenda para alinhar as versões.

A linha que mais importa é `<scope>test</scope>`. O **escopo** (*scope*) de uma dependência diz onde ela está disponível: a de escopo `test` só existe quando se compilam e se rodam os testes, e **não** faz parte do programa. Quem executar o `revisor` não precisa do JUnit, e não é obrigado a baixá-lo. Como o compilador agora tem o JUnit em `src/test/java` e não em `src/main/java`, o `import org.junit...` que falhou com o lançador na Lição 6 (a figura `fig06_01`) funciona nos testes e continuaria falhando no programa, que é exatamente o que se quer.

#### 7.1.3 O que se testa e o que não se testa

Antes de escrever o primeiro, uma decisão que vale para todo o curso: **quais partes do `revisor` merecem um teste hoje**. A resposta sai de olhar o que cada classe faz. `Clasificador`, `Formato` e `Resumen` são **regras**: recebem um dado e devolvem outro, sempre o mesmo para a mesma entrada, sem tocar na rede, no disco nem no relógio. São as mais fáceis de testar e as mais valiosas, porque ali vivem as decisões que alguém vai querer mudar numa segunda-feira (o limite, o formato do tempo, como se conta uma média). `Configuracion` é uma regra com uma borda voltada para fora: lê um arquivo, e o que há para vigiar são as suas mensagens de erro, que são a única coisa que vê quem se engana ao escrever `servicios.txt`. As quatro têm teste nesta lição.

`Revision` e `Simulador` não o têm, e a razão não é preguiça. `Simulador` é um andaime: inventa quatro respostas para que o programa tenha algo para verificar enquanto não há rede, e na Lição 8 desaparece. Um teste de um andaime verifica que o andaime diz o que você escreveu nele, e é jogado fora junto com ele. `Revision.revisar`, por sua vez, **espera** de verdade o que cada serviço diz (`Thread.sleep`): um teste dos quatro serviços levaria o mesmo tempo que o programa, uns 2.5 segundos, e os 18 testes desta lição rodam completos em uma fração de segundo. Essa velocidade não é um luxo: é o que faz você rodá-los a cada alteração, em vez de “no final”. Quando `revisar` fizer uma requisição real, na Lição 8, terá o seu próprio teste contra um servidor iniciado dentro do próprio teste, que responde o que o teste quer e quando quer.

Há uma terceira razão para começar pelas regras, que se nota na forma dos testes que vêm a seguir: as quatro classes de teste vivem no **mesmo pacote** que o que testam, `mx.habil.revisor`. Graças a isso, elas enxergam as classes de visibilidade de pacote que a Lição 6 deixou fechadas para o mundo, e não foi preciso abrir nada para poder testá-las. Os testes usam o programa como o usaria outra classe do pacote, e nada mais.

### 7.2 O primeiro teste: o resumo

#### 7.2.1 `ResumenTest`, linha por linha

Comecemos pelo mais simples do projeto. O resumo de uma verificação conta serviços, e a sua média inclui apenas os que responderam. Dois testes o vigiam: os quatro serviços de sempre (a média tem que ser 843), e o caso extremo de **ninguém** responder, em que a média não existe e o resumo escreve um traço.

```java
// fig07_01/src/test/java/mx/habil/revisor/ResumenTest.java

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

Lê-se de cima para baixo. A classe vive no mesmo pacote que o que ela testa e se chama `ResumenTest`: o nome termina em `Test` porque é assim que o *Surefire*, o *plugin* que roda os testes, o procura (por padrão inclui os arquivos que começam com `Test` ou terminam em `Test`, `Tests` ou `TestCase`, segundo a sua [documentação](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html); em 7.5 você verá o que acontece com um teste que não se chama assim). A classe não é `public`: o JUnit 6 não exige, e, pela regra da Lição 6 (6.1.3), fica com a visibilidade mais estreita.

O `import static` é novo. Um `import` normal deixa você usar uma **classe** pelo seu nome curto; um `import static` deixa você usar um **método estático** de uma classe sem escrever a classe na frente. Com `import static org.junit.jupiter.api.Assertions.assertEquals;` você pode escrever `assertEquals(...)` em vez de `Assertions.assertEquals(...)`. Os testes o usam em toda parte, então vira costume.

`private static Resultado resultado(String nombre, Respuesta respuesta)` é um **auxiliar**: uma função que os dois testes compartilham para montar um `Resultado` sem repetir três linhas a cada vez. É privado porque só interessa a esta classe. E cada método com `@Test` é um teste: tem um nome que **diz o que se afirma** (`sinNingunaRespuestaElPromedioEsUnaRaya`, não `prueba2`), não recebe parâmetros nem devolve nada, e respeita os três tempos separados por linhas em branco: monta-se a lista, chama-se `Formato.resumen(Resumen.de(resultados))`, e afirma-se. Em `assertEquals(esperado, actual)` **o valor esperado vem primeiro**; se você os inverter, o teste passa e falha do mesmo jeito, mas a mensagem de erro dirá o contrário do que ocorreu.

A partir da raiz do projeto, com `-Dtest=ResumenTest` para rodar só esta classe:

```text
$ mvn -B test -Dtest=ResumenTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.024 s -- in mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`. Dois testes, nenhum falhou. Você já sabe que passam. Falta saber se **podem falhar**.

#### 7.2.2 Vê-lo em vermelho antes de vê-lo em verde

Um teste que você nunca viu falhar não demonstra nada. Pode estar comparando a coisa errada, ou comparando algo que sempre é verdade, e o seu verde seria um enfeite. A única forma de saber que um teste vigia o que diz vigiar é **quebrar de propósito o código que ele vigia e verificar que fica vermelho**. Faça isso agora com os do resumo. Em `Resumen.java`, a média é calculada só com os serviços que responderam: disso se encarrega o `filter(OptionalLong::isPresent)`. Substitua essas duas linhas (`.filter(OptionalLong::isPresent)` e `.mapToLong(OptionalLong::getAsLong)`) por uma só, `.mapToLong(t -> t.orElse(0))`: agora o serviço que não respondeu conta como **zero milissegundo**, e rode de novo:

```text
$ mvn -B test -Dtest=ResumenTest
[INFO] Running mx.habil.revisor.ResumenTest
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0, Time elapsed: 0.027 s <<< FAILURE! -- in mx.habil.revisor.ResumenTest
[ERROR] mx.habil.revisor.ResumenTest.resumeLosCuatroServiciosCanonicos -- Time elapsed: 0.016 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms> but was: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 632ms>
	at mx.habil.revisor.ResumenTest.resumeLosCuatroServiciosCanonicos(ResumenTest.java:32)
[ERROR] mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya -- Time elapsed: 0.003 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio 0ms>
	at mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya(ResumenTest.java:41)
[ERROR]   ResumenTest.resumeLosCuatroServiciosCanonicos:32 expected: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 843ms> but was: <4 servicios · 2 arriba · 1 lento · 1 caído · promedio 632ms>
[ERROR]   ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya:41 expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio 0ms>
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Agora sim há mensagens, e são das boas. `Failures: 2`: os dois testes falharam, cada um por uma consequência diferente do mesmo erro. O primeiro diz `expected: <… promedio 843ms> but was: <… promedio 632ms>` (“promedio” é “média”): esperava 843, obteve 632; e 632 é `2529 / 4`, a soma dos tempos dividida pelos quatro serviços em vez de pelos três que responderam. O segundo diz `expected: <… promedio —> but was: <… promedio 0ms>`: quando ninguém responde, o resumo deve dizer que não há média, e a alteração a transforma em “zero milissegundo”, um número que parece dado e não é. A linha que começa com `at` é o stack trace: diz em que arquivo e em que linha do teste estava a asserção que falhou (`ResumenTest.java:32`), que é a primeira coisa que você abre. Com essas duas mensagens em mãos você sabe onde procurar, sem abrir o depurador. (Dois testes que caem pela mesma causa também contam algo: esse defeito se notava de dois ângulos, e o segundo teste existe justamente por isso.)

Desfaça a alteração e confirme que volta ao verde. Este exercício de **reverter e ver o vermelho** é o hábito que sai mais barato de todo o curso: leva um minuto e diz se os seus testes valem alguma coisa. No curso ele se repetirá cada vez que se escrever um teste novo.

### 7.3 A tabela de casos: `@ParameterizedTest` e `@CsvSource`

#### 7.3.1 Um teste, muitas linhas

A classificação de um serviço é uma regra com uma fronteira: mil milissegundos ou mais é lento. Para vigiá-la bem é preciso testar vários casos, e todos têm a mesma forma (entra um código e um tempo, sai um estado). Escrever seis métodos quase idênticos é um ruído que esconde o que é interessante. O JUnit tem uma forma melhor, o **teste parametrizado**: escreve-se o método uma vez, com parâmetros, e uma **tabela de casos** com os valores:

```java
// fig07_01/src/test/java/mx/habil/revisor/ClasificadorTest.java

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

`@ParameterizedTest` substitui `@Test` e diz ao JUnit que este método será executado várias vezes. `@CsvSource` é a tabela: cada texto da lista é uma **linha**, e as vírgulas separam as **colunas** (CSV vem de *comma-separated values*, “valores separados por vírgulas”). O JUnit pega cada linha, converte o texto de cada coluna para o tipo do parâmetro que lhe corresponde —`int`, `long`, e até `Estado`, que reconhece pelo nome da constante— e chama o método uma vez por linha. São seis chamadas, então o relatório contará **seis testes** desta única declaração, mais o sétimo: o de `SinRespuesta`, que vai como `@Test` à parte porque não tem a mesma forma (não há código nem tempo).

#### 7.3.2 Os casos são escolhidos na fronteira

O teste do formato do tempo é a mesma ideia, com uma coluna de entrada e uma de texto esperado. As suas quatro linhas são as que importam em torno da mudança de unidade: 87 e 999 são escritos em milissegundos, 1000 e 2300 em segundos.

```java
// fig07_01/src/test/java/mx/habil/revisor/FormatoTest.java

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

Os casos não foram escolhidos ao acaso. Foram escolhidos **em torno da fronteira**: 142 é um serviço normal; 999 é o último valor que continua no ar; 1000 é o primeiro que é lento (o caso que importa mais que todos: é o que detecta um `>` onde ia um `>=`); 2300 é claramente lento; 503 e 404 são erros do servidor e do cliente que, não importa quão rápido respondam, deixam o serviço fora do ar. Os **valores de fronteira** são onde se escondem os erros de um a mais ou um a menos, e uma tabela que não os inclua deixa a borda sem vigilância. Comprove da forma que você já conhece: em `Clasificador.java`, troque `tiempoMs >= UMBRAL_LENTO_MS` por `tiempoMs > UMBRAL_LENTO_MS` e rode o projeto todo:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.058 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[3] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <LENTO> but was: <ARRIBA>
	at mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(ClasificadorTest.java:26)

[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[ERROR] Failures: 
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:26 expected: <LENTO> but was: <ARRIBA>
[ERROR] Tests run: 18, Failures: 1, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[ERROR] BUILD FAILURE
```

Leia com calma, porque é o relatório de uma falha real e tem quatro coisas que convém reconhecer à primeira vista. Primeiro, **qual classe falhou**: `ClasificadorTest`, com `Tests run: 7, Failures: 1`; as outras três classes continuam em verde: a falha de uma não interrompe as demais. Segundo, **qual teste**: `clasificaUnaRespuesta(int, long, Estado)[3]`, onde `[3]` é o **número da linha da tabela**: a terceira, `200, 1000, LENTO`. Terceiro, **o que aconteceu**: `expected: <LENTO> but was: <ARRIBA>`. O serviço de 1000 milissegundos devia ser lento e saiu no ar, porque com `>` o 1000 já não conta. E quarto, a linha de fechamento: `Tests run: 18, Failures: 1` e `BUILD FAILURE`, com o código de saída diferente de zero que faz qualquer servidor de integração ficar vermelho. Uma única alteração de um caractere, uma única linha em vermelho, e o relatório diz qual: isso é o que a tabela oferece. Reverta a alteração antes de continuar.

#### 7.3.3 Rodar um único teste

Quando uma linha fica vermelha, o que você quer é repetir **aquele** teste enquanto o conserta, não os 18. Você já usou `-Dtest=ResumenTest` para rodar uma classe; com um `#` e o nome do método você escolhe um único teste da classe:

```text
$ mvn -B test -Dtest=ClasificadorTest#sinRespuestaEsCaido
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.017 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

`Tests run: 1`: dos sete testes da classe, rodou só o que você pediu. Repare também na fase: `mvn -B test` e não `verify`. Como `test` vem antes de `package` no ciclo de vida, o Maven para depois dos testes e não monta o `.jar`, que para repetir um teste não é necessário. É a forma de trabalhar enquanto você conserta algo: um teste, rápido, muitas vezes. E é também uma armadilha se você esquecer que a usou: um `Tests run: 1` em verde não diz nada sobre os outros 17. **Antes de dar algo por terminado, roda-se tudo**, com `mvn -B verify` e sem `-Dtest`, e lê-se o total.

### 7.4 `assertThrows` e `@TempDir`: testar um arquivo e o seu erro

A terceira família de testes vigia o que o `revisor` faz com o arquivo de configuração. Aqui há duas coisas novas. A primeira é que testar um arquivo exige ter um arquivo, e não se quer escrever na sua pasta de verdade nem deixar lixo. A segunda é que o que se quer afirmar não é um valor devolvido, mas uma **exceção**: que certos arquivos façam a leitura falhar, e com a mensagem exata.

```java
// fig07_01/src/test/java/mx/habil/revisor/ConfiguracionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de la lectura de servicios.txt. */
class ConfiguracionTest {

  @TempDir Path carpeta;

  @Test
  void leeUnArchivoValido() throws Exception {
    Path ruta = carpeta.resolve("servicios.txt");
    Files.writeString(
        ruta,
        """
        # nombre  url  [timeout_ms]
        catalogo   https://catalogo.example
        pagos      https://pagos.example  1000
        """);

    List<Servicio> servicios = Configuracion.leer(ruta);

    assertEquals(2, servicios.size());
    assertEquals("catalogo", servicios.get(0).nombre());
    assertEquals(Servicio.TIMEOUT_POR_OMISION_MS, servicios.get(0).timeoutMs());
    assertEquals(1000, servicios.get(1).timeoutMs());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          a https://a.x;b https://b.x;solo | renglón 3: se esperaban 2 o 3 campos y hay 1
          a https://a.x;b ftp://b.x | renglón 2: la url debe empezar con http:// o https://
          a https://a.x;b https://b.x 0 | renglón 2: timeout_ms debe ser un entero mayor que 0
          catalogo https://a.x;catalogo https://b.x | nombres repetidos: catalogo
          """)
  void rechazaUnArchivoInvalido(String contenido, String mensajeEsperado) throws IOException {
    Path ruta = carpeta.resolve("servicios.txt");
    Files.writeString(ruta, contenido.replace(";", "\n"));

    ErrorDeConfiguracion error =
        assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta));

    assertEquals(mensajeEsperado, error.getMessage());
  }
}
```

**`@TempDir Path carpeta`** pede ao JUnit uma pasta temporária: antes de cada teste a cria nova e vazia, e depois a apaga com tudo o que contenha. Os testes escrevem ali o seu `servicios.txt` com `Files.writeString` (a Lição 5 lia arquivos com `Files`; esta é a outra metade, escrevê-los) e não atrapalham uns aos outros nem sujam o disco.

**`assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta))`** executa o que você lhe dá (uma expressão escrita como `() -> ...`: uma *lambda*, a mesma forma que você usou nos *streams* da Lição 4) e verifica que lance **essa** exceção. Se não lançar nada, ou lançar outra, o teste falha; se lançar a esperada, `assertThrows` a devolve para que você a examine: o teste compara o seu `getMessage()` com a mensagem exata da tabela. Repare por que se afirma a mensagem e não só o tipo: esse texto é a única coisa que a pessoa que edita `servicios.txt` vê quando se engana; é o contrato com ela. Se alguém o reescrever sem perceber, o teste detecta.

A tabela de casos desta classe usa uma variante de `@CsvSource` que aceita um bloco de texto (`textBlock`), com o delimitador `|` em vez da vírgula para que cada linha se pareça com uma tabela. Há uma única complicação: cada linha da tabela ocupa uma linha de texto, mas um arquivo de configuração tem várias. Para isso a primeira coluna escreve as linhas do arquivo separadas por `;` (`a https://a.x;b ftp://b.x` são duas linhas), e o teste as converte em quebras de linha reais antes de escrever o arquivo. As quatro linhas são quatro dos motivos de rejeição que a Lição 5 definiu (os mais comuns): faltam campos, o endereço não é `http` nem `https`, o tempo limite não é maior que zero, e há nomes repetidos. O quinto teste da classe, `leeUnArchivoValido`, é o caso feliz: dois serviços, um com o tempo limite padrão e outro com 1000. **Sempre é preciso um:** uma classe que só testa rejeições poderia passar com um leitor que rejeita tudo.

### 7.5 Ler o relatório: a contagem de testes

#### 7.5.1 Onde está o número, e onde não está

Rode o projeto inteiro, completo, com o comando de sempre:

```text
$ mvn -B verify
...
[INFO] Running mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.052 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Running mx.habil.revisor.ResumenTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in mx.habil.revisor.ResumenTest
[INFO] Running mx.habil.revisor.ConfiguracionTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
[INFO] Running mx.habil.revisor.FormatoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in mx.habil.revisor.FormatoTest
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
```

O que importa neste relatório é uma única linha: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`. Os quatro números dizem quantos testes foram **executados**, quantos **falharam** uma asserção, quantos terminaram com um **erro** inesperado (uma exceção que o teste não esperava) e quantos foram **ignorados** (estão marcados para não rodar). E os 18 saem, exatamente, do que você já sabe: `ClasificadorTest` 7 (seis linhas e a de `SinRespuesta`), `FormatoTest` 4 (quatro linhas), `ConfiguracionTest` 5 (quatro linhas e o caso feliz) e `ResumenTest` 2. Some-os: 7 + 4 + 5 + 2. Se algum dia o 18 mudar sem que você tenha acrescentado ou removido um teste, algo se moveu, e é uma pergunta que vale a pena responder.

Cada classe de testes deixa também o seu próprio relatório na pasta `target/surefire-reports`, em dois formatos:

```text
$ ls target/surefire-reports
TEST-mx.habil.revisor.ClasificadorTest.xml
TEST-mx.habil.revisor.ConfiguracionTest.xml
TEST-mx.habil.revisor.FormatoTest.xml
TEST-mx.habil.revisor.ResumenTest.xml
mx.habil.revisor.ClasificadorTest.txt
mx.habil.revisor.ConfiguracionTest.txt
mx.habil.revisor.FormatoTest.txt
mx.habil.revisor.ResumenTest.txt
```

Os `.txt` são para você ler, e os `TEST-*.xml` são para as máquinas: o servidor de integração contínua, o editor, ou um script seu leem o XML para contar. Um relatório de texto de uma classe em verde tem três linhas, o que você viu acima, mas de uma única classe:

```text
$ cat target/surefire-reports/mx.habil.revisor.ConfiguracionTest.txt
-------------------------------------------------------------------------------
Test set: mx.habil.revisor.ConfiguracionTest
-------------------------------------------------------------------------------
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in mx.habil.revisor.ConfiguracionTest
```

#### 7.5.2 Três verdes que não testaram nada

E agora, a razão pela qual esta seção existe. **O número se lê do relatório, jamais da mensagem de sucesso nem do código de saída.** Há uma forma de o Maven terminar em `BUILD SUCCESS` sem ter rodado um único teste, e é mais fácil de provocar do que parece: basta que os nomes das classes de teste não sejam os que o *Surefire* procura. Para ver isso, as quatro classes deste projeto foram renomeadas de `...Test` para `...Prueba` (por exemplo `ClasificadorPrueba`), o que em espanhol soa igual, e rodou-se o mesmo comando:

```text
$ mvn -B verify
...
[INFO] --- surefire:3.6.0:test (default-test) @ revisor ---
[INFO] --- jar:3.5.1:jar (default-jar) @ revisor ---
[INFO] Building jar: ~/curso-java/revisor/target/revisor-0.7.0.jar
[INFO] BUILD SUCCESS
$ echo $?
0
```

Não há nenhuma linha `T E S T S`, nem `Running`, nem `Tests run`. E, no entanto: `BUILD SUCCESS`, e o código de saída é `0`, o que significa “tudo bem”. O Maven não encontrou testes, não teve nada que falhar e se declarou bem-sucedido. Se o único critério fosse “terminou em verde?”, este projeto sem um único teste efetivo teria passado em qualquer revisão. Tampouco se cria a pasta `target/surefire-reports`, que é outra pista: o relatório não existe porque não houve o que relatar. Compare com o que você viu na Lição 6, quando a pasta de testes estava vazia: ali o *Surefire* dizia `No tests to run.`, um aviso honesto. Aqui há quatro classes de teste compiladas em `target/test-classes`, e como nenhuma se chama como ele procura, ele não diz nada. **O silêncio é pior que o aviso**, e por isso o número se procura ativamente: não aparecer também é um dado.

Há outras duas variantes do mesmo mecanismo, também medidas. A primeira: uma classe de testes que **se ignora sozinha**. O JUnit permite que um teste diga “se tal condição não se cumpre, não me execute” (chama-se *suposição*, `Assumptions`, e serve de verdade para testes que precisam de algo que nem sempre está lá, como uma conexão). Mas um deslize nessa condição desliga todos os testes da classe sem que nada fique vermelho. Foi acrescentada uma suposição falsa no início de `FormatoTest` para ver isso. É este fragmento, colocado dentro da classe, antes do teste que ela já tinha (com os seus dois `import`: `org.junit.jupiter.api.Assumptions` e `org.junit.jupiter.api.BeforeAll`):

```java
  @BeforeAll
  static void soloSiHayRed() {
    Assumptions.assumeTrue(false, "sin red");
  }
```

`@BeforeAll` marca um método que o JUnit roda uma vez, antes de todos os testes da classe (por isso é `static`: ainda não há nenhum objeto da classe). `assumeTrue(condición, motivo)` diz “se a condição é falsa, não continue”. Aqui a condição é `false` às claras, para que o efeito seja óbvio; em um projeto real seria algo como “há conexão com o banco de dados”, e o deslize, uma condição que nunca se cumpre na máquina de integração. O resultado:

```text
$ mvn -B verify
...
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: 0.002 s -- in mx.habil.revisor.FormatoTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
$ echo $?
0
```

O Maven imprimiu um `WARNING` (amarelo, não vermelho) e seguiu: a classe inteira ficou em `Skipped: 1`, o total baixou de 18 para 15, `BUILD SUCCESS` e código de saída `0`. Quem olha apenas o verde nunca percebe que quatro testes deixaram de vigiar o formato do tempo. A segunda variante é do terminal e não do Maven: **o código de saída de um pipe é o do último programa do pipe.** Se por comodidade você corta a saída com `| tail` e confere `$?`, o que você confere é o código do `tail`, que sempre sai bem:

```text
$ mvn -B verify | tail -2
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
0
```

Aqui o Maven falhou (esta é a execução com o `>` quebrado de 7.3.2, e o seu próprio código de saída foi 1), mas o `0` que você vê é o do `tail`. Se você realmente precisa de um pipe, o terminal tem uma opção para que o código de saída seja o do primeiro programa que falhou e não o do último:

```text
$ set -o pipefail
$ mvn -B verify | tail -1
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
$ echo $?
1
```

Com `pipefail`, o `1` do Maven sobrevive ao pipe. Vale para o terminal em que você o escreveu (ou para o script que o tenha no início), e não muda o que é importante: o mais seguro é rodar os testes **sem filtrar a saída**, e ler dali a linha `Tests run:`. Nos três casos a defesa é a mesma, e é o hábito deste curso: depois de rodar os testes, leia essa linha e compare o número com o que você espera.

Há um quarto verde, mas esse sim avisa: `mvn -B verify -DskipTests` compila os testes, não os roda, diz `Tests are skipped.` e termina em `BUILD SUCCESS`. É uma opção legítima, que se usa de propósito quando você quer apenas o `.jar` e já rodou os testes antes; convém lembrar que esse verde não significa que algo tenha sido testado, e que nunca é o comando com que se decide se uma alteração está boa.

#### 7.5.3 O número é anotado

Em um projeto que já existe, a forma de saber quantos esperar é tê-lo anotado: no diário de bordo do curso, junto à data. O de hoje diz 18. Quando você acrescentar um teste, será 19, e você o anotará. É uma maneira barata de fazer com que uma mudança inesperada na contagem —para cima ou para baixo— chame a sua atenção.

### 7.6 O passo do projeto: o `revisor` com testes

Você tem todas as peças. Este é o projeto completo, tal como fica ao terminar a lição. Em relação à Lição 6 mudam duas coisas: o `pom.xml` (versão 0.7.0 e JUnit no escopo de testes, em 7.1.2) e a pasta `src/test/java`, que deixa de estar vazia. **O código do programa não mudou nem uma linha**: os testes foram escritos para o programa que já existia. Inclui-se tudo para que o projeto possa ser montado sem olhar outra lição:

```text
revisor/
  pom.xml                 versión 0.7.0 · release 25 · JUnit 6.1.3 en alcance de pruebas
  servicios.txt           sin cambios
  src/main/java/mx/habil/revisor/                       (los trece, sin cambios)
    Revisor.java  Estado.java  Respuesta.java  Servicio.java  Resultado.java
    Clasificador.java  Formato.java  Simulador.java  Revision.java  Resumen.java
    Reporte.java  Configuracion.java  ErrorDeConfiguracion.java
  src/test/java/mx/habil/revisor/
    ClasificadorTest.java  FormatoTest.java  ConfiguracionTest.java  ResumenTest.java   NUEVOS
```

Você já viu completos o `pom.xml` e os quatro testes. O seu `servicios.txt` é o de sempre:

```text fig07_01/servicios.txt
# nombre      url                           [timeout_ms]
catalogo      https://catalogo.example
inventario    https://inventario.example
pagos         https://pagos.example
reportes      https://reportes.example
```

E estes são os treze arquivos do programa, idênticos aos da Lição 6. Se você a acompanhou, já os tem; são incluídos para que o projeto desta lição possa ser construído sem depender de mais nada.

<details>
<summary>Os treze arquivos do programa, sem mudanças em relação à Lição 6</summary>

```java
// fig07_01/src/main/java/mx/habil/revisor/Revisor.java

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

```java
// fig07_01/src/main/java/mx/habil/revisor/Estado.java

package mx.habil.revisor;

enum Estado {
  ARRIBA,
  LENTO,
  CAIDO
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Respuesta.java

package mx.habil.revisor;

sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}
```

```java
// fig07_01/src/main/java/mx/habil/revisor/Servicio.java

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
// fig07_01/src/main/java/mx/habil/revisor/Resultado.java

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
// fig07_01/src/main/java/mx/habil/revisor/Clasificador.java

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
// fig07_01/src/main/java/mx/habil/revisor/Simulador.java

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
// fig07_01/src/main/java/mx/habil/revisor/Revision.java

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
// fig07_01/src/main/java/mx/habil/revisor/Resumen.java

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
// fig07_01/src/main/java/mx/habil/revisor/Reporte.java

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
// fig07_01/src/main/java/mx/habil/revisor/Formato.java

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
// fig07_01/src/main/java/mx/habil/revisor/Configuracion.java

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
// fig07_01/src/main/java/mx/habil/revisor/ErrorDeConfiguracion.java

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

Para terminar a lição: `mvn -B verify` deve dizer `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`, e deixar `target/revisor-0.7.0.jar`; `java -cp target/classes mx.habil.revisor.Revisor` deve imprimir a tabela com resumo da Lição 4 (4.5) de 843 ms, igual à Lição 6, porque o programa não mudou. Há um artigo da Hábil sobre [quais testes pegam erros de verdade e como medir a cobertura](https://www.habil.mx/pt/blog/testes-que-detectam-erros-cobertura-quality-gate/), que complementa esta lição do lado da qualidade; e outro sobre [como se monta a integração contínua de uma casa de software](https://www.habil.mx/pt/blog/cicd-devsecops-infraestrutura-propria/), o lugar onde estes 18 testes rodam sozinhos a cada alteração.


## O erro que você vai ver

A lição tem um erro principal, o do teste que fica vermelho, e várias mensagens de apoio que convém saber ler num relance.

**O erro principal.** É o de 7.3.2. Você troca `>=` por `>` na regra do limite, roda `mvn -B verify`, e o relatório diz que uma das linhas da tabela de casos, a terceira, esperava `LENTO` e obteve `ARRIBA`. Lê-se em quatro passos, sempre os mesmos: **qual classe** falhou (a linha com `<<< FAILURE!`), **qual teste** (o nome do método e, entre colchetes, o número da linha), **o que esperava e o que obteve** (`expected: <...> but was: <...>`), e **quantos** no total (`Tests run: 18, Failures: 1`). Com isso você sabe o que mudar sem abrir um depurador, e tem a certeza de que o teste vigia o que diz vigiar. Vê-lo falhar, de propósito, é o primeiro uso que você faz dele.

**As mensagens de apoio**, para reconhecê-las num relance:

- `org.opentest4j.AssertionFailedError: expected: <...> but was: <...>`: uma asserção não se cumpriu. O relatório a conta em `Failures`. Se os valores parecem invertidos, confira a ordem de `assertEquals`: o esperado vem primeiro.
- Uma linha com `Errors: 1` em vez de `Failures: 1`: o teste não chegou a afirmar nada porque uma exceção inesperada foi lançada. É outro tipo de problema: não “o resultado é outro”, mas “o código caiu”.
- `Expected mx.habil.revisor.ErrorDeConfiguracion to be thrown, but nothing was thrown.`: é a forma como o JUnit diz que um `assertThrows` não viu a exceção que pedia. A leitura do arquivo aceitou algo que devia rejeitar. (Medido trocando em `Configuracion.java` o `timeoutMs > 0` por `timeoutMs >= 0`: um `timeout_ms` de 0 deixa de ser rejeitado e cai a linha `[3]` de `rechazaUnArchivoInvalido`.)
- `[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`: uma classe inteira foi ignorada. Amarelo, não vermelho, e o total baixa: procure uma suposição (7.5.2).
- `Tests are skipped.`: alguém rodou com `-DskipTests`. Esse verde não testou nada.
- Nenhuma linha `Tests run:` e `BUILD SUCCESS`: o *Surefire* não encontrou classes que se chamem como ele procura (7.5.2).

## O que se faz errado

**Confiar no `BUILD SUCCESS`.** Você já viu o custo em 7.5: um projeto sem um único teste efetivo termina em verde e com código de saída zero. O que conta é a linha `Tests run:` e que o número seja o esperado.

**Confiar em um teste que você nunca viu em vermelho.** Um teste que sempre passa não se distingue de um que não vigia nada. Antes de dá-lo por bom, quebre o que ele vigia, verifique que falha com a mensagem que você espera e desfaça a alteração.

**Um teste sem asserção.** Um método com `@Test` que chama o código mas nunca chama `assertEquals` nem `assertThrows` passa sempre, a não ser que o código lance uma exceção: executa o código, não verifica nada. O verde que ele dá não vale mais que o de um projeto sem testes.

**Testar apenas o caso feliz, ou apenas as rejeições.** Uma classe que só testa que um arquivo bom é lido corretamente não sabe se os ruins são rejeitados; uma que só testa rejeições passaria com um leitor que rejeita tudo. Os dois lados são necessários, como em `ConfiguracionTest`.

**Escolher os casos ao acaso.** Os erros se escondem na fronteira: o 999 e o 1000, o 399 e o 400. Uma tabela de casos que não tem os dois lados de cada fronteira deixa a borda sem vigilância, e o exercício 1 mede quanto isso custa.

**Testes que dependem de algo que você não controla:** a rede, a hora do dia, a ordem em que rodam, a pasta de onde são executados ou um arquivo que “já estava lá”. Um teste tem que dar o mesmo resultado amanhã e no computador de outra pessoa; para arquivos, `@TempDir`; para dados, que o próprio caso os monte.

**Comentar ou apagar um teste que falha “para que passe”.** O teste estava dizendo algo. Entenda a falha: ou o código está errado, ou o teste descreve uma regra que mudou, e então se atualiza o teste (como no exercício 2), não se silencia.

**Cortar a saída dos testes com um pipe.** `mvn -B verify | tail` troca o código de saída pelo do `tail`, e a falha desaparece (7.5.2). Os testes são rodados sem filtrar, ou com `set -o pipefail`.

## Exercícios

**Cada exercício parte do projeto de 7.6, com os seus 18 testes.** Se um exercício deixar para você testes a mais ou uma alteração no código, desfaça antes de começar o seguinte; assim as contagens que você prevê e as das soluções são as mesmas.

### Exercício 1 — A fronteira que falta

A tabela de casos de `ClasificadorTest` tem dois erros HTTP, 503 e 404, mas nenhum perto da fronteira entre “respondeu bem” e “respondeu com erro”. A regra diz que um código **maior ou igual a 400** deixa o serviço fora do ar. **Antes de mexer em qualquer coisa, preveja:** se alguém trocar em `Clasificador.java` a comparação `codigo >= 400` por `codigo > 400`, quantos dos 18 testes ficam vermelhos? Execute e confira a sua previsão. Depois acrescente à tabela os casos `400, 50, CAIDO` e `399, 50, ARRIBA`, repita a alteração e anote qual linha fica vermelha e quantos testes o relatório diz agora.

### Exercício 2 — A pergunta da segunda-feira

Responda à pergunta com que a lição começou. Mude `UMBRAL_LENTO_MS` para 800 em `Clasificador.java`. **Preveja** qual linha da tabela fica vermelha e por quê, e execute `mvn -B verify` para conferir. Depois atualize o **teste**, não o código: mude as duas linhas da fronteira para que vigiem o novo valor (um valor logo abaixo e o valor do limite), e confirme que o relatório volta a dizer `Tests run: 18, Failures: 0`. Anote no diário de bordo por que o teste teve que mudar.

### Exercício 3 — Um teste novo, à mão

`Formato.contar(long cantidad, String palabra)` acrescenta um “s” à palavra, exceto quando a quantidade é 1 (`1 caído`, `3 caídos`). Escreva em `FormatoTest` um teste parametrizado com três linhas: `0, servicio` (deve dar `0 servicios`), `1, caído` e `3, caído`. **Antes de executar**, anote quantos testes você espera que o relatório diga (dica: agora há três linhas a mais). Depois faça-o falhar de propósito trocando `cantidad == 1` por `cantidad <= 1` em `Formato.java`, e observe quantos testes ficam vermelhos e de quais classes. Você esperava que fossem de mais de uma classe?

### Exercício 4 — O verde que não testou nada

Renomeie a classe `ResumenTest` para `ResumenPrueba`, no arquivo e na declaração da classe. Execute `mvn -B verify` e responda três perguntas: terminou em `BUILD SUCCESS`? Quantos testes o relatório diz agora? Qual é o código de saída, com `echo $?` logo em seguida? Restaure o nome, confirme que a contagem volta a 18 (se algum dia uma contagem não bater depois de renomear ou apagar uma classe, `mvn -B clean verify` apaga `target/` e constrói do zero) e escreva no diário de bordo a frase com que você vai se lembrar de ler a linha `Tests run:`.

## Soluções

### Solução 1

**Previsão.** Nenhum fica vermelho: os 18 passam. Os casos de erro da tabela são 503 e 404, e ambos são maiores que 400 com qualquer das duas comparações. A mudança de `>=` para `>` só importa para o valor 400 exato, e a tabela não o contém. É um caso real de uma alteração que os testes **não detectam**: o defeito sobrevive, e o verde diz que está tudo bem.

Os dois casos novos são acrescentados ao final da tabela:

```java
  @CsvSource({
    "200, 142,  ARRIBA",
    "200, 999,  ARRIBA",
    "200, 1000, LENTO",
    "200, 2300, LENTO",
    "503, 95,   CAIDO",
    "404, 50,   CAIDO",
    "400, 50,   CAIDO",
    "399, 50,   ARRIBA"
  })
```

Com os dois casos e a troca de `>=` por `>` em `Clasificador.java`, o relatório diz:

```text
$ mvn -B verify
...
[ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.053 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[7] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <CAIDO> but was: <ARRIBA>
...
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:28 expected: <CAIDO> but was: <ARRIBA>
[ERROR] Tests run: 20, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

A linha `[7]` é a de `400, 50, CAIDO`: esperava `CAIDO` e obteve `ARRIBA`, porque com `>` o 400 já não é erro. E o total subiu para 20, dos 18 originais mais as duas linhas novas. A linha `399, 50, ARRIBA` passa nas duas versões; está ali para que o outro lado da fronteira também fique vigiado. Reverta a alteração em `Clasificador.java`: com os dois casos ainda na tabela, o relatório deve dizer `Tests run: 20, Failures: 0`. Em um projeto real essas duas linhas ficam, porque vigiam uma fronteira que antes ninguém vigiava; para continuar com os exercícios, tire-as e volte aos 18.

### Solução 2

Com o limite em 800, a regra vigia um valor que a tabela não esperava. Falha a linha `[2]`, `200, 999, ARRIBA`: um serviço de 999 milissegundos já não está no ar, está lento.

```text
$ mvn -B verify
...
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.050 s <<< FAILURE! -- in mx.habil.revisor.ClasificadorTest
[ERROR] mx.habil.revisor.ClasificadorTest.clasificaUnaRespuesta(int, long, Estado)[2] -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <ARRIBA> but was: <LENTO>
...
[ERROR]   ClasificadorTest.clasificaUnaRespuesta:26 expected: <ARRIBA> but was: <LENTO>
[ERROR] Tests run: 18, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

O `expected: <ARRIBA> but was: <LENTO>` diz tudo. As demais linhas continuam passando (o 142 continua no ar, e o 1000 e o 2300 continuam lentos), e nenhuma outra classe de testes quebra, porque nenhuma outra depende do limite. O que falha é o teste, e a razão é que a **regra de negócio mudou**: o teste descrevia o limite antigo. Por isso é ele que se atualiza, e não o código. As duas linhas da fronteira passam a ser `200, 799, ARRIBA` (logo abaixo) e `200, 800, LENTO` (o limite):

```java
    "200, 142,  ARRIBA",
    "200, 799,  ARRIBA",
    "200, 800,  LENTO",
    "200, 2300, LENTO",
```

e o relatório volta ao verde, com o mesmo total do início, porque se mudou o conteúdo de duas linhas e não se acrescentou nenhuma:

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in mx.habil.revisor.ClasificadorTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

E a pergunta da segunda-feira já tem resposta escrita, executável e vigiada: um serviço de exatamente 800 milissegundos é lento.

### Solução 3

O teste novo, dentro da classe `FormatoTest`, junto ao que ela já tem:

```java
  @ParameterizedTest
  @CsvSource({"0, servicio, 0 servicios", "1, caído, 1 caído", "3, caído, 3 caídos"})
  void pluralizaSalvoCuandoEsUno(long cantidad, String palabra, String esperado) {
    assertEquals(esperado, Formato.contar(cantidad, palabra));
  }
```

São três linhas, então a contagem passa de 18 para **21**:

```text
$ mvn -B verify
...
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Com `cantidad <= 1` em vez de `cantidad == 1`, o zero deixa de levar “s” (`0 servicio`) e esse caso falha:

```text
$ mvn -B verify
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.050 s -- in mx.habil.revisor.ClasificadorTest
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.007 s <<< FAILURE! -- in mx.habil.revisor.ResumenTest
[ERROR] mx.habil.revisor.ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya -- Time elapsed: 0.002 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lento · 1 caído · promedio —>
...
[ERROR] Tests run: 7, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.009 s <<< FAILURE! -- in mx.habil.revisor.FormatoTest
[ERROR] mx.habil.revisor.FormatoTest.pluralizaSalvoCuandoEsUno(long, String, String)[1] -- Time elapsed: 0.001 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <0 servicios> but was: <0 servicio>
...
[ERROR]   FormatoTest.pluralizaSalvoCuandoEsUno:22 expected: <0 servicios> but was: <0 servicio>
[ERROR]   ResumenTest.sinNingunaRespuestaElPromedioEsUnaRaya:41 expected: <1 servicio · 0 arriba · 0 lentos · 1 caído · promedio —> but was: <1 servicio · 0 arriba · 0 lento · 1 caído · promedio —>
[ERROR] Tests run: 21, Failures: 2, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

Falham **dois** testes, de **duas classes**: o novo (`FormatoTest`, linha `[1]`, a de `0 servicios`) e um que já existia, `ResumenTest`, cujo caso de “ninguém respondeu” espera `0 lentos`. É normal que a surpresa seja essa: os testes são escritos para uma coisa, mas vigiam tudo o que passa por ali. Uma mudança numa função compartilhada é mais fácil de detectar se várias partes do programa dependem dela. E é uma boa razão para não apagar um teste “redundante”.

### Solução 4

Sem a classe `ResumenTest`, o *Surefire* já não a encontra, e o relatório diz:

```text
$ mvn -B verify
...
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
$ echo $?
0
```

`BUILD SUCCESS`, código de saída `0`, e **16** testes em vez de 18: faltam os dois de `ResumenTest`. Se você não sabia que o número correto era 18, nada teria avisado você. Essa é a frase para o diário de bordo: **“Tests run: 18, sempre; se o número muda e eu não o mudei, eu paro”.** Ao restaurar o nome, a contagem volta a 18. Um detalhe medido: com o Maven 3.10.0, `mvn -B verify` simples já dá 16, porque o compilador detecta que uma fonte mudou, recompila o módulo inteiro e não deixa em `target/test-classes` o `ResumenTest.class` antigo; `mvn -B clean verify` também dá 16 e não depende desse comportamento, então é a forma segura quando você renomeia ou apaga classes.

## Como sei que consegui

- [ ] `mvn -B verify`, a partir da raiz de `~/curso-java/revisor/`, termina com `BUILD SUCCESS` e diz `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`, e você sabe de onde sai cada um dos 18 (7 + 4 + 5 + 2).
- [ ] O `.jar` agora se chama `revisor-0.7.0.jar`, e `java -cp target/classes mx.habil.revisor.Revisor` continua imprimindo a tabela com `promedio 843ms`.
- [ ] Você trocou `>=` por `>` em `Clasificador.java`, viu a linha `[3]` em vermelho com `expected: <LENTO> but was: <ARRIBA>`, e desfez a alteração.
- [ ] Você rodou um único teste com `-Dtest=Clase#metodo` e depois todos, e sabe por que no final se rodam todos.
- [ ] Você renomeou `ResumenTest` para `ResumenPrueba`, viu `BUILD SUCCESS` com 16 testes, e escreveu no diário de bordo como vai se lembrar de ler a contagem.
- [ ] Você sabe explicar para que serve `<scope>test</scope>` e por que o JUnit não vai no programa.
- [ ] Você anotou no seu diário de bordo o número que o relatório diz, com a data.

## Resumo

Responda sem olhar a lição:

1. Quais são os três tempos de um teste? Qual argumento de `assertEquals` vem primeiro, e o que acontece se você os inverter?
2. Para que serve o BOM do JUnit e o que faz `<scope>test</scope>`?
3. Por que esta lição testa `Clasificador`, `Formato`, `Resumen` e `Configuracion`, e não `Revision` nem `Simulador`?
4. Por que se quebra de propósito o código antes de dar um teste por bom?
5. O que significa o `[3]` no nome de um teste parametrizado que falha? Por que os casos da tabela são escolhidos em torno da fronteira?
6. O que `assertThrows` verifica e o que acontece se o código não lançar nada? O que faz `@TempDir`?
7. Que diferença há entre `Failures` e `Errors` no relatório?
8. Três formas em que o Maven termina em verde sem ter testado o que você pensa: quais são e como você se protege de cada uma?

## Para ler mais

- [JUnit 6, guia do usuário](https://docs.junit.org/current/user-guide/): as anotações, as [asserções](https://docs.junit.org/current/writing-tests/assertions.html), as [suposições](https://docs.junit.org/current/writing-tests/assumptions.html) e os [testes parametrizados](https://docs.junit.org/current/writing-tests/parameterized-classes-and-tests.html) com todas as suas fontes de dados, além de `@CsvSource`. Consultado em 13 de outubro de 2026.
- [Maven Surefire, “Inclusions and Exclusions of Tests”](https://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html): quais nomes de classe ele roda e como mudar isso, útil quando uma contagem não bate. Consultado em 7 de outubro de 2026.
- [Maven, “Introduction to the Dependency Mechanism”](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html): os escopos (`test` entre eles), o BOM e como se importa com `<scope>import</scope>`. Consultado em 13 de outubro de 2026.
- [Manual do Bash, o comando `set`](https://www.gnu.org/software/bash/manual/html_node/The-Set-Builtin.html): o que o `pipefail` faz com o código de saída de um pipe. Consultado em 13 de outubro de 2026.

---

**Anterior:** [Lição 6 — Projeto com Maven](06-proyecto-maven.md) ·
**Próxima:** [Lição 8 — HTTP e JSON](08-http-y-json.md)
