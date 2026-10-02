#!/usr/bin/env bash
# Verifies the .shader schema (ShaderFileSchema) and caret scanner (ShaderPathScanner)
# against every *.shader file in an O3DE engine tree.
#
# Both classes are deliberately IntelliJ-free, so this compiles them directly with a
# JDK (no Gradle, no network) together with the checker and sweeps every caret offset
# of every engine .shader file:
#   - every JSON key the engine actually uses must be known to the schema
#   - every enum/bool value the engine actually uses must be offered for that key
#
# Usage:
#   ./tools/verify-shader-schema.sh <o3de-engine-root>
#
# JAVAC/Java are taken from $JAVAC/$JAVA if set, else from $PATH, else from the
# bundled JetBrains JBR (override with JBR=/path/to/jbr).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENGINE="${1:?usage: $0 <o3de-engine-root>}"

JBR="${JBR:-$HOME/.local/share/JetBrains/Toolbox/apps/clion/jbr}"
JAVAC="${JAVAC:-$(command -v javac || true)}"
JAVA="${JAVA:-$(command -v java || true)}"
if [ -z "$JAVAC" ] && [ -x "$JBR/bin/javac" ]; then JAVAC="$JBR/bin/javac"; fi
if [ -z "$JAVA" ] && [ -x "$JBR/bin/java" ]; then JAVA="$JBR/bin/java"; fi
if [ -z "$JAVAC" ] || [ -z "$JAVA" ]; then
    echo "error: no javac/java found (set JAVAC/JAVA or JBR)" >&2
    exit 1
fi

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

"$JAVAC" -d "$OUT" \
    "$ROOT/src/main/java/com/azsl/completion/shader/ShaderFileSchema.java" \
    "$ROOT/src/main/java/com/azsl/completion/shader/ShaderPathScanner.java" \
    "$ROOT/tools/shader-schema-check/com/azsl/completion/shader/Check.java"

"$JAVA" -cp "$OUT" com.azsl.completion.shader.Check "$ENGINE"
