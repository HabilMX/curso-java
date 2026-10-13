// fig10_04/src/main/java/mx/habil/revisor/ErrorDeUso.java

package mx.habil.revisor;

/** Los argumentos de la línea de comandos no se entienden; el mensaje dice cuál falló. */
final class ErrorDeUso extends Exception {

  private static final long serialVersionUID = 1L;

  ErrorDeUso(String mensaje) {
    super(mensaje);
  }
}
