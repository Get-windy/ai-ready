# CRM 模块全面审计报告（2026-09-23）

**范围**
- 前端：`frontend/apps/pc-admin/src/views/crm/**`（14 个目录 / **20 个 .vue**）+ `src/api/crm.ts`、`src/api/customer.ts` + `router/dynamicRoutes.ts` 的 CRM 路由补齐表
- 后端：`backend/crm`（**91 个 .java / 10 个控制器 / 185 个端点**）
- 数据库：devdb 中 **22 张 `crm_*` 表**、`sys_menu`(CRM 菜单 26 行)、`sys_permission`(93 条 `crm:*` 码)、`sys_role_permission`、`sys_module_permission`
- 文档基线：`docs/Yh-Spec/手动整理对标开发文档/CRM模块/*.md`（README + 16 篇页面文档）+ `系统菜单设计与管理/*.md`（5 篇）

**维度**：可用性 · 规范性 · 冗余 · 死代码 · 功能接通 · 跨模块关系 · 权限配置是否符合行业标准

**口径**：区分「代码缺陷」与「E2E/审计造数残留」——后者不计为缺陷。所有结论给出可复现证据（`文件:行号` / SQL / 真机 HTTP 实测）。
**真机环境**：`core-api-0.3.24-exec.jar`（当日构建）运行于 `localhost:5655`，`admin / 系统租户`（超管）登录；探针只读，未改库。

---

## 〇、摘要

| 级别 | 数量 | 代表问题 |
|---|---|---|
| **P0（本轮已修，真机验证）** | 4 类 5 个端点 | 跟进删除必 404 · 报价模板 3 端点必 500 · 报价版本列表 NPE 500 · 商机统计除零 500 |
| **P0（待拍板，涉及红线）** | 4 | 报价转订单把 CRM 客户 ID 写进 `erp_sale_order` · 订单明细漏写 `tenant_id` · 销售出库反向依赖 CRM 信用服务且 ID 域错位 · 10 个 CRM 菜单无法被权限控制 |
| **P1（待处理）** | 6 | CRM 发票页前后端权限码完全不相交 · 软删后重号（4 处算法仍带 `deleted=0`）· 报价模板 15 端点零 UI · 营销活动 27 端点零 CRUD 页 · 自动回收是日志桩但前端有按钮 · `getById` 空值语义两种口径 |
| **P1（已修 1 处）** | 1 | 信用状态 `overdue` 分支不可达 |
| **P2（待处理）** | 12 | 真僵尸权限码 5 条 · 前端死 API 23 个 · `formatAmount` 复制 10 份 · 11 个菜单 `menu_level=0` · 审计列两组并存 · 表类型不统一 · lint 11 处 `eqeqeq` |
| **端到端结论** | — | **185/185 端点 100% 有鉴权注解**；前端调用的接口**0 个 404**；菜单 17 页齐全；**但 CRM 业务数据全库近乎为空**（客户 0 行、合同 0 行），链路只到"接口通"未到"业务通" |

**整体判断**：CRM 的**骨架是齐的、鉴权覆盖是全的、菜单是通的**（185 端点 185 注解，0 裸端点；20 个页面调用的接口全部存在）。
问题集中在三类：
1. **写库缺陷**：5 个端点真机 500 / 404（本轮已修 4 类），根因都是"局部实现细节"（boolean 列写成 `=1`、漏空值守卫、分母取错）；
2. **跨界越线**：CRM 直接 INSERT `erp_sale_order`，并把 **CRM 客户 ID** 写进 ERP 单据的 `customer_id`——这正是本模块文档 README 明文禁止的红线（`README.md:24-27`），且**已产生脏数据**（见 §2.1）；
3. **权限体系半成品**：10 个 CRM 菜单 `menu_code` 在权限码库里查无对应码 ⇒ **对所有登录用户恒可见、无法被授权体系控制**；CRM 发票页的 7 个 `crm:invoice:*` 码在前端控按钮、后端却守 `invoice:*`（财务域），**两组码零交集**。

---

## 一、已修复（本轮，附真机验证）

### 1.1 【P0】跟进记录「删除」按钮必 404 —— 双 `/api` 前缀

**证据**（修复前实测，同一后端、同一 token）：

```
200  DELETE /api/crm/followUp/999999        ← 正确路径
404  DELETE /api/api/crm/followUp/999999    ← 前端实际发出的请求
     {"code":404,"message":"接口不存在: api/api/crm/followUp/999999"}
```

**根因**：`frontend/apps/pc-admin/src/api/crm.ts:379` 写的是 `` request.delete(`${CRM_BASE}/followUp/${id}`) ``，而 `CRM_BASE = '/api/crm'`（`crm.ts:271`）、标准 `request` 实例的 `baseURL` 已是 `/api`（`utils/request.ts:93`）⇒ 合成 `/api/api/...`。
同一文件 `crm.ts:768` 有为 customer-pool 写下的**同款错误注释警示**，说明这是漏网的一处。
该函数被 `views/crm/customer-follow/index.vue:681`（跟进记录列表「删除」）真实调用 —— 即**删除跟进记录必然失败**。

**修复**：改为相对路径 `request.delete(`/crm/followUp/${id}`)`，并补上同款注释。

### 1.2 【P0】报价模板 3 个端点必 500 —— boolean 列被当成整型比较

**证据**（修复前实测）：

```
500  /crm/quotation-template/active          {"code":500,"message":"系统异常，请稍后重试"}
500  /crm/quotation-template/customer/1      {"code":500,"message":"系统异常，请稍后重试"}
500  /crm/quotation-template/category/1      {"code":500,"message":"系统异常，请稍后重试"}
```

日志栈：

```
org.postgresql.util.PSQLException: 错误: 操作符不存在: boolean = integer
  at cn.aiedge.crm.quotation.service.impl.QuotationTemplateServiceImpl.listActiveTemplates(QuotationTemplateServiceImpl.java:56)
```

**根因**：`crm_quotation_template.active` 在库里是 **boolean**（`information_schema.columns` 已核），而 `QuotationTemplateMapper.java:14/17/20` 三条手写 `@Select` 全写 `active = 1`。

**修复**：三处改为 `active = TRUE`，并加注释说明列类型。
> 该缺陷**文档未登记**，是本次真机扫描新发现。

### 1.3 【P0】报价单「版本列表」NPE → 500

**证据**：`GET /api/crm/quotation/1/versions` → 500；日志：

```
java.lang.NullPointerException: Cannot invoke "cn.aiedge.crm.quotation.entity.Quotation.getParentId()" because "quotation" is null
  at cn.aiedge.crm.quotation.controller.QuotationController.listVersions(QuotationController.java:95)
```

**根因**：同控制器的 `getById`（`:59`）有 `BusinessException.notFound` 守卫，`listVersions` 没有 ⇒ 单号不存在时报 500 而非 404。

**修复**：在 `QuotationController.java:94` 后补空值守卫 → `BusinessException.notFound("报价单不存在")`。

### 1.4 【P0】商机统计除零 → 500

**证据**：`GET /api/crm/opportunity/statistics` → 500；日志 `java.lang.ArithmeticException: / by zero`。

**根因**：`CustomerOpportunityServiceImpl.java:235-237` 的分母是 `winCount + loseCount`，守卫却写成 `totalCount > 0`。本库 4 条商机**无一赢/输**（`crm_customer_opportunity` 状态均为进行中）⇒ 分母为 0 ⇒ 必然 500。
该缺陷在《商机开发文档》§9.0 与《商机阶段开发文档》§12 已登记"未修"，本轮予以修复。

**修复**：改为按 `closedCount = winCount + loseCount` 判零。

### 1.5 【P1】客户信用状态 `overdue` 分支永不命中

`CustomerCreditServiceImpl.java:104-110`（修复前）：

```java
if (usageRate >= 80)      creditStatus = "warning";
else if (usageRate >= 100) creditStatus = "overdue";   // ← 永不可达（≥100 也满足 ≥80）
```

对照本类自己的 `getCreditStatistics`（`:235-241`）用的是「先 ≥100 再 ≥80」的正确顺序 ⇒ 两处口径互相矛盾。

**修复**：调整为先判 `overdue` 后判 `warning`，与本类统计方法对齐。

> 说明：以上 5 项均为**无口径争议的实现缺陷**，故本轮直接修复。凡涉及业务口径、模块归属、红线的，一律不擅自改，登记在 §二 待拍板。

### 1.6 修复验证（真机复测）

**编译**：`./mvnw -o -DskipTests -pl core/api/core-api -am package` → **BUILD SUCCESS**（06:08 min）。
**复测方式**：用重新构建的 `core-api-0.3.24-exec.jar` 在**另一个端口 5690** 起第二个实例（不动用户原有 5655 实例），跑同一批只读探针；复测完毕已关闭 5690。

**后端 4 类缺陷复测（修复前 → 修复后）**：

| 端点 | 修复前 | 修复后 |
|---|---|---|
| `GET /api/crm/opportunity/statistics` | 500 `系统异常`（`ArithmeticException: / by zero`） | **200** |
| `GET /api/crm/quotation-template/active` | 500（`操作符不存在: boolean = integer`） | **200** |
| `GET /api/crm/quotation-template/customer/1` | 500 | **200** |
| `GET /api/crm/quotation-template/category/1` | 500 | **200** |
| `GET /api/crm/quotation/1/versions` | 500（NPE） | **404 `报价单不存在`**（预期语义） |

**全端点回归**：修复后重跑两轮扫描（36 个无参 GET + 33 个带参 GET）⇒ **5xx = 0，意外 404 = 0**。

**前端修复的验证**：`crm.ts:379` 改为相对路径后，`DELETE /api/crm/followUp/999999` 实测 **200**（修复前前端实际发的是 `/api/api/...`，实测 **404**）。该改动属前端，需随前端资源发布生效。

---

## 二、待拍板（涉及红线 / 业务口径，本轮未动）

### 2.1 【P0·红线】报价转订单直写 `erp_sale_order`，并把 **CRM 客户 ID** 写进 ERP 单据

**代码证据**：`QuotationMapper.java:36-54` 两条 `@Insert` 直接写 ERP 销售订单主/子表。
> 归属核对（grep 全后端 `INSERT INTO erp_sale_order`）：除 `erp/erp-sales` 自身的写路径、以及平台级维护工具 `SystemRebuildService`（`core/api/.../tenant/rebuild/`，专门用于删除草稿单据）之外，**唯一一处由业务模块写入该表的代码就是 CRM 的这个 Mapper**。

```java
@Insert("INSERT INTO erp_sale_order(id, tenant_id, order_no, customer_id, customer_name, ...) "
      + "VALUES(#{orderId}, #{quotation.tenantId}, #{orderNo}, "
      + "#{quotation.customerId}, #{quotation.customerName}, ...)")
```

调用方 `QuotationServiceImpl.createSaleOrder()`（`:385-401`）把 **CRM 客户 ID** 原样写进 `customer_id`。

**为什么是红线**：CRM 模块 README 明文规定（`README.md:24-27`）「业务单据的客户一律走 ERP 往来单位，CRM 客户不得直接开单」「严禁让 ERP 依赖 `crm_customer`」。本模块文档《报价单开发文档》§12 第 ⑥ 项也已把它登记为 P0。

**已产生脏数据的实证**：

