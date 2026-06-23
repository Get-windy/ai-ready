# 生产级ERP销售订单明细表字段分布方案

## 一、方案背景

本方案针对AI-Ready项目中销售订单明细表（`erp_sale_order_item`）的字段设计问题，旨在解决原有设计中76个字段全部存储在单一表中的问题，通过参考SAP S/4HANA、用友U8、金蝶K3、Odoo等成熟ERP系统的最佳实践，提出符合生产级标准的字段分布方案。

## 二、设计原则

### 2.1 核心原则
- **快照原则**：下单时确定且不应变化的数据应快照到订单明细表
- **关联原则**：可实时获取的主数据通过关联表获取
- **计算原则**：可通过其他字段推导的数据不存储物理列
- **去重原则**：合并重复字段，减少数据冗余
- **审计原则**：历史订单价格不受主数据变更影响

### 2.2 分布策略
```
┌─────────────────────────────────────────────────────────┐
│                     数据分布策略                         │
│                                                         │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │
│ │  快照存储    │ │  关联获取   │ │  实时查询/计算  │    │
│ │   (35个)    │ │   (8个)     │ │    (19个)      │    │
│ │   物理列     │ │   JOIN      │ │  API/计算字段  │    │
│ └─────────────┘ └─────────────┘ └─────────────────┘    │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │               去重合并 (9个)                    │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## 三、字段分布详情

### 3.1 订单明细表物理列（35个，快照存储）

#### 3.1.1 基础信息字段（10个）
| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| id | BIGINT | 主键 | 自增 |
| order_id | BIGINT | 订单ID | 外键关联erp_sale_order |
| line_no | INTEGER | 行号 | 递增行号 |
| product_id | BIGINT | 产品ID | 关联erp_product |
| product_code | VARCHAR(100) | 产品编码 | 快照 |
| product_name | VARCHAR(300) | 产品名称 | 快照 |
| pre_order_no | VARCHAR(100) | 预订货单编号 | 快照 |
| specification | VARCHAR(200) | 规格 | 快照 |
| model | VARCHAR(200) | 型号 | 快照 |
| location | VARCHAR(200) | 货位 | 快照 |

#### 3.1.2 批次保质期字段（3个）
| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| batch_code | VARCHAR(200) | 批次条码 | 快照 |
| production_date | TIMESTAMP | 生产日期 | 快照 |
| expiry_date | TIMESTAMP | 到期日期 | 快照 |

#### 3.1.3 数量金额字段（12个）
| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| quantity | DECIMAL(18,4) | 订购数量 | 快照 |
| unit_price | DECIMAL(18,2) | 单价 | 快照 |
| amount | DECIMAL(18,2) | 金额 | 快照 |
| cost_price | DECIMAL(18,2) | 参考成本单价 | 快照 |
| original_price | DECIMAL(18,2) | 原价 | 快照 |
| tax_rate | DECIMAL(8,2) | 税率 | 快照 |
| unit_price_with_tax | DECIMAL(18,2) | 含税单价 | 快照 |
| amount_with_tax | DECIMAL(18,2) | 含税金额 | 快照 |
| discount_rate | DECIMAL(8,2) | 折扣率(%) | 快照 |
| discount_percent | DECIMAL(8,2) | 优惠折扣(%) | 快照 |
| discount_amount | DECIMAL(18,2) | 折扣金额 | 快照 |
| use_pre_order_amount | DECIMAL(18,2) | 使用预订货款 | 快照 |

#### 3.1.4 价格等级字段（1个）
| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| price_grade_code | VARCHAR(20) | 价格等级代码 | 关联erp_price_grade_config |

#### 3.1.5 客户相关字段（8个）
| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| customer_grade_code | VARCHAR(20) | 客户等级代码 | 订单时的客户等级 |
| price_source | VARCHAR(50) | 价格来源 | 客户专属/客户等级/标准等 |
| calculated_price | DECIMAL(18,6) | 计算得出的单价 | 订单时价格快照 |
| discount_applied | JSON | 应用的折扣信息 | JSON格式存储折扣详情 |
| gift | BOOLEAN | 赠品标记 | 是否赠品 |
| gift_item | VARCHAR(200) | 兑换礼品 | 快照 |
| exchange_points | DECIMAL(18,2) | 兑换积分 | 快照 |
| used_points | DECIMAL(18,2) | 使用积分 | 快照 |

#### 3.1.6 其他字段（1个）
| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| remark | VARCHAR(500) | 备注 | 快照 |

### 3.2 通过关联获取的字段（8个，实时JOIN）

| 字段名 | 来源表 | 获取方式 | 说明 |
|--------|--------|----------|------|
| image | erp_product.image_url | product_id JOIN | 产品图片 |
| barcode | erp_product.barcode | product_id JOIN | 条码 |
| small_unit_barcode | erp_product.barcode | product_id JOIN | 小单位条码 |
| origin | erp_product.origin | product_id JOIN | 产地 |
| brand | erp_product.brand | product_id JOIN | 品牌 |
| shelf_life | erp_product.shelf_life_days | product_id JOIN | 保质期天数 |
| retail_price | erp_product.retail_price | product_id JOIN | 零售价 |
| wholesale_price | erp_product.wholesale_price | product_id JOIN | 批发价 |

### 3.3 实时查询计算的字段（19个）

| 字段名 | 获取方式 | 说明 |
|--------|----------|------|
| available_stock | 库存服务API | 实时查询可用库存 |
| available_stock_converted | 库存服务+换算 | 可用库存换算结果 |
| book_stock | 库存服务API | 账面库存 |
| conversion_relation | erp_product+单位换算表 | 大小单位换算关系 |
| latest_sale_date | erp_price_memory | 最近销售日期 |
| latest_sale_price | erp_price_memory | 最近售价 |
| lowest_price | 价格策略引擎 | 最低售价控制 |
| unshipped_quantity | 发货单汇总计算 | 未发数量 |
| shipped_quantity_detail | 发货单汇总计算 | 已发数量 |
| cost_amount | 计算字段 | 成本金额 = quantity * cost_price |
| gross_profit | 计算字段 | 参考毛利 = amount - cost_amount |
| discounted_unit_price | 计算字段 | 折后单价 = unit_price * (1 - discount_rate/100) |
| discounted_amount | 计算字段 | 折后金额 = amount * (1 - discount_rate/100) |
| favorable_unit_price | 计算字段 | 惠后单价 = discounted_unit_price * (1 - discount_percent/100) |
| favorable_amount | 计算字段 | 优惠后金额 = discounted_amount * (1 - discount_percent/100) |
| volume | 关联product获取 | 体积 |
| weight | 关联product获取 | 重量 |
| unit | 关联product获取 | 计价单位 |
| line_attribute | 关联product获取 | 商品行属性 |

### 3.4 重复字段去重（9个）
以下字段在原始76个字段中重复出现，已合并：
- out_restaurant（出现2次）→ 保留1个
- vip_level1（出现2次）→ 保留1个  
- unshipped_quantity（出现2次）→ 保留1个
- shipped_quantity_detail（出现2次）→ 保留1个
- conversion_relation（出现2次）→ 保留1个

## 四、相关表结构设计

### 4.1 价格等级配置表
```sql
CREATE TABLE erp_price_grade_config (
    id BIGINT PRIMARY KEY,
    grade_code VARCHAR(20) NOT NULL,        -- 价格等级代码：GRADE1, GRADE2...
    grade_name VARCHAR(100) NOT NULL,       -- 价格等级名称：价格等级1, 餐饮店价格等
    grade_sequence INTEGER NOT NULL,         -- 等级序号：1-10
    is_active BOOLEAN DEFAULT TRUE,          -- 是否启用
    is_system BOOLEAN DEFAULT FALSE,         -- 是否系统预设
    remark VARCHAR(500),                   -- 备注
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_grade_code (grade_code)
);
```

### 4.2 客户等级配置表
```sql
CREATE TABLE erp_customer_grade (
    id BIGINT PRIMARY KEY,
    grade_code VARCHAR(20) NOT NULL,        -- 等级代码：VIP1, WHOLESALE...
    grade_name VARCHAR(100) NOT NULL,       -- 等级名称：VIP客户, 批发商等
    grade_sequence INTEGER NOT NULL,         -- 等级序号
    is_default BOOLEAN DEFAULT FALSE,        -- 是否默认
    is_active BOOLEAN DEFAULT TRUE,          -- 是否启用
    discount_rate DECIMAL(5,2),             -- 默认折扣率
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_grade_code (grade_code)
);
```

### 4.3 客户价格本表
```sql
CREATE TABLE erp_customer_product_price (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,              -- 客户ID
    product_id BIGINT NOT NULL,               -- 产品ID
    price_grade_code VARCHAR(20),             -- 价格等级代码
    unit_price DECIMAL(18,6) NOT NULL,       -- 单价
    min_quantity DECIMAL(18,2),              -- 最小数量
    max_quantity DECIMAL(18,2),              -- 最大数量
    effective_from DATE,                      -- 生效日期
    effective_to DATE,                        -- 失效日期
    is_active BOOLEAN DEFAULT TRUE,           -- 是否启用
    price_source VARCHAR(50) DEFAULT 'CUSTOMER_SPECIFIC', -- 价格来源
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_customer_product (customer_id, product_id),
    INDEX idx_effective_dates (effective_from, effective_to)
);
```

### 4.4 价格记忆表
```sql
CREATE TABLE erp_price_memory (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    biz_type VARCHAR(20) NOT NULL,          -- 业务类型：SALE/PURCHASE
    customer_id BIGINT NOT NULL,             -- 客户ID
    product_id BIGINT NOT NULL,              -- 产品ID
    unit_price DECIMAL(20,6) NOT NULL,      -- 单价
    quantity DECIMAL(20,6) DEFAULT 0,        -- 数量
    total_amount DECIMAL(20,2) DEFAULT 0,    -- 总金额
    order_date DATE,                         -- 订单日期
    is_latest INTEGER NOT NULL DEFAULT 1,    -- 是否最新
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_customer_product (customer_id, product_id),
    INDEX idx_order_date (order_date)
);
```

## 五、价格引擎设计

### 5.1 价格获取优先级链
```
价格获取优先级（从高到低）：
┌────────────────────────────────────────────────────────────┐
│  L1: 客户专属特价（客户+物料精确匹配）                     │
│      SELECT unit_price FROM erp_customer_product_price     │
│      WHERE customer_id = ? AND product_id = ?              │
│                                                            │
│  L2: 客户等级价格（客户等级+物料）                        │
│      通过客户等级 → 价格等级映射 → 产品价格等级表          │
│                                                            │
│  L3: 价格等级标准价（物料的价格等级表）                    │
│      SELECT unit_price FROM erp_product_price_grade        │
│      WHERE product_id = ? AND price_grade_code = ?         │
│                                                            │
│  L4: 历史成交价                                           │
│      SELECT unit_price FROM erp_price_memory               │
│      WHERE customer_id = ? AND product_id = ?              │
│      ORDER BY order_date DESC LIMIT 1                      │
│                                                            │
│  L5: 标准/基础价格（兜底）                                │
│      SELECT retail_price FROM erp_product                  │
│      WHERE id = ?                                          │
└────────────────────────────────────────────────────────────┘
```

### 5.2 价格引擎服务
```java
@Service
public class PriceEngineService {
    
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        PriceCalculationResult result = new PriceCalculationResult();
        
