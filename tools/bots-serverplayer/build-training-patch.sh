#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
: "${JAVA:?Set JAVA to Java 21+}" "${ECJ:?Set ECJ compiler jar}" "${ASM:?Set ASM jar}"
leaf="$(realpath "${1:?usage: build-training-patch.sh leaf-bundler.jar}")"
base="$root/finalbuild/ElytrixBotsTrain-base-2.6.jar"
[ "$(sha256sum "$base" | cut -d' ' -f1)" = 15af743b57d161a6128fc9acd3237b321174e43b4d74c1849c2ccd4cd5a98c14 ] || { echo 'Unexpected base JAR' >&2; exit 1; }
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/lib" "$tmp/classes" "$tmp/transform"
(cd "$tmp/lib" && unzip -q "$leaf" 'META-INF/libraries/*')
cp="$(find "$tmp/lib" -name '*.jar' -printf '%p:')"
"$JAVA" -jar "$ECJ" -21 -nowarn -encoding UTF-8 -cp "$cp" -d "$tmp/classes" "$root/tools/bots-serverplayer/src/ru/elytrix/bots/TrainingArena.java"
"$JAVA" -jar "$ECJ" -21 -nowarn -cp "$ASM" -d "$tmp/transform" "$root/tools/bots-serverplayer/transform/TrainingTransform.java"
(cd "$tmp/classes" && unzip -qo "$base")
cp "$root/tools/bots-serverplayer/plugin.yml" "$tmp/classes/plugin.yml"
"$JAVA" -cp "$tmp/transform:$ASM" TrainingTransform "$tmp/classes/ru/elytrix/bots/BotsPlugin.class"
rm -f "$root/finalbuild/ElytrixBotsTrain.jar"
(cd "$tmp/classes" && zip -qr "$root/finalbuild/ElytrixBotsTrain.jar" .)
echo 'Built finalbuild/ElytrixBotsTrain.jar'
