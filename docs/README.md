# 文档维护与 Wiki 同步

## 规范性事实源

项目设计以 [`anime项目完整详细设计文档.md`](anime项目完整详细设计文档.md) 为总体基线，以 [`frontend/00-specification-index.md`](frontend/00-specification-index.md) 为 CMP Android 实施入口。`frontend/` 的产品、视觉、工程、运行时、领域、Fixture、追踪和逐 Feature 契约共同构成 V1.3 无歧义开发基线。它们位于主仓库 `docs/`，接受同一套版本审查。根目录和 `docs/` 中名称包含“探讨”的文档是历史研究材料，不作为开发约束，除非结论已经合并到规范性文档。

Gitea Wiki 是面向阅读和导航的发布结果，不是第二个可编辑源。Wiki 页面顶部会包含源文件路径和 SHA-256，用于确认内容来自哪一版基线。

```text
修改主仓库 docs/ 中的规范性文件
  -> 在同一个提交中评审设计和代码
  -> scripts/sync_wiki.py 拆分总体设计并原样发布前端分册
  -> 单向发布到 anime.wiki.git
```

禁止在主仓库和 Wiki 中双向人工维护相同内容。如果在线 Wiki 需要修正，先修改主仓库文档，再重新发布。

## 本地预览

在仓库根目录运行：

```powershell
python scripts/sync_wiki.py
```

生成结果位于 `build/wiki/`。脚本生成 43 个受管页面，只读取总体设计和 `docs/frontend/`，不读取历史探讨文档。

## 检查在线 Wiki 是否同步

```powershell
python scripts/sync_wiki.py --check
```

脚本会克隆 Wiki 到临时目录、生成预期页面并比较。如果在线 Wiki 已同步，退出码为 `0`；存在差异时退出码为 `1`。

## 手动发布

当前开发机已配置 Gitea SSH 凭据时运行：

```powershell
python scripts/sync_wiki.py --publish
```

默认根据主仓库 `origin` 推导 Wiki 地址：

```text
ssh://git@git.jokersh.site:2222/Holmes/anime.git
  -> ssh://git@git.jokersh.site:2222/Holmes/anime.wiki.git
```

也可以显式指定：

```powershell
python scripts/sync_wiki.py --publish `
  --wiki-remote ssh://git@git.jokersh.site:2222/Holmes/anime.wiki.git
```

## Gitea Actions 自动发布

`.gitea/workflows/wiki-sync.yml` 会在 `dev` 分支的设计基线或同步脚本发生变化时发布 Wiki，也支持手动触发。未配置 Token 时流水线会安全跳过发布，不会让其他检查失败。

流水线需要在 Anime 仓库中配置 Secret：

```text
WIKI_SYNC_TOKEN
```

该 Token 只需要 `Holmes/anime` 仓库 Wiki 的写权限。流水线通过 Git 的 HTTP Authorization Header 使用 Token，不把 Token 写入远程 URL、仓库或日志。

## 页面分组

| Wiki 页面 | 源章节 |
|---|---|
| 01 Project Overview | 0–3 |
| 02 Requirements and Flows | 4–6 |
| 03 System Architecture | 7 |
| 04 CMP Architecture | 8.1–8.8、8.11–8.12 |
| 05 Android Design | 8.9 |
| 06 iOS Design | 8.10 |
| 07 Backend Design | 9 |
| 08 Bangumi Integration and Sync | 10 |
| 09 API Design | 11 |
| 10 Data Cache and Scoring | 12–13 |
| 11 Community Security and Privacy | 14–15 |
| 12 Deployment and Operations | 16–18 |
| 13 Testing and Roadmap | 19–20 |
| 14 Decisions and Risks | 21–24 |
| 15 Frontend Product and Navigation | `frontend/01-product-and-navigation.md` |
| 16 UI Design System | `frontend/02-design-system.md` |
| 17 Glass Motion and Accessibility | `frontend/03-glass-motion-and-accessibility.md` |
| 18 Component Specifications | `frontend/04-component-specifications.md` |
| 19 Screen Specifications | `frontend/05-screen-specifications.md` |
| 20 UI State Matrix | `frontend/06-ui-state-matrix.md` |
| 21 Demo Fixtures and Contracts | `frontend/07-demo-fixtures-and-contracts.md` |
| 22 Frontend Testing and Acceptance | `frontend/08-testing-and-acceptance.md` |
| 23 Frontend Development Roadmap | `frontend/09-development-roadmap.md` |
| 24 Frontend Specification Index | `frontend/00-specification-index.md` |
| 25 Engineering Baseline | `frontend/10-engineering-baseline.md` |
| 26 Runtime Architecture | `frontend/11-runtime-architecture.md` |
| 27 Domain and Repository Contracts | `frontend/12-domain-repository-contracts.md` |
| 28 Fixture Specification | `frontend/13-fixture-specification.md` |
| 29 Compose Implementation Specification | `frontend/14-compose-implementation-spec.md` |
| 30 Requirements Traceability | `frontend/15-requirements-traceability.md` |
| 31 Decision Register | `frontend/16-decision-register.md` |
| 32 App Shell and Auth Contract | `frontend/features/01-app-shell-and-auth.md` |
| 33 Discover Feature Contract | `frontend/features/02-discover.md` |
| 34 Search Feature Contract | `frontend/features/03-search.md` |
| 35 Subject Feature Contract | `frontend/features/04-subject.md` |
| 36 Collection Feature Contract | `frontend/features/05-collection.md` |
| 37 Comment Feature Contract | `frontend/features/06-comment.md` |
| 38 Profile Settings Diagnostics Contract | `frontend/features/07-profile-settings-diagnostics.md` |
| 39 Frontend Implementation Log | `frontend/17-implementation-log.md` |
| 40 Windows Desktop Baseline | `frontend/18-windows-desktop-baseline.md` |
| 99 References | 25–27 |
| 99 Complete Design Baseline | 完整快照 |

页面文件名属于稳定公开链接。即使章节名称发生调整，也不要随意修改文件名；需要迁移时应保留旧页面并增加跳转说明。
