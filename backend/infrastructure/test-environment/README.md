# 采购合同模块集成测试环境配置指南

## 1. 环境概述

### 1.1 环境目标
本环境用于采购合同模块的集成测试，包含完整的ERP系统模块和监控系统，支持自动化测试执行和性能监控。

### 1.2 技术栈
- **容器编排**: Docker Compose
- **数据库**: MySQL 8.0
- **消息队列**: RabbitMQ 3.9
- **缓存**: Redis 6.2
- **应用服务**: Spring Boot 2.7+
- **API网关**: Nginx 1.20
- **监控系统**: ELK Stack + Prometheus + Grafana

### 1.3 网络架构
```
外部访问 → Nginx (80/443) → 应用服务 → 数据库/消息队列
                    ↓
            监控系统 (ELK + Prometheus + Grafana)
```

## 2. 环境部署

### 2.1 系统要求
- **操作系统**: Linux (Ubuntu 20.04+), macOS, Windows (WSL2)
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **内存**: 最低8GB，推荐16GB
- **磁盘空间**: 最低20GB

### 2.2 部署步骤

#### 步骤1: 克隆配置
```bash
# 创建测试环境目录
mkdir -p /opt/erp-test
cd /opt/erp-test

# 复制配置文件
cp -r test-environment-config/* .
```

#### 步骤2: 构建应用服务镜像
```bash
# 构建合同服务镜像
cd contract-service
docker build -t erp-contract-service:test .

# 构建供应商服务镜像
cd ../supplier-service
docker build -t erp-supplier-service:test .
```

#### 步骤3: 启动环境
```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

#### 步骤4: 验证环境
```bash
# 检查服务健康状态
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# 检查数据库连接
docker exec -it mysql-contract-test mysql -ucontract_user -pContract@123 contract_db -e "SELECT 1"

# 检查RabbitMQ管理界面
# 访问 http://localhost:15672
# 用户名: admin, 密码: Admin@123
```

### 2.3 环境配置

#### 端口映射表
| 服务 | 容器端口 | 主机端口 | 访问地址 |
|------|----------|----------|----------|
| 合同服务 | 8080 | 8081 | http://localhost:8081 |
| 供应商服务 | 8080 | 8082 | http://localhost:8082 |
| MySQL合同库 | 3306 | 33061 | localhost:33061 |
| MySQL供应商库 | 3306 | 33062 | localhost:33062 |
| RabbitMQ | 5672 | 5672 | localhost:5672 |
| RabbitMQ管理 | 15672 | 15672 | http://localhost:15672 |
| Redis | 6379 | 6379 | localhost:6379 |
| Elasticsearch | 9200 | 9200 | http://localhost:9200 |
| Kibana | 5601 | 5601 | http://localhost:5601 |
| Prometheus | 9090 | 9090 | http://localhost:9090 |
| Grafana | 3000 | 3000 | http://localhost:3000 |
| Nginx | 80 | 80 | http://localhost |

#### 账号密码表
| 服务 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| MySQL合同库 | contract_user | Contract@123 | 合同数据库用户 |
| MySQL供应商库 | supplier_user | Supplier@123 | 供应商数据库用户 |
| RabbitMQ | admin | Admin@123 | 管理员账号 |
| Redis | - | Redis@123 | Redis认证密码 |
| Grafana | admin | Grafana@123 | Grafana管理员 |

## 3. 测试数据初始化

### 3.1 数据库初始化脚本
在 `sql/` 目录下提供以下初始化脚本：

1. `init.sql` - 合同数据库表结构和基础数据
2. `supplier-init.sql` - 供应商数据库表结构和基础数据
3. `test-data.sql` - 测试数据生成脚本

### 3.2 基础测试数据内容
```sql
-- 供应商数据 (10条记录)
INSERT INTO suppliers (id, name, code, status, contact_person, phone, email) VALUES
('SUP001', 'ABC科技有限公司', 'ABC-TECH', 'ACTIVE', '张三', '13800138001', 'zhangsan@abc.com'),
('SUP002', 'XYZ制造有限公司', 'XYZ-MFG', 'ACTIVE', '李四', '13900139002', 'lisi@xyz.com');

-- 产品数据 (50条记录)
INSERT INTO products (id, sku, name, description, unit_price, category, supplier_id) VALUES
('PRD001', 'SKU-001', '服务器CPU', '高性能服务器处理器', 2500.00, '硬件', 'SUP001'),
('PRD002', 'SKU-002', '内存条', 'DDR4 16GB内存', 800.00, '硬件', 'SUP001');

-- 用户数据 (20条记录)
INSERT INTO users (id, username, password, real_name, role, department, status) VALUES
('USR001', 'zhangwei', 'password123', '张伟', 'CONTRACT_MANAGER', '采购部', 'ACTIVE'),
('USR002', 'lina', 'password123', '李娜', 'APPROVER', '财务部', 'ACTIVE');
```

### 3.3 数据生成工具
提供Python脚本自动生成测试数据：
```bash
# 生成测试数据
cd scripts
python generate_test_data.py --contracts 100 --suppliers 20 --products 200 --users 50

