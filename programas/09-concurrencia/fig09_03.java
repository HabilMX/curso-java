// fig09_03.java
String tarea(String nombre, long esperaMs) throws InterruptedException {
  Thread.sleep(esperaMs);
  return nombre;
}

void main() throws Exception {
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Callable<String>> tareas =
        List.of(
            () -> tarea("lenta", 300),
            () -> tarea("rapida", 50),
            () -> tarea("mediana", 150));

    for (Future<String> futuro : ejecutor.invokeAll(tareas)) {
      IO.println("en el orden de la lista: " + futuro.get());
    }

    List<Callable<String>> conFallo =
        List.of(
            () -> tarea("buena", 10),
            () -> {
              throw new IllegalStateException("fallé a propósito");
            });
    List<Future<String>> futuros = ejecutor.invokeAll(conFallo);
    IO.println("invokeAll regresó; la tarea 2 terminó: " + futuros.get(1).isDone());
    try {
      futuros.get(1).get();
    } catch (ExecutionException e) {
      IO.println("get() lanza " + e.getClass().getSimpleName());
      IO.println("la causa es " + e.getCause());
    }
  }
}
