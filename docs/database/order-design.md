# 订单管理模块数据库设计文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 版本 | v1.0.0 |
| 作者 | AI-Ready Team |
| 日期 | 2026-04-27 |
| 数据库 | PostgreSQL 15.x |
| 字符集 | UTF-8 |

---

## 1. 设计概述

### 1.1 设计目标
为 Sprint 27+1 测试环境的订单管理模块提供完整、高效、可扩展的数据库设计方案，支持销售订单、采购订单、退货订单的全生命周期管理。

### 1.2 设计原则
- **规范化设计**：遵循第三范式（3NF），减少数据冗余
- **性能优先**：核心查询路径通过索引优化
- **可扩展性**：预留扩展字段，支持未来业务变更
- **数据安全**：逻辑删除 + 审计字段，保证数据可追溯
- **多租户支持**：所有业务表包含 tenant_id 字段

### 1.3 表清单

| 序号 | 表名 | 中文名 | 说明 |
|------|------|--------|------|
| 1 | `erp_order` | 订单主表 | 存储订单核心信息 |
| 2 | `erp_order_item` | 订单明细表 | 存储订单商品明细 |
| 3 | `erp_order_status_log` | 订单状态流转表 | 记录订单状态变更历史 |
| 4 | `erp_order_payment` | 订单支付信息表 | 存储订单支付记录 |
| 5 | `erp_order_logistics` | 订单物流信息表 | 存储物流跟踪信息 |
| 6 | `erp_order_refund` | 订单退款表 | 存储退款申请记录 |

---

## 2. 表结构设计

### 2.1 订单主表 (erp_order)

存储订单的核心信息，包括客户信息、金额信息、状态信息、地址信息等。

```sql
CREATE TABLE erp_order (
    id                  BIGINT PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    order_no            VARCHAR(32) NOT NULL,
    customer_id         BIGINT NOT NULL,
    customer_name       VARCHAR(100),
    order_type          SMALLINT NOT NULL DEFAULT 1 COMMENT '1-销售订单 2-采购订单 3-退货订单',
    status              SMALLINT NOT NULL DEFAULT 0 COMMENT '0-草稿 1-待审核 2-已审核 3-已发货 4-已完成 5-已取消 6-已退货',
    source              SMALLINT DEFAULT 1 COMMENT '1-手动创建 2-线上下单 3-转单',
    sale_id             BIGINT,
    sale_name           VARCHAR(50),
    dept_id             BIGINT,
    total_amount        NUMERIC(18,4) NOT NULL DEFAULT 0,
    actual_amount       NUMERIC(18,4) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(18,4) NOT NULL DEFAULT 0,
    received_amount     NUMERIC(18,4) NOT NULL DEFAULT 0,
    order_date          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_ship_date  TIMESTAMP,
    actual_ship_date    TIMESTAMP,
    receiver_name       VARCHAR(50),
    receiver_phone      VARCHAR(20),
    receiver_province   VARCHAR(50),
    receiver_city       VARCHAR(50),
    receiver_district   VARCHAR(50),
    receiver_address    VARCHAR(255),
    remark              VARCHAR(500),
    audit_id            BIGINT,
    audit_name          VARCHAR(50),
    audit_time          TIMESTAMP,
    audit_remark        VARCHAR(500),
    ext_info            JSONB,
    deleted             SMALLINT NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);
```

**字段说明**：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键，雪花ID |
| tenant_id | BIGINT | NOT NULL, IDX | 租户ID |
| order_no | VARCHAR(32) | NOT NULL, UK | 订单编号，业务唯一 |
| customer_id | BIGINT | NOT NULL, IDX | 客户ID |
| status | SMALLINT | NOT NULL, IDX | 订单状态 |
| total_amount | NUMERIC(18,4) | NOT NULL | 订单总金额 |
| actual_amount | NUMERIC(18,4) | NOT NULL | 实际应收金额 |
| received_amount | NUMERIC(18,4) | NOT NULL | 已收金额 |
| ext_info | JSONB | | 扩展信息，灵活存储 |

---

### 2.2 订单明细表 (erp_order_item)

存储订单中的商品明细信息。

