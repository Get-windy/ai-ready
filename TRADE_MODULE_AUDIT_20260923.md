# 交易模块全栈审计报告（2026-09-23）

**范围**：交易域 = 商城（22 页，5 分组）前端 `frontend/apps/pc-admin/src/views/{trade,mall,payment}/**` + C 端 `frontend/apps/mobile-mall/src/**`、后端 `backend/erp/erp-mall`（113 端点）+ `backend/core/base/core-base/.../trade`（45 端点，其中 `monitor/**` 13 个属配送模块）+ `backend/core/payment/core-payment`（31 端点）、数据库 devdb 交易相关 25 张表 + 视图 `v_mall_product`。

**对照**：`docs/Yh-Spec/手动整理对标开发文档/交易模块/*.md`（README + 22 篇页文档 + `_开发指南-金标准.md`）、`docs/Yh-Spec/手动整理对标开发文档/系统菜单设计与管理/*.md`。

**维度**：可用性 · 规范性 · 冗余 · 死代码 · 功能接通 · 跨模块关系 · 权限配置。

**方法与可复现脚本**（本轮新建，均只读）：

| 脚本 | 作用 |
|---|---|
| `tools/audit-trade-module.py` | 后端端点 ↔ 前端调用双向比对、鉴权注解覆盖、权限码清单（`--links/--unused/--perms/--codes`）。**既有脚本，本轮修了 3 处口径**（鉴权向前窗口跨方法、断链判定改全后端集合、前端调用池按页面 import 补齐），修正前后差异见脚本内注释 |
| `tools/probe-trade-audit.cjs` | 真机探针（匿名/登录态、C 端、管理端、开放 API）。既有脚本，本轮复跑 |
| `tools/audit-trade-deadcode.py` | 后端类零引用扫描（区分 Spring 组件/普通类）。**本轮新建** |
| `tools/e2e-trade.cjs`（既有） | 模块级 E2E，本轮实跑结果见下 |

**台账**：`tool-results/trade-audit-e2e-20260923.txt`（E2E 全文 102/103）、`tool-results/trade-audit-perms.txt`（裸端点 67 条）、`tool-results/trade-audit-unused.txt`（无人调用候选 23 条）。

> ⚠️ **口径修正提示**：若你此前用旧版 `audit-trade-module.py` 得到过「65 条裸端点 / 断链若干」的结论，请以本报告为准 —— 旧版有 2 条假阴性（漏掉支付/退款回调）与 7 条假阳性（把跨模块接口误报为断链）。

---

## 〇、摘要

| 级别 | 数量 | 代表问题 |
|---|---|---|
| **P0** | 5 类 | 买家账号/买家申请两页**必然 500**（E2E 唯一失败项）· 支付回调**不验签且渠道全为桩**（可伪造「已支付」）· 外部平台**整体空壳**且开放 API 对外 401 · 模块对**非超管完全不可用**（菜单不可见 + 接口 403）· 商城 C 端订单接口**无归属校验**（IDOR + 买家自审） |
| **P1** | 9 条 | 商品上架新增/删除写残留表（保存/删除后列表不变）· 5 处「只写不读」的空开关（运费/装修/公告/关键词/单位显示）· C 端物流轨迹恒空 + 支付入口白屏 · 订单号进程内计数器 · `tenant_shop_config` 82 列 god table |
| **P2** | 8 组 | 前端 7 个死接口 · 2 个死实体类 · 3 张僵尸表 · 购物车「清空」语义错位 · 公告接口自称公开实则需登录 · 1 个僵尸权限码 等 |
| **健康项** | — | 22 页菜单组件 0 缺失 · 管理端 111 端点 109 个有鉴权注解 · 商品上架已改走权威视图 · 迁移无重号 |

### 与其它模块审计的交叉印证（避免重复排查）

本轮有 2 条结论与同期其它模块的审计**独立撞车**，说明是**系统级问题而非交易模块独有**：

- **`/erp/mall/admin/user/page` 拆箱 NPE 500** —— `ANALYTICS_MODULE_AUDIT_20260923.md` 也报了这一条（该页被分析模块的入口间接引用）。
- **`OpenApiController` 12 端点无鉴权 + 自称签名校验但 `signature` 从未使用** —— `DMS_MODULE_AUDIT_20260923.md` 独立报了同一组端点（同一份代码，交易侧看是「外部平台」，配送侧看是「API 监控」）。
- **「权限码只授 SUPER_ADMIN」已是第 N 例** —— 采购（2026-09-22）、仓储、DMS、财务、分析、交易均命中。建议**不要按模块逐个修**，而应作为一条全局任务处理（见 §五③）。

**整体判断**：交易模块的**管理端骨架是通的**（E2E 102/103，UI 抽样 6 页 0 console error），但**业务闭环大面积缺失**：

1. **商城（C 端）从未被真实使用过** —— `shop_user` / `shop_user_tenant` / `mall_cart` / `mall_address` / `mall_order` / `erp_sale_order(order_source IN (2,3))` **全部 0 行**，C 端支付链路只有接口定义没有调用方。
2. **「商城设置」6 页 + 「商城管理·单位显示」的配置项没有消费方** —— `tenant_shop_config` 82 个字段只有 **4 个**被运行期读取（其余 78 个只写不读）。
3. **「外部平台」3 页 + 开放 API 是空壳** —— 适配器返回硬编码假数据、同步是「模拟结果」、回调验签被注释掉。
4. **支付结算是独立账本** —— 渠道实现全为 TODO，验签器是死代码，且与总账/应收无任何联动。
5. **权限只授超管** —— 91/91 权限码仅关联 `SUPER_ADMIN`，实测非超管账号全部 403 且菜单树为空。

> **一句话**：交易模块当前是「**管理端 CRUD + 大量只写不读的配置 + 一套未接线的 C 端/支付/外部平台接口**」。若要变成**能用**的模块，需要先拍板「商城到底做不做」（见 §五）。

---

## 〇之二、本轮已执行的修复（2026-09-23）

> 口径：只做**无业务口径歧义**的修复（bug 修 + 安全兜底 + 死代码清理 + 删假实现）。凡涉及产品/账务口径的（§五 五项、以及 P0-2/P0-3 的真实对接）**一律未动**。

