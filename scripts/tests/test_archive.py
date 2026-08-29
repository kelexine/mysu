# Author: kelexine <https://github.com/kelexine>
"""Unit tests for mysu_port.archive."""

from __future__ import annotations

import os
import stat
import zipfile
from pathlib import Path

import pytest

from mysu_port.archive import repack, safe_extract
from mysu_port.exceptions import UnsafeArchiveError


def _make_zip_with_entry(
    zip_path: Path, entry_name: str, content: bytes = b"pwned"
) -> None:
    with zipfile.ZipFile(zip_path, "w") as zf:
        zf.writestr(entry_name, content)


def test_safe_extract_rejects_parent_traversal(tmp_path: Path):
    evil_zip = tmp_path / "evil.zip"
    _make_zip_with_entry(evil_zip, "../../../../tmp/escaped.txt")
    dest = tmp_path / "dest"
    with pytest.raises(UnsafeArchiveError):
        safe_extract(evil_zip, dest)


def test_safe_extract_rejects_absolute_path(tmp_path: Path):
    evil_zip = tmp_path / "evil_abs.zip"
    _make_zip_with_entry(evil_zip, "/etc/passwd")
    dest = tmp_path / "dest"
    with pytest.raises(UnsafeArchiveError):
        safe_extract(evil_zip, dest)


def test_safe_extract_allows_normal_nested_paths(tmp_path: Path):
    good_zip = tmp_path / "good.zip"
    _make_zip_with_entry(good_zip, "common/functions.sh", b"#!/system/bin/sh\n")
    dest = tmp_path / "dest"
    safe_extract(good_zip, dest)
    assert (dest / "common" / "functions.sh").read_bytes() == b"#!/system/bin/sh\n"


def test_repack_preserves_executable_bit(tmp_path: Path):
    work_dir = tmp_path / "module"
    work_dir.mkdir()
    script = work_dir / "customize.sh"
    script.write_text("#!/system/bin/sh\necho hi\n", encoding="utf-8")
    script.chmod(0o755)

    out_zip = tmp_path / "out.zip"
    repack(work_dir, out_zip)

    extract_dir = tmp_path / "roundtrip"
    safe_extract(out_zip, extract_dir)
    restored = extract_dir / "customize.sh"
    mode = stat.S_IMODE(os.stat(restored).st_mode)
    assert mode & stat.S_IXUSR, "customize.sh must remain executable after round-trip"
