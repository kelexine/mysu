"""Run mysu_port as a module: python -m mysu_port."""

from __future__ import annotations

import sys
from .cli import main

if __name__ == "__main__":
    sys.exit(main())
