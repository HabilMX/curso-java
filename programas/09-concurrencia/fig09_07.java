// fig09_07.java
static final ScopedValue<String> CORRIDA = ScopedValue.newInstance();

void anotar(String mensaje) {
  IO.println("[corrida " + CORRIDA.get() + "] " + mensaje);
}

void main() throws Exception {
  ScopedValue.where(CORRIDA, "r-001")
      .call(
          () -> {
            anotar("revisando 4 servicios");
            Thread otro =
                Thread.ofVirtual()
                    .start(() -> IO.println("en un hilo nuevo, ligado = " + CORRIDA.isBound()));
            otro.join();
            return null;
          });
  IO.println("fuera del bloque, ligado = " + CORRIDA.isBound());
}
