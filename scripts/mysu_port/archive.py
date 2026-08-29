"""Safe zip extraction and permission-preserving repackaging.

Module zips are untrusted, third-party input by design (this tool exists to
adapt *other people's* Magisk/KernelSU modules).

Author: kelexine <https://github.com/kelexine>
"""

from __future__ import annotations

import stat
import zipfile
from pathlib import Path

from .exceptions import UnsafeArchiveError

# High 16 bits of external_attr hold the standard unix st_mode; the shift is
# a long-standing zip format convention (also used by Info-ZIP itself).
_UNIX_MODE_SHIFT = 16


def _resolve_safe(dest_dir: Path, member_name: str) -> Path:
    """Resolve a zip member name against ``dest_dir``, rejecting escapes."""
    if member_name.startswith(("/", "\\")):
        raise UnsafeArchiveError(
            f"archive member has an absolute path: {member_name!r}"
        )

    candidate = (dest_dir / member_name).resolve()
    dest_resolved = dest_dir.resolve()
    if candidate != dest_resolved and dest_resolved not in candidate.parents:
        raise UnsafeArchiveError(
            f"archive member escapes extraction directory: {member_name!r}"
        )
    return candidate


def safe_extract(zip_path: Path, dest_dir: Path) -> None:
    """Extract ``zip_path`` into ``dest_dir``, rejecting any unsafe member.

    Raises ``UnsafeArchiveError`` (and leaves nothing extracted from that
    member onward) rather than silently skipping malicious entries — a
    module that needs traversal tricks to install is not one to adapt.
    """
    dest_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, "r") as zf:
        for info in zf.infolist():
            target = _resolve_safe(dest_dir, info.filename)
            if info.is_dir():
                target.mkdir(parents=True, exist_ok=True)
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            with zf.open(info) as src, open(target, "wb") as out:
                out.write(src.read())
            # Preserve the source mode when the zip recorded one (unix-made
            # archives store it in the upper 16 bits of external_attr).
            mode = info.external_attr >> _UNIX_MODE_SHIFT
            if mode:
                target.chmod(stat.S_IMODE(mode) or 0o644)


def repack(work_dir: Path, out_zip: Path) -> None:
    """Zip everything under ``work_dir`` into ``out_zip``, preserving modes."""
    out_zip.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out_zip, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for file_path in sorted(work_dir.rglob("*")):
            if not file_path.is_file():
                continue
            arcname = file_path.relative_to(work_dir).as_posix()
            info = zipfile.ZipInfo.from_file(file_path, arcname=arcname)
            info.compress_type = zipfile.ZIP_DEFLATED
            mode = file_path.stat().st_mode
            info.external_attr = (mode & 0xFFFF) << _UNIX_MODE_SHIFT
            with open(file_path, "rb") as src:
                zf.writestr(info, src.read())
