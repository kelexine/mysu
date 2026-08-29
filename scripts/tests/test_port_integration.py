# Author: kelexine <https://github.com/kelexine>
"""Integration tests for the end-to-end port_module flow."""

from __future__ import annotations

import os
import stat
import zipfile
from pathlib import Path

import pytest

from mysu_port.archive import safe_extract
from mysu_port.exceptions import InvalidModuleError, UnsafeArchiveError
from mysu_port.port import port_module
from mysu_port.rules import load_rules


@pytest.fixture(autouse=True)
def _clear_rules_cache():
    load_rules.cache_clear()
    yield
    load_rules.cache_clear()


def _build_legacy_module_zip(zip_path: Path) -> None:
    """Build a fixture zip resembling a real legacy KernelSU/Magisk module:
    a customize.sh referencing ksu paths, a webroot WebUI calling ksu.*, and
    a module.prop with all required fields.
    """
    with zipfile.ZipFile(zip_path, "w") as zf:
        zf.writestr(
            "module.prop",
            "id=legacy_mod\nname=Legacy Mod\nversion=v1\nversionCode=1\nauthor=someone\n",
        )
        customize = zipfile.ZipInfo("customize.sh")
        customize.external_attr = (0o755 & 0xFFFF) << 16
        zf.writestr(
            customize,
            "#!/system/bin/sh\ncp /data/adb/ksu/bin/busybox /data/adb/modules/legacy_mod/\n",
        )

        service = zipfile.ZipInfo("service.sh")
        service.external_attr = (0o755 & 0xFFFF) << 16
        zf.writestr(service, "#!/system/bin/sh\npgrep ksud\n")

        zf.writestr(
            "webroot/index.js",
            'window.onload = () => { if (typeof ksu !== "undefined") { ksu.toast("ready"); } };',
        )


def test_full_port_rewrites_paths_and_webui_and_preserves_exec_bit(tmp_path: Path):
    src_zip = tmp_path / "legacy_mod.zip"
    _build_legacy_module_zip(src_zip)

    result = port_module(src_zip, tmp_path / "ported.zip")

    assert result.output_path is not None
    assert result.module_id == "legacy_mod"
    assert result.files_modified >= 3

    check_dir = tmp_path / "check"
    safe_extract(result.output_path, check_dir)

    customize_text = (check_dir / "customize.sh").read_text()
    assert "/data/adb/mysu/bin/busybox" in customize_text
    assert "/data/adb/mysu/modules/legacy_mod/" in customize_text

    service_text = (check_dir / "service.sh").read_text()
    assert "mysud" in service_text and "ksud" not in service_text

    webui_text = (check_dir / "webroot" / "index.js").read_text()
    assert "mysu.toast" in webui_text
    assert "typeof mysu" in webui_text

    mode = stat.S_IMODE(os.stat(check_dir / "customize.sh").st_mode)
    assert mode & stat.S_IXUSR, (
        "customize.sh must stay executable through the full pipeline"
    )


def test_port_rejects_module_missing_module_prop(tmp_path: Path):
    zip_path = tmp_path / "no_prop.zip"
    with zipfile.ZipFile(zip_path, "w") as zf:
        zf.writestr("customize.sh", "#!/system/bin/sh\necho hi\n")

    with pytest.raises(InvalidModuleError):
        port_module(zip_path, tmp_path / "out.zip")


def test_port_skip_validation_bypasses_module_prop_check(tmp_path: Path):
    zip_path = tmp_path / "no_prop.zip"
    with zipfile.ZipFile(zip_path, "w") as zf:
        zf.writestr("customize.sh", "echo /data/adb/ksu/bin/busybox\n")

    result = port_module(zip_path, tmp_path / "out.zip", skip_validation=True)
    assert result.module_id == "<validation skipped>"


def test_port_rejects_zip_slip_archive(tmp_path: Path):
    zip_path = tmp_path / "evil.zip"
    with zipfile.ZipFile(zip_path, "w") as zf:
        zf.writestr(
            "module.prop", "id=x\nname=x\nversion=v1\nversionCode=1\nauthor=x\n"
        )
        zf.writestr("../../../../tmp/escape.sh", "echo pwned\n")

    with pytest.raises(UnsafeArchiveError):
        port_module(zip_path, tmp_path / "out.zip")


def test_port_in_place_directory(tmp_path: Path):
    mod_dir = tmp_path / "legacy_mod_dir"
    mod_dir.mkdir()
    (mod_dir / "module.prop").write_text(
        "id=legacy_dir\nname=X\nversion=v1\nversionCode=1\nauthor=x\n", encoding="utf-8"
    )
    (mod_dir / "customize.sh").write_text(
        "echo /data/adb/ksu/bin/busybox\n", encoding="utf-8"
    )

    result = port_module(mod_dir)
    assert result.output_path is None
    assert result.module_id == "legacy_dir"
    assert "/data/adb/mysu/bin/busybox" in (mod_dir / "customize.sh").read_text()
