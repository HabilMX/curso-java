// fig04_01.java
void main() {
  List<String> nombres = new ArrayList<>();
  nombres.add("catalogo");
  nombres.add("inventario");
  nombres.add("pagos");
  IO.println(nombres);
  IO.println("tamaño: " + nombres.size());
  IO.println("el segundo: " + nombres.get(1));
  IO.println("¿está pagos? " + nombres.contains("pagos"));
  IO.println("posición de pagos: " + nombres.indexOf("pagos"));
  IO.println("primero y último: " + nombres.getFirst() + ", " + nombres.getLast());

  nombres.add(1, "reportes");
  nombres.remove("pagos");
  nombres.set(0, "catalogo-v2");
  IO.println(nombres);

  for (String nombre : nombres) {
    IO.println("servicio: " + nombre);
  }
  for (int i = 0; i < nombres.size(); i++) {
    IO.println(i + " -> " + nombres.get(i));
  }
}
