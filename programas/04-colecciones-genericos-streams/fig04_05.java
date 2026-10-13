// fig04_05.java
record ReporteIngenuo(List<String> nombres) {}

record ReporteSeguro(List<String> nombres) {
  ReporteSeguro {
    nombres = List.copyOf(nombres);
  }
}

void main() {
  List<String> nombres = new ArrayList<>(List.of("catalogo", "pagos"));
  ReporteIngenuo ingenuo = new ReporteIngenuo(nombres);
  ReporteSeguro seguro = new ReporteSeguro(nombres);
  nombres.add("reportes");
  IO.println("ingenuo: " + ingenuo);
  IO.println("seguro:  " + seguro);

  List<String> vista = Collections.unmodifiableList(nombres);
  List<String> copia = List.copyOf(nombres);
  nombres.add("facturacion");
  IO.println("vista: " + vista);
  IO.println("copia: " + copia);
}
