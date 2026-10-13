// fig02_03.java
void main() {
  int codigo = 200;
  Integer codigoCaja = 200;
  Integer sinRespuesta = null;

  IO.println("primitivo:  " + codigo);
  IO.println("envoltorio: " + codigoCaja);
  IO.println("nulo:       " + sinRespuesta);
  IO.println("¿iguales?   " + codigoCaja.equals(codigo));

  Integer a = 127;
  Integer b = 127;
  IO.println("127 == 127:   " + (a == b));
  Integer c = 2300;
  Integer d = 2300;
  IO.println("2300 == 2300: " + (c == d));
  IO.println("2300 equals:  " + c.equals(d));
}
