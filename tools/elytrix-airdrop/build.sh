#!/usr/bin/env bash
set -euo pipefail
# JAVA is a Java 21 runtime; ECJ and ASM_CP are Eclipse compiler / ASM jars.
# API_CP is the target server's Bukkit/Leaf API and dependency jars.
: "${JAVA:?}" "${ECJ:?}" "${ASM_CP:?}" "${API_CP:?}" "${ORIGINAL_JAR:?}" "${OUTPUT_JAR:?}"
root=$(cd "$(dirname "$0")" && pwd)
expected=1ae338dae6e5ffb2ba819ecde808595f4500826f8b8b01f9e8262f1a0b493330
[[ $(sha256sum "$ORIGINAL_JAR" | cut -d' ' -f1) == "$expected" ]] || { echo 'Unexpected original JAR' >&2; exit 1; }
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/classes" "$tmp/compiled"
(cd "$tmp/classes" && unzip -qo "$ORIGINAL_JAR")
mapfile -t sources < <(find "$root/src" "$root/test" -name '*.java' | sort)
"$JAVA" -jar "$ECJ" -21 -proc:none -nowarn -cp "$ORIGINAL_JAR:$API_CP" -d "$tmp/compiled" "${sources[@]}"
"$JAVA" -jar "$ECJ" -21 -proc:none -nowarn -cp "$ASM_CP" -d "$tmp/compiled" "$root/transform/Patch.java"
"$JAVA" -cp "$tmp/compiled:$ASM_CP" Patch "$tmp/classes"
cp -R "$tmp/compiled/ru" "$tmp/classes/"
"$JAVA" -cp "$tmp/compiled:$ORIGINAL_JAR:$API_CP" NearbyRewardsTest
sed -i 's/^version: 1.0.0/version: 1.0.0-test-elytrix/' "$tmp/classes/plugin.yml"
mkdir -p "$(dirname "$OUTPUT_JAR")"
(cd "$tmp/classes" && zip -q -r "$OUTPUT_JAR" .)
unzip -t "$OUTPUT_JAR" | tail -1
