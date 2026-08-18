#!/usr/bin/env bash
# Script: mysu-module-port.sh
# Author: kelexine <https://github.com/kelexine>
# Date: 2026-08-18
# Purpose: Programmatically port Magisk / MySU modules to native MySU modules (no symlinks)
# Usage: ./scripts/mysu-module-port.sh <input_module.zip|module_dir> [output_module.zip]

set -euo pipefail

usage() {
    echo "Usage: $0 <input_module.zip|module_dir> [output_module.zip]"
    echo ""
    echo "Adapts legacy Magisk / MySU modules to native MySU standards by:"
    echo "  - Rewriting /data/adb/mysu/ paths to /data/adb/mysu/"
    echo "  - Rewriting /data/adb/modules/ paths to /data/adb/mysu/modules/"
    echo "  - Rewriting mysud invocations to mysud"
    echo "  - Preserving environment variables and binary compatibility"
    exit 1
}

if [ $# -lt 1 ]; then
    usage
fi

INPUT_TARGET="$1"
OUTPUT_TARGET="${2:-}"

TEMP_DIR=$(mktemp -d "/tmp/mysu_port_XXXXXX")
cleanup() {
    rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

echo "[+] MySU Module Porter initialized"

if [ -f "$INPUT_TARGET" ]; then
    echo "[+] Extracting module zip: $INPUT_TARGET"
    unzip -q "$INPUT_TARGET" -d "$TEMP_DIR/module"
    WORK_DIR="$TEMP_DIR/module"
    IS_ZIP=1
elif [ -d "$INPUT_TARGET" ]; then
    echo "[+] Working on module directory: $INPUT_TARGET"
    WORK_DIR="$INPUT_TARGET"
    IS_ZIP=0
else
    echo "[-] Error: Target $INPUT_TARGET does not exist!"
    exit 1
fi

adapt_file() {
    local FILE="$1"
    [ -f "$FILE" ] || return 0
    echo "  -> Adapting $(basename "$FILE")..."
    sed -i 's|/data/adb/ksu/|/data/adb/mysu/|g' "$FILE" 2>/dev/null || true
    sed -i 's|/data/adb/ksu\b|/data/adb/mysu|g' "$FILE" 2>/dev/null || true
    sed -i 's|/data/adb/modules/|/data/adb/mysu/modules/|g' "$FILE" 2>/dev/null || true
    sed -i 's|/data/adb/modules\b|/data/adb/mysu/modules|g' "$FILE" 2>/dev/null || true
    sed -i 's|/data/adb/ksud|/data/adb/mysud|g' "$FILE" 2>/dev/null || true
    sed -i 's|\bksud\b|mysud|g' "$FILE" 2>/dev/null || true
}

echo "[+] Scanning and transforming module scripts..."
find "$WORK_DIR" -type f \( -name "*.sh" -o -name "*.prop" -o -name "*.rc" \) | while read -r script_file; do
    adapt_file "$script_file"
done

if [ "$IS_ZIP" -eq 1 ]; then
    if [ -z "$OUTPUT_TARGET" ]; then
        BASENAME=$(basename "$INPUT_TARGET" .zip)
        OUTPUT_TARGET="${BASENAME}_mysu.zip"
    fi
    echo "[+] Packaging into $OUTPUT_TARGET..."
    (cd "$WORK_DIR" && zip -r -q "$OUTPUT_TARGET" .)
    mv "$WORK_DIR/$OUTPUT_TARGET" "$OUTPUT_TARGET"
    echo "[+] Module successfully ported and saved to: $OUTPUT_TARGET"
else
    echo "[+] Directory successfully ported in-place: $WORK_DIR"
fi
