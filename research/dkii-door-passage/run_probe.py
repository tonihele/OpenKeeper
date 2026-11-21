#!/usr/bin/env python3
"""Run the original same-tile door-opening candidate block on synthetic records."""
import argparse
import csv
import hashlib
from pathlib import Path
import subprocess

EXECUTABLE_SHA256 = "2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("executable", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--kind", choices=("opening", "routing"), default="opening")
    args = parser.parse_args()
    executable = args.executable.resolve()
    if hashlib.sha256(executable.read_bytes()).hexdigest() != EXECUTABLE_SHA256:
        parser.error("unsupported executable: probe addresses are version-specific")
    source = Path(__file__).with_name("candidate_probe.c" if args.kind == "opening" else "route_probe.c")
    repo = next(p for p in source.parents if (p / ".git").exists())
    output = args.output.resolve()
    if output.is_relative_to(repo):
        parser.error("captures and executables must stay outside the repository")
    output.mkdir(parents=True, exist_ok=True)
    binary, capture = output / "probe", output / "results.csv"
    if binary.exists() or capture.exists():
        parser.error("choose a fresh output directory")
    subprocess.run(["gcc", "-m32", "-O2", "-Wall", "-Wextra", "-fno-pie", "-no-pie",
                    str(source), "-o", str(binary)], check=True)
    with capture.open("w") as out:
        subprocess.run([str(binary), str(executable)], stdout=out, check=True)
    with capture.open(newline="") as inp:
        rows = list(csv.DictReader(inp))
    expected_count = 48 if args.kind == "opening" else 32
    if len(rows) != expected_count:
        raise AssertionError(f"incomplete native matrix: {len(rows)} rows")
    print(f"Verified {len(rows)} native {args.kind} cases; no mismatches.")
    print("These are isolated predicates, not a full door tick or an in-game movement trace.")
    print(f"Capture: {capture}")


if __name__ == "__main__":
    main()
