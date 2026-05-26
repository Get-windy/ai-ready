# 批次/序列号管理模块测试数据

## 1. 测试数据概述

本文档提供批次/序列号管理模块的完整测试数据集，覆盖正常场景、边界场景和异常场景。

**数据范围**:
- 批次数据: 20条
- 序列号数据: 50条
- 流转记录: 100条

---

## 2. 批次测试数据

### 2.1 正常场景数据

#### 批次1: 标准采购批次
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name, specification, unit,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    reserved_quantity, source_type, warehouse_id, warehouse_name, quality_status
) VALUES (
    'B202604290001', 1, 'PROD-001', '测试产品A', '规格A1', '件',
    '2026-04-29', '2027-04-29', 'ACTIVE', 1000.00, 800.00,
    200.00, 'PURCHASE', 1, '主仓库', 'NORMAL'
);
```

#### 批次2: 生产入库批次
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    source_type, warehouse_id, warehouse_name
) VALUES (
    'B202604280001', 2, 'PROD-002', '测试产品B',
    '2026-04-28', '2028-04-28', 'ACTIVE', 500.00, 500.00,
    'PRODUCTION', 1, '主仓库'
);
```

#### 批次3: 退货入库批次
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    source_type, warehouse_id, warehouse_name
) VALUES (
    'B202604270001', 1, 'PROD-001', '测试产品A',
    '2026-04-27', '2027-04-27', 'ACTIVE', 50.00, 50.00,
    'SALE_RETURN', 1, '主仓库'
);
```

### 2.2 边界场景数据

#### 批次4: 临期批次（30天内过期）
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    source_type, warehouse_id, warehouse_name
) VALUES (
    'B202601010001', 3, 'PROD-003', '临期测试产品',
    '2026-01-01', '2026-05-25', 'ACTIVE', 100.00, 100.00,
    'PURCHASE', 1, '主仓库'
);
```

#### 批次5: 已过期批次
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    source_type, warehouse_id, warehouse_name
) VALUES (
    'B202501010001', 4, 'PROD-004', '过期测试产品',
    '2025-01-01', '2026-01-01', 'EXPIRED', 200.00, 0.00,
    'PURCHASE', 1, '主仓库'
);
```

#### 批次6: 零库存批次
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    source_type, warehouse_id, warehouse_name
) VALUES (
    'B202604200001', 5, 'PROD-005', '零库存产品',
    '2026-04-20', '2027-04-20', 'ACTIVE', 0.00, 0.00,
    'PURCHASE', 1, '主仓库'
);
```

#### 批次7: 大数量批次
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    source_type, warehouse_id, warehouse_name
) VALUES (
    'B202604150001', 6, 'PROD-006', '大数量产品',
    '2026-04-15', '2027-04-15', 'ACTIVE', 999999.99, 999999.99,
    'PURCHASE', 1, '主仓库'
);
```

#### 批次8: 隔离中批次
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    source_type, warehouse_id, warehouse_name, quality_status
) VALUES (
    'B202604100001', 7, 'PROD-007', '隔离测试产品',
    '2026-04-10', '2027-04-10', 'QUARANTINED', 100.00, 0.00,
    'PURCHASE', 1, '主仓库', 'QUARANTINED'
);
```

### 2.3 异常场景数据

#### 批次9: 已取消批次
```sql
INSERT INTO batch_number (
    batch_no, product_id, product_code, product_name,
    production_date, expiration_date, batch_status, total_quantity, available_quantity,
    source_type, warehouse_id, warehouse_name
) VALUES (
    'B202604050001', 8, 'PROD-008', '已取消产品',
    '2026-04-05', '2027-04-05', 'CANCELLED', 0.00, 0.00,
    'PURCHASE', 1, '主仓库'
);
```

---

## 3. 序列号测试数据

### 3.1 正常场景数据

#### 序列号1-10: 可用状态
```sql
-- 产品PROD-001的序列号
INSERT INTO serial_number (serial_no, product_id, product_code, product_name, batch_id, batch_no,
    sn_status, sn_stage, manufacturing_date, warranty_period, warranty_start_date, warranty_end_date,
    warehouse_id, quality_status) VALUES
('SN-PROD001-20260429-000001', 1, 'PROD-001', '测试产品A', 1, 'B202604290001',
    'AVAILABLE', 'WAREHOUSE', '2026-04-29', 12, '2026-04-29', '2027-04-29',
    1, 'NORMAL'),
('SN-PROD001-20260429-000002', 1, 'PROD-001', '测试产品A', 1, 'B202604290001',
    'AVAILABLE', 'WAREHOUSE', '2026-04-29', 12, '2026-04-29', '2027-04-29',
    1, 'NORMAL'),
('SN-PROD001-20260429-000003', 1, 'PROD-001', '测试产品A', 1, 'B202604290001',
    'AVAILABLE', 'WAREHOUSE', '2026-04-29', 12, '2026-04-29', '2027-04-29',
    1, 'NORMAL');
```