```sql
CREATE TABLE erp_order_item (
    id                  BIGINT PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    order_id            BIGINT NOT NULL,
    product_id          BIGINT NOT NULL,
    product_name        VARCHAR(200) NOT NULL,
    product_code        VARCHAR(50),
    spec_model          VARCHAR(100),
    unit                VARCHAR(20),
    price               NUMERIC(18,4) NOT NULL DEFAULT 0,
    quantity            NUMERIC(18,4) NOT NULL DEFAULT 0,
    discount_rate       NUMERIC(5,4) DEFAULT 1.0000,
    subtotal            NUMERIC(18,4) NOT NULL DEFAULT 0,
    remark              VARCHAR(255),
    deleted             SMALLINT NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);
```

---

### 2.3 订单状态流转表 (erp_order_status_log)

记录订单从创建到完结的全状态变更历史，支持审计和追溯。

```sql
CREATE TABLE erp_order_status_log (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    order_id            BIGINT NOT NULL,
    order_no            VARCHAR(32) NOT NULL,
    old_status          SMALLINT NOT NULL COMMENT '变更前状态',
    new_status          SMALLINT NOT NULL COMMENT '变更后状态',
    operate_type        SMALLINT NOT NULL DEFAULT 1 COMMENT '1-系统变更 2-人工变更 3-自动流转',
    operator_id         BIGINT COMMENT '操作人ID',
    operator_name       VARCHAR(50) COMMENT '操作人名称',
    operate_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark              VARCHAR(500) COMMENT '操作备注',
    ext_info            JSONB COMMENT '扩展信息',
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**设计说明**：
- 使用 `BIGSERIAL` 自增主键，保证日志顺序性
- 记录 `old_status` → `new_status` 的完整状态转换路径
- `operate_type` 区分系统/人工/自动触发
- `ext_info` 存储差异数据快照（如变更前后的金额对比）

**状态流转规则**：

```
草稿(0) → 待审核(1) → 已审核(2) → 已发货(3) → 已完成(4)
    ↓           ↓           ↓
已取消(5)   已取消(5)   已退货(6)
```

---

### 2.4 订单支付信息表 (erp_order_payment)

存储订单的支付记录，支持多笔支付、多种支付方式。

```sql
CREATE TABLE erp_order_payment (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    order_id            BIGINT NOT NULL,
    order_no            VARCHAR(32) NOT NULL,
    payment_no          VARCHAR(64) NOT NULL COMMENT '支付流水号',
    payment_type        SMALLINT NOT NULL DEFAULT 1 COMMENT '1-现金 2-银行转账 3-支付宝 4-微信支付 5-信用卡 6-账期',
    payment_status      SMALLINT NOT NULL DEFAULT 0 COMMENT '0-待支付 1-支付中 2-支付成功 3-支付失败 4-已退款',
    amount              NUMERIC(18,4) NOT NULL DEFAULT 0 COMMENT '支付金额',
    actual_amount       NUMERIC(18,4) NOT NULL DEFAULT 0 COMMENT '实际到账金额',
    fee_amount          NUMERIC(18,4) DEFAULT 0 COMMENT '手续费',
    payer_name          VARCHAR(100) COMMENT '付款人',
    payer_account       VARCHAR(100) COMMENT '付款账号',
    payee_account       VARCHAR(100) COMMENT '收款账号',
    transaction_id      VARCHAR(100) COMMENT '第三方支付流水号',
    pay_time            TIMESTAMP COMMENT '支付时间',
    expire_time         TIMESTAMP COMMENT '过期时间',
    remark              VARCHAR(500),
    deleted             SMALLINT NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);
```

---

### 2.5 订单物流信息表 (erp_order_logistics)

存储订单的物流跟踪信息，支持多包裹、多次配送。

```sql
CREATE TABLE erp_order_logistics (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    order_id            BIGINT NOT NULL,
    order_no            VARCHAR(32) NOT NULL,
    logistics_no        VARCHAR(50) NOT NULL COMMENT '物流单号',
    logistics_company   VARCHAR(50) COMMENT '物流公司',
    logistics_company_code VARCHAR(20) COMMENT '物流公司编码',
    logistics_status    SMALLINT NOT NULL DEFAULT 0 COMMENT '0-待发货 1-已揽件 2-运输中 3-已签收 4-异常',
    sender_name         VARCHAR(50),
    sender_phone        VARCHAR(20),
    sender_address      VARCHAR(255),
    receiver_name       VARCHAR(50),
    receiver_phone      VARCHAR(20),
    receiver_address    VARCHAR(255),
    weight              NUMERIC(10,3) COMMENT '重量(kg)',
    volume              NUMERIC(10,4) COMMENT '体积(m³)',
    freight_amount      NUMERIC(18,4) DEFAULT 0 COMMENT '运费',
    ship_time           TIMESTAMP COMMENT '发货时间',
    receive_time        TIMESTAMP COMMENT '签收时间',
    remark              VARCHAR(500),
    deleted             SMALLINT NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);
