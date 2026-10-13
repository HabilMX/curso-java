// fig04_14.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", 142),
          new Medicion("inventario", 2300),
          new Medicion("pagos", 87));

  List<String> conBucle = new ArrayList<>();
  for (Medicion medicion : mediciones) {
    if (medicion.tiempoMs() >= 100) {
      conBucle.add(medicion.nombre() + ":" + medicion.tiempoMs());
    }
  }

  List<String> conStream =
      mediciones.stream()
          .filter(medicion -> medicion.tiempoMs() >= 100)
          .map(medicion -> medicion.nombre() + ":" + medicion.tiempoMs())
          .toList();

  IO.println(conBucle);
  IO.println(conStream);
  IO.println(conBucle.equals(conStream));

  List<String> nombres = mediciones.stream().map(Medicion::nombre).toList();
  long suma = mediciones.stream().mapToLong(Medicion::tiempoMs).sum();
  long rapidos = mediciones.stream().filter(medicion -> medicion.tiempoMs() < 100).count();
  IO.println(nombres);
  IO.println("suma: " + suma + " ms");
  IO.println("rápidos: " + rapidos);
}
