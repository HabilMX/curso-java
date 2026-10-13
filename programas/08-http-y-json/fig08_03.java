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
