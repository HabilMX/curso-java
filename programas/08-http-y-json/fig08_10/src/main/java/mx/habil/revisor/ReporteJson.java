// fig08_10/src/main/java/mx/habil/revisor/ReporteJson.java

package mx.habil.revisor;

import java.net.URI;
import java.util.List;
import java.util.OptionalLong;

/** La forma del reporte en JSON: es el contrato con quien lo consuma. */
record ReporteJson(List<Fila> servicios, ResumenJson resumen) {

  ReporteJson {
    servicios = List.copyOf(servicios);
  }

  /** Un servicio en el JSON; {@code tiempoMs} es nulo si no hubo respuesta. */
  record Fila(String nombre, String url, Estado estado, Long tiempoMs, String detalle) {}

  /** Las cuentas del JSON; {@code promedioMs} es nulo si nadie respondió. */
  record ResumenJson(int total, int arriba, int lento, int caido, Long promedioMs) {}

  /** Convierte el reporte del programa en su forma de JSON. */
  static ReporteJson de(Reporte reporte) {
    List<Fila> filas =
        reporte.resultados().stream()
            .map(
                r ->
                    new Fila(
                        r.servicio().nombre(),
                        sinSecretos(r.servicio().url()),
                        r.estado(),
                        nulo(r.tiempoMs()),
                        r.detalle()))
            .toList();
    Resumen resumen = reporte.resumen();
    return new ReporteJson(
        filas,
        new ResumenJson(
            resumen.total(),
            resumen.arriba(),
            resumen.lento(),
            resumen.caido(),
            nulo(resumen.promedioMs())));
  }

  /**
   * La dirección sin usuario, clave, consulta ni fragmento: solo esquema, servidor, puerto y
   * ruta. El reporte lo lee quien lo pida, y una consulta puede traer un token.
   */
  static String sinSecretos(URI url) {
    String puerto = url.getPort() == -1 ? "" : ":" + url.getPort();
    String ruta = url.getRawPath() == null ? "" : url.getRawPath();
    return url.getScheme() + "://" + url.getHost() + puerto + ruta;
  }

  private static Long nulo(OptionalLong valor) {
    return valor.isPresent() ? valor.getAsLong() : null;
  }
}
