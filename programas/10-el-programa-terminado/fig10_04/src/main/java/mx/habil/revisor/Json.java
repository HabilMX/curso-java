// fig10_04/src/main/java/mx/habil/revisor/Json.java

package mx.habil.revisor;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/** El lector y escritor de JSON del revisor: uno solo, configurado una vez. */
final class Json {

  /** Escribe los nombres en snake_case: {@code tiempoMs} sale como {@code tiempo_ms}. */
  static final JsonMapper MAPEADOR =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

  private Json() {}
}
