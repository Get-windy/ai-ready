# Core-API模块依赖修复指南

## 问题分析

### 当前问题
**错误**: `NoClassDefFoundError: org.springframework.beans.factory.aot.BeanRegistrationAotProcessor`

**根本原因**: 版本不兼容
- **父pom.xml**: Spring Boot 2.7.18
- **core-api模块**: Spring Cloud 2023.0.3 (需要Spring Boot 3.x)
- **core-api模块**: sa-token-spring-boot3-starter (需要Spring Boot 3.x)
- **core-api模块**: mybatis-plus-spring-boot3-starter (需要Spring Boot 3.x)

### 详细分析

#### 1. Spring Cloud版本冲突
- Spring Cloud 2023.x 需要 Spring Boot 3.x
- 项目使用 Spring Boot 2.7.18，不兼容

#### 2. Sa-Token版本问题
- `sa-token-spring-boot3-starter` 专为Spring Boot 3设计
- Spring Boot 2.x应使用 `sa-token-spring-boot-starter`

#### 3. MyBatis-Plus版本问题
- `mybatis-plus-spring-boot3-starter` 专为Spring Boot 3设计
- Spring Boot 2.x应使用 `mybatis-plus-boot-starter`

## 解决方案

### 方案A：降级依赖版本（推荐，更安全）

#### 步骤1：备份原文件
```bash
cd I:\AI-Ready\backend\core\api\core-api
cp pom.xml pom.xml.backup
```

#### 步骤2：应用修复版本
```bash
# 使用修复后的pom.xml
cp pom-fixed.xml pom.xml
```

#### 关键修改点：
1. **移除Spring Cloud Gateway依赖** - Spring Cloud Gateway与Spring Boot 2.7.18不兼容
2. **使用Spring Boot 2.x兼容的Sa-Token**: `sa-token-spring-boot-starter` (而非 `spring-boot3-starter`)
3. **使用Spring Boot 2.x兼容的MyBatis-Plus**: `mybatis-plus-boot-starter` (而非 `spring-boot3-starter`)
4. **降级Spring Cloud版本**: 2021.0.8 (与Spring Boot 2.7.x兼容)
5. **降级springdoc-openapi**: 1.7.0 (与Spring Boot 2.x兼容)

#### 步骤3：清理并重建
```bash
# 清理旧的构建文件
mvn clean

# 重新编译
mvn compile

# 打包应用
mvn package -DskipTests
```

#### 步骤4：验证修复
```bash
# 启动应用
java -jar target/core-api-1.0.0-SNAPSHOT.jar

# 验证健康检查
curl http://localhost:8080/actuator/health

# 验证API端点
curl http://localhost:8080/api/v1/users
```

### 方案B：升级到Spring Boot 3.x（需要更大改动）

#### 优点：
- 可以使用最新的Spring Cloud版本
- 可以使用最新的依赖版本
- 长期维护更方便

#### 缺点：
- 需要修改整个项目的依赖配置
- 需要更新所有模块的代码（javax → jakarta）
- 可能影响其他模块的兼容性

#### 步骤：

##### 1. 更新父pom.xml
```xml
<properties>
    <spring-boot.version>3.2.0</spring-boot.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
    <java.version>17</java.version>
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
        
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

##### 2. 更新代码中的命名空间
- `javax.*` → `jakarta.*`
- 例如：`javax.servlet` → `jakarta.servlet`
- 例如：`javax.persistence` → `jakarta.persistence`
- 例如：`javax.validation` → `jakarta.validation`

##### 3. 更新所有依赖配置
- 检查每个模块的pom.xml
- 确保所有依赖都兼容Spring Boot 3.x

##### 4. 重新编译和测试
```bash
# 清理所有模块
mvn clean

# 重新编译所有模块
mvn compile

# 运行所有测试
mvn test

# 打包所有模块
mvn package
```

## 推荐方案

### 短期方案（立即应用）
**推荐使用方案A**（降级依赖版本），原因：
1. **更安全** - 只需修改core-api模块，不影响其他模块
2. **更快** - 可以立即解决问题，快速解除阻塞
3. **风险低** - 不需要修改代码，只需调整依赖版本

### 长期方案（后续规划）
**考虑方案B**（升级到Spring Boot 3.x），但需要：
1. **充分测试** - 先在测试环境验证
2. **逐步迁移** - 不要一次性修改所有模块
3. **建立流程** - 制定版本升级流程和测试规范

## 注意事项

### 1. Spring Cloud Gateway移除的影响
修复后的版本移除了Spring Cloud Gateway，影响：
- **失去了网关功能** - 需要使用其他方式实现路由
- **解决方案** - 可以使用简单的Web应用或外部网关

### 2. 功能验证清单
修复后需要验证的功能：
- ✅ 应用可正常启动
- ✅ API端点可访问
- ✅ 数据库连接正常
- ✅ Redis连接正常
- ✅ 认证授权功能正常
- ✅ 监控端点可用

### 3. 测试建议
修复后应进行以下测试：
1. **单元测试** - 验证核心功能
2. **集成测试** - 验证模块间协作
3. **性能测试** - 验证性能没有下降
4. **兼容性测试** - 验证与现有系统的兼容性

## 验证步骤

### 1. 构建验证
```bash
cd I:\AI-Ready\backend\core\api\core-api
mvn clean package -DskipTests
```

预期结果：
- ✅ 编译成功
- ✅ JAR文件生成
- ✅ 无错误警告

### 2. 启动验证
```bash
java -jar target/core-api-1.0.0-SNAPSHOT.jar
```

预期结果：
- ✅ 应用启动成功
- ✅ 无启动错误
- ✅ 打印启动信息

### 3. 功能验证
```bash
# 健康检查
curl http://localhost:8080/actuator/health
# 预期: {"status":"UP"}

# API端点
curl http://localhost:8080/api/v1/users
# 预期: 返回用户列表

# 监控指标
curl http://localhost:8080/actuator/metrics
# 预期: 返回指标数据
```

## 恢复原配置

如果修复后出现问题，可以恢复原配置：
```bash
cd I:\AI-Ready\backend\core\api\core-api
cp pom.xml.backup pom.xml
mvn clean package -DskipTests
```

## 相关文件

### 修复文件
- `pom-fixed.xml` - 修复后的依赖配置
- `pom.xml.backup` - 原配置备份

### 文档文件
- `SPRING_CLOUD_LOADBALANCER_FIX.md` - 问题分析
- `CORE_API_DEPENDENCY_FIX_GUIDE.md` - 本修复指南

### 测试应用
- `test-application/` - 简化版测试应用（已验证可用）

## 总结

### 问题本质
版本不兼容导致的依赖冲突，Spring Boot 2.7.18无法使用Spring Boot 3.x的依赖。

### 解决思路
使用与Spring Boot版本兼容的依赖版本，避免版本冲突。

### 验证标准
应用可正常启动、API可访问、核心功能正常。

### 后续规划
建立依赖版本管理规范，定期检查依赖兼容性。

---

**创建者**: team-member
**创建时间**: 2026-04-29 03:15
**任务ID**: task_1777402640495_v1klh3hqi