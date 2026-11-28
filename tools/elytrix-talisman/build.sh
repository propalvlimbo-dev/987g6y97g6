#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
ORIGINAL=${1:?absolute path to original ElytrixTalisman.jar}
JAVA=${JAVA:-/tmp/air-java/jdk4py/java-runtime/bin/java}
ECJ=${ECJ:-/tmp/jdt/package/server/plugins/org.eclipse.jdt.core.compiler.batch_3.45.0.v20260224-0835.jar}
ASM=${ASM:-/tmp/jdt/package/server/plugins/org.objectweb.asm_9.9.1.jar:/tmp/jdt/package/server/plugins/org.objectweb.asm.tree_9.9.1.jar}
JOML=${JOML:-/tmp/joml/BLib.jar}
[[ $(sha256sum "$ORIGINAL" | cut -d' ' -f1) == bd3220a91d8381b4e221766d1e86078865d2da7e50cdc329cfb9ab0006cb69fa ]] || { echo 'Unexpected original jar';exit 1; }
tmp=$(mktemp -d);trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/jar" "$tmp/compiled" "$tmp/patch"
(cd "$tmp/jar" && unzip -q "$ORIGINAL")
"$JAVA" -jar "$ECJ" -21 -proc:none -nowarn -encoding UTF-8 -cp "$JOML" -d "$tmp/compiled" $(find "$ROOT/tools/elytrix-talisman/test/stubs" -name '*.java') $(find "$ROOT/tools/elytrix-talisman/src" -name '*.java') "$ROOT/tools/elytrix-talisman/test/HologramTest.java" "$ROOT/tools/elytrix-talisman/test/RecoveryTest.java"
"$JAVA" -cp "$tmp/compiled:$JOML" HologramTest
"$JAVA" -cp "$tmp/compiled:$JOML" RecoveryTest
"$JAVA" -jar "$ECJ" -21 -proc:none -encoding UTF-8 -cp "$ASM" -d "$tmp/patch" "$ROOT/tools/elytrix-talisman/transform/Patch.java"
"$JAVA" -cp "$tmp/patch:$ASM" Patch "$tmp/jar"
cp "$tmp/compiled/ru/rooyzee/elytrixtalisman/service/HologramService.class" "$tmp/jar/ru/rooyzee/elytrixtalisman/service/"
mkdir -p "$tmp/jar/ru/rooyzee/elytrixtalisman/patch"
cp "$tmp/compiled/ru/rooyzee/elytrixtalisman/patch/RecoveryGuard.class" "$tmp/jar/ru/rooyzee/elytrixtalisman/patch/"
mkdir -p "$ROOT/finalbuild"
(cd "$tmp/jar" && zip -qr "$ROOT/finalbuild/ElytrixTalisman-TEST1.jar" .)
unzip -t "$ROOT/finalbuild/ElytrixTalisman-TEST1.jar" | tail -1

ANALYSIS=/tmp/jdt/package/server/plugins/org.objectweb.asm.tree.analysis_9.9.1.jar
"$JAVA" -jar "$ECJ" -21 -proc:none -encoding UTF-8 -cp "$ASM:$ANALYSIS" -d "$tmp/patch" "$ROOT/tools/elytrix-talisman/test/BytecodeTest.java"
"$JAVA" -cp "$tmp/patch:$ASM:$ANALYSIS" BytecodeTest "$ROOT/finalbuild/ElytrixTalisman-TEST1.jar"
