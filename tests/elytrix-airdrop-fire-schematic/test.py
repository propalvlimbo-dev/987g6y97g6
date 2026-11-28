#!/usr/bin/env python3
"""Regression against the actual user-supplied Sponge schematic; standard library only."""
import gzip,struct,hashlib,collections,pathlib
path=pathlib.Path(__file__).with_name('fire.schem')
raw=path.read_bytes(); assert hashlib.sha256(raw).hexdigest()=='12f770b65e8928f0ecbd371a0942ee2d1df9bd668e11e72dd2012c7525c0ec1e'
data=memoryview(gzip.decompress(raw)); offset=0
def read(fmt):
 global offset
 size=struct.calcsize(fmt);val=struct.unpack_from(fmt,data,offset);offset+=size;return val[0] if len(val)==1 else val
def string():
 global offset
 n=read('>H');s=data[offset:offset+n].tobytes().decode();offset+=n;return s
def tag(kind):
 global offset
 if kind==1:return read('>b')
 if kind==2:return read('>h')
 if kind==3:return read('>i')
 if kind==4:return read('>q')
 if kind==5:return read('>f')
 if kind==6:return read('>d')
 if kind==7:
  n=read('>i');v=data[offset:offset+n].tobytes();offset+=n;return v
 if kind==8:return string()
 if kind==9:
  subtype=read('>B');n=read('>i');return [tag(subtype) for _ in range(n)]
 if kind==10:
  result={}
  while True:
   k=read('>B')
   if not k:return result
   name=string();result[name]=tag(k)
 if kind in (11,12):
  n=read('>i');return [read('>i' if kind==11 else '>q') for _ in range(n)]
 raise ValueError(kind)
assert read('>B')==10; string(); n=tag(10)
w,h,l=n['Width'],n['Height'],n['Length']; assert (w,h,l)==(41,6,41)
assert (n['Metadata']['WEOffsetX'],n['Metadata']['WEOffsetY'],n['Metadata']['WEOffsetZ'])==(-21,-5,-22)
pal={v:k for k,v in n['Palette'].items()}; blocks=n['BlockData'];assert len(blocks)==w*h*l and all(x<128 for x in blocks)
def name(x,y,z):return pal[blocks[(y*l+z)*w+x]]
def solid(s):return s!='minecraft:air' and not s.startswith(('minecraft:chain','minecraft:lantern'))
def full(s):return solid(s) and not any(part in s for part in ('_slab[','_stairs['))
columns={(x,z) for x in range(w) for z in range(l) if any(solid(name(x,y,z)) for y in range(h))}
assert len(columns)==1313
outside={(-1,-1)};queue=collections.deque(outside);dirs=((1,0),(-1,0),(0,1),(0,-1))
while queue:
 x,z=queue.popleft()
 for dx,dz in dirs:
  p=(x+dx,z+dz)
  if -1<=p[0]<=w and -1<=p[1]<=l and p not in outside and p not in columns:outside.add(p);queue.append(p)
edges=[(x,z) for x,z in columns if any((x+dx,z+dz) in outside for dx,dz in dirs)]
assert len(edges)==112
levels=collections.Counter(max(y for y in range(h) if full(name(x,y,z))) for x,z in edges)
assert levels=={4:101,3:11},levels
assert all(solid(name(x,0,z)) for x,z in edges) # OLD implementation picked underground y=0
print('User fire.schem: 41x6x41, 112 edge columns; top anchors y4=101/y3=11 (not underground y0)')
