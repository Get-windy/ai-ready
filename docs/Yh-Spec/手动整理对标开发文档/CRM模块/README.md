# CRM 模块 · 开发文档集

> **对应系统**：来肯企汇 ql361 v2.2（`22stable.ql361.com`）**没有 CRM 模块** —— 本模块是**本系统独有扩展**。
> **撰写口径**：按**业界成熟生产级 CRM 系统**（Odoo 17/18、SAP Sales Cloud / S/4HANA Sales、金蝶云·星空、用友 YonSuite）的标准能力设计，**不照搬任何一家界面**。
> **事实来源**：① 本系统源码（引用必须带 `文件:行号`）；② devdb 实测（`sys_menu` 菜单树、`crm_*` 表结构，2026-09-18 直连查询）；③ 业界官方文档（标题 + URL + 章节 + 查阅日期留痕）。
> **通用规范**：见《ql361对标/对标开发技术参考文档》第 4、5 章；布局金标准施工手册见《交易模块/_开发指南-金标准.md》（三条路线 A/A′/B、模板、陷阱、自检清单）。
> **模块定位一句话**：CRM = **全部有可能成为客户的单位与人的全生命周期经营**（线索 → 商机 → 报价 → 合同 → 发票 → 客户维护），**不等于** ERP 往来单位里的"客户"。

---

## 1. 模块定位与边界（最重要的一节，先读这里）

### 1.1 CRM 客户 ≠ ERP 客户

| | **CRM 客户** | **ERP 客户** |
|---|---|---|
| 菜单 | CRM → 客户管理 → 客户（`80200`） | 资料 → 往来单位 → 客户（`80510`） |
| 本系统路由 | `crm/customer/index` ↔ `crm/customer/form` | `md/customer/index` ↔ `md/customer/form` |
| 后端 | `crm` 模块 `cn.aiedge.crm.customer`，表 **`crm_customer`** | `erp-partner` 模块，表 **`biz_party`**（`party_type=customer`） |
| 语义 | **全部可能客户**：线索转来的、公海里躺着的、还没成交的、正在跟进的 | **与当前租户实际发生交易的合作客户**（有单据、有应收、有价格等级） |
| 关注点 | **客户服务与维护**：跟进记录、拜访、商机、合同、客户分级、流失预警 | 交易属性：价格等级、结款方式、账期、信用额度、往来对账 |
| 是否可用于开单 | **否** | **是**（销售订单/出库单/退货单/收款单的客户选择器一律取 ERP 客户） |

> **红线（沿用《资料模块/客户开发文档》P0 裁决）**：
> ① **业务单据的客户一律走 ERP 往来单位**，CRM 客户**不得**直接作为单据往来方；
> ② CRM 客户若成交，须经 **线索/商机转化** 或 **往来单位建档** 落到 ERP 侧，两侧通过 `crm_erp_customer_mapping` 建立映射，**不合并两张表**；
> ③ 严禁为 CRM 另建"第二套往来单位"或反向让 ERP 依赖 `crm_customer`。

### 1.2 域边界

```
线索(Lead) ──转化──▶ 商机(Opportunity) ──报价──▶ 报价单(Quotation) ──转订单──▶ ERP 销售订单
   │                      │                            │
   │                      │                            └──▶ 合同(Contract) ──审批/签署──▶ 生效执行
   │                      │                                                      │
   └────────▶ 客户(CRM Customer) ◀──公海池(CustomerPool)──┘                      └──▶ 发票(Invoice)
                    │
                    ├── 客户跟进(FollowUp) / 客户分级(Grade)
                    ├── 外勤拜访：拜访规划 → 拜访执行 → 拜访检视
                    └── 报表：销售漏斗 / 客户分析
```

---

## 2. 模块菜单结构与文档清单（`sys_menu` 实测 2026-09-18）

### 2.1 菜单树（devdb 直查，递归展开）

```
60007  CRM                                    (mega:crm, sort=700)
├─ 60701  客户管理                            (mega:crm:customer, sort=100)
│   ├─ 80200  客户        crm:customer        双入口  crm/customer/index  ↔ crm/customer/form   tag=添加
│   ├─ 80201  客户跟进    crm:customer-follow 单入口  crm/customer-follow
│   ├─ 70303  客户分级    crm:customer-grade  单入口  crm/customer-grade
│   └─ 80202  客户公海    crm:customer-pool   单入口  crm/customer-pool   ★2026-09-18 新增（迁移 V11.383.0）
├─ 60101  外勤拜访                            (mega:crm:visit, sort=150)
│   ├─ 70001  拜访规划    sales:visit-plan    单入口  sales/visit-plan
│   ├─ 70002  拜访执行    sales:visit-exec    单入口  sales/visit-exec
│   └─ 70003  拜访检视    sales:visit-review  单入口  sales/visit-review
├─ 60702  线索管理                            (mega:crm:lead, sort=200)
│   ├─ 80210  线索        crm:lead            双入口  crm/lead/index  ↔ crm/lead/form   tag=添加
│   └─ 70311  线索转化    crm:lead-convert    单入口  crm/lead-convert
├─ 60703  商机管理                            (mega:crm:opportunity, sort=300)
│   ├─ 80220  商机        crm:opportunity     双入口  crm/opportunity/index  ↔ crm/opportunity/form  tag=添加
│   └─ 70321  商机阶段    crm:opportunity-stage 单入口 crm/opportunity-stage
├─ 60704  报价管理                            (mega:crm:quotation, sort=400)
│   └─ 70330  报价单      crm:quotation       双入口  crm/quotation/form  ↔ crm/quotation/index  tag=历史
├─ 60705  合同管理                            (mega:crm:contract, sort=500)
│   ├─ 80230  合同        crm:contract        双入口  crm/contract/index  ↔ crm/contract/form   tag=添加
│   └─ 70341  合同审批    crm:contract-approval 单入口 crm/contract-approval
├─ 60706  发票管理                            (mega:crm:invoice, sort=600)
│   └─ 70350  发票        crm:invoice         双入口  crm/invoice/form  ↔ crm/invoice/index  tag=历史
└─ 60707  CRM报表                             (mega:crm:report, sort=700)
    ├─ 70360  销售漏斗    crm:funnel          单入口  crm/funnel
    └─ 70361  客户分析    crm:customer-analysis 单入口 crm/customer-analysis
```

**菜单数**：1 个顶级 + 8 个分组 + **17 个页面项**（原 16 + 2026-09-18 新增的「客户公海」80202）。

