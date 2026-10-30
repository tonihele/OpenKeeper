#!/usr/bin/env python3
"""Build a quiet native lava area from the already prepared lighting baseline."""
import hashlib,json,struct
from pathlib import Path
GAME=Path('/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2')
MAPS=GAME/'Data/editor/maps'
SOURCE='LightBase_______'
NAME='LavaProbe_______'
SUFFIXES=('.kwd','Globals.kld','Map.kld','Players.kld','Things.kld','Triggers.kld','Variables.kld')
OUTPUT=Path('/tmp/dkii-lava-probe')
files={s:(MAPS/(SOURCE+s)).read_bytes() for s in SUFFIXES}
inputs={SOURCE+s:hashlib.sha256(d).hexdigest() for s,d in files.items()}
files['.kwd']=files['.kwd'].replace(SOURCE.encode(),NAME.encode()).replace(SOURCE.encode('utf-16le'),NAME.encode('utf-16le'))
d=bytearray(files['Map.kld']);w,h=struct.unpack_from('<II',d,20);assert(w,h)==(48,48)
for y in range(37,45):
 for x in range(12,20):
  off=36+(y*w+x)*4;d[off:off+4]=bytes((5,0,1,0))
files['Map.kld']=bytes(d)
d=bytearray(files['Players.kld']);struct.pack_into('<HH',d,56+2*205+169,15,41);files['Players.kld']=bytes(d)
OUTPUT.mkdir(exist_ok=True)
assert not any((OUTPUT/(NAME+s)).exists() for s in SUFFIXES)
for s,d in files.items(): (OUTPUT/(NAME+s)).write_bytes(d)
(OUTPUT/'manifest.json').write_text(json.dumps(dict(name=NAME,lava=[12,37,19,44],camera=[15,41],source_sha256=inputs,files={NAME+s:hashlib.sha256(d).hexdigest() for s,d in files.items()}),indent=2)+'\n')
print(OUTPUT)
