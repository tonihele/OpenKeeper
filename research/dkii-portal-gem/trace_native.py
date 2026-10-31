"""GDB read-only carrier effect observations; writes JSONL under /tmp."""
import gdb, json, struct, time
out = open('/tmp/dkii-gem-native.jsonl', 'w', buffering=1)
requests = 0
elements = 0
draw_count = 0
started = time.monotonic()

def reg(name): return int(gdb.parse_and_eval('$' + name))
def read(address, fmt):
    return struct.unpack(fmt, bytes(gdb.selected_inferior().read_memory(address, struct.calcsize(fmt))))
def emit(kind, **data): out.write(json.dumps(dict(kind=kind, wall=time.monotonic(), **data)) + '\n')

class Marker(gdb.Breakpoint):
    def stop(self):
        global requests
        requests += 1
        emit('marker', position=read(reg('esp') + 0x54, '<3i'))
        return False

class EffectUpdate(gdb.Breakpoint):
    def stop(self):
        obj = reg('ecx')
        if read(obj+0x105, '<H')[0] != 257: return False
        emit('effect_update', obj=hex(obj), steps=read(reg('esp')+8, '<I')[0],
             life=read(obj+0x10b, '<i')[0], generation_scale=read(0x69752c, '<I')[0])
        if requests >= 20 and elements >= 20 and draw_count >= 30:
            emit('complete', requests=requests, elements=elements, draws=draw_count)
            return True
        return False

class Element(gdb.Breakpoint):
    def stop(self):
        global elements
        obj = reg('esi')
        if read(obj+0x101, '<H')[0] != 444: return False
        elements += 1
        if elements <= 40:
            emit('element', obj=hex(obj), rgba=hex(read(obj+0x6e,'<I')[0]),
                 life=read(obj+0x109,'<i')[0], position=read(obj+0xd6,'<3i'),
                 velocity=read(obj+8,'<3i'), scale=read(obj+0x2a,'<2I'),
                 texture_frame=read(obj+0x5a,'<2I'))
        draw.enabled = True
        return False

class Draw(gdb.Breakpoint):
    def stop(self):
        global draw_count
        draw_count += 1
        if draw_count > 120:
            self.enabled = False
            return False
        try:
            iface=gdb.parse_and_eval('iface')
            offset=gdb.parse_and_eval('(long)&((struct d3d_device*)0)->IDirect3DDevice3_iface')
            dev=(iface.cast(gdb.lookup_type('char').pointer())-int(offset)).cast(gdb.lookup_type('struct d3d_device').pointer()).dereference()
            state=dev['stateblock_state'].dereference(); tex=state['textures'][0]
            rs=state['rs']; ts=state['texture_states'][0]
            fvf=int(gdb.parse_and_eval('fvf')); count=int(gdb.parse_and_eval('vertex_count'))
            data=dict(fvf=fvf, count=count, texture=hex(int(tex)), src=int(rs[19]), dst=int(rs[20]),
                      z=int(rs[7]), zwrite=int(rs[14]), color_op=int(ts[0]), alpha_op=int(ts[3]))
            if fvf == 0x144:
                address=int(gdb.parse_and_eval('vertices'))
                data['vertices']=[read(address+i*28,'<4fI2f') for i in range(min(count,8))]
            if int(tex):
                resource=tex.dereference()['resource']; sub=tex.dereference()['sub_resources'][0]
                data['width']=int(resource['width']); data['height']=int(resource['height'])
                data['format']=str(resource['format'].dereference()['id'])
                buf=sub['user_memory'] if int(sub['user_memory']) else resource['heap_memory']
                if int(buf) and int(sub['size']) < 1024*1024:
                    path='/tmp/dkii-gem-texture-'+hex(int(tex))+'.bin'
                    open(path,'wb').write(bytes(gdb.selected_inferior().read_memory(buf,int(sub['size']))))
                    data['pixels']=path
            emit('draw', **data)
        except Exception as exc: emit('draw_error', error=str(exc))
        return False

Marker('*0x5a426b')
EffectUpdate('*0x434e80')
Element('*0x4363c6')
draw=Draw('d3d_device3_DrawIndexedPrimitive'); draw.enabled=False
