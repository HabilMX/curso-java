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
