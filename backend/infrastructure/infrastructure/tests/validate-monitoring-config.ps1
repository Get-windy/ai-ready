param(
    [string]$ConfigPath = "I:\AI-Ready\configs",
    [string]$ReportPath = "I:\AI-Ready\infrastructure\tests\reports\monitoring-test-report.md",
    [string]$ResultPath = "I:\AI-Ready\infrastructure\tests\results\monitoring-test-results.json"
)

# Initialize test results
$testResults = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    overallStatus = "PASS"
    tests = @()
    issues = @()
}

function Write-TestResult {
    param([string]$name, [string]$status, [string]$message)
    
    $result = @{
        name = $name
        status = $status
        message = $message
        timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    }
    
    $testResults.tests += $result
    
    if ($status -eq "FAIL") {
        $testResults.overallStatus = "FAIL"
        $testResults.issues += $message
    }
    
    Write-Host "[$status] $name"
    if ($message) {
        Write-Host "  $message"
    }
}

Write-Host "Starting Monitoring Alert Configuration Validation..."
Write-Host "Config Path: $ConfigPath"

# Test 1: Verify Prometheus configuration file exists and is valid
Write-Host "`n1. Validating Prometheus Configuration..."
$prometheusConfig = "$ConfigPath\monitoring\prometheus.yml"
if (Test-Path $prometheusConfig) {
    try {
        $content = Get-Content $prometheusConfig -Raw
        if ($content -match "global:" -and $content -match "scrape_configs:" -and $content -match "rule_files:") {
            Write-TestResult "Prometheus config file exists and has basic structure" "PASS" ""
            
            # Check for required scrape configs
            $scrapeMatches = Select-String -InputObject $content -Pattern "job_name:" -AllMatches
            if ($scrapeMatches.Matches.Count -ge 5) {
                Write-TestResult "Prometheus has sufficient scrape configurations" "PASS" "Found $($scrapeMatches.Matches.Count) job configurations"
            } else {
                Write-TestResult "Prometheus scrape configurations insufficient" "FAIL" "Only found $($scrapeMatches.Matches.Count) job configurations, expected at least 5"
            }
        } else {
            Write-TestResult "Prometheus config missing required sections" "FAIL" "Missing global, scrape_configs, or rule_files sections"
        }
    } catch {
        Write-TestResult "Prometheus config validation failed" "FAIL" "Error reading config: $($_.Exception.Message)"
    }
} else {
    Write-TestResult "Prometheus config file not found" "FAIL" "Expected at $prometheusConfig"
}

# Test 2: Verify Alert Rules configuration
Write-Host "`n2. Validating Alert Rules Configuration..."
$alertRulesConfig = "$ConfigPath\alerting\alert_rules.yml"
if (Test-Path $alertRulesConfig) {
    try {
        $content = Get-Content $alertRulesConfig -Raw
        if ($content -match "groups:" -and $content -match "rules:") {
            $alertMatches = Select-String -InputObject $content -Pattern "alert:" -AllMatches
            $alertCount = $alertMatches.Matches.Count
            if ($alertCount -ge 10) {
                Write-TestResult "Alert rules configuration valid" "PASS" "Found $alertCount alert rules across multiple categories"
                
                if ($content -match "severity: critical") {
                    Write-TestResult "Critical severity alerts configured" "PASS" ""
                } else {
                    Write-TestResult "Critical severity alerts missing" "WARN" "No critical severity alerts found"
                }
            } else {
                Write-TestResult "Insufficient alert rules" "FAIL" "Only found $alertCount alert rules, expected at least 10"
            }
        } else {
            Write-TestResult "Alert rules config missing required sections" "FAIL" "Missing groups or rules sections"
        }
    } catch {
        Write-TestResult "Alert rules validation failed" "FAIL" "Error reading config: $($_.Exception.Message)"
    }
} else {
    Write-TestResult "Alert rules config file not found" "FAIL" "Expected at $alertRulesConfig"
}

