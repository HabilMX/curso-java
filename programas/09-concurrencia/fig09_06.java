// fig09_06.java
void main() {
  var porEstado = new ConcurrentHashMap<String, Integer>();
  var total = new AtomicInteger();
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
      ejecutor.submit(
          () -> {
            porEstado.merge("ARRIBA", 1, Integer::sum);
            total.incrementAndGet();
          });
    }
  }
  IO.println(porEstado);
  IO.println("total atómico: " + total.get());
}
