# AI-Ready Docker镜像构建优化方案

**版本**: v3.0  
**日期**: 2026-04-12  
**作者**: devops-engineer  

---

## 一、优化概述

### 1.1 优化目标

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 镜像体积 | 800MB | ~180MB | 77%↓ |
| 构建时间 | 8分钟 | ~90秒 | 81%↓ |
| 启动时间 | 45秒 | ~12秒 | 73%↓ |
| 安全漏洞 | 50+ | <5 | 90%↓ |
| 构建缓存命中率 | 30% | 85% | 183%↑ |

### 1.2 核心技术

- ✅ **多阶段构建**: 分离构建和运行环境
- ✅ **分层缓存优化**: 最大化利用Docker缓存
- ✅ **Spring Boot分层JAR**: 优化应用层缓存
- ✅ **Alpine基础镜像**: 最小化基础镜像
- ✅ **非root用户**: 安全运行
- ✅ **Buildx多平台**: 支持AMD64/ARM64

---

## 二、Dockerfile优化详解

### 2.1 三阶段构建架构

```
┌─────────────────────────────────────────────────────────────┐
│  Stage 1: deps                                              │
│  ├── 复制 pom.xml, mvnw                                     │
│  └── 下载依赖 (缓存层)                                       │
├─────────────────────────────────────────────────────────────┤
│  Stage 2: builder                                           │
│  ├── 复制缓存的依赖                                          │
│  ├── 复制源代码                                             │
│  └── 构建并解压JAR                                          │
├─────────────────────────────────────────────────────────────┤
│  Stage 3: production                                        │
│  ├── 最小化JRE基础镜像                                       │
│  ├── 分层复制应用                                            │
│  └── 非root用户运行                                          │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 关键优化点

#### 2.2.1 依赖缓存分离

```dockerfile
# Stage 1: 仅复制pom.xml，利用缓存
FROM eclipse-temurin:21-jdk-alpine AS deps
COPY pom.xml .mvn mvnw ./
RUN ./mvnw dependency:go-offline -B

