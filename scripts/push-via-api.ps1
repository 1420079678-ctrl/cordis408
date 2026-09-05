# 当 github.com 的 git 端口被网络阻断、但 gh 已登录（api.github.com 可达）时，
# 通过 GitHub Git Database API 把当前工作区快照整体提交为一个 commit。
# 用法： ./scripts/push-via-api.ps1 [-CommitMessage "..."]
param(
  [string]$Owner = "1420079678-ctrl",
  [string]$Repo = "cordis408",
  [string]$Branch = "main",
  [string]$CommitMessage = "feat: update via GitHub API snapshot"
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root
$tmp = Join-Path $env:TEMP "gh-body.json"
function Invoke-Gh([string]$path, [object]$obj) {
  [IO.File]::WriteAllText($tmp, ($obj | ConvertTo-Json -Depth 12 -Compress), (New-Object Text.UTF8Encoding($false)))
  return (gh api $path -X POST --input $tmp | ConvertFrom-Json)
}

Write-Host "收集受版本管理的文件（git ls-files）..." -ForegroundColor Cyan
$files = git ls-files
Write-Host "共 $($files.Count) 个文件，开始创建 blobs..." -ForegroundColor Cyan
$tree = @()
$i = 0
foreach ($f in $files) {
  $i++
  $full = Join-Path $root ($f -replace '/', '\')
  $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($full))
  $blob = Invoke-Gh "repos/$Owner/$Repo/git/blobs" @{ content = $b64; encoding = "base64" }
  $tree += @{ path = ($f -replace '\\', '/'); mode = "100644"; type = "blob"; sha = $blob.sha }
  if ($i % 10 -eq 0) { Write-Host "  blobs $i / $($files.Count)" }
}

Write-Host "创建 tree..." -ForegroundColor Cyan
$treeResp = Invoke-Gh "repos/$Owner/$Repo/git/trees" @{ tree = $tree }

# 判断分支 ref 是否已存在，决定 parents
$parents = @()
$refExists = $true
try { gh api "repos/$Owner/$Repo/git/ref/heads/$Branch" *> $null } catch { $refExists = $false }
$commitObj = @{ message = $CommitMessage; tree = $treeResp.sha }
if ($refExists) {
  $head = (gh api "repos/$Owner/$Repo/git/ref/heads/$Branch" | ConvertFrom-Json)
  $parents = @($head.object.sha)
  $commitObj.parents = $parents
}
$commitResp = Invoke-Gh "repos/$Owner/$Repo/git/commits" $commitObj
Write-Host "commit = $($commitResp.sha)" -ForegroundColor Green

if ($refExists) {
  [IO.File]::WriteAllText($tmp, (@{ sha = $commitResp.sha; force = $false } | ConvertTo-Json -Compress), (New-Object Text.UTF8Encoding($false)))
  gh api "repos/$Owner/$Repo/git/refs/heads/$Branch" -X PATCH --input $tmp | Out-Null
} else {
  Invoke-Gh "repos/$Owner/$Repo/git/refs" @{ ref = "refs/heads/$Branch"; sha = $commitResp.sha } | Out-Null
}
Write-Host "完成：https://github.com/$Owner/$Repo/tree/$Branch" -ForegroundColor Green
