-- ============================================================================
-- V11.139.0  商品模块金标准增强
--  1) erp_product_unit 恢复 per-unit 等级价列（V9.29.0 误删导致实体/表单悬空映射）
--  2) erp_product 补商城信息列（排序方式 / 商品积分 / 关键字）
--  3) erp_product_shield  商品授权（按客户/区域屏蔽商品）
--  4) erp_product_cloud_catalog  云商品库（云导入来源）
-- ============================================================================

-- ── 1) 商品单位：恢复 8 个等级价列 ──────────────────────────────────────────
-- 背景：V9.23.0 把 6 个硬编码价格列重命名为 grade_price_1..6 并补到 8 列，
--       V9.29.0 拆分等级价到 erp_product_grade_price 时把这 8 列 DROP 了，
--       但 ProductUnit 实体与商品表单「商品单位」明细表（21 列含 8 个等级价）
--       仍按 per-unit 等级价读写 → 单位查询/保存报 "column grade_price_1 does not exist"。
-- 口径：erp_product_grade_price = 客户指定价/级别指定价（含有效期等高级规则）；
--       erp_product_unit.grade_price_N = 单位标准等级价（商品表单维护）。
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS grade_price_1 numeric(18,4) DEFAULT 0;
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS grade_price_2 numeric(18,4) DEFAULT 0;
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS grade_price_3 numeric(18,4) DEFAULT 0;
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS grade_price_4 numeric(18,4) DEFAULT 0;
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS grade_price_5 numeric(18,4) DEFAULT 0;
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS grade_price_6 numeric(18,4) DEFAULT 0;
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS grade_price_7 numeric(18,4) DEFAULT 0;
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS grade_price_8 numeric(18,4) DEFAULT 0;

-- ── 2) 商品主表：商城信息列 ────────────────────────────────────────────────
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS mall_sort_type varchar(20) DEFAULT 'DEFAULT';
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS mall_points numeric(18,2) DEFAULT 0;
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS keywords varchar(255);

COMMENT ON COLUMN erp_product.mall_sort_type IS '商城排序方式 DEFAULT/SALES/MANUAL';
COMMENT ON COLUMN erp_product.mall_points IS '商品积分';
COMMENT ON COLUMN erp_product.keywords IS '商城检索关键字';

-- ── 3) 商品授权（屏蔽客户）─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS erp_product_shield (
    id            bigint       PRIMARY KEY,
    tenant_id     bigint,
    product_id    bigint       NOT NULL,
    partner_id    bigint,
    partner_name  varchar(128),
    shield_level  varchar(32)  DEFAULT 'ALL',
    region        varchar(128),
    remark        varchar(255),
    deleted       integer      DEFAULT 0,
    create_by     varchar(64),
    create_time   timestamp    DEFAULT now(),
    update_by     varchar(64),
    update_time   timestamp    DEFAULT now()
);

COMMENT ON TABLE  erp_product_shield              IS '商品授权-屏蔽客户关系';
COMMENT ON COLUMN erp_product_shield.product_id   IS '商品ID(erp_product.id)';
COMMENT ON COLUMN erp_product_shield.partner_id   IS '屏蔽客户ID(biz_party.id)';
COMMENT ON COLUMN erp_product_shield.shield_level IS '屏蔽级别 ALL=完全屏蔽/CONSULT=需询价/HIDDEN_PRICE=隐藏价格';
COMMENT ON COLUMN erp_product_shield.region       IS '屏蔽区域';

CREATE INDEX IF NOT EXISTS idx_product_shield_product ON erp_product_shield (product_id);
CREATE INDEX IF NOT EXISTS idx_product_shield_partner ON erp_product_shield (partner_id);

-- ── 4) 云商品库（云导入数据源）────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS erp_product_cloud_catalog (
    id                    bigint       PRIMARY KEY,
    tenant_id             bigint,
    cloud_code            varchar(64),
    cloud_name            varchar(200) NOT NULL,
    spec                  varchar(128),
    model                 varchar(128),
    origin                varchar(64),
    brand                 varchar(64),
    unit                  varchar(32),
    barcode               varchar(64),
    category_name         varchar(128),
    industry_category     varchar(64),
    preset_purchase_price numeric(18,4),
    retail_price          numeric(18,4),
    wholesale_price       numeric(18,4),
    shelf_life_days       integer,
    deleted               integer      DEFAULT 0,
    create_by             varchar(64),
    create_time           timestamp    DEFAULT now(),
    update_by             varchar(64),
    update_time           timestamp    DEFAULT now()
);

COMMENT ON TABLE erp_product_cloud_catalog IS '云商品库（云导入可选的公共商品档案）';

CREATE INDEX IF NOT EXISTS idx_cloud_catalog_name ON erp_product_cloud_catalog (cloud_name);

INSERT INTO erp_product_cloud_catalog
    (id, tenant_id, cloud_code, cloud_name, spec, model, origin, brand, unit, barcode,
     category_name, industry_category, preset_purchase_price, retail_price, wholesale_price,
     shelf_life_days, deleted, create_time, update_time)
