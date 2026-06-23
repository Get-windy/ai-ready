# 销售订单76字段ERP深度对比分析报告

> 研究日期：2026-06-22
> 对比系统：SAP S/4HANA、用友U8、金蝶K3 WISE、Odoo 18
> 当前项目：AI-Ready (erp_sale_order / erp_sale_order_item)

---

## 一、各ERP系统核心表结构对比

### 1.1 SAP S/4HANA

| 表名 | 说明 |
|------|------|
| **VBAK** | 销售凭证抬头表 (订单主表) |
| **VBAP** | 销售凭证项目表 (订单明细行) |
| **VBEP** | 计划行数据 (交货计划) |
| **KONV** | 定价条件记录 (价格明细,独立存储) |
| **MARA** | 物料主数据 (基础信息) |
| **MARC** | 物料工厂数据 (库存/批次管理) |

**核心设计理念**：
- 价格不存储在VBAP中，而是通过条件技术(KONV)独立存储定价过程
- VBAP中存储的是"快照"字段：MATNR、KWMENG(数量)、VRKME(单位)、NETWR(净值)、WERKS(工厂)
- 物料基本信息（规格、品牌等）通过MATNR关联MARA实时获取，不在订单中冗余
- 双单位通过VRKME(销售单位) + MEINS(基本单位) + 换算关系(KONV)实现
- 批次通过CHARG字段关联MCH1/MCHA表
- 库存通过ATP检查实时获取，不在订单中存储

---

### 1.2 用友U8

| 表名 | 说明 |
|------|------|
| **SO_SOMain** | 销售订单主表 |
| **SO_SODetails** | 销售订单子表 (明细行) |
| **DispatchLists** | 发货单子表 |
| **SaleBillVouchs** | 发票子表 |
| **Inventory** | 存货档案表 |

**核心设计理念**：
- SO_SODetails将大量字段直接存储为物理列（中国ERP传统设计）
- 价格快照：iUnitPrice、iPrice(含税单价)、iMoney(金额)、iSum(价税合计)均存储在订单明细中
- 自定义字段：cDefine1-cDefine16(16个文本) + cFree1-cFree10(10个自由项)
- 累计字段：iFHQuantity(累计发货数)、iFHMoney(累计发货金额)通过发货单回写
- 辅助计量：iQuantity(主数量) + iNum(辅助数量/件数)实现双单位
- 批次通过存货档案的批次管理属性决定，不在订单明细中强制体现

---

### 1.3 金蝶K3 WISE

| 表名 | 说明 |
|------|------|
| **SEOrder** | 销售订单主表 |
| **SEOrderEntry** | 销售订单分录 (明细行) |
| **t_ICItem** | 物料表 (基础数据) |
| **t_ICItemCustom** | 物料自定义属性表 |

**核心设计理念**：
- SEOrderEntry采用简洁设计：FItemId, FQty, FPrice, FAmount, FTaxRate, FDiscount, FNote
- 物料信息(编码/名称/规格/型号/品牌等)全部通过FItemId关联t_ICItem获取
- 价格策略通过客户价格方案(独立表)管理，下单时快照到SEOrderEntry
- 自定义字段存储在物料或单据的自定义属性表中，不直接扩展明细表
- 批次通过SN/批号管理模块单独处理，出库时才指定
- 双单位通过基本单位+辅助单位+换算率实现

---

### 1.4 Odoo 18

| 模型 | 说明 |
|------|------|
| **sale.order** | 销售订单模型 |
| **sale.order.line** | 销售订单行模型 |
| **product.product** | 产品变体模型 |
| **product.template** | 产品模板模型 |
| **stock.quant** | 库存数量模型 |

**核心设计理念(Odoo sale.order.line关键字段)**：
- `product_id` -> product.product (关联产品)
- `product_uom_qty` (数量,Decimal)
- `product_uom` (单位,关联uom.uom)
- `price_unit` (单价,Decimal, compute字段)
- `price_subtotal` (小计,Monetary, compute=存储)
- `price_total` (含税合计,Monetary, compute=存储)
- `discount` (折扣百分比,Float,默认0)
- `tax_id` (税,Many2many)
- `name` (描述,Text)
- `customer_lead` (交付时间,Float)
- `qty_delivered` (已交付数量) -> 实时查询stock.move
- `qty_invoiced` (已开票数量) -> 实时查询account.move.line
- `product_packaging_id` (包装单位,用于双单位)
- `linked_line_id` (关联行,用于套件拆分)
- `state` (行状态,Selection)

