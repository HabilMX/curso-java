// fig08_09.java
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;

void mostrar(String texto) {
  try {
    URI url = new URI(texto);
    IO.println(texto + " -> esquema " + url.getScheme() + ", servidor " + url.getHost());
  } catch (URISyntaxException e) {
    IO.println(texto + " -> no es una URI válida");
  }
}

void main() {
  mostrar("http://127.0.0.1:8089/pagos");
  mostrar("http://LOCALHOST:8089/pagos");
  mostrar("http://127.0.0.1@169.254.169.254/latest/meta-data/");
  mostrar("http://127.0.0.1.otro.example/pagos");
  mostrar("http://[::1]:8089/pagos");
  mostrar("http://127.0.0.1:puerto/pagos");
  mostrar("file:///etc/passwd");
  mostrar("pagos");
  try {
    HttpRequest.newBuilder(URI.create("file:///etc/passwd"));
  } catch (IllegalArgumentException e) {
    IO.println("HttpRequest rechaza file: -> " + e.getMessage());
  }
}
