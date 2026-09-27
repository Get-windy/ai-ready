# 财务模块全面审计报告（2026-09-23）

**范围**
- 前端：`frontend/apps/pc-admin/src/views/finance/**`（45 个页面子目录、52 个 `.vue`）+ `views/budget/**`、`views/fixed-asset/**`、`src/api/finance/**`、`src/api/budget.ts`、`src/api/analytics-finance.ts`
- 后端：`backend/erp/erp-finance`（48 控制器 / 405 端点）、`backend/erp/erp-budget`（7 控制器 / 52 端点）
- 数据库：`devdb` 财务域 49 张表（PostgreSQL 18）
- 对照基准：`docs/Yh-Spec/手动整理对标开发文档/财务模块/*.md`（**26 篇**）、`系统菜单设计与管理/*.md`、`MASTER_TODO_20260920.md` §3.4

**维度**：可用性 · 规范性 · 冗余 · 死代码 · 功能接通 · 跨模块关系 · 权限配置
**口径**：结论分三级证据 —— **【实测】**（本次真机跑过，附命令与输出）· **【读码】**（我逐一打开文件核对过行号）· **【清点】**（机械扫描，标注方法，未逐条复核）。所有数字给出可复现来源。

> ⚠️ 本仓反复踩过的坑，本次已规避并写进方法：
> ① 未注册路由被 catch-all 承接，**URL 不变但渲染 404 页** —— 故判定"路由存在"必须四重检查（URL + `.not-found` + 正文文案 + 主内容渲染）；
> ② 判断"无引用"不能只搜表名/类名，跨包 `Service` 引用会漏 —— 死代码结论一律标注置信度；
> ③ 子代理的"可删"结论**不直接采信**（见 §7.2）。

---

## 〇、摘要

| 级别 | 数量 | 代表问题 |
|---|---|---|
| **P0 致命** | 5 类 | ~~总账≠凭证分录（利润表/资产负债表数字失真）~~ **已重算+自愈 ✅** · ~~记账失败静默吞异常~~ **已改为失败即回滚 ✅** · ~~关账红线在非 1 租户失效~~ · ~~账户余额/税负/审批人硬编码假数据~~（部分） · ~~预算台账可被直接改写~~ **已修 ✅** |
| **P1 严重** | 11 类 | 17 条菜单被普通租户整批过滤 · ~~工作台 4/6 入口 404~~ · ~~辅助核算开关 404~~ · ~~收款单批量打印 405~~ · 9 个空目录 + 7 个死页面 · 60 个死 API · 201/217 权限码仅超管持有 · 已取消单据应收仍挂账 · **经营看板的 5 个财务指标是随机数** · DMS→财务事件通道悬空 |
| **本轮已修复** | 3 条断链 + 36 处租户硬编码 + 凭证红冲 4 项 + 5 处尾斜杠端点 + 3 处硬编码假数据 + 总账重算（含自愈） + 预算台账防篡改 + 记账失败回滚（16 处） + **经营看板财务指标真实取数** + 死页面清理 | 见下方「本轮修复记录」（一~九） |
| **P2 一般** | 40+ 条 | 路径命名分裂 · 金额用 double · 全表内存聚合 · 字段全空 · 死表 |
| **确认无问题的项** | 9 项 | 48/48 入口可打开 · 457 端点 100% 有鉴权注解 · 菜单组件 0 缺失 · 权限码 100% 在库且已关联角色 · 编译通过 |

### 本轮修复记录（2026-09-23，3 条实测断链，均已验证）

| # | 问题 | 改动 | 验证结果 |
|---|---|---|---|
| 1 | 财务总览工作台 4/6 快速入口 404 | `views/finance/index.vue` 跳转表改为真实路径（`/md/accounting-subject`、`/finance/voucher/index`、`/finance/balance-report`、`/finance/reconciliation`），并补注释说明"路由由菜单 path 生成、财务前缀是 `finance/**`" | `node tools/verify-finance-dashboard-links.cjs` → **修复前 2/6 OK，修复后 6/6 OK** |
| 2 | 辅助核算「启用」开关 404 | 后端补 `PUT /erp/finance/auxiliary/{type\|item}/{id}/enable`（专用 `enable()` 只改 enabled，不走 `update` 的全字段校验；权限码复用已有的 `finance:auxiliary:update`） | 实测 `PUT /type/2/enable?enabled=false` → HTTP 200，库中 `enabled` **真实 t→f**，还原后 t；item 端点返回「项目不存在」（证明端点已接通） |
| 3 | 收款单「批量打印」405 | 后端补 `POST /erp/receipt/batch-print`，与付款单 `PaymentController#batchPrint` **完全对称**（注释已写明：两侧当前都只回执请求、不做模板渲染，真打印需接 `PrintDialog`，属独立待办） | 实测 HTTP 200，返回 `{success,count,template}`；对照付款单同为 200 |

**回归**：`node tools/e2e-finance-audit.cjs` → **48/48 入口正常、0 失败请求、0 console 错误**（修复前后一致，无回归）。
**构建**：`./mvnw -o -pl erp/erp-finance install` + `core-api package` → BUILD SUCCESS。

> ⚠️ 说明：第 3 条只消除了"前后端契约不一致"（405），**并未让批量打印真正输出纸张** —— 付款单同样如此。真正的打印能力缺失已单列为待办（见 §4.5），不在"修断链"范围内。

### 本轮修复记录之九：经营看板财务指标改为真实取数 + 死页面清理（2026-09-27）

**（一）§3.11 财务指标不再随机**

`MetricCollectorServiceImpl.collectFinanceMetrics()` 原先是 5 个 `generateRandomValue(...)`，
而 `MetricCollectorConfig` **每 5 分钟**采集一次（`@Profile("!local")`）⇒ `erp_business_metric`
已积累 **287 万行**、其中 finance 指标全是随机数（实测值域与 `generateRandomValue` 参数区间完全吻合：
`today_revenue=43321∈[30000,60000]`、`gross_margin=25∈[20,40]`…）——**经营看板上的财务数字与账务无关**。

改为真实取数（`erp-observability` 只依赖 core-base，故用同模块既有的 `JdbcTemplate` 直查财务表）：

| 指标 | 取数口径 |
|---|---|
| 今日收入 | 凭证日期为当日的**已过账**凭证中，收入类科目的贷方净额 |
| 今日支出 | 同上，费用类科目的借方净额 |
| 应收账款 | `finance_receivable.remaining_amount` 合计（排除 cancelled） |
| 应付账款 | `finance_payable.remaining_amount` 合计 |
| 毛利率 | 本年累计 (收入 − 成本) ÷ 收入 × 100 |

科目分类**按属性判定**、不硬编码编码前缀：收入类 = `subject_type=5 AND direction=2`、
费用类 = `subject_type=5 AND direction=1`、成本类 = `subject_type=4`
（实测分布：收入 14 个 / 费用 12 个 / 成本 1 个，107 个科目全部有 `direction`）。
各指标取数失败降级为 0 并记日志，不阻断其它指标。

**验证**：`node tools/verify-finance-metrics-real.cjs` → **5/5 通过**（独立用 SQL 按同口径算期望值再比对）。
修复前后对照（同一张表）：

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 应收 | 258913（随机） | **1200** |
| 应付 | 96705（随机） | **765** |
| 今日收入 / 支出 | 43321 / 28211（随机） | **0 / 0** |
| 毛利率 | 25（随机） | **100** |

> ⚠️ **同类问题不止财务**：同一实现里的 ORDER / INVENTORY / USER / SALES 指标**也全是 `generateRandomValue`**
> （表内 287 万行绝大多数是它们）。本次只改财务 5 个（属本审计范围），
> **其余类型的假指标登记为各模块的审计项**。

**（二）死页面与空目录清理**

- 删除 9 个空目录（`capital-flow`/`expense-apply`/`expense-pay`/`expense-reimburse`/
  `pre-receipt`/`report`/`subject`/`subsidiary-balance`/`trial-balance`）——复核均为 0 文件，
  且 git 本就未跟踪空目录。
- 删除 `views/finance/reports/`（3 个孤儿组件 689 行）——全 src 零引用，已有等价新页面。

**未删（需产品决策）**：`views/finance/receipt|payment|pre-payment/index.vue`（各 1000+ 行）
与 `voucher/VoucherDetail.vue` —— 三者**不可达但功能完整**（收款单/付款单列表+核销），
删除会丢失功能，需决定「接线挂菜单」还是「删除」。

### 本轮修复记录之八：§2.2 记账失败静默吞异常 → 改为「失败即整体回滚」（2026-09-26）

**先做了业界对标调研**（见下），结论分两种情况，而本项目正落在有明确答案的那一派。

**调研结论**：

| 场景 | 业界做法 | 代表 |
|---|---|---|
| **同库同事务**（本项目属此列） | **强一致：失败整单回滚** | **SAP FI/MM**：物料凭证与会计凭证在同一 **LUW** 内过账，FI 失败则整体回滚；SAP Note 743744 特别警告自定义的额外 `COMMIT WORK` 破坏原子性。**Oracle SLA**：Final Mode 下 Journal Import 失败「Data will be rolled back」。**金蝶**：「已生成凭证的单据不允许反审核」 |
| 跨系统 / 异步 | 允许中间态，但必须**显式失败 + 可重试 + 有监控** | 用友 U8C（标志位 + 人工重算）、Namassoft（请求队列 `Failed` 状态 + 手动 Reprocess） |
| **不可接受** | **`catch { log }` 静默吞掉** | 调研中**没有任何一家**这么做 |

