// fig03_12.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

static final long UMBRAL_LENTO_MS = 1000;

String clasificar(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio(int codigo, long _) when codigo >= 400 -> "CAIDO";
    case Respuesta.Respondio(int _, long tiempoMs) when tiempoMs >= UMBRAL_LENTO_MS -> "LENTO";
    case Respuesta.Respondio _ -> "ARRIBA";
    case Respuesta.SinRespuesta _ -> "CAIDO";
  };
}

void main() {
  IO.println(clasificar(new Respuesta.Respondio(200, 142)));
  IO.println(clasificar(new Respuesta.Respondio(200, 2300)));
  IO.println(clasificar(new Respuesta.Respondio(503, 95)));
  IO.println(clasificar(new Respuesta.SinRespuesta("sin conexión")));
}
