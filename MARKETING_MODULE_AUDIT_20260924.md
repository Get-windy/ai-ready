# 营销模块全面审计报告（2026-09-24）

**范围**
- 前端：`frontend/apps/pc-admin/src/views/marketing/**`（19 个 `index.vue` + `components/PromoActivityPage.vue`）+ `src/api/marketing.ts`（1756 行，92 个导出）
- 后端：`backend/erp/erp-marketing/src/main/java/cn/aiedge/erp/marketing/**`（24 个 controller、149 个端点）+ 营销页复用的 `erp-partner`（客户/会员建档）、`erp-stock`（商品图片）、`erp-mall`（弹窗广告/热门搜索词/拼团）
- 数据库：`devdb`（PostgreSQL）`sys_menu` / `sys_permission` / `sys_role_permission` + 营销域 28 张表 + `flyway_schema_history`
- 对照基准：`docs/Yh-Spec/手动整理对标开发文档/营销模块/*.md`（17 篇对标 + 2 篇本系统建模 + README）、`docs/Yh-Spec/手动整理对标开发文档/系统菜单设计与管理/*.md`

**维度**：可用性 · 规范性 · 冗余 · 死代码 · 功能接通 · 跨模块关系 · 权限配置
**口径**：三级证据 —— **【实测】**（本轮真机跑过，附命令与输出）· **【读码】**（逐一打开文件核对到行号）· **【清点】**（机械扫描，标注方法）

> ⚠️ 本仓反复踩过的坑，本次已规避并写进方法：
> ① **「E2E 全绿」不等于租户用户可用** —— 历次验收账号 `e2e_marketing`/`e2e_analytics` 都是 **SUPER_ADMIN 角色**，本轮专门用 `e2e_hr_ta`（SYSTEM_ADMIN，非超管）与 `e2e_hr_t2`（租户 2，非超管）复跑，当场炸出两个 P0；
> ② **未注册路由被 catch-all 承接** —— 页面可用性一律用四重检查（HTTP + 404 文案 + 主内容 + 表格/卡片元素 + console error）；
> ③ **子代理结论必须逐条复核** —— 本轮复核出 **2 条误判**（见 §8.2），已在本报告订正。

---

## 〇、摘要

| 级别 | 数量 | 代表问题 |
|---|---|---|
| **P0 致命** | 3 类 | **① 写入路径「租户盖章」两套口径**：同一会话下营销域写 `tenant_id=1`、资料域写 **0** ⇒ 会员/客户建档数据对自己租户不可见，且**直接卡死营销模块级 E2E**（前置守卫 0/1 通过）；**② 非系统租户看不到营销模块全部 19 页**（19 条菜单 `menu_level=3`，服务端对非系统租户只下发 `menu_level=0`）；**③ 非超管下 18/18 营销接口全 403**（105 个 `marketing:*` 权限码 **100% 只授 SUPER_ADMIN**） |
| **P1 严重** | 10 类 | 积分双轨（读旧表写新表，两表全 0 行）· 促销引擎试算端点**零前端消费者** · 券核销**两套实现**语义分叉 · 短信合规只做了一半（退订表 0 行、无上行回执） · 三个定时任务 `enabled=0` 且自动化台账零数据 · `StoredCardExpireJob` **无事务 + 吞异常** · 菜单码 `mkt:*` 与权限码 `marketing:*` **命名空间错位**（派生失效、fail-open） · 挂靠页 81004「积分流水」被塞进营销分组 · 4 个实体超 25 字段红线 · 10 处写操作复用了 `:create` 权限码 |
| **P2 一般** | 12 类 | 8 个零引用死 API 对象（含 `promotionEngineApi`）· 17 页逐字复制 `escapeHtml`、**0 页**复用既有 `utils/exportCsv` / `PrintDialog` · `member-config:440` 主区裸 `a-table` · 「我要推广」商品缩略图 404 · V3.2.0 幽灵表声明 8 张 · 零引用僵尸表 9 张 · 旧批次 7 张表无 `tenant_id`（其中 1 张在用、**已登记为已知豁免**）· 多张营销表缺业务唯一键与索引 · 迁移乱序执行已发生 · 文档未同步（发短信已是 4 Tab、建模页列数未登记）· 热词 20 条上限仅前端 · 按钮级权限全域为零 |
| **确认无问题的项** | 8 项 | 19 页骨架全过 · **逐页逐 Tab 列数与 ql361 对标 100% 一致** · 多 Tab 页 7 个全部独立 `storage-key`、无串用 · **149 个端点 0 个裸端点** · 权限码与 `sys_permission` 双向零差集 · 19 个 component ↔ 菜单 ↔ 文件三方对齐、**无孤儿页** · 无 TODO/桩/空 handler/假数据 · 菜单码在营销域无重复（无路由覆盖 404 风险） |

**整体判断**

营销模块的**页面做工与后端骨架是全仓最好的之一**：19 页统一路线 A/A′，**逐页逐 Tab 列数与 ql361 实测完全一致（21 组列配置无一偏差）**，多 Tab 页 27 个 `storage-key` 无一处重复，149 个端点全部带 `@SaCheckPermission`（**0 裸端点**），权限码与库双向零差集，无桩、无 TODO、无假数据，18/19 页真机干净（0 失败请求 / 0 console error）。**2026-09-18 那轮金标准落地是扎实的。**

但「**能不能用**」被三件事拦住了，且**全在页面之外**：

1. **租户盖章有两套口径，互相打架**。营销模块的每个写入控制器都自己 `tpl.setTenantId(SecurityUtils.getCurrentTenantId() == null ? 1L : ...)`；而资料域的客户/会员建档（`biz_party`）完全依赖 `MetaObjectHandler.insertFill` 自动盖章。本轮实测：**同一个 admin 会话**，POST `/erp/marketing/coupon-template` 落 `tenant_id=1`，POST `/erp/md/customer` 落 **`tenant_id=0`**（SQL 日志证实 INSERT 列清单里根本没有 `tenant_id`）。后果有两层：**① 会员管理页「新增会员/客户」建出来的人对自己租户不可见**（`biz_party` 里 tenant_id=0 的行从 2026-09-11 起持续累积，而最后一条 tenant_id=1 的客户停在 **2026-09-18 12:12**）；**② 营销模块的模块级 E2E 现在跑不起来** —— 套件第 0 节就是「造数租户归属」守卫，守卫不过就直接跳过全部 22 节业务套件（本轮实测 `0/1 通过`）。

