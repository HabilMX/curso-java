// fig04_18.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  List<Medicion> mediciones =
      List.of(
          new Medicion("pagos", 87),
          new Medicion("catalogo", 142),
          new Medicion("busqueda", 142),
          new Medicion("inventario", 2300));

  Comparator<Medicion> porTiempo = Comparator.comparingLong(Medicion::tiempoMs);

  List<Medicion> ascendente = mediciones.stream().sorted(porTiempo).toList();
  List<Medicion> descendente = mediciones.stream().sorted(porTiempo.reversed()).toList();
  List<Medicion> desempatado =
      mediciones.stream().sorted(porTiempo.thenComparing(Medicion::nombre)).toList();

  ascendente.forEach(medicion -> IO.println("asc  " + medicion));
  descendente.forEach(medicion -> IO.println("desc " + medicion));
  desempatado.forEach(medicion -> IO.println("tiempo y nombre " + medicion));

  Optional<Medicion> masLenta = mediciones.stream().max(porTiempo);
  IO.println(masLenta);
  IO.println(mediciones);
}
