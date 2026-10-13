// fig09_09/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.List;

/** El revisor: lee servicios.json, consulta cada servicio y escribe la tabla. */
public final class Revisor {

  private Revisor() {}

  /**
   * Punto de entrada del programa.
   *
   * @param args el primero, si lo hay, es el archivo de configuración
   * @throws InterruptedException si alguien interrumpe al programa mientras espera
   */
  public static void main(String[] args) throws InterruptedException {
    List<Servicio> servicios;
    try {
      Path ruta = args.length > 0 ? Path.of(args[0]) : Path.of("servicios.json");
      servicios = Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS);
    } catch (ErrorDeConfiguracion e) {
      System.err.println("error de configuración: " + e.getMessage());
      return;
    }
    var resultados = Revision.revisarTodos(servicios, Revision.LIMITE_POR_OMISION);
    System.out.println(Formato.tabla(Reporte.de(resultados)));
  }
}
