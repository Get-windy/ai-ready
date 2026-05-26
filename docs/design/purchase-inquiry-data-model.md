# 采购询价/报价管理 - 数据模型设计

## 📋 文档信息

| 项目 | 内容 |
|------|------|
| **版本** | 1.0.0 |
| **创建日期** | 2026-04-28 |
| **作者** | 产品分析师 |
| **项目** | AI-Ready Sprint 28 |

---

## 🗄️ 实体关系图

```
┌─────────────────┐       ┌──────────────────┐       ┌─────────────────┐
│  PurchaseInquiry │ 1   N │PurchaseInquiryItem│       │   Supplier      │
│    (询价单主表)  │◄──────►│   (询价明细表)   │       │   (供应商表)    │
└────────┬────────┘       └──────────────────┘       └─────────────────┘
         │
         │ 1
         │
         ▼ N
┌─────────────────┐       ┌──────────────────┐
│PurchaseSupplierQuote│ 1 │ PurchaseQuoteItem │
│   (供应商报价表) │◄──────►│   (报价明细表)   │
└────────┬────────┘       └──────────────────┘
         │
         │ N
         │
         ▼ 1
┌─────────────────┐
│  PurchaseOrder  │
│  (采购订单表)   │
└─────────────────┘
```

---

## 📊 数据表设计

### 1. purchase_inquiry (询价单主表)

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | BIGINT | - | 是 | 自增 | 主键 |
| inquiry_no | VARCHAR | 32 | 是 | - | 询价单号，唯一 |
| title | VARCHAR | 200 | 是 | - | 询价标题 |
| inquiry_type | VARCHAR | 20 | 是 | - | 询价类型 |
| status | VARCHAR | 20 | 是 | DRAFT | 状态枚举 |
| requirement_desc | TEXT | - | 否 | - | 需求描述 |
| urgency_level | VARCHAR | 10 | 否 | NORMAL | 紧急程度 |
| deadline_date | DATETIME | - | 是 | - | 报价截止日期 |
| publish_date | DATETIME | - | 否 | - | 发布日期 |
| close_date | DATETIME | - | 否 | - | 关闭日期 |
| department_id | BIGINT | - | 是 | - | 部门ID |
| requester_id | BIGINT | - | 否 | - | 申请人ID |
| purchaser_id | BIGINT | - | 是 | - | 采购专员ID |
| invited_supplier_ids | TEXT | - | 否 | - | 邀请供应商ID列表，JSON格式 |
| quote_count | INT | - | 否 | 0 | 已收报价数量 |
| approval_status | VARCHAR | 20 | 否 | PENDING | 审批状态 |
| approved_by | BIGINT | - | 否 | - | 审批人ID |
| approval_date | DATETIME | - | 否 | - | 审批日期 |
| approval_comment | TEXT | - | 否 | - | 审批意见 |
| related_order_id | BIGINT | - | 否 | - | 关联采购订单ID |
| created_by | BIGINT | - | 是 | - | 创建人ID |
| created_at | DATETIME | - | 是 | 当前时间 | 创建时间 |
| updated_by | BIGINT | - | 否 | - | 更新人ID |
| updated_at | DATETIME | - | 否 | - | 更新时间 |
| deleted | TINYINT | - | 是 | 0 | 逻辑删除标记 |

