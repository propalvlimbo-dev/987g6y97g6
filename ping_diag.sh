#!/usr/bin/env bash
# Elytrix / RW_GRIEF — диагностика пинга при большом онлайне.
# Запуск в SSH:  bash ping_diag.sh /root/RW_GRIEF
# Если путь не указать — возьмёт текущую папку, либо /root/RW_GRIEF.
#
# Задача: отделить "тормозит VDS / сеть хоста" от "тормозят плагины игрового сервера".
# Скрипт НИЧЕГО не меняет, только собирает цифры и печатает, что вставить в консоль.

set -u

SRV_DIR="${1:-}"
if [ -z "$SRV_DIR" ]; then
  if [ -d "./plugins" ]; then
    SRV_DIR="$(pwd)"
  elif [ -d "/root/RW_GRIEF/plugins" ]; then
    SRV_DIR="/root/RW_GRIEF"
  else
    SRV_DIR="$(pwd)"
  fi
fi

OUT="/tmp/elytrix-ping-$(date +%Y%m%d-%H%M%S).log"
if [ -n "${BASH_VERSION:-}" ]; then
  exec > >(tee "$OUT") 2>&1
else
  exec >"$OUT" 2>&1
fi

line() { echo "--------------------------------------------------------------------"; }
sec()  { echo ""; line; echo "=== $* ==="; line; }

echo "Elytrix ping diagnostic | $(date '+%Y-%m-%d %H:%M:%S %Z') | server=$SRV_DIR"
echo "Лог сохранится в: $OUT"

sec "1. ХОСТ / VDS (CPU, RAM, диск)"
echo "kernel : $(uname -a)"
echo "vCPU   : $(nproc 2>/dev/null || echo '?')"
echo "loadavg: $(cat /proc/loadavg 2>/dev/null)"
echo ""
free -h 2>/dev/null || cat /proc/meminfo | head -5
echo ""
df -h "$SRV_DIR" 2>/dev/null | tail -2

# CPU steal / iowait за 3 секунды (главный признак перепроданного VDS)
A="$(awk '/^cpu /{print $2,$3,$4,$5,$6,$7,$8,$9; exit}' /proc/stat 2>/dev/null)"
sleep 3
B="$(awk '/^cpu /{print $2,$3,$4,$5,$6,$7,$8,$9; exit}' /proc/stat 2>/dev/null)"
CPUSTAT="$(echo "$A $B" | awk '{
  t1=0; t2=0;
  for(i=1;i<=8;i++) t1+=$i;
  for(i=9;i<=16;i++) t2+=$i;
  dt=t2-t1; if(dt<=0) dt=1;
  steal=($16-$8)*100/dt; io=($13-$5)*100/dt; idle=($12-$4)*100/dt;
  printf "steal=%.1f iowait=%.1f idle=%.1f\n", steal, io, idle;
}' 2>/dev/null)"
echo "CPU за 3 сек: ${CPUSTAT:-не удалось посчитать}"
STEAL_PCT="$(echo "$CPUSTAT" | sed -n 's/.*steal=\([0-9.]*\).*/\1/p')"
IOWAIT_PCT="$(echo "$CPUSTAT" | sed -n 's/.*iowait=\([0-9.]*\).*/\1/p')"
STEAL_PCT="${STEAL_PCT:-0}"; IOWAIT_PCT="${IOWAIT_PCT:-0}"
LOAD1="$(awk '{print $1}' /proc/loadavg 2>/dev/null || echo 0)"
NCPU="$(nproc 2>/dev/null || echo 1)"
echo ""
echo "ПОЯСНЕНИЕ: steal > 5..10% = хост забирает CPU у других VM (виноват VDS)."
echo "           iowait > 15%   = тормозит диск VDS (БД/чанки/логи)."
echo "           load1 > vCPU   = CPU в упор."

if command -v vmstat >/dev/null 2>&1; then
  echo ""; echo "vmstat 1 4 (последняя строка — рабочая):"; vmstat 1 4 | tail -2
fi

sec "2. СЕТЬ С САМОГО VDS"
PING_AVG="нет"
if command -v ping >/dev/null 2>&1; then
  P="$(ping -c 5 -W 1 1.1.1.1 2>/dev/null || ping -c 5 -W 1 8.8.8.8 2>/dev/null || true)"
  echo "$P" | tail -3
  PING_AVG="$(echo "$P" | grep -oE 'avg = [0-9.]+|[0-9.]+/[0-9.]+/[0-9.]+' | tail -1 | grep -oE '[0-9.]+/[0-9.]+/[0-9.]+' | awk -F/ '{print $2}' || true)"
  [ -z "$PING_AVG" ] && PING_AVG="$(echo "$P" | sed -n 's/.*avg = \([0-9.]*\).*/\1/p')"
