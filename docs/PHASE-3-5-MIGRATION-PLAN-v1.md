# 阶段 3 + 5 迁移方案与回滚预案（v1）

> **本文件是什么**：`docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md` §11.3 里
> **阶段 3（往来单位升系统级）** 与 **阶段 5（单据四主体 + 角色）** 的**施工方案**。
> **本文件不是什么**：不改任何裁定（裁定都在 §3.4 / §6.1 / §7.7），也**不动代码** ——
> 它是"动手之前先把改动面、可逆性与回滚点写清楚"的那一份东西。
>
> **为什么这两阶段必须连做**（§11.5）：只做阶段 3（主体升系统级）会让"单据上的主体字段"无处落；
> 只做阶段 5 则主体还是租户内的旧形态 ⇒ 两者一起才闭环。
>
> **状态**：⬜ 待评审（本文档 = D1(c) 的产出）。**评审通过前不开工**。

---

## 一、前置事实（2026-09-23 真库实测，**方案必须建在这些数字上**）

| 事实 | 实测值 | 对方案的直接影响 |
|---|---|---|
| `biz_party` 总行数 | **152**（含软删） | 与文档口径一致 |
| **未删行数** | **19**（tenant_id=1 有 15、tenant_id=0 有 4） | ⚠️ **133 行是软删残留**。司法解释与归并的对象应当只有 19 行；"存量 152 行"是含软删的计数 |
| `unified_code`（统一社会信用代码） | **152 行全为空**，不同值 0 个 | 🔴 **决定性发现**：见 §3.4 —— 唯一合法识别键**在存量里根本没被填过** |
| 不同名称数 | 142 | 名称不作归并依据（裁定④），仅作核对线索 |
| **引用面（含 `*_party_id` / `customer_id` / `supplier_id` / `partner_id` / `contact_id`）** | **100 张表** | 阶段 3 的改动面上限 |
| 其中含 `customer_id` / `supplier_id` | **64 张表**（`customer_id` 38 · `supplier_id` 29） | 阶段 5 要动的主体列面 |
| `partner_id` / `party_id` / `contact_id` | 25 / 10 / 7 张表 | ⚠️ **`partner_id` 未必指向 `biz_party.id`** —— 见 §3.2 |

> ⚠️ 上表两个加粗的数字（**19 行**、**信用代码全空**）**推翻了"自动归并即可"的执行前提**，
> 这是本方案与 §5.3 原文之间**必须由你重新裁定**的一处（见 §3.4）。

---

## 二、阶段 3：往来单位升系统级（R2 拆两层 + R1 转正）

### 3.1 目标形态（引用既有裁定，不重复论证）

| 目标结构 | 依据 | 现状 |
|---|---|---|
| `party`（系统级主档，`unified_code` **唯一**） | §3.4.1 / §5.3 | ❌ 不存在（现在是 `biz_party` 兼着两个角色） |
| `party_tenant`（**按 `direction` 的有向边**） | ⑲ / §3.4.2b | ❌ 不存在 |
| `person_party`（R1，自然人×往来单位） | §3.4.2 / §7.7 G13 | 🔶 壳：`shop_user_party_link`（**0 行**，但审批字段齐全） |
| `party_alias` / `party_change_log` / `party_relation` / `merge_request` | §7.6 U1 | ❌ 全部不存在 |

### 3.2 第一步**不是建表**，是"引用面判定"（本方案最重要的建议）

100 张表里到底哪些列**真的**存 `biz_party.id`？现在**没人能回答**：
本仓**几乎没有外键约束**，列名也不统一（`customer_id` / `supplier_id` / `partner_id` / `party_id` / `contact_id`）。

⚠️ 典型陷阱：`partner_id` 有 25 张表在用，但它**可能指向别的表**（例如合作伙伴主档）。
**按列名猜引用关系 = 猜错就改错数据，而且不可逆。**

**产出物（开工的第 0 步）**：一张 `tools/refsurface.csv`（脚本生成，可复核），列：

| 表 | 列 | 判定 | 依据 | 读/写路径 | 是否单据主表 |
|---|---|---|---|---|---|
| `erp_sale_order` | `customer_id` | ✅ 指向 `biz_party.id` | 代码里 `MdCustomerController` 取数 + 注释 | 读+写 | 是 |

**判定方法（三条并证，缺一不可）**：
1. **代码证据**：该列被哪个 Mapper/Service 写入、又从哪里读出来展示（`grep` 到具体文件与行号）；
2. **数据证据**：抽样该列的值，看是否都能在 `biz_party.id` 里命中（命中率 <100% 的要单独列出来查为什么）；
3. **注释/文档证据**：实体类字段注释、开发文档里的说明。

**不在本步做的**：不改任何列、不建任何表。**这一步只出清单**。

#### 首轮实测结论（2026-09-23，`tools/refsurface.cjs` → `tools/refsurface.csv`）

候选 **111 处**（表×列），数据侧判定分布：

| 判定 | 处数 | 说明 |
|---|---|---|
| **指向 `biz_party`**（唯一 100% 命中） | **14** | 如 `erp_sale_outbound.customer_id` 32/32、`finance_receivable.customer_id` 6/6、`erp_expense_doc.partner_id` 28/28 |
| **同时命中 `biz_party`/`sys_user`**（数据侧**不可区分**） | **3** | ⚠️ `erp_sale_order.customer_id` 27/27、`dms_task.customer_id` 35/35、`crm_quotation.customer_id` 2/2 —— **必须看代码证据**，不能因为"先命中 biz_party"就下结论（本脚本第一版就是这么写的，已修正） |
| 部分命中（需人工） | 6 | 见下 |
| 指向 `biz_party_contact`（**联系人**，不是往来单位） | 2 | ⚠️ `erp_purchase_price_track.partner_id` 8/8、`finance_payable.supplier_id` 3/3 |
| 全空 / 不命中任何候选主档 | 其余 | 全空无从判定；不命中的多为**测试造数**（小整数 id） |

**代码证据已填（2026-09-23）—— 结果比预想的更值得警惕**：那 14 处里**只有 7 处有铁证**
（实体字段注释直接写明"客户ID → biz_party.id"一类），其余 7 处分别是：

