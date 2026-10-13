// fig05_01.java
void cadena(Class<?> tipo) {
  String texto = tipo.getSimpleName();
  for (Class<?> padre = tipo.getSuperclass(); padre != null; padre = padre.getSuperclass()) {
    texto = texto + " -> " + padre.getSimpleName();
  }
  boolean esExcepcion = Exception.class.isAssignableFrom(tipo);
  boolean esNoVerificada = RuntimeException.class.isAssignableFrom(tipo);
  String clase;
  if (Error.class.isAssignableFrom(tipo)) {
    clase = "error de la JVM";
  } else if (esExcepcion && !esNoVerificada) {
    clase = "verificada";
  } else {
    clase = "no verificada";
  }
  IO.println(texto);
  IO.println("    " + clase);
}

void main() {
  cadena(NoSuchFileException.class);
  cadena(InterruptedException.class);
  cadena(NumberFormatException.class);
  cadena(NullPointerException.class);
  cadena(OutOfMemoryError.class);
}