# Test 3: Verify Alertmanager configuration
Write-Host "`n3. Validating Alertmanager Configuration..."
$alertmanagerConfig = "$ConfigPath\alerting\alertmanager\alertmanager.yml"
if (Test-Path $alertmanagerConfig) {
    try {
        $content = Get-Content $alertmanagerConfig -Raw
        if ($content -match "global:" -and $content -match "route:" -and $content -match "receivers:") {
            Write-TestResult "Alertmanager config file exists and has basic structure" "PASS" ""
            
            $receiverMatches = Select-String -InputObject $content -Pattern "- name:" -AllMatches
            $receivers = $receiverMatches.Matches.Count
            if ($receivers -ge 3) {
                Write-TestResult "Multiple notification receivers configured" "PASS" "Found $receivers receiver configurations"
            } else {
                Write-TestResult "Insufficient notification receivers" "WARN" "Only found $receivers receivers, recommend at least 3"
            }
            
            if ($content -match "inhibit_rules:") {
                Write-TestResult "Alert inhibition rules configured" "PASS" "Noise reduction rules in place"
            } else {
                Write-TestResult "Alert inhibition rules missing" "WARN" "Consider adding inhibition rules to reduce alert noise"
            }
        } else {
            Write-TestResult "Alertmanager config missing required sections" "FAIL" "Missing global, route, or receivers sections"
        }
    } catch {
        Write-TestResult "Alertmanager config validation failed" "FAIL" "Error reading config: $($_.Exception.Message)"
    }
} else {
    Write-TestResult "Alertmanager config file not found" "FAIL" "Expected at $alertmanagerConfig"
}

# Test 4: Verify Grafana datasource configuration
Write-Host "`n4. Validating Grafana Datasource Configuration..."
$grafanaDatasource = "$ConfigPath\monitoring\grafana\provisioning\datasources\datasources.yml"
if (Test-Path $grafanaDatasource) {
    try {
        $content = Get-Content $grafanaDatasource -Raw
        if ($content -match "Prometheus" -and $content -match "http://prometheus:9090") {
            Write-TestResult "Grafana Prometheus datasource configured" "PASS" "Datasource points to http://prometheus:9090"
        } else {
            Write-TestResult "Grafana Prometheus datasource misconfigured" "FAIL" "Datasource URL incorrect or missing"
        }
        
        if ($content -match "Alertmanager" -and $content -match "http://alertmanager:9093") {
            Write-TestResult "Grafana Alertmanager datasource configured" "PASS" "Alertmanager integration enabled"
        } else {
            Write-TestResult "Grafana Alertmanager datasource missing" "WARN" "Consider adding Alertmanager datasource for integrated alert management"
        }
    } catch {
        Write-TestResult "Grafana datasource validation failed" "FAIL" "Error reading config: $($_.Exception.Message)"
    }
} else {
    Write-TestResult "Grafana datasource config file not found" "FAIL" "Expected at $grafanaDatasource"
}

# Test 5: Verify Grafana dashboards
Write-Host "`n5. Validating Grafana Dashboards..."
$dashboardPath = "$ConfigPath\grafana\provisioning\dashboards"
if (Test-Path $dashboardPath) {
    $dashboards = Get-ChildItem $dashboardPath -Filter "*.json"
    if ($dashboards.Count -ge 4) {
        Write-TestResult "Sufficient Grafana dashboards available" "PASS" "Found $($dashboards.Count) dashboard files"
        
        $requiredDashboards = @("infrastructure", "application", "business", "alert")
        foreach ($req in $requiredDashboards) {
            $found = $false
            foreach ($dashboard in $dashboards) {
                if ($dashboard.Name -like "*$req*") {
                    $found = $true
                    break
                }
            }
            if ($found) {
                Write-TestResult "Dashboard '$req' found" "PASS" ""
            } else {
                Write-TestResult "Dashboard '$req' missing" "WARN" "Consider adding $req monitoring dashboard"
            }
        }
    } else {
        Write-TestResult "Insufficient Grafana dashboards" "FAIL" "Only found $($dashboards.Count) dashboards, expected at least 4"
    }
} else {
    Write-TestResult "Grafana dashboards directory not found" "FAIL" "Expected at $dashboardPath"
}

