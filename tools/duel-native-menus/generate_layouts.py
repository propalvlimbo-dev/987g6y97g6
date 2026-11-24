#!/usr/bin/env python3
"""Convert the supplied BMenu reference into immutable Java layouts (no animation)."""
import json, pathlib, yaml
base=pathlib.Path(__file__).parent
out=base/'src/ru/elytrix/duelsbridge/Layouts.java'
def q(x): return json.dumps(str(x),ensure_ascii=False)
lines=['package ru.elytrix.duelsbridge;', '', 'final class Layouts {', '  private Layouts() {}', '  static void load(Menus menus) {']
for path in sorted((base/'reference').glob('*.yml')):
 d=yaml.safe_load(path.read_text(encoding='utf8')); items=d['items']; slots={}
 for frame in d.get('animation',[]):
  for op in frame.get('opcodes',[]):
   if isinstance(op,dict) and 'st' in op:
    name, positions=op['st'].split(' ',1)
    if name not in items: raise ValueError((path,name))
    for s in positions.split(','): slots[int(s)]=name
 lines.append('    menus.layout(%s, %s, %s);'%(q(d['id']),int(d['size']),q(d['title'])))
 for key, value in d.get('args', {}).items():
  lines.append('    menus.defaultArg(%s, %s, %s);'%(q(d['id']),q(key),q(value)))
 for slot,name in sorted(slots.items()):
  item=items[name]; actions=[]
  for action in item.get('on_click',[]):
   if not isinstance(action,str): continue
   if action.startswith('[player] '): actions.append('cmd:'+action[9:])
   elif action.startswith('[open] '): actions.append('open:'+action[7:])
   elif action=='[close]': actions.append('close')
   else: raise ValueError((path,name,action))
  lore=item.get('lore',[])
  lines.append('    menus.item(%s, %d, %s, %s, new String[]{%s}, new String[]{%s});'%(q(d['id']),slot,q(item['material'].upper()),q(item.get('name','')), ','.join(map(q,lore)), ','.join(map(q,actions))))
lines+=['  }','}']
out.write_text('\n'.join(lines)+'\n',encoding='utf8')
print('generated',out)
