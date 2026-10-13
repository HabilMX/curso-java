// fig03_02.java
record Punto(String nombre, long tiempoMs) {}

void main() {
  Punto punto = new Punto("catalogo", 142);
  punto.tiempoMs = 5;
}
