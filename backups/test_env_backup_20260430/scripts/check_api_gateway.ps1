# API Gateway Health Check Script
# Tests API Gateway on port 8080 and verifies routing to all services

param(
    [switch]$Verbose = $false
)

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "API Gateway Health Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Testing API Gateway at http://localhost:8080" -ForegroundColor Yellow
Write-Host ""

# API Gateway Tests
$tests = @(
    @{ Name = "API Gateway Root"; Path = "/" }
    @{ Name = "API Gateway Health"; Path = "/actuator/health" }
    @{ Name = "Users Service (via Gateway)"; Path = "/api/users/health" }
    @{ Name = "Orders Service (via Gateway)"; Path = "/api/orders/health" }
    @{ Name = "Inventory Service (via Gateway)"; Path = "/api/inventory/health" }
    @{ Name = "Monitoring Service (via Gateway)"; Path = "/api/monitoring/health" }
)

$results = @()
$passed = 0
$failed = 0

foreach ($test in $tests) {
    $url = "http://localhost:8080" + $test.Path
    $startTime = Get-Date
    
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 10 -ErrorAction Stop
        $elapsed = (Get-Date) - $startTime
        $statusCode = $response.StatusCode
        
        if ($statusCode -eq 200) {
            $status = "✅ PASS"
            $color = "Green"
            $passed++
        } else {
            $status = "⚠️ HTTP $statusCode"
            $color = "Yellow"
            $failed++
        }
        
        if ($Verbose -or $statusCode -ne 200) {
            Write-Host "[$status] $($test.Name)" -ForegroundColor $color
            Write-Host "       URL: $url" -ForegroundColor Gray
            Write-Host "       Status: $statusCode, Time: $($elapsed.TotalSeconds.ToString('F2'))s" -ForegroundColor Gray
        } else {
            Write-Host "[$status] $($test.Name)" -ForegroundColor $color
        }
        
        $results += @{
            Test = $test.Name
            URL = $url
            Status = $status
            StatusCode = $statusCode
            TimeSeconds = $elapsed.TotalSeconds
            Passed = ($statusCode -eq 200)
        }
    } catch {
        $elapsed = (Get-Date) - $startTime
        $errorMessage = $_.Exception.Message -replace "`n|`r", " "
        
        Write-Host "[❌ FAIL] $($test.Name)" -ForegroundColor Red
        Write-Host "         URL: $url" -ForegroundColor Gray
        Write-Host "         Error: $errorMessage" -ForegroundColor Gray
        Write-Host "         Time: $($elapsed.TotalSeconds.ToString('F2'))s" -ForegroundColor Gray
        
        $results += @{
            Test = $test.Name
            URL = $url
            Status = "❌ FAIL"
            StatusCode = "Error"
            TimeSeconds = $elapsed.TotalSeconds
            Passed = $false
            ErrorMessage = $errorMessage
        }
        $failed++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total Tests: $($tests.Count)" -ForegroundColor White
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor $([char]0x1b) + "[31m"
Write-Host ""

if ($failed -eq 0) {
    Write-Host "✅ API Gateway Health Check PASSED" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️ API Gateway Health Check completed with warnings" -ForegroundColor Yellow
    Write-Host "   Some services may need retry." -ForegroundColor Gray
    exit 1
}
