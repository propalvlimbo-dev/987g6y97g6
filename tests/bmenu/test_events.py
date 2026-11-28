"""PYTHONPATH=/tmp/bmenu-pyyaml python3 tests/bmenu/test_events.py
Checks the three event menu overlays and the plugin expansions shipped with them.
"""
from pathlib import Path
import re, zipfile
import yaml
root=Path(__file__).resolve().parents[2]
folder=root/'BMenu/menu/elytrix/events'
menus={name:yaml.safe_load((folder/(name+'.yml')).read_text(encoding='utf-8')) for name in ['events','airdrop_event','merchant_event']}
expected={'events':'elytrix:events','airdrop_event':'elytrix:airdrop_event','merchant_event':'elytrix:merchant_event'}
for name,doc in menus.items():
 assert doc['id']==expected[name] and doc['type']=='chest' and doc['size']==54
 assert doc['animation'] and all('opcodes' in f for f in doc['animation'])
 for frame in doc['animation']:
  for op in frame['opcodes']:
   if isinstance(op,dict) and 'st' in op:
    item, slots=op['st'].split(' ',1)
    assert item in doc['items']
    assert all(0<=int(s)<doc['size'] for s in slots.split(','))
texts='\n'.join((folder/(n+'.yml')).read_text(encoding='utf-8') for n in menus)
placeholders=set(re.findall(r'%([^%]+)%',texts))
assert placeholders=={'elytrixairdrop_time','elytrixairdrop_type','elytrixairdrop_stage','elytrixairdrop_location','elytrixtrader_tracker','elytrixtrader_location'},placeholders
source=(root/'tools/elytrix-airdrop/src/ru/rooyzee/elytrixairdrop/placeholders/AirdropPlaceholder.java').read_text(encoding='utf-8')
for p in placeholders:
 if p.startswith('elytrixairdrop_'): assert 'case "'+p.removeprefix('elytrixairdrop_')+'"' in source,p
with zipfile.ZipFile(root/'finalbuild/ElytrixAirdrop-TEST23.jar') as jar:
 airdrop=jar.read('ru/rooyzee/elytrixairdrop/placeholders/AirdropPlaceholder.class')
 for s in (b'elytrixairdrop',b'time',b'type',b'stage',b'location'): assert s in airdrop
with zipfile.ZipFile(root/'finalbuild/ElytrixTrader-TEST5.jar') as jar:
 trader=jar.read('ru/rooyzee/elytrixtrader/expansion/TraderExpansion.class')
 for s in (b'elytrixtrader',b'tracker',b'location'): assert s in trader
for item in ('airdrops','exp_merchant'):
 assert menus['events']['items'][item]['tick_speed']==20 and menus['events']['items'][item]['on_tick']=='[update]'
for name,item in [('airdrop_event','informations'),('merchant_event','informations'),('merchant_event','coordinates')]:
 d=menus[name]['items'][item];assert d['tick_speed']==20 and d['on_tick']=='[update]'
assert menus['events']['items']['airdrops']['on_click']==['[open] elytrix:airdrop_event']
assert menus['events']['items']['exp_merchant']['on_click']==['[open] elytrix:merchant_event']
assert menus['events']['items']['talisman']['on_click']==['[open] elytrix:talisman_event']
assert menus['merchant_event']['items']['back']['on_click']==['[open] elytrix:events']
assert menus['airdrop_event']['items']['close']['on_click']==['[open] elytrix:events']
with zipfile.ZipFile(root/'finalbuild/BMenu.zip') as original:
 for name in menus:
  previous=yaml.safe_load(original.read('BMenu/menu/elytrix/events/'+name+'.yml'))
  assert menus[name]['animation']==previous['animation'],name
 assert yaml.safe_load(original.read('BMenu/menu/elytrix/events/talisman_event.yml'))['id']=='elytrix:talisman_event'
assert '0' not in ' '.join(menus['merchant_event']['items']['coordinates']['lore'])
print('Event menus: YAML, slots, links, placeholder names and updates OK')
