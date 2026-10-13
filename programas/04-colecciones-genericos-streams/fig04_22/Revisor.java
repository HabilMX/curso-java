// fig04_22/Revisor.java
final List<Servicio> servicios =
    List.of(
        new Servicio("catalogo", URI.create("https://catalogo.example")),
        new Servicio("inventario", URI.create("https://inventario.example")),
        new Servicio("pagos", URI.create("https://pagos.example")),
        new Servicio("reportes", URI.create("https://reportes.example")));

void main() {
  IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
}
