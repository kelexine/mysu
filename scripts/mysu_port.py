#!/usr/bin/env python3
# Script: mysu_port.py
# Author: kelexine <https://github.com/kelexine>
# Date: 2026-08-18
# Purpose: Backward-compatible CLI shim for the mysu_port package.

from __future__ import annotations

import sys

from mysu_port.cli import main

if __name__ == "__main__":
    sys.exit(main())
