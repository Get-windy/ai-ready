# 批次 0：断点清零任务单

> 范围：8 个双入口列表页缺失 + 7 个降级页转正。全部无前置依赖，最优先执行。
> 预估总量：6.5 人日。

---

## 0.1 WMS 8 个作业单列表页（P0，2 人日）

**问题**：以下菜单 display_mode=1（双入口），但 list_path 指向的组件不存在，点 [历史] 落"页面组件未找到"占位：

| 菜单ID | 菜单 | path（表单） | list_path（缺失） | 表单组件（已存在） |
|---|---|---|---|---|
| 80010 | 借进单 | wms/borrow-in/form | wh/borrow-in/index | views/wh/borrow-in/form/index.vue |
| 80011 | 借出单 | wms/borrow-out/form | wh/borrow-out/index | views/wh/borrow-out/form/index.vue |
| 80012 | 收货单 | wms/receive/form | wh/receiving-order/index | views/wh/receiving-order/form/index.vue |
| 80013 | 上架单 | wms/putaway/form | wh/putaway-order/index | views/wh/putaway-order/form/index.vue |
| 80014 | 拣货单 | wms/pick/form | wh/picking-order/index | views/wh/picking-order/form/index.vue |
| 80015 | 发货单 | wms/ship/form | wh/shipping-order/index | views/wh/shipping-order/form/index.vue |
| 80016 | 移库单 | wms/move/form | wh/move-order/index | views/wh/move-order/form/index.vue |
| 80017 | 盘点作业单 | wms/check/form | wh/inventory-order/index | views/wh/inventory-order/form/index.vue |

**后端现状**（已就绪，先 grep 核实）：`/api/wms/borrow` 全套（V11.7.0）；6 个 WMS 作业单已有 detail/save 端点（C1 阶段补齐）。列表分页端点逐一 grep `wms/src/main/java` 下 Controller 的 `page` 映射确认。

**改造内容**：
- 前端：新建 8 个列表页 `views/wh/<name>/index.vue`，基于 ARReportPage 或 BillTableList：
  - 查询区：单号/日期范围/状态/仓库（借进借出加往来单位）
  - 表格：单号/日期/仓库/明细摘要/状态（Tag）/操作人/操作（查看、编辑-仅草稿、删除-仅草稿）
  - 行点击跳表单 `wms/<x>/form?id=<id>`（编辑模式，与既有表单页参数约定一致——先读表单页 route 参数处理代码确认）
  - "新增"按钮跳表单页
- 前端：8 个组件注册进 `router/dynamicRoutes.ts` componentMap（键名 = list_path：`wh/borrow-in/index` 等，与动态路由解析约定一致）
- 后端：列表分页端点缺失的补齐（先四问排查，大概率已存在）
- 注意：借进借出已有 `wh/borrow-query`（借进借出查询 107 行降级页），0.4 任务将其升级为独立分析页，与列表页分工：列表页=单据管理（操作向），查询页=统计分析（报表向）

**验收清单**：
- [ ] 8 个菜单点 [历史] 均进入真实列表页，不再是占位
- [ ] 列表数据来自真实分页端点（curl 200），查询条件生效
- [ ] 草稿单可编辑跳表单、已审核单只读
- [ ] `audit_pages.py` 重跑：list_path 缺失清单为 0
- [ ] vue-tsc 0 错误

---

## 0.2 支付方式/支付渠道主数据（P0，1.5 人日）

**问题**：`views/md/payment-method/index.vue`（83 行）、`views/md/payment-channel/index.vue`（59 行）为 a-alert 降级页（借用支付流水展示）。**DB 无任何支付方式/渠道主数据表**（已核 information_schema 为 0）。

**四问结论**：真缺失。落点：erp 域 md 主数据（参照 erp-partner 主数据模块结构）；支付账户页 `views/md/payment-account/index.vue` 已存在，需核对是否可复用其表（先读后写）。

**对标**：Odoo `account.journal`（type=bank/cash 即支付账户）+ `payment.method`（现金/银行/微信/支付宝/POS 等）。商贸惯例：支付方式（现金/银行转账/微信/支付宝/支票/其他）+ 支付渠道（渠道=支付方式的实例通道，如"微信-商户号A"）。

**改造内容**：
- DB：Flyway V11.29.0 建表（DO 块幂等写法）：
  - `md_payment_method`：id/tenant_id/method_code/method_name/method_type(cash/bank/wechat/alipay/check/other)/account_id(默认入账账户)/fee_rate/is_default/sort/status/审计五字段
  - `md_payment_channel`：id/tenant_id/channel_code/channel_name/method_id(关联支付方式)/merchant_no/config_json/sort/status/审计五字段
