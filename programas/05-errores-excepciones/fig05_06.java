// fig05_06.java
record Recurso(String nombre, boolean falla) implements AutoCloseable {
  Recurso {
    IO.println("abro " + nombre);
  }

  @Override
  public void close() {
    IO.println("cierro " + nombre);
    if (falla) {
      throw new IllegalStateException("no pude cerrar " + nombre);
    }
  }
}

void main() {
  try (Recurso entrada = new Recurso("entrada", false);
      Recurso salida = new Recurso("salida", false)) {
    IO.println("trabajo con " + entrada.nombre() + " y " + salida.nombre());
  }
  IO.println("--");
  try (Recurso fragil = new Recurso("frágil", true)) {
    IO.println("trabajo con " + fragil.nombre());
    throw new IllegalArgumentException("falló el trabajo");
  } catch (IllegalArgumentException e) {
    IO.println("atrapé: " + e.getMessage());
    for (Throwable suprimida : e.getSuppressed()) {
      IO.println("  suprimida: " + suprimida.getMessage());
    }
  }
}
