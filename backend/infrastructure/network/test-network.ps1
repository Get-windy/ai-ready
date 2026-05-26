# 网络连通性测试脚本（PowerShell）
Write-Host "=== 网络连通性测试 ===" -ForegroundColor Cyan

# 测试网关
$gateways = @('10.0.0.1', '10.0.1.1', '10.0.2.1', '10.0.3.1')
Write-Host "`n1. 测试网关连通性:" -ForegroundColor Yellow

foreach ($gw in $gateways) {
    try {
        $ping = Test-Connection -ComputerName $gw -Count 2 -Quiet
        if ($ping) {
            Write-Host "  ✓ 网关 $gw 可达" -ForegroundColor Green
        } else {
            Write-Host "  ✗ 网关 $gw 不可达" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ✗ 网关 $gw 测试失败: $_" -ForegroundColor Red
    }
}

# 测试常用服务端口
Write-Host "`n2. 测试常用服务端口:" -ForegroundColor Yellow

$services = @(
    @{Name="本地Web服务"; Host="localhost"; Port=80},
    @{Name="本地SSH"; Host="localhost"; Port=22},
    @{Name="数据库服务"; Host="localhost"; Port=5432},
    @{Name="Redis服务"; Host="localhost"; Port=6379}
)

foreach ($service in $services) {
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $result = $tcpClient.BeginConnect($service.Host, $service.Port, $null, $null)
        $success = $result.AsyncWaitHandle.WaitOne(1000, $false)
        
        if ($success) {
            $tcpClient.EndConnect($result)
            Write-Host "  ✓ $($service.Name) ($($service.Host):$($service.Port)) 端口正常" -ForegroundColor Green
        } else {
            Write-Host "  ✗ $($service.Name) ($($service.Host):$($service.Port)) 端口异常" -ForegroundColor Yellow
        }
        $tcpClient.Close()
    } catch {
        Write-Host "  ✗ $($service.Name) ($($service.Host):$($service.Port)) 测试失败: $_" -ForegroundColor Red
    }
}

# 测试DNS解析
Write-Host "`n3. 测试DNS解析:" -ForegroundColor Yellow

$domains = @('google.com', 'baidu.com', 'localhost')
foreach ($domain in $domains) {
    try {
        $ip = [System.Net.Dns]::GetHostAddresses($domain) | Select-Object -First 1
        Write-Host "  ✓ 域名 $domain 解析成功 -> $($ip.IPAddressToString)" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ 域名 $domain 解析失败" -ForegroundColor Red
    }
}

# 网络信息收集
Write-Host "`n4. 网络信息收集:" -ForegroundColor Yellow

# 获取IP配置
$ipConfig = ipconfig
Write-Host "  IP配置信息:" -ForegroundColor Cyan
$ipConfig | Select-String -Pattern "IPv4|默认网关|DNS" | ForEach-Object {
    Write-Host "    $_" -ForegroundColor Gray
}

# 获取网络适配器信息
Write-Host "`n  网络适配器状态:" -ForegroundColor Cyan
try {
    Get-NetAdapter | Select-Object Name, Status, LinkSpeed | ForEach-Object {
        $statusColor = if ($_.Status -eq "Up") { "Green" } else { "Red" }
        Write-Host "    $($_.Name): $($_.Status) ($($_.LinkSpeed))" -ForegroundColor $statusColor
    }
} catch {
    Write-Host "  无法获取网络适配器信息: $_" -ForegroundColor Yellow
}

Write-Host "`n=== 网络测试完成 ===" -ForegroundColor Cyan