2. **19 条菜单的 `menu_level` 全是 3**，而服务端对「非系统租户且非超管」强制只返回 `menu_level=0`。本轮用租户 2 账号实测：**营销 19 页下发 0 条**，只剩 5 个空分组挂在导航里，其中「会员中心」下只孤零零挂着一条 `81004 积分流水`（`member:` 域页面，被塞进了营销分组）。这与分析模块（已修 29 条）、DMS（已修 20 条）、财务（17 条）、HR 是**同一个坑**，营销是尚未修的其中之一。开发与验收一直用 `admin`（系统租户 + 超管，走早退分支不过滤），所以从未暴露。

3. **105 个营销权限码 100% 只授给了 SUPER_ADMIN**，其它角色（SYSTEM_ADMIN / DEPT_ADMIN）一条都没有。本轮用 `e2e_hr_ta`（SYSTEM_ADMIN，385 个权限码、系统租户）实测：**18 个代表端点全部 403**，包括会员管理、优惠券、促销、短信、储值卡、套餐、甚至跨模块复用的 `/erp/md/customer/member/page` 与 `/erp/product-kit/page`。**即：菜单救回来了，接口还是全 403。** 这是平台级角色设计问题（采购/销售/仓储/DMS/财务/交易/分析/HR 全都一样），不是营销独有，**不应按单模块缺陷去修**。

补一句关于「**积分体系从来没跑过**」：`mkt_points_exchange_product` 有 19 行商品、`mkt_product_points_rule` 有 22 行规则，但 `mkt_points_journal` / `mkt_points_batch` / `erp_sale_order_points_journal` **三张流水表全是 0 行** —— 积分只配不发。储值卡只到「建了 1 张卡 + 1 条流水」，营销自动化 `mkt_auto_campaign` 与执行台账**双 0 行**，短信退订名单 0 行。这些是**从未被真实使用**的功能，验收时不能被「E2E 全过」误导。

---

## 〇·补、本轮实测基线与复现方式

| 项 | 命令 | 本轮结果 |
|---|---|---|
| 模块级 E2E | `node tools/e2e-marketing.cjs` | ⛔ **0/1 通过** —— 第 0 节前置守卫失败（造数落 `tenant_id=0`），全部业务套件被跳过；日志 `tool-results/mkt-audit-e2e-baseline.log` |
| 19 页真机 UI | `node tools/audit-marketing-ui.cjs` | ✅ **18/19 页干净**；唯一问题页「我要推广」（商品缩略图 404） |
| 菜单下发 + 接口鉴权（超管/非超管/非系统租户） | `node tools/audit-marketing-runtime.cjs` | 结论见下表 |
| 写入路径租户盖章对照实验 | `node tools/audit-tenant-context.cjs` | 结论见 §2.1 |

`tools/audit-marketing-runtime.cjs` 实测矩阵（**本报告最重要的三行**）：

| 账号 | 角色 | 会话租户 | 营销菜单下发 | 营销接口探测 |
|---|---|---|---|---|
| `admin` | SUPER_ADMIN | 1 | **19/19** | 200 × 17（唯一 400 是探测脚本少传必填参数，非缺陷） |
| `e2e_hr_ta` | SYSTEM_ADMIN（非超管） | 1 | **19/19** | **403 × 18 / 200 × 0** |
| `e2e_hr_t2` | E2E_T2_ADMIN（非超管） | 2 | **0/19** | **403 × 18 / 200 × 0** |

> 读法：第 2 行说明「**菜单能看见 ≠ 能用**」（系统租户短路了 `menu_level` 过滤，但权限码没授）；第 3 行说明「**换成非 1 号租户，整块模块从导航里消失**」。

---

## 〇·补2、本轮修复记录（2026-09-26，已执行并逐项实测）

> 用户裁定：「代码开发阶段，堵点解决方案全部直接根因式解决」。据此对四个堵点做了**根因级**修复，全部先定位、后改动、再实测。

| # | 问题 | 根因（定位到行） | 改动 | 实测验证 |
|---|---|---|---|---|
| 1 | **`biz_party` 建档落 `tenant_id=0`**（§2.1） | **三层叠加**：① `insertFill` 对**没有标 `@TableField(fill=…)` 的普通字段**的赋值**永远进不了 SQL** —— MyBatis 在 `BaseStatementHandler` 里**先 `mappedStatement.getBoundSql()` 定稿 SQL**（MP 生成的 `<if test="et.tenantId != null">` 在此求值），**之后**才 `newParameterHandler()`，而填充发生在 `MybatisParameterHandler` 的构造参数求值里（**已用运行期 jar 反汇编双方字节码证实**）；② 唯一能兜底的父类 `processInsert` 被「超管整体豁免」**连同写一起跳过**；③ 全仓 **439 处显式 `setTenantId(...)`** 把①的危害掩盖了，`biz_party` 是**唯一只靠 fill** 的路径 | `AiReadyTenantLineInnerInterceptor.processInsert`：**把「读豁免」与「写盖章」拆开** —— INSERT 不再走 `shouldSkip()`，只要能解析出会话/临时租户就盖章（显式指定过的不覆盖）；`MyBatisPlusConfig` 原「自动填充」注释订正为真实作用范围 | `biz_party` 落库 `0 → 1`（`tools/audit-tenant-context.cjs`）；**营销 E2E 前置守卫由 `0/1` 解锁为全量跑通** |
| 2 | **非系统租户看不到营销 19 页**（§2.2） | 19 条菜单 `menu_level=3`，而 `SysMenuServiceImpl:277-278` 对「非系统租户且非超管」只下发 `level=0` | 新增迁移 **`V11.504.0`**：19 条 3→0（照搬分析 `V11.499.0` / DMS `V11.501.0` 范式） | 租户 2 账号菜单下发 **`0/19` → `19/19`**（`tools/audit-marketing-runtime.cjs`） |
| 3 | **非超管 18/18 接口 403**（§2.3） | 105 个 `marketing:*` 权限码 100% 只授 SUPER_ADMIN | 同一迁移：`SYSTEM_ADMIN ← 营销全量 105`、`DEPT_ADMIN ← 只读子集 50`；并同批补三页跨域码（`mall:popup-ad`/`mall:keyword`/`product:kit`）与「会员管理」页依赖的 `md:customer:*` | `e2e_hr_ta`：**`0×200 / 18×403` → `17×200`**（唯一 400 是探测脚本少传必填参数） |
| 4 | **【本轮新发现 P0】券被白扣：核销了但金额没抵扣** —— 端到端表现为 `券已核销=USED` 通过、`coupon_amount` 恒 `0` | `SaleOrderServiceImpl:302` 券核销**只按 `dto.getCouponIds()` 无脑遍历**，**从不校验促销引擎是否真的采纳了该券**；引擎的 `PromotionResult.appliedCouponIds` **全仓无任何消费方**。于是券被判不可用时（过期/未达门槛/非本客户/通道禁用/回填日期早于领券日），**券照样被核销、订单金额一分不减** | 改为**只核销引擎实际采纳的券**；传了券却有未被采纳的 ⇒ **整单 400 并把引擎给的跳过原因回给用户**（同事务回滚，不留已存单据）。附带修掉自己引入的一个时机 bug：`savePromotionDetails` 末尾会 `lastPromotionResult.remove()`，故促销结果必须**提前取出** | 回填日期单 → `400「券 FV…：尚未到生效日」`且**券未被扣**；当天单 → **`coupon_amount=30` / `bill_amount=70`**（修复前恒 `0`） |
| 5 | E2E 三处 **fixture 缺陷**（会把真缺陷掩盖成假失败/假绿） | ① 券用例硬编码单据日期 `2026-09-18`，早于当日领券日 ⇒ 必被引擎判「尚未到生效日」；② 掩码用例造 **10 位**手机号却断言 4 颗星（`DesensitizeUtils.mobile` 对 11 位号码输出 `137****XXXX`，实现本来就对）；③ 造数清理断言**未按本轮 `STAMP` 过滤** ⇒ 历史残留算成本轮未清理 | ① 券单改用当天日期；② 造 11 位号码；③ 清理断言按 `STAMP` 收口（并注明「不加 STAMP 会变成永远绿的假失败」） | 见下 |
| 6 | 数据卫生 | —— | 清掉 4 条 **2026-09-18 遗留**的 `E2E*` 草稿促销活动（历史失败跑没清干净） | 残留计数归 0 |

