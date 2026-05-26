 INTO serial_number_info (
    serial_number, material_code, batch_number,
    production_date, status, current_location
) VALUES 
('SN202404300001', 'MAT003', 'BN20240430010', '2024-01-15', 'IN_STOCK', 'WH001-A01'),
('SN202404300002', 'MAT003', 'BN20240430010', '2024-01-15', 'IN_STOCK', 'WH001-A01'),
('SN202404300003', 'MAT003', 'BN20240430010', '2024-01-15', 'SOLD', 'CUS001');
```

**序列号测试场景数据**:
- 在库序列号: 8000个
- 已出库序列号: 5000个
- 待入库序列号: 2000个

#### 4.3.2 采购询价测试数据

**询价单测试数据**:
```sql
-- 询价单数据示例
INSERT INTO purchase_inquiry (
    inquiry_no, inquiry_date, status, 
    material_code, quantity, required_date,
    requester, created_by, created_time
) VALUES 
('INQ20240430001', '2024-04-30', 'PENDING',
 'MAT001', 1000, '2024-05-30',
 '采购员A', 'admin', NOW()),
('INQ20240430002', '2024-04-30', 'SENT',
 'MAT002', 500, '2024-05-20',
 '采购员B', 'admin', NOW()),
('INQ20240430003', '2024-04-30', 'CLOSED',
 'MAT003', 200, '2024-05-15',
 '采购员A', 'admin', NOW());
```

**询价单状态分布**:
- 待审批: 100个
- 已发送: 200个
- 报价中: 150个
- 已截止: 200个
- 已中标: 250个
- 已关闭: 100个

**报价单测试数据**:
```sql
-- 报价单数据示例
INSERT INTO supplier_quotation (
    quotation_no, inquiry_no, supplier_code,
    unit_price, quantity, total_amount,
    delivery_days, payment_terms, status,
    quoted_by, quoted_time
) VALUES 
('QT20240430001', 'INQ20240430002', 'SUP001',
 100.00, 500, 50000.00,
 15, 'NET30', 'SUBMITTED',
 '供应商A', NOW()),
('QT20240430002', 'INQ20240430002', 'SUP002',
 95.00, 500, 47500.00,
 20, 'NET30', 'SUBMITTED',
 '供应商B', NOW()),
('QT20240430003', 'INQ20240430002', 'SUP003',
 105.00, 500, 52500.00,
 10, 'NET15', 'SUBMITTED',
 '供应商C', NOW());
```

**报价单状态分布**:
- 已提交: 1500个
- 已修改: 500个
- 已撤回: 200个
- 已中标: 500个
- 未中标: 300个

#### 4.3.3 销售价格策略测试数据

**价格策略测试数据**:
```sql
-- 客户等级价格策略
INSERT INTO price_strategy (
    strategy_code, strategy_name, strategy_type,
    material_code, customer_level, price,
    effective_date, expiry_date, status
) VALUES 
('PS-VIP-001', 'VIP客户价格', 'CUSTOMER_LEVEL',
 'MAT001', 'VIP', 90.00,
 '2024-01-01', '2024-12-31', 'ACTIVE'),
('PS-STD-001', '普通客户价格', 'CUSTOMER_LEVEL',
 'MAT001', 'NORMAL', 100.00,
 '2024-01-01', '2024-12-31', 'ACTIVE'),
('PS-QTY-001', '数量折扣', 'QUANTITY_DISCOUNT',
 'MAT001', NULL, NULL,
 '2024-01-01', '2024-12-31', 'ACTIVE');

-- 数量折扣阶梯
INSERT INTO quantity_discount_tier (
    strategy_code, tier_no, min_quantity,
    max_quantity, discount_rate
) VALUES 
('PS-QTY-001', 1, 1, 99, 0),
('PS-QTY-001', 2, 100, 499, 0.05),
('PS-QTY-001', 3, 500, 999, 0.10),
('PS-QTY-001', 4, 1000, NULL, 0.15);

-- 促销价格策略
INSERT INTO promotion_price (
    promotion_code, promotion_name, material_code,
    promotion_price, start_time, end_time,
    status
) VALUES 
('PROMO-001', '春季促销', 'MAT001',
 85.00, '2024-04-01 00:00:00', '2024-04-30 23:59:59',
 'ACTIVE'),
('PROMO-002', '五一促销', 'MAT002',
 150.00, '2024-05-01 00:00:00', '2024-05-05 23:59:59',
 'PENDING');

-- 区域定价策略
INSERT INTO region_price (
    region_code, region_name, material_code,
    price, status
) VALUES 
('EAST', '华东地区', 'MAT001', 95.00, 'ACTIVE'),
('SOUTH', '华南地区', 'MAT001', 98.00, 'ACTIVE'),
('NORTH', '华北地区', 'MAT001', 100.00, 'ACTIVE'),
('WEST', '西部地区', 'MAT001', 105.00, 'ACTIVE');
```

**价格策略分布**:
- 客户等级价格: 2000条
- 数量折扣策略: 500条
- 促销价格: 100条
- 区域定价: 800条

#### 4.3.4 供应商门户测试数据

**供应商门户账号**:
```sql
-- 供应商门户用户
INSERT INTO supplier_portal_user (
    supplier_code, username, password,
    email, phone, status, last_login_time
) VALUES 
('SUP001', 'portal_sup001', 'hashed_password',
 'portal@supplier001.com', '13800138001',
 'ACTIVE', '2024-04-29 10:00:00'),
