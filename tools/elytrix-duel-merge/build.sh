#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
: "${JAVA:?Set JAVA to Java 21+}"
: "${ECJ:?Set ECJ to the Eclipse Java compiler jar}"
: "${ASM:?Set ASM to ASM 9 jar}"
epic="${1:?usage: build.sh EpicDuels-ElyTriX-3.1.2.jar ElytrixDuelsBridge-native-menus-TEST.jar output.jar}"
bridge="${2:?missing bridge input}"
out="${3:?missing output}"
epic="$(realpath "$epic")"; bridge="$(realpath "$bridge")"; out="$(realpath -m "$out")"
stage="$(mktemp -d)"; trap 'rm -rf "$stage"' EXIT
mkdir -p "$stage/classes" "$stage/helper" "$stage/transform" "$(dirname "$out")"
python3 "$root/tools/duel-world-guard/patch.py" "$epic" "$stage/epic-guard.jar"
"$JAVA" -jar "$ECJ" -21 -encoding UTF-8 -nowarn -cp "$ASM" -d "$stage/transform" "$root/tools/elytrix-duel-merge/transform/MergeTransform.java" "$root/tools/elytrix-duel-merge/transform/WorldCopyTransform.java" "$root/tools/elytrix-duel-merge/transform/ArenaBoundaryTransform.java" "$root/tools/elytrix-duel-merge/transform/SharedWorldTransform.java"
"$JAVA" -jar "$ECJ" -21 -encoding UTF-8 -nowarn -d "$stage/helper" $(find "$root/tools/elytrix-duel-merge/src" "$root/tools/elytrix-duel-merge/stubs" -name '*.java')
python3 - "$stage" "$bridge" <<'PY'
from zipfile import ZipFile
from pathlib import Path
import hashlib,sys
stage,bridge=map(Path,sys.argv[1:]); dest=stage/'classes'
# Refuse accidentally merging the original BMenu bridge or a different build.
expected='222e6ad1f1d9dc7ff2ecdf13bb2cadb5929464172605a3b4a98c34fb51402a2f'
assert hashlib.sha256(bridge.read_bytes()).hexdigest()==expected,'unexpected bridge hash'
with ZipFile(stage/'epic-guard.jar') as z: z.extractall(dest)
with ZipFile(bridge) as z:
 for name in z.namelist():
  if name.startswith('ru/elytrix/duelsbridge/') and not name.endswith('/'):
   target=dest/name; target.parent.mkdir(parents=True,exist_ok=True); target.write_bytes(z.read(name))
 (dest/'bridge-config.yml').write_bytes(z.read('config.yml'))
p=dest/'plugin.yml'; text=p.read_text(encoding='utf8')
assert 'name: EpicDuels\n' in text and 'main: dev.epicduels.EpicDuels\n' in text
text=text.replace('name: EpicDuels\n','name: ElytrixDuel\n')
text=text.replace('  EpicDuels:\n','  ElytrixDuel:\n')
text=text.replace('main: dev.epicduels.EpicDuels\n','main: ru.elytrix.duelsbridge.ElytrixDuelsBridge\n')
needle='  party:\n'; assert text.count(needle)==1
text=text.replace(needle,'  duelx:\n    description: Elytrix duel menu actions\n    aliases: [dx]\n'+needle)
p.write_text(text,encoding='utf8')
helpers=stage/'helper/ru/elytrix/duelsbridge'
for name in ('BridgeFiles', 'SafeWorldCopy', 'LegacyDataMigration', 'ArenaBoundary', 'TextFilter', 'MaceHeightDamage', 'SharedWorldSafety', 'SharedBattleWorld'):
 matches=list(helpers.glob(name+'*.class'))
 assert matches, name
 for p in matches: (dest/'ru/elytrix/duelsbridge'/p.name).write_bytes(p.read_bytes())
PY
cp "$root/finalbuild/duel-world-guard/EpicDuels-UPSTREAM-LICENSE.txt" "$stage/classes/META-INF/LICENSE-EpicDuels.txt"
"$JAVA" -cp "$stage/transform:$ASM" MergeTransform "$stage/classes"
"$JAVA" -cp "$stage/transform:$ASM" WorldCopyTransform "$stage/classes"
"$JAVA" -cp "$stage/transform:$ASM" ArenaBoundaryTransform "$stage/classes"
if [[ "${SHARED_WORLD_EXPERIMENTAL:-0}" == 1 ]]; then
  "$JAVA" -cp "$stage/transform:$ASM" SharedWorldTransform "$stage/classes"
fi
python3 - "$stage/classes" "$out" <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import sys
src,out=map(Path,sys.argv[1:])
with ZipFile(out,'w',ZIP_DEFLATED) as z:
 for p in sorted(src.rglob('*')):
  if p.is_file(): z.write(p,p.relative_to(src).as_posix())
with ZipFile(out) as z: assert z.testzip() is None
print('Built',out)
PY
