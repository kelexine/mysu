"""mysu_port — adapts legacy Magisk/KernelSU modules to native MySU conventions.

Author: kelexine <https://github.com/kelexine>
"""

from __future__ import annotations

from .exceptions import (
    InvalidModuleError,
    ModulePortError,
    RulesLoadError,
    UnsafeArchiveError,
)
from .port import PortResult, port_module

__all__ = [
    "InvalidModuleError",
    "ModulePortError",
    "PortResult",
    "RulesLoadError",
    "UnsafeArchiveError",
    "port_module",
]

__version__ = "2.0.0"