fi
echo "Средний пинг с VDS: ${PING_AVG:-не определён} мс"
if command -v ss >/dev/null 2>&1; then echo ""; ss -s; fi
if command -v netstat >/dev/null 2>&1; then
  echo ""; netstat -s 2>/dev/null | grep -Ei 'retrans|segments retrans|packet receive errors' | head -10
fi

sec "3. JAVA-ПРОЦЕСС ИГРОВОГО СЕРВЕРА"
PID=""
if command -v pgrep >/dev/null 2>&1; then
  PID="$(pgrep -f 'java.*(paper|leaf|purpur|spigot|server\.jar|nogui|RW_GRIEF)' 2>/dev/null | head -1 || true)"
  [ -z "$PID" ] && PID="$(pgrep -f 'java' 2>/dev/null | head -1 || true)"
fi
echo "PID: ${PID:-не найден}"
if [ -n "$PID" ] && [ -d "/proc/$PID" ]; then
  echo ""; ps -p "$PID" -o pid,ppid,%cpu,%mem,rss,nlwp,etime,cmd 2>/dev/null | cut -c1-220
  echo ""; grep -E 'Max open files|Open files' "/proc/$PID/limits" 2>/dev/null || true
  echo ""; tr '\0' ' ' < "/proc/$PID/cmdline" 2>/dev/null | cut -c1-400; echo
  if command -v jcmd >/dev/null 2>&1; then
    echo ""; echo "JVM flags:"; jcmd "$PID" VM.flags 2>/dev/null | grep -Ei 'Xmx|Xms|UseG1|MaxGCPause|ParallelGC|ActiveProcessorCount' | head -10
  fi
  if command -v jstat >/dev/null 2>&1; then
    echo ""; echo "GC (5 замеров, FGC/FGCT не должен расти каждый тик):"; jstat -gcutil "$PID" 1000 5 2>/dev/null || true
  fi
  if command -v jcmd >/dev/null 2>&1; then
    TH="/tmp/elytrix_threads_$$.txt"; : > "$TH"
    for i in 1 2 3; do jcmd "$PID" Thread.print -l >> "$TH" 2>/dev/null || true; sleep 1; done
    echo ""; echo "Топ стеков из 3 jcmd Thread.print (что реально грузит CPU):"
    grep -E '^[[:space:]]+at ' "$TH" 2>/dev/null | sed -E 's/^[[:space:]]+at //' | awk '{print $1}' \
      | sort | uniq -c | sort -nr | head -25
    rm -f "$TH"
  fi
else
  echo "[!] Процесс java не найден — сервер выключен или ищется другим именем."
fi

sec "4. ЛОГИ ИГРОВОГО СЕРВЕРА"
LOG="$SRV_DIR/logs/latest.log"
if [ -f "$LOG" ]; then
  echo "Файл: $LOG ($(du -h "$LOG" | awk '{print $1}'))"
  echo ""; echo "Can't keep up / тик-лаги (последние 15):"
  grep -Ei "Can't keep up|Skipping|tick|MSPT|TPS|overloaded" "$LOG" 2>/dev/null | tail -15
  echo ""; echo "Сетевые таймауты (последние 15):"
  grep -Ei "timed out|connection reset|read timed out|keepalive|disconnect.*timeout|lost connection" "$LOG" 2>/dev/null | tail -15
else
  echo "latest.log не найден: $LOG"
fi

sec "5. НАСТРОЙКИ ЯДРА / ПРОВЕРКА apply_perf.sh"
for f in "$SRV_DIR/spigot.yml" "$SRV_DIR/config/paper-global.yml" "$SRV_DIR/config/paper-world-defaults.yml" "$SRV_DIR/server.properties"; do
  [ -f "$f" ] || continue
  echo "--- ${f#$SRV_DIR/} ---"
  grep -Ei 'netty-threads|worker-threads|max-joins-per-tick|view-distance|simulation-distance|max-auto-save|player-max-concurrent|optimize-explosions|network-compression-threshold|max-per-tick' "$f" 2>/dev/null | head -20
