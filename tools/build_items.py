#!/usr/bin/env python3
"""Patch the binary-only ElytrixItem 2.5.7. Requires jawa==2.2.0."""
import io, struct, zipfile
from pathlib import Path
from jawa.cf import ClassFile

root = Path(__file__).resolve().parents[1]
source = root / 'ElytrixItem-2.5.7.jar'
target = root / 'finalbuild/ElytrixItem-fix4.jar'
base = 'ru/rooyzee/elytrixitem/'

def replace_method(data, name, change):
    c = ClassFile(io.BytesIO(data))
    old = c.methods.find_one(name=name).code._code
    new = change(old)
    assert len(old) == len(new) and data.count(old) == 1
    return data.replace(old, new, 1)

def gear(data):
    def edit(code):
        # Bogatyr's elytra should have no enchantments, only the unbreakable flag.
        old = bytes.fromhex('1259b20054125bb70019b2002906b60022b2002c04b60022b6002f')
        new = bytes.fromhex('1259b20054125bb70019') + b'\x00' * 14 + bytes.fromhex('b6002f')
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

def patch_gear_creation(data):
    count, end = cp_end(data)
    # Append a static helper reference without changing existing constant indices.
    def utf(s):
        b = s.encode('utf-8'); return b'\x01' + struct.pack('>H', len(b)) + b
    u2 = lambda n: struct.pack('>H', n)
    name = base + 'item/impl/BogatyrElytraFix'
    desc = '(Lru/rooyzee/elytrixitem/item/impl/GearItem;Lorg/bukkit/inventory/ItemStack;)Lorg/bukkit/inventory/ItemStack;'
    pool = (utf(name) + b'\x07' + u2(count) + utf('finish') + utf(desc)
            + b'\x0c' + u2(count+2) + u2(count+3)
            + b'\x0a' + u2(count+1) + u2(count+4))
    def edit(code):
        old = bytes.fromhex('2bb8009a2bb0')
        new = b'\x2a\x2b\xb8' + u2(count+5) + b'\xb0'
        assert code.count(old) == 1
        return code.replace(old, new, 1)
    data = replace_method(data, 'createItem', edit)
    return data[:8] + u2(count+6) + data[10:end] + pool + data[end:]

def bogatyr_helper():
    # Java 5 verifier infers frames; this straight-line helper has one branch.
    helper = base + 'item/impl/BogatyrElytraFix'
    gear_type = base + 'item/impl/GearItem'
    c = ClassFile.create(helper); c.version = (49, 0)
    m = c.methods.create('finish', '(L'+gear_type+';Lorg/bukkit/inventory/ItemStack;)Lorg/bukkit/inventory/ItemStack;', code=True)
    m.access_flags.acc_public = True; m.access_flags.acc_static = True
    def u2(n): return struct.pack('>H', n)
    details = c.constants.create_method_ref(base+'util/ItemStackUtil','showAllDetails','(Lorg/bukkit/inventory/ItemStack;)V').index
    ident = c.constants.create_string('elytra1').index
    getid = c.constants.create_method_ref(gear_type,'getId','()Ljava/lang/String;').index
    equals = c.constants.create_method_ref('java/lang/String','equals','(Ljava/lang/Object;)Z').index
    getmeta = c.constants.create_method_ref('org/bukkit/inventory/ItemStack','getItemMeta','()Lorg/bukkit/inventory/meta/ItemMeta;').index
    unbreakable = c.constants.create_interface_method_ref('org/bukkit/inventory/meta/ItemMeta','setUnbreakable','(Z)V').index
    setmeta = c.constants.create_method_ref('org/bukkit/inventory/ItemStack','setItemMeta','(Lorg/bukkit/inventory/meta/ItemMeta;)Z').index
    prefix = (b'\x2b\xb8'+u2(details)+b'\x13'+u2(ident)+b'\x2a\xb6'+u2(getid)+b'\xb6'+u2(equals))
    body = (b'\x2b\xb6'+u2(getmeta)+b'\x4d\x2c\x04\xb9'+u2(unbreakable)+b'\x02\x00'
            +b'\x2b\x2c\xb6'+u2(setmeta)+b'\x57')
    m.code.max_stack = 2; m.code.max_locals = 3
    m.code._code = prefix + b'\x99' + u2(len(body)+3) + body + b'\x2b\xb0'
    out = io.BytesIO(); c.save(out); return out.getvalue()

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

def patch_trap(data):
    data = fix_worldedit(data)
    count, end = cp_end(data)
    def u2(n): return struct.pack('>H', n)
    def utf(s):
        b = s.encode(); return b'\x01' + u2(len(b)) + b
    pool = (utf(base+'item/impl/TrapSafety') + b'\x07' + u2(count)
            + utf('prepare')
            + utf('(Ljava/lang/Object;Lorg/bukkit/entity/Player;Lorg/bukkit/World;IIIIIILjava/util/Map;)Z')
            + b'\x0c' + u2(count+2) + u2(count+3) + b'\x0a' + u2(count+1) + u2(count+4)
            + utf('restore') + utf('(Lorg/bukkit/block/BlockState;ZZ)Z')
            + b'\x0c' + u2(count+6) + u2(count+7) + b'\x0a' + u2(count+1) + u2(count+8))
    def prepare(code):
        old = bytes.fromhex('b602159a0006a7015c')
        assert code.count(old) == 1
        return code.replace(old, b'\xb8'+u2(count+5)+old[3:])
    def restore(code):
        old = bytes.fromhex('b902520300')
        assert code.count(old) == 1
        return code.replace(old, b'\xb8'+u2(count+9)+b'\x00\x00')
    data = replace_method(data, 'pasteSchematic', prepare)
    data = replace_method(data, 'tick', restore)
    return data[:8]+u2(count+10)+data[10:end]+pool+data[end:]

with zipfile.ZipFile(source) as src, zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as out:
    schematic = src.read('schem/trapa.schem')
    for info in src.infolist():
        data = src.read(info.filename)
        if info.filename == base+'item/GearItems.class': data=gear(data)
        if info.filename == base+'item/impl/GearItem.class': data=patch_gear_creation(data)
        if info.filename == base+'item/ConsumableItems.class': data=arrows(data)
        if info.filename == base+'item/impl/ExplosiveTrapListener.class': data=patch_trap(data)
        if info.filename == 'plugin.yml':
            data = data.replace(b"version: '2.5.7'", b"version: '2.5.7-fix4'")
            data = data.replace(b'softdepend: [WorldGuard, Vault]', b'softdepend: [WorldGuard, Vault, WorldEdit, FastAsyncWorldEdit]')
        out.writestr(info,data)
    out.writestr(base+'item/impl/BogatyrElytraFix.class', bogatyr_helper())
    for name in ('TrapSafety.class', 'TrapSafety$DeferredRestore.class'):
        out.writestr(base+'item/impl/'+name, (root/'tools/items-classes'/name).read_bytes())
    (root/'finalbuild/trapa.schem').write_bytes(schematic)
print(target)