# Stage 2: 复制缓存的依赖
FROM eclipse-temurin:21-jdk-alpine AS builder
COPY --from=deps /root/.m2 /root/.m2
```

**优势**: 源代码变更不会触发依赖重新下载

#### 2.2.2 Spring Boot分层JAR

```dockerfile
# 解压分层
RUN java -Djarmode=layertools -jar target/*.jar extract

# 分层复制（dependencies层最稳定）
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./
```

**优势**: 
- dependencies层几乎不变，缓存命中率高
- 应用代码变更只重建最后一层

#### 2.2.3 最小化基础镜像

```dockerfile
# 使用JRE而非JDK
FROM eclipse-temurin:21-jre-alpine

# 仅安装必要工具
RUN apk add --no-cache curl=~8.5
```

**优势**: 相比JDK减少~200MB

#### 2.2.4 JVM参数优化

```dockerfile
ENV JAVA_OPTS="\
    -server \
    -Xms512m -Xmx1024m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"
```

---

## 三、构建脚本使用

### 3.1 快速开始

```bash
cd I:\AI-Ready\docker

# 本地构建
./build-optimized.sh build

# 构建并推送
./build-optimized.sh build --push

# 完整流程（构建+推送+扫描）
./build-optimized.sh all
```

### 3.2 构建命令详解

| 命令 | 说明 |
|------|------|
| `build` | 仅构建镜像 |
| `push` | 构建并推送 |
| `scan` | 构建并扫描漏洞 |
| `test` | 构建并测试启动 |
| `all` | 完整流程 |
| `cleanup` | 清理缓存 |

### 3.3 环境变量配置

```bash
# 镜像仓库
export REGISTRY=ghcr.io
export IMAGE_NAME=ai-ready/ai-ready-api

# 版本控制
export VERSION=1.2.3
export GIT_SHA=$(git rev-parse --short HEAD)

# 构建配置
export PLATFORMS=linux/amd64,linux/arm64
export CACHE_DIR=/tmp/docker-build-cache
```

---

## 四、CI/CD集成

### 4.1 GitHub Actions

```yaml
name: Docker Build

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Login to Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Build and Push
        run: |
          cd docker
          ./build-optimized.sh all --push
        env:
          VERSION: ${{ github.ref_name }}
          GIT_SHA: ${{ github.sha }}
```

### 4.2 GitLab CI

```yaml
stages:
  - build
  - scan
  - push

docker-build:
  stage: build
  script:
    - cd docker
    - ./build-optimized.sh build
  cache:
    paths:
      - /tmp/docker-build-cache/

docker-scan:
  stage: scan
  script:
    - cd docker
    - ./build-optimized.sh scan
  artifacts:
    reports:
      container_scanning: trivy-report-*.json

docker-push:
  stage: push
  script:
    - cd docker
    - ./build-optimized.sh push
  only:
    - main
    - tags
```

---

## 五、缓存策略

### 5.1 Docker层缓存

```dockerfile
# 1. 最稳定的层（依赖）
COPY pom.xml ./
RUN mvn dependency:go-offline

# 2. 偶尔变化的层（配置）
COPY config ./config

# 3. 频繁变化的层（代码）
COPY src ./src
RUN mvn package
```

### 5.2 Buildx缓存

```bash
# 本地缓存
docker buildx build \
  --cache-from type=local,src=/tmp/cache \
  --cache-to type=local,dest=/tmp/cache,mode=max

# 远程缓存 (Registry)
docker buildx build \
  --cache-from type=registry,ref=ghcr.io/ai-ready/cache \
  --cache-to type=registry,ref=ghcr.io/ai-ready/cache,mode=max
```

### 5.3 CI缓存

```yaml
# GitHub Actions缓存
- uses: actions/cache@v3
  with:
    path: /tmp/docker-build-cache
    key: docker-${{ github.sha }}
    restore-keys: docker-
```

---

## 六、多平台构建

### 6.1 支持的平台

| 平台 | 说明 |
|------|------|
| linux/amd64 | x86_64服务器 |
| linux/arm64 | ARM服务器 (AWS Graviton) |
| linux/arm/v7 | 树莓派等ARM设备 |

### 6.2 构建命令

```bash
# 多平台构建
./build-optimized.sh build --platform linux/amd64,linux/arm64

# 指定单一平台
./build-optimized.sh build --platform linux/arm64
```

---

## 七、安全扫描

### 7.1 Trivy扫描

```bash
# 扫描镜像
trivy image ghcr.io/ai-ready/ai-ready-api:latest

# 生成报告
trivy image --format json -o report.json ghcr.io/ai-ready/ai-ready-api:latest
```

### 7.2 安全最佳实践

- ✅ 使用非root用户运行
- ✅ 最小化基础镜像
- ✅ 定期更新基础镜像
- ✅ 扫描依赖漏洞
- ✅ 限制容器权限

---

## 八、性能测试

### 8.1 镜像大小对比

```bash
# 查看镜像大小
docker images ghcr.io/ai-ready/ai-ready-api --format "table {{.Tag}}\t{{.Size}}"

# 查看层大小
docker history ghcr.io/ai-ready/ai-ready-api:latest
```

### 8.2 启动时间测试

```bash
# 测试启动时间
time docker run --rm ghcr.io/ai-ready/ai-ready-api:latest
```

### 8.3 资源使用

```bash
# 监控资源使用
docker stats ai-ready-api
```

---

## 九、故障排查

### 9.1 构建失败

```bash
# 查看详细构建日志
docker build --progress=plain -t test .

# 调试特定阶段
docker build --target builder -t test-builder .
docker run -it test-builder sh
```

### 9.2 缓存问题

```bash
# 清理构建缓存
docker builder prune -f

# 无缓存构建
docker build --no-cache -t test .
```

### 9.3 多平台构建问题

```bash
# 检查buildx
docker buildx ls
docker buildx inspect

# 重新创建builder
docker buildx create --use --name ai-ready-builder
```

---

## 十、附录

### 10.1 镜像标签策略

| 标签 | 用途 | 示例 |
|------|------|------|
| latest | 开发测试 | ai-ready-api:latest |
| v1.2.3 | 正式发布 | ai-ready-api:v1.2.3 |
| 20260412-180200 | 时间戳 | ai-ready-api:20260412-180200 |
| abc1234 | Git SHA | ai-ready-api:abc1234 |

### 10.2 相关文档

- [Dockerfile参考](https://docs.docker.com/engine/reference/builder/)
- [Buildx文档](https://docs.docker.com/buildx/working-with-buildx/)
- [Spring Boot Docker](https://spring.io/guides/topicals/spring-boot-docker/)

---

**文档版本**: v3.0  
**最后更新**: 2026-04-12
