"""Core module-porting orchestration.

Author: kelexine <https://github.com/kelexine>
"""

from __future__ import annotations

import logging
import tempfile
import zipfile
from dataclasses import dataclass
from pathlib import Path

from . import archive
from .exceptions import InvalidModuleError, ModulePortError
from .rules import RuleSet, apply_rules, load_rules
from .validation import validate_module

logger = logging.getLogger("mysu_port")


@dataclass(frozen=True, slots=True)
class PortResult:
    """Summary of a completed port, for CLI reporting and tests."""

    output_path: Path | None
    files_modified: int
    substitutions: int
    module_id: str


def _process_file(path: Path, rule_set: RuleSet) -> int:
    """Apply the appropriate rule set(s) to a single file in place.

    Returns the number of substitutions made (0 if the file isn't a rewrite
    target, or a target with no matches).
    """
    is_text_target = rule_set.matches_text_target(path)
    is_webui_target = rule_set.matches_webui_target(path)
    if not (is_text_target or is_webui_target):
        return 0

    try:
        content = path.read_text(encoding="utf-8", errors="ignore")
    except OSError as exc:
        logger.warning("skipping unreadable file %s: %s", path, exc)
        return 0

    total = 0
    if is_text_target:
        content, count = apply_rules(content, rule_set.path_rules)
        total += count
    if is_webui_target:
        content, count = apply_rules(content, rule_set.webui_rules)
        total += count

    if total:
        path.write_text(content, encoding="utf-8")
        logger.info(
            "adapted %s (%d substitution%s)", path, total, "" if total == 1 else "s"
        )
    return total


def process_directory(work_dir: Path, rule_set: RuleSet) -> tuple[int, int]:
    """Walk ``work_dir`` and adapt every matching file.

    Returns ``(files_modified, total_substitutions)``.
    """
    files_modified = 0
    total_substitutions = 0
    for path in work_dir.rglob("*"):
        if not path.is_file():
            continue
        count = _process_file(path, rule_set)
        if count:
            files_modified += 1
            total_substitutions += count
    return files_modified, total_substitutions


def port_module(
    input_path: Path,
    output_path: Path | None = None,
    *,
    skip_validation: bool = False,
) -> PortResult:
    """Port a legacy Magisk/KernelSU module (zip or directory) to MySU.

    Raises ``InvalidModuleError`` if the input has no valid ``module.prop``
    (unless ``skip_validation`` is set) and ``UnsafeArchiveError`` if a zip
    input contains a path-traversal attempt.
    """
    rule_set = load_rules()

    if input_path.is_file():
        if not zipfile.is_zipfile(input_path):
            raise InvalidModuleError(f"{input_path} is not a valid zip archive")
        with tempfile.TemporaryDirectory(prefix="mysu_port_") as tmp:
            work_dir = Path(tmp) / "module"
            archive.safe_extract(input_path, work_dir)
            module_id = _validate_or_skip(work_dir, rule_set, skip_validation)

            files_modified, substitutions = process_directory(work_dir, rule_set)

            out = output_path or input_path.with_name(f"{input_path.stem}_mysu.zip")
            archive.repack(work_dir, out)
            logger.info(
                "ported %s -> %s (%d file(s) modified)", input_path, out, files_modified
            )
            return PortResult(out, files_modified, substitutions, module_id)

    elif input_path.is_dir():
        module_id = _validate_or_skip(input_path, rule_set, skip_validation)
        files_modified, substitutions = process_directory(input_path, rule_set)
        logger.info(
            "ported %s in-place (%d file(s) modified)", input_path, files_modified
        )
        return PortResult(None, files_modified, substitutions, module_id)

    else:
        raise ModulePortError(f"{input_path} is neither a zip file nor a directory")


def _validate_or_skip(work_dir: Path, rule_set: RuleSet, skip_validation: bool) -> str:
    if skip_validation:
        return "<validation skipped>"
    fields = validate_module(work_dir, rule_set)
    return fields.get("id", "<unknown>")