**Odoo的关键设计差异**：
1. price_unit是compute字段：由价目表(pricelist)自动计算，但下单时存储快照
2. 产品属性（规格/型号/品牌等）通过product.template的attribute_line_ids(变体属性)管理
3. 库存实时查询，不存储在订单行中
4. 双单位通过product_packaging实现包装单位换算
5. 已发货/已开票数量通过stock.move和account.move.line反向统计

---

## 二、76字段逐字段ERP对比分析

### 第一部分：商品基础信息（字段1-25）

| # | 字段名 | 中文名 | SAP VBAP | 用友U8 SO_SODetails | 金蝶K3 SEOrderEntry | Odoo sale.order.line | 当前AI-Ready SaleOrderItem | 建议存储策略 |
|---|--------|--------|----------|---------------------|---------------------|----------------------|---------------------------|-------------|
| 1 | rowNo | 行号 | POSNR(6位数字) | iRowNo(整数) | FEntryId(自增) | sequence(整数) | lineNo(Integer) | **明细表存储** |
| 2 | action | 操作 | 无对应(PSTYV决定) | 无直接字段 | 无直接字段 | state控制 | 体现为status | **前端逻辑,不存储** |
| 3 | image | 图片 | 不存(关联MARA) | 不存(关联Inventory) | 不存(关联t_ICItem) | product_id.image_1920 | image(String) | **关联product表获取** |
| 4 | productName | 商品名称 | ARKTX(短文本) | cInvName | 通过FItemId关联 | name(描述文本) | productName(String) | **快照到明细表** (防止改名后历史单据显示变化) |
| 5 | itemCode | 货号 | MATNR(40位) | cInvCode | 通过FItemId关联 | product_id.default_code | productCode(String) | **快照到明细表** + 冗余productId关联 |
| 6 | preOrderNo | 预订货单编号 | VGBEL(参考单据) | 关联字段(来源单号) | FSourceBillNo | linked_line_id | preOrderNo(String) | **快照到明细表** |
| 7 | smallUnitBarcode | 小单位条码 | 通过物料主数据 | 关联Inventory | 关联t_ICItem | product_id.barcode | smallUnitBarcode(String) | **关联product表获取** |
| 8 | usePreOrderAmount | 使用预订货款 | 无对应 | 无对应 | 无对应 | 无对应 | usePreOrderAmount(BigDecimal) | **快照到明细表**(业务特有) |
| 9 | barcode | 条码 | 通过MARA | 关联Inventory | 关联t_ICItem | product_id.barcode | barcode(String) | **关联product表获取** |
| 10 | specification | 规格 | 通过MARA | cInvStd | 关联t_ICItem.FModel | product.template描述 | specification(String) | **快照到明细表** (规格可能版本变更) |
| 11 | model | 型号 | 通过MARA | 自由项或自定义 | 关联t_ICItem | product.template属性 | model(String) | **快照到明细表** |
| 12 | area | 区域 | 不存 | 不存 | 不存 | 通过warehouse.location | area(String) | **通过warehouse/location获取** |
| 13 | location | 货位 | LGORT(存储地点) | 不存(通过仓库) | 不存 | stock.move.location_id | location(String) | **快照到明细表**(选货时确定) |
| 14 | origin | 产地 | 通过MARA-MATKL | 自由项/自定义 | 关联t_ICItem | product.template.origin | origin(String) | **关联product表获取** |
| 15 | brand | 品牌 | 通过MARA分类 | 自定义字段 | 关联t_ICItem | product.template属性 | brand(String) | **关联product表获取** |
| 16 | lineAttribute | 商品行属性 | PSTYV(项目类别) | 无对应 | 无对应 | 通过route_id等判断 | lineAttribute(String) | **快照到明细表** |
| 17 | batchCode | 批次条码 | CHARG(10位) | 通过批次管理模块 | 批次管理模块 | lot_id(关联stock.lot) | batchCode(String) | **快照到明细表**(出库时确定) |
| 18 | productionDate | 生产日期 | 通过批次MCH1 | 通过批次 | 通过批次 | lot_id.production_date | productionDate(Timestamp) | **关联批次表获取,可选快照** |
| 19 | shelfLife | 保质期 | 通过物料MARA | 通过存货档案 | 通过物料 | product.template.shelf_life | shelfLife(String) | **关联product表获取** |
| 20 | expiryDate | 到期日期 | 通过批次MCH1 | productionDate+shelfLife计算 | 通过批次 | lot_id.expiration_date | expiryDate(Timestamp) | **快照到明细表**(关键业务字段) |
| 21 | unit | 计价单位 | VRKME(销售单位) | cComUnitCode | FUnitID | product_uom | unit(String) | **快照到明细表** |
| 22 | customField1-10 | 自定义字段 | 通过扩展(CI_结构) | cDefine1-16/cFree1-10 | 自定义属性表 | 通过继承扩展 | customField1-10(混合类型) | **快照到明细表**(用友模式:物理列) |
| 23 | smallUnit | 小单位 | 辅助单位(通过KONV) | 辅助计量单位 | 辅助单位 | product_packaging_id | smallUnit(String) | **关联product包装单位获取** |
| 24 | smallUnitPrice | 小单位单价 | 通过定价条件 | 换算 | 换算 | 换算 | smallUnitPrice(BigDecimal) | **由主单位价格+换算率计算** |
| 25 | smallUnitQuantity | 小单位数量 | 通过换算 | iNum(件数) | 换算 | product_packaging_qty | smallUnitQuantity(BigDecimal) | **由主数量+换算率计算** |

