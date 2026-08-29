#!/usr/bin/env bash
# Script: mysu-module-port.sh
# Author: kelexine <https://github.com/kelexine>
# Date: 2026-08-18
# Purpose: Shell entry point for porting Magisk/KernelSU modules to native MySU.
# Usage: ./scripts/mysu-module-port.sh <input_module.zip|module_dir> [output_module.zip] [--skip-validation] [-v]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    echo "Usage: $0 <input_module.zip|module_dir> [output_module.zip] [--skip-validation] [-v]"
    echo ""
    echo "Adapts legacy Magisk / KernelSU modules to native MySU standards by:"
    echo "  - Rewriting /data/adb/ksu/ paths to /data/adb/mysu/"
    echo "  - Rewriting /data/adb/modules/ paths to /data/adb/mysu/modules/"
    echo "  - Rewriting ksud invocations to mysud"
    echo "  - Rewriting WebUI ksu.* bridge calls (JS/HTML) to mysu.*"
    echo "  - Validating module.prop and preserving script executable bits"
    exit 1
}

if [ $# -lt 1 ]; then
    usage
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "[-] Error: python3 is required but was not found on PATH" >&2
    exit 1
fi

exec python3 "${SCRIPT_DIR}/mysu_port.py" "$@"
