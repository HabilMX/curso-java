// fig04_12.java
record Medicion(String nombre, long tiempoMs) {}

void main() {
  Predicate<Medicion> esLenta = medicion -> medicion.tiempoMs() >= 1000;
  Function<Medicion, String> etiqueta = medicion -> medicion.nombre() + ":" + medicion.tiempoMs();
  Function<Medicion, String> soloNombre = Medicion::nombre;
  Supplier<String> saludo = () -> "revisando...";

  Medicion inventario = new Medicion("inventario", 2300);
  Medicion pagos = new Medicion("pagos", 87);
  IO.println(esLenta.test(inventario));
  IO.println(esLenta.test(pagos));
  IO.println(etiqueta.apply(pagos));
  IO.println(soloNombre.apply(inventario));
  IO.println(saludo.get());
}
