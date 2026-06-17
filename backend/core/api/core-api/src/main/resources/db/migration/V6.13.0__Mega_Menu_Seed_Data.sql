-- ============================================================
-- V6.13.0: Mega Menu 改造 - 种子数据迁移
--
-- 变更说明：
--   1. 设置新字段默认值
--   2. 软删除旧版 display_group=1 一级分组菜单
--   3. 创建 13 个新一级 mega 菜单
--   4. 创建列分组目录节点（二级目录，约 61 列）
--   5. 将现有叶子菜单重新挂载到新分组下
--   6. 插入系统级菜单（44 项）
--   7. 清理孤儿节点
--
-- 菜单 ID 规划：
--   60001-60013   — 13 个一级菜单
--   60101+        — 销售模块
--   60201+        — 采购模块
--   60301+        — 仓储模块
--   60401+        — 配发收模块
--   60501+        — 配送模块
--   60601+        — 财务模块
--   60701+        — CRM 模块
--   60801+        — 营销模块
--   60901+        — 商城模块
--   61001+        — 分析模块
--   61101+        — 资料模块
--   61201+        — 设置模块
--   61301+        — 系统模块
-- ============================================================

-- ============================================================
-- 1. 填充新字段默认值
-- ============================================================
UPDATE sys_menu SET
    display_mode = 0,
    menu_level = 0,
    update_time = CURRENT_TIMESTAMP
WHERE display_mode IS NULL OR menu_level IS NULL;

-- ============================================================
-- 2. 软删除旧的一级分组菜单（display_group=1 的顶级节点）
--    这些节点在新架构中被新的 13 个一级菜单替代
-- ============================================================
UPDATE sys_menu SET
    deleted = 1,
    update_time = CURRENT_TIMESTAMP
WHERE parent_id = 0 AND display_group = 1 AND deleted = 0;

