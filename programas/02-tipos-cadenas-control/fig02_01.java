// fig02_01.java
void main() {
  int codigo = 200;
  long tiempoMs = 2300L;
  double segundos = tiempoMs / 1000.0;
  boolean lento = tiempoMs >= 1000;
  char inicial = 'c';

  IO.println("código:    " + codigo);
  IO.println("tiempo:    " + tiempoMs);
  IO.println("segundos:  " + segundos);
  IO.println("lento:     " + lento);
  IO.println("inicial:   " + inicial);

  IO.println("7 / 2      = " + 7 / 2);
  IO.println("7 / 2.0    = " + 7 / 2.0);
  IO.println("2300 / 1000 = " + 2300 / 1000);
  IO.println("0.1 + 0.2  = " + (0.1 + 0.2));
}
