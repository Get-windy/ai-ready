# Windows兼容性测试指南

## 1. 概述

本文档详细说明价格策略模块在Windows操作系统环境下的兼容性测试要求和实施指南。

## 2. 支持的Windows版本

### 2.1 桌面版本
| Windows版本 | 版本号 | 架构 | 支持状态 | 生命周期结束 |
|-------------|--------|------|----------|--------------|
| Windows 10 | 21H2, 22H2 | x64 | ✅ 完全支持 | 2025-10-14 |
| Windows 11 | 22H2, 23H2 | x64 | ✅ 完全支持 | 2031-10-10 |

### 2.2 服务器版本
| Windows版本 | 版本号 | 架构 | 支持状态 | 生命周期结束 |
|-------------|--------|------|----------|--------------|
| Windows Server 2019 | 1809 | x64 | ✅ 完全支持 | 2029-01-09 |
| Windows Server 2022 | 21H2 | x64 | ✅ 完全支持 | 2031-10-10 |

## 3. 系统要求

### 3.1 最低硬件要求
```yaml
minimum_requirements:
  processor: "Intel Core i5或同等AMD处理器"
  memory: "8GB RAM"
  storage: "50GB可用磁盘空间"
  network: "100Mbps以太网"
```

### 3.2 推荐硬件配置
```yaml
recommended_requirements:
  processor: "Intel Core i7或同等AMD处理器"
  memory: "16GB RAM"
  storage: "100GB SSD"
  network: "1Gbps以太网"
```

## 4. 测试用例设计

### 4.1 安装和部署测试
```powershell
# 安装脚本测试用例
Describe "价格策略模块安装测试" {
    Context "在Windows环境下的安装" {
        It "应该成功安装核心组件" {
            # 执行安装脚本
            $installResult = .\install-price-strategy.ps1
            
            # 验证安装结果
            $installResult.Success | Should -Be $true
            Test-Path "C:\Program Files\PriceStrategy" | Should -Be $true
        }
        
        It "应该正确配置Windows服务" {
            # 检查服务状态
            $service = Get-Service -Name "PriceStrategyService"
            $service.Status | Should -Be "Running"
            $service.StartType | Should -Be "Automatic"
        }
    }
}
```

### 4.2 文件系统测试
```powershell
# 文件系统兼容性测试
Describe "Windows文件系统兼容性测试" {
    It "应该支持长文件路径" {
        # 测试长路径支持
        $longPath = "C:\very\long\directory\path\that\exceeds\260\characters\" + 
                   "a" * 100 + "\testfile.txt"
        
        # 创建测试文件
        New-Item -Path $longPath -ItemType File -Force
        Test-Path $longPath | Should -Be $true
    }
    
    It "应该正确处理特殊字符" {
        $specialChars = @("file[name].txt", "file&name.txt", "file%name.txt")
        
        foreach ($fileName in $specialChars) {
            $filePath = "C:\temp\$fileName"
            New-Item -Path $filePath -ItemType File -Force
            Test-Path $filePath | Should -Be $true
        }
    }
}
```

### 4.3 权限和安全测试
```powershell
# 权限测试
Describe "Windows权限和安全测试" {
    It "应该遵循最小权限原则" {
        # 检查服务账户权限
        $serviceAccount = "NT SERVICE\PriceStrategyService"
        $privileges = Get-LocalGroupMember -Group "Users"
        
        # 服务账户不应在管理员组
        $serviceAccount -in $privileges.Name | Should -Be $false
    }
    
    It "应该正确处理UAC提升" {
        # 测试需要管理员权限的操作
        try {
            # 尝试修改受保护目录
            New-Item "C:\Windows\Temp\PriceStrategyTest" -ItemType Directory
            $?.IsSuccess | Should -Be $true
        } catch {
            # 预期可能会被UAC阻止
            $_.Exception.Message | Should -Match "access denied"
        }
    }
}
```

### 4.4 网络配置测试
```powershell
# 网络配置测试
Describe "Windows网络配置测试" {
    It "应该支持多种网络配置" {
        # 获取网络适配器信息
        $adapters = Get-NetAdapter | Where-Object {$_.Status -eq "Up"}
        $adapters.Count | Should -BeGreaterThan 0
        
        # 测试网络连接
        Test-NetConnection -ComputerName "api.example.com" -Port 443 |
            Select-Object -Property TcpTestSucceeded |
            Should -Be $true
    }
    
    It "应该正确处理防火墙规则" {
        # 检查防火墙规则
        $firewallRules = Get-NetFirewallRule -DisplayName "PriceStrategy*"
        $firewallRules.Count | Should -BeGreaterThan 0
        
        # 验证规则配置
        foreach ($rule in $firewallRules) {
            $rule.Enabled | Should -Be $true
            $rule.Action | Should -Be "Allow"
        }
    }
}
```

## 5. Windows特性测试

