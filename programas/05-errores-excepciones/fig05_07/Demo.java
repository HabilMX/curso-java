// fig05_07/Demo.java
void leer(Path ruta) throws ErrorDeConfiguracion {
  try {
    Files.readAllLines(ruta);
  } catch (NoSuchFileException e) {
    throw new ErrorDeConfiguracion("no existe el archivo " + ruta.getFileName(), e);
  } catch (IOException e) {
    throw new ErrorDeConfiguracion("no se pudo leer el archivo " + ruta.getFileName(), e);
  }
}

void main() {
  try {
    leer(Path.of("servicios-que-no-existe.txt"));
  } catch (ErrorDeConfiguracion e) {
    IO.println("mensaje: " + e.getMessage());
    IO.println("causa:   " + e.getCause());
    e.printStackTrace();
  }
}
