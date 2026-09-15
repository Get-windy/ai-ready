-- ============================================================
-- V9.5.0: 注册 4 个新模块菜单（HR / Trade / Quality / Payment）
--
-- 这 4 个模块各有完整的前端 Vue 实现和后端 Controller，
-- 但一直未注册到菜单系统中。本次迁移将其激活。
--
-- 菜单 ID 规划：
--   60014        — HR（人力资源）一级 Mega 菜单
--   60015        — Trade（交易）一级 Mega 菜单
--   60016        — Quality（质量）一级 Mega 菜单
--   60017        — Payment（支付）一级 Mega 菜单
--   61401-61404  — HR 列分组
--   61501-61506  — Trade 列分组
--   61601-61603  — Quality 列分组
--   61701-61704  — Payment 列分组
--   90001+       — HR 叶子菜单
--   90101+       — Trade 叶子菜单
--   90201+       — Quality 叶子菜单
--   90301+       — Payment 叶子菜单
-- ============================================================

-- ============================================================
-- 1. 创建 4 个一级 Mega 菜单
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60014, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '人力资源', 'mega:hr', 0, NULL, 'TeamOutlined', 1400, 1, 1, 'tenant-admin', 0, 0),
  (60015, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '交易', 'mega:trade', 0, NULL, 'ShoppingOutlined', 1500, 1, 1, 'tenant-admin', 0, 0),
  (60016, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '质量', 'mega:quality', 0, NULL, 'SafetyCertificateOutlined', 1600, 1, 1, 'tenant-admin', 0, 0),
  (60017, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '支付', 'mega:payment', 0, NULL, 'PayCircleOutlined', 1700, 1, 1, 'tenant-admin', 0, 0);

-- ============================================================
-- 2. 创建列分组目录节点（二级目录）
-- ============================================================

-- 2.1 HR（60014）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61401, 0, 60014, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '员工管理', 'mega:hr:employee', 0, 100, 1, 1, 'tenant-admin', 0, 0),
  (61402, 0, 60014, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '考勤管理', 'mega:hr:attendance', 0, 200, 1, 1, 'tenant-admin', 0, 0),
  (61403, 0, 60014, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '薪资管理', 'mega:hr:salary', 0, 300, 1, 1, 'tenant-admin', 0, 0),
  (61404, 0, 60014, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '组织管理', 'mega:hr:organization', 0, 400, 1, 1, 'tenant-admin', 0, 0);

