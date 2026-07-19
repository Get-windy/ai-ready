# 销售订单模块参考架构

## 数据库表结构设计

### 1. 主表：sale_order (20列)
```sql
CREATE TABLE sale_order (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    order_no        VARCHAR(50) NOT NULL,
    order_date      DATE NOT NULL,
    order_type      VARCHAR(20) DEFAULT 'NORMAL',
    status          VARCHAR(20) NOT NULL,
    
    -- 外键关联
    customer_id     BIGINT NOT NULL,
    warehouse_id    BIGINT,
    salesman_id     BIGINT,
    dept_id         BIGINT,
    
    -- 金额汇总
    product_amount  DECIMAL(18,2) DEFAULT 0,
    discount_amount DECIMAL(18,2) DEFAULT 0,
    tax_amount      DECIMAL(18,2) DEFAULT 0,
    total_amount    DECIMAL(18,2) DEFAULT 0,
    
    -- 数量汇总
    total_quantity  DECIMAL(18,2) DEFAULT 0,
    
    -- 其他
    source          VARCHAR(50),
    remark          VARCHAR(500),
    
    -- 审计字段
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);

CREATE INDEX idx_sale_order_tenant ON sale_order(tenant_id);
CREATE INDEX idx_sale_order_customer ON sale_order(customer_id);
CREATE INDEX idx_sale_order_date ON sale_order(order_date);
CREATE INDEX idx_sale_order_status ON sale_order(status);
```

### 2. 客户快照表：sale_order_partner_snapshot (1:1)
```sql
CREATE TABLE sale_order_partner_snapshot (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,
    
    -- 客户信息快照
    customer_name   VARCHAR(200),
    customer_code   VARCHAR(50),
    customer_level  VARCHAR(50),
    customer_grade_code VARCHAR(50),
    
    -- 银行税务信息
    bank_name       VARCHAR(100),
    bank_account    VARCHAR(50),
    tax_no          VARCHAR(50),
    
    -- 其他
    customer_remark VARCHAR(500),
    region          VARCHAR(100),
    snapshot_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_partner_snapshot_order ON sale_order_partner_snapshot(order_id);
```

### 3. 收货地址表：sale_order_delivery_address (1:N)
```sql
CREATE TABLE sale_order_delivery_address (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    address_type    VARCHAR(20) NOT NULL, -- RECEIVER/PICKUP
    
    contact_name    VARCHAR(100),
    phone           VARCHAR(50),
    address         VARCHAR(500),
    
    is_default      BOOLEAN DEFAULT FALSE,
    sequence        INTEGER DEFAULT 0,
    
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_delivery_address_order ON sale_order_delivery_address(order_id);
```

### 4. 收款记录表：sale_order_payment (1:N)
```sql
CREATE TABLE sale_order_payment (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    
    payment_account_id BIGINT,
    payment_method  VARCHAR(50),
    payment_date    DATE,
    amount          DECIMAL(18,2) NOT NULL,
    payment_status  VARCHAR(20),
    
    settlement_method VARCHAR(50),
    reconciliation_date DATE,
    
    remark          VARCHAR(500),
    
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_order ON sale_order_payment(order_id);
```

### 5. 物流信息表：sale_order_logistics (1:N)
```sql
CREATE TABLE sale_order_logistics (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    
    logistics_type  VARCHAR(20) NOT NULL, -- DELIVERY/EXPRESS/PICKUP
    
    -- 配送信息
    delivery_method VARCHAR(50),
    delivery_route_id BIGINT,
    driver_id       BIGINT,
    driver_name     VARCHAR(100),
    vehicle         VARCHAR(100),
    
    -- 快递信息
    logistics_company VARCHAR(100),
    logistics_no    VARCHAR(200),
    waybill_no      VARCHAR(200),
    
    -- 费用
    freight_payer   VARCHAR(50),
    shipping_fee    DECIMAL(18,2) DEFAULT 0,
    cod_amount      DECIMAL(18,2) DEFAULT 0,
    
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_logistics_order ON sale_order_logistics(order_id);
```

### 6. 审核流水表：sale_order_audit_trail (1:N)
```sql
CREATE TABLE sale_order_audit_trail (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    
    action          VARCHAR(20) NOT NULL, -- SUBMIT/APPROVE/REJECT
    operator_id     BIGINT NOT NULL,
    operator_name   VARCHAR(100),
    action_time     TIMESTAMP NOT NULL,
    remark          VARCHAR(500)
);

CREATE INDEX idx_audit_trail_order ON sale_order_audit_trail(order_id);
```

