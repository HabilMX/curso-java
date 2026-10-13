#!/usr/bin/env bash
# Compila y ejecuta los programas de las lecciones con el JDK instalado, y compara
# la salida real contra la salida documentada al lado de cada programa.
#
# Tres clases de programa:
#   figNN_NN.java + figNN_NN.salida.txt          -> debe correr y coincidir al carácter
#   figNN_NN.java + figNN_NN.error-esperado.txt  -> NO debe compilar (el error es la lección)
#   la lección 8                                 -> se salta: necesita el laboratorio HTTP
#                                                   encendido y Jackson 3 en el classpath,
#                                                   que la lección manda preparar a mano
# Falla cerrado: si no encuentra programas, sale en error, no en verde.
set -uo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROGRAMAS="$RAIZ/programas"
LIMITE_SEGUNDOS=60
SALTAR_LECCION="08-http-y-json"

command -v java >/dev/null || { echo "❌ no hay java en el PATH"; exit 2; }
[ -d "$PROGRAMAS" ] || { echo "❌ no existe $PROGRAMAS"; exit 2; }

echo "  JDK: $(java --version 2>&1 | head -1)"
echo

correr() { perl -e 'alarm shift; exec @ARGV' "$LIMITE_SEGUNDOS" "$@"; }

ok=0; fallas=0; errores_ok=0; saltados=0; sin_referencia=0
declare -a detalle_fallas=()

for carpeta in "$PROGRAMAS"/*/; do
  leccion="$(basename "$carpeta")"
  if [ "$leccion" = "$SALTAR_LECCION" ]; then
    n=$(find "$carpeta" -maxdepth 1 -name 'fig*.java' | wc -l | tr -d ' ')
    saltados=$((saltados + n))
    printf "  %-34s %s\n" "$leccion" "saltada ($n programas: requieren el laboratorio y el classpath)"
    continue
  fi

  cuenta_leccion=0
  for archivo in "$carpeta"fig*.java; do
    [ -f "$archivo" ] || continue
    base="${archivo%.java}"
    nombre="$(basename "$archivo")"
    cuenta_leccion=$((cuenta_leccion + 1))
    real="$(mktemp)"

    if [ -f "$base.error-esperado.txt" ]; then
      if ( cd "$carpeta" && correr java "$nombre" ) >"$real" 2>&1; then
        detalle_fallas+=("$leccion/$nombre: compiló, y la lección enseña su error de compilación")
        fallas=$((fallas + 1))
      else
        errores_ok=$((errores_ok + 1))
      fi
    elif [ -f "$base.salida.txt" ]; then
      if ! ( cd "$carpeta" && correr java "$nombre" ) >"$real" 2>&1; then
        detalle_fallas+=("$leccion/$nombre: no compiló o terminó en error")
        fallas=$((fallas + 1))
      elif diff -q "$real" "$base.salida.txt" >/dev/null 2>&1; then
        ok=$((ok + 1))
      else
        detalle_fallas+=("$leccion/$nombre: la salida real no coincide con $(basename "$base.salida.txt")")
        fallas=$((fallas + 1))
      fi
    else
      detalle_fallas+=("$leccion/$nombre: no tiene salida ni error documentado al lado")
      sin_referencia=$((sin_referencia + 1))
      fallas=$((fallas + 1))
    fi
    rm -f "$real"
  done
  printf "  %-34s %s\n" "$leccion" "$cuenta_leccion programas"
done

total=$((ok + errores_ok + fallas))
if [ "$total" -eq 0 ]; then
  echo "❌ no se encontró ningún programa: la verificación no midió nada"
  exit 2
fi

echo
echo "  salidas que coinciden     : $ok"
echo "  errores de compilación que la lección enseña, y que en efecto ocurren : $errores_ok"
echo "  saltados (lección 8)      : $saltados"
echo "  fallas                    : $fallas"

if [ "$fallas" -gt 0 ]; then
  echo
  for f in "${detalle_fallas[@]}"; do echo "  ❌ $f"; done
  exit 1
fi

echo
echo "✅ cada programa compila, corre y dice lo que la lección documenta."