| 处 | 问题 | 处置 |
|---|---|---|
| `erp_loyalty_coupon.partner_id` | ⚠️ **注释与数据打架**：注释写"关联往来单位**联系人**"，而数据 44/44 命中 `biz_party`、命中 `biz_party_contact` = 0（注释疑似过时） | 待人工确认 |
| `erp_capital_flow.party_id` | ⚠️ **多态引用**：紧邻 `partyType`（接口里叫「对方类型」）⇒ 按类型决定指向哪张表 | **不能当 `biz_party.id` 唯一处理** |
| `mkt_sms_consent` / `mkt_stored_card` / `mkt_stored_card_flow` `.partner_id` | 无注释；后两者数据只 1~2 行（**样本太少**）；前者的赋值来自某 `info` 的 id，来源未追清 | 待人工确认 |
| `erp_pre_receipt.customer_id` / `erp_partner_attachment.partner_id` | 无注释，写入来源未追清 | 待人工确认 |

**后续追证（同日）**：又结掉 2 处 —— `erp_loyalty_coupon.partner_id` **已结案**（`PromotionEngineImpl:349-350`
把它与 `req.getCustomerId()` **直接比较** ⇒ 同一域，实体注释「关联往来单位联系人」**过时**）；
`erp_pre_receipt.customer_id` 为**中等证据**（写入方 `PaymentBusinessIntegrationService:306`
写成 `setCustomerId(partyId)`，变量名即往来单位 id + 数据全命中，但样本只 2 行）。
⇒ **仍待人工确认的收敛到 4 处**：`erp_partner_attachment.partner_id` · `mkt_sms_consent.partner_id` ·
`mkt_stored_card.partner_id` · `mkt_stored_card_flow.partner_id`（后两者数据仅 1~2 行，**样本太少**，
靠现有数据无法定性）。**另**：`erp_capital_flow.party_id` 已定性为**多态引用**，须与 `partyType` 一起处理。

**✅ 2026-09-27 结案：这 4 处全部确认为 `biz_party`（往来单位），"样本太少"不再构成障碍。**

| 处 | 结案依据（**代码证据是决定性的那一证**） | 数据 |
|---|---|---|
| `erp_partner_attachment.partner_id` | `MdCustomerController:858` 用 `getPartnerId` 按 party id 统计附件、`:878` 随即按**同一个 id** 删主体；`PartyAttachmentController:29` 的路径变量本就叫 `partyId`；前端 `partner.ts:522` 以客户表单 id 调 `getByPartner` | 11/11 命中 `biz_party`；contact 命中 6/11 是**小整数主键重叠噪声**、非语义指向 |
| `mkt_sms_consent.partner_id` | `SmsSendServiceImpl:103` 取的 `receiverId` 来自 `MarketingQueryMapper:70` 的 `SELECT p.id AS partner_id FROM biz_party p` —— **来源追清了**，不是联系人 | 10/10 命中 `biz_party`、contact 0/10 |
| `mkt_stored_card.partner_id` | `StoredCardController:93` 收 card；前端 `stored-card/index.vue:675` 写 `issueForm.partnerId = record.id`，而 `record` 出自 `PartnerSelectModal`（数据源 `/erp/md/customer/page`，**往来单位单一口径**）；`StoredCardAccountingServiceImpl:96` 又把它当 `customerId` 推应收 | 1/1（样本虽少但**代码链完整**） |
| `mkt_stored_card_flow.partner_id` | `StoredCardServiceImpl:223` `setPartnerId(card.getPartnerId())`、`StoredCardExpireJob:105` 同源 ⇒ **纯衍生自卡**、非独立写入 ⇒ 随卡定性 | 1/1 |

结果已回写 `tools/refsurface.csv`（这 4 行的 verdict 从"待人工确认"改为"✅ 指向 biz_party"）。

⇒ **现在只剩 1 处是"不能当 `biz_party.id` 唯一处理"的**：`erp_capital_flow.party_id`（多态，
须与 `partyType` 一起处理）。**那不是"待确认"，是"待按类型分派"** —— 切读时它要单独一套处理。

⇒ **印证了方案 §3.2 的判断**：**"数据侧成立" ≠ "引用关系成立"**；"样本太少"这个理由本身也说明
**不能只靠数据**——上面 4 处最后都是被**代码证据**（谁把什么对象的 id 写进去）结掉的。

**三条必须记住的实测警告**：

1. ✅ **`erp_purchase_inbound.supplier_id` 命中 `biz_party` = 0 —— 已查清：不是代码缺陷，是数据不可测。**
   代码证据：`inbound.setSupplierId(order.getSupplierId())`（`PurchaseInboundServiceImpl:229`），
   即"入库单复制采购订单的供应商"，语义**正确**；再往上追到订单侧，实测值是
   `2099000000000000901`（E2E 造数 id）与 `100`（小整数），且 `supplier_name` 为 null。
   ⇒ **dev 库的采购单据是"用假 id 造的"，不对应任何真实 `biz_party` 行**
   （`biz_party` 未删仅 19 行），所以命中率必然是 0。
   **结论**：⚠️ **"数据侧 0 命中"不能作为"代码写错了"的证据**；
   但它有一个**真实后果**：**用这批数据端到端验证不了"应付侧三层结算口径按 `biz_party.id` 取档案"那条链路**
   —— 要验证必须先造一条"供应商指向**真实 `biz_party` 行**"的采购入库单（属**验收夹具**问题，不是产品缺陷）。
2. ⚠️ **`partner_id` 不是"往来单位"的同义词**：25 张表在用，实测已有 2 处指向**联系人表**。
   ⇒ 阶段 3/5 若按列名批量替换，**必然改错**（这正是"引用面判定"要排在建表之前的原因）。
3. ⚠️ **dev 库大量主键是小整数**，同一个值会同时落在多个主档里 ⇒
   **数据证据单独不足以定性**，三证并立里"代码证据"是决定性的那一证。

### 3.2b 逐列归位：`biz_party` 76 列各去哪（**这一步不做，第 3 步"切读"根本开不了工**）

实测（2026-09-23）：`biz_party` **76 列**，而新建的 `party`(14) + `party_tenant`(20) 合计只承接 **34 列**
⇒ **读方需要的列大半还只在旧表里**，"读路径逐表切换"因此**不能开始**。
先把每一列的去向定下来（这一步纯设计、不动代码），再谈双写与切读。

