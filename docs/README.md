# 文档维护与 Wiki 同步

## 唯一事实源

项目设计以 [`anime项目完整详细设计文档.md`](anime项目完整详细设计文档.md) 为唯一事实源。根目录下其余探讨文档是历史研究材料，不作为开发约束，除非其结论已经合并到完整设计基线。

Gitea Wiki 是面向阅读和导航的发布结果，不是第二个可编辑源。Wiki 页面顶部会包含源文件路径和 SHA-256，用于确认内容来自哪一版基线。

```text
修改主仓库 docs/
  -> 在同一个提交中评审设计和代码
  -> scripts/sync_wiki.py 自动拆分页面
  -> 单向发布到 anime.wiki.git
```

禁止在主仓库和 Wiki 中双向人工维护相同内容。如果在线 Wiki 需要修正，先修改主仓库文档，再重新发布。

## 本地预览

在仓库根目录运行：

```powershell
python scripts/sync_wiki.py
```

生成结果位于 `build/wiki/`。脚本只生成受管页面，不读取历史探讨文档。

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

`.gitea/workflows/wiki-sync.yml` 会在 `dev` 分支的设计基线或同步脚本发生变化时发布 Wiki，也支持手动触发。

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
| 99 References | 25–26 |
| 99 Complete Design Baseline | 完整快照 |

页面文件名属于稳定公开链接。即使章节名称发生调整，也不要随意修改文件名；需要迁移时应保留旧页面并增加跳转说明。
