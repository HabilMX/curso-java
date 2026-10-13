// fig03_07.java
enum Estado { ARRIBA, LENTO, CAIDO }

String aviso(Estado estado) {
  return switch (estado) {
    case ARRIBA -> "todo bien";
    case LENTO -> "revisar pronto";
  };
}

void main() {
  IO.println(aviso(Estado.CAIDO));
}
