# ERP系统自动化测试报告 - 第一阶段

## 测试时间
2026-06-12 08:30 - 09:05

## 系统状态

### 前端状态
- **状态**: 正常运行
- **端口**: 5656
- **进程**: node.exe (PID 40308)
- **测试结果**: 所有前端页面返回 200，页面内容正常

### 后端状态
- **状态**: 运行中但有错误
- **端口**: 5655  
- **进程**: java.exe (PID 51748)
- **主要问题**: 
  - 数据库表 `sys_screenshot_task` 不存在
  - RabbitMQ 未运行 (连接被拒绝)
  - 登录 API 返回 500 错误

## 发现的错误

### 1. 数据库错误 (阻断级)
- **错误**: `sys_screenshot_task` 表不存在
- **位置**: `cn/aiedge/erp/printing/mapper/SysScreenshotTaskMapper.java`
- **影响**: 定时任务 `ScreenshotAutoConfirmScheduler` 每分钟执行失败
- **修复方案**: 创建 Flyway 迁移脚本 `V6.7.0__Fix_Missing_Screenshot_Task_Table.sql`

### 2. 登录错误 (阻断级)
- **错误**: 登录 API 返回 500 (系统异常)
- **可能原因**: Redis 连接问题或 LoginAttemptService 错误
- **测试结果**: 
  - 验证码接口正常: `/api/auth/captcha` 返回 200
  - 登录接口失败: `/api/auth/login` 返回 500
  - 租户名 "SYSTEM" 存在但登录失败

### 3. RabbitMQ 错误 (非阻断)
- **错误**: Connection refused (端口未监听)
- **影响**: 消息队列功能不可用，但不影响核心功能
- **建议**: 启动 RabbitMQ 或在开发环境禁用

### 4. API 路径问题
- **错误**: `/api/crm/customer/list` 返回 404
- **正确路径**: `/api/customer/page`
- **原因**: CRM 模块使用 `/api/customer` 前缀

## 前端页面测试结果

| 页面路径 | 状态 | 说明 |
|---------|------|------|
| `/` | 200 | 正常 |
| `/login` | 200 | 正常 |
| `/dashboard` | 200 | 正常 |
| `/sale` | 200 | 正常 |
| `/purchase` | 200 | 正常 |
| `/stock` | 200 | 正常 |
| `/erp/product` | 200 | 正常 |
| `/crm/customer` | 200 | 正常 |
| `/finance` | 200 | 正常 |
| `/system/user` | 200 | 正常 |

## 已完成的修复

1. **创建迁移脚本**: `V6.7.0__Fix_Missing_Screenshot_Task_Table.sql`
   - 位置: `i:/AI-Ready/backend/core/api/core-api/src/main/resources/db/migration/`
   - 需要重新编译后端项目才能生效

## 待处理任务

1. **重新编译后端**: 执行 `mvn package` 让 Flyway 迁移生效
2. **启动 RabbitMQ**: 或禁用 RabbitMQ 相关配置
3. **修复登录问题**: 检查 Redis 连接和 LoginAttemptService
4. **修复 API 路径**: 前端可能使用了错误的 CRM API 路径
5. **完整自动化测试**: 使用 Playwright 进行完整的浏览器测试

## 建议的下一步操作

```bash
# 1. 重新编译后端
cd i:/AI-Ready/backend
mvn clean package -DskipTests -pl core/api/core-api -am

# 2. 重新启动后端
taskkill /F /IM java.exe
java -jar core/api/core-api/target/core-api-0.2.0-exec.jar --spring.profiles.active=dev

# 3. 启动 RabbitMQ (可选)
rabbitmq-server

# 4. 运行完整测试
cd i:/AI-Ready/frontend
pnpm exec playwright test tests/e2e/full-system-scan.spec.ts
```

## 总结

- **前端**: 10/10 页面正常加载
- **后端**: 需要修复数据库表和登录问题
- **阻断问题**: 2个 (数据库表缺失、登录失败)
- **非阻断问题**: 1个 (RabbitMQ 未运行)
- **已创建修复**: 1个 (迁移脚本待编译生效)