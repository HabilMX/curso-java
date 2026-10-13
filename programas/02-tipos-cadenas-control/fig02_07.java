// fig02_07.java
void main() {
  String linea = "  catalogo  ";
  linea.strip();
  IO.println("[" + linea + "]");

  linea = linea.strip();
  IO.println("[" + linea + "]");

  IO.println("longitud:    " + linea.length());
  IO.println("mayúsculas:  " + linea.toUpperCase());
  IO.println("¿contiene 'log'? " + linea.contains("log"));
  IO.println("¿empieza con 'cat'? " + linea.startsWith("cat"));
  IO.println("recorte:     " + linea.substring(0, 3));
  IO.println("¿vacía?      " + "   ".isBlank());
}