**小结**：商品基础信息中，`productName`/`itemCode`/`specification`/`model`/`location`/`lineAttribute`/`batchCode`/`expiryDate`/`unit` 应快照到明细表。`image`/`barcode`/`origin`/`brand`/`shelfLife` 应通过product_id关联获取。`area`宜通过warehouse/location关系获取。

---

### 第二部分：价格相关（字段26-42）

| # | 字段名 | 中文名 | SAP | 用友 | 金蝶 | Odoo | 当前AI-Ready | 建议策略 |
|---|--------|--------|-----|------|------|------|-------------|---------|
| 26 | latestSaleDate | 最近销售日期 | 实时查询VBAP/VBAK | 实时查询价格记忆表 | 实时查询 | 通过sale.order查询 | latestSaleDate | **通过价格记忆表获取** (erp_price_memory) |
| 27 | latestSalePrice | 最近售价 | 实时查询KONV | 实时查询价格记忆表 | 实时查询 | 通过历史订单查询 | latestSalePrice | **通过价格记忆表获取** |
| 28 | retailPrice | 零售价 | 商品主数据 | 存货档案零售价 | t_ICItem价格字段 | product.lst_price | retailPrice | **关联product表获取** |
| 29 | wholesalePrice | 批发价 | 条件记录 | 存货档案批发价 | 价格方案 | pricelist | wholesalePrice | **关联product表或价目表获取** |
| 30 | lowestPrice | 最低售价 | 条件记录(下限) | 最低售价控制 | 价格控制 | 通过权限/规则 | lowestPrice | **关联product表或价格策略获取** |
| 31 | restaurant | 餐饮店 | 客户组/渠道 | 客户分类 | 客户分类 | customer属性 | restaurant(Boolean) | **快照到明细表**(客户分类标记) |
| 32 | canteen | 食堂团餐 | 客户组 | 客户分类 | 客户分类 | 客户属性 | canteen(Boolean) | **快照到明细表** |
| 33 | vipSelf | 自助vip | 客户组 | 客户等级 | 客户等级 | 客户等级 | vipSelf(Boolean) | **快照到明细表** |
| 34 | largeGroup | 大团餐 | 客户组 | 客户分类 | 客户分类 | 客户属性 | largeGroup(Boolean) | **快照到明细表** |
| 35 | specialCustomer | 特价客户 | 条件记录 | 特价客户 | 特价客户 | 客户标记 | specialCustomer(Boolean) | **快照到明细表** |
| 36 | outRestaurant | 外围餐饮店 | 渠道 | 客户分类 | 客户分类 | 客户属性 | outRestaurant(Boolean) | **快照到明细表** |
| 37 | vipLevel1 | 重点vip01 | 客户等级 | 客户等级 | 客户等级 | 客户等级 | vipLevel1(Boolean) | **快照到明细表**(重复字段,可合并) |
| 38 | vipLevel2 | 连锁vip | 客户等级 | 客户等级 | 客户等级 | 客户等级 | vipLevel2(Boolean) | **快照到明细表**(重复字段,可合并) |
| 39 | availableStock | 可用库存 | 实时ATP检查 | 实时查询现存量 | 实时查询 | stock.quant实时查询 | availableStock | **通过库存服务实时获取** |
| 40 | availableStockConverted | 可用库存换算 | 换算后 | 换算后 | 换算后 | 换算 | availableStockConverted | **由库存服务换算** |
| 41 | bookStock | 账面库存 | 实时查询MARD | 实时查询 | 实时查询 | stock.quant | bookStock | **通过库存服务实时获取** |
| 42 | conversionRelation | 换算关系 | MARM(物料单位换算) | 存货档案换算率 | 单位换算表 | uom换算 | conversionRelation | **关联product单位换算表获取** |