-- ============================================================
-- 3. 创建 13 个一级 Mega 菜单
--    统一 client_type='pc-admin', tenant_id=0, menu_level=0
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60001, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售', 'mega:sale', 0, NULL, 'ShoppingOutlined', 100, 1, 1, 'pc-admin', 0, 0),
  (60002, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购', 'mega:purchase', 0, NULL, 'ShoppingCartOutlined', 200, 1, 1, 'pc-admin', 0, 0),
  (60003, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '仓储', 'mega:warehouse', 0, NULL, 'ContainerOutlined', 300, 1, 1, 'pc-admin', 0, 0),
  (60004, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配发收', 'mega:dispatch-receive', 0, NULL, 'SendOutlined', 400, 1, 1, 'pc-admin', 0, 0),
  (60005, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送', 'mega:delivery', 0, NULL, 'AuditOutlined', 500, 1, 1, 'pc-admin', 0, 0),
  (60006, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '财务', 'mega:finance', 0, NULL, 'AccountBookOutlined', 600, 1, 1, 'pc-admin', 0, 0),
  (60007, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'CRM', 'mega:crm', 0, NULL, 'TeamOutlined', 700, 1, 1, 'pc-admin', 0, 0),
  (60008, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '营销', 'mega:marketing', 0, NULL, 'StarOutlined', 800, 1, 1, 'pc-admin', 0, 0),
  (60009, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商城', 'mega:mall', 0, NULL, 'ShopOutlined', 900, 1, 1, 'pc-admin', 0, 0),
  (60010, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '分析', 'mega:analytics', 0, NULL, 'BarChartOutlined', 1000, 1, 1, 'pc-admin', 0, 0),
  (60011, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资料', 'mega:master-data', 0, NULL, 'FileTextOutlined', 1100, 1, 1, 'pc-admin', 0, 0),
  (60012, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '设置', 'mega:settings', 0, NULL, 'SettingOutlined', 1200, 1, 1, 'pc-admin', 0, 0),
  (60013, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统', 'mega:system', 0, NULL, 'MonitorOutlined', 1300, 1, 1, 'pc-admin', 0, 1);

-- ============================================================
-- 4. 创建列分组目录节点（二级目录）
--    这些节点作为 mega 面板的列
-- ============================================================

-- 4.1 销售（60001）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60101, 0, 60001, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '外勤拜访', 'mega:sale:visit', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60102, 0, 60001, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '订货业务', 'mega:sale:order', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60103, 0, 60001, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售业务', 'mega:sale:transact', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (60104, 0, 60001, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售查询', 'mega:sale:query', 0, 400, 1, 1, 'pc-admin', 0, 0),
  (60105, 0, 60001, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '订单中心', 'mega:sale:order-center', 0, 500, 1, 1, 'pc-admin', 0, 0);

-- 4.2 采购（60002）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60201, 0, 60002, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购准备', 'mega:purchase:prepare', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60202, 0, 60002, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购业务', 'mega:purchase:biz', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60203, 0, 60002, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购查询', 'mega:purchase:query', 0, 300, 1, 1, 'pc-admin', 0, 0);

-- 4.3 仓储（60003）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60301, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '其他出入库', 'mega:wh:other-io', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60302, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '盘点', 'mega:wh:stocktake', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60303, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '生产', 'mega:wh:production', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (60304, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存预警', 'mega:wh:alert', 0, 400, 1, 1, 'pc-admin', 0, 0),
  (60305, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '借进借出', 'mega:wh:lend', 0, 500, 1, 1, 'pc-admin', 0, 0),
  (60306, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收货作业', 'mega:wh:receiving', 0, 600, 1, 1, 'pc-admin', 0, 0),
  (60307, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '上架作业', 'mega:wh:putaway', 0, 700, 1, 1, 'pc-admin', 0, 0),
  (60308, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拣货作业', 'mega:wh:picking', 0, 800, 1, 1, 'pc-admin', 0, 0),
  (60309, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发货作业', 'mega:wh:shipping', 0, 900, 1, 1, 'pc-admin', 0, 0),
  (60310, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存作业', 'mega:wh:inventory', 0, 1000, 1, 1, 'pc-admin', 0, 0);

-- 4.4 配发收（60004）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60401, 0, 60004, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送业务', 'mega:dr:dispatch', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60402, 0, 60004, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发货业务', 'mega:dr:ship', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60403, 0, 60004, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收货业务', 'mega:dr:receive', 0, 300, 1, 1, 'pc-admin', 0, 0);

-- 4.5 配送（60005）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60501, 0, 60005, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '线路管理', 'mega:dms:route', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60502, 0, 60005, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '车辆管理', 'mega:dms:vehicle', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60503, 0, 60005, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '骑手管理', 'mega:dms:rider', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (60504, 0, 60005, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '调度管理', 'mega:dms:dispatch', 0, 400, 1, 1, 'pc-admin', 0, 0),
  (60505, 0, 60005, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送跟踪', 'mega:dms:tracking', 0, 500, 1, 1, 'pc-admin', 0, 0),
  (60506, 0, 60005, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送配置', 'mega:dms:config', 0, 600, 1, 1, 'pc-admin', 0, 0);

-- 4.6 财务（60006）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60601, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收款', 'mega:fin:receipt', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60602, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '付款', 'mega:fin:payment', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60603, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收入支出', 'mega:fin:income-expense', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (60604, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '账务处理', 'mega:fin:accounting', 0, 400, 1, 1, 'pc-admin', 0, 0),
  (60605, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '账簿', 'mega:fin:ledger', 0, 500, 1, 1, 'pc-admin', 0, 0),
  (60606, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '财务报表', 'mega:fin:report', 0, 600, 1, 1, 'pc-admin', 0, 0),
  (60607, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产管理', 'mega:fin:asset', 0, 700, 1, 1, 'pc-admin', 0, 0),
  (60608, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预算管理', 'mega:fin:budget', 0, 800, 1, 1, 'pc-admin', 0, 0),
  (60609, 0, 60006, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用管理', 'mega:fin:expense', 0, 900, 1, 1, 'pc-admin', 0, 0);

-- 4.7 CRM（60007）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60701, 0, 60007, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户管理', 'mega:crm:customer', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60702, 0, 60007, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '线索管理', 'mega:crm:lead', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60703, 0, 60007, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商机管理', 'mega:crm:opportunity', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (60704, 0, 60007, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '报价管理', 'mega:crm:quotation', 0, 400, 1, 1, 'pc-admin', 0, 0),
  (60705, 0, 60007, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '合同管理', 'mega:crm:contract', 0, 500, 1, 1, 'pc-admin', 0, 0),
  (60706, 0, 60007, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发票管理', 'mega:crm:invoice', 0, 600, 1, 1, 'pc-admin', 0, 0),
  (60707, 0, 60007, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'CRM报表', 'mega:crm:report', 0, 700, 1, 1, 'pc-admin', 0, 0);

-- 4.8 营销（60008）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60801, 0, 60008, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '会员中心', 'mega:mkt:member', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60802, 0, 60008, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '营销活动', 'mega:mkt:activity', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60803, 0, 60008, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商城营销', 'mega:mkt:mall', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (60804, 0, 60008, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '营销推广', 'mega:mkt:promotion', 0, 400, 1, 1, 'pc-admin', 0, 0);

-- 4.9 商城（60009）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60901, 0, 60009, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '订单处理', 'mega:mall:order', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (60902, 0, 60009, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '基础业务', 'mega:mall:biz', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (60903, 0, 60009, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商城设置', 'mega:mall:config', 0, 300, 1, 1, 'pc-admin', 0, 0);

-- 4.10 分析（60010）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61001, 0, 60010, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '综合单据', 'mega:ana:doc-center', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (61002, 0, 60010, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采销分析', 'mega:ana:ps-analytics', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (61003, 0, 60010, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '仓配分析', 'mega:ana:wh-analytics', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (61004, 0, 60010, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '提成分析', 'mega:ana:commission', 0, 400, 1, 1, 'pc-admin', 0, 0),
  (61005, 0, 60010, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '财务分析', 'mega:ana:fin-analytics', 0, 500, 1, 1, 'pc-admin', 0, 0),
  (61006, 0, 60010, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '营销分析', 'mega:ana:mkt-analytics', 0, 600, 1, 1, 'pc-admin', 0, 0);

-- 4.11 资料（60011）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61101, 0, 60011, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商品管理', 'mega:md:product', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (61102, 0, 60011, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '往来单位', 'mega:md:partner', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (61103, 0, 60011, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '仓库管理', 'mega:md:warehouse', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (61104, 0, 60011, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送管理', 'mega:md:delivery', 0, 400, 1, 1, 'pc-admin', 0, 0),
  (61105, 0, 60011, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '职员权限', 'mega:md:staff', 0, 500, 1, 1, 'pc-admin', 0, 0),
  (61106, 0, 60011, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '财务账户', 'mega:md:fin-account', 0, 600, 1, 1, 'pc-admin', 0, 0);

-- 4.12 设置（60012）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61201, 0, 60012, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统配置', 'mega:set:sys-config', 0, 100, 1, 1, 'pc-admin', 0, 0),
  (61202, 0, 60012, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '期初录入', 'mega:set:initial', 0, 200, 1, 1, 'pc-admin', 0, 0),
  (61203, 0, 60012, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '账套操作', 'mega:set:account', 0, 300, 1, 1, 'pc-admin', 0, 0),
  (61204, 0, 60012, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '财务设置', 'mega:set:fin-config', 0, 400, 1, 1, 'pc-admin', 0, 0),
  (61205, 0, 60012, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '打印管理', 'mega:set:printing', 0, 500, 1, 1, 'pc-admin', 0, 0),
  (61206, 0, 60012, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '工作流', 'mega:set:workflow', 0, 600, 1, 1, 'pc-admin', 0, 0);

-- 4.13 系统（60013）— 系统级菜单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61301, 0, 60013, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '租户管理', 'mega:sys:tenant', 0, 100, 1, 1, 'pc-admin', 0, 1),
  (61302, 0, 60013, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '模块管理', 'mega:sys:module', 0, 200, 1, 1, 'pc-admin', 0, 1),
  (61303, 0, 60013, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统监控', 'mega:sys:monitor', 0, 300, 1, 1, 'pc-admin', 0, 1),
  (61304, 0, 60013, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '数据管理', 'mega:sys:data', 0, 400, 1, 1, 'pc-admin', 0, 1),
  (61305, 0, 60013, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '开发工具', 'mega:sys:devtools', 0, 500, 1, 1, 'pc-admin', 0, 1),
  (61306, 0, 60013, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '平台设置', 'mega:sys:platform', 0, 600, 1, 1, 'pc-admin', 0, 1);

-- ============================================================
-- 5. 将现有叶子菜单重新挂载到新分组下
--    按 menu_code（或 menu_name）匹配，UPDATE parent_id
-- ============================================================

-- 5.1 销售模块 - 重新挂载
-- 订货业务列（60102）：销售订单、销售退货申请、预订货单
UPDATE sys_menu SET parent_id = 60102, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'sales:order' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60102, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'sales:return' AND deleted = 0;

-- 销售业务列（60103）：零售单、销售出库单、销售退货单、销售换货单
UPDATE sys_menu SET parent_id = 60103, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'sales:shipment' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60103, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:sale-outbound' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60103, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:return' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60103, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:purchase-exchange' AND deleted = 0;

-- 销售查询列（60104）：销售单据查询、销售明细查询、销售价格跟踪
UPDATE sys_menu SET parent_id = 60104, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'sales:analysis' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60104, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'sales:report' AND deleted = 0;

-- 外勤拜访列（60101）：拜访规划、拜访执行、拜访检视

-- 5.2 采购模块 - 重新挂载
-- 采购业务列（60202）：采购订单、采购入库单、采购退货单、采购换货单、采购费用分摊
UPDATE sys_menu SET parent_id = 60202, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'purchase:order' AND deleted = 0;

-- 采购查询列（60203）：采购单据查询、采购明细查询、采购价格跟踪

-- 5.3 仓储模块 - 重新挂载
-- 其他出入库列（60301）：其他出库单、其他入库单、调拨单
UPDATE sys_menu SET parent_id = 60301, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-transfer' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60301, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-in' AND deleted = 0;

-- 盘点列（60302）：报损单、报溢单、盘点单、成本调价单
UPDATE sys_menu SET parent_id = 60302, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-damage' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60302, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-overflow' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60302, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stocktake' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60302, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-cost-adjust' AND deleted = 0;

-- 生产列（60303）：生产模板、组装单、拆卸单
UPDATE sys_menu SET parent_id = 60303, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-assemble' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60303, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-split' AND deleted = 0;

-- 库存预警列（60304）：预警查询、预警设置
UPDATE sys_menu SET parent_id = 60304, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-alert-config' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60304, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:stock-replenishment' AND deleted = 0;

-- 借进借出列（60305）：借进单、借出单、借进借出查询

-- 收货作业列（60306）：收货单、收货质检
-- 上架作业列（60307）：上架任务、上架策略
-- 拣货作业列（60308）：拣货单、波次管理、拣货策略
-- 发货作业列（60309）：发货单、复核打包
-- 库存作业列（60310）：移库单、库存查询

-- 5.4 财务模块 - 重新挂载
-- 收款列（60601）：收款单、预收款单、按单收款、待确认款项、在线支付对账单
UPDATE sys_menu SET parent_id = 60601, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'finance:receivable' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60601, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'finance:receipt' AND deleted = 0;

-- 付款列（60602）：付款单、预付款单、按单付款
UPDATE sys_menu SET parent_id = 60602, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'finance:payable' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60602, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'finance:payment' AND deleted = 0;

-- 收入支出列（60603）：费用单、其他收入、应收应付调整、账款交账
-- 账务处理列（60604）：会计凭证、月结
UPDATE sys_menu SET parent_id = 60604, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'finance:voucher' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60604, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'finance:subject' AND deleted = 0;

-- 账簿列（60605）：总账、明细账、科目余额表、辅助核算余额表
-- 财务报表列（60606）：资产负债表、利润表
-- 资产管理列（60607）：资产卡片、资产折旧、资产调拨、资产处置、资产盘点、资产报表
UPDATE sys_menu SET parent_id = 60607, update_time = CURRENT_TIMESTAMP WHERE menu_code LIKE 'fixed-asset:%' AND deleted = 0;

-- 预算管理列（60608）：预算模板、年度预算、预算调整、预算报表
UPDATE sys_menu SET parent_id = 60608, update_time = CURRENT_TIMESTAMP WHERE menu_code LIKE 'budget:%' AND deleted = 0;

-- 费用管理列（60609）：费用申请、费用报销、费用审批、费用支付、费用统计
UPDATE sys_menu SET parent_id = 60609, update_time = CURRENT_TIMESTAMP WHERE menu_code LIKE 'erp:expense:%' AND deleted = 0;

-- 5.7 CRM 模块 - 重新挂载
UPDATE sys_menu SET parent_id = 60701, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'crm:customer' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60702, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'crm:lead' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60703, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'crm:opportunity' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60704, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'crm:quotation' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60705, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'crm:contract' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60706, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'crm:invoice' AND deleted = 0;

-- 5.8 营销模块 - 重新挂载
-- 会员中心列（60801）：会员管理、积分兑换、会员设置
-- 营销活动列（60802）：发短信、优惠券、商品促销、整单促销、特价、套餐
-- 商城营销列（60803）：商城拼团、商城秒杀、商城预售、商城弹窗广告、加价购、热门搜索词推荐
-- 营销推广列（60804）：我要推广、推广历史查询

-- 5.9 商城模块 - 重新挂载
UPDATE sys_menu SET parent_id = 60901, update_time = CURRENT_TIMESTAMP WHERE menu_code LIKE 'mall:order%' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60902, update_time = CURRENT_TIMESTAMP WHERE menu_code LIKE 'mall:product%' AND deleted = 0;
UPDATE sys_menu SET parent_id = 60903, update_time = CURRENT_TIMESTAMP WHERE menu_code LIKE 'mall:config%' AND deleted = 0;

-- 5.10 资料模块 - 重新挂载
-- 商品管理列（61101）：商品、商品条码、商品价格管理、商品辅助资料、图片管理
-- 往来单位列（61102）：客户、供应商、物流公司、互联账号
-- 仓库管理列（61103）：仓库规划、商品货位设置
-- 职员权限列（61105）：职员部门、岗位权限、全部操作员
-- 财务账户列（61106）：银行账户、费用类型、其他收入、会计科目
UPDATE sys_menu SET parent_id = 61102, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'crm:supplier' AND deleted = 0;
UPDATE sys_menu SET parent_id = 61102, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'erp:partner' AND deleted = 0;

-- 5.11 设置模块 - 重新挂载
UPDATE sys_menu SET parent_id = 61201, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'system:menu' AND deleted = 0;
UPDATE sys_menu SET parent_id = 61201, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'system:config' AND deleted = 0;
UPDATE sys_menu SET parent_id = 61201, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'system:dict' AND deleted = 0;
UPDATE sys_menu SET parent_id = 61201, update_time = CURRENT_TIMESTAMP WHERE menu_code LIKE 'printing:%' AND deleted = 0;
UPDATE sys_menu SET parent_id = 61201, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'system:permission' AND deleted = 0;
UPDATE sys_menu SET parent_id = 61205, update_time = CURRENT_TIMESTAMP WHERE menu_code LIKE 'printing:%' AND deleted = 0;
UPDATE sys_menu SET parent_id = 61203, update_time = CURRENT_TIMESTAMP WHERE menu_code = 'system:log' AND deleted = 0;

-- ============================================================
-- 6. 插入系统级菜单项（menu_level=1）
--    这些菜单需要分配到系统模块（60013）的各列下
-- ============================================================

-- 6.1 租户管理列（61301）
-- 使用 'views/common/placeholder/index.vue' 作为未实现功能的占位组件
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (62001, 0, 61301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '租户列表', 'system:tenant:list', 1, 'admin/tenant/list', 'views/common/placeholder/index.vue', 'TeamOutlined', 100, 1, 1, 'pc-admin', 0, 1),
  (62002, 0, 61301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '租户审批', 'system:tenant:approve', 1, 'admin/tenant/approval', 'views/common/placeholder/index.vue', 'AuditOutlined', 200, 1, 1, 'pc-admin', 0, 1),
  (62003, 0, 61301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '租户套餐', 'system:tenant:package', 1, 'admin/tenant/package', 'views/common/placeholder/index.vue', 'AppstoreOutlined', 300, 1, 1, 'pc-admin', 0, 1),
  (62004, 0, 61301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '模块授权', 'system:tenant:module-auth', 1, 'admin/tenant/module-auth', 'views/common/placeholder/index.vue', 'SafetyOutlined', 400, 1, 1, 'pc-admin', 0, 1),
  (62005, 0, 61301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配额管理', 'system:tenant:quota', 1, 'admin/tenant/quota', 'views/common/placeholder/index.vue', 'DashboardOutlined', 500, 1, 1, 'pc-admin', 0, 1);

-- 6.2 模块管理列（61302）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (62101, 0, 61302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '模块列表', 'system:module:list', 1, 'admin/module/list', 'views/common/placeholder/index.vue', 'AppstoreOutlined', 100, 1, 1, 'pc-admin', 0, 1),
  (62102, 0, 61302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '模块版本', 'system:module:version', 1, 'admin/module/version', 'views/common/placeholder/index.vue', 'BranchesOutlined', 200, 1, 1, 'pc-admin', 0, 1),
  (62103, 0, 61302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '模块发布', 'system:module:release', 1, 'admin/module/release', 'views/common/placeholder/index.vue', 'SendOutlined', 300, 1, 1, 'pc-admin', 0, 1),
  (62104, 0, 61302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '使用统计', 'system:module:usage', 1, 'admin/module/usage', 'views/common/placeholder/index.vue', 'BarChartOutlined', 400, 1, 1, 'pc-admin', 0, 1);

-- 6.3 系统监控列（61303）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (62201, 0, 61303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '服务状态', 'system:monitor:health', 1, 'admin/monitor/health', 'views/common/placeholder/index.vue', 'CheckCircleOutlined', 100, 1, 1, 'pc-admin', 0, 1),
  (62202, 0, 61303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '性能监控', 'system:monitor:performance', 1, 'admin/monitor/performance', 'views/common/placeholder/index.vue', 'LineChartOutlined', 200, 1, 1, 'pc-admin', 0, 1),
  (62203, 0, 61303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '接口监控', 'system:monitor:api', 1, 'admin/monitor/api', 'views/common/placeholder/index.vue', 'SwapOutlined', 300, 1, 1, 'pc-admin', 0, 1),
  (62204, 0, 61303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统日志', 'system:monitor:log', 1, 'admin/monitor/log', 'views/system/log/index.vue', 'FileTextOutlined', 400, 1, 1, 'pc-admin', 0, 1),
  (62205, 0, 61303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '操作审计', 'system:monitor:audit', 1, 'admin/monitor/audit', 'views/common/placeholder/index.vue', 'SafetyOutlined', 500, 1, 1, 'pc-admin', 0, 1),
  (62206, 0, 61303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '缓存管理', 'system:monitor:cache', 1, 'admin/monitor/cache', 'views/common/placeholder/index.vue', 'InboxOutlined', 600, 1, 1, 'pc-admin', 0, 1);

-- 6.4 数据管理列（61304）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (62301, 0, 61304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '连接管理', 'system:data:connection', 1, 'admin/data/connection', 'views/common/placeholder/index.vue', 'LinkOutlined', 100, 1, 1, 'pc-admin', 0, 1),
  (62302, 0, 61304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '慢查询', 'system:data:slow-query', 1, 'admin/data/slow-query', 'views/common/placeholder/index.vue', 'SearchOutlined', 200, 1, 1, 'pc-admin', 0, 1),
  (62303, 0, 61304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '备份管理', 'system:data:backup', 1, 'admin/data/backup', 'views/common/placeholder/index.vue', 'FolderOutlined', 300, 1, 1, 'pc-admin', 0, 1),
  (62304, 0, 61304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '同步任务', 'system:data:sync', 1, 'admin/data/sync', 'views/common/placeholder/index.vue', 'SwapOutlined', 400, 1, 1, 'pc-admin', 0, 1),
  (62305, 0, 61304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '清理规则', 'system:data:cleanup', 1, 'admin/data/cleanup', 'views/common/placeholder/index.vue', 'DeleteOutlined', 500, 1, 1, 'pc-admin', 0, 1);

-- 6.5 开发工具列（61305）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (62401, 0, 61305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '代码生成', 'system:dev:codegen', 1, 'admin/dev/codegen', 'views/common/placeholder/index.vue', 'BuildOutlined', 100, 1, 1, 'pc-admin', 0, 1),
  (62402, 0, 61305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '模板管理', 'system:dev:template', 1, 'admin/dev/template', 'views/common/placeholder/index.vue', 'FileTextOutlined', 200, 1, 1, 'pc-admin', 0, 1),
  (62403, 0, 61305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'API文档', 'system:dev:api-doc', 1, 'admin/dev/api-doc', 'views/common/placeholder/index.vue', 'FileOutlined', 300, 1, 1, 'pc-admin', 0, 1),
  (62404, 0, 61305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'API测试', 'system:dev:api-test', 1, 'admin/dev/api-test', 'views/common/placeholder/index.vue', 'MonitorOutlined', 400, 1, 1, 'pc-admin', 0, 1),
  (62405, 0, 61305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '定时任务', 'system:dev:scheduler', 1, 'admin/dev/scheduler', 'views/common/placeholder/index.vue', 'ClockCircleOutlined', 500, 1, 1, 'pc-admin', 0, 1);

-- 6.6 平台设置列（61306）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (62501, 0, 61306, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '平台参数', 'system:platform:params', 1, 'admin/platform/params', 'views/common/placeholder/index.vue', 'SettingOutlined', 100, 1, 1, 'pc-admin', 0, 1),
  (62502, 0, 61306, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '邮件配置', 'system:platform:mail', 1, 'admin/platform/mail', 'views/common/placeholder/index.vue', 'MailOutlined', 200, 1, 1, 'pc-admin', 0, 1),
  (62503, 0, 61306, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '短信配置', 'system:platform:sms', 1, 'admin/platform/sms', 'views/common/placeholder/index.vue', 'BellOutlined', 300, 1, 1, 'pc-admin', 0, 1),
  (62504, 0, 61306, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '存储配置', 'system:platform:storage', 1, 'admin/platform/storage', 'views/common/placeholder/index.vue', 'FolderOutlined', 400, 1, 1, 'pc-admin', 0, 1),
  (62505, 0, 61306, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '安全策略', 'system:platform:security', 1, 'admin/platform/security', 'views/common/placeholder/index.vue', 'SafetyOutlined', 500, 1, 1, 'pc-admin', 0, 1);

-- ============================================================
-- 7. 清理孤儿节点（parent_id 引用了不存在的菜单 ID）
--    将未能匹配到新分组的节点挂到"未分类"节点下（通过 menu_code 范围）
-- ============================================================
-- 将仍指向旧分组（ID < 50000 且非 0）的节点重置为 parent_id=0
UPDATE sys_menu SET parent_id = 0, update_time = CURRENT_TIMESTAMP
WHERE parent_id > 0
  AND parent_id < 50000
  AND parent_id NOT IN (SELECT id FROM sys_menu)
  AND deleted = 0;

-- ============================================================
-- 8. 旧一级分组下的系统管理节点处理
--    将原"系统管理"分组下的菜单重新归类
-- ============================================================
-- 将原系统管理下的菜单挂到设置或系统模块下
-- 系统管理分组ID大约在 51600 范围（参考 V6.3.0）
UPDATE sys_menu SET parent_id = 61201, update_time = CURRENT_TIMESTAMP
WHERE parent_id IN (SELECT id FROM sys_menu WHERE menu_code IN ('system-ops', 'system:management') AND deleted = 0)
  AND deleted = 0;

-- ============================================================
-- 9. 数据完整性校验
-- ============================================================
-- 检查是否有叶子菜单仍指向已被软删除的父节点
-- 这只是一个注释说明，实际需要应用运行时验证
