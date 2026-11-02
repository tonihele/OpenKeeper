#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "$0")" && pwd)"
repo_dir="$(cd -- "$script_dir/../.." && pwd)"
showcase_dir="${TMPDIR:-/tmp}/openkeeper-room-showcase"
mkdir -p "$showcase_dir"
export JAVA_HOME="${JAVA_HOME:-/home/wietse/.jdks/ms-25.0.4}"
cat > "$showcase_dir/classpath.gradle" <<'GRADLE'
allprojects { p ->
    if (p == rootProject) {
        tasks.register('roomShowcaseClasspath') {
            doLast {
                new File(System.getProperty('roomShowcaseClasspath')).text = p.sourceSets.main.runtimeClasspath.asPath
            }
        }
    }
}
GRADLE
cd "$repo_dir"
./gradlew classes roomShowcaseClasspath --offline --no-configuration-cache \
    -I "$showcase_dir/classpath.gradle" -DroomShowcaseClasspath="$showcase_dir/classpath.txt"
showcase_classpath="$(cat "$showcase_dir/classpath.txt")"
"$JAVA_HOME/bin/javac" -cp "$showcase_classpath" -d "$showcase_dir" "$script_dir/ShowRoomOrder.java"
exec "$JAVA_HOME/bin/java" -cp "$showcase_dir:$showcase_classpath" ShowRoomOrder "$@"
