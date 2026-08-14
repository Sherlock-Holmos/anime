#!/usr/bin/env python3
"""Generate and publish the Anime Gitea Wiki from canonical repository docs."""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE_RELATIVE = Path("docs") / "anime项目完整详细设计文档.md"
SOURCE_PATH = REPO_ROOT / SOURCE_RELATIVE
FRONTEND_PAGE_SOURCES = {
    "15-Frontend-Product-and-Navigation.md": Path("docs/frontend/01-product-and-navigation.md"),
    "16-UI-Design-System.md": Path("docs/frontend/02-design-system.md"),
    "17-Glass-Motion-and-Accessibility.md": Path("docs/frontend/03-glass-motion-and-accessibility.md"),
    "18-Component-Specifications.md": Path("docs/frontend/04-component-specifications.md"),
    "19-Screen-Specifications.md": Path("docs/frontend/05-screen-specifications.md"),
    "20-UI-State-Matrix.md": Path("docs/frontend/06-ui-state-matrix.md"),
    "21-Demo-Fixtures-and-Contracts.md": Path("docs/frontend/07-demo-fixtures-and-contracts.md"),
    "22-Frontend-Testing-and-Acceptance.md": Path("docs/frontend/08-testing-and-acceptance.md"),
    "23-Frontend-Development-Roadmap.md": Path("docs/frontend/09-development-roadmap.md"),
    "24-Frontend-Specification-Index.md": Path("docs/frontend/00-specification-index.md"),
    "25-Engineering-Baseline.md": Path("docs/frontend/10-engineering-baseline.md"),
    "26-Runtime-Architecture.md": Path("docs/frontend/11-runtime-architecture.md"),
    "27-Domain-and-Repository-Contracts.md": Path("docs/frontend/12-domain-repository-contracts.md"),
    "28-Fixture-Specification.md": Path("docs/frontend/13-fixture-specification.md"),
    "29-Compose-Implementation-Specification.md": Path("docs/frontend/14-compose-implementation-spec.md"),
    "30-Requirements-Traceability.md": Path("docs/frontend/15-requirements-traceability.md"),
    "31-Decision-Register.md": Path("docs/frontend/16-decision-register.md"),
    "32-App-Shell-and-Auth-Contract.md": Path("docs/frontend/features/01-app-shell-and-auth.md"),
    "33-Discover-Feature-Contract.md": Path("docs/frontend/features/02-discover.md"),
    "34-Search-Feature-Contract.md": Path("docs/frontend/features/03-search.md"),
    "35-Subject-Feature-Contract.md": Path("docs/frontend/features/04-subject.md"),
    "36-Collection-Feature-Contract.md": Path("docs/frontend/features/05-collection.md"),
    "37-Comment-Feature-Contract.md": Path("docs/frontend/features/06-comment.md"),
    "38-Profile-Settings-Diagnostics-Contract.md": Path("docs/frontend/features/07-profile-settings-diagnostics.md"),
    "39-Frontend-Implementation-Log.md": Path("docs/frontend/17-implementation-log.md"),
    "40-Windows-Desktop-Baseline.md": Path("docs/frontend/18-windows-desktop-baseline.md"),
    "40A-Apple-Design-Baseline.md": Path("docs/frontend/19-apple-design-baseline.md"),
    "40B-Color-and-Layout-Specification.md": Path("docs/frontend/20-color-and-layout-specification.md"),
}
BACKEND_PAGE_SOURCES = {
    "41-Backend-Specification-Index.md": Path("docs/backend/00-backend-specification-index.md"),
    "42-Backend-API-Contract.md": Path("docs/backend/01-api-contract.md"),
    "43-Backend-Database-Contract.md": Path("docs/backend/02-database-contract.md"),
    "44-Bangumi-Adapter-and-Sync-Contract.md": Path("docs/backend/03-bangumi-and-sync.md"),
    "45-Backend-Testing-and-Delivery.md": Path("docs/backend/04-testing-and-delivery.md"),
    "46-Catalog-Cache-and-Media.md": Path("docs/backend/05-catalog-cache-and-media.md"),
}
PAGE_SOURCES = {**FRONTEND_PAGE_SOURCES, **BACKEND_PAGE_SOURCES}

