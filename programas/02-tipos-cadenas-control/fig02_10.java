// fig02_10.java
void main() {
  String encabezado = """
      # nombre      url
      catalogo      https://catalogo.example
      pagos         https://pagos.example
      """;
  IO.print(encabezado);
  IO.println("renglones con salto final: " + encabezado.endsWith("\n"));

  String cita = """
      El servicio "%s" tardó %d ms.\
       Revísalo.""".formatted("inventario", 2300);
  IO.println(cita);
}