**小结**：价格基准数据(零售价/批发价/最低售价)通过product表或价目表获取。客户分类标记(餐饮店/食堂/vip等)快照到明细表以记录下单时客户的身份。库存类数据必须通过库存服务实时获取，绝不在订单中快照存储（会导致库存数据不一致）。

---

### 第三部分：数量金额相关（字段43-59）

| # | 字段名 | 中文名 | SAP | 用友 | 金蝶 | Odoo | 当前AI-Ready | 建议策略 |
|---|--------|--------|-----|------|------|------|-------------|---------|
| 43 | unshippedQuantity | 未发数量 | KWMENG-LSMENG(计算) | iQuantity-iFHQuantity | 计算 | qty_delivered方法 | unshippedQuantity | **计算字段(quantity-shippedQuantity),不存储** |
| 44 | shippedQuantityDetail | 已发数量 | LSMENG(累计交货) | iFHQuantity(回写) | 累计发货 | qty_delivered(计算) | shippedQuantityDetail | **计算字段,不存储;或回写快照** |
| 45 | quantity | 数量 | KWMENG | iQuantity | FQty | product_uom_qty | quantity | **快照到明细表** |
| 46 | conversionRelation | 换算关系 | 见上面 | 见上面 | 见上面 | 见上面 | conversionRelation | **关联product获取** |
| 47 | unshippedQuantity | 未发数量 | 同上43 | 同上43 | 同上43 | 同上43 | unshippedQuantity(重复字段) | **去重,与43合并** |
| 48 | shippedQuantityDetail | 已发数量 | 同上44 | 同上44 | 同上44 | 同上44 | shippedQuantityDetail(重复) | **去重,与44合并** |
| 49 | unitPrice | 单价 | NETPR(净价) | iUnitPrice(无税) | FPrice | price_unit | unitPrice | **快照到明细表** (核心快照) |
| 50 | amount | 金额 | NETWR(净值) | iMoney(无税金额) | FAmount | price_subtotal | amount | **快照到明细表** (核心快照) |
| 51 | costPrice | 参考成本单价 | WAVWR(成本) | 通过存货核算 | 成本价 | purchase_price(成本) | costPrice | **快照到明细表** (下单时成本) |
| 52 | costAmount | 参考成本金额 | 计算 | 计算 | 计算 | 计算 | costAmount | **计算字段(quantity*costPrice),不存储** |
| 53 | grossProfit | 参考毛利 | 计算 | 计算 | 计算 | margin | grossProfit | **计算字段(amount-costAmount),不存储** |
| 54 | discount | 折扣(%) | 条件记录(KONV) | iDiscount(折扣额) | FDiscount | discount | discountRate(BigDecimal) | **快照到明细表** |
| 55 | outRestaurant | 外围餐饮店 | 重复 | 重复 | 重复 | 重复 | outRestaurant(重复) | **去重,与36合并** |
| 56 | discountedUnitPrice | 折后单价 | 计算 | 计算 | 计算 | 计算 | discountedUnitPrice | **计算字段,不存储** |
| 57 | originalPrice | 折单原价 | 条件记录 | 报价iQuotedPrice | 报价 | 价目表原价 | originalPrice | **快照到明细表** (记录原始定价) |
| 58 | vipLevel1 | 重点vip | 重复 | 重复 | 重复 | 重复 | vipLevel1(重复) | **去重,与37合并** |
| 59 | discountedAmount | 折后金额 | 计算 | 计算 | 计算 | 计算 | discountedAmount | **计算字段,不存储** |

