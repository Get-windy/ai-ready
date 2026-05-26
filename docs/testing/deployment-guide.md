# 测试环境部署指南

## 概述

本文档提供AI-Ready项目测试环境的完整部署指南，包括环境准备、部署步骤、验证方法和故障排除。

## 环境要求

### 硬件要求

| 资源 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 2核 | 4核 |
| 内存 | 4GB | 8GB |
| 存储 | 20GB | 50GB |
| 网络 | 100Mbps | 1Gbps |

### 软件要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.0+ | 容器编排 |
| Node.js | 18.0+ | 前端构建 |
| pnpm | 8.0+ | 包管理器 |
| Git | 2.30+ | 版本控制 |

### 网络要求

- 端口范围: 80, 443, 3000-3010, 8080-8090, 9090, 3100
- 域名解析: 需要配置测试环境域名
- SSL证书: 可选，用于HTTPS支持

## 部署准备

### 1. 获取代码

```bash
# 克隆代码库
git clone https://github.com/ai-ready/ai-ready.git
cd ai-ready

# 切换到测试分支
git checkout develop
```

### 2. 环境变量配置

创建环境变量文件：

```bash
# 复制环境变量模板
cp frontend/.env.example frontend/.env.test

# 编辑环境变量
vim frontend/.env.test
```

配置示例：
```env
# 基本配置
NODE_ENV=test
VITE_APP_ENV=test
VITE_APP_TITLE=AI-Ready 测试环境

# API配置
VITE_API_BASE_URL=/api/v1
VITE_API_TIMEOUT=30000
VITE_API_ADMIN_URL=/api/admin
VITE_API_MOBILE_URL=/api/mobile

# 代理配置
VITE_PROXY_TARGET=http://backend-test:8080
VITE_PROXY_CHANGE_ORIGIN=true

# 功能开关
VITE_FEATURE_DEBUG=true
VITE_FEATURE_MOCK_API=true
```

### 3. 域名配置

在 `/etc/hosts` 或DNS服务器中添加：

```
127.0.0.1 frontend.test.ai-ready.com
127.0.0.1 admin.test.ai-ready.com
127.0.0.1 mobile.test.ai-ready.com
127.0.0.1 backend.test.ai-ready.com
```

## Docker部署

### 单容器部署

```bash
# 进入前端目录
cd frontend

# 构建Docker镜像
docker build -t ai-ready-frontend:test .

# 运行容器
docker run -d \
  --name ai-ready-frontend-test \
  -p 8081:80 \
  -e NODE_ENV=test \
  -e VITE_APP_ENV=test \
  -v ./logs:/var/log/nginx \
  ai-ready-frontend:test
```

### Docker Compose部署

```bash
# 使用Docker Compose启动完整测试环境
docker-compose -f docker-compose.test.yml up -d

# 查看服务状态
docker-compose -f docker-compose.test.yml ps

# 查看日志
docker-compose -f docker-compose.test.yml logs -f frontend

# 停止服务
docker-compose -f docker-compose.test.yml down
```

### 服务说明

| 服务 | 端口 | 说明 | 访问地址 |
|------|------|------|----------|
| frontend | 8081 | 前端应用 | http://localhost:8081 |
| backend-test | 8082 | 模拟后端 | http://localhost:8082 |
| prometheus | 9090 | 监控指标 | http://localhost:9090 |
| grafana | 3000 | 监控面板 | http://localhost:3000 |
| loki | 3100 | 日志收集 | http://localhost:3100 |

## Kubernetes部署

### 1. 集群准备

```bash
# 创建命名空间
kubectl create namespace ai-ready-test

# 配置上下文
kubectl config set-context --current --namespace=ai-ready-test
```

### 2. 部署前端应用

```bash
# 应用部署配置
kubectl apply -f infra/k8s/frontend/deployment.yaml

# 查看部署状态
kubectl rollout status deployment/ai-ready-frontend

# 查看Pod状态
kubectl get pods -l app=ai-ready-frontend

# 查看服务
kubectl get services

# 查看Ingress
kubectl get ingress
```

### 3. 配置Ingress控制器

如果使用Nginx Ingress Controller：