| 去向 | 列数 | 列 |
|---|---|---|
| **A → `party`**（主体主档，与租户无关的属性） | 27 | `party_code` `party_name` `short_name` `company_full_name` `mnemonic_code` `party_type` `legal_person` `unified_code` `business_license` `business_license_expiry` `tax_number` `bank_name` `bank_account` `bank_address` `fax` `legal_person_phone` `website` `email` `phone` `address` `region` `province` `city` `district` `latitude` `longitude` `status` |
| **B → `party_tenant`**（关系 / 商务条件，**逐方向**） | 24（+2 新增列） | `tenant_id` `credit_limit` `party_level` `current_debt` `settlement_type` `settlement_days` `credit_days` `payment_days` `payment_term_type` `fixed_payment_day` `fixed_credit_day` `settlement_day` `statement_day` `price_track_enabled` `default_handler_id` `default_handler_name` `promoter_id` `promoter_name` `buyer_account` `customer_source` `roles` `category_id` `warehouse_name` `last_trade_time`（另加 `effective_from`/`effective_to`，已在建表时留位） |
| **C → 会员子表（**归属待裁定**，见下）** | 12 | `member_card_no` `member_name` `member_level` `member_card_status` `member_valid_start` `member_valid_end` `member_total_consume` `member_issue_time` `member_initial_points` `points` `birthday` `customer_one_pass` |
| **D → 期初余额（随关系，或财务子表）** | 4 | `opening_payable` `opening_prepaid` `opening_receivable` `opening_pre_received` |
| **E → 技术列，不动** | 7 | `id`（**已复用为 `party.id`**）· `remark` · `deleted` · `create_by` · `create_time` · `update_by` · `update_time` |

**⚠️ 两处必须由你拍板（本节卡在这里，不给默认答案）**：

1. **C 组（会员属性）去哪** —— ✅ **2026-09-26 用户裁定：选乙**（**会员 = 自然人属性**，
   并入 `shop_user`；`biz_party` 不再有会员概念）。**落地前实测了三件事**（决定这一步的真实工作量）：
   · `party_level='MEMBER'` 的**只有 1 行**（散客 `id=1`），另有 2 行是等级名、其余 null ⇒ 语义面很小；
   · 但**有 4 行带 `member_card_no`**（会员卡数据与 `party_level` 并不一致）——
     而自然人侧 `shop_user` 现在 **0 行** ⇒ **那 4 张卡没有可承接的主体**；
   · 散客 `id=1` 被 **3 张销售出库单**引用 ⇒ 它**必须继续作为 `party` 存在**（交易要认主体），
     只是"它是 MEMBER 会员"这个**属性**不再由 `biz_party.party_level` 表达。
   ⇒ **C 组的处置**：12 列从 `biz_party` 移出（先停写、阶段 3 收口时删）；会员语义归 `shop_user`；
   ⚠️ **那 4 张卡的数据无处迁移**（自然人侧空）⇒ **导出留档 + 列弃用**，
   **不假装迁移**、也不给它们编一个自然人（"空库跑不通的路 = 从未验证过"的同一条纪律）。
2. **D 组（期初余额）** —— ✅ **2026-09-26 用户裁定：随关系**（`party_tenant.opening_*`），
   **但实施顺序排在"与财务口径对齐"之后**（它与财务期初模块 `InitialFinancePartner` 有耦合）。
   ⇒ 本组**暂缓**，不进第一批扩列。

**另外**：A 组落地后，`party` 会有 ~41 列 ⇒ **超过本仓"主表 ≤25 列"的规范**
（`docs` 里的开发技术规范）⇒ 应按需拆：证件/银行/地址三类**各自成子表**（`party_cert` / `party_bank` / `party_address`）。
**这一条必须在扩列之前定**，否则就是把 `biz_party` 的"上帝表"原样搬到 `party` 上。

### 3.2c A 组落地：**先拆三张子表，再扩列**（不然就是把上帝表原样搬家）

A 组 27 列若全部塞进 `party` ⇒ **41 列，超本仓"主表 ≤25 列"的规范**。
⇒ **扩列之前先拆子表**（三张，见下）；拆完 `party` 落到 **22 列**，留出余量。

| 表 | 承接哪些列 | 为什么单独成表 |
|---|---|---|
| **`party`**（薄主档，**22 列**） | `id` `tenant_id` `party_code` `party_name` `short_name` `company_full_name` `mnemonic_code` `party_type` `unified_code` `legal_person` `phone` `email` `website` `fax` `legal_person_phone` `status` `remark` + 技术列 5（`create_by/time`、`update_by/time`、`deleted`） | 只放"**一个主体只有一个值**"的识别与主联系方式 |
| **`party_cert`** | `business_license` `business_license_expiry` `tax_number` | 证件是**多张、带有效期**的（执照/税务/许可证…），单列存不下第二张 |
| **`party_bank`** | `bank_name` `bank_account` `bank_address` | 一个主体**可以有多个银行账户**，且要能选默认 |
| **`party_address`** | `address` `region` `province` `city` `district` `latitude` `longitude` | 地址要分**注册/营业/收货/发货**（`biz_party_address` 已是这个形状，直接沿用） |

**分批扩列（每批可独立验收、独立回滚）**：

| 批 | 内容 | 前置 | 状态 |
|---|---|---|---|
| **1** | `party` 补到 22 列（A 组里"单值"的那 14 列）+ 建三张子表并回填 | 无（纯新增） | ✅ `V11.506.0` |
| **2** | `party_tenant` 补 B 组列（关系与商务条件） | 批 1；且**方向已定**（18 条边） | ✅ `V11.520.0`（见 §3.2d） |
| **2b** | `party_cert`/`party_bank`/`party_address` 的**双写覆盖** | 批 1；**须先给三张子表补幂等唯一键**（现在只有 IDENTITY 主键 + 普通索引，写不了 `ON CONFLICT`） | ✅ `V11.521.0`（见 §3.2e） |
| **3** | C 组 12 列**停写**（语义归 `shop_user`）+ 那 4 张会员卡导出留档 | 用户已裁定乙 | 🟡 **部分阻塞**：留档已做；12 列里**只有 4 列能停**，另 8 列被业务逻辑读着 ⇒ 见 §3.2f |
| **4** | D 组 4 列 ⇒ `party_tenant.opening_*` | **等"与财务口径对齐"**（用户 2026-09-26 裁定） | 🔴 阻塞 |
| **5** | 读路径逐表切换 + 观察期 | 批 1~4 + 批 2b；**且需 `refsurface.csv` 里待确认列定案** | ⏳ 待做 |

