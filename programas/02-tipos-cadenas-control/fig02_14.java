// fig02_14.java
void main() {
  String nombre = "inventario";
  int antiguo = 0;
  switch (nombre) {
    case "catalogo":
      antiguo = 1;
    case "inventario":
      antiguo = 2;
    case "pagos":
      antiguo = 3;
      break;
    default:
      antiguo = -1;
  }
  IO.println("switch con dos puntos y sin break: " + antiguo);

  int moderno = switch (nombre) {
    case "catalogo" -> 1;
    case "inventario" -> 2;
    case "pagos" -> 3;
    default -> -1;
  };
  IO.println("switch con flecha:                " + moderno);
}
