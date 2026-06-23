# AI-Ready 下一阶段任务规划

> 基于后端模块对标分析，明确商贸企业核心差距和建设路径

## 当前状态评估

### 系统定位
- **目标用户**: 中小型商贸企业（批发、仓储、配送）
- **对标系统**: Odoo商贸版 + 金蝶云星空 + 用友U8
- **当前版本**: v0.3.3

### 业务模块覆盖情况

| 功能领域 | 覆盖状态 | 所在模块 | 完整度 |
|----------|----------|----------|--------|
| 采购管理 | ✅ 完整 | erp-purchase | 95% |
| 销售管理 | ✅ 完整 | erp-sales | 95% |
| 库存管理 | ✅ 完整 | erp-stock | 90% |
| 仓储作业 | ✅ 进阶 | wms | 95% |
| 配送物流 | ✅ 进阶 | dms | 95% |
| 财务管理 | ✅ 基础完整 | erp-finance | 80% |
| 往来单位 | ✅ 完整 | erp-partner | 90% |
| 定价管理 | ✅ 完整 | erp-pricing | 85% |
| 供应商门户 | ✅ 进阶 | erp-supplier-portal | 90% |
| CRM客户 | ✅ 完整 | crm | 85% |
| 固定资产 | ✅ 完整 | erp-fixed-asset | 85% |
| 预算管理 | ✅ 完整 | erp-budget | 80% |
| B2B商城 | ✅ 完整 | erp-b2b-mall | 80% |
| 打印服务 | ✅ 完整 | erp-printing | 85% |

**商贸业务覆盖率: 90%** ✅

---

## P0 优先级任务（核心差距补全）

### 1. 审批工作流引擎 (BPM)

**现状**: 已有 WorkflowDefinition/WorkflowInstance/WorkflowTask 实体，但未实现流程引擎

**建设位置**: `core/base/core-base`

**功能需求**:
- 流程定义管理（节点、条件、审批人配置）
- 流程实例创建与执行
- 审批任务分配与处理
- 流程状态追踪与历史记录
- 常用审批场景模板（采购审批、销售审批、费用审批）

**实体设计**:
```
WorkflowDefinition   - 流程定义（已有）
WorkflowNode         - 流程节点（新增）
WorkflowCondition    - 条件分支（新增）
WorkflowInstance     - 流程实例（已有）
WorkflowTask         - 审批任务（已有）
WorkflowHistory      - 审批历史（新增）
```

**API设计**:
- `/workflow/definitions` - 流程定义管理
- `/workflow/instances` - 流程实例管理
- `/workflow/tasks` - 任务待办处理
- `/workflow/history` - 审批历史查询

**预计工作量**: 2周

---

### 2. 人力资源管理模块 (HRM)

**现状**: 无人力资源管理模块

**建设位置**: 新建一级模块 `hr/`（与 erp/crm/wms/dms 平级）

**模块结构**:
```
hr/
├── pom.xml
└── src/main/java/cn/aiedge/hr/
    ├── organization/     # 组织架构（部门、岗位）
    ├── employee/         # 员工档案
    ├── attendance/       # 考勤管理
    ├── salary/           # 薪酬管理
    └ performance/        # 绩效管理
    └ recruitment/        # 招聘管理（可选）
    └ training/           # 培训管理（可选）
```

**功能需求**:
- 组织架构管理（部门、岗位、人员编制）
- 员工档案管理（基本信息、合同、证件）
- 考勤管理（打卡、请假、加班、调休）
- 薪酬管理（薪资结构、工资计算、发放记录）
- 绩效管理（考核指标、评分、结果）

**实体设计**:
```
SysDept              - 部门（已有，需扩展）
SysPosition          - 岗位（新增）
Employee             - 员工档案（新增）
EmployeeContract     - 劳动合同（新增）
AttendanceRecord     - 考勤记录（新增）
LeaveRequest         - 请假申请（新增）
SalaryStructure      - 薪资结构（新增）
SalaryPayment        - 工资发放（新增）
PerformanceReview    - 绩效考核（新增）
```

**API设计**:
- `/hr/departments` - 部门管理
- `/hr/positions` - 岗位管理
- `/hr/employees` - 员工管理
- `/hr/attendance` - 考勤管理
- `/hr/salary` - 薪酬管理
- `/hr/performance` - 绩效管理

**预计工作量**: 3周

---

## P1 优先级任务（功能增强）

### 3. 成本核算增强

**现状**: erp-finance 有基础核算，缺少成本分摊、毛利分析

**建设位置**: `erp-finance`

