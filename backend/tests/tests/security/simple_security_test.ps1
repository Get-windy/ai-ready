# 简单安全测试脚本
Write-Host "=== 用户管理模块安全测试开始 ===" -ForegroundColor Green

# 定义测试目标
$baseUrl = "http://localhost:8083"

# 测试1: 检查服务健康状态
Write-Host "`n测试1: 服务健康检查" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method GET
    Write-Host "  服务状态: $($health.status)" -ForegroundColor Green
    Write-Host "  数据库: $($health.components.db.status)" -ForegroundColor Yellow
    Write-Host "  Redis: $($health.components.redis.status)" -ForegroundColor Yellow
} catch {
    Write-Host "  健康检查失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 测试2: API文档检查
Write-Host "`n测试2: API文档检查" -ForegroundColor Cyan
try {
    $apiDocs = Invoke-RestMethod -Uri "$baseUrl/v3/api-docs" -Method GET
    Write-Host "  API文档: 可用" -ForegroundColor Green
    Write-Host "  OpenAPI版本: $($apiDocs.openapi)" -ForegroundColor Yellow
    Write-Host "  接口标签: $($apiDocs.tags.name -join ', ')" -ForegroundColor Yellow
} catch {
    Write-Host "  API文档: 不可用" -ForegroundColor Red
}

# 测试3: 登录测试
Write-Host "`n测试3: 登录认证测试" -ForegroundColor Cyan
try {
    $loginData = @{
        usernameOrEmail = "testuser"
        password = "Test123456"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body $loginData
    Write-Host "  登录成功: 是" -ForegroundColor Green
    Write-Host "  Token类型: $($response.tokenType)" -ForegroundColor Yellow
    Write-Host "  过期时间: $($response.expiresIn)秒" -ForegroundColor Yellow
} catch {
    Write-Host "  登录失败: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== 用户管理模块基础测试完成 ===" -ForegroundColor Green