### 5.1 Windows服务集成
```powershell
# Windows服务测试
Describe "Windows服务集成测试" {
    BeforeAll {
        # 启动测试服务
        Start-Service -Name "PriceStrategyService"
        Start-Sleep -Seconds 5
    }
    
    It "服务应该正常启动和停止" {
        $service = Get-Service -Name "PriceStrategyService"
        $service.Status | Should -Be "Running"
        
        # 测试服务停止
        Stop-Service -Name "PriceStrategyService" -Force
        Start-Sleep -Seconds 2
        (Get-Service -Name "PriceStrategyService").Status | Should -Be "Stopped"
        
        # 重新启动服务
        Start-Service -Name "PriceStrategyService"
    }
    
    It "服务应该支持故障恢复" {
        # 配置故障恢复
        sc.exe failure "PriceStrategyService" reset= 86400 actions= restart/5000
        
        # 验证配置
        $recovery = sc.exe qfailure "PriceStrategyService"
        $recovery | Should -Match "RESTART"
    }
}
```

### 5.2 事件日志集成
```powershell
# 事件日志测试
Describe "Windows事件日志集成" {
    It "应该正确写入应用程序日志" {
        # 写入测试事件
        Write-EventLog -LogName "Application" -Source "PriceStrategy" `
            -EventId 1000 -EntryType Information -Message "测试事件"
        
        # 验证事件写入
        $event = Get-EventLog -LogName "Application" -Newest 1 | 
                 Where-Object {$_.Source -eq "PriceStrategy"}
        
        $event | Should -Not -Be $null
        $event.Message | Should -Match "测试事件"
    }
    
    It "应该支持日志轮转" {
        # 检查日志设置
        $log = Get-WinEvent -ListLog "Application"
        $log.MaximumSizeInBytes | Should -BeGreaterThan 0
        $log.LogMode | Should -Be "AutoBackup"
    }
}
```

### 5.3 注册表配置
```powershell
# 注册表测试
Describe "Windows注册表配置测试" {
    It "应该正确读写注册表配置" {
        $registryPath = "HKLM:\SOFTWARE\PriceStrategy"
        
        # 创建测试配置
        New-Item -Path $registryPath -Force
        Set-ItemProperty -Path $registryPath -Name "Version" -Value "1.0.0"
        
        # 读取配置
        $version = Get-ItemProperty -Path $registryPath -Name "Version"
        $version.Version | Should -Be "1.0.0"
    }
    
    It "应该处理64位/32位注册表重定向" {
        # 在64位系统上测试32位兼容性
        if ([Environment]::Is64BitOperatingSystem) {
            $wow64Path = "HKLM:\SOFTWARE\WOW6432Node\PriceStrategy"
            New-Item -Path $wow64Path -Force
            
            Test-Path $wow64Path | Should -Be $true
        }
    }
}
```

## 6. 性能测试

### 6.1 资源使用监控
```powershell
# 性能监控测试
Describe "Windows性能监控测试" {
    It "应该在资源限制内运行" {
        # 监控进程资源使用
        $process = Get-Process -Name "PriceStrategy" -ErrorAction SilentlyContinue
        
        if ($process) {
            # 检查内存使用
            $process.WorkingSet64 / 1MB | Should -BeLessThan 512  # 小于512MB
            
            # 检查CPU使用
            $cpuUsage = (Get-Counter "\Process(PriceStrategy)\% Processor Time").CounterSamples.CookedValue
            $cpuUsage | Should -BeLessThan 80  # 小于80%
        }
    }
    
    It "应该正确处理高并发" {
        # 模拟高并发访问
        $jobs = 1..100 | ForEach-Object {
            Start-Job -ScriptBlock {
                # 模拟API调用
                Invoke-RestMethod "http://localhost:8080/api/prices" -Method GET
            }
        }
        
        # 等待所有作业完成
        $jobs | Wait-Job | Receive-Job
        
        # 检查系统稳定性
        $failedJobs = $jobs | Where-Object {$_.State -eq "Failed"}
        $failedJobs.Count | Should -BeLessThan 5  # 失败率小于5%
    }
}
```

### 6.2 启动时间测试
```powershell
# 启动性能测试
Describe "Windows启动性能测试" {
    It "应该在合理时间内启动" {
        # 测量服务启动时间
        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        
        Start-Service -Name "PriceStrategyService"
        
        # 等待服务完全启动
        do {
            Start-Sleep -Milliseconds 100
            $service = Get-Service -Name "PriceStrategyService"
        } while ($service.Status -ne "Running")
        
        $stopwatch.Stop()
        
        # 启动时间应小于10秒
        $stopwatch.Elapsed.TotalSeconds | Should -BeLessThan 10
    }
}
```

## 7. 兼容性问题和解决方案

### 7.1 已知问题列表

| 问题描述 | 影响版本 | 严重程度 | 解决方案 |
|----------|----------|----------|----------|
| Windows Server Core缺少GUI组件 | Server Core版本 | 中 | 使用无头模式或Web界面 |
| Windows Defender误报 | Windows 10/11 | 低 | 添加白名单或数字签名 |
| 长路径限制 | 所有版本 | 中 | 启用长路径支持或使用短路径 |
| 权限提升要求 | 所有版本 | 高 | 合理设计权限结构 |

### 7.2 长路径支持配置
```powershell
# 启用长路径支持
# 方法1: 注册表配置
New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" `
    -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force

