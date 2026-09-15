"""Source-level module porting orchestration for MySU.

Author: kelexine <https://github.com/kelexine>
Date: 2026-09-14
Purpose: Port root modules directly from their source repositories (WebUI, native daemons,
         scripts) to native MySU conventions and packages flashable archives.
"""

from __future__ import annotations

import hashlib
import json
import logging
import os
import re
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path

from . import archive
from .exceptions import ModulePortError
from .port import PortResult, inject_webui_polyfill, process_directory, update_all_checksums
from .rules import RuleSet, load_rules
from .validation import validate_module

logger = logging.getLogger("mysu_port")

MYSU_JS_PKG = "@mysu-org/mysu"
MYSU_JS_VERSION = "^3.0.2"


@dataclass(frozen=True, slots=True)
class SourcePortResult:
    """Summary of a completed source-level port."""

    output_zip: Path
    webui_built: bool
    native_built: bool
    files_modified: int
    substitutions: int
    module_id: str


def find_android_ndk() -> Path | None:
    """Attempt to locate the Android NDK root on the local system."""
    for env_var in ("ANDROID_NDK_ROOT", "ANDROID_NDK_HOME", "NDK_HOME"):
        val = os.environ.get(env_var)
        if val and Path(val).is_dir():
            return Path(val)

    sdk_root = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    search_dirs: list[Path] = []
    if sdk_root:
        search_dirs.append(Path(sdk_root) / "ndk")
    home = Path.home()
    search_dirs.extend([
        home / "Android" / "Sdk" / "ndk",
        home / "Android" / "sdk" / "ndk",
        Path("/opt/android-ndk"),
        Path("/opt/android-sdk/ndk"),
    ])

    for ndk_dir in search_dirs:
        if ndk_dir.is_dir():
            versions = sorted(ndk_dir.iterdir(), key=lambda p: p.stat().st_mtime, reverse=True)
            for ver in versions:
                if (ver / "ndk-build").is_file():
                    return ver

    ndk_build_path = shutil.which("ndk-build")
    if ndk_build_path:
        return Path(ndk_build_path).resolve().parent

    return None


def adapt_webui_source(webui_dir: Path, rule_set: RuleSet) -> tuple[int, int]:
    """Rewrite WebUI source files to replace legacy ksu imports and calls with @mysu-org/mysu."""
    files_modified = 0
    total_substitutions = 0

    pkg_json = webui_dir / "package.json"
    if pkg_json.is_file():
        try:
            data = json.loads(pkg_json.read_text(encoding="utf-8"))
            modified_pkg = False
            for dep_key in ("dependencies", "devDependencies"):
                deps = data.get(dep_key, {})
                for legacy_dep in ("kernelsu", "kernelsu-alt", "@kernelsu/core"):
                    if legacy_dep in deps:
                        del deps[legacy_dep]
                        deps[MYSU_JS_PKG] = MYSU_JS_VERSION
                        modified_pkg = True
            if modified_pkg:
                pkg_json.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
                files_modified += 1
                total_substitutions += 1
                logger.info("adapted %s: replaced legacy dependencies with %s", pkg_json, MYSU_JS_PKG)
        except (OSError, json.JSONDecodeError) as exc:
            logger.warning("failed to patch package.json in %s: %s", webui_dir, exc)

    source_extensions = {".js", ".ts", ".vue", ".jsx", ".tsx", ".mjs", ".cjs", ".css", ".html"}
    for path in webui_dir.rglob("*"):
        if not path.is_file() or path.suffix not in source_extensions:
            continue
        if any(part in ("node_modules", "dist", ".git", ".vite") for part in path.parts):
            continue

        try:
            content = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue

        orig = content
        sub_count = 0

        # Import statements matching kernelsu and kernelsu-alt
        new_content, c = re.subn(r'from\s+[\'"](?:kernelsu|kernelsu-alt)[\'"]', f'from "{MYSU_JS_PKG}"', content)
        content = new_content
        sub_count += c

        new_content, c = re.subn(r'import\s+[\'"](?:kernelsu|kernelsu-alt)[\'"]', f'import "{MYSU_JS_PKG}"', content)
        content = new_content
        sub_count += c

        # Global object and bridge method calls
        new_content, c = re.subn(r'\bwindow\.ksu\b', 'window.mysu', content)
        content = new_content
        sub_count += c

        new_content, c = re.subn(r'\bksu\.(exec|spawn|toast|fullScreen|moduleInfo)\b', r'mysu.\1', content)
        content = new_content
        sub_count += c

        # Icon URIs and virtual domains
        new_content, c = re.subn(r'ksu://', 'mysu://', content)
        content = new_content
        sub_count += c

        new_content, c = re.subn(r'https://mui\.kernelsu\.org/', 'https://mui.mysu.org/', content)
        content = new_content
        sub_count += c

        # Text branding
        new_content, c = re.subn(r'\bKernelSU\b', 'MySU', content)
        content = new_content
        sub_count += c

        if sub_count > 0:
            path.write_text(content, encoding="utf-8")
            files_modified += 1
            total_substitutions += sub_count
            logger.debug("adapted %s (%d substitution(s))", path, sub_count)

    return files_modified, total_substitutions


