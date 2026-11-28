#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
ORIGINAL=${1:?absolute path to original ElytrixTrader.jar}
BASE=${2:-$ROOT/finalbuild/ElytrixTrader-TEST1.jar}
JAVA=${JAVA:-/tmp/air-java/jdk4py/java-runtime/bin/java}
ECJ=${ECJ:-/tmp/jdt/package/server/plugins/org.eclipse.jdt.core.compiler.batch_3.45.0.v20260224-0835.jar}
ASM=${ASM:-/tmp/jdt/package/server/plugins/org.objectweb.asm_9.9.1.jar:/tmp/jdt/package/server/plugins/org.objectweb.asm.tree_9.9.1.jar}
JOML=${JOML:-/tmp/joml/BLib.jar}
[[ $(sha256sum "$ORIGINAL" | cut -d' ' -f1) == 2344ad2f86ffc7189960824770feb4e8a5836b31fce9772f7962056ca883a62a ]] || { echo 'Unexpected original jar';exit 1; }
tmp=$(mktemp -d);trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/jar" "$tmp/base" "$tmp/patch" "$tmp/compiled"
(cd "$tmp/jar" && unzip -q "$ORIGINAL")
(cd "$tmp/base" && unzip -q "$BASE" 'ru/rooyzee/elytrixtrader/patch/*')
"$JAVA" -jar "$ECJ" -21 -proc:none -nowarn -cp "$JOML" -d "$tmp/compiled" $(find "$ROOT/tools/elytrix-trader/test/stubs" -name '*.java') "$ROOT/tools/elytrix-trader/src/ru/rooyzee/elytrixtrader/patch/TraderGalaxy.java" "$ROOT/tools/elytrix-trader/test/GalaxyTest.java"
"$JAVA" -cp "$tmp/compiled:$JOML" ru.rooyzee.elytrixtrader.patch.GalaxyTest
"$JAVA" -jar "$ECJ" -21 -proc:none -cp "$ASM" -d "$tmp/patch" "$ROOT/tools/elytrix-trader/transform/Patch.java" "$ROOT/tools/elytrix-trader/test/GalaxyBytecodeTest.java"
"$JAVA" -cp "$tmp/patch:$ASM" Patch "$tmp/jar"
mkdir -p "$tmp/jar/ru/rooyzee/elytrixtrader/patch"
cp "$tmp/base/ru/rooyzee/elytrixtrader/patch/"*.class "$tmp/jar/ru/rooyzee/elytrixtrader/patch/"
cp "$tmp/compiled/ru/rooyzee/elytrixtrader/patch/TraderGalaxy"*.class "$tmp/jar/ru/rooyzee/elytrixtrader/patch/"
(cd "$tmp/jar" && zip -qr "$ROOT/finalbuild/ElytrixTrader-TEST2.jar" .)
unzip -t "$ROOT/finalbuild/ElytrixTrader-TEST2.jar" | tail -1
"$JAVA" -cp "$tmp/patch:$ASM" GalaxyBytecodeTest "$ROOT/finalbuild/ElytrixTrader-TEST2.jar"
