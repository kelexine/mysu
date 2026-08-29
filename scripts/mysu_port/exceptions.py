"""Exception hierarchy for the mysu_port module-porting tool.

Author: kelexine <https://github.com/kelexine>
"""

from __future__ import annotations


class ModulePortError(Exception):
    """Base class for all mysu_port errors."""


class InvalidModuleError(ModulePortError):
    """Raised when the input does not look like a valid Magisk/MySU module.

    Typically raised for a missing/unparsable ``module.prop`` or a module
    missing required fields (see ``rules.json:required_module_prop_fields``).
    """


class UnsafeArchiveError(ModulePortError):
    """Raised when a zip archive contains an entry that would escape the
    extraction directory (path traversal / "zip-slip") or an unsupported
    entry type (e.g. an absolute-path member).
    """


class RulesLoadError(ModulePortError):
    """Raised when ``rules.json`` is missing, unreadable, or malformed."""
