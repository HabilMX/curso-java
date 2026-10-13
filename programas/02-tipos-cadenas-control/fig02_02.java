// fig02_02.java
void main() {
  int maximo = Integer.MAX_VALUE;
  IO.println("máximo de un int: " + maximo);
  IO.println("máximo + 1:       " + (maximo + 1));

  long mayor = Long.MAX_VALUE;
  IO.println("máximo de un long: " + mayor);

  int milisegundosEnUnDia = 24 * 60 * 60 * 1000;
  IO.println("ms en un día:      " + milisegundosEnUnDia);
  int milisegundosEnUnMes = 30 * 24 * 60 * 60 * 1000;
  IO.println("ms en 30 días (int):  " + milisegundosEnUnMes);
  long enUnMes = 30L * 24 * 60 * 60 * 1000;
  IO.println("ms en 30 días (long): " + enUnMes);
}
