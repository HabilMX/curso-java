// fig02_18.java
void main() {
  Integer codigo = codigoDe("reportes");
  IO.println("código: " + codigo);
  int sumado = codigo + 1;
  IO.println("sumado: " + sumado);
}

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}
