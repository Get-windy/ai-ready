-- ============================================================
-- V9.6.0: 菜单聚合改造（17 → 13 个一级菜单）
--
-- 设计文档: docs/design/menu-consolidation-v2.md
--
-- 改造内容:
--   1. 配发收(60004) → 并入「配送」
--   2. 商城(60009)   → 并入「交易」
--   3. 质量(60016)   → 3列合1列 → 并入「仓储」
--   4. 支付(60017)   → 流水→「交易」，配置→「资料」
--   5. 职员权限(61105) → 整体移入「人力资源」
--   6. 系统监控(61506) → 从「交易」移入「配送」
--
-- 执行顺序:
--   Step 1-8: 新建节点 + 移动节点 + 新建占位叶子
--   Step 9:   物理删除废弃节点（叶子已全部迁出后才能删）
--
-- 注意：本脚本包含 DELETE 操作，需要排他锁。
--   设置 lock_timeout 防止启动时与应用菜单加载产生死锁。
-- ============================================================

-- 设置锁等待超时（30秒），避免启动时与菜单加载服务死锁
SET lock_timeout = '30s';

-- ============================================================
-- Step 1: 新建 5 个列组（二级目录）
-- ============================================================

-- 1.1 仓储·质量管理（parent=60003，sort=1100 排在库存作业之后）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60311, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '质量管理', 'mega:wh:quality', 0, 1100, 1, 1, 'tenant-admin', 0, 0);

-- 1.2 交易·商城管理（parent=60015，sort=300 排在商城订单之后）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61507, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '商城管理', 'mega:trade:mall-mgmt', 0, 300, 1, 1, 'tenant-admin', 0, 0);

-- 1.3 交易·支付结算（parent=60015，sort=700 排在库存同步之后）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61508, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '支付结算', 'mega:trade:payment', 0, 700, 1, 1, 'tenant-admin', 0, 0);

-- 1.4 [已取消] 不再新建 60507，直接复用 61506(系统监控) 移入配送并重命名为 API监控
--     原因：61506 只有一个叶子(90107)，移入配送后重命名即可，避免创建冗余空列

-- 1.5 资料·支付管理（parent=60011，sort=550 排在配送管理之后、财务账户之前）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61107, 0, 60011, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '支付管理', 'mega:md:payment-config', 0, 550, 1, 1, 'tenant-admin', 0, 0);

-- 1.6 人力资源·职员管理（parent=60014，sort=500 排在组织管理之后）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61405, 0, 60014, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '职员管理', 'mega:hr:staff', 0, 500, 1, 1, 'tenant-admin', 0, 0);

-- ============================================================
-- Step 2: 配发收(60004) 列组 → 配送(60005)
--   叶子随列组自动迁移，无需单独处理
-- ============================================================

-- 2.1 配送业务(60401) → 配送，sort=100（与原有配送列组衔接，排在最前）
UPDATE sys_menu SET parent_id = 60005, sort = 100, update_time = CURRENT_TIMESTAMP
WHERE id = 60401;

-- 2.2 发货业务(60402) → 配送，sort=200
UPDATE sys_menu SET parent_id = 60005, sort = 200, update_time = CURRENT_TIMESTAMP
WHERE id = 60402;

-- 2.3 收货业务(60403) → 配送，sort=300
UPDATE sys_menu SET parent_id = 60005, sort = 300, update_time = CURRENT_TIMESTAMP
WHERE id = 60403;

-- ============================================================
-- Step 3: 商城(60009) 列组 → 交易(60015)
-- ============================================================

-- 3.1 订单处理(60901) → 商城订单(61502)
--   其叶子(80350/80351)与现有(90102/90103)并列，需调整 sort
UPDATE sys_menu SET parent_id = 61502, update_time = CURRENT_TIMESTAMP
WHERE id = 60901;

-- 订单处理/退货申请处理 排在商城订单/购物车之后
UPDATE sys_menu SET sort = 3, update_time = CURRENT_TIMESTAMP WHERE id = 80350;
UPDATE sys_menu SET sort = 4, update_time = CURRENT_TIMESTAMP WHERE id = 80351;

-- 3.2 基础业务(60902) → 商城管理(61507)
UPDATE sys_menu SET parent_id = 61507, sort = 100, update_time = CURRENT_TIMESTAMP
WHERE id = 60902;

-- 3.3 商城设置(60903) → 商城管理(61507)
UPDATE sys_menu SET parent_id = 61507, sort = 200, update_time = CURRENT_TIMESTAMP
WHERE id = 60903;

-- ============================================================
-- Step 4: 质量(60016) 叶子 → 仓储·质量管理(60311)
--   3 个叶子合并到 1 个列组
-- ============================================================

UPDATE sys_menu SET parent_id = 60311, sort = 1, update_time = CURRENT_TIMESTAMP
WHERE id = 90201;  -- 质检单

