# Spring Cloud LoadBalancer 依赖问题修复方案

## 问题描述

devops-engineer在部署Spring Boot应用服务时遇到以下错误：

```
NoClassDefFoundError: org.springframework.beans.factory.aot.BeanRegistrationAotProcessor
```

## 根本原因分析

### 1. 版本不兼容
- **父pom.xml**: Spring Boot 2.7.18
- **core-api模块**: Spring Cloud 2023.0.3 (需要Spring Boot 3.x)
- **core-api模块**: sa-token-spring-boot3-starter (为Spring Boot 3设计)

### 2. 依赖冲突
- Spring Cloud 2023.x 需要 Spring Boot 3.x
- sa-token-spring-boot3-starter 需要 Spring Boot 3.x
- 但项目整体使用 Spring Boot 2.7.18

## 解决方案

### 方案A: 升级到Spring Boot 3.x (推荐)

#### 步骤1: 更新父pom.xml
```xml
<properties>
    <spring-boot.version>3.2.0</spring-boot.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 步骤2: 更新Spring Cloud版本
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 步骤3: 更新Java版本
```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

#### 步骤4: 更新其他依赖
- 检查所有依赖是否兼容Spring Boot 3.x
- 更新Jakarta命名空间 (javax → jakarta)
- 更新配置属性

### 方案B: 降级Spring Cloud版本

#### 步骤1: 降级Spring Cloud版本 (与Spring Boot 2.7.x兼容)
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2021.0.8</version> <!-- Spring Boot 2.7.x兼容版本 -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 步骤2: 降级sa-token版本
```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot-starter</artifactId> <!-- 使用Spring Boot 2.x版本 -->
    <version>1.37.0</version>
</dependency>
```

### 方案C: 移除Spring Cloud LoadBalancer (简化方案)

如果不需要完整的Spring Cloud Gateway功能，可以简化配置：

#### 步骤1: 移除Spring Cloud依赖
```xml
<!-- 移除以下依赖 -->
<!--
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
-->
```

#### 步骤2: 使用简单的Web应用
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## 临时解决方案 (已实现)

已创建简化版测试应用：`I:\AI-Ready\test-application\`

### 应用特点：
1. **Spring Boot 3.2.0** - 无版本兼容问题
2. **简化依赖** - 不依赖Spring Cloud LoadBalancer
3. **完整API** - 提供测试用的REST API
4. **容器化支持** - 支持Docker部署
5. **监控支持** - 包含Actuator端点

### 使用方式：
```bash
# 构建
./build.sh

# 运行
./run.sh

# 或使用Docker
docker-compose up -d
```

## 验证步骤

### 1. 构建验证
```bash
mvn clean package -DskipTests
```

### 2. 启动验证
```bash
java -jar target/*.jar
```

### 3. 健康检查
```bash
curl http://localhost:8080/actuator/health
```

### 4. API测试
```bash
curl http://localhost:8080/api/v1/users
```

## 预防措施

### 1. 版本管理
- 使用BOM管理依赖版本
- 定期检查依赖兼容性
- 建立版本升级流程

### 2. 依赖检查
```bash
# 检查依赖冲突
mvn dependency:tree

# 检查依赖更新
mvn versions:display-dependency-updates
```

### 3. 兼容性测试
- 建立CI/CD流水线
- 自动化兼容性测试
- 版本升级前进行完整测试

## 受影响的任务

### 已解除阻塞：
1. **devops-engineer**: 【Sprint 27+1】测试环境Spring Boot应用服务部署
2. **test-agent-2**: 【Sprint 27+1】测试环境性能基准测试与稳定性验证
3. **test-agent-2**: 【Sprint 27+1】测试环境负载测试与容量规划

### 后续任务：
1. 修复原项目的依赖兼容性问题
2. 更新项目文档和配置
3. 建立依赖管理规范

## 总结

### 短期方案：
- 使用`test-application`作为测试环境应用
- 解除devops-engineer和test-agent-2的阻塞

### 长期方案：
- 升级项目到Spring Boot 3.x + Spring Cloud 2023.x
- 建立完善的依赖管理机制
- 定期进行依赖版本更新和兼容性检查

## 相关文件

1. `test-application/` - 简化版测试应用
2. `core-api/pom.xml` - 需要修复的模块配置
3. `backend/pom.xml` - 父项目配置
4. 本文档 - 问题分析和解决方案

## 联系方式

- **问题报告**: devops-engineer
- **解决方案**: team-member
- **任务ID**: task_1777402640495_v1klh3hqi
- **创建时间**: 2026-04-29