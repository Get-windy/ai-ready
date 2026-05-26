# 容器管理常见问题

## 问题1：容器无法启动

**症状**: `docker-compose up` 报错，容器无法启动

**解决方法**:
```bash
# 检查端口占用
netstat -ano | findstr :5433  # PostgreSQL端口
netstat -ano | findstr :6380  # Redis端口
netstat -ano | findstr :5673  # RabbitMQ端口
netstat -ano | findstr :9201  # Elasticsearch端口
netstat -ano | findstr :9001  # MinIO端口

# 停止占用端口的进程
taskkill /PID <PID> /F

# 或修改docker-compose.yml中的端口映射
# 例如将 PostgreSQL 5433 改为 5434
```

## 问题2：卷权限问题

**症状**: PostgreSQL/MinIO等服务无法写入数据卷

**解决方法**:
```bash
# 删除所有卷
docker-compose -f docker-compose.test-env.yml down -v

# 重新创建
docker-compose -f docker-compose.test-env.yml up -d
```

## 问题3：容器健康检查失败

**症状**: 容器启动但healthcheck失败

**解决方法**:
```bash
# 查看容器日志
docker logs ai-ready-postgresql-test
docker logs ai-ready-redis-test

# 检查容器内部进程
docker exec ai-ready-postgresql-test pg_isready -U test_user

# 重启容器
docker-compose -f docker-compose.test-env.yml restart postgresql-test
```

## 问题4：容器间网络连接问题

**症状**: 应用容器无法连接到测试容器

**解决方法**:
```bash
# 确保所有容器在同一个网络
docker network ls

# 检查网络连接
docker exec ai-ready-test-container ping postgresql-test

# 使用服务名称而不是localhost
# 例如: postgresql://test_user:test_password@postgresql-test:5432/ai_ready_test
```

## 问题5：Docker Desktop资源不足

**症状**: 容器启动缓慢或崩溃

**解决方法**:
```bash
# 停止不必要的容器
docker-compose -f docker-compose.test-env.yml stop

# 增加Docker Desktop资源限制
# 设置 -> Resources -> CPUs/Memory/Swap

# 只启动必要的服务
docker-compose -f docker-compose.test-env.yml up -d postgresql-test redis-test
```

## 问题6：Windows路径问题

**症状**: 权限错误或路径不存在

**解决方法**:
```bash
# 使用正斜杠或双反斜杠
# 正确: -v C:/workspace:/app
# 正确: -v C:\\workspace:/app
# 错误: -v C:\workspace:/app

# 使用Docker Compose变量
docker-compose -f docker-compose.test-env.yml config
```

## 问题7：Python依赖安装失败

**症状**: 测试容器构建时pip安装失败

**解决方法**:
```bash
# 使用国内镜像源
RUN pip install -r requirements-test.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 或临时更换源
pip install -r requirements-test.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

## 问题8：容器中文乱码

**症状**: 容器内输出中文乱码

**解决方法**:
```bash
# 设置环境变量
-e LANG=C.UTF-8 \
-e LC_ALL=C.UTF-8 \

# 在Dockerfile中添加
RUN apk add --no-cache font-noto-cjk
```

## 调试技巧

### 查看容器实时日志
```bash
docker-compose -f docker-compose.test-env.yml logs -f
```

### 进入容器调试
```bash
docker exec -it ai-ready-postgresql-test sh
docker exec -it ai-ready-redis-test redis-cli
```

### 重启单个服务
```bash
docker-compose -f docker-compose.test-env.yml restart postgresql-test
```

### 重建容器
```bash
docker-compose -f docker-compose.test-env.yml build --no-cache
docker-compose -f docker-compose.test-env.yml up -d
```

### 查看容器详细信息
```bash
docker inspect ai-ready-postgresql-test
```

## 常用命令速查

```bash
# 启动所有服务
./docker-manage.sh start

# 停止所有服务
./docker-manage.sh stop

# 重启所有服务
./docker-manage.sh restart

# 查看状态
./docker-manage.sh status

# 查看日志
./docker-manage.sh logs postgresql-test

# 完全清理
./docker-manage.sh clean
```
