// fig04_11.java
static <T> T primeroO(List<T> lista, T porOmision) {
  if (lista.isEmpty()) {
    return porOmision;
  }
  return lista.get(0);
}

void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<Long> tiempos = List.of();
  String primerNombre = primeroO(nombres, "ninguno");
  Long primerTiempo = primeroO(tiempos, 0L);
  IO.println(primerNombre);
  IO.println(primerTiempo);
}
