// fig04_17.java
record Medicion(String nombre, String estado, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("catalogo", "ARRIBA", 142),
          new Medicion("inventario", "LENTO", 2300),
          new Medicion("pagos", "ARRIBA", 87),
          new Medicion("reportes", "CAIDO", 0));

  Map<String, Long> porEstado =
      mediciones.stream()
          .collect(Collectors.groupingBy(Medicion::estado, TreeMap::new, Collectors.counting()));
  IO.println(porEstado);

  Map<String, List<String>> nombresPorEstado =
      mediciones.stream()
          .collect(
              Collectors.groupingBy(
                  Medicion::estado,
                  TreeMap::new,
                  Collectors.mapping(Medicion::nombre, Collectors.toList())));
  IO.println(nombresPorEstado);

  String todos = mediciones.stream().map(Medicion::nombre).collect(Collectors.joining(", "));
  IO.println(todos);

  OptionalDouble promedio =
      mediciones.stream()
          .filter(medicion -> !medicion.estado().equals("CAIDO"))
          .mapToLong(Medicion::tiempoMs)
          .average();
  IO.println(promedio);
  IO.println(Math.round(promedio.getAsDouble()));

  OptionalDouble sinDatos =
      mediciones.stream()
          .filter(medicion -> medicion.tiempoMs() > 9999)
          .mapToLong(Medicion::tiempoMs)
          .average();
  IO.println(sinDatos);
}
