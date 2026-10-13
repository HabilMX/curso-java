// fig04_10.java
void main() {
  List<String> nombres = new ArrayList<>();
  List<Integer> tiempos = new ArrayList<>();
  IO.println(nombres.getClass() == tiempos.getClass());
  IO.println(nombres.getClass().getName());

  List crudo = nombres;
  crudo.add(142);
  IO.println("agregué un número a una lista de String y nadie se quejó");
  String primero = nombres.get(0);
  IO.println(primero);
}