**回归结果**：`ERP_PORT=5691 node tools/e2e-marketing.cjs` → **378/378 通过，退出码 0**（修复前：前置守卫失败、`0/1`、22 节业务套件全被跳过）。
日志：`tool-results/mkt-audit-e2e-final.log`（修复前基线 `tool-results/mkt-audit-e2e-baseline.log`）。

> ⚠️ **生效边界（务必知悉）**：上述**代码改动（#1 / #4）目前只在源码与诊断实例 `5691` 上验证过**。
> 运行中的共享实例 `5655` 用的是 `2026-09-23 22:48` 构建的 `core-api-0.3.24-exec.jar`，
> **必须重新构建 `core-api` fat jar 并重启**才会生效（重启共享实例会影响并行会话，故未擅自执行）。
> **数据类改动（#2 / #3 / #6）已直接落库，立即生效**，无需重启（权限/菜单缓存 30s–5min 后自然刷新）。

---

## 一、已确认可用的部分（正面结论，收尾阶段可放心）

| # | 结论 | 证据 |
|---|---|---|
| 1 | **19 页骨架合规**：`ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable` + `StandardPagination variant="classic"`；`member-config` 是路线 A′ 参数页形态（`index.vue:9` 自注），符合 README §4.1 | 【清点】逐文件 `grep -c` 五件套；【读码】`member-config/index.vue:1-60,695`、`product-promo/index.vue:16`、`order-promo/index.vue:16` |
| 2 | **逐页逐 Tab 列数与 ql361 对标 100% 一致**（21 组列配置，含我要推广 6 Tab、优惠券/拼团/预售各 2 Tab） | 【读码】逐文件数 `columns` 条目并扣掉工具列；`member-manage` 反证（19−3=16，隐藏 6→10，恰等于对标 16/10） |
| 3 | **7 个多 Tab 页全部逐 Tab 独立 `storage-key`**，27 个键无一处重复、无串用 | 【清点】`grep -rh 'storage-key=' \| sort \| uniq -c` 全为 1；动态键见 `promote-create:264,167` |
| 4 | **后端 149 个端点 0 个裸端点**，全部方法级 `@SaCheckPermission`；无类级鉴权、无 `@SaIgnore` | 【清点】`grep -c` 逐文件比对（149/149）；另有 CI 门禁 `AuthzAnnotationCoverageTest` + 基线文件 `known-unauthorized-controllers.txt:256-259` 佐证营销域历史上从未欠账 |
| 5 | **权限码与库双向零差集**：代码用 100 个去重码，`sys_permission` 中 `marketing:%` 105 条，**无「代码引用但库里没有」、无「库里有但无人用」** | 【实测】`tools/audit-permission-codes.py` 口径复算 + SQL 直查 |
| 6 | **19 个页面目录 ↔ 19 条菜单 component ↔ `dynamicRoutes.ts:602-639` componentMap 三方一一对应**，无孤儿页、无缺文件、无缺登记 | 【实测】文件系统 + SQL + 读码三方交叉 |
| 7 | **无 TODO / FIXME / `console.log` / `debugger` / mock / 假数据**；无空函数 handler；无「按钮存在但无 @click」；无写死行数据 | 【清点】逐词 `grep -rn \| wc -l` 各 0；49 处 `console.error` 全在 catch 分支 |
| 8 | **营销域 `menu_code` 两两唯一、`route_name` 全空但派生路由名唯一** ⇒ 不存在分析模块那种「同名路由互相覆盖 → 静默 404」；`client_type` 全 `tenant-admin` + `visible=1` ⇒ 不被硬过滤误伤 | 【实测】全库 `menu_code` 重复 9 组全在 CRM/采购/仓储域，**营销 0 组** |

---

## 二、P0 致命问题

### 2.1 写入路径「租户盖章」两套口径 —— 营销域写 1、资料域写 0【实测】【✅ 已修 2026-09-26，见 §〇·补2 #1】

**现象**（`node tools/audit-tenant-context.cjs`，同一 admin 会话、连续四条写请求）：

```
账号=admin 会话租户（/auth/userinfo 读 session.tenantId）=1

❌ 客户（biz_party）· 无头                  HTTP=200 → biz_party.tenant_id=0
❌ 客户（biz_party）· 带 X-Tenant-Id=1      HTTP=200 → biz_party.tenant_id=0
✅ 优惠券模板（营销域，控制器显式 setTenantId） HTTP=200 → mkt_coupon_template.tenant_id=1
```

**SQL 级证据**（运行实例日志 `logs/backend-hr-verify-2.log:538042-538058`）：

```
original SQL: INSERT INTO biz_party  ( party_code, party_name,   party_type, settlement_type, status, roles,
                                       create_by, create_time, update_by, update_time ) VALUES ( ... )
```

