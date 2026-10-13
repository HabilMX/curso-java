// fig03_04.java
record Umbral(String nombre, long limiteMs) {
  Umbral {
    nombre = nombre.strip();
    if (limiteMs <= 0) {
      throw new IllegalArgumentException("limiteMs debe ser mayor que 0 y llegó " + limiteMs);
    }
  }
}

void main() {
  Umbral lento = new Umbral("  lento ", 1000);
  IO.println(lento);
  Umbral roto = new Umbral("roto", 0);
  IO.println(roto);
}
