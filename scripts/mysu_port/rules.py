"""Rewrite-rule loading and application for mysu_port.

Rules live in `rules.json` next to this file - the single source of truth
consumed by both the Python CLI and (indirectly, via the bash shim in
`scripts/mysu-module-port.sh`) any shell-based caller. Do not hand-duplicate
regex rules elsewhere; add new rules to `rules.json` instead.

Author: kelexine <https://github.com/kelexine>
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from re import Pattern

from .exceptions import RulesLoadError

_RULES_PATH = Path(__file__).with_name("rules.json")


@dataclass(frozen=True, slots=True)
class CompiledRule:
    """A single compiled find/replace rule."""

    pattern: Pattern[str]
    replacement: str
    note: str = ""


@dataclass(frozen=True, slots=True)
class RuleSet:
    """All rules and file-matching metadata loaded from ``rules.json``."""

    path_rules: tuple[CompiledRule, ...]
    webui_rules: tuple[CompiledRule, ...]
    text_extensions: frozenset[str]
    webui_extensions: frozenset[str]
    explicit_filenames: frozenset[str]
    required_module_prop_fields: tuple[str, ...]

    def matches_text_target(self, path: Path) -> bool:
        """True if ``path`` should receive path-rewrite rules."""
        return (
            path.suffix.lower() in self.text_extensions
            or path.name in self.explicit_filenames
        )

    def matches_webui_target(self, path: Path) -> bool:
        """True if ``path`` should additionally receive WebUI bridge rules."""
        suffix = path.suffix.lower()
        if suffix in self.webui_extensions:
            return True
        if suffix == ".json" and ("webroot" in path.parts or path.name == "config.json"):
            return True
        return False


def _compile_rules(raw: list[dict[str, str]]) -> tuple[CompiledRule, ...]:
    compiled = []
    for entry in raw:
        try:
            compiled.append(
                CompiledRule(
                    pattern=re.compile(entry["pattern"]),
                    replacement=entry["replacement"],
                    note=entry.get("note", ""),
                )
            )
        except re.error as exc:
            raise RulesLoadError(
                f"invalid regex in rules.json: {entry.get('pattern')!r}: {exc}"
            ) from exc
        except KeyError as exc:
            raise RulesLoadError(f"rule entry missing required key: {exc}") from exc
    return tuple(compiled)


@lru_cache(maxsize=1)
def load_rules(rules_path: Path | None = None) -> RuleSet:
    """Load and compile the rule set from ``rules.json``.

    Cached — call ``load_rules.cache_clear()`` in tests that swap the file.
    """
    path = rules_path or _RULES_PATH
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise RulesLoadError(f"rules file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise RulesLoadError(f"rules file is not valid JSON: {path}: {exc}") from exc

    try:
        return RuleSet(
            path_rules=_compile_rules(raw["path_rules"]),
            webui_rules=_compile_rules(raw["webui_rules"]),
            text_extensions=frozenset(e.lower() for e in raw["text_extensions"]),
            webui_extensions=frozenset(e.lower() for e in raw["webui_extensions"]),
            explicit_filenames=frozenset(raw["explicit_filenames"]),
            required_module_prop_fields=tuple(raw["required_module_prop_fields"]),
        )
    except KeyError as exc:
        raise RulesLoadError(f"rules.json missing required key: {exc}") from exc


def apply_rules(content: str, rule_set: tuple[CompiledRule, ...]) -> tuple[str, int]:
    """Apply every rule in ``rule_set`` to ``content`` in order.

    Returns the transformed content and the total number of substitutions
    made across all rules (informational only — a rule matching its own
    output on a second pass, e.g. re-porting an already-ported module, still
    counts as a substitution even though the text is unchanged).
    """
    result = content
    total = 0
    for rule in rule_set:
        result, count = rule.pattern.subn(rule.replacement, result)
        total += count
    return result, total