```
erp_sale_order #1827624375412493
  order_no = QO202607232493   remark = 报价转单   temp 值 order_source = 1
  customer_id   = 1      ← 写入的是 CRM 客户 ID
  customer_name = 验收测试客户  ← 文本来自 CRM
```

而 `biz_party#1` 实际是 **「散客（零售默认）」**；`crm_customer` 表**当前 0 行**（含软删）。
⇒ 该订单在 ERP 侧显示为「客户 = 散客（零售默认）」，而 `customer_name` 列写着「验收测试客户」——**ID 与名称自相矛盾**，任何按 `customer_id` 关联的 ERP 下游（应收、价格等级、账期）都会挂到错误主体上。

**同时存在的次生风险**：

| 项 | 证据 |
|---|---|
| 订单号靠 `orderId % 10000` 拼 4 位 | `QuotationServiceImpl.java:390-391`；`erp_sale_order.order_no` **无唯一索引**（仅 pkey）⇒ 撞号不报错、**静默重号**（比 400 更危险） |
| orderId 用 `currentTimeMillis()<<10 \| random` 生成 | 同上；两个并发请求可能算出同一个 `orderId`（主键冲突） |
| `original_order_id / original_order_no` 被写入 CRM 报价单 id/号 | 同插入语句；ERP 的"原单"字段语义被 CRM 单据污染 |
| `order_source = 1`、`remark = '报价转单'` 硬编码 | 同插入语句 |

**修法（需拍板，二选一）**：
① ERP 侧暴露一个"由 CRM 报价创建销售订单"的**服务接口**（`erp-sales` 拥有 `erp_sale_order` 的写权），CRM 只传报价单号 + **ERP 往来单位 ID**；客户未建档时先走建映射（`crm_erp_customer_mapping`，见 §2.1 附）；
② 保留直写，但**必须**先做 CRM 客户 → ERP 往来单位的映射解析，禁用 `quotation.customerId` 直填。

> 附：`crm_erp_customer_mapping` 表（18 列）在库中**0 行**，`crm_customer.md_partner_id` 列**全后端 0 处引用**（grep 实测）⇒ 红线所要求的"映射环节"**代码完全未接线**，两道经（建档 / 转化建映射）都不存在。

### 2.2 【P0】`erp_sale_order_item` 明细插入漏写 `tenant_id`

**代码证据**：`QuotationMapper.java:49-54`

```java
@Insert("INSERT INTO erp_sale_order_item(id, order_id, line_no, product_id, product_name, "
      + "quantity, unit_price, amount, remark, create_time, update_time) VALUES(...)")
```

**列定义**：`erp_sale_order_item.tenant_id` 是 **NOT NULL，DEFAULT 1**（`information_schema.columns` 已核）。
⇒ 漏写不会报错，而是**静默落成租户 1**。对租户 2 的报价转单，明细行会写进租户 1 —— 租户隔离在这条链路上失效（与记忆 `mybatis-custom-insert-no-tenant-fill` 是同一类坑）。

对比同一文件 `insertSaleOrderRaw` **写了** `tenant_id` ⇒ 同一功能内两条语句口径不一致。

**修法**：补 `tenant_id`（取 `quotation.getTenantId()`）与 `deleted`、`create_by/update_by`。

### 2.3 【P0】ERP 销售出库**反向依赖 CRM 信用服务**，且客户 ID 域错位

**代码证据**：`erp/erp-sales/.../SaleOutboundServiceImpl.java:67`

```java
private final CustomerCreditService customerCreditService;   // ← 来自 cn.aiedge.crm.customer.service
```

调用点 `SaleOutboundServiceImpl.java:1042-1065`：

```java
boolean creditOk = customerCreditService.checkCreditAvailable(outbound.getCustomerId(), totalAmount);
```

而 `CustomerCreditServiceImpl` 的每个方法都是 `customerService.getById(customerId)` / `getById(customerId)` —— **按 `crm_customer.id` 查**。

**问题**：`outbound.getCustomerId()` 是 **ERP 往来单位 ID**（实测 `erp_sale_order.customer_id` 27/29 命中 `biz_party`）。用 ERP 往来单位 ID 去查 CRM 客户表，是**跨域 ID 混用**：命中纯属巧合，命中错了就按别人的额度放行/警告。

**量化影响**（全后端精确统计，`customerCreditService.` 调用点）：

| 方法 | 外部调用 |
|---|---|
| `checkCreditAvailable` / `getAvailableCredit` / `getCreditLimit` / `getCreditStatus` / `getCurrentDebt` / `updateCustomerDebt` | 各 1~2 处，**全部来自 `SaleOutboundServiceImpl`** |

⇒ `CustomerCreditService` **不是死代码**（纠正一版静态扫描的初判），但它是"CRM 模块内服务被 ERP 反向依赖"的**模块边界违规**，且业务上算的是错人。

**加重项**：`CustomerCreditServiceImpl` 的欠款计算是桩：

```java
private BigDecimal calculateTotalDebt(Long customerId) { return BigDecimal.ZERO; }   // :297-299
```

配合 `@Scheduled(cron = "0 0 1 * * ?")` 的 `batchUpdateDebt()`（`:192-209`）——**每天凌晨 1 点把所有 CRM 客户的 `current_debt` 写成 0**。

**修法（需拍板）**：信用额度应挂在 ERP 往来单位（`biz_party.credit_limit/current_debt` 列已存在），CRM 侧服务仅服务 CRM 页面；或明确由谁承接"信用"这一职责后单向依赖。

### 2.4 【P0】10 个 CRM 菜单**无法被权限体系控制**

菜单可见性由 `MenuPermissionDeriver` 从 `sys_permission` 派生（平台-AUTHZ-01），判定规则在 `SysMenuServiceImpl.java:342-348` 写死为**过渡口径**：

```java
if (menuCode != null && !menuCode.isEmpty()
        && knownMenuCodes.contains(menuCode)          // ← 权限码库里必须存在该菜单码
        && !heldMenuCodes.contains(menuCode)) {       // ← 才按"用户是否持有"隐藏
    blockedByPermission++;
    continue;
}
```

即：**`menu_code` 在权限码库里查不到对应码的菜单，对所有登录用户恒可见**。

**SQL 实测**（`NOT EXISTS(权限码 = 菜单码 OR 权限码 LIKE 菜单码||':%')`）——CRM 相关共 **10 个菜单页命中**：

| 菜单 id | 名称 | menu_code | path | 备注 |
|---|---|---|---|---|
| 80201 | 客户跟进 | `crm:customer-follow` | `crm/customer-follow` | 后端码是 `crm:follow-up:*`，**词根不一致** |
| 70303 | 客户分级 | `crm:customer-grade` | `crm/customer-grade` | 文档建议 `crm:customer-grade:adjust` |
| 70311 | 线索转化 | `crm:lead-convert` | `crm/lead-convert` | — |
| 70321 | 商机阶段 | `crm:opportunity-stage` | `crm/opportunity-stage` | 文档建议 `advance/win/lose` |
| 70341 | 合同审批 | `crm:contract-approval` | `crm/contract-approval` | — |
| 70360 | 销售漏斗 | `crm:funnel` | `crm/funnel` | — |
| 70361 | 客户分析 | `crm:customer-analysis` | `crm/customer-analysis` | — |
| 70001/70002/70003 | 拜访规划/执行/检视 | `sales:visit-plan/exec/review` | `sales/visit-*` | 见 §5.2 |

**后果**：这 10 页**不能按角色收窄**——想让销售看不到"客户分析"做不到；且菜单能显示、接口仍 403（页面主体调用 `/customer/page` 等受 `crm:customer:list` 保护的端点）⇒ 用户看到入口点进去全是空数据/报错。

**修法**：给这 10 个 `menu_code` 建对应权限码并接进后端端点（先补 `sys_permission` 种子再补注解——本仓铁律）。**因涉及 `sys_menu`/`sys_permission` 数据变更，按既有纪律需先出方案获批。**

### 2.5 【P0】CRM「发票」页的权限码**前后端完全不相交**

**前端**（`views/crm/invoice/index.vue`）用 7 个码控按钮：

```
:35  v-permission="'crm:invoice:create'"      :351 v-permission="'crm:invoice:send'"
:324 v-permission="'crm:invoice:view'"        :360 v-permission="'crm:invoice:cancelconfirm'"
:333 v-permission="'crm:invoice:edit'"        :476 v-permission="'crm:invoice:detailrefresh'"
:342 v-permission="'crm:invoice:issue'"
```

**后端**：这 7 个码**在 `backend/` 全后端零消费**（grep 实测）。页面真正调用的是 `/api/erp/invoice/*`（财务模块 `InvoiceController`），其 26 处注解用的是 **另一套** `invoice:*`：

```
invoice:create(9) invoice:detail(6) invoice:export(1) invoice:list(2) invoice:update(3) invoice:view(5)
```

**两组码零交集**（`crm:invoice:*` vs `invoice:*`）。真机实测两者都通（超管 `*` 豁免），但对非超管：

| 场景 | 结果 |
|---|---|
| 给了 `crm:invoice:send` | 按钮**可见**，点击 → 后端要 `invoice:create` → **403** |
| 给了 `invoice:create` 但没给 `crm:invoice:*` | 接口**能用**，按钮**看不见** |

**并且还有一层模块权益冲突**：`sys_module_permission` 映射 `crm:` → `crm` 模块、`invoice:` → **`finance` 模块**。于是"CRM 菜单下的页面"受 **finance 模块权益门**管辖：租户只买 CRM 不买财务时，菜单可见、点进去 **403「finance 模块未开通」**。

**修法（需拍板）**：先裁定"发票"到底属 CRM 还是财务（文档 §5 已结论：走 ERP 财务模块，`README.md:233`）。若属财务 → 从 CRM 菜单移除、前端改用 `invoice:*` 码；若保留 CRM 菜单 → 后端须用 `crm:invoice:*` 码另起一套（不推荐，等于两套码守同一批端点）。

### 2.6 【P0】软删单据后重号撞唯一索引（4 处算法仍未修）

**真库索引实测**：CRM 域共 **12 个**"业务单号 UNIQUE、不含 `deleted`"的唯一索引：

```
crm_contract.contract_no        crm_quotation.quotation_no        crm_quotation_template.template_code
crm_customer.customer_code      crm_customer_lead.lead_code       crm_customer_opportunity.opportunity_code
crm_customer_follow_up.follow_up_code                             crm_marketing_campaign.campaign_code
crm_visit_plan.plan_no          crm_marketing_channel.channel_code crm_marketing_content.content_code
crm_contract_change.change_no
```

**修复进度分裂**：5 张表已换成 `CrmDocNo`（`common/CrmDocNo.java:43-53`，取当日最大号 SQL **不带 `deleted`** ⇒ 软删行也占号，安全）：
`customer / lead / opportunity / follow_up / visit_plan`。

**仍带 `.eq(deleted, 0)` 的旧算法 4 处**（软删行不参与计数但占着号 ⇒ 复用号 ⇒ 唯一索引冲突 ⇒ 前端看到 400「请求数据不完整或存在冲突」）：

| 文件:行 | 前缀 |
|---|---|
| `ContractServiceImpl.generateContractNo` `:114-128` | `CT` |
| `QuotationServiceImpl.generateQuotationNo` `:98-113` | `QT` |
| `QuotationTemplateServiceImpl.generateTemplateCode` `:70-85` | `QT-TPL` |
| `MarketingCampaignServiceImpl.generateCampaignCode` `:93-108` | `MC` |

