// fig04_23.java
void main() {
  List<String> nombres = Stream.of("catalogo", "inventario", "pagos").toList();
  IO.println(nombres);
  nombres.add("reportes");
}
