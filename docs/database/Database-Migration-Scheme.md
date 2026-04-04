# AI-Ready 数据库迁移方案文档

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-03-30  
> **维护者**: devops-engineer

---

## 目录

1. [概述](#概述)
2. [迁移方案](#迁移方案)
3. [迁移脚本](#迁移脚本)
4. [部署说明](#部署说明)
5. [回滚指南](#回滚指南)

---

## 概述

### 迁移目标

- **工具选择**: Flyway (推荐) / Liquibase
- **数据库**: PostgreSQL 14+ / MySQL 8.0+
- **版本管理**: 语义化版本控制
- **CI/CD集成**: GitHub Actions + Flyway Maven插件

### 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| V1.0.0 | 2026-03-30 | 初始表结构 (40+表) |
| V1.0.1 | 2026-03-30 | 监控监控表 (9表) |
| V1.0.2 | 2026-03-30 | 业务表 (12表) |

---

## 迁移方案

### 技术选型

| 特性 | Flyway | Liquibase |
|------|--------|-----------|
| 学习曲线 | 简单 | 较复杂 |
| SQL支持 | 原生 | XML/YAML/JSON |
| 版本控制 | 顺序 | 分支合并 |
| 文档 | 简洁 | 丰富 |
| Maven集成 | ✅ 内置 | ✅ 内置 |
| 推荐度 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

### 推荐方案: Flyway

#### 项目结构

```
src/main/resources/
├── application.yml
├── application-flyway.yml    # Flyway 配置
└── db/
    └── migration/
        ├── V1.0.0__Initial_Schema.sql
        ├── V1.0.1__Add_Monitoring_Tables.sql
        ├── V1.0.2__Add_Business_Tables.sql
        ├── V1.0.3__Data_Migration.sql
        ├── V1.0.3__Data_Migration__rollback.sql
        ├── V1.0.2__Add_Business_Tables__rollback.sql
        ├── V1.0.1__Add_Monitoring_Tables__rollback.sql
        └── V999.999__Migration_Utility.sql
```

#### 配置项

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    table: schema_version
    validate-on-migrate: true
```

---

## 迁移脚本

### V1.0.0 - Initial Schema

**包含表**:
- sys_tenant (租户表)
- sys_user (用户表)
- sys_role (角色表)
- sys_permission (权限表)
- sys_user_role (用户角色)
- sys_role_permission (角色权限)
- sys_dept (部门表)
- sys_post (岗位表)
- sys_dict (字典表)
- sys_dict_item (字典项)
- sys_log (系统日志)
- sys_operation_log (操作日志)
- sys_config (系统配置)
- sys_notice (通知公告)
- sys_message (消息中心)
- sys_file (文件表)
- sys_audit_log (审计日志)
- sys_job (定时任务)
- sys_job_log (定时任务日志)

**表统计**: 19个核心表

### V1.0.1 - Add Monitoring Tables

**新增表**:
- sys_metrics (指标数据)
- sys_operation_metrics (操作指标)
- sys_db_metrics (数据库指标)
- sys_cache_metrics (缓存指标)
- sys_server_metrics (服务器指标)
- sys_trace (链路追踪)
- sys_event_log (事件日志)
- sys_alarm_rule (告警规则)
- sys_alarm_record (告警记录)

**表统计**: 9个监控表

### V1.0.2 - Add Business Tables

**新增表**:
- biz_customer (客户)
- biz_supplier (供应商)
- biz_product (商品)
- biz_order (订单)
- biz_order_item (订单明细)
- biz_invoice (发票)
- biz_payment (支付)
- biz_warehouse (仓库)
- biz_stock (库存)
- biz_stock_log (库存变动)
- biz_sales_report (销售统计)
- biz_purchase_report (采购统计)

**表统计**: 12个业务表

### V1.0.3 - Data Migration

**包含数据**:
- 默认租户 (tenant_id=1)
- 默认用户 (admin, devops)
- 默认角色 (super-admin, admin, devops, developer)
- 默认权限 (系统管理, 用户管理, 角色管理等)
- 用户角色关联
- 角色权限关联
- 字典数据 (客户类型, 商品类型, 订单状态等)
- 系统配置
- 通知公告

---

## 部署说明

### Maven配置

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-maven-plugin</artifactId>
            <version>10.0.1</version>
            <configuration>
                <url>jdbc:postgresql://localhost:5432/ai_ready</url>
                <user>ai_ready_user</user>
                <password>your_password</password>
                <locations>
                    <location>filesystem:src/main/resources/db/migration</location>
                </locations>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Maven命令

```bash
# 查看当前版本
mvn flyway:info

# 执行迁移
mvn flyway:migrate

# 回滚到指定版本
mvn flyway:undo -Dflyway.version=1.0.2

# 清理数据库
mvn flyway:clean

# 验证迁移
mvn flyway:validate
```

### CI/CD集成

```yaml
# .github/workflows/ci.yml
jobs:
  flyway-migrate:
    name: Flyway Database Migration
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      
      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-
      
      - name: Run Flyway Migrate
        run: mvn flyway:migrate -Dflyway.url=${{ env.DATABASE_URL }} -Dflyway.user=${{ env.DATABASE_USER }} -Dflyway.password=${{ env.DATABASE_PASSWORD }}
        env:
          DATABASE_URL: ${{ secrets.DATABASE_URL }}
          DATABASE_USER: ${{ secrets.DATABASE_USER }}
          DATABASE_PASSWORD: ${{ secrets.DATABASE_PASSWORD }}
```

---

## 回滚指南

### 回滚策略

| 策略 | 适用场景 | 命令 |
|------|----------|------|
| Flyway Undo | 开发/测试环境 | `mvn flyway:undo` |
| Manual SQL | 生产环境 | 手动执行rollback脚本 |
| Backup Restore | 严重问题 | 从备份恢复 |

### FlywayUndo使用

```bash
# 回滚到上一个版本
mvn flyway:undo

# 回滚到指定版本
mvn flyway:undo -Dflyway.version=1.0.2

# 回滚所有版本
mvn flyway:undoAll
```

### 手动回滚步骤

1. **备份当前数据库**
   ```bash
   pg_dump -U ai_ready_user ai_ready > backup-$(date +%Y%m%d).sql
   ```

2. **执行回滚脚本**
   ```bash
   psql -U ai_ready_user ai_ready < V1.0.2__Add_Business_Tables__rollback.sql
   ```

3. **更新Flyway历史表**
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '1.0.2';
   ```

4. **验证数据库状态**
   ```bash
   mvn flyway:info
   ```

### 回滚检查清单

```markdown
## 回滚前检查

- [ ] 备份已完成
- [ ] 回滚脚本已审核
- [ ] 回滚影响已评估
- [ ] 相关方已通知

## 回滚后验证

- [ ] 数据库连接正常
- [ ] 表结构符合预期
- [ ] 数据完整性检查
- [ ] 应用可以启动
```

---

## 最佳实践

### 迁移最佳实践

1. **版本递增**: 严格递增的版本号
2. **向后兼容**: 新增字段/表，避免修改删除
3. **数据迁移**: 分批迁移，大表加索引
4. **测试先行**: 本地测试通过再部署
5. **备份保证**: 生产环境 mandatory backup

### 安全最佳实践

1. **敏感数据**: 加密存储密码
2. **权限控制**: 最小权限原则
3. **审计日志**: 记录所有迁移操作
4. **版本锁定**: 避免并发执行

### 监控指标

| 指标 | 阈值 | 告警级别 |
|------|------|----------|
| Migration Duration | >5分钟 | Warning |
| Migration Failures | 任何失败 | Critical |
| Schema Mismatch | 不匹配 | Critical |
|数据迁移速度 | <100行/秒 | Warning |

---

## 故障排查

### 常见问题

#### 1. Flyway验证失败

```
Error: Validate failed: Migration checksum mismatch
```

**解决方案**:
```bash
mvn flyway:repair
```

#### 2. 迁移脚本执行失败

```
Error: Syntax error in SQL statement
```

**解决方案**:
1. 检查SQL语法
2. 在数据库中手动测试
3. 使用rollback脚本回滚

#### 3. 数据库连接失败

```
Error: Connection refused
```

**解决方案**:
1. 检查数据库服务状态
2. 检查连接参数
3. 检查网络防火墙

### 调试命令

```bash
# 查看所有迁移信息
mvn flyway:info

# 显示详细信息
mvn flyway:info -X

# 强制验证
mvn flyway:validate -Dflyway.validateMigrationMode=always

# 详细输出
mvn flyway:migrate -X
```

---

## 附录

### 配置文件清单

| 文件 | 位置 | 大小 |
|------|------|------|
| application-flyway.yml | src/main/resources/ | 0.5KB |
| V1.0.0__Initial_Schema.sql | src/main/resources/db/migration/ | 14KB |
| V1.0.1__Add_Monitoring_Tables.sql | src/main/resources/db/migration/ | 7KB |
| V1.0.2__Add_Business_Tables.sql | src/main/resources/db/migration/ | 10KB |
| V1.0.3__Data_Migration.sql | src/main/resources/db/migration/ | 8KB |
| Rollback scripts | src/main/resources/db/migration/ | 6KB |
| Migration_Utility.sql | src/main/resources/db/migration/ | 3KB |

### 常用SQL查询

```sql
-- 查看迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_on DESC;

-- 查看所有表
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;

-- 查看表大小
SELECT 
    relname as table_name,
    pg_size_pretty(pg_total_relation_size(relid)) as total_size,
    pg_size_pretty(pg_relation_size(relid)) as data_size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;

-- 查看表记录数
SELECT 
    relname as table_name,
    n_live_tup as row_count
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;
```

### 迁移脚本命名规范

```
格式: V<VERSION>__<DESCRIPTION>.sql
示例: V1.0.0__Initial_Schema.sql

回滚脚本: V<VERSION>__<DESCRIPTION>__rollback.sql
示例: V1.0.0__Initial_Schema__rollback.sql

可重复脚本: R<VERSION>__<DESCRIPTION>.sql
示例: R1.0.0__Troubleshooting_Guide.sql
```

---

*文档由 devops-engineer 自动生成和维护*
*最后更新: 2026-03-30*