// fig04_09.java
void main() {
  Map<String, Long> tiempos = Map.of("catalogo", 142L, "pagos", 87L);
  long tiempoPagos = tiempos.get("pagos");
  IO.println("pagos: " + tiempoPagos);
  long tiempoReportes = tiempos.get("reportes");
  IO.println("reportes: " + tiempoReportes);
}