> **⚠️ 双入口的两副面孔（易错）**：
> - **`path` = 列表页**（客户 / 线索 / 商机 / 合同）：菜单点进去是**台账列表**，标签 `添加` 跳到表单页 → `list_path` 指向 `*/form`；
> - **`path` = 表单页**（**报价单 70330 / 发票 70350**）：菜单点进去**直接进录单页**，标签 `历史` 跳到台账列表 → `list_path` 指向 `*/index`。
>
> 写文档/做验收时**不要默认 `path` 一定是列表页**，必须按 `sys_menu` 实测值判断。

### 2.2 页面 ↔ 文档对照表

| 分组 | 页面 | 菜单ID | menu_code | 路由 | 组件 | 文档 |
|------|------|:-----:|-----------|------|------|------|
| 客户管理 | 客户 | 80200 | `crm:customer` | `crm/customer/index` ↔ `crm/customer/form` | `views/crm/customer/{index,form}.vue` | [客户开发文档](./客户开发文档.md) |
| | 客户跟进 | 80201 | `crm:customer-follow` | `crm/customer-follow` | `views/crm/customer-follow/index.vue` | [客户跟进开发文档](./客户跟进开发文档.md) |
| | 客户分级 | 70303 | `crm:customer-grade` | `crm/customer-grade` | `views/crm/customer-grade/index.vue` | [客户分级开发文档](./客户分级开发文档.md) |
| | 客户公海 ★新增 | 80202 | `crm:customer-pool` | `crm/customer-pool` | `views/crm/customer-pool/index.vue` | 无独立文档（后端 `CustomerPoolController` 10 端点，见 §2.3） |
| 外勤拜访 | 拜访规划 | 70001 | `sales:visit-plan` | `sales/visit-plan` | `views/sales/visit-plan/index.vue` | [拜访规划开发文档](./拜访规划开发文档.md) |
| | 拜访执行 | 70002 | `sales:visit-exec` | `sales/visit-exec` | `views/sales/visit-exec/index.vue` | [拜访执行开发文档](./拜访执行开发文档.md) |
| | 拜访检视 | 70003 | `sales:visit-review` | `sales/visit-review` | `views/sales/visit-review/index.vue` | [拜访检视开发文档](./拜访检视开发文档.md) |
| 线索管理 | 线索 | 80210 | `crm:lead` | `crm/lead/index` ↔ `crm/lead/form` | `views/crm/lead/{index,form}.vue` | [线索开发文档](./线索开发文档.md) |
| | 线索转化 | 70311 | `crm:lead-convert` | `crm/lead-convert` | `views/crm/lead-convert/index.vue` | [线索转化开发文档](./线索转化开发文档.md) |
| 商机管理 | 商机 | 80220 | `crm:opportunity` | `crm/opportunity/index` ↔ `crm/opportunity/form` | `views/crm/opportunity/{index,form}.vue` | [商机开发文档](./商机开发文档.md) |
| | 商机阶段 | 70321 | `crm:opportunity-stage` | `crm/opportunity-stage` | `views/crm/opportunity-stage/index.vue` | [商机阶段开发文档](./商机阶段开发文档.md) |
| 报价管理 | 报价单 | 70330 | `crm:quotation` | `crm/quotation/form` ↔ `crm/quotation/index` | `views/crm/quotation/{form,index}.vue` | [报价单开发文档](./报价单开发文档.md) |
| 合同管理 | 合同 | 80230 | `crm:contract` | `crm/contract/index` ↔ `crm/contract/form` | `views/crm/contract/{index,form}.vue` | [合同开发文档](./合同开发文档.md) |
| | 合同审批 | 70341 | `crm:contract-approval` | `crm/contract-approval` | `views/crm/contract-approval/index.vue` | [合同审批开发文档](./合同审批开发文档.md) |
| 发票管理 | 发票 | 70350 | `crm:invoice` | `crm/invoice/form` ↔ `crm/invoice/index` | `views/crm/invoice/{form,index}.vue` | [发票开发文档](./发票开发文档.md) |
| CRM报表 | 销售漏斗 | 70360 | `crm:funnel` | `crm/funnel` | `views/crm/funnel/index.vue` | [销售漏斗开发文档](./销售漏斗开发文档.md) |
| | 客户分析 | 70361 | `crm:customer-analysis` | `crm/customer-analysis` | `views/crm/customer-analysis/index.vue` | [客户分析开发文档](./客户分析开发文档.md) |

> **16 个菜单页 ↔ 16 篇页面文档**，一一对应，无多余、无遗漏。

### 2.3 无菜单的孤儿实现（必须在文档中如实标注，不得当作"已完成"）

| 项 | 位置 | 实情 |
|---|---|---|
| ~~客户公海池~~ | 后端 `CustomerPoolController` → `/api/crm/customer-pool/*`（10 个端点，表 `crm_customer_pool` 30 列） | ✅ **2026-09-18 已闭环**：新建页 `views/crm/customer-pool/index.vue` + 菜单 `80202`（迁移 `V11.383.0`）+ `customerPoolApi` 10 个方法。原本「前端无页面、`sys_menu` 无菜单，后端能力齐全但用户不可达」。 |
| **CRM 供应商页** | `views/crm/supplier/index.vue`（1494 行）+ 路由 `crm/supplier/index`（`router/dynamicRoutes.ts:69`） | **无菜单绑定**的孤立路由（全仓无任何迁移把菜单 component 指到它）；且其数据源是 ERP 的 `supplierApi`（`/supplier/*`），**不属于 CRM 域**，`产品/评估/门户/联系记录` 四个弹窗为**假数据桩**。 |
| **营销活动** | 后端 `MarketingCampaignController` → `/api/crm/marketing/*`（25 个端点，5 张 `crm_marketing_*` 表） | 前端零引用、无菜单。与《营销模块》职责重叠，归属待裁定。 |

---

## 3. 本模块当前实现现状（2026-09-18 源码核对）

> **2026-09-18 本轮升级后**：17 个菜单页（原 16 + 新增「客户公海」）**已全部落到路线 A / A′ / B（报表）**，
> 外壳、列配置齿轮、页面配置弹窗、经典分页已齐；8 条 P0 与文档点名的 P1 已逐条处置。
> 下表为**升级后的实际情况**；仍存的缺口见 §7 与各页文档「剩余缺口」章。

### 3.1 页面外壳符合度（本轮升级后）

