#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
JAVA=${JAVA:-java}
: "${ECJ:?Set ECJ to a Java 21-capable Eclipse JDT compiler jar}"
input=${1:?Usage: build.sh path/to/ElytrixCasino-2.2.1.jar}
input=$(realpath "$input")
echo "f21984324b15c1a34d866a66facd2845e5724562467245b03287161c285038d9  $input" | sha256sum -c -
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/classes" "$tmp/transform" "$tmp/test" finalbuild
exports=(--add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree.analysis=ALL-UNNAMED)
mapfile -t sources < <(find tools/casino/stubs tools/casino/src -name '*.java' | sort)
"$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -21 -proc:none -nowarn -classpath "$input" -d "$tmp/classes" "${sources[@]}"
"$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -21 -nowarn "${exports[@]}" -d "$tmp/transform" tools/casino/Transform.java tools/casino/Verify.java
"$JAVA" "${exports[@]}" -cp "$tmp/transform" Transform "$input" "$tmp/classes" "$tmp/ElytrixCasino.jar"
"$JAVA" "${exports[@]}" -cp "$tmp/transform" Verify "$tmp/ElytrixCasino.jar"
"$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -21 -proc:none -nowarn -classpath "$tmp/ElytrixCasino.jar:$tmp/classes" -d "$tmp/test" tools/casino/tests/CasinoTests.java
"$JAVA" -Xverify:all -cp "$tmp/test:$tmp/ElytrixCasino.jar:$tmp/classes" CasinoTests
python3 tools/casino/tests/miner_rtp.py
unzip -t "$tmp/ElytrixCasino.jar" >/dev/null
cp "$tmp/ElytrixCasino.jar" finalbuild/ElytrixCasino.jar
printf 'Built finalbuild/ElytrixCasino.jar\n'