**小结**：数量金额部分的核心快照是 quantity(数量)、unitPrice(单价)、amount(金额)、costPrice(参考成本)、discountRate(折扣)、originalPrice(原价)。计算字段(unshippedQuantity/shippedQuantity/costAmount/grossProfit/discountedUnitPrice/discountedAmount)不应物理存储，应实时计算。

---

### 第四部分：优惠折扣积分（字段60-76）

| # | 字段名 | 中文名 | SAP | 用友 | 金蝶 | Odoo | 当前AI-Ready | 建议策略 |
|---|--------|--------|-----|------|------|------|-------------|---------|
| 60 | discountPercent | 优惠折扣(%) | 条件记录 | 整单折扣 | 整单折扣 | discount | discountPercent | **快照到明细表** (与54discount的区别取决于业务) |
| 61 | favorableUnitPrice | 惠后单价 | 计算 | 计算 | 计算 | 计算 | favorableUnitPrice | **计算字段,不存储** |
| 62 | favorableAmount | 优惠后金额 | 计算 | 计算 | 计算 | 计算 | favorableAmount | **计算字段,不存储** |
| 63 | giftItem | 兑换礼品 | 无直接 | 赠品管理 | 赠品管理 | 通过reward模块 | giftItem(String) | **快照到明细表** |
| 64 | exchangePoints | 兑换积分 | 无直接 | 积分兑换 | 积分 | loyalty模块 | exchangePoints(BigDecimal) | **快照到明细表** |
| 65 | usedPoints | 使用积分 | 无直接 | 积分使用 | 积分 | loyalty模块 | usedPoints(BigDecimal) | **快照到明细表** |
| 66 | volume | 体积(m3) | VOLUM | 存货档案 | 物料 | product.volume | volume(BigDecimal) | **关联product获取,订单需快照** |
| 67 | weight | 重量(kg) | NTGEW/BRGEW | 存货档案 | 物料 | product.weight | weight(BigDecimal) | **关联product获取,订单需快照** |
| 68 | gift | 赠品 | POSAR(项目类型) | 赠品标记 | 赠品标记 | 赠品处理 | gift(Boolean) | **快照到明细表** |
| 69 | remark | 备注 | ARKTX | cMemo | FNote | name | remark(String) | **快照到明细表** |

**小结**：积分/礼品/赠品类字段属于业务特有，快照到明细表。体积/重量可从product获取但订单可能需要记录实际发货值。remark是通用字段，快照。

---

## 三、各ERP系统的分类理念总结

### 3.1 SAP: 三层模型
```
物料主数据(MARA/MARC)  →  [快照字段]  →  VBAP(订单明细)
  基础信息+价格+库存        material/unit/quantity/net_value    
```
- 订单只存最少必要信息
- 通过条件技术(KONV)独立管理定价
- 通过ATP实时检查库存

### 3.2 用友: 宽表模型
```
存货档案(Inventory) →  [大量快照字段]  →  SO_SODetails(订单明细) + 16自定义+10自由项
  基础信息                几乎全量快照
```
- 订单明细是超大宽表
- 价格/折扣/数量全部快照
- 累计发货/开票通过回写更新

### 3.3 金蝶: 精简模型
```
物料表(t_ICItem) →  [最少快照]  →  SEOrderEntry(订单明细) + 自定义属性表
  基础信息            FItemId/FQty/FPrice/FAmount/FDiscount
```
- 订单明细极简
- 所有属性通过FItemId关联
- 自定义通过独立属性表扩展