```bash
# 部署Ingress控制器（如果未部署）
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml

# 等待控制器就绪
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

### 4. 域名解析配置

配置DNS或本地hosts文件：
```
<集群IP> frontend.test.ai-ready.com
<集群IP> admin.test.ai-ready.com
<集群IP> mobile.test.ai-ready.com
```

## 手动构建部署

### 1. 前端构建

```bash
# 安装依赖
cd frontend
pnpm install

# 测试环境构建
pnpm run build:test

# 开发环境运行
pnpm run dev
```

### 2. Nginx配置部署

```bash
# 复制构建产物
cp -r dist /var/www/html/ai-ready-test

# 配置Nginx
cp nginx.default.conf /etc/nginx/conf.d/ai-ready-test.conf

# 重启Nginx
systemctl restart nginx
```

### 3. Nginx配置文件示例

```nginx
server {
    listen 80;
    server_name frontend.test.ai-ready.com;
    
    root /var/www/html/ai-ready-test;
    index index.html;
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # API代理
    location /api/ {
        proxy_pass http://backend.test.ai-ready.com:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # SPA路由
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

## 部署验证

### 1. 健康检查

```bash
# 基础健康检查
curl -f http://frontend.test.ai-ready.com/health

# API健康检查
curl -f http://frontend.test.ai-ready.com/api/health

# 版本信息
curl -f http://frontend.test.ai-ready.com/version
```

### 2. 功能验证

```bash
# 页面访问测试
curl -I http://frontend.test.ai-ready.com

# 管理端访问测试
curl -I http://admin.test.ai-ready.com

# 移动端访问测试
curl -I http://mobile.test.ai-ready.com
```

### 3. 性能测试

```bash
# 使用ab进行压力测试
ab -n 1000 -c 10 http://frontend.test.ai-ready.com/

# 使用curl测试响应时间
time curl -o /dev/null -s -w "%{time_total}\n" http://frontend.test.ai-ready.com
```

### 4. 监控验证

```bash
# Prometheus指标
curl http://localhost:9090/metrics

# Grafana面板
open http://localhost:3000

# Loki日志
curl http://localhost:3100/ready
```

## 自动化部署

### GitHub Actions

配置文件位置：`.github/workflows/frontend-test-env.yml`

触发条件：
- 推送到develop分支
- 手动触发工作流
- 创建Pull Request到main分支

部署流程：
1. 代码质量检查
2. Docker镜像构建
3. 部署到测试环境
4. 性能测试
5. 安全扫描

### 手动触发部署

```bash
# 在GitHub仓库页面
# Actions → Frontend Test Environment CI/CD → Run workflow
```

## 维护操作

### 更新部署

```bash
# Docker Compose更新
docker-compose -f docker-compose.test.yml down
docker-compose -f docker-compose.test.yml pull
docker-compose -f docker-compose.test.yml up -d

# Kubernetes更新
kubectl rollout restart deployment/ai-ready-frontend
```

### 日志查看

```bash
# Docker日志
docker logs -f ai-ready-frontend-test

# Kubernetes日志
kubectl logs -f deployment/ai-ready-frontend

# Nginx访问日志
tail -f /var/log/nginx/access.log

# 应用错误日志
tail -f /var/log/nginx/error.log
```

### 监控和告警

```bash
# 查看资源使用
docker stats ai-ready-frontend-test

# Kubernetes资源监控
kubectl top pods -l app=ai-ready-frontend

# 查看Prometheus告警
curl http://localhost:9090/api/v1/alerts
```

## 故障排除

### 常见问题

#### 1. 应用无法访问

**症状**: 浏览器显示无法连接或超时

**排查步骤**:
```bash
# 检查服务状态
docker ps | grep frontend
kubectl get pods -l app=ai-ready-frontend

# 检查端口监听
netstat -tlnp | grep :80
kubectl get services -l app=ai-ready-frontend

# 检查Ingress配置
kubectl describe ingress ai-ready-frontend-ingress

# 检查防火墙
iptables -L -n | grep 80
```

**解决方案**:
- 重启服务
- 检查端口配置
- 检查防火墙规则
- 检查Ingress控制器

#### 2. API请求失败

**症状**: 前端显示API错误或网络错误

**排查步骤**:
```bash
# 检查后端服务
curl -f http://backend.test.ai-ready.com:8080/health

# 检查代理配置
docker exec ai-ready-frontend-test cat /etc/nginx/conf.d/default.conf

# 检查网络连通性
docker exec ai-ready-frontend-test ping backend-test

# 查看Nginx错误日志
docker logs ai-ready-frontend-test 2>&1 | grep -i error
```

**解决方案**:
- 检查后端服务状态
- 检查代理配置
- 检查网络配置
- 检查跨域配置

#### 3. 静态资源404

**症状**: CSS、JS、图片等静态资源加载失败

**排查步骤**:
```bash
# 检查构建产物
ls -la frontend/dist/

# 检查Nginx配置
docker exec ai-ready-frontend-test nginx -t

# 查看请求日志
docker logs ai-ready-frontend-test | grep ".js\|.css\|.png"
```

**解决方案**:
- 重新构建前端应用
- 检查Nginx配置
- 检查文件权限
- 检查缓存配置

#### 4. 内存泄漏

**症状**: 应用运行一段时间后变慢或崩溃

**排查步骤**:
```bash
# 查看内存使用
docker stats ai-ready-frontend-test
kubectl top pods -l app=ai-ready-frontend

# 查看进程状态
docker exec ai-ready-frontend-test ps aux

# 检查日志中的内存警告
docker logs ai-ready-frontend-test | grep -i memory
```

**解决方案**:
- 增加内存限制
- 优化应用代码
- 重启服务
- 分析内存快照

### 调试模式

启用调试模式：

```bash
# 设置环境变量
export VITE_FEATURE_DEBUG=true

# 重启服务
docker-compose -f docker-compose.test.yml restart frontend
```

调试信息包括：
- API请求/响应日志
- 组件生命周期日志
- 状态变化日志
- 性能指标

## 备份和恢复

### 配置备份

```bash
# 备份环境变量
cp frontend/.env.test frontend/.env.test.backup

# 备份Docker配置
docker inspect ai-ready-frontend-test > frontend-docker-config.json

# 备份Kubernetes配置
kubectl get deployment ai-ready-frontend -o yaml > frontend-deployment.yaml.backup
```

### 数据备份

```bash
# 备份日志
tar -czf nginx-logs-$(date +%Y%m%d).tar.gz /var/log/nginx/

# 备份监控数据
docker exec ai-ready-prometheus-test tar -czf /prometheus-data.tar.gz /prometheus
docker cp ai-ready-prometheus-test:/prometheus-data.tar.gz .
```

### 恢复操作

```bash
# 恢复环境变量
cp frontend/.env.test.backup frontend/.env.test

# 重新部署
docker-compose -f docker-compose.test.yml up -d --force-recreate
```

## 升级指南

### 版本升级

1. **检查更新日志**
2. **备份当前配置**
3. **测试新版本**
4. **逐步部署**
5. **验证功能**

### 回滚操作

```bash
# Docker回滚
docker-compose -f docker-compose.test.yml down
docker-compose -f docker-compose.test.yml up -d --force-recreate

# Kubernetes回滚
kubectl rollout undo deployment/ai-ready-frontend
```

## 安全注意事项

### 访问控制

1. **限制访问IP**
2. **启用身份验证**
3. **配置防火墙规则**
4. **监控异常访问**

### 数据安全

1. **加密敏感数据**
2. **定期备份数据**
3. **安全删除日志**
4. **审计访问记录**

### 容器安全

1. **使用非root用户**
2. **限制容器权限**
3. **扫描镜像漏洞**
4. **更新安全补丁**

## 性能优化

### 构建优化

1. **代码分割**
2. **Tree Shaking**
3. **缓存优化**
4. **压缩资源**

### 运行时优化

1. **CDN加速**
2. **浏览器缓存**
3. **懒加载组件**
4. **预加载资源**

### 监控优化

1. **实时监控**
2. **告警机制**
3. **性能分析**
4. **容量规划**

## 附录

### 参考文档

- [Vite官方文档](https://vitejs.dev/)
- [Docker文档](https://docs.docker.com/)
- [Kubernetes文档](https://kubernetes.io/docs/)
- [Nginx配置指南](https://nginx.org/en/docs/)

### 工具链接

- 测试环境: http://frontend.test.ai-ready.com
- 监控面板: http://localhost:3000
- API文档: http://backend.test.ai-ready.com:8080/swagger-ui.html
- 代码仓库: https://github.com/ai-ready/ai-ready

### 联系方式

- 部署问题: 运维团队
- 功能问题: 前端开发团队
- 监控问题: SRE团队
- 紧急联系: 项目负责人