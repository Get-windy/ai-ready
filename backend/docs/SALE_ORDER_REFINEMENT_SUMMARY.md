# 销售订单表单生产级重构总结

## 项目背景

当前销售订单表单采用"宽表模式"，将所有字段分布在 SaleOrder（表头）和 SaleOrderItem（表体/明细）两个主要实体中，其中 SaleOrderItem 包含过多字段，不符合生产级 ERP 设计规范。根据成熟 ERP 系统的设计理念，需将字段按策略重新分布。

## 重构目标

- 将字段按策略分类：快照存储、关联 JOIN、计算/API 查询
- 解决 8 个价格等级布尔字段的错误设计
- 移除冗余的合计字段
- 集成价格引擎实现动态价格计算

## 数据分布策略

### 表头字段 (SaleOrder 实体) - 快照存储
- 基础快照：id, tenantId, orderNo, customerId, customerName, orderDate, expectedShipDate, status, saleType
- 联系信息快照：shippingAddress, receiverName, receiverPhone
- 财务快照：paymentAccountId
- 物流快照：logisticsCompany, logisticsNo, shippingFee
- 会员快照：memberCardNo, memberName, memberDiscount
- 备注快照：orderRemark, buyerRemark, remark
- 扩展信息：extInfo (JSON)
- 客户等级快照：customerGradeCode, customerGradeName
- 系统字段：deleted, createTime, updateTime, createBy, updateBy

### 表体字段 (SaleOrderItem 实体) - 分布式存储

**快照存储 (~50 字段)：**
- 基础快照：id, orderId, lineNo, productId, productCode, productName, preOrderNo, specification, model, location, remark
- 批次保质期快照：batchCode, productionDate, expiryDate
- 数量快照：quantity
- 价格快照：unitPrice, amount, costPrice, originalPrice, taxRate, unitPriceWithTax, amountWithTax, discountRate, discountPercent, discountAmount, usePreOrderAmount, calculatedPrice
- 客户价格等级快照：customerGradeCode, customerGradeName, priceGradeCode, priceSource, discountApplied
- 营销快照：gift, giftItem, exchangePoints, usedPoints
- 包装快照：bigPack, midPack, smallPack
- 区域快照：area
- 小单位快照：smallUnit, smallUnitPrice, smallUnitQuantity
- 自定义字段：customField1-10

**JOIN获取 (~12 字段)：**
- 通过 erp_product 关联获取：image, barcode, smallUnitBarcode, origin, brand, shelfLife, retailPrice, wholesalePrice, unit, lineAttribute, volume, weight

**实时计算 (~19 字段)：**
- 库存相关：availableStock, availableStockConverted, bookStock, conversionRelation
- 价格记忆：latestSaleDate, latestSalePrice, lowestPrice
- 出库相关：unshippedQuantity, shippedQuantityDetail, shippedQuantity
- 金额计算：costAmount, grossProfit, discountedUnitPrice, discountedAmount, favorableUnitPrice, favorableAmount, taxAmount

## 主要改进点

1. **价格等级重构**：
   - 移除了 8 个错误的价格等级布尔字段
   - 引入了客户等级体系(customerGradeCode/CustomerGradeName)
   - 添加了价格等级代码(priceGradeCode)和价格来源(priceSource)
   - 通过价格引擎动态计算价格

2. **字段精简**：
   - 移除了表头的合计字段(totalAmount, taxAmount, totalAmountWithTax)
   - 这些字段通过明细实时计算得出

3. **数据关联优化**：
   - 产品相关信息通过 JOIN 获取，减少冗余存储
   - 实时计算字段在查询时动态生成

4. **价格引擎集成**：
   - 与价格引擎服务集成
   - 根据客户等级自动计算价格
   - 支持多种定价策略

## 技术实现

### 修正的服务层逻辑

在 `SaleOrderServiceImpl.java` 中：

1. **修复了缺失导入**：添加了ProductService、Set、PricingStrategy等相关导入
2. **修正了价格计算请求**：使用正确的方式创建PriceCalculationRequest实例
3. **改进了批量产品查询**：使用listByIds()替代不存在的getMapByIds()方法
4. **完善了价格等级处理**：基于客户等级和应用策略生成价格等级代码
5. **增强了价格快照机制**：将计算结果快照存储到订单明细中

### 数据库迁移

创建了数据库迁移脚本来添加新字段和优化索引：

- 添加了 customer_grade_code, customer_grade_name, price_grade_code, price_source, calculated_price, discount_applied 字段
- 为常用查询字段创建了索引

## 验证结果

1. **完整性验证**：所有字段均基于后端代码和数据库表结构
2. **无冗余实现**：移除了与设计冲突的冗余字段和错误实现
3. **数据一致性**：JOIN字段能正确关联，计算字段能实时计算
4. **价格等级修正**：8个boolean标记正确替换为客户等级体系
5. **表头表体分离**：表头不存储应由明细计算的合计字段

## 生产级特性

1. **分布式存储**：符合现代ERP系统设计规范
2. **快照机制**：关键数据快照存储，保证数据一致性
3. **实时查询**：动态关联和计算，保证数据实时性
4. **扩展性**：预留自定义字段，便于业务扩展
5. **性能优化**：合理索引和JOIN策略

## 总结

本次重构成功地将销售订单表单从传统的宽表模式转换为生产级的分布式存储模式，解决了原有设计中的冗余字段、价格等级错误实现等问题，引入了现代化的ERP设计理念，提高了系统的可维护性和扩展性。