| # | 对应条目 | 改动 | 文件 |
|---|---|---|---|
| 1 | **P0-1** | 修拆箱 NPE：`? Integer.valueOf(ENABLED_YES) : status`，语义与原意图完全一致（`showDisabled=false` → 强制启用；否则用 `status`，null 即不过滤） | `erp-mall/.../service/impl/MallAdminServiceImpl.java:225` |
| 2 | **P0-5** | 新增 `requireMyOrder(id)`（归属校验），`getOrderDetail` / `cancelOrder` / `confirmOrder` 全部改走它；归属口径 = `会话激活身份 ?? shop_user.partyId ?? shop_user.id`，**与 `listOrders` 的 `queryPartyId` 同一处逻辑抽出**，杜绝"列表看得到、详情说无权" | `erp-mall/.../service/MallOrderServiceImpl.java` |
| 3 | **P0-5 / P2-5** | **删除**买家端的 3 个端点：`POST /orders/{id}/pay`、`PUT /orders/{id}/approve`、`PUT /orders/{id}/reject`（含 controller + service 接口 + 实现）。理由：仅需登录、不校验归属（未归属校验前可自审/置已付）、`payOrder` 不产生支付记录等于"白拿单"通道、管理端已有带权限码的等价实现、两个前端应用零调用。将来做 C 端支付应走 `core-payment.createPayment` + 渠道回调驱动状态 | `MallOrderController.java` / `MallOrderService.java` / `MallOrderServiceImpl.java` |
| 4 | **P0-3D** | 删除 `TaobaoChannelAdapter.pullOrders` 的**硬编码假订单**（`TB_ORDER_001`/淘宝买家/张三/13800138000），改为返回空列表 + WARN 日志；`isConnected()` 由恒 `true` 改为 `false`（未接 SDK 就没有连接） | `core-base/.../trade/channel/TaobaoChannelAdapter.java` |
| 5 | **P0-2D** | 更正 `api/payment/index.ts` 里与实现相反的注释（原文称回调会 fail-closed 拒绝，实际是**不验签当成功**），写明真实口径与"配了也不生效" | `frontend/.../api/payment/index.ts:43-60` |
| 6 | **P2-1** | 删除前端死接口 `mallReturnApi`（7 个函数，全仓零引用，后端无 `/erp/mall/admin/return/*`）及配套 `MallReturn` 类型 | `frontend/.../api/erp/mall.ts` |
| 7 | **P2-2** | 删除两个死实体 `MallOrder.java` / `MallOrderItem.java`（Java 零引用、互相引用的孤岛，映射 V9.0.0 已标注废弃的 `mall_order`/`mall_order_item`） | `erp-mall/.../model/` |
| 8 | **P1-8** | 移除购物车页的「清空购物车」按钮及其 `mallCartApi.clearCart()`（该端点按**调用者本人**清空，对管理端列表无影响）；清空需求由"勾选行 → 批量删除"覆盖 | `views/trade/cart/list.vue` / `api/erp/mall.ts` |

**静态验证**：
- 后端 `mvn -o -pl erp/erp-mall -am package -DskipTests` → **BUILD SUCCESS**（core-base 48.7s + erp-mall 19.5s）；
  下游 `mvn -o -pl erp/erp-sales,core/payment/core-payment -am compile` → **全部 SUCCESS**（core-base 改动未破坏任何下游）；
  全应用 `mvn -o package -DskipTests -pl core/api/core-api -am` → **BUILD SUCCESS**。
- 前端 `eslint`（改动文件）→ **0 error**（50 warning 均为改动前既有的 `no-explicit-any`/`eqeqeq`）；
  `vue-tsc --noEmit` 全量 → **改动文件与整个交易域 0 报错**（其余报错在 `wh/ wms/ workflow/`，属既有问题）。
  ⚠️ 该检查只成功跑完一轮：复跑时因**并行会话正在删改 HR 文件**（`views/hr/attendance/index.vue` 在检查过程中消失）而报 `TS6053 File not found` 提前中止 —— 与本次改动无关，故不以其为结论；
  `vite build` → **✓ built in 6m 3s，退出码 0**。

**真机复验（独立实例 :5691，不干扰其它会话在用的 :5655）**：

> 做法：`mvn -o package -pl core/api/core-api -am` 出 fat jar → 复制到私有路径 → `java -jar … --server.port=5691`（与共享实例同 cwd、同 dev profile）→ 用**专用验收账号** `e2e_tradefix`（SUPER_ADMIN，避免与他人账号互踢）跑验收。

| 验收项 | 修复前（旧 jar） | 修复后（:5691） |
|---|---|---|
| `node tools/e2e-trade.cjs` | **102/103**（唯一失败项 = 买家账号 500） | **103/103 ✅** |
| `/erp/mall/admin/user/page` 4 种参数组合（含不传 `status`） | 3 组 **500** | **4 组全部 200 ✅** |
| `POST /v1/mall/orders/{id}/pay`、`PUT …/approve`、`PUT …/reject` | 存在且可调（仅需登录） | **404 接口不存在 ✅** |
| 读**他人**订单详情 `/v1/mall/orders/{他人单}` | 200（含收件人/电话/地址） | **403 无权访问该订单 ✅** |
| 读**自己**订单详情 | 200 | **200 ✅**（无误伤） |
| 取消 / 确认收货 **他人**的订单 | 200 | **403 无权访问该订单 ✅** |

> 后四项用两张临时单（`AUDITTEST-ORPHAN` customer_id=1、`AUDITTEST-MINE` customer_id=调用者）验证，**测完已删净**（残留 0 行）。
> 复验用的独立实例已关闭、私有 jar/日志已删、专用账号 `e2e_tradefix` 三张关联行已清除（残留 0）。

**遗留（未做，需你确认）**：
- 运行中的共享后端（:5655）仍是**旧 fat jar**（它由其它会话以 `tool-results/system-module/core-api-mine.jar` 启动）。要让修复在该实例生效，需**由该实例的属主**重新打包重启（`tools/build-backend.sh`，含 `clean`——`target/` 里还残留已删类的 `.class`，Maven 不删已删源文件的产物）。我未擅自重启他人的实例。

---

## 一、P0（阻断级）

### P0-1 【实测 500】买家账号 / 买家申请管理 两页打开即 500 —— 三元运算符拆箱 NPE　✅**已修（见 §〇之二 #1，真机 103/103）**

**症状**：`GET /api/erp/mall/admin/user/page` 恒 500「系统异常，请稍后重试」。因前端默认勾选「显示停用」→ 传 `showDisabled: undefined`（即不传），后端 `status` 参数为 `null` → 立即 NPE。

**根因**（`backend/erp/erp-mall/.../service/impl/MallAdminServiceImpl.java:225`）：

