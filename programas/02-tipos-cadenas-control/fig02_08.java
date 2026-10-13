// fig02_08.java
void main() {
  String prefijo = "inven";
  String armado = prefijo + "tario";
  String literal = "inventario";
  String constante = "inven" + "tario";

  IO.println("armado == literal:       " + (armado == literal));
  IO.println("armado.equals(literal):  " + armado.equals(literal));
  IO.println("constante == literal:    " + (constante == literal));
  IO.println("mayúsculas distintas:    " + "INVENTARIO".equals(literal));
  IO.println("ignorando mayúsculas:    " + "INVENTARIO".equalsIgnoreCase(literal));
}
