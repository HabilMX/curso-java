// fig04_13.java
void main() {
  List<Long> tiempos = List.of(142L, 2300L, 87L);
  long suma = 0;
  tiempos.forEach(tiempo -> suma += tiempo);
  IO.println(suma);
}
