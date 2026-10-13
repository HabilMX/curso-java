// fig03_13.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio _ -> "ARRIBA";
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.SinRespuesta _ -> "CAIDO";
  };
}

void main() {
  IO.println(clasificar(new Respuesta.Respondio(503, 95)));
}
