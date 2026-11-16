#!/usr/bin/env python3
"""Build the supplied binary-only plugin with a renamed expansion and compact expiry.
Requires jawa==2.2.0. The original jar is an input; no server classes are bundled.
"""
import io, struct, zipfile
from pathlib import Path
from jawa.cf import ClassFile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'ElytrixSubscribe-2.0.1.jar'
TARGET = ROOT / 'finalbuild/ElytrixSubscribe.jar'
CLASS = 'ru/rooyzee/elytrixsubscribe/placeholder/CustomPlaceHolder.class'
HELPER = 'ru/rooyzee/elytrixsubscribe/placeholder/RemainingDays'

def u2(i): return struct.pack('>H', i)

def patch(data):
    # Preserve bytecode offsets, StackMapTable, and every unrelated method verbatim.
    assert data[:4] == b'\xca\xfe\xba\xbe'
    count = struct.unpack_from('>H', data, 8)[0]
    pos, i, pool = 10, 1, bytearray()
    while i < count:
        start = pos; tag = data[pos]; pos += 1
        if tag == 1:
            n = struct.unpack_from('>H', data, pos)[0]; pos += 2 + n
            raw = data[start+3:pos]
            if raw in (b'slidesubscribe', '&7Нет'.encode('utf-8')):
                raw = b'elytrixsubscribe' if raw == b'slidesubscribe' else 'Нет'.encode('utf-8')
                pool += b'\x01' + u2(len(raw)) + raw
            else: pool += data[start:pos]
        else:
            size = {3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}[tag]
            pos += size; pool += data[start:pos]
            if tag in (5,6): i += 1
        i += 1
    entries = []
    def add(tag, content):
        entries.append(bytes([tag]) + content); return count + len(entries) - 1
    def utf(s):
        b = s.encode(); return add(1, u2(len(b)) + b)
    def cls(s): return add(7,u2(utf(s)))
    def ref(tag, owner, name, desc):
        c = cls(owner); n = utf(name); d = utf(desc); nt = add(12,u2(n)+u2(d)); return add(tag,u2(c)+u2(nt))
    service = ref(10,'ru/rooyzee/elytrixsubscribe/Main','getService','()Lru/rooyzee/elytrixsubscribe/service/SubService;')
    uuid = ref(11,'org/bukkit/entity/Player','getUniqueId','()Ljava/util/UUID;')
    expiry = ref(10,'ru/rooyzee/elytrixsubscribe/service/SubService','getTimeToEnd','(Ljava/util/UUID;)J')
    remaining = ref(10,HELPER,'format','(J)Ljava/lang/String;')
    old = bytes.fromhex('2ab4000db600442db6005eba00620000b80058b0')
    new = b'\x2a\xb4\x00\x0d\xb6'+u2(service)+b'\x2d\xb9'+u2(uuid)+b'\x01\x00\xb6'+u2(expiry)+b'\xb8'+u2(remaining)+b'\xb0'
    assert len(old)==len(new) and data[pos:].count(old)==1
    return data[:8]+u2(count+len(entries))+pool+b''.join(entries)+data[pos:].replace(old,new,1)

def helper():
    c = ClassFile.create(HELPER); c.version=(52,0)
    m = c.methods.create('format','(J)Ljava/lang/String;', code=True)
    m.access_flags.acc_public=True; m.access_flags.acc_static=True
    now = c.constants.create_method_ref('java/lang/System','currentTimeMillis','()J').index
    clamp = c.constants.create_method_ref('java/lang/Math','max','(JJ)J').index
    day = c.constants.create_long(86400000).index
    almost = c.constants.create_long(86399999).index
    tostring = c.constants.create_method_ref('java/lang/String','valueOf','(J)Ljava/lang/String;').index
    suffix = c.constants.create_string('д').index
    concat = c.constants.create_method_ref('java/lang/String','concat','(Ljava/lang/String;)Ljava/lang/String;').index
    # ceil(max(0, expiry - now) / 86400000), with at least 1 day for an active subscription.
    m.code.max_stack=6; m.code.max_locals=2
    m.code._code=(b'\x09\x1e\xb8'+u2(now)+b'\x65\xb8'+u2(clamp)+b'\x14'+u2(almost)+b'\x61\x14'+u2(day)+b'\x6d\xb8'+u2(tostring)+b'\x13'+u2(suffix)+b'\xb6'+u2(concat)+b'\xb0')
    out=io.BytesIO(); c.save(out); return out.getvalue()

TARGET.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(SOURCE) as src, zipfile.ZipFile(TARGET,'w',zipfile.ZIP_DEFLATED) as dst:
    for info in src.infolist():
        data=src.read(info.filename)
        if info.filename==CLASS: data=patch(data)
        if info.filename=='config.yml':
            s=data.decode('utf-8'); s='\n'.join(line for line in s.splitlines() if not line.lstrip().startswith('#'))+'\n'
            s=s.replace("noHaveSubscribePlaceholder: '&7Нет'", "noHaveSubscribePlaceholder: 'Нет'")
            s=s.replace('&fОшибка:', '&cОшибка:').replace('&fВам была успешно выдана подписка!', '&aПодписка успешно выдана!')
            data=s.encode('utf-8')
        dst.writestr(info,data)
    dst.writestr(HELPER+'.class',helper())
print(TARGET)