        // 按优先级链依次尝试获取价格
        BigDecimal price = tryGetCustomerSpecificPrice(request);
        if (price != null) {
            result.setCalculatedPrice(price);
            result.setPriceSource("CUSTOMER_SPECIFIC");
            return result;
        }
        
        price = tryGetCustomerGradePrice(request);
        if (price != null) {
            result.setCalculatedPrice(price);
            result.setPriceSource("CUSTOMER_GRADE");
            return result;
        }
        
        price = tryGetProductGradePrice(request);
        if (price != null) {
            result.setCalculatedPrice(price);
            result.setPriceSource("PRODUCT_GRADE");
            return result;
        }
        
        price = tryGetHistoryPrice(request);
        if (price != null) {
            result.setCalculatedPrice(price);
            result.setPriceSource("HISTORY");
            return result;
        }
        
        // 兜底：使用产品标准价格
        price = getProductStandardPrice(request.getProductId());
        result.setCalculatedPrice(price);
        result.setPriceSource("STANDARD");
        
        return result;
    }
}
```

## 六、订单处理流程

### 6.1 订单创建时的字段处理
```java
@Transactional
public SaleOrder createOrder(SaleOrderCreateRequest request) {
    SaleOrder order = new SaleOrder();
    // ... 订单基本信息设置
    
    List<SaleOrderItem> items = new ArrayList<>();
    for (OrderItemRequest itemReq : request.getItems()) {
        SaleOrderItem item = createOrderItem(itemReq, request.getCustomerId());
        items.add(item);
    }
    
    order.setItems(items);
    return saveOrder(order);
}

