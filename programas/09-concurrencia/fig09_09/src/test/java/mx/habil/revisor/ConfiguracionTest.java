// fig09_09/src/test/java/mx/habil/revisor/ConfiguracionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Pruebas de la lectura y validación de servicios.json. */
class ConfiguracionTest {

  @TempDir Path carpeta;

  private Path escribir(String json) throws IOException {
    Path ruta = carpeta.resolve("servicios.json");
    Files.writeString(ruta, json);
    return ruta;
  }

  @Test
  void leeUnArchivoValido() throws Exception {
    Path ruta =
        escribir(
            """
            {"servicios": [
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
              {"nombre": "pagos", "url": "http://localhost:8089/pagos", "timeout_ms": 1000}
            ]}
            """);

    List<Servicio> servicios = Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS);

    assertEquals(2, servicios.size());
    assertEquals("catalogo", servicios.get(0).nombre());
    assertEquals(Servicio.TIMEOUT_POR_OMISION_MS, servicios.get(0).timeoutMs());
    assertEquals(1000, servicios.get(1).timeoutMs());
  }

  static Stream<Arguments> archivosInvalidos() {
    return Stream.of(
        Arguments.of(
            """
            {"servicios": [{"nombre": "a", "url": "ftp://127.0.0.1/a"}]}
            """,
            "servicio 1: 'url' debe empezar con http:// o https://"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a"},
              {"nombre": "b", "url": "http://169.254.169.254/latest/meta-data/"}
            ]}
            """,
            "servicio 2: el servidor 169.254.169.254 no está en la lista permitida"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": 1.5}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "a", "url": "http://127.0.0.1:8089/a", "timeout_ms": "1000"}
            ]}
            """,
            "servicio 1: 'timeout_ms' debe ser un entero mayor que 0"),
        Arguments.of(
            """
            {"servicios": [{"url": "http://127.0.0.1:8089/a"}]}
            """,
            "servicio 1: falta 'nombre'"),
        Arguments.of(
            """
            {"servicios": [
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/a"},
              {"nombre": "catalogo", "url": "http://127.0.0.1:8089/b"}
            ]}
            """,
            "nombres repetidos: catalogo"));
  }

  @ParameterizedTest
  @MethodSource("archivosInvalidos")
  void rechazaUnArchivoInvalido(String json, String mensajeEsperado) throws IOException {
    Path ruta = escribir(json);

    ErrorDeConfiguracion error =
        assertThrows(
            ErrorDeConfiguracion.class,
            () -> Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS));

    assertEquals(mensajeEsperado, error.getMessage());
  }
}
