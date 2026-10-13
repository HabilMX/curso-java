// fig09_08.java
String consultar(String nombre) {
  return "respuesta de " + nombre;
}

void main() throws Exception {
  try (var alcance = StructuredTaskScope.open()) {
    var catalogo = alcance.fork(() -> consultar("catalogo"));
    var pagos = alcance.fork(() -> consultar("pagos"));
    alcance.join();
    IO.println(catalogo.get() + " / " + pagos.get());
  }
}
