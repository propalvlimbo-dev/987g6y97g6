# Диагностика пинга при большом онлайне

Одна команда в SSH на VDS (ничего не меняет на сервере, только собирает цифры):

```bash
cd /root/RW_GRIEF && curl -fsSL -o /tmp/ping_diag.sh "https://raw.githubusercontent.com/propalvlimbo-dev/987g6y97g6/arena/0bbb1289-987g6y97g6/ping_diag.sh" && bash /tmp/ping_diag.sh /root/RW_GRIEF
```

Если `curl` нет:

```bash
cd /root/RW_GRIEF && wget -O /tmp/ping_diag.sh "https://raw.githubusercontent.com/propalvlimbo-dev/987g6y97g6/arena/0bbb1289-987g6y97g6/ping_diag.sh" && bash /tmp/ping_diag.sh /root/RW_GRIEF
```

Скрипт сохранит лог в `/tmp/elytrix-ping-<дата>.log` и напечатает:

- CPU `steal` / `iowait` / load average за 3 секунды,
- пинг с самого VDS до `1.1.1.1`, `ss -s`, ретрансмиты,
- PID java, `-Xmx`/GC, топ стеков из `jcmd Thread.print`,
- `Can't keep up` / таймауты из `logs/latest.log`,
- применён ли `apply_perf.sh` (`worker-threads`, `max-joins-per-tick`, `netty-threads`),
- блок команд для **консоли сервера** и правило, кто виноват.

## В консоль сервера (не в SSH)

```text
tps
mspt
list
ver ElytrixProhibition
plugman list
wg reload
spark profiler --timeout 300 --only-ticks-over 50
spark tickmonitor --threshold 50 --without-gc
```

Если Spark не установлен: положить `spark-bukkit.jar` в `plugins/`, затем `plugman load spark` и снова `/spark profiler --timeout 300`.

## Как отделить VDS от плагинов

| Признак | Виновник |
|---|---|
| `MSPT > 50` / `TPS < 18`, но `steal < 5%`, `iowait < 15%`, пинг с VDS нормальный | **Плагины / игровой сервер**. Смотреть `/spark profiler`. |
| `steal > 5..10%`, `load1 > число vCPU`, `iowait > 15%`, пинг с VDS тоже высокий, растут ретрансмиты | **VDS / хост / сеть**. Писать хостеру, плагины не помогут. |
| И MSPT нормальный, и VDS сеть чистая, а пинг растёт только у игроков | **Пакетный спам**: ProtocolLib, ViaVersion, ModelEngine, FancyNpcs, DecentHolograms, TAB/PlaceholderAPI, voicechat. |

На этом сервере 93 плагина и 3 vCPU. При пиковом онлайне в первую очередь профилировать:
`ModelEngine`, `MythicMobs`, `ElytrixFreakModel`, `FancyNpcs`, `DecentHolograms`,
`ProtocolLib` + `ViaVersion/ViaBackwards`, `GrimAC`, `LPX`, `TAB` + `PlaceholderAPI`,
`FastAsyncWorldEdit`, `Orebfuscator`, `Chunky`, `LagFixer/WorldCleaner`,
`GreatCombat / WPvPRoom / NPvp / WGExtender` (комбат-теги), `voicechat`.

Пришлите: лог скрипта + вывод `tps`/`mspt`/`list` + ссылку из `/spark profiler` + онлайн в момент просадки.
