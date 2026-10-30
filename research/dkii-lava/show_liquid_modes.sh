#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "$0")" && pwd)"
repo_dir="$(cd -- "$script_dir/../.." && pwd)"
jdk_dir="${LIQUID_PROBE_JAVA_HOME:-/home/wietse/.jdks/ms-25.0.4}"
classes_dir="$(mktemp -d /tmp/openkeeper-liquid-comparison.XXXXXX)"
trap 'rm -rf -- "$classes_dir"' EXIT
cd "$repo_dir"
classpath='build/classes/java/main:build/install/OpenKeeper/lib/*'
"$jdk_dir/bin/javac" -cp "$classpath" -d "$classes_dir" "$script_dir/ShowLiquidModes.java"
"$jdk_dir/bin/java" --enable-native-access=ALL-UNNAMED -cp "$classes_dir:$classpath" ShowLiquidModes "$@"