> 双写（`PartyServiceImpl` 覆盖 6 个写方法 + `PartyMirrorWriter`/`PartyMirrorMapper`）已随批 1/批 2 同步落地并跑绿：
> `node tools/verify-party-dual-write.cjs`（建档→镜像→改名→**双写失败不阻断建档**→B 组与方向纯度），
> 兜底为幂等对账 `node tools/sync-party-from-biz-party.cjs`。

### 3.2d 批 2 落地记录（`V11.520.0`）—— **三个口径在此定案，别在切读时重新猜**

批 2 表面是"补 16 列"，真正的工作量在三处**必须掰开才知道往哪写**的口径上：

**① 方向归位（裁定 ⑲「账期不可传递」）—— 逐列钉死方向，不许"照抄到每条边"**

| 归向 | 列 | 判据（谁说的） |
|---|---|---|
| **SALE 独有** | `credit_limit` `current_debt` `credit_days` `fixed_credit_day` | `credit_limit` = 「**我给他的赊销额度**」（§3.4.2 表定义原话，"SALE 方向才有意义"）；`current_debt` 与它同一杆秤（`PartyCreditServiceImpl.getAvailableCredit = creditLimit - currentDebt`）；`credit_days` = **应收**期限（`BusinessAccountingServiceImpl.resolveIntraTenantDueDate`：`receivableSide ? creditDays : paymentDays`） |
| **PURCHASE 独有** | `payment_days` `fixed_payment_day` | 同上，应付侧取 `payment_days`；`fixed_payment_day` 与 `payment_days` 同词根 |
| **逐边照抄** | 其余（`party_level` `settlement_type` `settlement_days` `statement_day` `settlement_day` `payment_term_type` `price_track_enabled` `default_handler_*` `promoter_*` `buyer_account` `customer_source` `roles` `category_id` `warehouse_name` `last_trade_time` `status`） | 源表**只有单值**且**当前没有任何代码按方向读它们**；照抄＝不丢信息 |

实现方式：`PartyMirrorMapper.upsertEdge` 里用 `CASE WHEN #{direction} = 'SALE' … END`
把方向独占列**钉在自己的方向上**，另一方向落 NULL；迁移回填用同一套 CASE。
机检：`V11.520.0` 自检 4.3 显式断言"应付列不出现在 SALE 边、应收列不出现在 PURCHASE 边"，
`verify-party-dual-write.cjs` ⑥ 再从**真机建档**这一侧复验一次。

**⚠️ 切读前必须定案（现在就记下来，别到时候当成"方向已定"用）**：
`settlement_days` `statement_day` `settlement_day` `payment_term_type` 这 4 列**名字上可能也隐含方向**，
但当前**没有**任何消费方按方向读 ⇒ 本批按"忠实保留"处理。
**切读之前**要么确认它们确实与方向无关，要么按 ① 的表拆开 —— 不许带着"可能串了"的状态切读。

**② `settlement_type` 的口径（本模块词表，不是协议那套五项枚举）**

源 `biz_party.settlement_type` 是 `V9.14.0` 定下的**历史两值整数**（`0` = 现结；非 0 = 有账期），
而目标列是 `VARCHAR(32)`，域模型 §3.4.2 里标注的是**文案**「现结 / 账期 / 预付 …」。
⇒ 取**本模块自己正在用的词表**（`MdCustomerController.resolveSettlementType` 类注释：
「前端传「挂账/现结」文案，库中存 1/0」）：**`0 → '现结'`、非 0 → `'挂账'`**。

⚠️ 与协议侧的 `SETTLEMENT_TYPE` 字段字典（`V11.492.0`，五项
`CASH_PREPAY`/`CASH_SPOT`/`CASH_ON_DELIVERY`/`CREDIT`/`ROLLING`，`consumer_point = AR_DUE_DATE`）
**不是同一套编码** —— `PartySettlementProfile` 类注释对此有明确警告，两边**不许混用**。
将来若统一到五项编码，这是一次**文本值重映射**（可逆，19 行级别），不是不可逆操作。

**③ 类型/窄化对不齐（双写会直接报错，且报错后果很重）**

- `party_tenant.price_track_enabled` 是 `boolean`，源是 `integer(0/1)` ⇒ 必须 `COALESCE(…,0) <> 0`；
- `party_level` 建表给的 `VARCHAR(32)` 比源 `VARCHAR(100)` **窄** ⇒ 本批放宽到 100
  （今天值最长 6 字符，但只要有人录进第 33 个字符，双写就会报错）；
- 源列宽度是**下限**：新增列一律照抄源列类型，别凭印象写小。

**④ 顺带补的洞（诚实记账）**

`upsertParty` 此前漏写 A 组的 5 列（`company_full_name` `mnemonic_code` `website` `fax`
`legal_person_phone`）⇒ 客户表单里这几项**一填就漂移**。本批已补齐
（`legal_person_phone` 在 `MdCustomerController.fromBody` 里**根本没有入参**，
所以真机验不到它，只能靠 SQL 侧对齐）。

### 3.2e 批 2b 落地记录（`V11.521.0`）—— 子表双写与**清空语义**

批 1 把 `party_cert`/`party_bank`/`party_address` 建好并回填了，但**双写一开始没覆盖它们**。
这不是小事：客户表单里 `tax_number` / `bank_name` / `bank_account` / `address` 都是可编辑项，
并存期只要有人填一次**银行账号**，`party_bank` 就永远缺这一行 —— **读路径一造就等于丢数据**
（"配了不生效 / 接了没走通"的同一类包袱）。

**① 幂等键（`V11.521.0`）—— 与批 1 回填的行形状一一对应，不许另起一套**

| 子表 | 幂等键（部分唯一索引） | 对应批 1 回填口径（`V11.506.0` §5.2~5.4） |
|---|---|---|
| `party_cert` | `(party_id, cert_type) WHERE deleted = 0` | 执照 `BUSINESS_LICENSE`、税务 `TAX` 各一条 |
| `party_bank` | `(party_id) WHERE deleted = 0 AND is_default = 1` | 源表那**唯一一个**账户 ⇒ 它天然是默认账户 |
| `party_address` | `(party_id, address_type) WHERE deleted = 0` | 源表地址 ⇒ **注册地址**（`address_type = 1`） |

