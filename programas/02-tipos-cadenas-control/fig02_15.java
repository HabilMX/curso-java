// fig02_15.java
void main() {
  IO.println(describir(200));
  IO.println(describir(301));
  IO.println(describir(404));
  IO.println(describir(503));
  IO.println(describir(0));
}

String describir(int codigo) {
  int familia = codigo / 100;
  return switch (familia) {
    case 2 -> "correcto";
    case 3 -> "redirección";
    case 4, 5 -> {
      String quien = familia == 4 ? "cliente" : "servidor";
      yield "error del " + quien;
    }
    default -> "sin clasificar";
  };
}
