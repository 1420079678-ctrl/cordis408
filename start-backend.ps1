# 仅启动后端
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Join-Path $root "backend\target\cordis408-backend-1.0.0.jar"
if (-not (Test-Path $jar)) {
  Push-Location (Join-Path $root "backend"); mvn -DskipTests package; Pop-Location
}
Set-Location (Join-Path $root "backend")
java -jar target\cordis408-backend-1.0.0.jar
