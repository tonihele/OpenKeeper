#!/usr/bin/env python3
"""Validate the native lava samples against the recovered height/normal formula."""
import json,math,struct,sys
from pathlib import Path
folder=Path(__file__).resolve().parent
path=Path(sys.argv[1]) if len(sys.argv)>1 else folder/'native-lava.jsonl'
rows=[json.loads(s) for s in path.read_text().splitlines()]
samples=[r for r in rows if r['event']=='height_draw_submit']
summary=[r for r in rows if r['event']=='summary']
assert len(samples)==8 and len(summary)==1
assert len({r['frame'] for r in samples})==8
assert len({tuple(r['position']) for r in samples})==1
assert len({tuple(r['heights']) for r in samples})>1
assert len({tuple(r['normals']) for r in samples})>1
f32=lambda x:struct.unpack('<f',struct.pack('<f',x))[0]
pi=f32(math.pi);height_error=normal_error=unit_error=0
for r in samples:
 assert r['dimension']==2 and len(r['heights'])==9 and len(r['normals'])==27
 x,y,_=r['position'];tick=r['tick'];assert tick is not None
 def phase(tile,i):return (((32*i-64*int(tile)+tick)<<10)&0x1ffff)*pi/65536
 for j in range(3):
  for i in range(3):
   px,py=phase(x,i),phase(y,j)
   expected=-f32(.85)+.04*(math.sin(px)+math.sin(py))
   height_error=max(height_error,abs(expected-r['heights'][j*3+i]))
   nx,ny=math.cos(px),math.cos(py);length=math.sqrt(nx*nx+ny*ny+9)
   expected_normal=[nx/length,ny/length,-3/length]
   observed=r['normals'][(j*3+i)*3:(j*3+i+1)*3]
   normal_error=max(normal_error,*(abs(a-b) for a,b in zip(expected_normal,observed)))
   unit_error=max(unit_error,abs(sum(a*a for a in observed)-1))
assert height_error<1e-5,height_error
assert normal_error<1e-5,normal_error
result=dict(samples=len(samples),vertices_per_sample=9,frames=[r['frame'] for r in samples],ticks=[r['tick'] for r in samples],position=samples[0]['position'],max_height_error=height_error,max_normal_component_error=normal_error,max_normal_length_squared_error=unit_error,bump_config=summary[0]['bump_config'],renderer_flags=summary[0]['renderer_flags'])
print(json.dumps(result,indent=2))
