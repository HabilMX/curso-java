// fig09_05.java
final AtomicInteger activas = new AtomicInteger();
final AtomicInteger maximo = new AtomicInteger();

void trabajar() {
  int ahora = activas.incrementAndGet();
  maximo.accumulateAndGet(ahora, Math::max);
  try {
    Thread.sleep(100);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
  activas.decrementAndGet();
}

int maximoCon(ExecutorService ejecutor) {
  maximo.set(0);
  try (ejecutor) {
    for (int i = 0; i < 6; i++) {
      ejecutor.submit(this::trabajar);
    }
  }
  return maximo.get();
}

void main() {
  var fabrica = Thread.ofVirtual().factory();
  int conPool = maximoCon(Executors.newFixedThreadPool(2, fabrica));
  int sinPool = maximoCon(Executors.newVirtualThreadPerTaskExecutor());
  IO.println("pool fijo de 2 hilos virtuales: " + conPool);
  IO.println("un hilo virtual por tarea: " + sinPool);
}
