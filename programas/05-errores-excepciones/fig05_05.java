// fig05_05.java
void main() {
  List<String> nombres = List.of("catalogo", "pagos");
  List<String> revisados =
      nombres.stream()
          .map(
              nombre -> {
                Thread.sleep(10);
                return nombre + " revisado";
              })
          .toList();
  IO.println(revisados);
}
