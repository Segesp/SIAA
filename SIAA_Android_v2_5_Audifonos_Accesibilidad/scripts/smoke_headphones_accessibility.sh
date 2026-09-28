#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KOTLIN_HOME="$(cd "$(dirname "$(command -v kotlinc)")/.." && pwd)"
CP="$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
mapfile -t SRC < <(find "$ROOT/core/model/src/main/kotlin" "$ROOT/core/algorithm/src/main/kotlin" "$ROOT/core/runtime/src/main/kotlin" -name '*.kt' | sort)
kotlinc "${SRC[@]}" "$ROOT/tools/runtime_regression_smoke.kt" "$ROOT/tools/headphones_accessibility_smoke.kt" -cp "$CP" -d "$TMP/tests.jar"
kotlin -cp "$TMP/tests.jar:$CP" Headphones_accessibility_smokeKt
