// fig05_09.java
long tiempoOLanza(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142;
    case "pagos" -> 87;
    default -> throw new IllegalStateException(nombre + ": sin conexión");
  };
}

OptionalLong tiempoO(String nombre) {
  return switch (nombre) {
    case "catalogo" -> OptionalLong.of(142);
    case "pagos" -> OptionalLong.of(87);
    default -> OptionalLong.empty();
  };
}

void main() {
  List<String> nombres = List.of("catalogo", "reportes", "pagos");

  IO.println("con una excepción:");
  try {
    for (String nombre : nombres) {
      IO.println("  " + nombre + ": " + tiempoOLanza(nombre) + " ms");
    }
  } catch (IllegalStateException e) {
    IO.println("  se cayó el lote: " + e.getMessage());
  }

  IO.println("con un valor:");
  for (String nombre : nombres) {
    OptionalLong tiempo = tiempoO(nombre);
    String texto = tiempo.isPresent() ? tiempo.getAsLong() + " ms" : "sin respuesta";
    IO.println("  " + nombre + ": " + texto);
  }
}
