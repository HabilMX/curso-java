// fig02_16.java
void main() {
  String nombre = "reportes";
  int codigo = switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
  };
  IO.println(codigo);
}
