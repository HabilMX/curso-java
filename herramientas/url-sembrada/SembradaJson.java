// herramientas/url-sembrada/SembradaJson.java
//
// La prueba de la URL sembrada para el revisor que lee servicios.json (lecciones 8 a 10).
// No es parte de ninguna lección: la compila verificar-url-sembrada.sh junto con el código
// del revisor y con Jackson en el classpath. Comprueba tres cosas: que una URL con usuario o
// clave se rechace al cargar, que ningún mensaje de error (ni el de sus causas) la repita, y
// que el reporte, en tabla y en JSON, no repita la consulta ni el fragmento de una URL que sí
// se acepta: el JSON conserva el campo "url", redactado a esquema, servidor, puerto y ruta.

package mx.habil.revisor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SembradaJson {

  static final String CON_CLAVE = "http://usuario:clave@127.0.0.1:1/estado?token=SECRETO#x";
  static final String MAL_ESCRITA = "http://usuario:clave@127.0.0.1:1/es|tado?token=SECRETO#x";
  static final String CON_TOKEN = "http://127.0.0.1:1/estado?token=SECRETO#x";
  static final String REDACTADA = "\"url\" : \"http://127.0.0.1:1/estado\"";
  static final List<String> SECRETOS = List.of("usuario:clave", "SECRETO", "token=", "#x");

  private SembradaJson() {}

  public static void main(String[] args) throws Exception {
    Path carpeta = Files.createTempDirectory("url-sembrada");
    Path ruta = carpeta.resolve("servicios.json");
    List<String> fallas = new ArrayList<>();
    int comprobaciones = 0;
    for (String url : List.of(CON_CLAVE, MAL_ESCRITA)) {
      escribir(ruta, url);
      comprobaciones += 2;
      try {
        Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS);
        fallas.add("aceptó al cargar una URL con usuario y clave");
      } catch (ErrorDeConfiguracion e) {
        String repetido = secretoEn(e);
        if (repetido != null) {
          fallas.add("el error repite «" + repetido + "»: " + e.getMessage());
        }
      }
    }
    escribir(ruta, CON_TOKEN);
    comprobaciones += 3;
    try {
      Servicio servicio = Configuracion.leer(ruta, Configuracion.SERVIDORES_PERMITIDOS).get(0);
      Reporte reporte =
          Reporte.de(
              List.of(
                  new Resultado(
                      servicio, Estado.CAIDO, new Respuesta.SinRespuesta("sin conexión"))));
      String json = Formato.json(reporte);
      String tabla = Formato.tabla(reporte);
      if (secretoEn(json) != null) {
        fallas.add("el JSON repite «" + secretoEn(json) + "»");
      }
      if (!json.contains(REDACTADA)) {
        fallas.add("el JSON no trae la URL redactada " + REDACTADA);
      }
      if (secretoEn(tabla) != null) {
        fallas.add("la tabla repite «" + secretoEn(tabla) + "»");
      }
    } catch (ErrorDeConfiguracion e) {
      fallas.add("rechazó una URL sin usuario ni clave: " + e.getMessage());
    }
    Files.delete(ruta);
    Files.delete(carpeta);
    for (String falla : fallas) {
      System.out.println("  ❌ " + falla);
    }
    System.out.println(
        "  comprobaciones: " + comprobaciones + " · en verde: " + (comprobaciones - fallas.size()));
    System.exit(fallas.isEmpty() ? 0 : 1);
  }

  static void escribir(Path ruta, String url) throws Exception {
    Files.writeString(
        ruta, "{\"servicios\": [{\"nombre\": \"sembrado\", \"url\": \"" + url + "\"}]}\n");
  }

  static String secretoEn(String texto) {
    for (String secreto : SECRETOS) {
      if (texto.contains(secreto)) {
        return secreto;
      }
    }
    return null;
  }

  static String secretoEn(Throwable error) {
    for (Throwable t = error; t != null; t = t.getCause()) {
      String repetido = secretoEn(t.toString());
      if (repetido != null) {
        return repetido;
      }
    }
    return null;
  }
}