done
if [ -f "$SRV_DIR/plugins/FloriumAC/saas.yml" ]; then
  grep -E 'http://url|ws://url|enabled:' "$SRV_DIR/plugins/FloriumAC/saas.yml" | head -10
fi

sec "6. ЧТО ВСТАВИТЬ В КОНСОЛЬ СЕРВЕРА (не в SSH!)"
cat <<'CONSOLE'
tps
mspt
list
ver ElytrixProhibition
plugman list
wg reload

# Если стоит Spark (лучший способ найти плагин-виновник):
spark profiler --timeout 300 --only-ticks-over 50
spark tickmonitor --threshold 50 --without-gc

# Если Spark ещё нет — положить spark-bukkit.jar в plugins/ и:
# plugman load spark
# потом снова /spark profiler --timeout 300

# Если Paper всё ещё поддерживает timings:
timings on
# подождать 5 минут при пиковом онлайне, затем:
timings paste
CONSOLE

sec "7. КАК ЧИТАТЬ РЕЗУЛЬТАТ (VDS vs ПЛАГИНЫ)"
VDS_FLAG="$(awk -v s="$STEAL_PCT" -v io="$IOWAIT_PCT" -v l="$LOAD1" -v n="$NCPU" -v p="$PING_AVG" '
  BEGIN { pp=(p+0); if (s+0>5 || io+0>15 || l+0>n*1.2 || (p ~ /^[0-9.]+$/ && pp>80)) print "VDS"; else print "PLUGINS_OR_CLIENT_NET" }')"
echo "СВОДКА: vCPU=$NCPU load1=$LOAD1 steal=${STEAL_PCT}% iowait=${IOWAIT_PCT}% pingVDS=${PING_AVG}мс"
echo "Авто-подозрение: $VDS_FLAG"
echo ""
cat <<'VERDICT'
ЕСЛИ в консоли MSPT > 50 или TPS < 18, ПРИ ЭТОМ steal < 5%, iowait < 15%
   и пинг с самого VDS до 1.1.1.1 нормальный  -> ВИНОВАТЫ ПЛАГИНЫ / игровой сервер.
   Смотри /spark profiler: в отчёте ищи плагин с biggest self-time.
   На этом сервере при росте онлайна в первую очередь проверяй:
     ModelEngine, MythicMobs, ElytrixFreakModel, FancyNpcs, DecentHolograms,
     ProtocolLib + ViaVersion/ViaBackwards (пакеты на каждого игрока),
     GrimAC, LPX, TAB + PlaceholderAPI (рефреш плейсхолдеров),
     FAWE, Orebfuscator, Chunky, LagFixer/WorldCleaner, MythicMobs мобы,
     GreatCombat / WPvPRoom / NPvp / WGExtender (комбат-теги), voicechat.

ЕСЛИ steal > 5..10% ИЛИ load1 > число vCPU ИЛИ iowait > 15%
   ИЛИ пинг с VDS до 1.1.1.1 тоже высокий / растут ретрансмиты
                                          -> ВИНОВАТ VDS / ХОСТ / СЕТЬ.
   Пиши хостеру: "CPU steal X%, iowait Y%, load Z на N vCPU, прошу проверить
   оверселл / диск / сетевой линк". Никакой плагин это не починит.

ЕСЛИ и MSPT/TPS нормальные, и VDS сеть чистая, а пинг растёт ТОЛЬКО у игроков:
   -> пакетный спам / proxies. Проверь ProtocolLib, ViaVersion, ModelEngine,
      FancyNpcs, DecentHolograms, TAB, scoreboard/placeholder refresh, voicechat UDP.
      Плюс /ss -s и retrans в разделе 2.

ВАЖНО: на этом сервере уже есть elytrix-perf-optimization-update.zip / apply_perf.sh.
Убедись, что он реально применён (в разделе 5 должны быть worker-threads: 1,
max-joins-per-tick: 3, netty-threads: 2, optimize-explosions: true).
Если этих строк нет — сначала накати перф-пакет, потом Profiler'ом ищи остатки.
VERDICT

sec "8. ЧТО ПРИСЛАТЬ МНЕ"
echo "1) Файл $OUT"
echo "2) Вывод команд из раздела 6 (tps / mspt / list / spark profiler ссылка)"
echo "3) Во сколько онлайн был пиковый и какой пинг показывал клиент"
echo ""
echo "Готово. Скрипт ничего не менял на сервере."