### 3.4 Odoo: ORM动态模型
```
product.product →  [compute+store]  →  sale.order.line
  属性/变体            关联+计算+存储混合
```
- price_unit是compute但store=True
- delivered/invoiced通过反向统计实时获取
- 通过继承机制扩展字段

---

## 四、最佳实践方案：数据库表结构设计

### 4.1 核心原则

1. **快照原则**：下单时刻确定后不会变化的数据，应快照到订单明细表
2. **关联原则**：随时可能更新且需要最新值的，通过关联表实时获取
3. **计算原则**：可由其他字段推导的，不存储物理列
4. **去重原则**：重复字段(如76字段中的部分字段出现多次)应合并

### 4.2 字段分布方案

#### A. 保留在订单明细表 erp_sale_order_item 中（快照字段，共约35个）

```sql
-- ============================================
-- 建议的 erp_sale_order_item 核心字段
-- ============================================
CREATE TABLE erp_sale_order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,          -- 外键关联erp_sale_order
    
    -- 行基础信息 (快照)
    line_no INTEGER,                    -- 行号 [1]
    product_id BIGINT,                  -- 产品ID (关联键)
    product_code VARCHAR(100),          -- 产品编码/货号 [5] 快照
    product_name VARCHAR(300),          -- 产品名称 [4] 快照
    pre_order_no VARCHAR(100),          -- 预订货单编号 [6] 快照
    use_pre_order_amount DECIMAL(18,2), -- 使用预订货款 [8] 快照
    specification VARCHAR(200),         -- 规格 [10] 快照
    model VARCHAR(200),                 -- 型号 [11] 快照
    location VARCHAR(200),              -- 货位 [13] 快照
    unit VARCHAR(50),                   -- 计价单位 [21] 快照
    line_attribute VARCHAR(200),        -- 商品行属性 [16] 快照
    
    -- 批次/保质期 (快照)
    batch_code VARCHAR(200),            -- 批次条码 [17] 快照
    production_date TIMESTAMP,          -- 生产日期 [18] 快照
    expiry_date TIMESTAMP,              -- 到期日期 [20] 快照
    
    -- 数量 (快照)
    quantity DECIMAL(18,4),             -- 订购数量 [45] 快照
    
    -- 价格金额 (核心快照)
    unit_price DECIMAL(18,2),           -- 单价(不含税) [49] 快照
    amount DECIMAL(18,2),               -- 金额(不含税) [50] 快照
    cost_price DECIMAL(18,2),           -- 参考成本单价 [51] 快照
    original_price DECIMAL(18,2),       -- 原价/折前单价 [57] 快照
    tax_rate DECIMAL(8,2),              -- 税率
    unit_price_with_tax DECIMAL(18,2),  -- 含税单价
    amount_with_tax DECIMAL(18,2),      -- 含税金额
    
    -- 折扣优惠 (快照)
    discount_rate DECIMAL(8,2),         -- 折扣率(%) [54] 快照
    discount_percent DECIMAL(8,2),      -- 优惠折扣(%) [60] 快照
    discount_amount DECIMAL(18,2),      -- 折扣金额
    
    -- 客户分类标记 (快照,记录下单时的客户身份)
    restaurant BOOLEAN DEFAULT FALSE,    -- [31]
    canteen BOOLEAN DEFAULT FALSE,       -- [32]
    vip_self BOOLEAN DEFAULT FALSE,      -- [33]
    large_group BOOLEAN DEFAULT FALSE,   -- [34]
    special_customer BOOLEAN DEFAULT FALSE, -- [35]
    out_restaurant BOOLEAN DEFAULT FALSE,   -- [36]
    vip_level1 BOOLEAN DEFAULT FALSE,    -- [37]
    vip_level2 BOOLEAN DEFAULT FALSE,    -- [38]
    
    -- 积分兑换 (快照)
    gift_item VARCHAR(200),             -- 兑换礼品 [63]
    exchange_points DECIMAL(18,2),      -- 兑换积分 [64]
    used_points DECIMAL(18,2),          -- 使用积分 [65]
    
    -- 赠品 (快照)
    gift BOOLEAN DEFAULT FALSE,          -- 赠品标记 [68]
    
    -- 物流信息 (快照)
    volume DECIMAL(18,4),               -- 体积 [66]
    weight DECIMAL(18,4),               -- 重量 [67]
    
    -- 备注 (快照)
    remark VARCHAR(500),                -- 备注 [69]
    
    -- 双单位 (快照)
    small_unit VARCHAR(50),             -- 小单位 [23]
    small_unit_quantity DECIMAL(18,4),  -- 小单位数量 [25]
    small_unit_price DECIMAL(18,2),     -- 小单位单价 [24]
    
    -- 自定义字段 (快照,10个,参考用友模式物理列)
    custom_field1 DECIMAL(18,2),        -- [22] 数字
    custom_field2 DECIMAL(18,2),
    custom_field3 DECIMAL(18,2),
    custom_field4 VARCHAR(200),         -- 文本
    custom_field5 VARCHAR(200),
    custom_field6 DECIMAL(18,2),
    custom_field7 DECIMAL(18,2),
    custom_field8 BIGINT,               -- 往来单位ID
    custom_field9 BIGINT,               -- 职员ID
    custom_field10 BIGINT,              -- 部门ID
    
    -- 系统字段
    warehouse_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### B. 通过关联 product 表获取（不存储，实时查询，共约8个）

| 字段 | 获取来源 | 说明 |
|------|---------|------|
| image [3] | erp_product.image_url | 产品图片 |
| barcode [9] | erp_product.barcode | 条码 |
| smallUnitBarcode [7] | erp_product.barcode | 小单位条码(同条码) |
| origin [14] | erp_product.origin | 产地 |
| brand [15] | erp_product.brand | 品牌 |
| shelfLife [19] | erp_product.shelf_life_days | 保质期天数 |
| retailPrice [28] | erp_product.retail_price | 零售价 |
| wholesalePrice [29] | erp_product.wholesale_price | 批发价 |

#### C. 通过库存服务/其他服务实时获取（不存储，共约7个）

| 字段 | 获取来源 | 说明 |
|------|---------|------|
| availableStock [39] | 库存服务(erp_stock / WMS) | 实时查询可用库存 |
| availableStockConverted [40] | 库存服务+单位换算 | 可用库存换算 |
| bookStock [41] | 库存服务 | 账面库存 |
| conversionRelation [42/46] | erp_product + 单位换算表 | 大小单位换算关系 |
| lowestPrice [30] | 价格策略引擎 | 最低售价控制 |
| latestSaleDate [26] | erp_price_memory | 最近销售日期 |
| latestSalePrice [27] | erp_price_memory | 最近售价 |

#### D. 计算字段（不物理存储，共约12个）

| 字段 | 计算公式 | 说明 |
|------|---------|------|
| unshippedQuantity [43/47] | quantity - SUM(shipped_quantity) | 通过发货单汇总计算 |
| shippedQuantityDetail [44/48] | SUM(shipped_quantity) | 通过发货单汇总计算 |
| costAmount [52] | quantity * costPrice | 成本金额 |
| grossProfit [53] | amount - costAmount | 参考毛利 |
| discountedUnitPrice [56] | unitPrice * (1 - discountRate/100) | 折后单价 |
| discountedAmount [59] | amount * (1 - discountRate/100) | 折后金额 |
| favorableUnitPrice [61] | discountedUnitPrice * (1 - discountPercent/100) | 惠后单价 |
| favorableAmount [62] | discountedAmount * (1 - discountPercent/100) | 优惠后金额 |

#### E. 去重合并字段

| 原字段 | 重复出现 | 建议 |
|--------|---------|------|
| outRestaurant [36/55] | 两次 | 保留一处 |
| vipLevel1 [37/58] | 两次 | 保留一处 |
| unshippedQuantity [43/47] | 两次 | 保留一处 |
| shippedQuantityDetail [44/48] | 两次 | 保留一处 |
| conversionRelation [42/46] | 两次 | 保留一处 |

#### F. 前端逻辑字段（不存储）

| 字段 | 说明 |
|------|------|
| action [2] | 前端操作按钮状态,由status推导 |
| area [12] | 可通过warehouse.location关联获取,不太大必要 |

---

### 4.3 关联表设计

```sql
-- ============================================
-- 价格记忆表 (已存在: erp_price_memory)
-- ============================================
-- 用于获取最近售价/最近销售日期
CREATE TABLE IF NOT EXISTS erp_price_memory (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    biz_type VARCHAR(20) NOT NULL,       -- SALE/PURCHASE
    product_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    unit_price DECIMAL(20,6) NOT NULL,
    quantity DECIMAL(20,6) DEFAULT 0,
    total_amount DECIMAL(20,2) DEFAULT 0,
    order_date DATE,
    is_latest INTEGER NOT NULL DEFAULT 1,
    ...
);

