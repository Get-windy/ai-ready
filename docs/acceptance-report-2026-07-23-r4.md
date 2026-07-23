# 验收审查报告（第四轮）

> 日期：2026-07-23（R4）｜ 验收人：PM（AI）
> **总体判定：❌ 不通过。且本轮暴露的问题性质升级：修复仅停留在源码层——未构建、未部署、未迁移、未自验；源码内还发现断链与量级 bug。**

---

## 第一部分：交付物状态核查（开门见山）

| 检查 | 事实 | 判定 |
|---|---|---|
| 后端 jar | 仍是 10:31 构建的 0.3.9（R3 同一产物）；CostSharingServiceImpl 改于 11:23、QuotationMapper 改于 11:19，**均在新 jar 之后** | ❌ 修复未构建 |
| 运行实例 | PID 39752 运行的是旧 jar（R3 代码） | ❌ 修复未部署 |
| V11.33.0 迁移 | 文件已备（remark 补列 + seq_sale_order_no 序列），但 **flyway_schema_history 无记录、md_payment_method 仍无 remark 列** | ❌ 迁移未执行 |
| vue-tsc | **19 错误，与 R3 完全相同，零进展** | ❌ 基线未清 |
| mvn 编译 | crm + erp-purchase 源码编译通过（exit=0）——唯一正常项 | ✅ |

**结论：团队交付的是"源码修改"，不是"可运行系统"。连续第三轮出现交付物自身未经验证的情况。**

## 第二部分：源码级修复审查（在未部署的代码里发现的实质缺陷）

### 🔴 报价转订单：断链 + 根本性设计错误（不通过）
- `QuotationServiceImpl.createSaleOrder` 中，明细插入调用点被一段 **十六进制垃圾注释 `// 668265e0660e7ec663d25165`** 取代——`insertSaleOrderItems` 定义了但**全仓零调用**（grep 证实），明细依然不会插入
- 即使补上调用，`insertSaleOrderItems` 的 SQL 也是错的：**`order_id = NEXTVAL('seq_sale_order_no')`**——每条明细取一次新序列值，与主单 id（主单插入时已消耗过序列值）**永远对不上**，明细将成为孤儿行
- 主单 SQL 本身已改进 ✅（source_type/source_id/source_no 规范关联 + 序列单号），但 NEXTVAL 连用两次（id 与单号各取一次）浪费且口径混乱
- **返工要求**：主单插入用 `RETURNING id`（或先取 currval）拿到新单 id → 明细行 order_id 统一赋该值 → 批量插入；删除垃圾注释

### 🟠 费用分摊：回写已加但有量级 bug 与目标歧义（不通过）
- 进展 ✅：`complete()` 新增 `updateInboundItemCost` 回写（成本=原+分摊）
- 缺陷 1（量级 bug）：SQL 为 `unit_cost = unit_cost + #{allocatedCost}`——allocatedCost 是**行级总分摊额**，直接加到**单位成本**上。10 件商品分摊 100 元，单位成本会 +100 而非 +10，**成本放大数量倍**
- 缺陷 2（目标歧义）：调用传 `item.getInboundOrderId()`（命名是"入库单ID"），而更新目标是 `erp_purchase_inbound_item.id`（明细行 ID）——若存的是单号而非行号，更新打错行或不生效
- 缺陷 3：仍 **0 处 StockService 引用**，erp_stock 移动加权成本未重算——库存账与入库行成本将不一致
- **返工要求**：分摊额 ÷ 数量入 unit_cost（或仅累计 line 级分摊额后重算单价）；明确行 ID 语义；complete 同步重算 erp_stock 成本并留流水；E2E 用"2 行 × 不同数量"用例验证

### ✅ 源码层面真实进展（值得肯定）
- V11.33.0 迁移内容正确（remark 补列 IF NOT EXISTS + 序列）
- 报价主单 SQL 规范化（source_type 关联）
- crm/erp-purchase 编译绿

## 第三部分：采购域页面复审（第二轮起冻结）

前端文件 mtime 取证：`erp/purchase/form.vue` 最后改动 7-22 21:18、`purchase/inbound/index.vue` 7-19——**连续两轮零改动**。

| 页 | 判定（维持） |
|---|---|
| 采购订单表单 | ⚠️ 8 误植价格等级列仍在、缺红冲/附件 |
| 采购订单列表 | ⚠️ 缺状态主 Tab |
| 采购入库单表单 | ✅ 达标（仅剩 1 个 vue-tsc 标注错误） |
| 采购入库单列表 | ❌ 149 行无 Tab |
| 费用分摊 | 🔴 后端量级 bug（见上） |
| 补货四页 | ⚠️ 待 E2E |
| 查询三页 | ❌ 未返工 |

## 第四部分：流程性问题（必须管理动作，不只是代码）

连续三轮（R2 起）出现同一模式：**"已修复"≠ 已验证**。建议对开发团队强制执行**交付门禁**（每次交付必须附带 6 件套，缺一不收）：

1. 构建日志（mvn install 成功尾部）
2. Flyway 记录查询（新迁移 success=t）
3. 启动日志（端口监听成功）
4. 新端点 curl 实测输出（带数据的，不是空壳 200）
5. vue-tsc 输出（error 计数 = 0）
6. 本批改动文件清单（git status）

## 第五轮复验条件

1. 新 jar 构建 + 部署 + Flyway V11.33.0 success=t + remark 列存在
2. vue-tsc = 0
3. 报价转订单 E2E：erp_sale_order 新单 **+ erp_sale_order_item 行数=报价明细行数且 order_id 正确关联**（DB 查询证据）
4. 费用分摊 E2E：2 行明细（数量 10/20）分摊 60 元 → unit_cost 分别 +2/+2（按数量加权）或按规则正确 + erp_stock 成本重算证据
5. 采购入库列表/查询三页/8 误植列 实际改动
6. 交付 6 件套齐全
