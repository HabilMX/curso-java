// fig05_07/ErrorDeConfiguracion.java
final class ErrorDeConfiguracion extends Exception {
  ErrorDeConfiguracion(String mensaje) {
    super(mensaje);
  }

  ErrorDeConfiguracion(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