| 页面 | 外壳（当前） | 路线 | 状态 |
|------|-------------|:----:|------|
| 客户（80200） | `ErrorBoundary > PageContainer(full-height) > CategoryListLayout(等级左树) > BillTableList` | A | ✅ |
| 客户跟进（80201） | 同上（`BillDetailTable` + `StandardPagination`） | A | ✅ |
| 客户分级（70303） | 同上（等级左树 + 5 张统计卡，卡可下钻） | A | ✅ |
| **客户公海（80202，本轮新建）** | 同上（池状态左树 + 统计卡） | A | ✅ |
| 拜访规划 / 执行 / 检视（70001-3） | 同上（原 `ARReportPage` 路线 B → 路线 A） | A | ✅ |
| 线索（80210） | 同上 | A | ✅ |
| 线索转化（70311） | 同上 | A | ✅ |
| 商机（80220） | 同上（管道看板 / 列表 / 统计三视图共用外壳） | A′ | ✅ |
| 商机阶段（70321） | 同上 | A | ✅ |
| 报价单（70330） | 同上（菜单 `path` 是表单页，`list_path` 才是台账） | A | ✅ |
| 合同（80230） | 同上 | A | ✅ |
| 合同审批（70341） | 同上 | A | ✅ |
| 发票（70350） | 同上（3 个 Tab，各带独立列/查询/按钮/storage-key） | A | ✅ |
| 销售漏斗（70360） | `ErrorBoundary > PageContainer(full-height)` + 报表组件 | B | ✅（纯报表，按指南保留路线 B） |
| 客户分析（70361） | 同上 | B | ✅（同上） |

> **客户公海（80202）为本轮新增页**：后端 `CustomerPoolController` 的 10 个端点此前**前端零引用、`sys_menu` 无菜单**，
> 用户完全不可达（原 §2.3「无菜单的孤儿实现」）。本轮已建页并补菜单（迁移 `V11.383.0`）。

### 3.2 全模块共性问题的处置结果（2026-09-18 本轮）

> 下列第 1–14 条是**升级前**的全模块共性问题清单（保留作为问题档案）。
> **本轮逐条处置结果见下表**；仍未闭环的项在 §7 与各页「剩余缺口」中如实登记。

| # | 问题 | 处置 |
|:-:|------|------|
| 1 | 无 `CategoryListLayout` 外壳 | ✅ 17 页全部落位（客户/分级/公海带等级·状态左树） |
| 2 | 无 `PageConfigPanel` | ✅ 全部列表页补齐，且同传两个 `default-*-config` |
| 3 | 列 `formatter` 对象解构签名错配 | ✅ 改为位置参数或改用插槽渲染 |
| 4 | 桩按钮（约 20 处） | ✅ 有端点的已接线；无端点的**明确提示**并登记缺口（不造假数据） |
| 5 | 前端字段名与后端契约不一致 | ✅ `status` 统一 `1=正常`；`contractType`/`invoiceType` 改数字枚举；商机阶段统一 1–5 |
| 6 | 4 个端点前端在调而后端不存在 | ✅ 后端全部补齐（见 §7） |
| 7 | 三表缺 `tenant_id` 列 → 整页 500 | ✅ 迁移 `V11.379.0` 补列 |
| 8 | 权限码仅 5 条 / 合同审批无鉴权 | ✅ 迁移 `V11.379.0` 补至 57 条；合同 submit/approve/reject 加鉴权 |
| 9 | 号段 `selectCount+1` 撞唯一约束 | ✅ 5 处改为「当日最大号 + 1」（取值含已逻辑删除行） |
| 10 | `status` 两套相反语义 | ✅ 统一 `1=正常 / 0=停用` |
| 11 | 审计列 `created_by`/`updated_at` 不填充 | ⚠️ 未闭环（`MetaObjectHandler` 只填 `createTime`/`updateTime`/`tenantId`，属全仓共性） |
| 12 | 无模块级 E2E | ✅ 新增 `tools/e2e-crm.cjs` |
| 13 | 无验收截图资产 | ✅ UI 节自动截屏到 `tool-results/e2e-crm/` |
| 14 | 业务数据几乎为空 | ✅ E2E 自造数并在结束前清理干净 |

---

### 3.2.1 升级前的共性问题原文（问题档案，保留备查）

1. **无 `CategoryListLayout` 外壳**：全部列表页缺分类树位与统一 Tab 条（CRM 天然需要"客户分类/客户等级/线索来源/商机阶段"左侧过滤树）。
2. **无 `PageConfigPanel`**：CRM 各页**均未实测过对标**（本模块无对标系统），查询条件/功能按钮的显隐**尚未收口**；按金标准第 5 项，本模块应**统一提供**页面配置弹窗（因为无对标页可参照，一律按本系统标准形态做）。
3. **列配置未接入表头齿轮**：多数页列定义是 vxe 风格 `formatter: ({row}) => ...` **对象解构**，而 `BillDetailTable` 以 **位置参数** `col.formatter(raw, record)` 调用（`BillFormPage/BillDetailTable/index.vue:1415,1422`）→ **格式化函数实际不生效**，页面靠插槽兜底。这是全模块统一的**运行期缺陷**。
4. **桩按钮**：客户（批量分配、跟进记录、订单记录、合同记录）、线索（导入、分配、批量转化、查看详情、添加跟进、跟进记录）、商机（保存表单、移动阶段、转订单、添加跟进、创建报价、关闭商机）、报价单（批量发送、下载PDF、版本历史）、合同（批量审批、下载合同、续签申请、开票申请）—— 详见各页「实现差异说明」。
5. **前端字段名与后端契约不一致**：`status` 存在 **两套相反语义**（`customer/index.vue` 用 `0=正常/1=停用`，`customer/form.vue` 与 `customer-grade` 用 `1=正常/0=停用`）；`contractType`/`invoiceType`/`quotationStatus` 前端发字符串、后端是 `Integer`/英文枚举；`opportunity.stage` 前端用字符串 key（`lead`/`qualification`…）而后端是数字 1–6。
6. **端点缺口（前端已调用、后端不存在 → 必然 404）**：

   | 端点 | 调用方 | 后果 |
   |------|--------|------|
   | `PUT /api/crm/opportunity/{id}/stage` | `opportunityApi.updateStage` | 商机看板**拖拽改阶段必挂** |
   | `POST /api/crm/lead/batch-convert` | `leadApi.batchConvert` | 线索页/线索转化页「批量转化」必挂 |
   | `DELETE /api/crm/quotation/{id}` | `quotationApi.delete` | 报价单「删除」必挂 |
   | `PUT /erp/invoice/{id}` | `invoiceApi.update` | 发票表单页「编辑」必挂 |

