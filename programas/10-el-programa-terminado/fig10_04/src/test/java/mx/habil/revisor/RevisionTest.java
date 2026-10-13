// fig10_04/src/test/java/mx/habil/revisor/RevisionTest.java

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
