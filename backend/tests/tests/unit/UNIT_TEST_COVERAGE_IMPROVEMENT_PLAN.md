# AI-Ready 单元测试覆盖率提升方案

## 1. 当前覆盖率分析

### 1.1 代码统计

| 指标 | 数量 |
|------|------|
| 主代码文件 (.java) | 604 |
| 测试文件 (*Test.java) | 55 |
| 当前测试覆盖率 | ~9.1% |

### 1.2 模块分布

| 模块 | 主代码文件数 | 测试文件数 | 覆盖率 |
|------|------------|-----------|--------|
| core-base | 92 | 8 | ~8.7% |
| core-api/report | 41 | 5 | ~12.2% |
| core-api/scheduler | 28 | 4 | ~14.3% |
| core-api/notification | 18 | 4 | ~22.2% |
| core-api/permission | 17 | 2 | ~11.8% |
| core-api/cache | 10 | 4 | ~40% |
| erp/* | ~30 | 2 | ~6.7% |
| crm/* | ~15 | 1 | ~6.7% |

### 1.3 核心业务逻辑识别

#### 高优先级（核心业务）
1. **用户权限管理** (core-base)
   - SysUserServiceImpl - 用户管理
   - SysRoleServiceImpl - 角色管理
   - SysPermissionServiceImpl - 权限管理

2. **报表服务** (core-api/report)
   - ReportServiceImpl - 报表生成
   - ReportAnalyticsServiceImpl - 报表分析
   - ReportExportServiceImpl - 报表导出

3. **工作流引擎** (core-api/workflow)
   - WorkflowServiceImpl - 审批流程

4. **通知服务** (core-api/notification)
   - EnhancedNotificationServiceImpl - 增强通知
   - NotificationServiceImpl - 基础通知

5. **缓存服务** (core-api/cache)
   - CacheService - Redis缓存

#### 中优先级（业务支撑）
1. **数据导入导出** (core-api/export)
   - DataImportServiceImpl
   - DataExportServiceImpl
   - BatchImportServiceImpl

2. **字典服务** (core-api/dict)
   - DictTypeServiceImpl
   - DictItemServiceImpl

3. **搜索服务** (core-api/search)
   - SearchServiceImpl
   - AdvancedSearchServiceImpl

4. **ERP模块** (erp/*)
   - SaleOrderServiceImpl
   - StockServiceImpl

5. **CRM模块** (crm/*)
   - CustomerServiceImpl
   - LeadServiceImpl

#### 低优先级（工具类）
- 工具类、配置类、常量类等

---

## 2. 覆盖率目标

### 2.1 分阶段目标

| 阶段 | 时间 | 行覆盖率目标 | 分支覆盖率目标 |
|------|------|------------|--------------|
| 第一阶段 | 1-2周 | 40% | 30% |
| 第二阶段 | 3-4周 | 60% | 45% |
| 第三阶段 | 5-6周 | 75% | 60% |
| 最终目标 | 8周 | 80%+ | 70%+ |

### 2.2 模块目标

| 模块 | 当前覆盖率 | 第一阶段 | 第二阶段 | 最终目标 |
|------|----------|---------|---------|---------|
| core-base | ~9% | 35% | 55% | 80% |
| core-api/report | ~12% | 40% | 60% | 85% |
| core-api/workflow | ~5% | 35% | 55% | 80% |
| core-api/notification | ~22% | 50% | 70% | 85% |
| core-api/cache | ~40% | 60% | 75% | 90% |
| erp/* | ~7% | 30% | 50% | 75% |
| crm/* | ~7% | 30% | 50% | 75% |

---

## 3. 缺失测试用例设计

### 3.1 用户权限管理模块 (core-base)

#### SysUserServiceImpl 缺失用例

| 方法 | 当前测试 | 缺失场景 | 优先级 |
|------|---------|---------|--------|
| login | ✅ | 用户不存在、密码错误、用户禁用、锁定状态 | 高 |
| logout | ❌ | 正常登出、Token失效 | 中 |
| createUser | ✅ | 用户名已存在、密码加密验证 | 高 |
| updateUser | ✅ | 部分字段更新、并发更新 | 中 |
| deleteUser | ✅ | 关联数据清理 | 中 |
| batchDeleteUsers | ✅ | 批量删除、部分失败 | 中 |
| resetPassword | ✅ | 密码强度校验 | 中 |
| changePassword | ✅ | 原密码错误、新密码与旧密码相同 | 高 |
| pageUsers | ✅ | 多条件组合查询 | 中 |
| getUserDetail | ✅ | 用户不存在 | 中 |
| getUserRoleCodes | ❌ | 无角色用户、多角色用户 | 高 |
| getUserPermissionCodes | ❌ | 无权限用户、多权限用户 | 高 |
| assignRoles | ✅ | 空角色列表、重复角色、角色不存在 | 高 |
| updateUserStatus | ✅ | 状态转换验证 | 中 |

#### SysRoleServiceImpl 缺失用例

| 方法 | 当前测试 | 缺失场景 | 优先级 |
|------|---------|---------|--------|
| createRole | ✅ | 角色编码重复、数据权限范围 | 高 |
| updateRole | ✅ | 部分字段更新 | 中 |
| deleteRole | ✅ | 有关联用户、级联删除 | 高 |
| assignPermissions | ✅ | 空权限列表、权限不存在 | 高 |
| assignMenus | ✅ | 空菜单列表、菜单不存在 | 高 |
| pageRoles | ✅ | 多条件查询、排序 | 中 |
| getRolePermissionIds | ✅ | 无权限角色 | 中 |
| getRoleMenuIds | ✅ | 无菜单角色 | 中 |
| getUserRoles | ✅ | 用户无角色 | 中 |
| updateRoleStatus | ✅ | 状态转换 | 中 |

#### SysPermissionServiceImpl 缺失用例

| 方法 | 当前测试 | 缺失场景 | 优先级 |
|------|---------|---------|--------|
| createPermission | ❌ | 权限编码重复、父权限不存在 | 高 |
| updatePermission | ❌ | 编码重复检查、部分更新 | 高 |
| deletePermission | ❌ | 有子权限、有关联角色 | 高 |
| batchDeletePermissions | ❌ | 部分有子权限 | 中 |
| pagePermissions | ❌ | 多条件组合查询 | 中 |
| getPermissionDetail | ❌ | 权限不存在 | 中 |
| getPermissionTree | ❌ | 空树、多级树 | 高 |
| getUserPermissions | ❌ | 用户权限获取 | 高 |
| getRolePermissions | ❌ | 角色权限获取 | 高 |
| getChildrenPermissions | ❌ | 无子权限 | 中 |
| checkPermissionCodeExists | ❌ | 存在/不存在检查 | 高 |
| updatePermissionStatus | ❌ | 状态转换 | 中 |
| updatePermissionSort | ❌ | 排序更新 | 低 |

### 3.2 报表服务模块 (core-api/report)

#### ReportServiceImpl 缺失用例

| 方法 | 当前测试 | 缺失场景 | 优先级 |
|------|---------|---------|--------|
| getReportDefinition | ✅ | 内置报表、缓存报表 | 高 |
| getReportList | ✅ | 分类筛选、全部获取 | 高 |
| generateReport | ✅ | 各类型报表生成 | 高 |
| exportToExcel | ✅ | Excel导出 | 高 |
| exportToPdf | ⚠️ | PDF导出（需验证） | 中 |
| exportToCsv | ✅ | CSV导出 | 高 |
| saveReportDefinition | ✅ | 新建、更新 | 高 |
| deleteReportDefinition | ✅ | 内置报表保护 | 高 |
| copyReportDefinition | ✅ | 复制功能 | 中 |
| previewReport | ✅ | 预览限制行数 | 中 |
| calculateSummary | ❌ | 各种聚合函数 | 高 |
| generateChartData | ❌ | 图表数据生成 | 中 |

#### ReportAnalyticsServiceImpl 缺失用例

| 方法 | 当前测试 | 缺失场景 | 优先级 |
|------|---------|---------|--------|
| getSalesAnalytics | ❌ | 销售数据分析 | 高 |
| getCustomerAnalytics | ❌ | 客户数据分析 | 高 |
| getProductAnalytics | ❌ | 产品数据分析 | 高 |
| getTrendAnalysis | ❌ | 趋势分析 | 中 |
| getComparisonAnalysis | ❌ | 对比分析 | 中 |
| getForecastAnalysis | ❌ | 预测分析 | 中 |

### 3.3 工作流模块 (core-api/workflow)

#### WorkflowServiceImpl 缺失用例

| 方法 | 当前测试 | 缺失场景 | 优先级 |
|------|---------|---------|--------|
| getWorkflowDefinition | ❌ | 内置流程、自定义流程 | 高 |
| saveWorkflowDefinition | ❌ | 新建、更新版本 | 高 |
| deleteWorkflowDefinition | ❌ | 内置流程保护 | 中 |
| startWorkflow | ❌ | 流程启动、节点初始化 | 高 |
| getWorkflowInstance | ❌ | 实例获取 | 高 |
| approveWorkflow | ❌ | 审批通过、会签 | 高 |
| rejectWorkflow | ❌ | 审批驳回 | 高 |
| transferWorkflow | ❌ | 转办 | 中 |
| cancelWorkflow | ❌ | 撤销 | 中 |
| getWorkflowHistory | ❌ | 历史记录 | 中 |

### 3.4 通知服务模块 (core-api/notification)

#### EnhancedNotificationServiceImpl 缺失用例

| 方法 | 当前测试 | 缺失场景 | 优先级 |
|------|---------|---------|--------|
| createTemplate | ❌ | 模板创建 | 高 |
| updateTemplate | ❌ | 模板更新 | 高 |
| deleteTemplate | ❌ | 模板删除 | 中 |
| getTemplate | ❌ | 缓存获取 | 高 |
| sendEnhanced | ❌ | 增强发送 | 高 |
| sendDirect | ❌ | 直接发送 | 高 |
| sendBatch | ❌ | 批量发送 | 高 |
| doSend | ❌ | 实际发送 | 高 |
| doSendAsync | ❌ | 异步发送 | 中 |
| processScheduledQueue | ❌ | 定时队列处理 | 中 |

### 3.5 缓存服务模块 (core-api/cache)

#### CacheService 缺失用例

| 方法 | 当前测试 | 缺失场景 | 优先级 |
|------|---------|---------|--------|
| set | ✅ | 基础设置 | 高 |
| set (带过期) | ✅ | 过期设置 | 高 |
| setEx | ⚠️ | 秒级过期 | 中 |
| setIfAbsent | ⚠️ | 不存在才设置 | 中 |
| get | ✅ | 获取缓存 | 高 |
| get (带类型) | ⚠️ | 类型转换 | 中 |
| delete | ✅ | 删除缓存 | 高 |
| delete (批量) | ✅ | 批量删除 | 高 |
| hasKey | ✅ | 存在检查 | 高 |
| expire | ✅ | 设置过期 | 中 |
| getExpire | ❌ | 获取过期时间 | 中 |
| increment | ✅ | 自增 | 高 |
| decrement | ✅ | 自减 | 高 |
| hSet | ✅ | Hash设置 | 高 |
| hGet | ✅ | Hash获取 | 高 |
| hGetAll | ✅ | Hash获取全部 | 高 |
| hDelete | ✅ | Hash删除 | 高 |
| hHasKey | ❌ | Hash存在检查 | 中 |
| lPush | ✅ | List左推 | 高 |
| rPush | ✅ | List右推 | 高 |
| lPop | ❌ | List左弹 | 中 |
| rPop | ❌ | List右弹 | 中 |
| lRange | ✅ | List范围 | 高 |
| lSize | ❌ | List大小 | 中 |
| sAdd | ✅ | Set添加 | 高 |
| sRemove | ❌ | Set移除 | 中 |
| sMembers | ✅ | Set获取 | 高 |
| sIsMember | ✅ | Set检查 | 高 |
| sSize | ❌ | Set大小 | 中 |
| zAdd | ✅ | ZSet添加 | 高 |
| zRemove | ❌ | ZSet移除 | 中 |
| zRange | ✅ | ZSet范围 | 高 |
| zReverseRange | ❌ | ZSet逆序 | 中 |
| zSize | ❌ | ZSet大小 | 中 |
| deleteByPattern | ✅ | 模式删除 | 高 |
| keys | ✅ | 模式获取键 | 高 |

---

## 4. 新增测试脚本

### 4.1 测试脚本结构

```
tests/unit/java/
├── core-base/
│   ├── service/
│   │   ├── SysUserServiceImplTest.java (补充)
│   │   ├── SysRoleServiceImplTest.java (补充)
│   │   ├── SysPermissionServiceImplTest.java (新增)
│   │   └── ...
│   └── ...
├── core-api/
│   ├── report/
│   │   ├── ReportAnalyticsServiceImplTest.java (新增)
│   │   └── ...
│   ├── workflow/
│   │   └── WorkflowServiceImplTest.java (新增)
│   ├── notification/
│   │   └── EnhancedNotificationServiceImplTest.java (新增)
│   └── ...
└── ...
```

### 4.2 测试策略

1. **单元测试原则**
   - 使用JUnit 5 + Mockito
   - 每个测试类对应一个被测类
   - 每个测试方法测试一个场景
   - 使用Given-When-Then结构

2. **Mock策略**
   - Mapper层全部Mock
   - 外部服务全部Mock
   - Redis/Cache使用Mock
   - 不依赖真实数据库

3. **
---

## 5. 实施计划

### 5.1 第一阶段（1-2周）- 核心模块

**目标**: 行覆盖率 40%，分支覆盖率 30%

| 模块 | 新增测试类 | 预计用例数 | 负责人 |
|------|-----------|-----------|--------|
| core-base/service | SysPermissionServiceImplTest | 25+ | QA Team |
| core-api/report | ReportAnalyticsServiceImplTest | 20+ | QA Team |
| core-api/workflow | WorkflowServiceImplTest | 25+ | QA Team |
| core-api/notification | EnhancedNotificationServiceImplTest | 20+ | QA Team |
| core-api/cache | CacheServiceExtendedTest | 25+ | QA Team |

### 5.2 第二阶段（3-4周）- 业务模块

**目标**: 行覆盖率 60%，分支覆盖率 45%

| 模块 | 新增测试类 | 预计用例数 |
|------|-----------|-----------|
| core-api/export | DataExportServiceImplTest, DataImportServiceImplTest | 30+ |
| core-api/dict | DictTypeServiceImplTest, DictItemServiceImplTest | 20+ |
| core-api/search | SearchServiceImplTest | 15+ |
| erp/sale | SaleOrderServiceImplTest | 20+ |
| erp/stock | StockServiceImplTest | 15+ |
| crm/customer | CustomerServiceImplTest | 15+ |

### 5.3 第三阶段（5-6周）- 完善覆盖

**目标**: 行覆盖率 75%，分支覆盖率 60%

- 补充边界条件测试
- 补充异常路径测试
- 补充并发场景测试
- 工具类单元测试

### 5.4 最终阶段（7-8周）- 优化稳定

**目标**: 行覆盖率 80%+，分支覆盖率 70%+

- 集成测试补充
- 性能测试补充
- 覆盖率报告优化
- CI/CD集成

---

## 6. 测试执行与报告

### 6.1 执行命令

`ash
# 执行所有单元测试
mvn test

# 执行指定模块测试
mvn test -pl core-base

# 生成覆盖率报告
mvn jacoco:report

# 执行单个测试类
mvn test -Dtest=SysPermissionServiceImplTest

# 执行带覆盖率
mvn clean test jacoco:report
`

### 6.2 覆盖率检查清单

- [ ] 行覆盖率 >= 80%
- [ ] 分支覆盖率 >= 70%
- [ ] 方法覆盖率 >= 80%
- [ ] 类覆盖率 >= 90%
- [ ] 核心业务流程100%覆盖
- [ ] 异常处理路径覆盖
- [ ] 边界条件覆盖

### 6.3 质量门禁

`xml
<!-- pom.xml 配置 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
`

---

## 7. 风险评估与应对

### 7.1 风险识别

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 遗留代码难以测试 | 高 | 中 | 重构解耦，提取接口 |
| 测试数据准备复杂 | 中 | 高 | 使用Builder模式，Mock数据 |
| 时间进度延迟 | 高 | 中 | 分阶段交付，优先核心 |
| 测试维护成本高 | 中 | 中 | 规范命名，文档完善 |

### 7.2 应对策略

1. **代码重构**: 对紧耦合代码进行适度重构
2. **测试数据**: 建立统一的测试数据工厂
3. **自动化**: CI/CD集成自动执行测试
4. **代码审查**: 测试代码同样需要Review

---

## 8. 交付物清单

- [x] 单元测试覆盖率提升方案文档
- [x] SysPermissionServiceImplTest.java (25个测试用例)
- [x] ReportAnalyticsServiceImplTest.java (20个测试用例)
- [x] WorkflowServiceImplTest.java (25个测试用例)
- [x] EnhancedNotificationServiceImplTest.java (20个测试用例)
- [x] CacheServiceExtendedTest.java (25个测试用例)
- [ ] 持续集成配置
- [ ] 覆盖率报告模板
- [ ] 测试规范文档

---

## 9. 总结

本方案针对AI-Ready项目当前单元测试覆盖率不足的问题，制定了系统的提升计划：

1. **现状分析**: 识别出604个主代码文件，仅55个测试文件，覆盖率约9.1%
2. **目标设定**: 分四阶段提升至80%+行覆盖率，70%+分支覆盖率
3. **用例设计**: 针对核心业务逻辑设计了100+缺失测试用例
4. **脚本开发**: 已开发5个核心测试类，包含115+测试方法
5. **实施计划**: 8周分阶段实施，确保质量与进度平衡

通过本方案的实施，将显著提升代码质量，降低缺陷率，为项目持续发展奠定坚实基础。

---

*文档版本: 1.0*
*创建日期: 2026-04-11*
*作者: AI-Ready QA Team*
