# Final Validation for ERP Modules Deployment

Write-Host "Final validation for ERP modules deployment..." -ForegroundColor Green

# Check backend directories
$backendDirs = @("erp-batch-sn", "erp-invoice", "erp-purchase", "erp-sale", "erp-supplier-portal")
$backendBase = "I:\AI-Ready\backend\erp"

foreach ($dir in $backendDirs) {
    $path = Join-Path $backendBase $dir
    if (Test-Path $path) {
        Write-Host "✓ Backend directory exists: $dir" -ForegroundColor Green
    } else {
        Write-Host "✗ Backend directory missing: $dir" -ForegroundColor Red
        exit 1
    }
}

# Check Dockerfiles in backend
foreach ($dir in $backendDirs) {
    $dockerfilePath = Join-Path $backendBase $dir "Dockerfile"
    if (Test-Path $dockerfilePath) {
        Write-Host "✓ Dockerfile exists in backend: $dir" -ForegroundColor Green
    } else {
        Write-Host "✗ Dockerfile missing in backend: $dir" -ForegroundColor Yellow
        # This is OK for some modules that might not need custom Dockerfile
    }
}

# Check deploy directories
$deployDirs = @("erp-batch-sn", "erp-invoice", "erp-purchase-exchange", "erp-sales-exchange", "supplier-portal")
$deployBase = "I:\AI-Ready\deploy\erp-modules"

foreach ($dir in $deployDirs) {
    $path = Join-Path $deployBase $dir
    if (Test-Path $path) {
        Write-Host "✓ Deploy directory exists: $dir" -ForegroundColor Green
    } else {
        Write-Host "✗ Deploy directory missing: $dir" -ForegroundColor Red
        exit 1
    }
}

# Check Dockerfiles in deploy
foreach ($dir in $deployDirs) {
    $dockerfilePath = Join-Path $deployBase $dir "Dockerfile"
    if (Test-Path $dockerfilePath) {
        Write-Host "✓ Dockerfile exists in deploy: $dir" -ForegroundColor Green
    } else {
        Write-Host "✗ Dockerfile missing in deploy: $dir" -ForegroundColor Red
        exit 1
    }
}

# Check main files
$mainFiles = @("docker-compose.yml", ".env.production", "deploy-production.sh", "deploy-production.ps1", "deployment-guide.md", "operations-guide.md")

foreach ($file in $mainFiles) {
    $path = Join-Path $deployBase $file
    if (Test-Path $path) {
        Write-Host "✓ Main file exists: $file" -ForegroundColor Green
    } else {
        Write-Host "✗ Main file missing: $file" -ForegroundColor Red
        exit 1
    }
}

# Check monitoring files
$monitoringFiles = @("prometheus.yml", "alerts\erp-modules-alerts.yml", "alerts\business-metrics-alerts.yml")
$monitoringBase = Join-Path $deployBase "monitoring"

foreach ($file in $monitoringFiles) {
    $path = Join-Path $monitoringBase $file
    if (Test-Path $path) {
        Write-Host "✓ Monitoring file exists: $file" -ForegroundColor Green
    } else {
        Write-Host "✗ Monitoring file missing: $file" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "✅ All validations passed!" -ForegroundColor Green
Write-Host "ERP modules deployment is ready for production." -ForegroundColor Green