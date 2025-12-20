# Run Spring Boot Application
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Starting Edu360 Management System" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$projectPath = "e:\Study\DOANFINAL\360edu_BE"
Set-Location $projectPath

Write-Host "Current Directory: $PWD" -ForegroundColor Yellow
Write-Host ""
Write-Host "Checking requirements..." -ForegroundColor Green

# Check Java
$javaVersion = java -version 2>&1 | Select-String "version"
if ($javaVersion) {
    Write-Host "✓ Java: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "✗ Java not found!" -ForegroundColor Red
    exit 1
}

# Check Maven
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue
if ($mavenCmd) {
    $mavenVersion = mvn --version 2>&1 | Select-String "Apache Maven" | Select-Object -First 1
    Write-Host "✓ Maven: $mavenVersion" -ForegroundColor Green
} else {
    Write-Host "✗ Maven not found!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Make sure MySQL is running on localhost:3306" -ForegroundColor Yellow
Write-Host ""
Write-Host "Starting Spring Boot application..." -ForegroundColor Green
Write-Host "Backend will be available at: http://localhost:8080" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Run Spring Boot
mvn spring-boot:run
