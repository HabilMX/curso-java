// fig04_19.java
Optional<Long> tiempoDe(Map<String, Long> tiempos, String nombre) {
  return Optional.ofNullable(tiempos.get(nombre));
}

void main() {
  Map<String, Long> tiempos = Map.of("catalogo", 142L, "pagos", 87L);

  Optional<Long> hay = tiempoDe(tiempos, "pagos");
  Optional<Long> noHay = tiempoDe(tiempos, "reportes");
  IO.println(hay);
  IO.println(noHay);
  IO.println(hay.isPresent() + " " + noHay.isEmpty());

  IO.println(hay.orElse(0L));
  IO.println(noHay.orElse(0L));
  IO.println(hay.map(tiempo -> tiempo + " ms").orElse("sin tiempo"));
  IO.println(noHay.map(tiempo -> tiempo + " ms").orElse("sin tiempo"));
  hay.ifPresent(tiempo -> IO.println("pagos respondió en " + tiempo));
  noHay.ifPresent(tiempo -> IO.println("esto no se imprime"));

  OptionalLong mayor = tiempos.values().stream().mapToLong(Long::longValue).max();
  IO.println(mayor);
  IO.println(mayor.getAsLong());
}
