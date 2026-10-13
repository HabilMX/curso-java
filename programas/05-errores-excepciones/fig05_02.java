// fig05_02.java
void main() {
  List<String> renglones = Files.readAllLines(Path.of("servicios.txt"));
  IO.println(renglones.size());
}