**另**：两套号算法并存本身即冗余（`CrmDocNo` 与 4 份内联复制），且有共同并发竞态（两请求算同号）。
**修法**：三选一（① 4 处去掉 `deleted` 条件，最小改动；② 12 个索引改部分唯一索引 `WHERE deleted=0`，需先清洗存量；③ 改序列/随机号，顺带解决并发）。**需拍板**，故本轮未动。

### 2.7 【P1】`getById` 空值语义两种口径

真机实测（id 不存在时）：

| 端点 | 返回 |
|---|---|
| `GET /api/crm/contract/1` | **404** `合同不存在` |
| `GET /api/crm/quotation/1` | **404** `报价单不存在` |
| `GET /api/crm/quotation-template/1` | **404** `报价模板不存在` |
| `GET /api/crm/marketing/1` | **404** `营销活动不存在` |
| `GET /api/customer/1` | **200 + 空响应体** |
| `GET /api/crm/followUp/1` | **200 + 空响应体** |
| `GET /api/crm/lead/1` | **200 + 空响应体** |
| `GET /api/crm/opportunity/1` | **200 + 空响应体** |

同一模块内"单据不存在"报 404 还是 200-null 不一致，前端要写两套判空；对外接口语义也不规范。
**修法**：统一为 404（`BusinessException.notFound`），4 个控制器各 1 处。

---

## 三、死代码与冗余

### 3.1 真僵尸权限码 5 条（库里有、前后端零引用）

判定口径：对 93 条 `crm:*` 码，分别与「后端 `@SaCheckPermission` 用到的 **65** 条」和「前端 `v-permission`/代码引用的 **43** 条」求差集（`comm` 实测）。

| 权限码 | 名称 | 后端 | 前端 |
|---|---|---|---|
| `crm:contract:batchapprove` | CRM合同批量审批 | ✗ | ✗ |
| `crm:manage` | CRM客户管理 | ✗ | ✗ |
| `crm:opportunity:detailrefresh` | CRM商机刷新 | ✗ | ✗ |
| `crm:opportunity:reset` | CRM商机重置 | ✗ | ✗ |
| `crm:opportunity:search` | CRM商机搜索 | ✗ | ✗ |

> 另有 **23 条"仅前端用、后端无对应注解"**的码（`crm:customer:edit`、`crm:customer:addtolevel`、`crm:customer:refresh`、`crm:lead:assign`、`crm:lead:import`、`crm:opportunity:convert/move`、`crm:quotation:batchsend/convertfromdetail/detailrefresh/sendfromdetail`、`crm:contract:detailrefresh`、`crm:create`、`crm:refresh`、7 条 `crm:invoice:*`）——这些是**前端单方面设的门**，后端不校验，属"看得见的按钮才有权限、直调接口无权限"，是**权限假门**。其中 `crm:invoice:*` 整组见 §2.5。
> **特别注意 `crm:customer:edit` vs `crm:customer:update`**：后端守的是 `update`，前端按钮门用的是 `edit`，**同一动作两个码**。

### 3.2 报价模板：整个后端（15 端点 / 2 张表）**前端零入口**

- 前端全仓 grep `quotation-template` / `QuotationTemplate` → **0 命中**（`views/` 与 `api/` 都查过）。
- 菜单 26 行中**没有**"报价模板"这一页。
- 后端 `QuotationTemplateController` 15 个端点、`crm_quotation_template` + `crm_quotation_template_item` 两张表（共 48 列）全部闲置；其中 3 个端点原本还是 500（§1.2 已修）。

### 3.3 营销活动：27 端点**无任何 CRUD 页**

- `MarketingCampaignController` 27 个端点、5 张 `crm_marketing_*` 表（156 列）。
- 前端仅在**分析模块**两页读取：`views/analytics/mkt-activity-analysis/index.vue`、`mkt-promote-analysis/index.vue`（经 `api/analytics.ts:728` 调 `/crm/marketing/page` 与 `/statistics`）。
- 全仓 grep `crm:marketing` 于 `.vue` → **0 命中**；营销模块的 19 个菜单（80300~80331）用的是另一套 `mkt:*` 码。
⇒ 营销活动的"建/改/批/执行/目标"全链路**无 UI**。

### 3.4 客户信用服务：8 个方法零调用 + 每日定时清零

`CustomerCreditService` 共 14 个方法，精确统计 `customerCreditService.` 调用点后，**从未被外部使用**的有：

```
setCreditLimit · getCreditWarningList · getOverCreditList · getCreditStatistics
freezeCredit · unfreezeCredit · isCreditFrozen · batchUpdateDebt
```

其中 `batchUpdateDebt` 带 `@Scheduled(cron="0 0 1 * * ?")`，每天凌晨把所有客户 `current_debt` 写成 `calculateTotalDebt()` 的返回值 —— 而那是一个**恒返回 `BigDecimal.ZERO` 的桩**（`:297-299`）。

### 3.5 前端死 API 23 个

`api/crm.ts` 中**全前端零调用**的导出（反查命令：`grep -rn "<方法名>" --include=*.vue --include=*.ts`，排除定义文件自身）：

| 分组 | 函数 |
|---|---|
| `opportunityApi` | `getStatistics`、`listByCustomer`、`advanceStage`、`win`、`delete` |
| `invoiceApi` | `page`、`create`、`updateStatus`、`voidInvoice`、`sendInvoice`、`getStatistics`（6 个；页面用内联 `invoiceOps` 绕过） |
| `customerPoolApi` | `put`、`listAvailable`、`listMyClaimed`、`listMyReturned` |
| `contractApi` | `reject`、`getStatistics` |
| `leadConvertApi` | `convert` |
| `api/customer.ts` | `updateStatus`、`export`、`getFollowRecords`、`getOrderRecords`、`getOptions` |

**其中 `customerPoolApi.put`（放入公海）零调用值得单列**：后端 `POST /api/crm/customer-pool/put/{customerId}` 存在，前端**没有任何按钮**能触发它 ⇒ "把客户放进公海"这条业务动作在 UI 上不可达（只有"领取/退回"可达）。

`api/customer.ts` 与 `api/crm.ts` 的 `crmCustomerApi` 还**定义了同一批 CRM 客户端点**（`/customer/page`、`/customer/{id}`、`/customer/export`、`/customer/import` …）——重复 API 模块。
另注：`api/customer.ts` 把裸响应标注为 `ApiResponse<PageResponse<T>>`（类型与运行时不符），调用方靠 `res?.data ?? res` 兜底（`views/crm/customer/index.vue:1222/1267`）。

### 3.6 前端复制粘贴块

| 重复物 | 份数 | 位置 |
|---|---|---|
| `formatAmount()` | **10** | contract-approval:408、contract/index:946、invoice/form:1001、invoice/index:821、lead-convert:601、lead/index:484、opportunity-stage:374、opportunity/index:1202、quotation/form:925、quotation/index:898（3~4 种变体：空值返 `'0.00'`/`'-'`、是否带 `¥`） |
| `escapeHtml()` | **6** | contract/index:1459、customer-follow:719、invoice/form:1642、invoice/index:1254、quotation/form:1746、quotation/index:1525 |
| `handlePrint()`（整段 `window.open + document.write + 内联 CSS`） | **6** | 同上 6 处 |
| 手写 CSV 导出（未走 `@/utils/exportCsv`） | **3** | customer-grade:511-530、customer-pool:569-585，另 `customer-follow/index.vue:690` **本地又定义了一个同名 `exportCsv()`** |
| `buildQueryWrapper` 分页条件拼装 | **5** 个 Service 各写一遍 | CustomerServiceImpl:48、CustomerLeadServiceImpl:63、CustomerOpportunityServiceImpl:58、ContractServiceImpl:66、QuotationServiceImpl:58 |
| 状态机（草稿→审批→通过→签署→生效） | **2** 套 | Contract 与 Quotation 各实现一遍，动作名与状态迁移几乎一致 |

### 3.7 数据库层冗余与规范不一致（22 张表）

| 问题 | 证据 |
|---|---|
| **两组审计列并存** | `crm_customer` 同时有 `create_by/create_time/update_by/update_time`(bigint/ts) **和** `created_by/created_at/updated_by/updated_at`(bigint/ts)；共 1 张表 8 列 |
| **审计列类型不统一** | `created_by/updated_by` 在 5 张表是 **varchar**（`crm_customer_lead` / `crm_customer_opportunity` / `crm_customer_follow_up` / `crm_customer_pool` / `crm_visit_plan`），全仓其余表是 `bigint` |
| **时间戳类型不统一** | 21 张表 `timestamp without time zone`，`crm_erp_customer_mapping` 是 **`with time zone`** |
| **映射表无唯一约束** | `crm_erp_customer_mapping` 除 pkey 外**无任何 UNIQUE** ⇒ `(crm_id, erp_id)` 可重复插入 |
| **10 张子表缺 `tenant_id` 索引** | `crm_quotation_item`、`crm_contract_clause/attachment/payment/change`、`crm_marketing_target/execution`、`crm_quotation_template_item`、`crm_visit_plan/record`、`crm_marketing_channel/content` 仅有 pkey + 一个外键索引 |
| **列命名前缀不统一** | `crm_customer_lead` 用 `lead_status`/`lead_name`；`crm_customer_opportunity` 用 `status`（同域两套前缀） |

### 3.8 桩与空实现（**有接口、有按钮、无行为**）

| 位置 | 行为 | 前端可达性 |
|---|---|---|
| `CustomerPoolServiceImpl.autoRecovery:157-160` | **只打一行日志**，无任何回收逻辑 | `views/crm/customer-pool/index.vue:543` 有按钮「自动回收」⇒ 用户点完提示成功、数据无变化 |
| `CustomerController.getOrderRecords:189-191` | 恒返回 `List.of()`（注释自述"订单由销售模块提供"） | 客户详情「订单记录」 |
| `QuotationServiceImpl.createFromTemplate:153-156` | 忽略 `templateId`/`customerId`，`createQuotation(new Quotation(), null)` 生成**空报价单** | 无 UI（模板页不存在） |
| `QuotationServiceImpl.createFromOpportunity:141-150` | 只写 `opportunityId` + 默认有效期，**不复制商机金额/客户** | `opportunity` 页「创建报价」 |
| `ContractServiceImpl.createFromQuotation:146-160` | 不读报价内容，仅设 `quotationId` + 硬编码 `paymentDays=30/deliveryDays=7/warrantyMonths=12/...` | 报价页「转合同」 |
| `CustomerCreditServiceImpl.calculateTotalDebt:297-299` | 恒 `ZERO` | 无 UI |

### 3.9 前端 lint 现状

`npx eslint src/views/crm src/api/crm.ts src/api/customer.ts --ext .vue,.ts`
⇒ **651 problems / 0 errors**，其中：

- `@typescript-eslint/no-explicit-any`：绝大多数（逐处 `any`，属技术债，非缺陷）
- **`eqeqeq` 11 处**（`==`/`!=`，其中 `quotation/form.vue` 8 处、`quotation/index.vue` 2 处、`invoice/form.vue:1214` 1 处）——**这批是唯一可能有实际行为差异的子集**
- `@typescript-eslint/no-unused-vars` **2 处**：`quotation/form.vue:1579` `handleF8Key`、`quotation/index.vue:1579` `handleF8Key` —— 定义了 F8 快捷键处理函数但**从未绑定** ⇒ 页面上的"F8 打印"提示是假提示
- `vue/singleline-html-element-content-newline` 2 处（`quotation/form.vue:688/695`，纯格式）

