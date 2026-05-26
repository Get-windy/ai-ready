# AI-API服务重建脚本 (PowerShell版本)
# 用于修复JAR manifest问题并重新启动服务

Write-Host "=== AI-API服务重建脚本 ===" -ForegroundColor Green
Write-Host "开始时间: $(Get-Date)" -ForegroundColor Cyan
Write-Host ""

# 1. 停止并删除现有容器
Write-Host "1. 停止并删除现有容器..." -ForegroundColor Yellow
docker stop ai-ready-api 2>$null
docker rm ai-ready-api 2>$null
Write-Host "   完成" -ForegroundColor Green

# 2. 构建项目
Write-Host "2. 构建core-api项目..." -ForegroundColor Yellow
Set-Location I:\AI-Ready\backend\core\api\core-api
mvn clean package -DskipTests
Write-Host "   构建完成" -ForegroundColor Green

# 3. 检查JAR文件
Write-Host "3. 检查JAR文件..." -ForegroundColor Yellow
$jarFile = "target\core-api-1.0.0-SNAPSHOT.jar"
if (Test-Path $jarFile) {
    Write-Host "   JAR文件存在: $jarFile" -ForegroundColor Green
    # 检查JAR文件是否有主清单属性
    Write-Host "   检查JAR主清单属性..." -ForegroundColor Cyan
    try {
        $output = java -jar $jarFile --version 2>&1
        $output | Select-Object -First 5
        Write-Host "   JAR文件检查完成" -ForegroundColor Green
    } catch {
        Write-Host "   JAR文件检查失败: $_" -ForegroundColor Red
    }
} else {
    Write-Host "   错误: JAR文件不存在" -ForegroundColor Red
    exit 1
}

# 4. 构建Docker镜像
Write-Host "4. 构建Docker镜像..." -ForegroundColor Yellow
docker build -t ai-ready:latest -f Dockerfile.optimized .
Write-Host "   Docker镜像构建完成" -ForegroundColor Green

# 5. 启动容器
Write-Host "5. 启动ai-ready-api容器..." -ForegroundColor Yellow
docker run -d `
    --name ai-ready-api `
    --network bridge `
    -p 8080:8080 `
    -e SPRING_PROFILES_ACTIVE=test `
    -e JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" `
    ai-ready:latest
Write-Host "   容器已启动" -ForegroundColor Green

# 6. 等待服务启动
Write-Host "6. 等待服务启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 7. 检查服务状态
Write-Host "7. 检查服务状态..." -ForegroundColor Yellow
docker ps | Select-String "ai-ready-api"
Write-Host ""
Write-Host "容器日志:" -ForegroundColor Cyan
docker logs ai-ready-api --tail 20

# 8. 健康检查
Write-Host "8. 执行健康检查..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -ErrorAction Stop
    Write-Host "   服务健康检查通过" -ForegroundColor Green
} catch {
    Write-Host "   服务健康检查失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 重建完成 ===" -ForegroundColor Green
Write-Host "完成时间: $(Get-Date)" -ForegroundColor Cyan