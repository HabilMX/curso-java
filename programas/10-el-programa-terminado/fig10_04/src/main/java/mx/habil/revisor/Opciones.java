// fig10_04/src/main/java/mx/habil/revisor/Opciones.java

package mx.habil.revisor;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/** Lo que la persona pidió en la línea de comandos, ya validado. */
record Opciones(
    Path config,
    String formato,
    int concurrencia,
    Set<String> permitidos,
    OptionalInt puerto,
    boolean ayuda,
    boolean version) {

  Opciones {
    permitidos = Set.copyOf(permitidos);
  }

  static Opciones leer(String[] args) throws ErrorDeUso {
    Path config = Path.of("servicios.json");
    String formato = "tabla";
    int concurrencia = Revision.LIMITE_POR_OMISION;
    Set<String> permitidos = Configuracion.SERVIDORES_PERMITIDOS;
    OptionalInt puerto = OptionalInt.empty();
    boolean ayuda = false;
    boolean version = false;

    Deque<String> pendientes = new ArrayDeque<>(List.of(args));
    while (!pendientes.isEmpty()) {
      String bandera = pendientes.removeFirst();
      switch (bandera) {
        case "--ayuda" -> ayuda = true;
        case "--version" -> version = true;
        case "--config" -> config = Path.of(valor(pendientes, bandera));
        case "--formato" -> formato = formato(valor(pendientes, bandera));
        case "--concurrencia" ->
            concurrencia = entero(valor(pendientes, bandera), bandera, 1, Integer.MAX_VALUE);
        case "--permitir" -> permitidos = servidores(valor(pendientes, bandera));
        case "--servir" ->
            puerto = OptionalInt.of(entero(valor(pendientes, bandera), bandera, 1, 65535));
        default -> throw new ErrorDeUso("opción desconocida: " + bandera);
      }
    }
    return new Opciones(config, formato, concurrencia, permitidos, puerto, ayuda, version);
  }

  private static String valor(Deque<String> pendientes, String bandera) throws ErrorDeUso {
    if (pendientes.isEmpty()) {
      throw new ErrorDeUso("falta el valor de " + bandera);
    }
    return pendientes.removeFirst();
  }

  private static String formato(String texto) throws ErrorDeUso {
    if (!texto.equals("tabla") && !texto.equals("json")) {
      throw new ErrorDeUso("--formato debe ser tabla o json, no " + texto);
    }
    return texto;
  }

  private static int entero(String texto, String bandera, int minimo, int maximo)
      throws ErrorDeUso {
    try {
      int numero = Integer.parseInt(texto);
      if (numero >= minimo && numero <= maximo) {
        return numero;
      }
    } catch (NumberFormatException e) {
      // cae al error de abajo: un texto que no es número y un número fuera de rango dicen lo mismo
    }
    String rango =
        maximo == Integer.MAX_VALUE
            ? "mayor o igual que " + minimo
            : "entre " + minimo + " y " + maximo;
    throw new ErrorDeUso(bandera + " debe ser un entero " + rango + ", no " + texto);
  }

  private static Set<String> servidores(String texto) throws ErrorDeUso {
    Set<String> servidores =
        Arrays.stream(texto.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    if (servidores.isEmpty()) {
      throw new ErrorDeUso("--permitir necesita al menos un servidor");
    }
    return servidores;
  }
}
