#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
output="core/build/offline-verification"
mkdir -p "$output"
find core/src/main/java core/src/test/java -name '*.java' -print > "$output/sources.txt"
javac --release 21 -d "$output" @"$output/sources.txt"
java -Xmx512m -cp "$output" dev.castleforge.core.CoreVerification
java -Xmx512m -cp "$output" dev.castleforge.core.TerracedVerification
java -Xmx512m -cp "$output" dev.castleforge.core.IdentityVerification
java -Xmx512m -cp "$output" dev.castleforge.core.ReferenceVerification

