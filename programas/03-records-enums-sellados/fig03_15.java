// fig03_15.java
sealed interface Respuesta permits Respuesta.Respondio, Respuesta.SinRespuesta {
  record Respondio(int codigo, long tiempoMs) implements Respuesta {}

  record SinRespuesta(String motivo) implements Respuesta {}
}

boolean esError(Respuesta respuesta) {
  return respuesta instanceof Respuesta.Respondio r && r.codigo() >= 400;
}

void main() {
  IO.println(esError(new Respuesta.Respondio(503, 95)));
  IO.println(esError(new Respuesta.Respondio(200, 142)));
  IO.println(esError(new Respuesta.SinRespuesta("sin conexión")));
}
