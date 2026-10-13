// fig04_25.java
void main() {
  List<Integer> tiempos = new ArrayList<>(List.of(142, 2300, 87));
  tiempos.remove(Integer.valueOf(87));
  IO.println(tiempos);
  tiempos.remove(87);
  IO.println(tiempos);
}
