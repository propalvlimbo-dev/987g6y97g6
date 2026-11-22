#!/usr/bin/env python3
"""Patch the binary-only ElytrixItem 2.5.7. Requires jawa==2.2.0."""
import io, struct, zipfile
from pathlib import Path
from jawa.cf import ClassFile

root = Path(__file__).resolve().parents[1]
source = root / 'ElytrixItem-2.5.7.jar'
target = root / 'finalbuild/ElytrixItem.jar'
base = 'ru/rooyzee/elytrixitem/'

def replace_method(data, name, change):
    c = ClassFile(io.BytesIO(data))
    old = c.methods.find_one(name=name).code._code
    new = change(old)
    assert len(old) == len(new) and data.count(old) == 1
    return data.replace(old, new, 1)

def gear(data):
    def edit(code):
        # elytra1 already has Unbreaking III; remove only its Mending I.
        old = bytes.fromhex('1259b20054125bb70019b2002906b60022b2002c04b60022b6002f')
        new = bytes.fromhex('1259b20054125bb70019b2002906b60022') + b'\x00' * 7 + bytes.fromhex('b6002f')
        assert code.count(old) == 1
        return code.replace(old, new, 1)
    return replace_method(data, 'register', edit)

def cp_end(data):
    count = struct.unpack_from('>H',data,8)[0]; p=10; i=1
    while i<count:
        tag=data[p]; p+=1
        if tag==1:
            length=struct.unpack_from('>H',data,p)[0]; p+=2+length
        else:
            p+={3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}[tag]
            if tag in (5,6): i+=1
        i+=1
    return count,p

def arrows(data):
    # 1.21.11 applies the tipped-arrow potion duration scale (1/8).
    # 480 ticks becomes 3 seconds and 800 becomes 5 seconds on hit.
    count, end = cp_end(data)
    assert count < 256
    def edit(code):
        assert code.count(bytes.fromhex('1100c8')) == 9
        code = code.replace(bytes.fromhex('1100c8'), bytes.fromhex('1101e0'))
        old = bytes.fromhex('b20062106404b8002b')
        assert code.count(old) == 1
        return code.replace(old, bytes.fromhex('b2006212') + bytes([count]) + bytes.fromhex('04b8002b'))
    data = replace_method(data, 'registerArrows', edit)
    return data[:8] + struct.pack('>H', count+1) + data[10:end] + b'\x03' + struct.pack('>i',800) + data[end:]

def fix_worldedit(data):
    # FAWE and WorldEdit expose adapt(BlockStateHolder), not adapt(BlockState).
    old = b'(Lcom/sk89q/worldedit/world/block/BlockState;)Lorg/bukkit/block/data/BlockData;'
    new = b'(Lcom/sk89q/worldedit/world/block/BlockStateHolder;)Lorg/bukkit/block/data/BlockData;'
    before = b'\x01' + struct.pack('>H', len(old)) + old
    after = b'\x01' + struct.pack('>H', len(new)) + new
    assert data.count(before) == 1
    return data.replace(before, after, 1)

with zipfile.ZipFile(source) as src, zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as out:
    schematic = src.read('schem/trapa.schem')
    for info in src.infolist():
        data = src.read(info.filename)
        if info.filename == base+'item/GearItems.class': data=gear(data)
        if info.filename == base+'item/ConsumableItems.class': data=arrows(data)
        if info.filename == base+'item/impl/ExplosiveTrapListener.class': data=fix_worldedit(data)
        if info.filename == 'plugin.yml':
            data = data.replace(b'softdepend: [WorldGuard, Vault]', b'softdepend: [WorldGuard, Vault, WorldEdit, FastAsyncWorldEdit]')
        out.writestr(info,data)
    (root/'finalbuild/trapa.schem').write_bytes(schematic)
print(target)
