#!/usr/bin/env bash
set -euo pipefail
# JAVA is a Java 21 runtime; ECJ and ASM_CP are Eclipse compiler / ASM jars.
# API_CP is the target server's Bukkit/Leaf API and dependency jars, including JOML.
: "${JAVA:?}" "${ECJ:?}" "${ASM_CP:?}" "${API_CP:?}" "${ORIGINAL_JAR:?}" "${OUTPUT_JAR:?}"
root=$(cd "$(dirname "$0")" && pwd)
expected=1ae338dae6e5ffb2ba819ecde808595f4500826f8b8b01f9e8262f1a0b493330
[[ $(sha256sum "$ORIGINAL_JAR" | cut -d' ' -f1) == "$expected" ]] || { echo 'Unexpected original JAR' >&2; exit 1; }
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/classes" "$tmp/compiled" "$tmp/tests"
(cd "$tmp/classes" && unzip -qo "$ORIGINAL_JAR")
mapfile -t sources < <(find "$root/src" -name '*.java' | sort)
"$JAVA" -jar "$ECJ" -21 -proc:none -nowarn -cp "$ORIGINAL_JAR:$API_CP" -d "$tmp/compiled" "${sources[@]}"
"$JAVA" -jar "$ECJ" -21 -proc:none -nowarn -cp "$tmp/compiled:$ORIGINAL_JAR:$API_CP:$ASM_CP" -d "$tmp/tests" "$root/transform/Patch.java" $(find "$root/test" -name '*.java' | sort)
"$JAVA" -cp "$tmp/tests:$ASM_CP" Patch "$tmp/classes"
rm -f "$tmp/classes/ru/rooyzee/elytrixairdrop/airdrops/types/ActiveFireAirdrop$"*.class
cp -R "$tmp/compiled/ru" "$tmp/classes/"
"$JAVA" -cp "$tmp/tests:$tmp/compiled:$ORIGINAL_JAR:$API_CP" NearbyRewardsTest
"$JAVA" -cp "$tmp/tests:$tmp/compiled:$ORIGINAL_JAR:$API_CP" WeightedLootTest
"$JAVA" -cp "$tmp/tests:$tmp/compiled:$ORIGINAL_JAR:$API_CP" ru.rooyzee.elytrixairdrop.patch.SkyMechanicsTest
"$JAVA" -cp "$tmp/tests:$tmp/compiled:$ORIGINAL_JAR:$API_CP" ru.rooyzee.elytrixairdrop.patch.FireScheduleTest
"$JAVA" -cp "$tmp/tests:$tmp/compiled:$ORIGINAL_JAR:$API_CP" ru.rooyzee.elytrixairdrop.patch.ReopenGuardTest
"$JAVA" -cp "$tmp/tests:$tmp/compiled:$ORIGINAL_JAR:$API_CP" ru.rooyzee.elytrixairdrop.patch.HologramTextTest
sed -i 's/^version: 1.0.0/version: 1.0.0-test-elytrix/' "$tmp/classes/plugin.yml"
"$JAVA" -cp "$tmp/tests:$ASM_CP" PatchPluginYaml "$tmp/classes/plugin.yml"
# Fresh installs expose an opt-out; existing configs are never edited.
printf '\n# Set true to use your airdrops.*.hologram lines for main event text.\nholograms:\n  use-custom-lines: false\n' >> "$tmp/classes/config.yml"
mkdir -p "$(dirname "$OUTPUT_JAR")"
(cd "$tmp/classes" && zip -q -r "$tmp/output.jar" .)
unzip -t "$tmp/output.jar" | tail -1
"$JAVA" -cp "$tmp/tests:$ASM_CP" BytecodeTest "$tmp/output.jar"
if [[ -n "${ITEM_JAR:-}" ]]; then "$JAVA" -cp "$tmp/tests:$ASM_CP" PresetBytecodeTest "$tmp/output.jar" "$ITEM_JAR"; fi
mv "$tmp/output.jar" "$OUTPUT_JAR"