```

**物流状态流转**：

```
待发货(0) → 已揽件(1) → 运输中(2) → 已签收(3)
                    ↓
                  异常(4)
```

---

### 2.6 订单退款表 (erp_order_refund)

存储订单退款申请及处理记录。

```sql
CREATE TABLE erp_order_refund (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    order_id            BIGINT NOT NULL,
    order_no            VARCHAR(32) NOT NULL,
    refund_no           VARCHAR(32) NOT NULL COMMENT '退款单号',
    refund_type         SMALLINT NOT NULL DEFAULT 1 COMMENT '1-全额退款 2-部分退款',
    refund_status       SMALLINT NOT NULL DEFAULT 0 COMMENT '0-待审核 1-审核通过 2-审核拒绝 3-退款中 4-退款完成 5-退款失败',
    refund_amount       NUMERIC(18,4) NOT NULL DEFAULT 0,
    refund_reason       VARCHAR(255),
    refund_reason_type  SMALLINT COMMENT '1-质量问题 2-发错货 3-少发漏发 4-物流损坏 5-客户原因 6-其他',
    attachments         JSONB COMMENT '附件凭证',
    applicant_id        BIGINT COMMENT '申请人ID',
    applicant_name      VARCHAR(50),
    apply_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auditor_id          BIGINT COMMENT '审核人ID',
    auditor_name        VARCHAR(50),
    audit_time          TIMESTAMP,
    audit_remark        VARCHAR(500),
    actual_refund_time  TIMESTAMP COMMENT '实际退款时间',
    transaction_id      VARCHAR(100) COMMENT '退款交易流水号',
    remark              VARCHAR(500),
    deleted             SMALLINT NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. 索引设计

### 3.1 订单主表索引

```sql
-- 唯一索引：订单编号
CREATE UNIQUE INDEX uk_erp_order_order_no ON erp_order(order_no) WHERE deleted = 0;

-- 复合索引：租户+状态（最常用的查询场景）
CREATE INDEX idx_erp_order_tenant_status ON erp_order(tenant_id, status) WHERE deleted = 0;

-- 复合索引：租户+客户（客户订单查询）
CREATE INDEX idx_erp_order_tenant_customer ON erp_order(tenant_id, customer_id) WHERE deleted = 0;

-- 复合索引：租户+日期（按日期范围查询）
CREATE INDEX idx_erp_order_tenant_date ON erp_order(tenant_id, order_date) WHERE deleted = 0;

-- 复合索引：租户+销售人员（销售报表）
CREATE INDEX idx_erp_order_tenant_sale ON erp_order(tenant_id, sale_id, status) WHERE deleted = 0;

-- 单列索引：状态（状态统计）
CREATE INDEX idx_erp_order_status ON erp_order(status) WHERE deleted = 0;
```

### 3.2 订单明细表索引

```sql
-- 复合索引：订单ID查询明细
CREATE INDEX idx_erp_order_item_order_id ON erp_order_item(order_id) WHERE deleted = 0;

-- 复合索引：租户+商品（商品销售统计）
CREATE INDEX idx_erp_order_item_tenant_product ON erp_order_item(tenant_id, product_id) WHERE deleted = 0;
```

### 3.3 状态流转表索引

```sql
-- 复合索引：订单ID+时间（查询某订单状态历史）
CREATE INDEX idx_order_status_log_order_time ON erp_order_status_log(order_id, operate_time);

-- 复合索引：租户+时间（运营分析）
CREATE INDEX idx_order_status_log_tenant_time ON erp_order_status_log(tenant_id, operate_time);
```

### 3.4 支付信息表索引

```sql
-- 唯一索引：支付流水号
CREATE UNIQUE INDEX uk_order_payment_no ON erp_order_payment(payment_no) WHERE deleted = 0;

-- 复合索引：订单ID（查询订单支付记录）
CREATE INDEX idx_order_payment_order ON erp_order_payment(order_id) WHERE deleted = 0;

-- 复合索引：支付状态（支付对账）
CREATE INDEX idx_order_payment_status ON erp_order_payment(payment_status, pay_time) WHERE deleted = 0;
```

### 3.5 物流信息表索引

```sql
-- 复合索引：订单ID
CREATE INDEX idx_order_logistics_order ON erp_order_logistics(order_id) WHERE deleted = 0;

-- 复合索引：物流单号
CREATE INDEX idx_order_logistics_no ON erp_order_logistics(logistics_no) WHERE deleted = 0;
```

---

## 4. 约束设计

### 4.1 外键约束（逻辑外键）

本项目采用**逻辑外键**设计，不在数据库层面强制外键约束，通过应用程序保证数据一致性。原因如下：
- 性能考虑：外键约束会增加写操作开销
- 灵活性：支持分布式架构和分库分表
- 逻辑删除：物理外键与逻辑删除冲突

### 4.2 检查约束

```sql
-- 订单状态有效性检查
ALTER TABLE erp_order ADD CONSTRAINT chk_order_status 
    CHECK (status IN (0, 1, 2, 3, 4, 5, 6));

-- 订单类型有效性检查
ALTER TABLE erp_order ADD CONSTRAINT chk_order_type 
    CHECK (order_type IN (1, 2, 3));

-- 金额非负检查
ALTER TABLE erp_order ADD CONSTRAINT chk_order_amount 
    CHECK (total_amount >= 0 AND actual_amount >= 0 AND received_amount >= 0);

-- 删除标记检查
ALTER TABLE erp_order ADD CONSTRAINT chk_order_deleted 
    CHECK (deleted IN (0, 1));
```

---

## 5. 分区策略（未来扩展）

当单表数据量超过 1000 万时，建议按以下策略分区：

### 5.1 订单主表按时间范围分区

```sql
-- 按月分区示例
CREATE TABLE erp_order_partitioned (
    LIKE erp_order INCLUDING ALL
) PARTITION BY RANGE (order_date);

-- 创建月度分区
CREATE TABLE erp_order_y2026m04 PARTITION OF erp_order_partitioned
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE erp_order_y2026m05 PARTITION OF erp_order_partitioned
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
```

### 5.2 状态流转表按时间范围分区

```sql
CREATE TABLE erp_order_status_log_partitioned (
    LIKE erp_order_status_log INCLUDING ALL
) PARTITION BY RANGE (operate_time);
```

---

## 6. 注释规范

所有表和字段必须添加中文注释：

```sql
COMMENT ON TABLE erp_order IS '订单主表';
COMMENT ON COLUMN erp_order.order_no IS '订单编号，业务唯一';
COMMENT ON COLUMN erp_order.status IS '订单状态：0-草稿 1-待审核 2-已审核 3-已发货 4-已完成 5-已取消 6-已退货';
```

---

## 7. ER 关系图

```
┌─────────────────┐       ┌──────────────────┐
│   erp_order     │◄──────┤ erp_order_item   │
│   (订单主表)     │  1:N  │  (订单明细表)     │
└────────┬────────┘       └──────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────┐
│ erp_order_status_log│
│  (状态流转表)        │
└────────┬────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────┐    ┌─────────────────────┐
│ erp_order_payment   │    │ erp_order_logistics │
│  (支付信息表)        │    │  (物流信息表)        │
└─────────────────────┘    └─────────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────┐
│  erp_order_refund   │
│   (退款表)           │
└─────────────────────┘
```

---

## 8. 附录

### 8.1 数据字典

| 枚举类型 | 值 | 含义 |
|----------|-----|------|
| order_type | 1 | 销售订单 |
| order_type | 2 | 采购订单 |
| order_type | 3 | 退货订单 |
| status | 0 | 草稿 |
| status | 1 | 待审核 |
| status | 2 | 已审核 |
| status | 3 | 已发货 |
| status | 4 | 已完成 |
| status | 5 | 已取消 |
| status | 6 | 已退货 |
| payment_type | 1 | 现金 |
| payment_type | 2 | 银行转账 |
| payment_type | 3 | 支付宝 |
| payment_type | 4 | 微信支付 |
| payment_type | 5 | 信用卡 |
| payment_type | 6 | 账期 |

### 8.2 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0.0 | 2026-04-27 | 初始版本 | AI-Ready Team |