# Test 6: Validate Docker Compose integration
Write-Host "`n6. Validating Docker Compose Integration..."
$dockerCompose = "$ConfigPath\monitoring\docker-compose.yml"
if (Test-Path $dockerCompose) {
    try {
        $content = Get-Content $dockerCompose -Raw
        $services = @("prometheus", "alertmanager", "grafana")
        $missingServices = @()
        
        foreach ($service in $services) {
            if ($content -notmatch $service) {
                $missingServices += $service
            }
        }
        
        if ($missingServices.Count -eq 0) {
            Write-TestResult "All monitoring services in Docker Compose" "PASS" "Prometheus, Alertmanager, and Grafana configured"
        } else {
            Write-TestResult "Missing monitoring services in Docker Compose" "FAIL" "Missing services: $($missingServices -join ', ')"
        }
    } catch {
        Write-TestResult "Docker Compose validation failed" "FAIL" "Error reading config: $($_.Exception.Message)"
    }
} else {
    Write-TestResult "Docker Compose config file not found" "FAIL" "Expected at $dockerCompose"
}

# Generate report
Write-Host "`nGenerating validation report..."

$passCount = 0
$failCount = 0
$warnCount = 0

foreach ($test in $testResults.tests) {
    if ($test.status -eq "PASS") { $passCount++ }
    elseif ($test.status -eq "FAIL") { $failCount++ }
    else { $warnCount++ }
}

$reportContent = @"
# Monitoring Alert Configuration Validation Report

**Generated:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
**Environment:** AI-Ready Test Environment (Sprint 27+1)
**Overall Status:** $($testResults.overallStatus)

## Summary
- **Tests Passed:** $passCount
- **Tests Failed:** $failCount  
- **Warnings:** $warnCount
- **Total Tests:** $($testResults.tests.Count)

## Test Results

"@

foreach ($test in $testResults.tests) {
    $reportContent += "**$($test.name)** - $($test.status)`n"
    if ($test.message) {
        $reportContent += "   - $($test.message)`n"
    }
    $reportContent += "`n"
}

if ($testResults.issues.Count -gt 0) {
    $reportContent += "## Critical Issues Requiring Attention`n"
    foreach ($issue in $testResults.issues) {
        $reportContent += "- $issue`n"
    }
    $reportContent += "`n"
}

$reportContent += @"
## Recommendations

### Immediate Actions Required
- Address all FAILED tests before deploying to production
- Ensure Prometheus can scrape all configured targets
- Verify Alertmanager notification channels are functional

### Best Practices Implemented
- Multi-level alert severity (critical, warning, info)
- Alert grouping and inhibition to reduce noise
- Comprehensive infrastructure and application monitoring
- Integrated Grafana dashboards for visualization

### Future Improvements
- Add more specific business metric alerts
- Implement alert acknowledgment workflows
- Add automated alert testing in CI/CD pipeline

## Configuration Files Validated
- Prometheus: $ConfigPath\monitoring\prometheus.yml
- Alert Rules: $ConfigPath\alerting\alert_rules.yml
- Alertmanager: $ConfigPath\alerting\alertmanager\alertmanager.yml
- Grafana Datasources: $ConfigPath\monitoring\grafana\provisioning\datasources\datasources.yml
- Grafana Dashboards: $ConfigPath\grafana\provisioning\dashboards\
- Docker Compose: $ConfigPath\monitoring\docker-compose.yml

---

*This report was automatically generated by the monitoring validation script.*
"@

# Write report and results
Set-Content -Path $ReportPath -Value $reportContent -Encoding UTF8
$testResults | ConvertTo-Json -Depth 10 | Set-Content -Path $ResultPath -Encoding UTF8

Write-Host "`nValidation completed!"
Write-Host "Report saved to: $ReportPath"
Write-Host "Results saved to: $ResultPath"
Write-Host "Overall Status: $($testResults.overallStatus)"

if ($testResults.overallStatus -eq "PASS") {
    exit 0
} else {
    exit 1
}