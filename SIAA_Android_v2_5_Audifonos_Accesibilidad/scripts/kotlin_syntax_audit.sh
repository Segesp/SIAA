#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KOTLIN_HOME="$(cd "$(dirname "$(command -v kotlinc)")/.." && pwd)"
COMPILER="$KOTLIN_HOME/lib/kotlin-compiler.jar"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
kotlinc "$ROOT/tools/kotlin_syntax_audit.kt" -cp "$COMPILER" -d "$TMP/parser.jar"
kotlin -cp "$TMP/parser.jar:$COMPILER" Kotlin_syntax_auditKt "$ROOT"
