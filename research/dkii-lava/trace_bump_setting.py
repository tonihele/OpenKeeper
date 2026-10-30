"""Read-only startup trace for the documented -enablebumpmapping switch."""
import gdb,json,struct,hashlib
from pathlib import Path
exe=Path('/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/DKII.exe')
assert hashlib.sha256(exe.read_bytes()).hexdigest()=='2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163'
out=open('/tmp/dkii-bump-setting.jsonl','w',buffering=1)
def u(p):return struct.unpack('<I',bytes(gdb.selected_inferior().read_memory(p,4)))[0]
def reg(n):return int(gdb.parse_and_eval('$'+n))
def emit(event,**d):out.write(json.dumps(dict(event=event,**d))+'\n')
class Point(gdb.Breakpoint):
 def __init__(self,a,label):self.label=label;super().__init__('*'+hex(a))
 def stop(self):
  sp=reg('esp');d=dict(bump_config=u(0x759b10),renderer_flags=hex(u(0x764bb8)))
  if self.label=='setting':d.update(caller=hex(u(sp)),requested=u(sp+4))
  if self.label=='backend_request':d.update(caller=hex(u(sp)),requested_flags=hex(u(sp+20)))
  if self.label=='capability':d.update(caps_byte=hex(u(sp+0xf8)&255))
  if self.label=='resources':d.update(resources={hex(i):hex(u(reg('esi')+i)) for i in (0x7e,0x82,0x86)})
  emit(self.label,**d)
  return self.label=='resources'
for a,l in [(0x566860,'setting'),(0x573ed0,'backend_request'),(0x573e5d,'capability'),(0x573eae,'capability_rejected'),(0x598614,'resources')]:Point(a,l)