---

## 四、功能接通性（前后端）

### 4.1 接口存在性：20 个页面调用的端点 **0 个 404**

对 20 个 .vue 逐个提取 api 层真实 URL，与后端 `@RequestMapping` 全量比对 + 真机抽查，结论：**调用的端点全部存在**（唯一例外是 §1.1 那个前端自己拼错路径的删除接口，已修）。
> 后端全部无路径参数的 GET 端点 36 个 + 带参 GET 端点 33 个，真机扫描 **404 数为 0**。

### 4.2 按钮与交互：无空函数

全仓 grep `TODO|FIXME|console.log` 于 `views/crm` → 0 命中；`@click` handler 与定义交叉比对，未发现悬挂 handler（`handleSave/handleSubmit` 来自 `useBasicForm` 解构，非缺失）。
**但**有两类"看起来接通、实际不生效"：① §3.8 的桩（自动回收）；② §2.4 的菜单可见但接口 403。

### 4.3 路由与菜单

- CRM 顶层菜单 `60007 CRM`（`menu_code=mega:crm`）→ 8 个分组（客户管理 60701 / 外勤拜访 60101 / 线索管理 60702 / 商机管理 60703 / 报价管理 60704 / 合同管理 60705 / 发票管理 60706 / CRM报表 60707）→ **17 个页面**，与《CRM README》§2.1 一致。
- 真机 `GET /api/menu/user/mega/tenant-admin`（超管）返回 **416 条菜单**，其中 CRM/拜访相关 **26 条 = 1 顶层 + 8 分组 + 17 页**，**17 页一页不缺**。
- **双入口方向正确**：客户/线索/商机/合同 `path`=列表页、`list_path`=表单页、`tag_label=添加`；报价单 70330、发票 70350 相反（`path`=表单页、`list_path`=列表页、`tag_label=历史`）——与《41条双入口菜单的正确配置》§规范一致。
- **孤儿页面 0 个**：20 个 .vue 全部有路由可达。
- **硬编码补齐表**（`dynamicRoutes.ts` 的 `getRequiredRoutes()`）中 CRM 仅 1 条 `crm/customer/:id`（`:1334-1337`），指向的 `customer/form.vue` 存在。
- **菜单字段规范性问题**：
  - `menu_level`：**11 个 CRM 叶子菜单为 `0`**（70001/70002/70003、70303、70311、70321、70330、70341、70350、70360、70361），而 6 个（80200/80201/80202/80210/80220/80230）为 `3`。《营销模块菜单新增》文档明确「页面 `menu_level` 沿用同组既有值（页面=3）」⇒ 11 个不符。
  - `component` 写法两种：4 个菜单用短式 `crm/xxx/index`（80200/80210/80220/80230），其余 10 个用 `views/crm/.../index.vue` 全式。
  - 其余字段（`client_type=tenant-admin`、`tenant_id=0`、`visible=1`、`status=1`）**全部合规**，历史那条"`tenant_id=1`+`client_type='pc-admin'`"错误写法已由 `V11.392.0` 订正。

### 4.4 后端鉴权覆盖：**185/185，零裸端点**

```
端点注解数: 185        @SaCheckPermission 数: 185        空权限码/orRole: 0
```

`CrmPermissions`（历史程序化校验类）已删除、无引用。历史遗留的"8 个控制器整模块零鉴权"（`MD-AUTHZ-01`）与"144 裸端点"（E-01）**均已闭环**。

**但语义错配仍在**（职责分离角度）：

| 端点 | 现用码 | 应为 |
|---|---|---|
| 合同 `effective`/`complete`/`terminate`/`cancel`/`progress` | `crm:contract:edit` | 生命周期动作应有独立码（§文档已登记） |
| 合同 `confirmPayment` | `crm:contract:edit` | 确认收款应用财务/审批类码 |
| 报价 `mark-accepted`/`mark-rejected`/`cancel` | `crm:quotation:edit` | 客户反馈类动作 |
| 报价 `mark-expired`（批量） | `crm:quotation:create` | 与合同同类端点 `crm:contract:refresh` 口径不一致 |
| 跟进 `listByLead` | `crm:lead:view` | 资源是 follow-up，却用 lead 域码 |
| 合同 `clauses/{id}`、`attachments/{id}` 的 DELETE | `crm:contract:edit` | 删除子资源用 edit |

### 4.5 权限码落库与角色关联

- 库中 `crm:*` 码 **93 条**，全部 `status=0`（启用语义）+ `deleted=0`。
- 后端注解用到 65 条，**"用了但库里没有" = 0 条** ⇒ 不存在"注解码未落库导致 403"的历史问题。
- 角色关联：**只授给了 `SUPER_ADMIN`**（另 `E2E_T2_ADMIN` 被测试夹具补授 2 条）。`SYSTEM_ADMIN`、`DEPT_ADMIN` 等租户角色**一条 crm 码都没有**。
- 租户 1、2 在 `sys_tenant_module` 中**均已启用 `crm`**，且后端 `ModuleEntitlementInterceptor` 已接进请求链（不再零调用）⇒ 现状是"租户买了 CRM、模块门放行、但角色没码 ⇒ 除超管外进 CRM 一律 403"（与 MASTER_TODO 记录一致）。

---

## 五、跨模块关系

### 5.1 CRM 客户 ↔ ERP 往来单位（红线现状：**未落地**）

文档 README 的红线（`:24-27`）：业务单据的客户走 ERP 往来单位；CRM→ERP 须经 `crm_erp_customer_mapping`；严禁为 CRM 另建第二套往来单位。

**实测**：

| 检查项 | 结果 |
|---|---|
| `crm_erp_customer_mapping` 表 | 存在（18 列）、**0 行** |
| 该表的实体/Mapper/Service | `CrmErpCustomerMapping` **全后端 0 处引用**（grep 无命中） |
| `crm_customer.md_partner_id` 列 | 存在、**全后端 0 处引用** |
| CRM→ERP 的写入路径 | **只有一条**：§2.1 的报价转订单直写 `erp_sale_order`（且写的是 CRM 客户 ID，**恰恰违反红线**） |
| ERP→CRM 的写入路径 | **只有一条**：§2.3 的销售出库调 CRM 信用服务 |
| `crm_customer` 与 `biz_party` 数据 | `crm_customer` **0 行**；`biz_party` 1 行（`散客（零售默认）`） |

⇒ 两条红线路径（"经映射"和"先建档"）**代码都不存在**；现有的两条跨模块写入**都踩线**。这是本模块最需要拍板的结构性问题。

### 5.2 外勤拜访：**路径/权限码/模块三处错位**

| 维度 | 现状 | 应有 |
|---|---|---|
| 前端目录 | `views/sales/visit-plan|exec|review` | 应在 `views/crm/` |
| 前端 API 来源 | `import { visitPlanApi } from '@/api/crm'`（`views/sales/visit-plan/index.vue:405`） | — |
| 菜单归属 | 挂在 CRM 顶层（`parents=60101`，`menu_name=外勤拜访`） | ✅ 与文档一致 |
| `menu_code` | `sales:visit-plan/exec/review` | 文档 4 处一致建议收敛为 `crm:visit-*` |
| 后端权限码 | `crm:visit:*`（6 条） | ✅ CRM 域 |
| 前端模块权益映射 | `MODULE_ROUTE_MAP` 的 `sale` 列表**不含** `sales/visit-plan`，`crm` 列表只有 `['crm']` ⇒ `getRouteModule()` 返回 `undefined` ⇒ **完全跳过模块权益校验** | 应纳入 `crm` 模块管辖 |

⇒ 后果：**拜访三页不受模块权益门管辖**（租户没买 CRM 也能通过直链访问），而它们的接口又要求 `crm:visit:*`（倾向 403）；且菜单 `menu_code` 在权限库无对应码 ⇒ §2.4 的"恒可见"。
一处小事实：CRM 后端确有一整套 `crm_visit_plan`/`crm_visit_record` 实现（`VisitController` 11 端点），**不存在两套拜访后端**，只是前端放错了目录。

### 5.3 发票：CRM 的菜单 + 财务的接口 + 财务的权益门

| 层 | 实际归属 |
|---|---|
| 菜单 | `70350 发票`，父级 `60706 发票管理`，在 **CRM** 顶层下；`menu_code=crm:invoice` |
| 前端页面 | `views/crm/invoice/{form,index}.vue`（2 个 .vue、3 个 Tab） |
| 前端按钮门 | `crm:invoice:*`（7 条，CRM 域） |
| 实际调用接口 | `/api/erp/invoice/*`（**财务模块** `InvoiceController`，前缀 `/api/erp/invoice`） |
| 后端守卫码 | `invoice:*`（6 条，**finance 域**） |
| 模块权益门 | `sys_module_permission`：`invoice:` → **finance**；`crm:` → crm |
| 文档裁定 | 《发票开发文档》§5 + README:233 ——「发票走 ERP 财务模块，不属于 crm 模块」 |

⇒ 这是 §2.5 的完整画像。另注：同文件旁还有 `InvoiceApplicationController`（16 端点）**前端零引用**，且**系统内没有"发票申请"页** ⇒ `POST /erp/invoice/create-from-application` 要求 `applicationId`，前端"新建发票"必然失败（文档 §9.0 已登记，本次未复测，因为无法构造申请单）。

### 5.4 营销活动：后端在 CRM、前端在分析

见 §3.3。补充：`api/marketing.ts:528` 也有一段 `营销活动/推广（/api/crm/marketing）` 的封装，但**没有页面 import 它**做 CRUD。

### 5.5 模块权益门（现状核对）

- `ModuleEntitlementService` 已实现，`ModuleEntitlementInterceptor` 已注册进 `WebMvcConfig`（历史"零调用"已闭环）。
- 归属判定按**权限码前缀**（非 URL）：`crm:` → `crm` 模块、`crm` 前缀规则 `sort=60`。
- 三道**有意放行**：码无归属前缀不拦 / 平台超管不拦 / 读库失败不拦（并在类注释与代码中写明理由）。
- **CRM 侧的一个真实坑**：`CustomerController` 映射为 **`/api/customer`**（全 CRM 唯一不守 `/api/crm/*` 约定的控制器，另 9 个都是 `/api/crm/...`）。
  运行时无冲突（`/api/crm/customer/page` 实测 404，不会与它撞），但：
  ① 与 ERP 的 `/api/erp/md/customer`、`/api/erp/customer/*` 形成三个"customer"前缀，排查时极易混淆；
  ② 若后续用"URL 前缀 → 模块"的生成器补码，会推出 `customer:*` 而非 `crm:customer:*`，直接踩坏模块映射。**这是维护性陷阱，建议在收尾期一并把前缀改成 `/api/crm/customer`**（需前端同步改 8 处调用，见 `api/crm.ts:436/440/444/448`、`api/customer.ts:92/127/141/182`）。

---

## 六、数据库与规范性

### 6.1 表与索引

- **22 张 `crm_*` 表**，全部具备 `id / tenant_id / deleted` 三列（✅ 历史"三表缺 tenant_id"已由 `V11.379.0` 补齐）。
- 每表均有主键；**12 个业务单号唯一索引不含 `deleted`**（见 §2.6）。
- **10 张表仅有 pkey + 1 个外键索引，缺 `tenant_id` 索引**（多租户查询会全表扫）。
- `crm_erp_customer_mapping` **无业务唯一约束**（可重复插入同一对映射）。

