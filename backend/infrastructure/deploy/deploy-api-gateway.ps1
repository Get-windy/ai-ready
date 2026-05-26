# API Gateway Deployment Script
# Deploys Nginx as API Gateway on port 8080

param(
    [switch]$Remove = $false,
    [switch]$NoBuild = $false
)

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Gateway Deployment Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$containerName = "ai-ready-api-gateway"
$imageName = "nginx:alpine"
$hostPort = 8080
$containerPort = 80
$configFile = "I:\AI-Ready\test_env\etc\important\api-gateway.conf"
$hostConfigPath = "I:\AI-Ready\deploy\nginx\api-gateway.conf"

# Step 1: Create nginx config directory
Write-Host "[1/6] Creating nginx configuration directory..." -ForegroundColor Yellow
try {
    New-Item -Path "I:\AI-Ready\deploy\nginx" -ItemType Directory -Force | Out-Null
    Write-Host "  ✅ Directory created" -ForegroundColor Green
} catch {
    Write-Host "  ⚠️  Error creating directory: $_" -ForegroundColor Yellow
}

# Step 2: Copy config to deploy directory
Write-Host "[2/6] Copying nginx configuration..." -ForegroundColor Yellow
try {
    Copy-Item -Path $configFile -Destination $hostConfigPath -Force
    Write-Host "  ✅ Configuration copied to $hostConfigPath" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Error copying config: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Create docker-compose.yml if it doesn't exist
Write-Host "[3/6] Checking docker-compose configuration..." -ForegroundColor Yellow
$composeFile = "I:\AI-Ready\deploy\docker-compose.yml"
if (-not (Test-Path $composeFile)) {
    Write-Host "  Creating docker-compose.yml..." -ForegroundColor Yellow
    $composeContent = @{
        version = "3.8"
        services = @{
            api-gateway = @{
                image = "nginx:alpine"
                container_name = "ai-ready-api-gateway"
                ports = @(
                    "8080:80"
                )
                volumes = @(
                    "I:\AI-Ready\deploy\nginx\api-gateway.conf:/etc/nginx/conf.d/default.conf:ro"
                )
                depends_on = @(
                    "ai-ready-user-service-8081"
                    "ai-ready-user-service-8082"
                    "ai-ready-order-service-8083"
                    "ai-ready-inventory-service-8085"
                )
                networks = @("ai-ready-network")
                restart = "unless-stopped"
            }
        }
        networks = @{
            ai-ready-network = @{
                external = $true
                name = "ai-ready-test-network"
            }
        }
    }
    
    $composeContent | ConvertTo-Yaml | Out-File -FilePath $composeFile -Encoding UTF8
    Write-Host "  ✅ docker-compose.yml created" -ForegroundColor Green
} else {
    Write-Host "  ✅ docker-compose.yml exists" -ForegroundColor Green
}

# Step 4: Check if network exists
Write-Host "[4/6] Checking Docker network..." -ForegroundColor Yellow
try {
    $network = docker network ls --filter "name=ai-ready-test-network" --format "{{.Name}}" 2>$null
    if (-not $network) {
        Write-Host "  Creating ai-ready-test-network..." -ForegroundColor Yellow
        docker network create "ai-ready-test-network" 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✅ Network created" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️  Network already exists or creation skipped" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ✅ Network exists: $network" -ForegroundColor Green
    }
} catch {
    Write-Host "  ⚠️  Error checking network: $_" -ForegroundColor Yellow
}

# Step 5: Deploy or remove container
Write-Host "[5/6] $(
    if ($Remove) { "Removing" } else { "Deploying" }
) API Gateway container..." -ForegroundColor Yellow

if ($Remove) {
    # Remove existing container
    $existing = docker ps -a --filter "name=$containerName" --format "{{.Names}}" 2>$null
    if ($existing) {
        Write-Host " Stopping and removing container: $containerName" -ForegroundColor Yellow
        docker stop $containerName 2>$null | Out-Null
        docker rm $containerName 2>$null | Out-Null
        Write-Host "  ✅ Container removed" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Container not found" -ForegroundColor Yellow
    }
    exit 0
}

# Deploy new container
try {
    # Stop existing container if running
    $existing = docker ps --filter "name=$containerName" --format "{{.Names}}" 2>$null
    if ($existing) {
        Write-Host "  Stopping existing container..." -ForegroundColor Yellow
        docker stop $containerName 2>$null | Out-Null
        docker rm $containerName 2>$null | Out-Null
    }
    
    # Pull latest nginx image
    if (-not $NoBuild) {
        Write-Host "  Pulling nginx:alpine image..." -ForegroundColor Yellow
        docker pull $imageName 2>$null | Out-Null
    }
    
    # Run container
    Write-Host "  Starting API Gateway container..." -ForegroundColor Yellow
    docker run -d `
        --name $containerName `
        -p "${hostPort}:${containerPort}" `
        -v "${hostConfigPath}:/etc/nginx/conf.d/default.conf:ro" `
        --network ai-ready-test-network `
        --restart unless-stopped `
        $imageName 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ Container started successfully" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Failed to start container" -ForegroundColor Red
        Write-Host "  Error: $_" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  ❌ Error deploying container: $_" -ForegroundColor Red
    exit 1
}

# Step 6: Wait and verify
Write-Host "[6/6] Waiting for container to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# Check container status
$containerStatus = docker ps --filter "name=$containerName" --format "{{.Status}}" 2>$null
if ($containerStatus) {
    Write-Host "  ✅ Container running: $containerStatus" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Container may still be starting" -ForegroundColor Yellow
}

# Test endpoint
Write-Host ""
Write-Host "Testing API Gateway..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✅ API Gateway responding on port $hostPort" -ForegroundColor Green
        Write-Host "  Response: $($response.Content)" -ForegroundColor Gray
    } else {
        Write-Host "  ⚠️  API Gateway responded with status: $($response.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ⚠️  Could not connect to API Gateway: $_" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Gateway Deployment Complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "API Gateway Information:" -ForegroundColor Yellow
Write-Host "  URL: http://localhost:8080" -ForegroundColor White
Write-Host "  Health: http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "  Routes:" -ForegroundColor White
Write-Host "    - http://localhost:8080/api/users/*" -ForegroundColor Gray
Write-Host "    - http://localhost:8080/api/orders/*" -ForegroundColor Gray
Write-Host "    - http://localhost:8080/api/inventory/*" -ForegroundColor Gray
Write-Host "    - http://localhost:8080/api/monitoring/*" -ForegroundColor Gray
Write-Host ""
