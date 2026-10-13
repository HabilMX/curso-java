// fig04_22/Revision.java
import java.util.List;

final class Revision {
  static Resultado revisar(Servicio servicio) {
    Respuesta respuesta =
        Simulador.buscar(servicio.nombre()).orElse(new Respuesta.SinRespuesta("sin conexión"));
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  static List<Resultado> revisarTodos(List<Servicio> servicios) {
    return servicios.stream().map(Revision::revisar).toList();
  }
}