### 6.2 数据现状（**全库近乎为空**，这是"可用性"评估的关键事实）

真实 `COUNT(*)`（非 `pg_stat` 估算）：

| 表 | 行数 | 说明 |
|---|---|---|
| `crm_customer` | **0** | 含软删行也是 0 ⇒ 客户主数据为空 |
| `crm_customer_lead` | 2 | 两条 **均 `deleted=1`**（软删残留） |
| `crm_customer_opportunity` | 4 | **`customer_id` 全为 NULL** ⇒ 商机未挂客户 |
| `crm_quotation` | 2 | **`customer_id=1` 是悬空引用**（`crm_customer` 无 id=1） |
| `crm_contract` | 0 | — |
| 其余 17 张 | 0 | 含 `crm_customer_pool`、`crm_visit_plan/record`、全部 `crm_marketing_*`、`crm_contract_*` 4 张子表、`crm_quotation_template*` |

⇒ **CRM 域没有任何一条端到端可跑的真实业务数据**（客户 0 行 ⇒ 跟进/公海/分级/分析全部无源）。数据链路的"通过"目前只证明到接口层。

### 6.3 审计列

`created_by/updated_by` 恒 NULL（本轮实测：`crm_customer_lead` 两条 `created_by=NULL`）。根因是全仓共性——`MetaObjectHandler` 只填 `createTime/updateTime/tenantId`，不填操作人；叠加 §3.7 的"两组审计列并存 + varchar 类型"，这一域基本无法追责。

### 6.4 号段

- 两套算法并存（`CrmDocNo` 5 处 + 内联 4 处），见 §2.6。
- 均无并发保护（"查最大号 +1"天然竞态）；`erp_sale_order` 那条更用了 `%10000`。
- `crm_quotation.quotation_no` 实测值 `QT202607230001/2` 与 `System.currentTimeMillis()` 生成的 `id`（19 位）混用，说明历史数据来源不统一。

---

## 七、E2E 与验收覆盖

`tools/e2e-crm.cjs`（6 节 72 项，2026-09-18 建立）当前的覆盖缺口：

| 缺口 | 证据 |
|---|---|
| **6 个 .vue 未被 UI 遍历** | `UI_PAGES`（`e2e-crm.cjs:415-433`）覆盖 14/20：漏 `contract/form`、`customer/form`、`lead/form`、`opportunity/form`（4 个表单页）+ `invoice/index.vue`、`quotation/index.vue`（2 个列表页，只测了对应的 form 入口） |
| **发票断言测的不是页面真实端点** | §4 对发票断言 `/erp/invoice/page?page=0&size=1`（`:379`），而页面实际调 `/erp/invoice/query`（`views/crm/invoice/index.vue:684`）⇒ 两端点都存在、断言必然通过，但**覆盖口径失真** |
| **未覆盖本轮新发现的 500/404** | 本次真机扫出的 5 个 500 端点中，`UI_PAGES` 未遍历报价模板相关页（无菜单），但 `opportunity/statistics` 已存在于断言集之外；`followUpApi.delete` 的 404 **不在任何断言里**（§1.1 的 bug 因此一直没被发现） |

⇒ 建议把「**每个页面的每个写操作按钮至少跑一次并断言非 4xx/5xx**」补进脚本（本轮 4 类缺陷全部是该口径能捕获的）。

---

## 八、修复建议清单（按优先级）

### 8.1 本轮已完成（代码已改，后端已编译 + 真机复测通过）
1. `api/crm.ts:379` 双 `/api` 前缀 → 相对路径（前端改动，需随前端发布生效）
2. `QuotationTemplateMapper.java:14/17/20` `active = 1` → `active = TRUE` ✅ 复测 3 端点 500→200
3. `QuotationController.listVersions` 补空值守卫 → 404 ✅ 复测 500→404
4. `CustomerOpportunityServiceImpl` `winRate` 除零守卫 ✅ 复测 500→200
5. `CustomerCreditServiceImpl.getCreditStatus` 分支顺序（`overdue` 可达）

### 8.2 建议尽快拍板（改前需一次决定口径）
| # | 事项 | 需要的决定 |
|---|---|---|
| 1 | 报价转订单写 `erp_sale_order` | 走 ERP 服务接口，还是保留直写但强制映射 ERP 往来单位？ |
| 2 | `erp_sale_order_item` 漏 `tenant_id` | 直接补（无争议，但归属上一并定） |
| 3 | CRM 信用服务的归属 | 信用挂 ERP 往来单位还是 CRM 客户？`calculateTotalDebt` 谁来实现？ |
| 4 | 软删重号 4 处算法 + 12 个唯一索引 | 选 ①②③ 哪种修法 |
| 5 | 发票页面归属 | 留 CRM 还是要搬回财务 |
| 6 | 10 个无权限码菜单 | 补哪些码（先补 `sys_permission` 种子）——**属系统配置数据变更，需先出方案** |

### 8.3 可立即动手（低风险、无口径争议）
1. 4 个控制器 `getById` 空值统一为 404（§2.7）
2. 删除 5 条真僵尸权限码 + 处理 `crm:customer:edit`/`update` 双码（§3.1）
3. 删除前端死 API 23 个（§3.5）
4. 抽取共享 `formatAmount` / `escapeHtml` / 打印 / CSV（§3.6）
5. `customer-pool` 「自动回收」按钮：要么接实现，要么**去掉按钮**（当前是"点了没反应还说成功"）
6. 2 处 `handleF8Key` 未绑定（§3.9）
7. 11 处 `eqeqeq` 逐条确认（`quotation/*`）
8. 11 个菜单 `menu_level` 订正为 3（若确认规范）
9. `CustomerController` 前缀改为 `/api/crm/customer`（前后端同步 8 处调用）

### 8.4 需独立立项（不是收尾能带的）
- **CRM 售后阶段**（工单/售后）：全库 0 张 ticket/service 表、0 个端点、0 个菜单、`crm:service:*` 码域为空 —— 用户 2026-09-21 已明确"后期迭代再补"。
- **客户信用体系**：需要 `current_debt` 的真实计算源（应收侧）+ 冻结/解冻/预警页面。
- **报价模板 / 营销活动**：要么补页面，要么按"不重复开发"纪律裁剪后端（当前是 15+27 个端点 + 7 张表的空转）。

---

## 附：本轮结论一览（可直接贴进 TODO）

| 类别 | 已闭环 | 未闭环 |
|---|---|---|
| 鉴权注解 | 185/185 端点、0 裸端点、0 空码 | 生命周期动作码语义错配 6 组 |
| 菜单 | 17 页齐全、双入口方向正确、字段合规 | 10 页无权限码（恒可见）、11 页 `menu_level` 不规范、3 页目录错位 |
| 接口接通 | 前端 0 个 404 | 4 类 5 个端点 500/404（本轮已修）、发票页码错配 |
| 权限码 | 93 条在库、"用了没落库"=0 | 5 条僵尸 + 23 条仅前端门 + `crm:customer:edit/update` 双码 |
| 跨模块 | 模块权益门已接线 | 两条跨界写入均踩红线、映射表未接线、拜访/发票/营销三处归属错位 |
| 数据 | 22 表结构齐、tenant/deleted 齐 | 业务数据近乎全空、4 条悬空/孤儿引用、审计列全 NULL |
| 验收 | E2E 72 项 | 6 个 .vue 未覆盖、发票断言口径失真、写操作无断言 |

> 本报告的全部结论均可用文中的 `文件:行号` / SQL / HTTP 实测复现。真机探针脚本为一次性产物，未落盘至仓库（`tools/` 未新增文件）。

---

## 九、执行记录（2026-09-23 第二轮：按推荐直接落地）

### 9.1 数据库迁移 `V11.500.0__Fix_Crm_Menu_Codes_RouteName_And_MenuLevel.sql`

新增文件：`backend/core/api/core-api/src/main/resources/db/migration/V11.500.0__...sql`（含逐条依据与自检 SQL 的长注释）。
做法**刻意对齐仓库既有同类迁移** `V11.498.0`（采购域菜单码对齐）与 `V11.499.0`（路由名冲突修复），**只改 menu_code / route_name / menu_level，不新增权限码、不改 path/component、不删任何菜单**。

**① 10 条菜单 `menu_code` 前缀对齐**（改动依据 = 「该页真实调用的接口所需的权限码前缀」）：

| id | 菜单 | 原 menu_code | 新 menu_code | 依据（页面调用的接口 → 所需码） |
|---|---|---|---|---|
| 80201 | 客户跟进 | `crm:customer-follow` | `crm:follow-up` | `/crm/followUp/page` → `crm:follow-up:list` |
| 70303 | 客户分级 | `crm:customer-grade` | `crm:customer` | `/customer/page` → `crm:customer:list`；`PUT /customer/{id}` → `crm:customer:update` |
| 70361 | 客户分析 | `crm:customer-analysis` | `crm:customer` | `/customer/export` → `crm:customer:list` |
| 70311 | 线索转化 | `crm:lead-convert` | `crm:lead` | `/crm/lead/page` → `crm:lead:view`；`/batch-convert` → `crm:lead:batchconvert` |
| 70321 | 商机阶段 | `crm:opportunity-stage` | `crm:opportunity` | `/crm/opportunity/{page,advance,win,lose}` → `crm:opportunity:{view,edit}` |
| 70360 | 销售漏斗 | `crm:funnel` | `crm:opportunity` | `/crm/opportunity/export` → `crm:opportunity:view` |
| 70341 | 合同审批 | `crm:contract-approval` | `crm:contract` | `/crm/contract/page` → `crm:contract:view`；`/{id}/approve` → `crm:contract:approve` |
| 70001/70002/70003 | 拜访规划/执行/检视 | `sales:visit-plan/exec/review` | `crm:visit` | `/crm/visit/**` → `crm:visit:*`（后端与前端 API 全在 crm 域） |

**② 14 条菜单补唯一 `route_name`**（防「同名路由后者覆盖前者」——`dynamicRoutes.ts:966` 路由名规则是 `menu.routeName || menu.menuCode`，本批改动后 80200/70303/70361 会同时叫 `crm:customer`，lead / opportunity / contract 各有 2~3 条，不补则先注册的那条永远 404）。
**③ 6 条 `menu_level` 3 → 0**（80200/80201/80202/80210/80220/80230；`SysMenuServiceImpl` 对非系统租户非超管强制 `eq(menu_level, 0)`，3 是文档与前端菜单管理页都不承认的取值；`V11.499.0` 已按同一理由处理分析模块并把「crm 6 条」留给 CRM 审计）。

**应用与验证**：迁移已由 Flyway 应用（`flyway_schema_history` 有 `11.500.0 | Fix Crm Menu Codes RouteName And MenuLevel | success=true`）。库内核对：