def build_webui(webui_dir: Path, out_webroot: Path) -> bool:
    """Build the WebUI assets using bun, pnpm, or npm, then copy output to out_webroot."""
    pm = None
    for tool in ("bun", "pnpm", "npm"):
        if shutil.which(tool):
            pm = tool
            break

    if not pm:
        logger.warning("no suitable package manager found (bun/pnpm/npm) for building WebUI")
        return False

    # Install dependencies if node_modules is missing
    if not (webui_dir / "node_modules").is_dir():
        install_cmd = [pm, "install"]
        logger.info("installing WebUI dependencies in %s via %s...", webui_dir, " ".join(install_cmd))
        ires = subprocess.run(install_cmd, cwd=webui_dir, capture_output=True, text=True)
        if ires.returncode != 0:
            logger.warning("package install encountered issues:\n%s\n%s", ires.stdout, ires.stderr)

    cmd = [pm, "run", "build"]
    logger.info("building WebUI in %s via %s...", webui_dir, " ".join(cmd))
    res = subprocess.run(cmd, cwd=webui_dir, capture_output=True, text=True)
    if res.returncode != 0:
        logger.error("WebUI build failed:\n%s\n%s", res.stdout, res.stderr)
        return False

    dist_dir = webui_dir / "dist"
    alt_webui = out_webroot.parent / "webui"

    if (out_webroot / "index.html").is_file():
        logger.info("WebUI assets built directly to %s", out_webroot)
        return True
    elif dist_dir.is_dir() and (dist_dir / "index.html").is_file():
        out_webroot.mkdir(parents=True, exist_ok=True)
        for item in dist_dir.iterdir():
            dest = out_webroot / item.name
            if item.is_dir():
                shutil.copytree(item, dest, dirs_exist_ok=True)
            else:
                shutil.copy2(item, dest)
        logger.info("deployed WebUI dist -> %s", out_webroot)
        return True
    elif alt_webui.is_dir() and (alt_webui / "index.html").is_file():
        out_webroot.mkdir(parents=True, exist_ok=True)
        for item in alt_webui.iterdir():
            dest = out_webroot / item.name
            if item.is_dir():
                shutil.copytree(item, dest, dirs_exist_ok=True)
            else:
                shutil.copy2(item, dest)
        logger.info("deployed WebUI from %s -> %s", alt_webui, out_webroot)
        return True
    else:
        logger.warning("WebUI build completed, but no index.html found in %s, %s, or %s", out_webroot, dist_dir, alt_webui)
        return False


def build_native_daemon(repo_root: Path, out_libs_dir: Path) -> bool:
    """Compile native C/C++ daemons using ndk-build if jni/ is present."""
    jni_dir = repo_root / "jni"
    if not jni_dir.is_dir():
        return False

    ndk_path = find_android_ndk()
    if not ndk_path:
        logger.warning("jni/ directory found but Android NDK is not detected; skipping native build")
        return False

    ndk_build = ndk_path / "ndk-build"
    if not ndk_build.is_file():
        ndk_build = ndk_path / "ndk-build.cmd"
    if not ndk_build.is_file():
        logger.warning("ndk-build binary not found in %s", ndk_path)
        return False

    logger.info("building native binaries in %s using NDK at %s...", repo_root, ndk_path)
    res = subprocess.run(
        [str(ndk_build), f"-j{os.cpu_count() or 4}"],
        cwd=repo_root,
        capture_output=True,
        text=True,
    )
    if res.returncode != 0:
        logger.error("ndk-build failed:\n%s\n%s", res.stdout, res.stderr)
        return False

    libs_src = repo_root / "libs"
    if libs_src.is_dir():
        out_libs_dir.mkdir(parents=True, exist_ok=True)
        shutil.copytree(libs_src, out_libs_dir, dirs_exist_ok=True)
        logger.info("copied built native libraries -> %s", out_libs_dir)
        return True

    return False


