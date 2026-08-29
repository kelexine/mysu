"""`module.prop` validation for candidate Magisk/MySU modules.

Author: kelexine <https://github.com/kelexine>
"""

from __future__ import annotations

from pathlib import Path

from .exceptions import InvalidModuleError
from .rules import RuleSet


def _parse_module_prop(text: str) -> dict[str, str]:
    fields: dict[str, str] = {}
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        fields[key.strip()] = value.strip()
    return fields


def validate_module(work_dir: Path, rule_set: RuleSet) -> dict[str, str]:
    """Validate that `work_dir` contains a well-formed `module.prop`.

    Returns the parsed field map on success. Raises `InvalidModuleError`
    with a message naming every missing field otherwise - this tool ports
    real modules; it should refuse ambiguous or non-module input rather than
    silently "adapting" zero files.
    """
    prop_path = work_dir / "module.prop"
    if not prop_path.is_file():
        raise InvalidModuleError(
            f"{work_dir} has no module.prop - not a valid Magisk/MySU module"
        )

    fields = _parse_module_prop(prop_path.read_text(encoding="utf-8", errors="ignore"))
    missing = [f for f in rule_set.required_module_prop_fields if not fields.get(f)]
    if missing:
        raise InvalidModuleError(
            "module.prop is missing required field(s): " + ", ".join(missing)
        )
    return fields
