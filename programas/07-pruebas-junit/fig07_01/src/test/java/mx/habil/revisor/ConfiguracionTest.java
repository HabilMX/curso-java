// fig07_01/src/test/java/mx/habil/revisor/ConfiguracionTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de la lectura de servicios.txt. */
class ConfiguracionTest {

  @TempDir Path carpeta;

  @Test
  void leeUnArchivoValido() throws Exception {
    Path ruta = carpeta.resolve("servicios.txt");
    Files.writeString(
        ruta,
        """
        # nombre  url  [timeout_ms]
        catalogo   https://catalogo.example
        pagos      https://pagos.example  1000
        """);

    List<Servicio> servicios = Configuracion.leer(ruta);

    assertEquals(2, servicios.size());
    assertEquals("catalogo", servicios.get(0).nombre());
    assertEquals(Servicio.TIMEOUT_POR_OMISION_MS, servicios.get(0).timeoutMs());
    assertEquals(1000, servicios.get(1).timeoutMs());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          a https://a.x;b https://b.x;solo | renglón 3: se esperaban 2 o 3 campos y hay 1
          a https://a.x;b ftp://b.x | renglón 2: la url debe empezar con http:// o https://
          a https://a.x;b https://b.x 0 | renglón 2: timeout_ms debe ser un entero mayor que 0
          catalogo https://a.x;catalogo https://b.x | nombres repetidos: catalogo
          """)
  void rechazaUnArchivoInvalido(String contenido, String mensajeEsperado) throws IOException {
    Path ruta = carpeta.resolve("servicios.txt");
    Files.writeString(ruta, contenido.replace(";", "\n"));

    ErrorDeConfiguracion error =
        assertThrows(ErrorDeConfiguracion.class, () -> Configuracion.leer(ruta));

    assertEquals(mensajeEsperado, error.getMessage());
  }
}
