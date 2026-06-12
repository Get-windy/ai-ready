# ERP 系统自动化测试报告 - 第二轮迭代

## 测试概览

| 指标 | 第一轮 | 第二轮 |
|------|--------|--------|
| 总页面数 | 128 | 128 |
| 测试通过 | 114 | 128 |
| 测试失败 | 14 | 0 |
| 成功率 | 89.1% | 100% |

## 本次修复内容

### 1. PostgreSQL 日期类型不匹配问题（已修复）

**问题**: LambdaQueryWrapper 直接使用字符串日期与 timestamp/date 列比较，导致 PostgreSQL 类型转换错误。

**修复文件**:
- `PurchaseExchangeServiceImpl.java` - pageList/exportList 方法
- `SaleExchangeServiceImpl.java` - pageList/exportList 方法
- `SaleOrderServiceImpl.java` - pageOrders 方法

**修复方案**: 将 String 转换为 LocalDateTime/LocalDate 后使用。

### 2. tenantId Header 缺失问题（本次修复）

**问题**: 打印模块 V2 控制器使用必传的 `@RequestHeader Long tenantId`，前端请求时缺少此 header。

**修复文件**:
- `PrintTemplateV2Controller.java`
- `PrintChainController.java`
- `PrintClientController.java`

**修复方案**:
- 将 `@RequestHeader Long tenantId` 改为 `@RequestHeader(required = false) Long tenantId`
- 添加 `resolveTenantId()` 方法从 Sa-Token session 获取 tenantId 作为 fallback
- 添加 `resolveUserId()` 方法从 Sa-Token 获取 userId

### 3. 测试脚本 tenantId 设置（本次修复）

**问题**: API 登录后未设置 localStorage.tenantId。

**修复文件**: `erp-auto-test.js`

**修复方案**: 在 API 登录成功后添加 localStorage.setItem('tenantId', tenantId)

## 错误统计变化

| 错误类型 | 第一轮 | 第二轮 |
|----------|--------|--------|
| BadSqlGrammarException | 254 | 0 |
| MissingRequestHeaderException | 54 | 0 |
| AsyncRequestTimeoutException | 167 | 正常（SSE超时） |
| ServletException | 3 | 0 |

## 测试环境

- 前端: http://localhost:5656 (Vue 3 + Vite)
- 后端: http://localhost:5655 (Spring Boot 3.2.5)
- 登录: admin/admin123, 租户 SYSTEM
- 测试时间: 2026-06-12 21:15 - 21:20

## 结论

所有 128 个页面已通过自动化测试，系统运行稳定。本次迭代主要解决了：
1. PostgreSQL 日期类型兼容问题
2. tenantId header 必传导致的请求失败问题
3. 测试脚本 localStorage 配置问题

---

**报告生成时间**: 2026-06-12 21:22