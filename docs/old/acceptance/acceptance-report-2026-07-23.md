# 验收检查完成报告（第一轮）

> 日期：2026-07-23 ｜ 验收人：PM（AI）
> 验收依据：`docs/page-implementation-master-plan.md` + `docs/task-sheets/` 任务单验收清单
> 验收方式：结构审计脚本重跑 + vue-tsc/mvn 基线 + DB 实表核对 + 运行实例（5655）curl E2E + 代码抽查
> **总体判定：❌ 不通过（2 条红线 + 1 个严重缺陷 + 5 项范围缺口），返工后复验**

---

## 一、通过项 ✅（结构目标基本达成）

| # | 验收项 | 结果 | 证据 |
|---|---|---|---|
| 1 | 菜单→组件可达性 | ✅ 218/218 | audit_pages.py 重跑（audit-report-v2.txt） |
| 2 | 8 个 WMS 列表页断点 | ✅ 全部建成 | wh/borrow-in 等 8 目录均有 index.vue，ARReportPage+真实 borrowApi；list_path 缺失 8→0 |
| 3 | 页面降级标记 | ✅ 审计脚本清零 | degraded 标记 18→0（但应用中心仍有 a-alert，见失败项 5） |
| 4 | componentMap 健康度 | ✅ 655 键 0 断链（+28 新键） | 脚本校验 |
| 5 | Flyway 迁移纪律 | ✅ V11.29.0/V11.30.0 已执行+登记 success=t | flyway_schema_history rank 185/186 |
| 6 | 批次 0.3 营销三页后端 | ✅ 端点 200 + 种子数据 | presale/addon-rule/popup-ad curl 200；md_payment_method 种子 6 行 |
| 7 | 收款单表单重构（1.1） | ✅ 59→504 行，核销明细网格/客户选择器加载未核销应收/状态机按钮（完成核销）/状态标签完整 | 代码抽查通过 |
| 8 | 付款单/凭证/报价单表单 | ✅ 59→453 / 74→245 / 57→567 行，报价单含产品选择器+有效期 | 代码抽查 |
| 9 | 商机看板（4.4） | ✅ 阶段拖拽落库 | opportunity/index.vue 拖拽实现 |
| 10 | 信用额度（5.2） | ✅ 前端警告+后端 SaleOrderServiceImpl 完整校验（额度/已欠/可用提示语） | 代码抽查 |
| 11 | 借进借出查询（0.4a） | ✅ 107→173 行，无降级标注 | 代码抽查 |
| 12 | 运行实例 | ✅ 5655 为 7-23 新构建 | 进程启动 08:18 vs jar 构建 08:14 |

## 二、失败项 ❌（必须返工）

### 🔴 红线 1：vue-tsc 基线破坏（17 个错误）
- **位置**：`src/views/purchase/inbound/form.vue` 922-939 行——在普通 `<script setup>` 中使用 JSX 语法（Modal.confirm 的 content 用 `<div>`/`<a-input>`）
- **判定**：硬性基线"vue-tsc 全仓 0 错误"被破坏，**一票否决**
- **返工要求**：改用 `h()` 函数或模板内 Modal；修复后全仓 vue-tsc 复测为 0

### 🔴 红线 2：费用分摊"前端空转"（批次 2.3 不通过）
- **现象**：前端 758 行表单（分摊方式/预览 UI 完整），但 **后端无任何 CostSharing 类**，`/api/erp/purchase/cost-sharing/page` 404；表单仅 import optionsApi 与 inboundApi，**没有保存/执行分摊的端点调用**
- **判定**：违反硬性标准"每一个页面/功能点必须有对应的后端API支撑"；分摊不落库 = 业务闭环断裂
- **返工要求**：补后端（分摊单实体+表+分摊执行 Service：回写入库行成本+库存成本重算+流水），前端接保存/执行端点，E2E 验证"分摊后库存成本=原值+分摊额"