**本轮改动**（6 个文件 16 处，均为「单据动作 + 记账」链路）：

| 文件 | 处数 |
|---|---|
| `BusinessAccountingServiceImpl`（业财直调网关） | 1 |
| `PaymentBusinessIntegrationService` | 8 |
| `PreReceiptServiceImpl` | 2 |
| `PrePaymentServiceImpl` | 2 |
| `OffsetServiceImpl` | 2 |
| `OtherIncomeDocServiceImpl` | 1 |

改法统一：`catch (Exception e) { log.warn(...); }` → `log.error(...) + throw BusinessException.badRequest("记账失败，操作已回滚: ...")`。
这些方法本身都有 `@Transactional(rollbackFor = Exception.class)`，且调用方（销售/采购/费用/固资）在本调用**之外未做 catch** ⇒ 异常会传播并回滚其单据事务（这正是 SAP 同一 LUW 的语义）。

**未改（已登记，非「记账失败」）**：
- `CapitalFlowServiceImpl:314-324`（支付方式解析失败兜底为「银行转账」）—— 数据解析问题，不该阻断支付
- `AnalyticsSupport:50-54`（报表取数把非数字当 0）—— 报表兜底
- `ExpenseServiceImpl:270`（位于**待废弃的旧 `erp/expense` 包**，见 FIN-DUP-01）
- `InvoiceMatchingServiceImpl:455`（属**无入口的死岛**，见 §3.10；待接线时一并处理）

**验证**：`node tools/verify-accounting-failure-rollback.cjs` → **8/8 通过**
手法是用「关闭会计期间」构造必然失败的记账场景，然后断言：

| 断言 | 结果 |
|---|---|
| 接口返回失败（不再静默成功） | ✅ `400 记账失败，操作已回滚: 会计期间已关闭...` |
| **单据状态未被推进为「已记账」** | ✅ status `0 → 0`（**回滚生效的直接证据**） |
| 未产生任何凭证 | ✅ 该期凭证数 29 → 29 |
| **对照组**：恢复期间后再 confirm | ✅ 成功且状态推进 `0 → 1`（排除「接口被别的原因拦掉」） |

### 本轮修复记录之七：§2.5 预算台账防篡改（2026-09-26）

`BudgetControlServiceImpl` 类注释明示 P0 口径：

> 预算科目三列为唯一事实来源，恒等式必须成立 —— `remaining_amount = budget_amount − used_amount − frozen_amount`；
> **禁止任何页面/接口直接改写已执行、冻结或剩余额**。

而 `BudgetItemServiceImpl.update` 用 `BeanUtil.copyProperties` **全量覆盖** ⇒ 请求体只要带上这几个字段就能直接篡改台账，绕过全部预算控制。本轮修两处：

| # | 问题 | 修法 |
|---|---|---|
| ① | `update` 可改写 `usedAmount/frozenAmount/remainingAmount/executionRate` | 四列从拷贝中排除；预算额若调整，按恒等式重算 remaining 与 executionRate（口径对齐 `applyItemAmounts`） |
| ② | `freezeAmount`/`releaseFrozenAmount`/`consumeBudget` 三个入口**无 `amount > 0` 校验** | 三处均加正数校验 —— 原先传负数可把「冻结」变「解冻」、「消耗」变「回冲」 |

**修复过程中发现并修掉的一个副作用**：Hutool 的 `copyProperties` **默认连 null 一起覆盖**，所以排除字段后仍会把未传的 `budgetAmount` 清成 null，使恒等式算出 0（验证时实测：只传 `usedAmount` 时 remaining 变成 0）。改用 `CopyOptions.setIgnoreNullValue(true)` 建立「忽略 null 的部分更新」语义。

**验证**：`node tools/verify-budget-ledger-protection.cjs` → **13/13 通过**
（① 请求体带 usedAmount=99999 时三列均纹丝不动；② 改 budgetAmount=30000 后 remaining 随之为 30000；
③ 冻结/释放/消耗传 -100 均被 400 拒绝，正数对照正常；测试数据已恢复）。

### 本轮修复记录之六：`postToLedger` 改为「按凭证重算」——总账自愈（2026-09-26）

**病根**：`LedgerServiceImpl.postToLedger()` 原为**纯累加式**（`periodDebit += 本次借额`），
凭证删改或过账被重复触发后总账只增不减。之五只是**订正了数据**，病根未除。

**改法**：把累加改为「按 (科目, 期间) 重算」——
期初 = 该期之前所有期的已过账分录净额累计；本期发生 = 该期已过账分录合计；期末 = 期初 + 借 − 贷。
口径与 `tools/recalc-finance-ledger.sql` 完全一致。

**效果（`tools/verify-post-to-ledger-idempotent.cjs` → 10/10 通过）**：

| 断言 | 结果 |
|---|---|
| 正常过账后总账 = 凭证分录合计 | ✅ 1090 = 1090 |
| **重复过账不翻倍**（把状态回退为 audited 后再过账一次） | ✅ 1090 → 1090 |
| **人为制造的漂移被自愈**（把借方改成 11089，再做一次该科目该期记账） | ✅ 纠正回 1091 |

**连带修复的根因（本次才查出来）**：`buildItem` 直接取前端传的 `subjectId`，而前端手工录单
**只传 subjectCode** ⇒ 145 条分录中 **36 条 subject_id 为 NULL**。重算是按 `subject_id` 汇总的，
这些分录会被**整批漏算**（第一次验证时表现为"总账纹丝不动"，因为 subjectIds 为空集合、循环根本没进）。
修法：`buildItem` 增加按 `subjectCode` 反查科目表的兜底解析；存量 36 条已在 `recalc-finance-ledger.sql`
的前置步骤中回填（现 145/145 全部有值）。

> ⚠️ **至此 §2.1 只剩 `closePeriod()` 的接线/删除未做**（全仓无调用方，属清理项）。

### 本轮修复记录之五：总账全量重算（2026-09-26）

**这是全模块最严重的一项 P0**（§2.1）：`LedgerServiceImpl.postToLedger()` 是**纯累加式**（`period_debit += 分录借额`），
没有「按凭证重算/回滚」入口 ⇒ 凭证删改后总账**永久残留、只增不减**，而利润表/资产负债表/总账账簿全部读它。

**执行的订正**（脚本固化在 `tools/recalc-finance-ledger.sql`，幂等、可重复执行）：

| 步骤 | 内容 |
|---|---|
| 备份 | `finance_ledger_backup_20260926`（22 行，含全部原值） |
| 前置修正 | 修正 **1 行 `tenant_id` 为 NULL** 的总账行（科目 2203）——这类行因租户为空，重算的 JOIN 匹配不上会被**静默跳过**，表现为「重算后仍有一行不符」 |
| 重算 | 按 (租户, 科目, 年, 期) 重算四段：期初 = 该期之前所有期的净额累计（纯凭证口径）；本期发生 = 该期「已过账且未删」凭证分录合计；期末 = 期初 + 本期借 − 本期贷 |

**结果（前后对比）**：

| 指标 | 重算前 | 重算后 | 凭证口径 |
|---|---|---|---|
| 总账借方合计 | **1,000,361** | **949,921** | **949,921** ✅ |
| 与凭证分录不一致的行 | 14 行 | **0 行** | — |
| 2026 / 2027 年度借贷平衡 | 否 | **均 true** | — |
| 资产负债表 `balanced` | — | **true** | — |

主要虚增被消除的科目（2026-9）：`6001` 20900/13630 → 0/1200；`1122` 12700/21200 → 1200/300；
`1403` 4560/3120 → 0/0；`2203` 2360/2500 → 0/0（孤儿行，凭证已不存在）。

> ⚠️ **这是「订正数据」而非「修复病根」**：`postToLedger` 的累加式逻辑**未改**，
> 今后再删改凭证仍会产生漂移。§2.1 的后续两步仍待做：
> ② `LedgerServiceImpl.closePeriod()` 接线或删除（当前全仓无调用方）；
> ③ 把 `postToLedger` 从「累加」改为「按凭证重算该科目该期」，实现自愈。
> 在此之前，**每次月结或批量删改凭证后应重跑 `tools/recalc-finance-ledger.sql`**。

### 本轮修复记录之四：§2.4 硬编码假数据（2026-09-23）

§2.4 共列 11 项，本轮按「**影响财务数字 + 改动明确、不需拍板**」选了 3 项修复：

| # | 位置 | 修复前 | 修复后 | 验证 |
|---|---|---|---|---|
| ① | `FinanceTransactionServiceImpl.getAccountBalance()` | 方法体只有 `return BigDecimal.ZERO;`，账户余额恒 0 | 读 `finance_account.balance`（由 `updateAccountBalance` 维护） | **代码级**——该方法**全仓无任何调用方**（无 HTTP 入口），已登记 |
| ② | `FinancialReportServiceImpl.generateIncomeStatement()` | `totalTax` 声明后从未累加 ⇒ 所得税恒 0、净利润虚高 | 补累加所得税类科目（6801） | 利润表接口**不回归**（200）。⚠️ 库中**无 6801 科目数据**，故修复后该项仍为 0——那是「数据未录」，不再是「代码写死」 |
| ③ | `ExpenseDocServiceImpl` 4 个付款账户 | `paySubjectCode` 一律写死 `1002`（银行存款）⇒ 现金账户付款也记成银行存款 | 新增 `resolveAccountSubject()` 取 `finance_account.subject_code`，查不到才兜底 1002 | **端到端实测 ✅**：账户 2（库存现金，档案科目 1001）建费用单 → `pay_subject_code = 1001`（修复前恒为 1002） |

