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

logger = logging.getLogger("mysu_port")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="mysu_port",
        description="Adapt a legacy Magisk/KernelSU module (zip or directory) to native MySU "
        "conventions: rewrites /data/adb/ksu paths, module paths, the ksud binary name, and "
        "WebUI ksu.* bridge calls to their mysu equivalents.",
    )
    parser.add_argument("input", type=Path, help="Input module zip or directory")
    parser.add_argument(
        "output", type=Path, nargs="?", default=None, help="Output zip (zip input only)"
    )
    parser.add_argument(
        "--skip-validation",
        action="store_true",
        help="Skip module.prop validation (adapt whatever is there, even if malformed)",
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
