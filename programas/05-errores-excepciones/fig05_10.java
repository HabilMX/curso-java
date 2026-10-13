// fig05_10.java
void main() {
  Thread.currentThread().interrupt();
  IO.println("¿marcado como interrumpido? " + Thread.currentThread().isInterrupted());
  try {
    Thread.sleep(1000);
    IO.println("esto no se imprime");
  } catch (InterruptedException e) {
    IO.println("sleep lanzó: " + e.getMessage());
    IO.println("¿sigue marcado? " + Thread.currentThread().isInterrupted());
    Thread.currentThread().interrupt();
    IO.println("¿y después de restaurarlo? " + Thread.currentThread().isInterrupted());
  }
}