**验证**：`node tools/verify-finance-hardcode-fix.cjs` → **6/6 通过**。

**§2.4 其余项的处置判断（未做，附理由）**：

| 项 | 为何未做 |
|---|---|
| `Payment/ReceiptServiceImpl` 的 `setPrintCount(0)` | **不是假数据**——`erp_payment`/`erp_receipt` **表与实体都没有 print 列**（已实测确认），属「该列未建模」，修它要加迁移+实体，是**新增功能**不是修缺陷 |
| `ExpenseServiceImpl` 审批人写死 `"finance_manager"` | 位于**旧 `erp/expense` 包**（FIN-DUP-01 登记的待废弃实现，新实现在 `expense-approval` 模块且已走 `sys_project_config`）⇒ 修废弃代码不划算 |
| 发票生成返回申请 ID / 批量开票·下载·导出返回空 / `FinanceReportServiceImpl` 只 new 对象 | 属「功能未实现」而非「假数据」，需要设计（发票实体、模板服务、报表取数），**超出本次范围** |
| `AccountSubjectServiceImpl:326` 恒 `setEnabled(true)` | 影响面小（辅助核算类型选项），可并入后续批次 |

### 本轮修复记录之三：凭证红冲 P0 红线 + 尾斜杠端点（2026-09-23）

**（一）`VoucherServiceImpl.reverse()` —— 报告 §2.3 那条 P0 红线**

修复前 4 个问题（前 2 个是原报告已记的，后 2 个是本次复核新查出的）：

| # | 问题 | 后果 | 修法 |
|---|---|---|---|
| 1 | 不调 `assertPeriodOpen` | 可向**已关账期间**生成红冲凭证并直接入账 | 补期间校验（用原凭证的 `fiscalYear/fiscalPeriod`） |
| 2 | 红冲凭证直接 `setStatus("posted")` + 手工 `postToLedger` | 绕过审核环节与凭证状态机 | 改为以 `draft` 入库，走 `audit() → post()` 标准流程 |
| 3 | **无原凭证状态前置校验** | 可红冲**草稿**凭证 ⇒ 原凭证未入账、冲销却入账，总账凭空多出反向分录；已冲销的也能重复红冲 | 仅允许 `posted` 状态可红冲 |
| 4 | 借用原凭证 `postBy` 当制单人 | 制单责任错位 | 取真实登录会话（`StpUtil`），不沿用 Controller 的 `userId` 请求头（那条路径缺失时会兜底成 `mock_*`） |

**（二）5 处 `@PostMapping("/")` 导致前端建单恒 404【本次新发现，属实测阻塞】**

调 `VoucherController` 时发现 `@PostMapping("/")` 在 **Spring Boot 3** 下要求请求带尾斜杠才匹配，
而前端一律用**无尾斜杠**（`voucherApi.create` / `receivableApi.create` / `payableApi.create`…）⇒ **新增凭证/应收/应付点了必 404**。

实测对照：

```
404  POST /erp/finance/voucher     {"code":404,"message":"接口不存在: api/erp/finance/voucher"}
200  POST /erp/finance/voucher/    {"code":200,"message":"创建成功","data":{"id":194,...}}
```

修复：财务域 5 处 `@PostMapping("/")` → `@PostMapping`（`VoucherController`、`ReceivableController`、`PayableController`、
`AccountingPeriodController`、`FinanceTransactionController`）。
> 另在 `core-api` 还有 3 处同类写法（`CleanupRuleController`/`DataSourceController`/`SyncTaskController`），
> 非本次范围、且需先确认调用方，**未擅自改动**，已登记。

**验证**：`node tools/verify-voucher-reverse.cjs` → **18/18 通过**（A 正常红冲 9 项 · B 草稿被拒 · C 重复红冲被拒 · D 已关账被拒 + 期间还原 · 清理 + 总账快照恢复）。

> ⚠️ **脚本自身的一个坑（已内建处理）**：红冲会 `postToLedger`，而 `finance_ledger` 是**纯累加式**
> （§2.1 的根因：删凭证**不会**回退总账）⇒ 只删测试凭证会把 1001/6001 的发生额永久抬高、污染报表。
> 脚本因此加入「测试前采集总账快照 → 跑完按快照精确恢复」并断言恢复结果，实测 `恢复后=[3,17] 测试前=[3,17]`。

### 本轮修复记录之二：P0 租户硬编码清理（2026-09-23）

报告 §2.3 当时写的是"8 处"，**实际清点是 36 处 / 17 个文件** —— 这是本轮修正的一个口径：原先按"含 `DEFAULT_TENANT_ID` 常量的文件"计数，漏掉了直接 `setTenantId(1L)` 的写入点。

| 类别 | 处数 | 改法 |
|---|---|---|
| **查询侧**（`DEFAULT_TENANT_ID` 常量） | 19 | 换为 `currentTenantId()`（会话租户优先，取不到回落 1），与项目已有的正确范本 `AccountingPeriodServiceImpl` 完全一致 |
| **写入侧**（`setTenantId(1L)`） | 17 | **删除**，改由 `MyBatisPlusConfig.insertFill` 填会话租户 |

**涉及文件**：`VoucherServiceImpl`(1) · `MonthClosingServiceImpl`(6) · `ReconciliationServiceImpl`(1) · `FinanceAuxiliaryTypeServiceImpl`(5) · `FinanceAuxiliaryItemServiceImpl`(6) · `ArApAdjustServiceImpl`(1) · `CashTransferServiceImpl`(1) · `ExpenseDocServiceImpl`(1) · `OffsetServiceImpl`(2) · `PrePaymentServiceImpl`(3) · `PreReceiptServiceImpl`(3) · `CapitalFlowServiceImpl`(1) · 5 个 Controller 各 1

**为什么写入侧敢直接删**（三条证据，缺一不可）：
1. 财务域 **零手写 `@Insert`**（全模块 grep 命中 0 条）⇒ 所有插入都走 `BaseMapper.insert` / `IService.save`，必然触发 `insertFill`；
2. `insertFill` 的实现是 `if (tenantId == null) { fill(会话租户) }` ⇒ **显式 `setTenantId(1L)` 反而会阻止自动填充**，所以删掉才是对的；
3. 财务域**无定时任务**（`@Scheduled`/`JobHandler` 全 0），且这些入口都带 `@SaCheckPermission` ⇒ 必然有登录会话，不会走到"未登录 → tenantId 为 null"的分支。

**验证**：`erp-finance test-compile` → BUILD SUCCESS；新增 `tools/verify-finance-tenant-fix.cjs`（新建 → 断言落库 `tenant_id` 等于会话租户且不为 NULL → 重名校验 → 列表可见 → enable 端点 → 清理测试数据）。

> ⚠️ **未能真机验证的部分（如实记录）**：租户 2 在库中只有 1 个 HR 账号（`e2e_hr_t2`，角色 `E2E_T2_ADMIN` 持有**财务权限码 0 个**），调财务接口必 403，因此**"非 1 租户会落成 2"目前只有代码证据与逻辑推理，没有真机断言**。要闭环需先给该角色授财务码（属权限配置，见 §3.7，需先拍板）。

**整体判断**

财务模块的**页面骨架是通的、权限注解是齐的**——48 个菜单入口实测 100% 能打开、0 失败请求、0 console 错误；457 个后端端点 100% 带 `@SaCheckPermission`（全仓唯一做到 0 裸端点的业务域）；47 条财务菜单的组件文件 0 缺失。这三项比销售/采购模块做得更好。

但**账务内核是坏的**：

1. **总账与凭证分录对不上**（差 44,130），而利润表/资产负债表/总账账簿全部读总账 ⇒ **对外报出的财务数字不可信**。这是全模块第一优先级。
2. **记账失败被静默吞掉**：付款/预收/预付/对冲/费用五条主链路共 10+ 处 `catch(Exception){log}` 后照常返回成功，单据与资金流水已落库、凭证却没生成，**账实不符且无人知晓**。
3. **"关账后禁记账"这条 P0 红线在非 1 租户下静默失效**（`DEFAULT_TENANT_ID = 1L`）。
4. **多处硬编码假数据**直接返回给前端：账户余额恒 `0`、利润表所得税恒 `0`、审批人写死 `"finance_manager"`、发票生成返回申请 ID 当占位符、批量开票/导出返回空数组。
5. **权限虽然注解齐全，但落不到人**：217 个财务码中 201 个只挂超管，非超管除 16 个码（月结/其他收入/会计期间/对账）外**全部 403**；SoD 规则表 0 行。

---

## 一、已确认可用的部分（正面结论，全部【实测】）

这些是本次检查中**验证通过**的项，供收尾阶段放心：

