#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "$0")" && pwd)"
game_exe="${1:-/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/DKII.exe}"
output_dir="${2:-/tmp/dkii-lava-stages}"
python3 - "$game_exe" <<'PY'
import hashlib,sys
actual=hashlib.sha256(open(sys.argv[1],'rb').read()).hexdigest()
if actual!='2a00df642ff7f9296f1e8d053329ba152f5dac07e357f2b39b6f1f1f1d77e163':
 raise SystemExit('Unsupported executable SHA-256: '+actual)
PY
mkdir -p "$output_dir"
gcc -m32 -O0 -fno-pie -no-pie -Wall -Wextra -o "$output_dir/replay_texture_stages" "$script_dir/replay_texture_stages.c" -lm
"$output_dir/replay_texture_stages" "$game_exe" > "$output_dir/stages.jsonl"
python3 "$script_dir/verify_stages.py" "$output_dir/stages.jsonl"
