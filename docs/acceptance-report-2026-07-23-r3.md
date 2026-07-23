# 验收审查报告（第三轮）

> 日期：2026-07-23（R3）｜ 验收人：PM（AI）
> 范围：① R2 六项复验条件核查 ② 采购域 10 页复审 ③ 新交付后端逻辑深审
> **总体判定：❌ 不通过。基础设施修复有效，但出现两处新"假闭环"，采购前端页零改动，基线未清零**

---

## 第一部分：R2 返工复验

| # | 复验项 | 判定 | 证据 |
|---|---|---|---|
| 1 | 应用可启动 + Flyway 到 V11.31.0 | ✅ | V11.31.0/V11.32.0 均 success=t；5655 运行 0.3.9（10:31 构建）；启动验证通过 |
| 2 | vue-tsc 全仓 0 错误 | ❌ **76→19，未清零** | 剩余 19 个全为 TS2322 字面量类型标注问题（`align: string` 应为 `"right"` 字面量等），分布于 cost-sharing/form(8)、receipt-doc(2)、payment-doc、voucher、inbound、initial-stock/finance、mall-order/mall-return、borrow-query、daily 等 12 文件。**无运行时致命项，但基线是 0，不达标** |
| 3 | 费用分摊 E2E（成本回写） | 🔴 **假闭环** | 后端三件套已建（Controller/6 端点/3 表），`/page` 200；但 `complete()` **仅翻转状态**——无 StockService 引用、无入库行成本回写、无库存成本重算、无流水（grep 零命中）。分摊的核心业务价值（费用计入存货成本）完全未实现 |
| 4 | 报价转订单 E2E | 🟠 **建单了但四缺陷** | 路径已对齐 ✅；`insertSaleOrder` 真实 INSERT erp_sale_order ✅。但：① **未插 erp_sale_order_item——明细全丢，转出来的是空壳订单**；② 主键直接复用报价单 id（撞库即 PK 冲突）；③ 单号 SELECT MAX()+1 无锁并发风险（biz_number_sequence 单号服务未用）；④ 源单关联仅靠 remark 文本拼接 |
| 5 | 采购入库列表/查询三页对齐 | ❌ **零改动** | 文件行数与 R2 完全一致（149/109/110/137），入库列表仍无 Tab |
| 6 | payment-method/channel 200 | ❌ **仍 500** | 根因实锤（重启抓栈）：V11.32.0 改了 5 个审计列名但**漏加 `remark` 列**，BaseEntity 映射 remark → `字段 "remark" 不存在`。channel/page 的 200 是假象（MP 对 count=0 跳过分页 SELECT）；有数据的端点全 500 |

## 第二部分：采购域 10 页复审

**前端页面自 R2 起零改动**（文件行数逐一比对一致），页面级判定维持 R2 结论：

| 页 | 判定 | 说明 |
|---|---|---|
| 采购订单表单 | ⚠️ 维持 | 付款区/预付联动/配置弹窗 ✅；**8 个误植价格等级字段仍在**（开发文档明确要求处理）；缺红冲/附件 |
| 采购订单列表 | ⚠️ 维持 | 双 Tab 39/59 列达标；缺状态主 Tab |
| 采购入库单表单 | ✅ 达标 | 本轮 vue-tsc 仅剩 1 个类型标注错误 |
| 采购入库单列表 | ❌ 维持 | 149 行无 Tab，未返工 |
| 费用分摊 | 🔴 降级 | 前端 ✅ 后端假闭环（见上） |
| 补货四页 | ⚠️ 待 E2E | 静态达标；因费用分摊/启动问题本轮未完成生成订单 E2E |
| 查询三页 | ❌ 维持 | 未返工 |

**采购域实质进展为零**（除入库表单类型修复外）——团队本轮精力在后端基建，前端返工项未动。

## 第三部分：本轮新发现问题

1. **假闭环模式复发**（费用分摊 complete/报价转订单空壳）——这是继 R1 报价转订单后第二次出现"状态翻转=完成"的实现方式。**特此升级为模式级红线**：凡涉及"执行/完成/转换"类动作，验收必查下游实体真实落库（成本/订单/库存/凭证），仅状态字段变化一律判不通过
2. **V11.32.0 修复不彻底**（漏 remark）——修复迁移本身未做全链路验证（只要 curl 过一次带数据的端点就会暴露）
3. **erp_cost_sharing vs erp_purchase_cost_sharing 两套表并存**——V9.10.1 曾建 cost_sharing 相关表（迁移描述 "Add Visit CostSharing Dispatch Tables"），V11.31.0 又建一套，需团队确认哪套在用、另一套清理（四问流程未执行的表现）

## 第四部分：返工清单（按序）

1. 🔴 V11.33.0：md_payment_method/channel 补 `remark` 列 → psql 手动执行+登记 → curl 带数据端点复测
2. 🔴 费用分摊 complete() 真闭环：按分摊明细回写 erp_purchase_inbound_item 成本 → 调 StockService 重算库存成本（移动加权）→ 留成本调整流水；E2E 证据（分摊前后库存成本对比查询）
3. 🟠 报价转订单四缺陷：插 erp_sale_order_item 明细（含金额/数量/单价）；主键走 IdType.ASSIGN_ID 或序列；单号用 biz_number_sequence；source_type/source_id 关联报价单
4. 🟠 vue-tsc 19→0（全是 align 字面量标注，机械修复）
5. 🟠 采购前端三处返工（R2 已列，本轮未动）：入库列表对齐出库列表结构、查询三页对齐销售查询、表单去 8 误植列
6. 🟡 费用分摊重复表清理（确认 erp_cost_sharing 与 erp_purchase_cost_sharing 留一）
7. 🟡 promotion_id 归因、期初锁定（连续两轮未动，请给出排期或说明）

## 第四轮复验条件

1. vue-tsc = 0；5655 运行最新构建；Flyway ≥ V11.33.0
2. payment-method/channel 全部端点 200（含带数据的分页）
3. 费用分摊 E2E：create→complete→`SELECT unit_cost FROM erp_purchase_inbound_item` 变化 + erp_stock 成本重算证据
4. 报价转订单 E2E：erp_sale_order 新单 + **erp_sale_order_item 明细行数=报价明细行数** + 源单号关联
5. 采购入库列表/查询三页结构对齐证据（行数与 Tab 结构）