def adapt_module_scripts(module_dir: Path) -> tuple[int, int]:
    """Adapt module shell scripts to properly bind to MySU Manager and bin paths."""
    modified = 0
    subs = 0

    for script_name in ("customize.sh", "action.sh", "service.sh", "post-fs-data.sh", "uninstall.sh", "boot-completed.sh"):
        script_path = module_dir / script_name
        if not script_path.is_file():
            continue
        content = script_path.read_text(encoding="utf-8", errors="ignore")
        orig = content
        s = 0

        # Prepend /data/adb/mysu/bin to PATH if PATH starts with /data/adb/
        if "PATH=/data/adb/" in content and "/data/adb/mysu/bin" not in content:
            content, c = re.subn(r'PATH=/data/adb/', 'PATH=/data/adb/mysu/bin:/data/adb/', content)
            s += c

        # Add /data/adb/mysu/bin to manager_paths if present
        if 'manager_paths=' in content and '/data/adb/mysu/bin' not in content:
            content, c = re.subn(r'manager_paths="([^"]*)"', r'manager_paths="/data/adb/mysu/bin \1"', content)
            s += c

        # Adjust legacy minimum version thresholds (e.g. MIN_KERNELSU_VERSION=32000 -> 30000)
        content, c = re.subn(r'MIN_KERNELSU_VERSION=\d+', 'MIN_KERNELSU_VERSION=30000', content)
        s += c

        # Support MySU Manager in action.sh WebUI launch check
        if script_name == "action.sh" and "io.github.a13e300.ksuwebui" in content and "dev.kelexine.mysu" not in content:
            mysu_intent_check = (
                'pm path dev.kelexine.mysu > /dev/null 2>&1 && {\n'
                '\techo "- Launching WebUI in MySU Manager..."\n'
                '\tam start -n "dev.kelexine.mysu/.ui.webui.WebUIActivity" -e id "$ID"\n'
                '\texit 0\n'
                '}\n\t'
            )
            content, c = re.subn(r'(pm path io\.github\.a13e300\.ksuwebui)', mysu_intent_check + r'\1', content)
            s += c

        if s > 0:
            script_path.write_text(content, encoding="utf-8")
            modified += 1
            subs += s

    return modified, subs


def port_source_module(
    repo_path: Path,
    output_zip: Path | None = None,
    *,
    skip_build: bool = False,
) -> SourcePortResult:
    """Port a complete module source repository to native MySU conventions and package a flashable zip."""
    rule_set = load_rules()
    repo_path = repo_path.resolve()

    # Determine module packaging directory
    module_dir = repo_path / "module" if (repo_path / "module").is_dir() else repo_path
    prop_path = module_dir / "module.prop"
    if not prop_path.is_file():
        raise ModulePortError(f"No module.prop found in {module_dir}")

    total_files_modified = 0
    total_substitutions = 0

    # 1. Adapt and build WebUI if present
    webui_candidates = [repo_path / d for d in ("webui", "web", "frontend", "ui", "src/webui")]
    webui_dir = next((d for d in webui_candidates if (d / "package.json").is_file()), None)
    webui_built = False
    if webui_dir:
        mod_count, sub_count = adapt_webui_source(webui_dir, rule_set)
        total_files_modified += mod_count
        total_substitutions += sub_count
        if not skip_build:
            out_webroot = module_dir / "webroot"
            webui_built = build_webui(webui_dir, out_webroot)

    # Normalize webroot if built to webui
    if (module_dir / "webui" / "index.html").is_file() and not (module_dir / "webroot" / "index.html").is_file():
        (module_dir / "webroot").mkdir(parents=True, exist_ok=True)
        shutil.copytree(module_dir / "webui", module_dir / "webroot", dirs_exist_ok=True)
        logger.info("normalized %s -> %s", module_dir / "webui", module_dir / "webroot")

    # 2. Native C/C++ compilation
    native_built = False
    if not skip_build and (repo_path / "jni").is_dir():
        out_libs = module_dir / "libs"
        native_built = build_native_daemon(repo_path, out_libs)

    # 3. Adapt module shell scripts, config files, and properties
    files_mod, subs = process_directory(module_dir, rule_set)
    total_files_modified += files_mod
    total_substitutions += subs

    # 3b. Adapt specific module scripts for MySU Manager and bin paths
    script_mods, script_subs = adapt_module_scripts(module_dir)
    total_files_modified += script_mods
    total_substitutions += script_subs

    # 4. Inject fallback WebUI bridge polyfill if needed
    injected = inject_webui_polyfill(module_dir)
    total_files_modified += injected

    # 5. Synchronize .sha256 companion checksums
    checksums_updated = update_all_checksums(module_dir)
    if checksums_updated:
        logger.info("synchronized %d companion checksum file(s)", checksums_updated)

    # 6. Validate module
    fields = validate_module(module_dir, rule_set)
    module_id = fields.get("id", "unknown")

    # 7. Package flashable zip
    out_zip = output_zip or (repo_path / f"{module_id}-mysu.zip")
    archive.repack(module_dir, out_zip)
    logger.info("packaged flashable module -> %s", out_zip)

    return SourcePortResult(
        output_zip=out_zip,
        webui_built=webui_built,
        native_built=native_built,
        files_modified=total_files_modified,
        substitutions=total_substitutions,
        module_id=module_id,
    )