| # | 结论 | 证据 |
|---|---|---|
| 1 | **48 个财务菜单入口 100% 可打开**，0 失败请求、0 console 错误 | `node tools/e2e-finance-audit.cjs` → `入口 48｜异常 0｜失败请求页 0｜console 错误页 0`（`tool-results/finance-audit/e2e-result.md`） |
| 2 | **后端 457 端点 100% 带 `@SaCheckPermission`**，0 裸端点 | 逐控制器比对「`@*Mapping` 数 = `@SaCheckPermission` 数」55/55 相等；`known-unauthorized-controllers.txt` 财务域条目已全部注释「本域已出清」 |
| 3 | **47 条财务菜单的 component 文件 0 缺失** | 全表 310 条 component 做文件系统命中测试，财务域 37 行全部命中 |
| 4 | **代码引用的财务权限码 100% 在库、100% 已关联角色** | 无「代码引用但库里没有」的财务码 ⇒ 非超管不会因缺码被意外 403（对比：crm 域有 1 个 `crm:contract:refresh` 缺失） |
| 5 | **`erp-finance` 编译通过** | `./mvnw -o -pl erp/erp-finance test-compile` → `BUILD SUCCESS`（**纠正**：多篇开发文档记载的"编译阻塞、6+ 处 pre-existing 编译错误"是过时记录） |
| 6 | **凭证头 `summary` 已回填**（FIN-BREAK-02 修复生效） | `finance_voucher` 69 行：`summary` **69/69** 有值、`source_no` **59/69** 有值（修复前为 0/69） |
| 7 | **凭证借贷平衡** | `finance_voucher_item` 借方合计 = 贷方合计 = **953,291.00** |
| 8 | **无 TODO/FIXME 残留** | 两模块 grep 真实待办标记 = **0 处**（仅 2 处 javadoc 里的文档引用） |
| 9 | **财务模块无双 `/api` 前缀问题** | 697 处前端调用里双前缀 = 0（全仓仅 2 处，均在 `views/erp/mall/user-audit/form.vue`） |

---

## 二、P0 致命问题

### 2.1 总账 ≠ 凭证分录，利润表/资产负债表数字失真【实测·读码】

**这是全模块最严重的问题，且 `MASTER_TODO` 登记为"待财务确认"，至今未修。**

同一批业务，两套口径对不上（`finance_voucher_item` 是分录明细，`finance_ledger` 是总账余额表）：

| 科目 | 凭证分录 借 / 贷 | 总账 借 / 贷 | 差异 |
|---|---|---|---|
| 1002 银行存款 | 550.00 / 946,666.00 | 1,400.00 / 947,666.00 | 总账多 1,850 |
| 1122 应收账款 | 3,700.00 / 1,700.00 | **17,900.00 / 22,000.00** | 总账多 **44,500** |
| 1403 库存商品 | 255.00 / 0.00 | 4,815.00 / 3,120.00 | 差异 7,680 |
| 2203 预收账款 | 0.00 / 250.00 | 820.00 / 1,100.00 | 差异 1,670 |
| 6001 主营业务收入 | 1,200.00 / 3,700.00 | 20,900.00 / **18,230.00** | 总账多 **23,230** |
| 6401 主营业务成本 | 720.00 / 0.00 | 3,120.00 / 4,560.00 | 差异 6,960 |
| **合计** | **953,291.00** | **997,421.00** | **44,130** |

**代码根因**（`LedgerServiceImpl.java:448`）：`postToLedger()` 是**纯累加式**——`setPeriodDebit(existing + debit)`，**无按凭证重算/回滚入口**；唯一的重算方法 `closePeriod()`（`:482`）**全仓无调用方**。凭证删改后总账永久残留。

**取数源分裂**：`TrialBalanceMapper`（试算平衡）读 `finance_voucher_item`（正确），而 `FinancialReportServiceImpl` 在 `:47,:68,:188,:547,:809,:876` 六处读 `finance_ledger`（虚增）⇒ **利润表、资产负债表、总账账簿三个报表全部偏**。

**影响**：不是显示问题，是**对外财务数据不可信**。

**修法**（三步）——**当前状态**：
① 新增"按凭证重算 `finance_ledger`"入口并执行一次 → **✅ 已完成 2026-09-26**（入口 `tools/recalc-finance-ledger.sql`，已执行：总账借方合计 1,000,361 → 949,921，与凭证口径一致）；
② `closePeriod()` 接线或删除 → ⬜ 未做（仍全仓无调用方）；
③ 全部报表统一到「凭证 → 总账」单向派生 → **✅ 已完成**（`postToLedger` 已改为按凭证重算，总账成为凭证的确定性派生，报表读它就是读凭证口径）。

### 2.2 记账失败静默吞异常 —— 单据与资金已落库、凭证缺失【读码】

10 处主链路 `catch (Exception e) { log.xxx(...) }` 后**不重抛、不置失败态**，方法继续返回成功：

| 位置 | 后果 |
|---|---|
| `payment/service/integration/PaymentBusinessIntegrationService.java:70,93,115,137,161,183,205,227` | 8 个业财集成方法全部静默；类上带 `@Transactional(rollbackFor=Exception.class)`，但异常被吞后**事务照常提交** |
| `payment/service/impl/PreReceiptServiceImpl.java:132,211` | 预收款单已标记 received、流水已写，「预收账款 2203」凭证可能不存在 |
| `payment/service/impl/PrePaymentServiceImpl.java:131,207` | 预付款同构，1123 凭证可能缺失 |
| `payment/service/impl/OffsetServiceImpl.java:130,144` | 应收/应付凭证各一个独立 catch ⇒ **可能单边挂账、对冲凭证不平衡** |
| `finance/otherincome/service/impl/OtherIncomeDocServiceImpl.java:364-369` | 凭证为 null 仍置 `status=1`（已记账）、`settleStatus=1`；且 `confirm` **无 `@Transactional`**（`:298`） |
| `finance/service/impl/BusinessAccountingServiceImpl.java:161-165` | `audit/post` 失败仅 warn，仍 `return created`（草稿）⇒ 调用方无法区分"已过账"与"仅建草稿" |
| `expense/service/impl/ExpenseServiceImpl.java:270-273` | 注释直言 `// Do not rollback the approval`，费用单状态 APPROVED 但凭证未生成 |
| `invoice/service/impl/InvoiceMatchingServiceImpl.java:455-459` | 付款记录已置"已匹配"、发票侧未更新 |
| `payment/service/impl/CapitalFlowServiceImpl.java:314-324` | `catch (NumberFormatException) { return 2; }` 把无法解析的支付方式一律当"银行转账" |
| `finance/analytics/support/AnalyticsSupport.java:50-54` | 非数字金额静默当 0；`numericId` 静默返回 null |

**对照（做对了的）**：`ExpenseDocServiceImpl.java:260`、`CashTransferServiceImpl.java:235`、`ArApAdjustServiceImpl.java:223`、`FinanceAuxiliaryBalanceServiceImpl.java:109` 四处都是 **catch 后抛 `BusinessException`** —— 同一模块内两种口径并存，属于应当统一到"抛异常"的那一侧。

### 2.3 关账红线在非 1 租户下静默失效【读码】

```java
// VoucherServiceImpl.java:40
private static final Long DEFAULT_TENANT_ID = 1L;
// :113  assertPeriodOpen
accountingPeriodMapper.findByPeriodCode(DEFAULT_TENANT_ID, periodCode)
        .ifPresent(p -> { if (p.getStatus() == 0) throw BusinessException.badRequest("会计期间已关闭…"); });
```

非 1 租户会话下查不到期间记录 ⇒ `ifPresent` 不触发 ⇒ **已关账期间仍可新增/修改凭证**。「月结」文档里的 P0 红线（"严禁在已关闭期间新增/修改凭证"）只在租户 1 成立。

**同源问题**：`MonthClosingServiceImpl`（`:57,:181,:195,:203,:393,:419` 六处）、`ReconciliationServiceImpl.java:342-345`（`return 1L; // 临时实现`）、`FinanceAuxiliaryItem/TypeServiceImpl`、`ExpenseDocServiceImpl:192,386`、`CashTransferServiceImpl:172,298`、`ArApAdjustServiceImpl:171,287` 全部硬编码租户 1。

**附带（本轮已修 ✅）**：`VoucherServiceImpl.reverse()` 曾**既不走 `assertPeriodOpen` 也不走审核**，直接 `setStatus("posted")` + `postToLedger` ⇒ 可向已关账期间生成红冲凭证并直接入账。
本轮复核还查出**另外两个未记录的问题**：① 无原凭证状态前置校验 ⇒ 可红冲**草稿**凭证（原凭证根本没入账，冲销却入了账，总账凭空多出反向分录）；② 借用原凭证的 `postBy` 当制单人。详见「本轮修复记录之三」。

### 2.4 硬编码假数据直接返回给前端【读码】

| 位置 | 假数据 |
|---|---|
| `finance/service/impl/FinanceTransactionServiceImpl.java:94-95` | ~~`getAccountBalance()` 方法体 **只有 `return BigDecimal.ZERO;`**，不查任何表 ⇒ 账户余额恒 0~~ **已修 ✅**（改查 `finance_account.balance`；⚠️ 该方法**当前无任何调用方**，属死方法，本次保留了能力并修正行为） |
| `finance/service/impl/FinancialReportServiceImpl.java:816,853` | ~~`totalTax` 声明后**从未累加**~~ **已修 ✅**（补累加所得税类科目 6801；注：**库中当前无 6801 数据**，故修复后该项仍为 0 —— 属「数据未录」而非「代码写死」）。**未修**：`:819-830` 收入/成本/费用科目编码仍写死 `{"6001","6051",...}` |
| `expense/service/impl/ExpenseServiceImpl.java:535-559` | 审批人返回写死字符串 `"finance_manager"` / `"dept_manager_"+departmentId` / `"approver_default"` ⇒ **审批任务指派给不存在的登录主体，审批流形同虚设** |
| `invoice/service/impl/InvoiceApplicationServiceImpl.java:214-222` | 注释自认"为了简化"，`setInvoiceId` 被注释掉，`return application.getId()`（**返回申请 ID 当发票 ID**），发票实体从未创建 |
| `invoice/service/impl/InvoiceServiceImpl.java:489-490,380-383,482-484` | 批量开票 `return List.of()`；下载 `return new byte[0]`；导出 `return new byte[0]` ⇒ **接口恒 200 + 空** |
| `finance/service/impl/FinanceReportServiceImpl.java:22-34` | "生成财务报表"只 new 一个对象就 insert，**未做任何取数计算** |
| `payment/controller/PaymentController.java:286-295` | `batchPrint` 只 `put("success", true); put("count", ids.size())`，**未生成任何打印内容** |
| `payment/service/impl/PaymentServiceImpl.java:206` / `ReceiptServiceImpl.java:264` | 无条件 `setPrintCount(0)` ⇒ 打印次数报表恒 0 |
| `finance/service/impl/AccountSubjectServiceImpl.java:326` | `getAuxTypeOptions` 不看库中实际值，恒 `setEnabled(true)` |
| `finance/cashtransfer` + `finance/expensedoc` | ~~4 个付款账户的 `paySubjectCode` 一律置 `1002`~~ **expensedoc 已修 ✅**（新增 `resolveAccountSubject()` 取 `finance_account.subject_code`，查不到才兜底 1002）；**cashtransfer 未修** |
| `expense/service/integration/ExpenseAccountingService.java:50-58` | 借/贷科目写死 `6602`/`2241` ⇒ 所有费用类型（差旅/销售/研发）一律计入管理费用 |