7. **多租户缺列（P0，预期整页 500）**：`crm_customer_lead`(32列)、`crm_customer_opportunity`(33列)、`crm_customer_follow_up`(29列) **没有 `tenant_id` 列**（devdb 实测 2026-09-18），且**不在** `MyBatisPlusConfig.IGNORE_TENANT_TABLES` 白名单里 →
   MyBatis-Plus 多租户拦截器会注入 `AND tenant_id = ?` → **线索页 / 商机页 / 客户跟进页在登录态下读路径预期 500**。
   > 与 `shop_template` 先例同源（见《交易模块/_开发指南-金标准》陷阱 15）。修法二选一：**补 `tenant_id` 列**（推荐，与 `crm_customer`/`crm_contract` 一致）或加入忽略表。
   > **同一坑的父子表**：`crm_customer`(53列)、`crm_customer_pool`(30)、`crm_contract`(76)、`crm_quotation`(65)、`crm_visit_plan`(19)、`crm_visit_record`(20) **都有** `tenant_id` —— 即三张表是**漏建**，不是设计如此。

8. **权限码大面积缺失**：`sys_permission` 中 `crm%` 只有 **5 条**（`crm:customer:create|delete|list|update` + `crm:manage`）。
   而前端大量使用 `v-permission="'crm:lead:view'"`、`crm:opportunity:edit`、`crm:contract:batchapprove`、`crm:quotation:batchsend`、`crm:invoice:view` 等**未登记**的权限码 →
   **受控按钮在非超管账号下预期全部隐藏**（须真机实测确认，参见《_开发指南-金标准》§六之三「清单与实际鉴权不一致」的既有先例）。
   > **同一坑**：后端 `ContractController` 的 `submit/approve/reject` 三端点**无权限注解** → 任何登录用户均可自审。

9. **号段生成器会撞号**：`CustomerLeadServiceImpl` / `CustomerFollowUpServiceImpl` / `VisitPlanServiceImpl` 等用
   `selectCount(...) + 1` 拼编号，而唯一索引（如 `uk_crm_visit_plan_no`）**不含 `deleted` 条件** →
   **「删一条再建一条」必撞唯一约束（500）**；且序号是全局流水、非当日流水。
   > 应改为号段服务（`next-no`），并检查存量唯一索引定义。

10. **`status` 单列默认值与新老口径不一致（P0 连锁）**：devdb 实测 `crm_customer.status` 的**列默认值已是 `1`**
    （与 `form.vue` / `customer-grade` / `customer-analysis` / `/dropdown` 的"1=正常"一致），
    **只有客户列表页 `customer/index.vue` 按 "0=正常" 判读** ——
    后果：**列表页显示"正常"的客户，在客户跟进页的客户下拉（`/dropdown` 固定 `status=1`）里看不到，无法为其建跟进**。
    > 该缺陷是 §3.2-5 的具体化，**P0 必须先修**，否则后续验收全部失真。

11. **审计列命名决定行为（隐性坑）**：全仓 `MetaObjectHandler` 只填 `createTime` / `updateTime` / `tenantId`。
    CRM 实体若映射的是 `created_at/updated_at/created_by/updated_by` 一组，则
    **`created_by`/`updated_by` 恒 NULL、`updated_at` 永不刷新**（`crm_visit_plan` 已实测如此），
    `version` 也无实际并发保护。改实体/迁移前必须先核对映射的是哪一组（`crm_customer` 两组并存）。

12. **无模块级 E2E**：`tools/` 与 `tools/acceptance/` 下**不存在** `e2e-crm*.cjs` / `verify-crm*`；本模块**从未跑过端到端验收**。
13. **无验收截图资产**：`C:/Users/Administrator/Pictures/编程软件截图/企智连截图/` 下**没有 CRM 模块目录**。
14. **业务数据几乎为空**：devdb 实测 `crm_customer` / `crm_customer_lead` / `crm_contract` / `crm_visit_plan` **均 0 行**，`crm_quotation` **2 行** —— 任何 UI 验收都必须**先造数**。

### 3.3 后端能力盘点（**后端远比前端完整**）

| 域 | 控制器 | 端点数 | 表 | 说明 |
|----|--------|:-----:|----|------|
| 客户 | `CustomerController` | 16 | `crm_customer`(53列) | 含导入/导出/下拉/跟进 |
| 线索 | `CustomerLeadController` | 10 | `crm_customer_lead`(32列) | 含 `/convert` 转化 |
| 商机 | `CustomerOpportunityController` | 13 | `crm_customer_opportunity`(33列) | 含 advance/win/lose/statistics |
| 跟进 | `CustomerFollowUpController` | 9 | `crm_customer_follow_up`(29列) | 按客户/商机/线索三向查询 |
| 公海池 | `CustomerPoolController` | 10 | `crm_customer_pool`(30列) | **前端未接** |
| 报价 | `QuotationController` | 29 | `crm_quotation`(65) + `crm_quotation_item`(31) | 含版本/审批/发送/转订单（**真实写 `erp_sale_order`**） |
| 报价模板 | `QuotationTemplateController` | 15 | `crm_quotation_template`(28) + `_item`(20) | 前端未接；`from-template` 忽略全部参数（假实现） |
| 合同 | `ContractController` | 40 | `crm_contract`(76) + 条款/付款/变更/附件 4 张 | 含审批/签署/生效/完成/终止/续签/执行进度/付款计划/变更 |
| 外勤拜访 | `VisitController` | 11 | `crm_visit_plan`(19) + `crm_visit_record`(20) | 规划/执行/检视（含统计汇总） |
| 营销活动 | `MarketingCampaignController` | 25 | `crm_marketing_*`(5张) | **前端零引用** |
| 发票 | **不在 crm 模块** | — | `erp_finance` 下 `/api/erp/invoice/*` | CRM 发票页调的是 ERP 财务模块端点 |

---

## 4. 与 ERP / 其他模块的接线点

| 方向 | 接线点 | 实现位置 | 状态 |
|------|--------|---------|------|
| CRM → ERP 客户 | `crm_erp_customer_mapping`（18列） | 表已建 | ⚠️ 无 Service/Mapper 读写，**未接线** |
| 报价 → 销售订单 | `QuotationMapper` 内 `@Insert` 直写 `erp_sale_order` | `quotation/mapper/QuotationMapper.java` | ✅ 已实现（`POST /crm/quotation/{id}/convert`） |
| 商机 → 报价 | `POST /crm/quotation/from-opportunity/{opportunityId}` | `QuotationController:111` | ✅ 端点存在（仅填 `opportunityId`+日期，不复制商机数据） |
| 报价 → 合同 | `POST /crm/contract/from-quotation/{quotationId}` | `ContractController:106` | ✅ 端点存在 |
| CRM 客户 → 会员/积分 | — | — | ⚠️ 未接线（会员在营销模块） |
| 合同 → 财务凭证 | — | — | ⚠️ 未接线（对比配送结算已有推 ERP 记账） |
| 客户 → 应收/信用 | `CustomerCreditService` | `customer/service/CustomerCreditService.java` | ⚠️ 存在但未见调用方（详见《客户开发文档》） |

