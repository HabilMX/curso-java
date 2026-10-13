// fig05_04.java
void main() {
  try {
    IO.println(Integer.parseInt("5 s"));
  } catch (Exception e) {
    IO.println("algo falló");
  } catch (NumberFormatException e) {
    IO.println("no es un número");
  }
}
