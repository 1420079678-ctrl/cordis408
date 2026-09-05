# 仅启动前端
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Join-Path $root "frontend")
if (-not (Test-Path "node_modules")) { npm install }
npm run dev
