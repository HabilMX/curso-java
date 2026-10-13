// fig08_06.java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

void main() {
  String texto = """
      {
        "servicios": [
          {"nombre": "catalogo", "url": "http://127.0.0.1:8089/catalogo"},
          {"nombre": "pagos", "url": "http://127.0.0.1:8089/pagos", "timeout_ms": 1000}
        ]
      }
      """;
  JsonMapper mapeador = JsonMapper.builder().build();
  JsonNode raiz = mapeador.readTree(texto);

  JsonNode lista = raiz.get("servicios");
  IO.println("¿es lista? " + lista.isArray() + ", elementos: " + lista.size());

  JsonNode segundo = lista.get(1);
  IO.println("nombre: " + segundo.get("nombre").stringValue());
  IO.println("timeout_ms: " + segundo.get("timeout_ms").intValue());

  IO.println("llave que no existe -> " + lista.get(0).get("timeout_ms"));
  IO.println("la misma con path()  -> " + lista.get(0).path("timeout_ms").isMissingNode());
}
