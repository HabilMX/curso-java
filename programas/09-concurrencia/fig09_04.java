// fig09_04.java
final AtomicInteger activas = new AtomicInteger();
final AtomicInteger maximo = new AtomicInteger();

void trabajar() throws InterruptedException {
  int ahora = activas.incrementAndGet();
  maximo.accumulateAndGet(ahora, Math::max);
  Thread.sleep(100);
  activas.decrementAndGet();
}

void main() throws Exception {
  var semaforo = new Semaphore(2);
  try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 6; i++) {
      ejecutor.submit(
          () -> {
            semaforo.acquire();
            try {
              trabajar();
            } finally {
              semaforo.release();
            }
            return null;
          });
    }
  }
  IO.println("tareas lanzadas: 6, máximo al mismo tiempo: " + maximo.get());
}
