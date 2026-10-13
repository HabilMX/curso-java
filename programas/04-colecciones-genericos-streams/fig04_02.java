// fig04_02.java
void main() {
  Set<String> vistos = new LinkedHashSet<>();
  IO.println(vistos.add("catalogo"));
  IO.println(vistos.add("pagos"));
  IO.println(vistos.add("catalogo"));
  IO.println(vistos + ", tamaño " + vistos.size());

  Map<String, Long> tiempos = new LinkedHashMap<>();
  tiempos.put("catalogo", 142L);
  tiempos.put("inventario", 2300L);
  tiempos.put("pagos", 87L);
  IO.println(tiempos);
  IO.println(tiempos.get("pagos"));
  IO.println(tiempos.get("reportes"));
  IO.println(tiempos.getOrDefault("reportes", 0L));
  IO.println(tiempos.containsKey("reportes"));
  IO.println(tiempos.put("pagos", 90L));
  IO.println(tiempos);

  for (Map.Entry<String, Long> par : tiempos.entrySet()) {
    IO.println(par.getKey() + " tardó " + par.getValue() + " ms");
  }

  Map<String, Long> ordenado = new TreeMap<>(tiempos);
  IO.println(ordenado);
}
