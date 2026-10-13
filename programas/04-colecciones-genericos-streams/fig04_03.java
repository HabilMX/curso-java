// fig04_03.java
record Direccion(String servidor, int puerto) {}

void main() {
  Set<Direccion> direcciones = new LinkedHashSet<>();
  direcciones.add(new Direccion("catalogo.example", 443));
  direcciones.add(new Direccion("pagos.example", 443));
  direcciones.add(new Direccion("catalogo.example", 443));
  IO.println(direcciones.size());
  IO.println(direcciones);

  Map<Direccion, String> duenos = new HashMap<>();
  duenos.put(new Direccion("pagos.example", 443), "equipo de cobranza");
  IO.println(duenos.get(new Direccion("pagos.example", 443)));
  IO.println(duenos.get(new Direccion("pagos.example", 8443)));
}
