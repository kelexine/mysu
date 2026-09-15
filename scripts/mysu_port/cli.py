"""CLI entrypoint for mysu_port.

Author: kelexine <https://github.com/kelexine>
"""

from __future__ import annotations

import argparse
import logging
import sys
from pathlib import Path

from .exceptions import ModulePortError
from .port import port_module
from .source import port_source_module

logger = logging.getLogger("mysu_port")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="mysu_port",
        description="Adapt a legacy Magisk/KernelSU module (zip or directory) or build from source "
        "repository to native MySU conventions: rewrites /data/adb/ksu paths, module paths, the "
        "ksud binary name, and WebUI ksu.* bridge calls to their mysu equivalents.",
    )
    parser.add_argument("input", type=Path, help="Input module zip, directory, or source repository")
    parser.add_argument(
        "output", type=Path, nargs="?", default=None, help="Output zip (zip input or source mode)"
    )
    parser.add_argument(
        "--source",
        "-s",
        action="store_true",
        help="Port directly from source repository (adapts WebUI source, builds daemons, packages zip)",
    )
    parser.add_argument(
        "--skip-build",
        action="store_true",
        help="Skip WebUI/native compilation in source mode (adapt code and package only)",
    )
    parser.add_argument(
        "--upload-org",
        metavar="ORG",
        help="Push ported source repo to GitHub organization (e.g. MySU-org)",
    )
    parser.add_argument(
        "--register-catalog",
        type=Path,
        metavar="CATALOG_DIR",
        help="Register output zip in MySU module catalog repository directory",
    )
    parser.add_argument(
        "--verbose", "-v", action="store_true", help="Enable debug logging"
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="[%(levelname)s] %(message)s",
    )

    try:
        if args.source:
            res = port_source_module(
                args.input, args.output, skip_build=args.skip_build
            )
            logger.info(
                "done: source module %s -> %s (webui: %s, native: %s, %d substitution(s))",
                res.module_id,
                res.output_zip,
                "built" if res.webui_built else "skipped",
                "built" if res.native_built else "skipped",
                res.substitutions,
            )

            if args.upload_org:
                import subprocess
                repo_path = args.input.resolve()
                org = args.upload_org
                repo_name = res.module_id
                target_url = f"https://github.com/{org}/{repo_name}"
                logger.info("uploading source repository to %s...", target_url)
                # Ensure git repo initialized and remote configured
                subprocess.run(["git", "init", "-b", "main"], cwd=repo_path, check=False)
                subprocess.run(["git", "config", "user.name", "kelexine"], cwd=repo_path, check=False)
                subprocess.run(["git", "config", "user.email", "frankiekelechi@gmail.com"], cwd=repo_path, check=False)
                subprocess.run(["git", "add", "-A"], cwd=repo_path, check=False)
                subprocess.run(["git", "commit", "-m", f"feat(mysu): natively port {repo_name} to MySU ecosystem\n\nSigned-off-by: kelexine <frankiekelechi@gmail.com>"], cwd=repo_path, check=False)
                # Create remote repo on GitHub Org if needed
                subprocess.run(["gh", "repo", "create", f"{org}/{repo_name}", "--public", "--confirm"], check=False)
                subprocess.run(["git", "remote", "remove", "origin"], cwd=repo_path, check=False)
                subprocess.run(["git", "remote", "add", "origin", f"git@github.com:{org}/{repo_name}.git"], cwd=repo_path, check=False)
                subprocess.run(["git", "push", "-u", "origin", "main", "--force"], cwd=repo_path, check=False)
                logger.info("source repository pushed -> %s", target_url)

            if args.register_catalog:
                import subprocess
                catalog_script = args.register_catalog / "scripts" / "add_module.py"
                if catalog_script.is_file():
                    logger.info("registering module %s in catalog %s...", res.module_id, args.register_catalog)
                    subprocess.run([
                        sys.executable,
                        str(catalog_script),
                        "--id", res.module_id,
                        "--name", res.module_id.replace("_", " ").title(),
                        "--summary", f"{res.module_id.replace('_', ' ').title()} natively ported for MySU",
                        "--version", "1.0.0-mysu",
                        "--version-code", "1000",
                        "--zip", str(res.output_zip),
                        "--gh-release",
                    ], cwd=args.register_catalog, check=False)

            return 0

        result = port_module(
            args.input, args.output, skip_validation=args.skip_validation
        )
    except ModulePortError as exc:
        logger.error("%s", exc)
        return 1

    if result.output_path is not None:
        logger.info(
            "done: %s -> %s (%d substitution(s) across %d file(s))",
            result.module_id,
            result.output_path,
            result.substitutions,
            result.files_modified,
        )
    else:
        logger.info(
            "done: %s ported in-place (%d substitution(s) across %d file(s))",
            result.module_id,
            result.substitutions,
            result.files_modified,
        )
    return 0


if __name__ == "__main__":
    sys.exit(main())
