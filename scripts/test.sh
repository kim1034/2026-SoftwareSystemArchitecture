#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/build.sh
find src/test/java -name '*.java' > build/tests.txt
javac -encoding UTF-8 --release 17 -cp build/classes -d build/test-classes @build/tests.txt
java -ea -cp build/classes:build/test-classes timefit.CoreTests
