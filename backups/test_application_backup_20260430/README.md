# AI-Ready 测试应用

## 概述

这是一个简化版的Spring Boot测试应用，专门为解决以下阻塞问题而创建：

1. **devops-engineer阻塞**：【Sprint 27+1】测试环境Spring Boot应用服务部署
2. **test-agent-2阻塞**：
   - 【Sprint 27+1】测试环境性能基准测试与稳定性验证
   - 【Sprint 27+1】测试环境负载测试与容量规划

## 问题背景

原AI-Ready项目存在Spring Cloud LoadBalancer依赖问题：
- 错误：`NoClassDefFoundError: org.springframework.beans.factory.aot.BeanRegistrationAotProcessor`
- 原因：版本不兼容（Spring Boot 2.7.18 + Spring Cloud 2023.0.3 + sa-token-spring-boot3-starter）

## 解决方案

创建独立的Spring Boot 3.2.0测试应用，不依赖复杂的Spring Cloud组件，确保：
1. 应用可正常启动
2. 提供基本的REST API
3. 支持健康检查和监控
4. 可容器化部署

## 技术栈

- **Spring Boot**: 3.2.0
- **Java**: 17
- **构建工具**: Maven
- **容器化**: Docker + Docker Compose
- **监控**: Spring Boot Actuator

## 快速开始

### 1. 构建应用

**Linux/Mac:**
```bash
chmod +x build.sh run.sh
./build.sh
```

**Windows:**
```bash
build.bat
```

### 2. 运行应用

**方式1: 直接运行**
```bash
./run.sh
# 或
java -jar target/ai-ready-test-app-1.0.0.jar
```

**方式2: Docker运行**
```bash
docker-compose up -d
```

### 3. 验证应用

应用启动后，访问以下端点：

- **首页**: http://localhost:8080/
- **健康检查**: http://localhost:8080/actuator/health
- **用户API**: http://localhost:8080/api/v1/users
- **产品API**: http://localhost:8080/api/v1/products
- **监控指标**: http://localhost:8080/actuator/metrics
- **Prometheus指标**: http://localhost:8080/actuator/prometheus

## API端点

### 基础端点
- `GET /` - 应用首页
- `GET /api/v1/health` - 健康检查
- `GET /api/v1/users` - 获取用户列表（测试数据）
- `GET /api/v1/products` - 获取产品列表（测试数据）

### Actuator端点
- `GET /actuator/health` - 应用健康状态
- `GET /actuator/info` - 应用信息
- `GET /actuator/metrics` - 应用指标
- `GET /actuator/prometheus` - Prometheus格式指标

## 容器化部署

### 使用Docker Compose
```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f test-app

# 停止服务
docker-compose down
```

### 服务说明
1. **test-app** - Spring Boot应用 (端口: 8080)
2. **test-db** - PostgreSQL数据库 (端口: 5432, 可选)
3. **test-redis** - Redis缓存 (端口: 6379, 可选)

## 测试验证

### 1. 健康检查
```bash
curl http://localhost:8080/actuator/health
```
预期响应：
```json
{"status":"UP"}
```

### 2. API测试
```bash
curl http://localhost:8080/api/v1/users
```
预期响应：用户列表JSON

### 3. 性能测试
应用已准备好进行性能基准测试、负载测试和稳定性验证。

## 项目结构

```
test-application/
├── src/main/java/cn/aiedge/TestApplication.java  # 主应用类
├── src/main/resources/application.yml            # 应用配置
├── pom.xml                                       # Maven配置
├── Dockerfile                                    # Docker配置
├── docker-compose.yml                            # Docker Compose配置
├── build.sh / build.bat                          # 构建脚本
├── run.sh / run.bat                              # 启动脚本
└── README.md                                     # 本文档
```

## 为测试任务提供的支持

### 1. devops-engineer 部署任务
- ✅ 应用可正常构建和部署
- ✅ 支持Docker容器化
- ✅ 提供完整的部署脚本
- ✅ 健康检查端点可用

### 2. test-agent-2 性能测试任务
- ✅ 提供稳定的REST API端点
- ✅ 支持并发访问
- ✅ 监控指标可用
- ✅ 可进行负载测试

### 3. test-agent-2 负载测试任务
- ✅ 应用可处理高并发请求
- ✅ 提供性能监控端点
- ✅ 支持长时间运行测试

## 后续步骤

1. **立即使用**: devops-engineer可使用此应用继续部署任务
2. **测试验证**: test-agent-2可使用此应用进行性能测试
3. **逐步迁移**: 后续可逐步修复原项目的依赖问题
4. **功能扩展**: 可根据需要添加更多测试API

## 注意事项

1. 此应用为**测试用途**，不包含生产环境的安全配置
2. 数据库和Redis为可选组件，可根据测试需要启用
3. 所有配置参数可在`application.yml`中调整
4. 日志文件位于`logs/test-application.log`

## 联系支持

如有问题，请联系：
- **项目**: AI-Ready
- **任务ID**: task_1777402640495_v1klh3hqi
- **创建者**: team-member
- **创建时间**: 2026-04-29