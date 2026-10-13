// fig02_17.java
static final int UMBRAL_LENTO_MS = 1000;

void main() {
  long tiempoMs = 2300;
  IO.println(marcar(tiempoMs));
  IO.println("después de la llamada, tiempoMs sigue valiendo " + tiempoMs);
  IO.println(marcar(87));
}

String marcar(long tiempoMs) {
  String marca = tiempoMs >= UMBRAL_LENTO_MS ? "lento" : "rápido";
  tiempoMs = 0;
  return marca;
}