MANAGED_PAGE_NAMES = (
    "Home.md",
    "01-Project-Overview.md",
    "02-Requirements-and-Flows.md",
    "03-System-Architecture.md",
    "04-CMP-Architecture.md",
    "05-Android-Design.md",
    "06-iOS-Design.md",
    "07-Backend-Design.md",
    "08-Bangumi-Integration-and-Sync.md",
    "09-API-Design.md",
    "10-Data-Cache-and-Scoring.md",
    "11-Community-Security-and-Privacy.md",
    "12-Deployment-and-Operations.md",
    "13-Testing-and-Roadmap.md",
    "14-Decisions-and-Risks.md",
    *PAGE_SOURCES.keys(),
    "99-References.md",
    "99-Complete-Design-Baseline.md",
)


def run_git(*args: str, cwd: Path = REPO_ROOT, capture: bool = False) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=cwd,
        check=True,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE if capture else None,
    )
    return result.stdout.strip() if capture else ""


def normalize(text: str) -> str:
    return text.replace("\r\n", "\n").replace("\r", "\n").rstrip() + "\n"


def find_unique(lines: list[str], pattern: str, label: str) -> int:
    regex = re.compile(pattern)
    matches = [index for index, line in enumerate(lines) if regex.match(line)]
    if len(matches) != 1:
        raise ValueError(f"{label} expected one match, found {len(matches)}")
    return matches[0]


def h2_section(lines: list[str], number: int) -> list[str]:
    start = find_unique(lines, rf"^## {number}\.\s", f"section {number}")
    end = len(lines)
    for index in range(start + 1, len(lines)):
        if re.match(r"^## \d+\.\s", lines[index]):
            end = index
            break
    return lines[start:end]


def range_between(
    lines: list[str], start_pattern: str, end_pattern: str | None, label: str
) -> list[str]:
    start = find_unique(lines, start_pattern, f"{label} start")
    if end_pattern is None:
        end = len(lines)
    else:
        end = find_unique(lines, end_pattern, f"{label} end")
        if end <= start:
            raise ValueError(f"{label} end appears before start")
    return lines[start:end]


def join_blocks(blocks: Iterable[Iterable[str]]) -> str:
    return "\n\n".join("\n".join(block).strip() for block in blocks if block).strip()


def promote_headings(lines: list[str]) -> list[str]:
    promoted: list[str] = []
    for line in lines:
        if line.startswith("#### "):
            promoted.append(line[1:])
        elif line.startswith("### "):
            promoted.append(line[1:])
        else:
            promoted.append(line)
    return promoted


def sha256_text(text: str) -> str:
    return hashlib.sha256(normalize(text).encode("utf-8")).hexdigest()


def source_set_hash(main_source_text: str) -> str:
    digest = hashlib.sha256()
    sources = {SOURCE_RELATIVE: normalize(main_source_text)}
    for relative in PAGE_SOURCES.values():
        sources[relative] = normalize((REPO_ROOT / relative).read_text(encoding="utf-8"))
    for relative, content in sorted(sources.items(), key=lambda item: item[0].as_posix()):
        digest.update(relative.as_posix().encode("utf-8"))
        digest.update(b"\0")
        digest.update(content.encode("utf-8"))
    return digest.hexdigest()


def page_banner(source_relative: Path | str, source_hash: str) -> str:
    source_label = source_relative.as_posix() if isinstance(source_relative, Path) else source_relative
    return normalize(
        f"""> 本页面由 Anime 主仓库自动生成，请勿直接在 Wiki 修改。<br>
> 源文件：`{source_label}`<br>
> 源文件 SHA-256：`{source_hash}`"""
    ).strip()