```java
Integer enabledFilter = Boolean.FALSE.equals(showDisabled) ? ShopUserTenant.ENABLED_YES : status;
```

`ShopUserTenant.ENABLED_YES` 是 **`int` 基本类型**（`ShopUserTenant.java:53`），Java 条件表达式做二元数值提升 ⇒ 整个表达式类型为 `int`（不是 `Integer`）⇒ **无论走哪个分支，`status`（`Integer`）都要参与拆箱**。`status == null` ⇒ NPE。

**实测证据**（`tools/probe-trade-audit.cjs`，后端 5655）：

```
GET /api/erp/mall/admin/user/page?pageNum=1&pageSize=10                     -> 500
GET /api/erp/mall/admin/user/page?...&showDisabled=true                    -> 500
GET /api/erp/mall/admin/user/page?...&auditStatus=1                        -> 500   (status 仍为 null)
GET /api/erp/mall/admin/user/page?...&showDisabled=false                   -> 200   (只走 true 分支才不拆箱)
```

错误日志 `backend/logs/errors/2026-09-23.jsonl` 两条：
`Cannot invoke "java.lang.Integer.intValue()" because "status" is null`。
（`showDisabled=false` 时**不**求值 `status`，故不报错 —— 这正是它一直没被发现的原因。）

**E2E 交叉印证**：`node tools/e2e-trade.cjs` → **102/103**，唯一失败项就是它：

```
❌ 买家账号/买家申请 → /erp/mall/admin/user/page 可用且返回分页结构 — status=500 total=NaN
```

**回归来源**：`git log -S "enabledFilter"` → 提交 `a490efe6 feat(mall): 启用/停用下沉到关联表（阶段 1 遗留项）`。该提交之后交易模块 E2E 未再跑过，故长期未被发现（README 记的 103/103 是**该提交之前**的结果）。

**影响面**：菜单 80363「买家账号」、80362「买家申请管理」两页**整页不可用**（列表、统计卡全空）。前端这两页是**交易模块里唯一会主动打这个接口**的地方，且买家申请页一次打 3 次（3 个审核状态统计卡）。

**建议修法**（最小改动、保留原语义）：把常量参与比较改成装箱安全写法，例如
`Integer enabledFilter = (status != null) ? status : (Boolean.FALSE.equals(showDisabled) ? Integer.valueOf(ShopUserTenant.ENABLED_YES) : null);`
—— 具体写法需业务确认「只看启用」与「显式 status」的优先级，故**未自行修改**。

---

### P0-2 支付回调不验签 + 5 个渠道实现全为桩 ⇒ 任意登录用户可伪造「已支付」

**A. 回调端点无鉴权注解、也无验签调用**

`backend/core/payment/core-payment/.../controller/PaymentController.java:95`：

```java
@PostMapping("/callback/{channel}")          // ← 无 @SaCheckPermission（本轮扫描的 67 个裸端点之一）
public Result<PaymentRecord> handleCallback(@PathVariable String channel, @RequestBody String callbackData) {
    PaymentRecord record = paymentService.handleCallback(channel, callbackData);   // ← 直接交给渠道实现
```

`PaymentServiceImpl.handleCallback`（`service/impl/PaymentServiceImpl.java:135-160`）把报文原样交给 `paymentChannel.handleCallback(callbackData)`，**没有任何验签步骤**。

**B. 渠道实现是无条件成功桩**

| 渠道 | `handleCallback` | `createRefund` | `closePayment` |
|---|---|---|---|
| `AlipayChannel.java:45` | `record.setStatus(2)`（已支付），**不解析报文、不验签**（`// TODO: 解析支付宝回调数据`） | 返回伪造单号 `"ALIPAY_REFUND_" + id` | 空实现（TODO） |
| `WechatChannel.java:43` | 同上，`setStatus(2)` | `"WECHAT_REFUND_" + id` | 空实现 |
| `UnionPayChannel.java:43` | 同上，`setStatus(2)` | `"UNIONPAY_REFUND_" + id` | 空实现 |
| `CashChannel.java:44` | `setStatus(2)` + `callbackTime` | `"CASH_REFUND_" + id` | 空 |
| `BankChannel.java:43` | `setStatus(0)`（无自动回调） | `"BANK_REFUND_" + id` | 空 |

`queryRefund` 同样是硬编码状态（`CashChannel` 直接 `setStatus(1) // 线下退款默认成功`）。

⇒ 只要拿到一个登录态，`POST /api/payment/callback/ALIPAY`（body 随便写）就会：**写一条 `status=2` 的 `payment_record`，并把 `channel_order_no` 匹配上的 `payment_request` 置为已支付**（`PaymentServiceImpl.java:147-157`）。

**C. 三个验签器是死代码**

`callback/AlipayCallbackVerifier`、`WechatCallbackVerifier`、`UnionPayCallbackVerifier`、`TenantChannelCredentialReader` —— `grep -rn` 全仓，**生产代码零引用，只有 `src/test/**` 引用**。它们带完整 RSA2 / APIv3 / 平台证书表校验逻辑（`PaymentCredentialValidationTest` 全覆盖），但**从未被接线**。

**D. 前端注释与实现相反**

`frontend/apps/pc-admin/src/api/payment/index.ts:57-60` 写着：

> 「不配这些，回调端点会 fail-closed 拒绝（**不会「不验签当成功」**）」

实际行为恰恰是「不验签当成功」。**该注释必须改，否则会误导后续排障**。

**E. 真实渠道也调不通**

`/api/payment/callback/{channel}` 不在 `SaTokenConfig` 的匿名白名单里 ⇒ **匿名调用返回 401「请先登录」**（实测）。外部支付平台不可能有 Sa-Token 会话 ⇒ 即便渠道对接好了，回调也进不来。

> ⚠️ 因涉及「改哪一层验签、失败如何记账、回调是否要放行匿名」等口径，**本轮未改动代码**。

---

### P0-3 外部平台三页 + 开放 API 是空壳，且「开放 API」对外不可用

**A. 开放 API 需要登录 — 外部平台根本接不进来**

实测（`tools/probe-trade-audit.cjs`）：

```
匿名   GET /api/open/health        -> 401 {"code":401,"message":"请先登录"}
带登录 GET /api/open/health        -> 200 {"data":{"status":"UP","version":"1.0.0"}}
```

`/api/open/**`（12 个端点）不在 `SaTokenConfig` 的 `excludePathPatterns` 里 ⇒ 被 `SaInterceptor(StpUtil::checkLogin)` 拦住。**「供外部平台调用」的接口，外部平台拿不到会话；反而任意已登录用户（包括 C 端买家）都能调。**