---

## 5. 业界对标口径（本模块无对标页）

### 5.1 为什么不能照 ql361 抄

ql361（来肯企汇）是**快消品进销存 + 商城**系统，**没有 CRM 域** —— 它的"客户"就是 ERP 往来单位。本模块**必须**按业界成熟 CRM 产品的能力集建模，否则会退化成"第二套客户档案"。

### 5.2 四家参照系统与主责能力

| 系统 | 官方文档入口 | 本模块主要参照点 |
|------|-------------|-----------------|
| **Odoo 17/18** | `https://www.odoo.com/documentation/18.0/`（各页文档正文引 17.0 根地址，**精确章节见 §5.4**） | 线索/商机同表模型与转化、相似线索合并、销售管道与阶段、预测式线索评分、活动(Activity)驱动跟进、报价单有效期、订阅合同与续约、从订单开票、贷项通知单 |
| **SAP Sales Cloud / S/4HANA Sales** | `https://help.sap.com/` | 线索→商机→报价→订单文档流、销售阶段与赢率、活动管理、大纲协议(Contract/Scheduling Agreement)、信用管理(信用额度/敞口/冻结)、开票计划 |
| **金蝶云·星空** | `https://vip.kingdee.com/`、`https://help.kingdee.com/` | 线索池/公海池（自动回收、领取上限、分配规则）、客户价值分级、商机阶段与赢率、合同审批流 |
| **用友 YonSuite** | `https://success.yonyou.com/`、`https://fwq.yonyou.com/` | 线索→商机→报价→合同→订单的全流程闭环、审批流挂接（协同云统一配置，支持多级/分支条件） |
| （补充）HubSpot / Salesforce / D365 | `https://knowledge.hubspot.com/`、`https://help.salesforce.com/`、`https://learn.microsoft.com/dynamics365/` | 线索评分(Lead Scoring)、去重规则、客户 360/健康度、流失预警 |

### 5.3 借什么、不借什么（沿用 2026-09-15 已裁定口径）

1. **借**：做法、字段、交互形态（状态机、卡片式布局、步骤条、向导弹窗、二次确认、幂等约束、重试分流、启停/测试态切换）。**卡片式也是交互，要借鉴。**
2. **实现**：能用现成组件就用现成组件；现成组件不满足**可以新建**；但**颜色、样式、字体必须与本系统现有代码统一**。
3. **留痕**：官方文档引用保留 —— 标题 + URL + 章节 + 查阅日期，并标注口径来源是**官方文档**还是**行业实践**。
4. **不借**：业界界面的**视觉样式**（配色/圆角/阴影/字体/组件外观皮肤）；**文档中不使用业界系统截图**。
5. **不足处按业界补齐**：无对标页 → 按业界成熟经验做，但**不发明本系统无法支撑的字段**（无数据源的能力如实标注为缺口，不造假数据）。

### 5.4 对标取证底稿（本模块的"事实源"，各页 业界对标 章的出处）

> **底稿**：`docs/Yh-Spec/抓取结果/业界对标-CRM模块-20260918.md`（2026-09-18 调研，含证据可信度分级 A/B/C/D、逐条 URL、来源汇总）。
> 各页文档「业界对标」章的**参考来源行只写官方文档根地址**（避免深链失效），**逐条精确出处以本底稿为准**。

**证据可信度分级**（底稿通用口径，引用时必须带级别）：

| 级别 | 含义 |
|:----:|------|
| **A** | 厂商官方产品文档 / 源码内文档 |
| **B** | 厂商社区官方问答、厂商授权服务商文档、官方镜像站 |
| **C** | 第三方教程、博客、集成商文章 |
| **D** | 推论或未能证实 |

**A 级核心来源（按能力，逐条可点）**：

| 能力 | 系统 | 来源 |
|------|------|------|
| 线索→商机转化 | Odoo | `https://www.odoo.com/documentation/18.0/applications/sales/crm/acquire_leads/convert.html` |
| 相似线索合并/去重 | Odoo | `https://www.odoo.com/documentation/18.0/applications/sales/crm/pipeline/merge_similar.html` |
| 销售团队与线索分配 | Odoo | `https://www.odoo.com/documentation/18.0/applications/sales/crm/pipeline/manage_sales_teams.html` |
| 预测式线索评分 | Odoo | `https://www.odoo.com/documentation/18.0/applications/sales/crm/track_leads/lead_scoring.html` |
| 未跟进线索报表（停滞） | Odoo | `https://www.odoo.com/documentation/18.0/applications/sales/crm/track_leads/unattended_leads_report.html` |
| 预期收入 / 预测报表 | Odoo | `https://www.odoo.com/documentation/18.0/applications/sales/crm/performance/expected_revenue_report.html`、`.../forecast_report.html` |
| 合同/续约（以 Subscriptions 承载） | Odoo | `https://www.odoo.com/documentation/18.0/applications/sales/subscriptions.html`、`.../renewals.html` |
| 活动（Activity）驱动跟进 | Odoo | `https://www.odoo.com/documentation/18.0/zh_CN/applications/essentials/activities.html` |
| 贷项通知单（红冲） | Odoo | `https://www.odooai.cn/documentation/18.0/applications/finance/accounting/customer_invoices/credit_notes.html` |
| 阶段模型源码文档 | Odoo | `addons/crm/doc/stage_status.rst`（源码内文档） |
| 线索认定→商机（对标模型） | Dynamics 365 | `https://learn.microsoft.com/en-us/dynamics365/sales/qualify-lead-convert-opportunity-sales` |

**⚠️ 底稿明确列出的「未找到官方文档佐证」清单（12 项，引用时不得以"业界标准如此"口径描述）**，其中与本模块直接相关的几条：

