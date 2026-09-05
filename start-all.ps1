# 一键启动 Cordis408 前后端（Windows PowerShell）
# 用法：在项目根目录执行  ./start-all.ps1
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# ---- 后端：缺 jar 则先打包 ----
$jar = Join-Path $root "backend\target\cordis408-backend-1.0.0.jar"
if (-not (Test-Path $jar)) {
  Write-Host "未发现后端 jar，开始 Maven 打包..." -ForegroundColor Yellow
  Push-Location (Join-Path $root "backend")
  mvn -DskipTests package
  Pop-Location
}
Start-Process powershell -ArgumentList "-NoExit", "-Command", `"cd '$($root -replace "'","''")\backend'; java -jar target\cordis408-backend-1.0.0.jar`"
Write-Host "后端已在新窗口启动：http://localhost:8080" -ForegroundColor Green

# ---- 前端：缺依赖则先安装 ----
if (-not (Test-Path (Join-Path $root "frontend\node_modules"))) {
  Write-Host "未发现前端依赖，开始 npm install..." -ForegroundColor Yellow
  Push-Location (Join-Path $root "frontend")
  npm install
  Pop-Location
}
Start-Process powershell -ArgumentList "-NoExit", "-Command", `"cd '$($root -replace "'","''")\frontend'; npm run dev`"
Write-Host "前端已在新窗口启动：http://localhost:5173" -ForegroundColor Green

Start-Sleep -Seconds 2
Write-Host "即将打开浏览器..." -ForegroundColor Cyan
Start-Process "http://localhost:5173"
