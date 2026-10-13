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
