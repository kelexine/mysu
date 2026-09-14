"""Unit tests for mysu_port.source module.

Author: kelexine <https://github.com/kelexine>
Date: 2026-09-14
"""

from __future__ import annotations

import json
import zipfile
from pathlib import Path
from unittest.mock import patch

import pytest

from mysu_port.rules import load_rules
from mysu_port.source import (
    MYSU_JS_PKG,
    adapt_webui_source,
    find_android_ndk,
    port_source_module,
)


def test_find_android_ndk_from_env(tmp_path: Path):
    mock_ndk = tmp_path / "ndk" / "29.0.0"
    mock_ndk.mkdir(parents=True)
    with patch.dict("os.environ", {"ANDROID_NDK_ROOT": str(mock_ndk)}):
        found = find_android_ndk()
        assert found == mock_ndk


def test_adapt_webui_source(tmp_path: Path):
    webui_dir = tmp_path / "webui"
    webui_dir.mkdir()

    pkg_json = webui_dir / "package.json"
    pkg_json.write_text(
        json.dumps({
            "name": "test-module-ui",
            "dependencies": {"kernelsu": "^1.0.0", "vue": "^3.0.0"},
            "devDependencies": {"vite": "^5.0.0"}
        }),
        encoding="utf-8",
    )

    src_js = webui_dir / "App.vue"
    src_js.write_text(
        """<script>
import { exec } from "kernelsu";
if (typeof window.ksu !== "undefined") {
    console.log("running on ksu");
}
const icon = "ksu://icon/com.example.app";
</script>
<style>
@import url('https://mui.kernelsu.org/internal/insets.css');
</style>
""",
        encoding="utf-8",
    )

    rule_set = load_rules()
    files_mod, subs = adapt_webui_source(webui_dir, rule_set)

    assert files_mod >= 2
    assert subs >= 4

    # Verify package.json
    data = json.loads(pkg_json.read_text(encoding="utf-8"))
    assert "kernelsu" not in data["dependencies"]
    assert MYSU_JS_PKG in data["dependencies"]

    # Verify source file
    updated_content = src_js.read_text(encoding="utf-8")
    assert f'from "{MYSU_JS_PKG}"' in updated_content
    assert "kernelsu" not in updated_content
    assert "mysu://icon/" in updated_content
    assert "https://mui.mysu.org/internal/insets.css" in updated_content


def test_port_source_module(tmp_path: Path):
    repo_dir = tmp_path / "repo"
    module_dir = repo_dir / "module"
    module_dir.mkdir(parents=True)

    prop = module_dir / "module.prop"
    prop.write_text(
        "id=source_test\nname=Source Test\nversion=1.0.0\nversionCode=1\nauthor=kelexine\ndescription=test\n",
        encoding="utf-8",
    )

    cust = module_dir / "customize.sh"
    cust.write_text(
        'if [ "$KSU" = "true" ]; then\n  BIN="/data/adb/ksu/bin"\nfi\n',
        encoding="utf-8",
    )

    webui_dir = repo_dir / "webui"
    webui_dir.mkdir()
    (webui_dir / "package.json").write_text(
        json.dumps({"name": "ui", "dependencies": {"kernelsu": "1.0.0"}}),
        encoding="utf-8",
    )
    (webui_dir / "main.js").write_text('import ksu from "kernelsu";\n', encoding="utf-8")

    out_zip = tmp_path / "source_test_mysu.zip"
    res = port_source_module(repo_dir, out_zip, skip_build=True)

    assert res.output_zip == out_zip
    assert res.module_id == "source_test"
    assert out_zip.is_file()

    with zipfile.ZipFile(out_zip) as zf:
        namelist = zf.namelist()
        assert "module.prop" in namelist
        assert "customize.sh" in namelist

        cust_content = zf.read("customize.sh").decode("utf-8")
        assert "$MYSU" in cust_content
        assert "/data/adb/mysu/bin" in cust_content
