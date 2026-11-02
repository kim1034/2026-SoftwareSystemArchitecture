#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build api-manual
find src/main/java -name '*.java' > build/sources.txt
javadoc -Xdoclint:all,-missing -quiet -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -d api-manual -sourcepath src/main/java -subpackages timefit -windowtitle 'TimeFit API 0.4.0' -doctitle 'TimeFit Beta API 0.4.0' -notimestamp
