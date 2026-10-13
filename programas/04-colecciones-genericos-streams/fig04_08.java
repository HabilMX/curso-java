// fig04_08.java
void main() {
  List<Integer> tiempos = List.of(87, 87, 1000, 1000);
  IO.println(tiempos.get(0) == tiempos.get(1));
  IO.println(tiempos.get(2) == tiempos.get(3));
  IO.println(tiempos.get(2).equals(tiempos.get(3)));
}
