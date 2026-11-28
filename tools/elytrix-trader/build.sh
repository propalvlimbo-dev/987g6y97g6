#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
SRC=${1:?path to original ElytrixTrader.jar}
JAVA=${JAVA:-/tmp/air-java/jdk4py/java-runtime/bin/java}
ECJ=${ECJ:-/tmp/jdt/package/server/plugins/org.eclipse.jdt.core.compiler.batch_3.45.0.v20260224-0835.jar}
ASM=${ASM:-/tmp/jdt/package/server/plugins/org.objectweb.asm_9.9.1.jar:/tmp/jdt/package/server/plugins/org.objectweb.asm.tree_9.9.1.jar}
LIBS=${LIBS:-/tmp/botlibs/META-INF/libraries}
BLIB=${BLIB:-/tmp/joml/BLib.jar}
VIRTUAL=${VIRTUAL:-/tmp/VirtualEntityApi.jar}
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/jar" "$tmp/classes" "$tmp/patch"
(cd "$tmp/jar" && unzip -q "$SRC")
CP="$SRC:$BLIB:$VIRTUAL"; while IFS= read -r j; do CP="$CP:$j"; done < <(find "$LIBS" -name '*.jar')
"$JAVA" -jar "$ECJ" -21 -proc:none -encoding UTF-8 -cp "$CP" -d "$tmp/classes" $(find "$ROOT/tools/elytrix-trader/src" -name '*.java')
"$JAVA" -jar "$ECJ" -21 -proc:none -cp "$ASM" -d "$tmp/patch" "$ROOT/tools/elytrix-trader/transform/Patch.java"
"$JAVA" -cp "$tmp/patch:$ASM" Patch "$tmp/jar"
cp -R "$tmp/classes/ru" "$tmp/jar/"
mkdir -p "$ROOT/finalbuild"
(cd "$tmp/jar" && zip -qr "$ROOT/finalbuild/ElytrixTrader-TEST1.jar" .)
echo 'Built finalbuild/ElytrixTrader-TEST1.jar'
