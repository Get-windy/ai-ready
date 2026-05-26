# Docker环境配置文档

## 环境检查结果

- Docker版本: 29.3.1 (要求 >= 20.10) ✅
- Docker Compose版本: 5.1.1 ✅

## 项目技术栈

后端技术栈：
- Java 17
- Spring Boot 2.7.18
- PostgreSQL
- Redis
- RabbitMQ
- Elasticsearch 8.x
- MinIO

## 测试容器需求

1. **PostgreSQL容器** - 测试数据库
2. **Redis容器** - 测试缓存
3. **RabbitMQ容器** - 测试消息队列
4. **Elasticsearch容器** - 测试搜索引擎
5. **MinIO容器** - 测试对象存储

## Docker安装说明

### Windows安装

1. 下载 Docker Desktop: https://www.docker.com/products/docker-desktop/
2. 安装并启动 Docker Desktop
3. 验证安装:
```bash
docker --version
docker compose version
```

### Linux安装

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 验证安装
docker --version
```

## Docker Compose安装

Docker Desktop已包含Docker Compose。独立安装:

```bash
# 下载Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```
