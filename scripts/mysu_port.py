#!/usr/bin/env python3
# Script: mysu_port.py
# Author: kelexine <https://github.com/kelexine>
# Date: 2026-08-18
# Purpose: Programmatic porting and adaptation utility for MySU modules
# Usage: python3 scripts/mysu_port.py <input_module> [output_zip]

from __future__ import annotations

import argparse
import os
import re
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

REPLACEMENTS = [
    (re.compile(r"/data/adb/ksu/"), "/data/adb/mysu/"),
    (re.compile(r"/data/adb/ksu\b"), "/data/adb/mysu"),
    (re.compile(r"/data/adb/modules/"), "/data/adb/mysu/modules/"),
    (re.compile(r"/data/adb/modules\b"), "/data/adb/mysu/modules"),
    (re.compile(r"/data/adb/ksud"), "/data/adb/mysud"),
    (re.compile(r"\bksud\b"), "mysud"),
]

TARGET_EXTENSIONS = {".sh", ".prop", ".rc", ".txt", ".json"}


def adapt_text(content: str) -> tuple[str, int]:
    count = 0
    result = content
    for pattern, replacement in REPLACEMENTS:
        new_result, n = pattern.subn(replacement, result)
        result = new_result
        count += n
    return result, count


def process_directory(directory: Path) -> int:
    total_modifications = 0
    for path in directory.rglob("*"):
        if path.is_file() and (path.suffix in TARGET_EXTENSIONS or path.name in {"customize.sh", "post-fs-data.sh", "service.sh", "action.sh", "module.prop"}):
            try:
                content = path.read_text(encoding="utf-8", errors="ignore")
                adapted, mods = adapt_text(content)
                if mods > 0:
                    path.write_text(adapted, encoding="utf-8")
                    print(f"  [+] Adapted {path.relative_to(directory)} ({mods} substitutions)")
                    total_modifications += mods
            except Exception as e:
                print(f"  [-] Failed processing {path}: {e}", file=sys.stderr)
    return total_modifications


def port_module(input_path: Path, output_path: Path | None) -> None:
    if input_path.is_file() and zipfile.is_zipfile(input_path):
        with tempfile.TemporaryDirectory() as tmpdir:
            work_dir = Path(tmpdir)
            with zipfile.ZipFile(input_path, "r") as zip_ref:
                zip_ref.extractall(work_dir)

            print(f"[+] Adapting module archive: {input_path.name}")
            mods = process_directory(work_dir)

            out = output_path or input_path.with_stem(f"{input_path.stem}_mysu")
            with zipfile.ZipFile(out, "w", compression=zipfile.ZIP_DEFLATED) as zip_out:
                for file_path in work_dir.rglob("*"):
                    if file_path.is_file():
                        zip_out.write(file_path, arcname=file_path.relative_to(work_dir))
            print(f"[+] Finished: Ported {mods} references. Output saved to {out}")

    elif input_path.is_dir():
        print(f"[+] Adapting module directory in-place: {input_path}")
        mods = process_directory(input_path)
        print(f"[+] Finished: Ported {mods} references in-place.")
    else:
        print(f"[-] Error: {input_path} is neither a valid zip nor a directory", file=sys.stderr)
        sys.exit(1)


def main() -> None:
    parser = argparse.ArgumentParser(description="Port Magisk/MySU modules to MySU")
    parser.add_argument("input", type=Path, help="Input module ZIP or directory")
    parser.add_argument("output", type=Path, nargs="?", default=None, help="Output module ZIP (optional)")
    args = parser.parse_args()

    port_module(args.input, args.output)


if __name__ == "__main__":
    main()
