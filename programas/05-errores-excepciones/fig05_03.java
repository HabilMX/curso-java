// fig05_03.java
int leerTimeout(String texto) {
  try {
    int valor = Integer.parseInt(texto);
    IO.println("  convertí " + valor);
    return valor;
  } catch (NumberFormatException e) {
    IO.println("  no sirve: " + e.getMessage());
    return -1;
  } finally {
    IO.println("  (fin de la conversión)");
  }
}

void main() {
  IO.println("leyendo 5000");
  IO.println("  resultado: " + leerTimeout("5000"));
  IO.println("leyendo 5 s");
  IO.println("  resultado: " + leerTimeout("5 s"));
}