('SUP002', 'portal_sup002', 'hashed_password',
 'portal@supplier002.com', '13800138002',
 'ACTIVE', '2024-04-28 15:30:00'),
('SUP003', 'portal_sup003', 'hashed_password',
 'portal@supplier003.com', '13800138003',
 'ACTIVE', NULL);
```

**门户订单数据**:
```sql
-- 供应商待确认订单
INSERT INTO supplier_order (
    order_no, supplier_code, order_date,
    delivery_date, total_amount, status,
    created_time
) VALUES 
('PO20240430001', 'SUP001', '2024-04-30',
 '2024-05-20', 50000.00, 'PENDING_CONFIRM',
 NOW()),
('PO20240430002', 'SUP001', '2024-04-29',
 '2024-05-18', 30000.00, 'CONFIRMED',
 NOW()),
('PO20240430003', 'SUP002', '2024-04-30',
 '2024-05-25', 75000.00, 'PENDING_CONFIRM',
 NOW());
```

**门户数据分布**:
- 待确认订单: 200个
- 已确认订单: 800个
- 待报价询价: 150个
- 已报价询价: 350个

#### 4.3.5 发票管理测试数据

**发票测试数据**:
```sql
-- 销售发票
INSERT INTO sales_invoice (
    invoice_no, invoice_code, order_no,
    customer_code, invoice_date, amount,
    tax_amount, total_amount, tax_rate,
    status, created_by, created_time
) VALUES 
('INV20240430001', '1100123456', 'SO20240430001',
 'CUS001', '2024-04-30', 100000.00,
 13000.00, 113000.00, 0.13,
 'ISSUED', '开票员A', NOW()),
('INV20240430002', '1100123456', 'SO20240430002',
 'CUS002', '2024-04-30', 50000.00,
 6500.00, 56500.00, 0.13,
 'ISSUED', '开票员A', NOW()),
('INV20240430003', '1100123456', 'SO20240430003',
 'CUS003', '2024-04-29', 80000.00,
 7200.00, 87200.00, 0.09,
 'CERTIFIED', '开票员B', NOW());

-- 采购发票
INSERT INTO purchase_invoice (
    invoice_no, invoice_code, order_no,
    supplier_code, invoice_date, amount,
    tax_amount, total_amount, tax_rate,
    certification_status, created_time
) VALUES 
('PI20240430001', '3100987654', 'PO20240430001',
 'SUP001', '2024-04-30', 50000.00,
 6500.00, 56500.00, 0.13,
 'PENDING', NOW()),
('PI20240430002', '3100987654', 'PO20240430002',
 'SUP002', '2024-04-29', 30000.00,
 3900.00, 33900.00, 0.13,
 'CERTIFIED', NOW()),
('PI20240430003', '3100987654', 'PO20240430003',
 'SUP003', '2024-04-28', 20000.00,
 1800.00, 21800.00, 0.09,
 'CERTIFIED', NOW());
```

**发票状态分布**:
- 已开具待认证: 500张
- 已认证: 1000张
- 认证失败: 50张
- 已作废: 100张
- 已归档: 1350张

**税率分布**:
- 13%税率: 1500张
- 9%税率: 800张
- 6%税率: 500张
- 免税: 200张

---

## 5. 数据生成工具

### 5.1 数据生成脚本

#### 5.1.1 物料主数据生成
```python
# generate_material_data.py
import random
import string
from datetime import datetime, timedelta

def generate_material_code(index):
    return f"MAT{str(index).zfill(6)}"

def generate_material_data(count=1000):
    materials = []
    categories = ['原材料', '半成品', '成品', '辅料', '包材']
    units = ['件', '个', 'kg', 'm', 'L']
    
    for i in range(1, count + 1):
        material = {
            'material_code': generate_material_code(i),
            'material_name': f'测试物料{str(i).zfill(4)}',
            'category': random.choice(categories),
            'unit': random.choice(units),
            'batch_enabled': random.random() < 0.6,
            'serial_enabled': random.random() < 0.2,
            'shelf_life_days': random.choice([90, 180, 365, 730]),
            'default_warehouse': f'WH{random.randint(1, 5):03d}',
            'status': 'ACTIVE'
        }
        materials.append(material)
    
    return materials

# 生成数据
materials = generate_material_data(1000)
# 保存到文件或数据库
```

#### 5.1.2 批次数据生成
```python
# generate_batch_data.py
import random
from datetime import datetime, timedelta

def generate_batch_number(material_code, index):
    date_str = datetime.now().strftime('%Y%m%d')
    return f"BN{date_str}{str(index).zfill(4)}"

def generate_batch_data(materials, count=2000):
    batches = []
    warehouses = ['WH001', 'WH002', 'WH003', 'WH004', 'WH005']
    
    for i in range(1, count + 1):
        material = random.choice([m for m in materials if m['batch_enabled']])
        production_date = datetime.now() - timedelta(days=random.randint(1, 365))
        expiry_date = production_date + timedelta(days=material['shelf_life_days'])
        
        batch = {
            'batch_number': generate_batch_number(material['material_code'], i),
            'material_code': material['material_code'],
            'production_date': production_date.strftime('%Y-%m-%d'),
            'expiry_date':