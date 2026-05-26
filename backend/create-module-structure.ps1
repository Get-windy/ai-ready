# PowerShell脚本：为ERP模块创建标准目录结构
# 基于ERP_ARCHITECTURE_DESIGN.md规范
# 作者：devops-engineer
# 创建时间：2026-05-05

Write-Host "开始创建ERP模块标准化目录结构..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

# 模块列表
$modules = @("batch", "customer", "expense", "finance", "inventory", "invoice", "metrics", "monitor", "order", "purchase", "sales", "supplier")

# 基础目录
$basePath = "I:\AI-Ready\backend\erp-modules"

foreach ($module in $modules) {
    $modulePath = Join-Path $basePath $module
    Write-Host "处理模块: $module" -ForegroundColor Yellow
    
    # 检查模块目录是否存在
    if (Test-Path $modulePath) {
        Write-Host "  ✓ 模块目录已存在: $module" -ForegroundColor Green
        
        # 创建标准目录结构
        $dirs = @(
            "src/main/java/cn/aiedge/erp/$module/controller",
            "src/main/java/cn/aiedge/erp/$module/service", 
            "src/main/java/cn/aiedge/erp/$module/repository",
            "src/main/java/cn/aiedge/erp/$module/model",
            "src/main/java/cn/aiedge/erp/$module/config",
            "src/test/java/cn/aiedge/erp/$module",
            "docs"
        )
        
        foreach ($dir in $dirs) {
            $fullDir = Join-Path $modulePath $dir
            if (!(Test-Path $fullDir)) {
                New-Item -ItemType Directory -Path $fullDir -Force | Out-Null
                Write-Host "  + 创建目录: $dir" -ForegroundColor Cyan
            } else {
                Write-Host "  ✓ 目录已存在: $dir" -ForegroundColor Gray
            }
        }
        
        # 创建基础文件
        $pomPath = Join-Path $modulePath "pom.xml"
        if (!(Test-Path $pomPath)) {
            $pomContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>cn.aiedge.erp</groupId>
        <artifactId>erp-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    
    <artifactId>erp-$module</artifactId>
    <name>ERP $module Module</name>
    <description>ERP $module Management Module</description>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- Common Dependencies -->
        <dependency>
            <groupId>cn.aiedge.erp</groupId>
            <artifactId>erp-common</artifactId>
            <version>\${project.version}</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
"@
            Set-Content -Path $pomPath -Value $pomContent -Encoding UTF8
            Write-Host "  + 创建pom.xml" -ForegroundColor Cyan
        } else {
            Write-Host "  ✓ pom.xml已存在" -ForegroundColor Gray
        }
        
        # 创建README.md
        $readmePath = Join-Path $modulePath "README.md"
        if (!(Test-Path $readmePath)) {
            $readmeContent = @"
# ERP $module Module

## 概述
$module管理模块，负责ERP系统中的$module相关业务功能。

## 功能特性
- 功能1：待补充
- 功能2：待补充
- 功能3：待补充

## 技术栈
- Java 17
- Spring Boot 3.2.x
- PostgreSQL 16
- Maven 3.9+

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.9+
- PostgreSQL 16+

### 2. 构建项目
```bash
mvn clean package
```

### 3. 运行项目
```bash
mvn spring-boot:run
```

### 4. API文档
启动后访问：http://localhost:8080/swagger-ui.html

## API规范
- 基础路径：/api/erp/v1/$module
- 包结构：cn.aiedge.erp.$module

## 数据库设计
待补充

## 部署说明
待补充

## 维护者
- 模块负责人：待分配
- 技术支持：devops-engineer
"@
            Set-Content -Path $readmePath -Value $readmeContent -Encoding UTF8
            Write-Host "  + 创建README.md" -ForegroundColor Cyan
        } else {
            Write-Host "  ✓ README.md已存在" -ForegroundColor Gray
        }
        
        Write-Host "  ✓ 模块 $module 结构创建完成" -ForegroundColor Green
        
    } else {
        Write-Host "  ✗ 模块目录不存在: $module" -ForegroundColor Red
    }
    
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ERP模块标准化目录结构创建完成！" -ForegroundColor Green
Write-Host "总计处理模块: $($modules.Count) 个" -ForegroundColor Yellow