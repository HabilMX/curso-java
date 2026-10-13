// fig04_21.java
String calcular(String quien) {
  IO.println("  (calculando el valor por omisión para " + quien + ")");
  return "ninguno";
}

void main() {
  Optional<String> hay = Optional.of("catalogo");
  Optional<String> noHay = Optional.empty();

  IO.println("orElse con valor presente:");
  IO.println(hay.orElse(calcular("orElse")));
  IO.println("orElseGet con valor presente:");
  IO.println(hay.orElseGet(() -> calcular("orElseGet")));
  IO.println("orElseGet con valor ausente:");
  IO.println(noHay.orElseGet(() -> calcular("orElseGet")));
}
