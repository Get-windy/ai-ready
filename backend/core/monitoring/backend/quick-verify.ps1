# AI-Ready 监控告警模块快速验证脚本
Write-Host "AI-Ready 监控告警模块快速验证" -ForegroundColor Green
Write-Host "================================================"
Write-Host ""

# 1. 验证监控基础设施
Write-Host "1. 验证监控基础设施状态..." -ForegroundColor Yellow
Write-Host "----------------------------------------"

# 检查 Prometheus
try {
    $prometheusResponse = Invoke-RestMethod -Uri "http://localhost:9090/api/v1/status/runtimeinfo" -Method Get -ErrorAction SilentlyContinue
    Write-Host "  ✅ Prometheus 运行正常" -ForegroundColor Green
    Write-Host "    启动时间: $($prometheusResponse.data.startTime)"
    Write-Host "    Goroutine 数量: $($prometheusResponse.data.goroutineCount)"
} catch {
    Write-Host "  ❌ Prometheus 未运行或无法访问" -ForegroundColor Red
}

# 检查 Grafana
try {
    $grafanaResponse = Invoke-RestMethod -Uri "http://localhost:3000/api/health" -Method Get -ErrorAction SilentlyContinue
    Write-Host "  ✅ Grafana 运行正常" -ForegroundColor Green
    Write-Host "    版本: $($grafanaResponse.version)"
    Write-Host "    数据库状态: $($grafanaResponse.database)"
} catch {
    Write-Host "  ❌ Grafana 未运行或无法访问" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. 验证监控告警模块配置..." -ForegroundColor Yellow
Write-Host "----------------------------------------"

# 检查配置文件
$configDir = "I:\AI-Ready\monitoring\backend\config"
if (Test-Path $configDir) {
    Write-Host "  ✅ 配置目录存在" -ForegroundColor Green
    
    $configFiles = Get-ChildItem -Path $configDir -Filter *.yml -Recurse
    if ($configFiles.Count -gt 0) {
        Write-Host "    找到 $($configFiles.Count) 个配置文件:"
        foreach ($file in $configFiles) {
            Write-Host "      - $($file.Name)"
        }
    } else {
        Write-Host "    ⚠️  未找到配置文件" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ 配置目录不存在" -ForegroundColor Red
}

# 检查告警规则配置
$rulesDir = "I:\AI-Ready\monitoring\backend\config\alert-rules"
if (Test-Path $rulesDir) {
    $ruleFiles = Get-ChildItem -Path $rulesDir -Filter *.yaml -Recurse
    if ($ruleFiles.Count -gt 0) {
        Write-Host "  ✅ 告警规则目录存在" -ForegroundColor Green
        Write-Host "    找到 $($ruleFiles.Count) 个告警规则文件"
        
        # 显示规则文件内容摘要
        foreach ($file in $ruleFiles | Select-Object -First 3) {
            Write-Host "    - $($file.Name):"
            $content = Get-Content $file.FullName -TotalCount 10
            foreach ($line in $content) {
                if ($line -match "rule_name:|name:|description:") {
                    Write-Host "      $line"
                }
            }
        }
    } else {
        Write-Host "  ⚠️  告警规则目录存在但为空" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  告警规则目录不存在" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "3. 验证代码结构..." -ForegroundColor Yellow
Write-Host "----------------------------------------"

# 检查主要代码文件
$srcDir = "I:\AI-Ready\monitoring\backend\src\main\java\com\qizhilian\monitoring"
if (Test-Path $srcDir) {
    Write-Host "  ✅ 源代码目录存在" -ForegroundColor Green
    
    # 检查主要控制器
    $controllers = @(
        "AlertController.java",
        "MetricController.java"
    )
    
    foreach ($controller in $controllers) {
        $controllerPath = Join-Path $srcDir "controller" $controller
        if (Test-Path $controllerPath) {
            Write-Host "    ✅ $controller 存在" -ForegroundColor Green
        } else {
            Write-Host "    ❌ $controller 不存在" -ForegroundColor Red
        }
    }
    
    # 检查引擎
    $enginePath = Join-Path $srcDir "engine" "AlertRuleEngine.java"
    if (Test-Path $enginePath) {
        Write-Host "    ✅ AlertRuleEngine.java 存在" -ForegroundColor Green
    } else {
        Write-Host "    ❌ AlertRuleEngine.java 不存在" -ForegroundColor Red
    }
} else {
    Write-Host "  ❌ 源代码目录不存在" -ForegroundColor Red
}

Write-Host ""
Write-Host "4. 验证部署配置..." -ForegroundColor Yellow
Write-Host "----------------------------------------"

# 检查部署文件
$deployFiles = @(
    "docker-compose.yml",
    "docker-compose-simple.yml",
    "Dockerfile"
)

foreach ($file in $deployFiles) {
    $filePath = "I:\AI-Ready\monitoring\backend\$file"
    if (Test-Path $filePath) {
        $fileSize = (Get-Item $filePath).Length / 1KB
        Write-Host "    ✅ $file 存在 ($([math]::Round($fileSize, 2)) KB)" -ForegroundColor Green
    } else {
        Write-Host "    ❌ $file 不存在" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "5. 生成优化建议..." -ForegroundColor Yellow
Write-Host "----------------------------------------"

Write-Host "  1. 性能优化建议:" -ForegroundColor Cyan
Write-Host "     - 为监控告警服务配置合理的JVM参数"
Write-Host "     - 设置资源限制，避免资源过度消耗"
Write-Host "     - 优化数据库查询，添加索引"
Write-Host ""
Write-Host "  2. 告警规则优化:" -ForegroundColor Cyan
Write-Host "     - 设置适当的告警阈值"
Write-Host "     - 添加告警冷却时间"
Write-Host "     - 实现分级告警"
Write-Host ""
Write-Host "  3. 运维文档更新:" -ForegroundColor Cyan
Write-Host "     - 创建故障排查指南"
Write-Host "     - 编写告警规则配置文档"
Write-Host "     - 制定性能调优手册"

Write-Host ""
Write-Host "6. 验证结果汇总..." -ForegroundColor Magenta
Write-Host "----------------------------------------"

# 生成验证摘要
$summary = @{
    "监控基础设施" = if ($prometheusResponse -and $grafanaResponse) { "✅" } else { "⚠️" }
    "配置目录" = if (Test-Path $configDir) { "✅" } else { "❌" }
    "告警规则" = if (Test-Path $rulesDir -and (Get-ChildItem $rulesDir -Filter *.yaml).Count -gt 0) { "✅" } else { "⚠️" }
    "源代码结构" = if (Test-Path $srcDir) { "✅" } else { "❌" }
    "部署配置" = if (Test-Path "I:\AI-Ready\monitoring\backend\docker-compose.yml") { "✅" } else { "❌" }
}

foreach ($item in $summary.GetEnumerator()) {
    Write-Host "  $($item.Key): $($item.Value)" -ForegroundColor White
}

Write-Host ""
Write-Host "================================================"
Write-Host "快速验证完成！" -ForegroundColor Green
Write-Host ""
Write-Host "建议下一步操作:" -ForegroundColor Cyan
Write-Host "  1. 修复docker-compose.yml语法错误"
Write-Host "  2. 使用简化版配置启动监控告警服务"
Write-Host "  3. 执行API接口测试"
Write-Host "  4. 验证告警触发功能"

# 生成验证报告文件
$reportPath = "I:\AI-Ready\monitoring\backend\quick-validation-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
@"
AI-Ready 监控告警模块快速验证报告
生成时间: $(Get-Date)
验证人: 运维工程师 (devops-engineer)

验证结果汇总:
$(foreach ($item in $summary.GetEnumerator()) {
    "  $($item.Key): $($item.Value)"
})

详细验证结果:
1. 监控基础设施:
   - Prometheus: $(if ($prometheusResponse) { "运行正常" } else { "未运行" })
   - Grafana: $(if ($grafanaResponse) { "运行正常" } else { "未运行" })

2. 监控告警模块配置:
   - 配置目录: $(if (Test-Path $configDir) { "存在" } else { "不存在" })
   - 告警规则目录: $(if (Test-Path $rulesDir) { "存在" } else { "不存在" })
   - 规则文件数量: $(if (Test-Path $rulesDir) { (Get-ChildItem $rulesDir -Filter *.yaml).Count } else { "0" })

3. 源代码结构:
   - AlertController: $(if (Test-Path (Join-Path $srcDir "controller\AlertController.java")) { "存在" } else { "不存在" })
   - MetricController: $(if (Test-Path (Join-Path $srcDir "controller\MetricController.java")) { "存在" } else { "不存在" })
   - AlertRuleEngine: $(if (Test-Path (Join-Path $srcDir "engine\AlertRuleEngine.java")) { "存在" } else { "不存在" })

4. 部署配置:
   - docker-compose.yml: $(if (Test-Path "I:\AI-Ready\monitoring\backend\docker-compose.yml") { "存在" } else { "不存在" })
   - docker-compose-simple.yml: $(if (Test-Path "I:\AI-Ready\monitoring\backend\docker-compose-simple.yml") { "存在" } else { "不存在" })

优化建议:
  1. 性能优化:
     - 配置合理的JVM参数
     - 设置资源限制
     - 优化数据库查询

  2. 告警规则优化:
     - 设置适当的告警阈值
     - 添加告警冷却时间
     - 实现分级告警

  3. 运维文档:
     - 创建故障排查指南
     - 编写告警规则配置文档
     - 制定性能调优手册
"@ | Out-File -FilePath $reportPath -Encoding UTF8

Write-Host "验证报告已保存到: $reportPath" -ForegroundColor Green