// fig06_02/src/mx/habil/revisor/Hola.java

package mx.habil.revisor;

/** Una clase mínima dentro de un paquete. */
public final class Hola {

  private Hola() {}

  /** Imprime el nombre completo de su propia clase. */
  public static void main(String[] args) {
    System.out.println("hola desde " + Hola.class.getName());
  }
}
