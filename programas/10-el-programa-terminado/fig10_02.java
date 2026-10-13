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
