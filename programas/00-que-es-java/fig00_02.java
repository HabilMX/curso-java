// fig00_02.java
import java.util.ArrayList;

void main() {
  var palabras = new ArrayList<String>();
  var numeros = new ArrayList<Integer>();
  IO.println(palabras.getClass() == numeros.getClass());
  IO.println(palabras.getClass().getName());
}
