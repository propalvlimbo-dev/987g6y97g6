#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
runtime="${1:?usage: build.sh /path/to/leaf-runtime}"
: "${JAVA:?set JAVA to the Java 21 binary}"
: "${ECJ:?set ECJ to the Eclipse JDT compiler jar}"
classes="$(mktemp -d)"; trap 'rm -rf "$classes"' EXIT
unzip -p "$root/plugin_2jar.zip" GreatCombat-1.9.jar > "$classes/greatcombat-api.jar"
cp="$classes/greatcombat-api.jar:$(find "$runtime/libraries" -type f -name '*.jar' -printf '%p:' )$runtime/versions/1.21.11/leaf-1.21.11.jar"
"$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -21 -encoding UTF-8 -classpath "$cp" -d "$classes" $(find "$root/tools/bots-serverplayer/src" -name '*.java')
CLASSES="$classes" ROOT="$root" python3 - <<'PY'
import os
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root=Path(os.environ['ROOT']); classes=Path(os.environ['CLASSES'])
with ZipFile(root/'finalbuild/ElytrixBotsTrain.jar','w',ZIP_DEFLATED) as z:
    z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPaperweight-Mappings-Namespace: mojang\n\n')
    z.write(root/'tools/bots-serverplayer/plugin.yml','plugin.yml')
    for f in sorted((classes/'ru').rglob('*.class')): z.write(f, f.relative_to(classes).as_posix())
PY
