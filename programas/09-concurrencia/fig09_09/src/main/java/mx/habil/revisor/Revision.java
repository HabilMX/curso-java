// fig09_09/src/main/java/mx/habil/revisor/Revision.java

package mx.habil.revisor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import mx.habil.revisor.Respuesta.Respondio;
import mx.habil.revisor.Respuesta.SinRespuesta;

/** Consulta los servicios por HTTP y arma sus resultados. */
final class Revision {

  /** Cuánto se espera para establecer la conexión, antes de decir que no hay respuesta. */
  static final long TIEMPO_CONEXION_MS = 2000;

  /** Cuántas peticiones puede haber a la vez si nadie dice otra cosa. */
  static final int LIMITE_POR_OMISION = 10;

  private Revision() {}

  /** Un cliente HTTP con tiempo de conexión y sin seguir redirecciones. */
  static HttpClient nuevoCliente() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIEMPO_CONEXION_MS))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  /** Consulta un servicio y mide cuánto tarda; ninguna falla de red sale de este método. */
  static Resultado revisar(HttpClient cliente, Servicio servicio) throws InterruptedException {
    HttpRequest peticion =
        HttpRequest.newBuilder(servicio.url())
            .timeout(Duration.ofMillis(servicio.timeoutMs()))
            .GET()
            .build();
    long inicio = System.nanoTime();
    Respuesta respuesta;
    try {
      HttpResponse<Void> http = cliente.send(peticion, BodyHandlers.discarding());
      long tiempoMs = (System.nanoTime() - inicio) / 1_000_000;
      respuesta = new Respondio(http.statusCode(), tiempoMs);
    } catch (HttpTimeoutException e) {
      respuesta = new SinRespuesta("tiempo agotado");
    } catch (ConnectException e) {
      respuesta = new SinRespuesta("sin conexión");
    } catch (IOException e) {
      respuesta = new SinRespuesta("error de red");
    }
    return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
  }

  /**
   * Revisa todos los servicios a la vez, con a lo más {@code limite} peticiones en vuelo.
   *
   * @param servicios los servicios, en el orden en que debe salir el reporte
   * @param limite cuántas peticiones puede haber al mismo tiempo; al menos 1
   * @return los resultados, en el mismo orden que {@code servicios}
   * @throws InterruptedException si alguien interrumpe al programa mientras espera
   */
  static List<Resultado> revisarTodos(List<Servicio> servicios, int limite)
      throws InterruptedException {
    if (limite < 1) {
      throw new IllegalArgumentException("el límite debe ser al menos 1");
    }
    var semaforo = new Semaphore(limite);
    try (var cliente = nuevoCliente();
        var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
      var tareas = new ArrayList<Callable<Resultado>>();
      for (Servicio servicio : servicios) {
        tareas.add(
            () -> {
              semaforo.acquire();
              try {
                return revisar(cliente, servicio);
              } finally {
                semaforo.release();
              }
            });
      }
      var resultados = new ArrayList<Resultado>();
      for (Future<Resultado> futuro : ejecutor.invokeAll(tareas)) {
        try {
          resultados.add(futuro.get());
        } catch (ExecutionException e) {
          throw new IllegalStateException("la revisión de un servicio falló", e.getCause());
        }
      }
      return resultados;
    }
  }
}