private SaleOrderItem createOrderItem(OrderItemRequest request, Long customerId) {
    SaleOrderItem item = new SaleOrderItem();
    item.setProductId(request.getProductId());
    item.setQuantity(request.getQuantity());
    
    // 获取客户等级
    String customerGradeCode = getCustomerGradeCode(customerId);
    item.setCustomerGradeCode(customerGradeCode);
    
    // 计算价格
    PriceCalculationRequest priceReq = PriceCalculationRequest.builder()
        .customerId(customerId)
        .productId(request.getProductId())
        .quantity(request.getQuantity())
        .orderDate(new Date())
        .build();
        
    PriceCalculationResult priceResult = priceEngineService.calculatePrice(priceReq);
    
    // 设置价格信息（快照）
    item.setPriceGradeCode(priceResult.getPriceGradeCode());
    item.setPriceSource(priceResult.getPriceSource());
    item.setCalculatedPrice(priceResult.getCalculatedPrice());
    item.setUnitPrice(priceResult.getCalculatedPrice());
    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
    
    // 保存价格到记忆表
    priceMemoryService.recordPrice(
        customerId, 
        request.getProductId(), 
        priceResult.getCalculatedPrice(), 
        new Date()
    );
    
    return item;
}
```

## 七、性能优化策略

### 7.1 索引优化
```sql
-- 订单明细表索引
ALTER TABLE erp_sale_order_item 
ADD INDEX idx_order_id (order_id),
ADD INDEX idx_product_id (product_id),
ADD INDEX idx_customer_grade (customer_grade_code),
ADD INDEX idx_price_grade (price_grade_code),
ADD INDEX idx_calculated_price (calculated_price);

