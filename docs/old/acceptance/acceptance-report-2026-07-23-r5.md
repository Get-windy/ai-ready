# 验收审查报告（第五轮）

> 日期：2026-07-23（R5）｜ 验收人：PM（AI）
> **总体判定：❌ 不通过（连续第四轮）。修复依然只到源码层，且源码中仍残留我上一轮明确指出的同类缺陷。**

---

## 一、复验总表

| # | R4 复验条件 | 判定 | 证据 |
|---|---|---|---|
| 1 | 新 jar 构建+部署+V11.33.0+remark 列 | ❌ **全未做** | jar 仍 10:31；实例仍 PID 39752（R4 同一进程）；flyway_schema_history 无 11.33.0；md_payment_method 无 remark 列 |
| 2 | vue-tsc = 0 | ❌ 19 错误，**连续两轮零变化** | 与 R3/R4 完全一致 |
| 3 | 报价转订单明细正确关联 | ❌ **接线了但关联仍错**（详见二.1） | 源码审查 |
| 4 | 费用分摊按数量加权+库存重算 | ⚠️ 半修：÷数量已加，库存重算仍缺 | 源码审查 |
| 5 | 采购前端页改动 | ❌ **连续三轮零改动** | form.vue mtime 7-22 21:18 |
| 6 | 交付 6 件套 | ❌ 未提供 | — |

## 二、源码级缺陷详查（修复本身仍不合格）

### 1. 报价转订单：CURRVAL 误用，明细依然必成孤儿行 🔴

本轮进展：`insertSaleOrderItems(items)` 调用已接上、垃圾注释已清 ✅。但 R4 指出的核心缺陷**换一种形式依然存在**：

```sql
-- 主单：id = NEXTVAL(seq)  → 比如 100；单号又 NEXTVAL → 101
-- 明细（每条）：
id       = NEXTVAL('seq_sale_order_no')   -- 102, 103, 104...
order_id = CURRVAL('seq_sale_order_no')   -- 同行 NEXTVAL 之后的当前值 = 102, 103, 104...
```

**每条明细的 order_id = 自己的 id，永远不等于主单 id（100）**。CURRVAL 取的是"本会话最近一次 NEXTVAL"，同一语句里每行 NEXTVAL 后 CURRVAL 即变——这是 PG 序列的基本语义，任何一次真实执行都会暴露（再次证明团队没有跑过）。

**正确做法（二选一，照做即可）**：
- 方案 A：主单 SQL 末尾加 `RETURNING id`，Mapper 返回 Long，Service 拿到后 `items.forEach(i -> i.setOrderId(orderId))` 再批量插
- 方案 B：先 `SELECT NEXTVAL('seq_sale_order_no')` 取出 orderId 变量，主单/明细全部用该绑定值（禁止在 SQL 里混用 NEXTVAL/CURRVAL）

另：单号沿用订单序列表导致单号跳号（主单消耗 2 个值+明细每行 1 个），建议单号走 biz_number_sequence 或独立序列。

### 2. 费用分摊：量级 bug 已修，闭环仍缺库存重算 ⚠️

- ✅ `allocatedCost / quantity` 后再加 unit_cost（HALF_UP 2 位），量级正确
- ❌ **erp_stock 移动加权成本仍未重算**（StockService/erp_stock 零引用）——入库行成本变了、库存账不变，两本账不一致
- ⚠️ `item.getInboundOrderId()` 语义依旧不明（更新的是 `erp_purchase_inbound_item.id`——要求存的是明细行 ID，请团队自查数据流向）；`item.getQuantity()` 为 null 时会 NPE
- ⚠️ 上轮 SQL 同步更新了 line_amount，本轮改后只更新 unit_cost——行金额与单价口径请统一

### 3. 采购前端页：第三轮零改动 ❌

入库列表（149 行无 Tab）、查询三页（109/110/137）、8 误植价格等级列——全部原样。

## 三、管理层必须介入的问题

R2→R5 连续四轮同一模式：**团队报"已修复"，实际未构建/未部署/未执行迁移/未跑过一次功能**。R4 报告已给出交付门禁 6 件套，本轮依然无一提供。建议：

1. **立即执行交付门禁**（构建日志/Flyway 记录/启动日志/带数据 curl/vue-tsc=0/改动清单），由您直接要求
2. 建议指定一名开发按 R4/R5 报告的精确修复说明执行，**改完自己先跑一遍**（启动→curl→DB 查询），再报完成
3. 我也可以提供"陪跑验收"：修复声称完成后，当场逐条跑复验脚本，10 分钟出结果，避免再来回

## 四、第五轮复验条件（不变，继续有效）

1. 新 jar 构建+部署+Flyway V11.33.0 success=t+remark 列存在
2. vue-tsc = 0
3. 报价转订单 E2E：erp_sale_order 新单 + **erp_sale_order_item.order_id = 新单 id**（DB 查询证据，行数=报价明细数）
4. 费用分摊 E2E：complete 后入库行 unit_cost 按数量加权增加 **且 erp_stock 成本同步重算**
5. 采购入库列表/查询三页/8 误植列实际改动
6. 交付 6 件套
