# Author: kelexine <https://github.com/kelexine>
"""Unit tests for mysu_port.validation."""

from __future__ import annotations

from pathlib import Path

import pytest

from mysu_port.exceptions import InvalidModuleError
from mysu_port.rules import load_rules
from mysu_port.validation import validate_module


@pytest.fixture(autouse=True)
def _clear_rules_cache():
    load_rules.cache_clear()
    yield
    load_rules.cache_clear()


def _write_prop(work_dir: Path, text: str) -> None:
    (work_dir / "module.prop").write_text(text, encoding="utf-8")


def test_valid_module_prop_passes(tmp_path: Path):
    _write_prop(
        tmp_path,
        "id=example_module\nname=Example\nversion=v1.0\nversionCode=1\nauthor=kelexine\n",
    )
    fields = validate_module(tmp_path, load_rules())
    assert fields["id"] == "example_module"


def test_missing_module_prop_raises(tmp_path: Path):
    with pytest.raises(InvalidModuleError, match="no module.prop"):
        validate_module(tmp_path, load_rules())


def test_missing_required_field_raises(tmp_path: Path):
    _write_prop(tmp_path, "id=example_module\nname=Example\n")
    with pytest.raises(InvalidModuleError, match="version"):
        validate_module(tmp_path, load_rules())


def test_comments_and_blank_lines_ignored(tmp_path: Path):
    _write_prop(
        tmp_path,
        "# a comment\n\nid=example_module\nname=Example\nversion=v1.0\n"
        "versionCode=1\nauthor=kelexine\n",
    )
    fields = validate_module(tmp_path, load_rules())
    assert len(fields) == 5
