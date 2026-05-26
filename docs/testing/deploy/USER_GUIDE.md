# Sprint 27+1 测试环境使用手册

## 目录

1. [快速入门指南](#1-快速入门指南)
2. [用户操作指南](#2-用户操作指南)
3. [管理员配置指南](#3-管理员配置指南)
4. [常用操作说明](#4-常用操作说明)
5. [附录](#5-附录)

---

## 1. 快速入门指南

### 1.1 测试环境概述

Sprint 27+1 测试环境用于验证以下核心模块：

- **订单管理模块**：订单创建、审批、跟踪、结算
- **库存管理模块**：库存查询、调拨、盘点、预警
- **采购管理模块**：采购申请、审批、供应商管理
- **财务结算模块**：成本核算、应收应付、财务报表
- **AI审批助手**：智能审批分析、风险评估、建议生成
- **监控告警系统**：性能监控、告警通知、健康检查

### 1.2 环境访问方式

#### Web界面访问
- **前端地址**: `http://test.example.com:8080`
- **监控大盘**: `http://grafana.test.example.com:3000`
- **API文档**: `http://api.test.example.com:8080/swagger-ui.html`

#### SSH访问
```bash
ssh deploy@test-server.example.com -p 22
# 用户名: test_user
# 密码: 由管理员分配
```

#### 数据库访问
```bash
# MySQL
mysql -h test-db.example.com -P 3306 -u test_user -p

# Redis
redis-cli -h test-redis.example.com -p 6379
```

### 1.3 首次登录流程

1. **获取账户信息**
   - 向管理员申请测试账号
   - 接收账户和初始密码邮件

2. **首次登录**
   ```
   访问 http://test.example.com:8080
   输入用户名和密码
   完成首次登录验证
   ```

3. **修改密码**
   - 登录后立即修改初始密码
   - 建议使用强密码（8位以上，包含大小写字母、数字）

4. **配置个人偏好**
   - 设置语言偏好（中文/英文）
   - 配置通知方式（邮件/企业微信）

### 1.4 快速验证测试环境

运行一键验证脚本：
```bash
cd /opt/ai-ready/scripts/validation
./quick-validate.sh
```

预期输出：
```
✅ 容器状态检查: PASS
✅ 端口可用性检查: PASS  
✅ 健康检查: PASS
✅ 数据库连接检查: PASS
✅ API可用性检查: PASS
✅ 日志检查: PASS
✅ 资源使用检查: PASS
```

---

## 2. 用户操作指南

### 2.1 订单管理操作

#### 创建订单
1. 登录系统，进入「订单管理」模块
2. 点击「新建订单」按钮
3. 填写订单信息：
   - 订单类型（销售订单/采购订单）
   - 客户/供应商信息
   - 产品清单和数量
   - 交货日期和地址
4. 点击「提交」保存订单
5. 系统生成订单号（格式: ORD-YYYYMMDD-XXXX）

#### 审批订单
1. 进入「订单审批」页面
2. 选择待审批订单
3. 查看订单详情和AI审批建议：
   - AI风险评估报告
   - 历史相似订单对比
   - 智能审批建议（批准/驳回/人工复核）
4. 执行审批操作：
   - 批准：订单进入执行阶段
   - 驳回：订单返回修改
   - 人工复核：转交高级审批人

#### 查询订单
- **订单列表查询**: 使用筛选条件（日期、状态、类型）
- **订单详情查看**: 点击订单号查看完整信息
- **订单跟踪**: 查看订单执行进度和状态变更历史

### 2.2 库存管理操作

#### 库存查询
```sql
-- 查询指定产品库存
SELECT product_id, product_name, stock_quantity, warehouse_location
FROM inventory
WHERE product_id = 'PRD-001';

-- 查询预警库存
SELECT * FROM inventory WHERE stock_quantity < safety_stock;
```

#### 库存调拨
1. 进入「库存调拨」模块
2. 选择源仓库和目标仓库
3. 输入调拨产品清单和数量
4. 提交调拨申请
5. 等待审批（如需）或直接执行

#### 库存盘点
1. 创建盘点计划
2. 执行实物盘点
3. 输入盘点结果
4. 系统对比账面数量和实物数量
5. 处理盘盈盘亏

### 2.3 采购管理操作

#### 创建采购申请
1. 进入「采购管理」模块
2. 点击「新建采购申请」
3. 填写采购需求：
   - 采购类型（原材料/办公用品/设备）
   - 产品清单和数量
   - 预估价格
   - 供应商建议
4. 提交采购申请
5. 等待审批流程

#### 供应商管理
- **新增供应商**: 填写供应商基本信息和资质
- **供应商评价**: 查看历史合作记录和质量评分
- **供应商选择**: 基于价格、质量、交货时间综合评估

### 2.4 财务结算操作

#### 成本核算
- 订单成本计算：自动汇总订单相关成本
- 项目成本统计：按项目维度统计成本
- 成本报表导出：导出Excel或PDF格式报表

#### 应收应付管理
- **应收账款**: 客户欠款记录和催收状态
- **应付账款**: 供应商付款记录和账期管理
- **账龄分析**: 按账龄分组统计应收应付

#### 财务报表
- 每日财务汇总报表
- 月度成本分析报表
- 季度财务结算报表

### 2.5 AI审批助手使用

#### 查看AI审批建议
订单审批页面自动显示AI分析结果：
```
AI审批建议：
- 风险等级: 中等
- 历史相似订单: 12个
- 批准成功率: 85%
- 建议: 批准（需关注交货日期）
```

#### AI审批报告详情
点击「查看详细分析」查看：
- 客户信用评估
- 产品库存匹配度
- 交货时间可行性
- 历史订单对比分析
- 风险因素识别

---

## 3. 管理员配置指南

### 3.1 系统配置

#### 用户管理
```bash
# 添加新用户
cd /opt/ai-ready/scripts/user-management
./add-user.sh --username newuser --role tester

# 修改用户权限
./update-user.sh --username newuser --permissions "order_view,inventory_edit"

# 删除用户
./delete-user.sh --username olduser
```

#### 权限配置
权限级别：
- **admin**: 系统管理员（全部权限）
- **manager**: 业务主管（审批权限）
- **operator**: 业务操作员（读写权限）
- **viewer**: 查看权限（只读）

#### 系统参数配置
修改配置文件 `/opt/ai-ready/config/application.yml`：
```yaml
# 系统参数
system:
  max-order-amount: 1000000    # 最大订单金额
  approval-auto-threshold: 0.9  # AI自动审批阈值
  inventory-warning-level: 10   # 库存预警阈值

# 监控参数
monitoring:
  enabled: true
  retention-days: 30
  alert-channels:
    - email
    - wechat
```

### 3.2 数据库管理

#### 数据库备份
```bash
# 手动备份
mysqldump -h test-db -u backup_user -p ai_ready > backup_$(date +%Y%m%d).sql

# 自动备份脚本
cd /opt/ai-ready/scripts/database
./backup-schedule.sh --daily
```

#### 数据库恢复
```bash
mysql -h test-db -u restore_user -p ai_ready < backup_20260427.sql
```

#### 数据库清理
```bash
# 清理测试数据（保留配置数据）
./cleanup-test-data.sh --keep-config

# 完全重置数据库（慎用）
./reset-database.sh --confirm
```

### 3.3 监控告警配置

#### Prometheus配置
编辑 `/opt/ai-ready/config/prometheus.yml`：
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ai-ready-services'
    static_configs:
      - targets: ['localhost:8080', 'localhost:8081', 'localhost:8082']

rule_files:
  - '/opt/ai-ready/config/alert-rules.yml'
```

#### 告警规则配置
编辑 `/opt/ai-ready/config/alert-rules.yml`：
```yaml
groups:
  - name: service_health
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.job }} 已宕机"

      - alert: HighMemoryUsage
        expr: process_resident_memory_bytes > 1GB
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "内存使用过高"
```

#### Grafana仪表盘配置
访问 Grafana `http://grafana.test.example.com:3000`：
1. 登录（admin/admin123）
2. 导入仪表盘模板
3. 配置数据源（Prometheus）
4. 设置告警通知通道

### 3.4 日志管理

#### 日志配置
编辑 `/opt/ai-ready/config/logback.xml`：
```xml
<configuration>
  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>/var/log/ai-ready/application.log</file>
    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger - %msg%n</pattern>
    </encoder>
  </appender>
  
  <root level="INFO">
    <appender-ref ref="FILE"/>
  </root>
</configuration>
```

#### 日志清理
```bash
# 保留最近7天日志
find /var/log/ai-ready -name "*.log" -mtime +7 -delete

# 压缩归档日志
cd /var/log/ai-ready/archive
gzip *.log
```

---

## 4. 常用操作说明

### 4.1 服务管理

#### 启动服务
```bash
# 启动全部服务
cd /opt/ai-ready
docker-compose up -d

# 启动指定服务
docker-compose up -d order-service inventory-service
```

#### 停止服务
```bash
# 停止全部服务
docker-compose down

# 停止指定服务
docker-compose stop order-service
```

#### 重启服务
```bash
# 重启全部服务
docker-compose restart

# 重启指定服务
docker-compose restart order-service
```

#### 查看服务状态
```bash
# Docker服务状态
docker-compose ps

# 服务健康检查
curl http://localhost:8080/actuator/health
```

### 4.2 数据管理

#### 导入测试数据
```bash
cd /opt/ai-ready/test-data
./import-test-data.sh --all
```

#### 导出测试数据
```bash
./export-test-data.sh --format json --output /tmp/test-data-export.json
```

#### 清理测试数据
```bash
./cleanup-test-data.sh --scope orders
./cleanup-test-data.sh --scope inventory
./cleanup-test-data.sh --scope all
```

### 4.3 监控查询

#### 查看实时监控
访问 Grafana: `http://grafana.test.example.com:3000`

常用仪表盘：
- **系统概览**: CPU、内存、磁盘使用率
- **服务性能**: API响应时间、请求成功率
- **业务监控**: 订单处理速度、库存周转率

#### 查看告警历史
```bash
# 查看近期告警
curl http://localhost:9093/api/v1/alerts

# 查看告警历史
curl http://localhost:9093/api/v1/alerts/groups
```

### 4.4 故障恢复

#### 服务异常恢复
```bash
# 检查服务日志
docker-compose logs order-service

# 重启异常服务
docker-compose restart order-service

# 如重启失败，重建容器
docker-compose up -d --force-recreate order-service
```

#### 数据库异常恢复
```bash
# 检查数据库连接
mysql -h test-db -u test_user -p -e "SHOW PROCESSLIST"

# 如连接异常，重启数据库
docker-compose restart mysql

# 从备份恢复数据
./restore-database.sh --backup backup_20260427.sql
```

---

## 5. 附录

### 5.1 常用命令速查表

| 操作 | 命令 |
|------|------|
| 启动服务 | `docker-compose up -d` |
| 停止服务 | `docker-compose down` |
| 重启服务 | `docker-compose restart` |
| 查看日志 | `docker-compose logs [service]` |
| 健康检查 | `curl http://localhost:8080/actuator/health` |
| 一键验证 | `./quick-validate.sh` |
| 数据备份 | `mysqldump -h test-db -u backup -p ai_ready > backup.sql` |

### 5.2 端口映射表

| 服务 | 容器端口 | 外部端口 | 说明 |
|------|----------|----------|------|
| 前端服务 | 80 | 8080 | Web界面 |
| 后端API | 8080 | 8080 | RESTful API |
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存服务 |
| Prometheus | 9090 | 9090 | 监控系统 |
| Grafana | 3000 | 3000 | 监控大盘 |
| AlertManager | 9093 | 9093 | 告警管理 |

### 5.3 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DB_HOST | 数据库主机 | test-db.example.com |
| DB_PORT | 数据库端口 | 3306 |
| DB_USER | 数据库用户 | test_user |
| REDIS_HOST | Redis主机 | test-redis.example.com |
| API_PORT | API端口 | 8080 |
| LOG_LEVEL | 日志级别 | INFO |
| MONITOR_ENABLED | 监控开关 | true |

### 5.4 常见问题快速解决

| 问题 | 解决方案 |
|------|----------|
| 无法登录 | 检查用户名密码，联系管理员重置 |
| 服务无响应 | 检查容器状态，重启服务 |
| 数据库连接失败 | 检查数据库配置，重启MySQL容器 |
| 监控数据缺失 | 检查Prometheus配置，重启监控服务 |
| 告警未发送 | 检查AlertManager配置，验证通知通道 |

---

**文档版本**: v1.0  
**最后更新**: 2026-04-27  
**维护团队**: AI-Ready测试团队  
**联系方式**: test-support@example.com