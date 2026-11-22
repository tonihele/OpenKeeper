#!/usr/bin/env python3
"""Exercise native secret-door discovery against synthetic records; capture outside the repo."""
import argparse
import csv
import hashlib
from pathlib import Path
import subprocess

EXECUTABLE_SHA256 = '2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('executable', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    executable = args.executable.resolve()
    if hashlib.sha256(executable.read_bytes()).hexdigest() != EXECUTABLE_SHA256:
        parser.error('unsupported executable; these addresses are version-specific')
    source = Path(__file__).with_name('probe.c').resolve()
    repo = next(p for p in source.parents if (p / '.git').exists())
    output = args.output.resolve()
    if output.is_relative_to(repo):
        parser.error('captures must remain outside the repository')
    if output.exists():
        parser.error('choose a fresh output directory')
    output.mkdir(parents=True)
    binary = output / 'probe'
    subprocess.run(['gcc', '-m32', '-O2', '-Wall', '-Wextra', '-fno-pie', '-no-pie',
                    str(source), '-o', str(binary)], check=True)
    capture = output / 'discovery.csv'
    with capture.open('w') as stream:
        subprocess.run([str(binary), str(executable)], stdout=stream, check=True)
    with capture.open() as stream:
        rows = list(csv.DictReader(stream))
    if len(rows) != 4800:
        raise AssertionError(f'incomplete capture: {len(rows)} cases')
    print(f'Verified {len(rows)} native discovery cases; capture: {capture}')
    print('Original predicates and state initialization; controlled LOS and world-service callbacks, not a live game.')


if __name__ == '__main__':
    main()