三张表建时**只有 IDENTITY 主键 + 普通索引**，没有可写 `ON CONFLICT` 的键 —— 这就是为什么
批 2b 必须先补索引。非默认账户**不设唯一约束**，将来"多账户"进来不会被挡。

**② 清空语义：源列被清空时必须软删镜像行**

只 upsert 不软删的话，用户把银行账号删掉之后镜像里还留着一条旧账户 ——
而这类"**多出来的**"漂移**对账脚本看不见**（它只比对"缺"）。
⇒ `PartyMirrorWriter.mirrorSubTables` 对四类行都是「有值 ⇒ upsert / 空值 ⇒ 软删」。

**②b 删除路径：删主体时子表必须一并软删（本批自己漏过一次，已补）**

批 2b 第一版**只加了"写"、忘了"删"** ⇒ 删掉主体后它的证件/银行/地址行还挂着
`deleted = 0`、指向一个已软删的主体。⚠️ 这个洞的暴露时机很晚：`V11.520.0`/`V11.521.0`
的迁移自检里恰好有"子表不悬空"这条断言，所以它会在**下一次跑迁移**时才炸出来
（那时已经隔了一版，难定位）。⇒ `onDelete` 现在一并软删 边 + 两张证件 + 默认银行 + 注册地址 + 主档；
验收脚本 ⑧ 组真机验一遍（删之前**先断言五类行本来都在**，否则"删后都是 0"可能只是从来没建过 ——
这是个很容易写出的假绿）。

**③ 已知限制（明写出来，别当成"已覆盖"）**

- `latitude` / `longitude` **不写**：这两列 `Party` 实体根本没映射、全仓也没有消费方
  （实测 21 行里仅 1 行有值，且非本模块写入）。想在双写里带上它们，得先给实体补字段；
  在那之前"不写"比"写个死值"诚实。批 1 回填按源表搬过一次，故存量行可能有值。
- `business_license`（执照号）在 `MdCustomerController.fromBody` 里**没有入参**
  ⇒ 真机上建不出 `BUSINESS_LICENSE` 行，验收脚本只验了 `TAX` 那一条。
- 三张子表**没有指向 `party` 的外键** ⇒ 验收入口的自清理必须显式删它们
  （`verify-party-dual-write.cjs` 的 `cleanup()` 已覆盖），否则探针会以"子表孤儿"留下来。

**④ 对账脚本同步重写（`tools/sync-party-from-biz-party.cjs`）**

它此前**已经过期到会帮倒忙**：补边时**不写 B 组列** ⇒ 补出来的是一条"只有 id 和方向"的
残缺边，而且因为边已存在，之后再也没人会去刷它。重写后它与双写逐字同口径：
- 方向改成**可并存**（`roles` 含 CUSTOMER ⇒ SALE、含 SUPPLIER 或 `party_type=2` ⇒ PURCHASE）。
  ⚠️ 建表回填用的是 `CASE ... THEN 'SALE' ELSE 'PURCHASE'` 的**单边**口径 ⇒
  "既是客户又是供应商"的行一直**缺一条边**，只有按新口径才能补出来；
- 商务条件按方向归属（⑲）；子表四类行也一起补；
- 差额快照从 4 个数扩到 8 个（新增 `边缺商务条件`、`子表缺行`、`主档无租户`、`多余贸易边`）；
- ⚠️ 没有 `tenant_id` 的主体**建不了边**（边必须有 tenant_id），双写那边也是跳过 + WARN
  ⇒ 对账口径同样排除，否则会报一个**永远补不上**的假差额。
- ⚠️ **必须同时数"多"、不能只数"缺"**（本轮补上）：脚本原本只找"缺贸易边"，
  于是"角色变了但旧方向的边没被软删"这类**多余边**它一辈子也发现不了。
  现在多了一项 `多余贸易边`（判据与 `directionsOf` 一致：SALE 要 roles 含 CUSTOMER、
  PURCHASE 要 roles 含 SUPPLIER 或 `party_type = 2`），并在补齐阶段一并软删；
  已用"故意插一条多余边 → dry-run 报 1 → 真跑后归 0"自测过。

> **本轮反复撞到的一类错，值得单独记住：镜像的"写"与"删"必须成对。**
> 一天之内在**同一批代码**上踩了两次：① 子表只加了 upsert、没加"删主体时一并软删"；
> ② 边只 upsert 了"适用方向"、没清理"不再适用的方向"。
> 两次的共性：**失败是静默的**，而且**对账脚本原先只数"缺"、看不见"多"** ⇒ 没有任何东西会报警。
> ⇒ 以后给镜像加一类新数据时，先问三句：**建/改怎么办？删怎么办？变得不再适用怎么办？**

### 3.2f 批 3（C 组会员列）**不能按原写法做** —— 逐列判定与前置（2026-09-27 实测）

原计划写的是「C 组 12 列**停写**」。**逐列查过之后，只有 4 列真能停。**
剩下的 8 列都有业务逻辑在读 `biz_party` 这一列（不是"少个展示字段"，是会算错数）。

**入口（写）只有 4 条**：
① `MdCustomerController.fromBody`（`MdCustomerController.java:1535-1590`，
被 `POST /erp/md/customer` 建档 / `PUT /{id}` 更新 / `POST /import` 三条路复用）；
② `RetailOrderServiceImpl.java:674`（零售收银完单写 `points`）；
③ `MemberLevelRuleController.java:81`（会员等级批处理 `UPDATE biz_party SET member_level=?`）；
④ `PartyController.java:117-132`（通用 CRUD 直接收 `@RequestBody Party` 落库 —— ⚠️ **只删 `fromBody` 的 set 并不彻底**，这条也在写）。

