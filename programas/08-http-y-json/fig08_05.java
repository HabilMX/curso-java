// fig08_05.java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

void main() throws Exception {
  JsonNode raiz = new ObjectMapper().readTree("{\"servicios\": []}");
  IO.println(raiz.get("servicios").size());
}
