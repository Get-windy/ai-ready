# Validate ERP Modules Deployment Configuration

$DeployDir = $PSScriptRoot
$BackendDir = Join-Path (Split-Path $DeployDir -Parent) "backend" "erp"

Write-Host "Validating ERP modules deployment configuration..." -ForegroundColor Green

# Check module directories
$modules = @("erp-batch-sn", "erp-invoice", "erp-purchase-exchange", "erp-sales-exchange", "erp-supplier-portal")
$dockerModules = @("erp-batch-sn", "erp-invoice", "erp-purchase-exchange", "erp-sales-exchange", "supplier-portal")

for ($i = 0; $i -lt $modules.Length; $i++) {
    $module = $modules[$i]
    $dockerModule = $dockerModules[$i]
    
    Write-Host "Checking module: $module" -ForegroundColor Yellow
    
    # Check backend code directory
    $backendPath = Join-Path $BackendDir $module
    if (Test-Path $backendPath) {
        Write-Host "  + Backend code directory exists: $backendPath" -ForegroundColor Green
    } else {
        Write-Host "  - Backend code directory missing: $backendPath" -ForegroundColor Red
        exit 1
    }
    
    # Check Dockerfile
    $dockerfilePath = Join-Path $DeployDir $dockerModule "Dockerfile"
    if (Test-Path $dockerfilePath) {
        Write-Host "  + Dockerfile exists: $dockerfilePath" -ForegroundColor Green
    } else {
        Write-Host "  - Dockerfile missing: $dockerfilePath" -ForegroundColor Red
        exit 1
    }
    
    # Check pom.xml
    $pomPath = Join-Path $backendPath "pom.xml"
    if (Test-Path $pomPath) {
        Write-Host "  + pom.xml exists: $pomPath" -ForegroundColor Green
    } else {
        Write-Host "  - pom.xml missing: $pomPath" -ForegroundColor Red
        exit 1
    }
}

# Check docker-compose.yml
$composePath = Join-Path $DeployDir "docker-compose.yml"
if (Test-Path $composePath) {
    Write-Host "+ docker-compose.yml exists" -ForegroundColor Green
} else {
    Write-Host "- docker-compose.yml missing" -ForegroundColor Red
    exit 1
}

# Check environment config file
$envPath = Join-Path $DeployDir ".env.production"
if (Test-Path $envPath) {
    Write-Host "+ .env.production exists" -ForegroundColor Green
} else {
    Write-Host "- .env.production missing" -ForegroundColor Red
    exit 1
}

# Check deployment scripts
$shScriptPath = Join-Path $DeployDir "deploy-production.sh"
if (Test-Path $shScriptPath) {
    Write-Host "+ deploy-production.sh exists" -ForegroundColor Green
} else {
    Write-Host "- deploy-production.sh missing" -ForegroundColor Red
    exit 1
}

$psScriptPath = Join-Path $DeployDir "deploy-production.ps1"
if (Test-Path $psScriptPath) {
    Write-Host "+ deploy-production.ps1 exists" -ForegroundColor Green
} else {
    Write-Host "- deploy-production.ps1 missing" -ForegroundColor Red
    exit 1
}

# Check documentation
$guidePath = Join-Path $DeployDir "deployment-guide.md"
if (Test-Path $guidePath) {
    Write-Host "+ deployment-guide.md exists" -ForegroundColor Green
} else {
    Write-Host "- deployment-guide.md missing" -ForegroundColor Red
    exit 1
}

$opsGuidePath = Join-Path $DeployDir "operations-guide.md"
if (Test-Path $opsGuidePath) {
    Write-Host "+ operations-guide.md exists" -ForegroundColor Green
} else {
    Write-Host "- operations-guide.md missing" -ForegroundColor Red
    exit 1
}

# Check monitoring configuration
$prometheusPath = Join-Path $DeployDir "monitoring" "prometheus.yml"
if (Test-Path $prometheusPath) {
    Write-Host "+ Prometheus configuration exists" -ForegroundColor Green
} else {
    Write-Host "- Prometheus configuration missing" -ForegroundColor Red
    exit 1
}

$systemAlertsPath = Join-Path $DeployDir "monitoring" "alerts" "erp-modules-alerts.yml"
if (Test-Path $systemAlertsPath) {
    Write-Host "+ System alert rules exist" -ForegroundColor Green
} else {
    Write-Host "- System alert rules missing" -ForegroundColor Red
    exit 1
}

$businessAlertsPath = Join-Path $DeployDir "monitoring" "alerts" "business-metrics-alerts.yml"
if (Test-Path $businessAlertsPath) {
    Write-Host "+ Business alert rules exist" -ForegroundColor Green
} else {
    Write-Host "- Business alert rules missing" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "SUCCESS: All deployment configurations validated!" -ForegroundColor Green
Write-Host "Deployment environment is ready." -ForegroundColor Green