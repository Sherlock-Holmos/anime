"""Reject newly added hard-coded user-facing copy."""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys


UI_CALL = re.compile(
    r"(?:Text|Button|TextButton|OutlinedButton|Label|Section|Picker|TextField|DatePicker|"
    r"ContentUnavailableView|Anime(?:Primary|Secondary)Button|navigationTitle|"
    r"accessibilityLabel|title|subtitle|description|actionLabel)\s*(?:\([^\n]*|=\s*)"
)
STRING_LITERAL = re.compile(r'"((?:\\.|[^"\\])*)"')
RESOURCE_CALL = re.compile(r"(?:stringResource|animeString|LocalizedStringKey|Res\.string)")
TECHNICAL = re.compile(
    r"^(?:https?://|[a-z][a-z0-9_.-]*(?:/[a-z0-9_.-]+)*|"
    r"systemImage|public|private|internal|all|tv|web|ova|movie|other|"
    r"following|popular|relevance|rating|updated|published|hidden|deleted)$"
)
FORMAT_ONLY = re.compile(r"^[%\\d.,:/()_+\-–—· ]+$")
FORMAT_SPEC = re.compile(r"^%[-+0-9.*]*[a-zA-Z]$")


def changed_lines(base: str | None) -> list[tuple[str, int, str]]:
    # Compare the working tree with the selected base. This deliberately includes
    # staged and unstaged edits so the rule also protects local development before
    # a commit is created.
    command = ["git", "diff", "--unified=0", base or "HEAD", "--"]
    result = subprocess.run(
        command,
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    current_file = None
    current_line = 0
    output: list[tuple[str, int, str]] = []
    for line in result.stdout.splitlines():
        if line.startswith("+++"):
            current_file = line[6:] if line.startswith("+++ b/") else None
        elif line.startswith("@@"):
            match = re.search(r"\+([0-9]+)(?:,([0-9]+))?", line)
            if match:
                current_line = int(match.group(1))
        elif line.startswith("+") and not line.startswith("+++") and current_file:
            output.append((current_file, current_line, line[1:]))
            current_line += 1
        elif not line.startswith("-") and current_file:
            current_line += 1
    return output


def is_source_file(path: str) -> bool:
    normalized = f"/{path}"
    return (
        (path.endswith(".kt") or path.endswith(".swift"))
        and "/build/" not in normalized
        and "/src/test/" not in normalized
        and "/src/commonTest/" not in normalized
        and "/src/androidHostTest/" not in normalized
    )


def violations(lines: list[tuple[str, int, str]]) -> list[str]:
    errors: list[str] = []
    for path, line_number, line in lines:
        stripped = line.strip()
        if not is_source_file(path) or stripped.startswith("//") or stripped.startswith("*"):
            continue
        if "i18n-ignore" in line or RESOURCE_CALL.search(line) or not UI_CALL.search(line):
            continue
        for match in STRING_LITERAL.finditer(line):
            value = match.group(1)
            if not value or "${" in value or "\\(" in value:
                continue
            normalized = value.strip()
            if TECHNICAL.fullmatch(normalized) or FORMAT_ONLY.fullmatch(normalized) or FORMAT_SPEC.fullmatch(normalized):
                continue
            # Catch single-word UI labels as well as phrases. Protocol values such as
            # `tv` and `published` are explicitly allow-listed above; product copy must
            # still go through a resource even when it contains no whitespace.
            if any(ch.isalpha() for ch in value) or re.search(r"[\u4e00-\u9fff\u3040-\u30ff]", value):
                errors.append(f"{path}:{line_number}: hard-coded UI copy: {value}")
                break
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default=None, help="Git base ref for a pull request")
    parser.add_argument("--changed-only", action="store_true")
    args = parser.parse_args()
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if not args.changed_only:
        parser.error("Only --changed-only is supported until the legacy migration is complete")
    base = args.base or os.environ.get("LOCALIZATION_BASE_REF")
    try:
        errors = violations(changed_lines(base))
    except subprocess.CalledProcessError as exc:
        print(exc.stderr or "Unable to calculate localization diff", file=sys.stderr)
        return 2
    if errors:
        print("Localization check failed. Move user-facing copy to a resource file:")
        print("\n".join(f"- {error}" for error in errors))
        return 1
    print("Localization check passed: no new hard-coded UI copy detected.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