-- ============================================
-- 产品多单位换算表 (建议新增)
-- ============================================
CREATE TABLE IF NOT EXISTS erp_product_unit_conversion (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    product_id BIGINT NOT NULL,
    from_unit VARCHAR(20) NOT NULL,      -- 来源单位
    to_unit VARCHAR(20) NOT NULL,        -- 目标单位
    conversion_rate DECIMAL(18,6) NOT NULL, -- 换算率
    is_active INTEGER DEFAULT 1,
    ...
);

-- ============================================
-- 发货明细汇总回写 (订单已发数量)
-- ============================================
-- 不在订单明细中直接存储已发数量,
-- 而是通过发货单表(已存在WMS/库存出库表)汇总查询:
-- SELECT COALESCE(SUM(shipped_qty), 0) 
-- FROM erp_dispatch_item 
-- WHERE order_item_id = ? AND status IN ('APPROVED','COMPLETED')
```

---

### 4.4 价格引擎设计（参考ERP最佳实践）

```
取价优先级链:
  客户特定价 > 等级价格 > 最近交易价 > 标准售价

取价流程:
  erp_pricing_rule_config (取价规则配置)
    -> 根据客户等级/产品检查 erp_partner_grade_product_price (客户等级产品价)
    -> 未命中则查 erp_product_grade_price (产品等级价)
    -> 未命中则查 erp_price_memory (历史交易价)
    -> 未命中则使用 erp_product.standard_price (标准售价)