- 后端：实体+Mapper+Service+Controller（`/api/erp/md/payment-method`、`/api/erp/md/payment-channel` 全套 CRUD+启停用），参照既有 md 域 Controller 写法
- 前端：两页重写为标准主数据 CRUD 页（查询区+表格+新增/编辑弹窗+启停用+删除引用检查），去除 a-alert
- 联动：收付款单表单的"收款方式"下拉改为读支付方式档案（批次 1 落地，本批次仅建数据）

**验收清单**：
- [ ] 两表 Flyway 迁移已手动执行 + flyway_schema_history 登记
- [ ] CRUD 端点 curl 全 200；启停用生效；删除被引用记录有提示
- [ ] 页面无 a-alert 降级标注，新增/编辑/删除全可用
- [ ] 预置示例数据（现金/银行/微信/支付宝）随迁移写入（status=1）

---

## 0.3 商城营销三页写端点（P0，2 人日）

**问题**：商城预售（107 行）/弹窗广告（88 行）/加价购（94 行）三页降级。**DB 无对应表**（已核；V11.9-11.11 仅建了 mkt_flash_sale/mall_notice/mall_keyword）。

**四问结论**：真缺失。落点：erp-marketing（预售/加价购，参照 mkt_flash_sale 模块结构）+ erp-b2b-mall（弹窗广告，参照 mall_notice）。

**对标**：Odoo `sale.coupon.program`/promotion rules + 国内商城惯例。

**改造内容**：
- DB：V11.30.0 建表：
  - `mkt_presale`（预售活动）：id/tenant_id/activity_name/product_id/deposit_amount(定金)/final_amount(尾款)/start_time/end_time/deposit_end_time(定金截止)/final_start_time(尾款开始)/stock_limit/sold_count/status(0未开始1进行中2已结束)/审计
  - `mkt_presale_order`（预售订单关联）：presale_id/mall_order_id/paid_deposit/paid_final/status
  - `mall_popup_ad`（弹窗广告）：id/tenant_id/title/image_url/link_url/show_type(once/everyday/always)/target_user(all/member/new)/start_time/end_time/sort/status/审计
  - `mkt_addon_rule`（加价购规则）：id/tenant_id/rule_name/main_product_id(购主品)/addon_product_id(加价品)/addon_price/max_per_order/start_time/end_time/status/审计
- 后端：三模块 CRUD + 状态流转端点（启用/停用）；加价购补结算试算端点（购物车加价购匹配，可先仅管理端 CRUD，结算联动标注后续任务）
- 前端：三页重写（活动列表+新增/编辑表单弹窗或独立表单页+状态管理+数据统计入口），参照同域已转正的 mall-flash（商城秒杀）页面结构——**先读它的实现作为模板**

**验收清单**：
- [ ] 迁移执行+登记；三组端点 curl 200
- [ ] 三页无 a-alert，CRUD+状态流转可用
- [ ] 预售/加价购时间冲突校验（同商品活动时间重叠禁止）

---

## 0.4 借进借出查询升级 + 应用中心定位（P1，1 人日）

**0.4a 借进借出查询（107 行降级页）**：
- 现状：借用库存流水展示。后端 `/api/wms/borrow` 已有完整数据（订单/明细/归还）
- 改造：重写为 ARReportPage 报表——查询区（单号/类型[借进/借出]/往来单位/状态/日期）、统计卡（借出未还笔数/逾期未还/本月借进额）、表格+明细穿透（展开显示归还进度：已还/未还数量）
- 验收：数据与 wms_borrow_order 实表一致；明细穿透可用

**0.4b 应用中心（245 行降级页）**：
- 现状：set/app-center 降级。功能定位待确认（模块市场？）
- 处置（二选一，PM 已与业务对齐为方案 A）：**方案 A** 页面对接 system-admin 侧模块授权数据（admin/module/list 已有完整实现，租户侧只读展示已授权模块清单）；**方案 B** 菜单下线（sys_menu visible=0，留记录）
- 验收：页面有真实数据或菜单隐藏，二选一不留降级态

---

## 批次 0 交付物

- [ ] `audit_pages.py` 重跑：未解析=0、list_path 缺失=0、degraded 标记仅剩经确认的 0 项
- [ ] 迁移 V11.29.0/V11.30.0 手动执行 + Flyway 登记
- [ ] vue-tsc 0 错误；后端 exec.jar 重启验证
- [ ] E2E 证据汇总（每组端点 curl + DB 终态）