### 2.5 预算台账可被绕过改写【读码】

- `BudgetItemServiceImpl.update`（`:33-37`）：`BeanUtil.copyProperties(dto, entity, "id", "budgetId")` 把 `usedAmount/frozenAmount/remainingAmount/executionRate` 全部覆盖，**无任何恒等式校验**。而同模块 `BudgetControlServiceImpl` 类注释（`:20-29`）明示"三列为唯一事实来源…禁止任何页面/接口直接改写"。
- `BudgetControlServiceImpl`（`:77,:108,:147`）：`freezeAmount/releaseFrozenAmount/consumeBudget` **无 `amount > 0` 校验** ⇒ 传负金额时"冻结"变"解冻"、"消耗"变"回冲"，**可用请求参数反向篡改预算台账**。
- 同类：全部为 `findById → 内存加减 → save`，`BudgetItem` 及其基类**无 `@Version`**（对比发票实体有）⇒ 并发冻结/消耗后写覆盖先写。
- `BudgetAdjustmentServiceImpl.delete`（`:199-208`）用 `deleteById` **物理删除**（同模块 AnnualBudget/Template 均用逻辑删除）⇒ 审批痕迹不可恢复。

---

## 三、P1 严重问题

### 3.1 17 条财务菜单在普通租户下被整批过滤【实测+读码】

`SysMenuServiceImpl.getUserMegaMenus` 对「非系统租户 + 非超管」强制 `wrapper.eq(SysMenu::getMenuLevel, 0)`（`:277-279`）。

而 `sys_menu.menu_level` 全库分布为 **0:249 / 1:43 / 3:127 / 4:3** —— **没有 2，却有一个前端不支持的取值 3**（前端菜单管理页只提供 0=租户级/1=系统级）。财务域 **17 条**菜单正是 `menu_level=3`：

> 按单收款(80100) · 收款单(80101) · 预收款单(80102) · 提现存现转款(80103) · 待确认款项(80104) · 在线支付对账单(80105) · 按单付款(80110) · 付款单(80111) · 预付款单(80112) · 费用单(80115) · 其他收入(80116) · 应收应付调整(80117) · 会计凭证(80120) · 月结(80121) · 对账(80122) · 预算编制(80130) · 预算执行(80131)

**后果**：普通租户登录后，**收付款、费用、凭证、预算这些核心录入页在导航里根本不出现**。超管（系统租户）不受影响 —— 所以开发/验证时发现不了。

### 3.2 财务总览工作台 4/6 快速入口点了必 404【实测 · 本轮已修 ✅】

`views/finance/index.vue:309-319` 硬编码跳转表，与真实路由不符：

| 卡片 | 代码写死 | 实际落地 | 判定 |
|---|---|---|---|
| 科目管理 | `/erp/finance/subject` | 同 URL | **404** |
| 凭证管理 | `/erp/finance/voucher` | 同 URL | **404** |
| 应收账款 | `/finance/receivable` | 同 URL | OK |
| 应付账款 | `/finance/payable` | 同 URL | OK |
| 财务报表 | `/erp/finance/reports` | 同 URL | **404** |
| 对账管理 | `/erp/finance/reconciliation` | 同 URL | **404** |

实测命令 `node tools/verify-finance-dashboard-links.cjs`，判定四重（URL + `.not-found` + 正文文案 + 主内容渲染），结果：`科目管理:**404** | 凭证管理:**404** | 应收账款:OK | 应付账款:OK | 财务报表:**404** | 对账管理:**404**`。
> 真实路由前缀是 `/finance/*`（菜单 path 驱动），而代码写的是 `/erp/finance/*`；`MODULE_ROUTE_MAP` 里的 `'finance': ['finance','erp/finance',...]` 只用于**模块授权校验、不产生路由**（已读码确认），不能救这些路径。

**✅ 本轮已修**：跳转表已改为真实路径，实测 **6/6 OK**（见「本轮修复记录」#1）。

### 3.3 辅助核算「启用」开关点了必 404【实测 · 本轮已修 ✅】

`views/finance/auxiliary/index.vue:394,558` 调用 `auxiliaryTypeApi.toggleEnabled` / `auxiliaryItemApi.toggleEnabled` → `PUT /erp/finance/auxiliary/{type|item}/{id}/enable`。
但后端 `FinanceAuxiliaryController` 只有 `PUT /type/{id}`、`PUT /item/{id}`（更新），**没有 `/enable` 端点**。

实测：

```
404  PUT /erp/finance/auxiliary/type/1/enable   {"code":404,"message":"接口不存在: api/erp/finance/auxiliary/type/1/enable"}
404  PUT /erp/finance/auxiliary/item/1/enable   {"code":404,"message":"接口不存在: api/erp/finance/auxiliary/item/1/enable"}
400  PUT /erp/finance/auxiliary/type/1          {"code":400,"message":"类型编码不能为空"}   ← 对照：该端点存在
```
**用户在列表里拨「启用」开关，界面会报错、状态不会变。**

**✅ 本轮已修**：后端补 `PUT /{type|item}/{id}/enable`，实测 HTTP 200 且库中 `enabled` 真实变更（见「本轮修复记录」#2）。

### 3.4 收款单「批量打印」返回 405【实测 · 本轮已修 ✅】

`views/finance/receipt-doc/index.vue:790` 调 `receiptApi.batchPrint` → `POST /erp/receipt/batch-print`，而 `ReceiptController`（基路径 `/api/erp/receipt`）**没有 `batch-print` 端点**。

```
405  POST /erp/receipt/batch-print      {"code":405,"message":"请求方法不支持，请使用 GET,PUT"}
200  POST /erp/payment/batch-print      ← 对照：付款单同功能正常
```
405 而非 404 的原因：该路径被 `ReceiptController` 的 `@GetMapping("/{id}")` / `@PutMapping("/{id}")` 吞掉（`id="batch-print"`），方法不匹配才报 405。**付款单有、收款单没有 —— 典型的单侧漏实现。**

**✅ 本轮已修**：后端补 `POST /erp/receipt/batch-print`（与付款单对称），实测 200。⚠️ 仅消除契约不一致，**真打印仍未实现**（见「本轮修复记录」#3）。

### 3.5 9 个空目录 + 7 个死页面【实测 · 已处理 ✅】

**空目录（`find` 确认 0 文件）**：`capital-flow`、`expense-apply`、`expense-pay`、`expense-reimburse`、`pre-receipt`、`report`、`subject`、`subsidiary-balance`、`trial-balance`（另有空子目录 `reconciliation/components`）。

**死页面（三方比对：菜单 component/list_path + 路由注册表 + 全仓 grep）**：

| 文件 | 行数 | 判定依据 |
|---|---|---|
| `views/finance/receipt/index.vue` | 1105 | `componentMap` 有键但无菜单引用 ⇒ 不生成路由 |
| `views/finance/payment/index.vue` | 1033 | 同上 |
| `views/finance/pre-payment/index.vue` | 1068 | 菜单 80112 指向 `advance-payment/form.vue`，此页无人引用 |
| `views/finance/voucher/VoucherDetail.vue` | 487 | `voucher/index.vue` 不 import，跳的是 `/finance/voucher/form?id=` |
| `views/finance/reports/components/{BalanceSheet,CashFlowStatement,ProfitStatement}.vue` | 241+224+224 | 全仓 grep `reports/components` **0 命中**；内部有真实 API 调用，是被新页面取代的旧实现 |

**连带风险**：**11 个 e2e 测试文件把上述空目录/死页面当作已实现页面做断言**（如 `frontend/tests/e2e/automation-test.ts:86-93` 断言 `/finance/pre-receipt`、`/finance/capital-flow`），因 catch-all 掩盖而长期绿灯。

**另**：`v-permission` 在 finance 有 29 次，其中 **18 次（62%）写在死页面上**；`receiptApi.complete`、`paymentApi.complete`、`voucherApi.reverse` 的**唯一调用方都是死页面** ⇒ 这些能力当前无入口。

### 3.6 60 个死 API + 4 个整对象零引用【清点】

零引用的 api 对象：`writeOffApi`(4 方法)、`depositConditionApi`(8)、`partnerBalanceApi`(1)、finance 版 `collectionStatsApi`(1)。
另有 6 组"同路径双实现"的旧封装零引用。

