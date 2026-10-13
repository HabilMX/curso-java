// fig10_04/src/main/java/mx/habil/revisor/Revisor.java

package mx.habil.revisor;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/** El revisor: consulta los servicios de la configuración y entrega el reporte. */
public final class Revisor {

  static final int CODIGO_TODO_ARRIBA = 0;
  static final int CODIGO_HAY_PROBLEMAS = 1;
  static final int CODIGO_USO_INVALIDO = 2;

  private static final String AYUDA =
      """
      uso: revisor [opciones]

        --config RUTA      archivo de servicios (por omisión servicios.json)
        --formato F        tabla o json (por omisión tabla)
        --concurrencia N   revisiones a la vez, al menos 1 (por omisión {limite})
        --permitir H1,H2   servidores permitidos (por omisión 127.0.0.1,localhost)
        --servir PUERTO    en vez de imprimir, sirve /reporte en 127.0.0.1:PUERTO
        --version          imprime la versión
        --ayuda            imprime esta ayuda

      códigos de salida: 0 todo arriba · 1 hay un servicio lento o caído · 2 uso o configuración
      inválidos
      """;

  private Revisor() {}

  /**
   * Punto de entrada: corre el revisor y sale con su código.
   *
   * @param args las opciones de la línea de comandos; {@code --ayuda} las lista
   */
  public static void main(String[] args) {
    System.exit(ejecutar(args, System.out, System.err));
  }

  static int ejecutar(String[] args, PrintStream salida, PrintStream errores) {
    Opciones opciones;
    try {
      opciones = Opciones.leer(args);
    } catch (ErrorDeUso e) {
      errores.println("error de uso: " + e.getMessage());
      errores.println("prueba: revisor --ayuda");
      return CODIGO_USO_INVALIDO;
    }
    if (opciones.ayuda()) {
      salida.print(ayuda());
      return CODIGO_TODO_ARRIBA;
    }
    if (opciones.version()) {
      salida.println("revisor " + version());
      return CODIGO_TODO_ARRIBA;
    }
    List<Servicio> servicios;
    try {
      servicios = Configuracion.leer(opciones.config(), opciones.permitidos());
    } catch (ErrorDeConfiguracion e) {
      errores.println("error de configuración: " + e.getMessage());
      return CODIGO_USO_INVALIDO;
    }
    try {
      if (opciones.puerto().isPresent()) {
        return servir(opciones.puerto().getAsInt(), servicios, opciones, salida);
      }
      Reporte reporte = revisar(servicios, opciones);
      boolean comoJson = opciones.formato().equals("json");
      salida.println(comoJson ? Formato.json(reporte) : Formato.tabla(reporte));
      boolean todoArriba = reporte.resumen().arriba() == reporte.resumen().total();
      return todoArriba ? CODIGO_TODO_ARRIBA : CODIGO_HAY_PROBLEMAS;
    } catch (IOException e) {
      errores.println("no se pudo abrir el puerto: " + e.getMessage());
      return CODIGO_USO_INVALIDO;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      errores.println("revisión interrumpida");
      return CODIGO_HAY_PROBLEMAS;
    }
  }

  private static Reporte revisar(List<Servicio> servicios, Opciones opciones)
      throws InterruptedException {
    return Reporte.de(Revision.revisarTodos(servicios, opciones.concurrencia()));
  }

  private static int servir(
      int puerto, List<Servicio> servicios, Opciones opciones, PrintStream salida)
      throws IOException, InterruptedException {
    HttpServer servidor = Servidor.iniciar(puerto, () -> revisar(servicios, opciones));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  servidor.stop(1);
                  salida.println("servidor detenido");
                }));
    salida.println("sirviendo http://127.0.0.1:" + puerto + "/reporte (Ctrl+C para detener)");
    new CountDownLatch(1).await();
    return CODIGO_TODO_ARRIBA;
  }

  static String ayuda() {
    return AYUDA.replace("{limite}", String.valueOf(Revision.LIMITE_POR_OMISION));
  }

  static String version() {
    Properties propiedades = new Properties();
    try (InputStream flujo = Revisor.class.getResourceAsStream("/revisor.properties")) {
      if (flujo != null) {
        propiedades.load(flujo);
      }
    } catch (IOException e) {
      return "desconocida";
    }
    return propiedades.getProperty("version", "desconocida");
  }
}