**B. 类注释宣称的「安全验证 - 签名校验」不存在**

`OpenApiController.java:29` 注释写「4. 安全验证 - 签名校验」，方法签名收下 `String signature` 参数后**从未使用**：

```java
public Result<ExternalOrderRaw> orderCallback(..., @RequestParam(required = false) String signature) {
    log.info("接收订单回调: channel={}", channelCode);           // signature 未出现
```

**C. 回调验签的实际代码被注释掉**

`ExternalOrderServiceImpl.java:55-58`：

```java
// 验证签名
// if (!adapter.verifyCallbackSignature(callbackData, signature)) {
//     throw new SecurityException("回调签名验证失败");
// }
```

（`ErpApiChannelAdapter.verifyCallbackSignature` / `TaobaoChannelAdapter` 里是有实现的，只是没被调用。）

**D. 渠道适配器的「拉单」返回硬编码假数据**

`core-base/.../trade/channel/TaobaoChannelAdapter.java:56-73`：

```java
public List<ExternalOrderDTO> pullOrders(String startTime, String endTime, Integer pageSize) {
    // TODO: 调用taobao.trades.sold.get接口
    ExternalOrderDTO dto = new ExternalOrderDTO();
    dto.setExternalOrderId("TB_" + System.currentTimeMillis());
    dto.setExternalOrderNo("TB_ORDER_001");
    dto.setBuyerName("淘宝买家");
    dto.setReceiverName("张三");
    dto.setReceiverPhone("13800138000");
    dto.setOrderAmount(BigDecimal.valueOf(100.00));
    ...
```

`isConnected()` 恒 `true`、`initialize()` 空、`refreshToken()` 空、`pushOrderStatus/pushLogistics/pushProduct/updatePrice` 全 TODO 且返回假 `success`。

**E. 「渠道同步」是「模拟同步结果」**

`ChannelConfigServiceImpl.java:162-195`：

```java
public boolean initializeChannel(Long id) { ... // TODO: 实际调用外部API验证连接
    return true; }
public Map<String, Object> syncChannelData(Long id) {
    // 模拟同步结果（实际应调用外部API拉取数据）
    int syncedOrders = 0;  int syncedProducts = 0;
    ... updateSyncTime(id);
```

前端「渠道配置」页的**初始化 / 同步**按钮 → 永远「成功」，拉 0 条。

**F. 开放 API 里还有 3 个 TODO 桩**

`OpenApiController`：`/inventory/lock`（TODO：实现库存锁定，仅做可用量比较后返回）、`/inventory/release`（TODO：直接 `return success()`）、`/order/status/{id}`（TODO：直接 `return success()`）。

**G. 数据侧印证**：`external_channel_config` 0 行、`external_order_raw` 0 行、`inventory_sync_record` 0 行 ⇒ 整条链路从未被真实数据走过。

---

### P0-4 模块对**非超管**完全不可用（菜单不可见 + 全部接口 403）

**A. 91 个交易域权限码 100% 只授 `SUPER_ADMIN`**

```sql
SELECT p.permission_code, string_agg(DISTINCT r.role_code, ',')
FROM sys_permission p
LEFT JOIN sys_role_permission rp ON rp.permission_id = p.id
LEFT JOIN sys_role r ON r.id = rp.role_id
WHERE p.deleted=0 AND (p.permission_code LIKE 'mall:%' OR p.permission_code LIKE 'trade:%' OR p.permission_code LIKE 'payment:%')
GROUP BY p.permission_code;
-- 91 条，角色列全部 = [SUPER_ADMIN]；未关联任何角色的：0 条
```

对比：全库 1836 个权限码中 **1612 个（87.8%）只授超管**；`sys_role_menu` 全表仅 3 行、**交易菜单 0 行**（菜单可见性依赖权限码派生，见 `SysMenuServiceImpl.getUserMegaMenus` 注释「平台-AUTHZ-01」）。

**B. 实测：非超管账号全部 403**

```
== e2e_scheduler_plain (SYSTEM_ADMIN, 系统租户) ==
   403  /api/erp/mall/admin/order/page      无权限访问: mall:order:list
   403  /api/erp/mall/admin/config          无权限访问: mall:config:view
   403  /api/erp/mall/admin/user/page       无权限访问: mall:user:list
   403  /api/payment/request/page           无权限访问: payment:request:list
   403  /api/trade/channel/page             无权限访问: trade:channel:list
   菜单树节点数 = 0
== e2e_hr_t2 (E2E_T2_ADMIN, 租户 2) ==   （同上 5 条全 403，菜单树 0 节点）
```

> 这也解释了**为什么 E2E 一直「全绿」**：`tools/e2e-trade.cjs` 默认账号 `e2e_dispatch`，经查是 `SUPER_ADMIN`（与采购审计 `43/46 用户是超管致 E2E 假绿` 同一根因）。

**C. 13 条菜单 `menu_level = 3` —— 非法取值，非系统租户直接被过滤**

`sys_menu` 实测：合法语义只有 `0=租户级 / 1=系统级`（`docs/.../mega-menu-redesign.md:865-866`、`SysMenu.java:172`）。当前库内分布：

| menu_level | 条数 | 合法性 |
|---|---|---|
| 0 | 252 | ✅ |
| 1 | 43 | ✅ |
| **3** | **124** | ❌ 非法 |
| **4** | **3** | ❌ 非法 |

过滤逻辑 `SysMenuServiceImpl.java:276-281`：

```java
// 普通租户：只返回租户级菜单（menuLevel=0），但超管不受限制
if (!isSystemTenant && !isSuperAdmin) {
    wrapper.eq(SysMenu::getMenuLevel, 0);
}
```

交易域 22 页里 **13 页是 `menu_level=3`**（80350/80351/80360~80364/80370~80375）⇒ **对「非系统租户 且 非超管」的用户，这 13 页永远不会出现在导航里**；剩下 9 页（90102/90103/90301~90304/90104~90106）虽为 0，但接口 403 同样进不去。

> 开发环境长期「看不见」这个问题：dev 登录账号 `admin` 属**系统租户（tenant_id=1）+ SUPER_ADMIN**，两个豁免条件**同时命中**（`isSystemTenant=true` 与 `isSuperAdmin=true`），过滤分支根本不执行。

---

### P0-5 商城 C 端订单接口**无归属校验**（IDOR）+ 买家可自行审批　✅**已修（见 §〇之二 #2/#3，真机 403/200 已验证）**

