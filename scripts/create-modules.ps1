# 智企连·AI-Ready - 创建缺失模块骨架
# 脚本功能：批量创建ERP/CRM模块的骨架代码

$baseDir = "I:\AI-Ready"
$modules = @(
    @{Name="erp-stock"; Package="stock"; Description="库存管理模块"},
    @{Name="erp-account"; Package="account"; Description="财务管理模块"},
    @{Name="erp-warehouse"; Package="warehouse"; Description="仓储管理模块"},
    @{Name="crm-opportunity"; Package="opportunity"; Description="商机管理模块"},
    @{Name="crm-customer"; Package="customer"; Description="客户管理模块"},
    @{Name="crm-activity"; Package="activity"; Description="活动记录模块"}
)

foreach ($module in $modules) {
    $dir = "$baseDir\$($module.Name)"
    $packagePath = $module.Package -replace '-', '/'
    
    # 创建目录结构
    New-Item -ItemType Directory -Path "$dir/src/main/java/cn/aiedge/$($module.Package)" -Force | Out-Null
    New-Item -ItemType Directory -Path "$dir/src/main/resources" -Force | Out-Null
    New-Item -ItemType Directory -Path "$dir/src/test/java/cn/aiedge/$($module.Package)" -Force | Out-Null
    
    Write-Host "创建模块: $($module.Name) - $($module.Description)" -ForegroundColor Green
}

Write-Host "`n所有模块骨架已创建完成！`n" -ForegroundColor Yellow
Write-Host "下一步:" -ForegroundColor Cyan
Write-Host "1. 检查 pom.xml 模板" -ForegroundColor White
Write-Host "2. 编写实体类（参考现有模块）" -ForegroundColor White
Write-Host "3. 编写Service/Controller类" -ForegroundColor White
Write-Host "4. 数据库表初始化" -ForegroundColor White
