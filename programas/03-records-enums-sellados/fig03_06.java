// fig03_06.java
enum Estado { ARRIBA, LENTO, CAIDO }

enum Severidad {
  BAJA(1), MEDIA(5), ALTA(10);

  final int peso;

  Severidad(int peso) {
    this.peso = peso;
  }

  boolean urgente() {
    return peso >= 5;
  }
}

String aviso(Estado estado) {
  return switch (estado) {
    case ARRIBA -> "todo bien";
    case LENTO -> "revisar pronto";
    case CAIDO -> "avisar ya";
  };
}

void main() {
  Estado estado = Estado.LENTO;
  IO.println(estado);
  IO.println("name():    " + estado.name());
  IO.println("ordinal(): " + estado.ordinal());
  IO.println("== :       " + (estado == Estado.LENTO));
  IO.println("valueOf:   " + Estado.valueOf("CAIDO"));
  IO.println("values():  " + Arrays.toString(Estado.values()));
  IO.println(aviso(estado));
  IO.println("ALTA pesa " + Severidad.ALTA.peso + " y urgente = " + Severidad.ALTA.urgente());
  IO.println("BAJA urgente = " + Severidad.BAJA.urgente());
}
