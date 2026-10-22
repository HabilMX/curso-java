#!/usr/bin/env bash
# verificar-url-sembrada.sh — la prueba de la URL sembrada, contra CADA revisor del curso.
#
# Por qué existe: el revisor aceptaba «http://usuario:clave@127.0.0.1:1/estado?token=SECRETO#x»
# y el reporte en JSON la devolvía entera. La regla del curso: una URL con usuario o clave se
# rechaza al cargar, y ningún error, registro ni reporte repite la URL completa (el JSON la
# lleva redactada: esquema, servidor, puerto y ruta).
#
# Cómo: compila cada revisor con javac junto con una prueba que no es de ninguna lección
# (herramientas/url-sembrada/), y la corre. La de la lección 10 además ejecuta el programa de
# verdad: la URL con clave por la línea de comandos, la que trae token con --formato json, y
# GET /reporte con --servir. Los revisores de las lecciones 8 a 10 necesitan Jackson: su
# classpath lo resuelve Maven (con red la primera vez).
#
# Es OBLIGATORIA y FALLA CERRADO: verificar-programas.sh la llama y se pone rojo si este guion
# falta, si un revisor sale en rojo, o si corrió en menos revisores de los que el curso tiene.
# Los seis revisores viven en programas/, que también viaja a la copia publicada (git archive):
# allí se esperan los mismos seis, porque ninguno está marcado export-ignore.
# Uso: herramientas/verificar-url-sembrada.sh   (desde cualquier carpeta)
set -uo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ" || exit 2
PRUEBAS="herramientas/url-sembrada"
ESPERADOS=6
CON_CLAVE='http://usuario:clave@127.0.0.1:1/estado?token=SECRETO#x'
CON_TOKEN='http://127.0.0.1:1/estado?token=SECRETO#x'
REDACTADA='"url" : "http://127.0.0.1:1/estado"'
PUERTO="${PUERTO_URL_SEMBRADA:-18089}"

for f in "$PRUEBAS/SembradaTexto.java" "$PRUEBAS/SembradaJson.java"; do
  [ -f "$f" ] || { echo "❌ falta $f: la prueba de la URL sembrada es obligatoria"; exit 1; }
done
command -v javac >/dev/null || { echo "❌ no hay javac en el PATH"; exit 2; }

TEMPORAL="$(mktemp -d)"
SERVIDOR_PID=""
limpiar() { [ -n "$SERVIDOR_PID" ] && kill "$SERVIDOR_PID" 2>/dev/null; rm -rf "$TEMPORAL"; }
trap limpiar EXIT

corridas=0; rojas=0
tiene_secreto() { grep -qF -e 'usuario:clave' -e 'SECRETO' -e 'token=' -e '#x' "$1"; }
falla() { echo "  ❌ $1"; rojo=1; }

