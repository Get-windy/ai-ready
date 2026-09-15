-- 创建产品表
CREATE TABLE IF NOT EXISTS erp_product (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit VARCHAR(20),
    category VARCHAR(100),
    spec VARCHAR(200),
    status VARCHAR(20) DEFAULT 'ENABLED',
    remark TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50)
);

-- 创建仓库表
CREATE TABLE IF NOT EXISTS erp_warehouse (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    warehouse_code VARCHAR(50) NOT NULL,
    warehouse_name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ENABLED',
    remark TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50)
);

-- 插入默认产品数据
INSERT INTO erp_product (product_code, product_name, unit, category, spec, status) VALUES
('P001', '笔记本电脑', '台', '电子产品', 'ThinkPad X1', 'ENABLED'),
('P002', '机械键盘', '个', '外设', 'Cherry MX', 'ENABLED'),
('P003', '显示器', '台', '电子产品', '27寸 4K', 'ENABLED'),
('P004', '办公桌', '张', '家具', '1400x700mm', 'ENABLED'),
('P005', '办公椅', '把', '家具', '人体工学椅', 'ENABLED')
ON CONFLICT DO NOTHING;

-- 插入默认仓库数据
INSERT INTO erp_warehouse (warehouse_code, warehouse_name, address, contact_person, contact_phone, status) VALUES
('WH001', '主仓库', '北京市朝阳区XX路1号', '张三', '13800001111', 'ENABLED'),
('WH002', '华东分仓', '上海市浦东新区YY路2号', '李四', '13800002222', 'ENABLED'),
('WH003', '华南分仓', '广州市天河区ZZ路3号', '王五', '13800003333', 'ENABLED')
ON CONFLICT DO NOTHING;
