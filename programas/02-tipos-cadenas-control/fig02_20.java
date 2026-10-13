// fig02_20.java
static final int UMBRAL_LENTO_MS = 1000;

Integer codigoDe(String nombre) {
  return switch (nombre) {
    case "catalogo", "inventario", "pagos" -> 200;
    default -> null;
  };
}

Long tiempoDe(String nombre) {
  return switch (nombre) {
    case "catalogo" -> 142L;
    case "inventario" -> 2300L;
    case "pagos" -> 87L;
    default -> null;
  };
}

String clasificar(Integer codigo, Long tiempoMs) {
  boolean respondio = codigo != null && tiempoMs != null;
  if (!respondio) {
    return "CAIDO";
  }
  if (codigo >= 400) {
    return "CAIDO";
  }
  if (tiempoMs >= UMBRAL_LENTO_MS) {
    return "LENTO";
  }
  return "ARRIBA";
}

String formatearTiempo(Long tiempoMs) {
  if (tiempoMs == null) {
    return "—";
  }
  if (tiempoMs < UMBRAL_LENTO_MS) {
    return tiempoMs + "ms";
  }
  return String.format(Locale.ROOT, "%.1fs", tiempoMs / 1000.0);
}

String detalle(Integer codigo) {
  if (codigo == null) {
    return "sin conexión";
  }
  return String.valueOf(codigo);
}

String linea(String nombre, String estado, String tiempo, String detalle) {
  return String.format(Locale.ROOT, "%-12s%-10s%6s  %s", nombre, estado, tiempo, detalle);
}

String encabezado() {
  return linea("SERVICIO", "ESTADO", "TIEMPO", "DETALLE");
}

String revisar(String nombre) {
  Integer codigo = codigoDe(nombre);
  Long tiempoMs = tiempoDe(nombre);
  return linea(
      nombre, clasificar(codigo, tiempoMs), formatearTiempo(tiempoMs), detalle(codigo));
}

void main() {
  IO.println(encabezado());
  IO.println(revisar("catalogo"));
  IO.println(revisar("inventario"));
  IO.println(revisar("pagos"));
  IO.println(revisar("reportes"));
}
