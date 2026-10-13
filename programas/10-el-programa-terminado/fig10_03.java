// fig10_03.java
void main() throws InterruptedException {
  Runtime.getRuntime()
      .addShutdownHook(new Thread(() -> IO.println("gancho: cerrando con orden")));
  IO.println("trabajando...");
  Thread.sleep(300);
  IO.println("main termina");
}
