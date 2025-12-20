# Build and Install Dependencies
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Installing Project Dependencies" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$projectPath = "e:\Study\DOANFINAL\360edu_BE"
Set-Location $projectPath

Write-Host "Current Directory: $PWD" -ForegroundColor Yellow
Write-Host ""
Write-Host "Downloading and installing all dependencies..." -ForegroundColor Green
Write-Host "This may take a few minutes on first run..." -ForegroundColor Green
Write-Host ""

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"

# Run Maven directly without wrapper
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue
if ($mavenCmd) {
    mvn clean install -DskipTests
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "=========================================" -ForegroundColor Green
        Write-Host "  Dependencies Installed Successfully!" -ForegroundColor Green
        Write-Host "=========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Now you can run the application with:" -ForegroundColor Yellow
        Write-Host "  mvn spring-boot:run" -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "=========================================" -ForegroundColor Red
        Write-Host "  Installation Failed!" -ForegroundColor Red
        Write-Host "=========================================" -ForegroundColor Red
    }
} else {
    Write-Host "Maven not found in PATH!" -ForegroundColor Red
    Write-Host "Please install Maven or add it to PATH" -ForegroundColor Red
}

Write-Host ""
Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
