#!/usr/bin/env python3
"""Create an isolated native gem-carrier map; never overwrite original maps."""
import argparse
import struct
from pathlib import Path
ROOT = Path('/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2')
SOURCE = 'TrainMixed______'
NAME = 'GemCarrierProbe_'
SUFFIXES = ('.kwd', 'Globals.kld', 'Map.kld', 'Players.kld', 'Things.kld', 'Triggers.kld', 'Variables.kld')

def generate(output):
    maps = ROOT / 'Data/editor/maps'
    files = {s: (maps / (SOURCE+s)).read_bytes() for s in SUFFIXES}
    assert len(NAME) == len(SOURCE)
    files['.kwd'] = files['.kwd'].replace(SOURCE.encode(), NAME.encode()).replace(SOURCE.encode('utf-16le'), NAME.encode('utf-16le'))
    data = files['Things.kld']; off = 56; records = []
    while off < len(data):
        tag, size = struct.unpack_from('<II', data, off)
        record = data[off:off+8+size]; off += 8+size
        if tag == 204 and record[-2] == 14: records.append(record)
    assert len(records) == 1
    # A friendly, initially visible Lord runs the original CREATURE_CREATED ->
    # ATTACH_PORTAL_GEM action, with no injected game-memory writes.
    records.append(struct.pack('<IIiiiHBBiiHBB', 200, 28, 13, 40, 0, 0, 1, 0x09, 100, 0, 85, 29, 3))
    payload = b''.join(records); header = bytearray(data[:56])
    struct.pack_into('<I', header, 8, 56+len(payload)); struct.pack_into('<I', header, 20, len(records)); struct.pack_into('<I', header, 52, len(payload))
    files['Things.kld'] = bytes(header)+payload
    original = (maps/'level1Triggers.kld').read_bytes(); selected = {}; off = 60
    while off < len(original):
        tag, size = struct.unpack_from('<II', original, off)
        record = bytearray(original[off:off+8+size]); off += 8+size
        ident = struct.unpack_from('<H', record, 16)[0]
        if ident in (85, 65521):
            struct.pack_into('<H', record, 18, 0)
            selected[ident] = bytes(record)
    assert len(selected) == 2
    header = bytearray(original[:60]); payload = selected[85]+selected[65521]
    struct.pack_into('<I', header, 8, 60+len(payload)); struct.pack_into('<II', header, 20, 1, 1); struct.pack_into('<I', header, 56, 16)
    files['Triggers.kld'] = bytes(header)+payload
    output.mkdir(parents=True, exist_ok=True)
    for suffix in SUFFIXES:
        path = output/(NAME+suffix)
        if path.exists(): raise FileExistsError(path)
    for suffix, data in files.items(): (output/(NAME+suffix)).write_bytes(data)
    print('Created', NAME, 'Lord at (13,40), original create/attach triggers 85/65521')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--output', type=Path, default=Path('/tmp/dkii-gem-probe'))
    generate(parser.parse_args().output)
