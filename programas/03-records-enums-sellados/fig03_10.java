// fig03_10.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio r -> "respondió " + r.codigo();
  };
}

void main() {
  IO.println(describir(new Respuesta.SinRespuesta("sin conexión")));
}
