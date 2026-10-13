// fig07_01/src/test/java/mx/habil/revisor/FormatoTest.java

package mx.habil.revisor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pruebas de cómo se escribe el tiempo en el reporte. */
class FormatoTest {

  @ParameterizedTest
  @CsvSource({"87, 87ms", "999, 999ms", "1000, 1.0s", "2300, 2.3s"})
  void formateaElTiempo(long tiempoMs, String esperado) {
    assertEquals(esperado, Formato.formatearTiempo(tiempoMs));
  }
}
