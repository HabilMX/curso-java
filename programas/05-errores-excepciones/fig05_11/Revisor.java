// fig05_11/Revisor.java
void main() throws InterruptedException {
  Path ruta = Path.of("servicios.txt");
  try {
    List<Servicio> servicios = Configuracion.leer(ruta);
    IO.println(Formato.tabla(Reporte.de(Revision.revisarTodos(servicios))));
  } catch (ErrorDeConfiguracion e) {
    System.err.println("error de configuración: " + e.getMessage());
  }
}
