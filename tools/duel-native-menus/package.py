#!/usr/bin/env python3
"""Produce an experimental native-menu bridge from the exact supplied bridge JAR."""
from pathlib import Path
from zipfile import ZipFile
import hashlib, sys

if len(sys.argv) != 4:
    raise SystemExit('usage: package.py original-bridge.jar compiled-classes output.jar')
source, classes, output = map(Path, sys.argv[1:])
if hashlib.sha256(source.read_bytes()).hexdigest() != '1e06b3f967a25bbab36ad683fee35ddcac82653b6fe8d7780fb9628b1ad7d5d8':
    raise SystemExit('unexpected input bridge version')
prefix = 'ru/elytrix/duelsbridge/'
new = ['Menus.class', 'Menus$Layout.class', 'Menus$Button.class', 'Menus$MenuHolder.class', 'Layouts.class']
with ZipFile(source) as src, ZipFile(output, 'w') as dst:
    for info in src.infolist():
        if info.filename == prefix + 'Menus.class':
            continue
        data = src.read(info.filename)
        if info.filename == 'plugin.yml':
            old = b'depend: [EpicDuels, BMenu]'
            assert data.count(old) == 1
            data = data.replace(old, b'depend: [EpicDuels]\nsoftdepend: [BMenu]')
            data = data.replace('Меню EpicDuels в стиле ElytriX на BMenu'.encode(), 'Встроенные меню дуэлей ElytriX'.encode())
        if info.filename == prefix + 'ElytrixDuelsBridge.class':
            def utf(old, new):
                a, b = old.encode(), new.encode()
                before = b'\x01' + len(a).to_bytes(2, 'big') + a
                after = b'\x01' + len(b).to_bytes(2, 'big') + b
                return before, after
            for old_message, new_message in [
                ('BMenu не найден: окна EpicDuels будут открываться как есть.', 'BMenu не найден: используются встроенные меню дуэлей.'),
                ('ElytriX: мосты меню EpicDuels -> BMenu включены', 'ElytriX: встроенные меню дуэлей включены'),
            ]:
                before, after = utf(old_message, new_message)
                assert data.count(before) == 1
                data = data.replace(before, after)
        dst.writestr(info, data)
    for name in new:
        dst.write(classes / prefix / name, prefix + name)
print('built', output, hashlib.sha256(output.read_bytes()).hexdigest())