UPDATE sys_menu SET parent_id = 60311, sort = 2, update_time = CURRENT_TIMESTAMP
WHERE id = 90202;  -- 质量标准

UPDATE sys_menu SET parent_id = 60311, sort = 3, update_time = CURRENT_TIMESTAMP
WHERE id = 90203;  -- 缺陷记录

-- ============================================================
-- Step 5: 支付(60017) 叶子 → 交易·支付结算(61508)
-- ============================================================

UPDATE sys_menu SET parent_id = 61508, sort = 1, update_time = CURRENT_TIMESTAMP
WHERE id = 90301;  -- 支付请求

UPDATE sys_menu SET parent_id = 61508, sort = 2, update_time = CURRENT_TIMESTAMP
WHERE id = 90302;  -- 支付记录

UPDATE sys_menu SET parent_id = 61508, sort = 3, update_time = CURRENT_TIMESTAMP
WHERE id = 90303;  -- 退款管理

UPDATE sys_menu SET parent_id = 61508, sort = 4, update_time = CURRENT_TIMESTAMP
WHERE id = 90304;  -- 每日对账

-- ============================================================
-- Step 6: 交易·系统监控(61506) → 配送(60005)，重命名为API监控
--   直接复用 61506 作为配送下的 API监控列，无需新建列
--   其叶子(90107) 随列组自动迁移，parent_id 无需单独改
-- ============================================================

UPDATE sys_menu SET parent_id = 60005, sort = 700,
    menu_name = 'API监控', menu_code = 'mega:dms:api-monitor',
    update_time = CURRENT_TIMESTAMP
WHERE id = 61506;

-- ============================================================
-- Step 7: 职员权限(61105) 叶子 → 人力资源·职员管理(61405)
-- ============================================================

UPDATE sys_menu SET parent_id = 61405, sort = 1, update_time = CURRENT_TIMESTAMP
WHERE id = 80530;  -- 职员部门

UPDATE sys_menu SET parent_id = 61405, sort = 2, update_time = CURRENT_TIMESTAMP
WHERE id = 80531;  -- 岗位权限

UPDATE sys_menu SET parent_id = 61405, sort = 3, update_time = CURRENT_TIMESTAMP
WHERE id = 80532;  -- 全部操作员

-- ============================================================
-- Step 8: 新建资料·支付管理(61107) 占位叶子
-- ============================================================

INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80550, 0, 61107, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '支付方式', 'md:payment-method', 1,
   'md/payment-method', 'views/common/placeholder/index.vue',
   'PayCircleOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0),
  (80551, 0, 61107, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '支付渠道', 'md:payment-channel', 1,
   'md/payment-channel', 'views/common/placeholder/index.vue',
   'ApiOutlined', 2, 1, 1, 'tenant-admin', 0, 0, 0),
  (80552, 0, 61107, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '支付账户', 'md:payment-account', 1,
   'md/payment-account', 'views/common/placeholder/index.vue',
   'BankOutlined', 3, 1, 1, 'tenant-admin', 0, 0, 0);

-- ============================================================
-- Step 9: 物理删除废弃节点
--   执行前提：所有叶子已迁出，列组下无残留子节点
-- ============================================================

-- 9.1 清理 sys_tenant_menu 中引用即将删除的菜单 ID 的授权记录
DELETE FROM sys_tenant_menu
WHERE menu_id IN (
    60004, 60009, 60016, 60017,          -- 4 个一级菜单
    61601, 61602, 61603,                  -- 质量 3 个列组
    61701, 61702, 61703, 61704,           -- 支付 4 个列组
    61105                                 -- 职员权限列组
);

-- 9.2 物理删除质量(60016)的 3 个空列组
DELETE FROM sys_menu WHERE id IN (61601, 61602, 61603);

-- 9.3 物理删除支付(60017)的 4 个空列组
DELETE FROM sys_menu WHERE id IN (61701, 61702, 61703, 61704);

-- 9.4 物理删除职员权限(61105)空列组
DELETE FROM sys_menu WHERE id = 61105;

-- 9.5 物理删除 4 个空一级菜单
DELETE FROM sys_menu WHERE id IN (60004, 60009, 60016, 60017);

-- ============================================================
-- Step 10: 验证数据完整性
-- ============================================================

-- 确认无孤儿节点（parent_id 指向不存在的父节点）
-- 生产环境可执行以下查询验证：
-- SELECT id, menu_name, parent_id
-- FROM sys_menu
-- WHERE deleted = 0
--   AND parent_id > 0
--   AND parent_id NOT IN (SELECT id FROM sys_menu WHERE deleted = 0);

-- ============================================================
-- Flyway 历史记录
-- ============================================================
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT COALESCE(MAX(installed_rank), 0) + 1, '9.6.0', 'Menu Consolidation', 'SQL', 'V9.6.0__Menu_Consolidation.sql', NULL, 'manual', CURRENT_TIMESTAMP, 0, true
FROM flyway_schema_history
WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '9.6.0');
