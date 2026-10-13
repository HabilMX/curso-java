// fig09_01.java
void main() throws InterruptedException {
  Thread plataforma =
      Thread.ofPlatform()
          .name("de-plataforma")
          .start(() -> describir("de-plataforma"));
  plataforma.join();

  Thread virtual =
      Thread.ofVirtual()
          .name("virtual-1")
          .start(() -> describir("virtual-1"));
  virtual.join();
}

void describir(String nombre) {
  Thread hilo = Thread.currentThread();
  IO.println(nombre + ": virtual=" + hilo.isVirtual() + ", demonio=" + hilo.isDaemon());
}
