"""Check edited shop menus and (optionally) compare to the original BMenu zip.

PYTHONPATH=/path/to/pyyaml python3 tests/bmenu/test_shop.py [/path/to/BMenu.zip]
"""
from pathlib import Path
import re
import sys
import zipfile
import yaml

root = Path(__file__).resolve().parents[2]
shop = root / 'BMenu/menu/elytrix/shop'
arrows_text = (shop / 'arrows.yml').read_text(encoding='utf-8')
unique_text = (shop / 'unical_items.yml').read_text(encoding='utf-8')
arrows = yaml.safe_load(arrows_text)
unique = yaml.safe_load(unique_text)
assert arrows['id'] == 'elytrix:arrows' and unique['id'] == 'elytrix:unical_items'
assert arrows['size'] == unique['size'] == 54
for i in range(1, 6):
    item = arrows['items'][f'arrow{i}']
    times = re.findall(r'\(0:\d\d\)', '\n'.join(item['lore']))
    assert times and set(times) == ({'(0:05)'} if i == 5 else {'(0:03)'})
    commands = item['on_click'][0]['do']
    assert any(x.startswith('[console] eitems give ') for x in commands)
trap = unique['items']['explosive_trap']
assert trap['material'] == 'fire_charge'
assert any('39 коинов' in line for line in trap['lore'])
assert any('3 блока' in line for line in trap['lore'])
condition = trap['on_click'][0]
assert condition['if'] == '%playerpoints_points% >= 39'
assert '[console] eitems give %player_name% explosive_trap 1' in condition['do']
assert '[console] p take %player_name% 39' in condition['do']
assert not any('eitems give' in x or 'p take' in x for x in condition['else'])
slots = []
for frame in unique['animation']:
    for op in frame['opcodes']:
        if isinstance(op, dict) and 'st' in op:
            name, positions = op['st'].split(maxsplit=1)
            for slot in positions.split(','):
                if int(slot) == 30:
                    slots.append(name)
assert slots == ['explosive_trap'], slots

if len(sys.argv) > 1:
    with zipfile.ZipFile(sys.argv[1]) as original:
        orig_arrow = original.read('BMenu/menu/elytrix/shop/arrows.yml').decode('utf-8')
        orig_unique = yaml.safe_load(original.read('BMenu/menu/elytrix/shop/unical_items.yml'))
    assert arrows_text == orig_arrow.replace('(0:10)', '(0:03)')
    for key, item in orig_unique['items'].items():
        assert unique['items'][key] == item, key
    orig_animation = [f for f in unique['animation'] if not any(isinstance(o, dict) and o.get('st') == 'explosive_trap 30' for o in f['opcodes'])]
    assert orig_animation == orig_unique['animation']
print('BMenu shop checks passed')