**功能需求**:
- 成本分摊规则配置（部门、产品、订单维度）
- 标准成本管理（产品成本标准设定）
- 实际成本计算（采购成本、仓储成本、配送成本）
- 毛利分析报表（产品毛利、客户毛利、订单毛利）
- 成本差异分析（标准vs实际）

**实体设计**:
```
CostAllocationRule   - 成本分摊规则（新增）
ProductCostStandard  - 产品标准成本（新增）
CostCalculation      - 成本计算记录（新增）
ProfitAnalysis       - 毛利分析结果（新增）
```

**预计工作量**: 1周

---

### 4. 零售POS收银

**现状**: 有 erp-sales（销售管理），前端缺少零售收银客户端

**后端建设位置**: 扩展 `erp-sales`（无需新建模块）

**前端建设位置**: 新建 `frontend/apps/pos-client/`

**后端功能需求**:
- 扩展销售订单支持零售场景（快速结算、会员识别）
- 收款方式管理（现金、刷卡、扫码支付）
- 交接班记录
- 日结报表

**前端POS客户端需求**:
- 收银界面（扫码、快速结算）
- 会员识别（手机号/会员卡）
- 促销活动应用
- 多支付方式支持
- 交接班操作
- 日结打印

**后端实体设计**（扩展 erp-sales）:
```
PosShift             - 交接班记录（新增）
PosDailySummary      - 日结报表（新增）
PaymentMethod        - 收款方式配置（扩展）
```

**预计工作量**: 1周（后端0.5周 + 前端0.5周）

---

### 5. 多租户隔离增强

**现状**: 有 SysTenant 实体，未实现数据隔离拦截器

**建设位置**: `core/base/core-base`

**功能需求**:
- MyBatis-Plus TenantLineHandler 集成
- 租户ID自动注入（INSERT/UPDATE）
- 租户数据过滤（SELECT）
- 租户配置隔离
- 租户模块授权

**预计工作量**: 0.5周

---

## P2 优先级任务（安全合规）

### 6. 数据权限增强

**现状**: 有 SysDataScope 实体，未实现行级权限

**建设位置**: `core/base/core-base`

**功能需求**:
- 部门数据权限（本部门/本部门及下级/全部）
- 角色数据权限配置
- MyBatis拦截器自动过滤
- 权限注解生效

**预计工作量**: 0.5周

---

### 7. API限流防护

**现状**: 无接口级别限流机制

**建设位置**: `core/base/core-base`

**功能需求**:
- Sentinel Dashboard 集成
- 接口限流规则配置
- 热点参数限流
- 系统自适应保护

**预计工作量**: 0.5周

---

### 8. 质量管理模块 (QMS)

**现状**: 有批次追溯，缺少质检标准

**建设位置**: 新建 `erp/erp-quality`

**功能需求**:
- 质检标准管理（检验项目、合格标准）
- 进货检验记录
- 不合格处理流程
- 质量追溯增强

**实体设计**:
```
QualityStandard      - 质检标准（新增）
QualityInspection    - 检验记录（新增）
QualityIssue         - 不合格处理（新增）
```

**预计工作量**: 1周

---

## 实施路径

### 阶段1: P0核心补全（约4周）
```
Week 1-2: 审批工作流引擎 (BPM)
Week 3-4: 人力资源管理 (HRM)
```

### 阶段2: P1功能增强（约3周）
```
Week 5: 成本核算增强
Week 6-7: 零售POS + 多租户隔离
```

### 阶段3: P2安全合规（约2周）
```
Week 8: 数据权限 + API限流
Week 9: 质量管理 (可选)
```

---

## 总工作量估算

| 任务 | 优先级 | 工作量 | 建设位置 |
|------|--------|--------|----------|
| 审批工作流 | P0 | 2周 | core/base |
| 人力资源 | P0 | 3周 | **hr/** (一级模块) |
| 成本核算增强 | P1 | 1周 | erp-finance |
| 零售POS | P1 | 1周 | erp-sales扩展 + pos-client前端 |
| 多租户隔离 | P1 | 0.5周 | core/base |
| 数据权限 | P2 | 0.5周 | core/base |
| API限流 | P2 | 0.5周 | core/base |
| 质量管理 | P2 | 1周 | erp-quality (新建) |
| **总计** | | **9周** | |

---

## 完成后对标评估

完成上述任务后，系统覆盖率预期：

| 系统 | 当前覆盖率 | 补全后覆盖率 |
|------|------------|--------------|
| 管家婆 | 95% | 98% |
| Odoo商贸版 | 85% | 95% |
| 金蝶云星空 | 83% | 93% |
| 用友U8 | 83% | 93% |
| SAP S/4HANA | 60% | 70%（定位不同） |

**目标**: 达到金蝶云星空级别，成为完整的商贸企业一体化ERP系统。