| 列 | 谁在读（`biz_party` 这一列） | 停写会怎样 | 能不能停 |
|---|---|---|---|
| `points` | 零售 `RetailOrderServiceImpl:663`（**余额校验 + 扣减**）、等级升级门槛 `MemberLevelRuleController:137`、商城 `MallAuthServiceImpl:471/489`、筛选 `PartyMapper.xml:186` | **积分闭环直接拆掉**（余额永远算成旧值） | ❌ **必须先迁权威值** |
| `member_level` | 升降级判定 `MemberLevelRuleController:126/137`、促销定向 `PromoRefQueryMapper:41`、`PartyMapper.xml:170` | 自动升降级永远算旧值 | ❌ 需先切读 |
| `member_card_no` | 会员列表 `PartyMapper.xml:142`、营销触发、商城、等级批处理 | "谁是会员"判定失效，会员列表/营销定向一起空 | ❌ 需先有目标表 |
| `member_name` | 同 `member_card_no` | 同上 | ❌ 需先有目标表 |
| `member_total_consume` | 消费额升级门槛 | 门槛失效（且此列**当前已无人写入**，只有表单能填） | ❌ 需先切读 |
| `member_valid_end` | 卡到期营销 `selectCardExpiring` | 到期营销无候选 | ❌ 需先切读 |
| `birthday` | 生日营销 `selectBirthday` | 生日营销无候选 | ❌ 需先切读 |
| `customer_one_pass` | 预售报表 `PreOrderAnalysisReportServiceImpl:205` | 报表该列变空 | ⚠️ 仅报表，可随前端一起停 |
| `member_valid_start` | 仅 `PartyMapper.xml:199` 排序 | 展示/排序变空 | ✅ 可停 |
| `member_card_status` | 仅展示 / 排序 | 展示变空 | ✅ 可停 |
| `member_initial_points` | 仅展示 | 展示变空 | ✅ 可停 |
| `member_issue_time` | 仅展示 | 展示变空 | ✅ 可停 |

**⚠️ 前置条件（做完才能动这 8 列）**：
1. `shop_user` **接不了**：它没有卡号/积分/生日/等级字段，且当时 **0 行**（"自然人也还没有"）。
2. 积分台账 `mkt_points_batch` / `mkt_points_journal`、卡积分 `erp_loyalty_card.points` 都存在，
   **但零售余额的权威值仍是 `biz_party.points`** ⇒ 要么把权威值迁到台账汇总并切读，
   要么明确"零售余额 = 台账汇总"再停写。**这一步没做完就停写 `points` = 拆功能。**
3. 前端 `md/customer/components/MemberCardModal.vue` 仍会提交这些字段
   ⇒ 只改后端会出现"保存成功但回显丢失"，**前后端必须同批改**。

**✅ 本轮已做的部分**：那 4 张 `member_card_no` 的行已**导出留档**
（`docs/archive/party-member-cards-20260927.csv`，附 `docs/archive/README.md` 说明为什么留、
以及"别拿它的 `points` 当台账"）—— 按纪律**导出留档 + 列弃用**，不给它们编一个自然人。





| 方案 | 做法 | 可逆性 | 评价 |
|---|---|---|---|
| A · 视图投影 | 建 `party` **视图**从 `biz_party` 投影（`biz_party.id` 即 `party.id`） | 高 | 改动最小，但**解不了病根**（`biz_party.tenant_id` 还在，同一真单位仍是两条记录）⇒ 只能作为**过渡**，不能作为终态 |
| **B · 新增表 + 双写 + 读走视图**（推荐） | 新建 `party`/`party_tenant` 物理表；写路径**同时**写新旧；读路径**先走视图**（视图 = 新旧 UNION，语义与旧一致） | 高 | 唯一能"边跑边切、随时退回"的路子；代价是**并存期内两份数据要一致**，必须有**一致性巡检脚本** |
| C · 直接改表（改名/加列/改引用） | 一次性改到目标形态 | **无** | ❌ 不可回滚，100 张表的读路径必须同一次发布全改完，风险不可接受 |

**方案 B 的四个阶段（每阶段独立可验收、独立可回滚）**：
1. **建表 + 装闸门**：建 `party`/`party_tenant` 空表；装 `unified_code` 唯一约束、一照一店闸门（⑯/⑥）；
   **不动任何读路径**。
   ✅ **2026-09-23 已执行第一批**（`V11.497.0`）：两张表 + 两个唯一索引
   （`uk_party_unified_code` / `uk_party_tenant_edge`）+ **一照一档回填 19 行**（`party.tenant_id` 恒 0）
   + **18 条贸易边**（16 SALE + 2 PURCHASE）；⚠️ **方向不猜** —— `party_type=3`（承运商＝第三方服务主体）
   那 1 行**只进 party、不建边**，并在自检里显式断言"它在 party 里但不在 party_tenant 里"以证明是"不猜"而非漏搬。
   同批把 **`party` 登记进 `IGNORE_TENANT_TABLES`**（共享层，否则租户会话被注入 `tenant_id=1` ⇒ **一行都读不到**，
   与 `shop_user` 同一形态）；`party_tenant` **刻意不登记**（它的 tenant_id 就是档案所属租户，正是要按会话过滤）。
   迁移在事务内跑通后回滚验证（DDL + 回填 + 5 条自检全过、无残留），**待下次重启由 Flyway 正式应用**。
2. **双写 + 回填**：写路径增写新表；用一次性迁移把存量 19 行搬过去（见 §3.4）；
   同时上**一致性巡检脚本**（逐行比对新旧，差异必须为 0 才允许进下一步）。
3. **读路径逐个切换**：按 §3.2 的清单**一表一提交**地切到视图；每切一处跑一次该模块的既有 E2E。
4. **观察期后收口**：确认无回退需求，才删旧列/旧表（**单独一次发布**，见 §5）。

### 3.4 存量归并：**当前不可执行**（本方案要请你重新裁定的一处）

**裁定②**（§6.1）说：存量是"开发模拟数据"，**不需人工确认，自动归并即可**。
**但实测把这个前提推翻了**：

