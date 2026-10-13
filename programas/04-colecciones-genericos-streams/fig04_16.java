// fig04_16.java
void main() {
  Stream<String> nombres = Stream.of("catalogo", "inventario", "pagos");
  IO.println(nombres.count());
  IO.println(nombres.count());
}
