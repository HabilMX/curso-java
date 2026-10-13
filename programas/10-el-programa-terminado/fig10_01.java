// fig10_01.java
void main(String[] args) {
  IO.println("recibí " + args.length + " argumentos: " + Arrays.toString(args));
  if (args.length == 1 && args[0].equals("--falla")) {
    System.err.println("error de uso: pediste fallar");
    System.exit(2);
  }
  IO.println("termino bien");
}
