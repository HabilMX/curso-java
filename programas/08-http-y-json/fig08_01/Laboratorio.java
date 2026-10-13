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
