Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Starting Smart Traffic Police Management System..." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

mvn javafx:run
if ($LASTEXITCODE -ne 0) {
    Write-Host "Executing via standard Java launcher fallback..." -ForegroundColor Yellow
    mvn compile exec:java -Dexec.mainClass="com.smarttraffic.SmartTrafficAppLauncher"
}