⇒ **最终 SQL 的列清单里根本没有 `tenant_id`**。而同一条语句里 `create_time` 有值（`2026-09-24T08:11:33.726800900`），说明 `MetaObjectHandler.insertFill` **确实执行了**（`Party.createTime` 带 `@TableField(fill = FieldFill.INSERT)`，`party/entity/Party.java:215-219`），只是租户分支取到了 `null`。该表 `tenant_id` 列默认值 0，于是静默落到 0。

**代码侧根因（两套口径）**

| 口径 | 代表位置 | 取不到会话租户时 |
|---|---|---|
| **A. 控制器显式盖章**（营销域全套、商城域部分） | `PromotionEngineController`/`CouponTemplateController.java:65-68`/`StoredCardController`/… 共 `setTenantId` 类 | 硬编码回落 **`1L`** |
| **B. 依赖 `insertFill` 自动盖章**（资料域 `biz_party` 等） | `MyBatisPlusConfig.java:340-356` → `getCurrentTenantIdValue()` | 不写列 → **落列默认值 0** |

口径 A 的回落 `1L` 本身也是缺陷（`SecurityUtils.getCurrentTenantId()` 返回 null 时把数据写进 1 号租户，属 fail-open），后端代理在 4 处命中：`MemberLevelRuleController.java:110-113`、`PromoActivityController.java:44-47`、`SmsMarketingController.java:58-61`、`CommissionAnalyticsServiceImpl.java:76-79`。

**影响面（实测数据）**

- `biz_party` 中 `tenant_id=0` 的行**从 2026-09-11 起持续累积到本轮（09-24）**；`tenant_id=1` 的客户**最后一条停在 2026-09-18 12:12** ⇒ 该路径**已坏 6 天**，期间所有新建客户/会员对自己租户不可见。
- **营销模块级 E2E 被彻底卡死**：`e2e-marketing.cjs` 第 0 节守卫即为「造一条客户查库核对租户」，不过就 `process.exit` 跳过全部 22 节。本轮实测 `0/1 通过`（`tool-results/mkt-audit-e2e-baseline.log`）。
- 本次审计的探针行（id 307/308 等）已逻辑删除，但**历史上 17 行 tenant_id=0 的 `biz_party` 仍在库**（多为 E2E/DBG 残留，建议随本轮清理）。

**⚠️ 尚未完全定性的部分（如实标注，避免误导）**：同一 token 的 `/auth/userinfo`（`AuthController.java:357` 直读 `StpUtil.getSession().get("tenantId")`）**能读到 1**，紧接着的 INSERT 内 `getCurrentTenantIdValue()` 却解析为 null；且 `MyBatisPlusConfig$2.insertFill` 与 `getCurrentTenantIdValue` 的**运行期字节码已逐条反汇编比对，与源码一致**（无超管豁免分支）。**下一步定位只需三选一**：① 抓一次带完整 DEBUG 的写请求，看 `shouldSkip()` 与 `isLogin()` 的返回值；② 在 `insertFill` 加临时日志重打 jar 复现（需停共享实例，须先与其它会话协调）；③ 用非超管账号做同类建档（当前无任何非超管角色持有 `md:customer:create`，需先补授权）。

### 2.2 非系统租户看不到营销模块全部 19 页（`menu_level=3`）【实测】【✅ 已修 2026-09-26：`V11.504.0`，见 §〇·补2 #2】

**实测**（`tools/audit-marketing-runtime.cjs`，账号 `e2e_hr_t2`，租户 2）：

```
菜单下发：营销子页 0/19，营销分组节点 5/5（HTTP 200）
  分组 id：60008(营销), 60801(会员中心), 60802(营销活动), 60803(商城营销), 60804(营销推广)
```

**根因**（读码）：`core-base` `SysMenuServiceImpl.java:277-278`

```java
if (!isSystemTenant && !isSuperAdmin) { wrapper.eq(SysMenu::getMenuLevel, 0); }
```

`SYSTEM_TENANT_ID = 1L`（同文件 `:49`）；`buildMenuTree` 又**不剪枝空父节点**（`:413-424`），于是导航里留下 5 个点不开的空分组。

**库内事实**（SQL）：营销域 19 个叶子页 `menu_level` **全部为 3**；顶级/4 分组为 0。全库 `menu_level=3` 共 **65 条叶子**（财务 17、交易 11、设置 10、商城营销 6、营销推广 2…），营销是其中最大的一块。

**与文档直接冲突**：`系统菜单设计与管理/mega-menu-redesign.md` §6.3 明确定义 `menu_level TINYINT DEFAULT 0`（0=租户级 / 1=系统级），§6.7 把**营销列为「租户级菜单（`client_type='tenant-admin', menu_level=0`）」**；而 `系统菜单开发文档-营销模块菜单新增.md` §2 把 `menu_level=3` 写死进迁移 `V11.377.0`（「沿用同组既有值」）—— **是这份文档把既有 bug 固化成了 80303/80304 的默认值**，值 `3` 在两篇文档里从未被定义过。

**同类已修先例**：分析模块 29 条 3→0（`V11.499.0`）、DMS 20 条（`V11.501.0`）、CRM 17 条（`V11.500.0`）。**修法有现成范式，可直接照搬。**

### 2.3 非超管下 18/18 营销接口全 403（105 个权限码只授超管）【实测】【✅ 已修 2026-09-26：`V11.504.0` 授权，见 §〇·补2 #3】

**实测**（`e2e_hr_ta`，SYSTEM_ADMIN，385 个权限码，系统租户）：会员设置、积分兑换、优惠券、促销、发短信、推广历史、我要推广、营销自动化、储值卡、会员等级、积分批次、加价购、秒杀、预售、促销试算、会员管理（复用资料域）、弹窗广告（复用商城域）、套餐（复用资料域）—— **18 个端点 100% 403**。

**授权矩阵**（SQL 直查）：

| 指标 | 数值 |
|---|---|
| `sys_permission` 中 `marketing:%` | **105** |
| 其中授给 `SUPER_ADMIN` | **105（100%）** |
| 授给其它任何角色 | **0** |
| 加盟友域：`mall:%` | 52 条，同样**仅** SUPER_ADMIN |
| 加盟友域：`product:%`（套餐页复用） | 67 条，同样**仅** SUPER_ADMIN |

