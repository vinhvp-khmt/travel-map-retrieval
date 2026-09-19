#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
cd "$(dirname "$0")"
exec ./mvnw "$@"
