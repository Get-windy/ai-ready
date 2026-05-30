# AI-Ready 后端文档

## 模块结构

```
backend/
├── core/            # 核心模块
│   ├── base/        # 基础模块（用户、权限、菜单、字典、操作日志）
│   ├── common/      # 公共模块（常量、枚举、DTO、异常）
│   ├── api/         # API 模块（应用入口、调度、导出、通知、集成）
│   ├── web-admin/   # Web 管理模块（菜单、配置、财务凭证）
│   ├── agent/       # AI Agent 模块
│   ├── custom-field/ # 自定义字段模块
│   ├── automation/  # 自动化模块
│   ├── record-permission/ # 记录权限模块
│   ├── kanban/      # 看板模块
│   ├── webhook/     # Webhook 模块
│   └── monitoring/  # 监控模块
├── erp/             # ERP 业务模块
│   ├── purchase/    # 采购管理
│   ├── sales/       # 销售管理
│   ├── erp-stock/   # 库存管理
│   ├── finance/     # 财务管理
│   ├── invoice/     # 发票管理
│   ├── party/       # 往来单位
│   └── ...共22个模块
├── crm/             # CRM 业务模块（4个）
├── user/            # 用户服务模块
├── infrastructure/  # 基础设施（Docker/K8s/监控/日志）
├── tests/           # 测试框架
├── scripts/         # 运维脚本
├── sql/             # 数据库初始化脚本
└── tools/           # 工具模块
```

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.5 | 应用框架 |
| MyBatis-Plus | 3.5.10 | ORM 框架 |
| PostgreSQL | 15.x | 关系型数据库 |
| Redis | 7.x | 缓存 / Token |
| Sa-Token | 1.37.0 | 认证授权 |
| Flyway | 10.8 | 数据库版本管理 |
| Drools | 8.x | 规则引擎 |
| Knife4j | 4.5 | API 文档 |
| XXL-Job | 2.4 | 分布式任务调度 |

## 快速启动

```bash
# 1. 初始化数据库
psql -U postgres -f backend/sql/00_init_all.sql

# 2. 启动应用
cd backend
mvn clean install -DskipTests
java -jar core/api/core-api/target/*-exec.jar --spring.profiles.active=dev

# 3. 访问 API 文档
open http://localhost:8080/doc.html
```

## 安全机制

- **认证**: Sa-Token JWT 模式
- **授权**: @SaCheckPermission 注解控制接口权限
- **SQL注入防护**: AOP 切面 + Filter 双重防护
- **XSS 防护**: 请求参数清洗 + HTML 转义
- **密码加密**: BCrypt (Spring Security PasswordEncoder)
- **速率限制**: 基于 Redis 的令牌桶限流
- **操作审计**: @OperLog 注解 + AOP 自动记录
- **多租户**: 所有表含 tenant_id 隔离
- **乐观锁**: BaseEntity @Version 并发控制
