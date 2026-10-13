// fig03_01.java
record Punto(String nombre, long tiempoMs) {}

void main() {
  Punto a = new Punto("catalogo", 142);
  Punto b = new Punto("catalogo", 142);
  Punto c = new Punto("pagos", 87);

  IO.println(a);
  IO.println("nombre:     " + a.nombre());
  IO.println("tiempoMs:   " + a.tiempoMs());
  IO.println("a.equals(b): " + a.equals(b));
  IO.println("a == b:      " + (a == b));
  IO.println("a.equals(c): " + a.equals(c));
  IO.println("mismo hash:  " + (a.hashCode() == b.hashCode()));
}
