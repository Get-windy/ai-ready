# Validate Monitoring Alerts Deployment Configuration

$DeployDir = $PSScriptRoot

Write-Host "Validating monitoring alerts deployment configuration..." -ForegroundColor Green

# Check main files
$mainFiles = @("docker-compose.yml", ".env.production", "deploy-production.sh", "deploy-production.ps1", "deployment-guide.md", "operations-guide.md")

foreach ($file in $mainFiles) {
    $path = Join-Path $DeployDir $file
    if (Test-Path $path) {
        Write-Host "+ Main file exists: $file" -ForegroundColor Green
    } else {
        Write-Host "- Main file missing: $file" -ForegroundColor Red
        exit 1
    }
}

# Check monitoring config directories
$monitoringDirs = @("prometheus", "alertmanager", "grafana")

foreach ($dir in $monitoringDirs) {
    $path = Join-Path $DeployDir $dir
    if (Test-Path $path) {
        Write-Host "+ Monitoring config directory exists: $dir" -ForegroundColor Green
    } else {
        Write-Host "- Monitoring config directory missing: $dir" -ForegroundColor Red
        exit 1
    }
}

# Check backend code directory
$backendDir = Join-Path (Split-Path $DeployDir -Parent) ".." "backend" "monitoring"
if (Test-Path $backendDir) {
    Write-Host "+ Monitoring module backend directory exists" -ForegroundColor Green
} else {
    Write-Host "- Monitoring module backend directory missing" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "SUCCESS: All deployment configurations validated!" -ForegroundColor Green
Write-Host "Monitoring alerts deployment environment is ready." -ForegroundColor Green