> 注：`offsetApi.getItems`（`GET /erp/offset/{id}/items`）实测 **404**，且页面未调用 ⇒ 死 API + 断链双属性。

### 3.7 权限配好了但落不到人【实测】

| 指标 | 数值 |
|---|---|
| 财务语义权限码 | **217**（`finance:` 138） |
| 仅 SUPER_ADMIN 持有 | **201 / 217** |
| 非超管角色实际持有的财务码 | SYSTEM_ADMIN 与 DEPT_ADMIN **各 16 个、完全相同**（仅月结/其他收入/会计期间/对账） |
| 库中存在但代码零引用（僵尸码） | 5（与仓内 `permission-effectivity.json` 完全一致） |
| 未关联任何角色的码 | 0 |
| `sys_role_menu` 全表 | **3 行，全属 SUPER_ADMIN** |
| `sys_sod_rule` / `sys_data_scope` / `sys_field_permission` | **全 0 行** |

**两个后果**：
1. **非超管登录后，应收应付、收付款、凭证、预算、资产、报表全部 403** —— 权限注解是齐的，但没有任何功能角色被授权。
2. **菜单权限派生失效**：可见性改由 `sys_permission` 前缀派生（`MenuPermissionDeriver`），但财务菜单码里有 **23/38 条用短横线**（`finance:receipt-doc`），权限码用**冒号**（`finance:receipt:view`）⇒ 前缀永远命中不上 ⇒ 退化为 fail-open「无对应权限码 ⇒ 保持可见」。菜单级权限对财务域基本不起作用。

**SoD 零落地**：`DEPT_ADMIN` 同时持有"月结执行"与"月结反结"（典型互斥职责），文档中亦无任何 SoD 要求（26 篇**零命中**）—— 属规范空白。

### 3.8 已取消单据的应收仍挂账【实测】

```sql
select r.source_no, r.total_amount, r.status, o.status as outbound_status
from finance_receivable r left join erp_sale_outbound o on o.outbound_no = r.source_no;
```
结果：6 行全部 `status='normal'`，而对应销售出库单 `status=12`（**已取消**）。
⇒ 与销售模块审计（`SALES_MODULE_AUDIT_20260922.md` §2.2）同一问题：**销售侧取消出库不冲销应收**，在财务侧表现为 6 笔挂账永不清理。

### 3.9 重复记账风险：同一单据多时点写流水【读码+实测】

`PaymentServiceImpl.java:301,482,499` —— `recordPaymentFlow(payment)` 在 **create / complete / cancel 三个时点各调一次**，全部用 `payment.getPaymentAmount()` 全额入账。`ReceiptServiceImpl.java:347,528,545` 完全对称。

实测数据可见同额多流水：
```
YBFYD-20260910-058 → CF202609100028/033/038，各 5000.00 OUT（14:40:29 / 14:40:41 / 14:40:44）
```
> ⚠️ 口径说明：这 3 条间隔 12 秒，**更可能来自测试期重复操作**，不能直接断言为代码缺陷。但代码层面"三个时点各写全额流水"是事实，取消时也写一笔**全额 IN**（而非冲销）⇒ 统计口径会被放大，建议核对。

**同源**：`PaymentServiceImpl.writeOff`（`:577-580`）核销时第 3 参 `payableId` **硬传 null**；`ReceiptServiceImpl:623-626` 同构 ⇒ **核销记录无法关联单据**。`PaymentAccountingService.postPaymentVoucher`（`:95-98,:148-151`）的核销分支因**所有调用方末参恒传 null 而永不执行** ⇒ 只有凭证、没有核销。

### 3.10 发票匹配整块是"死岛"【清点·高置信】

`InvoiceMatchingService`（接口，19 个方法）+ `InvoiceMatchingServiceImpl` + `MatchingStatistics`(dto) + `PaymentRecordRepository` + `PaymentRecord`(invoice 包) 构成一个**完整的零入口子系统**：
- 55 个 Controller 无一处注入
- 全仓类名引用只落在自身与文档/迁移注释里
- 编译产物常量池证明 impl 解析到的是接口的内嵌 `MatchingStatistics`，`model/dto/MatchingStatistics` 是**被遮蔽的死导入**

⇒ 发票「自动匹配 / 手动匹配 / 批量匹配 / 到期匹配 / 生成匹配报告」**没有任何 HTTP 或内部入口**。

同一报告另有：`PaymentBusinessIntegrationService` 整类零引用（12 个方法全零调用，类注释声称"为销售/采购/费用/资产提供入口"，实际无一调用）。

### 3.11 经营看板的 5 个财务指标是随机数【读码·本次亲自确认 · 已修 ✅】

`erp-observability` 的 `MetricCollectorServiceImpl.collectFinanceMetrics()`（`:196-236`）：

```java
"今日收入"  → generateRandomValue(30000, 60000)  "元"
"今日支出"  → generateRandomValue(10000, 30000)  "元"
"应收账款"  → generateRandomValue(100000, 300000) "元"
"应付账款"  → generateRandomValue(50000, 150000)  "元"
"毛利率"    → generateRandomValue(20, 40)         "%"
```

**5 个全部不查任何财务表，直接返回随机数**，并经 `collectAllMetrics`（`:230`）并入全量指标集。

⇒ 任何消费这些指标的地方（经营看板 / 监控大屏 / 指标告警）**看到的财务数字是随机的**。
这不是 `erp-finance` 内的代码，但呈现的是财务口径数据，建议与财务模块一并处置（接真实取数，或下线该指标集）。

**同源旁证**：同模块 `MetricsCalculationServiceImpl.java:493-504` 的 `finance_revenue_month` 实际读 `erp_sale_order`（销售表），`:510` 利润率直接 `return 0`；`crm` 的 `calculateTotalDebt()`（`:297-299`）是桩、恒返回 `BigDecimal.ZERO`。

### 3.12 DMS → 财务的事件通道悬空【读码·本次亲自确认】

DMS 用 outbox 向财务推送 `PAYMENT_PUSH_FINANCE`、`SETTLEMENT_PUSH_ERP`，URL 由 `EventService.java:172-175` 写死拼接：

```java
return "http://localhost:8080/api/" + target.toLowerCase() + "/callback/" + eventType.toLowerCase();
```

**核实结论**：
- 全仓搜 `@RequestMapping`/`@PostMapping` 匹配 `/api/erp/callback/{eventType}` —— **不存在**（搜到的 `/callback` 端点分属 workflow / payment 渠道回调 / dms 自身 / mall，无一是 ERP 侧承接方）；
- 端口亦不符：URL 写死 `8080`，本仓 ERP 监听 **5655**。

⇒ 该"解耦通道"从发布到消费整链不通。（DMS 侧同方法内另有直连 `BusinessAccountingService` 调用，故业务结果可能仍产生，但 outbox 这条路是死的。）

---

## 四、P2 问题（分类汇总）

### 4.1 规范性

| # | 问题 | 证据 |
|---|---|---|
| 1 | **`page` vs `list` 混用**、路径前缀不统一、动词化路径 | 见 `tool-results/finance-audit/backend-inventory.md` §5 |
| 2 | **响应契约三分裂**（同一模块三种包装结构） | 同上 §5.7 |
| 3 | 权限名中英混杂（如「财务receipt查看」） | `menu-permission-db.md` §4.1 |
| 4 | `view` 与 `detail` 语义重复成对码 | 同上 §4.2 |
| 5 | 单号生成口径不一：`KJPZ-` vs 实跑 `YYYYMM-XXXX` | `doc-spec.md` 红线冲突项 |
| 6 | 多处单号生成**无并发保护**（`selectCount+1` / `MAX+1` / `System.currentTimeMillis()`），无唯一约束兜底 | `VoucherServiceImpl:351`、`ExpenseServiceImpl:463`、`ReconciliationServiceImpl:79`、`PartnerLedgerServiceImpl:367` |
| 7 | `BigDecimal.ROUND_HALF_UP` 已弃用 API（4 处），同库混用两种口径 | `ExpenseServiceImpl:447` 等 |
| 8 | 异常类型不当：`RuntimeException` 而非 `BusinessException`（返回 500 而非 404/400） | `BudgetItemServiceImpl:35,47`、`BudgetTemplateServiceImpl:56,78,86,127` |
| 9 | 文档与代码矛盾：`PartnerLedgerController:32-36` 注释称"未挂权限码"，`:64` 实际已挂 | 读码确认 |
| 10 | **路径被 `/{id}` 吞掉**：`GET /erp/finance/subject/page` 实测返回 `400 参数[id]格式不正确`（`page` 被当作 id）——同类现象已在收款单 `batch-print`（405）上出现，属「宽泛路径变量 + 兄弟端点」的通病。**当前无前端消费方**，故仅登记 | 实测 |

### 4.2 精度与金额

- `FinanceTransactionServiceImpl:53,58`、`FinanceTaxDeclarationServiceImpl:64-70` **金额用 `double`** 汇总
- `InvoiceMatchingServiceImpl:373-381`、`MatchingStatistics:75-102` 百分比用 `double`
- `InvoiceServiceImpl:449-454` 用 `String.format("%.2f", BigDecimal)` 格式化
- `ExpenseItem.java:115` 除法未指定 scale/rounding ⇒ 遇非终止小数抛 `ArithmeticException`
- `PaymentServiceImpl` / `ReceiptServiceImpl` 用 `Integer != Integer` 做**引用比较**（当前值落在 Integer 缓存内"侥幸"正确）

### 4.3 性能