```
70001 | crm:visit      | CrmVisitPlan          | lvl=0 | 拜访规划
70002 | crm:visit      | CrmVisitExec          | lvl=0 | 拜访执行
70003 | crm:visit      | CrmVisitReview        | lvl=0 | 拜访检视
70303 | crm:customer   | CrmCustomerGrade      | lvl=0 | 客户分级
70311 | crm:lead       | CrmLeadConvert        | lvl=0 | 线索转化
70321 | crm:opportunity| CrmOpportunityStage   | lvl=0 | 商机阶段
70341 | crm:contract   | CrmContractApproval   | lvl=0 | 合同审批
70360 | crm:opportunity| CrmFunnel             | lvl=0 | 销售漏斗
70361 | crm:customer   | CrmCustomerAnalysis   | lvl=0 | 客户分析
80200 | crm:customer   | CrmCustomer           | lvl=0 | 客户
80201 | crm:follow-up  | CrmCustomerFollow     | lvl=0 | 客户跟进
80210 | crm:lead       | CrmLead               | lvl=0 | 线索
80220 | crm:opportunity| CrmOpportunity        | lvl=0 | 商机
80230 | crm:contract   | CrmContract           | lvl=0 | 合同
```

**预期行为变化（须知悉）**：这 10 页由「人人可见」变为**按权限派生**。租户角色目前一条 `crm:*` 码都没有 ⇒ 这些菜单对租户侧会**隐藏**（此前是可见但点进去 403）。这是平台-AUTHZ-01「零可见性原则」的预期结果，但**要让租户用起来必须给其角色授码**（属独立决定，见 §8.2 第 6 项）。

### 9.2 后端代码

| 改动 | 文件 | 说明 |
|---|---|---|
| 4 个控制器 `getById` 空值统一 404 | `CustomerController` / `CustomerFollowUpController` / `CustomerLeadController` / `CustomerOpportunityController` | 原返回 200 + 空响应体，与合同/报价/营销的 404 语义不一致 |
| 补 `mdPartnerId` 映射 | `customer/entity/Customer.java` | 该列（`crm_customer.md_partner_id`）此前**实体未映射、全后端 0 处引用**，是红线的落点 |
| 报价转订单红线守卫 | `quotation/service/impl/QuotationServiceImpl.java` | 转单前把 CRM 客户经 `md_partner_id` 解析为 **ERP 往来单位 ID**；客户不存在或未关联则**明确报错拒绝**，不再把 CRM 客户 ID 写进 `erp_sale_order.customer_id` |
| 明细行补租户列 | `quotation/mapper/QuotationMapper.java` | `erp_sale_order_item` 显式写 `tenant_id`（该列 NOT NULL **DEFAULT 1**，漏写会静默落租户 1）+ `deleted/create_by/update_by` |
| 单号去碰撞 | 同上 + `QuotationServiceImpl.generateSaleOrderNo()` | 原 `orderId % 10000` 取 4 位易撞号且该表**无唯一索引**（静默重号）；改为「查当日最大序号 + 1」 |
| `autoRecovery` 由桩改为真实实现 | `customer/service/impl/CustomerPoolServiceImpl.java` | 回收「有归属 + 最近跟进早于 `今天-N天`」的客户；已入池的跳过（幂等）；**从未有跟进记录的客户不回收**并在日志单列计数（无基准可算，避免把新建客户立刻回收） |
| 新增两个查询 | `CustomerFollowUpMapper.selectMaxFollowUpDate` / `CustomerPoolMapper.countAvailableByCustomer` | 上述两条规则的数据来源 |

**编译验证**：`./mvnw -o -DskipTests -pl crm -am compile` → **BUILD SUCCESS**（91 个源文件）。
**打包 + 真机复测**：`./mvnw -o -Dmaven.test.skip=true -pl core/api/core-api -am package` → BUILD SUCCESS；用新 jar 在 **5690 端口**起独立实例复测（不动其它实例），复测后已关闭。

| 验证项 | 改前 | 改后（实测） |
|---|---|---|
| `GET /api/customer/1` | 200 + 空响应体 | **404** `客户不存在` |
| `GET /api/crm/followUp/1` | 200 + 空响应体 | **404** `跟进记录不存在` |
| `GET /api/crm/lead/1` | 200 + 空响应体 | **404** `线索不存在` |
| `GET /api/crm/opportunity/1` | 200 + 空响应体 | **404** `商机不存在` |
| 报价转订单（CRM 客户未关联 ERP 往来单位） | 直接把 CRM 客户 ID 写进 `erp_sale_order.customer_id`（造出脏数据） | **400** `客户「…」尚未关联 ERP 往来单位，不能转订单。请先在「资料 → 往来单位」建档，并回填该 CRM 客户的关联往来单位后再转单。` |
| `POST /api/crm/customer-pool/auto-recovery` | 只打一行日志（日志桩） | **200 且真实执行**（日志：`扫描有归属客户 N 个，回收 M 个，因无跟进记录跳过 K 个`） |

**红线守卫的验证方式**（按本仓 E2E 纪律：造数 → 验证 → 物理清场）：临时插入 1 条 `crm_customer`（`md_partner_id` 为空）并把测试报价单指向它，触发转单 → 得到上表的 400 拒绝；随后**已复原**：报价单 `status=7 / customer_id=1` 原值回写、临时客户行物理删除、`erp_sale_order` 的 `QO%` 单数仍为 1（未新增任何脏单据）。

### 9.3 前端代码

| 改动 | 文件 | 说明 |
|---|---|---|
| 补 F8 监听 | `views/crm/quotation/index.vue` | `handleF8Key` 此前**定义了但从未绑定**（工具栏却写着「打印(F8)」⇒ 假提示）；已在 `onMounted/onUnmounted` 成对补上，与 `quotation/form.vue` 口径一致 |
| 口径对齐文案 | `views/crm/customer-pool/index.vue` | 「执行自动回收」弹窗原写「按 30 天无跟进把符合条件的客户自动放入公海池」，与后端实现口径不一致；改为明确「已分配 + 最近跟进超 30 天」，并说明从未跟进的客户不在范围内 |

**lint 复测**：`npx eslint src/views/crm src/api/crm.ts src/api/customer.ts` → **650 problems / 0 errors**（修前 651）。

### 9.4 判定「不改」的项（附理由）

| 项 | 结论 | 理由 |
|---|---|---|
| 11 处 `eqeqeq`（`==`/`!=`） | **不改** | 逐条看过：全部是 `x == null` / `x != null` 的 null-or-undefined 惯用法。改成 `===` 会让 `undefined` 漏判，**制造真 bug**。全仓同款写法 **657 处**，是规则与团队惯用法之间的既有张力，不是 CRM 的问题 |
| 635 处 `no-explicit-any` | 不批量改 | 属技术债，逐处替换需配合接口类型定义，不适合收尾期批量动 |
| 删除 5 条「僵尸权限码」 | **不改** | 复核后认为不该删：`crm:contract:batchapprove` 对应《合同开发文档》的「批量审批」按钮、`crm:opportunity:search/reset/detailrefresh` 对应商机页的「查询/重置/刷新」，都是**文档已规划、尚未实现**的能力。删了会在做这些功能时重新建码，属制造返工。正确处置是随对应功能一起接线 |
| ERP 侧服务接口重构 / 信用归属 | **未动** | 涉及产品决策（信用挂 ERP 往来单位还是 CRM 客户、`calculateTotalDebt` 由谁实现），不是收尾期能单方面定的事，保留在 §8.2 待拍板 |

> **环境备注（排查用）**：本轮打包期间检测到**另一个会话在并发执行 Maven 构建**，两次 `package` 因此失败（`core-base` 的 `target/classes` 被并发清空，报「程序包 cn.aiedge.base.entity 不存在」），且一度产出过一个**内含旧 `crm-0.3.24.jar`（12:47）的 exec jar** —— 用它启动的实例表现为「改动没生效」。最终以 `-Dmaven.test.skip=true` 在无并发时重打包，并**先校验 `BOOT-INF/lib/crm-0.3.24.jar` 内确实含新代码**再启动，才得到上表的实测结果。若后续还遇到「改了没生效」，先按这条排查。

### 9.5 本轮之后仍待拍板

1. **信用服务归属**（§2.3）：`erp-sales` → `crm.CustomerCreditService` 的反向依赖 + ID 域错位 + `calculateTotalDebt` 恒 0 + 每天凌晨清零，四项叠加，需要一次产品决策。
2. **12 张表的部分唯一索引 / 号段并发**（§2.6）：4 处旧号算法仍带 `deleted = 0`。
3. **CRM 发票页归属**（§2.5）：留 CRM 还是要搬回财务。
4. **租户角色的 `crm:*` 授码**（§9.1 的行为变化前提）。
5. **历史脏数据**：`erp_sale_order#1827624375412493`（`customer_id=1` 指向「散客（零售默认）」而 `customer_name` 是 CRM 客户名）—— 属一条真实脏数据，需人工确认后订正或作废；本轮**未动**（改业务数据不在审计范围内）。

---

## 十、执行记录（2026-09-26 第三轮：四项拍板后的落地）

> 用户口径：① 信用归属归 ERP，CRM 可以调用；② 12 张表唯一索引 / 号段并发全部改为新算法；
> ③ CRM 发票页搬回财务，CRM 只是调用；④ 那条脏数据直接删除；⑤ 租户角色的 `crm:*` 授码自己排版。

### 10.1 信用归属迁到 ERP（`erp-partner`）

**新增**（`backend/erp/erp-partner/.../party/`）：

| 文件 | 作用 |
|---|---|
| `service/PartyCreditService.java` | 信用服务接口（额度 / 欠款 / 可用 / 放行判定 / 状态 / 预警 / 超额清单 / 统计 / 重算） |
| `service/impl/PartyCreditServiceImpl.java` | 实现，操作对象由 `crm_customer` 换成 **`biz_party`** |
| `mapper/PartyCreditMapper.java` | 欠款汇总（对 `finance_receivable.remaining_amount` 的**只读 SUM**）+ 回写 `biz_party.current_debt` |

**关键订正**：

| 迁走前的缺陷 | 现在 |
|---|---|
| `erp-sales` 用**往来单位 ID** 调 CRM 的服务，而服务按 `crm_customer.id` 取数 | 服务的对象就是 `biz_party`，`outbound.getCustomerId()` 直接是主键，**ID 域一致** |
| `calculateTotalDebt` 恒返回 `BigDecimal.ZERO`（桩） | 按应收余额真实汇总 |
| 叠加 `@Scheduled` 每天把客户欠款**清零** | 定时任务保留但语义正确：按应收余额**重算**；跨租户重算所需的两条 SQL 标了 `@InterceptorIgnore(tenantLine = "true")` |
| `getCreditStatus` 的 `overdue` 分支不可达（≥100 也满足 ≥80） | 改为先判 `overdue` 再判 `warning` |
| CRM 侧 14 个方法中 8 个零调用（冻结/预警清单等） | 冻结/解冻**未迁移** —— 原实现绑的是 `Customer.status == 4`，而 `biz_party.status` 只有 0/1 语义、无冻结位；凭空造一个状态码不如不迁（这两条方法原本也没有任何调用方与入口） |

**调用方改造**：`SaleOutboundServiceImpl` 由注入 `crm.CustomerCreditService` 改为注入 `erp.party.PartyCreditService`（6 个调用点同名迁移，`updateCustomerDebt` → `recalcPartyDebt`）。

**CRM 侧"可以调用"的落地**：`crm/pom.xml` 新增 `erp-partner` 依赖；`CustomerServiceImpl` 在 `pageList` / `exportList` / `getById` 上把客户的「信用额度 / 当前欠款」按 **`md_partner_id`** 替换为 ERP 往来单位的值（未关联往来单位时保留 CRM 侧原值，因为该客户在 ERP 里还不存在）。CRM 客户表上的这两列自此只是历史登记位，不再是事实源。