# 导入测试数据
python import_test_data.py --db-host localhost --db-port 33061 --db-name contract_db
```

## 4. 监控配置

### 4.1 ELK日志监控
- **Elasticsearch**: 日志存储和索引
- **Logstash**: 日志收集和处理
- **Kibana**: 日志查看和分析

访问地址: http://localhost:5601

### 4.2 Prometheus指标监控
- **Prometheus**: 指标收集和存储
- **Grafana**: 指标展示和告警

访问地址:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/Grafana@123)

### 4.3 监控面板
预配置的Grafana面板:
1. **应用性能监控**: 接口响应时间、错误率、吞吐量
2. **系统资源监控**: CPU、内存、磁盘、网络使用情况
3. **数据库监控**: 连接数、查询性能、锁等待
4. **消息队列监控**: 队列深度、消息积压、消费者状态

## 5. 测试工具集成

### 5.1 自动化测试工具
```bash
# 安装测试工具
pip install -r requirements.txt

# 运行接口测试
python run_api_tests.py --env test --test-case integration

# 运行性能测试
jmeter -n -t performance-tests/contract-create.jmx -l results.jtl -e -o report
```

### 5.2 测试数据管理
```bash
# 备份测试数据
python backup_test_data.py --backup-dir /backup/test-data

# 恢复测试数据
python restore_test_data.py --backup-file /backup/test-data/backup_20230501.sql

# 清理测试数据
python cleanup_test_data.py --keep-days 7
```

## 6. 日常运维

### 6.1 环境维护命令
```bash
# 启动环境
docker-compose up -d

# 停止环境
docker-compose down

# 重启环境
docker-compose restart

# 查看日志
docker-compose logs -f contract-service

# 进入容器
docker exec -it mysql-contract-test bash
```

### 6.2 备份和恢复
```bash
# 备份数据库
docker exec mysql-contract-test mysqldump -ucontract_user -pContract@123 contract_db > backup_contract.sql
docker exec mysql-supplier-test mysqldump -usupplier_user -pSupplier@123 supplier_db > backup_supplier.sql

# 恢复数据库
docker exec -i mysql-contract-test mysql -ucontract_user -pContract@123 contract_db < backup_contract.sql
```

### 6.3 监控和维护
```bash
# 查看系统资源使用
docker stats

# 查看容器日志
docker-compose logs --tail=100

# 清理无用资源
docker system prune -a

# 更新镜像
docker-compose pull
```

## 7. 故障排除

### 7.1 常见问题

#### 问题1: 服务启动失败
```bash
# 查看详细错误信息
docker-compose logs --tail=50

# 检查端口冲突
netstat -tulpn | grep :8081

# 重新构建服务
docker-compose build --no-cache contract-service
```

#### 问题2: 数据库连接失败
```bash
# 检查数据库服务状态
docker-compose ps mysql-contract

# 检查数据库日志
docker-compose logs mysql-contract

# 测试数据库连接
docker exec mysql-contract-test mysql -ucontract_user -pContract@123 -e "SELECT 1"
```

#### 问题3: 内存不足
```bash
# 查看内存使用情况
free -h

# 调整Docker内存限制
# 编辑 /etc/docker/daemon.json
# 增加 "memory": "8g"
```

### 7.2 性能优化建议
1. **增加资源分配**:
   ```yaml
   # 在docker-compose.yml中增加资源限制
   contract-service:
     deploy:
       resources:
         limits:
           memory: 2G
           cpus: '1.0'
   ```

2. **启用缓存优化**:
   ```bash
   # 调整Redis配置
   docker exec redis-test redis-cli CONFIG SET maxmemory 1gb
   docker exec redis-test redis-cli CONFIG SET maxmemory-policy allkeys-lru
   ```

3. **优化数据库配置**:
   ```sql
   -- 创建索引优化查询性能
   CREATE INDEX idx_contract_status ON contracts(status);
   CREATE INDEX idx_contract_supplier ON contracts(supplier_id);
   ```

## 8. 安全配置

### 8.1 安全最佳实践
1. **修改默认密码**: 部署后立即修改所有服务的默认密码
2. **启用HTTPS**: 配置Nginx启用HTTPS，使用有效证书
3. **网络隔离**: 使用Docker网络隔离，限制服务间访问
4. **访问控制**: 配置防火墙规则，限制外部访问
5. **日志审计**: 启用详细日志记录，定期审计

### 8.2 安全加固脚本
```bash
# 运行安全加固脚本
cd security
./harden_environment.sh

# 检查安全配置
./security_audit.sh
```

## 9. 扩展配置

### 9.1 添加新服务
```yaml
# 在docker-compose.yml中添加新服务
new-service:
  image: your-image:tag
  container_name: new-service-test
  ports:
    - "8083:8080"
  environment:
    - DB_HOST=mysql-contract
  networks:
    - erp-test-network
```

### 9.2 自定义配置
1. **修改应用配置**: 编辑 `config/application-test.properties`
2. **调整监控配置**: 编辑 `prometheus/prometheus.yml`
3. **更新Nginx配置**: 编辑 `nginx/conf.d/default.conf`

## 10. 附录

### 10.1 环境检查清单
- [ ] Docker和Docker Compose已安装
- [ ] 系统资源满足要求
- [ ] 端口无冲突
- [ ] 配置文件已就绪
- [ ] 测试数据已准备
- [ ] 监控系统正常运行

### 10.2 联系方式
- **技术支持**: devops@example.com
- **测试团队**: qa-team@example.com
- **问题反馈**: issue-tracker.example.com

---
**文档版本**: 1.0  
**最后更新**: 2026-05-01  
**维护团队**: ERP测试团队  
**适用环境**: 集成测试环境