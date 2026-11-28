# ElytrixTrader TEST1

Patches the original `ElytrixTrader.jar` from main commit dd05efe (SHA-256 `2344ad2f86ffc7189960824770feb4e8a5836b31fce9772f7962056ca883a62a`). Build with `./tools/elytrix-trader/build.sh /absolute/path/ElytrixTrader.jar`; see script for dependency paths. The compiled artifact is `finalbuild/ElytrixTrader-TEST1.jar`.

Only `traders/experienced.yml` is loaded as a trader; other trader config files are left intact. Existing trade files are still loaded, including shared IDs referenced by experienced. Existing configuration, trade editor, scheduling and placeholder expansion are not reset. Hologram text and placeholders remain configured by `experienced.yml`; the first line becomes a larger pink title, default green accent becomes pink, while custom body colors remain. Global and trader hologram enable flags and height remain respected. The display layout uses fixed Airdrop-style spacing rather than the old armor stand line-height.

Two nonpersistent TextDisplays use transparent background, shadow and title/body scaling matching Airdrop. They are removed on animation, despawn, reload and disable; missing/cancelled display spawns retry when the NPC updates, without forcing chunks to load. The virtual armor stands are disabled. Basic filtering and Java bytecode verification were run; no live Minecraft server test was available. Back up the existing plugin data before installing and test trading, chunk unload/reload, animation and plugin reload on the target server.

## TEST2 — вращающаяся галактика вместо схематики

При появлении торговца схематика больше не вставляется, даже если `schematic.enabled` остался включён в старом `experienced.yml`. Файлы схематики и настройки на диске не удаляются. В безопасной точке спавна под ногами появляются 28 маленьких неколлизионных BlockDisplay: две вращающиеся спирали из фиолетовых, розовых и синих блоков, их оттенки плавно сменяются по времени. Реальные блоки и физика мира не меняются. Визуальная анимация запускается независимо от настроек голограммы; при анимации исчезновения, удалении торговца, reload и отключении плагина сущности и задача удаляются. При выгрузке чанка отображение удаляется и создаётся снова после загрузки.

Сборка: `./tools/elytrix-trader/build-galaxy.sh /absolute/path/ElytrixTrader.jar [TEST1.jar]`; использует исходный JAR и классы TEST1, ограниченные Bukkit-заглушки нужны только для сборки и теста (в плагин не вкладываются). Проверены жизненный цикл, вращение, повторная загрузка, очистка и ASM-хуки отключения схематики. Реальный серверный тест внешнего вида, анимации и совместимости не проводился: сначала проверить на тестовом сервере. В уже размещённом мире старые постройки из предыдущих версий автоматически не удаляются.

## TEST3 — зелёная галактика

Галактика переработана: три тонких спиральных рукава и редкая внешняя орбита из 72 небольших светящихся блоков (зелёное стекло, изумруд, лайм и светлые зелёные акценты), с неспешным вращением, постепенным мерцанием и зелёными пылинками. Эффект не изменяет блоки мира; при нехватке загруженных соседних чанков он не загружает их принудительно. Вместо смены цвета у всех элементов одновременно цвета сменяются по одному. В сравнении с TEST2 увеличено количество display-сущностей и обновлений, поэтому стоит проверить нагрузку при нескольких торговцах. Проверки геометрии, частиц, движения, удаления/перезагрузки чанка, байткода и отключения схематики прошли на заглушках; внешний вид и производительность в игре не проверены. Скрипт `build-galaxy.sh` теперь выпускает TEST3.

## TEST4 — сферы опыта вокруг торговца

Плоская галактика из блоков и её частицы полностью убраны. Вокруг торговца на высоте тела плавно летают шесть настоящих по внешнему виду сфер опыта, на двух встречных орбитах с плавными подъёмами и спусками. Они создаются пакетно существующей обязательной зависимостью VirtualEntityApi, а не как серверные сущности опыта: не дают опыт, не подбираются игроками и не меняют мир. При исчезновении торговца, остановке, перезагрузке и отключении отправляется удаление сфер, задача отменяется; при смене мира трекер пересоздаётся. Радиус показа — 64 блока.

Проверены геометрия, движение, пересоздание при смене мира, очистка и отсутствие BlockDisplay/серверных сущностей в новой анимации на заглушках и в байткоде; новая анимация также прошла JVM verification с фактическим VirtualEntityApi. Отображение, поведение клиентских сфер и совместимость конкретной версии VirtualEntityApi в игре не проверены — сначала запустить тест на сервере. Сборка TEST4: `build-galaxy.sh`.