`MallOrderServiceImpl` 的 6 个写/读方法全部只按主键取单，**不校验订单是否属于当前调用者**：

| 方法 | 行 | 校验 |
|---|---|---|
| `getOrderDetail(Long id)` | 395 | `selectById(id)`，无 `customerId` 比对 |
| `cancelOrder(Long id)` | 412 | 同 |
| `confirmOrder(Long id)` | 435 | 同 |
| `payOrder(Long id)` | 457 | 同 |
| `approveOrder(Long id)` | 480 | 同（且**只允许 PAID→APPROVED**，不校验调用者是谁） |
| `rejectOrder(Long id, reason)` | 501 | 同 |

对应端点 `/api/v1/mall/orders/**` **无 `@SaCheckPermission`**（仅 `StpUtil.checkLogin()`）。跨租户由租户拦截器兜住，但**同租户内跨用户**没有任何防线：

- 买家 A 猜/遍历 id 可读买家 B 的订单详情（收件人、电话、地址、金额）；
- 买家 A 可对自己（或他人）的订单**直接调 `/orders/{id}/approve` 自审通过**，绕过管理端审核；
- 可直接调 `/orders/{id}/pay` 把自己订单置为已付（`payOrder` 只是改状态 + `receivedAmount = totalAmount`，**不产生任何支付记录**）。

> `frontend/apps/mobile-mall` 目前**没有调用** approve/reject/pay（见 P1-6），所以是「**暴露但未被前端使用**」——不改变它可被直接调用的事实。

---

## 二、P1（功能/规范缺陷）

### P1-1 商品上架「新增」写残留表、「删除」静默无效

数据源口径已在代码注释里承认（`MallAdminServiceImpl.java:670-700, 757-775`），本轮复核确认**仍然如此**：

| 动作 | 端点 | 实际写哪 | 结果 |
|---|---|---|---|
| 列表 | `GET /erp/mall/admin/product/page` | 视图 `v_mall_product`（源表 `erp_product`） | ✅ 正常 |
| 编辑 | `PUT /erp/mall/admin/product/{id}` | 权威表 `erp_product`（`ErpProductWriteMapper`） | ✅ 正常 |
| **新增** | `POST /erp/mall/admin/product` | **已废弃的 `mall_product`** | ❌ 保存成功但**列表不出现新商品** |
| **删除** | `DELETE /erp/mall/admin/product/{id}` | **已废弃的 `mall_product`** | ❌ 从视图拿到的 id 恰好能命中 `mall_product` 同 id 行（见下），于是**「删除成功」但列表不变** |

实测数据（这正是「静默无效」比「报错」更危险的原因）：

```sql
SELECT count(*) FROM mall_product m JOIN erp_product p ON p.id = m.id;   -- 83（id 完全重叠）
SELECT count(*) FROM v_mall_product;                                     -- 6
SELECT tenant_id, deleted, count(*) FROM mall_product GROUP BY 1,2;      -- (1,0)=85
SELECT tenant_id, deleted, count(*) FROM erp_product GROUP BY 1,2;       -- (1,0)=6, (1,1)=77
```

`mall_product` 是 `erp_product` 的**触发器镜像**（`trg_erp_product_sync_mall AFTER INSERT OR UPDATE ON erp_product`），因此 id 一一对应 ⇒ 管理员点「删除」→ 软删镜像行的 `deleted` → **视图（查 `erp_product.deleted`）纹丝不动**。

### P1-2 `mall_product` 冗余镜像表 + 触发器 + 85 行僵尸数据　⚠️**本轮未动**（表、触发器、`MallProductMapper`、`model/MallProduct` 全部保留 —— 因为 P1-1 的两个坏分支还在写它；**必须先收口 P1-1 才能删**）

- 表：`mall_product`（19 列 / **85 行**，其中 77 行对应 `erp_product.deleted=1` 的已删商品，但镜像表 `deleted` 全为 0 ⇒ **镜像与源表已不一致**）。
- 触发器：`trg_erp_product_sync_mall`（`sync_erp_product_to_mall()`）为**单向** `erp_product → mall_product`；`mall_product.deleted` 不在同步列里。
- 读路径：**0 个**（列表/购物车/下单全部改走 `v_mall_product`）。
- 写路径：仅剩 P1-1 的两个坏分支。

⇒ 一旦 P1-1 修好，`mall_product` 表 + 触发器 + `MallProductMapper` + `model/MallProduct` 可整体删除（**当前不可删，因为 P1-1 的两个分支还在写它**）。⚠️ 删除镜像表前需确认无其它模块/报表读取。

### P1-3 「商城设置」与「单位显示」的配置项是**只写不读**的空开关

`tenant_shop_config`（`ShopConfig`）共 **82 个字段**。逐字段反查全后端 getter 消费方，运行期真正被读的只有 **4 个**：

| 字段 | 消费方 | 生效于 |
|---|---|---|
| `allowGuest` | `MallGuestAccess.guestMayBrowse` | 游客准入 |
| `guestShowPrice` | `MallGuestAccess.priceVisible` | 游客是否见价 |
| `regAuditRequired` / `enableAutoAudit` | `MallAuthServiceImpl` | 注册是否需审核 |

其余 **78 个字段（基础设置 / 店铺设置 / 运费设置 / 商城装修 / 公告设置的全部业务项）没有任何读取方**。

同类「只写不读」还有三处（各自独立确认）：

| 页面 | 写入目标 | 运行期读取方 | 结论 |
|---|---|---|---|
| 公告设置 80374 | `mall_notice` | `MallNoticeController`（存在）但**全仓前端 0 调用**（`mobile-mall` 无公告 UI） | 商城看不到公告 |
| 关键词库 80375 | `mall_keyword` | **无**。`MallProductServiceImpl.listProducts` 搜索只 `like(product_name, keyword)`，**不查关键词库** | 关键词库对搜索零影响 |
| 商城装修 80373 | `shop_decoration` / `shop_decoration_product` | **无**（仅管理端 CRUD）。C 端唯一读的展示数据是 `shop_banner`（`/products/banners`） | 装修配置不影响商城外观 |
| 单位显示 80361 | `erp_product.unit_display`、`erp_product_unit.unit_display_type` | **无**。`v_mall_product` 视图只映射 `p.unit`，不含该开关 | 单位显示不影响商城单位展示 |

### P1-4 C 端商城：物流轨迹恒空 + 「去支付」白屏 + 支付接口定义了但没人调