### 🟠 严重：支付方式/渠道端点 500（批次 0.2 不通过）
- **根因**：V11.29.0 建表用错审计列约定——`md_payment_method/channel` 用 `create_time/update_time/deleted/create_by/update_by`，而 erp-finance 模块 BaseEntity（MyBatis-Plus 版）映射 `created_at/updated_at/deleted_flag/created_by/updated_by/remark`（对照同模块可用的 fin_accounting_period 表）。MyBatis-Plus 查询生成 `created_at` 等列 → PG 列不存在 → 500
- **判定**：页面（235 行新前端）实际不可用；违反"接口层可调通"；暴露开发未遵守"先读后写"（未核对模块 BaseEntity 列约定）
- **返工要求**：新迁移（V11.31.0）ALTER 两表对齐模块约定（重命名 5 列+补 remark），或改实体注解（二选一，以模块多数派=表对齐 BaseEntity 为准）；手动执行+Flyway 登记；curl 复测 200

### 🟠 严重：报价转订单"假闭环"（批次 4.1 核心项不通过）
- **双重缺陷**：① 路径不匹配——前端调 `/crm/quotation/{id}/convert-to-order`（404），后端是 `/{id}/convert`；② 更本质——`QuotationServiceImpl.convertToOrder` **只把报价单状态改为 CONVERTED，未创建任何销售订单**（无 SaleOrder 注入、无明细复制、无源单关联）
- **判定**：违反业务层闭环；属"状态翻转假装完成"的造假式实现，触碰红线
- **返工要求**：统一前后端路径；Service 注入 erp-sales SaleOrderService 真实建单（明细/金额/客户带过去，sale_order 记录 source=quotation+源单号），E2E 验证"接受报价→转订单→销售订单草稿生成且源单可追溯"

### 🟠 应用中心未处置（0.4b 不通过）
- 页面仍挂 a-alert 降级标注，既未按方案 A 对接模块授权数据，也未按方案 B 菜单下线
- **返工要求**：按任务单二选一落地

## 三、范围缺口（抽查发现未实施项）

| 任务 | 缺口 | 证据 |
|---|---|---|
| 4.5 促销归因 | erp_sale_order 无 promotion_id 列 | information_schema 查询 0 行 |
| 5.6 期初锁定 | initial-stock 页无"启用账套/锁定"实现标记 | 代码 grep 无 |
| 3.2 盘点差异→报损联动 | 无 generateDamage 等联动代码 | grep erp-stock 无 |
| 4.1 报价版本管理 | 多次报价留痕未见 | 待深查 |
| 基建 | 结构化日志 %PARSER_ERROR 刷屏 | ai-ready.log 全文污染 |

## 四、抽查范围声明

本轮为**抽样验收**（非 219 页全量）：批次 0 全项、批次 1 的 1.1/1.2/1.7、批次 2 的 2.2/2.3、批次 4 的 4.1/4.4/4.5、批次 5 的 5.2/5.6。批次 3（仓储配送深化）、批次 4 其余项、批次 5 其余项、批次 6（报表口径）**未验**，不代表通过。

## 五、复验条件

1. vue-tsc 全仓 0 错误
2. `/api/erp/md/payment-method|payment-channel` curl 200 且页面 CRUD 可用
3. 费用分摊 E2E：分摊执行→入库行成本回写→库存成本重算，DB 可核
4. 报价转订单 E2E：已接受报价→转订单→erp_sale_order 新增草稿单（源单号=报价单号）
5. 应用中心按 A/B 方案之一落地
6. 第三节范围缺口逐项补交或说明
7. **下一轮换全量验收**：批次 3/4/5/6 逐项过 + 8 条核心业务链路 E2E（总规划第 6.2 节）

---
*附：本报告所有证据可复现——审计脚本 tool-results/audit_pages.py、登录与 curl 序列见 tool-results/。*