- 自动归并的**唯一合法依据**是**统一社会信用代码**（裁定④：手机号/邮箱/**名称一律不作依据**）；
- 而 **`unified_code` 152 行全为空** —— 一个可用的键都没有；
- ⇒ 剩下的"依据"只有**名称**（142 个不同值），而按名称归并**正是裁定④明令禁止的**
  （同名不同主体会把两家公司的账串到一起，**不可逆**）。

**三条出路（请选一条，我按选择改写 §5.3）**：

| 选项 | 内容 | 代价 | 我的意见 |
|---|---|---|---|
| **I** | **先不归并**：`party` 一照一档地搬过去（19 行 → 19 档），`party_tenant` 记它们与租户的关系；**归并留到有信用代码时再做**（`merge_request` 流程） | 同一个真单位在 E/F 两店**仍然**是两条 `party`（病根未除） | ✅ **推荐**：不猜、不串账；病根由"R2 有向边 + merge_request"逐步消除，而不是靠一次危险的批量合并 |
| II | **人工补录**：把 19 行的信用代码补齐后再自动归并 | 需要业务方提供 19 个单位的信用代码（这是**外部事实**，系统里没有） | 可行的后继步骤；但**先有 I，再有 II** |
| III | 按名称归并 | —— | ❌ 违反裁定④，**不建议** |

**✅ 2026-09-23 裁定与执行：用户选 I，并明确「当前数据是平台测试用的模拟数据，直接把信用代码模拟上」** ——
已执行 `tools/seed-mock-unified-code.cjs`：给 `biz_party` **19 行未删行**补**模拟**统一社会信用代码
（回查：19/19 有值、19 个不同值、全表无重复）。

**模拟代码的约定（三条，别当真实数据用）**：
1. **格式合法**：按 GB 32100-2015 生成 18 位，**字符集与校验位都算对**
   （否则将来加 CHECK 约束或写校验器时，这批模拟数据会第一个把它顶红）；
2. **一眼可辨**：前 12 位固定 `91999999FAKE`
   （`91` 工商企业 · `999999` **不存在的行政区划** · `FAKE` 明示假数据），后 5 位序号；
3. **幂等 + 可回退**：只填"当前为空"的行；`--revert` 按前缀整批置回 NULL。**只动 `unified_code` 一列**。

> ⚠️ 因为每行拿到的是**不同**的代码，所以**这一步不产生任何归并**（这正是选项 I 的形态：
> 一照一档）。将来真出现"同一主体两套档案"时，走 §3.4 的 `merge_request` 流程，而不是回头批量猜。

> **无论选哪条**：归并动作**必须可逆** ⇒ 建 `party_migration_log`（记 `旧id / 旧tenant_id / 新party_id / 依据 / 时间 / 操作人`），
> 这是"能不能退回去"的**唯一**保证。归并前先备份 `biz_party` 与全部引用列的快照。

### 3.5 先装的闸门（在搬数据**之前**）

- `party.unified_code` **唯一约束**（裁定②的连带：现状只有 id 唯一，`unified_code` 无约束）；
- **一照一店**闸门（⑥）：同一信用代码默认 1 个租户，第 2 店需"法人同意 + 平台审核"；
- `person_party` 的**授权范围/限额**列位（G13，阶段 3 转正 R1 时一并留位，实施在阶段 3 之后）。

### 3.6 回滚设计（阶段 3）

| 步骤 | 怎么退 |
|---|---|
| 建表 / 装闸门 | 表与约束都是**新增**，退 = 不启用（读路径未变） |
| 双写 | 退 = 关掉双写开关（新表变垃圾数据，无影响） |
| 回填 | 退 = 按 `party_migration_log` **反向**搬运；**所以必须先写日志再搬数据** |
| 切读 | 退 = 切回旧路径（视图仍在，语义一致）；**一表一提交**就是为了能单表回退 |
| 删旧列/旧表 | ⚠️ **不可逆**，见 §五 |

---

## 三、阶段 5：单据四主体 + 角色

### 3.1 目标形态（引用 §3.4.3 / §7.7 G12）

- 单据主表加：`seller_party_id` / `buyer_party_id` / `person_id`（自然人）/ `acting_party_id`（代谁）；
- **从属表 `trade_party_role`**（单据 × 角色 × 主体，多行）**为主**；
  另**只冗余"收货方/开票方"两列**到主表（便于查询与索引）；
- 角色**不止四个**（报关方、保险受益人、承运委托方…）⇒ 所以"为主"的是从属表，不是固定列。

### 3.2 迁移顺序（**先加列 → 双写 → 回填 → 切读 → 观察期 → 停写旧列**）

1. **加列**（可空）：64 张单据表加 `seller_party_id`/`buyer_party_id`（按 §3.2 的清单逐张确认它到底是买方还是卖方角色）；
2. **建 `trade_party_role`** + 收货/开票两列；
3. **双写**：新单写入时同时写新列/从属表；
4. **回填**：存量单据从 `customer_id`/`supplier_id` 映射（**只做映射，不猜主体**）；
5. **切读**：逐模块切换；每切一处跑该模块 E2E；
6. **观察期后停写旧列**（旧列**不删**，先停止写入；删除属不可逆操作）。

### 3.3 与**快照规范**（§11.6）配套 —— 这是本阶段最容易漏的一件事

**加 `seller_party_id`/`buyer_party_id` 的同一次迁移里，必须同时加它们的快照列**
（`seller_party_name` / `buyer_party_name` / `tax_no` / 地址联系人…，清单见 §11.6.2）。
否则就是 §11.6 要防的"晚做要全表回填"——**这次回填的是 64 张表**。

### 3.4 回滚设计（阶段 5）

同样是"只加不删 + 双写"：新列可空 ⇒ 读路径不切就完全无影响；
退 = 停止双写、继续用旧列。**唯一的不可逆点同样是"删旧列"**。

---

## 四、验收与门禁（两阶段共用）

1. **两向断言**（本仓纪律）：放行路径（新路径生效）与拒绝路径（越权/校验失败）**都要测**；
2. **守恒断言**：迁移前后 —— `biz_party` 未删行数守恒（19）、单据数守恒、金额合计守恒、
   **每张单据都能查回其主体**（不允许出现"迁移后主体为空"的单据）；
3. **一致性巡检**（并存期每次发布前跑）：新旧表逐行比对，差异必须为 0；
4. **门禁**：装配门禁（`ComponentScanCoverageTest`）与快照审计（`audit-snapshot-spec.cjs`）
   必须在每步之后仍是绿的 —— 它们的棘轮基线**只许缩，不许涨**。

---

## 五、不可逆操作清单（**必须单独一次发布，且必须等观察期结束**）

| 操作 | 阶段 | 为什么不可逆 | 前置条件 |
|---|---|---|---|
| `DROP COLUMN biz_party.tenant_id` | 3 | 旧数据被删，无回退依据 | 读路径全部切换 + 观察期内无回退 + 已备份 |
| `DROP TABLE biz_party`（或改名） | 3 | 同上 | 同上 |
| `DROP COLUMN` 各单据的 `customer_id`/`supplier_id` | 5 | 同上 | 同上，且 64 张表**逐张**确认无读方 |
| 物理删除归并掉的重复 `party` 行 | 3 | 串账证据消失 | 已按 §3.4 选 I/II 且 `party_migration_log` 完整 |

> **原则**：本方案里**所有"删除类"操作都排在最后、单独发布**；
> 在此之前，每一步都只做"新增 + 双写 + 切读"，**随时可以退**。

---

## 六、风险与开工顺序（不含时间估计）

| 序 | 步骤 | 依赖 | 风险 |
|---|---|---|---|
| 0 | **引用面判定**（出 `refsurface.csv`） | 无 | 低；**但它是后面一切的前提**，跳过必返工 |
| 1 | 3.4 的**出路裁定**（I/II/III） | 你拍板 | —— |
| 2 | 建 `party`/`party_tenant` + 装闸门 | 序 0/1 | 低（纯新增） |
| 3 | 双写 + 回填 + 一致性巡检 | 序 2 | 中（两份数据要一致） |
| 4 | 读路径逐表切换 | 序 3 全绿 | 🔴 高（100 张表，**一表一提交**） |
| 5 | 阶段 5 加列（**含快照列**）+ 双写 | 序 4 | 中高（64 张表） |
| 6 | 删旧列/旧表（单独发布） | 观察期 | 🔴 不可逆 |

**并行提示**：阶段 5 的"加列 + 快照列"与**阶段 2 的快照规范**是同一次迁移，**别拆成两次**。

---

## 七、变更记录

| 日期 | 版本 | 内容 |
|---|---|---|
| 2026-09-23 | v1.1 | **补「首轮实测结论」**（`tools/refsurface.cjs` → `tools/refsurface.csv`，候选 111 处）：数据侧确认指向 `biz_party` **14 处**；**3 处同时命中 `biz_party`/`sys_user` ⇒ 数据侧不可区分**（本脚本第一版误判为"指向 biz_party"，已修正）；**2 处实际指向 `biz_party_contact`（联系人）**，证明"`partner_id` ≠ 往来单位"；🔴 **`erp_purchase_inbound.supplier_id` 命中 `biz_party` = 0**，与代码注释矛盾，且**用这批数据验证不了应付侧的三层结算口径** ⇒ 三证里"代码证据"是决定性的一证 |
| 2026-09-23 | v1 | 初稿（D1(c) 产出）：① 前置事实实测（**未删仅 19 行**、**`unified_code` 全空**、引用面 **100 张表**/64 张含客户供应商列）；② 阶段 3 第一步定为**引用面判定**而非建表；③ 并存期三方案取 **B（新增+双写+视图）**；④ **指出"自动归并"当前不可执行**（唯一合法识别键全空），给三条出路并推荐 **I（先不归并）**；⑤ 阶段 5 强调**与快照列同批加**；⑥ 不可逆操作清单与"只加不删 + 双写"的回滚设计 |
| 2026-09-23 | v1.2 | **订正 v1.1 的警告 1**：`erp_purchase_inbound.supplier_id` 命中 `biz_party`=0 **已查清为"数据不可测"而非代码缺陷** —— 代码 `setSupplierId(order.getSupplierId())` 语义正确，实测值是 E2E 造数 id（`2099000000000000901`/`100`）且无名称快照 ⇒ dev 库的采购单据**用假 id 造**，不对应真实 `biz_party` 行。**通用教训：数据侧 0 命中 ≠ 代码错，先看值形态（造数假 id / 小整数）再下结论**；连带记下"应付侧档案口径那条链路用现有数据验不了，需先造真实供应商主体" |
| 2026-09-23 | v1.3 | **用户裁定选 I + 授权模拟信用代码**：执行 `tools/seed-mock-unified-code.cjs`，给 19 行未删 `biz_party` 补**格式合法且一眼可辨**的模拟统一社会信用代码（前缀 `91999999FAKE`，GB 32100 校验位算对），幂等且可 `--revert` 还原；**只动 `unified_code` 一列**。因每行代码不同 ⇒ **本步不产生归并**（选项 I 的形态：一照一档），真出现重复主体时走 `merge_request` 流程 |
| 2026-09-23 | v1.4 | **序 2 第一批执行**：`V11.497.0` 建 `party`/`party_tenant` + 两个唯一索引 + 一照一档回填（19 行 → 19 档 + 18 条边：16 SALE / 2 PURCHASE）；**方向不猜**（承运商 1 行不建边，自检显式断言其存在性以证明"不猜"）；`party` 进 `IGNORE_TENANT_TABLES`、`party_tenant` 不进（层级不同）。迁移事务内跑通后回滚验证，待重启应用 |
| 2026-09-23 | v1.5 | **`refsurface.csv` 的代码证据已填**：14 处"数据侧确认指向 biz_party"里**只有 7 处有铁证**；另 7 处 = **1 处注释与数据打架**（`erp_loyalty_coupon.partner_id`）+ **1 处多态引用**（`erp_capital_flow.party_id` 紧邻 `partyType`）+ **5 处无注释/样本太少/来源未追清** ⇒ 读路径切换前必须人工确认这 7 处 |
| 2026-09-23 | v1.6 | **补 §3.2b 逐列归位**：实测 `biz_party` **76 列** vs 新表只承接 34 列 ⇒ 读路径切换**不能开始**；给出 A/B/C/D/E 五组去向（A 主体主档 27 · B 关系商务条件 24+2 · C 会员属性 12 · D 期初余额 4 · E 技术列 7），并点出**两处待拍板**（会员属性归属、期初余额归属）与**一条硬约束**（A 组落地后 `party` ~41 列超"主表 ≤25"规范 ⇒ 扩列前必须先拆证件/银行/地址子表，否则等于把上帝表原样搬家）|
| 2026-09-26 | v1.7 | **C/D 裁定落地 + A 组子表方案**：C 选**乙**（会员=自然人属性并入 `shop_user`）——实测语义面极小（`MEMBER` 只 1 行）但**那 4 张会员卡无处迁移**（自然人侧 `shop_user` 0 行）⇒ **导出留档 + 列弃用，不假装迁移**；散客 `id=1` 被 3 张出库单引用 ⇒ **仍是 party**，只是不再带 MEMBER 属性。D 裁定**随关系**但**排在与财务口径对齐之后**（暂缓）。新增 §3.2c：A 组**先拆 `party_cert`/`party_bank`/`party_address` 三张子表再扩列**（否则 `party` 41 列超规范＝把上帝表搬家），拆完 `party` 落 22 列；并给出**五批分批扩列**（1 扩主档+子表 · 2 补 B 组 · 3 C 组停写+卡导出 · 4 D 组待财务 · 5 双写与切读）|
