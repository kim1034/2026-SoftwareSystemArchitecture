#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/classes build/test-classes dist
find src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 --release 17 -d build/classes @build/sources.txt
jar --create --file dist/ssd_ip_cp4_2026f_224569_app.jar --main-class timefit.App -C build/classes .
printf 'Built dist/ssd_ip_cp4_2026f_224569_app.jar\n'