for revisor in programas/05-*/fig05_11 programas/06-*/fig06_03 programas/07-*/fig07_01 \
               programas/08-*/fig08_10 programas/09-*/fig09_09 programas/10-*/fig10_04; do
  [ -d "$revisor" ] || { echo "❌ no encontré el revisor $revisor"; rojas=$((rojas + 1)); continue; }
  leccion=$((10#$(basename "$(dirname "$revisor")" | cut -c1-2)))
  trabajo="$TEMPORAL/$leccion"; mkdir -p "$trabajo/clases"
  rojo=0; cp=""
  echo "lección $leccion ($revisor)"
  if [ "$leccion" -eq 5 ]; then
    # sin paquete; Revisor.java es un archivo compacto (void main) y no hace falta aquí
    find "$revisor" -maxdepth 1 -name '*.java' ! -name 'Revisor.java' > "$trabajo/fuentes"
    cp "$PRUEBAS/SembradaTexto.java" "$trabajo/"
    principal=SembradaTexto
  else
    find "$revisor/src/main/java" -name '*.java' > "$trabajo/fuentes"
    if [ "$leccion" -le 7 ]; then
      { echo "package mx.habil.revisor;"; cat "$PRUEBAS/SembradaTexto.java"; } > "$trabajo/SembradaTexto.java"
      principal=mx.habil.revisor.SembradaTexto
    else
      cp "$PRUEBAS/SembradaJson.java" "$trabajo/"
      principal=mx.habil.revisor.SembradaJson
      ( cd "$revisor" && mvn -B -q dependency:build-classpath -Dmdep.outputFile="$trabajo/cp.txt" ) >"$trabajo/mvn.log" 2>&1 \
        || { echo "  ❌ Maven no resolvió el classpath"; tail -5 "$trabajo/mvn.log"; rojas=$((rojas + 1)); continue; }
      cp="$(cat "$trabajo/cp.txt")"
    fi
  fi
  find "$trabajo" -maxdepth 1 -name 'Sembrada*.java' >> "$trabajo/fuentes"
  if ! javac -nowarn -d "$trabajo/clases" ${cp:+-cp "$cp"} @"$trabajo/fuentes" >"$trabajo/javac.log" 2>&1; then
    echo "  ❌ no compiló"; head -10 "$trabajo/javac.log"; rojas=$((rojas + 1)); continue
  fi
  corridas=$((corridas + 1))
  java -cp "$trabajo/clases${cp:+:$cp}" "$principal" || rojo=1

  if [ "$leccion" -eq 10 ]; then
    # el programa de verdad, de punta a punta
    clases="$trabajo/clases:$cp"
    printf '{"servicios": [{"nombre": "sembrado", "url": "%s"}]}\n' "$CON_CLAVE" > "$trabajo/clave.json"
    printf '{"servicios": [{"nombre": "sembrado", "url": "%s"}]}\n' "$CON_TOKEN" > "$trabajo/token.json"
    java -cp "$clases" mx.habil.revisor.Revisor --config "$trabajo/clave.json" >"$trabajo/a.out" 2>&1
    rc=$?
    [ "$rc" -eq 2 ] || falla "con la URL con clave salió con $rc, y debía ser 2 (configuración inválida)"
    tiene_secreto "$trabajo/a.out" && falla "la línea de comandos repite un secreto: $(cat "$trabajo/a.out")"
    java -cp "$clases" mx.habil.revisor.Revisor --config "$trabajo/token.json" --formato json >"$trabajo/b.out" 2>&1
    tiene_secreto "$trabajo/b.out" && falla "--formato json repite un secreto"
    grep -qF "$REDACTADA" "$trabajo/b.out" || falla "--formato json no trae la URL redactada"
    java -cp "$clases" mx.habil.revisor.Revisor --config "$trabajo/token.json" --servir "$PUERTO" >"$trabajo/s.out" 2>&1 &
    SERVIDOR_PID=$!
    for _ in $(seq 1 50); do
      curl -s --max-time 5 "http://127.0.0.1:$PUERTO/reporte" >"$trabajo/c.out" 2>/dev/null && break
      sleep 0.2
    done
    kill "$SERVIDOR_PID" 2>/dev/null; wait "$SERVIDOR_PID" 2>/dev/null; SERVIDOR_PID=""
    [ -s "$trabajo/c.out" ] || falla "GET /reporte no contestó"
    tiene_secreto "$trabajo/c.out" && falla "GET /reporte repite un secreto"
    grep -qF "$REDACTADA" "$trabajo/c.out" || falla "GET /reporte no trae la URL redactada"
    [ "$rojo" -eq 0 ] && echo "  de punta a punta: línea de comandos, --formato json y GET /reporte en verde"
  fi
  [ "$rojo" -eq 0 ] || rojas=$((rojas + 1))
done

echo
if [ "$corridas" -ne "$ESPERADOS" ]; then
  echo "❌ URL sembrada: corrí la prueba en $corridas revisores, y el curso tiene $ESPERADOS"
  exit 1
fi
if [ "$rojas" -ne 0 ]; then
  echo "❌ URL sembrada: $rojas de $corridas revisores aceptan una URL con usuario o clave, o repiten sus secretos"
  exit 1
fi
echo "✅ URL sembrada: $corridas de $corridas revisores la rechazan al cargar y no repiten sus secretos"
