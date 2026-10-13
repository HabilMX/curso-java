// fig02_09.java
void main() {
  double segundos = 2.3;
  Locale mexico = Locale.of("es", "MX");
  Locale espana = Locale.of("es", "ES");

  IO.println(String.format(Locale.ROOT, "%.1fs", segundos));
  IO.println(String.format(mexico, "%.1fs", segundos));
  IO.println(String.format(espana, "%.1fs", segundos));
  IO.println(String.format(Locale.ROOT, "[%-12s][%6s][%d]", "catalogo", "142ms", 200));
  IO.println("%s tardó %d ms".formatted("pagos", 87));
}
