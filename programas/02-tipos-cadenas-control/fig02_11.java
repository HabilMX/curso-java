// fig02_11.java
void main() {
  int codigo = 503;
  long tiempoMs = 95;

  if (codigo >= 400) {
    IO.println("CAIDO: el servicio contestó " + codigo);
  } else if (tiempoMs >= 1000) {
    IO.println("LENTO");
  } else {
    IO.println("ARRIBA");
  }

  boolean respondio = codigo != 0 && tiempoMs > 0;
  IO.println("¿respondió? " + respondio);
  String mensaje = respondio ? "con respuesta" : "sin respuesta";
  IO.println(mensaje);
}