-- 2.2 Trade（60015）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61501, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '门店零售', 'mega:trade:pos', 0, 100, 1, 1, 'tenant-admin', 0, 0),
  (61502, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商城订单', 'mega:trade:mall-order', 0, 200, 1, 1, 'tenant-admin', 0, 0),
  (61503, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '渠道管理', 'mega:trade:channel', 0, 300, 1, 1, 'tenant-admin', 0, 0),
  (61504, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '外部订单', 'mega:trade:external', 0, 400, 1, 1, 'tenant-admin', 0, 0),
  (61505, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存同步', 'mega:trade:inventory-sync', 0, 500, 1, 1, 'tenant-admin', 0, 0),
  (61506, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统监控', 'mega:trade:monitor', 0, 600, 1, 1, 'tenant-admin', 0, 0);

-- 2.3 Quality（60016）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61601, 0, 60016, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '质检管理', 'mega:quality:inspection', 0, 100, 1, 1, 'tenant-admin', 0, 0),
  (61602, 0, 60016, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '质量标准', 'mega:quality:standard', 0, 200, 1, 1, 'tenant-admin', 0, 0),
  (61603, 0, 60016, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '缺陷管理', 'mega:quality:defect', 0, 300, 1, 1, 'tenant-admin', 0, 0);

-- 2.4 Payment（60017）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61701, 0, 60017, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '支付请求', 'mega:payment:request', 0, 100, 1, 1, 'tenant-admin', 0, 0),
  (61702, 0, 60017, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '支付记录', 'mega:payment:record', 0, 200, 1, 1, 'tenant-admin', 0, 0),
  (61703, 0, 60017, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '退款管理', 'mega:payment:refund', 0, 300, 1, 1, 'tenant-admin', 0, 0),
  (61704, 0, 60017, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '对账中心', 'mega:payment:reconciliation', 0, 400, 1, 1, 'tenant-admin', 0, 0);

-- ============================================================
-- 3. 清理 V6.23.0 遗留的死菜单项
--    这些页面的 Vue 文件已在孤立页面清理中删除，菜单条目需同步清理
-- ============================================================
DELETE FROM sys_menu WHERE id IN (90001, 90002, 90003);

-- ============================================================
-- 4. 创建叶子菜单项（20 个页面）
-- ============================================================

-- ── HR > 员工管理 (61401) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90001, 0, 61401, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '员工列表', 'hr:employee', 1, 'hr/employee', 'views/hr/employee/list.vue', 'UserOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── HR > 考勤管理 (61402) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90002, 0, 61402, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '考勤记录', 'hr:attendance', 1, 'hr/attendance', 'views/hr/attendance/list.vue', 'ClockCircleOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0),
  (90003, 0, 61402, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '请假管理', 'hr:leave', 1, 'hr/leave', 'views/hr/leave/list.vue', 'CalendarOutlined', 2, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── HR > 薪资管理 (61403) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90004, 0, 61403, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '薪资管理', 'hr:salary', 1, 'hr/salary', 'views/hr/salary/list.vue', 'DollarOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0),
  (90005, 0, 61403, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '绩效考核', 'hr:performance', 1, 'hr/performance', 'views/hr/performance/list.vue', 'StarOutlined', 2, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── HR > 组织管理 (61404) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90006, 0, 61404, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '职位管理', 'hr:position', 1, 'hr/organization/position', 'views/hr/organization/position-list.vue', 'ApartmentOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Trade > 门店零售 (61501) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90101, 0, 61501, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'POS收银', 'trade:pos', 1, 'trade/pos', 'views/trade/pos/index.vue', 'ShopOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Trade > 商城订单 (61502) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90102, 0, 61502, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商城订单', 'trade:mall-order', 1, 'trade/mall-order', 'views/trade/mall-order/list.vue', 'ShoppingCartOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0),
  (90103, 0, 61502, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '购物车', 'trade:cart', 1, 'trade/cart', 'views/trade/cart/list.vue', 'ShoppingOutlined', 2, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Trade > 渠道管理 (61503) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90104, 0, 61503, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '渠道配置', 'trade:channel-config', 1, 'trade/channel/config', 'views/trade/channel/config.vue', 'ApiOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Trade > 外部订单 (61504) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90105, 0, 61504, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '外部订单', 'trade:external-order', 1, 'trade/external-order', 'views/trade/external-order/list.vue', 'LinkOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Trade > 库存同步 (61505) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90106, 0, 61505, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存同步', 'trade:inventory-sync', 1, 'trade/inventory-sync', 'views/trade/inventory-sync/list.vue', 'SyncOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Trade > 系统监控 (61506) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90107, 0, 61506, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'API监控', 'trade:api-monitor', 1, 'trade/api-monitor', 'views/trade/api-monitor/list.vue', 'MonitorOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Quality > 质检管理 (61601) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90201, 0, 61601, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '质检单', 'quality:inspection', 1, 'quality/inspection', 'views/quality/inspection/list.vue', 'AuditOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Quality > 质量标准 (61602) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90202, 0, 61602, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '质量标准', 'quality:standard', 1, 'quality/standard', 'views/quality/standard/list.vue', 'SafetyCertificateOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Quality > 缺陷管理 (61603) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90203, 0, 61603, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '缺陷记录', 'quality:defect', 1, 'quality/defect', 'views/quality/defect/list.vue', 'BugOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Payment > 支付请求 (61701) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90301, 0, 61701, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '支付请求', 'payment:request', 1, 'payment/request', 'views/payment/request/list.vue', 'SendOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Payment > 支付记录 (61702) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90302, 0, 61702, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '支付记录', 'payment:record', 1, 'payment/record', 'views/payment/record/list.vue', 'FileTextOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Payment > 退款管理 (61703) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90303, 0, 61703, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '退款管理', 'payment:refund', 1, 'payment/refund', 'views/payment/refund/list.vue', 'RollbackOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

-- ── Payment > 对账中心 (61704) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (90304, 0, 61704, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '每日对账', 'payment:reconciliation', 1, 'payment/reconciliation/daily', 'views/payment/reconciliation/daily.vue', 'AccountBookOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);
