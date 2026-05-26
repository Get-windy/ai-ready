# 用户管理模块安全测试脚本
Write-Host "=== 用户管理模块安全测试开始 ===" -ForegroundColor Green

# 定义测试目标
$baseUrl = "http://localhost:8083"

# 测试1: 正常登录
Write-Host "`n测试1: 正常登录流程" -ForegroundColor Cyan
try {
    $loginData = @{
        usernameOrEmail = "testuser"
        password = "Test123456"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body $loginData
    Write-Host "  正常登录: 成功" -ForegroundColor Green
    Write-Host "  返回Token: $($response.accessToken)" -ForegroundColor Yellow
    Write-Host "  用户信息: $($response.user)" -ForegroundColor Yellow
} catch {
    Write-Host "  正常登录: 失败 - $($_.Exception.Message)" -ForegroundColor Red
}

# 测试2: 错误密码登录
Write-Host "`n测试2: 错误密码登录" -ForegroundColor Cyan
try {
    $loginData = @{
        usernameOrEmail = "testuser"
        password = "WrongPassword123"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body $loginData
    Write-Host "  错误密码登录: 成功 (可能存在安全问题)" -ForegroundColor Red
    Write-Host "  返回Token: $($response.accessToken)" -ForegroundColor Yellow
} catch {
    Write-Host "  错误密码登录: 失败 (正常行为) - $($_.Exception.Message)" -ForegroundColor Green
}

# 测试3: 使用Token访问受保护接口
Write-Host "`n测试3: Token验证测试" -ForegroundColor Cyan
try {
    # 先获取Token
    $loginData = @{
        usernameOrEmail = "testuser"
        password = "Test123456"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method POST -Headers @{"Content-Type"="application/json"} -Body $loginData
    $token = $loginResponse.accessToken
    
    # 使用Token访问用户列表
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $token"
    }
    
    $userList = Invoke-RestMethod -Uri "$baseUrl/api/v1/users" -Method GET -Headers $headers
    Write-Host "  Token验证: 成功" -ForegroundColor Green
    Write-Host "  获取用户列表: 成功" -ForegroundColor Green
} catch {
    Write-Host "  Token验证: 失败 - $($_.Exception.Message)" -ForegroundColor Red
}

# 测试4: 无Token访问受保护接口
Write-Host "`n测试4: 无Token访问测试" -ForegroundColor Cyan
try {
    $userList = Invoke-RestMethod -Uri "$baseUrl/api/v1/users" -Method GET -Headers @{"Content-Type"="application/json"}
    Write-Host "  无Token访问: 成功 (可能存在安全问题)" -ForegroundColor Red
} catch {
    Write-Host "  无Token访问: 失败 (正常行为) - $($_.Exception.Message)" -ForegroundColor Green
}

# 测试5: 无效Token访问
Write-Host "`n测试5: 无效Token访问测试" -ForegroundColor Cyan
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer invalid-token-12345"
    }
    
    $userList = Invoke-RestMethod -Uri "$baseUrl/api/v1/users" -Method GET -Headers $headers
    Write-Host "  无效Token访问: 成功 (可能存在安全问题)" -ForegroundColor Red
} catch {
    Write-Host "  无效Token访问: 失败 (正常行为) - $($_.Exception.Message)" -ForegroundColor Green
}

# 测试6: 检查API文档
Write-Host "`n测试6: API文档检查" -ForegroundColor Cyan
try {
    $apiDocs = Invoke-RestMethod -Uri "$baseUrl/v3/api-docs" -Method GET
    Write-Host "  API文档: 可用" -ForegroundColor Green
    Write-Host "  版本: $($apiDocs.openapi)" -ForegroundColor Yellow
    Write-Host "  接口数量: $(($apiDocs.paths.PSObject.Properties | Measure-Object).Count)" -ForegroundColor Yellow
} catch {
    Write-Host "  API文档: 不可用" -ForegroundColor Red
}

Write-Host "`n=== 用户管理模块安全测试完成 ===" -ForegroundColor Green