**索引设计**:
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_inquiry_no` (`inquiry_no`)
- KEY `idx_status` (`status`)
- KEY `idx_purchaser` (`purchaser_id`)
- KEY `idx_deadline` (`deadline_date`)
- KEY `idx_created_at` (`created_at`)

---

### 2. purchase_inquiry_item (询价明细表)

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | BIGINT | - | 是 | 自增 | 主键 |
| inquiry_id | BIGINT | - | 是 | - | 询价单ID，外键 |
| item_no | INT | - | 是 | - | 明细序号 |
| material_name | VARCHAR | 200 | 是 | - | 物料名称 |
| specification | TEXT | - | 否 | - | 规格型号 |
| unit | VARCHAR | 20 | 是 | - | 计量单位 |
| quantity | DECIMAL | 18,4 | 是 | - | 采购数量 |
| expected_brand | VARCHAR | 200 | 否 | - | 期望品牌 |
| quality_level | VARCHAR | 20 | 否 | - | 质量等级要求 |
| delivery_location | VARCHAR | 200 | 否 | - | 交付地点 |
| item_note | TEXT | - | 否 | - | 明细备注 |
| created_at | DATETIME | - | 是 | 当前时间 | 创建时间 |

**索引设计**:
- PRIMARY KEY (`id`)
- KEY `idx_inquiry_id` (`inquiry_id`)
- KEY `idx_item_no` (`inquiry_id`, `item_no`)

---

### 3. purchase_supplier_quote (供应商报价表)

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | BIGINT | - | 是 | 自增 | 主键 |
| quote_no | VARCHAR | 32 | 是 | - | 报价单号，唯一 |
| inquiry_id | BIGINT | - | 是 | - | 询价单ID，外键 |
| supplier_id | BIGINT | - | 是 | - | 供应商ID |
| quote_status | VARCHAR | 20 | 是 | SUBMITTED | 报价状态 |
| quote_date | DATETIME | - | 是 | 当前时间 | 报价日期 |
| valid_until | DATETIME | - | 是 | - | 有效期至 |
| total_amount | DECIMAL | 18,2 | 是 | 0.00 | 总金额 |
| tax_rate | DECIMAL | 5,2 | 否 | 13.00 | 税率 |
| tax_amount | DECIMAL | 18,2 | 否 | 0.00 | 税额 |
| payment_terms | TEXT | - | 否 | - | 付款条款 |
| delivery_terms | TEXT | - | 否 | - | 交付条款 |
| warranty_terms | TEXT | - | 否 | - | 质保条款 |
| supplier_note | TEXT | - | 否 | - | 供应商备注 |
| competitive_advantage | TEXT | - | 否 | - | 竞争优势说明 |
| attachment_urls | TEXT | - | 否 | - | 附件URL列表，JSON格式 |
| price_score | DECIMAL | 5,2 | 否 | - | 价格评分 |
| quality_score | DECIMAL | 5,2 | 否 | - | 质量评分 |
| service_score | DECIMAL | 5,2 | 否 | - | 服务评分 |
| total_score | DECIMAL | 5,2 | 否 | - | 综合评分 |
| is_recommended | TINYINT | - | 否 | 0 | 是否推荐 |
| recommend_reason | TEXT | - | 否 | - | 推荐理由 |
| review_status | VARCHAR | 20 | 否 | - | 审查状态 |
| reviewed_by | BIGINT | - | 否 | - | 审查人ID |
| review_date | DATETIME | - | 否 | - | 审查日期 |
| review_comment | TEXT | - | 否 | - | 审查意见 |
| created_by | BIGINT | - | 是 | - | 创建人ID |
| created_at | DATETIME | - | 是 | 当前时间 | 创建时间 |
| updated_at | DATETIME | - | 否 | - | 更新时间 |

**索引设计**:
- PRIMARY KEY (`id`)
- UNIQUE KEY `uk_quote_no` (`quote_no`)
- KEY `idx_inquiry_id` (`inquiry_id`)
- KEY `idx_supplier_id` (`supplier_id`)
- KEY `idx_quote_status` (`quote_status`)
- KEY `idx_total_score` (`inquiry_id`, `total_score`)

---

### 4. purchase_quote_item (报价明细表)

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | BIGINT | - | 是 | 自增 | 主键 |
| quote_id | BIGINT | - | 是 | - | 报价单ID，外键 |
| inquiry_item_id | BIGINT | - | 是 | - | 关联询价明细ID |
| material_name | VARCHAR | 200 | 是 | - | 物料名称 |
| specification | TEXT | - | 否 | - | 规格型号 |
| unit | VARCHAR | 20 | 是 | - | 计量单位 |
| quantity | DECIMAL | 18,4 | 是 | - | 报价数量 |
| unit_price | DECIMAL | 18,2 | 是 | - | 含税单价 |
| amount | DECIMAL | 18,2 | 是 | 0.00 | 金额 |
| tax_rate | DECIMAL | 5,2 | 否 | 13.00 | 税率 |
| tax_amount | DECIMAL | 18,2 | 否 | 0.00 | 税额 |
| brand | VARCHAR | 200 | 否 | - | 品牌 |
| model | VARCHAR | 200 | 否 | - | 型号 |
| quality_level | VARCHAR | 20 | 否 | - | 质量等级 |
| origin_country | VARCHAR | 50 | 否 | - | 产地 |
| lead_time | INT | - | 否 | - | 交货周期（天） |
| delivery_location | VARCHAR | 200 | 否 | - | 交货地点 |
| item_note | TEXT | - | 否 | - | 明细备注 |
| created_at | DATETIME | - | 是 | 当前时间 | 创建时间 |

**索引设计**:
- PRIMARY KEY (`id`)
- KEY `idx_quote_id` (`quote_id`)
- KEY `idx_inquiry_item` (`inquiry_item_id`)

---

## 🔗 关联关系

### 外键约束

| 子表 | 字段 | 父表 | 字段 | 级联操作 |
|------|------|------|------|----------|
| purchase_inquiry_item | inquiry_id | purchase_inquiry | id | CASCADE DELETE |
| purchase_supplier_quote | inquiry_id | purchase_inquiry | id | CASCADE DELETE |
| purchase_quote_item | quote_id | purchase_supplier_quote | id | CASCADE DELETE |
| purchase_quote_item | inquiry_item_id | purchase_inquiry_item | id | RESTRICT |

---

## 📋 枚举值定义

### InquiryStatus (询价单状态)

| 枚举值 | 说明 | 允许的操作 |
|--------|------|-----------|
| DRAFT | 草稿 | 编辑、删除、提交审批 |
| PENDING | 待审批 | 审批通过/驳回 |
| PUBLISHED | 已发布 | 关闭、查看报价 |
| QUOTING | 报价中 | 关闭、查看报价 |
| CLOSED | 已关闭 | 查看、生成订单 |
| CANCELLED | 已取消 | 仅查看 |

### QuoteStatus (报价状态)

| 枚举值 | 说明 | 允许的操作 |
|--------|------|-----------|
| SUBMITTED | 已提交 | 审查、撤回 |
| REVIEWED | 已审查 | 接受、拒绝 |
| ACCEPTED | 已接受 | 生成订单 |
| REJECTED | 已拒绝 | 仅查看 |
| WITHDRAWN | 已撤回 | 重新提交 |

### ApprovalStatus (审批状态)

| 枚举值 | 说明 |
|--------|------|
| PENDING | 待审批 |
| APPROVED | 已通过 |
| REJECTED | 已驳回 |

---

## 🧮 业务规则

### 1. 金额计算规则

```sql
-- 报价明细金额计算
amount = quantity × unit_price
tax_amount = amount × tax_rate / 100

-- 报价单总金额计算
total_amount = SUM(quote_item.amount)
total_tax = SUM(quote_item.tax_amount)
```

### 2. 评分计算规则

```sql
-- 综合评分计算
total_score = price_score × 0.4 + quality_score × 0.35 + service_score × 0.25

-- 价格评分算法（最低价得100分）
price_score = (min_price / current_price) × 100
```

### 3. 编号生成规则

| 编号类型 | 格式 | 示例 |
|----------|------|------|
| 询价单号 | INQ-YYYYMMDD-XXXX | INQ-20260428-0001 |
| 报价单号 | QUO-YYYYMMDD-XXXX | QUO-20260428-0001 |

---

## 📈 性能考虑

### 索引优化建议

1. **查询场景**: 按状态查询询价单
   - 索引: `idx_status`

2. **查询场景**: 采购员查看自己的询价单
   - 索引: `idx_purchaser`

3. **查询场景**: 查看询价单的所有报价
   - 索引: `idx_inquiry_id` (purchase_supplier_quote)

4. **查询场景**: 比价分析（按评分排序）
   - 索引: `idx_total_score`
