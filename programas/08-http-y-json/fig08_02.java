// fig08_02.java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

void main() throws Exception {
  try (HttpClient cliente = HttpClient.newHttpClient()) {
    HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8089/pagos")).build();
    long inicio = System.nanoTime();
    HttpResponse<String> respuesta = cliente.send(peticion, BodyHandlers.ofString());
    long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
    IO.println("código de estado: " + respuesta.statusCode());
    IO.println("cuerpo: " + respuesta.body().strip());
    IO.println("tardó al menos 87 ms: " + (tiempoMs >= 87));
  }
}
