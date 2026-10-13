// fig09_02.java
static final int TAREAS = 1_000;
static final int ESPERA_MS = 200;

void esperar() {
  try {
    Thread.sleep(ESPERA_MS);
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
  }
}

long medirMs(ExecutorService ejecutor) {
  long inicio = System.nanoTime();
  try (ejecutor) {
    for (int i = 0; i < TAREAS; i++) {
      ejecutor.submit(this::esperar);
    }
  }
  return (System.nanoTime() - inicio) / 1_000_000;
}

void main() {
  long conPool = medirMs(Executors.newFixedThreadPool(50));
  long conVirtuales = medirMs(Executors.newVirtualThreadPerTaskExecutor());
  IO.println("pool de 50 hilos de plataforma, más de 3 s: " + (conPool > 3000));
  IO.println("un hilo virtual por tarea, menos de 1 s: " + (conVirtuales < 1000));
}
