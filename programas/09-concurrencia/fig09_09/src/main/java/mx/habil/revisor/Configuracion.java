// fig09_09/src/main/java/mx/habil/revisor/Configuracion.java

package mx.habil.revisor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.JsonNode;

/** Lee la lista de servicios de servicios.json y desconfía de todo lo que trae. */
final class Configuracion {

  /** Los únicos servidores a los que el revisor acepta consultar. */
  static final Set<String> SERVIDORES_PERMITIDOS = Set.of("127.0.0.1", "localhost");

  private Configuracion() {}

  /** Lee y valida el archivo completo; cualquier problema sale como un solo tipo de error. */
  static List<Servicio> leer(Path ruta, Set<String> permitidos) throws ErrorDeConfiguracion {
    String nombreArchivo = String.valueOf(ruta.getFileName());
    JsonNode raiz = leerArbol(ruta, nombreArchivo);
    JsonNode lista = raiz.get("servicios");
    if (lista == null || !lista.isArray()) {
      throw new ErrorDeConfiguracion("falta la lista 'servicios'");
    }
    List<Servicio> servicios = new ArrayList<>();
    for (int i = 0; i < lista.size(); i++) {
      servicios.add(leerServicio(lista.get(i), i + 1, permitidos));
    }
    verificarNombresUnicos(servicios);
    return servicios;
  }

  private static JsonNode leerArbol(Path ruta, String nombreArchivo) throws ErrorDeConfiguracion {
    String texto;
    try {
      texto = Files.readString(ruta, StandardCharsets.UTF_8);
    } catch (NoSuchFileException e) {
      throw new ErrorDeConfiguracion("no existe el archivo " + nombreArchivo, e);
    } catch (IOException e) {
      throw new ErrorDeConfiguracion("no se pudo leer el archivo " + nombreArchivo, e);
    }
    try {
      return Json.MAPEADOR.readTree(texto);
    } catch (JacksonException e) {
      TokenStreamLocation lugar = e.getLocation();
      String donde =
          lugar == null
              ? ""
              : ": renglón %d, columna %d".formatted(lugar.getLineNr(), lugar.getColumnNr());
      throw new ErrorDeConfiguracion(nombreArchivo + " no es JSON válido" + donde, e);
    }
  }

  /** Valida un elemento de la lista y lo convierte en un servicio. */
  static Servicio leerServicio(JsonNode nodo, int posicion, Set<String> permitidos)
      throws ErrorDeConfiguracion {
    String prefijo = "servicio " + posicion + ": ";
    if (!nodo.isObject()) {
      throw new ErrorDeConfiguracion(prefijo + "debe ser un objeto");
    }
    String nombre = leerNombre(nodo, prefijo);
    URI url = leerUrl(nodo, prefijo, permitidos);
    JsonNode timeout = nodo.get("timeout_ms");
    if (timeout == null) {
      return new Servicio(nombre, url);
    }
    if (!timeout.isInt() || timeout.intValue() <= 0) {
      throw new ErrorDeConfiguracion(prefijo + "'timeout_ms' debe ser un entero mayor que 0");
    }
    return new Servicio(nombre, url, timeout.intValue());
  }

  private static String leerNombre(JsonNode nodo, String prefijo) throws ErrorDeConfiguracion {
    JsonNode nombre = nodo.get("nombre");
    if (nombre == null) {
      throw new ErrorDeConfiguracion(prefijo + "falta 'nombre'");
    }
    if (!nombre.isString() || nombre.stringValue().isBlank()) {
      throw new ErrorDeConfiguracion(
          prefijo + "'nombre' debe ser un texto que no esté vacío");
    }
    return nombre.stringValue();
  }

  private static URI leerUrl(JsonNode nodo, String prefijo, Set<String> permitidos)
      throws ErrorDeConfiguracion {
    JsonNode campo = nodo.get("url");
    if (campo == null) {
      throw new ErrorDeConfiguracion(prefijo + "falta 'url'");
    }
    if (!campo.isString()) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    URI url;
    try {
      url = new URI(campo.stringValue());
    } catch (URISyntaxException _) {
      // Sin la causa: su mensaje repite la dirección entera, con usuario, clave y token.
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    String esquema = url.getScheme();
    if (!"http".equals(esquema) && !"https".equals(esquema)) {
      throw new ErrorDeConfiguracion(prefijo + "'url' debe empezar con http:// o https://");
    }
    String servidor = url.getHost();
    if (servidor == null) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no es válida");
    }
    if (!permitidos.contains(servidor)) {
      throw new ErrorDeConfiguracion(
          prefijo + "el servidor " + servidor + " no está en la lista permitida");
    }
    if (url.getRawUserInfo() != null) {
      throw new ErrorDeConfiguracion(prefijo + "'url' no puede llevar usuario ni clave");
    }
    return url;
  }

  private static void verificarNombresUnicos(List<Servicio> servicios)
      throws ErrorDeConfiguracion {
    Set<String> vistos = new HashSet<>();
    Set<String> repetidos = new LinkedHashSet<>();
    for (Servicio servicio : servicios) {
      if (!vistos.add(servicio.nombre())) {
        repetidos.add(servicio.nombre());
      }
    }
    if (!repetidos.isEmpty()) {
      throw new ErrorDeConfiguracion("nombres repetidos: " + String.join(", ", repetidos));
    }
  }
}