VALUES
    (9000000000000000001, 1, 'YL-1001', '伊利纯牛奶(利乐枕)',   '240ml*16袋', 'LY-240', '内蒙古', '伊利',   '箱', '6907992500123', '常温食品', '其他', 42.5000, 55.0000, 49.0000, 180, 0, now(), now()),
    (9000000000000000002, 1, 'YL-1002', '伊利安慕希酸奶',       '205g*12盒',  'AMS-205','内蒙古', '伊利',   '箱', '6907992500130', '低温奶食', '其他', 52.0000, 66.0000, 59.0000, 21,  0, now(), now()),
    (9000000000000000003, 1, 'MN-2001', '蒙牛特仑苏纯牛奶',     '250ml*12盒', 'TLS-250','内蒙古', '蒙牛',   '箱', '6923644241234', '常温食品', '其他', 58.0000, 72.0000, 64.0000, 180, 0, now(), now()),
    (9000000000000000004, 1, 'MN-2002', '蒙牛真果粒',           '240g*16袋',  'ZGL-240','内蒙古', '蒙牛',   '箱', '6923644241241', '常温食品', '其他', 38.0000, 48.0000, 43.0000, 180, 0, now(), now()),
    (9000000000000000005, 1, 'WT-3001', '旺仔牛奶(罐装)',       '245ml*12罐', 'WZ-245', '上海',   '旺旺',   '箱', '6924743911234', '常温食品', '其他', 46.0000, 58.0000, 52.0000, 240, 0, now(), now()),
    (9000000000000000006, 1, 'WT-3002', '旺旺雪饼',             '84g*24袋',   'XB-84',  '上海',   '旺旺',   '箱', '6924743911241', '休闲食品', '其他', 52.0000, 65.0000, 58.0000, 270, 0, now(), now()),
    (9000000000000000007, 1, 'KLS-4001','康师傅红烧牛肉面',     '103g*24袋',  'KSF-103','天津',   '康师傅', '箱', '6920152401234', '米面制品', '其他', 44.0000, 55.0000, 49.0000, 180, 0, now(), now()),
    (9000000000000000008, 1, 'KLS-4002','康师傅冰红茶',         '500ml*15瓶', 'BHC-500','天津',   '康师傅', '箱', '6920152401241', '饮品原料', '其他', 36.0000, 45.0000, 40.0000, 365, 0, now(), now()),
    (9000000000000000009, 1, 'TT-5001', '统一鲜橙多',           '450ml*15瓶', 'XCD-450','江苏',   '统一',   '箱', '6925303711234', '饮品原料', '其他', 33.0000, 42.0000, 37.0000, 365, 0, now(), now()),
    (9000000000000000010, 1, 'TT-5002', '统一老坛酸菜牛肉面',   '122g*24袋',  'LTS-122','江苏',   '统一',   '箱', '6925303711241', '米面制品', '其他', 47.0000, 58.0000, 52.0000, 180, 0, now(), now()),
    (9000000000000000011, 1, 'HL-6001', '海天生抽酱油',         '1.9L*6瓶',   'HT-1.9', '广东',   '海天',   '箱', '6902265111234', '调味冻品', '其他', 62.0000, 78.0000, 70.0000, 540, 0, now(), now()),
    (9000000000000000012, 1, 'HL-6002', '海天蚝油',             '700g*12瓶',  'HY-700', '广东',   '海天',   '箱', '6902265111241', '调味冻品', '其他', 55.0000, 69.0000, 62.0000, 540, 0, now(), now()),
    (9000000000000000013, 1, 'LJ-7001', '李锦记蒸鱼豉油',       '410ml*12瓶', 'ZYC-410','广东',   '李锦记', '箱', '6902502411234', '调味冻品', '其他', 68.0000, 85.0000, 76.0000, 540, 0, now(), now()),
    (9000000000000000014, 1, 'SH-8001', '双汇火腿肠',           '60g*50支',   'SH-60',  '河南',   '双汇',   '箱', '6902890211234', '低温熟食', '其他', 58.0000, 72.0000, 65.0000, 90,  0, now(), now()),
    (9000000000000000015, 1, 'SH-8002', '双汇王中王火腿肠',     '100g*30支',  'WZW-100','河南',   '双汇',   '箱', '6902890211241', '低温熟食', '其他', 76.0000, 92.0000, 84.0000, 90,  0, now(), now()),
    (9000000000000000016, 1, 'JJD-9001','金龙鱼调和油',         '5L*4桶',     'JLY-5L', '上海',   '金龙鱼', '箱', '6902088711234', '米面制品', '其他', 268.0000, 320.0000, 295.0000, 540, 0, now(), now()),
    (9000000000000000017, 1, 'JJD-9002','金龙鱼东北大米',       '10kg*1袋',   'DM-10K', '黑龙江', '金龙鱼', '袋', '6902088711241', '米面制品', '其他', 58.0000, 72.0000, 65.0000, 365, 0, now(), now()),
    (9000000000000000018, 1, 'XF-1101', '三全芝麻汤圆',         '450g*12袋',  'SQ-450', '河南',   '三全',   '箱', '6908791111234', '米面制品', '其他', 82.0000, 98.0000, 90.0000, 365, 0, now(), now()),
    (9000000000000000019, 1, 'XF-1102', '三全水饺(猪肉白菜)',   '1kg*10袋',   'SJ-1K',  '河南',   '三全',   '箱', '6908791111241', '米面制品', '其他', 128.0000, 155.0000, 140.0000, 365, 0, now(), now()),
    (9000000000000000020, 1, 'XF-1103', '思念灌汤水饺',         '1kg*10袋',   'GN-1K',  '河南',   '思念',   '箱', '6907992811234', '米面制品', '其他', 118.0000, 142.0000, 130.0000, 365, 0, now(), now())
ON CONFLICT (id) DO NOTHING;

-- ── 5) 索引 ────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_erp_product_unit_product ON erp_product_unit (product_id);
CREATE INDEX IF NOT EXISTS idx_erp_product_category_id  ON erp_product (category_id);
CREATE INDEX IF NOT EXISTS idx_erp_product_shelf        ON erp_product (mall_shelf_status);
