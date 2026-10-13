// fig03_05.java
record Ruta(String nombre, String[] etapas) {}

void main() {
  String[] etapas = {"conectar", "esperar"};
  Ruta ruta = new Ruta("catalogo", etapas);
  IO.println("antes:   " + Arrays.toString(ruta.etapas()));

  etapas[0] = "CAMBIADO DESDE FUERA";
  ruta.etapas()[1] = "CAMBIADO DESDE EL ACCESOR";
  IO.println("después: " + Arrays.toString(ruta.etapas()));

  Ruta copia = new Ruta("catalogo", new String[] {"conectar", "esperar"});
  Ruta otra = new Ruta("catalogo", new String[] {"conectar", "esperar"});
  IO.println("copia.equals(otra): " + copia.equals(otra));
}
