#!/usr/bin/env bash
# Comprueba que el código de las lecciones y el de programas/ digan lo mismo.
#
# Las lecciones son la fuente; programas/ es la copia ejecutable. Si alguien corrige un
# bloque de código en una lección y no actualiza el archivo —o al revés—, el alumno lee
# una cosa y ejecuta otra, y ninguna otra verificación lo nota: verificar-programas.sh
# compara cada programa con su SALIDA documentada, no con la lección que lo muestra.
#
# Un bloque declara a qué archivo corresponde de dos maneras:
#   ```java            y su primer renglón es   // figNN_NN.java  (o una ruta más larga)
#   ```xml fig06_03/pom.xml                      la ruta va en la etiqueta del cercado
# La ruta es relativa a programas/<carpeta de la lección>/.
#
# Un bloque sin ruta declarada es un fragmento ilustrativo y no se compara.
# Los archivos de programas/ que ninguna lección declara se listan como arrastrados:
# son las clases que un proyecto de Maven necesita completas aunque la lección solo
# muestre las que cambian. No son un defecto, pero se dicen, para que el número no
# aparezca como un cero silencioso.
#
# Falla cerrado: si no encuentra ni un bloque declarado, sale en error, no en verde.
# Autoprueba:  herramientas/verificar-extractos.sh --probar
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

# ─── extrae de cada lección los bloques que declaran un archivo ────────────────────────
# Escribe un archivo por bloque en $TMP/<leccion>/<ruta> y la lista en $TMP/.declarados
extraer() {
  local raiz="$1"
  : > "$TMP/.declarados"
  local leccion
  for fuente in "$raiz"/[0-9][0-9]-*.md; do
    [ -f "$fuente" ] || continue
    leccion=$(basename "$fuente" .md)
    awk -v L="$leccion" -v D="$TMP" '
      function cerrar() {
        if (ruta != "") {
          destino = D "/" L "/" ruta
          n = split(destino, partes, "/"); dir = partes[1]
          for (k = 2; k < n; k++) dir = dir "/" partes[k]
          system("mkdir -p \"" dir "\"")
          printf "%s", cuerpo > destino
          close(destino)
          print L "\t" ruta >> (D "/.declarados")
        }
        dentro = 0; ruta = ""; cuerpo = ""
      }
      /^```/ {
        if (dentro) { cerrar(); next }
        # ¿la etiqueta del cercado trae la ruta? p. ej.  ```xml fig06_03/pom.xml
        if ($0 ~ /^```[a-zA-Z]+[ \t]+[^ \t]+/) {
          etiqueta = $0; sub(/^```[a-zA-Z]+[ \t]+/, "", etiqueta)
          sub(/[ \t]+$/, "", etiqueta)
          if (etiqueta ~ /^fig[0-9]/) { dentro = 1; ruta = etiqueta; cuerpo = ""; primera = 0; next }
        }
        if ($0 ~ /^```java[ \t]*$/) { dentro = 1; ruta = ""; cuerpo = ""; primera = 1; next }
        next
      }
      dentro {
        if (primera) {
          primera = 0
          if ($0 ~ /^\/\/[ \t]*fig[0-9]/) { ruta = $0; sub(/^\/\/[ \t]*/, "", ruta); sub(/[ \t]+$/, "", ruta) }
          else { dentro = 0; next }   # fragmento ilustrativo: no se compara
        }
        cuerpo = cuerpo $0 "\n"
      }
    ' "$fuente"
  done
}

# ─── autoprueba: el comparador tiene que CAZAR una diferencia sembrada ─────────────────
if [ "${1:-}" = "--probar" ]; then
  cp -R es "$TMP/es-probar"
  primera_leccion=$(ls "$TMP/es-probar"/[0-9][0-9]-*.md | head -1)
  # mete un renglón dentro del primer bloque declarado de esa lección
  awk 'BEGIN{hecho=0} {print} /^\/\/[ \t]*fig[0-9]/{ if(!hecho){ print "  // renglón sembrado por la autoprueba"; hecho=1 } }' \
    "$primera_leccion" > "$primera_leccion.nuevo" && mv "$primera_leccion.nuevo" "$primera_leccion"
  extraer "$TMP/es-probar"
  cazo=0
  while IFS=$'\t' read -r L ruta; do
    [ -n "$L" ] || continue
    diff -q "$TMP/$L/$ruta" "programas/$L/$ruta" >/dev/null 2>&1 || cazo=1
  done < "$TMP/.declarados"
  if [ "$cazo" -eq 1 ]; then echo "✅ autoprueba correcta: el comparador caza una diferencia sembrada."; exit 0
  else echo "🔴 autoprueba FALLIDA: sembré una diferencia y el comparador no la vio."; exit 1; fi
fi

# ─── comparación real ──────────────────────────────────────────────────────────────────
extraer es

declarados=$(wc -l < "$TMP/.declarados" | tr -d ' ')
if [ "$declarados" -eq 0 ]; then
  echo "🔴 ninguna lección declaró un archivo: la verificación no comparó nada"
  exit 2
fi

iguales=0; rc=0
: > "$TMP/.rutas"
while IFS=$'\t' read -r L ruta; do
  [ -n "$L" ] || continue
  echo "programas/$L/$ruta" >> "$TMP/.rutas"
  real="programas/$L/$ruta"
  if [ ! -f "$real" ]; then
    echo "  🔴 la lección $L muestra $ruta, y ese archivo no existe en programas/"
    rc=1
  elif diff -q "$TMP/$L/$ruta" "$real" >/dev/null 2>&1; then
    iguales=$((iguales + 1))
  else
    echo "  🔴 $real no dice lo mismo que la lección $L:"
    diff "$TMP/$L/$ruta" "$real" | head -8 | sed 's/^/       /'
    rc=1
  fi
done < "$TMP/.declarados"

# archivos que ninguna lección declara: los arrastra un proyecto de Maven
sort -u "$TMP/.rutas" > "$TMP/.rutas-ord"
find programas -type f \( -name '*.java' -o -name 'pom.xml' -o -name '*.properties' \) | sort > "$TMP/.reales"
arrastrados=$(comm -13 "$TMP/.rutas-ord" "$TMP/.reales" | wc -l | tr -d ' ')

echo
echo "  bloques de las lecciones que declaran un archivo : $declarados"
echo "  coinciden con programas/                         : $iguales"
echo "  arrastrados (ninguna lección los muestra)        : $arrastrados"

if [ "$rc" -ne 0 ]; then
  echo
  echo "🔴 Las lecciones y programas/ se separaron. Corrige el que esté mal: el alumno lee uno y ejecuta el otro."
  exit 1
fi

echo
echo "✅ cada bloque de código de las lecciones dice lo mismo que su archivo en programas/."