def wrap_page(title: str, body: str, source_relative: Path, source_hash: str) -> str:
    return normalize(
        f"# {title}\n\n{page_banner(source_relative, source_hash)}\n\n{body.strip()}"
    )


def inject_document_banner(document: str, source_relative: Path) -> str:
    document = normalize(document)
    source_hash = sha256_text(document)
    first_line, separator, rest = document.partition("\n")
    if not separator or not first_line.startswith("# "):
        raise ValueError(f"managed document must start with H1: {source_relative}")
    return normalize(
        f"{first_line}\n\n{page_banner(source_relative, source_hash)}\n\n{rest.lstrip()}"
    )


def build_home(documentation_hash: str) -> str:
    return normalize(
        f"""# Anime 项目 Wiki

{page_banner("docs/（总体设计、前端与后端规范受管集合）", documentation_hash)}

> 文档版本：V1.4（CMP 与后端可执行契约基线）<br>
> 编制日期：2026-07-21<br>
> 当前开发阶段：F0 工程与技术验证

## 项目简介

Anime 是一款面向动漫爱好者的移动端资料与社区应用。当前交付平台为 Android，未来计划支持 iOS。产品专注于动漫资料、观看记录与公开短评，不提供视频播放。

## 当前产品基线

- 使用 Bangumi 官方 API 获取和同步动漫资料；
- MVP 只展示 Bangumi 来源评分、投票数和评分分布；
- 不建设 Anime 自有评分、加权榜单或评分写入；
- Bangumi OAuth 是唯一登录方式；
- 收藏状态与观看进度和 Bangumi 双向同步；
- Android 使用液态玻璃视觉语言，并按系统与性能能力降级；
- 首个可玩 Android 版本由确定性 Fixture 驱动，无需等待 Anime 后端；
- 生产业务入口仅通过 Cloudflare Tunnel 域名提供。

## 技术栈

| 层级 | 技术 |
|---|---|
| 客户端 | Kotlin + Compose Multiplatform |
| 后端 | Rust + Axum + Tokio |
| 数据库 | PostgreSQL；Redis 仅作可丢失缓存 |
| UI 效果 | Kyant Backdrop + 项目设计系统封装 |
| 部署 | Docker + Cloudflare Tunnel |

## 文档导航

| 页面 | 说明 |
|---|---|
| [01-Project-Overview](01-Project-Overview) | 文档说明、产品概述、范围与权限 |
| [02-Requirements-and-Flows](02-Requirements-and-Flows) | 用户流程、功能与非功能需求 |
| [03-System-Architecture](03-System-Architecture) | 总体架构与模块化单体边界 |
| [04-CMP-Architecture](04-CMP-Architecture) | 共享客户端架构、状态、网络与存储 |
| [05-Android-Design](05-Android-Design) | Android 平台设计 |
| [06-iOS-Design](06-iOS-Design) | iOS 平台预留设计 |
| [07-Backend-Design](07-Backend-Design) | Rust 后端架构 |
| [08-Bangumi-Integration-and-Sync](08-Bangumi-Integration-and-Sync) | Bangumi 集成与双向同步 |
| [09-API-Design](09-API-Design) | REST API 设计 |
| [10-Data-Cache-and-Scoring](10-Data-Cache-and-Scoring) | 数据库、缓存与 Bangumi 评分镜像 |
| [11-Community-Security-and-Privacy](11-Community-Security-and-Privacy) | 社区治理、安全与隐私 |
| [12-Deployment-and-Operations](12-Deployment-and-Operations) | 部署、运维、备份与恢复 |
| [13-Testing-and-Roadmap](13-Testing-and-Roadmap) | 测试与开发阶段 |
| [14-Decisions-and-Risks](14-Decisions-and-Risks) | 决策、ADR、风险与 Agent 约束 |
| [15-Frontend-Product-and-Navigation](15-Frontend-Product-and-Navigation) | 前端产品边界、信息架构和路由 |
| [16-UI-Design-System](16-UI-Design-System) | 颜色、排版、间距、形状和响应式 Token |
| [17-Glass-Motion-and-Accessibility](17-Glass-Motion-and-Accessibility) | 玻璃、动效、性能降级与无障碍 |
| [18-Component-Specifications](18-Component-Specifications) | CMP 公共组件详细规范 |
| [19-Screen-Specifications](19-Screen-Specifications) | 发现、搜索、详情、收藏等页面规范 |
| [20-UI-State-Matrix](20-UI-State-Matrix) | 加载、离线、错误、同步与冲突状态 |
| [21-Demo-Fixtures-and-Contracts](21-Demo-Fixtures-and-Contracts) | Fixture、领域模型与 Repository 契约 |
| [22-Frontend-Testing-and-Acceptance](22-Frontend-Testing-and-Acceptance) | 前端测试、性能预算和验收门禁 |
| [23-Frontend-Development-Roadmap](23-Frontend-Development-Roadmap) | F0–F10 与多 Agent 协作边界 |
| [99-References](99-References) | 官方资料与完成定义 |
| [24-Frontend-Specification-Index](24-Frontend-Specification-Index) | 文档优先级、术语、完成定义与变更流程 |
| [25-Engineering-Baseline](25-Engineering-Baseline) | 固定工具链、依赖、模块、变体与构建门禁 |
| [26-Runtime-Architecture](26-Runtime-Architecture) | UDF、AppContainer、导航、登录门禁与同步运行时 |
| [27-Domain-and-Repository-Contracts](27-Domain-and-Repository-Contracts) | 完整领域模型和 Repository 合同 |
| [28-Fixture-Specification](28-Fixture-Specification) | 确定性数据、12 个条目与 12 个异常场景 |
| [29-Compose-Implementation-Specification](29-Compose-Implementation-Specification) | Compose API、尺寸、Glass、无障碍与性能 |
| [30-Requirements-Traceability](30-Requirements-Traceability) | 需求、场景、测试和合并门禁追踪 |
| [31-Decision-Register](31-Decision-Register) | 已批准、延后和否决的前端决策 |
| [32-App-Shell-and-Auth-Contract](32-App-Shell-and-Auth-Contract) | App Shell、四根栈与登录门禁 |
| [33-Discover-Feature-Contract](33-Discover-Feature-Contract) | 发现 Feature 完整合同 |
| [34-Search-Feature-Contract](34-Search-Feature-Contract) | 搜索 Feature 完整合同 |
| [35-Subject-Feature-Contract](35-Subject-Feature-Contract) | 条目详情 Feature 完整合同 |
| [36-Collection-Feature-Contract](36-Collection-Feature-Contract) | 收藏与同步 Feature 完整合同 |
| [37-Comment-Feature-Contract](37-Comment-Feature-Contract) | 评论 Feature 完整合同 |
| [38-Profile-Settings-Diagnostics-Contract](38-Profile-Settings-Diagnostics-Contract) | 个人、设置与诊断完整合同 |
| [39-Frontend-Implementation-Log](39-Frontend-Implementation-Log) | 已验证的前端实现增量 |
| [40-Windows-Desktop-Baseline](40-Windows-Desktop-Baseline) | Windows CMP 工程、发行与验收基线 |
| [41-Backend-Specification-Index](41-Backend-Specification-Index) | 后端规范优先级、技术边界与变更流程 |
| [42-Backend-API-Contract](42-Backend-API-Contract) | OpenAPI、认证、分页、幂等和错误语义 |
| [43-Backend-Database-Contract](43-Backend-Database-Contract) | PostgreSQL Schema、事务和 Migration 规则 |
| [44-Bangumi-Adapter-and-Sync-Contract](44-Bangumi-Adapter-and-Sync-Contract) | Bangumi 映射、限流、重试与双向同步 |
| [45-Backend-Testing-and-Delivery](45-Backend-Testing-and-Delivery) | 后端测试矩阵、阶段与完成定义 |
| [99-Complete-Design-Baseline](99-Complete-Design-Baseline) | 完整 V1.3 总体设计快照 |

## 维护原则

- 主仓库 `docs/` 是唯一事实源；
- Wiki 页面由 `scripts/sync_wiki.py` 单向生成；
- Wiki 不直接编辑；
- 设计变更与相关代码在同一个提交或 Pull Request 中评审；
- 页面顶部的源文件 SHA-256 用于核验同步状态。
"""
    )


