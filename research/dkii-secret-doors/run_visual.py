#!/usr/bin/env python3
"""Render actual, user-provided door assets through production visibility updates."""
import argparse
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('reference', type=Path, help='Original Dungeon Keeper 2 installation')
parser.add_argument('converted', type=Path, help='Existing assets/Converted directory')
parser.add_argument('--output', type=Path, default=Path('/tmp/openkeeper-secret-door-visual'))
args = parser.parse_args()
repo = Path(__file__).resolve().parents[2]
libs = repo / 'build/install/OpenKeeper/lib'
if not (libs / 'OpenKeeper.jar').exists():
    parser.error('Build the application first with ./gradlew installDist')
java = Path(os.environ['JAVA_HOME']) / 'bin' if 'JAVA_HOME' in os.environ else None
out = args.output.resolve()
out.mkdir(parents=True, exist_ok=True)
classpath = str(libs / '*')
subprocess.run([str(java / 'javac') if java else 'javac', '-cp', classpath, '-d', str(out),
                str(Path(__file__).with_name('ShowSecretDoors.java'))], check=True)
# Canonical asset keys use the project's assets/Converted relative to user.dir.
subprocess.run([str(java / 'java') if java else 'java', '-cp', str(out) + os.pathsep + classpath,
                'ShowSecretDoors', str(args.reference.resolve()), str(args.converted.resolve()),
                str(out / 'frames')], cwd=args.converted.resolve().parent.parent, check=True)
