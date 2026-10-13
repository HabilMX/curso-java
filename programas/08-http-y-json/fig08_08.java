// fig08_08.java
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

record Fila(String nombre, String estado, Long tiempoMs, String detalle) {}

record Resumen(int total, Long promedioMs) {}

record Reporte(List<Fila> servicios, Resumen resumen) {}

void main() {
  JsonMapper mapeador =
      JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
  Reporte reporte =
      new Reporte(
          List.of(
              new Fila("pagos", "ARRIBA", 87L, "200"),
              new Fila("reportes", "CAIDO", null, "sin conexión")),
          new Resumen(2, 87L));
  IO.println(mapeador.writerWithDefaultPrettyPrinter().writeValueAsString(reporte));
}