def build_pages(source_text: str) -> dict[str, str]:
    source_text = normalize(source_text)
    source_hash = sha256_text(source_text)
    documentation_hash = source_set_hash(source_text)
    lines = source_text.rstrip("\n").split("\n")

    client = h2_section(lines, 8)
    cmp_prefix = range_between(client, r"^## 8\.\s", r"^### 8\.9\s", "CMP prefix")
    android = range_between(client, r"^### 8\.9\s", r"^### 8\.10\s", "Android")
    ios = range_between(client, r"^### 8\.10\s", r"^### 8\.11\s", "iOS")
    cmp_tail = range_between(client, r"^### 8\.11\s", None, "CMP tail")

    page_bodies = {
        "01-Project-Overview.md": ("项目概述与范围", join_blocks(h2_section(lines, n) for n in range(0, 4))),
        "02-Requirements-and-Flows.md": ("需求与用户流程", join_blocks(h2_section(lines, n) for n in range(4, 7))),
        "03-System-Architecture.md": ("总体系统架构", join_blocks([h2_section(lines, 7)])),
        "04-CMP-Architecture.md": ("Compose Multiplatform 客户端架构", join_blocks([cmp_prefix, cmp_tail])),
        "05-Android-Design.md": ("Android 平台设计", join_blocks([promote_headings(android)])),
        "06-iOS-Design.md": ("iOS 平台设计", join_blocks([promote_headings(ios)])),
        "07-Backend-Design.md": ("Rust 后端设计", join_blocks([h2_section(lines, 9)])),
        "08-Bangumi-Integration-and-Sync.md": ("Bangumi 集成与同步", join_blocks([h2_section(lines, 10)])),
        "09-API-Design.md": ("REST API 设计", join_blocks([h2_section(lines, 11)])),
        "10-Data-Cache-and-Scoring.md": ("数据、缓存与 Bangumi 评分镜像", join_blocks(h2_section(lines, n) for n in (12, 13))),
        "11-Community-Security-and-Privacy.md": ("社区治理、安全与隐私", join_blocks(h2_section(lines, n) for n in (14, 15))),
        "12-Deployment-and-Operations.md": ("部署与运维", join_blocks(h2_section(lines, n) for n in (16, 17, 18))),
        "13-Testing-and-Roadmap.md": ("测试与开发路线图", join_blocks(h2_section(lines, n) for n in (19, 20))),
        "14-Decisions-and-Risks.md": ("决策、ADR 与风险", join_blocks(h2_section(lines, n) for n in (21, 22, 23, 24))),
        "99-References.md": ("官方资料、完成定义与前端规范", join_blocks(h2_section(lines, n) for n in (25, 26, 27))),
    }

    pages = {"Home.md": build_home(documentation_hash)}
    for filename, (title, body) in page_bodies.items():
        pages[filename] = wrap_page(title, body, SOURCE_RELATIVE, source_hash)

    for filename, source_relative in PAGE_SOURCES.items():
        document = (REPO_ROOT / source_relative).read_text(encoding="utf-8")
        pages[filename] = inject_document_banner(document, source_relative)

    complete = source_text.split("\n", 1)
    if len(complete) == 2:
        complete_text = f"{complete[0]}\n\n{page_banner(SOURCE_RELATIVE, source_hash)}\n\n{complete[1]}"
    else:
        complete_text = f"{page_banner(SOURCE_RELATIVE, source_hash)}\n\n{source_text}"
    pages["99-Complete-Design-Baseline.md"] = normalize(complete_text)

    if set(pages) != set(MANAGED_PAGE_NAMES):
        missing = set(MANAGED_PAGE_NAMES) - set(pages)
        extra = set(pages) - set(MANAGED_PAGE_NAMES)
        raise AssertionError(f"managed page mismatch; missing={missing}, extra={extra}")
    return pages


