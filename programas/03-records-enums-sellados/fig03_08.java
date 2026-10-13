// fig03_08.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

String describir(Respuesta respuesta) {
  return switch (respuesta) {
    case Respuesta.Respondio r -> "respondió " + r.codigo() + " en " + r.tiempoMs() + " ms";
    case Respuesta.SinRespuesta s -> "no respondió: " + s.motivo();
  };
}

void main() {
  Respuesta uno = new Respuesta.Respondio(200, 142);
  Respuesta otro = new Respuesta.SinRespuesta("sin conexión");
  IO.println(describir(uno));
  IO.println(describir(otro));
}