**顺带修掉一条模块边界违规**：`erp-sales` 对 `crm` 的依赖**整条移除**（改完后 `grep cn.aiedge.crm erp/erp-sales/` 为 0 处引用）。

### 10.2 号段统一 + 唯一索引改部分唯一（迁移 `V11.507.0`）

**算法侧**：CRM 域的 9 个取号点 + 1 个销售订单号，全部改为复用 core-base 的
`BizNumberGeneratorService`（`biz_number_sequence` 上的 `SELECT ... FOR UPDATE` + `UPDATE`，跨进程原子、按日重置）：

| 位置 | 原 bizType 风格 | 现 bizType |
|---|---|---|
| 客户 / 线索 / 商机 / 跟进 / 拜访计划 | `CrmDocNo`（查当日最大号 +1） | `CRM_CUSTOMER` / `CRM_LEAD` / `CRM_OPPORTUNITY` / `CRM_FOLLOWUP` / `CRM_VISITPLAN` |
| 合同 / 报价单 / 报价模板 / 营销活动 | 4 份内联「查最大号 +1」 | `CRM_CONTRACT` / `CRM_QUOTATION` / `CRM_QUOTTPL` / `CRM_CAMPAIGN` |
| CRM 转出的销售订单 | 自写 `QO+日期+orderId%10000` | 直接复用 `nextSaleOrderNo()`（与 ERP 自建订单同一号段） |

`CrmDocNo` 整个类、以及 6 个 Mapper 上已无用的 `selectMaxXxxCode` 方法一并删除。
**格式变化（用户可见）**：新号为 `PREFIX-yyyyMMdd-NNNN`，如 `CUS-20260926-0001`（原为 `CUS-202609180001`）；存量单号不动。

**索引侧**：12 个 `uk_crm_*` 由「全表唯一」改为**部分唯一索引 `WHERE deleted = 0`** —— 唯一性只约束活着的行，逻辑删除的行不再占号也不再与新建行冲突。

同时给租户 1 种下 9 条 CRM 号段（`V11.507.0`），其余租户首次使用时由既有的 `seedMissingSequence` 自愈。

### 10.3 发票搬回财务（迁移 `V11.508.0`）

| 层面 | 动作 |
|---|---|
| 前端页面 | `views/crm/invoice/{form,index}.vue` → **`views/finance/invoice/`**（`git mv`，import 全是 `@/` 绝对路径，无需改引用） |
| API 归属 | 新建 `api/finance/invoice.ts`，把 `api/crm.ts` 的 `invoiceApi` 与两个页面里的内联 `invoiceOps`/直连 `request` **合并收敛**成一套；从 `api/crm.ts` 移除发票类型与 API；`api/analytics.ts` 的再导出改指财务 |
| 客户下拉 | 由 CRM 公海客户（`crmCustomerApi`）改为 **ERP 往来单位**（`/erp/md/customer/list?partnerType=CUSTOMER`）—— 发票是财务单据，客户主体必须是 `biz_party`；该端点不支持关键词参数，故搜索改为对已加载列表做前端过滤 |
| 权限码 | 前端 7 个 `crm:invoice:*` 全部换成后端真正守卫的 `invoice:*`（新增→`create`、查看→`view`、编辑/开具→`update`、发送/作废→`create`、明细刷新→`detail`） |
| 菜单 | 删除 CRM 下的 `60706 发票管理` + `70350 发票`（`sys_role_menu`/`sys_tenant_menu` 引用均为 0，已核）；在财务 `60006` 下新增 `60610 发票管理` 分组 + `80152 发票` 双入口叶子（`path=finance/invoice/form`、`list_path=finance/invoice/index`、`tag=[历史]`、`menu_code=invoice`） |
| 路由 | `dynamicRoutes.ts` 组件映射键 `crm/invoice*` → `finance/invoice*`；`MODULE_ROUTE_MAP.finance` 加入 `finance/invoice`（此前只买 CRM 的租户能看到菜单、接口却被 `finance` 权益门拒） |
| 打印模板 | `views/printing/seed-templates.ts` 的 `pageCodes` 由 `crm/invoice` 改为 `finance/invoice` |
| 权限码清理 | 7 条 `crm:invoice:*` 连同其角色关联一并删除（后端零消费） |
| E2E 脚本 | `tools/e2e-crm.cjs` 移除发票的菜单/UI/端点断言（CRM 由 17 页变 16 页） |

> 核对了「不是重复实现」：全库此前**只有** CRM 下的发票菜单，财务模块没有发票入口（`analytics/invoice-stats` 是统计页，不冲突），所以这是**归位**而非合并。

### 10.4 租户角色授码（迁移 `V11.509.0`）

| 角色 | 授予 | 口径 |
|---|---|---|
| `SYSTEM_ADMIN`（租户系统管理员） | 全部 `crm:*`（80 条）+ 全部 `invoice:*`（14 条，含既有） | 业务全权 |
| `DEPT_ADMIN`（部门管理员） | 36 条：客户 查看/新建/编辑/导入/跟进、公海 查看/领取/退回、跟进记录全量、线索 查看/新建/编辑/转化/导入导出、商机 查看/新建/编辑/导出、报价 查看/新建/编辑/发送/下载、合同 **只读**、外勤拜访全量 | **职责分离**：能录入与跟进，**不含**删除 / 审批 / 转订单 / 续签 / 批量刷新 |
| `E2E_T2_ADMIN` | **刻意不动**（保持 2 条） | `tools/verify-module-authz.cjs` 用它当"无码非超管"探针验证「注解已生效 ⇒ 403」，授码会让该断言失效 |

### 10.5 脏数据删除

`erp_sale_order#1827624375412493`（及其 2 行明细）物理删除，删除后核对 `残留订单=0 / 残留明细=0 / 全库 QO 单=0`。

### 10.6 验证结果

**迁移应用**：Flyway 一次性应用 7 个迁移（含并发会话的 11.503.0~11.506.0）→ `Successfully applied 7 migrations, now at version v11.509.0`。

**库内核对**：

```
① CRM 号段种子=9          ② 非部分唯一索引残留=0（期望 0）
③ CRM 发票菜单残留=0      ④ 财务发票菜单=2（60610 分组 + 80152 叶子）
⑤ crm:invoice 残留=0
授码：SUPER_ADMIN=80 / SYSTEM_ADMIN=80 / DEPT_ADMIN=36 / E2E_T2_ADMIN=2（未动）
```

**真机复测**（新 jar 起在 5690，复测后关闭）：

| 项 | 结果 |
|---|---|
| 菜单树（超管） | CRM 叶子 **16 页**（发票已移出）；财务下 `#80152 发票 finance/invoice/form` |
| 号段 | 新建客户返回 `customerCode = CUS-20260926-0001`（统一号段格式生效）；验证后已物理清场（`crm_customer 残留=0`） |
| 发票接口 | `GET /erp/invoice/query` → **200** |
| 销售出库（信用新依赖） | `GET /erp/sale/outbound/page` → **200**（`PartyCreditService` Bean 注入成功） |
| 编译 | `crm -am compile`、`erp/erp-sales -am compile`、`core-api -am package` 全部 **BUILD SUCCESS** |

**前端 lint**：搬移后的 `views/finance/invoice` + `api/finance/invoice.ts` + CRM 侧 → **0 errors**（639 warnings，构成不变：绝大多数是 `no-explicit-any`）。

### 10.7 迁移版本号顺延（排查备查）

本轮三个迁移原定为 `V11.501/502/503.0`，启动时报
`Found more than one migration with version 11.501.0` —— **并发会话**的 DMS/HR/客户等级三个审计任务同时占用了 501/502/503。
已顺延为 **`V11.507.0` / `V11.508.0` / `V11.509.0`**，并同步了文件内与 `tools/e2e-crm.cjs` 的交叉引用。
⇒ 收尾期多会话并行时，**先 `ls db/migration | sort -V | tail` 再定版本号**，不要按"上一版 +1"取。

### 10.8 本轮之后仍未闭环

1. **信用冻结/解冻**未迁移（见 §10.1 的理由）——若将来要做，需先定义 `biz_party` 侧的「信用冻结」表达方式。
2. `biz_party.current_debt` 的重算依赖每日定时任务；若要有实时性，应在应收/收款核销处主动触发 `recalcPartyDebt`（属财务侧接线）。
3. 发票的**新建仍必失败**（后端只提供 `create-from-application`，要求 `applicationId`，而系统无发票申请页）——本轮只是把它搬回了正确的模块，这条后端缺口仍在。
4. 12 张表的**唯一索引已改部分唯一**，但「部分唯一索引」不参与 MyBatis-Plus 的逻辑删除自动处理，无需额外代码配合（已实测新建/删除正常）。

---

## 十一、执行记录（2026-09-26 第四轮：权限一致性与死代码收尾）

### 11.1 「假门」系统排查与对齐（前端门码 ≠ 后端守卫码）

**为什么这轮必须做**：第三轮的授码方案按职责分离只给了 DEPT_ADMIN `crm:customer:update`，而客户列表的「编辑」按钮挂的是 `crm:customer:edit`
⇒ **该角色看不到自己有权限做的操作**。这类"前端门与后端守卫不一致"的缺陷，授码之后才真正显形。

排查方法：把 CRM 全部 `v-permission` 码与后端 `@SaCheckPermission` 码求差集，再逐个确认按钮背后的 handler 调的是哪个接口。

**对齐 9 处**（前端改用后端真正守卫的码）：

| 位置 | 原码 | 改为 | 依据 |
|---|---|---|---|
| `customer/index.vue:431` 编辑 | `crm:customer:edit` | `crm:customer:update` | → `PUT /crm/customer/{id}`，守 `update` |
| `customer/index.vue:74` 批量分配 | `crm:customer:batchassign` | `crm:customer:update` | → 循环 `PUT /crm/customer/{id}` |
| `customer/index.vue:511` 新增该等级客户 | `crm:customer:addtolevel` | `crm:customer:create` | → 新增弹窗，`POST /crm/customer` |
| `lead/index.vue:320/48` 分配 / 批量分配 | `crm:lead:assign` / `batchassign` | `crm:lead:edit` | → 循环 `PUT /crm/lead/{id}` |
| `opportunity/index.vue:328` 移动阶段 | `crm:opportunity:move` | `crm:opportunity:edit` | → `PUT /crm/opportunity/{id}/stage` |
| `quotation/index.vue:38` 批量发送 | `crm:quotation:batchsend` | `crm:quotation:send` | → 循环 `POST /{id}/send` |
| `quotation/index.vue:645` 详情内发送 | `crm:quotation:sendfromdetail` | `crm:quotation:send` | 同上 |
| `quotation/index.vue:660` 详情内转订单 | `crm:quotation:convertfromdetail` | `crm:quotation:convert` | → `POST /{id}/convert` |

**复核结果**：对齐后前端仍在使用、而后端注解里没有的码只剩 5 个，
且它们**背后都没有任何接口调用**（不是假门，是纯前端门）：
`crm:customer:refresh`、`crm:contract:detailrefresh`、`crm:quotation:detailrefresh`（刷新按钮）、
`crm:lead:import`（占位按钮，点击提示"后端未提供"）、`crm:opportunity:convert`（纯说明弹窗）。这 5 个保留。

被换下来的 8 个专用码（`addtolevel` / `batchassign` / `assign` / `move` / `batchsend` / `sendfromdetail` / `convertfromdetail`）**保留在库中**，
登记为「待拆独立端点」—— 它们是文档定义的独立业务动作，只是当前复用粗粒度端点；拆分端点后即可真正独立管控。