def write_pages(output: Path, pages: dict[str, str]) -> None:
    output.mkdir(parents=True, exist_ok=True)
    for name in MANAGED_PAGE_NAMES:
        (output / name).write_text(pages[name], encoding="utf-8", newline="\n")


def derive_wiki_remote() -> str:
    remote = run_git("remote", "get-url", "origin", capture=True)
    if remote.endswith(".git"):
        return remote[:-4] + ".wiki.git"
    return remote.rstrip("/") + ".wiki.git"


def changed_managed_pages(repo: Path) -> list[str]:
    status = run_git("status", "--porcelain", "--", *MANAGED_PAGE_NAMES, cwd=repo, capture=True)
    return [line for line in status.splitlines() if line.strip()]


def sync_remote(remote: str, pages: dict[str, str], publish: bool) -> int:
    with tempfile.TemporaryDirectory(prefix="anime-wiki-sync-") as temp_dir:
        checkout = Path(temp_dir) / "wiki"
        # Wiki files are generated with LF on every platform. Disable the user's
        # global autocrlf setting so Windows checks do not report false changes.
        run_git("-c", "core.autocrlf=false", "clone", "--quiet", remote, str(checkout))
        run_git("config", "core.autocrlf", "false", cwd=checkout)
        write_pages(checkout, pages)
        changed = changed_managed_pages(checkout)
        if not changed:
            print("Wiki is already synchronized.")
            return 0
        print("Wiki pages with differences:")
        for line in changed:
            print(f"  {line}")
        if not publish:
            return 1

        documentation_hash = source_set_hash(SOURCE_PATH.read_text(encoding="utf-8"))
        run_git("config", "user.name", "Anime Wiki Sync", cwd=checkout)
        run_git("config", "user.email", "wiki-sync@jokersh.site", cwd=checkout)
        run_git("add", "--", *MANAGED_PAGE_NAMES, cwd=checkout)
        run_git("commit", "-m", f"docs: sync V1.4 implementation contracts ({documentation_hash[:12]})", cwd=checkout)
        run_git("push", "origin", "HEAD", cwd=checkout)
        print("Wiki synchronization published.")
        return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true", help="compare generated pages with the remote Wiki")
    mode.add_argument("--publish", action="store_true", help="commit and push generated pages to the remote Wiki")
    parser.add_argument("--wiki-remote", help="Wiki Git remote; defaults to origin with .wiki.git suffix")
    parser.add_argument(
        "--output",
        type=Path,
        default=REPO_ROOT / "build" / "wiki",
        help="local preview output directory",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not SOURCE_PATH.is_file():
        raise FileNotFoundError(f"canonical source not found: {SOURCE_PATH}")
    for relative in PAGE_SOURCES.values():
        if not (REPO_ROOT / relative).is_file():
            raise FileNotFoundError(f"canonical source not found: {REPO_ROOT / relative}")
    pages = build_pages(SOURCE_PATH.read_text(encoding="utf-8"))

    if args.check or args.publish:
        remote = args.wiki_remote or os.environ.get("ANIME_WIKI_REMOTE") or derive_wiki_remote()
        return sync_remote(remote, pages, publish=args.publish)

    output = args.output.resolve()
    write_pages(output, pages)
    print(f"Generated {len(pages)} Wiki pages in {output}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f"wiki sync failed: {error}", file=sys.stderr)
        raise SystemExit(2)
