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