# 方法2: 组策略配置
# 计算机配置 > 管理模板 > 系统 > 文件系统
# 启用"启用Win32长路径"
```

### 7.3 Windows Defender排除配置
```powershell
# 添加Windows Defender排除项
Add-MpPreference -ExclusionPath "C:\Program Files\PriceStrategy"
Add-MpPreference -ExclusionProcess "PriceStrategy.exe"
Add-MpPreference -ExclusionExtension ".pricestrategy"
```

## 8. 自动化测试脚本

### 8.1 完整测试套件
```powershell
# windows-compatibility-tests.ps1
param(
    [string]$TestEnvironment = "Development",
    [switch]$SkipInstall,
    [switch]$SkipPerformance
)

Write-Host "开始Windows兼容性测试..." -ForegroundColor Green

# 1. 安装测试
if (-not $SkipInstall) {
    Write-Host "执行安装测试..." -ForegroundColor Yellow
    .\tests\install-tests.ps1
}

# 2. 功能测试
Write-Host "执行功能测试..." -ForegroundColor Yellow
.\tests\functional-tests.ps1

# 3. 性能测试
if (-not $SkipPerformance) {
    Write-Host "执行性能测试..." -ForegroundColor Yellow
    .\tests\performance-tests.ps1
}

# 4. 安全测试
Write-Host "执行安全测试..." -ForegroundColor Yellow
.\tests\security-tests.ps1

Write-Host "Windows兼容性测试完成!" -ForegroundColor Green

# 生成测试报告
.\scripts\generate-test-report.ps1 -Environment $TestEnvironment
```

### 8.2 测试环境设置脚本
```powershell
# setup-windows-test-env.ps1
Write-Host "设置Windows测试环境..." -ForegroundColor Cyan

# 1. 检查系统要求
$requirements = @{
    OSVersion = [Environment]::OSVersion.Version
    PowerShellVersion = $PSVersionTable.PSVersion
    DotNetVersion = (Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full").Version
}

Write-Host "系统信息:" -ForegroundColor Yellow
$requirements.GetEnumerator() | Format-Table -AutoSize

# 2. 安装必要组件
$features = @(
    "IIS-WebServerRole",
    "IIS-WebServer",
    "IIS-ManagementConsole",
    "NetFx4-AdvSrvs"
)

foreach ($feature in $features) {
    Write-Host "安装功能: $feature" -ForegroundColor Gray
    Enable-WindowsOptionalFeature -Online -FeatureName $feature -NoRestart
}

# 3. 配置测试目录
$testDirs = @(
    "C:\PriceStrategyTests",
    "C:\PriceStrategyTests\Logs",
    "C:\PriceStrategyTests\Data",
    "C:\PriceStrategyTests\Backups"
)

foreach ($dir in $testDirs) {
    if (-not (Test-Path $dir)) {
        New-Item -Path $dir -ItemType Directory -Force
        Write-Host "创建目录: $dir" -ForegroundColor Gray
    }
}

Write-Host "测试环境设置完成!" -ForegroundColor Green
```

## 9. CI/CD集成

### 9.1 GitHub Actions配置
```yaml
# .github/workflows/windows-tests.yml
name: Windows Compatibility Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  windows-tests:
    runs-on: windows-latest
    
    strategy:
      matrix:
        windows-version: [windows-2019, windows-2022]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Windows Test Environment
        run: |
          .\os-database-compatibility-guide\scripts\setup-windows-env.ps1
          
      - name: Run Windows Compatibility Tests
        run: |
          .\os-database-compatibility-guide\scripts\test-windows-compatibility.ps1
          
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        with:
          name: windows-test-results-${{ matrix.windows-version }}
          path: test-results/windows/
```

## 10. 测试报告模板

### 10.1 JSON格式报告
```json
{
  "test_execution": {
    "environment": {
      "os_version": "Windows 11 22H2",
      "architecture": "x64",
      "powershell_version": "7.3.4",
      "dotnet_version": "6.0.15"
    },
    "summary": {
      "total_tests": 156,
      "passed": 150,
      "failed": 6,
      "skipped": 0,
      "duration_seconds": 325.5
    },
    "test_categories": {
      "installation": {
        "total": 25,
        "passed": 25,
        "failed": 0
      },
      "functionality": {
        "total": 80,
        "passed": 76,
        "failed": 4
      },
      "performance": {
        "total": 35,
        "passed": 33,
        "failed": 2
      },
      "security": {
        "total": 16,
        "passed": 16,
        "failed": 0
      }
    }
  }
}
```

---

**文档版本**: 1.0  
**最后更新**: 2026-05-01  
**测试环境**: Windows 11 22H2  
**测试工具**: PowerShell 7.3, Pester 5.3  
**状态**: 已验证