### 11.2 删除重复权限码 `crm:customer:edit`（迁移 `V11.511.0`）

它与 `crm:customer:update` 是**同一个动作的两个名字**（同端点、同语义，`api_path` 为空、后端从未使用），
不存在将来分家的可能，留着只会再次产生假门。全仓引用为 0 后删除。

### 11.3 客户控制器前缀统一为 `/api/crm/customer`（迁移 `V11.511.0`）

`CustomerController` 此前是 CRM 里**唯一**不守 `/api/crm/*` 约定的控制器（另外 9 个都在 `/api/crm/` 下）。
真正的问题不是"不好看"，而是它会**破坏权限码生成器**：`tools/gen-module-permission-seed.py` 按 URL 推导资源名，
`/api/customer` 会推出 `party:customer:*` 而不是库中的 `crm:customer:*` —— 该生成器的注释里就记着这个坑（`:361`）。

同步改动（全部核对过，共 5 类）：

| 层 | 位置 | 改动 |
|---|---|---|
| 后端 | `CustomerController` | `@RequestMapping("/api/customer")` → `"/api/crm/customer"` |
| 后端 | `PermissionInitializationConfig:276-279` | 4 条权限模板的 `apiPath` 同步 |
| 后端 | `ApiPerformanceConfig:97` | 慢请求白名单路径同步 |
| 后端 | `GatewayConfig:35` | 移除已失效的 `customer-service` 路由（`/api/customer/**` 已不存在，由 `crm-service` 接管） |
| 前端 | `api/customer.ts`(13) + `api/crm.ts`(4) | 调用路径同步（含模板字符串写法） |
| 前端 | 2 个 .vue 的说明性注释 | 同步 |
| 工具 | `tools/e2e-crm.cjs`(3) + `verify-authz-batch2.cjs`(3) | 断言路径同步 |
| 工具 | `tools/gen-module-permission-seed.py` | 新增 `'crm:customer'` 资源覆盖与 `/api/crm/customer` 端点覆盖（**保留**历史键，便于旧分支重新生成） |
| 数据库 | `sys_permission.api_path`（4 条） | `REPLACE('/api/customer' → '/api/crm/customer')` |

**真机复测**（新前缀 9 个端点全 200；旧前缀全 404）：

```
200 /crm/customer/page  /export  /dropdown  /list  /code/__none__
    /salesPerson/1  /level/1  /1/follows  /1/orders
404 /customer/page  /customer/export  /customer/dropdown  /customer/1
```

### 11.4 删除前端死 API 18 个

对每个待删函数做**对象限定**的反查（排除定义文件自身），确认外部引用为 0 后删除：

| 文件 | 删除的函数 |
|---|---|
| `api/crm.ts` | `opportunityApi.{delete, advanceStage, win, getStatistics, listByCustomer}`、`contractApi.{reject, getStatistics}`、`leadConvertApi.convert`、`customerPoolApi.{put, listAvailable, listMyClaimed, listMyReturned}` |
| `api/customer.ts` | `customerApi.{updateStatus, export, getFollowRecords, getOrderRecords, getOptions}` |

> 其中 `customerPoolApi.put`（放入公海）的删除登记为：**后端 `POST /crm/customer-pool/put/{customerId}` 仍在，但前端没有入口**；
> 将来补 UI 时重建这个 3 行封装即可。

**lint 复核**：`src/api/crm.ts` + `src/api/customer.ts` + `src/views/crm` → **0 errors**，warnings 由 650 降至 **520**。

### 11.5 本轮仍未做（登记，不做）

**前端 `formatAmount`(×10) / `escapeHtml`(×6) / `handlePrint`(×6) / CSV(×3) 的抽取**。

理由：这是纯 DRY 重构，要动 10 个**当前可正常工作**的页面，而收益只是可维护性。在无法逐个页面点验（本机没有起前端 dev server + 浏览器）的前提下，收尾期做这种改动**回归风险高于收益**。
建议单独立项，并配一次 UI 遍历验证（现有 `tools/e2e-crm.cjs` 的 §5 已能覆盖 16 页的骨架与 console error，可直接复用）。

---

## 十二、未做项复核（2026-09-26 收尾复查）

对报告里所有"未做 / 未闭环 / 待处理"逐条重跑验证命令后的**准确清单**。同时本轮复查**新发现并修复了 1 个真缺陷**（见 12.1）。

### 12.1 复查新发现（已修）

#### ① `crm:contract:refresh` 权限行被软删 ⇒ 该端点对非超管**永久 403**

| 项 | 内容 |
|---|---|
| 症状 | `ContractController#markExpiredContracts`（`POST /api/crm/contract/mark-expired`，`ContractController:236`）标着 `@SaCheckPermission("crm:contract:refresh")`，但该码在库里只剩一行 `deleted = 1` ⇒ 用户永远拿不到 ⇒ **除超管（通配符 `*`）外一律 403**，本地用超管调试完全看不出来 |
| 根因 | `V11.379.0` 播种 → `V11.452.0__Remove_Zombie_Core_And_Crm_Permissions` 当僵尸码**软删**（当时确实无人引用）→ E-01 补注解后 `V11.458.0` 想重新播种，但守卫写的是 `WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code)`，**没带 `deleted = 0`** ⇒ 被软删行判定为"已存在"，**静默跳过**。一次"看起来补了、其实没补"的迁移 |
| 修复 | 迁移 `V11.512.0`：就地恢复该行（`deleted=0` + 补 `api_path`/`method` + 订正名称为「CRM合同标记到期」），并授给 `SUPER_ADMIN`/`SYSTEM_ADMIN` |
| 验证 | 行已恢复（`id=91059, deleted=0, api_path=/api/crm/contract/mark-expired`），两角色各 1 条授权；`tools/audit-permission-codes.py` 的「代码引用了但库中没有」由 2 → 0 |

> 顺带订正了迁移里**主键写死基数**的写法：`9800000` 已被其它迁移占用（首跑撞 `sys_role_permission_pkey`），改为以「当前最大 id」顺延。

#### ② 审计工具 `tools/audit-permission-codes.py` 会扫到注释里的示例代码

`SetMenuConfigController` 的**类注释**里写了 `{@code @SaCheckPermission("set:menu-config:view|update")}`，被工具当成真实注解上报。
已修：扫描前先用等长空白剔除 `//` 与 `/* */`（保行号不变）。修后工具报 **0 个缺失**，可信。
另注：这个工具**早就报出过 ①**，是我前几轮自己写 SQL 复核（没过滤 `deleted`）而漏掉它 —— 教训是**优先跑仓库既有对账工具，而不是另写一份查询**。

#### ③ 同类隐患的量化（未改，只登记）

58 个含 `NOT EXISTS (SELECT 1 FROM sys_permission ...)` 的种子迁移中，**大量未带 `deleted = 0` 条件**：

```
V11.24.0 / V11.362.0 / V11.380.0 / V11.386.0 / V11.394.0 / V11.395.0 / V11.396.0 / V11.398.0
V11.399.0 / V11.400.0 / V11.401.0 / V11.402.0 / V11.403.0 / V11.404.0 / V11.405.0 / V11.407.0
V11.417.0 / V11.418.0 / V11.419.0 / V11.423.0 …（共 20+ 个，清单见本节复核命令）
```

**含义**：只要某个码"先被软删、之后又被重新引用并想补种"，那次补种就会静默失效 —— 与 ① 完全同源。
**不建议改历史迁移**（已应用、且改动会破坏 Flyway 校验和）。正确做法是**让检测常态化**：把
`python tools/audit-permission-codes.py` 接进发布前检查（当前 `.github/workflows/` 里**没有任何**脚本引用它）。
现状是**已知缺口为 0**（工具已复核），属"有检测、无拦截"。

### 12.2 确认仍「未做」的清单（每条都重跑过验证）

| # | 项 | 复核证据 | 性质 |
|---|---|---|---|
| 1 | **报价模板** 15 端点 / 2 张表前端零入口 | `grep -rl "quotation-template\|QuotationTemplate" src/` → **0 命中** | 能力闲置 |
| 2 | **营销活动** 27 端点无 CRUD 页 | `views/` 下仅 `analytics/mkt-activity-analysis`、`mkt-promote-analysis` 两个**只读**页 | 能力闲置 |
| 3 | `GET /crm/customer/{id}/orders` 仍是 `return List.of()` 桩 | `CustomerController:200` | 功能桩 |
| 4 | `createFromTemplate` 忽略入参，生成空报价单 | `QuotationServiceImpl:151-153`（`createQuotation(new Quotation(), null)`） | 功能桩 |
| 5 | `crm_erp_customer_mapping` 仍未接线 | 全后端 grep → **0 处** | **红线**（§5.1） |
| 6 | `crm_customer` 双审计列仍在（8 列并存） | `information_schema` 实测 = 8 | 规范 |
| 7 | 5 张表 `created_by/updated_by` 是 `varchar`（其余表为 bigint） | 实测 = 5 张 | 规范 |
| 8 | `crm_erp_customer_mapping` 除 pkey 外**无任何唯一约束** | 实测 = 0 | 规范 |
| 9 | **13 张** crm 表无 `tenant_id` 索引 | 实测 = 13（比首轮的 10 张更多，因统计口径含子表） | 性能 |
| 10 | 合同 `effective/complete/terminate/cancel/progress` 仍全归 `crm:contract:edit` | `ContractController:175/183/191/200/243` 逐行实测 | 权限粒度 |
| 11 | **拜访三页目录错位**：仍在 `views/sales/visit-*` | `ls views/sales \| grep visit` → 3 个 | 归属 |
| 12 | 拜访三页**不受模块权益管辖**：`MODULE_ROUTE_MAP` 无 `sales/visit-*` | `dynamicRoutes.ts` 实测 | 权益 |
| 13 | **E2E 覆盖缺 5 个 .vue**：`customer/form`、`lead/form`、`opportunity/form`、`contract/form`、`quotation/index` | `UI_PAGES` 16 条 vs 现有 18 个 .vue | 验收 |
| 14 | 菜单 `component` 短式/全式混用（4 个 `crm/xxx/index` vs 10 个 `views/...`） | SQL 实测 | 规范 |
| 15 | 审计列 `created_by/updated_by` 恒 NULL（全仓 `MetaObjectHandler` 共性） | 实测 | 规范 |
| 16 | 前端重复块：`formatAmount`×10 / `escapeHtml`×6 / `handlePrint`×6 / CSV×3 | 见 §3.6 | 可维护性 |
| 17 | 11 处 `eqeqeq` + 520 条 lint warning（绝大多数 `no-explicit-any`） | eslint 实测 | 技术债 |
| 18 | 信用**冻结/解冻**未迁移；发票**新建**仍必失败；`biz_party.current_debt` 依赖每日重算 | 见 §10.8 | 功能 |
| 19 | 「放入公海」后端端点在、**前端无入口**（`customerPoolApi.put` 已按死代码删除） | 见 §11.4 | 功能 |

> 排在前面的 #1~#5 建议按「要么补页面、要么裁剪后端」二选一处理；
> #5（红线映射）是唯一带合规性质的，建议优先；
> #11/#12 是一处改动能同时解决的两个问题（把三页搬进 `views/crm/` 并把 `sales/visit-*` 纳入 `crm` 模块映射）。


