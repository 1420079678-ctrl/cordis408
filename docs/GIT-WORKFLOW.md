# 日常修改与同步 GitHub 指南

远程仓库：<https://github.com/1420079678-ctrl/cordis408>（公开，main 分支）

本机 `gh` 已登录账号 `1420079678-ctrl`，仓库已关联为 `origin`。
由于网络对 `github.com` 的 git 数据端口可能不稳定，下面给**两条**同步路径，按当前网络任选其一。

## 路径 A：git 直连（开了代理 / 网络能连 github.com 时，推荐）

```powershell
# 如使用本地代理（示例 Clash 7890），仅给 git 配置一次：
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890
# 不用时取消： git config --global --unset http.proxy; git config --global --unset https.proxy

# 首次先与远程对齐历史（只需一次）
git pull --rebase origin main
git branch --set-upstream-to=origin/main main

# 之后每次改完：
git add -A
git commit -m "feat: 本次改了什么"
git push
```

提交信息建议用约定式：`feat 新功能 / fix 修复 / docs 文档 / refactor 重构 / style 样式 / chore 杂项`。

## 路径 B：API 快照推送（git 端口被重置、但 gh 登录正常时）

仓库已内置脚本，它走 `api.github.com`（通常可达），把**当前工作区整体**作为一个提交推上去，
自动包含新建文件、自动移除已删除文件，**无需 fetch、不会冲突**：

```powershell
# 在项目根目录
powershell -ExecutionPolicy Bypass -File scripts/push-via-api.ps1 -CommitMessage "feat: 本次改了什么"
```

> 该脚本用 `git ls-files --cached --others --exclude-standard` 决定上传内容，
> 因此 `.gitignore` 里的 `node_modules/`、`target/`、`dist/` 永远不会被上传。

## 常见操作

| 需求 | 命令 |
|---|---|
| 看改了什么 | `git status` / `git diff` |
| 新建功能分支 | `git checkout -b feat/xxx`（路径 A 下 `git push -u origin feat/xxx` 后在网页发 Pull Request） |
| 回到上次提交状态 | `git checkout -- .`（未提交改动会丢，谨慎） |
| 暂存当前改动去切分支 | `git stash` → 回来 `git stash pop` |
| 查看远程是否最新 | `gh api repos/1420079678-ctrl/cordis408/commits/main -q .sha` |
| 改仓库为私有 | `gh repo edit 1420079678-ctrl/cordis408 --visibility private` |

## 不要提交的东西
`node_modules/`、`backend/target/`、`frontend/dist/`、本地日志、API Key 等已在 `.gitignore` 中忽略；
`application.yml` 里若填了真实 `api-key`，提交前请改回占位或改用环境变量。