**Why 会一直没被发现**：库内 75 个用户里 42 个是超管（56%）；`e2e_marketing`、`e2e_analytics` 这些验收账号也全是 `SUPER_ADMIN` 角色 ⇒ **历次 E2E 全绿不代表真实租户用户可用**。这与 2026-09-22 采购审计、09-23 仓储/DMS/交易/HR/财务/分析审计的结论**同源**，属**平台级角色设计问题**，不是营销模块缺陷。

**How to apply**：谈这件事时先讲清是全系统口径，再谈角色矩阵（营销专员/店长/运营主管的读写子集）。**给营销角色授权的同一批**还要一并处理 `mall:*` 与 `product:*`（弹窗广告/热词/套餐三个页面跨域复用），否则菜单救回来、这三页仍然 403。

---

## 三、P1 严重问题

### 3.1 积分「读旧表、写新表」，两边都是 0 行【读码+实测】

| 角色 | 端点 | 落表 | 写入方 |
|---|---|---|---|
| **读**（会员管理 →「积分明细」弹窗） | `GET /erp/marketing/points/page` | **`erp_sale_order_points_journal`** | `erp-sales` `SaleOrderServiceImpl:1533` |
| **读**（新台账） | `GET /erp/marketing/points-ledger/journal/page` | `mkt_points_journal` | `erp-marketing` `PointsLedgerServiceImpl` |
| **写**（营销域积分动作） | `POST /points-ledger/earn|use|expire` | `mkt_points_journal` + `mkt_points_batch` | 同上 |

- `PointsJournalController.java:21` 的 javadoc 自述「数据源：`erp_sale_order_points_journal`（与销售订单会员信息 Tab 同源读写）」，SQL 在 `MarketingQueryMapper.java:194`。
- 但 `pointsLedgerApi.journalPage()`（`api/marketing.ts:1230` → `mkt_points_journal`）**前端 0 调用点** ⇒ **新台账没有页面读它**。
- 【实测】三张流水表 `erp_sale_order_points_journal` / `mkt_points_journal` / `mkt_points_batch` **全部 0 行**；`mkt_points_exchange_product` 19 行、`mkt_product_points_rule` 22 行 ⇒ **积分只配不发**，「积分有效期/FIFO/即将过期清单」这套 P1 能力**从未被任何真实数据走过**。

### 3.2 促销引擎试算端点零前端消费者（README 已自述待办）【实测】

- 后端**不是断头路**：`PromotionEngine` 确被销售下单主流程调用 —— `SaleOrderServiceImpl.java:106` 注入、`:759 promotionEngine.evaluate(...)`、`:802 recordUsage(...)`，券核销 `:305`、回滚 `:411` 与订单同事务。
- 但 **`promotionEngineApi`（`api/marketing.ts:1445` → `POST /erp/marketing/promotion/calc`）全前端 0 引用**（含 `views/sales/**`），README §6 也自述「仍待办：销售开单页接入试算展示」。
- ⇒ 端点可用（本轮实测 HTTP 200），但**开单页拿不到试算结果**，「促销试算展示」这一环在 UI 上是空的。

### 3.3 券核销两套实现，语义分叉【读码】

| 实现 | 端点 | 行为 |
|---|---|---|
| `promotion/CouponRedemptionServiceImpl.redeem`（P0 版） | `CouponTemplateController.java:152` `POST /coupon-template/record/{id}/redeem` | 写 status + usedOrderId + 来源单号、并发保护、维护模板 `usedCount`、可回滚 |
| `service/impl/LoyaltyCouponServiceImpl.useCoupon`（旧版） | `LoyaltyCouponController.java:56` `POST /coupon/{id}/use` | 仅 `UNUSED→USED` 简单翻转，无订单关联、无模板计数、无回滚 |

两套都对外暴露。旧版缺并发保护与回滚，一旦被调用会造成券状态与模板计数不一致（当前 `erp_loyalty_coupon` 44 行、`mkt_coupon_customer` 0 行，尚未暴露）。

### 3.4 短信合规链路只做了一半【实测+读码】

- 退订名单 `mkt_sms_opt_out` **0 行**、`mkt_sms_consent` 8 行、`mkt_sms_record` 14 行 ⇒ 合规四件套里的「退订」从未被真实触发过。
- README §7 自述「短信上行回执未接入（回复 R 目前靠人工登记）」⇒ **回复 R 不会自动进名单**，法规刚性要求（不得再发）在系统内无自动兜底。
- 发送入口仅两处：手工群发 `SmsMarketingController.java:280` + 自动化 `AutoCampaignServiceImpl.java:280`；**会员注册/订单完成/券到期均无短信触发**（候选全靠轮询 SQL `AutoCampaignServiceImpl.java:63-80`）。

### 3.5 三个定时任务默认熄火 + 自动化零数据【实测+读码】

- `V11.387.0__Marketing_Scheduled_Jobs.sql:25/38/51` 三行 `status='STOPPED'` + `enabled=0`，且自动化任务 `:50 execute_params='{"dryRun": true}'`；调度侧 `ScheduledTaskMapper:21` 只取 `enabled=1`、`TaskExecutor:70` 二次兜底，全仓无第二条 SQL 把它们改成 1。
- 【实测】`mkt_auto_campaign` **0 行**、`mkt_auto_campaign_log` **0 行** ⇒ 自动化营销页「规则/执行记录」双 Tab **从未有过数据**。
- 「默认停用 + 首次 dryRun」是**有意设计**（生产级做法），但**当前状态等于该能力整体未上线**，收尾时应明确是「保持停用」还是「上线前 dryRun 演练一次」。

### 3.6 `StoredCardExpireJob` 无事务 + 吞异常【读码】

```java
// job/StoredCardExpireJob.java:89 expireOneTenant() —— 无 @Transactional
storedCardMapper.update(null, ...)          // :96 置 EXPIRED
storedCardFlowMapper.insert(new StoredCardFlow() ...)  // :101 写流水
} catch (Exception e) { log.warn("[{}] 租户 {} 储值卡到期处理失败", KEY, tenantId, e); }  // :74 吞掉
```

⇒ 卡状态改了、流水没写（审计链断）；且失败只 `log.warn`，summary 不含失败数。同族问题：`MemberPointsExpireJob.java:75` 同样吞异常（其 `expireDue` 自身有事务，数据一致，但 summary 隐瞒失败）。**对照正向证据**：`StoredCardAccountingServiceImpl.java:111` 记账失败明确 rethrow、`CouponRedemptionServiceImpl.redeem` 全程事务 —— 说明本仓标准是「失败要炸」，这两个 Job 是例外。

