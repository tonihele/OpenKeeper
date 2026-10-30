"""Read-only native lava trace; start the quiet LavaProbe map via Wine GDB."""
import gdb,json,struct,collections,hashlib
from pathlib import Path
exe=Path('/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/DKII.exe')
assert hashlib.sha256(exe.read_bytes()).hexdigest()=='2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163'
out=open('/tmp/dkii-lava-trace.jsonl','w',buffering=1)
counts=collections.Counter();frames=0;engine=0

def reg(n):return int(gdb.parse_and_eval('$'+n))
def mem(p,n):return bytes(gdb.selected_inferior().read_memory(p,n))
def u(p):return struct.unpack('<I',mem(p,4))[0]
def floats(p,n):return list(struct.unpack('<'+'f'*n,mem(p,n*4)))
def emit(event,**d):out.write(json.dumps(dict(event=event,**d))+'\n')
class Point(gdb.Breakpoint):
 def __init__(self,address,label):self.label=label;self.target=None;self.last_frame=-1;super().__init__('*'+hex(address))
 def stop(self):
  if self.label=='height_draw_submit':
   pos=tuple(floats(reg('ecx')+0x20,3))
   if self.target is None:self.target=pos
   if pos!=self.target or self.last_frame==frames:return False
   self.last_frame=frames
  counts[self.label]+=1
  if counts[self.label]>=8:self.enabled=False
  global engine
  sp=reg('esp');ecx=reg('ecx');data=dict(address=hex(self.location and int(self.location[1:],16)),this=hex(ecx),stack=[hex(u(sp+i*4)) for i in range(12)],frame=frames)
  if self.label=='engine_resources':engine=reg('esi');data['bump_config']=u(0x759b10);data['resources']={hex(i):hex(u(reg('esi')+i)) for i in range(0x72,0x9a,4)}
  if self.label=='bump_setting_write':data['value']=u(sp+4)
  if self.label=='backend_request':data['flags']=hex(u(sp+20))
  if self.label.startswith('height_draw'):
   p=ecx;data['tick']=u(engine+0xc9) if engine else None;data.update(dimension=u(p+8),position=floats(p+0x20,3),flags=hex(u(p+0x3c)),textures=[hex(u(p+i)) for i in (0x14,0x18,0x1c)])
   n=min((u(p+8)+1)**2,100)
   try:data.update(normals=floats(u(p+0xc),n*3),heights=floats(u(p+0x10),n))
   except gdb.MemoryError:pass
  emit(self.label,**data)
  return False
class Frame(gdb.Breakpoint):
 def stop(self):
  global frames
  frames+=1
  if counts['height_draw_submit']<8 and frames<5000:return False
  emit('summary',frames=frames,counts=dict(counts),bump_config=u(0x759b10),renderer_flags=hex(u(0x764bb8)))
  return True
for a,n in [(0x566860,'bump_setting_write'),(0x5a1ca0,'terrain_construct'),(0x586bc0,'terrain_patch_construct'),(0x5a3310,'other_surface'),(0x598614,'engine_resources'),(0x573ed0,'backend_request'),(0x5a2570,'surface_sine'),(0x5a2be0,'surface_lava'),(0x5a2f10,'surface_water'),(0x5873a0,'height_construct'),(0x5875d0,'height_draw_prepare'),(0x587da0,'height_draw_submit')]:Point(a,n)
Frame('*0x43c011')
emit('provenance',exe_sha256=hashlib.sha256(exe.read_bytes()).hexdigest())
