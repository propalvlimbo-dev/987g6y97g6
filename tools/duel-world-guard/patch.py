#!/usr/bin/env python3
"""Experimental EpicDuels 3.1.2 world safety patch; see README.md."""
import hashlib, io, struct, sys, zipfile
from pathlib import Path

EXPECTED = '35590f2a7dafb915fd1c454284e1029f871aadda269dc4774f9cd6c6fce636e7'
def u2(x): return struct.pack('>H', x)
def u4(x): return struct.pack('>I', x)
def read2(b,p): return struct.unpack_from('>H',b,p)[0]
def read4(b,p): return struct.unpack_from('>I',b,p)[0]

def patch_class(b, changes):
    assert b[:4] == b'\xca\xfe\xba\xbe'
    n=read2(b,8); p=10; strings={}; i=1
    while i<n:
        tag=b[p]; p+=1
        if tag==1:
            l=read2(b,p); p+=2; strings[i]=b[p:p+l].decode('utf8'); p+=l
        elif tag in (3,4,9,10,11,12,17,18): p+=4
        elif tag in (5,6): p+=8; i+=1
        elif tag in (7,8,16,19,20): p+=2
        elif tag==15: p+=3
        else: raise ValueError('unknown constant '+str(tag))
        i+=1
    # Add a constant string for a truthful rename failure.
    msg=b'World protection: arena rename is disabled.'
    cp=b'\x01'+u2(len(msg))+msg+b'\x08'+u2(n)
    new_n=n+2
    cp_end=p
    # Skip class declaration, interfaces, and fields.
    p+=6; ic=read2(b,p); p+=2+2*ic
    def skip_member(pos):
        count=read2(b,pos+6); pos+=8
        for _ in range(count): pos+=6+read4(b,pos+2)
        return pos
    fc=read2(b,p); p+=2
    for _ in range(fc): p=skip_member(p)
    mcpos=p; mc=read2(b,p); p+=2
    out=bytearray(b[cp_end:p]); found=set()
    for _ in range(mc):
        start=p; name=strings[read2(b,p+2)]; desc=strings[read2(b,p+4)]; ac=read2(b,p+6); p+=8
        out+=b[start:p]
        for _ in range(ac):
            astart=p; ai=read2(b,p); length=read4(b,p+2); p+=6+length
            key=(name,desc)
            if key in changes and strings[ai]=='Code':
                kind=changes[key]; found.add(key)
                code={'false':b'\x03\xac','void':b'\xb1','error':b'\x13'+u2(n+1)+b'\xb0'}[kind]
                locals_=read2(b,astart+8)
                content=u2(1 if kind!='void' else 0)+u2(locals_)+u4(len(code))+code+u2(0)+u2(0)
                out+=u2(ai)+u4(len(content))+content
            else: out+=b[astart:p]
    if found != set(changes): raise ValueError('Missing methods: '+str(set(changes)-found))
    out+=b[p:]
    return b[:8]+u2(new_n)+b[10:cp_end]+cp+out

def main(inp,out):
    raw=Path(inp).read_bytes()
    digest=hashlib.sha256(raw).hexdigest()
    if digest!=EXPECTED: raise SystemExit('Unexpected input SHA-256: '+digest)
    updates={
      'dev/epicduels/manager/ArenaManager.class':{
        ('renameArena','(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;'):'error',
        ('deleteArena','(Ljava/lang/String;)Z'):'false'},
      'dev/epicduels/EpicDuels.class':{('cleanupLeftoverInstances','()V'):'void'},
    }
    seen=set()
    with zipfile.ZipFile(io.BytesIO(raw)) as src, zipfile.ZipFile(out,'w') as dst:
        for z in src.infolist():
            data=src.read(z.filename)
            if z.filename in updates:
                data=patch_class(data,updates[z.filename]); seen.add(z.filename)
            dst.writestr(z,data)
    assert seen == set(updates)
    print('Created',out,'SHA256',hashlib.sha256(Path(out).read_bytes()).hexdigest())
if __name__=='__main__':
    if len(sys.argv)!=3: raise SystemExit('usage: patch.py original.jar output.jar')
    main(*sys.argv[1:])