### 3.7 菜单码 `mkt:*` 与权限码 `marketing:*` 命名空间错位【实测+读码】

- 菜单码命名空间是 **`mkt:*`**（19 条：`mkt:coupon`/`mkt:member-manage`…），API 权限码命名空间是 **`marketing:*:*`**（105 条）；`sys_permission` 中 `mkt:%` = **0 行**。
- `MenuPermissionDeriver` 按「权限码前缀集合」判定菜单码是否有对应权限码 ⇒ `mkt:*` **全部落空**，按 fail-open 口径**不被派生过滤**。
- **当前后果为零**（菜单丢失纯由 `menu_level=3` 造成），但它意味着：**营销菜单这 19 条永远是「没有权限码对应」的 fail-open 项**，一旦将来把派生口径改成 fail-closed，营销菜单会整块消失。属埋点式隐患，建议随 §2.2 一并统一前缀。

### 3.8 挂靠页 81004「积分流水」被塞进营销分组【实测】

```
id=81004  parent_id=60801(会员中心)  menu_name=积分流水  menu_code=member:points-history
path=member/points-history  component=views/member/points-history/index.vue  menu_level=0
```

- 这是 `member:` 域的页面，却挂在营销「会员中心」下；它还是**全树唯一 `menu_level=0` 的叶子** ⇒ 在租户 2 视图里，「会员中心」分组下**只剩它一条**，**其余 19 页全不可见**（实测）。
- 与《营销模块菜单新增》§5.3 的期望复核口径（`parent_id=60801` 期望 5 行）不符：实测 **6 行**。

### 3.9 四个实体超「≤25 字段」红线【读码】

红线出处：`docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md:1698`（主表 ≤25 列）。

| 实体 | 表 | 字段数 |
|---|---|---|
| `MarketingRule` | `erp_marketing_rule` | **32** |
| `PromoActivity` | `erp_promotion_activity` | **31** |
| `Presale` | `mkt_presale` | **28** |
| `MemberConfig` | `mkt_member_config` | **26** |

### 3.10 10 处写操作复用了 `:create` 权限码【读码】

`AddonRuleController.java:82/90`（enable/disable）、`AutoCampaignController.java:112/142`、`CouponTemplateController.java:97/127/152/162`（void/issue/redeem/void）、`StoredCardController.java:99/115/122`（recharge/refund/status）、`FlashSaleController`/`PresaleController` 的 cancel 等。**不是安全漏洞**（都有权限码），但粒度偏粗：**只有「查看权 + create 权」的角色就能执行退款、改状态、作废券**；且与 `sys_permission` 里登记的 path（只登记了 `POST /api/erp/marketing/addon-rule`）不一致。

---

## 四、P2 一般问题

| # | 问题 | 证据 |
|---|---|---|
| 4.1 | **8 个零引用的死 API 对象**：`loyaltyCardApi`/`loyaltyProgramApi`/`marketingRuleApi`/`promotionApi`/`gradePriceApi`/`campaignApi`/`searchHotApi`/`promotionEngineApi`（`api/marketing.ts:118/163/212/257/513/583/674/1445`） | 【实测】我方抽查 5 个，`api/marketing.ts` 之外引用数均为 **0**。⚠️ 其中 `promotionEngineApi` 是 §3.2 的**预留接口**（README 明写待办），**不建议删**，其余 7 个可清理 |
| 4.2 | **17 个页面逐字复制 `escapeHtml`**（md5 全为 `2bb00fac`），同族 `handlePrint`×17 / `handleF8Key`×17 / `fmtTime`×15（md5 全为 `ebdd4f0f`）/ `handleExport`×12；而 **营销模块 0 个页面**引用既有共享件 `utils/exportCsv.ts`（另有 23 个页面在用）与 `components/PrintDialog/` | 【清点】`grep -rl` + md5 判定；`grep -rn "utils/exportCsv\|components/PrintDialog" views/marketing/` → 空 |
| 4.3 | **14 处裸 `a-table`**：13 处在 `<a-modal>` 内（合规，弹窗内小表不进列表口径），**唯一例外 `member-config/index.vue:440`**（会员等级内嵌表在主区） | 【读码】逐文件 `awk` 回溯最近的 `<a-modal` |
| 4.4 | **「我要推广」商品缩略图 404**：`GET /api/erp/md/image/view/2098688802869096449` → 404，只因 `erp_product_image` 该行 `image_url IS NULL`（`deleted=0`）；前端未判空即渲染 `<img>` | 【实测】`tools/audit-marketing-ui.cjs` + `tool-results/mkt-audit/diag-promote.cjs` + SQL |
| 4.5 | **旧批次 7 张表无 `tenant_id`**：`erp_marketing_discount/freight/gift/rule_product/threshold/tiered` + `erp_group_buy_participant` | 【实测】`information_schema` 直查。⚠️ **订正子代理误判**：`erp_group_buy_participant` 已在 `MyBatisPlusConfig.java:87-90` **登记进 `IGNORE_TENANT_TABLES` 并写明理由**（`V11.30.0` 建表即无该列，租户归属由主表决定），属**已知且有据的豁免**，不是漏网缺陷 |
| 4.6 | **8 张 V3.2.0「幽灵表」**：`erp_marketing_rule_partner`/`erp_promotion_channel`/`erp_promotion_log`/`erp_promotion_settlement`/`erp_share_activity`/`erp_share_commission_detail`/`erp_share_relation`/`erp_settlement_cycle` —— 仅 V3.2.0 有 CREATE，该版本号低于 Flyway baseline，**本库从未执行**，全库无 DROP | 【实测】`information_schema` 直查 + `flyway_schema_history` 无该版本 |
| 4.7 | **9 张零代码引用僵尸表**：上面 6 张 `erp_marketing_*` + `erp_commission_settlement_config` + `erp_supplier_points_rule` + `mall_category` | 【清点】表名词边界正则扫 `backend/` + `frontend/`（java/xml/yml/sql/ts/vue），已人工二次复核 |
| 4.8 | **业务唯一键/索引缺口**：`mkt_coupon_template`/`mkt_sms_template`/`mkt_auto_campaign`/`mkt_coupon_customer`/`erp_promotion_activity`/`erp_member_level`/`mkt_points_batch` **均无业务唯一键**（券名/模板名/活动名可重复建，`is_default` 可多真，批次无「来源单号」幂等键 ⇒ 定时任务重跑会重复发积分）；`erp_loyalty_coupon.code`、`erp_loyalty_card.card_code` 唯一键**不含 `tenant_id`** ⇒ 跨租户撞车；`erp_promotion_activity`（109 行且在增长）**除主键外无任何索引**，而促销引擎每次下单都按时间窗过滤它 | 【实测】`pg_indexes` 直查 |
| 4.9 | **Flyway 乱序执行已发生**：`installed_rank` 388=11.387.0、389=11.388.0、**390=11.391.0、391=11.392.0、392=11.389.0** ⇒ 说明 `outOfOrder` 已开（或 `validateOnMigrate=false`），**补低位号迁移不会再被拦下**，配号风险上升 | 【实测】`flyway_schema_history` 直查。好的一面：496 个迁移文件**无重复版本号**、**0 条 `success=false`** |
| 4.10 | **文档未同步**：① 发短信页已实为 **4 个 Tab**（send/history/template/optout，`sms-send:611-617`），README §2/§4.1 仍写「3 Tab」「无列配置弹窗」，且 §2 完全没收录短信的 7/7、4/4 列数；② `auto-campaign`/`stored-card` 两个建模页的列数（12/9、12/11）未登记进 §2；③ `mega-menu-redesign.md:1321,2224` 仍写「营销 4 列 **17** 项」，实为 19 | 【读码】逐文件比对 |
| 4.11 | 热词条数上限 `MAX_KEYWORDS = 20` **仅前端实现**（`hot-keywords/index.vue:197`），后端 `MallAdminKeywordController` 无条数校验 ⇒ 直连 API 可越过 | 【读码】两侧对照 |
| 4.12 | **按钮级权限全域为零**：营销 105 条权限码 `permission_type` **全为 3(API)**，`sys_menu` 中营销 `menu_type=2` **0 行**，营销视图 `v-permission`/`hasPermission` **0 命中**（全站其余 385 处）⇒ 页面内所有增删改入口对任何持页面访问权的用户全开 | 【实测】SQL + `grep -rn` |