### 7. 积分变动表：sale_order_points_journal (1:1)
```sql
CREATE TABLE sale_order_points_journal (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL UNIQUE,
    
    member_card_no  VARCHAR(64),
    
    prev_points     DECIMAL(18,2) DEFAULT 0,
    earned_points   DECIMAL(18,2) DEFAULT 0,
    used_points     DECIMAL(18,2) DEFAULT 0,
    exchanged_points DECIMAL(18,2) DEFAULT 0,
    current_points  DECIMAL(18,2) DEFAULT 0,
    
    journal_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_points_journal_order ON sale_order_points_journal(order_id);
```

### 8. 自定义字段表：sale_order_ext_field (1:N)
```sql
CREATE TABLE sale_order_ext_field (
    id              BIGINT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    
    field_code      VARCHAR(50) NOT NULL,
    field_type      VARCHAR(20) NOT NULL, -- NUMBER/TEXT/DATE/REF
    field_value_number DECIMAL(18,2),
    field_value_text VARCHAR(500),
    field_value_date DATE,
    field_value_ref_id BIGINT,
    
    sequence        INTEGER DEFAULT 0
);

CREATE INDEX idx_ext_field_order ON sale_order_ext_field(order_id);
CREATE INDEX idx_ext_field_code ON sale_order_ext_field(order_id, field_code);
```

## 后端实体设计

### 主实体：SaleOrder.java
```java
@Data
@TableName("sale_order")
public class SaleOrder {
    private Long id;
    private Long tenantId;
    private String orderNo;
    private LocalDate orderDate;
    private String orderType;
    private String status;
    
    // 外键
    private Long customerId;
    private Long warehouseId;
    private Long salesmanId;
    private Long deptId;
    
    // 金额
    private BigDecimal productAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    
    // 数量
    private BigDecimal totalQuantity;
    
    private String source;
    private String remark;
    
    // 审计
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

### 子实体：SaleOrderPartnerSnapshot.java
```java
@Data
@TableName("sale_order_partner_snapshot")
public class SaleOrderPartnerSnapshot {
    private Long id;
    private Long orderId;
    
    private String customerName;
    private String customerCode;
    private String customerLevel;
    private String customerGradeCode;
    
    private String bankName;
    private String bankAccount;
    private String taxNo;
    
    private String customerRemark;
    private String region;
    private LocalDateTime snapshotTime;
}
```

## 查询示例

### 列表页查询（JOIN）
```java
public Page<SaleOrderListDTO> list(SaleOrderQueryDTO query) {
    return saleOrderMapper.selectPage(query, 
        new QueryWrapper<SaleOrder>()
            .select("so.id", "so.order_no", "so.order_date", "so.status",
                    "so.total_amount", "so.total_quantity",
                    "p.customer_name", "p.customer_code",
                    "w.warehouse_name")
            .leftJoin("sale_order_partner_snapshot p ON p.order_id = so.id")
            .leftJoin("warehouse w ON w.id = so.warehouse_id")
            .eq("so.deleted", 0)
            .eq("so.tenant_id", query.getTenantId())
    );
}
```

### 详情页查询
```java
public SaleOrderDetailDTO getDetail(Long id) {
    // 1. 查主表
    SaleOrder order = getById(id);
    SaleOrderDetailDTO dto = BeanUtils.copy(order, SaleOrderDetailDTO.class);
    
    // 2. 查子表
    dto.setPartnerInfo(partnerSnapshotMapper.selectByOrderId(id));
    dto.setItems(itemMapper.selectByOrderId(id));
    dto.setDeliveryAddresses(addressMapper.selectByOrderId(id));
    dto.setPayments(paymentMapper.selectByOrderId(id));
    dto.setLogistics(logisticsMapper.selectByOrderId(id));
    dto.setAuditTrail(auditMapper.selectByOrderId(id));
    dto.setPointsJournal(pointsMapper.selectByOrderId(id));
    dto.setExtFields(extFieldMapper.selectByOrderId(id));
    
    return dto;
}
```

## 对比：旧设计 vs 新设计

| 维度 | 旧设计 | 新设计 |
|------|--------|--------|
| 主表字段数 | 130+ | 20 |
| 子表数量 | 1 (order_item) | 8 |
| 列表查询字段 | 130+ | 20 |
| 更新冲突 | 高（多人修改同一行） | 低（分散到不同表） |
| 扩展性 | 差（加字段改主表） | 好（加子表或ext_field） |
| 职责清晰度 | 差（混杂7个模块） | 好（每个表单一职责） |