#### 序列号11-15: 已销售状态
```sql
INSERT INTO serial_number (serial_no, product_id, product_code, product_name, batch_id, batch_no,
    sn_status, sn_stage, manufacturing_date, warranty_period, warranty_start_date, warranty_end_date,
    warehouse_id, sale_order_id, sale_order_no, quality_status) VALUES
('SN-PROD001-20260429-000011', 1, 'PROD-001', '测试产品A', 1, 'B202604290001',
    'IN_USE', 'EOF_CUSTOMER', '2026-04-29', 12, '2026-04-29', '2027-04-29',
    1, 1001, 'SO-20260501-001', 'NORMAL'),
('SN-PROD001-20260429-000012', 1, 'PROD-001', '测试产品A', 1, 'B202604290001',
    'IN_USE', 'EOF_CUSTOMER', '2026-04-29', 12, '2026-04-29', '2027-04-29',
    1, 1001, 'SO-20260501-001', 'NORMAL');
```

### 3.2 边界场景数据

#### 序列号16: 质保临期（30天内）
```sql
INSERT INTO serial_number (serial_no, product_id, product_code, product_name, batch_id, batch_no,
    sn_status, sn_stage, manufacturing_date, warranty_period, warranty_start_date, warranty_end_date,
    warehouse_id, quality_status) VALUES
('SN-PROD002-20250501-000001', 2, 'PROD-002', '测试产品B', 2, 'B202604280001',
    'IN_USE', 'EOF_CUSTOMER', '2025-05-01', 12, '2025-05-01', '2026-05-25',
    1, 'NORMAL');
```

#### 序列号17: 质保已过期
```sql
INSERT INTO serial_number (serial_no, product_id, product_code, product_name, batch_id, batch_no,
    sn_status, sn_stage, manufacturing_date, warranty_period, warranty_start_date, warranty_end_date,
    warehouse_id, quality_status) VALUES
('SN-PROD003-20240401-000001', 3, 'PROD-003', '测试产品C', 3, 'B202604270001',
    'IN_USE', 'EOF_CUSTOMER', '2024-04-01', 12, '2024-04-01', '2025-04-01',
    1, 'NORMAL');
```

#### 序列号18: 维修中状态
```sql
INSERT INTO serial_number (serial_no, product_id, product_code, product_name, batch_id, batch_no,
    sn_status, sn_stage, manufacturing_date, warranty_period, warranty_start_date, warranty_end_date,
    warehouse_id, maintenance_count, last_maintenance_date, quality_status) VALUES
('SN-PROD001-20260429-000018', 1, 'PROD-001', '测试产品A', 1, 'B202604290001',
    'MAINTAINED', 'IN_SERVICE', '2026-04-29', 12, '2026-04-29', '2027-04-29',
    1, 1, '2026-05-15 10:00:00', 'UNDER_REPAIR');
```

#### 序列号19: 多次维修
```sql
INSERT INTO serial_number (serial_no, product_id, product_code, product_name, batch_id, batch_no,
    sn_status, sn_stage, manufacturing_date, warranty_period, warranty_start_date, warranty_end_date,
    warehouse_id, maintenance_count, last_maintenance_date, quality_status) VALUES
('SN-PROD004-20260420-000001', 4, 'PROD-004', '测试产品D', 4, 'B202604200001',
    'IN_USE', 'EOF_CUSTOMER', '2026-04-20', 24, '2026-04-20', '2028-04-20',
    1, 3, '2026-05-10 14:30:00', 'NORMAL');
```

#### 序列号20: 已报废
```sql
INSERT INTO serial_number (serial_no, product_id, product_code, product_name, batch_id, batch_no,
    sn_status, sn_stage, manufacturing_date, warranty_period, warranty_start_date, warranty_end_date,
    warehouse_id, maintenance_count, quality_status) VALUES
('SN-PROD005-20260415-000001', 5, 'PROD-005', '测试产品E', 5, 'B202604150001',
    'SCRAP', 'SCRAPPED', '2026-04-15', 6, '2026-04-15', '2026-10-15',
    1, 5, 'DEFECTIVE');
```

### 3.3 异常场景数据

#### 序列号21: 无质保信息
```sql
INSERT INTO serial_number (serial_no, product_id, product_code, product