| 位置 | 问题 | 证据 |
|---|---|---|
| `mobile-mall/src/api/index.ts:185` | `track: (id) => request.get('/orders/${id}/track')` — **后端不存在该端点**（`MallOrderController` 只有 create/list/detail/cancel/confirm/pay/approve/reject/payment-methods） | 加载物流被 `catch` 吞掉 ⇒ 「待收货/已完成」订单**物流轨迹永远为空，且无任何报错** |
| `views/order/detail.vue:54`、`views/order/list.vue:51` | `router.push('/order/'+id+'/pay')` — **路由表中不存在 `/order/:id/pay`**，且无 catch-all | 点「去支付」→ 空白页 |
| `api/index.ts:183,189,190,191` | `order.pay` / `payment.createPayment` / `getPaymentStatus` / `payment.callback` **4 个接口定义了但全仓 0 调用** | C 端**支付链路未实现**；`payment.callback` 路径还写错（缺 `/{tenantId}/{channel}` 两段） |
| `api/index.ts:191` | `callback: (data) => request.post('/payments/callback', data)` — 后端实为 `/api/v1/mall/payments/callback/{tenantId}/{channel}` | 路径不匹配（且本不应由前端调回调） |

### P1-5 订单号用**进程内 AtomicLong**生成，与全站号段规范不一致

`MallOrderServiceImpl.java:549-553`：

```java
private static final AtomicLong ORDER_NO_COUNTER = new AtomicLong(0);
private String generateOrderNo() {
    String dateStr = LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_MS_PATTERN);
    long seq = ORDER_NO_COUNTER.incrementAndGet() % 10000;
    return "ORD" + dateStr + String.format("%04d", seq);
}
```

- **多实例部署**各自计数 ⇒ 同毫秒可撞号（`erp_sale_order.order_no` 若无唯一约束则静默重复）；
- `% 10000` 溢出后序号回绕；
- 全站其它单据统一走号段/`next-no`（如 `XSDD-YYYYMMDD-序号`），此处**单独一套**，属规范性偏离。

### P1-6 跨模块：商城订单与库存 / 财务 / 客户主数据**均未闭环**

| 关系 | 现状 |
|---|---|
| 商城下单 → 库存 | **只校验不占用**（`MallOrderServiceImpl.createOrder` 注释自陈）。并发下单可超卖。 |
| 商城订单发货 → 出库 / 库存 | `MallAdminServiceImpl.shipOrder` 只改 `delivery_status` + `status`，**不生成出库单、不扣库存**。 |
| 商城订单收款 → 应收 / 凭证 | `receivePayment` 只改订单状态。**不写 `finance_receivable`、不生成凭证**。（对比销售出库单有 `createReceivableOnShipment`。） |
| 商城订单 → 客户主数据 | `customer_id` 优先取 `biz_party.id`，**取不到时回落成 `shop_user.id`**（`MallOrderServiceImpl.java:236-238`，仅 `log.warn`）。若真发生，销售模块按 `customer_id` 关联往来单位会**指向错误主体**。 |
| 支付结算 → 总账 | `core-payment` 与财务模块**无任何代码依赖**（`PaymentServiceImpl`/`RefundServiceImpl` 未 import 财务包）⇒ 支付流水不进总账，是独立账本。 |

### P1-7 `tenant_shop_config` 82 列 —— 违反「单表 ≤25 列」技术规范

`docs/.../development-standards` 记：数据库单表 ≤25 列、实体 ≤25 字段。实测：

| 表 | 列数 | 超出 |
|---|---|---|
| `tenant_shop_config` | **82** | 3.3× |
| `shop_user` | **39** | 1.6× |

`tenant_shop_config` 把「基础信息 / 资质 / 联系方式 / 退货地址 / 店铺参数 / 注册设置 / 支付设置 / 运费 / 到店自提 / 装修」全部塞进一行，且 **78 列无消费方**（P1-3）⇒ 既是 god table，又是死重量。建议按业务域拆成 `shop_profile` / `shop_register` / `shop_freight` / `shop_decoration` 等，或在拍板「商城不做」后整体归档。

### P1-8 购物车页「清空购物车」清的是**管理员自己**的购物车　✅**已修（见 §〇之二 #8）**

`views/trade/cart/list.vue:378-387` → `mallCartApi.clearCart()` → `DELETE /api/v1/mall/cart` → `MallCartServiceImpl` 按 `StpUtil.getLoginIdAsLong()` 当 `customerId`。管理端列表展示的是**全体会员**的购物车（`GET /cart/page`），而「清空」作用于**调用者本人的 C 端购物车**（管理员通常没有）⇒ 按钮点了**对列表无任何影响**，属语义错位。单条/批量删除走 `/cart/{id}`、`/cart/batch`，与列表一致，是正确的；**只有「清空」这一个按钮错**。

### P1-9 支付/退款回调端点无鉴权注解（67 个裸端点的一部分）

`POST /api/payment/callback/{channel}`（`PaymentController.java:95`）、`POST /api/refund/callback/{channel}`（`RefundController.java:82`）**既无权限注解、也无验签**。见 P0-2。（配置 `/api/payment/config/*` 7 个端点**全部**有 `@SaCheckPermission`，是合规的。）

---

## 三、P2（死代码 / 冗余 / 边界）

### P2-1 前端 7 个死接口：`mallReturnApi`　✅**已删（见 §〇之二 #6）**

`frontend/apps/pc-admin/src/api/erp/mall.ts:1315-1337` 定义 `mallReturnApi`（page/stats/getById/approve/reject/batchApprove/batchReject → `/erp/mall/admin/return/*`）：

- 后端**不存在** `/erp/mall/admin/return/*` 任何一个端点；
- 全仓 `grep -rn "mallReturnApi"` **只有定义处一行，零引用**。

真正的退货处理页 `views/mall/return-process/index.vue:848` 用的是 `saleReturnApi`（`/erp/sale/return/*`）。⇒ **整个 `mallReturnApi` 对象（7 个函数）可直接删除**。

> ⚠️ 这正是「子代理审计报告必须复核」的典型：`audit-trade-module.py --links` 首轮把这 7 条报成「断链」，人工复核后确认是**死接口**而非真断链。

### P2-2 后端 2 个死实体类　✅**已删（见 §〇之二 #7）**

| 文件 | 映射 | 引用情况 |
|---|---|---|
| `erp-mall/.../model/MallOrder.java` | `mall_order`（22 列 / 0 行） | 全仓 **Java 零引用** |
| `erp-mall/.../model/MallOrderItem.java` | `mall_order_item`（14 列 / 0 行） | 仅被 `MallOrder.java:66` 引用（**互为孤岛**） |

