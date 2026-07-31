#!/usr/bin/env python3
"""Validate Anime's dependency-free backend contract baseline."""

from __future__ import annotations

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
OPENAPI = ROOT / "contracts/openapi/anime-v1.yaml"
MIGRATION = ROOT / "contracts/database/migrations/0001_initial.sql"
BACKEND_DOCS = ROOT / "docs/backend"

EXPECTED_DOCS = {
    "00-backend-specification-index.md",
    "01-api-contract.md",
    "02-database-contract.md",
    "03-bangumi-and-sync.md",
    "04-testing-and-delivery.md",
}
EXPECTED_PATHS = {
    "/health",
    "/home",
    "/calendar",
    "/subjects",
    "/search/subjects",
    "/subjects/{subject_id}",
    "/subjects/{subject_id}/episodes",
    "/subjects/{subject_id}/characters",
    "/subjects/{subject_id}/persons",
    "/subjects/{subject_id}/relations",
    "/subjects/{subject_id}/comments",
    "/auth/bangumi/start",
    "/auth/bangumi/callback",
    "/auth/refresh",
    "/auth/logout",
    "/me",
    "/me/collections",
    "/me/collections/{subject_id}",
    "/me/sync/status",
    "/me/sync",
    "/me/sync/conflicts",
    "/me/sync/conflicts/{conflict_id}/resolve",
    "/comments/{comment_id}",
    "/comments/{comment_id}/reports",
}
EXPECTED_TABLES = {
    "users",
    "external_accounts",
    "oauth_requests",
    "refresh_tokens",
    "subjects",
    "subject_aliases",
    "episodes",
    "characters",
    "persons",
    "subject_characters",
    "character_actors",
    "subject_persons",
    "subject_relations",
    "subject_stats",
    "user_subject_collections",
    "user_subject_sync_state",
    "sync_outbox",
    "sync_conflicts",
    "sync_runs",
    "comments",
    "comment_reports",
    "idempotency_keys",
}


def read_clean(path: Path) -> str:
    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        raise ValueError(f"{path.relative_to(ROOT)} contains a UTF-8 BOM")
    text = raw.decode("utf-8")
    if "\r" in text:
        raise ValueError(f"{path.relative_to(ROOT)} must use LF line endings")
    if "\t" in text:
        raise ValueError(f"{path.relative_to(ROOT)} contains tabs")
    trailing = [index for index, line in enumerate(text.splitlines(), 1) if line.rstrip() != line]
    if trailing:
        raise ValueError(f"{path.relative_to(ROOT)} has trailing whitespace on lines {trailing[:5]}")
    return text


def validate_openapi(text: str) -> None:
    if not text.startswith("openapi: 3.1.0\n"):
        raise ValueError("OpenAPI contract must declare 3.1.0")

    paths = set(re.findall(r"^  (/[^:]+):$", text, flags=re.MULTILINE))
    if paths != EXPECTED_PATHS:
        raise ValueError(
            f"OpenAPI paths differ; missing={sorted(EXPECTED_PATHS - paths)}, "
            f"extra={sorted(paths - EXPECTED_PATHS)}"
        )

    operation_ids = re.findall(r"^\s+operationId: ([A-Za-z][A-Za-z0-9]+)$", text, flags=re.MULTILINE)
    if len(operation_ids) != len(set(operation_ids)):
        raise ValueError("OpenAPI operationId values must be unique")
    if len(operation_ids) < len(EXPECTED_PATHS):
        raise ValueError("Every public path must declare at least one operationId")

    definitions: set[tuple[str, str]] = set()
    component_group: str | None = None
    in_components = False
    for line in text.splitlines():
        if line == "components:":
            in_components = True
            continue
        if not in_components:
            continue
        group = re.fullmatch(r"  ([A-Za-z][A-Za-z0-9]+):", line)
        if group:
            component_group = group.group(1)
            continue
        definition = re.fullmatch(r"    ([A-Za-z][A-Za-z0-9]+):", line)
        if definition and component_group:
            definitions.add((component_group, definition.group(1)))

    refs = set(
        re.findall(
            r'\$ref: "#/components/([A-Za-z][A-Za-z0-9]+)/([A-Za-z][A-Za-z0-9]+)"',
            text,
        )
    )
    unresolved = refs - definitions
    if unresolved:
        raise ValueError(f"OpenAPI contains unresolved component references: {sorted(unresolved)}")


def validate_migration(text: str) -> None:
    normalized = text.strip()
    if not normalized.startswith("BEGIN;") or not normalized.endswith("COMMIT;"):
        raise ValueError("Initial migration must be wrapped in BEGIN/COMMIT")
    if re.search(r"\b(DROP\s+(DATABASE|SCHEMA)|TRUNCATE)\b", text, flags=re.IGNORECASE):
        raise ValueError("Initial migration contains a destructive broad statement")

    tables = set(
        re.findall(
            r"^CREATE TABLE ([a-z][a-z0-9_]*) \(",
            text,
            flags=re.MULTILINE,
        )
    )
    if tables != EXPECTED_TABLES:
        raise ValueError(
            f"Migration tables differ; missing={sorted(EXPECTED_TABLES - tables)}, "
            f"extra={sorted(tables - EXPECTED_TABLES)}"
        )
    if text.count("(") != text.count(")"):
        raise ValueError("Migration has unbalanced parentheses")


def main() -> int:
    actual_docs = {path.name for path in BACKEND_DOCS.glob("*.md")}
    if actual_docs != EXPECTED_DOCS:
        raise ValueError(
            f"Backend docs differ; missing={sorted(EXPECTED_DOCS - actual_docs)}, "
            f"extra={sorted(actual_docs - EXPECTED_DOCS)}"
        )
    for name in sorted(EXPECTED_DOCS):
        read_clean(BACKEND_DOCS / name)
    validate_openapi(read_clean(OPENAPI))
    validate_migration(read_clean(MIGRATION))
    print(
        "Backend contracts valid: "
        f"{len(EXPECTED_PATHS)} paths, {len(EXPECTED_TABLES)} tables, {len(EXPECTED_DOCS)} docs."
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, UnicodeError, ValueError) as error:
        print(f"backend contract check failed: {error}", file=sys.stderr)
        raise SystemExit(1)