---

## 五、数据库小结

**营销域自有表 28 张**（`mkt_*` 22 + `erp_loyalty_*` 3 + `erp_group_buy_*` 2 + `erp_promotion_activity`）。

**结构合规面（好）**：`V11.369.0 ~ V11.388.0` 新建的 18 张表**全部有 `tenant_id NOT NULL`**，`CREATE TABLE / ADD COLUMN / CREATE INDEX` **全部带 `IF NOT EXISTS`**（41 处 ADD COLUMN 无一处裸写），无重复版本号、无失败迁移。

**数据现状（关键解读）**：真实有生命力的只有三块 —— **通用促销活动**（`erp_promotion_activity` 109 行 + `erp_sale_order_promo_detail` 72 行）、**券模板/券实例**（`mkt_coupon_template` 53 / `erp_loyalty_coupon` 44）、**商城配置类**（`mall_product` 85 / `mall_keyword` 35 / `mall_popup_ad` 16 / `mall_notice` 2）。

**零行表 = 从未被真实使用的功能**：`mkt_points_journal`、`mkt_points_batch`、`mkt_coupon_customer`、`mkt_auto_campaign`、`mkt_auto_campaign_log`、`mkt_flash_sale_order`、`mkt_presale_order`、`mkt_sms_opt_out`、`erp_sale_order_points_journal`、`erp_loyalty_program`、`erp_loyalty_card`、`erp_marketing_rule`、`erp_group_buy_participant`、`erp_commission_*`、`erp_user_balance`/`erp_balance_log`/`erp_withdraw_request`、`mall_order`/`mall_cart`/`mall_address`/`mall_category`。

**结构性重复（语义重叠，非命名重复）**：`mkt_points_journal` ↔ `erp_sale_order_points_journal`（§3.1）；`mkt_coupon_template`+`mkt_coupon_customer` ↔ `erp_loyalty_coupon`+`erp_loyalty_program`（V11.376 放宽 `program_id` 非空 = 正式放弃与 `erp_loyalty_program` 绑定，后者成孤儿）；`erp_group_buy_activity`（拼团，实体在 **erp-stock** 模块）↔ `erp_promotion_activity.promo_mode/combo_promo`。

---

## 六、跨模块关系小结（是否正确）

| 关系 | 判定 | 证据 |
|---|---|---|
| **营销 → 销售开单**（促销引擎/券核销） | ✅ **已接通**，非断头路 | `SaleOrderServiceImpl.java:106/759/802`（evaluate/recordUsage）、`:305` 券核销、`:411` 回滚，与订单同事务；分摊落 `erp_sale_order_promo_detail`（72 行） |
| **营销 → 销售开单**（试算展示） | ❌ **未接通** | `/promotion/calc` 前端 0 调用（§3.2） |
| **营销 会员管理 → 资料域 `biz_party`** | ⚠️ **接通但盖章错** | 会员=客户扩展档案，走 `POST /erp/md/customer` ⇒ 落 `tenant_id=0`（§2.1） |
| **营销 → 商城域**（弹窗广告/热词/拼团） | ✅ 接通 | 复用 `/erp/mall/admin/popup-ad`、`/erp/mall/admin/keyword`；拼团活动实体在 `erp-stock`（**模块归属错乱**，属历史遗留） |
| **营销 → 财务**（储值卡记账） | ✅ 闭环完整 | `StoredCardAccountingService` 4 个调用点（开卡/充值/消费/退款）全在事务内，凭证与余额同事务、失败整笔回滚、`flow.voucher_no` 幂等；无任何绕过记账的余额更新 SQL |
| **营销 → 分析模块** | ✅ 单向读 | `views/analytics/mkt-promote-analysis` 引 `couponApi/groupBuyApi/flashSaleApi`；`erp_sale_order_promo_detail` 是分析域「营销活动分析」的数据源 |
| **营销 → 会员域** | ⚠️ 菜单挂错 | 81004「积分流水」挂进 60801（§3.8） |
| **营销 → 定时任务底座** | ✅ SPI 正确，但**默认停用** | 三个 Job 均 `@Component` + `JobHandler.key()`，与 `JobHandlerRegistry:38-54` 白名单一致（重复 key 会启动失败）；`enabled=0` 见 §3.5 |

---

## 七、方法与脚本（可复现）