> 两个文件由 `V6.4.0__Create_B2B_Mall_Tables.sql` 建表，`V9.0.0__Trade_Center_Consolidation.sql` 收敛后已无业务用途。
> **注意** `audit-trade-deadcode.py` 首轮**漏报**了它们：前端 `api/erp/mall.ts` 里有一个同名的 TS 接口 `MallOrder`，词边界匹配无法区分语言 ⇒ 已用「只看 `*.java`」的定向 grep 人工确认。脚本这一局限已写进其文档串。

### P2-3 3 张僵尸表（0 行、0 引用）

| 表 | 列 | 行 | 说明 |
|---|---|---|---|
| `mall_order` | 22 | 0 | 见 P2-2，实体待删 |
| `mall_order_item` | 14 | 0 | 同上 |
| `mall_product` | 19 | 85 | 见 P1-1/P1-2（**待 P1-1 修好后再删**） |

### P2-4 公告接口自称「公开查询」，实际需要登录

`MallNoticeController.java:24-27` 注释：

> 「商城公告（**商城端公开查询**）… 免登录权限按模块现状（与商城商品浏览一致，走全局 Sa-Token 拦截器现状配置）」

实测：`/api/erp/mall/notice/list` 不在匿名白名单（`SaTokenConfig` 只放行了 `/api/v1/mall/auth/**` 与 `/api/v1/mall/products/**`）⇒ **匿名 401、必须登录**。且它挂在 `/api/erp/mall/**`（管理端前缀）下，与「商城端」定位不符。叠加 P1-3（C 端 0 调用），**公告功能整体空转**。

### P2-5 `mall/admin/order/{id}/approve|reject` 在 C 端 controller 重复暴露

`MallOrderController`（`/api/v1/mall/orders/**`）与管理端 `MallAdminController`（`/api/erp/mall/admin/order/**`）**各自暴露了 approve/reject，且走同一套 `erp_sale_order`**：

| | C 端 | 管理端 |
|---|---|---|
| 端点 | `PUT /api/v1/mall/orders/{id}/approve` | `PUT /api/erp/mall/admin/order/{id}/approve` |
| 鉴权 | 仅「已登录」 | `@SaCheckPermission("mall:order:approve")` |
| 归属校验 | **无** | 无（管理端不需要） |
| 前端调用方 | **无** | 订单处理 / 商城订单页 |

⇒ C 端这一对是**冗余且危险的暴露面**（见 P0-5），建议直接删除或加归属+角色校验。

### P2-6 代码目录归属与功能归属不一致（`api-monitor` / `mall-popup`）

`views/trade/api-monitor/list.vue` 位于 `views/trade/` 目录，但其菜单 `90107` 的父级是 **`61506`（配送 → API监控）**，文档在《配送模块/API监控开发文档》。**代码目录归属与功能归属不一致**（不影响运行，但会误导后续「交易模块都该看 views/trade」的检索）。同理 `mall/popup-ad` 的后端在 `erp-mall`、菜单却在**营销模块**（`80323 marketing/mall-popup`，前端 `api/marketing.ts:771`）——属**正常的跨模块复用**，不建议改，但需在文档里说清边界。

### P2-7 其它零散项

| 项 | 说明 |
|---|---|
| `MallAdminController:273,285` 的 `createProduct` / `updateProduct` 入参用同一实体 `MallProduct` | 该实体映射已废弃表；编辑分支只借它做 DTO。语义混淆，建议补一个专用 Request DTO |
| `mall_order_item` 相关 DTO `MallOrderItemPageDTO` | **在用**（`/order/page-detail`），不要与 P2-2 的死实体混淆 |
| `api/payment/index.ts:57-60` 注释 | 与实现相反（见 P0-2D），需改 |
| `ErpProductMall` / `ErpSaleOrderMall` 等 `dao/**` 下的实体 | 命名上像 DAO 实为实体，package 归属与全仓 `model/entity` 惯例不一致（不影响运行） |
| `MallCartController.getCart()`（`GET /api/v1/mall/cart`）与 `DELETE /api/v1/mall/cart` | 管理端「清空」误用（见 P1-8）；接口本身对 C 端是正确的 |

### P2-8 僵尸权限码 `payment:account:select`

`sys_permission` 中有 `payment:account:select`（"付款账户选择"，`V11.432.0__Seed_Fine_Grained_Business_Permissions.sql:86` 种子），但**全仓（Java / TS / Vue）零引用** —— 既无注解消费、也无前端 `v-permission` 消费。属可删的僵尸码。

> **反面提醒（避免误判）**：`mall:tag:list/create/update/delete/export`（5 个码）在交易模块的三个后端目录里搜不到，但它们是 **erp-stock 的 `MallTagController`（`/api/erp/mall-tag/*`）** 在用 —— 判定「权限码是否有消费方」必须扫全仓，不能只看模块目录。

---

## 四、健康项（本轮复核为**无问题**，避免后续重复排查）