| 能力 | 检索结论 | 对本模块的影响 |
|------|---------|---------------|
| **Odoo 公海池 / 自动回收** | **未找到任何 Odoo 官方文档** | 「客户公海池」的业界依据应引**金蝶/用友（B/C 级）**，**不可写"Odoo 如此"** |
| Odoo Lead Mining 独立功能 | 未获取官方文档页 | 去重口径以「相似线索合并 + Data Cleaning 去重规则」为准 |
| Odoo Rotting / Days to rot | **信息冲突**（有称 19 才引入），18.0 官方无专门章节 | 官方认可的替代做法是 **Unattended Leads Report** |
| Odoo 报价 revision / 版本对象 | 无官方文档，基础 Sales 无原生版本链对象 | 本系统 `parent_id` + `new-version` 属**本系统建模** |
| 独立「Contracts」应用 | 官方以 **Subscriptions** 命名；独立 Contracts 是 **OCA 社区模块** | 合同章节引用时须区分 |
| 金蝶官方帮助中心原文 | **未直接命中** `help.kingdee.com` / `vip.kingdee.com` 正文页 | 金蝶来源为 **B/C 级（服务商站、云社区）**，引用须标注 |
| 金蝶外勤签到/定位打卡 | 未找到官方文档 | 「定位打卡」依据应引 **SAP FSM** 或标注为行业实践 |
| Salesforce Customer Health Score | 无该命名的官方文档 | 该能力实际由 Einstein Discovery 流失预测输出 |

> **写作纪律**：凡引用上表能力，**必须**在文档中如实标注来源级别；**D 级（推论/未证实）一律不得写成"业界标准"**。

**⚠️ 两条最易踩空的能力，正确出处在这（**不要**引 Odoo）**：

| 能力 | Odoo 有无官方文档 | 应引的正确出处 |
|------|:---------------:|---------------|
| **客户公海池 / N 天无跟进自动回收 / 领取上限** | ❌ **无** | 用友云社区《CRM-客户公海&客户》`https://success.yonyou.com/community/askDetail?aId=70b0ca9d9b813ac65dc1122f42dc03920dd14276665dbbb7&cid=97970563a237e994&themeType=3`（官方交付社区，**B**）；纷享销客《7.3.2 客户和公海管理》（同业，**C**）；简道云《公海池设置》（同业，**C**） |
| **外勤定位打卡 / 签到签退 / 地理围栏** | ❌ 金蝶无官方佐证 | **SAP Help Portal《Visits》(A 级)** `https://help.sap.com/docs/sap-cloud-for-customer/solution-guide-for-sap-sales-cloud/visits`；用友云社区《CRM-行为管理操作手册》（**B**，含「行动类型可要求必须签到签退」）；简道云《销售外勤管理》（同业，**C**，含"签到范围限定 500 米防代签"、水印拍照） |

### 5.5 16 个页面的业界覆盖度自检（底稿结语，逐页可查）

> 摘自底稿《结语：对 16 个 CRM 页面覆盖度自检》。**"主要证据缺口"列非空 = 该页某些能力查不到厂商官方文档，撰写时必须降级标注**。

