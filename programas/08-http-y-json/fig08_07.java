// fig08_07.java
import tools.jackson.core.JacksonException;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

record Config(String nombre, int timeoutMs) {}

void probar(JsonMapper mapeador, String valor) {
  String json = "{\"nombre\": \"pagos\", \"timeout_ms\": " + valor + "}";
  try {
    Config config = mapeador.readValue(json, Config.class);
    IO.println("timeout_ms: " + valor + " -> entra como " + config.timeoutMs());
  } catch (JacksonException e) {
    IO.println("timeout_ms: " + valor + " -> rechazado (" + e.getClass().getSimpleName() + ")");
  }
}

void main() {
  JsonMapper mapeador =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
  probar(mapeador, "1000");
  probar(mapeador, "1.5");
  probar(mapeador, "\"1000\"");
  probar(mapeador, "true");
  probar(mapeador, "-5");
}