| 项 | 结论与证据 |
|---|---|
| 22 页菜单组件文件 | `sys_menu.component` 逐一映射到磁盘 ⇒ **0 缺失** |
| 前端调用 → 后端存在的路径 | **189 处**调用中，断链仅 9 条（7 条 = P2-1 死接口；2 条 = P1-4 C 端真断链），**其余全部命中**。判定基线为**全后端端点集**（含跨模块接口，如 `商品上架` 页依赖 erp-stock 的 `/erp/mall-tag/*`），且调用池覆盖 22 页依赖的全部 9 个 api 模块 |
| 管理端端点鉴权注解 | 111 个管理端端点中 **109 个**有 `@SaCheckPermission`，仅 2 个回调无（P1-9/P0-2） |
| 权限码在库 | 交易域 91 个权限码**全部存在**于 `sys_permission`（`deleted=0`），无「注解有码、库里没有」的锁死风险 |
| 商品上架数据源 | 列表/编辑已改走权威视图 `v_mall_product` + `erp_product`（`ErpProductWriteMapper`），**读写同源**（仅新增/删除两分支未收口） |
| 商城订单与销售订单同源 | 商城订单 = `erp_sale_order` + `order_source ∈ {2,3}`；销售模块 `SaleOrderServiceImpl` / `SaleAnalysisMapper` 已识别该字段，**双方口径一致** |
| Flyway 迁移 | 493 个迁移脚本**无重复版本号**（`ls \| sed 's/__.*//' \| sort \| uniq -d` 为空） |
| 租户拦截器忽略清单 | `shop_template` / `shop_user_party_link`（均无 `tenant_id` 列）**已登记**在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`，不会触发「字段 tenant_id 不存在」类 500 |
| 模块级 E2E | `tools/e2e-trade.cjs` 实跑 **102/103**；UI 抽样 6 页**全部**渲染金标准骨架、无 `[object Object]`/`NaN`、无 console error |
| 文档与库内菜单口径 | README「22 页 5 分组」与 `sys_menu` 实测**逐条一致**（2026-09-15 复核结论仍成立） |

---

## 五、待拍板（涉及口径/设计，本轮**未改动**）

> 以下 5 项都需要业务/架构决策后才能动手，按影响面排序。

### ① 商城（C 端）到底做不做？

**事实**：`shop_user` 0 行、`shop_user_tenant` 0 行、`mall_cart` 0 行、`mall_address` 0 行、`mall_order` 0 行、`erp_sale_order(order_source IN (2,3))` 0 行、`payment_*`/`refund_*` 全 0 行、`external_*` 全 0 行 ⇒ **C 端全链路从未被真实数据走过**。

- **选项 A「做」**：需补齐移动端支付链路（`/order/:id/pay` 路由 + 调用 `createPayment` + 轮询 `getPaymentStatus`）、支付渠道真实对接与回调验签接线、外部平台适配器真实实现、库存占用。工作量集中在 `core-payment` 与 `mobile-mall`。
- **选项 B「保留框架、不投入」**：应把 C 端与外部平台的**未接线接口标注为实验性**（或在菜单上加「未启用」标记），并删掉会造成误导的假数据（P0-3D）与错误注释（P0-2D）。

**建议**：先做 B 的「止血」（删假数据、修注释、关掉会写脏数据的能力），再决定 A 的投入。

### ② 商城订单与库存 / 财务的联动口径

当前：下单**只校验不占库存**、发货**不出库不扣库存**、收款**不生成应收不出凭证**。
需要拍板：商城订单是否走「确认后生成销售出库单 → 由出库单驱动库存与应收」（与销售模块一致），还是商城侧独立记账。**这决定了是补 3 处联动，还是把商城订单明确定位为「仅收集订单、履约在 ERP 侧手工开单」。**

### ③ 交易模块权限开放给哪些角色

现状 91/91 只授超管 ⇒ 非超管整模块不可用（P0-4B）。需要决定：
- 是否新建「商城管理员 / 支付专员 / 外部平台管理员」等角色并按最小权限授码；
- 13 条 `menu_level=3` 的菜单**修成 0**（租户级）还是**改成 1**（系统级）——注意全库另有 124 条非法 `menu_level`（3/4），**建议一次性全库普查**，不要只修交易域。

### ④ `tenant_shop_config` 是否拆表

82 列 / 78 列无消费方。拆表（按业务域）会牵动 9 个设置页的读写与迁移；不拆则持续违反 ≤25 列规范。**建议与①绑定决策**：若选 B，则归档而非拆分。

### ⑤ 支付结算是否需要与总账联通

现状是独立账本（无凭证、无应收）。若需要联通，需明确：支付流水进哪个科目、由哪个单据触发、线下确认收款（CASH/BANK）如何入账。

---

## 六、本轮产出的可复现脚本

| 脚本 | 用法 | 说明 |
|---|---|---|
| `tools/audit-trade-module.py` | `python tools/audit-trade-module.py --all` | 断链 / 无人调用 / 裸端点 / 权限码四段（本轮已修正三处口径） |
| `tools/audit-trade-deadcode.py` | `python tools/audit-trade-deadcode.py` | 类级零引用扫描（区分 Spring 组件与非组件；已知局限见 P2-2） |
| `tools/probe-trade-audit.cjs` | `node tools/probe-trade-audit.cjs 5655` | 真机探针：匿名 vs 登录态、C 端、管理端、开放 API |
| `tools/e2e-trade.cjs`（既有） | `ERP_PORT=5655 FE_URL=http://localhost:5656 E2E_USER=e2e_dispatch node tools/e2e-trade.cjs` | 模块 E2E；**注意 E2E 账号是超管，权限类问题会被掩盖** |

**结果留存**：`tool-results/trade-audit-e2e-20260923.txt`（102/103）、`tool-results/trade-audit-perms.txt`（67 裸端点）、`tool-results/trade-audit-unused.txt`（23 条无人调用候选）。

**扫描口径的三个坑（写审计脚本时会重复踩）**：见记忆 `endpoint-scan-three-false-positives.md` —— ① 鉴权注解「向前窗口」跨方法捞错注解；② 断链判定必须用**全后端**端点集（否则跨模块调用全是假断链）；③ 跨语言同名标识符（TS interface `MallOrder`）会掩盖 Java 死类，判 Java 死代码必须限定 `*.java` 再 grep。

---

## 七、附：关键数据快照（devdb @ 2026-09-23）

```
后端端点 189（erp-mall 113 / core-base·trade 45〔含 api-monitor 13，属配送〕/ core-payment 31）
  · 管理端 111 → 109 有鉴权注解
  · 裸端点 67 = /api/v1/mall 52 + /api/open 12 + /api/erp/mall 1 + /api/payment 1 + /api/refund 1
前端调用 189 处（22 页依赖的 9 个 api 模块 + mobile-mall 全量）→ 断链 9 条（7 死接口 + 2 C 端真断链）
后端「无人调用」候选 23 条（经人工甄别：C 端接口由 mobile-mall 调用、开放 API/支付回调为外部调用，**无真冗余端点**）

权限码 91（mall:* / trade:* / payment:*）→ 关联角色：SUPER_ADMIN × 91，其它角色 × 0
菜单 22 页：menu_level=0 × 9，menu_level=3 × 13（非法值）
sys_role_menu 交易菜单授权：0 行

交易相关表 25 张，其中 0 行者 18 张：
  external_channel_config / external_order_raw / mall_address / mall_cart / mall_category /
  mall_order / mall_order_item / shop_banner / shop_decoration / shop_decoration_product /
  shop_user / shop_user_party_link / shop_user_tenant /
  payment_request / payment_record / payment_reconciliation / refund_request / refund_record
有数据：erp_mall_tag 39 / mall_keyword 35 / mall_popup_ad 16 / shop_template 17 /
        mall_notice 2 / mall_product 85（僵尸镜像）/ tenant_shop_config 1
视图 v_mall_product 6（权威商品源，= erp_product WHERE deleted=0）
列数超规范：tenant_shop_config 82 / shop_user 39
```

