// fig03_16/Revisor.java
Respuesta simular(String nombre) {
  return switch (nombre) {
    case "catalogo" -> new Respuesta.Respondio(200, 142);
    case "inventario" -> new Respuesta.Respondio(200, 2300);
    case "pagos" -> new Respuesta.Respondio(200, 87);
    default -> new Respuesta.SinRespuesta("sin conexión");
  };
}

Resultado revisar(Servicio servicio) {
  Respuesta respuesta = simular(servicio.nombre());
  return new Resultado(servicio, Clasificador.clasificar(respuesta), respuesta);
}

void main() {
  Servicio catalogo = new Servicio("catalogo", URI.create("https://catalogo.example"));
  Servicio inventario = new Servicio("inventario", URI.create("https://inventario.example"));
  Servicio pagos = new Servicio("pagos", URI.create("https://pagos.example"));
  Servicio reportes = new Servicio("reportes", URI.create("https://reportes.example"));

  IO.println(Formato.encabezado());
  IO.println(Formato.linea(revisar(catalogo)));
  IO.println(Formato.linea(revisar(inventario)));
  IO.println(Formato.linea(revisar(pagos)));
  IO.println(Formato.linea(revisar(reportes)));
}
