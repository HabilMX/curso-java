// fig05_08.java
int leerTimeout(String texto) {
  try {
    return Integer.parseInt(texto);
  } catch (Exception e) {
  }
  return 5000;
}

void main() {
  IO.println("timeout_ms = " + leerTimeout("2000"));
  IO.println("timeout_ms = " + leerTimeout("dos mil"));
}
