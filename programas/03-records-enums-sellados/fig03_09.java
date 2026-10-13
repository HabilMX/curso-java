// fig03_09.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

record Redirigida(int codigo, String destino) implements Respuesta {}

void main() {
  Respuesta respuesta = new Redirigida(301, "https://catalogo.example/nuevo");
  IO.println(respuesta);
}
