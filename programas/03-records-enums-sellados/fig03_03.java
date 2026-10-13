// fig03_03.java
record Servicio(String nombre, URI url, int timeoutMs) {
  static final int TIMEOUT_POR_OMISION_MS = 5000;

  Servicio(String nombre, URI url) {
    this(nombre, url, TIMEOUT_POR_OMISION_MS);
  }
}

void main() {
  URI direccion = URI.create("https://catalogo.example");
  Servicio catalogo = new Servicio("catalogo", direccion);
  Servicio notificaciones =
      new Servicio("notificaciones", URI.create("https://notificaciones.example"), 1000);

  IO.println(catalogo);
  IO.println(notificaciones);
  IO.println("host de catalogo: " + catalogo.url().getHost());
}
