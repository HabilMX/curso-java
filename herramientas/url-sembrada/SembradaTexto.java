// herramientas/url-sembrada/SembradaTexto.java
//
// La prueba de la URL sembrada para el revisor que lee servicios.txt (lecciones 5 a 7).
// No es parte de ninguna lección: la compila verificar-url-sembrada.sh junto con el código
// del revisor, y le antepone el «package» cuando el revisor lo tiene (lecciones 6 y 7).
// Comprueba dos cosas: que una URL con usuario o clave se rechace al cargar, y que ningún
// mensaje de error (ni el de sus causas) repita usuario, clave, consulta ni fragmento.

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SembradaTexto {

  static final String CON_CLAVE = "http://usuario:clave@127.0.0.1:1/estado?token=SECRETO#x";
  static final String MAL_ESCRITA = "http://usuario:clave@127.0.0.1:1/es|tado?token=SECRETO#x";
  static final List<String> SECRETOS = List.of("usuario:clave", "SECRETO", "token=", "#x");

  private SembradaTexto() {}

  public static void main(String[] args) throws Exception {
    Path carpeta = Files.createTempDirectory("url-sembrada");
    List<String> fallas = new ArrayList<>();
    int comprobaciones = 0;
    for (String url : List.of(CON_CLAVE, MAL_ESCRITA)) {
      Path ruta = carpeta.resolve("servicios.txt");
      Files.writeString(ruta, "sembrado  " + url + "\n");
      comprobaciones += 2;
      try {
        Configuracion.leer(ruta);
        fallas.add("aceptó al cargar una URL con usuario y clave");
      } catch (ErrorDeConfiguracion e) {
        String repetido = secretoEn(e);
        if (repetido != null) {
          fallas.add("el error repite «" + repetido + "»: " + e.getMessage());
        }
      }
      Files.delete(ruta);
    }
    Files.delete(carpeta);
    terminar(comprobaciones, fallas);
  }

  /** El primer secreto que aparezca en el error o en cualquiera de sus causas; o null. */
  static String secretoEn(Throwable error) {
    for (Throwable t = error; t != null; t = t.getCause()) {
      String texto = t.toString();
      for (String secreto : SECRETOS) {
        if (texto.contains(secreto)) {
          return secreto;
        }
      }
    }
    return null;
  }

  static void terminar(int comprobaciones, List<String> fallas) {
    for (String falla : fallas) {
      System.out.println("  ❌ " + falla);
    }
    System.out.println(
        "  comprobaciones: " + comprobaciones + " · en verde: " + (comprobaciones - fallas.size()));
    System.exit(fallas.isEmpty() ? 0 : 1);
  }
}
