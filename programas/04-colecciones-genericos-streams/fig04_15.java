// fig04_15.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", 142),
          new Medicion("inventario", 2300),
          new Medicion("pagos", 87));

  Stream<String> armado =
      mediciones.stream()
          .peek(medicion -> IO.println("miro " + medicion.nombre()))
          .filter(medicion -> medicion.tiempoMs() >= 100)
          .peek(medicion -> IO.println("  pasa " + medicion.nombre()))
          .map(Medicion::nombre);
  IO.println("el stream está armado; todavía no pasó nada");

  List<String> resultado = armado.toList();
  IO.println(resultado);
}
