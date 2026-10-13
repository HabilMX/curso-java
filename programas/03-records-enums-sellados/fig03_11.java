// fig03_11.java
sealed interface Respuesta
    permits Respuesta.Respondio, Respuesta.SinRespuesta, Respuesta.Redirigida {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}

  record Redirigida(int codigo, String destino) implements Respuesta {}
}

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.Respondio _ -> "ARRIBA";
    default -> "CAIDO";
  };
}

void main() {
  Respuesta redirigida = new Respuesta.Redirigida(301, "https://catalogo.example/nuevo");
  IO.println(clasificar(redirigida));
}