`ExpenseServiceImpl:409`（`findAll()` 全表入内存后循环）、`LedgerServiceImpl:139,149` + `FinancialReportServiceImpl:72,289,550,810,880` + `AccountSubjectServiceImpl:293,340`（反复 `selectList(null)` 全量装载）、`BudgetExecutionServiceImpl:144-164`（内存分页）、`BudgetReportServiceImpl:66,121,155`（N+1）、`PartnerLedgerServiceImpl:99-105`（逐行查 `biz_party`）、`PaymentController:308-334` / `ReceiptController:293-319`（逐状态 count + 全量 list）。

### 4.4 数据库层【实测】

| 类别 | 明细 |
|---|---|
| **0 行表（财务域）** | `finance_auxiliary_balance`、`finance_report`、`finance_tax_declaration`、`finance_transaction`、`fin_chart_config`、`fin_reconciliation`、`fin_reconciliation_item`、`erp_ar_ap_adjust(_item)`、`erp_cost_sharing_expense_item`、`erp_initial_finance_partner`、`erp_initial_finance_subject`、`erp_finance_subject`、`budget_adjustment`、`budget_template(_item)`、`erp_purchase_order_settlement`、`erp_sale_order_settlement`、`expense_*`(6 张)、`fee_*`(3 张)、`payment_reconciliation`、`invoice_application(_item)`、`dms_settlement(_item)` |
| **两套科目表** | `erp_finance_subject`（**0 行**，仅 `V6.8.0` 建表，全仓**无任何 Java/WXML 引用** ⇒ 死表）vs `finance_account_subject`（**107 行**，真实在用） |
| **两套费用表** | `erp_expense_doc/item`（40/40 行，在用）vs `expense_application/approval/attachment/item/payment/reimbursement`（**全 0 行**，旧 `cn.aiedge.erp.expense` 包还在用）—— 即 FIN-DUP-01 |
| **字段全空** | `erp_capital_flow` 41 行：`bank_account` / `bank_name` / `transaction_no` / `reconcile_at` **全部 0**；`finance_voucher.handler_name/dept_name` **0/69**；`finance_voucher_item.aux_staff` 仅 2/145 |
| **应收/应付来源单一** | `finance_receivable` 6 行全 `SALE_SHIPMENT`；`finance_payable` 3 行全 `PURCHASE_RECEIPT` ⇒ 手工应收应付、调整单、期初均无数据 |

> `sys_dept` **0 行** vs `sys_department` **5 行**：费用审批链与辅助核算读的是 0 行的那张 ⇒ 部门维度在第一级就断。

### 4.5 功能未接通（文档自述 105 条待完善中的代表项）

| 项 | 出处 |
|---|---|
| 在线支付对账：对账动作未接通、标记后不进入待确认款项 | 在线支付对账文档（4 条未标 ✅） |
| 费用单：预算联动回写待补、往来单位核算项待核对 | 费用单文档 |
| 费用统计：预算对比阻塞于预算执行科目口径 | 费用统计文档 |
| 利润表：净利润与资产负债表权益变动勾稽待补 | 利润表文档 |
| 明细账：9 条待完善（最多的一篇） | 明细账文档 |
| 凭证编号/制单状态回写主单据未接通 | 会计凭证文档 |
| 「保存后立即打印」类死开关 | 前端盘点 §7 |

---

## 五、与开发文档的验收基准对照

**26 篇**文档（非 27/28；子目录「辅助核算余额表/」只有 5 张 PNG，无 .md）。

| 统计项 | 数值 |
|---|---|
| 文档自述「待完善/遗留」条目 | **105 条**（+预算执行跨模块观察 2 条） |
| 「跨模块检查问题」未标 ✅ | **20 条** |
| 「最终裁决方案」P0/P1 红线 | **72 条**（P0 24 + P1 48） |
| 无「最终裁决方案」小节的文档 | 2 篇（应收应付调整、账款交账） |
| 未给出菜单 ID 的文档 | 3 篇（应收应付调整、账款交账、资产负债表） |

**红线落地核查（抽样）**：

| 红线 | 出处 | 实际 |
|---|---|---|
| 关账后禁记账 | 月结 | ❌ 仅租户 1 生效（§2.3） |
| 结转经凭证转 4103，不得直改科目余额 | 月结 | ⚠️ 结转有实现，但总账与分录不符（§2.1） |
| 预算三列为唯一事实来源，禁止直接改写 | 预算 | ❌ 可被 `PUT /item/{id}` 覆盖（§2.5） |
| 无重复号、无双轨 | 会计凭证 | ⚠️ 单号无并发保护；`KJPZ-` 与 `YYYYMM-XXXX` 两套口径并存 |
| 期末结转前校验借贷平衡 | 月结 | ✅ 已实现（BALANCE_CHECK 阻断） |

> **溯源断裂**：22 篇文档的「跨模块检查问题」表标注来源《跨模块关系检查报告.md》，但该报告（161 行）的审查范围原文是"仓储库内作业 5 单据 + 质量管理 3 页面 与 采购/销售/库存引擎" —— **「财务」二字零出现** ⇒ 财务的 P0→P2 表缺少可追溯来源。

### ⚠️ 三条使用基准时的注意事项（逐字核对 26 篇 + 4 篇菜单文档后得出）

**① 不要采信文档标题判断"金标准是否已升级" —— 至少 10 篇文档自相矛盾。**

以下文档在「配置功能说明」写 **"⚠️ 本页尚未升级金标准组件"**，在「页面对齐」小节又写 **"已升级/已建成金标准"**，同文内未消解：

> 付款单 · 收款单 · 预收款单 · 其他收入单 · 提现存现转款 · 待确认款项 · 按单付款 · 按单收款 · 月结 · 在线支付对账

（会计凭证、费用单两篇两处一致。）而前端**实测**的金标准覆盖是 `CategoryListLayout 26 / BillTableList 35 / PageConfigPanel 19 / ColumnConfigPanel 0`（52 个 .vue）——**并非 100%**，说明两处描述至少有一处是错的。

**② 菜单与权限的成文规范几乎不存在，这是"权限配好却落不到人"的制度根因。**

逐字核对 4 份菜单/跨模块文档后确认，以下规则**全部"未提及"**：`menu_code` 命名规则 · `path` 命名规则 · 组件路径是否带 `views/` 前缀 · **权限码从 menu_code 的派生规则** · 菜单 ID 号段规则 · 排序字段规范 · **SoD 职责分离** · **数据权限** · 按钮级权限码。

⇒ 主报告 §3.7 的"菜单码短横线(`finance:receipt-doc`) vs 权限码冒号(`finance:receipt:view`)对不上"，制度上就**没有规则要求它们对应**。
⇒ `sys_sod_rule` 0 行、`sys_data_scope` 0 行，与"文档零要求"互为印证。

**③ `menu_level=3` 是一个既无文档依据、UI 也不可编辑的取值。**

`mega-menu-redesign.md` 的字段注释是 `/** 菜单层级：0=租户级, 1=系统级 */`，菜单管理表单也只提供"租户级/系统级"两个单选项 —— **文档与 UI 都只承认 0 和 1**，而库中存在 `menu_level=3` 的 **127 条**（财务 17 条，即 §3.1 的 P1）。

**另：规划与现实存在差距** —— `mega-menu-redesign.md` 规划财务面板为「9 列 42 项」，库中实际 36 个叶子：
**资产管理列几乎未建**（规划 10 项，实际只有 `80147 资产总览` 1 个入口）；
费用管理规划 5 项、实际 2 项 —— 缺失的"费用申请/费用报销/费用支付"正好对应前端 `views/finance/` 下 3 个**空目录**（`expense-apply`/`expense-reimburse`/`expense-pay`），即目录已建、页面未写。

> 详见 `tool-results/finance-audit/doc-verbatim-findings.md`（逐字证据）。

---

## 六、冗余与死代码清单

| 类别 | 数量 | 说明 |
|---|---|---|
| 空目录 | 9 + 1 | 见 §3.5 |
| 死页面 / 孤儿组件 | 7 | 见 §3.5 |
| 死 api 方法 | 60 | 含 4 个整对象零引用 |
| 死代码整类 | 6 + 1 冗余配置 | `PaymentBusinessIntegrationService`、`InvoiceMatchingService(+Impl)`、`MatchingStatistics`(dto)、`PaymentRecordRepository`、`PaymentRecord`(invoice)、`InvoiceServiceConfig`(冗余) |
| 死亡子图方法 | 31 + 115 | 随上述类失效的方法 + 方法级零引用（Mapper `@Select` 32 / JPA 24 / 枚举 38 / 领域 11 / setter 10） |
| 僵尸权限码 | 5 | 与仓内既有清单一致 |
| 死表 | `erp_finance_subject` 等 | 见 §4.4 |
| 死 API 端点 | 多处 | 后端有、前端 api 零引用（`contract-compare.md` §4） |

**已明确排除的误报**（避免整改跑偏）：`SaleApprovalCallback` 式的框架分发组件、`@RequestBody` DTO、Jackson 序列化触发的 getter、`InitialStockQueryMapper` 式的设计内兜底 —— 详见 `backend-inventory.md` §3.4 / §4.3。

---

## 七、跨模块关系

### 7.1 财务 ← 销售 / 采购