-- 客户价格本表索引
ALTER TABLE erp_customer_product_price 
ADD INDEX idx_customer_product (customer_id, product_id),
ADD INDEX idx_effective_dates (effective_from, effective_to);

-- 价格记忆表索引
ALTER TABLE erp_price_memory 
ADD INDEX idx_customer_product (customer_id, product_id),
ADD INDEX idx_order_date (order_date);
```

### 7.2 缓存策略
- **产品主数据**：使用Redis缓存erp_product表数据
- **价格等级配置**：缓存erp_price_grade_config表数据
- **客户等级配置**：缓存erp_customer_grade表数据
- **价格记忆数据**：近期价格记忆数据可缓存

## 八、实施建议

### 8.1 迁移策略
1. **第一步**：创建新表结构和相关表
2. **第二步**：数据迁移脚本将原76字段数据按新规则分布
3. **第三步**：更新后端业务逻辑适配新字段结构
4. **第四步**：更新前端表单和列配置适配新结构
5. **第五步**：逐步切换流量到新结构

### 8.2 分阶段实施
- **Phase 1**：核心快照字段（数量、价格、基本属性）
- **Phase 2**：客户等级与价格等级映射
- **Phase 3**：价格引擎与历史价格
- **Phase 4**：计算字段与实时查询

## 九、合规性保障

### 9.1 审计合规
- 所有价格在订单创建时快照，确保历史订单价格不变
- 完整的价格计算过程记录，便于追溯和审计
- 价格变更不影响历史订单数据

### 9.2 数据一致性
- 通过事务确保订单创建时的数据一致性
- 价格记忆表自动更新，确保历史价格准确性
- 库存数据通过服务API实时获取，确保一致性

## 十、扩展性考虑

### 10.1 字段扩展
- 新价格等级：新增到erp_price_grade_config表
- 新客户等级：新增到erp_customer_grade表
- 新自定义字段：可按需扩展

### 10.2 业务扩展
- 复杂定价策略：通过价格引擎配置扩展
- 阶梯定价：支持数量/金额阶梯折扣
- 多币种：后续可扩展多币种价格

---
**方案版本**：v1.0  
**创建日期**：2026-06-22  
**参考资料**：SAP S/4HANA、用友U8、金蝶K3、Odoo最佳实践