| 页面 | 已取得业界做法 + 出处 | 主要证据缺口 |
|------|---------------------|-------------|
| 客户 | Odoo `res.partner` + Partner Levels；用友客户 360；Salesforce Account 360；SAP BP | — |
| 客户跟进 | Odoo `mail.activity` + Activity Plans；SAP Activity Management；用友行动管理 | — |
| 客户分级 | Odoo Partner Level/Weight；金蝶客户优先级 1–99 + 四维分级 + 信用等级；用友客户评测/客户价值模型 | 金蝶分级为**第三方归纳** |
| 拜访规划 | SAP Visits + Route；纷享排线/路线；用友拜访推荐规则 | — |
| 拜访执行 | 用友行动类型签到签退；简道云定位拍照；SAP 拜访执行 | **金蝶外勤签到无官方佐证** |
| 拜访检视 | 简道云轨迹/覆盖率；畅捷通完成率/下单率/超期预警；SAP 产品审核+报告 | — |
| 线索 | Odoo `crm.lead` type=lead；SAP Lead；Dynamics Lead；用友线索池 | — |
| 线索转化 | Odoo Convert to Opportunity（Conversion Action / 合并）；Dynamics Qualify（Account+Contact+Opp，**最多 5 个商机**）；SAP 线索转化查重 | — |
| 商机 | Odoo `crm.lead` type=opportunity；SAP Opportunity；用友商机准入/分配 | — |
| 商机阶段 | Odoo `crm.stage`（sequence/probability/is_won/fold/requirements）；SAP Sales Phase（**Days in Phase、On Track/Slow/Stalled**）；金蝶阶段推进+成功概率；用友阶段属性+赢单率%+持续时间；Salesforce Stage+Forecast Category | — |
| 报价单 | Odoo Quotation（Validity Date / 模板）；SAP **Sales Quote Version**（Group ID / Active-Inactive）；用友销售报价（三方式/两类型/**变更历史**） | **Odoo 报价版本对象无官方佐证** |
| 合同 | SAP Outline Agreement（MK/WK/LP/LPA）；Odoo Subscriptions Renewal；用友销售合同下推链 | **Odoo 独立 Contracts 应用无官方佐证** |
| 合同审批 | 用友协同审批流程设计器（固定流/自由流/参与人/**分支条件**）；金蝶合同中心（电子签章） | — |
| 发票 | Odoo Credit Note（Reverse / Reverse and Create）；SAP Billing Plan + Billing Document；用友合同→订单→出库→发票 | — |
| 销售漏斗 | SAP Forecast/Pipeline；Odoo Prorated Revenue + Forecast/Pipeline 报表；金蝶销售漏斗报表；用友 CRM-决策分析-销售漏斗；通用转化率口径与基准 | — |
| 客户分析 | RFM 模型（Klaviyo / 阿里云 / Oracle Unity / Omnisend）；客户健康度（Pricefx / Churnkey / 专利）；Einstein Opportunity Scoring | **Salesforce 官方 "Customer Health Score" 文档无佐证**（实为 Einstein 流失预测输出） |

---

## 6. 写作与开发原则

- **两类配置严格分开**：查询条件/功能按钮 →「页面配置」；默认列 + 全量可配置列 →「数据表格列配置」。
- **不重复通用规范**：统一指向《对标开发技术参考文档》与《_开发指南-金标准.md》。
- **不发明字段**：列名/按钮逐字取自本系统源码；业界补充项**必须单独标注为"业界建议、本系统未实现"**。
- **页面组件优先复用**、**功能/模块不重复开发**（客户选择器、列配置、分页、上传、地区级联等一律复用现成组件）。
- **阶段结束前回写开发文档**：把「实现差异说明」更新为当前代码实际实现情况。
- **【金标准】模块级 E2E**：`tools/e2e-crm.cjs`（2026-09-18 新建，**真机 72/72 通过**）。
  用法 `FE_URL=http://localhost:<前端端口> CRM_PORT=<后端端口> node tools/e2e-crm.cjs`；
  `CRM_ONLY=api` 可只跑接口 + DB 对账节。覆盖 6 节：菜单完整性(5) · P0 修复(9) · 写路径闭环(14) ·
  17 页主查询连通性(18) · UI 17 页遍历+截屏(18) · 自造数据清理(8)。
  截图落 `tool-results/e2e-crm/`；造数一律带 `E2E-CRM-` 前缀并在结束前**物理清理**。
- **【金标准】启动进程随手关**：见 `AI_DEVELOPER_RULES.md` 7.3 会话收尾纪律；按端口/PID 精确 kill，勿用文件名批量匹配。

---

## 7. 全局缺口汇总（P0 → P2）

| 级别 | 缺口 | 影响 | 处置（2026-09-18 本轮） |
|:----:|------|------|---------|
| **P0** | `PUT /crm/opportunity/{id}/stage` 后端不存在 | 商机看板**拖拽改阶段必 404** | ✅ 已补端点（值域 1–5，越界 400；拖到「成交」按赢单口径收口，拖回则清赢单痕迹） |
| **P0** | `POST /crm/lead/batch-convert` 后端不存在 | 线索页「批量转化」**必 404** | ✅ 已补端点（逐条独立事务，单条失败不中断整批，返回成功/失败明细） |
| **P0** | `crm_customer.status` 两套相反语义（列表 `0=正常`，其余全部 `1=正常`；且**列默认值已是 1**） | 列表页判读与写入路径相互矛盾；**客户在跟进页下拉里消失** | ✅ 前端已统一 `1=正常 / 0=停用`（DB 列默认值本就是 1，无需迁移） |
| **P0** | `crm_customer_lead` / `crm_customer_opportunity` / `crm_customer_follow_up` **缺 `tenant_id` 列**且不在忽略表 | 三页登录态读路径**预期 500** | ✅ 迁移 `V11.379.0` 补列 + 索引（三表当时 0 行，`DEFAULT 1` 不翻转存量语义） |
| **P0** | `sys_permission` 仅 5 条 `crm%` 权限码，前端用了大量未登记码 | 受控按钮在非超管下**预期全隐藏** | ✅ 迁移 `V11.379.0` 按前端实际引用补至 **57 条**（幂等写法，不重复插入） |
| **P0** | 4 个端点后端不存在（商机 stage / 线索 batch-convert / 报价 delete / 发票 update） | 对应按钮**必 404** | ✅ 四个全部补齐（发票 `PUT /erp/invoice/{id}` 为 Jackson 合并的**部分更新**语义） |
| **P0** | 号段用 `selectCount+1`，唯一索引不含 `deleted` | **删一条再建一条必撞唯一约束** | ✅ 5 处改为「当日最大号 + 1」，取值 SQL **刻意不带 `deleted` 条件**（已逻辑删除的行仍占号）；新增 `cn.aiedge.crm.common.CrmDocNo` 收敛解析逻辑 |
| **P0** | 无客户公海页 | 后端 10 个端点 + 30 列表**完全不可达** | ✅ 新建页 `views/crm/customer-pool/index.vue` + 菜单 80202（迁移 `V11.383.0`）+ `customerPoolApi` 10 个方法 |
| **P1** | `BillDetailTable` 列 `formatter` 对象解构签名错配 | 全模块列格式化**静默失效** | ✅ 逐页改为位置参数 / 插槽渲染 |
| **P1** | 全部列表页缺 `CategoryListLayout` / `PageConfigPanel` | 未达金标准路线 A | ✅ 17 页全部升级（报表 2 页按指南保留路线 B，但已补 `ErrorBoundary` + 时段筛选） |
| **P1** | 桩按钮（约 20 处） | 用户点击无实际效果 | ✅ 有端点者已接线；无端点者改为**明确提示**并在此登记（不造假数据） |
| **P1** | `crm_erp_customer_mapping` 未接线 | CRM 客户与 ERP 客户**无映射**，转化后易重复建档 | 在 `lead/convert`、`customer/create` 补写映射 |
| **P1** | 合同审批为**单级**且**驳回原因不落库** | `reject()` 方法体内未写 `rejecterId`/`reason`；合同列表页的四级 `a-steps` 是**静态 UI**（后端 VO 无该五字段） | 补字段写入；多级审批按需扩 `crm_contract_approval` 表 |
| **P1** | 合同创建**无法保存**：前端 `contractType` 发字符串（`sales`），后端是 `Integer` | Jackson 探针实测 `contractType:"sales"` → **必 400** | 前端改数字枚举，或后端加反序列化兼容 |
| **P1** | 合同表单页 `form.vue` 用 `v-model` 绑 `useBasicForm` 的 `reactive` 常量 | 已知实踩陷阱（《billformpage-v-model-reactive-trap》）→ **表单根本存不了** | 改 `:model-value` + `@update:model-value` |
| **P1** | 报价单字段映射全面错位（`quotationName→title`、`contactPerson→contact_name`，`validDays`/`terms` **无对应列**，明细 `spec/unit/price/discount` 未对上 DTO） | 提交内容**静默丢弃**；DB 实证 `title`/`valid_to`/`contact_name` 恒 NULL | 逐字段对齐后端 DTO |
| **P1** | 报价单 `send` 的 `method` 是必填 `@RequestParam`，前端不传；且按钮只在草稿显示而后端要求"已审批" | **报价永远发不出去**（必 400） | 对齐状态机与参数 |
| **P1** | 发票走**纯 JPA**（`JpaRepository`），全仓无 `@Where`/`@TenantId` | **跨租户可见可改、作废票仍进列表** | 补租户/逻辑删过滤 |
| **P1** | 发票分页键错：后端是 Spring `Pageable`（认 `page`/`size`），前端发 `pageNum`/`pageSize` | **永远第 1 页 20 条** | 统一分页参数键 |
| **P1** | 发票汇总行 `summaryData` 缺 `key` 字段（组件期望 `{key,value}`） | 合计行**恒空** | 补 `key` |
| **P1** | 发票三个 Tab 未生效（`activeTab` 未参与查询） | 切 Tab 不改变结果 | 后端已有 `GET /erp/invoice/query?direction=sal|purchase`，接线即可 |
| **P1** | 商机「转订单」把 **CRM 客户 ID（甚至硬编码假值 1–5）** 写进 `erp_sale_order.customer_id` | 违反 §1.1 红线：ERP 单据必须取 ERP 往来单位 | 先建/映射 ERP 往来单位再转 |
| **P1** | 拜访"签到打卡"无定位能力（`location` 是自由文本） | 与业界移动签到标准差距最大 | `crm_visit_record` 的 `longitude`/`latitude`/`attachments` **三列已就绪**，补前端采集即可 |
| **P1** | 拜访记录与客户跟进记录**两张表互不相干** | 拜访完成不产生跟进记录 | 拜访保存时同步写一条 `follow_up_type=2 拜访` 的跟进记录 |
| **P1** | 跟进页无行操作（后端有 `PUT`/`DELETE`，前端未接） | 跟进记录**无法编辑/删除** | 补行操作 |
| **P1** | 跟进弹窗**两套并存**（客户页内嵌 vs 客户跟进页），且客户页那套字段名错（发 `followType`/`result`/`nextFollowTime`，实体是 `followUpType`/...） | Spring 默认忽略未知字段 → **静默丢失** | 收敛为一套并修字段名 |
| **P1** | 客户等级字典**已在 ERP**（`erp_customer_level`，含 `min_amount`/`min_frequency` 自动评级阈值列，`CustomerLevelController` 齐备），CRM 前端零引用 | 存在重复造字典风险 | **勿在 CRM 重造**，直接引用 ERP 字典 |
| **P1** | 报表页（销售漏斗/客户分析）**无任何时段筛选** | 无法做时间对比 | 补时段条件 |
| **P1** | 拜访检视 `byDate` 趋势数据后端已返回，前端未画图 | 能力闲置 | 补趋势图 |
| **P2** | `views/crm/supplier/index.vue` 孤儿路由 + 假数据 | 与 ERP 供应商重复实现 | 删除页面与路由，或改为重定向到 `supplier/index` |
| **P2** | `crm_marketing_*` 5 张表 + 25 端点零引用 | 职责与营销模块重叠 | 归属裁定后接线或下线 |
| **P2** | `QuotationTemplate` 前端未接 | 后端模板能力闲置 | 「从模板创建」补 UI |
| **P2** | 合同 `ContractChange`（变更）五端点前端完全未接；付款计划 `payments/*`、条款 `clauses/*`、附件 `attachments/*` 亦未接 | 后端高价值能力闲置 | 按《合同开发文档》缺口清单逐项接线 |
| **P2** | 外勤拜访三页 `menu_code` 前缀 `sales:` / 路由 `sales/*`，却挂在 CRM 菜单下 | 名实不符，归属混乱 | 改 `menu_code` 为 `crm:visit-*` 或明确归属 |
| **P2** | `PrintButton` 用 `Number(businessId)` | 雪花 ID 精度失真（组件级，影响全站） | 改字符串比对 |
| **P2** | 未安装 `MetaObjectHandler` 对 `created_by`/`updated_by` 的填充 | 审计字段恒 NULL、`updated_at` 不刷新 | 统一审计列命名 |
| **P2** | 无模块级 E2E、无验收截图资产、业务数据几乎为空（多数表 0 行） | 无法回归 | ✅ 已建 `tools/e2e-crm.cjs`（真机 **72/72 通过**），截图落 `tool-results/e2e-crm/` |

### 7.1 本轮**真机跑出来**的两条新 P0（原文档未登记）

> 这两条都是**静态读源码查不出、只有真机写一次才会暴露**的缺陷 —— 也印证了《_开发指南-金标准》§六之三
> 「编译通过 + 静态核对通过 ≠ 能用」。

| 级别 | 缺陷 | 真机证据 | 处置 |
|:----:|------|---------|------|
| **P0** | **`crm_customer` / `crm_quotation` / `crm_quotation_item` 三表缺自增主键序列**（同模块其余 **19 张**表都有）。实体统一写 `@TableId(type = IdType.AUTO)` ⇒ MyBatis-Plus 生成的 INSERT **不含 id 列** ⇒ 落 `id = NULL` ⇒ `null value in column "id" violates not-null constraint`。<br>**后果：客户建档、报价单创建、报价明细创建全部必然失败** —— `crm_customer` 真库 0 行正因如此；连带客户跟进（`customer_id` NOT NULL）与线索转化（`convertToCustomer` 抛异常）全线不可用。 | `POST /api/customer` → **HTTP 400**「请求数据不完整或存在冲突」；`POST /crm/lead/batch-convert` → `failures[].reason` 明写该约束 | ✅ 迁移 `V11.391.0` 补序列（起点 = 现有最大 id + 1）。**根因**：`V9.11.0` 建表写的是 `BIGSERIAL`，但真库实际生效的是**更早那份** `CREATE TABLE IF NOT EXISTS`（先执行者生效），那版 id 无 DEFAULT —— 与 §8 记载的 `erp_product_grade_price` 事故同源 |
| **P0** | **「客户公海」菜单行的 `tenant_id` / `client_type` 取值错**（照抄了 `V6.21.0` 的历史写法 `tenant_id=1` + `client_type='pc-admin'`，而 CRM 现有菜单实际都是 `tenant_id=0` + `client_type='tenant-admin'`）。`SysMenuServiceImpl.getMenuTree` 走 `listAllMenus(0L)`，**只取 `tenant_id = 0` 的菜单** ⇒ 该行虽在库里却**不进菜单树** ⇒ 前端不生成路由 ⇒ 直接访问 `crm/customer-pool` 落**全局 404 页**。 | E2E UI 节截图：客户公海页显示「404 抱歉，您访问的页面不存在」；`GET /menu/tree` 返回 379 条，**不含 80202** | ✅ 迁移 `V11.392.0` 订正（**不改** `V11.383.0` 正文，避免已应用迁移的 checksum 失配） |

### 7.2 同类坑（本轮顺带修掉）

| 缺陷 | 真机证据 | 处置 |
|------|---------|------|
| 拜访执行页存储键含 **`exec`** 触发安全拦截 | `GET /system/user-config/col-config/crm-visit-exec-table-columns` → **HTTP 400**（后端日志：`SecurityAspect 安全检查拦截 … 请求参数包含非法字符`） | ✅ 改为 `crm-visit-record-*`（与后端表名一致）。**这是全站性坑：存储键/导出名一律避开 `exec`/`select`/`update` 一类词** |
| 标准 `request` 实例误用绝对路径 | `GET /api/api/crm/customer-pool/page` → 404 | ✅ 改相对路径。**注意**：`postRaw`/`putRaw`/`getRaw` 走**原生 axios**（无 baseURL），那里的 `/api/...` 绝对路径是**有意为之，不要一起改** |