| 链路 | 状态 |
|---|---|
| 销售出库 → 应收（`source_type=SALE_SHIPMENT`） | ⚠️ 建立成功，但**取消出库不冲销**（§3.8） |
| 采购入库 → 应付（`source_type=PURCHASE_RECEIPT`） | ⚠️ 3 行数据在，反向冲销未验证 |
| 出库「完成」双写凭证 | ❌ 销售侧问题（`SALES_MODULE_AUDIT` §2.1）：同一单据 3 张凭证（1 posted + 2 draft）⇒ 收入重复、成本结转停在草稿 |
| 对账中心 `reconcile` | ⚠️ 用**新增负数应收/应付**代替核销（`PartnerLedgerServiceImpl:309-335`），负数单会被账龄/核销报表当普通未结单统计 |

### 7.2 财务 ← 库存 / DMS

- DMS 结算：`dms_settlement` / `dms_settlement_item` 全 **0 行**；`SettlementService.generate` 从未被调用 ⇒ 配送结算在财务侧无数据。
- 库存：`erp_stock` 与财务无直接耦合，但**成本结转依赖它**，而库存两轨漂移（`STK-BREAK-01`）会传导到 6401/1403。

### 7.3 模块归属错位

`erp-finance` 内含映射到非财务资源的控制器；反向亦存在其它模块映射 `/finance/*` 的情况（详见 `backend-inventory.md` §6，含 `erp-budget` 前缀归属分析）。

**最典型的一处**：`AccountDeliveryController`（账款交账，`/api/erp/finance/account-delivery`）**代码位于 `erp-sales` 模块**
（`erp-sales/.../sale/controller/AccountDeliveryController.java:18,22`，注释明说"路由前缀 erp/finance 保持财务菜单语义；代码位于 erp-sales 复用销售单据数据"）。
⇒ 财务菜单点进去执行的是销售模块的代码，改财务要跨模块改。**同一现象在权限侧亦有痕迹**：`PermissionInitializationConfig.java:388-391` 仍登记着过期的 `/api/finance/receivable/**` 旧路径（与真实的 `/api/erp/finance/receivable/**` 不匹配，注释 `:384-385` 自认）。

### 7.4 依赖全景：财务是纯"被调用方"的内向模块

（本节依据三轮专项扫描，详见 `tool-results/finance-audit/cross-module-deps.md`。）

**① 谁依赖财务 —— 6 个模块，全部单向**

`erp-sales`（应收+凭证+预收款余额）· `erp-purchase`（应付+凭证）· `erp-marketing`（储值卡凭证）· `erp-fixed-asset`（折旧凭证）· `dms`（结算凭证+应付）· `core-api`（装配）。
**零命中**：`erp-stock`、`erp-partner`、`erp-pricing`、`erp-mall`、`erp-printing`、`wms`、`crm`、`hr` 等 13 个模块。

**② 耦合方式 —— 95% 是直连 Java Bean，编译期强耦合**

各模块统一走 `BusinessAccountingService` / `PayableService` / `ReceivableService` 门面。
**唯一越界**：`erp-purchase` 的 `UnifiedPurchaseDocQueryController.java:53,121,164,208` **在 Controller 层直接注入 `PayableMapper` 读写 `finance_payable`** —— 绕过了财务门面，意味着 P0 红线里"禁止绕过核销直改应付余额"在采购侧没有强制力。

**③ 表级边界基本清晰**

财务只读 **4 类外部表**：`biz_party`（结算档案算账期）、`erp_stock`（**仅**期初存货对平）、`erp_sale_pre_order`（回款统计）、`sys_user`/`sys_dept`/`sys_department`/`sys_config`。
**明确未引用** `erp_sale_order`、`erp_purchase_order`、`erp_sale_outbound`、`erp_purchase_inbound` 等业务单据表 ⇒ 财务与业务单据的耦合走 Java 层而非 SQL。
> ⚠️ 但 `CollectionStatsAnalyticsServiceImpl:194,301` 用 JdbcTemplate 字符串 SQL 直读 `erp_sale_pre_order` —— 销售侧改表结构会**静默**损坏财务的"回款统计"页，无编译期保护。

**④ finance / budget 自身不发起任何跨进程调用**

`RestTemplate` / `WebClient` / `HttpClient` / `FeignClient` / `@Scheduled` / `JobHandler` / `@EventListener` / `publishEvent` / `RabbitTemplate` / `KafkaTemplate` —— **两个模块全部为 0**。
（全仓 `@FeignClient` 0 命中；`KafkaTemplate`/`StreamBridge` 全域 0，本仓只用 RabbitMQ。）

**⑤ 架构上的"解耦"设计均未落地**

- DMS outbox 事件通道 —— 悬空（§3.12）
- `BusinessAccountingController`（`/api/erp/finance/integration`）—— 全仓（Java + 前端）**零调用方**，因为所有模块都选择直连 Bean

### 7.5 ⚠️ 关于子代理结论的采信纪律

`MASTER_TODO` 明确记载过教训：**"判断某表/某类无引用不能只搜 SQL 字符串或 `@TableName`，必须搜 Service/接口名；子代理报告中的'无引用/可删'结论一律需复核后才能落到删除动作。"**

本次报告中的死代码/死表结论**均已标注来源等级**：【实测】的（空目录、死页面路由判定、404 接口）可直接行动；【清点】的（零引用类、死表）**建议按"备份 → 观察期 → 改名 `zz_deprecated_*` → 再观察 → drop"流程处理**，不要直接删。

---

## 八、检查方法与复现

| 脚本 | 用途 | 本次结果 |
|---|---|---|
| `tools/e2e-finance-audit.cjs`（**新增**） | 打开 48 个财务菜单入口，收集失败请求 / console 错误 / 表格行数 | **48/48 正常，0 失败请求** |
| `tools/verify-finance-dashboard-links.cjs`（**新增**） | 逐个点击工作台快速入口，四重判定路由是否存在 | **4/6 落到 404** |
| `tools/audit-finance-contract.py`（**新增**） | 前后端接口契约双向比对（后端扫全仓 3118 个端点） | 前端调用 697 处、**断链 22**、方法不匹配 9、双前缀 0 |
| `psql` 只读查询 | 行数 / 字段填充率 / 总账 vs 分录对照 | 见 §2.1、§3.8、§4.4 |
| `./mvnw -o -pl erp/erp-finance test-compile` | 编译验证 | **BUILD SUCCESS** |

**运行时前提**：后端 `5655`、前端 `5656`（vite）；账号 `admin / admin123`（系统租户）。
> ⚠️ **会话陷阱（已规避）**：sa-token 单端登录，脚本**必须全程共用一个 context**。循环里重新 `login()` 会把前一次踢下线，导致成片的 401/500，让整份报告失真。

**本次产出的子报告**（`tool-results/finance-audit/`）：

| 文件 | 内容 |
|---|---|
| `backend-inventory.md`（1485 行） | 后端 8 张表：端点全清单 / 裸端点 / 实现质量 / 死代码 / 路径规范 / 模块归属 / 跨模块依赖 / 状态机 |
| `frontend-inventory.md`（1011 行） | 前端：页面清单 / 死页面 / API 引用 / 权限使用 / 金标准覆盖 / 未接通项 |
| `doc-spec.md`（895 行） | 26 篇文档的规格矩阵 + 105 条待完善 + 72 条红线 |
| `menu-permission-db.md`（1356 行） | 菜单树 / 权限码 217 条 / 僵尸码 / 闭环 / SoD 现状 |
| `contract-compare.md` | 前后端契约比对结果 |
| `cross-module-deps.md` | 跨模块依赖全景（反向依赖 6 模块 / 表级耦合边界 / HTTP·MQ·事件·定时任务 三向扫描） |
| `doc-verbatim-findings.md` | 26 篇文档逐字核对：自相矛盾 10 篇 · 规范空白 9 项 · 规划与现实差距 · 溯源断裂 |
| `e2e-result.md` / `dashboard-links-result.md` | 两次实跑的原始结果 |

---

## 九、建议的处理顺序

**第一批（P0，需财务口径拍板后动）**
1. 统一总账口径：加"按凭证重算 `finance_ledger`"入口 + 执行一次重算，全报表收敛到单一取数源（§2.1）
2. 记账失败一律抛异常（对照 §2.2 表格 10 处，参照同模块 4 处已做对的写法）
3. 租户 ID 全部改为会话取值（§2.3 的 8 处硬编码）
4. 预算三列改造为只读 + `@Version`（§2.5）

**第二批（P1，明确缺陷、不需拍板）**
5. ~~修 3 个实测断链（工作台 4 个跳转、辅助核算 enable、收款单 batch-print）~~ **✅ 已完成（2026-09-23，见「本轮修复记录」）**
6. 17 条 `menu_level=3` 菜单改回 0（§3.1）
7. 清空目录 + 死页面 + 对应 e2e 断言（§3.5）
8. 权限码补齐给功能角色（201 个仅超管）+ 菜单码短横线改冒号（§3.7）

**第三批（P2 + 收尾）**
9. 死表/死代码按"观察期 → 改名 → drop"流程处理
10. 补 SoD 规则（当前 0 行，属规范空白）、精简报告中的重复实现
11. **补齐菜单/权限的成文规范**（`doc-verbatim-findings.md` §2 列的 9 项空白：menu_code 命名、权限码派生、号段、排序、SoD、数据权限…）—— 不做这条，同类问题会随新菜单继续产生
12. 处置 §3.4 的随机指标与 §3.12 的悬空通道（一个误导决策，一个是死代码通道）
13. 把 `UnifiedPurchaseDocQueryController` 对 `finance_payable` 的直读直写收回财务门面（§7.4 ②）

---

*报告生成：2026-09-23 · 所有【实测】结论均附可复现命令，【读码】结论附 `文件:行号`，【清点】结论附扫描方法。*
