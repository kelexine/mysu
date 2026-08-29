# Author: kelexine <https://github.com/kelexine>
"""Unit tests for mysu_port.rules."""

from __future__ import annotations

from pathlib import Path

import pytest

from mysu_port.exceptions import RulesLoadError
from mysu_port.rules import apply_rules, load_rules


@pytest.fixture(autouse=True)
def _clear_rules_cache():
    load_rules.cache_clear()
    yield
    load_rules.cache_clear()


def test_path_rules_rewrite_ksu_working_dir():
    rules = load_rules()
    text = "SRC=/data/adb/ksu/bin/busybox\n"
    result, count = apply_rules(text, rules.path_rules)
    assert result == "SRC=/data/adb/mysu/bin/busybox\n"
    assert count == 1


def test_path_rules_rewrite_modules_dir():
    rules = load_rules()
    text = 'MODDIR="/data/adb/modules/$MODID"\n'
    result, count = apply_rules(text, rules.path_rules)
    assert result == 'MODDIR="/data/adb/mysu/modules/$MODID"\n'
    assert count == 1


def test_path_rules_do_not_mangle_ksud_prematurely():
    """/data/adb/ksud must become /data/adb/mysud, not /data/adb/mysu + 'd'."""
    rules = load_rules()
    text = "exec /data/adb/ksud --daemon\n"
    result, count = apply_rules(text, rules.path_rules)
    assert result == "exec /data/adb/mysud --daemon\n"
    assert count >= 1


def test_bare_ksud_identifier_rewritten():
    rules = load_rules()
    text = "pgrep ksud >/dev/null\n"
    result, _ = apply_rules(text, rules.path_rules)
    assert result == "pgrep mysud >/dev/null\n"


def test_webui_rules_rewrite_bridge_calls():
    rules = load_rules()
    js = 'if (typeof ksu !== "undefined") { ksu.toast("hi"); window.ksu.exec("ls"); }'
    result, count = apply_rules(js, rules.webui_rules)
    assert "mysu.toast" in result
    assert "window.mysu" in result
    assert "typeof mysu" in result
    assert count == 3


def test_webui_rules_do_not_touch_unrelated_words():
    rules = load_rules()
    js = "const ksudo = 1; // unrelated identifier, must not be touched"
    result, count = apply_rules(js, rules.webui_rules)
    assert result == js
    assert count == 0


def test_load_rules_missing_file_raises(tmp_path: Path):
    missing = tmp_path / "does_not_exist.json"
    with pytest.raises(RulesLoadError):
        load_rules(missing)


def test_load_rules_malformed_json_raises(tmp_path: Path):
    bad = tmp_path / "rules.json"
    bad.write_text("{not valid json", encoding="utf-8")
    with pytest.raises(RulesLoadError):
        load_rules(bad)