| 脚本 | 用途 |
|---|---|
| `tools/audit-marketing-runtime.cjs` | 三个账号（超管 / 非超管 / 非系统租户）实测**菜单下发数 + 18 个营销端点状态码**矩阵 |
| `tools/audit-marketing-ui.cjs` | 19 页真机 UI 四重检查（HTTP + 404 文案 + 主内容 + 表格/卡片 + console error） |
| `tools/audit-tenant-context.cjs` | 写入路径租户盖章**对照实验**（同会话 × 不同实体 × 带/不带头） |
| `tool-results/mkt-audit/diag-promote.cjs` | 「我要推广」页失败请求定位（列 HTTP≥400 + console error + requestfailed） |
| `tools/apply_sql_migration.py` | **把一个 Flyway 迁移直接应用到 devdb**（幂等迁移可免重启即时生效，不打断并行会话） |
| `tools/check-stale-classes.py` | **ECJ 残缺类闸门**（扫 fat jar 10 万级 class + 指定目录；命中即退出 1）。已接进 `tools/build-backend.sh`，打包后启动前自动拦 |
| `tools/run-second-instance.sh` | **不重启共享实例就验后端改动**：`compile` 单文件到独立目录（绕开 IDE 覆盖 target/classes）/ `start` 起第二实例 / `stop`。封装了 `loader.path` 逗号分隔等三个坑 |
| `tools/dbq.py` | 库查询（**SQL 必须单行**，多行会被 cmd.exe 截断静默丢 WHERE） |
| 既有 | `tools/e2e-marketing.cjs`（模块级 E2E，当前被 §2.1 卡死）、`tools/audit-permission-codes.py`、`tools/check-menu-targets.py` |

### 7.1 子代理结论的本轮复核记录（2 条误判已订正）

| 子代理结论 | 复核结果 |
|---|---|
| 「`/api/sms/config`、`/api/sms/test`、`/api/search/hot` 三个 URL 后端全仓不存在」 | **误判 2/3**。`SmsConfigController`（`@RequestMapping("/api/sms")`）确有 `/config`(GET/POST) 与 `/test`；`SearchController`（`@RequestMapping("/api/search")`）确有 `/hot`。三条 URL **都存在**，只是 `smsApi`/`searchHotApi` 两个前端对象**无人调用**（属死代码，不属断链） |
| 「`erp_group_buy_participant` 无 `tenant_id`，多租户下必然串租（硬缺陷）」 | **订正**：该表已在 `MyBatisPlusConfig.java:87-90` 登记进 `IGNORE_TENANT_TABLES` 并写明理由（建表即无该列、租户归属由主表决定），属**已知且有据的豁免** |
| 其余关键结论（149 端点 0 裸端点、105 码只授超管、19 页 `menu_level=3`、8 个死 API 对象、4 个实体超 25 字段、14 处裸 `a-table`、17 页 `escapeHtml` 重复、`StoredCardExpireJob` 无事务+吞异常、积分双轨、菜单 81004 挂错） | **逐条复核属实**（本报告正文已附各自的行号与实测输出） |

---

## 八、处置建议（按批次，标注是否需先确认）

### 第一批（✅ 已于 2026-09-26 执行，详见 §〇·补2）

| # | 项 | 状态 |
|---|---|---|
| 1 | `menu_level` 19 条 3→0（§2.2） | ✅ 迁移 `V11.504.0` + 直接落库；租户 2 复验 `19/19` |
| 2 | 权限授权：营销全量给 `SYSTEM_ADMIN`、只读子集给 `DEPT_ADMIN`（§2.3） | ✅ 同迁移；`e2e_hr_ta` 复验 17×200 |
| 3 | 写入路径租户盖章（§2.1） | ✅ `processInsert` 拆分「读豁免/写盖章」；`biz_party` 复验落 1 |
| 4 | 券核销以引擎采纳为准（新发现 P0） | ✅ 只核销 `appliedCouponIds`；不可用则整单 400 带原因 |
| 5 | E2E 三处 fixture 缺陷 + 历史残留清理 | ✅ 复跑 **378/378** |
| 6 | **`StoredCardExpireJob` 补 `@Transactional` + 失败上抛/计入 summary**（§3.6） | ⏳ 未做（纯代码改动，只影响 Job，无阻塞性） |
| 7 | **文档回写**（§4.10）—— 发短信已 4 Tab、两个建模页列数、`mega-menu-redesign` 17→19 | ⏳ 未做 |
| 8 | **重新构建 `core-api` fat jar 并重启 5655**，让 #3/#4 的代码改动在共享实例生效 | ⏳ **需你点头**（重启会影响并行会话） |

### 第二批：需要先定口径再动

4. **`biz_party` 建档盖章**（§2.1）—— 先定位「同一会话两处读租户结果不一致」的机制（三条定位路径见 §2.1 末），**在没定性前不要改**；定性后二选一：① 让写入路径显式盖章（口径 A 统一）；② 修会话读取。同时应把口径 A 里 4 处硬编码回落 `1L` 改成**报错或显式取会话**（fail-closed），否则「取不到租户就写进 1 号租户」还会复发。
5. **营销角色矩阵**（§2.3）—— 属平台级角色设计，建议与采购/销售/仓储/DMS 一次性统一；营销侧最小集：`SYSTEM_ADMIN` 获全量、`DEPT_ADMIN` 获只读子集；**同批**处理 `mall:*`（52）与 `product:*`（67）中营销三页用到的部分。
6. **积分双轨收敛**（§3.1）—— 决定「积分明细」以 `mkt_points_journal` 为准（新台账）还是反过来；收敛前**不要把积分能力宣传为可用**。
7. **券核销两套实现**（§3.3）—— 废弃 `LoyaltyCouponController.useCoupon` 或让它委托到 `CouponRedemptionService`。
8. **三个定时任务是否上线**（§3.5）—— 明确「保持停用」还是「dryRun 演练一次后启用」。

### 第三批：收尾清理（可与第一/二批并行）

9. 删 7 个死 API 对象（保留 `promotionEngineApi`，§4.1）；抽公共 `usePrintExport` 组合式替换 17 页复制（§4.2）。
10. 图片判空（§4.4）、热词上限后端补校验（§4.11）。
11. `mkt_*` 缺唯一键/索引补齐（§4.8，**注意 `erp_loyalty_coupon.code` 唯一键要加 `tenant_id`**）；迁移乱序配号规则明确（§4.9）。
12. 僵尸表/幽灵表登记（§4.6/4.7）—— 建议在 `MASTER_TODO` 里登记「零引用表清单」，不急于 DROP。
13. 清理历史遗留的 17 行 `biz_party.tenant_id=0` 探测数据（含本轮 3 行，已逻辑删除）。

---

**报告人**：AtCode（2026-09-24）｜**证据模式**：全部结论可复现，脚本见 §7 ｜**未修改任何业务代码与数据库结构**（§8 第一批起需你确认后执行）
