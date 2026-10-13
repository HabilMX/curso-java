// fig04_24.java
void main() {
  List<String> nombres = new ArrayList<>(List.of("catalogo", "inventario", "pagos", "reportes"));
  for (String nombre : nombres) {
    if (nombre.startsWith("i")) {
      nombres.remove(nombre);
    }
  }
  IO.println(nombres);
}