下单快照:
  取价结果 -> 写入 sale_order_item.unit_price / original_price
  历史记录 -> 写入 erp_price_memory (后续取价参考)
```

---

## 五、优化建议优先级

### 高优先级（立即执行）

1. **去重合并**：将76字段中重复出现的字段合并（outRestaurant, vipLevel1, unshippedQuantity, shippedQuantityDetail, conversionRelation 各出现2次，合并后约71个独立字段）

2. **分离计算字段**：将12个计算字段从物理列移除，改为Java层实时计算或数据库视图

3. **分离库存字段**：availableStock, availableStockConverted, bookStock 移到库存服务接口，前端通过API调用获取

### 中优先级（分阶段优化）

4. **分离商品基础信息**：image, barcode, origin, brand, shelfLife 通过product_id关联获取，减轻明细表宽度

5. **分离价格参考字段**：latestSaleDate, latestSalePrice, retailPrice, wholesalePrice, lowestPrice 通过价格引擎/记忆表获取

6. **客户分类标记归一化**：将8个Boolean客户分类标记合并为一个customer_category_json字段(JSON)，或保留在订单明细但建议通过customer表关联

### 低优先级（长期优化）

7. **自定义字段EAV化**：如果未来自定义字段需求超过10个，考虑EAV模式而非物理列

8. **双单位标准化**：建立统一的单位换算表，统一管理大小单位换算

---

## 六、最终方案：推荐字段分布

| 类别 | 存储位置 | 字段数 | 占总比 |
|------|---------|--------|--------|
| 订单明细快照 | erp_sale_order_item 物理列 | ~35个 | 49% |
| 商品表关联获取 | erp_product JOIN | ~8个 | 11% |
| 库存服务实时 | API/WMS查询 | ~7个 | 10% |
| 计算字段 | Java/View即时计算 | ~12个 | 17% |
| 前端/去重 | 不存储 | ~9个 | 13% |

---

> **参考文档**：
> - SAP S/4HANA SD模块：表VBAK/VBAP/VBEP/KONV，条件技术定价
> - 用友U8：SO_SOMain/SO_SODetails，宽表+回写模式
> - 金蝶K3 WISE：SEOrder/SEOrderEntry，精简关联模式
> - Odoo 18：sale.order/sale.order.line，compute+store混合模式
