// fig02_19.java
void main() {
  String texto = codigoDe("reportes").toString();
  IO.println(texto);
}

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}
